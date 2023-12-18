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

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Color.ColorId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.ReportType.ReportTypeId;
import com.cobaltplatform.api.model.db.ScreeningFlow;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.AccountSourceForInstitution;
import com.cobaltplatform.api.service.AnalyticsService;
import com.cobaltplatform.api.service.AnalyticsService.AnalyticsResultNewVersusReturning;
import com.cobaltplatform.api.service.AnalyticsService.AppointmentClickToCallCount;
import com.cobaltplatform.api.service.AnalyticsService.AppointmentCount;
import com.cobaltplatform.api.service.AnalyticsService.ContentPageView;
import com.cobaltplatform.api.service.AnalyticsService.CrisisTriggerCount;
import com.cobaltplatform.api.service.AnalyticsService.GroupSessionCount;
import com.cobaltplatform.api.service.AnalyticsService.GroupSessionSummary;
import com.cobaltplatform.api.service.AnalyticsService.ResourceAndTopicSummary;
import com.cobaltplatform.api.service.AnalyticsService.ScreeningSessionCompletion;
import com.cobaltplatform.api.service.AnalyticsService.SectionCountSummary;
import com.cobaltplatform.api.service.AnalyticsService.TagGroupPageView;
import com.cobaltplatform.api.service.AnalyticsService.TagPageView;
import com.cobaltplatform.api.service.AnalyticsService.TopicCenterInteraction;
import com.cobaltplatform.api.service.AnalyticsService.TrafficSourceMediumCount;
import com.cobaltplatform.api.service.AnalyticsService.TrafficSourceReferrerCount;
import com.cobaltplatform.api.service.AnalyticsService.TrafficSourceSummary;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.ScreeningService;
import com.cobaltplatform.api.util.Formatter;
import com.lokalized.Strings;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.response.ApiResponse;
import com.soklet.web.response.CustomResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class AnalyticsResource {
	@Nonnull
	private final AnalyticsService analyticsService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final ScreeningService screeningService;
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Logger logger;

	@Inject
	public AnalyticsResource(@Nonnull AnalyticsService analyticsService,
													 @Nonnull AuthorizationService authorizationService,
													 @Nonnull ScreeningService screeningService,
													 @Nonnull InstitutionService institutionService,
													 @Nonnull Configuration configuration,
													 @Nonnull Provider<CurrentContext> currentContextProvider,
													 @Nonnull Strings strings,
													 @Nonnull Formatter formatter) {
		requireNonNull(analyticsService);
		requireNonNull(authorizationService);
		requireNonNull(screeningService);
		requireNonNull(institutionService);
		requireNonNull(configuration);
		requireNonNull(currentContextProvider);
		requireNonNull(strings);
		requireNonNull(formatter);

		this.analyticsService = analyticsService;
		this.authorizationService = authorizationService;
		this.screeningService = screeningService;
		this.institutionService = institutionService;
		this.configuration = configuration;
		this.currentContextProvider = currentContextProvider;
		this.strings = strings;
		this.formatter = formatter;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/analytics")
	@AuthenticationRequired
	public ApiResponse analytics(@Nonnull @QueryParameter LocalDate startDate,
															 @Nonnull @QueryParameter LocalDate endDate) {
		requireNonNull(startDate);
		requireNonNull(endDate);

		InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canViewAnalytics(institutionId, account))
			throw new AuthorizationException();

		// Overview analytics
		AnalyticsResultNewVersusReturning activeUserCountsNewVersusReturning = getAnalyticsService().findActiveUserCountsNewVersusReturning(institutionId, startDate, endDate);
		Map<AccountSourceId, Long> activeUserCountsByAccountSourceId = getAnalyticsService().findActiveUserCountsByAccountSourceId(institutionId, startDate, endDate);
		List<SectionCountSummary> sectionCountSummaries = getAnalyticsService().findSectionCountSummaries(institutionId, startDate, endDate);
		TrafficSourceSummary trafficSourceSummary = getAnalyticsService().findTrafficSourceSummary(institutionId, startDate, endDate);
		Map<String, Long> activeUserCountsByInstitutionLocation = getAnalyticsService().findActiveUserCountsByInstitutionLocation(institutionId, startDate, endDate);

		// Assessments and appointments analytics
		Map<UUID, ScreeningSessionCompletion> screeningSessionCompletions = getAnalyticsService().findClinicalScreeningSessionCompletionsByScreeningFlowId(institutionId, startDate, endDate);
		Map<UUID, SortedMap<String, Long>> screeningSessionSeverityCounts = getAnalyticsService().findClinicalScreeningSessionSeverityCountsByDescriptionByScreeningFlowId(institutionId, startDate, endDate);
		List<CrisisTriggerCount> crisisTriggerCounts = getAnalyticsService().findCrisisTriggerCounts(institutionId, startDate, endDate);
		List<AppointmentCount> appointmentCounts = getAnalyticsService().findAppointmentCounts(institutionId, startDate, endDate);
		List<AppointmentClickToCallCount> appointmentClickToCallCounts = getAnalyticsService().findAppointmentClickToCallCounts(institutionId, startDate, endDate);

		// Group Sessions
		GroupSessionSummary groupSessionSummary = getAnalyticsService().findGroupSessionSummary(institutionId, startDate, endDate);

		// Resources and Topics
		ResourceAndTopicSummary resourceAndTopicSummary = getAnalyticsService().findResourceAndTopicSummary(institutionId, startDate, endDate);

		Map<String, Object> response = new HashMap<>();
		response.put("sections", Map.of(
				"overview", Map.of(
						"activeUserCountsNewVersusReturning", activeUserCountsNewVersusReturning,
						"activeUserCountsByAccountSourceId", activeUserCountsByAccountSourceId,
						"activeUserCountsByInstitutionLocation", activeUserCountsByInstitutionLocation,
						"sectionCountSummaries", sectionCountSummaries,
						"trafficSourceSummary", trafficSourceSummary
				),
				"assessmentsAndAppointments", Map.of(
						"screeningSessionCompletions", screeningSessionCompletions,
						"screeningSessionSeverityCounts", screeningSessionSeverityCounts,
						"crisisTriggerCounts", crisisTriggerCounts,
						"appointmentCounts", appointmentCounts,
						"appointmentClickToCallCounts", appointmentClickToCallCounts
				),
				"groupSessions", Map.of(
						"groupSessionSummary", groupSessionSummary
				),
				"resourcesAndTopics", Map.of(
						"resourceAndTopicSummary", resourceAndTopicSummary
				)
		));

		return new ApiResponse(response);
	}

	@Nonnull
	@GET("/analytics/overview")
	@AuthenticationRequired
	public Object analyticsOverview(@Nonnull HttpServletResponse httpServletResponse,
																	@Nonnull @QueryParameter LocalDate startDate,
																	@Nonnull @QueryParameter LocalDate endDate) {
		requireNonNull(startDate);
		requireNonNull(endDate);

		InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canViewAnalytics(institutionId, account))
			throw new AuthorizationException();

		List<String> colorCssRepresentations = getInstitutionService().findInstitutionColorValuesByInstitutionId(institutionId, ColorId.BRAND_PRIMARY).stream()
				.map(institutionColorValue -> institutionColorValue.getCssRepresentation())
				.collect(Collectors.toList());

		AnalyticsResultNewVersusReturning activeUserCountsNewVersusReturning = getAnalyticsService().findActiveUserCountsNewVersusReturning(institutionId, startDate, endDate);
		Map<AccountSourceId, Long> activeUserCountsByAccountSourceId = getAnalyticsService().findActiveUserCountsByAccountSourceId(institutionId, startDate, endDate);
		List<SectionCountSummary> sectionCountSummaries = getAnalyticsService().findSectionCountSummaries(institutionId, startDate, endDate);
		TrafficSourceSummary trafficSourceSummary = getAnalyticsService().findTrafficSourceSummary(institutionId, startDate, endDate);
		Map<String, Long> activeUserCountsByInstitutionLocation = getAnalyticsService().findActiveUserCountsByInstitutionLocation(institutionId, startDate, endDate);

		boolean useExampleData = !getConfiguration().isProduction();

		if (useExampleData) {
			activeUserCountsNewVersusReturning = new AnalyticsResultNewVersusReturning(5000L, 1234L, 0L);

			activeUserCountsByAccountSourceId = new TreeMap<>(Map.of(
					AccountSourceId.EMAIL_PASSWORD, 123L,
					AccountSourceId.ANONYMOUS, 5000L,
					AccountSourceId.COBALT_SSO, 1000L
			));

			SectionCountSummary sectionCountSummary1 = new SectionCountSummary();
			sectionCountSummary1.setSection("Home");
			sectionCountSummary1.setUserCount(1234L);
			sectionCountSummary1.setActiveUserCount(300L);
			sectionCountSummary1.setPageViewCount(5678L);

			sectionCountSummaries = List.of(
					sectionCountSummary1
			);

			trafficSourceSummary = new TrafficSourceSummary();
			trafficSourceSummary.setTrafficSourceMediumCounts(List.of(
					new TrafficSourceMediumCount() {{
						setUserCount(5000L);
						setMedium("Direct");
					}},
					new TrafficSourceMediumCount() {{
						setUserCount(750L);
						setMedium("Referral");
					}}
			));

			trafficSourceSummary.setTrafficSourceReferrerCounts(List.of(
					new TrafficSourceReferrerCount() {{
						setUserCount(500L);
						setReferrer("Google");
					}},
					new TrafficSourceReferrerCount() {{
						setUserCount(150L);
						setReferrer("website1.com");
					}},
					new TrafficSourceReferrerCount() {{
						setUserCount(100L);
						setReferrer("website2.com");
					}}
			));

			trafficSourceSummary.setUsersFromNonDirectTrafficSourceMediumCount(750L);
			trafficSourceSummary.setUsersFromTrafficSourceMediumTotalCount(5750L);
			trafficSourceSummary.setUsersFromNonDirectTrafficSourceMediumPercentage((double) trafficSourceSummary.getUsersFromNonDirectTrafficSourceMediumCount() / (double) trafficSourceSummary.getUsersFromTrafficSourceMediumTotalCount());

			activeUserCountsByInstitutionLocation = new TreeMap<>(Map.of(
					"Location 1", 123L,
					"Location 2", 456L,
					"Location 3", 789L,
					"Location 4", 1234L,
					"Location 5", 2345L
			));
		}

		// Group 1
		AnalyticsPieChartWidget visitsWidget = new AnalyticsPieChartWidget();
		visitsWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_VISITS);
		visitsWidget.setWidgetTitle(getStrings().get("Visits"));
		visitsWidget.setWidgetSubtitle(getStrings().get("Total"));
		visitsWidget.setWidgetChartLabel(getStrings().get("Visits"));
		visitsWidget.setWidgetTotal(activeUserCountsNewVersusReturning.getNewUserCount() + activeUserCountsNewVersusReturning.getReturningUserCount());
		visitsWidget.setWidgetTotalDescription(getFormatter().formatNumber(visitsWidget.getWidgetTotal()));

		AnalyticsWidgetChartData visitsWidgetNewChartData = new AnalyticsWidgetChartData();
		visitsWidgetNewChartData.setLabel(getStrings().get("New"));
		visitsWidgetNewChartData.setCount(activeUserCountsNewVersusReturning.getNewUserCount());
		visitsWidgetNewChartData.setCountDescription(getFormatter().formatNumber(activeUserCountsNewVersusReturning.getNewUserCount()));
		visitsWidgetNewChartData.setColor(colorCssRepresentations.get(0 % colorCssRepresentations.size()));

		AnalyticsWidgetChartData visitsWidgetReturningChartData = new AnalyticsWidgetChartData();
		visitsWidgetReturningChartData.setLabel(getStrings().get("Returning"));
		visitsWidgetReturningChartData.setCount(activeUserCountsNewVersusReturning.getReturningUserCount());
		visitsWidgetReturningChartData.setCountDescription(getFormatter().formatNumber(activeUserCountsNewVersusReturning.getReturningUserCount()));
		visitsWidgetReturningChartData.setColor(colorCssRepresentations.get(1 % colorCssRepresentations.size()));

		visitsWidget.setWidgetData(List.of(visitsWidgetNewChartData, visitsWidgetReturningChartData));

		AnalyticsPieChartWidget usersWidget = new AnalyticsPieChartWidget();
		usersWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_USERS);
		usersWidget.setWidgetTitle(getStrings().get("Users"));
		usersWidget.setWidgetSubtitle(getStrings().get("Total"));
		usersWidget.setWidgetChartLabel(getStrings().get("Users"));
		usersWidget.setWidgetTotal(activeUserCountsByAccountSourceId.values().stream().collect(Collectors.summingLong(Long::longValue)));
		usersWidget.setWidgetTotalDescription(getFormatter().formatNumber(usersWidget.getWidgetTotal()));
		usersWidget.setWidgetData(new ArrayList<>(activeUserCountsByAccountSourceId.size()));

		Map<AccountSourceId, AccountSourceForInstitution> accountSourcesByAccountSourceId = new TreeMap<>(getInstitutionService().findAccountSourcesByInstitutionId(institutionId).stream()
				.collect(Collectors.toMap(accountSource -> accountSource.getAccountSourceId(), Function.identity())));

		int i = 0;

		for (Entry<AccountSourceId, Long> entry : activeUserCountsByAccountSourceId.entrySet()) {
			AccountSourceId accountSourceId = entry.getKey();
			Long count = entry.getValue();

			AnalyticsWidgetChartData widgetChartData = new AnalyticsWidgetChartData();
			widgetChartData.setLabel(accountSourcesByAccountSourceId.get(accountSourceId).getDescription());
			widgetChartData.setCount(count);
			widgetChartData.setCountDescription(getFormatter().formatNumber(count));
			widgetChartData.setColor(colorCssRepresentations.get(i % colorCssRepresentations.size()));

			usersWidget.getWidgetData().add(widgetChartData);

			++i;
		}

		AnalyticsPieChartWidget employersWidget = new AnalyticsPieChartWidget();
		employersWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_USERS);
		employersWidget.setWidgetTitle(getStrings().get("Employer"));
		employersWidget.setWidgetSubtitle(getStrings().get("Across {{employerCount}} Employer[s]", Map.of("employerCount", activeUserCountsByInstitutionLocation.size())));
		employersWidget.setWidgetChartLabel(getStrings().get("Users"));
		employersWidget.setWidgetTotal(activeUserCountsByInstitutionLocation.values().stream().collect(Collectors.summingLong(Long::longValue)));
		employersWidget.setWidgetTotalDescription(getFormatter().formatNumber(employersWidget.getWidgetTotal()));
		employersWidget.setWidgetData(new ArrayList<>(activeUserCountsByAccountSourceId.size()));

		i = 0;

		for (Entry<String, Long> entry : activeUserCountsByInstitutionLocation.entrySet()) {
			String institutionLocationName = entry.getKey();
			Long count = entry.getValue();

			AnalyticsWidgetChartData widgetChartData = new AnalyticsWidgetChartData();
			widgetChartData.setLabel(institutionLocationName);
			widgetChartData.setCount(count);
			widgetChartData.setCountDescription(getFormatter().formatNumber(count));
			widgetChartData.setColor(colorCssRepresentations.get(i % colorCssRepresentations.size()));

			employersWidget.getWidgetData().add(widgetChartData);

			++i;
		}

		// Group 2
		AnalyticsTableWidget pageviewsWidget = new AnalyticsTableWidget();
		pageviewsWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_PAGEVIEWS);
		pageviewsWidget.setWidgetTitle(getStrings().get("Pageviews"));

		AnalyticsWidgetTableData pageviewsWidgetData = new AnalyticsWidgetTableData();
		pageviewsWidgetData.setHeaders(List.of(
				getStrings().get("Section"),
				getStrings().get("Views"),
				getStrings().get("Users"),
				getStrings().get("Active Users")
		));
		pageviewsWidgetData.setRows(new ArrayList<>(sectionCountSummaries.size()));

		for (SectionCountSummary sectionCountSummary : sectionCountSummaries) {
			AnalyticsWidgetTableRow tableRow = new AnalyticsWidgetTableRow();

			tableRow.setData(List.of(
					sectionCountSummary.getSection(),
					getFormatter().formatNumber(sectionCountSummary.getPageViewCount()),
					getFormatter().formatNumber(sectionCountSummary.getUserCount()),
					getFormatter().formatNumber(sectionCountSummary.getActiveUserCount())
			));

			pageviewsWidgetData.getRows().add(tableRow);
		}

		pageviewsWidget.setWidgetData(pageviewsWidgetData);

		// Group 3
		AnalyticsBarChartWidget referralsWidget = new AnalyticsBarChartWidget();
		referralsWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_USER_REFERRALS);
		referralsWidget.setWidgetTitle(getStrings().get("Users from Referrals"));
		referralsWidget.setWidgetSubtitle(getStrings().get("{{percentage}} of Total", Map.of("percentage", getFormatter().formatPercent(trafficSourceSummary.getUsersFromNonDirectTrafficSourceMediumPercentage()))));
		referralsWidget.setWidgetChartLabel(getStrings().get("Users"));
		referralsWidget.setWidgetTotal(trafficSourceSummary.getUsersFromTrafficSourceMediumTotalCount());
		referralsWidget.setWidgetTotalDescription(getFormatter().formatNumber(referralsWidget.getWidgetTotal()));
		referralsWidget.setWidgetData(new ArrayList<>(trafficSourceSummary.getTrafficSourceMediumCounts().size()));

		i = 0;

		for (TrafficSourceMediumCount trafficSourceMediumCount : trafficSourceSummary.getTrafficSourceMediumCounts()) {
			AnalyticsWidgetChartData widgetChartData = new AnalyticsWidgetChartData();
			widgetChartData.setLabel(trafficSourceMediumCount.getMedium());
			widgetChartData.setCount(trafficSourceMediumCount.getUserCount());
			widgetChartData.setCountDescription(getFormatter().formatNumber(widgetChartData.getCount()));
			widgetChartData.setColor(colorCssRepresentations.get(i % colorCssRepresentations.size()));

			referralsWidget.getWidgetData().add(widgetChartData);

			++i;
		}

		AnalyticsTableWidget referringDomainsWidget = new AnalyticsTableWidget();
		referringDomainsWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_REFERRING_DOMAINS);
		referringDomainsWidget.setWidgetTitle(getStrings().get("Referring Domains"));

		AnalyticsWidgetTableData referringDomainsTableData = new AnalyticsWidgetTableData();
		referringDomainsTableData.setHeaders(
				List.of(
						getStrings().get("Domain"),
						getStrings().get("Users")
				)
		);
		referringDomainsTableData.setRows(new ArrayList<>(trafficSourceSummary.getTrafficSourceReferrerCounts().size()));

		int trafficSourceReferrerIndex = 0;

		for (TrafficSourceReferrerCount trafficSourceReferrerCount : trafficSourceSummary.getTrafficSourceReferrerCounts()) {
			++trafficSourceReferrerIndex;

			AnalyticsWidgetTableRow widgetTableRow = new AnalyticsWidgetTableRow();
			widgetTableRow.setData(List.of(
					getStrings().get("{{index}}. {{referrer}}", Map.of(
							"index", getFormatter().formatNumber(trafficSourceReferrerIndex),
							"referrer", trafficSourceReferrerCount.getReferrer()
					)),
					getFormatter().formatNumber(trafficSourceReferrerCount.getUserCount())
			));
			referringDomainsTableData.getRows().add(widgetTableRow);
		}

		referringDomainsWidget.setWidgetData(referringDomainsTableData);

		// Group the widgets
		AnalyticsWidgetGroup visitsUsersEmployersGroup = new AnalyticsWidgetGroup();
		visitsUsersEmployersGroup.setWidgets(List.of(visitsWidget, usersWidget, employersWidget));

		AnalyticsWidgetGroup pageviewsGroup = new AnalyticsWidgetGroup();
		pageviewsGroup.setWidgets(List.of(pageviewsWidget));

		AnalyticsWidgetGroup referralsGroup = new AnalyticsWidgetGroup();
		referralsGroup.setWidgets(List.of(referralsWidget, referringDomainsWidget));

		// Return the groups
		List<AnalyticsWidgetGroup> analyticsWidgetGroups = List.of(
				visitsUsersEmployersGroup,
				pageviewsGroup,
				referralsGroup
		);

		boolean returnExampleJson = false;

		if (!returnExampleJson)
			return new ApiResponse(Map.of(
					"analyticsWidgetGroups", analyticsWidgetGroups
			));

		String exampleJson = """
				{
				  "analyticsWidgetGroups": [
				    {
				      "widgets": [
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_VISTS",
				          "widgetTitle": "Visits",
				          "widgetTotal": 5900510,
				          "widgetTotalDescription": "5,900,510.00",
				          "widgetSubtitle": "Total",
				          "widgetTypeId": "PIE_CHART",
				          "widgetChartLabel": "Visits",
				          "widgetData": [
				            {
				              "label": "New",
				              "count": 10,
				              "countDescription": "10",
				              "color": "#30578E"
				            },
				            {
				              "label": "Returning",
				              "count": 10,
				              "countDescription": "10",
				              "color": "#C3D0EB"
				            }
				          ]
				        },
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_USERS",
				          "widgetTitle": "Users",
				          "widgetTotal": 5900510,
				          "widgetTotalDescription": "5,900,510.00",
				          "widgetSubtitle": "Total",
				          "widgetTypeId": "PIE_CHART",
				          "widgetChartLabel": "Users",
				          "widgetData": [
				            {
				              "label": "Logged In",
				              "count": 10,
				              "countDescription": "10",
				              "color": "#30578E"
				            },
				            {
				              "label": "Anonymous",
				              "count": 10,
				              "countDescription": "10",
				              "color": "#C3D0EB"
				            }
				          ]
				        },
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_EMPLOYERS",
				          "widgetTitle": "Employer",
				          "widgetTotal": 5900510,
				          "widgetTotalDescription": "5,900,510.00",
				          "widgetSubtitle": "Across 4 Employers",
				          "widgetTypeId": "PIE_CHART",
				          "widgetChartLabel": "Users",
				          "widgetData": [
				            {
				              "label": "Employer 1",
				              "count": 10,
				              "countDescription": "10",
				              "color": "#30578E"
				            },
				            {
				              "label": "Employer 2",
				              "count": 10,
				              "countDescription": "10",
				              "color": "#C3D0EB"
				            },
				            {
				              "label": "Employer 3",
				              "count": 10,
				              "countDescription": "10",
				              "color": "#7A97CE"
				            },
				            {
				              "label": "Employer 4",
				              "count": 10,
				              "countDescription": "10",
				              "color": "#20406C"
				            }
				          ]
				        }
				      ]
				    },
				    {
				      "widgets": [
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_PAGEVIEWS",
				          "widgetTitle": "Pageviews",
				          "widgetTypeId": "TABLE",
				          "widgetData": {
				            "headers": [
				              "Section",
				              "Views",
				              "Users",
				              "Active Users"
				            ],
				            "rows": [
				              {
				                "data": [
				                  "Sign In",
				                  "1,000,000",
				                  "500,000",
				                  "250,000"
				                ]
				              },
				              {
				                "data": [
				                  "Home Page",
				                  "1,000,000",
				                  "500,000",
				                  "250,000"
				                ]
				              },
				              {
				                "data": [
				                  "Therapy",
				                  "1,000,000",
				                  "500,000",
				                  "250,000"
				                ]
				              },
				              {
				                "data": [
				                  "CFA",
				                  "1,000,000",
				                  "500,000",
				                  "250,000"
				                ]
				              },
				              {
				                "data": [
				                  "Resource Library",
				                  "1,000,000",
				                  "500,000",
				                  "250,000"
				                ]
				              },
				              {
				                "data": [
				                  "Group Sessions",
				                  "1,000,000",
				                  "500,000",
				                  "250,000"
				                ]
				              }
				            ]
				          }
				        }
				      ]
				    },
				    {
				      "widgets": [
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_USER_REFERRALS",
				          "widgetTitle": "Users from Referrals",
				          "widgetTotal": 5900510,
				          "widgetTotalDescription": "5,900,510.00",
				          "widgetSubtitle": "100% of Total",
				          "widgetTypeId": "BAR_CHART",
				          "widgetChartLabel": "Users",
				          "widgetData": [
				            {
				              "label": "Direct",
				              "count": 10,
				              "countDescription": "10",
				              "color": "#E56F65"
				            },
				            {
				              "label": "Referral",
				              "count": 10,
				              "countDescription": "10",
				              "color": "#F2AD74"
				            },
				            {
				              "label": "Organic Search",
				              "count": 10,
				              "countDescription": "10",
				              "color": "#81B2B1"
				            },
				            {
				              "label": "Organic Social",
				              "count": 10,
				              "countDescription": "10",
				              "color": "#F2C87E"
				            },
				            {
				              "label": "Unassigned",
				              "count": 10,
				              "countDescription": "10",
				              "color": "#7A97CE"
				            }
				          ]
				        },
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_REFERRING_DOMAINS",
				          "widgetTitle": "Referring Domains",
				          "widgetTypeId": "TABLE",
				          "widgetData": {
				            "headers": [
				              "Domain",
				              "Users"
				            ],
				            "rows": [
				              {
				                "data": [
				                  "1. Domain Name",
				                  "1,000,000"
				                ]
				              }
				            ]
				          }
				        }
				      ]
				    }
				  ]
				}
				""";

		return writeMockJsonResponse(httpServletResponse, exampleJson);
	}

	@Nonnull
	@GET("/analytics/assessments-appointments")
	@AuthenticationRequired
	public Object analyticsAssessmentsAppointments(@Nonnull HttpServletResponse httpServletResponse,
																								 @Nonnull @QueryParameter LocalDate startDate,
																								 @Nonnull @QueryParameter LocalDate endDate) {
		requireNonNull(startDate);
		requireNonNull(endDate);

		InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canViewAnalytics(institutionId, account))
			throw new AuthorizationException();

		List<String> successColorCssRepresentations = getInstitutionService().findInstitutionColorValuesByInstitutionId(institutionId, ColorId.SEMANTIC_SUCCESS).stream()
				.map(institutionColorValue -> institutionColorValue.getCssRepresentation())
				.collect(Collectors.toList());

		List<String> warningColorCssRepresentations = getInstitutionService().findInstitutionColorValuesByInstitutionId(institutionId, ColorId.SEMANTIC_WARNING).stream()
				.map(institutionColorValue -> institutionColorValue.getCssRepresentation())
				.collect(Collectors.toList());

		List<String> dangerColorCssRepresentations = getInstitutionService().findInstitutionColorValuesByInstitutionId(institutionId, ColorId.SEMANTIC_DANGER).stream()
				.map(institutionColorValue -> institutionColorValue.getCssRepresentation())
				.collect(Collectors.toList());

		Map<UUID, ScreeningSessionCompletion> screeningSessionCompletions = getAnalyticsService().findClinicalScreeningSessionCompletionsByScreeningFlowId(institutionId, startDate, endDate);
		Map<UUID, SortedMap<String, Long>> screeningSessionSeverityCounts = getAnalyticsService().findClinicalScreeningSessionSeverityCountsByDescriptionByScreeningFlowId(institutionId, startDate, endDate);

		Set<UUID> screeningFlowIds = new HashSet<>(screeningSessionCompletions.size() + screeningSessionSeverityCounts.size());
		screeningFlowIds.addAll(screeningSessionCompletions.keySet());
		screeningFlowIds.addAll(screeningSessionSeverityCounts.keySet());

		Map<UUID, ScreeningFlow> screeningFlowsByScreeningFlowId = screeningFlowIds.stream()
				.map(screeningFlowId -> getScreeningService().findScreeningFlowById(screeningFlowId).get())
				.collect(Collectors.toMap(screeningFlow -> screeningFlow.getScreeningFlowId(), Function.identity()));

		List<CrisisTriggerCount> crisisTriggerCounts = getAnalyticsService().findCrisisTriggerCounts(institutionId, startDate, endDate);
		List<AppointmentCount> appointmentCounts = getAnalyticsService().findAppointmentCounts(institutionId, startDate, endDate);
		List<AppointmentClickToCallCount> appointmentClickToCallCounts = getAnalyticsService().findAppointmentClickToCallCounts(institutionId, startDate, endDate);

		boolean useExampleData = !getConfiguration().isProduction();

		if (useExampleData) {
			// Fake out clinical screening flow[s]
			screeningFlowIds = Set.of(UUID.randomUUID());
			int i = 0;

			for (UUID screeningFlowId : screeningFlowIds) {
				ScreeningSessionCompletion screeningSessionCompletion = new ScreeningSessionCompletion();
				screeningSessionCompletion.setStartedCount(75L);
				screeningSessionCompletion.setCompletedCount(25L);
				screeningSessionCompletion.setCompletionPercentage((double) screeningSessionCompletion.getCompletedCount() / (double) (screeningSessionCompletion.getStartedCount() + screeningSessionCompletion.getCompletedCount()));

				screeningSessionCompletions.put(screeningFlowId, screeningSessionCompletion);

				screeningSessionSeverityCounts.put(screeningFlowId, new TreeMap<>(Map.of(
						getStrings().get("Mild"), 50L,
						getStrings().get("Moderate"), 25L,
						getStrings().get("Severe"), 10L
				)));

				ScreeningFlow screeningFlow = new ScreeningFlow();
				screeningFlow.setScreeningFlowId(screeningFlowId);
				screeningFlow.setName(getStrings().get("Fake Screening Flow {{index}}", Map.of("index", ++i)));

				screeningFlowsByScreeningFlowId.put(screeningFlowId, screeningFlow);
			}

			// Other data
			crisisTriggerCounts = List.of(
					new CrisisTriggerCount() {{
						setCount(100L);
						setName(getStrings().get("Home Selection"));
					}},
					new CrisisTriggerCount() {{
						setCount(50L);
						setName(getStrings().get("Assessment"));
					}},
					new CrisisTriggerCount() {{
						setCount(125L);
						setName(getStrings().get("In Crisis Button"));
					}}
			);

			appointmentCounts = List.of(
					new AppointmentCount() {{
						setProviderId(UUID.randomUUID());
						setName(getStrings().get("Test Provider"));
						setAvailableAppointmentCount(150L);
						setBookedAppointmentCount(25L);
						setCanceledAppointmentCount(45L);
						setBookingPercentage((double) getBookedAppointmentCount() / (double) getAvailableAppointmentCount());
					}}
			);

			appointmentClickToCallCounts = List.of(
					new AppointmentClickToCallCount() {{
						setName(getStrings().get("Test Provider"));
						setCount(10L);
					}}
			);
		}

		List<AnalyticsWidget> clinicalAssessmentWidgets = new ArrayList<>();

		for (UUID screeningFlowId : screeningFlowIds) {
			ScreeningFlow screeningFlow = screeningFlowsByScreeningFlowId.get(screeningFlowId);
			ScreeningSessionCompletion screeningSessionCompletion = screeningSessionCompletions.get(screeningFlowId);
			SortedMap<String, Long> severityCountsByDescription = screeningSessionSeverityCounts.get(screeningFlowId);

			AnalyticsBarChartWidget clinicalAssessmentCompletionWidget = new AnalyticsBarChartWidget();
			clinicalAssessmentCompletionWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_CLINICAL_ASSESSMENT_COMPLETION);
			clinicalAssessmentCompletionWidget.setWidgetTitle(getStrings().get("{{screeningFlowName}} Completion", Map.of("screeningFlowName", screeningFlow.getName())));
			clinicalAssessmentCompletionWidget.setWidgetTotal(screeningSessionCompletion.getCompletionPercentage());
			clinicalAssessmentCompletionWidget.setWidgetTotalDescription(getFormatter().formatPercent(screeningSessionCompletion.getCompletionPercentage()));
			clinicalAssessmentCompletionWidget.setWidgetSubtitle(getStrings().get("Completion Rate"));
			clinicalAssessmentCompletionWidget.setWidgetChartLabel(getStrings().get("Assessments"));

			AnalyticsWidgetChartData startedWidgetChartData = new AnalyticsWidgetChartData();
			startedWidgetChartData.setCount(screeningSessionCompletion.getStartedCount());
			startedWidgetChartData.setCountDescription(getFormatter().formatNumber(screeningSessionCompletion.getStartedCount()));
			startedWidgetChartData.setLabel(getStrings().get("Started"));
			startedWidgetChartData.setColor(successColorCssRepresentations.get(0 % successColorCssRepresentations.size()));

			AnalyticsWidgetChartData completedWidgetChartData = new AnalyticsWidgetChartData();
			completedWidgetChartData.setCount(screeningSessionCompletion.getCompletedCount());
			completedWidgetChartData.setCountDescription(getFormatter().formatNumber(screeningSessionCompletion.getCompletedCount()));
			completedWidgetChartData.setLabel(getStrings().get("Completed"));
			completedWidgetChartData.setColor(successColorCssRepresentations.get(1 % successColorCssRepresentations.size()));

			clinicalAssessmentCompletionWidget.setWidgetData(List.of(
					startedWidgetChartData,
					completedWidgetChartData
			));

			clinicalAssessmentWidgets.add(clinicalAssessmentCompletionWidget);

			Long severityTotalCount = severityCountsByDescription.values().stream()
					.collect(Collectors.summingLong(Long::longValue));

			AnalyticsBarChartWidget clinicalAssessmentSeverityWidget = new AnalyticsBarChartWidget();
			clinicalAssessmentSeverityWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_CLINICAL_ASSESSMENT_SEVERITY);
			clinicalAssessmentSeverityWidget.setWidgetTitle(getStrings().get("{{screeningFlowName}} Severity", Map.of("screeningFlowName", screeningFlow.getName())));
			clinicalAssessmentSeverityWidget.setWidgetTotal(severityTotalCount);
			clinicalAssessmentSeverityWidget.setWidgetTotalDescription(getFormatter().formatNumber(severityTotalCount));
			clinicalAssessmentSeverityWidget.setWidgetSubtitle(getStrings().get("Completed Assessments"));
			clinicalAssessmentSeverityWidget.setWidgetChartLabel(getStrings().get("Assessments"));

			List<AnalyticsWidgetChartData> severityWidgetData = new ArrayList<>(severityCountsByDescription.size());
			int i = 0;

			for (Entry<String, Long> entry : severityCountsByDescription.entrySet()) {
				String description = entry.getKey();
				Long count = entry.getValue();

				AnalyticsWidgetChartData severityData = new AnalyticsWidgetChartData();
				severityData.setLabel(description);
				severityData.setCount(count);
				severityData.setCountDescription(getFormatter().formatNumber(count));
				severityData.setColor(warningColorCssRepresentations.get(i % warningColorCssRepresentations.size()));

				severityWidgetData.add(severityData);

				++i;
			}

			clinicalAssessmentSeverityWidget.setWidgetData(severityWidgetData);

			clinicalAssessmentWidgets.add(clinicalAssessmentSeverityWidget);
		}

		Long crisisTriggerTotalCount = crisisTriggerCounts.stream()
				.map(crisisTriggerCount -> crisisTriggerCount.getCount())
				.collect(Collectors.summingLong(Long::longValue));

		AnalyticsBarChartWidget crisisWidget = new AnalyticsBarChartWidget();
		crisisWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_CRISIS_TRIGGERS);
		crisisWidget.setWidgetTitle(getStrings().get("Crisis Triggers"));
		crisisWidget.setWidgetTotal(crisisTriggerTotalCount);
		crisisWidget.setWidgetTotalDescription(getFormatter().formatNumber(crisisTriggerTotalCount));
		crisisWidget.setWidgetSubtitle(getStrings().get("Total"));
		crisisWidget.setWidgetChartLabel(getStrings().get("Times Triggered"));

		List<AnalyticsWidgetChartData> crisisWidgetData = new ArrayList<>(crisisTriggerCounts.size());

		int i = 0;

		for (CrisisTriggerCount crisisTriggerCount : crisisTriggerCounts) {
			AnalyticsWidgetChartData crisisData = new AnalyticsWidgetChartData();
			crisisData.setLabel(crisisTriggerCount.getName());
			crisisData.setCount(crisisTriggerCount.getCount());
			crisisData.setCountDescription(getFormatter().formatNumber(crisisTriggerCount.getCount()));
			crisisData.setColor(dangerColorCssRepresentations.get(i % dangerColorCssRepresentations.size()));

			crisisWidgetData.add(crisisData);

			++i;
		}

		crisisWidget.setWidgetData(crisisWidgetData);

		// Provider Table
		AnalyticsTableWidget providerTableWidget = new AnalyticsTableWidget();
		providerTableWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_APPOINTMENTS_BOOKABLE);
		providerTableWidget.setWidgetTitle(getStrings().get("Appointments - Bookable Online"));

		AnalyticsWidgetTableData providerWidgetTableData = new AnalyticsWidgetTableData();

		providerWidgetTableData.setHeaders(List.of(
				getStrings().get("Provider Name"),
				getStrings().get("Available Appointments"),
				getStrings().get("Booked Appointments"),
				getStrings().get("Cancelled Appointments"),
				getStrings().get("% of Appts Booked & Kept")
		));

		List<AnalyticsWidgetTableRow> providerWidgetTableRows = new ArrayList<>(appointmentCounts.size());

		for (AppointmentCount appointmentCount : appointmentCounts) {
			AnalyticsWidgetTableRow row = new AnalyticsWidgetTableRow();
			row.setData(List.of(
					appointmentCount.getName(),
					getFormatter().formatNumber(appointmentCount.getAvailableAppointmentCount()),
					getFormatter().formatNumber(appointmentCount.getBookedAppointmentCount()),
					getFormatter().formatNumber(appointmentCount.getCanceledAppointmentCount()),
					getFormatter().formatPercent(appointmentCount.getBookingPercentage())
			));

			providerWidgetTableRows.add(row);
		}

		providerWidgetTableData.setRows(providerWidgetTableRows);

		providerTableWidget.setWidgetData(providerWidgetTableData);

		// Click to Call Table
		AnalyticsTableWidget clickToCallTableWidget = new AnalyticsTableWidget();
		clickToCallTableWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_APPOINTMENTS_CLICK_TO_CALL);
		clickToCallTableWidget.setWidgetTitle(getStrings().get("Appointments - Click to Call"));

		AnalyticsWidgetTableData clickToCallWidgetTableData = new AnalyticsWidgetTableData();

		clickToCallWidgetTableData.setHeaders(List.of(
				getStrings().get("Provider Name"),
				getStrings().get("# of Clicks to Call")
		));

		List<AnalyticsWidgetTableRow> clickToCallWidgetTableRows = new ArrayList<>(appointmentClickToCallCounts.size());

		for (AppointmentClickToCallCount appointmentClickToCallCount : appointmentClickToCallCounts) {
			AnalyticsWidgetTableRow row = new AnalyticsWidgetTableRow();
			row.setData(List.of(
					appointmentClickToCallCount.getName(),
					getFormatter().formatNumber(appointmentClickToCallCount.getCount())
			));

			clickToCallWidgetTableRows.add(row);
		}

		clickToCallWidgetTableData.setRows(clickToCallWidgetTableRows);

		clickToCallTableWidget.setWidgetData(clickToCallWidgetTableData);

		// Group the widgets
		List<AnalyticsWidget> assessmentAndCrisisWidgets = new ArrayList<>(clinicalAssessmentWidgets.size() + 1);
		assessmentAndCrisisWidgets.addAll(clinicalAssessmentWidgets);
		assessmentAndCrisisWidgets.add(crisisWidget);

		AnalyticsWidgetGroup firstGroup = new AnalyticsWidgetGroup();
		firstGroup.setWidgets(assessmentAndCrisisWidgets);

		AnalyticsWidgetGroup secondGroup = new AnalyticsWidgetGroup();
		secondGroup.setWidgets(List.of(providerTableWidget));

		AnalyticsWidgetGroup thirdGroup = new AnalyticsWidgetGroup();
		thirdGroup.setWidgets(List.of(clickToCallTableWidget));

		// Return the groups
		List<AnalyticsWidgetGroup> analyticsWidgetGroups = List.of(
				firstGroup,
				secondGroup,
				thirdGroup
		);

		boolean returnExampleJson = false;

		if (!returnExampleJson)
			return new ApiResponse(Map.of(
					"analyticsWidgetGroups", analyticsWidgetGroups
			));

		String exampleJson = """
				{
				  "analyticsWidgetGroups": [
				    {
				      "widgets": [
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_CLINICAL_ASSESSMENT_COMPLETION",
				          "widgetTitle": "Clinical Assessment Completion",
				          "widgetTotal": 0.25,
				          "widgetTotalDescription": "25%",
				          "widgetSubtitle": "Completion Rate",
				          "widgetTypeId": "BAR_CHART",
				          "widgetChartLabel": "Assessments",
				          "widgetData": [
				            {
				              "label": "Started",
				              "count": 120,
				              "countDescription": "120",
				              "color": "#EE934E"
				            },
				            {
				              "label": "Phone #s collected",
				              "count": 120,
				              "countDescription": "120",
				              "color": "#EE934E"
				            },
				            {
				              "label": "Completed",
				              "count": 120,
				              "countDescription": "120",
				              "color": "#EE934E"
				            }
				          ]
				        },
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_CLINICAL_ASSESSMENT_SEVERITY",
				          "widgetTitle": "Clinical Assessment Severity",
				          "widgetTotal": 1150,
				          "widgetTotalDescription": "1,150",
				          "widgetSubtitle": "Completed Assessments",
				          "widgetTypeId": "BAR_CHART",
				          "widgetChartLabel": "Assessments",
				          "widgetData": [
				            {
				              "label": "Mild",
				              "count": 120,
				              "countDescription": "120",
				              "color": "#81B2B1"
				            },
				            {
				              "label": "Moderate",
				              "count": 120,
				              "countDescription": "120",
				              "color": "#F0B756"
				            },
				            {
				              "label": "Severe",
				              "count": 120,
				              "countDescription": "120",
				              "color": "#E56F65"
				            }
				          ]
				        },
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_CRISIS_TRIGGERS",
				          "widgetTitle": "Crisis Triggers",
				          "widgetTotal": 1150,
				          "widgetTotalDescription": "1,150",
				          "widgetSubtitle": "Total",
				          "widgetTypeId": "BAR_CHART",
				          "widgetChartLabel": "Times Triggered",
				          "widgetData": [
				            {
				              "label": "Home Selection",
				              "count": 120,
				              "countDescription": "120",
				              "color": "#E56F65"
				            },
				            {
				              "label": "PHQ-9 Flags",
				              "count": 120,
				              "countDescription": "120",
				              "color": "#E56F65"
				            },
				            {
				              "label": "In Crisis Button",
				              "count": 120,
				              "countDescription": "120",
				              "color": "#E56F65"
				            }
				          ]
				        }
				      ]
				    },
				    {
				      "widgets": [
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_APPOINTMENTS_BOOKABLE",
				          "widgetTitle": "Appointments - Bookable Online",
				          "widgetTypeId": "TABLE",
				          "widgetData": {
				            "headers": [
				              "Provider Type",
				              "Available Appointments",
				              "Booked Appointments",
				              "Cancelled Appointments",
				              "% of Appts Booked & Kept"
				            ],
				            "rows": [
				              {
				                "data": [
				                  "Provider Type Name",
				                  "1,000",
				                  "1,000",
				                  "1,000",
				                  "100%"
				                ]
				              }
				            ]
				          }
				        }
				      ]
				    },
				    {
				      "widgets": [
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_APPOINTMENTS_CLICK_TO_CALL",
				          "widgetTitle": "Appointments - Click to Call",
				          "widgetTypeId": "TABLE",
				          "widgetData": {
				            "headers": [
				              "Provider Type",
				              "# of Clicks to Calls"
				            ],
				            "rows": [
				              {
				                "data": [
				                  "Provider Type Name",
				                  "1,000"
				                ]
				              }
				            ]
				          }
				        }
				      ]
				    }
				  ]
				}
				""";

		return writeMockJsonResponse(httpServletResponse, exampleJson);
	}

	@Nonnull
	@GET("/analytics/group-sessions")
	@AuthenticationRequired
	public Object analyticsGroupSessions(@Nonnull HttpServletResponse httpServletResponse,
																			 @Nonnull @QueryParameter LocalDate startDate,
																			 @Nonnull @QueryParameter LocalDate endDate) {
		requireNonNull(startDate);
		requireNonNull(endDate);

		InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canViewAnalytics(institutionId, account))
			throw new AuthorizationException();

		GroupSessionSummary groupSessionSummary = getAnalyticsService().findGroupSessionSummary(institutionId, startDate, endDate);

		boolean useExampleData = !getConfiguration().isProduction();

		if (useExampleData) {
			groupSessionSummary = new GroupSessionSummary();
			groupSessionSummary.setRegistrationCount(1234L);
			groupSessionSummary.setRequestCount(100L);

			GroupSessionCount groupSessionCount1 = new GroupSessionCount();
			groupSessionCount1.setGroupSessionId(UUID.randomUUID());
			groupSessionCount1.setTitle(getStrings().get("Group Session 1"));
			groupSessionCount1.setStartDateTime(LocalDateTime.of(LocalDate.of(2025, 1, 1), LocalTime.of(13, 30)));
			groupSessionCount1.setRegistrationCount(100L);
			groupSessionCount1.setPageViewCount(10000L);

			GroupSessionCount groupSessionCount2 = new GroupSessionCount();
			groupSessionCount2.setGroupSessionId(UUID.randomUUID());
			groupSessionCount2.setTitle(getStrings().get("Group Session 2"));
			groupSessionCount2.setStartDateTime(LocalDateTime.of(LocalDate.of(2026, 12, 31), LocalTime.of(18, 30)));
			groupSessionCount2.setRegistrationCount(1000L);
			groupSessionCount2.setPageViewCount(1250000L);

			groupSessionSummary.setGroupSessionCounts(List.of(
					groupSessionCount1,
					groupSessionCount2
			));
		}

		AnalyticsCounterWidget groupSessionRegistrationWidget = new AnalyticsCounterWidget();
		groupSessionRegistrationWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_GROUP_SESSION_REGISTRATIONS);
		groupSessionRegistrationWidget.setWidgetTitle(getStrings().get("Registrations"));
		groupSessionRegistrationWidget.setWidgetTotal(groupSessionSummary.getRegistrationCount());
		groupSessionRegistrationWidget.setWidgetTotalDescription(getFormatter().formatNumber(groupSessionSummary.getRegistrationCount()));
		groupSessionRegistrationWidget.setWidgetSubtitle(getStrings().get("Total"));

		AnalyticsCounterWidget groupSessionRequestWidget = new AnalyticsCounterWidget();
		groupSessionRequestWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_GROUP_SESSION_REQUESTS);
		groupSessionRequestWidget.setWidgetTitle(getStrings().get("Requests"));
		groupSessionRequestWidget.setWidgetTotal(groupSessionSummary.getRequestCount());
		groupSessionRequestWidget.setWidgetTotalDescription(getFormatter().formatNumber(groupSessionSummary.getRequestCount()));
		groupSessionRequestWidget.setWidgetSubtitle(getStrings().get("Total"));

		// Group Sessions Table
		AnalyticsTableWidget groupSessionTableWidget = new AnalyticsTableWidget();
		groupSessionTableWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_GROUP_SESSIONS);
		groupSessionTableWidget.setWidgetTitle(getStrings().get("Group Sessions"));

		AnalyticsWidgetTableData groupSessionTableWidgetData = new AnalyticsWidgetTableData();

		groupSessionTableWidgetData.setHeaders(List.of(
				getStrings().get("Session Title"),
				getStrings().get("Date Scheduled"),
				getStrings().get("Pageviews"),
				getStrings().get("Registrations")
		));

		List<AnalyticsWidgetTableRow> groupSessionWidgetTableRows = new ArrayList<>(groupSessionSummary.getGroupSessionCounts().size());

		for (GroupSessionCount groupSessionCount : groupSessionSummary.getGroupSessionCounts()) {
			AnalyticsWidgetTableRow row = new AnalyticsWidgetTableRow();
			row.setData(List.of(
					groupSessionCount.getTitle(),
					groupSessionCount.getStartDateTime() == null ? "--" : getFormatter().formatDateTime(groupSessionCount.getStartDateTime(), FormatStyle.SHORT, FormatStyle.SHORT),
					getFormatter().formatNumber(groupSessionCount.getPageViewCount()),
					getFormatter().formatNumber(groupSessionCount.getRegistrationCount())
			));

			groupSessionWidgetTableRows.add(row);
		}

		groupSessionTableWidgetData.setRows(groupSessionWidgetTableRows);

		groupSessionTableWidget.setWidgetData(groupSessionTableWidgetData);

		// Group the widgets
		AnalyticsWidgetGroup firstGroup = new AnalyticsWidgetGroup();
		firstGroup.setWidgets(List.of(groupSessionRegistrationWidget, groupSessionRequestWidget));

		AnalyticsWidgetGroup secondGroup = new AnalyticsWidgetGroup();
		secondGroup.setWidgets(List.of(groupSessionTableWidget));

		// Return the groups
		List<AnalyticsWidgetGroup> analyticsWidgetGroups = List.of(
				firstGroup,
				secondGroup
		);

		boolean returnExampleJson = false;

		if (!returnExampleJson)
			return new ApiResponse(Map.of(
					"analyticsWidgetGroups", analyticsWidgetGroups
			));

		String exampleJson = """
				{
				  "analyticsWidgetGroups": [
				    {
				      "widgets": [
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_GROUP_SESSION_REGISTRATIONS",
				          "widgetTitle": "Registrations",
				          "widgetTotal": 100,
				          "widgetTotalDescription": "100",
				          "widgetSubtitle": "Total",
				          "widgetTypeId": "COUNTER"
				        },
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_GROUP_SESSION_REQUESTS",
				          "widgetTitle": "Requests",
				          "widgetTotal": 100,
				          "widgetTotalDescription": "100",
				          "widgetSubtitle": "Total",
				          "widgetTypeId": "COUNTER"
				        }
				      ]
				    },
				    {
				      "widgets": [
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_GROUP_SESSIONS",
				          "widgetTitle": "Group Sessions",
				          "widgetTypeId": "TABLE",
				          "widgetData": {
				            "headers": [
				              "Session Title",
				              "Date Scheduled",
				              "Views",
				              "Registrations"
				            ],
				            "rows": [
				              {
				                "data": [
				                  "Group Session Title",
				                  "00/00/0000",
				                  "1,000",
				                  "1,000"
				                ]
				              },
				              {
				                "data": [
				                  "Test Session",
				                  "1/1/2023",
				                  "2,000",
				                  "3,000"
				                ]
				              }
				            ]
				          }
				        }
				      ]
				    }
				  ]
				}
				""";

		return writeMockJsonResponse(httpServletResponse, exampleJson);
	}

	@Nonnull
	@GET("/analytics/resources-topics")
	@AuthenticationRequired
	public Object analyticsResourcesTopics(@Nonnull HttpServletResponse httpServletResponse,
																				 @Nonnull @QueryParameter LocalDate startDate,
																				 @Nonnull @QueryParameter LocalDate endDate) {
		requireNonNull(startDate);
		requireNonNull(endDate);

		InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canViewAnalytics(institutionId, account))
			throw new AuthorizationException();

		ResourceAndTopicSummary resourceAndTopicSummary = getAnalyticsService().findResourceAndTopicSummary(institutionId, startDate, endDate);

		boolean useExampleData = !getConfiguration().isProduction();

		if (useExampleData) {
			TagGroupPageView tagGroupPageView1 = new TagGroupPageView();
			tagGroupPageView1.setPageViewCount(123L);
			tagGroupPageView1.setTagGroupId("SYMPTOMS");
			tagGroupPageView1.setTagGroupName(getStrings().get("Symptoms"));
			tagGroupPageView1.setUrlPath("/ignored");

			TagGroupPageView tagGroupPageView2 = new TagGroupPageView();
			tagGroupPageView2.setPageViewCount(1234L);
			tagGroupPageView2.setTagGroupId("WORK_LIFE");
			tagGroupPageView2.setTagGroupName(getStrings().get("Work Life"));
			tagGroupPageView2.setUrlPath("/ignored");

			List<TagGroupPageView> tagGroupPageViews = List.of(tagGroupPageView1, tagGroupPageView2);

			TagPageView tagPageView1 = new TagPageView();
			tagPageView1.setPageViewCount(5000L);
			tagPageView1.setTagId("MOOD");
			tagPageView1.setTagName(getStrings().get("Mood"));
			tagPageView1.setTagGroupId("SYMPTOMS");
			tagPageView1.setUrlPath("ignored");

			TagPageView tagPageView2 = new TagPageView();
			tagPageView2.setPageViewCount(100000L);
			tagPageView2.setTagId("STRESS");
			tagPageView2.setTagName(getStrings().get("Stress"));
			tagPageView2.setTagGroupId("SYMPTOMS");
			tagPageView2.setUrlPath("ignored");

			TagPageView tagPageView3 = new TagPageView();
			tagPageView3.setPageViewCount(50L);
			tagPageView3.setTagId("BURNOUT");
			tagPageView3.setTagName(getStrings().get("Burnout"));
			tagPageView3.setTagGroupId("WORK_LIFE");
			tagPageView3.setUrlPath("ignored");

			List<TagPageView> tagPageViews = List.of(tagPageView1, tagPageView2, tagPageView3);

			ContentPageView contentPageView1 = new ContentPageView();
			contentPageView1.setContentId(UUID.randomUUID());
			contentPageView1.setContentTitle(getStrings().get("Content Title 1"));
			contentPageView1.setPageViewCount(12345L);

			List<ContentPageView> contentPageViews = List.of(contentPageView1);

			TopicCenterInteraction topicCenterInteraction1 = new TopicCenterInteraction();
			topicCenterInteraction1.setTopicCenterId(UUID.randomUUID());
			topicCenterInteraction1.setName(getStrings().get("Topic Center 1"));
			topicCenterInteraction1.setPageViewCount(1000L);
			topicCenterInteraction1.setUniqueVisitorCount(500L);
			topicCenterInteraction1.setGroupSessionClickCount(250L);
			topicCenterInteraction1.setGroupSessionByRequestClickCount(50L);
			topicCenterInteraction1.setPinboardItemClickCount(10L);
			topicCenterInteraction1.setContentClickCount(1250L);

			List<TopicCenterInteraction> topicCenterInteractions = List.of(topicCenterInteraction1);

			resourceAndTopicSummary = new ResourceAndTopicSummary();
			resourceAndTopicSummary.setTagGroupPageViews(tagGroupPageViews);
			resourceAndTopicSummary.setTagPageViews(tagPageViews);
			resourceAndTopicSummary.setContentPageViews(contentPageViews);
			resourceAndTopicSummary.setTopicCenterInteractions(topicCenterInteractions);
		}

		// Index the tag page views by tag group to make nested rows easier to work with
		Map<String, List<TagPageView>> tagPageViewsByTagGroupIds = new HashMap<>(resourceAndTopicSummary.getTagGroupPageViews().size());

		for (TagPageView tagPageView : resourceAndTopicSummary.getTagPageViews()) {
			List<TagPageView> tagPageViews = tagPageViewsByTagGroupIds.get(tagPageView.getTagGroupId());

			if (tagPageViews == null) {
				tagPageViews = new ArrayList<>();
				tagPageViewsByTagGroupIds.put(tagPageView.getTagGroupId(), tagPageViews);
			}

			tagPageViews.add(tagPageView);
		}

		AnalyticsTableWidget tagGroupTableWidget = new AnalyticsTableWidget();
		tagGroupTableWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_RESOURCE_TOPIC_PAGEVIEWS);
		tagGroupTableWidget.setWidgetTitle(getStrings().get("Pageview by Resource Topic"));

		AnalyticsWidgetTableData tagGroupTableData = new AnalyticsWidgetTableData();

		tagGroupTableData.setHeaders(List.of(
				getStrings().get("Topic"),
				getStrings().get("Pageviews")
		));

		List<AnalyticsWidgetTableRow> tagGroupTableRows = new ArrayList<>();

		for (TagGroupPageView tagGroupPageView : resourceAndTopicSummary.getTagGroupPageViews()) {
			AnalyticsWidgetTableRow row = new AnalyticsWidgetTableRow();
			row.setData(List.of(
					tagGroupPageView.getTagGroupName(),
					getFormatter().formatNumber(tagGroupPageView.getPageViewCount())
			));

			List<TagPageView> nestedTagPageViews = tagPageViewsByTagGroupIds.get(tagGroupPageView.getTagGroupId());

			if (nestedTagPageViews != null) {
				List<AnalyticsWidgetTableRow> nestedRows = new ArrayList<>();

				for (TagPageView tagPageView : nestedTagPageViews) {
					AnalyticsWidgetTableRow nestedRow = new AnalyticsWidgetTableRow();
					nestedRow.setData(List.of(
							tagPageView.getTagName(),
							getFormatter().formatNumber(tagPageView.getPageViewCount())
					));

					nestedRows.add(nestedRow);
				}

				row.setNestedRows(nestedRows);
			}

			tagGroupTableRows.add(row);
		}

		tagGroupTableData.setRows(tagGroupTableRows);
		tagGroupTableWidget.setWidgetData(tagGroupTableData);

		// Resource detail table widget
		AnalyticsTableWidget resourceDetailTableWidget = new AnalyticsTableWidget();
		resourceDetailTableWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_RESOURCE_PAGEVIEWS);
		resourceDetailTableWidget.setWidgetTitle(getStrings().get("Resource Detail Pageviews (Top 25)"));

		AnalyticsWidgetTableData resourceDetailWidgetTableData = new AnalyticsWidgetTableData();

		resourceDetailWidgetTableData.setHeaders(List.of(
				getStrings().get("Content Title"),
				getStrings().get("Views")
		));

		List<AnalyticsWidgetTableRow> resourceDetailWidgetTableRows = new ArrayList<>(resourceAndTopicSummary.getContentPageViews().size());

		for (ContentPageView contentPageView : resourceAndTopicSummary.getContentPageViews()) {
			AnalyticsWidgetTableRow row = new AnalyticsWidgetTableRow();
			row.setData(List.of(
					contentPageView.getContentTitle(),
					getFormatter().formatNumber(contentPageView.getPageViewCount())
			));

			resourceDetailWidgetTableRows.add(row);
		}

		resourceDetailWidgetTableData.setRows(resourceDetailWidgetTableRows);
		resourceDetailTableWidget.setWidgetData(resourceDetailWidgetTableData);

		// Topic center table widget
		AnalyticsTableWidget topicCenterTableWidget = new AnalyticsTableWidget();
		topicCenterTableWidget.setWidgetReportId(ReportTypeId.ADMIN_ANALYTICS_TOPIC_CENTER_OVERVIEW);
		topicCenterTableWidget.setWidgetTitle(getStrings().get("Topic Center Overview"));

		AnalyticsWidgetTableData topicCenterWidgetTableData = new AnalyticsWidgetTableData();

		topicCenterWidgetTableData.setHeaders(List.of(
				getStrings().get("Topic Center Title"),
				getStrings().get("Pageviews"),
				getStrings().get("Unique Visitors"),
				getStrings().get("Group Session Clicks"),
				getStrings().get("Group Session By Request Clicks"),
				getStrings().get("Pinboard Clicks"),
				getStrings().get("Content Clicks")
		));

		List<AnalyticsWidgetTableRow> topicCenterWidgetTableRows = new ArrayList<>(resourceAndTopicSummary.getTopicCenterInteractions().size());

		for (TopicCenterInteraction topicCenterInteraction : resourceAndTopicSummary.getTopicCenterInteractions()) {
			AnalyticsWidgetTableRow row = new AnalyticsWidgetTableRow();
			row.setData(List.of(
					topicCenterInteraction.getName(),
					getFormatter().formatNumber(topicCenterInteraction.getPageViewCount()),
					getFormatter().formatNumber(topicCenterInteraction.getUniqueVisitorCount()),
					getFormatter().formatNumber(topicCenterInteraction.getGroupSessionClickCount()),
					getFormatter().formatNumber(topicCenterInteraction.getGroupSessionByRequestClickCount()),
					getFormatter().formatNumber(topicCenterInteraction.getPinboardItemClickCount()),
					getFormatter().formatNumber(topicCenterInteraction.getContentClickCount())
			));

			topicCenterWidgetTableRows.add(row);
		}

		topicCenterWidgetTableData.setRows(topicCenterWidgetTableRows);
		topicCenterTableWidget.setWidgetData(topicCenterWidgetTableData);

		// Group the widgets
		AnalyticsWidgetGroup firstGroup = new AnalyticsWidgetGroup();
		firstGroup.setWidgets(List.of(tagGroupTableWidget));

		AnalyticsWidgetGroup secondGroup = new AnalyticsWidgetGroup();
		secondGroup.setWidgets(List.of(resourceDetailTableWidget));

		AnalyticsWidgetGroup thirdGroup = new AnalyticsWidgetGroup();
		thirdGroup.setWidgets(List.of(topicCenterTableWidget));

		// Return the groups
		List<AnalyticsWidgetGroup> analyticsWidgetGroups = List.of(
				firstGroup,
				secondGroup,
				thirdGroup
		);

		boolean returnExampleJson = false;

		if (!returnExampleJson)
			return new ApiResponse(Map.of(
					"analyticsWidgetGroups", analyticsWidgetGroups
			));

		String exampleJson = """
				{
				  "analyticsWidgetGroups": [
				    {
				      "widgets": [
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_PAGEVIEWS_RESOURCE_TOPIC",
				          "widgetTitle": "Pageview by Resource Topic",
				          "widgetTypeId": "TABLE",
				          "widgetData": {
				            "headers": [
				              "Topic",
				              "Pageviews"
				            ],
				            "rows": [
				              {
				                "data": [
				                  "Topic Name",
				                  "1,000"
				                ],
				                "nestedRows": [
				                  {
				                    "data": [
				                      "Subtopic Name",
				                      "1,000"
				                    ]
				                  }
				                ]
				              }
				            ]
				          }
				        }
				      ]
				    },
				    {
				      "widgets": [
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_RESOURCE_PAGEVIEWS",
				          "widgetTitle": "Resource Detail Pageviews (Top 25)",
				          "widgetTypeId": "TABLE",
				          "widgetData": {
				            "headers": [
				              "Content Title",
				              "Views"
				            ],
				            "rows": [
				              {
				                "data": [
				                  "Title Text",
				                  "1,000"
				                ]
				              }
				            ]
				          }
				        }
				      ]
				    },
				    {
				      "widgets": [
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_TOPIC_CENTER_OVERVIEW",
				          "widgetTitle": "Topic Center Overview",
				          "widgetTypeId": "TABLE",
				          "widgetData": {
				            "headers": [
				              "Topic Center Title",
				              "Pageviews",
				              "Unique Visitors",
				              "Group Session Registrations",
				              "Group Session Requests",
				              "Community Connection Clicks",
				              "Food for Thought Clicks"
				            ],
				            "rows": [
				              {
				                "data": [
				                  "Topic Center Title",
				                  "1000",
				                  "1000",
				                  "1000",
				                  "1000",
				                  "1000",
				                  "1000"
				                ]
				              }
				            ]
				          }
				        }
				      ]
				    }
				  ]
				}
				""";

		return writeMockJsonResponse(httpServletResponse, exampleJson);
	}

	@Nonnull
	protected CustomResponse writeMockJsonResponse(@Nonnull HttpServletResponse httpServletResponse,
																								 @Nonnull String json) {
		requireNonNull(httpServletResponse);
		requireNonNull(json);

		httpServletResponse.setContentType("application/json; charset=UTF-8");

		try {
			IOUtils.copy(new StringReader(json), httpServletResponse.getOutputStream(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return CustomResponse.instance();
	}

	public enum AnalyticsWidgetTypeId {
		COUNTER,
		BAR_CHART,
		PIE_CHART,
		TABLE
	}

	@NotThreadSafe
	public static abstract class AnalyticsWidget {
		@Nonnull
		private final AnalyticsWidgetTypeId widgetTypeId;
		@Nullable
		private ReportTypeId widgetReportId;
		@Nullable
		private String widgetTitle;
		@Nullable
		private String widgetSubtitle;

		public AnalyticsWidget(@Nonnull AnalyticsWidgetTypeId widgetTypeId) {
			this.widgetTypeId = requireNonNull(widgetTypeId);
		}

		@Nonnull
		public final AnalyticsWidgetTypeId getWidgetTypeId() {
			return this.widgetTypeId;
		}

		@Nullable
		public ReportTypeId getWidgetReportId() {
			return this.widgetReportId;
		}

		public void setWidgetReportId(@Nullable ReportTypeId widgetReportId) {
			this.widgetReportId = widgetReportId;
		}

		@Nullable
		public String getWidgetTitle() {
			return this.widgetTitle;
		}

		public void setWidgetTitle(@Nullable String widgetTitle) {
			this.widgetTitle = widgetTitle;
		}

		@Nullable
		public String getWidgetSubtitle() {
			return this.widgetSubtitle;
		}

		public void setWidgetSubtitle(@Nullable String widgetSubtitle) {
			this.widgetSubtitle = widgetSubtitle;
		}
	}

	@NotThreadSafe
	public static class AnalyticsCounterWidget extends AnalyticsWidget {
		@Nullable
		private Number widgetTotal;
		@Nullable
		private String widgetTotalDescription;

		public AnalyticsCounterWidget() {
			super(AnalyticsWidgetTypeId.COUNTER);
		}

		@Nullable
		public Number getWidgetTotal() {
			return this.widgetTotal;
		}

		public void setWidgetTotal(@Nullable Number widgetTotal) {
			this.widgetTotal = widgetTotal;
		}

		@Nullable
		public String getWidgetTotalDescription() {
			return this.widgetTotalDescription;
		}

		public void setWidgetTotalDescription(@Nullable String widgetTotalDescription) {
			this.widgetTotalDescription = widgetTotalDescription;
		}
	}

	@NotThreadSafe
	public static class AnalyticsWidgetChartData {
		@Nullable
		private String label;
		@Nullable
		private Number count;
		@Nullable
		private String countDescription;
		@Nullable
		private String color;

		@Nullable
		public String getLabel() {
			return this.label;
		}

		public void setLabel(@Nullable String label) {
			this.label = label;
		}

		@Nullable
		public Number getCount() {
			return this.count;
		}

		public void setCount(@Nullable Number count) {
			this.count = count;
		}

		@Nullable
		public String getCountDescription() {
			return this.countDescription;
		}

		public void setCountDescription(@Nullable String countDescription) {
			this.countDescription = countDescription;
		}

		@Nullable
		public String getColor() {
			return this.color;
		}

		public void setColor(@Nullable String color) {
			this.color = color;
		}
	}

	@NotThreadSafe
	public static class AnalyticsPieChartWidget extends AnalyticsWidget {
		@Nullable
		private Number widgetTotal;
		@Nullable
		private String widgetTotalDescription;
		@Nullable
		private String widgetChartLabel;
		@Nullable
		private List<AnalyticsWidgetChartData> widgetData;

		public AnalyticsPieChartWidget() {
			super(AnalyticsWidgetTypeId.PIE_CHART);
		}

		@Nullable
		public Number getWidgetTotal() {
			return this.widgetTotal;
		}

		public void setWidgetTotal(@Nullable Number widgetTotal) {
			this.widgetTotal = widgetTotal;
		}

		@Nullable
		public String getWidgetTotalDescription() {
			return this.widgetTotalDescription;
		}

		public void setWidgetTotalDescription(@Nullable String widgetTotalDescription) {
			this.widgetTotalDescription = widgetTotalDescription;
		}

		@Nullable
		public String getWidgetChartLabel() {
			return this.widgetChartLabel;
		}

		public void setWidgetChartLabel(@Nullable String widgetChartLabel) {
			this.widgetChartLabel = widgetChartLabel;
		}

		@Nullable
		public List<AnalyticsWidgetChartData> getWidgetData() {
			return this.widgetData;
		}

		public void setWidgetData(@Nullable List<AnalyticsWidgetChartData> widgetData) {
			this.widgetData = widgetData;
		}
	}

	@NotThreadSafe
	public static class AnalyticsBarChartWidget extends AnalyticsWidget {
		@Nullable
		private Number widgetTotal;
		@Nullable
		private String widgetTotalDescription;
		@Nullable
		private String widgetChartLabel;
		@Nullable
		private List<AnalyticsWidgetChartData> widgetData;

		public AnalyticsBarChartWidget() {
			super(AnalyticsWidgetTypeId.BAR_CHART);
		}

		@Nullable
		public Number getWidgetTotal() {
			return this.widgetTotal;
		}

		public void setWidgetTotal(@Nullable Number widgetTotal) {
			this.widgetTotal = widgetTotal;
		}

		@Nullable
		public String getWidgetTotalDescription() {
			return this.widgetTotalDescription;
		}

		public void setWidgetTotalDescription(@Nullable String widgetTotalDescription) {
			this.widgetTotalDescription = widgetTotalDescription;
		}

		@Nullable
		public String getWidgetChartLabel() {
			return this.widgetChartLabel;
		}

		public void setWidgetChartLabel(@Nullable String widgetChartLabel) {
			this.widgetChartLabel = widgetChartLabel;
		}

		@Nullable
		public List<AnalyticsWidgetChartData> getWidgetData() {
			return this.widgetData;
		}

		public void setWidgetData(@Nullable List<AnalyticsWidgetChartData> widgetData) {
			this.widgetData = widgetData;
		}
	}

	@Nonnull
	public static class AnalyticsWidgetTableRow {
		@Nullable
		private List<String> data;
		@Nullable
		private List<AnalyticsWidgetTableRow> nestedRows;

		@Nullable
		public List<String> getData() {
			return this.data;
		}

		public void setData(@Nullable List<String> data) {
			this.data = data;
		}

		@Nullable
		public List<AnalyticsWidgetTableRow> getNestedRows() {
			return this.nestedRows;
		}

		public void setNestedRows(@Nullable List<AnalyticsWidgetTableRow> nestedRows) {
			this.nestedRows = nestedRows;
		}
	}

	@NotThreadSafe
	public static class AnalyticsWidgetTableData {
		@Nullable
		private List<String> headers;
		@Nullable
		private List<AnalyticsWidgetTableRow> rows;

		@Nullable
		public List<String> getHeaders() {
			return this.headers;
		}

		public void setHeaders(@Nullable List<String> headers) {
			this.headers = headers;
		}

		@Nullable
		public List<AnalyticsWidgetTableRow> getRows() {
			return this.rows;
		}

		public void setRows(@Nullable List<AnalyticsWidgetTableRow> rows) {
			this.rows = rows;
		}
	}

	@NotThreadSafe
	public static class AnalyticsTableWidget extends AnalyticsWidget {
		@Nullable
		private AnalyticsWidgetTableData widgetData;

		public AnalyticsTableWidget() {
			super(AnalyticsWidgetTypeId.TABLE);
		}

		@Nullable
		public AnalyticsWidgetTableData getWidgetData() {
			return this.widgetData;
		}

		public void setWidgetData(@Nullable AnalyticsWidgetTableData widgetData) {
			this.widgetData = widgetData;
		}
	}

	@NotThreadSafe
	public static class AnalyticsWidgetGroup {
		@Nullable
		private List<AnalyticsWidget> widgets;

		@Nullable
		public List<AnalyticsWidget> getWidgets() {
			return this.widgets;
		}

		public void setWidgets(@Nullable List<AnalyticsWidget> widgets) {
			this.widgets = widgets;
		}
	}

	@Nonnull
	protected AnalyticsService getAnalyticsService() {
		return this.analyticsService;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return this.authorizationService;
	}

	@Nonnull
	protected ScreeningService getScreeningService() {
		return this.screeningService;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionService;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
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
