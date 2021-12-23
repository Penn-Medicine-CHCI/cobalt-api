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

import com.cobaltplatform.api.integration.way2health.model.entity.Incident;
import com.cobaltplatform.api.integration.way2health.model.request.FindIncidentsRequest;
import com.cobaltplatform.api.integration.way2health.model.request.PatchIncidentsRequest;
import com.cobaltplatform.api.integration.way2health.model.response.BasicResponse;
import com.cobaltplatform.api.integration.way2health.model.response.PagedResponse;
import com.google.gson.Gson;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class DefaultWay2HealthClient implements Way2HealthClient {
	@Nonnull
	private static final MediaType JSON_MEDIA_TYPE;

	@Nonnull
	private final String baseUrl;
	@Nonnull
	private final String accessToken;
	@Nonnull
	private final Gson gson;
	@Nonnull
	private final OkHttpClient okHttpClient;
	@Nonnull
	private final Logger logger;

	static {
		JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
	}

	public DefaultWay2HealthClient(@Nonnull Way2HealthEnvironment way2HealthEnvironment,
																 @Nonnull String accessToken) {
		requireNonNull(way2HealthEnvironment);
		requireNonNull(accessToken);

		this.baseUrl = baseUrlForEnvironment(way2HealthEnvironment);
		this.accessToken = accessToken;
		this.gson = Way2HealthGsonSupport.sharedGson();
		this.okHttpClient = createOkHttpClient();
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@Override
	public PagedResponse<Incident> findIncidents(@Nonnull FindIncidentsRequest request) throws Way2HealthException {
		requireNonNull(request);
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public PagedResponse<Incident> findIncidents(@Nonnull String pageLink) throws Way2HealthException {
		requireNonNull(pageLink);
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public BasicResponse<Incident> patchIncidents(@Nonnull PatchIncidentsRequest request) throws Way2HealthException {
		requireNonNull(request);
		throw new UnsupportedOperationException();
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
	protected OkHttpClient createOkHttpClient() {
		return new OkHttpClient.Builder()
				.connectTimeout(10, TimeUnit.SECONDS)
				.writeTimeout(20, TimeUnit.SECONDS)
				.readTimeout(20, TimeUnit.SECONDS)
				.build();
	}

	@Nonnull
	protected Gson getGson() {
		return gson;
	}

	@Nonnull
	protected OkHttpClient getOkHttpClient() {
		return okHttpClient;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
