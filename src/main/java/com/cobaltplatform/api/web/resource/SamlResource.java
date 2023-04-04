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
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AuditLog;
import com.cobaltplatform.api.model.db.AuditLogEvent.AuditLogEventId;
import com.cobaltplatform.api.model.db.ClientDeviceType.ClientDeviceTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;
import com.cobaltplatform.api.model.security.SamlIdentityProvider;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AuditLogService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.util.Authenticator;
import com.cobaltplatform.api.util.LinkGenerator;
import com.cobaltplatform.api.util.SamlManager;
import com.cobaltplatform.api.web.response.ResponseGenerator;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.BadRequestException;
import com.soklet.web.response.ApiResponse;
import com.soklet.web.response.BinaryResponse;
import com.soklet.web.response.CustomResponse;
import com.soklet.web.response.RedirectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
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
	private final Authenticator authenticator;
	@Nonnull
	private final SamlManager samlManager;
	@Nonnull
	private final LinkGenerator linkGenerator;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final AuditLogService auditLogService;

	@Inject
	public SamlResource(@Nonnull AccountService accountService,
											@Nonnull InstitutionService institutionService,
											@Nonnull Authenticator authenticator,
											@Nonnull LinkGenerator linkGenerator,
											@Nonnull Configuration configuration,
											@Nonnull AuditLogService auditLogService) {
		requireNonNull(accountService);
		requireNonNull(institutionService);
		requireNonNull(authenticator);
		requireNonNull(linkGenerator);
		requireNonNull(configuration);
		requireNonNull(auditLogService);

		this.accountService = accountService;
		this.institutionService = institutionService;
		this.authenticator = authenticator;
		this.linkGenerator = linkGenerator;
		this.configuration = configuration;
		this.samlManager = new SamlManager(SamlIdentityProvider.COBALT, getConfiguration());
		this.logger = LoggerFactory.getLogger(getClass());
		this.auditLogService = auditLogService;
	}

	@Nonnull
	@GET("/saml/login")
	public Object samlLogin(@Nonnull @QueryParameter("redirectBaseUrl") Optional<String> providedRedirectBaseUrl,
													@Nonnull @QueryParameter Optional<UserExperienceTypeId> userExperienceTypeId,
													@Nonnull HttpServletRequest httpServletRequest,
													@Nonnull HttpServletResponse httpServletResponse) throws IOException {
		requireNonNull(providedRedirectBaseUrl);
		requireNonNull(httpServletRequest);
		requireNonNull(httpServletResponse);

		// Normal SAML
		if (getConfiguration().getShouldUseRealAuthentication())
			throw new IllegalStateException();

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

		UUID accountId = UUID.fromString(accountIdFromPost);
		Account account = getAccountService().findAccountById(accountId).get();
		String accessToken = getAuthenticator().generateAccessToken(accountId, account.getRoleId());

		String destinationUrl = null;

		// Permit override of base URL
		if (redirectBaseUrl != null)
			destinationUrl = getLinkGenerator().generateAuthenticationLink(redirectBaseUrl, accessToken);
		else
			// TODO: provide a way to say "default to X user experience type" if we don't have contextual information yet
			// (normally this is derived from the URL, e.g. a subdomain indicates STAFF or PATIENT, but in a SAML Assertion,
			// when an IdP does not support relay state, we might not know our context.
			// But - because we have the account, we could have a default stored at the account level, or at the institution-role level.
			// Or alternatively - and probably better - a cookie could be dropped prior to SAML flow that says what the user experience type should be.
			// For the moment, hardcode here.
			destinationUrl = getLinkGenerator().generateAuthenticationLink(InstitutionId.COBALT, UserExperienceTypeId.PATIENT, ClientDeviceTypeId.WEB_BROWSER, accessToken);

		return new RedirectResponse(destinationUrl, RedirectResponse.Type.TEMPORARY);
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