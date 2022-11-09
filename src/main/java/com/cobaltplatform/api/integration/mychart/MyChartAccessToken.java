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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class MyChartAccessToken {
	@Nonnull
	private static final Gson GSON;

	static {
		GsonBuilder gsonBuilder = new GsonBuilder().disableHtmlEscaping();
		GsonUtility.applyDefaultTypeAdapters(gsonBuilder, MyChartAccessToken.class);
		GSON = gsonBuilder.create();
	}

	@Nonnull
	private final String accessToken;
	@Nonnull
	private final String tokenType;
	@Nonnull
	private final Instant expiresAt;
	@Nonnull
	private final String state;
	@Nullable
	private final String scope;
	@Nullable
	private final String refreshToken;
	@Nonnull
	private final Map<String, Object> metadata;

	protected MyChartAccessToken(@Nonnull Builder builder) {
		requireNonNull(builder);

		this.accessToken = builder.accessToken;
		this.tokenType = builder.tokenType;
		this.expiresAt = builder.expiresAt;
		this.state = builder.state;
		this.scope = builder.scope;
		this.refreshToken = builder.refreshToken;
		this.metadata = builder.metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(builder.metadata));
	}

	@Nonnull
	public String serialize() {
		String json = GSON.toJson(this);
		return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}

	@Nonnull
	public static MyChartAccessToken deserialize(@Nonnull String serializedMyChartAccessToken) {
		requireNonNull(serializedMyChartAccessToken);

		String json = new String(Base64.getDecoder().decode(serializedMyChartAccessToken), StandardCharsets.UTF_8);
		return GSON.fromJson(json, MyChartAccessToken.class);
	}

	@Override
	public String toString() {
		return format("%s{accessToken=%s, tokenType=%s, expiresAt=%s, scope=%s, refreshToken=%s, state=%s, metadata=%s}",
				getClass().getSimpleName(), getAccessToken(), getTokenType(), getExpiresAt(),
				getScope(), getRefreshToken(), getState(), getMetadata());
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;

		if (other == null || !getClass().equals(other.getClass()))
			return false;

		MyChartAccessToken otherMyChartAccessToken = (MyChartAccessToken) other;
		return Objects.equals(this.getAccessToken(), otherMyChartAccessToken.getAccessToken())
				&& Objects.equals(this.getTokenType(), otherMyChartAccessToken.getTokenType())
				&& Objects.equals(this.getExpiresAt(), otherMyChartAccessToken.getExpiresAt())
				&& Objects.equals(this.getState(), otherMyChartAccessToken.getState())
				&& Objects.equals(this.getScope(), otherMyChartAccessToken.getScope())
				&& Objects.equals(this.getRefreshToken(), otherMyChartAccessToken.getRefreshToken());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getAccessToken(), getTokenType(), getExpiresAt(), getState(), getScope(), getRefreshToken());
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

	@Nonnull
	public Map<String, Object> getMetadata() {
		return this.metadata;
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
		private String state;
		@Nullable
		private String scope;
		@Nullable
		private String refreshToken;
		@Nullable
		private Map<String, Object> metadata;

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
		public Builder metadata(@Nullable Map<String, Object> metadata) {
			this.metadata = metadata;
			return this;
		}

		@Nonnull
		public MyChartAccessToken build() {
			return new MyChartAccessToken(this);
		}
	}
}
