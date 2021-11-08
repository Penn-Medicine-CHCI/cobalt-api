package com.cobaltplatform.ic.backend.model.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FhirTokenResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String scope;
    private String state;
    private String epicDstu2Patient;
    private String patient;

    @JsonProperty("access_token")
    public String getAccessToken() {
        return accessToken;
    }

    @JsonProperty("access_token")
    public void setAccessToken(String value) {
        this.accessToken = value;
    }

    @JsonProperty("token_type")
    public String getTokenType() {
        return tokenType;
    }

    @JsonProperty("token_type")
    public void setTokenType(String value) {
        this.tokenType = value;
    }

    @JsonProperty("expires_in")
    public long getExpiresIn() {
        return expiresIn;
    }

    @JsonProperty("expires_in")
    public void setExpiresIn(long value) {
        this.expiresIn = value;
    }

    @JsonProperty("scope")
    public String getScope() {
        return scope;
    }

    @JsonProperty("scope")
    public void setScope(String value) {
        this.scope = value;
    }

    @JsonProperty("state")
    public String getState() {
        return state;
    }

    @JsonProperty("state")
    public void setState(String value) {
        this.state = value;
    }

    @JsonProperty("__epic.dstu2.patient")
    public String getEpicDstu2Patient() {
        return epicDstu2Patient;
    }

    @JsonProperty("__epic.dstu2.patient")
    public void setEpicDstu2Patient(String value) {
        this.epicDstu2Patient = value;
    }

    @JsonProperty("patient")
    public String getPatient() {
        return patient;
    }

    @JsonProperty("patient")
    public void setPatient(String value) {
        this.patient = value;
    }
}
