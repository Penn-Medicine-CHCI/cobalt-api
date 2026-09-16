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

@ThreadSafe
@Category(UnitTest.class)
public class CareEncounterFollowUpEmailTemplateTests {
	@Test
	public void rendersNavigatorContentInTheV2Layout() {
		Map<String, Object> context = baseContext();
		String customEmailText = "<p>Here are your <strong>next steps</strong>.</p>";
		context.put("customEmailText", customEmailText);
		context.put("patientFirstName", "Jordan <Lee>");
		context.put("appointmentDateDescription", "Sep 9, 2026");
		context.put("careNavigatorBookingUrl", "https://example.com/providers?featureId=RESOURCE_NAVIGATOR");
		context.put("integratedCarePhoneNumberFormatted", "(215) 555-0100");

		String subject = render("subject", context).trim();
		String body = render("body", context);

		Assert.assertEquals("Cobalt: Follow-up from your Care Navigator", subject);
		Assert.assertTrue(body.contains("<!DOCTYPE html"));
		Assert.assertTrue(body.contains(
				"width:600px; max-width:600px; background-color:#FFFFFF; border-radius:8px"));
		Assert.assertTrue(body.contains("src=\"https://example.com/cobalt-logo.png\""));
		Assert.assertTrue(body.contains("background-color:#F7F8F7"));
		Assert.assertTrue(body.contains("Jordan &lt;Lee&gt;"));
		Assert.assertFalse(body.contains("Jordan <Lee>"));
		Assert.assertTrue(body.contains(customEmailText));
		Assert.assertTrue(body.contains("href=\"https://example.com/providers?featureId=RESOURCE_NAVIGATOR\""));
		Assert.assertTrue(body.contains("Please do not reply to this email. This mailbox is not monitored."));
		Assert.assertTrue(body.contains("href=\"https://example.com/privacy\""));
		Assert.assertTrue(body.contains(
				"You are receiving this transactional email because you met with a Care Navigator through Cobalt."));
		Assert.assertFalse(body.contains("font-family:'Karla'"));
	}

	@Test
	public void rendersTheEscapedInstitutionFooterWhenConfigured() {
		Map<String, Object> context = baseContext();
		context.put("customEmailText", "<p>Your follow-up.</p>");
		context.put("emailFooterText", "<Institution & footer>");

		String body = render("body", context);

		Assert.assertTrue(body.contains("&lt;Institution &amp; footer&gt;"));
		Assert.assertFalse(body.contains("<Institution & footer>"));
		Assert.assertFalse(body.contains(
				"You are receiving this transactional email because you met with a Care Navigator through Cobalt."));
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
		context.put("platformEmailImageUrl", "https://example.com/cobalt-logo.png");
		context.put("privacyPolicyUrl", "https://example.com/privacy");
		return context;
	}

	@Nonnull
	protected String render(@Nonnull String templateChildName,
											 @Nonnull Map<String, Object> context) {
		HandlebarsTemplater handlebarsTemplater = new HandlebarsTemplater.Builder(Paths.get("messages/email"))
				.viewsDirectoryName("views")
				.shouldCacheTemplates(false)
				.build();

		return handlebarsTemplater.mergeTemplate(
				EmailMessageTemplate.CARE_ENCOUNTER_FOLLOW_UP.name(),
				templateChildName,
				Locale.US,
				context
		).orElseThrow();
	}
}
