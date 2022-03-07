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
import com.cobaltplatform.api.error.ErrorReporter;
import com.cobaltplatform.api.integration.ic.IcClient;
import com.cobaltplatform.api.model.service.RemoteClient;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSource;
import com.cobaltplatform.api.model.security.AccessTokenClaims;
import com.cobaltplatform.api.model.security.AccessTokenStatus;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.FingerprintService;
import com.cobaltplatform.api.util.Authenticator;
import com.cobaltplatform.api.util.WebUtility;
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
import java.util.Optional;
import java.util.UUID;

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
	private static final String IC_SIGNING_TOKEN_REQUEST_PROPERTY_NAME;
	@Nonnull
	private static final String LOCALE_REQUEST_PROPERTY_NAME;
	@Nonnull
	private static final String TIME_ZONE_REQUEST_PROPERTY_NAME;
	@Nonnull
	private static final String SESSION_TRACKING_ID_PROPERTY_NAME;
	@Nonnull
	private static final String FINGERPRINT_ID_PROPERTY_NAME;

	@Nonnull
	private static final String CURRENT_CONTEXT_LOGGING_KEY;

	@Nonnull
	private final CurrentContextExecutor currentContextExecutor;
	@Nonnull
	private final AccountService accountService;
	@Nonnull
	private final FingerprintService fingerprintService;
	@Nonnull
	private final Authenticator authenticator;
	@Nonnull
	private final IcClient icClient;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final ErrorReporter errorReporter;
	@Nonnull
	private final Logger logger;

	static {
		ACCESS_TOKEN_REQUEST_PROPERTY_NAME = "X-Cobalt-Access-Token";
		IC_SIGNING_TOKEN_REQUEST_PROPERTY_NAME = "X-IC-Signing-Token";
		LOCALE_REQUEST_PROPERTY_NAME = "X-Locale";
		TIME_ZONE_REQUEST_PROPERTY_NAME = "X-Time-Zone";
		SESSION_TRACKING_ID_PROPERTY_NAME = "X-Session-Tracking-Id";
		FINGERPRINT_ID_PROPERTY_NAME = "X-Cobalt-Fingerprint-Id";

		CURRENT_CONTEXT_LOGGING_KEY = "CURRENT_CONTEXT";
	}

	@Inject
	public CurrentContextRequestHandler(@Nonnull CurrentContextExecutor currentContextExecutor,
																			@Nonnull AccountService accountService,
																			@Nonnull FingerprintService fingerprintService,
																			@Nonnull Authenticator authenticator,
																			@Nonnull IcClient icClient,
																			@Nonnull Configuration configuration,
																			@Nonnull ErrorReporter errorReporter) {
		requireNonNull(currentContextExecutor);
		requireNonNull(accountService);
		requireNonNull(fingerprintService);
		requireNonNull(authenticator);
		requireNonNull(icClient);
		requireNonNull(configuration);
		requireNonNull(errorReporter);

		this.currentContextExecutor = currentContextExecutor;
		this.accountService = accountService;
		this.fingerprintService = fingerprintService;
		this.authenticator = authenticator;
		this.icClient = icClient;
		this.configuration = configuration;
		this.errorReporter = errorReporter;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	public void handle(@Nonnull HttpServletRequest httpServletRequest,
										 @Nonnull CurrentContextExecutor.CurrentContextOperation currentContextOperation) {
		requireNonNull(httpServletRequest);
		requireNonNull(currentContextOperation);

		getErrorReporter().startScope();

		try {
			getErrorReporter().applyHttpServletRequest(httpServletRequest);

			Account account = null;

			// Try to load account data for access token
			String accessTokenValue = WebUtility.extractValueFromRequest(httpServletRequest, getAccessTokenRequestPropertyName()).orElse(null);
			AccessTokenStatus accessTokenStatus = null;

			if (accessTokenValue != null) {
				AccessTokenClaims accessTokenClaims = getAuthenticator().validateAccessToken(accessTokenValue).orElse(null);

				if (accessTokenClaims != null) {
					UUID accountId = accessTokenClaims.getAccountId();
					account = getAccountService().findAccountById(accountId).orElse(null);
					accessTokenStatus = getAuthenticator().determineAccessTokenStatus(accessTokenClaims);
				}
			}

			// Start with default locale and override as needed
			String localeValue = WebUtility.extractValueFromRequest(httpServletRequest, getLocaleRequestPropertyName()).orElse(null);
			Locale locale = httpServletRequest.getLocale();

			if (account != null)
				locale = account.getLocale();

			if (localeValue != null)
				locale = Locale.forLanguageTag(localeValue);

			// Start with default time zone and override as needed
			Optional<String> timeZoneValue = WebUtility.extractValueFromRequest(httpServletRequest, getTimeZoneRequestPropertyName());
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

			// Is this a signed IC request?
			String icSigningToken = WebUtility.extractValueFromRequest(httpServletRequest, getIcSigningTokenRequestPropertyName()).orElse(null);
			boolean signedByIc = false;

			if (icSigningToken != null)
				signedByIc = getIcClient().verifyIcSigningToken(icSigningToken);

			RemoteClient remoteClient = RemoteClient.fromHttpServletRequest(httpServletRequest);

			Optional<String> sessionTrackingString = WebUtility.extractValueFromRequest(httpServletRequest, getSessionTrackingIdPropertyName());
			UUID sessionTrackingId = sessionTrackingString.isPresent() ? UUID.fromString(sessionTrackingString.get()) : null;
			AccountSource accountSource = null;

			if (account != null)
				accountSource = getAccountService().findAccountSourceByAccountId(account.getAccountId()).orElse(null);

			// Try to get fingerprint id
			String fingerprintIdValue = WebUtility.extractValueFromRequest(httpServletRequest, getFingerprintIdPropertyName()).orElse(null);

			if (fingerprintIdValue != null && account != null) {
				getFingerprintService().storeFingerprintForAccount(account.getAccountId(), fingerprintIdValue);
			}

			CurrentContext currentContext = new CurrentContext.Builder(locale, timeZone)
					.accessToken(accessTokenValue)
					.accessTokenStatus(accessTokenStatus)
					.account(account)
					.remoteClient(remoteClient)
					.sessionTrackingId(sessionTrackingId)
					.signedByIc(signedByIc)
					.accountSource(accountSource)
					.fingerprintId(fingerprintIdValue)
					.build();

			String currentContextDescription = null;

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
	public static String getIcSigningTokenRequestPropertyName() {
		return IC_SIGNING_TOKEN_REQUEST_PROPERTY_NAME;
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
	public static String getCurrentContextLoggingKey() {
		return CURRENT_CONTEXT_LOGGING_KEY;
	}

	@Nonnull
	protected CurrentContextExecutor getCurrentContextExecutor() {
		return currentContextExecutor;
	}

	@Nonnull
	protected AccountService getAccountService() {
		return accountService;
	}

	@Nonnull
	protected FingerprintService getFingerprintService() {
		return fingerprintService;
	}

	@Nonnull
	protected Authenticator getAuthenticator() {
		return authenticator;
	}

	@Nonnull
	protected IcClient getIcClient() {
		return icClient;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected ErrorReporter getErrorReporter() {
		return errorReporter;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
