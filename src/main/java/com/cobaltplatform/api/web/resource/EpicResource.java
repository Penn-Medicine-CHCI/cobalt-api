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
import com.cobaltplatform.api.integration.epic.EpicClient;
import com.cobaltplatform.api.integration.epic.matching.EpicPatientMatch;
import com.cobaltplatform.api.integration.epic.matching.EpicPatientMatchRequest;
import com.cobaltplatform.api.integration.epic.matching.EpicPatientMatcher;
import com.cobaltplatform.api.integration.epic.request.PatientCreateRequest;
import com.cobaltplatform.api.integration.epic.response.PatientCreateResponse;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageManager;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.api.response.AccountApiResponse.AccountApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AuditLog;
import com.cobaltplatform.api.model.db.AuditLogEvent.AuditLogEventId;
import com.cobaltplatform.api.model.db.EpicDepartment;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.qualifier.AuditLogged;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AppointmentService;
import com.cobaltplatform.api.service.AuditLogService;
import com.cobaltplatform.api.service.ProviderService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.Normalizer;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.ValidationUtility;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.lokalized.Strings;
import com.pyranid.Database;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
import com.soklet.web.response.ApiResponse;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.util.ArrayList;
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
@Resource
@Singleton
@ThreadSafe
public class EpicResource {
	@Nonnull
	private final AccountService accountService;
	@Nonnull
	private final ProviderService providerService;
	@Nonnull
	private final AppointmentService appointmentService;
	@Nonnull
	private final AuditLogService auditLogService;
	@Nonnull
	private final EpicClient epicClient;
	@Nonnull
	private final EpicPatientMatcher epicPatientMatcher;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Normalizer normalizer;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final AccountApiResponseFactory accountApiResponseFactory;
	@Nonnull
	private final EmailMessageManager emailMessageManager;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;

	@Inject
	public EpicResource(@Nonnull AccountService accountService,
											@Nonnull ProviderService providerService,
											@Nonnull AppointmentService appointmentService,
											@Nonnull AuditLogService auditLogService,
											@Nonnull @AuditLogged EpicClient epicClient,
											@Nonnull EpicPatientMatcher epicPatientMatcher,
											@Nonnull RequestBodyParser requestBodyParser,
											@Nonnull Database database,
											@Nonnull Configuration configuration,
											@Nonnull JsonMapper jsonMapper,
											@Nonnull Formatter formatter,
											@Nonnull Normalizer normalizer,
											@Nonnull Strings strings,
											@Nonnull AccountApiResponseFactory accountApiResponseFactory,
											@Nonnull EmailMessageManager emailMessageManager,
											@Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(accountService);
		requireNonNull(providerService);
		requireNonNull(appointmentService);
		requireNonNull(auditLogService);
		requireNonNull(epicClient);
		requireNonNull(epicPatientMatcher);
		requireNonNull(requestBodyParser);
		requireNonNull(database);
		requireNonNull(configuration);
		requireNonNull(jsonMapper);
		requireNonNull(formatter);
		requireNonNull(normalizer);
		requireNonNull(strings);
		requireNonNull(accountApiResponseFactory);
		requireNonNull(emailMessageManager);
		requireNonNull(currentContextProvider);

		this.accountService = accountService;
		this.providerService = providerService;
		this.appointmentService = appointmentService;
		this.auditLogService = auditLogService;
		this.epicClient = epicClient;
		this.epicPatientMatcher = epicPatientMatcher;
		this.requestBodyParser = requestBodyParser;
		this.database = database;
		this.configuration = configuration;
		this.jsonMapper = jsonMapper;
		this.formatter = formatter;
		this.normalizer = normalizer;
		this.strings = strings;
		this.accountApiResponseFactory = accountApiResponseFactory;
		this.emailMessageManager = emailMessageManager;
		this.currentContextProvider = currentContextProvider;
	}

