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

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class DefaultGoogleBigQueryClient implements GoogleBigQueryClient {
	@Nonnull
	private final String projectId;
	@Nonnull
	private final String bigQueryResourceId;
	@Nonnull
	private final GoogleCredentials googleCredentials;
	@Nonnull
	private final BigQuery bigQuery;

	public DefaultGoogleBigQueryClient(@Nonnull String bigQueryResourceId,
																		 @Nonnull String serviceAccountPrivateKeyJson) {
		// ByteArrayInputStream does not need to be closed
		this(bigQueryResourceId, new ByteArrayInputStream(serviceAccountPrivateKeyJson.getBytes(StandardCharsets.UTF_8)));
	}

	public DefaultGoogleBigQueryClient(@Nonnull String bigQueryResourceId,
																		 @Nonnull InputStream serviceAccountPrivateKeyJsonInputStream) {
		requireNonNull(bigQueryResourceId);
		requireNonNull(serviceAccountPrivateKeyJsonInputStream);

		try {
			String serviceAccountPrivateKeyJson = CharStreams.toString(new InputStreamReader(requireNonNull(serviceAccountPrivateKeyJsonInputStream), StandardCharsets.UTF_8));

			// Confirm that this is well-formed JSON and extract the project ID
			Map<String, Object> jsonObject = new Gson().fromJson(serviceAccountPrivateKeyJson, new TypeToken<Map<String, Object>>() {
			}.getType());

			this.bigQueryResourceId = bigQueryResourceId;
			this.projectId = requireNonNull((String) jsonObject.get("project_id"));
			this.googleCredentials = acquireGoogleCredentials(serviceAccountPrivateKeyJson);
			this.bigQuery = createBigQuery(this.googleCredentials);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	@Override
	public String getProjectId() {
		return this.projectId;
	}

	@Nonnull
	@Override
	public String getBigQueryResourceId() {
		return this.bigQueryResourceId;
	}

	@Override
	@Nonnull
	public List<FieldValueList> queryForList(@Nonnull String sql) {
		requireNonNull(sql);

		// Special behavior: look for "{{datasetId}}" and replace it with the actual value
		// to make querying easier.
		sql = sql.replace("{{datasetId}}", getDatasetId());

		// See: https://cloud.google.com/bigquery/sql-reference/
		QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(sql)
				.setUseLegacySql(false)
				.build();

		// Create a job ID so that we can safely retry.
		JobId jobId = JobId.of(UUID.randomUUID().toString());
		Job queryJob = getBigQuery().create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

		// Wait for the query to complete.
		try {
			queryJob = queryJob.waitFor();
		} catch (InterruptedException e) {
			throw new RuntimeException("BigQuery job execution was interrupted", e);
		}

		// Check for errors
		if (queryJob == null) {
			throw new RuntimeException("Job no longer exists");
		} else if (queryJob.getStatus().getError() != null) {
			// You can also look at queryJob.getStatus().getExecutionErrors() for all
			// errors, not just the latest one.
			throw new RuntimeException(queryJob.getStatus().getError().toString());
		}

		try {
			// Get the results.
			TableResult result = queryJob.getQueryResults();
			List<FieldValueList> rows = new ArrayList<>();

			for (FieldValueList row : result.iterateAll())
				rows.add(row);

			return Collections.unmodifiableList(rows);
		} catch (InterruptedException e) {
			throw new RuntimeException("BigQuery results extraction was interrupted", e);
		}
	}

	@Nonnull
	protected GoogleCredentials acquireGoogleCredentials(@Nonnull String serviceAccountPrivateKeyJson) {
		requireNonNull(serviceAccountPrivateKeyJson);

		try (InputStream inputStream = new ByteArrayInputStream(serviceAccountPrivateKeyJson.getBytes(StandardCharsets.UTF_8))) {
			return ServiceAccountCredentials.fromStream(inputStream)
					.createScoped(Set.of("https://www.googleapis.com/auth/bigquery"));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	protected BigQuery createBigQuery(@Nonnull GoogleCredentials googleCredentials) {
		requireNonNull(googleCredentials);

		return BigQueryOptions.newBuilder()
				.setCredentials(googleCredentials)
				.build()
				.getService();
	}

	@Nonnull
	protected GoogleCredentials getGoogleCredentials() {
		return this.googleCredentials;
	}

	@Nonnull
	protected BigQuery getBigQuery() {
		return this.bigQuery;
	}
}
