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

import com.cobaltplatform.api.model.analytics.AnalyticsCounterWidget;
import com.cobaltplatform.api.model.analytics.AnalyticsMultiChartWidget;
import com.cobaltplatform.api.model.analytics.AnalyticsTableWidget;
import com.cobaltplatform.api.model.analytics.AnalyticsWidgetTableData;
import com.cobaltplatform.api.model.analytics.AnalyticsWidgetTableRow;
import com.cobaltplatform.api.model.db.AnalyticsNativeEventType.AnalyticsNativeEventTypeId;
import com.cobaltplatform.api.model.db.AnalyticsReportGroup;
import com.cobaltplatform.api.model.db.AnalyticsReportGroupReport;
import com.cobaltplatform.api.model.db.ColorValue.ColorValueId;
import com.cobaltplatform.api.model.db.Course;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionColorValue;
import com.cobaltplatform.api.model.db.ReportType.ReportTypeId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class AnalyticsXrayService {
	// In-memory cache of chart colors
	@Nonnull
	private final ConcurrentMap<InstitutionId, List<InstitutionColorValue>> chartColorsValuesByInstitutionIdCache;

	@Nonnull
	private final Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final Provider<CourseService> courseServiceProvider;
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Logger logger;

	@Inject
	public AnalyticsXrayService(@Nonnull Provider<InstitutionService> institutionServiceProvider,
															@Nonnull Provider<CourseService> courseServiceProvider,
															@Nonnull DatabaseProvider databaseProvider,
															@Nonnull Strings strings,
															@Nonnull Formatter formatter) {
		requireNonNull(institutionServiceProvider);
		requireNonNull(courseServiceProvider);
		requireNonNull(databaseProvider);
		requireNonNull(strings);
		requireNonNull(formatter);

		this.chartColorsValuesByInstitutionIdCache = new ConcurrentHashMap<>();

		this.institutionServiceProvider = institutionServiceProvider;
		this.courseServiceProvider = courseServiceProvider;
		this.databaseProvider = databaseProvider;
		this.strings = strings;
		this.formatter = formatter;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public List<AnalyticsReportGroup> findAnalyticsReportGroupsByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		return getReadReplicaDatabase().queryForList("""
				SELECT *
				FROM analytics_report_group
				WHERE institution_id=?
				ORDER BY display_order
				""", AnalyticsReportGroup.class, institutionId);
	}

	@Nonnull
	public List<AnalyticsReportGroupReport> findAnalyticsReportGroupReportsByAnalyticsReportGroupId(@Nullable UUID analyticsReportGroupId) {
		if (analyticsReportGroupId == null)
			return List.of();

		return getReadReplicaDatabase().queryForList("""
				SELECT *
				FROM analytics_report_group_report
				WHERE analytics_report_group_id=?
				ORDER BY display_order
				""", AnalyticsReportGroupReport.class, analyticsReportGroupId);
	}

	@Nonnull
	public AnalyticsMultiChartWidget createAccountVisitsWidget(@Nonnull InstitutionId institutionId,
																														 @Nonnull LocalDate startDate,
																														 @Nonnull LocalDate endDate) {
		requireNonNull(institutionId);
		requireNonNull(startDate);
		requireNonNull(endDate);

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		ZoneId timeZone = institution.getTimeZone();

		List<AccountVisitsRow> rows = getReadReplicaDatabase().queryForList("""
				WITH params AS (
				  SELECT
				      ? AS start_date,
				      ? AS end_date,
				      ? AS tz
				),
				bounds AS (
				  SELECT
				      (start_date::timestamp AT TIME ZONE tz) AS start_utc,
				      ((end_date + 1)::timestamp AT TIME ZONE tz) AS end_utc,
				      start_date, end_date, tz
				  FROM params
				),
				daily AS (
				  SELECT
				      (timezone(b.tz, ane."timestamp"))::date AS day,
				      COUNT(DISTINCT ane.account_id) AS distinct_accounts
				  FROM account a, analytics_native_event ane
				  CROSS JOIN bounds b
				  WHERE ane."timestamp" >= b.start_utc
				  AND ane."timestamp" <  b.end_utc
				  AND ane.institution_id=?
				  AND ane.account_id=a.account_id
				  AND a.role_id=?
				  AND a.test_account=FALSE
				  GROUP BY 1
				)
				SELECT d.day, COALESCE(dd.distinct_accounts, 0) AS distinct_accounts
				FROM (
				  SELECT generate_series(b.start_date, b.end_date, interval '1 day')::date AS day
				  FROM bounds b
				) d
				LEFT JOIN daily dd USING (day)
				ORDER BY d.day
				""", AccountVisitsRow.class, startDate, endDate, timeZone, institutionId, RoleId.PATIENT);

		List<InstitutionColorValue> chartColorValues = findChartColorValuesByInstitutionId(institutionId);

		Long widgetTotal = rows.stream()
				.map(AccountVisitsRow::getDistinctAccounts)
				.mapToLong(Long::longValue)
				.sum();

		// List of date labels for x axis
		List<String> labels = rows.stream()
				.map(row -> getFormatter().formatDate(row.getDay(), FormatStyle.SHORT))
				.collect(Collectors.toUnmodifiableList());

		List<Number> data = rows.stream()
				.map(row -> row.getDistinctAccounts())
				.collect(Collectors.toUnmodifiableList());

		List<String> dataDescriptions = data.stream()
				.map(rawData -> getFormatter().formatInteger(rawData))
				.collect(Collectors.toUnmodifiableList());

		List<String> borderColors = data.stream()
				.map(rawData -> chartColorValues.get(0).getCssRepresentation())
				.collect(Collectors.toUnmodifiableList());

		List<String> backgroundColors = data.stream()
				.map(rawData -> chartColorValues.get(0).getCssRepresentation())
				.collect(Collectors.toUnmodifiableList());

		AnalyticsMultiChartWidget.Dataset dataset = new AnalyticsMultiChartWidget.Dataset();
		dataset.setData(data);
		dataset.setDataDescriptions(dataDescriptions);
		dataset.setLabel(getStrings().get("Accounts"));
		dataset.setType(AnalyticsMultiChartWidget.DatasetType.LINE);
		dataset.setBorderColor(borderColors);
		dataset.setBackgroundColor(backgroundColors);

		AnalyticsMultiChartWidget.WidgetData widgetData = new AnalyticsMultiChartWidget.WidgetData();
		widgetData.setLabels(labels);
		widgetData.setDatasets(List.of(dataset));

		AnalyticsMultiChartWidget multiChartWidget = new AnalyticsMultiChartWidget();
		multiChartWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_ACCOUNT_VISITS);
		multiChartWidget.setWidgetTitle(getStrings().get("Account Visits"));
		multiChartWidget.setWidgetSubtitle(getStrings().get("This chart shows how many accounts visited {{platformName}} per-day", Map.of(
				"platformName", institution.getPlatformName()
		)));
		multiChartWidget.setWidgetTotal(widgetTotal);
		multiChartWidget.setWidgetTotalDescription(getFormatter().formatInteger(widgetTotal));
		multiChartWidget.setWidgetData(widgetData);

		return multiChartWidget;
	}

	@NotThreadSafe
	protected static class AccountVisitsRow {
		@Nullable
		private LocalDate day;
		@Nullable
		private Long distinctAccounts;

		@Nullable
		public LocalDate getDay() {
			return this.day;
		}

		public void setDay(@Nullable LocalDate day) {
			this.day = day;
		}

		@Nullable
		public Long getDistinctAccounts() {
			return this.distinctAccounts;
		}

		public void setDistinctAccounts(@Nullable Long distinctAccounts) {
			this.distinctAccounts = distinctAccounts;
		}
	}

	@Nonnull
	public AnalyticsMultiChartWidget createAccountsCreatedWidget(@Nonnull InstitutionId institutionId,
																															 @Nonnull LocalDate startDate,
																															 @Nonnull LocalDate endDate) {
		requireNonNull(institutionId);
		requireNonNull(startDate);
		requireNonNull(endDate);

		ZoneId timeZone = getInstitutionService().findInstitutionById(institutionId).get().getTimeZone();

		List<AccountsCreatedRow> rows = getReadReplicaDatabase().queryForList("""
				WITH params AS (
				  SELECT
				      ? AS start_date,
				      ? AS end_date,
				      ? AS tz
				),
				bounds AS (
				  SELECT
				      (start_date::timestamp AT TIME ZONE tz) AS start_utc,
				      ((end_date + 1)::timestamp AT TIME ZONE tz) AS end_utc,
				      start_date, end_date, tz
				  FROM params
				),
				agg AS (
				  SELECT
				      (timezone(b.tz, a.created))::date AS day,
				      COUNT(*)::bigint AS accounts_created
				  FROM bounds b
				  JOIN account a
				    ON a.created >= b.start_utc
				   AND a.created <  b.end_utc
				  WHERE a.role_id=?
				  AND a.test_account=FALSE
				  AND a.institution_id=?
				  GROUP BY 1
				)
				SELECT d.day, COALESCE(agg.accounts_created, 0) AS accounts_created
				FROM (
				  SELECT generate_series(b.start_date, b.end_date, interval '1 day')::date AS day
				  FROM bounds b
				) d
				LEFT JOIN agg USING (day)
				ORDER BY d.day
				""", AccountsCreatedRow.class, startDate, endDate, timeZone, RoleId.PATIENT, institutionId);

		List<InstitutionColorValue> chartColorValues = findChartColorValuesByInstitutionId(institutionId);

		Long widgetTotal = rows.stream()
				.map(AccountsCreatedRow::getAccountsCreated)
				.mapToLong(Long::longValue)
				.sum();

		// List of date labels for x axis
		List<String> labels = rows.stream()
				.map(row -> getFormatter().formatDate(row.getDay(), FormatStyle.SHORT))
				.collect(Collectors.toUnmodifiableList());

		List<Number> data = rows.stream()
				.map(row -> row.getAccountsCreated())
				.collect(Collectors.toUnmodifiableList());

		List<String> dataDescriptions = data.stream()
				.map(rawData -> getFormatter().formatInteger(rawData))
				.collect(Collectors.toUnmodifiableList());

		List<String> borderColors = data.stream()
				.map(rawData -> chartColorValues.get(0).getCssRepresentation())
				.collect(Collectors.toUnmodifiableList());

		List<String> backgroundColors = data.stream()
				.map(rawData -> chartColorValues.get(0).getCssRepresentation())
				.collect(Collectors.toUnmodifiableList());

		AnalyticsMultiChartWidget.Dataset dataset = new AnalyticsMultiChartWidget.Dataset();
		dataset.setData(data);
		dataset.setDataDescriptions(dataDescriptions);
		dataset.setLabel(getStrings().get("Accounts"));
		dataset.setType(AnalyticsMultiChartWidget.DatasetType.LINE);
		dataset.setBorderColor(borderColors);
		dataset.setBackgroundColor(backgroundColors);

		AnalyticsMultiChartWidget.WidgetData widgetData = new AnalyticsMultiChartWidget.WidgetData();
		widgetData.setLabels(labels);
		widgetData.setDatasets(List.of(dataset));

		AnalyticsMultiChartWidget multiChartWidget = new AnalyticsMultiChartWidget();
		multiChartWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_ACCOUNT_CREATION);
		multiChartWidget.setWidgetTitle(getStrings().get("Accounts Created"));
		multiChartWidget.setWidgetSubtitle(getStrings().get("The total number of new accounts created"));
		multiChartWidget.setWidgetTotal(widgetTotal);
		multiChartWidget.setWidgetTotalDescription(getFormatter().formatInteger(widgetTotal));
		multiChartWidget.setWidgetData(widgetData);

		return multiChartWidget;
	}

	@NotThreadSafe
	protected static class AccountsCreatedRow {
		@Nullable
		private LocalDate day;
		@Nullable
		private Long accountsCreated;

		@Nullable
		public LocalDate getDay() {
			return this.day;
		}

		public void setDay(@Nullable LocalDate day) {
			this.day = day;
		}

		@Nullable
		public Long getAccountsCreated() {
			return this.accountsCreated;
		}

		public void setAccountsCreated(@Nullable Long accountsCreated) {
			this.accountsCreated = accountsCreated;
		}
	}

	@Nonnull
	public AnalyticsCounterWidget createAccountRepeatVisitsWidget(@Nonnull InstitutionId institutionId,
																																@Nonnull LocalDate startDate,
																																@Nonnull LocalDate endDate) {
		requireNonNull(institutionId);
		requireNonNull(startDate);
		requireNonNull(endDate);

		ZoneId timeZone = getInstitutionService().findInstitutionById(institutionId).get().getTimeZone();

		Long accountsWithMoreThanOneSession = getReadReplicaDatabase().queryForObject("""
				WITH params AS (
				  SELECT
				      ? AS start_date,
				      ? AS end_date,
				      ? AS tz
				),
				bounds AS (
				  SELECT
				      (start_date::timestamp AT TIME ZONE tz) AS start_utc,
				      ((end_date + 1)::timestamp AT TIME ZONE tz) AS end_utc
				  FROM params
				)
				SELECT COUNT(*)::bigint AS accounts_with_multi_sessions
				FROM (
				  SELECT ane.account_id
				  FROM bounds b
				  JOIN analytics_native_event ane
				    ON ane."timestamp" >= b.start_utc
				   AND ane."timestamp" <  b.end_utc
				  WHERE EXISTS (
				          SELECT 1
				          FROM account a
				          WHERE a.account_id = ane.account_id
				          AND a.role_id=?
				          AND a.test_account=FALSE
				          AND a.institution_id=?
				        )
				  GROUP BY ane.account_id
				  HAVING COUNT(DISTINCT ane.session_id) > 1
				) t
				""", Long.class, startDate, endDate, timeZone, RoleId.PATIENT, institutionId).get();

		AnalyticsCounterWidget counterWidget = new AnalyticsCounterWidget();
		counterWidget.setWidgetTotal(accountsWithMoreThanOneSession);
		counterWidget.setWidgetTotalDescription(getFormatter().formatInteger(accountsWithMoreThanOneSession));
		counterWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_ACCOUNT_REPEAT_VISITS);
		counterWidget.setWidgetTitle(getStrings().get("Repeat Visits"));
		counterWidget.setWidgetSubtitle(getStrings().get("Accounts with more than 1 browsing session"));

		return counterWidget;
	}

	@Nonnull
	public AnalyticsTableWidget createAccountReferrersWidget(@Nonnull InstitutionId institutionId,
																													 @Nonnull LocalDate startDate,
																													 @Nonnull LocalDate endDate) {
		requireNonNull(institutionId);
		requireNonNull(startDate);
		requireNonNull(endDate);

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		ZoneId timeZone = institution.getTimeZone();
		String webappBaseUrl = getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institutionId, UserExperienceTypeId.PATIENT).get();

		List<AccountReferrersRow> rows = getReadReplicaDatabase().queryForList("""
						WITH params AS (
						  SELECT
						      ? AS start_date,
						      ? AS end_date,
						      ? AS tz
						),
						bounds AS (
						  SELECT
						      (start_date::timestamp AT TIME ZONE tz) AS start_utc,
						      ((end_date + 1)::timestamp AT TIME ZONE tz) AS end_utc
						  FROM params
						)
						SELECT
						  ane.data->>'referringUrl' AS referring_url,
						  COUNT(*)::bigint          AS event_count
						FROM bounds b
						JOIN analytics_native_event ane
							ON ane."timestamp" >= b.start_utc
						 AND ane."timestamp" <  b.end_utc
						JOIN account a
							ON a.account_id = ane.account_id
						WHERE ane.institution_id=?
							AND a.role_id=?
							AND a.test_account=FALSE
							AND ane.analytics_native_event_type_id=?
							AND ane.data->>'referringUrl' NOT LIKE CONCAT(?,'%')
						GROUP BY referring_url
						ORDER BY event_count DESC, referring_url
						""", AccountReferrersRow.class, startDate, endDate, timeZone, institutionId, RoleId.PATIENT,
				AnalyticsNativeEventTypeId.SESSION_STARTED, webappBaseUrl);

		Long widgetTotal = rows.stream()
				.map(AccountReferrersRow::getEventCount)
				.mapToLong(Long::longValue)
				.sum();

		AnalyticsWidgetTableData widgetData = new AnalyticsWidgetTableData();

		widgetData.setHeaders(List.of(
				getStrings().get("Referring Website"),
				getStrings().get("Referral Count")
		));

		widgetData.setRows(rows.stream()
				.map(row -> {
					AnalyticsWidgetTableRow tableRow = new AnalyticsWidgetTableRow();

					tableRow.setData(List.of(
							safeAnchorTag(row.getReferringUrl()),
							getFormatter().formatInteger(row.getEventCount())
					));

					return tableRow;
				})
				.collect(Collectors.toList())
		);

		AnalyticsTableWidget tableWidget = new AnalyticsTableWidget();
		tableWidget.setWidgetTitle(getStrings().get("Referrers"));
		tableWidget.setWidgetTotal(widgetTotal);
		tableWidget.setWidgetTotalDescription(getFormatter().formatInteger(widgetTotal));
		tableWidget.setWidgetSubtitle(getStrings().get("Other websites that directed users to {{platformName}}", Map.of(
				"platformName", institution.getPlatformName()
		)));
		tableWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_ACCOUNT_REFERRER);
		tableWidget.setWidgetData(widgetData);

		return tableWidget;
	}

	@NotThreadSafe
	protected static class AccountReferrersRow {
		@Nullable
		private String referringUrl;
		@Nullable
		private Long eventCount;

		@Nullable
		public String getReferringUrl() {
			return this.referringUrl;
		}

		public void setReferringUrl(@Nullable String referringUrl) {
			this.referringUrl = referringUrl;
		}

		@Nullable
		public Long getEventCount() {
			return this.eventCount;
		}

		public void setEventCount(@Nullable Long eventCount) {
			this.eventCount = eventCount;
		}
	}

	@Nonnull
	public AnalyticsMultiChartWidget createAccountOnboardingResultsWidget(@Nonnull InstitutionId institutionId,
																																				@Nonnull LocalDate startDate,
																																				@Nonnull LocalDate endDate) {
		requireNonNull(institutionId);
		requireNonNull(startDate);
		requireNonNull(endDate);

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		ZoneId timeZone = institution.getTimeZone();

		List<AccountOnboardingResultsRow> rows = getReadReplicaDatabase().queryForList("""
				WITH params AS (
				  SELECT
				      ? AS start_date,
				      ? AS end_date,
				      ? AS tz,
				      ? AS institution_id
				),
				bounds AS (
				  SELECT
				      (start_date::timestamp AT TIME ZONE tz) AS start_utc,
				      ((end_date + 1)::timestamp AT TIME ZONE tz) AS end_utc,
				      start_date, end_date, tz, institution_id
				  FROM params
				),
				inst AS (
				  -- Get the onboarding flow for this institution and carry window/tz
				  SELECT i.onboarding_screening_flow_id AS flow_id,
				         b.start_utc, b.end_utc, b.start_date, b.end_date, b.tz, b.institution_id
				  FROM bounds b
				  JOIN institution i ON i.institution_id = b.institution_id
				),
				-- Most recent session PER ACCOUNT for the institution's onboarding flow,
				-- *constrained to sessions whose created falls inside the window*
				latest AS (
				  SELECT DISTINCT ON (ss.target_account_id)
				         ss.screening_session_id,
				         ss.screening_flow_version_id,
				         ss.target_account_id,
				         ss.created,
				         ss.completed,
				         ss.completed_at
				  FROM screening_session ss
				  JOIN screening_flow_version sfv
				    ON sfv.screening_flow_version_id = ss.screening_flow_version_id
				  JOIN inst x
				    ON sfv.screening_flow_id = x.flow_id
				  JOIN account a
				    ON a.account_id = ss.target_account_id
				   AND a.institution_id = x.institution_id
				   AND a.role_id = ?
				   AND a.test_account=FALSE
				  WHERE ss.created >= (SELECT start_utc FROM inst LIMIT 1)
				    AND ss.created <  (SELECT end_utc   FROM inst LIMIT 1)
				  ORDER BY ss.target_account_id, ss.created DESC
				),
				started AS (
				  -- Bucket by the local date of CREATED for the latest-in-window session
				  SELECT
				      (timezone((SELECT tz FROM inst LIMIT 1), l.created))::date AS day,
				      COUNT(*)::bigint AS started_accounts
				  FROM latest l
				  GROUP BY 1
				),
				finished AS (
				  -- Among those latest-in-window sessions, count how many are marked completed
				  -- (ignore completed_at timestamp for bucketing to avoid cross-midnight mismatch)
				  SELECT
				      (timezone((SELECT tz FROM inst LIMIT 1), l.created))::date AS day,
				      COUNT(*)::bigint AS finished_accounts
				  FROM latest l
				  WHERE l.completed IS TRUE
				  GROUP BY 1
				)
				SELECT d.day,
				       COALESCE(s.started_accounts, 0)  AS started_accounts,
				       COALESCE(f.finished_accounts, 0) AS finished_accounts
				FROM (
				  SELECT generate_series(
				           (SELECT start_date FROM inst LIMIT 1),
				           (SELECT end_date   FROM inst LIMIT 1),
				           interval '1 day'
				         )::date AS day
				) d
				LEFT JOIN started  s USING (day)
				LEFT JOIN finished f USING (day)
				ORDER BY d.day
				""", AccountOnboardingResultsRow.class, startDate, endDate, timeZone, institutionId, RoleId.PATIENT);

		List<InstitutionColorValue> chartColorValues = findChartColorValuesByInstitutionId(institutionId);

		Long startedTotal = rows.stream()
				.map(AccountOnboardingResultsRow::getStartedAccounts)
				.mapToLong(Long::longValue)
				.sum();

		Long finishedTotal = rows.stream()
				.map(AccountOnboardingResultsRow::getFinishedAccounts)
				.mapToLong(Long::longValue)
				.sum();

		// List of date labels for x axis
		List<String> labels = rows.stream()
				.map(row -> getFormatter().formatDate(row.getDay(), FormatStyle.SHORT))
				.collect(Collectors.toUnmodifiableList());

		// Started data
		List<Number> startedData = rows.stream()
				.map(row -> row.getStartedAccounts())
				.collect(Collectors.toUnmodifiableList());

		List<String> startedDataDescriptions = startedData.stream()
				.map(rawData -> getFormatter().formatInteger(rawData))
				.collect(Collectors.toUnmodifiableList());

		List<String> startedBorderColors = startedData.stream()
				.map(rawData -> chartColorValues.get(0).getCssRepresentation())
				.collect(Collectors.toUnmodifiableList());

		List<String> startedBackgroundColors = startedData.stream()
				.map(rawData -> chartColorValues.get(0).getCssRepresentation())
				.collect(Collectors.toUnmodifiableList());

		AnalyticsMultiChartWidget.Dataset startedDataset = new AnalyticsMultiChartWidget.Dataset();
		startedDataset.setData(startedData);
		startedDataset.setDataDescriptions(startedDataDescriptions);
		startedDataset.setLabel(getStrings().get("Started"));
		startedDataset.setType(AnalyticsMultiChartWidget.DatasetType.BAR);
		startedDataset.setBorderColor(startedBorderColors);
		startedDataset.setBackgroundColor(startedBackgroundColors);

		// Finished data
		List<Number> finishedData = rows.stream()
				.map(row -> row.getFinishedAccounts())
				.collect(Collectors.toUnmodifiableList());

		List<String> finishedDataDescriptions = finishedData.stream()
				.map(rawData -> getFormatter().formatInteger(rawData))
				.collect(Collectors.toUnmodifiableList());

		List<String> finishedBorderColors = finishedData.stream()
				.map(rawData -> chartColorValues.get(1).getCssRepresentation())
				.collect(Collectors.toUnmodifiableList());

		List<String> finishedBackgroundColors = finishedData.stream()
				.map(rawData -> chartColorValues.get(1).getCssRepresentation())
				.collect(Collectors.toUnmodifiableList());

		AnalyticsMultiChartWidget.Dataset finishedDataset = new AnalyticsMultiChartWidget.Dataset();
		finishedDataset.setData(finishedData);
		finishedDataset.setDataDescriptions(finishedDataDescriptions);
		finishedDataset.setLabel(getStrings().get("Finished"));
		finishedDataset.setType(AnalyticsMultiChartWidget.DatasetType.BAR);
		finishedDataset.setBorderColor(finishedBorderColors);
		finishedDataset.setBackgroundColor(finishedBackgroundColors);

		AnalyticsMultiChartWidget.WidgetData widgetData = new AnalyticsMultiChartWidget.WidgetData();
		widgetData.setLabels(labels);
		widgetData.setDatasets(List.of(startedDataset, finishedDataset));

		AnalyticsMultiChartWidget multiChartWidget = new AnalyticsMultiChartWidget();
		multiChartWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_ACCOUNT_ONBOARDING_RESULTS);
		multiChartWidget.setWidgetTitle(getStrings().get("Account Onboarding: Started vs. Finished"));
		multiChartWidget.setWidgetSubtitle(getStrings().get("The total number of accounts who have started the onboarding assessment (vs. {{finishedDescription}} who finished)", Map.of(
				"finishedDescription", getFormatter().formatInteger(finishedTotal)
		)));
		multiChartWidget.setWidgetTotal(startedTotal);
		multiChartWidget.setWidgetTotalDescription(getFormatter().formatInteger(startedTotal));
		multiChartWidget.setWidgetData(widgetData);

		return multiChartWidget;
	}

	@NotThreadSafe
	protected static class AccountOnboardingResultsRow {
		@Nullable
		private LocalDate day;
		@Nullable
		private Long startedAccounts;
		@Nullable
		private Long finishedAccounts;

		@Nullable
		public LocalDate getDay() {
			return this.day;
		}

		public void setDay(@Nullable LocalDate day) {
			this.day = day;
		}

		@Nullable
		public Long getStartedAccounts() {
			return this.startedAccounts;
		}

		public void setStartedAccounts(@Nullable Long startedAccounts) {
			this.startedAccounts = startedAccounts;
		}

		@Nullable
		public Long getFinishedAccounts() {
			return this.finishedAccounts;
		}

		public void setFinishedAccounts(@Nullable Long finishedAccounts) {
			this.finishedAccounts = finishedAccounts;
		}
	}

	@Nonnull
	public AnalyticsMultiChartWidget createCourseAccountVisitsWidget(@Nonnull InstitutionId institutionId,
																																	 @Nonnull LocalDate startDate,
																																	 @Nonnull LocalDate endDate) {
		requireNonNull(institutionId);
		requireNonNull(startDate);
		requireNonNull(endDate);

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		ZoneId timeZone = institution.getTimeZone();

		// Data looks like this:
		//    day     |              course_id               |          course_title          | distinct_accounts
		// -----------+--------------------------------------+--------------------------------+-------------------
		// 2025-10-24 | 0bc19523-1795-4dbd-80f5-52f92b455e2f | Building Healthy Sleep         |                 1
		// 2025-10-24 | b38f9806-3ba2-4ec7-b836-538c395621df | Managing Challenging Behaviors |                 0
		// 2025-10-24 | 11ad19bb-3759-4610-b8f7-0fe56f647d45 | Parenting Teens                |                 1
		// 2025-10-24 | d5cf3b37-c3a3-4d30-9d47-7f6e33ce6b68 | Understanding Trauma           |                 0
		List<CourseAccountVisitsRow> rows = getReadReplicaDatabase().queryForList("""
						WITH params AS (
						  SELECT
						      ? AS start_date,
						      ? AS end_date,
						      ? AS tz
						),
						bounds AS (
						  SELECT
						    (start_date::timestamp AT TIME ZONE tz) AS start_utc,
						    ((end_date + 1)::timestamp AT TIME ZONE tz) AS end_utc,
						    start_date, end_date, tz
						  FROM params
						),
						-- Courses that actually appear in the window (limits the zero-fill set)
						courses_in_scope AS (
						  SELECT DISTINCT
						    c.course_id,
						    c.title
						  FROM bounds b
						  JOIN analytics_native_event ane
						    ON ane."timestamp" >= b.start_utc
						   AND ane."timestamp" <  b.end_utc
						   AND ane.institution_id = ?
						   AND ane.analytics_native_event_type_id = ?
						  JOIN account a
						    ON a.account_id = ane.account_id
						   AND a.role_id = ?
						   AND a.test_account = FALSE
						  JOIN course_unit cu
						    ON cu.course_unit_id = (ane.data->>'courseUnitId')::uuid
						  JOIN course_module cm
						    ON cm.course_module_id = cu.course_module_id
						  JOIN course c
						    ON c.course_id = cm.course_id
						),
						-- Daily distinct accounts per course
						daily AS (
						  SELECT
						    (timezone(b.tz, ane."timestamp"))::date AS day,
						    c.course_id,
						    c.title,
						    COUNT(DISTINCT ane.account_id)          AS distinct_accounts
						  FROM bounds b
						  JOIN analytics_native_event ane
						    ON ane."timestamp" >= b.start_utc
						   AND ane."timestamp" <  b.end_utc
						   AND ane.institution_id = ?
						   AND ane.analytics_native_event_type_id = ?
						  JOIN account a
						    ON a.account_id = ane.account_id
						   AND a.role_id = ?
						   AND a.test_account = FALSE
						  JOIN course_unit cu
						    ON cu.course_unit_id = (ane.data->>'courseUnitId')::uuid
						  JOIN course_module cm
						    ON cm.course_module_id = cu.course_module_id
						  JOIN course c
						    ON c.course_id = cm.course_id
						  GROUP BY 1, 2, 3
						),
						days AS (
						  SELECT generate_series(b.start_date, b.end_date, interval '1 day')::date AS day
						  FROM bounds b
						)
						SELECT
						  d.day,
						  cis.course_id,
						  cis.title AS course_title,
						  COALESCE(di.distinct_accounts, 0) AS distinct_accounts
						FROM days d
						CROSS JOIN courses_in_scope cis
						LEFT JOIN daily di
						  ON di.day = d.day
						 AND di.course_id = cis.course_id
						ORDER BY d.day, cis.title, cis.course_id
						""", CourseAccountVisitsRow.class, startDate, endDate, timeZone,
				institutionId, AnalyticsNativeEventTypeId.PAGE_VIEW_COURSE_UNIT, RoleId.PATIENT,
				institutionId, AnalyticsNativeEventTypeId.PAGE_VIEW_COURSE_UNIT, RoleId.PATIENT);

		List<InstitutionColorValue> chartColorValues = findChartColorValuesByInstitutionId(institutionId);
		List<Course> courses = getCourseService().findCoursesByInstitutionId(institutionId);
		Map<UUID, InstitutionColorValue> chartColorValuesByCourseId = new HashMap<>(courses.size());

		// "Stable" coloring based on institution's course sort order.
		// This way colors don't change on page reload
		for (int i = 0; i < courses.size(); ++i) {
			Course course = courses.get(i);
			chartColorValuesByCourseId.put(course.getCourseId(), chartColorValues.get(i % chartColorValues.size()));
		}

		Long totalVisitingAccounts = rows.stream()
				.map(CourseAccountVisitsRow::getDistinctAccounts)
				.mapToLong(Long::longValue)
				.sum();

		// List of date labels for x axis
		List<String> labels = rows.stream()
				.map(row -> row.getDay())
				.distinct()
				.map(day -> getFormatter().formatDate(day, FormatStyle.SHORT))
				.collect(Collectors.toUnmodifiableList());

		List<AnalyticsMultiChartWidget.Dataset> datasets = new ArrayList<>(courses.size());

		for (Course course : courses) {
			List<Number> data = rows.stream()
					.filter(row -> row.getCourseId().equals(course.getCourseId()))
					.map(row -> row.getDistinctAccounts())
					.collect(Collectors.toUnmodifiableList());

			List<String> dataDescriptions = data.stream()
					.map(rawData -> getFormatter().formatInteger(rawData))
					.collect(Collectors.toUnmodifiableList());

			InstitutionColorValue institutionColorValue = chartColorValuesByCourseId.get(course.getCourseId());

			List<String> borderColors = data.stream()
					.map(rawData -> institutionColorValue.getCssRepresentation())
					.collect(Collectors.toUnmodifiableList());

			List<String> backgroundColors = data.stream()
					.map(rawData -> institutionColorValue.getCssRepresentation())
					.collect(Collectors.toUnmodifiableList());

			AnalyticsMultiChartWidget.Dataset dataset = new AnalyticsMultiChartWidget.Dataset();
			dataset.setData(data);
			dataset.setDataDescriptions(dataDescriptions);
			dataset.setLabel(getStrings().get(course.getTitle()));
			dataset.setType(AnalyticsMultiChartWidget.DatasetType.BAR);
			dataset.setBorderColor(borderColors);
			dataset.setBackgroundColor(backgroundColors);

			datasets.add(dataset);
		}

		AnalyticsMultiChartWidget.WidgetData widgetData = new AnalyticsMultiChartWidget.WidgetData();
		widgetData.setLabels(labels);
		widgetData.setDatasets(datasets);

		AnalyticsMultiChartWidget multiChartWidget = new AnalyticsMultiChartWidget();
		multiChartWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_COURSE_ACCOUNT_VISITS);
		multiChartWidget.setWidgetTitle(getStrings().get("Course Visits"));
		multiChartWidget.setWidgetSubtitle(getStrings().get("The total number of accounts who have viewed a course unit at least once"));
		multiChartWidget.setWidgetTotal(totalVisitingAccounts);
		multiChartWidget.setWidgetTotalDescription(getFormatter().formatInteger(totalVisitingAccounts));
		multiChartWidget.setWidgetData(widgetData);

		return multiChartWidget;
	}

	@NotThreadSafe
	protected static class CourseAccountVisitsRow {
		@Nullable
		private LocalDate day;
		@Nullable
		private UUID courseId;
		@Nullable
		private String courseTitle;
		@Nullable
		private Long distinctAccounts;

		@Nullable
		public LocalDate getDay() {
			return this.day;
		}

		public void setDay(@Nullable LocalDate day) {
			this.day = day;
		}

		@Nullable
		public UUID getCourseId() {
			return this.courseId;
		}

		public void setCourseId(@Nullable UUID courseId) {
			this.courseId = courseId;
		}

		@Nullable
		public String getCourseTitle() {
			return this.courseTitle;
		}

		public void setCourseTitle(@Nullable String courseTitle) {
			this.courseTitle = courseTitle;
		}

		@Nullable
		public Long getDistinctAccounts() {
			return this.distinctAccounts;
		}

		public void setDistinctAccounts(@Nullable Long distinctAccounts) {
			this.distinctAccounts = distinctAccounts;
		}
	}

	@Nonnull
	public AnalyticsTableWidget createCourseDwellTimeWidget(@Nonnull InstitutionId institutionId,
																													@Nonnull LocalDate startDate,
																													@Nonnull LocalDate endDate) {
		requireNonNull(institutionId);
		requireNonNull(startDate);
		requireNonNull(endDate);

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		ZoneId timeZone = institution.getTimeZone();

		List<Course> courses = getCourseService().findCoursesByInstitutionId(institutionId);
		List<AnalyticsWidgetTableRow> tableRows = new ArrayList<>(courses.size());

		List<CourseDwellTimeRow> courseDwellTimeRows = getReadReplicaDatabase().queryForList("""
						WITH filtered_events AS (
						    SELECT
						        mv.institution_id,
						        mv.account_id,
						        mv.course_unit_id,
						        mv.dwell_time_seconds,
						        mv.page_viewed_at
						    FROM mv_analytics_dwell_time mv
						    WHERE mv.institution_id = ?
						      AND mv.page_view_type = ?
						      -- convert timestamps to the caller's local time zone
						      AND (mv.page_viewed_at AT TIME ZONE ?) >= ?::timestamp
						      AND (mv.page_viewed_at AT TIME ZONE ?) <
						          ((?::date + 1)::timestamp)     -- end_date inclusive
						),
						course_account_totals AS (
						    SELECT
						        fe.institution_id,
						        c.course_id,
						        fe.account_id,
						        SUM(fe.dwell_time_seconds) AS total_dwell_seconds
						    FROM filtered_events fe
						    JOIN course_unit   cu ON cu.course_unit_id   = fe.course_unit_id
						    JOIN course_module cm ON cm.course_module_id = cu.course_module_id
						    JOIN course        c  ON c.course_id         = cm.course_id
						    JOIN account       a  ON a.account_id        = fe.account_id
						    WHERE a.role_id = ?
						      AND a.test_account = FALSE
						    GROUP BY
						        fe.institution_id,
						        c.course_id,
						        fe.account_id
						)
						SELECT
						    cat.institution_id,
						    cat.course_id,
						    AVG(cat.total_dwell_seconds) AS mean_dwell_seconds_per_account,
						    percentile_cont(0.5)
						         WITHIN GROUP (ORDER BY cat.total_dwell_seconds)
						         AS median_dwell_seconds_per_account,
						    COUNT(*) AS account_count
						FROM course_account_totals cat
						GROUP BY
						    cat.institution_id,
						    cat.course_id
						ORDER BY
						    cat.institution_id,
						    cat.course_id
						""", CourseDwellTimeRow.class,
				institutionId,
				AnalyticsNativeEventTypeId.PAGE_VIEW_COURSE_UNIT,
				timeZone,
				startDate,
				timeZone,
				endDate,
				RoleId.PATIENT);

		Map<UUID, CourseDwellTimeRow> courseDwellTimeRowsByCourseId =
				courseDwellTimeRows.stream().collect(Collectors.toMap(CourseDwellTimeRow::getCourseId, Function.identity()));

		// Show all courses, even if no data
		for (Course course : courses) {
			CourseDwellTimeRow courseDwellTimeRow = courseDwellTimeRowsByCourseId.get(course.getCourseId());

			AnalyticsWidgetTableRow tableRow = new AnalyticsWidgetTableRow();
			tableRow.setData(List.of(
					safeAnchorTag(format("/courses/%s", course.getUrlName()), course.getTitle()),
					getFormatter().formatInteger(courseDwellTimeRow == null ? 0 : courseDwellTimeRow.getAccountCount()),
					formatDuration(courseDwellTimeRow == null ? 0 : courseDwellTimeRow.getMeanDwellSecondsPerAccount()),
					formatDuration(courseDwellTimeRow == null ? 0 : courseDwellTimeRow.getMedianDwellSecondsPerAccount())
			));

			tableRows.add(tableRow);
		}

		AnalyticsWidgetTableData tableData = new AnalyticsWidgetTableData();
		tableData.setHeaders(List.of(
				getStrings().get("Course"),
				getStrings().get("Total Number of Accounts"),
				getStrings().get("Mean Dwell Time"),
				getStrings().get("Median Dwell Time")
		));
		tableData.setRows(tableRows);

		AnalyticsTableWidget analyticsTableWidget = new AnalyticsTableWidget();
		analyticsTableWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_COURSE_DWELL_TIME);
		analyticsTableWidget.setWidgetTitle(getStrings().get("Overall Course Dwell Time"));
		analyticsTableWidget.setWidgetSubtitle(getStrings().get("Mean and median time (per account) spent on course unit pages within a course. Dwell times are determined by a 'heartbeat' every 5 seconds. If the user leaves a page before the first heartbeat, the dwell time is counted as 2.5 seconds."));
		analyticsTableWidget.setWidgetData(tableData);

		return analyticsTableWidget;
	}

	@NotThreadSafe
	protected static class CourseDwellTimeRow {
		@Nullable
		private UUID courseId;
		@Nullable
		private Double meanDwellSecondsPerAccount;
		@Nullable
		private Double medianDwellSecondsPerAccount;
		@Nullable
		private Long accountCount;

		@Nullable
		public UUID getCourseId() {
			return this.courseId;
		}

		public void setCourseId(@Nullable UUID courseId) {
			this.courseId = courseId;
		}

		@Nullable
		public Double getMeanDwellSecondsPerAccount() {
			return this.meanDwellSecondsPerAccount;
		}

		public void setMeanDwellSecondsPerAccount(@Nullable Double meanDwellSecondsPerAccount) {
			this.meanDwellSecondsPerAccount = meanDwellSecondsPerAccount;
		}

		@Nullable
		public Double getMedianDwellSecondsPerAccount() {
			return this.medianDwellSecondsPerAccount;
		}

		public void setMedianDwellSecondsPerAccount(@Nullable Double medianDwellSecondsPerAccount) {
			this.medianDwellSecondsPerAccount = medianDwellSecondsPerAccount;
		}

		@Nullable
		public Long getAccountCount() {
			return this.accountCount;
		}

		public void setAccountCount(@Nullable Long accountCount) {
			this.accountCount = accountCount;
		}
	}

	@Nonnull
	public List<AnalyticsTableWidget> createCourseUnitDwellTimeWidgets(@Nonnull InstitutionId institutionId,
																																		 @Nonnull LocalDate startDate,
																																		 @Nonnull LocalDate endDate) {
		requireNonNull(institutionId);
		requireNonNull(startDate);
		requireNonNull(endDate);

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		List<Course> courses = getCourseService().findCoursesByInstitutionId(institutionId);
		ZoneId timeZone = institution.getTimeZone();

		List<CourseModuleDwellTimeRow> courseModuleDwellTimeRows =
				getReadReplicaDatabase().queryForList("""
								WITH filtered_events AS (
								    SELECT
								        mv.institution_id,
								        mv.account_id,
								        mv.course_unit_id,
								        mv.dwell_time_seconds,
								        mv.page_viewed_at
								    FROM mv_analytics_dwell_time mv
								    WHERE mv.institution_id = ?
								      AND mv.page_view_type = ?
								      -- interpret start/end as local dates in the provided time zone
								      AND (mv.page_viewed_at AT TIME ZONE ?) >= ?::timestamp
								      AND (mv.page_viewed_at AT TIME ZONE ?) <
								          ((?::date + 1)::timestamp) -- end_date inclusive
								),
								course_module_account_totals AS (
								    -- Per-account total dwell time per course_module
								    SELECT
								        fe.institution_id,
								        cm.course_module_id,
								        fe.account_id,
								        SUM(fe.dwell_time_seconds) AS total_dwell_seconds
								    FROM filtered_events fe
								    JOIN course_unit   cu ON cu.course_unit_id   = fe.course_unit_id
								    JOIN course_module cm ON cm.course_module_id = cu.course_module_id
								    JOIN course        c  ON c.course_id         = cm.course_id
								    JOIN institution_course ic ON ic.course_id   = c.course_id
								    JOIN account       a  ON a.account_id        = fe.account_id
								    WHERE ic.institution_id = fe.institution_id
								      AND a.role_id = ?
								      AND a.test_account = FALSE
								    GROUP BY
								        fe.institution_id,
								        cm.course_module_id,
								        fe.account_id
								),
								course_module_stats AS (
								    -- Aggregate per course_module across accounts: mean, median, count
								    SELECT
								        cmat.institution_id,
								        c.course_id,
								        c.title AS course_title,
								        cm.course_module_id,
								        cm.title AS course_module_title,
								        cm.display_order AS course_module_display_order,
								        AVG(cmat.total_dwell_seconds) AS mean_dwell_seconds_per_account,
								        percentile_cont(0.5) WITHIN GROUP (ORDER BY cmat.total_dwell_seconds)
								            AS median_dwell_seconds_per_account,
								        COUNT(*) AS account_count  -- each row is one account
								    FROM course_module_account_totals cmat
								    JOIN course_module cm ON cm.course_module_id = cmat.course_module_id
								    JOIN course        c  ON c.course_id        = cm.course_id
								    GROUP BY
								        cmat.institution_id,
								        c.course_id,
								        c.title,
								        cm.course_module_id,
								        cm.title,
								        cm.display_order
								)
								SELECT
								    ic.institution_id,
								    cms.course_id,
								    cms.course_title,
								    cms.course_module_id,
								    cms.course_module_title,
								    cms.mean_dwell_seconds_per_account,
								    cms.median_dwell_seconds_per_account,
								    cms.account_count
								FROM course_module_stats cms
								JOIN institution_course ic
								  ON ic.course_id = cms.course_id
								 AND ic.institution_id = cms.institution_id
								WHERE ic.institution_id = ?
								ORDER BY
								    ic.display_order,
								    cms.course_module_display_order
								""",
						CourseModuleDwellTimeRow.class,
						institutionId,
						AnalyticsNativeEventTypeId.PAGE_VIEW_COURSE_UNIT,
						timeZone,
						startDate,
						timeZone,
						endDate,
						RoleId.PATIENT,
						institutionId);

		List<CourseUnitDwellTimeRow> courseUnitDwellTimeRows = getReadReplicaDatabase().queryForList("""
						WITH filtered_events AS (
						    SELECT
						        mv.institution_id,
						        mv.account_id,
						        mv.course_unit_id,
						        mv.dwell_time_seconds,
						        mv.page_viewed_at
						    FROM mv_analytics_dwell_time mv
						    WHERE mv.institution_id = ?
						      AND mv.page_view_type = ?
						      -- interpret start/end as local dates in the provided time zone
						      AND (mv.page_viewed_at AT TIME ZONE ?) >= ?::timestamp
						      AND (mv.page_viewed_at AT TIME ZONE ?) <
						          ((?::date + 1)::timestamp) -- end_date inclusive
						),
						course_unit_account_totals AS (
						    -- Per-account total dwell time per course_unit
						    SELECT
						        fe.institution_id,
						        cu.course_unit_id,
						        fe.account_id,
						        SUM(fe.dwell_time_seconds) AS total_dwell_seconds
						    FROM filtered_events fe
						    JOIN course_unit   cu ON cu.course_unit_id   = fe.course_unit_id
						    JOIN course_module cm ON cm.course_module_id = cu.course_module_id
						    JOIN course        c  ON c.course_id         = cm.course_id
						    JOIN institution_course ic ON ic.course_id   = c.course_id
						    JOIN account       a  ON a.account_id        = fe.account_id
						    WHERE ic.institution_id = fe.institution_id
						      AND a.role_id = ?
						      AND a.test_account = FALSE
						    GROUP BY
						        fe.institution_id,
						        cu.course_unit_id,
						        fe.account_id
						),
						course_unit_stats AS (
						    -- Aggregate per course_unit across accounts: mean, median, count
						    SELECT
						        cuat.institution_id,
						        cuat.course_unit_id,
						        AVG(cuat.total_dwell_seconds) AS mean_dwell_seconds_per_account,
						        percentile_cont(0.5) WITHIN GROUP (ORDER BY cuat.total_dwell_seconds)
						            AS median_dwell_seconds_per_account,
						        COUNT(*) AS account_count
						    FROM course_unit_account_totals cuat
						    GROUP BY
						        cuat.institution_id,
						        cuat.course_unit_id
						)
						SELECT
						    ic.institution_id,
						    c.course_id,
						    c.title AS course_title,
						    cm.course_module_id,
						    cm.title AS course_module_title,
						    cu.course_unit_id,
						    cu.title AS course_unit_title,
						    COALESCE(cus.mean_dwell_seconds_per_account, 0)    AS mean_dwell_seconds_per_account,
						    COALESCE(cus.median_dwell_seconds_per_account, 0)  AS median_dwell_seconds_per_account,
						    COALESCE(cus.account_count, 0)                     AS account_count
						FROM institution_course ic
						JOIN course c
						    ON c.course_id = ic.course_id
						LEFT JOIN course_module cm
						    ON cm.course_id = c.course_id
						LEFT JOIN course_unit cu
						    ON cu.course_module_id = cm.course_module_id
						LEFT JOIN course_unit_stats cus
						    ON cus.institution_id = ic.institution_id
						   AND cus.course_unit_id = cu.course_unit_id
						WHERE ic.institution_id = ?
						  AND cu.course_unit_id IS NOT NULL   -- one row per course_unit
						ORDER BY
						    ic.display_order,
						    cm.display_order,
						    cu.display_order
						""", CourseUnitDwellTimeRow.class,
				institutionId,
				AnalyticsNativeEventTypeId.PAGE_VIEW_COURSE_UNIT,
				timeZone,
				startDate,
				timeZone,
				endDate,
				RoleId.PATIENT,
				institutionId);

		// Group units by module, modules by course
		Map<UUID, List<CourseUnitDwellTimeRow>> unitsByModuleId = courseUnitDwellTimeRows.stream()
				.filter(row -> row.getCourseModuleId() != null)
				.collect(Collectors.groupingBy(
						CourseUnitDwellTimeRow::getCourseModuleId,
						LinkedHashMap::new,
						Collectors.toList()
				));

		Map<UUID, List<CourseModuleDwellTimeRow>> modulesByCourseId = courseModuleDwellTimeRows.stream()
				.filter(row -> row.getCourseId() != null)
				.collect(Collectors.groupingBy(
						CourseModuleDwellTimeRow::getCourseId,
						LinkedHashMap::new,
						Collectors.toList()
				));

		List<AnalyticsTableWidget> analyticsTableWidgets = new ArrayList<>(courses.size());

		for (Course course : courses) {
			UUID courseId = course.getCourseId();
			List<CourseModuleDwellTimeRow> modulesForCourse = modulesByCourseId.get(courseId);

			// No data for this course, so skip
			if (modulesForCourse == null || modulesForCourse.isEmpty())
				continue;

			List<AnalyticsWidgetTableRow> tableRows = new ArrayList<>();

			for (CourseModuleDwellTimeRow moduleRowData : modulesForCourse) {
				UUID moduleId = moduleRowData.getCourseModuleId();
				List<CourseUnitDwellTimeRow> unitsForModule =
						(moduleId != null) ? unitsByModuleId.get(moduleId) : null;

				// Build nested rows = units in this module
				List<AnalyticsWidgetTableRow> nestedRows = new ArrayList<>();

				if (unitsForModule != null) {
					for (CourseUnitDwellTimeRow unitRowData : unitsForModule) {
						AnalyticsWidgetTableRow nestedRow = new AnalyticsWidgetTableRow();

						nestedRow.setData(List.of(
								safeAnchorTag(format("/courses/%s/course-units/%s", course.getUrlName(), unitRowData.getCourseUnitId()), unitRowData.getCourseUnitTitle()),
								getFormatter().formatInteger(unitRowData.getAccountCount() == null ? 0 : unitRowData.getAccountCount()),
								formatDuration(unitRowData.getMeanDwellSecondsPerAccount()),
								formatDuration(unitRowData.getMedianDwellSecondsPerAccount())
						));

						nestedRows.add(nestedRow);
					}
				}

				// Module row from *module* stats (no double-counting accounts)
				AnalyticsWidgetTableRow moduleRow = new AnalyticsWidgetTableRow();

				moduleRow.setData(List.of(
						moduleRowData.getCourseModuleTitle(), // Not clickable HTML because clicks are used to expand/collapse nested unit display
						getFormatter().formatInteger(moduleRowData.getAccountCount() == null ? 0 : moduleRowData.getAccountCount()),
						formatDuration(moduleRowData.getMeanDwellSecondsPerAccount()),
						formatDuration(moduleRowData.getMedianDwellSecondsPerAccount())
				));

				moduleRow.setNestedRows(nestedRows);

				tableRows.add(moduleRow);
			}

			AnalyticsWidgetTableData tableData = new AnalyticsWidgetTableData();
			tableData.setHeaders(List.of(
					getStrings().get("Module"),
					getStrings().get("Total Number of Accounts"),
					getStrings().get("Mean Dwell Time"),
					getStrings().get("Median Dwell Time")
			));
			tableData.setRows(tableRows);

			AnalyticsTableWidget analyticsTableWidget = new AnalyticsTableWidget();
			analyticsTableWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_COURSE_DWELL_TIME);
			analyticsTableWidget.setWidgetTitle(
					getStrings().get("Course Module Dwell Time: {{courseTitle}}",
							Map.of("courseTitle", course.getTitle()))
			);
			analyticsTableWidget.setWidgetSubtitle(getStrings().get("Mean and median time (per account) spent on course unit pages within a course, broken down by module and then per-unit. Dwell times are determined by a 'heartbeat' every 5 seconds. If the user leaves a page before the first heartbeat, the dwell time is counted as 2.5 seconds."));
			analyticsTableWidget.setWidgetData(tableData);

			analyticsTableWidgets.add(analyticsTableWidget);
		}

		return analyticsTableWidgets;
	}

	@Nonnull
	private String formatDuration(@Nullable Number durationInSeconds) {
		final String NO_DATA_PLACEHOLDER = "--";

		if (durationInSeconds == null)
			return NO_DATA_PLACEHOLDER;

		String formatted = getFormatter().formatDuration(durationInSeconds);
		return formatted != null && formatted.trim().length() > 0 ? formatted : NO_DATA_PLACEHOLDER;
	}

	@NotThreadSafe
	protected static class CourseModuleDwellTimeRow {
		@Nullable
		private UUID courseId;
		@Nullable
		private String courseTitle;
		@Nullable
		private UUID courseModuleId;
		@Nullable
		private String courseModuleTitle;
		@Nullable
		private Double meanDwellSecondsPerAccount;
		@Nullable
		private Double medianDwellSecondsPerAccount;
		@Nullable
		private Long accountCount;

		@Nullable
		public UUID getCourseId() {
			return this.courseId;
		}

		public void setCourseId(@Nullable UUID courseId) {
			this.courseId = courseId;
		}

		@Nullable
		public String getCourseTitle() {
			return this.courseTitle;
		}

		public void setCourseTitle(@Nullable String courseTitle) {
			this.courseTitle = courseTitle;
		}

		@Nullable
		public UUID getCourseModuleId() {
			return this.courseModuleId;
		}

		public void setCourseModuleId(@Nullable UUID courseModuleId) {
			this.courseModuleId = courseModuleId;
		}

		@Nullable
		public String getCourseModuleTitle() {
			return this.courseModuleTitle;
		}

		public void setCourseModuleTitle(@Nullable String courseModuleTitle) {
			this.courseModuleTitle = courseModuleTitle;
		}

		@Nullable
		public Double getMeanDwellSecondsPerAccount() {
			return this.meanDwellSecondsPerAccount;
		}

		public void setMeanDwellSecondsPerAccount(@Nullable Double meanDwellSecondsPerAccount) {
			this.meanDwellSecondsPerAccount = meanDwellSecondsPerAccount;
		}

		@Nullable
		public Double getMedianDwellSecondsPerAccount() {
			return this.medianDwellSecondsPerAccount;
		}

		public void setMedianDwellSecondsPerAccount(@Nullable Double medianDwellSecondsPerAccount) {
			this.medianDwellSecondsPerAccount = medianDwellSecondsPerAccount;
		}

		@Nullable
		public Long getAccountCount() {
			return this.accountCount;
		}

		public void setAccountCount(@Nullable Long accountCount) {
			this.accountCount = accountCount;
		}
	}

	@NotThreadSafe
	protected static class CourseUnitDwellTimeRow {
		@Nullable
		private UUID courseId;
		@Nullable
		private String courseTitle;
		@Nullable
		private UUID courseModuleId;
		@Nullable
		private String courseModuleTitle;
		@Nullable
		private UUID courseUnitId;
		@Nullable
		private String courseUnitTitle;
		@Nullable
		private Double meanDwellSecondsPerAccount;
		@Nullable
		private Double medianDwellSecondsPerAccount;
		@Nullable
		private Long accountCount;

		@Nullable
		public UUID getCourseId() {
			return this.courseId;
		}

		public void setCourseId(@Nullable UUID courseId) {
			this.courseId = courseId;
		}

		@Nullable
		public String getCourseTitle() {
			return this.courseTitle;
		}

		public void setCourseTitle(@Nullable String courseTitle) {
			this.courseTitle = courseTitle;
		}

		@Nullable
		public UUID getCourseModuleId() {
			return this.courseModuleId;
		}

		public void setCourseModuleId(@Nullable UUID courseModuleId) {
			this.courseModuleId = courseModuleId;
		}

		@Nullable
		public String getCourseModuleTitle() {
			return this.courseModuleTitle;
		}

		public void setCourseModuleTitle(@Nullable String courseModuleTitle) {
			this.courseModuleTitle = courseModuleTitle;
		}

		@Nullable
		public UUID getCourseUnitId() {
			return this.courseUnitId;
		}

		public void setCourseUnitId(@Nullable UUID courseUnitId) {
			this.courseUnitId = courseUnitId;
		}

		@Nullable
		public String getCourseUnitTitle() {
			return this.courseUnitTitle;
		}

		public void setCourseUnitTitle(@Nullable String courseUnitTitle) {
			this.courseUnitTitle = courseUnitTitle;
		}

		@Nullable
		public Double getMeanDwellSecondsPerAccount() {
			return this.meanDwellSecondsPerAccount;
		}

		public void setMeanDwellSecondsPerAccount(@Nullable Double meanDwellSecondsPerAccount) {
			this.meanDwellSecondsPerAccount = meanDwellSecondsPerAccount;
		}

		@Nullable
		public Double getMedianDwellSecondsPerAccount() {
			return this.medianDwellSecondsPerAccount;
		}

		public void setMedianDwellSecondsPerAccount(@Nullable Double medianDwellSecondsPerAccount) {
			this.medianDwellSecondsPerAccount = medianDwellSecondsPerAccount;
		}

		@Nullable
		public Long getAccountCount() {
			return this.accountCount;
		}

		public void setAccountCount(@Nullable Long accountCount) {
			this.accountCount = accountCount;
		}
	}

	@Nonnull
	protected List<InstitutionColorValue> findChartColorValuesByInstitutionId(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);

		return getChartColorsValuesByInstitutionIdCache().computeIfAbsent(institutionId, (key) -> {
			List<InstitutionColorValue> institutionColorValues = getInstitutionService().findInstitutionColorValuesByInstitutionId(institutionId);

			// Try COBALT if no colors defined for this institution
			if (institutionColorValues.size() == 0 && institutionId != InstitutionId.COBALT)
				institutionColorValues = getInstitutionService().findInstitutionColorValuesByInstitutionId(InstitutionId.COBALT);

			// Should never occur; COBALT should be a failsafe
			if (institutionColorValues.size() == 0)
				throw new IllegalStateException(format("No colors available for %s institution", institutionId.name()));

			List<InstitutionColorValue> chartColors = new ArrayList<>(8);

			InstitutionColorValue p500 = institutionColorValues.stream().filter(icv -> icv.getColorValueId() == ColorValueId.P500).findFirst().orElse(null);
			InstitutionColorValue a500 = institutionColorValues.stream().filter(icv -> icv.getColorValueId() == ColorValueId.A500).findFirst().orElse(null);
			InstitutionColorValue d500 = institutionColorValues.stream().filter(icv -> icv.getColorValueId() == ColorValueId.D500).findFirst().orElse(null);
			InstitutionColorValue t500 = institutionColorValues.stream().filter(icv -> icv.getColorValueId() == ColorValueId.T500).findFirst().orElse(null);
			InstitutionColorValue w500 = institutionColorValues.stream().filter(icv -> icv.getColorValueId() == ColorValueId.W500).findFirst().orElse(null);
			InstitutionColorValue n500 = institutionColorValues.stream().filter(icv -> icv.getColorValueId() == ColorValueId.N500).findFirst().orElse(null);
			InstitutionColorValue i500 = institutionColorValues.stream().filter(icv -> icv.getColorValueId() == ColorValueId.I500).findFirst().orElse(null);

			if (p500 != null)
				chartColors.add(p500);
			if (a500 != null)
				chartColors.add(a500);
			if (d500 != null)
				chartColors.add(d500);
			if (t500 != null)
				chartColors.add(t500);
			if (w500 != null)
				chartColors.add(w500);
			if (n500 != null)
				chartColors.add(n500);
			if (i500 != null)
				chartColors.add(i500);

			if (chartColors.size() == 0)
				throw new IllegalStateException(format("No chart colors available for %s institution", institutionId.name()));

			return Collections.unmodifiableList(chartColors);
		});
	}

	@Nonnull
	private Optional<String> safeUrl(@Nullable String rawUrl) {
		rawUrl = trimToNull(rawUrl);

		if (rawUrl == null)
			return Optional.empty();

		// Reject control chars
		if (rawUrl.chars().anyMatch(ch -> ch < 0x20 && ch != '\t' && ch != '\n' && ch != '\r'))
			return Optional.empty();

		// Allow site-relative URLs like "/courses/example"
		if (rawUrl.startsWith("/")) {
			// Reject things like "/\u0001evil"
			try {
				URI uri = new URI(rawUrl);
				// Must be path-only: no scheme, no host, no authority
				if (uri.getScheme() == null && uri.getHost() == null && uri.getRawSchemeSpecificPart() != null)
					return Optional.of(uri.toString());
			} catch (Exception ignored) {
				return Optional.empty();
			}

			return Optional.empty();
		}

		// Otherwise it must be a valid http(s) absolute URL
		try {
			URI uri = new URI(rawUrl);
			String scheme = (uri.getScheme() == null) ? "" : uri.getScheme().toLowerCase(Locale.ROOT);

			if (!scheme.equals("http") && !scheme.equals("https"))
				return Optional.empty();

			return Optional.of(uri.toString());
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	@Nonnull
	private String safeAnchorTag(@Nullable String rawUrl) {
		return safeAnchorTag(rawUrl, null);
	}

	/**
	 * Renders a safe <a> tag or plaintext fallback.
	 *
	 * @param rawUrl      The user-entered URL
	 * @param rawLinkText Optional user-entered "friendly" text to use as the body of the <a> tag
	 */
	@Nonnull
	private String safeAnchorTag(@Nullable String rawUrl,
															 @Nullable String rawLinkText) {
		rawUrl = trimToNull(rawUrl);
		rawLinkText = trimToNull(rawLinkText);

		if (rawUrl == null)
			// No URL => empty string
			return "";

		String safeUrl = safeUrl(rawUrl).orElse(null);

		// If URL isn't safe or not http/https, fall back to plaintext.  Need 'http' to support local dev
		if (safeUrl == null || !(rawUrl.startsWith("/") || rawUrl.toLowerCase(Locale.ROOT).startsWith("http://") || rawUrl.toLowerCase(Locale.ROOT).startsWith("https://"))) {
			// Prefer the friendly text if present, otherwise show the raw URL
			String fallbackText = (rawLinkText != null) ? rawLinkText : rawUrl;
			return Encode.forHtmlContent(fallbackText);
		}

		// URL is safe; build an anchor tag.
		// Use friendly text if present, otherwise show the sanitized URL.
		String linkText = (rawLinkText != null) ? rawLinkText : safeUrl;

		return format(
				"<a href=\"%s\" target=\"_blank\" rel=\"noopener noreferrer\">%s</a>",
				Encode.forHtmlAttribute(safeUrl),
				Encode.forHtmlContent(linkText)
		);
	}

	@Nonnull
	protected ConcurrentMap<InstitutionId, List<InstitutionColorValue>> getChartColorsValuesByInstitutionIdCache() {
		return this.chartColorsValuesByInstitutionIdCache;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
	}

	@Nonnull
	protected CourseService getCourseService() {
		return this.courseServiceProvider.get();
	}

	@Nonnull
	protected Database getReadReplicaDatabase() {
		return this.databaseProvider.getReadReplicaDatabase();
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return this.formatter;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}