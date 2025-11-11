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
import com.cobaltplatform.api.model.db.MailingListEntry;
import com.cobaltplatform.api.model.db.MailingListEntryType.MailingListEntryTypeId;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.format.FormatStyle;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class MailingListEntryApiResponse {
	@Nonnull
	private final UUID mailingListEntryId;
	@Nonnull
	private final MailingListEntryTypeId mailingListEntryTypeId;
	@Nonnull
	private final UUID mailingListId;
	@Nonnull
	private final UUID accountId;
	@Nonnull
	private final UUID createdByAccountId;
	@Nonnull
	private final String value;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final Instant lastUpdated;
	@Nonnull
	private final String lastUpdatedDescription;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface MailingListEntryApiResponseFactory {
		@Nonnull
		MailingListEntryApiResponse create(@Nonnull MailingListEntry mailingListEntry);
	}

	@AssistedInject
	public MailingListEntryApiResponse(@Nonnull Provider<CurrentContext> currentContextProvider,
																		 @Nonnull Formatter formatter,
																		 @Assisted @Nonnull MailingListEntry mailingListEntry) {
		requireNonNull(currentContextProvider);
		requireNonNull(formatter);
		requireNonNull(mailingListEntry);

		this.mailingListEntryId = mailingListEntry.getMailingListEntryId();
		this.mailingListEntryTypeId = mailingListEntry.getMailingListEntryTypeId();
		this.mailingListId = mailingListEntry.getMailingListId();
		this.accountId = mailingListEntry.getAccountId();
		this.createdByAccountId = mailingListEntry.getCreatedByAccountId();
		this.value = mailingListEntry.getValue();
		this.created = mailingListEntry.getCreated();
		this.createdDescription = formatter.formatTimestamp(mailingListEntry.getCreated(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.lastUpdated = mailingListEntry.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(mailingListEntry.getLastUpdated(), FormatStyle.MEDIUM, FormatStyle.SHORT);
	}

	@Nonnull
	public UUID getMailingListEntryId() {
		return this.mailingListEntryId;
	}

	@Nonnull
	public MailingListEntryTypeId getMailingListEntryTypeId() {
		return this.mailingListEntryTypeId;
	}

	@Nonnull
	public UUID getMailingListId() {
		return this.mailingListId;
	}

	@Nonnull
	public UUID getAccountId() {
		return this.accountId;
	}

	@Nonnull
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	@Nonnull
	public String getValue() {
		return this.value;
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
}