package com.cobaltplatform.api.service;

import com.cobaltplatform.api.UnitTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.owasp.html.HtmlPolicyBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Category(UnitTest.class)
public class CareEncounterFollowUpLinkRewriterTests {
	@Test
	public void rewritesSanitizedResourceLinksAndPreservesTheirDestinations() {
		String sanitized = new HtmlPolicyBuilder()
				.allowElements("p", "a")
				.allowUrlProtocols("https")
				.allowAttributes("href", "rel", "target").onElements("a")
				.requireRelNofollowOnLinks()
				.toFactory()
				.sanitize("<p><a href=\"https://example.org/resource?a=1&b=2\">First</a> "
						+ "<a href=\"https://example.net/next\">Second</a></p>");
		List<String> destinations = new ArrayList<>();
		UUID firstId = UUID.fromString("eec412f6-0d8f-40f1-9375-0f1e88aaa111");
		UUID secondId = UUID.fromString("eec412f6-0d8f-40f1-9375-0f1e88aaa222");

		String rewritten = CareEncounterFollowUpLinkRewriter.rewrite(sanitized,
				"https://penncobalt.com/", destination -> {
					destinations.add(destination);
					return destinations.size() == 1 ? firstId : secondId;
				});

		Assert.assertEquals(List.of("https://example.org/resource?a=1&b=2", "https://example.net/next"), destinations);
		Assert.assertTrue(rewritten.contains("href=\"https://penncobalt.com/care-encounter-links/" + firstId + "/redirect\""));
		Assert.assertTrue(rewritten.contains("href=\"https://penncobalt.com/care-encounter-links/" + secondId + "/redirect\""));
		Assert.assertTrue(rewritten.contains("First</a>"));
		Assert.assertTrue(rewritten.contains("Second</a>"));
	}
}
