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

import com.amazonaws.HttpMethod;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.lokalized.Strings;
import com.cobaltplatform.api.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

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
	private final AmazonS3 amazonS3;
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
		GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(getConfiguration().getAmazonS3BucketName(), key);

		for (Entry<String, String> entry : metadata.entrySet())
			generatePresignedUrlRequest.putCustomRequestHeader(format("x-amz-meta-%s", entry.getKey()), entry.getValue());

		HttpMethod httpMethod = HttpMethod.PUT;

		generatePresignedUrlRequest.setMethod(httpMethod);
		generatePresignedUrlRequest.setExpiration(Date.from(expirationTimestamp));
		generatePresignedUrlRequest.setContentType(contentType);

		// Always set public read flag since these images are not sensitive information
		generatePresignedUrlRequest.addRequestParameter(
				Headers.S3_CANNED_ACL,
				CannedAccessControlList.PublicRead.toString()
		);

		getLogger().debug("Generating presigned S3 upload URL for key '{}' and metadata {}...", key, metadata);

		String url = getAmazonS3().generatePresignedUrl(generatePresignedUrlRequest).toString();

		// For Localstack, the post-S3-upload Lambda will not be triggered if we have any query parameters, so strip them off.
		// This took a while to figure out...
		if (getConfiguration().getAmazonUseLocalstack())
			url = url.substring(0, url.indexOf("?"));

		return new PresignedUpload(httpMethod.name(), url, contentType, expirationTimestamp, generatePresignedUrlRequest.getCustomRequestHeaders());
	}

	@Nonnull
	public String createPresignedViewUrl(@Nonnull String bucket,
																			 @Nonnull String key) {
		requireNonNull(bucket);
		requireNonNull(key);

		GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucket, key);

		// TODO: figure out further restrictions...

		return getAmazonS3().generatePresignedUrl(generatePresignedUrlRequest).toString();
	}

	@Nonnull
	protected AmazonS3 createAmazonS3() {
		AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();

		if (getConfiguration().getAmazonUseLocalstack()) {
			builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(getConfiguration().getAmazonS3BaseUrl(), getConfiguration().getAmazonS3Region().getName()))
					.withCredentials(getConfiguration().getAmazonCredentialsProvider());
		} else {
			builder.withCredentials(getConfiguration().getAmazonCredentialsProvider())
					.withRegion(getConfiguration().getAmazonS3Region().getName());
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
	protected AmazonS3 getAmazonS3() {
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
