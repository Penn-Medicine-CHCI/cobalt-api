package com.cobaltplatform.ic.backend.controller;

import com.cobaltplatform.ic.backend.model.auth.IcContext;
import com.cobaltplatform.ic.backend.model.db.DAssessment;
import com.cobaltplatform.ic.model.IcRole;
import com.cobaltplatform.ic.backend.service.AssessmentService;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class AssessmentController {
    private static final Logger logger = LoggerFactory.getLogger(AssessmentController.class);

    public static Handler postResponses = ctx -> {
        UUID assessmentID = ctx.pathParam("id", UUID.class).get();
        DAssessment assessment = AssessmentService.getAssessmentById(assessmentID).orElseThrow(NotFoundResponse::new);

        verifyAssessmentIsAccessible(assessment);

        QuestionnaireResponse response = ctx.bodyAsClass(QuestionnaireResponse.class);

        AssessmentService.saveResponses(assessment, response);

        ctx.status(HttpStatus.OK_200);
    };

    public static Handler getResponses = ctx -> {
        UUID assessmentID = ctx.pathParam("id", UUID.class).get();
        DAssessment assessment = AssessmentService.getAssessmentById(assessmentID).orElseThrow(NotFoundResponse::new);

        verifyAssessmentIsAccessible(assessment);

        QuestionnaireResponse response = AssessmentService.loadQuestionnaireResponse(assessmentID);
        ctx.json(response);
    };

    public static Handler getQuestionnaire = ctx -> {
        UUID assessmentID = ctx.pathParam("id", UUID.class).get();
        DAssessment assessment = AssessmentService.getAssessmentById(assessmentID).orElseThrow(NotFoundResponse::new);

        verifyAssessmentIsAccessible(assessment);

        Questionnaire questionnaire = AssessmentService.loadQuestionnaire(assessment);
        ctx.json(questionnaire);
    };

    public static Handler putComplete = ctx -> {
        // TODO: Don't allow doing this if assessment is incomplete
        UUID assessmentID = ctx.pathParam("id", UUID.class).get();
        DAssessment assessment = AssessmentService.getAssessmentById(assessmentID).orElseThrow(NotFoundResponse::new);

        verifyAssessmentIsAccessible(assessment);

        AssessmentService.completeAssessment(assessment);
        ctx.status(HttpStatus.OK_200);
    };

    private static void verifyAssessmentIsAccessible(@Nullable DAssessment assessment) {
        if(assessment == null)
            throw new NotFoundResponse();

        if(!IcContext.currentContextHasRole(IcRole.MHIC) && !IcContext.currentContextHasPatientId(assessment.getPatient().getId()))
            throw new NotFoundResponse();
    }
}
