package com.cobaltplatform.api.service;

import com.cobaltplatform.api.UnitTest;
import com.cobaltplatform.api.model.api.request.CreatePageRequest;
import com.cobaltplatform.api.model.api.request.UpdatePageSettingsRequest;
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
}
