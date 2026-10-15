package com.cobaltplatform.api.service;

import com.cobaltplatform.api.UnitTest;
import com.cobaltplatform.api.model.api.request.CreateAnalyticsNativeEventRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageSettingsRequest;
import com.cobaltplatform.api.model.db.AnalyticsNativeEventType.AnalyticsNativeEventTypeId;
import com.cobaltplatform.api.model.db.PageLocation.PageLocationId;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.cobaltplatform.api.web.request.RequestBodyParsingException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

@Category(UnitTest.class)
public class PageDirectoryRequestTests {
	private final RequestBodyParser parser = new RequestBodyParser(new JsonMapper());

	@Test
	public void locationParsingDistinguishesSupportedIdsFromOmittedAndNullValues() {
		for (PageLocationId location : PageLocationId.values()) {
			String body = "{\"pageLocationId\":\"" + location.name() + "\"}";
			assertEquals(location, parser.parse(body, CreatePageRequest.class).getPageLocationId());
			assertEquals(location, parser.parse(body, UpdatePageSettingsRequest.class).getPageLocationId());
		}
		for (String body : new String[]{"{}", "{\"pageLocationId\":null}"}) {
			assertNull(parser.parse(body, CreatePageRequest.class).getPageLocationId());
			assertNull(parser.parse(body, UpdatePageSettingsRequest.class).getPageLocationId());
		}
	}

	@Test
	public void unsupportedLocationsAreRejectedRatherThanSilentlyDropped() {
		for (String value : new String[]{"\"INVALID\"", "\"community\"", "\"\"", "1", "true", "[]", "{}"}) {
			String body = "{\"pageLocationId\":" + value + "}";
			assertThrows(RequestBodyParsingException.class, () -> parser.parse(body, CreatePageRequest.class));
			assertThrows(RequestBodyParsingException.class, () -> parser.parse(body, UpdatePageSettingsRequest.class));
		}
	}

	@Test
	public void directoryAnalyticsRequestsPreserveEventTypesAndDirectoryContext() {
		for (String[] event : new String[][]{
				{"PAGE_VIEW_FEATURED_TOPICS", "FEATURED_TOPIC"},
				{"PAGE_VIEW_COMMUNITIES", "COMMUNITY"}}) {
			CreateAnalyticsNativeEventRequest request = parser.parse("""
					{"analyticsNativeEventTypeId":"%s","data":{
					"pageLocationId":"%s","searchQuery":"anxiety","pageNumber":1,"pageSize":25,"totalCount":30}}
					""".formatted(event[0], event[1]), CreateAnalyticsNativeEventRequest.class);
			assertEquals(AnalyticsNativeEventTypeId.valueOf(event[0]), request.getAnalyticsNativeEventTypeId());
			assertEquals(event[1], request.getData().get("pageLocationId"));
			assertEquals("anxiety", request.getData().get("searchQuery"));
			assertEquals(1, ((Number) request.getData().get("pageNumber")).intValue());
			assertEquals(25, ((Number) request.getData().get("pageSize")).intValue());
			assertEquals(30, ((Number) request.getData().get("totalCount")).intValue());
		}
		CreateAnalyticsNativeEventRequest navigation = parser.parse("""
				{"analyticsNativeEventTypeId":"CLICKTHROUGH_PAGE_DIRECTORY","data":{"pageLocationId":"COMMUNITY","source":"NAV"}}
				""", CreateAnalyticsNativeEventRequest.class);
		assertEquals(AnalyticsNativeEventTypeId.CLICKTHROUGH_PAGE_DIRECTORY, navigation.getAnalyticsNativeEventTypeId());
		assertEquals("COMMUNITY", navigation.getData().get("pageLocationId"));
		assertEquals("NAV", navigation.getData().get("source"));

		CreateAnalyticsNativeEventRequest card = parser.parse("""
				{"analyticsNativeEventTypeId":"CLICKTHROUGH_PAGE","data":{
				"pageId":"00000000-0000-0000-0000-000000000001","source":"DIRECTORY",
				"pageLocationId":"FEATURED_TOPIC","siteLocationId":"FEATURED_TOPIC","pageNumber":0,"pageSize":25}}
				""", CreateAnalyticsNativeEventRequest.class);
		assertEquals(AnalyticsNativeEventTypeId.CLICKTHROUGH_PAGE, card.getAnalyticsNativeEventTypeId());
		assertEquals("DIRECTORY", card.getData().get("source"));
		assertEquals("FEATURED_TOPIC", card.getData().get("pageLocationId"));
		assertEquals("FEATURED_TOPIC", card.getData().get("siteLocationId"));
		assertFalse(card.getData().containsKey("searchQuery"));
	}
}
