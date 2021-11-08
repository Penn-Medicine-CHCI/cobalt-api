package com.cobaltplatform.ic.backend.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Regions;
import com.google.gson.Gson;
import com.cobaltplatform.ic.backend.util.KeyManager;
import com.cobaltplatform.ic.backend.util.SecretsManager;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;
import com.typesafe.config.ConfigSyntax;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.KeyPair;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

public class IcConfig {
	private static final Logger logger = LoggerFactory.getLogger(IcConfig.class);
	protected static ConfigParseOptions parseOptions =
			ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF).setAllowMissing(false);
	protected static ConfigResolveOptions resolveOptions =
			ConfigResolveOptions.defaults().setAllowUnresolved(true).setUseSystemEnvironment(true);
	public static final Config icConfig =
			ConfigFactory.parseResources("ic-backend.conf", parseOptions)
					.withFallback(ConfigFactory.parseResources("fallback.conf", parseOptions))
					.resolve(resolveOptions);
	private static final SecretsManager secretsManager = createSharedSecretsManager();
	private static final KeyPair keyPair = createKeyPair();

	public static String getBaseUrl() {
		return icConfig.getString("ic.baseUrl");
	}

	public static String getApiPrefix() {
		return icConfig.getString("ic.apiPrefix");
	}

	public static String getEnvironment() {
		return icConfig.getString("ic.environment");
	}

	public static Set<String> getCrisisPhoneNumbers() {
		final String FAKE = "+12155551212";
		return Set.of(FAKE);
	}

	public static String getJcaName() {
		return icConfig.getString("ic.jwt.jcaName");
	}

	public static KeyPair getKeyPair() {
		return keyPair;
	}

	public static String getCobaltBackendBaseUrl() {
		return icConfig.getString("ic.cobalt.baseUrl");
	}

	public static boolean isDevLogging() {
		return icConfig.getBoolean("ic.devLogging");
	}

	public static SecretsManager getSecretsManager() {
		return secretsManager;
	}

	private static SecretsManager createSharedSecretsManager() {
		Regions region = Regions.fromName(icConfig.getString("ic.aws.secretsmanager.region"));

		AwsConfig awsConfig;

		boolean useLocalstack = icConfig.getBoolean("ic.aws.secretsmanager.useLocalstack");

		if (useLocalstack) {
			Integer localstackPort = icConfig.getInt("ic.aws.secretsmanager.localstackPort");
			awsConfig = AwsConfig.forLocalstack(region, localstackPort);
		} else {
			boolean useEcsCredentialsProvider = false;

			if(useEcsCredentialsProvider) {
				awsConfig = AwsConfig.forAws(new EcsCredentialsProvider(), region);
			} else {
				String accessKey = icConfig.getString("ic.aws.accessKey");
				String secretKey = icConfig.getString("ic.aws.secretKey");

				AWSCredentialsProvider awsCredentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
				awsConfig = AwsConfig.forAws(awsCredentialsProvider, region);
			}
		}

		return new SecretsManager(awsConfig);
	}

	private static KeyPair createKeyPair() {
		String publicKeyLocation = icConfig.getString("ic.jwt.publicKey");
		String privateKeyLocation = icConfig.getString("ic.jwt.secretKey");

		String publicKeyAsString = getSecretsManager().fetchSecretString(publicKeyLocation).get();
		String privateKeyAsString = getSecretsManager().fetchSecretString(privateKeyLocation).get();

		return KeyManager.keyPairFromStringRepresentation(publicKeyAsString, privateKeyAsString);
	}

	@ThreadSafe
	protected static class EcsCredentialsProvider implements AWSCredentialsProvider {
		@Nonnull
		private static final Long REFRESH_INTERVAL_IN_MINUTES;

		@Nonnull
		private final ScheduledExecutorService scheduledExecutorService;
		@Nonnull
		private final OkHttpClient okHttpClient;
		@Nonnull
		private final Logger logger;

		@Nonnull
		private BasicSessionCredentials basicSessionCredentials;

		static {
			REFRESH_INTERVAL_IN_MINUTES = 1L;
		}

		public EcsCredentialsProvider() {
			this.okHttpClient = new OkHttpClient.Builder()
					.connectTimeout(10, TimeUnit.SECONDS)
					.writeTimeout(10, TimeUnit.SECONDS)
					.readTimeout(10, TimeUnit.SECONDS)
					.build();
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
			// Env var is provided by ECS
			String containerCredentialsRelativeUrl = trimToNull(System.getenv("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI"));

			if (containerCredentialsRelativeUrl == null)
				throw new RuntimeException("ESC Deployment Target required ENV var 'AWS_CONTAINER_CREDENTIALS_RELATIVE_URI' missing.");

			// ECS provides a magic URL that, if requested from an ECS Container, will return the security credentials for that task's role.
			String url = format("http://169.254.170.2%s", containerCredentialsRelativeUrl);

			Request request = new Request.Builder()
					.url(url)
					.build();

			try (Response response = getOkHttpClient().newCall(request).execute()) {
				String responseBody = response.body().string();

				if (response.code() >= 400)
					throw new IOException(format("Bad HTTP response (status %s). Response body was:\n%s", response.code(), responseBody));

				// Response:
				// "AccessKeyId": "ACCESS_KEY_ID",
				// "Expiration": "EXPIRATION_DATE",
				// "RoleArn": "TASK_ROLE_ARN",
				// "SecretAccessKey": "SECRET_ACCESS_KEY",
				// "Token": "SECURITY_TOKEN_STRING"
				AwsEcsRoleCredentialsResponse ecsRoleCredentialsResponse = new Gson().fromJson(responseBody, AwsEcsRoleCredentialsResponse.class);

				this.basicSessionCredentials = new BasicSessionCredentials(ecsRoleCredentialsResponse.getAccessKeyId(), ecsRoleCredentialsResponse.getSecretAccessKey(), ecsRoleCredentialsResponse.getToken());
			} catch(IOException e) {
				throw new UncheckedIOException(format("Unable to determine ECS credentials from %s", url), e);
			}
		}

		@Nonnull
		protected Long getRefreshIntervalInMinutes() {
			return REFRESH_INTERVAL_IN_MINUTES;
		}

		@Nonnull
		protected OkHttpClient getOkHttpClient() {
			return okHttpClient;
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
}
