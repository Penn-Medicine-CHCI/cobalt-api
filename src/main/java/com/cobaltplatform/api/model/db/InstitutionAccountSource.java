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

import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.AccountSourceDisplayStyle.AccountSourceDisplayStyleId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class InstitutionAccountSource {
	@Nullable
	private UUID institutionAccountSourceId;
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
	private Integer displayOrder;

	@Nullable
	public UUID getInstitutionAccountSourceId() {
		return this.institutionAccountSourceId;
	}

	public void setInstitutionAccountSourceId(@Nullable UUID institutionAccountSourceId) {
		this.institutionAccountSourceId = institutionAccountSourceId;
	}

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
	public Integer getDisplayOrder() {
		return this.displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}
}
