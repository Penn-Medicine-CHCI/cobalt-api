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

package com.cobaltplatform.api.integration.acuity;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.http.DefaultHttpClient;
import com.cobaltplatform.api.http.HttpClient;
import com.cobaltplatform.api.http.HttpMethod;
import com.cobaltplatform.api.http.HttpRequest;
import com.cobaltplatform.api.http.HttpResponse;
import com.cobaltplatform.api.integration.acuity.model.AcuityAppointment;
import com.cobaltplatform.api.integration.acuity.model.AcuityAppointmentType;
import com.cobaltplatform.api.integration.acuity.model.AcuityCalendar;
import com.cobaltplatform.api.integration.acuity.model.AcuityCheckTime;
import com.cobaltplatform.api.integration.acuity.model.AcuityClass;
import com.cobaltplatform.api.integration.acuity.model.AcuityDate;
import com.cobaltplatform.api.integration.acuity.model.AcuityError;
import com.cobaltplatform.api.integration.acuity.model.AcuityForm;
import com.cobaltplatform.api.integration.acuity.model.AcuityTime;
import com.cobaltplatform.api.integration.acuity.model.request.AcuityAppointmentCreateRequest;
import com.cobaltplatform.api.integration.acuity.model.request.AcuityAvailabilityCheckTimeRequest;
import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class DefaultAcuitySchedulingClient implements AcuitySchedulingClient {
	@Nonnull
	private static final Integer MAXIMUM_RETRY_COUNT;
	@Nonnull
	private static final Long RETRY_WAIT_DURATION_IN_MILLISECONDS;
	@Nonnull
	private static final DateTimeFormatter HISTOGRAM_BUCKET_FORMATTER;
	@Nonnull
	private static final Integer HISTOGRAM_BUCKET_MAXIMUM_COUNT;

	@Nonnull
	private final String acuityUserId;
	@Nonnull
	private final String acuityApiKey;
	@Nonnull
	private final HttpClient httpClient;
	@Nonnull
	private final Gson gson;
	@Nonnull
	private final RateLimiter rateLimiter;
	@Nonnull
	private final Map<String, LongAdder> callFrequencyHistogram;
	@Nonnull
	private final Logger logger;

	static {
		MAXIMUM_RETRY_COUNT = 3;
		RETRY_WAIT_DURATION_IN_MILLISECONDS = 200L;
		HISTOGRAM_BUCKET_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd' 'hh:mm:ss", Locale.US).withZone(ZoneId.of("America/New_York"));
		HISTOGRAM_BUCKET_MAXIMUM_COUNT = 180;
	}

	public DefaultAcuitySchedulingClient(@Nonnull Configuration configuration) {
		this(configuration.getAcuityUserId(), configuration.getAcuityApiKey());
	}

	public DefaultAcuitySchedulingClient(@Nonnull String acuityUserId,
																			 @Nonnull String acuityApiKey) {
		requireNonNull(acuityUserId);
		requireNonNull(acuityApiKey);

		this.acuityUserId = acuityUserId;
		this.acuityApiKey = acuityApiKey;
		this.httpClient = createHttpClient();
		this.gson = new Gson();
		this.rateLimiter = createRateLimiter();
		this.callFrequencyHistogram = Collections.synchronizedMap(new LinkedHashMap<>() {
			protected boolean removeEldestEntry(Entry<String, LongAdder> eldest) {
				return size() > getHistogramBucketMaximumCount();
			}
		});
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@Override
	public List<AcuityCalendar> findCalendars() {
		return makeGetApiCall("https://acuityscheduling.com/api/v1/calendars", (responseBody) -> {
					return getGson().fromJson(responseBody, new TypeToken<ArrayList<AcuityCalendar>>() {
					}.getType());
				}
		);
	}

	@Nonnull
	@Override
	public Optional<AcuityCalendar> findCalendarById(@Nullable Long calendarId) {
		if (calendarId == null)
			return Optional.empty();

		List<AcuityCalendar> calendars = findCalendars();

		for (AcuityCalendar calendar : calendars)
			if (calendar.getId().equals(calendarId))
				return Optional.of(calendar);

		return Optional.empty();
	}

	@Nonnull
	@Override
	public List<AcuityAppointmentType> findAppointmentTypes() {
		return makeGetApiCall("https://acuityscheduling.com/api/v1/appointment-types", (responseBody) -> {
					return getGson().fromJson(responseBody, new TypeToken<ArrayList<AcuityAppointmentType>>() {
					}.getType());
				}
		);
	}

	@Nonnull
	@Override
	public Optional<AcuityAppointmentType> findAppointmentTypeById(@Nullable Long appointmentTypeId) {
		if (appointmentTypeId == null)
			return Optional.empty();

		List<AcuityAppointmentType> appointmentTypes = findAppointmentTypes();

		for (AcuityAppointmentType appointmentType : appointmentTypes)
			if (appointmentType.getId().equals(appointmentTypeId))
				return Optional.of(appointmentType);

		return Optional.empty();
	}

	@Nonnull
	@Override
	public List<AcuityDate> findAvailabilityDates(@Nonnull Long calendarId,
																								@Nonnull Long appointmentTypeId,
																								@Nonnull YearMonth yearMonth,
																								@Nonnull ZoneId timeZone) {
		requireNonNull(calendarId);
		requireNonNull(appointmentTypeId);
		requireNonNull(yearMonth);
		requireNonNull(timeZone);

		return makeGetApiCall("https://acuityscheduling.com/api/v1/availability/dates", new HashMap<String, Object>() {{
					put("month", format("%d-%02d", yearMonth.getYear(), yearMonth.getMonthValue()));
					put("calendarID", calendarId);
					put("appointmentTypeID", appointmentTypeId);
					put("timezone", timeZone.getId());
				}}, (responseBody) -> {
					return getGson().fromJson(responseBody, new TypeToken<ArrayList<AcuityDate>>() {
					}.getType());
				}
		);
	}

	@Nonnull
	@Override
	public List<AcuityTime> findAvailabilityTimes(@Nonnull Long calendarId,
																								@Nonnull Long appointmentTypeId,
																								@Nonnull LocalDate localDate,
																								@Nonnull ZoneId timeZone) {
		requireNonNull(calendarId);
		requireNonNull(appointmentTypeId);
		requireNonNull(localDate);
		requireNonNull(timeZone);

		return makeGetApiCall("https://acuityscheduling.com/api/v1/availability/times", new HashMap<String, Object>() {{
					put("date", format("%d-%02d-%02d", localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth()));
					put("calendarID", calendarId);
					put("appointmentTypeID", appointmentTypeId);
					put("timezone", timeZone.getId());
				}}, (responseBody) -> {
					return getGson().fromJson(responseBody, new TypeToken<ArrayList<AcuityTime>>() {
					}.getType());
				}
		);
	}

	@Nonnull
	@Override
	public List<AcuityClass> findAvailabilityClasses(@Nonnull YearMonth yearMonth,
																									 @Nonnull ZoneId timeZone) {
		requireNonNull(yearMonth);
		requireNonNull(timeZone);

		return makeGetApiCall("https://acuityscheduling.com/api/v1/availability/classes", new HashMap<String, Object>() {{
					put("month", format("%d-%02d", yearMonth.getYear(), yearMonth.getMonthValue()));
					put("timezone", timeZone.getId());
					put("includeUnavailable", true);  // We want to pull back even those classes that are full or otherwise unavailable
				}}, (responseBody) -> {
					return getGson().fromJson(responseBody, new TypeToken<ArrayList<AcuityClass>>() {
					}.getType());
				}
		);
	}

	@Nonnull
	@Override
	public List<AcuityForm> findForms() {
		return makeGetApiCall("https://acuityscheduling.com/api/v1/forms", Collections.emptyMap(), (responseBody) -> {
					return getGson().fromJson(responseBody, new TypeToken<ArrayList<AcuityForm>>() {
					}.getType());
				}
		);
	}

	@Nonnull
	@Override
	public AcuityAppointment createAppointment(@Nonnull AcuityAppointmentCreateRequest request) {
		requireNonNull(request);

		Map<String, Object> requestBody = new HashMap<>();

		// e.g. 2016-02-03T14:00:00-0800
		String datetime = request.getDatetime().atZone(request.getTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

		requestBody.put("datetime", datetime);
		requestBody.put("calendarID", request.getCalendarId());
		requestBody.put("appointmentTypeID", request.getAppointmentTypeId());
		requestBody.put("timezone", request.getTimeZone().getId());
		requestBody.put("firstName", request.getFirstName());
		requestBody.put("lastName", request.getLastName());
		requestBody.put("email", request.getEmail());
		requestBody.put("fields", request.getFields());

		String requestBodyJson = getGson().toJson(requestBody);

		return makeApiCall(HttpMethod.POST, "https://acuityscheduling.com/api/v1/appointments", Collections.emptyMap(),
				requestBodyJson, (responseBody) -> {
					return getGson().fromJson(responseBody, AcuityAppointment.class);
				});
	}

	@Nonnull
	@Override
	public AcuityAppointment cancelAppointment(@Nonnull Long appointmentId) {
		requireNonNull(appointmentId);

		return makeApiCall(HttpMethod.PUT, format("https://acuityscheduling.com/api/v1/appointments/%s/cancel", appointmentId), Collections.emptyMap(),
				null, (responseBody) -> {
					return getGson().fromJson(responseBody, AcuityAppointment.class);
				});
	}

	@Nonnull
	@Override
	public List<AcuityAppointment> findAppointments() {
		return makeGetApiCall("https://acuityscheduling.com/api/v1/appointments",
				(responseBody) -> {
					return getGson().fromJson(responseBody, new TypeToken<ArrayList<AcuityAppointment>>() {
					}.getType());
				});
	}

	@Nonnull
	@Override
	public Optional<AcuityAppointment> findAppointmentById(@Nullable Long appointmentId) {
		if (appointmentId == null)
			return Optional.empty();

		try {
			return makeGetApiCall(format("https://acuityscheduling.com/api/v1/appointments/%s", appointmentId),
					(responseBody) -> {
						return Optional.of(getGson().fromJson(responseBody, AcuityAppointment.class));
					});
		} catch (AcuitySchedulingNotFoundException e) {
			return Optional.empty();
		}
	}

	@Nonnull
	@Override
	public AcuityCheckTime availabilityCheckTime(@Nonnull AcuityAvailabilityCheckTimeRequest request) {
		requireNonNull(request);

		Map<String, Object> requestBody = new HashMap<>();

		// e.g. 2016-02-03T14:00:00-0800
		String datetime = request.getDatetime().atZone(request.getTimeZone()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

		requestBody.put("datetime", datetime);
		requestBody.put("calendarID", request.getCalendarId());
		requestBody.put("appointmentTypeID", request.getAppointmentTypeId());

		String requestBodyJson = getGson().toJson(requestBody);

		return makeApiCall(HttpMethod.POST, "https://acuityscheduling.com/api/v1/availability/check-times", Collections.emptyMap(),
				requestBodyJson, (responseBody) -> {
					return getGson().fromJson(responseBody, AcuityCheckTime.class);
				});
	}

	@Nonnull
	protected <T> T makeGetApiCall(@Nonnull String url,
																 @Nonnull Function<String, T> responseBodyMapper) {
		return makeGetApiCall(url, null, responseBodyMapper);
	}

	@Nonnull
	protected <T> T makeGetApiCall(@Nonnull String url,
																 @Nullable Map<String, Object> queryParameters,
																 @Nonnull Function<String, T> responseBodyMapper) {
		return makeApiCall(HttpMethod.GET, url, queryParameters, null, responseBodyMapper);
	}

	@Nonnull
	protected <T> T makeApiCall(@Nonnull HttpMethod httpMethod,
															@Nonnull String url,
															@Nullable Map<String, Object> queryParameters,
															@Nullable String requestBody,
															@Nonnull Function<String, T> responseBodyMapper) {
		return makeApiCall(httpMethod, url, queryParameters, requestBody, responseBodyMapper, null);
	}

	@Nonnull
	protected <T> T makeApiCall(@Nonnull HttpMethod httpMethod,
															@Nonnull String url,
															@Nullable Map<String, Object> queryParameters,
															@Nullable String requestBody,
															@Nonnull Function<String, T> responseBodyMapper,
															@Nullable Integer retryCount) {
		requireNonNull(httpMethod);
		requireNonNull(url);
		requireNonNull(responseBodyMapper);

		if (queryParameters == null)
			queryParameters = Collections.emptyMap();

		if (retryCount == null)
			retryCount = 0;

		String basicAuthCredentials = format("%s:%s", getAcuityUserId(), getAcuityApiKey());
		String encodedBasicAuthCredentials = Base64.getEncoder().encodeToString(basicAuthCredentials.getBytes(StandardCharsets.UTF_8));

		HttpRequest.Builder httpRequestBuilder = new HttpRequest.Builder(httpMethod, url)
				.headers(new HashMap<String, Object>() {{
					put("Authorization", format("Basic %s", encodedBasicAuthCredentials));
				}});

		if (queryParameters.size() > 0)
			httpRequestBuilder.queryParameters(queryParameters);

		if (requestBody != null)
			httpRequestBuilder.body(requestBody);

		if (httpMethod == HttpMethod.POST)
			httpRequestBuilder.contentType("application/json; charset=utf-8");

		HttpRequest httpRequest = httpRequestBuilder.build();

		String queryParametersDescription = queryParameters.size() == 0 ? "[none]" : queryParameters.toString();
		String requestBodyDescription = requestBody == null ? "[none]" : requestBody;

		// Choke at 10 requests/second
		getRateLimiter().acquire();

		String histogramBucket = getHistogramBucketFormatter().format(Instant.now());
		getCallFrequencyHistogramInternal().computeIfAbsent(histogramBucket, ignored -> new LongAdder()).increment();

		try {
			HttpResponse httpResponse = getHttpClient().execute(httpRequest);
			byte[] rawResponseBody = httpResponse.getBody().orElse(null);
			String responseBody = rawResponseBody == null ? null : new String(rawResponseBody, StandardCharsets.UTF_8);

			if (httpResponse.getStatus() > 299) {
				// Example response body:
				// {"status_code":404,"message":"Not Found","error":"not_found"}

				// Seems like Acuity has some layer above its API that (mistakenly?) is rate-limiting us?
				// If we detect this special case, throw a special exception
				if (httpResponse.getStatus() == 403 && "</html>".equals(trimToEmpty(responseBody)))
					throw new AcuitySchedulingUndocumentedRateLimitException(format("Detected undocumented rate limit error for Acuity Scheduling API endpoint %s %s with query params %s and request body %s", httpResponse.getStatus(), httpRequest.getHttpMethod().name(), httpRequest.getUrl(), queryParametersDescription, requestBodyDescription));

				AcuityError acuityError = null;

				try {
					AcuityError parsedAcuityError = getGson().fromJson(responseBody, AcuityError.class);

					if (parsedAcuityError != null
							&& parsedAcuityError.getError() != null
							&& parsedAcuityError.getMessage() != null
							&& parsedAcuityError.getStatusCode() != null)
						acuityError = parsedAcuityError;
				} catch (Exception e) {
					getLogger().warn("Unable to parse Acuity error response body, continuing on with error flow...", e);
				}

				// Throw specific exceptions for cases we might want to handle specially
				if (acuityError != null) {
					if ("not_available".equals(acuityError.getError()))
						throw new AcuitySchedulingNotAvailableException(acuityError.getMessage(), acuityError);
					if ("not_found".equals(acuityError.getError()))
						throw new AcuitySchedulingNotFoundException(acuityError.getMessage(), acuityError);

					if (httpResponse.getStatus() == 429 || "too_many_requests".equals(acuityError.getError())) {
						// Rate limited - sleep and retry if we are under
						if (retryCount < getMaximumRetryCount()) {
							Integer newRetryCount = retryCount + 1;
							getLogger().info("Rate limited by Acuity - sleeping for {}ms and retrying ({} out of {})...", getRetryWaitDurationInMilliseconds(), newRetryCount, getMaximumRetryCount());

							try {
								Thread.sleep(getRetryWaitDurationInMilliseconds());
							} catch (InterruptedException e) {
								// Ignore
							}

							return makeApiCall(httpMethod, url, queryParameters, requestBody, responseBodyMapper, newRetryCount);
						} else {
							getLogger().warn("Retried {} times unsuccessfully, erroring out...", retryCount);
						}
					}
				}

				throw new AcuitySchedulingException(format("Bad HTTP response %d for Acuity Scheduling API endpoint %s %s with query params %s and request body %s. Response body was\n%s", httpResponse.getStatus(), httpRequest.getHttpMethod().name(), httpRequest.getUrl(), queryParametersDescription, requestBodyDescription, responseBody), acuityError);
			}

			try {
				return responseBodyMapper.apply(responseBody);
			} catch (Exception e) {
				throw new AcuitySchedulingException(format("Unable to parse JSON for Acuity Scheduling API endpoint %s %s with query params %s  and request body %s. Response body was\n%s", httpRequest.getHttpMethod().name(), httpRequest.getUrl(), queryParametersDescription, requestBodyDescription, responseBody));
			}
		} catch (IOException e) {
			throw new AcuitySchedulingException(format("Unable to call Acuity Scheduling API endpoint %s %s with query params %s  and request body %s", httpRequest.getHttpMethod().name(), httpRequest.getUrl(), queryParametersDescription, requestBodyDescription), e);
		}
	}

	@Nonnull
	@Override
	public Boolean verifyWebhookRequestCameFromAcuity(@Nonnull String acuitySignature,
																										@Nonnull String requestBody) {
		requireNonNull(acuitySignature);
		requireNonNull(requestBody);

		try {
			Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
			SecretKeySpec secretKey = new SecretKeySpec(getAcuityApiKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
			hmacSHA256.init(secretKey);

			String requestBodySignature = Base64.getEncoder().encodeToString(hmacSHA256.doFinal(requestBody.getBytes(StandardCharsets.UTF_8)));
			return requestBodySignature.equals(acuitySignature);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			// Should not happen
			throw new RuntimeException(e);
		}
	}

	@Nonnull
	protected HttpClient createHttpClient() {
		return new DefaultHttpClient("com.cobaltplatform.api.integration.acuity");
	}

	@Nonnull
	protected RateLimiter createRateLimiter() {
		// Per Acuity support, we can do 10 requests per second :(
		return RateLimiter.create(5);
	}

	@Nonnull
	protected Integer getMaximumRetryCount() {
		return MAXIMUM_RETRY_COUNT;
	}

	@Nonnull
	protected Long getRetryWaitDurationInMilliseconds() {
		return RETRY_WAIT_DURATION_IN_MILLISECONDS;
	}

	@Nonnull
	protected DateTimeFormatter getHistogramBucketFormatter() {
		return HISTOGRAM_BUCKET_FORMATTER;
	}

	@Nonnull
	protected Integer getHistogramBucketMaximumCount() {
		return HISTOGRAM_BUCKET_MAXIMUM_COUNT;
	}

	@Nonnull
	protected String getAcuityUserId() {
		return acuityUserId;
	}

	@Nonnull
	protected String getAcuityApiKey() {
		return acuityApiKey;
	}

	@Nonnull
	protected Gson getGson() {
		return gson;
	}

	@Nonnull
	protected RateLimiter getRateLimiter() {
		return rateLimiter;
	}

	@Nonnull
	protected Map<String, LongAdder> getCallFrequencyHistogramInternal() {
		return callFrequencyHistogram;
	}

	@Nonnull
	@Override
	public SortedMap<String, Long> getCallFrequencyHistogram() {
		SortedMap<String, Long> callFrequencyHistogram = new TreeMap<>((key1, key2) -> key2.compareTo(key1));

		for (Entry<String, LongAdder> entry : getCallFrequencyHistogramInternal().entrySet())
			callFrequencyHistogram.put(entry.getKey(), entry.getValue().longValue());

		return callFrequencyHistogram;
	}

	@Nonnull
	protected HttpClient getHttpClient() {
		return httpClient;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
