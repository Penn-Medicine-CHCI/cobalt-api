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
import com.cobaltplatform.api.integration.enterprise.EnterprisePlugin;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.model.db.CronJob;
import com.cobaltplatform.api.model.db.CronJobRunStatus.CronJobRunStatusId;
import com.cobaltplatform.api.model.db.FootprintEventGroupType.FootprintEventGroupTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
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
import java.time.ZonedDateTime;
import java.util.List;
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
public class CronService implements AutoCloseable {
	@Nonnull
	private static final Long BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS;
	@Nonnull
	private static final Long BACKGROUND_TASK_INTERVAL_IN_SECONDS;

	@Nonnull
	private final Provider<BackgroundTask> backgroundTaskProvider;
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final ErrorReporter errorReporter;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final Object backgroundTaskLock;
	@Nonnull
	private Boolean backgroundTaskStarted;
	@Nullable
	private ScheduledExecutorService backgroundTaskExecutorService;

	static {
		BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS = 10L;
		BACKGROUND_TASK_INTERVAL_IN_SECONDS = 30L;
	}

	@Inject
	public CronService(@Nonnull Provider<BackgroundTask> backgroundTaskProvider,
										 @Nonnull DatabaseProvider databaseProvider,
										 @Nonnull ErrorReporter errorReporter,
										 @Nonnull Configuration configuration,
										 @Nonnull Strings strings) {
		requireNonNull(backgroundTaskProvider);
		requireNonNull(databaseProvider);
		requireNonNull(errorReporter);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.backgroundTaskProvider = backgroundTaskProvider;
		this.databaseProvider = databaseProvider;
		this.errorReporter = errorReporter;
		this.configuration = configuration;
		this.strings = strings;
		this.backgroundTaskLock = new Object();
		this.backgroundTaskStarted = false;
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

			getLogger().trace("Starting cron background task...");

			this.backgroundTaskExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("cron-task").build());
			this.backgroundTaskStarted = true;

