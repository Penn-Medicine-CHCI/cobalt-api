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
import com.cobaltplatform.api.integration.way2health.Way2HealthClient;
import com.cobaltplatform.api.messaging.email.EmailMessageManager;
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
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class Way2HealthService implements AutoCloseable {
	@Nonnull
	private static final Long BACKGROUND_TASK_INTERVAL_IN_SECONDS;
	@Nonnull
	private static final Long BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS;

	@Nonnull
	private final Database database;
	@Nonnull
	private final Way2HealthClient way2HealthClient;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Provider<BackgroundSyncTask> backgroundSyncTaskProvider;
	@Nonnull
	private final Object backgroundTaskLock;
	@Nonnull
	private final Logger logger;

	@Nonnull
	private Boolean backgroundTaskStarted;
	@Nullable
	private ScheduledExecutorService backgroundTaskExecutorService;

	static {
		BACKGROUND_TASK_INTERVAL_IN_SECONDS = 60L;
		BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS = 10L;
	}

	@Inject
	public Way2HealthService(@Nonnull Way2HealthClient way2HealthClient,
													 @Nonnull Database database,
													 @Nonnull Strings strings,
													 @Nonnull Provider<BackgroundSyncTask> backgroundSyncTaskProvider) {
		requireNonNull(way2HealthClient);
		requireNonNull(database);
		requireNonNull(strings);
		requireNonNull(backgroundSyncTaskProvider);

		this.way2HealthClient = way2HealthClient;
		this.database = database;
		this.strings = strings;
		this.backgroundTaskLock = new Object();
		this.backgroundTaskStarted = false;
		this.backgroundSyncTaskProvider = backgroundSyncTaskProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public void close() throws Exception {
		stopBackgroundTask();
	}

	@Nonnull
	public Boolean startBackgroundTask() {
		synchronized (getBackgroundTaskLock()) {
			if (isBackgroundTaskStarted())
				return false;

			getLogger().trace("Starting group session background task...");

			this.backgroundTaskExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("way2health-background-task").build());
			this.backgroundTaskStarted = true;

			getBackgroundTaskExecutorService().get().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						getBackgroundSyncTaskProvider().get().run();
					} catch (Exception e) {
						getLogger().warn(format("Unable to complete group session background task - will retry in %s seconds", String.valueOf(getBackgroundTaskIntervalInSeconds())), e);
					}
				}
			}, getBackgroundTaskInitialDelayInSeconds(), getBackgroundTaskIntervalInSeconds(), TimeUnit.SECONDS);

			getLogger().trace("Group session background task started.");

			return true;
		}
	}

	@Nonnull
	public Boolean stopBackgroundTask() {
		synchronized (getBackgroundTaskLock()) {
			if (!isBackgroundTaskStarted())
				return false;

			getLogger().trace("Stopping group session background task...");

			getBackgroundTaskExecutorService().get().shutdownNow();
			this.backgroundTaskExecutorService = null;
			this.backgroundTaskStarted = false;

			getLogger().trace("Group session background task stopped.");

			return true;
		}
	}

	@ThreadSafe
	protected static class BackgroundSyncTask implements Runnable {
		@Nonnull
		private final CurrentContextExecutor currentContextExecutor;
		@Nonnull
		private final EmailMessageManager emailMessageManager;
		@Nonnull
		private final ErrorReporter errorReporter;
		@Nonnull
		private final Database database;
		@Nonnull
		private final Configuration configuration;
		@Nonnull
		private final Logger logger;

		@Inject
		public BackgroundSyncTask(@Nonnull CurrentContextExecutor currentContextExecutor,
															@Nonnull EmailMessageManager emailMessageManager,
															@Nonnull ErrorReporter errorReporter,
															@Nonnull Database database,
															@Nonnull Configuration configuration) {
			requireNonNull(currentContextExecutor);
			requireNonNull(emailMessageManager);
			requireNonNull(errorReporter);
			requireNonNull(database);
			requireNonNull(configuration);

			this.currentContextExecutor = currentContextExecutor;
			this.emailMessageManager = emailMessageManager;
			this.errorReporter = errorReporter;
			this.database = database;
			this.configuration = configuration;
			this.logger = LoggerFactory.getLogger(getClass());
		}

		@Override
		public void run() {
			CurrentContext currentContext = new CurrentContext.Builder(getConfiguration().getDefaultLocale(), getConfiguration().getDefaultTimeZone()).build();

			getCurrentContextExecutor().execute(currentContext, () -> {
				try {
					// TODO: pull from W2H
				} catch (Exception e) {
					getLogger().error("Unable to pull data from Way2Health", e);
					getErrorReporter().report(e);
				}

				// TODO: process data pulled from W2H
			});
		}

		@Nonnull
		protected CurrentContextExecutor getCurrentContextExecutor() {
			return currentContextExecutor;
		}

		@Nonnull
		protected EmailMessageManager getEmailMessageManager() {
			return emailMessageManager;
		}

		@Nonnull
		protected ErrorReporter getErrorReporter() {
			return errorReporter;
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
		protected Logger getLogger() {
			return logger;
		}
	}

	@Nonnull
	public Boolean isBackgroundTaskStarted() {
		synchronized (getBackgroundTaskLock()) {
			return backgroundTaskStarted;
		}
	}

	@Nonnull
	protected Long getBackgroundTaskIntervalInSeconds() {
		return BACKGROUND_TASK_INTERVAL_IN_SECONDS;
	}

	@Nonnull
	protected Long getBackgroundTaskInitialDelayInSeconds() {
		return BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS;
	}

	@Nonnull
	protected Way2HealthClient getWay2HealthClient() {
		return way2HealthClient;
	}

	@Nonnull
	protected Database getDatabase() {
		return database;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected Provider<BackgroundSyncTask> getBackgroundSyncTaskProvider() {
		return backgroundSyncTaskProvider;
	}

	@Nonnull
	protected Object getBackgroundTaskLock() {
		return backgroundTaskLock;
	}

	@Nonnull
	protected Optional<ScheduledExecutorService> getBackgroundTaskExecutorService() {
		return Optional.ofNullable(backgroundTaskExecutorService);
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
