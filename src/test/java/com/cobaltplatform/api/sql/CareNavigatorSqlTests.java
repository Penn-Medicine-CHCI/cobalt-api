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

package com.cobaltplatform.api.sql;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CareNavigatorSqlTests {
	@Test
	public void productionMigrationDefinesNavigatorLookupsWithoutFixtureData() throws IOException {
		String migrationSql = readSql("sql/updates/260-care-navigator.sql");
		String institutionServiceJava = readSql("src/main/java/com/cobaltplatform/api/service/InstitutionService.java");

		assertTrue(migrationSql.contains("'CARE_NAVIGATOR', 'Care Navigator', 12"));
		assertTrue(migrationSql.contains("'NAVIGATOR', 'Care Navigator'"));
		assertTrue(migrationSql.contains("'RESOURCE_NAVIGATOR'"));
		assertTrue(migrationSql.contains("SET name='Connect with a Care Navigator'"));
		assertTrue(migrationSql.contains("ADD COLUMN IF NOT EXISTS provider_id UUID REFERENCES provider(provider_id)"));
		assertTrue(migrationSql.contains("ON CONFLICT (support_role_id) DO UPDATE"));
		assertTrue(migrationSql.contains("ON CONFLICT (account_capability_type_id) DO UPDATE"));
		assertTrue(migrationSql.contains("LOWER('admin@cobaltinnovations.org')"));
		assertTrue(migrationSql.contains("account.role_id='ADMINISTRATOR'"));
		assertTrue(migrationSql.contains("ON CONFLICT (account_id, account_capability_type_id) DO NOTHING"));
		assertTrue(migrationSql.contains("CREATE TABLE care_navigator_provider_account"));
		assertTrue(migrationSql.contains("PRIMARY KEY (provider_id, account_id)"));
		assertTrue(migrationSql.contains("CREATE OR REPLACE FUNCTION validate_care_navigator_provider_account()"));
		assertTrue(migrationSql.contains("ON CONFLICT (feature_id, support_role_id) DO NOTHING"));
		assertTrue(migrationSql.contains("CREATE OR REPLACE FUNCTION validate_care_navigator_booking_provider()"));
		assertTrue(migrationSql.contains("NEW.provider_id IS NULL"));
		assertTrue(migrationSql.contains("provider.institution_id=NEW.institution_id"));
		assertTrue(migrationSql.contains("provider.active=TRUE"));
		assertTrue(migrationSql.contains("provider_support_role.support_role_id='CARE_NAVIGATOR'"));
		assertTrue(institutionServiceJava.contains("findCareNavigatorBookingProviderIdForInstitutionId"));
		assertTrue(institutionServiceJava.contains("removeUnavailableCareNavigatorFeature"));
		assertFalse(migrationSql.contains("care-navigator@cobaltinnovations.org"));
		assertFalse(migrationSql.contains("Care Navigation Consultation"));
	}

	@Test
	public void localFixtureBuildsCompleteNavigatorBookingGraph() throws IOException {
		String fixtureSql = readSql("sql/local/260-care-navigator-seed.sql");

		assertTrue(fixtureSql.contains("'259-local-only-provider-booking-seed'"));
		assertTrue(fixtureSql.contains("'260-care-navigator'"));
		assertTrue(fixtureSql.contains("care-navigator@cobaltinnovations.org"));
		assertTrue(fixtureSql.contains("'ADMINISTRATOR'"));
		assertTrue(fixtureSql.contains("'CARE_NAVIGATOR'"));
		assertTrue(fixtureSql.contains("'NAVIGATOR'"));
		assertTrue(fixtureSql.contains("INSERT INTO care_navigator_provider_account"));
		assertTrue(fixtureSql.contains("'RESOURCE_NAVIGATOR'"));
		assertFalse(fixtureSql.contains("name_override"));
		assertTrue(fixtureSql.contains("provider_id=EXCLUDED.provider_id"));
		assertTrue(fixtureSql.contains("'Cobalt Care Navigation'"));
		assertTrue(fixtureSql.contains("'Care Navigation'"));
		assertTrue(fixtureSql.contains("v_provider_name CONSTANT TEXT := 'Care Navigator'"));
		assertTrue(fixtureSql.contains("'SWITCHBOARD'"));
		assertFalse(fixtureSql.contains("'MICROSOFT_TEAMS'"));
		assertTrue(fixtureSql.contains("Your appointment is a 30 minute video call with a Care Navigator to discuss potential resources."));
		assertTrue(fixtureSql.contains("What is a Care Navigator"));
		assertTrue(fixtureSql.contains("Care Navigators are not licensed clinicians"));
		assertTrue(fixtureSql.contains("please call 911 or 988 immediately"));
		assertTrue(fixtureSql.contains("Your privacy is important to us"));
		assertFalse(fixtureSql.contains("Replace with image"));
		assertFalse(fixtureSql.contains("add number"));
		assertTrue(fixtureSql.contains("https://placehold.co/320x320/png?text=Care+Navigator"));
		assertTrue(fixtureSql.contains("https://fixtures.cobalt.care/providers/cobalt-care-navigator"));
		assertTrue(fixtureSql.contains("'[\"Provider matching\", \"Care options\", \"Mental health navigation\"]'"));
		assertTrue(fixtureSql.contains("INSERT INTO provider_payment_type"));
		assertTrue(fixtureSql.contains("'NO_FEE'"));
		assertTrue(fixtureSql.contains("INSERT INTO provider_location"));
		assertTrue(fixtureSql.contains("'Cobalt Virtual Care'"));
		assertTrue(fixtureSql.contains("Care Navigation Consultation"));
		assertTrue(fixtureSql.contains("Care Navigator Booking Assessment"));
		assertTrue(fixtureSql.contains("What would you like help navigating?"));
		assertTrue(fixtureSql.contains("Finding a mental health provider"));
		assertTrue(fixtureSql.contains("Understanding care options"));
		assertTrue(fixtureSql.contains("Something else"));
		assertTrue(fixtureSql.contains("What type of support would be most useful right now?"));
		assertTrue(fixtureSql.contains("Finding an in-network provider"));
		assertTrue(fixtureSql.contains("Understanding costs and benefits"));
		assertTrue(fixtureSql.contains("Preparing for a first appointment"));
		assertTrue(fixtureSql.contains("How would you prefer your Care Navigator to follow up?"));
		assertTrue(fixtureSql.contains("'Email'"));
		assertTrue(fixtureSql.contains("'Phone'"));
		assertTrue(fixtureSql.contains("'No preference'"));
		assertTrue(fixtureSql.contains("Is there anything else you would like your Care Navigator to know?"));
		assertTrue(fixtureSql.contains("'FREEFORM_TEXT'"));
		assertTrue(fixtureSql.contains("Share any context that would be helpful."));
		assertTrue(fixtureSql.contains("'PROVIDER_INTAKE'"));
		assertTrue(fixtureSql.contains("'APPOINTMENT_BOOKING_CONFIRMATION'"));
		assertTrue(fixtureSql.contains("output.context.result = belowScoringThreshold ? 'FAILURE' : 'SUCCESS';"));
		assertTrue(fixtureSql.contains("screening_flow_id=EXCLUDED.screening_flow_id"));
		assertTrue(fixtureSql.contains("existing.appointment_type_id=v_appointment_type_id"));
		assertTrue(fixtureSql.contains("ON CONFLICT (logical_availability_id, appointment_type_id) DO NOTHING"));
	}

	@Test
	public void databaseRecreationRunsNavigatorMigrationBeforeFixture() throws IOException {
		assertNavigatorPatchOrder(readSql("sql/recreate-local"));
		assertNavigatorPatchOrder(readSql("sql/recreate-bootstrap"));
	}

	protected void assertNavigatorPatchOrder(String recreateSql) {
		int migrationIndex = recreateSql.indexOf("updates/260-care-navigator.sql");
		int fixtureIndex = recreateSql.indexOf("local/260-care-navigator-seed.sql");

		assertTrue(migrationIndex >= 0);
		assertTrue(fixtureIndex > migrationIndex);
	}

	protected String readSql(String filename) throws IOException {
		return Files.readString(Path.of(filename), StandardCharsets.UTF_8);
	}
}
