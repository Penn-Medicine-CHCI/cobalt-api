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
import com.cobaltplatform.api.model.api.request.SendCallMessagesRequest;
import com.cobaltplatform.api.model.api.request.SendSmsMessagesRequest;
import com.cobaltplatform.api.model.security.IcSignedRequestRequired;
import com.cobaltplatform.api.service.MessageService;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import static java.util.Objects.requireNonNull;


/**
 * @author Transmogrify LLC.
 */

@Resource
@Singleton
@ThreadSafe
public class MessageResource {
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final MessageService messageService;
	@Nonnull
	private final RequestBodyParser requestBodyParser;

	@Inject
	public MessageResource(@Nonnull Provider<CurrentContext> currentContextProvider,
												 @Nonnull MessageService messageService,
												 @Nonnull RequestBodyParser requestBodyParser) {
		requireNonNull(currentContextProvider);
		requireNonNull(messageService);
		requireNonNull(requestBodyParser);

		this.currentContextProvider = currentContextProvider;
		this.messageService = messageService;
		this.requestBodyParser = requestBodyParser;
	}

	@IcSignedRequestRequired
	@POST("/messages/sms/send")
	public void sendSmsMessages(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		SendSmsMessagesRequest request = getRequestBodyParser().parse(requestBody, SendSmsMessagesRequest.class);
		getMessageService().sendSmsMessages(request);
	}

	@IcSignedRequestRequired
	@POST("/messages/call/send")
	public void sendCallMessages(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		SendCallMessagesRequest request = getRequestBodyParser().parse(requestBody, SendCallMessagesRequest.class);
		getMessageService().sendCallMessages(request);
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}

	@Nonnull
	protected MessageService getMessageService() {
		return messageService;
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return requestBodyParser;
	}
}
