package com.cobaltplatform.ic.backend.model.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.joda.time.DateTime;

@Entity
@Table(name="triage_review", schema = "ic")
public class DTriageReview extends DBaseModel {
    private DateTime bhpReviewedDt;
    private DateTime psychiatristReviewedDt;
    @Column(length = 10_000)
    private String comment;
    private Boolean needsFocusedReview;

    public DPatientDisposition getDisposition() { return disposition; }

    public DTriageReview setDisposition(DPatientDisposition disposition) {
        this.disposition = disposition;
        return this;
    }

    @OneToOne
    private DPatientDisposition disposition;


    public DateTime getBhpReviewedDt() {
        return bhpReviewedDt;
    }

    public DTriageReview setBhpReviewedDt(final DateTime bhpReviewedDt) {
        this.bhpReviewedDt = bhpReviewedDt;
        return this;
    }

    public DateTime getPsychiatristReviewedDt() {
        return psychiatristReviewedDt;
    }

    public DTriageReview setPsychiatristReviewedDt(final DateTime psychiatristReviewedDt) {
        this.psychiatristReviewedDt = psychiatristReviewedDt;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public DTriageReview setComment(final String comment) {
        this.comment = comment;
        return this;
    }

    public Boolean getNeedsFocusedReview() {
        return needsFocusedReview;
    }

    public DTriageReview setNeedsFocusedReview(final Boolean needsFocusedReview) {
        this.needsFocusedReview = needsFocusedReview;
        return this;
    }
}
