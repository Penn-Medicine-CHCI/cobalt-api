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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.ReportingRollup;
import com.cobaltplatform.api.model.service.ReportingChart;
import com.cobaltplatform.api.model.service.ReportingChartColor;
import com.cobaltplatform.api.model.service.ReportingChartDisplayPreferenceId;
import com.cobaltplatform.api.model.service.ReportingChartElement;
import com.cobaltplatform.api.model.service.ReportingChartIntervalId;
import com.cobaltplatform.api.model.service.ReportingChartMetric;
import com.cobaltplatform.api.model.service.ReportingChartMetricTypeId;
import com.cobaltplatform.api.model.service.ReportingChartTypeId;
import com.cobaltplatform.api.model.service.ReportingWindowId;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class ReportingService {
	@Nonnull
	private final Database database;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public ReportingService(@Nonnull Database database,
													@Nonnull Strings strings) {
		requireNonNull(database);
		requireNonNull(strings);

		this.database = database;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public List<ReportingRollup> findRollups(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);
		return findRollups(institutionId, ReportingWindowId.MONTHLY_ALL_TIME);
	}

	@Nonnull
	public List<ReportingRollup> findRollups(@Nonnull InstitutionId institutionId,
																					 @Nonnull ReportingWindowId reportingWindowId) {
		requireNonNull(institutionId);
		requireNonNull(reportingWindowId);

		List<ReportingRollup> rollups;

		if (reportingWindowId.getReportingChartIntervalId() == ReportingChartIntervalId.MONTHLY) {
			StringBuilder sql = new StringBuilder("SELECT * FROM reporting_monthly_rollup WHERE institution_id=? " +
					"AND make_date(year, month, 1) < now() " +
					"ORDER BY year DESC, month DESC ");

			if (reportingWindowId == ReportingWindowId.MONTHLY_3_MONTHS)
				sql.append("LIMIT 3");
			else if (reportingWindowId == ReportingWindowId.MONTHLY_12_MONTHS)
				sql.append("LIMIT 12");

			rollups = getDatabase().queryForList(sql.toString(), ReportingRollup.class, institutionId);
		} else if (reportingWindowId.getReportingChartIntervalId() == ReportingChartIntervalId.WEEKLY) {
			StringBuilder sql = new StringBuilder("SELECT * FROM reporting_weekly_rollup WHERE institution_id=? " +
					"AND make_date(year, month, week) < now() " +
					"ORDER BY year DESC, month DESC, week DESC ");

			if (reportingWindowId == ReportingWindowId.WEEKLY_4_WEEKS)
				sql.append("LIMIT 4");
			else if (reportingWindowId == ReportingWindowId.WEEKLY_8_WEEKS)
				sql.append("LIMIT 8");
			else if (reportingWindowId == ReportingWindowId.WEEKLY_12_WEEKS)
				sql.append("LIMIT 12");

			rollups = getDatabase().queryForList(sql.toString(), ReportingRollup.class, institutionId);
		} else {
			throw new UnsupportedOperationException(format("Unexpected value %s.%s", ReportingChartIntervalId.class.getSimpleName(), reportingWindowId.getReportingChartIntervalId().name()));
		}

		Collections.reverse(rollups);

		return rollups;
	}

	@Nonnull
	public List<ReportingChart> chartsForRollups(@Nullable List<ReportingRollup> rollups,
																							 @Nonnull ReportingWindowId reportingWindowId) {
		requireNonNull(reportingWindowId);

		if (rollups == null || rollups.size() == 0)
			return Collections.emptyList();

		List<ReportingChart> charts = new ArrayList<>(ReportingChartTypeId.values().length);

		// Accounts chart
		charts.add(chartForRollups(rollups, () -> {
			ReportingChart reportingChart = new ReportingChart();
			reportingChart.setChartTypeId(ReportingChartTypeId.ACCOUNTS);
			reportingChart.setDisplayPreferenceId(ReportingChartDisplayPreferenceId.LINE);
			reportingChart.setTitle(getStrings().get("users"));
			reportingChart.setDetail(descriptionForReportingWindow(reportingWindowId));

			return reportingChart;
		}, (rollup) -> {
			List<ReportingChartMetric> metrics = new ArrayList<>(1);

			ReportingChartMetric newMetric = new ReportingChartMetric();
			newMetric.setMetricTypeId(ReportingChartMetricTypeId.ACCOUNTS_NEW);
			newMetric.setCount(rollup.getUserCount().doubleValue());
			newMetric.setDescription(getStrings().get("new"));
			newMetric.setHexColor(ReportingChartColor.BLUE.getHexValue());
			newMetric.setAlpha(1.0);
			metrics.add(newMetric);

			return metrics;
		}));

		// Appointments chart
		charts.add(chartForRollups(rollups, () -> {
			ReportingChart reportingChart = new ReportingChart();
			reportingChart.setChartTypeId(ReportingChartTypeId.APPOINTMENTS);
			reportingChart.setDisplayPreferenceId(ReportingChartDisplayPreferenceId.BAR);
			reportingChart.setTitle(getStrings().get("appointments"));
			reportingChart.setDetail(descriptionForReportingWindow(reportingWindowId));

			return reportingChart;
		}, (rollup) -> {
			List<ReportingChartMetric> metrics = new ArrayList<>(3);

			ReportingChartMetric bookedMetric = new ReportingChartMetric();
			bookedMetric.setMetricTypeId(ReportingChartMetricTypeId.APPOINTMENTS_BOOKED);
			bookedMetric.setCount(rollup.getAppointmentCount().doubleValue());
			bookedMetric.setDescription(getStrings().get("booked"));
			bookedMetric.setHexColor(ReportingChartColor.BLUE.getHexValue());
			bookedMetric.setAlpha(1.0);
			metrics.add(bookedMetric);

			ReportingChartMetric canceledMetric = new ReportingChartMetric();
			canceledMetric.setMetricTypeId(ReportingChartMetricTypeId.APPOINTMENTS_CANCELED);
			canceledMetric.setCount(rollup.getAppointmentCanceledCount().doubleValue());
			canceledMetric.setDescription(getStrings().get("canceled"));
			canceledMetric.setHexColor(ReportingChartColor.YELLOW.getHexValue());
			canceledMetric.setAlpha(1.0);
			metrics.add(canceledMetric);

			ReportingChartMetric completedMetric = new ReportingChartMetric();
			completedMetric.setMetricTypeId(ReportingChartMetricTypeId.APPOINTMENTS_COMPLETED);
			completedMetric.setCount(rollup.getAppointmentCompletedCount().doubleValue());
			completedMetric.setDescription(getStrings().get("completed"));
			completedMetric.setHexColor(ReportingChartColor.GREEN.getHexValue());
			completedMetric.setAlpha(1.0);
			metrics.add(completedMetric);

			return metrics;
		}));

		return charts;
	}

	@Nonnull
	protected String descriptionForReportingWindow(@Nonnull ReportingWindowId reportingWindowId) {
		requireNonNull(reportingWindowId);

		if (reportingWindowId == ReportingWindowId.MONTHLY_ALL_TIME)
			return getStrings().get("All time");
		if (reportingWindowId == ReportingWindowId.MONTHLY_3_MONTHS)
			return getStrings().get("Last 3 months");
		if (reportingWindowId == ReportingWindowId.MONTHLY_12_MONTHS)
			return getStrings().get("Last 12 months");
		if (reportingWindowId == ReportingWindowId.WEEKLY_4_WEEKS)
			return getStrings().get("Last 4 weeks");
		if (reportingWindowId == ReportingWindowId.WEEKLY_8_WEEKS)
			return getStrings().get("Last 8 weeks");
		if (reportingWindowId == ReportingWindowId.WEEKLY_12_WEEKS)
			return getStrings().get("Last 12 weeks");

		throw new UnsupportedOperationException(format("Unexpected value %s.%s", ReportingChartIntervalId.class.getSimpleName(), reportingWindowId.getReportingChartIntervalId().name()));
	}

	@Nonnull
	protected ReportingChart chartForRollups(@Nonnull List<ReportingRollup> rollups,
																					 @Nonnull Supplier<ReportingChart> chartSupplier,
																					 @Nonnull Function<ReportingRollup, List<ReportingChartMetric>> metricsProvider) {
		requireNonNull(rollups);
		requireNonNull(chartSupplier);
		requireNonNull(metricsProvider);

		List<ReportingChartElement> elements = new ArrayList<>();

		for (ReportingRollup rollup : rollups) {
			List<ReportingChartMetric> metrics = metricsProvider.apply(rollup);
			LocalDate startDate;
			LocalDate endDate;

			if (rollup.getDayOfMonth() == null) {
				// Monthly data
				YearMonth yearMonth = YearMonth.of(rollup.getYear(), rollup.getMonth());
				startDate = yearMonth.atDay(1);
				endDate = yearMonth.atEndOfMonth();
			} else {
				// Weekly data
				startDate = LocalDate.of(rollup.getYear(), rollup.getMonth(), rollup.getDayOfMonth());
				endDate = startDate.plusWeeks(1).minusDays(1);
			}

			ReportingChartElement element = new ReportingChartElement();
			element.setStartDate(startDate);
			element.setEndDate(endDate);
			element.setMetrics(metrics);

			elements.add(element);
		}

		ReportingChart reportingChart = chartSupplier.get();
		reportingChart.setElements(elements);

		return reportingChart;
	}

	public void writeCsvForRollups(@Nonnull List<ReportingRollup> rollups,
																 @Nonnull ReportingWindowId reportingWindowId,
																 @Nonnull ZoneId timeZone,
																 @Nonnull Writer writer) {
		requireNonNull(rollups);
		requireNonNull(reportingWindowId);
		requireNonNull(timeZone);
		requireNonNull(writer);

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("M/d/yyyy 'at' h:mma z", Locale.US).withZone(timeZone);

		List<String> headerColumns = new ArrayList<>();
		headerColumns.add("year");
		headerColumns.add("month");

		if (reportingWindowId.getReportingChartIntervalId() == ReportingChartIntervalId.WEEKLY)
			headerColumns.add("week");

		headerColumns.add("user_count");
		headerColumns.add("apt_count");
		headerColumns.add("apt_completed_count");
		headerColumns.add("apt_canceled_count");
		headerColumns.add("apt_avail_count");
		headerColumns.add("prov_count");
		headerColumns.add("last_updated");

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headerColumns.toArray(new String[0])))) {
			for (ReportingRollup rollup : rollups) {
				List<Object> record = new ArrayList<>();

				record.add(rollup.getYear());
				record.add(rollup.getMonth());

				if (reportingWindowId.getReportingChartIntervalId() == ReportingChartIntervalId.WEEKLY)
					record.add(rollup.getDayOfMonth());

				record.add(rollup.getUserCount());
				record.add(rollup.getAppointmentCount());
				record.add(rollup.getAppointmentCompletedCount());
				record.add(rollup.getAppointmentCanceledCount());
				record.add(rollup.getAppointmentAvailableCount());
				record.add(rollup.getProviderCount());
				record.add(dateTimeFormatter.format(rollup.getLastUpdated()));

				csvPrinter.printRecord(record.toArray(new Object[0]));
			}

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
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
	protected Logger getLogger() {
		return logger;
	}
}
