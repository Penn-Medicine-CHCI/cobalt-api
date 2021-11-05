package com.cobaltplatform.ic.backend.service;

import com.cobaltplatform.ic.model.LOINCAnswerCode;
import com.cobaltplatform.ic.model.QuestionnaireScoring;
import com.cobaltplatform.ic.model.QuestionnaireType;
import com.cobaltplatform.ic.backend.model.db.v2.Assessment;
import com.cobaltplatform.ic.backend.model.db.v2.Patient;
import com.cobaltplatform.ic.backend.model.db.v2.ResponseItem;
import com.cobaltplatform.ic.backend.model.db.v2.Scoring;
import com.pyranid.Database;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AssessmentScoringService {
	@Nonnull
	private final Database database;

	public AssessmentScoringService(@Nonnull Database database) {
		requireNonNull(database);
		this.database = database;
	}

	@Nonnull
	public Map<QuestionnaireType, QuestionnaireScoring> determineQuestionnaireScoringByType(@Nonnull Patient patient,
																																													@Nonnull Map<String, List<ResponseItem>> responsesByLinkId) {
		requireNonNull(patient);
		requireNonNull(responsesByLinkId);

		List<QuestionnaireType> scorableQuestionnaireTypes = Arrays.stream(QuestionnaireType.values())
				.filter(questionnaireType -> (questionnaireType.isClinical() && includeInAssessment(questionnaireType, responsesByLinkId)) || questionnaireType == QuestionnaireType.CSSRS)
				.collect(Collectors.toList());

		Map<QuestionnaireType, QuestionnaireScoring> questionnaireScoringByType = scorableQuestionnaireTypes
				.stream()
				.map(questionnaireType -> Map.entry(questionnaireType, scoreQuestionnaire(questionnaireType, responsesByLinkId, patient)))
				.filter(entry -> entry.getValue().isPresent())
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get(), (l, r) -> {
					throw new IllegalArgumentException("Duplicate keys " + l + "and " + r + ".");
				}, () -> new EnumMap<>(QuestionnaireType.class)));

		return questionnaireScoringByType;
	}

	protected Optional<QuestionnaireScoring> scoreQuestionnaire(@Nonnull QuestionnaireType questionnaireType,
																															@Nonnull Map<String, List<ResponseItem>> responsesByLinkId,
																															@Nonnull Patient patient) {
		requireNonNull(questionnaireType);
		requireNonNull(responsesByLinkId);
		requireNonNull(patient);

		switch (questionnaireType) {
			case INFO:
			case SYMPTOMS:
			case DIAGNOSES:
			case MILITARY:
			case CSSRS_SHORT:
				return Optional.empty();
//			case CSSRS:
//				long score = QuestionnaireScoring.countYesResponses(this.getQuestions().stream()
//						.filter(q -> responses.containsKey(q.getLinkId()))
//						.map(q -> responses.get(q.getLinkId()).get(0))
//						.collect(Collectors.toList()));
//				AcuityCategory ac = score > 0 ? AcuityCategory.HIGH : AcuityCategory.LOW;
//
//				return Optional.of(new QuestionnaireScoring(score, ac));
			default:
				throw new UnsupportedOperationException(format("Unexpected questionnaire type '%s'", questionnaireType.name()));
		}
	}

	protected List<QuestionnaireItemComponent> getQuestions() {
		throw new UnsupportedOperationException();
		
//		if(this.questionnaire != null){
//			return this.questionnaire.getItem()
//					.stream()
//					.flatMap(i -> i.getItem().stream())
//					.collect(Collectors.toList());
//		} else {
//			return Collections.emptyList();
//		}
	}

	protected boolean includeInAssessment(@Nonnull QuestionnaireType questionnaireType,
																				@Nonnull Map<String, List<ResponseItem>> responsesByLinkId) {
		requireNonNull(questionnaireType);
		requireNonNull(responsesByLinkId);

		switch (questionnaireType) {
			case INFO:
			case SYMPTOMS:
			case DIAGNOSES:
			case MILITARY:
			case GOALS:
				return false;
			case CSSRS_SHORT:
			case PHQ9:
			case GAD7:
			case ISI:
			case PREPTSD:
			case ASRM:
			case PRIME5:
			case SIMPLEPAIN:
			case SIMPLEDRUGALCOHOL:
				return true;
			case CSSRS:
				return cssrsConcern(responsesByLinkId);
			case PTSD5:
				return ptsdConcern(responsesByLinkId);
			case BPI:
				return painConcern(responsesByLinkId);
			case OPIOIDSCREEN:
			case DAST10:
				return drugConcern(responsesByLinkId);
			case AUDITC:
				return alcoholConcern(responsesByLinkId);
			default:
				throw new UnsupportedOperationException(format("Unexpected questionnaire type '%s'", questionnaireType.name()));
		}
	}

	protected boolean cssrsConcern(@Nonnull Map<String, List<ResponseItem>> responsesByLinkId) {
		requireNonNull(responsesByLinkId);

		return getSingleResponse("/93267-3", responsesByLinkId)
				.flatMap((responseItem -> codingValue(responseItem)))
				.map(coding -> coding.getCode().equals(LOINCAnswerCode.YES.getAnswerId()))
				.orElse(false);
	}

	protected boolean ptsdConcern(@Nonnull Map<String, List<ResponseItem>> responsesByLinkId) {
		requireNonNull(responsesByLinkId);

		return getSingleResponse("/pre-ptsd-1", responsesByLinkId)
				.flatMap(responseItem -> codingValue(responseItem))
				.map(coding -> coding.getCode().equals(LOINCAnswerCode.YES.getAnswerId())).orElse(false);
	}

	protected boolean painConcern(@Nonnull Map<String, List<ResponseItem>> responsesByLinkId) {
		requireNonNull(responsesByLinkId);

		return getSingleResponse("/ic-simplePain-1", responsesByLinkId)
				.flatMap((responseItem -> codingValue(responseItem)))
				.map(coding -> coding.getCode().equals(LOINCAnswerCode.YES.getAnswerId()))
				.orElse(false);
	}

	protected boolean drugConcern(@Nonnull Map<String, List<ResponseItem>> responsesByLinkId) {
		requireNonNull(responsesByLinkId);

		return getSingleResponse("/single-q-drug-screen", responsesByLinkId)
				.flatMap((responseItem -> codingValue(responseItem)))
				.map(coding -> !coding.getCode().equals(LOINCAnswerCode.PC_0.getAnswerId()))
				.orElse(false);
	}

	protected boolean alcoholConcern(@Nonnull Map<String, List<ResponseItem>> responsesByLinkId) {
		requireNonNull(responsesByLinkId);

		return getSingleResponse("/68518-0", responsesByLinkId)
				.flatMap((responseItem -> codingValue(responseItem)))
				.map(coding -> !coding.getCode().equals(LOINCAnswerCode.NEVER.getAnswerId()))
				.orElse(false);
	}

	@Nonnull
	protected Optional<Coding> codingValue(@Nonnull ResponseItem responseItem) {
		requireNonNull(responseItem);

		if (responseItem.getCodeValue() != null)
			return Optional.of(new Coding(responseItem.getCodeSystem(), responseItem.getCodeValue(), responseItem.getStringValue()));

		return Optional.empty();
	}

	@Nonnull
	protected Optional<ResponseItem> getSingleResponse(@Nonnull String linkId,
																										 @Nonnull Map<String, List<ResponseItem>> responsesByLinkId) {
		requireNonNull(linkId);
		requireNonNull(responsesByLinkId);

		List<ResponseItem> responseItems = responsesByLinkId.get(linkId);

		if (responseItems == null)
			return Optional.empty();

		for (ResponseItem responseItem : responseItems)
			if (Objects.equals(responseItem.getLinkId(), linkId))
				return Optional.of(responseItem);

		return Optional.empty();
	}

	@Nonnull
	public Optional<Assessment> findAssessmentById(@Nullable UUID assessmentId) {
		if (assessmentId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM assessment WHERE id=?", Assessment.class, assessmentId);
	}

	@Nonnull
	public List<ResponseItem> findResponseItemsByAssessmentId(@Nullable UUID assessmentId) {
		if (assessmentId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM response_item WHERE assessment_id=? ORDER BY created_dt", ResponseItem.class, assessmentId);
	}

	@Nonnull
	public List<Scoring> findScoringsByAssessmentId(@Nullable UUID assessmentId) {
		if (assessmentId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM scoring WHERE assessment_id=? ORDER BY created_dt", Scoring.class, assessmentId);
	}

	@Nonnull
	public Optional<Assessment> findAssessmentByPatientDispositionId(@Nullable UUID patientDispostionId) {
		if (patientDispostionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM assessment WHERE disposition_id=?", Assessment.class, patientDispostionId);
	}

	@Nonnull
	public Optional<Patient> findPatientById(@Nullable UUID patientId) {
		if (patientId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM patient WHERE id=?", Patient.class, patientId);
	}

	@Nonnull
	protected Database getDatabase() {
		return database;
	}
}
