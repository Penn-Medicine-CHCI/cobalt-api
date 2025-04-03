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

import com.cobaltplatform.api.model.service.CourseUnitDownloadableFileWithFileDetails;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class CourseUnitDownloadableFileApiResponse {
	@Nullable
	private UUID courseUnitDownloadableFileId;
	@Nullable
	private UUID courseUnitId;
	@Nullable
	private UUID fileUploadId;
	@Nullable
	private Integer displayOrder;
	@Nullable
	private String url;
	@Nullable
	private String filename;
	@Nullable
	private String contentType;
	@Nullable
	private Long filesize;
	@Nullable
	private String filesizeDescription;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface CourseUnitDownloadableFileApiResponseFactory {
		@Nonnull
		CourseUnitDownloadableFileApiResponse create(@Nonnull CourseUnitDownloadableFileWithFileDetails courseUnitDownloadableFileWithFileDetails);
	}

	@AssistedInject
	public CourseUnitDownloadableFileApiResponse(@Assisted @Nonnull CourseUnitDownloadableFileWithFileDetails courseUnitDownloadableFileWithFileDetails,
																							 @Nonnull Formatter formatter,
																							 @Nonnull Strings strings) {
		requireNonNull(courseUnitDownloadableFileWithFileDetails);
		requireNonNull(formatter);
		requireNonNull(strings);

		this.courseUnitDownloadableFileId = courseUnitDownloadableFileWithFileDetails.getCourseUnitDownloadableFileId();
		this.courseUnitId = courseUnitDownloadableFileWithFileDetails.getCourseUnitId();
		this.fileUploadId = courseUnitDownloadableFileWithFileDetails.getFileUploadId();
		this.displayOrder = courseUnitDownloadableFileWithFileDetails.getDisplayOrder();
		this.url = courseUnitDownloadableFileWithFileDetails.getUrl();
		this.filename = courseUnitDownloadableFileWithFileDetails.getFilename();
		this.contentType = courseUnitDownloadableFileWithFileDetails.getContentType();
		this.filesize = courseUnitDownloadableFileWithFileDetails.getFilesize();
		this.filesizeDescription = formatter.formatFilesize(courseUnitDownloadableFileWithFileDetails.getFilesize());

	}

	@Nullable
	public UUID getCourseUnitDownloadableFileId() {
		return courseUnitDownloadableFileId;
	}

	@Nullable
	public UUID getCourseUnitId() {
		return courseUnitId;
	}

	@Nullable
	public UUID getFileUploadId() {
		return fileUploadId;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return displayOrder;
	}

	@Nullable
	public String getUrl() {
		return url;
	}

	@Nullable
	public String getFilename() {
		return filename;
	}

	@Nullable
	public String getContentType() {
		return contentType;
	}

	@Nullable
	public Long getFilesize() {
		return filesize;
	}

	@Nullable
	public String getFilesizeDescription() {
		return filesizeDescription;
	}


}