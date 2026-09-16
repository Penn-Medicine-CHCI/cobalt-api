/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningAnswersRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningAnswersRequest.CreateAnswerRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningSessionRequest;
import com.cobaltplatform.api.model.api.request.FindAppointmentBookingRequirementsRequest;
import com.cobaltplatform.api.model.api.response.AccountApiResponse.AccountApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityId;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentSelectionTypeId;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Feature.FeatureId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionLocation;
import com.cobaltplatform.api.model.db.ScreeningFlow;
import com.cobaltplatform.api.model.db.ScreeningFlowType.ScreeningFlowTypeId;
import com.cobaltplatform.api.model.db.ScreeningFlowVersion;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.service.AppointmentBookingRequirements;
import com.cobaltplatform.api.model.service.AppointmentBookingRequirements.AppointmentBookingRequirementsDestinationId;
import com.cobaltplatform.api.model.service.FeatureForInstitution;
import com.cobaltplatform.api.model.service.ScreeningQuestionContext;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.pyranid.Database;
import org.junit.Test;

import javax.annotation.concurrent.ThreadSafe;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@ThreadSafe
public class CobaltEmployerOnboardingTests {
	@Test
	public void onboardingFlowPublishesSingleEmployerQuestionWithoutPrompts() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			ScreeningService screeningService = app.getInjector().getInstance(ScreeningService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Institution institution = institutionService.findInstitutionById(InstitutionId.COBALT).get();
			UUID onboardingScreeningFlowId = institution.getOnboardingScreeningFlowId();

			assertNotNull(onboardingScreeningFlowId);
			ScreeningFlow screeningFlow = screeningService.findScreeningFlowById(onboardingScreeningFlowId).get();
			ScreeningFlowVersion screeningFlowVersion = screeningService
					.findScreeningFlowVersionById(screeningFlow.getActiveScreeningFlowVersionId()).get();
			assertEquals(ScreeningFlowTypeId.ONBOARDING, screeningFlow.getScreeningFlowTypeId());
			assertEquals(Boolean.FALSE, screeningFlowVersion.getSkippable());

			assertNull(screeningFlowVersion.getPreCompletionScreeningConfirmationPromptId());

			UUID accountId = createCobaltAccount(accountService);
			assertEquals("LARGE_MODAL", app.getInjector().getInstance(AccountApiResponseFactory.class)
					.create(accountService.findAccountById(accountId).get()).getOnboardingScreeningPresentationId());
			assertFalse(app.getInjector().getInstance(AccountApiResponseFactory.class)
					.create(accountService.findAccountById(accountId).get()).getOnboardingScreeningFlowAppliesToAccount());
			UUID screeningSessionId = createOnboardingSession(screeningService, onboardingScreeningFlowId, accountId);
			ScreeningQuestionContext questionContext = screeningService
					.findNextUnansweredScreeningQuestionContextByScreeningSessionId(screeningSessionId).get();

			assertEquals("Who is your employer?", questionContext.getScreeningQuestion().getQuestionText());
			assertEquals("Cobalt uses your employer to personalize your experience and identify the benefits and services available to you. We do not share your individual response with your employer, manager, or coworkers.",
					questionContext.getScreeningQuestion().getFooterText());
			assertEquals(Boolean.FALSE, questionContext.getScreeningQuestion().getPreferAutosubmit());
			assertEquals(Boolean.TRUE,
					questionContext.getScreeningQuestion().getMetadata().get("shouldUpdateAccountInstitutionLocation"));

			assertNull(questionContext.getScreeningQuestion().getPreQuestionScreeningConfirmationPromptId());

			List<String> expectedEmployerNames = institutionService.findLocationsByInstitutionId(InstitutionId.COBALT)
					.stream().map(InstitutionLocation::getName).toList();
			List<String> answerOptionTexts = questionContext.getScreeningAnswerOptions().stream()
					.map(answerOption -> answerOption.getAnswerOptionText()).toList();

			assertEquals(expectedEmployerNames,
					answerOptionTexts.subList(0, answerOptionTexts.size() - 1));
			assertEquals("I'm not sure / I'd rather not say", answerOptionTexts.get(answerOptionTexts.size() - 1));
		});
	}

	@Test
	public void employerAndDeclineAnswersUpdateAccountWithoutChangingCareNavigatorAccess() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			ScreeningService screeningService = app.getInjector().getInstance(ScreeningService.class);
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Institution institution = institutionService.findInstitutionById(InstitutionId.COBALT).get();
			UUID onboardingScreeningFlowId = institution.getOnboardingScreeningFlowId();

			UUID namedAccountId = createCobaltAccount(accountService);
			ScreeningQuestionContext namedQuestionContext = onboardingQuestionContext(screeningService,
					onboardingScreeningFlowId, namedAccountId);
			UUID expectedInstitutionLocationId = UUID.fromString((String) namedQuestionContext.getScreeningAnswerOptions()
					.get(0).getMetadata().get("institutionLocationId"));
			answer(screeningService, namedQuestionContext, namedAccountId, 0);

			assertFalse(screeningService.findNextUnansweredScreeningQuestionContextByScreeningSessionId(
					namedQuestionContext.getScreeningSessionScreening().getScreeningSessionId()).isPresent());

			Account namedAccount = accountService.findAccountById(namedAccountId).get();
			assertEquals(expectedInstitutionLocationId, namedAccount.getInstitutionLocationId());
			assertEquals(Boolean.TRUE, namedAccount.getPromptedForInstitutionLocation());
			assertTrue(screeningService.findScreeningSessionsByScreeningFlowIdAndTargetAccountId(
					onboardingScreeningFlowId, namedAccountId).stream().anyMatch(ScreeningSession::getCompleted));
			assertCareNavigatorAvailable(institutionService, institution, namedAccount);
			assertCareNavigatorBookable(appointmentService, namedAccount);

			UUID declinedAccountId = createCobaltAccount(accountService);
			ScreeningQuestionContext declinedQuestionContext = onboardingQuestionContext(screeningService,
					onboardingScreeningFlowId, declinedAccountId);
			answer(screeningService, declinedQuestionContext, declinedAccountId,
					declinedQuestionContext.getScreeningAnswerOptions().size() - 1);

			Account declinedAccount = accountService.findAccountById(declinedAccountId).get();
			assertNull(declinedAccount.getInstitutionLocationId());
			assertEquals(Boolean.TRUE, declinedAccount.getPromptedForInstitutionLocation());
			assertCareNavigatorAvailable(institutionService, institution, declinedAccount);
			assertCareNavigatorBookable(appointmentService, declinedAccount);

			database.execute("UPDATE provider SET active=FALSE WHERE provider_id=?",
					CareNavigatorBookingFixtureTests.CARE_NAVIGATOR_PROVIDER_ID);
			assertFalse(hasCareNavigatorFeature(institutionService.findFeaturesByInstitutionId(institution, namedAccount)));
			assertFalse(hasCareNavigatorFeature(institutionService.findFeaturesByInstitutionId(institution, declinedAccount)));
		});
	}

	@Test
	public void employerAnswerRejectsLocationFromAnotherInstitution() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			ScreeningService screeningService = app.getInjector().getInstance(ScreeningService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Institution institution = institutionService.findInstitutionById(InstitutionId.COBALT).get();
			UUID accountId = createCobaltAccount(accountService);
			ScreeningQuestionContext questionContext = onboardingQuestionContext(screeningService,
					institution.getOnboardingScreeningFlowId(), accountId);
			UUID otherInstitutionLocationId = UUID.randomUUID();

			database.execute("""
					INSERT INTO institution_location (
					  institution_location_id,
					  institution_id,
					  name,
					  display_order
					) VALUES (?, 'COBALT_IC', 'Other Institution Employer', 999)
					""", otherInstitutionLocationId);
			database.execute("""
					UPDATE screening_answer_option
					SET metadata=JSONB_BUILD_OBJECT('institutionLocationId', CAST(? AS TEXT))
					WHERE screening_answer_option_id=?
					""", otherInstitutionLocationId,
					questionContext.getScreeningAnswerOptions().get(0).getScreeningAnswerOptionId());

			ScreeningQuestionContext refreshedQuestionContext = screeningService
					.findNextUnansweredScreeningQuestionContextByScreeningSessionId(
						questionContext.getScreeningSessionScreening().getScreeningSessionId()).get();
			ValidationException exception = assertThrows(ValidationException.class,
					() -> answer(screeningService, refreshedQuestionContext, accountId, 0));

			assertTrue(exception.getFieldErrors().stream().anyMatch(error -> error.getField().equals("answers")));
			Account account = accountService.findAccountById(accountId).get();
			assertNull(account.getInstitutionLocationId());
			assertEquals(Boolean.FALSE, account.getPromptedForInstitutionLocation());
		});
	}

	protected static UUID createCobaltAccount(AccountService accountService) {
		return accountService.createAccount(new CreateAccountRequest() {{
			setAccountSourceId(AccountSourceId.ANONYMOUS);
			setInstitutionId(InstitutionId.COBALT);
		}});
	}

	protected static UUID createOnboardingSession(ScreeningService screeningService,
														 UUID screeningFlowId,
														 UUID accountId) {
		return screeningService.createScreeningSession(new CreateScreeningSessionRequest() {{
			setScreeningFlowId(screeningFlowId);
			setTargetAccountId(accountId);
			setCreatedByAccountId(accountId);
		}});
	}

	protected static ScreeningQuestionContext onboardingQuestionContext(ScreeningService screeningService,
																		UUID screeningFlowId,
																		UUID accountId) {
		UUID screeningSessionId = createOnboardingSession(screeningService, screeningFlowId, accountId);
		return screeningService.findNextUnansweredScreeningQuestionContextByScreeningSessionId(screeningSessionId).get();
	}

	protected static void answer(ScreeningService screeningService,
									 ScreeningQuestionContext questionContext,
									 UUID accountId,
									 int answerOptionIndex) {
		CreateAnswerRequest answer = new CreateAnswerRequest();
		answer.setScreeningAnswerOptionId(questionContext.getScreeningAnswerOptions().get(answerOptionIndex)
				.getScreeningAnswerOptionId());
		CreateScreeningAnswersRequest request = new CreateScreeningAnswersRequest();
		request.setScreeningQuestionContextId(questionContext.getScreeningQuestionContextId());
		request.setCreatedByAccountId(accountId);
		request.setAnswers(List.of(answer));
		request.setForce(true);
		screeningService.createScreeningAnswers(request);
	}

	protected static void assertCareNavigatorAvailable(InstitutionService institutionService,
															Institution institution,
															Account account) {
		List<FeatureForInstitution> features = institutionService.findFeaturesByInstitutionId(institution, account);
		assertTrue(hasCareNavigatorFeature(features));
		FeatureForInstitution careNavigatorFeature = features.stream()
				.filter(feature -> feature.getFeatureId() == FeatureId.RESOURCE_NAVIGATOR).findFirst().get();
		assertEquals(CareNavigatorBookingFixtureTests.CARE_NAVIGATOR_PROVIDER_ID,
				careNavigatorFeature.getProviderId());
	}

	protected static boolean hasCareNavigatorFeature(List<FeatureForInstitution> features) {
		return features.stream().anyMatch(feature -> feature.getFeatureId() == FeatureId.RESOURCE_NAVIGATOR);
	}

	protected static void assertCareNavigatorBookable(AppointmentService appointmentService, Account account) {
		LocalDate date = LocalDate.now(ZoneId.of("America/New_York")).plusDays(14);

		while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY)
			date = date.plusDays(1);

		FindAppointmentBookingRequirementsRequest request = new FindAppointmentBookingRequirementsRequest();
		request.setAccountId(account.getAccountId());
		request.setProviderId(CareNavigatorBookingFixtureTests.CARE_NAVIGATOR_PROVIDER_ID);
		request.setAppointmentTypeId(CareNavigatorBookingFixtureTests.CARE_NAVIGATOR_APPOINTMENT_TYPE_ID);
		request.setAppointmentSelectionTypeId(ProviderAppointmentSelectionTypeId.APPOINTMENT_PREDETERMINED);
		request.setAppointmentModalityId(ProviderAppointmentModalityId.VIRTUAL);
		request.setDate(date);
		request.setTime(LocalTime.of(9, 0));

		AppointmentBookingRequirements requirements = appointmentService
				.findAppointmentBookingRequirements(request, account);
		assertEquals(AppointmentBookingRequirementsDestinationId.SCREENING_SESSION,
				requirements.getAppointmentBookingRequirementsDestinationId());
		assertEquals(CareNavigatorBookingFixtureTests.CARE_NAVIGATOR_SCREENING_FLOW_ID,
				requirements.getScreeningFlowId());
	}
}
