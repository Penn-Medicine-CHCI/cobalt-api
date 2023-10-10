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
import com.cobaltplatform.api.integration.enterprise.EnterprisePlugin;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.integration.google.GoogleAnalyticsDataClient;
import com.cobaltplatform.api.integration.google.GoogleBigQueryClient;
import com.cobaltplatform.api.integration.mixpanel.MixpanelClient;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.service.AdvisoryLock;
import com.google.analytics.data.v1beta.DateRange;
import com.google.analytics.data.v1beta.Dimension;
import com.google.analytics.data.v1beta.Metric;
import com.google.analytics.data.v1beta.Row;
import com.google.analytics.data.v1beta.RunReportRequest;
import com.google.analytics.data.v1beta.RunReportResponse;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class AnalyticsService implements AutoCloseable {
	@Nonnull
	private static final Long ANALYTICS_SYNC_INTERVAL_IN_SECONDS;
	@Nonnull
	private static final Long ANALYTICS_SYNC_INITIAL_DELAY_IN_SECONDS;

	static {
		ANALYTICS_SYNC_INTERVAL_IN_SECONDS = 60L * 5L;
		ANALYTICS_SYNC_INITIAL_DELAY_IN_SECONDS = 10L;
	}

	@Nonnull
	private final Provider<SystemService> systemServiceProvider;
	@Nonnull
	private final Provider<AnalyticsSyncTask> analyticsSyncTaskProvider;
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Object analyticsSyncLock;
	@Nonnull
	private final Logger logger;

	@Nonnull
	private Boolean started;
	@Nullable
	private ScheduledExecutorService analyticsSyncExecutorService;

	@Inject
	public AnalyticsService(@Nonnull Provider<SystemService> systemServiceProvider,
													@Nonnull Provider<AnalyticsSyncTask> analyticsSyncTaskProvider,
													@Nonnull EnterprisePluginProvider enterprisePluginProvider,
													@Nonnull Database database,
													@Nonnull Strings strings) {
		requireNonNull(systemServiceProvider);
		requireNonNull(analyticsSyncTaskProvider);
		requireNonNull(enterprisePluginProvider);
		requireNonNull(database);
		requireNonNull(strings);

		this.systemServiceProvider = systemServiceProvider;
		this.analyticsSyncTaskProvider = analyticsSyncTaskProvider;
		this.enterprisePluginProvider = enterprisePluginProvider;
		this.database = database;
		this.strings = strings;
		this.analyticsSyncLock = new Object();
		this.started = false;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public void close() throws Exception {
		stopAnalyticsSync();
	}

	@Nonnull
	public Boolean startAnalyticsSync() {
		synchronized (getAnalyticsSyncLock()) {
			if (isStarted())
				return false;

			getLogger().trace("Starting analytics sync...");

			this.analyticsSyncExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("analytics-sync-task").build());

			this.started = true;

			getAnalyticsSyncExecutorService().get().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						getSystemService().performAdvisoryLockOperationIfAvailable(AdvisoryLock.ANALYTICS_SYNC, () -> {
							getAnalyticsSyncTaskProvider().get().run();
						});
					} catch (Exception e) {
						getLogger().warn(format("Unable to sync analytics - will retry in %s seconds", String.valueOf(getAnalyticsSyncIntervalInSeconds())), e);
					}
				}
			}, getAnalyticsSyncInitialDelayInSeconds(), getAnalyticsSyncIntervalInSeconds(), TimeUnit.SECONDS);

			getLogger().trace("Analytics sync started.");

			return true;
		}
	}

	@Nonnull
	public Boolean stopAnalyticsSync() {
		synchronized (getAnalyticsSyncLock()) {
			if (!isStarted())
				return false;

			getLogger().trace("Stopping analytics sync...");

			getAnalyticsSyncExecutorService().get().shutdownNow();
			this.analyticsSyncExecutorService = null;

			this.started = false;

			getLogger().trace("Analytics sync stopped.");

			return true;
		}
	}

	@Nonnull
	public AnalyticsResultNewVersusReturning activeUserCountsNewVersusReturning(@Nonnull InstitutionId institutionId,
																																							@Nonnull LocalDate startDate,
																																							@Nonnull LocalDate endDate) {
		requireNonNull(institutionId);
		requireNonNull(startDate);
		requireNonNull(endDate);

		GoogleAnalyticsDataClient googleAnalyticsDataClient = googleAnalyticsDataClientForInstitutionId(institutionId);

		RunReportRequest request = RunReportRequest.newBuilder()
				.setProperty(format("properties/%s", googleAnalyticsDataClient.getGa4PropertyId()))
				.addDimensions(Dimension.newBuilder().setName("newVsReturning"))
				.addMetrics(Metric.newBuilder().setName("activeUsers"))
				.addDateRanges(DateRange.newBuilder()
						.setStartDate(DateTimeFormatter.ISO_LOCAL_DATE.format(startDate))
						.setEndDate(DateTimeFormatter.ISO_LOCAL_DATE.format(endDate)))
				.build();

		RunReportResponse response = googleAnalyticsDataClient.runReport(request);

		Long newActiveUsers = 0L;
		Long returningActiveUsers = 0L;
		Long otherActiveUsers = 0L;

		for (Row row : response.getRowsList()) {
			String dimensionName = row.getDimensionValuesCount() > 0 ? row.getDimensionValues(0).getValue() : null;
			Long metricCount = row.getMetricValuesCount() > 0 ? Long.valueOf(row.getMetricValues(0).getValue()) : 0;

			if (dimensionName == null)
				continue;

			if ("new".equals(dimensionName)) {
				newActiveUsers = metricCount;
			} else if ("returning".equals(dimensionName)) {
				returningActiveUsers = metricCount;
			} else if ("(not set)".equals(dimensionName)) {
				otherActiveUsers = metricCount;
			} else {
				getLogger().warn("Unrecognized dimension name '{}' (metric value {})", dimensionName, metricCount);
			}
		}

		return new AnalyticsResultNewVersusReturning(newActiveUsers, returningActiveUsers, otherActiveUsers);
	}

	@Nonnull
	public Map<AccountSourceId, Long> activeUserCountsByAccountSourceId(@Nonnull InstitutionId institutionId,
																																			@Nonnull LocalDate startDate,
																																			@Nonnull LocalDate endDate) {
		requireNonNull(institutionId);
		requireNonNull(startDate);
		requireNonNull(endDate);

		Map<AccountSourceId, Long> activeUserCountsByAccountSourceId = new HashMap<>();

		// TODO: implement

		return activeUserCountsByAccountSourceId;
	}

	@Nonnull
	protected GoogleAnalyticsDataClient googleAnalyticsDataClientForInstitutionId(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);
		return getEnterprisePluginProvider().enterprisePluginForInstitutionId(institutionId).googleAnalyticsDataClient();
	}

	@Nonnull
	protected GoogleBigQueryClient googleBigQueryClientForInstitutionId(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);
		return getEnterprisePluginProvider().enterprisePluginForInstitutionId(institutionId).googleBigQueryClient();
	}

	@ThreadSafe
	public static class AnalyticsResultNewVersusReturning {
		@Nonnull
		private final Long newActiveUsers;
		@Nonnull
		private final Long returningActiveUsers;
		@Nonnull
		private final Long otherActiveUsers;

		public AnalyticsResultNewVersusReturning(@Nonnull Long newActiveUsers,
																						 @Nonnull Long returningActiveUsers,
																						 @Nonnull Long otherActiveUsers) {
			requireNonNull(newActiveUsers);
			requireNonNull(returningActiveUsers);
			requireNonNull(otherActiveUsers);

			this.newActiveUsers = newActiveUsers;
			this.returningActiveUsers = returningActiveUsers;
			this.otherActiveUsers = otherActiveUsers;
		}

		@Nonnull
		public Long getNewActiveUsers() {
			return this.newActiveUsers;
		}

		@Nonnull
		public Long getReturningActiveUsers() {
			return this.returningActiveUsers;
		}

		@Nonnull
		public Long getOtherActiveUsers() {
			return this.otherActiveUsers;
		}
	}

	@ThreadSafe
	protected static class AnalyticsSyncTask implements Runnable {
		@Nonnull
		private final Provider<AnalyticsService> analyticsServiceProvider;
		@Nonnull
		private final Provider<InstitutionService> institutionServiceProvider;
		@Nonnull
		private final EnterprisePluginProvider enterprisePluginProvider;
		@Nonnull
		private final CurrentContextExecutor currentContextExecutor;
		@Nonnull
		private final Database database;
		@Nonnull
		private final Configuration configuration;
		@Nonnull
		private final Logger logger;

		@Inject
		public AnalyticsSyncTask(@Nonnull Provider<AnalyticsService> analyticsServiceProvider,
														 @Nonnull Provider<InstitutionService> institutionServiceProvider,
														 @Nonnull EnterprisePluginProvider enterprisePluginProvider,
														 @Nonnull CurrentContextExecutor currentContextExecutor,
														 @Nonnull Database database,
														 @Nonnull Configuration configuration) {
			requireNonNull(analyticsServiceProvider);
			requireNonNull(institutionServiceProvider);
			requireNonNull(enterprisePluginProvider);
			requireNonNull(currentContextExecutor);
			requireNonNull(database);
			requireNonNull(configuration);

			this.analyticsServiceProvider = analyticsServiceProvider;
			this.institutionServiceProvider = institutionServiceProvider;
			this.currentContextExecutor = currentContextExecutor;
			this.enterprisePluginProvider = enterprisePluginProvider;
			this.database = database;
			this.configuration = configuration;
			this.logger = LoggerFactory.getLogger(getClass());
		}

		@Override
		public void run() {
			List<Institution> institutions = getDatabase().queryForList("""
					     SELECT *
					     FROM institution
					     WHERE google_bigquery_sync_enabled=TRUE OR mixpanel_sync_enabled=TRUE
					     ORDER BY institution_id
					""", Institution.class);

			for (Institution institution : institutions) {
				getLogger().trace("Analytics sync starting for {}...", institution.getInstitutionId().name());

				CurrentContext currentContext = new CurrentContext.Builder(institution.getInstitutionId(),
						getConfiguration().getDefaultLocale(), getConfiguration().getDefaultTimeZone()).build();

				try {
					getCurrentContextExecutor().execute(currentContext, () -> {
						EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(institution.getInstitutionId());

						if (institution.getMixpanelSyncEnabled()) {
							MixpanelClient mixpanelClient = enterprisePlugin.mixpanelClient();
							// TODO: sync Mixpanel
						}

						if (institution.getGoogleBigQuerySyncEnabled()) {
							GoogleBigQueryClient googleBigQueryClient = enterprisePlugin.googleBigQueryClient();
							// TODO: sync BigQuery
						}
					});
				} finally {
					getLogger().trace("Analytics sync complete for {}.", institution.getInstitutionId().name());
				}
			}
		}

		@Nonnull
		protected AnalyticsService getAnalyticsService() {
			return this.analyticsServiceProvider.get();
		}

		@Nonnull
		protected InstitutionService getInstitutionService() {
			return this.institutionServiceProvider.get();
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
		protected Database getDatabase() {
			return this.database;
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
	protected Long getAnalyticsSyncInitialDelayInSeconds() {
		return ANALYTICS_SYNC_INITIAL_DELAY_IN_SECONDS;
	}

	@Nonnull
	protected Long getAnalyticsSyncIntervalInSeconds() {
		return ANALYTICS_SYNC_INTERVAL_IN_SECONDS;
	}

	@Nonnull
	protected SystemService getSystemService() {
		return this.systemServiceProvider.get();
	}

	@Nonnull
	protected Provider<AnalyticsSyncTask> getAnalyticsSyncTaskProvider() {
		return this.analyticsSyncTaskProvider;
	}

	@Nonnull
	protected EnterprisePluginProvider getEnterprisePluginProvider() {
		return this.enterprisePluginProvider;
	}

	@Nonnull
	protected Database getDatabase() {
		return this.database;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Object getAnalyticsSyncLock() {
		return this.analyticsSyncLock;
	}

	@Nonnull
	protected Boolean isStarted() {
		return this.started;
	}

	@Nonnull
	protected Optional<ScheduledExecutorService> getAnalyticsSyncExecutorService() {
		return Optional.ofNullable(this.analyticsSyncExecutorService);
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
