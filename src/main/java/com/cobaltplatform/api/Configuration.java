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

package com.cobaltplatform.api;


import com.cobaltplatform.api.http.DefaultHttpClient;
import com.cobaltplatform.api.http.HttpMethod;
import com.cobaltplatform.api.http.HttpRequest;
import com.cobaltplatform.api.http.HttpResponse;
import com.cobaltplatform.api.integration.way2health.Way2HealthEnvironment;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.security.SamlIdentityProvider;
import com.cobaltplatform.api.model.security.SigningCredentials;
import com.cobaltplatform.api.util.AwsSecretConfigurationManager;
import com.cobaltplatform.api.util.AwsSecretManagerClient;
import com.cobaltplatform.api.util.CryptoUtility;
import com.cobaltplatform.api.util.DeploymentTarget;
import com.cobaltplatform.api.util.GitUtility;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.JsonMapper.MappingFormat;
import com.cobaltplatform.api.util.MavenUtility;
import com.cobaltplatform.api.util.SecretConfigurationManager;
import com.cobaltplatform.api.util.SensitiveDataStorageLocation;
import com.google.common.base.CaseFormat;
import com.google.common.base.Splitter;
import com.soklet.converter.ValueConversionException;
import com.soklet.converter.ValueConverter;
import com.soklet.converter.ValueConverterRegistry;
import com.soklet.util.PropertiesFileReader;
import com.soklet.web.server.ServerLauncher.StoppingStrategy;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.inject.Singleton;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.SecretConfigurationManager.SECRETS_PREFIX;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
@Singleton
public class Configuration {
	@Nonnull
	private static final String ENV_ENV_VARIABLE_NAME;
	@Nonnull
	private static final String PORT_ENV_VARIABLE_NAME;
	@Nonnull
	private static final String DEFAULT_ENV;
	@Nonnull
	private static final Integer DEFAULT_PORT;

	@Nonnull
	private final ValueConverterRegistry valueConverterRegistry;
	@Nonnull
	private final PropertiesFileReader propertiesFileReader;
	@Nonnull
	private final Logger logger;

	@Nonnull
	private final String environment;
	@Nonnull
	private final Locale defaultLocale;
	@Nonnull
	private final ZoneId defaultTimeZone;
	@Nonnull
	private final String host;
	@Nonnull
	private final Integer port;
	@Nonnull
	private final String baseUrl;
	@Nonnull
	private final String ipAddress;
	@Nonnull
	private final String applicationVersion;
	@Nonnull
	private final Instant buildTimestamp;
	@Nonnull
	private final String gitCommitHash;
	@Nonnull
	private final String gitBranch;
	@Nonnull
	private final Instant deploymentTimestamp;
	@Nonnull
	private final StoppingStrategy serverStoppingStrategy;
	@Nonnull
	private final MappingFormat jsonMappingFormat;
	@Nonnull
	private final DeploymentTarget deploymentTarget;
	@Nonnull
	private final SensitiveDataStorageLocation sensitiveDataStorageLocation;
	@Nonnull
	private final Boolean shouldDisplayStackTraces;
	@Nonnull
	private final Boolean shouldCacheHandlebarsTemplates;
	@Nonnull
	private final Boolean shouldSendRealEmailMessages;
	@Nonnull
	private final Boolean shouldSendRealPushMessages;
	@Nonnull
	private final Boolean shouldSendRealSmsMessages;
	@Nonnull
	private final Boolean shouldSendRealCallMessages;
	@Nonnull
	private final Boolean shouldSendErrorReports;
	@Nonnull
	private final Boolean shouldEnableDebuggingHeaders;
	@Nonnull
	private final Boolean shouldUseRealAuthentication;
	@Nonnull
	private final Boolean shouldUseRealAcuity;
	@Nonnull
	private final Boolean shouldPollAcuity;
	@Nonnull
	private final Boolean shouldUseRealEpic;
	@Nonnull
	private final Boolean shouldPollEpic;
	@Nonnull
	private final Boolean shouldUseRealBluejeans;
	@Nonnull
	private final Boolean shouldPollBluejeans;
	@Nonnull
	private final Boolean shouldUseRealWay2Health;
	@Nonnull
	private final Boolean shouldPollWay2Health;
	@Nonnull
	private final Boolean shouldEnableCacheDebugging;
	@Nonnull
	private final Boolean shouldEnableIcDebugging;
	@Nonnull
	private final String corsEnabledDomains;
	@Nonnull
	private final String emailDefaultFromAddress;
	@Nonnull
	private final Boolean downForMaintenance;
	@Nonnull
	private final String nodeIdentifier;
	@Nonnull
	private final Boolean shouldApplyDatabaseUpdates;
	@Nonnull
	private final Boolean shouldIncludeTestDataInIcReports;
	@Nonnull
	private final String jdbcUrl;
	@Nonnull
	private final String jdbcUsername;
	@Nonnull
	private final String jdbcPassword;
	@Nonnull
	private final Integer jdbcMaximumPoolSize;
	@Nonnull
	private final String jdbcReadReplicaUrl;
	@Nonnull
	private final String jdbcReadReplicaUsername;
	@Nonnull
	private final String jdbcReadReplicaPassword;
	@Nonnull
	private final Integer jdbcReadReplicaMaximumPoolSize;
	@Nullable
	private final String amazonAwsSecretsManagerContext;
	@Nonnull
	private final String amazonEc2RoleName;
	@Nonnull
	private final Boolean amazonUseLocalstack;
	@Nonnull
	private final Integer amazonLocalstackPort;
	@Nonnull
	private final String amazonLambdaCallbackBaseUrl;
	@Nonnull
	private final Region amazonSesRegion;
	@Nullable
	private final String amazonSesConfigurationSetName;
	@Nonnull
	private final Region amazonS3Region;
	@Nonnull
	private final String amazonS3BucketName;
	@Nonnull
	private final Integer amazonS3PresignedUploadExpirationInMinutes;
	@Nonnull
	private final String amazonS3BaseUrl;
	@Nonnull
	private final String redisHost;
	@Nonnull
	private final Integer redisPort;
	@Nonnull
	private final String sentryDsn;
	@Nonnull
	private final String bluejeansApiEndpoint;
	@Nonnull
	private final String bluejeansClientKey;
	@Nonnull
	private final String bluejeansSecretKey;
	@Nonnull
	private final Integer bluejeansDefaultUserId;
	@Nonnull
	private final String acuityUserId;
	@Nonnull
	private final String acuityApiKey;
	@Nonnull
	private final Long acuityVideoconferenceFormFieldId;
	@Nonnull
	private final String way2HealthAccessToken;
	@Nonnull
	private final Way2HealthEnvironment way2HealthEnvironment;

