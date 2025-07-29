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

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.AccountSourceDisplayStyle.AccountSourceDisplayStyleId;
import com.cobaltplatform.api.model.service.AccountSourceForInstitution;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.WebUtility;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AccountSourceApiResponse {
	@Nonnull
	private final AccountSourceId accountSourceId;
	@Nonnull
	private final String description;
	@Nullable
	private final String shortDescription;
	@Nonnull
	private final String authenticationDescription;
	@Nonnull
	private final AccountSourceDisplayStyleId accountSourceDisplayStyleId;
	@Nullable
	private final String ssoUrl;
	@Nullable
	private final String supplementMessage;
	@Nullable
	private final String supplementMessageStyle;
	@Nonnull
	private final Boolean visible;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface AccountSourceApiResponseFactory {
		@Nonnull
		AccountSourceApiResponse create(@Nonnull AccountSourceForInstitution accountSource,
																		@Nonnull String environment);

		@Nonnull
		AccountSourceApiResponse create(@Nonnull AccountSourceForInstitution accountSource,
																		@Nonnull String environment,
																		@Nonnull Map<String, String> additionalSsoUrlQueryParameters);
	}

	@AssistedInject
	public AccountSourceApiResponse(@Nonnull Formatter formatter,
																	@Nonnull Strings strings,
																	@Assisted @Nonnull AccountSourceForInstitution accountSource,
																	@Assisted @Nonnull String environment) {
		this(formatter, strings, accountSource, environment, Map.of());
	}

	@AssistedInject
	public AccountSourceApiResponse(@Nonnull Formatter formatter,
																	@Nonnull Strings strings,
																	@Assisted @Nonnull AccountSourceForInstitution accountSource,
																	@Assisted @Nonnull String environment,
																	@Assisted @Nonnull Map<String, String> additionalSsoUrlQueryParameters) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(accountSource);
		requireNonNull(additionalSsoUrlQueryParameters);

		this.accountSourceId = accountSource.getAccountSourceId();
		this.description = accountSource.getDescription();
		this.shortDescription = accountSource.getShortDescription();
		this.authenticationDescription = accountSource.getAuthenticationDescription();
		this.accountSourceDisplayStyleId = accountSource.getAccountSourceDisplayStyleId();
		this.supplementMessage = accountSource.getSupplementMessage();
		this.supplementMessageStyle = accountSource.getSupplementMessageStyle();
		this.visible = accountSource.getVisible();

		String ssoUrl = null;

		if (environment.endsWith("prod"))
			ssoUrl = accountSource.getProdSsoUrl();
		else if (environment.endsWith("dev"))
			ssoUrl = accountSource.getDevSsoUrl();
		else if (environment.equals("local"))
			ssoUrl = accountSource.getLocalSsoUrl();

		// Tack on any additional query parameters, if provided
		if (ssoUrl != null && !additionalSsoUrlQueryParameters.isEmpty())
			ssoUrl = WebUtility.appendQueryParameters(ssoUrl, additionalSsoUrlQueryParameters);

		this.ssoUrl = ssoUrl;
	}

	@Nonnull
	public AccountSourceId getAccountSourceId() {
		return this.accountSourceId;
	}

	@Nonnull
	public String getDescription() {
		return this.description;
	}

	@Nonnull
	public Optional<String> getShortDescription() {
		return Optional.ofNullable(this.shortDescription);
	}

	@Nonnull
	public String getAuthenticationDescription() {
		return this.authenticationDescription;
	}

	@Nonnull
	public AccountSourceDisplayStyleId getAccountSourceDisplayStyleId() {
		return this.accountSourceDisplayStyleId;
	}

	@Nonnull
	public Optional<String> getSsoUrl() {
		return Optional.ofNullable(ssoUrl);
	}

	@Nonnull
	public Optional<String> getSupplementMessage() {
		return Optional.ofNullable(this.supplementMessage);
	}

	@Nonnull
	public Optional<String> getSupplementMessageStyle() {
		return Optional.ofNullable(this.supplementMessageStyle);
	}

	@Nonnull
	public Boolean getVisible() {
		return this.visible;
	}
}