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


import com.cobaltplatform.api.model.db.Address;
import com.cobaltplatform.api.model.db.CareResourceLocation;
import com.cobaltplatform.api.model.db.CareResourceTag;
import com.cobaltplatform.api.service.AddressService;
import com.cobaltplatform.api.service.CareResourceService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.UUID;

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
	private Boolean wheelchairAccess;
	@Nullable
	private Boolean acceptingNewPatients;
	@Nullable
	List<CareResourceTag> languages;
	@Nullable
	private List<CareResourceTag> specialties;
	@Nullable
	private List<CareResourceTag> payors;
	@Nullable
	private List<CareResourceTag> therapyTypes;
	@Nullable
	private List<CareResourceTag> populationServed;
	@Nullable
	private List<CareResourceTag> genders;
	@Nullable
	private List<CareResourceTag> ethnicities;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface CareResourceLocationApiResponseFactory {
		@Nonnull
		CareResourceLocationApiResponse create(@Nonnull CareResourceLocation careResourceLocation);
	}

	@AssistedInject
	public CareResourceLocationApiResponse(@Nonnull CareResourceService careResourceService,
																				 @Assisted @Nonnull CareResourceLocation careResourceLocation,
																				 @Nonnull AddressService addressService,
																				 @Nonnull Formatter formatter) {
		requireNonNull(careResourceLocation);
		requireNonNull(formatter);

		this.careResourceId = careResourceLocation.getCareResourceId();
		this.websiteUrl = careResourceLocation.getWebsiteUrl();
		this.emailAddress = careResourceLocation.getEmailAddress();
		this.insuranceNotes = careResourceLocation.getInsuranceNotes();
		this.acceptingNewPatients = careResourceLocation.getAcceptingNewPatients();
		this.googlePlaceId = careResourceLocation.getGooglePlaceId();
		this.careResourceLocationId = careResourceLocation.getCareResourceLocationId();
		this.name = careResourceLocation.getName();
		this.address = addressService.findAddressById(careResourceLocation.getAddressId()).orElse(null);
		this.phoneNumber = careResourceLocation.getPhoneNumber();
		this.formattedPhoneNumber = formatter.formatPhoneNumber(careResourceLocation.getPhoneNumber());
		this.notes = careResourceLocation.getNotes();
		this.wheelchairAccess = careResourceLocation.getWheelchairAccess();
		this.languages = careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTag.CareResourceTagGroupId.LANGUAGES);
		this.specialties = careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTag.CareResourceTagGroupId.SPECIALTIES);
		this.payors = careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTag.CareResourceTagGroupId.PAYORS);
		this.therapyTypes = careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTag.CareResourceTagGroupId.THERAPY_TYPES);
		this.populationServed = careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTag.CareResourceTagGroupId.POPULATION_SERVED);
		this.genders = careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTag.CareResourceTagGroupId.GENDERS);
		this.ethnicities = careResourceService.findTagsByCareResourceLocationIdAndGroupId(careResourceLocation.getCareResourceLocationId(), CareResourceTag.CareResourceTagGroupId.ETHNICITIES);
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
	public List<CareResourceTag> getLanguages() {
		return languages;
	}

	@Nullable
	public List<CareResourceTag> getSpecialties() {
		return specialties;
	}

	@Nullable
	public List<CareResourceTag> getTherapyTypes() {
		return therapyTypes;
	}

	@Nullable
	public List<CareResourceTag> getPopulationServed() {
		return populationServed;
	}

	@Nullable
	public List<CareResourceTag> getGenders() {
		return genders;
	}

	@Nullable
	public List<CareResourceTag> getEthnicities() {
		return ethnicities;
	}

	@Nullable
	public List<CareResourceTag> getPayors() {
		return payors;
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
}