	@Nonnull
	private final String epicNonProdKeyId;
	@Nonnull
	private final String epicProdKeyId;
	@Nonnull
	private final String epicCurrentEnvironmentKeyId;

	@Nonnull
	private final SigningCredentials signingCredentials;
	@Nonnull
	private final SigningCredentials epicNonProdSigningCredentials;
	@Nonnull
	private final SigningCredentials epicProdSigningCredentials;
	@Nonnull
	private final SigningCredentials epicCurrentEnvironmentSigningCredentials;
	@Nonnull
	private final SigningCredentials microsoftSigningCredentials;
	@Nonnull
	private final String tinymceApiKey;

	@Nonnull
	private final Map<SamlIdentityProvider, Map<String, Object>> samlSettingsByIdentityProvider;

	@Nullable
	private final SecretConfigurationManager secretConfigurationManager;
	@Nullable
	private final AwsSecretManagerClient secretManagerClient;

	@Nonnull
	private final Boolean shouldRunDataSync;

	@Nonnull
	private final String dataSyncRemoteDb;
	@Nonnull
	private final Long dataSyncIntervalInSeconds;

	static {
		ENV_ENV_VARIABLE_NAME = "COBALT_API_ENV";
		PORT_ENV_VARIABLE_NAME = "COBALT_API_PORT";

		DEFAULT_ENV = "local";
		DEFAULT_PORT = 8080;
	}

	@Nonnull
	public static String determineEnvironment() {
		return environmentValueFor(ENV_ENV_VARIABLE_NAME).isPresent() ? environmentValueFor(ENV_ENV_VARIABLE_NAME).get() : DEFAULT_ENV;
	}

	public Configuration() {
		this(determineEnvironment());
	}

