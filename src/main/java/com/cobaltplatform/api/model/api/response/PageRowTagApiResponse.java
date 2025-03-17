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
import com.cobaltplatform.api.model.db.Tag;
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

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PageRowTagApiResponse {
	@Nonnull
	private final ColorId tagGroupColorId;
	@Nonnull
	private final String tagId;
	@Nonnull
	private final String tagGroupId;
	@Nonnull
	private final String name;
	@Nonnull
	private final String urlName;
	@Nonnull
	private final String description;
	@Nonnull
	private final Boolean deprecated;

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

		Tag tag = tagService.findTagById(pageRowTag.getTagId()).get();

		this.tagId = tag.getTagId();
		this.tagGroupId = tag.getTagGroupId();
		this.name = tag.getName();
		this.urlName = tag.getUrlName();
		this.description = tag.getDescription();
		this.deprecated = tag.getDeprecated();

		Optional<TagGroup> tagGroup = tagService.findUncachedTagGroupByTagId(tagId);

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
	public String getTagId() {
		return tagId;
	}

	@Nonnull
	public String getTagGroupId() {
		return tagGroupId;
	}

	@Nonnull
	public String getName() {
		return name;
	}

	@Nonnull
	public String getUrlName() {
		return urlName;
	}

	@Nonnull
	public String getDescription() {
		return description;
	}

	@Nonnull
	public Boolean getDeprecated() {
		return deprecated;
	}
}


