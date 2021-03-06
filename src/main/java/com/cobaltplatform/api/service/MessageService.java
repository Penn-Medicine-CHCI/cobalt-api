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
import com.cobaltplatform.api.messaging.Message;
import com.cobaltplatform.api.messaging.call.CallMessage;
import com.cobaltplatform.api.messaging.call.CallMessageManager;
import com.cobaltplatform.api.messaging.call.CallMessageSerializer;
import com.cobaltplatform.api.messaging.call.CallMessageTemplate;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageManager;
import com.cobaltplatform.api.messaging.email.EmailMessageSerializer;
import com.cobaltplatform.api.messaging.sms.SmsMessage;
import com.cobaltplatform.api.messaging.sms.SmsMessageManager;
import com.cobaltplatform.api.messaging.sms.SmsMessageSerializer;
import com.cobaltplatform.api.messaging.sms.SmsMessageTemplate;
import com.cobaltplatform.api.model.api.request.CreateScheduledMessageRequest;
import com.cobaltplatform.api.model.api.request.SendCallMessagesRequest;
import com.cobaltplatform.api.model.api.request.SendCallMessagesRequest.SendCallMessageRequest;
import com.cobaltplatform.api.model.api.request.SendSmsMessagesRequest;
import com.cobaltplatform.api.model.api.request.SendSmsMessagesRequest.SendSmsMessageRequest;
import com.cobaltplatform.api.model.db.MessageType.MessageTypeId;
import com.cobaltplatform.api.model.db.ScheduledMessage;
import com.cobaltplatform.api.model.db.ScheduledMessageStatus.ScheduledMessageStatusId;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.Normalizer;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lokalized.Strings;
import com.pyranid.Database;
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
	private final Provider<ScheduledMessageTask> scheduledMessageTaskProvider;
	@Nonnull
	private final EmailMessageSerializer emailMessageSerializer;
	@Nonnull
	private final SmsMessageManager smsMessageManager;
	@Nonnull
	private final SmsMessageSerializer smsMessageSerializer;
	@Nonnull
	private final CallMessageManager callMessageManager;
	@Nonnull
	private final CallMessageSerializer callMessageSerializer;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Normalizer normalizer;
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Object lock;
	@Nonnull
	private final Logger logger;

	@Nonnull
	private Boolean started;
	@Nullable
	private ScheduledExecutorService scheduledMessageTaskExecutorService;

	static {
		MAXIMUM_SMS_BODY_CHARACTER_COUNT = 1_600;
		FREEFORM_MESSAGE_LOCALE = Locale.forLanguageTag("en-US");
		SCHEDULED_MESSAGE_TASK_INTERVAL_IN_SECONDS = 15L;
		SCHEDULED_MESSAGE_TASK_INITIAL_DELAY_IN_SECONDS = 1L;
	}

	@Inject
	public MessageService(@Nonnull Provider<ScheduledMessageTask> scheduledMessageTaskProvider,
												@Nonnull EmailMessageSerializer emailMessageSerializer,
												@Nonnull SmsMessageManager smsMessageManager,
												@Nonnull SmsMessageSerializer smsMessageSerializer,
												@Nonnull CallMessageManager callMessageManager,
												@Nonnull CallMessageSerializer callMessageSerializer,
												@Nonnull Database database,
												@Nonnull Formatter formatter,
												@Nonnull Normalizer normalizer,
												@Nonnull JsonMapper jsonMapper,
												@Nonnull Strings strings) {
		requireNonNull(scheduledMessageTaskProvider);
		requireNonNull(emailMessageSerializer);
		requireNonNull(smsMessageManager);
		requireNonNull(smsMessageSerializer);
		requireNonNull(callMessageManager);
		requireNonNull(callMessageSerializer);
		requireNonNull(database);
		requireNonNull(formatter);
		requireNonNull(normalizer);
		requireNonNull(jsonMapper);
		requireNonNull(strings);

		this.scheduledMessageTaskProvider = scheduledMessageTaskProvider;
		this.emailMessageSerializer = emailMessageSerializer;
		this.smsMessageManager = smsMessageManager;
		this.smsMessageSerializer = smsMessageSerializer;
		this.callMessageManager = callMessageManager;
		this.callMessageSerializer = callMessageSerializer;
		this.database = database;
		this.formatter = formatter;
		this.normalizer = normalizer;
		this.jsonMapper = jsonMapper;
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

			this.scheduledMessageTaskExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("scheduled-message-task-executor").build());

			this.started = true;

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

			getScheduledMessageTaskExecutorService().get().shutdownNow();
			this.scheduledMessageTaskExecutorService = null;

			started = false;

			getLogger().trace("Message service stopped.");
		}
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
		UUID messageId = message == null ? null : message.getMessageId();
		MessageTypeId messageTypeId = message == null ? null : message.getMessageTypeId();
		LocalDateTime scheduledAt = message == null ? null : request.getScheduledAt();
		ZoneId timeZone = message == null ? null : request.getTimeZone();
		Map<String, Object> metadata = request.getMetadata() == null ? null : request.getMetadata();
		String serializedMessage;

		ValidationException validationException = new ValidationException();

		if (messageId == null)
			validationException.add(new FieldError("messageId", getStrings().get("Message ID is required.")));

		if (messageTypeId == null)
			validationException.add(new FieldError("messageTypeId", getStrings().get("Message Type ID is required.")));

		if (scheduledAt == null)
			validationException.add(new FieldError("scheduledAt", getStrings().get("'Scheduled at' date/time is required.")));

		if (timeZone == null)
			validationException.add(new FieldError("timeZone", getStrings().get("Time zone is required.")));

		if (validationException.hasErrors())
			throw validationException;

		if (messageTypeId == MessageTypeId.EMAIL)
			serializedMessage = getEmailMessageSerializer().serializeMessage((EmailMessage) message);
		else if (messageTypeId == MessageTypeId.SMS)
			serializedMessage = getSmsMessageSerializer().serializeMessage((SmsMessage) message);
		else if (messageTypeId == MessageTypeId.CALL)
			serializedMessage = getCallMessageSerializer().serializeMessage((CallMessage) message);
		else
			throw new IllegalStateException(format("Sorry, %s.%s is not yet supported.",
					MessageTypeId.class.getSimpleName(), messageTypeId.name()));

		String metadataAsJson = metadata == null ? null : getJsonMapper().toJson(metadata);

		getLogger().info("Creating scheduled message of type {}, scheduled for {} {}.\nMetadata:\n{}\nSerialized form:\n{}",
				messageTypeId.name(), scheduledAt, timeZone.getId(), metadata == null ? "[none]" : metadataAsJson, serializedMessage);

		getDatabase().execute("INSERT INTO scheduled_message (scheduled_message_id, message_id, message_type_id, " +
						"serialized_message, scheduled_at, time_zone, metadata) VALUES (?,?,?,CAST(? AS JSONB),?,?,CAST(? AS JSONB))",
				scheduledMessageId, messageId, messageTypeId, serializedMessage, scheduledAt, timeZone, metadataAsJson);

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
	public Boolean isStarted() {
		synchronized (getLock()) {
			return started;
		}
	}

	@ThreadSafe
	public static class ScheduledMessageTask implements Runnable {
		@Nonnull
		private final EmailMessageSerializer emailMessageSerializer;
		@Nonnull
		private final EmailMessageManager emailMessageManager;
		@Nonnull
		private final SmsMessageManager smsMessageManager;
		@Nonnull
		private final SmsMessageSerializer smsMessageSerializer;
		@Nonnull
		private final CallMessageManager callMessageManager;
		@Nonnull
		private final CallMessageSerializer callMessageSerializer;
		@Nonnull
		private final Database database;
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
		public ScheduledMessageTask(@Nonnull EmailMessageManager emailMessageManager,
																@Nonnull EmailMessageSerializer emailMessageSerializer,
																@Nonnull SmsMessageManager smsMessageManager,
																@Nonnull SmsMessageSerializer smsMessageSerializer,
																@Nonnull CallMessageManager callMessageManager,
																@Nonnull CallMessageSerializer callMessageSerializer,
																@Nonnull Database database,
																@Nonnull CurrentContextExecutor currentContextExecutor,
																@Nonnull ErrorReporter errorReporter,
																@Nonnull Formatter formatter,
																@Nonnull Configuration configuration) {
			requireNonNull(emailMessageManager);
			requireNonNull(emailMessageSerializer);
			requireNonNull(smsMessageManager);
			requireNonNull(smsMessageSerializer);
			requireNonNull(callMessageManager);
			requireNonNull(callMessageSerializer);
			requireNonNull(database);
			requireNonNull(currentContextExecutor);
			requireNonNull(errorReporter);
			requireNonNull(formatter);
			requireNonNull(configuration);

			this.emailMessageManager = emailMessageManager;
			this.emailMessageSerializer = emailMessageSerializer;
			this.smsMessageManager = smsMessageManager;
			this.smsMessageSerializer = smsMessageSerializer;
			this.callMessageManager = callMessageManager;
			this.callMessageSerializer = callMessageSerializer;
			this.database = database;
			this.currentContextExecutor = currentContextExecutor;
			this.errorReporter = errorReporter;
			this.formatter = formatter;
			this.configuration = configuration;
			this.logger = LoggerFactory.getLogger(getClass());
		}

		@Override
		public void run() {
			CurrentContext currentContext = new CurrentContext.Builder(getConfiguration().getDefaultLocale(),
					getConfiguration().getDefaultTimeZone()).build();

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
								getEmailMessageManager().enqueueMessage(emailMessage);
							} else if (scheduledMessage.getMessageTypeId() == MessageTypeId.SMS) {
								SmsMessage smsMessage = getSmsMessageSerializer().deserializeMessage(scheduledMessage.getSerializedMessage());
								getSmsMessageManager().enqueueMessage(smsMessage);
							} else if (scheduledMessage.getMessageTypeId() == MessageTypeId.CALL) {
								CallMessage callMessage = getCallMessageSerializer().deserializeMessage(scheduledMessage.getSerializedMessage());
								getCallMessageManager().enqueueMessage(callMessage);
							} else {
								throw new IllegalStateException(format("Sorry, %s.%s is not yet supported.",
										MessageTypeId.class.getSimpleName(), scheduledMessage.getMessageTypeId().name()));
							}

							getDatabase().execute("UPDATE scheduled_message SET scheduled_message_status_id=?, " +
											"processed_at=NOW() WHERE scheduled_message_id=?", ScheduledMessageStatusId.PROCESSED,
									scheduledMessage.getScheduledMessageId());

							getLogger().info("Successfully enqueued scheduled message {} of {}.", i + 1, sendableScheduledMessages.size());
						} catch (Exception e) {
							getLogger().info(format("Unable to enqueued scheduled message %d of %d, sending error report...", i + 1, sendableScheduledMessages.size()), e);
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
		protected EmailMessageManager getEmailMessageManager() {
			return emailMessageManager;
		}

		@Nonnull
		protected EmailMessageSerializer getEmailMessageSerializer() {
			return emailMessageSerializer;
		}

		@Nonnull
		protected SmsMessageManager getSmsMessageManager() {
			return smsMessageManager;
		}

		@Nonnull
		protected SmsMessageSerializer getSmsMessageSerializer() {
			return smsMessageSerializer;
		}

		@Nonnull
		protected CallMessageManager getCallMessageManager() {
			return callMessageManager;
		}

		@Nonnull
		protected CallMessageSerializer getCallMessageSerializer() {
			return callMessageSerializer;
		}

		@Nonnull
		protected Database getDatabase() {
			return database;
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
	protected Long getScheduledMessageTaskIntervalInSeconds() {
		return SCHEDULED_MESSAGE_TASK_INTERVAL_IN_SECONDS;
	}

	@Nonnull
	protected Long getScheduledMessageTaskInitialDelayInSeconds() {
		return SCHEDULED_MESSAGE_TASK_INITIAL_DELAY_IN_SECONDS;
	}

	@Nonnull
	protected Provider<ScheduledMessageTask> getScheduledMessageTaskProvider() {
		return scheduledMessageTaskProvider;
	}

	@Nonnull
	protected EmailMessageSerializer getEmailMessageSerializer() {
		return emailMessageSerializer;
	}

	@Nonnull
	protected SmsMessageManager getSmsMessageManager() {
		return smsMessageManager;
	}

	@Nonnull
	protected SmsMessageSerializer getSmsMessageSerializer() {
		return smsMessageSerializer;
	}

	@Nonnull
	protected CallMessageManager getCallMessageManager() {
		return callMessageManager;
	}

	@Nonnull
	protected CallMessageSerializer getCallMessageSerializer() {
		return callMessageSerializer;
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
	protected Optional<ScheduledExecutorService> getScheduledMessageTaskExecutorService() {
		return Optional.ofNullable(scheduledMessageTaskExecutorService);
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
