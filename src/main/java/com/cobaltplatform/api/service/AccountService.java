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
import com.cobaltplatform.api.error.ErrorReporter;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageManager;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.api.request.AccountRoleRequest;
import com.cobaltplatform.api.model.api.request.ApplyAccountEmailVerificationCodeRequest;
import com.cobaltplatform.api.model.api.request.CreateAccountEmailVerificationRequest;
import com.cobaltplatform.api.model.api.request.CreateAccountInviteRequest;
import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.api.request.CreateInteractionInstanceRequest;
import com.cobaltplatform.api.model.api.request.EmailPasswordAccessTokenRequest;
import com.cobaltplatform.api.model.api.request.ForgotPasswordRequest;
import com.cobaltplatform.api.model.api.request.ResetPasswordRequest;
import com.cobaltplatform.api.model.api.request.UpdateAccountAccessTokenExpiration;
import com.cobaltplatform.api.model.api.request.UpdateAccountBetaStatusRequest;
import com.cobaltplatform.api.model.api.request.UpdateAccountConsentFormAcceptedRequest;
import com.cobaltplatform.api.model.api.request.UpdateAccountEmailAddressRequest;
import com.cobaltplatform.api.model.api.request.UpdateAccountLocationRequest;
import com.cobaltplatform.api.model.api.request.UpdateAccountPhoneNumberRequest;
import com.cobaltplatform.api.model.api.request.UpdateAccountRoleRequest;
import com.cobaltplatform.api.model.api.request.UpdateBetaFeatureAlertRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Account.StandardMetadata;
import com.cobaltplatform.api.model.db.AccountInvite;
import com.cobaltplatform.api.model.db.AccountLoginRule;
import com.cobaltplatform.api.model.db.AccountSource;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.AuditLog;
import com.cobaltplatform.api.model.db.AuditLogEvent.AuditLogEventId;
import com.cobaltplatform.api.model.db.BetaFeature.BetaFeatureId;
import com.cobaltplatform.api.model.db.BetaFeatureAlert;
import com.cobaltplatform.api.model.db.BetaFeatureAlert.BetaFeatureAlertStatusId;
import com.cobaltplatform.api.model.db.BetaStatus.BetaStatusId;
import com.cobaltplatform.api.model.db.BirthSex;
import com.cobaltplatform.api.model.db.BirthSex.BirthSexId;
import com.cobaltplatform.api.model.db.ClientDeviceType;
import com.cobaltplatform.api.model.db.Ethnicity;
import com.cobaltplatform.api.model.db.Ethnicity.EthnicityId;
import com.cobaltplatform.api.model.db.GenderIdentity;
import com.cobaltplatform.api.model.db.GenderIdentity.GenderIdentityId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PasswordResetRequest;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.Race;
import com.cobaltplatform.api.model.db.Race.RaceId;
import com.cobaltplatform.api.model.db.Role;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.SourceSystem.SourceSystemId;
import com.cobaltplatform.api.model.security.AccessTokenClaims;
import com.cobaltplatform.api.model.service.AccountEmailVerificationFlowTypeId;
import com.cobaltplatform.api.model.service.IcTestPatientEmailAddress;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.ValidationUtility.isValidEmailAddress;
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
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Provider<AuditLogService> auditLogServiceProvider;
	@Nonnull
	private final Provider<InteractionService> interactionServiceProvider;
	@Nonnull
	private final Provider<AddressService> addressServiceProvider;
	@Nonnull
	private final Provider<PatientOrderService> patientOrderServiceProvider;
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
	@Nonnull
	private final ErrorReporter errorReporter;
	@Nonnull
	private final Set<ZoneId> accountTimeZones;
	@Nonnull
	private final Set<Locale> accountLanguages;
	@Nonnull
	private final Set<Locale> accountCountries;

	@Inject
	public AccountService(@Nonnull Provider<CurrentContext> currentContextProvider,
												@Nonnull Provider<AuditLogService> auditLogServiceProvider,
												@Nonnull Provider<InteractionService> interactionServiceProvider,
												@Nonnull Provider<AddressService> addressServiceProvider,
												@Nonnull Provider<PatientOrderService> patientOrderServiceProvider,
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
												@Nonnull ErrorReporter errorReporter,
												@Nonnull Strings strings) {
		requireNonNull(currentContextProvider);
		requireNonNull(auditLogServiceProvider);
		requireNonNull(interactionServiceProvider);
		requireNonNull(addressServiceProvider);
		requireNonNull(patientOrderServiceProvider);
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
		requireNonNull(errorReporter);

		this.currentContextProvider = currentContextProvider;
		this.auditLogServiceProvider = auditLogServiceProvider;
		this.interactionServiceProvider = interactionServiceProvider;
		this.addressServiceProvider = addressServiceProvider;
		this.patientOrderServiceProvider = patientOrderServiceProvider;
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
		this.errorReporter = errorReporter;
		this.accountTimeZones = Collections.unmodifiableSet(determineAccountTimeZones());
		this.accountLanguages = Collections.unmodifiableSet(determineAccountLanguages());
		this.accountCountries = Collections.unmodifiableSet(determineAccountCountries());
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
	public Optional<Account> findAccountByAccountSourceIdAndSsoIdAndInstitutionId(@Nullable AccountSourceId accountSourceId,
																																								@Nullable String ssoId,
																																								@Nullable InstitutionId institutionId) {
		if (accountSourceId == null || ssoId == null || institutionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM account 
				WHERE account_source_id=? 
				AND sso_id=?
				AND institution_id=?
				""", Account.class, accountSourceId, ssoId, institutionId);
	}

	@Nonnull
	public Optional<Account> findAccountByMrnAndInstitutionId(@Nullable String epicPatientMrn,
																														@Nullable InstitutionId institutionId) {
		epicPatientMrn = trimToNull(epicPatientMrn);

		if (epicPatientMrn == null || institutionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM account
				WHERE UPPER(?)=UPPER(epic_patient_mrn)
				AND institution_id=?
				""", Account.class, epicPatientMrn, institutionId);
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

		return getDatabase().queryForObject(format("SELECT count(*) FROM account_invite " +
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
		InstitutionId institutionId = request.getInstitutionId();
		String emailAddress = getNormalizer().normalizeEmailAddress(request.getEmailAddress()).orElse(null);
		String password = trimToNull(request.getPassword());
		UUID accountInviteCode = UUID.randomUUID();

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));
		else if (getInstitutionService().findInstitutionById(institutionId).isEmpty())
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is invalid.")));

		if (emailAddress == null)
			validationException.add(new FieldError("emailAddress", getStrings().get("Email address is required.")));
		else if (emailAddress != null && !isValidEmailAddress(emailAddress))
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
						"password, account_invite_code) VALUES (?,?,?,?,?)", accountInviteId, institutionId,
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
		String emailAddress = getNormalizer().normalizeEmailAddress(request.getEmailAddress()).orElse(null);
		String phoneNumber = trimToNull(request.getPhoneNumber());
		String password = null;
		Map<String, ?> ssoAttributes = request.getSsoAttributes();
		String ssoAttributesAsJson = trimToNull(request.getSsoAttributesAsJson());
		String epicPatientId = trimToNull(request.getEpicPatientId());
		String epicPatientIdType = trimToNull(request.getEpicPatientIdType());
		String epicPatientMrn = trimToNull(request.getEpicPatientMrn());
		GenderIdentityId genderIdentityId = request.getGenderIdentityId() == null ? GenderIdentityId.NOT_ASKED : request.getGenderIdentityId();
		EthnicityId ethnicityId = request.getEthnicityId() == null ? EthnicityId.NOT_ASKED : request.getEthnicityId();
		BirthSexId birthSexId = request.getBirthSexId() == null ? BirthSexId.NOT_ASKED : request.getBirthSexId();
		RaceId raceId = request.getRaceId() == null ? RaceId.NOT_ASKED : request.getRaceId();
		LocalDate birthdate = request.getBirthdate();
		UUID addressId = null;
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
			if (emailAddress != null && !isValidEmailAddress(emailAddress))
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

			if (institutionId != null) {
				Institution institution = getInstitutionService().findInstitutionById(institutionId).get();

				if (!institution.getEmailSignupEnabled())
					validationException.add(getStrings().get("Creating an account with an email and password is not supported for this institution."));
			}
		} else if (accountSourceId == AccountSourceId.MYCHART) {
			if (ssoAttributesAsJson == null) {
				validationException.add(new FieldError("ssoAttributesAsJson", getStrings().get("MyChart patient record is required.")));
			} else {
				try {
					getJsonMapper().fromJson(ssoAttributesAsJson, Map.class);
				} catch (Exception e) {
					getLogger().warn(format("Unable to process MyChart JSON: %s", ssoAttributesAsJson), e);
					validationException.add(new FieldError("myChartPatientRecordAsJson", getStrings().get("MyChart patient record could not be processed.")));
				}
			}
		} else {
			throw new UnsupportedOperationException(format("Don't know how to handle %s value %s yet", AccountSourceId.class.getSimpleName(), accountSourceId));
		}

		if (request.getAddress() != null) {
			try {
				addressId = getAddressService().createAddress(request.getAddress());
			} catch (ValidationException e) {
				validationException.add(e);
			}
		}

		if (phoneNumber != null)
			phoneNumber = getNormalizer().normalizePhoneNumberToE164(phoneNumber, Locale.US).orElse(null);

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (ssoAttributesAsJson != null) {
			try {
				getJsonMapper().fromJson(ssoAttributesAsJson, Map.class);
			} catch (Exception e) {
				getLogger().warn(format("Unable to process SSO JSON: %s", ssoAttributesAsJson), e);
				validationException.add(new FieldError("ssoAttributesAsJson", getStrings().get("Provided SSO attributes are invalid.")));
			}
		}

		if (epicPatientMrn != null)
			epicPatientMrn = epicPatientMrn.toUpperCase(Locale.US); // TODO: revisit when we support non-US institutions

		if (validationException.hasErrors())
			throw validationException;

		// Default time zone to institution's TZ
		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		ZoneId timeZone = institution.getTimeZone();

		String finalSsoAttributesAsJson = null;

		// Prefer raw JSON if available
		if (ssoAttributesAsJson != null)
			finalSsoAttributesAsJson = ssoAttributesAsJson;
		else if (ssoAttributes != null)
			finalSsoAttributesAsJson = getJsonMapper().toJson(ssoAttributes);

		getDatabase().execute("""
						INSERT INTO account (
						account_id, role_id, institution_id, account_source_id, source_system_id, sso_id, 
						first_name, last_name, display_name, email_address, phone_number, sso_attributes, password, epic_patient_id, 
						epic_patient_id_type, epic_patient_mrn, time_zone, gender_identity_id, ethnicity_id, birth_sex_id, race_id, birthdate
						) 
						VALUES (?,?,?,?,?,?,?,?,?,?,?,CAST(? AS JSONB),?,?,?,?,?,?,?,?,?,?)
						""",
				accountId, roleId, institutionId, accountSourceId, sourceSystemId, ssoId, firstName, lastName, displayName,
				emailAddress, phoneNumber, finalSsoAttributesAsJson, password, epicPatientId, epicPatientIdType, epicPatientMrn,
				timeZone, genderIdentityId, ethnicityId, birthSexId, raceId, birthdate);

		if (addressId != null) {
			getDatabase().execute("""
					INSERT INTO account_address (account_id, address_id, active)
					VALUES (?,?,?)
					""", accountId, addressId, true);
		}

		// If there are any patient orders to associate this account with, do it now
		getPatientOrderService().associatePatientAccountWithPatientOrders(accountId);

		return accountId;
	}

	@Nonnull
	public String obtainEmailPasswordAccessToken(@Nonnull EmailPasswordAccessTokenRequest request) {
		ValidationException validationException = new ValidationException();

		String emailAddress = getNormalizer().normalizeEmailAddress(request.getEmailAddress()).orElse(null);
		String password = trimToNull(request.getPassword());
		InstitutionId institutionId = request.getInstitutionId();
		Institution institution = null;

		if (emailAddress == null)
			validationException.add(new FieldError("emailAddress", getStrings().get("Email address is required")));

		if (password == null)
			validationException.add(new FieldError("password", getStrings().get("Password is required")));

		if (institutionId == null) {
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required")));
		} else {
			institution = getInstitutionService().findInstitutionById(institutionId).get();
		}

		Account account = findAccountByEmailAddressAndAccountSourceId(emailAddress, AccountSourceId.EMAIL_PASSWORD).orElse(null);

		// Special behavior: if this is an IC institution and debugging is enabled and this looks like a test patient account email
		// but no account exists for it yet, create the test patient account.
		if (account == null
				&& institution.getIntegratedCareEnabled()
				&& getConfiguration().getShouldEnableIcDebugging()
				&& IcTestPatientEmailAddress.isTestEmailAddress(emailAddress)) {
			IcTestPatientEmailAddress icTestPatientEmailAddress = IcTestPatientEmailAddress.fromEmailAddress(emailAddress).get();
			List<PatientOrder> patientOrders = getPatientOrderService().findPatientOrdersByTestPatientEmailAddressAndInstitutionId(icTestPatientEmailAddress, institutionId);

			if (patientOrders.size() > 0) {
				getLogger().info("Creating test IC patient account for email address '{}'...", emailAddress);

				String testPassword = patientOrders.get(0).getTestPatientPassword();

				UUID accountId = createAccount(new CreateAccountRequest() {{
					setEpicPatientId(icTestPatientEmailAddress.getUid());
					setEpicPatientIdType("UID");
					setEpicPatientMrn(icTestPatientEmailAddress.getMrn());
					setAccountSourceId(AccountSourceId.EMAIL_PASSWORD);
					setRoleId(RoleId.PATIENT);
					setInstitutionId(institutionId);
					setEmailAddress(icTestPatientEmailAddress.getEmailAddress());
					setPassword(testPassword);
				}});

				account = findAccountById(accountId).get();

				// Associate our new account with any orders we might have in the system that match
				getPatientOrderService().associatePatientAccountWithPatientOrders(accountId);
			} else {
				getLogger().warn("No test patient order records were found for email address '{}'", emailAddress);
			}
		}

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
		String emailAddress = getNormalizer().normalizeEmailAddress(request.getEmailAddress()).orElse(null);
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
		else if (!isValidEmailAddress(emailAddress))
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
	public void updateAccountAccessTokenExpiration(@Nonnull UpdateAccountAccessTokenExpiration request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		ValidationException validationException = new ValidationException();

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("UPDATE account SET access_token_expiration_in_minutes=?, access_token_short_expiration_in_minutes=? WHERE account_id=?",
				request.getAccessTokenExpirationInMinutes(), request.getAccessTokenShortExpirationInMinutes(), accountId);
	}

	@Nonnull
	public void markAccountLoginRoleAsExecuted(@Nonnull UUID accountLoginRuleId) {
		requireNonNull(accountLoginRuleId);

		getDatabase().execute("UPDATE account_login_rule SET login_rule_executed=TRUE, login_rule_execution_time=now() WHERE account_login_rule_id=?",
				accountLoginRuleId);
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
		else if (roleId == RoleId.ADMINISTRATOR)
			// Only allow this if done manually in the DB in special cases.  Very few of these users exist
			validationException.add(new FieldError("roleId", getStrings().get("You cannot update an account to be an administrator.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("UPDATE account SET role_id=?, provider_id=? WHERE account_id=?", roleId, providerId, accountId);
	}

	@Nonnull
	public void updateAccountConsentFormAccepted(@Nonnull UpdateAccountConsentFormAcceptedRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		Boolean accepted = request.getAccepted();
		ValidationException validationException = new ValidationException();

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (accepted == null)
			validationException.add(new FieldError("accepted", getStrings().get("'Accepted' flag is required.")));

		if (validationException.hasErrors())
			throw validationException;

		if (accepted)
			getDatabase().execute("UPDATE account SET consent_form_accepted = true, consent_form_accepted_date = now() " +
					" WHERE account_id = ?", accountId);
		else
			getDatabase().execute("UPDATE account SET consent_form_accepted = false, consent_form_rejected_date = now() " +
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
		String emailAddress = getNormalizer().normalizeEmailAddress(request.getEmailAddress()).orElse(null);

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
	public Optional<Account> resetPassword(ResetPasswordRequest request) {
		requireNonNull(request);

		ValidationException validationException = new ValidationException();

		UUID passwordResetToken = request.getPasswordResetToken();
		String password = trimToNull(request.getPassword());
		String confirmPassword = trimToNull(request.getConfirmPassword());
		PasswordResetRequest passwordResetRequest = findPasswordResetRequestByToken(passwordResetToken).orElse(null);

		if (password == null)
			validationException.add(new FieldError("password", getStrings().get("Password is required")));
		else if (confirmPassword == null)
			validationException.add(new FieldError("confirmPassword", getStrings().get("Password is required")));
		else {
			if (!password.equals(confirmPassword))
				validationException.add(new FieldError("password", getStrings().get("Passwords must match")));
			else if (!getAuthenticator().validatePasswordRules(password))
				validationException.add(new FieldError("password", getStrings().get("Password must be at least 8 characters long and contain at least one letter, one number")));

			if (passwordResetRequest == null)
				validationException.add(new FieldError("passwordResetRequest", getStrings().get("Sorry, unable to reset password")));
			else if (Instant.now().isAfter(passwordResetRequest.getExpirationTimestamp()))
				validationException.add(new FieldError("resetToken", getStrings().get("Sorry, unable to reset password")));
		}

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("UPDATE account SET password = ? WHERE account_id = ?",
				getAuthenticator().hashPassword(request.getPassword()), passwordResetRequest.getAccountId());

		getDatabase().execute("UPDATE password_reset_request SET expiration_timestamp = now() WHERE password_reset_request_id = ?",
				passwordResetRequest.getPasswordResetRequestId());

		return findAccountById(passwordResetRequest.getAccountId());
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
				"WHERE email_address=? AND account_source_id=? AND institution_id=? AND login_rule_executed = FALSE", AccountLoginRule.class, emailAddress, accountSourceId, institutionId);
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

	@Nonnull
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
	public Long findAccessTokenExpirationInMinutesByAccountId(@Nullable UUID accountId) {
		Account account = findAccountById(accountId).orElse(null);

		// Fallback to the default COBALT institution's configuration if no valid account
		if (account == null)
			return getInstitutionService().findInstitutionById(InstitutionId.COBALT)
					.get().getAnonAccessTokenExpirationInMinutes();

		if (account.getAccessTokenExpirationInMinutes() != null)
			return account.getAccessTokenExpirationInMinutes();
		else if (account.getAccountSourceId().equals(AccountSourceId.ANONYMOUS))
			return getInstitutionService().findInstitutionById(account.getInstitutionId()).get()
					.getAnonAccessTokenExpirationInMinutes();

		return getInstitutionService().findInstitutionById(account.getInstitutionId()).get()
				.getAccessTokenExpirationInMinutes();
	}

	@Nonnull
	public Long findAccessTokenShortExpirationInMinutesByAccount(@Nullable UUID accountId) {
		Account account = findAccountById(accountId).orElse(null);

		// Fallback to the default COBALT institution's configuration if no valid account
		if (account == null)
			return getInstitutionService().findInstitutionById(InstitutionId.COBALT)
					.get().getAnonAccessTokenShortExpirationInMinutes();

		if (account.getAccessTokenShortExpirationInMinutes() != null)
			return account.getAccessTokenShortExpirationInMinutes();
		else if (account.getAccountSourceId().equals(AccountSourceId.ANONYMOUS))
			return getInstitutionService().findInstitutionById(account.getInstitutionId()).get()
					.getAnonAccessTokenShortExpirationInMinutes();

		return getInstitutionService().findInstitutionById(account.getInstitutionId()).get()
				.getAccessTokenShortExpirationInMinutes();
	}

	@Nonnull
	public Optional<AccountSource> findAccountSourceByAccountId(@Nullable UUID accountId) {
		if (accountId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT aa.* FROM account_source aa, account a WHERE a.account_source_id = aa.account_source_id "
				+ "AND a.account_id = ?", AccountSource.class, accountId);
	}

	@Nonnull
	public List<Account> findAccountsMatchingMetadata(@Nullable StandardMetadata standardMetadata) {
		if (standardMetadata == null)
			return findAccountsMatchingMetadata(Collections.emptyMap());

		return findAccountsMatchingMetadata(getJsonMapper().toMap(standardMetadata));
	}

	@Nonnull
	public List<Account> findAccountsMatchingMetadata(@Nullable Map<String, Object> metadata) {
		if (metadata == null || metadata.size() == 0)
			return Collections.emptyList();

		String metadataAsJson = getJsonMapper().toJson(metadata);

		return getDatabase().queryForList("SELECT * FROM account WHERE metadata @> CAST(? AS JSONB)",
				Account.class, metadataAsJson);
	}

	@Nonnull
	public void requestRoleForAccount(@Nonnull AccountRoleRequest request) {
		requireNonNull(request);

		UUID requestingAccountId = request.getAccountId();
		RoleId roleId = request.getRoleId();
		Account requestingAccount = null;
		Account currentAccount = getCurrentContext().getAccount().orElse(null);
		ValidationException validationException = new ValidationException();

		if (requestingAccountId == null) {
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));
		} else {
			requestingAccount = findAccountById(requestingAccountId).orElse(null);

			if (requestingAccount == null)
				validationException.add(new FieldError("accountId", getStrings().get("Account ID is invalid.")));
		}

		if (roleId == null)
			validationException.add(new FieldError("roleId", getStrings().get("Role ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		// 1. Record the role request in our audit log
		AuditLog auditLog = new AuditLog();
		auditLog.setAccountId(currentAccount.getAccountId());
		auditLog.setAuditLogEventId(AuditLogEventId.ACCOUNT_ROLE_REQUEST);
		auditLog.setMessage(format("Role ID %s was requested for account ID %s", roleId.name(), requestingAccountId));
		auditLog.setPayload(getJsonMapper().toJson(new HashMap<String, Object>() {{
			put("requestingAccountId", requestingAccountId);
			put("roleId", roleId);
		}}));

		getAuditLogService().audit(auditLog);

		// 2. Pick out the role request interaction for the appropriate institution so we know how to record this request
		// and notify relevant people
		Institution institution = getInstitutionService().findInstitutionById(requestingAccount.getInstitutionId()).get();
		UUID roleRequestInteractionId = institution.getStandardMetadata().getDefaultRoleRequestInteractionId();

		if (roleRequestInteractionId == null) {
			getErrorReporter().report(format("No role request interaction ID is available for institution %s", institution.getInstitutionId()));
			return;
		}

		// Gather information to put into the interaction instance
		ZoneId timeZone = institution.getTimeZone();
		LocalDateTime now = LocalDateTime.now(timeZone);

		List<String> hipaaCompliantHtmlListItems = new ArrayList<>(2);

		hipaaCompliantHtmlListItems.add(format("<li><strong>Requesting Account ID</strong> %s</li>", requestingAccountId));

		if (requestingAccount.getFirstName() != null)
			hipaaCompliantHtmlListItems.add(format("<li><strong>First Name</strong> %s</li>", requestingAccount.getFirstName()));

		// Non-HIPAA is HIPAA plus a few more fields
		List<String> htmlListItems = new ArrayList<>(hipaaCompliantHtmlListItems);

		if (requestingAccount.getLastName() != null)
			htmlListItems.add(format("<li><strong>Last Name</strong> %s</li>", requestingAccount.getLastName()));
		if (requestingAccount.getEmailAddress() != null)
			htmlListItems.add(format("<li><strong>Email Address</strong> %s</li>", requestingAccount.getEmailAddress()));

		htmlListItems.add(format("<li><strong>Requested Role ID</strong> %s</li>", roleId.name()));

		// HIPAA
		Map<String, Object> hipaaCompliantMetadata = new HashMap<>();
		hipaaCompliantMetadata.put("requestingAccountId", requestingAccountId);
		hipaaCompliantMetadata.put("firstName", requestingAccount.getFirstName());
		hipaaCompliantMetadata.put("endUserHtmlRepresentation", format("<ul>%s</ul>", hipaaCompliantHtmlListItems.stream().collect(Collectors.joining(""))));

		// Non-HIPAA
		Map<String, Object> metadata = new HashMap<>(hipaaCompliantMetadata);
		metadata.put("lastName", requestingAccount.getLastName());
		metadata.put("emailAddress", requestingAccount.getEmailAddress());
		metadata.put("roleId", roleId);
		metadata.put("endUserHtmlRepresentation", format("<ul>%s</ul>", htmlListItems.stream().collect(Collectors.joining(""))));

		// Create our interaction instance to notify appropriate users to review the request
		getInteractionService().createInteractionInstance(new CreateInteractionInstanceRequest() {{
			setMetadata(metadata);
			setHipaaCompliantMetadata(hipaaCompliantMetadata);
			setStartDateTime(now);
			setTimeZone(timeZone);
			setInteractionId(roleRequestInteractionId);
		}});
	}

	@Nonnull
	public Optional<Account> findAccountByProviderId(@Nonnull UUID accountId) {
		requireNonNull(accountId);

		return getDatabase().queryForObject("SELECT * FROM account WHERE provider_id=?", Account.class, accountId);
	}

	@Nonnull
	public UUID createAccountEmailVerification(@Nonnull CreateAccountEmailVerificationRequest request) {
		requireNonNull(request);

		ValidationException validationException = new ValidationException();
		UUID accountId = request.getAccountId();
		String emailAddress = getNormalizer().normalizeEmailAddress(request.getEmailAddress()).orElse(null);
		AccountEmailVerificationFlowTypeId accountEmailVerificationFlowTypeId = request.getAccountEmailVerificationFlowTypeId();
		Account account = null;

		if (emailAddress == null)
			validationException.add(new FieldError("emailAddress", getStrings().get("Email address is required.")));
		else if (!isValidEmailAddress(emailAddress))
			validationException.add(new FieldError("emailAddress", getStrings().get("Email address is invalid.")));

		if (accountId == null) {
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));
		} else {
			account = findAccountById(accountId).orElse(null);

			if (account == null)
				validationException.add(new FieldError("accountId", getStrings().get("Account ID is invalid.")));
		}

		if (accountEmailVerificationFlowTypeId == null)
			validationException.add(new FieldError("accountEmailVerificationFlowTypeId", getStrings().get("Email verification flow type ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		UUID accountEmailVerificationId = UUID.randomUUID();
		Instant expiration = Instant.now().plus(10, ChronoUnit.MINUTES);
		String code = String.format("%06d", new Random().nextInt(999_999)); // random 6-digit number, zero-padded

		getDatabase().execute("""
				INSERT INTO account_email_verification (account_email_verification_id, account_id, code,
				email_address, expiration) VALUES (?,?,?,?,?)
				""", accountEmailVerificationId, accountId, code, emailAddress, expiration);

		// After transaction commits, send an email to the address so we can verify it
		Account pinnedAccount = account;

		// Email verification is worded a little differently when it occurs during the appointment booking flow
		String title = accountEmailVerificationFlowTypeId == AccountEmailVerificationFlowTypeId.APPOINTMENT_BOOKING ?
				getStrings().get("Appointment Confirmation Code") : getStrings().get("Email Confirmation Code");

		String codeTypeDescription = title.toLowerCase(account.getLocale());
		String salutation = getFormatter().formatEmailSalutation(account);

		getDatabase().currentTransaction().get().addPostCommitOperation(() -> {
			EmailMessage verificationEmail = new EmailMessage.Builder(
					EmailMessageTemplate.ACCOUNT_EMAIL_VERIFICATION, pinnedAccount.getLocale())
					.toAddresses(new ArrayList<>() {{
						add(emailAddress);
					}})
					.messageContext(new HashMap<String, Object>() {{
						put("title", title);
						put("salutation", salutation);
						put("codeTypeDescription", codeTypeDescription);
						put("code", code);
					}})
					.build();

			getEmailMessageManager().enqueueMessage(verificationEmail);
		});

		return accountEmailVerificationId;
	}

	@Nonnull
	public void applyAccountEmailVerificationCode(@Nonnull ApplyAccountEmailVerificationCodeRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		String code = trimToNull(request.getCode());
		String emailAddress = getNormalizer().normalizeEmailAddress(request.getEmailAddress()).orElse(null);

		ValidationException validationException = new ValidationException();

		if (accountId == null)
			validationException.add(new FieldError("accountId", "Account ID is required."));

		if (code == null)
			validationException.add(new FieldError("code", "Code is required."));

		if (emailAddress == null)
			validationException.add(new FieldError("emailAddress", "Email address is required."));

		if (validationException.hasErrors())
			throw validationException;

		// If this email has already been verified for this account, nothing to do
		if (isEmailAddressVerifiedForAccountId(emailAddress, accountId))
			return;

		// Friendly error handling - see if code is valid but expired
		boolean validButExpiredCode = getDatabase().queryForObject("""
				SELECT COUNT(*) > 0
				FROM account_email_verification
				WHERE verified=FALSE 
				AND account_id=?
				AND code=?
				AND email_address=?
				AND NOW() >= expiration
				""", Boolean.class, accountId, code, emailAddress).get();

		if (validButExpiredCode)
			throw new ValidationException(getStrings().get(
					"Sorry, this code is expired and cannot be used to verify your email address."));

		boolean verified = getDatabase().execute("""
				UPDATE account_email_verification
				SET verified=TRUE
				WHERE account_id=?
				AND code=?
				AND email_address=?
				AND NOW() < expiration
				""", accountId, code, emailAddress) > 0;

		if (!verified)
			throw new ValidationException(getStrings().get(
					"Sorry, we could not use this code to verify your email address.  Please make sure you typed it in correctly."));
	}

	@Nonnull
	public Boolean isEmailAddressVerifiedForAccountId(@Nullable String emailAddress,
																										@Nullable UUID accountId) {
		emailAddress = getNormalizer().normalizeEmailAddress(emailAddress).orElse(null);

		if (accountId == null || emailAddress == null)
			return false;

		return getDatabase().queryForObject("""
				SELECT COUNT(*) > 0
				FROM account_email_verification
				WHERE verified=TRUE
				AND email_address=?
				AND account_id=?
				""", Boolean.class, emailAddress, accountId).get();
	}

	@Nonnull
	public void updateAccountLocation(@Nonnull UpdateAccountLocationRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		String institutionLocationIdString = trimToNull(request.getInstitutionLocationId());
		UUID institutionLocationId = null;
		ValidationException validationException = new ValidationException();

		if (accountId == null) {
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));
		} else {
			Account account = findAccountById(accountId).orElse(null);

			if (account == null)
				validationException.add(new FieldError("accountId", getStrings().get("Account ID is invalid.")));
		}

		if (institutionLocationIdString != null && !ValidationUtility.isValidUUID(institutionLocationIdString))
			validationException.add(new FieldError("location", getStrings().get("Location ID is invalid.")));
		else if (institutionLocationIdString != null)
			institutionLocationId = UUID.fromString(institutionLocationIdString);

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("UPDATE account SET institution_location_id = ? WHERE account_id = ?", institutionLocationId, accountId);
	}

	@Nonnull
	public List<GenderIdentity> findGenderIdentities() {
		return getDatabase().queryForList("SELECT * FROM gender_identity ORDER BY display_order", GenderIdentity.class);
	}

	@Nonnull
	public List<Race> findRaces() {
		return getDatabase().queryForList("SELECT * FROM race ORDER BY display_order", Race.class);
	}

	@Nonnull
	public List<BirthSex> findBirthSexes() {
		return getDatabase().queryForList("SELECT * FROM birth_sex ORDER BY display_order", BirthSex.class);
	}

	@Nonnull
	public List<Ethnicity> findEthnicities() {
		return getDatabase().queryForList("SELECT * FROM ethnicity ORDER BY display_order", Ethnicity.class);
	}

	@Nonnull
	protected Set<Locale> determineAccountLanguages() {
		return Arrays.stream(Locale.getISOLanguages())
				.map(languageCode -> new Locale(languageCode))
				.collect(Collectors.toSet());
	}

	@Nonnull
	protected Set<Locale> determineAccountCountries() {
		return Arrays.stream(Locale.getISOCountries())
				.map(countryCode -> new Locale("", countryCode))
				.collect(Collectors.toSet());
	}

	@Nonnull
	protected Set<ZoneId> determineAccountTimeZones() {
		// Basically USA-only for now
		return Set.of(
				ZoneId.of("America/Anchorage"),
				ZoneId.of("America/Juneau"),
				ZoneId.of("America/Metlakatla"),
				ZoneId.of("America/Nome"),
				ZoneId.of("America/Sitka"),
				ZoneId.of("America/Yakutat"),
				ZoneId.of("America/Puerto_Rico"),
				ZoneId.of("America/Chicago"),
				ZoneId.of("America/Indiana/Knox"),
				ZoneId.of("America/Indiana/Tell_City"),
				ZoneId.of("America/Menominee"),
				ZoneId.of("America/North_Dakota/Beulah"),
				ZoneId.of("America/North_Dakota/Center"),
				ZoneId.of("America/North_Dakota/New_Salem"),
				ZoneId.of("America/Detroit"),
				ZoneId.of("America/Fort_Wayne"),
				ZoneId.of("America/Indiana/Indianapolis"),
				ZoneId.of("America/Kentucky/Louisville"),
				ZoneId.of("America/Kentucky/Monticello"),
				ZoneId.of("America/New_York"),
				ZoneId.of("America/Adak"),
				ZoneId.of("America/Atka"),
				ZoneId.of("America/Boise"),
				ZoneId.of("America/Denver"),
				ZoneId.of("America/Phoenix"),
				ZoneId.of("America/Shiprock"),
				ZoneId.of("America/Los_Angeles")
		);
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}

	@Nonnull
	protected AuditLogService getAuditLogService() {
		return auditLogServiceProvider.get();
	}

	@Nonnull
	protected InteractionService getInteractionService() {
		return interactionServiceProvider.get();
	}

	@Nonnull
	protected AddressService getAddressService() {
		return this.addressServiceProvider.get();
	}

	@Nonnull
	protected PatientOrderService getPatientOrderService() {
		return this.patientOrderServiceProvider.get();
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

	@Nonnull
	protected ErrorReporter getErrorReporter() {
		return errorReporter;
	}

	@Nonnull
	public Set<ZoneId> getAccountTimeZones() {
		return this.accountTimeZones;
	}

	@Nonnull
	public Set<Locale> getAccountLanguages() {
		return this.accountLanguages;
	}

	@Nonnull
	public Set<Locale> getAccountCountries() {
		return this.accountCountries;
	}
}
