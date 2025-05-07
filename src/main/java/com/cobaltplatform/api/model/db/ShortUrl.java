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

package com.cobaltplatform.api.model.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pyranid.DatabaseColumn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.WebUtility.urlEncode;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ShortUrl {
	@Nonnull
	private static final Gson GSON;

	static {
		GSON = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping()
				.create();
	}

	@Nullable
	private Long shortUrlId;
	@Nullable
	private String shortCode;
	@Nullable
	private String baseUrl;
	@Nullable
	@DatabaseColumn("query_parameters")
	private String queryParametersAsString;
	@Nullable
	private Map<String, String> queryParameters;
	@Nullable
	private String fragment;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nonnull
	public static Gson getGson() {
		return GSON;
	}

	@Nonnull
	public String getFullUrl() {
		if (trimToNull(getBaseUrl()) == null)
			throw new IllegalStateException("Cannot compute full URL because base URL is not present");

		StringBuilder fullUrl = new StringBuilder();

		fullUrl.append(getBaseUrl());

		if (getQueryParameters() != null && getQueryParameters().size() > 0) {
			// TODO
			fullUrl.append("?");

			List<String> nameValuePairs = new ArrayList<>(getQueryParameters().size());

			for (Entry<String, String> entry : getQueryParameters().entrySet()) {
				String name = urlEncode(entry.getKey());
				String value = urlEncode(entry.getValue());
				nameValuePairs.add(format("%s=%s", name, value));
			}

			fullUrl.append(nameValuePairs.stream().collect(Collectors.joining("&")));
		}

		if (getFragment() != null)
			fullUrl.append(format("#%s", urlEncode(getFragment())));

		return fullUrl.toString();
	}

	@Nullable
	public String getQueryParametersAsString() {
		return this.queryParametersAsString;
	}

	public void setQueryParametersAsString(@Nullable String queryParametersAsString) {
		this.queryParametersAsString = queryParametersAsString;

		String queryParameters = trimToNull(queryParametersAsString);
		this.queryParameters = queryParameters == null ? Map.of() : getGson().fromJson(queryParameters, new TypeToken<Map<String, String>>() {
		}.getType());
	}

	@Nonnull
	public Map<String, String> getQueryParameters() {
		return this.queryParameters;
	}

	@Nullable
	public Long getShortUrlId() {
		return this.shortUrlId;
	}

	public void setShortUrlId(@Nullable Long shortUrlId) {
		this.shortUrlId = shortUrlId;
	}

	@Nullable
	public String getShortCode() {
		return this.shortCode;
	}

	public void setShortCode(@Nullable String shortCode) {
		this.shortCode = shortCode;
	}

	@Nullable
	public String getBaseUrl() {
		return this.baseUrl;
	}

	public void setBaseUrl(@Nullable String baseUrl) {
		this.baseUrl = baseUrl;
	}

	@Nullable
	public String getFragment() {
		return this.fragment;
	}

	public void setFragment(@Nullable String fragment) {
		this.fragment = fragment;
	}

	@Nullable
	public Instant getCreated() {
		return this.created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}
