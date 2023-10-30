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

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.request.CreateContentRequest;
import com.cobaltplatform.api.model.api.request.FindResourceLibraryContentRequest;
import com.cobaltplatform.api.model.api.request.PersonalizeAssessmentChoicesCommand.SubmissionAnswer;
import com.cobaltplatform.api.model.api.request.UpdateContentArchivedStatus;
import com.cobaltplatform.api.model.api.request.UpdateContentRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSession;
import com.cobaltplatform.api.model.db.ActivityAction.ActivityActionId;
import com.cobaltplatform.api.model.db.ActivityType.ActivityTypeId;
import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.db.ContentStatus.ContentStatusId;
import com.cobaltplatform.api.model.db.ContentType;
import com.cobaltplatform.api.model.db.ContentType.ContentTypeId;
import com.cobaltplatform.api.model.db.ContentTypeLabel;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Tag;
import com.cobaltplatform.api.model.db.TagContent;
import com.cobaltplatform.api.model.service.AdminContent;
import com.cobaltplatform.api.model.service.ContentDurationId;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.LinkGenerator;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.ValidationUtility;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.model.db.Role.RoleId;
import static com.cobaltplatform.api.util.DatabaseUtility.sqlInListPlaceholders;
import static com.cobaltplatform.api.util.DatabaseUtility.sqlVaragsParameters;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify LLC.
 */
@Singleton
public class AdminContentService {
	@Nonnull
	private static final int DEFAULT_PAGE_SIZE = 15;
	private static final int MAXIMUM_PAGE_SIZE = 100;

	@Nonnull
	private final Database database;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final SessionService sessionService;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Provider<AssessmentService> assessmentServiceProvider;
	@Nonnull
	private final Provider<TagService> tagServiceProvider;
	@Nonnull
	private final Provider<MessageService> messageServiceProvider;
	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final Provider<Formatter> formatterProvider;
	@Nonnull
	private final Provider<LinkGenerator> linkGeneratorProvider;

	@Inject
	public AdminContentService(@Nonnull Provider<CurrentContext> currentContextProvider,
														 @Nonnull Provider<AssessmentService> assessmentServiceProvider,
														 @Nonnull Provider<TagService> tagServiceProvider,
														 @Nonnull Provider<MessageService> messageServiceProvider,
														 @Nonnull Provider<AccountService> accountServiceProvider,
														 @Nonnull Provider<Formatter> formatterProvider,
														 @Nonnull Provider<LinkGenerator> linkGeneratorProvider,
														 @Nonnull Database database,
														 @Nonnull SessionService sessionService,
														 @Nonnull InstitutionService institutionService,
														 @Nonnull Strings strings) {
		requireNonNull(currentContextProvider);
		requireNonNull(assessmentServiceProvider);
		requireNonNull(tagServiceProvider);
		requireNonNull(messageServiceProvider);
		requireNonNull(accountServiceProvider);
		requireNonNull(formatterProvider);
		requireNonNull(linkGeneratorProvider);
		requireNonNull(database);
		requireNonNull(sessionService);
		requireNonNull(institutionService);
		requireNonNull(strings);

		this.logger = LoggerFactory.getLogger(getClass());
		this.database = database;
		this.sessionService = sessionService;
		this.tagServiceProvider = tagServiceProvider;
		this.currentContextProvider = currentContextProvider;
		this.institutionService = institutionService;
		this.strings = strings;
		this.assessmentServiceProvider = assessmentServiceProvider;
		this.messageServiceProvider = messageServiceProvider;
		this.accountServiceProvider = accountServiceProvider;
		this.formatterProvider = formatterProvider;
		this.linkGeneratorProvider = linkGeneratorProvider;
	}

