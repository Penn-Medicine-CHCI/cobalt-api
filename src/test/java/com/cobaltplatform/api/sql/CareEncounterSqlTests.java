/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.cobaltplatform.api.sql;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CareEncounterSqlTests {
	@Test
	public void productionMigrationCreatesCareEncounterLifecycleAndOnlineOnlyRule() throws IOException {
		String migrationSql = readSql("sql/updates/261-care-encounters.sql");

		assertTrue(migrationSql.contains("'261-care-encounters'"));
		assertTrue(migrationSql.contains("virtual_appointments_only BOOLEAN NOT NULL DEFAULT FALSE"));
		assertTrue(migrationSql.contains("support_role_id='CARE_NAVIGATOR'"));
		assertTrue(migrationSql.contains("CREATE TABLE care_encounter"));
		assertTrue(migrationSql.contains("CREATE TABLE care_encounter_status"));
		assertTrue(migrationSql.contains("'OPEN', 'Open', FALSE"));
		assertTrue(migrationSql.contains("'CLOSED', 'Closed', TRUE"));
		assertTrue(migrationSql.contains("'CANCELED', 'Canceled by Care Navigator', TRUE"));
		assertTrue(migrationSql.contains("CREATE TABLE care_encounter_cancellation_reason"));
		assertTrue(migrationSql.contains("'PATIENT_REQUESTED', 'Patient requested cancellation', 1, FALSE"));
		assertTrue(migrationSql.contains("'NO_LONGER_NEEDED', 'Care navigation is no longer needed', 2, FALSE"));
		assertTrue(migrationSql.contains("'UNABLE_TO_REACH_PATIENT', 'Unable to reach patient', 3, FALSE"));
		assertTrue(migrationSql.contains("'SCHEDULING_CONFLICT', 'Scheduling conflict', 4, FALSE"));
		assertTrue(migrationSql.contains("'DUPLICATE_BOOKING', 'Duplicate booking', 5, FALSE"));
		assertTrue(migrationSql.contains("'OTHER', 'Other', 6, TRUE"));
		assertFalse(migrationSql.contains("appointment_id UUID NOT NULL UNIQUE REFERENCES appointment(appointment_id)"));
		assertTrue(migrationSql.contains("account_id UUID NOT NULL REFERENCES account(account_id)"));
		assertTrue(migrationSql.contains("care_navigator_account_id UUID REFERENCES account(account_id)"));
		assertTrue(migrationSql.contains("screening_session_id UUID REFERENCES screening_session(screening_session_id)"));
		assertTrue(migrationSql.contains("care_encounter_id UUID REFERENCES care_encounter(care_encounter_id)"));
		assertTrue(migrationSql.contains("canceled_by_account_id UUID REFERENCES account(account_id)"));
		assertTrue(migrationSql.contains("CREATE UNIQUE INDEX care_encounter_one_open_per_account_idx"));
		assertTrue(migrationSql.contains("ON care_encounter(account_id)"));
		assertTrue(migrationSql.contains("WHERE care_encounter_status_id='OPEN'"));
		assertTrue(migrationSql.contains("care_encounter_status_id TEXT NOT NULL REFERENCES care_encounter_status"));
		assertTrue(migrationSql.contains("email_address TEXT"));
		assertTrue(migrationSql.contains("notes TEXT"));
		assertTrue(migrationSql.contains("closed_at TIMESTAMPTZ"));
		assertTrue(migrationSql.contains("closed_by_account_id UUID REFERENCES account(account_id)"));
		assertTrue(migrationSql.contains("canceled_by_account_id UUID REFERENCES account(account_id)"));
		assertTrue(migrationSql.contains("care_encounter_cancellation_reason_id TEXT REFERENCES care_encounter_cancellation_reason"));
		assertTrue(migrationSql.contains("care_encounter_cancellation_reason_other_text TEXT"));
		assertTrue(migrationSql.contains("care_encounter_cancellation_reason_required_check"));
		assertTrue(migrationSql.contains("care_encounter_cancellation_reason_other_text_check"));
		assertTrue(migrationSql.contains("deleted BOOLEAN NOT NULL DEFAULT FALSE"));
		assertTrue(migrationSql.contains("CREATE UNIQUE INDEX care_encounter_one_active_appointment_idx"));
		assertTrue(migrationSql.contains("AND canceled_for_reschedule=FALSE"));
		assertTrue(migrationSql.contains("AND attendance_status_id='UNKNOWN'"));
		assertTrue(migrationSql.contains("CREATE TRIGGER attach_care_navigator_appointment_to_encounter"));
		assertTrue(migrationSql.contains("AFTER INSERT OR UPDATE OF provider_id, account_id, care_encounter_id ON appointment"));
		assertTrue(migrationSql.contains("NEW.email_address"));
		assertTrue(migrationSql.contains("v_new_appointment_is_active"));
		assertTrue(migrationSql.contains("New Care Navigator appointments cannot be attached to a terminal encounter."));
		assertTrue(migrationSql.contains("CREATE TRIGGER close_care_encounter_for_patient_cancellation"));
		assertTrue(migrationSql.contains("NEW.canceled_by_account_id=NEW.account_id"));
		assertTrue(migrationSql.contains("NEW.canceled_for_reschedule=FALSE"));
		assertFalse(migrationSql.contains("close_care_encounter_for_terminal_appointment"));
	}

	@Test
	public void localFixtureCreatesRepresentativeNavigatorAppointments() throws IOException {
		String fixtureSql = readSql("sql/local/261-care-encounter-seed.sql");

		assertTrue(fixtureSql.contains("'260-local-only-care-navigator-seed'"));
		assertTrue(fixtureSql.contains("'261-care-encounters'"));
		assertTrue(fixtureSql.contains("SET virtual_appointments_only=TRUE"));
		assertTrue(fixtureSql.contains("care-encounter.alex@example.com"));
		assertTrue(fixtureSql.contains("care-encounter.jordan@example.com"));
		assertTrue(fixtureSql.contains("care-encounter.taylor@example.com"));
		assertTrue(fixtureSql.contains("care-encounter.casey@example.com"));
		assertTrue(fixtureSql.contains("'ATTENDED'"));
		assertTrue(fixtureSql.contains("'UNKNOWN'"));
		assertTrue(fixtureSql.contains("'CANCELED'"));
		assertTrue(fixtureSql.contains("UPDATE care_encounter"));
		assertTrue(fixtureSql.contains("v_rebooked_appointment_id"));
		assertTrue(fixtureSql.contains("v_patient_canceled_appointment_id"));
		assertTrue(fixtureSql.contains("closed_by_account_id=v_patient_four_id"));
		assertTrue(fixtureSql.contains("replacement remains in the same encounter"));
	}

	@Test
	public void administrativeApiSupportsPaginatedSearchSortingAndNavigatorCancellation() throws IOException {
		String resourceJava = readSql("src/main/java/com/cobaltplatform/api/web/resource/CareEncounterResource.java");
		String requestJava = readSql("src/main/java/com/cobaltplatform/api/model/api/request/FindCareEncountersRequest.java");
		String serviceJava = readSql("src/main/java/com/cobaltplatform/api/service/CareEncounterService.java");
		String appointmentServiceJava = readSql("src/main/java/com/cobaltplatform/api/service/AppointmentService.java");
		String appointmentResourceJava = readSql("src/main/java/com/cobaltplatform/api/web/resource/AppointmentResource.java");
		String responseJava = readSql("src/main/java/com/cobaltplatform/api/model/api/response/CareEncounterApiResponse.java");
		String listResponseJava = readSql("src/main/java/com/cobaltplatform/api/model/api/response/CareEncounterListApiResponse.java");
		String listAppointmentResponseJava = readSql("src/main/java/com/cobaltplatform/api/model/api/response/CareEncounterAppointmentApiResponse.java");
		String modelJava = readSql("src/main/java/com/cobaltplatform/api/model/db/CareEncounter.java");
		String createRequestJava = readSql("src/main/java/com/cobaltplatform/api/model/api/request/CreateCareEncounterRequest.java");
		String updateRequestJava = readSql("src/main/java/com/cobaltplatform/api/model/api/request/UpdateCareEncounterRequest.java");

		assertTrue(resourceJava.contains("@GET(\"/admin/care-encounters\")"));
		assertTrue(resourceJava.contains("@GET(\"/admin/care-encounters/{careEncounterId}\")"));
		assertTrue(resourceJava.contains("Optional<Integer> pageNumber"));
		assertTrue(resourceJava.contains("Optional<Integer> pageSize"));
		assertTrue(resourceJava.contains("Optional<String> searchQuery"));
		assertTrue(resourceJava.contains("Optional<CareEncounterStatusId> careEncounterStatusId"));
		assertTrue(resourceJava.contains("Optional<CareEncounterAssignmentScopeId> careEncounterAssignmentScopeId"));
		assertTrue(resourceJava.contains("Optional<CareEncounterSortColumnId> careEncounterSortColumnId"));
		assertTrue(resourceJava.contains("Optional<SortDirectionId> sortDirectionId"));
		assertFalse(resourceJava.contains("Optional<OrderBy> orderBy"));
		assertTrue(resourceJava.contains("put(\"totalCountDescription\""));
		assertTrue(resourceJava.contains("put(\"otherCareEncounters\""));
		assertTrue(resourceJava.contains("put(\"otherCareEncountersTotalCount\""));
		assertTrue(resourceJava.contains("put(\"screeningSessionResult\""));
		assertTrue(resourceJava.contains("findScreeningSessionResult(careEncounter.getScreeningSessionId())"));
		assertTrue(requestJava.contains("APPOINTMENT_DATE"));
		assertTrue(requestJava.contains("PATIENT_NAME"));
		assertTrue(requestJava.contains("STATUS"));
		assertTrue(requestJava.contains("CREATED"));
		assertTrue(requestJava.contains("LAST_UPDATED"));
		assertTrue(requestJava.contains("ALL"));
		assertTrue(requestJava.contains("SELF"));
		assertTrue(requestJava.contains("UNASSIGNED"));
		assertTrue(requestJava.contains("CareEncounterAssignmentScopeId.ALL"));
		assertTrue(requestJava.contains("private SortDirectionId sortDirectionId"));
		assertTrue(requestJava.contains("private InstitutionId institutionId"));
		assertTrue(resourceJava.contains("request.setInstitutionId(account.getInstitutionId())"));
		assertTrue(resourceJava.contains("request.setCareNavigatorAccountId(account.getAccountId())"));
		assertFalse(resourceJava.contains("account.getProviderId()"));
		assertTrue(serviceJava.contains("COUNT(*) OVER() AS total_count"));
		assertTrue(serviceJava.contains("provider.institution_id=?"));
		assertTrue(serviceJava.contains("LIMIT ? OFFSET ?"));
		assertTrue(serviceJava.contains("care_encounter.notes ILIKE ?"));
		assertTrue(serviceJava.contains("CONCAT_WS(' ', search_appointment.first_name, search_appointment.last_name) ILIKE ?"));
		assertTrue(serviceJava.contains("SortDirectionId.ASCENDING ? \"ASC\" : \"DESC\""));
		assertTrue(serviceJava.contains("CareEncounterSortColumnId.CREATED"));
		assertTrue(serviceJava.contains("query.append(\"care_encounter.created \""));
		assertTrue(serviceJava.contains("findOtherCareEncountersByAccountId"));
		assertTrue(serviceJava.contains("care_encounter.care_encounter_id<>?"));
		assertTrue(serviceJava.contains("care_encounter.care_encounter_status_id<>'OPEN'"));
		assertTrue(serviceJava.contains("CASE WHEN care_encounter.care_encounter_status_id='OPEN' THEN 1 ELSE 0 END"));
		assertTrue(serviceJava.contains("care_encounter.care_navigator_account_id=?"));
		assertTrue(serviceJava.contains("care_encounter.care_navigator_account_id IS NULL"));
		assertTrue(resourceJava.contains("@PUT(\"/admin/care-encounters/{careEncounterId}/cancel\")"));
		assertTrue(resourceJava.contains("@GET(\"/admin/care-encounter-cancellation-reasons\")"));
		assertTrue(resourceJava.contains("CancelCareEncounterRequest.class"));
		assertTrue(resourceJava.contains("Map.of(\"careEncounterCancellationReasons\""));
		assertTrue(resourceJava.contains("\"freeformTextRequired\""));
		assertTrue(serviceJava.contains("findCareEncounterCancellationReasons"));
		assertTrue(serviceJava.contains("CareEncounterCancellationReasonId.OTHER"));
		assertFalse(serviceJava.contains("getAppointmentService().cancelAppointment(cancelAppointmentRequest)"));
		assertTrue(serviceJava.contains("findActiveAppointmentByCareEncounterIdForInstitutionId"));
		assertTrue(serviceJava.contains("SET care_encounter_status_id='CANCELED'"));
		assertTrue(serviceJava.contains("care_encounter_cancellation_reason_id=?"));
		assertTrue(serviceJava.contains("care_encounter_cancellation_reason_other_text=?"));
		assertTrue(resourceJava.contains("@PUT(\"/admin/care-encounters/{careEncounterId}/close\")"));
		assertTrue(resourceJava.contains("@PUT(\"/admin/care-encounters/{careEncounterId}/assignment\")"));
		assertTrue(serviceJava.contains("closeCareEncounter"));
		assertTrue(serviceJava.contains("assignCareEncounter"));
		assertTrue(appointmentServiceJava.contains("hasActiveCareNavigatorAppointmentForAccountId(accountId)"));
		assertTrue(appointmentServiceJava.contains("hasAttendedAppointmentInOpenCareEncounterForAccountId(accountId)"));
		assertTrue(appointmentServiceJava.contains("careNavigatorOpenAppointmentExists"));
		assertTrue(appointmentServiceJava.contains("careNavigatorEncounterAwaitingClosure"));
		assertTrue(appointmentServiceJava.contains("markCareNavigatorAppointmentPendingReschedule"));
		assertTrue(appointmentServiceJava.contains("canceled_by_account_id=?"));
		assertTrue(appointmentResourceJava.contains("request.setCanceledByAccountId(account.getAccountId())"));
		assertTrue(appointmentServiceJava.contains("SET screening_session_id=?"));
		assertTrue(appointmentServiceJava.contains("preserveExistingScreeningEligibility && excludedAppointmentId != null"));
		assertTrue(responseJava.contains("getCareEncounterStatusId()"));
		assertTrue(responseJava.contains("getCareEncounterStatusDisplayLabel()"));
		assertTrue(responseJava.contains("getCareEncounterStatusId().getDisplayLabel()"));
		assertTrue(responseJava.contains("private final AppointmentApiResponse appointment"));
		assertTrue(responseJava.contains("private final List<AppointmentApiResponse> appointmentHistory"));
		assertFalse(responseJava.contains("private final AppointmentApiResponse activeAppointment"));
		assertFalse(responseJava.contains("private final List<AppointmentApiResponse> appointments"));
		assertTrue(listResponseJava.contains("private final CareEncounterAppointmentApiResponse appointment"));
		assertFalse(listResponseJava.contains("private final AppointmentApiResponse appointment"));
		assertTrue(listAppointmentResponseJava.contains("private final UUID appointmentId"));
		assertTrue(resourceJava.contains("getCareEncounterListApiResponseFactory()::create"));
		assertTrue(responseJava.contains("private final UUID careNavigatorAccountId"));
		assertTrue(responseJava.contains("private final String careNavigatorDisplayName"));
		assertTrue(responseJava.contains("private final String patientFullName"));
		assertTrue(responseJava.contains("private final LocalDate appointmentDate"));
		assertTrue(responseJava.contains("private final String appointmentDateDescription"));
		assertTrue(responseJava.contains("private final LocalDate createdDate"));
		assertTrue(responseJava.contains("private final String createdDateDescription"));
		assertTrue(responseJava.contains("private final CareEncounterCancellationReasonId careEncounterCancellationReasonId"));
		assertTrue(responseJava.contains("private final String careEncounterCancellationReasonOtherText"));
		assertTrue(responseJava.contains("getCanceledByAccountId()"));
		assertTrue(responseJava.contains("getScreeningSessionId()"));
		assertTrue(responseJava.contains("private final String notes"));
		assertTrue(responseJava.contains("private final String emailAddress"));
		assertTrue(responseJava.contains("careEncounter.getEmailAddress()"));
		assertTrue(modelJava.contains("private String notes"));
		assertTrue(modelJava.contains("private String emailAddress"));
		assertTrue(modelJava.contains("private UUID careNavigatorAccountId"));
		assertTrue(modelJava.contains("private UUID closedByAccountId"));
		assertTrue(modelJava.contains("getNotes()"));
		assertTrue(modelJava.contains("setNotes(@Nullable String notes)"));
		assertTrue(createRequestJava.contains("private String notes"));
		assertTrue(updateRequestJava.contains("private String notes"));
		assertTrue(updateRequestJava.contains("private String emailAddress"));
		assertTrue(serviceJava.contains("SET email_address=?, notes=?, last_updated_by_account_id=?"));
	}

	@Test
	public void databaseRecreationRunsCareEncounterMigrationBeforeFixture() throws IOException {
		assertCareEncounterPatchOrder(readSql("sql/recreate-local"));
		assertCareEncounterPatchOrder(readSql("sql/recreate-bootstrap"));
	}

	protected void assertCareEncounterPatchOrder(String recreateSql) {
		int navigatorFixtureIndex = recreateSql.indexOf("local/260-care-navigator-seed.sql");
		int migrationIndex = recreateSql.indexOf("updates/261-care-encounters.sql");
		int fixtureIndex = recreateSql.indexOf("local/261-care-encounter-seed.sql");

		assertTrue(navigatorFixtureIndex >= 0);
		assertTrue(migrationIndex > navigatorFixtureIndex);
		assertTrue(fixtureIndex > migrationIndex);
	}

	protected String readSql(String filename) throws IOException {
		return Files.readString(Path.of(filename), StandardCharsets.UTF_8);
	}
}
