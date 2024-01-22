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
import com.cobaltplatform.api.model.api.request.CreateStudyFileUploadRequest;
import com.cobaltplatform.api.model.api.request.UpdateCheckInAction;
import com.cobaltplatform.api.model.api.response.AccountCheckInApiResponse.AccountCheckInApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FileUploadResultApiResponse.FileUploadResultApiResponseFactory;
import com.cobaltplatform.api.model.api.response.StudyAccountApiResponse.StudyAccountApiResponseFactory;
import com.cobaltplatform.api.model.api.response.StudyApiResponse.StudyApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountCheckInAction;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.AccountStudy;
import com.cobaltplatform.api.model.db.CheckInStatusGroup.CheckInStatusGroupId;
import com.cobaltplatform.api.model.db.EncryptionKeypair;
import com.cobaltplatform.api.model.db.Study;
import com.cobaltplatform.api.model.db.StudyBeiweConfig;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.FileUploadResult;
import com.cobaltplatform.api.service.StudyService;
import com.cobaltplatform.api.service.SystemService;
import com.cobaltplatform.api.web.request.RequestBodyParser;
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
import java.util.HashMap;
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

	@Inject
	public StudyResource(@Nonnull StudyService studyService,
											 @Nonnull SystemService systemService,
											 @Nonnull Provider<CurrentContext> currentContextProvider,
											 @Nonnull AccountCheckInApiResponseFactory accountCheckInApiResponseFactory,
											 @Nonnull StudyAccountApiResponseFactory studyAccountApiResponseFactory,
											 @Nonnull FileUploadResultApiResponseFactory fileUploadResultApiResponseFactory,
											 @Nonnull StudyApiResponseFactory studyApiResponseFactory,
											 @Nonnull RequestBodyParser requestBodyParser) {
		requireNonNull(studyService);
		requireNonNull(systemService);
		requireNonNull(currentContextProvider);
		requireNonNull(accountCheckInApiResponseFactory);
		requireNonNull(requestBodyParser);
		requireNonNull(studyAccountApiResponseFactory);
		requireNonNull(fileUploadResultApiResponseFactory);
		requireNonNull(studyApiResponseFactory);

		this.studyService = studyService;
		this.systemService = systemService;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
		this.accountCheckInApiResponseFactory = accountCheckInApiResponseFactory;
		this.requestBodyParser = requestBodyParser;
		this.studyAccountApiResponseFactory = studyAccountApiResponseFactory;
		this.fileUploadResultApiResponseFactory = fileUploadResultApiResponseFactory;
		this.studyApiResponseFactory = studyApiResponseFactory;
	}

	@Nonnull
	@POST("/studies/{studyId}/generate-accounts")
	@AuthenticationRequired
	public ApiResponse addCurrentAccountToStudy(@Nonnull @PathParameter UUID studyId,
																							@Nonnull @QueryParameter Optional<Integer> count) {
		requireNonNull(studyId);
		requireNonNull(count);

		Account account = getCurrentContext().getAccount().get();
		int finalCount = count.orElse(10);
		return new ApiResponse(new HashMap<String, Object>() {{
			put("accounts", getStudyService().generateAccountsForStudy(studyId, finalCount, account).stream().map(studyAccount ->
					getStudyAccountApiResponseFactory().create(studyAccount)).collect(Collectors.toList()));
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

		Study study = getStudyService().findStudyByIdentifier(studyIdentifier, getCurrentContext().getInstitutionId()).orElse(null);

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
		Study study = getStudyService().findStudyByIdentifier(studyIdentifier, getCurrentContext().getInstitutionId()).orElse(null);

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

		Study study = getStudyService().findStudyByIdentifier(studyIdentifier, getCurrentContext().getInstitutionId()).orElse(null);

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
		Study study = getStudyService().findStudyByIdentifier(studyIdentifier, getCurrentContext().getInstitutionId()).orElse(null);

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
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
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
}
