package com.cobaltplatform.ic.backend;

import com.cobaltplatform.ic.backend.auth.AuthUtils;
import com.cobaltplatform.ic.backend.config.IcConfig;
import com.cobaltplatform.ic.backend.model.serialize.DSTU3PatientSerializer;
import com.cobaltplatform.ic.backend.model.serialize.FhirR4Deserializer;
import com.cobaltplatform.ic.backend.model.serialize.FhirR4Serializer;
import com.cobaltplatform.ic.backend.service.CobaltAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.cobaltplatform.ic.backend.exception.ValidationException;
import com.cobaltplatform.ic.backend.exception.ValidationException.FieldError;
import com.cobaltplatform.ic.backend.model.auth.IcContext;
import com.cobaltplatform.ic.backend.model.cobalt.CobaltClaims;
import com.cobaltplatform.ic.backend.model.db.DCobaltAccount;
import com.cobaltplatform.ic.backend.model.db.DPatient;
import com.cobaltplatform.ic.model.IcRole;
import io.javalin.core.security.AccessManager;
import io.javalin.core.validation.JavalinValidation;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.Handler;
import io.javalin.http.RequestLogger;
import io.javalin.http.UnauthorizedResponse;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJackson;
import io.javalin.plugin.metrics.MicrometerPlugin;
import org.hl7.fhir.dstu3.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.trimToNull;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    public static void main(String[] args) {
        // Override system defaults for locale and timezone
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale.setDefault(Locale.US);

        logger.info("Starting IC...");

        Javalin app;
        if (IcConfig.getEnvironment().equals("local")) {
            app = Javalin.create(config -> {
                        config.accessManager(accessManager);
                        if (IcConfig.isDevLogging()) {
                            config.enableDevLogging();
                        }
                        config.enableCorsForOrigin("http://localhost:3000/", "http://127.0.0.1:3000/");
                        config.registerPlugin(new MicrometerPlugin());
                    }
            );
        } else {
            app = Javalin.create(config -> {
                if (IcConfig.isDevLogging()) {
                    config.enableDevLogging();
                }

                config.enableCorsForOrigin("https://www.example.com");

                config.accessManager(accessManager);
            });
        }

        for (Routes route : Routes.values()) {
            app.addHandler(route.getHandlerType(), route.getPath(), route.getHandler(), route.getRoles());
        }

        Config.configureJackson();
        IcRequestLogger.configure(app);
        configureExceptions(app);

        // Deserialize UUIDs in urls
        JavalinValidation.register(UUID.class, UUID::fromString);

        if (IcConfig.getEnvironment().equals("local")) {
            app.start(IcConfig.getBaseUrl(), 8888);
        } else {
            app.start(8888);
        }

        logger.info("IC is ready.");
    }

    private static void configureExceptions(Javalin app) {
        // Example response body:
        //
        //  {
        //    "globalErrors": [],
        //    "metadata": {},
        //    "fieldErrors": [
        //      {
        //        "field": "agency",
        //        "error": "Agency is required."
        //      },
        //      {
        //        "field": "date",
        //        "error": "Date is required."
        //      }
        //    ],
        //    "message": "Agency is required.\nDate is required."
        //  }
        app.exception(ValidationException.class, (validationException, ctx) -> {
            List<String> errors = new ArrayList<>();
            errors.addAll(validationException.getGlobalErrors());
            errors.addAll(validationException.getFieldErrors().stream().map(FieldError::getError).collect(toList()));

            String message = errors.stream().collect(joining("\n"));

            Map<String, Object> json = new HashMap<>();

            json.put("fieldErrors", validationException.getFieldErrors().stream().map(fieldError -> {
                Map<String, Object> fieldErrorAsMap = new HashMap<>();
                fieldErrorAsMap.put("field", fieldError.getField());
                fieldErrorAsMap.put("error", fieldError.getError());
                return fieldErrorAsMap;
            }).collect(toList()));

            json.put("globalErrors", validationException.getGlobalErrors());
            json.put("metadata", validationException.getMetadata());
            json.put("message", message);

            ctx.status(422);
            ctx.json(json);
        });
    }

    private static class IcRequestLogger {
        private static final Logger logger = LoggerFactory.getLogger(IcRequestLogger.class.getPackageName() + ".IC_REQUEST");

        public static void configure(Javalin app) {
            CobaltAccountService cobaltAccountService = CobaltAccountService.getSharedInstance();

            app.config.requestLogger(afterRequestLogger());
            app.before(beforeHandler());

            app.before((ctx) -> {
                if(shouldIgnoreRequest(ctx))
                    return;

                DCobaltAccount cobaltAccount = null;
                DPatient patient;
                CobaltClaims cobaltClaims = IcRole.getCobaltClaimsFromJWT(ctx.cookie("accessToken")).orElse(null);

                if(cobaltClaims != null) {
                    IcRole icRole = cobaltClaims.getIcRole();

                    if(icRole == IcRole.MHIC) {
                        // Only do this if you're an MHIC since we already have an IC patient table.
                        // This will create a record in the cobalt_account table (if needed) for non-patient users
                        cobaltAccount = cobaltAccountService.findOrCreateCobaltAccountForClaims(cobaltClaims).get();
                    } else if(icRole == IcRole.PATIENT) {
                        // Anything patient-specific to do?
                    } else {
                        logger.warn("Cobalt claims include unsupported role {}, ignoring...", icRole.name());
                    }
                }

                patient = AuthUtils.getIcPatient(ctx).orElse(null);

                IcContext.set(new IcContext(patient, cobaltAccount));
            });

            app.after((ctx) -> {
                // Ignore CORS noise
                if(ctx.method().equals("OPTIONS"))
                    return;

                IcContext.clear();
            });
        }

        private static Handler beforeHandler() {
            return (ctx) -> {
                if(!logger.isDebugEnabled())
                    return;

                if(shouldIgnoreRequest(ctx))
                    return;

                StringBuilder log = new StringBuilder(format("Received %s %s", ctx.method(), normalizeUrl(ctx.fullUrl())));
                String requestBody = null;

                if(ctx.isMultipart() || ctx.isMultipartFormData())
                    requestBody = "[multipart form data]";
                else
                    requestBody = trimToNull(ctx.body());

                if(requestBody != null && requestBody.length() > 0)
                    log.append(format(". Request body:\n%s", requestBody));

                logger.debug(log.toString());
            };
        }

        private static RequestLogger afterRequestLogger() {
            return (ctx, ms) -> {
                if(!logger.isDebugEnabled())
                    return;

                if(shouldIgnoreRequest(ctx))
                    return;

                StringBuilder log = new StringBuilder(format("[HTTP %d] Took %dms for %s %s", ctx.status(), ms.intValue(), ctx.method(), normalizeUrl(ctx.url())));
                String responseBody = trimToNull(ctx.resultString());

                if(responseBody != null)
                    log.append(format(". Response body:\n%s", ellipsize(responseBody, 400)));

                logger.debug(log.toString());
            };
        }

        private static boolean shouldIgnoreRequest(Context ctx) {
            // Ignore CORS noise
            return ctx.method().equals("OPTIONS");
        }

        private static String normalizeUrl(String url) {
            return url.replace("http://127.0.0.1:8888", "");
        }

        private static String ellipsize(String input, int maxLength) {
            if (input == null || input.length() < maxLength)
                return input;

            return input.substring(0, maxLength) + "... [truncated]";
        }
    }

    protected static class Config {
        public static void configureJackson() {
            SimpleModule fhirModule = new SimpleModule();
            fhirModule.addSerializer(Questionnaire.class, new FhirR4Serializer());
            fhirModule.addDeserializer(Questionnaire.class, new FhirR4Deserializer<>(Questionnaire.class));
            fhirModule.addSerializer(QuestionnaireResponse.class, new FhirR4Serializer());
            fhirModule.addDeserializer(QuestionnaireResponse.class, new FhirR4Deserializer<>(QuestionnaireResponse.class));

            fhirModule.addSerializer(Patient.class, new DSTU3PatientSerializer());
            DatabaseConfig config = new DatabaseConfig();

            // read the ebean.properties and load
            // those settings into this DatabaseConfig object
            config.loadFromProperties();

            ObjectMapper dbObjectMapper = new ObjectMapper();

            dbObjectMapper.registerModule(fhirModule);

            dbObjectMapper.registerModule((new JavaTimeModule())).configure(
                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,false);
            dbObjectMapper.registerModule(new JodaModule()).configure(
                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,false);
            config.setObjectMapper(dbObjectMapper);

            DatabaseFactory.create(config);

            JavalinJackson.getObjectMapper().registerModule(fhirModule);
            JavalinJackson.getObjectMapper().registerModule((new JavaTimeModule())).configure(
                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,false);
            JavalinJackson.getObjectMapper().registerModule(new JodaModule()).configure(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,false);
        }
    }

    private static final AccessManager accessManager = (handler, ctx, permittedRoles) -> {
        // Javalin has a marker interface concept for Roles.  Convert to our roles instead
        Set<IcRole> normalizedPermittedRoles = permittedRoles.stream()
            .map(role -> (IcRole) role)
            .collect(Collectors.toSet());

        if (normalizedPermittedRoles.contains(IcRole.ANYONE)) {
            handler.handle(ctx);
            return;
        }

        IcContext icContext = IcContext.getCurrentContext().orElse(null);

        // You are not authenticated at all
        if(icContext == null)
            throw new UnauthorizedResponse();

        // You are authenticated but you don't have the right role
        if(!IcContext.currentContextHasRole(normalizedPermittedRoles))
            throw new ForbiddenResponse();

        handler.handle(ctx);
    };
}