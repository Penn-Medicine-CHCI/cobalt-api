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

package com.cobaltplatform.api.messaging;


import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.model.db.MessageStatus.MessageStatusId;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.util.AmazonSqsManager;
import com.cobaltplatform.api.util.Formatter;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Provider;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class MessageManager<T extends Message> implements AutoCloseable {
	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final MessageSender<T> messageSender;
	@Nonnull
	private final MessageSerializer<T> messageSerializer;
	@Nonnull
	private final Function<AmazonSqsManager.AmazonSqsProcessingFunction, AmazonSqsManager> amazonSqsManagerProvider;
	@Nonnull
	private final Object lock;
	@Nonnull
	private final Logger logger;

	@Nonnull
	private Boolean started;
	@Nullable
	private AmazonSqsManager amazonSqsManager;

	public MessageManager(@Nonnull Provider<AccountService> accountServiceProvider,
												@Nonnull Database database,
												@Nonnull Configuration configuration,
												@Nonnull Formatter formatter,
												@Nonnull MessageSender<T> messageSender,
												@Nonnull MessageSerializer<T> messageSerializer,
												@Nonnull Function<AmazonSqsManager.AmazonSqsProcessingFunction, AmazonSqsManager> amazonSqsManagerProvider) {
		requireNonNull(accountServiceProvider);
		requireNonNull(database);
		requireNonNull(configuration);
		requireNonNull(formatter);
		requireNonNull(messageSender);
		requireNonNull(messageSerializer);
		requireNonNull(amazonSqsManagerProvider);

		this.accountServiceProvider = accountServiceProvider;
		this.database = database;
		this.configuration = configuration;
		this.formatter = formatter;
		this.messageSender = messageSender;
		this.messageSerializer = messageSerializer;
		this.amazonSqsManagerProvider = amazonSqsManagerProvider;
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
				getLogger().warn("Message manager already started, ignoring request to start...");
				return;
			}

			getLogger().trace("Starting message processor...");

			this.amazonSqsManager = getAmazonSqsManagerProvider().apply((message -> {
				handleDequeuedSqsMessage(message);
			}));

			getAmazonSqsManager().get().start();

			started = true;

			getLogger().trace("Message manager started.");
		}
	}

	public void stop() {
		synchronized (getLock()) {
			if (!isStarted()) {
				getLogger().warn("Message manager already stopped, ignoring request to stop...");
				return;
			}

			getLogger().trace("Stopping message manager...");

			started = false;

			getAmazonSqsManager().get().stop();
			amazonSqsManager = null;

			getLogger().trace("Message manager stopped.");
		}
	}

	/**
	 * Puts a message (email, SMS, etc.) on an SQS queue for later sending.
	 */
	@Nonnull
	public void enqueueMessage(@Nonnull T message) {
		requireNonNull(message);

		if (!isStarted())
			throw new IllegalStateException("Message manager is not started, cannot enqueue messages");

		String serializedMessage = getMessageSerializer().serializeMessage(message);

		getAmazonSqsManager().get().sendMessage((queueUrl) -> {
			// Special handling for FIFO/deduplication support
			return SendMessageRequest.builder()
					.queueUrl(queueUrl)
					.messageGroupId(message.getMessageId().toString())
					.messageDeduplicationId(message.getMessageId().toString())
					.messageBody(serializedMessage)
					.build();
		});

		getDatabase().execute("INSERT INTO message_log (message_id, message_type_id, message_status_id, message_vendor_id, serialized_message, enqueued) VALUES (?,?,?,?,CAST(? AS JSONB),NOW())",
				message.getMessageId(), message.getMessageTypeId(), MessageStatusId.ENQUEUED, getMessageSender().getMessageVendorId(), serializedMessage);
	}

	/**
	 * Handles a message (email, SMS, etc.) from an SQS queue (performs actual send operation).
	 */
	protected void handleDequeuedSqsMessage(@Nonnull software.amazon.awssdk.services.sqs.model.Message sqsMessage) throws Exception {
		requireNonNull(sqsMessage);

		T deserializedMessage = getMessageSerializer().deserializeMessage(sqsMessage.body());

		try {
			getMessageSender().sendMessage(deserializedMessage);

			try {
				getDatabase().execute("UPDATE message_log SET message_status_id=?, processed=NOW() WHERE message_id=?",
						MessageStatusId.SENT, deserializedMessage.getMessageId());
			} catch (Exception e2) {
				// Not much we can do, just bail
				getLogger().warn("Unable to update message log", e2);
			}
		} catch (Exception e) {
			try {
				String stackTrace = getFormatter().formatStackTrace(e);
				getDatabase().execute("UPDATE message_log SET message_status_id=?, processed=NOW(), stack_trace=? WHERE message_id=?",
						MessageStatusId.ERROR, stackTrace, deserializedMessage.getMessageId());
			} catch (Exception e2) {
				// Not much we can do, just bail
				getLogger().warn("Unable to update message log", e2);
			}

			throw e;
		} finally {
			// Always delete from the SQS queue regardless of whether the message was sent, no retry logic atm.
			// This avoids scenarios where we erroneously retry forever in a tight loop.
			// TODO: add in capped number of retries with exponential backoff
			getAmazonSqsManager().get().deleteMessage(sqsMessage.receiptHandle());
		}
	}

	@Nonnull
	protected AccountService getAccountService() {
		return accountServiceProvider.get();
	}

	@Nonnull
	protected Database getDatabase() {
		return database;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return formatter;
	}

	@Nonnull
	public MessageSender<T> getMessageSender() {
		return messageSender;
	}

	@Nonnull
	protected MessageSerializer<T> getMessageSerializer() {
		return messageSerializer;
	}

	@Nonnull
	protected Object getLock() {
		return lock;
	}

	@Nonnull
	protected Boolean isStarted() {
		synchronized (getLock()) {
			return started;
		}
	}

	@Nonnull
	protected Optional<AmazonSqsManager> getAmazonSqsManager() {
		return Optional.ofNullable(amazonSqsManager);
	}

	@Nonnull
	protected Function<AmazonSqsManager.AmazonSqsProcessingFunction, AmazonSqsManager> getAmazonSqsManagerProvider() {
		return amazonSqsManagerProvider;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}