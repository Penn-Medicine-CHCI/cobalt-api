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

package com.cobaltplatform.api.integration.tableau;

import com.cobaltplatform.api.util.GsonUtility;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class TableauDirectTrustCredential {
	@Nonnull
	private static final Gson GSON;

	static {
		GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
		GsonUtility.applyDefaultTypeAdapters(gsonBuilder, TableauDirectTrustCredential.class);
		GSON = gsonBuilder.create();
	}

	@Nonnull
	private final String clientId;
	@Nullable
	private final String secretId;
	@Nonnull
	private final String secretValue;

	protected TableauDirectTrustCredential(@Nonnull String clientId,
																				 @Nonnull String secretId,
																				 @Nonnull String secretValue) {
		requireNonNull(clientId);
		requireNonNull(secretId);
		requireNonNull(secretValue);

		this.clientId = clientId;
		this.secretId = secretId;
		this.secretValue = secretValue;
	}

	@Nonnull
	public String serialize() {
		return GSON.toJson(this);
	}

	@Nonnull
	public static TableauDirectTrustCredential deserialize(@Nonnull String serializedTableauDirectTrustCredentials) {
		requireNonNull(serializedTableauDirectTrustCredentials);
		return GSON.fromJson(serializedTableauDirectTrustCredentials, TableauDirectTrustCredential.class);
	}

	@Override
	public String toString() {
		return format("%s{clientId=%s, secretId=%s, secretValue=%s}", getClass().getSimpleName(),
				getClientId(), getSecretId(), getSecretValue());
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;

		if (other == null || !getClass().equals(other.getClass()))
			return false;

		TableauDirectTrustCredential otherTableauDirectTrustCredential = (TableauDirectTrustCredential) other;
		return Objects.equals(this.getClientId(), otherTableauDirectTrustCredential.getClientId())
				&& Objects.equals(this.getSecretId(), otherTableauDirectTrustCredential.getSecretId())
				&& Objects.equals(this.getSecretValue(), otherTableauDirectTrustCredential.getSecretValue());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getClientId(), getSecretId(), getSecretValue());
	}

	@Nonnull
	public String getClientId() {
		return this.clientId;
	}

	@Nullable
	public String getSecretId() {
		return this.secretId;
	}

	@Nonnull
	public String getSecretValue() {
		return this.secretValue;
	}
}