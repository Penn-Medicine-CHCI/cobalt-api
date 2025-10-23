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
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
	@Nonnull
	private final Provider<InstitutionService> institutionServiceProvider;
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
															@Nonnull DatabaseProvider databaseProvider,
															@Nonnull Strings strings,
															@Nonnull Formatter formatter) {
		requireNonNull(institutionServiceProvider);
		requireNonNull(databaseProvider);
		requireNonNull(strings);
		requireNonNull(formatter);

		this.institutionServiceProvider = institutionServiceProvider;
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
				.map(rawData -> "#1B4279")
				.collect(Collectors.toUnmodifiableList());

		List<String> backgroundColors = data.stream()
				.map(rawData -> "#102747")
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
		multiChartWidget.setWidgetSubtitle(getStrings().get("The total number of accounts who have visited {{platformName}} at least once", Map.of(
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
				.map(rawData -> "#1B4279")
				.collect(Collectors.toUnmodifiableList());

		List<String> backgroundColors = data.stream()
				.map(rawData -> "#102747")
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
							safeAnchorTagOrPlaintextFallback(row.getReferringUrl()),
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
				.map(rawData -> "#1B4279")
				.collect(Collectors.toUnmodifiableList());

		List<String> startedBackgroundColors = startedData.stream()
				.map(rawData -> "#102747")
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
				.map(rawData -> "#1E611E")
				.collect(Collectors.toUnmodifiableList());

		List<String> finishedBackgroundColors = finishedData.stream()
				.map(rawData -> "#2D912D")
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
		multiChartWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_ACCOUNT_VISITS);
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
	private Optional<String> safeUrl(@Nullable String rawUrl) {
		rawUrl = trimToNull(rawUrl);

		if (rawUrl == null)
			return Optional.empty();

		// Reject control chars
		if (rawUrl.chars().anyMatch(ch -> ch < 0x20 && ch != '\t' && ch != '\n' && ch != '\r'))
			return Optional.empty();

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
	private String safeAnchorTagOrPlaintextFallback(@Nullable String rawUrl) {
		rawUrl = trimToNull(rawUrl);

		if (rawUrl == null)
			return "";

		String safeUrl = safeUrl(rawUrl).orElse(null);

		if (safeUrl == null || !rawUrl.toLowerCase(Locale.ROOT).startsWith("https://"))
			return Encode.forHtmlContent(rawUrl);

		return format(
				"<a href=\"%s\" target=\"_blank\" rel=\"noopener noreferrer\">%s</a>",
				Encode.forHtmlAttribute(safeUrl),
				Encode.forHtmlContent(safeUrl)
		);
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
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