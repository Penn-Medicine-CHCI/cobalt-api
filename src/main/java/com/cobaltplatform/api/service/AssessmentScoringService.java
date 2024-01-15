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

import com.cobaltplatform.api.error.ErrorReporter;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.api.request.CreateInteractionInstanceRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSession;
import com.cobaltplatform.api.model.db.Answer;
import com.cobaltplatform.api.model.db.Assessment;
import com.cobaltplatform.api.model.db.AssessmentType.AssessmentTypeId;
import com.cobaltplatform.api.model.db.CrisisContact;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.service.EvidenceScores;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.model.service.EvidenceScores.Recommendation;
import static com.cobaltplatform.api.model.service.EvidenceScores.RecommendationLevel;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

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
	private final Provider<InteractionService> interactionServiceProvider;
	@Nonnull
	private final Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final Provider<MessageService> messageServiceProvider;
	@Nonnull
	private final ErrorReporter errorReporter;
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Strings strings;

	@Inject
	public AssessmentScoringService(@Nonnull Provider<AssessmentService> assessmentServiceProvder,
																	@Nonnull Provider<SessionService> sessionServiceProvder,
																	@Nonnull Provider<InteractionService> interactionServiceProvider,
																	@Nonnull Provider<InstitutionService> institutionServiceProvider,
																	@Nonnull Provider<MessageService> messageServiceProvider,
																	@Nonnull ErrorReporter errorReporter,
																	@Nonnull DatabaseProvider databaseProvider,
																	@Nonnull Formatter formatter,
																	@Nonnull Strings strings) {
		requireNonNull(assessmentServiceProvder);
		requireNonNull(sessionServiceProvder);
		requireNonNull(interactionServiceProvider);
		requireNonNull(institutionServiceProvider);
		requireNonNull(messageServiceProvider);
		requireNonNull(errorReporter);
		requireNonNull(databaseProvider);
		requireNonNull(formatter);
		requireNonNull(strings);

		this.assessmentServiceProvider = assessmentServiceProvder;
		this.sessionServiceProvider = sessionServiceProvder;
		this.interactionServiceProvider = interactionServiceProvider;
		this.institutionServiceProvider = institutionServiceProvider;
		this.messageServiceProvider = messageServiceProvider;
		this.errorReporter = errorReporter;
		this.databaseProvider = databaseProvider;
		this.formatter = formatter;
		this.strings = strings;
	}

	@Nonnull
	public void finishEvidenceAssessment(@Nonnull Account account) {
		sessionService().markCurrentSessionCompleteForAssessmentType(account, AssessmentTypeId.PHQ4);
		sessionService().markCurrentSessionCompleteForAssessmentType(account, AssessmentTypeId.PHQ9);
		sessionService().markCurrentSessionCompleteForAssessmentType(account, AssessmentTypeId.GAD7);
		sessionService().markCurrentSessionCompleteForAssessmentType(account, AssessmentTypeId.PCPTSD);
		EvidenceScores scores = getEvidenceAssessmentRecommendation(account)
				.orElseThrow(() -> new IllegalStateException("Just marked session as complete but was not able to calculate final score"));

		if (scores.getCrisis()) {
			// TODO: remove crisis email in favor of the crisis interaction
			sendCrisisEmail(account, scores);
			createCrisisInteraction(account, scores);
		}
	}

	// TODO: remove crisis email in favor of the crisis interaction
	private void sendCrisisEmail(Account crisisAccount, EvidenceScores scores) {

		List<CrisisContact> crisisContacts = getDatabase().queryForList("SELECT * FROM crisis_contact WHERE institution_id = ? AND active = true",
				CrisisContact.class, crisisAccount.getInstitutionId());

		if (crisisContacts.isEmpty()) {
			getErrorReporter().report(format("Crisis alert email needed, but there are no active crisis contacts for the users institution. institution_id = %s", crisisAccount.getInstitutionId()));
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
			getMessageService().enqueueMessage(new EmailMessage.Builder(crisisAccount.getInstitutionId(), EmailMessageTemplate.SUICIDE_RISK, crisisContact.getLocale())
					.toAddresses(new ArrayList<>() {{
						add(crisisContact.getEmailAddress());
					}})
					.messageContext(messageContext)
					.build());
		}
	}

	private void createCrisisInteraction(@Nonnull Account crisisAccount,
																			 @Nonnull EvidenceScores evidenceScores) {
		requireNonNull(crisisAccount);
		requireNonNull(evidenceScores);

		// Find the crisis interaction ID for this account's institution
		Institution institution = getInstitutionService().findInstitutionById(crisisAccount.getInstitutionId()).get();
		UUID defaultCrisisInteractionId = institution.getStandardMetadata().getDefaultCrisisInteractionId();

		if (defaultCrisisInteractionId == null) {
			getErrorReporter().report(format("No default crisis interaction ID is available for institution %s", institution.getInstitutionId()));
			return;
		}

		// Gather information to put into the interaction
		ZoneId timeZone = institution.getTimeZone();
		LocalDateTime now = LocalDateTime.now(timeZone);
		Locale locale = institution.getLocale();

		Map<String, Object> metadata = createCrisisInteractionMetadata(crisisAccount, evidenceScores, locale);
		Map<String, Object> hipaaCompliantMetadata = createCrisisInteractionHipaaCompliantMetadata(crisisAccount, evidenceScores, locale);

		// Record an interaction for this incident, which might send off some email messages (for example)
		getInteractionService().createInteractionInstance(new CreateInteractionInstanceRequest() {{
			setMetadata(metadata);
			setHipaaCompliantMetadata(hipaaCompliantMetadata);
			setStartDateTime(now);
			setTimeZone(timeZone);
			setInteractionId(defaultCrisisInteractionId);
		}});
	}

	@Nonnull
	protected Map<String, Object> createCrisisInteractionHipaaCompliantMetadata(@Nonnull Account crisisAccount,
																																							@Nonnull EvidenceScores evidenceScores,
																																							@Nonnull Locale locale) {
		requireNonNull(crisisAccount);
		requireNonNull(evidenceScores);
		requireNonNull(locale);

		List<String> htmlListItems = new ArrayList<>(2);

		if (crisisAccount.getFirstName() != null)
			htmlListItems.add(createHtmlListItem(getStrings().get("First Name", locale), crisisAccount.getFirstName()));

		if (crisisAccount.getPhoneNumber() != null)
			htmlListItems.add(createHtmlListItem(getStrings().get("Phone Number", locale), format("<a href='tel:%s'>%s</a>", crisisAccount.getPhoneNumber(), getFormatter().formatPhoneNumber(crisisAccount.getPhoneNumber(), locale)), false));
		else if (crisisAccount.getEmailAddress() != null)
			htmlListItems.add(createHtmlListItem(getStrings().get("Email Address", locale), format("<a href='mailto:%s'>%s</a>", crisisAccount.getEmailAddress(), crisisAccount.getEmailAddress()), false));
		else
			htmlListItems.add(createHtmlListItem(getStrings().get("Contact Information", locale), getStrings().get("None Available", locale)));

		String endUserHtmlRepresentation = format("<ul>%s</ul>", htmlListItems.stream().collect(Collectors.joining("")));

		return new HashMap<String, Object>() {{
			if (crisisAccount.getFirstName() != null)
				put("firstName", crisisAccount.getFirstName());

			if (crisisAccount.getPhoneNumber() != null) {
				put("phoneNumber", crisisAccount.getPhoneNumber());
				put("phoneNumberForDisplay", getFormatter().formatPhoneNumber(crisisAccount.getPhoneNumber(), locale));
			} else if (crisisAccount.getEmailAddress() != null) {
				put("emailAddress", crisisAccount.getEmailAddress());
			}

			put("endUserHtmlRepresentation", endUserHtmlRepresentation);
		}};
	}

	@Nonnull
	protected Map<String, Object> createCrisisInteractionMetadata(@Nonnull Account crisisAccount,
																																@Nonnull EvidenceScores evidenceScores,
																																@Nonnull Locale locale) {
		requireNonNull(crisisAccount);
		requireNonNull(evidenceScores);
		requireNonNull(locale);

		List<String> htmlListItems = new ArrayList<>(7);

		if (crisisAccount.getDisplayName() != null)
			htmlListItems.add(createHtmlListItem(getStrings().get("Name", locale), crisisAccount.getDisplayName()));
		if (crisisAccount.getPhoneNumber() != null)
			htmlListItems.add(createHtmlListItem(getStrings().get("Phone Number", locale), format("<a href='tel:%s'>%s</a>", crisisAccount.getPhoneNumber(), getFormatter().formatPhoneNumber(crisisAccount.getPhoneNumber(), locale)), false));
		if (crisisAccount.getEmailAddress() != null)
			htmlListItems.add(createHtmlListItem(getStrings().get("Email Address", locale), format("<a href='mailto:%s'>%s</a>", crisisAccount.getEmailAddress(), crisisAccount.getEmailAddress()), false));
		if (evidenceScores.getPhq4Recommendation().getAnswers() != null)
			htmlListItems.add(createHtmlListItem(getStrings().get("PHQ4 Answers", locale), evidenceScores.getPhq4Recommendation().getAnswers()));
		if (evidenceScores.getPhq9Recommendation().getAnswers() != null)
			htmlListItems.add(createHtmlListItem(getStrings().get("PHQ9 Answers", locale), evidenceScores.getPhq9Recommendation().getAnswers()));
		if (evidenceScores.getGad7Recommendation().getAnswers() != null)
			htmlListItems.add(createHtmlListItem(getStrings().get("GAD7 Answers", locale), evidenceScores.getGad7Recommendation().getAnswers()));
		if (evidenceScores.getPcptsdRecommendation().getAnswers() != null)
			htmlListItems.add(createHtmlListItem(getStrings().get("PCPTSD Answers", locale), evidenceScores.getPcptsdRecommendation().getAnswers()));

		String endUserHtmlRepresentation = format("<ul>%s</ul>", htmlListItems.stream().collect(Collectors.joining("")));

		return new HashMap<String, Object>() {{
			if (crisisAccount.getDisplayName() != null)
				put("name", crisisAccount.getDisplayName());

			if (crisisAccount.getPhoneNumber() != null) {
				put("phoneNumber", crisisAccount.getPhoneNumber());
				put("phoneNumberForDisplay", getFormatter().formatPhoneNumber(crisisAccount.getPhoneNumber(), locale));
			}

			if (crisisAccount.getEmailAddress() != null)
				put("emailAddress", crisisAccount.getEmailAddress());

			if (evidenceScores.getPhq4Recommendation().getAnswers() != null)
				put("phq4Answers", evidenceScores.getPhq4Recommendation().getAnswers());

			if (evidenceScores.getPhq9Recommendation().getAnswers() != null)
				put("phq9Answers", evidenceScores.getPhq9Recommendation().getAnswers());

			if (evidenceScores.getGad7Recommendation().getAnswers() != null)
				put("gad7Answers", evidenceScores.getGad7Recommendation().getAnswers());

			if (evidenceScores.getPcptsdRecommendation().getAnswers() != null)
				put("pcptsdAnswers", evidenceScores.getPcptsdRecommendation().getAnswers());

			put("endUserHtmlRepresentation", endUserHtmlRepresentation);
		}};
	}

	@Nonnull
	protected String createHtmlListItem(@Nonnull String fieldName,
																			@Nullable String fieldValue) {
		requireNonNull(fieldName);
		return createHtmlListItem(fieldName, fieldValue, true);
	}

	@Nonnull
	protected String createHtmlListItem(@Nonnull String fieldName,
																			@Nullable String fieldValue,
																			@Nonnull Boolean escapeHtml) {
		requireNonNull(fieldName);
		requireNonNull(escapeHtml);

		if (trimToNull(fieldValue) == null)
			fieldValue = getStrings().get("[unknown]");

		return format("<li><strong>%s</strong> %s</li>", escapeHtml ? escapeHtml4(fieldName) : fieldName,
				escapeHtml ? escapeHtml4(fieldValue) : fieldValue);
	}

	@Nonnull
	public Boolean isBookingAllowed(@Nonnull AccountSession accountSession) {
		Optional<Assessment> assessment = getAssessmentService().findAssessmentById(accountSession.getAssessmentId());

		if (!assessment.isPresent())
			return false;

		Integer count = getDatabase().queryForObject("SELECT SUM(a.answer_value) " +
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

		Optional<AccountSession> phq4Session = sessionService().getCompletedAssessmentSessionForAccount(account, AssessmentTypeId.PHQ4);
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

				Optional<AccountSession> phq9Session = sessionService().getCompletedAssessmentSessionForAccount(account, AssessmentTypeId.PHQ9);
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

				Optional<AccountSession> gad7Session = sessionService().getCompletedAssessmentSessionForAccount(account, AssessmentTypeId.GAD7);
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


				Optional<AccountSession> pcptsdSession = sessionService().getCompletedAssessmentSessionForAccount(account, AssessmentTypeId.PCPTSD);
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
	protected String generateAnswersString(List<Answer> answers) {
		return answers.stream().map(a -> a.getAnswerValue().toString()).collect(joining("-"));
	}

	@Nonnull
	protected AssessmentService getAssessmentService() {
		return assessmentServiceProvider.get();
	}

	@Nonnull
	protected SessionService sessionService() {
		return sessionServiceProvider.get();
	}

	@Nonnull
	protected InteractionService getInteractionService() {
		return interactionServiceProvider.get();
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return institutionServiceProvider.get();
	}

	@Nonnull
	protected MessageService getMessageService() {
		return messageServiceProvider.get();
	}

	@Nonnull
	protected ErrorReporter getErrorReporter() {
		return errorReporter;
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
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
