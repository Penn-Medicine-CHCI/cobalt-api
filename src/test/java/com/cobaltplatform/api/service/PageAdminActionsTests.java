package com.cobaltplatform.api.service;

import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowMailingListRequest;
import com.cobaltplatform.api.model.api.response.PageApiResponse;
import com.cobaltplatform.api.model.api.response.PageRowMailingListApiResponse;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.MailingListEntryType.MailingListEntryTypeId;
import com.cobaltplatform.api.model.db.PageLocation.PageLocationId;
import com.cobaltplatform.api.model.db.PageStatus.PageStatusId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.SiteLocation.SiteLocationId;
import com.cobaltplatform.api.service.PageDirectoryTests.Fixture;
import com.cobaltplatform.api.util.ValidationException;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import org.apache.commons.csv.CSVFormat;
import org.junit.Test;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.*;

public class PageAdminActionsTests {
	@Test
	public void editingPublishedPagesResumesSavedContentAndSettingsUntilPublication() {
		PageDirectoryTests.run(f -> {
			UUID publishedId = f.create("Saved edits", PageLocationId.FEATURED_TOPIC);
			assertEquals(publishedId, value(f.resource.editPage(publishedId.toString()), "pageId"));
			PageApiResponse published = value(f.resource.publishPage(publishedId), "page");
			UUID editId = value(f.resource.editPage(publishedId.toString()), "pageId");
			String savedHeadline = "Saved headline " + f.prefix;
			f.resource.updatePageHero(f.mapper.toJson(Map.of(
					"headline", savedHeadline, "description", published.getDescription(),
					"imageFileUploadId", f.imageId, "imageAltText", published.getImageAltText())), editId);
			f.resource.updatePageSettings(f.mapper.toJson(Map.of(
					"name", published.getName(), "urlName", published.getUrlName(),
					"pageLocationId", "COMMUNITY")), editId);

			assertEquals(editId, value(f.resource.editPage(publishedId.toString()), "pageId"));
			assertEquals(editId, value(f.resource.editPage(published.getUrlName()), "pageId"));
			PageApiResponse resumed = value(f.resource.page(editId.toString()), "page");
			assertEquals(savedHeadline, resumed.getHeadline());
			assertEquals(PageLocationId.COMMUNITY, resumed.getPageLocationId());
			PageApiResponse live = value(f.resource.publishedPage(published.getUrlName()), "page");
			assertEquals(published.getHeadline(), live.getHeadline());
			assertEquals(PageLocationId.FEATURED_TOPIC, live.getPageLocationId());

			f.resource.publishPage(editId);
			live = value(f.resource.publishedPage(published.getUrlName()), "page");
			assertEquals(savedHeadline, live.getHeadline());
			assertEquals(PageLocationId.COMMUNITY, live.getPageLocationId());
			UUID nextEditId = value(f.resource.editPage(editId.toString()), "pageId");
			assertNotEquals(editId, nextEditId);
			assertEquals(editId, f.page(nextEditId).getParentPageId());
			assertEquals(savedHeadline, f.page(nextEditId).getHeadline());
		});
	}

