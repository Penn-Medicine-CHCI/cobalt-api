package com.cobaltplatform.ic.backend.service.oauth;

import static org.eclipse.jetty.http.HttpCookie.SAME_SITE_LAX_COMMENT;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.http.Cookie;

import com.cobaltplatform.ic.backend.config.IcConfig;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import com.cobaltplatform.ic.backend.model.auth.FhirTokenResponse;
import com.cobaltplatform.ic.backend.model.response.PublicKeyResponse;
import io.javalin.plugin.json.JavalinJackson;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.jackson.io.JacksonDeserializer;
import io.jsonwebtoken.lang.Maps;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IicJwtService {
    private static final OkHttpClient httpClient = new OkHttpClient();
    public static final String IC_JWT_COOKIE_NAME = "iccobalt_patientcontext";
    private static final Logger logger = LoggerFactory.getLogger(IicJwtService.class);

    public String getPatientIcTokenFromFhirToken(FhirTokenResponse fhirToken) {
        return Jwts.builder()
            .setSubject(fhirToken.getPatient())
            .setIssuer(IcConfig.getBaseUrl())
            .setIssuedAt(DateTime.now().toDate())
            .setExpiration(DateTime.now().plusSeconds((int) (fhirToken.getExpiresIn() - 60)).toDate())
            .setNotBefore(DateTime.now().toDate())
            .setId(UUID.randomUUID().toString())
            .claim("epic_token", fhirToken)
            .claim("roleId", "PATIENT")
            .signWith(IcConfig.getKeyPair().getPrivate())
            .compact();
    }

    public static String getJwtToken(String iss, String aud, String sub, String jti, Date exp, PrivateKey prvKey) {
        return Jwts.builder()
            .setSubject(sub)
            .setIssuer(iss)
            .setIssuedAt(DateTime.now().toDate())
            .setExpiration(exp)
            .setId(jti)
            .setAudience(aud)
            .signWith(prvKey)
            .compact();
    }

    public Cookie getCookieForJwt(String jwtString, String originState) {
        var newCookie = new Cookie(IC_JWT_COOKIE_NAME, jwtString);
        if (StringUtils.equals(originState, "local")) {
            logger.debug("Issuing a cookie={} to a local client", newCookie.toString());
            newCookie.setDomain("127.0.0.1");
        } else {
            newCookie.setSecure(true);
            newCookie.setComment(SAME_SITE_LAX_COMMENT);
        }
        return newCookie;
    }

    public Cookie getCookieForAccessToken(String accessToken, String originState) {
        var newCookie = new Cookie("accessToken", accessToken);
        if (StringUtils.equals(originState, "local")) {
            logger.debug("Issuing a cookie={} to a local client", newCookie.toString());
            newCookie.setDomain("127.0.0.1");
        } else {
            newCookie.setSecure(true);
            newCookie.setComment(SAME_SITE_LAX_COMMENT);
        }
        return newCookie;
    }

    public static boolean getValidPublicKeyFromCobalt(String accessToken) {
        var getRequest = new Request.Builder().get().url(IcConfig.getCobaltBackendBaseUrl() + "/system/public-key")
                .addHeader(HttpHeaders.ACCEPT, MediaType.JSON_UTF_8.toString())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
                .build();

        try (var publicKeyResponse = httpClient.newCall(getRequest).execute()) {
            try {
                Validate.isTrue(publicKeyResponse.isSuccessful());
            } catch (Exception e) {
                logger.error("Failed to retrieve patientId={} with epicResponseCode={}",
                        publicKeyResponse.code(), e);
                return false;
            }
            var responseByteStream = Objects.requireNonNull(publicKeyResponse.body()).string();
            PublicKeyResponse cobaltKey = JavalinJackson.getObjectMapper().readValue(responseByteStream, PublicKeyResponse.class);
            PublicKey key = KeyFactory.getInstance(cobaltKey.getAlgorithm()).generatePublic(new X509EncodedKeySpec(cobaltKey.getPublicKey()));
            var isValid = IicJwtService.isValidTokenFromString(accessToken, key);

            logger.debug(responseByteStream);
            return isValid;

        } catch (Exception e) {
            logger.error("Failed to reach Epic to retrieve patientId", e);
            return false;
        }
    }

    public static boolean isValidTokenFromString(String cobaltToken, PublicKey publicKey) {
        logger.trace("Validating token={}", cobaltToken);
        var parser = Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build();

        try {
            parser.parseClaimsJws(String.valueOf(cobaltToken));
            return true;
        } catch (Exception  e) {
            logger.warn("Cobalt token validation failed", e);
            return false;
        }
    }

    public static FhirTokenResponse getFhirTokenFromString(String icToken) {
        logger.trace("Validating token={}", icToken);
        var parser = Jwts.parserBuilder()
            .setSigningKey(IcConfig.getKeyPair().getPublic())
            .requireIssuer(IcConfig.getBaseUrl())
            .deserializeJsonWith(new JacksonDeserializer(Maps.of("epic_token", FhirTokenResponse.class).build()))
            .build();

        var parsedToken = parser.parseClaimsJws(icToken);

        return parsedToken.getBody().get("epic_token", FhirTokenResponse.class);
    }
}
