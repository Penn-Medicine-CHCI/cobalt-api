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
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class EpicEmpCredentials {
	@Nonnull
	private final String clientId;
	@Nonnull
	private final String userId;
	@Nonnull
	private final String userIdType;
	@Nonnull
	private final String username;
	@Nonnull
	private final String password;

	public EpicEmpCredentials(@Nonnull String clientId,
														@Nonnull String userId,
														@Nonnull String userIdType,
														@Nonnull String username,
														@Nonnull String password) {
		requireNonNull(clientId);
		requireNonNull(userId);
		requireNonNull(userIdType);
		requireNonNull(username);
		requireNonNull(password);

		this.clientId = clientId;
		this.userId = userId;
		this.userIdType = userIdType;
		this.username = username;
		this.password = password;
	}

	@Override
	public String toString() {
		return format("%s{clientId=%s, userId=%s, userIdType=%s, username=%s}",
				getClass().getSimpleName(), getClientId(), getUserId(), getUserIdType(), getUsername());
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;

		if (other == null || !getClass().equals(other.getClass()))
			return false;

		EpicEmpCredentials otherEpicEmpCredentials = (EpicEmpCredentials) other;

		return Objects.equals(this.getClientId(), otherEpicEmpCredentials.getClientId())
				&& Objects.equals(this.getUserId(), otherEpicEmpCredentials.getUserId())
				&& Objects.equals(this.getUserIdType(), otherEpicEmpCredentials.getUserIdType())
				&& Objects.equals(this.getUsername(), otherEpicEmpCredentials.getUsername());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getClientId(), getUserId(), getUserIdType(), getUsername());
	}

	@Nonnull
	public String getClientId() {
		return this.clientId;
	}

	@Nonnull
	public String getUserId() {
		return this.userId;
	}

	@Nonnull
	public String getUserIdType() {
		return this.userIdType;
	}

	@Nonnull
	public String getUsername() {
		return this.username;
	}

	@Nonnull
	public String getPassword() {
		return this.password;
	}
}
