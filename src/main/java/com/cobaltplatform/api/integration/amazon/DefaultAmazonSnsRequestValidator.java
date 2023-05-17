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

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.http.DefaultHttpClient;
import com.cobaltplatform.api.http.HttpClient;
import com.cobaltplatform.api.http.HttpMethod;
import com.cobaltplatform.api.http.HttpRequest;
import com.cobaltplatform.api.http.HttpResponse;
import com.cobaltplatform.api.util.CryptoUtility;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
public class DefaultAmazonSnsRequestValidator implements AmazonSnsRequestValidator {
	@Nonnull
	private final Set<String> trustedSigningCertUriPrefixes;
	@Nonnull
	private final Map<AmazonSnsMessageType, Set<String>> signingFieldNamesByAmazonSnsMessageType;
	@Nonnull
	private final LoadingCache<URI, X509Certificate> signingCertByUriCache;
	@Nonnull
	private final HttpClient httpClient;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Logger logger;

	public DefaultAmazonSnsRequestValidator(@Nonnull Configuration configuration) {
		requireNonNull(configuration);

		this.configuration = configuration;
		this.httpClient = new DefaultHttpClient("amazon-sns-request-validator");
		this.logger = LoggerFactory.getLogger(getClass());

		this.trustedSigningCertUriPrefixes = Set.of(
				"https://sns.ap-south-1.amazonaws.com/",
				"https://sns.eu-south-1.amazonaws.com/",
				"https://sns.us-gov-east-1.amazonaws.com/",
				"https://sns.ca-central-1.amazonaws.com/",
				"https://sns.eu-central-1.amazonaws.com/",
				"https://sns.us-west-1.amazonaws.com/",
				"https://sns.us-west-2.amazonaws.com/",
				"https://sns.af-south-1.amazonaws.com/",
				"https://sns.eu-north-1.amazonaws.com/",
				"https://sns.eu-west-3.amazonaws.com/",
				"https://sns.eu-west-2.amazonaws.com/",
				"https://sns.eu-west-1.amazonaws.com/",
				"https://sns.ap-northeast-2.amazonaws.com/",
				"https://sns.ap-northeast-1.amazonaws.com/",
				"https://sns.me-south-1.amazonaws.com/",
				"https://sns.sa-east-1.amazonaws.com/",
				"https://sns.ap-east-1.amazonaws.com/",
				"https://sns.cn-north-1.amazonaws.com/",
				"https://sns.us-gov-west-1.amazonaws.com/",
				"https://sns.ap-southeast-1.amazonaws.com/",
				"https://sns.ap-southeast-2.amazonaws.com/",
				"https://sns.us-iso-east-1.amazonaws.com/",
				"https://sns.us-east-1.amazonaws.com/",
				"https://sns.us-east-2.amazonaws.com/",
				"https://sns.cn-northwest-1.amazonaws.com/",
				"https://sns.us-isob-east-1.amazonaws.com/",
				"https://sns.aws-global.amazonaws.com/",
				"https://sns.aws-cn-global.amazonaws.com/",
				"https://sns.aws-us-gov-global.amazonaws.com/",
				"https://sns.aws-iso-global.amazonaws.com/",
				"https://sns.aws-iso-b-global.amazonaws.com/"
		);

		// See https://docs.aws.amazon.com/sns/latest/dg/sns-verify-signature-of-message.html
		this.signingFieldNamesByAmazonSnsMessageType = Map.of(
				AmazonSnsMessageType.NOTIFICATION, Set.of("Message", "MessageId", "Subject", "Timestamp", "TopicArn", "Type"),
				AmazonSnsMessageType.SUBSCRIPTION_CONFIRMATION, Set.of("Message", "MessageId", "SubscribeURL", "Timestamp", "Token", "TopicArn", "Type"),
				AmazonSnsMessageType.UNSUBSCRIBE_CONFIRMATION, Set.of("Message", "MessageId", "SubscribeURL", "Timestamp", "Token", "TopicArn", "Type")
		);

		// We don't want to re-fetch the certificate every single time a webhook comes in - keep it in a cache keyed on URI
		this.signingCertByUriCache = Caffeine.newBuilder()
				.maximumSize(10)
				.refreshAfterWrite(Duration.ofMinutes(60 * 12))
				.expireAfterWrite(Duration.ofMinutes(60 * 24))
				.build(uri -> fetchSigningCertForUri(uri));
	}

