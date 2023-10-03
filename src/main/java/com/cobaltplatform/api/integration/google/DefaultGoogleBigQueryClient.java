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

import com.cobaltplatform.api.http.DefaultHttpClient;
import com.cobaltplatform.api.http.HttpClient;
import com.cobaltplatform.api.http.HttpMethod;
import com.cobaltplatform.api.http.HttpRequest;
import com.cobaltplatform.api.http.HttpResponse;
import com.google.auth.oauth2.AccessToken;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
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

		// See
		// https://developers.google.com/analytics/bigquery/basic-queries
		// https://cloud.google.com/bigquery/docs/querying-wildcard-tables

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

	public List<GoogleBigQueryExportRecord> test(@Nonnull String sql) {
		try {
			String json = Files.readString(Path.of("resources/test/bigquery-single-scroll-api-response.json"), StandardCharsets.UTF_8);

			GoogleBigQueryRestApiQueryResponse response = GoogleBigQueryRestApiQueryResponse.fromJson(json);

			Map<String, Integer> fieldIndicesByName = new HashMap<>(response.getSchema().getFields().size());

			for (int i = 0; i < response.getSchema().getFields().size(); ++i) {
				GoogleBigQueryRestApiQueryResponse.TableSchema.TableFieldSchema field = response.getSchema().getFields().get(i);
				fieldIndicesByName.put(field.getName(), i);
			}

			System.out.println("Total rows: " + response.getTotalRows());
			System.out.println("Page rows: " + response.getRows().size());
			System.out.println("Page token: " + response.getPageToken());

			// TODO: pull out as a static
			DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US);

			// TODO: implement paging to combine all pages into a single `rows` array
			List<GoogleBigQueryRestApiQueryResponse.Row> rows = new ArrayList<>(response.getRows());
			List<GoogleBigQueryExportRecord> exportRecords = new ArrayList<>(rows.size());

			// Pull relevant data from the response.
			// Schema is documented at https://support.google.com/analytics/answer/7029846?hl=en
			for (GoogleBigQueryRestApiQueryResponse.Row row : rows) {
				// *** Start Event ***
				GoogleBigQueryExportRecord.Event event = new GoogleBigQueryExportRecord.Event();

				// Event date
				GoogleBigQueryRestApiQueryResponse.Row.RowField eventDateField = row.getFields().get(fieldIndicesByName.get("event_date"));
				event.setDate(LocalDate.parse(eventDateField.getValue(), dateFormatter));

				// Event timestamp
				GoogleBigQueryRestApiQueryResponse.Row.RowField eventTimestampField = row.getFields().get(fieldIndicesByName.get("event_timestamp"));
				long eventTimestampFieldValueAsMicroseconds = Long.valueOf(eventTimestampField.getValue());
				Instant eventTimestamp = Instant.ofEpochSecond(
						TimeUnit.MICROSECONDS.toSeconds(eventTimestampFieldValueAsMicroseconds),
						TimeUnit.MICROSECONDS.toNanos(eventTimestampFieldValueAsMicroseconds % TimeUnit.SECONDS.toMicros(1)));
				event.setTimestamp(eventTimestamp);

				// Event previous timestamp
				GoogleBigQueryRestApiQueryResponse.Row.RowField eventPreviousTimestampField = row.getFields().get(fieldIndicesByName.get("event_previous_timestamp"));

				if (eventPreviousTimestampField.getValue() != null) {
					long eventPreviousTimestampFieldValueAsMicroseconds = Long.valueOf(eventPreviousTimestampField.getValue());
					Instant eventPreviousTimestamp = Instant.ofEpochSecond(
							TimeUnit.MICROSECONDS.toSeconds(eventPreviousTimestampFieldValueAsMicroseconds),
							TimeUnit.MICROSECONDS.toNanos(eventPreviousTimestampFieldValueAsMicroseconds % TimeUnit.SECONDS.toMicros(1)));
					event.setPreviousTimestamp(eventPreviousTimestamp);
				}

				// Event name
				GoogleBigQueryRestApiQueryResponse.Row.RowField eventNameField = row.getFields().get(fieldIndicesByName.get("event_name"));
				event.setName(eventNameField.getValue());

				// Event value in USD
				GoogleBigQueryRestApiQueryResponse.Row.RowField eventValueInUsdField = row.getFields().get(fieldIndicesByName.get("event_value_in_usd"));
				event.setValueInUsd(eventValueInUsdField.getValue() == null ? null : Double.valueOf(eventValueInUsdField.getValue()));

				// Event bundle sequence ID
				GoogleBigQueryRestApiQueryResponse.Row.RowField eventBundleSequenceIdField = row.getFields().get(fieldIndicesByName.get("event_bundle_sequence_id"));
				event.setBundleSequenceId(eventBundleSequenceIdField.getValue() == null ? null : Long.valueOf(eventBundleSequenceIdField.getValue()));

				// Event bundle sequence ID
				GoogleBigQueryRestApiQueryResponse.Row.RowField eventServerTimestampOffsetField = row.getFields().get(fieldIndicesByName.get("event_server_timestamp_offset"));
				event.setServerTimestampOffset(eventServerTimestampOffsetField.getValue() == null ? null : Long.valueOf(eventServerTimestampOffsetField.getValue()));

				// Event Params
				GoogleBigQueryRestApiQueryResponse.Row.RowField eventParamsField = row.getFields().get(fieldIndicesByName.get("event_params"));
				List<GoogleBigQueryRestApiQueryResponse.Row.RowField> eventParamsFields = eventParamsField.getFields();

				Map<String, GoogleBigQueryExportRecord.Event.EventParamValue> eventParameters = new HashMap<>();

				// Format is, for each field in the list there is another field object.  In that field ovject, there is a list of fields.
				// In the list of fields -
				// * the first element has a value with the name of the event
				// * the second element has a field object with a list of fields contained in it.  The first non-null value is the one to take
				//
				// Example:
				// [{
				//  "field": {
				//    "fields": [
				//      {
				//        "value": "link_detail"
				//      },
				//      {
				//        "field": {
				//          "fields": [
				//            {
				//              "value": "Wellness Coaching"
				//            },
				//            {},
				//            {},
				//            {}
				//          ]
				//        }
				//      }
				//    ]
				//  }
				// }

				for (GoogleBigQueryRestApiQueryResponse.Row.RowField field : eventParamsFields) {
					GoogleBigQueryRestApiQueryResponse.Row.RowField fieldLevel1 = field.getField();

					if (fieldLevel1 != null) {
						List<GoogleBigQueryRestApiQueryResponse.Row.RowField> fieldsLevel2 = fieldLevel1.getFields();

						if (fieldsLevel2 != null) {
							if (fieldsLevel2.size() != 2)
								throw new IllegalStateException("Not sure how to handle event params field " + field);

							// Name should just have a value
							GoogleBigQueryRestApiQueryResponse.Row.RowField fieldLevel3ParameterName = fieldsLevel2.get(0);
							String parameterName = fieldLevel3ParameterName.getValue();

							GoogleBigQueryRestApiQueryResponse.Row.RowField fieldLevel3ParameterValueLevel1 = fieldsLevel2.get(1);

							if (fieldLevel3ParameterValueLevel1 == null)
								throw new IllegalStateException("Not sure how to handle event params field " + field);

							// Values need to be picked out of a list
							GoogleBigQueryRestApiQueryResponse.Row.RowField fieldLevel3ParameterValueLevel2 = fieldLevel3ParameterValueLevel1.getField();

							if (fieldLevel3ParameterValueLevel2 == null
									|| fieldLevel3ParameterValueLevel2.getFields() == null
									|| fieldLevel3ParameterValueLevel2.getFields().size() != 4) // Event params always have STRING, INTEGER, FLOAT, DOUBLE options
								throw new IllegalStateException("Not sure how to handle event params field " + field);

							// Event params always have STRING, INTEGER, FLOAT, DOUBLE options
							GoogleBigQueryRestApiQueryResponse.Row.RowField stringRowField = fieldLevel3ParameterValueLevel2.getFields().get(0);
							String stringValue = stringRowField.getValue();
							GoogleBigQueryExportRecord.Event.EventParamValue eventParamValue = new GoogleBigQueryExportRecord.Event.EventParamValue();

							if (stringRowField.getValue() != null) {
								eventParamValue.setValue(stringValue);
								eventParamValue.setType(GoogleBigQueryExportRecord.Event.EventParamValueType.STRING);
							} else {
								// Google's INTEGER definition can be larger than Java's Integer.MAX_VALUE, so we use Long instead
								GoogleBigQueryRestApiQueryResponse.Row.RowField longRowField = fieldLevel3ParameterValueLevel2.getFields().get(1);
								Long longValue = longRowField.getValue() == null ? null : Long.valueOf(longRowField.getValue());

								if (longValue != null) {
									eventParamValue.setValue(longValue);
									eventParamValue.setType(GoogleBigQueryExportRecord.Event.EventParamValueType.INTEGER);
								} else {
									GoogleBigQueryRestApiQueryResponse.Row.RowField floatRowField = fieldLevel3ParameterValueLevel2.getFields().get(2);
									Float floatValue = floatRowField.getValue() == null ? null : Float.valueOf(floatRowField.getValue());

									if (floatValue != null) {
										eventParamValue.setValue(floatValue);
										eventParamValue.setType(GoogleBigQueryExportRecord.Event.EventParamValueType.FLOAT);
									} else {
										GoogleBigQueryRestApiQueryResponse.Row.RowField doubleRowField = fieldLevel3ParameterValueLevel2.getFields().get(3);
										Double doubleValue = doubleRowField.getValue() == null ? null : Double.valueOf(doubleRowField.getValue());

										if (doubleValue != null) {
											eventParamValue.setValue(doubleValue);
											eventParamValue.setType(GoogleBigQueryExportRecord.Event.EventParamValueType.DOUBLE);
										}
									}
								}
							}

							eventParameters.put(parameterName, eventParamValue);
						}
					}
				}

				event.setParameters(eventParameters);

				// *** End Event ***

				// *** Start User ***

				// User ID
				GoogleBigQueryExportRecord.User user = new GoogleBigQueryExportRecord.User();

				GoogleBigQueryRestApiQueryResponse.Row.RowField userIdField = row.getFields().get(fieldIndicesByName.get("user_id"));
				UUID userId = userIdField.getValue() == null ? null : UUID.fromString(userIdField.getValue());

				// User Pseudo ID
				GoogleBigQueryRestApiQueryResponse.Row.RowField userPseudoIdField = row.getFields().get(fieldIndicesByName.get("user_pseudo_id"));
				String userPseudoId = userPseudoIdField.getValue();

				// TODO

				// *** End User ***

				// *** Start Traffic Source ***
				GoogleBigQueryExportRecord.TrafficSource trafficSource = new GoogleBigQueryExportRecord.TrafficSource();
				GoogleBigQueryRestApiQueryResponse.Row.RowField trafficSourceField = row.getFields().get(fieldIndicesByName.get("traffic_source"));

				if (trafficSourceField.getField() != null) {
					GoogleBigQueryRestApiQueryResponse.Row.RowField fieldLevel1 = trafficSourceField.getField();

					if (fieldLevel1.getFields() != null) {
						List<GoogleBigQueryRestApiQueryResponse.Row.RowField> fieldsLevel2 = fieldLevel1.getFields();

						if (fieldsLevel2.size() != 3)
							throw new IllegalStateException("Not sure how to handle traffic source field " + trafficSourceField);

						String name = fieldsLevel2.get(0).getValue();
						String medium = fieldsLevel2.get(1).getValue();
						String source = fieldsLevel2.get(2).getValue();

						// TODO
					}
				}

				// *** End Traffic Source ***

				// *** Start Collected Traffic Source ***
				GoogleBigQueryExportRecord.CollectedTrafficSource collectedTrafficSource = new GoogleBigQueryExportRecord.CollectedTrafficSource();
				GoogleBigQueryRestApiQueryResponse.Row.RowField collectedTrafficSourceField = row.getFields().get(fieldIndicesByName.get("collected_traffic_source"));

				if (collectedTrafficSourceField.getField() != null) {
					GoogleBigQueryRestApiQueryResponse.Row.RowField fieldLevel1 = collectedTrafficSourceField.getField();

					if (fieldLevel1.getFields() != null) {
						List<GoogleBigQueryRestApiQueryResponse.Row.RowField> fieldsLevel2 = fieldLevel1.getFields();

						if (fieldsLevel2.size() != 9)
							throw new IllegalStateException("Not sure how to handle collected traffic source field " + trafficSourceField);

						String manualCampaignId = fieldsLevel2.get(0).getValue();
						String manualCampaignName = fieldsLevel2.get(1).getValue();
						String manualSource = fieldsLevel2.get(2).getValue();
						String manualMedium = fieldsLevel2.get(3).getValue();
						String manualTerm = fieldsLevel2.get(4).getValue();
						String manualContent = fieldsLevel2.get(5).getValue();
						String gclid = fieldsLevel2.get(6).getValue();
						String dclid = fieldsLevel2.get(7).getValue();
						String srsltid = fieldsLevel2.get(8).getValue();

						// TODO
					}
				}
				// *** End Collected Traffic Source ***

				// *** Start Device ***
				GoogleBigQueryExportRecord.Device device = new GoogleBigQueryExportRecord.Device();
				// TODO

				// *** End Device ***

				// *** Start Geo ***
				GoogleBigQueryExportRecord.Geo geo = new GoogleBigQueryExportRecord.Geo();
				// TODO

				// *** End Geo ***

				GoogleBigQueryExportRecord exportRecord = new GoogleBigQueryExportRecord();
				exportRecord.setEvent(event);
				exportRecord.setUser(user);
				exportRecord.setDevice(device);
				exportRecord.setGeo(geo);
				exportRecord.setTrafficSource(trafficSource);
				exportRecord.setCollectedTrafficSource(collectedTrafficSource);

				exportRecords.add(exportRecord);
			}

			return exportRecords;
		} catch (
				IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void performRestApiQuery(@Nonnull String sql) {
		requireNonNull(sql);

		// Special behavior: look for "{{datasetId}}" and replace it with the actual value
		// to make querying easier.
		sql = sql.replace("{{datasetId}}", getDatasetId());

		AccessToken accessToken = acquireAccessToken();
		String projectId = getProjectId();
		String url = format("https://www.googleapis.com/bigquery/v2/projects/%s/queries", projectId);

		HttpClient httpClient = new DefaultHttpClient("bigquery-rest-api");
		HttpRequest httpRequest = new HttpRequest.Builder(HttpMethod.POST, url)
				.contentType("application/json;charset=UTF-8")
				.queryParameters(Map.of(
						"access_token", accessToken.getTokenValue()
				))
				.body(new Gson().toJson(Map.of(
						"query", sql,
						// Note: if using legacy SQL in the future, duplicate rows will be returned.
						// If that happens - to get distinct results, compose a primary key (combination of these 4 fields) and filter:
						// event_name, event_timestamp, user_pseudo_id, event_bundle_sequence_id
						// See https://stackoverflow.com/a/75894260
						"use_legacy_sql", "false"
				)))
				.build();

		try {
			HttpResponse httpResponse = httpClient.execute(httpRequest);
			byte[] responseBody = httpResponse.getBody().orElse(null);
			String responseBodyAsString = responseBody == null ? null : new String(responseBody, StandardCharsets.UTF_8);

			if (httpResponse.getStatus() >= 400)
				throw new IOException(format("Received HTTP %d and response body:\n%s", httpResponse.getStatus(), responseBodyAsString));

			// TODO: finish up
		} catch (IOException e) {
			throw new UncheckedIOException(e);
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
	protected AccessToken acquireAccessToken() {
		try {
			getGoogleCredentials().refreshIfExpired();
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to refresh Google Credentials", e);
		}

		return getGoogleCredentials().getAccessToken();
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
