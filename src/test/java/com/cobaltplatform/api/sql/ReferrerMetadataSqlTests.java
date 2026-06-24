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

package com.cobaltplatform.api.sql;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Transmogrify, LLC.
 */
public class ReferrerMetadataSqlTests {
	@Test
	public void resultScreenBookingPathMigrationsWriteAppointmentTypeIdAndPath() throws IOException {
		assertResultScreenBookingMigrationWritesAppointmentTypeIdAndPath(
				readSql("sql/updates/256-provider-booking-screening.sql"));
	}

	@Test
	public void providerClinicDetailsHtmlSchemaAndFixtureContentAreSeparated() throws IOException {
		String functionalSql = readSql("sql/updates/256-provider-booking-screening.sql");
		String fixtureSql = readSql("sql/updates/256-local-only-provider-booking-test-data.sql");

		assertTrue(functionalSql.contains("ALTER TABLE provider ADD COLUMN IF NOT EXISTS details_html TEXT"));
		assertTrue(functionalSql.contains("ALTER TABLE clinic ADD COLUMN IF NOT EXISTS details_html TEXT"));
		assertTrue(fixtureSql.contains("<section class=\"mb-8\">"));
		assertTrue(fixtureSql.contains("<h2 class=\"mb-4\">About</h2>"));
		assertTrue(fixtureSql.contains("<h2 class=\"mb-4\">Accepted Insurances</h2>"));
		assertTrue(fixtureSql.contains("<div class=\"table-responsive\">"));
		assertTrue(fixtureSql.contains("<table class=\"table table-bordered align-middle mb-0\">"));
		assertTrue(fixtureSql.contains("<th scope=\"col\">Health Insurance</th>"));
		assertTrue(fixtureSql.contains("Aetna Choice Point-of-Service (POS) II"));
		assertTrue(fixtureSql.contains("Quest Behavioral Health"));
		assertTrue(fixtureSql.contains("LOWER(TRIM(description))=LOWER(TRIM('Penn Autism Clinic'))"));
	}

	@Test
	public void providerClinicLocationSchemaAndFixtureContentAreSeparated() throws IOException {
		String functionalSql = readSql("sql/updates/256-provider-booking-screening.sql");
		String fixtureSql = readSql("sql/updates/256-local-only-provider-booking-test-data.sql");

		assertTrue(functionalSql.contains("CREATE TABLE IF NOT EXISTS provider_location"));
		assertTrue(functionalSql.contains("CREATE TABLE IF NOT EXISTS clinic_location"));
		assertTrue(functionalSql.contains("ALTER TABLE provider ADD COLUMN IF NOT EXISTS website_url TEXT"));
		assertTrue(functionalSql.contains("ALTER TABLE clinic ADD COLUMN IF NOT EXISTS email_address TEXT"));
		assertFalse(functionalSql.contains("fixtures.cobalt.care/locations"));

		assertTrue(fixtureSql.contains("INSERT INTO provider_location"));
		assertTrue(fixtureSql.contains("INSERT INTO clinic_location"));
		assertTrue(fixtureSql.contains("INSERT INTO institution_location"));
		assertFalse(fixtureSql.contains("fixtures.cobalt.care/locations"));
	}

	@Test
	public void providerClinicLocationContactCleanupDropsAccidentalColumns() throws IOException {
		String cleanupSql = readSql("sql/updates/257-provider-booking-contact-ownership-cleanup.sql");

		assertTrue(cleanupSql.contains("ALTER TABLE provider ADD COLUMN IF NOT EXISTS website_url TEXT"));
		assertTrue(cleanupSql.contains("ALTER TABLE clinic ADD COLUMN IF NOT EXISTS email_address TEXT"));
		assertTrue(cleanupSql.contains("ALTER TABLE provider_location DROP COLUMN IF EXISTS phone_number"));
		assertTrue(cleanupSql.contains("ALTER TABLE provider_location DROP COLUMN IF EXISTS website_url"));
		assertTrue(cleanupSql.contains("ALTER TABLE provider_location DROP COLUMN IF EXISTS email_address"));
		assertTrue(cleanupSql.contains("ALTER TABLE clinic_location DROP COLUMN IF EXISTS phone_number"));
		assertTrue(cleanupSql.contains("ALTER TABLE clinic_location DROP COLUMN IF EXISTS website_url"));
		assertTrue(cleanupSql.contains("ALTER TABLE clinic_location DROP COLUMN IF EXISTS email_address"));
		assertTrue(cleanupSql.contains("ALTER TABLE institution_location DROP COLUMN IF EXISTS address_id"));
	}

	protected void assertResultScreenBookingMigrationWritesAppointmentTypeIdAndPath(String sql) {
		assertTrue(sql.contains("'{booking,path}'"));
		assertTrue(sql.contains("appointmentTypeId=%s&institutionLocationId=%s&featureId=%s"));
		assertTrue(sql.contains("'{booking,appointmentTypeId}'"));
		assertTrue(sql.contains("TO_JSONB(booking_route.appointment_type_id)"));
	}

	protected String readSql(String filename) throws IOException {
		return Files.readString(Path.of(filename), StandardCharsets.UTF_8);
	}

}
