package com.cobaltplatform.ic.backend.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

public class TriageReviewDTO {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DateTime psychiatristReviewedDt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DateTime bhpReviewedDt;

    private Boolean needsFocusedReview;

    private String comment;

    public TriageReviewDTO() {}

    public DateTime getPsychiatristReviewedDt() {
        return psychiatristReviewedDt;
    }

    public void setPsychiatristReviewedDt(DateTime psychiatristReviewedDt) {
        this.psychiatristReviewedDt = psychiatristReviewedDt;
    }

    public DateTime getBhpReviewedDt() {
        return bhpReviewedDt;
    }

    public void setBhpReviewedDt(DateTime bhpReviewedDt) {
        this.bhpReviewedDt = bhpReviewedDt;
    }

    public Boolean getNeedsFocusedReview() {
        return needsFocusedReview;
    }

    public void setNeedsFocusedReview(Boolean needFocusedReview) {
        this.needsFocusedReview = needFocusedReview;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
