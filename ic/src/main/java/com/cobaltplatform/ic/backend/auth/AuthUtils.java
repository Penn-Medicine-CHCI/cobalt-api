package com.cobaltplatform.ic.backend.auth;

import com.cobaltplatform.ic.backend.model.auth.FhirTokenResponse;
import com.cobaltplatform.ic.backend.model.db.DPatient;
import com.cobaltplatform.ic.backend.model.db.query.QDPatient;
import com.cobaltplatform.ic.backend.service.oauth.IicJwtService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.Optional;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trimToNull;

public class AuthUtils {
    private static final Logger logger = LoggerFactory.getLogger(AuthUtils.class);

    public static Optional<DPatient> getIcPatient(Context ctx) {
        String incomingJwt = getAuthorizationTokenValue(ctx).orElse(null);

        if(incomingJwt == null)
            return Optional.empty();

        FhirTokenResponse fhirTokenResponse;

        try {
            fhirTokenResponse = IicJwtService.getFhirTokenFromString(incomingJwt);
        } catch (Exception e) {
            logger.warn(format("Unable to extract FHIR token from JWT", incomingJwt), e);
            return Optional.empty();
        }

        return new QDPatient().where().fhirId.equalTo(fhirTokenResponse.getPatient()).findOneOrEmpty();
    }

    public static Optional<FhirTokenResponse> getPatientFhirToken(Context ctx) {
        String incomingJwt = getAuthorizationTokenValue(ctx).orElse(null);

        if(incomingJwt == null)
            return Optional.empty();

        try {
            return Optional.of(IicJwtService.getFhirTokenFromString(incomingJwt));
        } catch(Exception e) {
            logger.warn(format("Unable to get patient FHIR token for JWT %s", incomingJwt), e);
            return Optional.empty();
        }
    }

    public static Optional<String> getAuthorizationTokenValue(@NotNull Context ctx) {
        return Optional.ofNullable(trimToNull(ctx.cookie(IicJwtService.IC_JWT_COOKIE_NAME)));
    }
}
