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

import com.cobaltplatform.api.model.analytics.AnalyticsLineChartWidget;
import com.cobaltplatform.api.model.analytics.AnalyticsWidgetChartData;
import com.cobaltplatform.api.model.db.AnalyticsReportGroup;
import com.cobaltplatform.api.model.db.AnalyticsReportGroupReport;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.ReportType.ReportTypeId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.db.DatabaseProvider;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

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
	public AnalyticsLineChartWidget createAccountVisitsWidget(@Nonnull InstitutionId institutionId,
																														@Nonnull LocalDate startDate,
																														@Nonnull LocalDate endDate) {
		requireNonNull(institutionId);
		requireNonNull(startDate);
		requireNonNull(endDate);

		ZoneId timeZone = getInstitutionService().findInstitutionById(institutionId).get().getTimeZone();

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

		List<AnalyticsWidgetChartData> widgetData = rows.stream()
				.map((row) -> {
					AnalyticsWidgetChartData element = new AnalyticsWidgetChartData();
					element.setColor("#102747");
					element.setLabel(row.getDay().toString());
					element.setCount(row.getDistinctAccounts());
					element.setCountDescription(getFormatter().formatInteger(row.getDistinctAccounts()));

					return element;
				})
				.collect(Collectors.toList());

		AnalyticsLineChartWidget lineChartWidget = new AnalyticsLineChartWidget();
		lineChartWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_ACCOUNT_VISITS);
		lineChartWidget.setWidgetTitle(getStrings().get("Account Visits"));
		lineChartWidget.setWidgetSubtitle(getStrings().get("TODO: Subtitle"));
		lineChartWidget.setWidgetChartLabel(getStrings().get("TODO: Chart Label"));
		lineChartWidget.setWidgetTotal(widgetTotal);
		lineChartWidget.setWidgetTotalDescription(getFormatter().formatInteger(widgetTotal));
		lineChartWidget.setWidgetData(widgetData);

		return lineChartWidget;
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