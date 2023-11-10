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

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.model.service.PresignedUpload;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PresignedUploadApiResponse {
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
	private final String expirationTimestampDescription;
	@Nonnull
	private final Map<String, String> httpHeaders;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PresignedUploadApiResponseFactory {
		@Nonnull
		PresignedUploadApiResponse create(@Nonnull PresignedUpload presignedUpload);
	}

	@AssistedInject
	public PresignedUploadApiResponse(@Nonnull Formatter formatter,
																		@Assisted @Nonnull PresignedUpload presignedUpload) {
		requireNonNull(formatter);
		requireNonNull(presignedUpload);

		this.httpMethod = presignedUpload.getHttpMethod();
		this.url = presignedUpload.getUrl();
		this.accessUrl = presignedUpload.getAccessUrl();
		this.contentType = presignedUpload.getContentType();
		this.expirationTimestamp = presignedUpload.getExpirationTimestamp();
		this.expirationTimestampDescription = formatter.formatTimestamp(presignedUpload.getExpirationTimestamp());
		this.httpHeaders = Collections.unmodifiableMap(presignedUpload.getHttpHeaders());
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
	public String getExpirationTimestampDescription() {
		return expirationTimestampDescription;
	}

	@Nonnull
	public Map<String, String> getHttpHeaders() {
		return httpHeaders;
	}
}
