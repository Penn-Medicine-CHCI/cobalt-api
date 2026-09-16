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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
@Category(UnitTest.class)
public class V2EmailLayoutContractTests {
	@Nonnull
	private static final String PLATFORM_EMAIL_IMAGE_URL = "https://example.com/platform-logo.png";

	@Test
	public void everyV2EmailRendersTheSharedBrandedShell() {
		Arrays.stream(EmailMessageTemplate.values())
				.filter(messageTemplate -> messageTemplate.name().startsWith("V2_"))
				.forEach(messageTemplate -> {
					String body = render(messageTemplate, baseContext());

					Assert.assertTrue(messageTemplate + " should render an HTML document",
							body.contains("<!DOCTYPE html"));
					Assert.assertTrue(messageTemplate + " should render the institution logo",
							body.contains("src=\"" + PLATFORM_EMAIL_IMAGE_URL + "\""));
					Assert.assertTrue(messageTemplate + " should render the responsive card",
							body.contains("width:600px; max-width:600px; background-color:#FFFFFF; border-radius:8px"));
					Assert.assertTrue(messageTemplate + " should render institution colors",
							body.contains("background-color:#F7F8F7"));
					Assert.assertTrue(messageTemplate + " should render the privacy link",
							body.contains("href=\"https://example.com/privacy\""));
					Assert.assertFalse(messageTemplate + " should not render empty color declarations",
							body.contains("background-color:;"));
				});
	}

	@Test
	public void everyV2EmailUsesAnEscapedInstitutionFooterWhenConfigured() {
		Arrays.stream(EmailMessageTemplate.values())
				.filter(messageTemplate -> messageTemplate.name().startsWith("V2_"))
				.forEach(messageTemplate -> {
					Map<String, Object> context = baseContext();
					context.put("emailFooterText", "<Institution & footer>");
					String body = render(messageTemplate, context);

					Assert.assertTrue(messageTemplate + " should render escaped institution footer text",
							body.contains("&lt;Institution &amp; footer&gt;"));
					Assert.assertFalse(messageTemplate + " should not render raw institution footer HTML",
							body.contains("<Institution & footer>"));
				});
	}

	@Nonnull
	protected Map<String, Object> baseContext() {
		Map<String, Object> context = new HashMap<>();
		context.put("colors", Map.of(
				"n50", "#F7F8F7",
				"n900", "#2D3030",
				"p500", "#2F7F61"
		));
		context.put("institutionId", "COBALT");
		context.put("platformName", "Cobalt");
		context.put("platformEmailImageUrl", PLATFORM_EMAIL_IMAGE_URL);
		context.put("privacyPolicyUrl", "https://example.com/privacy");
		return context;
	}

	@Nonnull
	protected String render(@Nonnull EmailMessageTemplate messageTemplate,
											 @Nonnull Map<String, Object> context) {
		HandlebarsTemplater handlebarsTemplater = new HandlebarsTemplater.Builder(Paths.get("messages/email"))
				.viewsDirectoryName("views")
				.shouldCacheTemplates(false)
				.build();

		return handlebarsTemplater.mergeTemplate(
				messageTemplate.name(),
				"body",
				Locale.US,
				context
		).orElseThrow();
	}
}
