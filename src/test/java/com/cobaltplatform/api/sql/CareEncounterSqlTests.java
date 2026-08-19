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
		assertTrue(migrationSql.contains("appointment_id UUID NOT NULL UNIQUE REFERENCES appointment(appointment_id)"));
		assertTrue(migrationSql.contains("account_id UUID NOT NULL REFERENCES account(account_id)"));
		assertTrue(migrationSql.contains("CREATE UNIQUE INDEX care_encounter_one_open_per_account_idx"));
		assertTrue(migrationSql.contains("ON care_encounter(account_id)"));
		assertTrue(migrationSql.contains("WHERE care_encounter_status_id='OPEN'"));
		assertTrue(migrationSql.contains("care_encounter_status_id TEXT NOT NULL REFERENCES care_encounter_status"));
		assertTrue(migrationSql.contains("notes TEXT"));
		assertTrue(migrationSql.contains("closed_at TIMESTAMPTZ"));
		assertTrue(migrationSql.contains("canceled_by_account_id UUID REFERENCES account(account_id)"));
		assertTrue(migrationSql.contains("deleted BOOLEAN NOT NULL DEFAULT FALSE"));
		assertTrue(migrationSql.contains("CREATE TRIGGER create_care_encounter_for_appointment"));
		assertTrue(migrationSql.contains("AFTER INSERT OR UPDATE OF provider_id, account_id ON appointment"));
		assertTrue(migrationSql.contains("CREATE TRIGGER close_care_encounter_for_terminal_appointment"));
		assertTrue(migrationSql.contains("AFTER INSERT OR UPDATE OF attendance_status_id, canceled ON appointment"));
		assertTrue(migrationSql.contains("AND care_encounter_status_id='OPEN'"));
		assertTrue(migrationSql.contains("ON CONFLICT (appointment_id) DO NOTHING"));
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
		assertTrue(fixtureSql.contains("'ATTENDED'"));
		assertTrue(fixtureSql.contains("'UNKNOWN'"));
		assertTrue(fixtureSql.contains("'CANCELED'"));
		assertTrue(fixtureSql.contains("UPDATE care_encounter"));
		assertTrue(fixtureSql.contains("care_encounter_status_id=CASE appointment_id"));
		assertTrue(fixtureSql.contains("canceled_by_account_id=CASE"));
	}

	@Test
	public void administrativeApiSupportsPaginatedSearchSortingAndNavigatorCancellation() throws IOException {
		String resourceJava = readSql("src/main/java/com/cobaltplatform/api/web/resource/CareEncounterResource.java");
		String requestJava = readSql("src/main/java/com/cobaltplatform/api/model/api/request/FindCareEncountersRequest.java");
		String serviceJava = readSql("src/main/java/com/cobaltplatform/api/service/CareEncounterService.java");
		String appointmentServiceJava = readSql("src/main/java/com/cobaltplatform/api/service/AppointmentService.java");
		String responseJava = readSql("src/main/java/com/cobaltplatform/api/model/api/response/CareEncounterApiResponse.java");

		assertTrue(resourceJava.contains("@GET(\"/admin/care-encounters\")"));
		assertTrue(resourceJava.contains("Optional<Integer> pageNumber"));
		assertTrue(resourceJava.contains("Optional<Integer> pageSize"));
		assertTrue(resourceJava.contains("Optional<String> searchQuery"));
		assertTrue(resourceJava.contains("Optional<CareEncounterStatusId> careEncounterStatusId"));
		assertTrue(resourceJava.contains("Optional<OrderBy> orderBy"));
		assertTrue(resourceJava.contains("put(\"totalCountDescription\""));
		assertTrue(requestJava.contains("APPOINTMENT_START_TIME_DESC"));
		assertTrue(requestJava.contains("PATIENT_NAME_ASC"));
		assertTrue(requestJava.contains("STATUS_ASC"));
		assertTrue(requestJava.contains("LAST_UPDATED_DESC"));
		assertTrue(serviceJava.contains("COUNT(*) OVER() AS total_count"));
		assertTrue(serviceJava.contains("LIMIT ? OFFSET ?"));
		assertTrue(serviceJava.contains("care_encounter.notes ILIKE ?"));
		assertTrue(serviceJava.contains("care_encounter.care_encounter_status_id<>'OPEN'"));
		assertTrue(serviceJava.contains("CASE WHEN care_encounter.care_encounter_status_id='OPEN' THEN 1 ELSE 0 END"));
		assertTrue(resourceJava.contains("@PUT(\"/admin/care-encounters/{careEncounterId}/cancel\")"));
		assertTrue(serviceJava.contains("getAppointmentService().cancelAppointment(request)"));
		assertTrue(serviceJava.contains("FOR UPDATE OF care_encounter, appointment"));
		assertTrue(serviceJava.contains("SET care_encounter_status_id='CANCELED'"));
		assertTrue(appointmentServiceJava.contains("hasOpenCareEncounterForAccountId(accountId)"));
		assertTrue(appointmentServiceJava.contains("careNavigatorOpenAppointmentExists"));
		assertTrue(appointmentServiceJava.contains("closeOpenCareEncounterForReschedule"));
		assertTrue(responseJava.contains("getCareEncounterStatusId()"));
		assertTrue(responseJava.contains("getCareEncounterStatusDisplayLabel()"));
		assertTrue(responseJava.contains("getCareEncounterStatusId().getDisplayLabel()"));
		assertTrue(responseJava.contains("getCanceledByAccountId()"));
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
