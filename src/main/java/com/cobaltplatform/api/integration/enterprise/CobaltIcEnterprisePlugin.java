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

package com.cobaltplatform.api.integration.enterprise;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.integration.epic.EpicClient;
import com.cobaltplatform.api.model.db.Flowsheet;
import com.cobaltplatform.api.model.db.FlowsheetType.FlowsheetTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.ScreeningFlowType.ScreeningFlowTypeId;
import com.cobaltplatform.api.model.db.ScreeningQuestion;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.db.ScreeningType.ScreeningTypeId;
import com.cobaltplatform.api.model.service.ScreeningSessionResult;
import com.cobaltplatform.api.model.service.ScreeningSessionResult.ScreeningAnswerResult;
import com.cobaltplatform.api.model.service.ScreeningSessionResult.ScreeningQuestionResult;
import com.cobaltplatform.api.model.service.ScreeningSessionResult.ScreeningSessionScreeningResult;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.PatientOrderService;
import com.cobaltplatform.api.service.ScreeningService;
import com.cobaltplatform.api.util.AwsSecretManagerClient;
import com.cobaltplatform.api.util.ValidationException;
import com.lokalized.Strings;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class CobaltIcEnterprisePlugin extends DefaultEnterprisePlugin {
	@Nonnull
	private final ScreeningService screeningService;
	@Nonnull
	private final PatientOrderService patientOrderService;
	@Nonnull
	private final Strings strings;

	@Inject
	public CobaltIcEnterprisePlugin(@Nonnull InstitutionService institutionService,
																	@Nonnull AwsSecretManagerClient awsSecretManagerClient,
																	@Nonnull Configuration configuration,
																	@Nonnull ScreeningService screeningService,
																	@Nonnull PatientOrderService patientOrderService,
																	@Nonnull Strings strings) {
		super(institutionService, awsSecretManagerClient, configuration);

		requireNonNull(screeningService);
		requireNonNull(patientOrderService);
		requireNonNull(strings);

		this.screeningService = screeningService;
		this.patientOrderService = patientOrderService;
		this.strings = strings;
	}

	@Nonnull
	@Override
	public InstitutionId getInstitutionId() {
		return InstitutionId.COBALT_IC;
	}

	@Override
	public void performPatientOrderEncounterWriteback(@Nullable UUID patientOrderId) {
		// Look for a completed screening session...
		List<ScreeningSession> screeningSessions = getScreeningService().findScreeningSessionsByPatientOrderIdAndScreeningFlowTypeId(patientOrderId, ScreeningFlowTypeId.INTEGRATED_CARE);
		ScreeningSession completedScreeningSession = screeningSessions.stream()
				.filter(screeningSession -> screeningSession.getCompleted())
				.findFirst()
				.orElse(null);

		if (completedScreeningSession == null)
			throw new ValidationException(getStrings().get("Cannot perform encounter writeback; there is no completed assessment for this order."));

		// Pull flowsheets for writeback
		List<Flowsheet> flowsheets = getPatientOrderService().findFlowsheetsByInstitutionId(getInstitutionId());

		// Store flowsheets by ID for quick access
		Map<FlowsheetTypeId, Flowsheet> flowsheetsByTypeId = flowsheets.stream()
				.collect(Collectors.toMap(Flowsheet::getFlowsheetTypeId, Function.identity()));

		// Keep track of values to write for each flowsheet
		Map<FlowsheetTypeId, String> flowsheetValuesByTypeId = new HashMap<>(flowsheetsByTypeId.size());

		Map<Integer, String> phq9FlowsheetValuesByAnswerScore = Map.of(
				1, "Not at all",
				2, "Several days",
				3, "More than half the days",
				4, "Nearly every day"
		);

		ScreeningSessionResult screeningSessionResult = getScreeningService().findScreeningSessionResult(completedScreeningSession).get();

		for (ScreeningSessionScreeningResult screeningSessionScreeningResult : screeningSessionResult.getScreeningSessionScreeningResults()) {
			// We don't care about these scores for the writeback
			if (!(screeningSessionScreeningResult.getScreeningTypeId() == ScreeningTypeId.C_SSRS_8
					|| screeningSessionScreeningResult.getScreeningTypeId() == ScreeningTypeId.GAD_7
					|| screeningSessionScreeningResult.getScreeningTypeId() == ScreeningTypeId.PHQ_9)) {
				continue;
			}

			Integer overallScore = screeningSessionScreeningResult.getScreeningScore().getOverallScore();

			// Keep track of CSSRS scores so we can figure out value to send for CSSRS_IC_RISK_SCORE.
			// C-SSRS-8 crisis is indicated if (Q3, Q4, Q5, or Q6 is scored >= 1) OR (Q7 and Q8 are scored >= 1).
			Map<Integer, Integer> cssrsAnswerScoresByQuestionDisplayOrder = new HashMap<>(8);

			// Keep track of PHQ9 scores (need for PHQ2 calculation)
			Map<Integer, Integer> phq9AnswerScoresByQuestionDisplayOrder = new HashMap<>(9);

			// PHQ9 total score
			if (screeningSessionScreeningResult.getScreeningTypeId() == ScreeningTypeId.PHQ_9)
				flowsheetValuesByTypeId.put(FlowsheetTypeId.PHQ9_TOTAL_SCORE, String.valueOf(overallScore));

			// Only need GAD7 total score, don't need to keep track of additional values
			if (screeningSessionScreeningResult.getScreeningTypeId() == ScreeningTypeId.GAD_7)
				flowsheetValuesByTypeId.put(FlowsheetTypeId.GAD7_TOTAL_SCORE, String.valueOf(overallScore));

			for (ScreeningQuestionResult screeningQuestionResult : screeningSessionScreeningResult.getScreeningQuestionResults()) {
				ScreeningQuestion screeningQuestion = getScreeningService().findScreeningQuestionById(screeningQuestionResult.getScreeningQuestionId()).get();
				int questionOrder = screeningQuestion.getDisplayOrder();
				ScreeningAnswerResult answer = screeningQuestionResult.getScreeningAnswerResults().stream().findFirst().get();
				int answerScore = answer.getScore();

				if (screeningSessionScreeningResult.getScreeningTypeId() == ScreeningTypeId.C_SSRS_8) {
					// Keep track of CSSRS scores so we can figure out value to send for CSSRS_IC_RISK_SCORE.
					cssrsAnswerScoresByQuestionDisplayOrder.put(questionOrder, answerScore);

					if (questionOrder == 1)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.CSSRS_QUESTION_1, answerScore == 1 ? "Yes" : "No");
					else if (questionOrder == 2)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.CSSRS_QUESTION_2, answerScore == 1 ? "Yes" : "No");
					else if (questionOrder == 3)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.CSSRS_QUESTION_3, answerScore == 1 ? "Yes" : "No");
					else if (questionOrder == 4)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.CSSRS_QUESTION_4, answerScore == 1 ? "Yes" : "No");
					else if (questionOrder == 5)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.CSSRS_QUESTION_5, answerScore == 1 ? "Yes" : "No");
						// TODO: how to handle question 6 ("Do you intend to carry out this plan?")?
						// TODO: how to handle CSSRS_QUESTION_6_DESCRIPTION?
					else if (questionOrder == 7)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.CSSRS_QUESTION_6_LIFETIME, answerScore == 1 ? "Yes" : "No");
					else if (questionOrder == 8)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.CSSRS_QUESTION_6_3_MONTHS, answerScore == 1 ? "Yes" : "No");
				} else if (screeningSessionScreeningResult.getScreeningTypeId() == ScreeningTypeId.PHQ_9) {
					// Keep track of PHQ9 scores (need for PHQ2 calculation)
					phq9AnswerScoresByQuestionDisplayOrder.put(questionOrder, answerScore);

					if (questionOrder == 1)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.PHQ9_QUESTION_1, phq9FlowsheetValuesByAnswerScore.get(answerScore));
					if (questionOrder == 2)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.PHQ9_QUESTION_2, phq9FlowsheetValuesByAnswerScore.get(answerScore));
					if (questionOrder == 3)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.PHQ9_QUESTION_3, phq9FlowsheetValuesByAnswerScore.get(answerScore));
					if (questionOrder == 4)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.PHQ9_QUESTION_4, phq9FlowsheetValuesByAnswerScore.get(answerScore));
					if (questionOrder == 5)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.PHQ9_QUESTION_5, phq9FlowsheetValuesByAnswerScore.get(answerScore));
					if (questionOrder == 6)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.PHQ9_QUESTION_6, phq9FlowsheetValuesByAnswerScore.get(answerScore));
					if (questionOrder == 7)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.PHQ9_QUESTION_7, phq9FlowsheetValuesByAnswerScore.get(answerScore));
					if (questionOrder == 8)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.PHQ9_QUESTION_8, phq9FlowsheetValuesByAnswerScore.get(answerScore));
					if (questionOrder == 9)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.PHQ9_QUESTION_9, phq9FlowsheetValuesByAnswerScore.get(answerScore));

					// TODO: how to handle PHQ9_DIFFICULTY_FUNCTIONING?
				} else if (screeningSessionScreeningResult.getScreeningTypeId() == ScreeningTypeId.GAD_7) {
					if (questionOrder == 1)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.GAD7_QUESTION_1, String.valueOf(answerScore));
					else if (questionOrder == 2)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.GAD7_QUESTION_2, String.valueOf(answerScore));
					else if (questionOrder == 3)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.GAD7_QUESTION_3, String.valueOf(answerScore));
					else if (questionOrder == 4)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.GAD7_QUESTION_4, String.valueOf(answerScore));
					else if (questionOrder == 5)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.GAD7_QUESTION_5, String.valueOf(answerScore));
					else if (questionOrder == 6)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.GAD7_QUESTION_6, String.valueOf(answerScore));
					else if (questionOrder == 7)
						flowsheetValuesByTypeId.put(FlowsheetTypeId.GAD7_QUESTION_7, String.valueOf(answerScore));

					// TODO: how to handle GAD7_DIFFICULTY_FUNCTIONING?
				}
			}

			// C-SSRS-8 crisis is indicated if (Q3, Q4, Q5, or Q6 is scored >= 1) OR (Q7 and Q8 are scored >= 1).
			if (cssrsAnswerScoresByQuestionDisplayOrder.size() > 0) {
				boolean crisis = false;

				int question3Score = cssrsAnswerScoresByQuestionDisplayOrder.getOrDefault(3, 0);
				int question4Score = cssrsAnswerScoresByQuestionDisplayOrder.getOrDefault(4, 0);
				int question5Score = cssrsAnswerScoresByQuestionDisplayOrder.getOrDefault(5, 0);
				int question6Score = cssrsAnswerScoresByQuestionDisplayOrder.getOrDefault(6, 0);
				int question7Score = cssrsAnswerScoresByQuestionDisplayOrder.getOrDefault(7, 0);
				int question8Score = cssrsAnswerScoresByQuestionDisplayOrder.getOrDefault(8, 0);

				if (question3Score >= 1 || question4Score >= 1 || question5Score >= 1 || question6Score >= 1)
					crisis = true;

				if (question7Score >= 1 && question8Score >= 1)
					crisis = true;

				flowsheetValuesByTypeId.put(FlowsheetTypeId.CSSRS_IC_RISK_SCORE, crisis ? "Positive" : "Negative");
			}

			Integer phq9Question1AnswerScore = phq9AnswerScoresByQuestionDisplayOrder.get(1);
			Integer phq9Question2AnswerScore = phq9AnswerScoresByQuestionDisplayOrder.get(2);

			if (phq9Question1AnswerScore != null && phq9Question2AnswerScore != null)
				flowsheetValuesByTypeId.put(FlowsheetTypeId.PHQ2_SCORE, String.valueOf(phq9Question1AnswerScore + phq9Question2AnswerScore));
		}

		// For each flowsheet, write it back.
		EpicClient epicClient = epicClientForBackendService().get();
		List<FlowsheetTypeId> flowsheetTypeIds = flowsheetValuesByTypeId.keySet().stream().sorted().collect(Collectors.toList());

		for (FlowsheetTypeId flowsheetTypeId : flowsheetTypeIds) {

		}

		// This is a no-op for Cobalt IC for now...
		throw new UnsupportedOperationException("TODO");
	}

	@Nonnull
	protected ScreeningService getScreeningService() {
		return this.screeningService;
	}

	@Nonnull
	protected PatientOrderService getPatientOrderService() {
		return this.patientOrderService;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}
}
