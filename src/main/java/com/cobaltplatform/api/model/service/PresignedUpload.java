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

package com.cobaltplatform.api.model.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class PresignedUpload {
	@Nonnull
	private final String httpMethod;
	@Nonnull
	private final String url;
	@Nonnull
	private final String accessUrl;
	@Nonnull
	private final String contentType;
	@Nonnull
	private final Instant expirationTimestamp;
	@Nonnull
	private final Map<String, String> httpHeaders;

	public PresignedUpload(@Nonnull String httpMethod,
												 @Nonnull String url,
												 @Nonnull String contentType,
												 @Nonnull Instant expirationTimestamp,
												 @Nullable Map<String, String> httpHeaders) {
		requireNonNull(httpMethod);
		requireNonNull(url);
		requireNonNull(contentType);
		requireNonNull(expirationTimestamp);

		this.httpMethod = httpMethod;
		this.url = url;
		this.accessUrl = url.indexOf("?") == -1 ? url : url.substring(0, url.indexOf("?")); // Rip off query params
		this.contentType = contentType;
		this.expirationTimestamp = expirationTimestamp;
		this.httpHeaders = httpHeaders == null ? Collections.emptyMap() : Collections.unmodifiableMap(httpHeaders);
	}

	@Override
	public String toString() {
		return format("%s{httpMethod=%s, url=%s, accessUrl=%s, contentType=%s, expirationTimestamp=%s, httpHeaders=%s}",
				getClass().getSimpleName(), getHttpMethod(), getUrl(), getAccessUrl(), getContentType(), getExpirationTimestamp(), getHttpHeaders());
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;

		if (other == null || !getClass().equals(other.getClass()))
			return false;

		PresignedUpload otherPresignedUpload = (PresignedUpload) other;

		return Objects.equals(this.getHttpMethod(), otherPresignedUpload.getHttpMethod())
				&& Objects.equals(this.getUrl(), otherPresignedUpload.getUrl())
				&& Objects.equals(this.getAccessUrl(), otherPresignedUpload.getAccessUrl())
				&& Objects.equals(this.getContentType(), otherPresignedUpload.getContentType())
				&& Objects.equals(this.getExpirationTimestamp(), otherPresignedUpload.getExpirationTimestamp())
				&& Objects.equals(this.getHttpHeaders(), otherPresignedUpload.getHttpHeaders());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getHttpMethod(), getUrl(), getAccessUrl(), getContentType(), getExpirationTimestamp(), getHttpHeaders());
	}

	@Nonnull
	public String getHttpMethod() {
		return httpMethod;
	}

	@Nonnull
	public String getUrl() {
		return url;
	}

	@Nonnull
	public String getAccessUrl() {
		return accessUrl;
	}

	@Nonnull
	public String getContentType() {
		return contentType;
	}

	@Nonnull
	public Instant getExpirationTimestamp() {
		return expirationTimestamp;
	}

	@Nonnull
	public Map<String, String> getHttpHeaders() {
		return httpHeaders;
	}
}