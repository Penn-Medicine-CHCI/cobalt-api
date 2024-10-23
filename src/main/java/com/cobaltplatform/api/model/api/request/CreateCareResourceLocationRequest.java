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
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;

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
	private String notes;
	@Nullable
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
	private List<UUID> specialtyIds;
	@Nullable
	private List<SupportRoleId> supportRoleIds;
	@Nullable
	private Boolean wheelchairAccessible;

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
	public String getNotes() {
		return notes;
	}

	public void setNotes(@Nullable String notes) {
		this.notes = notes;
	}

	@Nullable
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(@Nullable String phoneNumber) {
		this.phoneNumber = phoneNumber;
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
	public List<UUID> getSpecialtyIds() {
		return specialtyIds;
	}

	public void setSpecialtyIds(@Nullable List<UUID> specialtyIds) {
		this.specialtyIds = specialtyIds;
	}

	@Nullable
	public List<SupportRoleId> getSupportRoleIds() {
		return supportRoleIds;
	}

	public void setSupportRoleIds(@Nullable List<SupportRoleId> supportRoleIds) {
		this.supportRoleIds = supportRoleIds;
	}

	@Nullable
	public Boolean getWheelchairAccessible() {
		return wheelchairAccessible;
	}

	public void setWheelchairAccessible(@Nullable Boolean wheelchairAccessible) {
		this.wheelchairAccessible = wheelchairAccessible;
	}

	@Nullable
	public String getWebsiteUrl() {
		return websiteUrl;
	}

	public void setWebsiteUrl(@Nullable String websiteUrl) {
		this.websiteUrl = websiteUrl;
	}
}
