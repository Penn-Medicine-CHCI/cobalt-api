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
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.model.api.response.InstitutionApiResponse;
import com.cobaltplatform.api.model.api.response.InstitutionApiResponse.InstitutionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionLocationApiResponse;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.pyranid.Database;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import org.junit.Test;

import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Transmogrify, LLC.
 */
public class InstitutionResourceTests {
	@Test
	public void getLocationsReturnsOptionalGroupsInDisplayOrder() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionResource institutionResource = app.getInjector().getInstance(InstitutionResource.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			JsonMapper jsonMapper = app.getInjector().getInstance(JsonMapper.class);
			UUID firstGroupId = UUID.randomUUID();
			UUID secondGroupId = UUID.randomUUID();
			UUID secondLocationInFirstGroupId = UUID.randomUUID();
			UUID firstLocationInFirstGroupId = UUID.randomUUID();
			UUID secondGroupLocationId = UUID.randomUUID();
			UUID ungroupedLocationId = UUID.randomUUID();

			database.execute("""
					INSERT INTO institution_location_group (
					  institution_location_group_id,
					  institution_id,
					  name,
					  display_order
					) VALUES
					  (?, 'COBALT', 'First Employer', 1),
					  (?, 'COBALT', 'Second Employer', 2)
					""", firstGroupId, secondGroupId);

			database.execute("""
					INSERT INTO institution_location (
					  institution_location_id,
					  institution_id,
					  name,
					  institution_location_group_id,
					  display_order
					) VALUES
					  (?, 'COBALT', 'First Group Location Two', ?, 992),
					  (?, 'COBALT', 'First Group Location One', ?, 991),
					  (?, 'COBALT', 'Second Group Location', ?, 1),
					  (?, 'COBALT', 'Ungrouped Location', NULL, 993)
					""", secondLocationInFirstGroupId, firstGroupId,
					firstLocationInFirstGroupId, firstGroupId,
					secondGroupLocationId, secondGroupId,
					ungroupedLocationId);

			ApiResponse response = institutionResource.getLocations();
			List<InstitutionLocationApiResponse> locations = responseModelValue(response, "locations");
			List<InstitutionLocationApiResponse> insertedLocations = locations.stream()
					.filter(location -> List.of(firstLocationInFirstGroupId, secondLocationInFirstGroupId,
							secondGroupLocationId, ungroupedLocationId)
							.contains(location.getInstitutionLocationId()))
					.toList();

			assertEquals(List.of(firstLocationInFirstGroupId, secondLocationInFirstGroupId,
					secondGroupLocationId, ungroupedLocationId),
					insertedLocations.stream().map(InstitutionLocationApiResponse::getInstitutionLocationId).toList());
			assertEquals(firstGroupId, insertedLocations.get(0).getInstitutionLocationGroup().get()
					.getInstitutionLocationGroupId());
			assertEquals("First Employer", insertedLocations.get(0).getInstitutionLocationGroup().get().getName());
			assertEquals(Integer.valueOf(1), insertedLocations.get(0).getInstitutionLocationGroup().get().getDisplayOrder());
			assertEquals(firstGroupId, insertedLocations.get(1).getInstitutionLocationGroup().get()
					.getInstitutionLocationGroupId());
			assertEquals(secondGroupId, insertedLocations.get(2).getInstitutionLocationGroup().get()
					.getInstitutionLocationGroupId());
			assertFalse(insertedLocations.get(3).getInstitutionLocationGroup().isPresent());

			Map<String, Object> serializedGroupedLocation = jsonMapper.toMap(insertedLocations.get(0));
			Map<?, ?> serializedGroup = (Map<?, ?>) serializedGroupedLocation
					.get("institutionLocationGroup");
			Map<String, Object> serializedUngroupedLocation = jsonMapper.toMap(insertedLocations.get(3));

			assertEquals(firstGroupId.toString(), serializedGroup.get("institutionLocationGroupId"));
			assertEquals("First Employer", serializedGroup.get("name"));
			assertEquals(Double.valueOf(1), serializedGroup.get("displayOrder"));
			assertFalse(serializedUngroupedLocation.containsKey("institutionLocationGroup"));
		});
	}

	@Test(expected = RuntimeException.class)
	public void institutionLocationGroupRejectsCaseInsensitiveDuplicateNameWithinInstitution() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			String groupName = "Employer " + UUID.randomUUID();

			database.execute("""
					INSERT INTO institution_location_group (institution_id, name, display_order)
					VALUES ('COBALT', ?, 1)
					""", groupName);
			database.execute("""
					INSERT INTO institution_location_group (institution_id, name, display_order)
					VALUES ('COBALT', ?, 2)
					""", groupName.toUpperCase(Locale.US));
		});
	}

	@Test(expected = RuntimeException.class)
	public void institutionLocationGroupRejectsBlankName() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();

			database.execute("""
					INSERT INTO institution_location_group (institution_id, name, display_order)
					VALUES ('COBALT', '   ', 1)
					""");
		});
	}

	@Test(expected = RuntimeException.class)
	public void institutionLocationRejectsGroupFromAnotherInstitution() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			UUID institutionLocationGroupId = UUID.randomUUID();

			database.execute("""
					INSERT INTO institution_location_group (
					  institution_location_group_id,
					  institution_id,
					  name,
					  display_order
					) VALUES (?, 'COBALT', 'Cross Institution Group', 1)
					""", institutionLocationGroupId);
			database.execute("""
					INSERT INTO institution_location (
					  institution_location_id,
					  institution_id,
					  name,
					  institution_location_group_id,
					  display_order
					) VALUES (?, 'COBALT_IC', 'Cross Institution Location', ?, 1)
					""", UUID.randomUUID(), institutionLocationGroupId);
		});
	}

	@Test
	public void getInstitutionHidesBookingV2ForIntegratedCareWhenStoredFlagIsEnabled() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionApiResponseFactory institutionApiResponseFactory = app.getInjector()
					.getInstance(InstitutionApiResponseFactory.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			Account account = app.getInjector().getInstance(AccountService.class)
					.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Institution institution = institutionService.findInstitutionById(InstitutionId.COBALT).get();
			institution.setIntegratedCareEnabled(true);
			institution.setBookingV2Enabled(true);
			CurrentContext currentContext = new CurrentContext.Builder(account, Locale.US,
					ZoneId.of("America/New_York")).build();

			InstitutionApiResponse response = institutionApiResponseFactory.create(institution, currentContext);

			assertEquals(Boolean.FALSE, response.getBookingV2Enabled());
		});
	}

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
			UUID institutionLocationGroupId = UUID.randomUUID();

			database.execute("""
					INSERT INTO institution_location_group (
					  institution_location_group_id,
					  institution_id,
					  name,
					  display_order
					) VALUES (?, 'COBALT', 'Example Employer', 3)
					""", institutionLocationGroupId);
			database.execute("""
					UPDATE institution_location
					SET institution_location_group_id=?
					WHERE institution_location_id=?
					""", institutionLocationGroupId, institutionLocationId);

			setBookingV2Enabled(database, true);

			ApiResponse response = institutionResource.getLocation(institutionLocationId);
			InstitutionLocationApiResponse location = responseModelValue(response, "location");

			assertEquals(200, response.status());
			assertEquals(institutionLocationId, location.getInstitutionLocationId());
			assertEquals(InstitutionId.COBALT, location.getInstitutionId());
			assertEquals(institutionLocationGroupId, location.getInstitutionLocationGroup().get()
					.getInstitutionLocationGroupId());
			assertEquals("Example Employer", location.getInstitutionLocationGroup().get().getName());
			assertEquals(Integer.valueOf(3), location.getInstitutionLocationGroup().get().getDisplayOrder());
		});
	}

	@Test(expected = NotFoundException.class)
	public void getLocationRejectsMissingLocation() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionResource institutionResource = app.getInjector().getInstance(InstitutionResource.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();

			setBookingV2Enabled(database, true);

			institutionResource.getLocation(UUID.randomUUID());
		});
	}

	@Test(expected = AuthorizationException.class)
	public void getLocationRejectsLocationFromAnotherInstitution() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionResource institutionResource = app.getInjector().getInstance(InstitutionResource.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			UUID institutionLocationId = UUID.randomUUID();

			setBookingV2Enabled(database, true);

			database.execute("""
					INSERT INTO institution_location (institution_location_id, institution_id, name, display_order)
					VALUES (?, ?, ?, ?)
					""", institutionLocationId, InstitutionId.COBALT_IC, "Cross Institution Location", 1);

			institutionResource.getLocation(institutionLocationId);
		});
	}

	@Test(expected = NotFoundException.class)
	public void getLocationReturnsNotFoundWhenBookingV2Disabled() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionResource institutionResource = app.getInjector().getInstance(InstitutionResource.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();

			setBookingV2Enabled(database, false);

			institutionResource.getLocation(UUID.randomUUID());
		});
	}

	@Test(expected = NotFoundException.class)
	public void getInstitutionCareTypesReturnsNotFoundWhenBookingV2Disabled() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionResource institutionResource = app.getInjector().getInstance(InstitutionResource.class);
			Account account = app.getInjector().getInstance(AccountService.class)
					.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);

			setBookingV2Enabled(database, false);

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, ZoneId.of("America/New_York")).build(),
					institutionResource::getInstitutionCareTypes);
		});
	}

	@SuppressWarnings("unchecked")
	private static <T> T responseModelValue(ApiResponse response,
																					String key) {
		Map<String, Object> model = (Map<String, Object>) response.model().get();
		return (T) model.get(key);
	}

	private static void setBookingV2Enabled(Database database,
																	boolean enabled) {
		database.execute("UPDATE institution SET booking_v2_enabled=? WHERE institution_id=?", enabled, InstitutionId.COBALT);
	}
}
