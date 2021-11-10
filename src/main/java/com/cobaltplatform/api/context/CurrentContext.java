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

package com.cobaltplatform.api.context;


import com.cobaltplatform.api.model.client.RemoteClient;
import com.cobaltplatform.api.model.db.Account;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class CurrentContext {
	@Nonnull
	private final Locale locale;
	@Nonnull
	private final ZoneId timeZone;
	@Nonnull
	private final Boolean signedByIc;
	@Nullable
	private final RemoteClient remoteClient;
	@Nullable
	private String accessToken;
	@Nullable
	private Account account;
	@Nonnull
	private final UUID sessionTrackingId;

	public CurrentContext(@Nonnull CurrentContext.Builder builder) {
		requireNonNull(builder);
		requireNonNull(builder.locale);
		requireNonNull(builder.timeZone);

		this.locale = builder.locale;
		this.timeZone = builder.timeZone;
		this.account = builder.account;
		this.accessToken = builder.accessToken;
		this.remoteClient = builder.remoteClient;
		this.sessionTrackingId = builder.sessionTrackingId;
		this.signedByIc = builder.signedByIc == null ? false : builder.signedByIc;
	}

	@Nonnull
	public Locale getLocale() {
		return this.locale;
	}

	@Nonnull
	public ZoneId getTimeZone() {
		return this.timeZone;
	}

	@Nonnull
	public Optional<String> getAccessToken() {
		return Optional.ofNullable(accessToken);
	}

	@Nonnull
	public Optional<Account> getAccount() {
		return Optional.ofNullable(account);
	}

	@Nullable
	public Optional<RemoteClient> getRemoteClient() {
		return Optional.ofNullable(remoteClient);
	}

	@Nonnull
	public Boolean getSignedByIc() {
		return signedByIc;
	}

	@Nonnull
	public UUID getSessionTrackingId() {
		return sessionTrackingId;
	}

	@NotThreadSafe
	public static class Builder {
		@Nonnull
		private final Locale locale;
		@Nonnull
		private final ZoneId timeZone;
		@Nullable
		private String accessToken;
		@Nullable
		private Account account;
		@Nullable
		private RemoteClient remoteClient;
		@Nullable
		private UUID sessionTrackingId;
		private Boolean signedByIc;

		public Builder(@Nonnull Locale locale, @Nonnull ZoneId timeZone) {
			requireNonNull(locale);
			requireNonNull(timeZone);

			this.locale = locale;
			this.timeZone = timeZone;
		}

		@Nonnull
		public Builder accessToken(@Nullable String accessToken) {
			this.accessToken = accessToken;
			return this;
		}

		@Nonnull
		public Builder account(@Nullable Account account) {
			this.account = account;
			return this;
		}

		@Nonnull
		public Builder remoteClient(@Nullable RemoteClient remoteClient) {
			this.remoteClient = remoteClient;
			return this;
		}

		@Nonnull
		public Builder signedByIc(@Nullable Boolean signedByIc) {
			this.signedByIc = signedByIc;
			return this;
		}

		@Nonnull
		public Builder sessionTrackingId(@Nullable UUID sessionTrackingId) {
			this.sessionTrackingId = sessionTrackingId;
			return this;
		}

		@Nonnull
		public CurrentContext build() {
			return new CurrentContext(this);
		}
	}
}