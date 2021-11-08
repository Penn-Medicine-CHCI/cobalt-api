package com.cobaltplatform.ic.backend.service;

import com.cobaltplatform.ic.backend.config.EpicConfig;
import com.cobaltplatform.ic.backend.service.oauth.IicJwtService;
import com.google.common.net.HttpHeaders;
import io.javalin.plugin.json.JavalinJackson;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.Validate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class EpicAuthService {
    private static final OkHttpClient httpClient = new OkHttpClient();
    private static final Logger logger = LoggerFactory.getLogger(EpicAuthService.class);

    public static String getFhirAuthToken() {
        var expirationTime = DateTime.now().plusSeconds(300).toDate();
        var JTI = UUID.randomUUID().toString();
        var prvKey = EpicConfig.getPrivateKey();
        var aud = EpicConfig.getBaseUrl() + EpicConfig.getEnvironmentPath() + "/oauth2/token";

        var sJWT = IicJwtService.getJwtToken(
            EpicConfig.getBackendClientId(),
            aud,
            EpicConfig.getBackendClientId(),
            JTI,
            expirationTime,
            prvKey
        );

        var postBody = new FormBody.Builder()
                .addEncoded("grant_type", "client_credentials")
                .addEncoded("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                .addEncoded("client_assertion", sJWT)
                .build();

        var postUrl = new Request.Builder().post(postBody).url(aud)
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .build();

        try (var response = httpClient.newCall(postUrl).execute()) {
            try {
                Validate.isTrue(response.isSuccessful());
                Map<String, String> accessToken = JavalinJackson.getObjectMapper().readValue(response.body().byteStream(), HashMap.class);
                return accessToken.get("access_token");
            } catch (Exception e) {
                logger.error("Failed to validate Epic Token: {}", response.code(), e);
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Failed validate Epic Token", e);
        }
        return null;
    }
}
