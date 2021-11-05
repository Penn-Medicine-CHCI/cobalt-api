package com.cobaltplatform.ic.backend.service;

import com.cobaltplatform.ic.backend.model.db.DContact;
import com.cobaltplatform.ic.backend.model.db.DPatientDisposition;
import com.cobaltplatform.ic.backend.model.db.query.QDContact;
import com.cobaltplatform.ic.backend.model.db.query.QDPatientDisposition;
import com.cobaltplatform.ic.model.ContactCallResult;
import com.cobaltplatform.ic.model.ContactReferringLocation;
import io.javalin.http.NotFoundResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ContactService {
    public static List<DContact> getContactsForDisposition (UUID request) {
        return new QDContact()
                .where()
                .disposition.id.equalTo(request)
                .findList();
    }

   public static DContact postContactForDisposition (UUID dispositionId, String note, String authoredBy, ContactCallResult callResult, ContactReferringLocation referringLocation) throws NotFoundResponse {
        DContact contact = new DContact();

        Optional<DPatientDisposition> disposition = new QDPatientDisposition().id.equalTo(dispositionId).findOneOrEmpty();


     if (disposition.isPresent()) {
       contact
           .setAuthoredBy(authoredBy)
           .setDisposition(disposition.get())
           .setNote(note)
           .setReferringLocation(referringLocation)
           .setCallResult(callResult)
           .save();
       return contact;
     }
     else {
       throw new NotFoundResponse(String.format("Disposition not found with id %s", dispositionId));
     }

   }
}
