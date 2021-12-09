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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.cobaltplatform.api.http.DefaultHttpClient;
import com.cobaltplatform.api.http.HttpClient;
import com.cobaltplatform.api.http.HttpMethod;
import com.cobaltplatform.api.http.HttpRequest;
import com.cobaltplatform.api.http.HttpResponse;
import com.cobaltplatform.api.integration.epic.EpicEnvironment;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.security.SamlIdentityProvider;
import com.cobaltplatform.api.util.CryptoUtility;
import com.cobaltplatform.api.util.CryptoUtility.PublicKeyFormat;
import com.cobaltplatform.api.util.DeploymentTarget;
import com.cobaltplatform.api.util.GitUtility;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.JsonMapper.MappingFormat;
import com.cobaltplatform.api.util.MavenUtility;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.crypto.SecretKey;
import javax.inject.Singleton;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
	private final Boolean shouldUseRealEpic;
	@Nonnull
	private final Boolean shouldUseRealBluejeans;
	@Nonnull
	private final Boolean shouldUseRealIc;
	@Nonnull
	private final Boolean shouldEnableCacheDebugging;
	@Nonnull
	private final String corsEnabledDomains;
	@Nonnull
	private final String emailDefaultFromAddress;
	@Nonnull
	private final Long accessTokenExpirationInMinutes;
	@Nonnull
	private final Long accessTokenShortExpirationInMinutes;
	@Nonnull
	private final Boolean downForMaintenance;
	@Nonnull
	private final SecretKey secretKey;
	@Nonnull
	private final String secretKeyAlgorithm;
	@Nonnull
	private final String webappBaseUrl;
	@Nonnull
	private final String icWebappBaseUrl;
	@Nonnull
	private final String icBackendBaseUrl;
	@Nonnull
	private final String nodeIdentifier;
	@Nonnull
	private final String jdbcUrl;
	@Nonnull
	private final String jdbcUsername;
	@Nonnull
	private final String jdbcPassword;
	@Nonnull
	private final Integer jdbcMaximumPoolSize;
	@Nonnull
	private final AWSCredentialsProvider amazonCredentialsProvider;
	@Nonnull
	private final String amazonEc2RoleName;
	@Nonnull
	private final Boolean amazonUseLocalstack;
	@Nonnull
	private final Integer amazonLocalstackPort;
	@Nonnull
	private final String amazonLambdaCallbackBaseUrl;
	@Nonnull
	private final Regions amazonSesRegion;
	@Nonnull
	private final Regions amazonS3Region;
	@Nonnull
	private final String amazonS3BucketName;
	@Nonnull
	private final Integer amazonS3PresignedUploadExpirationInMinutes;
	@Nonnull
	private final String amazonS3BaseUrl;
	@Nonnull
	private final Regions amazonSqsRegion;
	@Nonnull
	private final String amazonSqsEmailMessageQueueName;
	@Nonnull
	private final Integer amazonSqsEmailMessageQueueWaitTimeSeconds;
	@Nonnull
	private final String amazonSqsSmsMessageQueueName;
	@Nonnull
	private final Integer amazonSqsSmsMessageQueueWaitTimeSeconds;
	@Nonnull
	private final String amazonSqsCallMessageQueueName;
	@Nonnull
	private final Integer amazonSqsCallMessageQueueWaitTimeSeconds;
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
	private final EpicEnvironment epicEnvironment;
	@Nonnull
	private final String epicClientId;
	@Nonnull
	private final String epicUserId;
	@Nonnull
	private final String epicUsername;
	@Nonnull
	private final String epicPassword;
	@Nonnull
	private final String twilioFromNumber;
	@Nonnull
	private final String twilioSid;
	@Nonnull
	private final String twilioAccountSid;
	@Nonnull
	private final String twilioAuthToken;

	@Nonnull
	private final String defaultSubdomain;
	@Nonnull
	private final KeyPair keyPair;

	@Nonnull
	private final Map<SamlIdentityProvider, Map<String, Object>> samlSettingsByIdentityProvider;

	static {
		ENV_ENV_VARIABLE_NAME = "COBALT_API_ENV";
		PORT_ENV_VARIABLE_NAME = "COBALT_API_PORT";

		DEFAULT_ENV = "local";
		DEFAULT_PORT = 8080;
	}

	public Configuration() {
		this(environmentValueFor(ENV_ENV_VARIABLE_NAME).isPresent() ? environmentValueFor(ENV_ENV_VARIABLE_NAME).get() : DEFAULT_ENV);
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

		this.serverStoppingStrategy = valueFor("com.cobaltplatform.api.serverStoppingStrategy", StoppingStrategy.class);
		this.jsonMappingFormat = valueFor("com.cobaltplatform.api.jsonMappingFormat", MappingFormat.class);
		this.deploymentTarget = valueFor("com.cobaltplatform.api.deploymentTarget", DeploymentTarget.class);
		this.sensitiveDataStorageLocation = valueFor("com.cobaltplatform.api.sensitiveDataStorageLocation", SensitiveDataStorageLocation.class);
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
		this.shouldUseRealEpic = valueFor("com.cobaltplatform.api.shouldUseRealEpic", Boolean.class);
		this.shouldUseRealBluejeans = valueFor("com.cobaltplatform.api.shouldUseRealBluejeans", Boolean.class);
		this.shouldUseRealIc = valueFor("com.cobaltplatform.api.shouldUseRealIc", Boolean.class);
		this.shouldEnableCacheDebugging = valueFor("com.cobaltplatform.api.shouldEnableCacheDebugging", Boolean.class);
		this.corsEnabledDomains = valueFor("com.cobaltplatform.api.corsEnabledDomains", String.class);
		this.emailDefaultFromAddress = valueFor("com.cobaltplatform.api.emailDefaultFromAddress", String.class);
		this.accessTokenExpirationInMinutes = valueFor("com.cobaltplatform.api.accessTokenExpirationInMinutes", Long.class);
		this.accessTokenShortExpirationInMinutes = valueFor("com.cobaltplatform.api.accessTokenShortExpirationInMinutes", Long.class);
		this.downForMaintenance = valueFor("com.cobaltplatform.api.downForMaintenance", Boolean.class);
		this.secretKeyAlgorithm = valueFor("com.cobaltplatform.api.secretKeyAlgorithm", String.class);
		this.secretKey = CryptoUtility.loadSecretKeyInBase64(valueFor("com.cobaltplatform.api.secretKey", String.class), getSecretKeyAlgorithm());
		this.webappBaseUrl = valueFor("com.cobaltplatform.api.webappBaseUrl", String.class);
		this.icWebappBaseUrl = valueFor("com.cobaltplatform.api.icWebappBaseUrl", String.class);
		this.icBackendBaseUrl = valueFor("com.cobaltplatform.api.icBackendBaseUrl", String.class);

		// The https://github.com/impossibl/pgjdbc-ng driver uses jdbc:pgsql:// while the regular driver uses jdbc:postgresql://.
		// Due to limitations of the pgjdbc-ng driver, we force to the normal driver.
		// Due to chicken-and-egg issue in AWS deployment, it's simpler to just rewrite the URL here instead of having the AWS environment tweaked...
		this.jdbcUrl = valueFor("com.cobaltplatform.api.jdbc.url", String.class).replace("jdbc:pgsql://", "jdbc:postgresql://");
		this.jdbcUsername = valueFor("com.cobaltplatform.api.jdbc.username", String.class);
		this.jdbcPassword = valueFor("com.cobaltplatform.api.jdbc.password", String.class);
		this.jdbcMaximumPoolSize = valueFor("com.cobaltplatform.api.jdbc.maximumPoolSize", Integer.class);

		this.amazonEc2RoleName = valueFor("com.cobaltplatform.api.amazon.ec2RoleName", String.class);
		this.amazonUseLocalstack = valueFor("com.cobaltplatform.api.amazon.useLocalstack", Boolean.class);
		this.amazonLocalstackPort = valueFor("com.cobaltplatform.api.amazon.localstackPort", Integer.class);

		this.amazonCredentialsProvider = createAmazonCredentialsProviderForDeploymentTarget(getDeploymentTarget());
		this.amazonSesRegion = Regions.fromName(valueFor("com.cobaltplatform.api.amazon.ses.region", String.class));

		this.amazonS3Region = Regions.fromName(valueFor("com.cobaltplatform.api.amazon.s3.region", String.class));
		this.amazonS3BucketName = valueFor("com.cobaltplatform.api.amazon.s3.bucketName", String.class);
		this.amazonS3PresignedUploadExpirationInMinutes = valueFor("com.cobaltplatform.api.amazon.s3.presignedUploadExpirationInMinutes", Integer.class);
		this.amazonS3BaseUrl = determineAmazonS3BaseUrl();
		this.amazonLambdaCallbackBaseUrl = determineAmazonLambdaCallbackBaseUrl();

		this.amazonSqsRegion = Regions.fromName(valueFor("com.cobaltplatform.api.amazon.sqs.region", String.class));
		this.amazonSqsEmailMessageQueueName = valueFor("com.cobaltplatform.api.amazon.sqs.emailMessageQueueName", String.class);
		this.amazonSqsEmailMessageQueueWaitTimeSeconds = valueFor("com.cobaltplatform.api.amazon.sqs.emailMessageQueueWaitTimeSeconds", Integer.class);
		this.amazonSqsSmsMessageQueueName = valueFor("com.cobaltplatform.api.amazon.sqs.smsMessageQueueName", String.class);
		this.amazonSqsSmsMessageQueueWaitTimeSeconds = valueFor("com.cobaltplatform.api.amazon.sqs.smsMessageQueueWaitTimeSeconds", Integer.class);
		this.amazonSqsCallMessageQueueName = valueFor("com.cobaltplatform.api.amazon.sqs.callMessageQueueName", String.class);
		this.amazonSqsCallMessageQueueWaitTimeSeconds = valueFor("com.cobaltplatform.api.amazon.sqs.callMessageQueueWaitTimeSeconds", Integer.class);

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

		this.epicEnvironment = valueFor("com.cobaltplatform.api.epic.environment", EpicEnvironment.class);
		this.epicClientId = valueFor("com.cobaltplatform.api.epic.clientId", String.class);
		this.epicUserId = valueFor("com.cobaltplatform.api.epic.userId", String.class);
		this.epicUsername = valueFor("com.cobaltplatform.api.epic.username", String.class);
		this.epicPassword = valueFor("com.cobaltplatform.api.epic.password", String.class);

		this.twilioFromNumber = valueFor("com.cobaltplatform.api.twilio.fromNumber", String.class);
		this.twilioSid = valueFor("com.cobaltplatform.api.twilio.sid", String.class);
		this.twilioAccountSid = valueFor("com.cobaltplatform.api.twilio.accountSid", String.class);
		this.twilioAuthToken = valueFor("com.cobaltplatform.api.twilio.authToken", String.class);

		this.defaultSubdomain = valueFor("com.cobaltplatform.api.defaultSubdomain", String.class);

		RawKeypair rawKeypair = loadRawKeypair();

		this.keyPair = CryptoUtility.keyPairFromStringRepresentation(rawKeypair.getCert(), rawKeypair.getPrivateKey(), PublicKeyFormat.X509);
		this.samlSettingsByIdentityProvider = Collections.emptyMap();
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
	protected AWSCredentialsProvider createAmazonCredentialsProviderForDeploymentTarget(@Nonnull DeploymentTarget deploymentTarget) {
		requireNonNull(deploymentTarget);

		if (deploymentTarget == DeploymentTarget.LOCAL) {
			String accessKey = valueFor("com.cobaltplatform.api.amazon.accessKey", String.class);
			String secretKey = valueFor("com.cobaltplatform.api.amazon.secretKey", String.class);
			return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
		}

		if (deploymentTarget == DeploymentTarget.AMAZON_EC2) {
			throw new UnsupportedOperationException();
		}

		if (deploymentTarget == DeploymentTarget.AMAZON_ECS) {
			// Unfortunately InstanceProfileCredentialsProvider doesn't work on Fargate currently, and we need support for refreshable tokens.
			// So we write our own here...
			return new EcsCredentialsProvider();
		}

		throw new UnsupportedOperationException(format("Unknown deployment target %s", deploymentTarget.name()));
	}

	@ThreadSafe
	protected static class EcsCredentialsProvider implements AWSCredentialsProvider {
		@Nonnull
		private static final Long REFRESH_INTERVAL_IN_MINUTES;

		@Nonnull
		private final ScheduledExecutorService scheduledExecutorService;
		@Nonnull
		private final HttpClient httpClient;
		@Nonnull
		private final Logger logger;

		@Nonnull
		private BasicSessionCredentials basicSessionCredentials;

		static {
			REFRESH_INTERVAL_IN_MINUTES = 1L;
		}

		public EcsCredentialsProvider() {
			this.httpClient = new DefaultHttpClient("com.cobaltplatform.api.tokenrefresh");
			this.logger = LoggerFactory.getLogger(getClass());

			// Force initial synchronous pull of credentials
			refresh();

			this.scheduledExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
				@Override
				public Thread newThread(@Nonnull Runnable runnable) {
					requireNonNull(runnable);

					Thread thread = Executors.defaultThreadFactory().newThread(runnable);
					thread.setName("ecs-credentials-refresh");
					thread.setDaemon(true);
					return thread;
				}
			});

			getScheduledExecutorService().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						refresh();
					} catch (Exception e) {
						getLogger().warn(format("Unable to refresh ECS credentials - will retry in %s minute[s]", String.valueOf(getRefreshIntervalInMinutes())), e);
					}
				}
			}, getRefreshIntervalInMinutes(), getRefreshIntervalInMinutes(), TimeUnit.MINUTES);
		}

		@Override
		public AWSCredentials getCredentials() {
			return basicSessionCredentials;
		}

		@Override
		public void refresh() {
			if (!environmentValueFor("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI").isPresent())
				throw new RuntimeException("ESC Deployment Target required ENV var 'AWS_CONTAINER_CREDENTIALS_RELATIVE_URI' missing.");

			// ECS provides a magic URL that, if requested from an ECS Container, will return the security credentials for that task's role.
			String url = format("http://169.254.170.2%s", environmentValueFor("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI").get());

			try {
				HttpResponse httpResponse = getHttpClient().execute(new HttpRequest.Builder(HttpMethod.GET, url).build());
				String responseBody = new String(httpResponse.getBody().get(), StandardCharsets.UTF_8).trim();

				if (httpResponse.getStatus() >= 400)
					throw new IOException(format("Bad HTTP response (status %s). Response body was:\n%s", httpResponse.getStatus(), responseBody));

				// Response:
				// "AccessKeyId": "ACCESS_KEY_ID",
				// "Expiration": "EXPIRATION_DATE",
				// "RoleArn": "TASK_ROLE_ARN",
				// "SecretAccessKey": "SECRET_ACCESS_KEY",
				// "Token": "SECURITY_TOKEN_STRING"

				JsonMapper jsonMapper = new JsonMapper();
				AwsEcsRoleCredentialsResponse ecsRoleCredentialsResponse = jsonMapper.fromJson(responseBody, AwsEcsRoleCredentialsResponse.class);

				this.basicSessionCredentials = new BasicSessionCredentials(ecsRoleCredentialsResponse.getAccessKeyId(), ecsRoleCredentialsResponse.getSecretAccessKey(), ecsRoleCredentialsResponse.getToken());
			} catch (IOException e) {
				throw new UncheckedIOException(format("Unable to determine ECS credentials from %s", url), e);
			}
		}

		@Nonnull
		protected Long getRefreshIntervalInMinutes() {
			return REFRESH_INTERVAL_IN_MINUTES;
		}

		@Nonnull
		protected HttpClient getHttpClient() {
			return httpClient;
		}

		@Nonnull
		protected ScheduledExecutorService getScheduledExecutorService() {
			return scheduledExecutorService;
		}

		@Nonnull
		protected BasicSessionCredentials getBasicSessionCredentials() {
			return basicSessionCredentials;
		}

		@Nonnull
		protected Logger getLogger() {
			return logger;
		}
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
	protected <T> T valueFor(@Nonnull String key, @Nonnull Class<T> type) {
		requireNonNull(key);
		requireNonNull(type);

		// Example key: com.cobaltplatform.api.google.fcm.projectId
		// Example environment var: COBALT_API_GOOGLE_FCM_PROJECT_ID
		String environmentVariableName = environmentVariableNameFromKey(key);
		String environmentVariableValue = environmentValueFor(environmentVariableName).orElse(null);

		// If no environment variable value, read from properties file
		if (environmentVariableValue == null)
			return propertiesFileReader.valueFor(key, type);

		// Use environment variable value
		Optional<ValueConverter<Object, Object>> valueConverter = getValueConverterRegistry().get(String.class, type);

		if (!valueConverter.isPresent())
			throw new IllegalArgumentException(format(
					"Not sure how to convert environment variable '%s=%s' to requested type %s", environmentVariableName, environmentVariableValue, type));

		try {
			return (T) valueConverter.get().convert(environmentVariableValue);
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
		return format("http://%s:%d", ipAddress, 4572 /* getAmazonLocalstackPort() */);
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
	protected RawKeypair loadRawKeypair() {
		String cert;
		String privateKey;

		if (sensitiveDataStorageLocation == SensitiveDataStorageLocation.FILESYSTEM) {
			Path certFile = Paths.get(format("config/%s/cobalt.crt", getEnvironment()));

			if (!Files.isRegularFile(certFile))
				throw new IllegalStateException(format("Could not find SAML cert file at %s", certFile.toAbsolutePath()));

			Path privateKeyFile = Paths.get(format("config/%s/cobalt.pem", getEnvironment()));

			if (!Files.isRegularFile(privateKeyFile))
				throw new IllegalStateException(format("Could not find SAML private key file at %s", privateKeyFile.toAbsolutePath()));

			try {
				cert = new String(Files.readAllBytes(certFile), StandardCharsets.UTF_8);
				privateKey = new String(Files.readAllBytes(privateKeyFile), StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else if (sensitiveDataStorageLocation == SensitiveDataStorageLocation.AMAZON_S3) {
			AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();

			if (getAmazonUseLocalstack()) {
				builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(getAmazonS3BaseUrl(), getAmazonS3Region().getName()))
						.withCredentials(getAmazonCredentialsProvider());
			} else {
				builder.withCredentials(getAmazonCredentialsProvider()).withRegion(getAmazonS3Region().getName());
			}

			AmazonS3 amazonS3 = builder.build();

			// e.g. https://cobaltplatform.s3.us-east-2.amazonaws.com/prod/cobalt.crt
			S3Object certS3Object = amazonS3.getObject(getAmazonS3BucketName(), format("%s/cobalt.crt", getEnvironment()));
			try (InputStream inputStream = certS3Object.getObjectContent()) {
				cert = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			// e.g. https://cobaltplatform.s3.us-east-2.amazonaws.com/prod/cobaltplatform.pem
			S3Object privateKeyS3Object = amazonS3.getObject(getAmazonS3BucketName(), format("%s/cobalt.pem", getEnvironment()));
			try (InputStream inputStream = privateKeyS3Object.getObjectContent()) {
				privateKey = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		} else {
			throw new IllegalStateException(format("Unknown value for %s: %s", SensitiveDataStorageLocation.class.getSimpleName(), sensitiveDataStorageLocation.name()));
		}

		return new RawKeypair(cert, privateKey);
	}

	@Immutable
	protected static class RawKeypair {
		@Nonnull
		private final String cert;
		@Nonnull
		private final String privateKey;

		public RawKeypair(@Nonnull String cert,
											@Nonnull String privateKey) {
			requireNonNull(cert);
			requireNonNull(privateKey);

			this.cert = cert;
			this.privateKey = privateKey;
		}

		@Nonnull
		public String getCert() {
			return cert;
		}

		@Nonnull
		public String getPrivateKey() {
			return privateKey;
		}
	}

	@Nonnull
	protected Map<String, Object> determineSamlSettings(@Nonnull SamlIdentityProvider samlIdentityProvider,
																											@Nonnull RawKeypair rawKeypair) {
		requireNonNull(samlIdentityProvider);
		requireNonNull(rawKeypair);

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

		String cert = normalizeCert(rawKeypair.getCert());
		propertiesAsMap.put("onelogin.saml2.sp.x509cert", cert);

		String privateKey = normalizeCert(rawKeypair.getPrivateKey());
		propertiesAsMap.put("onelogin.saml2.sp.privatekey", privateKey);

		return Collections.unmodifiableMap(propertiesAsMap);
	}

	protected String normalizeCert(@Nonnull String certAsText) {
		requireNonNull(certAsText);

		// Remove header/footer
		certAsText = certAsText.replace("-----BEGIN CERTIFICATE-----", "");
		certAsText = certAsText.replace("-----END CERTIFICATE-----", "");
		certAsText = certAsText.replace("-----BEGIN PRIVATE KEY-----", "");
		certAsText = certAsText.replace("-----END PRIVATE KEY-----", "");

		// Remove all whitespace
		certAsText = certAsText.replaceAll("\\s", "");

		return certAsText;
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
	public Boolean getShouldUseRealIc() {
		return shouldUseRealIc;
	}

	@Nonnull
	public Boolean getShouldEnableCacheDebugging() {
		return shouldEnableCacheDebugging;
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
	public Long getAccessTokenExpirationInMinutes() {
		return accessTokenExpirationInMinutes;
	}

	@Nonnull
	public Long getAccessTokenShortExpirationInMinutes() { return accessTokenShortExpirationInMinutes; }

	@Nonnull
	@Deprecated
	public SecretKey getSecretKey() {
		return secretKey;
	}

	@Nonnull
	@Deprecated
	public String getSecretKeyAlgorithm() {
		return secretKeyAlgorithm;
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
	public String getWebappBaseUrl(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);

		String cobaltPlatformBaseUrl = webappBaseUrl;

		// TODO: we should query for this instead
		String institutionSubdomain = institutionId.name().toLowerCase(Locale.US);

		if ("dev".equals(getEnvironment())) {
			// e.g. https://dev.cobaltplatform.com would become https://chicago-dev.cobaltplatform.com
			return cobaltPlatformBaseUrl.replace("https://dev.", format("https://%s-dev.", institutionSubdomain));
		}

		if ("prod".equals(getEnvironment())) {
			// e.g. https://www.cobaltplatform.com would become https://chicago.cobaltplatform.com
			return cobaltPlatformBaseUrl.replace("https://www.", format("https://%s.", institutionSubdomain));
		}

		return webappBaseUrl;
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
	public AWSCredentialsProvider getAmazonCredentialsProvider() {
		return amazonCredentialsProvider;
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
	public Regions getAmazonSesRegion() {
		return amazonSesRegion;
	}

	@Nonnull
	public Regions getAmazonS3Region() {
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
	public Regions getAmazonSqsRegion() {
		return amazonSqsRegion;
	}

	@Nonnull
	public String getAmazonSqsEmailMessageQueueName() {
		return amazonSqsEmailMessageQueueName;
	}

	@Nonnull
	public Integer getAmazonSqsEmailMessageQueueWaitTimeSeconds() {
		return amazonSqsEmailMessageQueueWaitTimeSeconds;
	}

	@Nonnull
	public String getAmazonSqsSmsMessageQueueName() {
		return amazonSqsSmsMessageQueueName;
	}

	@Nonnull
	public Integer getAmazonSqsSmsMessageQueueWaitTimeSeconds() {
		return amazonSqsSmsMessageQueueWaitTimeSeconds;
	}

	@Nonnull
	public String getAmazonSqsCallMessageQueueName() {
		return amazonSqsCallMessageQueueName;
	}

	@Nonnull
	public Integer getAmazonSqsCallMessageQueueWaitTimeSeconds() {
		return amazonSqsCallMessageQueueWaitTimeSeconds;
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
	public EpicEnvironment getEpicEnvironment() {
		return epicEnvironment;
	}

	@Nonnull
	public String getEpicClientId() {
		return epicClientId;
	}

	@Nonnull
	public String getEpicUserId() {
		return epicUserId;
	}

	@Nonnull
	public String getEpicUsername() {
		return epicUsername;
	}

	@Nonnull
	public String getEpicPassword() {
		return epicPassword;
	}

	@Nonnull
	public String getTwilioFromNumber() {
		return twilioFromNumber;
	}

	@Nonnull
	public String getTwilioSid() {
		return twilioSid;
	}

	@Nonnull
	public String getTwilioAccountSid() {
		return twilioAccountSid;
	}

	@Nonnull
	public String getTwilioAuthToken() {
		return twilioAuthToken;
	}

	@Nonnull
	public String getDefaultSubdomain() {
		return defaultSubdomain;
	}

	@Nonnull
	public String getIcWebappBaseUrl() {
		return icWebappBaseUrl;
	}

	@Nonnull
	public String getIcBackendBaseUrl() {
		return icBackendBaseUrl;
	}

	@Nonnull
	public KeyPair getKeyPair() {
		return keyPair;
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
}
