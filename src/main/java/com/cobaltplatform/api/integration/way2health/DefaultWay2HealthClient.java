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

package com.cobaltplatform.api.integration.way2health;

import com.cobaltplatform.api.http.DefaultHttpClient;
import com.cobaltplatform.api.http.HttpClient;
import com.cobaltplatform.api.http.HttpMethod;
import com.cobaltplatform.api.http.HttpRequest;
import com.cobaltplatform.api.http.HttpResponse;
import com.cobaltplatform.api.integration.way2health.model.entity.Incident;
import com.cobaltplatform.api.integration.way2health.model.entity.Way2HealthError;
import com.cobaltplatform.api.integration.way2health.model.request.GetIncidentRequest;
import com.cobaltplatform.api.integration.way2health.model.request.GetIncidentsRequest;
import com.cobaltplatform.api.integration.way2health.model.request.UpdateIncidentRequest;
import com.cobaltplatform.api.integration.way2health.model.request.UpdateIncidentsRequest;
import com.cobaltplatform.api.integration.way2health.model.response.ListResponse;
import com.cobaltplatform.api.integration.way2health.model.response.ObjectResponse;
import com.cobaltplatform.api.integration.way2health.model.response.PagedResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class DefaultWay2HealthClient implements Way2HealthClient {
	@Nonnull
	private final String baseUrl;
	@Nonnull
	private final String accessToken;
	@Nonnull
	private final Gson gson;
	@Nonnull
	private final HttpClient httpClient;
	@Nonnull
	private final Logger logger;

	public DefaultWay2HealthClient(@Nonnull Way2HealthEnvironment way2HealthEnvironment,
																 @Nonnull String accessToken) {
		requireNonNull(way2HealthEnvironment);
		requireNonNull(accessToken);

		this.baseUrl = baseUrlForEnvironment(way2HealthEnvironment);
		this.accessToken = accessToken;
		this.gson = Way2HealthGsonSupport.sharedGson();
		this.httpClient = createHttpClient();
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@Override
	public ObjectResponse<Incident> getIncident(@Nonnull GetIncidentRequest request) throws Way2HealthException {
		requireNonNull(request);

		Map<String, Object> queryParameters = new HashMap<>();

		if (request.getIncidentId() == null)
			throw new Way2HealthException("Incident ID is required");

		if (request.getInclude() != null && request.getInclude().size() > 0)
			queryParameters.put("include", request.getInclude().stream().collect(Collectors.joining(",")));

		return makeApiCall(HttpMethod.GET, format("/incidents/%s", request.getIncidentId()), queryParameters, (responseBody) -> {
			ObjectResponse<Incident> response = getGson().fromJson(responseBody, new TypeToken<ObjectResponse<Incident>>() {
			}.getType());

			response.setRawResponseBody(responseBody);

			return response;
		});
	}

	@Nonnull
	@Override
	public PagedResponse<Incident> getIncidents(@Nonnull GetIncidentsRequest request) throws Way2HealthException {
		requireNonNull(request);

		Map<String, Object> queryParameters = new HashMap<>();

		if (request.getStudyId() != null)
			queryParameters.put("study_id", request.getStudyId());
		if (request.getPage() != null)
			queryParameters.put("page", request.getPage());
		if (request.getPerPage() != null)
			queryParameters.put("per_page", request.getPerPage());
		if (request.getInclude() != null && request.getInclude().size() > 0)
			queryParameters.put("include", request.getInclude().stream().collect(Collectors.joining(",")));
		if (request.getStatus() != null)
			queryParameters.put("status", request.getStatus());
		if (request.getType() != null)
			queryParameters.put("type", request.getType());
		if (request.getParticipantId() != null)
			queryParameters.put("participant.id", request.getParticipantId());
		if (request.getParticipantMrn() != null)
			queryParameters.put("participant.mrn", request.getParticipantMrn());
		if (request.getParticipantMrnType() != null)
			queryParameters.put("participant.mrn_type", request.getParticipantMrnType());
		if (request.getOrderBy() != null)
			queryParameters.put("order_by", request.getOrderBy());
		if (request.getGroupBy() != null)
			queryParameters.put("group_by", request.getGroupBy());

		return makeApiCall(HttpMethod.GET, "/incidents", queryParameters, (responseBody) -> {
			PagedResponse<Incident> response = getGson().fromJson(responseBody, new TypeToken<PagedResponse<Incident>>() {
			}.getType());

			response.setRawResponseBody(responseBody);

			return response;
		});
	}

	@Nonnull
	@Override
	public PagedResponse<Incident> getIncidents(@Nonnull String pageLink) throws Way2HealthException {
		requireNonNull(pageLink);

		Map<String, Object> queryParameters = new HashMap<>();

		try {
			HttpUrl httpUrl = HttpUrl.parse(pageLink);

			for (String queryParameterName : httpUrl.queryParameterNames())
				queryParameters.put(queryParameterName, httpUrl.queryParameter(queryParameterName));
		} catch (Exception e) {
			throw new Way2HealthException(format("Unable to parse incident page link '%s'", pageLink), e);
		}

		return makeApiCall(HttpMethod.GET, "/incidents", queryParameters, (responseBody) -> {
			PagedResponse<Incident> response = getGson().fromJson(responseBody, new TypeToken<PagedResponse<Incident>>() {
			}.getType());

			response.setRawResponseBody(responseBody);

			return response;
		});
	}

	@Nonnull
	@Override
	public ObjectResponse<Incident> updateIncident(@Nonnull UpdateIncidentRequest request) throws Way2HealthException {
		requireNonNull(request);

		Long incidentId = request.getIncidentId();

		if (incidentId == null)
			throw new Way2HealthException("You must provide an incident ID to update an incident.");

		// Guard against null values if caller uses double-brace initialization, which Gson does not support
		List<Map<String, Object>> patchOperations = request.getPatchOperations().stream()
				.map(patchOperation -> {
					Map<String, Object> patchOperationAsMap = new HashMap<>();
					patchOperationAsMap.put("path", patchOperation.getPath());
					patchOperationAsMap.put("op", patchOperation.getOp());
					patchOperationAsMap.put("value", patchOperation.getValue());

					return patchOperationAsMap;
				})
				.collect(Collectors.toList());

		String requestBody = getGson().toJson(patchOperations);

		return makeApiCall(HttpMethod.PATCH, format("/incidents/%s", incidentId), Collections.emptyMap(), requestBody, (responseBody) -> {
			ObjectResponse<Incident> response = getGson().fromJson(responseBody, new TypeToken<ObjectResponse<Incident>>() {
			}.getType());

			response.setRawResponseBody(responseBody);

			return response;
		});
	}

	@Nonnull
	@Override
	public ListResponse<Incident> updateIncidents(@Nonnull UpdateIncidentsRequest request) throws Way2HealthException {
		requireNonNull(request);

		String id = trimToNull(request.getId());

		if (id == null)
			throw new Way2HealthException("You must provide an incident ID filter parameter to update incident[s].");

		// Guard against null values if caller uses double-brace initialization, which Gson does not support
		List<Map<String, Object>> patchOperations = request.getPatchOperations().stream()
				.map(patchOperation -> {
					Map<String, Object> patchOperationAsMap = new HashMap<>();
					patchOperationAsMap.put("path", patchOperation.getPath());
					patchOperationAsMap.put("op", patchOperation.getOp());
					patchOperationAsMap.put("value", patchOperation.getValue());

					return patchOperationAsMap;
				})
				.collect(Collectors.toList());

		String requestBody = getGson().toJson(patchOperations);

		return makeApiCall(HttpMethod.PATCH, "/incidents", Map.of("id", id), requestBody, (responseBody) -> {
			ListResponse<Incident> response = getGson().fromJson(responseBody, new TypeToken<ListResponse<Incident>>() {
			}.getType());

			response.setRawResponseBody(responseBody);

			return response;
		});
	}

	@Nonnull
	protected <T> T makeApiCall(@Nonnull HttpMethod httpMethod,
															@Nonnull String relativeUrl,
															@Nullable Map<String, Object> queryParameters,
															@Nonnull Function<String, T> responseBodyMapper) throws Way2HealthException {
		return makeApiCall(httpMethod, relativeUrl, queryParameters, null, responseBodyMapper);
	}

	@Nonnull
	protected <T> T makeApiCall(@Nonnull HttpMethod httpMethod,
															@Nonnull String relativeUrl,
															@Nullable Map<String, Object> queryParameters,
															@Nullable String requestBody,
															@Nonnull Function<String, T> responseBodyMapper) throws Way2HealthException {
		requireNonNull(httpMethod);
		requireNonNull(relativeUrl);
		requireNonNull(responseBodyMapper);

		if (!relativeUrl.startsWith("/"))
			relativeUrl = format("/%s", relativeUrl);

		String url = format("%s%s", getBaseUrl(), relativeUrl);

		if (queryParameters == null)
			queryParameters = Collections.emptyMap();

		HttpRequest.Builder httpRequestBuilder = new HttpRequest.Builder(httpMethod, url)
				.headers(new HashMap<String, Object>() {{
					put("Authorization", format("Bearer %s", getAccessToken()));
				}});

		if (queryParameters.size() > 0)
			httpRequestBuilder.queryParameters(queryParameters);

		if (requestBody != null)
			httpRequestBuilder.body(requestBody);

		if (httpMethod == HttpMethod.POST
				|| httpMethod == HttpMethod.PUT
				|| httpMethod == HttpMethod.PATCH
				|| httpMethod == HttpMethod.DELETE)
			httpRequestBuilder.contentType("application/json; charset=utf-8");

		HttpRequest httpRequest = httpRequestBuilder.build();

		String queryParametersDescription = queryParameters.size() == 0 ? "[none]" : queryParameters.toString();
		String requestBodyDescription = requestBody == null ? "[none]" : requestBody;

		try {
			HttpResponse httpResponse = getHttpClient().execute(httpRequest);
			byte[] rawResponseBody = httpResponse.getBody().orElse(null);
			String responseBody = rawResponseBody == null ? null : new String(rawResponseBody, StandardCharsets.UTF_8);

			if (httpResponse.getStatus() > 299)
				throw new Way2HealthException(format("Bad HTTP response %d for Way2Health API endpoint %s %s " +
								"with query params %s and request body %s. Response body was\n%s", httpResponse.getStatus(),
						httpRequest.getHttpMethod().name(), httpRequest.getUrl(), queryParametersDescription,
						requestBodyDescription, responseBody));

			// Way2Health will normally return an HTTP 200 OK for errors and it's up to us to examine the response body
			// to see if it has the shape of an error.
			//
			// For example, an HTTP 200 might look like this:
			//
			// {
			//  "code": 401,
			//  "resource_id": null,
			//  "payload": null,
			//  "errors": [
			//    "You are not logged in"
			//  ],
			//  "field_errors": []
			// }

			Way2HealthError way2HealthError = null;
			boolean detectedError = false;

			try {
				way2HealthError = getGson().fromJson(responseBody, Way2HealthError.class);

				if (way2HealthError.getErrors() != null && way2HealthError.getFieldErrors() != null && way2HealthError.getCode() != null)
					detectedError = true;
			} catch (Exception ignored) {
				// If we can't parse response into a Way2HealthError, that's OK...it's probably not an error in that case :)
			}

			if (detectedError) {
				StringBuilder parsedErrorMessage = new StringBuilder();

				if (way2HealthError.getErrors() != null)
					parsedErrorMessage.append(way2HealthError.getErrors().stream()
							.collect(Collectors.joining(", ")));

				if (way2HealthError.getFieldErrors() != null)
					parsedErrorMessage.append(way2HealthError.getFieldErrors().stream()
							.map(fieldError -> format("[%s: %s (original value %s)]", fieldError.getField(), fieldError.getMessage(), fieldError.getOriginalValue()))
							.collect(Collectors.joining(", ")));

				String message = format("Received error code %d and message '%s' for Way2Health API endpoint %s %s " +
								"with query params %s and request body %s. Response body was\n%s", way2HealthError.getCode(),
						parsedErrorMessage, httpRequest.getHttpMethod().name(), httpRequest.getUrl(),
						queryParametersDescription, requestBodyDescription, responseBody);

				throw new Way2HealthResponseException(message, way2HealthError, responseBody);
			}

			try {
				return responseBodyMapper.apply(responseBody);
			} catch (Exception e) {
				throw new Way2HealthException(format("Unable to parse JSON for Way2Health API endpoint %s %s " +
								"with query params %s  and request body %s. Response body was\n%s", httpRequest.getHttpMethod().name(),
						httpRequest.getUrl(), queryParametersDescription, requestBodyDescription, responseBody));
			}
		} catch (IOException e) {
			throw new Way2HealthException(format("Unable to call Way2Health API endpoint %s %s with query params %s " +
							"and request body %s", httpRequest.getHttpMethod().name(), httpRequest.getUrl(), queryParametersDescription,
					requestBodyDescription), e);
		}
	}

	@Nonnull
	protected String baseUrlForEnvironment(@Nonnull Way2HealthEnvironment way2HealthEnvironment) {
		requireNonNull(way2HealthEnvironment);

		if (way2HealthEnvironment == Way2HealthEnvironment.PRODUCTION)
			return "https://app.waytohealth.org/api/v2";

		throw new UnsupportedOperationException(format("Not sure what to do with %s.%s",
				Way2HealthEnvironment.class.getSimpleName(), way2HealthEnvironment.name()));
	}

	@Nonnull
	protected HttpClient createHttpClient() {
		return new DefaultHttpClient("com.cobaltplatform.api.integration.way2health.Way2HealthAPI");
	}

	@Nonnull
	protected String getBaseUrl() {
		return baseUrl;
	}

	@Nonnull
	protected String getAccessToken() {
		return accessToken;
	}

	@Nonnull
	protected HttpClient getHttpClient() {
		return httpClient;
	}

	@Nonnull
	protected Gson getGson() {
		return gson;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
