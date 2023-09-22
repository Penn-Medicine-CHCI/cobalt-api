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

import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.integration.google.GoogleAnalyticsDataClient;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.google.analytics.data.v1beta.DateRange;
import com.google.analytics.data.v1beta.Dimension;
import com.google.analytics.data.v1beta.Metric;
import com.google.analytics.data.v1beta.Row;
import com.google.analytics.data.v1beta.RunReportRequest;
import com.google.analytics.data.v1beta.RunReportResponse;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class AnalyticsService {
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public AnalyticsService(@Nonnull EnterprisePluginProvider enterprisePluginProvider,
													@Nonnull Database database,
													@Nonnull Strings strings) {
		requireNonNull(enterprisePluginProvider);
		requireNonNull(database);
		requireNonNull(strings);

		this.enterprisePluginProvider = enterprisePluginProvider;
		this.database = database;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public AnalyticsResultNewVersusReturning newVersusReturning(@Nonnull InstitutionId institutionId,
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
	protected Logger getLogger() {
		return this.logger;
	}
}
