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

package com.cobaltplatform.api.web.resource;

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AnalyticsService;
import com.cobaltplatform.api.service.AuthorizationService;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class AnalyticsResource {
	@Nonnull
	private final AnalyticsService analyticsService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public AnalyticsResource(@Nonnull AnalyticsService analyticsService,
													 @Nonnull AuthorizationService authorizationService,
													 @Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(analyticsService);
		requireNonNull(authorizationService);
		requireNonNull(currentContextProvider);

		this.analyticsService = analyticsService;
		this.authorizationService = authorizationService;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/analytics")
	@AuthenticationRequired
	public Object analytics() {
		// TODO: build out
		throw new UnsupportedOperationException();
	}

	@Nonnull
	protected AnalyticsService getAnalyticsService() {
		return this.analyticsService;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return this.authorizationService;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
