/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Originally created at the University of Pennsylvania and Penn Medicine by:
 * Dr. David Asch; Dr. Lisa Bellini; Dr. Cecilia Livesey; Kelley Kugler; and Dr. Matthew Press.
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
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.ScreeningFlow;
import com.cobaltplatform.api.model.db.ScreeningFlowType.ScreeningFlowTypeId;
import com.cobaltplatform.api.model.db.ScreeningSession;
import org.junit.Test;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ScreeningServiceTests {
	@Test
	public void basicScreening() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionId institutionId = InstitutionId.COBALT;
			ScreeningService screeningService = app.getInjector().getInstance(ScreeningService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);

			// Make an anonymous account to test the flow
			UUID accountId = accountService.createAccount(new CreateAccountRequest() {{
				setAccountSourceId(AccountSourceId.ANONYMOUS);
				setInstitutionId(institutionId);
			}});

			ScreeningFlow providerTriageScreeningFlow = screeningService.findScreeningFlowsByInstitutionId(institutionId).stream()
					.filter(screeningFlow -> screeningFlow.getScreeningFlowTypeId() == ScreeningFlowTypeId.PROVIDER_TRIAGE)
					.findFirst().get();

			List<ScreeningSession> screeningSessions = screeningService.findScreeningSessionsByScreeningFlowId(
					providerTriageScreeningFlow.getScreeningFlowId(), accountId);

			assertEquals("Account already has a provider triage screening session", 0, screeningSessions.size());

			// TBD
		});
	}
}
