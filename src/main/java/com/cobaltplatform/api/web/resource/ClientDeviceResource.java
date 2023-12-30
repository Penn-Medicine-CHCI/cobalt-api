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
import com.cobaltplatform.api.model.api.request.TestClientDevicePushMessageRequest;
import com.cobaltplatform.api.model.api.request.UpsertClientDevicePushTokenRequest;
import com.cobaltplatform.api.model.api.request.UpsertClientDeviceRequest;
import com.cobaltplatform.api.model.api.response.ClientDeviceApiResponse.ClientDeviceApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ClientDevicePushTokenApiResponse.ClientDevicePushTokenApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.ClientDevice;
import com.cobaltplatform.api.model.db.ClientDevicePushToken;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.RemoteClient;
import com.cobaltplatform.api.service.ClientDeviceService;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class ClientDeviceResource {
	@Nonnull
	private final ClientDeviceService clientDeviceService;
	@Nonnull
	private final ClientDeviceApiResponseFactory clientDeviceApiResponseFactory;
	@Nonnull
	private final ClientDevicePushTokenApiResponseFactory clientDevicePushTokenApiResponseFactory;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public ClientDeviceResource(@Nonnull ClientDeviceService clientDeviceService,
															@Nonnull ClientDeviceApiResponseFactory clientDeviceApiResponseFactory,
															@Nonnull ClientDevicePushTokenApiResponseFactory clientDevicePushTokenApiResponseFactory,
															@Nonnull RequestBodyParser requestBodyParser,
															@Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(clientDeviceService);
		requireNonNull(clientDeviceApiResponseFactory);
		requireNonNull(clientDevicePushTokenApiResponseFactory);
		requireNonNull(requestBodyParser);
		requireNonNull(currentContextProvider);

		this.clientDeviceService = clientDeviceService;
		this.clientDeviceApiResponseFactory = clientDeviceApiResponseFactory;
		this.clientDevicePushTokenApiResponseFactory = clientDevicePushTokenApiResponseFactory;
		this.requestBodyParser = requestBodyParser;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@POST("/client-devices")
	@AuthenticationRequired
	public ApiResponse upsertClientDevice(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();
		UpsertClientDeviceRequest request = getRequestBodyParser().parse(requestBody, UpsertClientDeviceRequest.class);
		request.setAccountId(account.getAccountId());

		UUID clientDeviceId = getClientDeviceService().upsertClientDevice(request);
		ClientDevice clientDevice = getClientDeviceService().findClientDeviceById(clientDeviceId).get();

		return new ApiResponse(Map.of(
				"clientDevice", getClientDeviceApiResponseFactory().create(clientDevice)
		));
	}

	@Nonnull
	@POST("/client-device-push-tokens")
	@AuthenticationRequired
	public ApiResponse upsertClientDevicePushToken(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();
		RemoteClient remoteClient = getCurrentContext().getRemoteClient().orElse(null);

		UpsertClientDevicePushTokenRequest request = getRequestBodyParser().parse(requestBody, UpsertClientDevicePushTokenRequest.class);
		request.setAccountId(account.getAccountId());

		// Fill in field from client (here, X-Client-Device-Fingerprint header) if not explicitly specified in request body.
		// Lets clients say "register for the current device, for which you already have fingerprint data"
		if (remoteClient != null && request.getFingerprint() == null)
			request.setFingerprint(remoteClient.getFingerprint().orElse(null));

		UUID clientDevicePushTokenId = getClientDeviceService().upsertClientDevicePushToken(request);
		ClientDevicePushToken clientDevicePushToken = getClientDeviceService().findClientDevicePushTokenById(clientDevicePushTokenId).get();

		return new ApiResponse(Map.of(
				"clientDevicePushToken", getClientDevicePushTokenApiResponseFactory().create(clientDevicePushToken)
		));
	}

	@Nonnull
	@POST("/client-device-push-tokens/test")
	@AuthenticationRequired
	public ApiResponse testClientDevicePushMessage(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();
		TestClientDevicePushMessageRequest request = getRequestBodyParser().parse(requestBody, TestClientDevicePushMessageRequest.class);
		request.setAccountId(account.getAccountId());

		String messageId = getClientDeviceService().testClientDevicePushMessage(request);

		return new ApiResponse(Map.of(
				"messageId", messageId
		));
	}

	@Nonnull
	protected ClientDeviceService getClientDeviceService() {
		return this.clientDeviceService;
	}

	@Nonnull
	protected ClientDeviceApiResponseFactory getClientDeviceApiResponseFactory() {
		return this.clientDeviceApiResponseFactory;
	}

	@Nonnull
	protected ClientDevicePushTokenApiResponseFactory getClientDevicePushTokenApiResponseFactory() {
		return this.clientDevicePushTokenApiResponseFactory;
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
