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
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.LoginDestination.LoginDestinationId;
import com.cobaltplatform.api.model.db.Role.RoleId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AccountLoginRule {
	@Nullable
	private UUID accountLoginRuleId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private AccountSourceId accountSourceId;
	@Nullable
	private LoginDestinationId loginDestinationId;
	@Nullable
	private RoleId roleId;
	@Nullable
	private UUID providerId;
	@Nullable
	private String emailAddress;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getAccountLoginRuleId() {
		return accountLoginRuleId;
	}

	public void setAccountLoginRuleId(@Nullable UUID accountLoginRuleId) {
		this.accountLoginRuleId = accountLoginRuleId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public AccountSourceId getAccountSourceId() {
		return accountSourceId;
	}

	public void setAccountSourceId(@Nullable AccountSourceId accountSourceId) {
		this.accountSourceId = accountSourceId;
	}

	@Nullable
	public LoginDestinationId getLoginDestinationId() {
		return loginDestinationId;
	}

	public void setLoginDestinationId(@Nullable LoginDestinationId loginDestinationId) {
		this.loginDestinationId = loginDestinationId;
	}

	@Nullable
	public RoleId getRoleId() {
		return roleId;
	}

	public void setRoleId(@Nullable RoleId roleId) {
		this.roleId = roleId;
	}

	@Nullable
	public UUID getProviderId() {
		return providerId;
	}

	public void setProviderId(@Nullable UUID providerId) {
		this.providerId = providerId;
	}

	@Nullable
	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(@Nullable String emailAddress) {
		this.emailAddress = emailAddress;
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
}
