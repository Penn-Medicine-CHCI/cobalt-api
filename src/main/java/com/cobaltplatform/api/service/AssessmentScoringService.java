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

import com.lokalized.Strings;
import com.cobaltplatform.api.error.ErrorReporter;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageManager;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSession;
import com.cobaltplatform.api.model.db.CrisisContact;
import com.cobaltplatform.api.model.db.assessment.Answer;
import com.cobaltplatform.api.model.db.assessment.Assessment;
import com.cobaltplatform.api.model.db.assessment.Assessment.AssessmentType;
import com.cobaltplatform.api.model.service.EvidenceScores;
import com.cobaltplatform.api.util.Formatter;
import com.pyranid.Database;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.cobaltplatform.api.model.service.EvidenceScores.Recommendation;
import static com.cobaltplatform.api.model.service.EvidenceScores.RecommendationLevel;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

/**
 * @author Transmogrify LLC.
 */
@Singleton
public class AssessmentScoringService {

	@Nonnull
	private final Provider<SessionService> sessionServiceProvider;
	@Nonnull
	private final Provider<AssessmentService> assessmentServiceProvider;
	@Nonnull
	private final EmailMessageManager emailMessageManager;
	@Nonnull
	private final ErrorReporter errorReporter;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Strings strings;

	@Inject
	public AssessmentScoringService(@Nonnull Provider<AssessmentService> assessmentServiceProvder,
																	@Nonnull Provider<SessionService> sessionService,
																	@Nonnull EmailMessageManager emailMessageManager,
																	@Nonnull ErrorReporter errorReporter,
																	@Nonnull Database database,
																	@Nonnull Formatter formatter,
																	@Nonnull Strings strings) {
		this.assessmentServiceProvider = assessmentServiceProvder;
		this.sessionServiceProvider = sessionService;
		this.emailMessageManager = emailMessageManager;
		this.errorReporter = errorReporter;
		this.database = database;
		this.formatter = formatter;
		this.strings = strings;
	}

	@Nonnull
	public void finishEvidenceAssessment(@Nonnull Account account) {
		sessionService().markCurrentSessionCompleteForAssessmentType(account, AssessmentType.PHQ4);
		sessionService().markCurrentSessionCompleteForAssessmentType(account, AssessmentType.PHQ9);
		sessionService().markCurrentSessionCompleteForAssessmentType(account, AssessmentType.GAD7);
		sessionService().markCurrentSessionCompleteForAssessmentType(account, AssessmentType.PCPTSD);
		EvidenceScores scores = getEvidenceAssessmentRecommendation(account)
				.orElseThrow(() -> new IllegalStateException("Just marked session as complete but was not able to calculate final score"));

		if (scores.getCrisis()) {
			sendCrisisEmail(account, scores);
		}
	}

	private void sendCrisisEmail(Account crisisAccount, EvidenceScores scores) {

		List<CrisisContact> crisisContacts = database.queryForList("SELECT * FROM crisis_contact WHERE institution_id = ? AND active = true",
				CrisisContact.class, crisisAccount.getInstitutionId());

		if (crisisContacts.isEmpty()) {
			errorReporter.report(format("Crisis alert email needed, but there are no active crisis contacts for the users institution. institution_id = %s", crisisAccount.getInstitutionId()));
			return;
		}

		String name = crisisAccount.getDisplayName() == null ? "Anonymous User" : crisisAccount.getDisplayName();
		String emailAddress = crisisAccount.getEmailAddress() == null ? getStrings().get("[no email address]") : crisisAccount.getEmailAddress();
		String phoneNumber = getFormatter().formatPhoneNumber(crisisAccount.getPhoneNumber(), crisisAccount.getLocale());
		phoneNumber = phoneNumber == null ? getStrings().get("[no phone number]") : phoneNumber;

		String accountDescription = format("Q9 alert for %s at %s with %s and S3: %s and %s and %s and %s.",
				name, phoneNumber, emailAddress,
				scores.getPhq4Recommendation().getAnswers(),
				scores.getPhq9Recommendation().getAnswers(),
				scores.getGad7Recommendation().getAnswers(),
				scores.getPcptsdRecommendation().getAnswers());

		String institutionDescription = format("Account - %s at %s - %s - %s", crisisAccount.getAccountId(), crisisAccount.getInstitutionId(),
				emailAddress, phoneNumber);

		Map<String, Object> messageContext = new HashMap<>();
		messageContext.put("accountDescription", accountDescription);
		messageContext.put("institutionDescription", institutionDescription);

		for (CrisisContact crisisContact : crisisContacts) {
			emailMessageManager.enqueueMessage(new EmailMessage.Builder(EmailMessageTemplate.SUICIDE_RISK, crisisContact.getLocale())
					.toAddresses(new ArrayList<>() {{
						add(crisisContact.getEmailAddress());
					}})
					.messageContext(messageContext)
					.build());
		}
	}

	@Nonnull
	public Boolean isBookingAllowed(@Nonnull AccountSession accountSession) {
		Optional<Assessment> assessment = getAssessmentService().findAssessmentById(accountSession.getAssessmentId());

		if (!assessment.isPresent())
			return false;

		Integer count = database.queryForObject("SELECT SUM(a.answer_value) " +
						"FROM account_session_answer asa, answer a " +
						"WHERE asa.answer_id = a.answer_id AND asa.account_session_id = ?",
				Integer.class, accountSession.getAccountSessionId()).get();

		if (count >= assessment.get().getMinimumEligibilityScore())
			return true;
		else
			return false;
	}

