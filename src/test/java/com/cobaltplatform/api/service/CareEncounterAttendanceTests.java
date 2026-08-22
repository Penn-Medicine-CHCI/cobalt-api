/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.model.api.request.ChangeAppointmentAttendanceStatusRequest;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CareEncounterApiResponse;
import com.cobaltplatform.api.model.api.response.CareEncounterApiResponse.CareEncounterApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CareEncounterListApiResponse;
import com.cobaltplatform.api.model.api.response.CareEncounterListApiResponse.CareEncounterListApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.Appointment.AppointmentTimeStatusId;
import com.cobaltplatform.api.model.db.AttendanceStatus;
import com.cobaltplatform.api.model.db.AttendanceStatus.AttendanceStatusId;
import com.cobaltplatform.api.model.db.CareEncounter;
import com.cobaltplatform.api.model.db.CareEncounterStatus.CareEncounterStatusId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.cobaltplatform.api.web.resource.CareEncounterResource;
import com.pyranid.Database;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.response.ApiResponse;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.cobaltplatform.api.service.CareNavigatorBookingFixtureTests.CARE_NAVIGATOR_ACCOUNT_ID;
import static com.cobaltplatform.api.service.CareNavigatorBookingFixtureTests.CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID;
import static com.cobaltplatform.api.service.CareNavigatorBookingFixtureTests.CARE_NAVIGATOR_ACTIVE_FIXTURE_PATIENT_ID;
import static com.cobaltplatform.api.service.CareNavigatorBookingFixtureTests.CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID;
import static com.cobaltplatform.api.service.CareNavigatorBookingFixtureTests.CARE_NAVIGATOR_CANCELED_APPOINTMENT_ID;
import static com.cobaltplatform.api.service.CareNavigatorBookingFixtureTests.CARE_NAVIGATOR_REBOOKED_APPOINTMENT_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class CareEncounterAttendanceTests {
	@Test
	@SuppressWarnings("unchecked")
	public void attendanceStatusOptionsEndpointReturnsSelectableValuesToNavigatorsOnly() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			CareEncounterResource careEncounterResource = app.getInjector().getInstance(CareEncounterResource.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			Account navigator = accountService.findAccountById(CARE_NAVIGATOR_ACCOUNT_ID).get();
			Account patient = accountService.findAccountById(CARE_NAVIGATOR_ACTIVE_FIXTURE_PATIENT_ID).get();
			ZoneId timeZone = ZoneId.of("America/New_York");

			currentContextExecutor.execute(new CurrentContext.Builder(navigator, Locale.US, timeZone).build(), () -> {
				ApiResponse response = careEncounterResource.careEncounterAttendanceStatuses();
				Map<String, Object> responseModel = (Map<String, Object>) response.model().get();
				List<Map<String, Object>> attendanceStatuses =
						(List<Map<String, Object>>) responseModel.get("attendanceStatuses");

				assertEquals(2, attendanceStatuses.size());
				assertEquals(AttendanceStatusId.ATTENDED, attendanceStatuses.get(0).get("attendanceStatusId"));
				assertEquals("Attended", attendanceStatuses.get(0).get("description"));
				assertEquals(AttendanceStatusId.MISSED, attendanceStatuses.get(1).get("attendanceStatusId"));
				assertEquals("Missed", attendanceStatuses.get(1).get("description"));
			});

			assertThrows(AuthorizationException.class, () -> currentContextExecutor.execute(
					new CurrentContext.Builder(patient, Locale.US, timeZone).build(),
					careEncounterResource::careEncounterAttendanceStatuses));
		});
	}

	@Test
	public void selectableAttendanceStatusesAreLimitedAndOrdered() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			CareEncounterService careEncounterService = app.getInjector().getInstance(CareEncounterService.class);
			List<AttendanceStatus> attendanceStatuses = careEncounterService.findSelectableAttendanceStatuses();

			assertEquals(2, attendanceStatuses.size());
			assertEquals(AttendanceStatusId.ATTENDED, attendanceStatuses.get(0).getAttendanceStatusId());
			assertEquals("Attended", attendanceStatuses.get(0).getDescription());
			assertEquals(AttendanceStatusId.MISSED, attendanceStatuses.get(1).getAttendanceStatusId());
			assertEquals("Missed", attendanceStatuses.get(1).getDescription());
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void generalAndCareEncounterAppointmentResponsesExposeAppointmentTimeStatus() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			CareEncounterService careEncounterService = app.getInjector().getInstance(CareEncounterService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			AppointmentApiResponseFactory appointmentResponseFactory = app.getInjector()
					.getInstance(AppointmentApiResponseFactory.class);
			CareEncounterApiResponseFactory careEncounterResponseFactory = app.getInjector()
					.getInstance(CareEncounterApiResponseFactory.class);
			CareEncounterListApiResponseFactory careEncounterListResponseFactory = app.getInjector()
					.getInstance(CareEncounterListApiResponseFactory.class);
			UUID careEncounterId = careEncounterId(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);
			Appointment appointment = appointmentService.findAppointmentById(CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID).get();
			LocalDateTime now = LocalDateTime.now(appointment.getTimeZone());
			Account navigator = accountService.findAccountById(CARE_NAVIGATOR_ACCOUNT_ID).get();
			JsonMapper jsonMapper = new JsonMapper();

			resetAsOpenCurrentAppointment(database, careEncounterId, now.minusMinutes(5), now.plusMinutes(25));
			appointment = appointmentService.findAppointmentById(CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID).get();
			CareEncounter careEncounter = careEncounterService.findCareEncounterByIdForInstitutionId(
					careEncounterId, InstitutionId.COBALT).get();
			Appointment finalAppointment = appointment;

			currentContextExecutor.execute(new CurrentContext.Builder(navigator, Locale.US,
					appointment.getTimeZone()).build(), () -> {
				AppointmentApiResponse appointmentResponse = appointmentResponseFactory.create(finalAppointment);
				CareEncounterApiResponse careEncounterResponse = careEncounterResponseFactory.create(careEncounter);
				CareEncounterListApiResponse careEncounterListResponse =
						careEncounterListResponseFactory.create(careEncounter);

				assertEquals(AppointmentTimeStatusId.IN_SESSION, appointmentResponse.getAppointmentTimeStatusId());
				assertEquals(AppointmentTimeStatusId.IN_SESSION,
						careEncounterResponse.getAppointment().getAppointmentTimeStatusId());
				assertEquals(AppointmentTimeStatusId.IN_SESSION,
						careEncounterListResponse.getAppointment().getAppointmentTimeStatusId());
				Map<String, Object> appointmentResponseMap = jsonMapper.toMap(appointmentResponse);
				Map<String, Object> listAppointmentResponseMap = (Map<String, Object>) jsonMapper
						.toMap(careEncounterListResponse).get("appointment");
				assertEquals(AppointmentTimeStatusId.IN_SESSION.name(),
						appointmentResponseMap.get("appointmentTimeStatusId"));
				assertEquals(AppointmentTimeStatusId.IN_SESSION.name(),
						listAppointmentResponseMap.get("appointmentTimeStatusId"));
				assertFalse(appointmentResponseMap.containsKey("inSession"));
				assertFalse(listAppointmentResponseMap.containsKey("inSession"));
			});
		});
	}

	@Test
	public void navigatorCanRecordAndCorrectCurrentAppointmentAttendanceAfterStart() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CareEncounterService careEncounterService = app.getInjector().getInstance(CareEncounterService.class);
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			UUID careEncounterId = careEncounterId(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);
			Appointment appointment = appointmentService.findAppointmentById(CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID).get();
			LocalDateTime now = LocalDateTime.now(appointment.getTimeZone());
			ChangeAppointmentAttendanceStatusRequest request = request(AttendanceStatusId.ATTENDED);

			resetAsOpenCurrentAppointment(database, careEncounterId, now.minusMinutes(5), now.plusMinutes(25));

			CareEncounter careEncounter = careEncounterService.changeCareEncounterAppointmentAttendanceStatus(
					careEncounterId, InstitutionId.COBALT, request);
			assertEquals(CareEncounterStatusId.OPEN, careEncounter.getCareEncounterStatusId());
			assertEquals(CARE_NAVIGATOR_ACCOUNT_ID, careEncounter.getLastUpdatedByAccountId());
			assertEquals(AttendanceStatusId.ATTENDED, appointmentService
					.findAppointmentById(CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID).get().getAttendanceStatusId());

			request.setAttendanceStatusId(AttendanceStatusId.MISSED);
			careEncounterService.changeCareEncounterAppointmentAttendanceStatus(
					careEncounterId, InstitutionId.COBALT, request);
			assertEquals(AttendanceStatusId.MISSED, appointmentService
					.findAppointmentById(CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID).get().getAttendanceStatusId());

			careEncounterService.changeCareEncounterAppointmentAttendanceStatus(
					careEncounterId, InstitutionId.COBALT, request);
			assertEquals(AttendanceStatusId.MISSED, appointmentService
					.findAppointmentById(CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID).get().getAttendanceStatusId());
		});
	}

	@Test
	public void attendanceValidationRejectsUnavailableAppointmentsAndStatuses() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CareEncounterService careEncounterService = app.getInjector().getInstance(CareEncounterService.class);
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			UUID careEncounterId = careEncounterId(database, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);
			Appointment appointment = appointmentService.findAppointmentById(CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID).get();
			LocalDateTime now = LocalDateTime.now(appointment.getTimeZone());
			ChangeAppointmentAttendanceStatusRequest request = request(AttendanceStatusId.ATTENDED);

			resetAsOpenCurrentAppointment(database, careEncounterId, now.plusMinutes(5), now.plusMinutes(35));
			assertThrows(ValidationException.class, () -> careEncounterService
					.changeCareEncounterAppointmentAttendanceStatus(careEncounterId, InstitutionId.COBALT, request));

			resetAsOpenCurrentAppointment(database, careEncounterId, now.minusMinutes(5), now.plusMinutes(25));
			request.setAttendanceStatusId(AttendanceStatusId.UNKNOWN);
			assertThrows(ValidationException.class, () -> careEncounterService
					.changeCareEncounterAppointmentAttendanceStatus(careEncounterId, InstitutionId.COBALT, request));

			request.setAttendanceStatusId(AttendanceStatusId.ATTENDED);
			database.execute("UPDATE care_encounter SET care_encounter_status_id='CLOSED' WHERE care_encounter_id=?",
					careEncounterId);
			assertThrows(ValidationException.class, () -> careEncounterService
					.changeCareEncounterAppointmentAttendanceStatus(careEncounterId, InstitutionId.COBALT, request));

			database.execute("UPDATE care_encounter SET care_encounter_status_id='OPEN' WHERE care_encounter_id=?",
					careEncounterId);
			database.execute("""
					UPDATE appointment
					SET canceled=TRUE, attendance_status_id='CANCELED'
					WHERE appointment_id=?
					""", CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);
			assertThrows(ValidationException.class, () -> careEncounterService
					.changeCareEncounterAppointmentAttendanceStatus(careEncounterId, InstitutionId.COBALT, request));

			request.setAppointmentId(CARE_NAVIGATOR_ATTENDED_APPOINTMENT_ID);
			assertThrows(ValidationException.class, () -> careEncounterService
					.changeCareEncounterAppointmentAttendanceStatus(careEncounterId, InstitutionId.COBALT, request));

			request.setAppointmentId(CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);
			assertThrows(ValidationException.class, () -> careEncounterService
					.changeCareEncounterAppointmentAttendanceStatus(careEncounterId, InstitutionId.COBALT_IC, request));

			UUID rebookedCareEncounterId = careEncounterId(database, CARE_NAVIGATOR_REBOOKED_APPOINTMENT_ID);
			database.execute("UPDATE care_encounter SET care_encounter_status_id='OPEN' WHERE care_encounter_id=?",
					rebookedCareEncounterId);
			request.setAppointmentId(CARE_NAVIGATOR_CANCELED_APPOINTMENT_ID);
			assertThrows(ValidationException.class, () -> careEncounterService
					.changeCareEncounterAppointmentAttendanceStatus(rebookedCareEncounterId, InstitutionId.COBALT, request));
		});
	}

	private ChangeAppointmentAttendanceStatusRequest request(AttendanceStatusId attendanceStatusId) {
		ChangeAppointmentAttendanceStatusRequest request = new ChangeAppointmentAttendanceStatusRequest();
		request.setAppointmentId(CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);
		request.setAccountId(CARE_NAVIGATOR_ACCOUNT_ID);
		request.setAttendanceStatusId(attendanceStatusId);
		return request;
	}

	private UUID careEncounterId(Database database, UUID appointmentId) {
		return database.queryForObject("SELECT care_encounter_id FROM appointment WHERE appointment_id=?",
				UUID.class, appointmentId).get();
	}

	private void resetAsOpenCurrentAppointment(Database database,
														UUID careEncounterId,
														LocalDateTime startTime,
														LocalDateTime endTime) {
		database.execute("""
				UPDATE care_encounter
				SET care_encounter_status_id='OPEN',
					closed_at=NULL,
					closed_by_account_id=NULL,
					canceled_by_account_id=NULL,
					care_encounter_cancellation_reason_id=NULL,
					care_encounter_cancellation_reason_other_text=NULL
				WHERE care_encounter_id=?
				""", careEncounterId);
		database.execute("""
				UPDATE appointment
				SET start_time=?,
					end_time=?,
					attendance_status_id='UNKNOWN',
					canceled=FALSE,
					canceled_at=NULL,
					canceled_by_account_id=NULL,
					canceled_for_reschedule=FALSE,
					rescheduled_appointment_id=NULL
				WHERE appointment_id=?
				""", startTime, endTime, CARE_NAVIGATOR_ACTIVE_APPOINTMENT_ID);
	}
}
