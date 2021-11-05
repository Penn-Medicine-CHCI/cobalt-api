package com.cobaltplatform.ic.backend.model.db;

import com.cobaltplatform.ic.model.ContactCallResult;
import com.cobaltplatform.ic.model.ContactReferringLocation;

import javax.persistence.*;

@Entity
@Table(name = "contact", schema = "ic")
public class DContact extends  DBaseModel {

    public DPatientDisposition getDisposition() { return disposition; }

    public DContact setDisposition(DPatientDisposition disposition) {
        this.disposition = disposition;
        return this;
    }

    @ManyToOne
    private DPatientDisposition disposition;

    public String getAuthoredBy() { return authoredBy; }

    public DContact setAuthoredBy(final String authoredBy) {
        this.authoredBy = authoredBy;
        return this;
    }

    private String authoredBy;

    public ContactReferringLocation getReferringLocation() { return referringLocation; }

    public DContact setReferringLocation(final ContactReferringLocation referringLocation) {
        this.referringLocation = referringLocation;
        return this;
    }

    private ContactReferringLocation referringLocation;

    public ContactCallResult getCallResult() { return callResult; }

    public DContact setCallResult(final ContactCallResult callResult) {
        this.callResult = callResult;
        return this;
    }

    private ContactCallResult callResult;

    public String getNote() {
        return note;
    }

    public DContact setNote(final String note) {
        this.note = note;
        return this;
    }

    @Column(length = 10_000)
    private String note;

}

