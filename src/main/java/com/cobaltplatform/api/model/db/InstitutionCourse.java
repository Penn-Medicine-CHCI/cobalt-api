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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class InstitutionCourse {
	@Nullable
	private InstitutionId institutionId;

	@Nullable
	private UUID courseId;

	@Nullable
	private String urlName;

	@Nullable
	private Integer displayOrder;

	@Nullable
	private InstitutionCourseStatusId institutionCourseStatusId;

	public enum InstitutionCourseStatusId {
		COMING_SOON,
		AVAILABLE
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public UUID getCourseId() {
		return courseId;
	}

	public void setCourseId(@Nullable UUID courseId) {
		this.courseId = courseId;
	}

	@Nullable
	public String getUrlName() {
		return urlName;
	}

	public void setUrlName(@Nullable String urlName) {
		this.urlName = urlName;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	@Nullable
	public InstitutionCourseStatusId getInstitutionCourseStatusId() {
		return institutionCourseStatusId;
	}

	public void setInstitutionCourseStatusId(@Nullable InstitutionCourseStatusId institutionCourseStatusId) {
		this.institutionCourseStatusId = institutionCourseStatusId;
	}
}