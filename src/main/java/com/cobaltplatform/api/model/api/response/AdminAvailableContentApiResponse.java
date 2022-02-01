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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.ContentAction;
import com.cobaltplatform.api.model.db.ContentType.ContentTypeId;
import com.cobaltplatform.api.model.service.AdminContent;
import com.cobaltplatform.api.service.ContentService;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDate;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AdminAvailableContentApiResponse {
	@Nullable
	private UUID contentId;
	@Nullable
	private LocalDate dateCreated;
	@Nullable
	private String dateCreatedDescription;
	@Nullable
	private ContentTypeId contentTypeId;
	@Nullable
	private String title;
	@Nullable
	private String author;
	@Nullable
	private String ownerInstitution;
	@Nullable
	private List<ContentAction.ContentActionId> actions;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface AdminAvailableContentApiResponseFactory {
		@Nonnull
		AdminAvailableContentApiResponse create(@Nonnull Account account,
																						@Nonnull AdminContent adminContent);
	}

	@AssistedInject
	public AdminAvailableContentApiResponse(@Nonnull Formatter formatter,
																					@Nonnull ContentService contentService,
																					@Assisted @Nonnull Account account,
																					@Assisted @Nonnull AdminContent adminContent) {
		requireNonNull(formatter);
		requireNonNull(adminContent);
		requireNonNull(contentService);
		requireNonNull(account);

		List<ContentAction.ContentActionId> contentActionIdList = new ArrayList<>();

		this.contentId = adminContent.getContentId();
		this.dateCreated = adminContent.getDateCreated();
		this.dateCreatedDescription = adminContent.getDateCreated() != null ? formatter.formatDate(adminContent.getDateCreated(), FormatStyle.SHORT) : null;
		this.contentTypeId = adminContent.getContentTypeId();
		this.title = adminContent.getTitle();
		this.author = adminContent.getAuthor();
		this.ownerInstitution = adminContent.getOwnerInstitution();

		if (adminContent.getApprovedFlag())
			contentActionIdList.add(ContentAction.ContentActionId.REMOVE);
		else
			contentActionIdList.add(ContentAction.ContentActionId.APPROVE);

		this.actions = contentActionIdList;

	}

}