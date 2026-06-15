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

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.model.db.InstitutionReferrer;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Transmogrify, LLC.
 */
public class InstitutionReferrerApiResponseTests {
	@Test
	public void metadataIncludesScreenedAppointmentTypeIdInBookingMetadataAndPath() {
		UUID appointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000011");
		UUID staleAppointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000099");
		InstitutionReferrer institutionReferrer = institutionReferrer(format("""
				{
				  "resultScreens": {
				    "CONSULT_EVALUATION": {
				      "booking": {
				        "path": "/provider-confirm-appointment-time?providerSearchResultTypeId=CLINIC&returnTo=%%2Freferrals%%2Fautism-clinic&appointmentTypeId=%s#times",
				        "appointmentTypeIds": ["%s"]
				      }
				    }
				  }
				}
				""", staleAppointmentTypeId, staleAppointmentTypeId));

		Map<String, Object> metadata = InstitutionReferrerApiResponse.metadataFor(institutionReferrer, appointmentTypeId);
		Map<String, Object> resultScreens = map(metadata.get("resultScreens"));
		Map<String, Object> resultScreen = map(resultScreens.get("CONSULT_EVALUATION"));
		Map<String, Object> booking = map(resultScreen.get("booking"));
		String path = (String) booking.get("path");

		assertEquals(appointmentTypeId.toString(), booking.get("appointmentTypeId"));
		assertTrue(path.contains("returnTo=%2Freferrals%2Fautism-clinic"));
		assertTrue(path.contains(format("appointmentTypeId=%s", appointmentTypeId)));
		assertTrue(path.endsWith("#times"));
		assertFalse(path.contains(staleAppointmentTypeId.toString()));
	}

	@Test
	public void metadataAppendsScreenedAppointmentTypeIdToTopLevelBookingPath() {
		UUID appointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000011");
		InstitutionReferrer institutionReferrer = institutionReferrer("""
				{
				  "booking": {
				    "path": "/provider-confirm-appointment-time?providerSearchResultTypeId=CLINIC&clinicId=00000000-0000-0000-0000-000000000001"
				  }
				}
				""");

		Map<String, Object> metadata = InstitutionReferrerApiResponse.metadataFor(institutionReferrer, appointmentTypeId);
		Map<String, Object> booking = map(metadata.get("booking"));
		String path = (String) booking.get("path");

		assertEquals(appointmentTypeId.toString(), booking.get("appointmentTypeId"));
		assertTrue(path.contains("clinicId=00000000-0000-0000-0000-000000000001"));
		assertTrue(path.contains(format("appointmentTypeId=%s", appointmentTypeId)));
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object> map(Object value) {
		return (Map<String, Object>) value;
	}

	protected InstitutionReferrer institutionReferrer(String metadata) {
		InstitutionReferrer institutionReferrer = new InstitutionReferrer();
		institutionReferrer.setMetadataAsString(metadata);
		return institutionReferrer;
	}
}
