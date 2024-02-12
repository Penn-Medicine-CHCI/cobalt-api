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
import com.cobaltplatform.api.model.db.ScreeningType.ScreeningTypeId;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.model.db.Tag;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;
import com.cobaltplatform.api.model.service.AdvisoryLock;
import com.cobaltplatform.api.model.service.ScreeningScore;
import com.cobaltplatform.api.model.service.ScreeningSessionScreeningWithType;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.db.DatabaseProvider;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlInListPlaceholders;
import static com.cobaltplatform.api.util.DatabaseUtility.sqlVaragsParameters;
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
	private final Provider<ScreeningService> screeningServiceProvider;
	@Nonnull
	private final Provider<TagService> tagServiceProvider;
	@Nonnull
	private final Provider<AnalyticsSyncTask> analyticsSyncTaskProvider;
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final DatabaseProvider databaseProvider;
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
													@Nonnull Provider<ScreeningService> screeningServiceProvider,
													@Nonnull Provider<TagService> tagServiceProvider,
													@Nonnull Provider<AnalyticsSyncTask> analyticsSyncTaskProvider,
													@Nonnull EnterprisePluginProvider enterprisePluginProvider,
													@Nonnull DatabaseProvider databaseProvider,
													@Nonnull Strings strings) {
		requireNonNull(institutionServiceProvider);
		requireNonNull(systemServiceProvider);
		requireNonNull(screeningServiceProvider);
		requireNonNull(tagServiceProvider);
		requireNonNull(analyticsSyncTaskProvider);
		requireNonNull(enterprisePluginProvider);
		requireNonNull(databaseProvider);
		requireNonNull(strings);

		this.institutionServiceProvider = institutionServiceProvider;
		this.systemServiceProvider = systemServiceProvider;
		this.screeningServiceProvider = screeningServiceProvider;
		this.tagServiceProvider = tagServiceProvider;
		this.analyticsSyncTaskProvider = analyticsSyncTaskProvider;
		this.enterprisePluginProvider = enterprisePluginProvider;
		this.databaseProvider = databaseProvider;
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

		if (endDate.isBefore(startDate))
			throw new ValidationException(getStrings().get("End date cannot be before start date."));

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		Instant startTimestamp = LocalDateTime.of(startDate, LocalTime.MIN).atZone(institution.getTimeZone()).toInstant();
		Instant endTimestamp = LocalDateTime.of(endDate, LocalTime.MAX).atZone(institution.getTimeZone()).toInstant();

		// We were previously using GA4 API for this
		boolean useGoogleAnalytics = false;

		if (useGoogleAnalytics) {
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


		// Instead of calling the GA4 API, we query against our own database's BigQuery records for this information
		// so numbers are consistent with other sections.

		Long newUserCount;
		Long returningUserCount;
		Long otherUserCount = 0L; // This value only comes from GA4 API; we don't use it when pulling from BigQuery data

		newUserCount = getDatabase().queryForObject("""
				SELECT COUNT(DISTINCT account_id) AS count
				FROM v_analytics_account_interaction
				WHERE account_id IS NOT NULL
				AND activity_timestamp BETWEEN ? AND ?
				AND institution_id=?
				AND account_id NOT IN (
				  SELECT account_id
				  FROM account
				  WHERE institution_id=?
				  AND created < ?
				)
				""", Long.class, startTimestamp, endTimestamp, institutionId, institutionId, startTimestamp).get();

		returningUserCount = getDatabase().queryForObject("""
				SELECT COUNT(DISTINCT account_id) AS count
				FROM v_analytics_account_interaction
				WHERE account_id IS NOT NULL
				AND activity_timestamp BETWEEN ? AND ?
				AND institution_id=?
				AND account_id IN (
				  SELECT account_id
				  FROM account
				  WHERE institution_id=?
				  AND created < ?
				)
				""", Long.class, startTimestamp, endTimestamp, institutionId, institutionId, startTimestamp).get();

		return new AnalyticsResultNewVersusReturning(newUserCount, returningUserCount, otherUserCount);
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

		Map<AccountSourceId, Long> activeUserCountsByAccountSourceId = new TreeMap<>();

		for (AccountSourceIdCount accountSourceIdCount : accountSourceIdCounts)
			activeUserCountsByAccountSourceId.put(accountSourceIdCount.getAccountSourceId(), accountSourceIdCount.getCount());

		return activeUserCountsByAccountSourceId;
	}

	@Nonnull
	public Map<String, Long> findActiveUserCountsByInstitutionLocation(@Nonnull InstitutionId institutionId,
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

		// Accounts that have institution locations
		List<AccountSourceInstitutionLocationCount> accountSourceInstitutionLocationCounts = getDatabase().queryForList("""
				SELECT COUNT(DISTINCT a.account_id) AS count, COALESCE(il.short_name, il.name) AS institution_location_description
				FROM account a, v_analytics_account_interaction aai, institution_location il
				WHERE a.account_id=aai.account_id
				AND aai.activity_timestamp BETWEEN ? AND ?
				AND aai.institution_id=?
				AND a.institution_location_id=il.institution_location_id
				GROUP BY institution_location_description
				""", AccountSourceInstitutionLocationCount.class, startTimestamp, endTimestamp, institutionId);

		// Received a request to remove this data from the report
		boolean includeNotAsked = false;
		AccountSourceInstitutionLocationCount accountSourceInstitutionNotAskedLocationCount = null;

		if (includeNotAsked) {
			final String NOT_ASKED_LABEL = getStrings().get("Not Asked");

			// Accounts that do NOT have an institution location and were never asked to provide one
			accountSourceInstitutionNotAskedLocationCount = getDatabase().queryForObject("""
					SELECT COUNT(DISTINCT a.account_id) AS count, ? AS institution_location_description
					FROM account a, v_analytics_account_interaction aai
					WHERE a.account_id=aai.account_id
					AND aai.activity_timestamp BETWEEN ? AND ?
					AND aai.institution_id=?
					AND a.institution_location_id IS NULL
					AND a.prompted_for_institution_location=FALSE
					GROUP BY institution_location_description
					""", AccountSourceInstitutionLocationCount.class, NOT_ASKED_LABEL, startTimestamp, endTimestamp, institutionId).orElse(null);

			if (accountSourceInstitutionNotAskedLocationCount == null) {
				accountSourceInstitutionNotAskedLocationCount = new AccountSourceInstitutionLocationCount();
				accountSourceInstitutionNotAskedLocationCount.setCount(0L);
				accountSourceInstitutionNotAskedLocationCount.setInstitutionLocationDescription(NOT_ASKED_LABEL);
			}
		}

		final String DECLINED_TO_ANSWER_LABEL = getStrings().get("Declined to Answer");

		// Accounts that do NOT have an institution location even though they were asked to pick one
		AccountSourceInstitutionLocationCount accountSourceInstitutionDeclinedToAnswerLocationCount = getDatabase().queryForObject("""
				SELECT COUNT(DISTINCT a.account_id) AS count, ? AS institution_location_description
				FROM account a, v_analytics_account_interaction aai
				WHERE a.account_id=aai.account_id
				AND aai.activity_timestamp BETWEEN ? AND ?
				AND aai.institution_id=?
				AND a.institution_location_id IS NULL
				AND a.prompted_for_institution_location=TRUE
				GROUP BY institution_location_description
				""", AccountSourceInstitutionLocationCount.class, DECLINED_TO_ANSWER_LABEL, startTimestamp, endTimestamp, institutionId).orElse(null);

		if (accountSourceInstitutionDeclinedToAnswerLocationCount == null) {
			accountSourceInstitutionDeclinedToAnswerLocationCount = new AccountSourceInstitutionLocationCount();
			accountSourceInstitutionDeclinedToAnswerLocationCount.setCount(0L);
			accountSourceInstitutionDeclinedToAnswerLocationCount.setInstitutionLocationDescription(DECLINED_TO_ANSWER_LABEL);
		}

		Map<String, Long> activeUserCountsByInstitutionLocation = new TreeMap<>();

		if (includeNotAsked && accountSourceInstitutionNotAskedLocationCount != null)
			activeUserCountsByInstitutionLocation.put(accountSourceInstitutionNotAskedLocationCount.getInstitutionLocationDescription(), accountSourceInstitutionNotAskedLocationCount.getCount());

		activeUserCountsByInstitutionLocation.put(accountSourceInstitutionDeclinedToAnswerLocationCount.getInstitutionLocationDescription(), accountSourceInstitutionDeclinedToAnswerLocationCount.getCount());

		for (AccountSourceInstitutionLocationCount accountSourceInstitutionLocationCount : accountSourceInstitutionLocationCounts)
			activeUserCountsByInstitutionLocation.put(accountSourceInstitutionLocationCount.getInstitutionLocationDescription(), accountSourceInstitutionLocationCount.getCount());

		return activeUserCountsByInstitutionLocation;
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
				ORDER BY name
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

		// Now, determine user counts (user with any kind of event during the date range)
		String userSql = """
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

		List<Object> userParameters = new ArrayList<>();
		userParameters.add(urlPathRegex);
		userParameters.add(startTimestamp);
		userParameters.add(endTimestamp);
		userParameters.add(institutionId);

		List<String> userSectionSqls = new ArrayList<>();

		// Home
		userSectionSqls.add("""							
						SELECT ? AS section, COUNT(distinct aai.account_id) AS count
						FROM aai
						WHERE (url_path = '/' OR url_path LIKE '/?%')
						GROUP BY section
				""".trim());

		userParameters.add(HOME_PAGE_SECTION);

		// Features
		for (Feature feature : features) {
			userSectionSqls.add("""							
							SELECT COALESCE(if.name_override, f.name) AS section, COUNT(distinct aai.account_id) AS count
							FROM aai, feature f, institution_feature if
							WHERE f.feature_id=?
							AND if.feature_id=f.feature_id
							AND if.institution_id=aai.institution_id
							AND aai.url_path LIKE CONCAT(f.url_name, '%')
							GROUP BY section
					""".trim());

			userParameters.add(feature.getFeatureId());
		}

		userSql = userSql + userSectionSqls.stream().collect(Collectors.joining("\nUNION\n"));

		List<SectionCount> userSectionCounts = getDatabase().queryForList(userSql, SectionCount.class, userParameters.toArray(new Object[]{}));

		// There is no concept of user count for the sign-in screen b/c there is no one signed in!
		userSectionCounts.add(new SectionCount() {{
			setSection(SIGN_IN_SECTION);
			setCount(0L);
		}});

		// Now, determine *active* user counts (user with any kind of page view during the date range who has taking *meaningful* actions in Cobalt)
		String activeUserSql = """
				WITH aai AS (
				    select
				        *, regexp_replace(url, ?, '') as url_path
				    from
				        v_analytics_account_interaction
				    where
				        activity='page_view'
				        and activity_timestamp BETWEEN ? AND ?
				        and institution_id=?
				        and account_id in (
				          select account_id
				          from v_analytics_account_meaningful_interaction
				          where institution_id=?
				          and activity_timestamp BETWEEN ? AND ?
				        )
				)
				""";

		List<Object> activeUserParameters = new ArrayList<>();
		activeUserParameters.add(urlPathRegex);
		activeUserParameters.add(startTimestamp);
		activeUserParameters.add(endTimestamp);
		activeUserParameters.add(institutionId);
		activeUserParameters.add(institutionId);
		activeUserParameters.add(startTimestamp);
		activeUserParameters.add(endTimestamp);

		List<String> activeUserSectionSqls = new ArrayList<>();

		// Home
		activeUserSectionSqls.add("""							
						SELECT ? AS section, COUNT(distinct aai.account_id) AS count
						FROM aai
						WHERE (url_path = '/' OR url_path LIKE '/?%')
						GROUP BY section
				""".trim());

		activeUserParameters.add(HOME_PAGE_SECTION);

		// Features
		for (Feature feature : features) {
			activeUserSectionSqls.add("""							
							SELECT COALESCE(if.name_override, f.name) AS section, COUNT(distinct aai.account_id) AS count
							FROM aai, feature f, institution_feature if
							WHERE f.feature_id=?
							AND if.feature_id=f.feature_id
							AND if.institution_id=aai.institution_id
							AND aai.url_path LIKE CONCAT(f.url_name, '%')
							GROUP BY section
					""".trim());

			activeUserParameters.add(feature.getFeatureId());
		}

		activeUserSql = activeUserSql + activeUserSectionSqls.stream().collect(Collectors.joining("\nUNION\n"));

		List<SectionCount> activeUserSectionCounts = getDatabase().queryForList(activeUserSql, SectionCount.class, activeUserParameters.toArray(new Object[]{}));

		// There is no concept of active user count for the sign-in screen b/c there is no one signed in!
		activeUserSectionCounts.add(new SectionCount() {{
			setSection(SIGN_IN_SECTION);
			setCount(0L);
		}});

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

	@Nonnull
	public TrafficSourceSummary findTrafficSourceSummary(@Nonnull InstitutionId institutionId,
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

		String webappBaseUrl = getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institutionId, UserExperienceTypeId.PATIENT).get();

		// e.g. "referral, organic, (direct), ..."
		// Distinct by account since an account's events can have the same medium many times
		List<TrafficSourceMediumCount> trafficSourceMediumCounts = getDatabase().queryForList("""
				WITH ts AS (
				   SELECT DISTINCT traffic_source->>'medium' AS medium, account_id
				   FROM analytics_google_bigquery_event
				   WHERE account_id IS NOT NULL
				   AND timestamp BETWEEN ? AND ?
				   AND institution_id=?
				)
				SELECT COUNT(*) AS user_count, ts.medium
				FROM ts
				GROUP BY ts.medium
				ORDER BY user_count DESC
					""", TrafficSourceMediumCount.class, startTimestamp, endTimestamp, institutionId);

		// Nicer names here
		for (TrafficSourceMediumCount trafficSourceMediumCount : trafficSourceMediumCounts) {
			if ("(none)".equalsIgnoreCase(trafficSourceMediumCount.getMedium()))
				trafficSourceMediumCount.setMedium(getStrings().get("Direct"));
			else if ("referral".equalsIgnoreCase(trafficSourceMediumCount.getMedium()))
				trafficSourceMediumCount.setMedium(getStrings().get("Referral"));
			else if ("organic".equalsIgnoreCase(trafficSourceMediumCount.getMedium()))
				trafficSourceMediumCount.setMedium(getStrings().get("Organic Search"));
		}

		// e.g. "google, canva.com, yahoo, ..."
		List<TrafficSourceReferrerCount> trafficSourceReferrerCounts = List.of();

		// If false, use alternative approach of pulling from "event->'parameters'->'page_referrer'" which gives more detail
		boolean useTrafficSourceReferrer = true;

		if (useTrafficSourceReferrer) {
			trafficSourceReferrerCounts = getDatabase().queryForList("""
					WITH ts AS (
					   SELECT DISTINCT traffic_source->>'source' AS referrer, account_id, institution_id
					   FROM analytics_google_bigquery_event
					   WHERE account_id IS NOT NULL
					   AND timestamp BETWEEN ? AND ?
					   AND institution_id=?
					)
					SELECT COUNT(*) AS user_count, ts.referrer
					FROM ts
					WHERE ts.referrer != '(direct)'
					GROUP BY ts.referrer
					ORDER BY user_count DESC
					""", TrafficSourceReferrerCount.class, startTimestamp, endTimestamp, institutionId);
		} else {
			// Distinct by account since an account's events can have the same referrer many times
			trafficSourceReferrerCounts = getDatabase().queryForList("""
					WITH rd AS (
						SELECT DISTINCT event->'parameters'->'page_referrer'->>'value' as referrer, account_id
						FROM analytics_google_bigquery_event
						WHERE event->'parameters'->'page_referrer'->>'value' NOT LIKE CONCAT(?, '%')
						AND account_id IS NOT NULL
						AND timestamp BETWEEN ? AND ?
						AND institution_id=?
					)
					SELECT COUNT(*) AS user_count, referrer
					FROM rd
					GROUP BY referrer
					ORDER BY user_count DESC
										""", TrafficSourceReferrerCount.class, webappBaseUrl, startTimestamp, endTimestamp, institutionId);
		}

		Long usersFromTrafficSourceMediumTotalCount = trafficSourceMediumCounts.stream()
				.collect(Collectors.summingLong(trafficSourceMediumCount -> trafficSourceMediumCount.getUserCount()));

		Long usersFromNonDirectTrafficSourceMediumCount = trafficSourceMediumCounts.stream()
				.filter(trafficSourceMediumCount -> !trafficSourceMediumCount.getMedium().equals("(none)"))
				.collect(Collectors.summingLong(trafficSourceMediumCount -> trafficSourceMediumCount.getUserCount()));

		Double usersFromNonDirectTrafficSourceMediumPercentage = usersFromTrafficSourceMediumTotalCount.equals(0L) ? 0D : usersFromNonDirectTrafficSourceMediumCount.doubleValue() / usersFromTrafficSourceMediumTotalCount.doubleValue();

		TrafficSourceSummary trafficSourceSummary = new TrafficSourceSummary();
		trafficSourceSummary.setTrafficSourceMediumCounts(trafficSourceMediumCounts);
		trafficSourceSummary.setTrafficSourceReferrerCounts(trafficSourceReferrerCounts);
		trafficSourceSummary.setUsersFromTrafficSourceMediumTotalCount(usersFromTrafficSourceMediumTotalCount);
		trafficSourceSummary.setUsersFromNonDirectTrafficSourceMediumCount(usersFromNonDirectTrafficSourceMediumCount);
		trafficSourceSummary.setUsersFromNonDirectTrafficSourceMediumPercentage(usersFromNonDirectTrafficSourceMediumPercentage);

		return trafficSourceSummary;
	}

	@Nonnull
	public Map<UUID, ScreeningSessionCompletion> findClinicalScreeningSessionCompletionsByScreeningFlowId(@Nonnull InstitutionId institutionId,
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

		EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(institutionId);

		Set<UUID> screeningFlowIds = enterprisePlugin.analyticsClinicalScreeningFlowIds();
		Map<UUID, ScreeningSessionCompletion> screeningSessionCompletionsByScreeningFlowId = new HashMap<>(screeningFlowIds.size());

		for (UUID screeningFlowId : screeningFlowIds) {
			Long startedCount = getDatabase().queryForObject("""
					SELECT count(ss.*)
					FROM screening_session ss, account a, screening_flow_version sfv
					WHERE sfv.screening_flow_id=?
					AND ss.screening_flow_version_id = sfv.screening_flow_version_id
					AND ss.target_account_id=a.account_id
					AND a.institution_id=?
					AND ss.created BETWEEN ? AND ?
										""", Long.class, screeningFlowId, institutionId, startTimestamp, endTimestamp).get();

			StringBuilder completedSql = new StringBuilder("""
					SELECT count(ss.*)
					FROM screening_session ss, account a, screening_flow_version sfv
					WHERE sfv.screening_flow_id=?
					AND ss.screening_flow_version_id = sfv.screening_flow_version_id
					AND ss.target_account_id=a.account_id
					AND a.institution_id=?
					AND ss.created BETWEEN ? AND ?
					AND ss.completed=TRUE
					""");

			if (enterprisePlugin.analyticsClinicalScreeningFlowNeedsCrisisSkipWorkaround(screeningFlowId)) {
				// Support special legacy data where a screening session could end immediately on crisis, but due
				// to a bug, users could still back-button and skip after completing.
				// Alternative would be to update all affected screening sessions in the DB to remove "skipped=true" flag.
				completedSql.append("AND (ss.skipped=FALSE OR (ss.skipped=TRUE AND ss.crisis_indicated=TRUE))");
			} else {
				completedSql.append("AND ss.skipped=FALSE");
			}

			Long completedCount = getDatabase().queryForObject(completedSql.toString(), Long.class, screeningFlowId, institutionId, startTimestamp, endTimestamp).get();
			Double completionPercentage = startedCount.equals(0L) ? 0D : (completedCount.doubleValue() / startedCount.doubleValue());

			ScreeningSessionCompletion screeningSessionCompletion = new ScreeningSessionCompletion();
			screeningSessionCompletion.setStartedCount(startedCount);
			screeningSessionCompletion.setCompletedCount(completedCount);
			screeningSessionCompletion.setCompletionPercentage(completionPercentage);

			screeningSessionCompletionsByScreeningFlowId.put(screeningFlowId, screeningSessionCompletion);
		}

		return screeningSessionCompletionsByScreeningFlowId;
	}

	@Nonnull
	@SuppressWarnings("unused") // for example code
	public Map<UUID, SortedMap<String, Long>> findClinicalScreeningSessionSeverityCountsByDescriptionByScreeningFlowId(@Nonnull InstitutionId institutionId,
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

		EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(institutionId);

		boolean runExampleCode = false;

		// Example of how enterprise plugin might work to break out scores and group them according to custom scoring rules
		if (runExampleCode) {
			UUID screeningFlowId = UUID.randomUUID(); // just a placeholder for a clinical screening flow ID
			List<ScreeningSessionScreeningWithType> screeningSessionScreenings = getScreeningService().findScreeningSessionScreeningsWithTypeByScreeningFlowId(screeningFlowId, institutionId, startTimestamp, endTimestamp);

			// Group records by screening session ID
			Set<UUID> screeningSessionIds = screeningSessionScreenings.stream()
					.map(screeningSessionScreening -> screeningSessionScreening.getScreeningSessionId())
					.collect(Collectors.toSet());

			// 1. Prime the map - key is screening session ID, value is scores grouped by screening type ID
			// (it's assumed the screening session never has the same screening type twice)
			Map<UUID, Map<ScreeningTypeId, ScreeningScore>> screeningSessionScreeningScoresByScreeningTypeIdByScreeningSessionId = screeningSessionIds.stream()
					.collect(Collectors.toMap(Function.identity(), ignored -> new HashMap<>()));

			// 2. Load it up with score data by screening type for easy access
			for (ScreeningSessionScreeningWithType screeningSessionScreening : screeningSessionScreenings) {
				Map<ScreeningTypeId, ScreeningScore> screeningScoresByType = screeningSessionScreeningScoresByScreeningTypeIdByScreeningSessionId.get(screeningSessionScreening.getScreeningSessionId());
				screeningScoresByType.put(screeningSessionScreening.getScreeningTypeId(), screeningSessionScreening.getScoreAsObject().get());
			}

			// 3. Do our calculations and add to running totals
			Long mildCount = 0L;
			Long mediumCount = 0L;
			Long severeCount = 0L;

			for (Map<ScreeningTypeId, ScreeningScore> screeningScoresByType : screeningSessionScreeningScoresByScreeningTypeIdByScreeningSessionId.values()) {
				ScreeningScore phq9Score = screeningScoresByType.get(ScreeningTypeId.PHQ_9);
				ScreeningScore gad7Score = screeningScoresByType.get(ScreeningTypeId.GAD_7);
				ScreeningScore who5Score = screeningScoresByType.get(ScreeningTypeId.WHO_5);

				// These are just fake scores, used for an example
				if (phq9Score != null && phq9Score.getOverallScore() < 3)
					++mildCount;
				else if (phq9Score != null && phq9Score.getOverallScore() < 6)
					++mediumCount;
				else if (phq9Score != null && phq9Score.getOverallScore() < 6)
					++severeCount;
			}
		}

		return enterprisePlugin.analyticsClinicalScreeningSessionSeverityCountsByDescriptionByScreeningFlowId(startTimestamp, endTimestamp);
	}

	@Nonnull
	public List<CrisisTriggerCount> findCrisisTriggerCounts(@Nonnull InstitutionId institutionId,
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

		List<CrisisTriggerCount> crisisTriggerCounts = new ArrayList<>();

		crisisTriggerCounts.addAll(getDatabase().queryForList("""		
				SELECT COUNT(ss.*) as count, 'Assessment' as name
				FROM screening_session ss, account a
				WHERE ss.target_account_id=a.account_id
				AND a.institution_id=?
				AND ss.created BETWEEN ? AND ?
				AND ss.crisis_indicated=TRUE
								""", CrisisTriggerCount.class, institutionId, startTimestamp, endTimestamp));

		crisisTriggerCounts.addAll(getDatabase().queryForList("""		
				SELECT COUNT(*) AS count, 'HP Chiclet' AS name
				FROM analytics_google_bigquery_event
				WHERE name='HP Nav'
				AND event->'parameters'->'link_text'->>'value'='Crisis Support'
				AND institution_id=?
				AND timestamp BETWEEN ? AND ?
				""", CrisisTriggerCount.class, institutionId, startTimestamp, endTimestamp));

		crisisTriggerCounts.addAll(getDatabase().queryForList("""		
				SELECT COUNT(*) AS count, 'In Crisis Button' AS name
				FROM analytics_google_bigquery_event
				WHERE name='In Crisis Button'
				AND institution_id=?
				AND timestamp BETWEEN ? AND ?
				""", CrisisTriggerCount.class, institutionId, startTimestamp, endTimestamp));

		return crisisTriggerCounts;
	}

	@Nonnull
	public List<AppointmentCount> findAppointmentCounts(@Nonnull InstitutionId institutionId,
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

		List<AppointmentCount> appointmentCounts = getDatabase().queryForList("""
						WITH provider_row AS (
						    WITH booked_app AS (
						        SELECT app.appointment_id, app.provider_id
						        FROM appointment app, account a, institution i
						        WHERE app.account_id=a.account_id
						        AND app.canceled=FALSE
						        AND a.institution_id=i.institution_id
						        AND app.start_time AT TIME ZONE i.time_zone between ? AND ?
						        AND a.institution_id=?
						    ), canceled_app AS (
						        SELECT app.appointment_id, app.provider_id
						        FROM appointment app, account a, institution i
						        WHERE app.account_id=a.account_id
						        AND app.canceled=TRUE
						        AND a.institution_id=i.institution_id
						        AND app.start_time AT TIME ZONE i.time_zone between ? AND ?
						        AND a.institution_id=?
						    ), available_app AS (
						        SELECT pah.provider_availability_history_id, pah.slot_date_time, p.provider_id
						        FROM provider_availability_history pah, provider p, institution i
						        WHERE pah.provider_id=p.provider_id
						        AND p.institution_id=i.institution_id
						        AND p.display_phone_number_only_for_booking=FALSE
						        AND pah.slot_date_time AT TIME ZONE i.time_zone between ? AND ?
						        AND p.institution_id=?
						    )
						    SELECT p.provider_id, p.name, p.url_name, count(aa.*) as available_appointment_count, NULL::BIGINT as booked_appointment_count, NULL::BIGINT as canceled_appointment_count
						    FROM provider p
						    LEFT OUTER JOIN available_app aa ON p.provider_id=aa.provider_id
						    WHERE p.institution_id=?
						    GROUP BY p.provider_id, p.name, p.url_name, booked_appointment_count, canceled_appointment_count
						    HAVING count(aa.*) > 0
						    UNION
						    SELECT p.provider_id, p.name, p.url_name, NULL::BIGINT as available_appointment_count, count(ba.*) as booked_appointment_count, NULL::BIGINT as canceled_appointment_count
						    FROM provider p
						    LEFT OUTER JOIN booked_app ba ON p.provider_id=ba.provider_id
						    WHERE p.institution_id=?
						    GROUP BY p.provider_id, p.name, p.url_name, available_appointment_count, canceled_appointment_count
						    HAVING count(ba.*) > 0
						    UNION
						    SELECT p.provider_id, p.name, p.url_name, NULL::BIGINT as available_appointment_count, NULL::BIGINT as booked_appointment_count, count(ca.*) as canceled_appointment_count
						    FROM provider p
						    LEFT OUTER JOIN canceled_app ca ON p.provider_id=ca.provider_id
						    WHERE p.institution_id=?
						    GROUP BY p.provider_id, p.name, p.url_name, available_appointment_count, booked_appointment_count
						    HAVING count(ca.*) > 0
						)
						SELECT
						  pr.provider_id,
						  pr.name,
						  pr.url_name,
						  (COALESCE(MAX(pr.available_appointment_count), 0) + COALESCE(MAX(pr.booked_appointment_count), 0)) AS available_appointment_count,
						  COALESCE(MAX(pr.booked_appointment_count), 0) AS booked_appointment_count,
						  COALESCE(MAX(pr.canceled_appointment_count), 0) AS canceled_appointment_count,
						  CASE
						    WHEN (COALESCE(MAX(pr.available_appointment_count), 0) + COALESCE(MAX(pr.booked_appointment_count), 0)) = 0 THEN 0
						    ELSE COALESCE(MAX(pr.booked_appointment_count), 0)::DECIMAL / (COALESCE(MAX(pr.available_appointment_count), 0) + COALESCE(MAX(pr.booked_appointment_count), 0))::DECIMAL
						  END AS booking_percentage
						FROM provider_row pr
						GROUP BY pr.provider_id, pr.name, pr.url_name
						ORDER BY pr.name
						""", AppointmentCount.class, startTimestamp, endTimestamp, institutionId,
				startTimestamp, endTimestamp, institutionId,
				startTimestamp, endTimestamp, institutionId,
				institutionId, institutionId, institutionId
		);

		// Fill in support roles for each provider
		List<ProviderWithSupportRole> providerWithSupportRoles = getDatabase().queryForList("""
				SELECT p.provider_id, p.name AS provider_name, sr.support_role_id, sr.description AS support_role_description
				FROM provider p, provider_support_role psr, support_role sr
				WHERE p.institution_id=?
				AND p.provider_id=psr.provider_id
				AND psr.support_role_id=sr.support_role_id
				ORDER BY p.name, sr.description
								""", ProviderWithSupportRole.class, institutionId);

		Map<UUID, List<String>> supportRoleDescriptionsByProviderId = new HashMap<>(providerWithSupportRoles.size());

		for (ProviderWithSupportRole providerWithSupportRole : providerWithSupportRoles) {
			List<String> supportRoleDescriptions = supportRoleDescriptionsByProviderId.get(providerWithSupportRole.getProviderId());

			if (supportRoleDescriptions == null) {
				supportRoleDescriptions = new ArrayList<>();
				supportRoleDescriptionsByProviderId.put(providerWithSupportRole.getProviderId(), supportRoleDescriptions);
			}

			supportRoleDescriptions.add(providerWithSupportRole.getSupportRoleDescription());
		}

		// Now that we have the support role data easily accessible, merge it into the results
		for (AppointmentCount appointmentCount : appointmentCounts) {
			List<String> supportRoleDescriptions = supportRoleDescriptionsByProviderId.get(appointmentCount.getProviderId());
			appointmentCount.setSupportRolesDescription(supportRoleDescriptions == null || supportRoleDescriptions.size() == 0
					? getStrings().get("Unspecified")
					: supportRoleDescriptions.stream().collect(Collectors.joining(", ")));
		}

		return appointmentCounts;
	}

	@Nonnull
	public List<AppointmentClickToCallCount> findAppointmentClickToCallCounts(@Nonnull InstitutionId institutionId,
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

		List<AppointmentClickToCallCount> appointmentClickToCallCounts = new ArrayList<>();

		appointmentClickToCallCounts.addAll(getDatabase().queryForList("""		
				SELECT COUNT(*) AS count, 'Therapy Phone Call' AS name
				FROM analytics_google_bigquery_event
				WHERE name='Therapy Phone Call'
				AND institution_id=?
				AND timestamp BETWEEN ? AND ?
				""", AppointmentClickToCallCount.class, institutionId, startTimestamp, endTimestamp));

		appointmentClickToCallCounts.addAll(getDatabase().queryForList("""		
				SELECT COUNT(*) AS count, 'Medication Prescriber Phone Call' AS name
				FROM analytics_google_bigquery_event
				WHERE name='Medication Prescriber Phone Call'
				AND institution_id=?
				AND timestamp BETWEEN ? AND ?
				""", AppointmentClickToCallCount.class, institutionId, startTimestamp, endTimestamp));

		return appointmentClickToCallCounts;
	}

	@Nonnull
	public GroupSessionSummary findGroupSessionSummary(@Nonnull InstitutionId institutionId,
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

		Long registrationCount = getDatabase().queryForObject("""
				SELECT COUNT(gsr.*) as registration_count
				FROM v_group_session_reservation gsr, account a, institution i
				WHERE gsr.account_id=a.account_id
				AND a.institution_id=i.institution_id
				AND gsr.created BETWEEN ? AND ?
				AND a.institution_id=?
				""", Long.class, startTimestamp, endTimestamp, institutionId).get();

		Long requestCount = getDatabase().queryForObject("""
				SELECT COUNT(gr.*) as request_count
				FROM group_request gr, account a, institution i
				WHERE gr.requestor_account_id=a.account_id
				AND a.institution_id=i.institution_id
				AND gr.created BETWEEN ? AND ?
				AND a.institution_id=?
				""", Long.class, startTimestamp, endTimestamp, institutionId).get();

		// GA only reliably provides absolute URLs in its data, so we need to discard the prefix to find and work with the url_path.
		// For example, "https://www.cobaltplatform.com/group-sessions?abc=123" has url_path "/group-sessions?abc=123".
		// We do this by getting the webapp base URL and using it as part of a regex.
		String webappBaseUrl = getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institutionId, UserExperienceTypeId.PATIENT).get();
		String urlPathRegex = format("^%s", webappBaseUrl);

		List<GroupSessionCount> groupSessionCounts = getDatabase().queryForList("""
						WITH gs_row AS (
						    WITH gs_page_view AS (
						        SELECT COUNT(*) AS page_view_count, regexp_replace(url, ?, '') AS url_path
						        FROM v_analytics_account_interaction
						        WHERE activity = 'page_view'
						        AND activity_timestamp BETWEEN ? AND ?
						        AND institution_id=?
						        AND (
						            regexp_replace(url, ?, '') LIKE '/in-the-studio/group-session-scheduled/%'
						            OR
						            regexp_replace(url, ?, '') LIKE '/group-sessions/%'
						        )
						        GROUP BY url_path
						    ), gs_registration AS (
						        SELECT COUNT(gsr.*) as registration_count, gsr.group_session_id
						        FROM v_group_session_reservation gsr, account a, institution i
						        WHERE gsr.account_id=a.account_id
						        AND a.institution_id=i.institution_id
						        AND gsr.created BETWEEN ? AND ?
						        AND a.institution_id=?
						        GROUP BY gsr.group_session_id
						    )
						    SELECT gs.group_session_id, gs.title, SUM(gspv.page_view_count) AS page_view_count, NULL::BIGINT AS registration_count
						    FROM group_session gs, gs_page_view gspv
						    WHERE
						    (
						    gspv.url_path = '/group-sessions/' || gs.url_name
						    OR
						    gspv.url_path = '/group-sessions/' || gs.group_session_id
						    OR
						    gspv.url_path = '/in-the-studio/group-session-scheduled/' || gs.group_session_id
						    )
						    GROUP BY gs.group_session_id, gs.title, registration_count
						    UNION
						    SELECT gs.group_session_id, gs.title, NULL::BIGINT AS page_view_count, gsr.registration_count
						    FROM group_session gs, gs_registration gsr
						    WHERE gs.group_session_id=gsr.group_session_id
						)
						SELECT
						  gs.group_session_id,
						  gs.title,
						  gs.facilitator_name,
						  gs.start_date_time,
						  COALESCE(MAX(gsr.page_view_count), 0) AS page_view_count,
						  COALESCE(MAX(gsr.registration_count), 0) AS registration_count
						FROM gs_row gsr, group_session gs
						WHERE gsr.group_session_id=gs.group_session_id
						GROUP BY gs.group_session_id, gs.title, gs.facilitator_name, gs.start_date_time
						ORDER BY registration_count DESC, page_view_count DESC, gs.title, gs.start_date_time
						""", GroupSessionCount.class, urlPathRegex, startTimestamp, endTimestamp, institutionId, urlPathRegex, urlPathRegex,
				startTimestamp, endTimestamp, institutionId);

		GroupSessionSummary groupSessionSummary = new GroupSessionSummary();
		groupSessionSummary.setRegistrationCount(registrationCount);
		groupSessionSummary.setRequestCount(requestCount);
		groupSessionSummary.setGroupSessionCounts(groupSessionCounts);

		return groupSessionSummary;
	}

	@Nonnull
	public ResourceAndTopicSummary findResourceAndTopicSummary(@Nonnull InstitutionId institutionId,
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

		// GA only reliably provides absolute URLs in its data, so we need to discard the prefix to find and work with the url_path.
		// For example, "https://www.cobaltplatform.com/group-sessions?abc=123" has url_path "/group-sessions?abc=123".
		// We do this by getting the webapp base URL and using it as part of a regex.
		String webappBaseUrl = getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institutionId, UserExperienceTypeId.PATIENT).get();
		String urlPathRegex = format("^%s", webappBaseUrl);

		// Tag group page views
		// Chops off query parameters, so /resource-library/tag-groups/symptoms?test=123 is counted as /resource-library/tag-groups/symptoms.
		// Also determines tags based on URL name suffix.
		List<TagGroupPageView> directTagGroupPageViews = getDatabase().queryForList("""
				WITH tag_group_page_normalized_view AS (
				  WITH tag_group_page_view AS (
				      SELECT regexp_replace(url, ?, '') AS raw_url_path
				      FROM v_analytics_account_interaction
				      WHERE activity = 'page_view'
				      AND activity_timestamp BETWEEN ? AND ?
				      AND institution_id=?
				      AND regexp_replace(url, ?, '') LIKE '/resource-library/tag-groups/%'
				  )
				  SELECT COUNT(*) AS page_view_count,
				    CASE
				      WHEN STRPOS(tgpv.raw_url_path, ?) > 0 THEN SUBSTR(tgpv.raw_url_path, 0, STRPOS(tgpv.raw_url_path, ?))
				      ELSE tgpv.raw_url_path
				    END AS url_path
				  FROM tag_group_page_view tgpv
				  GROUP BY url_path
				)
				SELECT tgpnv.page_view_count, tgpnv.url_path, tg.tag_group_id, tg.name AS tag_group_name
				FROM tag_group_page_normalized_view tgpnv, tag_group tg
				WHERE tg.url_name = REVERSE(SUBSTR(REVERSE(tgpnv.url_path), 0, STRPOS(REVERSE(tgpnv.url_path), '/')))
				ORDER BY tgpnv.page_view_count DESC
												""", TagGroupPageView.class, urlPathRegex, startTimestamp, endTimestamp, institutionId, urlPathRegex, "?", "?");

		// Tag page views
		// Chops off query parameters, so /resource-library/tags/anxiety?test=123 is counted as /resource-library/tags/anxiety.
		// Also determines tags based on URL name suffix.
		List<TagPageView> directTagPageViews = getDatabase().queryForList("""
				WITH tag_page_normalized_view AS (
				  WITH tag_page_view AS (
				      SELECT regexp_replace(url, ?, '') AS raw_url_path
				      FROM v_analytics_account_interaction
				      WHERE activity = 'page_view'
				      AND activity_timestamp BETWEEN ? AND ?
				      AND institution_id=?
				      AND regexp_replace(url, ?, '') LIKE '/resource-library/tags/%'
				  )
				  SELECT COUNT(*) AS page_view_count,
				    CASE
				      WHEN STRPOS(tpv.raw_url_path, ?) > 0 THEN SUBSTR(tpv.raw_url_path, 0, STRPOS(tpv.raw_url_path, ?))
				      ELSE tpv.raw_url_path
				    END AS url_path
				  FROM tag_page_view tpv
				  GROUP BY url_path
				)
				SELECT tpnv.page_view_count, tpnv.url_path, t.tag_id, t.name AS tag_name, t.tag_group_id
				FROM tag_page_normalized_view tpnv, tag t
				WHERE t.url_name = REVERSE(SUBSTR(REVERSE(tpnv.url_path), 0, STRPOS(REVERSE(tpnv.url_path), '/')))
				ORDER BY tpnv.page_view_count DESC
								""", TagPageView.class, urlPathRegex, startTimestamp, endTimestamp, institutionId, urlPathRegex, "?", "?");

		// Index by tag ID for easy access
		Map<String, TagPageView> directTagPageViewsByTagId = directTagPageViews.stream()
				.collect(Collectors.toMap(TagPageView::getTagId, Function.identity()));

		// Tags based on content detail page views
		List<TagPageView> contentTagPageViews = new ArrayList<>();

		// Pull a list of all tags and fill with zeroes as a base list
		List<Tag> tags = getTagService().findTagsByInstitutionId(institutionId);

		// Prime the content data with all zero values for each tag
		for (Tag tag : tags) {
			TagPageView zeroedTagPageView = new TagPageView();
			zeroedTagPageView.setTagId(tag.getTagId());
			zeroedTagPageView.setTagName(tag.getName());
			zeroedTagPageView.setUrlPath(tag.getUrlName());
			zeroedTagPageView.setTagGroupId(tag.getTagGroupId());
			zeroedTagPageView.setPageViewCount(0L);

			contentTagPageViews.add(zeroedTagPageView);
		}

		// Index by tag ID for easy access
		Map<String, TagPageView> contentTagPageViewsByTagId = contentTagPageViews.stream()
				.collect(Collectors.toMap(TagPageView::getTagId, Function.identity()));

		// Sum up the tags for every content detail page view.
		// Example for a time window with 2 page_view events:
		// * Page view 1 for content with MOOD and SLEEP tags
		// * Page view 2 for content with BURNOUT and SLEEP tags
		// Resultset would be 1 MOOD, 1 BURNOUT, 2 SLEEP
		List<TagPageView> activeContentTagPageViews = getDatabase().queryForList("""
				WITH page_views_by_tag AS (
				    WITH content_page_normalized_view AS (
				        WITH content_page_view AS (
				            SELECT regexp_replace(url, ?, '') AS raw_url_path
				            FROM v_analytics_account_interaction
				            WHERE activity = 'page_view'
				            AND activity_timestamp BETWEEN ? AND ?
				            AND institution_id=?
				            AND regexp_replace(url, ?, '') ~ '^/resource-library/[0-9a-f]{8}-[0-9a-f]{4}-[4][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}'
				        )
				        SELECT COUNT(*) AS page_view_count,
				        CASE
				            WHEN STRPOS(cpv.raw_url_path, ?) > 0 THEN SUBSTR(cpv.raw_url_path, 0, STRPOS(cpv.raw_url_path, ?))
				            ELSE cpv.raw_url_path
				        END AS url_path
				        FROM content_page_view cpv
				        GROUP BY url_path
				    )
				    SELECT cpnv.page_view_count, cpnv.url_path, c.content_id, c.title AS content_title, tc.tag_id
				    FROM content_page_normalized_view cpnv, content c, tag_content tc, institution_content ic
				    WHERE c.content_id = (REVERSE(SUBSTR(REVERSE(cpnv.url_path), 0, STRPOS(REVERSE(cpnv.url_path), '/'))))::UUID
				    AND c.content_id = tc.content_id
				    AND tc.content_id = ic.content_id
				    AND ic.institution_id = ?
				)
				SELECT SUM(pvbt.page_view_count) AS page_view_count, pvbt.tag_id, t.name AS tag_name, t.tag_group_id, t.url_name AS url_path
				FROM page_views_by_tag pvbt, tag t
				WHERE pvbt.tag_id=t.tag_id
				GROUP BY pvbt.tag_id, t.name, t.tag_group_id, t.url_name
								""", TagPageView.class, urlPathRegex, startTimestamp, endTimestamp, institutionId, urlPathRegex, "?", "?", institutionId);

		// Overlay the query results onto the zeroed-out initial list
		for (TagPageView activeContentTagPageView : activeContentTagPageViews) {
			TagPageView contentTagPageView = contentTagPageViewsByTagId.get(activeContentTagPageView.getTagId());
			contentTagPageView.setPageViewCount(activeContentTagPageView.getPageViewCount());
		}

		// Sort the list by page view count descending, then tag name
		Collections.sort(contentTagPageViews, Comparator
				.comparing(TagPageView::getPageViewCount, Comparator.reverseOrder())
				.thenComparing(TagPageView::getTagName));

		// Content that matches URLs like "/resource-library/{uuid}", discarding query parameters
		List<ContentPageView> contentPageViews = getDatabase().queryForList("""
				WITH content_page_normalized_view AS (
				  WITH content_page_view AS (
				      SELECT regexp_replace(url, ?, '') AS raw_url_path
				      FROM v_analytics_account_interaction
				      WHERE activity = 'page_view'
				      AND activity_timestamp BETWEEN ? AND ?
				      AND institution_id=?
				      AND regexp_replace(url, ?, '') ~ '^/resource-library/[0-9a-f]{8}-[0-9a-f]{4}-[4][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}'
				  )
				  SELECT COUNT(*) AS page_view_count,
				    CASE
				      WHEN STRPOS(cpv.raw_url_path, ?) > 0 THEN SUBSTR(cpv.raw_url_path, 0, STRPOS(cpv.raw_url_path, ?))
				      ELSE cpv.raw_url_path
				    END AS url_path
				  FROM content_page_view cpv
				  GROUP BY url_path
				)
				SELECT cpnv.page_view_count, cpnv.url_path, c.content_id, c.title AS content_title
				FROM content_page_normalized_view cpnv, content c
				WHERE c.content_id = (REVERSE(SUBSTR(REVERSE(cpnv.url_path), 0, STRPOS(REVERSE(cpnv.url_path), '/'))))::UUID
				ORDER BY cpnv.page_view_count DESC, c.title
				LIMIT 25
				""", ContentPageView.class, urlPathRegex, startTimestamp, endTimestamp, institutionId, urlPathRegex, "?", "?");

		Set<UUID> contentIds = contentPageViews.stream().map(cpv -> cpv.getContentId()).collect(Collectors.toSet());

		List<Object> contentPageViewTagParameters = new ArrayList<>();
		contentPageViewTagParameters.add(institutionId);
		contentPageViewTagParameters.addAll(contentIds);

		StringBuilder contentPageViewTagsSql = new StringBuilder();
		contentPageViewTagsSql.append("""
				SELECT t.tag_id, tc.content_id, t.name AS tag_description, t.url_name AS tag_url_name
				FROM tag_content tc, tag t, institution_content ic
				WHERE ic.institution_id=?
				AND tc.tag_id=t.tag_id
				AND tc.content_id = ic.content_id
				""");

		if (contentIds.size() > 0) {
			contentPageViewTagsSql.append(format("""
					AND tc.content_id IN %s
					""", sqlInListPlaceholders(contentIds)));
		}

		contentPageViewTagsSql.append("""
				ORDER BY t.tag_id, t.description
				""");

		List<ContentPageViewTag> contentPageViewTags = getDatabase().queryForList(contentPageViewTagsSql.toString(),
				ContentPageViewTag.class, sqlVaragsParameters(contentPageViewTagParameters));

		Map<UUID, List<ContentPageViewTag>> contentPageViewTagsByContentId = new HashMap<>(contentPageViews.size());

		for (ContentPageViewTag contentPageViewTag : contentPageViewTags) {
			List<ContentPageViewTag> currentContentPageViewTags = contentPageViewTagsByContentId.get(contentPageViewTag.getContentId());

			if (currentContentPageViewTags == null) {
				currentContentPageViewTags = new ArrayList<>();
				contentPageViewTagsByContentId.put(contentPageViewTag.getContentId(), currentContentPageViewTags);
			}

			currentContentPageViewTags.add(contentPageViewTag);
		}

		for (ContentPageView contentPageView : contentPageViews) {
			List<ContentPageViewTag> currentContentPageViewTags = contentPageViewTagsByContentId.get(contentPageView.getContentId());
			contentPageView.setContentPageViewTags(currentContentPageViewTags == null ? List.of() : currentContentPageViewTags);
		}

		List<TopicCenterInteraction> topicCenterInteractions = getDatabase().queryForList("""
						WITH topic_center_row as (
						    WITH topic_center_page_view AS (
						        SELECT
						            COUNT(*) AS page_view_count,
						            regexp_replace(
						                regexp_replace(
						                    regexp_replace(url, ?, ''),
						                    '^/topic-centers/',
						                    '/community/'
						                ),
						                '^/featured-topics/',
						                '/community/'
						            ) AS url_path
						        FROM
						            v_analytics_account_interaction
						        WHERE
						            activity = 'page_view'
						            AND activity_timestamp BETWEEN ? AND ?
						            AND institution_id = ?
						            AND (
						                regexp_replace(url, ?, '') LIKE '/featured-topics/%'
						                OR regexp_replace(url, ?, '') LIKE '/community/%'
						                OR regexp_replace(url, ?, '') LIKE '/topic-centers/%'
						            )
						        GROUP BY
						            url_path
						    ),
						    topic_center_unique_visitor AS (
						        SELECT
						            COUNT(DISTINCT account_id) AS unique_visitor_count,
						            regexp_replace(
						                regexp_replace(
						                    regexp_replace(url, ?, ''),
						                    '^/topic-centers/',
						                    '/community/'
						                ),
						                '^/featured-topics/',
						                '/community/'
						            ) AS url_path
						        FROM
						            v_analytics_account_interaction
						        WHERE
						            activity = 'page_view'
						            AND activity_timestamp BETWEEN ? AND ?
						            AND institution_id = ?
						            AND (
						                regexp_replace(url, ?, '') LIKE '/featured-topics/%'
						                OR regexp_replace(url, ?, '') LIKE '/community/%'
						                OR regexp_replace(url, ?, '') LIKE '/topic-centers/%'
						            )
						        GROUP BY
						            url_path
						    ),
						    topic_center_active_user AS (
						        SELECT
						            COUNT(DISTINCT aai.account_id) AS active_user_count,
						            regexp_replace(
						                regexp_replace(
						                    regexp_replace(url, ?, ''),
						                    '^/topic-centers/',
						                    '/community/'
						                ),
						                '^/featured-topics/',
						                '/community/'
						            ) AS url_path
						        FROM
						            v_analytics_account_interaction aai, v_analytics_account_meaningful_interaction aami
						        WHERE
						        		aai.account_id = aami.account_id
						            AND aai.activity = 'page_view'
						            AND aai.activity_timestamp BETWEEN ? AND ?
						            AND aai.institution_id = ?
						            AND (
						                regexp_replace(aai.url, ?, '') LIKE '/featured-topics/%'
						                OR regexp_replace(aai.url, ?, '') LIKE '/community/%'
						                OR regexp_replace(aai.url, ?, '') LIKE '/topic-centers/%'
						            )
						            AND aami.institution_id = ?
												AND aai.activity_timestamp BETWEEN ? AND ?
						        GROUP BY
						            url_path
						    )
						    SELECT
						        tc.topic_center_id,
						        tc.name,
						        tcpv.page_view_count,
						        NULL :: BIGINT as unique_visitor_count,
						        NULL :: BIGINT AS active_user_count,
						        NULL :: BIGINT as group_session_click_count,
						        NULL :: BIGINT as group_session_by_request_click_count,
						        NULL :: BIGINT as pinboard_item_click_count,
						        NULL :: BIGINT as content_click_count
						    FROM
						        topic_center_page_view tcpv,
						        topic_center tc
						    WHERE
						        tc.url_name = REVERSE(
						            SUBSTR(
						                REVERSE(tcpv.url_path),
						                0,
						                STRPOS(REVERSE(tcpv.url_path), '/')
						            )
						        )
						    UNION
						    SELECT
						        tc.topic_center_id,
						        tc.name,
						        NULL :: BIGINT AS page_view_count,
						        tcuv.unique_visitor_count,
						        NULL :: BIGINT AS active_user_count,
						        NULL :: BIGINT as group_session_click_count,
						        NULL :: BIGINT as group_session_by_request_click_count,
						        NULL :: BIGINT as pinboard_item_click_count,
						        NULL :: BIGINT as content_click_count
						    FROM
						        topic_center_unique_visitor tcuv,
						        topic_center tc
						    WHERE
						        tc.url_name = REVERSE(
						            SUBSTR(
						                REVERSE(tcuv.url_path),
						                0,
						                STRPOS(REVERSE(tcuv.url_path), '/')
						            )
						        )
						    UNION
						    SELECT
						        tc.topic_center_id,
						        tc.name,
						        NULL :: BIGINT AS page_view_count,
						        NULL :: BIGINT AS unique_visitor_count,
						        tcau.active_user_count,
						        NULL :: BIGINT as group_session_click_count,
						        NULL :: BIGINT as group_session_by_request_click_count,
						        NULL :: BIGINT as pinboard_item_click_count,
						        NULL :: BIGINT as content_click_count
						    FROM
						        topic_center_active_user tcau,
						        topic_center tc
						    WHERE
						        tc.url_name = REVERSE(
						            SUBSTR(
						                REVERSE(tcau.url_path),
						                0,
						                STRPOS(REVERSE(tcau.url_path), '/')
						            )
						        )
						    UNION
						    SELECT
						        tc.topic_center_id,
						        tc.name,
						        NULL :: BIGINT AS page_view_count,
						        NULL :: BIGINT AS unique_visitor_count,
						        NULL :: BIGINT AS active_user_count,
						        COUNT(*) AS group_session_click_count,
						        NULL :: BIGINT AS group_session_by_request_click_count,
						        NULL :: BIGINT AS pinboard_item_click_count,
						        NULL :: BIGINT AS content_click_count
						    FROM
						        analytics_mixpanel_event ame,
						        topic_center tc
						    where
						        ame.name = 'Topic Center Group Session Click'
						        AND ame.timestamp BETWEEN ? AND ?
						        AND ame.institution_id = ?
						        AND (ame.properties ->> 'Topic Center ID') :: UUID = tc.topic_center_id
						    GROUP BY
						        tc.topic_center_id
						    UNION
						    SELECT
						        tc.topic_center_id,
						        tc.name,
						        NULL :: BIGINT AS page_view_count,
						        NULL :: BIGINT AS unique_visitor_count,
						        NULL :: BIGINT AS active_user_count,
						        NULL :: BIGINT AS group_session_click_count,
						        COUNT(*) AS group_session_by_request_click_count,
						        NULL :: BIGINT AS pinboard_item_click_count,
						        NULL :: BIGINT AS content_click_count
						    FROM
						        analytics_mixpanel_event ame,
						        topic_center tc
						    WHERE
						        ame.name = 'Topic Center Group Session By Request Click'
						        AND ame.timestamp BETWEEN ? AND ?
						        AND ame.institution_id = ?
						        AND (ame.properties ->> 'Topic Center ID') :: UUID = tc.topic_center_id
						    GROUP BY
						        tc.topic_center_id
						    UNION
						    SELECT
						        tc.topic_center_id,
						        tc.name,
						        NULL :: BIGINT AS page_view_count,
						        NULL :: BIGINT AS unique_visitor_count,
						        NULL :: BIGINT AS active_user_count,
						        NULL :: BIGINT AS group_session_click_count,
						        NULL :: BIGINT AS group_session_by_request_click_count,
						        COUNT(*) AS pinboard_item_click_count,
						        NULL :: BIGINT AS content_click_count
						    FROM
						        analytics_mixpanel_event ame,
						        topic_center tc
						    WHERE
						        ame.name = 'Topic Center Pinboard Item Click'
						        AND ame.timestamp BETWEEN ? AND ?
						        AND ame.institution_id = ?
						        AND (ame.properties ->> 'Topic Center ID') :: UUID = tc.topic_center_id
						    GROUP BY
						        tc.topic_center_id
						    UNION
						    SELECT
						        tc.topic_center_id,
						        tc.name,
						        NULL :: BIGINT AS page_view_count,
						        NULL :: BIGINT AS unique_visitor_count,
						        NULL :: BIGINT AS active_user_count,
						        NULL :: BIGINT AS group_session_click_count,
						        NULL :: BIGINT AS group_session_by_request_click_count,
						        NULL :: BIGINT AS pinboard_item_click_count,
						        COUNT(*) AS content_click_count
						    FROM
						        analytics_mixpanel_event ame,
						        topic_center tc
						    WHERE
						        ame.name = 'Topic Center Content Click'
						        AND ame.timestamp BETWEEN ? AND ?
						        AND ame.institution_id = ?
						        AND (ame.properties ->> 'Topic Center ID') :: UUID = tc.topic_center_id
						    GROUP BY
						        tc.topic_center_id
						)
						SELECT
						    tcr.topic_center_id,
						    name,
						    COALESCE(MAX(page_view_count), 0) AS page_view_count,
						    COALESCE(MAX(unique_visitor_count), 0) AS unique_visitor_count,
						    COALESCE(MAX(active_user_count), 0) AS active_user_count,
						    COALESCE(MAX(group_session_click_count), 0) AS group_session_click_count,
						    COALESCE(MAX(group_session_by_request_click_count), 0) AS group_session_by_request_click_count,
						    COALESCE(MAX(pinboard_item_click_count), 0) AS pinboard_item_click_count,
						    COALESCE(MAX(content_click_count), 0) AS content_click_count
						FROM
						    topic_center_row tcr
						GROUP BY
						    tcr.topic_center_id,
						    tcr.name
						ORDER BY
						    COALESCE(MAX(page_view_count), 0) DESC,
						    tcr.name
										""", TopicCenterInteraction.class, urlPathRegex, startTimestamp, endTimestamp, institutionId,
				urlPathRegex, urlPathRegex, urlPathRegex, urlPathRegex, startTimestamp, endTimestamp, institutionId,
				urlPathRegex, urlPathRegex, urlPathRegex, urlPathRegex, startTimestamp, endTimestamp, institutionId,
				urlPathRegex, urlPathRegex, urlPathRegex, institutionId, startTimestamp, endTimestamp,
				startTimestamp, endTimestamp, institutionId, startTimestamp,
				endTimestamp, institutionId, startTimestamp, endTimestamp, institutionId,
				startTimestamp, endTimestamp, institutionId);

		List<TagCount> contentTagCounts = getDatabase().queryForList("""
				SELECT COUNT(*), tag_id
				FROM tag_content tc, institution_content ic
				WHERE tc.content_id = ic.content_id
				AND ic.institution_id=?
				GROUP BY tag_id
				""", TagCount.class, institutionId);

		Map<String, Long> contentCountsByTagId = contentTagCounts.stream()
				.collect(Collectors.toMap(TagCount::getTagId, TagCount::getCount));

		ResourceAndTopicSummary resourceAndTopicSummary = new ResourceAndTopicSummary();
		resourceAndTopicSummary.setDirectTagGroupPageViews(directTagGroupPageViews);
		resourceAndTopicSummary.setDirectTagPageViews(directTagPageViews);
		resourceAndTopicSummary.setContentTagPageViews(contentTagPageViews);
		resourceAndTopicSummary.setContentPageViews(contentPageViews);
		resourceAndTopicSummary.setTopicCenterInteractions(topicCenterInteractions);
		resourceAndTopicSummary.setContentCountsByTagId(contentCountsByTagId);

		return resourceAndTopicSummary;
	}

	@NotThreadSafe
	public static class ResourceAndTopicSummary {
		@Nullable
		private List<TagGroupPageView> directTagGroupPageViews;
		@Nullable
		private List<TagPageView> directTagPageViews;
		@Nullable
		private List<TagPageView> contentTagPageViews;
		@Nullable
		private List<ContentPageView> contentPageViews;
		@Nullable
		private List<TopicCenterInteraction> topicCenterInteractions;
		@Nullable
		private Map<String, Long> contentCountsByTagId;

		@Nullable
		public List<TagGroupPageView> getDirectTagGroupPageViews() {
			return this.directTagGroupPageViews;
		}

		public void setDirectTagGroupPageViews(@Nullable List<TagGroupPageView> directTagGroupPageViews) {
			this.directTagGroupPageViews = directTagGroupPageViews;
		}

		@Nullable
		public List<TagPageView> getDirectTagPageViews() {
			return this.directTagPageViews;
		}

		public void setDirectTagPageViews(@Nullable List<TagPageView> directTagPageViews) {
			this.directTagPageViews = directTagPageViews;
		}

		@Nullable
		public List<TagPageView> getContentTagPageViews() {
			return this.contentTagPageViews;
		}

		public void setContentTagPageViews(@Nullable List<TagPageView> contentTagPageViews) {
			this.contentTagPageViews = contentTagPageViews;
		}

		@Nullable
		public List<ContentPageView> getContentPageViews() {
			return this.contentPageViews;
		}

		public void setContentPageViews(@Nullable List<ContentPageView> contentPageViews) {
			this.contentPageViews = contentPageViews;
		}

		@Nullable
		public List<TopicCenterInteraction> getTopicCenterInteractions() {
			return this.topicCenterInteractions;
		}

		public void setTopicCenterInteractions(@Nullable List<TopicCenterInteraction> topicCenterInteractions) {
			this.topicCenterInteractions = topicCenterInteractions;
		}

		@Nullable
		public Map<String, Long> getContentCountsByTagId() {
			return this.contentCountsByTagId;
		}

		public void setContentCountsByTagId(@Nullable Map<String, Long> contentCountsByTagId) {
			this.contentCountsByTagId = contentCountsByTagId;
		}
	}

	@NotThreadSafe
	public static class TopicCenterInteraction {
		@Nullable
		private UUID topicCenterId;
		@Nullable
		private String name;
		@Nullable
		private Long pageViewCount;
		@Nullable
		private Long uniqueVisitorCount;
		@Nullable
		private Long activeUserCount;
		@Nullable
		private Long groupSessionClickCount;
		@Nullable
		private Long groupSessionByRequestClickCount;
		@Nullable
		private Long pinboardItemClickCount;
		@Nullable
		private Long contentClickCount;

		@Nullable
		public UUID getTopicCenterId() {
			return this.topicCenterId;
		}

		public void setTopicCenterId(@Nullable UUID topicCenterId) {
			this.topicCenterId = topicCenterId;
		}

		@Nullable
		public String getName() {
			return this.name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public Long getPageViewCount() {
			return this.pageViewCount;
		}

		public void setPageViewCount(@Nullable Long pageViewCount) {
			this.pageViewCount = pageViewCount;
		}

		@Nullable
		public Long getUniqueVisitorCount() {
			return this.uniqueVisitorCount;
		}

		public void setUniqueVisitorCount(@Nullable Long uniqueVisitorCount) {
			this.uniqueVisitorCount = uniqueVisitorCount;
		}

		@Nullable
		public Long getActiveUserCount() {
			return this.activeUserCount;
		}

		public void setActiveUserCount(@Nullable Long activeUserCount) {
			this.activeUserCount = activeUserCount;
		}

		@Nullable
		public Long getGroupSessionClickCount() {
			return this.groupSessionClickCount;
		}

		public void setGroupSessionClickCount(@Nullable Long groupSessionClickCount) {
			this.groupSessionClickCount = groupSessionClickCount;
		}

		@Nullable
		public Long getGroupSessionByRequestClickCount() {
			return this.groupSessionByRequestClickCount;
		}

		public void setGroupSessionByRequestClickCount(@Nullable Long groupSessionByRequestClickCount) {
			this.groupSessionByRequestClickCount = groupSessionByRequestClickCount;
		}

		@Nullable
		public Long getPinboardItemClickCount() {
			return this.pinboardItemClickCount;
		}

		public void setPinboardItemClickCount(@Nullable Long pinboardItemClickCount) {
			this.pinboardItemClickCount = pinboardItemClickCount;
		}

		@Nullable
		public Long getContentClickCount() {
			return this.contentClickCount;
		}

		public void setContentClickCount(@Nullable Long contentClickCount) {
			this.contentClickCount = contentClickCount;
		}
	}

	@NotThreadSafe
	public static class ContentPageView {
		@Nullable
		private UUID contentId;
		@Nullable
		private String contentTitle;
		@Nullable
		private Long pageViewCount;
		@Nullable
		private List<ContentPageViewTag> contentPageViewTags;

		@Nullable
		public UUID getContentId() {
			return this.contentId;
		}

		public void setContentId(@Nullable UUID contentId) {
			this.contentId = contentId;
		}

		@Nullable
		public String getContentTitle() {
			return this.contentTitle;
		}

		public void setContentTitle(@Nullable String contentTitle) {
			this.contentTitle = contentTitle;
		}

		@Nullable
		public Long getPageViewCount() {
			return this.pageViewCount;
		}

		public void setPageViewCount(@Nullable Long pageViewCount) {
			this.pageViewCount = pageViewCount;
		}

		@Nullable
		public List<ContentPageViewTag> getContentPageViewTags() {
			return this.contentPageViewTags;
		}

		public void setContentPageViewTags(@Nullable List<ContentPageViewTag> contentPageViewTags) {
			this.contentPageViewTags = contentPageViewTags;
		}
	}

	@NotThreadSafe
	public static class ContentPageViewTag {
		@Nullable
		private String tagId;
		@Nullable
		private UUID contentId;
		@Nullable
		private String tagDescription;
		@Nullable
		private String tagUrlName;

		@Nullable
		public String getTagId() {
			return this.tagId;
		}

		public void setTagId(@Nullable String tagId) {
			this.tagId = tagId;
		}

		@Nullable
		public UUID getContentId() {
			return this.contentId;
		}

		public void setContentId(@Nullable UUID contentId) {
			this.contentId = contentId;
		}

		@Nullable
		public String getTagDescription() {
			return this.tagDescription;
		}

		public void setTagDescription(@Nullable String tagDescription) {
			this.tagDescription = tagDescription;
		}

		@Nullable
		public String getTagUrlName() {
			return this.tagUrlName;
		}

		public void setTagUrlName(@Nullable String tagUrlName) {
			this.tagUrlName = tagUrlName;
		}
	}

	@NotThreadSafe
	public static class TagGroupPageView {
		@Nullable
		private Long pageViewCount;
		@Nullable
		private String urlPath;
		@Nullable
		private String tagGroupId;
		@Nullable
		private String tagGroupName;

		@Nullable
		public Long getPageViewCount() {
			return this.pageViewCount;
		}

		public void setPageViewCount(@Nullable Long pageViewCount) {
			this.pageViewCount = pageViewCount;
		}

		@Nullable
		public String getUrlPath() {
			return this.urlPath;
		}

		public void setUrlPath(@Nullable String urlPath) {
			this.urlPath = urlPath;
		}

		@Nullable
		public String getTagGroupId() {
			return this.tagGroupId;
		}

		public void setTagGroupId(@Nullable String tagGroupId) {
			this.tagGroupId = tagGroupId;
		}

		@Nullable
		public String getTagGroupName() {
			return this.tagGroupName;
		}

		public void setTagGroupName(@Nullable String tagGroupName) {
			this.tagGroupName = tagGroupName;
		}
	}

	@NotThreadSafe
	public static class TagPageView {
		@Nullable
		private Long pageViewCount;
		@Nullable
		private String urlPath;
		@Nullable
		private String tagId;
		@Nullable
		private String tagName;
		@Nullable
		private String tagGroupId;

		@Nullable
		public Long getPageViewCount() {
			return this.pageViewCount;
		}

		public void setPageViewCount(@Nullable Long pageViewCount) {
			this.pageViewCount = pageViewCount;
		}

		@Nullable
		public String getUrlPath() {
			return this.urlPath;
		}

		public void setUrlPath(@Nullable String urlPath) {
			this.urlPath = urlPath;
		}

		@Nullable
		public String getTagId() {
			return this.tagId;
		}

		public void setTagId(@Nullable String tagId) {
			this.tagId = tagId;
		}

		@Nullable
		public String getTagName() {
			return this.tagName;
		}

		public void setTagName(@Nullable String tagName) {
			this.tagName = tagName;
		}

		@Nullable
		public String getTagGroupId() {
			return this.tagGroupId;
		}

		public void setTagGroupId(@Nullable String tagGroupId) {
			this.tagGroupId = tagGroupId;
		}
	}

	@NotThreadSafe
	public static class GroupSessionSummary {
		@Nullable
		private Long registrationCount;
		@Nullable
		private Long requestCount;
		@Nullable
		private List<GroupSessionCount> groupSessionCounts;

		@Nullable
		public Long getRegistrationCount() {
			return this.registrationCount;
		}

		public void setRegistrationCount(@Nullable Long registrationCount) {
			this.registrationCount = registrationCount;
		}

		@Nullable
		public Long getRequestCount() {
			return this.requestCount;
		}

		public void setRequestCount(@Nullable Long requestCount) {
			this.requestCount = requestCount;
		}

		@Nullable
		public List<GroupSessionCount> getGroupSessionCounts() {
			return this.groupSessionCounts;
		}

		public void setGroupSessionCounts(@Nullable List<GroupSessionCount> groupSessionCounts) {
			this.groupSessionCounts = groupSessionCounts;
		}
	}

	@NotThreadSafe
	protected static class ProviderWithSupportRole {
		@Nullable
		private UUID providerId;
		@Nullable
		private String providerName;
		@Nullable
		private SupportRoleId supportRoleId;
		@Nullable
		private String supportRoleDescription;

		@Nullable
		public UUID getProviderId() {
			return this.providerId;
		}

		public void setProviderId(@Nullable UUID providerId) {
			this.providerId = providerId;
		}

		@Nullable
		public String getProviderName() {
			return this.providerName;
		}

		public void setProviderName(@Nullable String providerName) {
			this.providerName = providerName;
		}

		@Nullable
		public SupportRoleId getSupportRoleId() {
			return this.supportRoleId;
		}

		public void setSupportRoleId(@Nullable SupportRoleId supportRoleId) {
			this.supportRoleId = supportRoleId;
		}

		@Nullable
		public String getSupportRoleDescription() {
			return this.supportRoleDescription;
		}

		public void setSupportRoleDescription(@Nullable String supportRoleDescription) {
			this.supportRoleDescription = supportRoleDescription;
		}
	}

	@NotThreadSafe
	public static class GroupSessionCount {
		@Nullable
		private UUID groupSessionId;
		@Nullable
		private String title;
		@Nullable
		private String facilitatorName;
		@Nullable
		private LocalDateTime startDateTime;
		@Nullable
		private Long registrationCount;
		@Nullable
		private Long pageViewCount;

		@Nullable
		public UUID getGroupSessionId() {
			return this.groupSessionId;
		}

		public void setGroupSessionId(@Nullable UUID groupSessionId) {
			this.groupSessionId = groupSessionId;
		}

		@Nullable
		public String getTitle() {
			return this.title;
		}

		public void setTitle(@Nullable String title) {
			this.title = title;
		}

		@Nullable
		public String getFacilitatorName() {
			return this.facilitatorName;
		}

		public void setFacilitatorName(@Nullable String facilitatorName) {
			this.facilitatorName = facilitatorName;
		}

		@Nullable
		public LocalDateTime getStartDateTime() {
			return this.startDateTime;
		}

		public void setStartDateTime(@Nullable LocalDateTime startDateTime) {
			this.startDateTime = startDateTime;
		}

		@Nullable
		public Long getRegistrationCount() {
			return this.registrationCount;
		}

		public void setRegistrationCount(@Nullable Long registrationCount) {
			this.registrationCount = registrationCount;
		}

		@Nullable
		public Long getPageViewCount() {
			return this.pageViewCount;
		}

		public void setPageViewCount(@Nullable Long pageViewCount) {
			this.pageViewCount = pageViewCount;
		}
	}

	@NotThreadSafe
	public static class AppointmentCount {
		@Nullable
		private UUID providerId;
		@Nullable
		private String name;
		@Nullable
		private String urlName;
		@Nullable
		private String supportRolesDescription;
		@Nullable
		private Long availableAppointmentCount;
		@Nullable
		private Long bookedAppointmentCount;
		@Nullable
		private Long canceledAppointmentCount;
		@Nullable
		private Double bookingPercentage;

		@Nullable
		public UUID getProviderId() {
			return this.providerId;
		}

		public void setProviderId(@Nullable UUID providerId) {
			this.providerId = providerId;
		}

		@Nullable
		public String getName() {
			return this.name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public String getUrlName() {
			return this.urlName;
		}

		public void setUrlName(@Nullable String urlName) {
			this.urlName = urlName;
		}

		@Nullable
		public String getSupportRolesDescription() {
			return this.supportRolesDescription;
		}

		public void setSupportRolesDescription(@Nullable String supportRolesDescription) {
			this.supportRolesDescription = supportRolesDescription;
		}

		@Nullable
		public Long getAvailableAppointmentCount() {
			return this.availableAppointmentCount;
		}

		public void setAvailableAppointmentCount(@Nullable Long availableAppointmentCount) {
			this.availableAppointmentCount = availableAppointmentCount;
		}

		@Nullable
		public Long getBookedAppointmentCount() {
			return this.bookedAppointmentCount;
		}

		public void setBookedAppointmentCount(@Nullable Long bookedAppointmentCount) {
			this.bookedAppointmentCount = bookedAppointmentCount;
		}

		@Nullable
		public Long getCanceledAppointmentCount() {
			return this.canceledAppointmentCount;
		}

		public void setCanceledAppointmentCount(@Nullable Long canceledAppointmentCount) {
			this.canceledAppointmentCount = canceledAppointmentCount;
		}

		@Nullable
		public Double getBookingPercentage() {
			return this.bookingPercentage;
		}

		public void setBookingPercentage(@Nullable Double bookingPercentage) {
			this.bookingPercentage = bookingPercentage;
		}
	}

	@NotThreadSafe
	public static class AppointmentClickToCallCount {
		@Nullable
		private String name;
		@Nullable
		private Long count;

		@Nullable
		public String getName() {
			return this.name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
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
	public static class CrisisTriggerCount {
		@Nullable
		private String name;
		@Nullable
		private Long count;

		@Nullable
		public String getName() {
			return this.name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
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
	public static class ScreeningSessionCompletion {
		@Nullable
		private Double completionPercentage;
		@Nullable
		private Long startedCount;
		@Nullable
		private Long completedCount;

		@Nullable
		public Double getCompletionPercentage() {
			return this.completionPercentage;
		}

		public void setCompletionPercentage(@Nullable Double completionPercentage) {
			this.completionPercentage = completionPercentage;
		}

		@Nullable
		public Long getStartedCount() {
			return this.startedCount;
		}

		public void setStartedCount(@Nullable Long startedCount) {
			this.startedCount = startedCount;
		}

		@Nullable
		public Long getCompletedCount() {
			return this.completedCount;
		}

		public void setCompletedCount(@Nullable Long completedCount) {
			this.completedCount = completedCount;
		}
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

	@NotThreadSafe
	protected static class AccountSourceInstitutionLocationCount {
		@Nullable
		private Long count;
		@Nullable
		private String institutionLocationDescription;

		@Nullable
		public Long getCount() {
			return this.count;
		}

		public void setCount(@Nullable Long count) {
			this.count = count;
		}

		@Nullable
		public String getInstitutionLocationDescription() {
			return this.institutionLocationDescription;
		}

		public void setInstitutionLocationDescription(@Nullable String institutionLocationDescription) {
			this.institutionLocationDescription = institutionLocationDescription;
		}
	}

	@NotThreadSafe
	public static class TrafficSourceMediumCount {
		@Nullable
		private String medium;
		@Nullable
		private Long userCount;

		@Nullable
		public String getMedium() {
			return this.medium;
		}

		public void setMedium(@Nullable String medium) {
			this.medium = medium;
		}

		@Nullable
		public Long getUserCount() {
			return this.userCount;
		}

		public void setUserCount(@Nullable Long userCount) {
			this.userCount = userCount;
		}
	}

	@NotThreadSafe
	public static class TrafficSourceReferrerCount {
		@Nullable
		private String referrer;
		@Nullable
		private Long userCount;

		@Nullable
		public String getReferrer() {
			return this.referrer;
		}

		public void setReferrer(@Nullable String referrer) {
			this.referrer = referrer;
		}

		@Nullable
		public Long getUserCount() {
			return this.userCount;
		}

		public void setUserCount(@Nullable Long userCount) {
			this.userCount = userCount;
		}
	}

	@NotThreadSafe
	protected static class TagCount {
		@Nullable
		private Long count;
		@Nullable
		private String tagId;

		@Nullable
		public Long getCount() {
			return this.count;
		}

		public void setCount(@Nullable Long count) {
			this.count = count;
		}

		@Nullable
		public String getTagId() {
			return this.tagId;
		}

		public void setTagId(@Nullable String tagId) {
			this.tagId = tagId;
		}
	}

	@NotThreadSafe
	public static class TrafficSourceSummary {
		@Nullable
		private Long usersFromTrafficSourceMediumTotalCount;
		@Nullable
		private Long usersFromNonDirectTrafficSourceMediumCount;
		@Nullable
		private Double usersFromNonDirectTrafficSourceMediumPercentage; // is usersFromNonDirectTrafficSourceMediumCount / usersTotalCount
		@Nullable
		private List<TrafficSourceMediumCount> trafficSourceMediumCounts;
		@Nullable
		private List<TrafficSourceReferrerCount> trafficSourceReferrerCounts;

		@Nullable
		public Long getUsersFromTrafficSourceMediumTotalCount() {
			return this.usersFromTrafficSourceMediumTotalCount;
		}

		public void setUsersFromTrafficSourceMediumTotalCount(@Nullable Long usersFromTrafficSourceMediumTotalCount) {
			this.usersFromTrafficSourceMediumTotalCount = usersFromTrafficSourceMediumTotalCount;
		}

		@Nullable
		public Long getUsersFromNonDirectTrafficSourceMediumCount() {
			return this.usersFromNonDirectTrafficSourceMediumCount;
		}

		public void setUsersFromNonDirectTrafficSourceMediumCount(@Nullable Long usersFromNonDirectTrafficSourceMediumCount) {
			this.usersFromNonDirectTrafficSourceMediumCount = usersFromNonDirectTrafficSourceMediumCount;
		}

		@Nullable
		public Double getUsersFromNonDirectTrafficSourceMediumPercentage() {
			return this.usersFromNonDirectTrafficSourceMediumPercentage;
		}

		public void setUsersFromNonDirectTrafficSourceMediumPercentage(@Nullable Double usersFromNonDirectTrafficSourceMediumPercentage) {
			this.usersFromNonDirectTrafficSourceMediumPercentage = usersFromNonDirectTrafficSourceMediumPercentage;
		}

		@Nullable
		public List<TrafficSourceMediumCount> getTrafficSourceMediumCounts() {
			return this.trafficSourceMediumCounts;
		}

		public void setTrafficSourceMediumCounts(@Nullable List<TrafficSourceMediumCount> trafficSourceMediumCounts) {
			this.trafficSourceMediumCounts = trafficSourceMediumCounts;
		}

		@Nullable
		public List<TrafficSourceReferrerCount> getTrafficSourceReferrerCounts() {
			return this.trafficSourceReferrerCounts;
		}

		public void setTrafficSourceReferrerCounts(@Nullable List<TrafficSourceReferrerCount> trafficSourceReferrerCounts) {
			this.trafficSourceReferrerCounts = trafficSourceReferrerCounts;
		}
	}

	@ThreadSafe
	public static class AnalyticsResultNewVersusReturning {
		@Nonnull
		private final Long newUserCount;
		@Nonnull
		private final Long returningUserCount;
		@Nonnull
		private final Long otherUserCount;

		public AnalyticsResultNewVersusReturning(@Nonnull Long newUserCount,
																						 @Nonnull Long returningUserCount,
																						 @Nonnull Long otherUserCount) {
			requireNonNull(newUserCount);
			requireNonNull(returningUserCount);
			requireNonNull(otherUserCount);

			this.newUserCount = newUserCount;
			this.returningUserCount = returningUserCount;
			this.otherUserCount = otherUserCount;
		}

		@Nonnull
		public Long getNewUserCount() {
			return this.newUserCount;
		}

		@Nonnull
		public Long getReturningUserCount() {
			return this.returningUserCount;
		}

		@Nonnull
		public Long getOtherUserCount() {
			return this.otherUserCount;
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
		private final DatabaseProvider databaseProvider;
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
														 @Nonnull DatabaseProvider databaseProvider,
														 @Nonnull Configuration configuration) {
			requireNonNull(analyticsServiceProvider);
			requireNonNull(institutionServiceProvider);
			requireNonNull(systemServiceProvider);
			requireNonNull(enterprisePluginProvider);
			requireNonNull(currentContextExecutor);
			requireNonNull(errorReporter);
			requireNonNull(databaseProvider);
			requireNonNull(configuration);

			this.analyticsServiceProvider = analyticsServiceProvider;
			this.institutionServiceProvider = institutionServiceProvider;
			this.systemServiceProvider = systemServiceProvider;
			this.enterprisePluginProvider = enterprisePluginProvider;
			this.currentContextExecutor = currentContextExecutor;
			this.errorReporter = errorReporter;
			this.databaseProvider = databaseProvider;
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
	protected ScreeningService getScreeningService() {
		return this.screeningServiceProvider.get();
	}

	@Nonnull
	protected TagService getTagService() {
		return this.tagServiceProvider.get();
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
		return this.databaseProvider.get();
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
