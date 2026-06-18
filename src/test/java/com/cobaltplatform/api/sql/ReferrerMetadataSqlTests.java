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
	public void providerClinicDetailsHtmlMigrationWritesPennAutismSemanticContent() throws IOException {
		String sql = readSql("sql/updates/258-provider-clinic-details-html.sql");

		assertTrue(sql.contains("ALTER TABLE provider ADD COLUMN IF NOT EXISTS details_html TEXT"));
		assertTrue(sql.contains("ALTER TABLE clinic ADD COLUMN IF NOT EXISTS details_html TEXT"));
		assertTrue(sql.contains("<section class=\"mb-8\">"));
		assertTrue(sql.contains("<h2 class=\"mb-4\">About</h2>"));
		assertTrue(sql.contains("<h2 class=\"mb-4\">Accepted Insurances</h2>"));
		assertTrue(sql.contains("<div class=\"table-responsive\">"));
		assertTrue(sql.contains("<table class=\"table table-bordered align-middle mb-0\">"));
		assertTrue(sql.contains("<th scope=\"col\">Health Insurance</th>"));
		assertTrue(sql.contains("Aetna Choice Point-of-Service (POS) II"));
		assertTrue(sql.contains("Quest Behavioral Health"));
		assertTrue(sql.contains("LOWER(TRIM(description)) = LOWER(TRIM('Penn Autism Clinic'))"));
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
