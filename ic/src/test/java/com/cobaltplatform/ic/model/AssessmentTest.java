package com.cobaltplatform.ic.model;

import com.cobaltplatform.ic.backend.model.db.DAssessment;
import com.cobaltplatform.ic.backend.model.db.DPatient;
import com.cobaltplatform.ic.backend.model.db.DResponseItem;
import com.cobaltplatform.ic.backend.model.db.DScoring;
import org.hl7.fhir.r4.model.Coding;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


public class AssessmentTest {
	@Test
	public void singleResponseOverallAcuity() {
		DScoring scoring = new DScoring()
				.setQuestionnaireType(QuestionnaireType.DAST10)
				.setAcuity(AcuityCategory.HIGH);

		var assessment = new Assessment(new DPatient(), Collections.emptyMap(), List.of(scoring));
		assertEquals(AcuityCategory.HIGH, assessment.getOverallAcuity().orElseThrow());
	}

	@Test
	public void multipleResponseOverallAcuity() {
		DScoring scoring = new DScoring()
				.setQuestionnaireType(QuestionnaireType.DAST10)
				.setAcuity(AcuityCategory.LOW);

		DScoring scoring2 = new DScoring()
				.setQuestionnaireType(QuestionnaireType.AUDITC)
				.setAcuity(AcuityCategory.MEDIUM);

		var assessment = new Assessment(new DPatient(), Collections.emptyMap(), List.of(scoring, scoring2));
		assertEquals(AcuityCategory.MEDIUM, assessment.getOverallAcuity().orElseThrow());
	}

	@Test
	public void skipNonClinicalInOverallAcuity() {
		DScoring qResponse1 = new DScoring()
				.setQuestionnaireType(QuestionnaireType.PREPTSD)
				.setAcuity(AcuityCategory.HIGH);

		DScoring qResponse2 = new DScoring()
				.setQuestionnaireType(QuestionnaireType.AUDITC)
				.setAcuity(AcuityCategory.MEDIUM);

		var assessment = new Assessment(new DPatient(), Collections.emptyMap(), List.of(qResponse1, qResponse2));
		assertEquals(AcuityCategory.MEDIUM, assessment.getOverallAcuity().orElseThrow());
	}

	@Test
	public void nullResponseOverallAcuity() {
		DScoring qResponse1 = new DScoring()
				.setQuestionnaireType(QuestionnaireType.PRIME5);

		var assessment = new Assessment(new DPatient(), Collections.emptyMap(), List.of(qResponse1));
		assertFalse(assessment.getOverallAcuity().isPresent());
	}

	@Test
	public void getDiagnosis_ALCOHOL_USE_DISORDER() {
		var patient = new DPatient().setPreferredGender("female");
		DAssessment dassessment = new DAssessment();

		DResponseItem item1 = new DResponseItem(dassessment, "/68518-0")
				.setCodingValue(
						new Coding().setCode("LA18926-8")
								.setDisplay("Monthly or less"));

		DResponseItem item2 = new DResponseItem(dassessment, "/68519-8")
				.setCodingValue(
						new Coding().setCode("LA18930-0")
								.setDisplay("5 or 6"));

		DResponseItem item3 = new DResponseItem(dassessment, "/68520-6")
				.setCodingValue(
						new Coding().setCode("LA18934-2")
								.setDisplay("Daily or almost daily"));

		Assessment assessment = new Assessment(patient, List.of(item1, item2, item3).stream().collect(Collectors.groupingBy(DResponseItem::getLinkId)));

		assertEquals(DispositionOutcomeDiagnosis.ALCOHOL_USE_DISORDER, assessment.getDiagnosis().orElseThrow());
	}

	@Test
	public void getDiagnosis_SELF_DIRECTED_DEFAULT() {
		Assessment myAssessment = new Assessment(new DPatient(), Collections.emptyMap(), Collections.emptyList());

		assertEquals(DispositionOutcomeDiagnosis.SELF_DIRECTED, myAssessment.getDiagnosis().orElseThrow());
	}

	@Test
	public void getDiagnosis_SELF_DIRECTED_LOW_OVERALL() {
		DScoring ptsdSavedResponse = new DScoring()
				.setQuestionnaireType(QuestionnaireType.PTSD5)
				.setAcuity(AcuityCategory.LOW)
				.setScore(0L);


		var assessment = new Assessment(new DPatient(), Collections.emptyMap(), List.of(ptsdSavedResponse));

		assertEquals(DispositionOutcomeDiagnosis.SELF_DIRECTED, assessment.getDiagnosis().orElseThrow());
	}

