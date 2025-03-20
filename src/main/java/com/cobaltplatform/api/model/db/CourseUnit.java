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

import com.cobaltplatform.api.model.db.CourseUnitType.CourseUnitTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CourseUnit {
	@Nullable
	private UUID courseUnitId;
	@Nullable
	private UUID courseModuleId;
	@Nullable
	private CourseUnitTypeId courseUnitTypeId;
	@Nullable
	private String title;
	@Nullable
	private String description;
	@Nullable
	private Integer estimatedCompletionTimeInMinutes;
	@Nullable
	private Integer displayOrder;
	@Nullable
	private UUID videoId; // Only applies to VIDEO course_unit_type_id
	@Nullable
	private UUID screeningFlowId; // Only applies to units that include questions and answers, e.g. QUIZ course_unit_type_id
	@Nullable
	private String imageUrl; // Only applies to units that include an embedded image, e.g. INFOGRAPHIC course_unit_type_id
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getCourseUnitId() {
		return this.courseUnitId;
	}

	public void setCourseUnitId(@Nullable UUID courseUnitId) {
		this.courseUnitId = courseUnitId;
	}

	@Nullable
	public UUID getCourseModuleId() {
		return this.courseModuleId;
	}

	public void setCourseModuleId(@Nullable UUID courseModuleId) {
		this.courseModuleId = courseModuleId;
	}

	@Nullable
	public CourseUnitTypeId getCourseUnitTypeId() {
		return this.courseUnitTypeId;
	}

	public void setCourseUnitTypeId(@Nullable CourseUnitTypeId courseUnitTypeId) {
		this.courseUnitTypeId = courseUnitTypeId;
	}

	@Nullable
	public String getTitle() {
		return this.title;
	}

	public void setTitle(@Nullable String title) {
		this.title = title;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public Integer getEstimatedCompletionTimeInMinutes() {
		return this.estimatedCompletionTimeInMinutes;
	}

	public void setEstimatedCompletionTimeInMinutes(@Nullable Integer estimatedCompletionTimeInMinutes) {
		this.estimatedCompletionTimeInMinutes = estimatedCompletionTimeInMinutes;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return this.displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	@Nullable
	public UUID getVideoId() {
		return this.videoId;
	}

	public void setVideoId(@Nullable UUID videoId) {
		this.videoId = videoId;
	}

	@Nullable
	public UUID getScreeningFlowId() {
		return this.screeningFlowId;
	}

	public void setScreeningFlowId(@Nullable UUID screeningFlowId) {
		this.screeningFlowId = screeningFlowId;
	}

	@Nullable
	public String getImageUrl() {
		return this.imageUrl;
	}

	public void setImageUrl(@Nullable String imageUrl) {
		this.imageUrl = imageUrl;
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