	@Test
	public void liveSettingsCarryForwardToAllAdminsSavedEditingCopiesAndPublication() {
		PageDirectoryTests.run(f -> {
			UUID publishedId = f.create("Live settings", PageLocationId.FEATURED_TOPIC);
			PageApiResponse published = value(f.resource.publishPage(publishedId), "page");
			UUID firstEditId = value(f.resource.editPage(publishedId.toString()), "pageId");
			String savedHeadline = "Saved builder content " + f.prefix;
			f.resource.updatePageHero(f.mapper.toJson(Map.of(
					"headline", savedHeadline, "description", published.getDescription(),
					"imageFileUploadId", f.imageId, "imageAltText", published.getImageAltText())), firstEditId);
			f.resource.updatePageSettings(f.mapper.toJson(Map.of(
					"name", f.prefix + " Pending name", "urlName", f.prefix + "-pending",
					"pageLocationId", "UNCATEGORIZED")), firstEditId);
			assertEquals(published.getName(), f.page(publishedId).getName());
			assertEquals(published.getUrlName(), f.page(publishedId).getUrlName());
			assertEquals(PageLocationId.FEATURED_TOPIC, f.page(publishedId).getPageLocationId());

			UUID firstAccountId = f.account.getAccountId();
			CreateAccountRequest accountRequest = new CreateAccountRequest();
			accountRequest.setAccountSourceId(AccountSourceId.ANONYMOUS);
			accountRequest.setInstitutionId(InstitutionId.COBALT);
			f.account.setAccountId(f.service.getAccountService().createAccount(accountRequest));
			UUID secondEditId = value(f.resource.editPage(publishedId.toString()), "pageId");
			f.account.setAccountId(firstAccountId);

			String latestName = f.prefix + " Latest live name";
			String latestUrlName = f.prefix + "-latest-live-url";
			f.resource.updatePageSettings(f.mapper.toJson(Map.of(
					"name", latestName, "urlName", latestUrlName, "pageLocationId", "COMMUNITY")), publishedId);
			for (UUID editingId : List.of(firstEditId, secondEditId)) {
				PageApiResponse editing = value(f.resource.page(editingId.toString()), "page");
				assertEquals(latestName, editing.getName());
				assertEquals(latestUrlName, editing.getUrlName());
				assertEquals(PageLocationId.COMMUNITY, editing.getPageLocationId());
			}
			assertEquals(firstEditId, value(f.resource.editPage(publishedId.toString()), "pageId"));
			assertEquals(firstEditId, value(f.resource.editPage(latestUrlName), "pageId"));
			assertEquals(savedHeadline, f.page(firstEditId).getHeadline());
			assertEquals(published.getHeadline(), f.page(publishedId).getHeadline());

			f.resource.publishPage(firstEditId);
			PageApiResponse live = value(f.resource.publishedPage(latestUrlName), "page");
			assertEquals(firstEditId, live.getPageId());
			assertEquals(latestName, live.getName());
			assertEquals(PageLocationId.COMMUNITY, live.getPageLocationId());
			assertEquals(savedHeadline, live.getHeadline());
			assertThrows(NotFoundException.class, () -> f.resource.publishedPage(published.getUrlName()));
		});
	}

	@Test
	public void liveSettingsSavesPreserveCopyOnlySettingsOnFieldsThatDidNotChange() {
		PageDirectoryTests.run(f -> {
			UUID publishedId = f.create("Partial settings", PageLocationId.FEATURED_TOPIC);
			PageApiResponse published = value(f.resource.publishPage(publishedId), "page");
			UUID editId = value(f.resource.editPage(publishedId.toString()), "pageId");
			String pendingName = f.prefix + " Pending name";
			String pendingUrlName = f.prefix + "-pending-url";
			f.resource.updatePageSettings(f.mapper.toJson(Map.of(
					"name", pendingName, "urlName", pendingUrlName, "pageLocationId", "COMMUNITY")), editId);

			f.resource.updatePageSettings(f.mapper.toJson(Map.of(
					"name", published.getName(), "urlName", published.getUrlName(),
					"pageLocationId", "FEATURED_TOPIC")), publishedId);
			PageApiResponse editing = value(f.resource.page(editId.toString()), "page");
			assertEquals(pendingName, editing.getName());
			assertEquals(pendingUrlName, editing.getUrlName());
			assertEquals(PageLocationId.COMMUNITY, editing.getPageLocationId());

			String latestName = f.prefix + " Latest name only";
			f.resource.updatePageSettings(f.mapper.toJson(Map.of(
					"name", latestName, "urlName", published.getUrlName())), publishedId);
			editing = value(f.resource.page(editId.toString()), "page");
			assertEquals(latestName, editing.getName());
			assertEquals(pendingUrlName, editing.getUrlName());
			assertEquals(PageLocationId.COMMUNITY, editing.getPageLocationId());
			PageApiResponse live = value(f.resource.publishedPage(published.getUrlName()), "page");
			assertEquals(latestName, live.getName());
			assertEquals(PageLocationId.FEATURED_TOPIC, live.getPageLocationId());

			f.resource.publishPage(editId);
			live = value(f.resource.publishedPage(pendingUrlName), "page");
			assertEquals(latestName, live.getName());
			assertEquals(PageLocationId.COMMUNITY, live.getPageLocationId());
		});
	}

