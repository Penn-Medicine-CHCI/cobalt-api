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
import com.cobaltplatform.api.integration.enterprise.EnterprisePlugin;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.integration.epic.EpicClient;
import com.cobaltplatform.api.integration.epic.MyChartAccessToken;
import com.cobaltplatform.api.integration.epic.MyChartAuthenticator;
import com.cobaltplatform.api.integration.epic.code.AddressUseCode;
import com.cobaltplatform.api.integration.epic.code.NameUseCode;
import com.cobaltplatform.api.integration.epic.code.TelecomUseCode;
import com.cobaltplatform.api.integration.epic.response.PatientReadFhirR4Response;
import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.api.request.CreateAddressRequest;
import com.cobaltplatform.api.model.api.request.CreateOrUpdateMyChartAccountRequest;
import com.cobaltplatform.api.model.api.request.ObtainMyChartAccessTokenRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.BirthSex.BirthSexId;
import com.cobaltplatform.api.model.db.Ethnicity.EthnicityId;
import com.cobaltplatform.api.model.db.GenderIdentity.GenderIdentityId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Race.RaceId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.security.SigningTokenClaims;
import com.cobaltplatform.api.model.service.MyChartAccessTokenWithClaims;
import com.cobaltplatform.api.util.Authenticator;
import com.cobaltplatform.api.util.Normalizer;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.db.DatabaseProvider;
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
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
public class MyChartService {
	@Nonnull
	private static final String SIGNING_TOKEN_NAME;
	@Nonnull
	private static final Long SIGNING_TOKEN_EXPIRATION_IN_SECONDS;
	@Nonnull
	private static final String ENVIRONMENT_CLAIMS_NAME;
	@Nonnull
	private static final String INSTITUTION_ID_CLAIMS_NAME;

	@Nonnull
	private final Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final Authenticator authenticator;
	@Nonnull
	private final Normalizer normalizer;
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	static {
		SIGNING_TOKEN_NAME = "mychart";
		SIGNING_TOKEN_EXPIRATION_IN_SECONDS = 30L * 60L;
		ENVIRONMENT_CLAIMS_NAME = "environment";
		INSTITUTION_ID_CLAIMS_NAME = "institutionId";
	}

	@Inject
	public MyChartService(@Nonnull Provider<InstitutionService> institutionServiceProvider,
												@Nonnull Provider<AccountService> accountServiceProvider,
												@Nonnull EnterprisePluginProvider enterprisePluginProvider,
												@Nonnull Authenticator authenticator,
												@Nonnull Normalizer normalizer,
												@Nonnull DatabaseProvider databaseProvider,
												@Nonnull Configuration configuration,
												@Nonnull Strings strings) {
		requireNonNull(institutionServiceProvider);
		requireNonNull(accountServiceProvider);
		requireNonNull(enterprisePluginProvider);
		requireNonNull(authenticator);
		requireNonNull(normalizer);
		requireNonNull(databaseProvider);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.institutionServiceProvider = institutionServiceProvider;
		this.accountServiceProvider = accountServiceProvider;
		this.enterprisePluginProvider = enterprisePluginProvider;
		this.authenticator = authenticator;
		this.normalizer = normalizer;
		this.databaseProvider = databaseProvider;
		this.configuration = configuration;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public String generateAuthenticationUrlForInstitutionId(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);
		return generateAuthenticationUrlForInstitutionId(institutionId, null);
	}

	@Nonnull
	public String generateAuthenticationUrlForInstitutionId(@Nonnull InstitutionId institutionId,
																													@Nullable Map<String, Object> claims) {
		requireNonNull(institutionId);

		if (claims == null)
			claims = Collections.emptyMap();

		if (claims.containsKey(getEnvironmentClaimsName()))
			throw new IllegalArgumentException(format("The claims name '%s' is reserved.", getEnvironmentClaimsName()));

		EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(institutionId);
		MyChartAuthenticator myChartAuthenticator = enterprisePlugin.myChartAuthenticator().orElse(null);

		if (myChartAuthenticator == null)
			throw new ValidationException(getStrings().get("MyChart is not available for this institution."));

		// Use a signing token (basically, a JWT with short lifespan) to encode our OAuth "state".
		// This lets us verify the callback from MyChart is actually coming from a request initiated by us - not someone
		// else forging a request and pretending to be MyChart - and also permits attaching arbitrary data
		// for later use by our callback.
		//
		// By default, we add the environment to the signing token JWT's claims so we can key off of it when we get our OAuth
		// callback.  This is useful for development, e.g. the scenario where we want to use the real MyChart flow
		// but have it redirect back from a dev environment to a local environment instead
		Map<String, Object> finalClaims = new HashMap<>(claims.size() + 2);
		finalClaims.putAll(claims);
		finalClaims.put(getEnvironmentClaimsName(), getConfiguration().getEnvironment());
		finalClaims.put(getInstitutionIdClaimsName(), institutionId.name());

		String state = getAuthenticator().generateSigningToken(getSigningTokenName(), getSigningTokenExpirationInSeconds(), finalClaims);

		return myChartAuthenticator.generateAuthenticationRedirectUrl(state);
	}

