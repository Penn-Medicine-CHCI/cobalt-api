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


import com.cobaltplatform.api.model.api.response.PageSectionApiResponse.PageSectionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageSiteLocationApiResponse.PageSiteLocationApiResponseFactory;
import com.cobaltplatform.api.model.db.Page;
import com.cobaltplatform.api.model.db.PageStatus.PageStatusId;
import com.cobaltplatform.api.service.PageService;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
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
	private final String relativeUrl;
	@Nullable
	private final PageStatusId pageStatusId;
	@Nullable
	private final String headline;
	@Nullable
	private final String description;
	@Nullable
	private final UUID imageFileUploadId;
	@Nullable
	private final String imageAltText;
	@Nullable
	private final String imageUrl;
	@Nullable
	private final LocalDate publishedDate;
	@Nullable
	private final String publishedDateDescription;
	@Nullable
	private final Instant created;
	@Nullable
	private final String createdDescription;
	@Nullable
	private final Instant lastUpdated;
	@Nullable
	private final String lastUpdatedDescription;
	@Nullable
	private final Boolean editingLivePage;
	@Nullable
	private final List<PageSectionApiResponse> pageSections;
	@Nullable
	private final List<PageSiteLocationApiResponse> livePageSiteLocations;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PageApiResponseFactory {
		@Nonnull
		PageApiResponse create(@Nonnull Page page,
													 @Nonnull Boolean includeDetails);
	}

	@AssistedInject
	public PageApiResponse(@Nonnull Formatter formatter,
												 @Nonnull Strings strings,
												 @Assisted @Nonnull Page page,
												 @Assisted @Nonnull Boolean includeDetails,
												 @Nonnull PageService pageService,
												 @Nonnull PageSectionApiResponseFactory pageSectionApiResponseFactory,
												 @Nonnull PageSiteLocationApiResponseFactory pageSiteLocationApiResponseFactory) {

		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(page);
		requireNonNull(pageService);
		requireNonNull(pageSectionApiResponseFactory);
		requireNonNull(pageSiteLocationApiResponseFactory);

		this.pageId = page.getPageId();
		this.name = page.getName();
		this.urlName = page.getUrlName();
		this.relativeUrl = format("/%s/%s", "pages", page.getUrlName());
		this.pageStatusId = page.getPageStatusId();
		this.headline = page.getHeadline();
		this.description = page.getDescription();
		this.imageFileUploadId = page.getImageFileUploadId();
		this.imageAltText = page.getImageAltText();
		this.imageUrl = page.getImageUrl();
		this.publishedDate = page.getPublishedDate();
		this.publishedDateDescription = this.publishedDate == null ? "Not Published" : formatter.formatDate(this.publishedDate, FormatStyle.MEDIUM);
		//Here we return the created date of the first "version" of this page
		this.created = page.getOriginalCreateDate();
		this.createdDescription = formatter.formatTimestamp(page.getOriginalCreateDate(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.lastUpdated = page.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(page.getLastUpdated(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.editingLivePage = page.getPageStatusId().equals(PageStatusId.COPY_FOR_EDITING);

		if (includeDetails) {
			this.pageSections = pageService.findPageSectionsByPageId(page.getPageId(), page.getInstitutionId())
					.stream().map(pageSection -> pageSectionApiResponseFactory.create(pageSection)).filter(apiResp -> apiResp.getDisplaySection()).collect(Collectors.toList());
			this.livePageSiteLocations = pageService.findLivePageSiteLocationsByPageId(page.getPageId(), page.getInstitutionId()).stream()
					.map(pageSiteLocation -> pageSiteLocationApiResponseFactory.create(pageSiteLocation))
					.collect(Collectors.toList());
		} else {
			this.pageSections = List.of();
			this.livePageSiteLocations = List.of();
		}
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
	public PageStatusId getPageStatusId() {
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

	@Nullable
	public Instant getCreated() {
		return created;
	}

	@Nullable
	public String getCreatedDescription() {
		return createdDescription;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	@Nullable
	public String getLastUpdatedDescription() {
		return lastUpdatedDescription;
	}

	@Nullable
	public List<PageSectionApiResponse> getPageSections() {
		return pageSections;
	}

	@Nullable
	public String getImageUrl() {
		return imageUrl;
	}

	@Nullable
	public String getRelativeUrl() {
		return relativeUrl;
	}

	@Nullable
	public Boolean getEditingLivePage() {
		return this.editingLivePage;
	}

	@Nullable
	public List<PageSiteLocationApiResponse> getLivePageSiteLocations() {
		return this.livePageSiteLocations;
	}
}


