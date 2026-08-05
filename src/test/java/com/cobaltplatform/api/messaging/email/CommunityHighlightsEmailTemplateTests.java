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

package com.cobaltplatform.api.messaging.email;

import com.cobaltplatform.api.UnitTest;
import com.cobaltplatform.api.util.HandlebarsTemplater;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
@Category(UnitTest.class)
public class CommunityHighlightsEmailTemplateTests {
	@Nonnull
	private static final String CUSTOM_CONTENT_HTML = "<table data-subscription-email-additional-content=\"true\"><tr><td>Custom content</td></tr></table>";
	@Nonnull
	private static final String SECTION_DIVIDER_STYLE = "border-top:1px solid #DBD7D3";

	@Test
	public void rendersAdditionalContentHtmlUnescapedBeforeSubscriptionFooter() {
		Map<String, Object> context = baseContext();
		context.put("subscriptionEmailAdditionalContentHtml", CUSTOM_CONTENT_HTML);

		String renderedEmail = render(context);
		int customContentIndex = renderedEmail.indexOf(CUSTOM_CONTENT_HTML);
		int subscriptionFooterIndex = renderedEmail.indexOf("because you subscribed to a page");

		Assert.assertTrue("Expected custom subscription-email content to render unescaped", customContentIndex >= 0);
		Assert.assertTrue("Expected custom content to render before the subscription footer",
				customContentIndex < subscriptionFooterIndex);
		Assert.assertEquals("Expected the custom content to have its own section divider",
				3, countOccurrences(renderedEmail, SECTION_DIVIDER_STYLE));
	}

	@Test
	public void omitsAdditionalContentWhenNotConfigured() {
		String renderedEmail = render(baseContext());

		Assert.assertFalse("Did not expect custom subscription-email content",
				renderedEmail.contains("data-subscription-email-additional-content"));
		Assert.assertEquals("Did not expect a divider for unconfigured custom content",
				2, countOccurrences(renderedEmail, SECTION_DIVIDER_STYLE));
	}

	@Nonnull
	protected Map<String, Object> baseContext() {
		Map<String, Object> context = new HashMap<>();
		context.put("colors", Map.of("n50", "#F7F7F7"));
		context.put("pageTitle", "Example Community");
		return context;
	}

	@Nonnull
	protected String render(@Nonnull Map<String, Object> context) {
		HandlebarsTemplater handlebarsTemplater = new HandlebarsTemplater.Builder(Paths.get("messages/email"))
				.viewsDirectoryName("views")
				.shouldCacheTemplates(false)
				.build();

		return handlebarsTemplater.mergeTemplate(
				EmailMessageTemplate.V2_COMMUNITY_HIGHLIGHTS.name(),
				"body",
				Locale.US,
				context
		).orElseThrow();
	}

	protected int countOccurrences(@Nonnull String value, @Nonnull String substring) {
		int occurrences = 0;
		int offset = 0;

		while ((offset = value.indexOf(substring, offset)) >= 0) {
			occurrences++;
			offset += substring.length();
		}

		return occurrences;
	}
}
