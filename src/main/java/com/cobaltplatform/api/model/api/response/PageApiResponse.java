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

import com.cobaltplatform.api.model.api.response.ContentAudienceTypeApiResponse.ContentAudienceTypeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TagApiResponse.TagApiResponseFactory;
import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.db.ContentType.ContentTypeId;
import com.cobaltplatform.api.model.db.ContentVisibilityType.ContentVisibilityTypeId;
import com.cobaltplatform.api.model.db.Page;
import com.cobaltplatform.api.service.ContentService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PageApiResponse {
	@Nonnull
	private final UUID pageId;
	@Nullable
	private final String name;
	@Nullable
	private final String urlName;
	@Nullable
	private final String pageTypeId;
	@Nullable
	private final String pageStatusId;
	@Nullable
	private final String headline;
	@Nullable
	private final String description;
	@Nullable
	private final UUID imageFileUploadId;
	@Nullable
	private final String imageAltText;
	@Nullable
	private final LocalDate publishedDate;
	@Nullable
	private final String publishedDateDescription;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PageApiResponseFactory {
		@Nonnull
		PageApiResponse create(@Nonnull Page page);
	}

	@AssistedInject
	public PageApiResponse(@Nonnull Formatter formatter,
												 @Nonnull Strings strings,
												 @Assisted @Nonnull Page page) {

		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(page);

		this.pageId = page.getPageId();
		this.name = page.getName();
		this.urlName = page.getUrlName();
		this.pageTypeId = page.getPageTypeId();
		this.pageStatusId = page.getPageStatusId();
		this.headline = page.getHeadline();
		this.description = page.getDescription();
		this.imageFileUploadId = page.getImageFileUploadId();
		this.imageAltText = page.getImageAltText();
		this.publishedDate = page.getPublishedDate();
		this.publishedDateDescription = this.publishedDate == null ? "Not Published" : formatter.formatDate(this.publishedDate, FormatStyle.LONG);
}

	@Nonnull
	public UUID getPageId() {
		return pageId;
	}

	@Nullable
	public String getName() {
		return name;
	}

	@Nullable
	public String getUrlName() {
		return urlName;
	}

	@Nullable
	public String getPageTypeId() {
		return pageTypeId;
	}

	@Nullable
	public String getPageStatusId() {
		return pageStatusId;
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
	public LocalDate getPublishedDate() {
		return publishedDate;
	}

	@Nullable
	public String getPublishedDateDescription() {
		return publishedDateDescription;
	}
}


