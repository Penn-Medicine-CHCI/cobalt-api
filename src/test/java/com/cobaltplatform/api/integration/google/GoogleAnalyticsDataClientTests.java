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

import com.google.analytics.data.v1beta.DateRange;
import com.google.analytics.data.v1beta.Dimension;
import com.google.analytics.data.v1beta.Metric;
import com.google.analytics.data.v1beta.Row;
import com.google.analytics.data.v1beta.RunReportRequest;
import com.google.analytics.data.v1beta.RunReportResponse;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.soklet.util.LoggingUtils.initializeLogback;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class GoogleAnalyticsDataClientTests {
	@BeforeClass
	public static void initialize() {
		initializeLogback(Paths.get("config/local/logback.xml"));
	}

	@Test
	public void testGoogleAnalyticsDataClient() throws Exception {
		String ga4PropertyId = Files.readString(
				Path.of("resources/test/google-ga4-property-id"), StandardCharsets.UTF_8);
		String reportingServiceAccountPrivateKeyJson = Files.readString(
				Path.of("resources/test/reporting-service-account-private-key.json"), StandardCharsets.UTF_8);

		GoogleAnalyticsDataClient googleAnalyticsDataClient = new DefaultGoogleAnalyticsDataClient(ga4PropertyId, reportingServiceAccountPrivateKeyJson);

		RunReportRequest request =
				RunReportRequest.newBuilder()
						.setProperty("properties/" + ga4PropertyId)
						.addDimensions(Dimension.newBuilder().setName("city"))
						.addMetrics(Metric.newBuilder().setName("activeUsers"))
						.addDateRanges(DateRange.newBuilder().setStartDate("2020-03-31").setEndDate("today"))
						.build();

		RunReportResponse response = googleAnalyticsDataClient.runReport(request);

		for (Row row : response.getRowsList()) {
			String city = row.getDimensionValues(0).getValue();
			Long count = Long.valueOf(row.getMetricValues(0).getValue());
			System.out.println(city + ": " + count);
		}
	}
}

