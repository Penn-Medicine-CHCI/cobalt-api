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

package com.cobaltplatform.api.web.resource;

import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.model.api.response.InstitutionLocationApiResponse;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.pyranid.Database;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * @author Transmogrify, LLC.
 */
public class InstitutionResourceTests {
	@Test
	public void getLocationReturnsLocationForCurrentInstitution() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionResource institutionResource = app.getInjector().getInstance(InstitutionResource.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			UUID institutionLocationId = database.queryForObject("""
					SELECT institution_location_id
					FROM institution_location
					WHERE institution_id=?
					AND name=?
					""", UUID.class, InstitutionId.COBALT, "Cobalt General").get();

			ApiResponse response = institutionResource.getLocation(institutionLocationId);
			InstitutionLocationApiResponse location = responseModelValue(response, "location");

			assertEquals(200, response.status());
			assertEquals(institutionLocationId, location.getInstitutionLocationId());
			assertEquals(InstitutionId.COBALT, location.getInstitutionId());
		});
	}

	@Test(expected = NotFoundException.class)
	public void getLocationRejectsMissingLocation() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionResource institutionResource = app.getInjector().getInstance(InstitutionResource.class);

			institutionResource.getLocation(UUID.randomUUID());
		});
	}

	@Test(expected = AuthorizationException.class)
	public void getLocationRejectsLocationFromAnotherInstitution() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionResource institutionResource = app.getInjector().getInstance(InstitutionResource.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			UUID institutionLocationId = UUID.randomUUID();

			database.execute("""
					INSERT INTO institution_location (institution_location_id, institution_id, name, display_order)
					VALUES (?, ?, ?, ?)
					""", institutionLocationId, InstitutionId.COBALT_IC, "Cross Institution Location", 1);

			institutionResource.getLocation(institutionLocationId);
		});
	}

	@SuppressWarnings("unchecked")
	private static <T> T responseModelValue(ApiResponse response,
																					String key) {
		Map<String, Object> model = (Map<String, Object>) response.model().get();
		return (T) model.get(key);
	}
}
