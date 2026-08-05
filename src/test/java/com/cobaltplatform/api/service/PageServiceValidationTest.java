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

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.UnitTest;
import com.cobaltplatform.api.model.db.PageRowColumn;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
@Category(UnitTest.class)
public class PageServiceValidationTest {
	@Test
	public void acceptsSafePageCallToActionUrls() {
		String[] safeUrls = {
				"https://example.com/path?query=value#fragment",
				"HTTP://localhost:4000/pages/example",
				"/pages/example",
				"pages/example",
				"./pages/example",
				"../pages/example",
				"?filter=example",
				"#details"
		};

		for (String safeUrl : safeUrls)
			Assert.assertTrue("Expected a safe CTA URL: " + safeUrl, PageService.isValidPageCallToActionUrl(safeUrl));
	}

	@Test
	public void rejectsUnsafePageCallToActionUrls() {
		String[] unsafeUrls = {
				null,
				"",
				"   ",
				"javascript:alert(document.domain)",
				"data:text/html,<script>alert(1)</script>",
				"vbscript:msgbox(1)",
				"mailto:person@example.com",
				"//example.com/path",
				"///example.com/path",
				"\\\\example.com/path",
				"https:example.com/path",
				"https://example.com/path\\@another.example",
				"https://example.com/line\nbreak"
		};

		for (String unsafeUrl : unsafeUrls)
			Assert.assertFalse("Expected an unsafe CTA URL: " + unsafeUrl, PageService.isValidPageCallToActionUrl(unsafeUrl));
	}

	@Test
	public void detectsQuillEmptyMarkup() {
		String[] emptyValues = {
				null,
				"",
				"   ",
				"<p><br></p>",
				"<p> &nbsp; </p>",
				"<div><strong>&#160;</strong></div>",
				"<p>&#xA0;</p>",
				"\u00A0",
				"<p>\u2003\u200B</p>"
		};

		for (String emptyValue : emptyValues)
			Assert.assertFalse("Expected empty page-builder text: " + emptyValue, PageService.hasMeaningfulPageBuilderText(emptyValue));

		Assert.assertTrue(PageService.hasMeaningfulPageBuilderText("<p>Meaningful content</p>"));
		Assert.assertTrue(PageService.hasMeaningfulPageBuilderText("A headline"));
	}

	@Test
	public void requiresHeadlineDescriptionAndUploadedImageForImageColumns() {
		PageRowColumn pageRowColumn = new PageRowColumn();
		pageRowColumn.setHeadline("Headline");
		pageRowColumn.setDescription("<p>Description</p>");
		pageRowColumn.setImageFileUploadId(UUID.randomUUID());
		pageRowColumn.setUsePlaceholderImage(false);

		Assert.assertTrue(PageService.isPublishableImagePageRowColumn(pageRowColumn));

		pageRowColumn.setHeadline("<p><br></p>");
		Assert.assertFalse(PageService.isPublishableImagePageRowColumn(pageRowColumn));
		pageRowColumn.setHeadline("Headline");

		pageRowColumn.setDescription(null);
		Assert.assertFalse(PageService.isPublishableImagePageRowColumn(pageRowColumn));
		pageRowColumn.setDescription("Description");

		pageRowColumn.setImageFileUploadId(null);
		Assert.assertFalse(PageService.isPublishableImagePageRowColumn(pageRowColumn));
		pageRowColumn.setImageFileUploadId(UUID.randomUUID());

		pageRowColumn.setUsePlaceholderImage(true);
		Assert.assertFalse(PageService.isPublishableImagePageRowColumn(pageRowColumn));
	}

	@Test
	public void preservesRowAnchorsOnlyForCopiesCreatedForEditing() {
		UUID sourcePageRowAnchorId = UUID.randomUUID();
		UUID newPageRowId = UUID.randomUUID();

		Assert.assertEquals(sourcePageRowAnchorId,
				PageService.pageRowAnchorIdForDuplicate(sourcePageRowAnchorId, newPageRowId, true));
		Assert.assertEquals(newPageRowId,
				PageService.pageRowAnchorIdForDuplicate(sourcePageRowAnchorId, newPageRowId, false));
	}
}
