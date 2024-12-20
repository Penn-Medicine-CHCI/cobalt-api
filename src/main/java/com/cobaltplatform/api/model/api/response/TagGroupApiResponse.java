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

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.response.TagApiResponse.TagApiResponseFactory;
import com.cobaltplatform.api.model.db.Color.ColorId;
import com.cobaltplatform.api.model.db.TagGroup;
import com.cobaltplatform.api.service.TagService;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class TagGroupApiResponse {
	@Nonnull
	private final String tagGroupId;
	@Nonnull
	private final ColorId colorId;
	@Nonnull
	private final String name;
	@Nonnull
	private final String urlName;
	@Nonnull
	private final String description;
	@Nonnull
	private final Boolean deprecated;
	@Nonnull
	private final List<TagApiResponse> tags;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface TagGroupApiResponseFactory {
		@Nonnull
		TagGroupApiResponse create(@Nonnull TagGroup tagGroup);

		@Nonnull
		TagGroupApiResponse create(@Nonnull TagGroup tagGroup,
															 @Nonnull Boolean includeDeprecatedTags);
	}

	@AssistedInject
	public TagGroupApiResponse(@Nonnull TagService tagService,
														 @Nonnull TagApiResponseFactory tagApiResponseFactory,
														 @Nonnull Provider<CurrentContext> currentContextProvider,
														 @Assisted @Nonnull TagGroup tagGroup) {
		this(tagService, tagApiResponseFactory, currentContextProvider, tagGroup, true);
	}

	@AssistedInject
	public TagGroupApiResponse(@Nonnull TagService tagService,
														 @Nonnull TagApiResponseFactory tagApiResponseFactory,
														 @Nonnull Provider<CurrentContext> currentContextProvider,
														 @Assisted @Nonnull TagGroup tagGroup,
														 @Assisted @Nonnull Boolean includeDeprecatedTags) {
		requireNonNull(tagService);
		requireNonNull(tagApiResponseFactory);
		requireNonNull(currentContextProvider);
		requireNonNull(tagGroup);
		requireNonNull(includeDeprecatedTags);

		this.tagGroupId = tagGroup.getTagGroupId();
		this.colorId = tagGroup.getColorId();
		this.name = tagGroup.getName();
		this.urlName = tagGroup.getUrlName();
		this.description = tagGroup.getDescription();
		this.deprecated = tagGroup.getDeprecated();
		this.tags = tagService.findTagsByTagGroupId(tagGroup.getTagGroupId()).stream()
				.filter(tag -> includeDeprecatedTags ? true : !tag.getDeprecated())
				.map(tag -> tagApiResponseFactory.create(tag))
				.collect(Collectors.toList());
	}

	@Nonnull
	public String getTagGroupId() {
		return this.tagGroupId;
	}

	@Nonnull
	public ColorId getColorId() {
		return this.colorId;
	}

	@Nonnull
	public String getName() {
		return this.name;
	}

	@Nonnull
	public String getUrlName() {
		return this.urlName;
	}

	@Nonnull
	public String getDescription() {
		return this.description;
	}

	@Nonnull
	public Boolean getDeprecated() {
		return this.deprecated;
	}

	@Nonnull
	public List<TagApiResponse> getTags() {
		return this.tags;
	}
}