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
import com.cobaltplatform.api.integration.google.GoogleAnalyticsDataClient;
import com.cobaltplatform.api.integration.google.GoogleBigQueryClient;
import com.cobaltplatform.api.integration.google.GoogleBigQueryExportRecord;
import com.cobaltplatform.api.integration.mixpanel.MixpanelClient;
import com.cobaltplatform.api.integration.mixpanel.MixpanelEvent;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.AnalyticsEventDateSync;
import com.cobaltplatform.api.model.db.AnalyticsGoogleBigQueryEvent;
import com.cobaltplatform.api.model.db.AnalyticsSyncStatus.AnalyticsSyncStatusId;
import com.cobaltplatform.api.model.db.AnalyticsVendor.AnalyticsVendorId;
import com.cobaltplatform.api.model.db.Feature;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;
import com.cobaltplatform.api.model.service.AdvisoryLock;
import com.cobaltplatform.api.util.ValidationException;
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
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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
	private final Provider<InstitutionService> institutionServiceProvider;
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
	public AnalyticsService(@Nonnull Provider<InstitutionService> institutionServiceProvider,
													@Nonnull Provider<SystemService> systemServiceProvider,
													@Nonnull Provider<AnalyticsSyncTask> analyticsSyncTaskProvider,
													@Nonnull EnterprisePluginProvider enterprisePluginProvider,
													@Nonnull Database database,
													@Nonnull Strings strings) {
		requireNonNull(institutionServiceProvider);
		requireNonNull(systemServiceProvider);
		requireNonNull(analyticsSyncTaskProvider);
		requireNonNull(enterprisePluginProvider);
		requireNonNull(database);
		requireNonNull(strings);

		this.institutionServiceProvider = institutionServiceProvider;
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

	/**
	 * Active "new vs returning" user counts: GA4 provides this data.
	 */
	@Nonnull
	public AnalyticsResultNewVersusReturning findActiveUserCountsNewVersusReturning(@Nonnull InstitutionId institutionId,
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

	/**
	 * Active user counts: at least one BigQuery event for an account ID during the date range.
	 */
	@Nonnull
	public Map<AccountSourceId, Long> findActiveUserCountsByAccountSourceId(@Nonnull InstitutionId institutionId,
																																					@Nonnull LocalDate startDate,
																																					@Nonnull LocalDate endDate) {
		requireNonNull(institutionId);
		requireNonNull(startDate);
		requireNonNull(endDate);

		if (endDate.isBefore(startDate))
			throw new ValidationException(getStrings().get("End date cannot be before start date."));

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		Instant startTimestamp = LocalDateTime.of(startDate, LocalTime.MIN).atZone(institution.getTimeZone()).toInstant();
		Instant endTimestamp = LocalDateTime.of(endDate, LocalTime.MAX).atZone(institution.getTimeZone()).toInstant();

		List<AccountSourceIdCount> accountSourceIdCounts = getDatabase().queryForList("""
				SELECT COUNT(DISTINCT a.account_id) AS count, a.account_source_id
				FROM account a, v_analytics_account_interaction aai
				WHERE a.account_id=aai.account_id
				AND aai.activity_timestamp BETWEEN ? AND ?
				AND aai.institution_id=?
				GROUP BY a.account_source_id
				""", AccountSourceIdCount.class, startTimestamp, endTimestamp, institutionId);

		Map<AccountSourceId, Long> activeUserCountsByAccountSourceId = new HashMap<>();

		for (AccountSourceIdCount accountSourceIdCount : accountSourceIdCounts)
			activeUserCountsByAccountSourceId.put(accountSourceIdCount.getAccountSourceId(), accountSourceIdCount.getCount());

		return activeUserCountsByAccountSourceId;
	}

	/**
	 * Section counts: groupings of "Sections" and how many page views/all users/active users
	 */
	@Nonnull
	public List<SectionCountSummary> findSectionCountSummaries(@Nonnull InstitutionId institutionId,
																														 @Nonnull LocalDate startDate,
																														 @Nonnull LocalDate endDate) {
		requireNonNull(institutionId);
		requireNonNull(startDate);
		requireNonNull(endDate);

		if (endDate.isBefore(startDate))
			throw new ValidationException(getStrings().get("End date cannot be before start date."));

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		Instant startTimestamp = LocalDateTime.of(startDate, LocalTime.MIN).atZone(institution.getTimeZone()).toInstant();
		Instant endTimestamp = LocalDateTime.of(endDate, LocalTime.MAX).atZone(institution.getTimeZone()).toInstant();

		List<Feature> features = getDatabase().queryForList("""
				SELECT f.feature_id, f.navigation_header_id, COALESCE(if.name_override, f.name) AS name, url_name
				FROM feature f, institution_feature if
				WHERE if.feature_id=f.feature_id
				AND if.institution_id=?
				ORDER BY url_name
				""", Feature.class, institution.getInstitutionId());

		// GA only reliably provides absolute URLs in its data, so we need to discard the prefix to find and work with the url_path.
		// For example, "https://www.cobaltplatform.com/group-sessions?abc=123" has url_path "/group-sessions?abc=123".
		// We do this by getting the webapp base URL and using it as part of a regex.
		String webappBaseUrl = getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institutionId, UserExperienceTypeId.PATIENT).get();
		String urlPathRegex = format("^%s", webappBaseUrl);

		// Sections are:
		// * Hardcoded "Sign In"
		// * Hardcoded "Home Page"
		// * Dynamic list of institution feature names, e.g. "Group Sessions" and "Therapy"
		final String SIGN_IN_SECTION = getStrings().get("Sign In");
		final String HOME_PAGE_SECTION = getStrings().get("Home Page");

		List<String> sections = new ArrayList<>();
		sections.add(SIGN_IN_SECTION);
		sections.add(HOME_PAGE_SECTION);
		sections.addAll(features.stream()
				.map(feature -> feature.getName())
				.collect(Collectors.toList()));

		String pageViewSql = """
				WITH aai AS (
				    select
				        *, regexp_replace(url, ?, '') as url_path
				    from
				        v_analytics_account_interaction
				    where
				        activity='page_view'
				        and activity_timestamp BETWEEN ? AND ?
				        and institution_id=?
				)
				""";

		List<Object> pageViewParameters = new ArrayList<>();
		pageViewParameters.add(urlPathRegex);
		pageViewParameters.add(startTimestamp);
		pageViewParameters.add(endTimestamp);
		pageViewParameters.add(institutionId);

		List<String> pageViewsSectionSqls = new ArrayList<>();

		// Home
		pageViewsSectionSqls.add("""							
						SELECT ? AS section, COUNT(aai.*) AS count
						FROM aai
						WHERE (url_path = '/' OR url_path LIKE '/?%')
						GROUP BY section
				""".trim());

		pageViewParameters.add(HOME_PAGE_SECTION);

		// Features
		for (Feature feature : features) {
			pageViewsSectionSqls.add("""							
							SELECT COALESCE(if.name_override, f.name) AS section, COUNT(aai.*) AS count
							FROM aai, feature f, institution_feature if
							WHERE f.feature_id=?
							AND if.feature_id=f.feature_id
							AND if.institution_id=aai.institution_id
							AND aai.url_path LIKE CONCAT(f.url_name, '%')
							GROUP BY section
					""".trim());

			pageViewParameters.add(feature.getFeatureId());
		}

		pageViewSql = pageViewSql + pageViewsSectionSqls.stream().collect(Collectors.joining("\nUNION\n"));

		List<SectionCount> pageViewSectionCounts = getDatabase().queryForList(pageViewSql, SectionCount.class, pageViewParameters.toArray(new Object[]{}));

		// Sign In page views - needs special handling.
		// We query for this differently because there is no signed-in user, so can't use v_analytics_account_interaction.
		// We just want the raw sign-in page views
		SectionCount signInPageViewSectionCount = getDatabase().queryForObject("""
				   WITH agbe AS (
				     SELECT regexp_replace(event->'parameters'->'page_location'->>'value', ?, '') as url_path
				     FROM analytics_google_bigquery_event
				     WHERE name='page_view'
				     AND institution_id=?
				     AND timestamp BETWEEN ? AND ?
				   )
				   SELECT ? AS section, COUNT(*) AS count
				   FROM agbe
				   WHERE url_path = '/sign-in'
				   OR url_path LIKE '/sign-in?%'
				   GROUP BY section
				""", SectionCount.class, urlPathRegex, institutionId, startTimestamp, endTimestamp, SIGN_IN_SECTION).orElse(null);

		// No data at all?  Count is zero
		if (signInPageViewSectionCount == null) {
			signInPageViewSectionCount = new SectionCount();
			signInPageViewSectionCount.setSection(SIGN_IN_SECTION);
			signInPageViewSectionCount.setCount(0L);
		}

		pageViewSectionCounts.add(signInPageViewSectionCount);

		// TODO: query for these other kinds of section counts
		List<SectionCount> userSectionCounts = new ArrayList<>();
		List<SectionCount> activeUserSectionCounts = new ArrayList<>();

		List<SectionCountSummary> sectionCountSummaries = new ArrayList<>(sections.size());
		Map<String, SectionCountSummary> sectionCountSummariesBySection = new HashMap<>(sections.size());

		// Prime the list...
		for (String section : sections) {
			SectionCountSummary sectionCountSummary = new SectionCountSummary();
			sectionCountSummary.setSection(section);
			sectionCountSummary.setPageViewCount(0L);
			sectionCountSummary.setUserCount(0L);
			sectionCountSummary.setActiveUserCount(0L);
			sectionCountSummaries.add(sectionCountSummary);

			sectionCountSummariesBySection.put(section, sectionCountSummary);
		}

		// ...and fill in with each type of data
		for (SectionCount pageViewSectionCount : pageViewSectionCounts) {
			System.out.println(pageViewSectionCount);
			SectionCountSummary sectionCountSummary = sectionCountSummariesBySection.get(pageViewSectionCount.getSection());
			sectionCountSummary.setPageViewCount(pageViewSectionCount.getCount());
		}

		for (SectionCount userSectionCount : userSectionCounts) {
			SectionCountSummary sectionCountSummary = sectionCountSummariesBySection.get(userSectionCount.getSection());
			sectionCountSummary.setUserCount(userSectionCount.getCount());
		}

		for (SectionCount activeUserSectionCount : activeUserSectionCounts) {
			SectionCountSummary sectionCountSummary = sectionCountSummariesBySection.get(activeUserSectionCount.getSection());
			sectionCountSummary.setActiveUserCount(activeUserSectionCount.getCount());
		}

		return sectionCountSummaries;
	}

	@NotThreadSafe
	protected static class SectionCount {
		@Nullable
		private String section;
		@Nullable
		private Long count;

		@Nullable
		public String getSection() {
			return this.section;
		}

		public void setSection(@Nullable String section) {
			this.section = section;
		}

		@Nullable
		public Long getCount() {
			return this.count;
		}

		public void setCount(@Nullable Long count) {
			this.count = count;
		}
	}

	@NotThreadSafe
	public static class SectionCountSummary {
		@Nullable
		private String section;
		@Nullable
		private Long pageViewCount;
		@Nullable
		private Long userCount;
		@Nullable
		private Long activeUserCount;

		@Nullable
		public String getSection() {
			return this.section;
		}

		public void setSection(@Nullable String section) {
			this.section = section;
		}

		@Nullable
		public Long getPageViewCount() {
			return this.pageViewCount;
		}

		public void setPageViewCount(@Nullable Long pageViewCount) {
			this.pageViewCount = pageViewCount;
		}

		@Nullable
		public Long getUserCount() {
			return this.userCount;
		}

		public void setUserCount(@Nullable Long userCount) {
			this.userCount = userCount;
		}

		@Nullable
		public Long getActiveUserCount() {
			return this.activeUserCount;
		}

		public void setActiveUserCount(@Nullable Long activeUserCount) {
			this.activeUserCount = activeUserCount;
		}
	}

	@NotThreadSafe
	protected static class AccountSourceIdCount {
		@Nullable
		private Long count;
		@Nullable
		private AccountSourceId accountSourceId;

		@Nullable
		public Long getCount() {
			return this.count;
		}

		public void setCount(@Nullable Long count) {
			this.count = count;
		}

		@Nullable
		public AccountSourceId getAccountSourceId() {
			return this.accountSourceId;
		}

		public void setAccountSourceId(@Nullable AccountSourceId accountSourceId) {
			this.accountSourceId = accountSourceId;
		}
	}

	public void persistGoogleBigQueryEvents(@Nonnull InstitutionId institutionId,
																					@Nonnull LocalDate date,
																					@Nonnull List<GoogleBigQueryExportRecord> exportRecords) {
		requireNonNull(institutionId);
		requireNonNull(exportRecords);
		requireNonNull(date);

		getLogger().info("Persisting {} Google BigQuery events for {} on {}...", exportRecords.size(), institutionId.name(), date);

		// First, clear out anything that's already stored off for this institution/date
		getDatabase().execute("""
				DELETE FROM analytics_google_bigquery_event
				WHERE institution_id=?
				AND date=?
				""", institutionId, date);

		// Then, prepare for batch insert of events for the institution/date
		List<List<Object>> parameterGroups = new ArrayList<>(exportRecords.size());

		for (GoogleBigQueryExportRecord exportRecord : exportRecords) {
			UUID accountId = null;

			if (exportRecord.getUser().getUserId() != null)
				accountId = UUID.fromString(exportRecord.getUser().getUserId());

			List<Object> parameterGroup = new ArrayList<>(14);
			parameterGroup.add(institutionId);
			parameterGroup.add(accountId);
			parameterGroup.add(exportRecord.getUser().getUserPseudoId());
			parameterGroup.add(exportRecord.getEvent().getBundleSequenceId());
			parameterGroup.add(exportRecord.getEvent().getName());
			parameterGroup.add(date);
			parameterGroup.add(exportRecord.getEvent().getTimestamp());

			AnalyticsGoogleBigQueryEvent.Event.EventParamValue timestampEventParamValue = exportRecord.getEvent().getParameters().get("timestamp");
			parameterGroup.add(timestampEventParamValue == null ? null : Instant.ofEpochMilli((long) timestampEventParamValue.getValue()));

			parameterGroup.add(exportRecord.getEvent().toJson());
			parameterGroup.add(exportRecord.getUser().toJson());
			parameterGroup.add(exportRecord.getTrafficSource().toJson());
			parameterGroup.add(exportRecord.getCollectedTrafficSource().toJson());
			parameterGroup.add(exportRecord.getGeo().toJson());
			parameterGroup.add(exportRecord.getDevice().toJson());

			parameterGroups.add(parameterGroup);
		}

		getDatabase().executeBatch("""
				INSERT INTO analytics_google_bigquery_event (
				    institution_id,
				    account_id,
				    user_pseudo_id,
				    event_bundle_sequence_id,
				    name,
				    date,
				    timestamp,
				    timestamp_parameter,
				    event,
				    bigquery_user,
				    traffic_source,
				    collected_traffic_source,
				    geo,
				    device
				) VALUES (?,?,?,?,?,?,?,?,CAST(? AS JSONB),CAST(? AS JSONB),CAST(? AS JSONB),CAST(? AS JSONB),CAST(? AS JSONB),CAST(? AS JSONB))
				""", parameterGroups);

		getLogger().info("Successfully persisted {} Google BigQuery events for {} on {}.", exportRecords.size(), institutionId.name(), date);
	}

	public void persistMixpanelEvents(@Nonnull InstitutionId institutionId,
																		@Nonnull LocalDate date,
																		@Nonnull List<MixpanelEvent> mixpanelEvents) {
		requireNonNull(institutionId);
		requireNonNull(mixpanelEvents);
		requireNonNull(date);

		getLogger().info("Persisting {} Mixpanel events for {} on {}...", mixpanelEvents.size(), institutionId.name(), date);

		// First, clear out anything that's already stored off for this institution/date
		getDatabase().execute("""
				DELETE FROM analytics_mixpanel_event
				WHERE institution_id=?
				AND date=?
				""", institutionId, date);

		// Then, prepare for batch insert of events for the institution/date
		List<List<Object>> parameterGroups = new ArrayList<>(mixpanelEvents.size());

		for (MixpanelEvent mixpanelEvent : mixpanelEvents) {
			UUID accountId = null;

			if (mixpanelEvent.getUserId().isPresent())
				accountId = UUID.fromString(mixpanelEvent.getUserId().get());

			List<Object> parameterGroup = new ArrayList<>(9);
			parameterGroup.add(institutionId);
			parameterGroup.add(accountId);
			parameterGroup.add(mixpanelEvent.getDistinctId());
			parameterGroup.add(mixpanelEvent.getAnonId().orElse(null));
			parameterGroup.add(mixpanelEvent.getDeviceId());
			parameterGroup.add(mixpanelEvent.getEvent());
			parameterGroup.add(date);
			parameterGroup.add(mixpanelEvent.getTime());
			parameterGroup.add(mixpanelEvent.getPropertiesAsJson().get());

			parameterGroups.add(parameterGroup);
		}

		getDatabase().executeBatch("""
				INSERT INTO analytics_mixpanel_event (
				    institution_id,
				    account_id,
				    distinct_id,
				    anon_id,
				    device_id,
				    name,
				    date,
				    timestamp,
				    properties
				) VALUES (?,?,?,?,?,?,?,?,CAST(? AS JSONB))
				ON CONFLICT ON CONSTRAINT analytics_mixpanel_event_institution_id_distinct_id_name_ti_key DO NOTHING
				""", parameterGroups);

		getLogger().info("Successfully persisted {} Mixpanel events for {} on {}.", mixpanelEvents.size(), institutionId.name(), date);
	}

	@Nonnull
	protected GoogleAnalyticsDataClient googleAnalyticsDataClientForInstitutionId(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);
		return getEnterprisePluginProvider().enterprisePluginForInstitutionId(institutionId).googleAnalyticsDataClient();
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
		private final Provider<SystemService> systemServiceProvider;
		@Nonnull
		private final EnterprisePluginProvider enterprisePluginProvider;
		@Nonnull
		private final CurrentContextExecutor currentContextExecutor;
		@Nonnull
		private final ErrorReporter errorReporter;
		@Nonnull
		private final Database database;
		@Nonnull
		private final Configuration configuration;
		@Nonnull
		private final Logger logger;

		@Inject
		public AnalyticsSyncTask(@Nonnull Provider<AnalyticsService> analyticsServiceProvider,
														 @Nonnull Provider<InstitutionService> institutionServiceProvider,
														 @Nonnull Provider<SystemService> systemServiceProvider,
														 @Nonnull EnterprisePluginProvider enterprisePluginProvider,
														 @Nonnull CurrentContextExecutor currentContextExecutor,
														 @Nonnull ErrorReporter errorReporter,
														 @Nonnull Database database,
														 @Nonnull Configuration configuration) {
			requireNonNull(analyticsServiceProvider);
			requireNonNull(institutionServiceProvider);
			requireNonNull(systemServiceProvider);
			requireNonNull(enterprisePluginProvider);
			requireNonNull(currentContextExecutor);
			requireNonNull(errorReporter);
			requireNonNull(database);
			requireNonNull(configuration);

			this.analyticsServiceProvider = analyticsServiceProvider;
			this.institutionServiceProvider = institutionServiceProvider;
			this.systemServiceProvider = systemServiceProvider;
			this.enterprisePluginProvider = enterprisePluginProvider;
			this.currentContextExecutor = currentContextExecutor;
			this.errorReporter = errorReporter;
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

						if (institution.getMixpanelSyncEnabled())
							performMixpanelSync(institution, enterprisePlugin.mixpanelClient());

						if (institution.getGoogleBigQuerySyncEnabled())
							performGoogleBigQuerySync(institution, enterprisePlugin.googleBigQueryClient());
					});
				} finally {
					getLogger().trace("Analytics sync complete for {}.", institution.getInstitutionId().name());
				}
			}
		}

		protected void performMixpanelSync(@Nonnull Institution institution,
																			 @Nonnull MixpanelClient mixpanelClient) {
			// Note rate limits per https://docs.mixpanel.com/docs/other-bits/rate-limits
			// Raw Export API (endpoint: data.mixpanel.com/api/2.0/export):
			// * A maximum of 100 concurrent queries, 60 queries per hour, and 3 queries per second.
			performAnalyticsSync(institution, AnalyticsVendorId.MIXPANEL, institution.getMixpanelSyncStartsAt(), (date) -> {
				// Pull events for date
				return mixpanelClient.findEventsForDateRange(date, date);
			}, (date, mixpanelEvents) -> {
				// Persist events for date
				getAnalyticsService().persistMixpanelEvents(institution.getInstitutionId(), date, mixpanelEvents);
			});
		}

		protected void performGoogleBigQuerySync(@Nonnull Institution institution,
																						 @Nonnull GoogleBigQueryClient googleBigQueryClient) {
			performAnalyticsSync(institution, AnalyticsVendorId.GOOGLE_BIGQUERY, institution.getGoogleBigQuerySyncStartsAt(), (date) -> {
				// Pull events for date
				List<GoogleBigQueryExportRecord> exportRecords = googleBigQueryClient.performRestApiQueryForExport(format("""
								SELECT *
								FROM `{{datasetId}}.events_*`
								WHERE _TABLE_SUFFIX BETWEEN '%s' AND '%s'
								""",
						googleBigQueryClient.dateAsTableSuffix(date),
						googleBigQueryClient.dateAsTableSuffix(date)), Duration.ofSeconds(30));

				return exportRecords;
			}, (date, exportRecords) -> {
				// Persist events for date
				getAnalyticsService().persistGoogleBigQueryEvents(institution.getInstitutionId(), date, exportRecords);
			});
		}

		protected <T> void performAnalyticsSync(@Nonnull Institution institution,
																						@Nonnull AnalyticsVendorId analyticsVendorId,
																						@Nonnull LocalDate minimumDate,
																						@Nonnull Function<LocalDate, List<T>> analyticsEventsFetchFunction,
																						@Nonnull BiConsumer<LocalDate, List<T>> analyticsEventsPersistFunction) {
			requireNonNull(institution);
			requireNonNull(analyticsVendorId);
			requireNonNull(minimumDate);
			requireNonNull(analyticsEventsFetchFunction);
			requireNonNull(analyticsEventsPersistFunction);

			List<LocalDate> datesToSync = new ArrayList<>();

			// Initial "fast" transaction to pull a set of all syncable dates, guarded by advisory lock.
			getDatabase().transaction(() -> {
				getSystemService().performAdvisoryLockOperationIfAvailable(AdvisoryLock.ANALYTICS_SYNC, () -> {
					// If "busy syncing" and sync started over an hour ago, assume there was a problem, and transition to "failed"
					// so sync can be retried
					getDatabase().execute("""
							UPDATE analytics_event_date_sync
							SET analytics_sync_status_id=?, sync_ended_at=NOW()
							WHERE (NOW() - sync_started_at) > INTERVAL '60 minutes'
							AND analytics_sync_status_id=?
							""", AnalyticsSyncStatusId.SYNC_FAILED, AnalyticsSyncStatusId.BUSY_SYNCING);

					// Find dates that we know are either already synced or in-progress, so we can skip over them
					Set<LocalDate> skippableDates = getDatabase().queryForList("""
											SELECT *
											FROM analytics_event_date_sync
											WHERE institution_id=?
											AND analytics_sync_status_id IN (?,?)
											AND analytics_vendor_id=?
											""", AnalyticsEventDateSync.class, institution.getInstitutionId(), AnalyticsSyncStatusId.BUSY_SYNCING, AnalyticsSyncStatusId.SYNCED,
									analyticsVendorId).stream()
							.map(analyticsEventDateSync -> analyticsEventDateSync.getDate())
							.collect(Collectors.toSet());

					// Furthest we can sync to is whatever date it was 36 hours ago in the institution's timezone.
					// This is because we don't have accurate up-to-the-minute data from our data warehouses
					LocalDate maximumDate = LocalDate.ofInstant(Instant.now().minus(36, ChronoUnit.HOURS), institution.getTimeZone());

					if (minimumDate.isAfter(maximumDate))
						throw new IllegalStateException(format("Configured minimum date %s is after maximum date %s", minimumDate, maximumDate));

					LocalDate potentialDateToSync = minimumDate;

					while (!potentialDateToSync.isAfter(maximumDate)) {
						if (!skippableDates.contains(potentialDateToSync))
							datesToSync.add(potentialDateToSync);

						potentialDateToSync = potentialDateToSync.plusDays(1);
					}

					// Mark all dates as "busy syncing"
					if (datesToSync.size() > 0) {
						getLogger().info("Need to sync {} {} dates: {}", datesToSync.size(), analyticsVendorId.name(), datesToSync);

						List<List<Object>> parameterGroups = new ArrayList<>(datesToSync.size());
						Instant syncStartedAt = Instant.now();

						for (LocalDate dateToSync : datesToSync) {
							List<Object> parameterGroup = new ArrayList<>();
							parameterGroup.add(institution.getInstitutionId());
							parameterGroup.add(analyticsVendorId);
							parameterGroup.add(AnalyticsSyncStatusId.BUSY_SYNCING);
							parameterGroup.add(dateToSync);
							parameterGroup.add(syncStartedAt);
							parameterGroup.add(null); // sync_ended_at

							parameterGroups.add(parameterGroup);
						}

						// Batch upsert to keep track of syncing.
						// Upsert is for scenarios like retrying a date that was in AnalyticsSyncStatusId.SYNC_FAILED state
						getDatabase().executeBatch("""
								INSERT INTO analytics_event_date_sync (
								  institution_id,
								  analytics_vendor_id,
								  analytics_sync_status_id,
								  date,
								  sync_started_at,
								  sync_ended_at
								) VALUES (?,?,?,?,?,?)
								ON CONFLICT ON CONSTRAINT analytics_event_date_sync_pk DO UPDATE
								  SET analytics_sync_status_id = EXCLUDED.analytics_sync_status_id,
								  sync_started_at = EXCLUDED.sync_started_at,
								  sync_ended_at = EXCLUDED.sync_ended_at
								""", parameterGroups);
					}
				});
			});

			if (datesToSync.size() > 0) {
				for (LocalDate dateToSync : datesToSync) {
					getLogger().info("Performing {} analytics sync for {} at {}...", analyticsVendorId.name(), dateToSync, institution.getInstitutionId().name());

					List<T> analyticsEvents = new ArrayList<>();

					try {
						analyticsEvents.addAll(analyticsEventsFetchFunction.apply(dateToSync));
					} catch (Exception e) {
						getLogger().error(format("Failed to fetch %s events for %s on %s", analyticsVendorId,
								institution.getInstitutionId(), dateToSync), e);

						getErrorReporter().report(e);

						// Mark the sync as failed in a separate transaction
						getDatabase().transaction(() -> {
							getDatabase().execute("""
									UPDATE analytics_event_date_sync
									SET sync_ended_at=NOW(), analytics_sync_status_id=?
									WHERE date=?
									AND analytics_vendor_id=?
									AND institution_id=?
									""", AnalyticsSyncStatusId.SYNC_FAILED, dateToSync, analyticsVendorId, institution.getInstitutionId());
						});

						continue;
					}

					getLogger().info("Found {} {} events for {} at {}.", analyticsEvents.size(), analyticsVendorId.name(), dateToSync, institution.getInstitutionId().name());

					getDatabase().transaction(() -> {
						try {
							analyticsEventsPersistFunction.accept(dateToSync, analyticsEvents);

							getDatabase().execute("""
									UPDATE analytics_event_date_sync
									SET sync_ended_at=NOW(), analytics_sync_status_id=?
									WHERE date=?
									AND analytics_vendor_id=?
									AND institution_id=?
									""", AnalyticsSyncStatusId.SYNCED, dateToSync, analyticsVendorId, institution.getInstitutionId());
						} catch (Exception e) {
							getLogger().error(format("Failed to persist %s events for %s on %s", analyticsVendorId,
									institution.getInstitutionId(), dateToSync), e);

							getErrorReporter().report(e);

							// Mark the sync as failed in a separate transaction
							getDatabase().transaction(() -> {
								getDatabase().execute("""
										UPDATE analytics_event_date_sync
										SET sync_ended_at=NOW(), analytics_sync_status_id=?
										WHERE date=?
										AND analytics_vendor_id=?
										AND institution_id=?
										""", AnalyticsSyncStatusId.SYNC_FAILED, dateToSync, analyticsVendorId, institution.getInstitutionId());
							});
						}
					});
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
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
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
