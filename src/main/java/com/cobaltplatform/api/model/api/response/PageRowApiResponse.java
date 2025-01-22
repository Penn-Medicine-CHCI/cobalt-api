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
import com.cobaltplatform.api.model.db.PageRowContent;
import com.cobaltplatform.api.model.db.PageRowGroupSession;
import com.cobaltplatform.api.model.db.PageRowTagGroup;
import com.cobaltplatform.api.model.db.RowType.RowTypeId;
import com.cobaltplatform.api.model.api.response.PageRowCustomOneColumnApiResponse.PageCustomOneColumnApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowCustomTwoColumnApiResponse.PageCustomTwoColumnApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowCustomThreeColumnApiResponse.PageCustomThreeColumnApiResponseFactory;
import com.cobaltplatform.api.service.PageService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PageRowApiResponse {
	@Nonnull
	private final UUID pageRowId;
	@Nonnull
	private final UUID pageSectionId;
	@Nonnull
	private final RowTypeId rowTypeId;
	@Nonnull
	private final Integer displayOrder;
	@Nonnull
	private  List<PageRowContent> contents;
	@Nullable
	private  List<PageRowGroupSession> groupSessions;
	@Nonnull
	private PageRowColumn columnOne;
	@Nonnull
	private PageRowColumn columnTwo;
	@Nonnull
	private PageRowColumn columnThree;
	@Nullable
	private  PageRowTagGroup tagGroup;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PageRowApiResponseFactory {
		@Nonnull
		PageRowApiResponse create(@Nonnull PageRow pageRow);
	}

	@AssistedInject
	public PageRowApiResponse(@Nonnull Formatter formatter,
														@Nonnull Strings strings,
														@Assisted @Nonnull PageRow pageRow,
														@Nonnull PageService pageService,
														@Nonnull PageCustomOneColumnApiResponseFactory pageCustomOneColumnApiResponseFactory,
														@Nonnull PageCustomTwoColumnApiResponseFactory pageCustomTwoColumnApiResponseFactory,
														@Nonnull PageCustomThreeColumnApiResponseFactory pageCustomThreeColumnApiResponseFactory) {

		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(pageRow);
		requireNonNull(pageCustomOneColumnApiResponseFactory);
		requireNonNull(pageCustomTwoColumnApiResponseFactory);
		requireNonNull(pageCustomThreeColumnApiResponseFactory);

		this.pageRowId = pageRow.getPageRowId();
		this.pageSectionId = pageRow.getPageSectionId();
		this.rowTypeId = pageRow.getRowTypeId();
		this.displayOrder = pageRow.getDisplayOrder();

		if (this.rowTypeId.equals(RowTypeId.RESOURCES))
			this.contents = pageService.findPageRowContentByPageRowId(pageRow.getPageRowId());
		else if (this.rowTypeId.equals(RowTypeId.GROUP_SESSIONS))
			this.groupSessions = pageService.findPageRowGroupSessionByPageRowId(pageRow.getPageRowId());
		else if (this.rowTypeId.equals(RowTypeId.TAG_GROUP))
			this.tagGroup = pageService.findPageRowTagGroupByRowId(pageRow.getPageRowId()).orElse(null);
		else if (this.rowTypeId.equals(RowTypeId.ONE_COLUMN_IMAGE))
			this.columnOne = pageService.findPageRowColumnByPageRowIdAndDisplayOrder(pageRow.getPageRowId(), 0).orElse(null);
		else if (this.rowTypeId.equals(RowTypeId.TWO_COLUMN_IMAGE)) {
			this.columnOne = pageService.findPageRowColumnByPageRowIdAndDisplayOrder(pageRow.getPageRowId(), 0).orElse(null);
			this.columnTwo = pageService.findPageRowColumnByPageRowIdAndDisplayOrder(pageRow.getPageRowId(), 1).orElse(null);
		} else if (this.rowTypeId.equals(RowTypeId.THREE_COLUMN_IMAGE)) {
			this.columnOne = pageService.findPageRowColumnByPageRowIdAndDisplayOrder(pageRow.getPageRowId(), 0).orElse(null);
			this.columnTwo = pageService.findPageRowColumnByPageRowIdAndDisplayOrder(pageRow.getPageRowId(), 1).orElse(null);
			this.columnThree = pageService.findPageRowColumnByPageRowIdAndDisplayOrder(pageRow.getPageRowId(), 2).orElse(null);
		}
	}

	@Nonnull
	public UUID getPageRowId() {
		return pageRowId;
	}

	@Nonnull
	public UUID getPageSectionId() {
		return pageSectionId;
	}

	@Nonnull
	public RowTypeId getRowTypeId() {
		return rowTypeId;
	}

	@Nonnull
	public Integer getDisplayOrder() {
		return displayOrder;
	}

	@Nonnull
	public List<PageRowContent> getContents() {
		return contents;
	}

	@Nullable
	public List<PageRowGroupSession> getGroupSessions() {
		return groupSessions;
	}

	@Nullable
	public PageRowTagGroup getTagGroup() {
		return tagGroup;
	}

	@Nonnull
	public PageRowColumn getColumnOne() {
		return columnOne;
	}

	@Nonnull
	public PageRowColumn getColumnTwo() {
		return columnTwo;
	}

	@Nonnull
	public PageRowColumn getColumnThree() {
		return columnThree;
	}
}


