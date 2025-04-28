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

import com.cobaltplatform.api.model.db.SiteLocation;
import com.cobaltplatform.api.model.db.SiteLocation.SiteLocationId;
import com.cobaltplatform.api.model.service.PageSiteLocation;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PageSiteLocationApiResponse {
	@Nonnull
	private final UUID pageId;
	@Nonnull
	private final SiteLocationId siteLocationId;
	@Nullable
	private final String relativeUrl;
	@Nullable
	private final String headline;
	@Nullable
	private final String description;
	@Nullable
	private final String shortDescription;
	@Nullable
	private final String imageAltText;
	@Nullable
	private final String imageUrl;
	@Nullable
	private final String callToAction;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PageSiteLocationApiResponseFactory {
		@Nonnull
		PageSiteLocationApiResponse create(@Nonnull PageSiteLocation pageSiteLocation);
	}

	@AssistedInject
	public PageSiteLocationApiResponse(@Assisted @Nonnull PageSiteLocation pageSiteLocation) {
		requireNonNull(pageSiteLocation);

		this.pageId = pageSiteLocation.getPageId();
		this.siteLocationId = pageSiteLocation.getSiteLocationId();
		this.relativeUrl = format("/%s/%s", "pages", pageSiteLocation.getUrlName());
		this.headline = pageSiteLocation.getHeadline();
		this.description = pageSiteLocation.getDescription();
		this.shortDescription = pageSiteLocation.getShortDescription();
		this.imageAltText = pageSiteLocation.getImageAltText();
		this.imageUrl = pageSiteLocation.getImageUrl();
		this.callToAction = pageSiteLocation.getCallToAction();
	}

	@Nonnull
	public UUID getPageId() {
		return pageId;
	}

	@Nonnull
	public SiteLocationId getSiteLocationId() {
		return this.siteLocationId;
	}

	@Nullable
	public String getRelativeUrl() {
		return relativeUrl;
	}

	@Nullable
	public String getHeadline() {
		return headline;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	@Nonnull
	public Optional<String> getShortDescription() {
		return Optional.ofNullable(this.shortDescription);
	}

	@Nullable
	public String getImageAltText() {
		return imageAltText;
	}

	@Nullable
	public String getImageUrl() {
		return imageUrl;
	}

	@Nullable
	public String getCallToAction() {
		return callToAction;
	}
}