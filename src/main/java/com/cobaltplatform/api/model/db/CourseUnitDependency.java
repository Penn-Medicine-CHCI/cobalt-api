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

package com.cobaltplatform.api.model.db;

import com.cobaltplatform.api.model.db.CourseUnitDependencyType.CourseUnitDependencyTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CourseUnitDependency {
	@Nullable
	private UUID determinantCourseUnitId;
	@Nullable
	private UUID dependentCourseUnitId;
	@Nullable
	private CourseUnitDependencyTypeId courseUnitDependencyTypeId;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getDeterminantCourseUnitId() {
		return this.determinantCourseUnitId;
	}

	public void setDeterminantCourseUnitId(@Nullable UUID determinantCourseUnitId) {
		this.determinantCourseUnitId = determinantCourseUnitId;
	}

	@Nullable
	public UUID getDependentCourseUnitId() {
		return this.dependentCourseUnitId;
	}

	public void setDependentCourseUnitId(@Nullable UUID dependentCourseUnitId) {
		this.dependentCourseUnitId = dependentCourseUnitId;
	}

	@Nullable
	public CourseUnitDependencyTypeId getCourseUnitDependencyTypeId() {
		return this.courseUnitDependencyTypeId;
	}

	public void setCourseUnitDependencyTypeId(@Nullable CourseUnitDependencyTypeId courseUnitDependencyTypeId) {
		this.courseUnitDependencyTypeId = courseUnitDependencyTypeId;
	}

	@Nullable
	public Instant getCreated() {
		return this.created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}