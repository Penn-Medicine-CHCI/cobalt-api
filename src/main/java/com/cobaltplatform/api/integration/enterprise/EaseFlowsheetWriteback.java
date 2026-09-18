/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
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

package com.cobaltplatform.api.integration.enterprise;

import com.cobaltplatform.api.integration.epic.EpicClient;
import com.cobaltplatform.api.integration.epic.request.AddFlowsheetValueRequest;
import com.cobaltplatform.api.model.db.Flowsheet;
import com.cobaltplatform.api.model.db.FlowsheetType.FlowsheetTypeId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.RawPatientOrder;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.db.ScreeningType.ScreeningTypeId;
import com.cobaltplatform.api.model.service.ScreeningSessionResult;
import com.cobaltplatform.api.model.service.ScreeningSessionResult.ScreeningAnswerResult;
import com.cobaltplatform.api.model.service.ScreeningSessionResult.ScreeningQuestionResult;
import com.cobaltplatform.api.model.service.ScreeningSessionResult.ScreeningSessionScreeningResult;
import com.cobaltplatform.api.util.ValidationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Comparator.nullsFirst;
import static java.util.Objects.requireNonNull;

/**
 * Maps EASE screening responses to institution-configured Epic flowsheet rows.
 */
@ThreadSafe
final class EaseFlowsheetWriteback {
	@Nonnull
	private static final List<FlowsheetTypeId> PHQ_9_FLOWSHEET_TYPE_IDS = List.of(
			FlowsheetTypeId.PHQ9_QUESTION_1,
			FlowsheetTypeId.PHQ9_QUESTION_2,
			FlowsheetTypeId.PHQ9_QUESTION_3,
			FlowsheetTypeId.PHQ9_QUESTION_4,
			FlowsheetTypeId.PHQ9_QUESTION_5,
			FlowsheetTypeId.PHQ9_QUESTION_6,
			FlowsheetTypeId.PHQ9_QUESTION_7,
			FlowsheetTypeId.PHQ9_QUESTION_8,
			FlowsheetTypeId.PHQ9_QUESTION_9
	);
	@Nonnull
	private static final List<FlowsheetTypeId> PROMIS_FLOWSHEET_TYPE_IDS = List.of(
			FlowsheetTypeId.PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_1,
			FlowsheetTypeId.PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_2,
			FlowsheetTypeId.PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_3,
			FlowsheetTypeId.PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_4,
			FlowsheetTypeId.PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_5,
			FlowsheetTypeId.PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_6,
			FlowsheetTypeId.PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_7,
			FlowsheetTypeId.PROMIS_SOCIAL_ROLES_8A_V1_QUESTION_8
	);
	@Nonnull
	private static final Map<Integer, String> PHQ_9_ANSWER_TEXT_BY_SCORE = Map.of(
			0, "Not at all",
			1, "Several days",
			2, "More than half the days",
			3, "Nearly every day"
	);

	private EaseFlowsheetWriteback() {
	}

	@Nullable
	static ScreeningSession findNewestCompletedScreeningSession(@Nonnull List<ScreeningSession> screeningSessions) {
		requireNonNull(screeningSessions);

		return screeningSessions.stream()
				.filter(screeningSession -> Boolean.TRUE.equals(screeningSession.getCompleted()))
				.max(Comparator.comparing(ScreeningSession::getCompletedAt, nullsFirst(Comparator.naturalOrder())))
				.orElse(null);
	}

	@Nonnull
	static Map<FlowsheetTypeId, String> determineFlowsheetValues(
			@Nonnull ScreeningSessionResult screeningSessionResult,
			@Nonnull Function<java.util.UUID, Integer> questionDisplayOrderProvider) {
		requireNonNull(screeningSessionResult);
		requireNonNull(questionDisplayOrderProvider);

		Map<FlowsheetTypeId, String> flowsheetValuesByTypeId = new EnumMap<>(FlowsheetTypeId.class);
		List<ScreeningSessionScreeningResult> screeningResults = screeningSessionResult.getScreeningSessionScreeningResults();

		if (screeningResults == null)
			return flowsheetValuesByTypeId;

		for (ScreeningSessionScreeningResult screeningResult : screeningResults) {
			ScreeningTypeId screeningTypeId = screeningResult.getScreeningTypeId();

			if (screeningTypeId != ScreeningTypeId.PHQ_9
					&& screeningTypeId != ScreeningTypeId.PROMIS_PARTICIPATION_SOCIAL_ROLES_8A_V1)
				continue;

			List<ScreeningQuestionResult> questionResults = screeningResult.getScreeningQuestionResults();

			if (questionResults == null)
				continue;

			for (ScreeningQuestionResult questionResult : questionResults) {
				List<ScreeningAnswerResult> answerResults = questionResult.getScreeningAnswerResults();

				// PHQ questions 3-9 are intentionally unanswered when the PHQ-2 branch completes early.
				if (answerResults == null || answerResults.isEmpty())
					continue;

				ScreeningAnswerResult answerResult = answerResults.get(0);
				Integer answerScore = answerResult.getScore();

				if (questionResult.getScreeningQuestionId() == null || answerScore == null)
					throw new ValidationException("Cannot perform encounter writeback; an answered EASE assessment question is missing its ID or score.");

				Integer questionDisplayOrder = questionDisplayOrderProvider.apply(questionResult.getScreeningQuestionId());

				if (questionDisplayOrder == null)
					throw new ValidationException("Cannot perform encounter writeback; an EASE assessment question could not be located.");

				FlowsheetTypeId flowsheetTypeId = flowsheetTypeIdForQuestion(screeningTypeId, questionDisplayOrder);

				// Ignore non-item questions, such as a potential PHQ difficulty/functioning question.
				if (flowsheetTypeId == null)
					continue;

				flowsheetValuesByTypeId.put(flowsheetTypeId,
						formatAnswerValue(screeningTypeId, questionDisplayOrder, answerScore));
			}
		}

		return flowsheetValuesByTypeId;
	}

