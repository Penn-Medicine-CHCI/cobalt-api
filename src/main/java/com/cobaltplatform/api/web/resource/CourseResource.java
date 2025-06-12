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
import com.cobaltplatform.api.model.api.request.CompleteCourseUnitRequest;
import com.cobaltplatform.api.model.api.request.CreateCourseSessionRequest;
import com.cobaltplatform.api.model.api.response.CourseApiResponse.CourseApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CourseApiResponse.CourseApiResponseType;
import com.cobaltplatform.api.model.api.response.CourseSessionApiResponse.CourseSessionApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Course;
import com.cobaltplatform.api.model.db.CourseSession;
import com.cobaltplatform.api.model.db.CourseSessionStatus.CourseSessionStatusId;
import com.cobaltplatform.api.model.db.CourseUnit;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionCourse.InstitutionCourseStatusId;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.CourseWithCourseSessionStatus;
import com.cobaltplatform.api.model.service.CourseWithInstitutionCourseStatus;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.CourseService;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.lokalized.Strings;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class CourseResource {
	@Nonnull
	private final CourseService courseService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final CourseApiResponseFactory courseApiResponseFactory;
	@Nonnull
	private final CourseSessionApiResponseFactory courseSessionApiResponseFactory;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public CourseResource(@Nonnull CourseService courseService,
												@Nonnull AuthorizationService authorizationService,
												@Nonnull RequestBodyParser requestBodyParser,
												@Nonnull Provider<CurrentContext> currentContextProvider,
												@Nonnull CourseApiResponseFactory courseApiResponseFactory,
												@Nonnull CourseSessionApiResponseFactory courseSessionApiResponseFactory,
												@Nonnull Strings strings) {
		requireNonNull(courseService);
		requireNonNull(authorizationService);
		requireNonNull(requestBodyParser);
		requireNonNull(currentContextProvider);
		requireNonNull(courseApiResponseFactory);
		requireNonNull(courseSessionApiResponseFactory);
		requireNonNull(strings);

		this.courseService = courseService;
		this.authorizationService = authorizationService;
		this.requestBodyParser = requestBodyParser;
		this.currentContextProvider = currentContextProvider;
		this.courseApiResponseFactory = courseApiResponseFactory;
		this.courseSessionApiResponseFactory = courseSessionApiResponseFactory;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/courses")
	@AuthenticationRequired
	public ApiResponse courses() {
		Account account = getCurrentContext().getAccount().get();
		List<CourseWithCourseSessionStatus> inProgressAndCompletedCourses = getCourseService().findInProgressAndCompletedCourseSessions(account.getAccountId(), account.getInstitutionId());
		List<CourseWithInstitutionCourseStatus> courseWithInstitutionCourseStatuses = getCourseService().findComingSoonAndAvailableCourses(account.getAccountId(), account.getInstitutionId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("inProgress", inProgressAndCompletedCourses.stream()
					.filter(course -> course.getCourseSessionStatusId() == CourseSessionStatusId.IN_PROGRESS)
					.map(course -> getCourseApiResponseFactory()
							.create(course, CourseApiResponseType.DETAIL))
					.collect(Collectors.toList()));
			put("completed", inProgressAndCompletedCourses.stream()
					.filter(course -> course.getCourseSessionStatusId() == CourseSessionStatusId.COMPLETED)
					.map(course -> getCourseApiResponseFactory()
							.create(course, CourseApiResponseType.DETAIL))
					.collect(Collectors.toList()));
			put("available", courseWithInstitutionCourseStatuses.stream()
					.filter(course -> course.getInstitutionCourseStatusId() == InstitutionCourseStatusId.AVAILABLE)
					.map(course -> getCourseApiResponseFactory()
							.create(course, CourseApiResponseType.LIST))
					.collect(Collectors.toList()));
			put("comingSoon", courseWithInstitutionCourseStatuses.stream()
					.filter(course -> course.getInstitutionCourseStatusId() == InstitutionCourseStatusId.COMING_SOON)
					.map(course -> getCourseApiResponseFactory()
							.create(course, CourseApiResponseType.LIST))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/courses/{courseIdentifier}")
	@AuthenticationRequired
	public ApiResponse course(@Nonnull @PathParameter String courseIdentifier /* UUID or urlName */) {
		requireNonNull(courseIdentifier);

		InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Course course = getCourseService().findCourseByIdentifier(courseIdentifier, institutionId).orElse(null);

		if (course == null)
			throw new NotFoundException();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("course", getCourseApiResponseFactory().create(course, CourseApiResponseType.DETAIL));
		}});
	}

	@Nonnull
	@POST("/course-sessions")
	@AuthenticationRequired
	public ApiResponse createCourseSession(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		CreateCourseSessionRequest request = getRequestBodyParser().parse(requestBody, CreateCourseSessionRequest.class);
		request.setAccountId(account.getAccountId());

		// Do a quick validation check here since we need to be able to perform an authorization check next
		if (request.getCourseId() == null)
			throw new ValidationException(new FieldError("courseId", getStrings().get("Course ID is required.")));

		if (!getAuthorizationService().canCreateCourseSession(request.getCourseId(), account))
			throw new AuthorizationException();

		UUID courseSessionId = getCourseService().createCourseSession(request);
		CourseSession courseSession = getCourseService().findCourseSessionById(courseSessionId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("courseSession", getCourseSessionApiResponseFactory().create(courseSession));
		}});
	}

	@Nonnull
	@POST("/course-units/{courseUnitId}/complete")
	@AuthenticationRequired
	public ApiResponse completeCourseUnit(@Nonnull @PathParameter UUID courseUnitId) {
		requireNonNull(courseUnitId);

		Account account = getCurrentContext().getAccount().get();

		CourseUnit courseUnit = getCourseService().findCourseUnitById(courseUnitId).orElse(null);

		if (courseUnit == null)
			throw new NotFoundException();

		Course course = getCourseService().findCourseByCourseUnitId(courseUnitId).get();
		CourseSession currentCourseSession = getCourseService().findCurrentCourseSession(account.getAccountId(), course.getCourseId()).orElse(null);

		// If there is no current session active, an attempt to complete a unit will create a new session first
		if (currentCourseSession == null) {
			if (!getAuthorizationService().canCreateCourseSession(course.getCourseId(), account))
				throw new AuthorizationException();

			CreateCourseSessionRequest createCourseSessionRequest = new CreateCourseSessionRequest();
			createCourseSessionRequest.setAccountId(account.getAccountId());
			createCourseSessionRequest.setCourseId(course.getCourseId());

			UUID currentCourseSessionId = getCourseService().createCourseSession(createCourseSessionRequest);
			currentCourseSession = getCourseService().findCourseSessionById(currentCourseSessionId).get();
		}

		// Make sure we're allowed to do this
		if (!getAuthorizationService().canModifyCourseSession(currentCourseSession, account))
			throw new AuthorizationException();

		CompleteCourseUnitRequest request = new CompleteCourseUnitRequest();
		request.setCourseUnitId(courseUnitId);
		request.setCourseSessionId(currentCourseSession.getCourseSessionId());
		request.setAccountId(account.getAccountId());

		getCourseService().completeCourseUnit(request);

		CourseSession courseSession = getCourseService().findCourseSessionById(currentCourseSession.getCourseSessionId()).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("courseSession", getCourseSessionApiResponseFactory().create(courseSession));
		}});
	}

	@Nonnull
	protected CourseService getCourseService() {
		return this.courseService;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return this.authorizationService;
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return this.requestBodyParser;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}

	@Nonnull
	protected CourseApiResponseFactory getCourseApiResponseFactory() {
		return this.courseApiResponseFactory;
	}

	@Nonnull
	protected CourseSessionApiResponseFactory getCourseSessionApiResponseFactory() {
		return this.courseSessionApiResponseFactory;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
