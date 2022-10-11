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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.Instant;
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
	private final String accessToken;
	@Nonnull
	private final String tokenType;
	@Nonnull
	private final Instant expiresAt;
	@Nonnull
	private final String refreshToken;
	@Nullable
	private final String scope;

	public MyChartAccessToken(@Nonnull String accessToken,
														@Nonnull String tokenType,
														@Nonnull Instant expiresAt,
														@Nonnull String refreshToken) {
		this(accessToken, tokenType, expiresAt, refreshToken, null);
	}

	public MyChartAccessToken(@Nonnull String accessToken,
														@Nonnull String tokenType,
														@Nonnull Instant expiresAt,
														@Nonnull String refreshToken,
														@Nullable String scope) {
		requireNonNull(accessToken);
		requireNonNull(tokenType);
		requireNonNull(expiresAt);
		requireNonNull(refreshToken);

		this.accessToken = accessToken;
		this.tokenType = tokenType;
		this.expiresAt = expiresAt;
		this.refreshToken = refreshToken;
		this.scope = scope;
	}

	@Override
	public String toString() {
		return format("%s{accessToken=%s, tokenType=%s, expiresAt=%s, refreshToken=%s, scope=%s}",
				getClass().getSimpleName(), getAccessToken(), getTokenType(), getExpiresAt(),
				getRefreshToken(), getScope());
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
				&& Objects.equals(this.getRefreshToken(), otherMyChartAccessToken.getRefreshToken())
				&& Objects.equals(this.getScope(), otherMyChartAccessToken.getScope());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getAccessToken(), getTokenType(), getExpiresAt(), getRefreshToken(), getScope());
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
	public String getRefreshToken() {
		return this.refreshToken;
	}

	@Nonnull
	public Optional<String> getScope() {
		return Optional.ofNullable(this.scope);
	}
}
