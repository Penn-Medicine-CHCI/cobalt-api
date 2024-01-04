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
import com.cobaltplatform.api.model.api.request.CreateFileUploadRequest;
import com.cobaltplatform.api.model.api.request.UpdateContentRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.db.ContentStatus;
import com.cobaltplatform.api.model.db.ContentStatus.ContentStatusId;
import com.cobaltplatform.api.model.db.ContentType.ContentTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.service.AdminContent;
import com.cobaltplatform.api.model.service.FileUploadResult;
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
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.cobaltplatform.api.model.db.Role.RoleId;
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
	@Nonnull
	private final Provider<ContentService> contentServiceProvider;

	@Nonnull
	private final SystemService systemService;

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
														 @Nonnull Strings strings,
														 @Nonnull Provider<ContentService> contentServiceProvider,
														 @Nonnull SystemService systemService) {
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
		requireNonNull(contentServiceProvider);
		requireNonNull(systemService);

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
		this.contentServiceProvider = contentServiceProvider;
		this.systemService = systemService;
	}

	public enum ContentSortOrder {
		DATE_ADDED_DESC,
		DATE_ADDED_ASC,
		PUBLISH_DATE_DESC,
		PUBLISH_DATE_ASC,
		EXPIRY_DATE_DESC,
		EXPIRY_DATE_ASC
	}

	@Nonnull
	public FindResult<AdminContent> findAllContentForAdmin(@Nonnull Account account,
																												 @Nonnull Optional<Integer> page,
																												 @Nonnull Optional<ContentTypeId> contentTypeId,
																												 @Nonnull Optional<InstitutionId> institutionId,
																												 @Nonnull Optional<String> search,
																												 @Nonnull Optional<ContentStatusId> contentStatusId,
																												 @Nonnull Optional<ContentSortOrder> orderBy,
																												 @Nonnull Optional<Boolean> sharingOn,
																												 @Nonnull Optional<String> tagId) {
		requireNonNull(account);

		List<Object> parameters = new ArrayList();
		Integer pageNumber = page.orElse(0);
		Integer limit = DEFAULT_PAGE_SIZE;
		Integer offset = pageNumber * DEFAULT_PAGE_SIZE;
		StringBuilder whereClause = new StringBuilder(" 1=1 ");
		StringBuilder orderByClause = new StringBuilder("ORDER BY ");

		if (contentTypeId.isPresent()) {
			whereClause.append("AND va.content_type_id = ? ");
			parameters.add(contentTypeId.get());
		}

		if (institutionId.isPresent()) {
			whereClause.append("AND (owner_institution_id = ? AND shared_flag = true AND content_status_id IN ('LIVE', 'SCHEDULED')) ");
			parameters.add(institutionId.get());
		} else {
			whereClause.append("AND ((owner_institution_id != ? AND shared_flag = true AND content_status_id IN ('LIVE', 'SCHEDULED')) ");
			whereClause.append("OR (owner_institution_id = ?)) ");
			parameters.add(account.getInstitutionId());
			parameters.add(account.getInstitutionId());
		}

		if (search.isPresent()) {
			String lowerSearch = trimToEmpty(search.get().toLowerCase());
			whereClause.append("AND (LOWER(title) % ? or SIMILARITY(LOWER(title), ?) > 0.5 OR LOWER(title) LIKE ?) ");
			parameters.add(lowerSearch);
			parameters.add(lowerSearch);
			parameters.add('%' + lowerSearch + '%');
		}

		if (contentStatusId.isPresent()) {
			whereClause.append("AND va.content_status_id = ? ");
			parameters.add(contentStatusId.get());
		}

		if (tagId.isPresent()) {
			whereClause.append("AND EXISTS (SELECT 'X' FROM tag_content tc WHERE tc.tag_id = ? AND va.content_id = tc.content_id) ");
			parameters.add(tagId.get());
		}

		if (sharingOn.isPresent()) {
			whereClause.append("AND va.shared_flag = ? ");
			parameters.add(sharingOn.get());
		}

		ContentSortOrder contentSortOrder = orderBy.isPresent() ? orderBy.get() : ContentSortOrder.DATE_ADDED_DESC;

		if (contentSortOrder.equals(ContentSortOrder.DATE_ADDED_DESC))
			orderByClause.append("va.created DESC");
		else if (contentSortOrder.equals(ContentSortOrder.DATE_ADDED_ASC))
			orderByClause.append("va.created ASC");
		else if (contentSortOrder.equals(ContentSortOrder.PUBLISH_DATE_DESC))
			orderByClause.append("va.publish_start_date DESC");
		else if (contentSortOrder.equals(ContentSortOrder.PUBLISH_DATE_ASC))
			orderByClause.append("va.publish_start_date ASC");
		else if (contentSortOrder.equals(ContentSortOrder.EXPIRY_DATE_DESC))
			orderByClause.append("va.publish_end_date DESC");
		else if (contentSortOrder.equals(ContentSortOrder.EXPIRY_DATE_ASC))
			orderByClause.append("va.publish_end_date ASC");

		String query =
				String.format("""
						SELECT va.*, 
						(select COUNT(*) FROM 
						 activity_tracking a WHERE
						va.content_id = CAST (a.context ->> 'contentId' AS UUID) AND 
						a.activity_action_id = 'VIEW' AND
						activity_type_id='CONTENT') AS views ,
						count(*) over() AS total_count 
						FROM v_admin_content va 
						WHERE %s 
						%s LIMIT ? OFFSET ? 
						""", whereClause.toString(), orderByClause.toString());

		parameters.add(limit);
		parameters.add(offset);
		List<AdminContent> content = getDatabase().queryForList(query, AdminContent.class, sqlVaragsParameters(parameters));
		Integer totalCount = content.stream().filter(it -> it.getTotalCount() != null).mapToInt(AdminContent::getTotalCount).findFirst().orElse(0);

		getContentService().applyTagsToAdminContents(content, account.getInstitutionId());
		getContentService().applyInstitutionsToAdminContents(content, account.getInstitutionId());

		return new FindResult<>(content, totalCount);
	}

	@Nonnull
	public AdminContent createContent(@Nonnull Account account,
																		@Nonnull CreateContentRequest command) {
		requireNonNull(account);
		requireNonNull(command);

		UUID contentId = UUID.randomUUID();
		String title = trimToNull(command.getTitle());
		String url = trimToNull(command.getUrl());
		String description = trimToNull(command.getDescription());
		String author = trimToNull(command.getAuthor());
		ContentTypeId contentTypeId = command.getContentTypeId();
		String durationInMinutesString = trimToNull(command.getDurationInMinutes());
		Set<String> tagIds = command.getTagIds() == null ? Set.of() : command.getTagIds();
		LocalDate publishStartDate = command.getPublishStartDate();
		LocalDate publishEndDate = command.getPublishEndDate();
		Boolean publishRecurring = command.getPublishRecurring() == null ? false : command.getPublishRecurring();
		String searchTerms = trimToNull(command.getSearchTerms());
		Boolean sharedFlag = command.getSharedFlag();
		UUID fileUploadId = command.getFileUploadId();
		UUID imageFileUploadId = command.getImageFileUploadId();

		ValidationException validationException = new ValidationException();

		if (title == null)
			validationException.add(new FieldError("title", getStrings().get("Title is required.")));

		if (url == null && contentTypeId != null && contentTypeId != ContentTypeId.ARTICLE && fileUploadId == null)
			validationException.add(new FieldError("url", getStrings().get("URL or file upload url is required.")));

		if (author == null)
			validationException.add(new FieldError("author", getStrings().get("Author is required.")));

		if (description == null) {
			validationException.add(new FieldError("description", getStrings().get("Description is required")));
		}

		if (contentTypeId == null) {
			validationException.add(new FieldError("contentTypeId", getStrings().get("Content type is required")));
		}

		if (sharedFlag == null) {
			validationException.add(new FieldError("sharedFlag", getStrings().get("Shared flag is required")));
		}

		if (publishStartDate == null) {
			validationException.add(new FieldError("publishStartDate", getStrings().get("Publish start date is required")));
		}

		if (durationInMinutesString != null && !ValidationUtility.isValidInteger(durationInMinutesString))
			validationException.add(new FieldError("durationInMinutes", getStrings().get("Must be an integer")));

		if (fileUploadId != null && url != null)
			validationException.add(new FieldError("url", getStrings().get("Can only specify a file url or a file upload url ")));

		if (validationException.hasErrors())
			throw validationException;

		Integer durationInMinutes = durationInMinutesString == null ? null : Integer.parseInt(durationInMinutesString);

		if (url != null && !url.startsWith("http://") && !url.startsWith("https://"))
			url = format("https://%s", url);

		InstitutionId ownerInstitutionId = account.getInstitutionId();

		getDatabase().execute("""
						INSERT INTO content (content_id, content_type_id, title, url,
						duration_in_minutes, description, author, shared_flag,
						search_terms, publish_start_date, publish_end_date, publish_recurring, owner_institution_id, date_created, 
						file_upload_id, image_file_upload_id)
						VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,now(),?,?)												
						""",
				contentId, command.getContentTypeId(), title, url,
				durationInMinutes, description, author, sharedFlag,
				searchTerms, publishStartDate, publishEndDate, publishRecurring, ownerInstitutionId, fileUploadId, imageFileUploadId);

		addContentToInstitution(contentId, account);

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
		UUID fileUploadId = command.getFileUploadId();
		UUID imageFileUploadId = command.getImageFileUploadId();

		ValidationException validationException = new ValidationException();

		AdminContent existingContent = findAdminContentByIdForInstitution(account.getInstitutionId(), command.getContentId()).orElseThrow();

		if (hasAdminAccessToContent(account, existingContent)) {
			if (titleCommand != null) {
				existingContent.setTitle(titleCommand);
			}

			if (urlCommand != null) {
				if (!urlCommand.startsWith("http://") && !urlCommand.startsWith("https://"))
					urlCommand = format("https://%s", urlCommand);
				existingContent.setUrl(urlCommand);
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

			if (sharedFlag != null)
				existingContent.setSharedFlag(sharedFlag);

			if (fileUploadId != null)
				existingContent.setFileUploadId(fileUploadId);
			else
				existingContent.setFileUploadId(null);

			if (imageFileUploadId != null)
				existingContent.setImageFileUploadId(imageFileUploadId);
			else
				existingContent.setImageFileUploadId(null);
		}

		if (durationInMinutesString != null && !ValidationUtility.isValidInteger(durationInMinutesString))
			validationException.add(new FieldError("durationInMinutes", getStrings().get("Must be an integer")));

		if (fileUploadId != null && urlCommand != null)
			validationException.add(new FieldError("url", getStrings().get("Can only specify a file url or a file upload url ")));

		if (validationException.hasErrors())
			throw validationException;

		Integer durationInMinutes = durationInMinutesString == null ? null : Integer.parseInt(durationInMinutesString);
		boolean shouldNotify = false;

		// If there is a url specified then null out the fileUploadId in case this content previously had a fileUploadId
		if (urlCommand != null)
			existingContent.setFileUploadId(null);

		getDatabase().execute("""
							 	UPDATE content SET content_type_id=?, title=?, url=?, 
							 	duration_in_minutes=?, description=?, author=?, publish_start_date=?, publish_end_date=?,
							 	publish_recurring=?, search_terms=?, shared_flag=?, file_upload_id=?, image_file_upload_id=?
								WHERE content_id=?
						""",
				existingContent.getContentTypeId(), existingContent.getTitle(), existingContent.getUrl(),
				durationInMinutes, existingContent.getDescription(), existingContent.getAuthor(), existingContent.getPublishStartDate(), existingContent.getPublishEndDate(),
				existingContent.getPublishRecurring(), existingContent.getSearchTerms(), existingContent.getSharedFlag(),existingContent.getFileUploadId(), existingContent.getImageFileUploadId(),
				existingContent.getContentId());

		AdminContent adminContent = findAdminContentByIdForInstitution(account.getInstitutionId(), command.getContentId()).orElse(null);

		applyTagsToAdminContent(adminContent, account.getInstitutionId());
		getContentService().applyInstitutionsToAdminContent(adminContent, account.getInstitutionId());

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
	public Boolean contentIsAddable(@Nonnull Content content,
																	@Nonnull InstitutionId institutionId) {
		requireNonNull(content);
		requireNonNull(institutionId);

		if (content.getOwnerInstitutionId() == institutionId)
			return true;
		else return (content.getContentStatusId().equals(ContentStatusId.LIVE) ||
				content.getContentStatusId().equals(ContentStatusId.SCHEDULED)) &&
				content.getSharedFlag() == true;

	}

	@Nonnull
	public void addContentToInstitution(@Nonnull UUID contentId,
																			@Nonnull Account account) {
		requireNonNull(contentId);
		requireNonNull(account);

		ValidationException validationException = new ValidationException();

		Boolean institutionHasContent = getDatabase().queryForObject("""
				SELECT count(*) > 0 
				FROM institution_content ic
				WHERE ic.institution_id = ? 
				AND ic.content_id = ?
				""", Boolean.class, account.getInstitutionId(), contentId).get();

		if (institutionHasContent) {
			validationException.add(new FieldError("contentId", getStrings().get("Your institution already has this content added.")));
		} else {
			Optional<Content> content = getContentService().findContentById(contentId);
			if (!content.isPresent())
				validationException.add(new FieldError("contentId", getStrings().get("Content does not exist.")));
			else if (!contentIsAddable(content.get(), account.getInstitutionId()))
				validationException.add(new FieldError("contentId", getStrings().get("Content cannot be added.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				INSERT INTO institution_content (institution_content_id, institution_id, content_id) 
				VALUES (?,?,?)
				""", UUID.randomUUID(), account.getInstitutionId(), contentId);
	}

	@Nonnull
	public void removeContentFromInstitution(@Nonnull UUID contentId,
																					 @Nonnull Account account) {
		requireNonNull(contentId);
		requireNonNull(account);

		ValidationException validationException = new ValidationException();
		Optional<Content> content = getContentService().findContentById(contentId);

		if (!content.isPresent()) {
			validationException.add(new FieldError("contentId", getStrings().get("Content is not valid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				DELETE FROM institution_content 
				WHERE institution_id=? AND content_id=?  				
				""", account.getInstitutionId(), contentId);
	}

	@Nonnull
	public void forceExpireContent(@Nonnull UUID contentId,
																 @Nonnull Account account) {
		requireNonNull(contentId);
		requireNonNull(account);

		ValidationException validationException = new ValidationException();
		Optional<Content> content = getContentService().findContentById(contentId);

		if (!content.isPresent()) {
			validationException.add(new FieldError("contentId", getStrings().get("Content is not valid.")));
		} else if (content.get().getOwnerInstitutionId() != account.getInstitutionId())
			validationException.add(new FieldError("contentId", getStrings().get("You must own the content to expire it.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				UPDATE content SET publish_end_date = NOW() - INTERVAL '1 DAY' 
				WHERE content_id=?  				
				""", contentId);
	}

	@Nonnull
	public void publishContent(@Nonnull UUID contentId,
																 @Nonnull Account account) {
		requireNonNull(contentId);
		requireNonNull(account);

		ValidationException validationException = new ValidationException();
		Optional<Content> content = getContentService().findContentById(contentId);

		if (!content.isPresent()) {
			validationException.add(new FieldError("contentId", getStrings().get("Content is not valid.")));
		} else if (content.get().getOwnerInstitutionId() != account.getInstitutionId())
			validationException.add(new FieldError("contentId", getStrings().get("You must own the content to publish it.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				UPDATE content SET published = true 
				WHERE content_id=?  				
				""", contentId);
	}
	@Nonnull
	public Optional<AdminContent> findAdminContentByIdForInstitution(@Nonnull InstitutionId institutionId, @Nonnull UUID contentId) {
		requireNonNull(institutionId);
		requireNonNull(contentId);

		AdminContent adminContent = getDatabase().queryForObject("SELECT vi.*, " +
						"(select COUNT(*) FROM " +
						" activity_tracking a WHERE " +
						" vi.content_id = CAST (a.context ->> 'contentId' AS UUID) AND " +
						" a.activity_action_id = 'VIEW' AND " +
						" activity_type_id='CONTENT') AS views " +
						"FROM v_admin_content vi " +
						"WHERE vi.content_id = ? " ,
						//TODO: Revisit this
						// "AND vi.institution_id = ? ",
				AdminContent.class, contentId).orElse(null);

		if (adminContent != null) {
			applyTagsToAdminContent(adminContent, institutionId);
			getContentService().applyInstitutionsToAdminContent(adminContent, institutionId);
		}

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
	public void deleteContentById(@Nonnull UUID contentId) {
		requireNonNull(contentId);

		getDatabase().execute("UPDATE content SET deleted_flag = true WHERE content_id = ? ", contentId);
	}

	@Nonnull
	public List<ContentStatus> findContentStatuses() {
		return getDatabase().queryForList("""
				SELECT *
				FROM content_status
				ORDER BY display_order
				""", ContentStatus.class);
	}

	@Nonnull
	public List<UUID> findContentIdsForInstitution(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);

		return getDatabase().queryForList("""
				SELECT content_id FROM institution_content WHERE institution_id=?
				""", UUID.class, institutionId);
	}

	@Nonnull
	public FileUploadResult createContentFileUpload(@Nonnull CreateFileUploadRequest request,
																									@Nonnull String storagePrefixKey) {
		requireNonNull(request);

		ValidationException validationException = new ValidationException();

		if (request.getAccountId() == null)
			validationException.add(new ValidationException.FieldError("accountId", getStrings().get("Account ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		// Make a separate instance so we don't mutate the request passed into this method
		CreateFileUploadRequest fileUploadRequest = new CreateFileUploadRequest();
		fileUploadRequest.setAccountId(request.getAccountId());
		fileUploadRequest.setContentType(request.getContentType());
		fileUploadRequest.setFilename(request.getFilename());
		fileUploadRequest.setPublicRead(false);
		fileUploadRequest.setStorageKeyPrefix(storagePrefixKey);
		fileUploadRequest.setFileUploadTypeId(request.getFileUploadTypeId());
		fileUploadRequest.setMetadata(Map.of(
				"account-id", request.getAccountId().toString()
		));

		FileUploadResult fileUploadResult = getSystemService().createFileUpload(fileUploadRequest);

		return fileUploadResult;
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

	@Nonnull
	protected ContentService getContentService() {
		return contentServiceProvider.get();
	}

	@Nonnull
	protected SystemService getSystemService() {
		return systemService;
	}
}
