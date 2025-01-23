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


import com.cobaltplatform.api.model.db.PageRow;
import com.cobaltplatform.api.model.db.PageRowContent;
import com.cobaltplatform.api.model.db.RowType.RowTypeId;
import com.cobaltplatform.api.model.api.response.ContentApiResponse.ContentApiResponseFactory;
import com.cobaltplatform.api.service.PageService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PageRowContentApiResponse {
	@Nonnull
	private final UUID pageRowId;
	@Nonnull
	private final Integer displayOrder;

	@Nonnull
	private final List<ContentApiResponse> contents;

	@Nonnull
	private final RowTypeId rowTypeId;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PageRowContentApiResponseFactory {
		@Nonnull
		PageRowContentApiResponse create(@Nonnull PageRow pageRow);
	}

	@AssistedInject
	public PageRowContentApiResponse(@Nonnull Formatter formatter,
																	 @Nonnull Strings strings,
																	 @Assisted @Nonnull PageRow pageRow,
																	 @Nonnull ContentApiResponseFactory contentApiResponseFactory,
																	 @Nonnull PageService pageService) {

		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(pageRow);
		requireNonNull(pageService);
		requireNonNull(contentApiResponseFactory);

		this.pageRowId = pageRow.getPageRowId();
		this.contents = pageService.findContentByPageRowId(pageRow.getPageRowId()).stream()
				.map(content -> contentApiResponseFactory.create(content)).collect(Collectors.toList());
		this.displayOrder = pageRow.getDisplayOrder();
		this.rowTypeId = pageRow.getRowTypeId();
}


	@Nonnull
	public UUID getPageRowId() {
		return pageRowId;
	}

	@Nonnull
	public List<ContentApiResponse> getContents() {
		return contents;
	}

	@Nonnull
	public Integer getDisplayOrder() {
		return displayOrder;
	}

	@Nonnull
	public RowTypeId getRowTypeId() {
		return rowTypeId;
	}
}


