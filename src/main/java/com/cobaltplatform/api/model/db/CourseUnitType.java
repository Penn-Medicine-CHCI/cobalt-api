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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import com.cobaltplatform.api.model.db.UnitCompletionType.UnitCompletionTypeId;

import static java.lang.String.format;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class CourseUnitType {
	@Nullable
	private CourseUnitTypeId courseUnitTypeId;
	@Nullable
	private String description;

	@Nullable
	private UnitCompletionTypeId unitCompletionTypeId;

	@Nullable
	private Boolean showRestartActivityWhenComplete;
	@Nullable
	private Boolean showUnitAsComplete;
	public enum CourseUnitTypeId {
		VIDEO,
		INFOGRAPHIC,
		HOMEWORK,
		CARD_SORT,
		QUIZ,
		REORDER,
		THINGS_TO_SHARE
	}

	@Override
	public String toString() {
		return format("%s{courseUnitTypeId=%s, description=%s}", getClass().getSimpleName(), getCourseUnitTypeId().name(), getDescription());
	}

	@Nullable
	public CourseUnitTypeId getCourseUnitTypeId() {
		return this.courseUnitTypeId;
	}

	public void setCourseUnitTypeId(@Nullable CourseUnitTypeId courseUnitTypeId) {
		this.courseUnitTypeId = courseUnitTypeId;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public UnitCompletionTypeId getUnitCompletionTypeId() {
		return unitCompletionTypeId;
	}

	public void setUnitCompletionTypeId(@Nullable UnitCompletionTypeId unitCompletionTypeId) {
		this.unitCompletionTypeId = unitCompletionTypeId;
	}

	@Nullable
	public Boolean getShowRestartActivityWhenComplete() {
		return showRestartActivityWhenComplete;
	}

	public void setShowRestartActivityWhenComplete(@Nullable Boolean showRestartActivityWhenComplete) {
		this.showRestartActivityWhenComplete = showRestartActivityWhenComplete;
	}

	@Nullable
	public Boolean getShowUnitAsComplete() {
		return showUnitAsComplete;
	}

	public void setShowUnitAsComplete(@Nullable Boolean showUnitAsComplete) {
		this.showUnitAsComplete = showUnitAsComplete;
	}
}