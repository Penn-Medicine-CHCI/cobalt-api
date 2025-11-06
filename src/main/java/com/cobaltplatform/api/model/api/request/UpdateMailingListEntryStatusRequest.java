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


import com.cobaltplatform.api.model.db.MailingListEntryStatus.MailingListEntryStatusId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class UpdateMailingListEntryStatusRequest {
	@Nullable
	private UUID mailingListEntryId;
	@Nullable
	private MailingListEntryStatusId mailingListEntryStatusId;

	@Nullable
	public UUID getMailingListEntryId() {
		return this.mailingListEntryId;
	}

	public void setMailingListEntryId(@Nullable UUID mailingListEntryId) {
		this.mailingListEntryId = mailingListEntryId;
	}

	@Nullable
	public MailingListEntryStatusId getMailingListEntryStatusId() {
		return this.mailingListEntryStatusId;
	}

	public void setMailingListEntryStatusId(@Nullable MailingListEntryStatusId mailingListEntryStatusId) {
		this.mailingListEntryStatusId = mailingListEntryStatusId;
	}
}
