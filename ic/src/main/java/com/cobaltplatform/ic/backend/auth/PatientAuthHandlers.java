package com.cobaltplatform.ic.backend.auth;


import com.cobaltplatform.ic.backend.config.EpicConfig;
import com.cobaltplatform.ic.backend.config.PatientOAuthConfig;
import com.cobaltplatform.ic.backend.config.IcConfig;
import com.cobaltplatform.ic.backend.service.CobaltService;
import com.cobaltplatform.ic.backend.service.PatientService;
import com.cobaltplatform.ic.backend.model.auth.FhirTokenResponse;
import com.cobaltplatform.ic.backend.model.cobalt.CreateMpmPatientRequest;
import com.cobaltplatform.ic.backend.model.db.DPatient;
import com.cobaltplatform.ic.backend.model.db.query.QDPatient;
import com.cobaltplatform.ic.backend.service.oauth.OAuthTokenService;
import com.cobaltplatform.ic.backend.service.oauth.IicJwtService;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import org.apache.commons.lang3.Validate;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Patient;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;

public class PatientAuthHandlers {
    private static final Logger logger = LoggerFactory.getLogger(PatientAuthHandlers.class);
    private static final PatientService patientService = new PatientService();
    private static final IicJwtService icJwtService = new IicJwtService();

    public static Handler handleCallback = ctx -> {
        var authorizationCode = ctx.queryParam("code");
        var state = ctx.queryParam("state");
        var fakeEmailAddress = ctx.queryParam("fakeEmailAddress");
        boolean fakeSso = Objects.equals("true", ctx.queryParam("fakeSso"));

        FhirTokenResponse fhirTokenResponse;
        Patient patient;
        String uid;

        logger.info("Fake SSO: {}, Fake SSO supported: {}, Fake Email Address: {}", fakeSso, EpicConfig.isFakeSsoSupported(), fakeEmailAddress);

        if(fakeSso && EpicConfig.isFakeSsoSupported()) {
            fhirTokenResponse = new FhirTokenResponse();
            fhirTokenResponse.setPatient(fakeEmailAddress);
            fhirTokenResponse.setExpiresIn(Instant.now().plusSeconds(60 * 60 * 24).toEpochMilli() / 1_000L);

            patient = patientService.fakePatient(fakeEmailAddress);
            uid = patientService.getUidFromPatientObject(patient).get();
        } else {
            try {
                Validate.notNull(authorizationCode);
                Validate.notBlank(authorizationCode);
            } catch (Exception e) {
                logger.warn("Authorization code not provided in call", e);
                ctx.redirect("/", HttpStatus.MOVED_TEMPORARILY_302);
                return;
            }

            //TODO: Ideal state would be to cache this authcode
            fhirTokenResponse = OAuthTokenService.getTokenFromAuthCode(
                authorizationCode,
                PatientOAuthConfig.getRedirectUrl(),
                PatientOAuthConfig.getClientId(),
                PatientOAuthConfig.getGrantType()
            );

            try {
                Validate.notNull(fhirTokenResponse);
            } catch (Exception e) {
                logger.error("Could not exchange retrieve access token for code", e);
                ctx.status(HttpStatus.FAILED_DEPENDENCY_424);
                throw e;
            }

            patient = patientService.getPatientByToken(fhirTokenResponse);

            uid = patientService.getUidFromPatientObject(patient).orElseThrow(() -> {
                logger.error("Could not find UID in patient object, {}", patient);
                return new NotFoundResponse();
            });
        }

        DPatient dPatient = new QDPatient()
                .or()
                    .fhirId.equalTo(fhirTokenResponse.getPatient())
                    .uid.equalTo(uid)
                .endOr()
                .findOneOrEmpty()
                .orElseGet(() -> new DPatient().setLoggedInDt(new ArrayList<>()));

        List<String> loggedInDt = dPatient.getLoggedInDt();
        loggedInDt.add(DateTime.now(DateTimeZone.UTC).withZone(DateTimeZone.UTC).toString());
        dPatient.setLoggedInDt(loggedInDt)
            .setFhirId(fhirTokenResponse.getPatient())
            .setUid(uid);

        // Ensure patient is linked to corresponding Cobalt patient account record
        var cobaltPatientResponse = CobaltService.getSharedInstance().createPatient(new CreateMpmPatientRequest(patient));
        dPatient.setCobaltAccountId(cobaltPatientResponse.getAccount().getAccountId());

        HumanName name = patient.getNameFirstRep();
        dPatient.setPreferredFirstName(name.getGivenAsSingleString());
        dPatient.setPreferredLastName(name.getFamily());
        //      only override values in ic if epic email data has not been updated
        if (!dPatient.getPreferredEmailHasBeenUpdated()) {
            Optional<ContactPoint> email = patient.getTelecom().stream().filter(t -> t.getSystem() == ContactPoint.ContactPointSystem.EMAIL).findFirst();
            if(email.isPresent()){
                dPatient.setPreferredEmail(email.get().getValue());
            }
        }

        //        only override values in ic if epic phone number data has not been updated
        if (!dPatient.getPreferredPhoneHasBeenUpdated()) {
            Optional<ContactPoint> mobilePhone = patient.getTelecom().stream().filter(t -> t.getSystem() == ContactPoint.ContactPointSystem.PHONE  && t.getUse() == ContactPoint.ContactPointUse.MOBILE).findFirst();
            Optional<ContactPoint> phone = patient.getTelecom().stream().filter(t -> t.getSystem() == ContactPoint.ContactPointSystem.PHONE).findFirst();

            if(mobilePhone.isPresent()){
                dPatient.setPreferredPhoneNumber(mobilePhone.get().getValue());
            }

            if(mobilePhone.isEmpty()) {
                dPatient.setPreferredPhoneNumber(phone.get().getValue());
            }
        }

        dPatient.setPreferredGender(patient.getGender().getDisplay());
        dPatient.save();

        var patientIcJwt = icJwtService.getPatientIcTokenFromFhirToken(fhirTokenResponse);

        ctx.cookie(icJwtService.getCookieForAccessToken(cobaltPatientResponse.getAccessToken(), state));
        ctx.cookie(icJwtService.getCookieForJwt(patientIcJwt, state));

        if (Objects.equals(IcConfig.getEnvironment(), "local")) {
            ctx.redirect(authRedirectUrl(cobaltPatientResponse.getAccessToken(), patientIcJwt));
        } else {
            String redirectUrl = authRedirectUrl(cobaltPatientResponse.getAccessToken(), patientIcJwt);

            String html = "<html>\n" +
                "<head><meta http-equiv='refresh' content=1;url='$IC_REDIRECT_URL'></head>\n" +
                "<body></body>\n" +
                "</html>";

            html = html.replace("$IC_REDIRECT_URL", redirectUrl);

            ctx.contentType("text/html");
            ctx.result(html);
        }
    };

