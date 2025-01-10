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

import com.cobaltplatform.api.model.db.BackgroundColor.BackgroundColorId;
import com.cobaltplatform.api.model.db.PageSection;
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
public class PageSectionApiResponse {
	@Nonnull
	private final UUID pageSectionId;
	@Nonnull
	private final UUID pageId;
	@Nullable
	private final String name;
	@Nullable
	private final String headline;
	@Nullable
	private final String description;
	@Nullable
	private final BackgroundColorId backgroundColorId;
	@Nonnull
	private Integer displayOrder;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PageSectionApiResponseFactory {
		@Nonnull
		PageSectionApiResponse create(@Nonnull PageSection pageSection);
	}

	@AssistedInject
	public PageSectionApiResponse(@Nonnull Formatter formatter,
																@Nonnull Strings strings,
																@Assisted @Nonnull PageSection pageSection) {

		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(pageSection);

		this.pageSectionId = pageSection.getPageSectionId();
		this.pageId = pageSection.getPageId();
		this.name = pageSection.getName();
		this.headline = pageSection.getHeadline();
		this.description = pageSection.getDescription();
		this.backgroundColorId = pageSection.getBackgroundColorId();
		this.displayOrder = pageSection.getDisplayOrder();
}

	@Nonnull
	public UUID getPageSectionId() {
		return pageSectionId;
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
	public String getHeadline() {
		return headline;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	@Nullable
	public BackgroundColorId getBackgroundColorId() {
		return backgroundColorId;
	}

	@Nonnull
	public Integer getDisplayOrder() {
		return displayOrder;
	}
}


