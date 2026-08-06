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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Transmogrify, LLC.
 */
public class ReferrerMetadataSqlTests {
	@Test
	public void cobaltProviderBookingConfigurationIsTenantScopedAndStaysDark() throws IOException {
		String initialSql = readSql("sql/initial/000-base-creates.sql");
		String functionalSql = readSql("sql/updates/259-provider-booking-database.sql");
		String configurationSql = readSql("sql/updates/259-cobalt-provider-booking-configuration.sql");
		String fixtureSql = readSql("sql/updates/259-local-only-provider-booking-seed.sql");

		assertTrue(initialSql.contains("booking_v2_enabled bool NOT NULL DEFAULT false"));
		assertTrue(functionalSql.contains("booking_v2_enabled BOOLEAN NOT NULL DEFAULT FALSE"));
		assertFalse(functionalSql.contains("SET booking_v2_enabled=TRUE"));
		assertFalse(configurationSql.contains("SET booking_v2_enabled=TRUE"));
		assertEquals(5, countOccurrences(configurationSql, "\nUPDATE "));
		assertTrue(configurationSql.contains("WHERE provider.institution_id='COBALT'"));
		assertEquals(2, countOccurrences(configurationSql, "AND sf.institution_id='COBALT'"));
		assertTrue(configurationSql.contains("WHERE institution_id='COBALT'"));
		assertTrue(configurationSql.contains("UPDATE institution_referrer"));
		assertTrue(configurationSql.contains("AND ir.from_institution_id='COBALT'"));
		assertTrue(configurationSql.contains("'{booking,v2Path}'"));
		assertFalse(configurationSql.contains("'{booking,path}'"));
		assertTrue(fixtureSql.contains("SET booking_v2_enabled=TRUE"));
		assertTrue(fixtureSql.contains("WHERE institution_id='COBALT'"));
	}

	@Test
	public void providerBookingDatabaseEnforcesIntegratedCareIsolation() throws IOException {
		String functionalSql = readSql("sql/updates/259-provider-booking-database.sql");

		assertTrue(functionalSql.contains("institution_booking_v2_not_integrated_care_check"));
		assertTrue(functionalSql.contains("CHECK (NOT (integrated_care_enabled AND booking_v2_enabled))"));
		assertTrue(functionalSql.contains("AND NOT EXISTS ("));
		assertTrue(functionalSql.contains("assessed_pat.appointment_type_id=ata.appointment_type_id"));
		assertTrue(functionalSql.contains("assessed_institution.integrated_care_enabled=TRUE"));
	}

	@Test
	public void providerClinicDetailsHtmlSchemaAndFixtureContentAreSeparated() throws IOException {
		String functionalSql = readSql("sql/updates/259-provider-booking-database.sql");
		String fixtureSql = readSql("sql/updates/259-local-only-provider-booking-seed.sql");

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
	public void providerSearchFeatureSupportRolesAreDeployable() throws IOException {
		String functionalSql = readSql("sql/updates/259-provider-booking-database.sql");
		String fixtureSql = readSql("sql/updates/259-local-only-provider-booking-seed.sql");

		assertTrue(functionalSql.contains("INSERT INTO feature_support_role"));
		assertTrue(functionalSql.contains("'MEDICATION_PRESCRIBER', 'PSYCHIATRIST'"));
		assertTrue(functionalSql.contains("'MENTAL_HEALTH_PROVIDERS', 'CARE_MANAGER'"));
		assertTrue(functionalSql.contains("ON CONFLICT (feature_id, support_role_id) DO NOTHING"));
		assertFalse(fixtureSql.contains("INSERT INTO feature_support_role"));
	}

	@Test
	public void providerClinicLocationSchemaAndFixtureContentAreSeparated() throws IOException {
		String functionalSql = readSql("sql/updates/259-provider-booking-database.sql");
		String fixtureSql = readSql("sql/updates/259-local-only-provider-booking-seed.sql");

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
		String functionalSql = readSql("sql/updates/259-provider-booking-database.sql");
		String fixtureSql = readSql("sql/updates/259-local-only-provider-booking-seed.sql");

		assertTrue(functionalSql.contains("ALTER TABLE provider ADD COLUMN IF NOT EXISTS website_url TEXT"));
		assertTrue(functionalSql.contains("ALTER TABLE clinic ADD COLUMN IF NOT EXISTS email_address TEXT"));
		assertTrue(functionalSql.contains("SET website_url=NULLIF(BTRIM(bio_url), '')"));
		assertTrue(functionalSql.contains("ALTER TABLE provider_location DROP COLUMN IF EXISTS phone_number"));
		assertTrue(functionalSql.contains("ALTER TABLE provider_location DROP COLUMN IF EXISTS website_url"));
		assertTrue(functionalSql.contains("ALTER TABLE provider_location DROP COLUMN IF EXISTS email_address"));
		assertTrue(functionalSql.contains("ALTER TABLE clinic_location DROP COLUMN IF EXISTS phone_number"));
		assertTrue(functionalSql.contains("ALTER TABLE clinic_location DROP COLUMN IF EXISTS website_url"));
		assertTrue(functionalSql.contains("ALTER TABLE clinic_location DROP COLUMN IF EXISTS email_address"));
		assertTrue(functionalSql.contains("ALTER TABLE institution_location DROP COLUMN IF EXISTS address_id"));
		assertTrue(fixtureSql.contains("'250-autism-clinic-referrer'"));
		assertTrue(fixtureSql.contains("'259-provider-booking-database'"));
		assertTrue(fixtureSql.contains("'259-cobalt-provider-booking-configuration'"));
		assertFalse(fixtureSql.contains("257-provider-booking-contact-" + "ownership-cleanup"));
	}

	@Test
	public void appointmentBookingDestinationUsesTerminalScoringOutcome() throws IOException {
		String functionalSql = readSql("sql/updates/259-provider-booking-database.sql");

		assertTrue(functionalSql.contains("Boolean(screeningSessionScreening.belowScoringThreshold)"));
		assertTrue(functionalSql.contains("const eligible = !belowScoringThreshold;"));
		assertFalse(functionalSql.contains("const eligible = overallScore >= minimumEligibilityScore;"));
	}

	@Test
	public void pennAutismBookingDestinationDeclaresExplicitResult() throws IOException {
		String configurationSql = readSql("sql/updates/259-cobalt-provider-booking-configuration.sql");

		assertTrue(configurationSql.contains("output.context.result = 'SUCCESS';"));
		assertTrue(configurationSql.contains("output.context.result = 'FAILURE';"));
	}

	@Test
	public void nativeAppointmentUniquenessAndContactSnapshotAreInProviderBookingMigration() throws IOException {
		String functionalSql = readSql("sql/updates/259-provider-booking-database.sql");

		assertTrue(functionalSql.contains("appointment_native_active_provider_start_time_idx"));
		assertTrue(functionalSql.contains("contact_phone_number=COALESCE(app.contact_phone_number, a.phone_number)"));
		assertTrue(functionalSql.contains("first_name=COALESCE(app.first_name, a.first_name)"));
		assertTrue(functionalSql.contains("last_name=COALESCE(app.last_name, a.last_name)"));
	}

	protected int countOccurrences(String value, String substring) {
		return (value.length() - value.replace(substring, "").length()) / substring.length();
	}

	protected String readSql(String filename) throws IOException {
		return Files.readString(Path.of(filename), StandardCharsets.UTF_8);
	}

}
