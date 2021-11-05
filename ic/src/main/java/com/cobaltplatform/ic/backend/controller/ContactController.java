package com.cobaltplatform.ic.backend.controller;

import com.cobaltplatform.ic.backend.model.response.ContactDTO;
import com.cobaltplatform.ic.backend.service.ContactService;
import io.javalin.http.NotFoundResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.javalin.http.Handler;
import org.apache.http.HttpStatus;
import java.util.NoSuchElementException;
import java.util.UUID;

public class ContactController {
    private static final Logger logger = LoggerFactory.getLogger(ContactController.class);
    public static Handler getAllPatientContactsByDispositionId = ctx -> {
        var request = ctx.pathParam("id", UUID.class);
        if (request.isValid()) {
            var contact = ContactService.getContactsForDisposition(request.getValue());
            ctx.json(contact);
        }
        else {
            ctx.status(HttpStatus.SC_NOT_FOUND);
        }
    };

    public static Handler postContact = ctx -> {
        ContactDTO contactDTO = ctx.bodyAsClass(ContactDTO.class);
        var dispositionId = contactDTO.getId();
        var note = contactDTO.getNote();
        var authoredBy = contactDTO.getAuthoredBy();
        var callResult = contactDTO.getCallResult();
        var referringLocation = contactDTO.getReferringLocation();

        try {
            ContactService.postContactForDisposition(dispositionId, note, authoredBy, callResult, referringLocation);
            ctx.status(HttpStatus.SC_CREATED);
            ctx.json(dispositionId);
        } catch (NoSuchElementException | NotFoundResponse e) {
            logger.error("Could not submit contact {}", dispositionId, e);
            throw new NotFoundResponse();
        }
    };
}
