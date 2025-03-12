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

import static java.lang.String.format;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class AccountSource {
	@Nullable
	private AccountSourceId accountSourceId;
	@Nullable
	private String description;
	@Nullable
	private String shortDescription;
	@Nullable
	private String authenticationDescription;
	@Nullable
	private String localSsoUrl;
	@Nullable
	private String devSsoUrl;
	@Nullable
	private String prodSsoUrl;

	public enum AccountSourceId {
		COBALT_SSO,
		ANONYMOUS,
		ANONYMOUS_IMPLICIT,
		EMAIL_PASSWORD,
		MYCHART,
		USERNAME
	}

	@Override
	public String toString() {
		return format("%s{accountSourceId=%s, description=%s}", getClass().getSimpleName(), getAccountSourceId(), getDescription());
	}

	@Nullable
	public AccountSourceId getAccountSourceId() {
		return accountSourceId;
	}

	public void setAccountSourceId(@Nullable AccountSourceId accountSourceId) {
		this.accountSourceId = accountSourceId;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public String getShortDescription() {
		return this.shortDescription;
	}

	public void setShortDescription(@Nullable String shortDescription) {
		this.shortDescription = shortDescription;
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
		return localSsoUrl;
	}

	public void setLocalSsoUrl(@Nullable String localSsoUrl) {
		this.localSsoUrl = localSsoUrl;
	}

	@Nullable
	public String getDevSsoUrl() {
		return devSsoUrl;
	}

	public void setDevSsoUrl(@Nullable String devSsoUrl) {
		this.devSsoUrl = devSsoUrl;
	}

	@Nullable
	public String getProdSsoUrl() {
		return prodSsoUrl;
	}

	public void setProdSsoUrl(@Nullable String prodSsoUrl) {
		this.prodSsoUrl = prodSsoUrl;
	}
}