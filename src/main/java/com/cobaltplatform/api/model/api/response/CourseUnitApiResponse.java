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

import com.cobaltplatform.api.model.db.CourseUnit;
import com.cobaltplatform.api.model.db.CourseUnitType;
import com.cobaltplatform.api.model.db.CourseUnitType.CourseUnitTypeId;
import com.cobaltplatform.api.model.api.response.CourseUnitDownloadableFileApiResponse.CourseUnitDownloadableFileApiResponseFactory;
import com.cobaltplatform.api.model.db.UnitCompletionType.UnitCompletionTypeId;
import com.cobaltplatform.api.service.CourseService;
import com.cobaltplatform.api.util.Formatter;
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
public class CourseUnitApiResponse {
	@Nonnull
	private final UUID courseUnitId;
	@Nonnull
	private final CourseUnitTypeId courseUnitTypeId;
	@Nonnull
	private final UnitCompletionTypeId unitCompletionTypeId;
	@Nonnull
	private final String courseUnitTypeIdDescription;
	@Nonnull
	private final UUID courseModuleId;
	@Nonnull
	private final String title;
	@Nullable
	private final String description;
	@Nullable
	private final Integer estimatedCompletionTimeInMinutes;
	@Nullable
	private final String estimatedCompletionTimeInMinutesDescription;
	@Nullable
	private final Integer completionThresholdInSeconds;
	@Nullable
	private final UUID videoId;
	@Nullable
	private final UUID screeningFlowId;
	@Nullable
	private final String imageUrl;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final Instant lastUpdated;
	@Nonnull
	private final String lastUpdatedDescription;
	@Nonnull
	private final List<CourseUnitDownloadableFileApiResponse> courseUnitDownloadableFiles;

	@Nonnull
	private final Boolean showRestartActivityWhenComplete;
	@Nonnull
	private final Boolean showUnitAsComplete;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface CourseUnitApiResponseFactory {
		@Nonnull
		CourseUnitApiResponse create(@Nonnull CourseUnit courseUnit);
	}

	@AssistedInject
	public CourseUnitApiResponse(@Nonnull CourseService courseService,
															 @Nonnull Formatter formatter,
															 @Nonnull Strings strings,
															 @Nonnull CourseUnitDownloadableFileApiResponseFactory courseUnitDownloadableFileApiResponseFactory,
															 @Assisted @Nonnull CourseUnit courseUnit) {
		requireNonNull(courseService);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(courseUnit);
		requireNonNull(courseUnitDownloadableFileApiResponseFactory);

		this.courseUnitId = courseUnit.getCourseUnitId();
		this.courseUnitTypeId = courseUnit.getCourseUnitTypeId();
		Optional<CourseUnitType> courseUnitType = courseService.findCourseUnitTypeById(courseUnit.getCourseUnitTypeId());
		this.showRestartActivityWhenComplete = courseUnitType.isPresent() ? courseUnitType.get().getShowRestartActivityWhenComplete() : null;
		this.showUnitAsComplete = courseUnitType.isPresent() ? courseUnitType.get().getShowUnitAsComplete() : null;
		this.unitCompletionTypeId = courseUnitType.isPresent() ? courseUnitType.get().getUnitCompletionTypeId() : null;
		this.courseUnitTypeIdDescription = courseService.determineCourseUnitTypeIdDescription(courseUnit.getCourseUnitTypeId());
		this.courseModuleId = courseUnit.getCourseModuleId();
		this.title = courseUnit.getTitle();
		this.description = courseUnit.getDescription();
		this.estimatedCompletionTimeInMinutes = courseUnit.getEstimatedCompletionTimeInMinutes();
		this.completionThresholdInSeconds = courseUnit.getCompletionThresholdInSeconds();
		this.estimatedCompletionTimeInMinutesDescription = this.estimatedCompletionTimeInMinutes == null ? null
				: strings.get("{{duration}} minutes", Map.of("duration", this.estimatedCompletionTimeInMinutes));
		this.videoId = courseUnit.getVideoId();
		this.screeningFlowId = courseUnit.getScreeningFlowId();
		this.imageUrl = courseUnit.getImageUrl();
		this.courseUnitDownloadableFiles = courseService.findCourseUnitDownloadableFiles(courseUnitId).stream()
				.map(courseUnitDownloadableFile -> courseUnitDownloadableFileApiResponseFactory.create(courseUnitDownloadableFile)).collect(Collectors.toList());
		this.created = courseUnit.getCreated();
		this.createdDescription = formatter.formatTimestamp(courseUnit.getCreated());
		this.lastUpdated = courseUnit.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(courseUnit.getLastUpdated());
	}

	@Nonnull
	public UUID getCourseUnitId() {
		return this.courseUnitId;
	}

	@Nonnull
	public UUID getCourseModuleId() {
		return this.courseModuleId;
	}

	@Nonnull
	public CourseUnitTypeId getCourseUnitTypeId() {
		return this.courseUnitTypeId;
	}
	
	@Nonnull
	public String getCourseUnitTypeIdDescription() {
		return this.courseUnitTypeIdDescription;
	}

	@Nonnull
	public String getTitle() {
		return this.title;
	}

	@Nonnull
	public Optional<String> getDescription() {
		return Optional.ofNullable(this.description);
	}

	@Nonnull
	public Optional<Integer> getEstimatedCompletionTimeInMinutes() {
		return Optional.ofNullable(this.estimatedCompletionTimeInMinutes);
	}

	@Nonnull
	public Optional<String> getEstimatedCompletionTimeInMinutesDescription() {
		return Optional.ofNullable(this.estimatedCompletionTimeInMinutesDescription);
	}

	@Nonnull
	public Optional<UUID> getVideoId() {
		return Optional.ofNullable(this.videoId);
	}

	@Nonnull
	public Optional<UUID> getScreeningFlowId() {
		return Optional.ofNullable(this.screeningFlowId);
	}

	@Nonnull
	public Optional<String> getImageUrl() {
		return Optional.ofNullable(this.imageUrl);
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
	public List<CourseUnitDownloadableFileApiResponse> getCourseUnitDownloadableFiles() {
		return courseUnitDownloadableFiles;
	}

	@Nullable
	public Integer getCompletionThresholdInSeconds() {
		return completionThresholdInSeconds;
	}

	@Nonnull
	public UnitCompletionTypeId getUnitCompletionTypeId() {
		return unitCompletionTypeId;
	}

	@Nonnull
	public Boolean getShowRestartActivityWhenComplete() {
		return showRestartActivityWhenComplete;
	}

	@Nonnull
	public Boolean getShowUnitAsComplete() {
		return showUnitAsComplete;
	}
}