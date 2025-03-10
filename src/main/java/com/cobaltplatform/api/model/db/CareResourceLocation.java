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

package com.cobaltplatform.api.model.db;

import org.apache.arrow.flatbuf.Bool;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CareResourceLocation {
	@Nullable
	private String name;
	@Nullable
	private UUID careResourceLocationId;
	@Nullable
	private UUID careResourceId;
	@Nullable
	private UUID addressId;
	@Nullable
	private String phoneNumber;
	@Nullable
	private String notes;
	@Nullable
	private String internalNotes;
	@Nullable
	private Boolean wheelchairAccess;
	@Nullable
	private Boolean acceptingNewPatients;
	@Nullable
	private String websiteUrl;
	@Nullable
	private String emailAddress;
	@Nullable
	private String insuranceNotes;
	@Nullable
	private Boolean overridePayors;
	@Nullable
	private Boolean overrideSpecialties;
	@Nullable
	private Boolean appointmentTypeInPerson;
	@Nullable
	private Boolean appointmentTypeOnline;

	@Nullable
	Double distanceInMiles;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	//From the Care Resource
	@Nullable
	private String resourceName;
	@Nullable
	private String resourceNotes;
	@Nullable
	private String resourceInsuranceNotes;

	@Nullable
	public UUID getCareResourceLocationId() {
		return careResourceLocationId;
	}

	public void setCareResourceLocationId(@Nullable UUID careResourceLocationId) {
		this.careResourceLocationId = careResourceLocationId;
	}

	@Nullable
	public UUID getAddressId() {
		return addressId;
	}

	public void setAddressId(@Nullable UUID addressId) {
		this.addressId = addressId;
	}

	@Nullable
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(@Nullable String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@Nullable
	public String getNotes() {
		return notes;
	}

	public void setNotes(@Nullable String notes) {
		this.notes = notes;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	@Nullable
	public UUID getCareResourceId() {
		return careResourceId;
	}

	public void setCareResourceId(@Nullable UUID careResourceId) {
		this.careResourceId = careResourceId;
	}

	@Nullable
	public Boolean getWheelchairAccess() {
		return wheelchairAccess;
	}

	public void setWheelchairAccess(@Nullable Boolean wheelchairAccess) {
		this.wheelchairAccess = wheelchairAccess;
	}

	@Nullable
	public Boolean getAcceptingNewPatients() {
		return acceptingNewPatients;
	}

	public void setAcceptingNewPatients(@Nullable Boolean acceptingNewPatients) {
		this.acceptingNewPatients = acceptingNewPatients;
	}

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getWebsiteUrl() {
		return websiteUrl;
	}

	public void setWebsiteUrl(@Nullable String websiteUrl) {
		this.websiteUrl = websiteUrl;
	}

	@Nullable
	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(@Nullable String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Nullable
	public String getInsuranceNotes() {
		return insuranceNotes;
	}

	public void setInsuranceNotes(@Nullable String insuranceNotes) {
		this.insuranceNotes = insuranceNotes;
	}

	@Nullable
	public String getInternalNotes() {
		return internalNotes;
	}

	public void setInternalNotes(@Nullable String internalNotes) {
		this.internalNotes = internalNotes;
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

	@Nullable
	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(@Nullable String resourceName) {
		this.resourceName = resourceName;
	}

	@Nullable
	public String getResourceNotes() {
		return resourceNotes;
	}

	public void setResourceNotes(@Nullable String resourceNotes) {
		this.resourceNotes = resourceNotes;
	}

	@Nullable
	public String getResourceInsuranceNotes() {
		return resourceInsuranceNotes;
	}

	public void setResourceInsuranceNotes(@Nullable String resourceInsuranceNotes) {
		this.resourceInsuranceNotes = resourceInsuranceNotes;
	}

	@Nullable
	public Double getDistanceInMiles() {
		return distanceInMiles;
	}

	public void setDistanceInMiles(@Nullable Double distanceInMiles) {
		this.distanceInMiles = distanceInMiles;
	}
}