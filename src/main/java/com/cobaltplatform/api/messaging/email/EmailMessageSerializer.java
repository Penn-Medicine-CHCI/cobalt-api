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

package com.cobaltplatform.api.messaging.email;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.messaging.MessageSerializer;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class EmailMessageSerializer implements MessageSerializer<EmailMessage> {
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final AmazonS3 amazonS3;
	@Nonnull
	private final Logger logger;

	@Inject
	public EmailMessageSerializer(@Nonnull JsonMapper jsonMapper,
																@Nonnull Configuration configuration) {
		requireNonNull(jsonMapper);
		requireNonNull(jsonMapper);

		this.jsonMapper = jsonMapper;
		this.configuration = configuration;
		this.amazonS3 = createAmazonS3();
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@Override
	public String serializeMessage(@Nonnull EmailMessage emailMessage) {
		requireNonNull(emailMessage);

		SerializableEmailMessage serializableEmailMessage = new SerializableEmailMessage();
		serializableEmailMessage.setMessageId(emailMessage.getMessageId());
		serializableEmailMessage.setBccAddresses(emailMessage.getBccAddresses());
		serializableEmailMessage.setCcAddresses(emailMessage.getCcAddresses());
		serializableEmailMessage.setFromAddress(emailMessage.getFromAddress().orElse(null));
		serializableEmailMessage.setMessageContext(emailMessage.getMessageContext());
		serializableEmailMessage.setMessageTemplate(emailMessage.getMessageTemplate());
		serializableEmailMessage.setLocale(emailMessage.getLocale());
		serializableEmailMessage.setToAddresses(emailMessage.getToAddresses());
		serializableEmailMessage.setEmailAttachments(emailMessage.getEmailAttachments().stream()
				.map(emailAttachment -> {
					SerializableEmailAttachment serializableEmailAttachment = new SerializableEmailAttachment();
					serializableEmailAttachment.setContentType(emailAttachment.getContentType());
					serializableEmailAttachment.setKey(storeEmailAttachmentData(emailMessage, emailAttachment));
					serializableEmailAttachment.setFilename(emailAttachment.getFilename());

					return serializableEmailAttachment;
				})
				.collect(Collectors.toList()));

		return getJsonMapper().toJson(serializableEmailMessage);
	}

	@Nonnull
	@Override
	public EmailMessage deserializeMessage(@Nonnull String serializedMessage) {
		requireNonNull(serializedMessage);

		SerializableEmailMessage serializableEmailMessage = getJsonMapper().fromJson(serializedMessage, SerializableEmailMessage.class);

		return new EmailMessage.Builder(serializableEmailMessage.getMessageId(), serializableEmailMessage.getMessageTemplate(), serializableEmailMessage.getLocale())
				.bccAddresses(serializableEmailMessage.getBccAddresses())
				.ccAddresses(serializableEmailMessage.getCcAddresses())
				.fromAddress(serializableEmailMessage.getFromAddress())
				.messageContext(serializableEmailMessage.getMessageContext())
				.toAddresses(serializableEmailMessage.getToAddresses())
				.emailAttachments(serializableEmailMessage.getEmailAttachments().stream()
						.map(serializableEmailAttachment ->
								new EmailAttachment(serializableEmailAttachment.getFilename(),
										serializableEmailAttachment.getContentType(),
										fetchEmailAttachmentDataForKey(serializableEmailAttachment.getKey())))
						.collect(Collectors.toList()))
				.build();
	}

	@Nonnull
	protected AmazonS3 createAmazonS3() {
		AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();

		if (getConfiguration().getAmazonUseLocalstack()) {
			builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(getConfiguration().getAmazonS3BaseUrl(), getConfiguration().getAmazonSesRegion().getName()))
					.withCredentials(getConfiguration().getAmazonCredentialsProvider());
		} else {
			builder.withCredentials(getConfiguration().getAmazonCredentialsProvider())
					.withRegion(getConfiguration().getAmazonSesRegion().getName());
		}

		return builder.build();
	}

	@Nonnull
	protected String storeEmailAttachmentData(@Nonnull EmailMessage emailMessage,
																						@Nonnull EmailAttachment emailAttachment) {
		requireNonNull(emailMessage);
		requireNonNull(emailAttachment);

		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentType(emailAttachment.getContentType());

		String key = determineEmailAttachmentDataKey(emailMessage.getMessageId(), emailAttachment.getFilename());

		getLogger().debug("Uploading email attachment data to {}...", key);

		try (ByteArrayInputStream inputStream = new ByteArrayInputStream(emailAttachment.getData())) {
			PutObjectRequest putObjectRequest = new PutObjectRequest(getConfiguration().getAmazonS3BucketName(), key, inputStream, objectMetadata);
			getAmazonS3().putObject(putObjectRequest);
			getLogger().debug("Email attachment data successfully uploaded to {}",
					format("%s/%s/%s", getConfiguration().getAmazonS3BaseUrl(), getConfiguration().getAmazonS3BucketName(), key));
			return key;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	protected String determineEmailAttachmentDataKey(@Nonnull UUID messageId,
																									 @Nonnull String emailAttachmentFilename) {
		requireNonNull(messageId);
		requireNonNull(emailAttachmentFilename);

		return format("email-attachments/%s/%s", messageId, emailAttachmentFilename);
	}

	@Nonnull
	protected byte[] fetchEmailAttachmentDataForKey(@Nonnull String key) {
		requireNonNull(key);

		getLogger().debug("Fetching email attachment data from {}...", key);

		S3Object s3Object = getAmazonS3().getObject(getConfiguration().getAmazonS3BucketName(), key);

		try (S3ObjectInputStream s3is = s3Object.getObjectContent()) {
			byte[] data = IOUtils.toByteArray(s3is);
			getLogger().debug("Email attachment data successfully fetched from {}.", key);
			return data;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return jsonMapper;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected AmazonS3 getAmazonS3() {
		return amazonS3;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
