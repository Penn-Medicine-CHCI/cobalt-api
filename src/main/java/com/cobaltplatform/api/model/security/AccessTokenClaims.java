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

package com.cobaltplatform.api.model.security;

import com.cobaltplatform.api.integration.epic.MyChartAccessToken;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class AccessTokenClaims {
	@Nonnull
	private final UUID accountId;
	@Nonnull
	private final Instant issuedAt;
	@Nonnull
	private final Instant expiration;
	@Nullable
	private MyChartAccessToken myChartAccessToken;

	public AccessTokenClaims(@Nonnull UUID accountId,
													 @Nonnull Instant issuedAt,
													 @Nonnull Instant expiration) {
		this(accountId, issuedAt, expiration, null);
	}

	public AccessTokenClaims(@Nonnull UUID accountId,
													 @Nonnull Instant issuedAt,
													 @Nonnull Instant expiration,
													 @Nullable MyChartAccessToken myChartAccessToken) {
		requireNonNull(accountId);
		requireNonNull(issuedAt);
		requireNonNull(expiration);

		this.accountId = accountId;
		this.issuedAt = issuedAt;
		this.expiration = expiration;
		this.myChartAccessToken = myChartAccessToken;
	}

	@Override
	public String toString() {
		return format("%s{accountId=%s, issuedAt=%s, expiration=%s, myChartAccessToken=%s}", getClass().getSimpleName(),
				getAccountId(), getIssuedAt(), getExpiration(), getMyChartAccessToken());
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;

		if (other == null || !getClass().equals(other.getClass()))
			return false;

		AccessTokenClaims otherAccessTokenClaims = (AccessTokenClaims) other;
		return Objects.equals(this.getAccountId(), otherAccessTokenClaims.getAccountId())
				&& Objects.equals(this.getIssuedAt(), otherAccessTokenClaims.getIssuedAt())
				&& Objects.equals(this.getExpiration(), otherAccessTokenClaims.getExpiration())
				&& Objects.equals(this.getMyChartAccessToken(), otherAccessTokenClaims.getMyChartAccessToken());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getAccountId(), getIssuedAt(), getExpiration(), getMyChartAccessToken());
	}

	@Nonnull
	public UUID getAccountId() {
		return accountId;
	}

	@Nonnull
	public Instant getIssuedAt() {
		return issuedAt;
	}

	@Nonnull
	public Instant getExpiration() {
		return expiration;
	}

	@Nonnull
	public Optional<MyChartAccessToken> getMyChartAccessToken() {
		return Optional.ofNullable(this.myChartAccessToken);
	}
}