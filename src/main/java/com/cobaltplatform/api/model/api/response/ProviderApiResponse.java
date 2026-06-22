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

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.model.api.response.AvailabilityTimeApiResponse.AvailabilityTimeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentSelectionTypeId;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityApiResponse;
import com.cobaltplatform.api.model.api.response.ProviderSearchResultApiResponse.AvailableAppointment;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.api.response.SupportRoleApiResponse.SupportRoleApiResponseFactory;
import com.cobaltplatform.api.model.db.Address;
import com.cobaltplatform.api.model.db.Clinic;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionLocation;
import com.cobaltplatform.api.model.db.PaymentFunding;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.SupportRole;
import com.cobaltplatform.api.model.service.AppointmentBookingScreeningKey;
import com.cobaltplatform.api.model.service.AvailabilityTime;
import com.cobaltplatform.api.model.service.ProviderFind;
import com.cobaltplatform.api.model.service.ProviderSearchScreeningRequirement;
import com.cobaltplatform.api.service.ClinicService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.ProviderService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.JsonMapper;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ProviderApiResponse {
	@Nonnull
	private final UUID providerId;
	@Nonnull
	private final InstitutionId institutionId;
	@Nonnull
	private final String urlName;
	@Nonnull
	private final String name;
	@Nonnull
	private final String emailAddress;
	@Nullable
	private final String title;
	@Nullable
	private final String entity;
	@Nullable
	private final String clinic;
	@Nullable
	private final String license;
	@Nullable
	private final String specialty;
	@Nullable
	private final String description;
	@Nullable
	private final String treatmentDescription;
	@Nullable
	private final String detailsHtml;
	@Nonnull
	private final String imageUrl;
	@Nonnull
	private final Boolean isDefaultImageUrl;
	@Nonnull
	private final ZoneId timeZone;
	@Nonnull
	private final Locale locale;
	@Nonnull
	private final List<String> tags; // e.g. ["Experienced Coach", "Calming Voice"]
	@Nullable
	private final List<AvailabilityTimeApiResponse> availabilityTimes;
	@Nullable
	private final List<SupportRoleApiResponse> supportRoles;
	@Nullable
	private final String supportRolesDescription;
	@Nullable
	@Deprecated // This is no longer used and always set to false
	private final Boolean phoneNumberRequiredForAppointment;
	@Nullable
	private final List<String> paymentFundingDescriptions;
	@Nullable
	private final String bioUrl;
	@Nullable
	private final String websiteUrl;
	@Nullable
	private final String bio;
	@Nullable
	private final String phoneNumber;
	@Nullable
	private final String phoneNumberDescription;
	@Nullable
	private final String formattedPhoneNumber;
	@Nullable
	private final Boolean displayPhoneNumberOnlyForBooking;
	@Nullable
	private final List<ProviderAppointmentModalityApiResponse> supportedAppointmentModalities;
	@Nullable
	private final List<InstitutionLocationApiResponse> locations;
	@Nullable
	private final ProviderAppointmentSelectionTypeId appointmentSelectionTypeId;
	@Nullable
	private final ProviderSearchScreeningRequirement screeningRequirement;

	public static class ProviderApiResponseBatchContext {
		@Nonnull
		private final Map<UUID, List<InstitutionLocation>> institutionLocationsByProviderId;
		@Nonnull
		private final Map<UUID, Address> addressesByAddressId;
		private final boolean institutionLocationsPreloaded;
		private final boolean addressesPreloaded;

		@Nonnull
		public static ProviderApiResponseBatchContext empty() {
			return new ProviderApiResponseBatchContext(Map.of(), Map.of(), false, false);
		}

		public ProviderApiResponseBatchContext(@Nonnull Map<UUID, List<InstitutionLocation>> institutionLocationsByProviderId,
																					 @Nonnull Map<UUID, Address> addressesByAddressId,
																					 boolean institutionLocationsPreloaded,
																					 boolean addressesPreloaded) {
			requireNonNull(institutionLocationsByProviderId);
			requireNonNull(addressesByAddressId);

			this.institutionLocationsByProviderId = new HashMap<>();
			this.addressesByAddressId = new HashMap<>(addressesByAddressId);
			this.institutionLocationsPreloaded = institutionLocationsPreloaded;
			this.addressesPreloaded = addressesPreloaded;

			for (Map.Entry<UUID, List<InstitutionLocation>> entry : institutionLocationsByProviderId.entrySet())
				this.institutionLocationsByProviderId.put(entry.getKey(), List.copyOf(entry.getValue()));
		}

		@Nonnull
		public List<InstitutionLocation> getInstitutionLocationsByProviderId(@Nullable UUID providerId) {
			if (providerId == null)
				return List.of();

			return this.institutionLocationsByProviderId.getOrDefault(providerId, List.of());
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

	public enum ProviderApiResponseSupplement {
		EVERYTHING,
		SUPPORT_ROLES,
		PAYMENT_FUNDING,
		DETAILS_HTML
	}

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ProviderApiResponseFactory {
		@Nonnull
		ProviderApiResponse create(@Nonnull Provider provider,
															 @Nullable ProviderApiResponseSupplement... supplements);

		@Nonnull
		ProviderApiResponse create(@Nonnull Provider provider,
															 @Nonnull ProviderApiResponseBatchContext batchContext,
															 @Nullable ProviderApiResponseSupplement... supplements);

		@Nonnull
		ProviderApiResponse create(@Nonnull Provider provider,
															 @Nonnull ProviderApiResponseBatchContext batchContext,
															 @Assisted("providerFind") @Nonnull ProviderFind providerFind,
															 @Assisted("appointmentTypesById") @Nonnull Map<UUID, AppointmentType> appointmentTypesById,
															 @Assisted("completedAppointmentBookingScreeningKeys") @Nonnull Set<AppointmentBookingScreeningKey> completedAppointmentBookingScreeningKeys,
															 @Nullable ProviderApiResponseSupplement... supplements);

		@Nonnull
		ProviderApiResponse create(@Nonnull Provider provider,
															 @Nullable List<AvailabilityTime> availabilityTimes,
															 @Nullable ProviderApiResponseSupplement... supplements);
	}

	@AssistedInject
	public ProviderApiResponse(@Nonnull ProviderService providerService,
														 @Nonnull ClinicService clinicService,
														 @Nonnull Formatter formatter,
														 @Nonnull Strings strings,
														 @Nonnull JsonMapper jsonMapper,
														 @Nonnull AvailabilityTimeApiResponseFactory availabilityTimeApiResponseFactory,
														 @Nonnull SupportRoleApiResponseFactory supportRoleApiResponseFactory,
														 @Nonnull InstitutionService institutionService,
														 @Nonnull Configuration configuration,
														 @Assisted @Nonnull Provider provider,
														 @Assisted @Nullable ProviderApiResponseSupplement... supplements) {
		this(providerService, clinicService, formatter, strings, jsonMapper, availabilityTimeApiResponseFactory, supportRoleApiResponseFactory,
				institutionService, configuration, provider, null, ProviderApiResponseBatchContext.empty(), true, supplements);
	}

	@AssistedInject
	public ProviderApiResponse(@Nonnull ProviderService providerService,
														 @Nonnull ClinicService clinicService,
														 @Nonnull Formatter formatter,
														 @Nonnull Strings strings,
														 @Nonnull JsonMapper jsonMapper,
														 @Nonnull AvailabilityTimeApiResponseFactory availabilityTimeApiResponseFactory,
														 @Nonnull SupportRoleApiResponseFactory supportRoleApiResponseFactory,
														 @Nonnull InstitutionService institutionService,
														 @Nonnull Configuration configuration,
														 @Assisted @Nonnull Provider provider,
														 @Assisted @Nullable List<AvailabilityTime> availabilityTimes,
														 @Assisted @Nullable ProviderApiResponseSupplement... supplements) {
		this(providerService, clinicService, formatter, strings, jsonMapper, availabilityTimeApiResponseFactory, supportRoleApiResponseFactory,
				institutionService, configuration, provider, availabilityTimes, ProviderApiResponseBatchContext.empty(), true, supplements);
	}

	@AssistedInject
	public ProviderApiResponse(@Nonnull ProviderService providerService,
														 @Nonnull ClinicService clinicService,
														 @Nonnull Formatter formatter,
														 @Nonnull Strings strings,
														 @Nonnull JsonMapper jsonMapper,
														 @Nonnull AvailabilityTimeApiResponseFactory availabilityTimeApiResponseFactory,
														 @Nonnull SupportRoleApiResponseFactory supportRoleApiResponseFactory,
														 @Nonnull InstitutionService institutionService,
														 @Nonnull Configuration configuration,
														 @Assisted @Nonnull Provider provider,
														 @Assisted @Nonnull ProviderApiResponseBatchContext batchContext,
														 @Assisted @Nullable ProviderApiResponseSupplement... supplements) {
		this(providerService, clinicService, formatter, strings, jsonMapper, availabilityTimeApiResponseFactory, supportRoleApiResponseFactory,
				institutionService, configuration, provider, null, batchContext, true, supplements);
	}

	@AssistedInject
	public ProviderApiResponse(@Nonnull ProviderService providerService,
														 @Nonnull ClinicService clinicService,
														 @Nonnull Formatter formatter,
														 @Nonnull Strings strings,
														 @Nonnull JsonMapper jsonMapper,
														 @Nonnull AvailabilityTimeApiResponseFactory availabilityTimeApiResponseFactory,
														 @Nonnull SupportRoleApiResponseFactory supportRoleApiResponseFactory,
														 @Nonnull InstitutionService institutionService,
														 @Nonnull Configuration configuration,
														 @Assisted @Nonnull Provider provider,
														 @Assisted @Nonnull ProviderApiResponseBatchContext batchContext,
														 @Assisted("providerFind") @Nonnull ProviderFind providerFind,
														 @Assisted("appointmentTypesById") @Nonnull Map<UUID, AppointmentType> appointmentTypesById,
														 @Assisted("completedAppointmentBookingScreeningKeys") @Nonnull Set<AppointmentBookingScreeningKey> completedAppointmentBookingScreeningKeys,
														 @Assisted @Nullable ProviderApiResponseSupplement... supplements) {
		this(providerService, clinicService, formatter, strings, jsonMapper, availabilityTimeApiResponseFactory, supportRoleApiResponseFactory,
				institutionService, configuration, provider, null, batchContext, true, providerFind, appointmentTypesById,
				completedAppointmentBookingScreeningKeys, supplements);
	}

	protected ProviderApiResponse(@Nonnull ProviderService providerService,
																@Nonnull ClinicService clinicService,
																@Nonnull Formatter formatter,
																@Nonnull Strings strings,
																@Nonnull JsonMapper jsonMapper,
																@Nonnull AvailabilityTimeApiResponseFactory availabilityTimeApiResponseFactory,
																@Nonnull SupportRoleApiResponseFactory supportRoleApiResponseFactory,
																@Nonnull InstitutionService institutionService,
																@Nonnull Configuration configuration,
																@Nonnull Provider provider,
																@Nullable List<AvailabilityTime> availabilityTimes,
																@Nonnull ProviderApiResponseBatchContext batchContext,
																boolean includeWebsiteAndLocations,
																@Nullable ProviderApiResponseSupplement... supplements) {
		this(providerService, clinicService, formatter, strings, jsonMapper, availabilityTimeApiResponseFactory, supportRoleApiResponseFactory,
				institutionService, configuration, provider, availabilityTimes, batchContext, includeWebsiteAndLocations, null, Map.of(),
				Set.of(), supplements);
	}

	protected ProviderApiResponse(@Nonnull ProviderService providerService,
																@Nonnull ClinicService clinicService,
																@Nonnull Formatter formatter,
																@Nonnull Strings strings,
																@Nonnull JsonMapper jsonMapper,
																@Nonnull AvailabilityTimeApiResponseFactory availabilityTimeApiResponseFactory,
																@Nonnull SupportRoleApiResponseFactory supportRoleApiResponseFactory,
																@Nonnull InstitutionService institutionService,
																@Nonnull Configuration configuration,
																@Nonnull Provider provider,
																@Nullable List<AvailabilityTime> availabilityTimes,
																@Nonnull ProviderApiResponseBatchContext batchContext,
																boolean includeWebsiteAndLocations,
																@Nullable ProviderFind providerFind,
																@Nonnull Map<UUID, AppointmentType> appointmentTypesById,
																@Nonnull Set<AppointmentBookingScreeningKey> completedAppointmentBookingScreeningKeys,
																@Nullable ProviderApiResponseSupplement... supplements) {
		requireNonNull(providerService);
		requireNonNull(clinicService);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(jsonMapper);
		requireNonNull(availabilityTimeApiResponseFactory);
		requireNonNull(supportRoleApiResponseFactory);
		requireNonNull(institutionService);
		requireNonNull(provider);
		requireNonNull(configuration);
		requireNonNull(batchContext);
		requireNonNull(appointmentTypesById);
		requireNonNull(completedAppointmentBookingScreeningKeys);

		List<ProviderApiResponseSupplement> supplementsList = Arrays.asList(supplements);
		boolean includeEverything = supplementsList.contains(ProviderApiResponseSupplement.EVERYTHING);
		boolean bookingV2Enabled = institutionService.isBookingV2Enabled(provider.getInstitutionId());
		String bioUrl = trimToNull(provider.getBioUrl());

		this.providerId = provider.getProviderId();
		this.institutionId = provider.getInstitutionId();
		this.emailAddress = provider.getEmailAddress();
		this.urlName = provider.getUrlName();
		this.name = provider.getName();
		this.title = provider.getTitle();
		this.clinic = provider.getClinic();
		this.specialty = provider.getSpecialty();
		this.license = provider.getLicense();
		this.entity = provider.getEntity();
		this.description = bookingV2Enabled ? provider.getDescription() : null;
		this.treatmentDescription = bookingV2Enabled && includeEverything ? treatmentDescriptionFor(clinicService.findClinicsByProviderId(provider.getProviderId())) : null;
		this.detailsHtml = supplementsList.contains(ProviderApiResponseSupplement.DETAILS_HTML) ? provider.getDetailsHtml() : null;
		this.imageUrl = provider.getImageUrl();
		this.isDefaultImageUrl = provider.getImageUrl() == null;
		this.timeZone = provider.getTimeZone();
		this.locale = provider.getLocale();
		this.tags = provider.getTags() == null ? Collections.emptyList() : jsonMapper.toList(provider.getTags(), String.class);
		this.bioUrl = bioUrl;
		this.websiteUrl = includeWebsiteAndLocations ? bioUrl : null;
		this.phoneNumber = provider.getPhoneNumber();
		this.displayPhoneNumberOnlyForBooking = provider.getDisplayPhoneNumberOnlyForBooking();
		this.phoneNumberDescription = bookingV2Enabled ? formatter.formatPhoneNumber(provider.getPhoneNumber(), provider.getLocale()) : null;
		this.formattedPhoneNumber = formatter.formatPhoneNumber(provider.getPhoneNumber(), provider.getLocale());
		this.supportedAppointmentModalities = bookingV2Enabled ? ProviderAppointmentModalitySupport.providerAppointmentModalityApiResponsesFor(provider, strings) : null;
		this.locations = includeWebsiteAndLocations ? institutionLocationApiResponsesFor(provider, providerService, formatter, batchContext) : null;
		if (providerFind == null) {
			this.appointmentSelectionTypeId = null;
			this.screeningRequirement = null;
		} else {
			Map<UUID, Provider> providersById = Map.of(provider.getProviderId(), provider);
			List<AvailableAppointment> availableAppointments = ProviderSearchResultApiResponse.availableAppointmentsFor(List.of(providerFind),
					providersById, appointmentTypesById);
			AvailableAppointment firstAvailableAppointment = availableAppointments.size() == 0 ? null : availableAppointments.get(0);

			this.appointmentSelectionTypeId = ProviderSearchResultApiResponse.appointmentSelectionTypeIdFor(List.of(providerFind),
					providersById, availableAppointments, appointmentTypesById, completedAppointmentBookingScreeningKeys);
			this.screeningRequirement = ProviderSearchResultApiResponse.screeningRequirementFor(firstAvailableAppointment,
					appointmentTypesById, completedAppointmentBookingScreeningKeys);
		}

		String bio = trimToNull(provider.getBio());

		if (bio != null) {
			// HTML-ify line breaks if we do have a bio
			bio = bio.replace("\n", "<br/>");
		} else if (bioUrl != null) {
			// Make a synthetic bio if we have a URL but no real bio
			bio = format(strings.get("<a target='_blank' href='{{bioUrl}}'>Click here to read more about {{providerName}}</a>", new HashMap<String, Object>() {{
				put("bioUrl", bioUrl);
				put("providerName", getName());
			}}));
		}

		this.bio = bio;

		if (availabilityTimes == null)
			this.availabilityTimes = null;
		else
			this.availabilityTimes = availabilityTimes.stream()
					.map((availabilityTime -> availabilityTimeApiResponseFactory.create(availabilityTime)))
					.collect(Collectors.toList());

		if (includeEverything || supplementsList.contains(ProviderApiResponseSupplement.SUPPORT_ROLES)) {
			this.supportRoles = providerService.findSupportRolesByProviderId(provider.getProviderId()).stream()
					.map((supportRole) -> supportRoleApiResponseFactory.create(supportRole))
					.collect(Collectors.toList());
			this.supportRolesDescription = this.supportRoles.size() == 0
					? null
					: this.supportRoles.stream()
					.map(supportRole -> supportRole.getDescription())
					.collect(Collectors.joining(", "));
			this.phoneNumberRequiredForAppointment = false;
		} else {
			this.supportRoles = null;
			this.supportRolesDescription = null;
			this.phoneNumberRequiredForAppointment = false;
		}

		if (includeEverything || supplementsList.contains(ProviderApiResponseSupplement.PAYMENT_FUNDING)) {
			// Can be done more optimally later...
			List<PaymentFunding> paymentFundings = providerService.findPaymentFundings();
			Map<PaymentFunding.PaymentFundingId, String> paymentFundingDescriptionsById = new HashMap<>(paymentFundings.size());

			for (PaymentFunding paymentFunding : paymentFundings)
				paymentFundingDescriptionsById.put(paymentFunding.getPaymentFundingId(), paymentFunding.getDescription());

			List<PaymentFunding> providerPaymentFundings = providerService.findPaymentFundingsByProviderId(provider.getProviderId());

			this.paymentFundingDescriptions = providerPaymentFundings.stream()
					.map(paymentFunding -> paymentFundingDescriptionsById.get(paymentFunding.getPaymentFundingId()))
					.collect(Collectors.toList());
		} else {
			this.paymentFundingDescriptions = null;
		}
	}

	@Nonnull
	protected List<InstitutionLocationApiResponse> institutionLocationApiResponsesFor(@Nonnull Provider provider,
																																									 @Nonnull ProviderService providerService,
																																									 @Nonnull Formatter formatter,
																																									 @Nonnull ProviderApiResponseBatchContext batchContext) {
		requireNonNull(provider);
		requireNonNull(providerService);
		requireNonNull(formatter);
		requireNonNull(batchContext);

		List<InstitutionLocation> institutionLocations = batchContext.isInstitutionLocationsPreloaded()
				? batchContext.getInstitutionLocationsByProviderId(provider.getProviderId())
				: providerService.findInstitutionLocationsByProviderId(provider.getProviderId());

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

	@Nullable
	protected String treatmentDescriptionFor(@Nonnull List<Clinic> clinics) {
		requireNonNull(clinics);

		List<String> treatmentDescriptions = clinics.stream()
				.map(Clinic::getTreatmentDescription)
				.map(treatmentDescription -> trimToNull(treatmentDescription))
				.filter(treatmentDescription -> treatmentDescription != null)
				.collect(Collectors.toList());

		return treatmentDescriptions.size() == 0
				? null
				: format("* %s", treatmentDescriptions.stream().collect(Collectors.joining(", ")));
	}

	@Nonnull
	public UUID getProviderId() {
		return providerId;
	}

	@Nonnull
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	@Nonnull
	public String getUrlName() {
		return this.urlName;
	}

	@Nonnull
	public String getEmailAddress() {
		return emailAddress;
	}

	@Nonnull
	public String getName() {
		return name;
	}

	@Nullable
	public String getTitle() {
		return title;
	}

	@Nullable
	public String getEntity() {
		return entity;
	}

	@Nullable
	public String getClinic() {
		return clinic;
	}

	@Nullable
	public String getLicense() {
		return license;
	}

	@Nullable
	public String getSpecialty() {
		return specialty;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	@Nullable
	public String getTreatmentDescription() {
		return this.treatmentDescription;
	}

	@Nullable
	public String getDetailsHtml() {
		return this.detailsHtml;
	}

	@Nullable
	public List<SupportRoleApiResponse> getSupportRoles() {
		return supportRoles;
	}

	@Nullable
	public String getSupportRolesDescription() {
		return supportRolesDescription;
	}

	@Nonnull
	public String getImageUrl() {
		return imageUrl;
	}

	@Nonnull
	public Boolean getDefaultImageUrl() {
		return isDefaultImageUrl;
	}

	@Nonnull
	public ZoneId getTimeZone() {
		return timeZone;
	}

	@Nonnull
	public Locale getLocale() {
		return locale;
	}

	@Nonnull
	public List<String> getTags() {
		return tags;
	}

	@Nullable
	public List<AvailabilityTimeApiResponse> getAvailabilityTimes() {
		return availabilityTimes;
	}

	@Nullable
	@Deprecated // this is no longer used and always false
	public Boolean getPhoneNumberRequiredForAppointment() {
		return phoneNumberRequiredForAppointment;
	}

	@Nullable
	public List<String> getPaymentFundingDescriptions() {
		return paymentFundingDescriptions;
	}

	@Nullable
	public String getBioUrl() {
		return bioUrl;
	}

	@Nullable
	public String getWebsiteUrl() {
		return this.websiteUrl;
	}

	@Nullable
	public String getBio() {
		return bio;
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
		return this.formattedPhoneNumber;
	}

	@Nullable
	public Boolean getDisplayPhoneNumberOnlyForBooking() {
		return this.displayPhoneNumberOnlyForBooking;
	}

	@Nullable
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
		return this.locations == null ? List.of() : this.locations;
	}
}
