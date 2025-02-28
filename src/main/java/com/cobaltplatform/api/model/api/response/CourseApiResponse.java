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

import com.cobaltplatform.api.model.db.Course;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.UUID;

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
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final Instant lastUpdated;
	@Nonnull
	private final String lastUpdatedDescription;

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
	public CourseApiResponse(@Nonnull Formatter formatter,
													 @Nonnull Strings strings,
													 @Assisted @Nonnull Course course,
													 @Assisted @Nonnull CourseApiResponseType type) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(course);
		requireNonNull(type);

		this.courseId = course.getCourseId();
		this.title = course.getTitle();
		this.description = course.getDescription();
		this.focus = course.getFocus();
		this.imageUrl = course.getImageUrl();
		this.created = course.getCreated();
		this.createdDescription = formatter.formatTimestamp(course.getCreated());
		this.lastUpdated = course.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(course.getLastUpdated());
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
}