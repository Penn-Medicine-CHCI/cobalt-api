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

import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityApiResponse;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityId;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentSelectionTypeId;
import com.cobaltplatform.api.model.api.response.ProviderSearchResultApiResponse.AvailableAppointment;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.Address;
import com.cobaltplatform.api.model.db.AppointmentBookingLevel.AppointmentBookingLevelId;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.Clinic;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionLocation;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.service.AppointmentBookingScreeningKey;
import com.cobaltplatform.api.model.service.ProviderFind;
import com.cobaltplatform.api.model.service.ProviderSearchScreeningRequirement;
import com.cobaltplatform.api.service.ClinicService;
import com.cobaltplatform.api.service.ProviderService;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ClinicApiResponse {
	@Nonnull
	private final UUID clinicId;
	@Nonnull
	private final InstitutionId institutionId;
	@Nullable
	private final UUID intakeAssessmentId;
	@Nonnull
	private final String name;
	@Nonnull
	private final String description;
	@Nullable
	private final String treatmentDescription;
	@Nullable
	private final String detailsHtml;
	@Nullable
	private final Boolean showIntakeAssessmentPrompt;
	@Nullable
	private final AppointmentBookingLevelId appointmentBookingLevelId;
	@Nullable
	private final String phoneNumber;
	@Nullable
	private final String phoneNumberDescription;
	@Nullable
	private final String formattedPhoneNumber;
	@Nullable
	private final String imageUrl;
	@Nullable
	private final String websiteUrl;
	@Nonnull
	private final List<ProviderAppointmentModalityApiResponse> supportedAppointmentModalities;
	@Nonnull
	private final List<InstitutionLocationApiResponse> locations;
	@Nullable
	private final ProviderAppointmentSelectionTypeId appointmentSelectionTypeId;
	@Nullable
	private final ProviderSearchScreeningRequirement screeningRequirement;

	public enum ClinicApiResponseSupplement {
		DETAILS_HTML
	}

	public static class ClinicApiResponseBatchContext {
		@Nonnull
		private final Map<UUID, List<InstitutionLocation>> institutionLocationsByClinicId;
		@Nonnull
		private final Map<UUID, Address> addressesByAddressId;
		private final boolean institutionLocationsPreloaded;
		private final boolean addressesPreloaded;

		@Nonnull
		public static ClinicApiResponseBatchContext empty() {
			return new ClinicApiResponseBatchContext(Map.of(), Map.of(), false, false);
		}

		public ClinicApiResponseBatchContext(@Nonnull Map<UUID, List<InstitutionLocation>> institutionLocationsByClinicId,
																				 @Nonnull Map<UUID, Address> addressesByAddressId,
																				 boolean institutionLocationsPreloaded,
																				 boolean addressesPreloaded) {
			requireNonNull(institutionLocationsByClinicId);
			requireNonNull(addressesByAddressId);

			this.institutionLocationsByClinicId = new HashMap<>();
			this.addressesByAddressId = new HashMap<>(addressesByAddressId);
			this.institutionLocationsPreloaded = institutionLocationsPreloaded;
			this.addressesPreloaded = addressesPreloaded;

			for (Map.Entry<UUID, List<InstitutionLocation>> entry : institutionLocationsByClinicId.entrySet())
				this.institutionLocationsByClinicId.put(entry.getKey(), List.copyOf(entry.getValue()));
		}

		@Nonnull
		public List<InstitutionLocation> getInstitutionLocationsByClinicId(@Nullable UUID clinicId) {
			if (clinicId == null)
				return List.of();

			return this.institutionLocationsByClinicId.getOrDefault(clinicId, List.of());
		}

		@Nullable
		public Address getAddressByAddressId(@Nullable UUID addressId) {
			return addressId == null ? null : this.addressesByAddressId.get(addressId);
		}

		public boolean isInstitutionLocationsPreloaded() {
			return this.institutionLocationsPreloaded;
		}

		public boolean isAddressesPreloaded() {
			return this.addressesPreloaded;
		}
	}

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ClinicApiResponseFactory {
		@Nonnull
		ClinicApiResponse create(@Nonnull Clinic clinic,
															@Nullable ClinicApiResponseSupplement... supplements);

		@Nonnull
		ClinicApiResponse create(@Nonnull Clinic clinic,
															@Nonnull ClinicApiResponseBatchContext batchContext,
															@Nullable ClinicApiResponseSupplement... supplements);

		@Nonnull
		ClinicApiResponse create(@Nonnull Clinic clinic,
															@Nonnull ClinicApiResponseBatchContext batchContext,
															@Assisted("providerFinds") @Nonnull List<ProviderFind> providerFinds,
															@Assisted("providersById") @Nonnull Map<UUID, Provider> providersById,
															@Assisted("appointmentTypesById") @Nonnull Map<UUID, AppointmentType> appointmentTypesById,
															@Assisted("completedAppointmentBookingScreeningKeys") @Nonnull Set<AppointmentBookingScreeningKey> completedAppointmentBookingScreeningKeys,
															@Nullable ClinicApiResponseSupplement... supplements);
	}

	@AssistedInject
	public ClinicApiResponse(@Nonnull ProviderService providerService,
													 @Nonnull ClinicService clinicService,
													 @Nonnull Formatter formatter,
													 @Nonnull Strings strings,
													 @Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
													 @Assisted @Nonnull Clinic clinic,
													 @Assisted @Nullable ClinicApiResponseSupplement... supplements) {
		this(providerService, clinicService, formatter, strings, currentContextProvider, clinic, ClinicApiResponseBatchContext.empty(), supplements);
	}

	@AssistedInject
	public ClinicApiResponse(@Nonnull ProviderService providerService,
													 @Nonnull ClinicService clinicService,
													 @Nonnull Formatter formatter,
													 @Nonnull Strings strings,
													 @Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
													 @Assisted @Nonnull Clinic clinic,
													 @Assisted @Nonnull ClinicApiResponseBatchContext batchContext,
													 @Assisted @Nullable ClinicApiResponseSupplement... supplements) {
		this(providerService, clinicService, formatter, strings, currentContextProvider, clinic, batchContext, false, null,
				Map.of(), Map.of(), Set.of(), supplements);
	}

	@AssistedInject
	public ClinicApiResponse(@Nonnull ProviderService providerService,
													 @Nonnull ClinicService clinicService,
													 @Nonnull Formatter formatter,
													 @Nonnull Strings strings,
													 @Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
													 @Assisted @Nonnull Clinic clinic,
													 @Assisted @Nonnull ClinicApiResponseBatchContext batchContext,
													 @Assisted("providerFinds") @Nonnull List<ProviderFind> providerFinds,
													 @Assisted("providersById") @Nonnull Map<UUID, Provider> providersById,
													 @Assisted("appointmentTypesById") @Nonnull Map<UUID, AppointmentType> appointmentTypesById,
													 @Assisted("completedAppointmentBookingScreeningKeys") @Nonnull Set<AppointmentBookingScreeningKey> completedAppointmentBookingScreeningKeys,
													 @Assisted @Nullable ClinicApiResponseSupplement... supplements) {
		this(providerService, clinicService, formatter, strings, currentContextProvider, clinic, batchContext, true, providerFinds,
				providersById, appointmentTypesById, completedAppointmentBookingScreeningKeys, supplements);
	}

	protected ClinicApiResponse(@Nonnull ProviderService providerService,
															@Nonnull ClinicService clinicService,
															@Nonnull Formatter formatter,
															@Nonnull Strings strings,
															@Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
															@Nonnull Clinic clinic,
															@Nonnull ClinicApiResponseBatchContext batchContext,
															boolean includeBookingContext,
															@Nullable List<ProviderFind> providerFinds,
															@Nonnull Map<UUID, Provider> providersById,
															@Nonnull Map<UUID, AppointmentType> appointmentTypesById,
															@Nonnull Set<AppointmentBookingScreeningKey> completedAppointmentBookingScreeningKeys,
															@Nullable ClinicApiResponseSupplement... supplements) {
		requireNonNull(providerService);
		requireNonNull(clinicService);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);
		requireNonNull(clinic);
		requireNonNull(batchContext);
		requireNonNull(providersById);
		requireNonNull(appointmentTypesById);
		requireNonNull(completedAppointmentBookingScreeningKeys);

		List<ClinicApiResponseSupplement> supplementsList = Arrays.asList(supplements);

		this.clinicId = clinic.getClinicId();
		this.institutionId = clinic.getInstitutionId();
		this.intakeAssessmentId = clinic.getIntakeAssessmentId();
		this.name = clinic.getDescription();
		this.description = clinic.getDescription();
		this.treatmentDescription = clinic.getTreatmentDescription();
		this.detailsHtml = supplementsList.contains(ClinicApiResponseSupplement.DETAILS_HTML) ? clinic.getDetailsHtml() : null;
		this.showIntakeAssessmentPrompt = clinic.getShowIntakeAssessmentPrompt();
		this.appointmentBookingLevelId = clinic.getAppointmentBookingLevelId();
		this.phoneNumber = clinic.getPhoneNumber();
		this.phoneNumberDescription = formatter.formatPhoneNumber(clinic.getPhoneNumber(), clinic.getLocale());
		this.formattedPhoneNumber = this.phoneNumberDescription;
		this.imageUrl = clinic.getImageUrl();
		this.websiteUrl = clinic.getWebsiteUrl();
		this.supportedAppointmentModalities = supportedAppointmentModalitiesFor(providerService.findProvidersByClinicId(clinic.getClinicId()), strings);
		this.locations = institutionLocationApiResponsesFor(clinic, clinicService, providerService, formatter, batchContext);
		if (!includeBookingContext) {
			this.appointmentSelectionTypeId = null;
			this.screeningRequirement = null;
		} else {
			requireNonNull(providerFinds);

			List<AvailableAppointment> availableAppointments = ProviderSearchResultApiResponse.availableAppointmentsFor(providerFinds,
					providersById, appointmentTypesById);
			AvailableAppointment firstAvailableAppointment = availableAppointments.size() == 0 ? null : availableAppointments.get(0);

			this.appointmentSelectionTypeId = ProviderSearchResultApiResponse.appointmentSelectionTypeIdFor(providerFinds, providersById,
					availableAppointments, appointmentTypesById, completedAppointmentBookingScreeningKeys);
			this.screeningRequirement = ProviderSearchResultApiResponse.screeningRequirementFor(firstAvailableAppointment,
					appointmentTypesById, completedAppointmentBookingScreeningKeys);
		}
	}

	@Nonnull
	protected List<InstitutionLocationApiResponse> institutionLocationApiResponsesFor(@Nonnull Clinic clinic,
																																									 @Nonnull ClinicService clinicService,
																																									 @Nonnull ProviderService providerService,
																																									 @Nonnull Formatter formatter,
																																									 @Nonnull ClinicApiResponseBatchContext batchContext) {
		requireNonNull(clinic);
		requireNonNull(clinicService);
		requireNonNull(providerService);
		requireNonNull(formatter);
		requireNonNull(batchContext);

		List<InstitutionLocation> institutionLocations = batchContext.isInstitutionLocationsPreloaded()
				? batchContext.getInstitutionLocationsByClinicId(clinic.getClinicId())
				: clinicService.findInstitutionLocationsByClinicId(clinic.getClinicId());

		Map<UUID, Address> addressesByAddressId;

		if (batchContext.isAddressesPreloaded()) {
			addressesByAddressId = Map.of();
		} else {
			Set<UUID> addressIds = new HashSet<>();

			for (InstitutionLocation institutionLocation : institutionLocations)
				if (institutionLocation.getAddressId() != null)
					addressIds.add(institutionLocation.getAddressId());

			addressesByAddressId = providerService.findAddressesByIds(addressIds);
		}

		return institutionLocations.stream()
				.map(institutionLocation -> new InstitutionLocationApiResponse(institutionLocation,
						batchContext.isAddressesPreloaded()
								? batchContext.getAddressByAddressId(institutionLocation.getAddressId())
								: addressesByAddressId.get(institutionLocation.getAddressId()),
						formatter))
				.collect(Collectors.toList());
	}

	@Nonnull
	protected List<ProviderAppointmentModalityApiResponse> supportedAppointmentModalitiesFor(@Nonnull List<Provider> providers,
																																													 @Nonnull Strings strings) {
		requireNonNull(providers);
		requireNonNull(strings);

		Set<ProviderAppointmentModalityId> providerAppointmentModalityIds = new HashSet<>();

		for (Provider provider : providers)
			providerAppointmentModalityIds.addAll(ProviderAppointmentModalitySupport.providerAppointmentModalityIdsFor(provider));

		return ProviderAppointmentModalitySupport.providerAppointmentModalityApiResponsesFor(providerAppointmentModalityIds, strings);
	}

	@Nonnull
	public UUID getClinicId() {
		return clinicId;
	}

	@Nonnull
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	@Nullable
	public UUID getIntakeAssessmentId() {
		return intakeAssessmentId;
	}

	@Nonnull
	public String getName() {
		return this.name;
	}

	@Nonnull
	public String getDescription() {
		return description;
	}

	@Nullable
	public String getTreatmentDescription() {
		return treatmentDescription;
	}

	@Nullable
	public String getDetailsHtml() {
		return this.detailsHtml;
	}

	@Nullable
	public Boolean getShowIntakeAssessmentPrompt() {
		return showIntakeAssessmentPrompt;
	}

	@Nullable
	public AppointmentBookingLevelId getAppointmentBookingLevelId() {
		return this.appointmentBookingLevelId;
	}

	@Nullable
	public String getPhoneNumber() {
		return this.phoneNumber;
	}

	@Nullable
	public String getPhoneNumberDescription() {
		return this.phoneNumberDescription;
	}

	@Nullable
	public String getFormattedPhoneNumber() {
		return formattedPhoneNumber;
	}

	@Nullable
	public String getImageUrl() {
		return imageUrl;
	}

	@Nullable
	public String getWebsiteUrl() {
		return this.websiteUrl;
	}

	@Nonnull
	public List<ProviderAppointmentModalityApiResponse> getSupportedAppointmentModalities() {
		return this.supportedAppointmentModalities;
	}

	@Nullable
	public ProviderAppointmentSelectionTypeId getAppointmentSelectionTypeId() {
		return this.appointmentSelectionTypeId;
	}

	@Nullable
	public ProviderSearchScreeningRequirement getScreeningRequirement() {
		return this.screeningRequirement;
	}

	@Nonnull
	public List<InstitutionLocationApiResponse> getLocations() {
		return this.locations;
	}
}
