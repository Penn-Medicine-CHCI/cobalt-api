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

import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSession;
import com.cobaltplatform.api.model.db.AccountSessionAnswer;
import com.cobaltplatform.api.model.db.Answer;
import com.cobaltplatform.api.model.db.Assessment;
import com.cobaltplatform.api.model.db.AssessmentType.AssessmentTypeId;
import com.cobaltplatform.api.model.db.Question;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

/**
 * @author Transmogrify LLC.
 */
@Singleton
@ThreadSafe
public class SessionService {

	@Nonnull
	private final Database database;
	@Nonnull
	private final Logger logger;

	@Inject
	public SessionService(@Nonnull Database database) {
		this.database = database;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<AccountSession> findAccountSessionById(@Nonnull UUID accountSessionId) {
		return database.queryForObject("SELECT * from account_session WHERE account_session_id = ?",
				AccountSession.class, accountSessionId);
	}

	@Nonnull
	public Optional<AccountSession> findAccountSessionByIdAndAccount(@Nonnull Account account, @Nonnull UUID sessionId) {
		return database.queryForObject("SELECT * from account_session WHERE account_id = ? and account_session_id = ?",
				AccountSession.class, account.getAccountId(), sessionId);
	}

	@Nonnull
	public Optional<AccountSession> findCurrentAccountSessionForAssessment(@Nonnull Account account, @Nonnull Assessment assessment) {
		return findCurrentAccountSessionForAssessmentId(account, assessment.getAssessmentId());
	}

	@Nonnull
	public Optional<AccountSession> findCurrentAccountSessionForAssessmentId(@Nonnull Account account, @Nonnull UUID assessmentId) {
		return database.queryForObject("SELECT * from account_session WHERE account_id = ? AND assessment_id = ? AND current_flag = ? ORDER BY last_updated DESC LIMIT 1",
				AccountSession.class, account.getAccountId(), assessmentId, true);
	}

	@Nonnull
	public List<Answer> findAnswersForLastCompleteIntroSession(@Nonnull AccountSession currentAccountSession,
																														 @Nonnull Question question) {
		Optional<AccountSession> lastCompleteSession = database.queryForObject("SELECT * FROM account_session WHERE " +
						"assessment_id = ? AND " +
						"account_id = ? AND " +
						"complete_flag = ? " +
						"ORDER BY created DESC LIMIT 1",
				AccountSession.class,
				currentAccountSession.getAssessmentId(), currentAccountSession.getAccountId(), true);

		if (lastCompleteSession.isEmpty()) return emptyList();
		return findAnswersForSessionAndQuestion(lastCompleteSession.get(), question);
	}

	public void markCurrentSessionCompleteForAssessmentType(@Nonnull Account account,
																													@Nonnull AssessmentTypeId assessmentTypeId) {
		database.execute("UPDATE account_session SET complete_flag = ? WHERE account_session_id = " +
						"(" +
						"SELECT account_session_id FROM " +
						"account_session as acs, " +
						"institution_assessment as ia, " +
						"assessment as ass " +
						"WHERE " +
						"ass.assessment_type_id = ? AND " +
						"acs.account_id = ? AND " +
						"ass.assessment_id = ia.assessment_id AND " +
						"acs.assessment_id = ass.assessment_id AND " +
						"ia.institution_id = ? AND " +
						"current_flag = ? " +
						"ORDER BY acs.last_updated DESC LIMIT 1)",
				true, assessmentTypeId, account.getAccountId(), account.getInstitutionId(), true);
	}


	@Nonnull
	public List<Answer> findAnswersForSession(@Nonnull AccountSession accountSession) {
		return database.queryForList("SELECT a.* FROM " +
						"answer as a, " +
						"question as q, " +
						"account_session_answer as asa WHERE " +
						"asa.account_session_id = ? AND " +
						"q.question_id = a.question_id AND " +
						"asa.answer_id = a.answer_id ORDER BY q.display_order",
				Answer.class, accountSession.getAccountSessionId());
	}

	@Nonnull
	public List<AccountSessionAnswer> findAccountSessionAnswersForAccountSessionId(@Nullable UUID accountSessionId) {
		if (accountSessionId == null)
			return Collections.emptyList();

		return database.queryForList("SELECT asa.* FROM " +
						"answer as a, " +
						"question as q, " +
						"account_session_answer as asa WHERE " +
						"asa.account_session_id = ? AND " +
						"q.question_id = a.question_id AND " +
						"asa.answer_id = a.answer_id ORDER BY q.display_order",
				AccountSessionAnswer.class, accountSessionId);
	}

	@Nonnull
	public Boolean canAccountIdViewAccountSessionId(@Nullable UUID accountId,
																									@Nullable UUID accountSessionId) {
		return database.queryForObject("SELECT COUNT(*) > 0 FROM assessment_viewer av, account_session a_s " +
				"WHERE av.assessment_id=a_s.assessment_id AND av.account_id=? AND a_s.account_session_id=?", Boolean.class, accountId, accountSessionId).get();
	}

	@Nonnull
	public Optional<AccountSession> getCurrentIntroSessionForAccount(@Nonnull Account account) {
		return database.queryForObject("SELECT acs.* FROM account_session as acs, assessment as a, institution_assessment as ia " +
						"WHERE " +
						"acs.account_id = ? AND " +
						"ia.institution_id = ? AND " +
						"a.assessment_type_id = ? AND " +
						"ia.assessment_id = a.assessment_id AND " +
						"acs.assessment_id = a.assessment_id AND " +
						"acs.current_flag = true " +
						"ORDER BY created DESC " +
						"LIMIT 1",
				AccountSession.class,
				account.getAccountId(),
				account.getInstitutionId(),
				AssessmentTypeId.INTRO);
	}

	@Nonnull
	public Optional<AccountSession> getCompletedAssessmentSessionForAccount(@Nonnull Account account,
																																					@Nonnull AssessmentTypeId assessmentTypeId) {
		return database.queryForObject("SELECT account_session.* FROM " +
						"account, " +
						"account_session, " +
						"institution_assessment, " +
						"assessment " +
						"WHERE " +
						"institution_assessment.institution_id = account.institution_id AND " +
						"institution_assessment.assessment_id = account_session.assessment_id AND " +
						"institution_assessment.assessment_id = assessment.assessment_id AND " +
						"account_session.account_id = ? AND " +
						"assessment.assessment_type_id = ? AND " +
						"account_session.complete_flag = ? " +
						"ORDER BY account_session.created DESC " +
						"LIMIT 1",
				AccountSession.class,
				account.getAccountId(),
				assessmentTypeId,
				true
		);
	}

	@Nonnull
	public void markSessionAsComplete(@Nonnull AccountSession accountSession) {
		logger.debug(format("Mark session complete - account_session_id: %s assessment_id: %s ", accountSession.getAccountSessionId(), accountSession.getAssessmentId()));
		database.execute("UPDATE account_session SET complete_flag = ? WHERE account_session_id = ?", true, accountSession.getAccountSessionId());
	}

	@Nonnull
	public AccountSession createSessionForAssessment(@Nonnull UUID accountId,
																									 @Nonnull Assessment assessment) {
		UUID newSessionId = UUID.randomUUID();
		logger.debug(format("Create session - account_session_id: %s assessment_id: %s ", newSessionId, assessment.getAssessmentId()));
		database.execute("UPDATE account_session SET current_flag = ? AND complete_flag = ? WHERE account_id = ? AND assessment_id = ?",
				false, false, accountId, assessment.getAssessmentId());

		if (assessment.getAssessmentTypeId().equals(AssessmentTypeId.PHQ4)) {
			database.execute(
					"UPDATE account_session SET current_flag = ? WHERE account_session_id IN (" +
							"SELECT acs.account_session_id " +
							"FROM " +
							"account_session as acs, " +
							"account as a, " +
							"institution_assessment as ia, " +
							"assessment as ass " +
							"WHERE " +
							"acs.assessment_id = ass.assessment_id AND " +
							"ia.institution_id = a.institution_id AND " +
							"ia.assessment_id = ass.assessment_id AND " +
							"a.account_id = acs.account_id AND " +
							"a.account_id = ? AND " +
							"ass.assessment_type_id IN (?, ?, ?, ?) " +
							")",
					false, accountId, AssessmentTypeId.PHQ4, AssessmentTypeId.PHQ9, AssessmentTypeId.GAD7, AssessmentTypeId.PCPTSD);
		}

		return database.executeReturning("INSERT INTO account_session VALUES (?,?,?) RETURNING *",
				AccountSession.class, newSessionId, accountId, assessment.getAssessmentId()).get();

	}


	@Nonnull
	public Optional<AccountSession> findCurrentIncompleteEvidenceAssessmentForAccount(@Nonnull Account account) {
		Optional<AccountSession> opt = database.queryForObject("SELECT acs.* " +
				"FROM " +
				"account_session as acs, " +
				"account as a, " +
				"institution_assessment as ia, " +
				"assessment as ass " +
				"WHERE " +
				"acs.assessment_id = ass.assessment_id AND " +
				"ia.institution_id = a.institution_id AND " +
				"ia.assessment_id = ass.assessment_id AND " +
				"a.account_id = ? AND " +
				"acs.account_id = a.account_id AND " +
				"acs.complete_flag = ? AND " +
				"acs.current_flag = ? AND " +
				"ass.assessment_type_id IN (?, ?, ?, ?) " +
				"ORDER by acs.created DESC LIMIT 1", AccountSession.class, account.getAccountId(), false, true, AssessmentTypeId.PHQ4, AssessmentTypeId.PHQ9, AssessmentTypeId.GAD7, AssessmentTypeId.PCPTSD);
		return opt;
	}

	@Nonnull
	public Optional<AccountSession> findCurrentIncompleteIntroAssessmentForAccount(@Nonnull Account account) {
		Optional<AccountSession> opt = database.queryForObject("SELECT acs.* " +
				"FROM " +
				"account_session as acs, " +
				"account as a, " +
				"institution_assessment as ia, " +
				"assessment as ass " +
				"WHERE " +
				"acs.assessment_id = ass.assessment_id AND " +
				"ia.institution_id = a.institution_id AND " +
				"ia.assessment_id = ass.assessment_id AND " +
				"a.account_id = ? AND " +
				"acs.account_id = a.account_id AND " +
				"acs.complete_flag = ? AND " +
				"acs.current_flag = ? AND " +
				"ass.assessment_type_id = ? " +
				"ORDER by acs.created DESC LIMIT 1", AccountSession.class, account.getAccountId(), false, true, AssessmentTypeId.INTRO);
		return opt;
	}

	@Nonnull
	public Optional<AccountSession> findIntakeAssessmentForAppointmentId(@Nonnull UUID appointmentId) {
		return database.queryForObject("SELECT acs.*  " +
						"FROM  " +
						"account_session as acs, " +
						"assessment as ass,  " +
						"appointment a " +
						"WHERE " +
						"acs.assessment_id = ass.assessment_id AND " +
						"ass.assessment_id = a.intake_assessment_id AND " +
						"acs.complete_flag = ? AND  " +
						"acs.current_flag = ? AND  " +
						"a.appointment_id = ? "+
						"ORDER by acs.created DESC LIMIT 1 "
				, AccountSession.class, true, true, appointmentId);
	}

	@Nonnull
	public Optional<AccountSession> findCurrentIntakeAssessmentForAccountAndProvider(@Nonnull Account account,
																																									 @Nonnull UUID providerId,
																																									 @Nullable UUID appointmentTypeId,
																																									 @Nonnull Boolean complete) {
		if (appointmentTypeId != null) {
			return database.queryForObject("SELECT acs.*  " +
							"FROM  " +
							"account as a,  " +
							"account_session as acs,  " +
							"assessment as ass,  " +
							"appointment_type_assessment ata, " +
							"provider_appointment_type pat " +
							"WHERE  " +
							"a.account_id = acs.account_id AND  " +
							"acs.assessment_id = ass.assessment_id AND " +
							"ass.assessment_id = ata.assessment_id AND " +
							"ata.appointment_type_id = pat.appointment_type_id AND " +
							"pat.provider_id = ? AND  " +
							"a.account_id = ? AND  " +
							//"ata.active = ? AND " +
							"acs.complete_flag = ? AND  " +
							"acs.current_flag = ? AND  " +
							"pat.appointment_type_id = ? " +
							"ORDER by acs.created DESC LIMIT 1 "
					, AccountSession.class, providerId,
					account.getAccountId(), complete, true, appointmentTypeId);
		}

		return database.queryForObject("SELECT acs.* " +
						"FROM " +
						"account_session as acs, " +
						"account as a, " +
						"assessment as ass, " +
						"provider_clinic pc, " +
						"clinic c " +
						"WHERE " +
						"acs.assessment_id = ass.assessment_id AND " +
						"c.clinic_id = pc.clinic_id AND " +
						"pc.provider_id = ? AND pc.primary_clinic = true AND " +
						"c.intake_assessment_id = ass.assessment_id AND " +
						"a.account_id = ? AND " +
						"acs.account_id = a.account_id AND " +
						"acs.complete_flag = ? AND " +
						"acs.current_flag = ? AND " +
						"ass.assessment_type_id = ? " +
						"ORDER by acs.created DESC LIMIT 1", AccountSession.class, providerId,
				account.getAccountId(), complete, true, AssessmentTypeId.INTAKE);
	}

	@Nonnull
	public List<AccountSession> findCompletedAccountSessionsForAccount(@Nonnull Account account) {
		return database.queryForList("SELECT * FROM account_session WHERE account_id = ? AND complete_flag = true", AccountSession.class,
				account.getAccountId());
	}

	@Nonnull
	public Optional<AccountSession> findCurrentIntakeAssessmentForAccountAndGroupSessionId(@Nonnull Account account, @Nonnull UUID groupSessionId,
																																												 @Nonnull Boolean complete) {
		Optional<AccountSession> opt = database.queryForObject("SELECT acs.* " +
						"FROM " +
						"account_session as acs, " +
						"account as a, " +
						"assessment as ass, " +
						"v_group_session gs " +
						"WHERE " +
						"acs.assessment_id = ass.assessment_id AND " +
						"gs.assessment_id = ass.assessment_id AND " +
						"gs.group_session_id=? AND " +
						"a.account_id = ? AND " +
						"acs.account_id = a.account_id AND " +
						"acs.complete_flag = ? AND " +
						"acs.current_flag = ? AND " +
						"ass.assessment_type_id = ? " +
						"ORDER by acs.created DESC LIMIT 1", AccountSession.class, groupSessionId,
				account.getAccountId(), complete, true, AssessmentTypeId.INTAKE);
		return opt;
	}

	@Nonnull
	public List<Answer> findAnswersForSessionAndQuestion(@Nonnull AccountSession accountSession, @Nonnull Question question) {
		return database.queryForList("SELECT an.answer_id, an.question_id, COALESCE(asa.answer_text, an.answer_text) AS answer_text, " +
						"an.answer_value, an.display_order, an.crisis, an.call, an.next_question_id " +
						"FROM " +
						"account_session_answer as asa, " +
						"answer as an " +
						"WHERE " +
						"asa.account_session_id = ? AND " +
						"asa.answer_id = an.answer_id AND " +
						"asa.answer_id IN (SELECT answer_id FROM answer WHERE question_id = ?)",
				Answer.class, accountSession.getAccountSessionId(), question.getQuestionId());
	}

	@Nonnull
	public SessionProgress getProgressForSession(Assessment assessment, Question question) {

		// Supreme hack just to get the bar to behave for launch

		switch (assessment.getAssessmentTypeId()) {

			case INTRO:
				return new SessionProgress(question.getDisplayOrder() - 1, 3);

			case PHQ4:
				return new SessionProgress(question.getDisplayOrder() - 1, 4);

			case PHQ9:
				return new SessionProgress(4 + question.getDisplayOrder() - 1, 21);

			case GAD7:
				return new SessionProgress(11 + question.getDisplayOrder() - 1, 21);

			case PCPTSD:
				return new SessionProgress(16 + question.getDisplayOrder() - 1, 21);

			default:
				return new SessionProgress(0, 1);
		}
	}

	@Nonnull
	public Boolean doesAccountSessionHaveAnswers(@Nonnull UUID accountSessionId) {
		return database.queryForObject("SELECT COUNT(*) > 0 FROM account_session_answer WHERE " +
				"account_session_id = ?", Boolean.class, accountSessionId).get();
	}


	public static class SessionProgress {
		private int progress;
		private int total;

		public SessionProgress(int progress, int total) {
			this.progress = progress;
			this.total = total;
		}

		public int getProgress() {
			return progress;
		}

		public int getTotal() {
			return total;
		}
	}

}
