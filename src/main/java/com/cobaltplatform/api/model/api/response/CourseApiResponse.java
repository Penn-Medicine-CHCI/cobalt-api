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

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.response.CourseModuleApiResponse.CourseModuleApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CourseSessionApiResponse.CourseSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.VideoApiResponse.VideoApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Course;
import com.cobaltplatform.api.model.db.CourseSession;
import com.cobaltplatform.api.model.db.CourseUnit;
import com.cobaltplatform.api.service.CourseService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class CourseApiResponse {
	@Nonnull
	private final UUID courseId;
	@Nonnull
	private final String title;
	@Nonnull
	private final String description;
	@Nonnull
	private final String focus;
	@Nonnull
	private final String imageUrl;
	@Nonnull
	private final String urlName;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final Instant lastUpdated;
	@Nonnull
	private final String lastUpdatedDescription;

	// For DETAIL type:

	@Nullable
	private final List<CourseModuleApiResponse> courseModules;
	@Nullable
	private final CourseSessionApiResponse currentCourseSession;
	@Nullable
	private final List<VideoApiResponse> videos;

	public enum CourseApiResponseType {
		LIST,
		DETAIL
	}

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface CourseApiResponseFactory {
		@Nonnull
		CourseApiResponse create(@Nonnull Course course,
														 @Nonnull CourseApiResponseType type);
	}

	@AssistedInject
	public CourseApiResponse(@Nonnull CourseService courseService,
													 @Nonnull CourseModuleApiResponseFactory courseModuleApiResponseFactory,
													 @Nonnull CourseSessionApiResponseFactory courseSessionApiResponseFactory,
													 @Nonnull VideoApiResponseFactory videoApiResponseFactory,
													 @Nonnull Provider<CurrentContext> currentContextProvider,
													 @Nonnull Formatter formatter,
													 @Nonnull Strings strings,
													 @Assisted @Nonnull Course course,
													 @Assisted @Nonnull CourseApiResponseType type) {
		requireNonNull(courseService);
		requireNonNull(courseModuleApiResponseFactory);
		requireNonNull(courseSessionApiResponseFactory);
		requireNonNull(videoApiResponseFactory);
		requireNonNull(currentContextProvider);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(course);
		requireNonNull(type);

		this.courseId = course.getCourseId();
		this.title = course.getTitle();
		this.description = course.getDescription();
		this.focus = course.getFocus();
		this.imageUrl = course.getImageUrl();
		this.urlName = course.getUrlName();
		this.created = course.getCreated();
		this.createdDescription = formatter.formatTimestamp(course.getCreated());
		this.lastUpdated = course.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(course.getLastUpdated());

		if (type == CourseApiResponseType.DETAIL) {
			List<CourseUnit> courseUnits = courseService.findCourseUnitsByCourseId(course.getCourseId());

			Map<UUID, List<CourseUnit>> courseUnitsByCourseModuleId = courseUnits.stream()
					.collect(Collectors.groupingBy(CourseUnit::getCourseModuleId));

			this.courseModules = courseService.findCourseModulesByCourseId(course.getCourseId()).stream()
					.map(courseModule -> {
						List<CourseUnit> courseUnitsForModule = courseUnitsByCourseModuleId.get(courseModule.getCourseModuleId());

						if (courseUnitsForModule == null)
							courseUnitsForModule = List.of();

						return courseModuleApiResponseFactory.create(courseModule, courseUnitsForModule);
					})
					.collect(Collectors.toList());

			Account account = currentContextProvider.get().getAccount().get();
			CourseSession courseSession = courseService.findCurrentCourseSession(account.getAccountId(), course.getCourseId()).orElse(null);

			this.currentCourseSession = courseSession == null ? null : courseSessionApiResponseFactory.create(courseSession);

			this.videos = courseService.findVideosByCourseId(course.getCourseId()).stream()
					.map(video -> videoApiResponseFactory.create(video))
					.collect(Collectors.toList());
		} else {
			this.courseModules = null;
			this.currentCourseSession = null;
			this.videos = null;
		}
	}

	@Nonnull
	public UUID getCourseId() {
		return this.courseId;
	}

	@Nonnull
	public String getTitle() {
		return this.title;
	}

	@Nonnull
	public String getDescription() {
		return this.description;
	}

	@Nonnull
	public String getFocus() {
		return this.focus;
	}

	@Nonnull
	public String getImageUrl() {
		return this.imageUrl;
	}

	@Nonnull
	public String getUrlName() {
		return this.urlName;
	}

	@Nonnull
	public Instant getCreated() {
		return this.created;
	}

	@Nonnull
	public String getCreatedDescription() {
		return this.createdDescription;
	}

	@Nonnull
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	@Nonnull
	public String getLastUpdatedDescription() {
		return this.lastUpdatedDescription;
	}

	@Nonnull
	public Optional<List<CourseModuleApiResponse>> getCourseModules() {
		return Optional.ofNullable(this.courseModules);
	}

	@Nonnull
	public Optional<CourseSessionApiResponse> getCurrentCourseSession() {
		return Optional.ofNullable(this.currentCourseSession);
	}
}