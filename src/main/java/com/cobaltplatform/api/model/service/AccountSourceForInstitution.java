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
package com.cobaltplatform.api.model.service;

import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.AccountSourceDisplayStyle.AccountSourceDisplayStyleId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.UserExperienceType;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AccountSourceForInstitution {
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private AccountSourceId accountSourceId;
	@Nullable
	private AccountSourceDisplayStyleId accountSourceDisplayStyleId;
	@Nullable
	private UserExperienceTypeId requiresUserExperienceTypeId;
	@Nullable
	private String description;
	@Nullable
	private String authenticationDescription;
	@Nullable
	private String localSsoUrl;
	@Nullable
	private String devSsoUrl;
	@Nullable
	private String prodSsoUrl;
	@Nullable
	private Boolean visible;
	@Nullable
	private Integer displayOrder;

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public AccountSourceId getAccountSourceId() {
		return this.accountSourceId;
	}

	public void setAccountSourceId(@Nullable AccountSourceId accountSourceId) {
		this.accountSourceId = accountSourceId;
	}

	@Nullable
	public AccountSourceDisplayStyleId getAccountSourceDisplayStyleId() {
		return this.accountSourceDisplayStyleId;
	}

	public void setAccountSourceDisplayStyleId(@Nullable AccountSourceDisplayStyleId accountSourceDisplayStyleId) {
		this.accountSourceDisplayStyleId = accountSourceDisplayStyleId;
	}

	@Nullable
	public UserExperienceTypeId getRequiresUserExperienceTypeId() {
		return this.requiresUserExperienceTypeId;
	}

	public void setRequiresUserExperienceTypeId(@Nullable UserExperienceTypeId requiresUserExperienceTypeId) {
		this.requiresUserExperienceTypeId = requiresUserExperienceTypeId;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public String getAuthenticationDescription() {
		return this.authenticationDescription;
	}

	public void setAuthenticationDescription(@Nullable String authenticationDescription) {
		this.authenticationDescription = authenticationDescription;
	}

	@Nullable
	public String getLocalSsoUrl() {
		return this.localSsoUrl;
	}

	public void setLocalSsoUrl(@Nullable String localSsoUrl) {
		this.localSsoUrl = localSsoUrl;
	}

	@Nullable
	public String getDevSsoUrl() {
		return this.devSsoUrl;
	}

	public void setDevSsoUrl(@Nullable String devSsoUrl) {
		this.devSsoUrl = devSsoUrl;
	}

	@Nullable
	public String getProdSsoUrl() {
		return this.prodSsoUrl;
	}

	public void setProdSsoUrl(@Nullable String prodSsoUrl) {
		this.prodSsoUrl = prodSsoUrl;
	}

	@Nullable
	public Boolean getVisible() {
		return this.visible;
	}

	public void setVisible(@Nullable Boolean visible) {
		this.visible = visible;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return this.displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}
}
