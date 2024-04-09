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

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.request.CreateAccountCheckInActionFileUploadRequest;
import com.cobaltplatform.api.model.api.request.CreateStudyAccountRequest;
import com.cobaltplatform.api.model.api.request.CreateStudyFileUploadRequest;
import com.cobaltplatform.api.model.api.request.UpdateCheckInAction;
import com.cobaltplatform.api.model.api.response.AccountCheckInApiResponse.AccountCheckInApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FileUploadResultApiResponse.FileUploadResultApiResponseFactory;
import com.cobaltplatform.api.model.api.response.StudyAccountApiResponse.StudyAccountApiResponseFactory;
import com.cobaltplatform.api.model.api.response.StudyApiResponse.StudyApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountCheckInAction;
import com.cobaltplatform.api.model.db.AccountCheckInActionFileUpload;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.AccountStudy;
import com.cobaltplatform.api.model.db.CheckInStatusGroup.CheckInStatusGroupId;
import com.cobaltplatform.api.model.db.EncryptionKeypair;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.Study;
import com.cobaltplatform.api.model.db.StudyBeiweConfig;
import com.cobaltplatform.api.model.db.StudyFileUpload;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.FileUploadResult;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.StudyService;
import com.cobaltplatform.api.service.SystemService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.lokalized.Strings;
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
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class StudyResource {
	@Nonnull
	private final AccountService accountService;
	@Nonnull
	private final StudyService studyService;
	@Nonnull
	private final SystemService systemService;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final AccountCheckInApiResponseFactory accountCheckInApiResponseFactory;
	@Nonnull
	private final StudyAccountApiResponseFactory studyAccountApiResponseFactory;
	@Nonnull
	private final FileUploadResultApiResponseFactory fileUploadResultApiResponseFactory;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final StudyApiResponseFactory studyApiResponseFactory;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Formatter formatter;

	@Inject
	public StudyResource(@Nonnull AccountService accountService,
											 @Nonnull StudyService studyService,
											 @Nonnull SystemService systemService,
											 @Nonnull Provider<CurrentContext> currentContextProvider,
											 @Nonnull AccountCheckInApiResponseFactory accountCheckInApiResponseFactory,
											 @Nonnull StudyAccountApiResponseFactory studyAccountApiResponseFactory,
											 @Nonnull FileUploadResultApiResponseFactory fileUploadResultApiResponseFactory,
											 @Nonnull StudyApiResponseFactory studyApiResponseFactory,
											 @Nonnull RequestBodyParser requestBodyParser,
											 @Nonnull Strings strings,
											 @Nonnull Formatter formatter) {
		requireNonNull(accountService);
		requireNonNull(studyService);
		requireNonNull(systemService);
		requireNonNull(currentContextProvider);
		requireNonNull(accountCheckInApiResponseFactory);
		requireNonNull(requestBodyParser);
		requireNonNull(studyAccountApiResponseFactory);
		requireNonNull(fileUploadResultApiResponseFactory);
		requireNonNull(studyApiResponseFactory);
		requireNonNull(strings);
		requireNonNull(formatter);

		this.accountService = accountService;
		this.studyService = studyService;
		this.systemService = systemService;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
		this.accountCheckInApiResponseFactory = accountCheckInApiResponseFactory;
		this.requestBodyParser = requestBodyParser;
		this.studyAccountApiResponseFactory = studyAccountApiResponseFactory;
		this.fileUploadResultApiResponseFactory = fileUploadResultApiResponseFactory;
		this.studyApiResponseFactory = studyApiResponseFactory;
		this.strings = strings;
		this.formatter = formatter;
	}

	@Nonnull
	@POST("/studies/generate-accounts")
	@AuthenticationRequired
	public ApiResponse generateAccounts(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();
		CreateStudyAccountRequest request = getRequestBodyParser().parse(requestBody, CreateStudyAccountRequest.class);
		return new ApiResponse(new HashMap<String, Object>() {{
			put("accounts", getStudyService().generateAccountsForStudies(request, account).stream().map(studyAccount ->
					getStudyAccountApiResponseFactory().create(studyAccount)).collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@POST("/studies/{studyId}/add-account/{accountId}")
	@AuthenticationRequired
	public ApiResponse addAccountToStudy(@Nonnull @PathParameter UUID studyId,
																			 @Nonnull @PathParameter UUID accountId) {
		requireNonNull(studyId);
		requireNonNull(accountId);

		Account account = getCurrentContext().getAccount().get();
		if (account.getRoleId() != RoleId.ADMINISTRATOR) {
			throw new AuthorizationException();
		}

		getStudyService().addAccountToStudy(accountId, studyId);
		return new ApiResponse(new HashMap<String, Object>() {{
			put("studies", getStudyService().findStudiesForAccountId(accountId)
					.stream().map(accountStudies -> getStudyApiResponseFactory().create(accountStudies)).collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/studies/{studyId}/check-in-list")
	@AuthenticationRequired
	public ApiResponse getAccountCheckInForStudy(@Nonnull @PathParameter UUID studyId,
																							 @QueryParameter Optional<CheckInStatusGroupId> checkInStatusGroupId) {
		requireNonNull(studyId);
		Account account = getCurrentContext().getAccount().get();

		getStudyService().rescheduleAccountCheckIn(account, studyId);
		return new ApiResponse(new HashMap<String, Object>() {{
			put("checkIns", getStudyService().findAccountCheckInsForAccountAndStudy(account, studyId, checkInStatusGroupId)
					.stream().map(accountCheckIn -> getAccountCheckInApiResponseFactory().create(accountCheckIn)).collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/studies/list")
	@AuthenticationRequired
	public ApiResponse studyList() {
		Account account = getCurrentContext().getAccount().get();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("studies", getStudyService().findStudiesForAccountId(account.getAccountId())
					.stream().map(accountStudies -> getStudyApiResponseFactory().create(accountStudies)).collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@PUT("/studies/{studyId}/check-in-action/{accountCheckInActionId}")
	@AuthenticationRequired
	public ApiResponse updateCheckInActionStatusId(@Nonnull @PathParameter UUID studyId,
																								 @Nonnull @PathParameter UUID accountCheckInActionId,
																								 @Nonnull @RequestBody String requestBody) {
		requireNonNull(studyId);
		requireNonNull(accountCheckInActionId);
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();
		UpdateCheckInAction request = getRequestBodyParser().parse(requestBody, UpdateCheckInAction.class);

		request.setAccountCheckInActionId(accountCheckInActionId);
		getStudyService().updateAccountCheckInAction(account, request);
		getStudyService().rescheduleAccountCheckIn(account, studyId);
		return new ApiResponse(new HashMap<String, Object>() {{
			put("checkIns", getStudyService().findAccountCheckInsForAccountAndStudy(account, studyId, Optional.of(CheckInStatusGroupId.TO_DO))
					.stream().map(accountCheckIn -> getAccountCheckInApiResponseFactory().create(accountCheckIn)).collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/studies/{studyIdentifier}")
	public ApiResponse study(@Nonnull @PathParameter String studyIdentifier) {
		requireNonNull(studyIdentifier);

		Study study = getStudyService().findStudyByIdentifier(studyIdentifier).orElse(null);

		if (study == null)
			throw new NotFoundException();

		// Note: currently this endpoint is publicly accessible. This is by design.
		// There is no "secret" information in the Study type, and it's necessary
		// for clients to have access to coordinator contact information for help with sign-in etc.

		return new ApiResponse(Map.of("study", getStudyApiResponseFactory().create(study)));
	}

	@Nonnull
	@GET("/studies/{studyIdentifier}/beiwe-config")
	@AuthenticationRequired
	public ApiResponse beiweConfig(@Nonnull @PathParameter String studyIdentifier) {
		requireNonNull(studyIdentifier);

		Account account = getCurrentContext().getAccount().get();
		Study study = getStudyService().findStudyByIdentifier(studyIdentifier).orElse(null);

		if (study == null)
			throw new NotFoundException();

		AccountStudy accountStudy = getStudyService().findAccountStudyByAccountIdAndStudyId(account.getAccountId(), study.getStudyId()).orElse(null);

		if (accountStudy == null)
			throw new AuthorizationException();

		EncryptionKeypair encryptionKeypair = getSystemService().findEncryptionKeypairById(accountStudy.getEncryptionKeypairId()).get();
		String clientPublicKey = encryptionKeypair.getPublicKeyAsString();

		// It's programmer error if this does not exist for a study.
		// Clients will only call this endpoint if they are using Beiwe
		StudyBeiweConfig studyBeiweConfig = getStudyService().findStudyBeiweConfigByStudyId(study.getStudyId()).get();
		Map<String, Object> deviceSettings = studyBeiweConfig.toDeviceSettingsRepresentation();

		// See format at https://github.com/onnela-lab/beiwe-backend/blob/main/api/mobile_api.py
		return new ApiResponse(new HashMap<String, Object>() {{
			put("client_public_key", clientPublicKey);
			put("device_settings", deviceSettings);
			put("ios_plist", Map.of() /* not currently used */);
			put("android_firebase_json", Map.of() /* not currently used */);
			put("study_name", study.getName());
			put("study_id", study.getStudyId());
		}});
	}

	@Nonnull
	@GET("/studies/{studyIdentifier}/onboarding")
	public ApiResponse studyOnboarding(@Nonnull @PathParameter String studyIdentifier) {
		requireNonNull(studyIdentifier);

		Study study = getStudyService().findStudyByIdentifier(studyIdentifier).orElse(null);

		if (study == null)
			throw new NotFoundException();

		Set<AccountSourceId> permittedAcountSourceIds = getStudyService().findPermittedAccountSourceIdsByStudyId(study.getStudyId());

		return new ApiResponse(new HashMap<>() {{
			put("onboardingDestinationUrl", study.getOnboardingDestinationUrl());
			put("permittedAccountSourceIds", permittedAcountSourceIds);
		}});
	}

	@Nonnull
	@POST("/studies/{studyIdentifier}/file-upload")
	@AuthenticationRequired
	public ApiResponse createStudyFileUpload(@Nonnull @PathParameter String studyIdentifier,
																					 @Nonnull @RequestBody String requestBody) {
		requireNonNull(studyIdentifier);
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();
		Study study = getStudyService().findStudyByIdentifier(studyIdentifier).orElse(null);

		if (study == null)
			throw new NotFoundException();

		AccountStudy accountStudy = getStudyService().findAccountStudyByAccountIdAndStudyId(account.getAccountId(), study.getStudyId()).orElse(null);

		if (accountStudy == null)
			throw new AuthorizationException();

		CreateStudyFileUploadRequest request = getRequestBodyParser().parse(requestBody, CreateStudyFileUploadRequest.class);
		request.setAccountId(account.getAccountId());
		request.setStudyId(study.getStudyId());

		FileUploadResult fileUploadResult = getStudyService().createStudyFileUpload(request);

		return new ApiResponse(Map.of(
				"fileUploadResult", getFileUploadResultApiResponseFactory().create(fileUploadResult)
		));
	}

	@Nonnull
	@POST("/account-check-in-actions/{accountCheckInActionId}/file-upload")
	@AuthenticationRequired
	public ApiResponse createAccountCheckInActionFileUpload(@Nonnull @PathParameter UUID accountCheckInActionId,
																													@Nonnull @RequestBody String requestBody) {
		requireNonNull(accountCheckInActionId);
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();
		AccountCheckInAction accountCheckInAction = getStudyService().findAccountCheckInActionById(accountCheckInActionId).orElse(null);

		if (accountCheckInAction == null)
			throw new NotFoundException();

		Study study = getStudyService().findStudyByStudyCheckInActionId(accountCheckInAction.getStudyCheckInActionId()).get();

		AccountStudy accountStudy = getStudyService().findAccountStudyByAccountIdAndStudyId(account.getAccountId(), study.getStudyId()).orElse(null);

		if (accountStudy == null)
			throw new AuthorizationException();

		CreateAccountCheckInActionFileUploadRequest request = getRequestBodyParser().parse(requestBody, CreateAccountCheckInActionFileUploadRequest.class);
		request.setAccountId(account.getAccountId());
		request.setAccountCheckInActionId(accountCheckInActionId);

		FileUploadResult fileUploadResult = getStudyService().createAccountCheckInActionFileUpload(request);

		return new ApiResponse(Map.of(
				"fileUploadResult", getFileUploadResultApiResponseFactory().create(fileUploadResult)
		));
	}

	@Nonnull
	@GET("/studies/{studyId}/file-uploads")
	@AuthenticationRequired
	public ApiResponse studyFileUploads(@Nonnull @PathParameter UUID studyId,
																			@Nonnull @QueryParameter("accountId") Optional<UUID> providedAccountId,
																			@Nonnull @QueryParameter("username") Optional<String> providedUsername) {
		requireNonNull(studyId);
		requireNonNull(providedAccountId);
		requireNonNull(providedUsername);

		Account account = getCurrentContext().getAccount().get();
		UUID accountId = account.getAccountId();

		// Administrators can override by providing an "accountId" query parameter
		if (providedAccountId.isPresent() || providedUsername.isPresent()) {
			if (account.getRoleId() != RoleId.ADMINISTRATOR)
				throw new AuthorizationException();

			if (providedAccountId.isPresent() && providedUsername.isPresent())
				throw new ValidationException("You cannot provide both 'accountId' and 'username' query parameters.");

			if (providedAccountId.isPresent()) {
				Account accountForAccountId = getAccountService().findAccountById(providedAccountId.get()).orElse(null);

				if (accountForAccountId == null)
					throw new ValidationException("Account ID is invalid.");

				if (!account.getInstitutionId().equals(accountForAccountId.getInstitutionId()))
					throw new AuthorizationException();

				accountId = accountForAccountId.getAccountId();
			} else if (providedUsername.isPresent()) {
				Account accountForUsername = getAccountService().findAccountByUsernameAndAccountSourceId(providedUsername.get(), AccountSourceId.USERNAME, account.getInstitutionId()).orElse(null);

				if (accountForUsername == null)
					throw new ValidationException("Account username is invalid.");

				accountId = accountForUsername.getAccountId();
			} else {
				throw new IllegalStateException();
			}
		}

		List<Study> studies = getStudyService().findStudiesForAccountId(accountId);
		List<Map<String, Object>> studiesJson = new ArrayList<>(studies.size());

		for (Study study : studies) {
			AccountStudy accountStudy = getStudyService().findAccountStudyByAccountIdAndStudyId(accountId, study.getStudyId()).get();
			EncryptionKeypair encryptionKeypair = getSystemService().findEncryptionKeypairById(accountStudy.getEncryptionKeypairId()).get();
			List<StudyFileUpload> studyFileUploads = getStudyService().findStudyFileUploadsByAccountStudyId(accountStudy.getAccountStudyId());
			List<AccountCheckInActionFileUpload> accountCheckInActionFileUploads = getStudyService().findAccountCheckInActionFileUploadsByAccountStudyId(accountStudy.getAccountStudyId());

			Map<String, Object> encryptionKeypairJson = new LinkedHashMap<>();
			encryptionKeypairJson.put("encryptionKeypairId", encryptionKeypair.getEncryptionKeypairId());
			encryptionKeypairJson.put("publicKeyFormatId", encryptionKeypair.getPublicKeyFormatId());
			encryptionKeypairJson.put("publicKey", encryptionKeypair.getPublicKeyAsString());

			Map<String, Object> accountStudyJson = new LinkedHashMap<>();
			accountStudyJson.put("accountId", accountStudy.getAccountId());
			accountStudyJson.put("username", account.getUsername());
			accountStudyJson.put("timeZone", accountStudy.getTimeZone());
			accountStudyJson.put("encryptionKeypair", encryptionKeypairJson);

			List<Map<String, Object>> studyFileUploadsJson = new ArrayList<>();

			for (StudyFileUpload studyFileUpload : studyFileUploads) {
				Map<String, Object> studyFileUploadJson = new LinkedHashMap<>();
				studyFileUploadJson.put("fileUploadId", studyFileUpload.getFileUploadId());
				studyFileUploadJson.put("fileUploadTypeId", studyFileUpload.getFileUploadTypeId());
				studyFileUploadJson.put("fileUploadFilename", studyFileUpload.getFileUploadFilename());
				studyFileUploadJson.put("fileUploadContentType", studyFileUpload.getFileUploadContentType());

				if (studyFileUpload.getFileUploadFilesize() != null) {
					studyFileUploadJson.put("fileUploadFilesize", studyFileUpload.getFileUploadFilesize());
					studyFileUploadJson.put("fileUploadFilesizeDescription", getFormatter().formatFilesize(studyFileUpload.getFileUploadFilesize()));
				}

				studyFileUploadJson.put("fileUploadUrl", studyFileUpload.getFileUploadUrl());
				studyFileUploadJson.put("fileUploadCreated", studyFileUpload.getFileUploadCreated());
				studyFileUploadJson.put("fileUploadCreatedDescription", getFormatter().formatTimestamp(studyFileUpload.getFileUploadCreated(), FormatStyle.MEDIUM, FormatStyle.SHORT, accountStudy.getTimeZone()));

				studyFileUploadsJson.add(studyFileUploadJson);
			}

			List<Map<String, Object>> accountCheckInActionFileUploadsJson = new ArrayList<>();

			for (AccountCheckInActionFileUpload accountCheckInActionFileUpload : accountCheckInActionFileUploads) {
				Map<String, Object> accountCheckInActionFileUploadJson = new LinkedHashMap<>();
				accountCheckInActionFileUploadJson.put("studyCheckInId", accountCheckInActionFileUpload.getStudyCheckInId());
				accountCheckInActionFileUploadJson.put("studyCheckInNumber", accountCheckInActionFileUpload.getStudyCheckInNumber());
				accountCheckInActionFileUploadJson.put("studyCheckInActionId", accountCheckInActionFileUpload.getStudyCheckInActionId());
				accountCheckInActionFileUploadJson.put("accountCheckInId", accountCheckInActionFileUpload.getAccountCheckInId());
				accountCheckInActionFileUploadJson.put("accountCheckInStatusId", accountCheckInActionFileUpload.getAccountCheckInStatusId());
				accountCheckInActionFileUploadJson.put("accountCheckInStartDateTime", accountCheckInActionFileUpload.getAccountCheckInStartDateTime());
				accountCheckInActionFileUploadJson.put("accountCheckInStartDateTimeDescription", getFormatter().formatTimestamp(accountCheckInActionFileUpload.getAccountCheckInStartDateTime(), FormatStyle.MEDIUM, FormatStyle.SHORT, accountStudy.getTimeZone()));
				accountCheckInActionFileUploadJson.put("accountCheckInEndDateTime", accountCheckInActionFileUpload.getAccountCheckInEndDateTime());
				accountCheckInActionFileUploadJson.put("accountCheckInEndDateTimeDescription", getFormatter().formatTimestamp(accountCheckInActionFileUpload.getAccountCheckInEndDateTime(), FormatStyle.MEDIUM, FormatStyle.SHORT, accountStudy.getTimeZone()));

				if (accountCheckInActionFileUpload.getAccountCheckInCompletedDate() != null) {
					accountCheckInActionFileUploadJson.put("accountCheckInCompletedDate", accountCheckInActionFileUpload.getAccountCheckInCompletedDate());
					accountCheckInActionFileUploadJson.put("accountCheckInCompletedDateDescription", getFormatter().formatTimestamp(accountCheckInActionFileUpload.getAccountCheckInCompletedDate(), FormatStyle.MEDIUM, FormatStyle.SHORT, accountStudy.getTimeZone()));
				}

				accountCheckInActionFileUploadJson.put("accountCheckInActionId", accountCheckInActionFileUpload.getAccountCheckInActionId());
				accountCheckInActionFileUploadJson.put("accountCheckInActionStatusId", accountCheckInActionFileUpload.getAccountCheckInActionStatusId());
				accountCheckInActionFileUploadJson.put("accountCheckInActionCreated", accountCheckInActionFileUpload.getAccountCheckInActionCreated());
				accountCheckInActionFileUploadJson.put("accountCheckInActionCreated", getFormatter().formatTimestamp(accountCheckInActionFileUpload.getAccountCheckInActionCreated(), FormatStyle.MEDIUM, FormatStyle.SHORT, accountStudy.getTimeZone()));
				accountCheckInActionFileUploadJson.put("accountCheckInActionLastUpdated", accountCheckInActionFileUpload.getAccountCheckInActionLastUpdated());
				accountCheckInActionFileUploadJson.put("accountCheckInActionLastUpdated", getFormatter().formatTimestamp(accountCheckInActionFileUpload.getAccountCheckInActionLastUpdated(), FormatStyle.MEDIUM, FormatStyle.SHORT, accountStudy.getTimeZone()));

				accountCheckInActionFileUploadJson.put("fileUploadId", accountCheckInActionFileUpload.getFileUploadId());
				accountCheckInActionFileUploadJson.put("fileUploadTypeId", accountCheckInActionFileUpload.getFileUploadTypeId());
				accountCheckInActionFileUploadJson.put("fileUploadFilename", accountCheckInActionFileUpload.getFileUploadFilename());
				accountCheckInActionFileUploadJson.put("fileUploadContentType", accountCheckInActionFileUpload.getFileUploadContentType());

				if (accountCheckInActionFileUpload.getFileUploadFilesize() != null) {
					accountCheckInActionFileUploadJson.put("fileUploadFilesize", accountCheckInActionFileUpload.getFileUploadFilesize());
					accountCheckInActionFileUploadJson.put("fileUploadFilesizeDescription", getFormatter().formatFilesize(accountCheckInActionFileUpload.getFileUploadFilesize()));
				}

				accountCheckInActionFileUploadJson.put("fileUploadUrl", accountCheckInActionFileUpload.getFileUploadUrl());
				accountCheckInActionFileUploadJson.put("fileUploadCreated", accountCheckInActionFileUpload.getFileUploadCreated());
				accountCheckInActionFileUploadJson.put("fileUploadCreatedDescription", getFormatter().formatTimestamp(accountCheckInActionFileUpload.getFileUploadCreated(), FormatStyle.MEDIUM, FormatStyle.SHORT, accountStudy.getTimeZone()));

				accountCheckInActionFileUploadsJson.add(accountCheckInActionFileUploadJson);
			}

			Map<String, Object> studyJson = new LinkedHashMap<>();
			studyJson.put("studyId", study.getStudyId());
			studyJson.put("urlName", study.getUrlName());
			studyJson.put("name", study.getName());
			studyJson.put("accountStudy", accountStudyJson);
			studyJson.put("studyFileUploads", studyFileUploadsJson);
			studyJson.put("accountCheckInActionFileUploads", accountCheckInActionFileUploadsJson);

			studiesJson.add(studyJson);
		}

		return new ApiResponse(Map.of(
				"studies", studiesJson
		));
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountService;
	}

	@Nonnull
	protected StudyService getStudyService() {
		return this.studyService;
	}

	@Nonnull
	protected SystemService getSystemService() {
		return this.systemService;
	}

	@Nonnull
	protected AccountCheckInApiResponseFactory getAccountCheckInApiResponseFactory() {
		return this.accountCheckInApiResponseFactory;
	}

	@Nonnull
	public RequestBodyParser getRequestBodyParser() {
		return this.requestBodyParser;
	}

	@Nonnull
	protected StudyAccountApiResponseFactory getStudyAccountApiResponseFactory() {
		return this.studyAccountApiResponseFactory;
	}

	@Nonnull
	protected FileUploadResultApiResponseFactory getFileUploadResultApiResponseFactory() {
		return this.fileUploadResultApiResponseFactory;
	}

	@Nonnull
	protected StudyApiResponseFactory getStudyApiResponseFactory() {
		return this.studyApiResponseFactory;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return this.formatter;
	}
}
