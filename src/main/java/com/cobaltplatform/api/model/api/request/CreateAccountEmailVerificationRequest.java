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

import com.cobaltplatform.api.model.service.AccountEmailVerificationFlowTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class CreateAccountEmailVerificationRequest {
	@Nullable
	private UUID accountId;
	@Nullable
	private String emailAddress;
	@Nullable
	private Boolean forceVerification;
	@Nullable
	private AccountEmailVerificationFlowTypeId accountEmailVerificationFlowTypeId;

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public String getEmailAddress() {
		return this.emailAddress;
	}

	public void setEmailAddress(@Nullable String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Nullable
	public Boolean getForceVerification() {
		return this.forceVerification;
	}

	public void setForceVerification(@Nullable Boolean forceVerification) {
		this.forceVerification = forceVerification;
	}

	@Nullable
	public AccountEmailVerificationFlowTypeId getAccountEmailVerificationFlowTypeId() {
		return this.accountEmailVerificationFlowTypeId;
	}

	public void setAccountEmailVerificationFlowTypeId(@Nullable AccountEmailVerificationFlowTypeId accountEmailVerificationFlowTypeId) {
		this.accountEmailVerificationFlowTypeId = accountEmailVerificationFlowTypeId;
	}
}
