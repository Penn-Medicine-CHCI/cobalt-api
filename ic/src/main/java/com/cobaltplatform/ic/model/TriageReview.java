package com.cobaltplatform.ic.model;

import com.cobaltplatform.ic.backend.model.db.DTriageReview;
import org.joda.time.DateTime;

public class TriageReview {
    private DateTime bhpReviewedDt;
    private DateTime psychiatristReviewedDt;
    private String comment;
    private Boolean needsFocusedReview;

    public DateTime getBhpReviewedDt() {
        return bhpReviewedDt;
    }

    public TriageReview setBhpReviewedDt(final DateTime bhpReviewedDt) {
        this.bhpReviewedDt = bhpReviewedDt;
        return this;
    }

    public DateTime getPsychiatristReviewedDt() {
        return psychiatristReviewedDt;
    }

    public TriageReview setPsychiatristReviewedDt(final DateTime psychiatristReviewedDt) {
        this.psychiatristReviewedDt = psychiatristReviewedDt;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public TriageReview setComment(final String comment) {
        this.comment = comment;
        return this;
    }

    public Boolean getNeedsFocusedReview() {
        return needsFocusedReview;
    }

    public TriageReview setNeedsFocusedReview(final Boolean needsFocusedReview) {
        this.needsFocusedReview = needsFocusedReview;
        return this;
    }

    public static TriageReview fromDTriageReview(DTriageReview triageReview){
        return new TriageReview()
                .setNeedsFocusedReview(triageReview.getNeedsFocusedReview())
                .setComment(triageReview.getComment())
                .setBhpReviewedDt(triageReview.getBhpReviewedDt())
                .setPsychiatristReviewedDt(triageReview.getPsychiatristReviewedDt());
    }

}
