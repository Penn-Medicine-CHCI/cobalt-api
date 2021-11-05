package com.cobaltplatform.ic.backend.config;

public class PatientOAuthConfig extends IcConfig {

    public static String getClientId() {
        return icConfig.getString("epic.patient.clientID");
    }

    public static String getGrantType() {
        return icConfig.getString("epic.patient.grantType");
    }

    public static String getRedirectUrl() {
        return icConfig.getString("epic.patient.redirectUrl");
    }
}
