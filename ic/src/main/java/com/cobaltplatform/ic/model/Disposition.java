package com.cobaltplatform.ic.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.cobaltplatform.ic.backend.model.serialize.ReviewedBy;

public class Disposition {
    private String patientID;
    private String createdDate;
    private ReviewedBy[] reviewedBy;
    private String status;
    private String outcome;

    @JsonProperty("patientID")
    public String getPatientID() {
        return patientID;
    }

    @JsonProperty("patientID")
    public void setPatientID(String value) {
        this.patientID = value;
    }

    @JsonProperty("createdDate")
    public String getCreatedDate() {
        return createdDate;
    }

    @JsonProperty("createdDate")
    public void setCreatedDate(String value) {
        this.createdDate = value;
    }

    @JsonProperty("reviewedBy")
    public ReviewedBy[] getReviewedBy() {
        return reviewedBy;
    }

    @JsonProperty("reviewedBy")
    public void setReviewedBy(ReviewedBy[] value) {
        this.reviewedBy = value;
    }

    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    @JsonProperty("status")
    public void setStatus(String value) {
        this.status = value;
    }

    @JsonProperty("outcome")
    public String getOutcome() {
        return outcome;
    }

    @JsonProperty("outcome")
    public void setOutcome(String value) {
        this.outcome = value;
    }
}
