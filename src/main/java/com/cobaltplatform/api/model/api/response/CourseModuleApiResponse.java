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

import com.cobaltplatform.api.model.api.response.CourseUnitApiResponse.CourseUnitApiResponseFactory;
import com.cobaltplatform.api.model.db.CourseModule;
import com.cobaltplatform.api.model.db.CourseUnit;
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
public class CourseModuleApiResponse {
	@Nonnull
	private final UUID courseModuleId;
	@Nonnull
	private final String title;
	@Nullable
	private final Integer estimatedCompletionTimeInMinutes;
	@Nullable
	private final String estimatedCompletionTimeInMinutesDescription;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final Instant lastUpdated;
	@Nonnull
	private final String lastUpdatedDescription;
	@Nonnull
	private final List<CourseUnitApiResponse> courseUnits;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface CourseModuleApiResponseFactory {
		@Nonnull
		CourseModuleApiResponse create(@Nonnull CourseModule courseModule,
																	 @Nonnull List<CourseUnit> courseUnits);
	}

	@AssistedInject
	public CourseModuleApiResponse(@Nonnull CourseUnitApiResponseFactory courseUnitApiResponseFactory,
																 @Nonnull Formatter formatter,
																 @Nonnull Strings strings,
																 @Assisted @Nonnull CourseModule courseModule,
																 @Assisted @Nonnull List<CourseUnit> courseUnits) {
		requireNonNull(courseUnitApiResponseFactory);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(courseModule);
		requireNonNull(courseUnits);

		// Sum each unit's estimated completion time to determine overall module completion time
		Integer estimatedCompletionTimeInMinutes = 0;

		for (CourseUnit courseUnit : courseUnits)
			if (courseUnit.getEstimatedCompletionTimeInMinutes() != null)
				estimatedCompletionTimeInMinutes += courseUnit.getEstimatedCompletionTimeInMinutes();

		if (estimatedCompletionTimeInMinutes == 0)
			estimatedCompletionTimeInMinutes = null;

		this.courseModuleId = courseModule.getCourseModuleId();
		this.title = courseModule.getTitle();
		this.estimatedCompletionTimeInMinutes = estimatedCompletionTimeInMinutes;
		this.estimatedCompletionTimeInMinutesDescription = estimatedCompletionTimeInMinutes == null ? null
				: strings.get("{{duration}} minutes", Map.of("duration", estimatedCompletionTimeInMinutes));
		this.created = courseModule.getCreated();
		this.createdDescription = formatter.formatTimestamp(courseModule.getCreated());
		this.lastUpdated = courseModule.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(courseModule.getLastUpdated());

		this.courseUnits = courseUnits.stream()
				.map(courseUnit -> courseUnitApiResponseFactory.create(courseUnit))
				.collect(Collectors.toList());
	}

	@Nonnull
	public UUID getCourseModuleId() {
		return this.courseModuleId;
	}

	@Nonnull
	public String getTitle() {
		return this.title;
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
	public List<CourseUnitApiResponse> getCourseUnits() {
		return this.courseUnits;
	}
}