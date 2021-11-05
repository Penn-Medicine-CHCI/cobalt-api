package com.cobaltplatform.ic.backend.model.response;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cobaltplatform.ic.model.DispositionAcuity;
import com.cobaltplatform.ic.model.DispositionNote;
import com.cobaltplatform.ic.model.DispositionOutcome;
import com.cobaltplatform.ic.model.ShortPatientInfo;
import com.cobaltplatform.ic.model.SpecialtyCareScheduling;
import com.cobaltplatform.ic.model.TriageReview;
import com.cobaltplatform.ic.backend.model.db.DPatientDisposition;
import org.joda.time.DateTime;

public class DispositionResponse {
    //TODO: pare down to patientId First name, last name, preferred phone number, preferred engagement
    private ShortPatientInfo patient;
    private DispositionOutcome outcome;
    private FlagResponse flag;
    private TriageReview triageReview;
    private DispositionAcuity acuity;
    private SpecialtyCareScheduling specialtyCareScheduling;
    private List<DispositionNote> notes;
    private boolean crisisAcknowledged;
    private DateTime createdAt;

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public DispositionResponse setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public List<OrderResponse> getOrders() {
        return orders;
    }

    public DispositionResponse setOrders(List<OrderResponse> orders) {
        this.orders = orders;
        return this;
    }

    private List<OrderResponse> orders;

    public List<ContactDTO> getContact() {
        return contact;
    }

    public DispositionResponse setContact(List<ContactDTO> contact) {
        this.contact = contact;
        return this;
    }

    private List<ContactDTO> contact;
    private UUID id;

    public UUID getId() {
        return id;
    }

    public DispositionResponse setId(final UUID id) {
        this.id = id;
        return this;
    }

    public ShortPatientInfo getPatient() {
        return patient;
    }

    public DispositionResponse setPatient(final ShortPatientInfo patient) {
        this.patient = patient;
        return this;
    }

    public DispositionOutcome getOutcome() {
        return outcome;
    }

    public DispositionResponse setOutcome(final DispositionOutcome outcome) {
        this.outcome = outcome;
        return this;
    }

    public FlagResponse getFlag() {
        return flag;
    }

    public DispositionResponse setFlag(final FlagResponse flag) {
        this.flag = flag;
        return this;
    }

    public TriageReview getTriageReview() {
        return triageReview;
    }

    public DispositionResponse setTriageReview(final TriageReview triageReview) {
        this.triageReview = triageReview;
        return this;
    }

    public DispositionAcuity getAcuity() {
        return acuity;
    }

    public DispositionResponse setAcuity(final DispositionAcuity acuity) {
        this.acuity = acuity;
        return this;
    }

    public SpecialtyCareScheduling getSpecialtyCareScheduling() {
        return specialtyCareScheduling;
    }

    public DispositionResponse setSpecialtyCareScheduling(SpecialtyCareScheduling specialtyCareScheduling) {
        this.specialtyCareScheduling = specialtyCareScheduling;
        return this;
    }

    public boolean isCrisisAcknowledged() {
        return crisisAcknowledged;
    }

    public DispositionResponse setCrisisAcknowledged(final boolean crisisAcknowledged) {
        this.crisisAcknowledged = crisisAcknowledged;
        return this;
    }

    public List<DispositionNote> getNotes() {
        return notes;
    }

    public DispositionResponse setNotes(List<DispositionNote> notes) {
        this.notes = notes;
        return this;
    }

    public static DispositionResponse fromDPatientDisposition(DPatientDisposition disposition) {
        //TODO: get from the referral
        DispositionResponse response = new DispositionResponse()
                .setCreatedAt(disposition.getCreatedDt())
                .setId(disposition.getId())
                .setPatient(new ShortPatientInfo()
                        .setCobaltAccountId(disposition.getPatient().getCobaltAccountId())
                        .setFirstName(disposition.getPatient().getPreferredFirstName())
                        .setLastName(disposition.getPatient().getPreferredLastName())
                        .setId(disposition.getPatient().getId())
                        .setPreferredPhoneNumber(disposition.getPatient().getPreferredPhoneNumber())
                        .setPreferredEngagement(null)) //TODO: get from the referral
                .setAcuity(new DispositionAcuity().setCategory(disposition.getAcuityCategory()))
                .setSpecialtyCareScheduling(SpecialtyCareScheduling.fromModel(disposition.getSpecialtyCareScheduling()))
                .setFlag(FlagResponse.forDispositionFlag(disposition.getFlag()))
                .setNotes(disposition.getDispositionNotes() == null ? Collections.emptyList() : disposition.getDispositionNotes().stream()
                    .map(dispositionNote -> DispositionNote.from(dispositionNote).get())
                    .sorted((note1, note2) -> {
                        return note2.getCreatedDt().compareTo(note1.getCreatedDt());
                    })
                    .collect(Collectors.toList())
                )
                .setCrisisAcknowledged(disposition.isCrisisAcknowledged())
                .setTriageReview(disposition.getTriageReview().map(TriageReview::fromDTriageReview)
                        .orElse(new TriageReview()))
                .setContact(disposition.getContact().stream().map(ContactDTO::forDispositionContacts).collect(Collectors.toList()))
                .setOrders(disposition.getOrders().stream().map(OrderResponse::forDispositionOrders).collect(Collectors.toList()));
        DispositionOutcome dOutcome = new DispositionOutcome();
        if(disposition.getDiagnosis() != null){
            dOutcome.setDiagnosis(disposition.getDiagnosis())
                    .setCare(disposition.getDiagnosis().getCare());
        }
        dOutcome.setCrisis(disposition.isCrisis());
        response.setOutcome(dOutcome);
        return response;
    }
}

