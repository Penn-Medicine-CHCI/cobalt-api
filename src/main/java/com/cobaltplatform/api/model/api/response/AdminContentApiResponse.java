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
import com.cobaltplatform.api.model.db.ApprovalStatus;
import com.cobaltplatform.api.model.db.AvailableStatus.AvailableStatusId;
import com.cobaltplatform.api.model.db.ContentType;
import com.cobaltplatform.api.model.db.ContentType.ContentTypeId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Role;
import com.cobaltplatform.api.model.service.AdminContent;
import com.cobaltplatform.api.service.ContentService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDate;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.model.db.ContentAction.ContentActionId;
import static com.cobaltplatform.api.model.db.Visibility.VisibilityId;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AdminContentApiResponse {
	@Nullable
	private UUID contentId;
	@Nullable
	private LocalDate dateCreated;
	@Nullable
	private String dateCreatedDescription;
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
	private AvailableStatusId availableStatusId;
	@Nullable
	private VisibilityId visibilityId;
	@Nullable
	private Integer views;
	@Nullable
	private List<ContentActionId> actions;
	@Nullable
	private List<UUID> contentTagIds;
	@Nullable
	private String duration;
	@Nullable
	private Integer durationInMinutes;
	@Nullable
	private Boolean visibleToOtherInstitutions;
	@Nullable
	private List<Institution> selectedNetworkInstitutions;
	@Nullable
	private String contentTypeLabelId;
	@Nullable
	private ApprovalStatus ownerInstitutionApprovalStatus;
	@Nullable
	private ApprovalStatus otherInstitutionApprovalStatus;
	@Nonnull
	private final List<String> tagIds;

	public enum AdminContentDisplayType {
		DETAIL,
		AVAILABLE_CONTENT,
		MY_CONTENT
	}

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface AdminContentApiResponseFactory {
		@Nonnull
		AdminContentApiResponse create(@Nonnull Account account,
																	 @Nonnull AdminContent adminContent,
																	 @Nonnull AdminContentDisplayType adminContentDisplayType);
	}

	@AssistedInject
	public AdminContentApiResponse(@Nonnull Formatter formatter,
																 @Nonnull ContentService contentService,
																 @Nonnull InstitutionService institutionService,
																 @Assisted @Nonnull Account account,
																 @Assisted @Nonnull AdminContent adminContent,
																 @Assisted @Nonnull AdminContentDisplayType adminContentDisplayType) {
		requireNonNull(formatter);
		requireNonNull(adminContent);
		requireNonNull(contentService);
		requireNonNull(account);

		List<ContentActionId> contentActionIdList = new ArrayList<>();

		this.contentId = adminContent.getContentId();
		this.dateCreated = adminContent.getDateCreated();
		this.dateCreatedDescription = adminContent.getDateCreated() != null ? formatter.formatDate(adminContent.getDateCreated(), FormatStyle.SHORT) : null;
		this.contentTypeId = adminContent.getContentTypeId();
		this.title = adminContent.getTitle();
		this.author = adminContent.getAuthor();
		this.description = adminContent.getDescription();
		this.url = adminContent.getUrl();
		this.imageUrl = adminContent.getImageUrl();
		this.ownerInstitution = adminContent.getOwnerInstitution();
		this.views = adminContent.getViews();
		this.visibilityId = adminContent.getVisibilityId();
		//TODO: update frontend to use durationInMinutes
		this.duration = adminContent.getDurationInMinutes() != null ? adminContent.getDurationInMinutes().toString() : null;
		this.durationInMinutes = adminContent.getDurationInMinutes();
		this.contentTypeLabelId = adminContent.getContentTypeLabelId();
		this.visibleToOtherInstitutions = this.visibilityId == VisibilityId.NETWORK || this.visibilityId == VisibilityId.PUBLIC;
		this.ownerInstitutionApprovalStatus = contentService.findApprovalStatusById(adminContent.getOwnerInstitutionApprovalStatusId());

		if (visibleToOtherInstitutions) {
			this.otherInstitutionApprovalStatus = contentService.findApprovalStatusById(adminContent.getOtherInstitutionApprovalStatusId());
		} else {
			this.otherInstitutionApprovalStatus = contentService.findApprovalStatusById(ApprovalStatus.ApprovalStatusId.NOT_APPLICABLE);
		}

		if (adminContent.getArchivedFlag()) {
			this.ownerInstitutionApprovalStatus = contentService.findApprovalStatusById(ApprovalStatus.ApprovalStatusId.ARCHIVED);
			this.otherInstitutionApprovalStatus = contentService.findApprovalStatusById(ApprovalStatus.ApprovalStatusId.ARCHIVED);
		}


		if (adminContentDisplayType == AdminContentDisplayType.DETAIL) {
			this.contentTagIds = contentService.findTagsForContent(adminContent.getContentId());
			if (this.visibilityId == VisibilityId.NETWORK) {
				this.selectedNetworkInstitutions = institutionService.findSelectedNetworkInstitutionsForContentId(adminContent.getOwnerInstitutionId(), contentId);
			}
		}

		if (adminContentDisplayType == AdminContentDisplayType.MY_CONTENT) {
			contentActionIdList.add(ContentActionId.EDIT);
		}

		if (adminContent.getArchivedFlag()) {
			contentActionIdList.add(ContentActionId.UNARCHIVE);
			contentActionIdList.add(ContentActionId.DELETE);
		} else {
			if (adminContentDisplayType == AdminContentDisplayType.MY_CONTENT) {
				if (account.getRoleId() == Role.RoleId.SUPER_ADMINISTRATOR) {
					if (this.visibilityId == VisibilityId.PUBLIC) {
						switch (adminContent.getOtherInstitutionApprovalStatusId()) {
							case PENDING:
								contentActionIdList.add(ContentActionId.APPROVE);
								contentActionIdList.add(ContentActionId.REJECT);
								break;
							case APPROVED:
								contentActionIdList.add(ContentActionId.ARCHIVE);
								break;
							case REJECTED:
								contentActionIdList.add(ContentActionId.APPROVE);
								contentActionIdList.add(ContentActionId.DELETE);
								break;
							case ARCHIVED:
								break;
						}
					}
				} else {
					if (this.visibilityId == VisibilityId.PUBLIC) {
						switch (adminContent.getOtherInstitutionApprovalStatusId()) {
							case PENDING:
								contentActionIdList.add(ContentActionId.REJECT);
								contentActionIdList.add(ContentActionId.ARCHIVE);
								break;
							case APPROVED:
								contentActionIdList.add(ContentActionId.ARCHIVE);
								break;
							case REJECTED:
								contentActionIdList.add(ContentActionId.APPROVE);
								contentActionIdList.add(ContentActionId.DELETE);
								break;
							case ARCHIVED:
								break;
						}
					} else if (this.visibilityId == VisibilityId.PRIVATE) {
						switch (adminContent.getOwnerInstitutionApprovalStatusId()) {

							case PENDING:
								contentActionIdList.add(ContentActionId.APPROVE);
								contentActionIdList.add(ContentActionId.REJECT);
								break;
							case APPROVED:
								contentActionIdList.add(ContentActionId.ARCHIVE);
								break;
							case REJECTED:
								contentActionIdList.add(ContentActionId.APPROVE);
								contentActionIdList.add(ContentActionId.DELETE);
								break;
							case ARCHIVED:
								contentActionIdList.add(ContentActionId.APPROVE);
								break;
						}
					} else {
						if (!adminContent.getArchivedFlag())
							contentActionIdList.add(ContentActionId.ARCHIVE);
					}
				}
			} else if (adminContentDisplayType == AdminContentDisplayType.AVAILABLE_CONTENT) {
				if (adminContent.getApprovedFlag()) {
					contentActionIdList.add(ContentActionId.REMOVE);
					availableStatusId = AvailableStatusId.ADDED;
				} else {
					contentActionIdList.add(ContentActionId.ADD);
					availableStatusId = AvailableStatusId.AVAILABLE;
				}
			}
		}
		this.actions = contentActionIdList;

		this.tagIds = adminContent.getTags() == null ? Collections.emptyList() : adminContent.getTags().stream()
				.map(tag -> tag.getTagId())
				.collect(Collectors.toList());
	}

	@Nonnull
	public UUID getContentId() {
		return contentId;
	}

	@Nullable
	public LocalDate getDateCreated() {
		return dateCreated;
	}

	@Nonnull
	public String getDateCreatedDescription() {
		return dateCreatedDescription;
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

	@Nonnull
	public List<ContentActionId> getActions() {
		return actions;
	}

	@Nullable
	public String getDuration() {
		return duration;
	}

	@Nullable
	public Integer getDurationInMinutes() {
		return durationInMinutes;
	}

	@Nullable
	public Boolean getVisibleToOtherInstitutions() {
		return visibleToOtherInstitutions;
	}

	@Nullable
	public List<Institution> getSelectedNetworkInstitutions() {
		return selectedNetworkInstitutions;
	}

	@Nullable
	public String getContentTypeLabelId() {
		return contentTypeLabelId;
	}

	@Nullable
	public VisibilityId getVisibilityId() {
		return visibilityId;
	}

	@Nullable
	public ApprovalStatus getOwnerInstitutionApprovalStatus() {
		return ownerInstitutionApprovalStatus;
	}

	@Nullable
	public ApprovalStatus getOtherInstitutionApprovalStatus() {
		return otherInstitutionApprovalStatus;
	}

	@Nonnull
	public List<String> getTagIds() {
		return this.tagIds;
	}
}