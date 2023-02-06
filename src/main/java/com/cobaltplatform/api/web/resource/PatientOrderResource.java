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
import com.cobaltplatform.api.model.api.request.CreatePatientOrderImportRequest;
import com.cobaltplatform.api.model.api.response.PatientOrderApiResponse.PatientOrderApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.PatientOrderImportType.PatientOrderImportTypeId;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.PatientOrderService;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class PatientOrderResource {
	@Nonnull
	private final PatientOrderService patientOrderService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final PatientOrderApiResponseFactory patientOrderApiResponseFactory;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public PatientOrderResource(@Nonnull PatientOrderService patientOrderService,
															@Nonnull AuthorizationService authorizationService,
															@Nonnull PatientOrderApiResponseFactory patientOrderApiResponseFactory,
															@Nonnull RequestBodyParser requestBodyParser,
															@Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(patientOrderService);
		requireNonNull(authorizationService);
		requireNonNull(patientOrderApiResponseFactory);
		requireNonNull(requestBodyParser);
		requireNonNull(currentContextProvider);

		this.patientOrderService = patientOrderService;
		this.authorizationService = authorizationService;
		this.patientOrderApiResponseFactory = patientOrderApiResponseFactory;
		this.requestBodyParser = requestBodyParser;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/patient-orders/{patientOrderId}")
	@AuthenticationRequired
	public ApiResponse patientOrder(@Nonnull @PathParameter UUID patientOrderId) {
		requireNonNull(patientOrderId);

		Account account = getCurrentContext().getAccount().get();
		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canViewPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrder", getPatientOrderApiResponseFactory().create(patientOrder));
		}});
	}

	@Nonnull
	@POST("/patient-order-imports")
	@AuthenticationRequired
	public ApiResponse createPatientOrderImport(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreatePatientOrderImportRequest request = getRequestBodyParser().parse(requestBody, CreatePatientOrderImportRequest.class);
		request.setInstitutionId(account.getInstitutionId());
		request.setAccountId(account.getAccountId());
		request.setPatientOrderImportTypeId(PatientOrderImportTypeId.CSV);

		getPatientOrderService().createPatientOrderImport(request);

		return new ApiResponse();
	}

	@Nonnull
	protected PatientOrderService getPatientOrderService() {
		return this.patientOrderService;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return this.authorizationService;
	}

	@Nonnull
	protected PatientOrderApiResponseFactory getPatientOrderApiResponseFactory() {
		return this.patientOrderApiResponseFactory;
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
