package com.cobaltplatform.ic.backend.util;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;
import com.cobaltplatform.ic.backend.config.AwsConfig;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class SecretsManager {
	@Nonnull
	private static final Gson GSON;

	@Nonnull
	private final AWSSecretsManager awsSecretsManager;
	@Nonnull
	private final Logger logger;

	static {
		GSON = new Gson();
	}

	public SecretsManager(@Nonnull AwsConfig awsConfig) {
		requireNonNull(awsConfig);

		this.awsSecretsManager = createAwsSecretsManager(awsConfig);
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<String> fetchSecretString(@Nonnull String name) {
		requireNonNull(name);

		GetSecretValueResult result = getSecretValueResult(name);

		String string = result.getSecretString();

		if (string == null)
			return Optional.empty();

		// AWS returns keys in form {"<name>": "<value>"} so we have to parse the value out of the JSON
		Map json = getGson().fromJson(string, Map.class);
		String value = (String) json.get(name);

		return Optional.ofNullable(value);
	}

	@Nonnull
	public Optional<String> fetchSecretStringAndBase64Decode(@Nonnull String name) {
		requireNonNull(name);

		String string = fetchSecretString(name).orElse(null);

		// Strip newlines, if present
		string = string.replaceAll("\\R", "");

		return Optional.of(new String(Base64.getDecoder().decode(string), StandardCharsets.UTF_8));
	}

	@Nonnull
	public Optional<ByteBuffer> fetchSecretBinary(@Nonnull String name) {
		requireNonNull(name);

		GetSecretValueResult result = getSecretValueResult(name);
		ByteBuffer byteBuffer = result.getSecretBinary();

		if (byteBuffer == null)
			return Optional.empty();

		return Optional.of(byteBuffer);
	}

	@Nonnull
	public Optional<String> fetchSecretBinaryAsString(@Nonnull String name) {
		requireNonNull(name);

		ByteBuffer byteBuffer = fetchSecretBinary(name).orElse(null);

		if (byteBuffer == null)
			return Optional.empty();

		return Optional.of(StandardCharsets.UTF_8.decode(byteBuffer).toString());
	}

	@Nonnull
	protected GetSecretValueResult getSecretValueResult(@Nonnull String name) {
		requireNonNull(name);

		GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
				.withSecretId(name)
				.withVersionStage("AWSCURRENT");

		try {
			return getAwsSecretsManager().getSecretValue(getSecretValueRequest);
		} catch (ResourceNotFoundException e) {
			throw new RuntimeException(format("Unable to find SecretManager secret named '%s'", name), e);
		}
	}

	@Nonnull
	protected AWSSecretsManager createAwsSecretsManager(@Nonnull AwsConfig awsConfig) {
		requireNonNull(awsConfig);

		AWSSecretsManagerClientBuilder builder = AWSSecretsManagerClientBuilder.standard();

		if (awsConfig.getUseLocalstack()) {
			builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(format("http://localhost:%d", awsConfig.getLocalstackPort().get()), awsConfig.getRegion().getName()))
					.withCredentials(awsConfig.getAwsCredentialsProvider());
		} else {
			builder.withCredentials(awsConfig.getAwsCredentialsProvider())
					.withRegion(awsConfig.getRegion().getName());
		}

		return builder.build();
	}

	@Nonnull
	protected Gson getGson() {
		return GSON;
	}

	@Nonnull
	protected AWSSecretsManager getAwsSecretsManager() {
		return awsSecretsManager;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
