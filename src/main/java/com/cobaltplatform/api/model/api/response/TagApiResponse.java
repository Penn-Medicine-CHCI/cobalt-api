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
import com.cobaltplatform.api.model.db.Tag;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class TagApiResponse {
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
	public interface TagApiResponseFactory {
		@Nonnull
		TagApiResponse create(@Nonnull Tag tag);
	}

	@AssistedInject
	public TagApiResponse(@Nonnull Provider<CurrentContext> currentContextProvider,
												@Assisted @Nonnull Tag tag) {
		requireNonNull(currentContextProvider);
		requireNonNull(tag);

		this.tagId = tag.getTagId();
		this.tagGroupId = tag.getTagGroupId();
		this.name = tag.getName();
		this.urlName = tag.getUrlName();
		this.description = tag.getDescription();
		this.deprecated = tag.getDeprecated();
	}

	@Nonnull
	public String getTagId() {
		return this.tagId;
	}

	@Nonnull
	public String getTagGroupId() {
		return this.tagGroupId;
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
}