	@Test
	public void liveSettingsSynchronizationRespectsEditingStatusDeletionParentAndInstitution() {
		PageDirectoryTests.run(f -> {
			UUID publishedId = f.create("Scoped settings", PageLocationId.FEATURED_TOPIC);
			PageApiResponse published = value(f.resource.publishPage(publishedId), "page");
			UUID deletedCopy = f.duplicate(publishedId, true);
			f.resource.deletePage(deletedCopy);
			UUID foreignCopy = f.duplicate(publishedId, true);
			f.database.execute("UPDATE page SET institution_id='COBALT_IC' WHERE page_id=?", foreignCopy);
			UUID draftCopy = f.duplicate(publishedId, true);
			f.database.execute("UPDATE page SET page_status_id='DRAFT' WHERE page_id=?", draftCopy);
			UUID otherParent = f.create("Other parent", PageLocationId.FEATURED_TOPIC);
			f.resource.publishPage(otherParent);
			UUID unrelatedCopy = value(f.resource.editPage(otherParent.toString()), "pageId");
			String unrelatedName = f.page(unrelatedCopy).getName();

			f.resource.updatePageSettings(f.mapper.toJson(Map.of(
					"name", f.prefix + " Updated scoped settings", "urlName", f.prefix + "-updated-scoped-settings",
					"pageLocationId", "COMMUNITY")), publishedId);
			for (UUID excludedId : List.of(deletedCopy, foreignCopy, draftCopy)) {
				assertEquals(published.getName(), f.database.queryForObject(
						"SELECT name FROM page WHERE page_id=?", String.class, excludedId).orElseThrow());
				assertEquals(published.getUrlName(), f.database.queryForObject(
						"SELECT url_name FROM page WHERE page_id=?", String.class, excludedId).orElseThrow());
				assertEquals(PageLocationId.FEATURED_TOPIC.name(), f.database.queryForObject(
						"SELECT page_location_id FROM page WHERE page_id=?", String.class, excludedId).orElseThrow());
			}
			assertEquals(unrelatedName, f.page(unrelatedCopy).getName());
			assertEquals(PageLocationId.FEATURED_TOPIC, f.page(unrelatedCopy).getPageLocationId());
		});
	}

	@Test
	public void editingResumesOnlyTheCurrentAdminsCopyAndReplacesDeletedCopies() {
		PageDirectoryTests.run(f -> {
			UUID publishedId = f.create("Separate edits", PageLocationId.COMMUNITY);
			f.resource.publishPage(publishedId);
			UUID firstEditId = value(f.resource.editPage(publishedId.toString()), "pageId");
			UUID firstAccountId = f.account.getAccountId();
			CreateAccountRequest accountRequest = new CreateAccountRequest();
			accountRequest.setAccountSourceId(AccountSourceId.ANONYMOUS);
			accountRequest.setInstitutionId(InstitutionId.COBALT);
			UUID secondAccountId = f.service.getAccountService().createAccount(accountRequest);
			f.account.setAccountId(secondAccountId);
			UUID secondEditId = value(f.resource.editPage(publishedId.toString()), "pageId");
			assertNotEquals(firstEditId, secondEditId);
			assertEquals(secondEditId, value(f.resource.editPage(publishedId.toString()), "pageId"));
			f.account.setAccountId(firstAccountId);
			assertEquals(firstEditId, value(f.resource.editPage(publishedId.toString()), "pageId"));

			f.resource.deletePage(firstEditId);
			UUID replacementEditId = value(f.resource.editPage(publishedId.toString()), "pageId");
			assertNotEquals(firstEditId, replacementEditId);
			assertNotEquals(secondEditId, replacementEditId);
			assertEquals(firstAccountId, f.page(replacementEditId).getCreatedByAccountId());
			assertThrows(NotFoundException.class, () -> f.resource.page(firstEditId.toString()));

			f.resource.publishPage(secondEditId);
			UUID currentEditId = value(f.resource.editPage(secondEditId.toString()), "pageId");
			assertNotEquals(replacementEditId, currentEditId);
			assertEquals(secondEditId, f.page(currentEditId).getParentPageId());
		});
	}

