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
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.ScreeningFlow;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AnalyticsService;
import com.cobaltplatform.api.service.AnalyticsService.AnalyticsResultNewVersusReturning;
import com.cobaltplatform.api.service.AnalyticsService.CrisisTriggerCount;
import com.cobaltplatform.api.service.AnalyticsService.ScreeningSessionCompletion;
import com.cobaltplatform.api.service.AnalyticsService.SectionCountSummary;
import com.cobaltplatform.api.service.AnalyticsService.TrafficSourceSummary;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.ScreeningService;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
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
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public AnalyticsResource(@Nonnull AnalyticsService analyticsService,
													 @Nonnull AuthorizationService authorizationService,
													 @Nonnull ScreeningService screeningService,
													 @Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(analyticsService);
		requireNonNull(authorizationService);
		requireNonNull(screeningService);
		requireNonNull(currentContextProvider);

		this.analyticsService = analyticsService;
		this.authorizationService = authorizationService;
		this.screeningService = screeningService;
		this.currentContextProvider = currentContextProvider;
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

		Set<UUID> screeningFlowIds = new HashSet<>(screeningSessionCompletions.size() + screeningSessionSeverityCounts.size());
		screeningFlowIds.addAll(screeningSessionCompletions.keySet());
		screeningFlowIds.addAll(screeningSessionSeverityCounts.keySet());

		Map<UUID, ScreeningFlow> screeningFlowsByScreeningFlowId = screeningFlowIds.stream()
				.map(screeningFlowId -> getScreeningService().findScreeningFlowById(screeningFlowId).get())
				.collect(Collectors.toMap(screeningFlow -> screeningFlow.getScreeningFlowId(), Function.identity()));

		List<CrisisTriggerCount> crisisTriggerCounts = getAnalyticsService().findCrisisTriggerCounts(institutionId, startDate, endDate);

		// NOTE: this is a WIP

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
						"crisisTriggerCounts", crisisTriggerCounts
				),
				"groupSessions", Map.of(

				),
				"resourcesAndTopics", Map.of(

				)
		));

		return new ApiResponse(response);
	}

	@Nonnull
	@GET("/analytics/overview")
	@AuthenticationRequired
	public CustomResponse analyticsOverview(@Nonnull HttpServletResponse httpServletResponse,
																					@Nonnull @QueryParameter LocalDate startDate,
																					@Nonnull @QueryParameter LocalDate endDate) {
		requireNonNull(startDate);
		requireNonNull(endDate);

		InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canViewAnalytics(institutionId, account))
			throw new AuthorizationException();

		String exampleJson = """
				{
				  "analyticsWidgetGroups": [
				    {
				      "widgets": [
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_VISTS",
				          "widgetTitle": "Visits",
				          "widgetTotal": "5,900,510.00",
				          "widgetSubtitle": "Total",
				          "widgetTypeId": "pie-chart",
				          "widgetChartLabel": "Visits",
				          "widgetData": [
				            {
				              "label": "New",
				              "count": 10,
				              "color": "#30578E"
				            },
				            {
				              "label": "Returning",
				              "count": 10,
				              "color": "#C3D0EB"
				            }
				          ]
				        },
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_USERS",
				          "widgetTitle": "Users",
				          "widgetTotal": "5,900,510.00",
				          "widgetSubtitle": "Total",
				          "widgetTypeId": "pie-chart",
				          "widgetChartLabel": "Users",
				          "widgetData": [
				            {
				              "label": "Logged In",
				              "count": 10,
				              "color": "#30578E"
				            },
				            {
				              "label": "Anonymous",
				              "count": 10,
				              "color": "#C3D0EB"
				            }
				          ]
				        },
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_EMPLOYERS",
				          "widgetTitle": "Employer",
				          "widgetTotal": "5,900,510.00",
				          "widgetSubtitle": "Across 4 Employers",
				          "widgetTypeId": "pie-chart",
				          "widgetChartLabel": "Users",
				          "widgetData": [
				            {
				              "label": "Employer 1",
				              "count": 10,
				              "color": "#30578E"
				            },
				            {
				              "label": "Employer 2",
				              "count": 10,
				              "color": "#C3D0EB"
				            },
				            {
				              "label": "Employer 3",
				              "count": 10,
				              "color": "#7A97CE"
				            },
				            {
				              "label": "Employer 4",
				              "count": 10,
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
				          "widgetTypeId": "table",
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
				          "widgetTotal": "5,900,510.00",
				          "widgetSubtitle": "100% of Total",
				          "widgetTypeId": "bar-chart",
				          "widgetChartLabel": "New Users",
				          "widgetData": [
				            {
				              "label": "Direct",
				              "count": 10,
				              "color": "#E56F65"
				            },
				            {
				              "label": "Referral",
				              "count": 10,
				              "color": "#F2AD74"
				            },
				            {
				              "label": "Organic Search",
				              "count": 10,
				              "color": "#81B2B1"
				            },
				            {
				              "label": "Organic Social",
				              "count": 10,
				              "color": "#F2C87E"
				            },
				            {
				              "label": "Unassigned",
				              "count": 10,
				              "color": "#7A97CE"
				            }
				          ]
				        },
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_REFERRING_DOMAINS",
				          "widgetTitle": "Referring Domains",
				          "widgetTypeId": "table",
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
	public CustomResponse analyticsAssessmentsAppointments(@Nonnull HttpServletResponse httpServletResponse,
																												 @Nonnull @QueryParameter LocalDate startDate,
																												 @Nonnull @QueryParameter LocalDate endDate) {
		requireNonNull(startDate);
		requireNonNull(endDate);

		InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canViewAnalytics(institutionId, account))
			throw new AuthorizationException();

		String exampleJson = """
				{
				  "analyticsWidgetGroups": [
				    {
				      "widgets": [
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_CLINICAL_ASSESSMENT_COMPLETION",
				          "widgetTitle": "Clinical Assessment Completion",
				          "widgetTotal": "25%",
				          "widgetSubtitle": "Completion Rate",
				          "widgetTypeId": "bar-chart",
				          "widgetChartLabel": "Assessments",
				          "widgetData": [
				            {
				              "label": "Started",
				              "count": 120,
				              "color": "#EE934E"
				            },
				            {
				              "label": "Phone #s collected",
				              "count": 120,
				              "color": "#EE934E"
				            },
				            {
				              "label": "Completed",
				              "count": 120,
				              "color": "#EE934E"
				            }
				          ]
				        },
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_CLINICAL_ASSESSMENT_SEVERITY",
				          "widgetTitle": "Clinical Assessment Severity",
				          "widgetTotal": "1,150",
				          "widgetSubtitle": "Completed Assessments",
				          "widgetTypeId": "bar-chart",
				          "widgetChartLabel": "Assessments",
				          "widgetData": [
				            {
				              "label": "Mild",
				              "count": 120,
				              "color": "#81B2B1"
				            },
				            {
				              "label": "Moderate",
				              "count": 120,
				              "color": "#F0B756"
				            },
				            {
				              "label": "Severe",
				              "count": 120,
				              "color": "#E56F65"
				            }
				          ]
				        },
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_CRISIS_TRIGGERS",
				          "widgetTitle": "Crisis Triggers",
				          "widgetTotal": "1,150",
				          "widgetSubtitle": "Total",
				          "widgetTypeId": "bar-chart",
				          "widgetChartLabel": "Times Triggered",
				          "widgetData": [
				            {
				              "label": "Home Selection",
				              "count": 120,
				              "color": "#E56F65"
				            },
				            {
				              "label": "PHQ-9 Flags",
				              "count": 120,
				              "color": "#E56F65"
				            },
				            {
				              "label": "In Crisis Button",
				              "count": 120,
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
				          "widgetTypeId": "table",
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
				          "widgetTypeId": "table",
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
	public CustomResponse analyticsGroupSessions(@Nonnull HttpServletResponse httpServletResponse,
																							 @Nonnull @QueryParameter LocalDate startDate,
																							 @Nonnull @QueryParameter LocalDate endDate) {
		requireNonNull(startDate);
		requireNonNull(endDate);

		InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canViewAnalytics(institutionId, account))
			throw new AuthorizationException();

		String exampleJson = """
				{
				  "analyticsWidgetGroups": [
				    {
				      "widgets": [
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_GROUP_SESSION_REGISTRATIONS",
				          "widgetTitle": "Registrations",
				          "widgetTotal": "100",
				          "widgetSubtitle": "Total",
				          "widgetTypeId": "counter"
				        },
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_GROUP_SESSION_REQUESTS",
				          "widgetTitle": "Requests",
				          "widgetTotal": "100",
				          "widgetSubtitle": "Total",
				          "widgetTypeId": "counter"
				        }
				      ]
				    },
				    {
				      "widgets": [
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_GROUP_SESSIONS",
				          "widgetTitle": "Group Sessions",
				          "widgetTypeId": "table",
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
	public CustomResponse analyticsResourcesTopics(@Nonnull HttpServletResponse httpServletResponse,
																								 @Nonnull @QueryParameter LocalDate startDate,
																								 @Nonnull @QueryParameter LocalDate endDate) {
		requireNonNull(startDate);
		requireNonNull(endDate);

		InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canViewAnalytics(institutionId, account))
			throw new AuthorizationException();

		String exampleJson = """
				{
				  "analyticsWidgetGroups": [
				    {
				      "widgets": [
				        {
				          "widgetReportId": "ADMIN_ANALYTICS_PAGEVIEWS_RESOURCE_TOPIC",
				          "widgetTitle": "Pageview by Resource Topic",
				          "widgetTypeId": "table",
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
				          "widgetTypeId": "table",
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
				          "widgetTypeId": "table",
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
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
