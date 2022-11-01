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
import com.cobaltplatform.api.model.api.request.CreateActivityTrackingRequest;
import com.cobaltplatform.api.model.api.response.ActivityTrackingApiResponse.ActivityTrackingApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.ActivityTracking;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.ActivityTrackingService;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
import com.soklet.web.response.ApiResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;


/**
 * @author Transmogrify LLC.
 */

@Resource
@Singleton
@ThreadSafe
public class ActivityTrackingResource {

	@Nonnull
	private final ActivityTrackingService activityTrackingService;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final ActivityTrackingApiResponseFactory activityTrackingApiResponseFactory;

	@Inject
	public ActivityTrackingResource(@Nonnull ActivityTrackingService activityTrackingService,
																	@Nonnull RequestBodyParser requestBodyParser,
																	@Nonnull Provider<CurrentContext> currentContextProvider,
																	@Nonnull ActivityTrackingApiResponseFactory activityTrackingApiResponseFactory) {
		this.activityTrackingService = activityTrackingService;
		this.requestBodyParser = requestBodyParser;
		this.currentContextProvider = currentContextProvider;
		this.activityTrackingApiResponseFactory = activityTrackingApiResponseFactory;
	}

	@POST("/activity-tracking")
	@AuthenticationRequired
	public ApiResponse createActivityTracking(@Nonnull @RequestBody String body) {
		requireNonNull(body);

		Account account = getCurrentContext().getAccount().get();
		CreateActivityTrackingRequest request = getRequestBodyParser().parse(body, CreateActivityTrackingRequest.class);
		request.setSessionTrackingId(getCurrentContext().getSessionTrackingId().orElse(null));

		UUID activityTrackingId = getActivityTrackingService().trackActivity(Optional.of(account), request);

		ActivityTracking activityTracking = getActivityTrackingService().findActivityTrackingById(activityTrackingId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("activityTracking", getActivityTrackingApiResponseFactory().create(activityTracking));
		}});
	}

	@POST("/unauthenticated-activity-tracking")
	public ApiResponse createUnauthenticatedActivityTracking(@Nonnull @RequestBody String body) {
		requireNonNull(body);

		CreateActivityTrackingRequest request = getRequestBodyParser().parse(body, CreateActivityTrackingRequest.class);
		request.setSessionTrackingId(getCurrentContext().getSessionTrackingId().orElse(null));

		UUID activityTrackingId = getActivityTrackingService().trackActivity(Optional.empty(), request);

		ActivityTracking activityTracking = getActivityTrackingService().findActivityTrackingById(activityTrackingId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("activityTracking", getActivityTrackingApiResponseFactory().create(activityTracking));
		}});
	}

	@Nonnull
	public ActivityTrackingService getActivityTrackingService() {
		return activityTrackingService;
	}

	@Nonnull
	public RequestBodyParser getRequestBodyParser() {
		return requestBodyParser;
	}

	@Nonnull
	public ActivityTrackingApiResponseFactory getActivityTrackingApiResponseFactory() {
		return activityTrackingApiResponseFactory;
	}

	@Nonnull
	public CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}
}
