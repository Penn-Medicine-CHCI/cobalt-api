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

package com.cobaltplatform.api.model.db;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.security.PublicKey;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PublicKeyFormat {
	@Nullable
	private PublicKeyFormatId publicKeyFormatId;
	@Nullable
	private String description;

	public enum PublicKeyFormatId {
		X509;

		@Nonnull
		public static Optional<PublicKeyFormatId> fromPublicKey(@Nonnull PublicKey publicKey) {
			requireNonNull(publicKey);

			if ("X.509".equals(publicKey.getFormat()))
				return Optional.of(PublicKeyFormatId.X509);

			return Optional.empty();
		}
	}

	@Override
	public String toString() {
		return format("%s{publicKeyFormatId=%s, description=%s}", getClass().getSimpleName(),
				getPublicKeyFormatId(), getDescription());
	}

	@Nullable
	public PublicKeyFormatId getPublicKeyFormatId() {
		return this.publicKeyFormatId;
	}

	public void setPublicKeyFormatId(@Nullable PublicKeyFormatId publicKeyFormatId) {
		this.publicKeyFormatId = publicKeyFormatId;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}
}