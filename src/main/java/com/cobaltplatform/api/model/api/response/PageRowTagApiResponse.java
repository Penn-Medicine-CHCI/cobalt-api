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

import com.cobaltplatform.api.model.db.Color.ColorId;
import com.cobaltplatform.api.model.db.PageRow;
import com.cobaltplatform.api.model.db.PageRowTag;
import com.cobaltplatform.api.model.db.RowType.RowTypeId;
import com.cobaltplatform.api.model.db.TagGroup;
import com.cobaltplatform.api.service.PageService;
import com.cobaltplatform.api.service.TagService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PageRowTagApiResponse {
	@Nonnull
	private final UUID pageRowId;
	@Nonnull
	private Integer displayOrder;
	@Nonnull
	private final RowTypeId rowTypeId;
	@Nonnull
	private ColorId tagGroupColorId;
	@Nonnull
	private String tagId;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PageRowTagApiResponseFactory {
		@Nonnull
		PageRowTagApiResponse create(@Nonnull PageRow pageRow,
																 @Nonnull PageRowTag pageRowTag);
	}

	@AssistedInject
	public PageRowTagApiResponse(@Nonnull Formatter formatter,
															 @Nonnull Strings strings,
															 @Assisted @Nonnull PageRow pageRow,
															 @Assisted @Nonnull PageRowTag pageRowTag,
															 @Nonnull TagGroupApiResponse.TagGroupApiResponseFactory tagGroupApiResponseFactory,
															 @Nonnull PageService pageService,
															 @Nonnull TagService tagService) {

		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(pageRow);
		requireNonNull(pageService);
		requireNonNull(tagGroupApiResponseFactory);
		requireNonNull(pageRowTag);
		requireNonNull(tagService);

		this.pageRowId = pageRow.getPageRowId();
		this.displayOrder = pageRow.getDisplayOrder();
		this.tagId = pageRowTag.getTagId();
		this.rowTypeId = pageRow.getRowTypeId();

		Optional<TagGroup> tagGroup = tagService.findUncachedTagGroupByTagId(tagId);

		if (tagGroup.isPresent())
			this.tagGroupColorId = tagGroup.get().getColorId();
		else
			this.tagGroupColorId = ColorId.NEUTRAL;

	}

	@Nonnull
	public UUID getPageRowId() {
		return pageRowId;
	}

	@Nonnull
	public Integer getDisplayOrder() {
		return displayOrder;
	}


	@Nonnull
	public String getTagId() {
		return tagId;
	}

	@Nonnull
	public RowTypeId getRowTypeId() {
		return rowTypeId;
	}


}


