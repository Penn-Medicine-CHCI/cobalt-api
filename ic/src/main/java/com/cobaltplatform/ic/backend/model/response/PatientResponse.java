package com.cobaltplatform.ic.backend.model.response;

import com.cobaltplatform.ic.backend.model.serialize.DSTU3PatientSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hl7.fhir.dstu3.model.Patient;
import org.joda.time.DateTime;

import java.util.List;
import java.util.UUID;

public class PatientResponse {

    private UUID id;
    private String preferredFirstName;
    private String preferredLastName;
    private String preferredEmail;
    private String preferredPhoneNumber;
    private String preferredGender;
    private String uid;
    private Patient epicProfile;
    private String specialist;
    private DateTime referredToIc;
    private UUID cobaltAccountId;
    private List<DateTime> loggedIn;
    private List<String> patientGoals;

    public UUID getID() {
        return id;
    }

    public void setID(UUID value) {
        this.id = value;
    }

    @JsonSerialize(using = DSTU3PatientSerializer.class)
    public Patient getEpicProfile() {
        return epicProfile;
    }

    public void setEpicProfile(Patient value) {
        this.epicProfile = value;
    }

    public String getSpecialist() {
        return specialist;
    }

    public void setSpecialist(String value) {
        this.specialist = value;
    }

    public DateTime getReferredToIc() {
        return referredToIc;
    }

    public void setReferredToIc(DateTime value) {
        this.referredToIc = value;
    }

    public UUID getCobaltAccountId() {
        return cobaltAccountId;
    }

    public void setCobaltAccountId(UUID cobaltAccountId) {
        this.cobaltAccountId = cobaltAccountId;
    }

    public List<DateTime> getLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(List<DateTime> value) {
        this.loggedIn = value;
    }

    public List<String> getPatientGoals() {
        return patientGoals;
    }

    public void setPatientGoals(List<String> value) {
        this.patientGoals = value;
    }

    public String getPreferredFirstName() {
        return preferredFirstName;
    }

    public void setPreferredFirstName(String preferredFirstName) {
        this.preferredFirstName = preferredFirstName;
    }

    public String getPreferredLastName() {
        return preferredLastName;
    }

    public void setPreferredLastName(String preferredLastName) {
        this.preferredLastName = preferredLastName;
    }

    public String getPreferredEmail() {
        return preferredEmail;
    }

    public void setPreferredEmail(String preferredEmail) {
        this.preferredEmail = preferredEmail;
    }

    public String getPreferredPhoneNumber() {
        return preferredPhoneNumber;
    }

    public void setPreferredPhoneNumber(String preferredPhoneNumber) {
        this.preferredPhoneNumber = preferredPhoneNumber;
    }

    public String getPreferredGender() {
        return preferredGender;
    }

    public void setPreferredGender(String preferredGender) {
        this.preferredGender = preferredGender;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
