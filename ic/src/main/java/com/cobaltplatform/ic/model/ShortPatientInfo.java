package com.cobaltplatform.ic.model;

import java.util.UUID;

public class ShortPatientInfo {
    private UUID id;
    private UUID cobaltAccountId;
    private String firstName;
    private String lastName;
    private String preferredPhoneNumber;
    private PatientEngagementType preferredEngagement;

    public UUID getId() {
        return id;
    }

    public ShortPatientInfo setId(final UUID id) {
        this.id = id;
        return this;
    }

    public UUID getCobaltAccountId() {
        return cobaltAccountId;
    }

    public ShortPatientInfo setCobaltAccountId(UUID cobaltAccountId) {
        this.cobaltAccountId = cobaltAccountId;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public ShortPatientInfo setFirstName(final String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public ShortPatientInfo setLastName(final String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getPreferredPhoneNumber() {
        return preferredPhoneNumber;
    }

    public ShortPatientInfo setPreferredPhoneNumber(final String preferredPhoneNumber) {
        this.preferredPhoneNumber = preferredPhoneNumber;
        return this;
    }

    public PatientEngagementType getPreferredEngagement() {
        return preferredEngagement;
    }

    public ShortPatientInfo setPreferredEngagement(
        final PatientEngagementType preferredEngagement) {
        this.preferredEngagement = preferredEngagement;
        return this;
    }
}

