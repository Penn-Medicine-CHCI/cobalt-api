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
import com.cobaltplatform.api.error.ErrorReporter;
import com.cobaltplatform.api.integration.enterprise.EnterprisePlugin;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.integration.twilio.TwilioError;
import com.cobaltplatform.api.integration.twilio.TwilioErrorResolver;
import com.cobaltplatform.api.integration.twilio.TwilioMessageStatus;
import com.cobaltplatform.api.integration.twilio.TwilioMessageWebhookRequestBody;
import com.cobaltplatform.api.integration.twilio.TwilioRequestValidator;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.MessageLog;
import com.cobaltplatform.api.model.db.MessageVendor.MessageVendorId;
import com.cobaltplatform.api.service.MessageService;
import com.cobaltplatform.api.util.WebUtility;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.RequestHeader;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
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

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class TwilioMessageStatusCallbackResource {
	@Nonnull
	private final MessageService messageService;
	@Nonnull
	private final ErrorReporter errorReporter;
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final TwilioErrorResolver twilioErrorResolver;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Logger logger;

	@Inject
	public TwilioMessageStatusCallbackResource(@Nonnull MessageService messageService,
																						 @Nonnull ErrorReporter errorReporter,
																						 @Nonnull EnterprisePluginProvider enterprisePluginProvider,
																						 @Nonnull TwilioErrorResolver twilioErrorResolver,
																						 @Nonnull Configuration configuration) {
		requireNonNull(messageService);
		requireNonNull(errorReporter);
		requireNonNull(enterprisePluginProvider);
		requireNonNull(twilioErrorResolver);
		requireNonNull(configuration);

		this.messageService = messageService;
		this.errorReporter = errorReporter;
		this.enterprisePluginProvider = enterprisePluginProvider;
		this.twilioErrorResolver = twilioErrorResolver;
		this.configuration = configuration;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	// Twilio sends an application/x-www-form-urlencoded POST to us
	@POST("/twilio/{institutionId}/message-status-callback")
	public void smsStatusCallback(@Nonnull HttpServletRequest httpServletRequest,
																@Nonnull @PathParameter InstitutionId institutionId,
																@Nonnull @RequestHeader("X-Twilio-Signature") String twilioSignature,
																@Nonnull @RequestHeader("I-Twilio-Idempotency-Token") Optional<String> twilioIdempotencyToken,
																@Nonnull @RequestBody String requestBody) {
		requireNonNull(httpServletRequest);
		requireNonNull(institutionId);
		requireNonNull(twilioSignature);
		requireNonNull(twilioIdempotencyToken);
		requireNonNull(requestBody);

		String requestUrl = getConfiguration().getBaseUrl() + WebUtility.httpServletRequestUrl(httpServletRequest);

		getLogger().info("Received message status callback from Twilio. Signature is '{}', idempotency token is '{}', request body is '{}'",
				twilioSignature, twilioIdempotencyToken.orElse(null), requestBody);

		Map<String, String> requestHeaders = new HashMap<>();

		for (String headerName : Collections.list(httpServletRequest.getHeaderNames())) {
			getLogger().info("Request Header: {}={}", headerName, httpServletRequest.getHeader(headerName));
			requestHeaders.put(headerName, httpServletRequest.getHeader(headerName));
		}

		EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(institutionId);
		TwilioRequestValidator twilioRequestValidator = enterprisePlugin.twilioRequestValidator();

		boolean valid = twilioRequestValidator.validateRequest(requestUrl, twilioSignature, requestBody);

		if (!valid) {
			getErrorReporter().report(format("Unable to validate Twilio webhook with request body: %s", requestBody));
			throw new AuthorizationException();
		}

		TwilioMessageWebhookRequestBody twilioMessageWebhookRequestBody = new TwilioMessageWebhookRequestBody(requestBody);

		String vendorAssignedId = twilioMessageWebhookRequestBody.getParameters().get("MessageSid");
		MessageVendorId messageVendorId = MessageVendorId.TWILIO;
		MessageLog messageLog = getMessageService().findMessageLogByVendorAssignedId(vendorAssignedId, messageVendorId).orElse(null);

		// Useful for testing via manually-created messages
//		if (messageLog == null) {
//			getMessageService().createTestMessageLog(MessageTypeId.SMS, messageVendorId, vendorAssignedId);
//			messageLog = getMessageService().findMessageLogByVendorAssignedId(vendorAssignedId, messageVendorId).orElse(null);
//		}

		if (messageLog == null) {
			getLogger().info("We have no record of {} message with vendor-assigned ID {}, ignoring webhook...", messageVendorId.name(), vendorAssignedId);
		} else {
			// Based on notification, update message status
			getMessageService().createMessageLogEvent(messageLog.getMessageId(), requestHeaders, requestBody);

			TwilioMessageStatus twilioMessageStatus = twilioMessageWebhookRequestBody.getTwilioMessageStatus().orElse(null);

			boolean deliverySucceeded = twilioMessageStatus == TwilioMessageStatus.DELIVERED;

			boolean deliveryFailed = twilioMessageStatus == TwilioMessageStatus.CANCELED ||
					twilioMessageStatus == TwilioMessageStatus.FAILED ||
					twilioMessageStatus == TwilioMessageStatus.UNDELIVERED;

			if (deliverySucceeded) {
				getMessageService().recordMessageDelivery(messageLog.getMessageId());
			} else if (deliveryFailed) {
				String deliveryFailedReason = "Unknown error";

				String errorCode = twilioMessageWebhookRequestBody.getParameters().get("ErrorCode");
				TwilioError twilioError = getTwilioErrorResolver().resolveTwilioErrorForErrorCode(errorCode).orElse(null);

				if (twilioError != null) {
					if (twilioError.getMessage() != null && twilioError.getSecondaryMessage() != null) {
						deliveryFailedReason = format("%s. %s", twilioError.getMessage(), twilioError.getSecondaryMessage());
					} else if (twilioError.getMessage() != null) {
						deliveryFailedReason = twilioError.getMessage();
					}
				}

				getMessageService().recordMessageDeliveryFailed(messageLog.getMessageId(), deliveryFailedReason);
			} else {
				getLogger().info("Nothing to do for Twilio message status '{}', ignoring...", twilioMessageStatus == null ? "[null]" : twilioMessageStatus.name());
			}
		}
	}

	@Nonnull
	protected MessageService getMessageService() {
		return this.messageService;
	}

	@Nonnull
	protected ErrorReporter getErrorReporter() {
		return this.errorReporter;
	}

	@Nonnull
	protected EnterprisePluginProvider getEnterprisePluginProvider() {
		return this.enterprisePluginProvider;
	}

	@Nonnull
	protected TwilioErrorResolver getTwilioErrorResolver() {
		return this.twilioErrorResolver;
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
