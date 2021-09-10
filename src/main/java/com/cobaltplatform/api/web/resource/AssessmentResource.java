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

import com.lokalized.Strings;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.request.PersonalizeAssessmentChoicesCommand;
import com.cobaltplatform.api.model.api.request.SubmitAssessmentAnswerRequest;
import com.cobaltplatform.api.model.api.response.AssessmentApiResponse.AssessmentQuestionAnswerApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AssessmentFormApiResponse.AssessmentFormApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ProviderApiResponse.ProviderApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSession;
import com.cobaltplatform.api.model.db.assessment.Assessment;
import com.cobaltplatform.api.model.db.assessment.Assessment.AssessmentType;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.AssessmentQuestionAnswers;
import com.cobaltplatform.api.service.AssessmentScoringService;
import com.cobaltplatform.api.service.AssessmentService;
import com.cobaltplatform.api.service.ProviderService;
import com.cobaltplatform.api.service.SessionService;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PUT;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthenticationException;
import com.soklet.web.response.ApiResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class AssessmentResource {
	@Nonnull
	private final SessionService sessionService;
	@Nonnull
	private final AssessmentService assessmentService;
	@Nonnull
	private final AssessmentScoringService assessmentScoringService;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private javax.inject.Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final AssessmentQuestionAnswerApiResponseFactory assessmentQuestionAnswerApiResponseFactory;
	@Nonnull
	private final AssessmentFormApiResponseFactory assessmentFormApiResponseFactory;
	@Nonnull
	private final ProviderService providerService;
	@Nonnull
	private final ProviderApiResponseFactory providerApiResponseFactory;
	@Nonnull
	private final Strings strings;

	@Inject
	public AssessmentResource(@Nonnull SessionService sessionService,
														@Nonnull AssessmentService assessmentService,
														@Nonnull AssessmentScoringService assessmentScoringService,
														@Nonnull RequestBodyParser requestBodyParser,
														@Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
														@Nonnull AssessmentQuestionAnswerApiResponseFactory assessmentQuestionAnswerApiResponseFactory,
														@Nonnull AssessmentFormApiResponseFactory assessmentFormApiResponseFactory,
														@Nonnull ProviderService providerService,
														@Nonnull ProviderApiResponseFactory providerApiResponseFactory,
														@Nonnull Strings strings) {
		this.sessionService = sessionService;
		this.assessmentService = assessmentService;
		this.assessmentScoringService = assessmentScoringService;
		this.requestBodyParser = requestBodyParser;
		this.currentContextProvider = currentContextProvider;
		this.assessmentQuestionAnswerApiResponseFactory = assessmentQuestionAnswerApiResponseFactory;
		this.assessmentFormApiResponseFactory = assessmentFormApiResponseFactory;
		this.providerService = providerService;
		this.providerApiResponseFactory = providerApiResponseFactory;
		this.strings = strings;
	}

	@AuthenticationRequired
	@GET("/assessment/personalize")
	public ApiResponse getPersonalizedQuestions() {
		Account account = getCurrentContext().getAccount().orElseThrow(AuthenticationException::new);
		Assessment assessment = getAssessmentService().findAssessmentByTypeForUser(AssessmentType.INTRO, account).orElseThrow();
		Optional<AccountSession> accountSession = sessionService.findCurrentAccountSessionForAssessment(account, assessment);

		return new ApiResponse(Map.of(
				"assessment", getAssessmentFormApiResponseFactory().create(assessment, accountSession)
		));
	}

	@AuthenticationRequired
	@POST("/assessment/personalize")
	public ApiResponse submitPersonalizedAnswers(@RequestBody String body) {
		Account account = getCurrentContext().getAccount().orElseThrow(AuthenticationException::new);
		PersonalizeAssessmentChoicesCommand command = requestBodyParser.parse(body, PersonalizeAssessmentChoicesCommand.class);
		UUID sessionId = assessmentService.submitPersonalizeAssessmentAnswers(account, command);
		Assessment assessment = getAssessmentService().findAssessmentByTypeForUser(AssessmentType.INTRO, account).orElseThrow();
		Optional<AccountSession> accountSession = getSessionService().findAccountSessionById(sessionId);

		return new ApiResponse(
				Map.of("assessment", getAssessmentFormApiResponseFactory().create(assessment, accountSession))
		);
	}

	@AuthenticationRequired
	@GET("/assessment/intro")
	public ApiResponse getIntroAssessmentQuestion(@QueryParameter("questionId") Optional<String> questionId,
																								@QueryParameter("sessionId") Optional<String> sessionId) {
		return getAssessmentQuestion(AssessmentType.INTRO, questionId.orElse(null), sessionId.orElse(null), null, null);
	}


	@GET("/assessment/intake")
	public ApiResponse getIntakeAssessmentQuestion(@QueryParameter("questionId") Optional<String> questionId,
																								 @QueryParameter("sessionId") Optional<String> sessionId,
																								 @QueryParameter("providerId") Optional<UUID> providerId,
																								 @QueryParameter("groupSessionId") Optional<UUID> groupSessionId) {
		return getAssessmentQuestion(AssessmentType.INTAKE, questionId.orElse(null), sessionId.orElse(null),
				providerId.orElse(null), groupSessionId.orElse(null));
	}


	@AuthenticationRequired
	@GET("/assessment/evidence")
	public ApiResponse getEvidenceAssessmentQuestion(@QueryParameter("questionId") Optional<String> questionId,
																									 @QueryParameter("sessionId") Optional<String> sessionId) {
		return getAssessmentQuestion(AssessmentType.PHQ4, questionId.orElse(null), sessionId.orElse(null), null, null);
	}

	@AuthenticationRequired
	private ApiResponse getAssessmentQuestion(@Nonnull AssessmentType assessmentType,
																						@Nullable String questionId,
																						@Nullable String sessionId,
																						@Nullable UUID providerId,
																						@Nullable UUID groupSessionId) {
		Account account = getCurrentContext().getAccount().get();
		AssessmentQuestionAnswers assessmentQuestionAnswers = getAssessmentService().getNextAssessmentQuestion(account,
				assessmentType, questionId, sessionId, providerId, groupSessionId);
		return new ApiResponse(new HashMap<String, Object>() {{
			put("assessment", getAssessmentQuestionAnswerApiResponseFactory().create(assessmentQuestionAnswers, account));
		}});
	}

	@AuthenticationRequired
	@PUT("/assessment/intro")
	public ApiResponse submitIntroAssessmentAnswer(@RequestBody String body) {
		SubmitAssessmentAnswerRequest request = getRequestBodyParser().parse(body, SubmitAssessmentAnswerRequest.class);
		return submitAssessmentAnswer(request);
	}

	@AuthenticationRequired
	@PUT("/assessment/evidence")
	public ApiResponse submitEvidenceAssessmentAnswer(@RequestBody String body) {
		SubmitAssessmentAnswerRequest request = getRequestBodyParser().parse(body, SubmitAssessmentAnswerRequest.class);
		return submitAssessmentAnswer(request);
	}

	@AuthenticationRequired
	@PUT("/assessment/intake")
	public ApiResponse submitIntakeAssessmentAnswer(@RequestBody String body) {
		SubmitAssessmentAnswerRequest request = getRequestBodyParser().parse(body, SubmitAssessmentAnswerRequest.class);
		return submitAssessmentAnswer(request);
	}

	@Nonnull
	protected ApiResponse submitAssessmentAnswer(@Nonnull SubmitAssessmentAnswerRequest request) {
		requireNonNull(request);

		Account account = getCurrentContext().getAccount().get();
		AssessmentQuestionAnswers nextQuestion = getAssessmentService().submitAssessmentAnswer(account, request);

		if (nextQuestion == null) {
			AccountSession accountSession = getSessionService().findAccountSessionByIdAndAccount(account,
					UUID.fromString(request.getSessionId())).get();
			Optional<Assessment> assessment = getAssessmentService().findAssessmentById(accountSession.getAssessmentId());

			if (!assessment.isPresent() || assessment.get().getAssessmentTypeId() != AssessmentType.INTAKE)
				return new ApiResponse(new HashMap<String, Object>());
			else {
				Map<String, Object> response = new LinkedHashMap<>();
				response.put("bookingAllowed", getAssessmentScoringService().isBookingAllowed(accountSession));
				return new ApiResponse(new HashMap<String, Object>() {{
					put("assessment", response);
				}});
			}
		} else {
			return new ApiResponse(new HashMap<String, Object>() {{
				put("assessment", getAssessmentQuestionAnswerApiResponseFactory().create(nextQuestion, account));
			}});
		}
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
	protected AssessmentScoringService getAssessmentScoringService() {
		return assessmentScoringService;
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return requestBodyParser;
	}

	@Nonnull
	protected AssessmentQuestionAnswerApiResponseFactory getAssessmentQuestionAnswerApiResponseFactory() {
		return assessmentQuestionAnswerApiResponseFactory;
	}

	@Nonnull
	protected ProviderApiResponseFactory getProviderApiResponseFactory() {
		return providerApiResponseFactory;
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return providerService;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected AssessmentFormApiResponseFactory getAssessmentFormApiResponseFactory() {
		return assessmentFormApiResponseFactory;
	}
}
