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

package com.cobaltplatform.api.integration.epic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class EpicBackendServiceAccessToken {
	@Nonnull
	private final String accessToken;
	@Nonnull
	private final String tokenType;
	@Nonnull
	private final Instant expiresAt;
	@Nullable
	private final String scope;

	protected EpicBackendServiceAccessToken(@Nonnull Builder builder) {
		requireNonNull(builder);

		this.accessToken = builder.accessToken;
		this.tokenType = builder.tokenType;
		this.expiresAt = builder.expiresAt;
		this.scope = builder.scope;
	}

	@Override
	public String toString() {
		return format("%s{accessToken=%s, tokenType=%s, expiresAt=%s, scope=%s}",
				getClass().getSimpleName(), getAccessToken(), getTokenType(), getExpiresAt(), getScope());
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;

		if (other == null || !getClass().equals(other.getClass()))
			return false;

		EpicBackendServiceAccessToken otherEpicBackendServiceAccessToken = (EpicBackendServiceAccessToken) other;

		return Objects.equals(this.getAccessToken(), otherEpicBackendServiceAccessToken.getAccessToken())
				&& Objects.equals(this.getTokenType(), otherEpicBackendServiceAccessToken.getTokenType())
				&& Objects.equals(this.getExpiresAt(), otherEpicBackendServiceAccessToken.getExpiresAt())
				&& Objects.equals(this.getScope(), otherEpicBackendServiceAccessToken.getScope());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getAccessToken(), getTokenType(), getExpiresAt(), getScope());
	}

	@Nonnull
	public String getAccessToken() {
		return this.accessToken;
	}

	@Nonnull
	public String getTokenType() {
		return this.tokenType;
	}

	@Nonnull
	public Instant getExpiresAt() {
		return this.expiresAt;
	}

	@Nonnull
	public Optional<String> getScope() {
		return Optional.ofNullable(this.scope);
	}

	@NotThreadSafe
	public static class Builder {
		@Nonnull
		private final String accessToken;
		@Nonnull
		private final String tokenType;
		@Nonnull
		private final Instant expiresAt;
		@Nullable
		private String scope;

		public Builder(@Nonnull String accessToken,
									 @Nonnull String tokenType,
									 @Nonnull Instant expiresAt) {
			requireNonNull(accessToken);
			requireNonNull(tokenType);
			requireNonNull(expiresAt);

			this.accessToken = accessToken;
			this.tokenType = tokenType;
			this.expiresAt = expiresAt;
		}

		@Nonnull
		public Builder scope(@Nullable String scope) {
			this.scope = scope;
			return this;
		}

		@Nonnull
		public EpicBackendServiceAccessToken build() {
			return new EpicBackendServiceAccessToken(this);
		}
	}
}
