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
public class AuthenticationRedirectRequest {
	@Nullable
	private String tenant;
	@Nullable
	private String clientId;
	@Nullable
	private String responseType;
	@Nullable
	private String redirectUri;
	@Nullable
	private String scope;
	@Nullable
	private String responseMode;
	@Nullable
	private String state;
	@Nullable
	private String prompt;
	@Nullable
	private String loginHint;
	@Nullable
	private String domainHint;
	@Nullable
	private String codeChallenge;
	@Nullable
	private String codeChallengeMethod;

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
	public String getResponseType() {
		return this.responseType;
	}

	public void setResponseType(@Nullable String responseType) {
		this.responseType = responseType;
	}

	@Nullable
	public String getRedirectUri() {
		return this.redirectUri;
	}

	public void setRedirectUri(@Nullable String redirectUri) {
		this.redirectUri = redirectUri;
	}

	@Nullable
	public String getScope() {
		return this.scope;
	}

	public void setScope(@Nullable String scope) {
		this.scope = scope;
	}

	@Nullable
	public String getResponseMode() {
		return this.responseMode;
	}

	public void setResponseMode(@Nullable String responseMode) {
		this.responseMode = responseMode;
	}

	@Nullable
	public String getState() {
		return this.state;
	}

	public void setState(@Nullable String state) {
		this.state = state;
	}

	@Nullable
	public String getPrompt() {
		return this.prompt;
	}

	public void setPrompt(@Nullable String prompt) {
		this.prompt = prompt;
	}

	@Nullable
	public String getLoginHint() {
		return this.loginHint;
	}

	public void setLoginHint(@Nullable String loginHint) {
		this.loginHint = loginHint;
	}

	@Nullable
	public String getDomainHint() {
		return this.domainHint;
	}

	public void setDomainHint(@Nullable String domainHint) {
		this.domainHint = domainHint;
	}

	@Nullable
	public String getCodeChallenge() {
		return this.codeChallenge;
	}

	public void setCodeChallenge(@Nullable String codeChallenge) {
		this.codeChallenge = codeChallenge;
	}

	@Nullable
	public String getCodeChallengeMethod() {
		return this.codeChallengeMethod;
	}

	public void setCodeChallengeMethod(@Nullable String codeChallengeMethod) {
		this.codeChallengeMethod = codeChallengeMethod;
	}
}