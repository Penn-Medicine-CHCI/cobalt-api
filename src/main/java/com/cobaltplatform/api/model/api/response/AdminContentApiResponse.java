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

import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.ContentStatus.ContentStatusId;
import com.cobaltplatform.api.model.db.ContentType;
import com.cobaltplatform.api.model.db.ContentType.ContentTypeId;
import com.cobaltplatform.api.model.db.Role;
import com.cobaltplatform.api.model.service.AdminContent;
import com.cobaltplatform.api.service.ContentService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDate;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.model.db.ContentAction.ContentActionId;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AdminContentApiResponse {
	@Nullable
	private UUID contentId;
	@Nullable
	private ContentType.ContentTypeId contentTypeId;
	@Nullable
	private String title;
	@Nullable
	private String author;
	@Nullable
	private String description;
	@Nullable
	private String url;
	@Nullable
	private String imageUrl;
	@Nullable
	private String ownerInstitution;
	@Nullable
	private Integer views;
	@Nullable
	private String duration;
	@Nullable
	private Integer durationInMinutes;
	@Nonnull
	private final List<String> tagIds;
	@Nullable
	private LocalDate publishStartDate;
	@Nullable
	private String publishStartDateDescription;
	@Nullable
	private LocalDate publishEndDate;
	@Nullable
	private String publishEndDateDescription;
	@Nullable
	private LocalDate dateCreated;
	@Nullable
	private String dateCreatedDescription;
	@Nullable
	private Boolean publishRecurring;
	@Nullable
	private String searchTerms;
	@Nullable
	private Boolean sharedFlag;
	@Nullable
	private ContentStatusId contentStatusId;
	@Nullable
	private String contentStatusDescription;
	@Nullable
	private List<ContentActionId> actions;
	@Nullable
	private String contentTypeDescription;
	@Nullable
	private Integer inUseCount;
	@Nullable
	private String inUseInstitutionDescription;
	@Nullable
	private Boolean newFlag;
	@Nullable
	private String durationInMinutesDescription;

	@Nullable
	private String callToAction;

	@Nullable
	private UUID fileUploadId;
	@Nullable
	private final List<TagApiResponse> tags;

	@Nullable
	private Boolean isEditable;

	@Nullable
	private String filename;

	@Nullable
	private UUID imageFileUploadId;

	@Nullable
	private String fileContentType;

	public enum AdminContentDisplayType {
		DETAIL,
		LIST
	}

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface AdminContentApiResponseFactory {
		@Nonnull
		AdminContentApiResponse create(@Nonnull Account account,
																	 @Nonnull AdminContent adminContent,
																	 @Nonnull AdminContentDisplayType adminContentDisplayType,
																	 @Nonnull List<UUID> institutionContentIds);
	}

	@AssistedInject
	public AdminContentApiResponse(@Nonnull Formatter formatter,
																 @Nonnull ContentService contentService,
																 @Assisted @Nonnull Account account,
																 @Assisted @Nonnull AdminContent adminContent,
																 @Assisted @Nonnull AdminContentDisplayType adminContentDisplayType,
																 @Assisted @Nonnull List<UUID> institutionContentIds,
																 @Nonnull Strings strings,
																 @Nonnull TagApiResponse.TagApiResponseFactory tagApiResponseFactory) {
		requireNonNull(formatter);
		requireNonNull(adminContent);
		requireNonNull(contentService);
		requireNonNull(account);
		requireNonNull(institutionContentIds);
		requireNonNull(strings);
		requireNonNull(tagApiResponseFactory);

		List<ContentActionId> contentActionIdList = new ArrayList<>();
		Boolean contentOwnedByCurrentAccount = account.getInstitutionId().equals(adminContent.getOwnerInstitutionId());

		this.contentId = adminContent.getContentId();
		this.contentTypeId = adminContent.getContentTypeId();
		this.title = adminContent.getTitle();
		this.author = adminContent.getAuthor();
		this.description = adminContent.getDescription();
		this.url = adminContent.getFileUploadId() != null ? adminContent.getFileUrl() : adminContent.getUrl();
		this.imageUrl = adminContent.getImageUrl();
		this.ownerInstitution = adminContent.getOwnerInstitution();
		this.views = adminContent.getViews();
		//TODO: update frontend to use durationInMinutes
		this.duration = adminContent.getDurationInMinutes() != null ? adminContent.getDurationInMinutes().toString() : null;
		this.durationInMinutes = adminContent.getDurationInMinutes();
		this.publishStartDate = adminContent.getPublishStartDate();
		this.publishStartDateDescription = adminContent.getPublishStartDate() != null ? formatter.formatDate(adminContent.getPublishStartDate(), FormatStyle.SHORT) : null;
		this.publishEndDate = adminContent.getPublishEndDate();
		this.publishEndDateDescription = adminContent.getPublishEndDate() != null ? formatter.formatDate(adminContent.getPublishEndDate(), FormatStyle.SHORT) : "No Expiry";
		this.dateCreated = adminContent.getDateCreated();
		this.dateCreatedDescription = formatter.formatDate(adminContent.getDateCreated(), FormatStyle.SHORT);
		this.publishRecurring = adminContent.getPublishRecurring();
		this.searchTerms = adminContent.getSearchTerms();
		this.sharedFlag = adminContent.getSharedFlag();
		this.contentTypeDescription = adminContent.getContentTypeDescription();

		if (contentOwnedByCurrentAccount) {
			this.contentStatusId = adminContent.getContentStatusId();
			this.contentStatusDescription = adminContent.getContentStatusDescription();
			if (adminContent.getContentStatusId().equals(ContentStatusId.DRAFT)) {
				contentActionIdList.add(ContentActionId.EDIT);
				contentActionIdList.add(ContentActionId.DELETE);
			} else if (adminContent.getContentStatusId().equals(ContentStatusId.LIVE)) {
				contentActionIdList.add(ContentActionId.EDIT);
				contentActionIdList.add(ContentActionId.VIEW_ON_COBALT);
				contentActionIdList.add(ContentActionId.EXPIRE);
			} else if (adminContent.getContentStatusId().equals(ContentStatusId.EXPIRED)) {
				contentActionIdList.add(ContentActionId.EDIT);
				contentActionIdList.add(ContentActionId.DELETE);
			} else if (adminContent.getContentStatusId().equals(ContentStatusId.SCHEDULED)) {
				contentActionIdList.add(ContentActionId.EDIT);
			}
		} else {
			if (institutionContentIds.contains(adminContent.getContentId())) {
				this.contentStatusId = adminContent.getContentStatusId();
				this.contentStatusDescription = adminContent.getContentStatusDescription();
				contentActionIdList.add(ContentActionId.REMOVE);
				contentActionIdList.add(ContentActionId.VIEW_ON_COBALT);
			} else {
				this.contentStatusId = ContentStatusId.AVAILABLE;
				this.contentStatusDescription = "Available";
				contentActionIdList.add(ContentActionId.ADD);
			}
		}

		this.actions = contentActionIdList;

		this.tagIds = adminContent.getTags() == null ? Collections.emptyList() : adminContent.getTags().stream()
				.map(tag -> tag.getTagId())
				.collect(Collectors.toList());

		//TODO: Better logic to set the new flag
		this.newFlag = this.dateCreated.compareTo(LocalDate.now().minus(14, ChronoUnit.DAYS)) > 0;
		this.inUseCount = adminContent.getInUseCount();
		this.inUseInstitutionDescription = adminContent.getInUseInstitutionDescription();

		this.callToAction = adminContent.getCallToAction();
		this.durationInMinutesDescription = adminContent.getDurationInMinutes() != null ?
				strings.get("{{minutes}} min", new HashMap<>() {{
					put("minutes", formatter.formatNumber(adminContent.getDurationInMinutes()));
				}}) : null;

		this.fileUploadId = adminContent.getFileUploadId();

		this.imageFileUploadId = adminContent.getImageFileUploadId();

		this.tags = adminContent.getTags() == null ? Collections.emptyList() : adminContent.getTags().stream()
				.map(tag -> tagApiResponseFactory.create(tag))
				.collect(Collectors.toList());

		this.isEditable = account.getInstitutionId().equals(adminContent.getOwnerInstitutionId())
				&& account.getRoleId().equals(Role.RoleId.ADMINISTRATOR);

		this.filename = adminContent.getFilename();

		this.fileContentType = adminContent.getFileContentType();
	}


	/*
	@Nonnull
	private void sendAdminNotification(@Nonnull Account accountAddingContent,
																		 @Nonnull AdminContent adminContent) {
		List<Account> accountsToNotify = accountAddingContent.getRoleId() == RoleId.ADMINISTRATOR
				? List.of() : getAccountService().findAdminAccountsForInstitution(accountAddingContent.getInstitutionId());

		String date = adminContent.getDateCreated() == null ? getFormatter().formatDate(LocalDate.now(), FormatStyle.SHORT) : getFormatter().formatDate(adminContent.getDateCreated(), FormatStyle.SHORT);

		getDatabase().currentTransaction().get().addPostCommitOperation(() -> {
			for (Account accountToNotify : accountsToNotify) {
				if (accountToNotify.getEmailAddress() != null) {
					EmailMessage emailMessage = new EmailMessage.Builder(accountToNotify.getInstitutionId(), EmailMessageTemplate.ADMIN_CMS_CONTENT_ADDED, accountToNotify.getLocale())
							.toAddresses(List.of(accountToNotify.getEmailAddress()))
							.messageContext(Map.of(
									"adminAccountName", Normalizer.normalizeName(accountToNotify.getFirstName(), accountToNotify.getLastName()).orElse(getStrings().get("Anonymous User")),
									"submittingAccountName", Normalizer.normalizeName(accountAddingContent.getFirstName(), accountAddingContent.getLastName()).orElse(getStrings().get("Anonymous User")),
									"contentType", adminContent.getContentTypeId().name(),
									"contentTitle", adminContent.getTitle(),
									"contentAuthor", adminContent.getAuthor(),
									"submissionDate", date,
									"cmsListUrl", getLinkGenerator().generateCmsMyContentLink(accountToNotify.getInstitutionId(), UserExperienceTypeId.STAFF)
							))
							.build();

					getEmailMessageManager().enqueueMessage(emailMessage);
				}
			}
		});

	}
*/

	@Nonnull
	public UUID getContentId() {
		return contentId;
	}

	@Nonnull
	public ContentTypeId getContentTypeId() {
		return contentTypeId;
	}

	@Nonnull
	public String getTitle() {
		return title;
	}

	@Nullable
	public String getAuthor() {
		return author;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	@Nullable
	public String getOwnerInstitution() {
		return ownerInstitution;
	}

	@Nonnull
	public Integer getViews() {
		return views;
	}


	@Nullable
	public String getDuration() {
		return duration;
	}

	@Nullable
	public Integer getDurationInMinutes() {
		return durationInMinutes;
	}

	@Nonnull
	public List<String> getTagIds() {
		return this.tagIds;
	}

	@Nullable
	public String getUrl() {
		return url;
	}

	@Nullable
	public String getImageUrl() {
		return imageUrl;
	}

	@Nullable
	public LocalDate getPublishStartDate() {
		return publishStartDate;
	}

	@Nullable
	public LocalDate getPublishEndDate() {
		return publishEndDate;
	}

	@Nullable
	public Boolean getPublishRecurring() {
		return publishRecurring;
	}

	@Nullable
	public String getSearchTerms() {
		return searchTerms;
	}

	@Nullable
	public Boolean getSharedFlag() {
		return sharedFlag;
	}

	@Nullable
	public ContentStatusId getContentStatusId() {
		return contentStatusId;
	}

	@Nullable
	public String getPublishStartDateDescription() {
		return publishStartDateDescription;
	}

	@Nullable
	public String getPublishEndDateDescription() {
		return publishEndDateDescription;
	}

	@Nullable
	public String getContentStatusDescription() {
		return contentStatusDescription;
	}

	@Nullable
	public List<ContentActionId> getActions() {
		return actions;
	}

	@Nullable
	public LocalDate getDateCreated() {
		return dateCreated;
	}

	@Nullable
	public String getDateCreatedDescription() {
		return dateCreatedDescription;
	}

	@Nullable
	public String getContentTypeDescription() {
		return contentTypeDescription;
	}

	@Nullable
	public Integer getInUseCount() {
		return inUseCount;
	}

	@Nullable
	public String getInUseInstitutionDescription() {
		return inUseInstitutionDescription;
	}

	@Nullable
	public Boolean getNewFlag() {
		return newFlag;
	}

	@Nullable
	public String getDurationInMinutesDescription() {
		return durationInMinutesDescription;
	}

	@Nullable
	public String getCallToAction() {
		return callToAction;
	}

	@Nullable
	public UUID getFileUploadId() {
		return fileUploadId;
	}

	@Nullable
	public Boolean getEditable() {
		return isEditable;
	}

	@Nullable
	public List<TagApiResponse> getTags() {
		return tags;
	}

	@Nullable
	public String getFilename() {
		return filename;
	}

	@Nullable
	public String getFileContentType() {
		return fileContentType;
	}

	@Nullable
	public UUID getImageFileUploadId() {
		return imageFileUploadId;
	}
}