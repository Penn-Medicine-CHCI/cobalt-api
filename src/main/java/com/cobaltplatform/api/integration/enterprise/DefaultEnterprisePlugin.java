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

package com.cobaltplatform.api.integration.enterprise;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.integration.epic.DefaultEpicBackendServiceAuthenticator;
import com.cobaltplatform.api.integration.epic.DefaultEpicClient;
import com.cobaltplatform.api.integration.epic.DefaultMyChartAuthenticator;
import com.cobaltplatform.api.integration.epic.EpicBackendServiceAccessToken;
import com.cobaltplatform.api.integration.epic.EpicBackendServiceAuthenticator;
import com.cobaltplatform.api.integration.epic.EpicBackendServiceConfiguration;
import com.cobaltplatform.api.integration.epic.EpicClient;
import com.cobaltplatform.api.integration.epic.EpicConfiguration;
import com.cobaltplatform.api.integration.epic.EpicEmpCredentials;
import com.cobaltplatform.api.integration.epic.MyChartAccessToken;
import com.cobaltplatform.api.integration.epic.MyChartAuthenticator;
import com.cobaltplatform.api.integration.epic.MyChartConfiguration;
import com.cobaltplatform.api.integration.google.DefaultGoogleAnalyticsDataClient;
import com.cobaltplatform.api.integration.google.DefaultGoogleBigQueryClient;
import com.cobaltplatform.api.integration.google.GoogleAnalyticsDataClient;
import com.cobaltplatform.api.integration.google.GoogleBigQueryClient;
import com.cobaltplatform.api.integration.google.MockGoogleAnalyticsDataClient;
import com.cobaltplatform.api.integration.google.MockGoogleBigQueryClient;
import com.cobaltplatform.api.integration.microsoft.DefaultMicrosoftAuthenticator;
import com.cobaltplatform.api.integration.microsoft.DefaultMicrosoftClient;
import com.cobaltplatform.api.integration.microsoft.MicrosoftAccessToken;
import com.cobaltplatform.api.integration.microsoft.MicrosoftAuthenticator;
import com.cobaltplatform.api.integration.microsoft.MicrosoftClient;
import com.cobaltplatform.api.integration.microsoft.request.AccessTokenRequest;
import com.cobaltplatform.api.integration.mixpanel.DefaultMixpanelClient;
import com.cobaltplatform.api.integration.mixpanel.MixpanelClient;
import com.cobaltplatform.api.integration.mixpanel.MockMixpanelClient;
import com.cobaltplatform.api.integration.tableau.DefaultTableauClient;
import com.cobaltplatform.api.integration.tableau.TableauClient;
import com.cobaltplatform.api.integration.tableau.TableauDirectTrustCredential;
import com.cobaltplatform.api.integration.twilio.DefaultTwilioRequestValidator;
import com.cobaltplatform.api.integration.twilio.MockTwilioRequestValidator;
import com.cobaltplatform.api.integration.twilio.TwilioRequestValidator;
import com.cobaltplatform.api.messaging.MessageSender;
import com.cobaltplatform.api.messaging.call.CallMessage;
import com.cobaltplatform.api.messaging.call.ConsoleCallMessageSender;
import com.cobaltplatform.api.messaging.call.TwilioCallMessageSender;
import com.cobaltplatform.api.messaging.push.ConsolePushMessageSender;
import com.cobaltplatform.api.messaging.push.GoogleFcmPushMessageSender;
import com.cobaltplatform.api.messaging.push.PushMessage;
import com.cobaltplatform.api.messaging.sms.ConsoleSmsMessageSender;
import com.cobaltplatform.api.messaging.sms.SmsMessage;
import com.cobaltplatform.api.messaging.sms.TwilioSmsMessageSender;
import com.cobaltplatform.api.model.db.ClientDevicePushTokenType.ClientDevicePushTokenTypeId;
import com.cobaltplatform.api.model.db.EpicBackendServiceAuthType;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.security.SigningCredentials;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.util.AwsSecretManagerClient;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Duration;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public abstract class DefaultEnterprisePlugin implements EnterprisePlugin {
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final AwsSecretManagerClient awsSecretManagerClient;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final LoadingCache<ExpensiveClientCacheKey, Object> expensiveClientCache;

	public DefaultEnterprisePlugin(@Nonnull InstitutionService institutionService,
																 @Nonnull AwsSecretManagerClient awsSecretManagerClient,
																 @Nonnull Configuration configuration) {
		requireNonNull(institutionService);
		requireNonNull(awsSecretManagerClient);
		requireNonNull(configuration);

		this.institutionService = institutionService;
		this.awsSecretManagerClient = awsSecretManagerClient;
		this.configuration = configuration;
		this.expensiveClientCache = createExpensiveClientCache();
	}

	@Nonnull
	@Override
	public GoogleBigQueryClient googleBigQueryClient() {
		return (GoogleBigQueryClient) getExpensiveClientCache().get(ExpensiveClientCacheKey.GOOGLE_BIG_QUERY);
	}

	@Nonnull
	@Override
	public GoogleAnalyticsDataClient googleAnalyticsDataClient() {
		return (GoogleAnalyticsDataClient) getExpensiveClientCache().get(ExpensiveClientCacheKey.GOOGLE_ANALYTICS_DATA);
	}

	@Nonnull
	@Override
	public MixpanelClient mixpanelClient() {
		return (MixpanelClient) getExpensiveClientCache().get(ExpensiveClientCacheKey.MIXPANEL);
	}

	@Nonnull
	@Override
	public Optional<MicrosoftAuthenticator> microsoftAuthenticator() {
		return (Optional<MicrosoftAuthenticator>) getExpensiveClientCache().get(ExpensiveClientCacheKey.MICROSOFT_AUTHENTICATOR);
	}

	@Nonnull
	public Optional<MicrosoftClient> microsoftTeamsClientForDaemon() {
		return (Optional<MicrosoftClient>) getExpensiveClientCache().get(ExpensiveClientCacheKey.MICROSOFT_TEAMS_CLIENT_FOR_DAEMON);
	}

	@Nonnull
	@Override
	public Optional<MyChartAuthenticator> myChartAuthenticator() {
		return (Optional<MyChartAuthenticator>) getExpensiveClientCache().get(ExpensiveClientCacheKey.MYCHART_AUTHENTICATOR);
	}

	@Nonnull
	@Override
	public Optional<TableauClient> tableauClient() {
		return (Optional<TableauClient>) getExpensiveClientCache().get(ExpensiveClientCacheKey.TABLEAU);
	}

	@Nonnull
	@Override
	public Optional<EpicClient> epicClientForBackendService() {
		return (Optional<EpicClient>) getExpensiveClientCache().get(ExpensiveClientCacheKey.EPIC_CLIENT_FOR_BACKEND_SERVICE);
	}

	@Nonnull
	@Override
	public Optional<EpicClient> epicClientForPatient(@Nonnull MyChartAccessToken myChartAccessToken) {
		requireNonNull(myChartAccessToken);

		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();

		String clientId = institution.getEpicClientId();
		String baseUrl = institution.getEpicBaseUrl();

		EpicConfiguration epicConfiguration = new EpicConfiguration.Builder(myChartAccessToken, clientId, baseUrl).build();

		return Optional.of(new DefaultEpicClient(epicConfiguration));
	}

	@Nonnull
	@Override
	public MessageSender<PushMessage> pushMessageSenderForPushTokenTypeId(@Nonnull ClientDevicePushTokenTypeId clientDevicePushTokenTypeId) {
		return (MessageSender<PushMessage>) getExpensiveClientCache().get(ExpensiveClientCacheKey.GOOGLE_FCM_PUSH_MESSAGE_SENDER);
	}

	@Nonnull
	@Override
	public MessageSender<SmsMessage> smsMessageSender() {
		return (MessageSender<SmsMessage>) getExpensiveClientCache().get(ExpensiveClientCacheKey.TWILIO_SMS_MESSAGE_SENDER);
	}

	@Nonnull
	@Override
	public MessageSender<CallMessage> callMessageSender() {
		return (MessageSender<CallMessage>) getExpensiveClientCache().get(ExpensiveClientCacheKey.TWILIO_CALL_MESSAGE_SENDER);
	}

	@Nonnull
	@Override
	public TwilioRequestValidator twilioRequestValidator() {
		return (TwilioRequestValidator) getExpensiveClientCache().get(ExpensiveClientCacheKey.TWILIO_REQUEST_VALIDATOR);
	}

	@Nonnull
	protected LoadingCache<ExpensiveClientCacheKey, Object> createExpensiveClientCache() {
		// Keep expensive clients around for a little bit so we don't recreate them constantly.
		// We keep expiration short so changes to configuration/database (for example) can be reflected
		// without requiring a redeploy of the application
		return Caffeine.newBuilder()
				.maximumSize(25)
				.expireAfterWrite(Duration.ofMinutes(5))
				.refreshAfterWrite(Duration.ofMinutes(1))
				.build(expensiveClientCacheKey -> {
					requireNonNull(expensiveClientCacheKey);

					if (expensiveClientCacheKey == ExpensiveClientCacheKey.GOOGLE_BIG_QUERY)
						return uncachedGoogleBigQueryClient();
					if (expensiveClientCacheKey == ExpensiveClientCacheKey.GOOGLE_ANALYTICS_DATA)
						return uncachedGoogleAnalyticsDataClient();
					if (expensiveClientCacheKey == ExpensiveClientCacheKey.MIXPANEL)
						return uncachedMixpanelClient();
					if (expensiveClientCacheKey == ExpensiveClientCacheKey.MICROSOFT_AUTHENTICATOR)
						return uncachedMicrosoftAuthenticator();
					if (expensiveClientCacheKey == ExpensiveClientCacheKey.MICROSOFT_TEAMS_CLIENT_FOR_DAEMON)
						return uncachedMicrosoftTeamsClientForDaemon();
					if (expensiveClientCacheKey == ExpensiveClientCacheKey.MYCHART_AUTHENTICATOR)
						return uncachedMyChartAuthenticator();
					if (expensiveClientCacheKey == ExpensiveClientCacheKey.EPIC_CLIENT_FOR_BACKEND_SERVICE)
						return uncachedEpicClientForBackendService();
					if (expensiveClientCacheKey == ExpensiveClientCacheKey.GOOGLE_FCM_PUSH_MESSAGE_SENDER)
						return uncachedGoogleFcmPushMessageSender();
					if (expensiveClientCacheKey == ExpensiveClientCacheKey.TWILIO_SMS_MESSAGE_SENDER)
						return uncachedTwilioSmsMessageSender();
					if (expensiveClientCacheKey == ExpensiveClientCacheKey.TWILIO_CALL_MESSAGE_SENDER)
						return uncachedTwilioCallMessageSender();
					if (expensiveClientCacheKey == ExpensiveClientCacheKey.TWILIO_REQUEST_VALIDATOR)
						return uncachedTwilioRequestValidator();
					if (expensiveClientCacheKey == ExpensiveClientCacheKey.TABLEAU)
						return uncachedTableauClient();

					throw new IllegalStateException(format("Unexpected value %s was provided for %s",
							expensiveClientCacheKey.name(), ExpensiveClientCacheKey.class.getSimpleName()));
				});
	}

	@Nonnull
	protected GoogleBigQueryClient uncachedGoogleBigQueryClient() {
		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();

		String googleBigQueryResourceId = institution.getGoogleBigQueryResourceId();
		String googleReportingServiceAccountPrivateKey = institution.getGoogleReportingServiceAccountPrivateKey();

		if (googleBigQueryResourceId == null || googleReportingServiceAccountPrivateKey == null)
			return new MockGoogleBigQueryClient();

		return new DefaultGoogleBigQueryClient(googleBigQueryResourceId, googleReportingServiceAccountPrivateKey);
	}

	@Nonnull
	protected GoogleAnalyticsDataClient uncachedGoogleAnalyticsDataClient() {
		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();

		String googleGa4PropertyId = institution.getGoogleGa4PropertyId();
		String googleReportingServiceAccountPrivateKey = institution.getGoogleReportingServiceAccountPrivateKey();

		if (googleGa4PropertyId == null || googleReportingServiceAccountPrivateKey == null)
			return new MockGoogleAnalyticsDataClient();

		return new DefaultGoogleAnalyticsDataClient(googleGa4PropertyId, googleReportingServiceAccountPrivateKey);
	}

	@Nonnull
	protected MixpanelClient uncachedMixpanelClient() {
		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();

		Long mixpanelProjectId = institution.getMixpanelProjectId();
		String mixpanelServiceAccountUsername = institution.getMixpanelServiceAccountUsername();
		String mixpanelServiceAccountSecret = institution.getMixpanelServiceAccountSecret();

		if (mixpanelProjectId == null || mixpanelServiceAccountUsername == null || mixpanelServiceAccountSecret == null)
			return new MockMixpanelClient();

		return new DefaultMixpanelClient(mixpanelProjectId, mixpanelServiceAccountUsername, mixpanelServiceAccountSecret);
	}

	@Nonnull
	protected Optional<MicrosoftAuthenticator> uncachedMicrosoftAuthenticator() {
		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();

		if (institution.getMicrosoftTenantId() == null || institution.getMicrosoftClientId() == null)
			return Optional.empty();

		return Optional.of(new DefaultMicrosoftAuthenticator(
				institution.getMicrosoftTenantId(),
				institution.getMicrosoftClientId(),
				getConfiguration().getMicrosoftSigningCredentials()));
	}

	@Nonnull
	protected Optional<MyChartAuthenticator> uncachedMyChartAuthenticator() {
		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();

		MyChartConfiguration myChartConfiguration = new MyChartConfiguration();
		myChartConfiguration.setClientId(institution.getMyChartClientId());
		myChartConfiguration.setScope(institution.getMyChartScope());
		myChartConfiguration.setAud(institution.getMyChartAud());
		myChartConfiguration.setResponseType(institution.getMyChartResponseType());
		myChartConfiguration.setCallbackUrl(institution.getMyChartCallbackUrl());
		myChartConfiguration.setAuthorizeUrl(institution.getEpicAuthorizeUrl());
		myChartConfiguration.setTokenUrl(institution.getEpicTokenUrl());

		return Optional.of(new DefaultMyChartAuthenticator(myChartConfiguration));
	}

	@Nonnull
	protected Optional<EpicClient> uncachedEpicClientForBackendService() {
		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();
		EpicClient epicClient = null;

		if (institution.getEpicBackendServiceAuthTypeId() == EpicBackendServiceAuthType.EpicBackendServiceAuthTypeId.OAUTH_20) {
			String clientId = institution.getEpicClientId();
			String jwksKeyId = getConfiguration().getEpicCurrentEnvironmentKeyId();
			SigningCredentials signingCredentials = getConfiguration().getEpicCurrentEnvironmentSigningCredentials();
			String tokenUrl = institution.getEpicTokenUrl();
			String jwksUrl = format("%s/epic/fhir/jwks", getConfiguration().getBaseUrl());

			EpicBackendServiceConfiguration epicBackendServiceConfiguration = new EpicBackendServiceConfiguration(clientId, jwksKeyId, signingCredentials, tokenUrl, jwksUrl);
			EpicBackendServiceAuthenticator epicBackendServiceAuthenticator = new DefaultEpicBackendServiceAuthenticator(epicBackendServiceConfiguration);

			EpicBackendServiceAccessToken epicBackendServiceAccessToken = epicBackendServiceAuthenticator.obtainAccessTokenFromBackendServiceJwt();
			EpicConfiguration epicConfiguration = new EpicConfiguration.Builder(epicBackendServiceAccessToken, institution.getEpicClientId(), institution.getEpicBaseUrl())
					.build();

			epicClient = new DefaultEpicClient(epicConfiguration);
		} else if (institution.getEpicBackendServiceAuthTypeId() == EpicBackendServiceAuthType.EpicBackendServiceAuthTypeId.EMP_CREDENTIALS) {
			String clientId = institution.getEpicClientId();
			String userId = institution.getEpicUserId();
			String userIdType = institution.getEpicUserIdType();
			String username = institution.getEpicUsername();
			String password = institution.getEpicPassword();

			EpicEmpCredentials epicEmpCredentials = new EpicEmpCredentials(clientId, userId, userIdType, username, password);
			EpicConfiguration epicConfiguration = new EpicConfiguration.Builder(epicEmpCredentials, institution.getEpicClientId(), institution.getEpicBaseUrl())
					.build();

			epicClient = new DefaultEpicClient(epicConfiguration);
		}

		return Optional.ofNullable(epicClient);
	}

	@Nonnull
	protected MessageSender<PushMessage> uncachedGoogleFcmPushMessageSender() {
		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();

		if (!institution.getGoogleFcmPushNotificationsEnabled())
			return new ConsolePushMessageSender();

		// Read client secret from AWS Secrets Manager
		String googleFcmServiceAccountPrivateKey = getAwsSecretManagerClient().getSecretString(format("%s-google-fcm-service-account-private-key-%s",
				getConfiguration().getAmazonAwsSecretsManagerContext().get(), getInstitutionId().name()));

		return new GoogleFcmPushMessageSender(googleFcmServiceAccountPrivateKey);
	}

	@Nonnull
	protected MessageSender<SmsMessage> uncachedTwilioSmsMessageSender() {
		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();

		if (!institution.getSmsMessagesEnabled() || institution.getTwilioAccountSid() == null || institution.getTwilioFromNumber() == null)
			return new ConsoleSmsMessageSender();

		// Read client secret from AWS Secrets Manager
		String twilioAuthToken = getAwsSecretManagerClient().getSecretString(format("%s-twilio-auth-token-%s",
				getConfiguration().getAmazonAwsSecretsManagerContext().get(), getInstitutionId().name()));

		return new TwilioSmsMessageSender.Builder(institution.getTwilioAccountSid(), twilioAuthToken)
				.twilioFromNumber(institution.getTwilioFromNumber())
				.twilioStatusCallbackUrl(format("%s/twilio/%s/message-status-callback", getConfiguration().getBaseUrl(), getInstitutionId().name()))
				.build();
	}

	@Nonnull
	protected MessageSender<CallMessage> uncachedTwilioCallMessageSender() {
		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();

		if (!institution.getCallMessagesEnabled() || institution.getTwilioAccountSid() == null || institution.getTwilioFromNumber() == null)
			return new ConsoleCallMessageSender();

		// Read client secret from AWS Secrets Manager
		String twilioAuthToken = getAwsSecretManagerClient().getSecretString(format("%s-twilio-auth-token-%s",
				getConfiguration().getAmazonAwsSecretsManagerContext().get(), getInstitutionId().name()));

		return new TwilioCallMessageSender.Builder(institution.getTwilioAccountSid(), twilioAuthToken)
				.twilioFromNumber(institution.getTwilioFromNumber())
				.twilioStatusCallbackUrl(format("%s/twilio/%s/call-status-callback", getConfiguration().getBaseUrl(), getInstitutionId().name()))
				.build();
	}

	@Nonnull
	protected TwilioRequestValidator uncachedTwilioRequestValidator() {
		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();

		if (institution.getTwilioAccountSid() == null)
			return new MockTwilioRequestValidator();

		// Read client secret from AWS Secrets Manager
		String twilioAuthToken = getAwsSecretManagerClient().getSecretString(format("%s-twilio-auth-token-%s",
				getConfiguration().getAmazonAwsSecretsManagerContext().get(), getInstitutionId().name()));

		return new DefaultTwilioRequestValidator(twilioAuthToken);
	}

	@Nonnull
	protected Optional<MicrosoftClient> uncachedMicrosoftTeamsClientForDaemon() {
		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();

		if (!institution.getMicrosoftTeamsEnabled())
			return Optional.empty();

		String microsoftTeamsClientId = trimToNull(institution.getMicrosoftTeamsClientId());
		String microsoftTeamsTenantId = trimToNull(institution.getMicrosoftTeamsTenantId());
		String microsoftTeamsUserId = trimToNull(institution.getMicrosoftTeamsUserId());

		if (microsoftTeamsClientId == null || microsoftTeamsTenantId == null || microsoftTeamsUserId == null)
			throw new IllegalStateException(format("Microsoft Teams is enabled for %s but required values are missing on institution record", getInstitutionId().name()));

		// Read client secret from AWS Secrets Manager
		String clientSecret = getAwsSecretManagerClient().getSecretString(format("%s-microsoft-teams-client-secret-%s",
				getConfiguration().getAmazonAwsSecretsManagerContext().get(), getInstitutionId().name()));

		MicrosoftAuthenticator microsoftAuthenticator = new DefaultMicrosoftAuthenticator(microsoftTeamsTenantId,
				microsoftTeamsClientId, getConfiguration().getMicrosoftSigningCredentials());

		AccessTokenRequest accessTokenRequest = new AccessTokenRequest();
		accessTokenRequest.setClientSecret(clientSecret);
		accessTokenRequest.setScope("https://graph.microsoft.com/.default");
		accessTokenRequest.setGrantType("client_credentials");

		MicrosoftAccessToken microsoftAccessToken = microsoftAuthenticator.obtainAccessToken(accessTokenRequest);

		return Optional.of(new DefaultMicrosoftClient(() -> microsoftAccessToken));
	}

	@Nonnull
	protected Optional<TableauClient> uncachedTableauClient() {
		Institution institution = getInstitutionService().findInstitutionById(getInstitutionId()).get();

		if (!institution.getMicrosoftTeamsEnabled())
			return Optional.empty();

		String clientId = trimToNull(institution.getTableauClientId());
		String apiBaseUrl = trimToNull(institution.getTableauApiBaseUrl());

		if (clientId == null || apiBaseUrl == null)
			throw new IllegalStateException(format("Tableau is enabled for %s but required values are missing on institution record", getInstitutionId().name()));

		// Read client secret from AWS Secrets Manager
		String secretId = getAwsSecretManagerClient().getSecretString(format("%s-tableau-secret-id-%s",
				getConfiguration().getAmazonAwsSecretsManagerContext().get(), getInstitutionId().name()));
		String secretValue = getAwsSecretManagerClient().getSecretString(format("%s-tableau-secret-value-%s",
				getConfiguration().getAmazonAwsSecretsManagerContext().get(), getInstitutionId().name()));

		TableauDirectTrustCredential directTrustCredential = new TableauDirectTrustCredential(clientId, secretId, secretValue);
		return Optional.of(new DefaultTableauClient(apiBaseUrl, directTrustCredential));
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionService;
	}

	@Nonnull
	protected AwsSecretManagerClient getAwsSecretManagerClient() {
		return this.awsSecretManagerClient;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	private LoadingCache<ExpensiveClientCacheKey, Object> getExpensiveClientCache() {
		return this.expensiveClientCache;
	}

	enum ExpensiveClientCacheKey {
		GOOGLE_BIG_QUERY,
		GOOGLE_ANALYTICS_DATA,
		MIXPANEL,
		MICROSOFT_AUTHENTICATOR,
		MYCHART_AUTHENTICATOR,
		MICROSOFT_TEAMS_CLIENT_FOR_DAEMON,
		EPIC_CLIENT_FOR_BACKEND_SERVICE,
		GOOGLE_FCM_PUSH_MESSAGE_SENDER,
		TWILIO_SMS_MESSAGE_SENDER,
		TWILIO_CALL_MESSAGE_SENDER,
		TWILIO_REQUEST_VALIDATOR,
		TABLEAU
	}
}
