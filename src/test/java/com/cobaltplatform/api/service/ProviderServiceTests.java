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
import com.cobaltplatform.api.model.api.request.ProviderFindRequest;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentSelectionTypeId;
import com.cobaltplatform.api.model.api.response.ProviderSearchResultApiResponse;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AppointmentBookingLevel.AppointmentBookingLevelId;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.Clinic;
import com.cobaltplatform.api.model.db.Feature.FeatureId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.service.AppointmentBookingRequirements.AppointmentBookingRequirementsDestinationId;
import com.cobaltplatform.api.model.service.AppointmentBookingScreeningKey;
import com.cobaltplatform.api.model.service.ProviderFind;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityDate;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityTime;
import com.cobaltplatform.api.model.service.ProviderSearchResult;
import com.cobaltplatform.api.model.service.ProviderSearchResult.ProviderSearchResultTypeId;
import com.cobaltplatform.api.model.service.ProviderSearchScreeningRequirement;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Transmogrify, LLC.
 */
public class ProviderServiceTests {
	@Test
	public void providerSearchResultsIncludeCurrentClinicLevelBookingFixture() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ProviderService providerService = app.getInjector().getInstance(ProviderService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			UUID clinicId = UUID.fromString("ab629384-400a-4688-8465-04636ec2eaa2");
			UUID providerId = UUID.fromString("dc7aeafd-0fc8-4c4d-b09a-09d4dc3079c1");

			database.execute("UPDATE clinic SET appointment_booking_level_id=? WHERE clinic_id=?",
					AppointmentBookingLevelId.CLINIC, clinicId);

			List<ProviderSearchResult> providerSearchResults = providerService.findProviderSearchResults(FeatureId.SPIRITUAL_SUPPORT, null, account);

			assertTrue("Expected spiritual-support provider result to remain visible",
					providerSearchResults.stream()
							.anyMatch(providerSearchResult -> providerSearchResult.getProviderSearchResultTypeId() == ProviderSearchResultTypeId.PROVIDER
									&& providerSearchResult.getProviderSearchResultId().equals(providerId)));
			assertTrue("Expected current clinic-level booking fixture to be included as a clinic result",
					providerSearchResults.stream()
							.anyMatch(providerSearchResult -> providerSearchResult.getProviderSearchResultTypeId() == ProviderSearchResultTypeId.CLINIC
									&& providerSearchResult.getProviderSearchResultId().equals(clinicId)
									&& providerSearchResult.getAppointmentBookingLevelId() == AppointmentBookingLevelId.CLINIC));

			database.execute("UPDATE clinic SET appointment_booking_level_id=? WHERE clinic_id=?",
					AppointmentBookingLevelId.PROVIDER, clinicId);
			providerSearchResults = providerService.findProviderSearchResults(FeatureId.SPIRITUAL_SUPPORT, null, account);

			assertTrue("Expected spiritual-support provider result to remain visible when clinic is not bookable",
					providerSearchResults.stream()
							.anyMatch(providerSearchResult -> providerSearchResult.getProviderSearchResultTypeId() == ProviderSearchResultTypeId.PROVIDER
									&& providerSearchResult.getProviderSearchResultId().equals(providerId)));
			assertFalse("Expected provider-level booking clinic to be excluded as a clinic result",
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
			UUID markAllenProviderId = UUID.fromString("15f9711d-38e1-44a1-a933-f1522ddd2c81");
			UUID rabbiGraysonProviderId = UUID.fromString("dc7aeafd-0fc8-4c4d-b09a-09d4dc3079c1");
			UUID joeFritzProviderId = UUID.fromString("360d46c4-2ee9-4031-aab6-aa6a16f398d7");
			UUID caseyWatsonProviderId = UUID.fromString("2d6b7032-0145-4273-84f5-94e7238bc331");
			UUID khaledShaabanProviderId = UUID.fromString("ed461fc4-0436-4880-b340-b075d56a06f4");
			UUID lizJonesProviderId = UUID.fromString("9dcc6e07-821e-4b64-8975-aee5fcd5ca8b");

			database.execute("UPDATE clinic SET appointment_booking_level_id=? WHERE clinic_id=?",
					AppointmentBookingLevelId.CLINIC, adultAutismServicesClinicId);
			ensureProviderSupportRole(database, markAllenProviderId, "CLINICIAN");
			ensureProviderCategorized(database, markAllenProviderId, "Cobalt General");
			ensureProviderCategorized(database, rabbiGraysonProviderId, "Cobalt General");
			ensureProviderCategorized(database, joeFritzProviderId, "Cobalt Health System");
			ensureProviderCategorized(database, caseyWatsonProviderId, "Cobalt General");
			ensureProviderCategorized(database, khaledShaabanProviderId, "Cobalt Health System");
			ensureProviderCategorized(database, lizJonesProviderId, "Cobalt General");

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

			assertContainsProviderSearchResult(cobaltGeneralTherapyResults, ProviderSearchResultTypeId.PROVIDER, markAllenProviderId);
			assertContainsProviderSearchResult(cobaltHealthSystemTherapyResults, ProviderSearchResultTypeId.PROVIDER, joeFritzProviderId);
			assertDoesNotContainProviderSearchResult(cobaltHealthSystemTherapyResults, ProviderSearchResultTypeId.PROVIDER, markAllenProviderId);
			assertDoesNotContainProviderSearchResult(cobaltGeneralTherapyResults, ProviderSearchResultTypeId.PROVIDER, joeFritzProviderId);

			List<ProviderSearchResult> cobaltGeneralCoachingResults =
					providerService.findProviderSearchResults(FeatureId.COACHING, cobaltGeneralLocationId, account);
			List<ProviderSearchResult> cobaltHealthSystemCoachingResults =
					providerService.findProviderSearchResults(FeatureId.COACHING, cobaltHealthSystemLocationId, account);

			assertContainsProviderSearchResult(cobaltGeneralCoachingResults, ProviderSearchResultTypeId.PROVIDER, caseyWatsonProviderId);
			assertContainsProviderSearchResult(cobaltGeneralCoachingResults, ProviderSearchResultTypeId.PROVIDER, lizJonesProviderId);
			assertContainsProviderSearchResult(cobaltHealthSystemCoachingResults, ProviderSearchResultTypeId.PROVIDER, khaledShaabanProviderId);
			assertDoesNotContainProviderSearchResult(cobaltHealthSystemCoachingResults, ProviderSearchResultTypeId.PROVIDER, caseyWatsonProviderId);
			assertDoesNotContainProviderSearchResult(cobaltGeneralCoachingResults, ProviderSearchResultTypeId.PROVIDER, khaledShaabanProviderId);
		});
	}

	@Test
	public void providerSearchResultsUseAutismClinicFullscreenScreeningFixture() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ProviderService providerService = app.getInjector().getInstance(ProviderService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Formatter formatter = app.getInjector().getInstance(Formatter.class);
			Strings strings = app.getInjector().getInstance(Strings.class);
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			UUID clinicId = database.queryForObject("""
					SELECT clinic_id
					FROM clinic
					WHERE institution_id=?
					AND description='Penn Autism Clinic'
					""", UUID.class, InstitutionId.COBALT).get();
			UUID providerId = database.queryForObject("""
					SELECT p.provider_id
					FROM provider p, provider_clinic pc
					WHERE p.provider_id=pc.provider_id
					AND pc.clinic_id=?
					AND p.name='John Skokowski'
					""", UUID.class, clinicId).get();
			UUID screeningFlowId = database.queryForObject("""
					SELECT intake_screening_flow_id
					FROM institution_referrer
					WHERE from_institution_id=?
					AND url_name='autism-clinic'
					""", UUID.class, InstitutionId.COBALT).get();
			String appointmentBookingLevelId = database.queryForObject("""
					SELECT appointment_booking_level_id
					FROM clinic
					WHERE clinic_id=?
					""", String.class, clinicId).get();
			Boolean fullscreen = database.queryForObject("""
					SELECT metadata->'screening'->>'fullscreen'='true'
					FROM institution_referrer
					WHERE from_institution_id=?
					AND url_name='autism-clinic'
					""", Boolean.class, InstitutionId.COBALT).orElse(false);
			Long assignedAppointmentTypeCount = database.queryForObject("""
					SELECT COUNT(*)
					FROM appointment_type
					WHERE scheduling_system_id='COBALT'
					AND name IN ('Autism Clinic Intake Call', 'Autism Clinic Consult Call')
					AND screening_flow_id=?
					""", Long.class, screeningFlowId).get();
			Long screeningSessionCountBefore = database.queryForObject("SELECT COUNT(*) FROM screening_session", Long.class).get();
			ProviderFindRequest request = new ProviderFindRequest();
			request.setInstitutionId(InstitutionId.COBALT);
			request.setClinicIds(Set.of(clinicId));

			List<ProviderSearchResult> providerSearchResults = providerService.findProviderSearchResults(request, account);
			ProviderSearchResult clinicProviderSearchResult = providerSearchResults.stream()
					.filter(providerSearchResult -> providerSearchResult.getProviderSearchResultTypeId() == ProviderSearchResultTypeId.CLINIC
							&& providerSearchResult.getProviderSearchResultId().equals(clinicId))
					.findFirst()
					.get();
			ProviderSearchResultApiResponse response = new ProviderSearchResultApiResponse(formatter, strings, clinicProviderSearchResult);
			ProviderSearchScreeningRequirement screeningRequirement = response.getScreeningRequirement();
			Long screeningSessionCountAfter = database.queryForObject("SELECT COUNT(*) FROM screening_session", Long.class).get();

			assertEquals(AppointmentBookingLevelId.CLINIC.name(), appointmentBookingLevelId);
			assertTrue(fullscreen);
			assertEquals(2L, assignedAppointmentTypeCount.longValue());
			assertNotNull(response.getFirstAvailableAppointment());
			assertEquals(providerId, response.getFirstAvailableAppointment().getProviderId());
			assertNull(response.getFirstAvailableAppointment().getAppointmentTypeId());
			assertEquals(2, response.getFirstAvailableAppointment().getAppointmentTypeIds().size());
			assertEquals(ProviderAppointmentSelectionTypeId.APPOINTMENT_UNDETERMINED, response.getAppointmentSelectionTypeId());
			assertNotNull(screeningRequirement);
			assertEquals(AppointmentBookingRequirementsDestinationId.SCREENING_SESSION,
					screeningRequirement.getAppointmentBookingRequirementsDestinationId());
			assertEquals(screeningFlowId, screeningRequirement.getScreeningFlowId());
			assertEquals(true, screeningRequirement.getScreeningRequired());
			assertEquals(false, screeningRequirement.getScreeningSatisfied());
			assertEquals(screeningSessionCountBefore, screeningSessionCountAfter);
		});
	}

	@Test
	public void providerSearchResultsUsePhoneFallbackForUnresolvedAppointmentTypeAmbiguityFixture() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			ProviderService providerService = app.getInjector().getInstance(ProviderService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Formatter formatter = app.getInjector().getInstance(Formatter.class);
			Strings strings = app.getInjector().getInstance(Strings.class);
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			UUID providerId = UUID.fromString("15f9711d-38e1-44a1-a933-f1522ddd2c81");
			UUID logicalAvailabilityId = UUID.fromString("409b6b18-78b4-4a0b-bb03-6c77ff100001");
			ProviderFindRequest request = new ProviderFindRequest();
			request.setInstitutionId(InstitutionId.COBALT);
			request.setProviderId(providerId);

			ensurePhoneBookingProviderFixture(database, providerId);
			ensureProviderLogicalAvailability(database, providerId, logicalAvailabilityId, account.getAccountId());

			List<ProviderSearchResult> providerSearchResults = providerService.findProviderSearchResults(request, account);
			ProviderSearchResult providerSearchResult = providerSearchResults.stream()
					.filter(result -> result.getProviderSearchResultTypeId() == ProviderSearchResultTypeId.PROVIDER
							&& result.getProviderSearchResultId().equals(providerId))
					.findFirst()
					.get();
			ProviderSearchResultApiResponse response = new ProviderSearchResultApiResponse(formatter, strings, providerSearchResult);

			assertNotNull(response.getFirstAvailableAppointment());
			assertNull(response.getFirstAvailableAppointment().getAppointmentTypeId());
			assertEquals(2, response.getFirstAvailableAppointment().getAppointmentTypeIds().size());
			assertNull(response.getScreeningRequirement());
			assertNotNull(response.getPhoneNumber());
			assertEquals(ProviderAppointmentSelectionTypeId.APPOINTMENT_BY_PHONE, response.getAppointmentSelectionTypeId());
		});
	}

	@Test
	public void clinicBookedAtClinicLevel() {
		Clinic clinic = new Clinic();

		clinic.setAppointmentBookingLevelId(AppointmentBookingLevelId.CLINIC);

		assertTrue(ProviderService.clinicBookedAtClinicLevel(clinic));
	}

	@Test
	public void clinicBookedAtClinicLevelRejectsProviderLevel() {
		Clinic clinic = new Clinic();

		clinic.setAppointmentBookingLevelId(AppointmentBookingLevelId.PROVIDER);

		assertFalse(ProviderService.clinicBookedAtClinicLevel(clinic));
	}

	@Test
	public void clinicBookedAtClinicLevelRejectsMissingLevel() {
		assertFalse(ProviderService.clinicBookedAtClinicLevel(new Clinic()));
	}

	@Test
	public void providerSearchResultsIncludeBookableClinicWithoutAvailability() {
		UUID providerId = UUID.fromString("05d8cfa9-fd76-476b-bc74-c2dad239054d");
		UUID clinicId = UUID.fromString("a01108d7-871e-45b5-a3e2-9a2b71d23efc");

		List<ProviderSearchResult> providerSearchResults = ProviderService.providerSearchResultsFor(
				List.of(providerFind(providerId, "Spiritual Support")),
				Map.of(providerId, provider(providerId, "Spiritual Support")),
				Map.of(providerId, List.of(clinic(clinicId, "Spiritual Care", AppointmentBookingLevelId.CLINIC))),
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
				Map.of(alphaProviderId, List.of(clinic(alphaClinicId, "Alpha", AppointmentBookingLevelId.CLINIC))),
				Map.of());

		assertEquals(alphaProviderId, providerSearchResults.get(0).getProviderSearchResultId());
		assertEquals(AppointmentBookingLevelId.PROVIDER, providerSearchResults.get(0).getAppointmentBookingLevelId());
		assertEquals(alphaClinicId, providerSearchResults.get(1).getProviderSearchResultId());
		assertEquals(AppointmentBookingLevelId.CLINIC, providerSearchResults.get(1).getAppointmentBookingLevelId());
		assertEquals(betaProviderId, providerSearchResults.get(2).getProviderSearchResultId());
		assertEquals(AppointmentBookingLevelId.PROVIDER, providerSearchResults.get(2).getAppointmentBookingLevelId());
	}

	@Test
	public void appointmentBookingScreeningKeysOnlyIncludeRequiredScreeningFlows() {
		UUID providerId = UUID.randomUUID();
		UUID noScreeningAppointmentTypeId = UUID.randomUUID();
		UUID screeningAppointmentTypeId = UUID.randomUUID();
		UUID screeningFlowId = UUID.randomUUID();
		ProviderFind providerFind = providerFind(providerId, "Provider", Set.of(noScreeningAppointmentTypeId, screeningAppointmentTypeId));
		Map<UUID, AppointmentType> appointmentTypesById = Map.of(
				noScreeningAppointmentTypeId, appointmentType(noScreeningAppointmentTypeId, "Consult", null, null),
				screeningAppointmentTypeId, appointmentType(screeningAppointmentTypeId, "Therapy", "Therapy intake", screeningFlowId));

		Set<AppointmentBookingScreeningKey> appointmentBookingScreeningKeys =
				ProviderService.appointmentBookingScreeningKeysFor(List.of(providerFind), appointmentTypesById);

		assertEquals(Set.of(new AppointmentBookingScreeningKey(providerId, screeningAppointmentTypeId, screeningFlowId)),
				appointmentBookingScreeningKeys);
	}

	@Test
	public void appointmentBookingScreeningKeysIncludeAvailabilityTimeAppointmentTypeIds() {
		UUID providerId = UUID.randomUUID();
		UUID appointmentTypeId = UUID.randomUUID();
		UUID screeningFlowId = UUID.randomUUID();
		ProviderFind providerFind = providerFind(providerId, "Provider", null);
		AvailabilityDate availabilityDate = new AvailabilityDate();
		AvailabilityTime availabilityTime = new AvailabilityTime();

		availabilityDate.setDate(LocalDate.of(2026, 1, 1));
		availabilityTime.setTime(LocalTime.NOON);
		availabilityTime.setAppointmentTypeIds(List.of(appointmentTypeId));
		availabilityDate.setTimes(List.of(availabilityTime));
		providerFind.setDates(List.of(availabilityDate));

		Map<UUID, AppointmentType> appointmentTypesById = Map.of(appointmentTypeId,
				appointmentType(appointmentTypeId, "Visit", null, screeningFlowId));

		Set<AppointmentBookingScreeningKey> appointmentBookingScreeningKeys =
				ProviderService.appointmentBookingScreeningKeysFor(List.of(providerFind), appointmentTypesById);

		assertEquals(Set.of(new AppointmentBookingScreeningKey(providerId, appointmentTypeId, screeningFlowId)),
				appointmentBookingScreeningKeys);
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
		return providerFind(providerId, name, null);
	}

	@Nonnull
	protected ProviderFind providerFind(@Nonnull UUID providerId,
																			@Nonnull String name,
																			@Nullable Set<UUID> appointmentTypeIds) {
		ProviderFind providerFind = new ProviderFind();
		providerFind.setProviderId(providerId);
		providerFind.setName(name);
		providerFind.setAppointmentTypeIds(appointmentTypeIds);

		return providerFind;
	}

	@Nonnull
	protected AppointmentType appointmentType(@Nonnull UUID appointmentTypeId,
																						@Nonnull String name,
																						@Nullable String description,
																						@Nullable UUID screeningFlowId) {
		AppointmentType appointmentType = new AppointmentType();
		appointmentType.setAppointmentTypeId(appointmentTypeId);
		appointmentType.setName(name);
		appointmentType.setDescription(description);
		appointmentType.setScreeningFlowId(screeningFlowId);

		return appointmentType;
	}

	@Nonnull
	protected Clinic clinic(@Nonnull UUID clinicId,
													@Nonnull String description,
													@Nullable AppointmentBookingLevelId appointmentBookingLevelId) {
		Clinic clinic = new Clinic();
		clinic.setClinicId(clinicId);
		clinic.setDescription(description);
		clinic.setInstitutionId(InstitutionId.COBALT);
		clinic.setAppointmentBookingLevelId(appointmentBookingLevelId);

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

	protected void ensureProviderCategorized(@Nonnull Database database,
																					 @Nonnull UUID providerId,
																					 @Nonnull String institutionLocationName) {
		database.execute("""
				INSERT INTO provider_institution_location (
					provider_id,
					institution_location_id
				)
				SELECT ?, institution_location_id
				FROM institution_location
				WHERE institution_id=?
				AND name=?
				AND NOT EXISTS (
					SELECT 1
					FROM provider_institution_location existing_provider_location
					WHERE existing_provider_location.provider_id=?
					AND existing_provider_location.institution_location_id=institution_location.institution_location_id
				)
				""", providerId, InstitutionId.COBALT, institutionLocationName, providerId);
	}

	protected void ensureProviderSupportRole(@Nonnull Database database,
																					 @Nonnull UUID providerId,
																					 @Nonnull String supportRoleId) {
		database.execute("""
				INSERT INTO provider_support_role (
					provider_id,
					support_role_id
				)
				SELECT ?, ?
				WHERE NOT EXISTS (
					SELECT 1
					FROM provider_support_role
					WHERE provider_id=?
					AND support_role_id=?
				)
				""", providerId, supportRoleId, providerId, supportRoleId);
	}

	protected void ensurePhoneBookingProviderFixture(@Nonnull Database database,
																									@Nonnull UUID providerId) {
		database.execute("""
				UPDATE provider
				SET phone_number=?,
				    videoconference_platform_id='TELEPHONE',
				    display_phone_number_only_for_booking=FALSE
				WHERE provider_id=?
				""", "+12155551001", providerId);
	}

	protected void ensureProviderLogicalAvailability(@Nonnull Database database,
																									@Nonnull UUID providerId,
																									@Nonnull UUID logicalAvailabilityId,
																									@Nonnull UUID accountId) {
		database.execute("""
				INSERT INTO logical_availability (
					logical_availability_id,
					provider_id,
					start_date_time,
					end_date_time,
					logical_availability_type_id,
					recurrence_type_id,
					recur_sunday,
					recur_monday,
					recur_tuesday,
					recur_wednesday,
					recur_thursday,
					recur_friday,
					recur_saturday,
					created_by_account_id,
					last_updated_by_account_id
				) VALUES (
					?,
					?,
					TIMESTAMP '2026-01-05 09:00:00',
					TIMESTAMP '2099-12-31 17:00:00',
					'OPEN',
					'DAILY',
					FALSE,
					TRUE,
					TRUE,
					TRUE,
					TRUE,
					TRUE,
					FALSE,
					?,
					?
				)
				ON CONFLICT (logical_availability_id) DO UPDATE
				SET provider_id=EXCLUDED.provider_id,
				    start_date_time=EXCLUDED.start_date_time,
				    end_date_time=EXCLUDED.end_date_time,
				    logical_availability_type_id=EXCLUDED.logical_availability_type_id,
				    recurrence_type_id=EXCLUDED.recurrence_type_id,
				    recur_sunday=EXCLUDED.recur_sunday,
				    recur_monday=EXCLUDED.recur_monday,
				    recur_tuesday=EXCLUDED.recur_tuesday,
				    recur_wednesday=EXCLUDED.recur_wednesday,
				    recur_thursday=EXCLUDED.recur_thursday,
				    recur_friday=EXCLUDED.recur_friday,
				    recur_saturday=EXCLUDED.recur_saturday,
				    last_updated_by_account_id=EXCLUDED.last_updated_by_account_id
				""", logicalAvailabilityId, providerId, accountId, accountId);
		database.execute("""
				INSERT INTO logical_availability_appointment_type (
					logical_availability_id,
					appointment_type_id
				)
				SELECT ?, provider_appointment_type.appointment_type_id
				FROM provider_appointment_type
				JOIN appointment_type
					ON appointment_type.appointment_type_id=provider_appointment_type.appointment_type_id
					AND COALESCE(appointment_type.deleted, FALSE)=FALSE
				WHERE provider_appointment_type.provider_id=?
				ON CONFLICT (logical_availability_id, appointment_type_id) DO NOTHING
				""", logicalAvailabilityId, providerId);
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