	@Nonnull
	static List<AddFlowsheetValueRequest> writeFlowsheetValues(
			@Nonnull ScreeningSessionResult screeningSessionResult,
			@Nonnull Function<java.util.UUID, Integer> questionDisplayOrderProvider,
			@Nonnull List<Flowsheet> flowsheets,
			@Nonnull RawPatientOrder patientOrder,
			@Nonnull Institution institution,
			@Nonnull String encounterCsn,
			@Nonnull Instant instantValueTaken,
			@Nonnull EpicClient epicClient) {
		requireNonNull(screeningSessionResult);
		requireNonNull(questionDisplayOrderProvider);
		requireNonNull(flowsheets);
		requireNonNull(patientOrder);
		requireNonNull(institution);
		requireNonNull(encounterCsn);
		requireNonNull(instantValueTaken);
		requireNonNull(epicClient);

		Map<FlowsheetTypeId, Flowsheet> flowsheetsByTypeId = flowsheets.stream()
				.collect(Collectors.toMap(Flowsheet::getFlowsheetTypeId, Function.identity()));
		Map<FlowsheetTypeId, String> flowsheetValuesByTypeId = determineFlowsheetValues(
				screeningSessionResult, questionDisplayOrderProvider);
		List<FlowsheetTypeId> flowsheetTypeIds = flowsheetValuesByTypeId.keySet().stream()
				.sorted()
				.toList();
		List<AddFlowsheetValueRequest> requests = new ArrayList<>(flowsheetTypeIds.size());

		// Build and validate every request before writing any row so configuration errors cannot cause a partial sync.
		for (FlowsheetTypeId flowsheetTypeId : flowsheetTypeIds) {
			Flowsheet flowsheet = flowsheetsByTypeId.get(flowsheetTypeId);

			if (flowsheet == null)
				throw new ValidationException(format("Cannot perform encounter writeback; no flowsheet is configured for %s.", flowsheetTypeId));

			AddFlowsheetValueRequest request = new AddFlowsheetValueRequest();
			request.setPatientID(patientOrder.getPatientUniqueId());
			request.setPatientIDType(patientOrder.getPatientUniqueIdType());
			request.setContactID(encounterCsn);
			request.setContactIDType("CSN");
			request.setFlowsheetID(flowsheet.getEpicFlowsheetId());
			request.setFlowsheetIDType(flowsheet.getEpicFlowsheetIdType());
			request.setFlowsheetTemplateID(flowsheet.getEpicFlowsheetTemplateId());
			request.setFlowsheetTemplateIDType(flowsheet.getEpicFlowsheetTemplateIdType());
			request.setUserID(institution.getEpicUserId());
			request.setUserIDType(institution.getEpicUserIdType());
			request.setComment(null);
			request.setValue(flowsheetValuesByTypeId.get(flowsheetTypeId));
			request.setInstantValueToken(instantValueTaken);
			requests.add(request);
		}

		for (AddFlowsheetValueRequest request : requests)
			epicClient.addFlowsheetValue(request);

		return List.copyOf(requests);
	}

	@Nullable
	private static FlowsheetTypeId flowsheetTypeIdForQuestion(@Nonnull ScreeningTypeId screeningTypeId,
																												 @Nonnull Integer questionDisplayOrder) {
		requireNonNull(screeningTypeId);
		requireNonNull(questionDisplayOrder);

		List<FlowsheetTypeId> flowsheetTypeIds = screeningTypeId == ScreeningTypeId.PHQ_9
				? PHQ_9_FLOWSHEET_TYPE_IDS
				: PROMIS_FLOWSHEET_TYPE_IDS;

		if (questionDisplayOrder < 1 || questionDisplayOrder > flowsheetTypeIds.size())
			return null;

		return flowsheetTypeIds.get(questionDisplayOrder - 1);
	}

	@Nonnull
	private static String formatAnswerValue(@Nonnull ScreeningTypeId screeningTypeId,
																			 @Nonnull Integer questionDisplayOrder,
																			 @Nonnull Integer answerScore) {
		requireNonNull(screeningTypeId);
		requireNonNull(questionDisplayOrder);
		requireNonNull(answerScore);

		if (screeningTypeId == ScreeningTypeId.PROMIS_PARTICIPATION_SOCIAL_ROLES_8A_V1) {
			if (answerScore < 1 || answerScore > 5)
				throw new ValidationException(format("Cannot perform encounter writeback; PROMIS answer score %d is invalid.", answerScore));

			return String.valueOf(answerScore);
		}

		String answerText = PHQ_9_ANSWER_TEXT_BY_SCORE.get(answerScore);

		if (answerText == null)
			throw new ValidationException(format("Cannot perform encounter writeback; PHQ-9 answer score %d is invalid.", answerScore));

		return questionDisplayOrder <= 2
				? String.valueOf(answerScore)
				: format("%d - %s", answerScore, answerText);
	}
}
