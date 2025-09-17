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
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.error.ErrorReporter;
import com.cobaltplatform.api.integration.enterprise.EnterprisePlugin;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.messaging.call.CallMessage;
import com.cobaltplatform.api.messaging.call.CallMessageTemplate;
import com.cobaltplatform.api.model.api.request.ClosePatientOrderRequest;
import com.cobaltplatform.api.model.api.request.CreateGroupSessionReservationRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderScheduledMessageGroupRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderTriageGroupRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderTriageGroupRequest.CreatePatientOrderTriageRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningAnswersRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningAnswersRequest.CreateAnswerRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningSessionRequest;
import com.cobaltplatform.api.model.api.request.SkipScreeningSessionRequest;
import com.cobaltplatform.api.model.api.request.UpdateCheckInAction;
import com.cobaltplatform.api.model.api.request.UpdateCourseSessionUnitCompletionMessageRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderResourcingStatusRequest;
import com.cobaltplatform.api.model.api.response.ScreeningConfirmationPromptApiResponse.ScreeningConfirmationPromptApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountCheckIn;
import com.cobaltplatform.api.model.db.AccountCheckInAction;
import com.cobaltplatform.api.model.db.AccountSession;
import com.cobaltplatform.api.model.db.AccountSource;
import com.cobaltplatform.api.model.db.Assessment;
import com.cobaltplatform.api.model.db.AssessmentType.AssessmentTypeId;
import com.cobaltplatform.api.model.db.CheckInActionStatus.CheckInActionStatusId;
import com.cobaltplatform.api.model.db.CourseSession;
import com.cobaltplatform.api.model.db.CourseSessionStatus.CourseSessionStatusId;
import com.cobaltplatform.api.model.db.CourseSessionUnitStatus.CourseSessionUnitStatusId;
import com.cobaltplatform.api.model.db.CourseUnit;
import com.cobaltplatform.api.model.db.Feature.FeatureId;
import com.cobaltplatform.api.model.db.GroupSession;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.MessageType.MessageTypeId;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.PatientOrderCareType.PatientOrderCareTypeId;
import com.cobaltplatform.api.model.db.PatientOrderClosureReason.PatientOrderClosureReasonId;
import com.cobaltplatform.api.model.db.PatientOrderConsentStatus.PatientOrderConsentStatusId;
import com.cobaltplatform.api.model.db.PatientOrderCrisisHandler;
import com.cobaltplatform.api.model.db.PatientOrderDisposition;
import com.cobaltplatform.api.model.db.PatientOrderFocusType.PatientOrderFocusTypeId;
import com.cobaltplatform.api.model.db.PatientOrderIntakeInsuranceStatus.PatientOrderIntakeInsuranceStatusId;
import com.cobaltplatform.api.model.db.PatientOrderIntakeLocationStatus.PatientOrderIntakeLocationStatusId;
import com.cobaltplatform.api.model.db.PatientOrderIntakeWantsServicesStatus.PatientOrderIntakeWantsServicesStatusId;
import com.cobaltplatform.api.model.db.PatientOrderReferralReason.PatientOrderReferralReasonId;
import com.cobaltplatform.api.model.db.PatientOrderReferralSource.PatientOrderReferralSourceId;
import com.cobaltplatform.api.model.db.PatientOrderResourcingStatus.PatientOrderResourcingStatusId;
import com.cobaltplatform.api.model.db.PatientOrderSafetyPlanningStatus.PatientOrderSafetyPlanningStatusId;
import com.cobaltplatform.api.model.db.PatientOrderScheduledMessageType.PatientOrderScheduledMessageTypeId;
import com.cobaltplatform.api.model.db.PatientOrderTriageOverrideReason.PatientOrderTriageOverrideReasonId;
import com.cobaltplatform.api.model.db.PatientOrderTriageSource.PatientOrderTriageSourceId;
import com.cobaltplatform.api.model.db.RawPatientOrder;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.Screening;
import com.cobaltplatform.api.model.db.ScreeningAnswer;
import com.cobaltplatform.api.model.db.ScreeningAnswerContentHint.ScreeningAnswerContentHintId;
import com.cobaltplatform.api.model.db.ScreeningAnswerFormat.ScreeningAnswerFormatId;
import com.cobaltplatform.api.model.db.ScreeningAnswerOption;
import com.cobaltplatform.api.model.db.ScreeningConfirmationPrompt;
import com.cobaltplatform.api.model.db.ScreeningFlow;
import com.cobaltplatform.api.model.db.ScreeningFlowType.ScreeningFlowTypeId;
import com.cobaltplatform.api.model.db.ScreeningFlowVersion;
import com.cobaltplatform.api.model.db.ScreeningQuestion;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.db.ScreeningSessionAnsweredScreeningQuestion;
import com.cobaltplatform.api.model.db.ScreeningSessionScreening;
import com.cobaltplatform.api.model.db.ScreeningType;
import com.cobaltplatform.api.model.db.ScreeningType.ScreeningTypeId;
import com.cobaltplatform.api.model.db.ScreeningVersion;
import com.cobaltplatform.api.model.db.Study;
import com.cobaltplatform.api.model.db.SupportRole;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.model.service.ScreeningQuestionContext;
import com.cobaltplatform.api.model.service.ScreeningQuestionContextId;
import com.cobaltplatform.api.model.service.ScreeningQuestionWithAnswerOptions;
import com.cobaltplatform.api.model.service.ScreeningScore;
import com.cobaltplatform.api.model.service.ScreeningSessionDestination;
import com.cobaltplatform.api.model.service.ScreeningSessionDestination.ScreeningSessionDestinationId;
import com.cobaltplatform.api.model.service.ScreeningSessionDestinationResultId;
import com.cobaltplatform.api.model.service.ScreeningSessionResult;
import com.cobaltplatform.api.model.service.ScreeningSessionResult.ScreeningAnswerResult;
import com.cobaltplatform.api.model.service.ScreeningSessionResult.ScreeningQuestionResult;
import com.cobaltplatform.api.model.service.ScreeningSessionResult.ScreeningSessionScreeningResult;
import com.cobaltplatform.api.model.service.ScreeningSessionScreeningWithType;
import com.cobaltplatform.api.service.ScreeningService.CreateScreeningAnswersResult.CreateScreeningAnswersMessage;
import com.cobaltplatform.api.service.ScreeningService.CreateScreeningAnswersResult.CreateScreeningAnswersQuestionResult;
import com.cobaltplatform.api.service.ScreeningService.ResultsFunctionOutput.SupportRoleRecommendation;
import com.cobaltplatform.api.util.JavascriptExecutionException;
import com.cobaltplatform.api.util.JavascriptExecutor;
import com.cobaltplatform.api.util.Normalizer;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
import com.pyranid.DatabaseException;
import com.pyranid.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.sql.Savepoint;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlInListPlaceholders;
import static com.cobaltplatform.api.util.ValidationUtility.isValidEmailAddress;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class ScreeningService {
	@Nonnull
	private final Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final Provider<InteractionService> interactionServiceProvider;
	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final Provider<PatientOrderService> patientOrderServiceProvider;
	@Nonnull
	private final Provider<GroupSessionService> groupSessionServiceProvider;
	@Nonnull
	private final Provider<CourseService> courseServiceProvider;
	@Nonnull
	private final Provider<AuthorizationService> authorizationServiceProvider;
	@Nonnull
	private final Provider<MessageService> messageServiceProvider;
	@Nonnull
	private final Provider<StudyService> studyServiceProvider;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final ScreeningConfirmationPromptApiResponseFactory screeningConfirmationPromptApiResponseFactory;
	@Nonnull
	private final JavascriptExecutor javascriptExecutor;
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final ErrorReporter errorReporter;
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

	@Inject
	public ScreeningService(@Nonnull Provider<InstitutionService> institutionServiceProvider,
													@Nonnull Provider<InteractionService> interactionServiceProvider,
													@Nonnull Provider<AccountService> accountServiceProvider,
													@Nonnull Provider<PatientOrderService> patientOrderServiceProvider,
													@Nonnull Provider<GroupSessionService> groupSessionServiceProvider,
													@Nonnull Provider<CourseService> courseServiceProvider,
													@Nonnull Provider<AuthorizationService> authorizationServiceProvider,
													@Nonnull Provider<MessageService> messageServiceProvider,
													@Nonnull Provider<StudyService> studyServiceProvider,
													@Nonnull Provider<CurrentContext> currentContextProvider,
													@Nonnull ScreeningConfirmationPromptApiResponseFactory screeningConfirmationPromptApiResponseFactory,
													@Nonnull JavascriptExecutor javascriptExecutor,
													@Nonnull EnterprisePluginProvider enterprisePluginProvider,
													@Nonnull ErrorReporter errorReporter,
													@Nonnull Normalizer normalizer,
													@Nonnull DatabaseProvider databaseProvider,
													@Nonnull Configuration configuration,
													@Nonnull Strings strings) {
		requireNonNull(institutionServiceProvider);
		requireNonNull(interactionServiceProvider);
		requireNonNull(accountServiceProvider);
		requireNonNull(patientOrderServiceProvider);
		requireNonNull(groupSessionServiceProvider);
		requireNonNull(courseServiceProvider);
		requireNonNull(authorizationServiceProvider);
		requireNonNull(messageServiceProvider);
		requireNonNull(studyServiceProvider);
		requireNonNull(currentContextProvider);
		requireNonNull(screeningConfirmationPromptApiResponseFactory);
		requireNonNull(javascriptExecutor);
		requireNonNull(enterprisePluginProvider);
		requireNonNull(errorReporter);
		requireNonNull(normalizer);
		requireNonNull(databaseProvider);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.institutionServiceProvider = institutionServiceProvider;
		this.interactionServiceProvider = interactionServiceProvider;
		this.accountServiceProvider = accountServiceProvider;
		this.patientOrderServiceProvider = patientOrderServiceProvider;
		this.groupSessionServiceProvider = groupSessionServiceProvider;
		this.courseServiceProvider = courseServiceProvider;
		this.authorizationServiceProvider = authorizationServiceProvider;
		this.messageServiceProvider = messageServiceProvider;
		this.studyServiceProvider = studyServiceProvider;
		this.currentContextProvider = currentContextProvider;
		this.screeningConfirmationPromptApiResponseFactory = screeningConfirmationPromptApiResponseFactory;
		this.javascriptExecutor = javascriptExecutor;
		this.enterprisePluginProvider = enterprisePluginProvider;
		this.errorReporter = errorReporter;
		this.normalizer = normalizer;
		this.databaseProvider = databaseProvider;
		this.configuration = configuration;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<Screening> findScreeningById(@Nullable UUID screeningId) {
		if (screeningId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening WHERE screening_id=?",
				Screening.class, screeningId);
	}

	@Nonnull
	public Optional<ScreeningVersion> findScreeningVersionById(@Nullable UUID screeningVersionId) {
		if (screeningVersionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_version WHERE screening_version_id=?",
				ScreeningVersion.class, screeningVersionId);
	}

	@Nonnull
	public Optional<ScreeningSession> findScreeningSessionById(@Nullable UUID screeningSessionId) {
		if (screeningSessionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_session WHERE screening_session_id=?",
				ScreeningSession.class, screeningSessionId);
	}

	@Nonnull
	public Optional<ScreeningSessionScreening> findScreeningSessionScreeningById(@Nullable UUID screeningSessionScreeningId) {
		if (screeningSessionScreeningId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_session_screening WHERE screening_session_screening_id=?",
				ScreeningSessionScreening.class, screeningSessionScreeningId);
	}

	@Nonnull
	public Optional<ScreeningType> findScreeningTypeById(@Nullable ScreeningTypeId screeningTypeId) {
		if (screeningTypeId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_type WHERE screening_type_id=?",
				ScreeningType.class, screeningTypeId);
	}

	@Nonnull
	public Optional<ScreeningFlow> findScreeningFlowById(@Nullable UUID screeningFlowId) {
		if (screeningFlowId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_flow WHERE screening_flow_id=?",
				ScreeningFlow.class, screeningFlowId);
	}

	@Nonnull
	public Optional<ScreeningFlowVersion> findScreeningFlowVersionById(@Nullable UUID screeningFlowVersionId) {
		if (screeningFlowVersionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_flow_version WHERE screening_flow_version_id=?",
				ScreeningFlowVersion.class, screeningFlowVersionId);
	}

	@Nonnull
	public List<AccountSource> findRequiredAccountSourcesByScreeningFlowVersionId(@Nullable UUID screeningFlowVersionId) {
		if (screeningFlowVersionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT asrc.*
				FROM account_source asrc, screening_flow_version_account_source sfvas
				WHERE asrc.account_source_id=sfvas.account_source_id
				AND sfvas.screening_flow_version_id=?
				ORDER BY sfvas.display_order
				""", AccountSource.class, screeningFlowVersionId);
	}

	@Nonnull
	public List<ScreeningQuestion> findInitialScreeningQuestionsByScreeningFlowVersionId(@Nullable UUID screeningFlowVersionId) {
		if (screeningFlowVersionId == null)
			return List.of();

		// We don't currently have a mechanism to know up-front all possible screenings that might be part of a screening flow version,
		// so for now we just take the questions in the initial screening.
		//
		// In general, while we could maintain a list of possible screening per screening flow version, we would not be able
		// to reliably store off things like question ordering because flows support arbitrary branching
		return getDatabase().queryForList("""
				SELECT sq.*
				FROM screening_question sq, screening s, screening_flow_version sfv
				WHERE sq.screening_version_id=s.active_screening_version_id
				AND s.screening_id=sfv.initial_screening_id
				AND sfv.screening_flow_version_id=?
				ORDER BY sq.display_order
				""", ScreeningQuestion.class, screeningFlowVersionId);
	}

	@Nonnull
	public Optional<ScreeningConfirmationPrompt> findScreeningConfirmationPromptById(@Nullable UUID screeningConfirmationPromptId) {
		if (screeningConfirmationPromptId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_confirmation_prompt WHERE screening_confirmation_prompt_id=?",
				ScreeningConfirmationPrompt.class, screeningConfirmationPromptId);
	}

	@Nonnull
	public Boolean applyTemplatingToScreeningConfirmationPromptForScreeningSession(@Nullable ScreeningConfirmationPrompt screeningConfirmationPrompt,
																																								 @Nullable UUID screeningSessionId) {
		if (screeningConfirmationPrompt == null || screeningSessionId == null)
			return false;

		ScreeningSession screeningSession = findScreeningSessionById(screeningSessionId).orElse(null);

		if (screeningSession == null)
			return false;

		return applyTemplatingToScreeningConfirmationPromptForScreeningSession(screeningConfirmationPrompt, screeningSession);
	}

	/**
	 * For example, turns:
	 * <pre>
	 * Thank you, {{patientFirstName}}
	 * </pre>
	 * into
	 * <pre>
	 * Thank you, Eleanor
	 * </pre>
	 */
	@Nonnull
	public Boolean applyTemplatingToScreeningConfirmationPromptForScreeningSession(@Nullable ScreeningConfirmationPrompt screeningConfirmationPrompt,
																																								 @Nullable ScreeningSession screeningSession) {
		if (screeningConfirmationPrompt == null || screeningSession == null)
			return false;

		// For now, templating data is only used for scenarios where this screening is tied to a patient order.
		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(screeningSession.getPatientOrderId()).orElse(null);

		if (patientOrder == null)
			return false;

		Map<String, String> placeholderValuesByName = new HashMap<>();
		placeholderValuesByName.put("patientFirstName", patientOrder.getPatientFirstName() == null
				? getStrings().get("Patient")
				: patientOrder.getPatientFirstName());

		// Really quick-and-dirty replacement using {{handlebars}} kind of syntax.
		// This has a number of drawbacks but it's simple and good enough for the very basic scenarios we need to support.
		// A more robust solution would actually use Handlebars or a similar templating system.
		String text = screeningConfirmationPrompt.getText();

		for (Map.Entry<String, String> entry : placeholderValuesByName.entrySet()) {
			String placeholderName = entry.getKey();
			String placeholderValue = entry.getValue();

			text = text.replace(format("{{%s}}", placeholderName), placeholderValue);
		}

		screeningConfirmationPrompt.setText(text);

		return true;
	}

	@Nonnull
	public Optional<ScreeningSession> findMostRecentCompletedScreeningSession(@Nullable UUID accountId,
																																						@Nullable UUID screeningFlowId) {
		if (accountId == null || screeningFlowId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT ss.*
				FROM screening_session ss, screening_flow_version sfv
				WHERE sfv.screening_flow_version_id=ss.screening_flow_version_id
				AND sfv.screening_flow_id=? 
				AND ss.completed = TRUE
				AND ss.skipped = FALSE
				AND ss.target_account_id=?
				ORDER BY ss.last_updated DESC
				LIMIT 1
				""", ScreeningSession.class, screeningFlowId, accountId);
	}

	@Nonnull
	public Boolean hasAccountIdTakenScreeningFlowId(@Nullable Account account,
																									@Nullable UUID screeningFlowId) {
		if (account == null || screeningFlowId == null)
			return false;

		ScreeningFlow screeningFlow = findScreeningFlowById(screeningFlowId).orElse(null);

		if (screeningFlow == null)
			return false;

		ScreeningSession screeningSession = findMostRecentCompletedScreeningSession(account.getAccountId(), screeningFlowId).orElse(null);

		return screeningSession != null;
	}

	@Nonnull
	public Boolean shouldAccountIdTakeScreeningFlowId(@Nullable Account account,
																										@Nullable UUID screeningFlowId) {
		if (account == null || screeningFlowId == null)
			return false;

		ScreeningFlow screeningFlow = findScreeningFlowById(screeningFlowId).orElse(null);

		if (screeningFlow == null)
			return false;

		ScreeningSession screeningSession = findMostRecentCompletedScreeningSession(account.getAccountId(), screeningFlowId).orElse(null);

		// If there is no screening session then return true because this user has not taken a screening
		if (screeningSession == null)
			return true;

		ScreeningFlowVersion screeningFlowVersion = findScreeningFlowVersionById(screeningFlow.getActiveScreeningFlowVersionId()).get();

		// If there is no minutes until retake specified, assume not retakeable
		if (screeningFlowVersion.getMinutesUntilRetake() == null)
			return false;

		return Duration.between(screeningSession.getCompletedAt(), Instant.now()).toMinutes() > screeningFlowVersion.getMinutesUntilRetake();
	}

	@Nonnull
	public List<SupportRole> findRecommendedSupportRolesByAccountId(@Nullable UUID accountId,
																																	@Nullable UUID triageScreeningFlowId) {
		if (accountId == null || triageScreeningFlowId == null)
			return Collections.emptyList();

		Account account = getAccountService().findAccountById(accountId).orElse(null);

		if (account == null)
			return Collections.emptyList();

		ScreeningSession mostRecentCompletedScreeningSession =
				findMostRecentCompletedScreeningSession(accountId, triageScreeningFlowId).orElse(null);

		if (mostRecentCompletedScreeningSession == null)
			return Collections.emptyList();

		List<SupportRole> recommendedSupportRoles = getDatabase().queryForList("""
				SELECT sr.*
				FROM support_role sr, screening_session_support_role_recommendation sssrr
				WHERE sr.support_role_id=sssrr.support_role_id
				AND sssrr.screening_session_id=?
				ORDER BY sssrr.weight DESC
				""", SupportRole.class, mostRecentCompletedScreeningSession.getScreeningSessionId());

		return recommendedSupportRoles;
	}

	@Nonnull
	public List<Screening> findScreeningsByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT s.* FROM screening s, screening_institution si 
				WHERE s.screening_id=si.screening_id
				AND si.institution_id=?
				ORDER BY s.name
				""", Screening.class, institutionId);
	}

	@Nonnull
	public List<ScreeningFlow> findScreeningFlowsByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM screening_flow WHERE institution_id=? ORDER BY name",
				ScreeningFlow.class, institutionId);
	}

	@Nonnull
	public List<ScreeningFlow> findScreeningFlowsByInstitutionIdAndScreeningFlowTypeId(@Nullable InstitutionId institutionId,
																																										 @Nullable ScreeningFlowTypeId screeningFlowTypeId) {
		if (institutionId == null || screeningFlowTypeId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
						SELECT *
						FROM screening_flow
						WHERE institution_id=?
						AND screening_flow_type_id=?
						ORDER BY name
						""",
				ScreeningFlow.class, institutionId, screeningFlowTypeId);
	}

	@Nonnull
	public List<ScreeningType> findScreeningTypes() {
		return getDatabase().queryForList("SELECT * FROM screening_type ORDER BY description", ScreeningType.class);
	}

	@Nonnull
	public List<ScreeningType> findScreeningTypesByScreeningFlowVersionId(@Nullable UUID screeningFlowVersionId) {
		if (screeningFlowVersionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT st.*
				FROM screening_type st, screening_flow_version_screening_type sfvst
				WHERE sfvst.screening_type_id=st.screening_type_id
				AND sfvst.screening_flow_version_id=?
				ORDER BY st.description
				""", ScreeningType.class, screeningFlowVersionId);
	}

	@Nonnull
	public Optional<ScreeningAnswerOption> findScreeningAnswerOptionById(@Nullable UUID screeningAnswerOptionId) {
		if (screeningAnswerOptionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_answer_option WHERE screening_answer_option_id=?",
				ScreeningAnswerOption.class, screeningAnswerOptionId);
	}

	@Nonnull
	public Optional<ScreeningQuestion> findScreeningQuestionById(@Nullable UUID screeningQuestionId) {
		if (screeningQuestionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_question WHERE screening_question_id=?",
				ScreeningQuestion.class, screeningQuestionId);
	}

	@Nonnull
	public Optional<ScreeningAnswer> findScreeningAnswerById(@Nullable UUID screeningAnswerId) {
		if (screeningAnswerId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM screening_answer WHERE screening_answer_id=?",
				ScreeningAnswer.class, screeningAnswerId);
	}

	@Nonnull
	public List<ScreeningFlowVersion> findScreeningFlowVersionsByScreeningFlowId(@Nullable UUID screeningFlowId) {
		if (screeningFlowId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT *
				FROM screening_flow_version
				WHERE screening_flow_id=?
				ORDER BY version_number DESC
				""", ScreeningFlowVersion.class, screeningFlowId);
	}

	@Nonnull
	public List<ScreeningSession> findScreeningSessionsByScreeningFlowId(@Nullable UUID screeningFlowId,
																																			 @Nullable UUID participantAccountId) {
		if (screeningFlowId == null || participantAccountId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
						SELECT ss.* FROM screening_session ss, screening_flow_version sfv 
						WHERE sfv.screening_flow_id=? AND ss.screening_flow_version_id=sfv.screening_flow_version_id 
						AND (ss.target_account_id=? OR ss.created_by_account_id=?)
						ORDER BY ss.created DESC
						""",
				ScreeningSession.class, screeningFlowId, participantAccountId, participantAccountId);
	}

	@Nonnull
	public List<ScreeningSession> findScreeningSessionsByScreeningFlowIdAndTargetAccountId(@Nullable UUID screeningFlowId,
																																												 @Nullable UUID targetAccountId) {
		if (screeningFlowId == null || targetAccountId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
						SELECT ss.* FROM screening_session ss, screening_flow_version sfv 
						WHERE sfv.screening_flow_id=? AND ss.screening_flow_version_id=sfv.screening_flow_version_id 
						AND ss.target_account_id=?
						ORDER BY ss.created DESC
						""",
				ScreeningSession.class, screeningFlowId, targetAccountId);
	}

	@Nonnull
	public List<ScreeningSession> findScreeningSessionsByScreeningFlowVersionIdAndTargetAccountId(@Nullable UUID screeningFlowVersionId,
																																																@Nullable UUID targetAccountId) {
		if (screeningFlowVersionId == null || targetAccountId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
						SELECT * FROM screening_session
						WHERE screening_flow_version_id=?
						AND target_account_id=?
						ORDER BY created DESC
						""",
				ScreeningSession.class, screeningFlowVersionId, targetAccountId);
	}

	@Nonnull
	public List<ScreeningSession> findScreeningSessionsByScreeningFlowIdAndPatientOrderId(@Nullable UUID screeningFlowId,
																																												@Nullable UUID patientOrderId) {
		if (screeningFlowId == null || patientOrderId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
						SELECT ss.* FROM screening_session ss, screening_flow_version sfv
						WHERE sfv.screening_flow_id=? AND ss.screening_flow_version_id=sfv.screening_flow_version_id
						AND ss.patient_order_id=?
						ORDER BY ss.created DESC
						""",
				ScreeningSession.class, screeningFlowId, patientOrderId);
	}

	@Nonnull
	public Optional<ScreeningSession> findMostRecentlyCompletedScreeningSessionByScreeningFlowAndTargetAccountId(@Nullable UUID screeningFlowId,
																																																							 @Nullable UUID targetAccountId) {
		if (screeningFlowId == null || targetAccountId == null)
			return Optional.empty();

		List<ScreeningSession> completedScreeningSessions = findScreeningSessionsByScreeningFlowIdAndTargetAccountId(screeningFlowId, targetAccountId).stream()
				.filter(screeningSession -> screeningSession.getCompleted() && !screeningSession.getSkipped())
				.collect(Collectors.toList());

		// Most recently completed first
		Collections.sort(completedScreeningSessions, (ss1, ss2) -> ss2.getCompletedAt().compareTo(ss1.getCompletedAt()));

		return Optional.ofNullable(completedScreeningSessions.size() > 0 ? completedScreeningSessions.get(0) : null);
	}

	@Nonnull
	public List<ScreeningSessionScreening> findScreeningSessionScreeningsByScreeningSessionId(@Nullable UUID screeningSessionId) {
		if (screeningSessionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT * 
				FROM v_screening_session_screening 
				WHERE screening_session_id=? 
				ORDER BY screening_order
				""", ScreeningSessionScreening.class, screeningSessionId);
	}

	@Nonnull
	public UUID createScreeningSession(@Nonnull CreateScreeningSessionRequest request) {
		requireNonNull(request);

		UUID targetAccountId = request.getTargetAccountId();
		UUID createdByAccountId = request.getCreatedByAccountId();
		UUID screeningFlowId = request.getScreeningFlowId();
		UUID screeningFlowVersionId = request.getScreeningFlowVersionId();
		UUID patientOrderId = request.getPatientOrderId();
		UUID groupSessionId = request.getGroupSessionId();
		UUID courseSessionId = request.getCourseSessionId();
		UUID accountCheckInActionId = request.getAccountCheckInActionId();
		ScreeningFlowVersion screeningFlowVersion = null;
		Institution institution = null;
		Account targetAccount = null;
		Account createdByAccount = null;
		boolean immediatelySkip = request.getImmediatelySkip() == null ? false : request.getImmediatelySkip();
		Map<String, Object> metadata = request.getMetadata();
		UUID screeningSessionId = UUID.randomUUID();
		ValidationException validationException = new ValidationException();

		boolean creatingIntegratedCareScreeningSession = false;

		if (screeningFlowId == null && screeningFlowVersionId == null) {
			validationException.add(getStrings().get("Either a screening flow ID or screening flow version ID is required."));
		} else if (screeningFlowId != null && screeningFlowVersionId != null) {
			validationException.add(getStrings().get("You cannot provide both a screening flow ID and a screening flow version ID."));
		} else if (screeningFlowVersionId != null) {
			screeningFlowVersion = findScreeningFlowVersionById(screeningFlowVersionId).orElse(null);

			if (screeningFlowVersion == null)
				validationException.add(new FieldError("screeningFlowVersionId", getStrings().get("Screening flow ID is invalid.")));
		} else {
			ScreeningFlow screeningFlow = findScreeningFlowById(screeningFlowId).orElse(null);

			if (screeningFlow == null) {
				validationException.add(new FieldError("screeningFlowId", getStrings().get("Screening flow ID is invalid.")));
			} else {
				screeningFlowVersion = findScreeningFlowVersionById(screeningFlow.getActiveScreeningFlowVersionId()).get();

				if (screeningFlow.getScreeningFlowTypeId() == ScreeningFlowTypeId.GROUP_SESSION_INTAKE && groupSessionId == null)
					validationException.add(new FieldError("groupSessionId", getStrings().get("Group Session ID is required for this type of screening flow.")));
				else if (screeningFlow.getScreeningFlowTypeId() != ScreeningFlowTypeId.GROUP_SESSION_INTAKE && groupSessionId != null)
					throw new IllegalStateException(format("It's illegal to specify a Group Session ID for %s.%s",
							ScreeningFlowTypeId.class.getSimpleName(), screeningFlow.getScreeningFlowTypeId().name()));

				if (screeningFlow.getScreeningFlowTypeId() == ScreeningFlowTypeId.STUDY && accountCheckInActionId == null)
					validationException.add(new FieldError("accountCheckInActionId", getStrings().get("Account check-in action ID is required for this type of screening flow.")));
				else if (screeningFlow.getScreeningFlowTypeId() != ScreeningFlowTypeId.STUDY && accountCheckInActionId != null)
					throw new IllegalStateException(format("It's illegal to specify a account check-in action ID for %s.%s",
							ScreeningFlowTypeId.class.getSimpleName(), screeningFlow.getScreeningFlowTypeId().name()));
				else {
					Boolean screeningSessionExists = getDatabase().queryForObject("""
							SELECT count(*) > 0 
							FROM screening_session
							WHERE account_check_in_action_id = ?""", Boolean.class, accountCheckInActionId).get();

					if (screeningSessionExists)
						validationException.add(new FieldError("accountCheckInActionId", getStrings().get("Screening session already exists for this account check in action.")));
				}
			}
		}

		if (createdByAccountId == null) {
			validationException.add(new FieldError("createdByAccountId", getStrings().get("Created-by account ID is required.")));
		} else {
			createdByAccount = getAccountService().findAccountById(createdByAccountId).orElse(null);

			if (createdByAccount == null) {
				validationException.add(new FieldError("createdByAccountId", getStrings().get("Created-by account ID is invalid.")));
			} else {
				institution = getInstitutionService().findInstitutionById(createdByAccount.getInstitutionId()).get();

				// Special behavior if we're IC and this is the special IC screening flow
				creatingIntegratedCareScreeningSession = institution.getIntegratedCareEnabled()
						&& (Objects.equals(screeningFlowVersion.getScreeningFlowId(), institution.getIntegratedCareScreeningFlowId())
						|| Objects.equals(screeningFlowVersion.getScreeningFlowId(), institution.getIntegratedCareIntakeScreeningFlowId()));
			}
		}

		// Integrated care screening sessions require a patient order.  Target account is optional (maybe patient has not signed in yet)
		// Otherwise, target account is required.
		if (creatingIntegratedCareScreeningSession) {
			if (patientOrderId == null) {
				validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is required.")));
			} else {
				RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderId).orElse(null);

				if (patientOrder == null)
					validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is invalid.")));
			}

			if (targetAccountId != null) {
				targetAccount = getAccountService().findAccountById(targetAccountId).orElse(null);

				if (targetAccount == null)
					validationException.add(new FieldError("targetAccountId", getStrings().get("Target account ID is invalid.")));
			}
		} else {
			if (targetAccountId == null) {
				validationException.add(new FieldError("targetAccountId", getStrings().get("Target account ID is required.")));
			} else {
				targetAccount = getAccountService().findAccountById(targetAccountId).orElse(null);

				if (targetAccount == null)
					validationException.add(new FieldError("targetAccountId", getStrings().get("Target account ID is invalid.")));
			}

			// It's illegal to specify a patient order for non-IC screenings, so make sure it's nulled out
			patientOrderId = null;
		}

		if (accountCheckInActionId != null) {
			if (targetAccountId == null) {
				validationException.add(new FieldError("targetAccountId", getStrings().get("Target account ID is required.")));
			} else {
				Optional<AccountCheckInAction> accountCheckInAction = getStudyService().findAccountCheckInActionFoAccountAndCheckIn(targetAccountId, accountCheckInActionId);
				if (!accountCheckInAction.isPresent())
					validationException.add(new FieldError("accountCheckInActionId", getStrings().get("Account check-in is not valid for this account.")));
			}
		}

		if (validationException.hasErrors())
			throw validationException;

		String metadataAsJson = ScreeningSession.metadataToJson(metadata).orElse(null);

		try {
			getDatabase().execute("""
							INSERT INTO screening_session(
								screening_session_id,
								screening_flow_version_id,
								target_account_id,
								created_by_account_id,
								patient_order_id,
								group_session_id,
								course_session_id,
								account_check_in_action_id,
								metadata
							)
							VALUES (?,?,?,?,?,?,?,?,CAST(? AS JSONB))
							""", screeningSessionId, screeningFlowVersion.getScreeningFlowVersionId(), targetAccountId, createdByAccountId,
					patientOrderId, groupSessionId, courseSessionId, accountCheckInActionId, metadataAsJson);
		} catch (DatabaseException e) {
			getErrorReporter().report(e);
			getLogger().error(format("Unable to create screening session for screeningFlowVersion %s and accountCheckInActionId %s", screeningFlowVersion.getScreeningFlowVersionId(), accountCheckInActionId), e);
			validationException.add(new FieldError("accountCheckInActionId", getStrings().get("Cannot start assessment at this time. Please try again later")));
			throw validationException;
		}

		if (creatingIntegratedCareScreeningSession) {
			// Integrated care only: if there are any pending welcome reminder messages, cancel them
			getPatientOrderService().deleteFuturePatientOrderScheduledMessageGroupsForPatientOrderId(patientOrderId, createdByAccountId, Set.of(
					PatientOrderScheduledMessageTypeId.WELCOME_REMINDER
			));

			// Special case: if we are creating an IC intake screening session and an IC clinical screening session already exists for
			// this patient order, then mark the clinical screening session as "skipped" in order to invalidate it.
			// If we did not do this, the clinical screening would still appear active even though it's no longer relevant.
			// On this UI side, we get in this state if a patient completes the intake screening, starts the clinical screening,
			// then returns to the homepage and chooses to restart the screenings from zero.  Old behavior was we would create a new intake
			// screening but leave the clinical one hanging out, so the `mostRecentScreeningSession*` fields would be populated even though they should not have been.
			if (Objects.equals(screeningFlowVersion.getScreeningFlowId(), institution.getIntegratedCareIntakeScreeningFlowId())) {
				PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).get();
				if (patientOrder.getMostRecentScreeningSessionId() != null) {
					getLogger().info("Since we are starting an IC intake screening session and clinical screening session ID {} already exists, mark it as skipped.", patientOrder.getMostRecentScreeningSessionId());

					skipScreeningSession(new SkipScreeningSessionRequest() {{
						setScreeningSessionId(patientOrder.getMostRecentScreeningSessionId());
						setForceSkip(true);
					}}, true);
				}
			}
		}

		// If we're immediately skipping, mark this session as completed/skipped and do nothing else.
		// If we're not immediately skipping, create an initial screening session screening
		if (immediatelySkip) {
			skipScreeningSession(new SkipScreeningSessionRequest() {{
				setScreeningSessionId(screeningSessionId);
			}});

			if (accountCheckInActionId != null)
				getStudyService().updateAccountCheckInAction(targetAccount, new UpdateCheckInAction() {{
					setAccountCheckInActionId(accountCheckInActionId);
					setCheckInActionStatusId(CheckInActionStatusId.COMPLETE);
				}});
		} else {
			UUID initialScreeningId = null;

			// If there's an initialization function, invoke it.
			// The function can optionally allow for starting on a different screening than what's specified as the default
			if (screeningFlowVersion.getInitializationFunction() != null) {
				InitializationFunctionOutput initializationFunctionOutput = executeScreeningFlowFunction(screeningFlowVersion.getInitializationFunction(),
						InitializationFunctionOutput.class, screeningSessionId, createdByAccount.getInstitutionId(), Map.of()).get();

				initialScreeningId = initializationFunctionOutput.getInitialScreeningId();
			}

			if (initialScreeningId == null)
				initialScreeningId = screeningFlowVersion.getInitialScreeningId();

			Screening screening = findScreeningById(initialScreeningId).get();

			// Initial screening is the current version of the screening specified in the flow
			getDatabase().execute("""
					INSERT INTO screening_session_screening(screening_session_id, screening_version_id, screening_order)
					VALUES (?,?,?)
					""", screeningSessionId, screening.getActiveScreeningVersionId(), 1);

			if (accountCheckInActionId != null)
				getStudyService().updateAccountCheckInAction(targetAccount, new UpdateCheckInAction() {{
					setAccountCheckInActionId(accountCheckInActionId);
					setCheckInActionStatusId(CheckInActionStatusId.IN_PROGRESS);
				}});
		}

		return screeningSessionId;
	}

	@Nonnull
	public void skipScreeningSession(@Nonnull SkipScreeningSessionRequest request) {
		requireNonNull(request);
		skipScreeningSession(request, false);
	}

	@Nonnull
	public void skipScreeningSession(@Nonnull SkipScreeningSessionRequest request,
																	 @Nonnull Boolean forceSkipNoMatterWhat) {
		requireNonNull(request);
		requireNonNull(forceSkipNoMatterWhat);

		UUID screeningSessionId = request.getScreeningSessionId();
		boolean forceSkip = request.getForceSkip() == null ? false : request.getForceSkip();
		ValidationException validationException = new ValidationException();

		if (screeningSessionId == null) {
			validationException.add(new FieldError("screeningSessionId", getStrings().get("Screening Session ID is required.")));
		} else {
			ScreeningSession screeningSession = findScreeningSessionById(screeningSessionId).orElse(null);

			if (screeningSession == null) {
				validationException.add(new FieldError("screeningSessionId", getStrings().get("Screening Session ID is invalid.")));
			} else {
				if (forceSkipNoMatterWhat) {
					// No further validation is necessary; we know what we're doing, force a skip no matter what
				} else {
					// Default skip checks: can't skip completed screening sessions, and can't skip unskippable sessions unless forced to
					if (screeningSession.getCompleted()) {
						validationException.add(getStrings().get("Sorry, you are not permitted to skip this assessment because it has already been completed."));
					} else {
						ScreeningFlowVersion screeningFlowVersion = findScreeningFlowVersionById(screeningSession.getScreeningFlowVersionId()).get();

						if (!forceSkip && !screeningFlowVersion.getSkippable())
							validationException.add(getStrings().get("Sorry, you are not permitted to skip this assessment."));
					}
				}
			}
		}

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				UPDATE screening_session
				SET completed=TRUE, completed_at=NOW(), skipped=TRUE, skipped_at=NOW()
				WHERE screening_session_id=?
				""", screeningSessionId);
	}

	@Nonnull
	public Optional<ScreeningQuestionContext> findScreeningQuestionContextById(@Nullable ScreeningQuestionContextId screeningQuestionContextId) {
		if (screeningQuestionContextId == null)
			return Optional.empty();

		ScreeningSessionScreening screeningSessionScreening = findScreeningSessionScreeningById(screeningQuestionContextId.getScreeningSessionScreeningId()).orElse(null);

		if (screeningSessionScreening == null)
			return Optional.empty();

		// Get all the questions + answer options
		List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions = findScreeningQuestionsWithAnswerOptionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());

		ScreeningQuestionWithAnswerOptions screeningQuestionWithAnswerOptions = null;

		for (ScreeningQuestionWithAnswerOptions potentialScreeningQuestionWithAnswerOptions : screeningQuestionsWithAnswerOptions) {
			if (potentialScreeningQuestionWithAnswerOptions.getScreeningQuestion().getScreeningQuestionId().equals(screeningQuestionContextId.getScreeningQuestionId())) {
				screeningQuestionWithAnswerOptions = potentialScreeningQuestionWithAnswerOptions;
				break;
			}
		}

		if (screeningQuestionWithAnswerOptions == null)
			return Optional.empty();

		List<ScreeningAnswer> screeningAnswers = findScreeningAnswersByScreeningQuestionContextId(screeningQuestionContextId);

		return Optional.of(new ScreeningQuestionContext(screeningSessionScreening,
				screeningQuestionWithAnswerOptions.getScreeningQuestion(),
				screeningQuestionWithAnswerOptions.getScreeningAnswerOptions(),
				screeningAnswers));
	}

	@Nonnull
	public Optional<ScreeningQuestionContext> findPreviousScreeningQuestionContextByScreeningQuestionContextId(@Nullable ScreeningQuestionContextId screeningQuestionContextId) {
		if (screeningQuestionContextId == null)
			return Optional.empty();

		ScreeningSessionScreening screeningSessionScreening = findScreeningSessionScreeningById(screeningQuestionContextId.getScreeningSessionScreeningId()).orElse(null);

		if (screeningSessionScreening == null)
			return Optional.empty();

		// First, see if the previously-answered question is within the same screening session screening...
		List<ScreeningSessionAnsweredScreeningQuestion> screeningSessionAnsweredScreeningQuestions =
				findScreeningSessionAnsweredScreeningQuestionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());

		UUID sameScreeningSessionScreeningPreviousAnsweredQuestionId = null;

		for (ScreeningSessionAnsweredScreeningQuestion screeningSessionAnsweredScreeningQuestion : screeningSessionAnsweredScreeningQuestions) {
			if (screeningSessionAnsweredScreeningQuestion.getScreeningQuestionId().equals(screeningQuestionContextId.getScreeningQuestionId()))
				break;

			sameScreeningSessionScreeningPreviousAnsweredQuestionId = screeningSessionAnsweredScreeningQuestion.getScreeningQuestionId();
		}

		// Great, we found a previously-answered question in the same screening.  Let's return its context
		if (sameScreeningSessionScreeningPreviousAnsweredQuestionId != null)
			return findScreeningQuestionContextById(new ScreeningQuestionContextId(
					screeningSessionScreening.getScreeningSessionScreeningId(), sameScreeningSessionScreeningPreviousAnsweredQuestionId));

		// The previously-answered question was not in the same screening session screening.
		// Let's check the previous screening session screening and find the last-answered question there.
		//
		// Note: we are assuming the previous screening has at least 1 answered question.  It's technically possible but not practically likely that we
		// will have "empty" screenings w/ no questions
		List<ScreeningSessionScreening> screeningSessionScreenings = findCurrentScreeningSessionScreeningsByScreeningSessionId(screeningSessionScreening.getScreeningSessionId());

		ScreeningSessionScreening previousScreeningSessionScreening = null;

		for (ScreeningSessionScreening potentialPreviousScreeningSessionScreening : screeningSessionScreenings) {
			if (potentialPreviousScreeningSessionScreening.getScreeningSessionScreeningId().equals(screeningSessionScreening.getScreeningSessionScreeningId()))
				break;

			previousScreeningSessionScreening = potentialPreviousScreeningSessionScreening;
		}

		// No previous screening in the session?  That means the request was for the first question of the first screening, so there is no previous context
		if (previousScreeningSessionScreening == null)
			return Optional.empty();

		// Grab the previous screening's last answered question
		List<ScreeningSessionAnsweredScreeningQuestion> previousScreeningSessionAnsweredScreeningQuestions =
				findScreeningSessionAnsweredScreeningQuestionsByScreeningSessionScreeningId(previousScreeningSessionScreening.getScreeningSessionScreeningId());

		// We should not be in this state
		if (previousScreeningSessionAnsweredScreeningQuestions.size() == 0)
			return Optional.empty();

		ScreeningSessionAnsweredScreeningQuestion previousScreeningSessionLastAnsweredScreeningQuestion =
				previousScreeningSessionAnsweredScreeningQuestions.get(previousScreeningSessionAnsweredScreeningQuestions.size() - 1);

		return findScreeningQuestionContextById(new ScreeningQuestionContextId(
				previousScreeningSessionScreening.getScreeningSessionScreeningId(), previousScreeningSessionLastAnsweredScreeningQuestion.getScreeningQuestionId()));
	}

	@Nonnull
	public Optional<ScreeningQuestionContext> findNextUnansweredScreeningQuestionContextByScreeningSessionId(@Nullable UUID screeningSessionId) {
		if (screeningSessionId == null)
			return Optional.empty();

		ScreeningSession screeningSession = findScreeningSessionById(screeningSessionId).orElse(null);

		// If the session does not exist, there is no next question
		if (screeningSession == null)
			return Optional.empty();

		// If the session is already completed, there is no next question
		if (screeningSession.getCompleted())
			return Optional.empty();

		// Get the most recent screening for this session
		ScreeningSessionScreening screeningSessionScreening = findCurrentScreeningSessionScreeningByScreeningSessionId(screeningSessionId).orElse(null);

		// Indicates programmer error
		if (screeningSessionScreening == null)
			throw new IllegalStateException(format("Screening session ID %s does not have a current screening session screening.",
					screeningSessionId));

		// Get all the questions + answer options, filtering out any inapplicable questions (that is, if we have branching logic to skip over some)
		Set<UUID> inapplicableScreeningQuestionIds = findScreeningSessionInapplicableScreeningQuestionIdsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());
		List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions = findScreeningQuestionsWithAnswerOptionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId()).stream()
				.filter(screeningQuestionWithAnswerOptions -> !inapplicableScreeningQuestionIds.contains(screeningQuestionWithAnswerOptions.getScreeningQuestion().getScreeningQuestionId()))
				.collect(Collectors.toList());

		// Get all the questions that have already been answered for this session
		List<ScreeningSessionAnsweredScreeningQuestion> screeningSessionAnsweredScreeningQuestions = findScreeningSessionAnsweredScreeningQuestionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());

		// Holder for the next unanswered question (if there is one)
		ScreeningQuestionWithAnswerOptions nextScreeningQuestionWithAnswerOptions = null;

		// These lists are both ordered by question order, so figure out "next" by picking the last-answered index + 1
		if (screeningSessionAnsweredScreeningQuestions.size() < screeningQuestionsWithAnswerOptions.size())
			nextScreeningQuestionWithAnswerOptions = screeningQuestionsWithAnswerOptions.get(screeningSessionAnsweredScreeningQuestions.size());

		// If everything was already answered, nothing comes next for this screening session screening
		if (nextScreeningQuestionWithAnswerOptions == null)
			return Optional.empty();

		return Optional.of(new ScreeningQuestionContext(screeningSessionScreening, nextScreeningQuestionWithAnswerOptions.getScreeningQuestion(),
				nextScreeningQuestionWithAnswerOptions.getScreeningAnswerOptions()));
	}

	@Nonnull
	protected Optional<ScreeningSessionScreening> findCurrentScreeningSessionScreeningByScreeningSessionId(@Nullable UUID screeningSessionId) {
		if (screeningSessionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_screening_session_screening
				WHERE screening_session_id=? 
				ORDER BY screening_order DESC LIMIT 1
				""", ScreeningSessionScreening.class, screeningSessionId);
	}

	@Nonnull
	protected List<ScreeningQuestion> findScreeningQuestionsByScreeningSessionScreeningId(@Nullable UUID screeningSessionScreeningId) {
		if (screeningSessionScreeningId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT sq.*
				FROM screening_question sq, v_screening_session_screening sss
				WHERE sss.screening_session_screening_id=?
				AND sss.screening_version_id=sq.screening_version_id
				ORDER BY sq.display_order
				""", ScreeningQuestion.class, screeningSessionScreeningId);
	}

	@Nonnull
	protected List<ScreeningQuestionWithAnswerOptions> findScreeningQuestionsWithAnswerOptionsByScreeningSessionScreeningId(@Nullable UUID screeningSessionScreeningId) {
		if (screeningSessionScreeningId == null)
			return Collections.emptyList();

		List<ScreeningQuestion> screeningQuestions = getDatabase().queryForList("""
				SELECT sq.*
				FROM screening_question sq, v_screening_session_screening sss 
				WHERE sss.screening_session_screening_id=?
				AND sss.screening_version_id=sq.screening_version_id
				ORDER BY sq.display_order
				""", ScreeningQuestion.class, screeningSessionScreeningId);

		List<ScreeningAnswerOption> screeningAnswerOptions = getDatabase().queryForList("""
				SELECT sao.*
				FROM screening_answer_option sao, screening_question sq, v_screening_session_screening sss
				WHERE sao.screening_question_id=sq.screening_question_id
				AND sss.screening_version_id=sq.screening_version_id
				AND sss.screening_session_screening_id=?
				ORDER BY sao.display_order
				""", ScreeningAnswerOption.class, screeningSessionScreeningId);

		List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions = new ArrayList<>(screeningQuestions.size());

		// Group answer options by question
		for (ScreeningQuestion screeningQuestion : screeningQuestions) {
			List<ScreeningAnswerOption> screeningAnswerOptionsForQuestion = screeningAnswerOptions.stream()
					.filter(screeningAnswerOption -> screeningAnswerOption.getScreeningQuestionId().equals(screeningQuestion.getScreeningQuestionId()))
					.collect(Collectors.toList());

			screeningQuestionsWithAnswerOptions.add(new ScreeningQuestionWithAnswerOptions(screeningQuestion, screeningAnswerOptionsForQuestion));
		}

		return screeningQuestionsWithAnswerOptions;
	}

	@Nonnull
	protected Set<UUID> findScreeningSessionInapplicableScreeningQuestionIdsByScreeningSessionScreeningId(@Nullable UUID screeningSessionScreeningId) {
		if (screeningSessionScreeningId == null)
			return Collections.emptySet();

		return new HashSet<>(getDatabase().queryForList("""
				SELECT ssisq.screening_question_id
				FROM v_screening_session_inapplicable_screening_question ssisq, screening_question sq
				WHERE ssisq.screening_session_screening_id=?
				AND ssisq.screening_question_id=sq.screening_question_id
				""", UUID.class, screeningSessionScreeningId));
	}

	@Nonnull
	protected List<ScreeningSessionAnsweredScreeningQuestion> findScreeningSessionAnsweredScreeningQuestionsByScreeningSessionScreeningId(@Nullable UUID screeningSessionScreeningId) {
		if (screeningSessionScreeningId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT ssasq.*
				FROM v_screening_session_answered_screening_question ssasq, screening_question sq
				WHERE ssasq.screening_session_screening_id=?
				AND ssasq.screening_question_id=sq.screening_question_id
				ORDER BY sq.display_order
				""", ScreeningSessionAnsweredScreeningQuestion.class, screeningSessionScreeningId);
	}

	@Nonnull
	protected List<ScreeningAnswer> findScreeningAnswersAcrossAllQuestionsByScreeningSessionScreeningId(@Nullable UUID screeningSessionScreeningId) {
		if (screeningSessionScreeningId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT sa.*
				FROM v_screening_session_answered_screening_question ssasq, screening_question sq, screening_answer_option sao, v_screening_answer sa
				WHERE ssasq.screening_session_screening_id=?
				AND ssasq.screening_question_id=sq.screening_question_id
				AND sq.screening_question_id=sao.screening_question_id
				AND sa.screening_answer_option_id=sao.screening_answer_option_id
				AND sa.screening_session_answered_screening_question_id=ssasq.screening_session_answered_screening_question_id
				ORDER BY sa.created, sa.screening_answer_id
				""", ScreeningAnswer.class, screeningSessionScreeningId);
	}

	@Nonnull
	public List<ScreeningAnswer> findScreeningAnswersByScreeningQuestionContextId(@Nullable ScreeningQuestionContextId screeningQuestionContextId) {
		if (screeningQuestionContextId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT sa.*
				FROM v_screening_session_answered_screening_question ssasq, screening_question sq, screening_answer_option sao, v_screening_answer sa
				WHERE ssasq.screening_session_screening_id=?
				AND ssasq.screening_question_id=?
				AND ssasq.screening_question_id=sq.screening_question_id
				AND sq.screening_question_id=sao.screening_question_id
				AND sa.screening_answer_option_id=sao.screening_answer_option_id
				AND sa.screening_session_answered_screening_question_id=ssasq.screening_session_answered_screening_question_id
				ORDER BY sa.created, sa.screening_answer_id
				""", ScreeningAnswer.class, screeningQuestionContextId.getScreeningSessionScreeningId(), screeningQuestionContextId.getScreeningQuestionId());
	}

	@Nonnull
	protected List<ScreeningSessionScreening> findCurrentScreeningSessionScreeningsByScreeningSessionId(@Nullable UUID screeningSessionId) {
		if (screeningSessionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT * 
				FROM v_screening_session_screening
				WHERE screening_session_id=?
				ORDER BY screening_order
				""", ScreeningSessionScreening.class, screeningSessionId);
	}

	@Nonnull
	public Boolean hasPreviouslyAnsweredQuestionInScreeningSessionScreening(@Nullable UUID screeningQuestionId,
																																					@Nullable UUID screeningSessionScreeningId) {
		if (screeningQuestionId == null || screeningSessionScreeningId == null)
			return false;

		return getDatabase().queryForObject("""
				SELECT COUNT(*)
				FROM screening_session_answered_screening_question
				WHERE screening_session_screening_id=?
				AND screening_question_id=?
				""", Integer.class, screeningSessionScreeningId, screeningQuestionId).get() > 0;
	}

	@ThreadSafe
	public static class CreateScreeningAnswersResult {
		@Nonnull
		private final List<UUID> screeningAnswerIds;
		@Nonnull
		private final List<CreateScreeningAnswersMessage> messages;
		@Nonnull
		private final Map<UUID, CreateScreeningAnswersQuestionResult> questionResultsByScreeningAnswerOptionId;

		public CreateScreeningAnswersResult(@Nonnull List<UUID> screeningAnswerIds,
																				@Nonnull List<CreateScreeningAnswersMessage> messages,
																				@Nonnull Map<UUID, CreateScreeningAnswersQuestionResult> questionResultsByScreeningAnswerOptionId) {
			requireNonNull(screeningAnswerIds);
			requireNonNull(messages);
			requireNonNull(questionResultsByScreeningAnswerOptionId);

			this.screeningAnswerIds = Collections.unmodifiableList(new ArrayList<>(screeningAnswerIds));
			this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
			this.questionResultsByScreeningAnswerOptionId = Collections.unmodifiableMap(new HashMap<>(questionResultsByScreeningAnswerOptionId));
		}

		// Color of the border to draw around the answer option.
		// "DEFAULT" means the user didn't answer.
		// Other values mean that the user answered.
		public enum CreateScreeningAnswersDisplayTypeId {
			DEFAULT,
			PRIMARY,
			SECONDARY,
			SUCCESS,
			DANGER,
			WARNING,
			INFO,
			DARK,
			LIGHT
		}

		// Correctness is independent of what the user answered - an answer option is either correct or not.
		public enum CreateScreeningAnswersCorrectnessIndicatorId {
			CORRECT,
			INCORRECT
		}

		@ThreadSafe
		public static class CreateScreeningAnswersQuestionResult {
			@Nonnull
			private final CreateScreeningAnswersCorrectnessIndicatorId correctnessIndicatorId;
			@Nonnull
			private final CreateScreeningAnswersDisplayTypeId displayTypeId;

			public CreateScreeningAnswersQuestionResult(@Nonnull CreateScreeningAnswersCorrectnessIndicatorId correctnessIndicatorId,
																									@Nonnull CreateScreeningAnswersDisplayTypeId displayTypeId) {
				requireNonNull(correctnessIndicatorId);
				requireNonNull(displayTypeId);

				this.correctnessIndicatorId = correctnessIndicatorId;
				this.displayTypeId = displayTypeId;
			}

			@Nonnull
			public CreateScreeningAnswersCorrectnessIndicatorId getCorrectnessIndicatorId() {
				return this.correctnessIndicatorId;
			}

			@Nonnull
			public CreateScreeningAnswersDisplayTypeId getDisplayTypeId() {
				return this.displayTypeId;
			}
		}

		@ThreadSafe
		public static class CreateScreeningAnswersMessage {
			@Nonnull
			private final CreateScreeningAnswersDisplayTypeId displayTypeId;
			@Nonnull
			private final String title;
			@Nonnull
			private final String message; // Can include HTML

			public CreateScreeningAnswersMessage(@Nonnull CreateScreeningAnswersDisplayTypeId displayTypeId,
																					 @Nonnull String title,
																					 @Nonnull String message) {
				requireNonNull(displayTypeId);
				requireNonNull(title);
				requireNonNull(message);

				this.displayTypeId = displayTypeId;
				this.title = title;
				this.message = message;
			}

			@Nonnull
			public CreateScreeningAnswersDisplayTypeId getDisplayTypeId() {
				return this.displayTypeId;
			}

			@Nonnull
			public String getTitle() {
				return this.title;
			}

			@Nonnull
			public String getMessage() {
				return this.message;
			}
		}

		@Nonnull
		public List<UUID> getScreeningAnswerIds() {
			return this.screeningAnswerIds;
		}

		@Nonnull
		public List<CreateScreeningAnswersMessage> getMessages() {
			return this.messages;
		}

		@Nonnull
		public Map<UUID, CreateScreeningAnswersQuestionResult> getQuestionResultsByScreeningAnswerOptionId() {
			return this.questionResultsByScreeningAnswerOptionId;
		}
	}

	@Nonnull
	public CreateScreeningAnswersResult createScreeningAnswers(@Nullable CreateScreeningAnswersRequest request) {
		requireNonNull(request);

		ScreeningQuestionContextId screeningQuestionContextId = request.getScreeningQuestionContextId();
		List<CreateAnswerRequest> answers = request.getAnswers() == null ? List.of() : request.getAnswers().stream()
				.filter(answer -> answer != null)
				.collect(Collectors.toList());
		UUID createdByAccountId = request.getCreatedByAccountId();
		UUID screeningSessionScreeningId = null;
		UUID screeningQuestionId = null;
		ScreeningSessionScreening screeningSessionScreening = null;
		ScreeningQuestion screeningQuestion = null;
		List<ScreeningAnswerOption> screeningAnswerOptions = new ArrayList<>();
		Account createdByAccount = null;
		boolean force = request.getForce() == null ? false : request.getForce();
		String accountPhoneNumberToUpdate = null;
		ValidationException validationException = new ValidationException();

		if (screeningQuestionContextId == null) {
			validationException.add(new FieldError("screeningQuestionContextId", getStrings().get("Screening question context ID is required.")));
		} else {
			screeningSessionScreeningId = screeningQuestionContextId.getScreeningSessionScreeningId();
			screeningSessionScreening = findScreeningSessionScreeningById(screeningSessionScreeningId).orElse(null);

			if (screeningSessionScreening == null)
				validationException.add(new FieldError("screeningQuestionContextId", getStrings().get("Screening question context ID specifies an invalid screening session screening ID.")));

			screeningQuestionId = screeningQuestionContextId.getScreeningQuestionId();

			if (screeningQuestionId == null)
				validationException.add(new FieldError("screeningQuestionContextId", getStrings().get("Screening question context ID specifies an invalid screening question ID.")));
		}

		if (screeningQuestionId != null && screeningSessionScreening != null) {
			screeningQuestion = findScreeningQuestionById(screeningQuestionId).orElse(null);

			if (screeningQuestion == null) {
				validationException.add(new FieldError("screeningQuestionId", getStrings().get("Screening question ID is invalid.")));
			} else {
				ScreeningQuestion pinnedScreeningQuestion = screeningQuestion;

				if (answers.size() < screeningQuestion.getMinimumAnswerCount() || answers.size() > screeningQuestion.getMaximumAnswerCount()) {
					// Special case, friendlier message
					if (screeningQuestion.getMinimumAnswerCount() == 1 && screeningQuestion.getMaximumAnswerCount() == 1)
						validationException.add(new FieldError("answers", getStrings().get("You must answer the question to proceed.")));
					else if (answers.size() < screeningQuestion.getMinimumAnswerCount())
						validationException.add(new FieldError("answers", getStrings().get("You must choose at least {{minimumAnswerCount}} answers to proceed.", new HashMap<>() {{
							put("minimumAnswerCount", pinnedScreeningQuestion.getMinimumAnswerCount());
						}})));
					else if (answers.size() > screeningQuestion.getMaximumAnswerCount())
						validationException.add(new FieldError("answers", getStrings().get("You must choose at most {{maximumAnswerCount}} answers to proceed.", new HashMap<>() {{
							put("maximumAnswerCount", pinnedScreeningQuestion.getMaximumAnswerCount());
						}})));
					else
						validationException.add(new FieldError("answers", getStrings().get("You must choose {{minimumAnswerCount}}-{{maximumAnswerCount}} answers to proceed.", new HashMap<>() {{
							put("minimumAnswerCount", pinnedScreeningQuestion.getMinimumAnswerCount());
							put("maximumAnswerCount", pinnedScreeningQuestion.getMaximumAnswerCount());
						}})));
				} else {
					int i = 0;
					boolean illegalScreeningQuestionId = false;

					for (CreateAnswerRequest answer : answers) {
						UUID screeningAnswerOptionId = answer.getScreeningAnswerOptionId();
						String text = trimToNull(answer.getText());

						// Use the trimmed version for insert later
						answer.setText(text);

						if (screeningAnswerOptionId == null) {
							validationException.add(new FieldError(format("answers.screeningAnswerOptionId[%d]", i), getStrings().get("Screening answer option ID is required.")));
						} else {
							ScreeningAnswerOption screeningAnswerOption = findScreeningAnswerOptionById(screeningAnswerOptionId).orElse(null);

							if (screeningAnswerOption == null) {
								validationException.add(new FieldError(format("answers.screeningAnswerOptionId[%d]", i), getStrings().get("Screening answer option ID is invalid.")));
							} else if (screeningQuestion != null) {
								if (!screeningAnswerOption.getScreeningQuestionId().equals(screeningQuestionId))
									illegalScreeningQuestionId = true;

								if (screeningQuestion.getScreeningAnswerFormatId() == ScreeningAnswerFormatId.FREEFORM_TEXT) {
									if (text == null) {
										validationException.add(new FieldError("text", getStrings().get("Your response is required.")));
									} else {
										switch (screeningQuestion.getScreeningAnswerContentHintId()) {
											case PHONE_NUMBER -> {
												text = getNormalizer().normalizePhoneNumberToE164(text).orElse(null);

												if (text == null)
													validationException.add(new FieldError("text", getStrings().get("A valid phone number is required.")));
												else if (screeningQuestion.getMetadata() != null && Objects.equals(Boolean.TRUE, screeningQuestion.getMetadata().get("shouldUpdateAccountPhoneNumber")))
													accountPhoneNumberToUpdate = text;
											}
											case EMAIL_ADDRESS -> {
												text = getNormalizer().normalizeEmailAddress(text).orElse(null);

												if (!isValidEmailAddress(text))
													validationException.add(new FieldError("text", getStrings().get("A valid email address is required.")));
											}
										}
									}
								} else if (screeningQuestion.getScreeningAnswerFormatId() != ScreeningAnswerFormatId.FREEFORM_TEXT) {
									// Handle "supplement" (like a radio button with an "Other" option where people can type whatever they like)
									if (screeningAnswerOption.getFreeformSupplement()) {
										if (text == null)
											validationException.add(new FieldError("text", getStrings().get("Please specify a value for '{{screeningAnswerOption}}'.", new HashMap<String, Object>() {{
												put("screeningAnswerOption", screeningAnswerOption.getAnswerOptionText());
											}})));
									} else {
										// If we're not freeform text and this is NOT a supplement, clear out the text so we don't get odd
										// data in the DB in the event of a FE bug
										answer.setText(null);
									}
								}

								screeningAnswerOptions.add(screeningAnswerOption);
							}
						}

						++i;
					}

					if (illegalScreeningQuestionId)
						validationException.add(getStrings().get("You can only supply answers for the current question."));
				}
			}
		}

		if (createdByAccountId == null) {
			validationException.add(new FieldError("createdByAccountId", getStrings().get("Created by account ID is required.")));
		} else {
			createdByAccount = getAccountService().findAccountById(createdByAccountId).orElse(null);

			if (createdByAccount == null)
				validationException.add(new FieldError("createdByAccountId", getStrings().get("Created by account ID is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		ScreeningSession screeningSession = findScreeningSessionById(screeningSessionScreening.getScreeningSessionId()).get();

		if (accountPhoneNumberToUpdate != null) {
			getLogger().info("Setting phone number for account ID {} to {}...", screeningSession.getTargetAccountId(), accountPhoneNumberToUpdate);
			getDatabase().execute("UPDATE account SET phone_number=? WHERE account_id=?", accountPhoneNumberToUpdate, screeningSession.getTargetAccountId());
		}

		if (screeningSession.getCompleted())
			throw new ValidationException(getStrings().get("This assessment is complete and cannot have its answers changed."));

		UUID courseSessionId = screeningSession.getCourseSessionId();

		ScreeningVersion screeningVersion = findScreeningVersionById(screeningSessionScreening.getScreeningVersionId()).get();
		ScreeningFlowVersion screeningFlowVersion = findScreeningFlowVersionById(screeningSession.getScreeningFlowVersionId()).get();
		Institution institution = getInstitutionService().findInstitutionById(createdByAccount.getInstitutionId()).get();

		// Special failsafe checks for IC
		if (institution.getIntegratedCareEnabled() &&
				(institution.getIntegratedCareScreeningFlowId().equals(screeningFlowVersion.getScreeningFlowId())
						|| institution.getIntegratedCareIntakeScreeningFlowId().equals(screeningFlowVersion.getScreeningFlowId()))) {
			// Something is wrong if this doesn't exist, fail-fast with .get()
			PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(screeningSession.getPatientOrderId()).get();

			if (patientOrder.getPatientOrderDispositionId() != PatientOrderDisposition.PatientOrderDispositionId.OPEN) {
				getLogger().warn("Attempted to answer screening session ID {} for patient order ID {} in {} disposition",
						screeningSession.getScreeningSessionId(), patientOrder.getPatientOrderId(), patientOrder.getPatientOrderDispositionId().name());
				throw new ValidationException(getStrings().get("Sorry, this order is closed, so you cannot answer this question."),
						Map.of("shouldExitScreeningSession", true));
			} else if (patientOrder.getMostRecentIntakeScreeningSessionId() != null || patientOrder.getMostRecentScreeningSessionId() != null) {
				if (patientOrder.getMostRecentIntakeScreeningSessionId().equals(screeningSession.getScreeningSessionId())
						|| patientOrder.getMostRecentScreeningSessionId().equals(screeningSession.getScreeningSessionId())) {
					Account screeningSessionCreatedByAccount = getAccountService().findAccountById(screeningSession.getCreatedByAccountId()).get();

					// If a patient tries to answer a question for an assessment that's being performed on the patient's behalf by an MHIC,
					// prevent that from happening
					if (createdByAccount.getRoleId() == RoleId.PATIENT &&
							!createdByAccount.getAccountId().equals(screeningSessionCreatedByAccount.getAccountId())) {
						getLogger().warn("Attempted to answer screening session ID {} for patient order ID {}, but a patient can't answer a screening being worked on by an MHIC",
								screeningSession.getScreeningSessionId(), patientOrder.getPatientOrderId());
						throw new ValidationException(getStrings().get("Sorry, this assessment is currently being performed by a mental health specialist on your behalf.  You are not permitted to answer it."),
								Map.of("shouldExitScreeningSession", true));
					}
				} else {
					getLogger().warn("Attempted to answer out-of-date screening session ID {} for patient order ID {}",
							screeningSession.getScreeningSessionId(), patientOrder.getPatientOrderId());
					throw new ValidationException(getStrings().get("Sorry, this assessment is out-of-date, so you cannot answer this question."),
							Map.of("shouldExitScreeningSession", true));
				}
			}
		}

		// See if this question was already answered for this session...
		ScreeningSessionAnsweredScreeningQuestion screeningSessionAnsweredScreeningQuestion = getDatabase().queryForObject("""
				SELECT *
				FROM v_screening_session_answered_screening_question
				WHERE screening_session_screening_id=?
				AND screening_question_id=?
				""", ScreeningSessionAnsweredScreeningQuestion.class, screeningSessionScreeningId, screeningQuestionId).orElse(null);

		if (screeningSessionAnsweredScreeningQuestion != null) {
			// The question was already answered.
			// Mark all downstream answered questions, answers, and screenings in this session as invalid.
			getLogger().info("Screening session screening ID {} ({}) screening question ID {} was previously answered and is being answered again.",
					screeningSessionScreeningId, screeningVersion.getScreeningTypeId().name(), screeningQuestionId);

			// Mark downstream answered questions as invalid
			long downstreamQuestionsInvalidatedCount = getDatabase().execute("""
					UPDATE screening_session_answered_screening_question AS ssasq
					SET valid=FALSE
					FROM screening_session_screening sss
					WHERE sss.screening_session_id=?
					AND ssasq.valid=TRUE
					AND sss.screening_session_screening_id=ssasq.screening_session_screening_id
					AND ssasq.created >= ?""", screeningSession.getScreeningSessionId(), screeningSessionAnsweredScreeningQuestion.getCreated());

			// Mark downstream answers as invalid
			getDatabase().execute("""
					UPDATE screening_answer AS sa
					SET valid=FALSE
					FROM screening_session_answered_screening_question ssasq, screening_session_screening sss
					WHERE sa.screening_session_answered_screening_question_id=ssasq.screening_session_answered_screening_question_id
					AND sa.valid=TRUE
					AND ssasq.screening_session_screening_id=sss.screening_session_screening_id
					AND sss.screening_session_id=?
					AND sa.created >= ?""", screeningSession.getScreeningSessionId(), screeningSessionAnsweredScreeningQuestion.getCreated());

			// Mark downstream screenings as invalid
			long downstreamScreeningSessionScreeningsInvalidatedCount = getDatabase().execute("""
					UPDATE screening_session_screening
					SET valid=FALSE
					WHERE screening_session_id=?
					AND valid=TRUE
					AND screening_order > ?""", screeningSession.getScreeningSessionId(), screeningSessionScreening.getScreeningOrder());

			getLogger().info("Marked {} downstream answered question[s] as invalid and {} downstream screening session screening[s] as invalid.",
					downstreamQuestionsInvalidatedCount, downstreamScreeningSessionScreeningsInvalidatedCount);
		}

		UUID screeningSessionAnsweredScreeningQuestionId = UUID.randomUUID();

		try {
			getDatabase().execute("""
							INSERT INTO screening_session_answered_screening_question(screening_session_answered_screening_question_id,
							screening_session_screening_id, screening_question_id, created)
							VALUES(?,?,?,?)
					""", screeningSessionAnsweredScreeningQuestionId, screeningSessionScreeningId, screeningQuestionId, Instant.now());
		} catch (DatabaseException e) {
			String contraintViolated = e.constraint().orElse(null);

			if (Objects.equals(contraintViolated, "idx_screening_session_answered_screening_question_valid")) {
				getErrorReporter().report(format("Detected idx_screening_session_answered_screening_question_valid violation for screening_session_screening_id %s and screening_question_id %s",
						screeningSessionScreeningId, screeningQuestionId));

				throw new ValidationException(getStrings().get("Sorry, we were temporarily unable to record your answer. Please try again. If the issue persists, try exiting this assessment and re-launching it."));
			}

			throw e;
		}

		List<UUID> screeningAnswerIds = new ArrayList<>(answers.size());
		Instant now = Instant.now();

		// Batch up the answers...
		List<List<Object>> answerParameters =
				IntStream.range(0, answers.size())
						.mapToObj(i -> {
							CreateAnswerRequest answer = answers.get(i);

							// Generate a unique ID and record it for later use
							UUID screeningAnswerId = UUID.randomUUID();
							screeningAnswerIds.add(screeningAnswerId);

							// Create parameters list; note that answer.getText() might be null, so we can't use List.of(...)
							List<Object> parameters = new ArrayList<>();
							parameters.add(screeningAnswerId);
							parameters.add(answer.getScreeningAnswerOptionId());
							parameters.add(screeningSessionAnsweredScreeningQuestionId);
							parameters.add(createdByAccountId);
							parameters.add(answer.getText());
							parameters.add(now);
							parameters.add(i);

							return parameters;
						})
						.collect(Collectors.toList());

		try {
			// ...for an efficient INSERT.
			getDatabase().executeBatch("""
					INSERT INTO
					screening_answer(screening_answer_id, screening_answer_option_id, screening_session_answered_screening_question_id, created_by_account_id, text, created, answer_order)
					VALUES(?,?,?,?,?,?,?)
					""", answerParameters);
		} catch (DatabaseException e) {
			// Unfortunately this is the best way to find the `screening_answer_option_id_unique` violation
			boolean constraintViolated = e.getMessage() != null && e.getMessage().contains("screening_answer_option_id_unique");

			if (constraintViolated) {
				getErrorReporter().report(format("Detected screening_answer_option_id_unique violation for screening_session_screening_id %s and screening_question_id %s",
						screeningSessionScreeningId, screeningQuestionId));

				throw new ValidationException(getStrings().get("Sorry, we were temporarily unable to record your answer. Please try again. If the issue persists, try exiting this assessment and re-launching it."));
			}

			throw e;
		}

		// Score the individual screening by calling its scoring function
		List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions =
				findScreeningQuestionsWithAnswerOptionsByScreeningSessionScreeningId(screeningSessionScreeningId);
		List<ScreeningAnswer> screeningAnswers = findScreeningAnswersAcrossAllQuestionsByScreeningSessionScreeningId(screeningSessionScreeningId);

		List<ScreeningSessionAnsweredScreeningQuestion> screeningSessionAnsweredScreeningQuestions = getDatabase().queryForList("""
				SELECT *
				FROM v_screening_session_answered_screening_question
				WHERE screening_session_screening_id=?
				""", ScreeningSessionAnsweredScreeningQuestion.class, screeningSessionScreeningId);

		ScreeningScoringFunctionOutput screeningScoringFunctionOutput = executeScreeningScoringFunction(screeningQuestionId,
				screeningVersion.getScoringFunction(), screeningQuestionsWithAnswerOptions, screeningAnswers, screeningSessionAnsweredScreeningQuestions);

		getLogger().info("Screening session screening ID {} ({}) was scored {} with completed flag={}.", screeningSessionScreeningId,
				screeningVersion.getScreeningTypeId().name(), screeningScoringFunctionOutput.getScore(), screeningScoringFunctionOutput.getCompleted());

		// Always forget any downstream "inapplicable" questions in this screening to handle the case where
		// user backs up and answers a potentially-branching question that might lead to another branch.
		// This is similar to what we do above, where we discard downstream answers
		long downstreamQuestionsInvalidatedCount = getDatabase().execute("""
				UPDATE screening_session_inapplicable_screening_question AS ssisq
				SET valid=FALSE
				FROM screening_session_screening sss, screening_question sq
				WHERE sss.screening_session_id=?
				AND sq.screening_question_id=ssisq.screening_question_id
				AND sq.display_order > ?
				AND ssisq.valid=TRUE
				AND sss.screening_session_screening_id=ssisq.screening_session_screening_id
				""", screeningSession.getScreeningSessionId(), screeningQuestion.getDisplayOrder());

		if (downstreamQuestionsInvalidatedCount > 0)
			getLogger().info("Marked {} downstream inapplicable question[s] as invalid.", downstreamQuestionsInvalidatedCount);

		// We can branch within a screening if the scoring function returns an explicit "next" screening question ID.
		// If not provided, we just assume the next question (if any) in the natural display order is next.
		if (screeningScoringFunctionOutput.getNextScreeningQuestionId() != null) {
			getLogger().info("Screening session screening ID {} explicitly set next screening question ID to {}", screeningSessionScreeningId,
					screeningScoringFunctionOutput.getNextScreeningQuestionId());

			// Walk the screening questions for this screening and find all the questions between the question that was just answered and the
			// specified "next question".  Mark any question in that range as "inapplicable", because we are skipping over it.
			List<ScreeningQuestion> screeningQuestions = findScreeningQuestionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());
			List<ScreeningQuestion> screeningQuestionsToMarkInapplicable = new ArrayList<>(screeningQuestions.size());
			boolean canMarkInapplicable = false;

			for (ScreeningQuestion potentiallyInapplicableScreeningQuestion : screeningQuestions) {
				// We hit the current question - anything after this but before the specified "next question" will be marked inapplicable
				if (potentiallyInapplicableScreeningQuestion.getScreeningQuestionId().equals(screeningQuestionId)) {
					canMarkInapplicable = true;
					continue;
				}

				// We hit the specified "next question" - we're done looking for questions to mark inapplicable
				if (potentiallyInapplicableScreeningQuestion.getScreeningQuestionId().equals(screeningScoringFunctionOutput.getNextScreeningQuestionId()))
					break;

				if (canMarkInapplicable)
					screeningQuestionsToMarkInapplicable.add(potentiallyInapplicableScreeningQuestion);
			}

			getLogger().info("Marking {} downstream question[s] as inapplicable...", screeningQuestionsToMarkInapplicable.size());

			if (screeningQuestionsToMarkInapplicable.size() > 0) {
				// Batch up our parameters into a single INSERT to improve performance
				UUID pinnedScreeningSessionScreeningId = screeningSessionScreening.getScreeningSessionScreeningId();
				List<List<Object>> markInapplicableParameters = screeningQuestionsToMarkInapplicable.stream()
						.map(screeningQuestionToMarkInapplicable -> {
							return List.<Object>of(pinnedScreeningSessionScreeningId, screeningQuestionToMarkInapplicable.getScreeningQuestionId(), now);
						})
						.collect(Collectors.toList());

				getDatabase().executeBatch("""
						INSERT INTO
						screening_session_inapplicable_screening_question(screening_session_screening_id, screening_question_id, created)
						VALUES(?,?,?)
						""", markInapplicableParameters);
			}
		}

		// Based on screening scoring function output, set score/completed flags
		getDatabase().execute("""
						UPDATE screening_session_screening
						SET completed=?, score=CAST(? AS JSONB), below_scoring_threshold=?
						WHERE screening_session_screening_id=?
						""", screeningScoringFunctionOutput.getCompleted(),
				screeningScoringFunctionOutput.getScore().toJsonRepresentation(), screeningScoringFunctionOutput.getBelowScoringThreshold(),
				screeningSessionScreening.getScreeningSessionScreeningId());

		OrchestrationFunctionOutput orchestrationFunctionOutput = executeScreeningFlowOrchestrationFunction(screeningFlowVersion.getOrchestrationFunction(), screeningSession.getScreeningSessionId(), createdByAccount.getInstitutionId(), Map.of()).get();

		if (orchestrationFunctionOutput.getNextScreeningId() != null) {
			Integer nextScreeningOrder = getDatabase().queryForObject("""
					SELECT MAX (screening_order) + 1
					FROM screening_session_screening
					WHERE screening_session_id =?
					""", Integer.class, screeningSession.getScreeningSessionId()).get();

			Screening nextScreening = findScreeningById(orchestrationFunctionOutput.getNextScreeningId()).get();
			ScreeningVersion nextScreeningVersion = findScreeningVersionById(nextScreening.getActiveScreeningVersionId()).get();

			getLogger().info("Orchestration function for screening session screening ID {} ({}) indicates that we should transition to screening ID {} ({}).", screeningSessionScreeningId,
					screeningVersion.getScreeningTypeId().name(), nextScreening.getScreeningId(), nextScreeningVersion.getScreeningTypeId().name());

			getDatabase().execute("""
					INSERT INTO screening_session_screening(screening_session_id, screening_version_id, screening_order)
					VALUES( ?,?,?)
					""", screeningSession.getScreeningSessionId(), nextScreeningVersion.getScreeningVersionId(), nextScreeningOrder);
		}

		// If orchestration logic says we are in crisis, trigger crisis flow
		if (orchestrationFunctionOutput.getCrisisIndicated()) {
			if (screeningSession.getCrisisIndicated()) {
				getLogger().info("Orchestration function for screening session screening ID {} ({}) indicated crisis. " +
						"This session was already marked as having a crisis indicated, so no action needed.", screeningSessionScreeningId, screeningVersion.getScreeningTypeId().name());
			} else {
				getLogger().info("Orchestration function for screening session screening ID {} ({}) indicated crisis.  Creating crisis interaction instance...",
						screeningSessionScreeningId, screeningVersion.getScreeningTypeId().name());

				getDatabase().execute("UPDATE screening_session SET crisis_indicated=TRUE, crisis_indicated_at=NOW() WHERE screening_session_id=?", screeningSession.getScreeningSessionId());

				getInteractionService().createCrisisInteraction(screeningSession.getScreeningSessionId());
			}

			// If this screening session is done for a patient order, mark the order as "crisis indicated"
			if (screeningSession.getPatientOrderId() != null) {
				RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(screeningSession.getPatientOrderId()).get();

				if (patientOrder.getPatientOrderSafetyPlanningStatusId() != PatientOrderSafetyPlanningStatusId.NEEDS_SAFETY_PLANNING) {
					getLogger().info("Patient order ID {} will be marked as 'needs safety planning'.", patientOrder.getPatientOrderId());
					getDatabase().execute("""
							UPDATE patient_order
							SET patient_order_safety_planning_status_id=?
							WHERE patient_order_id=?
							""", PatientOrderSafetyPlanningStatusId.NEEDS_SAFETY_PLANNING, patientOrder.getPatientOrderId());

					boolean selfAdministered = getAccountService().findAccountById(screeningSession.getCreatedByAccountId()).get().getRoleId() == RoleId.PATIENT;

					// Notify any "crisis handlers" for this institution if a patient is self-screening and indicated crisis
					if (selfAdministered) {
						// First, get crisis handlers who are always notified for all orders
						List<PatientOrderCrisisHandler> allOrdersPatientOrderCrisisHandlers = getPatientOrderService().findPatientOrderCrisisHandlersForAllOrdersByInstitutionId(patientOrder.getInstitutionId());

						// Then, pick out any crisis handlers specifically assigned to this order's department
						List<PatientOrderCrisisHandler> epicDepartmentSpecificPatientOrderCrisisHandlers = getPatientOrderService().findPatientOrderCrisisHandlersSpecificallyForEpicDepartmentId(patientOrder.getEpicDepartmentId());

						// Combine them into a final list of crisis handlers to notify
						List<PatientOrderCrisisHandler> notifiablePatientOrderCrisisHandlers = new ArrayList<>(allOrdersPatientOrderCrisisHandlers.size() + epicDepartmentSpecificPatientOrderCrisisHandlers.size());
						notifiablePatientOrderCrisisHandlers.addAll(allOrdersPatientOrderCrisisHandlers);
						notifiablePatientOrderCrisisHandlers.addAll(epicDepartmentSpecificPatientOrderCrisisHandlers);

						getLogger().info("Notifying {} IC crisis handlers for institution ID {}...", notifiablePatientOrderCrisisHandlers.size(), institution.getInstitutionId());

						for (PatientOrderCrisisHandler patientOrderCrisisHandler : notifiablePatientOrderCrisisHandlers) {
							getLogger().info("Enqueuing IC crisis call message for {}...", patientOrderCrisisHandler.getPhoneNumber());

							Map<String, Object> messageContext = new HashMap<>();

							if (!configuration.isProduction())
								messageContext.put("additionalDetails", getStrings().get("This notification is for a test patient in a nonproduction environment."));

							CallMessage callMessage = new CallMessage.Builder(institution.getInstitutionId(), CallMessageTemplate.IC_CRISIS, patientOrderCrisisHandler.getPhoneNumber(), institution.getLocale())
									.messageContext(messageContext)
									.build();

							getMessageService().enqueueMessage(callMessage);
						}

						// Also automatically assign to the institution's designated safety manager, if one exists
						UUID integratedCareSafetyPlanningManagerAccountId = institution.getIntegratedCareSafetyPlanningManagerAccountId();

						// If there is a department-specific safety manager, assign that person instead.
						// If there are multiple department-specific safety managers, pick the first one we encounter.
						List<Account> epicDepartmentSafetyPlanningManagerAccounts = getPatientOrderService().findEpicDepartmentSafetyPlanningManagerAccountsByEpicDepartmentId(patientOrder.getEpicDepartmentId());

						if (epicDepartmentSafetyPlanningManagerAccounts.size() > 0) {
							Account epicDepartmentSafetyPlanningManagerAccount = epicDepartmentSafetyPlanningManagerAccounts.get(0);
							getLogger().info("Using specially-configured safety planning manager account {} for epic department ID {}...", epicDepartmentSafetyPlanningManagerAccount.getEmailAddress(), patientOrder.getEpicDepartmentId());
							integratedCareSafetyPlanningManagerAccountId = epicDepartmentSafetyPlanningManagerAccount.getAccountId();
						}

						if (integratedCareSafetyPlanningManagerAccountId != null) {
							Account serviceAccount = getAccountService().findServiceAccountByInstitutionId(institution.getInstitutionId()).get();
							getPatientOrderService().assignPatientOrderToPanelAccount(patientOrder.getPatientOrderId(), integratedCareSafetyPlanningManagerAccountId, serviceAccount.getAccountId());
						}
					}

					// TODO: write to patient order event table to keep track of when this happened
				}
			}
		}

		// If the orchestration function gave us an order closure reason, close out the order
		if (orchestrationFunctionOutput.getPatientOrderClosureReasonId() != null) {
			// Sanity check
			if (screeningSession.getPatientOrderId() == null)
				throw new IllegalStateException(format("Received %s.%s in orchestration function output, but we are not operating in the context of a patient order",
						PatientOrderClosureReasonId.class.getSimpleName(), orchestrationFunctionOutput.getPatientOrderClosureReasonId().name()));

			getLogger().info("Orchestrator said to close out patient order ID {} with reason {}", screeningSession.getPatientOrderId(), orchestrationFunctionOutput.getPatientOrderClosureReasonId().name());
			getPatientOrderService().closePatientOrder(new ClosePatientOrderRequest() {{
				setPatientOrderId(screeningSession.getPatientOrderId());
				setPatientOrderClosureReasonId(orchestrationFunctionOutput.getPatientOrderClosureReasonId());
				setAccountId(createdByAccountId);
			}});
		}

		// Course session specific: find the course unit with which this screening session's screening flow is associated, and mark it as completed.
		CourseUnit courseUnit = null;

		if (screeningSession.getCourseSessionId() != null) {
			CourseSession courseSession = getCourseService().findCourseSessionById(courseSessionId).orElse(null);

			if (courseSession == null) {
				getErrorReporter().report(format("Illegal Course Session ID %s was specified when attempting to answer a screening question", courseSessionId));
				throw new ValidationException(getStrings().get("Sorry, we were unable to record your answer."));
			}

			if (courseSession.getCourseSessionStatusId() != CourseSessionStatusId.IN_PROGRESS) {
				getErrorReporter().report(format("Course Session ID %s has status %s but attempted to answer a screening question", courseSessionId, courseSession.getCourseSessionStatusId().name()));
				throw new ValidationException(getStrings().get("This course is complete - no further answers are accepted."));
			}

			// There must be exactly one corresponding course unit - it's illegal to re-use the same screening flow for multiple units in the same course.
			courseUnit = getCourseService().findCourseUnitByCourseSessionIdAndScreeningQuestionId(courseSessionId, screeningQuestionId).get();

			getLogger().info("Answer[s] are for course unit ID {} ({})", courseUnit.getCourseUnitId(), courseUnit.getTitle());
		}
		getLogger().debug("orchestrationFunctionOutput.getCompleted() = " + orchestrationFunctionOutput.getCompleted());
		if (orchestrationFunctionOutput.getCompleted()) {
			boolean skipped = orchestrationFunctionOutput.getSkipped() != null && orchestrationFunctionOutput.getSkipped();

			getLogger().info("Orchestration function for screening session screening ID {} ({}) indicated that screening session ID {} is now complete.", screeningSessionScreeningId,
					screeningVersion.getScreeningTypeId().name(), screeningSession.getScreeningSessionId());

			// Course session specific completion handling
			if (screeningSession.getCourseSessionId() != null) {
				// If the orchestration function says that we have course modules to mark as optional for the current course session, then do so
				if (orchestrationFunctionOutput.getOptionalCourseModuleIdsToSet() != null
						&& orchestrationFunctionOutput.getOptionalCourseModuleIdsToSet().size() > 0) {
					for (UUID optionalCourseModuleId : orchestrationFunctionOutput.getOptionalCourseModuleIdsToSet()) {

						// Use a savepoint to safely avoid duplicate inserts and continue on with our txn
						Transaction transaction = getDatabase().currentTransaction().get();
						Savepoint savepoint = transaction.createSavepoint();

						try {
							getDatabase().execute("""
									INSERT INTO course_session_optional_module (
									  course_session_id,
									  course_module_id
									) VALUES (?,?)
									""", screeningSession.getCourseSessionId(), optionalCourseModuleId);
						} catch (DatabaseException e) {
							if ("course_session_optional_module_pkey".equals(e.constraint().orElse(null))) {
								getLogger().debug("Course session ID {} already has optional course module ID {}, no need to re-mark as optional.",
										screeningSession.getCourseSessionId(), optionalCourseModuleId);
								transaction.rollback(savepoint);
							} else {
								throw e;
							}
						}
					}
				}

				// If the orchestration function says that we have course modules to *un*mark as optional for the current course session, then do so
				if (orchestrationFunctionOutput.getOptionalCourseModuleIdsToClear() != null
						&& orchestrationFunctionOutput.getOptionalCourseModuleIdsToClear().size() > 0) {
					for (UUID optionalCourseModuleId : orchestrationFunctionOutput.getOptionalCourseModuleIdsToClear())
						getDatabase().execute("""
								DELETE FROM course_session_optional_module WHERE course_session_id=? AND course_module_id=?
								""", screeningSession.getCourseSessionId(), optionalCourseModuleId);
				}

				// Mark the unit as completed.
				// Use a savepoint to prevent duplicate insertions (you can complete a unit exactly once, but you can keep re-taking its screening flow many times).
				Transaction transaction = getDatabase().currentTransaction().get();
				Savepoint savepoint = transaction.createSavepoint();

				try {
					getDatabase().execute("""
							INSERT INTO course_session_unit (
							  course_session_id,
							  course_unit_id,
							  course_session_unit_status_id
							) VALUES (?,?,?)
							""", courseSessionId, courseUnit.getCourseUnitId(), CourseSessionUnitStatusId.COMPLETED);
				} catch (DatabaseException e) {
					if ("course_session_unit_pkey".equals(e.constraint().orElse(null))) {
						getLogger().debug("Course session ID {} is already completed, no need to re-mark as completed.",
								screeningSession.getCourseSessionId());
						transaction.rollback(savepoint);
					} else {
						throw e;
					}
				}

				getCourseService().checkAndSetCourseComplete(courseSessionId, courseUnit.getCourseUnitId());
			}

			// Special case: if we have a pre-completion screening confirmation prompt for this flow AND request did not indicate that we
			// should "force" the answer, throw a special exception that provides the confirmation prompt to display to the user
			// (if user accepts, request should specify to "force" the answer to indicate confirmation and skip this check).
			if (screeningFlowVersion.getPreCompletionScreeningConfirmationPromptId() != null && !force) {
				ValidationException screeningConfirmationPromptValidationException = new ValidationException(getStrings().get("Please confirm your answers before submitting."));

				// Frontend should look for this special metadata field "screeningConfirmationPrompt" and present the user with it
				ScreeningConfirmationPrompt screeningConfirmationPrompt = findScreeningConfirmationPromptById(screeningFlowVersion.getPreCompletionScreeningConfirmationPromptId()).get();
				screeningConfirmationPromptValidationException.setMetadata(Map.of("screeningConfirmationPrompt", getScreeningConfirmationPromptApiResponseFactory().create(screeningConfirmationPrompt)));

				throw screeningConfirmationPromptValidationException;
			}

			getDatabase().execute("UPDATE screening_session SET completed=TRUE, completed_at=NOW() WHERE screening_session_id=?", screeningSession.getScreeningSessionId());

			if (skipped) {
				getLogger().info("Orchestration function for screening session screening ID {} ({}) also indicated that screening session ID {} should be marked as 'skipped'.", screeningSessionScreeningId,
						screeningVersion.getScreeningTypeId().name(), screeningSession.getScreeningSessionId());

				skipScreeningSession(new SkipScreeningSessionRequest() {{
					setScreeningSessionId(screeningSession.getScreeningSessionId());
					setForceSkip(true);
				}});
			} else {
				getLogger().info("Now determining screening flow results for screening session screening ID {} ({})...", screeningSessionScreeningId,
						screeningVersion.getScreeningTypeId().name());

				// Execute results function and store off results
				ResultsFunctionOutput resultsFunctionOutput = executeScreeningFlowResultsFunction(screeningFlowVersion.getResultsFunction(),
						screeningSession.getScreeningSessionId(), createdByAccount.getInstitutionId()).get();

				// If this is a course session, apply the completion message (or null it out)
				if (courseSessionId != null) {
					UpdateCourseSessionUnitCompletionMessageRequest updateRequest = new UpdateCourseSessionUnitCompletionMessageRequest();
					updateRequest.setCourseSessionId(courseSessionId);
					updateRequest.setCourseUnitId(courseUnit.getCourseUnitId());
					updateRequest.setCompletionMessage(resultsFunctionOutput.getCourseSessionUnitCompletionMessage());

					getCourseService().updateCourseSessionUnitCompletionMessage(updateRequest);
				}

				// Ensure highest-weighted are first, so if we see duplicate support roles returned, we picked the highest-weight to persist
				Set<SupportRoleId> recommendedSupportRoleIds = new HashSet<>();
				List<SupportRoleRecommendation> sortedSupportRoleRecommendations = new ArrayList<>(resultsFunctionOutput.getSupportRoleRecommendations());
				sortedSupportRoleRecommendations.sort(Comparator.comparing(SupportRoleRecommendation::getSupportRoleId)
						.thenComparing(SupportRoleRecommendation::getWeight, Comparator.reverseOrder()));

				for (SupportRoleRecommendation supportRoleRecommendation : sortedSupportRoleRecommendations) {
					// If we see a duplicate support role, ignore it
					if (recommendedSupportRoleIds.contains(supportRoleRecommendation.getSupportRoleId()))
						continue;

					recommendedSupportRoleIds.add(supportRoleRecommendation.getSupportRoleId());

					getDatabase().execute("INSERT INTO screening_session_support_role_recommendation(screening_session_id, support_role_id, weight) " +
							"VALUES (?,?,?)", screeningSession.getScreeningSessionId(), supportRoleRecommendation.getSupportRoleId(), supportRoleRecommendation.getWeight());
				}

				// Legacy content check-off support
				// TODO: remove this once we have new tagging infrastructure
				if (resultsFunctionOutput.getRecommendLegacyContentAnswerIds()) {
					Assessment introAssessment = getDatabase().queryForObject("""
							SELECT a.*
							FROM assessment a, institution_assessment ia
							WHERE a.assessment_id=ia.assessment_id
							AND ia.institution_id=?
							AND a.assessment_type_id=?
							ORDER BY a.created DESC
							LIMIT 1
							""", Assessment.class, createdByAccount.getInstitutionId(), AssessmentTypeId.INTRO).orElse(null);

					if (introAssessment != null) {
						// Clear out any existing account sessions for this user
						List<AccountSession> introAssessmentAccountSessions = getDatabase().queryForList("""
								SELECT * FROM account_session
								WHERE account_id=? AND assessment_id=?
								""", AccountSession.class, screeningSession.getTargetAccountId(), introAssessment.getAssessmentId());

						for (AccountSession introAssessmentAccountSession : introAssessmentAccountSessions) {
							getDatabase().execute("""
											UPDATE account_session
											SET current_flag=FALSE
											WHERE account_session_id=?
											""",
									introAssessmentAccountSession.getAccountSessionId());
						}

						// Create a new account session with these recommended answers
						UUID newAccountSessionId = UUID.randomUUID();
						getDatabase().execute("""
								INSERT INTO account_session (account_session_id, account_id, assessment_id, 
								current_flag, complete_flag) VALUES (?,?,?,TRUE,TRUE)
								""", newAccountSessionId, screeningSession.getTargetAccountId(), introAssessment.getAssessmentId());

						for (UUID legacyContentAnswerId : resultsFunctionOutput.getLegacyContentAnswerIds()) {
							getDatabase().execute("INSERT INTO account_session_answer (account_session_answer_id, account_session_id, answer_id) VALUES (?,?,?)",
									UUID.randomUUID(), newAccountSessionId, legacyContentAnswerId);
						}
					}
				}

				// Store off recommended tags per this screening session's answers, if any
				for (String tagId : resultsFunctionOutput.getRecommendedTagIds()) {
					getDatabase().execute("INSERT INTO tag_screening_session (tag_id, screening_session_id) VALUES (?,?)",
							tagId, screeningSession.getScreeningSessionId());
				}

				// Store off recommended features per this screening session's answers, if any
				for (FeatureId featureId : resultsFunctionOutput.getRecommendedFeatureIds()) {
					getDatabase().execute("INSERT INTO screening_session_feature_recommendation(screening_session_id, feature_id) " +
							"VALUES (?,?)", screeningSession.getScreeningSessionId(), featureId);
				}

				// Special handling for IC intake flow
				if (institution.getIntegratedCareEnabled()
						&& Objects.equals(institution.getIntegratedCareIntakeScreeningFlowId(), screeningFlowVersion.getScreeningFlowId())) {
					PatientOrder patientOrder = getPatientOrderService().findPatientOrderByScreeningSessionId(screeningSession.getScreeningSessionId()).orElse(null);

					if (patientOrder == null) {
						getLogger().warn("No patient order for target account ID {} and screening session ID {}, ignoring intake results...", screeningSession.getTargetAccountId(), screeningSession.getScreeningSessionId());
					} else {
						PatientOrderIntakeLocationStatusId patientOrderIntakeLocationStatusId = patientOrder.getPatientOrderIntakeLocationStatusId();
						PatientOrderIntakeInsuranceStatusId patientOrderIntakeInsuranceStatusId = patientOrder.getPatientOrderIntakeInsuranceStatusId();
						PatientOrderIntakeWantsServicesStatusId patientOrderIntakeWantsServicesStatusId = patientOrder.getPatientOrderIntakeWantsServicesStatusId();
						PatientOrderConsentStatusId patientOrderConsentStatusId = patientOrder.getPatientOrderConsentStatusId();

						if (resultsFunctionOutput.getPatientOrderIntakeLocationStatusId() != null)
							patientOrderIntakeLocationStatusId = resultsFunctionOutput.getPatientOrderIntakeLocationStatusId();

						if (resultsFunctionOutput.getPatientOrderIntakeInsuranceStatusId() != null)
							patientOrderIntakeInsuranceStatusId = resultsFunctionOutput.getPatientOrderIntakeInsuranceStatusId();

						if (resultsFunctionOutput.getPatientOrderIntakeWantsServicesStatusId() != null)
							patientOrderIntakeWantsServicesStatusId = resultsFunctionOutput.getPatientOrderIntakeWantsServicesStatusId();

						if (resultsFunctionOutput.getPatientOrderConsentStatusId() != null)
							patientOrderConsentStatusId = resultsFunctionOutput.getPatientOrderConsentStatusId();

						getDatabase().execute("""
										UPDATE patient_order
										SET patient_order_intake_insurance_status_id=?,
										patient_order_intake_location_status_id=?,
										patient_order_intake_wants_services_status_id=?,
										patient_order_consent_status_id=?
										WHERE patient_order_id=?
										""", patientOrderIntakeInsuranceStatusId, patientOrderIntakeLocationStatusId,
								patientOrderIntakeWantsServicesStatusId, patientOrderConsentStatusId, patientOrder.getPatientOrderId());
					}
				}

				// Special handling for IC clinical flow
				if (institution.getIntegratedCareEnabled()
						&& Objects.equals(institution.getIntegratedCareScreeningFlowId(), screeningFlowVersion.getScreeningFlowId())) {
					RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderByScreeningSessionId(screeningSession.getScreeningSessionId()).orElse(null);

					if (patientOrder == null) {
						getLogger().warn("No patient order for target account ID {} and screening session ID {}, ignoring clinical results...", screeningSession.getTargetAccountId(), screeningSession.getScreeningSessionId());
					} else {
						CreatePatientOrderTriageGroupRequest patientOrderTriageGroupRequest = new CreatePatientOrderTriageGroupRequest();
						patientOrderTriageGroupRequest.setAccountId(screeningSession.getCreatedByAccountId());
						patientOrderTriageGroupRequest.setPatientOrderId(patientOrder.getPatientOrderId());
						patientOrderTriageGroupRequest.setPatientOrderTriageSourceId(PatientOrderTriageSourceId.COBALT);
						patientOrderTriageGroupRequest.setPatientOrderCareTypeId(resultsFunctionOutput.getIntegratedCareTriagedCareTypeId());
						patientOrderTriageGroupRequest.setScreeningSessionId(screeningSession.getScreeningSessionId());
						patientOrderTriageGroupRequest.setPatientOrderTriageOverrideReasonId(PatientOrderTriageOverrideReasonId.NOT_OVERRIDDEN);
						patientOrderTriageGroupRequest.setPatientOrderTriages(resultsFunctionOutput.getIntegratedCareTriages().stream()
								.map(integratedCareTriage -> {
									CreatePatientOrderTriageRequest createRequest = new CreatePatientOrderTriageRequest();
									createRequest.setPatientOrderFocusTypeId(integratedCareTriage.getPatientOrderFocusTypeId());
									createRequest.setPatientOrderCareTypeId(integratedCareTriage.getPatientOrderCareTypeId());
									createRequest.setReason(integratedCareTriage.getReason());

									return createRequest;
								})
								.collect(Collectors.toList()));

						getPatientOrderService().createPatientOrderTriageGroup(patientOrderTriageGroupRequest);

						// If topmost triage is specialty care, then mark the order as "needs resources"
						PatientOrder updatedPatientOrder = getPatientOrderService().findPatientOrderById(patientOrder.getPatientOrderId()).get();

						if (updatedPatientOrder.getPatientOrderCareTypeId() == PatientOrderCareTypeId.SPECIALTY) {
							getLogger().info("Triage results indicated specialty care, so marking order as 'needs resources'...");

							getPatientOrderService().updatePatientOrderResourcingStatus(new UpdatePatientOrderResourcingStatusRequest() {{
								setAccountId(screeningSession.getCreatedByAccountId());
								setPatientOrderId(patientOrder.getPatientOrderId());
								setPatientOrderResourcingStatusId(PatientOrderResourcingStatusId.NEEDS_RESOURCES);
							}});
						} else if (updatedPatientOrder.getPatientOrderCareTypeId() == PatientOrderCareTypeId.COLLABORATIVE) {
							// If patient was self-assessing and triage is collaborative, schedule a reminder message to book an appointment.
							// The reminder will be canceled if:
							//   * an appointment is scheduled
							//   * if the triage is changed to something other than collaborative
							//   * the order is closed or archived
							Account account = getAccountService().findAccountById(screeningSession.getCreatedByAccountId()).get();
							boolean selfAdministered = account.getRoleId() == RoleId.PATIENT;
							// Prevent self-referral IC institutions from scheduling appointment booking reminder messages
							boolean providerReferred = patientOrder.getPatientOrderReferralSourceId() == PatientOrderReferralSourceId.PROVIDER;
							// For now - disable appointment reminders.  We might re-enable in the future
							boolean shouldAutomaticallyScheduleAppointmentReminder = false;

							if (selfAdministered && providerReferred && shouldAutomaticallyScheduleAppointmentReminder) {
								LocalDateTime currentDateTime = LocalDateTime.now(institution.getTimeZone());
								LocalDate reminderScheduledAtDate = currentDateTime.toLocalDate().plusDays(1);
								LocalTime reminderScheduledAtTime = currentDateTime.toLocalTime();

								Set<MessageTypeId> messageTypeIds = new HashSet<>();
								if (updatedPatientOrder.getPatientEmailAddress() != null)
									messageTypeIds.add(MessageTypeId.EMAIL);
								if (updatedPatientOrder.getPatientPhoneNumber() != null)
									messageTypeIds.add(MessageTypeId.SMS);

								getPatientOrderService().createPatientOrderScheduledMessageGroup(new CreatePatientOrderScheduledMessageGroupRequest() {{
									setPatientOrderId(updatedPatientOrder.getPatientOrderId());
									setAccountId(account.getAccountId());
									setPatientOrderScheduledMessageTypeId(PatientOrderScheduledMessageTypeId.APPOINTMENT_BOOKING_REMINDER);
									setMessageTypeIds(messageTypeIds);
									setScheduledAtDate(reminderScheduledAtDate);
									setScheduledAtTimeAsLocalTime(reminderScheduledAtTime);
								}});
							}
						}
					}
				}
			}

			if (screeningSession.getAccountCheckInActionId() != null) {
				//This screening session is associated with a study check-in so mark this check-in complete
				UpdateCheckInAction updateCheckInActionRequest = new UpdateCheckInAction();
				updateCheckInActionRequest.setAccountCheckInActionId(screeningSession.getAccountCheckInActionId());
				updateCheckInActionRequest.setCheckInActionStatusId(CheckInActionStatusId.COMPLETE);

				getStudyService().updateAccountCheckInAction(createdByAccount, updateCheckInActionRequest);
			}
		}

		return new CreateScreeningAnswersResult(screeningAnswerIds, screeningScoringFunctionOutput.getMessages(), screeningScoringFunctionOutput.getQuestionResultsByScreeningAnswerOptionId());
	}

	@Nonnull
	public Optional<ScreeningSessionDestination> determineDestinationForScreeningSessionId(@Nullable UUID screeningSessionId) {
		return determineDestinationForScreeningSessionId(screeningSessionId, false);
	}

	@Nonnull
	public Optional<ScreeningSessionDestination> determineDestinationForScreeningSessionId(@Nullable UUID screeningSessionId,
																																												 @Nullable Boolean withSideEffects) {
		if (withSideEffects == null)
			withSideEffects = false;

		if (screeningSessionId == null)
			return Optional.empty();

		ScreeningSession screeningSession = findScreeningSessionById(screeningSessionId).orElse(null);

		if (screeningSession == null || !screeningSession.getCompleted())
			return Optional.empty();

		ScreeningFlowVersion screeningFlowVersion = findScreeningFlowVersionById(screeningSession.getScreeningFlowVersionId()).get();
		Account createdByAccount = getAccountService().findAccountById(screeningSession.getCreatedByAccountId()).get();
		Institution institution = getInstitutionService().findInstitutionById(createdByAccount.getInstitutionId()).get();

		// Extra data gets sent for special IC screening flows
		UUID patientOrderId = null;
		boolean integratedCareIntakeScreeningFlow = institution.getIntegratedCareEnabled() && Objects.equals(screeningFlowVersion.getScreeningFlowId(), institution.getIntegratedCareIntakeScreeningFlowId());
		boolean integratedCareClinicalScreeningFlow = institution.getIntegratedCareEnabled() && Objects.equals(screeningFlowVersion.getScreeningFlowId(), institution.getIntegratedCareScreeningFlowId());

		if (integratedCareIntakeScreeningFlow || integratedCareClinicalScreeningFlow) {
			RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderByScreeningSessionId(screeningSessionId).get();
			patientOrderId = patientOrder.getPatientOrderId();
		}

		Map<String, Object> additionalContext = new HashMap<>();
		additionalContext.put("patientOrderId", patientOrderId);

		DestinationFunctionOutput destinationFunctionOutput = executeScreeningFlowDestinationFunction(screeningFlowVersion.getDestinationFunction(),
				screeningSessionId, institution.getInstitutionId(), additionalContext).get();

		if (destinationFunctionOutput.getScreeningSessionDestinationId() == null)
			return Optional.empty();

		ScreeningSessionDestinationId screeningSessionDestinationId = destinationFunctionOutput.getScreeningSessionDestinationId();
		Map<String, Object> context = destinationFunctionOutput.getContext() == null ? new HashMap<>() : new HashMap<>(destinationFunctionOutput.getContext());
		ScreeningSessionDestination screeningSessionDestination = new ScreeningSessionDestination(screeningSessionDestinationId, context);

		if (withSideEffects) {
			// Special handling for IC intake screening flow; create the clinical screening flow immediately after completing and
			// route to it as the destination
			if (integratedCareIntakeScreeningFlow &&
					(screeningSessionDestinationId == ScreeningSessionDestinationId.IC_PATIENT_CLINICAL_SCREENING
							|| screeningSessionDestinationId == ScreeningSessionDestinationId.IC_MHIC_CLINICAL_SCREENING)) {

				getLogger().info("Because the IC Intake screening flow was completed and we're supposed to transition to clinical, immediately create the clinical one...");

				ScreeningFlow icClinicalScreeningFlow = findScreeningFlowById(institution.getIntegratedCareScreeningFlowId()).get();

				CreateScreeningSessionRequest request = new CreateScreeningSessionRequest();
				request.setScreeningFlowId(icClinicalScreeningFlow.getScreeningFlowId());
				request.setPatientOrderId(patientOrderId);
				request.setTargetAccountId(screeningSession.getTargetAccountId());
				request.setCreatedByAccountId(screeningSession.getCreatedByAccountId());
				request.setMetadata(screeningSession.getMetadata()); // Carry over metadata from the intake screening session

				UUID icClinicalScreeningSessionId = createScreeningSession(request);

				ScreeningQuestionContext nextScreeningQuestionContext =
						findNextUnansweredScreeningQuestionContextByScreeningSessionId(icClinicalScreeningSessionId).orElse(null);

				ScreeningQuestionContextId nextScreeningQuestionContextId = nextScreeningQuestionContext.getScreeningQuestionContextId();
				context.put("nextScreeningQuestionContextId", nextScreeningQuestionContextId);
			} else if (screeningSession.getGroupSessionId() != null
					&& screeningSessionDestination.getScreeningSessionDestinationResultId() == ScreeningSessionDestinationResultId.SUCCESS) {
				// Special handling for group session intake screening flows when they succeed - book a reservation.
				// Walk all of the answers to the intake and pick out the answer that's for an email-address question.
				// We assume it's programmer error to specify a group session intake that does not have exactly one email address question
				String emailAddress = null;
				ScreeningSessionResult screeningSessionResult = findScreeningSessionResult(screeningSession).get();

				for (ScreeningSessionScreeningResult screeningSessionScreeningResult : screeningSessionResult.getScreeningSessionScreeningResults()) {
					for (ScreeningQuestionResult screeningQuestionResult : screeningSessionScreeningResult.getScreeningQuestionResults()) {
						if (screeningQuestionResult.getScreeningAnswerFormatId() == ScreeningAnswerFormatId.FREEFORM_TEXT
								&& screeningQuestionResult.getScreeningAnswerContentHintId() == ScreeningAnswerContentHintId.EMAIL_ADDRESS) {
							for (ScreeningAnswerResult screeningAnswerResult : screeningQuestionResult.getScreeningAnswerResults()) {
								if (emailAddress != null)
									throw new IllegalStateException(format("There are multiple answers that provide an email address for group session ID %s intake (screening session ID %s), not sure which one to use.",
											screeningSession.getGroupSessionId(), screeningSession.getScreeningSessionId()));

								emailAddress = screeningAnswerResult.getText();
							}
						}
					}
				}

				if (emailAddress == null)
					throw new IllegalStateException(format("There is no answer that provides an email address for group session ID %s intake (screening session ID %s).",
							screeningSession.getGroupSessionId(), screeningSession.getScreeningSessionId()));

				CreateGroupSessionReservationRequest request = new CreateGroupSessionReservationRequest();
				request.setAccountId(createdByAccount.getAccountId());
				request.setGroupSessionId(screeningSession.getGroupSessionId());
				request.setEmailAddress(emailAddress);

				getGroupSessionService().createGroupSessionReservation(request, createdByAccount);
			}
		}

		return Optional.of(new ScreeningSessionDestination(screeningSessionDestinationId, context));
	}

	@Nonnull
	public List<ScreeningSession> findScreeningSessionsByPatientOrderIdAndScreeningFlowTypeId(@Nullable UUID patientOrderId,
																																														@Nullable ScreeningFlowTypeId screeningFlowTypeId) {
		if (patientOrderId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT ss.*
				FROM screening_session ss, screening_flow_version sfv, screening_flow sf
				WHERE ss.patient_order_id=?
				AND ss.screening_flow_version_id=sfv.screening_flow_version_id
				AND sfv.screening_flow_id = sf.screening_flow_id
				AND sf.screening_flow_type_id=?
				ORDER BY ss.created DESC
				""", ScreeningSession.class, patientOrderId, screeningFlowTypeId);
	}

	@Nonnull
	protected ScreeningScoringFunctionOutput executeScreeningScoringFunction(@Nonnull UUID screeningQuestionId,
																																					 @Nonnull String screeningScoringFunctionJavascript,
																																					 @Nonnull List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions,
																																					 @Nonnull List<ScreeningAnswer> screeningAnswers,
																																					 @Nonnull List<ScreeningSessionAnsweredScreeningQuestion> screeningSessionAnsweredScreeningQuestions) {
		requireNonNull(screeningQuestionId);
		requireNonNull(screeningScoringFunctionJavascript);
		requireNonNull(screeningQuestionsWithAnswerOptions);
		requireNonNull(screeningAnswers);
		requireNonNull(screeningSessionAnsweredScreeningQuestions);

		int answeredScreeningQuestionCount = screeningSessionAnsweredScreeningQuestions.size();

		Set<UUID> answeredScreeningQuestionIds = screeningSessionAnsweredScreeningQuestions.stream()
				.map(screeningSessionAnsweredScreeningQuestion -> screeningSessionAnsweredScreeningQuestion.getScreeningQuestionId())
				.collect(Collectors.toSet());

		ScreeningScoringFunctionOutput screeningScoringFunctionOutput;

		// Massage data a bit to make it easier for function to get a screening answer option given a screening answer ID
		Map<UUID, ScreeningAnswerOption> screeningAnswerOptionsById = new HashMap<>();
		Map<UUID, ScreeningAnswerOption> screeningAnswerOptionsByScreeningAnswerId = new HashMap<>(screeningAnswers.size());
		Map<String, UUID> screeningQuestionIdsByQuestionText = new HashMap<>(screeningQuestionsWithAnswerOptions.size());
		Map<UUID, Set<UUID>> screeningAnswerIdsByScreeningQuestionId = new HashMap<>(screeningQuestionsWithAnswerOptions.size());

		for (ScreeningQuestionWithAnswerOptions screeningQuestionWithAnswerOptions : screeningQuestionsWithAnswerOptions)
			for (ScreeningAnswerOption screeningAnswerOption : screeningQuestionWithAnswerOptions.getScreeningAnswerOptions())
				screeningAnswerOptionsById.put(screeningAnswerOption.getScreeningAnswerOptionId(), screeningAnswerOption);

		for (ScreeningAnswer screeningAnswer : screeningAnswers)
			screeningAnswerOptionsByScreeningAnswerId.put(screeningAnswer.getScreeningAnswerId(), screeningAnswerOptionsById.get(screeningAnswer.getScreeningAnswerOptionId()));

		for (ScreeningQuestionWithAnswerOptions screeningQuestionWithAnswerOptions : screeningQuestionsWithAnswerOptions)
			if (screeningQuestionWithAnswerOptions.getScreeningQuestion().getQuestionText() != null)
				screeningQuestionIdsByQuestionText.put(screeningQuestionWithAnswerOptions.getScreeningQuestion().getQuestionText(), screeningQuestionWithAnswerOptions.getScreeningQuestion().getScreeningQuestionId());

		// Nested loops, but they are generally quite small...
		for (ScreeningAnswer screeningAnswer : screeningAnswers) {
			ScreeningAnswerOption screeningAnswerOption = screeningAnswerOptionsByScreeningAnswerId.get(screeningAnswer.getScreeningAnswerId());

			for (ScreeningQuestionWithAnswerOptions screeningQuestionWithAnswerOptions : screeningQuestionsWithAnswerOptions) {
				for (ScreeningAnswerOption potentialScreeningAnswerOption : screeningQuestionWithAnswerOptions.getScreeningAnswerOptions()) {
					if (potentialScreeningAnswerOption.getScreeningAnswerOptionId().equals(screeningAnswerOption.getScreeningAnswerOptionId())) {
						Set<UUID> screeningAnswerIds = screeningAnswerIdsByScreeningQuestionId.get(screeningQuestionWithAnswerOptions.getScreeningQuestion().getScreeningQuestionId());

						if (screeningAnswerIds == null) {
							screeningAnswerIds = new HashSet<>();
							screeningAnswerIdsByScreeningQuestionId.put(screeningQuestionWithAnswerOptions.getScreeningQuestion().getScreeningQuestionId(), screeningAnswerIds);
						}

						screeningAnswerIds.add(screeningAnswer.getScreeningAnswerId());
					}
				}
			}
		}

		Map<String, Object> context = new HashMap<>();
		context.put("screeningQuestionId", screeningQuestionId);
		context.put("screeningQuestionsWithAnswerOptions", screeningQuestionsWithAnswerOptions);
		context.put("screeningAnswers", screeningAnswers);
		context.put("screeningAnswerOptionsByScreeningAnswerId", screeningAnswerOptionsByScreeningAnswerId);
		context.put("answeredScreeningQuestionCount", answeredScreeningQuestionCount);
		context.put("answeredScreeningQuestionIds", answeredScreeningQuestionIds);
		context.put("screeningQuestionIdsByQuestionText", screeningQuestionIdsByQuestionText);
		context.put("screeningAnswerIdsByScreeningQuestionId", screeningAnswerIdsByScreeningQuestionId);

		try {
			screeningScoringFunctionOutput = getJavascriptExecutor().execute(screeningScoringFunctionJavascript, context, ScreeningScoringFunctionOutput.class);
		} catch (JavascriptExecutionException e) {
			throw new RuntimeException(e);
		}

		if (screeningScoringFunctionOutput.getCompleted() == null)
			throw new IllegalStateException("Screening scoring function must provide a 'completed' value in output");

		if (screeningScoringFunctionOutput.getScore() == null)
			throw new IllegalStateException("Screening scoring function must provide a 'score' value in output");

		if (screeningScoringFunctionOutput.getCompleted() == null && screeningScoringFunctionOutput.getNextScreeningQuestionId() != null)
			throw new IllegalStateException("Screening scoring function must cannot indicate it is complete and indicate a 'next question' at the same time");

		if (screeningScoringFunctionOutput.getMessages() == null)
			screeningScoringFunctionOutput.setMessages(List.of());

		if (screeningScoringFunctionOutput.getQuestionResultsByScreeningAnswerOptionId() == null)
			screeningScoringFunctionOutput.setQuestionResultsByScreeningAnswerOptionId(Map.of());

		return screeningScoringFunctionOutput;
	}

	@Nonnull
	protected Optional<OrchestrationFunctionOutput> executeScreeningFlowOrchestrationFunction(@Nonnull String screeningFlowOrchestrationFunctionJavascript,
																																														@Nullable UUID screeningSessionId,
																																														@Nullable InstitutionId institutionId,
																																														@Nullable Map<String, Object> additionalContext) {
		requireNonNull(screeningFlowOrchestrationFunctionJavascript);

		if (screeningSessionId == null || institutionId == null)
			return Optional.empty();

		OrchestrationFunctionOutput orchestrationFunctionOutput = executeScreeningFlowFunction(screeningFlowOrchestrationFunctionJavascript,
				OrchestrationFunctionOutput.class, screeningSessionId, institutionId, additionalContext).orElse(null);

		if (orchestrationFunctionOutput == null)
			return Optional.empty();

		if (orchestrationFunctionOutput.getCompleted() == null)
			throw new IllegalStateException("Screening flow orchestration function must provide a 'completed' value in output");

		if (orchestrationFunctionOutput.getCrisisIndicated() == null)
			orchestrationFunctionOutput.setCrisisIndicated(false);

		if (orchestrationFunctionOutput.getCompleted() && orchestrationFunctionOutput.getNextScreeningId() != null)
			throw new IllegalStateException(format("Screening flow orchestration function output says this screening session is completed, but also provides a nonnull 'nextScreeningId' value"));

		return Optional.of(orchestrationFunctionOutput);
	}

	@Nonnull
	protected Optional<ResultsFunctionOutput> executeScreeningFlowResultsFunction(@Nonnull String screeningFlowResultsFunctionJavascript,
																																								@Nullable UUID screeningSessionId,
																																								@Nullable InstitutionId institutionId) {
		requireNonNull(screeningFlowResultsFunctionJavascript);

		if (screeningSessionId == null || institutionId == null)
			return Optional.empty();

		ResultsFunctionOutput resultsFunctionOutput = executeScreeningFlowFunction(screeningFlowResultsFunctionJavascript,
				ResultsFunctionOutput.class, screeningSessionId, institutionId, Map.of()).orElse(null);

		if (resultsFunctionOutput == null)
			return Optional.empty();

		if (resultsFunctionOutput.getSupportRoleRecommendations() == null)
			resultsFunctionOutput.setSupportRoleRecommendations(List.of());

		// Normalize data a bit
		if (resultsFunctionOutput.getRecommendLegacyContentAnswerIds() == null)
			resultsFunctionOutput.setRecommendLegacyContentAnswerIds(false);

		if (resultsFunctionOutput.getLegacyContentAnswerIds() == null)
			resultsFunctionOutput.setLegacyContentAnswerIds(Collections.emptySet());

		if (resultsFunctionOutput.getRecommendedTagIds() == null)
			resultsFunctionOutput.setRecommendedTagIds(Set.of());

		if (resultsFunctionOutput.getRecommendedFeatureIds() == null)
			resultsFunctionOutput.setRecommendedFeatureIds(Set.of());

		if (resultsFunctionOutput.getIntegratedCareTriages() == null)
			resultsFunctionOutput.setIntegratedCareTriages(List.of());

		return Optional.of(resultsFunctionOutput);
	}

	@Nonnull
	protected Optional<DestinationFunctionOutput> executeScreeningFlowDestinationFunction(@Nonnull String screeningFlowDestinationFunctionJavascript,
																																												@Nullable UUID screeningSessionId,
																																												@Nullable InstitutionId institutionId,
																																												@Nullable Map<String, Object> additionalContext) {
		requireNonNull(screeningFlowDestinationFunctionJavascript);

		if (screeningSessionId == null || institutionId == null)
			return Optional.empty();

		DestinationFunctionOutput destinationFunctionOutput = executeScreeningFlowFunction(screeningFlowDestinationFunctionJavascript,
				DestinationFunctionOutput.class, screeningSessionId, institutionId, additionalContext).orElse(null);

		if (destinationFunctionOutput == null)
			return Optional.empty();

		return Optional.of(destinationFunctionOutput);
	}

	@Nonnull
	protected <T> Optional<T> executeScreeningFlowFunction(@Nonnull String screeningFlowFunctionJavascript,
																												 @Nonnull Class<T> screeningFlowFunctionResultType,
																												 @Nullable UUID screeningSessionId,
																												 @Nullable InstitutionId institutionId,
																												 @Nullable Map<String, Object> additionalContext) {
		requireNonNull(screeningFlowFunctionJavascript);

		if (screeningSessionId == null || institutionId == null)
			return Optional.empty();

		T screeningFlowFunctionResult;

		ScreeningSession screeningSession = findScreeningSessionById(screeningSessionId).orElse(null);

		if (screeningSession == null)
			return Optional.empty();

		List<Screening> screenings = findScreeningsByInstitutionId(institutionId);

		if (screenings.size() == 0)
			return Optional.empty();

		// Massage data a bit to make it easier for function to get a handle to a screening by using its name
		Map<String, Screening> screeningsByName = new HashMap<>(screenings.size());

		for (Screening screening : screenings)
			screeningsByName.put(screening.getName(), screening);

		// Set up data so it's easy to access questions/answers to make decisions, look at scores, etc.
		//
		// Example:
		//
		// const phq9Questions = input.screeningQuestionsByScreeningSessionScreeningId[phq9.screeningSessionScreeningId];
		// const phq9Question9 = phq9Questions[8];
		//
		// // The screeningResponses array is only present if the user has answered the question in some way
		// console.log(phq9Question9.screeningResponses[0].screeningAnswerOption.score); // easy access to the score for the selected answer option
		// console.log(phq9Question9.screeningResponses[0].screeningAnswer.text); // the answers themselves, in case you need them (e.g. free-form text)
		List<ScreeningSessionScreening> screeningSessionScreenings = findCurrentScreeningSessionScreeningsByScreeningSessionId(screeningSessionId);
		Map<UUID, Object> screeningResultsByScreeningSessionScreeningId = new HashMap<>();

		// We could do this as a single query, but the dataset is small, and this is a little clearer and fast enough
		for (ScreeningSessionScreening screeningSessionScreening : screeningSessionScreenings) {
			List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions = findScreeningQuestionsWithAnswerOptionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());
			List<ScreeningAnswer> screeningAnswers = findScreeningAnswersAcrossAllQuestionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());
			Map<UUID, ScreeningAnswer> screeningAnswersByAnswerOptionId = new HashMap<>(screeningAnswers.size());

			for (ScreeningAnswer screeningAnswer : screeningAnswers)
				screeningAnswersByAnswerOptionId.put(screeningAnswer.getScreeningAnswerOptionId(), screeningAnswer);

			List<Map<String, Object>> screeningResults = new ArrayList<>();

			for (ScreeningQuestionWithAnswerOptions screeningQuestionsWithAnswerOption : screeningQuestionsWithAnswerOptions) {
				Map<String, Object> screeningResult = new HashMap<>();

				ScreeningQuestion screeningQuestion = screeningQuestionsWithAnswerOption.getScreeningQuestion();
				List<ScreeningAnswerOption> screeningAnswerOptions = screeningQuestionsWithAnswerOption.getScreeningAnswerOptions();
				Map<UUID, ScreeningAnswerOption> screeningAnswerOptionsById = new HashMap<>(screeningAnswerOptions.size());

				for (ScreeningAnswerOption screeningAnswerOption : screeningAnswerOptions)
					screeningAnswerOptionsById.put(screeningAnswerOption.getScreeningAnswerOptionId(), screeningAnswerOption);

				screeningResult.put("screeningQuestionId", screeningQuestion.getScreeningQuestionId());
				screeningResult.put("screeningQuestionDisplayOrder", screeningQuestion.getDisplayOrder());
				screeningResult.put("screeningAnswerFormatId", screeningQuestion.getScreeningAnswerFormatId());
				screeningResult.put("screeningAnswerContentHintId", screeningQuestion.getScreeningAnswerContentHintId());
				screeningResult.put("questionText", screeningQuestion.getQuestionText());
				screeningResult.put("minimumAnswerCount", screeningQuestion.getMinimumAnswerCount());
				screeningResult.put("maximumAnswerCount", screeningQuestion.getMaximumAnswerCount());
				screeningResult.put("metadata", screeningQuestion.getMetadata());

				// Each element in this list is a screeningAnswerOption and screeningAnswer pair
				List<Map<String, Object>> screeningResponses = new ArrayList<>();

				for (ScreeningAnswer screeningAnswer : screeningAnswers) {
					if (screeningAnswerOptionsById.keySet().contains(screeningAnswer.getScreeningAnswerOptionId())) {
						ScreeningAnswerOption screeningAnswerOption = screeningAnswerOptionsById.get(screeningAnswer.getScreeningAnswerOptionId());

						Map<String, Object> screeningAnswerOptionJson = new HashMap<>(4);
						screeningAnswerOptionJson.put("screeningAnswerOptionId", screeningAnswerOption.getScreeningAnswerOptionId());
						screeningAnswerOptionJson.put("answerOptionText", screeningAnswerOption.getAnswerOptionText());
						screeningAnswerOptionJson.put("indicatesCrisis", screeningAnswerOption.getIndicatesCrisis());
						screeningAnswerOptionJson.put("score", screeningAnswerOption.getScore());
						screeningAnswerOptionJson.put("metadata", screeningAnswerOption.getMetadata());

						Map<String, Object> screeningAnswerJson = new HashMap<>(2);
						screeningAnswerJson.put("screeningAnswerId", screeningAnswer.getScreeningAnswerId());
						screeningAnswerJson.put("text", screeningAnswer.getText());

						Map<String, Object> screeningResponse = new HashMap<>(2);
						screeningResponse.put("screeningAnswerOption", screeningAnswerOptionJson);
						screeningResponse.put("screeningAnswer", screeningAnswerJson);

						screeningResponses.add(screeningResponse);
					}
				}

				screeningResult.put("screeningResponses", screeningResponses);
				screeningResults.add(screeningResult);
			}

			screeningResultsByScreeningSessionScreeningId.put(screeningSessionScreening.getScreeningSessionScreeningId(), screeningResults);
		}

		// Pulls screening name and corresponding screening version ID for all screening session screenings in this context.
		// Useful for JS to get a handle to screening version IDs in a human-readable way
		List<UUID> screeningVersionIds = screeningSessionScreenings.stream()
				.map(screeningSessionScreening -> screeningSessionScreening.getScreeningVersionId())
				.collect(Collectors.toList());

		List<ScreeningVersionName> screeningVersionNames = screeningSessionScreenings.size() == 0 ? List.of() : getDatabase().queryForList(format("""
				SELECT s.name, sv.screening_version_id
				FROM screening s, screening_version sv
				WHERE s.screening_id=sv.screening_id
				AND sv.screening_version_id IN %s
				""", sqlInListPlaceholders(screeningVersionIds)), ScreeningVersionName.class, screeningVersionIds.toArray(new Object[]{}));

		Map<String, UUID> screeningVersionIdsByName = new HashMap<>(screeningVersionNames.size());

		for (ScreeningVersionName screeningVersionName : screeningVersionNames) {
			if (screeningVersionIdsByName.containsKey(screeningVersionName.getName()))
				throw new IllegalStateException(format("""
						There are multiple versions of the same screening name within a screening session.\n
						Name: %s\n
						Versions: %s, %s
						""", screeningVersionName.getName(), screeningVersionName.getScreeningVersionId(), screeningVersionIdsByName.get(screeningVersionName.getName())).trim());

			screeningVersionIdsByName.put(screeningVersionName.getName(), screeningVersionName.getScreeningVersionId());
		}

		boolean selfAdministered = isScreeningSessionSelfAdministered(screeningSession);

		Map<String, Object> context = new HashMap<>();

		// Patient age can help determine how to orchestrate, e.g. only perform a particular screening if
		// patient is below a certain age
		if (screeningSession.getPatientOrderId() != null) {
			Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
			RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(screeningSession.getPatientOrderId()).get();
			LocalDate currentDate = LocalDateTime.ofInstant(Instant.now(), institution.getTimeZone()).toLocalDate();

			Long patientAgeInYears = patientOrder.getPatientBirthdate() == null
					? null : Period.between(patientOrder.getPatientBirthdate(), currentDate).get(ChronoUnit.YEARS);

			context.put("patientAgeInYears", patientAgeInYears);
			context.put("patientBirthSexId", patientOrder.getPatientBirthSexId());

			// Self-administered value should be false in the special case where an MHIC performs a screening
			// but there is no target account (i.e. patient never signed in/created account).
			// So we assume self-administered only in the case where the created-by account is a patient
			selfAdministered = getAccountService().findAccountById(screeningSession.getCreatedByAccountId()).get().getRoleId() == RoleId.PATIENT;

			// Expose some fields that could be used to aid logic/triage
			context.put("patientOrderIntakeLocationStatusId", patientOrder.getPatientOrderIntakeLocationStatusId());
			context.put("patientOrderIntakeInsuranceStatusId", patientOrder.getPatientOrderIntakeInsuranceStatusId());
			context.put("patientOrderIntakeWantsServicesStatusId", patientOrder.getPatientOrderIntakeWantsServicesStatusId());
			context.put("patientOrderConsentStatusId", patientOrder.getPatientOrderConsentStatusId());

			Set<PatientOrderReferralReasonId> patientOrderReferralReasonIds = getPatientOrderService().findPatientOrderReferralsByPatientOrderId(patientOrder.getPatientOrderId()).stream()
					.map(patientOrderReferral -> patientOrderReferral.getPatientOrderReferralReasonId())
					.collect(Collectors.toSet());

			context.put("patientOrderReferralReasonIds", patientOrderReferralReasonIds);

			// If this order is part of one or more studies, expose study URL names for easy JS access
			List<Study> studies = getStudyService().findStudiesByPatientOrderId(patientOrder.getPatientOrderId());

			context.put("studyUrlNames", studies.stream()
					.map(study -> study.getUrlName())
					.collect(Collectors.toList()));
		}

		Account account = getCurrentContext().getAccount().orElse(null);

		context.put("accountId", account == null ? null : account.getAccountId());
		context.put("screenings", screenings);
		context.put("screeningsByName", screeningsByName);
		context.put("screeningSession", screeningSession);
		context.put("screeningSessionScreenings", screeningSessionScreenings);
		context.put("screeningVersionIdsByName", screeningVersionIdsByName);
		context.put("screeningResultsByScreeningSessionScreeningId", screeningResultsByScreeningSessionScreeningId);
		context.put("selfAdministered", selfAdministered);
		context.put("additionalContext", additionalContext == null ? Map.of() : additionalContext);

		if (screeningSession.getGroupSessionId() != null) {
			GroupSession groupSession = getGroupSessionService().findGroupSessionById(screeningSession.getGroupSessionId(), institutionId).orElse(null);

			// If we couldn't find the session, perhaps it was deleted.  Use failsafe here
			if (groupSession == null)
				groupSession = getGroupSessionService().findGroupSessionByIdIncludingDeleted(screeningSession.getGroupSessionId()).get();

			context.put("groupSession", groupSession);
		}

		if (screeningSession.getAccountCheckInActionId() != null) {
			Optional<AccountCheckIn> accountCheckIn = getStudyService().findAccountCheckInByActionId(screeningSession.getAccountCheckInActionId());
			if (accountCheckIn.isPresent()) {
				context.put("askContentSatisfaction", accountCheckIn.get().getCheckInNumber() == null ? false : accountCheckIn.get().getCheckInNumber() > 1);
			}
		}

		// Expose any custom metadata for this session, or the empty object if there is none
		Map<String, Object> metadata = screeningSession.getMetadata();
		context.put("metadata", metadata == null ? Map.of() : metadata);

		try {
			screeningFlowFunctionResult = getJavascriptExecutor().execute(screeningFlowFunctionJavascript, context, screeningFlowFunctionResultType);
		} catch (JavascriptExecutionException e) {
			throw new RuntimeException(e);
		}

		return Optional.ofNullable(screeningFlowFunctionResult);
	}

	@Nonnull
	public Boolean isScreeningSessionSelfAdministered(@Nonnull ScreeningSession screeningSession) {
		requireNonNull(screeningSession);

		return Objects.equals(screeningSession.getCreatedByAccountId(), screeningSession.getTargetAccountId())
				|| screeningSession.getTargetAccountId() == null;
	}

	@NotThreadSafe
	protected static class ScreeningVersionName {
		@Nullable
		private UUID screeningVersionId;
		@Nullable
		private String name;

		@Nullable
		public UUID getScreeningVersionId() {
			return this.screeningVersionId;
		}

		public void setScreeningVersionId(@Nullable UUID screeningVersionId) {
			this.screeningVersionId = screeningVersionId;
		}

		@Nullable
		public String getName() {
			return this.name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}
	}

	@Nonnull
	public Optional<ScreeningSessionResult> findScreeningSessionResult(@Nullable UUID screeningSessionId) {
		ScreeningSession screeningSession = findScreeningSessionById(screeningSessionId).orElse(null);

		if (screeningSession == null)
			return Optional.empty();

		return findScreeningSessionResult(screeningSession);
	}

	@Nonnull
	public Optional<ScreeningSessionResult> findScreeningSessionResult(@Nullable ScreeningSession screeningSession) {
		if (screeningSession == null)
			return Optional.empty();

		ScreeningFlowVersion screeningFlowVersion = findScreeningFlowVersionById(screeningSession.getScreeningFlowVersionId()).get();
		ScreeningFlow screeningFlow = findScreeningFlowById(screeningFlowVersion.getScreeningFlowId()).get();
		List<ScreeningSessionScreening> screeningSessionScreenings = findCurrentScreeningSessionScreeningsByScreeningSessionId(screeningSession.getScreeningSessionId());
		List<ScreeningSessionScreeningResult> screeningSessionScreeningResults = new ArrayList<>();

		for (ScreeningSessionScreening screeningSessionScreening : screeningSessionScreenings) {
			ScreeningVersion screeningVersion = findScreeningVersionById(screeningSessionScreening.getScreeningVersionId()).get();
			Screening screening = findScreeningById(screeningVersion.getScreeningId()).get();
			ScreeningScore screeningScore = screeningSessionScreening.getScoreAsObject().get();
			List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions = findScreeningQuestionsWithAnswerOptionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());
			List<ScreeningSessionAnsweredScreeningQuestion> screeningSessionAnsweredScreeningQuestions = findScreeningSessionAnsweredScreeningQuestionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());
			List<ScreeningQuestionResult> screeningQuestionResults = new ArrayList<>();

			for (ScreeningSessionAnsweredScreeningQuestion screeningSessionAnsweredScreeningQuestion : screeningSessionAnsweredScreeningQuestions) {
				for (ScreeningQuestionWithAnswerOptions screeningQuestionWithAnswerOptions : screeningQuestionsWithAnswerOptions) {
					ScreeningQuestion screeningQuestion = screeningQuestionWithAnswerOptions.getScreeningQuestion();
					List<ScreeningAnswerOption> screeningAnswerOptions = screeningQuestionWithAnswerOptions.getScreeningAnswerOptions();
					List<ScreeningAnswerResult> screeningAnswerResults = new ArrayList<>();

					if (screeningQuestion.getScreeningQuestionId().equals(screeningSessionAnsweredScreeningQuestion.getScreeningQuestionId())) {
						List<ScreeningAnswer> screeningAnswers = findScreeningAnswersByScreeningQuestionContextId(
								new ScreeningQuestionContextId(screeningSessionScreening.getScreeningSessionScreeningId(), screeningQuestionWithAnswerOptions.getScreeningQuestion().getScreeningQuestionId()));

						for (ScreeningAnswerOption potentialScreeningAnswerOption : screeningAnswerOptions) {
							for (ScreeningAnswer screeningAnswer : screeningAnswers) {
								if (screeningAnswer.getScreeningAnswerOptionId().equals(potentialScreeningAnswerOption.getScreeningAnswerOptionId())) {
									ScreeningAnswerResult screeningAnswerResult = new ScreeningAnswerResult();
									screeningAnswerResult.setScreeningAnswerId(screeningAnswer.getScreeningAnswerId());
									screeningAnswerResult.setScreeningAnswerOptionId(screeningAnswer.getScreeningAnswerOptionId());
									screeningAnswerResult.setAnswerOptionText(potentialScreeningAnswerOption.getAnswerOptionText());
									screeningAnswerResult.setText(screeningAnswer.getText());
									screeningAnswerResult.setScore(potentialScreeningAnswerOption.getScore());
									screeningAnswerResults.add(screeningAnswerResult);
								}
							}
						}

						ScreeningQuestionResult screeningQuestionResult = new ScreeningQuestionResult();
						screeningQuestionResult.setScreeningQuestionId(screeningQuestion.getScreeningQuestionId());
						screeningQuestionResult.setScreeningAnswerFormatId(screeningQuestion.getScreeningAnswerFormatId());
						screeningQuestionResult.setScreeningAnswerContentHintId(screeningQuestion.getScreeningAnswerContentHintId());
						screeningQuestionResult.setScreeningQuestionIntroText(screeningQuestion.getIntroText());
						screeningQuestionResult.setScreeningQuestionText(screeningQuestion.getQuestionText());
						screeningQuestionResult.setScreeningAnswerResults(screeningAnswerResults);
						screeningQuestionResults.add(screeningQuestionResult);
					}
				}
			}

			ScreeningSessionScreeningResult screeningSessionScreeningResult = new ScreeningSessionScreeningResult();
			screeningSessionScreeningResult.setScreeningId(screening.getScreeningId());
			screeningSessionScreeningResult.setScreeningName(screening.getName());
			screeningSessionScreeningResult.setScreeningScore(screeningScore);
			screeningSessionScreeningResult.setScreeningVersionNumber(screeningVersion.getVersionNumber());
			screeningSessionScreeningResult.setScreeningTypeId(screeningVersion.getScreeningTypeId());
			screeningSessionScreeningResult.setScreeningVersionId(screeningVersion.getScreeningVersionId());
			screeningSessionScreeningResult.setBelowScoringThreshold(screeningSessionScreening.getBelowScoringThreshold());
			screeningSessionScreeningResult.setScreeningQuestionResults(screeningQuestionResults);

			screeningSessionScreeningResults.add(screeningSessionScreeningResult);
		}

		ScreeningSessionResult screeningSessionResult = new ScreeningSessionResult();
		screeningSessionResult.setScreeningFlowId(screeningFlow.getScreeningFlowId());
		screeningSessionResult.setScreeningFlowName(screeningFlow.getName());
		screeningSessionResult.setScreeningFlowVersionId(screeningFlowVersion.getScreeningFlowVersionId());
		screeningSessionResult.setScreeningFlowVersionNumber(screeningFlowVersion.getVersionNumber());
		screeningSessionResult.setScreeningSessionScreeningResults(screeningSessionScreeningResults);

		return Optional.of(screeningSessionResult);
	}

	@Nonnull
	public List<ScreeningSessionScreeningWithType> findScreeningSessionScreeningsWithTypeByScreeningFlowId(@Nullable UUID screeningFlowId,
																																																				 @Nullable InstitutionId institutionId,
																																																				 @Nullable Instant startTimestamp,
																																																				 @Nullable Instant endTimestamp) {
		if (screeningFlowId == null || institutionId == null || startTimestamp == null || endTimestamp == null)
			return List.of();

		EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(institutionId);
		StringBuilder sql = new StringBuilder("""
				SELECT sv.screening_type_id, sss.*
				FROM v_screening_session_screening sss, screening_session ss, account a, screening_version sv, screening_flow_version sfv
				WHERE sss.screening_session_id=ss.screening_session_id
				AND sv.screening_version_id=sss.screening_version_id
				AND sfv.screening_flow_id=?
				AND ss.screening_flow_version_id=sfv.screening_flow_version_id
				AND ss.target_account_id=a.account_id
				AND a.institution_id=?
				AND ss.created BETWEEN ? AND ?
				AND sss.completed=TRUE
				AND ss.completed=TRUE
				""");

		if (enterprisePlugin.analyticsClinicalScreeningFlowNeedsCrisisSkipWorkaround(screeningFlowId)) {
			// Support special legacy data where a screening session could end immediately on crisis, but due
			// to a bug, users could still back-button and skip after completing.
			// Alternative would be to update all affected screening sessions in the DB to remove "skipped=true" flag.
			sql.append("AND (ss.skipped=FALSE OR (ss.skipped=TRUE AND ss.crisis_indicated=TRUE))");
		} else {
			sql.append("AND ss.skipped=FALSE");
		}

		return getDatabase().queryForList(sql.toString(),
				ScreeningSessionScreeningWithType.class, screeningFlowId, institutionId, startTimestamp, endTimestamp);
	}

	public void debugScreeningSession(@Nonnull UUID screeningSessionId) {
		requireNonNull(screeningSessionId);

		if (!getLogger().isDebugEnabled())
			return;

		ScreeningSession screeningSession = findScreeningSessionById(screeningSessionId).get();
		ScreeningFlowVersion screeningFlowVersion = findScreeningFlowVersionById(screeningSession.getScreeningFlowVersionId()).get();
		ScreeningFlow screeningFlow = findScreeningFlowById(screeningFlowVersion.getScreeningFlowId()).get();
		List<ScreeningSessionScreening> screeningSessionScreenings = findCurrentScreeningSessionScreeningsByScreeningSessionId(screeningSessionId);

		List<String> logLines = new ArrayList<>();

		logLines.add("*** SCREENING SESSION DEBUG ***");
		logLines.add(format("Screening Flow '%s', version %d", screeningFlow.getName(), screeningFlowVersion.getVersionNumber()));

		for (ScreeningSessionScreening screeningSessionScreening : screeningSessionScreenings) {
			ScreeningVersion screeningVersion = findScreeningVersionById(screeningSessionScreening.getScreeningVersionId()).get();
			Screening screening = findScreeningById(screeningVersion.getScreeningId()).get();
			ScreeningScore screeningScore = screeningSessionScreening.getScoreAsObject().get();

			logLines.add(format("\tScreening '%s', version %d, score %d", screening.getName(), screeningVersion.getVersionNumber(), screeningScore.getOverallScore()));

			List<ScreeningQuestionWithAnswerOptions> screeningQuestionsWithAnswerOptions = findScreeningQuestionsWithAnswerOptionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());
			List<ScreeningSessionAnsweredScreeningQuestion> screeningSessionAnsweredScreeningQuestions = findScreeningSessionAnsweredScreeningQuestionsByScreeningSessionScreeningId(screeningSessionScreening.getScreeningSessionScreeningId());

			for (ScreeningSessionAnsweredScreeningQuestion screeningSessionAnsweredScreeningQuestion : screeningSessionAnsweredScreeningQuestions) {
				for (ScreeningQuestionWithAnswerOptions screeningQuestionWithAnswerOptions : screeningQuestionsWithAnswerOptions) {
					if (screeningQuestionWithAnswerOptions.getScreeningQuestion().getScreeningQuestionId().equals(screeningSessionAnsweredScreeningQuestion.getScreeningQuestionId())) {
						logLines.add(format("\t\tQuestion: %s", screeningQuestionWithAnswerOptions.getScreeningQuestion().getQuestionText()));

						List<ScreeningAnswer> screeningAnswers = findScreeningAnswersByScreeningQuestionContextId(
								new ScreeningQuestionContextId(screeningSessionScreening.getScreeningSessionScreeningId(), screeningQuestionWithAnswerOptions.getScreeningQuestion().getScreeningQuestionId()));

						for (ScreeningAnswerOption potentialScreeningAnswerOption : screeningQuestionWithAnswerOptions.getScreeningAnswerOptions()) {
							String answers = screeningAnswers.stream().map(screeningAnswer -> {
										if (screeningAnswer.getScreeningAnswerOptionId().equals(potentialScreeningAnswerOption.getScreeningAnswerOptionId())) {
											if (potentialScreeningAnswerOption.getFreeformSupplement())
												return format("%s (answer: %s) (score %d)", potentialScreeningAnswerOption.getAnswerOptionText(), screeningAnswer.getText(), potentialScreeningAnswerOption.getScore());
											else
												return format("%s (score %d)", potentialScreeningAnswerOption.getAnswerOptionText(), potentialScreeningAnswerOption.getScore());
										}

										return null;
									})
									.filter(answer -> answer != null)
									.collect(Collectors.joining(", "));

							if (answers.length() > 0)
								logLines.add(format("\t\tAnswer: %s", answers));
						}
					}
				}
			}
		}

		getLogger().debug(logLines.stream().collect(Collectors.joining("\n")));
	}

	@NotThreadSafe
	protected static class ScreeningScoringFunctionOutput {
		@Nullable
		private Boolean completed;
		@Nullable
		private ScreeningScore score;
		@Nullable
		private Boolean belowScoringThreshold;
		@Nullable
		private UUID nextScreeningQuestionId; // Entirely optional; if not specified the next question in the progression is picked
		@Nullable
		private List<CreateScreeningAnswersMessage> messages; // Optional
		@Nonnull
		private Map<UUID, CreateScreeningAnswersQuestionResult> questionResultsByScreeningAnswerOptionId; // Optional

		@Nullable
		public Boolean getCompleted() {
			return this.completed;
		}

		public void setCompleted(@Nullable Boolean completed) {
			this.completed = completed;
		}

		@Nullable
		public ScreeningScore getScore() {
			return this.score;
		}

		public void setScore(@Nullable ScreeningScore score) {
			this.score = score;
		}

		@Nullable
		public Boolean getBelowScoringThreshold() {
			return this.belowScoringThreshold;
		}

		public void setBelowScoringThreshold(@Nullable Boolean belowScoringThreshold) {
			this.belowScoringThreshold = belowScoringThreshold;
		}

		@Nullable
		public UUID getNextScreeningQuestionId() {
			return this.nextScreeningQuestionId;
		}

		public void setNextScreeningQuestionId(@Nullable UUID nextScreeningQuestionId) {
			this.nextScreeningQuestionId = nextScreeningQuestionId;
		}

		@Nullable
		public List<CreateScreeningAnswersMessage> getMessages() {
			return this.messages;
		}

		public void setMessages(@Nullable List<CreateScreeningAnswersMessage> messages) {
			this.messages = messages;
		}

		@Nonnull
		public Map<UUID, CreateScreeningAnswersQuestionResult> getQuestionResultsByScreeningAnswerOptionId() {
			return this.questionResultsByScreeningAnswerOptionId;
		}

		public void setQuestionResultsByScreeningAnswerOptionId(@Nonnull Map<UUID, CreateScreeningAnswersQuestionResult> questionResultsByScreeningAnswerOptionId) {
			this.questionResultsByScreeningAnswerOptionId = questionResultsByScreeningAnswerOptionId;
		}
	}

	@NotThreadSafe
	protected static class OrchestrationFunctionOutput {
		@Nullable
		private Boolean crisisIndicated;
		@Nullable
		private Boolean completed;
		@Nullable
		private Boolean skipped;
		@Nullable
		private UUID nextScreeningId;
		@Nullable
		private PatientOrderClosureReasonId patientOrderClosureReasonId;
		@Nullable
		private Set<UUID> optionalCourseModuleIdsToSet;
		@Nullable
		private Set<UUID> optionalCourseModuleIdsToClear;

		@Nullable
		public Boolean getCrisisIndicated() {
			return this.crisisIndicated;
		}

		public void setCrisisIndicated(@Nullable Boolean crisisIndicated) {
			this.crisisIndicated = crisisIndicated;
		}

		@Nullable
		public Boolean getCompleted() {
			return this.completed;
		}

		public void setCompleted(@Nullable Boolean completed) {
			this.completed = completed;
		}

		@Nullable
		public Boolean getSkipped() {
			return this.skipped;
		}

		public void setSkipped(@Nullable Boolean skipped) {
			this.skipped = skipped;
		}

		@Nullable
		public UUID getNextScreeningId() {
			return this.nextScreeningId;
		}

		public void setNextScreeningId(@Nullable UUID nextScreeningId) {
			this.nextScreeningId = nextScreeningId;
		}

		@Nullable
		public PatientOrderClosureReasonId getPatientOrderClosureReasonId() {
			return this.patientOrderClosureReasonId;
		}

		public void setPatientOrderClosureReasonId(@Nullable PatientOrderClosureReasonId patientOrderClosureReasonId) {
			this.patientOrderClosureReasonId = patientOrderClosureReasonId;
		}

		@Nullable
		public Set<UUID> getOptionalCourseModuleIdsToSet() {
			return this.optionalCourseModuleIdsToSet;
		}

		public void setOptionalCourseModuleIdsToSet(@Nullable Set<UUID> optionalCourseModuleIdsToSet) {
			this.optionalCourseModuleIdsToSet = optionalCourseModuleIdsToSet;
		}

		@Nullable
		public Set<UUID> getOptionalCourseModuleIdsToClear() {
			return this.optionalCourseModuleIdsToClear;
		}

		public void setOptionalCourseModuleIdsToClear(@Nullable Set<UUID> optionalCourseModuleIdsToClear) {
			this.optionalCourseModuleIdsToClear = optionalCourseModuleIdsToClear;
		}
	}

	@NotThreadSafe
	protected static class InitializationFunctionOutput {
		@Nullable
		private UUID initialScreeningId;

		@Nullable
		public UUID getInitialScreeningId() {
			return this.initialScreeningId;
		}

		public void setInitialScreeningId(@Nullable UUID initialScreeningId) {
			this.initialScreeningId = initialScreeningId;
		}
	}

	@NotThreadSafe
	protected static class ResultsFunctionOutput {
		@Nullable
		private Set<String> recommendedTagIds;
		@Nullable
		private List<SupportRoleRecommendation> supportRoleRecommendations;
		@Nullable
		private List<IntegratedCareTriage> integratedCareTriages;
		@Nullable
		private PatientOrderCareTypeId integratedCareTriagedCareTypeId; // the overall "winning" triage for IC
		@Nullable
		private Set<FeatureId> recommendedFeatureIds;
		@Nullable
		@Deprecated
		private Set<UUID> legacyContentAnswerIds;
		@Nullable
		@Deprecated
		private Boolean recommendLegacyContentAnswerIds;
		@Nullable
		private PatientOrderIntakeLocationStatusId patientOrderIntakeLocationStatusId;
		@Nullable
		private PatientOrderIntakeInsuranceStatusId patientOrderIntakeInsuranceStatusId;
		@Nullable
		private PatientOrderIntakeWantsServicesStatusId patientOrderIntakeWantsServicesStatusId;
		@Nullable
		private PatientOrderConsentStatusId patientOrderConsentStatusId;
		@Nullable
		private String courseSessionUnitCompletionMessage;

		@Nullable
		public Set<String> getRecommendedTagIds() {
			return this.recommendedTagIds;
		}

		public void setRecommendedTagIds(@Nullable Set<String> recommendedTagIds) {
			this.recommendedTagIds = recommendedTagIds;
		}

		@Nullable
		public List<SupportRoleRecommendation> getSupportRoleRecommendations() {
			return this.supportRoleRecommendations;
		}

		public void setSupportRoleRecommendations(@Nullable List<SupportRoleRecommendation> supportRoleRecommendations) {
			this.supportRoleRecommendations = supportRoleRecommendations;
		}

		@Nullable
		public Set<UUID> getLegacyContentAnswerIds() {
			return this.legacyContentAnswerIds;
		}

		@Deprecated
		public void setLegacyContentAnswerIds(@Nullable Set<UUID> legacyContentAnswerIds) {
			this.legacyContentAnswerIds = legacyContentAnswerIds;
		}

		@Deprecated
		@Nullable
		public Boolean getRecommendLegacyContentAnswerIds() {
			return this.recommendLegacyContentAnswerIds;
		}

		@Deprecated
		public void setRecommendLegacyContentAnswerIds(@Nullable Boolean recommendLegacyContentAnswerIds) {
			this.recommendLegacyContentAnswerIds = recommendLegacyContentAnswerIds;
		}

		@Nullable
		public List<IntegratedCareTriage> getIntegratedCareTriages() {
			return this.integratedCareTriages;
		}

		public void setIntegratedCareTriages(@Nullable List<IntegratedCareTriage> integratedCareTriages) {
			this.integratedCareTriages = integratedCareTriages;
		}

		@Nullable
		public PatientOrderCareTypeId getIntegratedCareTriagedCareTypeId() {
			return this.integratedCareTriagedCareTypeId;
		}

		public void setIntegratedCareTriagedCareTypeId(@Nullable PatientOrderCareTypeId integratedCareTriagedCareTypeId) {
			this.integratedCareTriagedCareTypeId = integratedCareTriagedCareTypeId;
		}

		@Nullable
		public Set<FeatureId> getRecommendedFeatureIds() {
			return this.recommendedFeatureIds;
		}

		public void setRecommendedFeatureIds(@Nullable Set<FeatureId> recommendedFeatureIds) {
			this.recommendedFeatureIds = recommendedFeatureIds;
		}

		@Nullable
		public PatientOrderIntakeLocationStatusId getPatientOrderIntakeLocationStatusId() {
			return this.patientOrderIntakeLocationStatusId;
		}

		public void setPatientOrderIntakeLocationStatusId(@Nullable PatientOrderIntakeLocationStatusId patientOrderIntakeLocationStatusId) {
			this.patientOrderIntakeLocationStatusId = patientOrderIntakeLocationStatusId;
		}

		@Nullable
		public PatientOrderIntakeInsuranceStatusId getPatientOrderIntakeInsuranceStatusId() {
			return this.patientOrderIntakeInsuranceStatusId;
		}

		public void setPatientOrderIntakeInsuranceStatusId(@Nullable PatientOrderIntakeInsuranceStatusId patientOrderIntakeInsuranceStatusId) {
			this.patientOrderIntakeInsuranceStatusId = patientOrderIntakeInsuranceStatusId;
		}

		@Nullable
		public PatientOrderIntakeWantsServicesStatusId getPatientOrderIntakeWantsServicesStatusId() {
			return this.patientOrderIntakeWantsServicesStatusId;
		}

		public void setPatientOrderIntakeWantsServicesStatusId(@Nullable PatientOrderIntakeWantsServicesStatusId patientOrderIntakeWantsServicesStatusId) {
			this.patientOrderIntakeWantsServicesStatusId = patientOrderIntakeWantsServicesStatusId;
		}

		@Nullable
		public PatientOrderConsentStatusId getPatientOrderConsentStatusId() {
			return this.patientOrderConsentStatusId;
		}

		public void setPatientOrderConsentStatusId(@Nullable PatientOrderConsentStatusId patientOrderConsentStatusId) {
			this.patientOrderConsentStatusId = patientOrderConsentStatusId;
		}

		@Nullable
		public String getCourseSessionUnitCompletionMessage() {
			return this.courseSessionUnitCompletionMessage;
		}

		public void setCourseSessionUnitCompletionMessage(@Nullable String courseSessionUnitCompletionMessage) {
			this.courseSessionUnitCompletionMessage = courseSessionUnitCompletionMessage;
		}

		@NotThreadSafe
		public static class SupportRoleRecommendation {
			@Nullable
			private SupportRoleId supportRoleId;
			@Nullable
			private Double weight;

			@Nullable
			public SupportRoleId getSupportRoleId() {
				return this.supportRoleId;
			}

			public void setSupportRoleId(@Nullable SupportRoleId supportRoleId) {
				this.supportRoleId = supportRoleId;
			}

			@Nullable
			public Double getWeight() {
				return this.weight;
			}

			public void setWeight(@Nullable Double weight) {
				this.weight = weight;
			}
		}

		@NotThreadSafe
		public static class IntegratedCareTriage {
			@Nullable
			private PatientOrderFocusTypeId patientOrderFocusTypeId;
			@Nullable
			private PatientOrderCareTypeId patientOrderCareTypeId;
			@Nullable
			private String reason;

			@Nullable
			public PatientOrderFocusTypeId getPatientOrderFocusTypeId() {
				return this.patientOrderFocusTypeId;
			}

			public void setPatientOrderFocusTypeId(@Nullable PatientOrderFocusTypeId patientOrderFocusTypeId) {
				this.patientOrderFocusTypeId = patientOrderFocusTypeId;
			}

			@Nullable
			public PatientOrderCareTypeId getPatientOrderCareTypeId() {
				return this.patientOrderCareTypeId;
			}

			public void setPatientOrderCareTypeId(@Nullable PatientOrderCareTypeId patientOrderCareTypeId) {
				this.patientOrderCareTypeId = patientOrderCareTypeId;
			}

			@Nullable
			public String getReason() {
				return this.reason;
			}

			public void setReason(@Nullable String reason) {
				this.reason = reason;
			}
		}
	}

	@NotThreadSafe
	protected static class DestinationFunctionOutput {
		@Nullable
		private ScreeningSessionDestinationId screeningSessionDestinationId;
		@Nullable
		private Map<String, Object> context;

		@Nullable
		public ScreeningSessionDestinationId getScreeningSessionDestinationId() {
			return this.screeningSessionDestinationId;
		}

		public void setScreeningSessionDestinationId(@Nullable ScreeningSessionDestinationId screeningSessionDestinationId) {
			this.screeningSessionDestinationId = screeningSessionDestinationId;
		}

		@Nullable
		public Map<String, Object> getContext() {
			return this.context;
		}

		public void setContext(@Nullable Map<String, Object> context) {
			this.context = context;
		}
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
	}

	@Nonnull
	protected InteractionService getInteractionService() {
		return this.interactionServiceProvider.get();
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountServiceProvider.get();
	}

	@Nonnull
	protected PatientOrderService getPatientOrderService() {
		return this.patientOrderServiceProvider.get();
	}

	@Nonnull
	protected GroupSessionService getGroupSessionService() {
		return this.groupSessionServiceProvider.get();
	}

	@Nonnull
	protected CourseService getCourseService() {
		return this.courseServiceProvider.get();
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return this.authorizationServiceProvider.get();
	}

	@Nonnull
	protected MessageService getMessageService() {
		return this.messageServiceProvider.get();
	}

	@Nonnull
	protected StudyService getStudyService() {
		return this.studyServiceProvider.get();
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected ScreeningConfirmationPromptApiResponseFactory getScreeningConfirmationPromptApiResponseFactory() {
		return this.screeningConfirmationPromptApiResponseFactory;
	}

	@Nonnull
	protected JavascriptExecutor getJavascriptExecutor() {
		return this.javascriptExecutor;
	}

	@Nonnull
	protected EnterprisePluginProvider getEnterprisePluginProvider() {
		return this.enterprisePluginProvider;
	}

	@Nonnull
	protected ErrorReporter getErrorReporter() {
		return this.errorReporter;
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
