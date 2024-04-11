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
import com.cobaltplatform.api.model.service.PresignedUpload;
import com.lokalized.Strings;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
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
	private final S3Client s3Client;
	@Nonnull
	private final S3Presigner s3Presigner;
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
		this.s3Client = createS3Client();
		this.s3Presigner = createS3Presigner();
	}

	@Nonnull
	public PresignedUpload createPresignedUpload(@Nonnull String key,
																							 @Nonnull String contentType,
																							 @Nonnull Boolean publicRead) {
		requireNonNull(key);
		requireNonNull(contentType);
		requireNonNull(publicRead);

		return createPresignedUpload(key, contentType, publicRead, null);
	}

	@Nonnull
	public PresignedUpload createPresignedUpload(@Nonnull String key,
																							 @Nonnull String contentType,
																							 @Nonnull Boolean publicRead,
																							 @Nullable Map<String, String> metadata) {
		requireNonNull(key);
		requireNonNull(contentType);
		requireNonNull(publicRead);

		if (metadata == null)
			metadata = Map.of();

		Instant expirationTimestamp = Instant.now().plus(getConfiguration().getAmazonS3PresignedUploadExpirationInMinutes(), MINUTES);

		PutObjectRequest.Builder putObjectRequestBuilder = PutObjectRequest.builder()
				.bucket(getConfiguration().getAmazonS3BucketName())
				.key(key)
				.contentType(contentType)
				.metadata(metadata.entrySet().stream().collect(Collectors.toMap(e -> format("x-amz-meta-%s", e.getKey()), Map.Entry::getValue)));

		if (publicRead)
			putObjectRequestBuilder.acl(ObjectCannedACL.PUBLIC_READ);

		PutObjectRequest putObjectRequest = putObjectRequestBuilder.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
				// TODO: expiration should be configurable
				.signatureDuration(Duration.ofMinutes(getConfiguration().getAmazonS3PresignedUploadExpirationInMinutes()))
				.putObjectRequest(putObjectRequest)
				.build();

		getLogger().debug("Generating presigned S3 upload URL for key '{}' and metadata {}...", key, metadata);

		PresignedPutObjectRequest presignedRequest = getS3Presigner().presignPutObject(presignRequest);

		String url = presignedRequest.url().toString();

		Map<String, String> finalMetadata = new HashMap<>(putObjectRequest.metadata());

		if (publicRead)
			finalMetadata.put("x-amz-acl", "public-read"); // If you do not include this header for ObjectCannedACL.PUBLIC_READ, you will get a 403

		return new PresignedUpload(HttpMethod.PUT.name(), url, contentType, expirationTimestamp, finalMetadata);
	}

	public String createPresignedViewUrl(@Nonnull String bucket,
																			 @Nonnull String key) {
		requireNonNull(bucket);
		requireNonNull(key);

		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.build();

		// TODO: expiration should be configurable
		GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
				.signatureDuration(Duration.ofMinutes(getConfiguration().getAmazonS3PresignedUploadExpirationInMinutes()))
				.getObjectRequest(getObjectRequest)
				.build();

		PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(getObjectPresignRequest);
		return presignedGetObjectRequest.url().toString();
	}

	@Nonnull
	public void downloadFileLocatedByStorageKey(@Nonnull String storageKey,
																							@Nonnull BufferedOutputStream bufferedOutputStream) {
		requireNonNull(storageKey);
		requireNonNull(bufferedOutputStream);

		GetObjectRequest objectRequest = GetObjectRequest.builder()
				.bucket(getConfiguration().getAmazonS3BucketName())
				.key(storageKey)
				.build();

		try (ResponseInputStream<GetObjectResponse> s3Object = getS3Client().getObject(objectRequest)) {
			IOUtils.copy(s3Object, bufferedOutputStream);
		} catch (NoSuchKeyException e) {
			throw new ValidationException(getStrings().get("No file exists for storage key '{{storageKey}}'.",
					Map.of("storageKey", storageKey)));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	protected S3Client createS3Client() {
		S3ClientBuilder s3ClientBuilder = S3Client.builder().region(getConfiguration().getAmazonS3Region());

		if (getConfiguration().getAmazonUseLocalstack())
			s3ClientBuilder.endpointOverride(URI.create(getConfiguration().getAmazonS3BaseUrl()));

		return s3ClientBuilder.build();
	}

	@Nonnull
	protected S3Presigner createS3Presigner() {
		S3Presigner.Builder builder = S3Presigner.builder()
				.region(getConfiguration().getAmazonS3Region());

		if (getConfiguration().getAmazonUseLocalstack())
			builder.endpointOverride(URI.create(getConfiguration().getAmazonS3BaseUrl()));

		return builder.build();
	}

	@Nonnull
	protected S3Client getS3Client() {
		return this.s3Client;
	}

	@Nonnull
	protected S3Presigner getS3Presigner() {
		return s3Presigner;
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
