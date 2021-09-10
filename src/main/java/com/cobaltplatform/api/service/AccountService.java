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

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.cache.Cache;
import com.cobaltplatform.api.cache.DistributedCache;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageManager;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.api.request.AcceptAccountConsentFormRequest;
import com.cobaltplatform.api.model.api.request.AccessTokenRequest;
import com.cobaltplatform.api.model.api.request.CreateAccountInviteRequest;
import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.api.request.ForgotPasswordRequest;
import com.cobaltplatform.api.model.api.request.ResetPasswordRequest;
import com.cobaltplatform.api.model.api.request.UpdateAccountBetaStatusRequest;
import com.cobaltplatform.api.model.api.request.UpdateAccountEmailAddressRequest;
import com.cobaltplatform.api.model.api.request.UpdateAccountPhoneNumberRequest;
import com.cobaltplatform.api.model.api.request.UpdateAccountRoleRequest;
import com.cobaltplatform.api.model.api.request.UpdateBetaFeatureAlertRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountInvite;
import com.cobaltplatform.api.model.db.AccountLoginRule;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.BetaFeature.BetaFeatureId;
import com.cobaltplatform.api.model.db.BetaFeatureAlert;
import com.cobaltplatform.api.model.db.BetaFeatureAlert.BetaFeatureAlertStatusId;
import com.cobaltplatform.api.model.db.BetaStatus.BetaStatusId;
import com.cobaltplatform.api.model.db.ClientDeviceType;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PasswordResetRequest;
import com.cobaltplatform.api.model.db.Role;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.SourceSystem.SourceSystemId;
import com.cobaltplatform.api.model.security.AccessTokenClaims;
import com.cobaltplatform.api.util.Authenticator;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.LinkGenerator;
import com.cobaltplatform.api.util.Normalizer;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.ValidationUtility;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class AccountService {
	@Nonnull
	private static final UUID TEST_CASEY_WATSON_ACCOUNT_ID;

	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Cache distributedCache;
	@Nonnull
	private final Authenticator authenticator;
	@Nonnull
	private final Normalizer normalizer;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final EmailMessageManager emailMessageManager;
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final LinkGenerator linkGenerator;

	static {
		TEST_CASEY_WATSON_ACCOUNT_ID = UUID.fromString("e16ebeb4-b2fd-4e04-b9d2-cac6362c96ec");
	}

	@Inject
	public AccountService(@Nonnull Provider<CurrentContext> currentContextProvider,
												@Nonnull Database database,
												@Nonnull @DistributedCache Cache distributedCache,
												@Nonnull Authenticator authenticator,
												@Nonnull Normalizer normalizer,
												@Nonnull Formatter formatter,
												@Nonnull JsonMapper jsonMapper,
												@Nonnull Configuration configuration,
												@Nonnull EmailMessageManager emailMessageManager,
												@Nonnull InstitutionService institutionService,
												@Nonnull LinkGenerator linkGenerator,
												@Nonnull Strings strings) {
		requireNonNull(currentContextProvider);
		requireNonNull(database);
		requireNonNull(distributedCache);
		requireNonNull(authenticator);
		requireNonNull(normalizer);
		requireNonNull(formatter);
		requireNonNull(jsonMapper);
		requireNonNull(configuration);
		requireNonNull(strings);
		requireNonNull(emailMessageManager);
		requireNonNull(institutionService);
		requireNonNull(linkGenerator);

		this.currentContextProvider = currentContextProvider;
		this.database = database;
		this.distributedCache = distributedCache;
		this.authenticator = authenticator;
		this.normalizer = normalizer;
		this.formatter = formatter;
		this.jsonMapper = jsonMapper;
		this.configuration = configuration;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
		this.emailMessageManager = emailMessageManager;
		this.institutionService = institutionService;
		this.linkGenerator = linkGenerator;
	}

	@Nonnull
	public Optional<Account> findAccountById(@Nullable UUID accountId) {
		if (accountId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM account WHERE account_id=?", Account.class, accountId);
	}

	@Nonnull
	public Optional<Account> findAccountByAccessToken(@Nullable String accessToken) {
		accessToken = trimToNull(accessToken);

		if (accessToken == null)
			return Optional.empty();

		Optional<AccessTokenClaims> accessTokenClaims = getAuthenticator().validateAccessToken(accessToken);

		if (accessTokenClaims.isPresent()) {
			UUID accountId = accessTokenClaims.get().getAccountId();
			return findAccountById(accountId);
		}

		return Optional.empty();
	}

	@Nonnull
	public List<Account> findSuperAdminAccounts() {
		return getDatabase().queryForList("SELECT * FROM account WHERE role_id = ?", Account.class, RoleId.SUPER_ADMINISTRATOR);
	}

	@Nonnull
	public List<Account> findAdminAccountsForInstitution(InstitutionId institutionId) {
		return getDatabase().queryForList("SELECT * FROM account WHERE role_id = ? AND institution_id = ?", Account.class, RoleId.ADMINISTRATOR, institutionId.name());
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	public Optional<Role> findRoleById(@Nullable RoleId roleId) {
		if (roleId == null)
			return Optional.empty();

		return Optional.ofNullable(getDistributedCache().get(format("roleByRoleId-%s", roleId.name()), () -> {
			return getDatabase().queryForObject("SELECT * FROM role WHERE role_id=?", Role.class, roleId).orElse(null);
		}, Role.class));
	}

	@Nonnull
	public List<Account> findTestSsoAccounts(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);

		return getDatabase().queryForList("SELECT * FROM account WHERE (email_address LIKE '%@xmog.com' OR email_address LIKE '%@pennmedicine.upenn.edu' OR sso_id='fake-sso-id-lisa.lombard@northwestern.edu' OR sso_id='fake-sso-id-gwen.holtzman@northwestern.edu') " +
				"AND institution_id IN (?,?) ORDER BY email_address", Account.class, institutionId, InstitutionId.COBALT);
	}

	@Nonnull
	private Account findRequiredAccountById(@Nonnull UUID accountId) {
		requireNonNull(accountId);
		return findAccountById(accountId).get();
	}

	@Nonnull
	public Optional<Account> findAccountByAccountSourceIdAndSsoId(@Nullable AccountSourceId accountSourceId,
																																@Nullable String ssoId) {
		if (accountSourceId == null || ssoId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM account WHERE account_source_id=? AND sso_id=?", Account.class, accountSourceId, ssoId);
	}

	@Nonnull
	public Optional<AccountInvite> findAccountInviteByCode(UUID accountInviteCode) {
		return getDatabase().queryForObject("SELECT * FROM account_invite WHERE account_invite_code = ?",
				AccountInvite.class, accountInviteCode);
	}

	@Nonnull
	private Optional<AccountInvite> findAccountInviteById(UUID accountInviteId) {
		return getDatabase().queryForObject("SELECT * FROM account_invite WHERE account_invite_id = ?",
				AccountInvite.class, accountInviteId);
	}

	@Nonnull
	public Boolean accountInviteExpired(UUID accountInviteCode) {
		Integer INVITE_GOOD_FOR_MINUTES = 10;

		return getDatabase().queryForObject(String.format("SELECT count(*) FROM account_invite " +
						"WHERE account_invite_code = ? AND now() >= created + INTERVAL '%s MINUTES'", INVITE_GOOD_FOR_MINUTES),
				Boolean.class, accountInviteCode).get();
	}

	@Nonnull
	public UUID claimAccountInvite(@Nonnull UUID accountInviteCode) {
		requireNonNull(accountInviteCode);

		ValidationException validationException = new ValidationException();
		Optional<AccountInvite> accountInvite = findAccountInviteByCode(accountInviteCode);

		if (!accountInvite.isPresent())
			validationException.add(new FieldError("accountInvite", getStrings().get("Could not find account invite.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("UPDATE account_invite SET claimed=TRUE WHERE account_invite_id = ?",
				accountInvite.get().getAccountInviteId());

		UUID accountId = createAccount(new CreateAccountRequest() {{
			setRoleId(RoleId.PATIENT);
			setInstitutionId(accountInvite.get().getInstitutionId());
			setAccountSourceId(AccountSourceId.EMAIL_PASSWORD);
			setEmailAddress(accountInvite.get().getEmailAddress());
			setPassword(accountInvite.get().getPassword());
		}});

		return accountId;
	}

	@Nonnull
	public UUID createAccountInvite(@Nonnull CreateAccountInviteRequest request) {
		requireNonNull(request);

		UUID accountInviteId = UUID.randomUUID();
		ValidationException validationException = new ValidationException();
		String emailAddress = trimToNull(request.getEmailAddress());
		String password = trimToNull(request.getPassword());
		UUID accountInviteCode = UUID.randomUUID();
		String subdomain;

		if (trimToNull(request.getSubdomain()) != null)
			subdomain = request.getSubdomain();
		else
			subdomain = getConfiguration().getDefaultSubdomain();

		Optional<Institution> institution = getInstitutionService().findInstitutionBySubdomain(subdomain);

		if (!institution.isPresent())
			validationException.add(new FieldError("subdomain", getStrings().get("Could not find institution.")));

		if (emailAddress == null)
			validationException.add(new FieldError("emailAddress", getStrings().get("Email address is required.")));
		else if (emailAddress != null && !ValidationUtility.isValidEmailAddress(emailAddress))
			validationException.add(new FieldError("emailAddress", getStrings().get("Email address is invalid.")));
		else if (findAccountByEmailAddressAndAccountSourceId(emailAddress, AccountSourceId.EMAIL_PASSWORD).isPresent())
			validationException.add(new FieldError("emailAddress", getStrings().get("Email address is already in use.")));
		if (password == null)
			validationException.add(new FieldError("password", getStrings().get("Password is required")));
		else if (!getAuthenticator().validatePasswordRules(password))
			validationException.add(new FieldError("password", getStrings().get("Password must be at least 8 characters long and contain at least one letter, one number")));

		if (validationException.hasErrors())
			throw validationException;


		getDatabase().execute("INSERT INTO account_invite (account_invite_id, institution_id, email_address, " +
						"password, account_invite_code) VALUES (?,?,?,?,?)", accountInviteId, institution.get().getInstitutionId(),
				emailAddress, getAuthenticator().hashPassword(password), accountInviteCode);

		AccountInvite accountInvite = findAccountInviteById(accountInviteId).get();

		sendAccountVerificationEmail(accountInvite);
		return accountInviteId;
	}

	@Nonnull
	public void resendAccountVerificationEmail(UUID accountInviteId) {
		ValidationException validationException = new ValidationException();

		Optional<AccountInvite> accountInvite = findAccountInviteById(accountInviteId);

		if (!accountInvite.isPresent())
			validationException.add(new FieldError("accountInvite", getStrings().get("This is not a valid account invite.")));

		if (validationException.hasErrors())
			throw validationException;

		sendAccountVerificationEmail(accountInvite.get());
	}

	@Nonnull
	private void sendAccountVerificationEmail(AccountInvite accountInvite) {
		requireNonNull(accountInvite);

		Institution institution = getInstitutionService().findInstitutionById(accountInvite.getInstitutionId()).get();
		String webBaseUrl = getConfiguration().getWebappBaseUrl(accountInvite.getInstitutionId());

		EmailMessage verificationEmail = new EmailMessage.Builder(
				EmailMessageTemplate.ACCOUNT_VERIFICATION, institution.getLocale())
				.toAddresses(new ArrayList<>() {{
					add(accountInvite.getEmailAddress());
				}})
				.messageContext(new HashMap<String, Object>() {{
					put("verificationUrl", getLinkGenerator().generateAccountInviteLink(institution.getInstitutionId(),
							ClientDeviceType.ClientDeviceTypeId.WEB_BROWSER, accountInvite.getAccountInviteCode()));
				}})
				.build();

		getEmailMessageManager().enqueueMessage(verificationEmail);
	}

	@Nonnull
	public UUID createAccount(@Nonnull CreateAccountRequest request) {
		requireNonNull(request);

		UUID accountId = UUID.randomUUID();
		AccountSourceId accountSourceId = request.getAccountSourceId();
		SourceSystemId sourceSystemId = request.getSourceSystemId() == null ? SourceSystemId.COBALT : request.getSourceSystemId();
		InstitutionId institutionId = request.getInstitutionId();
		RoleId roleId = request.getRoleId();
		String ssoId = trimToNull(request.getSsoId());
		String firstName = trimToNull(request.getFirstName());
		String lastName = trimToNull(request.getLastName());
		String displayName = trimToNull(request.getDisplayName());
		String emailAddress = trimToNull(request.getEmailAddress());
		String phoneNumber = trimToNull(request.getPhoneNumber());
		String password = null;
		Map<String, ?> ssoAttributes = request.getSsoAttributes();
		String epicPatientId = trimToNull(request.getEpicPatientId());
		String epicPatientIdType = trimToNull(request.getEpicPatientIdType());
		ValidationException validationException = new ValidationException();

		if (accountSourceId == null) {
			validationException.add(new FieldError("accountSourceId", getStrings().get("Account source ID is required.")));
		} else if (accountSourceId == AccountSourceId.ANONYMOUS) {
			roleId = RoleId.PATIENT;
			ssoId = null;
			firstName = null;
			lastName = null;
			displayName = null;
			emailAddress = null;
		} else if (accountSourceId == AccountSourceId.COBALT_SSO) {
			institutionId = InstitutionId.COBALT;

			if (ssoId == null)
				validationException.add(new FieldError("ssoId", getStrings().get("SSO ID is required.")));

			// In some cases, we will not have an email address from SSO.  This is OK, flow is similar to anonymous user
			if (emailAddress != null && !ValidationUtility.isValidEmailAddress(emailAddress))
				validationException.add(new FieldError("emailAddress", getStrings().get("Email address is invalid.")));

			if (displayName == null)
				displayName = Normalizer.normalizeName(firstName, lastName).orElse(null);

			// Failsafe so we don't get rejected on weird SSO assertion data
			if (firstName == null && lastName == null && displayName == null) {
				// validationException.add(getStrings().get("Name is required."));
				firstName = getStrings().get("Unknown");
				lastName = getStrings().get("User");
				displayName = getStrings().get("Unknown User");
			}
		} else if (accountSourceId == AccountSourceId.EMAIL_PASSWORD) {
			password = request.getPassword();
		} else {
			throw new UnsupportedOperationException(format("Don't know how to handle %s value %s yet", AccountSourceId.class.getSimpleName(), accountSourceId));
		}

		if (phoneNumber != null)
			phoneNumber = getNormalizer().normalizePhoneNumberToE164(phoneNumber, Locale.US).orElse(null);

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		// Default time zone to institution's TZ
		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		ZoneId timeZone = institution.getTimeZone();

		String ssoAttributesJson = ssoAttributes == null ? null : getJsonMapper().toJson(ssoAttributes);
		getDatabase().execute("INSERT INTO account (account_id, role_id, institution_id, account_source_id, source_system_id, sso_id, "
						+ "first_name, last_name, display_name, email_address, phone_number, sso_attributes, password, epic_patient_id, epic_patient_id_type, time_zone) VALUES (?,?,?,?,?,?,?,?,?,?,?,CAST(? AS JSONB),?,?,?,?)",
				accountId, roleId, institutionId, accountSourceId, sourceSystemId, ssoId, firstName, lastName, displayName, emailAddress, phoneNumber, ssoAttributesJson, password, epicPatientId, epicPatientIdType, timeZone);

		return accountId;
	}

	@Nonnull
	public String obtainAccessToken(@Nonnull AccessTokenRequest request) {
		ValidationException validationException = new ValidationException();

		String emailAddress = getNormalizer().normalizeEmailAddress(request.getEmailAddress()).orElse(null);
		String password = trimToNull(request.getPassword());

		if (emailAddress == null)
			validationException.add(new FieldError("emailAddress", getStrings().get("Email address is required")));

		if (password == null)
			validationException.add(new FieldError("password", getStrings().get("Password is required")));

		Account account = findAccountByEmailAddressAndAccountSourceId(emailAddress, AccountSourceId.EMAIL_PASSWORD).orElse(null);

		if (account == null) {
			validationException.add(new FieldError("emailAddress", getStrings().get("You have entered an invalid email address or password")));
		} else {
			Boolean verified = false;

			if (password != null)
				verified = getAuthenticator().verifyPassword(password, account.getPassword());

			if (!verified)
				validationException.add(new FieldError("password", getStrings().get("You have entered an invalid email address or password")));
		}

		if (validationException.hasErrors())
			throw validationException;

		return getAuthenticator().generateAccessToken(account.getAccountId(), account.getRoleId());
	}

	@Nonnull
	public Optional<Account> findAccountByEmailAddressAndAccountSourceId(@Nullable String emailAddress,
																																			 @Nullable AccountSourceId accountSourceId) {
		emailAddress = getNormalizer().normalizeEmailAddress(emailAddress).orElse(null);

		if (emailAddress == null || accountSourceId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM account WHERE email_address=? AND account_source_id=?", Account.class, emailAddress, accountSourceId);
	}

	@Nonnull
	public void updateAccountEmailAddress(@Nonnull UpdateAccountEmailAddressRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		String emailAddress = trimToNull(request.getEmailAddress());
		ValidationException validationException = new ValidationException();

		if (accountId == null) {
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));
		} else {
			Account account = findAccountById(accountId).orElse(null);

			if (account == null)
				validationException.add(new FieldError("accountId", getStrings().get("Account ID is invalid.")));
		}

		if (emailAddress == null)
			validationException.add(new FieldError("emailAddress", getStrings().get("Email address is required.")));
		else if (!ValidationUtility.isValidEmailAddress(emailAddress))
			validationException.add(new FieldError("emailAddress", getStrings().get("Email address is invalid.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("UPDATE account SET email_address = ? WHERE account_id = ?", emailAddress, accountId);
	}

	@Nonnull
	public void updateAccountPhoneNumber(@Nonnull UpdateAccountPhoneNumberRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		String phoneNumber = trimToNull(request.getPhoneNumber());
		ValidationException validationException = new ValidationException();

		if (accountId == null) {
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));
		} else {
			Account account = findAccountById(accountId).orElse(null);

			if (account == null)
				validationException.add(new FieldError("accountId", getStrings().get("Account ID is invalid.")));
		}

		if (phoneNumber == null) {
			validationException.add(new FieldError("phoneNumber", getStrings().get("Phone number is required.")));
		} else {
			phoneNumber = getNormalizer().normalizePhoneNumberToE164(phoneNumber).orElse(null);

			if (phoneNumber == null)
				validationException.add(new FieldError("phoneNumber", getStrings().get("Phone number is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("UPDATE account SET phone_number = ? WHERE account_id = ?", phoneNumber, accountId);
	}

	@Nonnull
	public void updateAccountRole(@Nonnull UpdateAccountRoleRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		UUID providerId = request.getProviderId();
		RoleId roleId = request.getRoleId();
		ValidationException validationException = new ValidationException();

		if (accountId == null) {
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));
		} else {
			Account account = findAccountById(accountId).orElse(null);

			if (account == null)
				validationException.add(new FieldError("accountId", getStrings().get("Account ID is invalid.")));
		}

		if (roleId == null)
			validationException.add(new FieldError("roleId", getStrings().get("Role ID is required.")));
		else if (roleId == RoleId.SUPER_ADMINISTRATOR)
			// Only allow this if done manually in the DB in special cases.  Very few of these users exist
			validationException.add(new FieldError("roleId", getStrings().get("You cannot update an account to be a super administrator.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("UPDATE account SET role_id=?, provider_id=? WHERE account_id=?", roleId, providerId, accountId);
	}

	@Nonnull
	public void updateAccountConsentFormAccepted(@Nonnull AcceptAccountConsentFormRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		ValidationException validationException = new ValidationException();

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("UPDATE account SET consent_form_accepted = true, consent_form_accepted_date = now() " +
				" WHERE account_id = ?", accountId);
	}

	public void updateAccountEpicPatient(@Nullable UUID accountId,
																			 @Nullable String epicPatientId,
																			 @Nullable String epicPatientIdType) {
		getDatabase().execute("UPDATE account SET epic_patient_id=?, epic_patient_id_type=? " +
				"WHERE account_id = ?", epicPatientId, epicPatientIdType, accountId);
	}

	public void updateAccountEpicPatient(@Nullable UUID accountId,
																			 @Nullable String epicPatientId,
																			 @Nullable String epicPatientIdType,
																			 @Nullable Boolean epicPatientCreatedByCobalt) {
		getDatabase().execute("UPDATE account SET epic_patient_id=?, epic_patient_id_type=?, epic_patient_created_by_cobalt=? " +
				"WHERE account_id = ?", epicPatientId, epicPatientIdType, epicPatientCreatedByCobalt, accountId);
	}

	public void forgotPassword(@Nullable ForgotPasswordRequest request) {
		requireNonNull(request);

		ValidationException validationException = new ValidationException();
		String emailAddress = trimToNull(request.getEmailAddress());

		if (emailAddress == null)
			validationException.add(new FieldError("emailAddress", getStrings().get("Email address is required.")));

		if (validationException.hasErrors())
			throw validationException;

		Optional<Account> account = findAccountByEmailAddressAndAccountSourceId(emailAddress, AccountSourceId.EMAIL_PASSWORD);

		if (account.isPresent()) {
			UUID passwordResetToken = UUID.randomUUID();
			Instant expirationTimestamp = Instant.now().plus(1, ChronoUnit.HOURS);

			getDatabase().execute("INSERT INTO password_reset_request (account_id, password_reset_token, expiration_timestamp) VALUES (?, ?, ?)",
					account.get().getAccountId(), passwordResetToken, expirationTimestamp);

			EmailMessage passwordResetEmail = new EmailMessage.Builder(
					EmailMessageTemplate.PASSWORD_RESET, account.get().getLocale())
					.toAddresses(new ArrayList<>() {{
						add(emailAddress);
					}})
					.messageContext(new HashMap<String, Object>() {{
						put("passwordResetLink", getLinkGenerator().generatePasswordResetLink(account.get().getInstitutionId(),
								ClientDeviceType.ClientDeviceTypeId.WEB_BROWSER, passwordResetToken));
					}})
					.build();

			getEmailMessageManager().enqueueMessage(passwordResetEmail);
		}
	}

	@Nonnull
	private Optional<PasswordResetRequest> findPasswordResetRequestByToken(UUID passwordResetToken) {
		requireNonNull(passwordResetToken);

		return database.queryForObject("SELECT * FROM password_reset_request WHERE password_reset_token = ?",
				PasswordResetRequest.class, passwordResetToken);
	}

	@Nonnull
	public Account resetPassword(ResetPasswordRequest request) {
		requireNonNull(request);

		ValidationException validationException = new ValidationException();

		UUID passwordResetToken = request.getPasswordResetToken();
		String password = trimToNull(request.getPassword());
		String confirmPassword = trimToNull(request.getConfirmPassword());
		Optional<PasswordResetRequest> passwordResetRequest = findPasswordResetRequestByToken(passwordResetToken);

		if (password == null)
			validationException.add(new FieldError("password", strings.get("Password is required")));
		else if (confirmPassword == null)
			validationException.add(new FieldError("confirmPassword", strings.get("Password is required")));
		else {
			if (!password.equals(confirmPassword))
				validationException.add(new FieldError("password", strings.get("Passwords must match")));
			else if (!getAuthenticator().validatePasswordRules(password))
				validationException.add(new FieldError("password", getStrings().get("Password must be at least 8 characters long and contain at least one letter, one number")));

			if (!passwordResetRequest.isPresent())
				validationException.add(new FieldError("passwordResetRequest", strings.get("Sorry, unable to reset password")));
			else if (Instant.now().isAfter(passwordResetRequest.get().getExpirationTimestamp()))
				validationException.add(new FieldError("resetToken", strings.get("Sorry, unable to reset password")));
		}

		if (validationException.hasErrors())
			throw validationException;

		database.execute("UPDATE account SET password = ? WHERE account_id = ?",
				authenticator.hashPassword(request.getPassword()), passwordResetRequest.get().getAccountId());

		database.execute("UPDATE password_reset_request SET expiration_timestamp = now() WHERE password_reset_request_id = ?",
				passwordResetRequest.get().getPasswordResetRequestId());

		return findRequiredAccountById(passwordResetRequest.get().getAccountId());
	}

	@Nonnull
	public String determineDisplayName(@Nonnull Account account) {
		requireNonNull(account);

		String firstName = trimToNull(account.getFirstName());
		String lastName = trimToNull(account.getLastName());
		String displayName = trimToNull(account.getDisplayName());

		if (displayName != null)
			return displayName;

		if (firstName != null && lastName != null)
			return format("%s %s", firstName, lastName);

		return getStrings().get("Anonymous User");
	}

	@Nonnull
	public Optional<AccountLoginRule> findAccountLoginRuleByEmailAddress(@Nullable String emailAddress,
																																			 @Nullable AccountSourceId accountSourceId,
																																			 @Nullable InstitutionId institutionId) {
		emailAddress = getNormalizer().normalizeEmailAddress(emailAddress).orElse(null);

		if (emailAddress == null || accountSourceId == null || institutionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM account_login_rule " +
				"WHERE email_address=? AND account_source_id=? AND institution_id=?", AccountLoginRule.class, emailAddress, accountSourceId, institutionId);
	}

	@Nonnull
	public List<BetaFeatureAlert> findBetaFeatureAlertsByAccountId(@Nullable UUID accountId) {
		if (accountId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM v_beta_feature_alert " +
				"WHERE account_id=? ORDER BY description", BetaFeatureAlert.class, accountId);
	}

	@Nonnull
	public Map<BetaFeatureId, BetaFeatureAlert> findBetaFeaturesAlertsAsMap(@Nullable UUID accountId) {
		if (accountId == null)
			return Collections.emptyMap();

		return findBetaFeatureAlertsByAccountId(accountId).stream()
				.collect(Collectors.toMap(BetaFeatureAlert::getBetaFeatureId, betaFeatureAlert -> betaFeatureAlert));
	}

	@Nonnull
	public BetaFeatureAlert updateBetaFeatureAlert(@Nonnull UpdateBetaFeatureAlertRequest request) {
		requireNonNull(request);

		BetaFeatureId betaFeatureId = request.getBetaFeatureId();
		BetaFeatureAlertStatusId betaFeatureAlertStatusId = request.getBetaFeatureAlertStatusId();
		UUID accountId = request.getAccountId();
		ValidationException validationException = new ValidationException();

		if (betaFeatureId == null)
			validationException.add(new FieldError("betaFeatureId", getStrings().get("Beta Feature ID is required.")));

		if (betaFeatureAlertStatusId == null)
			validationException.add(new FieldError("betaFeatureAlertStatusId", getStrings().get("Beta Feature Alert Status ID is required.")));

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		if (betaFeatureAlertStatusId == BetaFeatureAlertStatusId.UNKNOWN) {
			getDatabase().execute("DELETE FROM beta_feature_alert WHERE account_id=? AND beta_feature_id=?", accountId, betaFeatureId);
		} else {
			// Perform an upsert
			boolean enabled = betaFeatureAlertStatusId == BetaFeatureAlertStatusId.ENABLED;
			getDatabase().execute("INSERT INTO beta_feature_alert (account_id, beta_feature_id, enabled) VALUES (?,?,?) " +
					"ON CONFLICT (account_id, beta_feature_id) DO UPDATE SET enabled=EXCLUDED.enabled", accountId, betaFeatureId, enabled);
		}

		return findBetaFeaturesAlertsAsMap(accountId).get(betaFeatureId);
	}

	public void updateAccountBetaStatus(@Nonnull UpdateAccountBetaStatusRequest request) {
		requireNonNull(request);

		BetaStatusId betaStatusId = request.getBetaStatusId();
		UUID accountId = request.getAccountId();
		ValidationException validationException = new ValidationException();

		if (betaStatusId == null)
			validationException.add(new FieldError("betaStatusId", getStrings().get("Beta Status ID is required.")));

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("UPDATE account SET beta_status_id=? WHERE account_id=?", betaStatusId, accountId);
	}

	@Nonnull
	public UUID getTestCaseyWatsonAccountId() {
		return TEST_CASEY_WATSON_ACCOUNT_ID;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}

	@Nonnull
	protected Database getDatabase() {
		return database;
	}

	@Nonnull
	protected Cache getDistributedCache() {
		return distributedCache;
	}

	@Nonnull
	protected Authenticator getAuthenticator() {
		return authenticator;
	}

	@Nonnull
	protected Normalizer getNormalizer() {
		return normalizer;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return formatter;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return jsonMapper;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}

	@Nonnull
	protected EmailMessageManager getEmailMessageManager() {
		return emailMessageManager;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return institutionService;
	}

	@Nonnull
	protected LinkGenerator getLinkGenerator() {
		return linkGenerator;
	}
}
