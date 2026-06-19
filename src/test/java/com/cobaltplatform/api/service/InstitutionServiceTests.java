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

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Feature.FeatureId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionLocation;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.model.service.FeatureForInstitution;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.pyranid.Database;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class InstitutionServiceTests {
	@Test
	public void findLocationById() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			UUID institutionLocationId = database.queryForObject("""
					SELECT institution_location_id
					FROM institution_location
					WHERE institution_id=?
					AND name=?
					""", UUID.class, InstitutionId.COBALT, "Cobalt General").get();

			InstitutionLocation institutionLocation = institutionService.findLocationById(institutionLocationId).get();

			Assert.assertEquals(institutionLocationId, institutionLocation.getInstitutionLocationId());
			Assert.assertEquals(InstitutionId.COBALT, institutionLocation.getInstitutionId());
			Assert.assertFalse(institutionService.findLocationById(UUID.randomUUID()).isPresent());
			Assert.assertFalse(institutionService.findLocationById(null).isPresent());
		});
	}

	@Test
	public void findInstitutionByIdMapsBookingV2Enabled() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();

			database.execute("""
					UPDATE institution
					SET booking_v2_enabled=TRUE
					WHERE institution_id=?
					""", InstitutionId.COBALT);

			Institution institution = institutionService.findInstitutionById(InstitutionId.COBALT).get();

			Assert.assertEquals(Boolean.TRUE, institution.getBookingV2Enabled());
		});
	}

	@Test
	public void featuresUseProviderSearchUrlNamesWhenBookingV2Enabled() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();

			ensureFeatureSupportRole(database, FeatureId.THERAPY, SupportRoleId.CLINICIAN);

			Institution institution = institutionService.findInstitutionById(InstitutionId.COBALT).get();
			institution.setBookingV2Enabled(true);
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Map<FeatureId, FeatureForInstitution> featuresByFeatureId = featuresByFeatureId(institutionService.findFeaturesByInstitutionId(institution, account));

			Assert.assertEquals("/providers?featureId=THERAPY", featuresByFeatureId.get(FeatureId.THERAPY).getUrlName());
		});
	}

	@Test
	public void featuresKeepStoredUrlNamesWhenBookingV2Disabled() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();

			ensureFeatureSupportRole(database, FeatureId.THERAPY, SupportRoleId.CLINICIAN);

			Institution institution = institutionService.findInstitutionById(InstitutionId.COBALT).get();
			institution.setBookingV2Enabled(false);
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Map<FeatureId, FeatureForInstitution> featuresByFeatureId = featuresByFeatureId(institutionService.findFeaturesByInstitutionId(institution, account));

			Assert.assertEquals("/connect-with-support/therapy", featuresByFeatureId.get(FeatureId.THERAPY).getUrlName());
		});
	}

	@Test
	public void featuresKeepStoredUrlNamesForNonProviderSearchFeaturesWhenBookingV2Enabled() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();

			database.execute("""
					DELETE FROM feature_support_role
					WHERE feature_id=?
					""", FeatureId.SELF_HELP_RESOURCES);

			Institution institution = institutionService.findInstitutionById(InstitutionId.COBALT).get();
			institution.setBookingV2Enabled(true);
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Map<FeatureId, FeatureForInstitution> featuresByFeatureId = featuresByFeatureId(institutionService.findFeaturesByInstitutionId(institution, account));

			Assert.assertEquals("/resource-library", featuresByFeatureId.get(FeatureId.SELF_HELP_RESOURCES).getUrlName());
		});
	}

	@Test
	public void featuresRewriteLegacyRecommendationBookingUrlOverridesWhenBookingV2Enabled() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();

			ensureFeatureSupportRole(database, FeatureId.THERAPY, SupportRoleId.CLINICIAN);
			ensureFeatureSupportRole(database, FeatureId.COACHING, SupportRoleId.COACH);
			setRecommendationBookingUrlOverride(database, FeatureId.THERAPY, "/connect-with-support/therapy");
			setRecommendationBookingUrlOverride(database, FeatureId.COACHING, "/connect-with-care/coaching");

			Institution institution = institutionService.findInstitutionById(InstitutionId.COBALT).get();
			institution.setBookingV2Enabled(true);
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Map<FeatureId, FeatureForInstitution> featuresByFeatureId = featuresByFeatureId(institutionService.findFeaturesByInstitutionId(institution, account));

			Assert.assertEquals("/providers?featureId=THERAPY", featuresByFeatureId.get(FeatureId.THERAPY).getRecommendationBookingUrlOverride());
			Assert.assertEquals("/providers?featureId=COACHING", featuresByFeatureId.get(FeatureId.COACHING).getRecommendationBookingUrlOverride());
		});
	}

	@Test
	public void featuresKeepNonLegacyRecommendationBookingUrlOverridesWhenBookingV2Enabled() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			String customRecommendationBookingUrlOverride = "/custom/provider-booking";

			ensureFeatureSupportRole(database, FeatureId.THERAPY, SupportRoleId.CLINICIAN);
			setRecommendationBookingUrlOverride(database, FeatureId.THERAPY, customRecommendationBookingUrlOverride);

			Institution institution = institutionService.findInstitutionById(InstitutionId.COBALT).get();
			institution.setBookingV2Enabled(true);
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Map<FeatureId, FeatureForInstitution> featuresByFeatureId = featuresByFeatureId(institutionService.findFeaturesByInstitutionId(institution, account));

			Assert.assertEquals(customRecommendationBookingUrlOverride, featuresByFeatureId.get(FeatureId.THERAPY).getRecommendationBookingUrlOverride());
		});
	}

	@Test
	public void careTypesForInstitution() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			String therapyOverride = "Therapy Override";

			database.execute("""
					UPDATE institution_feature
					SET nav_visible=TRUE,
					    name_override=NULL
					WHERE institution_id=?
					AND feature_id IN (?,?,?,?,?)
					""", InstitutionId.COBALT, FeatureId.THERAPY, FeatureId.COACHING, FeatureId.MEDICATION_PRESCRIBER,
					FeatureId.SELF_HELP_RESOURCES, FeatureId.SPIRITUAL_SUPPORT);

			database.execute("""
					UPDATE institution_feature
					SET name_override=?
					WHERE institution_id=?
					AND feature_id=?
					""", therapyOverride, InstitutionId.COBALT, FeatureId.THERAPY);

			database.execute("""
					UPDATE institution_feature
					SET nav_visible=FALSE
					WHERE institution_id=?
					AND feature_id=?
					""", InstitutionId.COBALT, FeatureId.COACHING);

			database.execute("""
					DELETE FROM feature_support_role
					WHERE feature_id=?
					""", FeatureId.MEDICATION_PRESCRIBER);

			database.execute("""
					INSERT INTO feature_support_role (feature_id, support_role_id)
					VALUES (?,?)
					ON CONFLICT (feature_id, support_role_id) DO NOTHING
					""", FeatureId.SELF_HELP_RESOURCES, SupportRoleId.CLINICIAN);

			List<FeatureForInstitution> careTypes = institutionService.findCareTypesByInstitutionId(InstitutionId.COBALT);
			Map<FeatureId, FeatureForInstitution> careTypesByFeatureId = featuresByFeatureId(careTypes);

			Assert.assertTrue("Expected visible CONNECT_WITH_SUPPORT feature with support role to be included",
					careTypesByFeatureId.containsKey(FeatureId.THERAPY));
			Assert.assertEquals("Expected institution-specific care type name override",
					therapyOverride, careTypesByFeatureId.get(FeatureId.THERAPY).getName());
			Assert.assertEquals("Expected care type to navigate to v2 provider search",
					"/providers?featureId=THERAPY", careTypesByFeatureId.get(FeatureId.THERAPY).getUrlName());
			Assert.assertFalse("Expected hidden feature to be excluded",
					careTypesByFeatureId.containsKey(FeatureId.COACHING));
			Assert.assertFalse("Expected CONNECT_WITH_SUPPORT feature without support role to be excluded",
					careTypesByFeatureId.containsKey(FeatureId.MEDICATION_PRESCRIBER));
			Assert.assertFalse("Expected non-CONNECT_WITH_SUPPORT feature to be excluded",
					careTypesByFeatureId.containsKey(FeatureId.SELF_HELP_RESOURCES));
		});
	}

	private static void ensureFeatureSupportRole(Database database,
																							 FeatureId featureId,
																							 SupportRoleId supportRoleId) {
		database.execute("""
				INSERT INTO feature_support_role (feature_id, support_role_id)
				VALUES (?,?)
				ON CONFLICT (feature_id, support_role_id) DO NOTHING
				""", featureId, supportRoleId);
	}

	private static void setRecommendationBookingUrlOverride(Database database,
																													FeatureId featureId,
																													String recommendationBookingUrlOverride) {
		database.execute("""
				UPDATE institution_feature
				SET recommendation_booking_url_override=?
				WHERE institution_id=?
				AND feature_id=?
				""", recommendationBookingUrlOverride, InstitutionId.COBALT, featureId);
	}

	private static Map<FeatureId, FeatureForInstitution> featuresByFeatureId(List<FeatureForInstitution> features) {
		return features.stream()
				.collect(Collectors.toMap(FeatureForInstitution::getFeatureId, feature -> feature));
	}
}
