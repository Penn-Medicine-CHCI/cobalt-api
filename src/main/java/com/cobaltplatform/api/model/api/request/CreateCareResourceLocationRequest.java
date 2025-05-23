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

package com.cobaltplatform.api.model.api.request;

import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreateCareResourceLocationRequest {
	@Nullable
	private UUID careResourceId;
	@Nullable
	private String googlePlaceId;
	@Nullable
	private String name;
	@Nullable
	private String notes;
	@Nullable
	private String emailAddress;
	@Nullable
	private String streetAddress2;
	@Nullable
	private String insuranceNotes;
	private String phoneNumber;
	@Nullable
	private String websiteUrl;
	@Nullable
	private Boolean acceptingNewPatients;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private List<String> payorIds;
	@Nullable
	private List<String> specialtyIds;
	@Nullable
	private List<String> therapyTypeIds;
	@Nullable
	private List<String> populationServedIds;
	@Nullable
	private List<String> genderIds;
	@Nullable
	private List<String> ethnicityIds;
	@Nullable
	private List<String> languageIds;
	@Nullable
	private List<String> facilityTypes;
	@Nullable
	private Boolean wheelchairAccess;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private Boolean overridePayors;
	@Nullable
	private Boolean overrideSpecialties;
	@Nullable
	private Boolean appointmentTypeInPerson;
	@Nullable
	private Boolean appointmentTypeOnline;
	@Nullable
	public UUID getCareResourceId() {
		return careResourceId;
	}

	public void setCareResourceId(@Nullable UUID careResourceId) {
		this.careResourceId = careResourceId;
	}

	@Nullable
	public String getGooglePlaceId() {
		return googlePlaceId;
	}

	public void setGooglePlaceId(@Nullable String googlePlaceId) {
		this.googlePlaceId = googlePlaceId;
	}

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getNotes() {
		return notes;
	}

	public void setNotes(@Nullable String notes) {
		this.notes = notes;
	}

	@Nullable
	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(@Nullable String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Nullable
	public String getStreetAddress2() {
		return streetAddress2;
	}

	public void setStreetAddress2(@Nullable String streetAddress2) {
		this.streetAddress2 = streetAddress2;
	}

	@Nullable
	public String getInsuranceNotes() {
		return insuranceNotes;
	}

	public void setInsuranceNotes(@Nullable String insuranceNotes) {
		this.insuranceNotes = insuranceNotes;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@Nullable
	public String getWebsiteUrl() {
		return websiteUrl;
	}

	public void setWebsiteUrl(@Nullable String websiteUrl) {
		this.websiteUrl = websiteUrl;
	}

	@Nullable
	public Boolean getAcceptingNewPatients() {
		return acceptingNewPatients;
	}

	public void setAcceptingNewPatients(@Nullable Boolean acceptingNewPatients) {
		this.acceptingNewPatients = acceptingNewPatients;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public List<String> getPayorIds() {
		return payorIds;
	}

	public void setPayorIds(@Nullable List<String> payorIds) {
		this.payorIds = payorIds;
	}

	@Nullable
	public List<String> getSpecialtyIds() {
		return specialtyIds;
	}

	public void setSpecialtyIds(@Nullable List<String> specialtyIds) {
		this.specialtyIds = specialtyIds;
	}

	@Nullable
	public List<String> getTherapyTypeIds() {
		return therapyTypeIds;
	}

	public void setTherapyTypeIds(@Nullable List<String> therapyTypeIds) {
		this.therapyTypeIds = therapyTypeIds;
	}

	@Nullable
	public List<String> getPopulationServedIds() {
		return populationServedIds;
	}

	public void setPopulationServedIds(@Nullable List<String> populationServedIds) {
		this.populationServedIds = populationServedIds;
	}

	@Nullable
	public List<String> getGenderIds() {
		return genderIds;
	}

	public void setGenderIds(@Nullable List<String> genderIds) {
		this.genderIds = genderIds;
	}

	@Nullable
	public List<String> getEthnicityIds() {
		return ethnicityIds;
	}

	public void setEthnicityIds(@Nullable List<String> ethnicityIds) {
		this.ethnicityIds = ethnicityIds;
	}

	@Nullable
	public List<String> getLanguageIds() {
		return languageIds;
	}

	public void setLanguageIds(@Nullable List<String> languageIds) {
		this.languageIds = languageIds;
	}

	@Nullable
	public Boolean getWheelchairAccess() {
		return wheelchairAccess;
	}

	public void setWheelchairAccess(@Nullable Boolean wheelchairAccess) {
		this.wheelchairAccess = wheelchairAccess;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public List<String> getFacilityTypes() {
		return facilityTypes;
	}

	public void setFacilityTypes(@Nullable List<String> facilityTypes) {
		this.facilityTypes = facilityTypes;
	}

	@Nullable
	public Boolean getOverridePayors() {
		return overridePayors;
	}

	public void setOverridePayors(@Nullable Boolean overridePayors) {
		this.overridePayors = overridePayors;
	}

	@Nullable
	public Boolean getOverrideSpecialties() {
		return overrideSpecialties;
	}

	public void setOverrideSpecialties(@Nullable Boolean overrideSpecialties) {
		this.overrideSpecialties = overrideSpecialties;
	}

	@Nullable
	public Boolean getAppointmentTypeInPerson() {
		return appointmentTypeInPerson;
	}

	public void setAppointmentTypeInPerson(@Nullable Boolean appointmentTypeInPerson) {
		this.appointmentTypeInPerson = appointmentTypeInPerson;
	}

	@Nullable
	public Boolean getAppointmentTypeOnline() {
		return appointmentTypeOnline;
	}

	public void setAppointmentTypeOnline(@Nullable Boolean appointmentTypeOnline) {
		this.appointmentTypeOnline = appointmentTypeOnline;
	}
}