	@Test
	public void republishingAfterDraftEditsInvalidatesEveryAdminsEarlierEditingCopy() {
		PageDirectoryTests.run(f -> {
			UUID publishedId = f.create("Republished page", PageLocationId.FEATURED_TOPIC);
			PageApiResponse published = value(f.resource.publishPage(publishedId), "page");
			UUID firstEditId = value(f.resource.editPage(publishedId.toString()), "pageId");
			String savedHeadline = "Earlier saved content " + f.prefix;
			f.resource.updatePageHero(f.mapper.toJson(Map.of(
					"headline", savedHeadline, "description", published.getDescription(),
					"imageFileUploadId", f.imageId, "imageAltText", published.getImageAltText())), firstEditId);
			UUID firstAccountId = f.account.getAccountId();
			CreateAccountRequest accountRequest = new CreateAccountRequest();
			accountRequest.setAccountSourceId(AccountSourceId.ANONYMOUS);
			accountRequest.setInstitutionId(InstitutionId.COBALT);
			f.account.setAccountId(f.service.getAccountService().createAccount(accountRequest));
			UUID secondEditId = value(f.resource.editPage(publishedId.toString()), "pageId");
			f.account.setAccountId(firstAccountId);

			f.resource.unpublishPage(publishedId);
			assertEquals(publishedId, value(f.resource.editPage(publishedId.toString()), "pageId"));
			String latestHeadline = "Latest republished content " + f.prefix;
			String latestName = f.prefix + " Latest republished name";
			String latestUrlName = f.prefix + "-latest-republished-url";
			f.resource.updatePageHero(f.mapper.toJson(Map.of(
					"headline", latestHeadline, "description", published.getDescription(),
					"imageFileUploadId", f.imageId, "imageAltText", published.getImageAltText())), publishedId);
			f.resource.updatePageSettings(f.mapper.toJson(Map.of(
					"name", latestName, "urlName", latestUrlName, "pageLocationId", "COMMUNITY")), publishedId);
			f.resource.publishPage(publishedId);

			UUID freshEditId = value(f.resource.editPage(publishedId.toString()), "pageId");
			assertNotEquals(firstEditId, freshEditId);
			assertNotEquals(secondEditId, freshEditId);
			assertEquals(freshEditId, value(f.resource.editPage(latestUrlName), "pageId"));
			PageApiResponse fresh = value(f.resource.page(freshEditId.toString()), "page");
			assertEquals(latestHeadline, fresh.getHeadline());
			assertEquals(latestName, fresh.getName());
			assertEquals(latestUrlName, fresh.getUrlName());
			assertEquals(PageLocationId.COMMUNITY, fresh.getPageLocationId());
			for (UUID staleId : List.of(firstEditId, secondEditId)) {
				PageApiResponse saved = value(f.resource.page(staleId.toString()), "page");
				assertEquals(PageStatusId.COPY_FOR_EDITING, saved.getPageStatusId());
				assertNull(saved.getPublishedDate());
				assertThrows(ValidationException.class, () -> f.resource.publishPage(staleId));
				assertThrows(ValidationException.class, () -> f.resource.unpublishPage(staleId));
			}
			assertEquals(savedHeadline, f.page(firstEditId).getHeadline());
			PageApiResponse live = value(f.resource.publishedPage(latestUrlName), "page");
			assertEquals(publishedId, live.getPageId());
			assertEquals(latestHeadline, live.getHeadline());
			assertEquals(latestName, live.getName());
			assertEquals(PageLocationId.COMMUNITY, live.getPageLocationId());
			assertThrows(NotFoundException.class, () -> f.resource.publishedPage(published.getUrlName()));
		});
	}

	@Test
	public void unpublishingThroughEditingCopyPreservesResponseAndInvalidatesItAfterRepublishing() {
		PageDirectoryTests.run(f -> {
			UUID publishedId = f.create("Unpublished through copy", PageLocationId.COMMUNITY);
			PageApiResponse published = value(f.resource.publishPage(publishedId), "page");
			UUID editId = value(f.resource.editPage(publishedId.toString()), "pageId");

			PageApiResponse unpublishedCopy = value(f.resource.unpublishPage(editId), "page");
			assertEquals(editId, unpublishedCopy.getPageId());
			assertEquals(PageStatusId.COPY_FOR_EDITING, unpublishedCopy.getPageStatusId());
			assertNull(unpublishedCopy.getPublishedDate());
			assertEquals(PageStatusId.DRAFT, f.page(publishedId).getPageStatusId());
			assertNull(f.page(publishedId).getPublishedDate());
			assertThrows(ValidationException.class, () -> f.duplicate(publishedId, true));
			assertThrows(NotFoundException.class, () -> f.resource.publishedPage(published.getUrlName()));
			assertThrows(ValidationException.class, () -> f.resource.publishPage(editId));
			assertEquals(publishedId, value(f.resource.editPage(publishedId.toString()), "pageId"));

			String latestHeadline = "Content saved after unpublishing " + f.prefix;
			f.resource.updatePageHero(f.mapper.toJson(Map.of(
					"headline", latestHeadline, "description", published.getDescription(),
					"imageFileUploadId", f.imageId, "imageAltText", published.getImageAltText())), publishedId);
			f.resource.publishPage(publishedId);
			UUID freshEditId = value(f.resource.editPage(publishedId.toString()), "pageId");
			assertNotEquals(editId, freshEditId);
			assertEquals(latestHeadline, f.page(freshEditId).getHeadline());
			assertThrows(ValidationException.class, () -> f.resource.publishPage(editId));
			assertThrows(ValidationException.class, () -> f.resource.unpublishPage(editId));
			PageApiResponse live = value(f.resource.publishedPage(published.getUrlName()), "page");
			assertEquals(publishedId, live.getPageId());
			assertEquals(latestHeadline, live.getHeadline());
		});
	}

