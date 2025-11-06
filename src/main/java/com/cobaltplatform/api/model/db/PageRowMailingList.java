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

package com.cobaltplatform.api.model.db;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PageRowMailingList {
	@Nullable
	private UUID pageRowMailingListId;
	@Nullable
	private UUID mailingListId;
	@Nullable
	private String title;
	@Nullable
	private String description;

	@Nullable
	public UUID getPageRowMailingListId() {
		return this.pageRowMailingListId;
	}

	public void setPageRowMailingListId(@Nullable UUID pageRowMailingListId) {
		this.pageRowMailingListId = pageRowMailingListId;
	}

	@Nullable
	public UUID getMailingListId() {
		return this.mailingListId;
	}

	public void setMailingListId(@Nullable UUID mailingListId) {
		this.mailingListId = mailingListId;
	}

	@Nullable
	public String getTitle() {
		return this.title;
	}

	public void setTitle(@Nullable String title) {
		this.title = title;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}
}