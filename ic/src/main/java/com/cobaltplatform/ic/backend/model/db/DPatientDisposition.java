package com.cobaltplatform.ic.backend.model.db;

import com.cobaltplatform.ic.model.AcuityCategory;
import com.cobaltplatform.ic.model.DispositionFlag;
import com.cobaltplatform.ic.model.DispositionOutcomeDiagnosis;
import io.ebean.annotation.DbDefault;

import javax.persistence.*;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "patient_disposition", schema = "ic")
public class DPatientDisposition extends DBaseModel {

    @ManyToOne
    private DPatient patient;

    private DispositionOutcomeDiagnosis diagnosis;

    private boolean crisis = false;

    @Column(nullable = false)
    @DbDefault("11") // not yet screened
    private DispositionFlag flag;

    @OneToOne(mappedBy = "disposition")
    private DTriageReview triageReview;

    @OneToOne(mappedBy = "disposition")
    private DAssessment assessment;

    @OneToOne(mappedBy = "disposition")
    private DSpecialtyCareScheduling specialtyCareScheduling;

    @OneToMany(mappedBy = "disposition")
    private List<DReferralOrderReport> orders;

    @OneToMany(mappedBy = "disposition")
    private List<DDispositionNote> dispositionNotes;

    public List<DContact> getContact() {
        return contact;
    }

    public void setContact(List<DContact> contact) {
        this.contact = contact;
    }

    @OneToMany(mappedBy="disposition")
    private List<DContact> contact;

    private AcuityCategory acuityCategory;

    public boolean isDigital() {
        return isDigital;
    }

    public DPatientDisposition setDigital(boolean digital) {
        isDigital = digital;
        return this;
    }

    @DbDefault("true")
    private boolean isDigital;

    @DbDefault("false")
    private boolean crisisAcknowledged;

    public DPatient getPatient() {
        return patient;
    }

    public DPatientDisposition setPatient(final DPatient patient) {
        this.patient = patient;
        return this;
    }

    public DispositionOutcomeDiagnosis getDiagnosis() {
        return diagnosis;
    }

    public DPatientDisposition setDiagnosis(DispositionOutcomeDiagnosis diagnosis) {
        this.diagnosis = diagnosis;
        return this;
    }

    public DispositionFlag getFlag() {
        return flag;
    }

    public DPatientDisposition setFlag(final DispositionFlag flag) {
        this.flag = flag;
        return this;
    }

    public Optional<DTriageReview> getTriageReview() {
        return Optional.ofNullable(triageReview);
    }

    public DPatientDisposition setTriageReview(final DTriageReview triageReview) {
        this.triageReview = triageReview;
        return this;
    }

    public AcuityCategory getAcuityCategory() {
        return acuityCategory;
    }

    public DPatientDisposition setAcuityCategory(final AcuityCategory acuityCategory) {
        this.acuityCategory = acuityCategory;
        return this;
    }

    public List<DReferralOrderReport> getOrders(){
        return this.orders;
    }

    public DPatientDisposition setOrders(final List<DReferralOrderReport> orders) {
        this.orders = orders;
        return this;
    }

    public DPatientDisposition addOrder(final DReferralOrderReport order) {
        this.orders.add(order);
        return this;
    }

    public DPatientDisposition setCrisis(final boolean crisis){
        this.crisis = crisis;
        return this;
    }

    public boolean isCrisis(){
        return this.crisis;
    }

    public DPatientDisposition setCrisisAcknowledged(final boolean crisisAcknowledged){
        this.crisisAcknowledged = crisisAcknowledged;
        return this;
    }

    public boolean isCrisisAcknowledged(){
        return this.crisisAcknowledged;
    }

    public DSpecialtyCareScheduling getSpecialtyCareScheduling() {
        return specialtyCareScheduling;
    }

    public DPatientDisposition setSpecialtyCareScheduling(DSpecialtyCareScheduling specialtyCareScheduling) {
        this.specialtyCareScheduling = specialtyCareScheduling;
        return this;
    }

    public DAssessment getAssessment() {
        return assessment;
    }

    public void setAssessment(DAssessment assessment) {
        this.assessment = assessment;
    }

    public List<DDispositionNote> getDispositionNotes() {
        return dispositionNotes;
    }

    public void setDispositionNotes(List<DDispositionNote> dispositionNotes) {
        this.dispositionNotes = dispositionNotes;
    }
}