	@Test
	public void getDiagnosis_TRAUMA_BEFORE_ALCOHOL_USE_DISORDER() {
		var assessment = new DAssessment();

		DScoring ptsdSavedResponse = new DScoring()
				.setQuestionnaireType(QuestionnaireType.PTSD5)
				.setAcuity(AcuityCategory.HIGH)
				.setScore(0L)
				.setAssessment(assessment);

		DScoring auditCScoring = new DScoring()
				.setQuestionnaireType(QuestionnaireType.AUDITC)
				.setAcuity(AcuityCategory.HIGH)
				.setScore(7)
				.setAssessment(assessment);

		Assessment myAssessment = new Assessment(new DPatient(), Collections.emptyMap(), List.of(ptsdSavedResponse, auditCScoring));

		assertEquals(DispositionOutcomeDiagnosis.TRAUMA, myAssessment.getDiagnosis().orElseThrow());
	}

	@org.junit.jupiter.api.Test
	public void getDiagnosis_GRIEF() {
		var patient = new DPatient();
		var assessment = new DAssessment();

		List<DResponseItem> responses = new ArrayList<>();

		responses.add(new DResponseItem(assessment, "/symptom/grief").setBooleanValue(true));
		responses.add(new DResponseItem(assessment, "/69725-0")
						.setCodingValue(new Coding().setCode("LA6570-1").setDisplay("More than half the days")));
		responses.add(new DResponseItem(assessment, "/68509-9")
					.setCodingValue(new Coding().setCode("LA18938-3").setDisplay("More days than not")));
		responses.add(new DResponseItem(assessment, "/69733-4")
						.setCodingValue(new Coding().setCode("LA6570-1").setDisplay("More than half the days")));
		responses.add(new DResponseItem(assessment, "/69734-2")
						.setCodingValue(new Coding().setCode("LA6570-1").setDisplay("More than half the days")));
		responses.add(new DResponseItem(assessment, "/69734-2")
						.setCodingValue(new Coding().setCode("LA6568-5").setDisplay("Not at all")));
		responses.add(new DResponseItem(assessment, "/69689-8")
						.setCodingValue(new Coding().setCode("LA6568-5").setDisplay("Not at all")));
		responses.add(new DResponseItem(assessment, "/69736-7")
						.setCodingValue(new Coding().setCode("LA6568-5").setDisplay("Not at all")));


		Assessment myAssessment = new Assessment(patient, responses.stream().collect(Collectors.groupingBy(DResponseItem::getLinkId)));

		assertEquals(DispositionOutcomeDiagnosis.GRIEF, myAssessment.getDiagnosis().orElseThrow());
	}

	@Test
	public void getDiagnosis_OPIOID() {
		var patient = new DPatient();
		var assessment = new DAssessment();

		DScoring opiodScoring = new DScoring()
				.setQuestionnaireType(QuestionnaireType.OPIOIDSCREEN)
				.setScore(1L)
				.setAssessment(assessment);

		DScoring dastScoring = new DScoring()
				.setQuestionnaireType(QuestionnaireType.DAST10)
				.setAcuity(AcuityCategory.HIGH)
				.setScore(7)
				.setAssessment(assessment);

		Assessment myAssessment = new Assessment(patient, Collections.emptyMap(), List.of(opiodScoring, dastScoring));

		assertEquals(DispositionOutcomeDiagnosis.OPIOID_USE_DISORDER, myAssessment.getDiagnosis().orElseThrow());
	}

	@Test
	public void getDiagnosis_NO_OPIOID_IF_DAST_LOW() {
		var patient = new DPatient();
		var assessment = new DAssessment();

		DScoring opiodScoring = new DScoring()
				.setQuestionnaireType(QuestionnaireType.OPIOIDSCREEN)
				.setScore(1L)
				.setAssessment(assessment);

		DScoring dastScoring = new DScoring()
				.setQuestionnaireType(QuestionnaireType.DAST10)
				.setAcuity(AcuityCategory.LOW)
				.setScore(0)
				.setAssessment(assessment);

		Assessment myAssessment = new Assessment(patient, Collections.emptyMap(), List.of(opiodScoring, dastScoring));

		assertNotEquals(DispositionOutcomeDiagnosis.OPIOID_USE_DISORDER, myAssessment.getDiagnosis().orElseThrow());
	}

	@Test
	public void getDiagnosis_DOESNT_THROW_ON_NO_MATCH() {
		var patient = new DPatient();
		var assessment = new DAssessment();

		DResponseItem item = new DResponseItem(assessment, "BadLink");

		Assessment myAssessment = new Assessment(patient, Map.of("BadLink", List.of(item)));

		assertEquals(DispositionOutcomeDiagnosis.SELF_DIRECTED, myAssessment.getDiagnosis().orElseThrow());
	}

}