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
import com.cobaltplatform.api.model.db.Clinic;
import com.cobaltplatform.api.model.db.Feature.FeatureId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.service.ProviderFind;
import com.cobaltplatform.api.model.service.ProviderSearchResult;
import com.cobaltplatform.api.model.service.ProviderSearchResult.ProviderSearchResultTypeId;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.pyranid.Database;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Transmogrify, LLC.
 */
public class ProviderServiceTests {
	@Test
	public void providerSearchResultsIncludeCurrentBookableClinicFixture() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ProviderService providerService = app.getInjector().getInstance(ProviderService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			UUID clinicId = UUID.fromString("ab629384-400a-4688-8465-04636ec2eaa2");
			UUID providerId = UUID.fromString("dc7aeafd-0fc8-4c4d-b09a-09d4dc3079c1");

			database.execute("ALTER TABLE clinic ADD COLUMN IF NOT EXISTS bookable_as_provider BOOLEAN NOT NULL DEFAULT FALSE");
			database.execute("UPDATE clinic SET bookable_as_provider=TRUE WHERE clinic_id=?", clinicId);

			List<ProviderSearchResult> providerSearchResults = providerService.findProviderSearchResults(FeatureId.SPIRITUAL_SUPPORT, null, account);

			assertTrue("Expected spiritual-support provider result to remain visible",
					providerSearchResults.stream()
							.anyMatch(providerSearchResult -> providerSearchResult.getProviderSearchResultTypeId() == ProviderSearchResultTypeId.PROVIDER
									&& providerSearchResult.getProviderSearchResultId().equals(providerId)));
			assertTrue("Expected current bookable clinic fixture to be included as a clinic result",
					providerSearchResults.stream()
							.anyMatch(providerSearchResult -> providerSearchResult.getProviderSearchResultTypeId() == ProviderSearchResultTypeId.CLINIC
									&& providerSearchResult.getProviderSearchResultId().equals(clinicId)));

			database.execute("UPDATE clinic SET bookable_as_provider=FALSE WHERE clinic_id=?", clinicId);
			providerSearchResults = providerService.findProviderSearchResults(FeatureId.SPIRITUAL_SUPPORT, null, account);

			assertTrue("Expected spiritual-support provider result to remain visible when clinic is not bookable",
					providerSearchResults.stream()
							.anyMatch(providerSearchResult -> providerSearchResult.getProviderSearchResultTypeId() == ProviderSearchResultTypeId.PROVIDER
									&& providerSearchResult.getProviderSearchResultId().equals(providerId)));
			assertFalse("Expected non-bookable clinic to be excluded as a clinic result",
					providerSearchResults.stream()
							.anyMatch(providerSearchResult -> providerSearchResult.getProviderSearchResultTypeId() == ProviderSearchResultTypeId.CLINIC
									&& providerSearchResult.getProviderSearchResultId().equals(clinicId)));
		});
	}

	@Test
	public void providerSearchResultsRespectCurrentLocationFixtureCategorization() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ProviderService providerService = app.getInjector().getInstance(ProviderService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			UUID cobaltGeneralLocationId = institutionLocationId(database, "Cobalt General");
			UUID cobaltHealthSystemLocationId = institutionLocationId(database, "Cobalt Health System");
			UUID adultAutismServicesClinicId = UUID.fromString("ab629384-400a-4688-8465-04636ec2eaa2");
			UUID rabbiGraysonProviderId = UUID.fromString("dc7aeafd-0fc8-4c4d-b09a-09d4dc3079c1");
			UUID joeFritzProviderId = UUID.fromString("360d46c4-2ee9-4031-aab6-aa6a16f398d7");

			database.execute("ALTER TABLE clinic ADD COLUMN IF NOT EXISTS bookable_as_provider BOOLEAN NOT NULL DEFAULT FALSE");
			database.execute("UPDATE clinic SET bookable_as_provider=TRUE WHERE clinic_id=?", adultAutismServicesClinicId);
			categorizeProvider(database, rabbiGraysonProviderId, "Cobalt General");
			categorizeProvider(database, joeFritzProviderId, "Cobalt Health System");

			List<ProviderSearchResult> cobaltGeneralSpiritualSupportResults =
					providerService.findProviderSearchResults(FeatureId.SPIRITUAL_SUPPORT, cobaltGeneralLocationId, account);
			List<ProviderSearchResult> cobaltHealthSystemSpiritualSupportResults =
					providerService.findProviderSearchResults(FeatureId.SPIRITUAL_SUPPORT, cobaltHealthSystemLocationId, account);

			assertContainsProviderSearchResult(cobaltGeneralSpiritualSupportResults, ProviderSearchResultTypeId.PROVIDER, rabbiGraysonProviderId);
			assertContainsProviderSearchResult(cobaltGeneralSpiritualSupportResults, ProviderSearchResultTypeId.CLINIC, adultAutismServicesClinicId);
			assertDoesNotContainProviderSearchResult(cobaltHealthSystemSpiritualSupportResults, ProviderSearchResultTypeId.PROVIDER, rabbiGraysonProviderId);
			assertDoesNotContainProviderSearchResult(cobaltHealthSystemSpiritualSupportResults, ProviderSearchResultTypeId.CLINIC, adultAutismServicesClinicId);

			List<ProviderSearchResult> cobaltHealthSystemTherapyResults =
					providerService.findProviderSearchResults(FeatureId.THERAPY, cobaltHealthSystemLocationId, account);
			List<ProviderSearchResult> cobaltGeneralTherapyResults =
					providerService.findProviderSearchResults(FeatureId.THERAPY, cobaltGeneralLocationId, account);

			assertContainsProviderSearchResult(cobaltHealthSystemTherapyResults, ProviderSearchResultTypeId.PROVIDER, joeFritzProviderId);
			assertDoesNotContainProviderSearchResult(cobaltGeneralTherapyResults, ProviderSearchResultTypeId.PROVIDER, joeFritzProviderId);
		});
	}

	@Test
	public void clinicBookableAsProvider() {
		Clinic clinic = new Clinic();

		clinic.setBookableAsProvider(true);

		assertTrue(ProviderService.clinicBookableAsProvider(clinic));
	}

	@Test
	public void clinicBookableAsProviderRejectsFalse() {
		Clinic clinic = new Clinic();

		clinic.setBookableAsProvider(false);

		assertFalse(ProviderService.clinicBookableAsProvider(clinic));
	}

	@Test
	public void clinicBookableAsProviderRejectsMissingFlag() {
		assertFalse(ProviderService.clinicBookableAsProvider(new Clinic()));
	}

	@Test
	public void providerSearchResultsIncludeBookableClinicWithoutAvailability() {
		UUID providerId = UUID.fromString("05d8cfa9-fd76-476b-bc74-c2dad239054d");
		UUID clinicId = UUID.fromString("a01108d7-871e-45b5-a3e2-9a2b71d23efc");

		List<ProviderSearchResult> providerSearchResults = ProviderService.providerSearchResultsFor(
				List.of(providerFind(providerId, "Spiritual Support")),
				Map.of(providerId, provider(providerId, "Spiritual Support")),
				Map.of(providerId, List.of(clinic(clinicId, "Spiritual Care", true))),
				Map.of());

		assertTrue("Expected clinic result even when linked provider has no available dates",
				providerSearchResults.stream()
						.anyMatch(providerSearchResult -> providerSearchResult.getProviderSearchResultTypeId() == ProviderSearchResultTypeId.CLINIC
								&& providerSearchResult.getProviderSearchResultId().equals(clinicId)));
	}

	@Test
	public void providerSearchResultsSortByNameTypeAndId() {
		UUID alphaProviderId = UUID.fromString("00000000-0000-0000-0000-000000000002");
		UUID alphaClinicId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID betaProviderId = UUID.fromString("00000000-0000-0000-0000-000000000003");

		List<ProviderSearchResult> providerSearchResults = ProviderService.providerSearchResultsFor(
				List.of(providerFind(betaProviderId, "Beta"), providerFind(alphaProviderId, "Alpha")),
				Map.of(
						betaProviderId, provider(betaProviderId, "Beta"),
						alphaProviderId, provider(alphaProviderId, "Alpha")),
				Map.of(alphaProviderId, List.of(clinic(alphaClinicId, "Alpha", true))),
				Map.of());

		assertEquals(alphaProviderId, providerSearchResults.get(0).getProviderSearchResultId());
		assertEquals(alphaClinicId, providerSearchResults.get(1).getProviderSearchResultId());
		assertEquals(betaProviderId, providerSearchResults.get(2).getProviderSearchResultId());
	}

	@Nonnull
	protected Provider provider(@Nonnull UUID providerId,
															@Nonnull String name) {
		Provider provider = new Provider();
		provider.setProviderId(providerId);
		provider.setName(name);
		provider.setInstitutionId(InstitutionId.COBALT);

		return provider;
	}

	@Nonnull
	protected ProviderFind providerFind(@Nonnull UUID providerId,
																			@Nonnull String name) {
		ProviderFind providerFind = new ProviderFind();
		providerFind.setProviderId(providerId);
		providerFind.setName(name);

		return providerFind;
	}

	@Nonnull
	protected Clinic clinic(@Nonnull UUID clinicId,
													@Nonnull String description,
													boolean bookableAsProvider) {
		Clinic clinic = new Clinic();
		clinic.setClinicId(clinicId);
		clinic.setDescription(description);
		clinic.setInstitutionId(InstitutionId.COBALT);
		clinic.setBookableAsProvider(bookableAsProvider);

		return clinic;
	}

	@Nonnull
	protected UUID institutionLocationId(@Nonnull Database database,
																			 @Nonnull String name) {
		return database.queryForObject("""
				SELECT institution_location_id
				FROM institution_location
				WHERE institution_id=?
				AND name=?
				""", UUID.class, InstitutionId.COBALT, name).get();
	}

	protected void categorizeProvider(@Nonnull Database database,
																	 @Nonnull UUID providerId,
																	 @Nonnull String institutionLocationName) {
		database.execute("DELETE FROM provider_institution_location WHERE provider_id=?", providerId);
		database.execute("""
				INSERT INTO provider_institution_location (
					provider_id,
					institution_location_id
				)
				SELECT ?, institution_location_id
				FROM institution_location
				WHERE institution_id=?
				AND name=?
				""", providerId, InstitutionId.COBALT, institutionLocationName);
	}

	protected void assertContainsProviderSearchResult(@Nonnull List<ProviderSearchResult> providerSearchResults,
																										@Nonnull ProviderSearchResultTypeId providerSearchResultTypeId,
																										@Nonnull UUID providerSearchResultId) {
		assertTrue(providerSearchResults.stream()
				.anyMatch(providerSearchResult -> providerSearchResult.getProviderSearchResultTypeId() == providerSearchResultTypeId
						&& providerSearchResult.getProviderSearchResultId().equals(providerSearchResultId)));
	}

	protected void assertDoesNotContainProviderSearchResult(@Nonnull List<ProviderSearchResult> providerSearchResults,
																												 @Nonnull ProviderSearchResultTypeId providerSearchResultTypeId,
																												 @Nonnull UUID providerSearchResultId) {
		assertFalse(providerSearchResults.stream()
				.anyMatch(providerSearchResult -> providerSearchResult.getProviderSearchResultTypeId() == providerSearchResultTypeId
						&& providerSearchResult.getProviderSearchResultId().equals(providerSearchResultId)));
	}
}
