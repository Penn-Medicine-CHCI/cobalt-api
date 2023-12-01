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

import com.google.analytics.data.v1beta.BetaAnalyticsDataClient;
import com.google.analytics.data.v1beta.BetaAnalyticsDataSettings;
import com.google.analytics.data.v1beta.RunReportRequest;
import com.google.analytics.data.v1beta.RunReportResponse;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Requires that the system account email address be added as a Viewer directly on the GA4 property.
 *
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class DefaultGoogleAnalyticsDataClient implements GoogleAnalyticsDataClient {
	@Nonnull
	private final String ga4PropertyId;
	@Nonnull
	private final String projectId;
	@Nonnull
	private final GoogleCredentials googleCredentials;
	@Nonnull
	private final BetaAnalyticsDataClient betaAnalyticsDataClient;

	public DefaultGoogleAnalyticsDataClient(@Nonnull String ga4PropertyId,
																					@Nonnull String serviceAccountPrivateKeyJson) {
		// ByteArrayInputStream does not need to be closed
		this(ga4PropertyId, new ByteArrayInputStream(serviceAccountPrivateKeyJson.getBytes(StandardCharsets.UTF_8)));
	}

	public DefaultGoogleAnalyticsDataClient(@Nonnull String ga4PropertyId,
																					@Nonnull InputStream serviceAccountPrivateKeyJsonInputStream) {
		requireNonNull(ga4PropertyId);
		requireNonNull(serviceAccountPrivateKeyJsonInputStream);

		try {
			String serviceAccountPrivateKeyJson = CharStreams.toString(new InputStreamReader(requireNonNull(serviceAccountPrivateKeyJsonInputStream), StandardCharsets.UTF_8));

			// Confirm that this is well-formed JSON and extract the project ID
			Map<String, Object> jsonObject = new Gson().fromJson(serviceAccountPrivateKeyJson, new TypeToken<Map<String, Object>>() {
			}.getType());

			this.ga4PropertyId = ga4PropertyId;
			this.projectId = requireNonNull((String) jsonObject.get("project_id"));
			this.googleCredentials = acquireGoogleCredentials(serviceAccountPrivateKeyJson);
			this.betaAnalyticsDataClient = createBetaAnalyticsDataClient(this.googleCredentials);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	@Nonnull
	public String getGa4PropertyId() {
		return this.ga4PropertyId;
	}

	@Override
	@Nonnull
	public String getProjectId() {
		return this.projectId;
	}

	@Nonnull
	@Override
	public RunReportResponse runReport(@Nonnull RunReportRequest runReportRequest) {
		requireNonNull(runReportRequest);

		// See https://developers.google.com/analytics/devguides/reporting/data/v1/api-schema
		return getBetaAnalyticsDataClient().runReport(runReportRequest);
	}

	@Nonnull
	protected GoogleCredentials acquireGoogleCredentials(@Nonnull String serviceAccountPrivateKeyJson) {
		requireNonNull(serviceAccountPrivateKeyJson);

		try (InputStream inputStream = new ByteArrayInputStream(serviceAccountPrivateKeyJson.getBytes(StandardCharsets.UTF_8))) {
			return ServiceAccountCredentials.fromStream(inputStream);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	protected BetaAnalyticsDataClient createBetaAnalyticsDataClient(@Nonnull GoogleCredentials googleCredentials) {
		requireNonNull(googleCredentials);

		try {
			return BetaAnalyticsDataClient.create(BetaAnalyticsDataSettings.newBuilder()
					.setCredentialsProvider(FixedCredentialsProvider.create(googleCredentials))
					.build());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	protected GoogleCredentials getGoogleCredentials() {
		return this.googleCredentials;
	}

	@Nonnull
	protected BetaAnalyticsDataClient getBetaAnalyticsDataClient() {
		return this.betaAnalyticsDataClient;
	}
}
