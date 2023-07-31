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

package com.cobaltplatform.api.web.request;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.context.CurrentContextExecutor.CurrentContextOperation;
import com.cobaltplatform.api.error.ErrorReporter;
import com.cobaltplatform.api.integration.epic.MyChartAccessToken;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionUrl;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;
import com.cobaltplatform.api.model.security.AccessTokenClaims;
import com.cobaltplatform.api.model.security.AccessTokenStatus;
import com.cobaltplatform.api.model.service.AccountSourceForInstitution;
import com.cobaltplatform.api.model.service.RemoteClient;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.FingerprintService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.util.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.cobaltplatform.api.util.WebUtility.extractValueFromRequest;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class CurrentContextRequestHandler {
	@Nonnull
	private static final String ACCESS_TOKEN_REQUEST_PROPERTY_NAME;
	@Nonnull
	private static final String LOCALE_REQUEST_PROPERTY_NAME;
	@Nonnull
	private static final String TIME_ZONE_REQUEST_PROPERTY_NAME;
	@Nonnull
	private static final String SESSION_TRACKING_ID_PROPERTY_NAME;
	@Nonnull
	private static final String FINGERPRINT_ID_PROPERTY_NAME;
	@Nonnull
	private static final String WEBAPP_BASE_URL_PROPERTY_NAME;
	@Nonnull
	private static final String WEBAPP_CURRENT_URL_PROPERTY_NAME;

	@Nonnull
	private static final String CURRENT_CONTEXT_LOGGING_KEY;

	@Nonnull
	private final CurrentContextExecutor currentContextExecutor;
	@Nonnull
	private final AccountService accountService;
	@Nonnull
	private final FingerprintService fingerprintService;
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final Authenticator authenticator;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final ErrorReporter errorReporter;
	@Nonnull
	private final Logger logger;

	static {
		ACCESS_TOKEN_REQUEST_PROPERTY_NAME = "X-Cobalt-Access-Token";
		LOCALE_REQUEST_PROPERTY_NAME = "X-Locale";
		TIME_ZONE_REQUEST_PROPERTY_NAME = "X-Time-Zone";
		SESSION_TRACKING_ID_PROPERTY_NAME = "X-Session-Tracking-Id";
		FINGERPRINT_ID_PROPERTY_NAME = "X-Cobalt-Fingerprint-Id";
		WEBAPP_BASE_URL_PROPERTY_NAME = "X-Cobalt-Webapp-Base-Url";
		WEBAPP_CURRENT_URL_PROPERTY_NAME = "X-Cobalt-Webapp-Current-Url";

		CURRENT_CONTEXT_LOGGING_KEY = "CURRENT_CONTEXT";
	}

	@Inject
	public CurrentContextRequestHandler(@Nonnull CurrentContextExecutor currentContextExecutor,
																			@Nonnull AccountService accountService,
																			@Nonnull InstitutionService institutionService,
																			@Nonnull FingerprintService fingerprintService,
																			@Nonnull Authenticator authenticator,
																			@Nonnull Configuration configuration,
																			@Nonnull ErrorReporter errorReporter) {
		requireNonNull(currentContextExecutor);
		requireNonNull(accountService);
		requireNonNull(institutionService);
		requireNonNull(fingerprintService);
		requireNonNull(authenticator);
		requireNonNull(configuration);
		requireNonNull(errorReporter);

		this.currentContextExecutor = currentContextExecutor;
		this.accountService = accountService;
		this.institutionService = institutionService;
		this.fingerprintService = fingerprintService;
		this.authenticator = authenticator;
		this.configuration = configuration;
		this.errorReporter = errorReporter;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	public void handle(@Nonnull HttpServletRequest httpServletRequest,
										 @Nonnull CurrentContextOperation currentContextOperation) {
		requireNonNull(httpServletRequest);
		requireNonNull(currentContextOperation);

		getErrorReporter().startScope();

		MyChartAccessToken myChartAccessToken = null;

		try {
			getErrorReporter().applyHttpServletRequest(httpServletRequest);

			Account account = null;

			// Try to load account data for access token
			String accessTokenValue = extractValueFromRequest(httpServletRequest, getAccessTokenRequestPropertyName()).orElse(null);
			AccessTokenStatus accessTokenStatus = null;

			if (accessTokenValue != null) {
				AccessTokenClaims accessTokenClaims = getAuthenticator().validateAccessToken(accessTokenValue).orElse(null);

				if (accessTokenClaims != null) {
					UUID accountId = accessTokenClaims.getAccountId();
					account = getAccountService().findAccountById(accountId).orElse(null);
					accessTokenStatus = getAuthenticator().determineAccessTokenStatus(accessTokenClaims);
					myChartAccessToken = accessTokenClaims.getMyChartAccessToken().orElse(null);
				}
			}

			// Start with default locale and override as needed
			String localeValue = extractValueFromRequest(httpServletRequest, getLocaleRequestPropertyName()).orElse(null);
			Locale locale = httpServletRequest.getLocale();

			if (account != null)
				locale = account.getLocale();

			if (localeValue != null)
				locale = Locale.forLanguageTag(localeValue);

			// Start with default time zone and override as needed
			Optional<String> timeZoneValue = extractValueFromRequest(httpServletRequest, getTimeZoneRequestPropertyName());
			ZoneId timeZone = ZoneId.of("UTC");

			if (account != null)
				timeZone = account.getTimeZone();

			if (timeZoneValue.isPresent()) {
				try {
					timeZone = ZoneId.of(timeZoneValue.get());
				} catch (Exception e) {
					// Bad timezone; ignore it
				}
			}

			RemoteClient remoteClient = RemoteClient.fromHttpServletRequest(httpServletRequest);

			Optional<String> sessionTrackingString = extractValueFromRequest(httpServletRequest, getSessionTrackingIdPropertyName());
			UUID sessionTrackingId = sessionTrackingString.isPresent() ? UUID.fromString(sessionTrackingString.get()) : null;
			AccountSourceForInstitution accountSource = null;

			if (account != null)
				accountSource = getAccountService().findAccountSourceByAccountId(account.getAccountId()).get();

			// Try to get fingerprint id
			String fingerprintIdValue = extractValueFromRequest(httpServletRequest, getFingerprintIdPropertyName()).orElse(null);

			if (fingerprintIdValue != null && account != null)
				getFingerprintService().storeFingerprintForAccount(account.getAccountId(), fingerprintIdValue);

			// We use webappBaseUrl to derive the institution context for this request (IOW - the URL the user sees in their browser drives the institution)
			String webappBaseUrl = extractValueFromRequest(httpServletRequest, getWebappBaseUrlPropertyName()).orElse(null);
			// TODO: this new property should replace the old webappBaseUrl above since that can be calculated from this
			String webappCurrentUrl = extractValueFromRequest(httpServletRequest, getWebappCurrentUrlPropertyName()).orElse(null);

			// In general, webappBaseUrl is usually non-null (clients should always include the X-Cobalt-Webapp-Base-Url header).
			// However, in special cases like an OAuth callback, we won't get that header because we can't control how we're called.
			Institution institution = getInstitutionService().findInstitutionByWebappBaseUrl(webappBaseUrl).orElse(null);

			if (account == null && institution == null) {
				// If no signed-in account or X-Cobalt-Webapp-Base-Url header, assume default COBALT institution.
				// This would be the case for an OAuth callback, for example
				institution = getInstitutionService().findInstitutionById(InstitutionId.COBALT).get();
			} else if (account != null && institution == null) {
				getLogger().debug("This request did not specify its institution via {}, so current context will default to {}, " +
						"which is associated with account ID {}", getWebappBaseUrlPropertyName(), account.getInstitutionId().name(), account.getAccountId());
			} else if (account != null && institution != null && !Objects.equals(account.getInstitutionId(), institution.getInstitutionId())) {
				// It's illegal to access an account outside of its own institution's context
				throw new IllegalStateException(format("Account ID %s is associated with %s but is being accessed in the context of %s",
						account.getAccountId(), account.getInstitutionId().name(), institution.getInstitutionId().name()));
			}

			InstitutionUrl institutionUrl = getInstitutionService().findInstitutionUrlByWebappBaseUrl(webappBaseUrl).orElse(null);
			UserExperienceTypeId userExperienceTypeId = institutionUrl == null ? null : institutionUrl.getUserExperienceTypeId();

			CurrentContext.Builder currentContextBuilder = account == null
					? new CurrentContext.Builder(institution.getInstitutionId(), locale, timeZone)
					: new CurrentContext.Builder(account, locale, timeZone);

			CurrentContext currentContext = currentContextBuilder
					.accessToken(accessTokenValue)
					.accessTokenStatus(accessTokenStatus)
					.remoteClient(remoteClient)
					.webappBaseUrl(webappBaseUrl)
					.webappCurrentUrl(webappCurrentUrl)
					.userExperienceTypeId(userExperienceTypeId)
					.sessionTrackingId(sessionTrackingId)
					.accountSource(accountSource)
					.fingerprintId(fingerprintIdValue)
					.myChartAccessToken(myChartAccessToken)
					.build();

			String currentContextDescription;

			if (account != null) {
				String accountIdentifier = account.getEmailAddress() == null ? "[anonymous]" : account.getEmailAddress();
				getLogger().debug(format("Authenticated '%s' for this request.", accountIdentifier));

				currentContextDescription = format("%s, %s, %s",
						accountIdentifier,
						locale.toLanguageTag(),
						timeZone.getId());
			} else {
				getLogger().trace("This request is unauthenticated.");

				currentContextDescription = format("%s, %s, %s",
						remoteClient.getDescription(),
						locale.toLanguageTag(),
						timeZone.getId());
			}

			try {
				MDC.put(getCurrentContextLoggingKey(), currentContextDescription);
				getCurrentContextExecutor().execute(currentContext, currentContextOperation);
			} finally {
				MDC.remove(getCurrentContextLoggingKey());
			}
		} finally {
			getErrorReporter().endScope();
		}

	}

	@Nonnull
	public static String getAccessTokenRequestPropertyName() {
		return ACCESS_TOKEN_REQUEST_PROPERTY_NAME;
	}

	@Nonnull
	public static String getLocaleRequestPropertyName() {
		return LOCALE_REQUEST_PROPERTY_NAME;
	}

	@Nonnull
	public static String getTimeZoneRequestPropertyName() {
		return TIME_ZONE_REQUEST_PROPERTY_NAME;
	}

	@Nonnull
	public static String getSessionTrackingIdPropertyName() {
		return SESSION_TRACKING_ID_PROPERTY_NAME;
	}

	@Nonnull
	public static String getFingerprintIdPropertyName() {
		return FINGERPRINT_ID_PROPERTY_NAME;
	}

	@Nonnull
	public static String getWebappBaseUrlPropertyName() {
		return WEBAPP_BASE_URL_PROPERTY_NAME;
	}

	@Nonnull
	public static String getWebappCurrentUrlPropertyName() {
		return WEBAPP_CURRENT_URL_PROPERTY_NAME;
	}

	@Nonnull
	public static String getCurrentContextLoggingKey() {
		return CURRENT_CONTEXT_LOGGING_KEY;
	}

	@Nonnull
	protected CurrentContextExecutor getCurrentContextExecutor() {
		return this.currentContextExecutor;
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountService;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionService;
	}

	@Nonnull
	protected FingerprintService getFingerprintService() {
		return this.fingerprintService;
	}

	@Nonnull
	protected Authenticator getAuthenticator() {
		return this.authenticator;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected ErrorReporter getErrorReporter() {
		return this.errorReporter;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
