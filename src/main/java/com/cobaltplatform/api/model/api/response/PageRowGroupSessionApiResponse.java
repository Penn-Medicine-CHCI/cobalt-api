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
import com.cobaltplatform.api.model.db.RowType.RowTypeId;
import com.cobaltplatform.api.model.api.response.GroupSessionApiResponse.GroupSessionApiResponseFactory;
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
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PageRowGroupSessionApiResponse {
	@Nullable
	private UUID pageRowId;
	@Nullable
	private Integer displayOrder;
	@Nonnull
	private final RowTypeId rowTypeId;
	@Nullable
	private final List<GroupSessionApiResponse> groupSessions;
	@Nullable
	private GroupSessionApiResponseFactory groupSessionApiResponseFactory;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PageRowGroupSessionApiResponseFactory {
		@Nonnull
		PageRowGroupSessionApiResponse create(@Nonnull PageRow pageRow);
	}

	@AssistedInject
	public PageRowGroupSessionApiResponse(@Nonnull Formatter formatter,
																				@Nonnull Strings strings,
																				@Assisted @Nonnull PageRow pageRow,
																				@Nonnull GroupSessionApiResponseFactory groupSessionApiResponseFactory,
																				@Nonnull PageService pageService) {

		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(pageRow);
		requireNonNull(pageService);
		requireNonNull(groupSessionApiResponseFactory);

		this.pageRowId = pageRow.getPageRowId();
		this.groupSessions = pageService.findGroupSessionsByPageRowId(pageRow.getPageRowId()).stream()
				.map(groupSession -> groupSessionApiResponseFactory.create(groupSession)).collect(Collectors.toList());
		this.displayOrder = pageRow.getDisplayOrder();
		this.rowTypeId = pageRow.getRowTypeId();

}

	@Nullable
	public UUID getPageRowId() {
		return pageRowId;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return displayOrder;
	}

	@Nullable
	public List<GroupSessionApiResponse> getGroupSessions() {
		return groupSessions;
	}

	@Nonnull
	public RowTypeId getRowTypeId() {
		return rowTypeId;
	}
}


