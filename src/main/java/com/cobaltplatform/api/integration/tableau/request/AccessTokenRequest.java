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

package com.cobaltplatform.api.integration.tableau.request;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AccessTokenRequest {
	@Nullable
	private final String emailAddress;
	@Nonnull
	private final List<String> scopes;
	@Nonnull
	private final Map<String, String> claims;

	protected AccessTokenRequest(@Nonnull Builder builder) {
		requireNonNull(builder);
		this.emailAddress = builder.emailAddress;
		this.scopes = builder.scopes == null ? List.of() : Collections.unmodifiableList(builder.scopes);
		this.claims = builder.claims == null ? Map.of() : Collections.unmodifiableMap(builder.claims);
	}

	@NotThreadSafe
	public static class Builder {
		@Nonnull
		private final String emailAddress;
		@Nullable
		private List<String> scopes;
		@Nullable
		private Map<String, String> claims;

		public Builder(@Nonnull String emailAddress) {
			requireNonNull(emailAddress);
			this.emailAddress = emailAddress;
		}

		@Nonnull
		public Builder scopes(@Nullable List<String> scopes) {
			this.scopes = scopes;
			return this;
		}

		@Nonnull
		public Builder claims(@Nullable Map<String, String> claims) {
			this.claims = claims;
			return this;
		}

		@Nonnull
		public AccessTokenRequest build() {
			return new AccessTokenRequest(this);
		}
	}

	@Nullable
	public String getEmailAddress() {
		return this.emailAddress;
	}

	@Nonnull
	public List<String> getScopes() {
		return this.scopes;
	}

	@Nonnull
	public Map<String, String> getClaims() {
		return this.claims;
	}
}