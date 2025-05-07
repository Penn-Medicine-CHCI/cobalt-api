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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class InstitutionUrl {
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private UserExperienceTypeId userExperienceTypeId;
	@Nullable
	private String url;
	@Nullable
	private String hostname;
	@Nullable
	private Boolean preferred;
	@Nullable
	private String messageBaseUrl;

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public UserExperienceTypeId getUserExperienceTypeId() {
		return this.userExperienceTypeId;
	}

	public void setUserExperienceTypeId(@Nullable UserExperienceTypeId userExperienceTypeId) {
		this.userExperienceTypeId = userExperienceTypeId;
	}

	@Nullable
	public String getUrl() {
		return this.url;
	}

	public void setUrl(@Nullable String url) {
		this.url = url;
	}

	@Nullable
	public String getHostname() {
		return this.hostname;
	}

	public void setHostname(@Nullable String hostname) {
		this.hostname = hostname;
	}

	@Nullable
	public Boolean getPreferred() {
		return this.preferred;
	}

	public void setPreferred(@Nullable Boolean preferred) {
		this.preferred = preferred;
	}

	@Nullable
	public String getMessageBaseUrl() {
		return this.messageBaseUrl;
	}

	public void setMessageBaseUrl(@Nullable String messageBaseUrl) {
		this.messageBaseUrl = messageBaseUrl;
	}
}