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
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.request.AcceptAccountConsentFormRequest;
import com.cobaltplatform.api.model.api.request.AccessTokenRequest;
import com.cobaltplatform.api.model.api.request.AccountRoleRequest;
import com.cobaltplatform.api.model.api.request.CreateAccountInviteRequest;
import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.api.request.CreateActivityTrackingRequest;
import com.cobaltplatform.api.model.api.request.CreateIcMpmAccountRequest;
import com.cobaltplatform.api.model.api.request.CreateIcOrderReportAccountRequest;
import com.cobaltplatform.api.model.api.request.FindGroupSessionsRequest;
import com.cobaltplatform.api.model.api.request.ForgotPasswordRequest;
import com.cobaltplatform.api.model.api.request.ResetPasswordRequest;
import com.cobaltplatform.api.model.api.request.UpdateAccountAccessTokenExpiration;
import com.cobaltplatform.api.model.api.request.UpdateAccountBetaStatusRequest;
import com.cobaltplatform.api.model.api.request.UpdateAccountEmailAddressRequest;
import com.cobaltplatform.api.model.api.request.UpdateAccountPhoneNumberRequest;
import com.cobaltplatform.api.model.api.request.UpdateAccountRoleRequest;
import com.cobaltplatform.api.model.api.request.UpdateBetaFeatureAlertRequest;
import com.cobaltplatform.api.model.api.response.AccountApiResponse;
import com.cobaltplatform.api.model.api.response.AccountApiResponse.AccountApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AccountApiResponse.AccountApiResponseSupplement;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseSupplement;
import com.cobaltplatform.api.model.api.response.AssessmentFormApiResponse.AssessmentFormApiResponseFactory;
import com.cobaltplatform.api.model.api.response.BetaFeatureAlertApiResponse.BetaFeatureAlertApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ContentApiResponse;
import com.cobaltplatform.api.model.api.response.ContentApiResponse.ContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupEventApiResponse;
import com.cobaltplatform.api.model.api.response.GroupEventApiResponse.GroupEventApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionApiResponse;
import com.cobaltplatform.api.model.api.response.GroupSessionApiResponse.GroupSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionApiResponse.InstitutionApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountInvite;
import com.cobaltplatform.api.model.db.AccountLoginRule;
import com.cobaltplatform.api.model.db.AccountSession;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.ActivityAction;
import com.cobaltplatform.api.model.db.ActivityType;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.Assessment;
import com.cobaltplatform.api.model.db.AuditLog;
import com.cobaltplatform.api.model.db.AuditLogEvent;
import com.cobaltplatform.api.model.db.AuditLogEvent.AuditLogEventId;
import com.cobaltplatform.api.model.db.BetaFeatureAlert;
import com.cobaltplatform.api.model.db.ClientDeviceType;
import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.db.GroupSession;
import com.cobaltplatform.api.model.db.GroupSessionStatus.GroupSessionStatusId;
import com.cobaltplatform.api.model.db.GroupSessionSystem.GroupSessionSystemId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.LoginDestination.LoginDestinationId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.security.IcSignedRequestRequired;
import com.cobaltplatform.api.model.service.GroupEvent;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.ActivityTrackingService;
import com.cobaltplatform.api.service.AppointmentService;
import com.cobaltplatform.api.service.AssessmentService;
import com.cobaltplatform.api.service.AuditLogService;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.ContentService;
import com.cobaltplatform.api.service.GroupEventService;
import com.cobaltplatform.api.service.GroupSessionService;
import com.cobaltplatform.api.service.IcService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.ProviderService;
import com.cobaltplatform.api.service.SessionService;
import com.cobaltplatform.api.util.Authenticator;
import com.cobaltplatform.api.util.LinkGenerator;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.WebUtility;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.lokalized.Strings;
import com.soklet.json.JSONObject;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PUT;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import com.soklet.web.response.CustomResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class AccountResource {
	@Nonnull
	private final AccountService accountService;
	@Nonnull
	private final IcService icService;
	@Nonnull
	private final GroupEventService groupEventService;
	@Nonnull
	private final GroupSessionService groupSessionService;
	@Nonnull
	private final ContentService contentService;
	@Nonnull
	private final AppointmentService appointmentService;
	@Nonnull
	private final AccountApiResponseFactory accountApiResponseFactory;
	@Nonnull
	private final GroupEventApiResponseFactory groupEventApiResponseFactory;
	@Nonnull
	private final GroupSessionApiResponseFactory groupSessionApiResponseFactory;
	@Nonnull
	private final ContentApiResponseFactory contentApiResponseFactory;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Authenticator authenticator;
	@Nonnull
	private final LinkGenerator linkGenerator;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final AuditLogService auditLogService;
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final InstitutionApiResponseFactory institutionApiResponseFactory;
	@Nonnull
	private final BetaFeatureAlertApiResponseFactory betaFeatureAlertApiResponseFactory;
	@Nonnull
	private final ActivityTrackingService activityTrackingService;
	@Nonnull
	private final Strings strings;
	@Nonnull
	final AppointmentApiResponseFactory appointmentApiResponseFactory;
	@Nonnull
	final AuthorizationService authorizationService;
	@Nonnull
	final ProviderService providerService;
	@Nonnull
	final SessionService sessionService;
	@Nonnull
	final AssessmentService assessmentService;
	@Nonnull
	final AssessmentFormApiResponseFactory assessmentFormApiResponseFactory;

	@Inject
	public AccountResource(@Nonnull AccountService accountService,
												 @Nonnull IcService icService,
												 @Nonnull GroupEventService groupEventService,
												 @Nonnull GroupSessionService groupSessionService,
												 @Nonnull ContentService contentService,
												 @Nonnull AppointmentService appointmentService,
												 @Nonnull AccountApiResponseFactory accountApiResponseFactory,
												 @Nonnull GroupEventApiResponseFactory groupEventApiResponseFactory,
												 @Nonnull GroupSessionApiResponseFactory groupSessionApiResponseFactory,
												 @Nonnull ContentApiResponseFactory contentApiResponseFactory,
												 @Nonnull Configuration configuration,
												 @Nonnull RequestBodyParser requestBodyParser,
												 @Nonnull Authenticator authenticator,
												 @Nonnull LinkGenerator linkGenerator,
												 @Nonnull Provider<CurrentContext> currentContextProvider,
												 @Nonnull AuditLogService auditLogService,
												 @Nonnull InstitutionService institutionService,
												 @Nonnull InstitutionApiResponseFactory institutionApiResponseFactory,
												 @Nonnull BetaFeatureAlertApiResponseFactory betaFeatureAlertApiResponseFactory,
												 @Nonnull ActivityTrackingService activityTrackingService,
												 @Nonnull Strings strings,
												 @Nonnull AppointmentApiResponseFactory appointmentApiResponseFactory,
												 @Nonnull AuthorizationService authorizationService,
												 @Nonnull ProviderService providerService,
												 @Nonnull SessionService sessionService,
												 @Nonnull AssessmentService assessmentService,
												 @Nonnull AssessmentFormApiResponseFactory assessmentFormApiResponseFactory) {
		requireNonNull(accountService);
		requireNonNull(icService);
		requireNonNull(groupEventService);
		requireNonNull(groupSessionService);
		requireNonNull(contentService);
		requireNonNull(appointmentService);
		requireNonNull(accountApiResponseFactory);
		requireNonNull(groupEventApiResponseFactory);
		requireNonNull(groupSessionApiResponseFactory);
		requireNonNull(contentApiResponseFactory);
		requireNonNull(configuration);
		requireNonNull(currentContextProvider);
		requireNonNull(authenticator);
		requireNonNull(linkGenerator);
		requireNonNull(requestBodyParser);
		requireNonNull(auditLogService);
		requireNonNull(institutionService);
		requireNonNull(institutionApiResponseFactory);
		requireNonNull(activityTrackingService);
		requireNonNull(strings);
		requireNonNull(appointmentApiResponseFactory);
		requireNonNull(authorizationService);
		requireNonNull(providerService);
		requireNonNull(sessionService);
		requireNonNull(assessmentService);
		requireNonNull(assessmentFormApiResponseFactory);

		this.accountService = accountService;
		this.icService = icService;
		this.groupEventService = groupEventService;
		this.groupSessionService = groupSessionService;
		this.contentService = contentService;
		this.appointmentService = appointmentService;
		this.accountApiResponseFactory = accountApiResponseFactory;
		this.groupEventApiResponseFactory = groupEventApiResponseFactory;
		this.groupSessionApiResponseFactory = groupSessionApiResponseFactory;
		this.contentApiResponseFactory = contentApiResponseFactory;
		this.configuration = configuration;
		this.currentContextProvider = currentContextProvider;
		this.requestBodyParser = requestBodyParser;
		this.authenticator = authenticator;
		this.linkGenerator = linkGenerator;
		this.logger = LoggerFactory.getLogger(getClass());
		this.auditLogService = auditLogService;
		this.institutionService = institutionService;
		this.institutionApiResponseFactory = institutionApiResponseFactory;
		this.betaFeatureAlertApiResponseFactory = betaFeatureAlertApiResponseFactory;
		this.activityTrackingService = activityTrackingService;
		this.strings = strings;
		this.appointmentApiResponseFactory = appointmentApiResponseFactory;
		this.authorizationService = authorizationService;
		this.providerService = providerService;
		this.sessionService = sessionService;
		this.assessmentService = assessmentService;
		this.assessmentFormApiResponseFactory = assessmentFormApiResponseFactory;
	}

	@Nonnull
	@GET("/accounts/{accountId}")
	@AuthenticationRequired
	public ApiResponse account(@Nonnull @PathParameter UUID accountId,
														 @Nonnull @QueryParameter Optional<List<AccountApiResponseSupplement>> supplements) {
		requireNonNull(accountId);
		requireNonNull(supplements);

		Account account = getAccountService().findAccountById(accountId).orElse(null);
		AuditLog auditLog = new AuditLog();
		auditLog.setAccountId(accountId);

		if (account == null) {
			auditLog.setAuditLogEventId(AuditLogEventId.ACCOUNT_LOOKUP_FAILURE);
			getAuditLogService().audit(auditLog);
			throw new NotFoundException();
		}

		auditLog.setAuditLogEventId(AuditLogEventId.ACCOUNT_LOOKUP_SUCCESS);
		getAuditLogService().audit(auditLog);

		Institution institution = getInstitutionService().findInstitutionById(account.getInstitutionId())
				.orElse(null);

		if (institution == null)
			throw new NotFoundException();

		// Permit pulling capabilities only
		Set<AccountApiResponseSupplement> finalSupplements = new HashSet<>();

		if (supplements.isPresent() && supplements.get().contains(AccountApiResponseSupplement.CAPABILITIES))
			finalSupplements.add(AccountApiResponseSupplement.CAPABILITIES);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("account", getAccountApiResponseFactory().create(account, finalSupplements));
			put("institution", getInstitutionApiResponseFactory().create(institution));
		}});
	}

	@Nonnull
	@POST("/accounts/access-token")
	public ApiResponse accountAccessToken(@Nonnull @RequestBody String requestBody,
																				@Nonnull HttpServletResponse httpServletResponse) {
		requireNonNull(requestBody);
		requireNonNull(httpServletResponse);

		AccessTokenRequest request = getRequestBodyParser().parse(requestBody, AccessTokenRequest.class);
		String accessToken = getAccountService().obtainAccessToken(request);
		Account account = getAccountService().findAccountByAccessToken(accessToken).get();

		AccountLoginRule accountLoginRule = getAccountService().findAccountLoginRuleByEmailAddress(account.getEmailAddress(), AccountSourceId.EMAIL_PASSWORD, account.getInstitutionId()).orElse(null);
		LoginDestinationId loginDestinationId = LoginDestinationId.COBALT_PATIENT;
		UUID pinnedAccountId = account.getAccountId();

		if (accountLoginRule != null) {
			loginDestinationId = accountLoginRule.getLoginDestinationId();

			// Update role to what's been specified in the DB
			getAccountService().updateAccountRole(new UpdateAccountRoleRequest() {{
				setAccountId(pinnedAccountId);
				setProviderId(accountLoginRule.getProviderId());
				setRoleId(accountLoginRule.getRoleId());
			}});

			// Update account access token values with what has been specified
			getAccountService().updateAccountAccessTokenExpiration(new UpdateAccountAccessTokenExpiration() {{
				setAccountId(pinnedAccountId);
				setAccessTokenExpirationInMinutes(accountLoginRule.getAccessTokenExpirationInMinutes());
				setAccessTokenShortExpirationInMinutes(accountLoginRule.getAccessTokenShortExpirationInMinutes());
			}});

			// Mark the rule as executed
			getAccountService().markAccountLoginRoleAsExecuted(accountLoginRule.getAccountLoginRuleId());

			// Reload account
			account = getAccountService().findAccountById(account.getAccountId()).get();
		}

		Account pinnedAccount = account;
		String destinationUrl = getLinkGenerator().generateAuthenticationLink(account.getInstitutionId(), loginDestinationId, ClientDeviceType.ClientDeviceTypeId.WEB_BROWSER, accessToken);

		CreateActivityTrackingRequest activityTrackingRequest = new CreateActivityTrackingRequest();
		activityTrackingRequest.setSessionTrackingId(getCurrentContext().getSessionTrackingId());
		activityTrackingRequest.setActivityActionId(ActivityAction.ActivityActionId.SIGN_IN);
		activityTrackingRequest.setActivityTypeId(ActivityType.ActivityTypeId.ACCOUNT);
		activityTrackingRequest.setContext(new JSONObject().put("accountId", account.getAccountId().toString()).toString());

		getActivityTrackingService().trackActivity(Optional.of(account), activityTrackingRequest);

		// TODO: remove this hack
		if (loginDestinationId == LoginDestinationId.IC_PANEL)
			destinationUrl = format("%s/accounts/ic/auth-redirect?accessToken=%s", getConfiguration().getBaseUrl(), WebUtility.urlEncode(accessToken));

		String pinnedDestinationUrl = destinationUrl;

		return new ApiResponse(new HashMap<String, Object>() {{
			put("account", getAccountApiResponseFactory().create(pinnedAccount));
			put("accessToken", accessToken);
			put("destinationUrl", pinnedDestinationUrl);
		}});
	}

	@Nonnull
	@GET("/accounts/ic/auth-redirect")
	public CustomResponse authRedirect(@Nonnull @QueryParameter String accessToken,
																		 @Nonnull HttpServletResponse httpServletResponse) throws IOException {
		requireNonNull(accessToken);
		requireNonNull(httpServletResponse);

		String icRedirectUrl;
		Cookie cookie = new Cookie("accessToken", accessToken);

		if (getConfiguration().getEnvironment().equals("local")) {
			cookie.setDomain("127.0.0.1");
			icRedirectUrl = format("http://127.0.0.1:8888/auth/redirect?accessToken=%s", WebUtility.urlEncode(accessToken));
		} else {
			throw new UnsupportedOperationException();
		}

		httpServletResponse.addCookie(cookie);

		httpServletResponse.setContentType("text/html");

		// Writes cookies to the response and lets the browser "settle" (no immediate 302 - do a 200 and then have a client-side redirect)
		// so the cookie is actually written for this domain
		String html = "<html>\n" +
				"<head><meta http-equiv='refresh' content=1;url='$IC_REDIRECT_URL'></head>\n" +
				"<body></body>\n" +
				"</html>";

		html = html.replace("$IC_REDIRECT_URL", icRedirectUrl);

		httpServletResponse.getWriter().print(html);

		return CustomResponse.instance();
	}

	@Nonnull
	@POST("/accounts/ic/order-report")
	@IcSignedRequestRequired
	public ApiResponse createIcOrderReportPatientAccount(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		CreateIcOrderReportAccountRequest request = getRequestBodyParser().parse(requestBody, CreateIcOrderReportAccountRequest.class);
		UUID accountId = getIcService().createOrUpdateIcPatientAccount(request);

		return generateAccountResponse(accountId);
	}

	@Nonnull
	@POST("/accounts/ic/mpm")
	@IcSignedRequestRequired
	public ApiResponse createIcMpmPatientAccount(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		CreateIcMpmAccountRequest request = getRequestBodyParser().parse(requestBody, CreateIcMpmAccountRequest.class);
		UUID accountId = getIcService().createOrUpdateIcPatientAccount(request);

		return generateAccountResponse(accountId);
	}

	@Nonnull
	protected ApiResponse generateAccountResponse(@Nonnull UUID accountId) {
		requireNonNull(accountId);

		Account account = getAccountService().findAccountById(accountId).get();
		String accessToken = getAuthenticator().generateAccessToken(accountId, account.getRoleId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("account", getAccountApiResponseFactory().create(account, Set.of(AccountApiResponseSupplement.EVERYTHING)));
			put("accessToken", accessToken);
		}});
	}

	@Nonnull
	@GET("/accounts/ic/{accountId}")
	@IcSignedRequestRequired
	public ApiResponse accountForIc(@Nonnull @PathParameter UUID accountId) {
		requireNonNull(accountId);

		Account account = getAccountService().findAccountById(accountId).orElse(null);

		if (account == null)
			throw new NotFoundException();

		Institution institution = getInstitutionService().findInstitutionById(account.getInstitutionId()).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("account", getAccountApiResponseFactory().create(account, Set.of(AccountApiResponseSupplement.EVERYTHING)));
			put("institution", getInstitutionApiResponseFactory().create(institution));
		}});
	}

	@Nonnull
	@GET("/accounts/{accountId}/recommendations")
	@AuthenticationRequired
	public ApiResponse accountRecommendations(@Nonnull @PathParameter UUID accountId) {
		requireNonNull(accountId);

		Account account = getAccountService().findAccountById(accountId).orElse(null);

		if (account == null)
			throw new NotFoundException();

		if (!account.getAccountId().equals(accountId))
			throw new AuthorizationException();

		final int MAXIMUM_GROUP_SESSIONS = 3;
		List<GroupSession> groupSessions = new ArrayList<>(getGroupSessionService().findGroupSessions(new FindGroupSessionsRequest() {{
			setGroupSessionStatusId(GroupSessionStatusId.ADDED);
			setInstitutionId(account.getInstitutionId());
		}}).getResults());

		Collections.sort(groupSessions, (groupSession1, groupSession2) -> {
			return groupSession1.getStartDateTime().compareTo(groupSession2.getStartDateTime());
		});

		// Don't show too many events
		if (groupSessions.size() > MAXIMUM_GROUP_SESSIONS)
			groupSessions = groupSessions.subList(0, MAXIMUM_GROUP_SESSIONS /* exclusive */);

		Institution institution = getInstitutionService().findInstitutionById(account.getInstitutionId()).get();

		final int MAXIMUM_GROUP_EVENTS = 3;
		List<GroupEvent> groupEvents = institution.getGroupSessionSystemId() != GroupSessionSystemId.ACUITY ? Collections.emptyList() : getGroupEventService().findGroupEventsByInstitutionId(account.getInstitutionId(), account.getTimeZone())
				.stream()
				// Only show events with available seats
				.filter(groupEvent -> groupEvent.getSeatsAvailable() > 0)
				.collect(Collectors.toList());

		// Don't show too many events
		if (groupEvents.size() > MAXIMUM_GROUP_EVENTS)
			groupEvents = groupEvents.subList(0, MAXIMUM_GROUP_EVENTS /* exclusive */);

		List<String> groupEventIds = groupEvents.stream().map((groupEvent) -> groupEvent.getGroupEventId()).collect(Collectors.toList());
		Map<String, Appointment> appointmentsByGroupEventId = getAppointmentService().findAppointmentsForGroupEvents(account.getAccountId(), groupEventIds);

		List<GroupEventApiResponse> groupEventApiResponses = groupEvents.stream()
				.map((groupEvent) -> getGroupEventApiResponseFactory().create(groupEvent, appointmentsByGroupEventId.get(groupEvent.getGroupEventId())))
				.collect(Collectors.toList());

		final int MAXIMUM_CONTENTS = 3;
		List<Content> contents = getContentService().findContentForAccount(account, Optional.empty(), Optional.empty());

		// Don't show too many content pieces
		if (contents.size() > MAXIMUM_CONTENTS)
			contents = contents.subList(0, MAXIMUM_CONTENTS /* exclusive */);
		else if (contents.size() < MAXIMUM_CONTENTS) {
			contents.addAll(getContentService().findAdditionalContentForAccount(account, contents, Optional.empty(), Optional.empty()));
			contents = contents.subList(0, min(contents.size(), MAXIMUM_CONTENTS));
		}

		List<ContentApiResponse> contentApiResponses = contents.stream()
				.map(content -> getContentApiResponseFactory().create(content))
				.collect(Collectors.toList());

		List<GroupSessionApiResponse> groupSessionApiResponses = groupSessions.stream()
				.map(groupSession -> getGroupSessionApiResponseFactory().create(groupSession))
				.collect(Collectors.toList());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("groupEvents", groupEventApiResponses);
			put("contents", contentApiResponses);
			put("groupSessions", groupSessionApiResponses);
		}});
	}

	@Nonnull
	@POST("/accounts/invite")
	public ApiResponse createAccountInvite(@Nonnull @RequestBody Optional<String> body) {
		requireNonNull(body);
		CreateAccountInviteRequest request = getRequestBodyParser().parse(body.get(), CreateAccountInviteRequest.class);

		UUID accountInviteId = getAccountService().createAccountInvite(request);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("accountInviteId", accountInviteId);
		}});
	}

	@Nonnull
	@PUT("/accounts/claim-invite/{accountInviteCode}")
	public ApiResponse createAccountClaimInvite(@Nonnull @PathParameter UUID accountInviteCode) {
		requireNonNull(accountInviteCode);

		AccountApiResponse account = null;
		Boolean inviteExpired = getAccountService().accountInviteExpired(accountInviteCode);

		if (!inviteExpired) {
			AccountInvite accountInvite = getAccountService().findAccountInviteByCode(accountInviteCode).get();
			Optional<Account> existingAccount = getAccountService().findAccountByEmailAddressAndAccountSourceId(accountInvite.getEmailAddress(), AccountSourceId.EMAIL_PASSWORD);
			if (!existingAccount.isPresent()) {
				UUID accountId = getAccountService().claimAccountInvite(accountInviteCode);
				account = getAccountApiResponseFactory().create(getAccountService().findAccountById(accountId).get());
			} else
				account = getAccountApiResponseFactory().create(existingAccount.get());
		}

		AccountApiResponse finalAccount = account;

		return new ApiResponse(new HashMap<String, Object>() {{
			put("inviteExpired", inviteExpired);
			put("account", finalAccount);
		}});
	}

	@Nonnull
	@POST("/accounts/resend-invite/{accountInviteId}")
	public ApiResponse resendAccountClaimInvite(@Nonnull @PathParameter UUID accountInviteId) {
		requireNonNull(accountInviteId);

		getAccountService().resendAccountVerificationEmail(accountInviteId);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("accountInviteId", accountInviteId);
		}});
	}


	@Nonnull
	@POST("/accounts")
	public ApiResponse createAccount(@Nonnull @RequestBody Optional<String> body) {
		requireNonNull(body);

		UUID accountId;

		// Optional request body is to support legacy frontend before we finish rolling out new multi-institution support
		if (body.isPresent()) {
			CreateAccountRequest request = getRequestBodyParser().parse(body.get(), CreateAccountRequest.class);
			String subdomain;

			if (trimToNull(request.getSubdomain()) != null)
				subdomain = request.getSubdomain();
			else
				subdomain = getConfiguration().getDefaultSubdomain();

			Institution institution = getInstitutionService().findInstitutionBySubdomain(subdomain);

			// For now - this is only to generate anonymous accounts
			accountId = getAccountService().createAccount(new CreateAccountRequest() {{
				setRoleId(RoleId.PATIENT);
				setInstitutionId(institution.getInstitutionId());
				setAccountSourceId(AccountSourceId.ANONYMOUS);
			}});
		} else {
			// For now - this is only to generate anonymous accounts
			accountId = getAccountService().createAccount(new CreateAccountRequest() {{
				setRoleId(RoleId.PATIENT);
				setInstitutionId(InstitutionId.COBALT);
				setAccountSourceId(AccountSourceId.ANONYMOUS);
			}});
		}

		Account account = getAccountService().findAccountById(accountId).get();

		AuditLog auditLog = new AuditLog();
		auditLog.setAccountId(account.getAccountId());
		auditLog.setAuditLogEventId(AuditLogEventId.ACCOUNT_CREATE);
		getAuditLogService().audit(auditLog);

		String accessToken = getAuthenticator().generateAccessToken(account.getAccountId(), account.getRoleId());

		CreateActivityTrackingRequest activityTrackingRequest = new CreateActivityTrackingRequest();
		activityTrackingRequest.setSessionTrackingId(getCurrentContext().getSessionTrackingId());
		activityTrackingRequest.setActivityActionId(ActivityAction.ActivityActionId.CREATE);
		activityTrackingRequest.setActivityTypeId(ActivityType.ActivityTypeId.ACCOUNT);
		activityTrackingRequest.setContext(new JSONObject().put("accountId", accountId.toString()).toString());

		getActivityTrackingService().trackActivity(Optional.of(account), activityTrackingRequest);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("account", getAccountApiResponseFactory().create(account));
			put("accessToken", accessToken);
		}});
	}

	@Nonnull
	@PUT("/accounts/{accountId}/email-address")
	@AuthenticationRequired
	public ApiResponse updateAccountEmailAddress(@Nonnull @PathParameter UUID accountId,
																							 @Nonnull @RequestBody String body) {
		requireNonNull(accountId);
		requireNonNull(body);

		Account currentAccount = getCurrentContext().getAccount().get();

		if (!currentAccount.getAccountId().equals(accountId))
			throw new AuthorizationException();

		Account account = getAccountService().findAccountById(accountId).orElse(null);

		if (account == null)
			throw new NotFoundException();

		UpdateAccountEmailAddressRequest request = getRequestBodyParser().parse(body, UpdateAccountEmailAddressRequest.class);
		request.setAccountId(accountId);

		getAccountService().updateAccountEmailAddress(request);

		Account updatedAccount = getAccountService().findAccountById(accountId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("account", getAccountApiResponseFactory().create(updatedAccount));
		}});
	}

	@Nonnull
	@PUT("/accounts/{accountId}/phone-number")
	@AuthenticationRequired
	public ApiResponse updateAccountPhoneNumber(@Nonnull @PathParameter UUID accountId,
																							@Nonnull @RequestBody String body) {
		requireNonNull(accountId);
		requireNonNull(body);

		Account currentAccount = getCurrentContext().getAccount().get();

		if (!currentAccount.getAccountId().equals(accountId))
			throw new AuthorizationException();

		Account account = getAccountService().findAccountById(accountId).orElse(null);

		if (account == null)
			throw new NotFoundException();

		UpdateAccountPhoneNumberRequest request = getRequestBodyParser().parse(body, UpdateAccountPhoneNumberRequest.class);
		request.setAccountId(accountId);

		getAccountService().updateAccountPhoneNumber(request);

		Account updatedAccount = getAccountService().findAccountById(accountId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("account", getAccountApiResponseFactory().create(updatedAccount));
		}});
	}

	@Nonnull
	@PUT("/accounts/{accountId}/consent-form-accepted")
	@AuthenticationRequired
	public ApiResponse updateAccountConsentFormAccepted(@Nonnull @PathParameter UUID accountId) {
		requireNonNull(accountId);

		Account currentAccount = getCurrentContext().getAccount().get();

		if (!currentAccount.getAccountId().equals(accountId))
			throw new AuthorizationException();

		Account account = getAccountService().findAccountById(accountId).orElse(null);

		if (account == null)
			throw new NotFoundException();

		AcceptAccountConsentFormRequest request = new AcceptAccountConsentFormRequest();
		request.setAccountId(accountId);

		getAccountService().updateAccountConsentFormAccepted(request);

		Account updatedAccount = getAccountService().findAccountById(accountId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("account", getAccountApiResponseFactory().create(updatedAccount));
		}});
	}

	@POST("/accounts/forgot-password")
	public ApiResponse forgotPassword(@Nonnull @RequestBody String body) {
		requireNonNull(body);

		ForgotPasswordRequest request = getRequestBodyParser().parse(body, ForgotPasswordRequest.class);
		getAccountService().forgotPassword(request);

		return new ApiResponse();
	}

	@POST("/accounts/reset-password")
	public ApiResponse resetPassword(@Nonnull @RequestBody String body) {
		requireNonNull(body);

		ResetPasswordRequest request = getRequestBodyParser().parse(body, ResetPasswordRequest.class);
		Account account = getAccountService().resetPassword(request).orElse(null);

		if (account == null)
			throw new ValidationException(getStrings().get("Sorry, we were unable to reset your password."));

		return new ApiResponse(new HashMap<String, Object>() {{
			put("account", getAccountApiResponseFactory().create(account));
		}});
	}

	@AuthenticationRequired
	@GET("/accounts/{accountId}/beta-feature-alerts")
	public ApiResponse betaFeatureAlerts(@Nonnull @PathParameter UUID accountId) {
		requireNonNull(accountId);

		// For now, you can only pull for yourself.  Later we could permit admins at your institution to do this too (for example)
		Account account = getCurrentContext().getAccount().get();
		if (!Objects.equals(account.getAccountId(), accountId))
			throw new AuthorizationException();

		List<BetaFeatureAlert> betaFeatureAlerts = getAccountService().findBetaFeatureAlertsByAccountId(accountId);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("betaFeatureAlerts", betaFeatureAlerts.stream()
					.map(betaFeatureAlert -> getBetaFeatureAlertApiResponseFactory().create(betaFeatureAlert))
					.collect(Collectors.toList()));
		}});
	}

	@AuthenticationRequired
	@PUT("/accounts/{accountId}/beta-feature-alerts")
	public ApiResponse updateBetaFeatureAlert(@Nonnull @PathParameter UUID accountId,
																						@Nonnull @RequestBody String body) {
		requireNonNull(accountId);
		requireNonNull(body);

		UpdateBetaFeatureAlertRequest request = getRequestBodyParser().parse(body, UpdateBetaFeatureAlertRequest.class);
		request.setAccountId(accountId);

		// For now, you can only update yourself.  Later we could permit admins at your institution to do this too (for example)
		Account account = getCurrentContext().getAccount().get();
		if (!Objects.equals(account.getAccountId(), accountId))
			throw new AuthorizationException();

		BetaFeatureAlert betaFeatureAlert = getAccountService().updateBetaFeatureAlert(request);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("betaFeatureAlert", getBetaFeatureAlertApiResponseFactory().create(betaFeatureAlert));
		}});
	}

	@AuthenticationRequired
	@PUT("/accounts/{accountId}/beta-status")
	public ApiResponse updateAccountBetaStatus(@Nonnull @PathParameter UUID accountId,
																						 @Nonnull @RequestBody String body) {
		requireNonNull(accountId);
		requireNonNull(body);

		UpdateAccountBetaStatusRequest request = getRequestBodyParser().parse(body, UpdateAccountBetaStatusRequest.class);
		request.setAccountId(accountId);

		// For now, you can only update yourself.  Later we could permit admins at your institution to do this too (for example)
		Account account = getCurrentContext().getAccount().get();
		if (!Objects.equals(account.getAccountId(), accountId))
			throw new AuthorizationException();

		getAccountService().updateAccountBetaStatus(request);
		Account updatedAccount = getAccountService().findAccountById(accountId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("account", getAccountApiResponseFactory().create(updatedAccount));
		}});
	}

	@AuthenticationRequired
	@GET("/accounts/{accountId}/appointment-details/{appointmentId}")
	public ApiResponse accountWithAppointmentDetails(@Nonnull @PathParameter UUID accountId,
																									 @Nonnull @PathParameter UUID appointmentId) {
		requireNonNull(accountId);
		requireNonNull(appointmentId);

		Account account = getCurrentContext().getAccount().get();
		Account appointmentAccount = getAccountService().findAccountById(accountId).orElse(null);
		AuditLog auditLog = new AuditLog();
		auditLog.setAccountId(account.getAccountId());

		if (appointmentAccount == null) {
			auditLog.setAuditLogEventId(AuditLogEventId.ACCOUNT_LOOKUP_FAILURE);
			getAuditLogService().audit(auditLog);
			throw new NotFoundException();
		}

		Appointment appointment = getAppointmentService().findAppointmentById(appointmentId).orElse(null);

		if (appointment == null)
			throw new NotFoundException();

		com.cobaltplatform.api.model.db.Provider provider = getProviderService().findProviderById(appointment.getProviderId()).orElse(null);

		if (provider == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canViewProviderCalendar(provider, account))
			throw new AuthorizationException();

		auditLog.setAuditLogEventId(AuditLogEventId.ACCOUNT_LOOKUP_SUCCESS);
		getAuditLogService().audit(auditLog);

		Set<AccountApiResponseSupplement> finalSupplements = new HashSet<>();
		finalSupplements.add(AccountApiResponseSupplement.EVERYTHING);

		List<Appointment> appointments = getAppointmentService().findUpcomingAppointmentsByAccountId(appointmentAccount.getAccountId(), getCurrentContext().getTimeZone());

		Map<String, Object> responseData = new HashMap<>();
		responseData.put("account", getAccountApiResponseFactory().create(appointmentAccount, finalSupplements));
		responseData.put("appointment", getAppointmentApiResponseFactory().create(appointment, Set.of(AppointmentApiResponseSupplement.PROVIDER, AppointmentApiResponseSupplement.APPOINTMENT_TYPE)));
		responseData.put("appointments", appointments.stream()
				.map((a) -> getAppointmentApiResponseFactory().create(a, Set.of(AppointmentApiResponseSupplement.PROVIDER, AppointmentApiResponseSupplement.APPOINTMENT_TYPE))).collect(Collectors.toList()));

		AccountSession intakeSession = getSessionService().findCurrentIntakeAssessmentForAccountAndProvider(appointmentAccount,
				provider.getProviderId(), appointment.getAppointmentTypeId(), true).orElse(null);

		if (intakeSession != null) {
			Assessment intakeAssessment = getAssessmentService().findAssessmentById(intakeSession.getAssessmentId()).orElse(null);
			responseData.put("assessment", getAssessmentFormApiResponseFactory().create(intakeAssessment, Optional.of(intakeSession)));
		}

		return new ApiResponse(responseData);
	}

	@AuthenticationRequired
	@POST("/accounts/{accountId}/role-request")
	public void processRoleRequest(@Nonnull @PathParameter UUID accountId,
																 @Nonnull @RequestBody String body) {
		requireNonNull(accountId);
		requireNonNull(body);

		AccountRoleRequest request = getRequestBodyParser().parse(body, AccountRoleRequest.class);
		request.setAccountId(accountId);

		Account currentAccount = getCurrentContext().getAccount().get();

		// You can only request for yourself for now
		if(!currentAccount.getAccountId().equals(accountId))
			throw new AuthorizationException();

		getAccountService().requestRoleForAccount(request);
	}

	@Nonnull
	protected AccountService getAccountService() {
		return accountService;
	}

	@Nonnull
	protected IcService getIcService() {
		return icService;
	}

	@Nonnull
	protected AccountApiResponseFactory getAccountApiResponseFactory() {
		return accountApiResponseFactory;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return requestBodyParser;
	}

	@Nonnull
	protected Authenticator getAuthenticator() {
		return authenticator;
	}

	@Nonnull
	protected LinkGenerator getLinkGenerator() {
		return linkGenerator;
	}

	@Nonnull
	protected GroupEventService getGroupEventService() {
		return groupEventService;
	}

	@Nonnull
	protected GroupSessionService getGroupSessionService() {
		return groupSessionService;
	}

	@Nonnull
	protected ContentService getContentService() {
		return contentService;
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return appointmentService;
	}

	@Nonnull
	protected GroupEventApiResponseFactory getGroupEventApiResponseFactory() {
		return groupEventApiResponseFactory;
	}

	@Nonnull
	protected GroupSessionApiResponseFactory getGroupSessionApiResponseFactory() {
		return groupSessionApiResponseFactory;
	}

	@Nonnull
	protected ContentApiResponseFactory getContentApiResponseFactory() {
		return contentApiResponseFactory;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}

	@Nonnull
	protected AuditLogService getAuditLogService() {
		return auditLogService;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return institutionService;
	}

	@Nonnull
	protected InstitutionApiResponseFactory getInstitutionApiResponseFactory() {
		return institutionApiResponseFactory;
	}

	@Nonnull
	protected BetaFeatureAlertApiResponseFactory getBetaFeatureAlertApiResponseFactory() {
		return betaFeatureAlertApiResponseFactory;
	}

	@Nonnull
	protected ActivityTrackingService getActivityTrackingService() {
		return activityTrackingService;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected AppointmentApiResponseFactory getAppointmentApiResponseFactory() {
		return appointmentApiResponseFactory;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return authorizationService;
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return providerService;
	}

	@Nonnull
	protected SessionService getSessionService() {
		return sessionService;
	}

	@Nonnull
	protected AssessmentService getAssessmentService() {
		return assessmentService;
	}

	@Nonnull
	protected AssessmentFormApiResponseFactory getAssessmentFormApiResponseFactory() {
		return assessmentFormApiResponseFactory;
	}
}