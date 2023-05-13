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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Base64;
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
	private final Map<AmazonSnsMessageType, Set<String>> signingFieldNamesByAmazonSnsMessageType;
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

		// See https://docs.aws.amazon.com/sns/latest/dg/sns-verify-signature-of-message.html
		this.signingFieldNamesByAmazonSnsMessageType = Map.of(
				AmazonSnsMessageType.NOTIFICATION, Set.of("Message", "MessageId", "Subject", "Timestamp", "TopicArn", "Type"),
				AmazonSnsMessageType.SUBSCRIPTION_CONFIRMATION, Set.of("Message", "MessageId", "SubscribeURL", "Timestamp", "Token", "TopicArn", "Type"),
				AmazonSnsMessageType.UNSUBSCRIBE_CONFIRMATION, Set.of("Message", "MessageId", "SubscribeURL", "Timestamp", "Token", "TopicArn", "Type")
		);
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
		// The SigningCertURL value points to the location of the X509 certificate used to create the
		// digital signature for the message. Retrieve the certificate from this location.
		HttpRequest httpRequest = new HttpRequest.Builder(HttpMethod.GET, amazonSnsRequestBody.getSigningCertUrl().toString()).build();
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
		X509Certificate signingCert = CryptoUtility.toX509Certificate(signingCertAsString);

		getLogger().info("Amazon SNS signing certificate has issuer '{}'", signingCert.getIssuerX500Principal());

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
	protected Map<AmazonSnsMessageType, Set<String>> getSigningFieldNamesByAmazonSnsMessageType() {
		return this.signingFieldNamesByAmazonSnsMessageType;
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
