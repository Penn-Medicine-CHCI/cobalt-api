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
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.model.api.request.CreateAppointmentRequest;
import com.cobaltplatform.api.model.api.request.CreateAppointmentRequest.BookingExperienceId;
import com.cobaltplatform.api.model.api.request.CancelCareEncounterAppointmentRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningAnswersRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningAnswersRequest.CreateAnswerRequest;
import com.cobaltplatform.api.model.api.request.CancelCareEncounterRequest;
import com.cobaltplatform.api.model.api.request.FindAppointmentBookingRequirementsRequest;
import com.cobaltplatform.api.model.api.request.FindCareEncountersRequest;
import com.cobaltplatform.api.model.api.request.FindCareEncountersRequest.CareEncounterAssignmentScopeId;
import com.cobaltplatform.api.model.api.request.UpdateAppointmentRequest;
import com.cobaltplatform.api.model.api.request.UpdateCareEncounterRequest;
import com.cobaltplatform.api.model.api.response.LocationApiResponse;
import com.cobaltplatform.api.model.api.response.InstitutionApiResponse;
import com.cobaltplatform.api.model.api.response.ProviderApiResponse;
import com.cobaltplatform.api.model.api.response.CareEncounterApiResponse;
import com.cobaltplatform.api.model.api.response.CareEncounterApiResponse.CareEncounterApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CareEncounterListApiResponse;
import com.cobaltplatform.api.model.api.response.CareEncounterListApiResponse.CareEncounterListApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityId;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentSelectionTypeId;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.AttendanceStatus.AttendanceStatusId;
import com.cobaltplatform.api.model.db.CareEncounter;
import com.cobaltplatform.api.model.db.CareEncounterCancellationReason.CareEncounterCancellationReasonId;
import com.cobaltplatform.api.model.db.CareEncounterStatus.CareEncounterStatusId;
import com.cobaltplatform.api.model.db.Feature.FeatureId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.service.AppointmentBookingRequirements;
import com.cobaltplatform.api.model.service.AppointmentBookingRequirements.AppointmentBookingRequirementsDestinationId;
import com.cobaltplatform.api.model.service.FeatureForInstitution;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.model.service.ScreeningQuestionContext;
import com.cobaltplatform.api.model.service.ScreeningSessionDestination;
import com.cobaltplatform.api.model.service.ScreeningSessionDestination.ScreeningSessionDestinationId;
import com.cobaltplatform.api.model.service.ScreeningSessionDestinationResultId;
import com.cobaltplatform.api.model.service.ScreeningSessionResult;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.cobaltplatform.api.web.resource.AccountResource;
import com.cobaltplatform.api.web.resource.ProviderResource;
import com.pyranid.Database;
import com.soklet.web.response.ApiResponse;
import org.junit.Test;

import javax.annotation.concurrent.ThreadSafe;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@ThreadSafe
public class CareNavigatorBookingFixtureTests {
	protected static final String NAVIGATOR_CONTEXT_FIXTURE_TEXT =
			"I would like help finding an in-network therapist with evening availability.";
	protected static final UUID CARE_NAVIGATOR_ACCOUNT_ID = UUID.fromString("ca4e0000-0000-4000-8000-000000000001");
	protected static final UUID CARE_NAVIGATOR_PROVIDER_ID = UUID.fromString("ca4e0000-0000-4000-8000-000000000002");
	protected static final UUID CARE_NAVIGATOR_APPOINTMENT_TYPE_ID = UUID.fromString("ca4e0000-0000-4000-8000-000000000003");
	protected static final UUID CARE_NAVIGATOR_SCREENING_FLOW_ID = UUID.fromString("ca4e0000-0000-4000-8000-00000000000b");
	protected static final UUID CARE_NAVIGATOR_PROVIDER_LOCATION_ID = UUID.fromString("ca4e0000-0000-4000-8000-00000000000e");
	protected static final UUID CARE_NAVIGATOR_ATTENDED_FIXTURE_PATIENT_ID = UUID.fromString("ca4e1000-0000-4000-8000-000000000001");
	protected static final UUID CARE_NAVIGATOR_ACTIVE_FIXTURE_PATIENT_ID = UUID.fromString("ca4e1000-0000-4000-8000-000000000002");
	protected static final UUID CARE_NAVIGATOR_ACCOUNT_FIXTURE_PATIENT_ID = UUID.fromString("ca4e1000-0000-4000-8000-000000000003");
	protected static final UUID CARE_NAVIGATOR_PATIENT_CANCELED_FIXTURE_PATIENT_ID = UUID.fromString("ca4e1000-0000-4000-8000-000000000004");
	protected static final UUID CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID = UUID.fromString("ca4e2000-0000-4000-8000-000000000001");
	protected static final UUID CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID = UUID.fromString("ca4e2000-0000-4000-8000-000000000002");
	protected static final UUID CARE_NAVIGATOR_CANCELED_APPOINTMENT_ID = UUID.fromString("ca4e2000-0000-4000-8000-000000000003");
	protected static final UUID CARE_NAVIGATOR_REBOOKED_APPOINTMENT_ID = UUID.fromString("ca4e2000-0000-4000-8000-000000000004");
	protected static final UUID CARE_NAVIGATOR_PATIENT_CANCELED_APPOINTMENT_ID = UUID.fromString("ca4e2000-0000-4000-8000-000000000005");
	protected static final UUID CARE_NAVIGATOR_UPCOMING_SCREENING_SESSION_ID = UUID.fromString("ca4e3000-0000-4000-8000-000000000001");

