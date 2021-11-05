package com.cobaltplatform.ic.backend;

import com.cobaltplatform.ic.backend.auth.PatientAuthHandlers;
import com.cobaltplatform.ic.backend.controller.AssessmentController;
import com.cobaltplatform.ic.backend.controller.CobaltController;
import com.cobaltplatform.ic.backend.controller.ContactController;
import com.cobaltplatform.ic.backend.controller.DispositionController;
import com.cobaltplatform.ic.backend.controller.OrderReportController;
import com.cobaltplatform.ic.backend.controller.PatientController;
import com.cobaltplatform.ic.backend.controller.SystemController;
import com.cobaltplatform.ic.backend.controller.TriageReviewController;
import com.cobaltplatform.ic.model.IcRole;
import io.javalin.core.security.Role;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;

import java.util.Set;

public enum Routes {
    OAUTH_CALLBACK(RoutePathString.OAUTH_CALLBACK, HandlerType.GET, PatientAuthHandlers.handleCallback, IcRole.ANYONE),
    AUTH_REDIRECT(RoutePathString.AUTH_REDIRECT, HandlerType.GET, PatientAuthHandlers.handleAuthRedirect, IcRole.ANYONE),
    PATIENT_LOGOUT(RoutePathString.LOGOUT, HandlerType.GET, PatientAuthHandlers.handleLogout, IcRole.ANYONE),
    GET_PATIENT(RoutePathString.PATIENT, HandlerType.GET, PatientController.getPatient, IcRole.PATIENT, IcRole.MHIC),
    GET_PATIENTS(RoutePathString.PATIENTS, HandlerType.GET, PatientController.searchPatients, IcRole.MHIC),
    POST_ASSESSMENT_RESPONSES(RoutePathString.ASSESSMENT_RESPONSES, HandlerType.POST, AssessmentController.postResponses, IcRole.PATIENT, IcRole.MHIC),
    GET_ASSESSMENT_RESPONSES(RoutePathString.ASSESSMENT_RESPONSES, HandlerType.GET, AssessmentController.getResponses, IcRole.PATIENT, IcRole.MHIC),
    GET_ASSESSMENT_QUESTIONNAIRE(RoutePathString.ASSESSMENT_QUESTIONNAIRE, HandlerType.GET, AssessmentController.getQuestionnaire, IcRole.PATIENT, IcRole.MHIC),
    PUT_ASSESSMENT_COMPLETE(RoutePathString.ASSESSMENT_COMPLETE, HandlerType.PUT, AssessmentController.putComplete, IcRole.PATIENT, IcRole.MHIC),
    GET_DISPOSITION(RoutePathString.DISPOSITION, HandlerType.GET, DispositionController.getPatientDisposition, IcRole.PATIENT, IcRole.MHIC),
    PUT_DISPOSITION_ACKNOWLEDGE_CRISIS(RoutePathString.DISPOSITION_CRISIS_ACKNOWLEDGED, HandlerType.PUT, DispositionController.patientDispositionCrisisAcknowledged, IcRole.PATIENT),
    GET_DISPOSITION_FOR_MHIC(RoutePathString.DISPOSITION_FOR_MHIC, HandlerType.GET, DispositionController.getPatientDispositionForMhic, IcRole.MHIC),
    POST_DISPOSITION(RoutePathString.DISPOSITION, HandlerType.POST, DispositionController.postPatientDisposition, IcRole.PATIENT, IcRole.MHIC),
    DISPOSITION_RESPONSES_FOR_MHIC(RoutePathString.DISPOSITION_RESPONSES_FOR_MHIC, HandlerType.GET, DispositionController.getQuestionnaireResponses, IcRole.MHIC),
    GET_IC_PUBLIC_KEY(RoutePathString.SYSTEM_PUBLIC_KEY, HandlerType.GET, SystemController.getPublicKey, IcRole.ANYONE),
    GET_IC_MHIC_PATIENT_ID(RoutePathString.PATIENT_ID, HandlerType.GET, PatientController.getPatientId, IcRole.MHIC),
    GET_DISPOSITIONS_MHIC(RoutePathString.DISPOSITIONS, HandlerType.GET, DispositionController.getAllPatientDispositions, IcRole.MHIC),
    PUT_DISPOSITION_FLAG(RoutePathString.DISPOSITION_FLAG, HandlerType.PUT, DispositionController.updateDispositionFlag, IcRole.MHIC),
    ORDER_UPLOAD(RoutePathString.ORDER_UPLOAD, HandlerType.POST, OrderReportController.uploadOrderReportCsv, IcRole.MHIC),
    GET_DISPOSITION_CONTACT_FOR_MHIC(RoutePathString.DISPOSITION_CONTACT, HandlerType.GET, ContactController.getAllPatientContactsByDispositionId, IcRole.MHIC),
    POST_DISPOSITION_CONTACT(RoutePathString.DISPOSITION_CONTACT, HandlerType.POST, ContactController.postContact, IcRole.MHIC),
    PUT_DISPOSITION_TRIAGE_REVIEW(RoutePathString.DISPOSITION_TRIAGE_REVIEW, HandlerType.PUT, TriageReviewController.updateTriageReview, IcRole.MHIC),
    GET_IS_BUSINESS_HOURS(RoutePathString.IS_BUSINESS_HOURS, HandlerType.GET, SystemController.isBusinessHours, IcRole.ANYONE),
    GET_OR_CREATE_ASSESSMENT_AND_DISPOSITION(RoutePathString.ASSESSMENT_AND_DISPOSITION, HandlerType.GET, PatientController.getOrCreateDispositionAndAssessment, IcRole.PATIENT, IcRole.MHIC),
    GET_DISPOSITION_BY_ID(RoutePathString.DISPOSITION_BY_ID, HandlerType.GET, DispositionController.getDispositionById, IcRole.MHIC),
    GET_OR_CREATE_ASSESSMENT_FOR_DISPOSITION(RoutePathString.CURRENT_ASSESSMENT_FOR_DISPOSITION, HandlerType.GET, DispositionController.getOrCreateAssessmentForDisposition, IcRole.MHIC),
    PUT_DISPOSITION_OUTCOME(RoutePathString.DISPOSITION_OUTCOME, HandlerType.PUT, DispositionController.updateDispositionOutcome, IcRole.MHIC),
    PUT_PATIENT_DEMOGRAPHICS(RoutePathString.PATIENT_DEMOGRAPHICS, HandlerType.PUT, PatientController.updateDemographics, IcRole.MHIC, IcRole.PATIENT),
    COBALT_APPOINTMENT_CREATED(RoutePathString.COBALT_APPOINTMENT_CREATED, HandlerType.POST, CobaltController.appointmentCreated, IcRole.ANYONE),
    COBALT_APPOINTMENT_CANCELED(RoutePathString.COBALT_APPOINTMENT_CANCELED, HandlerType.POST, CobaltController.appointmentCanceled, IcRole.ANYONE),
    POST_DISPOSITION_SPECIALTY_CARE_SCHEDULING(RoutePathString.DISPOSITION_SPECIALTY_CARE_SCHEDULING, HandlerType.POST, DispositionController.createOrUpdateSpecialtyCareScheduling, IcRole.MHIC),
    DELETE_DISPOSITION_SPECIALTY_CARE_SCHEDULING(RoutePathString.DISPOSITION_SPECIALTY_CARE_SCHEDULING, HandlerType.DELETE, DispositionController.deleteSpecialtyCareScheduling, IcRole.MHIC),
    POST_DISPOSITION_NOTE(RoutePathString.DISPOSITION_NOTE_CREATE, HandlerType.POST, DispositionController.createNote, IcRole.MHIC, IcRole.BHS, IcRole.PROVIDER),
    DELETE_DISPOSITION_NOTE(RoutePathString.DISPOSITION_NOTE_DELETE, HandlerType.DELETE, DispositionController.deleteNote, IcRole.MHIC, IcRole.BHS, IcRole.PROVIDER),
    ;

