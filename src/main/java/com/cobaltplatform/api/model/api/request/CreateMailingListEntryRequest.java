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

package com.cobaltplatform.api.model.api.request;

import com.cobaltplatform.api.model.db.MailingListEntryType.MailingListEntryTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class CreateMailingListEntryRequest {
	@Nullable
	private UUID mailingListId;
	@Nullable
	private MailingListEntryTypeId mailingListEntryTypeId;
	@Nullable
	private UUID accountId;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private String value;

	@Nullable
	public UUID getMailingListId() {
		return this.mailingListId;
	}

	public void setMailingListId(@Nullable UUID mailingListId) {
		this.mailingListId = mailingListId;
	}

	@Nullable
	public MailingListEntryTypeId getMailingListEntryTypeId() {
		return this.mailingListEntryTypeId;
	}

	public void setMailingListEntryTypeId(@Nullable MailingListEntryTypeId mailingListEntryTypeId) {
		this.mailingListEntryTypeId = mailingListEntryTypeId;
	}

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public String getValue() {
		return this.value;
	}

	public void setValue(@Nullable String value) {
		this.value = value;
	}
}
