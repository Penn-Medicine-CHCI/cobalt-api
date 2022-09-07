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

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.google.common.base.CaseFormat;
import com.google.inject.Injector;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class EnterprisePluginProvider {
	@Nonnull
	private final Injector injector;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Map<InstitutionId, Class<? extends EnterprisePlugin>> enterprisePluginClassesByInstitutionId;

	@Inject
	public EnterprisePluginProvider(@Nonnull Injector injector,
																	@Nonnull Configuration configuration,
																	@Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(injector);
		requireNonNull(configuration);
		requireNonNull(currentContextProvider);

		this.injector = injector;
		this.configuration = configuration;
		this.currentContextProvider = currentContextProvider;
		this.enterprisePluginClassesByInstitutionId = Collections.unmodifiableMap(createEnterprisePluginClassesByInstitutionId());
	}

	@Nonnull
	public EnterprisePlugin enterprisePluginForCurrentInstitution() {
		return enterprisePluginForInstitutionId(getCurrentContext().getInstitutionId());
	}

	@Nonnull
	public EnterprisePlugin enterprisePluginForInstitutionId(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);

		try {
			Class<? extends EnterprisePlugin> enterprisePluginClass = getEnterprisePluginClassesByInstitutionId().get(institutionId);
			return getInjector().getInstance(enterprisePluginClass);
		} catch (Exception e) {
			throw new RuntimeException(format("Unable to load enterprise plugin for institution ID %s.", institutionId), e);
		}
	}

	@Nonnull
	protected Map<InstitutionId, Class<? extends EnterprisePlugin>> createEnterprisePluginClassesByInstitutionId() {
		Map<InstitutionId, Class<? extends EnterprisePlugin>> enterprisePluginClassesByInstitutionId = new HashMap<>();

		// Magic: figure out institution plugin class names based on Institution IDs.
		// For example, turns institution ID "EXAMPLE_INSTITUTION" into "ExampleInstitutionEnterprisePlugin".
		// This permits us to avoid merge conflicts w/enterprise repo and we normally don't need extra flexibility around plugin loading.
		for (InstitutionId institutionId : InstitutionId.values()) {
			// e.g. COBALT_EXAMPLE -> CobaltExampleEnterprisePlugin
			String className = format("%s%s", CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, institutionId.name()),
					EnterprisePlugin.class.getSimpleName());
			// Assumes plugins are in the same package as this class
			String packageName = EnterprisePluginProvider.class.getPackageName();
			String fullyQualifiedClassName = format("%s.%s", packageName, className);

			try {
				// This line appears unsafe, but we do a check below to confirm it's kosher
				Class<? extends EnterprisePlugin> enterprisePluginClass = (Class<? extends EnterprisePlugin>) Class.forName(fullyQualifiedClassName);

				if (!Arrays.stream(enterprisePluginClass.getInterfaces()).toList().contains(EnterprisePlugin.class))
					throw new IllegalStateException(format("Plugin class %s must be modified to implement the %s interface.",
							fullyQualifiedClassName, EnterprisePlugin.class));

				enterprisePluginClassesByInstitutionId.put(institutionId, enterprisePluginClass);
			} catch (Exception e) {
				throw new IllegalStateException(format("Unable to load enterprise plugin class for name %s", fullyQualifiedClassName), e);
			}
		}

		return enterprisePluginClassesByInstitutionId;
	}

	@Nonnull
	protected Injector getInjector() {
		return this.injector;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected Map<InstitutionId, Class<? extends EnterprisePlugin>> getEnterprisePluginClassesByInstitutionId() {
		return this.enterprisePluginClassesByInstitutionId;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		try {
			return currentContextProvider.get();
		} catch (Exception ignored) {
			return new CurrentContext.Builder(InstitutionId.COBALT, getConfiguration().getDefaultLocale(),
					getConfiguration().getDefaultTimeZone()).build();
		}
	}
}
