package com.cobaltplatform.ic.model;

import java.time.Instant;
import java.util.UUID;

public class CobaltAccount {
    private UUID accountId;
    private UUID providerId;
    private String roleId;
    private String institutionId;
    private String accountSourceId;
    private String sourceSystemId;
    private String firstName;
    private String lastName;
    private String displayName;
    private String emailAddress;
    private boolean completedIntroAssessment;
    private String timeZone;
    private String locale;
    private boolean consentFormAccepted;
    private boolean epicPatientCreatedByCobalt;
    private Instant created;
    private String createdDescription;
    private Instant lastUpdated;
    private String lastUpdatedDescription;

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public void setProviderId(UUID providerId) {
        this.providerId = providerId;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(String institutionId) {
        this.institutionId = institutionId;
    }

    public String getAccountSourceId() {
        return accountSourceId;
    }

    public void setAccountSourceId(String accountSourceId) {
        this.accountSourceId = accountSourceId;
    }

    public String getSourceSystemId() {
        return sourceSystemId;
    }

    public void setSourceSystemId(String sourceSystemId) {
        this.sourceSystemId = sourceSystemId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public boolean isCompletedIntroAssessment() {
        return completedIntroAssessment;
    }

    public void setCompletedIntroAssessment(boolean completedIntroAssessment) {
        this.completedIntroAssessment = completedIntroAssessment;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public boolean isConsentFormAccepted() {
        return consentFormAccepted;
    }

    public void setConsentFormAccepted(boolean consentFormAccepted) {
        this.consentFormAccepted = consentFormAccepted;
    }

    public boolean isEpicPatientCreatedByCobalt() {
        return epicPatientCreatedByCobalt;
    }

    public void setEpicPatientCreatedByCobalt(boolean epicPatientCreatedByCobalt) {
        this.epicPatientCreatedByCobalt = epicPatientCreatedByCobalt;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public String getCreatedDescription() {
        return createdDescription;
    }

    public void setCreatedDescription(String createdDescription) {
        this.createdDescription = createdDescription;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getLastUpdatedDescription() {
        return lastUpdatedDescription;
    }

    public void setLastUpdatedDescription(String lastUpdatedDescription) {
        this.lastUpdatedDescription = lastUpdatedDescription;
    }
}