	@Nonnull
	public AdminContent createContent(@Nonnull Account account,
																		@Nonnull CreateContentRequest command) {
		requireNonNull(account);
		requireNonNull(command);

		UUID contentId = UUID.randomUUID();
		String title = trimToNull(command.getTitle());
		String url = trimToNull(command.getUrl());
		String imageUrl = trimToNull(command.getImageUrl());
		String description = trimToNull(command.getDescription());
		String author = trimToNull(command.getAuthor());
		ContentStatusId contentStatusId = command.getContentStatusId();
		ContentTypeId contentTypeId = command.getContentTypeId();
		String durationInMinutesString = trimToNull(command.getDurationInMinutes());
		Set<String> tagIds = command.getTagIds() == null ? Set.of() : command.getTagIds();
		LocalDate publishStartDate = command.getPublishStartDate();
		LocalDate publishEndDate = command.getPublishEndDate();
		Boolean publishRecurring = command.getPublishRecurring() == null ? false : command.getPublishRecurring();
		String searchTerms = trimToNull(command.getSearchTerms());
		Boolean sharedFlag = command.getSharedFlag();

		ValidationException validationException = new ValidationException();

		if (title == null)
			validationException.add(new FieldError("title", getStrings().get("Title is required.")));

		if (url == null && contentTypeId != null && contentTypeId != ContentTypeId.ARTICLE)
			validationException.add(new FieldError("url", getStrings().get("URL is required.")));

		if (author == null)
			validationException.add(new FieldError("author", getStrings().get("Author is required.")));

		if (description == null) {
			validationException.add(new FieldError("description", getStrings().get("Description is required")));
		}

		if (contentTypeId == null) {
			validationException.add(new FieldError("contentTypeId", getStrings().get("Content type is required")));
		}

		if (contentStatusId == null) {
			validationException.add(new FieldError("contentStatusId", getStrings().get("Content status is required")));
		}

		if (sharedFlag == null) {
			validationException.add(new FieldError("sharedFlag", getStrings().get("Shared flag is required")));
		}

		if (contentStatusId == null) {
			validationException.add(new FieldError("contentStatusId", getStrings().get("Content status is required")));
		}

		if (publishStartDate == null) {
			validationException.add(new FieldError("publishStartDate", getStrings().get("Publish start date is required")));
		}

		if (durationInMinutesString != null && !ValidationUtility.isValidInteger(durationInMinutesString))
			validationException.add(new FieldError("durationInMinutes", getStrings().get("Must be an integer")));

		if (validationException.hasErrors())
			throw validationException;

		Integer durationInMinutes = durationInMinutesString == null ? null : Integer.parseInt(durationInMinutesString);

		if (url != null && !url.startsWith("http://") && !url.startsWith("https://"))
			url = String.format("https://%s", url);

		InstitutionId ownerInstitutionId = account.getInstitutionId();

		getDatabase().execute("""
						INSERT INTO content (content_id, content_type_id, title, url, image_url,
						duration_in_minutes, description, author, content_status_id, shared_flag,
						search_terms, publish_start_date, publish_end_date, publish_recurring, owner_institution_id, date_created)
						VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, now())												
						""",
				contentId, command.getContentTypeId(), title, url, imageUrl,
				durationInMinutes, description, author, contentStatusId, sharedFlag,
				searchTerms, publishStartDate, publishEndDate, publishRecurring, ownerInstitutionId);

		addContentToInstitution(contentId, account.getInstitutionId());

		for (String tagId : tagIds) {
			tagId = trimToNull(tagId);

			if (tagId == null)
				continue;

			getDatabase().execute("""
					INSERT INTO tag_content(tag_id, institution_id, content_id) 
					VALUES (?,?,?)
					""", tagId, account.getInstitutionId(), contentId);
		}

		AdminContent adminContent = findAdminContentByIdForInstitution(account.getInstitutionId(), contentId).get();
		applyTagsToAdminContent(adminContent, account.getInstitutionId());
		//TODO: Do we still want to send emails?
		//sendAdminNotification(account, adminContent);
		return adminContent;
	}

	@Nonnull
	public AdminContent updateContent(@Nonnull Account account, @Nonnull UpdateContentRequest command) {
		requireNonNull(account);
		requireNonNull(command);

		String titleCommand = trimToNull(command.getTitle());
		String urlCommand = trimToNull(command.getUrl());
		String imageUrlCommand = trimToNull(command.getImageUrl());
		String descriptionCommand = trimToNull(command.getDescription());
		String authorCommand = trimToNull(command.getAuthor());
		ContentTypeId contentTypeIdCommand = command.getContentTypeId();
		String durationInMinutesString = trimToNull(command.getDurationInMinutes());
		Set<String> tagIds = command.getTagIds() == null ? Set.of() : command.getTagIds();
		LocalDate publishStartDate = command.getPublishStartDate();
		LocalDate publishEndDate = command.getPublishEndDate();
		Boolean publishRecurring = command.getPublishRecurring();
		String searchTerms = trimToNull(command.getSearchTerms());
		Boolean sharedFlag = command.getSharedFlag();

		ValidationException validationException = new ValidationException();

		AdminContent existingContent = findAdminContentByIdForInstitution(account.getInstitutionId(), command.getContentId()).orElseThrow();

		if (hasAdminAccessToContent(account, existingContent)) {
			if (titleCommand != null) {
				existingContent.setTitle(titleCommand);
			}

			if (urlCommand != null) {
				if (!urlCommand.startsWith("http://") && !urlCommand.startsWith("https://"))
					urlCommand = String.format("https://%s", urlCommand);
				existingContent.setUrl(urlCommand);
			}

			if (imageUrlCommand != null) {
				existingContent.setImageUrl(imageUrlCommand);
			}

			if (descriptionCommand != null) {
				existingContent.setDescription(descriptionCommand);
			}

			if (authorCommand != null) {
				existingContent.setAuthor(authorCommand);
			}

			if (contentTypeIdCommand != null) {
				existingContent.setContentTypeId(contentTypeIdCommand);
			}

			if (publishStartDate != null)
				existingContent.setPublishStartDate(publishStartDate);

			if (publishEndDate != null)
				existingContent.setPublishEndDate(publishEndDate);

			if (publishRecurring != null)
				existingContent.setPublishRecurring(publishRecurring);

			if (searchTerms != null)
				existingContent.setSearchTerms(searchTerms);

			if (sharedFlag !=null)
				existingContent.setSharedFlag(sharedFlag);
		}

		if (durationInMinutesString != null && !ValidationUtility.isValidInteger(durationInMinutesString))
			validationException.add(new FieldError("durationInMinutes", getStrings().get("Must be an integer")));

		if (validationException.hasErrors())
			throw validationException;

		Integer durationInMinutes = durationInMinutesString == null ? null : Integer.parseInt(durationInMinutesString);
		boolean shouldNotify = false;

		getDatabase().execute("""
							 	UPDATE content SET content_type_id=?, title=?, url=?, image_url=?, 
							 	duration_in_minutes=?, description=?, author=?, publish_start_date=?, publish_end_date=?,
							 	publish_recurring=?, search_terms=?, shared_flag=?, content_status_id=?
								WHERE content_id=?
						""",
				existingContent.getContentTypeId(), existingContent.getTitle(), existingContent.getUrl(), existingContent.getImageUrl(),
				durationInMinutes, existingContent.getDescription(), existingContent.getAuthor(), existingContent.getPublishStartDate(), existingContent.getPublishEndDate(),
				existingContent.getPublishRecurring(), existingContent.getSearchTerms(), existingContent.getSharedFlag(), existingContent.getContentStatusId(),
				existingContent.getContentId());

		AdminContent adminContent = findAdminContentByIdForInstitution(account.getInstitutionId(), command.getContentId()).orElse(null);

		applyTagsToAdminContent(adminContent, account.getInstitutionId());

		getDatabase().execute("""
				DELETE FROM tag_content
				WHERE content_id=?
				AND institution_id=? 
				""", command.getContentId(), account.getInstitutionId());

		for (String tagId : tagIds) {
			tagId = trimToNull(tagId);

			if (tagId == null)
				continue;

			getDatabase().execute("""
					INSERT INTO tag_content(tag_id, institution_id, content_id) 
					VALUES (?,?,?)
					""", tagId, account.getInstitutionId(), command.getContentId());
		}

		/*
		if (shouldNotify) {
			sendAdminNotification(account, adminContent);
		}
		*/
		return adminContent;
	}

