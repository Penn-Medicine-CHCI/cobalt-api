package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.api.response.AccountApiResponse.AccountApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.pyranid.Database;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccountOnboardingScreeningPresentationTests {
	@Test
	public void presentationChangesWithAccountSourceConfigurationAndEligibilityRemainsSeparate() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback(app -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			AccountApiResponseFactory factory = app.getInjector().getInstance(AccountApiResponseFactory.class);
			CreateAccountRequest request = new CreateAccountRequest();
			request.setInstitutionId(InstitutionId.COBALT);
			request.setAccountSourceId(AccountSourceId.ANONYMOUS);
			Account account = accountService.findAccountById(accountService.createAccount(request)).get();

			assertEquals("LARGE_MODAL", factory.create(account).getOnboardingScreeningPresentationId());
			assertFalse(factory.create(account).getOnboardingScreeningFlowAppliesToAccount());

			for (String presentation : new String[]{"SMALL_MODAL", "FUTURE_PRESENTATION", "LARGE_MODAL"}) {
				database.execute("UPDATE account_source SET onboarding_screening_presentation_id=? WHERE account_source_id=?",
						presentation, AccountSourceId.ANONYMOUS);
				assertEquals(presentation, factory.create(account).getOnboardingScreeningPresentationId());
				assertEquals("LARGE_MODAL", accountService.findAccountSourceById(AccountSourceId.EMAIL_PASSWORD).get()
						.getOnboardingScreeningPresentationId());
			}

			account.setAccountSourceId(AccountSourceId.COBALT_SSO);
			assertEquals("SMALL_MODAL", factory.create(account).getOnboardingScreeningPresentationId());
			assertTrue(factory.create(account).getOnboardingScreeningFlowAppliesToAccount());
		});
	}
}
