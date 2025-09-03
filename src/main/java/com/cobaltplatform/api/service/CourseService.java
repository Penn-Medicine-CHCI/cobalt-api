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

import com.cobaltplatform.api.model.api.request.CompleteCourseUnitRequest;
import com.cobaltplatform.api.model.api.request.CreateCourseSessionRequest;
import com.cobaltplatform.api.model.api.request.UpdateCourseSessionUnitCompletionMessageRequest;
import com.cobaltplatform.api.model.db.Course;
import com.cobaltplatform.api.model.db.CourseModule;
import com.cobaltplatform.api.model.db.CourseSession;
import com.cobaltplatform.api.model.db.CourseSessionStatus.CourseSessionStatusId;
import com.cobaltplatform.api.model.db.CourseSessionUnit;
import com.cobaltplatform.api.model.db.CourseSessionUnitStatus.CourseSessionUnitStatusId;
import com.cobaltplatform.api.model.db.CourseUnit;
import com.cobaltplatform.api.model.db.CourseUnitDependency;
import com.cobaltplatform.api.model.db.CourseUnitDependencyType.CourseUnitDependencyTypeId;
import com.cobaltplatform.api.model.db.CourseUnitType;
import com.cobaltplatform.api.model.db.CourseUnitType.CourseUnitTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.service.CourseSessionCompletionPercentage;
import com.cobaltplatform.api.model.service.CourseUnitDownloadableFileWithFileDetails;
import com.cobaltplatform.api.model.service.CourseUnitLockStatus;
import com.cobaltplatform.api.model.service.CourseWithCourseSessionStatus;
import com.cobaltplatform.api.model.service.CourseWithInstitutionCourseStatus;
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

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

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
	public List<UUID> findOptionalCourseModuleIdsByCourseSessionId(@Nullable UUID courseSessionId) {
		if (courseSessionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT cm.course_module_id
				FROM course_session_optional_module csom, course_module cm
				WHERE csom.course_session_id=?
				AND csom.course_module_id=cm.course_module_id
				ORDER BY cm.display_order
				""", UUID.class, courseSessionId);
	}

	@Nonnull
	public Optional<CourseUnit> findCourseUnitById(@Nullable UUID courseUnitId) {
		if (courseUnitId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM course_unit
				WHERE course_unit_id=?
				""", CourseUnit.class, courseUnitId);
	}

	@Nonnull
	public Optional<Course> findCourseByCourseUnitId(@Nullable UUID courseUnitId) {
		if (courseUnitId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT c.*
				FROM course_unit cu, course_module cm, course c
				WHERE cu.course_unit_id=?
				AND cu.course_module_id=cm.course_module_id
				AND cm.course_id=c.course_id
				""", Course.class, courseUnitId);
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

	public List<CourseWithInstitutionCourseStatus> findComingSoonAndAvailableCourses(@Nullable UUID accountId,
																																									 @Nullable InstitutionId institutionId) {
		if (accountId == null || institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT c.*, ic.institution_course_status_id
				FROM v_course c, institution_course ic
				WHERE c.course_id = ic.course_id				
				AND ic.institution_id=?
				AND c.course_id NOT IN
				(SELECT cs.course_id
				FROM course_session cs
				WHERE cs.account_id=?)
				ORDER BY ic.display_order
				""", CourseWithInstitutionCourseStatus.class, institutionId, accountId);
	}

	public List<CourseWithCourseSessionStatus> findInProgressAndCompletedCourseSessions(
			@Nullable UUID accountId,
			@Nullable InstitutionId institutionId) {
		if (accountId == null || institutionId == null) {
			return List.of();
		}

		return getDatabase().queryForList("""
							WITH prioritized_sessions AS (
							SELECT
								cs.course_id,
								cs.course_session_status_id,
								cs.created,
								ROW_NUMBER() OVER (
									PARTITION BY cs.course_id
									ORDER BY
										-- rank IN_PROGRESS first, then COMPLETED,
										CASE cs.course_session_status_id
											WHEN ? THEN 1
											WHEN ? THEN 2
										END,
										cs.created DESC
								) AS rn
							FROM course_session cs
							WHERE cs.account_id = ?
								AND cs.course_session_status_id IN (?,?)
						)
						SELECT
							c.*,
							ps.course_session_status_id
						FROM prioritized_sessions ps
						JOIN v_course c
							ON c.course_id = ps.course_id
						JOIN institution_course ic
							ON ic.course_id = c.course_id
						 AND ic.institution_id = ?
						WHERE ps.rn = 1
						ORDER BY ps.created DESC
						""",
				CourseWithCourseSessionStatus.class,
				CourseSessionStatusId.IN_PROGRESS,
				CourseSessionStatusId.COMPLETED,
				accountId,
				CourseSessionStatusId.IN_PROGRESS,
				CourseSessionStatusId.COMPLETED,
				institutionId
		);
	}


	public Optional<CourseSessionCompletionPercentage> findCourseSessionCompletionPercentage(@Nullable UUID courseSessionId) {
		if (courseSessionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				WITH session_courses AS (
				  -- grab only the one session’s course
				  SELECT
				    cs.course_session_id,
				    cs.course_id
				  FROM course_session AS cs
				  WHERE cs.course_session_id = ?
				),
				all_units AS (
				  -- for that session, pull in every unit in its course
				  SELECT
				    sc.course_session_id,
				    cu.course_unit_id,
				    cu.estimated_completion_time_in_minutes
				  FROM session_courses AS sc
				  JOIN course_module AS cm
				    ON cm.course_id = sc.course_id
				  JOIN course_unit AS cu
				    ON cu.course_module_id = cm.course_module_id
				     WHERE cm.course_module_id NOT IN
				 (SELECT cso.course_module_id
				   FROM course_session_optional_module cso
				  WHERE cso.course_session_id = sc.course_session_id)
				)
				SELECT
				  au.course_session_id,
				
				  -- completed minutes (only units marked COMPLETED)
				  SUM(
				    CASE
				      WHEN csu.course_session_unit_status_id = 'COMPLETED'
				      THEN au.estimated_completion_time_in_minutes
				      ELSE 0
				    END
				  ) AS minutes_completed,
				
				  -- total minutes possible (all units in the course)
				  SUM(au.estimated_completion_time_in_minutes) AS total_minutes,
				
				  -- completion percentage
				  ROUND(
				    SUM(
				      CASE
				        WHEN csu.course_session_unit_status_id = 'COMPLETED'
				        THEN au.estimated_completion_time_in_minutes
				        ELSE 0
				      END
				    )::numeric
				    / NULLIF(SUM(au.estimated_completion_time_in_minutes), 0)				    
				  , 2) AS completion_percentage
				
				FROM all_units AS au
				
				-- bring in any completion status rows (if they exist)
				LEFT JOIN course_session_unit AS csu
				  ON csu.course_session_id = au.course_session_id
				 AND csu.course_unit_id    = au.course_unit_id
				
				-- since CTE already filtered to the one session, no extra WHERE is needed
				GROUP BY
				  au.course_session_id;				    
				""", CourseSessionCompletionPercentage.class, courseSessionId);
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
	public Optional<CourseUnit> findCourseUnitByCourseSessionIdAndScreeningQuestionId(@Nullable UUID courseSessionId,
																																										@Nullable UUID screeningQuestionId) {
		if (courseSessionId == null || screeningQuestionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT DISTINCT cu.*
				FROM screening_question sq, screening_session ss, v_screening_session_screening sss, course_unit cu, course_session cs, course_module cm, screening_flow_version sfv
				WHERE ss.course_session_id=?
				AND ss.screening_session_id=sss.screening_session_id
				AND sss.screening_version_id=sq.screening_version_id
				AND sq.screening_question_id=?
				AND cu.course_module_id=cm.course_module_id
				AND cm.course_id=cs.course_id
				AND cs.course_session_id=ss.course_session_id
				AND ss.screening_flow_version_id=sfv.screening_flow_version_id
				AND sfv.screening_flow_id=cu.screening_flow_id
				""", CourseUnit.class, courseSessionId, screeningQuestionId);
	}

	@Nonnull
	public Boolean updateCourseSessionUnitCompletionMessage(@Nonnull UpdateCourseSessionUnitCompletionMessageRequest request) {
		requireNonNull(request);

		UUID courseSessionId = request.getCourseSessionId();
		UUID courseUnitId = request.getCourseUnitId();
		String completionMessage = trimToNull(request.getCompletionMessage());
		ValidationException validationException = new ValidationException();

		if (courseSessionId == null)
			validationException.add(new FieldError("courseSessionId", getStrings().get("Course Session ID is required.")));

		if (courseUnitId == null)
			validationException.add(new FieldError("courseUnitId", getStrings().get("Course Unit ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		boolean updated = getDatabase().execute("""
				UPDATE course_session_unit
				SET completion_message=?
				WHERE course_session_id=?
				AND course_unit_id=?
				""", completionMessage, courseSessionId, courseUnitId) > 0;

		return updated;
	}

	@Nonnull
	public Boolean completeCourseUnit(@Nonnull CompleteCourseUnitRequest request) {
		requireNonNull(request);
		logger.debug("completeCourseUnit");
		UUID courseSessionId = request.getCourseSessionId();
		UUID courseUnitId = request.getCourseUnitId();
		UUID accountId = request.getAccountId();
		ValidationException validationException = new ValidationException();

		if (courseSessionId == null)
			validationException.add(new FieldError("courseSessionId", getStrings().get("Course Session ID is required.")));

		if (courseUnitId == null)
			validationException.add(new FieldError("courseUnitId", getStrings().get("Course Unit ID is required.")));

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		// Already completed for this session? No-op
		CourseSessionUnit courseSessionUnit = findCourseSessionUnitByCourseSessionIdAndUnitId(courseSessionId, courseUnitId).orElse(null);

		if (courseSessionUnit != null && courseSessionUnit.getCourseSessionUnitStatusId() == CourseSessionUnitStatusId.COMPLETED)
			return false;

		// Mark completed, supporting concurrent updates via ON CONFLICT DO UPDATE
		boolean updated = getDatabase().execute("""
				INSERT INTO course_session_unit (course_session_id, course_unit_id, course_session_unit_status_id)
				VALUES (?,?,?)
				ON CONFLICT (course_session_id, course_unit_id)
				DO UPDATE SET course_session_unit_status_id = EXCLUDED.course_session_unit_status_id;
				""", courseSessionId, courseUnitId, CourseSessionUnitStatusId.COMPLETED) > 0;

		if (updated) {
			checkAndSetCourseComplete(courseSessionId, courseUnitId);
		}

		return updated;
	}

	@Nonnull
	public void checkAndSetCourseComplete(@Nonnull UUID courseSessionId,
																				@Nonnull UUID courseUnitId) {
		ValidationException validationException = new ValidationException();
		if (courseSessionId == null)
			validationException.add(new FieldError("courseSessionId", getStrings().get("Course Session ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		Optional<Course> course = getDatabase().queryForObject("""
				SELECT vc.*
				FROM v_course vc, course_module cm, course_unit cu
				WHERE vc.course_id = cm.course_id
				AND cm.course_module_id = cu.course_module_id
				AND cu.course_unit_id=?
				""", Course.class, courseUnitId);

		if (course.isPresent()) {
			Boolean courseComplete = getDatabase().queryForObject("""
					SELECT COUNT(*) = 0
					FROM course_unit cu, course_module cm
					WHERE cu.course_module_id = cm.course_module_id
					AND cu.optional_unit = FALSE
					AND cm.course_id = ?
					AND cm.course_module_id NOT IN
					(SELECT cso.course_module_id
					FROM course_session_optional_module cso
					WHERE cso.course_session_id = ?)
					AND cu.course_unit_id NOT IN
					(SELECT csu.course_unit_id
					FROM course_session_unit csu, course_session cs
					WHERE csu.course_session_id = cs.course_session_id
					AND cs.course_session_id = ?)					
					""", Boolean.class, course.get().getCourseId(), courseSessionId, courseSessionId).get();

			if (courseComplete)
				getDatabase().execute("""
						UPDATE course_session
						SET course_session_status_id = ?
						WHERE course_session_id=?
						""", CourseSessionStatusId.COMPLETED, courseSessionId);
		}
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
	public Optional<CourseSessionUnit> findCourseSessionUnitByCourseSessionIdAndUnitId(@Nullable UUID courseSessionId,
																																										 @Nullable UUID courseUnitId) {
		if (courseSessionId == null || courseUnitId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM course_session_unit
				WHERE course_session_id=?
				AND course_unit_id=?
				""", CourseSessionUnit.class, courseSessionId, courseUnitId);
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
	public Map<UUID, CourseUnitLockStatus> determineDefaultCourseUnitLockStatusesByCourseUnitId(@Nullable UUID courseId) {
		if (courseId == null)
			return Map.of();

		List<CourseUnit> courseUnits = findCourseUnitsByCourseId(courseId);
		List<CourseUnitDependency> courseUnitDependencies = findCourseUnitDependenciesByCourseId(courseId);

		// Determine locks per course unit based on dependencies + completion status
		Map<UUID, CourseUnitLockStatus> courseUnitLockStatusesByCourseUnitId = new HashMap<>();

		for (CourseUnit courseUnit : courseUnits) {
			Map<CourseUnitDependencyTypeId, List<UUID>> determinantCourseUnitIdsByDependencyTypeIds = new HashMap<>();
			List<CourseUnitDependency> applicableCourseUnitDependencies = courseUnitDependencies.stream()
					.filter(courseUnitDependency -> courseUnitDependency.getDependentCourseUnitId().equals(courseUnit.getCourseUnitId()))
					.collect(Collectors.toUnmodifiableList());

			for (CourseUnitDependency courseUnitDependency : applicableCourseUnitDependencies)
				determinantCourseUnitIdsByDependencyTypeIds.computeIfAbsent(courseUnitDependency.getCourseUnitDependencyTypeId(), cudti -> new ArrayList<>()).add(courseUnitDependency.getDeterminantCourseUnitId());

			courseUnitLockStatusesByCourseUnitId.put(courseUnit.getCourseUnitId(), new CourseUnitLockStatus(determinantCourseUnitIdsByDependencyTypeIds));
		}

		return courseUnitLockStatusesByCourseUnitId;
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
				.filter(courseSessionUnit -> courseSessionUnit.getCourseSessionUnitStatusId() == CourseSessionUnitStatusId.COMPLETED)
				.map(CourseSessionUnit::getCourseUnitId)
				.collect(Collectors.toSet());

		// Determine locks per course unit based on dependencies + completion status
		Map<UUID, CourseUnitLockStatus> courseUnitLockStatusesByCourseUnitId = new HashMap<>();

		for (CourseUnit courseUnit : courseUnits) {
			Map<CourseUnitDependencyTypeId, List<UUID>> determinantCourseUnitIdsByDependencyTypeIds = new HashMap<>();
			List<CourseUnitDependency> applicableCourseUnitDependencies = courseUnitDependencies.stream()
					.filter(courseUnitDependency -> courseUnitDependency.getDependentCourseUnitId().equals(courseUnit.getCourseUnitId()))
					.collect(Collectors.toUnmodifiableList());

			for (CourseUnitDependency courseUnitDependency : applicableCourseUnitDependencies)
				if (!completedCourseSessionUnitIds.contains(courseUnitDependency.getDeterminantCourseUnitId()))
					determinantCourseUnitIdsByDependencyTypeIds.computeIfAbsent(courseUnitDependency.getCourseUnitDependencyTypeId(), cudti -> new ArrayList<>()).add(courseUnitDependency.getDeterminantCourseUnitId());

			courseUnitLockStatusesByCourseUnitId.put(courseUnit.getCourseUnitId(), new CourseUnitLockStatus(determinantCourseUnitIdsByDependencyTypeIds));
		}

		return courseUnitLockStatusesByCourseUnitId;
	}

	@Nonnull
	public String determineCourseUnitTypeIdDescription(@Nonnull CourseUnitTypeId courseUnitTypeId) {
		requireNonNull(courseUnitTypeId);

		if (courseUnitTypeId == CourseUnitTypeId.QUIZ
				|| courseUnitTypeId == CourseUnitTypeId.CARD_SORT
				|| courseUnitTypeId == CourseUnitTypeId.REORDER)
			return getStrings().get("Activity");

		if (courseUnitTypeId == CourseUnitTypeId.VIDEO)
			return getStrings().get("Video");

		if (courseUnitTypeId == CourseUnitTypeId.INFOGRAPHIC
				|| courseUnitTypeId == CourseUnitTypeId.HOMEWORK
				|| courseUnitTypeId == CourseUnitTypeId.THINGS_TO_SHARE)
			return getStrings().get("Info");

		throw new UnsupportedOperationException(format("Unexpected value: %s.%s", CourseUnitTypeId.class.getSimpleName(), courseUnitTypeId.name()));
	}

	@Nonnull
	public List<CourseUnitDownloadableFileWithFileDetails> findCourseUnitDownloadableFiles(@Nullable UUID courseUnitId) {
		if (courseUnitId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT vcu.*
				FROM v_course_unit_downloadable_file vcu
				WHERE vcu.course_unit_id = ?
				ORDER BY vcu.display_order
				""", CourseUnitDownloadableFileWithFileDetails.class, courseUnitId);
	}

	@Nonnull
	public Optional<CourseUnitType> findCourseUnitTypeById(@Nullable CourseUnitTypeId courseUnitTypeId) {
		if (courseUnitTypeId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM course_unit_type
				WHERE course_unit_type_id=?
				""", CourseUnitType.class, courseUnitTypeId);
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}
}