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

import com.cobaltplatform.api.model.api.response.MailingListEntryApiResponse.MailingListEntryApiResponseFactory;
import com.cobaltplatform.api.model.db.PageRow;
import com.cobaltplatform.api.model.db.PageRowMailingList;
import com.cobaltplatform.api.model.db.RowType.RowTypeId;
import com.cobaltplatform.api.service.MailingListService;
import com.cobaltplatform.api.service.MailingListService.MailingListEntryStatusFilter;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PageRowMailingListApiResponse {
	@Nonnull
	private final UUID pageRowId;
	@Nonnull
	private final Integer displayOrder;
	@Nonnull
	private final RowTypeId rowTypeId;
	@Nonnull
	private final UUID mailingListId;
	@Nonnull
	private final String title;
	@Nonnull
	private final String description;
	@Nullable
	private List<MailingListEntryApiResponse> mailingListEntries;

	public enum Supplement {
		MAILING_LIST_ENTRIES
	}

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PageRowMailingListApiResponseFactory {
		@Nonnull
		PageRowMailingListApiResponse create(@Nonnull PageRow pageRow,
																				 @Nonnull PageRowMailingList pageRowMailingList,
																				 @Nullable Supplement... supplements);
	}

	@AssistedInject
	public PageRowMailingListApiResponse(@Nonnull MailingListService mailingListService,
																			 @Nonnull MailingListEntryApiResponseFactory mailingListEntryApiResponseFactory,
																			 @Nonnull Formatter formatter,
																			 @Nonnull Strings strings,
																			 @Assisted @Nonnull PageRow pageRow,
																			 @Assisted @Nonnull PageRowMailingList pageRowMailingList,
																			 @Assisted @Nullable Supplement... supplements) {
		requireNonNull(mailingListService);
		requireNonNull(mailingListEntryApiResponseFactory);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(pageRow);
		requireNonNull(pageRowMailingList);

		Set<Supplement> supplementsAsSet = supplements == null ? Set.of() : new HashSet<>(Arrays.asList(supplements));

		this.pageRowId = pageRow.getPageRowId();
		this.displayOrder = pageRow.getDisplayOrder();
		this.rowTypeId = pageRow.getRowTypeId();
		this.mailingListId = pageRowMailingList.getMailingListId();
		this.title = pageRowMailingList.getTitle();
		this.description = pageRowMailingList.getDescription();

		this.mailingListEntries = supplementsAsSet.contains(Supplement.MAILING_LIST_ENTRIES)
				? mailingListService.findMailingListEntriesByMailingListId(getMailingListId(), MailingListEntryStatusFilter.SUBSCRIBED).stream()
				.map(mailingListEntry -> mailingListEntryApiResponseFactory.create(mailingListEntry))
				.collect(Collectors.toUnmodifiableList())
				: null;
	}

	@Nonnull
	public UUID getPageRowId() {
		return this.pageRowId;
	}

	@Nonnull
	public Integer getDisplayOrder() {
		return this.displayOrder;
	}

	@Nonnull
	public RowTypeId getRowTypeId() {
		return this.rowTypeId;
	}

	@Nonnull
	public UUID getMailingListId() {
		return this.mailingListId;
	}

	@Nonnull
	public String getTitle() {
		return this.title;
	}

	@Nonnull
	public String getDescription() {
		return this.description;
	}

	@Nullable
	public List<MailingListEntryApiResponse> getMailingListEntries() {
		return this.mailingListEntries;
	}
}


