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
import com.cobaltplatform.api.http.DefaultHttpClient;
import com.cobaltplatform.api.http.HttpClient;
import com.cobaltplatform.api.http.HttpMethod;
import com.cobaltplatform.api.http.HttpRequest;
import com.cobaltplatform.api.http.HttpResponse;
import com.cobaltplatform.api.integration.amazon.AmazonSnsMessageType;
import com.cobaltplatform.api.integration.amazon.AmazonSnsRequestBody;
import com.cobaltplatform.api.integration.amazon.AmazonSnsRequestValidator;
import com.cobaltplatform.api.model.db.MessageLog;
import com.cobaltplatform.api.model.db.MessageVendor.MessageVendorId;
import com.cobaltplatform.api.service.MessageService;
import com.google.gson.Gson;
import com.soklet.web.annotation.POST;
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
import java.io.IOException;
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
public class AmazonSnsCallbackResource {
	@Nonnull
	private final MessageService messageService;
	@Nonnull
	private final ErrorReporter errorReporter;
	@Nonnull
	private final AmazonSnsRequestValidator amazonSnsRequestValidator;
	@Nonnull
	private final HttpClient httpClient;
	@Nonnull
	private final Gson gson;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Logger logger;

	@Inject
	public AmazonSnsCallbackResource(@Nonnull MessageService messageService,
																	 @Nonnull ErrorReporter errorReporter,
																	 @Nonnull AmazonSnsRequestValidator amazonSnsRequestValidator,
																	 @Nonnull Configuration configuration) {
		requireNonNull(messageService);
		requireNonNull(errorReporter);
		requireNonNull(amazonSnsRequestValidator);
		requireNonNull(configuration);

		this.messageService = messageService;
		this.errorReporter = errorReporter;
		this.amazonSnsRequestValidator = amazonSnsRequestValidator;
		this.httpClient = new DefaultHttpClient("amazon-sns-callback");
		this.gson = new Gson();
		this.configuration = configuration;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	// Amazon
	@POST("/amazon/sns/email-callback")
	public void smsStatusCallback(@Nonnull HttpServletRequest httpServletRequest,
																@Nonnull @RequestHeader("X-Amz-Sns-Message-Type") String snsMessageType,
																@Nonnull @RequestHeader("X-Amz-Sns-Message-Id") String snsMessageId,
																@Nonnull @RequestHeader("X-Amz-Sns-Topic-Arn") String snsTopicArn,
																@Nonnull @RequestHeader("X-Amz-Sns-Subscription-Arn") Optional<String> snsSubscriptionArn,
																@Nonnull @RequestBody String requestBody) throws IOException {
		requireNonNull(httpServletRequest);
		requireNonNull(snsMessageType);
		requireNonNull(snsMessageId);
		requireNonNull(snsTopicArn);
		requireNonNull(snsSubscriptionArn);
		requireNonNull(requestBody);

		getLogger().debug("Received Amazon SNS callback. Request body is '{}'", requestBody);

		Map<String, String> requestHeaders = new HashMap<>();

		for (String headerName : Collections.list(httpServletRequest.getHeaderNames())) {
			getLogger().debug("Request Header: {}={}", headerName, httpServletRequest.getHeader(headerName));
			requestHeaders.put(headerName, httpServletRequest.getHeader(headerName));
		}

		AmazonSnsRequestBody amazonSnsRequestBody = new AmazonSnsRequestBody(requestBody);

		if (!getAmazonSnsRequestValidator().validateRequest(amazonSnsRequestBody)) {
			getErrorReporter().report(format("Unable to validate Amazon SNS webhook with request body: %s", requestBody));
			throw new AuthorizationException();
		}

		getLogger().info("This Amazon SNS callback is for message ID {}", amazonSnsRequestBody.getMessageId());

		if (amazonSnsRequestBody.getType() == AmazonSnsMessageType.SUBSCRIPTION_CONFIRMATION) {
			getLogger().info("This is an Amazon SNS subscription confirmation request - attempting to confirm...");

			HttpRequest httpRequest = new HttpRequest.Builder(HttpMethod.GET, amazonSnsRequestBody.getSubscribeUrl().get().toString()).build();
			HttpResponse httpResponse = getHttpClient().execute(httpRequest);

			if (httpResponse.getStatus() >= 400)
				throw new IOException(format("Failed to confirm Amazon SNS subscription because subscribe URL HTTP status was %d", httpResponse.getStatus()));

			getLogger().info("Successfully confirmed Amazon SNS subscription.");
		} else if (amazonSnsRequestBody.getType() == AmazonSnsMessageType.UNSUBSCRIBE_CONFIRMATION) {
			// TODO: handle unsubscribe, if needed
		} else if (amazonSnsRequestBody.getType() == AmazonSnsMessageType.NOTIFICATION) {
			String vendorAssignedId = (String) amazonSnsRequestBody.getMailAsMap().get("messageId");
			MessageVendorId messageVendorId = MessageVendorId.AMAZON_SES;

			MessageLog messageLog = getMessageService().findMessageLogByVendorAssignedId(vendorAssignedId, messageVendorId).orElse(null);

			// Useful for testing via AWS console manually-created messages
//			if (messageLog == null) {
//				getMessageService().createTestMessageLog(MessageTypeId.EMAIL, messageVendorId, vendorAssignedId);
//				messageLog = getMessageService().findMessageLogByVendorAssignedId(vendorAssignedId, messageVendorId).orElse(null);
//			}

			if (messageLog == null) {
				getLogger().info("We have no record of {} message with vendor-assigned ID {}, ignoring webhook...", messageVendorId.name(), vendorAssignedId);
			} else {
				// Based on notification, update message status
				getMessageService().createMessageLogEvent(messageLog.getMessageId(), requestHeaders, requestBody);

				String eventType = (String) amazonSnsRequestBody.getMessageAsMap().get("eventType");

				if ("Bounce".equals(eventType)) {
					getLogger().info("Detected a bounce.");

					Map<String, Object> bounce = amazonSnsRequestBody.getBounceAsMap().get();
					String bounceType = (String) bounce.get("bounceType");

					if ("Undetermined".equals(bounceType) || "Permanent".equals(bounceType)) {
						getLogger().info("This bounce is of type {} and indicates delivery failure", bounceType);

						String bounceSubType = (String) bounce.get("bounceSubType");
						String deliveryFailedReason = format("Bounced (%s/%s)",
								bounceType, (bounceSubType == null ? "Unspecified" : bounceSubType));

						getMessageService().recordMessageDeliveryFailed(messageLog.getMessageId(), deliveryFailedReason);
					} else {
						getLogger().info("This bounce is of type {} and does not indicate delivery failure", bounceType);
					}
				} else if ("Delivery".equals(eventType)) {
					getLogger().info("Detected successful delivery");
					getMessageService().recordMessageDelivery(messageLog.getMessageId());
				} else if ("Complaint".equals(eventType)) {
					getLogger().info("Detected complaint");
					getMessageService().recordMessageComplaint(messageLog.getMessageId());
				} else {
					getLogger().info("Not sure what to do with event type '{}', ignoring...", eventType);
				}
			}
		} else {
			throw new IllegalStateException(format("Not sure how to handle SNS request: %s", requestBody));
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
	protected AmazonSnsRequestValidator getAmazonSnsRequestValidator() {
		return this.amazonSnsRequestValidator;
	}

	@Nonnull
	protected Gson getGson() {
		return this.gson;
	}

	@Nonnull
	protected HttpClient getHttpClient() {
		return this.httpClient;
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
