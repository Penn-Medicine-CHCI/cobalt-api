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

package com.cobaltplatform.api.integration.mychart;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class MyChartConfiguration {
	@Nullable
	private String clientId;
	@Nullable
	private String clientSecret;
	@Nullable
	private String responseType; // e.g. "code"
	@Nullable
	private String scope; // e.g. "patient/*.* user/*.*"
	@Nullable
	private String tokenUrl; // e.g. https://EPIC_BASE_URL/ENV-FHIR/oauth2/token";
	@Nullable
	private String authorizeUrl; // e.g. https://EPIC_BASE_URL/ENV-FHIR/oauth2/authorize";
	@Nullable
	private String callbackUrl; // e.g. https://COBALT_BACKEND_BASE_URL/mychart/oauth/callback

	@Nullable
	public String getClientId() {
		return this.clientId;
	}

	public void setClientId(@Nullable String clientId) {
		this.clientId = clientId;
	}

	@Nullable
	public String getClientSecret() {
		return this.clientSecret;
	}

	public void setClientSecret(@Nullable String clientSecret) {
		this.clientSecret = clientSecret;
	}

	@Nullable
	public String getResponseType() {
		return this.responseType;
	}

	public void setResponseType(@Nullable String responseType) {
		this.responseType = responseType;
	}

	@Nullable
	public String getScope() {
		return this.scope;
	}

	public void setScope(@Nullable String scope) {
		this.scope = scope;
	}

	@Nullable
	public String getTokenUrl() {
		return this.tokenUrl;
	}

	public void setTokenUrl(@Nullable String tokenUrl) {
		this.tokenUrl = tokenUrl;
	}

	@Nullable
	public String getAuthorizeUrl() {
		return this.authorizeUrl;
	}

	public void setAuthorizeUrl(@Nullable String authorizeUrl) {
		this.authorizeUrl = authorizeUrl;
	}

	@Nullable
	public String getCallbackUrl() {
		return this.callbackUrl;
	}

	public void setCallbackUrl(@Nullable String callbackUrl) {
		this.callbackUrl = callbackUrl;
	}
}
