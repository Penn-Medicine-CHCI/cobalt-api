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

package com.cobaltplatform.api.util;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.http.HttpMethod;
import com.lokalized.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class UploadManager {
	@Nonnull
	private final S3Presigner amazonS3;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public UploadManager(@Nonnull Configuration configuration,
											 @Nonnull Strings strings) {
		requireNonNull(configuration);
		requireNonNull(strings);

		this.configuration = configuration;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
		this.amazonS3 = createAmazonS3();
	}

	@Nonnull
	public PresignedUpload createPresignedUpload(@Nonnull String key,
																							 @Nonnull String contentType) {
		requireNonNull(key);
		requireNonNull(contentType);

		return createPresignedUpload(key, contentType, Collections.emptyMap());
	}

	@Nonnull
	public PresignedUpload createPresignedUpload(@Nonnull String key,
																							 @Nonnull String contentType,
																							 @Nullable Map<String, String> metadata) {
		requireNonNull(key);
		requireNonNull(contentType);

		if (metadata == null)
			metadata = Collections.emptyMap();

		Instant expirationTimestamp = Instant.now().plus(getConfiguration().getAmazonS3PresignedUploadExpirationInMinutes(), MINUTES);
		PutObjectRequest.Builder putObjectRequestBuilder = PutObjectRequest.builder()
				.bucket(getConfiguration().getAmazonS3BucketName())
				.key(key)
				.contentType(contentType)
				.expires(expirationTimestamp)
				// Always set public read flag since these images are not sensitive information
				.acl(ObjectCannedACL.PUBLIC_READ)
				.metadata(metadata.entrySet().stream().collect(Collectors.toMap(e -> format("x-amz-meta-%s", e.getKey()), Map.Entry::getValue)));

		PutObjectRequest putObjectRequest = putObjectRequestBuilder.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
				.signatureDuration(Duration.ofMinutes(getConfiguration().getAmazonS3PresignedUploadExpirationInMinutes()))
				.putObjectRequest(putObjectRequest)
				.build();

		getLogger().debug("Generating presigned S3 upload URL for key '{}' and metadata {}...", key, metadata);
		PresignedPutObjectRequest presignedRequest = amazonS3.presignPutObject(presignRequest);
		String url = presignedRequest.url().toString();
		//
		// For AWS SDK 1.x , For Localstack, the post-S3-upload Lambda will not be triggered if we have any query parameters, so strip them off.
		// This took a while to figure out...
		//
		// For AWS SDK 2.x, query parameters must remain for presigned urls, not sure about lambda triggers
		//if (getConfiguration().getAmazonUseLocalstack())
		//  url = url.substring(0, url.indexOf("?"));

		return new PresignedUpload(HttpMethod.PUT.name(), url, contentType, expirationTimestamp, putObjectRequest.metadata());
	}

	public String createPresignedViewUrl(@Nonnull String bucket,
																			 @Nonnull String key) {
		requireNonNull(bucket);
		requireNonNull(key);

		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.build();

		GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
				.signatureDuration(Duration.ofMinutes(getConfiguration().getAmazonS3PresignedUploadExpirationInMinutes()))
				.getObjectRequest(getObjectRequest)
				.build();

		PresignedGetObjectRequest presignedGetObjectRequest = amazonS3.presignGetObject(getObjectPresignRequest);
		return presignedGetObjectRequest.url().toString();
	}

	@Nonnull
	protected S3Presigner createAmazonS3() {
		S3Presigner.Builder builder = S3Presigner.builder()
				.region(getConfiguration().getAmazonS3Region());

		if (getConfiguration().getAmazonUseLocalstack()) {
			builder.endpointOverride(URI.create(getConfiguration().getAmazonS3BaseUrl()));
		}

		return builder.build();
	}

	@Immutable
	public static class PresignedUpload {
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

	@Nonnull
	protected S3Presigner getAmazonS3() {
		return amazonS3;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
