package com.cobaltplatform.api.service;

import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.model.api.request.CreateAnalyticsNativeEventRequest;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.pyranid.Database;
import org.junit.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class PageDirectoryAnalyticsTests {
	@Test
	public void directoryViewsAndClickthroughsPersistTheirEventTypesAndContext() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback(app -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AnalyticsService analytics = app.getInjector().getInstance(AnalyticsService.class);
			JsonMapper mapper = app.getInjector().getInstance(JsonMapper.class);
			RequestBodyParser parser = new RequestBodyParser(mapper);
			UUID clientDeviceId = UUID.randomUUID();
			UUID sessionId = UUID.randomUUID();
			database.execute("""
					INSERT INTO client_device (client_device_id,client_device_type_id,fingerprint,last_updated)
					VALUES (?,'WEB_BROWSER',?,NOW())
					""", clientDeviceId, UUID.randomUUID());

			for (String[] event : new String[][]{
					{"PAGE_VIEW_FEATURED_TOPICS", "/featured-topics?searchQuery=anxiety&pageNumber=1", """
							{"pageLocationId":"FEATURED_TOPIC","searchQuery":"anxiety","pageNumber":1,"pageSize":25,"totalCount":30}
							"""},
					{"PAGE_VIEW_COMMUNITIES", "/communities", """
							{"pageLocationId":"COMMUNITY","pageNumber":0,"pageSize":25,"totalCount":0}
							"""},
					{"CLICKTHROUGH_PAGE_DIRECTORY", "/", """
							{"pageLocationId":"COMMUNITY","source":"NAV"}
							"""},
					{"CLICKTHROUGH_PAGE", "/featured-topics?searchQuery=anxiety&pageNumber=1", """
							{"pageId":"00000000-0000-0000-0000-000000000001","source":"DIRECTORY",
							"pageLocationId":"FEATURED_TOPIC","siteLocationId":"FEATURED_TOPIC",
							"searchQuery":"anxiety","pageNumber":1,"pageSize":25}
							"""}}) {
				CreateAnalyticsNativeEventRequest request = parser.parse("""
						{"analyticsNativeEventTypeId":"%s","data":%s}
						""".formatted(event[0], event[2]), CreateAnalyticsNativeEventRequest.class);
				request.setInstitutionId(InstitutionId.COBALT);
				request.setClientDeviceId(clientDeviceId);
				request.setSessionId(sessionId);
				request.setTimestamp(Instant.now());
				request.setAppName("Cobalt Website");
				request.setAppVersion("page-directory-analytics-test");
				request.setWebappUrl("https://example.com" + event[1]);

				UUID eventId = analytics.createAnalyticsNativeEvent(request);
				assertEquals(event[0], database.queryForObject("SELECT analytics_native_event_type_id FROM analytics_native_event WHERE analytics_native_event_id=?", String.class, eventId).orElseThrow());
				assertEquals(event[1], database.queryForObject("SELECT webapp_url FROM analytics_native_event WHERE analytics_native_event_id=?", String.class, eventId).orElseThrow());
				String savedData = database.queryForObject("SELECT data::TEXT FROM analytics_native_event WHERE analytics_native_event_id=?", String.class, eventId).orElseThrow();
				assertEquals(request.getData(), mapper.toMapFromRawJson(savedData));
			}
			assertEquals(Integer.valueOf(4), database.queryForObject("SELECT COUNT(*) FROM analytics_native_event WHERE session_id=? AND client_device_id=?", Integer.class, sessionId, clientDeviceId).orElseThrow());
		});
	}
}
