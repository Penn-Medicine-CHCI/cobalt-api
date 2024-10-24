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

package com.cobaltplatform.api.integration.ipstack;

import com.cobaltplatform.api.http.DefaultHttpClient;
import com.cobaltplatform.api.http.HttpClient;
import com.cobaltplatform.api.http.HttpMethod;
import com.cobaltplatform.api.http.HttpRequest;
import com.cobaltplatform.api.http.HttpResponse;
import com.cobaltplatform.api.util.WebUtility;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class DefaultIpstackClient implements IpstackClient {
	@Nonnull
	private static final String IPSTACK_API_BASE_URL;

	@Nonnull
	private final String accessKey;
	@Nonnull
	private final HttpClient httpClient;
	@Nonnull
	private final Gson gson;

	static {
		IPSTACK_API_BASE_URL = "https://api.ipstack.com";
	}

	public DefaultIpstackClient(@Nonnull String accessKey) {
		requireNonNull(accessKey);
		this.accessKey = accessKey;
		this.httpClient = new DefaultHttpClient("ipstack-client");
		this.gson = new GsonBuilder().disableHtmlEscaping().create();
	}

	@Nonnull
	@Override
	public IpstackStandardLookupResponse performStandardLookup(@Nonnull IpstackStandardLookupRequest request) {
		requireNonNull(request);

		Map<String, String> queryParameters = new HashMap<>(6);
		queryParameters.put("access_key", WebUtility.urlEncode(getAccessKey().trim()));
		queryParameters.put("output", "json");

		if (request.getFields().size() > 0)
			queryParameters.put("fields", request.getFields().stream()
					.map(field -> WebUtility.urlEncode(field.trim()))
					.collect(Collectors.joining(",")));

		if (request.getHostname())
			queryParameters.put("hostname", "1");

		if (request.getSecurity())
			queryParameters.put("security", "1");

		if (request.getLanguage().isPresent())
			queryParameters.put("language", WebUtility.urlEncode(request.getLanguage().get().trim()));

		List<String> queryParameterNameValuePairs = new ArrayList<>(queryParameters.size());

		for (Entry<String, String> entry : queryParameters.entrySet()) {
			String name = entry.getKey();
			String value = entry.getValue();
			queryParameterNameValuePairs.add(format("%s=%s", name, value));
		}

		String queryParametersAsString = queryParameterNameValuePairs.stream().collect(Collectors.joining("&"));
		String url = format("%s/%s?%s", IPSTACK_API_BASE_URL, request.getIpAddress().trim(), queryParametersAsString);

		HttpResponse httpResponse;

		try {
			httpResponse = getHttpClient().execute(new HttpRequest.Builder(HttpMethod.GET, url).build());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		String responseBody = new String(httpResponse.getBody().get(), StandardCharsets.UTF_8).trim();

		// TODO: support errors.  The HTTP status is 200 for them...
		// {
		//  "success": false,
		//  "error": {
		//    "code": 104,
		//    "type": "monthly_limit_reached",
		//    "info": "Your monthly API request volume has been reached. Please upgrade your plan."
		//  }
		// }

		if (httpResponse.getStatus() >= 400)
			throw new RuntimeException(format("Bad HTTP response (status %s). Response body was:\n%s", httpResponse.getStatus(), responseBody));

		// TODO: actually parse this out
		IpstackStandardLookupResponse response = new IpstackStandardLookupResponse();
		response.setRawJson(responseBody);

		return response;
	}

	@Nonnull
	protected String getAccessKey() {
		return this.accessKey;
	}

	@Nonnull
	protected HttpClient getHttpClient() {
		return this.httpClient;
	}

	@Nonnull
	protected Gson getGson() {
		return this.gson;
	}
}
