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

import com.cobaltplatform.api.model.api.request.CreateCourseSessionRequest;
import com.cobaltplatform.api.model.db.Course;
import com.cobaltplatform.api.model.db.CourseModule;
import com.cobaltplatform.api.model.db.CourseSession;
import com.cobaltplatform.api.model.db.CourseSessionUnit;
import com.cobaltplatform.api.model.db.CourseSessionUnitStatus.CourseSessionUnitStatusId;
import com.cobaltplatform.api.model.db.CourseUnit;
import com.cobaltplatform.api.model.db.CourseUnitDependency;
import com.cobaltplatform.api.model.db.CourseUnitDependencyType.CourseUnitDependencyTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.service.CourseUnitLockStatus;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
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
import java.util.ArrayList;
import java.util.HashMap;
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
	public Optional<Course> findCourseById(@Nullable UUID courseId,
																				 @Nullable InstitutionId institutionId) {
		if (courseId == null || institutionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_course
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
				SELECT *
				FROM v_course
				WHERE institution_id=?
				AND url_name=?
				""", Course.class, institutionId, urlName);
	}

	@Nonnull
	public Optional<Course> findCourseByIdentifier(@Nullable String courseIdentifier,
																								 @Nullable InstitutionId institutionId) {
		if (courseIdentifier == null || institutionId == null)
			return Optional.empty();

		if (ValidationUtility.isValidUUID(courseIdentifier))
			return findCourseById(UUID.fromString(courseIdentifier), institutionId);

		return findCourseByInstitutionIdAndUrlName(institutionId, courseIdentifier);
	}

	@Nonnull
	public List<Course> findCoursesByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM v_course
				WHERE institution_id=?
				ORDER BY display_order
				""", Course.class, institutionId);
	}

	@Nonnull
	public List<CourseModule> findCourseModulesByCourseId(@Nullable UUID courseId) {
		if (courseId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM course_module
				WHERE course_id=?
				ORDER BY display_order
				""", CourseModule.class, courseId);
	}

	@Nonnull
	public List<CourseUnit> findCourseUnitsByCourseId(@Nullable UUID courseId) {
		if (courseId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT cu.*
				FROM course_module cm, course_unit cu
				WHERE cm.course_id=?
				AND cu.course_module_id=cm.course_module_id
				ORDER BY cu.display_order
				""", CourseUnit.class, courseId);
	}

	@Nonnull
	public Optional<CourseSession> findCourseSessionById(@Nullable UUID courseSessionId) {
		if (courseSessionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM course_session
				WHERE course_session_id=?
				""", CourseSession.class, courseSessionId);
	}

	@Nonnull
	public Optional<CourseSession> findCurrentCourseSession(@Nullable UUID accountId,
																													@Nullable UUID courseId) {
		if (accountId == null || courseId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM course_session
				WHERE account_id=?
				AND course_id=?
				ORDER BY created DESC
				LIMIT 1
				""", CourseSession.class, accountId, courseId);
	}

	@Nonnull
	public UUID createCourseSession(@Nonnull CreateCourseSessionRequest request) {
		requireNonNull(request);

		UUID courseId = request.getCourseId();
		UUID accountId = request.getAccountId();
		UUID courseSessionId = UUID.randomUUID();
		ValidationException validationException = new ValidationException();

		if (courseId == null)
			validationException.add(new FieldError("courseId", getStrings().get("Course ID is required.")));

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				INSERT INTO course_session (
				  course_session_id,
				  course_id,
				  account_id
				) VALUES (?,?,?)
				""", courseSessionId, courseId, accountId);

		return courseSessionId;
	}

	@Nonnull
	public List<CourseSessionUnit> findCourseSessionUnitsByCourseSessionId(@Nullable UUID courseSessionId) {
		if (courseSessionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT csu.*
				FROM course_session_unit csu, course_unit cu
				WHERE csu.course_session_id=?
				AND csu.course_unit_id=cu.course_unit_id
				ORDER BY cu.display_order
				""", CourseSessionUnit.class, courseSessionId);
	}

	@Nonnull
	public List<CourseUnitDependency> findCourseUnitDependenciesByCourseId(@Nullable UUID courseId) {
		if (courseId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT cud.*
				FROM course_unit_dependency cud, course_unit cu, course_module cm
				WHERE cud.determinant_course_unit_id=cu.course_unit_id
				AND cu.course_module_id=cm.course_module_id
				AND cm.course_id=?
				ORDER BY cud.determinant_course_unit_id, cud.dependent_course_unit_id
				""", CourseUnitDependency.class, courseId);
	}

	@Nonnull
	public Map<UUID, CourseUnitLockStatus> determineCourseUnitLockStatusesByCourseUnitId(@Nullable List<CourseUnit> courseUnits,
																																											 @Nullable List<CourseSessionUnit> courseSessionUnits,
																																											 @Nullable List<CourseUnitDependency> courseUnitDependencies) {
		if (courseUnits == null)
			return Map.of();

		if (courseSessionUnits == null)
			courseSessionUnits = List.of();

		if (courseUnitDependencies == null)
			courseUnitDependencies = List.of();

		// Build a set of completed course unit IDs for this session
		Set<UUID> completedCourseSessionUnitIds = courseSessionUnits.stream()
				.filter(courseSessionUnit -> courseSessionUnit.getCourseSessionUnitStatusId() == CourseSessionUnitStatusId.COMPLETE)
				.map(CourseSessionUnit::getCourseUnitId)
				.collect(Collectors.toSet());

		Map<UUID, CourseUnitLockStatus> courseUnitLockStatusesByCourseUnitId = new HashMap<>();

		for (CourseUnit unit : courseUnits) {
			Map<CourseUnitDependencyTypeId, List<UUID>> determinantCourseUnitIdsByDependencyTypeIds = new HashMap<>();

			for (CourseUnitDependency courseUnitDependency : courseUnitDependencies)
				if (!completedCourseSessionUnitIds.contains(courseUnitDependency.getDeterminantCourseUnitId()))
					determinantCourseUnitIdsByDependencyTypeIds.computeIfAbsent(courseUnitDependency.getCourseUnitDependencyTypeId(), cudti -> new ArrayList<>()).add(courseUnitDependency.getDeterminantCourseUnitId());

			courseUnitLockStatusesByCourseUnitId.put(unit.getCourseUnitId(), new CourseUnitLockStatus(determinantCourseUnitIdsByDependencyTypeIds));
		}

		return courseUnitLockStatusesByCourseUnitId;
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