	@Test
	public void careNavigatorFixturePopulatesHomepageFeatureResponse() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AccountResource accountResource = app.getInjector().getInstance(AccountResource.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			assertEquals(CARE_NAVIGATOR_PROVIDER_ID,
					institutionService.findCareNavigatorBookingProviderIdForInstitutionId(InstitutionId.COBALT).get());

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, ZoneId.of("America/New_York")).build(), () -> {
				ApiResponse response = accountResource.account(account.getAccountId(), Optional.empty());
				Map<String, Object> model = (Map<String, Object>) response.model().get();
				InstitutionApiResponse institution = (InstitutionApiResponse) model.get("institution");
				FeatureForInstitution resourceNavigator = institution.getFeatures().stream()
						.filter(feature -> feature.getFeatureId() == FeatureId.RESOURCE_NAVIGATOR)
						.findFirst()
						.get();

				assertEquals(200, response.status());
				assertEquals("Connect with a Care Navigator", resourceNavigator.getName());
				assertEquals(CARE_NAVIGATOR_PROVIDER_ID, resourceNavigator.getProviderId());
				assertEquals(CARE_NAVIGATOR_PROVIDER_ID.toString(),
						new JsonMapper().toMap(resourceNavigator).get("providerId"));
			});
		});
	}

	@Test
	public void careNavigatorFixturePopulatesProviderDetailsResponse() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ProviderResource providerResource = app.getInjector().getInstance(ProviderResource.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, ZoneId.of("America/New_York")).build(), () -> {
				ApiResponse response = providerResource.provider("cobalt-care-navigator");
				Map<String, Object> model = (Map<String, Object>) response.model().get();
				ProviderApiResponse provider = (ProviderApiResponse) model.get("provider");

				assertEquals(200, response.status());
				assertEquals(CARE_NAVIGATOR_PROVIDER_ID, provider.getProviderId());
				assertEquals("Cobalt Care Navigator", provider.getName());
				assertEquals("Care Navigator", provider.getTitle());
				assertEquals("Cobalt", provider.getEntity());
				assertEquals("Cobalt Care Navigation", provider.getClinic());
				assertEquals("Care Navigation", provider.getSpecialty());
				assertEquals("Care navigation consultations for provider matching, understanding available care options, and identifying next steps.",
						provider.getDescription());
				assertEquals("https://placehold.co/320x320/png?text=Care+Navigator", provider.getImageUrl());
				assertEquals(Boolean.FALSE, provider.getDefaultImageUrl());
				assertEquals("https://fixtures.cobalt.care/providers/cobalt-care-navigator/bio", provider.getBioUrl());
				assertEquals("https://fixtures.cobalt.care/providers/cobalt-care-navigator", provider.getWebsiteUrl());
				assertTrue(provider.getBio().contains("find a mental health provider"));
				assertTrue(provider.getDetailsHtml().contains("How a Care Navigator can help"));
				assertEquals(List.of("Provider matching", "Care options", "Mental health navigation"), provider.getTags());
				assertEquals("+12155551014", provider.getPhoneNumber());
				assertEquals("(215) 555-1014", provider.getFormattedPhoneNumber());
				assertEquals("Care Navigator", provider.getSupportRolesDescription());
				assertEquals(List.of("No Fee"), provider.getPaymentFundingDescriptions());
				assertEquals(ProviderAppointmentSelectionTypeId.APPOINTMENT_PREDETERMINED,
						provider.getAppointmentSelectionTypeId());
				assertNotNull(provider.getScreeningRequirement());
				assertEquals(1, provider.getLocations().size());
				LocationApiResponse location = provider.getLocations().get(0);
				assertEquals(CARE_NAVIGATOR_PROVIDER_LOCATION_ID, location.getLocationId());
				assertEquals("Cobalt Virtual Care", location.getName());
				assertEquals("Virtual Care", location.getShortName());
				assertNull(location.getAddress());
			});
		});
	}

	@Test
	public void careNavigatorFixtureCompletesAssessmentAndAllowsNativeBooking() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			CareEncounterService careEncounterService = app.getInjector().getInstance(CareEncounterService.class);
			ScreeningService screeningService = app.getInjector().getInstance(ScreeningService.class);
			CareEncounterApiResponseFactory responseFactory = app.getInjector().getInstance(CareEncounterApiResponseFactory.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);

			assertFixtureGraph(database);

			LocalDate bookingDate = nextWeekday(LocalDate.now(ZoneId.of("America/New_York")).plusDays(14));
			LocalTime bookingTime = LocalTime.of(9, 0);
			FindAppointmentBookingRequirementsRequest requirementsRequest = requirementsRequestFor(account, bookingDate,
					bookingTime);

			AppointmentBookingRequirements initialRequirements =
					appointmentService.findAppointmentBookingRequirements(requirementsRequest, account);

			assertEquals(AppointmentBookingRequirementsDestinationId.SCREENING_SESSION,
					initialRequirements.getAppointmentBookingRequirementsDestinationId());
			assertEquals(CARE_NAVIGATOR_SCREENING_FLOW_ID, initialRequirements.getScreeningFlowId());
			assertEquals(Boolean.TRUE, initialRequirements.getScreeningRequired());
			assertEquals(Boolean.FALSE, initialRequirements.getScreeningSatisfied());
			assertNotNull(initialRequirements.getScreeningSession());

			UUID screeningSessionId = initialRequirements.getScreeningSession().getScreeningSessionId();
			List<String> expectedQuestionTexts = List.of(
					"What would you like help navigating?",
					"What type of support would be most useful right now?",
					"How would you prefer your Care Navigator to follow up?",
					"Is there anything else you would like your Care Navigator to know?"
			);
			int questionIndex = 0;
			Optional<ScreeningQuestionContext> questionContext;

			while ((questionContext = screeningService
					.findNextUnansweredScreeningQuestionContextByScreeningSessionId(screeningSessionId)).isPresent()) {
				ScreeningQuestionContext currentQuestionContext = questionContext.get();
				assertEquals(expectedQuestionTexts.get(questionIndex),
						currentQuestionContext.getScreeningQuestion().getQuestionText());

				List<CreateAnswerRequest> answers;
				if (questionIndex == 1) {
					assertEquals(3, currentQuestionContext.getScreeningAnswerOptions().size());
					CreateAnswerRequest providerSupportAnswer = new CreateAnswerRequest();
					providerSupportAnswer.setScreeningAnswerOptionId(
							currentQuestionContext.getScreeningAnswerOptions().get(0).getScreeningAnswerOptionId());
					CreateAnswerRequest benefitsSupportAnswer = new CreateAnswerRequest();
					benefitsSupportAnswer.setScreeningAnswerOptionId(
							currentQuestionContext.getScreeningAnswerOptions().get(1).getScreeningAnswerOptionId());
					answers = List.of(providerSupportAnswer, benefitsSupportAnswer);
				} else {
					CreateAnswerRequest answer = new CreateAnswerRequest();
					answer.setScreeningAnswerOptionId(
							currentQuestionContext.getScreeningAnswerOptions().get(0).getScreeningAnswerOptionId());

					if (questionIndex == 3)
						answer.setText(NAVIGATOR_CONTEXT_FIXTURE_TEXT);

					answers = List.of(answer);
				}

				CreateScreeningAnswersRequest answerRequest = new CreateScreeningAnswersRequest();
				answerRequest.setScreeningQuestionContextId(currentQuestionContext.getScreeningQuestionContextId());
				answerRequest.setCreatedByAccountId(account.getAccountId());
				answerRequest.setAnswers(answers);
				screeningService.createScreeningAnswers(answerRequest);
				++questionIndex;
			}

			assertEquals(expectedQuestionTexts.size(), questionIndex);

			assertNull(screeningService.findNextUnansweredScreeningQuestionContextByScreeningSessionId(screeningSessionId)
					.orElse(null));
			ScreeningSessionDestination destination = screeningService
					.determineDestinationForScreeningSessionId(screeningSessionId)
					.get();
			assertEquals(ScreeningSessionDestinationId.APPOINTMENT_BOOKING_CONFIRMATION,
					destination.getScreeningSessionDestinationId());
			assertEquals(ScreeningSessionDestinationResultId.SUCCESS,
					destination.getScreeningSessionDestinationResultId());

			AppointmentBookingRequirements completedRequirements =
					appointmentService.findAppointmentBookingRequirements(requirementsRequest, account);
			assertEquals(AppointmentBookingRequirementsDestinationId.APPOINTMENT_BOOKING,
					completedRequirements.getAppointmentBookingRequirementsDestinationId());
			assertEquals(Boolean.TRUE, completedRequirements.getScreeningSatisfied());

			String bookingEmailAddress = "care-navigator-booking-test@cobaltinnovations.org";
			database.execute("""
					INSERT INTO account_email_verification (
					  account_email_verification_id, account_id, code, email_address, verified
					) VALUES (?, ?, ?, ?, TRUE)
					""", UUID.randomUUID(), account.getAccountId(), "123456", bookingEmailAddress);

			CreateAppointmentRequest appointmentRequest = new CreateAppointmentRequest();
			appointmentRequest.setAccountId(account.getAccountId());
			appointmentRequest.setCreatedByAcountId(account.getAccountId());
			appointmentRequest.setProviderId(CARE_NAVIGATOR_PROVIDER_ID);
			appointmentRequest.setAppointmentTypeId(CARE_NAVIGATOR_APPOINTMENT_TYPE_ID);
			appointmentRequest.setDate(bookingDate);
			appointmentRequest.setTime(bookingTime);
			appointmentRequest.setFirstName("Care");
			appointmentRequest.setLastName("Navigator Booking Test");
			appointmentRequest.setEmailAddress(bookingEmailAddress);
			appointmentRequest.setPhoneNumber("+12155550123");
			appointmentRequest.setBookingExperienceId(BookingExperienceId.V2);
			appointmentRequest.setAppointmentModalityId(ProviderAppointmentModalityId.VIRTUAL);

			UUID appointmentId = appointmentService.createAppointment(appointmentRequest);
			assertNotNull(appointmentId);
			Appointment appointment = appointmentService.findAppointmentById(appointmentId).get();
			assertEquals(screeningSessionId, appointment.getScreeningSessionId());
			assertEquals(CARE_NAVIGATOR_ACCOUNT_ID, database.queryForObject("""
					SELECT care_encounter.care_navigator_account_id
					FROM appointment
					JOIN care_encounter ON care_encounter.care_encounter_id=appointment.care_encounter_id
					WHERE appointment.appointment_id=?
					""", UUID.class, appointmentId).get());

			CareEncounter careEncounter = careEncounterService.findCareEncounterByIdForInstitutionId(
					appointment.getCareEncounterId(), InstitutionId.COBALT).get();
			CareEncounterApiResponse encounterResponse = responseFactory.create(careEncounter);
			assertEquals(appointmentId, encounterResponse.getAppointment().getAppointmentId());
			assertEquals(screeningSessionId, encounterResponse.getAppointment().getScreeningSessionId());
			ScreeningSessionResult screeningSessionResult = encounterResponse.getAppointment().getScreeningSessionResult();
			assertNotNull(screeningSessionResult);
			assertEquals(1, screeningSessionResult.getScreeningSessionScreeningResults().size());
			List<ScreeningSessionResult.ScreeningQuestionResult> questionResults = screeningSessionResult
					.getScreeningSessionScreeningResults().get(0).getScreeningQuestionResults();
			assertEquals(expectedQuestionTexts,
					questionResults.stream().map(ScreeningSessionResult.ScreeningQuestionResult::getScreeningQuestionText)
							.toList());
			assertEquals(List.of("Finding a mental health provider"), questionResults.get(0)
					.getScreeningAnswerResults().stream()
					.map(ScreeningSessionResult.ScreeningAnswerResult::getAnswerOptionText).toList());
			assertEquals(List.of("Finding an in-network provider", "Understanding costs and benefits"),
					questionResults.get(1).getScreeningAnswerResults().stream()
							.map(ScreeningSessionResult.ScreeningAnswerResult::getAnswerOptionText).toList());
			assertEquals("Email", questionResults.get(2).getScreeningAnswerResults().get(0).getAnswerOptionText());
			assertEquals(NAVIGATOR_CONTEXT_FIXTURE_TEXT,
					questionResults.get(3).getScreeningAnswerResults().get(0).getText());
			Map<String, Object> serializedEncounter = new JsonMapper().toMap(encounterResponse);
			Map<?, ?> serializedAppointment = (Map<?, ?>) serializedEncounter.get("appointment");
			assertTrue(serializedAppointment.containsKey("screeningSessionId"));
			assertTrue(serializedAppointment.containsKey("screeningSessionResult"));

			UpdateAppointmentRequest updateAppointmentRequest = new UpdateAppointmentRequest();
			updateAppointmentRequest.setBookingExperienceId(BookingExperienceId.V2);
			updateAppointmentRequest.setAppointmentId(appointmentId);
			updateAppointmentRequest.setAccountId(account.getAccountId());
			updateAppointmentRequest.setCreatedByAcountId(account.getAccountId());
			updateAppointmentRequest.setProviderId(CARE_NAVIGATOR_PROVIDER_ID);
			updateAppointmentRequest.setAppointmentTypeId(CARE_NAVIGATOR_APPOINTMENT_TYPE_ID);
			updateAppointmentRequest.setDate(nextWeekday(bookingDate.plusDays(1)));
			updateAppointmentRequest.setTime(bookingTime);
			UUID replacementAppointmentId = appointmentService.rescheduleAppointment(updateAppointmentRequest);
			assertEquals(screeningSessionId,
					appointmentService.findAppointmentById(replacementAppointmentId).get().getScreeningSessionId());

			database.execute("UPDATE appointment SET screening_session_id=NULL WHERE appointment_id=?",
					replacementAppointmentId);
			database.execute("UPDATE appointment SET created=created + INTERVAL '1 second' WHERE appointment_id=?",
					replacementAppointmentId);

			CareEncounterApiResponse appointmentScopedResponse = responseFactory.create(careEncounterService
					.findCareEncounterByIdForInstitutionId(appointment.getCareEncounterId(), InstitutionId.COBALT).get());
			assertEquals(replacementAppointmentId, appointmentScopedResponse.getAppointment().getAppointmentId());
			assertNull(appointmentScopedResponse.getAppointment().getScreeningSessionId());
			assertNull(appointmentScopedResponse.getAppointment().getScreeningSessionResult());
			assertEquals(1, appointmentScopedResponse.getAppointmentHistory().size());
			assertEquals(appointmentId, appointmentScopedResponse.getAppointmentHistory().get(0).getAppointmentId());
			assertEquals(screeningSessionId,
					appointmentScopedResponse.getAppointmentHistory().get(0).getScreeningSessionId());
			assertNotNull(appointmentScopedResponse.getAppointmentHistory().get(0).getScreeningSessionResult());
		});
	}

	@Test
	public void databaseRejectsSecondActiveAttendedAndTerminalEncounterBookings() {
		RuntimeException secondActiveException = assertThrows(RuntimeException.class, () ->
				IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
					Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
					cloneAsActiveAppointment(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID, UUID.randomUUID(), 20, null);
				}));
		assertTrue(exceptionContains(secondActiveException, "already has an active appointment"));

		RuntimeException attendedException = assertThrows(RuntimeException.class, () ->
				IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
					Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
					UUID careEncounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID);
					cloneAsActiveAppointment(database, CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID, UUID.randomUUID(), 21,
							careEncounterId);
				}));
		assertTrue(exceptionContains(attendedException, "must be closed before another appointment"));

		RuntimeException terminalException = assertThrows(RuntimeException.class, () ->
				IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
					Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
					UUID careEncounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_PATIENT_CANCELED_APPOINTMENT_ID);
					cloneAsActiveAppointment(database, CARE_NAVIGATOR_PATIENT_CANCELED_APPOINTMENT_ID, UUID.randomUUID(), 22,
							careEncounterId);
				}));
		assertTrue(exceptionContains(terminalException, "cannot be attached to a terminal encounter"));
	}

	@Test
	public void cancellationActorsControlAutomaticEncounterClosure() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			UUID careEncounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);

			cancelAppointment(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID, CARE_NAVIGATOR_ACCOUNT_ID, false);
			assertEquals("OPEN", careEncounterStatus(database, careEncounterId));

			UUID externallyCanceledAppointmentId = UUID.randomUUID();
			cloneAsActiveAppointment(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID, externallyCanceledAppointmentId, 23, null);
			assertEquals(careEncounterId, careEncounterIdForAppointment(database, externallyCanceledAppointmentId));
			cancelAppointment(database, externallyCanceledAppointmentId, null, false);
			assertEquals("OPEN", careEncounterStatus(database, careEncounterId));

			UUID missedHistoryAppointmentId = UUID.randomUUID();
			cloneAsActiveAppointment(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID, missedHistoryAppointmentId, 24, null);
			database.execute("UPDATE appointment SET attendance_status_id='MISSED' WHERE appointment_id=?",
					missedHistoryAppointmentId);

			UUID patientCanceledAppointmentId = UUID.randomUUID();
			cloneAsActiveAppointment(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID, patientCanceledAppointmentId, 25, null);
			cancelAppointment(database, missedHistoryAppointmentId, CARE_NAVIGATOR_ACTIVE_FIXTURE_PATIENT_ID, false);
			assertEquals("OPEN", careEncounterStatus(database, careEncounterId));
			cancelAppointment(database, patientCanceledAppointmentId, CARE_NAVIGATOR_ACTIVE_FIXTURE_PATIENT_ID, false);
			assertEquals("CLOSED", careEncounterStatus(database, careEncounterId));
			assertEquals(CARE_NAVIGATOR_ACTIVE_FIXTURE_PATIENT_ID, database.queryForObject("""
					SELECT closed_by_account_id
					FROM care_encounter
					WHERE care_encounter_id=?
					""", UUID.class, careEncounterId).get());
		});
	}

	@Test
	public void navigatorCanCancelActiveAppointmentWithUserSubmittedReason() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CareEncounterService careEncounterService = app.getInjector().getInstance(CareEncounterService.class);
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			CareEncounterApiResponseFactory responseFactory = app.getInjector().getInstance(CareEncounterApiResponseFactory.class);
			UUID careEncounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);
			CancelCareEncounterAppointmentRequest request = new CancelCareEncounterAppointmentRequest();
			Account navigator = accountService.findAccountById(CARE_NAVIGATOR_ACCOUNT_ID).get();

			resetAppointmentAsActive(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);

			request.setCancellationReason("   ");
			assertThrows(ValidationException.class, () -> careEncounterService.cancelCareEncounterAppointment(
					careEncounterId, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID, InstitutionId.COBALT,
					CARE_NAVIGATOR_ACCOUNT_ID, request));
			assertFalse(appointmentService.findAppointmentById(CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID).get().getCanceled());

			request.setCancellationReason("x".repeat(2_001));
			assertThrows(ValidationException.class, () -> careEncounterService.cancelCareEncounterAppointment(
					careEncounterId, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID, InstitutionId.COBALT,
					CARE_NAVIGATOR_ACCOUNT_ID, request));

			request.setCancellationReason("  Patient requested a different appointment time.  ");
			CareEncounter careEncounter = careEncounterService.cancelCareEncounterAppointment(
					careEncounterId, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID, InstitutionId.COBALT,
					CARE_NAVIGATOR_ACCOUNT_ID, request);
			Appointment appointment = appointmentService.findAppointmentById(CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID).get();

			assertTrue(appointment.getCanceled());
			assertEquals(AttendanceStatusId.CANCELED, appointment.getAttendanceStatusId());
			assertNotNull(appointment.getCanceledAt());
			assertEquals(CARE_NAVIGATOR_ACCOUNT_ID, appointment.getCanceledByAccountId());
			assertFalse(appointment.getCanceledForReschedule());
			assertEquals("Patient requested a different appointment time.", appointment.getCancellationReason());
			assertEquals(CareEncounterStatusId.OPEN, careEncounter.getCareEncounterStatusId());
			assertEquals(CARE_NAVIGATOR_ACCOUNT_ID, careEncounter.getLastUpdatedByAccountId());

			currentContextExecutor.execute(new CurrentContext.Builder(navigator, Locale.US,
					ZoneId.of("America/New_York")).build(), () -> {
				CareEncounterApiResponse response = responseFactory.create(careEncounter);
				assertEquals(1, response.getAppointmentHistory().size());
				assertEquals(CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID,
						response.getAppointmentHistory().get(0).getAppointmentId());
				assertTrue(response.getAppointmentHistory().get(0).getCanceled());
				assertEquals(AttendanceStatusId.CANCELED,
						response.getAppointmentHistory().get(0).getAttendanceStatusId());
				assertEquals("Patient requested a different appointment time.",
						response.getAppointmentHistory().get(0).getCancellationReason());
			});
		});
	}

	@Test
	public void missedRebookingManualClosureAndPostClosureBookingUseExpectedEncounters() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CareEncounterService careEncounterService = app.getInjector().getInstance(CareEncounterService.class);
			UUID originalCareEncounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_REBOOKED_APPOINTMENT_ID);

			cancelAppointment(database, CARE_NAVIGATOR_REBOOKED_APPOINTMENT_ID, CARE_NAVIGATOR_ACCOUNT_ID, false);
			UUID missedAppointmentId = UUID.randomUUID();
			cloneAsActiveAppointment(database, CARE_NAVIGATOR_REBOOKED_APPOINTMENT_ID, missedAppointmentId, 25, null);
			database.execute("UPDATE appointment SET attendance_status_id='MISSED' WHERE appointment_id=?", missedAppointmentId);

			UUID attendedAppointmentId = UUID.randomUUID();
			cloneAsActiveAppointment(database, missedAppointmentId, attendedAppointmentId, 1, null);
			assertEquals(originalCareEncounterId, careEncounterIdForAppointment(database, attendedAppointmentId));
			database.execute("UPDATE appointment SET attendance_status_id='ATTENDED' WHERE appointment_id=?", attendedAppointmentId);
			assertEquals("OPEN", careEncounterStatus(database, originalCareEncounterId));

			CareEncounter closedEncounter = careEncounterService.closeCareEncounter(originalCareEncounterId,
					InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID);
			assertEquals(CareEncounterStatusId.CLOSED, closedEncounter.getCareEncounterStatusId());
			assertEquals(CARE_NAVIGATOR_ACCOUNT_ID, closedEncounter.getClosedByAccountId());
			assertEquals(4, careEncounterService.findAppointmentsByCareEncounterIdForInstitutionId(
					originalCareEncounterId, InstitutionId.COBALT).size());

			UUID postClosureAppointmentId = UUID.randomUUID();
			cloneAsActiveAppointment(database, attendedAppointmentId, postClosureAppointmentId, 1, null);
			UUID postClosureCareEncounterId = careEncounterIdForAppointment(database, postClosureAppointmentId);
			assertNotEquals(originalCareEncounterId, postClosureCareEncounterId);
			assertEquals(CARE_NAVIGATOR_ACCOUNT_ID, database.queryForObject("""
					SELECT care_navigator_account_id
					FROM care_encounter
					WHERE care_encounter_id=?
					""", UUID.class, postClosureCareEncounterId).get());
		});
	}

	@Test
	public void assignmentOrderingEligibilityAndAdministrativeLifecycleAreEnforced() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CareEncounterService careEncounterService = app.getInjector().getInstance(CareEncounterService.class);
			UUID activeCareEncounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);
			CancelCareEncounterRequest activeCancelRequest = new CancelCareEncounterRequest();
			activeCancelRequest.setCareEncounterCancellationReasonId(CareEncounterCancellationReasonId.NO_LONGER_NEEDED);
			assertThrows(ValidationException.class, () -> careEncounterService.closeCareEncounter(activeCareEncounterId,
					InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID));
			assertThrows(ValidationException.class, () -> careEncounterService.cancelCareEncounter(activeCareEncounterId,
					InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID, activeCancelRequest));
			assertThrows(ValidationException.class, () -> careEncounterService.deleteCareEncounter(activeCareEncounterId,
					InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID));
			UUID secondaryNavigatorAccountId = database.queryForObject("""
					SELECT account.account_id
					FROM account
					JOIN account_capability ON account_capability.account_id=account.account_id
					WHERE account.institution_id='COBALT'
					AND LOWER(account.email_address)=LOWER('admin@cobaltinnovations.org')
					AND account_capability.account_capability_type_id='NAVIGATOR'
					""", UUID.class).get();

			database.execute("""
					INSERT INTO care_navigator_provider_account (provider_id, account_id, display_order)
					VALUES (?, ?, 2)
					""", CARE_NAVIGATOR_PROVIDER_ID, secondaryNavigatorAccountId);
			assertEquals(CARE_NAVIGATOR_ACCOUNT_ID, database.queryForObject(
					"SELECT first_care_navigator_account_for_provider(?)", UUID.class, CARE_NAVIGATOR_PROVIDER_ID).get());

			database.execute("UPDATE care_navigator_provider_account SET display_order=3 WHERE provider_id=? AND account_id=?",
					CARE_NAVIGATOR_PROVIDER_ID, CARE_NAVIGATOR_ACCOUNT_ID);
			database.execute("UPDATE care_navigator_provider_account SET display_order=1 WHERE provider_id=? AND account_id=?",
					CARE_NAVIGATOR_PROVIDER_ID, secondaryNavigatorAccountId);
			assertEquals(secondaryNavigatorAccountId, database.queryForObject(
					"SELECT first_care_navigator_account_for_provider(?)", UUID.class, CARE_NAVIGATOR_PROVIDER_ID).get());

			database.execute("UPDATE account SET active=FALSE WHERE account_id=?", secondaryNavigatorAccountId);
			assertEquals(CARE_NAVIGATOR_ACCOUNT_ID, database.queryForObject(
					"SELECT first_care_navigator_account_for_provider(?)", UUID.class, CARE_NAVIGATOR_PROVIDER_ID).get());
			UUID attendedCareEncounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID);
			assertThrows(ValidationException.class, () -> careEncounterService.assignCareEncounter(attendedCareEncounterId,
					InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID, secondaryNavigatorAccountId));
			database.execute("UPDATE account SET active=TRUE WHERE account_id=?", secondaryNavigatorAccountId);

			CareEncounter assignedEncounter = careEncounterService.assignCareEncounter(attendedCareEncounterId,
					InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID, secondaryNavigatorAccountId);
			assertEquals(secondaryNavigatorAccountId, assignedEncounter.getCareNavigatorAccountId());

			CancelCareEncounterRequest cancelRequest = new CancelCareEncounterRequest();
			cancelRequest.setCareEncounterCancellationReasonId(CareEncounterCancellationReasonId.NO_LONGER_NEEDED);
			CareEncounter canceledEncounter = careEncounterService.cancelCareEncounter(attendedCareEncounterId,
					InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID, cancelRequest);
			assertEquals(CareEncounterStatusId.CANCELED, canceledEncounter.getCareEncounterStatusId());
			assertFalse(database.queryForObject("SELECT canceled FROM appointment WHERE appointment_id=?", Boolean.class,
					CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID).get());
			assertTrue(careEncounterService.deleteCareEncounter(attendedCareEncounterId, InstitutionId.COBALT,
					CARE_NAVIGATOR_ACCOUNT_ID));
		});
	}

	@Test
	public void navigatorCanUpdateEncounterEmailWithoutChangingAppointmentEmail() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CareEncounterService careEncounterService = app.getInjector().getInstance(CareEncounterService.class);
			UUID careEncounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);
			String appointmentEmailAddress = database.queryForObject("""
					SELECT email_address
					FROM appointment
					WHERE appointment_id=?
					""", String.class, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID).get();

			CareEncounter originalCareEncounter = careEncounterService.findCareEncounterByIdForInstitutionId(
					careEncounterId, InstitutionId.COBALT).get();
			assertEquals(appointmentEmailAddress, originalCareEncounter.getEmailAddress());

			UpdateCareEncounterRequest request = new UpdateCareEncounterRequest();
			request.setCareEncounterId(careEncounterId);
			request.setInstitutionId(InstitutionId.COBALT);
			request.setAccountId(CARE_NAVIGATOR_ACCOUNT_ID);
			request.setEmailAddress("  Navigator.Contact@Example.com  ");
			request.setNotes(originalCareEncounter.getNotes());

			CareEncounter updatedCareEncounter = careEncounterService.updateCareEncounter(request);
			assertEquals("navigator.contact@example.com", updatedCareEncounter.getEmailAddress());
			assertEquals(appointmentEmailAddress, database.queryForObject("""
					SELECT email_address
					FROM appointment
					WHERE appointment_id=?
					""", String.class, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID).get());

			request.setEmailAddress("invalid-email-address");
			assertThrows(ValidationException.class, () -> careEncounterService.updateCareEncounter(request));
			assertEquals("navigator.contact@example.com", database.queryForObject("""
					SELECT email_address
					FROM care_encounter
					WHERE care_encounter_id=?
					""", String.class, careEncounterId).get());
		});
	}

	@Test
	public void encounterResponsesUseLatestAppointmentAndIncludeInactiveAppointmentsInHistory() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CareEncounterService careEncounterService = app.getInjector().getInstance(CareEncounterService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			CareEncounterApiResponseFactory responseFactory = app.getInjector().getInstance(CareEncounterApiResponseFactory.class);
			CareEncounterListApiResponseFactory listResponseFactory = app.getInjector()
					.getInstance(CareEncounterListApiResponseFactory.class);
			Account navigator = accountService.findAccountById(CARE_NAVIGATOR_ACCOUNT_ID).get();
			resetAppointmentAsActive(database, CARE_NAVIGATOR_REBOOKED_APPOINTMENT_ID);
			resetAppointmentAsActive(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);
			assertEquals(1, database.execute("""
					UPDATE care_encounter
					SET care_encounter_status_id='OPEN',
						closed_at=NULL,
						closed_by_account_id=NULL,
						canceled_by_account_id=NULL,
						care_encounter_cancellation_reason_id=NULL,
						care_encounter_cancellation_reason_other_text=NULL
					WHERE care_encounter_id=(
						SELECT care_encounter_id
						FROM appointment
						WHERE appointment_id=?
					)
					""", CARE_NAVIGATOR_REBOOKED_APPOINTMENT_ID));
			UUID activeCareEncounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_REBOOKED_APPOINTMENT_ID);
			CareEncounter activeCareEncounter = careEncounterService.findCareEncounterByIdForInstitutionId(
					activeCareEncounterId, InstitutionId.COBALT).get();

			currentContextExecutor.execute(new CurrentContext.Builder(navigator, Locale.US,
					ZoneId.of("America/New_York")).build(), () -> {
				CareEncounterApiResponse response = responseFactory.create(activeCareEncounter);
				assertEquals(CARE_NAVIGATOR_REBOOKED_APPOINTMENT_ID, response.getAppointmentId());
				assertEquals(CARE_NAVIGATOR_REBOOKED_APPOINTMENT_ID,
						response.getAppointment().getAppointmentId());
				assertEquals(1, response.getAppointmentHistory().size());
				assertEquals(CARE_NAVIGATOR_CANCELED_APPOINTMENT_ID,
						response.getAppointmentHistory().get(0).getAppointmentId());
				assertTrue(response.getAppointmentHistory().get(0).getCanceled());
				assertEquals(activeCareEncounterId, response.getAppointment().getCareEncounterId());
				assertEquals(CARE_NAVIGATOR_ACCOUNT_ID, response.getCareNavigatorAccountId());
				assertNotNull(response.getCareNavigatorDisplayName());
				assertEquals(activeCareEncounter.getEmailAddress(), response.getEmailAddress());
				Map<String, Object> serializedDetail = new JsonMapper().toMap(response);
				assertTrue(serializedDetail.containsKey("appointment"));
				assertTrue(serializedDetail.containsKey("appointmentHistory"));
				assertTrue(serializedDetail.containsKey("emailAddress"));
				assertFalse(serializedDetail.containsKey("activeAppointment"));
				assertFalse(serializedDetail.containsKey("activeAppointmentId"));
				assertFalse(serializedDetail.containsKey("appointments"));

				CareEncounterListApiResponse listResponse = listResponseFactory.create(activeCareEncounter);
				assertEquals(CARE_NAVIGATOR_REBOOKED_APPOINTMENT_ID, listResponse.getAppointmentId());
				assertEquals(CARE_NAVIGATOR_REBOOKED_APPOINTMENT_ID,
						listResponse.getAppointment().getAppointmentId());
				assertEquals(CARE_NAVIGATOR_PROVIDER_ID, listResponse.getAppointment().getProviderId());
				Map<String, Object> serializedListItem = new JsonMapper().toMap(listResponse);
				Map<?, ?> serializedListAppointment = (Map<?, ?>) serializedListItem.get("appointment");
				assertFalse(serializedListItem.containsKey("appointmentHistory"));
				assertFalse(serializedListItem.containsKey("screeningSessionResult"));
				assertFalse(serializedListAppointment.containsKey("account"));
				assertFalse(serializedListAppointment.containsKey("appointmentReason"));
				assertFalse(serializedListAppointment.containsKey("emailAddress"));
				assertFalse(serializedListAppointment.containsKey("screeningSessionResult"));
			});

			UUID attendedCareEncounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID);
			CareEncounter attendedCareEncounter = careEncounterService.findCareEncounterByIdForInstitutionId(
					attendedCareEncounterId, InstitutionId.COBALT).get();
			currentContextExecutor.execute(new CurrentContext.Builder(navigator, Locale.US,
					ZoneId.of("America/New_York")).build(), () -> {
				CareEncounterApiResponse response = responseFactory.create(attendedCareEncounter);
				assertEquals(CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID, response.getAppointmentId());
				assertEquals(CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID,
						response.getAppointment().getAppointmentId());
				assertEquals(1, response.getAppointmentHistory().size());
				assertEquals(CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID,
						response.getAppointmentHistory().get(0).getAppointmentId());
				assertEquals(AttendanceStatusId.ATTENDED,
						response.getAppointmentHistory().get(0).getAttendanceStatusId());
			});

			UUID upcomingCareEncounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);
			CareEncounter upcomingCareEncounter = careEncounterService.findCareEncounterByIdForInstitutionId(
					upcomingCareEncounterId, InstitutionId.COBALT).get();
			currentContextExecutor.execute(new CurrentContext.Builder(navigator, Locale.US,
					ZoneId.of("America/New_York")).build(), () -> {
				CareEncounterApiResponse response = responseFactory.create(upcomingCareEncounter);
				assertEquals(CARE_NAVIGATOR_UPCOMING_SCREENING_SESSION_ID,
						response.getAppointment().getScreeningSessionId());
				List<ScreeningSessionResult.ScreeningQuestionResult> questionResults = response.getAppointment()
						.getScreeningSessionResult().getScreeningSessionScreeningResults().get(0)
						.getScreeningQuestionResults();
				assertEquals(4, questionResults.size());
				assertEquals(NAVIGATOR_CONTEXT_FIXTURE_TEXT,
						questionResults.get(3).getScreeningAnswerResults().get(0).getText());

				CareEncounterListApiResponse listResponse = listResponseFactory.create(upcomingCareEncounter);
				Map<String, Object> serializedListItem = new JsonMapper().toMap(listResponse);
				assertFalse(serializedListItem.containsKey("screeningSessionResult"));
				assertFalse(((Map<?, ?>) serializedListItem.get("appointment"))
						.containsKey("screeningSessionResult"));
			});
		});
	}

	@Test
	public void encounterListAssignmentScopeDefaultsToAllAndFiltersSelfAndUnassigned() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CareEncounterService careEncounterService = app.getInjector().getInstance(CareEncounterService.class);
			UUID careEncounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_REBOOKED_APPOINTMENT_ID);

			FindCareEncountersRequest defaultRequest = new FindCareEncountersRequest();
			defaultRequest.setInstitutionId(InstitutionId.COBALT);
			defaultRequest.setCareNavigatorAccountId(CARE_NAVIGATOR_ACCOUNT_ID);
			assertEquals(CareEncounterAssignmentScopeId.ALL, defaultRequest.getCareEncounterAssignmentScopeId());
			assertTrue(careEncounterService.findCareEncounters(defaultRequest).getResults().stream()
					.anyMatch(careEncounter -> careEncounterId.equals(careEncounter.getCareEncounterId())));

			assertEquals(1, database.execute("""
					UPDATE care_encounter
					SET care_navigator_account_id=NULL
					WHERE care_encounter_id=?
					""", careEncounterId));

			FindCareEncountersRequest selfRequest = new FindCareEncountersRequest();
			selfRequest.setInstitutionId(InstitutionId.COBALT);
			selfRequest.setCareNavigatorAccountId(CARE_NAVIGATOR_ACCOUNT_ID);
			selfRequest.setCareEncounterAssignmentScopeId(CareEncounterAssignmentScopeId.SELF);
			FindResult<CareEncounter> selfResult = careEncounterService.findCareEncounters(selfRequest);
			assertFalse(selfResult.getResults().stream()
					.anyMatch(careEncounter -> careEncounterId.equals(careEncounter.getCareEncounterId())));
			assertTrue(selfResult.getResults().stream()
					.allMatch(careEncounter -> CARE_NAVIGATOR_ACCOUNT_ID.equals(careEncounter.getCareNavigatorAccountId())));

			FindCareEncountersRequest unassignedRequest = new FindCareEncountersRequest();
			unassignedRequest.setInstitutionId(InstitutionId.COBALT);
			unassignedRequest.setCareNavigatorAccountId(CARE_NAVIGATOR_ACCOUNT_ID);
			unassignedRequest.setCareEncounterAssignmentScopeId(CareEncounterAssignmentScopeId.UNASSIGNED);
			FindResult<CareEncounter> unassignedResult = careEncounterService.findCareEncounters(unassignedRequest);
			assertTrue(unassignedResult.getResults().stream()
					.anyMatch(careEncounter -> careEncounterId.equals(careEncounter.getCareEncounterId())));
			assertTrue(unassignedResult.getResults().stream()
					.allMatch(careEncounter -> careEncounter.getCareNavigatorAccountId() == null));

			FindResult<CareEncounter> allResult = careEncounterService.findCareEncounters(defaultRequest);
			assertTrue(allResult.getResults().stream()
					.anyMatch(careEncounter -> careEncounterId.equals(careEncounter.getCareEncounterId())));
		});
	}

	@Test
	public void mappingRejectsAccountsWithoutNavigatorEligibility() {
		RuntimeException exception = assertThrows(RuntimeException.class, () ->
				IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
					Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
					database.execute("""
							INSERT INTO care_navigator_provider_account (provider_id, account_id, display_order)
							VALUES (?, ?, 99)
							""", CARE_NAVIGATOR_PROVIDER_ID, CARE_NAVIGATOR_ACTIVE_FIXTURE_PATIENT_ID);
				}));
		assertTrue(exceptionContains(exception, "active Navigator-capable Administrator or Provider account"));
	}

	protected void assertFixtureGraph(Database database) {
		assertEquals(CARE_NAVIGATOR_PROVIDER_ID, database.queryForObject("""
				SELECT provider_id
				FROM account
				WHERE account_id=?
				AND email_address='care-navigator@cobaltinnovations.org'
				AND role_id='ADMINISTRATOR'
				""", UUID.class, CARE_NAVIGATOR_ACCOUNT_ID).get());
		assertEquals(Long.valueOf(1L), database.queryForObject("""
				SELECT COUNT(*)
				FROM account_capability
				WHERE account_id=?
				AND account_capability_type_id='NAVIGATOR'
				""", Long.class, CARE_NAVIGATOR_ACCOUNT_ID).get());
		assertEquals(Long.valueOf(1L), database.queryForObject("""
				SELECT COUNT(*)
				FROM account
				JOIN account_capability ON account_capability.account_id=account.account_id
				WHERE account.institution_id='COBALT'
				AND account.role_id='ADMINISTRATOR'
				AND LOWER(account.email_address)=LOWER('admin@cobaltinnovations.org')
				AND account_capability.account_capability_type_id='NAVIGATOR'
				""", Long.class).get());
		assertEquals(Long.valueOf(1L), database.queryForObject("""
				SELECT COUNT(*)
				FROM provider_support_role
				WHERE provider_id=?
				AND support_role_id='CARE_NAVIGATOR'
				""", Long.class, CARE_NAVIGATOR_PROVIDER_ID).get());
		assertEquals(Long.valueOf(1L), database.queryForObject("""
				SELECT COUNT(*)
				FROM care_navigator_provider_account
				WHERE provider_id=?
				AND account_id=?
				AND display_order=1
				""", Long.class, CARE_NAVIGATOR_PROVIDER_ID, CARE_NAVIGATOR_ACCOUNT_ID).get());
		assertEquals(Long.valueOf(2L), database.queryForObject("""
				SELECT COUNT(*)
				FROM appointment
				WHERE care_encounter_id=(
					SELECT care_encounter_id
					FROM care_encounter
					WHERE account_id=?
					AND care_encounter_status_id='OPEN'
				)
				""", Long.class, CARE_NAVIGATOR_ACCOUNT_FIXTURE_PATIENT_ID).get());
		assertEquals(CARE_NAVIGATOR_ACCOUNT_ID, database.queryForObject("""
				SELECT care_navigator_account_id
				FROM care_encounter
				WHERE account_id=?
				AND care_encounter_status_id='OPEN'
				""", UUID.class, CARE_NAVIGATOR_ACCOUNT_FIXTURE_PATIENT_ID).get());
		assertEquals(Long.valueOf(1L), database.queryForObject("""
				SELECT COUNT(*)
				FROM care_encounter
				WHERE account_id=?
				AND care_encounter_status_id='CLOSED'
				AND closed_by_account_id=?
				""", Long.class, CARE_NAVIGATOR_PATIENT_CANCELED_FIXTURE_PATIENT_ID,
				CARE_NAVIGATOR_PATIENT_CANCELED_FIXTURE_PATIENT_ID).get());
		assertEquals(CARE_NAVIGATOR_SCREENING_FLOW_ID, database.queryForObject("""
				SELECT screening_flow_id
				FROM appointment_type
				WHERE appointment_type_id=?
				AND name='Care Navigation Consultation'
				AND duration_in_minutes=30
				""", UUID.class, CARE_NAVIGATOR_APPOINTMENT_TYPE_ID).get());
		assertEquals(Long.valueOf(1L), database.queryForObject("""
				SELECT COUNT(*)
				FROM logical_availability la
				JOIN logical_availability_appointment_type laat
				  ON laat.logical_availability_id=la.logical_availability_id
				WHERE la.provider_id=?
				AND laat.appointment_type_id=?
				AND la.recur_monday=TRUE
				AND la.recur_tuesday=TRUE
				AND la.recur_wednesday=TRUE
				AND la.recur_thursday=TRUE
				AND la.recur_friday=TRUE
				""", Long.class, CARE_NAVIGATOR_PROVIDER_ID, CARE_NAVIGATOR_APPOINTMENT_TYPE_ID).get());
	}

	protected void cloneAsActiveAppointment(Database database,
														 UUID sourceAppointmentId,
														 UUID appointmentId,
														 int daysAfterSource,
														 UUID careEncounterId) {
		assertEquals(1, database.execute("""
				INSERT INTO appointment (
					appointment_id,
					provider_id,
					account_id,
					care_encounter_id,
					screening_session_id,
					created_by_account_id,
					first_name,
					last_name,
					email_address,
					contact_phone_number,
					appointment_type_id,
					title,
					start_time,
					end_time,
					duration_in_minutes,
					time_zone,
					videoconference_url,
					videoconference_platform_id,
					scheduling_system_id,
					appointment_reason_id,
					attendance_status_id,
					canceled,
					canceled_at,
					canceled_by_account_id,
					canceled_for_reschedule,
					rescheduled_appointment_id
				)
				SELECT
					?,
					provider_id,
					account_id,
					?,
					screening_session_id,
					created_by_account_id,
					first_name,
					last_name,
					email_address,
					contact_phone_number,
					appointment_type_id,
					title,
					start_time + (CAST(? AS INTEGER) * INTERVAL '1 day'),
					end_time + (CAST(? AS INTEGER) * INTERVAL '1 day'),
					duration_in_minutes,
					time_zone,
					videoconference_url,
					videoconference_platform_id,
					scheduling_system_id,
					appointment_reason_id,
					'UNKNOWN',
					FALSE,
					NULL,
					NULL,
					FALSE,
					NULL
				FROM appointment
				WHERE appointment_id=?
				""", appointmentId, careEncounterId, daysAfterSource, daysAfterSource, sourceAppointmentId));
	}

	protected void cancelAppointment(Database database,
								 UUID appointmentId,
								 UUID canceledByAccountId,
											 boolean canceledForReschedule) {
		assertEquals(1, database.execute("""
				UPDATE appointment
				SET canceled=TRUE,
					attendance_status_id='CANCELED',
					canceled_at=NOW(),
					canceled_by_account_id=?,
					canceled_for_reschedule=?
				WHERE appointment_id=?
				""", canceledByAccountId, canceledForReschedule, appointmentId));
	}

	protected void resetAppointmentAsActive(Database database, UUID appointmentId) {
		assertEquals(1, database.execute("""
				UPDATE appointment
				SET canceled=FALSE,
					attendance_status_id='UNKNOWN',
					canceled_at=NULL,
					canceled_by_account_id=NULL,
					canceled_for_reschedule=FALSE,
					rescheduled_appointment_id=NULL,
					appointment_cancelation_reason_id='UNSPECIFIED',
					cancellation_reason=NULL
				WHERE appointment_id=?
				""", appointmentId));
	}

	protected UUID careEncounterIdForAppointment(Database database, UUID appointmentId) {
		return database.queryForObject("SELECT care_encounter_id FROM appointment WHERE appointment_id=?", UUID.class,
				appointmentId).get();
	}

	protected String careEncounterStatus(Database database, UUID careEncounterId) {
		return database.queryForObject("SELECT care_encounter_status_id FROM care_encounter WHERE care_encounter_id=?",
				String.class, careEncounterId).get();
	}

	protected boolean exceptionContains(Throwable throwable, String expectedText) {
		for (Throwable current = throwable; current != null; current = current.getCause())
			if (current.getMessage() != null && current.getMessage().contains(expectedText))
				return true;

		return false;
	}

	protected FindAppointmentBookingRequirementsRequest requirementsRequestFor(Account account,
																					 LocalDate bookingDate,
																					 LocalTime bookingTime) {
		FindAppointmentBookingRequirementsRequest request = new FindAppointmentBookingRequirementsRequest();
		request.setAccountId(account.getAccountId());
		request.setProviderId(CARE_NAVIGATOR_PROVIDER_ID);
		request.setAppointmentTypeId(CARE_NAVIGATOR_APPOINTMENT_TYPE_ID);
		request.setAppointmentSelectionTypeId(ProviderAppointmentSelectionTypeId.APPOINTMENT_PREDETERMINED);
		request.setAppointmentModalityId(ProviderAppointmentModalityId.VIRTUAL);
		request.setDate(bookingDate);
		request.setTime(bookingTime);
		return request;
	}

	protected LocalDate nextWeekday(LocalDate date) {
		while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY)
			date = date.plusDays(1);

		return date;
	}
}
