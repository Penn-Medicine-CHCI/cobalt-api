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

package com.cobaltplatform.api.integration.enterprise;

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.ZoneId;
import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class EnterprisePluginProvider {
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;

	@Inject
	public EnterprisePluginProvider(@Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(currentContextProvider);
		this.currentContextProvider = currentContextProvider;
	}

	@Nonnull
	public EnterprisePlugin enterprisePluginForCurrentContext() {
		InstitutionId institutionId = InstitutionId.COBALT; // Default
		CurrentContext currentContext = getCurrentContext();
		Account account = currentContext.getAccount().orElse(null);

		if (account != null)
			institutionId = account.getInstitutionId();

		return enterprisePluginForInstitutionId(institutionId);
	}

	@Nonnull
	public EnterprisePlugin enterprisePluginForInstitutionId(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);

		return new CobaltEnterprisePlugin();
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		try {
			return currentContextProvider.get();
		} catch (Exception ignored) {
			return new CurrentContext.Builder(Locale.getDefault(), ZoneId.systemDefault()).build();
		}
	}
}
