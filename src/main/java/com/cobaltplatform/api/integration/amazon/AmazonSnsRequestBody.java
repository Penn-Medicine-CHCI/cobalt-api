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

package com.cobaltplatform.api.integration.amazon;

import com.google.gson.Gson;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AmazonSnsRequestBody {
	@Nonnull
	private static final Gson GSON;

	static {
		GSON = new Gson();
	}

	@Nonnull
	private final Map<String, String> requestBodyAsMap;
	@Nonnull
	private final Map<String, Object> messageAsMap;
	@Nonnull
	private final Map<String, Object> mailAsMap;
	@Nullable
	private final Map<String, Object> bounceAsMap;

	@Nonnull
	private final String messageId;
	@Nonnull
	private final AmazonSnsMessageType type;
	@Nullable
	private final String token;
	@Nullable
	private final String topicArn;
	@Nullable
	private final String subject;
	@Nonnull
	private final String message;
	@Nonnull
	private final Instant timestamp;
	@Nonnull
	private final String signatureVersion;
	@Nonnull
	private final String signature;
	@Nonnull
	private final URI signingCertUrl;
	@Nullable
	private final URI subscribeUrl;
	@Nullable
	private final URI unsubscribeUrl;

	public AmazonSnsRequestBody(@Nonnull String requestBody) {
		requireNonNull(requestBody);

		Map<String, String> requestBodyAsMap = getGson().fromJson(requestBody, Map.class);
		this.requestBodyAsMap = Collections.unmodifiableMap(requestBodyAsMap);

		this.messageId = requestBodyAsMap.get("MessageId");
		this.type = AmazonSnsMessageType.fromType(requestBodyAsMap.get("Type")).get();
		this.token = requestBodyAsMap.get("Token");
		this.topicArn = requestBodyAsMap.get("TopicArn");
		this.subject = requestBodyAsMap.get("Subject");
		// This is a JSON string...
		this.message = requestBodyAsMap.get("Message");

		// ...so provide it as a map for simplified access.
		Map<String, Object> messageAsMap = getGson().fromJson(this.message, Map.class);
		this.messageAsMap = Collections.unmodifiableMap(messageAsMap);

		// There will always be a "mail" object.
		Map<String, Object> mailAsMap = (Map<String, Object>) messageAsMap.get("mail");
		this.mailAsMap = Collections.unmodifiableMap(mailAsMap);

		if ("Bounce".equals(messageAsMap.get("eventType"))) {
			Map<String, Object> bounceAsMap = (Map<String, Object>) messageAsMap.get("bounce");
			this.bounceAsMap = Collections.unmodifiableMap(bounceAsMap);
		} else {
			this.bounceAsMap = null;
		}

		// e.g. 2023-05-12T18:08:47.059Z
		this.timestamp = Instant.parse(requestBodyAsMap.get("Timestamp"));
		this.signatureVersion = requestBodyAsMap.get("SignatureVersion");
		this.signature = requestBodyAsMap.get("Signature");

		try {
			String subscribeUrl = requestBodyAsMap.get("SubscribeURL");
			this.subscribeUrl = subscribeUrl == null ? null : new URI(subscribeUrl);

			String signingCertUrl = requestBodyAsMap.get("SigningCertURL");
			this.signingCertUrl = signingCertUrl == null ? null : new URI(signingCertUrl);

			String unsubscribeUrl = requestBodyAsMap.get("UnsubscribeURL");
			this.unsubscribeUrl = unsubscribeUrl == null ? null : new URI(unsubscribeUrl);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	@Nonnull
	public String toString() {
		return format("%s{requestBodyAsMap=%s}", getClass().getSimpleName(), getRequestBodyAsMap());
	}

	@Nonnull
	protected Gson getGson() {
		return GSON;
	}

	@Nonnull
	public Map<String, String> getRequestBodyAsMap() {
		return this.requestBodyAsMap;
	}

	@Nonnull
	public String getMessageId() {
		return this.messageId;
	}

	@Nonnull
	public AmazonSnsMessageType getType() {
		return this.type;
	}

	@Nonnull
	public Optional<String> getToken() {
		return Optional.ofNullable(this.token);
	}

	@Nonnull
	public Optional<String> getTopicArn() {
		return Optional.ofNullable(this.topicArn);
	}

	@Nonnull
	public Optional<String> getSubject() {
		return Optional.ofNullable(this.subject);
	}

	@Nonnull
	public String getMessage() {
		return this.message;
	}

	@Nonnull
	public Map<String, Object> getMessageAsMap() {
		return this.messageAsMap;
	}

	@Nonnull
	public Map<String, Object> getMailAsMap() {
		return this.mailAsMap;
	}

	@Nonnull
	public Optional<Map<String, Object>> getBounceAsMap() {
		return Optional.ofNullable(this.bounceAsMap);
	}

	@Nonnull
	public Instant getTimestamp() {
		return this.timestamp;
	}

	@Nonnull
	public String getSignatureVersion() {
		return this.signatureVersion;
	}

	@Nonnull
	public String getSignature() {
		return this.signature;
	}

	@Nonnull
	public URI getSigningCertUrl() {
		return this.signingCertUrl;
	}

	@Nonnull
	public Optional<URI> getSubscribeUrl() {
		return Optional.ofNullable(this.subscribeUrl);
	}

	@Nonnull
	public Optional<URI> getUnsubscribeUrl() {
		return Optional.ofNullable(this.unsubscribeUrl);
	}
}
