package com.cobaltplatform.api.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify LLC.
 */
public class AwsSecretManagerClient {
	@Nonnull
	private static final String LOCALSTACK_CREDENTIALS = "fake";

	@Nonnull
	private final Boolean usingLocalstack;
	@Nonnull
	private final SecretsManagerClient secretsManagerClient;
	@Nonnull
	private final Gson gson;
	@Nonnull
	private final Logger logger;

	@Nonnull
	public static AwsSecretManagerClient forRegion(@Nonnull Region region) {
		requireNonNull(region);
		return new AwsSecretManagerClient(region, null);
	}

	@Nonnull
	public static AwsSecretManagerClient forLocalstackRegion(@Nonnull Region region,
																													 @Nullable Integer localstackPort) {
		requireNonNull(region);
		requireNonNull(localstackPort);

		return new AwsSecretManagerClient(region, localstackPort);
	}

	protected AwsSecretManagerClient(@Nonnull Region region,
																	 @Nullable Integer localstackPort) {
		requireNonNull(region);

		this.usingLocalstack = localstackPort != null;
		this.gson = new Gson();
		this.logger = LoggerFactory.getLogger(getClass());

		SecretsManagerClientBuilder builder = SecretsManagerClient.builder().region(region);

		if (usingLocalstack) {
			if (localstackPort == null)
				throw new ConfigurationException("Illegal localstack configuration, localstack port is required");

			builder.credentialsProvider(StaticCredentialsProvider.create(
					AwsBasicCredentials.create(LOCALSTACK_CREDENTIALS, LOCALSTACK_CREDENTIALS)));
			builder.endpointOverride(URI.create(format("http://localhost:%d", localstackPort)));
		}

		this.secretsManagerClient = builder.build();
	}

	@Nonnull
	public String getSecretString(@Nonnull String name) {
		requireNonNull(name);

		GetSecretValueResponse response = getSecret(name);
		if (response.secretString() != null) {
			return response.secretString();
		} else {
			throw new ConfigurationException(format("Unable to find string secret for %s", name));
		}
	}

	@Nonnull
	public Map<String, String> getSecretMap(@Nonnull String name) {
		requireNonNull(name);

		GetSecretValueResponse response = getSecret(name);
		if (response.secretString() == null) {
			throw new ConfigurationException(format("Unable to find string secret for %s", name));
		}
		return getGson().fromJson(response.secretString(), new TypeToken<HashMap<String, String>>() {
		}.getType());
	}

	@Nonnull
	public byte[] getSecretBinary(@Nonnull String name) {
		requireNonNull(name);

		GetSecretValueResponse response = getSecret(name);

		if (response.secretBinary() != null) {
			if (getUsingLocalstack()) {
				// Localstack appears to base64-encode binary data
				return Base64.getDecoder().decode(response.secretBinary().asUtf8String());
			} else {
				return response.secretBinary().asByteArray();
			}
		} else {
			throw new ConfigurationException(format("Unable to find binary secret for %s", name));
		}
	}

	@Nonnull
	protected GetSecretValueResponse getSecret(@Nonnull String name) {
		requireNonNull(name);

		logger.trace("Fetching secret {}", name);

		try {
			GetSecretValueRequest request = GetSecretValueRequest.builder().secretId(name).build();
			return getSecretsManagerClient().getSecretValue(request);
		} catch (ResourceNotFoundException ex) {
			logger.error("Secret with name {} did not exist", name);
			throw ex;
		}
	}

	@Nonnull
	protected Boolean getUsingLocalstack() {
		return this.usingLocalstack;
	}

	@Nonnull
	protected SecretsManagerClient getSecretsManagerClient() {
		return this.secretsManagerClient;
	}

	@Nonnull
	protected Gson getGson() {
		return this.gson;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
