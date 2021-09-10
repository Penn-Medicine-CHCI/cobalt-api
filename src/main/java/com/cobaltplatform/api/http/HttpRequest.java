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

package com.cobaltplatform.api.http;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify LLC.
 */
@Immutable
public class HttpRequest {
	@Nonnull
	private final HttpMethod httpMethod;
	@Nonnull
	private final String url;
	@Nullable
	private final String contentType;
	@Nonnull
	private Map<String, Object> headers;
	@Nonnull
	private Map<String, Object> queryParameters;
	@Nullable
	private byte[] body;

	private HttpRequest(@Nonnull HttpMethod httpMethod, @Nonnull String url, @Nullable String contentType,
											@Nullable Map<String, Object> headers, Map<String, Object> queryParameters,
											@Nullable byte[] body) {
		requireNonNull(httpMethod);
		requireNonNull(url);

		if (body != null && body.length == 0)
			body = null;

		contentType = trimToNull(contentType);

		if (httpMethod == HttpMethod.GET && body != null)
			throw new IllegalArgumentException(format("It is not legal for a %s method to have a body.",
					httpMethod.name()));

		if (httpMethod == HttpMethod.GET && contentType != null)
			throw new IllegalArgumentException(format("It is not legal for a %s method to have a content type.",
					httpMethod.name()));

		if (body != null && contentType == null)
			throw new IllegalArgumentException("If you supply a request body, you must also supply a content type.");

		if (url.contains("?") && queryParameters != null && queryParameters.size() > 0)
			throw new IllegalArgumentException("Your URL cannot already include a query string if you specify query parameters.");

		this.httpMethod = httpMethod;
		this.url = url;
		this.contentType = contentType;
		this.headers = headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(headers);
		this.queryParameters = queryParameters == null ? Collections.emptyMap() : Collections.unmodifiableMap(queryParameters);
		this.body = body;
	}

	@Nonnull
	public HttpMethod getHttpMethod() {
		return httpMethod;
	}

	@Nonnull
	public String getUrl() {
		return url;
	}

	@Nonnull
	public Optional<String> getContentType() {
		return Optional.ofNullable(contentType);
	}

	@Nonnull
	public Map<String, Object> getHeaders() {
		return headers;
	}

	@Nonnull
	public Map<String, Object> getQueryParameters() {
		return queryParameters;
	}

	@Nonnull
	public Optional<byte[]> getBody() {
		return Optional.ofNullable(body);
	}

	@NotThreadSafe
	public static class Builder {
		@Nonnull
		private final HttpMethod httpMethod;
		@Nonnull
		private final String url;
		@Nullable
		private String contentType;
		@Nullable
		private Map<String, Object> headers;
		@Nullable
		private Map<String, Object> queryParameters;
		@Nullable
		private byte[] body;

		public Builder(@Nonnull HttpMethod httpMethod, @Nonnull String url) {
			requireNonNull(httpMethod);
			requireNonNull(url);

			this.httpMethod = httpMethod;
			this.url = url;
		}

		@Nonnull
		public Builder contentType(@Nullable String contentType) {
			this.contentType = contentType;
			return this;
		}

		@Nonnull
		public Builder headers(@Nullable Map<String, Object> headers) {
			this.headers = headers;
			return this;
		}

		@Nonnull
		public Builder queryParameters(@Nullable Map<String, Object> queryParameters) {
			this.queryParameters = queryParameters;
			return this;
		}

		@Nonnull
		public Builder body(@Nullable byte[] body) {
			this.body = body;
			return this;
		}

		@Nonnull
		public Builder body(@Nullable String body) {
			this.body = body == null ? null : body.getBytes(UTF_8);
			return this;
		}

		@Nonnull
		public HttpRequest build() {
			return new HttpRequest(this.httpMethod, this.url, this.contentType, this.headers, this.queryParameters, this.body);
		}
	}
}