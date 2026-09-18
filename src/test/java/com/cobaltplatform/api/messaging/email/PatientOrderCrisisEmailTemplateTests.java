/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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
import java.util.Locale;
import java.util.Map;

@ThreadSafe
@Category(UnitTest.class)
public class PatientOrderCrisisEmailTemplateTests {
	@Test
	public void rendersGenericSafetyReviewEmailWithInstitutionName() {
		Map<String, Object> context = Map.of(
				"colors", Map.of(
						"n50", "#F7F8F7",
						"n900", "#2D3030",
						"p500", "#2F7F61"
				),
				"institutionId", "EXAMPLE_HEALTH",
				"institutionName", "Example Health",
				"platformName", "Penn Cobalt",
				"platformEmailImageUrl", "https://example.com/cobalt-logo.png",
				"privacyPolicyUrl", "https://example.com/privacy",
				"patientOrderReferenceNumber", "12345",
				"staffPatientOrderUrl", "https://staff.example.com/ic/mhic/patient-orders/order-id"
		);

		String subject = render("subject", context).trim();
		String body = render("body", context);

		Assert.assertEquals("Penn Cobalt: Example Health safety review needed", subject);
		Assert.assertTrue(body.contains("Example Health safety review needed"));
		Assert.assertTrue(body.contains("review Example Health order <strong>12345</strong>"));
		Assert.assertTrue(body.contains("href=\"https://staff.example.com/ic/mhic/patient-orders/order-id\""));
		Assert.assertTrue(body.contains("background-color:#2F7F61"));
		Assert.assertTrue(body.contains("color:#FFFFFF"));
		Assert.assertTrue(body.contains("a safety-review contact for Example Health in Penn Cobalt"));
		Assert.assertFalse(subject.contains("EASE"));
		Assert.assertFalse(body.contains("EASE"));
		Assert.assertTrue(body.contains("Please do not reply to this email. This mailbox is not monitored."));
		Assert.assertTrue(body.contains("src=\"https://example.com/cobalt-logo.png\""));
		Assert.assertTrue(body.contains("href=\"https://example.com/privacy\""));
	}

	@Nonnull
	protected String render(@Nonnull String templateChildName,
												 @Nonnull Map<String, Object> context) {
		HandlebarsTemplater handlebarsTemplater = new HandlebarsTemplater.Builder(Paths.get("messages/email"))
				.viewsDirectoryName("views")
				.shouldCacheTemplates(false)
				.build();

		return handlebarsTemplater.mergeTemplate(
				EmailMessageTemplate.V2_PATIENT_ORDER_CRISIS.name(),
				templateChildName,
				Locale.US,
				context
		).orElseThrow();
	}
}