	@Test
	public void preexistingCopiesFromEarlierPublicationOnSameDayCannotResumeOrPublish() {
		PageDirectoryTests.run(f -> {
			UUID publishedId = f.create("Historical editing copy", PageLocationId.FEATURED_TOPIC);
			PageApiResponse published = value(f.resource.publishPage(publishedId), "page");
			UUID oldEditId = value(f.resource.editPage(publishedId.toString()), "pageId");
			f.resource.unpublishPage(publishedId);
			f.resource.publishPage(publishedId);
			// Represent a saved copy created before deployment, from an earlier publication on the same day.
			f.database.execute("UPDATE page SET published_date=TIMESTAMPTZ '2026-10-15 12:00:00.123456+00' WHERE page_id=?", publishedId);
			f.database.execute("UPDATE page SET published_date=TIMESTAMPTZ '2026-10-15 12:00:00.123455+00' WHERE page_id=?", oldEditId);
			assertEquals(f.page(publishedId).getPublishedDate(), f.page(oldEditId).getPublishedDate());

			UUID freshEditId = value(f.resource.editPage(publishedId.toString()), "pageId");
			assertNotEquals(oldEditId, freshEditId);
			assertEquals(freshEditId, value(f.resource.editPage(publishedId.toString()), "pageId"));
			assertThrows(ValidationException.class, () -> f.resource.publishPage(oldEditId));
			assertThrows(ValidationException.class, () -> f.resource.unpublishPage(oldEditId));
			PageApiResponse live = value(f.resource.publishedPage(published.getUrlName()), "page");
			assertEquals(publishedId, live.getPageId());
			assertEquals(published.getHeadline(), live.getHeadline());
			f.resource.publishPage(freshEditId);
			live = value(f.resource.publishedPage(published.getUrlName()), "page");
			assertEquals(freshEditId, live.getPageId());
		});
	}

