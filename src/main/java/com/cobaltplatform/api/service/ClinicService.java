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

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.model.db.Clinic;
import com.cobaltplatform.api.model.db.ClinicLocation;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionLocation;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlInListPlaceholders;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class ClinicService {
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public ClinicService(@Nonnull DatabaseProvider databaseProvider,
											 @Nonnull Configuration configuration,
											 @Nonnull Strings strings) {
		requireNonNull(databaseProvider);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.databaseProvider = databaseProvider;
		this.configuration = configuration;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<Clinic> findClinicById(@Nullable UUID clinicId) {
		if (clinicId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM clinic WHERE clinic_id=?", Clinic.class, clinicId);
	}

	@Nonnull
	public List<Clinic> findClinicsByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM clinic WHERE institution_id=? ORDER BY description", Clinic.class, institutionId);
	}

	@Nonnull
	public List<Clinic> findClinicsByProviderId(@Nullable UUID providerId) {
		if (providerId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT c.* FROM provider_clinic pc, clinic c " +
				"WHERE pc.provider_id=? AND c.clinic_id=pc.clinic_id " +
				"ORDER BY pc.primary_clinic, c.description, c.treatment_description", Clinic.class, providerId);
	}

	@Nonnull
	public Map<UUID, List<Clinic>> findClinicsByProviderIds(@Nullable Set<UUID> providerIds) {
		if (providerIds == null || providerIds.size() == 0)
			return Collections.emptyMap();

		List<Object> parameters = new ArrayList<>(providerIds.size());
		parameters.addAll(providerIds);

		List<ClinicWithProviderId> clinics = getDatabase().queryForList(format("""
				SELECT c.*, pc.provider_id
				FROM provider_clinic pc, clinic c
				WHERE pc.provider_id IN %s
				AND c.clinic_id=pc.clinic_id
				ORDER BY pc.provider_id, pc.primary_clinic, c.description, c.treatment_description
				""", sqlInListPlaceholders(providerIds)), ClinicWithProviderId.class, parameters.toArray(new Object[]{}));

		Map<UUID, List<Clinic>> clinicsByProviderId = new HashMap<>();

		for (ClinicWithProviderId clinic : clinics) {
			List<Clinic> providerClinics = clinicsByProviderId.get(clinic.getProviderId());

			if (providerClinics == null) {
				providerClinics = new ArrayList<>();
				clinicsByProviderId.put(clinic.getProviderId(), providerClinics);
			}

			providerClinics.add(clinic);
		}

		return clinicsByProviderId;
	}

	@Nonnull
	public List<ClinicLocation> findClinicLocationsByClinicId(@Nullable UUID clinicId) {
		if (clinicId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT *
				FROM clinic_location
				WHERE clinic_id=?
				ORDER BY display_order, name, clinic_location_id
				""", ClinicLocation.class, clinicId);
	}

	@Nonnull
	public Map<UUID, List<ClinicLocation>> findClinicLocationsByClinicIds(@Nullable Set<UUID> clinicIds) {
		if (clinicIds == null || clinicIds.isEmpty())
			return Collections.emptyMap();

		List<ClinicLocation> clinicLocations = getDatabase().queryForList("""
				SELECT *
				FROM clinic_location
				WHERE clinic_id = ANY (CAST(? AS UUID[]))
				ORDER BY clinic_id, display_order, name, clinic_location_id
				""", ClinicLocation.class, (Object) clinicIds.toArray(new UUID[0]));

		Map<UUID, List<ClinicLocation>> clinicLocationsByClinicId = new HashMap<>();

		for (ClinicLocation clinicLocation : clinicLocations) {
			if (clinicLocation.getClinicId() == null)
				continue;

			List<ClinicLocation> clinicLocationsForClinic =
					clinicLocationsByClinicId.computeIfAbsent(clinicLocation.getClinicId(), ignored -> new ArrayList<>());
			clinicLocationsForClinic.add(clinicLocation);
		}

		return clinicLocationsByClinicId;
	}

	@Nonnull
	public List<InstitutionLocation> findInstitutionLocationsByClinicId(@Nullable UUID clinicId) {
		if (clinicId == null)
			return Collections.emptyList();

		List<InstitutionLocation> institutionLocations = getDatabase().queryForList("""
				SELECT il.*
				FROM provider_clinic pc, provider p, provider_institution_location pil, institution_location il
				WHERE pc.clinic_id=?
				AND p.provider_id=pc.provider_id
				AND p.active=TRUE
				AND pil.provider_id=p.provider_id
				AND il.institution_location_id=pil.institution_location_id
				ORDER BY il.display_order, il.name, il.institution_location_id
				""", InstitutionLocation.class, clinicId);

		return deduplicatedInstitutionLocations(institutionLocations);
	}

	@Nonnull
	public Map<UUID, List<InstitutionLocation>> findInstitutionLocationsByClinicIds(@Nullable Set<UUID> clinicIds) {
		if (clinicIds == null || clinicIds.isEmpty())
			return Collections.emptyMap();

		List<InstitutionLocationWithClinicId> institutionLocations = getDatabase().queryForList("""
				SELECT il.*, pc.clinic_id
				FROM provider_clinic pc, provider p, provider_institution_location pil, institution_location il
				WHERE pc.clinic_id = ANY (CAST(? AS UUID[]))
				AND p.provider_id=pc.provider_id
				AND p.active=TRUE
				AND pil.provider_id=p.provider_id
				AND il.institution_location_id=pil.institution_location_id
				ORDER BY pc.clinic_id, il.display_order, il.name, il.institution_location_id
				""", InstitutionLocationWithClinicId.class, (Object) clinicIds.toArray(new UUID[0]));

		Map<UUID, List<InstitutionLocation>> institutionLocationsByClinicId = new HashMap<>();
		Map<UUID, Set<UUID>> institutionLocationIdsByClinicId = new HashMap<>();

		for (InstitutionLocationWithClinicId institutionLocation : institutionLocations) {
			if (institutionLocation.getClinicId() == null || institutionLocation.getInstitutionLocationId() == null)
				continue;

			Set<UUID> institutionLocationIdsForClinic =
					institutionLocationIdsByClinicId.computeIfAbsent(institutionLocation.getClinicId(), ignored -> new HashSet<>());

			if (!institutionLocationIdsForClinic.add(institutionLocation.getInstitutionLocationId()))
				continue;

			List<InstitutionLocation> institutionLocationsForClinic =
					institutionLocationsByClinicId.computeIfAbsent(institutionLocation.getClinicId(), ignored -> new ArrayList<>());
			institutionLocationsForClinic.add(institutionLocation);
		}

		return institutionLocationsByClinicId;
	}

	@Nonnull
	protected List<InstitutionLocation> deduplicatedInstitutionLocations(@Nonnull List<InstitutionLocation> institutionLocations) {
		requireNonNull(institutionLocations);

		List<InstitutionLocation> deduplicatedInstitutionLocations = new ArrayList<>(institutionLocations.size());
		Set<UUID> institutionLocationIds = new HashSet<>();

		for (InstitutionLocation institutionLocation : institutionLocations) {
			if (institutionLocation.getInstitutionLocationId() == null)
				continue;

			if (!institutionLocationIds.add(institutionLocation.getInstitutionLocationId()))
				continue;

			deduplicatedInstitutionLocations.add(institutionLocation);
		}

		return deduplicatedInstitutionLocations;
	}

	@Nonnull
	public List<Clinic> findClinicsForAutocomplete(@Nullable String query,
																								 @Nullable InstitutionId institutionId) {
		query = trimToNull(query);

		if (institutionId == null)
			return Collections.emptyList();

		if (query == null)
			return findClinicsByInstitutionId(institutionId);

		return getDatabase().queryForList("SELECT * FROM clinic WHERE institution_id=? " +
				"AND UPPER(description) LIKE UPPER(?) ORDER BY description", Clinic.class, institutionId, "%" + query + "%");
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}

	protected static class ClinicWithProviderId extends Clinic {
		@Nullable
		private UUID providerId;

		@Nullable
		public UUID getProviderId() {
			return this.providerId;
		}

		public void setProviderId(@Nullable UUID providerId) {
			this.providerId = providerId;
		}
	}

	protected static class InstitutionLocationWithClinicId extends InstitutionLocation {
		@Nullable
		private UUID clinicId;

		@Nullable
		public UUID getClinicId() {
			return this.clinicId;
		}

		public void setClinicId(@Nullable UUID clinicId) {
			this.clinicId = clinicId;
		}
	}
}
