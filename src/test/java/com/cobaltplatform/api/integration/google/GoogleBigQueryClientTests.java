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

package com.cobaltplatform.api.integration.google;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.soklet.util.LoggingUtils.initializeLogback;
import static java.lang.String.format;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class GoogleBigQueryClientTests {
	@BeforeClass
	public static void initialize() {
		initializeLogback(Paths.get("config/local/logback.xml"));
	}

	@Test
	public void testLargeResultset() {
		GoogleBigQueryClient googleBigQueryClient = acquireGoogleBigQueryClient();

		// 20230613 is epoch
		List<FieldValueList> rows = googleBigQueryClient.queryForList(format("""
						SELECT DISTINCT user_id
						FROM `{{datasetId}}.events_*`
						WHERE _TABLE_SUFFIX BETWEEN '%s' AND '%s'
						""",
				googleBigQueryClient.dateAsTableSuffix(LocalDate.of(2023, 6, 13)),
				googleBigQueryClient.dateAsTableSuffix(LocalDate.of(2023, 9, 1))));

		Set<UUID> accountIds = new HashSet<>();

		for (FieldValueList row : rows) {
			FieldValue fieldValue = row.get(0);

			if (fieldValue.isNull())
				continue;

			accountIds.add(UUID.fromString(fieldValue.getStringValue()));
		}

		// The "+ 1" is to take the null value into account
		Assert.assertTrue("Account IDs don't match row count", accountIds.size() + 1 == rows.size());
		Assert.assertTrue("No account IDs found", accountIds.size() > 0);
	}

	@Test
	public void testGoogleBigQuery() {
		GoogleBigQueryClient googleBigQueryClient = acquireGoogleBigQueryClient();

		List<FieldValueList> rows = googleBigQueryClient.queryForList("""
				SELECT event_date, event_timestamp, event_name, event_params
				FROM {{datasetId}}.events_20230913
				WHERE event_name='Top Nav Dropdown'
				ORDER BY event_timestamp
				LIMIT 100
				""");

		// ** Event names
		//
		// first_visit
		// user_engagement
		// form_start
		// In Crisis Button
		// video_complete
		// scroll
		// video_start
		// Top Nav Dropdown
		// click
		// session_start
		// Top Nav
		// Sign in
		// video_progress
		// page_view
		// User Clicked Crisis Link in Header
		// HP Nav

		for (FieldValueList row : rows) {
			String eventName = row.get("event_name").getStringValue();

			if (eventName.equals("Top Nav Dropdown")) {
				FieldValue eventParams = row.get("event_params");
				FieldValueList eventParamsValueRecords = (FieldValueList) eventParams.getValue();

				for (FieldValue eventParamsValueRecord : eventParamsValueRecords) {
					FieldValueList recordValue = eventParamsValueRecord.getRecordValue();

					FieldValue recordKeyValue = recordValue.get(0);
					String recordKey = recordKeyValue.getStringValue();

					if (recordKey.equals("link_text")) {
						FieldValue linkTextValue = recordValue.get(1);
						String linkText = linkTextValue.getRecordValue().get(0).getStringValue();
						Assert.assertNotNull("Missing link_text", linkText);
					} else if (recordKey.equals("page_location")) {
						FieldValue pageLocationValue = recordValue.get(1);
						String pageLocation = pageLocationValue.getRecordValue().get(0).getStringValue();
						Assert.assertNotNull("Missing page_location", pageLocation);
					}
				}
			}
		}
	}

	@Nonnull
	protected GoogleBigQueryClient acquireGoogleBigQueryClient() {
		try {
			String bigQueryResourceId = Files.readString(
					Path.of("resources/test/bigquery-resource-id"), StandardCharsets.UTF_8);

			String reportingServiceAccountPrivateKeyJson = Files.readString(
					Path.of("resources/test/reporting-service-account-private-key.json"), StandardCharsets.UTF_8);

			return new DefaultGoogleBigQueryClient(bigQueryResourceId, reportingServiceAccountPrivateKeyJson);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