    private static String authRedirectUrl(final String accessToken,
                                          final String patientContext) {
        String baseUrl;

        if (Objects.equals(IcConfig.getEnvironment(), "local")) {
            return format("http://127.0.0.1:3000/pic/%s", patientContext == null ? "mhic" : "home");
        } else {
            baseUrl = "https://example.com/pic/finish-sso";
        }

        return format("%s?accessToken=%s&patientContext=%s",
            baseUrl,
            URLEncoder.encode(accessToken, StandardCharsets.UTF_8),
            patientContext == null ? "" : URLEncoder.encode(patientContext, StandardCharsets.UTF_8));
    }

    public static Handler handleLogout = ctx -> {
        ctx.removeCookie(IicJwtService.IC_JWT_COOKIE_NAME);

        ctx.status(HttpStatus.OK_200);
    };

    public static Handler handleAuthRedirect = ctx -> {
        var accessToken = ctx.queryParam("accessToken");

        ctx.cookie(icJwtService.getCookieForAccessToken(accessToken, null));

        String redirectUrl = authRedirectUrl(accessToken, null);

        String html = "<html>\n" +
            "<head><meta http-equiv='refresh' content=1;url='$IC_REDIRECT_URL'></head>\n" +
            "<body></body>\n" +
            "</html>";

        html = html.replace("$IC_REDIRECT_URL", redirectUrl);

        ctx.contentType("text/html");
        ctx.result(html);
    };
}
