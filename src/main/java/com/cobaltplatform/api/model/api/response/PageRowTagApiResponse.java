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
import com.cobaltplatform.api.model.db.RowType;
import com.cobaltplatform.api.model.db.Tag;
import com.cobaltplatform.api.model.db.TagGroup;
import com.cobaltplatform.api.model.api.response.TagApiResponse.TagApiResponseFactory;
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
	private final ColorId tagGroupColorId;
	@Nonnull
	private final UUID pageRowId;
	@Nonnull
	private final Integer displayOrder;
	@Nonnull
	private final RowType.RowTypeId rowTypeId;

	@Nonnull
	private final TagApiResponse tag;

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
															 @Nonnull TagService tagService,
															 @Nonnull TagApiResponseFactory tagApiResponseFactory) {

		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(pageRow);
		requireNonNull(pageRowTag);
		requireNonNull(tagService);
		requireNonNull(tagApiResponseFactory);

		Optional<TagGroup> tagGroup = tagService.findUncachedTagGroupByTagId(pageRowTag.getTagId());

		this.pageRowId = pageRow.getPageRowId();
		this.displayOrder = pageRow.getDisplayOrder();
		this.rowTypeId = pageRow.getRowTypeId();
		this.tag = tagApiResponseFactory.create(tagService.findTagById(pageRowTag.getTagId()).get());

		if (tagGroup.isPresent())
			this.tagGroupColorId = tagGroup.get().getColorId();
		else
			this.tagGroupColorId = ColorId.NEUTRAL;

	}


	@Nonnull
	public ColorId getTagGroupColorId() {
		return tagGroupColorId;
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
	public RowType.RowTypeId getRowTypeId() {
		return rowTypeId;
	}

	@Nonnull
	public TagApiResponse getTag() {
		return tag;
	}
}


