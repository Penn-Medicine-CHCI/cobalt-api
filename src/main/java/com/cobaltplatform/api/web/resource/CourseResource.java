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
import com.cobaltplatform.api.model.api.response.CourseApiResponse.CourseApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CourseApiResponse.CourseApiResponseType;
import com.cobaltplatform.api.model.db.Course;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.CourseService;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
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
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final CourseApiResponseFactory courseApiResponseFactory;
	@Nonnull
	private final Logger logger;

	@Inject
	public CourseResource(@Nonnull CourseService courseService,
												@Nonnull Provider<CurrentContext> currentContextProvider,
												@Nonnull CourseApiResponseFactory courseApiResponseFactory) {
		requireNonNull(courseService);
		requireNonNull(currentContextProvider);
		requireNonNull(courseApiResponseFactory);

		this.courseService = courseService;
		this.currentContextProvider = currentContextProvider;
		this.courseApiResponseFactory = courseApiResponseFactory;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/courses")
	@AuthenticationRequired
	public ApiResponse courses() {
		InstitutionId institutionId = getCurrentContext().getInstitutionId();
		List<Course> courses = getCourseService().findCoursesByInstitutionId(institutionId);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("course", courses.stream()
					.map(course -> getCourseApiResponseFactory().create(course, CourseApiResponseType.LIST))
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
	protected CourseService getCourseService() {
		return this.courseService;
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
	protected Logger getLogger() {
		return logger;
	}
}
