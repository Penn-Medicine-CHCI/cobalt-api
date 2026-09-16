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
public class CareNavigatorAppointmentEmailTemplateTests {
	@Nonnull
	private static final String PATIENT_APPOINTMENT_URL = "https://cobalt.example/appointments/appointment-id";
	@Nonnull
	private static final String CANCEL_URL = "https://cobalt.example/my-calendar?appointmentId=appointment-id&action=cancel";
	@Nonnull
	private static final String STAFF_APPOINTMENT_URL = "https://admin.cobalt.example/scheduling/appointments/appointment-id";

	@Test
	public void rendersPatientLifecycleEmails() {
		Map<String, Object> context = baseContext();
		context.put("patientName", "Patient <One>");
		context.put("patientAppointmentUrl", PATIENT_APPOINTMENT_URL);
		context.put("cancelUrl", CANCEL_URL);
		context.put("appointmentCreatedPatientEmailBodyHtml", "<p>Bring your resource questions.</p>");

		String createdSubject = render(EmailMessageTemplate.V2_CARE_NAVIGATOR_APPOINTMENT_CREATED_PATIENT,
				"subject", context).trim();
		String createdBody = render(EmailMessageTemplate.V2_CARE_NAVIGATOR_APPOINTMENT_CREATED_PATIENT,
				"body", context);
		String reminderSubject = render(EmailMessageTemplate.V2_CARE_NAVIGATOR_APPOINTMENT_REMINDER_PATIENT,
				"subject", context).trim();
		String reminderBody = render(EmailMessageTemplate.V2_CARE_NAVIGATOR_APPOINTMENT_REMINDER_PATIENT,
				"body", context);
		String canceledSubject = render(EmailMessageTemplate.V2_CARE_NAVIGATOR_APPOINTMENT_CANCELED_PATIENT,
				"subject", context).trim();
		String canceledBody = render(EmailMessageTemplate.V2_CARE_NAVIGATOR_APPOINTMENT_CANCELED_PATIENT,
				"body", context);

		Assert.assertEquals("Cobalt: Care Navigator appointment confirmed", createdSubject);
		Assert.assertTrue(createdBody.contains("Patient &lt;One&gt;"));
		Assert.assertFalse(createdBody.contains("Patient <One>"));
		Assert.assertTrue(createdBody.contains("January 15, 2027"));
		Assert.assertTrue(createdBody.contains("10:30 AM"));
		Assert.assertTrue(createdBody.contains("href=\"" + PATIENT_APPOINTMENT_URL + "\""));
		Assert.assertTrue(createdBody.contains("href=\"" + CANCEL_URL + "\""));
		Assert.assertTrue(createdBody.contains("<p>Bring your resource questions.</p>"));
		assertNoReplyAndBranding(createdBody);

		Assert.assertEquals("Cobalt: Care Navigator appointment reminder", reminderSubject);
		Assert.assertTrue(reminderBody.contains("January 15, 2027"));
		Assert.assertTrue(reminderBody.contains("10:30 AM"));
		Assert.assertFalse(reminderBody.contains(PATIENT_APPOINTMENT_URL));
		assertNoReplyAndBranding(reminderBody);

		Assert.assertEquals("Cobalt: Care Navigator appointment canceled", canceledSubject);
		Assert.assertTrue(canceledBody.contains("Your Care Navigator appointment was canceled"));
		Assert.assertTrue(canceledBody.contains("January 15, 2027"));
		Assert.assertTrue(canceledBody.contains("10:30 AM"));
		assertNoReplyAndBranding(canceledBody);
	}

	@Test
	public void rendersNavigatorLifecycleEmails() {
		Map<String, Object> context = baseContext();
		context.put("careNavigatorName", "Navigator <One>");
		context.put("patientName", "Patient & One");
		context.put("patientEmailAddress", "patient@example.com");
		context.put("staffAppointmentUrl", STAFF_APPOINTMENT_URL);

		String createdSubject = render(EmailMessageTemplate.V2_CARE_NAVIGATOR_APPOINTMENT_CREATED_NAVIGATOR,
				"subject", context).trim();
		String createdBody = render(EmailMessageTemplate.V2_CARE_NAVIGATOR_APPOINTMENT_CREATED_NAVIGATOR,
				"body", context);
		String canceledSubject = render(EmailMessageTemplate.V2_CARE_NAVIGATOR_APPOINTMENT_CANCELED_NAVIGATOR,
				"subject", context).trim();
		String canceledBody = render(EmailMessageTemplate.V2_CARE_NAVIGATOR_APPOINTMENT_CANCELED_NAVIGATOR,
				"body", context);

		Assert.assertEquals("Cobalt: Care Navigator appointment booked for Friday, January 15, 2027 at 10:30 AM",
				createdSubject);
		Assert.assertTrue(createdBody.contains("Navigator &lt;One&gt;"));
		Assert.assertTrue(createdBody.contains("Patient &amp; One"));
		Assert.assertFalse(createdBody.contains("Patient & One"));
		Assert.assertTrue(createdBody.contains("patient@example.com"));
		Assert.assertTrue(createdBody.contains("href=\"" + STAFF_APPOINTMENT_URL + "\""));
		assertNoReplyAndBranding(createdBody);

		Assert.assertEquals("Cobalt: Care Navigator appointment canceled for Friday, January 15, 2027 at 10:30 AM",
				canceledSubject);
		Assert.assertTrue(canceledBody.contains("A Care Navigator appointment was canceled"));
		Assert.assertTrue(canceledBody.contains("Patient &amp; One"));
		Assert.assertFalse(canceledBody.contains(STAFF_APPOINTMENT_URL));
		assertNoReplyAndBranding(canceledBody);
	}

	protected void assertNoReplyAndBranding(@Nonnull String body) {
		Assert.assertTrue(body.contains("Please do not reply to this email. This mailbox is not monitored."));
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
	protected String render(@Nonnull EmailMessageTemplate messageTemplate,
											 @Nonnull String templateChildName,
											 @Nonnull Map<String, Object> context) {
		HandlebarsTemplater handlebarsTemplater = new HandlebarsTemplater.Builder(Paths.get("messages/email"))
				.viewsDirectoryName("views")
				.shouldCacheTemplates(false)
				.build();

		return handlebarsTemplater.mergeTemplate(
				messageTemplate.name(),
				templateChildName,
				Locale.US,
				context
		).orElseThrow();
	}
}
