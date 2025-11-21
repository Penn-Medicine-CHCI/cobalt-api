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
import com.cobaltplatform.api.model.api.response.MailingListEntryApiResponse.MailingListEntryApiResponseFactory;
import com.cobaltplatform.api.model.db.MailingList;
import com.cobaltplatform.api.service.MailingListService;
import com.cobaltplatform.api.service.MailingListService.MailingListEntryStatusFilter;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class MailingListApiResponse {
	@Nonnull
	private final UUID mailingListId;
	@Nonnull
	private final UUID createdByAccountId;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final Instant lastUpdated;
	@Nonnull
	private final String lastUpdatedDescription;
	@Nonnull
	private final List<MailingListEntryApiResponse> mailingListEntries;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface MailingListApiResponseFactory {
		@Nonnull
		MailingListApiResponse create(@Nonnull MailingList mailingList);
	}

	@AssistedInject
	public MailingListApiResponse(@Nonnull MailingListService mailingListService,
																@Nonnull MailingListEntryApiResponseFactory mailingListEntryApiResponseFactory,
																@Nonnull Provider<CurrentContext> currentContextProvider,
																@Nonnull Formatter formatter,
																@Assisted @Nonnull MailingList mailingList) {
		requireNonNull(mailingListService);
		requireNonNull(mailingListEntryApiResponseFactory);
		requireNonNull(currentContextProvider);
		requireNonNull(formatter);
		requireNonNull(mailingList);

		this.mailingListId = mailingList.getMailingListId();
		this.createdByAccountId = mailingList.getCreatedByAccountId();
		this.created = mailingList.getCreated();
		this.createdDescription = formatter.formatTimestamp(mailingList.getCreated(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.lastUpdated = mailingList.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(mailingList.getLastUpdated(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.mailingListEntries = mailingListService.findMailingListEntriesByMailingListId(mailingList.getMailingListId(), MailingListEntryStatusFilter.SUBSCRIBED).stream()
				.map(mailingListEntry -> mailingListEntryApiResponseFactory.create(mailingListEntry))
				.collect(Collectors.toList());
	}

	@Nonnull
	public UUID getMailingListId() {
		return this.mailingListId;
	}

	@Nonnull
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	@Nonnull
	public Instant getCreated() {
		return this.created;
	}

	@Nonnull
	public String getCreatedDescription() {
		return this.createdDescription;
	}

	@Nonnull
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	@Nonnull
	public String getLastUpdatedDescription() {
		return this.lastUpdatedDescription;
	}

	@Nonnull
	public List<MailingListEntryApiResponse> getMailingListEntries() {
		return this.mailingListEntries;
	}
}