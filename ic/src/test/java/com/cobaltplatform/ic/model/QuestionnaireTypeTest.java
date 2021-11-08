package com.cobaltplatform.ic.model;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import com.cobaltplatform.ic.backend.model.db.DAssessment;
import com.cobaltplatform.ic.backend.model.db.DPatient;
import com.cobaltplatform.ic.backend.model.db.DResponseItem;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionnaireTypeTest {
    FhirContext context = FhirContext.forCached(FhirVersionEnum.R4);

    @Test
    void scoreAuditC_LowMale_MediumFemale() {
        DPatient malePatient = new DPatient().setPreferredGender("male");
        DPatient femalePatient = new DPatient().setPreferredGender("female");
        DAssessment assessment = new DAssessment();

        DResponseItem item1 = new DResponseItem(assessment, "/68518-0")
                .setCodingValue(
                        new Coding().setCode("LA18926-8")
                                .setDisplay("Monthly or less"));

        DResponseItem item2 = new DResponseItem(assessment, "/68519-8")
                .setCodingValue(
                        new Coding().setCode("LA18930-0")
                                .setDisplay("5 or 6"));

        DResponseItem item3 = new DResponseItem(assessment, "/68520-6")
                .setCodingValue(
                        new Coding().setCode("LA6270-8")
                                .setDisplay("Never"));

        Map<String, List<DResponseItem>> items = List.of(item1, item2, item3).stream().collect(Collectors.groupingBy(DResponseItem::getLinkId));
        
        QuestionnaireScoring maleScore = QuestionnaireType.AUDITC.score(items, malePatient).get();
        QuestionnaireScoring femaleScore = QuestionnaireType.AUDITC.score(items, femalePatient).get();

        assertAll(
                () -> assertEquals(new QuestionnaireScoring(3, AcuityCategory.LOW), maleScore),
                () -> assertEquals(new QuestionnaireScoring(3, AcuityCategory.MEDIUM), femaleScore)
        );
    }

    @Test
    void scoreAuditC_LowBoth() {
        DPatient malePatient = new DPatient().setPreferredGender("male");
        DPatient femalePatient = new DPatient().setPreferredGender("female");
        DAssessment assessment = new DAssessment();

        DResponseItem item1 = new DResponseItem(assessment, "/68518-0")
                .setCodingValue(
                        new Coding().setCode("LA6270-8")
                                .setDisplay("Never"));

        Map<String, List<DResponseItem>> items = List.of(item1).stream().collect(Collectors.groupingBy(DResponseItem::getLinkId));

        QuestionnaireScoring maleScore = QuestionnaireType.AUDITC.score(items, malePatient).get();
        QuestionnaireScoring femaleScore = QuestionnaireType.AUDITC.score(items, femalePatient).get();

        assertAll(
            () -> assertEquals(new QuestionnaireScoring(0, AcuityCategory.LOW), maleScore),
            () -> assertEquals(new QuestionnaireScoring(0, AcuityCategory.LOW), femaleScore)
        );
    }

    @Test
    void noHighAcuityISI() {
        DPatient patient = new DPatient();
        DAssessment assessment = new DAssessment();

        DResponseItem item1 = new DResponseItem(assessment, "/65502-7")
                .setCodingValue(
                        new Coding().setCode("LA13958-6")
                                .setDisplay("Very severe"));
        DResponseItem item2 = new DResponseItem(assessment, "/65503-5")
                .setCodingValue(
                        new Coding().setCode("LA13958-6")
                                .setDisplay("Very severe"));
        DResponseItem item3 = new DResponseItem(assessment, "/65504-3")
                .setCodingValue(
                        new Coding().setCode("LA13958-6")
                                .setDisplay("Very severe"));
        DResponseItem item4 = new DResponseItem(assessment, "/ISI-SAT")
                .setCodingValue(
                        new Coding().setCode("LA13958-6")
                                .setDisplay("Very Dissatisfied"));

        Map<String, List<DResponseItem>> items = List.of(item1, item2, item3, item4).stream().collect(Collectors.groupingBy(DResponseItem::getLinkId));

        QuestionnaireScoring scoreISI = QuestionnaireType.ISI.score(items, patient).get();

        assertEquals(new QuestionnaireScoring(16, AcuityCategory.MEDIUM), scoreISI);
    }

    @Test
    void positiveOpioidQuestionnaire() {
        DPatient patient = new DPatient();
        DAssessment assessment = new DAssessment();

        DResponseItem item1 = new DResponseItem(assessment, "/ic-opioid-1")
                .setCodingValue(
                        new Coding().setCode(LOINCAnswerCode.YES.getAnswerId())
                                .setDisplay("Yes"));

        Map<String, List<DResponseItem>> items = List.of(item1).stream().collect(Collectors.groupingBy(DResponseItem::getLinkId));

        QuestionnaireScoring score = QuestionnaireType.OPIOIDSCREEN.score(items, patient).get();

        assertEquals(new QuestionnaireScoring(1, null), score);
    }

    private QuestionnaireResponse loadResponse(String path){
        IParser parser = context.newJsonParser();
        return parser.parseResource(QuestionnaireResponse.class, this.getClass().getResourceAsStream(path));
    }
}