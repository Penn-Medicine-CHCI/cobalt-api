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
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageSerializer;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.api.request.CancelAppointmentRequest;
import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.api.request.CreateAppointmentRequest;
import com.cobaltplatform.api.model.api.request.CreateAppointmentRequest.BookingExperienceId;
import com.cobaltplatform.api.model.api.request.CancelCareEncounterAppointmentRequest;
import com.cobaltplatform.api.model.api.request.CreateCareEncounterNoteRequest;
import com.cobaltplatform.api.model.api.request.CreateCareEncounterScheduledMessageRequest;
import com.cobaltplatform.api.model.api.request.PreviewCareEncounterScheduledMessageRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningAnswersRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningAnswersRequest.CreateAnswerRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningSessionRequest;
import com.cobaltplatform.api.model.api.request.CancelCareEncounterRequest;
import com.cobaltplatform.api.model.api.request.FindAppointmentBookingRequirementsRequest;
import com.cobaltplatform.api.model.api.request.FindCareEncountersRequest;
import com.cobaltplatform.api.model.api.request.FindCareEncountersRequest.CareEncounterAssignmentScopeId;
import com.cobaltplatform.api.model.api.request.UpdateAppointmentRequest;
import com.cobaltplatform.api.model.api.request.UpdateCareEncounterNoteRequest;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse;
import com.cobaltplatform.api.model.api.response.CareEncounterApiResponse;
import com.cobaltplatform.api.model.api.response.CareEncounterApiResponse.CareEncounterApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CareEncounterListApiResponse;
import com.cobaltplatform.api.model.api.response.CareEncounterListApiResponse.CareEncounterListApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionApiResponse;
import com.cobaltplatform.api.model.api.response.LocationApiResponse;
import com.cobaltplatform.api.model.api.response.ProviderApiResponse;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityId;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentSelectionTypeId;
import com.cobaltplatform.api.model.api.response.ScreeningAnswerOptionApiResponse;
import com.cobaltplatform.api.model.api.response.ScreeningAnswerOptionApiResponse.ScreeningAnswerOptionApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.AttendanceStatus.AttendanceStatusId;
import com.cobaltplatform.api.model.db.CareEncounter;
import com.cobaltplatform.api.model.db.CareEncounterCancellationReason.CareEncounterCancellationReasonId;
import com.cobaltplatform.api.model.db.CareEncounterNote;
import com.cobaltplatform.api.model.db.CareEncounterScheduledMessage;
import com.cobaltplatform.api.model.db.CareEncounterScheduledMessageType.CareEncounterScheduledMessageTypeId;
import com.cobaltplatform.api.model.db.CareEncounterStatus.CareEncounterStatusId;
import com.cobaltplatform.api.model.db.Feature.FeatureId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.MessageLog;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.ScheduledMessageSource.ScheduledMessageSourceId;
import com.cobaltplatform.api.model.db.ScheduledMessage;
import com.cobaltplatform.api.model.db.ScheduledMessageStatus.ScheduledMessageStatusId;
import com.cobaltplatform.api.model.db.ScreeningAnswerFormat.ScreeningAnswerFormatId;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;
import com.cobaltplatform.api.model.db.VideoconferencePlatform.VideoconferencePlatformId;
import com.cobaltplatform.api.model.service.AppointmentBookingRequirements;
import com.cobaltplatform.api.model.service.AppointmentBookingRequirements.AppointmentBookingRequirementsDestinationId;
import com.cobaltplatform.api.model.service.AppointmentBookingScreeningKey;
import com.cobaltplatform.api.model.service.FeatureForInstitution;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.model.service.ScreeningQuestionContext;
import com.cobaltplatform.api.model.service.ScreeningSessionDestination;
import com.cobaltplatform.api.model.service.ScreeningSessionDestination.ScreeningSessionDestinationId;
import com.cobaltplatform.api.model.service.ScreeningSessionDestinationResultId;
import com.cobaltplatform.api.model.service.ScreeningSessionResult;
import com.cobaltplatform.api.model.service.RenderedEmailMessage;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.cobaltplatform.api.web.resource.AccountResource;
import com.cobaltplatform.api.web.resource.CareEncounterResource;
import com.cobaltplatform.api.web.resource.ProviderResource;
import com.pyranid.Database;
import com.soklet.web.response.ApiResponse;
import org.junit.Test;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
	protected static final UUID CARE_NAVIGATOR_SUPPORT_TYPE_QUESTION_ID = UUID.fromString("ca4e5000-0000-4000-8000-000000000014");
	protected static final UUID CARE_NAVIGATOR_OTHER_SUPPORT_ANSWER_OPTION_ID = UUID.fromString("ca4e5000-0000-4000-8000-000000000508");
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
	public void careNavigatorBookingEnqueuesV2PatientNavigatorAndReminderEmails() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			MessageService messageService = app.getInjector().getInstance(MessageService.class);
			EmailMessageSerializer emailMessageSerializer = app.getInjector().getInstance(EmailMessageSerializer.class);
			Appointment appointment = appointmentService.findAppointmentById(CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID).get();
			Account navigator = accountService.findAccountById(CARE_NAVIGATOR_ACCOUNT_ID).get();
			Institution institution = institutionService.findInstitutionById(InstitutionId.COBALT).get();

			appointmentService.sendPatientAndProviderCobaltAppointmentCreatedEmails(appointment.getAppointmentId());

			List<EmailMessage> emails = enqueuedEmailsForAppointment(database, emailMessageSerializer,
					appointment.getAppointmentId());
			assertEquals(2, emails.size());
			EmailMessage patientEmail = emailWithTemplate(emails,
					EmailMessageTemplate.V2_CARE_NAVIGATOR_APPOINTMENT_CREATED_PATIENT);
			EmailMessage navigatorEmail = emailWithTemplate(emails,
					EmailMessageTemplate.V2_CARE_NAVIGATOR_APPOINTMENT_CREATED_NAVIGATOR);
			String patientWebappBaseUrl = institutionService.findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(
					InstitutionId.COBALT, UserExperienceTypeId.PATIENT).get();
			String staffWebappBaseUrl = institutionService.findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(
					InstitutionId.COBALT, UserExperienceTypeId.STAFF).get();
			String patientAppointmentUrl = String.format("%s/appointments/%s", patientWebappBaseUrl,
					appointment.getAppointmentId());
			String staffAppointmentUrl = String.format("%s/scheduling/appointments/%s", staffWebappBaseUrl,
					appointment.getAppointmentId());

			assertEquals(List.of(appointment.getEmailAddress()), patientEmail.getToAddresses());
			assertEquals(List.of(navigator.getEmailAddress()), navigatorEmail.getToAddresses());
			assertTrue(patientEmail.getReplyToAddress().isEmpty());
			assertTrue(navigatorEmail.getReplyToAddress().isEmpty());
			assertEquals(patientAppointmentUrl, patientEmail.getMessageContext().get("patientAppointmentUrl"));
			assertEquals(staffAppointmentUrl, navigatorEmail.getMessageContext().get("staffAppointmentUrl"));
			assertCalendarAttachment(patientEmail, patientAppointmentUrl, navigator.getEmailAddress(), "METHOD:REQUEST");
			assertCalendarAttachment(navigatorEmail, staffAppointmentUrl, navigator.getEmailAddress(), "METHOD:REQUEST");

			Appointment updatedAppointment = appointmentService.findAppointmentById(appointment.getAppointmentId()).get();
			assertNotNull(updatedAppointment.getPatientReminderScheduledMessageId());
			ScheduledMessage scheduledMessage = messageService.findScheduledMessageById(
					updatedAppointment.getPatientReminderScheduledMessageId()).get();
			EmailMessage reminderEmail = emailMessageSerializer.deserializeMessage(scheduledMessage.getSerializedMessage());
			LocalDate reminderDate = appointment.getStartTime().toLocalDate()
					.minusDays(institution.getAppointmentReservationDefaultReminderDayOffset());
			assertEquals(EmailMessageTemplate.V2_CARE_NAVIGATOR_APPOINTMENT_REMINDER_PATIENT,
					reminderEmail.getMessageTemplate());
			assertEquals(List.of(appointment.getEmailAddress()), reminderEmail.getToAddresses());
			assertTrue(reminderEmail.getReplyToAddress().isEmpty());
			assertTrue(reminderEmail.getEmailAttachments().isEmpty());
			assertEquals(LocalDateTime.of(reminderDate,
					institution.getAppointmentReservationDefaultReminderTimeOfDay()), scheduledMessage.getScheduledAt());
			assertEquals(appointment.getTimeZone(), scheduledMessage.getTimeZone());
			assertEquals(ScheduledMessageStatusId.PENDING, scheduledMessage.getScheduledMessageStatusId());
		});
	}

	@Test
	public void careNavigatorCancellationEnqueuesV2PatientAndNavigatorEmails() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			EmailMessageSerializer emailMessageSerializer = app.getInjector().getInstance(EmailMessageSerializer.class);
			Appointment appointment = appointmentService.findAppointmentById(CARE_NAVIGATOR_CANCELED_APPOINTMENT_ID).get();
			Account navigator = accountService.findAccountById(CARE_NAVIGATOR_ACCOUNT_ID).get();

			appointmentService.sendPatientAndProviderCobaltAppointmentCanceledEmails(appointment.getAppointmentId());

			List<EmailMessage> emails = enqueuedEmailsForAppointment(database, emailMessageSerializer,
					appointment.getAppointmentId());
			assertEquals(2, emails.size());
			EmailMessage patientEmail = emailWithTemplate(emails,
					EmailMessageTemplate.V2_CARE_NAVIGATOR_APPOINTMENT_CANCELED_PATIENT);
			EmailMessage navigatorEmail = emailWithTemplate(emails,
					EmailMessageTemplate.V2_CARE_NAVIGATOR_APPOINTMENT_CANCELED_NAVIGATOR);
			String patientWebappBaseUrl = institutionService.findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(
					InstitutionId.COBALT, UserExperienceTypeId.PATIENT).get();
			String staffWebappBaseUrl = institutionService.findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(
					InstitutionId.COBALT, UserExperienceTypeId.STAFF).get();
			String patientAppointmentUrl = String.format("%s/appointments/%s", patientWebappBaseUrl,
					appointment.getAppointmentId());
			String staffAppointmentUrl = String.format("%s/scheduling/appointments/%s", staffWebappBaseUrl,
					appointment.getAppointmentId());

			assertEquals(List.of(appointment.getEmailAddress()), patientEmail.getToAddresses());
			assertEquals(List.of(navigator.getEmailAddress()), navigatorEmail.getToAddresses());
			assertTrue(patientEmail.getReplyToAddress().isEmpty());
			assertTrue(navigatorEmail.getReplyToAddress().isEmpty());
			assertCalendarAttachment(patientEmail, patientAppointmentUrl, navigator.getEmailAddress(), "METHOD:CANCEL");
			assertCalendarAttachment(navigatorEmail, staffAppointmentUrl, navigator.getEmailAddress(), "METHOD:CANCEL");
		});
	}

	@Test
	public void careNavigatorCancellationCancelsTheV2PatientReminder() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			MessageService messageService = app.getInjector().getInstance(MessageService.class);
			Appointment appointment = appointmentService.findAppointmentById(CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID).get();

			appointmentService.sendPatientAndProviderCobaltAppointmentCreatedEmails(appointment.getAppointmentId());
			UUID reminderScheduledMessageId = appointmentService.findAppointmentById(appointment.getAppointmentId())
					.get().getPatientReminderScheduledMessageId();
			assertEquals(ScheduledMessageStatusId.PENDING, messageService.findScheduledMessageById(
					reminderScheduledMessageId).get().getScheduledMessageStatusId());

			CancelAppointmentRequest request = new CancelAppointmentRequest();
			request.setAppointmentId(appointment.getAppointmentId());
			request.setAccountId(appointment.getAccountId());
			request.setCanceledByAccountId(appointment.getAccountId());
			request.setCanceledByWebhook(false);
			request.setCanceledForReschedule(false);

			assertTrue(appointmentService.cancelAppointment(request));
			assertEquals(ScheduledMessageStatusId.CANCELED, messageService.findScheduledMessageById(
					reminderScheduledMessageId).get().getScheduledMessageStatusId());
		});
	}

	@Test
	public void careNavigatorBookingWithoutAssigneeStillNotifiesPatient() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			ProviderService providerService = app.getInjector().getInstance(ProviderService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			EmailMessageSerializer emailMessageSerializer = app.getInjector().getInstance(EmailMessageSerializer.class);
			Appointment appointment = appointmentService.findAppointmentById(CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID).get();
			Provider provider = providerService.findProviderById(appointment.getProviderId()).get();

			assertEquals(1, database.execute("""
					UPDATE care_encounter
					SET care_navigator_account_id=NULL
					WHERE care_encounter_id=?
					""", appointment.getCareEncounterId()));

			appointmentService.sendPatientAndProviderCobaltAppointmentCreatedEmails(appointment.getAppointmentId());

			List<EmailMessage> emails = enqueuedEmailsForAppointment(database, emailMessageSerializer,
					appointment.getAppointmentId());
			assertEquals(1, emails.size());
			EmailMessage patientEmail = emailWithTemplate(emails,
					EmailMessageTemplate.V2_CARE_NAVIGATOR_APPOINTMENT_CREATED_PATIENT);
			String patientWebappBaseUrl = institutionService.findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(
					InstitutionId.COBALT, UserExperienceTypeId.PATIENT).get();
			String patientAppointmentUrl = String.format("%s/appointments/%s", patientWebappBaseUrl,
					appointment.getAppointmentId());

			assertEquals(List.of(appointment.getEmailAddress()), patientEmail.getToAddresses());
			assertCalendarAttachment(patientEmail, patientAppointmentUrl, provider.getEmailAddress(), "METHOD:REQUEST");
			assertNotNull(appointmentService.findAppointmentById(appointment.getAppointmentId()).get()
					.getPatientReminderScheduledMessageId());
		});
	}

	@Test
	public void careNavigatorPatientEmailFallsBackToAccountEmail() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			MessageService messageService = app.getInjector().getInstance(MessageService.class);
			EmailMessageSerializer emailMessageSerializer = app.getInjector().getInstance(EmailMessageSerializer.class);
			Appointment appointment = appointmentService.findAppointmentById(CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID).get();
			Account patient = accountService.findAccountById(appointment.getAccountId()).get();

			assertEquals(1, database.execute("""
					UPDATE appointment
					SET email_address=NULL
					WHERE appointment_id=?
					""", appointment.getAppointmentId()));

			appointmentService.sendPatientAndProviderCobaltAppointmentCreatedEmails(appointment.getAppointmentId());

			EmailMessage patientEmail = emailWithTemplate(
					enqueuedEmailsForAppointment(database, emailMessageSerializer, appointment.getAppointmentId()),
					EmailMessageTemplate.V2_CARE_NAVIGATOR_APPOINTMENT_CREATED_PATIENT);
			assertEquals(List.of(patient.getEmailAddress()), patientEmail.getToAddresses());

			Appointment updatedAppointment = appointmentService.findAppointmentById(appointment.getAppointmentId()).get();
			EmailMessage reminderEmail = emailMessageSerializer.deserializeMessage(messageService.findScheduledMessageById(
					updatedAppointment.getPatientReminderScheduledMessageId()).get().getSerializedMessage());
			assertEquals(List.of(patient.getEmailAddress()), reminderEmail.getToAddresses());
		});
	}

	@Test
	public void careNavigatorEmailRoutingDoesNotChangeOtherAppointmentTemplates() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			EmailMessageSerializer emailMessageSerializer = app.getInjector().getInstance(EmailMessageSerializer.class);
			UUID switchboardProviderId = createActiveProvider(database, "Unrelated Switchboard Provider");
			UUID switchboardAppointmentId = UUID.randomUUID();
			cloneAsActiveAppointmentForProviderAndAccount(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID,
					switchboardAppointmentId, switchboardProviderId,
					CARE_NAVIGATOR_PATIENT_CANCELED_FIXTURE_PATIENT_ID, 70);

			appointmentService.sendPatientAndProviderCobaltAppointmentCreatedEmails(switchboardAppointmentId);
			assertTrue(enqueuedEmailsForAppointment(database, emailMessageSerializer, switchboardAppointmentId).isEmpty());

			UUID ordinaryProviderId = createActiveProvider(database, "Ordinary Cobalt Provider");
			UUID ordinaryAppointmentId = UUID.randomUUID();
			cloneAsActiveAppointmentForProviderAndAccount(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID,
					ordinaryAppointmentId, ordinaryProviderId,
					CARE_NAVIGATOR_PATIENT_CANCELED_FIXTURE_PATIENT_ID, 71);
			assertEquals(1, database.execute("""
					UPDATE appointment
					SET videoconference_platform_id='EXTERNAL',
						videoconference_url='https://example.com/ordinary-appointment'
					WHERE appointment_id=?
					""", ordinaryAppointmentId));

			appointmentService.sendPatientAndProviderCobaltAppointmentCreatedEmails(ordinaryAppointmentId);

			List<EmailMessage> ordinaryEmails = enqueuedEmailsForAppointment(database, emailMessageSerializer,
					ordinaryAppointmentId);
			assertEquals(2, ordinaryEmails.size());
			emailWithTemplate(ordinaryEmails, EmailMessageTemplate.APPOINTMENT_CREATED_PATIENT);
			emailWithTemplate(ordinaryEmails, EmailMessageTemplate.APPOINTMENT_CREATED_PROVIDER);
		});
	}

	@Test
	public void telephoneProviderReceivesV2IntakeEmailWithAppointmentContactAndScreeningResponses() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			EmailMessageSerializer emailMessageSerializer = app.getInjector().getInstance(EmailMessageSerializer.class);
			UUID telephoneProviderId = createActiveProvider(database, "CuraLinc Intake Counselor");
			UUID appointmentId = UUID.randomUUID();

			assertEquals(1, database.execute("""
					UPDATE provider
					SET videoconference_platform_id='TELEPHONE',
						phone_number='+18885032380',
						videoconference_url=NULL
					WHERE provider_id=?
					""", telephoneProviderId));

			cloneAsActiveAppointmentForProviderAndAccount(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID,
					appointmentId, telephoneProviderId, CARE_NAVIGATOR_ACTIVE_FIXTURE_PATIENT_ID, 72);

			assertEquals(1, database.execute("""
					UPDATE appointment
					SET first_name='Booking',
						last_name='Contact',
						email_address='booking-contact@example.com',
						contact_phone_number='+12155550123',
						screening_session_id=?,
						videoconference_platform_id='TELEPHONE',
						videoconference_url='https://cobalt.example/appointments/telephone'
					WHERE appointment_id=?
					""", CARE_NAVIGATOR_UPCOMING_SCREENING_SESSION_ID, appointmentId));

			appointmentService.sendPatientAndProviderCobaltAppointmentCreatedEmails(appointmentId);

			List<EmailMessage> emails = enqueuedEmailsForAppointment(database, emailMessageSerializer, appointmentId);
			assertEquals(2, emails.size());
			emailWithTemplate(emails, EmailMessageTemplate.APPOINTMENT_CREATED_PATIENT);
			EmailMessage providerEmail = emailWithTemplate(emails,
					EmailMessageTemplate.V2_PROVIDER_INTAKE_APPOINTMENT_CREATED_PROVIDER);

			assertEquals("Booking Contact", providerEmail.getMessageContext().get("patientName"));
			assertEquals("booking-contact@example.com", providerEmail.getMessageContext().get("patientEmailAddress"));
			assertEquals("(215) 555-0123", providerEmail.getMessageContext().get("patientPhoneNumber"));
			assertTrue(providerEmail.getMessageContext().get("providerSchedulingUrl").toString()
					.endsWith("/scheduling/appointments/" + appointmentId));

			List<?> intakeResponses = (List<?>) providerEmail.getMessageContext().get("intakeResponses");
			assertNotNull(intakeResponses);
			assertFalse(intakeResponses.isEmpty());
			assertTrue(intakeResponses.stream()
					.map(response -> (Map<?, ?>) response)
					.anyMatch(response -> response.get("answer").toString().contains(NAVIGATOR_CONTEXT_FIXTURE_TEXT)));
		});
	}

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
				assertEquals("Connect with a Care Navigator", resourceNavigator.getNavDescription());
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
				assertEquals("Care Navigator", provider.getName());
				assertEquals("Care Navigator", provider.getTitle());
				assertEquals("Cobalt", provider.getEntity());
				assertEquals("Cobalt Care Navigation", provider.getClinic());
				assertEquals("Care Navigation", provider.getSpecialty());
				assertEquals("Our Care Navigator is here to help you identify and connect with mental health and wellness resources that best fit your needs.",
						provider.getDescription());
				assertEquals("https://placehold.co/320x320/png?text=Care+Navigator", provider.getImageUrl());
				assertEquals(Boolean.FALSE, provider.getDefaultImageUrl());
				assertEquals("https://fixtures.cobalt.care/providers/cobalt-care-navigator/bio", provider.getBioUrl());
				assertEquals("care-navigator@cobaltinnovations.org", provider.getEmailAddress());
				assertNull(provider.getWebsiteUrl());
				assertTrue(provider.getBio().contains("During the video call"));
				assertTrue(provider.getDetailsHtml().contains("What is a Care Navigator"));
				assertTrue(provider.getDetailsHtml().contains("Care Navigators are not licensed clinicians"));
				assertTrue(provider.getDetailsHtml().contains("please call 911 or 988 immediately"));
				assertTrue(provider.getDetailsHtml().contains("Your privacy is important to us"));
				assertEquals(List.of("Provider matching", "Care options", "Mental health navigation"), provider.getTags());
				assertNull(provider.getPhoneNumber());
				assertNull(provider.getFormattedPhoneNumber());
				assertEquals("Care Navigator", provider.getSupportRolesDescription());
				assertEquals(List.of("No Fee"), provider.getPaymentFundingDescriptions());
				assertEquals(1, provider.getSupportedAppointmentModalities().size());
				assertEquals(ProviderAppointmentModalityId.VIRTUAL,
						provider.getSupportedAppointmentModalities().get(0).getAppointmentModalityId());
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
	public void careNavigatorProviderAppointmentDetailsIncludeBookingContactAndScreeningResponses() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AccountResource accountResource = app.getInjector().getInstance(AccountResource.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			Account careNavigatorAccount = accountService.findAccountById(CARE_NAVIGATOR_ACCOUNT_ID).get();

			currentContextExecutor.execute(
					new CurrentContext.Builder(careNavigatorAccount, Locale.US, ZoneId.of("America/New_York")).build(),
					() -> {
						ApiResponse response = accountResource.accountWithAppointmentDetails(
								CARE_NAVIGATOR_ACTIVE_FIXTURE_PATIENT_ID, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);
						Map<String, Object> model = (Map<String, Object>) response.model().get();
						AppointmentApiResponse appointment = (AppointmentApiResponse) model.get("appointment");

						assertEquals(200, response.status());
						assertEquals("Jordan", appointment.getFirstName());
						assertEquals("Lee", appointment.getLastName());
						assertEquals("care-encounter.jordan@example.com", appointment.getEmailAddress());
						assertEquals("+12155553002", appointment.getContactPhoneNumber());
						assertNotNull(appointment.getScreeningSessionResult());
						assertEquals(5, appointment.getScreeningSessionResult()
								.getScreeningSessionScreeningResults().get(0).getScreeningQuestionResults().size());
						assertEquals(NAVIGATOR_CONTEXT_FIXTURE_TEXT, appointment.getScreeningSessionResult()
								.getScreeningSessionScreeningResults().get(0).getScreeningQuestionResults().get(4)
								.getScreeningAnswerResults().get(1).getText());
					});
		});
	}

	@Test
	public void careNavigatorEncounterIdentifiesSelfBookingPatient() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			CareEncounterService careEncounterService = app.getInjector().getInstance(CareEncounterService.class);
			CareEncounterApiResponseFactory responseFactory = app.getInjector()
					.getInstance(CareEncounterApiResponseFactory.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			Account careNavigatorAccount = accountService.findAccountById(CARE_NAVIGATOR_ACCOUNT_ID).get();
			Appointment appointment = appointmentService.findAppointmentById(CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID).get();
			CareEncounter careEncounter = careEncounterService.findCareEncounterByIdForInstitutionId(
					appointment.getCareEncounterId(), InstitutionId.COBALT).get();

			currentContextExecutor.execute(
					new CurrentContext.Builder(careNavigatorAccount, Locale.US, ZoneId.of("America/New_York")).build(),
					() -> {
						CareEncounterApiResponse response = responseFactory.create(careEncounter);

						assertEquals(CARE_NAVIGATOR_ACTIVE_FIXTURE_PATIENT_ID, response.getCreatedByAccountId());
						assertEquals("Jordan Lee (Patient — self-booked)", response.getCreatedByAccountDisplayName());
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

			// This scenario exercises creation of a fresh lifecycle. Keep it isolated from
			// any canceled appointments left behind by local manual testing.
			database.execute("""
					UPDATE care_encounter
					SET care_encounter_status_id='CLOSED',
					    closed_at=NOW(),
					    closed_by_account_id=?
					WHERE account_id=?
					AND care_encounter_status_id='OPEN'
					""", account.getAccountId(), account.getAccountId());
			assertFixtureGraph(database);
			assertEquals(Long.valueOf(0), database.queryForObject("""
					SELECT COUNT(*)
					FROM care_encounter
					WHERE account_id=?
					AND care_encounter_status_id='OPEN'
					AND deleted=FALSE
					""", Long.class, account.getAccountId()).get());

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
					"Who are you seeking support for?",
					"Who is your current employer?",
					"Select your current health insurance plan from the list below.",
					"Select your current behavioral health insurance plan from the list below.",
					"What kind of support are you looking for today?"
			);
			List<List<String>> expectedAnswerOptions = List.of(
					List.of("Myself", "Spouse/partner", "Child/children", "Other"),
					List.of("UPHS (University Pennsylvania Health System)", "UPenn (University of Pennsylvania)",
							"LGH (Lancaster General Health)", "Princeton (Princeton Health)",
							"CCH (Chester County Hospital)", "Doylestown (Doylestown Health)",
							"I'm not sure / I'd rather not say"),
					List.of("PennCare PPO (UPHS)", "PennCare HDHP (UPHS)", "Aetna POS (UPenn)",
							"Aetna HDHP (UPenn)", "PennCare PPO (UPenn)", "Keystone / AmeriHealth HMO (UPenn)",
							"Tricare", "Medicaid", "Medicare", "I'm not sure / I don't know", "Other"),
					List.of("Aetna Behavioral Health Network", "Independence Behavioral Health Network (IBX)",
							"Lyra | Carelon Behavioral Health", "I'm not sure / I don't know", "Other"),
					List.of("Finding a therapist or behavioral health provider",
							"Stress, burnout, or work-life challenges",
							"Anxiety, depression, or other emotional well-being concerns",
							"Medication or psychiatry questions", "Parenting, childcare or caregiving support",
							"Help understanding available behavioral health services or benefits",
							"Wellness resources or support groups", "Help navigating the Cobalt website",
							"Something else / I'm not sure")
			);
			List<ScreeningAnswerFormatId> expectedAnswerFormats = List.of(
					ScreeningAnswerFormatId.MULTI_SELECT,
					ScreeningAnswerFormatId.SINGLE_SELECT,
					ScreeningAnswerFormatId.SINGLE_SELECT,
					ScreeningAnswerFormatId.SINGLE_SELECT,
					ScreeningAnswerFormatId.MULTI_SELECT
			);
			List<Integer> expectedMaximumAnswerCounts = List.of(4, 1, 1, 1, 9);
			List<Boolean> expectedPreferAutosubmit = List.of(false, true, true, true, false);
			int questionIndex = 0;
			Optional<ScreeningQuestionContext> questionContext;

			while ((questionContext = screeningService
					.findNextUnansweredScreeningQuestionContextByScreeningSessionId(screeningSessionId)).isPresent()) {
				ScreeningQuestionContext currentQuestionContext = questionContext.get();
				assertEquals(expectedQuestionTexts.get(questionIndex),
						currentQuestionContext.getScreeningQuestion().getQuestionText());
				assertEquals(expectedAnswerFormats.get(questionIndex),
						currentQuestionContext.getScreeningQuestion().getScreeningAnswerFormatId());
				assertEquals(Integer.valueOf(1),
						currentQuestionContext.getScreeningQuestion().getMinimumAnswerCount());
				assertEquals(expectedMaximumAnswerCounts.get(questionIndex),
						currentQuestionContext.getScreeningQuestion().getMaximumAnswerCount());
				assertEquals(expectedPreferAutosubmit.get(questionIndex),
						currentQuestionContext.getScreeningQuestion().getPreferAutosubmit());
				assertNull(currentQuestionContext.getScreeningQuestion().getIntroText());
				assertNull(currentQuestionContext.getScreeningQuestion().getSupplementText());
				assertEquals(expectedAnswerOptions.get(questionIndex), currentQuestionContext.getScreeningAnswerOptions()
						.stream().map(answerOption -> answerOption.getAnswerOptionText()).toList());

				List<CreateAnswerRequest> answers;
				if (questionIndex == 0) {
					CreateAnswerRequest myselfAnswer = new CreateAnswerRequest();
					myselfAnswer.setScreeningAnswerOptionId(
							currentQuestionContext.getScreeningAnswerOptions().get(0).getScreeningAnswerOptionId());
					CreateAnswerRequest spouseAnswer = new CreateAnswerRequest();
					spouseAnswer.setScreeningAnswerOptionId(
							currentQuestionContext.getScreeningAnswerOptions().get(1).getScreeningAnswerOptionId());
					answers = List.of(myselfAnswer, spouseAnswer);
					} else if (questionIndex == 4) {
						assertEquals(9, currentQuestionContext.getScreeningAnswerOptions().size());
						CreateAnswerRequest therapistAnswer = new CreateAnswerRequest();
					therapistAnswer.setScreeningAnswerOptionId(
							currentQuestionContext.getScreeningAnswerOptions().get(0).getScreeningAnswerOptionId());
					CreateAnswerRequest otherAnswer = new CreateAnswerRequest();
					otherAnswer.setScreeningAnswerOptionId(CARE_NAVIGATOR_OTHER_SUPPORT_ANSWER_OPTION_ID);
					otherAnswer.setText(NAVIGATOR_CONTEXT_FIXTURE_TEXT);
					answers = List.of(therapistAnswer, otherAnswer);
				} else {
					CreateAnswerRequest answer = new CreateAnswerRequest();
					answer.setScreeningAnswerOptionId(
							currentQuestionContext.getScreeningAnswerOptions().get(0).getScreeningAnswerOptionId());
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
			assertNull(database.queryForObject("SELECT screening_session_contact_email_address(?)", String.class,
					screeningSessionId).orElse(null));
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
			assertEquals(bookingEmailAddress, appointment.getEmailAddress());
			assertEquals(VideoconferencePlatformId.SWITCHBOARD, appointment.getVideoconferencePlatformId());
			assertNull(appointment.getMicrosoftTeamsMeetingId());

			assertEquals(CARE_NAVIGATOR_ACCOUNT_ID, database.queryForObject("""
					SELECT care_encounter.care_navigator_account_id
					FROM appointment
					JOIN care_encounter ON care_encounter.care_encounter_id=appointment.care_encounter_id
					WHERE appointment.appointment_id=?
					""", UUID.class, appointmentId).get());
			assertEquals(bookingEmailAddress, database.queryForObject("""
					SELECT care_encounter.email_address
					FROM appointment
					JOIN care_encounter ON care_encounter.care_encounter_id=appointment.care_encounter_id
					WHERE appointment.appointment_id=?
					""", String.class, appointmentId).get());

			CareEncounter careEncounter = careEncounterService.findCareEncounterByIdForInstitutionId(
					appointment.getCareEncounterId(), InstitutionId.COBALT).get();
			assertEquals(bookingEmailAddress, careEncounter.getEmailAddress());
			AppointmentBookingScreeningKey consumedScreeningKey = new AppointmentBookingScreeningKey(
					CARE_NAVIGATOR_PROVIDER_ID, CARE_NAVIGATOR_APPOINTMENT_TYPE_ID,
					CARE_NAVIGATOR_SCREENING_FLOW_ID);
			assertFalse(appointmentService.findCompletedAppointmentBookingScreeningKeys(account.getAccountId(),
					Set.of(consumedScreeningKey))
					.contains(consumedScreeningKey));
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
			assertEquals(List.of("Myself", "Spouse/partner"), questionResults.get(0)
					.getScreeningAnswerResults().stream()
					.map(ScreeningSessionResult.ScreeningAnswerResult::getAnswerOptionText).toList());
			assertEquals("UPHS (University Pennsylvania Health System)",
					questionResults.get(1).getScreeningAnswerResults().get(0).getAnswerOptionText());
			assertEquals("PennCare PPO (UPHS)",
					questionResults.get(2).getScreeningAnswerResults().get(0).getAnswerOptionText());
			assertEquals("Aetna Behavioral Health Network",
					questionResults.get(3).getScreeningAnswerResults().get(0).getAnswerOptionText());
			assertEquals(List.of("Finding a therapist or behavioral health provider", "Something else / I'm not sure"),
					questionResults.get(4).getScreeningAnswerResults().stream()
							.map(ScreeningSessionResult.ScreeningAnswerResult::getAnswerOptionText).toList());
			assertEquals(NAVIGATOR_CONTEXT_FIXTURE_TEXT,
					questionResults.get(4).getScreeningAnswerResults().get(1).getText());
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

			ValidationException duplicateRequirementsException = assertThrows(ValidationException.class,
					() -> appointmentService.findAppointmentBookingRequirements(requirementsRequest, account));
			assertEquals(Boolean.TRUE,
					duplicateRequirementsException.getMetadata().get("careNavigatorOpenAppointmentExists"));
		});
	}

	@Test
	public void careNavigatorBookingRequirementsRejectDuplicatesBeforeChangingScreeningSessions() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			ScreeningService screeningService = app.getInjector().getInstance(ScreeningService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Account account = accountService.findAccountById(CARE_NAVIGATOR_ACTIVE_FIXTURE_PATIENT_ID).get();
			LocalDate bookingDate = nextWeekday(LocalDate.now(ZoneId.of("America/New_York")).plusDays(14));
			LocalTime bookingTime = LocalTime.of(9, 0);
			String untouchedMetadata = "{\"preflightTest\":true}";

			database.execute("""
					UPDATE screening_session
					SET completed=FALSE,
					    completed_at=NULL,
					    metadata=CAST(? AS JSONB),
					    last_updated=NOW() - INTERVAL '1 day'
					WHERE screening_session_id=?
					""", untouchedMetadata, CARE_NAVIGATOR_UPCOMING_SCREENING_SESSION_ID);
			ScreeningSession sessionBefore = screeningService
					.findScreeningSessionById(CARE_NAVIGATOR_UPCOMING_SCREENING_SESSION_ID).get();

			ValidationException exception = assertThrows(ValidationException.class,
					() -> appointmentService.findAppointmentBookingRequirements(
						requirementsRequestFor(account, bookingDate, bookingTime), account));

			assertEquals(Boolean.TRUE, exception.getMetadata().get("careNavigatorOpenAppointmentExists"));
			ScreeningSession sessionAfter = screeningService
					.findScreeningSessionById(CARE_NAVIGATOR_UPCOMING_SCREENING_SESSION_ID).get();
			assertEquals(sessionBefore.getLastUpdated(), sessionAfter.getLastUpdated());
			assertEquals(sessionBefore.getMetadata(), sessionAfter.getMetadata());
		});

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			ScreeningService screeningService = app.getInjector().getInstance(ScreeningService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Account account = accountService.findAccountById(CARE_NAVIGATOR_ATTENDED_FIXTURE_PATIENT_ID).get();
			LocalDate bookingDate = nextWeekday(LocalDate.now(ZoneId.of("America/New_York")).plusDays(14));
			LocalTime bookingTime = LocalTime.of(9, 0);
			int sessionCountBefore = screeningService.findScreeningSessionsByScreeningFlowIdAndTargetAccountId(
					CARE_NAVIGATOR_SCREENING_FLOW_ID, account.getAccountId()).size();

			ValidationException exception = assertThrows(ValidationException.class,
					() -> appointmentService.findAppointmentBookingRequirements(
						requirementsRequestFor(account, bookingDate, bookingTime), account));

			assertEquals(Boolean.TRUE, exception.getMetadata().get("careNavigatorEncounterAwaitingClosure"));
			assertEquals(sessionCountBefore, screeningService.findScreeningSessionsByScreeningFlowIdAndTargetAccountId(
					CARE_NAVIGATOR_SCREENING_FLOW_ID, account.getAccountId()).size());
		});
	}

	@Test
	public void appointmentCreationRetainsLockedCareNavigatorDuplicateValidation() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Account account = accountService.findAccountById(CARE_NAVIGATOR_ACTIVE_FIXTURE_PATIENT_ID).get();
			String emailAddress = "care-navigator-race-check@cobaltinnovations.org";

			// Make the fixture's completed screening available without removing its active appointment.
			database.execute("UPDATE appointment SET screening_session_id=NULL WHERE appointment_id=?",
					CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);
			database.execute("""
					UPDATE appointment
					SET created=(
					  SELECT completed_at - INTERVAL '1 second'
					  FROM screening_session
					  WHERE screening_session_id=?
					)
					WHERE appointment_id=?
					""", CARE_NAVIGATOR_UPCOMING_SCREENING_SESSION_ID, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);
			database.execute("""
					INSERT INTO account_email_verification (
					  account_email_verification_id, account_id, code, email_address, verified
					) VALUES (?, ?, ?, ?, TRUE)
					""", UUID.randomUUID(), account.getAccountId(), "123456", emailAddress);

			CreateAppointmentRequest request = new CreateAppointmentRequest();
			request.setAccountId(account.getAccountId());
			request.setCreatedByAcountId(account.getAccountId());
			request.setProviderId(CARE_NAVIGATOR_PROVIDER_ID);
			request.setAppointmentTypeId(CARE_NAVIGATOR_APPOINTMENT_TYPE_ID);
			request.setDate(nextWeekday(LocalDate.now(ZoneId.of("America/New_York")).plusDays(14)));
			request.setTime(LocalTime.of(9, 0));
			request.setFirstName("Care");
			request.setLastName("Navigator Race Check");
			request.setEmailAddress(emailAddress);
			request.setPhoneNumber("+12155550123");
			request.setBookingExperienceId(BookingExperienceId.V2);
			request.setAppointmentModalityId(ProviderAppointmentModalityId.VIRTUAL);

			ValidationException exception = assertThrows(ValidationException.class,
					() -> appointmentService.createAppointment(request));
			assertEquals(Boolean.TRUE, exception.getMetadata().get("careNavigatorOpenAppointmentExists"));
		});
	}

	@Test
	public void canceledAppointmentsAndClosedEncountersCanReachBookingRequirements() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Account canceledAppointmentAccount = accountService
					.findAccountById(CARE_NAVIGATOR_ACTIVE_FIXTURE_PATIENT_ID).get();
			LocalDate bookingDate = nextWeekday(LocalDate.now(ZoneId.of("America/New_York")).plusDays(14));
			LocalTime bookingTime = LocalTime.of(9, 0);

			database.execute("""
					UPDATE appointment
					SET canceled=TRUE,
					    attendance_status_id='CANCELED'
					WHERE appointment_id=?
					""", CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);
			AppointmentBookingRequirements canceledRequirements = appointmentService.findAppointmentBookingRequirements(
					requirementsRequestFor(canceledAppointmentAccount, bookingDate, bookingTime),
					canceledAppointmentAccount);
			assertNotNull(canceledRequirements);

			Account closedEncounterAccount = accountService
					.findAccountById(CARE_NAVIGATOR_ATTENDED_FIXTURE_PATIENT_ID).get();
			database.execute("""
					UPDATE care_encounter
					SET care_encounter_status_id='CLOSED',
					    closed_at=NOW(),
					    closed_by_account_id=?
					WHERE account_id=?
					AND care_encounter_status_id='OPEN'
					""", CARE_NAVIGATOR_ACCOUNT_ID, closedEncounterAccount.getAccountId());
			AppointmentBookingRequirements closedRequirements = appointmentService.findAppointmentBookingRequirements(
					requirementsRequestFor(closedEncounterAccount, bookingDate, bookingTime), closedEncounterAccount);
			assertNotNull(closedRequirements);
		});
	}

	@Test
	public void careNavigatorSupplementRequirednessIsExposedAndEnforced() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ScreeningService screeningService = app.getInjector().getInstance(ScreeningService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			ScreeningAnswerOptionApiResponseFactory responseFactory =
					app.getInjector().getInstance(ScreeningAnswerOptionApiResponseFactory.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);

			ScreeningAnswerOptionApiResponse optionalResponse = responseFactory.create(screeningService
					.findScreeningAnswerOptionById(CARE_NAVIGATOR_OTHER_SUPPORT_ANSWER_OPTION_ID).get());
			assertEquals(Boolean.TRUE, optionalResponse.getFreeformSupplement());
			assertEquals("Tell us more", optionalResponse.getFreeformSupplementText().get());
			assertEquals(Boolean.FALSE, optionalResponse.getFreeformSupplementTextRequired());
			assertEquals(Boolean.FALSE,
					new JsonMapper().toMap(optionalResponse).get("freeformSupplementTextRequired"));

			UUID optionalScreeningSessionId = screeningService.createScreeningSession(new CreateScreeningSessionRequest() {{
				setScreeningFlowId(CARE_NAVIGATOR_SCREENING_FLOW_ID);
				setTargetAccountId(account.getAccountId());
				setCreatedByAccountId(account.getAccountId());
			}});
			answerFirstFourCareNavigatorQuestions(screeningService, optionalScreeningSessionId, account.getAccountId());

			ScreeningQuestionContext optionalSupportTypeQuestionContext = screeningService
					.findNextUnansweredScreeningQuestionContextByScreeningSessionId(optionalScreeningSessionId).get();
			CreateAnswerRequest optionalBlankAnswer = new CreateAnswerRequest();
			optionalBlankAnswer.setScreeningAnswerOptionId(CARE_NAVIGATOR_OTHER_SUPPORT_ANSWER_OPTION_ID);
			CreateScreeningAnswersRequest optionalBlankRequest = new CreateScreeningAnswersRequest();
			optionalBlankRequest.setScreeningQuestionContextId(
					optionalSupportTypeQuestionContext.getScreeningQuestionContextId());
			optionalBlankRequest.setCreatedByAccountId(account.getAccountId());
			optionalBlankRequest.setAnswers(List.of(optionalBlankAnswer));
			screeningService.createScreeningAnswers(optionalBlankRequest);
			assertNull(screeningService.findNextUnansweredScreeningQuestionContextByScreeningSessionId(
					optionalScreeningSessionId).orElse(null));

			UUID requiredSupplementAnswerOptionId = UUID.randomUUID();
			database.execute("""
					INSERT INTO screening_answer_option (
					  screening_answer_option_id,
					  screening_question_id,
					  answer_option_text,
					  score,
					  indicates_crisis,
					  freeform_supplement,
					  freeform_supplement_text,
					  display_order
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
					""", requiredSupplementAnswerOptionId, CARE_NAVIGATOR_SUPPORT_TYPE_QUESTION_ID,
					"Required supplement test option", 1, false, true, "Required details", 10);

			ScreeningAnswerOptionApiResponse requiredResponse = responseFactory.create(screeningService
					.findScreeningAnswerOptionById(requiredSupplementAnswerOptionId).get());
			assertEquals(Boolean.TRUE, requiredResponse.getFreeformSupplementTextRequired());

			UUID screeningSessionId = screeningService.createScreeningSession(new CreateScreeningSessionRequest() {{
				setScreeningFlowId(CARE_NAVIGATOR_SCREENING_FLOW_ID);
				setTargetAccountId(account.getAccountId());
				setCreatedByAccountId(account.getAccountId());
			}});

			answerFirstFourCareNavigatorQuestions(screeningService, screeningSessionId, account.getAccountId());

			ScreeningQuestionContext supportTypeQuestionContext = screeningService
					.findNextUnansweredScreeningQuestionContextByScreeningSessionId(screeningSessionId).get();
			assertEquals(CARE_NAVIGATOR_SUPPORT_TYPE_QUESTION_ID,
					supportTypeQuestionContext.getScreeningQuestion().getScreeningQuestionId());
			CreateAnswerRequest blankRequiredAnswer = new CreateAnswerRequest();
			blankRequiredAnswer.setScreeningAnswerOptionId(requiredSupplementAnswerOptionId);
			CreateScreeningAnswersRequest blankRequiredRequest = new CreateScreeningAnswersRequest();
			blankRequiredRequest.setScreeningQuestionContextId(supportTypeQuestionContext.getScreeningQuestionContextId());
			blankRequiredRequest.setCreatedByAccountId(account.getAccountId());
			blankRequiredRequest.setAnswers(List.of(blankRequiredAnswer));

			assertThrows(ValidationException.class,
					() -> screeningService.createScreeningAnswers(blankRequiredRequest));

			blankRequiredAnswer.setText("Required supplement value");
			screeningService.createScreeningAnswers(blankRequiredRequest);
			assertNull(screeningService.findNextUnansweredScreeningQuestionContextByScreeningSessionId(screeningSessionId)
					.orElse(null));
		});
	}

	protected void answerFirstFourCareNavigatorQuestions(ScreeningService screeningService,
																				 UUID screeningSessionId,
																				 UUID accountId) {
		for (int questionIndex = 0; questionIndex < 4; ++questionIndex) {
			ScreeningQuestionContext questionContext = screeningService
					.findNextUnansweredScreeningQuestionContextByScreeningSessionId(screeningSessionId).get();
			CreateAnswerRequest answer = new CreateAnswerRequest();
			answer.setScreeningAnswerOptionId(
					questionContext.getScreeningAnswerOptions().get(0).getScreeningAnswerOptionId());
			CreateScreeningAnswersRequest request = new CreateScreeningAnswersRequest();
			request.setScreeningQuestionContextId(questionContext.getScreeningQuestionContextId());
			request.setCreatedByAccountId(accountId);
			request.setAnswers(List.of(answer));
			screeningService.createScreeningAnswers(request);
		}
	}

	@Test
	public void careEncounterPreservesAppointmentEmailWhenScreeningSessionIsAbsent() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			UUID appointmentId = UUID.randomUUID();
			UUID patientAccountId = accountService.createAccount(new CreateAccountRequest() {{
				setAccountSourceId(AccountSourceId.ANONYMOUS);
				setInstitutionId(InstitutionId.COBALT);
			}});
			String appointmentEmailAddress = "care-encounter.booking-fallback@example.com";

			assertEquals(1, database.execute("""
					UPDATE appointment
					SET email_address=?
					WHERE appointment_id=?
					""", appointmentEmailAddress, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID));

			cloneAsActiveAppointmentForProviderAndAccount(
					database,
					CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID,
					appointmentId,
					CARE_NAVIGATOR_PROVIDER_ID,
					patientAccountId,
					60);

			assertNull(database.queryForObject("""
					SELECT screening_session_id
					FROM appointment
					WHERE appointment_id=?
					""", UUID.class, appointmentId).orElse(null));

			UUID careEncounterId = careEncounterIdForAppointment(database, appointmentId);
			assertEquals(appointmentEmailAddress, database.queryForObject("""
					SELECT email_address
					FROM care_encounter
					WHERE care_encounter_id=?
					""", String.class, careEncounterId).get());
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
				assertEquals(CARE_NAVIGATOR_ACCOUNT_ID,
						response.getAppointmentHistory().get(0).getCanceledByAccountId());
				assertEquals(accountService.determineDisplayName(navigator),
						response.getAppointmentHistory().get(0).getCanceledByAccountDisplayName());
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
			activeCancelRequest.setCareEncounterCancellationReasonId(
					CareEncounterCancellationReasonId.CARE_DELIVERED_DURING_CALL);
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
			cancelRequest.setCareEncounterCancellationReasonId(
					CareEncounterCancellationReasonId.CARE_DELIVERED_DURING_CALL);
			CareEncounter canceledEncounter = careEncounterService.cancelCareEncounter(attendedCareEncounterId,
					InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID, cancelRequest);
			assertEquals(CareEncounterStatusId.CANCELED, canceledEncounter.getCareEncounterStatusId());
			assertFalse(database.queryForObject("SELECT canceled FROM appointment WHERE appointment_id=?", Boolean.class,
					CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID).get());
			assertTrue(careEncounterService.deleteCareEncounter(attendedCareEncounterId, InstitutionId.COBALT,
					CARE_NAVIGATOR_ACCOUNT_ID));
			assertEquals(Integer.valueOf(1), database.queryForObject(
					"SELECT COUNT(*) FROM care_encounter WHERE care_encounter_id=?",
					Integer.class, attendedCareEncounterId).get());
			assertTrue(database.queryForObject("SELECT deleted FROM care_encounter WHERE care_encounter_id=?",
					Boolean.class, attendedCareEncounterId).get());
		});
	}

	@Test
	public void navigatorContactEmailEndpointUpdatesOpenEncounterOnly() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CareEncounterService careEncounterService = app.getInjector().getInstance(CareEncounterService.class);
			CareEncounterResource careEncounterResource = app.getInjector().getInstance(CareEncounterResource.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			Account navigator = accountService.findAccountById(CARE_NAVIGATOR_ACCOUNT_ID).get();
			CurrentContext currentContext = new CurrentContext.Builder(navigator, Locale.US,
					ZoneId.of("America/New_York")).build();
			UUID careEncounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);
			String appointmentEmailAddress = database.queryForObject("""
					SELECT email_address
					FROM appointment
					WHERE appointment_id=?
					""", String.class, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID).get();

			CareEncounter originalCareEncounter = careEncounterService.findCareEncounterByIdForInstitutionId(
					careEncounterId, InstitutionId.COBALT).get();
			assertEquals(appointmentEmailAddress, originalCareEncounter.getEmailAddress());

			currentContextExecutor.execute(currentContext, () -> {
				ApiResponse response = careEncounterResource.updateCareEncounter(careEncounterId,
						"{\"emailAddress\":\"  Navigator.Contact@Example.com  \"}");
				Map<String, Object> model = (Map<String, Object>) response.model().get();
				CareEncounterApiResponse updatedCareEncounter = (CareEncounterApiResponse) model.get("careEncounter");
				assertEquals("navigator.contact@example.com", updatedCareEncounter.getEmailAddress());
			});
			assertEquals(appointmentEmailAddress, database.queryForObject("""
					SELECT email_address
					FROM appointment
					WHERE appointment_id=?
					""", String.class, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID).get());

			assertThrows(ValidationException.class, () -> currentContextExecutor.execute(currentContext,
					() -> careEncounterResource.updateCareEncounter(careEncounterId,
							"{\"emailAddress\":\"invalid-email-address\"}")));
			assertEquals("navigator.contact@example.com", database.queryForObject("""
					SELECT email_address
					FROM care_encounter
					WHERE care_encounter_id=?
					""", String.class, careEncounterId).get());

			UUID closedCareEncounterId = careEncounterIdForAppointment(database,
					CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID);
			CareEncounter closedCareEncounter = careEncounterService.closeCareEncounter(closedCareEncounterId,
					InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID);
			String closedEncounterEmailAddress = closedCareEncounter.getEmailAddress();
			ValidationException closedEncounterException = assertThrows(ValidationException.class,
					() -> currentContextExecutor.execute(currentContext,
							() -> careEncounterResource.updateCareEncounter(closedCareEncounterId,
									"{\"emailAddress\":\"closed.encounter@example.com\"}")));
			assertTrue(closedEncounterException.getFieldErrors().stream()
					.anyMatch(fieldError -> fieldError.getField().equals("careEncounterStatusId")));
			assertEquals(closedEncounterEmailAddress, careEncounterService.findCareEncounterByIdForInstitutionId(
					closedCareEncounterId, InstitutionId.COBALT).get().getEmailAddress());
		});
	}

	@Test
	public void encounterEmailCorrectionUpdatesPendingFollowUpAndRemovingEmailCancelsIt() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CareEncounterService careEncounterService = app.getInjector().getInstance(CareEncounterService.class);
			CareEncounterResource careEncounterResource = app.getInjector().getInstance(CareEncounterResource.class);
			MessageService messageService = app.getInjector().getInstance(MessageService.class);
			EmailMessageSerializer emailMessageSerializer = app.getInjector().getInstance(EmailMessageSerializer.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			Account navigator = accountService.findAccountById(CARE_NAVIGATOR_ACCOUNT_ID).get();
			ZoneId timeZone = ZoneId.of("America/New_York");
			CurrentContext currentContext = new CurrentContext.Builder(navigator, Locale.US, timeZone).build();
			UUID careEncounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID);
			CreateCareEncounterScheduledMessageRequest request = scheduledFollowUpRequest(
					LocalDate.now(timeZone).plusDays(1), LocalTime.of(9, 30), "<p>Private follow-up resources.</p>");
			CareEncounterScheduledMessage originalMessage = careEncounterService.createCareEncounterScheduledMessage(
					careEncounterId, InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID, request);

			currentContextExecutor.execute(currentContext, () -> careEncounterResource.updateCareEncounter(
					careEncounterId, "{\"emailAddress\":\"  Corrected.Patient@Example.com  \"}"));

			CareEncounterScheduledMessage correctedMessage = careEncounterService.findCareEncounterScheduledMessageById(
					careEncounterId, originalMessage.getCareEncounterScheduledMessageId()).get();
			assertEquals("corrected.patient@example.com",
					careEncounterService.findCareEncounterByIdForInstitutionId(
							careEncounterId, InstitutionId.COBALT).get().getEmailAddress());
			assertEquals("corrected.patient@example.com", correctedMessage.getRecipientEmailAddress());
			assertEquals(ScheduledMessageStatusId.PENDING, correctedMessage.getScheduledMessageStatusId());
			assertEquals(originalMessage.getScheduledMessageId(), correctedMessage.getScheduledMessageId());
			assertEquals(originalMessage.getMessageId(), correctedMessage.getMessageId());
			assertEquals(originalMessage.getScheduledAt(), correctedMessage.getScheduledAt());
			assertEquals(originalMessage.getTimeZone(), correctedMessage.getTimeZone());
			assertEquals(originalMessage.getCustomEmailText(), correctedMessage.getCustomEmailText());
			assertEquals(originalMessage.getEmailSubject(), correctedMessage.getEmailSubject());
			assertEquals(originalMessage.getEmailBody(), correctedMessage.getEmailBody());

			ScheduledMessage scheduledMessage = messageService.findScheduledMessageById(
					correctedMessage.getScheduledMessageId()).get();
			EmailMessage pendingEmail = emailMessageSerializer.deserializeMessage(scheduledMessage.getSerializedMessage());
			assertEquals(List.of("corrected.patient@example.com"), pendingEmail.getToAddresses());
			assertEquals(originalMessage.getMessageId(), pendingEmail.getMessageId());

			currentContextExecutor.execute(currentContext, () -> careEncounterResource.updateCareEncounter(
					careEncounterId, "{\"emailAddress\":null}"));
			assertNull(careEncounterService.findCareEncounterByIdForInstitutionId(
					careEncounterId, InstitutionId.COBALT).get().getEmailAddress());
			CareEncounterScheduledMessage canceledMessage = careEncounterService.findCareEncounterScheduledMessageById(
					careEncounterId, originalMessage.getCareEncounterScheduledMessageId()).get();
			assertEquals(ScheduledMessageStatusId.CANCELED, canceledMessage.getScheduledMessageStatusId());
			assertNotNull(canceledMessage.getCanceledAt());
		});
	}

	@Test
	public void navigatorCanCreateMultipleEncounterNotesAndEditAnEarlierNote() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CareEncounterService careEncounterService = app.getInjector().getInstance(CareEncounterService.class);
			UUID careEncounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);
			int originalNoteCount = careEncounterService.findCareEncounterNotesByCareEncounterId(careEncounterId).size();

			CreateCareEncounterNoteRequest firstRequest = new CreateCareEncounterNoteRequest();
			firstRequest.setNote("  First follow-up note.  ");
			CareEncounterNote firstNote = careEncounterService.createCareEncounterNote(
					careEncounterId, InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID, firstRequest);

			CreateCareEncounterNoteRequest secondRequest = new CreateCareEncounterNoteRequest();
			secondRequest.setNote("Second follow-up note.");
			CareEncounterNote secondNote = careEncounterService.createCareEncounterNote(
					careEncounterId, InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID, secondRequest);

			assertEquals(originalNoteCount + 2,
					careEncounterService.findCareEncounterNotesByCareEncounterId(careEncounterId).size());
			assertEquals("First follow-up note.", firstNote.getNote());
			assertEquals("Second follow-up note.", secondNote.getNote());

			UpdateCareEncounterNoteRequest updateRequest = new UpdateCareEncounterNoteRequest();
			updateRequest.setNote("Updated first follow-up note.");
			CareEncounterNote updatedFirstNote = careEncounterService.updateCareEncounterNote(
					careEncounterId, firstNote.getCareEncounterNoteId(), InstitutionId.COBALT,
					CARE_NAVIGATOR_ACCOUNT_ID, updateRequest);

			assertEquals("Updated first follow-up note.", updatedFirstNote.getNote());
			assertEquals("Second follow-up note.", careEncounterService.findCareEncounterNoteByIdAndCareEncounterId(
					secondNote.getCareEncounterNoteId(), careEncounterId).get().getNote());
			assertEquals(CARE_NAVIGATOR_ACCOUNT_ID, updatedFirstNote.getCreatedByAccountId());
			assertEquals(CARE_NAVIGATOR_ACCOUNT_ID, updatedFirstNote.getLastUpdatedByAccountId());

			CreateCareEncounterNoteRequest blankRequest = new CreateCareEncounterNoteRequest();
			blankRequest.setNote("   ");
			assertThrows(ValidationException.class, () -> careEncounterService.createCareEncounterNote(
					careEncounterId, InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID, blankRequest));

			assertTrue(careEncounterService.deleteCareEncounterNote(careEncounterId,
					firstNote.getCareEncounterNoteId(), InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID));
			assertTrue(careEncounterService.findCareEncounterNoteByIdAndCareEncounterId(
					firstNote.getCareEncounterNoteId(), careEncounterId).isEmpty());
			assertEquals(originalNoteCount + 1,
					careEncounterService.findCareEncounterNotesByCareEncounterId(careEncounterId).size());
			assertTrue(database.queryForObject("""
					SELECT deleted FROM care_encounter_note WHERE care_encounter_note_id=?
					""", Boolean.class, firstNote.getCareEncounterNoteId()).get());
			assertEquals(CARE_NAVIGATOR_ACCOUNT_ID, database.queryForObject("""
					SELECT deleted_by_account_id FROM care_encounter_note WHERE care_encounter_note_id=?
					""", UUID.class, firstNote.getCareEncounterNoteId()).get());
		});
	}

	@Test
	public void encounterFollowUpRequiresCurrentAppointmentAttendance() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CareEncounterService service = app.getInjector().getInstance(CareEncounterService.class);
			UUID encounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);
			ZoneId timeZone = ZoneId.of("America/New_York");
			int originalMessageCount = service.findCareEncounterScheduledMessagesByCareEncounterId(encounterId).size();
			CreateCareEncounterScheduledMessageRequest request = scheduledFollowUpRequest(
					LocalDate.now(timeZone).plusDays(1), LocalTime.of(9, 30), "<p>Premature follow-up.</p>");

			ValidationException exception = assertThrows(ValidationException.class,
					() -> service.createCareEncounterScheduledMessage(encounterId, InstitutionId.COBALT,
							CARE_NAVIGATOR_ACCOUNT_ID, request));

			assertTrue(exception.getFieldErrors().stream()
					.anyMatch(error -> error.getField().equals("attendanceStatusId")));
			assertEquals(originalMessageCount,
					service.findCareEncounterScheduledMessagesByCareEncounterId(encounterId).size());
		});
	}

	@Test
	public void encounterFollowUpAllowsPastScheduledTimeForImmediateProcessing() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CareEncounterService service = app.getInjector().getInstance(CareEncounterService.class);
			UUID encounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID);
			ZoneId timeZone = ZoneId.of("America/New_York");
			LocalDate scheduledDate = LocalDate.now(timeZone).minusDays(1);
			LocalTime scheduledTime = LocalTime.of(9, 30);
			CreateCareEncounterScheduledMessageRequest request = scheduledFollowUpRequest(
					scheduledDate, scheduledTime, "<p>Send these resources immediately.</p>");

			CareEncounterScheduledMessage message = service.createCareEncounterScheduledMessage(
					encounterId, InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID, request);

			assertEquals(scheduledDate, message.getScheduledAt().toLocalDate());
			assertEquals(scheduledTime, message.getScheduledAt().toLocalTime());
		});
	}

	@Test
	public void patientCancellationCancelsPendingEncounterFollowUp() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CareEncounterService service = app.getInjector().getInstance(CareEncounterService.class);
			UUID encounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID);
			ZoneId timeZone = ZoneId.of("America/New_York");
			CreateCareEncounterScheduledMessageRequest request = scheduledFollowUpRequest(
					LocalDate.now(timeZone).plusDays(1), LocalTime.of(9, 30), "<p>Pending resources.</p>");
			CareEncounterScheduledMessage message = service.createCareEncounterScheduledMessage(encounterId,
					InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID, request);
			assertEquals(ScheduledMessageStatusId.PENDING, message.getScheduledMessageStatusId());

			cancelAppointment(database, CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID,
					CARE_NAVIGATOR_ATTENDED_FIXTURE_PATIENT_ID, false);

			assertEquals("CLOSED", careEncounterStatus(database, encounterId));
			CareEncounterScheduledMessage canceledMessage = service.findCareEncounterScheduledMessageById(
					encounterId, message.getCareEncounterScheduledMessageId()).get();
			assertEquals(ScheduledMessageStatusId.CANCELED, canceledMessage.getScheduledMessageStatusId());
			assertNotNull(canceledMessage.getCanceledAt());
			assertFalse(canceledMessage.getDeleted());
		});
	}

	@Test
	public void attendanceCorrectionCancelsPendingEncounterFollowUp() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CareEncounterService service = app.getInjector().getInstance(CareEncounterService.class);
			UUID encounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID);
			ZoneId timeZone = ZoneId.of("America/New_York");
			CreateCareEncounterScheduledMessageRequest request = scheduledFollowUpRequest(
					LocalDate.now(timeZone).plusDays(1), LocalTime.of(9, 30), "<p>Pending resources.</p>");
			CareEncounterScheduledMessage message = service.createCareEncounterScheduledMessage(encounterId,
					InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID, request);

			assertEquals(1, database.execute("""
					UPDATE appointment
					SET attendance_status_id='MISSED'
					WHERE appointment_id=?
					""", CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID));

			assertEquals("OPEN", careEncounterStatus(database, encounterId));
			CareEncounterScheduledMessage canceledMessage = service.findCareEncounterScheduledMessageById(
					encounterId, message.getCareEncounterScheduledMessageId()).get();
			assertEquals(ScheduledMessageStatusId.CANCELED, canceledMessage.getScheduledMessageStatusId());
			assertNotNull(canceledMessage.getCanceledAt());
		});
	}

	@Test
	public void encounterFollowUpCanBePreviewedEditedAndSoftDeletedBeforeClosure() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CareEncounterService service = app.getInjector().getInstance(CareEncounterService.class);
			CareEncounterResource resource = app.getInjector().getInstance(CareEncounterResource.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			Account navigator = accountService.findAccountById(CARE_NAVIGATOR_ACCOUNT_ID).get();
			UUID encounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID);
			ZoneId timeZone = ZoneId.of("America/New_York");

			PreviewCareEncounterScheduledMessageRequest previewRequest = new PreviewCareEncounterScheduledMessageRequest();
			previewRequest.setCareEncounterScheduledMessageTypeId(CareEncounterScheduledMessageTypeId.FOLLOW_UP);
			previewRequest.setCustomEmailText("<p>Here is your <strong>follow-up</strong>.</p><script>bad()</script>");
			RenderedEmailMessage preview = service.previewCareEncounterScheduledMessage(
					encounterId, InstitutionId.COBALT, previewRequest);
			assertTrue(preview.getEmailSubject().contains("Follow-up"));
			assertTrue(preview.getEmailBody().contains("<!DOCTYPE html"));
			assertTrue(preview.getEmailBody().contains(
					"width:600px; max-width:600px; background-color:#FFFFFF; border-radius:8px"));
			assertTrue(preview.getEmailBody().contains("logo@2x.jpg"));
			assertTrue(preview.getEmailBody().contains("<strong>follow-up</strong>"));
			assertFalse(preview.getEmailBody().contains("<script>"));
			assertTrue(preview.getEmailBody().indexOf("<strong>follow-up</strong>")
					< preview.getEmailBody().indexOf("Please do not reply to this email."));

			CreateCareEncounterScheduledMessageRequest createRequest = scheduledFollowUpRequest(
					LocalDate.now(timeZone).plusDays(1), LocalTime.of(9, 30), "<p>Original resources.</p>");
			CareEncounterScheduledMessage created = service.createCareEncounterScheduledMessage(encounterId,
					InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID, createRequest);
			assertEquals(ScheduledMessageStatusId.PENDING, created.getScheduledMessageStatusId());
			assertEquals(ScheduledMessageSourceId.MANUAL, created.getScheduledMessageSourceId());
			assertEquals(CARE_NAVIGATOR_ACCOUNT_ID, created.getScheduledByAccountId());
			assertFalse(created.getDeleted());
			assertThrows(ValidationException.class, () -> service.createCareEncounterScheduledMessage(
					encounterId, InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID, createRequest));

			CreateCareEncounterScheduledMessageRequest updateRequest = scheduledFollowUpRequest(
					LocalDate.now(timeZone).plusDays(2), LocalTime.of(10, 15), "<p>Updated resources.</p>");
			CareEncounterScheduledMessage updated = service.updateCareEncounterScheduledMessage(encounterId,
					created.getCareEncounterScheduledMessageId(), InstitutionId.COBALT,
					CARE_NAVIGATOR_ACCOUNT_ID, updateRequest);
			assertEquals(created.getScheduledMessageId(), updated.getScheduledMessageId());
			assertEquals(created.getMessageId(), updated.getMessageId());
			assertEquals("<p>Updated resources.</p>", updated.getCustomEmailText());
			assertEquals(LocalDate.now(timeZone).plusDays(2), updated.getScheduledAt().toLocalDate());

			ValidationException closeWhilePending = assertThrows(ValidationException.class,
					() -> service.closeCareEncounter(encounterId, InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID));
			assertTrue(closeWhilePending.getFieldErrors().stream()
					.anyMatch(error -> error.getField().equals("careEncounterScheduledMessageId")));

			CareEncounterScheduledMessage deleted = service.deleteCareEncounterScheduledMessage(encounterId,
					created.getCareEncounterScheduledMessageId(), InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID);
			assertTrue(deleted.getDeleted());
			assertNotNull(deleted.getDeletedAt());
			assertEquals(CARE_NAVIGATOR_ACCOUNT_ID, deleted.getDeletedByAccountId());
			assertEquals(ScheduledMessageStatusId.CANCELED, deleted.getScheduledMessageStatusId());
			assertEquals(1, service.findCareEncounterScheduledMessagesByCareEncounterId(encounterId).size());
			currentContextExecutor.execute(new CurrentContext.Builder(navigator, Locale.US, timeZone).build(), () -> {
				Map<String, Object> detailModel = (Map<String, Object>) resource.careEncounter(encounterId).model().get();
				CareEncounterApiResponse detail = (CareEncounterApiResponse) detailModel.get("careEncounter");
				assertEquals(1, detail.getCareEncounterScheduledMessages().size());
				assertTrue(detail.getCareEncounterScheduledMessages().get(0).getDeleted());
				assertEquals(ScheduledMessageStatusId.CANCELED,
						detail.getCareEncounterScheduledMessages().get(0).getScheduledMessageStatusId());
			});

			CareEncounter closed = service.closeCareEncounter(encounterId, InstitutionId.COBALT,
					CARE_NAVIGATOR_ACCOUNT_ID);
			assertEquals(CareEncounterStatusId.CLOSED, closed.getCareEncounterStatusId());
			assertThrows(ValidationException.class, () -> service.updateCareEncounterScheduledMessage(encounterId,
					created.getCareEncounterScheduledMessageId(), InstitutionId.COBALT,
					CARE_NAVIGATOR_ACCOUNT_ID, updateRequest));
		});
	}

	@Test
	public void encounterNotesBecomeReadOnlyWhenEncounterCloses() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CareEncounterService service = app.getInjector().getInstance(CareEncounterService.class);
			UUID encounterId = careEncounterIdForAppointment(database, CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID);
			CreateCareEncounterNoteRequest createRequest = new CreateCareEncounterNoteRequest();
			createRequest.setNote("Final open note.");
			CareEncounterNote note = service.createCareEncounterNote(encounterId, InstitutionId.COBALT,
					CARE_NAVIGATOR_ACCOUNT_ID, createRequest);
			service.closeCareEncounter(encounterId, InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID);

			UpdateCareEncounterNoteRequest updateRequest = new UpdateCareEncounterNoteRequest();
			updateRequest.setNote("Too late.");
			assertThrows(ValidationException.class, () -> service.updateCareEncounterNote(encounterId,
					note.getCareEncounterNoteId(), InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID, updateRequest));
			assertThrows(ValidationException.class, () -> service.deleteCareEncounterNote(encounterId,
					note.getCareEncounterNoteId(), InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID));
			assertThrows(ValidationException.class, () -> service.createCareEncounterNote(encounterId,
					InstitutionId.COBALT, CARE_NAVIGATOR_ACCOUNT_ID, createRequest));
		});
	}

	@Test
	public void encounterResponsesUseLatestAppointmentAndIncludeInactiveAppointmentsInHistory() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CareEncounterService careEncounterService = app.getInjector().getInstance(CareEncounterService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			CareEncounterResource careEncounterResource = app.getInjector().getInstance(CareEncounterResource.class);
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
				assertFalse(response.getCareEncounterNotes().isEmpty());
				Map<String, Object> serializedDetail = new JsonMapper().toMap(response);
				assertTrue(serializedDetail.containsKey("appointment"));
				assertTrue(serializedDetail.containsKey("appointmentHistory"));
				assertTrue(serializedDetail.containsKey("emailAddress"));
				assertTrue(serializedDetail.containsKey("careEncounterNotes"));
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

				Map<String, Object> detailModel = (Map<String, Object>) careEncounterResource
						.careEncounter(activeCareEncounterId).model().get();
				List<CareEncounterListApiResponse> encounterHistory =
						(List<CareEncounterListApiResponse>) detailModel.get("careEncounterHistory");
				assertTrue(encounterHistory.stream()
						.anyMatch(item -> item.getCareEncounterId().equals(activeCareEncounterId)));
				assertEquals(encounterHistory.size(), detailModel.get("careEncounterHistoryTotalCount"));
				assertFalse(detailModel.containsKey("otherCareEncounters"));
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
				assertEquals(5, questionResults.size());
				assertNull(questionResults.get(3).getScreeningAnswerResults().get(0).getText());
				assertEquals(NAVIGATOR_CONTEXT_FIXTURE_TEXT,
						questionResults.get(4).getScreeningAnswerResults().stream()
								.filter(answer -> CARE_NAVIGATOR_OTHER_SUPPORT_ANSWER_OPTION_ID.equals(
										answer.getScreeningAnswerOptionId()))
								.findFirst().get().getText());

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
	public void assigningCareNavigatorRoleDoesNotConvertExistingAppointments() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			UUID providerId = createActiveProvider(database, "Patient Triage Provider");

			UUID existingAppointmentId = UUID.randomUUID();
			cloneAsActiveAppointmentForProviderAndAccount(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID,
					existingAppointmentId, providerId, CARE_NAVIGATOR_PATIENT_CANCELED_FIXTURE_PATIENT_ID, 40);
			assertEquals(Long.valueOf(1L), database.queryForObject("""
					SELECT COUNT(*)
					FROM appointment
					WHERE appointment_id=?
					AND care_encounter_id IS NULL
					""", Long.class, existingAppointmentId).get());

			assertEquals(1, database.execute("""
					INSERT INTO provider_support_role (provider_id, support_role_id)
					VALUES (?, 'CARE_NAVIGATOR')
					""", providerId));
			assertTrue(database.queryForObject("""
					SELECT virtual_appointments_only
					FROM provider
					WHERE provider_id=?
					""", Boolean.class, providerId).get());
			assertEquals(Long.valueOf(1L), database.queryForObject("""
					SELECT COUNT(*)
					FROM appointment
					WHERE appointment_id=?
					AND care_encounter_id IS NULL
					""", Long.class, existingAppointmentId).get());

			UUID newAppointmentId = UUID.randomUUID();
			cloneAsActiveAppointmentForProviderAndAccount(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID,
					newAppointmentId, providerId, CARE_NAVIGATOR_PATIENT_CANCELED_FIXTURE_PATIENT_ID, 41);
			assertEquals(Long.valueOf(1L), database.queryForObject("""
					SELECT COUNT(*)
					FROM appointment
					WHERE appointment_id=?
					AND care_encounter_id IS NOT NULL
					""", Long.class, newAppointmentId).get());
			assertEquals(Long.valueOf(1L), database.queryForObject("""
					SELECT COUNT(*)
					FROM appointment
					WHERE appointment_id=?
					AND care_encounter_id IS NULL
					""", Long.class, existingAppointmentId).get());
		});
	}

	@Test
	public void staleNavigatorMappingDoesNotAuthorizeAppointmentAccess() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AuthorizationService authorizationService = app.getInjector().getInstance(AuthorizationService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Account navigator = accountService.findAccountById(CARE_NAVIGATOR_ACCOUNT_ID).get();
			Account patient = accountService.findAccountById(
					CARE_NAVIGATOR_PATIENT_CANCELED_FIXTURE_PATIENT_ID).get();
			UUID providerId = createActiveProvider(database, "Secondary Care Navigator Provider");

			assertEquals(1, database.execute("""
					INSERT INTO provider_support_role (provider_id, support_role_id)
					VALUES (?, 'CARE_NAVIGATOR')
					""", providerId));
			assertEquals(1, database.execute("""
					INSERT INTO care_navigator_provider_account (provider_id, account_id, display_order)
					VALUES (?, ?, 2)
					""", providerId, CARE_NAVIGATOR_ACCOUNT_ID));
			UUID appointmentId = UUID.randomUUID();
			cloneAsActiveAppointmentForProviderAndAccount(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID,
					appointmentId, providerId, CARE_NAVIGATOR_PATIENT_CANCELED_FIXTURE_PATIENT_ID, 42);
			Appointment appointment = appointmentService.findAppointmentById(appointmentId).get();
			assertNotNull(appointment.getCareEncounterId());
			assertNotEquals(navigator.getProviderId(), providerId);
			assertTrue(appointmentService.isCareNavigatorAccountMappedToProvider(
					CARE_NAVIGATOR_ACCOUNT_ID, providerId));
			assertTrue(authorizationService.canViewAppointment(appointment, navigator));
			assertTrue(authorizationService.canCancelAppointment(appointment, navigator, patient));

			assertEquals(1, database.execute("UPDATE provider SET active=FALSE WHERE provider_id=?", providerId));
			assertEquals(Long.valueOf(1L), database.queryForObject("""
					SELECT COUNT(*)
					FROM care_navigator_provider_account
					WHERE provider_id=? AND account_id=?
					""", Long.class, providerId, CARE_NAVIGATOR_ACCOUNT_ID).get());
			assertFalse(appointmentService.isCareNavigatorAccountMappedToProvider(
					CARE_NAVIGATOR_ACCOUNT_ID, providerId));
			assertFalse(authorizationService.canViewAppointment(appointment, navigator));
			assertFalse(authorizationService.canCancelAppointment(appointment, navigator, patient));

			assertEquals(1, database.execute("UPDATE provider SET active=TRUE WHERE provider_id=?", providerId));
			assertTrue(appointmentService.isCareNavigatorAccountMappedToProvider(
					CARE_NAVIGATOR_ACCOUNT_ID, providerId));
			assertTrue(authorizationService.canCancelAppointment(appointment, navigator, patient));

			assertEquals(1, database.execute("""
					DELETE FROM provider_support_role
					WHERE provider_id=? AND support_role_id='CARE_NAVIGATOR'
					""", providerId));
			assertEquals(Long.valueOf(1L), database.queryForObject("""
					SELECT COUNT(*)
					FROM care_navigator_provider_account
					WHERE provider_id=? AND account_id=?
					""", Long.class, providerId, CARE_NAVIGATOR_ACCOUNT_ID).get());
			assertFalse(appointmentService.isCareNavigatorAccountMappedToProvider(
					CARE_NAVIGATOR_ACCOUNT_ID, providerId));
			assertFalse(authorizationService.canViewAppointment(appointment, navigator));
			assertFalse(authorizationService.canCancelAppointment(appointment, navigator, patient));
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

	protected void cloneAsActiveAppointmentForProviderAndAccount(Database database,
														 UUID sourceAppointmentId,
														 UUID appointmentId,
														 UUID providerId,
														 UUID accountId,
														 int daysAfterSource) {
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
					?,
					?,
					NULL,
					NULL,
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
				""", appointmentId, providerId, accountId, daysAfterSource, daysAfterSource, sourceAppointmentId));
	}

	protected List<EmailMessage> enqueuedEmailsForAppointment(Database database,
																					 EmailMessageSerializer emailMessageSerializer,
																					 UUID appointmentId) {
		return database.queryForList("""
				SELECT *
				FROM message_log
				WHERE message_type_id='EMAIL'
				AND serialized_message->'messageContext'->>'appointmentId'=?
				ORDER BY enqueued, message_id
				""", MessageLog.class, appointmentId.toString()).stream()
				.map(messageLog -> emailMessageSerializer.deserializeMessage(messageLog.getSerializedMessage()))
				.toList();
	}

	protected EmailMessage emailWithTemplate(List<EmailMessage> emailMessages,
																					EmailMessageTemplate emailMessageTemplate) {
		return emailMessages.stream()
				.filter(emailMessage -> emailMessage.getMessageTemplate() == emailMessageTemplate)
				.findFirst()
				.orElseThrow(() -> new AssertionError("Missing email template " + emailMessageTemplate));
	}

	protected void assertCalendarAttachment(EmailMessage emailMessage,
																			 String expectedLocation,
																			 String expectedOrganizerEmailAddress,
																			 String expectedMethod) {
		assertEquals(1, emailMessage.getEmailAttachments().size());
		String calendar = new String(emailMessage.getEmailAttachments().get(0).getData(), StandardCharsets.UTF_8)
				.replace("\r\n ", "")
				.replace("\r\n\t", "");
		assertTrue(calendar.contains(expectedMethod));
		assertTrue(calendar.contains("LOCATION:" + expectedLocation));
		assertTrue(calendar.contains("mailto:" + expectedOrganizerEmailAddress));
	}

	protected UUID createActiveProvider(Database database, String name) {
		UUID providerId = UUID.randomUUID();
		String uniqueSuffix = providerId.toString();
		assertEquals(1, database.execute("""
				INSERT INTO provider (
					provider_id,
					institution_id,
					name,
					url_name,
					email_address,
					locale,
					time_zone,
					scheduling_system_id,
					videoconference_platform_id,
					videoconference_url,
					virtual_appointments_only
				)
				VALUES (?, 'COBALT', ?, ?, ?, 'en-US', 'America/New_York',
					'COBALT', 'SWITCHBOARD', 'https://example.com/care-navigation', FALSE)
				""", providerId, name, String.format("care-navigator-%s", uniqueSuffix),
				String.format("care-navigator-%s@example.com", uniqueSuffix)));
		return providerId;
	}

	protected CreateCareEncounterScheduledMessageRequest scheduledFollowUpRequest(LocalDate date,
																												 LocalTime time,
																												 String customEmailText) {
		CreateCareEncounterScheduledMessageRequest request = new CreateCareEncounterScheduledMessageRequest();
		request.setCareEncounterScheduledMessageTypeId(CareEncounterScheduledMessageTypeId.FOLLOW_UP);
		request.setScheduledAtDate(date);
		request.setScheduledAtTime(time);
		request.setCustomEmailText(customEmailText);
		return request;
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
