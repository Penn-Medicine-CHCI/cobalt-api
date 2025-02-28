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

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.model.db.Course;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.util.ValidationUtility;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class CourseService {
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public CourseService(@Nonnull DatabaseProvider databaseProvider,
											 @Nonnull Strings strings) {
		requireNonNull(databaseProvider);
		requireNonNull(strings);

		this.databaseProvider = databaseProvider;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<Course> findCourseById(@Nullable UUID courseId) {
		if (courseId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM course
				WHERE course_id=?
				""", Course.class, courseId);
	}

	@Nonnull
	public Optional<Course> findCourseByInstitutionIdAndUrlName(@Nullable InstitutionId institutionId,
																															@Nullable String urlName) {
		if (institutionId == null || urlName == null)
			return Optional.empty();

		urlName = urlName.trim();

		return getDatabase().queryForObject("""
				SELECT c.*
				FROM course c, institution_course ic
				WHERE c.course_id=ic.course_id 
				AND ic.institution_id=?
				AND ic.url_name=?
				""", Course.class, institutionId, urlName);
	}

	@Nonnull
	public Optional<Course> findCourseByIdentifier(@Nullable String courseIdentifier,
																								 @Nullable InstitutionId institutionId) {
		if (courseIdentifier == null || institutionId == null)
			return Optional.empty();

		if (ValidationUtility.isValidUUID(courseIdentifier))
			return findCourseById(UUID.fromString(courseIdentifier));

		return findCourseByInstitutionIdAndUrlName(institutionId, courseIdentifier);
	}

	@Nonnull
	public List<Course> findCoursesByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT c.*
				FROM course c, institution_course ic
				WHERE c.course_id=ic.course_id
				AND ic.institution_id=?
				ORDER BY ic.display_order
				""", Course.class, institutionId);
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}