    protected enum RoutePathString {
        OAUTH_CALLBACK("oauth/callback"),
        AUTH_REDIRECT("auth/redirect"),
        LOGOUT("logout"),
        PATIENT_ID("mhic/patients/:id"),
        PATIENT("patient"),
        PATIENTS("patients"),
        PATIENT_DEMOGRAPHICS("patient/:id/demographics"),
        ASSESSMENT_RESPONSES("assessment/:id/responses"),
        ASSESSMENT_QUESTIONNAIRE("assessment/:id/questionnaire"),
        ASSESSMENT_COMPLETE("assessment/:id/complete"),
        DISPOSITION("disposition"),
        DISPOSITION_CRISIS_ACKNOWLEDGED("disposition/:id/crisis-acknowledged"),
        DISPOSITION_FOR_MHIC("mhic/disposition"),
        DISPOSITION_BY_ID("mhic/disposition/:id"),
        CURRENT_ASSESSMENT_FOR_DISPOSITION("mhic/disposition/:id/assessment"),
        DISPOSITION_RESPONSES_FOR_MHIC("mhic/disposition/:id/responses"),
        DISPOSITION_FLAG("mhic/disposition/:id/flag"),
        DISPOSITION_CONTACT("mhic/disposition/:id/contact"),
        DISPOSITION_TRIAGE_REVIEW("mhic/disposition/:id/triage-review"),
        DISPOSITION_OUTCOME("mhic/disposition/:id/outcome"),
        DISPOSITION_SPECIALTY_CARE_SCHEDULING("mhic/disposition/:id/specialty-care-scheduling"),
        DISPOSITIONS("mhic/dispositions"),
        SYSTEM_PUBLIC_KEY("system/public-key"),
        ORDER_UPLOAD("order-upload"),
        IS_BUSINESS_HOURS("system/is-business-hours"),
        ASSESSMENT_AND_DISPOSITION("patient/:id/assessment"),
        COBALT_APPOINTMENT_CREATED("cobalt/appointment-created"),
        COBALT_APPOINTMENT_CANCELED("cobalt/appointment-canceled"),
        DISPOSITION_NOTE_CREATE("mhic/disposition-notes"),
        DISPOSITION_NOTE_DELETE("mhic/disposition-notes/:id"),
        ;

        public String toString() {
            return path;
        }

        private final String path;

        RoutePathString(String path) {
            this.path = path;
        }
    }

    private final RoutePathString path;
    private final Handler handler;
    private final HandlerType handlerType;
    private final Set<Role> roles;

    Routes(RoutePathString routeUrl, final HandlerType handlerType, final Handler handler, final IcRole... roles) {
        this.path = routeUrl;
        this.handler = handler;
        this.handlerType = handlerType;
        this.roles = Set.of(roles);
    }

    public HandlerType getHandlerType() {
        return handlerType;
    }

    public String getPath() {
        return path.toString();
    }

    public Handler getHandler() {
        return handler;
    }

    public Set<Role> getRoles() {
        return roles;
    }
}
