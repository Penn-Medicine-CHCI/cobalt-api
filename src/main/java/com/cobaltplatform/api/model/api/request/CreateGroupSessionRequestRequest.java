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
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreateGroupSessionRequestRequest {
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private UUID submitterAccountId;
	@Nullable
	private String title;
	@Nullable
	private String description;
	@Nullable
	private String urlName;
	@Nullable
	private UUID facilitatorAccountId;
	@Nullable
	private String facilitatorName;
	@Nullable
	private String facilitatorEmailAddress;
	@Nullable
	private String imageUrl;
	@Nullable
	private String customQuestion1;
	@Nullable
	private String customQuestion2;
	@Nullable
	private Boolean dataCollectionEnabled;

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public UUID getSubmitterAccountId() {
		return submitterAccountId;
	}

	public void setSubmitterAccountId(@Nullable UUID submitterAccountId) {
		this.submitterAccountId = submitterAccountId;
	}

	@Nullable
	public String getTitle() {
		return title;
	}

	public void setTitle(@Nullable String title) {
		this.title = title;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public String getUrlName() {
		return urlName;
	}

	public void setUrlName(@Nullable String urlName) {
		this.urlName = urlName;
	}

	@Nullable
	public UUID getFacilitatorAccountId() {
		return facilitatorAccountId;
	}

	public void setFacilitatorAccountId(@Nullable UUID facilitatorAccountId) {
		this.facilitatorAccountId = facilitatorAccountId;
	}

	@Nullable
	public String getFacilitatorName() {
		return facilitatorName;
	}

	public void setFacilitatorName(@Nullable String facilitatorName) {
		this.facilitatorName = facilitatorName;
	}

	@Nullable
	public String getFacilitatorEmailAddress() {
		return facilitatorEmailAddress;
	}

	public void setFacilitatorEmailAddress(@Nullable String facilitatorEmailAddress) {
		this.facilitatorEmailAddress = facilitatorEmailAddress;
	}

	@Nullable
	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(@Nullable String imageUrl) {
		this.imageUrl = imageUrl;
	}

	@Nullable
	public String getCustomQuestion1() {
		return customQuestion1;
	}

	public void setCustomQuestion1(@Nullable String customQuestion1) {
		this.customQuestion1 = customQuestion1;
	}

	@Nullable
	public String getCustomQuestion2() {
		return customQuestion2;
	}

	public void setCustomQuestion2(@Nullable String customQuestion2) {
		this.customQuestion2 = customQuestion2;
	}

	@Nullable
	public Boolean getDataCollectionEnabled() {
		return this.dataCollectionEnabled;
	}

	public void setDataCollectionEnabled(@Nullable Boolean dataCollectionEnabled) {
		this.dataCollectionEnabled = dataCollectionEnabled;
	}
}