	@Test
	public void creationSettingsAndDuplicationExposePublicationHistoryAndValidateRequiredFields() {
		PageDirectoryTests.run(f -> {
			assertThrows(ValidationException.class, () -> f.resource.createPage("{}"));
			assertThrows(ValidationException.class, () -> f.resource.createPage(f.mapper.toJson(Map.of(
					"name", f.prefix + " Empty normalized URL", "urlName", "/?#"))));
			String slug = f.prefix + "-created";
			PageApiResponse created = value(f.resource.createPage(f.mapper.toJson(Map.of(
					"name", f.prefix + " Created", "urlName", slug, "pageLocationId", "COMMUNITY"))), "page");
			assertEquals(PageStatusId.DRAFT, created.getPageStatusId());
			assertEquals(PageLocationId.COMMUNITY, created.getPageLocationId());
			assertFalse(created.getHasBeenPublished());
			assertFalse((Boolean) f.mapper.toMap(created).get("hasBeenPublished"));
			assertEquals(1, f.service.findPageSectionsByPageId(created.getPageId(), InstitutionId.COBALT).size());
			assertThrows(ValidationException.class, () -> f.resource.createPage(f.mapper.toJson(Map.of(
					"name", f.prefix + " Other", "urlName", slug.toUpperCase()))));
			assertThrows(ValidationException.class, () -> f.resource.updatePageSettings(
					f.mapper.toJson(Map.of("name", "", "urlName", slug)), created.getPageId()));
			for (String invalidSlug : List.of("invalid/path", "invalid?query", "invalid#fragment", "invalid%20path")) {
				assertThrows(ValidationException.class, () -> f.resource.updatePageSettings(
						f.mapper.toJson(Map.of("name", created.getName(), "urlName", invalidSlug)), created.getPageId()));
				assertThrows(ValidationException.class, () -> f.resource.duplicatePage(
						f.mapper.toJson(Map.of("name", f.prefix + " Invalid duplicate", "urlName", invalidSlug)), created.getPageId()));
			}
			assertEquals(created.getName(), f.page(created.getPageId()).getName());

			UUID publishedId = f.create("Published", PageLocationId.FEATURED_TOPIC);
			PageApiResponse published = value(f.resource.publishPage(publishedId), "page");
			assertTrue(published.getHasBeenPublished());
			assertNotNull(published.getPublishedDate());
			String oldSlug = published.getUrlName();
			String changedSlug = f.prefix + "-changed";
			PageApiResponse saved = value(f.resource.updatePageSettings(f.mapper.toJson(Map.of(
					"name", f.prefix + " Renamed", "urlName", changedSlug, "pageLocationId", "COMMUNITY")), publishedId), "page");
			assertTrue(saved.getHasBeenPublished());
			assertEquals("/pages/" + changedSlug, saved.getRelativeUrl());
			assertThrows(NotFoundException.class, () -> f.resource.publishedPage(oldSlug));
			PageApiResponse publicPage = value(f.resource.publishedPage(changedSlug), "page");
			assertEquals(saved.getName(), publicPage.getName());
			assertEquals(PageLocationId.COMMUNITY, publicPage.getPageLocationId());

			UUID editId = value(f.resource.editPage(publishedId.toString()), "pageId");
			PageApiResponse editing = value(f.resource.page(editId.toString()), "page");
			assertTrue(editing.getHasBeenPublished());
			assertEquals(PageStatusId.COPY_FOR_EDITING, editing.getPageStatusId());
			f.resource.updatePageSettings(f.mapper.toJson(Map.of(
					"name", f.prefix + " Pending name", "urlName", f.prefix + "-pending",
					"pageLocationId", "FEATURED_TOPIC")), editId);
			publicPage = value(f.resource.publishedPage(changedSlug), "page");
			assertEquals(saved.getName(), publicPage.getName());

			PageApiResponse duplicate = value(f.resource.duplicatePage(f.mapper.toJson(Map.of(
					"name", f.prefix + " Duplicate", "urlName", (f.prefix + "-duplicate").toUpperCase(java.util.Locale.ROOT),
					"copyForEditing", true)), publishedId), "page");
			assertEquals(PageStatusId.DRAFT, duplicate.getPageStatusId());
			assertEquals(PageLocationId.COMMUNITY, duplicate.getPageLocationId());
			assertFalse(duplicate.getHasBeenPublished());
			assertEquals(f.prefix + "-duplicate", duplicate.getUrlName());
			assertNull(duplicate.getPublishedDate());
			assertNull(f.page(duplicate.getPageId()).getParentPageId());
			assertNotEquals(published.getPageGroupId(), duplicate.getPageGroupId());

			PageApiResponse unpublished = value(f.resource.unpublishPage(publishedId), "page");
			assertEquals(PageStatusId.DRAFT, unpublished.getPageStatusId());
			assertTrue(unpublished.getHasBeenPublished());
			assertNull(unpublished.getPublishedDate());
			assertThrows(NotFoundException.class, () -> f.resource.publishedPage(changedSlug));
			PageApiResponse renamedAfterUnpublish = value(f.resource.updatePageSettings(f.mapper.toJson(Map.of(
					"name", unpublished.getName(), "urlName", f.prefix + "-unpublished")), publishedId), "page");
			assertTrue(renamedAfterUnpublish.getHasBeenPublished());
			List<PageApiResponse> summaries = value(f.resource.pages(Optional.empty(), Optional.empty(), Optional.of(f.prefix), Optional.empty()), "pages");
			assertTrue(summaries.stream().filter(p -> p.getPageId().equals(publishedId)).findFirst().orElseThrow().getHasBeenPublished());
		});
	}

