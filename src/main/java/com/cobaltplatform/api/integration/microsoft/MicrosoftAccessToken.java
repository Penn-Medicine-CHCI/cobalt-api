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

package com.cobaltplatform.api.integration.microsoft;

import com.cobaltplatform.api.util.GsonUtility;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class MicrosoftAccessToken {
	@Nonnull
	private static final Gson GSON;

	static {
		GsonBuilder gsonBuilder = new GsonBuilder().disableHtmlEscaping();
		GsonUtility.applyDefaultTypeAdapters(gsonBuilder, MicrosoftAccessToken.class);
		GSON = gsonBuilder.create();
	}

	@Nonnull
	private final String accessToken;
	@Nullable
	private final String idToken;
	@Nonnull
	private final String tokenType;
	@Nonnull
	private final Instant expiresAt;
	@Nullable
	private final Instant extExpiresAt;
	@Nonnull
	private final String state;
	@Nullable
	private final String scope;
	@Nullable
	private final String refreshToken;

	protected MicrosoftAccessToken(@Nonnull Builder builder) {
		requireNonNull(builder);

		this.accessToken = builder.accessToken;
		this.idToken = builder.idToken;
		this.tokenType = builder.tokenType;
		this.expiresAt = builder.expiresAt;
		this.extExpiresAt = builder.extExpiresAt;
		this.state = builder.state;
		this.scope = builder.scope;
		this.refreshToken = builder.refreshToken;
	}

	@Nonnull
	public String serialize() {
		String json = GSON.toJson(this);
		return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}

	@Nonnull
	public static MicrosoftAccessToken deserialize(@Nonnull String serializedMicrosoftAccessToken) {
		requireNonNull(serializedMicrosoftAccessToken);

		String json = new String(Base64.getDecoder().decode(serializedMicrosoftAccessToken), StandardCharsets.UTF_8);
		return GSON.fromJson(json, MicrosoftAccessToken.class);
	}

	@Override
	public String toString() {
		return format("%s{accessToken=%s, idToken=%s, tokenType=%s, expiresAt=%s, extExpiresAt=%s, scope=%s, refreshToken=%s, state=%s}",
				getClass().getSimpleName(), getAccessToken(), getIdToken(), getTokenType(), getExpiresAt(), getExtExpiresAt(),
				getScope(), getRefreshToken(), getState());
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;

		if (other == null || !getClass().equals(other.getClass()))
			return false;

		MicrosoftAccessToken otherMicrosoftAccessToken = (MicrosoftAccessToken) other;
		return Objects.equals(this.getAccessToken(), otherMicrosoftAccessToken.getAccessToken())
				&& Objects.equals(this.getIdToken(), otherMicrosoftAccessToken.getIdToken())
				&& Objects.equals(this.getTokenType(), otherMicrosoftAccessToken.getTokenType())
				&& Objects.equals(this.getExpiresAt(), otherMicrosoftAccessToken.getExpiresAt())
				&& Objects.equals(this.getExtExpiresAt(), otherMicrosoftAccessToken.getExtExpiresAt())
				&& Objects.equals(this.getState(), otherMicrosoftAccessToken.getState())
				&& Objects.equals(this.getScope(), otherMicrosoftAccessToken.getScope())
				&& Objects.equals(this.getRefreshToken(), otherMicrosoftAccessToken.getRefreshToken());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getAccessToken(), getIdToken(), getTokenType(), getExpiresAt(), getExtExpiresAt(), getState(), getScope(), getRefreshToken());
	}

	@Nonnull
	public String getAccessToken() {
		return this.accessToken;
	}

	@Nonnull
	public Optional<String> getIdToken() {
		return Optional.ofNullable(this.idToken);
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
	public Optional<Instant> getExtExpiresAt() {
		return Optional.ofNullable(this.extExpiresAt);
	}

	@Nonnull
	public Optional<String> getState() {
		return Optional.ofNullable(this.state);
	}

	@Nonnull
	public Optional<String> getScope() {
		return Optional.ofNullable(this.scope);
	}

	@Nonnull
	public Optional<String> getRefreshToken() {
		return Optional.ofNullable(this.refreshToken);
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
		private Instant extExpiresAt;
		@Nullable
		private String state;
		@Nullable
		private String scope;
		@Nullable
		private String refreshToken;
		@Nullable
		private String idToken;

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
		public Builder state(@Nullable String state) {
			this.state = state;
			return this;
		}

		@Nonnull
		public Builder scope(@Nullable String scope) {
			this.scope = scope;
			return this;
		}

		@Nonnull
		public Builder refreshToken(@Nullable String refreshToken) {
			this.refreshToken = refreshToken;
			return this;
		}

		@Nonnull
		public Builder idToken(@Nullable String idToken) {
			this.idToken = idToken;
			return this;
		}

		@Nonnull
		public Builder extExpiresAt(@Nullable Instant extExpiresAt) {
			this.extExpiresAt = extExpiresAt;
			return this;
		}

		@Nonnull
		public MicrosoftAccessToken build() {
			return new MicrosoftAccessToken(this);
		}
	}
}
