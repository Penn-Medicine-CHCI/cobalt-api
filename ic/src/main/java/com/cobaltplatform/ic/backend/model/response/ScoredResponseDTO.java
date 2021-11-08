package com.cobaltplatform.ic.backend.model.response;

import com.cobaltplatform.ic.model.AcuityCategory;
import com.cobaltplatform.ic.model.QuestionnaireType;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

public class ScoredResponseDTO {
    QuestionnaireResponse response;
    Long score;
    AcuityCategory acuityCategory;
    QuestionnaireType questionnaireType;

    public ScoredResponseDTO(QuestionnaireType questionnaireType, QuestionnaireResponse response){
        this.questionnaireType = questionnaireType;
        this.response = response;
    }

    public ScoredResponseDTO setScore(long score){
        this.score = score;
        return this;
    }

    public ScoredResponseDTO setAcuity(AcuityCategory acuity){
        this.acuityCategory = acuity;
        return this;
    }


    public String getQuestionnaireType(){
        return this.questionnaireType.getLinkId();
    }

    public QuestionnaireResponse getResponse(){
        return this.response;
    }

    public Long getScore(){
        return this.score;
    }

    public AcuityCategory getAcuityCategory(){
        return this.acuityCategory;
    }


}
