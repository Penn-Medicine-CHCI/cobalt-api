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
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.integration.epic.request.GetProviderScheduleRequest;
import com.cobaltplatform.api.integration.epic.request.ScheduleAppointmentWithInsuranceRequest;
import com.cobaltplatform.api.integration.epic.response.CancelAppointmentResponse;
import com.cobaltplatform.api.integration.epic.response.GetProviderScheduleResponse;
import com.cobaltplatform.api.integration.epic.response.ScheduleAppointmentWithInsuranceResponse;
import com.cobaltplatform.api.model.api.request.CreateAppointmentRequest;
import com.cobaltplatform.api.model.api.request.CreateAppointmentRequest.BookingExperienceId;
import com.cobaltplatform.api.model.api.request.UpdateAppointmentRequest;
import com.cobaltplatform.api.model.api.request.UpdateAppointmentTypeRequest;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityId;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.pyranid.Database;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Database-backed booking and persistence coverage. The Epic client records calls
 * locally, and every fixture and successful booking is rolled back.
 */
public class AppointmentServiceStartTimeAlignmentTests {
	private static final LocalTime OFF_GRID_TIME = LocalTime.of(10, 15);
	private static final LocalTime ALIGNED_TIME = LocalTime.of(10, 30);

	@Test
	public void epicV1RejectsOffGridBookingBeforeEpicCallsOrAppointmentWrites() {
		assertEpicOffGridBookingRejected(false);
	}

	@Test
	public void epicV2RejectsOffGridBookingBeforeEpicCallsOrAppointmentWrites() {
		assertEpicOffGridBookingRejected(true);
	}

