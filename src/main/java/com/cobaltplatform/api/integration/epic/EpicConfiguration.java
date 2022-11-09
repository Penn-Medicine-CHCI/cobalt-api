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
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
public class EpicConfiguration {
	@Nonnull
	private final String clientId;
	@Nonnull
	private final String baseUrl;
	@Nonnull
	private final Boolean permitUnsafeCerts;
	@Nullable
	private final String userId;
	@Nullable
	private final String userIdType;
	@Nullable
	private final String username;
	@Nullable
	private final String password;
	@Nonnull
	private final Consumer<Map<String, String>> requestHeaderCustomizer;

	protected EpicConfiguration(@Nonnull EpicConfiguration.Builder builder) {
		requireNonNull(builder);

		this.clientId = builder.clientId;
		this.baseUrl = builder.baseUrl;
		this.permitUnsafeCerts = builder.permitUnsafeCerts == null ? false : builder.permitUnsafeCerts;
		this.userId = builder.userId;
		this.userIdType = builder.userIdType;
		this.username = builder.username;
		this.password = builder.password;
		this.requestHeaderCustomizer = builder.requestHeaderCustomizer == null ? (requestHeaders -> {
			// no-op
		}) : builder.requestHeaderCustomizer;
	}

	@Nonnull
	public String getClientId() {
		return this.clientId;
	}

	@Nonnull
	public String getBaseUrl() {
		return this.baseUrl;
	}

	@Nonnull
	public Boolean getPermitUnsafeCerts() {
		return this.permitUnsafeCerts;
	}

	@Nonnull
	public Optional<String> getUserId() {
		return Optional.ofNullable(this.userId);
	}

	@Nonnull
	public Optional<String> getUserIdType() {
		return Optional.ofNullable(this.userIdType);
	}

	@Nonnull
	public Optional<String> getUsername() {
		return Optional.ofNullable(this.username);
	}

	@Nonnull
	public Optional<String> getPassword() {
		return Optional.ofNullable(this.password);
	}

	@Nonnull
	public Consumer<Map<String, String>> getRequestHeaderCustomizer() {
		return this.requestHeaderCustomizer;
	}

	@NotThreadSafe
	public static class Builder {
		@Nonnull
		private final String clientId;
		@Nonnull
		private final String baseUrl;
		@Nullable
		private Boolean permitUnsafeCerts;
		@Nullable
		private String userId;
		@Nullable
		private String userIdType;
		@Nullable
		private String username;
		@Nullable
		private String password;
		@Nullable
		private Consumer<Map<String, String>> requestHeaderCustomizer;

		@Nonnull
		public Builder(@Nonnull String clientId,
									 @Nonnull String baseUrl) {
			requireNonNull(clientId);
			requireNonNull(baseUrl);

			this.clientId = clientId;
			this.baseUrl = baseUrl;
		}

		@Nonnull
		public Builder permitUnsafeCerts(@Nullable Boolean permitUnsafeCerts) {
			this.permitUnsafeCerts = permitUnsafeCerts;
			return this;
		}

		@Nonnull
		public Builder userId(@Nullable String userId) {
			this.userId = userId;
			return this;
		}

		@Nonnull
		public Builder userIdType(@Nullable String userIdType) {
			this.userIdType = userIdType;
			return this;
		}

		@Nonnull
		public Builder username(@Nullable String username) {
			this.username = username;
			return this;
		}

		@Nonnull
		public Builder password(@Nullable String password) {
			this.password = password;
			return this;
		}

		@Nonnull
		public Builder requestHeaderCustomizer(@Nullable Consumer<Map<String, String>> requestHeaderCustomizer) {
			this.requestHeaderCustomizer = requestHeaderCustomizer;
			return this;
		}

		@Nonnull
		public EpicConfiguration build() {
			return new EpicConfiguration(this);
		}
	}
}
