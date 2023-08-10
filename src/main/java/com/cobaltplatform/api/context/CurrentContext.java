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


import com.cobaltplatform.api.integration.epic.MyChartAccessToken;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;
import com.cobaltplatform.api.model.security.AccessTokenStatus;
import com.cobaltplatform.api.model.service.AccountSourceForInstitution;
import com.cobaltplatform.api.model.service.RemoteClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class CurrentContext {
	@Nonnull
	private final Locale locale;
	@Nonnull
	private final ZoneId timeZone;
	@Nullable
	private final RemoteClient remoteClient;
	@Nullable
	private final String webappBaseUrl;
	@Nullable
	private final URL webappCurrentUrl;
	@Nullable
	private final String accessToken;
	@Nullable
	private final AccessTokenStatus accessTokenStatus;
	@Nonnull
	private final InstitutionId institutionId;
	@Nullable
	private final UserExperienceTypeId userExperienceTypeId;
	@Nullable
	private final Account account;
	@Nullable
	private final UUID sessionTrackingId;
	@Nullable
	private final AccountSourceForInstitution accountSource;
	@Nullable
	private final String fingerprintId;
	@Nullable
	private final MyChartAccessToken myChartAccessToken;

	public CurrentContext(@Nonnull Builder builder) {
		requireNonNull(builder);
		requireNonNull(builder.locale);
		requireNonNull(builder.timeZone);

		if (builder.account == null && builder.institutionId == null)
			throw new IllegalArgumentException("Either an account or institution must be provided.");

		this.locale = builder.locale;
		this.timeZone = builder.timeZone;
		this.account = builder.account;
		this.institutionId = builder.institutionId;
		this.userExperienceTypeId = builder.userExperienceTypeId;
		this.accessToken = trimToNull(builder.accessToken);
		this.accessTokenStatus = builder.accessTokenStatus;
		this.remoteClient = builder.remoteClient;
		this.webappBaseUrl = trimToNull(builder.webappBaseUrl);
		this.sessionTrackingId = builder.sessionTrackingId;
		this.accountSource = builder.accountSource;
		this.fingerprintId = builder.fingerprintId;
		this.myChartAccessToken = builder.myChartAccessToken;

		String webappCurrentUrl = trimToNull(builder.webappCurrentUrl);
		URL webappCurrentUrlAsUrl = null;

		if (webappCurrentUrl != null) {
			try {
				webappCurrentUrlAsUrl = new URL(webappCurrentUrl);
			} catch (MalformedURLException e) {
				// If we don't have a legal URL, just ignore it
			}
		}

		this.webappCurrentUrl = webappCurrentUrlAsUrl;
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
		return Optional.ofNullable(this.accessToken);
	}

	@Nonnull
	public Optional<AccessTokenStatus> getAccessTokenStatus() {
		return Optional.ofNullable(this.accessTokenStatus);
	}

	@Nonnull
	public Optional<Account> getAccount() {
		return Optional.ofNullable(this.account);
	}

	@Nonnull
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	@Nonnull
	public Optional<UserExperienceTypeId> getUserExperienceTypeId() {
		return Optional.ofNullable(this.userExperienceTypeId);
	}

	@Nonnull
	public Optional<RemoteClient> getRemoteClient() {
		return Optional.ofNullable(this.remoteClient);
	}

	@Nonnull
	public Optional<String> getWebappBaseUrl() {
		return Optional.ofNullable(this.webappBaseUrl);
	}

	@Nonnull
	public Optional<URL> getWebappCurrentUrl() {
		return Optional.ofNullable(this.webappCurrentUrl);
	}

	@Nonnull
	public Optional<UUID> getSessionTrackingId() {
		return Optional.ofNullable(this.sessionTrackingId);
	}

	@Nonnull
	public Optional<AccountSourceForInstitution> getAccountSource() {
		return Optional.ofNullable(this.accountSource);
	}

	@Nonnull
	public Optional<String> getFingerprintId() {
		return Optional.ofNullable(this.fingerprintId);
	}

	@Nonnull
	public Optional<MyChartAccessToken> getMyChartAccessToken() {
		return Optional.ofNullable(this.myChartAccessToken);
	}

	@NotThreadSafe
	public static class Builder {
		@Nonnull
		private final Locale locale;
		@Nonnull
		private final ZoneId timeZone;
		@Nullable
		private final Account account;
		@Nullable
		private final InstitutionId institutionId;
		@Nullable
		private UserExperienceTypeId userExperienceTypeId;
		@Nullable
		private String accessToken;
		@Nullable
		private AccessTokenStatus accessTokenStatus;
		@Nullable
		private RemoteClient remoteClient;
		@Nullable
		private String webappBaseUrl;
		@Nullable
		private String webappCurrentUrl;
		@Nullable
		private UUID sessionTrackingId;
		@Nullable
		private AccountSourceForInstitution accountSource;
		@Nullable
		private String fingerprintId;
		@Nullable
		private MyChartAccessToken myChartAccessToken;

		public Builder(@Nonnull Account account,
									 @Nonnull Locale locale,
									 @Nonnull ZoneId timeZone) {
			requireNonNull(account);
			requireNonNull(locale);
			requireNonNull(timeZone);

			this.account = account;
			this.institutionId = account.getInstitutionId();
			this.locale = locale;
			this.timeZone = timeZone;
		}

		public Builder(@Nonnull InstitutionId institutionId,
									 @Nonnull Locale locale,
									 @Nonnull ZoneId timeZone) {
			requireNonNull(institutionId);
			requireNonNull(locale);
			requireNonNull(timeZone);

			this.account = null;
			this.institutionId = institutionId;
			this.locale = locale;
			this.timeZone = timeZone;
		}

		@Nonnull
		public Builder userExperienceTypeId(@Nullable UserExperienceTypeId userExperienceTypeId) {
			this.userExperienceTypeId = userExperienceTypeId;
			return this;
		}

		@Nonnull
		public Builder accessToken(@Nullable String accessToken) {
			this.accessToken = accessToken;
			return this;
		}

		@Nonnull
		public Builder accessTokenStatus(@Nullable AccessTokenStatus accessTokenStatus) {
			this.accessTokenStatus = accessTokenStatus;
			return this;
		}

		@Nonnull
		public Builder remoteClient(@Nullable RemoteClient remoteClient) {
			this.remoteClient = remoteClient;
			return this;
		}

		@Nonnull
		public Builder sessionTrackingId(@Nullable UUID sessionTrackingId) {
			this.sessionTrackingId = sessionTrackingId;
			return this;
		}

		@Nonnull
		public Builder accountSource(@Nullable AccountSourceForInstitution accountSource) {
			this.accountSource = accountSource;
			return this;
		}

		@Nonnull
		public Builder fingerprintId(@Nullable String fingerprintId) {
			this.fingerprintId = fingerprintId;
			return this;
		}

		@Nonnull
		public Builder webappBaseUrl(@Nullable String webappBaseUrl) {
			this.webappBaseUrl = webappBaseUrl;
			return this;
		}

		@Nonnull
		public Builder webappCurrentUrl(@Nullable String webappCurrentUrl) {
			this.webappCurrentUrl = webappCurrentUrl;
			return this;
		}

		@Nonnull
		public Builder myChartAccessToken(@Nullable MyChartAccessToken myChartAccessToken) {
			this.myChartAccessToken = myChartAccessToken;
			return this;
		}

		@Nonnull
		public CurrentContext build() {
			return new CurrentContext(this);
		}
	}
}