	public Configuration(@Nonnull String environment) {
		requireNonNull(environment);

		this.logger = LoggerFactory.getLogger(getClass());
		this.valueConverterRegistry = new ValueConverterRegistry();
		this.buildTimestamp = determineBuildTimestamp();
		this.applicationVersion = determineApplicationVersion();
		this.gitCommitHash = determineGitCommitHash();
		this.gitBranch = determineGitBranch();
		this.deploymentTimestamp = Instant.now();
		this.ipAddress = determineIpAddress(true).orElse(null);
		this.propertiesFileReader = new PropertiesFileReader(Paths.get(format("config/%s/app.properties", environment)));

		Integer port = DEFAULT_PORT;

		try {
			port = Integer.parseInt(environmentValueFor(PORT_ENV_VARIABLE_NAME).orElse(null));
		} catch (Exception e) {
			// Ignored, use default if we don't have a valid value
		}

		// If base URL exists, use it.  Otherwise set a default one from host and port
		Optional<String> baseUrlFromPropertiesFile = propertiesFileReader.optionalValueFor("com.cobaltplatform.api.baseUrl", String.class);
		String baseUrl;

		if (baseUrlFromPropertiesFile.isPresent()) {
			baseUrl = baseUrlFromPropertiesFile.get();
		} else {
			baseUrl = format("http://%s", getIpAddress().orElse("localhost"));

			if (port != 80)
				baseUrl = format("%s:%s", baseUrl, port);
		}

		// Default locale and timezone are deliberately hardcoded and should not be driven by a configuration file.
		// They are in alignment with the database configuration.
		this.defaultLocale = new Locale.Builder().setLanguage("en").setRegion("US").build();
		this.defaultTimeZone = ZoneId.of("UTC");

		this.environment = environment;
		this.host = "0.0.0.0";
		this.port = port;
		this.baseUrl = baseUrl;

		this.amazonUseLocalstack = valueFor("com.cobaltplatform.api.amazon.useLocalstack", Boolean.class);
		this.amazonLocalstackPort = valueFor("com.cobaltplatform.api.amazon.localstackPort", Integer.class);
		this.sensitiveDataStorageLocation = valueFor("com.cobaltplatform.api.sensitiveDataStorageLocation", SensitiveDataStorageLocation.class);

		// Only load secrets manager stuff if we're configured to use it
		if (this.sensitiveDataStorageLocation == SensitiveDataStorageLocation.AMAZON_SECRETS_MANAGER) {
			Region amazonSecretsRegion = Region.of(propertiesFileReader.valueFor("com.cobaltplatform.api.amazon.secrets.region", String.class));

			if (this.amazonUseLocalstack)
				this.secretManagerClient = AwsSecretManagerClient.forLocalstackRegion(amazonSecretsRegion, this.amazonLocalstackPort);
			else
				this.secretManagerClient = AwsSecretManagerClient.forRegion(amazonSecretsRegion);

			this.amazonAwsSecretsManagerContext = valueFor("com.cobaltplatform.api.amazon.secrets.context", String.class);
			this.secretConfigurationManager = new AwsSecretConfigurationManager(this.secretManagerClient, amazonAwsSecretsManagerContext);
		} else {
			this.amazonAwsSecretsManagerContext = null;
			this.secretConfigurationManager = null;
			this.secretManagerClient = null;
		}

		this.serverStoppingStrategy = valueFor("com.cobaltplatform.api.serverStoppingStrategy", StoppingStrategy.class);
		this.jsonMappingFormat = valueFor("com.cobaltplatform.api.jsonMappingFormat", MappingFormat.class);
		this.deploymentTarget = valueFor("com.cobaltplatform.api.deploymentTarget", DeploymentTarget.class);
		this.nodeIdentifier = determineNodeIdentifierForDeploymentTarget(getDeploymentTarget());
		this.shouldDisplayStackTraces = valueFor("com.cobaltplatform.api.shouldDisplayStackTraces", Boolean.class);
		this.shouldCacheHandlebarsTemplates = valueFor("com.cobaltplatform.api.shouldCacheHandlebarsTemplates", Boolean.class);
		this.shouldSendRealEmailMessages = valueFor("com.cobaltplatform.api.shouldSendRealEmailMessages", Boolean.class);
		this.shouldSendRealPushMessages = valueFor("com.cobaltplatform.api.shouldSendRealPushMessages", Boolean.class);
		this.shouldSendRealSmsMessages = valueFor("com.cobaltplatform.api.shouldSendRealSmsMessages", Boolean.class);
		this.shouldSendRealCallMessages = valueFor("com.cobaltplatform.api.shouldSendRealCallMessages", Boolean.class);
		this.shouldSendErrorReports = valueFor("com.cobaltplatform.api.shouldSendErrorReports", Boolean.class);
		this.shouldEnableDebuggingHeaders = valueFor("com.cobaltplatform.api.shouldEnableDebuggingHeaders", Boolean.class);
		this.shouldUseRealAuthentication = valueFor("com.cobaltplatform.api.shouldUseRealAuthentication", Boolean.class);
		this.shouldUseRealAcuity = valueFor("com.cobaltplatform.api.shouldUseRealAcuity", Boolean.class);
		this.shouldPollAcuity = valueFor("com.cobaltplatform.api.shouldPollAcuity", Boolean.class);
		this.shouldUseRealEpic = valueFor("com.cobaltplatform.api.shouldUseRealEpic", Boolean.class);
		this.shouldPollEpic = valueFor("com.cobaltplatform.api.shouldPollEpic", Boolean.class);
		this.shouldUseRealBluejeans = valueFor("com.cobaltplatform.api.shouldUseRealBluejeans", Boolean.class);
		this.shouldPollBluejeans = valueFor("com.cobaltplatform.api.shouldPollBluejeans", Boolean.class);
		this.shouldUseRealWay2Health = valueFor("com.cobaltplatform.api.shouldUseRealWay2Health", Boolean.class);
		this.shouldPollWay2Health = valueFor("com.cobaltplatform.api.shouldPollWay2Health", Boolean.class);
		this.shouldEnableCacheDebugging = valueFor("com.cobaltplatform.api.shouldEnableCacheDebugging", Boolean.class);
		this.shouldEnableIcDebugging = valueFor("com.cobaltplatform.api.shouldEnableIcDebugging", Boolean.class);
		this.corsEnabledDomains = valueFor("com.cobaltplatform.api.corsEnabledDomains", String.class);
		this.emailDefaultFromAddress = valueFor("com.cobaltplatform.api.emailDefaultFromAddress", String.class);
		this.downForMaintenance = valueFor("com.cobaltplatform.api.downForMaintenance", Boolean.class);
		this.shouldApplyDatabaseUpdates = valueFor("com.cobaltplatform.api.shouldApplyDatabaseUpdates", Boolean.class);

		this.jdbcUrl = valueFor("com.cobaltplatform.api.jdbc.url", String.class);
		this.jdbcUsername = valueFor("com.cobaltplatform.api.jdbc.username", String.class);
		this.jdbcPassword = valueFor("com.cobaltplatform.api.jdbc.password", String.class);
		this.jdbcMaximumPoolSize = valueFor("com.cobaltplatform.api.jdbc.maximumPoolSize", Integer.class);

		this.jdbcReadReplicaUrl = valueFor("com.cobaltplatform.api.jdbc.readReplicaUrl", String.class);
		this.jdbcReadReplicaUsername = valueFor("com.cobaltplatform.api.jdbc.readReplicaUsername", String.class);
		this.jdbcReadReplicaPassword = valueFor("com.cobaltplatform.api.jdbc.readReplicaPassword", String.class);
		this.jdbcReadReplicaMaximumPoolSize = valueFor("com.cobaltplatform.api.jdbc.readReplicaMaximumPoolSize", Integer.class);

		this.amazonEc2RoleName = valueFor("com.cobaltplatform.api.amazon.ec2RoleName", String.class);

		this.amazonSesRegion = Region.of(valueFor("com.cobaltplatform.api.amazon.ses.region", String.class));
		this.amazonSesConfigurationSetName = valueFor("com.cobaltplatform.api.amazon.ses.configurationSetName", String.class, false);

		this.amazonS3Region = Region.of(valueFor("com.cobaltplatform.api.amazon.s3.region", String.class));
		this.amazonS3BucketName = valueFor("com.cobaltplatform.api.amazon.s3.bucketName", String.class);
		this.amazonS3PresignedUploadExpirationInMinutes = valueFor("com.cobaltplatform.api.amazon.s3.presignedUploadExpirationInMinutes", Integer.class);
		this.amazonS3BaseUrl = determineAmazonS3BaseUrl();
		this.amazonLambdaCallbackBaseUrl = determineAmazonLambdaCallbackBaseUrl();

		this.redisHost = valueFor("com.cobaltplatform.api.redis.host", String.class);
		this.redisPort = valueFor("com.cobaltplatform.api.redis.port", Integer.class);

		this.sentryDsn = valueFor("com.cobaltplatform.api.sentry.dsn", String.class);

		this.bluejeansApiEndpoint = valueFor("com.cobaltplatform.api.bluejeans.apiEndpoint", String.class);
		this.bluejeansClientKey = valueFor("com.cobaltplatform.api.bluejeans.clientKey", String.class);
		this.bluejeansSecretKey = valueFor("com.cobaltplatform.api.bluejeans.secretKey", String.class);
		this.bluejeansDefaultUserId = valueFor("com.cobaltplatform.api.bluejeans.defaultUserId", Integer.class);

		this.acuityUserId = valueFor("com.cobaltplatform.api.acuity.userId", String.class);
		this.acuityApiKey = valueFor("com.cobaltplatform.api.acuity.apiKey", String.class);
		this.acuityVideoconferenceFormFieldId = valueFor("com.cobaltplatform.api.acuity.videoconferenceFormFieldId", Long.class);

		this.way2HealthAccessToken = valueFor("com.cobaltplatform.api.way2health.accessToken", String.class);
		this.way2HealthEnvironment = valueFor("com.cobaltplatform.api.way2health.environment", Way2HealthEnvironment.class);

		this.tinymceApiKey = valueFor("com.cobaltplatform.api.tinymce.apiKey", String.class);

		// TODO: data-drive, institution-specific
		this.epicNonProdKeyId = "e8c96880-4a36-4dbd-9a32-1f1009cc507c";
		this.epicProdKeyId = "e40560fb-4a47-43ea-949f-bc0b3ad7bd50";
		this.epicCurrentEnvironmentKeyId = isProduction() ? getEpicProdKeyId() : getEpicNonProdKeyId();

		this.signingCredentials = loadSigningCredentials("cobalt");
		this.epicNonProdSigningCredentials = loadSigningCredentials("cobalt-epic-nonprod");
		this.epicProdSigningCredentials = loadSigningCredentials("cobalt-epic-prod");
		this.epicCurrentEnvironmentSigningCredentials = isProduction() ? getEpicProdSigningCredentials() : getEpicNonProdSigningCredentials();
		this.microsoftSigningCredentials = loadSigningCredentials("cobalt-microsoft");

		this.samlSettingsByIdentityProvider = Collections.emptyMap();

		this.shouldIncludeTestDataInIcReports = !isProduction();

		this.shouldRunDataSync = valueFor("com.cobaltplatform.api.shouldRunDataSync", Boolean.class);
		this.dataSyncRemoteDb = valueFor("com.cobaltplatform.api.dataSyncRemoteDb", String.class);
		this.dataSyncIntervalInSeconds = valueFor("com.cobaltplatform.api.dataSyncIntervalInSeconds", Long.class);

		if (getAmazonUseLocalstack()) {
			// Prime the default credential provider chain
			// https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html
			System.setProperty("aws.accessKeyId", "fake");
			System.setProperty("aws.secretAccessKey", "fake");
		}
	}