	@Nonnull
	@AuthenticationRequired
	@POST("/epic/patient-match")
	public ApiResponse epicPatientMatch(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account originalAccount = getCurrentContext().getAccount().get();
		com.cobaltplatform.api.model.db.Provider provider = null;
		EpicDepartment epicDepartment = null;
		EpicPatientMatchRequest request = getRequestBodyParser().parse(requestBody, EpicPatientMatchRequest.class);

		AuditLog matchAuditLog = new AuditLog();
		matchAuditLog.setAccountId(originalAccount.getAccountId());
		matchAuditLog.setAuditLogEventId(AuditLogEventId.EPIC_MATCH_ATTEMPT);
		matchAuditLog.setPayload(requestBody);
		getAuditLogService().audit(matchAuditLog);

		String firstName = trimToNull(request.getFirstName());
		String lastName = trimToNull(request.getLastName());
		String middleInitial = trimToNull(request.getMiddleInitial());
		LocalDate dateOfBirth = request.getDateOfBirth();
		EpicPatientMatchRequest.Address address = request.getAddress();
		String addressLine1 = address == null ? null : trimToNull(address.getLine1());
		String addressLine2 = address == null ? null : trimToNull(address.getLine2());
		String addressCity = address == null ? null : trimToNull(address.getCity());
		String addressState = address == null ? null : trimToNull(address.getState());
		String addressPostalCode = address == null ? null : trimToNull(address.getPostalCode());
		String addressCountry = address == null ? null : trimToNull(address.getCountry());
		String emailAddress = trimToNull(request.getEmailAddress());
		String phoneNumber = trimToNull(request.getPhoneNumber());
		String phoneType = trimToNull(request.getPhoneType());
		String nationalIdentifier = trimToNull(request.getNationalIdentifier());
		EpicPatientMatchRequest.Gender gender = request.getGender();
		UUID providerId = request.getProviderId();
		UUID epicDepartmentId = request.getEpicDepartmentId();
		boolean applyToCurrentAccount = request.getApplyToCurrentAccount() == null ? false : request.getApplyToCurrentAccount();
		EpicPatientMatchRequest.MatchStep matchStep = request.getMatchStep();

		boolean validateName = false;
		boolean validateDateOfBirth = false;
		boolean validatePhoneNumber = false;
		boolean validateEmailAddress = false;
		boolean validateAddress = false;
		boolean validateNationalIdentifier = false;
		boolean validateGender = false;

		ValidationException validationException = new ValidationException();

		if (matchStep == null) {
			validationException.add(new FieldError("matchStep", getStrings().get("Match step is required.")));
		} else {
			int value = matchStep.getValue();

			if (value >= 1) {
				validateName = true;
				validateDateOfBirth = true;
			}

			if (value >= 2) {
				validatePhoneNumber = true;
				validateEmailAddress = true;
				validateAddress = true;
			}

			if (value >= 3) {
				validateNationalIdentifier = true;
				validateGender = true;
			}
		}

		// Global validation
		if (providerId == null) {
			validationException.add(new FieldError("providerId", getStrings().get("Provider ID is required.")));
		} else {
			provider = getProviderService().findProviderById(request.getProviderId()).orElse(null);

			if (provider == null) {
				validationException.add(new FieldError("providerId", getStrings().get("Provider ID is invalid.")));
			} else if (provider.getSchedulingSystemId() != SchedulingSystemId.EPIC) {
				validationException.add(new FieldError("providerId", getStrings().get("This provider is not configured for EPIC.")));
			} else {
				if (epicDepartmentId == null) {
					validationException.add(new FieldError("epicDepartmentId", getStrings().get("EPIC department ID is required.")));
				} else {
					List<EpicDepartment> epicDepartments = getAppointmentService().findEpicDepartmentsByProviderId(providerId);

					for (EpicDepartment potentialEpicDepartment : epicDepartments) {
						if (potentialEpicDepartment.getEpicDepartmentId().equals(epicDepartmentId)) {
							epicDepartment = potentialEpicDepartment;
							break;
						}
					}

					if (epicDepartment == null)
						validationException.add(getStrings().get("Can't find an appropriate department in EPIC for the selected provider."));
				}
			}
		}

		if (validateName) {
			if (firstName == null)
				validationException.add(new FieldError("firstName", getStrings().get("First name is required.")));
			if (lastName == null)
				validationException.add(new FieldError("lastName", getStrings().get("Last name is required.")));
		}

		if (validateDateOfBirth) {
			if (dateOfBirth == null)
				validationException.add(new FieldError("dateOfBirth", getStrings().get("Date of birth is required.")));
			else if (request.getDateOfBirth().isAfter(LocalDate.now(getCurrentContext().getTimeZone())))
				validationException.add(new FieldError("dateOfBirth", getStrings().get("Date of birth must be in the past.")));
		}

		if (validateEmailAddress) {
			if (emailAddress == null) {
				validationException.add(new FieldError("emailAddress", getStrings().get("Email address is required.")));
			} else {
				emailAddress = getNormalizer().normalizeEmailAddress(emailAddress).orElse(null);

				if (emailAddress == null || !ValidationUtility.isValidEmailAddress(emailAddress))
					validationException.add(new FieldError("emailAddress", getStrings().get("Email address is invalid.")));
			}
		}

		if (validatePhoneNumber) {
			if (phoneNumber == null) {
				validationException.add(new FieldError("phoneNumber", getStrings().get("Phone number is required.")));
			} else {
				phoneNumber = getNormalizer().normalizePhoneNumberToE164(phoneNumber).orElse(null);

				if (phoneNumber == null)
					validationException.add(new FieldError("phoneNumber", getStrings().get("Phone number is invalid.")));
			}
		}

		if (validateAddress) {
			if (addressLine1 == null)
				validationException.add(new FieldError("address.line1", getStrings().get("Street address is required.")));
			if (addressCity == null)
				validationException.add(new FieldError("address.city", getStrings().get("City is required.")));
			if (addressState == null)
				validationException.add(new FieldError("address.state", getStrings().get("State is required.")));
			if (addressPostalCode == null)
				validationException.add(new FieldError("address.postalCode", getStrings().get("Postal code is required.")));
		}

		if (validateNationalIdentifier) {
			if (nationalIdentifier == null) {
				validationException.add(new FieldError("nationalIdentifier", getStrings().get("SSN is required.")));
			} else {
				nationalIdentifier = nationalIdentifier.toUpperCase(Locale.US);

				boolean isLastFour = StringUtils.isNumeric(nationalIdentifier) && nationalIdentifier.length() == 4;

				// Here, only check last 4.  We check the whole thing if we need to create a new EPIC account
				if (!isLastFour && !isFullSocialSecurityNumber(nationalIdentifier))
					validationException.add(new FieldError("nationalIdentifier", getStrings().get("SSN is invalid.")));
			}
		}

		if (validateGender) {
			if (gender == null)
				validationException.add(new FieldError("gender", getStrings().get("Gender is required.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		EpicPatientMatchRequest matchRequest = new EpicPatientMatchRequest();
		matchRequest.setPhoneNumber(phoneNumber);
		matchRequest.setPhoneType(phoneType);
		matchRequest.setNationalIdentifier(nationalIdentifier);

		if (gender != null)
			matchRequest.setGender(EpicPatientMatchRequest.Gender.valueOf(gender.name()));

		matchRequest.setDateOfBirth(dateOfBirth);
		matchRequest.setLastName(lastName);
		matchRequest.setFirstName(firstName);
		matchRequest.setMiddleInitial(middleInitial);
		matchRequest.setEmailAddress(emailAddress);

		if (address != null)
			matchRequest.setAddress(new EpicPatientMatchRequest.Address() {
				{
					setLine1(addressLine1);
					setLine2(addressLine2);
					setCity(addressCity);
					setState(addressState);
					setPostalCode(addressPostalCode);
					setCountry(addressCountry);
				}
			});

		Integer matchCount = 0;
		Integer matchScoreThreshold = getEpicPatientMatcher().getLowThreshold();

		Integer topMatchScore = 0;
		Double topMatchPercentage = 0D;
		String topMatchConfidence = "LOW";
		String topMatchConfidenceDescription = getStrings().get("low");

		boolean appliedToCurrentAccount = false;
		Account updatedAccount = null;

		List<EpicPatientMatch> matches = getEpicPatientMatcher().match(matchRequest);

		List<EpicPatientMatch> matchesMeetingThreshold = matches.stream()
				.filter(match -> match.getScore() >= matchScoreThreshold)
				.collect(Collectors.toList());

		matchCount = matchesMeetingThreshold.size();

		EpicPatientMatch topMatch = matches.size() > 0 ? matches.get(0) : null;

		if (topMatch != null) {
			topMatchScore = topMatch.getScore();
			topMatchPercentage = Math.min(1, (double) topMatch.getScore() / (double) matchScoreThreshold);

			// Do we need to tweak these numbers?
			if (topMatchPercentage > 0.7) {
				topMatchConfidence = "HIGH";
				topMatchConfidenceDescription = getStrings().get("high");
			} else if (topMatchPercentage > 0.5) {
				topMatchConfidence = "MEDIUM";
				topMatchConfidenceDescription = getStrings().get("medium");
			}
		}

		if (applyToCurrentAccount) {
			if (matchesMeetingThreshold.size() > 1) {
				// Error: you match multiple accounts
				Map<String, Object> messageContext = new HashMap<>();
				messageContext.put("phoneNumberDescription", getFormatter().formatPhoneNumber(phoneNumber));

				getEmailMessageManager().enqueueMessage(new EmailMessage.Builder(EmailMessageTemplate.MULTIPLE_EPIC_MATCHES, originalAccount.getLocale())
						.toAddresses(new ArrayList<>() {{
							add("cobaltplatform@xmog.com");
						}})
						.messageContext(messageContext)
						.build());

				Map<String, Object> payload = new HashMap<>();
				payload.put("request", request);
				payload.put("phoneNumber", getFormatter().formatPhoneNumber(phoneNumber));
				payload.put("matches", matchesMeetingThreshold);

				AuditLog auditLog = new AuditLog();
				auditLog.setAccountId(originalAccount.getAccountId());
				auditLog.setAuditLogEventId(AuditLogEventId.EPIC_ACCOUNT_CREATE);
				auditLog.setMessage(format("%d EPIC matches met threshold for account ID %s", matchesMeetingThreshold.size(), originalAccount.getAccountId()));
				auditLog.setPayload(getJsonMapper().toJson(payload));
				getAuditLogService().audit(auditLog);
			} else {
				if (matchesMeetingThreshold.size() == 1) {
					// You match exactly one account - associate with it
					String uid = getEpicClient().extractUIDFromPatientEntry(topMatch.getPatient()).get();
					getAccountService().updateAccountEpicPatient(originalAccount.getAccountId(), uid, "UID", false);

					Map<String, Object> payload = new HashMap<>();
					payload.put("request", request);
					payload.put("uid", uid);
					payload.put("patient", topMatch.getPatient());

					AuditLog auditLog = new AuditLog();
					auditLog.setAccountId(originalAccount.getAccountId());
					auditLog.setAuditLogEventId(AuditLogEventId.EPIC_ACCOUNT_ASSOCIATE);
					auditLog.setMessage(format("Associated account ID %s with EPIC UID %s", originalAccount.getAccountId(), uid));
					auditLog.setPayload(getJsonMapper().toJson(payload));
					getAuditLogService().audit(auditLog);
				} else {
					// You don't match any accounts - create a new patient record

					// We need your full SSN here
					if (!isFullSocialSecurityNumber(nationalIdentifier))
						throw new ValidationException(new FieldError("nationalIdentifier", getStrings().get("SSN is invalid.")));

					PatientCreateRequest.Name name = new PatientCreateRequest.Name();
					name.setFirst(request.getFirstName());
					name.setLastName(request.getLastName());
					name.setMiddle(request.getMiddleInitial());

					PatientCreateRequest.Address createAddress = new PatientCreateRequest.Address();
					createAddress.setStreet(addressLine1);
					createAddress.setStreetLine2(addressLine2);
					createAddress.setCity(addressCity);
					createAddress.setState(addressState);
					createAddress.setZipCode(addressPostalCode);
					createAddress.setEmail(emailAddress);

					if (phoneNumber != null) {
						PatientCreateRequest.Phone phone = new PatientCreateRequest.Phone();
						phone.setNumber(getEpicClient().formatPhoneNumber(phoneNumber));
						phone.setType(phoneType);

						createAddress.setPhones(Collections.singletonList(phone));
					}

					PatientCreateRequest createRequest = new PatientCreateRequest();
					createRequest.setName(name);
					createRequest.setDepartmentID(epicDepartment.getDepartmentId());
					createRequest.setDepartmentIDType(epicDepartment.getDepartmentIdType());
					createRequest.setGender(gender.name());
					createRequest.setNationalIdentifier(nationalIdentifier);
					createRequest.setDateOfBirth(getEpicClient().formatDateWithHyphens(dateOfBirth));
					createRequest.setAddress(createAddress);

					PatientCreateResponse createResponse = getEpicClient().performPatientCreate(createRequest);
					String hupMrnId = null;

					for (PatientCreateResponse.PatientID patientID : createResponse.getPatientIDs()) {
						if ("HUP MRN".equals(patientID.getType())) {
							hupMrnId = patientID.getID();
							break;
						}
					}

					// Exchange HUP MRN for UID
					String uid = epicClient.determineLatestUIDForPatientIdentifier(hupMrnId, "HUP MRN").get();
					getAccountService().updateAccountEpicPatient(originalAccount.getAccountId(), uid, "UID", true);

					Map<String, Object> payload = new HashMap<>();
					payload.put("request", request);
					payload.put("uid", uid);
					payload.put("patient", topMatch.getPatient());

					AuditLog auditLog = new AuditLog();
					auditLog.setAccountId(originalAccount.getAccountId());
					auditLog.setAuditLogEventId(AuditLogEventId.EPIC_ACCOUNT_CREATE);
					auditLog.setMessage(format("Created EPIC UID %s and associated with account ID %s", uid, originalAccount.getAccountId()));
					auditLog.setPayload(getJsonMapper().toJson(payload));
					getAuditLogService().audit(auditLog);
				}

				// Side effect: we update the current account's email and phone to match what they type in here
				getDatabase().execute("UPDATE account SET email_address=?, phone_number=? WHERE account_id=?",
						emailAddress, phoneNumber, originalAccount.getAccountId());

				updatedAccount = getAccountService().findAccountById(originalAccount.getAccountId()).get();
				appliedToCurrentAccount = true;
			}
		}

		Map<String, Object> response = new HashMap<>();
		response.put("matchCount", matchCount);
		response.put("matchCountDescription", getFormatter().formatNumber(matchCount));
		response.put("matchScoreThreshold", matchScoreThreshold);
		response.put("matchScoreThresholdDescription", getFormatter().formatNumber(matchScoreThreshold));
		response.put("topMatchScore", topMatchScore);
		response.put("topMatchScoreDescription", getFormatter().formatNumber(topMatchScore));
		response.put("topMatchPercentage", topMatchPercentage);
		response.put("topMatchPercentageDescription", getFormatter().formatPercent(topMatchPercentage));
		response.put("topMatchConfidence", topMatchConfidence);
		response.put("topMatchConfidenceDescription", topMatchConfidenceDescription);
		response.put("appliedToCurrentAccount", appliedToCurrentAccount);

		if (appliedToCurrentAccount)
			response.put("account", getAccountApiResponseFactory().create(updatedAccount));

		return new ApiResponse(response);
	}

	@Nonnull
	protected Boolean isFullSocialSecurityNumber(@Nullable String socialSecurityNumber) {
		socialSecurityNumber = trimToNull(socialSecurityNumber);

		if (socialSecurityNumber == null)
			return false;

		return socialSecurityNumber.matches("^\\d{3}-\\d{2}-\\d{4}$");
	}

	@Nonnull
	@AuthenticationRequired
	@POST("/epic/test-provider")
	public ApiResponse epicTestProvider(@Nonnull @RequestBody String requestBody) {
		EpicTestProviderRequest request = getRequestBodyParser().parse(requestBody, EpicTestProviderRequest.class);

		boolean dryRun = request.getDryRun() == null || request.getDryRun();

		EpicPatientMatchRequest matchRequest = new EpicPatientMatchRequest();

		matchRequest.setPhoneNumber(request.getPhoneNumber());
		matchRequest.setPhoneType(request.getPhoneType());
		matchRequest.setNationalIdentifier(request.getNationalIdentifier());

		if (request.getGender() != null)
			matchRequest.setGender(EpicPatientMatchRequest.Gender.valueOf(request.getGender().name()));

		matchRequest.setDateOfBirth(request.getDateOfBirth());
		matchRequest.setLastName(request.getLastName());
		matchRequest.setFirstName(request.getFirstName());
		matchRequest.setMiddleInitial(request.getMiddleInitial());
		matchRequest.setEmailAddress(request.getEmailAddress());

		if (request.getAddress() != null)
			matchRequest.setAddress(new EpicPatientMatchRequest.Address() {{
				setLine1(request.getAddress().getLine1());
				setCity(request.getAddress().getCity());
				setState(request.getAddress().getState());
				setPostalCode(request.getAddress().getPostalCode());
				setCountry(request.getAddress().getCountry());
			}});

		List<EpicPatientMatch> matches = getEpicPatientMatcher().match(matchRequest);

		List<String> dryRunMatches = new ArrayList<>(matches.size());

		for (EpicPatientMatch match : matches) {
			String patientId = epicClient.extractUIDFromPatientEntry(match.getPatient()).get();
			String patientName = match.getPatient().getResource().getName().get(0).getGiven().stream().collect(Collectors.joining(" "))
					+ " " + match.getPatient().getResource().getName().get(0).getFamily().stream().collect(Collectors.joining(" "));

			dryRunMatches.add(format("UID %s (%s): score %d (%s) [%s]", patientId, patientName, match.getScore(), match.getMatch() ? "MATCH" : "NO MATCH", match.getMatchRules().stream()
					.sorted((matchRule1, matchRule2) -> matchRule2.getWeight().compareTo(matchRule1.getWeight()))
					.map(matchRule -> format("%s (%s)", matchRule.name(), matchRule.getWeight()))
					.collect(Collectors.joining(", "))));
		}

		Account originalAccount = getCurrentContext().getAccount().get();

		if (!dryRun) {
			if (matches.size() > 0 && matches.get(0).getMatch()) {
				EpicPatientMatch match = matches.get(0);
				String uid = getEpicClient().extractUIDFromPatientEntry(match.getPatient()).get();
				getAccountService().updateAccountEpicPatient(originalAccount.getAccountId(), uid, "UID", false);
			} else {
				// This is the minimal set of fields required to create a patient
				PatientCreateRequest.Name name = new PatientCreateRequest.Name();
				name.setFirst(request.getFirstName());
				name.setLastName(request.getLastName());
				name.setMiddle(request.getMiddleInitial());

				PatientCreateRequest.Address address = new PatientCreateRequest.Address();

				if (request.getAddress() != null) {
					address.setStreet(request.getAddress().getLine1());
					address.setCity(request.getAddress().getCity());
					address.setState(request.getAddress().getState());
					address.setZipCode(request.getAddress().getPostalCode());
				}

				address.setEmail(request.getEmailAddress());

				if (request.getPhoneNumber() != null && request.getPhoneNumber().trim().length() > 0) {
					PatientCreateRequest.Phone phone = new PatientCreateRequest.Phone();
					phone.setNumber(epicClient.formatPhoneNumber(request.getPhoneNumber()));
					phone.setType(request.getPhoneType()); // alternatively, HOME PHONE will work

					address.setPhones(Collections.singletonList(phone));
				}

				PatientCreateRequest createRequest = new PatientCreateRequest();
				createRequest.setName(name);
				createRequest.setDepartmentID("603"); // This is the provider's department ID
				createRequest.setDepartmentIDType("External");
				createRequest.setGender(request.getGender().name());
				createRequest.setNationalIdentifier(request.getNationalIdentifier());
				createRequest.setDateOfBirth(epicClient.formatDateWithHyphens(request.getDateOfBirth()));
				createRequest.setAddress(address);

				PatientCreateResponse createResponse = epicClient.performPatientCreate(createRequest);
				String hupMrnId = null;

				for (PatientCreateResponse.PatientID patientID : createResponse.getPatientIDs()) {
					if ("HUP MRN".equals(patientID.getType())) {
						hupMrnId = patientID.getID();
						break;
					}
				}

				// Exchange HUP MRN for UID
				String uid = epicClient.determineLatestUIDForPatientIdentifier(hupMrnId, "HUP MRN").get();

				getAccountService().updateAccountEpicPatient(originalAccount.getAccountId(), uid, "UID", true);
			}
		}

		Account updatedAccount = getAccountService().findAccountById(originalAccount.getAccountId()).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("dryRun", dryRun);
			put("dryRunMatches", dryRunMatches);
			put("account", getAccountApiResponseFactory().create(updatedAccount));
		}});
	}

	@NotThreadSafe
	public static class EpicTestProviderRequest {
		@Nullable
		private LocalDate dateOfBirth;
		@Nullable
		private String emailAddress;
		@Nullable
		private String firstName;
		@Nullable
		private String lastName;
		@Nullable
		private String middleInitial;
		@Nullable
		private String nationalIdentifier;
		@Nullable
		private String phoneNumber;
		@Nullable
		private String phoneType;
		@Nullable
		private Boolean dryRun;
		@Nullable
		private Address address;
		@Nullable
		private Gender gender;

		public enum Gender {
			MALE, FEMALE
		}

		@NotThreadSafe
		public static class Address {
			@Nullable
			private String line1;
			@Nullable
			private String city;
			@Nullable
			private String state;
			@Nullable
			private String postalCode;
			@Nullable
			private String country;

			@Nullable
			public String getLine1() {
				return line1;
			}

			public void setLine1(@Nullable String line1) {
				this.line1 = line1;
			}

			@Nullable
			public String getCity() {
				return city;
			}

			public void setCity(@Nullable String city) {
				this.city = city;
			}

			@Nullable
			public String getState() {
				return state;
			}

			public void setState(@Nullable String state) {
				this.state = state;
			}

			@Nullable
			public String getPostalCode() {
				return postalCode;
			}

			public void setPostalCode(@Nullable String postalCode) {
				this.postalCode = postalCode;
			}

			@Nullable
			public String getCountry() {
				return country;
			}

			public void setCountry(@Nullable String country) {
				this.country = country;
			}
		}

		@Nullable
		public LocalDate getDateOfBirth() {
			return dateOfBirth;
		}

		public void setDateOfBirth(@Nullable LocalDate dateOfBirth) {
			this.dateOfBirth = dateOfBirth;
		}

		@Nullable
		public String getEmailAddress() {
			return emailAddress;
		}

		public void setEmailAddress(@Nullable String emailAddress) {
			this.emailAddress = emailAddress;
		}

		@Nullable
		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(@Nullable String firstName) {
			this.firstName = firstName;
		}

		@Nullable
		public String getLastName() {
			return lastName;
		}

		public void setLastName(@Nullable String lastName) {
			this.lastName = lastName;
		}

		@Nullable
		public String getMiddleInitial() {
			return middleInitial;
		}

		public void setMiddleInitial(@Nullable String middleInitial) {
			this.middleInitial = middleInitial;
		}

		@Nullable
		public String getNationalIdentifier() {
			return nationalIdentifier;
		}

		public void setNationalIdentifier(@Nullable String nationalIdentifier) {
			this.nationalIdentifier = nationalIdentifier;
		}

		@Nullable
		public String getPhoneNumber() {
			return phoneNumber;
		}

		public void setPhoneNumber(@Nullable String phoneNumber) {
			this.phoneNumber = phoneNumber;
		}

		@Nullable
		public String getPhoneType() {
			return phoneType;
		}

		public void setPhoneType(@Nullable String phoneType) {
			this.phoneType = phoneType;
		}

		@Nullable
		public Boolean getDryRun() {
			return dryRun;
		}

		public void setDryRun(@Nullable Boolean dryRun) {
			this.dryRun = dryRun;
		}

		@Nullable
		public Address getAddress() {
			return address;
		}

		public void setAddress(@Nullable Address address) {
			this.address = address;
		}

		@Nullable
		public Gender getGender() {
			return gender;
		}

		public void setGender(@Nullable Gender gender) {
			this.gender = gender;
		}

	}

	@Nonnull
	protected EpicClient getEpicClient() {
		return epicClient;
	}

	@Nonnull
	protected EpicPatientMatcher getEpicPatientMatcher() {
		return epicPatientMatcher;
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return requestBodyParser;
	}

	@Nonnull
	protected Database getDatabase() {
		return database;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return formatter;
	}

	@Nonnull
	protected Normalizer getNormalizer() {
		return normalizer;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected AccountService getAccountService() {
		return accountService;
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return providerService;
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return appointmentService;
	}

	@Nonnull
	protected AuditLogService getAuditLogService() {
		return auditLogService;
	}

	@Nonnull
	protected AccountApiResponseFactory getAccountApiResponseFactory() {
		return accountApiResponseFactory;
	}

	@Nonnull
	protected EmailMessageManager getEmailMessageManager() {
		return emailMessageManager;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return jsonMapper;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}
}