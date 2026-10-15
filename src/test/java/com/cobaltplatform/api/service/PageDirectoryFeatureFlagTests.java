package com.cobaltplatform.api.service;

import com.cobaltplatform.api.App;
import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.model.api.response.InstitutionApiResponse;
import com.cobaltplatform.api.model.api.response.InstitutionApiResponse.InstitutionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageApiResponse;
import com.cobaltplatform.api.model.api.response.PageLocationApiResponse;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PageLocation.PageLocationId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.SiteLocation.SiteLocationId;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.exception.ExceptionStatusMapper;
import com.soklet.web.response.ApiResponse;
import org.junit.Test;

import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class PageDirectoryFeatureFlagTests {
	@Test
	public void directoryAccessTracksFlagChangesForAnExistingSession() {
		run((f, app) -> {
			UUID featuredTopic = f.create("Featured topic", PageLocationId.FEATURED_TOPIC);
			UUID community = f.create("Community", PageLocationId.COMMUNITY);
			f.service.publishPage(featuredTopic, InstitutionId.COBALT);
			f.service.publishPage(community, InstitutionId.COBALT);
			f.account.setRoleId(RoleId.PATIENT);
			f.account.setAccountCapabilityTypeIdsAsString(null);

			setEnabled(f, InstitutionId.COBALT, false);
			ExceptionStatusMapper statusMapper = app.getInjector().getInstance(ExceptionStatusMapper.class);
			for (PageLocationId location : List.of(PageLocationId.FEATURED_TOPIC, PageLocationId.COMMUNITY)) {
				NotFoundException exception = assertThrows(NotFoundException.class, () -> directory(f, location));
				Integer status = statusMapper.statusForException(exception);
				assertEquals(Integer.valueOf(404), status);
			}

			setEnabled(f, InstitutionId.COBALT, true);
			assertEquals(List.of(featuredTopic), pageIds(directory(f, PageLocationId.FEATURED_TOPIC)));
			assertEquals(List.of(community), pageIds(directory(f, PageLocationId.COMMUNITY)));

			// The same resource and authenticated context must observe a subsequent disable.
			setEnabled(f, InstitutionId.COBALT, false);
			assertThrows(NotFoundException.class, () -> directory(f, PageLocationId.FEATURED_TOPIC));
			assertThrows(NotFoundException.class, () -> directory(f, PageLocationId.COMMUNITY));
			assertThrows(NotFoundException.class, () -> f.resource.publishedPages(
					Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
		});
	}

	@Test
	public void enabledInstitutionDoesNotEnableAnotherInstitutionsDirectories() {
		run((f, app) -> {
			UUID cobaltPage = f.create("Cobalt page", PageLocationId.FEATURED_TOPIC);
			UUID otherPage = f.create("Other institution page", PageLocationId.FEATURED_TOPIC);
			f.service.publishPage(cobaltPage, InstitutionId.COBALT);
			f.service.publishPage(otherPage, InstitutionId.COBALT);
			f.database.execute("UPDATE page SET institution_id=? WHERE page_id=?", InstitutionId.COBALT_IC, otherPage);
			setEnabled(f, InstitutionId.COBALT, true);
			setEnabled(f, InstitutionId.COBALT_IC, false);
			assertEquals(List.of(cobaltPage), pageIds(directory(f, PageLocationId.FEATURED_TOPIC)));

			f.account.setInstitutionId(InstitutionId.COBALT_IC);
			app.getInjector().getInstance(CurrentContextExecutor.class).execute(context(f), () -> {
				assertThrows(NotFoundException.class, () -> directory(f, PageLocationId.FEATURED_TOPIC));
				setEnabled(f, InstitutionId.COBALT_IC, true);
				assertEquals(List.of(otherPage), pageIds(directory(f, PageLocationId.FEATURED_TOPIC)));
			});
		});
	}

	@Test
	public void disablingDirectoriesPreservesIndividualPagesCategoryMetadataAndCuratedNavigation() {
		run((f, app) -> {
			UUID pageId = f.create("Existing published page", PageLocationId.COMMUNITY);
			f.placement(pageId, SiteLocationId.COMMUNITY, 0, false);
			f.service.publishPage(pageId, InstitutionId.COBALT);
			setEnabled(f, InstitutionId.COBALT, false);

			PageApiResponse page = value(f.resource.publishedPage(pageId.toString()), "page");
			assertEquals(pageId, page.getPageId());
			assertEquals(PageLocationId.COMMUNITY, page.getPageLocationId());
			List<PageLocationApiResponse> categories = value(f.resource.pageLocations(), "pageLocations");
			assertEquals(List.of(PageLocationId.FEATURED_TOPIC, PageLocationId.COMMUNITY, PageLocationId.UNCATEGORIZED),
					categories.stream().map(PageLocationApiResponse::getPageLocationId).toList());
			assertTrue(f.service.findPageNavigationItemsBySiteLocationId(SiteLocationId.COMMUNITY, InstitutionId.COBALT)
					.stream().anyMatch(item -> item.getPageId().equals(pageId)));
			assertThrows(NotFoundException.class, () -> directory(f, PageLocationId.COMMUNITY));
		});
	}

	@Test
	public void institutionResponseAlwaysSerializesAnExplicitBooleanDirectoryFlag() {
		run((f, app) -> {
			InstitutionService institutions = app.getInjector().getInstance(InstitutionService.class);
			InstitutionApiResponseFactory responses = app.getInjector().getInstance(InstitutionApiResponseFactory.class);
			for (boolean enabled : new boolean[]{false, true, false}) {
				setEnabled(f, InstitutionId.COBALT, enabled);
				Institution institution = institutions.findInstitutionById(InstitutionId.COBALT).orElseThrow();
				assertEquals(enabled, institution.getPageDirectoriesEnabled());
				assertEquals(enabled, institutions.isPageDirectoriesEnabled(InstitutionId.COBALT));
				InstitutionApiResponse response = responses.create(institution, context(f));
				assertEquals(enabled, response.getPageDirectoriesEnabled());
				assertEquals(enabled, f.mapper.toMap(response).get("pageDirectoriesEnabled"));
			}
			Institution institution = institutions.findInstitutionById(InstitutionId.COBALT).orElseThrow();
			institution.setPageDirectoriesEnabled(null);
			assertEquals(Boolean.FALSE, f.mapper.toMap(responses.create(institution, context(f))).get("pageDirectoriesEnabled"));
			assertFalse(institutions.isPageDirectoriesEnabled(null));
		});
	}

	private static void setEnabled(PageDirectoryTests.Fixture fixture, InstitutionId institutionId, boolean enabled) {
		fixture.database.execute("UPDATE institution SET page_directories_enabled=? WHERE institution_id=?", enabled, institutionId);
	}

	private static ApiResponse directory(PageDirectoryTests.Fixture fixture, PageLocationId location) {
		return fixture.resource.publishedPages(Optional.of(location), Optional.of(fixture.prefix), Optional.empty(), Optional.empty());
	}

	private static List<UUID> pageIds(ApiResponse response) {
		List<PageApiResponse> pages = value(response, "pages");
		return pages.stream().map(PageApiResponse::getPageId).toList();
	}

	@SuppressWarnings("unchecked")
	private static <T> T value(ApiResponse response, String key) {
		return (T) ((Map<String, Object>) response.model().orElseThrow()).get(key);
	}

	private static CurrentContext context(PageDirectoryTests.Fixture fixture) {
		return new CurrentContext.Builder(fixture.account, Locale.US, ZoneId.of("America/New_York")).build();
	}

	private static void run(BiConsumer<PageDirectoryTests.Fixture, App> test) {
		IntegrationTestExecutor.runTransactionallyAndForceRollback(app -> {
			PageDirectoryTests.Fixture fixture = new PageDirectoryTests.Fixture(app);
			app.getInjector().getInstance(CurrentContextExecutor.class).execute(context(fixture), () -> test.accept(fixture, app));
		});
	}
}