	@Nonnull
	public Map<String, Object> extractAndValidateClaimsFromMyChartState(@Nonnull InstitutionId institutionId,
																																			@Nonnull String state) {
		requireNonNull(institutionId);
		requireNonNull(state);

		SigningTokenClaims stateClaims = null;
		ValidationException validationException = new ValidationException();

		try {
			stateClaims = getAuthenticator().validateSigningToken(state);
		} catch (Exception e) {
			getLogger().warn("Unable to validate MyChart state", e);
			validationException.add(new FieldError("state", "Unable to validate MyChart state."));
		}

		if (stateClaims != null) {
			InstitutionId claimsInstitutionId = InstitutionId.valueOf((String) stateClaims.getClaims().get(getInstitutionIdClaimsName()));

			// This suggests something is pretty wrong and needs further investigation
			if (claimsInstitutionId != institutionId)
				throw new IllegalStateException(format("Institution ID %s in MyChart state claims doesn't match expected institution %s.",
						claimsInstitutionId == null ? "[null]" : claimsInstitutionId.name(), institutionId.name()));
		}

		if (validationException.hasErrors())
			throw validationException;

		return stateClaims.getClaims();
	}

	@Nonnull
	public MyChartAccessTokenWithClaims obtainMyChartAccessToken(@Nonnull ObtainMyChartAccessTokenRequest request) {
		requireNonNull(request);

		String code = trimToNull(request.getCode());
		String state = trimToNull(request.getState());
		InstitutionId institutionId = request.getInstitutionId();
		EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(institutionId);
		MyChartAuthenticator myChartAuthenticator = enterprisePlugin.myChartAuthenticator().get();
		Map<String, Object> claims = null;
		ValidationException validationException = new ValidationException();

		if (code == null)
			validationException.add(new FieldError("code", "Code is required."));

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", "Institution ID is required."));

		if (state == null) {
			validationException.add(new FieldError("state", "State is required."));
		} else {
			// Ensure we have non-spoofed claims for this state
			try {
				claims = extractAndValidateClaimsFromMyChartState(institutionId, state);
			} catch (ValidationException e) {
				validationException.add(e);
			}
		}

		if (validationException.hasErrors())
			throw validationException;

		try {
			MyChartAccessToken myChartAccessToken = myChartAuthenticator.obtainAccessTokenFromCode(code, state);
			return new MyChartAccessTokenWithClaims(myChartAccessToken, claims);
		} catch (Exception e) {
			getLogger().warn("Unable to obtain a MyChart access token", e);
			throw new ValidationException(getStrings().get("Unable to obtain a MyChart access token."));
		}
	}

