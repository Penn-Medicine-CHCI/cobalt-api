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
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
public class EpicConfiguration {
	@Nullable
	private final MyChartAccessToken myChartAccessToken;
	@Nullable
	private final EpicBackendServiceAccessToken epicBackendServiceAccessToken;
	@Nullable
	private final EpicEmpCredentials epicEmpCredentials;
	@Nonnull
	private final String clientId;
	@Nonnull
	private final String baseUrl;
	@Nonnull
	private final Boolean permitUnsafeCerts;

	protected EpicConfiguration(@Nonnull EpicConfiguration.Builder builder) {
		requireNonNull(builder);

		this.myChartAccessToken = builder.myChartAccessToken;
		this.epicEmpCredentials = builder.epicEmpCredentials;
		this.epicBackendServiceAccessToken = builder.epicBackendServiceAccessToken;
		this.clientId = builder.clientId;
		this.baseUrl = builder.baseUrl;
		this.permitUnsafeCerts = builder.permitUnsafeCerts == null ? false : builder.permitUnsafeCerts;
	}

	@Nonnull
	public Optional<MyChartAccessToken> getMyChartAccessToken() {
		return Optional.ofNullable(this.myChartAccessToken);
	}

	@Nonnull
	public Optional<EpicBackendServiceAccessToken> getEpicBackendServiceAccessToken() {
		return Optional.ofNullable(this.epicBackendServiceAccessToken);
	}

	@Nonnull
	public Optional<EpicEmpCredentials> getEpicEmpCredentials() {
		return Optional.ofNullable(this.epicEmpCredentials);
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

	@NotThreadSafe
	public static class Builder {
		@Nullable
		private MyChartAccessToken myChartAccessToken;
		@Nullable
		private EpicBackendServiceAccessToken epicBackendServiceAccessToken;
		@Nullable
		private EpicEmpCredentials epicEmpCredentials;
		@Nonnull
		private final String clientId;
		@Nonnull
		private final String baseUrl;
		@Nullable
		private Boolean permitUnsafeCerts;

		@Nonnull
		public Builder(@Nonnull EpicBackendServiceAccessToken epicBackendServiceAccessToken,
									 @Nonnull String clientId,
									 @Nonnull String baseUrl) {
			requireNonNull(epicBackendServiceAccessToken);
			requireNonNull(clientId);
			requireNonNull(baseUrl);

			this.epicBackendServiceAccessToken = epicBackendServiceAccessToken;
			this.clientId = clientId;
			this.baseUrl = baseUrl;
		}

		@Nonnull
		public Builder(@Nonnull EpicEmpCredentials epicEmpCredentials,
									 @Nonnull String clientId,
									 @Nonnull String baseUrl) {
			requireNonNull(epicEmpCredentials);
			requireNonNull(clientId);
			requireNonNull(baseUrl);

			this.epicEmpCredentials = epicEmpCredentials;
			this.clientId = clientId;
			this.baseUrl = baseUrl;
		}

		@Nonnull
		public Builder(@Nonnull MyChartAccessToken myChartAccessToken,
									 @Nonnull String clientId,
									 @Nonnull String baseUrl) {
			requireNonNull(myChartAccessToken);
			requireNonNull(clientId);
			requireNonNull(baseUrl);

			this.myChartAccessToken = myChartAccessToken;
			this.clientId = clientId;
			this.baseUrl = baseUrl;
		}

		@Nonnull
		public Builder permitUnsafeCerts(@Nullable Boolean permitUnsafeCerts) {
			this.permitUnsafeCerts = permitUnsafeCerts;
			return this;
		}

		@Nonnull
		public EpicConfiguration build() {
			return new EpicConfiguration(this);
		}
	}
}