	@Nonnull
	public Boolean validateRequest(@Nonnull AmazonSnsRequestBody amazonSnsRequestBody) {
		requireNonNull(amazonSnsRequestBody);

		// See https://docs.aws.amazon.com/sns/latest/dg/SendMessageToHttp.prepare.html
		// and https://docs.aws.amazon.com/sns/latest/dg/sns-verify-signature-of-message.html

		// Pick out the signing fields for the request type and sort them
		SortedMap<String, String> signingFieldValuesByFieldName = new TreeMap<>();
		Set<String> signingFieldNames = getSigningFieldNamesByAmazonSnsMessageType().get(amazonSnsRequestBody.getType());

		if (signingFieldNames == null)
			throw new IllegalStateException(format("Don't know how to determine signing fields for type '%s'",
					amazonSnsRequestBody.getType().name()));

		for (Entry<String, String> entry : amazonSnsRequestBody.getRequestBodyAsMap().entrySet())
			if (signingFieldNames.contains(entry.getKey()))
				signingFieldValuesByFieldName.put(entry.getKey(), entry.getValue());

		// With the signing fields sorted by key, join them into a newline-separated string
		String signableString = signingFieldValuesByFieldName.entrySet().stream()
				.map(entry -> format("%s\n%s", entry.getKey(), entry.getValue()))
				.collect(Collectors.joining("\n"));

		// Spec says each line must end with a newline...so make sure we add a final one here
		signableString = signableString + "\n";

		// Get the X509 certificate that Amazon SNS used to sign the message.
		X509Certificate signingCert = getSigningCertByUriCache().get(amazonSnsRequestBody.getSigningCertUrl());

		// Perform verification of the AWS-provided signature using the public key and
		// the signable string we created from elements of the request body
		try {
			Signature signature = signatureForSignatureVersion(amazonSnsRequestBody.getSignatureVersion());
			byte[] signatureToVerify = Base64.getDecoder().decode(amazonSnsRequestBody.getSignature());
			signature.initVerify(signingCert.getPublicKey());
			signature.update(signableString.getBytes());
			return signature.verify(signatureToVerify);
		} catch (Exception e) {
			throw new RuntimeException("Unable to process SNS signature", e);
		}
	}

	@Nonnull
	protected X509Certificate fetchSigningCertForUri(@Nonnull URI uri) {
		getLogger().info("Fetching signing cert for URI {}...", uri);

		// Prevent spoofing.
		// Certificate URLs should look like https://sns.us-east-1.amazonaws.com/SimpleNotificationService-xxx.pem
		String normalizedSigningCertUri = uri.toString().toLowerCase(Locale.US);
		boolean signingCertUriTrusted = false;

		for (String trustedSigningCertUriPrefix : getTrustedSigningCertUriPrefixes()) {
			if (normalizedSigningCertUri.startsWith(trustedSigningCertUriPrefix)) {
				signingCertUriTrusted = true;
				break;
			}
		}

		if (!signingCertUriTrusted)
			throw new IllegalStateException(format("Signing certificate URI is %s, which does not start with any known trusted prefixes: %s",
					normalizedSigningCertUri, getTrustedSigningCertUriPrefixes()));

		// The SigningCertURL value points to the location of the X509 certificate used to create the
		// digital signature for the message. Retrieve the certificate from this location.
		HttpRequest httpRequest = new HttpRequest.Builder(HttpMethod.GET, uri.toString()).build();
		String signingCertAsString;

		try {
			HttpResponse httpResponse = getHttpClient().execute(httpRequest);

			if (httpResponse.getStatus() >= 400)
				throw new IOException(format("Failed to fetch Amazon SNS X509 signing certificate - HTTP status was %d", httpResponse.getStatus()));

			signingCertAsString = new String(httpResponse.getBody().get(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		// Extract the public key from the certificate.
		// The public key from the certificate specified by SigningCertURL is used to verify the
		// authenticity and integrity of the message.
		return CryptoUtility.toX509Certificate(signingCertAsString);
	}

	@Nonnull
	protected Signature signatureForSignatureVersion(@Nonnull String signatureVersion) {
		requireNonNull(signatureVersion);

		String algorithm = null;

		if ("1".equals(signatureVersion))
			algorithm = "SHA1withRSA";
		else if ("2".equals(signatureVersion))
			algorithm = "SHA256withRSA";
		else
			throw new IllegalStateException(format("Not sure which hash function to pick for signature version '%s'", signatureVersion));

		try {
			return Signature.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(format("Runtime does not support algorithm '%s'", algorithm), e);
		}
	}

	@Nonnull
	protected Set<String> getTrustedSigningCertUriPrefixes() {
		return this.trustedSigningCertUriPrefixes;
	}

	@Nonnull
	protected Map<AmazonSnsMessageType, Set<String>> getSigningFieldNamesByAmazonSnsMessageType() {
		return this.signingFieldNamesByAmazonSnsMessageType;
	}

	@Nonnull
	protected LoadingCache<URI, X509Certificate> getSigningCertByUriCache() {
		return this.signingCertByUriCache;
	}

	@Nonnull
	protected HttpClient getHttpClient() {
		return this.httpClient;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
