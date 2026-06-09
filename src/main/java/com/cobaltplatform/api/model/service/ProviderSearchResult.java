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

package com.cobaltplatform.api.model.service;

import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.Clinic;
import com.cobaltplatform.api.model.db.Provider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ProviderSearchResult {
	@Nonnull
	private final ProviderSearchResultTypeId providerSearchResultTypeId;
	@Nonnull
	private final UUID providerSearchResultId;
	@Nullable
	private final String name;
	@Nullable
	private final Provider provider;
	@Nullable
	private final ProviderFind providerFind;
	@Nullable
	private final Clinic clinic;
	@Nonnull
	private final List<ProviderFind> providerFinds;
	@Nonnull
	private final Map<UUID, Provider> providersById;
	@Nonnull
	private final Map<UUID, AppointmentType> appointmentTypesById;
	@Nonnull
	private final List<ProviderSearchScreeningRequirement> screeningRequirements;

	public enum ProviderSearchResultTypeId {
		PROVIDER,
		CLINIC
	}

	@Nonnull
	public static ProviderSearchResult forProvider(@Nonnull Provider provider,
																									 @Nonnull ProviderFind providerFind,
																									 @Nonnull Map<UUID, AppointmentType> appointmentTypesById) {
		return forProvider(provider, providerFind, appointmentTypesById, List.of());
	}

	@Nonnull
	public static ProviderSearchResult forProvider(@Nonnull Provider provider,
																									 @Nonnull ProviderFind providerFind,
																									 @Nonnull Map<UUID, AppointmentType> appointmentTypesById,
																									 @Nonnull List<ProviderSearchScreeningRequirement> screeningRequirements) {
		requireNonNull(provider);
		requireNonNull(providerFind);
		requireNonNull(appointmentTypesById);
		requireNonNull(screeningRequirements);

		return new ProviderSearchResult(ProviderSearchResultTypeId.PROVIDER, requireNonNull(provider.getProviderId()),
				providerFind.getName(), provider, providerFind, null, List.of(providerFind), Map.of(provider.getProviderId(), provider),
				appointmentTypesById, List.copyOf(screeningRequirements));
	}

	@Nonnull
	public static ProviderSearchResult forClinic(@Nonnull Clinic clinic,
																								 @Nonnull List<ProviderFind> providerFinds,
																								 @Nonnull Map<UUID, Provider> providersById,
																								 @Nonnull Map<UUID, AppointmentType> appointmentTypesById) {
		return forClinic(clinic, providerFinds, providersById, appointmentTypesById, List.of());
	}

	@Nonnull
	public static ProviderSearchResult forClinic(@Nonnull Clinic clinic,
																								 @Nonnull List<ProviderFind> providerFinds,
																								 @Nonnull Map<UUID, Provider> providersById,
																								 @Nonnull Map<UUID, AppointmentType> appointmentTypesById,
																								 @Nonnull List<ProviderSearchScreeningRequirement> screeningRequirements) {
		requireNonNull(clinic);
		requireNonNull(providerFinds);
		requireNonNull(providersById);
		requireNonNull(appointmentTypesById);
		requireNonNull(screeningRequirements);

		return new ProviderSearchResult(ProviderSearchResultTypeId.CLINIC, requireNonNull(clinic.getClinicId()),
				clinic.getDescription(), null, null, clinic, List.copyOf(providerFinds), providersById, appointmentTypesById,
				List.copyOf(screeningRequirements));
	}

	protected ProviderSearchResult(@Nonnull ProviderSearchResultTypeId providerSearchResultTypeId,
																 @Nonnull UUID providerSearchResultId,
																 @Nullable String name,
																 @Nullable Provider provider,
																 @Nullable ProviderFind providerFind,
																	 @Nullable Clinic clinic,
																	 @Nonnull List<ProviderFind> providerFinds,
																	 @Nonnull Map<UUID, Provider> providersById,
																	 @Nonnull Map<UUID, AppointmentType> appointmentTypesById,
																	 @Nonnull List<ProviderSearchScreeningRequirement> screeningRequirements) {
		requireNonNull(providerSearchResultTypeId);
		requireNonNull(providerSearchResultId);
		requireNonNull(providerFinds);
		requireNonNull(providersById);
		requireNonNull(appointmentTypesById);
		requireNonNull(screeningRequirements);

		this.providerSearchResultTypeId = providerSearchResultTypeId;
		this.providerSearchResultId = providerSearchResultId;
		this.name = name;
		this.provider = provider;
		this.providerFind = providerFind;
		this.clinic = clinic;
		this.providerFinds = providerFinds;
		this.providersById = providersById;
		this.appointmentTypesById = appointmentTypesById;
		this.screeningRequirements = screeningRequirements;
	}

	@Nonnull
	public ProviderSearchResultTypeId getProviderSearchResultTypeId() {
		return providerSearchResultTypeId;
	}

	@Nonnull
	public UUID getProviderSearchResultId() {
		return providerSearchResultId;
	}

	@Nullable
	public String getName() {
		return name;
	}

	@Nullable
	public Provider getProvider() {
		return provider;
	}

	@Nullable
	public ProviderFind getProviderFind() {
		return providerFind;
	}

	@Nullable
	public Clinic getClinic() {
		return clinic;
	}

	@Nonnull
	public List<ProviderFind> getProviderFinds() {
		return providerFinds;
	}

	@Nonnull
	public Map<UUID, Provider> getProvidersById() {
		return providersById;
	}

	@Nonnull
	public Map<UUID, AppointmentType> getAppointmentTypesById() {
		return appointmentTypesById;
	}

	@Nonnull
	public List<ProviderSearchScreeningRequirement> getScreeningRequirements() {
		return this.screeningRequirements;
	}
}
