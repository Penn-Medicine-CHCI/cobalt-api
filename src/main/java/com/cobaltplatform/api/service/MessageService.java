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

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.error.ErrorReporter;
import com.cobaltplatform.api.integration.amazon.AmazonSnsRequestBody;
import com.cobaltplatform.api.integration.enterprise.EnterprisePlugin;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.integration.twilio.TwilioRequestBody;
import com.cobaltplatform.api.messaging.Message;
import com.cobaltplatform.api.messaging.MessageSender;
import com.cobaltplatform.api.messaging.call.CallMessage;
import com.cobaltplatform.api.messaging.call.CallMessageSerializer;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageContextKey;
import com.cobaltplatform.api.messaging.email.EmailMessageSerializer;
import com.cobaltplatform.api.messaging.push.PushMessage;
import com.cobaltplatform.api.messaging.push.PushMessageSerializer;
import com.cobaltplatform.api.messaging.sms.SmsMessage;
import com.cobaltplatform.api.messaging.sms.SmsMessageSerializer;
import com.cobaltplatform.api.model.api.request.CreateScheduledMessageRequest;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionColorValue;
import com.cobaltplatform.api.model.db.MessageLog;
import com.cobaltplatform.api.model.db.MessageStatus.MessageStatusId;
import com.cobaltplatform.api.model.db.MessageType.MessageTypeId;
import com.cobaltplatform.api.model.db.MessageVendor.MessageVendorId;
import com.cobaltplatform.api.model.db.ScheduledMessage;
import com.cobaltplatform.api.model.db.ScheduledMessageStatus.ScheduledMessageStatusId;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.Normalizer;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lokalized.Strings;
import com.pyranid.Database;
import com.pyranid.Transaction;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class MessageService implements AutoCloseable {
	@Nonnull
	private static final Integer MAXIMUM_SMS_BODY_CHARACTER_COUNT;
	@Nonnull
	private static final Locale FREEFORM_MESSAGE_LOCALE;
	@Nonnull
	private static final Long SCHEDULED_MESSAGE_TASK_INTERVAL_IN_SECONDS;
	@Nonnull
	private static final Long SCHEDULED_MESSAGE_TASK_INITIAL_DELAY_IN_SECONDS;
	@Nonnull
	private static final Long SEND_MESSAGE_TASK_INTERVAL_IN_SECONDS;
	@Nonnull
	private static final Long SEND_MESSAGE_TASK_INITIAL_DELAY_IN_SECONDS;

	@Nonnull
	private final Provider<SendMessageTask> sendMessageTaskProvider;
	@Nonnull
	private final Provider<ScheduledMessageTask> scheduledMessageTaskProvider;
	@Nonnull
	private final Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final EmailMessageSerializer emailMessageSerializer;
	@Nonnull
	private final MessageSender<EmailMessage> emailMessageSender;
	@Nonnull
	private final SmsMessageSerializer smsMessageSerializer;
	@Nonnull
	private final CallMessageSerializer callMessageSerializer;
	@Nonnull
	private final PushMessageSerializer pushMessageSerializer;
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Normalizer normalizer;
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Object lock;
	@Nonnull
	private final Logger logger;

	@Nonnull
	private Boolean started;
	@Nullable
	private ScheduledExecutorService sendMessageTaskExecutorService;
	@Nullable
	private ScheduledExecutorService scheduledMessageTaskExecutorService;

	static {
		MAXIMUM_SMS_BODY_CHARACTER_COUNT = 1_600;
		FREEFORM_MESSAGE_LOCALE = Locale.forLanguageTag("en-US");
		SEND_MESSAGE_TASK_INTERVAL_IN_SECONDS = 5L;
		SEND_MESSAGE_TASK_INITIAL_DELAY_IN_SECONDS = 10L;
		SCHEDULED_MESSAGE_TASK_INTERVAL_IN_SECONDS = 15L;
		SCHEDULED_MESSAGE_TASK_INITIAL_DELAY_IN_SECONDS = 10L;
	}

	@Inject
	public MessageService(@Nonnull Provider<InstitutionService> institutionServiceProvider,
												@Nonnull Provider<SendMessageTask> sendMessageTaskProvider,
												@Nonnull Provider<ScheduledMessageTask> scheduledMessageTaskProvider,
												@Nonnull EmailMessageSerializer emailMessageSerializer,
												@Nonnull MessageSender<EmailMessage> emailMessageSender,
												@Nonnull SmsMessageSerializer smsMessageSerializer,
												@Nonnull CallMessageSerializer callMessageSerializer,
												@Nonnull PushMessageSerializer pushMessageSerializer,
												@Nonnull DatabaseProvider databaseProvider,
												@Nonnull Configuration configuration,
												@Nonnull Formatter formatter,
												@Nonnull Normalizer normalizer,
												@Nonnull JsonMapper jsonMapper,
												@Nonnull EnterprisePluginProvider enterprisePluginProvider,
												@Nonnull Strings strings) {
		requireNonNull(institutionServiceProvider);
		requireNonNull(sendMessageTaskProvider);
		requireNonNull(scheduledMessageTaskProvider);
		requireNonNull(emailMessageSerializer);
		requireNonNull(emailMessageSender);
		requireNonNull(smsMessageSerializer);
		requireNonNull(callMessageSerializer);
		requireNonNull(pushMessageSerializer);
		requireNonNull(databaseProvider);
		requireNonNull(configuration);
		requireNonNull(formatter);
		requireNonNull(normalizer);
		requireNonNull(jsonMapper);
		requireNonNull(enterprisePluginProvider);
		requireNonNull(strings);

		this.institutionServiceProvider = institutionServiceProvider;
		this.sendMessageTaskProvider = sendMessageTaskProvider;
		this.scheduledMessageTaskProvider = scheduledMessageTaskProvider;
		this.emailMessageSerializer = emailMessageSerializer;
		this.emailMessageSender = emailMessageSender;
		this.smsMessageSerializer = smsMessageSerializer;
		this.callMessageSerializer = callMessageSerializer;
		this.pushMessageSerializer = pushMessageSerializer;
		this.databaseProvider = databaseProvider;
		this.configuration = configuration;
		this.formatter = formatter;
		this.normalizer = normalizer;
		this.jsonMapper = jsonMapper;
		this.enterprisePluginProvider = enterprisePluginProvider;
		this.strings = strings;
		this.lock = new Object();
		this.started = false;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public void close() throws Exception {
		stop();
	}

	public void start() {
		synchronized (getLock()) {
			if (isStarted()) {
				getLogger().warn("Message service already started, ignoring request to start...");
				return;
			}

			getLogger().trace("Starting message service...");

			this.sendMessageTaskExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("send-message-task-executor").build());
			this.scheduledMessageTaskExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("scheduled-message-task-executor").build());

			this.started = true;

			getSendMessageTaskExecutorService().get().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						getSendMessageTaskProvider().get().run();
					} catch (Exception e) {
						getLogger().warn(format("Unable to process sending messages - will retry in %s seconds", getSendMessageTaskIntervalInSeconds()), e);
					}
				}
			}, getSendMessageTaskInitialDelayInSeconds(), getSendMessageTaskIntervalInSeconds(), TimeUnit.SECONDS);

			getScheduledMessageTaskExecutorService().get().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						getScheduledMessageTaskProvider().get().run();
					} catch (Exception e) {
						getLogger().warn(format("Unable to process scheduled messages - will retry in %s seconds", getScheduledMessageTaskIntervalInSeconds()), e);
					}
				}
			}, getScheduledMessageTaskInitialDelayInSeconds(), getScheduledMessageTaskIntervalInSeconds(), TimeUnit.SECONDS);

			getLogger().trace("Message service started.");
		}
	}

	public void stop() {
		synchronized (getLock()) {
			if (!isStarted()) {
				getLogger().warn("Message service already stopped, ignoring request to stop...");
				return;
			}

			getLogger().trace("Stopping message service...");

			getScheduledMessageTaskExecutorService().get().shutdown();
			this.scheduledMessageTaskExecutorService = null;

			getSendMessageTaskExecutorService().get().shutdown();
			this.sendMessageTaskExecutorService = null;

			started = false;

			getLogger().trace("Message service stopped.");
		}
	}

	@Nonnull
	public <T extends Message> void enqueueMessage(@Nonnull T message) {
		requireNonNull(message);

		if (!isStarted())
			throw new IllegalStateException("Message manager is not started, cannot enqueue messages");

		String serializedMessage;
		MessageVendorId messageVendorId;

		if (message.getMessageTypeId() == MessageTypeId.EMAIL) {
			EmailMessage customizedEmailMessage = (EmailMessage) message;

			// Customize the message
			InstitutionId institutionId = message.getInstitutionId();
			Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
			List<InstitutionColorValue> institutionColorValues = getInstitutionService().findInstitutionColorValuesByInstitutionId(institutionId);

			// Add some common global fields to the email before it goes out
			Map<String, Object> messageContext = new HashMap<>(customizedEmailMessage.getMessageContext()); // Mutable copy

			// e.g. https://cobaltplatform.s3.us-east-2.amazonaws.com/local/emails/button-start-appointment@2x.jpg
			String staticFileUrlPrefix = format("https://%s.s3.%s.amazonaws.com/%s/emails",
					getConfiguration().getAmazonS3BucketName(), getConfiguration().getAmazonS3Region().id(), getConfiguration().getEnvironment());

			messageContext.put("staticFileUrlPrefix", staticFileUrlPrefix);
			messageContext.put("copyrightYear", LocalDateTime.now(institution.getTimeZone()).getYear());

			// e.g. "p900" -> "#FEA123"
			Map<String, String> cssColorRepresentationsByName = institutionColorValues.stream()
					.collect(Collectors.toMap(
									institutionColorValue -> institutionColorValue.getName(),
									institutionColorValue -> institutionColorValue.getCssRepresentation()
							)
					);

			messageContext.put("colors", cssColorRepresentationsByName);

			// Platform name (optionally overridable)
			String platformName = ObjectUtils.firstNonNull(
					trimToNull((String) messageContext.get(EmailMessageContextKey.OVERRIDE_PLATFORM_NAME.name())),
					getStrings().get("Cobalt")
			);

			messageContext.put("platformName", platformName);

			// Support email address (optionally overridable)
			String supportEmailAddress = ObjectUtils.firstNonNull(
					trimToNull((String) messageContext.get(EmailMessageContextKey.OVERRIDE_PLATFORM_SUPPORT_EMAIL_ADDRESS.name())),
					institution.getSupportEmailAddress()
			);

			messageContext.put("supportEmailAddress", supportEmailAddress);

			// Support email address (optionally overridable)
			String platformEmailImageUrl = ObjectUtils.firstNonNull(
					trimToNull((String) messageContext.get(EmailMessageContextKey.OVERRIDE_PLATFORM_EMAIL_IMAGE_URL.name())),
					format("%s/logo@2x.jpg", staticFileUrlPrefix)
			);

			messageContext.put("platformEmailImageUrl", platformEmailImageUrl);

			// Create a new email message using the updated email message context
			customizedEmailMessage = customizedEmailMessage.toBuilder()
					.messageContext(messageContext)
					.build();

			// Hook for institutions to further customize outgoing emails
			EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(message.getInstitutionId());
			customizedEmailMessage = enterprisePlugin.customizeEmailMessage(customizedEmailMessage);

			message = (T) customizedEmailMessage;

			serializedMessage = getEmailMessageSerializer().serializeMessage((EmailMessage) message);
			messageVendorId = getEmailMessageSender().getMessageVendorId();
		} else if (message.getMessageTypeId() == MessageTypeId.SMS) {
			serializedMessage = getSmsMessageSerializer().serializeMessage((SmsMessage) message);
			EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(message.getInstitutionId());
			messageVendorId = enterprisePlugin.callMessageSender().getMessageVendorId();
		} else if (message.getMessageTypeId() == MessageTypeId.CALL) {
			serializedMessage = getCallMessageSerializer().serializeMessage((CallMessage) message);
			EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(message.getInstitutionId());
			messageVendorId = enterprisePlugin.callMessageSender().getMessageVendorId();
		} else if (message.getMessageTypeId() == MessageTypeId.PUSH) {
			PushMessage pushMessage = (PushMessage) message;
			serializedMessage = getPushMessageSerializer().serializeMessage(pushMessage);
			EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(message.getInstitutionId());
			messageVendorId = enterprisePlugin.pushMessageSenderForPushTokenTypeId(pushMessage.getClientDevicePushTokenTypeId()).getMessageVendorId();
		} else {
			throw new IllegalStateException(format("Sorry, %s.%s is not yet supported.",
					MessageTypeId.class.getSimpleName(), message.getMessageTypeId().name()));
		}

		getDatabase().execute("INSERT INTO message_log (message_id, institution_id, message_type_id, message_status_id, message_vendor_id, serialized_message, enqueued) VALUES (?,?,?,?,?,CAST(? AS JSONB),NOW())",
				message.getMessageId(), message.getInstitutionId(), message.getMessageTypeId(), MessageStatusId.ENQUEUED, messageVendorId, serializedMessage);
	}

	/**
	 * Schedules a message that the system will send on or after the specified date/time.
	 *
	 * @param request (nonnull) the data necessary to create the scheduled message
	 * @return (nonnull) the scheduled message ID
	 */
	@Nonnull
	public UUID createScheduledMessage(@Nonnull CreateScheduledMessageRequest request) {
		requireNonNull(request);

		Message message = request.getMessage();
		UUID scheduledMessageId = UUID.randomUUID();
		LocalDateTime scheduledAt = message == null ? null : request.getScheduledAt();
		ZoneId timeZone = message == null ? null : request.getTimeZone();
		Map<String, Object> metadata = request.getMetadata() == null ? null : request.getMetadata();
		String serializedMessage;

		ValidationException validationException = new ValidationException();

		if (message == null)
			validationException.add(new FieldError("message", getStrings().get("Message is required.")));

		if (scheduledAt == null)
			validationException.add(new FieldError("scheduledAt", getStrings().get("'Scheduled at' date/time is required.")));

		if (timeZone == null)
			validationException.add(new FieldError("timeZone", getStrings().get("Time zone is required.")));

		if (validationException.hasErrors())
			throw validationException;

		if (message.getMessageTypeId() == MessageTypeId.EMAIL)
			serializedMessage = getEmailMessageSerializer().serializeMessage((EmailMessage) message);
		else if (message.getMessageTypeId() == MessageTypeId.SMS)
			serializedMessage = getSmsMessageSerializer().serializeMessage((SmsMessage) message);
		else if (message.getMessageTypeId() == MessageTypeId.CALL)
			serializedMessage = getCallMessageSerializer().serializeMessage((CallMessage) message);
		else if (message.getMessageTypeId() == MessageTypeId.PUSH)
			serializedMessage = getPushMessageSerializer().serializeMessage((PushMessage) message);
		else
			throw new IllegalStateException(format("Sorry, %s.%s is not yet supported.",
					MessageTypeId.class.getSimpleName(), message.getMessageTypeId().name()));

		String metadataAsJson = metadata == null ? null : getJsonMapper().toJson(metadata);

		getLogger().info("Creating scheduled message of type {}, scheduled for {} {}.\nMetadata:\n{}\nSerialized form:\n{}",
				message.getMessageTypeId().name(), scheduledAt, timeZone.getId(), metadata == null ? "[none]" : metadataAsJson, serializedMessage);

		getDatabase().execute("INSERT INTO scheduled_message (scheduled_message_id, institution_id, message_id, message_type_id, " +
						"serialized_message, scheduled_at, time_zone, metadata) VALUES (?,?,?,?,CAST(? AS JSONB),?,?,CAST(? AS JSONB))",
				scheduledMessageId, message.getInstitutionId(), message.getMessageId(), message.getMessageTypeId(), serializedMessage, scheduledAt, timeZone, metadataAsJson);

		return scheduledMessageId;
	}

	/**
	 * Finds a scheduled message given its ID.
	 *
	 * @param scheduledMessageId (nullable) the ID of the scheduled message to find
	 * @return (nonnull) an {@link Optional} representation of the scheduled message
	 */
	@Nonnull
	public Optional<ScheduledMessage> findScheduledMessageById(@Nullable UUID scheduledMessageId) {
		if (scheduledMessageId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM scheduled_message WHERE scheduled_message_id=?",
				ScheduledMessage.class, scheduledMessageId);
	}

	/**
	 * Finds scheduled messages that match (contain) the provided metadata.
	 *
	 * @param metadata (nullable) the metadata to match against
	 * @return (nonnull) a list of scheduled messages that contain the metadata
	 */
	@Nonnull
	public List<ScheduledMessage> findScheduledMessagesMatchingMetadata(@Nullable Map<String, Object> metadata) {
		if (metadata == null || metadata.size() == 0)
			return Collections.emptyList();

		String metadataAsJson = getJsonMapper().toJson(metadata);

		// Uses JSONB containment operator.
		//
		// Example from http://www.silota.com/docs/recipes/sql-postgres-json-data-types.html:
		//
		// select '{"name": "Alice", "agent": {"bot": true} }'::jsonb @> '{"agent": {"bot": false}}';
		// -- returns false
		//
		// select '{"name": "Alice", "agent": {"bot": true} }'::jsonb @> '{"agent": {"bot": true}}';
		// -- returns true
		return getDatabase().queryForList("SELECT * FROM scheduled_message WHERE metadata @> CAST(? AS JSONB) " +
				"ORDER BY TIMEZONE(time_zone, scheduled_at)", ScheduledMessage.class, metadataAsJson);
	}

	/**
	 * Cancels a scheduled message. Only applicable to messages still in PENDING status.
	 *
	 * @param scheduledMessageId (nullable) the ID of the scheduled message to cancel
	 * @return (nonnull) {@code true} if cancelation succeeded, {@code false} otherwise
	 */
	@Nonnull
	public Boolean cancelScheduledMessage(@Nullable UUID scheduledMessageId) {
		if (scheduledMessageId == null)
			return false;

		boolean canceled = getDatabase().execute("UPDATE scheduled_message SET scheduled_message_status_id=?, canceled_at=NOW() " +
						"WHERE scheduled_message_status_id=? AND scheduled_message_id=?", ScheduledMessageStatusId.CANCELED,
				ScheduledMessageStatusId.PENDING, scheduledMessageId) > 0;

		getLogger().info("Scheduled message ID {} was {} canceled.", scheduledMessageId, canceled ? "successfully" : "NOT");

		return canceled;
	}

	public void createTestMessageLog(@Nonnull MessageTypeId messageTypeId,
																	 @Nonnull MessageVendorId messageVendorId,
																	 @Nonnull String vendorAssignedId) {
		requireNonNull(messageTypeId);
		requireNonNull(messageVendorId);
		requireNonNull(vendorAssignedId);

		UUID messageId = UUID.randomUUID();
		MessageStatusId messageStatusId = MessageStatusId.SENT;
		String serializedMessage = "{}";

		getDatabase().execute("""
				INSERT INTO message_log (
				message_id,
				vendor_assigned_id,
				message_type_id,
				message_status_id,
				message_vendor_id,
				serialized_message,
				enqueued,
				processed
				) VALUES (?,?,?,?,?,CAST(? AS JSONB),NOW(),NOW())
				""", messageId, vendorAssignedId, messageTypeId, messageStatusId.SENT, messageVendorId, serializedMessage);
	}

	@Nonnull
	public Optional<MessageLog> findMessageLogById(@Nullable UUID messageLogId) {
		if (messageLogId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM message_log
				WHERE message_id=?
				""", MessageLog.class, messageLogId);
	}

	@Nonnull
	public Optional<MessageLog> findMessageLogByVendorAssignedId(@Nullable String vendorAssignedId,
																															 @Nullable MessageVendorId messageVendorId) {
		if (vendorAssignedId == null || messageVendorId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM message_log
				WHERE vendor_assigned_id=?
				AND message_vendor_id=?
				""", MessageLog.class, vendorAssignedId, messageVendorId);
	}

	@Nonnull
	public UUID createMessageLogEvent(@Nonnull UUID messageId,
																		@Nonnull Map<String, String> webhookHeaders,
																		@Nonnull String webhookRequestBody) {
		requireNonNull(messageId);
		requireNonNull(webhookHeaders);
		requireNonNull(webhookRequestBody);

		UUID messageLogEventId = UUID.randomUUID();
		MessageLog messageLog = findMessageLogById(messageId).get();

		// The `webhookPayload` field is designed to be easily queryable.
		// Therefore, we ensure it's pure JSON (not a string of encoded JSON, or form parameters, or w/e like
		// the raw request body itself might be) by only putting in "clean" data.
		// We still persist the raw data in `webhookRequestBody` in case it's needed later
		Map<String, Object> webhookPayload = new HashMap<>();

		if (messageLog.getMessageVendorId() == MessageVendorId.TWILIO) {
			TwilioRequestBody twilioRequestBody = new TwilioRequestBody(webhookRequestBody);
			webhookPayload.putAll(twilioRequestBody.getParameters());
		} else if (messageLog.getMessageVendorId() == MessageVendorId.AMAZON_SES) {
			AmazonSnsRequestBody amazonSnsRequestBody = new AmazonSnsRequestBody(webhookRequestBody);
			Map<String, Object> requestBodyAsMap = new HashMap<>(amazonSnsRequestBody.getRequestBodyAsMap());

			// Tricky: the request body payload of field 'Message' is an encoded JSON string, e.g. "{\"notificationType\":\"Delivery\", ...
			// We parse the JSON and put it back in so it's a "real" object and easily queryable in our DB
			Map<String, Object> messageAsJson;

			try {
				messageAsJson = getJsonMapper().fromJson(amazonSnsRequestBody.getMessage());
			} catch (Exception ignored) {
				// There are corner cases where this field is not JSON, e.g. Configuration Set change notification.
				// Detect that here and use a failsafe
				messageAsJson = Map.of("message", amazonSnsRequestBody.getMessage());
			}

			requestBodyAsMap.put("Message", messageAsJson);

			webhookPayload.putAll(requestBodyAsMap);
		} else {
			throw new IllegalStateException(format("Unsupported %s value '%s'",
					MessageVendorId.class.getSimpleName(), messageLog.getMessageVendorId().name()));
		}

		Map<String, Object> eventData = new HashMap<>();
		eventData.put("webhookHeaders", webhookHeaders);
		eventData.put("webhookRequestBody", webhookRequestBody);
		eventData.put("webhookPayload", webhookPayload);

		String eventDataAsJson = getJsonMapper().toJson(eventData);

		getDatabase().execute("""
				INSERT INTO message_log_event (
				message_log_event_id,
				message_id,
				event_data
				) VALUES (?,?,CAST(? AS JSONB))
				""", messageLogEventId, messageId, eventDataAsJson);

		return messageLogEventId;
	}

	@Nonnull
	public Boolean recordMessageDelivery(@Nonnull UUID messageId) {
		requireNonNull(messageId);

		return getDatabase().execute("""
				UPDATE message_log
				SET message_status_id=?, delivered=NOW()
				WHERE message_id=?
				""", MessageStatusId.DELIVERED, messageId) > 0;
	}

	@Nonnull
	public Boolean recordMessageComplaint(@Nonnull UUID messageId) {
		requireNonNull(messageId);

		return getDatabase().execute("""
				UPDATE message_log
				SET complaint_registered=NOW()
				WHERE message_id=?
				""", messageId) > 0;
	}

	public void recordMessageDeliveryFailed(@Nonnull UUID messageId,
																					@Nullable String deliveryFailedReason) {
		requireNonNull(messageId);

		getDatabase().execute("""
				UPDATE message_log
				SET message_status_id=?, delivery_failed_reason=?, delivery_failed=NOW()
				WHERE message_id=?
				""", MessageStatusId.DELIVERY_FAILED, deliveryFailedReason, messageId);
	}

	@Nonnull
	public Boolean isStarted() {
		synchronized (getLock()) {
			return started;
		}
	}

	@ThreadSafe
	public static class SendMessageTask implements Runnable {
		@Nonnull
		private final MessageService messageService;
		@Nonnull
		private final MessageSender<EmailMessage> emailMessageSender;
		@Nonnull
		private final EmailMessageSerializer emailMessageSerializer;
		@Nonnull
		private final MessageSender<SmsMessage> smsMessageSender;
		@Nonnull
		private final SmsMessageSerializer smsMessageSerializer;
		@Nonnull
		private final MessageSender<CallMessage> callMessageSender;
		@Nonnull
		private final CallMessageSerializer callMessageSerializer;
		@Nonnull
		private final PushMessageSerializer pushMessageSerializer;
		@Nonnull
		private final DatabaseProvider databaseProvider;
		@Nonnull
		private final EnterprisePluginProvider enterprisePluginProvider;
		@Nonnull
		private final CurrentContextExecutor currentContextExecutor;
		@Nonnull
		private final ErrorReporter errorReporter;
		@Nonnull
		private final Formatter formatter;
		@Nonnull
		private final Configuration configuration;
		@Nonnull
		private final Logger logger;

		@Inject
		public SendMessageTask(@Nonnull MessageService messageService,
													 @Nonnull EmailMessageSerializer emailMessageSerializer,
													 @Nonnull MessageSender<EmailMessage> emailMessageSender,
													 @Nonnull SmsMessageSerializer smsMessageSerializer,
													 @Nonnull MessageSender<SmsMessage> smsMessageSender,
													 @Nonnull CallMessageSerializer callMessageSerializer,
													 @Nonnull MessageSender<CallMessage> callMessageSender,
													 @Nonnull PushMessageSerializer pushMessageSerializer,
													 @Nonnull DatabaseProvider databaseProvider,
													 @Nonnull EnterprisePluginProvider enterprisePluginProvider,
													 @Nonnull CurrentContextExecutor currentContextExecutor,
													 @Nonnull ErrorReporter errorReporter,
													 @Nonnull Formatter formatter,
													 @Nonnull Configuration configuration) {
			requireNonNull(messageService);
			requireNonNull(emailMessageSerializer);
			requireNonNull(emailMessageSender);
			requireNonNull(smsMessageSerializer);
			requireNonNull(smsMessageSender);
			requireNonNull(callMessageSerializer);
			requireNonNull(callMessageSender);
			requireNonNull(pushMessageSerializer);
			requireNonNull(databaseProvider);
			requireNonNull(enterprisePluginProvider);
			requireNonNull(currentContextExecutor);
			requireNonNull(errorReporter);
			requireNonNull(formatter);
			requireNonNull(configuration);

			this.messageService = messageService;
			this.emailMessageSerializer = emailMessageSerializer;
			this.emailMessageSender = emailMessageSender;
			this.smsMessageSerializer = smsMessageSerializer;
			this.smsMessageSender = smsMessageSender;
			this.callMessageSerializer = callMessageSerializer;
			this.pushMessageSerializer = pushMessageSerializer;
			this.callMessageSender = callMessageSender;
			this.databaseProvider = databaseProvider;
			this.enterprisePluginProvider = enterprisePluginProvider;
			this.currentContextExecutor = currentContextExecutor;
			this.errorReporter = errorReporter;
			this.formatter = formatter;
			this.configuration = configuration;
			this.logger = LoggerFactory.getLogger(getClass());
		}

		protected void forceTransitionMessageToError(@Nonnull Message message) {
			requireNonNull(message);

			getLogger().warn("Force-transitioning message ID {} to {}...", message.getMessageId(), MessageStatusId.ERROR.name());

			Transaction transaction = getDatabase().currentTransaction().orElse(null);

			if (transaction != null) {
				getLogger().info("We are currently in a transaction, let's wait until it's complete before forcing...");

				// TODO: once Pyranid supports PostTransactionOperations, use one of them instead of splitting like this
				transaction.addPostCommitOperation(() -> {
					getLogger().warn("OK, we are post-commit, now force-transitioning message ID {} to {}...", message.getMessageId(), MessageStatusId.ERROR.name());
					forceTransitionMessageToErrorInternal(message);
				});

				transaction.addPostRollbackOperation(() -> {
					getLogger().warn("OK, we are post-rollback, now force-transitioning message ID {} to {}...", message.getMessageId(), MessageStatusId.ERROR.name());
					forceTransitionMessageToErrorInternal(message);
				});
			} else {
				forceTransitionMessageToErrorInternal(message);
			}
		}

		protected void forceTransitionMessageToErrorInternal(@Nonnull Message message) {
			requireNonNull(message);

			try {
				getDatabase().execute("UPDATE message_log SET message_status_id=?, processed=NOW() WHERE message_id=?",
						MessageStatusId.ERROR, message.getMessageId());

				getLogger().warn("Force-transitioning message ID {} to {} has completed successfully.", message.getMessageId(), MessageStatusId.ERROR.name());
			} catch (Throwable t) {
				getLogger().error(format("Unable to force-transition message ID %s to %s", message.getMessageId(), MessageStatusId.ENQUEUED), t);
				getErrorReporter().report(t);
			}
		}

		@Nonnull
		protected Boolean dequeueAndSendMessage(@Nonnull MessageLog messageLog) {
			requireNonNull(messageLog);

			MessageSender messageSender;
			Message deserializedMessage;

			if (messageLog.getMessageTypeId() == MessageTypeId.EMAIL) {
				deserializedMessage = getEmailMessageSerializer().deserializeMessage(messageLog.getSerializedMessage());
				messageSender = getEmailMessageSender();
			} else if (messageLog.getMessageTypeId() == MessageTypeId.SMS) {
				deserializedMessage = getSmsMessageSerializer().deserializeMessage(messageLog.getSerializedMessage());
				messageSender = getEnterprisePluginProvider().enterprisePluginForCurrentInstitution().smsMessageSender();
			} else if (messageLog.getMessageTypeId() == MessageTypeId.CALL) {
				deserializedMessage = getCallMessageSerializer().deserializeMessage(messageLog.getSerializedMessage());
				messageSender = getEnterprisePluginProvider().enterprisePluginForCurrentInstitution().callMessageSender();
			} else if (messageLog.getMessageTypeId() == MessageTypeId.PUSH) {
				deserializedMessage = getPushMessageSerializer().deserializeMessage(messageLog.getSerializedMessage());
				PushMessage pushMessage = (PushMessage) deserializedMessage;
				messageSender = getEnterprisePluginProvider().enterprisePluginForInstitutionId(deserializedMessage.getInstitutionId()).pushMessageSenderForPushTokenTypeId(pushMessage.getClientDevicePushTokenTypeId());
			} else {
				throw new IllegalStateException(format("Sorry, %s.%s is not yet supported.",
						MessageTypeId.class.getSimpleName(), messageLog.getMessageTypeId().name()));
			}

			try {
				String vendorAssignedId = messageSender.sendMessage(deserializedMessage);

				try {
					getDatabase().execute("UPDATE message_log SET message_status_id=?, vendor_assigned_id=?, processed=NOW() WHERE message_id=?",
							MessageStatusId.SENT, vendorAssignedId, deserializedMessage.getMessageId());

					return true;
				} catch (Throwable t) {
					try {
						// Not much we can do, just bail
						getLogger().error(format("Unable to update message log for message ID %s", deserializedMessage.getMessageId()), t);
						getErrorReporter().report(t);
					} finally {
						forceTransitionMessageToError(deserializedMessage);
					}

					return false;
				}
			} catch (Throwable t) {
				getLogger().warn(format("Unable to send message with message ID %s, going to mark as %s...",
						deserializedMessage.getMessageId(), MessageStatusId.ERROR.name()), t);

				String stackTrace = null;

				try {
					stackTrace = getFormatter().formatStackTrace(t);
				} catch (Throwable t2) {
					getLogger().warn(format("Unable to extract stack trace for failed message send for message ID %s",
							deserializedMessage.getMessageId()), t);
				}

				try {
					getDatabase().execute("UPDATE message_log SET message_status_id=?, processed=NOW(), stack_trace=? WHERE message_id=?",
							MessageStatusId.ERROR, stackTrace, deserializedMessage.getMessageId());

					return false;
				} catch (Throwable t2) {
					// Not much we can do, just bail
					try {
						getLogger().error(format("Unable to update message log for message ID %s", deserializedMessage.getMessageId()), t2);
						getErrorReporter().report(t2);
					} finally {
						forceTransitionMessageToError(deserializedMessage);
					}

					return false;
				}
			}
		}

		@Override
		public void run() {
			CurrentContext currentContext = new CurrentContext.Builder(InstitutionId.COBALT,
					getConfiguration().getDefaultLocale(), getConfiguration().getDefaultTimeZone()).build();

			getCurrentContextExecutor().execute(currentContext, () -> {
				// Not used for thead safety, but rather as a "pinned" reference we can change inside of the closure
				AtomicBoolean moreMessagesExist = new AtomicBoolean(true);
				AtomicInteger totalMessagesProcessed = new AtomicInteger(0);
				AtomicInteger totalMessagesSent = new AtomicInteger(0);
				AtomicInteger totalMessagesFailed = new AtomicInteger(0);

				while (moreMessagesExist.get()) {
					getDatabase().transaction(() -> {
						// For now, keep batch size at 1 so we insert right away after the send, which
						// means by the time a confirmation webhook arrives, we'll certainly have the transaction committed/record ready to receive it.
						// In the future, if we need higher-volume sending, we should revisit this approach
						int batchSize = 1;

						// Anything in ENQUEUED status can be sent
						List<MessageLog> sendableMessages = getDatabase().queryForList("""
								SELECT *
								FROM message_log
								WHERE message_status_id=?
								LIMIT ?
								FOR UPDATE
								SKIP LOCKED
								""", MessageLog.class, MessageStatusId.ENQUEUED, batchSize);

						if (sendableMessages.size() == 0) {
							moreMessagesExist.set(false);
						} else {
							getLogger().info("Sending a batch of {} message[s]...", sendableMessages.size());
							int i = 0;

							for (MessageLog sendableMessage : sendableMessages) {
								totalMessagesProcessed.incrementAndGet();

								getLogger().info("Sending message {} of {}...", i + 1, sendableMessages.size());

								if (dequeueAndSendMessage(sendableMessage))
									totalMessagesSent.incrementAndGet();
								else
									totalMessagesFailed.incrementAndGet();

								++i;
							}
						}
					});
				}

				if (totalMessagesProcessed.get() > 0)
					getLogger().info("Processed a total of {} message[s] in this send-message task invocation: {} sent and {} failed.",
							totalMessagesProcessed.get(), totalMessagesSent.get(), totalMessagesFailed.get());
			});
		}

		@Nonnull
		protected MessageService getMessageService() {
			return this.messageService;
		}

		@Nonnull
		protected MessageSender<EmailMessage> getEmailMessageSender() {
			return this.emailMessageSender;
		}

		@Nonnull
		protected EmailMessageSerializer getEmailMessageSerializer() {
			return this.emailMessageSerializer;
		}

		@Nonnull
		protected SmsMessageSerializer getSmsMessageSerializer() {
			return this.smsMessageSerializer;
		}

		@Nonnull
		protected CallMessageSerializer getCallMessageSerializer() {
			return this.callMessageSerializer;
		}

		@Nonnull
		protected PushMessageSerializer getPushMessageSerializer() {
			return this.pushMessageSerializer;
		}

		@Nonnull
		protected Database getDatabase() {
			return this.databaseProvider.get();
		}

		@Nonnull
		protected CurrentContextExecutor getCurrentContextExecutor() {
			return this.currentContextExecutor;
		}

		@Nonnull
		protected EnterprisePluginProvider getEnterprisePluginProvider() {
			return this.enterprisePluginProvider;
		}

		@Nonnull
		protected ErrorReporter getErrorReporter() {
			return this.errorReporter;
		}

		@Nonnull
		protected Formatter getFormatter() {
			return this.formatter;
		}

		@Nonnull
		protected Configuration getConfiguration() {
			return this.configuration;
		}

		@Nonnull
		protected Logger getLogger() {
			return this.logger;
		}
	}

	@ThreadSafe
	public static class ScheduledMessageTask implements Runnable {
		@Nonnull
		private final MessageService messageService;
		@Nonnull
		private final EmailMessageSerializer emailMessageSerializer;
		@Nonnull
		private final SmsMessageSerializer smsMessageSerializer;
		@Nonnull
		private final CallMessageSerializer callMessageSerializer;
		@Nonnull
		private final PushMessageSerializer pushMessageSerializer;
		@Nonnull
		private final DatabaseProvider databaseProvider;
		@Nonnull
		private final CurrentContextExecutor currentContextExecutor;
		@Nonnull
		private final ErrorReporter errorReporter;
		@Nonnull
		private final Formatter formatter;
		@Nonnull
		private final Configuration configuration;
		@Nonnull
		private final Logger logger;

		@Inject
		public ScheduledMessageTask(@Nonnull MessageService messageService,
																@Nonnull EmailMessageSerializer emailMessageSerializer,
																@Nonnull SmsMessageSerializer smsMessageSerializer,
																@Nonnull CallMessageSerializer callMessageSerializer,
																@Nonnull PushMessageSerializer pushMessageSerializer,
																@Nonnull DatabaseProvider databaseProvider,
																@Nonnull CurrentContextExecutor currentContextExecutor,
																@Nonnull ErrorReporter errorReporter,
																@Nonnull Formatter formatter,
																@Nonnull Configuration configuration) {
			requireNonNull(messageService);
			requireNonNull(emailMessageSerializer);
			requireNonNull(smsMessageSerializer);
			requireNonNull(callMessageSerializer);
			requireNonNull(pushMessageSerializer);
			requireNonNull(databaseProvider);
			requireNonNull(currentContextExecutor);
			requireNonNull(errorReporter);
			requireNonNull(formatter);
			requireNonNull(configuration);

			this.messageService = messageService;
			this.emailMessageSerializer = emailMessageSerializer;
			this.smsMessageSerializer = smsMessageSerializer;
			this.callMessageSerializer = callMessageSerializer;
			this.pushMessageSerializer = pushMessageSerializer;
			this.databaseProvider = databaseProvider;
			this.currentContextExecutor = currentContextExecutor;
			this.errorReporter = errorReporter;
			this.formatter = formatter;
			this.configuration = configuration;
			this.logger = LoggerFactory.getLogger(getClass());
		}

		@Override
		public void run() {
			CurrentContext currentContext = new CurrentContext.Builder(InstitutionId.COBALT,
					getConfiguration().getDefaultLocale(), getConfiguration().getDefaultTimeZone()).build();

			getCurrentContextExecutor().execute(currentContext, () -> {
				getDatabase().transaction(() -> {
					Instant now = Instant.now();

					// Anything scheduled for before this instant and in PENDING status can be sent
					List<ScheduledMessage> sendableScheduledMessages = getDatabase().queryForList("SELECT * FROM scheduled_message " +
							"WHERE scheduled_message_status_id=? AND TIMEZONE(time_zone, scheduled_at) <= ? " +
							"FOR UPDATE", ScheduledMessage.class, ScheduledMessageStatusId.PENDING, now);

					if (sendableScheduledMessages.size() == 0) {
						getLogger().trace("No scheduled messages need to be sent.");
						return;
					}

					getLogger().info("Detected {} scheduled message[s] that are ready to send, enqueuing for send now...", sendableScheduledMessages.size());
					int i = 0;

					for (ScheduledMessage scheduledMessage : sendableScheduledMessages) {
						getLogger().info("Enqueuing scheduled message {} of {}...", i + 1, sendableScheduledMessages.size());

						try {
							if (scheduledMessage.getMessageTypeId() == MessageTypeId.EMAIL) {
								EmailMessage emailMessage = getEmailMessageSerializer().deserializeMessage(scheduledMessage.getSerializedMessage());
								getMessageService().enqueueMessage(emailMessage);
							} else if (scheduledMessage.getMessageTypeId() == MessageTypeId.SMS) {
								SmsMessage smsMessage = getSmsMessageSerializer().deserializeMessage(scheduledMessage.getSerializedMessage());
								getMessageService().enqueueMessage(smsMessage);
							} else if (scheduledMessage.getMessageTypeId() == MessageTypeId.CALL) {
								CallMessage callMessage = getCallMessageSerializer().deserializeMessage(scheduledMessage.getSerializedMessage());
								getMessageService().enqueueMessage(callMessage);
							} else if (scheduledMessage.getMessageTypeId() == MessageTypeId.PUSH) {
								PushMessage pushMessage = getPushMessageSerializer().deserializeMessage(scheduledMessage.getSerializedMessage());
								getMessageService().enqueueMessage(pushMessage);
							} else {
								throw new IllegalStateException(format("Sorry, %s.%s is not yet supported.",
										MessageTypeId.class.getSimpleName(), scheduledMessage.getMessageTypeId().name()));
							}

							getDatabase().execute("UPDATE scheduled_message SET scheduled_message_status_id=?, " +
											"processed_at=NOW() WHERE scheduled_message_id=?", ScheduledMessageStatusId.PROCESSED,
									scheduledMessage.getScheduledMessageId());

							getLogger().info("Successfully enqueued scheduled message {} of {}.", i + 1, sendableScheduledMessages.size());
						} catch (Exception e) {
							getLogger().info(format("Unable to enqueue scheduled message %d of %d, sending error report...", i + 1, sendableScheduledMessages.size()), e);
							getErrorReporter().report(e);

							String stackTrace = getFormatter().formatStackTrace(e);
							getDatabase().execute("UPDATE scheduled_message SET scheduled_message_status_id=?, stack_trace=?, " +
											"errored_at=NOW() WHERE scheduled_message_id=?", ScheduledMessageStatusId.ERROR, stackTrace,
									scheduledMessage.getScheduledMessageId());
						} finally {
							++i;
						}
					}
				});
			});
		}

		@Nonnull
		protected MessageService getMessageService() {
			return this.messageService;
		}

		@Nonnull
		protected EmailMessageSerializer getEmailMessageSerializer() {
			return this.emailMessageSerializer;
		}

		@Nonnull
		protected SmsMessageSerializer getSmsMessageSerializer() {
			return this.smsMessageSerializer;
		}

		@Nonnull
		protected CallMessageSerializer getCallMessageSerializer() {
			return callMessageSerializer;
		}

		@Nonnull
		protected PushMessageSerializer getPushMessageSerializer() {
			return this.pushMessageSerializer;
		}

		@Nonnull
		protected Database getDatabase() {
			return this.databaseProvider.get();
		}

		@Nonnull
		protected CurrentContextExecutor getCurrentContextExecutor() {
			return currentContextExecutor;
		}

		@Nonnull
		protected ErrorReporter getErrorReporter() {
			return errorReporter;
		}

		@Nonnull
		protected Formatter getFormatter() {
			return formatter;
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

	@Nonnull
	protected Integer getMaximumSmsBodyCharacterCount() {
		return MAXIMUM_SMS_BODY_CHARACTER_COUNT;
	}

	@Nonnull
	protected Locale getFreeformMessageLocale() {
		return FREEFORM_MESSAGE_LOCALE;
	}

	@Nonnull
	protected Long getSendMessageTaskIntervalInSeconds() {
		return SEND_MESSAGE_TASK_INTERVAL_IN_SECONDS;
	}

	@Nonnull
	protected Long getSendMessageTaskInitialDelayInSeconds() {
		return SEND_MESSAGE_TASK_INITIAL_DELAY_IN_SECONDS;
	}

	@Nonnull
	protected Long getScheduledMessageTaskIntervalInSeconds() {
		return SCHEDULED_MESSAGE_TASK_INTERVAL_IN_SECONDS;
	}

	@Nonnull
	protected Long getScheduledMessageTaskInitialDelayInSeconds() {
		return SCHEDULED_MESSAGE_TASK_INITIAL_DELAY_IN_SECONDS;
	}

	@Nonnull
	protected Provider<SendMessageTask> getSendMessageTaskProvider() {
		return this.sendMessageTaskProvider;
	}

	@Nonnull
	protected Provider<ScheduledMessageTask> getScheduledMessageTaskProvider() {
		return scheduledMessageTaskProvider;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected EnterprisePluginProvider getEnterprisePluginProvider() {
		return this.enterprisePluginProvider;
	}

	@Nonnull
	protected EmailMessageSerializer getEmailMessageSerializer() {
		return this.emailMessageSerializer;
	}

	@Nonnull
	protected MessageSender<EmailMessage> getEmailMessageSender() {
		return this.emailMessageSender;
	}

	@Nonnull
	protected SmsMessageSerializer getSmsMessageSerializer() {
		return this.smsMessageSerializer;
	}

	@Nonnull
	protected CallMessageSerializer getCallMessageSerializer() {
		return this.callMessageSerializer;
	}

	@Nonnull
	protected PushMessageSerializer getPushMessageSerializer() {
		return this.pushMessageSerializer;
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
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
	protected JsonMapper getJsonMapper() {
		return jsonMapper;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected Object getLock() {
		return lock;
	}

	@Nonnull
	protected Optional<ScheduledExecutorService> getSendMessageTaskExecutorService() {
		return Optional.ofNullable(sendMessageTaskExecutorService);
	}

	@Nonnull
	protected Optional<ScheduledExecutorService> getScheduledMessageTaskExecutorService() {
		return Optional.ofNullable(scheduledMessageTaskExecutorService);
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
