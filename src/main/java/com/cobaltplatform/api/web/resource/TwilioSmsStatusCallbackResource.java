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

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.integration.twilio.TwilioRequestBody;
import com.cobaltplatform.api.integration.twilio.TwilioRequestValidator;
import com.cobaltplatform.api.model.db.MessageLog;
import com.cobaltplatform.api.model.db.MessageVendor.MessageVendorId;
import com.cobaltplatform.api.service.MessageService;
import com.cobaltplatform.api.util.WebUtility;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.RequestHeader;
import com.soklet.web.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class TwilioSmsStatusCallbackResource {
	@Nonnull
	private final MessageService messageService;
	@Nonnull
	private final TwilioRequestValidator twilioRequestValidator;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Logger logger;

	@Inject
	public TwilioSmsStatusCallbackResource(@Nonnull MessageService messageService,
																				 @Nonnull TwilioRequestValidator twilioRequestValidator,
																				 @Nonnull Configuration configuration) {
		requireNonNull(messageService);
		requireNonNull(twilioRequestValidator);
		requireNonNull(configuration);

		this.messageService = messageService;
		this.twilioRequestValidator = twilioRequestValidator;
		this.configuration = configuration;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	// Twilio sends an application/x-www-form-urlencoded POST to us
	@POST("/twilio/sms-status-callback")
	public void smsStatusCallback(@Nonnull HttpServletRequest httpServletRequest,
																@Nonnull @RequestHeader("X-Twilio-Signature") String twilioSignature,
																@Nonnull @RequestHeader("I-Twilio-Idempotency-Token") Optional<String> twilioIdempotencyToken,
																@Nonnull @RequestBody String requestBody) {
		requireNonNull(httpServletRequest);
		requireNonNull(twilioSignature);
		requireNonNull(twilioIdempotencyToken);
		requireNonNull(requestBody);

		String requestUrl = getConfiguration().getBaseUrl() + WebUtility.httpServletRequestUrl(httpServletRequest);

		getLogger().info("Received SMS status callback from Twilio. Signature is '{}', idempotency token is '{}', request body is '{}'",
				twilioSignature, twilioIdempotencyToken.orElse(null), requestBody);

		Map<String, String> requestHeaders = new HashMap<>();

		for (String headerName : Collections.list(httpServletRequest.getHeaderNames())) {
			getLogger().info("Request Header: {}={}", headerName, httpServletRequest.getHeader(headerName));
			requestHeaders.put(headerName, httpServletRequest.getHeader(headerName));
		}

		boolean valid = getTwilioRequestValidator().validateRequest(requestUrl, twilioSignature, requestBody);

		getLogger().info("Twilio SMS status callback validated: {}", valid);

		TwilioRequestBody twilioRequestBody = new TwilioRequestBody(requestBody);

		String vendorAssignedId = twilioRequestBody.getParameters().get("sid");
		MessageVendorId messageVendorId = MessageVendorId.TWILIO;

		// Useful for testing via manually-created messages
		// getMessageService().createTestMessageLog(MessageTypeId.SMS, messageVendorId, vendorAssignedId);

		MessageLog messageLog = getMessageService().findMessageLogByVendorAssignedId(vendorAssignedId, messageVendorId).orElse(null);

		if (messageLog == null) {
			getLogger().info("We have no record of {} message with vendor-assigned ID {}, ignoring webhook...", messageVendorId.name(), vendorAssignedId);
		} else {
			// TODO: based on notification, update message status
			getMessageService().createMessageLogEvent(messageLog.getMessageId(), requestHeaders, requestBody);
		}
	}

	@Nonnull
	protected MessageService getMessageService() {
		return this.messageService;
	}

	@Nonnull
	protected TwilioRequestValidator getTwilioRequestValidator() {
		return this.twilioRequestValidator;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
