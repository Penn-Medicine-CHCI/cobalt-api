package com.cobaltplatform.ic.backend.config;

import com.cobaltplatform.ic.backend.util.KeyManager;

import java.security.PrivateKey;
import java.util.Objects;

public class EpicConfig extends IcConfig {
    private static PrivateKey privateKey = loadPrivateKey();

    public static String getBaseUrl() {
        return icConfig.getString("epic.baseUrl");
    }

    public static String getEnvironmentPath() {
        return icConfig.getString("epic.environmentPath");
    }

    public static String getOauthTokenPath() {
        return icConfig.getString("epic.oauthTokenPath");
    }

    public static String getStu3ApiPath() {
        return icConfig.getString("epic.stu3ApiPath");
    }

    public static String getBackendClientId() { return icConfig.getString("epic.backendClientID"); }

    public static PrivateKey getPrivateKey() {
        return privateKey;
    }

    public static boolean isFakeSsoSupported() {
        return Objects.equals(getBaseUrl(), "https://ssproxytest.example.com");
    }

    private static PrivateKey loadPrivateKey() {
        String privateKeyLocation = icConfig.getString("epic.privateKey");
        String privateKeyAsString = getSecretsManager().fetchSecretString(privateKeyLocation).get();

        return KeyManager.privateKeyFromPem(privateKeyAsString);
    }
}
