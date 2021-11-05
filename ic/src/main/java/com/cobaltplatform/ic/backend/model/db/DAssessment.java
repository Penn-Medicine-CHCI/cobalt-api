package com.cobaltplatform.ic.backend.model.db;

import com.cobaltplatform.ic.model.AssessmentStatus;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "assessment", schema = "ic")
public class DAssessment extends DBaseModel {

    @ManyToOne
    private DPatient patient;

    private AssessmentStatus status;

    private DateTime due;

    @OneToMany(mappedBy="assessment", cascade=CascadeType.ALL)
    private List<DResponseItem> responses;

    @OneToMany(mappedBy="assessment", cascade=CascadeType.ALL)
    private List<DScoring> scoring;

    @OneToMany(mappedBy="assessment")
    private List<DAssessmentStatusChange> statusChanges;

    private String authoredBy;

    public DPatient getPatient() {
        return patient;
    }

    public DAssessment setPatient(final DPatient patient) {
        this.patient = patient;
        return this;
    }

    public DPatientDisposition getDisposition() {
        return disposition;
    }

    public DAssessment setDisposition(DPatientDisposition disposition) {
        this.disposition = disposition;
        return this;
    }

    @OneToOne
    private DPatientDisposition disposition;

    public AssessmentStatus getStatus() {
        return status;
    }

    public DAssessment setStatus(final AssessmentStatus status) {
        this.status = status;
        return this;
    }

    public DateTime getDue() {
        return due;
    }

    public DAssessment setDue(final DateTime due) {
        this.due = due;
        return this;
    }

    public String getAuthoredBy() {
        return authoredBy;
    }


    public DAssessment setAuthoredBy(final String authoredBy) {
        this.authoredBy = authoredBy;
        return this;
    }

    public DAssessment setResponses(List<DResponseItem> responses){
        this.responses = responses;
        return this;
    }

    public List<DResponseItem> getResponses(){
        return this.responses;
    }

    public DAssessment setScoring(List<DScoring> scoring){
        this.scoring = scoring;
        return this;
    }

    public List<DScoring> getScoring(){
        return this.scoring;
    }

    public List<DAssessmentStatusChange> getStatusChanges() {
        return statusChanges;
    }

    public void setStatusChanges(List<DAssessmentStatusChange> statusChanges) {
        this.statusChanges = statusChanges;
    }
}
