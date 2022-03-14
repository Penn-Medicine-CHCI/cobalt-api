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

package com.cobaltplatform.api.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Common code to handle long-polling/message processing for an SQS queue.
 * <p>
 * Note: while long-polling thread count is configurable, normally you should have only *1* long-polling thread and
 * at least 1 processing thread.
 *
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AmazonSqsManager implements AutoCloseable {
	@Nonnull
	private final String queueName;
	@Nonnull
	private final Boolean useLocalstack;
	@Nonnull
	private final Integer localstackPort;
	@Nonnull
	private final AmazonSqsProcessingFunction processingFunction;
	@Nonnull
	private final Integer pollingThreadCount;
	@Nonnull
	private final ThreadFactory pollingThreadFactory;
	@Nonnull
	private final Integer processingThreadCount;
	@Nonnull
	private final ThreadFactory processingThreadFactory;
	@Nullable
	private final Integer queueWaitTimeSeconds;
	@Nonnull
	private final BlockingQueue<Message> messageQueue;
	@Nonnull
	private final Object lock;
	@Nonnull
	private final Logger logger;

	// Mutable state (based on start/stop)

	@Nonnull
	private Boolean started;
	@Nonnull
	private SqsClient amazonSqs;
	@Nonnull
	private Region region;
	@Nullable
	private String queueUrl;
	@Nullable
	private ExecutorService pollingExecutorService;
	@Nullable
	private ExecutorService processingExecutorService;

	protected AmazonSqsManager(@Nonnull Builder builder) {
		requireNonNull(builder);

		this.queueName = builder.queueName;
		this.region = builder.region;
		this.useLocalstack = builder.useLocalstack == null ? false : builder.useLocalstack;
		this.localstackPort = builder.localstackPort == null ? 4566 : builder.localstackPort;
		this.processingFunction = builder.processingFunction == null ? (ignored) -> {
		} : builder.processingFunction;
		this.pollingThreadCount = builder.pollingThreadCount == null ? 1 : builder.pollingThreadCount;
		this.pollingThreadFactory = builder.pollingThreadFactory == null ? new ThreadFactoryBuilder().setNameFormat("sqs-polling-task-%d").build() : builder.pollingThreadFactory;
		this.processingThreadCount = builder.processingThreadCount == null ? 3 : builder.processingThreadCount;
		this.processingThreadFactory = builder.processingThreadFactory == null ? new ThreadFactoryBuilder().setNameFormat("sqs-processing-task-%d").build() : builder.processingThreadFactory;
		this.queueWaitTimeSeconds = builder.queueWaitTimeSeconds;
		this.messageQueue = new LinkedBlockingQueue<>();
		this.lock = new Object();
		this.started = false;
		this.logger = LoggerFactory.getLogger(getClass());

		if (pollingThreadCount < 0)
			throw new IllegalArgumentException("Polling thread count must be >= 0");

		if (processingThreadCount < 0)
			throw new IllegalArgumentException("Processing thread count must be >= 0");
	}

	@Override
	public void close() throws Exception {
		stop();
	}

	@Nonnull
	public Boolean start() {
		synchronized (getLock()) {
			if (isStarted())
				return false;

			this.amazonSqs = createAmazonSqs();
			this.queueUrl = determineQueueUrl();
			this.pollingExecutorService = createPollingExecutorService();
			this.processingExecutorService = createProcessingExecutorService();

			this.started = true;

			for (int i = 0; i < getPollingThreadCount(); ++i)
				getPollingExecutorService().get().submit(new PollingTask(this));

			for (int i = 0; i < getProcessingThreadCount(); ++i)
				getProcessingExecutorService().get().submit(new ProcessingTask(this));

			return true;
		}
	}

	@Nonnull
	public Boolean stop() {
		synchronized (getLock()) {
			if (!isStarted())
				return false;

			started = false;

			// Need explicit shutdown in order to have SQS client's internal socket[s] tear down long polling.
			// It's not sufficient to interrupt the thread
			getAmazonSqs().get().close();

			getPollingExecutorService().get().shutdownNow();
			pollingExecutorService = null;

			getProcessingExecutorService().get().shutdownNow();
			processingExecutorService = null;

			this.amazonSqs = null;
			this.queueUrl = null;

			return true;
		}
	}

	@Nonnull
	public Boolean isStarted() {
		synchronized (getLock()) {
			return started;
		}
	}

	@Nonnull
	public SendMessageResponse sendMessage(@Nonnull Function<String, SendMessageRequest> sendMessageFunction) {
		requireNonNull(sendMessageFunction);

		if (!isStarted())
			throw new IllegalStateException("Can't send a message if the SQS manager is not started");

		// Give caller the Queue URL so caller can construct a SendMessageRequest
		SendMessageRequest sendMessageRequest = sendMessageFunction.apply(getQueueUrl().get());

		if (sendMessageRequest == null)
			throw new IllegalStateException("Can't send a null message request");

		return getAmazonSqs().get().sendMessage(sendMessageRequest);
	}

	@Nonnull
	public DeleteMessageResponse deleteMessage(@Nonnull String messageReceiptHandle) {
		requireNonNull(messageReceiptHandle);
		getLogger().trace("Deleting message with receipt handle {}", messageReceiptHandle);
		return getAmazonSqs().get().deleteMessage(DeleteMessageRequest.builder().queueUrl(getQueueUrl().get()).receiptHandle(messageReceiptHandle).build());
	}

	@Nonnull
	protected ExecutorService createPollingExecutorService() {
		return Executors.newFixedThreadPool(getPollingThreadCount(), getPollingThreadFactory());
	}

	@Nonnull
	protected ExecutorService createProcessingExecutorService() {
		return Executors.newFixedThreadPool(getProcessingThreadCount(), getProcessingThreadFactory());
	}

	@Nonnull
	protected SqsClient createAmazonSqs() {
		SqsClientBuilder builder = SqsClient.builder()
				.region(region);
		if (getUseLocalstack()) {
			builder.endpointOverride(URI.create(format("http://localhost:%d", localstackPort)));
		}
		return builder.build();
	}

	@Nonnull
	protected String determineQueueUrl() {
		try {
			return getAmazonSqs().get().getQueueUrl(GetQueueUrlRequest.builder().queueName(getQueueName()).build()).queueUrl();
		} catch (QueueDoesNotExistException e) {
			getLogger().warn("Could not find SQS queue with name '{}'", getQueueName());
			throw e;
		}
	}

	@ThreadSafe
	protected static class PollingTask implements Runnable {
		@Nonnull
		private final AmazonSqsManager amazonSqsManager;
		@Nonnull
		private final Logger logger;

		public PollingTask(@Nonnull AmazonSqsManager amazonSqsManager) {
			requireNonNull(amazonSqsManager);

			this.amazonSqsManager = amazonSqsManager;
			this.logger = LoggerFactory.getLogger(getClass());
		}

		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					ReceiveMessageRequest.Builder receiveMessageRequestBuilder = ReceiveMessageRequest.builder()
							.queueUrl(getAmazonSqsManager().getQueueUrl().get());

					// Localstack does not support this long-poll configuration
					if (!getAmazonSqsManager().getUseLocalstack()) {
						Integer queueWaitTimeSeconds = getAmazonSqsManager().getQueueWaitTimeSeconds().orElse(null);

						if (queueWaitTimeSeconds != null)
							receiveMessageRequestBuilder.waitTimeSeconds(queueWaitTimeSeconds);
					}

					SqsClient amazonSqs = getAmazonSqsManager().getAmazonSqs().orElse(null);
					List<Message> messages = Collections.emptyList();

					// Might be null in small time window where we are shutting down
					if (amazonSqs != null) {
						ReceiveMessageResponse receiveMessageResult = amazonSqs.receiveMessage(receiveMessageRequestBuilder.build());
						messages = receiveMessageResult.messages();
					}

					for (Message message : messages) {
						getAmazonSqsManager().getMessageQueue().put(message);
					}
				} catch (AbortedException e) {
					if (Thread.currentThread().isInterrupted()) {
						getLogger().trace("SQS long-polling stopped, exiting long-polling task...");
						return;
					} else {
						getLogger().warn("SQS long-polling was aborted but thread was NOT interrupted - we are in a bad state", e);
					}
				} catch (Exception e) {
					// Unfortunately, seems to be no better way to determine that AmazonSQS client was shut down during long-poll blocking
					if (e instanceof IllegalStateException && "Connection pool shut down".equals(e.getMessage())) {
						getLogger().trace("SQS long-polling stopped, exiting long-polling task...");
						return;
					} else {
						getLogger().warn("Exception occurred while polling SQS", e);
					}
				}
			}
		}

		@Nonnull
		protected AmazonSqsManager getAmazonSqsManager() {
			return amazonSqsManager;
		}

		@Nonnull
		protected Logger getLogger() {
			return logger;
		}
	}

	@ThreadSafe
	protected static class ProcessingTask implements Runnable {
		@Nonnull
		private final AmazonSqsManager amazonSqsPoller;
		@Nonnull
		private final Logger logger;

		public ProcessingTask(@Nonnull AmazonSqsManager amazonSqsPoller) {
			requireNonNull(amazonSqsPoller);

			this.amazonSqsPoller = amazonSqsPoller;
			this.logger = LoggerFactory.getLogger(getClass());
		}

		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					Message message = getAmazonSqsPoller().getMessageQueue().take();
					getAmazonSqsPoller().getProcessingFunction().process(message);
				} catch (InterruptedException e) {
					getLogger().trace("Message processing thread was interrupted, exiting.");
				} catch (Exception e) {
					getLogger().warn("Exception occurred while processing SQS message", e);
				}
			}
		}

		@Nonnull
		protected AmazonSqsManager getAmazonSqsPoller() {
			return amazonSqsPoller;
		}

		@Nonnull
		protected Logger getLogger() {
			return logger;
		}
	}

	@FunctionalInterface
	public interface AmazonSqsProcessingFunction {
		void process(@Nonnull Message message) throws Exception;
	}

	@NotThreadSafe
	public static class Builder {
		@Nonnull
		private final String queueName;
		@Nonnull
		private final Region region;
		@Nullable
		private Boolean useLocalstack;
		@Nullable
		private Integer localstackPort;
		@Nullable
		private AmazonSqsProcessingFunction processingFunction;
		@Nullable
		private Integer pollingThreadCount;
		@Nullable
		private ThreadFactory pollingThreadFactory;
		@Nullable
		private Integer processingThreadCount;
		@Nullable
		private ThreadFactory processingThreadFactory;
		@Nullable
		private Integer queueWaitTimeSeconds;

		public Builder(@Nonnull String queueName,
									 @Nonnull Region region) {
			requireNonNull(queueName);
			requireNonNull(region);

			this.queueName = queueName;
			this.region = region;
		}

		public Builder useLocalstack(@Nullable Boolean useLocalstack) {
			this.useLocalstack = useLocalstack;
			return this;
		}

		public Builder localstackPort(@Nullable Integer localstackPort) {
			this.localstackPort = localstackPort;
			return this;
		}

		public Builder processingFunction(@Nullable AmazonSqsProcessingFunction processingFunction) {
			this.processingFunction = processingFunction;
			return this;
		}

		public Builder pollingThreadCount(@Nullable Integer pollingThreadCount) {
			this.pollingThreadCount = pollingThreadCount;
			return this;
		}

		public Builder pollingThreadFactory(@Nullable ThreadFactory pollingThreadFactory) {
			this.pollingThreadFactory = pollingThreadFactory;
			return this;
		}

		public Builder processingThreadCount(@Nullable Integer processingThreadCount) {
			this.processingThreadCount = processingThreadCount;
			return this;
		}

		public Builder processingThreadFactory(@Nullable ThreadFactory processingThreadFactory) {
			this.processingThreadFactory = processingThreadFactory;
			return this;
		}

		public Builder queueWaitTimeSeconds(@Nullable Integer queueWaitTimeSeconds) {
			this.queueWaitTimeSeconds = queueWaitTimeSeconds;
			return this;
		}

		public AmazonSqsManager build() {
			return new AmazonSqsManager(this);
		}
	}

	@Nonnull
	protected String getQueueName() {
		return queueName;
	}

	@Nonnull
	protected Region getRegion() {
		return region;
	}

	@Nonnull
	protected Boolean getUseLocalstack() {
		return useLocalstack;
	}

	@Nonnull
	protected AmazonSqsProcessingFunction getProcessingFunction() {
		return processingFunction;
	}

	@Nonnull
	protected ThreadFactory getPollingThreadFactory() {
		return pollingThreadFactory;
	}

	@Nonnull
	protected ThreadFactory getProcessingThreadFactory() {
		return processingThreadFactory;
	}

	@Nonnull
	protected BlockingQueue<Message> getMessageQueue() {
		return messageQueue;
	}

	@Nonnull
	protected Optional<ExecutorService> getPollingExecutorService() {
		return Optional.ofNullable(pollingExecutorService);
	}

	@Nonnull
	protected Optional<ExecutorService> getProcessingExecutorService() {
		return Optional.ofNullable(processingExecutorService);
	}

	@Nonnull
	protected Integer getPollingThreadCount() {
		return pollingThreadCount;
	}

	@Nonnull
	protected Integer getProcessingThreadCount() {
		return processingThreadCount;
	}

	@Nullable
	protected Optional<Integer> getQueueWaitTimeSeconds() {
		return Optional.ofNullable(queueWaitTimeSeconds);
	}

	@Nonnull
	protected Object getLock() {
		return lock;
	}

	@Nullable
	protected Optional<SqsClient> getAmazonSqs() {
		return Optional.ofNullable(amazonSqs);
	}

	@Nullable
	protected Optional<String> getQueueUrl() {
		return Optional.ofNullable(queueUrl);
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
