package com.cobaltplatform.ic.backend.model.db;

import com.cobaltplatform.ic.model.AcuityCategory;
import com.cobaltplatform.ic.model.QuestionnaireType;

import javax.persistence.*;

@Entity
@Table(name = "scoring", schema = "ic")
public class DScoring extends DBaseModel {
    @ManyToOne
    private DAssessment assessment;

    private long score;

    private AcuityCategory acuity;

    @Enumerated(EnumType.STRING)
    private QuestionnaireType questionnaireType;

    public DAssessment getAssessment() {
        return assessment;
    }

    public DScoring setAssessment (final DAssessment assessment) {
        this.assessment = assessment;
        return this;
    }

    public QuestionnaireType getQuestionnaireType() {
        return this.questionnaireType;
    }

    public DScoring setQuestionnaireType(QuestionnaireType questionnaireType){
        this.questionnaireType = questionnaireType;
        return this;
    }

    public long getScore() {
        return score;
    }

    public DScoring setScore(final long score) {
        this.score = score;
        return this;
    }

    public AcuityCategory getAcuity() {
        return acuity;
    }

    public DScoring setAcuity(final AcuityCategory acuity) {
        this.acuity = acuity;
        return this;
    }

}
