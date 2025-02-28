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
import com.cobaltplatform.api.model.api.request.DuplicatePageRequest;
import com.cobaltplatform.api.model.api.request.CreateFileUploadRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowColumnRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowContentRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowCustomThreeColumnRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowCustomTwoColumnRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowGroupSessionRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowCustomOneColumnRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowTagGroupRequest;
import com.cobaltplatform.api.model.api.request.CreatePageSectionRequest;
import com.cobaltplatform.api.model.api.request.FindPagesRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageHeroRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageRowColumnRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageRowContentRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageRowCustomOneColumnRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageRowCustomThreeColumnRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageRowCustomTwoColumnRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageRowDisplayOrderRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageRowGroupSessionRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageRowTagGroupRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageSectionDisplayOrderRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageSectionRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageSettingsRequest;
import com.cobaltplatform.api.model.db.BackgroundColor.BackgroundColorId;
import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.db.GroupSession;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Page;
import com.cobaltplatform.api.model.db.PageRow;
import com.cobaltplatform.api.model.db.PageRowContent;
import com.cobaltplatform.api.model.db.PageRowGroupSession;
import com.cobaltplatform.api.model.db.PageRowColumn;
import com.cobaltplatform.api.model.db.PageRowTagGroup;
import com.cobaltplatform.api.model.db.PageSection;
import com.cobaltplatform.api.model.db.PageStatus.PageStatusId;
import com.cobaltplatform.api.model.db.RowType.RowTypeId;
import com.cobaltplatform.api.model.db.SiteLocation.SiteLocationId;
import com.cobaltplatform.api.model.db.TagGroup;
import com.cobaltplatform.api.model.service.FileUploadResult;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.model.service.PageUrlValidationResult;
import com.cobaltplatform.api.model.service.PageWithTotalCount;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationUtility;
import com.cobaltplatform.api.util.WebUtility;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
import com.soklet.web.exception.NotFoundException;
import org.apache.arrow.flatbuf.Null;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.cobaltplatform.api.util.ValidationUtility.isValidUUID;
import static com.cobaltplatform.api.util.ValidationUtility.isValidUrlSubdirectory;
import static java.lang.String.format;
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
	public Optional<Page> findPageById(@Nullable Object pageIdentifier,
																		 @Nullable InstitutionId institutionId,
																		 @Nullable Boolean includeUnpublished) {
		requireNonNull(pageIdentifier);
		requireNonNull(institutionId);
		requireNonNull(includeUnpublished);

		if (pageIdentifier == null || institutionId == null)
			return Optional.empty();
		Page page = null;
		UUID pageId = null;

		if (pageIdentifier instanceof UUID)
			pageId = (UUID) pageIdentifier;
		else if (pageIdentifier instanceof String && ValidationUtility.isValidUUID((String) pageIdentifier))
			pageId = UUID.fromString((String) pageIdentifier);

		StringBuilder query = new StringBuilder("SELECT * FROM v_page WHERE institution_id =? ");
		List<Object> parameters = new ArrayList<>();
		parameters.add(institutionId);

		if (!includeUnpublished) {
			query.append("AND page_status_id = ? ");
			parameters.add(PageStatusId.LIVE);
		}

		if (pageId != null) {
			query.append("AND page_id = ? ");
			parameters.add(pageId);
		} else if (pageIdentifier instanceof String) {
			query.append("AND url_name = ? ");
			parameters.add(pageIdentifier);
		}

		page = getDatabase().queryForObject(query.toString(), Page.class, parameters.toArray()).orElse(null);

		if (page == null)
			return Optional.empty();

		return Optional.of(page);
	}

	@Nonnull
	private void validatePublishedPage(@Nonnull UUID pageId,
																		 @Nonnull InstitutionId institutionId) {
		requireNonNull(pageId);
		requireNonNull(institutionId);

		ValidationException validationException = new ValidationException();
		Optional<Page> page = findPageById(pageId, institutionId, true);

		if (!page.isPresent())
			throw new NotFoundException();

		String pageHeadline = trimToNull(page.get().getHeadline());
		String pageDescription = trimToNull(page.get().getDescription());
		UUID pageImageFileUploadId = page.get().getImageFileUploadId();
		String imageAltText = trimToNull(page.get().getImageAltText());
		Map<String, Object> metadata = new HashMap<>();

		if (pageHeadline == null)
			validationException.add(new ValidationException.FieldError("pageHeadline", getStrings().get(format("A Headline is required for Page %s.\n", page.get().getName()))));
		if (pageDescription == null)
			validationException.add(new ValidationException.FieldError("pageDescription", getStrings().get(format("A Description is required for Page %s.\n", page.get().getName()))));
		if (pageImageFileUploadId == null)
			validationException.add(new ValidationException.FieldError("pageImageFileUploadId", getStrings().get(format("An Image is required for Page %s.\n", page.get().getName()))));
		if (imageAltText == null)
			validationException.add(new ValidationException.FieldError("imageAltText", getStrings().get(format("Image Alt Text is required for Page %s.\n", page.get().getName()))));

		if (validationException.hasErrors()) {
			metadata.put("pageId", pageId);
		}

		List<PageSection> pageSections = findPageSectionsByPageId(pageId);

		if (pageSections.size() == 0)
			validationException.add(new ValidationException.FieldError("pageSection", getStrings().get(format("At least one section is required for Page %s.\n", page.get().getName()))));

		for (PageSection pageSection : pageSections) {
			//Name and background color are required when creating or updating a page section so no need to validate them
			String headline = trimToNull(pageSection.getHeadline());
			String description = trimToNull(pageSection.getDescription());

			if (headline == null)
				validationException.add(new ValidationException.FieldError("headline", getStrings().get(format("A headline is required for Section %s.\n", pageSection.getName()))));
			if (description == null)
				validationException.add(new ValidationException.FieldError("description", getStrings().get(format("A description is required for Section %s.\n", pageSection.getName()))));

			if (validationException.hasErrors() && metadata.isEmpty()) {
				metadata.put("sectionId", pageSection.getPageSectionId());
			}

			List<PageRow> pageRows = findPageRowsBySectionId(pageSection.getPageSectionId());

			if (pageRows.size() == 0) {
				validationException.add(new ValidationException.FieldError("pageRows", getStrings().get(format("At least one row is required for Section %s.\n", pageSection.getName()))));
				if (validationException.hasErrors() && metadata.isEmpty()) {
					metadata.put("sectionId", pageSection.getPageSectionId());
				}
			} else {
				for (PageRow pageRow : pageRows) {
					if (pageRow.getRowTypeId().equals(RowTypeId.RESOURCES)) {
						List<PageRowContent> pageRowContent = findPageRowContentByPageRowId(pageRow.getPageRowId());
						if (pageRowContent.isEmpty())
							validationException.add(new ValidationException.FieldError("pageRow", getStrings().get(format("At least one Resource is required for the Resource row in Section %s.\n", pageSection.getName()))));
					} else if (pageRow.getRowTypeId().equals(RowTypeId.GROUP_SESSIONS)) {
						List<PageRowGroupSession> pageRowGroupSessions = findPageRowGroupSessionByPageRowId(pageRow.getPageRowId());
						if (pageRowGroupSessions.isEmpty())
							validationException.add(new ValidationException.FieldError("pageRow", getStrings().get(format("At least one Group Session is required for the Resource row in Section %s.\n", pageSection.getName()))));
					} else if (pageRow.getRowTypeId().equals(RowTypeId.ONE_COLUMN_IMAGE) || pageRow.getRowTypeId().equals(RowTypeId.TWO_COLUMN_IMAGE)
							|| pageRow.getRowTypeId().equals(RowTypeId.THREE_COLUMN_IMAGE)) {
						Optional<PageRowColumn> pageRowColumn = Optional.empty();
						pageRowColumn = findPageRowColumnByPageRowIdAndDisplayOrder(pageRow.getPageRowId(), 0);

						if (pageRowColumn.isEmpty())
							validationException.add(new ValidationException.FieldError("pageRowColumn", getStrings().get(format("Column 1 not present for Custom row in Section %s.\n", pageSection.getName()))));
						else {
							validatePageRowColum(pageRowColumn.get(), pageSection, validationException);
						}

						if (pageRow.getRowTypeId().equals(RowTypeId.TWO_COLUMN_IMAGE) || pageRow.getRowTypeId().equals(RowTypeId.THREE_COLUMN_IMAGE)) {
							pageRowColumn = findPageRowColumnByPageRowIdAndDisplayOrder(pageRow.getPageRowId(), 1);
							if (pageRowColumn.isEmpty())
								validationException.add(new ValidationException.FieldError("pageRowColumn", getStrings().get(format("Column 2 not present for Custom row in Section %s.\n", pageSection.getName()))));
							else {
								validatePageRowColum(pageRowColumn.get(), pageSection, validationException);
							}
						}

						if (pageRow.getRowTypeId().equals(RowTypeId.THREE_COLUMN_IMAGE)) {
							pageRowColumn = findPageRowColumnByPageRowIdAndDisplayOrder(pageRow.getPageRowId(), 2);
							if (pageRowColumn.isEmpty())
								validationException.add(new ValidationException.FieldError("pageRowColumn", getStrings().get(format("Column 3 not present for Custom row in Section %s.\n", pageSection.getName()))));
							else {
								validatePageRowColum(pageRowColumn.get(), pageSection, validationException);
							}
						}
					}

					if (validationException.hasErrors() && metadata.isEmpty()) {
						metadata.put("sectionId", pageSection.getPageSectionId());
						metadata.put("rowId", pageRow.getPageRowId());
					}
				}
			}
		}

		validationException.setMetadata(metadata);

		if (validationException.hasErrors())
			throw validationException;
	}

	@Nonnull
	private void validatePageRowColum(@Nonnull PageRowColumn pageRowColumn,
																		@Nonnull PageSection pageSection,
																		@Nonnull ValidationException validationException) {
		requireNonNull(pageRowColumn);
		requireNonNull(validationException);
		requireNonNull(pageSection);

		String headline = trimToNull(pageRowColumn.getHeadline());
		String description = trimToNull(pageRowColumn.getDescription());
		String imageAltText = trimToNull(pageRowColumn.getImageAltText());
		UUID imageFileUploadId = pageRowColumn.getImageFileUploadId();
		String pageSectionName = pageSection.getName();
		Integer itemNumber = pageRowColumn.getColumnDisplayOrder() + 1;

		if (headline == null)
			validationException.add(new ValidationException.FieldError("headline", getStrings().get(format("A Headline is required for Item %s in the Content Row in Section %s.\n", itemNumber, pageSectionName))));
		if (description == null)
			validationException.add(new ValidationException.FieldError("description", getStrings().get(format("A Description is required for Item %s in the Content Row in Section %s.\n", itemNumber, pageSectionName))));
		if (imageAltText == null)
			validationException.add(new ValidationException.FieldError("imageAltText", getStrings().get(format("Image Alt Text is required for Item %s in the Content Row in Section %s.\n", itemNumber, pageSectionName))));
		if (imageFileUploadId == null)
			validationException.add(new ValidationException.FieldError("imageFileUploadId", getStrings().get(format("Image is required for Item %s in the Content Row in Section %s.\n", itemNumber, pageSectionName))));
	}

	@Nonnull
	public UUID createPage(@Nonnull CreatePageRequest request) {
		requireNonNull(request);

		String name = trimToNull(request.getName());
		String urlName = request.getUrlName() == null ? null : WebUtility.normalizeUrlName(request.getUrlName()).orElse(null);
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
		else if (nameExistsForInstitutionId(name, institutionId, null))
			validationException.add(new ValidationException.FieldError("name", getStrings().get("Name already exists.")));

		if (institutionId == null)
			validationException.add(new ValidationException.FieldError("institutionId", getStrings().get("Institution is required.")));

		if (urlName == null)
			validationException.add(new ValidationException.FieldError("urlName", getStrings().get("Friendly URL name is required.")));
		else if (!isValidUrlSubdirectory(urlName))
			validationException.add(new ValidationException.FieldError("urlName", getStrings().get("Not a valid Friendly URL")));
		else if (urlNameExistsForInstitutionId(urlName, institutionId, pageId))
			validationException.add(new ValidationException.FieldError("urlName", getStrings().get("Friendly URL name is already in use.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
						INSERT INTO page
						  (page_id, name, url_name, page_status_id, headline, description, image_file_upload_id, image_alt_text, 
						  published_date, institution_id, created_by_account_id)
						VALUES
						  (?,?,?,?,?,?,?,?,?,?,?)   
						""", pageId, name, urlName, PageStatusId.DRAFT, headline, description, imageFileUploadId, imageAltText,
				publishedDate, institutionId, createdByAccountId);

		return pageId;
	}

	@Nonnull
	public UUID updatePageHero(@Nonnull UpdatePageHeroRequest request) {
		requireNonNull(request);

		String headline = trimToNull(request.getHeadline());
		String description = trimToNull(request.getDescription());
		String imageFileUploadIdString = request.getImageFileUploadId();
		String imageAltText = trimToNull(request.getImageAltText());
		UUID pageId = request.getPageId();
		UUID imageFileUploadId = null;

		if (isValidUUID(imageFileUploadIdString))
			imageFileUploadId = UUID.fromString(imageFileUploadIdString);

		getDatabase().execute("""
				UPDATE page SET
				  headline=?, description=?, image_file_upload_id=?, image_alt_text=?
				WHERE page_id=?   
				""", headline, description, imageFileUploadId, imageAltText, pageId);

		return pageId;
	}

	@Nonnull
	public UUID publishPage(@Nonnull UUID pageId,
													@Nonnull InstitutionId institutionId) {
		requireNonNull(pageId);
		requireNonNull(institutionId);

		Optional<Page> page = findPageById(pageId, institutionId, true);

		validatePublishedPage(pageId, institutionId);

		//If one exists, update the parent page to draft and delete it
		if (page.get().getParentPageId() != null)
			getDatabase().execute("""
					UPDATE page 
					SET page_status_id = ?, deleted_flag=true
					WHERE page_id = ?""", PageStatusId.DRAFT, page.get().getParentPageId());


		getDatabase().execute("""
				UPDATE page SET
				page_status_id=?, published_date=NOW()
				WHERE page_id = ?
				AND institution_id = ?
				""", PageStatusId.LIVE, pageId, institutionId);

		return pageId;
	}

	@Nonnull
	public UUID unpublishPage(@Nonnull UUID pageId,
														@Nonnull InstitutionId institutionId) {
		requireNonNull(pageId);
		requireNonNull(institutionId);

		getDatabase().execute("""
				UPDATE page SET
				page_status_id=?,
				published_date=null
				WHERE page_id=?					   
				AND institution_id = ?
				""", PageStatusId.DRAFT, pageId, institutionId);

		return pageId;
	}

	@Nonnull
	public PageUrlValidationResult findPageUrlValidationResults(@Nonnull String urlName,
																															@Nonnull InstitutionId institutionId,
																															@Nullable UUID pageId) {
		requireNonNull(urlName);
		requireNonNull(institutionId);

		PageUrlValidationResult result = new PageUrlValidationResult();

		boolean urlNameContainsIllegalCharacters = WebUtility.urlNameContainsIllegalCharacters(urlName.trim());
		urlName = WebUtility.normalizeUrlName(urlName).orElse("");

		if (urlNameContainsIllegalCharacters) {
			result.setAvailable(false);
			result.setRecommendation(recommendedUrlNameForUrlName(urlName, institutionId, pageId));
		} else if (!urlNameExistsForInstitutionId(urlName, institutionId, pageId)) {
			result.setAvailable(true);
			result.setRecommendation(urlName);
		} else {
			result.setAvailable(false);
			result.setRecommendation(recommendedUrlNameForUrlName(urlName, institutionId, pageId));
		}

		return result;
	}

	@Nonnull
	protected Boolean nameExistsForInstitutionId(@Nonnull String name,
																							 @Nonnull InstitutionId institutionId,
																							 @Nullable UUID pageId) {
		requireNonNull(name);
		requireNonNull(institutionId);

		List<Object> parameters = new ArrayList<>();
		StringBuilder query = new StringBuilder("""
				SELECT COUNT(*) > 0
				FROM v_page p
				WHERE p.institution_id = ?
				AND LOWER(p.name) = LOWER(?)
				""");

		parameters.add(institutionId);
		parameters.add(name);

		if (pageId != null) {
			query.append(" AND p.page_id != ?");
			parameters.add(pageId);
		}

		return getDatabase().queryForObject(query.toString(), Boolean.class, parameters.toArray()).get();
	}


	@Nonnull
	protected Boolean urlNameExistsForInstitutionId(@Nonnull String urlName,
																									@Nonnull InstitutionId institutionId,
																									@Nullable UUID pageId) {
		requireNonNull(urlName);
		requireNonNull(institutionId);

		urlName = WebUtility.normalizeUrlName(urlName).orElse("");

		List<Object> parameters = new ArrayList<>();
		StringBuilder query = new StringBuilder("""
				SELECT COUNT(*) > 0
				FROM v_page p
				WHERE p.institution_id = ?
				AND LOWER(p.url_name) = LOWER(?)				
				""");

		parameters.add(institutionId);
		parameters.add(urlName);

		if (pageId != null) {
			query.append(" AND p.page_id != ?");
			parameters.add(pageId);
		}

		return getDatabase().queryForObject(query.toString(), Boolean.class, parameters.toArray()).get();
	}

	@Nonnull
	protected String recommendedUrlNameForUrlName(@Nonnull String urlName,
																								@Nonnull InstitutionId institutionId,
																								@Nullable UUID pageId) {
		requireNonNull(urlName);
		requireNonNull(institutionId);

		String recommendedUrlName = urlName;
		boolean suggestedUrlAvailable = !urlNameExistsForInstitutionId(recommendedUrlName, institutionId, pageId);
		int urlSuffix = 1;

		while (!suggestedUrlAvailable) {
			recommendedUrlName = format("%s-%s", urlName, urlSuffix);
			suggestedUrlAvailable = !urlNameExistsForInstitutionId(recommendedUrlName, institutionId, pageId);
			urlSuffix++;
		}

		return recommendedUrlName;
	}

	@Nonnull
	public UUID updatePageSettings(@Nonnull UpdatePageSettingsRequest request) {
		requireNonNull(request);

		String name = trimToNull(request.getName());
		UUID pageId = request.getPageId();
		ValidationException validationException = new ValidationException();

		if (name == null)
			validationException.add(new ValidationException.FieldError("name", getStrings().get("Name is required.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				UPDATE page SET
				name=?
				WHERE page_id=?					   
				""", name, pageId);

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
	public List<PageSection> findPageSectionsByPageId(@Nullable UUID pageId) {
		requireNonNull(pageId);

		return getDatabase().queryForList("""
				SELECT *
				FROM v_page_section
				WHERE page_id = ?
				""", PageSection.class, pageId);
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
		InstitutionId institutionId = request.getInstitutionId();

		ValidationException validationException = new ValidationException();

		if (pageId == null)
			validationException.add(new ValidationException.FieldError("pageId", getStrings().get("Page is required.")));
		if (backgroundColorId == null)
			validationException.add(new ValidationException.FieldError("backgroundColorId", getStrings().get("Background Color is required.")));
		if (institutionId == null)
			validationException.add(new ValidationException.FieldError("institutionId", getStrings().get("Institution ID is required.")));
		if (name == null)
			validationException.add(new ValidationException.FieldError("name", getStrings().get("Name is required.")));

		if (validationException.hasErrors())
			throw validationException;

		Integer displayOrder = getDatabase().queryForObject("""
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
	public void updatePageSectionDisplayOrder(@Nonnull UpdatePageSectionDisplayOrderRequest request) {
		requireNonNull(request);

		int displayOrder = 0;

		for (UUID pageSectionId : request.getPageSectionIds()) {
			getDatabase().execute("""
					UPDATE page_section
					SET display_order = ?
					WHERE page_section_id = ?
					""", displayOrder, pageSectionId);
			displayOrder++;
		}
	}

	@Nonnull
	public void updatePageRowDisplayOrder(@Nonnull UpdatePageRowDisplayOrderRequest request) {
		requireNonNull(request);

		int displayOrder = 0;

		for (UUID pageRowId : request.getPageRowIds()) {
			getDatabase().execute("""
					UPDATE page_row
					SET display_order = ?
					WHERE page_row_id = ?
					""", displayOrder, pageRowId);
			displayOrder++;
		}
	}

	@Nonnull
	public UUID updatePageSection(@Nonnull UpdatePageSectionRequest request) {
		requireNonNull(request);

		UUID pageSectionId = request.getPageSectionId();
		String name = trimToNull(request.getName());
		String headline = trimToNull(request.getHeadline());
		String description = trimToNull(request.getDescription());
		BackgroundColorId backgroundColorId = request.getBackgroundColorId();
		Integer displayOrder = request.getDisplayOrder();

		ValidationException validationException = new ValidationException();

		Optional<PageSection> pageSection = findPageSectionById(pageSectionId);

		if (!pageSection.isPresent())
			validationException.add(new ValidationException.FieldError("pageSection", getStrings().get("Could not find page section.")));
		if (backgroundColorId == null)
			validationException.add(new ValidationException.FieldError("backgroundColorId", getStrings().get("Background Color is required.")));
		if (name == null)
			validationException.add(new ValidationException.FieldError("name", getStrings().get("Name is required.")));
		if (displayOrder == null)
			validationException.add(new ValidationException.FieldError("displayOrder", getStrings().get("Display Order is required.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
						UPDATE page_section
						SET display_order = 
						CASE
								WHEN display_order >= ? AND display_order < ? THEN display_order + 1
								WHEN display_order <= ? AND display_order > ? THEN display_order - 1
								ELSE display_order
						END
						WHERE page_section_id != ?
						""", displayOrder, pageSection.get().getDisplayOrder(),
				displayOrder, pageSection.get().getDisplayOrder(), pageSectionId);

		getDatabase().execute("""
				UPDATE page_section SET
				  name=?, headline=?, description=?, background_color_id=?,
				  display_order=?
				WHERE page_section_id=?				   
				""", name, headline, description, backgroundColorId, displayOrder, pageSectionId);

		return pageSectionId;
	}

	@Nonnull
	public void deletePageSection(@Nullable UUID pageSectionId) {
		requireNonNull(pageSectionId);

		ValidationException validationException = new ValidationException();
		Optional<PageSection> pageSection = findPageSectionById(pageSectionId);

		if (!pageSection.isPresent())
			validationException.add(new ValidationException.FieldError("pageSection", getStrings().get("Could not find page section.")));
		if (validationException.hasErrors())
			throw validationException;

		UUID pageId = pageSection.get().getPageId();

		getDatabase().execute("""
				DELETE FROM page_row_column
				WHERE page_row_id IN 
				(SELECT pr.page_row_id
				FROM page_row pr
				WHERE pr.page_section_id =?)
				""", pageSectionId);

		getDatabase().execute("""
				DELETE FROM page_row_group_session
				WHERE page_row_id IN 
				(SELECT pr.page_row_id
				FROM page_row pr
				WHERE pr.page_section_id =?)
				""", pageSectionId);

		getDatabase().execute("""
				DELETE FROM page_row_content
				WHERE page_row_id IN 
				(SELECT pr.page_row_id
				FROM page_row pr
				WHERE pr.page_section_id =?) 
				""", pageSectionId);

		getDatabase().execute("""
				DELETE FROM page_row_tag_group
				WHERE page_row_id IN 
				(SELECT pr.page_row_id
				FROM page_row pr
				WHERE pr.page_section_id =?) 
				""", pageSectionId);

		getDatabase().execute("""
				DELETE FROM page_row
				WHERE page_section_id=? 
				""", pageSectionId);

		getDatabase().execute("""
				DELETE FROM page_section 				
				WHERE page_section_id = ?				
				""", pageSectionId);

		getDatabase().execute("""
				WITH ordered AS (
				  SELECT
				    page_section_id,
				    row_number() OVER (PARTITION BY page_id ORDER BY display_order) - 1 AS new_order
				  FROM page_section
				  WHERE page_id=?
				)
				UPDATE page_section AS ps
				SET display_order = ordered.new_order
				FROM ordered
				WHERE ps.page_section_id = ordered.page_section_id
				AND page_id=?;
				""", pageId, pageId);
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
	public void deletePage(@Nullable UUID pageId,
												 @Nullable InstitutionId institutionId) {
		requireNonNull(pageId);
		requireNonNull(institutionId);

		getDatabase().execute("""
				UPDATE page
				SET deleted_flag = TRUE
				WHERE page_id = ?
				AND institution_id = ?
				""", pageId, institutionId);
	}

	@Nonnull
	public List<PageRow> findPageRowsBySectionId(@Nullable UUID pageSectionId) {
		requireNonNull(pageSectionId);

		return getDatabase().queryForList("""
				SELECT *
				FROM v_page_row
				WHERE page_section_id = ?
				""", PageRow.class, pageSectionId);
	}

	@Nonnull
	public UUID createPageRow(@Nonnull CreatePageRowRequest request) {
		requireNonNull(request);

		UUID pageSectionId = request.getPageSectionId();
		UUID pageRowId = UUID.randomUUID();
		RowTypeId rowTypeId = request.getRowTypeId();
		UUID createdByAccountId = request.getCreatedByAccountId();
		Integer displayOrder = request.getDisplayOrder();
		ValidationException validationException = new ValidationException();
		Optional<PageSection> pageSection = findPageSectionById(pageSectionId);

		if (pageSectionId == null)
			validationException.add(new ValidationException.FieldError("pageId", getStrings().get("Page is required.")));
		if (rowTypeId == null)
			validationException.add(new ValidationException.FieldError("rowTypeId", getStrings().get("Row Type is require.")));
		if (pageSection == null)
			validationException.add(new ValidationException.FieldError("pageSection", getStrings().get("Could not find Page Section")));

		if (validationException.hasErrors())
			throw validationException;

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
	public void deletePageRow(@Nullable UUID pageRowId) {
		requireNonNull(pageRowId);
		ValidationException validationException = new ValidationException();

		Optional<PageRow> pageRow = findPageRowById(pageRowId);

		if (!pageRow.isPresent())
			validationException.add(new ValidationException.FieldError("pageRow", getStrings().get("Could not find page row.")));
		if (validationException.hasErrors())
			throw validationException;

		UUID pageSectionId = pageRow.get().getPageSectionId();

		getDatabase().execute("""
				DELETE FROM page_row_column
				WHERE page_row_id =?
				""", pageRowId);

		getDatabase().execute("""
				DELETE FROM page_row_group_session
				WHERE page_row_id =?
				""", pageRowId);

		getDatabase().execute("""
				DELETE FROM page_row_content
				WHERE page_row_id =? 
				""", pageRowId);

		getDatabase().execute("""
				DELETE FROM page_row_tag_group
				WHERE page_row_id =? 
				""", pageRowId);

		getDatabase().execute("""
				DELETE FROM page_row
				WHERE page_row_id =? 
				""", pageRowId);

		getDatabase().execute("""
				WITH ordered AS (
				  SELECT
				    page_row_id,
				    row_number() OVER (PARTITION BY page_section_id ORDER BY display_order) - 1 AS new_order
				  FROM page_row
				  WHERE page_section_id=?
				)
				UPDATE page_row AS pr
				SET display_order = ordered.new_order
				FROM ordered
				WHERE pr.page_row_id = ordered.page_row_id
				AND page_section_id=?;
				""", pageSectionId, pageSectionId);
	}

	@Nonnull
	public Optional<PageRowColumn> findPageRowColumnByPageRowIdAndDisplayOrder(@Nullable UUID pageRowId,
																																						 @Nullable Integer displayOrder) {
		requireNonNull(pageRowId);
		requireNonNull(displayOrder);

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_page_row_column
				WHERE page_row_id = ?
				AND column_display_order = ?
				""", PageRowColumn.class, pageRowId, displayOrder);
	}

	@Nonnull
	public UUID createPageRowOneColumn(@Nonnull CreatePageRowCustomOneColumnRequest request) {
		requireNonNull(request);

		UUID pageSectionId = request.getPageSectionId();
		UUID createdByAccountId = request.getCreatedByAccountId();

		ValidationException validationException = new ValidationException();

		if (pageSectionId == null)
			validationException.add(new ValidationException.FieldError("pageRowId", getStrings().get("Page row is required.")));

		if (validationException.hasErrors())
			throw validationException;

		Integer displayOrder = findNextRowDisplayOrderByPageSectionId(pageSectionId);

		CreatePageRowRequest createPageRowRequest = new CreatePageRowRequest();
		createPageRowRequest.setPageSectionId(pageSectionId);
		createPageRowRequest.setCreatedByAccountId(createdByAccountId);
		createPageRowRequest.setDisplayOrder(displayOrder);
		createPageRowRequest.setRowTypeId(RowTypeId.ONE_COLUMN_IMAGE);

		UUID pageRowId = createPageRow(createPageRowRequest);

		request.getColumnOne().setColumnDisplayOrder(0);
		createPageRowColumn(request.getColumnOne(), pageRowId);

		return pageRowId;
	}

	@Nonnull
	public UUID updatePageRowOneColumn(@Nonnull UpdatePageRowCustomOneColumnRequest request) {
		requireNonNull(request);

		UUID pageRowId = request.getPageRowId();
		CreatePageRowColumnRequest columnOne = request.getColumnOne();
		Optional<PageRow> pageRow = findPageRowById(pageRowId);

		ValidationException validationException = new ValidationException();

		if (!pageRow.isPresent())
			validationException.add(new ValidationException.FieldError("pageRowId", getStrings().get("Page is required.")));
		if (pageRow.isPresent() && !pageRow.get().getRowTypeId().equals(RowTypeId.ONE_COLUMN_IMAGE))
			validationException.add(new ValidationException.FieldError("rowTypeId", getStrings()
					.get(format("Row provided is of type %s, %s is required.", pageRow.get().getRowTypeId(), RowTypeId.ONE_COLUMN_IMAGE))));
		if (columnOne == null)
			validationException.add(new ValidationException.FieldError("columnOne", getStrings().get("Column one is required.")));

		if (validationException.hasErrors())
			throw validationException;

		UpdatePageRowColumnRequest updatePageRowColumnRequest = new UpdatePageRowColumnRequest();
		updatePageRowColumnRequest.setColumnDisplayOrder(0);
		updatePageRowColumnRequest.setDescription(request.getColumnOne().getDescription());
		updatePageRowColumnRequest.setHeadline(request.getColumnOne().getHeadline());
		updatePageRowColumnRequest.setImageFileUploadId(request.getColumnOne().getImageFileUploadId());
		updatePageRowColumnRequest.setImageAltText(request.getColumnOne().getImageAltText());
		updatePageRowColumnRequest.setPageRowId(pageRowId);

		updatePageRowColumn(updatePageRowColumnRequest);

		return pageRowId;
	}

	@Nonnull
	public UUID updatePageRowTwoColumn(@Nonnull UpdatePageRowCustomTwoColumnRequest request) {
		requireNonNull(request);

		UUID pageRowId = request.getPageRowId();
		CreatePageRowColumnRequest columnOne = request.getColumnOne();
		Optional<PageRow> pageRow = findPageRowById(pageRowId);

		ValidationException validationException = new ValidationException();

		if (!pageRow.isPresent())
			validationException.add(new ValidationException.FieldError("pageRowId", getStrings().get("Page is required.")));
		if (pageRow.isPresent() && !pageRow.get().getRowTypeId().equals(RowTypeId.TWO_COLUMN_IMAGE))
			validationException.add(new ValidationException.FieldError("rowTypeId", getStrings()
					.get(format("Row provided is of type %s, %s is required.", pageRow.get().getRowTypeId(), RowTypeId.TWO_COLUMN_IMAGE))));
		if (columnOne == null)
			validationException.add(new ValidationException.FieldError("columnOne", getStrings().get("Column one is required.")));

		if (validationException.hasErrors())
			throw validationException;

		UpdatePageRowColumnRequest updatePageRowColumnRequest1 = new UpdatePageRowColumnRequest();
		updatePageRowColumnRequest1.setColumnDisplayOrder(0);
		updatePageRowColumnRequest1.setDescription(request.getColumnOne().getDescription());
		updatePageRowColumnRequest1.setHeadline(request.getColumnOne().getHeadline());
		updatePageRowColumnRequest1.setImageFileUploadId(request.getColumnOne().getImageFileUploadId());
		updatePageRowColumnRequest1.setImageAltText(request.getColumnOne().getImageAltText());
		updatePageRowColumnRequest1.setPageRowId(pageRowId);

		updatePageRowColumn(updatePageRowColumnRequest1);

		UpdatePageRowColumnRequest updatePageRowColumnRequest2 = new UpdatePageRowColumnRequest();
		updatePageRowColumnRequest2.setColumnDisplayOrder(1);
		updatePageRowColumnRequest2.setDescription(request.getColumnTwo().getDescription());
		updatePageRowColumnRequest2.setHeadline(request.getColumnTwo().getHeadline());
		updatePageRowColumnRequest2.setImageFileUploadId(request.getColumnTwo().getImageFileUploadId());
		updatePageRowColumnRequest2.setImageAltText(request.getColumnTwo().getImageAltText());
		updatePageRowColumnRequest2.setPageRowId(pageRowId);

		updatePageRowColumn(updatePageRowColumnRequest2);

		return pageRowId;
	}

	@Nonnull
	public UUID updatePageRowThreeColumn(@Nonnull UpdatePageRowCustomThreeColumnRequest request) {
		requireNonNull(request);

		UUID pageRowId = request.getPageRowId();
		CreatePageRowColumnRequest columnOne = request.getColumnOne();
		Optional<PageRow> pageRow = findPageRowById(pageRowId);

		ValidationException validationException = new ValidationException();

		if (!pageRow.isPresent())
			validationException.add(new ValidationException.FieldError("pageRowId", getStrings().get("Page is required.")));
		if (pageRow.isPresent() && !pageRow.get().getRowTypeId().equals(RowTypeId.THREE_COLUMN_IMAGE))
			validationException.add(new ValidationException.FieldError("rowTypeId", getStrings()
					.get(format("Row provided is of type %s, %s is required.", pageRow.get().getRowTypeId(), RowTypeId.THREE_COLUMN_IMAGE))));
		if (columnOne == null)
			validationException.add(new ValidationException.FieldError("columnOne", getStrings().get("Column one is required.")));

		if (validationException.hasErrors())
			throw validationException;

		UpdatePageRowColumnRequest updatePageRowColumnRequest1 = new UpdatePageRowColumnRequest();
		updatePageRowColumnRequest1.setColumnDisplayOrder(0);
		updatePageRowColumnRequest1.setDescription(request.getColumnOne().getDescription());
		updatePageRowColumnRequest1.setHeadline(request.getColumnOne().getHeadline());
		updatePageRowColumnRequest1.setImageFileUploadId(request.getColumnOne().getImageFileUploadId());
		updatePageRowColumnRequest1.setImageAltText(request.getColumnOne().getImageAltText());
		updatePageRowColumnRequest1.setPageRowId(pageRowId);

		updatePageRowColumn(updatePageRowColumnRequest1);

		UpdatePageRowColumnRequest updatePageRowColumnRequest2 = new UpdatePageRowColumnRequest();
		updatePageRowColumnRequest2.setColumnDisplayOrder(1);
		updatePageRowColumnRequest2.setDescription(request.getColumnTwo().getDescription());
		updatePageRowColumnRequest2.setHeadline(request.getColumnTwo().getHeadline());
		updatePageRowColumnRequest2.setImageFileUploadId(request.getColumnTwo().getImageFileUploadId());
		updatePageRowColumnRequest2.setImageAltText(request.getColumnTwo().getImageAltText());
		updatePageRowColumnRequest2.setPageRowId(pageRowId);

		updatePageRowColumn(updatePageRowColumnRequest2);

		UpdatePageRowColumnRequest updatePageRowColumnRequest3 = new UpdatePageRowColumnRequest();
		updatePageRowColumnRequest3.setColumnDisplayOrder(2);
		updatePageRowColumnRequest3.setDescription(request.getColumnThree().getDescription());
		updatePageRowColumnRequest3.setHeadline(request.getColumnThree().getHeadline());
		updatePageRowColumnRequest3.setImageFileUploadId(request.getColumnThree().getImageFileUploadId());
		updatePageRowColumnRequest3.setImageAltText(request.getColumnThree().getImageAltText());
		updatePageRowColumnRequest3.setPageRowId(pageRowId);

		updatePageRowColumn(updatePageRowColumnRequest3);

		return pageRowId;
	}

	@Nonnull
	public UUID createPageRowTwoColumn(@Nonnull CreatePageRowCustomTwoColumnRequest request) {
		requireNonNull(request);

		UUID pageSectionId = request.getPageSectionId();
		UUID createdByAccountId = request.getCreatedByAccountId();

		ValidationException validationException = new ValidationException();

		if (pageSectionId == null)
			validationException.add(new ValidationException.FieldError("pageRowId", getStrings().get("Page row is required.")));

		if (validationException.hasErrors())
			throw validationException;

		Integer displayOrder = findNextRowDisplayOrderByPageSectionId(pageSectionId);

		CreatePageRowRequest createPageRowRequest = new CreatePageRowRequest();
		createPageRowRequest.setPageSectionId(pageSectionId);
		createPageRowRequest.setCreatedByAccountId(createdByAccountId);
		createPageRowRequest.setDisplayOrder(displayOrder);
		createPageRowRequest.setRowTypeId(RowTypeId.TWO_COLUMN_IMAGE);

		UUID pageRowId = createPageRow(createPageRowRequest);

		request.getColumnOne().setColumnDisplayOrder(0);
		request.getColumnTwo().setColumnDisplayOrder(1);
		createPageRowColumn(request.getColumnOne(), pageRowId);
		createPageRowColumn(request.getColumnTwo(), pageRowId);

		return pageRowId;
	}

	@Nonnull
	public UUID createPageRowThreeColumn(@Nonnull CreatePageRowCustomThreeColumnRequest request) {
		requireNonNull(request);

		UUID pageSectionId = request.getPageSectionId();
		UUID createdByAccountId = request.getCreatedByAccountId();

		ValidationException validationException = new ValidationException();

		if (pageSectionId == null)
			validationException.add(new ValidationException.FieldError("pageRowId", getStrings().get("Page row is required.")));

		if (validationException.hasErrors())
			throw validationException;

		Integer displayOrder = findNextRowDisplayOrderByPageSectionId(pageSectionId);

		CreatePageRowRequest createPageRowRequest = new CreatePageRowRequest();
		createPageRowRequest.setPageSectionId(pageSectionId);
		createPageRowRequest.setCreatedByAccountId(createdByAccountId);
		createPageRowRequest.setDisplayOrder(displayOrder);
		createPageRowRequest.setRowTypeId(RowTypeId.THREE_COLUMN_IMAGE);

		UUID pageRowId = createPageRow(createPageRowRequest);

		request.getColumnOne().setColumnDisplayOrder(0);
		request.getColumnTwo().setColumnDisplayOrder(1);
		request.getColumnThree().setColumnDisplayOrder(2);
		createPageRowColumn(request.getColumnOne(), pageRowId);
		createPageRowColumn(request.getColumnTwo(), pageRowId);
		createPageRowColumn(request.getColumnThree(), pageRowId);

		return pageRowId;
	}

	@Nonnull
	public UUID createPageRowColumn(@Nonnull CreatePageRowColumnRequest request,
																	@Nonnull UUID pageRowId) {
		requireNonNull(request);
		requireNonNull(pageRowId);

		UUID pageRowColumnId = UUID.randomUUID();
		String headline = trimToNull(request.getHeadline());
		String description = trimToNull(request.getDescription());
		String imageFileUploadIdString = request.getImageFileUploadId();
		String imageAltText = trimToNull(request.getImageAltText());
		Integer columnDisplayOrder = request.getColumnDisplayOrder();
		UUID imageFileUploadId = null;

		if (isValidUUID(imageFileUploadIdString))
			imageFileUploadId = UUID.fromString(imageFileUploadIdString);

		ValidationException validationException = new ValidationException();

		if (pageRowId == null)
			validationException.add(new ValidationException.FieldError("pageRowId", getStrings().get("Page row is required.")));
		if (columnDisplayOrder == null)
			validationException.add(new ValidationException.FieldError("columnDisplayOrder", getStrings().get("Column display order is required.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				INSERT INTO page_row_column
				  (page_row_column_id, page_row_id, headline, description, image_file_upload_id, image_alt_text, column_display_order)
				VALUES
				  (?,?,?,?,?,?,?)   
				""", pageRowColumnId, pageRowId, headline, description, imageFileUploadId, imageAltText, columnDisplayOrder);

		return pageRowColumnId;
	}

	public void updatePageRowColumn(@Nonnull UpdatePageRowColumnRequest request) {
		requireNonNull(request);

		UUID pageRowId = request.getPageRowId();
		String headline = trimToNull(request.getHeadline());
		String description = trimToNull(request.getDescription());
		String imageAltText = trimToNull(request.getImageAltText());
		Integer columnDisplayOrder = request.getColumnDisplayOrder();
		String imageFileUploadIdString = request.getImageFileUploadId();
		UUID imageFileUploadId = null;

		if (isValidUUID(imageFileUploadIdString))
			imageFileUploadId = UUID.fromString(imageFileUploadIdString);

		ValidationException validationException = new ValidationException();

		if (pageRowId == null)
			validationException.add(new ValidationException.FieldError("pageRowId", getStrings().get("Page row is required.")));
		if (columnDisplayOrder == null)
			validationException.add(new ValidationException.FieldError("columnDisplayOrder", getStrings().get("Display order is required.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				UPDATE page_row_column SET
				headline=?, description=?, image_file_upload_id=?, image_alt_text=?
				WHERE page_row_id=? 
				AND column_display_order=?
				""", headline, description, imageFileUploadId, imageAltText, pageRowId, columnDisplayOrder);

	}

	@Nonnull
	private Integer findNextRowDisplayOrderByPageSectionId(@Nonnull UUID pageSectionId) {
		return getDatabase().queryForObject("""
				SELECT COALESCE(MAX(display_order) + 1, 0)
				FROM v_page_row
				WHERE page_section_id = ?
				""", Integer.class, pageSectionId).get();
	}

	@Nonnull
	public List<PageRowContent> findPageRowContentByPageRowId(@Nullable UUID pageRowId) {
		requireNonNull(pageRowId);

		return getDatabase().queryForList("""
				SELECT *
				FROM v_page_row_content
				WHERE page_row_id = ?
				""", PageRowContent.class, pageRowId);
	}

	@Nonnull
	public List<Content> findContentByPageRowId(@Nullable UUID pageRowId) {
		requireNonNull(pageRowId);

		return getDatabase().queryForList("""
				SELECT va.*
				FROM v_admin_content va, v_page_row_content vp
				WHERE va.content_id = vp.content_id
				AND vp.page_row_id = ?
				ORDER BY vp.content_display_order
				""", Content.class, pageRowId);
	}

	@Nonnull
	public void updatePageRowContent(@Nonnull UpdatePageRowContentRequest request) {
		requireNonNull(request);

		UUID pageRowId = request.getPageRowId();
		List<UUID> contentIds = request.getContentIds();

		Optional<PageRow> pageRow = findPageRowById(pageRowId);

		ValidationException validationException = new ValidationException();

		if (!pageRow.isPresent())
			validationException.add(new ValidationException.FieldError("pageRowId", getStrings().get("Page is required.")));
		if (!pageRow.get().getRowTypeId().equals(RowTypeId.RESOURCES))
			validationException.add(new ValidationException.FieldError("rowTypeId", getStrings()
					.get(format("Row provided is of type %s, %s is required.", pageRow.get().getRowTypeId(), RowTypeId.RESOURCES))));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				DELETE FROM page_row_content
				WHERE page_row_id = ?""", pageRowId);

		int contentDisplayOrder = 0;

		for (UUID contentId : contentIds) {
			getDatabase().execute("""
					INSERT INTO page_row_content
					  (page_row_id, content_id, content_display_order)
					VALUES
					  (?, ?, ?)   
					""", pageRowId, contentId, contentDisplayOrder);
			contentDisplayOrder++;
		}
	}

	@Nonnull
	public void updatePageRowGroupSession(@Nonnull UpdatePageRowGroupSessionRequest request) {
		requireNonNull(request);

		UUID pageRowId = request.getPageRowId();
		List<UUID> groupSessionIds = request.getGroupSessionIds();

		Optional<PageRow> pageRow = findPageRowById(pageRowId);

		ValidationException validationException = new ValidationException();

		if (!pageRow.isPresent())
			validationException.add(new ValidationException.FieldError("pageRowId", getStrings().get("Page is required.")));
		if (!pageRow.get().getRowTypeId().equals(RowTypeId.GROUP_SESSIONS))
			validationException.add(new ValidationException.FieldError("rowTypeId", getStrings()
					.get(format("Row provided is of type %s, %s is required.", pageRow.get().getRowTypeId(), RowTypeId.GROUP_SESSIONS))));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				DELETE FROM page_row_group_session
				WHERE page_row_id = ?""", pageRowId);

		int groupSessionDisplayOrder = 0;

		for (UUID groupSessionId : groupSessionIds) {
			getDatabase().execute("""
					INSERT INTO page_row_group_session
					   (page_row_id, group_session_id, group_session_display_order)
					 VALUES
					   (?,?,?)   
					""", pageRowId, groupSessionId, groupSessionDisplayOrder);
			groupSessionDisplayOrder++;
		}

	}

	@Nonnull
	public UUID createPageRowContent(@Nonnull CreatePageRowContentRequest request) {
		requireNonNull(request);

		UUID pageSectionId = request.getPageSectionId();
		List<UUID> contentIds = request.getContentIds();
		UUID createdByAccountId = request.getCreatedByAccountId();

		ValidationException validationException = new ValidationException();

		if (pageSectionId == null)
			validationException.add(new ValidationException.FieldError("pageSectionId", getStrings().get("Page Section is required.")));

		if (validationException.hasErrors())
			throw validationException;

		Integer displayOrder = findNextRowDisplayOrderByPageSectionId(pageSectionId);

		CreatePageRowRequest createPageRowRequest = new CreatePageRowRequest();
		createPageRowRequest.setPageSectionId(pageSectionId);
		createPageRowRequest.setCreatedByAccountId(createdByAccountId);
		createPageRowRequest.setDisplayOrder(displayOrder);
		createPageRowRequest.setRowTypeId(RowTypeId.RESOURCES);

		UUID pageRowId = createPageRow(createPageRowRequest);
		int contentDisplayOrder = 0;

		for (UUID contentId : contentIds) {
			getDatabase().execute("""
					INSERT INTO page_row_content
					  (page_row_id, content_id, content_display_order)
					VALUES
					  (?, ?, ?)   
					""", pageRowId, contentId, contentDisplayOrder);
			contentDisplayOrder++;
		}

		return pageRowId;
	}

	@Nonnull
	public void deletePageRowContent(@Nullable UUID pageRowId,
																	 @Nullable UUID contentId) {
		requireNonNull(pageRowId);
		requireNonNull(contentId);

		getDatabase().execute("""
				DELETE FROM page_row_content 				
				WHERE page_row_id = ?
				AND content_id = ?
				""", pageRowId, contentId);

		getDatabase().execute("""
				WITH ordered AS (
				  SELECT
				    page_row_content_id,
				    row_number() OVER (PARTITION BY page_row_id ORDER BY content_display_order) - 1 AS new_order
				  FROM page_row_content
				  WHERE page_row_id=?
				)
				UPDATE page_row_content AS prc
				SET content_display_order = ordered.new_order
				FROM ordered
				WHERE prc.page_row_content_id = ordered.page_row_content_id
				AND page_row_id=?;			
				""", pageRowId, pageRowId);
	}

	@Nonnull
	public Optional<PageRowTagGroup> findPageRowTagGroupByRowId(@Nullable UUID pageRowId) {
		requireNonNull(pageRowId);

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_page_row_tag_group
				WHERE page_row_id = ?
				""", PageRowTagGroup.class, pageRowId);
	}

	@Nonnull
	public Optional<TagGroup> findTagGroupByRowId(@Nullable UUID pageRowId) {
		requireNonNull(pageRowId);

		return getDatabase().queryForObject("""
				SELECT tg.*
				FROM v_page_row_tag_group vp, tag_group tg
				WHERE vp.tag_group_id = tg.tag_group_id 
				AND page_row_id = ?
				""", TagGroup.class, pageRowId);
	}

	@Nonnull
	public UUID createPageTagGroup(@Nonnull CreatePageRowTagGroupRequest request) {
		requireNonNull(request);

		UUID pageRowTagGroupId = UUID.randomUUID();
		UUID pageSectionId = request.getPageSectionId();
		String tagGroupId = trimToNull(request.getTagGroupId());
		UUID createdByAccountId = request.getCreatedByAccountId();

		ValidationException validationException = new ValidationException();

		if (pageSectionId == null)
			validationException.add(new ValidationException.FieldError("pageSectionId", getStrings().get("Page Section is required.")));
		if (tagGroupId == null)
			validationException.add(new ValidationException.FieldError("tagGroupId", getStrings().get("Tag group is required.")));
		if (createdByAccountId == null)
			validationException.add(new ValidationException.FieldError("createdByAccountId", getStrings().get("Created by account is required.")));

		if (validationException.hasErrors())
			throw validationException;

		Integer displayOrder = getDatabase().queryForObject("""
				SELECT COALESCE(MAX(display_order) + 1, 0)
				FROM v_page_row
				WHERE page_section_id = ?
				""", Integer.class, pageSectionId).get();

		CreatePageRowRequest createPageRowRequest = new CreatePageRowRequest();
		createPageRowRequest.setPageSectionId(pageSectionId);
		createPageRowRequest.setCreatedByAccountId(createdByAccountId);
		createPageRowRequest.setDisplayOrder(displayOrder);
		createPageRowRequest.setRowTypeId(RowTypeId.TAG_GROUP);

		UUID pageRowId = createPageRow(createPageRowRequest);

		getDatabase().execute("""
				INSERT INTO page_row_tag_group
				  (page_row_tag_group_id, page_row_id, tag_group_id)
				VALUES
				  (?,?,?)   
				""", pageRowTagGroupId, pageRowId, tagGroupId);

		return pageRowId;
	}

	@Nonnull
	public void updatePageTagGroup(@Nonnull UpdatePageRowTagGroupRequest request) {
		requireNonNull(request);

		UUID pageRowId = request.getPageRowId();
		String tagGroupId = trimToNull(request.getTagGroupId());

		ValidationException validationException = new ValidationException();

		Optional<PageRow> pageRow = findPageRowById(pageRowId);

		if (!pageRow.isPresent())
			validationException.add(new ValidationException.FieldError("pageRow", getStrings().get("Page not found.")));
		if (tagGroupId == null)

			if (validationException.hasErrors())
				throw validationException;

		getDatabase().execute("""
				UPDATE page_row_tag_group
				SET tag_group_id=?
				WHERE page_row_id=?
				""", tagGroupId, pageRowId);
	}

	@Nonnull
	public List<PageRowGroupSession> findPageRowGroupSessionByPageRowId(@Nullable UUID pageRowId) {
		requireNonNull(pageRowId);

		return getDatabase().queryForList("""
				SELECT *
				FROM v_page_row_group_session
				WHERE page_row_id = ?
				""", PageRowGroupSession.class, pageRowId);
	}

	@Nonnull
	public List<GroupSession> findGroupSessionsByPageRowId(@Nullable UUID pageRowId) {
		requireNonNull(pageRowId);

		return getDatabase().queryForList("""
				SELECT vgs.*
				FROM v_page_row_group_session vp, v_group_session vgs
				WHERE vp.group_session_id = vgs.group_session_id
				AND page_row_id = ?
				ORDER BY vp.group_session_display_order
				""", GroupSession.class, pageRowId);
	}

	@Nonnull
	public UUID createPageRowGroupSession(@Nonnull CreatePageRowGroupSessionRequest request) {
		requireNonNull(request);

		UUID pageSectionId = request.getPageSectionId();
		UUID createdByAccountId = request.getCreatedByAccountId();
		List<UUID> groupSessionIds = request.getGroupSessionIds();

		ValidationException validationException = new ValidationException();

		if (pageSectionId == null)
			validationException.add(new ValidationException.FieldError("pageSectionId", getStrings().get("Could not find Page Section")));

		if (validationException.hasErrors())
			throw validationException;

		Integer displayOrder = findNextRowDisplayOrderByPageSectionId(pageSectionId);

		CreatePageRowRequest createPageRowRequest = new CreatePageRowRequest();
		createPageRowRequest.setPageSectionId(pageSectionId);
		createPageRowRequest.setCreatedByAccountId(createdByAccountId);
		createPageRowRequest.setDisplayOrder(displayOrder);
		createPageRowRequest.setRowTypeId(RowTypeId.GROUP_SESSIONS);

		UUID pageRowId = createPageRow(createPageRowRequest);
		int groupSessionDisplayOrder = 0;

		for (UUID groupSessionId : groupSessionIds) {
			getDatabase().execute("""
					INSERT INTO page_row_group_session
					   (page_row_id, group_session_id, group_session_display_order)
					 VALUES
					   (?,?,?)   
					""", pageRowId, groupSessionId, groupSessionDisplayOrder);
			groupSessionDisplayOrder++;
		}

		return pageRowId;
	}

	@Nonnull
	public void deletePageRowGroupSession(@Nullable UUID pageRowId,
																				@Nullable UUID groupSessionId) {
		requireNonNull(pageRowId);
		requireNonNull(groupSessionId);

		getLogger().debug(format("DELETE %s and %s", pageRowId, groupSessionId));
		getDatabase().execute("""
				DELETE FROM page_row_group_session				
				WHERE page_row_id = ?
				AND group_session_id = ?
				""", pageRowId, groupSessionId);

		getDatabase().execute("""
				WITH ordered AS (
				  SELECT
				    page_row_group_session_id,
				    row_number() OVER (PARTITION BY page_row_id ORDER BY group_session_display_order) - 1 AS new_order
				  FROM page_row_group_session
				  WHERE page_row_id=?
				)
				UPDATE page_row_group_session AS prgs
				SET group_session_display_order = ordered.new_order
				FROM ordered
				WHERE prgs.page_row_group_session_id = ordered.page_row_group_session_id
				AND page_row_id=?;
				""", pageRowId, pageRowId);
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
	public List<Page> findAllPagesBySiteLocation(@Nonnull SiteLocationId siteLocationId,
																							 @Nonnull InstitutionId institutionId) {
		requireNonNull(siteLocationId);
		requireNonNull(institutionId);

		return getDatabase().queryForList("""
				SELECT p.*
				FROM v_page p, page_site_location psl
				WHERE p.page_id = psl.page_id
				AND p.institution_id = ?
				AND NOW() BETWEEN psl.publish_start_date and psl.publish_end_date""", Page.class, institutionId);

	}

	@Nonnull
	public FindResult<Page> findAllPagesByInstitutionId(@Nonnull FindPagesRequest request) {
		requireNonNull(request);

		InstitutionId institutionId = request.getInstitutionId();
		Integer pageNumber = request.getPageNumber();
		Integer pageSize = request.getPageSize();
		FindPagesRequest.OrderBy orderBy = request.getOrderBy() == null ? FindPagesRequest.OrderBy.CREATED_DESC : request.getOrderBy();
		final int DEFAULT_PAGE_SIZE = 25;
		final int MAXIMUM_PAGE_SIZE = 100;

		if (pageNumber == null || pageNumber < 0)
			pageNumber = 0;

		if (pageSize == null || pageSize <= 0)
			pageSize = DEFAULT_PAGE_SIZE;
		else if (pageSize > MAXIMUM_PAGE_SIZE)
			pageSize = MAXIMUM_PAGE_SIZE;

		Integer limit = pageSize;
		Integer offset = pageNumber * pageSize;
		List<Object> parameters = new ArrayList<>();

		StringBuilder query = new StringBuilder("SELECT vp.*, COUNT(vp.page_id) OVER() AS total_count FROM v_page vp ");

		query.append("WHERE vp.institution_id = ? ");
		parameters.add(institutionId);

		query.append("ORDER BY ");

		if (orderBy == FindPagesRequest.OrderBy.CREATED_DESC)
			query.append("vp.created DESC ");
		else if (orderBy == FindPagesRequest.OrderBy.CREATED_ASC)
			query.append("vp.created  ASC ");

		query.append("LIMIT ? OFFSET ? ");

		parameters.add(limit);
		parameters.add(offset);

		List<PageWithTotalCount> pages = getDatabase().queryForList(query.toString(), PageWithTotalCount.class, parameters.toArray());

		FindResult<? extends Page> findResult = new FindResult<>(pages, pages.size() == 0 ? 0 : pages.get(0).getTotalCount());

		return (FindResult<Page>) findResult;

	}

	@Nonnull
	public UUID duplicatePage(@Nonnull DuplicatePageRequest request) {
		requireNonNull(request);

		UUID newPageId = UUID.randomUUID();
		UUID newPageSectionId;
		UUID newPageRowId;
		UUID accountId = request.getCreatedByAccountId();
		UUID pageId = request.getPageId();
		String name =trimToNull(request.getName());
		String urlName = trimToNull(request.getUrlName());
		Boolean copyForEditing = request.getCopyForEditing();
		InstitutionId institutionId = request.getInstitutionId();
		ValidationException validationException = new ValidationException();

		Optional<Page> page = findPageById(pageId, institutionId, true);

		if (!page.isPresent())
			validationException.add(new ValidationException.FieldError("pageId", getStrings().get("Could not find page.")));

		if (!copyForEditing){
			if (name == null)
				validationException.add(new ValidationException.FieldError("name", getStrings().get("Name is required.")));
			else if (nameExistsForInstitutionId(name, institutionId, null))
				validationException.add(new ValidationException.FieldError("name", getStrings().get("Name already exists.")));

			if (urlName == null)
				validationException.add(new ValidationException.FieldError("urlName", getStrings().get("Friendly URL name is required.")));
			else if (!isValidUrlSubdirectory(urlName))
				validationException.add(new ValidationException.FieldError("urlName", getStrings().get("Not a valid Friendly URL")));
			else if (urlNameExistsForInstitutionId(urlName, institutionId, pageId))
				validationException.add(new ValidationException.FieldError("urlName", getStrings().get("Friendly URL name is already in use.")));

		}

		if (validationException.hasErrors())
			throw validationException;

		if (copyForEditing) {
			name = page.get().getName();
			urlName = page.get().getUrlName();
		}

		getDatabase().execute("""
				INSERT INTO page
				(page_id,name,url_name,page_status_id,headline,description,image_file_upload_id,
				 image_alt_text,published_date,deleted_flag,institution_id,created_by_account_id, parent_page_id)
				SELECT ?,?,?,page_status_id,headline,description,image_file_upload_id,
				       image_alt_text,published_date,deleted_flag,institution_id,?,?
				FROM page 
				WHERE page_id=?
				""", newPageId, name, urlName, accountId, pageId, pageId);

		List<PageSection> pageSections = findPageSectionsByPageId(pageId);
		List<PageRow> pageRows;

		for (PageSection pageSection : pageSections) {
			newPageSectionId = UUID.randomUUID();

			getDatabase().execute("""
					INSERT INTO page_section
					(page_section_id,page_id,name,headline,description,
					 background_color_id,deleted_flag,display_order,created_by_account_id)
					 SELECT ?,?,name,headline,description,
					        background_color_id,deleted_flag,display_order,?
					 FROM page_section
					 WHERE page_section_id=?					  					 
					""", newPageSectionId, newPageId, accountId, pageSection.getPageSectionId());

			pageRows = findPageRowsBySectionId(pageSection.getPageSectionId());

			for (PageRow pageRow : pageRows) {
				newPageRowId = UUID.randomUUID();

				getDatabase().execute("""
						INSERT INTO page_row
						(page_row_id,page_section_id,row_type_id,deleted_flag,display_order,created_by_account_id)
						SELECT ?, ?, row_type_id,deleted_flag,display_order, ?
						FROM page_row
						WHERE page_row_id = ?
						""", newPageRowId, newPageSectionId, accountId, pageRow.getPageRowId());

				getDatabase().execute("""
						INSERT INTO page_row_column
						(page_row_id,headline,description,image_file_upload_id,image_alt_text,column_display_order)						 
						SELECT ?,headline,description,image_file_upload_id,image_alt_text,column_display_order
						FROM page_row_column
						WHERE page_row_id = ?""", newPageRowId, pageRow.getPageRowId());

				getDatabase().execute("""
						INSERT INTO page_row_group_session
						(page_row_id,group_session_id,group_session_display_order)
						SELECT ?,group_session_id,group_session_display_order
						FROM page_row_group_session
						WHERE page_row_id=?
						""", newPageRowId, pageRow.getPageRowId());

				getDatabase().execute("""
						INSERT INTO page_row_content
						(page_row_id, content_id, content_display_order)
						SELECT ?, content_id, content_display_order
						FROM page_row_content
						WHERE page_row_id=?
						""", newPageRowId, pageRow.getPageRowId());

				getDatabase().execute("""
						INSERT INTO page_row_tag_group
						(page_row_id, tag_group_id)
						SELECT ?, tag_group_id
						FROM page_row_tag_group
						WHERE page_row_id=?
						""", newPageRowId, pageRow.getPageRowId());
			}
		}

		return newPageId;
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
