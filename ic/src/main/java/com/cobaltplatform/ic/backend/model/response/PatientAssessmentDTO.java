package com.cobaltplatform.ic.backend.model.response;

import com.cobaltplatform.ic.model.AssessmentStatus;
import com.cobaltplatform.ic.backend.model.db.DAssessment;
import org.joda.time.DateTime;

import java.util.UUID;

public class PatientAssessmentDTO {
    private UUID id;
    private AssessmentStatus status;
    private DateTime due;
    private String authoredBy;

    public static PatientAssessmentDTO forAssessment(DAssessment assessment){
        PatientAssessmentDTO dto = new PatientAssessmentDTO();
        dto.id = assessment.getId();
        dto.status = assessment.getStatus();
        dto.due = assessment.getDue();
        dto.authoredBy = assessment.getAuthoredBy();
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public AssessmentStatus getStatus() {
        return status;
    }

    public DateTime getDue() {
        return due;
    }

    public String getAuthoredBy() {
        return authoredBy;
    }
}
