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

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface CourseSessionApiResponseFactory {
		@Nonnull
		CourseSessionApiResponse create(@Nonnull CourseSession courseSession);
	}

	@AssistedInject
	public CourseSessionApiResponse(@Nonnull Formatter formatter,
																	@Nonnull Strings strings,
																	@Assisted @Nonnull CourseSession courseSession) {
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
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	@Nonnull
	public String getLastUpdatedDescription() {
		return this.lastUpdatedDescription;
	}
}