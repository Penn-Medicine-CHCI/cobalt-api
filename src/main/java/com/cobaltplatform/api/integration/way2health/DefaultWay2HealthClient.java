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
import com.cobaltplatform.api.integration.way2health.model.request.FindIncidentsRequest;
import com.cobaltplatform.api.integration.way2health.model.request.PatchIncidentsRequest;
import com.cobaltplatform.api.integration.way2health.model.response.BasicResponse;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

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
	public PagedResponse<Incident> findIncidents(@Nonnull FindIncidentsRequest request) throws Way2HealthException {
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

		return makeGetApiCall("/incidents", queryParameters, (responseBody) -> {
			PagedResponse<Incident> response = getGson().fromJson(responseBody, new TypeToken<PagedResponse<Incident>>() {
			}.getType());

			response.setRawResponseBody(responseBody);

			return response;
		});
	}

	@Nonnull
	@Override
	public PagedResponse<Incident> findIncidents(@Nonnull String pageLink) throws Way2HealthException {
		requireNonNull(pageLink);

		Map<String, Object> queryParameters = new HashMap<>();

		try {
			HttpUrl httpUrl = HttpUrl.parse(pageLink);

			for (String queryParameterName : httpUrl.queryParameterNames())
				queryParameters.put(queryParameterName, httpUrl.queryParameter(queryParameterName));
		} catch (Exception e) {
			throw new Way2HealthException(format("Unable to parse incident page link '%s'", pageLink), e);
		}
		return makeGetApiCall("/incidents", queryParameters, (responseBody) -> {
			PagedResponse<Incident> response = getGson().fromJson(responseBody, new TypeToken<PagedResponse<Incident>>() {
			}.getType());

			response.setRawResponseBody(responseBody);

			return response;
		});
	}

	@Nonnull
	@Override
	public BasicResponse<Incident> patchIncidents(@Nonnull PatchIncidentsRequest request) throws Way2HealthException {
		requireNonNull(request);
		throw new UnsupportedOperationException();
	}

	@Nonnull
	protected <T> T makeGetApiCall(@Nonnull String relativeUrl,
																 @Nonnull Function<String, T> responseBodyMapper) throws Way2HealthException {
		return makeGetApiCall(relativeUrl, null, responseBodyMapper);
	}

	@Nonnull
	protected <T> T makeGetApiCall(@Nonnull String relativeUrl,
																 @Nullable Map<String, Object> queryParameters,
																 @Nonnull Function<String, T> responseBodyMapper) throws Way2HealthException {
		return makeApiCall(HttpMethod.GET, relativeUrl, queryParameters, null, responseBodyMapper);
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

		if (httpMethod == HttpMethod.POST)
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
