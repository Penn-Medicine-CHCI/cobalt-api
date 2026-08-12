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

import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.model.api.request.CreateGroupSessionRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRequest;
import com.cobaltplatform.api.model.api.request.DuplicatePageRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.BackgroundColor.BackgroundColorId;
import com.cobaltplatform.api.model.db.GroupSessionLocationType.GroupSessionLocationTypeId;
import com.cobaltplatform.api.model.db.GroupSessionSchedulingSystem.GroupSessionSchedulingSystemId;
import com.cobaltplatform.api.model.db.GroupSessionVisibilityType.GroupSessionVisibilityTypeId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PageRow;
import com.cobaltplatform.api.model.db.PageRowCallToAction;
import com.cobaltplatform.api.model.db.PageRowColumn;
import com.cobaltplatform.api.model.db.PageRowColumnContentOrder.PageRowColumnContentOrderId;
import com.cobaltplatform.api.model.db.PageRowPadding.PageRowPaddingId;
import com.cobaltplatform.api.model.db.PageSection;
import com.cobaltplatform.api.model.db.PageStatus.PageStatusId;
import com.cobaltplatform.api.model.db.RowType.RowTypeId;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.pyranid.Database;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static java.lang.String.format;

@ThreadSafe
public class PageServiceRowDuplicationTests {
	@Test
	public void duplicatePageRowCopiesSupportedDataAndWholePageDuplicationStillUsesTheSameBehavior() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			UUID accountId = database.queryForObject("""
					SELECT account_id
					FROM v_account
					WHERE institution_id = ?
					ORDER BY created
					LIMIT 1
					""", UUID.class, InstitutionId.COBALT).orElseThrow();
			Account account = accountService.findAccountById(accountId).orElseThrow();
			Institution institution = institutionService.findInstitutionById(account.getInstitutionId()).orElseThrow();

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, institution.getTimeZone()).build(), () -> {
				PageService pageService = app.getInjector().getInstance(PageService.class);
				GroupSessionService groupSessionService = app.getInjector().getInstance(GroupSessionService.class);
				UUID pageId = createPage(pageService, account);
				UUID pageSectionId = pageService.findPageSectionsByPageId(pageId, account.getInstitutionId()).get(0)
						.getPageSectionId();
				UUID fileUploadId = createFileUpload(database, accountId);
				UUID contentId = createContent(database, account.getInstitutionId());
				UUID groupSessionId = createGroupSession(groupSessionService, account);
				String tagGroupId = format("PAGE_ROW_DUPLICATION_%s", UUID.randomUUID());
				String tagId = format("PAGE_ROW_DUPLICATION_%s", UUID.randomUUID());
				UUID mailingListId = UUID.randomUUID();

				database.execute("""
						INSERT INTO tag_group
						(tag_group_id, color_id, name, url_name, description)
						VALUES (?, 'BRAND_PRIMARY', ?, ?, ?)
						""", tagGroupId, tagGroupId, tagGroupId.toLowerCase(Locale.US), "Row duplication tag group");
				database.execute("""
						INSERT INTO tag
						(tag_id, name, url_name, description, tag_group_id)
						VALUES (?, ?, ?, ?, ?)
						""", tagId, tagId, tagId.toLowerCase(Locale.US), "Row duplication tag", tagGroupId);
				database.execute("""
						INSERT INTO mailing_list
						(mailing_list_id, institution_id, created_by_account_id)
						VALUES (?, ?, ?)
						""", mailingListId, account.getInstitutionId(), accountId);

				UUID customRowId = createRow(database, pageSectionId, accountId, RowTypeId.CUSTOM_ROW,
						"Feature row", 0, BackgroundColorId.NEUTRAL, PageRowPaddingId.SMALL, PageRowPaddingId.LARGE);
				UUID resourceRowId = createRow(database, pageSectionId, accountId, RowTypeId.RESOURCES,
						"Resource row", 1, BackgroundColorId.WHITE, PageRowPaddingId.MEDIUM, PageRowPaddingId.MEDIUM);
				UUID groupSessionRowId = createRow(database, pageSectionId, accountId, RowTypeId.GROUP_SESSIONS,
						"Group session row", 2, BackgroundColorId.WHITE, PageRowPaddingId.MEDIUM, PageRowPaddingId.MEDIUM);
				UUID tagGroupRowId = createRow(database, pageSectionId, accountId, RowTypeId.TAG_GROUP,
						"Tag group row", 3, BackgroundColorId.WHITE, PageRowPaddingId.MEDIUM, PageRowPaddingId.MEDIUM);
				UUID tagRowId = createRow(database, pageSectionId, accountId, RowTypeId.TAG,
						"Tag row", 4, BackgroundColorId.WHITE, PageRowPaddingId.MEDIUM, PageRowPaddingId.MEDIUM);
				UUID callToActionRowId = createRow(database, pageSectionId, accountId, RowTypeId.CALL_TO_ACTION_BLOCK,
						"Call to action row", 5, BackgroundColorId.NEUTRAL, PageRowPaddingId.SMALL, PageRowPaddingId.LARGE);
				UUID mailingListRowId = createRow(database, pageSectionId, accountId, RowTypeId.MAILING_LIST,
						"Subscribe row", 6, BackgroundColorId.WHITE, PageRowPaddingId.MEDIUM, PageRowPaddingId.MEDIUM);

				UUID firstColumnId = UUID.randomUUID();
				UUID secondColumnId = UUID.randomUUID();
				database.execute("""
						INSERT INTO page_row_column
						(page_row_column_id, page_row_id, headline, description, image_file_upload_id, image_alt_text,
						 use_placeholder_image, column_display_order, content_order_id)
						VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?), (?, ?, ?, ?, NULL, ?, ?, ?, ?)
						""", firstColumnId, customRowId, "First headline", "<p>First description</p>", fileUploadId,
						"First image", false, 0, PageRowColumnContentOrderId.TEXT_THEN_IMAGE,
						secondColumnId, customRowId, "Second headline", "<p>Second description</p>", "Second image",
						true, 1, PageRowColumnContentOrderId.IMAGE_THEN_TEXT);
				database.execute("""
						INSERT INTO page_row_content
						(page_row_id, content_id, content_display_order)
						VALUES (?, ?, 0)
						""", resourceRowId, contentId);
				database.execute("""
						INSERT INTO page_row_group_session
						(page_row_id, group_session_id, group_session_display_order)
						VALUES (?, ?, 0)
						""", groupSessionRowId, groupSessionId);
				database.execute("INSERT INTO page_row_tag_group (page_row_id, tag_group_id) VALUES (?, ?)",
						tagGroupRowId, tagGroupId);
				database.execute("INSERT INTO page_row_tag (page_row_id, tag_id) VALUES (?, ?)", tagRowId, tagId);
				database.execute("""
						INSERT INTO page_row_call_to_action
						(page_row_id, headline, description, button_text, button_url, image_file_upload_id)
						VALUES (?, ?, ?, ?, ?, ?)
						""", callToActionRowId, "CTA headline", "CTA description", "Learn more", "/learn-more", fileUploadId);
				database.execute("""
						INSERT INTO page_row_mailing_list
						(page_row_id, mailing_list_id, title, description)
						VALUES (?, ?, ?, ?)
						""", mailingListRowId, mailingListId, "Subscribe", "<p>Subscribe description</p>");

				UUID customCopyId = duplicateAndAssertPosition(pageService, customRowId, account, "Feature row Copy");
				assertCopiedRowMetadata(pageService, customRowId, customCopyId);
				assertColumnsCopied(pageService, customRowId, customCopyId, fileUploadId);

				UUID resourceCopyId = duplicateAndAssertPosition(pageService, resourceRowId, account, "Resource row Copy");
				assertAssociationCopied(database, "page_row_content", "page_row_content_id", "content_id",
						resourceRowId, resourceCopyId, contentId);

				UUID groupSessionCopyId = duplicateAndAssertPosition(pageService, groupSessionRowId, account,
						"Group session row Copy");
				assertAssociationCopied(database, "page_row_group_session", "page_row_group_session_id", "group_session_id",
						groupSessionRowId, groupSessionCopyId, groupSessionId);

				UUID tagGroupCopyId = duplicateAndAssertPosition(pageService, tagGroupRowId, account, "Tag group row Copy");
				assertStringAssociationCopied(database, "page_row_tag_group", "page_row_tag_group_id", "tag_group_id",
						tagGroupRowId, tagGroupCopyId, tagGroupId);

				UUID tagCopyId = duplicateAndAssertPosition(pageService, tagRowId, account, "Tag row Copy");
				assertStringAssociationCopied(database, "page_row_tag", "page_row_tag_id", "tag_id",
						tagRowId, tagCopyId, tagId);

				UUID callToActionCopyId = duplicateAndAssertPosition(pageService, callToActionRowId, account,
						"Call to action row Copy");
				assertCallToActionCopied(pageService, callToActionRowId, callToActionCopyId, fileUploadId);

				int rowCountBeforeRejectedDuplicate = pageService.findPageRowsBySectionId(pageSectionId,
						account.getInstitutionId()).size();
				Assert.assertThrows(ValidationException.class,
						() -> pageService.duplicatePageRow(mailingListRowId, accountId, account.getInstitutionId()));
				Assert.assertEquals(rowCountBeforeRejectedDuplicate,
						pageService.findPageRowsBySectionId(pageSectionId, account.getInstitutionId()).size());

				DuplicatePageRequest duplicatePageRequest = new DuplicatePageRequest();
				duplicatePageRequest.setPageId(pageId);
				duplicatePageRequest.setInstitutionId(account.getInstitutionId());
				duplicatePageRequest.setCreatedByAccountId(accountId);
				duplicatePageRequest.setName(format("Row duplication copy %s", UUID.randomUUID()));
				duplicatePageRequest.setUrlName(format("row-duplication-copy-%s", UUID.randomUUID()));
				duplicatePageRequest.setCopyForEditing(false);
				duplicatePageRequest.setPageStatusId(PageStatusId.DRAFT);
				UUID duplicatedPageId = pageService.duplicatePage(duplicatePageRequest, account.getInstitutionId());
				PageSection duplicatedPageSection = pageService.findPageSectionsByPageId(duplicatedPageId,
						account.getInstitutionId()).get(0);
				List<PageRow> duplicatedPageRows = pageService.findPageRowsBySectionId(duplicatedPageSection.getPageSectionId(),
						account.getInstitutionId());
				Assert.assertEquals(rowCountBeforeRejectedDuplicate, duplicatedPageRows.size());

				PageRow duplicatedCustomRow = findRowByName(duplicatedPageRows, "Feature row");
				Assert.assertNotEquals(customRowId, duplicatedCustomRow.getPageRowId());
				Assert.assertNotEquals(pageService.findPageRowById(customRowId, account.getInstitutionId()).orElseThrow()
						.getPageRowAnchorId(), duplicatedCustomRow.getPageRowAnchorId());
				assertColumnsCopied(pageService, customRowId, duplicatedCustomRow.getPageRowId(), fileUploadId);

				PageRow duplicatedResourceRow = findRowByName(duplicatedPageRows, "Resource row");
				assertAssociationCopied(database, "page_row_content", "page_row_content_id", "content_id",
						resourceRowId, duplicatedResourceRow.getPageRowId(), contentId);
				PageRow duplicatedGroupSessionRow = findRowByName(duplicatedPageRows, "Group session row");
				assertAssociationCopied(database, "page_row_group_session", "page_row_group_session_id", "group_session_id",
						groupSessionRowId, duplicatedGroupSessionRow.getPageRowId(), groupSessionId);
				PageRow duplicatedTagGroupRow = findRowByName(duplicatedPageRows, "Tag group row");
				assertStringAssociationCopied(database, "page_row_tag_group", "page_row_tag_group_id", "tag_group_id",
						tagGroupRowId, duplicatedTagGroupRow.getPageRowId(), tagGroupId);
				PageRow duplicatedTagRow = findRowByName(duplicatedPageRows, "Tag row");
				assertStringAssociationCopied(database, "page_row_tag", "page_row_tag_id", "tag_id",
						tagRowId, duplicatedTagRow.getPageRowId(), tagId);
				PageRow duplicatedCallToActionRow = findRowByName(duplicatedPageRows, "Call to action row");
				assertCallToActionCopied(pageService, callToActionRowId, duplicatedCallToActionRow.getPageRowId(), fileUploadId);

				PageRow duplicatedMailingListRow = findRowByName(duplicatedPageRows, "Subscribe row");
				assertAssociationCopied(database, "page_row_mailing_list", "page_row_mailing_list_id", "mailing_list_id",
						mailingListRowId, duplicatedMailingListRow.getPageRowId(), mailingListId);
			});
		});
	}

	@Nonnull
	private UUID createPage(@Nonnull PageService pageService, @Nonnull Account account) {
		CreatePageRequest request = new CreatePageRequest();
		request.setName(format("Page row duplication test %s", UUID.randomUUID()));
		request.setUrlName(format("page-row-duplication-test-%s", UUID.randomUUID()));
		request.setInstitutionId(account.getInstitutionId());
		request.setCreatedByAccountId(account.getAccountId());
		return pageService.createPage(request);
	}

	@Nonnull
	private UUID createFileUpload(@Nonnull Database database, @Nonnull UUID accountId) {
		UUID fileUploadId = UUID.randomUUID();
		String storageKey = format("page-row-duplication/%s.png", fileUploadId);
		database.execute("""
				INSERT INTO file_upload
				(file_upload_id, account_id, url, storage_key, filename, content_type)
				VALUES (?, ?, ?, ?, ?, ?)
				""", fileUploadId, accountId, format("https://example.com/%s", storageKey), storageKey,
				format("%s.png", fileUploadId), "image/png");
		return fileUploadId;
	}

	@Nonnull
	private UUID createContent(@Nonnull Database database, @Nonnull InstitutionId institutionId) {
		UUID contentId = UUID.randomUUID();
		String contentTypeId = database.queryForObject("""
				SELECT content_type_id
				FROM content_type
				WHERE deleted = FALSE
				ORDER BY content_type_id
				LIMIT 1
				""", String.class).orElseThrow();
		database.execute("""
				INSERT INTO content
				(content_id, content_type_id, title, description, date_created, publish_start_date, owner_institution_id)
				VALUES (?, ?, ?, ?, NOW(), NOW(), ?)
				""", contentId, contentTypeId, "Page row duplication content", "Page row duplication content description",
				institutionId);
		return contentId;
	}

	@Nonnull
	private UUID createGroupSession(@Nonnull GroupSessionService groupSessionService, @Nonnull Account account) {
		LocalDateTime startDateTime = LocalDateTime.now().plusDays(7);
		CreateGroupSessionRequest request = new CreateGroupSessionRequest();
		request.setInstitutionId(account.getInstitutionId());
		request.setGroupSessionSchedulingSystemId(GroupSessionSchedulingSystemId.COBALT);
		request.setGroupSessionLocationTypeId(GroupSessionLocationTypeId.IN_PERSON);
		request.setSubmitterAccountId(account.getAccountId());
		request.setTitle("Page row duplication group session");
		request.setDescription("Page row duplication group session description");
		request.setUrlName(format("page-row-duplication-group-session-%s", UUID.randomUUID()));
		request.setInPersonLocation("Test location");
		request.setFacilitatorName("Test Facilitator");
		request.setFacilitatorEmailAddress("facilitator@example.com");
		request.setStartDateTime(startDateTime);
		request.setEndDateTime(startDateTime.plusHours(1));
		request.setGroupSessionVisibilityTypeId(GroupSessionVisibilityTypeId.PUBLIC);
		request.setDifferentEmailAddressForNotifications(false);
		request.setSingleSessionFlag(true);
		request.setSendFollowupEmail(false);
		request.setSendReminderEmail(false);
		return groupSessionService.createGroupSession(request, account);
	}

	@Nonnull
	private UUID createRow(@Nonnull Database database,
										 @Nonnull UUID pageSectionId,
										 @Nonnull UUID accountId,
										 @Nonnull RowTypeId rowTypeId,
										 @Nonnull String name,
										 int displayOrder,
										 @Nonnull BackgroundColorId backgroundColorId,
										 @Nonnull PageRowPaddingId paddingTopId,
										 @Nonnull PageRowPaddingId paddingBottomId) {
		UUID pageRowId = UUID.randomUUID();
		database.execute("""
				INSERT INTO page_row
				(page_row_id, page_row_anchor_id, page_section_id, row_type_id, name, background_color_id,
				 padding_id, padding_top_id, padding_bottom_id, deleted_flag, display_order, created_by_account_id)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, ?, ?)
				""", pageRowId, pageRowId, pageSectionId, rowTypeId, name, backgroundColorId, paddingTopId,
				paddingTopId, paddingBottomId, displayOrder, accountId);
		return pageRowId;
	}

	@Nonnull
	private UUID duplicateAndAssertPosition(@Nonnull PageService pageService,
																			 @Nonnull UUID sourcePageRowId,
																			 @Nonnull Account account,
																			 @Nonnull String expectedName) {
		UUID duplicatedPageRowId = pageService.duplicatePageRow(sourcePageRowId, account.getAccountId(),
				account.getInstitutionId());
		PageRow source = pageService.findPageRowById(sourcePageRowId, account.getInstitutionId()).orElseThrow();
		PageRow duplicate = pageService.findPageRowById(duplicatedPageRowId, account.getInstitutionId()).orElseThrow();
		List<PageRow> rows = pageService.findPageRowsBySectionId(source.getPageSectionId(), account.getInstitutionId());
		int sourceIndex = rows.stream().map(PageRow::getPageRowId).toList().indexOf(sourcePageRowId);

		Assert.assertEquals(sourcePageRowId, rows.get(sourceIndex).getPageRowId());
		Assert.assertEquals(duplicatedPageRowId, rows.get(sourceIndex + 1).getPageRowId());
		Assert.assertEquals(Integer.valueOf(source.getDisplayOrder() + 1), duplicate.getDisplayOrder());
		Assert.assertEquals(expectedName, duplicate.getName());
		Assert.assertNotEquals(source.getPageRowId(), duplicate.getPageRowId());
		Assert.assertNotEquals(source.getPageRowAnchorId(), duplicate.getPageRowAnchorId());
		Assert.assertEquals(duplicate.getPageRowId(), duplicate.getPageRowAnchorId());
		return duplicatedPageRowId;
	}

	private void assertCopiedRowMetadata(@Nonnull PageService pageService,
																			 @Nonnull UUID sourcePageRowId,
																			 @Nonnull UUID duplicatePageRowId) {
		PageRow source = pageService.findPageRowById(sourcePageRowId, InstitutionId.COBALT).orElseThrow();
		PageRow duplicate = pageService.findPageRowById(duplicatePageRowId, InstitutionId.COBALT).orElseThrow();
		Assert.assertEquals(source.getRowTypeId(), duplicate.getRowTypeId());
		Assert.assertEquals(source.getBackgroundColorId(), duplicate.getBackgroundColorId());
		Assert.assertEquals(source.getPaddingId(), duplicate.getPaddingId());
		Assert.assertEquals(source.getPaddingTopId(), duplicate.getPaddingTopId());
		Assert.assertEquals(source.getPaddingBottomId(), duplicate.getPaddingBottomId());
	}

	private void assertColumnsCopied(@Nonnull PageService pageService,
															 @Nonnull UUID sourcePageRowId,
															 @Nonnull UUID duplicatePageRowId,
															 @Nonnull UUID expectedImageFileUploadId) {
		List<PageRowColumn> sourceColumns = pageService.findPageRowColumnsByPageRowId(sourcePageRowId);
		List<PageRowColumn> duplicateColumns = pageService.findPageRowColumnsByPageRowId(duplicatePageRowId);
		Assert.assertEquals(sourceColumns.size(), duplicateColumns.size());

		for (int index = 0; index < sourceColumns.size(); ++index) {
			PageRowColumn source = sourceColumns.get(index);
			PageRowColumn duplicate = duplicateColumns.get(index);
			Assert.assertNotEquals(source.getPageRowColumnId(), duplicate.getPageRowColumnId());
			Assert.assertEquals(duplicatePageRowId, duplicate.getPageRowId());
			Assert.assertEquals(source.getHeadline(), duplicate.getHeadline());
			Assert.assertEquals(source.getDescription(), duplicate.getDescription());
			Assert.assertEquals(source.getImageFileUploadId(), duplicate.getImageFileUploadId());
			Assert.assertEquals(source.getImageAltText(), duplicate.getImageAltText());
			Assert.assertEquals(source.getUsePlaceholderImage(), duplicate.getUsePlaceholderImage());
			Assert.assertEquals(source.getColumnDisplayOrder(), duplicate.getColumnDisplayOrder());
			Assert.assertEquals(source.getContentOrderId(), duplicate.getContentOrderId());
		}

		Assert.assertEquals(expectedImageFileUploadId, duplicateColumns.get(0).getImageFileUploadId());
	}

	private void assertCallToActionCopied(@Nonnull PageService pageService,
																			 @Nonnull UUID sourcePageRowId,
																			 @Nonnull UUID duplicatePageRowId,
																			 @Nonnull UUID expectedImageFileUploadId) {
		PageRowCallToAction source = pageService.findPageRowCallToActionByRowId(sourcePageRowId).orElseThrow();
		PageRowCallToAction duplicate = pageService.findPageRowCallToActionByRowId(duplicatePageRowId).orElseThrow();
		Assert.assertNotEquals(source.getPageRowCallToActionId(), duplicate.getPageRowCallToActionId());
		Assert.assertEquals(source.getHeadline(), duplicate.getHeadline());
		Assert.assertEquals(source.getDescription(), duplicate.getDescription());
		Assert.assertEquals(source.getButtonText(), duplicate.getButtonText());
		Assert.assertEquals(source.getButtonUrl(), duplicate.getButtonUrl());
		Assert.assertEquals(expectedImageFileUploadId, duplicate.getImageFileUploadId());
	}

	private void assertAssociationCopied(@Nonnull Database database,
															 @Nonnull String table,
															 @Nonnull String associationIdColumn,
															 @Nonnull String referenceColumn,
															 @Nonnull UUID sourcePageRowId,
															 @Nonnull UUID duplicatePageRowId,
															 @Nonnull UUID expectedReferenceId) {
		UUID sourceAssociationId = database.queryForObject(format("SELECT %s FROM %s WHERE page_row_id = ?",
				associationIdColumn, table), UUID.class, sourcePageRowId).orElseThrow();
		UUID duplicateAssociationId = database.queryForObject(format("SELECT %s FROM %s WHERE page_row_id = ?",
				associationIdColumn, table), UUID.class, duplicatePageRowId).orElseThrow();
		UUID duplicateReferenceId = database.queryForObject(format("SELECT %s FROM %s WHERE page_row_id = ?",
				referenceColumn, table), UUID.class, duplicatePageRowId).orElseThrow();
		Assert.assertNotEquals(sourceAssociationId, duplicateAssociationId);
		Assert.assertEquals(expectedReferenceId, duplicateReferenceId);
	}

	private void assertStringAssociationCopied(@Nonnull Database database,
																				 @Nonnull String table,
																				 @Nonnull String associationIdColumn,
																				 @Nonnull String referenceColumn,
																				 @Nonnull UUID sourcePageRowId,
																				 @Nonnull UUID duplicatePageRowId,
																				 @Nonnull String expectedReferenceId) {
		UUID sourceAssociationId = database.queryForObject(format("SELECT %s FROM %s WHERE page_row_id = ?",
				associationIdColumn, table), UUID.class, sourcePageRowId).orElseThrow();
		UUID duplicateAssociationId = database.queryForObject(format("SELECT %s FROM %s WHERE page_row_id = ?",
				associationIdColumn, table), UUID.class, duplicatePageRowId).orElseThrow();
		String duplicateReferenceId = database.queryForObject(format("SELECT %s FROM %s WHERE page_row_id = ?",
				referenceColumn, table), String.class, duplicatePageRowId).orElseThrow();
		Assert.assertNotEquals(sourceAssociationId, duplicateAssociationId);
		Assert.assertEquals(expectedReferenceId, duplicateReferenceId);
	}

	@Nonnull
	private PageRow findRowByName(@Nonnull List<PageRow> pageRows, @Nonnull String name) {
		return pageRows.stream()
				.filter(pageRow -> name.equals(pageRow.getName()))
				.findFirst()
				.orElseThrow();
	}
}
