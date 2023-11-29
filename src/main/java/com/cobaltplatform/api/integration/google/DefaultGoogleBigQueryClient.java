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
import com.cobaltplatform.api.http.HttpRequestOption;
import com.cobaltplatform.api.http.HttpResponse;
import com.cobaltplatform.api.model.db.AnalyticsGoogleBigQueryEvent;
import com.cobaltplatform.api.util.GsonUtility;
import com.cobaltplatform.api.util.WebUtility;
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
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
	private static final DateTimeFormatter EXPORT_DATE_FORMATTER;

	static {
		EXPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US);
	}

	@Nonnull
	private final String projectId;
	@Nonnull
	private final String bigQueryResourceId;
	@Nonnull
	private final GoogleCredentials googleCredentials;
	@Nonnull
	private final BigQuery bigQuery;
	@Nonnull
	private final Gson gson;
	@Nonnull
	private final Logger logger;

	public DefaultGoogleBigQueryClient(@Nonnull String bigQueryResourceId,
																		 @Nonnull String serviceAccountPrivateKeyJson) {
		// ByteArrayInputStream does not need to be closed
		this(bigQueryResourceId, new ByteArrayInputStream(serviceAccountPrivateKeyJson.getBytes(StandardCharsets.UTF_8)));
	}

	public DefaultGoogleBigQueryClient(@Nonnull String bigQueryResourceId,
																		 @Nonnull InputStream serviceAccountPrivateKeyJsonInputStream) {
		requireNonNull(bigQueryResourceId);
		requireNonNull(serviceAccountPrivateKeyJsonInputStream);

		GsonBuilder gsonBuilder = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping();

		GsonUtility.applyDefaultTypeAdapters(gsonBuilder);

		this.logger = LoggerFactory.getLogger(getClass());
		this.gson = gsonBuilder.create();

		try {
			String serviceAccountPrivateKeyJson = CharStreams.toString(new InputStreamReader(requireNonNull(serviceAccountPrivateKeyJsonInputStream), StandardCharsets.UTF_8));

			// Confirm that this is well-formed JSON and extract the project ID
			Map<String, Object> jsonObject = getGson().fromJson(serviceAccountPrivateKeyJson, new TypeToken<Map<String, Object>>() {
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

	/**
	 * Package-private to allow tests to access.
	 */
	GoogleBigQueryExportRecordsPage extractGoogleBigQueryExportRecordsFromPageJson(@Nonnull String pageJson) {
		requireNonNull(pageJson);

		GoogleBigQueryRestApiQueryResponse response = GoogleBigQueryRestApiQueryResponse.fromJson(pageJson);

		Map<String, Integer> fieldIndicesByName = new HashMap<>(response.getSchema().getFields().size());

		for (int i = 0; i < response.getSchema().getFields().size(); ++i) {
			GoogleBigQueryRestApiQueryResponse.TableSchema.TableFieldSchema field = response.getSchema().getFields().get(i);
			fieldIndicesByName.put(field.getName(), i);
		}

		// TODO: gracefully detect "data is not ready yet" from response body and throw a specific exception.
		// This way sync code can catch something like GoogleBigQueryNoDataAvailableYetException and know it's OK to re-try syncing later
		if (response.getRows() == null)
			throw new IllegalStateException(format("BigQuery data is not available yet. Page JSON was: %s", pageJson));

		List<GoogleBigQueryRestApiQueryResponse.Row> rows = new ArrayList<>(response.getRows());
		List<GoogleBigQueryExportRecord> exportRecords = new ArrayList<>(rows.size());

		// Pull relevant data from the response.
		// Schema is documented at https://support.google.com/analytics/answer/7029846?hl=en
		for (GoogleBigQueryRestApiQueryResponse.Row row : rows) {
			// *** Start Event ***
			AnalyticsGoogleBigQueryEvent.Event event = new AnalyticsGoogleBigQueryEvent.Event();

			// Event date
			GoogleBigQueryRestApiQueryResponse.Row.RowField eventDateField = row.getFields().get(fieldIndicesByName.get("event_date"));
			event.setDate(LocalDate.parse(eventDateField.getValue(), EXPORT_DATE_FORMATTER));

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

			// Event server timestamp offset
			GoogleBigQueryRestApiQueryResponse.Row.RowField eventServerTimestampOffsetField = row.getFields().get(fieldIndicesByName.get("event_server_timestamp_offset"));
			event.setServerTimestampOffset(eventServerTimestampOffsetField.getValue() == null ? null : Long.valueOf(eventServerTimestampOffsetField.getValue()));

			// Event Params
			GoogleBigQueryRestApiQueryResponse.Row.RowField eventParamsField = row.getFields().get(fieldIndicesByName.get("event_params"));
			List<GoogleBigQueryRestApiQueryResponse.Row.RowField> eventParamsFields = eventParamsField.getFields();

			Map<String, AnalyticsGoogleBigQueryEvent.Event.EventParamValue> eventParameters = new HashMap<>();

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
						AnalyticsGoogleBigQueryEvent.Event.EventParamValue eventParamValue = new AnalyticsGoogleBigQueryEvent.Event.EventParamValue();

						if (stringRowField.getValue() != null) {
							eventParamValue.setValue(stringValue);
							eventParamValue.setType(AnalyticsGoogleBigQueryEvent.Event.EventParamValueType.STRING);
						} else {
							// Google's INTEGER definition can be larger than Java's Integer.MAX_VALUE, so we use Long instead
							GoogleBigQueryRestApiQueryResponse.Row.RowField longRowField = fieldLevel3ParameterValueLevel2.getFields().get(1);
							Long longValue = longRowField.getValue() == null ? null : Long.valueOf(longRowField.getValue());

							if (longValue != null) {
								eventParamValue.setValue(longValue);
								eventParamValue.setType(AnalyticsGoogleBigQueryEvent.Event.EventParamValueType.INTEGER);
							} else {
								GoogleBigQueryRestApiQueryResponse.Row.RowField floatRowField = fieldLevel3ParameterValueLevel2.getFields().get(2);
								Float floatValue = floatRowField.getValue() == null ? null : Float.valueOf(floatRowField.getValue());

								if (floatValue != null) {
									eventParamValue.setValue(floatValue);
									eventParamValue.setType(AnalyticsGoogleBigQueryEvent.Event.EventParamValueType.FLOAT);
								} else {
									GoogleBigQueryRestApiQueryResponse.Row.RowField doubleRowField = fieldLevel3ParameterValueLevel2.getFields().get(3);
									Double doubleValue = doubleRowField.getValue() == null ? null : Double.valueOf(doubleRowField.getValue());

									if (doubleValue != null) {
										eventParamValue.setValue(doubleValue);
										eventParamValue.setType(AnalyticsGoogleBigQueryEvent.Event.EventParamValueType.DOUBLE);
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
			AnalyticsGoogleBigQueryEvent.User user = new AnalyticsGoogleBigQueryEvent.User();

			GoogleBigQueryRestApiQueryResponse.Row.RowField userIdField = row.getFields().get(fieldIndicesByName.get("user_id"));
			user.setUserId(userIdField.getValue());

			// User Pseudo ID
			GoogleBigQueryRestApiQueryResponse.Row.RowField userPseudoIdField = row.getFields().get(fieldIndicesByName.get("user_pseudo_id"));
			user.setUserPseudoId(userPseudoIdField.getValue());

			// Is Active User
			GoogleBigQueryRestApiQueryResponse.Row.RowField isActiveUserField = row.getFields().get(fieldIndicesByName.get("is_active_user"));
			user.setIsActiveUser("true".equalsIgnoreCase(isActiveUserField.getValue()));

			// First Touch Timestamp
			GoogleBigQueryRestApiQueryResponse.Row.RowField userFirstTouchTimestampField = row.getFields().get(fieldIndicesByName.get("user_first_touch_timestamp"));

			if (userFirstTouchTimestampField.getValue() != null) {
				long userFirstTouchTimestampFieldValueAsMicroseconds = Long.valueOf(userFirstTouchTimestampField.getValue());
				Instant userFirstTouchTimestamp = Instant.ofEpochSecond(
						TimeUnit.MICROSECONDS.toSeconds(userFirstTouchTimestampFieldValueAsMicroseconds),
						TimeUnit.MICROSECONDS.toNanos(userFirstTouchTimestampFieldValueAsMicroseconds % TimeUnit.SECONDS.toMicros(1)));
				user.setUserFirstTouchTimestamp(userFirstTouchTimestamp);
			}

			// *** End User ***

			// *** Start Traffic Source ***

			AnalyticsGoogleBigQueryEvent.TrafficSource trafficSource = new AnalyticsGoogleBigQueryEvent.TrafficSource();
			GoogleBigQueryRestApiQueryResponse.Row.RowField trafficSourceField = row.getFields().get(fieldIndicesByName.get("traffic_source"));

			if (trafficSourceField.getField() != null) {
				GoogleBigQueryRestApiQueryResponse.Row.RowField fieldLevel1 = trafficSourceField.getField();

				if (fieldLevel1.getFields() != null) {
					List<GoogleBigQueryRestApiQueryResponse.Row.RowField> fieldsLevel2 = fieldLevel1.getFields();

					if (fieldsLevel2.size() != 3)
						throw new IllegalStateException("Not sure how to handle traffic source field " + trafficSourceField);

					trafficSource.setName(fieldsLevel2.get(0).getValue());
					trafficSource.setMedium(fieldsLevel2.get(1).getValue());
					trafficSource.setSource(fieldsLevel2.get(2).getValue());
				}
			}

			// *** End Traffic Source ***

			// *** Start Collected Traffic Source ***

			AnalyticsGoogleBigQueryEvent.CollectedTrafficSource collectedTrafficSource = new AnalyticsGoogleBigQueryEvent.CollectedTrafficSource();
			GoogleBigQueryRestApiQueryResponse.Row.RowField collectedTrafficSourceField = row.getFields().get(fieldIndicesByName.get("collected_traffic_source"));

			if (collectedTrafficSourceField.getField() != null) {
				GoogleBigQueryRestApiQueryResponse.Row.RowField fieldLevel1 = collectedTrafficSourceField.getField();

				if (fieldLevel1.getFields() != null) {
					List<GoogleBigQueryRestApiQueryResponse.Row.RowField> fieldsLevel2 = fieldLevel1.getFields();

					if (fieldsLevel2.size() != 9)
						throw new IllegalStateException("Not sure how to handle collected traffic source field " + trafficSourceField);

					collectedTrafficSource.setManualCampaignId(fieldsLevel2.get(0).getValue());
					collectedTrafficSource.setManualCampaignName(fieldsLevel2.get(1).getValue());
					collectedTrafficSource.setManualSource(fieldsLevel2.get(2).getValue());
					collectedTrafficSource.setManualMedium(fieldsLevel2.get(3).getValue());
					collectedTrafficSource.setManualTerm(fieldsLevel2.get(4).getValue());
					collectedTrafficSource.setManualContent(fieldsLevel2.get(5).getValue());
					collectedTrafficSource.setGclid(fieldsLevel2.get(6).getValue());
					collectedTrafficSource.setDclid(fieldsLevel2.get(7).getValue());
					collectedTrafficSource.setSrsltid(fieldsLevel2.get(8).getValue());
				}
			}

			// *** End Collected Traffic Source ***

			// *** Start Device ***

			AnalyticsGoogleBigQueryEvent.Device device = new AnalyticsGoogleBigQueryEvent.Device();
			GoogleBigQueryRestApiQueryResponse.Row.RowField deviceField = row.getFields().get(fieldIndicesByName.get("device"));

			if (deviceField.getField() != null) {
				GoogleBigQueryRestApiQueryResponse.Row.RowField fieldLevel1 = deviceField.getField();

				if (fieldLevel1.getFields() != null) {
					List<GoogleBigQueryRestApiQueryResponse.Row.RowField> fieldsLevel2 = fieldLevel1.getFields();

					if (fieldsLevel2.size() != 15)
						throw new IllegalStateException("Not sure how to handle device field " + deviceField);

					device.setCategory(fieldsLevel2.get(0).getValue());
					device.setMobileBrandName(fieldsLevel2.get(1).getValue());
					device.setMobileModelName(fieldsLevel2.get(2).getValue());
					device.setMobileMarketingName(fieldsLevel2.get(3).getValue());
					device.setMobileOsHardwareModel(fieldsLevel2.get(4).getValue());
					device.setOperatingSystem(fieldsLevel2.get(5).getValue());
					device.setOperatingSystemVersion(fieldsLevel2.get(6).getValue());
					device.setVendorId(fieldsLevel2.get(7).getValue());
					device.setAdvertisingId(fieldsLevel2.get(8).getValue());
					device.setLanguage(fieldsLevel2.get(9).getValue());

					String isLimitedAdTrackingAsString = fieldsLevel2.get(10).getValue();
					device.setIsLimitedAdTracking(isLimitedAdTrackingAsString != null && !isLimitedAdTrackingAsString.equalsIgnoreCase("no"));

					String timeZoneOffsetSecondsAsString = fieldsLevel2.get(11).getValue();

					if (timeZoneOffsetSecondsAsString != null)
						device.setTimeZoneOffsetSeconds(Long.valueOf(timeZoneOffsetSecondsAsString));

					device.setBrowser(fieldsLevel2.get(12).getValue());
					device.setBrowserVersion(fieldsLevel2.get(13).getValue());

					GoogleBigQueryRestApiQueryResponse.Row.RowField webInfoField = fieldsLevel2.get(14).getField();

					if (webInfoField != null) {
						if (webInfoField.getFields().size() != 3)
							throw new IllegalStateException("Not sure how to handle device web info field " + webInfoField);

						device.setWebInfoBrowser(webInfoField.getFields().get(0).getValue());
						device.setWebInfoBrowserVersion(webInfoField.getFields().get(1).getValue());
						device.setWebInfoBrowserHostname(webInfoField.getFields().get(2).getValue());
					}
				}
			}

			// *** End Device ***

			// *** Start Geo ***

			AnalyticsGoogleBigQueryEvent.Geo geo = new AnalyticsGoogleBigQueryEvent.Geo();
			GoogleBigQueryRestApiQueryResponse.Row.RowField geoField = row.getFields().get(fieldIndicesByName.get("geo"));

			if (geoField.getField() != null) {
				GoogleBigQueryRestApiQueryResponse.Row.RowField fieldLevel1 = geoField.getField();

				if (fieldLevel1.getFields() != null) {
					List<GoogleBigQueryRestApiQueryResponse.Row.RowField> fieldsLevel2 = fieldLevel1.getFields();

					if (fieldsLevel2.size() != 6)
						throw new IllegalStateException("Not sure how to handle geo field " + geoField);

					geo.setCity(fieldsLevel2.get(0).getValue());
					geo.setCountry(fieldsLevel2.get(1).getValue());
					geo.setContinent(fieldsLevel2.get(2).getValue());
					geo.setRegion(fieldsLevel2.get(3).getValue());
					geo.setSubContinent(fieldsLevel2.get(4).getValue());
					geo.setMetro(fieldsLevel2.get(5).getValue());
				}
			}

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

		return new GoogleBigQueryExportRecordsPage(response, exportRecords);
	}

	@Override
	@Nonnull
	public List<GoogleBigQueryExportRecord> performRestApiQueryForExport(@Nonnull String sql,
																																			 @Nonnull Duration timeout) {
		requireNonNull(sql);
		requireNonNull(timeout);

		// Special behavior: look for "{{datasetId}}" and replace it with the actual value
		// to make querying easier.
		sql = sql.replace("{{datasetId}}", getDatasetId());

		AccessToken accessToken = acquireAccessToken();
		String projectId = getProjectId();

		// See https://cloud.google.com/bigquery/docs/reference/rest/v2/jobs/query
		String url = format("https://www.googleapis.com/bigquery/v2/projects/%s/queries", projectId);

		HttpClient httpClient = new DefaultHttpClient("bigquery-rest-api");
		HttpRequest httpRequest = new HttpRequest.Builder(HttpMethod.POST, url)
				.contentType("application/json;charset=UTF-8")
				.queryParameters(Map.of(
						"access_token", accessToken.getTokenValue()
				))
				.body(getGson().toJson(Map.of(
						"query", sql,
						// Note: if using legacy SQL in the future, duplicate rows will be returned.
						// If that happens - to get distinct results, compose a primary key (combination of these 4 fields) and filter:
						// event_name, event_timestamp, user_pseudo_id, event_bundle_sequence_id
						// See https://stackoverflow.com/a/75894260
						"use_legacy_sql", "false",
						"timeout_ms", timeout.toMillis()
				)))
				.build();

		try {
			HttpResponse httpResponse = httpClient.execute(httpRequest, HttpRequestOption.SUPPRESS_RESPONSE_BODY_LOGGING);
			byte[] responseBody = httpResponse.getBody().orElse(null);
			String responseBodyAsString = responseBody == null ? null : new String(responseBody, StandardCharsets.UTF_8);

			if (httpResponse.getStatus() >= 400)
				throw new IOException(format("Received HTTP %d and response body:\n%s", httpResponse.getStatus(), responseBodyAsString));

			// First page of data
			GoogleBigQueryExportRecordsPage page = extractGoogleBigQueryExportRecordsFromPageJson(responseBodyAsString);
			List<GoogleBigQueryExportRecord> exportRecords = new ArrayList<>(Integer.valueOf(page.getResponse().getTotalRows()));
			exportRecords.addAll(page.getExportRecords());

			getLogger().info("BigQuery page has {} rows ({} of {} total).", page.getResponse().getRows().size(), exportRecords.size(), page.getResponse().getTotalRows());

			// More pages?  Walk them using the page token
			while (page.getResponse().getPageToken() != null) {
				// See https://cloud.google.com/bigquery/docs/paging-results
				// See https://cloud.google.com/bigquery/docs/reference/rest/v2/jobs/getQueryResults
				String pageUrl = format("https://www.googleapis.com/bigquery/v2/projects/%s/queries/%s", projectId, page.getResponse().getJobReference().getJobId());

				HttpRequest httpRequestForPage = new HttpRequest.Builder(HttpMethod.GET, pageUrl)
						.queryParameters(Map.of(
								"access_token", accessToken.getTokenValue(),
								"timeout_ms", timeout.toMillis(),
								"page_token", page.getResponse().getPageToken()
						))
						.build();

				HttpResponse httpResponseForPage = httpClient.execute(httpRequestForPage, HttpRequestOption.SUPPRESS_RESPONSE_BODY_LOGGING);
				byte[] responseBodyForPage = httpResponseForPage.getBody().orElse(null);
				String responseBodyAsStringForPage = responseBodyForPage == null ? null : new String(responseBodyForPage, StandardCharsets.UTF_8);

				if (httpResponseForPage.getStatus() >= 400)
					throw new IOException(format("Received HTTP %d and response body:\n%s", httpResponseForPage.getStatus(), responseBodyAsStringForPage));

				// Parse the page of data
				page = extractGoogleBigQueryExportRecordsFromPageJson(responseBodyAsStringForPage);
				exportRecords.addAll(page.getExportRecords());
				getLogger().info("BigQuery page has {} rows ({} of {} total).", page.getResponse().getRows().size(), exportRecords.size(), page.getResponse().getTotalRows());
			}

			// Elide sensitive data in URLs
			for (GoogleBigQueryExportRecord exportRecord : exportRecords) {
				// These modify `event` in-place
				elideSensitiveDataInUrlForEventParameterName("page_location", exportRecord.getEvent());
				elideSensitiveDataInUrlForEventParameterName("page_referrer", exportRecord.getEvent());
			}

			return exportRecords;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// Modifies `event` in-place
	protected void elideSensitiveDataInUrlForEventParameterName(@Nonnull String parameterName,
																															@Nonnull AnalyticsGoogleBigQueryEvent.Event event) {
		requireNonNull(parameterName);
		requireNonNull(event);

		AnalyticsGoogleBigQueryEvent.Event.EventParamValue eventParamValue = event.getParameters().get(parameterName);

		if (eventParamValue != null) {
			String url = (String) eventParamValue.getValue();

			if (url != null) {
				url = WebUtility.elideSensitiveDataInUrl(url);
				eventParamValue.setValue(url);
			}
		}
	}

	@NotThreadSafe
	static class GoogleBigQueryExportRecordsPage {
		@Nonnull
		private final GoogleBigQueryRestApiQueryResponse response;
		@Nonnull
		private final List<GoogleBigQueryExportRecord> exportRecords;

		public GoogleBigQueryExportRecordsPage(@Nonnull GoogleBigQueryRestApiQueryResponse response,
																					 @Nonnull List<GoogleBigQueryExportRecord> exportRecords) {
			requireNonNull(response);
			requireNonNull(exportRecords);

			this.response = response;
			this.exportRecords = exportRecords;
		}

		@Nonnull
		public GoogleBigQueryRestApiQueryResponse getResponse() {
			return this.response;
		}

		@Nonnull
		public List<GoogleBigQueryExportRecord> getExportRecords() {
			return this.exportRecords;
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

	@Nonnull
	protected Gson getGson() {
		return this.gson;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
