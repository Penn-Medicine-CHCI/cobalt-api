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
import com.cobaltplatform.api.model.db.PageRowColumn;
import com.cobaltplatform.api.service.PageService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PageRowCustomOneColumnApiResponse {
	@Nonnull
	private final UUID pageRowId;
	@Nonnull
	private final Integer displayOrder;
	@Nonnull
	private final PageRowColumn columnOne;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PageCustomOneColumnApiResponseFactory {
		@Nonnull
		PageRowCustomOneColumnApiResponse create(@Nonnull PageRow pageRow);
	}

	@AssistedInject
	public PageRowCustomOneColumnApiResponse(@Nonnull Formatter formatter,
																					 @Nonnull Strings strings,
																					 @Assisted @Nonnull PageRow pageRow,
																					 @Nonnull PageService pageService) {

		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(pageRow);
		requireNonNull(pageService);

		this.pageRowId = pageRow.getPageRowId();
		this.columnOne = pageService.findPageRowImageByPageRowIdAndDisplayOrder(pageRow.getPageRowId(), 0).orElse(null);
		this.displayOrder = pageRow.getDisplayOrder();
}


	@Nonnull
	public UUID getPageRowId() {
		return pageRowId;
	}

	@Nonnull
	public PageRowColumn getColumnOne() {
		return columnOne;
	}

	@Nonnull
	public Integer getDisplayOrder() {
		return displayOrder;
	}

}


