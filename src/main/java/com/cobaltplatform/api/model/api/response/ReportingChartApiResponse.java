package com.cobaltplatform.api.model.api.response;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;
import com.cobaltplatform.api.model.api.response.ReportingChartApiResponse.ReportingChartElementApiResponse.ReportingChartElementApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ReportingChartApiResponse.ReportingChartMetricApiResponse.ReportingChartMetricApiResponseFactory;
import com.cobaltplatform.api.model.service.ReportingChart;
import com.cobaltplatform.api.model.service.ReportingChartDisplayPreferenceId;
import com.cobaltplatform.api.model.service.ReportingChartElement;
import com.cobaltplatform.api.model.service.ReportingChartIntervalId;
import com.cobaltplatform.api.model.service.ReportingChartMetric;
import com.cobaltplatform.api.model.service.ReportingChartMetricTypeId;
import com.cobaltplatform.api.model.service.ReportingChartTypeId;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDate;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ReportingChartApiResponse {
	@Nonnull
	private final ReportingChartTypeId chartTypeId;
	@Nonnull
	private final ReportingChartIntervalId intervalId;
	@Nonnull
	private final ReportingChartDisplayPreferenceId displayPreferenceId;
	@Nonnull
	private final String title;
	@Nonnull
	private final String detail;
	@Nonnull
	private final List<ReportingChartElementApiResponse> elements;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ReportingChartApiResponseFactory {
		@Nonnull
		ReportingChartApiResponse create(@Nonnull ReportingChart chart);
	}

	@AssistedInject
	public ReportingChartApiResponse(@Nonnull Formatter formatter,
																	 @Nonnull Strings strings,
																	 @Nonnull ReportingChartElementApiResponseFactory reportingChartElementApiResponseFactory,
																	 @Assisted @Nonnull ReportingChart chart) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(reportingChartElementApiResponseFactory);
		requireNonNull(chart);

		this.chartTypeId = chart.getChartTypeId();
		this.intervalId = chart.getIntervalId();
		this.displayPreferenceId = chart.getDisplayPreferenceId();
		this.title = chart.getTitle();
		this.detail = chart.getDetail();
		this.elements = chart.getElements().stream()
				.map(element -> reportingChartElementApiResponseFactory.create(element))
				.collect(Collectors.toList());
	}

	@Immutable
	public static class ReportingChartElementApiResponse {
		@Nonnull
		private final String description;
		@Nonnull
		private final LocalDate startDate;
		@Nonnull
		private final String startDateDescription;
		@Nonnull
		private final LocalDate endDate;
		@Nonnull
		private final String endDateDescription;
		@Nonnull
		private final List<ReportingChartMetricApiResponse> metrics;

		// Note: requires FactoryModuleBuilder entry in AppModule
		@ThreadSafe
		public interface ReportingChartElementApiResponseFactory {
			@Nonnull
			ReportingChartElementApiResponse create(@Nonnull ReportingChartElement element);
		}

		@AssistedInject
		public ReportingChartElementApiResponse(@Nonnull Formatter formatter,
																						@Nonnull Strings strings,
																						@Nonnull ReportingChartMetricApiResponseFactory reportingChartMetricApiResponseFactory,
																						@Assisted @Nonnull ReportingChartElement element) {
			requireNonNull(formatter);
			requireNonNull(strings);
			requireNonNull(reportingChartMetricApiResponseFactory);
			requireNonNull(element);

			this.startDate = element.getStartDate();
			this.startDateDescription = formatter.formatDate(getStartDate(), FormatStyle.SHORT);
			this.endDate = element.getEndDate();
			this.endDateDescription = formatter.formatDate(getEndDate(), FormatStyle.SHORT);
			this.metrics = element.getMetrics().stream()
					.map(metric -> reportingChartMetricApiResponseFactory.create(metric))
					.collect(Collectors.toList());

			this.description = format("%s - %s", getStartDateDescription(), getEndDateDescription());
		}

		@Nonnull
		public String getDescription() {
			return description;
		}

		@Nonnull
		public LocalDate getStartDate() {
			return startDate;
		}

		@Nonnull
		public String getStartDateDescription() {
			return startDateDescription;
		}

		@Nonnull
		public LocalDate getEndDate() {
			return endDate;
		}

		@Nonnull
		public String getEndDateDescription() {
			return endDateDescription;
		}

		@Nonnull
		public List<ReportingChartMetricApiResponse> getMetrics() {
			return metrics;
		}
	}

	@Immutable
	public static class ReportingChartMetricApiResponse {
		@Nonnull
		private final ReportingChartMetricTypeId metricTypeId;
		@Nonnull
		private final String description;
		@Nonnull
		private final Double count;
		@Nonnull
		private final String countDescription;
		@Nonnull
		private final String color;
		@Nonnull
		private final Double alpha;

		// Note: requires FactoryModuleBuilder entry in AppModule
		@ThreadSafe
		public interface ReportingChartMetricApiResponseFactory {
			@Nonnull
			ReportingChartMetricApiResponse create(@Nonnull ReportingChartMetric metric);
		}

		@AssistedInject
		public ReportingChartMetricApiResponse(@Nonnull Formatter formatter,
																					 @Nonnull Strings strings,
																					 @Assisted @Nonnull ReportingChartMetric metric) {
			requireNonNull(formatter);
			requireNonNull(strings);
			requireNonNull(metric);

			this.metricTypeId = metric.getMetricTypeId();
			this.description = metric.getDescription();
			this.count = metric.getCount();
			this.countDescription = formatter.formatNumber(getCount());
			this.color = formatter.formatHexColor(metric.getHexColor());
			this.alpha = metric.getAlpha();
		}

		@Nonnull
		public ReportingChartMetricTypeId getMetricTypeId() {
			return metricTypeId;
		}

		@Nonnull
		public String getDescription() {
			return description;
		}

		@Nonnull
		public Double getCount() {
			return count;
		}

		@Nonnull
		public String getCountDescription() {
			return countDescription;
		}

		@Nonnull
		public String getColor() {
			return color;
		}

		@Nonnull
		public Double getAlpha() {
			return alpha;
		}
	}

	@Nonnull
	public ReportingChartTypeId getChartTypeId() {
		return chartTypeId;
	}

	@Nonnull
	public ReportingChartIntervalId getIntervalId() {
		return intervalId;
	}

	@Nonnull
	public ReportingChartDisplayPreferenceId getDisplayPreferenceId() {
		return displayPreferenceId;
	}

	@Nonnull
	public String getTitle() {
		return title;
	}

	@Nonnull
	public String getDetail() {
		return detail;
	}

	@Nonnull
	public List<ReportingChartElementApiResponse> getElements() {
		return elements;
	}
}