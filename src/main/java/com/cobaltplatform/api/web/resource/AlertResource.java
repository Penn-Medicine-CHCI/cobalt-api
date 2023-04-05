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
import com.cobaltplatform.api.model.api.request.CreateAlertDismissalRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AlertService;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class AlertResource {
	@Nonnull
	private final AlertService alertService;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public AlertResource(@Nonnull AlertService alertService,
											 @Nonnull RequestBodyParser requestBodyParser,
											 @Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(alertService);
		requireNonNull(requestBodyParser);
		requireNonNull(currentContextProvider);

		this.alertService = alertService;
		this.requestBodyParser = requestBodyParser;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@POST("/alerts/{alertId}/dismiss")
	@AuthenticationRequired
	public Object createAlertDismissal(@Nonnull @PathParameter UUID alertId) {
		requireNonNull(alertId);

		Account account = getCurrentContext().getAccount().get();

		CreateAlertDismissalRequest request = new CreateAlertDismissalRequest();
		request.setAlertId(alertId);
		request.setAccountId(account.getAccountId());

		getAlertService().createAlertDismissal(request);

		return new ApiResponse(); // 204
	}

	@Nonnull
	protected AlertService getAlertService() {
		return this.alertService;
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return this.requestBodyParser;
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
