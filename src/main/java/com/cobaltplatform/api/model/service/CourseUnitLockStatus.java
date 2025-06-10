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

package com.cobaltplatform.api.model.service;

import com.cobaltplatform.api.model.db.CourseUnitDependencyType.CourseUnitDependencyTypeId;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class CourseUnitLockStatus {
	@Nonnull
	private final Map<CourseUnitDependencyTypeId, List<UUID>> determinantCourseUnitIdsByDependencyTypeIds;
	@Nonnull
	private final CourseUnitLockTypeId courseUnitLockTypeId;

	public CourseUnitLockStatus(@Nonnull Map<CourseUnitDependencyTypeId, List<UUID>> determinantCourseUnitIdsByDependencyTypeIds) {
		requireNonNull(determinantCourseUnitIdsByDependencyTypeIds);

		this.determinantCourseUnitIdsByDependencyTypeIds = Collections.unmodifiableMap(new HashMap<>(determinantCourseUnitIdsByDependencyTypeIds));

		CourseUnitLockTypeId courseUnitLockTypeId = CourseUnitLockTypeId.UNLOCKED;

		if (determinantCourseUnitIdsByDependencyTypeIds.containsKey(CourseUnitDependencyTypeId.STRONG))
			courseUnitLockTypeId = CourseUnitLockTypeId.STRONGLY_LOCKED;
		else if (determinantCourseUnitIdsByDependencyTypeIds.containsKey(CourseUnitDependencyTypeId.WEAK))
			courseUnitLockTypeId = CourseUnitLockTypeId.WEAKLY_LOCKED;

		this.courseUnitLockTypeId = courseUnitLockTypeId;
	}

	@Nonnull
	public Map<CourseUnitDependencyTypeId, List<UUID>> getDeterminantCourseUnitIdsByDependencyTypeIds() {
		return this.determinantCourseUnitIdsByDependencyTypeIds;
	}

	@Nonnull
	public CourseUnitLockTypeId getCourseUnitLockTypeId() {
		return this.courseUnitLockTypeId;
	}
}
