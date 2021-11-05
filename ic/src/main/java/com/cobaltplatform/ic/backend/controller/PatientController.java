package com.cobaltplatform.ic.backend.controller;

import com.cobaltplatform.ic.backend.auth.AuthUtils;
import com.cobaltplatform.ic.backend.model.auth.FhirTokenResponse;
import com.cobaltplatform.ic.backend.model.auth.IcContext;
import com.cobaltplatform.ic.backend.model.db.DAssessment;
import com.cobaltplatform.ic.backend.model.db.DPatient;
import com.cobaltplatform.ic.backend.model.db.query.QDPatient;
import com.cobaltplatform.ic.backend.model.response.PatientAssessmentDTO;
import com.cobaltplatform.ic.backend.model.response.PatientDemographicsDTO;
import com.cobaltplatform.ic.backend.model.response.PatientResponse;
import com.cobaltplatform.ic.backend.service.PatientService;
import com.cobaltplatform.ic.model.IcRole;
import com.cobaltplatform.ic.backend.service.EpicAuthService;
import io.ebean.DB;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnauthorizedResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;

import static java.util.Objects.requireNonNull;

public class PatientController {
    private static final PatientService patientService = new PatientService();

    private static final Logger logger = LoggerFactory.getLogger(
        PatientController.class);
    public static Handler getPatient = ctx -> {
        var patientService = new PatientService();

        FhirTokenResponse fhirTokenResponse;
        try {
            fhirTokenResponse = AuthUtils.getPatientFhirToken(ctx).get();
        } catch (Exception e) {
            throw new UnauthorizedResponse();
        }

        var fhirPatientObject = patientService.getPatientByToken(fhirTokenResponse);

        PatientResponse returnablePatient = DB.findDto(PatientResponse.class,
            selectPatientQuery("fhir_id = :fhirId"))
            .setParameter("fhirId", fhirTokenResponse.getPatient()).setRelaxedMode().findOneOrEmpty().orElseThrow(NotFoundResponse::new);

        returnablePatient.setEpicProfile(fhirPatientObject);

        ctx.json(returnablePatient);
    };

    public static Handler getPatientId = ctx -> {
        String patientId = ctx.pathParam("id");

        PatientResponse returnablePatient = DB.findDto(PatientResponse.class,
                selectPatientQuery("id = :patientId"))
                .setParameter("patientId", UUID.fromString(patientId)).setRelaxedMode().findOne();

        if (returnablePatient == null) {
            throw new NotFoundResponse();
        }

        DPatient patient = new QDPatient()
            .id.eq(UUID.fromString(patientId))
            .findOneOrEmpty().orElseThrow(NotFoundResponse::new);


        var accessToken = EpicAuthService.getFhirAuthToken();
        var fhirObject = patientService.getPatientByFhirIdAndAccessToken(patient.getFhirId(), accessToken);
        returnablePatient.setEpicProfile(fhirObject);

        ctx.json(returnablePatient);
    };

    public static Handler searchPatients = ctx -> {
        List<DPatient> patients = patientService.searchPatients(ctx.queryParam("query"));
        ctx.json(new HashMap<String, Object>() {{
            put("patients", patients);
        }});
    };

    public static Handler getOrCreateDispositionAndAssessment = ctx -> {
        var request = ctx.pathParam("id", UUID.class);
        if (!request.isValid()) {
            throw new BadRequestResponse();
        }

        UUID patientId = request.get();


        if(!IcContext.currentContextHasRole(IcRole.MHIC) && !IcContext.currentContextHasPatientId(patientId)) {
            throw new UnauthorizedResponse();
        }

        Optional<DAssessment> assessment = patientService.getOrCreateDispositionAndAssessmentForPatient(patientId);
        ctx.json(PatientAssessmentDTO.forAssessment(assessment.orElseThrow(NotFoundResponse::new)));
    };

    public static Handler updateDemographics = ctx -> {
        PatientDemographicsDTO patientDemographicsDTO = ctx.bodyAsClass(PatientDemographicsDTO.class);
        var email = patientDemographicsDTO.getEmail();
        var phone = patientDemographicsDTO.getPhone();

        UUID patientId = ctx.pathParam("id", UUID.class).get();

        logger.debug("Updating Demographics for patient {}", patientId);

        if(!IcContext.currentContextHasRole(IcRole.MHIC) && !IcContext.currentContextHasPatientId(patientId)) {
            throw new UnauthorizedResponse();
        }

        try {
            PatientService.updatePatientDemographics(patientId, email, phone);
            ctx.status(HttpStatus.OK_200);
            ctx.json(patientId);
        } catch (NoSuchElementException e) {
            logger.error("Could not find disposition {}", patientId, e);
            throw new NotFoundResponse();
        }

    };

    @Nonnull
    private static String selectPatientQuery(@Nonnull String whereClause) {
        requireNonNull(whereClause);

        return "select id, cobalt_account_id as cobaltAccountId, logged_in_dt as loggedIn, goals as patientGoals, " +
            "preferred_first_name as preferredFirstName, preferred_last_name as preferredLastName, preferred_email as preferredEmail, " +
            "preferred_phone_number as preferredPhoneNumber, preferred_gender as preferredGender, uid " +
            "from patient where " +
            whereClause;
    }
}
