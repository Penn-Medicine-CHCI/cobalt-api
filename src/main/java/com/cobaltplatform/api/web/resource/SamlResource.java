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

package com.cobaltplatform.api.web.resource;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.model.api.request.CreateScreeningSessionRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.AuditLog;
import com.cobaltplatform.api.model.db.AuditLogEvent.AuditLogEventId;
import com.cobaltplatform.api.model.db.ClientDeviceType.ClientDeviceTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;
import com.cobaltplatform.api.model.security.SamlIdentityProvider;
import com.cobaltplatform.api.model.security.SigningTokenClaims;
import com.cobaltplatform.api.model.service.ScreeningQuestionContext;
import com.cobaltplatform.api.model.service.SsoAuthenticationAction;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AuditLogService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.ScreeningService;
import com.cobaltplatform.api.util.Authenticator;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.LinkGenerator;
import com.cobaltplatform.api.util.SamlManager;
import com.cobaltplatform.api.util.WebUtility;
import com.cobaltplatform.api.web.response.ResponseGenerator;
import com.google.gson.annotations.SerializedName;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.RequestCookie;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.BadRequestException;
import com.soklet.web.response.ApiResponse;
import com.soklet.web.response.BinaryResponse;
import com.soklet.web.response.CustomResponse;
import com.soklet.web.response.RedirectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class SamlResource {
	@Nonnull
	private final AccountService accountService;
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final ScreeningService screeningService;
	@Nonnull
	private final Authenticator authenticator;
	@Nonnull
	private final SamlManager samlManager;
	@Nonnull
	private final LinkGenerator linkGenerator;
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final AuditLogService auditLogService;

	@Inject
	public SamlResource(@Nonnull AccountService accountService,
											@Nonnull InstitutionService institutionService,
											@Nonnull ScreeningService screeningService,
											@Nonnull Authenticator authenticator,
											@Nonnull LinkGenerator linkGenerator,
											@Nonnull JsonMapper jsonMapper,
											@Nonnull Configuration configuration,
											@Nonnull AuditLogService auditLogService) {
		requireNonNull(accountService);
		requireNonNull(institutionService);
		requireNonNull(screeningService);
		requireNonNull(authenticator);
		requireNonNull(linkGenerator);
		requireNonNull(jsonMapper);
		requireNonNull(configuration);
		requireNonNull(auditLogService);

		this.accountService = accountService;
		this.institutionService = institutionService;
		this.screeningService = screeningService;
		this.authenticator = authenticator;
		this.linkGenerator = linkGenerator;
		this.jsonMapper = jsonMapper;
		this.configuration = configuration;
		this.samlManager = new SamlManager(SamlIdentityProvider.COBALT, getConfiguration());
		this.logger = LoggerFactory.getLogger(getClass());
		this.auditLogService = auditLogService;
	}

	// Note: this entire class is unused/replaced for enterprise, it's just an example for local dev
	@Nonnull
	@GET("/saml/login")
	public Object samlLogin(@Nonnull @QueryParameter("redirectBaseUrl") Optional<String> providedRedirectBaseUrl,
													@Nonnull @QueryParameter Optional<UserExperienceTypeId> userExperienceTypeId,
													@Nonnull @QueryParameter Optional<InstitutionId> institutionId,
													@Nonnull @QueryParameter Optional<String> signingToken,
													@Nonnull HttpServletRequest httpServletRequest,
													@Nonnull HttpServletResponse httpServletResponse) throws IOException {
		requireNonNull(providedRedirectBaseUrl);
		requireNonNull(userExperienceTypeId);
		requireNonNull(institutionId);
		requireNonNull(signingToken);
		requireNonNull(httpServletRequest);
		requireNonNull(httpServletResponse);

		// Normal SAML
		if (getConfiguration().getShouldUseRealAuthentication())
			throw new IllegalStateException();

		if (institutionId.isPresent()) {
			getLogger().debug("Found institution ID {} for example SAML login flow, setting cookie...", institutionId.get().name());
			httpServletResponse.addCookie(new Cookie("ssoInstitutionId", institutionId.get().name()));
		} else {
			getLogger().warn("No institution ID present for example SAML login flow.");
		}

		if (signingToken.isPresent()) {
			getLogger().debug("Found signing token {} for example SAML login flow, setting cookie...", signingToken.get());
			httpServletResponse.addCookie(new Cookie("ssoSigningToken", signingToken.get()));
		} else {
			getLogger().warn("No signing token present for example SAML login flow.");
		}

		// Default if not specified...
		String redirectBaseUrl = providedRedirectBaseUrl.orElse(getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(InstitutionId.COBALT, userExperienceTypeId.orElse(UserExperienceTypeId.PATIENT)).get());

		// Fake login page for testing
		String html = Files.readString(Paths.get("web/pages/saml-login.html"), StandardCharsets.UTF_8);

		html = html.replace("{{redirectBaseUrl}}", redirectBaseUrl);
		html = html.replace("{{samlAssertionPath}}", "/saml/assertion");

		String accountSelectOptions = getAccountService().findTestSsoAccounts(InstitutionId.COBALT).stream()
				.map(account -> format("<option value='%s'>%s</option>", account.getAccountId(), account.getEmailAddress()))
				.collect(Collectors.joining("\n"));

		html = html.replace("{{accountSelectOptions}}", accountSelectOptions);

		return ResponseGenerator.utf8Response(html, "text/html");
	}

	@Nonnull
	@GET("/saml/metadata")
	public BinaryResponse samlMetadata() {
		String xml = getSamlManager().generateSpMetadata();
		return ResponseGenerator.utf8Response(xml, "application/xml");
	}

	@Nonnull
	@POST("/saml/assertion")
	public Object samlAssertion(@Nonnull @QueryParameter("redirectBaseUrl") Optional<String> providedRedirectBaseUrl,
															@Nonnull @RequestCookie("ssoInstitutionId") Optional<Cookie> ssoInstitutionIdCookie,
															@Nonnull @RequestCookie("ssoSigningToken") Optional<Cookie> ssoSigningTokenCookie,
															@Nonnull @RequestBody String requestBody,
															@Nonnull HttpServletRequest httpServletRequest,
															@Nonnull HttpServletResponse httpServletResponse) {
		requireNonNull(providedRedirectBaseUrl);
		requireNonNull(requestBody);
		requireNonNull(httpServletRequest);
		requireNonNull(httpServletResponse);

		// Default if not specified...
		String redirectBaseUrl = providedRedirectBaseUrl.orElse(null);

		// Normal SAML flow
		if (getConfiguration().getShouldUseRealAuthentication())
			throw new UnsupportedOperationException();

		// Fake handling for testing
		String accountIdFromPost = trimToNull(httpServletRequest.getParameter("accountId"));

		if (accountIdFromPost == null)
			throw new BadRequestException("You must provide an accountId in your POST");

		InstitutionId institutionId = InstitutionId.COBALT;
		Set<InstitutionId> legalInstitutionIds = Set.of(InstitutionId.COBALT);
		UUID accountId = UUID.fromString(accountIdFromPost);
		String forceDestinationUrl = null;

		if (ssoInstitutionIdCookie.isPresent()) {
			getLogger().debug("Found SAML login institution ID cookie {}", ssoInstitutionIdCookie.get().getValue());

			try {
				InstitutionId cookieInstitutionId = InstitutionId.valueOf(ssoInstitutionIdCookie.get().getValue());

				if (!legalInstitutionIds.contains(cookieInstitutionId))
					throw new IllegalArgumentException(format("Illegal institution specified: %s. Must be one of %s", cookieInstitutionId, legalInstitutionIds));

				getLogger().debug("Using institution ID from cookie: {}", cookieInstitutionId.name());
				institutionId = cookieInstitutionId;
			} catch (Exception e) {
				getLogger().error(format("Unable to process institution ID cookie %s", ssoInstitutionIdCookie), e);
			}

			// Delete the cookie
			ssoInstitutionIdCookie.get().setMaxAge(0);
			httpServletResponse.addCookie(ssoInstitutionIdCookie.get());
		} else {
			getLogger().warn("No SAML login institution ID cookie found, defaulting institution ID to {}.", institutionId);
		}

		if (ssoSigningTokenCookie.isPresent()) {
			String signingToken = ssoSigningTokenCookie.get().getValue();
			getLogger().debug("Found SAML login signing token cookie {}", signingToken);

			try {
				// Acquire strongly-typed claim data from token claims after validating them
				SigningTokenClaims rawSigningTokenClaims = getAuthenticator().validateSigningToken(signingToken);
				SsoSigningTokenClaims ssoSigningTokenClaims = getJsonMapper().fromJson(getJsonMapper().toJson(rawSigningTokenClaims.getClaims()), SsoSigningTokenClaims.class);

				Set<SsoAuthenticationAction> ssoAuthenticationActions = ssoSigningTokenClaims.getSsoAuthenticationActions();

				// If we need to upgrade the account, do so
				if (ssoAuthenticationActions.contains(SsoAuthenticationAction.UPGRADE_ACCOUNT)) {
					UUID claimsAccountId = ssoSigningTokenClaims.getAccountId();

					if (claimsAccountId == null)
						throw new IllegalStateException(format("%s.%s specified but no account ID (subject) specified",
								SsoAuthenticationAction.class.getSimpleName(), SsoAuthenticationAction.CREATE_SCREENING_SESSION.name()));

					Account accountToUpgrade = getAccountService().findAccountById(claimsAccountId).get();

					// For now, only support upgrading anon and implicit anon accounts
					Set<AccountSourceId> upgradeableAccountSourceIds = Set.of(
							AccountSourceId.ANONYMOUS,
							AccountSourceId.ANONYMOUS_IMPLICIT
					);

					if (!upgradeableAccountSourceIds.contains(accountToUpgrade.getAccountSourceId()))
						throw new IllegalArgumentException(format("Cannot upgrade account ID %s because it has account source ID %s. Upgradeable account source IDs are: %s",
								accountId, accountToUpgrade.getAccountSourceId().name(), upgradeableAccountSourceIds));

					// TODO: upgrade account
					// If needed, set accountId = claimsAccountId;
				}

				// If we need to create a screening session, do so
				if (ssoAuthenticationActions.contains(SsoAuthenticationAction.CREATE_SCREENING_SESSION)) {
					UUID screeningFlowVersionId = ssoSigningTokenClaims.getScreeningFlowVersionId();

					if (screeningFlowVersionId == null)
						throw new IllegalStateException(format("%s.%s specified but no screening flow version ID specified",
								SsoAuthenticationAction.class.getSimpleName(), SsoAuthenticationAction.CREATE_SCREENING_SESSION.name()));

					CreateScreeningSessionRequest request = new CreateScreeningSessionRequest();
					request.setCreatedByAccountId(accountId);
					request.setTargetAccountId(accountId);
					request.setScreeningFlowVersionId(screeningFlowVersionId);

					// Create a new screening session
					UUID screeningSessionId = getScreeningService().createScreeningSession(request);

					// Pick the next screening question context for the new session (in practice, this will be the first question)
					ScreeningQuestionContext nextScreeningQuestionContext =
							getScreeningService().findNextUnansweredScreeningQuestionContextByScreeningSessionId(screeningSessionId).get();

					// Instruct FE to redirect to the first screening question context immediately after authentication
					forceDestinationUrl = format("/screening-questions/%s", nextScreeningQuestionContext.getScreeningQuestionContextId());
				}

				// Delete the cookie
				ssoSigningTokenCookie.get().setMaxAge(0);
				httpServletResponse.addCookie(ssoSigningTokenCookie.get());
			} catch (Exception e) {
				getLogger().error("Unable to process signing token, redirecting to default login screen", e);

				// Delete the cookie
				ssoSigningTokenCookie.get().setMaxAge(0);
				httpServletResponse.addCookie(ssoSigningTokenCookie.get());

				return new RedirectResponse(getLinkGenerator().generateDefaultLink(InstitutionId.COBALT, UserExperienceTypeId.PATIENT, ClientDeviceTypeId.WEB_BROWSER), RedirectResponse.Type.TEMPORARY);
			}
		}

		Account account = getAccountService().findAccountById(accountId).get();
		String accessToken = getAuthenticator().generateAccessToken(accountId, account.getRoleId());

		String destinationUrl;

		// Permit override of base URL
		if (redirectBaseUrl != null) {
			destinationUrl = getLinkGenerator().generateAuthenticationLink(redirectBaseUrl, accessToken);

			// Special hack for local dev to tack on "forceDestination" if present
			if (forceDestinationUrl != null)
				destinationUrl = WebUtility.appendQueryParameters(destinationUrl, Map.of("forceDestination", forceDestinationUrl));
		} else {
			// TODO: provide a way to say "default to X user experience type" if we don't have contextual information yet
			// (normally this is derived from the URL, e.g. a subdomain indicates STAFF or PATIENT, but in a SAML Assertion,
			// when an IdP does not support relay state, we might not know our context.
			// But - because we have the account, we could have a default stored at the account level, or at the institution-role level.
			// Or alternatively - and probably better - a cookie could be dropped prior to SAML flow that says what the user experience type should be.
			// For the moment, hardcode here.
			destinationUrl = getLinkGenerator().generateAuthenticationLink(InstitutionId.COBALT, UserExperienceTypeId.PATIENT, ClientDeviceTypeId.WEB_BROWSER, accessToken, forceDestinationUrl);
		}

		return new RedirectResponse(destinationUrl, RedirectResponse.Type.TEMPORARY);
	}

	@NotThreadSafe
	protected static class SsoSigningTokenClaims {
		@Nullable
		private UUID screeningFlowVersionId;
		@Nullable
		private Set<SsoAuthenticationAction> ssoAuthenticationActions;
		@Nullable
		@SerializedName("sub")
		private UUID accountId;

		@Nullable
		public UUID getAccountId() {
			return this.accountId;
		}

		public void setAccountId(@Nullable UUID accountId) {
			this.accountId = accountId;
		}

		@Nullable
		public UUID getScreeningFlowVersionId() {
			return this.screeningFlowVersionId;
		}

		public void setScreeningFlowVersionId(@Nullable UUID screeningFlowVersionId) {
			this.screeningFlowVersionId = screeningFlowVersionId;
		}

		@Nullable
		public Set<SsoAuthenticationAction> getSsoAuthenticationActions() {
			return this.ssoAuthenticationActions;
		}

		public void setSsoAuthenticationActions(@Nullable Set<SsoAuthenticationAction> ssoAuthenticationActions) {
			this.ssoAuthenticationActions = ssoAuthenticationActions;
		}
	}

	@Nonnull
	@POST("/saml/logout")
	public ApiResponse samlHandlePostLogout(@Nonnull @RequestBody String requestBody) {
		return new ApiResponse();
	}

	@GET("/saml/logout")
	public ApiResponse samlHandleGetLogout() {
		return new ApiResponse();
	}

	@Nonnull
	@GET("/saml/initiate-logout")
	public CustomResponse samlRedirectToLogout(@Nonnull HttpServletRequest httpServletRequest,
																						 @Nonnull HttpServletResponse httpServletResponse) {
		requireNonNull(httpServletRequest);
		requireNonNull(httpServletResponse);

		AuditLog auditLog = new AuditLog();
		auditLog.setAuditLogEventId(AuditLogEventId.ACCOUNT_COBALT_SAML_LOGOUT);
		auditLog.setMessage(httpServletRequest.toString());
		getAuditLogService().audit(auditLog);

		String returnToUrl = format("%s/saml/logout", getConfiguration().getBaseUrl());
		getSamlManager().redirectToLogout(httpServletRequest, httpServletResponse, returnToUrl);
		return CustomResponse.instance();
	}

	@Nonnull
	protected AccountService getAccountService() {
		return accountService;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionService;
	}

	@Nonnull
	protected ScreeningService getScreeningService() {
		return this.screeningService;
	}

	@Nonnull
	protected Authenticator getAuthenticator() {
		return authenticator;
	}

	@Nonnull
	protected SamlManager getSamlManager() {
		return samlManager;
	}

	@Nonnull
	protected LinkGenerator getLinkGenerator() {
		return linkGenerator;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return this.jsonMapper;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}

	@Nonnull
	protected AuditLogService getAuditLogService() {
		return auditLogService;
	}
}