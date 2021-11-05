package com.cobaltplatform.ic.backend.service.oauth;

import com.cobaltplatform.ic.backend.config.EpicConfig;
import com.google.common.net.MediaType;
import com.cobaltplatform.ic.backend.model.auth.FhirTokenResponse;
import io.javalin.plugin.json.JavalinJackson;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.Validate;
import org.eclipse.jetty.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuthTokenService {
    private static final OkHttpClient OK_HTTP_CLIENT;
    private static final Logger LOGGER;

    static {
        OK_HTTP_CLIENT = new OkHttpClient();
        LOGGER = LoggerFactory.getLogger(OAuthTokenService.class);
    }

    public static FhirTokenResponse getTokenFromAuthCode(
        String authorizationCode,
        String redirectUri,
        String clientId,
        String grantType
    ) {
        var requestBody = new FormBody.Builder()
            .addEncoded("grant_type", "authorization_code")
            .addEncoded("code", authorizationCode)
            .addEncoded("redirect_uri", redirectUri)
            .addEncoded("client_id", clientId)
            .build();

        String url = EpicConfig.getBaseUrl() + EpicConfig.getEnvironmentPath() + EpicConfig.getOauthTokenPath();

        var request = new Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader(HttpHeader.CONTENT_TYPE.asString(), MediaType.FORM_DATA.toString())
            .build();

        LOGGER.info("Exchanging FHIR token for auth code:\nPOST {}\ncode={}, client_id={}, redirect_uri={}", url, authorizationCode, clientId, redirectUri);

        try (var tokenResponse = OK_HTTP_CLIENT.newCall(request).execute()) {
            String responseBody = tokenResponse.body().string();

            LOGGER.info("FHIR token response (HTTP {}):\n{}", tokenResponse.code(), responseBody);

            try {
                Validate.isTrue(tokenResponse.isSuccessful());
            } catch (Exception e) {
                LOGGER.error("Authorization code could not be exchanged for token", e);
                return null;
            }

            var fhirTokenResponse =
                JavalinJackson.getObjectMapper().readValue(responseBody, FhirTokenResponse.class);
            return fhirTokenResponse;
        } catch (Exception e) {
            LOGGER.error("Could not exchange retrieve access token for code", e);
        }
        
        return null;
    }
}
