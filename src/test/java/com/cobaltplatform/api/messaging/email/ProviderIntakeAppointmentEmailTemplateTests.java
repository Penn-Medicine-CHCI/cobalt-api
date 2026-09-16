/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
@Category(UnitTest.class)
public class ProviderIntakeAppointmentEmailTemplateTests {
	@Test
	public void rendersAppointmentContactAndScreeningResponses() {
		Map<String, Object> context = baseContext();
		context.put("providerName", "CuraLinc <Intake>");
		context.put("patientName", "Patient <One>");
		context.put("patientEmailAddress", "patient+booking@example.com");
		context.put("patientPhoneNumber", "(215) 555-1212");
		context.put("providerSchedulingUrl", "https://cobalt.example/scheduling/appointments/appointment-id");
		context.put("intakeResponses", List.of(
				Map.of(
						"question", "Would it be OK to leave a voicemail?",
						"answer", "Yes & please call after 5"
				),
				Map.of(
						"question", "Are you a University employee?",
						"answer", "Yes"
				)
		));

		String subject = render("subject", context).trim();
		String body = render("body", context);

		Assert.assertEquals("Cobalt: New telephone intake appointment for January 15, 2027 at 10:30 AM", subject);
		Assert.assertTrue(body.contains("CuraLinc &lt;Intake&gt;"));
		Assert.assertTrue(body.contains("Patient &lt;One&gt;"));
		Assert.assertFalse(body.contains("Patient <One>"));
		Assert.assertTrue(body.contains("patient+booking@example.com"));
		Assert.assertTrue(body.contains("(215) 555-1212"));
		Assert.assertTrue(body.contains("Would it be OK to leave a voicemail?"));
		Assert.assertTrue(body.contains("Yes &amp; please call after 5"));
		Assert.assertTrue(body.contains("href=\"https://cobalt.example/scheduling/appointments/appointment-id\""));
		Assert.assertTrue(body.contains("src=\"https://example.com/cobalt-logo.png\""));
		Assert.assertTrue(body.contains("href=\"https://example.com/privacy\""));
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
		context.put("appointmentStartDateDescription", "January 15, 2027");
		context.put("appointmentStartTimeDescription", "10:30 AM");
		context.put("appointmentStartDateTimeDescription", "Friday, January 15, 2027 at 10:30 AM");
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
				EmailMessageTemplate.V2_PROVIDER_INTAKE_APPOINTMENT_CREATED_PROVIDER.name(),
				templateChildName,
				Locale.US,
				context
		).orElseThrow();
	}
}
