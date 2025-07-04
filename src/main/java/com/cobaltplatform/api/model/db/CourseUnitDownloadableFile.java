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
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CourseUnitDownloadableFile {
	@Nullable
	private UUID courseUnitDownloadableFileId;
	@Nullable
	private UUID courseUnitId;
	@Nullable
	private UUID fileUploadId;
	@Nullable
	private Integer displayOrder;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getCourseUnitDownloadableFileId() {
		return this.courseUnitDownloadableFileId;
	}

	public void setCourseUnitDownloadableFileId(@Nullable UUID courseUnitDownloadableFileId) {
		this.courseUnitDownloadableFileId = courseUnitDownloadableFileId;
	}

	@Nullable
	public UUID getCourseUnitId() {
		return this.courseUnitId;
	}

	public void setCourseUnitId(@Nullable UUID courseUnitId) {
		this.courseUnitId = courseUnitId;
	}

	@Nullable
	public UUID getFileUploadId() {
		return this.fileUploadId;
	}

	public void setFileUploadId(@Nullable UUID fileUploadId) {
		this.fileUploadId = fileUploadId;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return this.displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
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