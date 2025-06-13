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

import com.cobaltplatform.api.model.db.CourseSession;
import com.cobaltplatform.api.model.db.CourseSessionStatus.CourseSessionStatusId;
import com.cobaltplatform.api.model.db.CourseSessionUnit;
import com.cobaltplatform.api.model.db.CourseSessionUnitStatus.CourseSessionUnitStatusId;
import com.cobaltplatform.api.model.db.CourseUnit;
import com.cobaltplatform.api.model.db.CourseUnitDependency;
import com.cobaltplatform.api.model.service.CourseSessionCompletionPercentage;
import com.cobaltplatform.api.model.service.CourseUnitLockStatus;
import com.cobaltplatform.api.service.CourseService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class CourseSessionApiResponse {
	@Nonnull
	private final UUID courseSessionId;
	@Nonnull
	private final UUID courseId;
	@Nonnull
	private final UUID accountId;
	@Nonnull
	private final CourseSessionStatusId courseSessionStatusId;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final Instant lastUpdated;
	@Nonnull
	private final String lastUpdatedDescription;

	// Computed fields follow

	@Nonnull
	private final Map<UUID, CourseUnitLockStatus> courseUnitLockStatusesByCourseUnitId;
	@Nonnull
	private final Map<UUID, CourseSessionUnitStatusId> courseSessionUnitStatusIdsByCourseUnitId;
	@Nonnull
	private final List<UUID> optionalCourseModuleIds;

	@Nonnull
	private final CourseSessionCompletionPercentage courseSessionCompletionPercentage;

	@Nonnull
	private final String completionPercentage;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface CourseSessionApiResponseFactory {
		@Nonnull
		CourseSessionApiResponse create(@Nonnull CourseSession courseSession);
	}

	@AssistedInject
	public CourseSessionApiResponse(@Nonnull CourseService courseService,
																	@Nonnull Formatter formatter,
																	@Nonnull Strings strings,
																	@Assisted @Nonnull CourseSession courseSession) {
		requireNonNull(courseService);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(courseSession);

		this.courseSessionId = courseSession.getCourseSessionId();
		this.courseId = courseSession.getCourseId();
		this.accountId = courseSession.getAccountId();
		this.courseSessionStatusId = courseSession.getCourseSessionStatusId();
		this.created = courseSession.getCreated();
		this.createdDescription = formatter.formatTimestamp(courseSession.getCreated());
		this.lastUpdated = courseSession.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(courseSession.getLastUpdated());

		// Show status for each course unit in the session.
		// If we don't have status yet, create a synthetic "INCOMPLETE" record
		List<CourseUnit> courseUnits = courseService.findCourseUnitsByCourseId(courseSession.getCourseId());
		List<CourseSessionUnit> courseSessionUnits = courseService.findCourseSessionUnitsByCourseSessionId(courseSession.getCourseSessionId());
		List<CourseUnitDependency> courseUnitDependencies = courseService.findCourseUnitDependenciesByCourseId(courseSession.getCourseId());

		Map<UUID, CourseSessionUnit> courseSessionUnitsByCourseUnitId = courseSessionUnits.stream()
				.collect(Collectors.toMap(CourseSessionUnit::getCourseUnitId, Function.identity()));

		// Calculate and expose our dependencies so we know which units are locked
		this.courseUnitLockStatusesByCourseUnitId = courseService.determineCourseUnitLockStatusesByCourseUnitId(courseUnits, courseSessionUnits, courseUnitDependencies);

		Map<UUID, CourseSessionUnitStatusId> courseSessionUnitStatusIdsByCourseUnitId = new HashMap<>(courseUnits.size());

		for (CourseUnit courseUnit : courseUnits) {
			CourseSessionUnit courseSessionUnit = courseSessionUnitsByCourseUnitId.get(courseUnit.getCourseUnitId());

			if (courseSessionUnit != null)
				courseSessionUnitStatusIdsByCourseUnitId.put(courseUnit.getCourseUnitId(), courseSessionUnit.getCourseSessionUnitStatusId());
		}

		this.courseSessionUnitStatusIdsByCourseUnitId = courseSessionUnitStatusIdsByCourseUnitId;

		this.optionalCourseModuleIds = courseService.findOptionalCourseModuleIdsByCourseSessionId(courseSession.getCourseSessionId());

		Optional<CourseSessionCompletionPercentage> sessionCompletionPercentage = courseService.findCourseSessionCompletionPercentage(courseSessionId);
		this.courseSessionCompletionPercentage = sessionCompletionPercentage.orElse(null);

		this.completionPercentage = sessionCompletionPercentage.isPresent() ?
				formatter.formatPercent(sessionCompletionPercentage.get().getCompletionPercentage()) : null;

		// TODO: fill in remaining fields
	}

	@Nonnull
	public UUID getCourseSessionId() {
		return this.courseSessionId;
	}

	@Nonnull
	public UUID getCourseId() {
		return this.courseId;
	}

	@Nonnull
	public UUID getAccountId() {
		return this.accountId;
	}

	@Nonnull
	public CourseSessionStatusId getCourseSessionStatusId() {
		return this.courseSessionStatusId;
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
	public CourseSessionCompletionPercentage getCourseSessionCompletionPercentage() {
		return courseSessionCompletionPercentage;
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
	public Map<UUID, CourseUnitLockStatus> getCourseUnitLockStatusesByCourseUnitId() {
		return this.courseUnitLockStatusesByCourseUnitId;
	}

	@Nonnull
	public Map<UUID, CourseSessionUnitStatusId> getCourseSessionUnitStatusIdsByCourseUnitId() {
		return this.courseSessionUnitStatusIdsByCourseUnitId;
	}

	@Nonnull
	public List<UUID> getOptionalCourseModuleIds() {
		return this.optionalCourseModuleIds;
	}
}