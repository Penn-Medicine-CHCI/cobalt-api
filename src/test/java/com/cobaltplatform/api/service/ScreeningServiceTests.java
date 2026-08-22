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
import com.cobaltplatform.api.model.api.request.CreateScreeningAnswersRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningSessionRequest;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.ScreeningFlow;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.service.ScreeningQuestionContext;
import com.cobaltplatform.api.model.service.ScreeningQuestionContextId;
import com.cobaltplatform.api.model.service.ScreeningSessionDestination;
import com.cobaltplatform.api.model.service.ScreeningSessionDestination.ScreeningSessionDestinationId;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.pyranid.Database;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ScreeningServiceTests {
	@Test
	public void appointmentBookingConfirmationDestinationCarriesAppointmentBookingContext() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionId institutionId = InstitutionId.COBALT;
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			ScreeningService screeningService = app.getInjector().getInstance(ScreeningService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Institution institution = institutionService.findInstitutionById(institutionId).get();
			ScreeningFlow screeningFlow = screeningService.findScreeningFlowById(institution.getFeatureScreeningFlowId()).get();
			UUID providerId = UUID.randomUUID();
			UUID appointmentTypeId = UUID.randomUUID();

			setBookingV2Enabled(database, institutionId, true);

			database.execute("""
					UPDATE screening_flow_version
					SET destination_function=?
					WHERE screening_flow_version_id=?
					""", """
					output.screeningSessionDestinationId = 'APPOINTMENT_BOOKING_CONFIRMATION';
					output.context = { result: 'SUCCESS' };
					""", screeningFlow.getActiveScreeningFlowVersionId());

			UUID accountId = accountService.createAccount(new CreateAccountRequest() {{
				setAccountSourceId(AccountSourceId.ANONYMOUS);
				setInstitutionId(institutionId);
			}});

			UUID screeningSessionId = screeningService.createScreeningSession(new CreateScreeningSessionRequest() {{
				setScreeningFlowId(screeningFlow.getScreeningFlowId());
				setTargetAccountId(accountId);
				setCreatedByAccountId(accountId);
				setMetadata(Map.of("appointmentBooking", Map.of(
						"providerId", providerId.toString(),
						"appointmentTypeId", appointmentTypeId.toString(),
						"date", "2026-01-01",
						"time", "09:30:00"
				)));
			}});

			database.execute("""
					UPDATE screening_session
					SET completed=TRUE,
					completed_at=NOW()
					WHERE screening_session_id=?
					""", screeningSessionId);

			ScreeningSessionDestination screeningSessionDestination =
					screeningService.determineDestinationForScreeningSessionId(screeningSessionId).get();

			assertEquals(ScreeningSessionDestinationId.APPOINTMENT_BOOKING_CONFIRMATION,
					screeningSessionDestination.getScreeningSessionDestinationId());
			assertEquals("SUCCESS", screeningSessionDestination.getContext().get("result"));
			assertEquals(providerId.toString(), screeningSessionDestination.getContext().get("providerId"));
			assertEquals(appointmentTypeId.toString(), screeningSessionDestination.getContext().get("appointmentTypeId"));
			assertEquals("2026-01-01", screeningSessionDestination.getContext().get("date"));
			assertEquals("09:30:00", screeningSessionDestination.getContext().get("time"));
		});
	}

	@Test
	public void appointmentBookingConfirmationDestinationInfersProviderContextWhenMetadataMissing() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionId institutionId = InstitutionId.COBALT;
			ScreeningService screeningService = app.getInjector().getInstance(ScreeningService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			UUID accountId = accountService.createAccount(new CreateAccountRequest() {{
				setAccountSourceId(AccountSourceId.ANONYMOUS);
				setInstitutionId(institutionId);
			}});
			UUID providerId = UUID.randomUUID();
			UUID appointmentTypeId = UUID.randomUUID();
			UUID screeningId = UUID.randomUUID();
			UUID screeningVersionId = UUID.randomUUID();
			UUID screeningFlowId = UUID.randomUUID();
			UUID screeningFlowVersionId = UUID.randomUUID();

			setBookingV2Enabled(database, institutionId, true);

			database.execute("""
					INSERT INTO provider (
					  provider_id,
					  institution_id,
					  name,
					  email_address,
					  url_name,
					  scheduling_system_id,
					  videoconference_platform_id,
					  active
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
					""", providerId, institutionId, "Provider Intake Destination Test",
					format("provider-intake-destination-%s@example.com", providerId),
					format("provider-intake-destination-%s", providerId), "COBALT", "SWITCHBOARD", true);

			database.execute("""
					INSERT INTO appointment_type (
					  appointment_type_id,
					  visit_type_id,
					  name,
					  description,
					  duration_in_minutes,
					  scheduling_system_id,
					  screening_flow_id
					) VALUES (?, ?, ?, ?, ?, ?, ?)
					""", appointmentTypeId, "INITIAL", "Provider Intake Destination Test",
					"Provider intake destination test", 30L, "COBALT", null);

			database.execute("""
					INSERT INTO provider_appointment_type (
					  provider_id,
					  appointment_type_id,
					  display_order
					) VALUES (?, ?, ?)
					""", providerId, appointmentTypeId, 1);

			database.execute("""
					INSERT INTO screening (
					  screening_id,
					  name,
					  active_screening_version_id,
					  created_by_account_id
					) VALUES (?, ?, ?, ?)
					""", screeningId, "Provider Intake Destination Test", null, accountId);

			database.execute("""
					INSERT INTO screening_version (
					  screening_version_id,
					  screening_id,
					  screening_type_id,
					  created_by_account_id,
					  version_number,
					  scoring_function
					) VALUES (?, ?, ?, ?, ?, ?)
					""", screeningVersionId, screeningId, "CUSTOM", accountId, 1,
					"output.completed = true; output.score = {};");

			database.execute("""
					UPDATE screening
					SET active_screening_version_id=?
					WHERE screening_id=?
					""", screeningVersionId, screeningId);

			database.execute("""
					INSERT INTO screening_institution (
					  screening_id,
					  institution_id
					) VALUES (?, ?)
					""", screeningId, institutionId);

			database.execute("""
					INSERT INTO screening_flow (
					  screening_flow_id,
					  institution_id,
					  active_screening_flow_version_id,
					  screening_flow_type_id,
					  created_by_account_id,
					  name
					) VALUES (?, ?, ?, ?, ?, ?)
					""", screeningFlowId, institutionId, null, "PROVIDER_INTAKE", accountId,
					format("Provider Intake Destination Test %s", screeningFlowId));

			database.execute("""
					INSERT INTO screening_flow_version (
					  screening_flow_version_id,
					  screening_flow_id,
					  initial_screening_id,
					  phone_number_required,
					  version_number,
					  orchestration_function,
					  results_function,
					  destination_function,
					  created_by_account_id
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
					""", screeningFlowVersionId, screeningFlowId, screeningId, false, 1,
					"output.completed = Boolean(input.screeningSession.completed); output.crisisIndicated = false;",
					"output.supportRoleRecommendations = [];",
					"""
					output.screeningSessionDestinationId = 'APPOINTMENT_BOOKING_CONFIRMATION';
					output.context = { result: 'SUCCESS' };
					""", accountId);

			database.execute("""
					UPDATE screening_flow
					SET active_screening_flow_version_id=?
					WHERE screening_flow_id=?
					""", screeningFlowVersionId, screeningFlowId);

			database.execute("""
					UPDATE appointment_type
					SET screening_flow_id=?
					WHERE appointment_type_id=?
					""", screeningFlowId, appointmentTypeId);

			UUID screeningSessionId = screeningService.createScreeningSession(new CreateScreeningSessionRequest() {{
				setScreeningFlowId(screeningFlowId);
				setTargetAccountId(accountId);
				setCreatedByAccountId(accountId);
			}});

			database.execute("""
					UPDATE screening_session
					SET completed=TRUE,
					    completed_at=NOW()
					WHERE screening_session_id=?
					""", screeningSessionId);

			ScreeningSessionDestination screeningSessionDestination =
					screeningService.determineDestinationForScreeningSessionId(screeningSessionId).get();

			assertEquals(ScreeningSessionDestinationId.APPOINTMENT_BOOKING_CONFIRMATION,
					screeningSessionDestination.getScreeningSessionDestinationId());
			assertEquals("SUCCESS", screeningSessionDestination.getContext().get("result"));
			assertEquals(accountId.toString(), screeningSessionDestination.getContext().get("accountId"));
			assertEquals("PROVIDER", screeningSessionDestination.getContext().get("providerSearchResultTypeId"));
			assertEquals(providerId.toString(), screeningSessionDestination.getContext().get("providerId"));
			assertEquals(appointmentTypeId.toString(), screeningSessionDestination.getContext().get("appointmentTypeId"));
			assertEquals(screeningFlowId.toString(), screeningSessionDestination.getContext().get("screeningFlowId"));
		});
	}

	@Test
	public void legacyProviderAppointmentBookingDestinationCarriesFlatAppointmentBookingContext() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionId institutionId = InstitutionId.COBALT;
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			ScreeningService screeningService = app.getInjector().getInstance(ScreeningService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Institution institution = institutionService.findInstitutionById(institutionId).get();
			ScreeningFlow screeningFlow = screeningService.findScreeningFlowById(institution.getFeatureScreeningFlowId()).get();
			UUID providerId = UUID.randomUUID();
			UUID appointmentTypeId = UUID.randomUUID();

			setBookingV2Enabled(database, institutionId, true);

			database.execute("""
					UPDATE screening_flow_version
					SET destination_function=?
					WHERE screening_flow_version_id=?
					""", """
					output.screeningSessionDestinationId = 'PROVIDER_APPOINTMENT_BOOKING';
					output.context = { result: 'SUCCESS' };
					""", screeningFlow.getActiveScreeningFlowVersionId());

			UUID accountId = accountService.createAccount(new CreateAccountRequest() {{
				setAccountSourceId(AccountSourceId.ANONYMOUS);
				setInstitutionId(institutionId);
			}});

			UUID screeningSessionId = screeningService.createScreeningSession(new CreateScreeningSessionRequest() {{
				setScreeningFlowId(screeningFlow.getScreeningFlowId());
				setTargetAccountId(accountId);
				setCreatedByAccountId(accountId);
				setMetadata(Map.of("appointmentBooking", Map.of(
						"providerId", providerId.toString(),
						"appointmentTypeId", appointmentTypeId.toString()
				)));
			}});

			database.execute("""
					UPDATE screening_session
					SET completed=TRUE,
					completed_at=NOW()
					WHERE screening_session_id=?
					""", screeningSessionId);

			ScreeningSessionDestination screeningSessionDestination =
					screeningService.determineDestinationForScreeningSessionId(screeningSessionId).get();

			assertEquals(ScreeningSessionDestinationId.PROVIDER_APPOINTMENT_BOOKING,
					screeningSessionDestination.getScreeningSessionDestinationId());
			assertEquals("SUCCESS", screeningSessionDestination.getContext().get("result"));
			assertEquals(providerId.toString(), screeningSessionDestination.getContext().get("providerId"));
			assertEquals(appointmentTypeId.toString(), screeningSessionDestination.getContext().get("appointmentTypeId"));
		});
	}

	@Test
	public void institutionReferrerDestinationWithAppointmentBookingMetadataPreservesDestination() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionId institutionId = InstitutionId.COBALT;
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			ScreeningService screeningService = app.getInjector().getInstance(ScreeningService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Institution institution = institutionService.findInstitutionById(institutionId).get();
			ScreeningFlow screeningFlow = screeningService.findScreeningFlowById(institution.getFeatureScreeningFlowId()).get();
			UUID providerId = UUID.randomUUID();
			UUID appointmentTypeId = UUID.randomUUID();

			setBookingV2Enabled(database, institutionId, true);

			database.execute("""
					UPDATE screening_flow_version
					SET destination_function=?
					WHERE screening_flow_version_id=?
					""", """
					output.screeningSessionDestinationId = 'INSTITUTION_REFERRER_DETAIL';
					output.context = { institutionReferrerUrlName: 'autism-clinic' };
					""", screeningFlow.getActiveScreeningFlowVersionId());

			UUID accountId = accountService.createAccount(new CreateAccountRequest() {{
				setAccountSourceId(AccountSourceId.ANONYMOUS);
				setInstitutionId(institutionId);
			}});

			UUID screeningSessionId = screeningService.createScreeningSession(new CreateScreeningSessionRequest() {{
				setScreeningFlowId(screeningFlow.getScreeningFlowId());
				setTargetAccountId(accountId);
				setCreatedByAccountId(accountId);
				setMetadata(Map.of("appointmentBooking", Map.of(
						"providerId", providerId.toString(),
						"appointmentTypeId", appointmentTypeId.toString()
				)));
			}});

			database.execute("""
					UPDATE screening_session
					SET completed=TRUE,
					completed_at=NOW()
					WHERE screening_session_id=?
					""", screeningSessionId);

			ScreeningSessionDestination screeningSessionDestination =
					screeningService.determineDestinationForScreeningSessionId(screeningSessionId).get();
			Map<String, Object> appointmentBookingContext =
					(Map<String, Object>) screeningSessionDestination.getContext().get("appointmentBooking");

			assertEquals(ScreeningSessionDestinationId.INSTITUTION_REFERRER_DETAIL,
					screeningSessionDestination.getScreeningSessionDestinationId());
			assertEquals("autism-clinic", screeningSessionDestination.getContext().get("institutionReferrerUrlName"));
			assertEquals(ScreeningSessionDestinationId.APPOINTMENT_BOOKING_CONFIRMATION.name(),
					screeningSessionDestination.getContext().get("appointmentBookingCompletionDestinationId"));
			assertEquals(providerId.toString(), appointmentBookingContext.get("providerId"));
			assertEquals(appointmentTypeId.toString(), appointmentBookingContext.get("appointmentTypeId"));
		});
	}

	@Test
	public void institutionReferralDestinationWithAppointmentBookingMetadataAppendsBookingQueryParameters() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionId institutionId = InstitutionId.COBALT;
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			ScreeningService screeningService = app.getInjector().getInstance(ScreeningService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Institution institution = institutionService.findInstitutionById(institutionId).get();
			ScreeningFlow screeningFlow = screeningService.findScreeningFlowById(institution.getFeatureScreeningFlowId()).get();
			UUID providerId = UUID.randomUUID();
			UUID appointmentTypeId = UUID.randomUUID();

			setBookingV2Enabled(database, institutionId, true);

			database.execute("""
					UPDATE screening_flow_version
					SET destination_function=?
					WHERE screening_flow_version_id=?
					""", """
					output.screeningSessionDestinationId = 'INSTITUTION_REFERRAL';
					output.context = {
					  institutionReferralUrl: '/referrals/autism-clinic/CONSULT_EVALUATION?returnTo=%2Freferrals%2Fautism-clinic'
					};
					""", screeningFlow.getActiveScreeningFlowVersionId());

			UUID accountId = accountService.createAccount(new CreateAccountRequest() {{
				setAccountSourceId(AccountSourceId.ANONYMOUS);
				setInstitutionId(institutionId);
			}});

			UUID screeningSessionId = screeningService.createScreeningSession(new CreateScreeningSessionRequest() {{
				setScreeningFlowId(screeningFlow.getScreeningFlowId());
				setTargetAccountId(accountId);
				setCreatedByAccountId(accountId);
				setMetadata(Map.of("appointmentBooking", Map.of(
						"providerId", providerId.toString(),
						"appointmentTypeId", appointmentTypeId.toString(),
						"date", "2026-01-01",
						"time", "09:30:00"
				)));
			}});

			database.execute("""
					UPDATE screening_session
					SET completed=TRUE,
					completed_at=NOW()
					WHERE screening_session_id=?
					""", screeningSessionId);

			ScreeningSessionDestination screeningSessionDestination =
					screeningService.determineDestinationForScreeningSessionId(screeningSessionId).get();
			Map<String, Object> appointmentBookingContext =
					(Map<String, Object>) screeningSessionDestination.getContext().get("appointmentBooking");
			String institutionReferralUrl = (String) screeningSessionDestination.getContext().get("institutionReferralUrl");

			assertEquals(ScreeningSessionDestinationId.INSTITUTION_REFERRAL,
					screeningSessionDestination.getScreeningSessionDestinationId());
			assertEquals(ScreeningSessionDestinationId.APPOINTMENT_BOOKING_CONFIRMATION.name(),
					screeningSessionDestination.getContext().get("appointmentBookingCompletionDestinationId"));
			assertEquals(providerId.toString(), appointmentBookingContext.get("providerId"));
			assertEquals(appointmentTypeId.toString(), appointmentBookingContext.get("appointmentTypeId"));
			Assert.assertTrue(institutionReferralUrl.startsWith("/referrals/autism-clinic/CONSULT_EVALUATION?"));
			Assert.assertTrue(institutionReferralUrl.contains("returnTo=%2Freferrals%2Fautism-clinic"));
			Assert.assertTrue(institutionReferralUrl.contains("appointmentBookingCompletionDestinationId=APPOINTMENT_BOOKING_CONFIRMATION"));
			Assert.assertTrue(institutionReferralUrl.contains(format("providerId=%s", providerId)));
			Assert.assertTrue(institutionReferralUrl.contains(format("appointmentTypeId=%s", appointmentTypeId)));
			Assert.assertTrue(institutionReferralUrl.contains("date=2026-01-01"));
			Assert.assertTrue(institutionReferralUrl.contains("time=09%3A30%3A00"));
		});
	}

	@Test
	public void appointmentBookingMetadataIsIgnoredWhenBookingV2Disabled() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionId institutionId = InstitutionId.COBALT;
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			ScreeningService screeningService = app.getInjector().getInstance(ScreeningService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Institution institution = institutionService.findInstitutionById(institutionId).get();
			ScreeningFlow screeningFlow = screeningService.findScreeningFlowById(institution.getFeatureScreeningFlowId()).get();
			UUID providerId = UUID.randomUUID();
			UUID appointmentTypeId = UUID.randomUUID();

			setBookingV2Enabled(database, institutionId, false);

			database.execute("""
					UPDATE screening_flow_version
					SET destination_function=?
					WHERE screening_flow_version_id=?
					""", """
					output.screeningSessionDestinationId = 'INSTITUTION_REFERRAL';
					output.context = {
					  institutionReferralUrl: '/referrals/autism-clinic/CONSULT_EVALUATION?returnTo=%2Freferrals%2Fautism-clinic'
					};
					""", screeningFlow.getActiveScreeningFlowVersionId());

			UUID accountId = accountService.createAccount(new CreateAccountRequest() {{
				setAccountSourceId(AccountSourceId.ANONYMOUS);
				setInstitutionId(institutionId);
			}});

			UUID screeningSessionId = screeningService.createScreeningSession(new CreateScreeningSessionRequest() {{
				setScreeningFlowId(screeningFlow.getScreeningFlowId());
				setTargetAccountId(accountId);
				setCreatedByAccountId(accountId);
				setMetadata(Map.of("appointmentBooking", Map.of(
						"providerId", providerId.toString(),
						"appointmentTypeId", appointmentTypeId.toString()
				)));
			}});

			database.execute("""
					UPDATE screening_session
					SET completed=TRUE,
					completed_at=NOW()
					WHERE screening_session_id=?
					""", screeningSessionId);

			ScreeningSessionDestination screeningSessionDestination =
					screeningService.determineDestinationForScreeningSessionId(screeningSessionId).get();
			String institutionReferralUrl = (String) screeningSessionDestination.getContext().get("institutionReferralUrl");

			assertEquals(ScreeningSessionDestinationId.INSTITUTION_REFERRAL,
					screeningSessionDestination.getScreeningSessionDestinationId());
			assertEquals("/referrals/autism-clinic/CONSULT_EVALUATION?returnTo=%2Freferrals%2Fautism-clinic", institutionReferralUrl);
			Assert.assertFalse(screeningSessionDestination.getContext().containsKey("appointmentBooking"));
			Assert.assertFalse(screeningSessionDestination.getContext().containsKey("appointmentBookingCompletionDestinationId"));
		});
	}

	@Test
	public void basicScreening() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionId institutionId = InstitutionId.COBALT;
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			ScreeningService screeningService = app.getInjector().getInstance(ScreeningService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);

			Institution institution = institutionService.findInstitutionById(institutionId).get();

			// Make an anonymous account to test the flow
			UUID accountId = accountService.createAccount(new CreateAccountRequest() {{
				setAccountSourceId(AccountSourceId.ANONYMOUS);
				setInstitutionId(institutionId);
			}});

			// Find the provider triage flow for the account's institution
			ScreeningFlow providerTriageScreeningFlow = screeningService.findScreeningFlowById(institution.getFeatureScreeningFlowId()).get();

			// Confirm there are no existing screening sessions for that flow
			List<ScreeningSession> screeningSessions = screeningService.findScreeningSessionsByScreeningFlowId(
					providerTriageScreeningFlow.getScreeningFlowId(), accountId);

			assertEquals("Account already has a provider triage screening session", 0, screeningSessions.size());

			// Start a new screening session for that flow
			UUID screeningSessionId = screeningService.createScreeningSession(new CreateScreeningSessionRequest() {{
				setScreeningFlowId(providerTriageScreeningFlow.getScreeningFlowId());
				setTargetAccountId(accountId);
				setCreatedByAccountId(accountId);
			}});

			// Ensure the screening session was created
			screeningSessions = screeningService.findScreeningSessionsByScreeningFlowId(
					providerTriageScreeningFlow.getScreeningFlowId(), accountId);

			assertEquals("Account is missing a provider triage screening session", 1, screeningSessions.size());

			// Keep track of some things so we can answer questions from a few screenings, then "reset" to an earlier
			// screening by jumping back and changing our answer to an earlier question
			int screeningQuestionIndexToResetTo = 2;
			int screeningQuestionIndexToResetAt = 12;
			boolean reset = false;
			ScreeningQuestionContext screeningQuestionContextToResetTo = null;
			int i = 0;

			while (true) {
				ScreeningQuestionContext screeningQuestionContext = screeningService.findNextUnansweredScreeningQuestionContextByScreeningSessionId(screeningSessionId).orElse(null);

				// No more questions in the session, we're done.
				if (screeningQuestionContext == null)
					break;

				// Store off so we can reset to this question later
				if (i == screeningQuestionIndexToResetTo)
					screeningQuestionContextToResetTo = screeningQuestionContext;

				// If it's time to reset, restore the old question context
				if (i == screeningQuestionIndexToResetAt && !reset)
					screeningQuestionContext = screeningQuestionContextToResetTo;

				// Pick the last answer option...
				UUID screeningAnswerOptionId = screeningQuestionContext.getScreeningAnswerOptions().get(
						screeningQuestionContext.getScreeningAnswerOptions().size() - 1).getScreeningAnswerOptionId();

				// ...or, if we are resetting, change the previous answer to a different one (the first option)
				if (i == screeningQuestionIndexToResetAt) {
					screeningAnswerOptionId = screeningQuestionContext.getScreeningAnswerOptions().get(0).getScreeningAnswerOptionId();
					reset = true;
				}

				ScreeningQuestionContextId screeningQuestionContextId = new ScreeningQuestionContextId(
						screeningQuestionContext.getScreeningSessionScreening().getScreeningSessionScreeningId(),
						screeningQuestionContext.getScreeningQuestion().getScreeningQuestionId());

				UUID pinnedScreeningAnswerOptionId = screeningAnswerOptionId;

				// ...and answer it.
				screeningService.createScreeningAnswers(new CreateScreeningAnswersRequest() {{
					setScreeningQuestionContextId(screeningQuestionContextId);
					setCreatedByAccountId(accountId);
					setAnswers(List.of(new CreateAnswerRequest() {{
						setScreeningAnswerOptionId(pinnedScreeningAnswerOptionId);
					}}));
				}});

				++i;
			}

			ScreeningSessionDestination screeningSessionDestination = screeningService.determineDestinationForScreeningSessionId(screeningSessionId).get();

			Assert.assertEquals("Post-session destination should have been the crisis screen",
					ScreeningSessionDestinationId.CRISIS, screeningSessionDestination.getScreeningSessionDestinationId());
		});
	}

	protected static void setBookingV2Enabled(Database database,
																						InstitutionId institutionId,
																						boolean enabled) {
		database.execute("UPDATE institution SET booking_v2_enabled=? WHERE institution_id=?", enabled, institutionId);
	}
}
