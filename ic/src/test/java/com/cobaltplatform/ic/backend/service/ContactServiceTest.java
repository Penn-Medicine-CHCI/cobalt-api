package com.cobaltplatform.ic.backend.service;

import com.cobaltplatform.ic.backend.model.db.DBaseModel;
import com.cobaltplatform.ic.backend.model.db.DContact;
import com.cobaltplatform.ic.backend.model.db.DPatient;
import com.cobaltplatform.ic.backend.model.db.DPatientDisposition;
import com.cobaltplatform.ic.model.ContactCallResult;
import com.cobaltplatform.ic.model.ContactReferringLocation;
import com.cobaltplatform.ic.model.DispositionFlag;
import io.ebean.test.ForTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ContactServiceTest {
    private ForTests.RollbackAll rollbackAll;

    @BeforeEach
    public void before() {
        rollbackAll = ForTests.createRollbackAll();
    }

    @AfterEach
    public void after() {
        rollbackAll.close();
    }


    @Test
    void getContactsForDisposition_Matches_Responses() {
        var patient = new DPatient();
        var disposition = new DPatientDisposition();
        var contact1 = new DContact();
        var contact2 = new DContact();

        disposition
                .setPatient(patient)
                .setFlag(DispositionFlag.GRADUATED)
                .setId(UUID.randomUUID())
                .save();

        contact1
                .setCallResult(ContactCallResult.BUSY)
                .setReferringLocation(ContactReferringLocation.REFERRING_LOCATION_3)
                .setNote("random note left about call")
                .setAuthoredBy("Billy Bob")
                .setDisposition(disposition)
                .save();

        contact2
                .setCallResult(ContactCallResult.LEFT_VOICE_MAIL)
                .setReferringLocation(ContactReferringLocation.REFERRING_LOCATION_0)
                .setNote("random note left about call")
                .setAuthoredBy("Billy Bob")
                .setDisposition(disposition)
                .save();

        var contacts = ContactService.getContactsForDisposition(disposition.getId());
        assertEquals(Set.of(contact1.getId(), contact2.getId()), contacts.stream().map(DBaseModel::getId).collect(Collectors.toSet()));
    }

    @Test
    void getContactsForDisposition_Does_Not_Match_Disposition() {
        var patient = new DPatient();
        var disposition = new DPatientDisposition();
        var contact1 = new DContact();
        var contact2 = new DContact();

        patient.save();

        disposition
                .setPatient(patient)
                .setFlag(DispositionFlag.GRADUATED)
                .setId(UUID.randomUUID())
                .save();

        contact1
                .setCallResult(ContactCallResult.BUSY)
                .setReferringLocation(ContactReferringLocation.REFERRING_LOCATION_3)
                .setNote("random note left about call")
                .setAuthoredBy("Billy Bob")
                .setDisposition(disposition)
                .save();

        contact2
                .setCallResult(ContactCallResult.LEFT_VOICE_MAIL)
                .setReferringLocation(ContactReferringLocation.REFERRING_LOCATION_0)
                .setNote("random note left about call")
                .setAuthoredBy("Billy Bob")
                .save();


        var contacts = ContactService.getContactsForDisposition(disposition.getId());
        assertEquals(Set.of(contact1.getId()), contacts.stream().map(DBaseModel::getId).collect(Collectors.toSet()));
    }

}
