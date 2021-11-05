package com.cobaltplatform.ic.backend.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.DataFormatException;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.model.Questionnaire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Objects;


public class QuestionnaireService {
    private static final FhirContext context = FhirContext.forCached(FhirVersionEnum.R4);
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static Questionnaire getQuestionnaireByName(String questionnaireName) {
        try {
            File file = new File(
                Objects.requireNonNull(QuestionnaireService.class.getClassLoader().getResource(questionnaireName)).getFile());
            return context.newJsonParser().parseResource(Questionnaire.class, FileUtils.openInputStream(file));
        } catch (IOException | DataFormatException e) {
            e.printStackTrace();
        }
        return null;
    }

}
