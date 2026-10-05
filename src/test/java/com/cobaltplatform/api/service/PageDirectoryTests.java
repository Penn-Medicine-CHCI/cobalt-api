package com.cobaltplatform.api.service;

import com.cobaltplatform.api.App;
import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.model.api.request.CreatePageRequest;
import com.cobaltplatform.api.model.api.request.DuplicatePageRequest;
import com.cobaltplatform.api.model.api.request.FindPublishedPagesRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageSettingsRequest;
import com.cobaltplatform.api.model.api.response.PageApiResponse;
import com.cobaltplatform.api.model.api.response.PageLocationApiResponse;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Page;
import com.cobaltplatform.api.model.db.PageLocation.PageLocationId;
import com.cobaltplatform.api.model.db.PageStatus.PageStatusId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.SiteLocation.SiteLocationId;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.cobaltplatform.api.web.resource.PageResource;
import com.pyranid.Database;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class PageDirectoryTests {
	@Test
	public void settingsMetadataAndSummaryResponsesRoundTripWithoutChangingPlacements() {
		run(f -> {
			List<PageLocationApiResponse> locations = value(f.resource.pageLocations(), "pageLocations");
			assertEquals(List.of(PageLocationId.FEATURED_TOPIC, PageLocationId.COMMUNITY, PageLocationId.UNCATEGORIZED),
					locations.stream().map(PageLocationApiResponse::getPageLocationId).toList());
			assertEquals(List.of("Featured Topics","Penn Communities","Uncategorized"), locations.stream().map(PageLocationApiResponse::getDescription).toList());
			assertEquals("/featured-topics", locations.get(0).getDirectoryRelativeUrl().orElseThrow());
			assertEquals("/communities", locations.get(1).getDirectoryRelativeUrl().orElseThrow());
			assertFalse(f.mapper.toMap(locations.get(2)).containsKey("directoryRelativeUrl"));

			PageApiResponse created = value(f.resource.createPage("{\"name\":\"" + f.prefix + " default\",\"urlName\":\"" + f.prefix + "-default\"}"), "page");
			assertEquals(PageLocationId.UNCATEGORIZED, created.getPageLocationId());
			assertEquals("Uncategorized", created.getPageLocationDescription());
			assertEquals("Unpublished", created.getPageStatusDescription());
			UUID pageId = created.getPageId();
			f.placement(pageId, SiteLocationId.FEATURED_TOPIC, 42, true);
			Instant placementUpdated = f.database.queryForObject("SELECT last_updated FROM page_site_location WHERE page_id=?", Instant.class, pageId).orElseThrow();
			for (PageLocationId location : List.of(PageLocationId.FEATURED_TOPIC, PageLocationId.COMMUNITY, PageLocationId.UNCATEGORIZED)) {
				PageApiResponse saved = value(f.resource.updatePageSettings(f.settingsBody(created, "\"" + location.name() + "\""), pageId), "page");
				assertEquals(location, saved.getPageLocationId());
				assertEquals(location.name(), f.mapper.toMap(saved).get("pageLocationId"));
			}
			f.resource.updatePageSettings(f.settingsBody(created, "\"COMMUNITY\""), pageId);
			for (String location : new String[]{null, "null"}) {
				PageApiResponse saved = value(f.resource.updatePageSettings(f.settingsBody(created, location), pageId), "page");
				assertEquals(PageLocationId.COMMUNITY, saved.getPageLocationId());
			}
			assertEquals(placementUpdated, f.database.queryForObject("SELECT last_updated FROM page_site_location WHERE page_id=?", Instant.class, pageId).orElseThrow());
			assertEquals(Integer.valueOf(42), f.database.queryForObject("SELECT display_order FROM page_site_location WHERE page_id=?", Integer.class, pageId).orElseThrow());
			List<PageApiResponse> adminPages = value(f.resource.pages(Optional.empty(), Optional.empty(), Optional.of(f.prefix), Optional.empty()), "pages");
			assertEquals("Penn Communities", adminPages.stream().filter(p -> p.getPageId().equals(pageId)).findFirst().orElseThrow().getPageLocationDescription());
			assertTrue(f.directory(PageLocationId.COMMUNITY, 0, 25).getResults().isEmpty());
			UUID published = f.create("Patient-readable community", PageLocationId.COMMUNITY);
			f.service.publishPage(published, InstitutionId.COBALT);

			Account unauthorized = f.account;
			unauthorized.setRoleId(RoleId.PATIENT);
			unauthorized.setAccountCapabilityTypeIdsAsString(null);
			assertThrows(AuthorizationException.class, () -> f.resource.updatePageSettings(f.settingsBody(created, "\"FEATURED_TOPIC\""), pageId));
			assertThrows(AuthorizationException.class, () -> f.resource.createPage("{}"));
			assertEquals(PageLocationId.COMMUNITY, f.page(pageId).getPageLocationId());
			List<PageApiResponse> patientPages = value(f.resource.publishedPages(Optional.of(PageLocationId.COMMUNITY), Optional.of(f.prefix), Optional.empty(), Optional.empty()), "pages");
			assertEquals(List.of(published), patientPages.stream().map(PageApiResponse::getPageId).toList());
		});
	}

	@Test
	public void directoriesFilterPublishedPagesAlphabeticallyAcrossPagesAndSearchFields() {
		run(f -> {
			UUID beta = f.create("beta", PageLocationId.FEATURED_TOPIC);
			UUID alpha = f.create("Alpha", PageLocationId.FEATURED_TOPIC);
			UUID sameAlpha = f.create("Alpha copy", PageLocationId.FEATURED_TOPIC);
			f.database.execute("UPDATE page SET name=? WHERE page_id=?", f.prefix + " alpha", sameAlpha);
			UUID zulu = f.create("Zulu", PageLocationId.FEATURED_TOPIC);
			UUID draft = f.create("Draft", PageLocationId.FEATURED_TOPIC);
			UUID editing = f.create("Editing", PageLocationId.FEATURED_TOPIC);
			UUID deleted = f.create("Deleted", PageLocationId.FEATURED_TOPIC);
			UUID community = f.create("Community", PageLocationId.COMMUNITY);
			UUID uncategorized = f.create("Uncategorized", null);
			UUID otherTenant = f.create("Other tenant", PageLocationId.FEATURED_TOPIC);
			for (UUID id : List.of(beta,alpha,sameAlpha,zulu,deleted,community,uncategorized,otherTenant))
				f.service.publishPage(id, InstitutionId.COBALT);
			f.database.execute("UPDATE page SET page_status_id='COPY_FOR_EDITING' WHERE page_id=?", editing);
			f.database.execute("UPDATE page SET deleted_flag=TRUE WHERE page_id=?", deleted);
			f.database.execute("UPDATE page SET institution_id='COBALT_IC' WHERE page_id=?", otherTenant);
			f.database.execute("UPDATE page SET headline=? WHERE page_id=?", f.prefix + " unique headline term", beta);
			f.database.execute("UPDATE page SET url_name=? WHERE page_id=?", f.prefix + "-slugmatch", zulu);
			f.placement(alpha, SiteLocationId.FEATURED_TOPIC, 1, true);
			f.placement(beta, SiteLocationId.COMMUNITY, 0, false);

			List<UUID> alphas = new ArrayList<>(List.of(alpha,sameAlpha));
			alphas.sort(java.util.Comparator.comparing(UUID::toString));
			FindResult<Page> first = f.directory(PageLocationId.FEATURED_TOPIC, 0, 2);
			assertEquals(Integer.valueOf(4), first.getTotalCount());
			assertEquals(alphas, ids(first));
			assertEquals(List.of(beta,zulu), ids(f.directory(PageLocationId.FEATURED_TOPIC, 1, 2)));
			FindResult<Page> beyond = f.directory(PageLocationId.FEATURED_TOPIC, 50, 2);
			assertEquals(Integer.valueOf(4), beyond.getTotalCount());
			assertTrue(beyond.getResults().isEmpty());
			assertEquals(ids(first), ids(f.directory(PageLocationId.FEATURED_TOPIC, -1, 2)));
			assertEquals(List.of(community), ids(f.directory(PageLocationId.COMMUNITY, 0, 25)));
			assertEquals(List.of(beta), ids(f.search(PageLocationId.FEATURED_TOPIC, (f.prefix + " unique headline term").toUpperCase(Locale.US))));
			assertEquals(List.of(zulu), ids(f.search(PageLocationId.FEATURED_TOPIC, f.prefix + "-slugmatch")));
			assertEquals(alphas, ids(f.search(PageLocationId.FEATURED_TOPIC, (f.prefix + " ALPHA").toUpperCase(Locale.US))));
			assertEquals(Integer.valueOf(0), f.search(PageLocationId.FEATURED_TOPIC, "no-matching-title").getTotalCount());
			assertFalse(f.service.findAllPagesBySiteLocation(SiteLocationId.FEATURED_TOPIC, InstitutionId.COBALT).stream().anyMatch(p -> p.getPageId().equals(alpha)));
			assertTrue(f.service.findPageNavigationItemsBySiteLocationId(SiteLocationId.COMMUNITY, InstitutionId.COBALT).stream().anyMatch(p -> p.getPageId().equals(beta)));
			assertFalse(f.service.findPageNavigationItemsBySiteLocationId(SiteLocationId.COMMUNITY, InstitutionId.COBALT).stream().anyMatch(p -> p.getPageId().equals(community)));
			assertThrows(NotFoundException.class, () -> f.resource.publishedPage(otherTenant.toString()));
			UpdatePageSettingsRequest wrongTenant = f.settings(alpha, PageLocationId.COMMUNITY);
			wrongTenant.setInstitutionId(InstitutionId.COBALT_IC);
			assertThrows(ValidationException.class, () -> f.service.updatePageSettings(wrongTenant));
			assertEquals(PageLocationId.FEATURED_TOPIC, f.page(alpha).getPageLocationId());

			List<PageApiResponse> apiPages = value(f.resource.publishedPages(Optional.of(PageLocationId.FEATURED_TOPIC), Optional.of(f.prefix), Optional.of(0), Optional.of(2)), "pages");
			assertEquals(alphas, apiPages.stream().map(PageApiResponse::getPageId).toList());
			Map<String,Object> serialized = f.mapper.toMap(apiPages.get(0));
			assertEquals("Published", serialized.get("pageStatusDescription"));
			assertEquals("Featured Topics", serialized.get("pageLocationDescription"));
			assertEquals("LIVE", serialized.get("pageStatusId"));
			assertEquals(f.page(alphas.get(0)).getName(), serialized.get("name"));
			assertNotNull(serialized.get("imageUrl"));
			assertEquals("Directory image", serialized.get("imageAltText"));
			assertTrue(serialized.get("relativeUrl").toString().startsWith("/pages/"));
			ApiResponse emptyLaterPage = f.resource.publishedPages(Optional.of(PageLocationId.FEATURED_TOPIC), Optional.of(f.prefix), Optional.of(50), Optional.of(2));
			assertEquals(Integer.valueOf(4), value(emptyLaterPage, "totalCount"));
			assertEquals("4", value(emptyLaterPage, "totalCountDescription"));
			assertTrue(PageDirectoryTests.<List<PageApiResponse>>value(emptyLaterPage, "pages").isEmpty());
			assertThrows(ValidationException.class, () -> f.resource.publishedPages(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
		});
	}

	@Test
	public void publicationCopiesAndDeletionKeepDirectoryMembershipInSync() {
		run(f -> {
			UUID source = f.create("Source", PageLocationId.FEATURED_TOPIC);
			f.placement(source, SiteLocationId.FEATURED_TOPIC, 3, false);
			f.service.publishPage(source, InstitutionId.COBALT);
			UUID edit = f.duplicate(source, true);
			assertEquals(PageLocationId.FEATURED_TOPIC, f.page(edit).getPageLocationId());
			assertEquals("Copy for Editing", f.page(edit).getPageStatusDescription());
			f.service.updatePageSettings(f.settings(edit, PageLocationId.COMMUNITY));
			assertEquals(List.of(source), ids(f.directory(PageLocationId.FEATURED_TOPIC, 0, 25)));
			assertTrue(f.directory(PageLocationId.COMMUNITY, 0, 25).getResults().isEmpty());
			f.service.publishPage(edit, InstitutionId.COBALT);
			assertTrue(f.directory(PageLocationId.FEATURED_TOPIC, 0, 25).getResults().isEmpty());
			assertEquals(List.of(edit), ids(f.directory(PageLocationId.COMMUNITY, 0, 25)));
			assertTrue(f.service.findAllPagesBySiteLocation(SiteLocationId.FEATURED_TOPIC, InstitutionId.COBALT).stream().anyMatch(p -> p.getPageId().equals(edit)));
			UUID standalone = f.duplicate(edit, false);
			assertEquals(PageLocationId.COMMUNITY, f.page(standalone).getPageLocationId());
			assertEquals(PageStatusId.DRAFT, f.page(standalone).getPageStatusId());
			assertEquals(List.of(edit), ids(f.directory(PageLocationId.COMMUNITY, 0, 25)));
			f.service.unpublishPage(edit, InstitutionId.COBALT);
			assertTrue(f.directory(PageLocationId.COMMUNITY, 0, 25).getResults().isEmpty());
			f.service.publishPage(edit, InstitutionId.COBALT);
			f.service.deletePage(edit, InstitutionId.COBALT);
			assertTrue(f.directory(PageLocationId.COMMUNITY, 0, 25).getResults().isEmpty());
		});
	}

	@Test
	public void directoryPaginationUsesDefaultsCapsAndOverflowSafeOffsets() {
		run(f -> {
			for (int index=0; index<103; index++) {
				UUID pageId = f.create(String.format("Item %03d", index), PageLocationId.FEATURED_TOPIC);
				f.database.execute("UPDATE page SET page_status_id='LIVE',published_date=NOW() WHERE page_id=?", pageId);
			}
			assertEquals(25, f.directory(PageLocationId.FEATURED_TOPIC, null, null).getResults().size());
			assertEquals(25, f.directory(PageLocationId.FEATURED_TOPIC, 0, 0).getResults().size());
			assertEquals(100, f.directory(PageLocationId.FEATURED_TOPIC, 0, 500).getResults().size());
			assertEquals(3, f.directory(PageLocationId.FEATURED_TOPIC, 1, 100).getResults().size());
			FindResult<Page> beyond = f.directory(PageLocationId.FEATURED_TOPIC, Integer.MAX_VALUE, 100);
			assertEquals(Integer.valueOf(103), beyond.getTotalCount());
			assertTrue(beyond.getResults().isEmpty());
		});
	}

	private static List<UUID> ids(FindResult<Page> result) {
		return result.getResults().stream().map(Page::getPageId).toList();
	}

	@SuppressWarnings("unchecked")
	private static <T> T value(ApiResponse response, String key) {
		return (T)((Map<String,Object>)response.model().orElseThrow()).get(key);
	}

	static void run(Consumer<Fixture> test) {
		IntegrationTestExecutor.runTransactionallyAndForceRollback(app -> {
			Fixture fixture = new Fixture(app);
			app.getInjector().getInstance(CurrentContextExecutor.class).execute(
					new CurrentContext.Builder(fixture.account, Locale.US, ZoneId.of("America/New_York")).build(), () -> test.accept(fixture));
		});
	}

	static class Fixture {
		final String prefix = "directory-" + UUID.randomUUID();
		final Database database;
		final PageService service;
		final TopicCenterService topicCenterService;
		final PageResource resource;
		final JsonMapper mapper;
		final Account account;
		final UUID imageId = UUID.randomUUID();

		Fixture(App app) {
			database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			service = app.getInjector().getInstance(PageService.class);
			topicCenterService = app.getInjector().getInstance(TopicCenterService.class);
			resource = app.getInjector().getInstance(PageResource.class);
			mapper = app.getInjector().getInstance(JsonMapper.class);
			UUID accountId = database.queryForObject("SELECT account_id FROM account WHERE institution_id='COBALT' ORDER BY created LIMIT 1", UUID.class).orElseThrow();
			account = app.getInjector().getInstance(AccountService.class).findAccountById(accountId).orElseThrow();
			account.setRoleId(RoleId.ADMINISTRATOR);
			database.execute("""
					INSERT INTO file_upload (file_upload_id,account_id,url,storage_key,filename,content_type,file_upload_type_id)
					VALUES (?, ?, ?, ?, 'directory.jpg', 'image/jpeg', 'PAGE_IMAGE')
					""", imageId, accountId, "https://example.com/" + imageId + ".jpg", imageId.toString());
		}

		UUID create(String name, PageLocationId location) {
			CreatePageRequest request = new CreatePageRequest();
			request.setInstitutionId(InstitutionId.COBALT);
			request.setCreatedByAccountId(account.getAccountId());
			request.setName(prefix + " " + name);
			request.setUrlName(prefix + "-" + UUID.randomUUID());
			request.setHeadline("Headline " + name);
			request.setDescription("Description");
			request.setImageFileUploadId(imageId);
			request.setImageAltText("Directory image");
			request.setPageLocationId(location);
			return service.createPage(request);
		}

		Page page(UUID id) {
			return service.findPageById(id, InstitutionId.COBALT, true).orElseThrow();
		}

		FindResult<Page> directory(PageLocationId location, Integer number, Integer size) {
			FindPublishedPagesRequest request = new FindPublishedPagesRequest();
			request.setInstitutionId(InstitutionId.COBALT);
			request.setPageLocationId(location);
			request.setSearchQuery(prefix);
			request.setPageNumber(number);
			request.setPageSize(size);
			return service.findPublishedPages(request);
		}

		FindResult<Page> search(PageLocationId location, String search) {
			FindPublishedPagesRequest request = new FindPublishedPagesRequest();
			request.setInstitutionId(InstitutionId.COBALT);
			request.setPageLocationId(location);
			request.setSearchQuery(search);
			return service.findPublishedPages(request);
		}

		UpdatePageSettingsRequest settings(UUID pageId, PageLocationId location) {
			Page page = page(pageId);
			UpdatePageSettingsRequest request = new UpdatePageSettingsRequest();
			request.setPageId(pageId);
			request.setInstitutionId(InstitutionId.COBALT);
			request.setName(page.getName());
			request.setUrlName(page.getUrlName());
			request.setPageLocationId(location);
			return request;
		}

		String settingsBody(PageApiResponse page, String locationJson) {
			return "{\"name\":\"" + page.getName() + "\",\"urlName\":\"" + page.getUrlName() + "\""
					+ (locationJson == null ? "" : ",\"pageLocationId\":" + locationJson) + "}";
		}

		void placement(UUID pageId, SiteLocationId location, int order, boolean future) {
			database.execute("""
					INSERT INTO page_site_location (page_id,site_location_id,display_order,publish_start_date,call_to_action,created_by_account_id)
					VALUES (?,?,?,CASE WHEN ? THEN NOW()+INTERVAL '1 day' ELSE NULL END,'Read more',?)
					""", pageId, location, order, future, account.getAccountId());
		}

		UUID duplicate(UUID pageId, boolean editing) {
			DuplicatePageRequest request = new DuplicatePageRequest();
			request.setPageId(pageId);
			request.setCreatedByAccountId(account.getAccountId());
			request.setCopyForEditing(editing);
			request.setPageStatusId(editing ? PageStatusId.COPY_FOR_EDITING : PageStatusId.DRAFT);
			request.setName(prefix + " Duplicate " + UUID.randomUUID());
			request.setUrlName(prefix + "-duplicate-" + UUID.randomUUID());
			return service.duplicatePage(request, InstitutionId.COBALT);
		}
	}
}
