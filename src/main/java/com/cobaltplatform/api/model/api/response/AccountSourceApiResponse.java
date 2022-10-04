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
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
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
	@Nonnull
	private final String authenticationDescription;
	@Nonnull
	private final AccountSourceDisplayStyleId accountSourceDisplayStyleId;
	@Nullable
	private final String ssoUrl;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface AccountSourceApiResponseFactory {
		@Nonnull
		AccountSourceApiResponse create(@Nonnull AccountSourceForInstitution accountSource,
																		@Nonnull String environment);
	}

	@AssistedInject
	public AccountSourceApiResponse(@Nonnull Formatter formatter,
																	@Nonnull Strings strings,
																	@Assisted @Nonnull AccountSourceForInstitution accountSource,
																	@Assisted @Nonnull String environment) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(accountSource);

		this.accountSourceId = accountSource.getAccountSourceId();
		this.description = accountSource.getDescription();
		this.authenticationDescription = accountSource.getAuthenticationDescription();
		this.accountSourceDisplayStyleId = accountSource.getAccountSourceDisplayStyleId();
		this.ssoUrl = environment.equals("prod") ?
				accountSource.getProdSsoUrl() : environment.equals("dev") ?
				accountSource.getDevSsoUrl() : accountSource.getLocalSsoUrl();
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
}