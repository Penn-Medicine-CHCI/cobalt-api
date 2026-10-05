package com.cobaltplatform.api.service;

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PageLocation.PageLocationId;
import com.cobaltplatform.api.model.db.SiteLocation.SiteLocationId;
import com.cobaltplatform.api.model.service.NavigationItem;
import org.junit.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class PageNavigationTests {
	@Test
	public void navigationUsesSelectedPlacementsInOrderWithSupportingTextAndActiveSchedules() {
		PageDirectoryTests.run(f -> {
			UUID later = f.create("Later selected", PageLocationId.COMMUNITY);
			UUID first = f.create("First selected", PageLocationId.COMMUNITY);
			UUID unselected = f.create("Unselected", PageLocationId.COMMUNITY);
			UUID otherCategory = f.create("Selected other category", PageLocationId.FEATURED_TOPIC);
			UUID future = f.create("Future", PageLocationId.COMMUNITY);
			UUID expired = f.create("Expired", PageLocationId.COMMUNITY);
			UUID draft = f.create("Draft", PageLocationId.COMMUNITY);
			UUID deleted = f.create("Deleted", PageLocationId.COMMUNITY);
			UUID otherInstitution = f.create("Other institution", PageLocationId.COMMUNITY);
			for (UUID id : List.of(later, first, unselected, otherCategory, future, expired, deleted, otherInstitution))
				f.service.publishPage(id, InstitutionId.COBALT);
			f.placement(later, SiteLocationId.COMMUNITY, 8, false);
			f.placement(first, SiteLocationId.COMMUNITY, 2, false);
			f.placement(otherCategory, SiteLocationId.COMMUNITY, 5, false);
			f.placement(future, SiteLocationId.COMMUNITY, 0, true);
			for (UUID id : List.of(expired, draft, deleted, otherInstitution))
				f.placement(id, SiteLocationId.COMMUNITY, 1, false);
			String description = "<p>Supporting text with <strong>emphasis</strong>.</p>";
			f.database.execute("UPDATE page_site_location SET short_description=?, publish_start_date=NOW()-INTERVAL '1 day' WHERE page_id=?", description, first);
			f.database.execute("UPDATE page_site_location SET publish_end_date=NOW()-INTERVAL '1 second' WHERE page_id=?", expired);
			f.database.execute("UPDATE page SET deleted_flag=TRUE WHERE page_id=?", deleted);
			f.database.execute("UPDATE page SET institution_id='COBALT_IC' WHERE page_id=?", otherInstitution);

			List<NavigationItem> items = navigation(f);
			assertEquals(List.of(first, otherCategory, later), items.stream().map(NavigationItem::getPageId).toList());
			NavigationItem item = items.get(0);
			assertEquals(f.page(first).getName(), item.getName());
			assertEquals("/pages/" + f.page(first).getUrlName(), item.getUrl());
			assertEquals(f.page(first).getUrlName(), item.getPageUrlName());
			assertNotNull(item.getImageUrl());
			assertEquals(description, item.getShortDescription());
			assertEquals(description, f.mapper.toMap(item).get("shortDescription"));
			assertNull(items.get(2).getShortDescription());
			assertFalse(f.mapper.toMap(items.get(2)).containsKey("shortDescription"));
			// Category controls directory membership even when no placement is selected or its window is inactive.
			List<UUID> directory = f.directory(PageLocationId.COMMUNITY, 0, 25).getResults().stream().map(p -> p.getPageId()).toList();
			assertTrue(directory.containsAll(List.of(first, later, unselected, future, expired)));
			assertFalse(directory.contains(otherCategory));
		});
	}

	@Test
	public void publishingEditingCopiesPreservesSelectionOrderSupportingTextAndSchedule() {
		PageDirectoryTests.run(f -> {
			UUID source = f.create("Selected source", PageLocationId.COMMUNITY);
			f.placement(source, SiteLocationId.COMMUNITY, 17, false);
			String description = "<p>Curated supporting text</p>";
			f.database.execute("UPDATE page_site_location SET short_description=?, publish_start_date=NOW()-INTERVAL '1 day', publish_end_date=NOW()+INTERVAL '2 days' WHERE page_id=?", description, source);
			Instant start = placementDate(f, source, "publish_start_date");
			Instant end = placementDate(f, source, "publish_end_date");
			f.service.publishPage(source, InstitutionId.COBALT);
			UUID editing = f.duplicate(source, true);
			assertEquals(List.of(source), navigation(f).stream().map(NavigationItem::getPageId).toList());
			assertEquals(Integer.valueOf(17), f.database.queryForObject("SELECT display_order FROM page_site_location WHERE page_id=? AND site_location_id='COMMUNITY'", Integer.class, editing).orElseThrow());
			assertEquals(start, placementDate(f, editing, "publish_start_date"));
			assertEquals(end, placementDate(f, editing, "publish_end_date"));
			f.service.publishPage(editing, InstitutionId.COBALT);
			assertEquals(List.of(editing), navigation(f).stream().map(NavigationItem::getPageId).toList());
			assertEquals(description, navigation(f).get(0).getShortDescription());
			f.service.unpublishPage(editing, InstitutionId.COBALT);
			assertTrue(navigation(f).isEmpty());
			assertTrue(f.page(editing).getHasBeenPublished());
			f.service.publishPage(editing, InstitutionId.COBALT);
			assertEquals(List.of(editing), navigation(f).stream().map(NavigationItem::getPageId).toList());
			f.service.deletePage(editing, InstitutionId.COBALT);
			assertTrue(navigation(f).isEmpty());
		});
	}

	@Test
	public void legacyNavigationRetainsSelectionOrderRoutesAndNavigationDescriptions() {
		PageDirectoryTests.run(f -> {
			UUID later = legacyTopic(f, "Later", true, 8, "<p>Legacy supporting text</p>");
			UUID first = legacyTopic(f, "First", true, 2, null);
			legacyTopic(f, "Unselected", false, 0, "Hidden description");
			List<NavigationItem> items = f.topicCenterService.findTopicCenterNavigationItemsByInstitutionId(InstitutionId.COBALT)
					.stream().filter(item -> item.getName().startsWith(f.prefix)).toList();
			assertEquals(List.of(first, later), items.stream().map(NavigationItem::getTopicCenterId).toList());
			assertEquals("/topic-centers/" + f.prefix + "-Later", items.get(1).getUrl());
			assertEquals("<p>Legacy supporting text</p>", f.mapper.toMap(items.get(1)).get("shortDescription"));
			assertNull(items.get(0).getShortDescription());
		});
	}

	private static UUID legacyTopic(PageDirectoryTests.Fixture f, String name, boolean enabled, int order, String description) {
		UUID id = UUID.randomUUID();
		f.database.execute("INSERT INTO topic_center (topic_center_id,name,url_name,created_by_account_id,nav_description) VALUES (?,?,?,?,?)", id, f.prefix + " " + name, f.prefix + "-" + name, f.account.getAccountId(), description);
		f.database.execute("INSERT INTO institution_topic_center (institution_id,topic_center_id,navigation_item_enabled,navigation_display_order) VALUES ('COBALT',?,?,?)", id, enabled, order);
		return id;
	}

	private static List<NavigationItem> navigation(PageDirectoryTests.Fixture f) {
		return f.service.findPageNavigationItemsBySiteLocationId(SiteLocationId.COMMUNITY, InstitutionId.COBALT)
				.stream().filter(item -> item.getName().startsWith(f.prefix)).toList();
	}

	private static Instant placementDate(PageDirectoryTests.Fixture f, UUID id, String column) {
		return f.database.queryForObject("SELECT " + column + " FROM page_site_location WHERE page_id=? AND site_location_id='COMMUNITY'", Instant.class, id).orElseThrow();
	}
}
