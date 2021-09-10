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

package com.cobaltplatform.api.service;

import com.lokalized.Strings;
import com.cobaltplatform.api.messaging.call.CallMessage;
import com.cobaltplatform.api.messaging.call.CallMessageManager;
import com.cobaltplatform.api.messaging.call.CallMessageTemplate;
import com.cobaltplatform.api.messaging.email.EmailMessageManager;
import com.cobaltplatform.api.messaging.sms.SmsMessage;
import com.cobaltplatform.api.messaging.sms.SmsMessageManager;
import com.cobaltplatform.api.messaging.sms.SmsMessageTemplate;
import com.cobaltplatform.api.model.api.request.SendCallMessagesRequest;
import com.cobaltplatform.api.model.api.request.SendCallMessagesRequest.SendCallMessageRequest;
import com.cobaltplatform.api.model.api.request.SendSmsMessagesRequest;
import com.cobaltplatform.api.model.api.request.SendSmsMessagesRequest.SendSmsMessageRequest;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.Normalizer;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class MessageService {
	@Nonnull
	private static final Integer MAXIMUM_MESSAGE_SEND_BATCH_SIZE;
	@Nonnull
	private static final Integer MAXIMUM_SMS_BODY_CHARACTER_COUNT;
	@Nonnull
	private static final Locale FREEFORM_MESSAGE_LOCALE;

	@Nonnull
	private final EmailMessageManager emailMessageManager;
	@Nonnull
	private final SmsMessageManager smsMessageManager;
	@Nonnull
	private final CallMessageManager callMessageManager;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Normalizer normalizer;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	static {
		MAXIMUM_MESSAGE_SEND_BATCH_SIZE = 100;
		MAXIMUM_SMS_BODY_CHARACTER_COUNT = 1_600;
		FREEFORM_MESSAGE_LOCALE = Locale.US;
	}

	@Inject
	public MessageService(@Nonnull EmailMessageManager emailMessageManager,
												@Nonnull SmsMessageManager smsMessageManager,
												@Nonnull CallMessageManager callMessageManager,
												@Nonnull Database database,
												@Nonnull Formatter formatter,
												@Nonnull Normalizer normalizer,
												@Nonnull Strings strings) {
		requireNonNull(emailMessageManager);
		requireNonNull(smsMessageManager);
		requireNonNull(callMessageManager);
		requireNonNull(database);
		requireNonNull(formatter);
		requireNonNull(normalizer);
		requireNonNull(strings);

		this.emailMessageManager = emailMessageManager;
		this.smsMessageManager = smsMessageManager;
		this.callMessageManager = callMessageManager;
		this.database = database;
		this.formatter = formatter;
		this.normalizer = normalizer;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	public void sendSmsMessages(@Nonnull SendSmsMessagesRequest request) {
		requireNonNull(request);

		List<SendSmsMessageRequest> messageRequests = request.getSmsMessages() == null ? Collections.emptyList() : request.getSmsMessages();
		ValidationException validationException = new ValidationException();
		int i = 0;

		for (SendSmsMessageRequest messageRequest : messageRequests) {
			if (messageRequest == null) {
				validationException.add(new FieldError(format("smsMessages[%s]", i), getStrings().get("SMS message element is missing.")));
			} else {
				String toNumber = trimToNull(messageRequest.getToNumber());
				String body = trimToNull(messageRequest.getBody());

				if (toNumber == null) {
					validationException.add(new FieldError(format("smsMessages[%s].toNumber", i), getStrings().get("SMS 'to' number is required.")));
				} else if (getNormalizer().normalizePhoneNumberToE164(toNumber).isEmpty()) {
					validationException.add(new FieldError(format("smsMessages[%s].toNumber", i), getStrings().get("SMS 'to' number {{toNumber}} is invalid.", new HashMap<String, Object>() {{
						put("toNumber", toNumber);
					}})));
				}

				if (body == null) {
					validationException.add(new FieldError(format("smsMessages[%s].body", i), getStrings().get("SMS message body is required.")));
				} else if (body.length() > getMaximumSmsBodyCharacterCount()) {
					validationException.add(new FieldError(format("smsMessages[%s].body", i), getStrings().get("SMS message body cannot exceed {{limit}} characters.", new HashMap<String, Object>() {{
						put("limit", getFormatter().formatNumber(getMaximumSmsBodyCharacterCount()));
					}})));
				}
			}

			++i;
		}

		if (validationException.hasErrors())
			throw validationException;

		for (SendSmsMessageRequest messageRequest : messageRequests) {
			String toNumber = trimToNull(messageRequest.getToNumber());
			String body = trimToNull(messageRequest.getBody());

			getSmsMessageManager().enqueueMessage(new SmsMessage.Builder(SmsMessageTemplate.FREEFORM, toNumber, getFreeformMessageLocale())
					.messageContext(new HashMap<String, Object>() {{
						put("body", body);
					}}).build());
		}
	}

	public void sendCallMessages(@Nonnull SendCallMessagesRequest request) {
		requireNonNull(request);

		List<SendCallMessageRequest> messageRequests = request.getCallMessages() == null ? Collections.emptyList() : request.getCallMessages();
		ValidationException validationException = new ValidationException();
		int i = 0;

		for (SendCallMessageRequest messageRequest : messageRequests) {
			if (messageRequest == null) {
				validationException.add(new FieldError(format("callMessages[%s]", i), getStrings().get("Call message element is missing.")));
			} else {
				String toNumber = trimToNull(messageRequest.getToNumber());
				String body = trimToNull(messageRequest.getBody());

				if (toNumber == null) {
					validationException.add(new FieldError(format("callMessages[%s].toNumber", i), getStrings().get("Call message 'to' number is required.")));
				} else if (getNormalizer().normalizePhoneNumberToE164(toNumber).isEmpty()) {
					validationException.add(new FieldError(format("callMessages[%s].toNumber", i), getStrings().get("Call message 'to' number {{toNumber}} is invalid.", new HashMap<String, Object>() {{
						put("toNumber", toNumber);
					}})));
				}

				if (body == null) {
					validationException.add(new FieldError(format("callMessages[%s].body", i), getStrings().get("Call message body is required.")));
				} else if (body.length() > getMaximumSmsBodyCharacterCount()) {
					validationException.add(new FieldError(format("callMessages[%s].body", i), getStrings().get("Call message body cannot exceed {{limit}} characters.", new HashMap<String, Object>() {{
						put("limit", getFormatter().formatNumber(getMaximumSmsBodyCharacterCount()));
					}})));
				}
			}

			++i;
		}

		if (validationException.hasErrors())
			throw validationException;

		for (SendCallMessageRequest messageRequest : messageRequests) {
			String toNumber = trimToNull(messageRequest.getToNumber());
			String body = trimToNull(messageRequest.getBody());

			getCallMessageManager().enqueueMessage(new CallMessage.Builder(CallMessageTemplate.FREEFORM, toNumber, getFreeformMessageLocale())
					.messageContext(new HashMap<String, Object>() {{
						put("body", body);
					}}).build());
		}
	}

	@Nonnull
	protected Integer getMaximumMessageSendBatchSize() {
		return MAXIMUM_MESSAGE_SEND_BATCH_SIZE;
	}

	@Nonnull
	protected Integer getMaximumSmsBodyCharacterCount() {
		return MAXIMUM_SMS_BODY_CHARACTER_COUNT;
	}

	@Nonnull
	protected Locale getFreeformMessageLocale() {
		return FREEFORM_MESSAGE_LOCALE;
	}

	@Nonnull
	protected EmailMessageManager getEmailMessageManager() {
		return emailMessageManager;
	}

	@Nonnull
	protected SmsMessageManager getSmsMessageManager() {
		return smsMessageManager;
	}

	@Nonnull
	protected CallMessageManager getCallMessageManager() {
		return callMessageManager;
	}

	@Nonnull
	protected Database getDatabase() {
		return database;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return formatter;
	}

	@Nonnull
	protected Normalizer getNormalizer() {
		return normalizer;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