	@Nonnull
	public Boolean isLocal() {
		String environment = getEnvironment().toLowerCase(Locale.US);
		return environment.equals("local");
	}

	@Nonnull
	public Boolean isProduction() {
		String environment = getEnvironment().toLowerCase(Locale.US);
		return environment.equals("prod") || environment.endsWith("-prod");
	}

	@Nonnull
	public String getDefaultEmailToAddress(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);
		return "cobaltplatform@xmog.com";
	}

	@Nonnull
	public String getDefaultGroupSessionImageUrlForEmail() {
		// Kind of a hack
		return format("https://cobaltplatform.s3.us-east-2.amazonaws.com/%s/emails/default-group-session.png", getEnvironment());
	}

	@Nonnull
	public String getDefaultProviderImageUrlForEmail() {
		// Kind of a hack
		return format("https://cobaltplatform.s3.us-east-2.amazonaws.com/%s/emails/default-provider.png", getEnvironment());
	}

	@Nonnull
	protected String determineNodeIdentifierForDeploymentTarget(@Nonnull DeploymentTarget deploymentTarget) {
		requireNonNull(deploymentTarget);

		if (deploymentTarget == DeploymentTarget.LOCAL)
			return determineHostname();

		if (deploymentTarget == DeploymentTarget.AMAZON_EC2)
			return determineAmazonEc2NodeIdentifier();

		if (deploymentTarget == DeploymentTarget.AMAZON_ECS)
			return determineAmazonEcsTaskIdentifier();

		throw new UnsupportedOperationException(format("Unknown deployment target %s", deploymentTarget.name()));
	}

	@Nonnull
	protected String determineHostname() {
		String hostname = null;

		try {
			InetAddress inetAddress = InetAddress.getLocalHost();
			hostname = trimToNull(inetAddress.getHostName());
		} catch (UnknownHostException ignored) {
			// Don't care why it failed
		}

		return hostname == null ? "localhost" : hostname;
	}

	@Nonnull
	protected String determineAmazonEc2NodeIdentifier() {
		try {
			// Amazon provides a magic URL that, if requested from an EC2 instance, will return the instance's ID.
			// Only works if Amazon is DNS provider, which should be the case unless your configuration is special
			HttpResponse httpResponse = new DefaultHttpClient().execute(
					new HttpRequest.Builder(HttpMethod.GET, "http://instance-data/latest/meta-data/instance-id").build());

			String responseBody = new String(httpResponse.getBody().get(), StandardCharsets.UTF_8).trim();

			if (httpResponse.getStatus() >= 400)
				throw new IOException(format("Bad HTTP response (status %s). Response body was:\n%s", httpResponse.getStatus(), responseBody));

			// Arbitrarily pick 100 characters as cutoff for "this doesn't look right"
			if (responseBody.length() > 100)
				throw new IOException(format("Unexpected HTTP response, this doesn't look like a node identifier. Response body was:\n%s", responseBody));

			return responseBody;
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to determine EC2 node identifier", e);
		}
	}

	@Nonnull
	protected String determineAmazonEcsTaskIdentifier() {
		try {
			// https://docs.aws.amazon.com/AmazonECS/latest/userguide/task-metadata-endpoint-fargate.html
			HttpResponse httpResponse = new DefaultHttpClient().execute(
					new HttpRequest.Builder(HttpMethod.GET, "http://169.254.170.2/v2/metadata").build());

			String responseBody = new String(httpResponse.getBody().get(), StandardCharsets.UTF_8).trim();

			if (httpResponse.getStatus() >= 400)
				throw new IOException(format("Bad HTTP response (status %s). Response body was:\n%s", httpResponse.getStatus(), responseBody));

			JsonMapper jsonMapper = new JsonMapper();
			AwsEcsMetadataResponse awsEcsMetadataResponse = jsonMapper.fromJson(responseBody, AwsEcsMetadataResponse.class);

			return awsEcsMetadataResponse.getTaskARN();
		} catch (IOException e) {
			throw new UncheckedIOException("Unable to determine ECS Task identifier", e);
		}
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	protected <T> T valueFor(@Nonnull String key,
													 @Nonnull Class<T> type) {
		requireNonNull(key);
		requireNonNull(type);

		return valueFor(key, type, true);
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	protected <T> T valueFor(@Nonnull String key,
													 @Nonnull Class<T> type,
													 @Nonnull Boolean required) {
		requireNonNull(key);
		requireNonNull(type);
		requireNonNull(required);

		// Example key: com.cobaltplatform.api.google.fcm.projectId
		// Example environment var: COBALT_API_GOOGLE_FCM_PROJECT_ID
		String environmentVariableName = environmentVariableNameFromKey(key);
		String stringValueToConvert = environmentValueFor(environmentVariableName).orElse(null);

		// If no environment variable value, read from properties file
		if (stringValueToConvert == null) {
			T value;

			if (required)
				value = propertiesFileReader.valueFor(key, type);
			else
				value = propertiesFileReader.optionalValueFor(key, type).orElse(null);

			if (value == null)
				return null;

			String stringValue = String.valueOf(value);

			// If it's a secret value, extract it by key
			if (stringValue.equals(SECRETS_PREFIX)) {
				stringValueToConvert = secretConfigurationManager.valueFor(key);
			} else {
				return value;
			}
		}

		// Use environment variable value
		Optional<ValueConverter<Object, Object>> valueConverter = getValueConverterRegistry().get(String.class, type);

		if (!valueConverter.isPresent())
			throw new IllegalArgumentException(format(
					"Not sure how to convert environment variable '%s=%s' to requested type %s", environmentVariableName, stringValueToConvert, type));

		try {
			return (T) valueConverter.get().convert(stringValueToConvert);
		} catch (ValueConversionException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * This method is static so it can be called from this class' constructor.
	 */
	@Nonnull
	protected static Optional<String> environmentValueFor(@Nonnull String key) {
		requireNonNull(key);

		key = key.trim();

		// First, try env var...
		String value = trimToNull(System.getenv(key));

		if (value != null)
			return Optional.of(value);

		// If not there, try JVM argument...
		value = trimToNull(System.getProperty(key));

		if (value != null)
			return Optional.of(value);

		// Nothing was found
		return Optional.empty();
	}

	/**
	 * Example input: com.cobaltplatform.api.google.fcm.projectId
	 * Example output: COBALT_API_GOOGLE_FCM_PROJECT_ID
	 */
	@Nonnull
	protected String environmentVariableNameFromKey(@Nonnull String key) {
		requireNonNull(key);

		final String IGNORED_PREFIX = "com.cobaltplatform.api.";

		if (!key.startsWith(IGNORED_PREFIX))
			throw new IllegalStateException(format("Properties must be prefixed with '%s'", IGNORED_PREFIX));

		final String NEW_PREFIX = "COBALT_API";

		key = key.trim().substring(IGNORED_PREFIX.length());

		Iterable<String> components = Splitter.on('.').trimResults().omitEmptyStrings().split(key);
		List<String> normalizedComponents = new ArrayList<>();

		for (String component : components)
			normalizedComponents.add(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, component));

		return format("%s_%s", NEW_PREFIX, normalizedComponents.stream().collect(Collectors.joining("_")));
	}

	@Nonnull
	protected Instant determineBuildTimestamp() {
		// Archiver will create this file for builds designed for deployment.
		// If the file doesn't exist, we ask Maven to read the POM for us
		Path buildTimestampFile = Paths.get("build-timestamp");

		if (Files.isRegularFile(buildTimestampFile)) {
			try {
				return Instant.parse(Files.readString(buildTimestampFile, StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		return Instant.now();
	}

	@Nonnull
	protected String determineApplicationVersion() {
		// Archiver will create this file for builds designed for deployment.
		// If the file doesn't exist, we ask Maven to read the POM for us
		Path applicationVersionFile = Paths.get("application-version");

		if (Files.isRegularFile(applicationVersionFile)) {
			try {
				return Files.readString(applicationVersionFile, StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		return MavenUtility.getPomVersion();
	}

	@Nonnull
	protected String determineGitCommitHash() {
		// Archiver will create this file for builds designed for deployment.
		// If the file doesn't exist, we ask the OS's git to tell us the commit hash
		Path gitCommitHashFile = Paths.get("git-commit-hash");

		if (Files.isRegularFile(gitCommitHashFile)) {
			try {
				return Files.readString(gitCommitHashFile, StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		return GitUtility.getHeadCommitHash();
	}

	@Nonnull
	protected String determineGitBranch() {
		// Archiver will create this file for builds designed for deployment.
		// If the file doesn't exist, we ask the OS's git to tell us the branch
		Path getBranchFile = Paths.get("git-branch");

		if (Files.isRegularFile(getBranchFile)) {
			try {
				return Files.readString(getBranchFile, StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		return GitUtility.getBranch();
	}

	@Nonnull
	protected String determineAmazonS3BaseUrl() {
		if (!getAmazonUseLocalstack())
			return "https://s3.amazonaws.com";

		String ipAddress = getIpAddress().orElse(null);

		if (ipAddress == null)
			throw new IllegalStateException("Your IP address is required to use Localstack, but we could not detect it");

		// We need to use your IP instead of localhost for Localstack.
		// For example, the AWS SDK will generate bucket URLs like http://cobalt-local.localhost:4566 but they should be
		// http://192.168.1.21:4566/cobalt-local, otherwise Localstack has problems dealing with them (CORS, bucket policies, ...)
		return format("http://%s:%d", ipAddress, getAmazonLocalstackPort());
	}

	@Nonnull
	protected String determineAmazonLambdaCallbackBaseUrl() {
		String amazonLambdaCallbackBaseUrl = null;

		if (getAmazonUseLocalstack()) {
			String ipAddress = getIpAddress().orElse(null);

			if (ipAddress != null)
				amazonLambdaCallbackBaseUrl = format("http://%s:%s", ipAddress, getPort());
		}

		return amazonLambdaCallbackBaseUrl == null ? getBaseUrl() : amazonLambdaCallbackBaseUrl;
	}

	// See https://stackoverflow.com/a/13007325
	@Nonnull
	public Optional<String> determineIpAddress(@Nonnull Boolean useIpv4) {
		requireNonNull(useIpv4);

		// Alternatively: returns a value like "/192.168.1.21"
		// Socket socket = new Socket();
		// socket.connect(new InetSocketAddress("google.com", 80));
		// String ip = socket.getLocalAddress().toString();

		List<String> interfaceNames = new ArrayList<>();
		List<String> ipAddresses = new ArrayList<>();
		Map<String, List<String>> ipAddressesByInterfaceName = new HashMap<>();

		try {
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces) {
				String interfaceName = intf.getName();
				interfaceNames.add(interfaceName);
				List<InetAddress> addrs = Collections.list(intf.getInetAddresses());

				for (InetAddress addr : addrs) {
					if (!addr.isLoopbackAddress()) {
						String sAddr = addr.getHostAddress();
						//boolean isIpv4 = InetAddressUtils.isIPv4Address(sAddr);
						boolean isIpv4 = sAddr.indexOf(':') < 0;
						String normalizedIpAddress = null;

						if (useIpv4) {
							if (isIpv4) {
								normalizedIpAddress = sAddr;
							}
						} else {
							if (!isIpv4) {
								int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
								normalizedIpAddress = delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
							}
						}

						if (normalizedIpAddress != null && normalizedIpAddress.length() > 0) {
							ipAddresses.add(normalizedIpAddress);

							List<String> currentIpAddresses = ipAddressesByInterfaceName.get(interfaceName);

							if (currentIpAddresses == null) {
								currentIpAddresses = new ArrayList<>();
								ipAddressesByInterfaceName.put(interfaceName, currentIpAddresses);
							}

							currentIpAddresses.add(normalizedIpAddress);

							Collections.sort(currentIpAddresses);
						}
					}
				}
			}
		} catch (Exception ignored) {
			// for now eat exceptions
		}

		Collections.sort(interfaceNames);

		for (String interfaceName : interfaceNames) {
			List<String> ipAddressesForInterface = ipAddressesByInterfaceName.get(interfaceName);

			if (ipAddressesForInterface != null && ipAddressesForInterface.size() > 0)
				return Optional.of(ipAddressesForInterface.get(0));
		}

		return Optional.empty();
	}

	@Nonnull
	protected SigningCredentials loadSigningCredentials(@Nonnull String namePrefix) {
		requireNonNull(namePrefix);

		String certificateAsString;
		String privateKeyAsString;

		if (sensitiveDataStorageLocation == SensitiveDataStorageLocation.FILESYSTEM) {
			Path certFile = Paths.get(format("config/%s/%s.crt", getEnvironment(), namePrefix));

			if (!Files.isRegularFile(certFile))
				throw new IllegalStateException(format("Could not find cert file at %s", certFile.toAbsolutePath()));

			Path privateKeyFile = Paths.get(format("config/%s/%s.pem", getEnvironment(), namePrefix));

			if (!Files.isRegularFile(privateKeyFile))
				throw new IllegalStateException(format("Could not find private key file at %s", privateKeyFile.toAbsolutePath()));

			try {
				certificateAsString = new String(Files.readAllBytes(certFile), StandardCharsets.UTF_8);
				privateKeyAsString = new String(Files.readAllBytes(privateKeyFile), StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else if (sensitiveDataStorageLocation == SensitiveDataStorageLocation.AMAZON_S3) {
			S3ClientBuilder builder = S3Client.builder().region(getAmazonS3Region());

			if (getAmazonUseLocalstack()) {
				builder.endpointOverride(URI.create(getAmazonS3BaseUrl()));
			}

			S3Client amazonS3 = builder.build();

			getLogger().info("Requesting bucket {} with key {}", getAmazonS3BucketName(), format("%s/%s.crt", getEnvironment(), namePrefix));

			// e.g. https://cobaltplatform.s3.us-east-2.amazonaws.com/prod/{namePrefix}.crt
			GetObjectRequest certObjectRequest = GetObjectRequest.builder()
					.bucket(getAmazonS3BucketName())
					.key(format("%s/%s.crt", getEnvironment(), namePrefix))
					.build();

			try (InputStream inputStream = amazonS3.getObject(certObjectRequest)) {
				certificateAsString = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			// e.g. https://cobaltplatform.s3.us-east-2.amazonaws.com/prod/{namePrefix}.pem
			GetObjectRequest privateKeyObjectRequest = GetObjectRequest.builder()
					.bucket(getAmazonS3BucketName())
					.key(format("%s/%s.pem", getEnvironment(), namePrefix))
					.build();

			try (InputStream inputStream = amazonS3.getObject(privateKeyObjectRequest)) {
				privateKeyAsString = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else if (sensitiveDataStorageLocation == SensitiveDataStorageLocation.AMAZON_SECRETS_MANAGER) {
			byte[] certificate = secretManagerClient.getSecretBinary(getAmazonAwsSecretsManagerContext().get() + "-" + namePrefix + "-crt");
			certificateAsString = new String(certificate, StandardCharsets.UTF_8);
			byte[] privateKey = secretManagerClient.getSecretBinary(getAmazonAwsSecretsManagerContext().get() + "-" + namePrefix + "-pem");
			privateKeyAsString = new String(privateKey, StandardCharsets.UTF_8);
		} else {
			throw new IllegalStateException(format("Unknown value for %s: %s", SensitiveDataStorageLocation.class.getSimpleName(), sensitiveDataStorageLocation.name()));
		}

		return new SigningCredentials(certificateAsString.trim(), privateKeyAsString.trim());
	}

	@Nonnull
	protected Map<String, Object> determineSamlSettings(@Nonnull SamlIdentityProvider samlIdentityProvider,
																											@Nonnull SigningCredentials signingCredentials) {
		requireNonNull(samlIdentityProvider);
		requireNonNull(signingCredentials);

		Path samlPropertiesFile = Paths.get(format("config/%s/saml/%s.properties", getEnvironment(), samlIdentityProvider.name()));

		if (!Files.isRegularFile(samlPropertiesFile))
			throw new IllegalStateException(format("Could not find SAML properties file at %s", samlPropertiesFile.toAbsolutePath()));

		Properties properties = new Properties();

		try (FileInputStream fileInputStream = new FileInputStream(samlPropertiesFile.toFile())) {
			properties.load(fileInputStream);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		Map<String, Object> propertiesAsMap = properties.entrySet().stream().collect(
				Collectors.toMap(
						e -> e.getKey().toString(),
						e -> e.getValue().toString()
				)
		);

		String certificateAsString = CryptoUtility.base64Representation(signingCredentials.getX509Certificate());
		propertiesAsMap.put("onelogin.saml2.sp.x509cert", certificateAsString);

		String privateKey = CryptoUtility.base64Representation(signingCredentials.getPrivateKey());
		propertiesAsMap.put("onelogin.saml2.sp.privatekey", privateKey);

		return Collections.unmodifiableMap(propertiesAsMap);
	}

	@NotThreadSafe
	protected static class AwsEcsMetadataResponse {
		@Nullable
		private String TaskARN;

		@Nullable
		public String getTaskARN() {
			return TaskARN;
		}

		public void setTaskARN(@Nullable String taskARN) {
			TaskARN = taskARN;
		}
	}

	@NotThreadSafe
	protected static class AwsEcsRoleCredentialsResponse {
		@Nullable
		private String RoleArn;
		@Nullable
		private String AccessKeyId;
		@Nullable
		private String SecretAccessKey;
		@Nullable
		private String Token;
		@Nullable
		private String Expiration;

		@Nullable
		public String getRoleArn() {
			return RoleArn;
		}

		public void setRoleArn(@Nullable String roleArn) {
			RoleArn = roleArn;
		}

		@Nullable
		public String getAccessKeyId() {
			return AccessKeyId;
		}

		public void setAccessKeyId(@Nullable String accessKeyId) {
			AccessKeyId = accessKeyId;
		}

		@Nullable
		public String getSecretAccessKey() {
			return SecretAccessKey;
		}

		public void setSecretAccessKey(@Nullable String secretAccessKey) {
			SecretAccessKey = secretAccessKey;
		}

		@Nullable
		public String getToken() {
			return Token;
		}

		public void setToken(@Nullable String token) {
			Token = token;
		}

		@Nullable
		public String getExpiration() {
			return Expiration;
		}

		public void setExpiration(@Nullable String expiration) {
			Expiration = expiration;
		}
	}

	@Nonnull
	protected ValueConverterRegistry getValueConverterRegistry() {
		return valueConverterRegistry;
	}

	@Nonnull
	protected PropertiesFileReader getPropertiesFileReader() {
		return propertiesFileReader;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}

	@Nonnull
	protected String getEnvEnvVariableName() {
		return ENV_ENV_VARIABLE_NAME;
	}

	@Nonnull
	protected String getPortEnvVariableName() {
		return PORT_ENV_VARIABLE_NAME;
	}

	@Nonnull
	protected String getDefaultEnv() {
		return DEFAULT_ENV;
	}

	@Nonnull
	protected Integer getDefaultPort() {
		return DEFAULT_PORT;
	}

	@Nonnull
	protected Map<SamlIdentityProvider, Map<String, Object>> getSamlSettingsByIdentityProvider() {
		return samlSettingsByIdentityProvider;
	}

	@Nonnull
	public String getEnvironment() {
		return this.environment;
	}

	@Nonnull
	public Locale getDefaultLocale() {
		return this.defaultLocale;
	}

	@Nonnull
	public ZoneId getDefaultTimeZone() {
		return this.defaultTimeZone;
	}

	@Nonnull
	public String getHost() {
		return host;
	}

	@Nonnull
	public Integer getPort() {
		return port;
	}

	@Nonnull
	public String getBaseUrl() {
		return baseUrl;
	}

	@Nonnull
	public String getApplicationVersion() {
		return applicationVersion;
	}

	@Nonnull
	public StoppingStrategy getServerStoppingStrategy() {
		return this.serverStoppingStrategy;
	}

	@Nonnull
	public MappingFormat getJsonMappingFormat() {
		return this.jsonMappingFormat;
	}

	@Nonnull
	public Boolean getShouldDisplayStackTraces() {
		return this.shouldDisplayStackTraces;
	}

	@Nonnull
	public Boolean getShouldCacheHandlebarsTemplates() {
		return this.shouldCacheHandlebarsTemplates;
	}

	@Nonnull
	public Boolean getShouldSendRealEmailMessages() {
		return shouldSendRealEmailMessages;
	}

	@Nonnull
	public Boolean getShouldSendRealPushMessages() {
		return shouldSendRealPushMessages;
	}

	@Nonnull
	public Boolean getShouldSendRealSmsMessages() {
		return shouldSendRealSmsMessages;
	}

	@Nonnull
	public Boolean getShouldSendRealCallMessages() {
		return shouldSendRealCallMessages;
	}

	@Nonnull
	public Boolean getShouldSendErrorReports() {
		return shouldSendErrorReports;
	}

	@Nonnull
	public Boolean getShouldEnableDebuggingHeaders() {
		return shouldEnableDebuggingHeaders;
	}

	@Nonnull
	public Boolean getShouldUseRealAuthentication() {
		return shouldUseRealAuthentication;
	}

	@Nonnull
	public Boolean getShouldUseRealAcuity() {
		return shouldUseRealAcuity;
	}

	@Nonnull
	public Boolean getShouldUseRealEpic() {
		return shouldUseRealEpic;
	}

	@Nonnull
	public Boolean getShouldUseRealBluejeans() {
		return shouldUseRealBluejeans;
	}

	@Nonnull
	public Boolean getShouldUseRealWay2Health() {
		return shouldUseRealWay2Health;
	}

	@Nonnull
	public Boolean getShouldEnableCacheDebugging() {
		return shouldEnableCacheDebugging;
	}

	@Nonnull
	public Boolean getShouldEnableIcDebugging() {
		return this.shouldEnableIcDebugging;
	}

	@Nonnull
	public String getCorsEnabledDomains() {
		return corsEnabledDomains;
	}

	@Nonnull
	public String getEmailDefaultFromAddress() {
		return emailDefaultFromAddress;
	}

	@Nonnull
	public DeploymentTarget getDeploymentTarget() {
		return deploymentTarget;
	}

	@Nonnull
	public SensitiveDataStorageLocation getSensitiveDataStorageLocation() {
		return sensitiveDataStorageLocation;
	}

	@Nonnull
	public String getNodeIdentifier() {
		return nodeIdentifier;
	}

	@Nonnull
	public String getJdbcUrl() {
		return this.jdbcUrl;
	}

	@Nonnull
	public String getJdbcUsername() {
		return this.jdbcUsername;
	}

	@Nonnull
	public String getJdbcPassword() {
		return this.jdbcPassword;
	}

	@Nonnull
	public Integer getJdbcMaximumPoolSize() {
		return this.jdbcMaximumPoolSize;
	}

	@Nonnull
	public String getJdbcReadReplicaUrl() {
		return this.jdbcReadReplicaUrl;
	}

	@Nonnull
	public String getJdbcReadReplicaUsername() {
		return this.jdbcReadReplicaUsername;
	}

	@Nonnull
	public String getJdbcReadReplicaPassword() {
		return this.jdbcReadReplicaPassword;
	}

	@Nonnull
	public Integer getJdbcReadReplicaMaximumPoolSize() {
		return this.jdbcReadReplicaMaximumPoolSize;
	}

	@Nonnull
	public Boolean getDownForMaintenance() {
		return downForMaintenance;
	}

	@Nonnull
	public Boolean getAmazonUseLocalstack() {
		return amazonUseLocalstack;
	}

	@Nonnull
	public Integer getAmazonLocalstackPort() {
		return amazonLocalstackPort;
	}

	@Nonnull
	public String getAmazonEc2RoleName() {
		return amazonEc2RoleName;
	}

	@Nonnull
	public String getAmazonLambdaCallbackBaseUrl() {
		return amazonLambdaCallbackBaseUrl;
	}

	@Nonnull
	public Region getAmazonSesRegion() {
		return amazonSesRegion;
	}

	@Nonnull
	public Optional<String> getAmazonSesConfigurationSetName() {
		return Optional.ofNullable(this.amazonSesConfigurationSetName);
	}

	@Nonnull
	public Region getAmazonS3Region() {
		return amazonS3Region;
	}

	@Nonnull
	public String getAmazonS3BucketName() {
		return amazonS3BucketName;
	}

	@Nonnull
	public Integer getAmazonS3PresignedUploadExpirationInMinutes() {
		return amazonS3PresignedUploadExpirationInMinutes;
	}

	@Nonnull
	public String getAmazonS3BaseUrl() {
		return amazonS3BaseUrl;
	}

	@Nonnull
	public String getRedisHost() {
		return redisHost;
	}

	@Nonnull
	public Integer getRedisPort() {
		return redisPort;
	}

	@Nonnull
	public String getSentryDsn() {
		return sentryDsn;
	}

	@Nonnull
	public String getGitCommitHash() {
		return gitCommitHash;
	}

	@Nonnull
	public String getGitBranch() {
		return gitBranch;
	}

	@Nonnull
	public Instant getBuildTimestamp() {
		return buildTimestamp;
	}

	@Nonnull
	public Instant getDeploymentTimestamp() {
		return deploymentTimestamp;
	}

	@Nonnull
	public Optional<String> getIpAddress() {
		return Optional.ofNullable(ipAddress);
	}

	@Nonnull
	public String getBluejeansApiEndpoint() {
		return bluejeansApiEndpoint;
	}

	@Nonnull
	public String getBluejeansClientKey() {
		return bluejeansClientKey;
	}

	@Nonnull
	public String getBluejeansSecretKey() {
		return bluejeansSecretKey;
	}

	@Nonnull
	public Integer getBluejeansDefaultUserId() {
		return bluejeansDefaultUserId;
	}

	@Nonnull
	public String getAcuityUserId() {
		return acuityUserId;
	}

	@Nonnull
	public String getAcuityApiKey() {
		return acuityApiKey;
	}

	@Nonnull
	public Long getAcuityVideoconferenceFormFieldId() {
		return acuityVideoconferenceFormFieldId;
	}

	@Nonnull
	public String getWay2HealthAccessToken() {
		return way2HealthAccessToken;
	}

	@Nonnull
	public Way2HealthEnvironment getWay2HealthEnvironment() {
		return way2HealthEnvironment;
	}

	@Nonnull
	public String getTinymceApiKey() {
		return this.tinymceApiKey;
	}

	@Nonnull
	public Boolean getShouldApplyDatabaseUpdates() {
		return shouldApplyDatabaseUpdates;
	}

	@Nonnull
	public Boolean getShouldIncludeTestDataInIcReports() {
		return shouldIncludeTestDataInIcReports;
	}

	public String getEpicNonProdKeyId() {
		return this.epicNonProdKeyId;
	}

	@Nonnull
	public String getEpicProdKeyId() {
		return this.epicProdKeyId;
	}

	@Nonnull
	public String getEpicCurrentEnvironmentKeyId() {
		return this.epicCurrentEnvironmentKeyId;
	}

	@Nonnull
	public SigningCredentials getSigningCredentials() {
		return this.signingCredentials;
	}

	@Nonnull
	public SigningCredentials getEpicNonProdSigningCredentials() {
		return this.epicNonProdSigningCredentials;
	}

	@Nonnull
	public SigningCredentials getEpicProdSigningCredentials() {
		return this.epicProdSigningCredentials;
	}

	@Nonnull
	public SigningCredentials getEpicCurrentEnvironmentSigningCredentials() {
		return this.epicCurrentEnvironmentSigningCredentials;
	}

	@Nonnull
	public SigningCredentials getMicrosoftSigningCredentials() {
		return this.microsoftSigningCredentials;
	}

	@Nonnull
	public Boolean getShouldPollAcuity() {
		return shouldPollAcuity;
	}

	@Nonnull
	public Boolean getShouldPollEpic() {
		return shouldPollEpic;
	}

	@Nonnull
	public Boolean getShouldPollBluejeans() {
		return shouldPollBluejeans;
	}

	@Nonnull
	public Boolean getShouldPollWay2Health() {
		return shouldPollWay2Health;
	}

	@Nonnull
	public Optional<String> getAmazonAwsSecretsManagerContext() {
		return Optional.ofNullable(this.amazonAwsSecretsManagerContext);
	}

	@Nonnull
	public Optional<SecretConfigurationManager> getSecretConfigurationManager() {
		return Optional.ofNullable(this.secretConfigurationManager);
	}

	@Nonnull
	public Optional<AwsSecretManagerClient> getSecretManagerClient() {
		return Optional.ofNullable(this.secretManagerClient);
	}

	@Nonnull
	public Optional<Map<String, Object>> getSamlSettingsForIdentityProvider(@Nullable SamlIdentityProvider samlIdentityProvider) {
		if (samlIdentityProvider == null)
			return Optional.empty();

		return Optional.ofNullable(getSamlSettingsByIdentityProvider().get(samlIdentityProvider));
	}

	@Nonnull
	public Boolean isRunningInIntegrationTestMode() {
		// Integration tests can override this at runtime to unlock "secret" abilities not allowed during normal system operation
		return false;
	}

	@Nonnull
	public Boolean getShouldRunDataSync() {
		return shouldRunDataSync;
	}

	@Nonnull
	public String getDataSyncRemoteDb() {
		return dataSyncRemoteDb;
	}

	@Nonnull
	public Long getDataSyncIntervalInSeconds() {
		return dataSyncIntervalInSeconds;
	}
}