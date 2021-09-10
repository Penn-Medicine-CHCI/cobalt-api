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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class SigningTokenClaims {
	@Nonnull
	private final Map<String, Object> claims;
	@Nonnull
	private final Instant expiration;

	public SigningTokenClaims(@Nonnull Map<String, Object> claims,
														@Nonnull Instant expiration) {
		requireNonNull(claims);
		requireNonNull(expiration);

		this.claims = Collections.unmodifiableMap(claims);
		this.expiration = expiration;
	}

	@Override
	public String toString() {
		return format("%s{claims=%s, expiration=%s}", getClass().getSimpleName(), getClaims(), getExpiration());
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;

		if (other == null || !getClass().equals(other.getClass()))
			return false;

		SigningTokenClaims otherSigningTokenClaims = (SigningTokenClaims) other;
		return Objects.equals(this.getClaims(), otherSigningTokenClaims.getClaims())
				&& Objects.equals(this.getExpiration(), otherSigningTokenClaims.getExpiration());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getClaims(), getExpiration());
	}

	@Nonnull
	public Map<String, Object> getClaims() {
		return claims;
	}

	@Nonnull
	public Instant getExpiration() {
		return expiration;
	}
}