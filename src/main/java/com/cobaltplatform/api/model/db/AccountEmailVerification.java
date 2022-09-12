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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AccountEmailVerification {
	@Nullable
	private UUID accountEmailVerificationId;
	@Nullable
	private UUID accountId;
	@Nullable
	private String code;
	@Nullable
	private String emailAddress;
	@Nullable
	private Boolean verified;
	@Nullable
	private Instant expiration;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getAccountEmailVerificationId() {
		return this.accountEmailVerificationId;
	}

	public void setAccountEmailVerificationId(@Nullable UUID accountEmailVerificationId) {
		this.accountEmailVerificationId = accountEmailVerificationId;
	}

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public String getCode() {
		return this.code;
	}

	public void setCode(@Nullable String code) {
		this.code = code;
	}

	@Nullable
	public String getEmailAddress() {
		return this.emailAddress;
	}

	public void setEmailAddress(@Nullable String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Nullable
	public Boolean getVerified() {
		return this.verified;
	}

	public void setVerified(@Nullable Boolean verified) {
		this.verified = verified;
	}

	@Nullable
	public Instant getExpiration() {
		return this.expiration;
	}

	public void setExpiration(@Nullable Instant expiration) {
		this.expiration = expiration;
	}

	@Nullable
	public Instant getCreated() {
		return this.created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}
