package com.cobaltplatform.ic.backend.model.db;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.*;

import io.ebean.annotation.DbArray;
import io.ebean.annotation.DbDefault;

@Entity
@Table(name = "patient", schema = "ic")
public class DPatient extends DBaseModel {

    @DbArray
    private List<String> loggedInDt = new ArrayList<String>();

    @Column(unique=true)
    private String fhirId;

    private String fhirProvider;
    @DbArray
    private List<String> goals = new ArrayList<>();
    private String preferredFirstName;
    private String preferredLastName;
    private String preferredEmail;
    private String preferredPhoneNumber;
    private String preferredGender;

    @DbDefault("false")
    private boolean preferredEmailHasBeenUpdated;
    @DbDefault("false")
    private boolean preferredPhoneHasBeenUpdated;

    @Column(unique=true)
    private String uid;

    @Column(unique=true)
    private UUID cobaltAccountId;

    public boolean getPreferredEmailHasBeenUpdated() {
        return preferredEmailHasBeenUpdated;
    }

    public DPatient setPreferredEmailHasBeenUpdated(boolean preferredEmailHasBeenUpdated) {
        this.preferredEmailHasBeenUpdated = preferredEmailHasBeenUpdated;
        return this;
    }

    public boolean getPreferredPhoneHasBeenUpdated() {
        return preferredPhoneHasBeenUpdated;
    }

    public DPatient setPreferredPhoneHasBeenUpdated(boolean preferredPhoneHasBeenUpdated) {
        this.preferredPhoneHasBeenUpdated = preferredPhoneHasBeenUpdated;
        return this;
    }

    public List<String> getLoggedInDt() {
        return loggedInDt;
    }

    public DPatient setLoggedInDt(final List<String> loggedInDt) {
        this.loggedInDt = loggedInDt;
        return this;
    }

    public String getFhirId() {
        return fhirId;
    }

    public DPatient setFhirId(final String fhirId) {
        this.fhirId = fhirId;
        return this;
    }

    public String getFhirProvider() {
        return fhirProvider;
    }

    public DPatient setFhirProvider(final String fhirProvider) {
        this.fhirProvider = fhirProvider;
        return this;
    }

    public List<String> getGoals() {
        return goals;
    }

    public DPatient setGoals(final List<String> goals) {
        this.goals = goals;
        return this;
    }

    public String getPreferredFirstName() {
        return preferredFirstName;
    }

    public DPatient setPreferredFirstName(final String preferredFirstName) {
        this.preferredFirstName = preferredFirstName;
        return this;
    }

    public String getPreferredLastName() {
        return preferredLastName;
    }

    public DPatient setPreferredLastName(final String preferredLastName) {
        this.preferredLastName = preferredLastName;
        return this;
    }

    public String getPreferredEmail() {
        return preferredEmail;
    }

    public DPatient setPreferredEmail(final String preferredEmail) {
        this.preferredEmail = preferredEmail;
        return this;
    }

    public String getPreferredPhoneNumber() {
        return preferredPhoneNumber;
    }

    public DPatient setPreferredPhoneNumber(final String preferredPhoneNumber) {
        this.preferredPhoneNumber = preferredPhoneNumber;
        return this;
    }

    public String getPreferredGender() {
        return preferredGender;
    }

    public DPatient setPreferredGender(final String preferredGender) {
        this.preferredGender = preferredGender;
        return this;
    }

    public String getUid() {
        return uid;
    }

    public DPatient setUid(final String uid) {
        this.uid = uid;
        return this;
    }

    public UUID getCobaltAccountId() {
        return cobaltAccountId;
    }

    public DPatient setCobaltAccountId(final UUID cobaltAccountId) {
        this.cobaltAccountId = cobaltAccountId;
        return this;
    }
}