	@Test
	public void subscriberActionsReturnActiveEntriesAndSupportEmailOnlyCsvDownloads() {
		PageDirectoryTests.run(f -> {
			UUID pageId = f.create("Subscribers", PageLocationId.COMMUNITY);
			UUID mailingListId = subscribeRow(f, pageId);
			entry(f, mailingListId, "EMAIL_ADDRESS", "SUBSCRIBED", "alpha@example.com");
			entry(f, mailingListId, "EMAIL_ADDRESS", "SUBSCRIBED", "beta@example.com");
			entry(f, mailingListId, "EMAIL_ADDRESS", "UNSUBSCRIBED", "unsubscribed@example.com");
			entry(f, mailingListId, "SMS", "SUBSCRIBED", "+12155550101");

			List<PageRowMailingListApiResponse> rows = value(f.resource.pageRowMailingLists(pageId.toString()), "pageRowMailingLists");
			assertEquals(1, rows.size());
			assertEquals(List.of("alpha@example.com", "beta@example.com", "+12155550101"),
					rows.get(0).getMailingListEntries().stream().map(e -> e.getValue()).toList());
			List<PageApiResponse> summaries = value(f.resource.pages(Optional.empty(), Optional.empty(), Optional.of(f.prefix), Optional.empty()), "pages");
			assertEquals(Integer.valueOf(3), summaries.get(0).getMailingListEntryCount().orElseThrow());

			try {
				CapturedResponse emailDownload = new CapturedResponse();
				f.resource.pageRowMailingListsCsv(pageId.toString(), Optional.of(MailingListEntryTypeId.EMAIL_ADDRESS), emailDownload.response);
				assertEquals("text/csv", emailDownload.contentType);
				assertEquals("gzip", emailDownload.headers.get("Content-Encoding"));
				assertTrue(emailDownload.headers.get("Content-Disposition").contains("attachment"));
				assertTrue(emailDownload.headers.get("Content-Disposition").contains(".csv"));
				try (var parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new StringReader(emailDownload.csv()))) {
					var records = parser.getRecords();
					assertEquals(List.of("Mailing List", "Type", "Value"), parser.getHeaderNames());
					assertEquals(List.of("alpha@example.com", "beta@example.com"), records.stream().map(r -> r.get("Value")).toList());
					assertTrue(records.stream().allMatch(r -> r.get("Type").equals("EMAIL_ADDRESS")));
				}
				CapturedResponse allDownload = new CapturedResponse();
				f.resource.pageRowMailingListsCsv(pageId.toString(), Optional.empty(), allDownload.response);
				try (var parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new StringReader(allDownload.csv()))) {
					assertEquals(3, parser.getRecords().size());
				}
				UUID emptyId = f.create("No subscribers", PageLocationId.COMMUNITY);
				CapturedResponse emptyDownload = new CapturedResponse();
				f.resource.pageRowMailingListsCsv(emptyId.toString(), Optional.of(MailingListEntryTypeId.EMAIL_ADDRESS), emptyDownload.response);
				try (var parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new StringReader(emptyDownload.csv()))) {
					assertTrue(parser.getRecords().isEmpty());
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	@Test
	public void adminActionsRequirePageManagementPermissionAndRespectInstitutionBoundaries() {
		PageDirectoryTests.run(f -> {
			UUID pageId = f.create("Own page", PageLocationId.COMMUNITY);
			UUID foreignId = f.create("Foreign page", PageLocationId.COMMUNITY);
			f.database.execute("UPDATE page SET institution_id='COBALT_IC' WHERE page_id=?", foreignId);
			String settings = f.mapper.toJson(Map.of("name", f.prefix + " Name", "urlName", f.prefix + "-name"));
			assertThrows(NotFoundException.class, () -> f.resource.editPage(foreignId.toString()));
			assertThrows(NotFoundException.class, () -> f.resource.page(foreignId.toString()));
			assertThrows(ValidationException.class, () -> f.resource.updatePageSettings(settings, foreignId));
			assertThrows(ValidationException.class, () -> f.resource.duplicatePage(settings, foreignId));
			assertThrows(ValidationException.class, () -> f.resource.publishPage(foreignId));
			assertThrows(ValidationException.class, () -> f.resource.unpublishPage(foreignId));
			assertThrows(NotFoundException.class, () -> f.resource.pageRowMailingLists(foreignId.toString()));
			assertThrows(NotFoundException.class, () -> f.resource.pageRowMailingListsCsv(foreignId.toString(), Optional.empty(), new CapturedResponse().response));
			f.resource.deletePage(foreignId);
			assertFalse(f.database.queryForObject("SELECT deleted_flag FROM page WHERE page_id=?", Boolean.class, foreignId).orElseThrow());

			f.account.setRoleId(RoleId.PATIENT);
			f.account.setAccountCapabilityTypeIdsAsString(null);
			assertThrows(AuthorizationException.class, () -> f.resource.createPage(settings));
			assertThrows(AuthorizationException.class, () -> f.resource.editPage(pageId.toString()));
			assertThrows(AuthorizationException.class, () -> f.resource.page(pageId.toString()));
			assertThrows(AuthorizationException.class, () -> f.resource.updatePageSettings(settings, pageId));
			assertThrows(AuthorizationException.class, () -> f.resource.duplicatePage(settings, pageId));
			assertThrows(AuthorizationException.class, () -> f.resource.publishPage(pageId));
			assertThrows(AuthorizationException.class, () -> f.resource.unpublishPage(pageId));
			assertThrows(AuthorizationException.class, () -> f.resource.deletePage(pageId));
			assertThrows(AuthorizationException.class, () -> f.resource.pageRowMailingLists(pageId.toString()));
			assertThrows(AuthorizationException.class, () -> f.resource.pageRowMailingListsCsv(pageId.toString(), Optional.empty(), new CapturedResponse().response));
			assertFalse(f.page(pageId).getDeletedFlag());
		});
	}

	@Test
	public void unpublishAndDeleteRemovePagesImmediatelyFromPublicAndAdminResults() {
		PageDirectoryTests.run(f -> {
			UUID pageId = f.create("Removed page", PageLocationId.FEATURED_TOPIC);
			f.placement(pageId, SiteLocationId.COMMUNITY, 1, false);
			f.resource.publishPage(pageId);
			String slug = f.page(pageId).getUrlName();
			assertEquals(1, f.directory(PageLocationId.FEATURED_TOPIC, 0, 25).getResults().size());
			f.resource.unpublishPage(pageId);
			assertTrue(f.directory(PageLocationId.FEATURED_TOPIC, 0, 25).getResults().isEmpty());
			assertThrows(NotFoundException.class, () -> f.resource.publishedPage(slug));
			assertFalse(f.service.findPageNavigationItemsBySiteLocationId(SiteLocationId.COMMUNITY, InstitutionId.COBALT)
					.stream().anyMatch(p -> p.getPageId().equals(pageId)));
			f.resource.deletePage(pageId);
			assertTrue(f.service.findPageById(pageId, InstitutionId.COBALT, true).isEmpty());
			assertThrows(NotFoundException.class, () -> f.resource.page(pageId.toString()));
			assertThrows(NotFoundException.class, () -> f.resource.pageRowMailingLists(pageId.toString()));
			List<PageApiResponse> summaries = value(f.resource.pages(Optional.empty(), Optional.empty(), Optional.of(f.prefix), Optional.empty()), "pages");
			assertTrue(summaries.isEmpty());
		});
	}

	private static UUID subscribeRow(Fixture f, UUID pageId) {
		CreatePageRowMailingListRequest request = new CreatePageRowMailingListRequest();
		request.setInstitutionId(InstitutionId.COBALT);
		request.setPageSectionId(f.service.findPageSectionsByPageId(pageId, InstitutionId.COBALT).get(0).getPageSectionId());
		request.setCreatedByAccountId(f.account.getAccountId());
		request.setTitle("Newsletter");
		request.setDescription("Get updates");
		UUID rowId = f.service.createPageRowMailingList(request);
		return f.service.findPageRowMailingListByRowId(rowId).orElseThrow().getMailingListId();
	}

	private static void entry(Fixture f, UUID mailingListId, String type, String status, String value) {
		f.database.execute("""
				INSERT INTO mailing_list_entry
				  (mailing_list_entry_id,mailing_list_id,mailing_list_entry_type_id,mailing_list_entry_status_id,account_id,created_by_account_id,value)
				VALUES (?,?,?,?,?,?,?)
				""", UUID.randomUUID(), mailingListId, type, status, f.account.getAccountId(), f.account.getAccountId(), value);
	}

	@SuppressWarnings("unchecked")
	private static <T> T value(ApiResponse response, String key) {
		return (T) ((Map<String, Object>) response.model().orElseThrow()).get(key);
	}

	private static class CapturedResponse {
		final ByteArrayOutputStream body = new ByteArrayOutputStream();
		final Map<String, String> headers = new HashMap<>();
		String contentType;
		final ServletOutputStream output = new ServletOutputStream() {
			@Override public void write(int value) { body.write(value); }
			@Override public boolean isReady() { return true; }
			@Override public void setWriteListener(WriteListener listener) { throw new UnsupportedOperationException(); }
		};
		final HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
				HttpServletResponse.class.getClassLoader(), new Class[]{HttpServletResponse.class}, (proxy, method, arguments) -> {
					switch (method.getName()) {
						case "setContentType": contentType = (String) arguments[0]; return null;
						case "setHeader": headers.put((String) arguments[0], (String) arguments[1]); return null;
						case "getOutputStream": return output;
						default: throw new UnsupportedOperationException(method.getName());
					}
				});

		String csv() throws IOException {
			try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(body.toByteArray()))) {
				return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
			}
		}
	}
}
