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
import static org.junit.Assert.assertTrue;

/**
 * @author Transmogrify, LLC.
 */
public class ReferrerMetadataSqlTests {
	@Test
	public void autismReferrerMetadataSeedsSingularAppointmentTypeIdForResultScreenBookings() throws IOException {
		String sql = readSql("sql/updates/250-autism-clinic-referrer.sql");

		assertEquals(6, countOccurrences(sql, "\"appointmentTypeId\": \"%s\""));
		assertEquals(6, countOccurrences(sql, "\"appointmentTypeIds\": [\"%s\"]"));
	}

	@Test
	public void resultScreenBookingPathMigrationsWriteAppointmentTypeIdAndPath() throws IOException {
		assertResultScreenBookingMigrationWritesAppointmentTypeIdAndPath(
				readSql("sql/updates/259-referrer-result-screen-booking-confirmation-path.sql"));
		assertResultScreenBookingMigrationWritesAppointmentTypeIdAndPath(
				readSql("sql/updates/261-referrer-result-screen-booking-appointment-type-id.sql"));
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

	protected int countOccurrences(String value,
																 String substring) {
		int count = 0;
		int index = 0;

		while ((index = value.indexOf(substring, index)) >= 0) {
			++count;
			index += substring.length();
		}

		return count;
	}
}
