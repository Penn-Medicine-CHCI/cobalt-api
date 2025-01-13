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

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.model.api.request.CreateFileUploadRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowContentRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowGroupSessionRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowImageRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowRequest;
import com.cobaltplatform.api.model.api.request.CreatePageSectionRequest;
import com.cobaltplatform.api.model.db.BackgroundColor.BackgroundColorId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Page;
import com.cobaltplatform.api.model.db.PageRow;
import com.cobaltplatform.api.model.db.PageRowContent;
import com.cobaltplatform.api.model.db.PageRowGroupSession;
import com.cobaltplatform.api.model.db.PageRowImage;
import com.cobaltplatform.api.model.db.PageSection;
import com.cobaltplatform.api.model.db.PageStatus.PageStatusId;
import com.cobaltplatform.api.model.db.PageType.PageTypeId;
import com.cobaltplatform.api.model.db.RowType.RowTypeId;
import com.cobaltplatform.api.model.service.FileUploadResult;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class PageService {
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Provider<SystemService> systemServiceProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public PageService(@Nonnull DatabaseProvider databaseProvider,
										 @Nonnull Configuration configuration,
										 @Nonnull Provider<SystemService> systemServiceProvider,
										 @Nonnull Strings strings) {
		requireNonNull(databaseProvider);
		requireNonNull(configuration);
		requireNonNull(strings);
		requireNonNull(systemServiceProvider);

		this.databaseProvider = databaseProvider;
		this.configuration = configuration;
		this.strings = strings;
		this.systemServiceProvider = systemServiceProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<Page> findPageById(@Nullable UUID pageId) {
		requireNonNull(pageId);

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_page
				WHERE page_id = ?
				""", Page.class, pageId);
	}

	@Nonnull
	private void validatePublishedPage(@Nonnull UUID pageId) {
		requireNonNull(pageId);

		ValidationException validationException = new ValidationException();
		Optional<Page> page = findPageById(pageId);

		if (!page.isPresent())
			validationException.add(new ValidationException.FieldError("pageId", getStrings().get("Could not find page.")));
	}

	@Nonnull
	public UUID createPage(@Nonnull CreatePageRequest request) {
		requireNonNull(request);

		String name = trimToNull(request.getName());
		String urlName = trimToNull(request.getUrlName());
		PageTypeId pageTypeId = request.getPageTypeId();
		PageStatusId pageStatusId = request.getPageStatusId();
		String headline = trimToNull(request.getHeadline());
		String description = trimToNull(request.getDescription());
		UUID imageFileUploadId = request.getImageFileUploadId();
		String imageAltText = trimToNull(request.getImageAltText());
		UUID pageId = UUID.randomUUID();
		UUID createdByAccountId = request.getCreatedByAccountId();
		InstitutionId institutionId = request.getInstitutionId();
		Instant publishedDate = null;
		ValidationException validationException = new ValidationException();

		if (name == null)
			validationException.add(new ValidationException.FieldError("name", getStrings().get("Name is required.")));

		if (urlName == null)
			validationException.add(new ValidationException.FieldError("name", getStrings().get("URL is required.")));

		if (pageTypeId == null)
			validationException.add(new ValidationException.FieldError("pageTypeId", getStrings().get("Page Type is required.")));

		if (pageStatusId == null)
			validationException.add(new ValidationException.FieldError("pageStatusId", getStrings().get("Page Status is required.")));

		if (institutionId == null)
			validationException.add(new ValidationException.FieldError("institutionId", getStrings().get("Institution is required.")));

		if (validationException.hasErrors())
			throw validationException;

		if (pageStatusId.equals(PageStatusId.LIVE)) {
			validatePublishedPage(pageId);
			publishedDate = Instant.now();
		}

		getDatabase().execute("""
						INSERT INTO page
						  (page_id, name, url_name, page_type_id, page_status_id, headline, description, image_file_upload_id, image_alt_text, 
						  published_date, institution_id, created_by_account_id)
						VALUES
						  (?,?,?,?,?,?,?,?,?,?,?,?)   
						""", pageId, name, urlName, pageTypeId, pageStatusId, headline, description, imageFileUploadId, imageAltText,
				publishedDate, institutionId, createdByAccountId);

		return pageId;
	}

	@Nonnull
	public Optional<PageSection> findPageSectionById(@Nullable UUID pageSectionId) {
		requireNonNull(pageSectionId);

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_page_section
				WHERE page_section_id = ?
				""", PageSection.class, pageSectionId);
	}

	@Nonnull
	public UUID createPageSection(@Nonnull CreatePageSectionRequest request) {
		requireNonNull(request);

		UUID pageId = request.getPageId();
		String name = trimToNull(request.getName());
		String headline = trimToNull(request.getHeadline());
		String description = trimToNull(request.getDescription());
		BackgroundColorId backgroundColorId = request.getBackgroundColorId();
		UUID pageSectionId = UUID.randomUUID();
		UUID createdByAccountId = request.getCreatedByAccountId();
		PageStatusId pageStatusId = request.getPageStatusId();
		Integer displayOrder = request.getDisplayOrder();
		ValidationException validationException = new ValidationException();

		if (pageId == null)
			validationException.add(new ValidationException.FieldError("pageId", getStrings().get("Page is required.")));
		if (pageStatusId == null)
			validationException.add(new ValidationException.FieldError("pageStatusId", getStrings().get("Page Status is required.")));
		if (backgroundColorId == null)
			validationException.add(new ValidationException.FieldError("backgroundColorId", getStrings().get("Background Color is required.")));

		if (validationException.hasErrors())
			throw validationException;

		if (pageStatusId.equals(PageStatusId.LIVE))
			validatePublishedPage(pageId);

		if (displayOrder == null)
			displayOrder = getDatabase().queryForObject("""
					SELECT COALESCE(MAX(display_order) + 1, 0)
					FROM v_page_section
					WHERE page_id = ?
					""", Integer.class, pageId).get();

		getDatabase().execute("""
				INSERT INTO page_section
				  (page_section_id, page_id, name, headline, description, background_color_id, created_by_account_id, display_order)
				VALUES
				  (?,?,?,?,?,?,?,?)   
				""", pageSectionId, pageId, name, headline, description, backgroundColorId, createdByAccountId, displayOrder);

		return pageSectionId;
	}

	@Nonnull
	public Optional<PageRow> findPageRowById(@Nullable UUID pageRowId) {
		requireNonNull(pageRowId);

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_page_row
				WHERE page_row_id = ?
				""", PageRow.class, pageRowId);
	}

	@Nonnull
	public UUID createPageRow(@Nonnull CreatePageRowRequest request) {
		requireNonNull(request);

		UUID pageSectionId = request.getPageSectionId();
		UUID pageRowId = UUID.randomUUID();
		RowTypeId rowTypeId = request.getRowTypeId();
		UUID createdByAccountId = request.getCreatedByAccountId();
		Integer displayOrder = request.getDisplayOrder();
		PageStatusId pageStatusId = request.getPageStatusId();
		ValidationException validationException = new ValidationException();
		Optional<PageSection> pageSection = findPageSectionById(pageSectionId);

		if (pageSectionId == null)
			validationException.add(new ValidationException.FieldError("pageId", getStrings().get("Page is required.")));
		if (pageStatusId == null)
			validationException.add(new ValidationException.FieldError("pageStatusId", getStrings().get("Page Status is required.")));
		if (rowTypeId == null)
			validationException.add(new ValidationException.FieldError("rowTypeId", getStrings().get("Row Type is require.")));
		if (pageSection == null)
			validationException.add(new ValidationException.FieldError("pageSection", getStrings().get("Could not find Page Section")));

		if (validationException.hasErrors())
			throw validationException;

		if (pageStatusId.equals(PageStatusId.LIVE))
			validatePublishedPage(pageSection.get().getPageId());

		if (displayOrder == null)
			displayOrder = getDatabase().queryForObject("""
					SELECT COALESCE(MAX(display_order) + 1, 0)
					FROM v_page_row
					WHERE page_section_id = ?
					""", Integer.class, pageSectionId).get();

		getDatabase().execute("""
				INSERT INTO page_row
				  (page_row_id, page_section_id, row_type_id, created_by_account_id, display_order)
				VALUES
				  (?,?,?,?,?)   
				""", pageRowId, pageSectionId, rowTypeId, createdByAccountId, displayOrder);

		return pageRowId;
	}

	@Nonnull
	public Optional<PageRowImage> findPageRowImageById(@Nullable UUID pageRowImageId) {
		requireNonNull(pageRowImageId);

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_page_row_image
				WHERE page_row_image_id = ?
				""", PageRowImage.class, pageRowImageId);
	}

	@Nonnull
	public UUID createPageRowImage(@Nonnull CreatePageRowImageRequest request) {
		requireNonNull(request);

		UUID pageRowImageId = UUID.randomUUID();
		UUID pageRowId = request.getPageRowId();
		String headline = trimToNull(request.getHeadline());
		String description = trimToNull(request.getDescription());
		UUID imageFileUploadId = request.getImageFileUploadId();
		String imageAltText = trimToNull(request.getImageAltText());
		UUID createdByAccountId = request.getCreatedByAccountId();
		Integer displayOrder = request.getDisplayOrder();

		ValidationException validationException = new ValidationException();

		if (pageRowId == null)
			validationException.add(new ValidationException.FieldError("pageRowId", getStrings().get("Could not find Page Row")));

		if (validationException.hasErrors())
			throw validationException;

		if (displayOrder == null)
			displayOrder = getDatabase().queryForObject("""
					SELECT COALESCE(MAX(display_order) + 1, 0)
					FROM v_page_row_image
					WHERE page_row_id = ?
					""", Integer.class, pageRowId).get();

		getDatabase().execute("""
				INSERT INTO page_row_image
				  (page_row_image_id, page_row_id, headline, description, image_file_upload_id, image_alt_text, display_order, created_by_account_id)
				VALUES
				  (?,?,?,?,?,?,?,?)   
				""", pageRowImageId, pageRowId, headline, description, imageFileUploadId, imageAltText, displayOrder, createdByAccountId);

		return pageRowImageId;
	}

	@Nonnull
	public Optional<PageRowContent> findPageRowContentById(@Nullable UUID pageRowContentId) {
		requireNonNull(pageRowContentId);

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_page_row_content
				WHERE page_row_content_id = ?
				""", PageRowContent.class, pageRowContentId);
	}

	@Nonnull
	public UUID createPageRowContent(@Nonnull CreatePageRowContentRequest request) {
		requireNonNull(request);

		UUID pageRowContentId = UUID.randomUUID();
		UUID pageRowId = request.getPageRowId();
		UUID contentId = request.getContentId();
		UUID createdByAccountId = request.getCreatedByAccountId();
		Integer displayOrder = request.getDisplayOrder();

		ValidationException validationException = new ValidationException();

		if (pageRowId == null)
			validationException.add(new ValidationException.FieldError("pageRowId", getStrings().get("Could not find Page Row")));

		if (validationException.hasErrors())
			throw validationException;

		if (displayOrder == null)
			displayOrder = getDatabase().queryForObject("""
					SELECT COALESCE(MAX(display_order) + 1, 0)
					FROM v_page_row_content
					WHERE page_row_id = ?
					""", Integer.class, pageRowId).get();

		getDatabase().execute("""
				INSERT INTO page_row_content
				  (page_row_content_id, page_row_id, content_id, display_order, created_by_account_id)
				VALUES
				  (?,?,?,?,?)   
				""", pageRowContentId, pageRowId, contentId, displayOrder, createdByAccountId);

		return pageRowContentId;
	}

	@Nonnull
	public Optional<PageRowGroupSession> findPageRowGroupSessionById(@Nullable UUID pageRowGroupSessionId) {
		requireNonNull(pageRowGroupSessionId);

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_page_row_group_session
				WHERE page_row_group_session_id = ?
				""", PageRowGroupSession.class, pageRowGroupSessionId);
	}

	@Nonnull
	public UUID createPageRowGroupSession(@Nonnull CreatePageRowGroupSessionRequest request) {
		requireNonNull(request);

		UUID pageRowGroupSessionId = UUID.randomUUID();
		UUID pageRowId = request.getPageRowId();
		UUID groupSessionId = request.getGroupSessionId();
		UUID createdByAccountId = request.getCreatedByAccountId();
		Integer displayOrder = request.getDisplayOrder();

		ValidationException validationException = new ValidationException();

		if (pageRowId == null)
			validationException.add(new ValidationException.FieldError("pageRowId", getStrings().get("Could not find Page Row")));

		if (validationException.hasErrors())
			throw validationException;

		if (displayOrder == null)
			displayOrder = getDatabase().queryForObject("""
					SELECT COALESCE(MAX(display_order) + 1, 0)
					FROM v_page_row_group_session
					WHERE page_row_id = ?
					""", Integer.class, pageRowId).get();

		getDatabase().execute("""
				INSERT INTO page_row_group_session
				  (page_row_group_session_id, page_row_id, group_session_id, display_order, created_by_account_id)
				VALUES
				  (?,?,?,?,?)   
				""", pageRowGroupSessionId, pageRowId, groupSessionId, displayOrder, createdByAccountId);

		return pageRowGroupSessionId;
	}

	@Nonnull
	public FileUploadResult createPageFileUpload(@Nonnull CreateFileUploadRequest request,
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
		fileUploadRequest.setPublicRead(true);
		fileUploadRequest.setStorageKeyPrefix(storagePrefixKey);
		fileUploadRequest.setFileUploadTypeId(request.getFileUploadTypeId());
		fileUploadRequest.setMetadata(Map.of(
				"account-id", request.getAccountId().toString()
		));
		fileUploadRequest.setFilesize(request.getFilesize());

		FileUploadResult fileUploadResult = getSystemService().createFileUpload(fileUploadRequest);

		return fileUploadResult;
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}

	@Nonnull
	public SystemService getSystemService() {
		return systemServiceProvider.get();
	}
}
