package com.cobaltplatform.ic.backend.service;

import com.cobaltplatform.ic.backend.model.db.DAssessment;
import com.cobaltplatform.ic.backend.model.db.DResponseItem;
import io.ebean.test.ForTests;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AssessmentServiceTest {
    private ForTests.RollbackAll rollbackAll;

    @BeforeEach
    public void before() {
        rollbackAll = ForTests.createRollbackAll();
    }

    @AfterEach
    public void after() {
        rollbackAll.close();
    }

    @Test
    void saveResponses() {
        DAssessment assessment = new DAssessment();
        assessment.save();

        QuestionnaireResponse response = new QuestionnaireResponse();
        response.addItem().setLinkId("Link").addAnswer()
                .setValue(new Coding("System", "Code", "Display"));
        response.addItem().setLinkId("Link2").addAnswer()
                .setValue(new Coding("System2", "Code2", "Display2"));

        AssessmentService.saveResponses(assessment, response);

        DAssessment retrieved = AssessmentService.getAssessmentById(assessment.getId()).orElseThrow();
        Coding firstCoding = retrieved.getResponses().stream().filter(r -> r.getLinkId().equals("Link")).findFirst().flatMap(DResponseItem::getCodingValue).orElseThrow();
        assertAll("Loaded Responses",
                () -> assertTrue(firstCoding.is("System", "Code")),
                () -> assertEquals(2, retrieved.getResponses().size()));
    }

    @Test
    void saveResponses_overwrites() {
        DAssessment assessment = new DAssessment();
        assessment.save();

        QuestionnaireResponse response = new QuestionnaireResponse();
        response.addItem().setLinkId("Link").addAnswer()
                .setValue(new Coding("System", "Code", "Display"));
        response.addItem().setLinkId("Link2").addAnswer()
                .setValue(new Coding("System2", "Code2", "Display2"));

        AssessmentService.saveResponses(assessment, response);
        QuestionnaireResponse response2 = new QuestionnaireResponse();
        response2.addItem().setLinkId("Link").addAnswer()
                .setValue(new Coding("System", "Code3", "Display"));
        response2.addItem().setLinkId("Link2").addAnswer()
                .setValue(new Coding("System2", "Code4", "Display2"));
        AssessmentService.saveResponses(assessment, response2);

        DAssessment retrieved = AssessmentService.getAssessmentById(assessment.getId()).orElseThrow();
        Coding firstCoding = retrieved.getResponses().stream().filter(r -> r.getLinkId().equals("Link")).findFirst().flatMap(DResponseItem::getCodingValue).orElseThrow();;
        assertAll("Loaded Responses",
                () -> assertTrue(firstCoding.is("System", "Code3")),
                () -> assertEquals(2, retrieved.getResponses().size()));
    }

    @Test
    void saveResponses_appends() {
        DAssessment assessment = new DAssessment();
        assessment.save();

        QuestionnaireResponse response = new QuestionnaireResponse();
        response.addItem().setLinkId("Link").addAnswer()
                .setValue(new Coding("System", "Code", "Display"));

        AssessmentService.saveResponses(assessment, response);
        QuestionnaireResponse response2 = new QuestionnaireResponse();
        response2.addItem().setLinkId("Link2").addAnswer()
                .setValue(new Coding("System2", "Code4", "Display2"));
        AssessmentService.saveResponses(assessment, response2);

        DAssessment retrieved = AssessmentService.getAssessmentById(assessment.getId()).orElseThrow();
        Coding firstCoding = retrieved.getResponses().stream().filter(r -> r.getLinkId().equals("Link")).findFirst().orElseThrow().getCodingValue().orElseThrow();;
        Coding secondCoding = retrieved.getResponses().stream().filter(r -> r.getLinkId().equals("Link2")).findFirst().orElseThrow().getCodingValue().orElseThrow();;

        assertAll("Loaded Responses",
                () -> assertTrue(firstCoding.is("System", "Code")),
                () -> assertTrue(secondCoding.is("System2", "Code4")),
                () -> assertEquals(2, retrieved.getResponses().size())
        );
    }

    @Test
    void loadQuestionnaireResponse() {
        DAssessment assessment = new DAssessment();
        assessment.save();

        QuestionnaireResponse response = new QuestionnaireResponse();
        response.addItem().setLinkId("Link").addAnswer()
                .setValue(new Coding("System", "Code", "Display"));
        response.addItem().setLinkId("Link2").addAnswer()
                .setValue(new Coding("System2", "Code2", "Display2"));
        response.addItem().setLinkId("Link3").addAnswer()
                .setValue(new BooleanType(true));
        response.addItem().setLinkId("Link4").addAnswer()
                .setValue(new StringType("Value"));
        QuestionnaireResponse.QuestionnaireResponseItemComponent multiAnswer = response.addItem().setLinkId("Link5");
        multiAnswer.addAnswer().setValue(new StringType("Answer1"));
        multiAnswer.addAnswer().setValue(new StringType("Answer2"));

        AssessmentService.saveResponses(assessment, response);

        QuestionnaireResponse returnedResponse = AssessmentService.loadQuestionnaireResponse(assessment.getId());
        QuestionnaireResponse.QuestionnaireResponseItemComponent item1 = returnedResponse.getItem().stream().filter(i -> i.getLinkId().equals("Link")).findFirst().orElseThrow();
        QuestionnaireResponse.QuestionnaireResponseItemComponent item2 = returnedResponse.getItem().stream().filter(i -> i.getLinkId().equals("Link2")).findFirst().orElseThrow();
        QuestionnaireResponse.QuestionnaireResponseItemComponent item3 = returnedResponse.getItem().stream().filter(i -> i.getLinkId().equals("Link3")).findFirst().orElseThrow();
        QuestionnaireResponse.QuestionnaireResponseItemComponent item4 = returnedResponse.getItem().stream().filter(i -> i.getLinkId().equals("Link4")).findFirst().orElseThrow();
        QuestionnaireResponse.QuestionnaireResponseItemComponent item5 = returnedResponse.getItem().stream().filter(i -> i.getLinkId().equals("Link5")).findFirst().orElseThrow();


        assertAll("Contains Saved Responses",
                () -> assertEquals(5, returnedResponse.getItem().size()),
                () -> assertTrue(item1.getAnswerFirstRep().getValueCoding().is("System", "Code")),
                () -> assertTrue(item2.getAnswerFirstRep().getValueCoding().is("System2", "Code2")),
                () -> assertTrue(item3.getAnswerFirstRep().getValueBooleanType().booleanValue()),
                () -> assertEquals("Value", item4.getAnswerFirstRep().getValueStringType().getValue()),
                () -> assertEquals(Set.of("Answer1", "Answer2"), item5.getAnswer().stream().map(i -> i.getValueStringType().getValue()).collect(Collectors.toSet()))
        );
    }
}