	@Nonnull
	private void addContentToInstitution(@Nonnull UUID contentId, @Nonnull InstitutionId institutionId) {
		requireNonNull(contentId);
		requireNonNull(institutionId);

		getDatabase().execute("""
				INSERT INTO institution_content (institution_content_id, institution_id, content_id) 
				VALUES (?,?,?)
				""", UUID.randomUUID(), institutionId, contentId);
	}

	@Nonnull
	public Optional<AdminContent> findAdminContentByIdForInstitution(@Nonnull InstitutionId institutionId, @Nonnull UUID contentId) {
		requireNonNull(institutionId);
		requireNonNull(contentId);

		AdminContent adminContent = getDatabase().queryForObject("SELECT va.*, " +
						"(select COUNT(*) FROM " +
						" activity_tracking a WHERE " +
						" va.content_id = CAST (a.context ->> 'contentId' AS UUID) AND " +
						" a.activity_action_id = 'VIEW' AND " +
						" activity_type_id='CONTENT') AS views " +
						"FROM v_admin_content va " +
						"WHERE va.content_id = ? " +
						"AND va.institution_id = ? ",
				AdminContent.class, contentId, institutionId).orElse(null);

		if (adminContent != null)
			applyTagsToAdminContent(adminContent, institutionId);

		return Optional.ofNullable(adminContent);
	}

	/**
	 * Note: modifies {@code content} parameter in-place.
	 */
	@Nonnull
	protected <T extends AdminContent> void applyTagsToAdminContent(@Nonnull T adminContent,
																																	@Nonnull InstitutionId institutionId) {
		requireNonNull(adminContent);
		requireNonNull(institutionId);

		adminContent.setTags(getTagService().findTagsByContentIdAndInstitutionId(adminContent.getContentId(), institutionId));
	}

	@Nonnull
	public Boolean hasAdminAccessToContent(@Nonnull Account account,
																				 @Nonnull AdminContent content) {
		requireNonNull(account);
		requireNonNull(content);

		return account.getRoleId() == RoleId.ADMINISTRATOR && account.getInstitutionId() == content.getOwnerInstitutionId();
	}

	@Nonnull
	protected SessionService getSessionService() {
		return sessionService;
	}

	@Nonnull
	protected Database getDatabase() {
		return database;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return institutionService;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected AssessmentService getAssessmentService() {
		return assessmentServiceProvider.get();
	}

	@Nonnull
	protected TagService getTagService() {
		return this.tagServiceProvider.get();
	}

	@Nonnull
	protected MessageService getEmailMessageManager() {
		return messageServiceProvider.get();
	}

	@Nonnull
	protected AccountService getAccountService() {
		return accountServiceProvider.get();
	}

	@Nonnull
	protected Formatter getFormatter() {
		return formatterProvider.get();
	}

	@Nonnull
	protected LinkGenerator getLinkGenerator() {
		return linkGeneratorProvider.get();
	}
}
