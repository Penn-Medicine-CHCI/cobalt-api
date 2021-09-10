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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;
import com.cobaltplatform.api.model.db.AccountSource;
import com.cobaltplatform.api.util.Formatter;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AccountSourceApiResponse {
	@Nullable
	private AccountSource.AccountSourceId accountSourceId;
	@Nullable
	private String description;
	@Nullable
	private String ssoUrl;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface AccountSourceApiResponseFactory {
		@Nonnull
		AccountSourceApiResponse create(@Nonnull AccountSource accountSource,
																		@NonNull String environment);
	}

	@AssistedInject
	public AccountSourceApiResponse(@Nonnull Formatter formatter,
																	@Nonnull Strings strings,
																	@Assisted @Nonnull AccountSource accountSource,
																	@Assisted @NonNull String environment) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(accountSource);

		this.accountSourceId = accountSource.getAccountSourceId();
		this.description = accountSource.getDescription();
		this.ssoUrl = environment.equals("prod") ?
				accountSource.getProdSsoUrl() : environment.equals("dev") ?
				accountSource.getDevSsoUrl() :accountSource.getLocalSsoUrl();
	}

	@Nullable
	public AccountSource.AccountSourceId getAccountSourceId() {
		return accountSourceId;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	@Nullable
	public String getSsoUrl() {
		return ssoUrl;
	}

}