	@Nonnull
	public Optional<EvidenceScores> getEvidenceAssessmentRecommendation(@Nonnull Account account) {

		Optional<AccountSession> phq4Session = sessionService().getCompletedAssessmentSessionForAccount(account, AssessmentType.PHQ4);
		if (phq4Session.isEmpty()) {
			return Optional.empty();
		} else {
			List<Answer> phq4UserAnswers = sessionService().findAnswersForSession(phq4Session.get());
			int phq4AnswerValue = phq4UserAnswers.stream().mapToInt(Answer::getAnswerValue).sum();
			String phq4AnswerString = generateAnswersString(phq4UserAnswers);
			Recommendation phq4Recommendation = new Recommendation(RecommendationLevel.PEER_COACH, phq4AnswerValue, phq4Session.get().getAccountSessionId(), phq4AnswerString);
			if (phq4AnswerValue <= 2) {
				return Optional.of(new EvidenceScores(phq4Recommendation, null, null, null, false));
			} else {

				Optional<AccountSession> phq9Session = sessionService().getCompletedAssessmentSessionForAccount(account, AssessmentType.PHQ9);
				if (phq9Session.isEmpty()) return Optional.empty();
				List<Answer> phq9UserAnswers = sessionService().findAnswersForSession(phq9Session.get());
				phq9UserAnswers.add(0, phq4UserAnswers.get(2));
				phq9UserAnswers.add(1, phq4UserAnswers.get(3));
				int phq9AnswerValue = phq9UserAnswers.stream().mapToInt(Answer::getAnswerValue).sum();
				String phq9AnswerString = generateAnswersString(phq9UserAnswers);

				Boolean isCrisis = phq9UserAnswers.stream().anyMatch(Answer::getCrisis);
				Recommendation phq9Recommendation;
				if (phq9AnswerValue < 5) {
					phq9Recommendation = new Recommendation(RecommendationLevel.PEER_COACH, phq9AnswerValue, phq9Session.get().getAccountSessionId(), phq9AnswerString);
				} else if (phq9AnswerValue < 10) {
					phq9Recommendation = new Recommendation(RecommendationLevel.COACH, phq9AnswerValue, phq9Session.get().getAccountSessionId(), phq9AnswerString);
				} else if (phq9AnswerValue < 20) {
					phq9Recommendation = new Recommendation(RecommendationLevel.CLINICIAN, phq9AnswerValue, phq9Session.get().getAccountSessionId(), phq9AnswerString);
				} else {
					phq9Recommendation = new Recommendation(RecommendationLevel.PSYCHIATRIST, phq9AnswerValue, phq9Session.get().getAccountSessionId(), phq9AnswerString);
				}

				Optional<AccountSession> gad7Session = sessionService().getCompletedAssessmentSessionForAccount(account, AssessmentType.GAD7);
				if (gad7Session.isEmpty()) return Optional.empty();
				List<Answer> gad7UserAnswers = sessionService().findAnswersForSession(gad7Session.get());
				gad7UserAnswers.add(0, phq4UserAnswers.get(0));
				gad7UserAnswers.add(1, phq4UserAnswers.get(1));
				int gad7AnswerValue = gad7UserAnswers.stream().mapToInt(Answer::getAnswerValue).sum();
				String gad7AnswerString = generateAnswersString(gad7UserAnswers);
				Recommendation gad7Recommendation;
				if (gad7AnswerValue < 5) {
					gad7Recommendation = new Recommendation(RecommendationLevel.PEER_COACH, gad7AnswerValue, gad7Session.get().getAccountSessionId(), gad7AnswerString);
				} else if (gad7AnswerValue < 10) {
					gad7Recommendation = new Recommendation(RecommendationLevel.COACH_CLINICIAN, gad7AnswerValue, gad7Session.get().getAccountSessionId(), gad7AnswerString);
				} else if (gad7AnswerValue < 20) {
					gad7Recommendation = new Recommendation(RecommendationLevel.CLINICIAN, gad7AnswerValue, gad7Session.get().getAccountSessionId(), gad7AnswerString);
				} else {
					gad7Recommendation = new Recommendation(RecommendationLevel.PSYCHIATRIST, gad7AnswerValue, gad7Session.get().getAccountSessionId(), gad7AnswerString);
				}


				Optional<AccountSession> pcptsdSession = sessionService().getCompletedAssessmentSessionForAccount(account, AssessmentType.PCPTSD);
				if (pcptsdSession.isEmpty()) return Optional.empty();
				List<Answer> pcptsdUserAnswers = sessionService().findAnswersForSession(pcptsdSession.get());
				int pcptsdAnswerValue = pcptsdUserAnswers.stream().mapToInt(Answer::getAnswerValue).sum();
				String pcptsdAnswerString = generateAnswersString(pcptsdUserAnswers);
				Recommendation pcptsdRecommendation;
				if (pcptsdAnswerValue < 3) {
					pcptsdRecommendation = new Recommendation(RecommendationLevel.COACH_CLINICIAN, pcptsdAnswerValue, pcptsdSession.get().getAccountSessionId(), pcptsdAnswerString);
				} else {
					pcptsdRecommendation = new Recommendation(RecommendationLevel.CLINICIAN_PSYCHIATRIST, pcptsdAnswerValue, pcptsdSession.get().getAccountSessionId(), pcptsdAnswerString);
				}
				return Optional.of(new EvidenceScores(phq4Recommendation, phq9Recommendation, gad7Recommendation, pcptsdRecommendation, isCrisis));
			}
		}
	}

	@Nonnull
	private String generateAnswersString(List<Answer> answers) {
		return answers.stream().map(a -> a.getAnswerValue().toString()).collect(joining("-"));
	}

	@Nonnull
	protected AssessmentService getAssessmentService() {
		return assessmentServiceProvider.get();
	}

	private SessionService sessionService() {
		return sessionServiceProvider.get();
	}

	@Nonnull
	protected Formatter getFormatter() {
		return formatter;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}
}
