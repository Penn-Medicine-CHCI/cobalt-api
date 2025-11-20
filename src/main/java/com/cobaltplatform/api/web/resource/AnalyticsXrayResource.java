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

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.analytics.AnalyticsWidget;
import com.cobaltplatform.api.model.api.response.AnalyticsReportGroupApiResponse.AnalyticsReportGroupApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AnalyticsReportGroup;
import com.cobaltplatform.api.model.db.AnalyticsReportGroupReport;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AnalyticsXrayService;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.util.db.ReadReplica;
import com.cobaltplatform.api.model.analytics.AnalyticsMultiChartWidget;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class AnalyticsXrayResource {
	@Nonnull
	private final AnalyticsXrayService analyticsXrayService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final AnalyticsReportGroupApiResponseFactory analyticsReportGroupApiResponseFactory;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public AnalyticsXrayResource(@Nonnull AnalyticsXrayService analyticsXrayService,
															 @Nonnull AuthorizationService authorizationService,
															 @Nonnull AnalyticsReportGroupApiResponseFactory analyticsReportGroupApiResponseFactory,
															 @Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(analyticsXrayService);
		requireNonNull(authorizationService);
		requireNonNull(analyticsReportGroupApiResponseFactory);
		requireNonNull(currentContextProvider);

		this.analyticsXrayService = analyticsXrayService;
		this.authorizationService = authorizationService;
		this.analyticsReportGroupApiResponseFactory = analyticsReportGroupApiResponseFactory;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/analytics-report-groups")
	@AuthenticationRequired
	@ReadReplica
	public ApiResponse analyticsReportGroups() {
		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = account.getInstitutionId();

		if (!getAuthorizationService().canViewAnalytics(institutionId, account))
			throw new AuthorizationException();

		List<AnalyticsReportGroup> analyticsReportGroups = getAnalyticsXrayService().findAnalyticsReportGroupsByInstitutionId(institutionId);

		return new ApiResponse(Map.of(
				"analyticsReportGroups", analyticsReportGroups.stream()
						.map(analyticsReportGroup -> getAnalyticsReportGroupApiResponseFactory().create(analyticsReportGroup))
						.collect(Collectors.toList())
		));
	}

	@Nonnull
	@GET("/analytics-report-groups/{analyticsReportGroupId}/widgets")
	@AuthenticationRequired
	@ReadReplica
	public ApiResponse widgetsByAnalyticsReportGroupId(@Nonnull @PathParameter UUID analyticsReportGroupId,
																										 @Nonnull @QueryParameter LocalDate startDate,
																										 @Nonnull @QueryParameter LocalDate endDate) {
		requireNonNull(analyticsReportGroupId);
		requireNonNull(startDate);
		requireNonNull(endDate);

		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = account.getInstitutionId();

		if (!getAuthorizationService().canViewAnalytics(institutionId, account))
			throw new AuthorizationException();

		List<AnalyticsReportGroupReport> reports = getAnalyticsXrayService().findAnalyticsReportGroupReportsByAnalyticsReportGroupId(analyticsReportGroupId);
		List<AnalyticsWidget> widgets = new ArrayList<>(reports.size());

		// TODO: temporary hack, remove
		endDate = endDate.plusDays(1);

		// Based on report type, pull data for it
		for (AnalyticsReportGroupReport report : reports) {
			switch (report.getReportTypeId()) {
				// ** Account-related reports

				// N of unique people that accessed the website
				case ADMIN_ANALYTICS_ACCOUNT_VISITS ->
						widgets.add(getAnalyticsXrayService().createAccountVisitsWidget(institutionId, startDate, endDate));

				// N of people that created an account
				case ADMIN_ANALYTICS_ACCOUNT_CREATION ->
						widgets.add(getAnalyticsXrayService().createAccountsCreatedWidget(institutionId, startDate, endDate));

				// N of repeat users (logged on more than once)
				case ADMIN_ANALYTICS_ACCOUNT_REPEAT_VISITS ->
						widgets.add(getAnalyticsXrayService().createAccountRepeatVisitsWidget(institutionId, startDate, endDate));

				// List of websites from which the user accessed the platform
				case ADMIN_ANALYTICS_ACCOUNT_REFERRER ->
						widgets.add(getAnalyticsXrayService().createAccountReferrersWidget(institutionId, startDate, endDate));

				// N of people who started the onboarding screening flow and N of people who finished
				case ADMIN_ANALYTICS_ACCOUNT_ONBOARDING_RESULTS ->
						widgets.add(getAnalyticsXrayService().createAccountOnboardingResultsWidget(institutionId, startDate, endDate));

				// Using IP address to organized by zip code
				//	case ADMIN_ANALYTICS_ACCOUNT_LOCATION -> throw new UnsupportedOperationException("TODO");

				// ** Course-related reports

				// N of users per course
				case ADMIN_ANALYTICS_COURSE_ACCOUNT_VISITS ->
						widgets.add(getAnalyticsXrayService().createCourseAccountVisitsWidget(institutionId, startDate, endDate));

				// N of users doing more than one course
				case ADMIN_ANALYTICS_COURSE_AGGREGATE_VISITS ->
						widgets.add(getAnalyticsXrayService().createCourseAggregateVisitsWidget(institutionId, startDate, endDate));

				// N of people that clicked/opened a unique module
				// case ADMIN_ANALYTICS_COURSE_MODULE_ACCOUNT_VISITS -> throw new UnsupportedOperationException("TODO");

				// N of minutes it takes for people to complete a course (mean, median, mode)
				// case ADMIN_ANALYTICS_COURSE_DWELL_TIME -> throw new UnsupportedOperationException("TODO");

				// N of minutes it takes for people to get through a unique module (mean, median, mode)
				// case ADMIN_ANALYTICS_COURSE_MODULE_DWELL_TIME -> throw new UnsupportedOperationException("TODO");

				// N of people who complete each course
				case ADMIN_ANALYTICS_COURSE_COMPLETION ->
						widgets.add(getAnalyticsXrayService().createCourseCompletionWidget(institutionId, startDate, endDate));

				// N of people completing one or more course
				case ADMIN_ANALYTICS_COURSE_AGGREGATE_COMPLETIONS ->
						widgets.add(getAnalyticsXrayService().createCourseAggregateCompletionsWidget(institutionId, startDate, endDate));

				// N of people who complete a unique module
				case ADMIN_ANALYTICS_COURSE_MODULE_COMPLETION -> {
					List<AnalyticsMultiChartWidget> widgetList =
							getAnalyticsXrayService().createCourseModuleCompletionWidget(
									institutionId, startDate, endDate);
					widgets.addAll(widgetList);
				}

				// default ->
				// 	throw new UnsupportedOperationException(format("Unsupported %s value '%s' for analytics_report_group_id %s",
				// 		ReportTypeId.class.getSimpleName(), report.getReportTypeId().name(), analyticsReportGroupId));
			}
		}

		return new ApiResponse(
				Map.of("widgets", widgets)
		);
	}

	@Nonnull
	protected AnalyticsXrayService getAnalyticsXrayService() {
		return this.analyticsXrayService;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return this.authorizationService;
	}

	@Nonnull
	protected AnalyticsReportGroupApiResponseFactory getAnalyticsReportGroupApiResponseFactory() {
		return this.analyticsReportGroupApiResponseFactory;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
