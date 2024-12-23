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
import com.cobaltplatform.api.model.db.CareResource;
import com.cobaltplatform.api.model.db.CareResourceLocation;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

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
	private CareResourceTagApiResponseFactory careResourceTagApiResponseFactory;


	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface CareResourceLocationApiResponseFactory {
		@Nonnull
		CareResourceLocationApiResponse create(@Nonnull CareResourceLocation careResourceLocation,
																					 @Nonnull CareResource careResource);
	}

	@AssistedInject
	public CareResourceLocationApiResponse(@Nonnull CareResourceService careResourceService,
																				 @Assisted @Nonnull CareResourceLocation careResourceLocation,
																				 @Assisted @Nonnull CareResource careResource,
																				 @Nonnull AddressService addressService,
																				 @Nonnull CareResourceTagApiResponseFactory careResourceTagApiResponseFactory,
																				 @Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
																				 @Nonnull Formatter formatter) {
		requireNonNull(careResourceService);
		requireNonNull(careResourceLocation);
		requireNonNull(addressService);
		requireNonNull(formatter);
		requireNonNull(careResourceTagApiResponseFactory);
		requireNonNull(currentContextProvider);

		CurrentContext currentContext = currentContextProvider.get();
		Account account = currentContext.getAccount().get();

		if (account.getRoleId() == Role.RoleId.MHIC)
			this.internalNotes = careResourceLocation.getInternalNotes();

		this.resourceName = careResource.getName();
		this.resourceNotes = careResource.getNotes();
		this.careResourceId = careResourceLocation.getCareResourceId();
		this.websiteUrl = careResourceLocation.getWebsiteUrl();
		this.emailAddress = careResourceLocation.getEmailAddress();
		this.acceptingNewPatients = careResourceLocation.getAcceptingNewPatients();
		this.careResourceLocationId = careResourceLocation.getCareResourceLocationId();
		this.name = careResourceLocation.getName();
		this.address = addressService.findAddressById(careResourceLocation.getAddressId()).orElse(null);
		this.phoneNumber = careResourceLocation.getPhoneNumber();
		this.formattedPhoneNumber = formatter.formatPhoneNumber(careResourceLocation.getPhoneNumber());
		this.notes = careResourceLocation.getNotes();
		this.wheelchairAccess = careResourceLocation.getWheelchairAccess();
		this.overridePayors = careResourceLocation.getOverridePayors();
		this.overrideSpecialties = careResourceLocation.getOverrideSpecialties();
		this.appointmentTypeInPerson = careResourceLocation.getAppointmentTypeInPerson();
		this.appointmentTypeOnline = careResourceLocation.getAppointmentTypeOnline();
		this.languages = careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.LANGUAGES).stream()
				.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
				.collect(Collectors.toList());

		if (this.overrideSpecialties)
			this.specialties = careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.SPECIALTIES).stream()
					.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
					.collect(Collectors.toList());
		else
			this.specialties = careResourceService.findTagsByCareResourceIdAndGroupId(careResource.getCareResourceId(), CareResourceTagGroupId.SPECIALTIES).stream()
					.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
					.collect(Collectors.toList());

		if (this.overridePayors) {
			this.payors = careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.PAYORS).stream()
					.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
					.collect(Collectors.toList());
			this.insuranceNotes = careResourceLocation.getInsuranceNotes();
		} else {
			this.payors = careResourceService.findTagsByCareResourceIdAndGroupId(careResource.getCareResourceId(), CareResourceTagGroupId.PAYORS).stream()
					.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
					.collect(Collectors.toList());
			this.insuranceNotes = careResource.getInsuranceNotes();
		}
		this.therapyTypes = careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.THERAPY_TYPES).stream()
				.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
				.collect(Collectors.toList());
		this.populationServed = careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.POPULATION_SERVED).stream()
				.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
				.collect(Collectors.toList());
		this.genders = careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.GENDERS).stream()
				.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
				.collect(Collectors.toList());
		this.ethnicities = careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.ETHNICITIES).stream()
				.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
				.collect(Collectors.toList());
		this.facilityTypes = careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTagGroupId.FACILITY_TYPES).stream()
				.map(careResourceTag -> careResourceTagApiResponseFactory.create(careResourceTag))
				.collect(Collectors.toList());
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
}