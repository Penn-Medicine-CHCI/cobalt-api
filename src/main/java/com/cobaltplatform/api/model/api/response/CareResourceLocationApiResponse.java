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


import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Address;
import com.cobaltplatform.api.model.db.CareResourceLocation;
import com.cobaltplatform.api.model.db.CareResourceTag;
import com.cobaltplatform.api.model.db.CareResourceTag.CareResourceTagGroupId;
import com.cobaltplatform.api.model.db.Role;
import com.cobaltplatform.api.service.AddressService;
import com.cobaltplatform.api.service.CareResourceService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.model.api.response.CareResourceTagApiResponse.CareResourceTagApiResponseFactory;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class CareResourceLocationApiResponse {
	@Nullable
	private UUID careResourceId;
	@Nullable
	private String googlePlaceId;
	@Nullable
	private String resourceName;
	@Nullable
	private String resourceNotes;
	@Nullable
	private String name;
	@Nullable
	private UUID careResourceLocationId;
	@Nullable
	private Address address;
	@Nullable
	private String phoneNumber;
	@Nullable
	private String websiteUrl;
	@Nullable
	private String emailAddress;
	@Nullable
	private String insuranceNotes;
	@Nullable
	private String formattedPhoneNumber;
	@Nullable
	private String notes;
	@Nullable
	private String internalNotes;
	@Nullable
	private Boolean wheelchairAccess;
	@Nullable
	private Boolean acceptingNewPatients;
	@Nullable
	private Boolean overridePayors;
	@Nullable
	private Boolean overrideSpecialties;
	@Nullable
	private List<CareResourceTagApiResponse> languages;
	@Nullable
	private List<CareResourceTagApiResponse> specialties;
	@Nullable
	private List<CareResourceTagApiResponse> payors;
	@Nullable
	private List<CareResourceTagApiResponse> therapyTypes;
	@Nullable
	private List<CareResourceTagApiResponse> populationServed;
	@Nullable
	private List<CareResourceTagApiResponse> genders;
	@Nullable
	private List<CareResourceTagApiResponse> ethnicities;
	@Nullable
	private List<CareResourceTagApiResponse> facilityTypes;
	@Nullable
	private Boolean appointmentTypeInPerson;
	@Nullable
	private Boolean appointmentTypeOnline;
	@Nullable
	private Double distanceInMiles;
	@Nullable
	private CareResourceTagApiResponseFactory careResourceTagApiResponseFactory;

	@Immutable
	public static class CareResourceLocationApiResponseBatchContext {
		@Nonnull
		private final Map<UUID, Address> addressesByAddressId;
		private final boolean addressesPreloaded;
		@Nonnull
		private final Map<UUID, Map<CareResourceTagGroupId, List<CareResourceTag>>> tagsByCareResourceLocationId;
		private final boolean careResourceLocationTagsPreloaded;
		@Nonnull
		private final Map<UUID, Map<CareResourceTagGroupId, List<CareResourceTag>>> tagsByCareResourceId;
		private final boolean careResourceTagsPreloaded;

		@Nonnull
		public static CareResourceLocationApiResponseBatchContext empty() {
			return new CareResourceLocationApiResponseBatchContext(Map.of(), false, Map.of(), false, Map.of(), false);
		}

		public CareResourceLocationApiResponseBatchContext(@Nonnull Map<UUID, Address> addressesByAddressId,
																											 boolean addressesPreloaded,
																											 @Nonnull Map<UUID, Map<CareResourceTagGroupId, List<CareResourceTag>>> tagsByCareResourceLocationId,
																											 boolean careResourceLocationTagsPreloaded,
																											 @Nonnull Map<UUID, Map<CareResourceTagGroupId, List<CareResourceTag>>> tagsByCareResourceId,
																											 boolean careResourceTagsPreloaded) {
			requireNonNull(addressesByAddressId);
			requireNonNull(tagsByCareResourceLocationId);
			requireNonNull(tagsByCareResourceId);

			this.addressesByAddressId = new LinkedHashMap<>(addressesByAddressId);
			this.addressesPreloaded = addressesPreloaded;
			this.tagsByCareResourceLocationId = deepCopy(tagsByCareResourceLocationId);
			this.careResourceLocationTagsPreloaded = careResourceLocationTagsPreloaded;
			this.tagsByCareResourceId = deepCopy(tagsByCareResourceId);
			this.careResourceTagsPreloaded = careResourceTagsPreloaded;
		}

		@Nonnull
		private Map<UUID, Map<CareResourceTagGroupId, List<CareResourceTag>>> deepCopy(@Nonnull Map<UUID, Map<CareResourceTagGroupId, List<CareResourceTag>>> tagsByParentId) {
			requireNonNull(tagsByParentId);

			Map<UUID, Map<CareResourceTagGroupId, List<CareResourceTag>>> tagsByParentIdCopy = new LinkedHashMap<>(tagsByParentId.size());

			for (Map.Entry<UUID, Map<CareResourceTagGroupId, List<CareResourceTag>>> parentEntry : tagsByParentId.entrySet()) {
				UUID parentId = parentEntry.getKey();
				Map<CareResourceTagGroupId, List<CareResourceTag>> tagsByGroupId = parentEntry.getValue();
				Map<CareResourceTagGroupId, List<CareResourceTag>> tagsByGroupIdCopy = new LinkedHashMap<>();

				if (tagsByGroupId != null) {
					for (Map.Entry<CareResourceTagGroupId, List<CareResourceTag>> groupEntry : tagsByGroupId.entrySet())
						tagsByGroupIdCopy.put(groupEntry.getKey(), groupEntry.getValue() == null ? List.of() : List.copyOf(groupEntry.getValue()));
				}

				tagsByParentIdCopy.put(parentId, tagsByGroupIdCopy);
			}

			return tagsByParentIdCopy;
		}

		@Nullable
		public Address getAddressByAddressId(@Nullable UUID addressId) {
			return addressId == null ? null : addressesByAddressId.get(addressId);
		}

		@Nonnull
		public List<CareResourceTag> getTagsByCareResourceLocationIdAndGroupId(@Nullable UUID careResourceLocationId,
																																			 @Nonnull CareResourceTagGroupId careResourceTagGroupId) {
			requireNonNull(careResourceTagGroupId);

			if (careResourceLocationId == null)
				return List.of();

			Map<CareResourceTagGroupId, List<CareResourceTag>> tagsByGroupId = tagsByCareResourceLocationId.get(careResourceLocationId);
			return tagsByGroupId == null ? List.of() : tagsByGroupId.getOrDefault(careResourceTagGroupId, List.of());
		}

		@Nonnull
		public List<CareResourceTag> getTagsByCareResourceIdAndGroupId(@Nullable UUID careResourceId,
																														 @Nonnull CareResourceTagGroupId careResourceTagGroupId) {
			requireNonNull(careResourceTagGroupId);

			if (careResourceId == null)
				return List.of();

			Map<CareResourceTagGroupId, List<CareResourceTag>> tagsByGroupId = tagsByCareResourceId.get(careResourceId);
			return tagsByGroupId == null ? List.of() : tagsByGroupId.getOrDefault(careResourceTagGroupId, List.of());
		}

		public boolean isAddressesPreloaded() {
			return addressesPreloaded;
		}

		public boolean isCareResourceLocationTagsPreloaded() {
			return careResourceLocationTagsPreloaded;
		}

		public boolean isCareResourceTagsPreloaded() {
			return careResourceTagsPreloaded;
		}
	}

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface CareResourceLocationApiResponseFactory {
		@Nonnull
		CareResourceLocationApiResponse create(@Nonnull CareResourceLocation careResourceLocation);

		@Nonnull
		CareResourceLocationApiResponse create(@Nonnull CareResourceLocation careResourceLocation,
																					 @Nonnull CareResourceLocationApiResponseBatchContext batchContext);
	}

	@AssistedInject
	public CareResourceLocationApiResponse(@Nonnull CareResourceService careResourceService,
																				 @Assisted @Nonnull CareResourceLocation careResourceLocation,
																				 @Nonnull AddressService addressService,
																				 @Nonnull CareResourceTagApiResponseFactory careResourceTagApiResponseFactory,
																				 @Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
																				 @Nonnull Formatter formatter) {
		this(careResourceService,
				careResourceLocation,
				addressService,
				careResourceTagApiResponseFactory,
				currentContextProvider,
				formatter,
				CareResourceLocationApiResponseBatchContext.empty());
	}

	@AssistedInject
	public CareResourceLocationApiResponse(@Nonnull CareResourceService careResourceService,
																				 @Assisted @Nonnull CareResourceLocation careResourceLocation,
																				 @Nonnull AddressService addressService,
																				 @Nonnull CareResourceTagApiResponseFactory careResourceTagApiResponseFactory,
																				 @Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
																				 @Nonnull Formatter formatter,
																				 @Assisted @Nonnull CareResourceLocationApiResponseBatchContext batchContext) {
		requireNonNull(careResourceService);
		requireNonNull(careResourceLocation);
		requireNonNull(addressService);
		requireNonNull(formatter);
		requireNonNull(careResourceTagApiResponseFactory);
		requireNonNull(currentContextProvider);
		requireNonNull(batchContext);

		CurrentContext currentContext = currentContextProvider.get();
		Account account = currentContext.getAccount().get();

		if (account.getRoleId() == Role.RoleId.MHIC)
			this.internalNotes = careResourceLocation.getInternalNotes();

		this.resourceName = careResourceLocation.getResourceName();
		this.resourceNotes = careResourceLocation.getResourceNotes();
		this.careResourceId = careResourceLocation.getCareResourceId();
		this.websiteUrl = careResourceLocation.getWebsiteUrl();
		this.emailAddress = careResourceLocation.getEmailAddress();
		this.acceptingNewPatients = careResourceLocation.getAcceptingNewPatients();
		this.careResourceLocationId = careResourceLocation.getCareResourceLocationId();
		this.name = careResourceLocation.getName();
		this.address = batchContext.isAddressesPreloaded()
				? batchContext.getAddressByAddressId(careResourceLocation.getAddressId())
				: addressService.findAddressById(careResourceLocation.getAddressId()).orElse(null);
		this.phoneNumber = careResourceLocation.getPhoneNumber();
		this.formattedPhoneNumber = formatter.formatPhoneNumber(careResourceLocation.getPhoneNumber());
		this.notes = careResourceLocation.getNotes();
		this.wheelchairAccess = careResourceLocation.getWheelchairAccess();
		this.overridePayors = careResourceLocation.getOverridePayors();
		this.overrideSpecialties = careResourceLocation.getOverrideSpecialties();
		this.appointmentTypeInPerson = careResourceLocation.getAppointmentTypeInPerson();
		this.appointmentTypeOnline = careResourceLocation.getAppointmentTypeOnline();

		List<CareResourceTag> locationLanguages = batchContext.isCareResourceLocationTagsPreloaded()
				? batchContext.getTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.LANGUAGES)
				: careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.LANGUAGES);
		this.languages = locationLanguages.stream()
				.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
				.collect(Collectors.toList());

		boolean overrideSpecialties = Boolean.TRUE.equals(this.overrideSpecialties);
		boolean overridePayors = Boolean.TRUE.equals(this.overridePayors);

		if (overrideSpecialties) {
			List<CareResourceTag> locationSpecialties = batchContext.isCareResourceLocationTagsPreloaded()
					? batchContext.getTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.SPECIALTIES)
					: careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.SPECIALTIES);
			this.specialties = locationSpecialties.stream()
					.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
					.collect(Collectors.toList());
		} else {
			List<CareResourceTag> resourceSpecialties = batchContext.isCareResourceTagsPreloaded()
					? batchContext.getTagsByCareResourceIdAndGroupId(careResourceLocation.getCareResourceId(), CareResourceTagGroupId.SPECIALTIES)
					: careResourceService.findTagsByCareResourceIdAndGroupId(careResourceLocation.getCareResourceId(), CareResourceTagGroupId.SPECIALTIES);
			this.specialties = resourceSpecialties.stream()
					.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
					.collect(Collectors.toList());
		}

		if (overridePayors) {
			List<CareResourceTag> locationPayors = batchContext.isCareResourceLocationTagsPreloaded()
					? batchContext.getTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.PAYORS)
					: careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.PAYORS);
			this.payors = locationPayors.stream()
					.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
					.collect(Collectors.toList());
			this.insuranceNotes = careResourceLocation.getInsuranceNotes();
		} else {
			List<CareResourceTag> resourcePayors = batchContext.isCareResourceTagsPreloaded()
					? batchContext.getTagsByCareResourceIdAndGroupId(careResourceLocation.getCareResourceId(), CareResourceTagGroupId.PAYORS)
					: careResourceService.findTagsByCareResourceIdAndGroupId(careResourceLocation.getCareResourceId(), CareResourceTagGroupId.PAYORS);
			this.payors = resourcePayors.stream()
					.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
					.collect(Collectors.toList());
			this.insuranceNotes = careResourceLocation.getResourceInsuranceNotes();
		}

		List<CareResourceTag> locationTherapyTypes = batchContext.isCareResourceLocationTagsPreloaded()
				? batchContext.getTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.THERAPY_TYPES)
				: careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.THERAPY_TYPES);
		this.therapyTypes = locationTherapyTypes.stream()
				.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
				.collect(Collectors.toList());

		List<CareResourceTag> locationPopulationServed = batchContext.isCareResourceLocationTagsPreloaded()
				? batchContext.getTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.POPULATION_SERVED)
				: careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.POPULATION_SERVED);
		this.populationServed = locationPopulationServed.stream()
				.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
				.collect(Collectors.toList());

		List<CareResourceTag> locationGenders = batchContext.isCareResourceLocationTagsPreloaded()
				? batchContext.getTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.GENDERS)
				: careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.GENDERS);
		this.genders = locationGenders.stream()
				.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
				.collect(Collectors.toList());

		List<CareResourceTag> locationEthnicities = batchContext.isCareResourceLocationTagsPreloaded()
				? batchContext.getTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.ETHNICITIES)
				: careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.ETHNICITIES);
		this.ethnicities = locationEthnicities.stream()
				.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
				.collect(Collectors.toList());

		List<CareResourceTag> locationFacilityTypes = batchContext.isCareResourceLocationTagsPreloaded()
				? batchContext.getTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.FACILITY_TYPES)
				: careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.FACILITY_TYPES);
		this.facilityTypes = locationFacilityTypes.stream()
				.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
				.collect(Collectors.toList());
		this.distanceInMiles = careResourceLocation.getDistanceInMiles();
	}

	@Nullable
	public UUID getCareResourceLocationId() {
		return careResourceLocationId;
	}

	@Nullable
	public Address getAddress() {
		return address;
	}

	@Nullable
	public String getPhoneNumber() {
		return phoneNumber;
	}

	@Nullable
	public String getFormattedPhoneNumber() {
		return formattedPhoneNumber;
	}

	@Nullable
	public String getNotes() {
		return notes;
	}

	@Nullable
	public Boolean getWheelchairAccess() {
		return wheelchairAccess;
	}

	@Nullable
	public List<CareResourceTagApiResponse> getLanguages() {
		return languages;
	}


	@Nullable
	public CareResourceTagApiResponseFactory getCareResourceTagApiResponseFactory() {
		return careResourceTagApiResponseFactory;
	}

	@Nullable
	public List<CareResourceTagApiResponse> getSpecialties() {
		return specialties;
	}

	@Nullable
	public List<CareResourceTagApiResponse> getPayors() {
		return payors;
	}

	@Nullable
	public List<CareResourceTagApiResponse> getTherapyTypes() {
		return therapyTypes;
	}

	@Nullable
	public List<CareResourceTagApiResponse> getPopulationServed() {
		return populationServed;
	}

	@Nullable
	public List<CareResourceTagApiResponse> getGenders() {
		return genders;
	}

	@Nullable
	public List<CareResourceTagApiResponse> getEthnicities() {
		return ethnicities;
	}

	@Nullable
	public UUID getCareResourceId() {
		return careResourceId;
	}

	@Nullable
	public String getGooglePlaceId() {
		return googlePlaceId;
	}

	@Nullable
	public String getName() {
		return name;
	}

	@Nullable
	public String getWebsiteUrl() {
		return websiteUrl;
	}

	@Nullable
	public String getEmailAddress() {
		return emailAddress;
	}

	@Nullable
	public String getInsuranceNotes() {
		return insuranceNotes;
	}

	@Nullable
	public Boolean getAcceptingNewPatients() {
		return acceptingNewPatients;
	}

	@Nullable
	public String getInternalNotes() {
		return internalNotes;
	}

	@Nullable
	public String getResourceName() {
		return resourceName;
	}

	@Nullable
	public String getResourceNotes() {
		return resourceNotes;
	}

	@Nullable
	public List<CareResourceTagApiResponse> getFacilityTypes() {
		return facilityTypes;
	}

	@Nullable
	public Boolean getOverridePayors() {
		return overridePayors;
	}

	@Nullable
	public Boolean getOverrideSpecialties() {
		return overrideSpecialties;
	}

	@Nullable
	public Boolean getAppointmentTypeInPerson() {
		return appointmentTypeInPerson;
	}

	@Nullable
	public Boolean getAppointmentTypeOnline() {
		return appointmentTypeOnline;
	}

	@Nullable
	public Double getDistanceInMiles() {
		return distanceInMiles;
	}
}
