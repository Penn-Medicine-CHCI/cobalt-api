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

import com.cobaltplatform.api.model.db.PageRowContent;
import com.cobaltplatform.api.model.db.PageRowTagGroup;
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
public class PageRowTagGroupApiResponse {
	@Nonnull
	private final UUID pageRowTagGroupId;
	@Nonnull
	private final UUID pageRowId;
	@Nonnull
	private final String tagGroupId;
	@Nullable
	private final Integer displayOrder;


	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PageRowTagGroupApiResponseFactory {
		@Nonnull
		PageRowTagGroupApiResponse create(@Nonnull PageRowTagGroup pageRowTagGroup);
	}

	@AssistedInject
	public PageRowTagGroupApiResponse(@Nonnull Formatter formatter,
																		@Nonnull Strings strings,
																		@Assisted @Nonnull PageRowTagGroup pageRowTagGroup) {

		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(pageRowTagGroup);

		this.pageRowTagGroupId = pageRowTagGroup.getPageRowTagGroupId();
		this.pageRowId = pageRowTagGroup.getPageRowId();
		this.tagGroupId = pageRowTagGroup.getTagGroupId();
		this.displayOrder = pageRowTagGroup.getDisplayOrder();
}

	@Nonnull
	public UUID getPageRowTagGroupId() {
		return pageRowTagGroupId;
	}

	@Nonnull
	public UUID getPageRowId() {
		return pageRowId;
	}

	@Nonnull
	public String getTagGroupId() {
		return tagGroupId;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return displayOrder;
	}
}


