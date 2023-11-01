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
import com.cobaltplatform.api.model.api.request.UpdateCheckInAction;
import com.cobaltplatform.api.model.api.response.AccountCheckInApiResponse.AccountCheckInApiResponseFactory;
import com.cobaltplatform.api.model.api.response.StudyAccountApiResponse.StudyAccountApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.CheckInStatusGroup.CheckInStatusGroupId;
import com.cobaltplatform.api.model.db.Study;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.StudyService;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PUT;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
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
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final AccountCheckInApiResponseFactory accountCheckInApiResponseFactory;

	@Nonnull
	private final StudyAccountApiResponseFactory studyAccountApiResponseFactory;
	@Nonnull
	private final RequestBodyParser requestBodyParser;

	@Inject
	public StudyResource(@Nonnull StudyService studyService,
											 @Nonnull Provider<CurrentContext> currentContextProvider,
											 @Nonnull AccountCheckInApiResponseFactory accountCheckInApiResponseFactory,
											 @Nonnull StudyAccountApiResponseFactory studyAccountApiResponseFactory,
											 @Nonnull RequestBodyParser requestBodyParser) {
		requireNonNull(studyService);
		requireNonNull(currentContextProvider);
		requireNonNull(accountCheckInApiResponseFactory);
		requireNonNull(requestBodyParser);
		requireNonNull(studyAccountApiResponseFactory);

		this.studyService = studyService;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
		this.accountCheckInApiResponseFactory = accountCheckInApiResponseFactory;
		this.requestBodyParser = requestBodyParser;
		this.studyAccountApiResponseFactory = studyAccountApiResponseFactory;
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
	@GET("/studies/{studyId}/beiwe-config")
	@AuthenticationRequired
	public ApiResponse beiweConfig(@Nonnull @PathParameter UUID studyId) {
		requireNonNull(studyId);

		Account account = getCurrentContext().getAccount().get();
		Study study = getStudyService().findStudyById(studyId).orElse(null);

		if (study == null)
			throw new NotFoundException();

		// TODO: authorization check

		// See format at https://github.com/onnela-lab/beiwe-backend/blob/main/api/mobile_api.py
		//
		//     return_obj = {
		//        'client_public_key': get_client_public_key_string(patient_id, participant.study.object_id),
		//        'device_settings': participant.study.device_settings.export(),
		//        'ios_plist': firebase_plist_data,
		//        'android_firebase_json': firebase_json_data,
		//        'study_name': participant.study.name,
		//        'study_id': participant.study.object_id,
		//    }
		//

		String clientPublicKey = "";

		// Device Settings model is available at https://github.com/onnela-lab/beiwe-backend/blob/main/database/study_models.py
		Map<String, Object> deviceSettings = new HashMap<>();

		//     # set up FCM files
		//    firebase_plist_data = None
		//    firebase_json_data = None
		//    if participant.os_type == 'IOS':
		//        ios_credentials = FileAsText.objects.filter(tag=IOS_FIREBASE_CREDENTIALS).first()
		//        if ios_credentials:
		//            firebase_plist_data = plistlib.loads(ios_credentials.text.encode())
		//    elif participant.os_type == 'ANDROID':
		//        android_credentials = FileAsText.objects.filter(tag=ANDROID_FIREBASE_CREDENTIALS).first()
		//        if android_credentials:
		//            firebase_json_data = json.loads(android_credentials.text)

		// TODO: implement
		Map<String, Object> iosPlist = new HashMap<>();
		Map<String, Object> androidFirebaseJson = new HashMap<>();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("client_public_key", clientPublicKey);
			put("device_settings", deviceSettings);
			put("ios_plist", iosPlist);
			put("android_firebase_json", androidFirebaseJson);
			put("study_name", study.getName());
			put("study_id", study.getStudyId());
		}});
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
	protected AccountCheckInApiResponseFactory getAccountCheckInApiResponseFactory() {
		return this.accountCheckInApiResponseFactory;
	}

	@Nonnull
	public RequestBodyParser getRequestBodyParser() {
		return requestBodyParser;
	}

	@Nonnull
	protected StudyAccountApiResponseFactory getStudyAccountApiResponseFactory() {
		return studyAccountApiResponseFactory;
	}
}
