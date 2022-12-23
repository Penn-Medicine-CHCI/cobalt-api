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

package com.cobaltplatform.api.integration.microsoft.request;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * See https://learn.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-auth-code-flow.
 *
 * @author Transmogrify, LLC.
 */
@Immutable
public class AccessTokenRequest {
	@Nullable
	private String tenant;
	@Nullable
	private String clientId;
	@Nullable
	private String scope;
	@Nullable
	private String code;
	@Nullable
	private String redirectUri;
	@Nullable
	private String grantType;
	@Nullable
	private String codeVerifier;
	@Nullable
	private String clientSecret;

	@Nullable
	public String getTenant() {
		return this.tenant;
	}

	public void setTenant(@Nullable String tenant) {
		this.tenant = tenant;
	}

	@Nullable
	public String getClientId() {
		return this.clientId;
	}

	public void setClientId(@Nullable String clientId) {
		this.clientId = clientId;
	}

	@Nullable
	public String getScope() {
		return this.scope;
	}

	public void setScope(@Nullable String scope) {
		this.scope = scope;
	}

	@Nullable
	public String getCode() {
		return this.code;
	}

	public void setCode(@Nullable String code) {
		this.code = code;
	}

	@Nullable
	public String getRedirectUri() {
		return this.redirectUri;
	}

	public void setRedirectUri(@Nullable String redirectUri) {
		this.redirectUri = redirectUri;
	}

	@Nullable
	public String getGrantType() {
		return this.grantType;
	}

	public void setGrantType(@Nullable String grantType) {
		this.grantType = grantType;
	}

	@Nullable
	public String getCodeVerifier() {
		return this.codeVerifier;
	}

	public void setCodeVerifier(@Nullable String codeVerifier) {
		this.codeVerifier = codeVerifier;
	}

	@Nullable
	public String getClientSecret() {
		return this.clientSecret;
	}

	public void setClientSecret(@Nullable String clientSecret) {
		this.clientSecret = clientSecret;
	}
}