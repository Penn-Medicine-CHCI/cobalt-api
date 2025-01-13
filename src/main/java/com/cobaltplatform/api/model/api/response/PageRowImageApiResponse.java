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

import com.cobaltplatform.api.model.db.PageRowImage;
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
public class PageRowImageApiResponse {
	@Nullable
	private UUID pageRowImageId;
	@Nullable
	private UUID pageRowId;
	@Nullable
	private String headline;
	@Nullable
	private String description;
	@Nullable
	private UUID imageFileUploadId;
	@Nullable
	private String imageAltText;
	@Nullable
	private Integer displayOrder;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PageRowImageApiResponseFactory {
		@Nonnull
		PageRowImageApiResponse create(@Nonnull PageRowImage pageRowImage);
	}

	@AssistedInject
	public PageRowImageApiResponse(@Nonnull Formatter formatter,
																 @Nonnull Strings strings,
																 @Assisted @Nonnull PageRowImage pageRowImage) {

		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(pageRowImage);

		this.pageRowImageId = pageRowImage.getPageRowImageId();
		this.pageRowId = pageRowImage.getPageRowId();
		this.headline = pageRowImage.getHeadline();
		this.description = pageRowImage.getDescription();
		this.imageFileUploadId = pageRowImage.getImageFileUploadId();
		this.imageAltText = pageRowImage.getImageAltText();
		this.displayOrder = pageRowImage.getDisplayOrder();

}

	@Nullable
	public UUID getPageRowImageId() {
		return pageRowImageId;
	}

	@Nullable
	public UUID getPageRowId() {
		return pageRowId;
	}

	@Nullable
	public String getHeadline() {
		return headline;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	@Nullable
	public UUID getImageFileUploadId() {
		return imageFileUploadId;
	}

	@Nullable
	public String getImageAltText() {
		return imageAltText;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return displayOrder;
	}
}


