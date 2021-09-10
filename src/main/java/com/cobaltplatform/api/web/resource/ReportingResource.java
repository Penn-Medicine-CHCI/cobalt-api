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

package com.cobaltplatform.api.web.resource;

import com.lokalized.Strings;
import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.response.ReportingChartApiResponse.ReportingChartApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.ReportingRollup;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.ReportingChart;
import com.cobaltplatform.api.model.service.ReportingWindowId;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.ReportingService;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.response.ApiResponse;
import com.soklet.web.response.CustomResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class ReportingResource {
	@Nonnull
	private final ReportingService reportingService;
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final ReportingChartApiResponseFactory reportingChartApiResponseFactory;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Strings strings;

	@Inject
	public ReportingResource(@Nonnull ReportingService reportingService,
													 @Nonnull InstitutionService institutionService,
													 @Nonnull ReportingChartApiResponseFactory reportingChartApiResponseFactory,
													 @Nonnull Configuration configuration,
													 @Nonnull Provider<CurrentContext> currentContextProvider,
													 @Nonnull Strings strings) {
		requireNonNull(reportingService);
		requireNonNull(institutionService);
		requireNonNull(reportingChartApiResponseFactory);
		requireNonNull(configuration);
		requireNonNull(currentContextProvider);
		requireNonNull(strings);

		this.reportingService = reportingService;
		this.institutionService = institutionService;
		this.reportingChartApiResponseFactory = reportingChartApiResponseFactory;
		this.configuration = configuration;
		this.currentContextProvider = currentContextProvider;
		this.strings = strings;
	}

	@Nonnull
	@GET("/reporting/charts")
	@AuthenticationRequired
	public ApiResponse charts(@Nonnull @QueryParameter Optional<ReportingWindowId> reportingWindowId) {
		requireNonNull(reportingWindowId);

		Account account = authorizedAccount();
		Institution institution = getInstitutionService().findInstitutionById(account.getInstitutionId()).get();
		ReportingWindowId finalReportingWindowId = reportingWindowId.orElse(ReportingWindowId.MONTHLY_ALL_TIME);
		List<ReportingRollup> rollups = getReportingService().findRollups(institution.getInstitutionId(), finalReportingWindowId);
		List<ReportingChart> charts = getReportingService().chartsForRollups(rollups, finalReportingWindowId);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("charts", charts.stream()
					.map(chart -> getReportingChartApiResponseFactory().create(chart))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/reporting/csv")
	@AuthenticationRequired
	public CustomResponse csv(@Nonnull @QueryParameter Optional<ReportingWindowId> reportingWindowId,
														@Nonnull HttpServletResponse httpServletResponse) throws IOException {
		requireNonNull(reportingWindowId);
		requireNonNull(httpServletResponse);

		Account account = authorizedAccount();
		Institution institution = getInstitutionService().findInstitutionById(account.getInstitutionId()).get();
		ReportingWindowId finalReportingWindowId = reportingWindowId.orElse(ReportingWindowId.MONTHLY_ALL_TIME);
		List<ReportingRollup> rollups = getReportingService().findRollups(institution.getInstitutionId(), finalReportingWindowId);

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss", Locale.US).withZone(account.getTimeZone());
		String filename = format("%s-cobalt-reports.csv", dateTimeFormatter.format(Instant.now()));

		httpServletResponse.setContentType("text/csv");
		httpServletResponse.setHeader("Content-Encoding", "gzip");
		httpServletResponse.setHeader("Content-Disposition", format("attachment; filename=\"%s\"", filename));

		try (PrintWriter printWriter = new PrintWriter(new GZIPOutputStream(httpServletResponse.getOutputStream()))) {
			getReportingService().writeCsvForRollups(rollups, finalReportingWindowId, account.getTimeZone(), printWriter);
		}

		return CustomResponse.instance();
	}

	@Nonnull
	protected Account authorizedAccount() {
		Account account = getCurrentContext().getAccount().get();

		if (account.getRoleId() != RoleId.ADMINISTRATOR)
			throw new AuthorizationException();

		return account;
	}

	@Nonnull
	protected ReportingService getReportingService() {
		return reportingService;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return institutionService;
	}

	@Nonnull
	protected ReportingChartApiResponseFactory getReportingChartApiResponseFactory() {
		return reportingChartApiResponseFactory;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}
}