	private void assertEpicOffGridBookingRejected(boolean bookingV2Enabled) {
		RecordingEpicClient epicClient = new RecordingEpicClient(OFF_GRID_TIME);
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			EpicBookingFixture fixture = createEpicFixture(database, bookingV2Enabled);
			Account before = accountService.findAccountById(fixture.accountId).get();

			assertTimeslotUnavailable(() -> appointmentService.createAppointment(
					requestFor(fixture, fixture.providerId, fixture.appointmentTypeId, OFF_GRID_TIME)));

			assertEquals(0, epicClient.scheduleLookupCount);
			assertEquals(0, epicClient.scheduleCount);
			assertEquals(0L, appointmentCount(database, fixture.accountId));
			Account after = accountService.findAccountById(fixture.accountId).get();
			assertEquals(before.getFirstName(), after.getFirstName());
			assertEquals(before.getLastName(), after.getLastName());
			assertEquals(before.getEmailAddress(), after.getEmailAddress());
			assertEquals(before.getPhoneNumber(), after.getPhoneNumber());
		}, epicModule(epicClient));
	}

	@Test
	public void epicAlignedBookingAndUnconfiguredProviderTypePairsRemainBookable() {
		RecordingEpicClient epicClient = new RecordingEpicClient(ALIGNED_TIME);
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			EpicBookingFixture fixture = createEpicFixture(database, false);

			UUID alignedAppointmentId = appointmentService.createAppointment(
					requestFor(fixture, fixture.providerId, fixture.appointmentTypeId, ALIGNED_TIME));
			assertEquals(fixture.bookingDate.atTime(ALIGNED_TIME),
					appointmentService.findAppointmentById(alignedAppointmentId).get().getStartTime());

			// The same provider's other type and the same type's other provider
			// have null settings; neither inherits the configured pair's interval.
			epicClient.openTime = OFF_GRID_TIME;
			UUID otherTypeAppointmentId = appointmentService.createAppointment(
					requestFor(fixture, fixture.providerId, fixture.otherAppointmentTypeId, OFF_GRID_TIME));
			UUID otherProviderAppointmentId = appointmentService.createAppointment(
					requestFor(fixture, fixture.otherProviderId, fixture.appointmentTypeId, OFF_GRID_TIME));
			assertNotNull(otherTypeAppointmentId);
			assertNotNull(otherProviderAppointmentId);
			assertEquals(3, epicClient.scheduleLookupCount);
			assertEquals(3, epicClient.scheduleCount);
			assertEquals(3L, appointmentCount(database, fixture.accountId));
		}, epicModule(epicClient));
	}

	@Test
	public void epicOffGridRescheduleLeavesOriginalActiveAndMakesNoEpicCalls() {
		RecordingEpicClient epicClient = new RecordingEpicClient(ALIGNED_TIME);
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			EpicBookingFixture fixture = createEpicFixture(database, false);
			UUID originalAppointmentId = appointmentService.createAppointment(
					requestFor(fixture, fixture.providerId, fixture.appointmentTypeId, ALIGNED_TIME));
			epicClient.resetCounts();
			epicClient.openTime = OFF_GRID_TIME;
			UpdateAppointmentRequest request = new UpdateAppointmentRequest();
			populateRequest(request, fixture, fixture.providerId, fixture.appointmentTypeId, OFF_GRID_TIME);
			request.setAppointmentId(originalAppointmentId);

			assertTimeslotUnavailable(() -> appointmentService.rescheduleAppointment(request));

			Appointment originalAppointment = appointmentService.findAppointmentById(originalAppointmentId).get();
			assertFalse(originalAppointment.getCanceled());
			assertFalse(originalAppointment.getCanceledForReschedule());
			assertNull(originalAppointment.getRescheduledAppointmentId());
			assertEquals(fixture.bookingDate.atTime(ALIGNED_TIME), originalAppointment.getStartTime());
			assertEquals(1L, appointmentCount(database, fixture.accountId));
			assertEquals(0, epicClient.scheduleLookupCount);
			assertEquals(0, epicClient.scheduleCount);
			assertEquals(0, epicClient.cancelCount);
		}, epicModule(epicClient));
	}

	@Test
	public void nativeBookingIgnoresEpicStartTimeAlignmentSetting() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			Account account = app.getInjector().getInstance(AccountService.class)
					.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			AppointmentServiceNativeBookingTests.NativeBookingFixture fixture =
					AppointmentServiceNativeBookingTests.createFixture(database, appointmentService,
							app.getInjector().getInstance(AvailabilityService.class), account);
			database.execute("""
					UPDATE provider_appointment_type
					SET scheduling_start_time_interval_in_minutes=60
					WHERE provider_id=? AND appointment_type_id=?
					""", fixture.getProviderId(), fixture.getAppointmentTypeId());

			UUID appointmentId = appointmentService.createAppointment(
					AppointmentServiceNativeBookingTests.requestFor(fixture, ALIGNED_TIME));
			Appointment appointment = appointmentService.findAppointmentById(appointmentId).get();
			assertFalse(appointment.getCanceled());
			assertEquals(SchedulingSystemId.COBALT, appointment.getSchedulingSystemId());
			assertEquals(fixture.getBookingDate().atTime(ALIGNED_TIME), appointment.getStartTime());
		});
	}

	@Test
	public void appointmentTypeEditPreservesExistingPairIntervalAndIdentity() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			Account account = app.getInjector().getInstance(AccountService.class)
					.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			AppointmentServiceNativeBookingTests.NativeBookingFixture fixture =
					AppointmentServiceNativeBookingTests.createFixture(database, appointmentService,
							app.getInjector().getInstance(AvailabilityService.class), account);
			database.execute("""
					UPDATE provider_appointment_type
					SET scheduling_start_time_interval_in_minutes=30
					WHERE provider_id=? AND appointment_type_id=?
					""", fixture.getProviderId(), fixture.getAppointmentTypeId());
			UUID originalAssociationId = database.queryForObject("""
					SELECT provider_appointment_type_id FROM provider_appointment_type
					WHERE provider_id=? AND appointment_type_id=?
					""", UUID.class, fixture.getProviderId(), fixture.getAppointmentTypeId()).get();
			UpdateAppointmentTypeRequest request = new UpdateAppointmentTypeRequest();
			request.setProviderId(fixture.getProviderId());
			request.setAppointmentTypeId(fixture.getAppointmentTypeId());
			request.setSchedulingSystemId(SchedulingSystemId.COBALT);
			request.setVisitTypeId(VisitTypeId.FOLLOWUP);
			request.setName("Updated Alignment Test Visit");
			request.setDescription("Updated visit description");
			request.setDurationInMinutes(45L);
			request.setHexColor("#663399");
			request.setScreeningQuestions(Collections.emptyList());
			request.setPatientIntakeQuestions(Collections.emptyList());

			assertTrue(appointmentService.updateAppointmentType(request));

			assertEquals(List.of(fixture.getGridAppointmentTypeId(), fixture.getAppointmentTypeId()),
					appointmentService.findAppointmentTypesByProviderId(fixture.getProviderId()).stream()
							.map(appointmentType -> appointmentType.getAppointmentTypeId()).toList());
			assertEquals(originalAssociationId, database.queryForObject("""
					SELECT provider_appointment_type_id FROM provider_appointment_type
					WHERE provider_id=? AND appointment_type_id=?
					""", UUID.class, fixture.getProviderId(), fixture.getAppointmentTypeId()).get());
			assertEquals(Integer.valueOf(30), database.queryForObject("""
					SELECT scheduling_start_time_interval_in_minutes FROM provider_appointment_type
					WHERE provider_id=? AND appointment_type_id=?
					""", Integer.class, fixture.getProviderId(), fixture.getAppointmentTypeId()).get());
			assertFalse(database.queryForObject("""
					SELECT scheduling_start_time_interval_in_minutes IS NOT NULL FROM provider_appointment_type
					WHERE provider_id=? AND appointment_type_id=?
					""", Boolean.class, fixture.getProviderId(), fixture.getGridAppointmentTypeId()).get());
			assertEquals(Long.valueOf(45L), appointmentService.findAppointmentTypeById(
					fixture.getAppointmentTypeId()).get().getDurationInMinutes());
			assertEquals("Updated Alignment Test Visit", appointmentService.findAppointmentTypeById(
					fixture.getAppointmentTypeId()).get().getName());
		});
	}

	private static EpicBookingFixture createEpicFixture(Database database, boolean bookingV2Enabled) {
		EpicBookingFixture fixture = new EpicBookingFixture();
		fixture.bookingV2Enabled = bookingV2Enabled;
		// V2 is available only outside integrated care. Keep this change local
		// to the rollback fixture while retaining the recording Epic plugin.
		database.execute("""
				UPDATE institution SET booking_v2_enabled=?, integrated_care_enabled=?,
				    appointment_feedback_survey_enabled=FALSE
				WHERE institution_id='COBALT_IC'
				""", bookingV2Enabled, !bookingV2Enabled);
		database.execute("""
				INSERT INTO account (
				  account_id, institution_id, role_id, account_source_id, first_name, last_name,
				  email_address, phone_number, epic_patient_unique_id, epic_patient_unique_id_type
				) VALUES (?, 'COBALT_IC', 'PATIENT', 'EMAIL_PASSWORD', 'Alignment', 'Patient',
				  ?, '+12155550123', ?, 'UID')
				""", fixture.accountId, fixture.emailAddress, "test-patient-" + fixture.accountId);
		database.execute("""
				INSERT INTO account_email_verification (
				  account_email_verification_id, account_id, code, email_address, verified
				) VALUES (?, ?, '123456', ?, TRUE)
				""", UUID.randomUUID(), fixture.accountId, fixture.emailAddress);
		database.execute("""
				INSERT INTO epic_department (epic_department_id, institution_id, department_id, department_id_type, name)
				VALUES (?, 'COBALT_IC', ?, 'INTERNAL', 'Alignment test department')
				""", fixture.departmentId, "alignment-department-" + fixture.departmentId);
		for (UUID appointmentTypeId : List.of(fixture.appointmentTypeId, fixture.otherAppointmentTypeId))
			database.execute("""
					INSERT INTO appointment_type (
					  appointment_type_id, name, duration_in_minutes, scheduling_system_id,
					  epic_visit_type_id, epic_visit_type_id_type, visit_type_id
					) VALUES (?, 'Alignment test appointment', 60, 'EPIC', '1008', 'INTERNAL', 'INITIAL')
					""", appointmentTypeId);
		for (UUID providerId : List.of(fixture.providerId, fixture.otherProviderId)) {
			database.execute("""
					INSERT INTO provider (
					  provider_id, institution_id, name, url_name, scheduling_system_id,
					  videoconference_platform_id, epic_provider_id, epic_provider_id_type, time_zone
					) VALUES (?, 'COBALT_IC', 'Alignment test provider', ?, 'EPIC', 'EXTERNAL', ?,
					  'INTERNAL', 'America/New_York')
					""", providerId, "alignment-provider-" + providerId, providerId.toString());
			for (LocalTime time : List.of(OFF_GRID_TIME, ALIGNED_TIME))
				database.execute("""
						INSERT INTO provider_availability (
						  provider_availability_id, provider_id, date_time, appointment_type_id, epic_department_id
						) VALUES (?, ?, ?, ?, ?)
						""", UUID.randomUUID(), providerId, fixture.bookingDate.atTime(time),
						fixture.appointmentTypeId, fixture.departmentId);
		}
		database.execute("""
				INSERT INTO provider_appointment_type (
				  provider_id, appointment_type_id, display_order, scheduling_start_time_interval_in_minutes
				) VALUES (?, ?, 1, 30), (?, ?, 2, NULL), (?, ?, 1, NULL)
				""", fixture.providerId, fixture.appointmentTypeId,
				fixture.providerId, fixture.otherAppointmentTypeId, fixture.otherProviderId, fixture.appointmentTypeId);
		UUID patientOrderImportId = UUID.randomUUID();
		database.execute("""
				INSERT INTO patient_order_import (
				  patient_order_import_id, patient_order_import_type_id, institution_id, raw_order
				) VALUES (?, 'CSV', 'COBALT_IC', '{}')
				""", patientOrderImportId);
		database.execute("""
				INSERT INTO patient_order (
				  patient_order_id, patient_order_import_id, institution_id, patient_last_name, patient_first_name,
				  patient_mrn, patient_unique_id, patient_unique_id_type, order_id, epic_department_id
				) VALUES (?, ?, 'COBALT_IC', 'Patient', 'Alignment', ?, ?, 'INTERNAL', ?, ?)
				""", fixture.patientOrderId, patientOrderImportId, "test-mrn-" + fixture.patientOrderId,
				"test-patient-" + fixture.accountId, "test-order-" + fixture.patientOrderId, fixture.departmentId);
		database.execute("""
				INSERT INTO audit_log_event (audit_log_event_id, description)
				VALUES ('EPIC_APPOINTMENT_CREATE', 'Create an Epic appointment')
				ON CONFLICT (audit_log_event_id) DO NOTHING
				""");
		return fixture;
	}

	private static CreateAppointmentRequest requestFor(EpicBookingFixture fixture,
			UUID providerId, UUID appointmentTypeId, LocalTime time) {
		CreateAppointmentRequest request = new CreateAppointmentRequest();
		populateRequest(request, fixture, providerId, appointmentTypeId, time);
		return request;
	}

	private static void populateRequest(CreateAppointmentRequest request, EpicBookingFixture fixture,
			UUID providerId, UUID appointmentTypeId, LocalTime time) {
		request.setAccountId(fixture.accountId);
		request.setCreatedByAcountId(fixture.accountId);
		request.setProviderId(providerId);
		request.setAppointmentTypeId(appointmentTypeId);
		request.setPatientOrderId(fixture.patientOrderId);
		request.setDate(fixture.bookingDate);
		request.setTime(time);
		request.setFirstName("Submitted");
		request.setLastName("Contact");
		request.setEmailAddress(fixture.emailAddress);
		request.setPhoneNumber("+12155550124");
		request.setBookingExperienceId(fixture.bookingV2Enabled ? BookingExperienceId.V2 : BookingExperienceId.V1);
		request.setAppointmentModalityId(ProviderAppointmentModalityId.VIRTUAL);
	}

	private static void assertTimeslotUnavailable(Runnable booking) {
		try {
			booking.run();
			fail("Expected an off-grid Epic start time to be rejected.");
		} catch (ValidationException e) {
			assertEquals(Boolean.TRUE, e.getMetadata().get("appointmentTimeslotUnavailable"));
		}
	}

	private static long appointmentCount(Database database, UUID accountId) {
		return database.queryForObject("SELECT count(*) FROM appointment WHERE account_id=?",
				Long.class, accountId).get();
	}

	private static Module epicModule(RecordingEpicClient epicClient) {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(PatientOrderProviderEligibilityTests.RecordingEpicClient.class).toInstance(epicClient);
				bind(EnterprisePluginProvider.class).to(
						PatientOrderProviderEligibilityTests.RecordingEnterprisePluginProvider.class);
			}
		};
	}

	private static class EpicBookingFixture {
		private final UUID accountId = UUID.randomUUID();
		private final UUID providerId = UUID.randomUUID();
		private final UUID otherProviderId = UUID.randomUUID();
		private final UUID appointmentTypeId = UUID.randomUUID();
		private final UUID otherAppointmentTypeId = UUID.randomUUID();
		private final UUID departmentId = UUID.randomUUID();
		private final UUID patientOrderId = UUID.randomUUID();
		private final LocalDate bookingDate = LocalDate.now(ZoneId.of("America/New_York")).plusDays(14);
		private final String emailAddress = "alignment-" + accountId + "@example.com";
		private boolean bookingV2Enabled;
	}

	private static class RecordingEpicClient extends PatientOrderProviderEligibilityTests.RecordingEpicClient {
		private LocalTime openTime;
		private int scheduleLookupCount;
		private int scheduleCount;
		private int cancelCount;

		private RecordingEpicClient(LocalTime openTime) {
			super(openTime);
			this.openTime = openTime;
		}

		@Nonnull
		@Override
		public LocalTime parseTimeAmPm(@Nonnull String time) {
			return openTime;
		}

		@Nonnull
		@Override
		public GetProviderScheduleResponse performGetProviderSchedule(@Nonnull GetProviderScheduleRequest request) {
			++scheduleLookupCount;
			return super.performGetProviderSchedule(request);
		}

		@Nonnull
		@Override
		public ScheduleAppointmentWithInsuranceResponse performScheduleAppointmentWithInsurance(
				@Nonnull ScheduleAppointmentWithInsuranceRequest request) {
			++scheduleCount;
			return super.performScheduleAppointmentWithInsurance(request);
		}

		@Nonnull
		@Override
		public CancelAppointmentResponse performCancelAppointment(
				@Nonnull com.cobaltplatform.api.integration.epic.request.CancelAppointmentRequest request) {
			++cancelCount;
			return new CancelAppointmentResponse();
		}

		private void resetCounts() {
			scheduleLookupCount = 0;
			scheduleCount = 0;
			cancelCount = 0;
		}
	}
}