			getBackgroundTaskExecutorService().get().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						getBackgroundTaskProvider().get().run();
					} catch (Exception e) {
						getLogger().warn(format("Unable to complete cron background task - will retry in %s seconds", String.valueOf(BACKGROUND_TASK_INTERVAL_IN_SECONDS), e));
					}
				}
			}, getBackgroundTaskInitialDelayInSeconds(), BACKGROUND_TASK_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);

			getLogger().trace("Cron background task started.");

			return true;
		}
	}

	@Nonnull
	public Boolean stopBackgroundTask() {
		synchronized (getBackgroundTaskLock()) {
			if (!isBackgroundTaskStarted())
				return false;

			getLogger().trace("Stopping cron background task...");

			getBackgroundTaskExecutorService().get().shutdownNow();
			this.backgroundTaskExecutorService = null;
			this.backgroundTaskStarted = false;

			getLogger().trace("Cron background task stopped.");

			return true;
		}
	}

	@ThreadSafe
	protected static class BackgroundTask implements Runnable {
		@Nonnull
		private final Provider<SystemService> systemServiceProvider;
		@Nonnull
		private final EnterprisePluginProvider enterprisePluginProvider;
		@Nonnull
		private final CurrentContextExecutor currentContextExecutor;
		@Nonnull
		private final ErrorReporter errorReporter;
		@Nonnull
		private final Formatter formatter;
		@Nonnull
		private final DatabaseProvider databaseProvider;
		@Nonnull
		private final Configuration configuration;
		@Nonnull
		private final Logger logger;

		@Inject
		public BackgroundTask(@Nonnull Provider<SystemService> systemServiceProvider,
													@Nonnull EnterprisePluginProvider enterprisePluginProvider,
													@Nonnull CurrentContextExecutor currentContextExecutor,
													@Nonnull ErrorReporter errorReporter,
													@Nonnull Formatter formatter,
													@Nonnull DatabaseProvider databaseProvider,
													@Nonnull Configuration configuration) {
			requireNonNull(systemServiceProvider);
			requireNonNull(enterprisePluginProvider);
			requireNonNull(currentContextExecutor);
			requireNonNull(errorReporter);
			requireNonNull(formatter);
			requireNonNull(databaseProvider);
			requireNonNull(configuration);

			this.systemServiceProvider = systemServiceProvider;
			this.enterprisePluginProvider = enterprisePluginProvider;
			this.currentContextExecutor = currentContextExecutor;
			this.errorReporter = errorReporter;
			this.formatter = formatter;
			this.databaseProvider = databaseProvider;
			this.configuration = configuration;
			this.logger = LoggerFactory.getLogger(getClass());
		}

		@Override
		public void run() {
			CurrentContext currentContext = new CurrentContext.Builder(InstitutionId.COBALT,
					getConfiguration().getDefaultLocale(), getConfiguration().getDefaultTimeZone()).build();

			getCurrentContextExecutor().execute(currentContext, () -> {
				try {
					getDatabase().transaction(() -> {
						getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.CRON_JOB);

						List<CronJob> dueCronJobs = getDatabase().queryForList("""
								    SELECT *
								    FROM cron_job
								    WHERE next_run_at <= now()
								    AND enabled=TRUE
								    ORDER BY next_run_at
								    FOR UPDATE SKIP LOCKED
								    LIMIT 5
								""", CronJob.class);

						// Note: keep cron job "run" time very short so we don't hold this txn open for a long time!
						// If there is a longer-running task like firing off a materialized view refresh, fire off a separate
						// thread (in your EnterprisePlugin::runCronJob implementation)
						for (CronJob cronJob : dueCronJobs) {
							try {
								run(cronJob);
								markSuccess(cronJob);
							} catch (Throwable t) {
								markFailure(cronJob, t);
							}
						}
					});
				} catch (Exception e) {
					getLogger().error("Unable to complete cron background task", e);
					getErrorReporter().report(e);
				}
			});
		}

		protected void run(@Nonnull CronJob cronJob) {
			getLogger().info("Running cron job ID {} ({} for {})...", cronJob.getCronJobId(), cronJob.getCallbackType(), cronJob.getInstitutionId());
			requireNonNull(cronJob);

			// Immediately attempt a write to the cron record before running the job.
			// This will ensure there is no issue writing to the database (gives confidence we can mark the run as finished after executing it).
			// For example, suppose we are unintentionally operating on a read-replica.  This would fail-fast and the cron job would never be run.
			// If this write did not occur, the cron job could potentially be run and then fail to be marked as completed, causing repeated re-runs.
			getDatabase().execute("""
							UPDATE
								cron_job
							SET
								next_run_at=NULL,
								last_run_started_at=?,
								last_run_finished_at=NULL,
								last_run_status_id=?,
								last_run_stack_trace=NULL
							WHERE
								cron_job_id=?
							""",
					Instant.now(), CronJobRunStatusId.UNDEFINED, cronJob.getCronJobId());

			// Delegate execution to the enterprise plugin
			EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(cronJob.getInstitutionId());
			enterprisePlugin.runCronJob(cronJob);
		}

		protected void markSuccess(@Nonnull CronJob cronJob) {
			requireNonNull(cronJob);

			Instant lastRunFinishedAt = Instant.now();
			Instant nextRunAt = null;

			// In the event of a malformed cron expression which prevents us from figuring out the next run,
			// set to null and send an error report.
			try {
				nextRunAt = calculateNextRunAt(cronJob);
			} catch (Exception e) {
				getErrorReporter().report(e);
			}

			getDatabase().execute("""
							UPDATE
								cron_job
							SET
								next_run_at=?,
								last_run_finished_at=?,
								last_run_status_id=?,
								last_run_stack_trace=NULL
							WHERE
								cron_job_id=?
							""",
					nextRunAt, lastRunFinishedAt, CronJobRunStatusId.SUCCEEDED, cronJob.getCronJobId());
		}

		protected void markFailure(@Nonnull CronJob cronJob,
															 @Nonnull Throwable throwable) {
			requireNonNull(cronJob);
			requireNonNull(throwable);

			getLogger().error(format("Unable to complete cron job '%s' for institution ID %s", cronJob.getCallbackType(), cronJob.getInstitutionId()), throwable);
			getErrorReporter().report(throwable);

			Instant lastRunFinishedAt = Instant.now();
			Instant nextRunAt = null;

			// In the event of a malformed cron expression which prevents us from figuring out the next run,
			// set to null and send an error report.
			try {
				nextRunAt = calculateNextRunAt(cronJob);
			} catch (Exception e) {
				getErrorReporter().report(e);
			}

			getDatabase().execute("""
							UPDATE
								cron_job
							SET
								next_run_at=?,
								last_run_finished_at=?,
								last_run_status_id=?,
								last_run_stack_trace=?
							WHERE
								cron_job_id=?
							""",
					nextRunAt, lastRunFinishedAt, CronJobRunStatusId.FAILED, getFormatter().formatStackTrace(throwable), cronJob.getCronJobId());
		}

		@Nonnull
		protected Instant calculateNextRunAt(@Nonnull CronJob cronJob) {
			requireNonNull(cronJob);

			CronParser cronParser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
			ExecutionTime executionTime;

			try {
				Cron cron = cronParser.parse(cronJob.getCronExpression());
				executionTime = ExecutionTime.forCron(cron);
			} catch (Exception e) {
				throw new RuntimeException(format("Unable to process cron expression '%s' for cron job ID %s", cronJob.getCronExpression(), cronJob.getCronJobId()), e);
			}

			return executionTime.nextExecution(ZonedDateTime.ofInstant(Instant.now(), cronJob.getTimeZone()))
					.map(ZonedDateTime::toInstant)
					.orElseThrow(() -> new IllegalStateException("Unreachable – invalid cron"));
		}

		@Nonnull
		protected SystemService getSystemService() {
			return this.systemServiceProvider.get();
		}

		@Nonnull
		protected EnterprisePluginProvider getEnterprisePluginProvider() {
			return this.enterprisePluginProvider;
		}

		@Nonnull
		protected CurrentContextExecutor getCurrentContextExecutor() {
			return this.currentContextExecutor;
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
		protected Database getDatabase() {
			return this.databaseProvider.get();
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

	@Nonnull
	public Boolean isBackgroundTaskStarted() {
		synchronized (getBackgroundTaskLock()) {
			return this.backgroundTaskStarted;
		}
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected ErrorReporter getErrorReporter() {
		return this.errorReporter;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	protected Long getBackgroundTaskInitialDelayInSeconds() {
		return BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS;
	}

	@Nonnull
	protected Object getBackgroundTaskLock() {
		return this.backgroundTaskLock;
	}

	@Nonnull
	protected Provider<BackgroundTask> getBackgroundTaskProvider() {
		return this.backgroundTaskProvider;
	}

	@Nonnull
	protected Optional<ScheduledExecutorService> getBackgroundTaskExecutorService() {
		return Optional.ofNullable(this.backgroundTaskExecutorService);
	}
}