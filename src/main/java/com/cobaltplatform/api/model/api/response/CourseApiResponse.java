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
import com.cobaltplatform.api.model.api.response.ContentApiResponse.ContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ContentApiResponse.ContentApiResponseSupplement;
import com.cobaltplatform.api.model.api.response.CourseModuleApiResponse.CourseModuleApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CourseSessionApiResponse.CourseSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.VideoApiResponse.VideoApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Course;
import com.cobaltplatform.api.model.db.CourseUnit;
import com.cobaltplatform.api.model.service.CourseUnitLockStatus;
import com.cobaltplatform.api.service.ContentService;
import com.cobaltplatform.api.service.CourseService;
import com.cobaltplatform.api.service.VideoService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
	@Nullable
	private final Map<UUID, CourseUnitLockStatus> defaultCourseUnitLockStatusesByCourseUnitId;
	@Nullable
	private final Map<UUID, List<ContentApiResponse>> contentsByCourseUnitId;

	public enum CourseApiResponseType {
		LIST,
		DETAIL,
		PARTIAL_DETAIL
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
													 @Nonnull ContentService contentService,
													 @Nonnull VideoService videoService,
													 @Nonnull CourseModuleApiResponseFactory courseModuleApiResponseFactory,
													 @Nonnull CourseSessionApiResponseFactory courseSessionApiResponseFactory,
													 @Nonnull VideoApiResponseFactory videoApiResponseFactory,
													 @Nonnull ContentApiResponseFactory contentApiResponseFactory,
													 @Nonnull Provider<CurrentContext> currentContextProvider,
													 @Nonnull Formatter formatter,
													 @Nonnull Strings strings,
													 @Assisted @Nonnull Course course,
													 @Assisted @Nonnull CourseApiResponseType type) {
		requireNonNull(courseService);
		requireNonNull(contentService);
		requireNonNull(videoService);
		requireNonNull(courseModuleApiResponseFactory);
		requireNonNull(courseSessionApiResponseFactory);
		requireNonNull(videoApiResponseFactory);
		requireNonNull(contentApiResponseFactory);
		requireNonNull(currentContextProvider);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(course);
		requireNonNull(type);

		List<CourseModuleApiResponse> courseModulesTemp = null;
		CourseSessionApiResponse currentCourseSessionTemp = null;
		List<VideoApiResponse> videosTemp = null;
		Map<UUID, CourseUnitLockStatus> defaultCourseUnitLockStatusesByCourseUnitIdTemp = null;
		Map<UUID, List<ContentApiResponse>> contentsByCourseUnitIdTemp = null;

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

		Account account = currentContextProvider.get()
				.getAccount()
				.orElseThrow(() -> new IllegalStateException("No logged-in account"));

		switch (type) {
			case DETAIL:
				// 1) load & group all units
				List<CourseUnit> allUnits = courseService.findCourseUnitsByCourseId(course.getCourseId());
				Map<UUID, List<CourseUnit>> unitsByModule =
						allUnits.stream()
								.collect(Collectors.groupingBy(CourseUnit::getCourseModuleId));

				// 2) build full module list (with units)
				courseModulesTemp = courseService.findCourseModulesByCourseId(course.getCourseId()).stream()
						.map(module -> {
							List<CourseUnit> units = unitsByModule.getOrDefault(module.getCourseModuleId(),
									Collections.emptyList());
							return courseModuleApiResponseFactory.create(module, units);
						})
						.collect(Collectors.toList());

				// 3) videos, locks, contents only for DETAIL
				videosTemp = videoService.findVideosByCourseId(course.getCourseId()).stream()
						.map(videoApiResponseFactory::create)
						.collect(Collectors.toList());

				defaultCourseUnitLockStatusesByCourseUnitIdTemp =
						courseService.determineDefaultCourseUnitLockStatusesByCourseUnitId(course.getCourseId());

				contentsByCourseUnitIdTemp = contentService.findContentsByCourseUnitIdForCourseId(course.getCourseId(), account.getInstitutionId()).entrySet().stream()
						.collect(Collectors.toMap(
								Map.Entry::getKey,
								entry -> entry.getValue().stream()
										.map(content -> contentApiResponseFactory.create(
												content, Set.of(ContentApiResponseSupplement.TAGS)))
										.toList()
						));
				// fall-through to PARTIAL_DETAIL logic
			case PARTIAL_DETAIL:
				// current session is needed for both DETAIL & PARTIAL_DETAIL
				currentCourseSessionTemp = courseService
						.findCurrentCourseSession(account.getAccountId(), course.getCourseId())
						.map(courseSessionApiResponseFactory::create)
						.orElse(null);
				break;

			case LIST:
			default:
				// only IDs & basics in LIST
				courseModulesTemp = null;
				currentCourseSessionTemp = null;
				videosTemp = null;
				defaultCourseUnitLockStatusesByCourseUnitIdTemp = null;
				contentsByCourseUnitIdTemp = null;
				break;
		}

		this.courseModules = courseModulesTemp;
		this.currentCourseSession = currentCourseSessionTemp;
		this.videos = videosTemp;
		this.defaultCourseUnitLockStatusesByCourseUnitId = defaultCourseUnitLockStatusesByCourseUnitIdTemp;
		this.contentsByCourseUnitId = contentsByCourseUnitIdTemp;

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

	@Nonnull
	public Optional<List<VideoApiResponse>> getVideos() {
		return Optional.ofNullable(this.videos);
	}

	@Nonnull
	public Optional<Map<UUID, CourseUnitLockStatus>> getDefaultCourseUnitLockStatusesByCourseUnitId() {
		return Optional.ofNullable(this.defaultCourseUnitLockStatusesByCourseUnitId);
	}

	@Nonnull
	public Optional<Map<UUID, List<ContentApiResponse>>> getContentsByCourseUnitId() {
		return Optional.ofNullable(this.contentsByCourseUnitId);
	}
}