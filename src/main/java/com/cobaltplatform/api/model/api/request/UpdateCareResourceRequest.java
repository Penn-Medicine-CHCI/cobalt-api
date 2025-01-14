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
public class UpdateCareResourceRequest {
	@Nullable
	private UUID careResourceId;
	@Nullable
	private String name;
	@Nullable
	private String notes;
	@Nullable
	private String insuranceNotes;
	@Nullable
	private List<String> specialtyIds;
	@Nullable
	private List<String> payorIds;
	@Nullable
	private String phoneNumber;
	@Nullable
	private String emailAddress;
	@Nullable
	private String websiteUrl;
	@Nullable
	private InstitutionId institutionId;

	@Nullable
	public UUID getCareResourceId() {
		return careResourceId;
	}

	public void setCareResourceId(@Nullable UUID careResourceId) {
		this.careResourceId = careResourceId;
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
	public String getInsuranceNotes() {
		return insuranceNotes;
	}

	public void setInsuranceNotes(@Nullable String insuranceNotes) {
		this.insuranceNotes = insuranceNotes;
	}

	@Nullable
	public List<String> getSpecialtyIds() {
		return specialtyIds;
	}

	public void setSpecialtyIds(@Nullable List<String> specialtyIds) {
		this.specialtyIds = specialtyIds;
	}

	@Nullable
	public List<String> getPayorIds() {
		return payorIds;
	}

	public void setPayorIds(@Nullable List<String> payorIds) {
		this.payorIds = payorIds;
	}

	@Nullable
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(@Nullable String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@Nullable
	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(@Nullable String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Nullable
	public String getWebsiteUrl() {
		return websiteUrl;
	}

	public void setWebsiteUrl(@Nullable String websiteUrl) {
		this.websiteUrl = websiteUrl;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}
}
