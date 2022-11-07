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
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.model.api.request.AcceptAccountConsentFormRequest;
import com.cobaltplatform.api.model.api.request.AccessTokenRequest;
import com.cobaltplatform.api.model.api.request.AccountRoleRequest;
import com.cobaltplatform.api.model.api.request.ApplyAccountEmailVerificationCodeRequest;
import com.cobaltplatform.api.model.api.request.CreateAccountEmailVerificationRequest;
import com.cobaltplatform.api.model.api.request.CreateAccountInviteRequest;
import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.api.request.CreateActivityTrackingRequest;
import com.cobaltplatform.api.model.api.request.CreateMyChartAccountRequest;
import com.cobaltplatform.api.model.api.request.FindGroupSessionRequestsRequest;
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
import com.cobaltplatform.api.model.api.response.CountryApiResponse;
import com.cobaltplatform.api.model.api.response.CountryApiResponse.CountryApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionApiResponse;
import com.cobaltplatform.api.model.api.response.GroupSessionApiResponse.GroupSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionRequestApiResponse;
import com.cobaltplatform.api.model.api.response.GroupSessionRequestApiResponse.GroupSessionRequestApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InstitutionApiResponse.InstitutionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.LanguageApiResponse;
import com.cobaltplatform.api.model.api.response.LanguageApiResponse.LanguageApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TimeZoneApiResponse;
import com.cobaltplatform.api.model.api.response.TimeZoneApiResponse.TimeZoneApiResponseFactory;
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
import com.cobaltplatform.api.model.db.AuditLogEvent.AuditLogEventId;
import com.cobaltplatform.api.model.db.BetaFeatureAlert;
import com.cobaltplatform.api.model.db.ClientDeviceType.ClientDeviceTypeId;
import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.db.GroupSession;
import com.cobaltplatform.api.model.db.GroupSessionRequest;
import com.cobaltplatform.api.model.db.GroupSessionRequestStatus.GroupSessionRequestStatusId;
import com.cobaltplatform.api.model.db.GroupSessionStatus.GroupSessionStatusId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.LoginDestination.LoginDestinationId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.AccountSourceForInstitution;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.ActivityTrackingService;
import com.cobaltplatform.api.service.AppointmentService;
import com.cobaltplatform.api.service.AssessmentService;
import com.cobaltplatform.api.service.AuditLogService;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.ContentService;
import com.cobaltplatform.api.service.GroupSessionService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.MyChartService;
import com.cobaltplatform.api.service.ProviderService;
import com.cobaltplatform.api.service.SessionService;
import com.cobaltplatform.api.util.Authenticator;
import com.cobaltplatform.api.util.LinkGenerator;
import com.cobaltplatform.api.util.ValidationException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

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
	private final GroupSessionService groupSessionService;
	@Nonnull
	private final ContentService contentService;
	@Nonnull
	private final AppointmentService appointmentService;
	@Nonnull
	private final MyChartService myChartService;
	@Nonnull
	private final AccountApiResponseFactory accountApiResponseFactory;
	@Nonnull
	private final GroupSessionApiResponseFactory groupSessionApiResponseFactory;
	@Nonnull
	private final GroupSessionRequestApiResponseFactory groupSessionRequestApiResponseFactory;
	@Nonnull
	private final ContentApiResponseFactory contentApiResponseFactory;
	@Nonnull
	private final TimeZoneApiResponseFactory timeZoneApiResponseFactory;
	@Nonnull
	private final LanguageApiResponseFactory languageApiResponseFactory;
	@Nonnull
	private final CountryApiResponseFactory countryApiResponseFactory;
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
	private final AppointmentApiResponseFactory appointmentApiResponseFactory;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final ProviderService providerService;
	@Nonnull
	private final SessionService sessionService;
	@Nonnull
	private final AssessmentService assessmentService;
	@Nonnull
	private final AssessmentFormApiResponseFactory assessmentFormApiResponseFactory;
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;

	@Inject
	public AccountResource(@Nonnull AccountService accountService,
												 @Nonnull GroupSessionService groupSessionService,
												 @Nonnull ContentService contentService,
												 @Nonnull AppointmentService appointmentService,
												 @Nonnull MyChartService myChartService,
												 @Nonnull AccountApiResponseFactory accountApiResponseFactory,
												 @Nonnull GroupSessionApiResponseFactory groupSessionApiResponseFactory,
												 @Nonnull GroupSessionRequestApiResponseFactory groupSessionRequestApiResponseFactory,
												 @Nonnull ContentApiResponseFactory contentApiResponseFactory,
												 @Nonnull TimeZoneApiResponseFactory timeZoneApiResponseFactory,
												 @Nonnull LanguageApiResponseFactory languageApiResponseFactory,
												 @Nonnull CountryApiResponseFactory countryApiResponseFactory,
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
												 @Nonnull AssessmentFormApiResponseFactory assessmentFormApiResponseFactory,
												 @Nonnull EnterprisePluginProvider enterprisePluginProvider) {
		requireNonNull(accountService);
		requireNonNull(groupSessionService);
		requireNonNull(contentService);
		requireNonNull(appointmentService);
		requireNonNull(myChartService);
		requireNonNull(accountApiResponseFactory);
		requireNonNull(groupSessionApiResponseFactory);
		requireNonNull(groupSessionRequestApiResponseFactory);
		requireNonNull(contentApiResponseFactory);
		requireNonNull(timeZoneApiResponseFactory);
		requireNonNull(languageApiResponseFactory);
		requireNonNull(countryApiResponseFactory);
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
		requireNonNull(enterprisePluginProvider);

		this.accountService = accountService;
		this.groupSessionService = groupSessionService;
		this.contentService = contentService;
		this.appointmentService = appointmentService;
		this.myChartService = myChartService;
		this.accountApiResponseFactory = accountApiResponseFactory;
		this.groupSessionApiResponseFactory = groupSessionApiResponseFactory;
		this.groupSessionRequestApiResponseFactory = groupSessionRequestApiResponseFactory;
		this.contentApiResponseFactory = contentApiResponseFactory;
		this.timeZoneApiResponseFactory = timeZoneApiResponseFactory;
		this.languageApiResponseFactory = languageApiResponseFactory;
		this.countryApiResponseFactory = countryApiResponseFactory;
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
		this.enterprisePluginProvider = enterprisePluginProvider;
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
		String destinationUrl = getLinkGenerator().generateAuthenticationLink(account.getInstitutionId(), loginDestinationId, ClientDeviceTypeId.WEB_BROWSER, accessToken);

		UUID sessionTrackingId = getCurrentContext().getSessionTrackingId().orElse(null);

		if (sessionTrackingId != null) {
			CreateActivityTrackingRequest activityTrackingRequest = new CreateActivityTrackingRequest();
			activityTrackingRequest.setSessionTrackingId(sessionTrackingId);
			activityTrackingRequest.setActivityActionId(ActivityAction.ActivityActionId.SIGN_IN);
			activityTrackingRequest.setActivityTypeId(ActivityType.ActivityTypeId.ACCOUNT);
			activityTrackingRequest.setContext(new JSONObject().put("accountId", account.getAccountId().toString()).toString());

			getActivityTrackingService().trackActivity(Optional.of(account), activityTrackingRequest);
		}

		String pinnedDestinationUrl = destinationUrl;

		return new ApiResponse(new HashMap<String, Object>() {{
			put("account", getAccountApiResponseFactory().create(pinnedAccount));
			put("accessToken", accessToken);
			put("destinationUrl", pinnedDestinationUrl);
		}});
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
	@GET("/accounts/{accountId}/recommendations")
	@AuthenticationRequired
	public ApiResponse accountRecommendations(@Nonnull @PathParameter UUID accountId) {
		requireNonNull(accountId);

		Account account = getAccountService().findAccountById(accountId).orElse(null);

		if (account == null)
			throw new NotFoundException();

		if (!account.getAccountId().equals(accountId))
			throw new AuthorizationException();

		Institution institution = getInstitutionService().findInstitutionById(account.getInstitutionId()).get();

		final int MAXIMUM_GROUP_SESSIONS = 8;

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

		final int MAXIMUM_GROUP_SESSION_REQUESTS = 8;

		List<GroupSessionRequest> groupSessionRequests = Collections.emptyList();

		// Not everyone gets recommended these group sessions by-request
		if (institution.getRecommendGroupSessionRequests()) {
			groupSessionRequests = new ArrayList<>(getGroupSessionService().findGroupSessionRequests(new FindGroupSessionRequestsRequest() {{
				setGroupSessionRequestStatusId(GroupSessionRequestStatusId.ADDED);
				setInstitutionId(account.getInstitutionId());
			}}).getResults());

			// Don't show too many events
			if (groupSessionRequests.size() > MAXIMUM_GROUP_SESSION_REQUESTS)
				groupSessionRequests = groupSessionRequests.subList(0, MAXIMUM_GROUP_SESSION_REQUESTS /* exclusive */);
		}

		final int MAXIMUM_CONTENTS = 12;

		List<Content> contents = getContentService().findContentForAccount(account);

		// Don't show too many content pieces
		if (contents.size() > MAXIMUM_CONTENTS)
			contents = contents.subList(0, MAXIMUM_CONTENTS /* exclusive */);
		else if (contents.size() < MAXIMUM_CONTENTS) {
			contents.addAll(getContentService().findAdditionalContentForAccount(account, contents));
			contents = contents.subList(0, min(contents.size(), MAXIMUM_CONTENTS));
		}

		List<ContentApiResponse> contentApiResponses = contents.stream()
				.map(content -> getContentApiResponseFactory().create(content))
				.collect(Collectors.toList());

		List<GroupSessionApiResponse> groupSessionApiResponses = groupSessions.stream()
				.map(groupSession -> getGroupSessionApiResponseFactory().create(groupSession))
				.collect(Collectors.toList());

		List<GroupSessionRequestApiResponse> groupSessionRequestApiResponses = groupSessionRequests.stream()
				.map(groupSessionRequest -> getGroupSessionRequestApiResponseFactory().create(groupSessionRequest))
				.collect(Collectors.toList());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("contents", contentApiResponses);
			put("groupSessions", groupSessionApiResponses);
			put("groupSessionRequests", groupSessionRequestApiResponses);
		}});
	}

	@Nonnull
	@POST("/accounts/invite")
	public ApiResponse createAccountInvite(@Nonnull @RequestBody String body) {
		requireNonNull(body);

		CreateAccountInviteRequest request = getRequestBodyParser().parse(body, CreateAccountInviteRequest.class);
		request.setInstitutionId(getCurrentContext().getInstitutionId());

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
	public ApiResponse createAccount() {
		InstitutionId institutionId = getCurrentContext().getInstitutionId();
		List<AccountSourceForInstitution> accountSources = getInstitutionService().findAccountSourcesByInstitutionId(institutionId);

		boolean supportsAnonymous = false;

		for (AccountSourceForInstitution accountSource : accountSources) {
			if (accountSource.getAccountSourceId() == AccountSourceId.ANONYMOUS) {
				supportsAnonymous = true;
				break;
			}
		}

		if (!supportsAnonymous)
			throw new IllegalStateException(format("Not permitted to create anonymous accounts for institution ID %s", institutionId.name()));

		// For now - this is only to generate anonymous accounts
		UUID accountId = getAccountService().createAccount(new CreateAccountRequest() {{
			setRoleId(RoleId.PATIENT);
			setInstitutionId(getCurrentContext().getInstitutionId());
			setAccountSourceId(AccountSourceId.ANONYMOUS);
		}});

		Account account = getAccountService().findAccountById(accountId).get();

		AuditLog auditLog = new AuditLog();
		auditLog.setAccountId(account.getAccountId());
		auditLog.setAuditLogEventId(AuditLogEventId.ACCOUNT_CREATE);
		getAuditLogService().audit(auditLog);

		String accessToken = getAuthenticator().generateAccessToken(account.getAccountId(), account.getRoleId());

		UUID sessionTrackingId = getCurrentContext().getSessionTrackingId().orElse(null);

		if (sessionTrackingId != null) {
			CreateActivityTrackingRequest activityTrackingRequest = new CreateActivityTrackingRequest();
			activityTrackingRequest.setSessionTrackingId(sessionTrackingId);
			activityTrackingRequest.setActivityActionId(ActivityAction.ActivityActionId.CREATE);
			activityTrackingRequest.setActivityTypeId(ActivityType.ActivityTypeId.ACCOUNT);
			activityTrackingRequest.setContext(new JSONObject().put("accountId", accountId.toString()).toString());

			getActivityTrackingService().trackActivity(Optional.of(account), activityTrackingRequest);
		}

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
	@POST("/accounts/mychart")
	public ApiResponse createMyChartAccount(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		CreateMyChartAccountRequest request = getRequestBodyParser().parse(requestBody, CreateMyChartAccountRequest.class);
		request.setInstitutionId(getCurrentContext().getInstitutionId());

		UUID accountId = getMyChartService().createAccount(request);

		return generateAccountResponse(accountId);
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

		List<Appointment> appointments = getAppointmentService().findUpcomingAppointmentsByAccountIdAndProviderId(appointmentAccount.getAccountId(), provider.getProviderId(),
				getCurrentContext().getTimeZone());

		Map<String, Object> responseData = new HashMap<>();
		responseData.put("account", getAccountApiResponseFactory().create(appointmentAccount, finalSupplements));
		responseData.put("appointment", getAppointmentApiResponseFactory().create(appointment, Set.of(AppointmentApiResponseSupplement.PROVIDER, AppointmentApiResponseSupplement.APPOINTMENT_TYPE)));
		responseData.put("appointments", appointments.stream()
				.map((a) -> getAppointmentApiResponseFactory().create(a, Set.of(AppointmentApiResponseSupplement.PROVIDER, AppointmentApiResponseSupplement.APPOINTMENT_TYPE))).collect(Collectors.toList()));

		if (appointment.getIntakeAccountSessionId() != null) {
			AccountSession intakeSession = getSessionService().findAccountSessionById(appointment.getIntakeAccountSessionId()).orElse(null);
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
		if (!currentAccount.getAccountId().equals(accountId))
			throw new AuthorizationException();

		getAccountService().requestRoleForAccount(request);
	}

	@Nonnull
	@GET("/accounts/{accountId}/federated-logout-url")
	public ApiResponse federatedLogoutUrl(@Nonnull @PathParameter UUID accountId) {
		Account account = getAccountService().findAccountById(accountId).orElse(null);
		String federatedLogoutUrl = getEnterprisePluginProvider().enterprisePluginForInstitutionId(account.getInstitutionId())
				.federatedLogoutUrl(account).orElse(null);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("federatedLogoutUrl", federatedLogoutUrl);
		}});
	}

	@Nonnull
	@AuthenticationRequired
	@POST("/accounts/{accountId}/email-verification-code")
	public ApiResponse createAccountEmailVerificationCode(@Nonnull @PathParameter UUID accountId,
																												@Nonnull @RequestBody String body) {
		requireNonNull(accountId);
		requireNonNull(body);

		CreateAccountEmailVerificationRequest request = getRequestBodyParser().parse(body, CreateAccountEmailVerificationRequest.class);
		request.setAccountId(accountId);

		Account currentAccount = getCurrentContext().getAccount().get();

		// You can only request for yourself for now
		if (!currentAccount.getAccountId().equals(accountId))
			throw new AuthorizationException();

		boolean verified = getAccountService().isEmailAddressVerifiedForAccountId(request.getEmailAddress(), accountId);
		boolean forceVerification = request.getForceVerification() == null ? false : request.getForceVerification();

		if (forceVerification || !verified)
			getAccountService().createAccountEmailVerification(request);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("verified", verified);
		}});
	}

	@Nonnull
	@AuthenticationRequired
	@POST("/accounts/{accountId}/apply-email-verification-code")
	public ApiResponse applyAccountEmailVerificationCode(@Nonnull @PathParameter UUID accountId,
																											 @Nonnull @RequestBody String body) {
		requireNonNull(accountId);
		requireNonNull(body);

		ApplyAccountEmailVerificationCodeRequest request = getRequestBodyParser().parse(body, ApplyAccountEmailVerificationCodeRequest.class);
		request.setAccountId(accountId);

		Account currentAccount = getCurrentContext().getAccount().get();

		// You can only request for yourself for now
		if (!currentAccount.getAccountId().equals(accountId))
			throw new AuthorizationException();

		getAccountService().applyAccountEmailVerificationCode(request);

		return new ApiResponse();
	}

	@Nonnull
	@AuthenticationRequired
	@GET("/accounts/{accountId}/check-email-verification")
	public ApiResponse checkEmailVerification(@Nonnull @PathParameter UUID accountId,
																						@Nonnull @QueryParameter String emailAddress) {
		requireNonNull(accountId);
		requireNonNull(emailAddress);

		Account currentAccount = getCurrentContext().getAccount().get();

		// You can only request for yourself for now
		if (!currentAccount.getAccountId().equals(accountId))
			throw new AuthorizationException();

		boolean verified = getAccountService().isEmailAddressVerifiedForAccountId(emailAddress, accountId);

		Map<String, Object> response = new HashMap<>();
		response.put("verified", verified);
		response.put("emailAddress", currentAccount.getEmailAddress());

		return new ApiResponse(response);
	}

	@Nonnull
	@GET("/accounts/reference-data")
	public ApiResponse lookupTimeZones() {
		// Time zones
		List<TimeZoneApiResponse> timeZones = getAccountService().getAccountTimeZones().stream()
				.map(timeZone -> getTimeZoneApiResponseFactory().create(timeZone))
				.collect(Collectors.toList());

		Collections.sort(timeZones, (tz1, tz2) -> {
			return tz1.getDescription().compareTo(tz2.getDescription());
		});

		// Countries
		Set<Locale> countryLocales = getAccountService().getAccountCountries();
		List<CountryApiResponse> countries = new ArrayList<>(countryLocales.size());

		for (Locale locale : countryLocales)
			countries.add(getCountryApiResponseFactory().create(locale));

		Collections.sort(countries, (country1, country2) -> {
			return country1.getDescription().compareTo(country2.getDescription());
		});

		// Languages
		List<LanguageApiResponse> languages = getAccountService().getAccountLanguages().stream()
				.map(language -> getLanguageApiResponseFactory().create(language))
				.collect(Collectors.toList());

		Collections.sort(languages, (language1, language2) -> {
			return language1.getDescription().compareTo(language2.getDescription());
		});

		return new ApiResponse(new HashMap<String, Object>() {{
			put("timeZones", timeZones);
			put("countries", countries);
			put("languages", languages);
		}});
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountService;
	}

	@Nonnull
	protected AccountApiResponseFactory getAccountApiResponseFactory() {
		return this.accountApiResponseFactory;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return this.requestBodyParser;
	}

	@Nonnull
	protected Authenticator getAuthenticator() {
		return this.authenticator;
	}

	@Nonnull
	protected LinkGenerator getLinkGenerator() {
		return this.linkGenerator;
	}

	@Nonnull
	protected GroupSessionService getGroupSessionService() {
		return this.groupSessionService;
	}

	@Nonnull
	protected ContentService getContentService() {
		return this.contentService;
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return this.appointmentService;
	}

	@Nonnull
	protected MyChartService getMyChartService() {
		return this.myChartService;
	}

	@Nonnull
	protected GroupSessionApiResponseFactory getGroupSessionApiResponseFactory() {
		return this.groupSessionApiResponseFactory;
	}

	@Nonnull
	protected GroupSessionRequestApiResponseFactory getGroupSessionRequestApiResponseFactory() {
		return this.groupSessionRequestApiResponseFactory;
	}

	@Nonnull
	protected ContentApiResponseFactory getContentApiResponseFactory() {
		return this.contentApiResponseFactory;
	}

	@Nonnull
	protected TimeZoneApiResponseFactory getTimeZoneApiResponseFactory() {
		return this.timeZoneApiResponseFactory;
	}

	@Nonnull
	protected LanguageApiResponseFactory getLanguageApiResponseFactory() {
		return this.languageApiResponseFactory;
	}

	@Nonnull
	protected CountryApiResponseFactory getCountryApiResponseFactory() {
		return this.countryApiResponseFactory;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	protected AuditLogService getAuditLogService() {
		return this.auditLogService;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionService;
	}

	@Nonnull
	protected InstitutionApiResponseFactory getInstitutionApiResponseFactory() {
		return this.institutionApiResponseFactory;
	}

	@Nonnull
	protected BetaFeatureAlertApiResponseFactory getBetaFeatureAlertApiResponseFactory() {
		return this.betaFeatureAlertApiResponseFactory;
	}

	@Nonnull
	protected ActivityTrackingService getActivityTrackingService() {
		return this.activityTrackingService;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected AppointmentApiResponseFactory getAppointmentApiResponseFactory() {
		return this.appointmentApiResponseFactory;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return this.authorizationService;
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return this.providerService;
	}

	@Nonnull
	protected SessionService getSessionService() {
		return this.sessionService;
	}

	@Nonnull
	protected AssessmentService getAssessmentService() {
		return this.assessmentService;
	}

	@Nonnull
	protected AssessmentFormApiResponseFactory getAssessmentFormApiResponseFactory() {
		return this.assessmentFormApiResponseFactory;
	}

	@Nonnull
	protected EnterprisePluginProvider getEnterprisePluginProvider() {
		return this.enterprisePluginProvider;
	}
}