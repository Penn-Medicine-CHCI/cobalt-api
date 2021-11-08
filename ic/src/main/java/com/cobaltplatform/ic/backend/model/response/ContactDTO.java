package com.cobaltplatform.ic.backend.model.response;

import com.cobaltplatform.ic.model.ContactCallResult;
import com.cobaltplatform.ic.model.ContactReferringLocation;
import com.cobaltplatform.ic.backend.model.db.DContact;
import org.joda.time.DateTime;

import java.util.UUID;


public class ContactDTO {
    private UUID id;

    private ContactReferringLocation referringLocation;

    private String authoredBy;

    private ContactCallResult callResult;

    private String note;



    private DateTime createdAt;

    public ContactDTO() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ContactReferringLocation getReferringLocation() {
        return referringLocation;
    }

    public void setReferringLocation(ContactReferringLocation referringLocation) {
        this.referringLocation = referringLocation;
    }

    public ContactCallResult getCallResult() {
        return callResult;
    }

    public void setCallResult(ContactCallResult callResult) {
        this.callResult = callResult;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getAuthoredBy() {
        return authoredBy;
    }

    public void setAuthoredBy(String authoredBy) {
        this.authoredBy = authoredBy;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static  ContactDTO forDispositionContacts(DContact contact) {
        var contactDTO = new ContactDTO() ;
        contactDTO
                .setAuthoredBy(contact.getAuthoredBy());
        contactDTO
                .setNote(contact.getNote());
        contactDTO
                .setReferringLocation(contact.getReferringLocation());
        contactDTO
                .setCallResult(contact.getCallResult());
        contactDTO
                .setId(contact.getId());
        contactDTO
                .setCreatedAt(contact.getCreatedDt());

        return contactDTO;
    }

}