	@Nonnull
	public UUID createOrUpdateAccount(@Nonnull CreateOrUpdateMyChartAccountRequest request) {
		requireNonNull(request);

		String epicPatientFhirId = null;
		PatientReadFhirR4Response patient = null;
		MyChartAccessToken myChartAccessToken = request.getMyChartAccessToken();
		InstitutionId institutionId = request.getInstitutionId();
		EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(institutionId);
		EpicClient epicClient = enterprisePlugin.epicClientForPatient(myChartAccessToken).get();
		ValidationException validationException = new ValidationException();

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", "Institution ID is required."));

		if (myChartAccessToken == null) {
			validationException.add(new FieldError("myChartAccessToken", "MyChart access token is required."));
		} else {
			epicPatientFhirId = enterprisePlugin.extractPatientFhirIdFromMyChartAccessToken(myChartAccessToken).orElse(null);

			if (epicPatientFhirId == null)
				validationException.add(new FieldError("myChartAccessToken", "Cannot find patient FHIR ID in MyChart access token."));
		}

		if (validationException.hasErrors())
			throw validationException;

		// TODO: remove this line once patient app has requisite access to Patient.Read (R4).
		epicClient = enterprisePlugin.epicClientForBackendService().get();

		try {
			// This check lets Epic confirm signature/authentication/authorization of the patient's short-lived MyChart token, preventing forgery
			patient = epicClient.patientReadFhirR4(epicPatientFhirId).orElse(null);

			if (patient == null) {
				getLogger().warn("Unable to find record in EPIC for patient FHIR ID {}", epicPatientFhirId);
				validationException.add(getStrings().get("Unable to find patient record in EPIC."));
			}
		} catch (Exception e) {
			getLogger().warn(format("Unable to load patient data from EPIC for patient FHIR ID %s", epicPatientFhirId), e);
			validationException.add(getStrings().get("Unable to load patient data from EPIC."));
		}

		if (validationException.hasErrors())
			throw validationException;

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();

		String epicPatientMrn = institution.getEpicPatientMrnSystem() != null
				? patient.extractIdentifierBySystem(institution.getEpicPatientMrnSystem()).orElse(null)
				: patient.extractIdentifierByType(institution.getEpicPatientMrnTypeName()).orElse(null);

		// One last try
		if (epicPatientMrn == null)
			epicPatientMrn = patient.extractIdentifierByType(institution.getEpicPatientMrnTypeAlternateName()).orElse(null);

		// Failsafe; should never occur unless institution is misconfigured
		if (epicPatientMrn == null)
			throw new IllegalStateException(format("Unable to determine MRN for patient FHIR ID '%s'", epicPatientFhirId));

		String ssoId = epicPatientMrn;
		String epicPatientUniqueId = null;
		String epicPatientUniqueIdType = null;

		if (institution.getEpicPatientUniqueIdType() != null && institution.getEpicPatientUniqueIdSystem() != null) {
			epicPatientUniqueId = patient.extractIdentifierBySystem(institution.getEpicPatientUniqueIdSystem()).orElse(null);
			epicPatientUniqueIdType = institution.getEpicPatientUniqueIdType();

			// Failsafe; should never occur unless institution is misconfigured
			if (epicPatientUniqueId == null)
				throw new IllegalStateException(format("Unable to determine unique ID for patient FHIR ID '%s'", epicPatientFhirId));

			ssoId = epicPatientUniqueId;
		}

		Account existingAccount = getAccountService().findAccountByAccountSourceIdAndSsoIdAndInstitutionId(AccountSourceId.MYCHART, ssoId, institutionId).orElse(null);

		// Account already exists for this account source/SSO ID/institution, return it instead of creating another
		if (existingAccount != null)
			return existingAccount.getAccountId();

		GenderIdentityId genderIdentityId = patient.extractGenderIdentityId().orElse(null);
		RaceId raceId = patient.extractRaceId().orElse(null);
		EthnicityId ethnicityId = patient.extractEthnicityId().orElse(null);
		BirthSexId birthSexId = patient.extractBirthSexId().orElse(null);
		LocalDate birthdate = patient.getBirthDate();
		PatientReadFhirR4Response.Name name = patient.extractFirstMatchingName(NameUseCode.OFFICIAL, NameUseCode.USUAL).orElse(null);
		PatientReadFhirR4Response.Address address = patient.extractFirstMatchingAddress(AddressUseCode.HOME, AddressUseCode.BILLING, AddressUseCode.TEMP).orElse(null);
		String phoneNumber = patient.extractFirstMatchingPhoneNumber(TelecomUseCode.MOBILE, TelecomUseCode.HOME, TelecomUseCode.WORK).orElse(null);
		List<String> emailAddresses = patient.extractEmailAddresses();
		String ssoAttributesAsJson = patient.getRawJson();

		String firstName = name == null || name.getGiven() == null ? null : name.getGiven().stream().collect(Collectors.joining(" "));
		String lastName = name == null || name.getFamily() == null ? null : trimToNull(name.getFamily());
		String displayName = name == null || name.getText() == null ? null : trimToNull(name.getText());
		String emailAddress = emailAddresses.size() == 0 ? null : getNormalizer().normalizeEmailAddress(emailAddresses.get(0)).orElse(null);
		// TODO: revisit once we support non-US EPIC users
		String normalizedPhoneNumber = phoneNumber == null ? null : getNormalizer().normalizePhoneNumberToE164(phoneNumber, Locale.US).orElse(null);

		CreateAddressRequest addressRequest = null;

		if (address != null) {
			addressRequest = new CreateAddressRequest();
			addressRequest.setPostalName(displayName);

			if (address.getLine() != null) {
				for (int i = 0; i < address.getLine().size(); ++i) {
					String line = trimToNull(address.getLine().get(i));

					if (line != null) {
						if (i == 0)
							addressRequest.setStreetAddress1(line);
						else if (i == 1)
							addressRequest.setStreetAddress2(line);
						else if (i == 2)
							addressRequest.setStreetAddress3(line);
						else if (i == 3)
							addressRequest.setStreetAddress4(line);
					}
				}
			}

			// TODO: revisit once we support non-US EPIC users
			addressRequest.setLocality(trimToNull(address.getCity()));
			addressRequest.setRegion(trimToNull(address.getState()));
			addressRequest.setPostalCode(trimToNull(address.getPostalCode()));
			addressRequest.setCountryCode(trimToNull(address.getCountry()));

			// If we don't have sufficient information to compose a valid US address,
			// then pretend we have no address at all
			boolean validUsAddress = addressRequest.getPostalName() != null
					&& addressRequest.getStreetAddress1() != null
					&& addressRequest.getLocality() != null
					&& addressRequest.getRegion() != null
					&& addressRequest.getPostalCode() != null
					&& addressRequest.getCountryCode() != null;

			if (!validUsAddress)
				addressRequest = null;
		}

		CreateAddressRequest pinnedAddressRequest = addressRequest;
		String pinnedEpicPatientMrn = epicPatientMrn;
		String pinnedEpicPatientFhirId = epicPatientFhirId;
		String pinnedEpicPatientUniqueId = epicPatientUniqueId;
		String pinnedEpicPatientUniqueIdType = epicPatientUniqueIdType;
		String pinnedSsoId = ssoId;

		return getAccountService().createAccount(new CreateAccountRequest() {{
			setEpicPatientMrn(pinnedEpicPatientMrn);
			setEpicPatientFhirId(pinnedEpicPatientFhirId);
			setEpicPatientUniqueId(pinnedEpicPatientUniqueId);
			setEpicPatientUniqueIdType(pinnedEpicPatientUniqueIdType);
			setAccountSourceId(AccountSourceId.MYCHART);
			setRoleId(RoleId.PATIENT);
			setSsoId(pinnedSsoId);
			setInstitutionId(institutionId);
			setSsoAttributesAsJson(ssoAttributesAsJson);
			setGenderIdentityId(genderIdentityId);
			setRaceId(raceId);
			setEthnicityId(ethnicityId);
			setBirthSexId(birthSexId);
			setBirthdate(birthdate);
			setDisplayName(displayName);
			setFirstName(firstName);
			setLastName(lastName);
			setEmailAddress(emailAddress);
			setPhoneNumber(normalizedPhoneNumber);
			setAddress(pinnedAddressRequest);
		}});
	}

	@Nonnull
	protected String getSigningTokenName() {
		return SIGNING_TOKEN_NAME;
	}

	@Nonnull
	protected Long getSigningTokenExpirationInSeconds() {
		return SIGNING_TOKEN_EXPIRATION_IN_SECONDS;
	}

	@Nonnull
	protected String getEnvironmentClaimsName() {
		return ENVIRONMENT_CLAIMS_NAME;
	}

	@Nonnull
	protected String getInstitutionIdClaimsName() {
		return INSTITUTION_ID_CLAIMS_NAME;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountServiceProvider.get();
	}

	@Nonnull
	protected EnterprisePluginProvider getEnterprisePluginProvider() {
		return this.enterprisePluginProvider;
	}

	@Nonnull
	protected Authenticator getAuthenticator() {
		return this.authenticator;
	}

	@Nonnull
	protected Normalizer getNormalizer() {
		return this.normalizer;
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
