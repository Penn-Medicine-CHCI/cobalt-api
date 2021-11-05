package com.cobaltplatform.ic.backend.controller;

import com.cobaltplatform.ic.backend.auth.AuthUtils;
import com.cobaltplatform.ic.backend.model.auth.IcContext;
import com.cobaltplatform.ic.backend.model.db.DAssessment;
import com.cobaltplatform.ic.backend.model.db.DDispositionNote;
import com.cobaltplatform.ic.backend.model.db.DPatient;
import com.cobaltplatform.ic.backend.model.db.DPatientDisposition;
import com.cobaltplatform.ic.backend.model.request.DispositionNoteCreateRequest;
import com.cobaltplatform.ic.backend.model.response.DispositionFlagDTO;
import com.cobaltplatform.ic.backend.model.response.DispositionOutcomeDTO;
import com.cobaltplatform.ic.backend.model.response.DispositionResponse;
import com.cobaltplatform.ic.backend.model.response.PatientAssessmentDTO;
import com.cobaltplatform.ic.backend.model.response.ScoredResponseDTO;
import com.cobaltplatform.ic.backend.service.AssessmentService;
import com.cobaltplatform.ic.backend.service.DispositionService;
import com.cobaltplatform.ic.model.DispositionFlag;
import com.cobaltplatform.ic.model.DispositionNote;
import com.cobaltplatform.ic.model.DispositionOutcomeDiagnosis;
import com.cobaltplatform.ic.model.SpecialtyCareScheduling;
import io.javalin.core.validation.Validator;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnauthorizedResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DispositionController {
    public static Handler postPatientDisposition = ctx -> {

    };
    private static final Logger logger = LoggerFactory.getLogger(DispositionController.class);
    public static Handler getPatientDisposition = ctx -> {
        DPatient patient = AuthUtils.getIcPatient(ctx).get();
        DispositionResponse response = DispositionService.getLatestDispositionForPatient(patient.getId())
                .map(DispositionResponse::fromDPatientDisposition)
                .orElse(new DispositionResponse());
        ctx.json(response);
    };

    public static Handler getPatientDispositionForMhic = ctx -> {
        // MHICs are permitted to query for dispositions for a specific patient
        UUID patientId = ctx.queryParam("patientId", UUID.class).getOrNull();
        UUID cobaltAccountId = ctx.queryParam("cobaltAccountId", UUID.class).getOrNull();

        if(patientId != null && cobaltAccountId != null)
            throw new BadRequestResponse("You cannot specify both patientId and cobaltAccountId query parameters");

        DispositionResponse response = null;

        if(patientId != null)
            response = DispositionService.getLatestDispositionForPatient(patientId)
                    .map(DispositionResponse::fromDPatientDisposition).orElse(null);
        else if(cobaltAccountId != null)
            response = DispositionService.getLatestDispositionForCobaltAccountId(cobaltAccountId)
                    .map(DispositionResponse::fromDPatientDisposition).orElse(null);

        ctx.json(response == null ? new DispositionResponse() : response);
    };

    public static Handler getAllPatientDispositions = ctx -> {

            List<DispositionResponse> response = DispositionService.getDispositionsForAllPatients().stream()
                .map(DispositionResponse::fromDPatientDisposition)
                // .sorted(new SortByFlag())
                // TODO: this is temporary until we get the correct solution figured out
                .sorted((DispositionResponse a, DispositionResponse b) -> {
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList());

        ctx.json(response);
    };

    public static Handler updateDispositionFlag = ctx -> {
        var flagId = ctx.bodyAsClass(DispositionFlagDTO.class).getId();
        DispositionFlag newFlag;
        try {
            newFlag = DispositionFlag.valueOf(flagId);
        } catch (NoSuchElementException e)
        {
            logger.error("Could not find flag with id {}", flagId, e);
            throw new NotFoundResponse();
        }

        UUID dispositionId = ctx.pathParam("id", UUID.class).get();
        logger.debug("Updating Disposition {} to status {}", dispositionId, newFlag);

        try {
            DispositionService.updateDispositionFlag(dispositionId, newFlag);
            ctx.status(HttpStatus.OK_200);
            ctx.json(newFlag);
        } catch (NoSuchElementException e) {
            logger.error("Could not find disposition {}", dispositionId, e);
            throw new NotFoundResponse();
        }
    };

    public static Handler updateDispositionOutcome = ctx -> {
        var code = ctx.bodyAsClass(DispositionOutcomeDTO.class).getCode();

        Optional<DispositionOutcomeDiagnosis> newOutcome = DispositionOutcomeDiagnosis.fromCode(code);

        if (newOutcome.isEmpty()) {
            logger.error("Could not find outcome for code {}", code);
            throw new NotFoundResponse();
        }

        UUID dispositionId = ctx.pathParam("id", UUID.class).get();
        logger.debug("Updating Disposition {} to outcome {}", dispositionId, newOutcome);

        try {
            DispositionService.updateDispositionOutcome(dispositionId, newOutcome.get());
            ctx.status(HttpStatus.OK_200);
            ctx.json(newOutcome.get());
        } catch (NoSuchElementException e) {
            logger.error("Could not find disposition {}", dispositionId, e);
            throw new NotFoundResponse();
        }
    };

    public static Handler getQuestionnaireResponses = ctx -> {
        Validator<UUID> dispositionIdParam = ctx.pathParam("id", UUID.class);
        if (dispositionIdParam.isValid()) {
            UUID dispositionId = dispositionIdParam.get();
            Optional<DPatientDisposition> disposition = DispositionService.getDisposition(dispositionId);
            if(disposition.isEmpty()) {
                throw new NotFoundResponse();
            }
            Optional<DAssessment> assessment = AssessmentService.getLatestAssessment(disposition.get());
            if(assessment.isEmpty()){
                ctx.json(Collections.emptyList());
                return;
            }

            List<ScoredResponseDTO> scoredResponses = AssessmentService.getScoredResponses(assessment.get());
            ctx.json(scoredResponses);
        }
        else {
            throw new NotFoundResponse();
        }
    };

    public static Handler getOrCreateAssessmentForDisposition = ctx -> {
        Validator<UUID> dispositionIdParam = ctx.pathParam("id", UUID.class);
        if (dispositionIdParam.isValid()) {
            UUID dispositionId = dispositionIdParam.get();
            Optional<DPatientDisposition> disposition = DispositionService.getDisposition(dispositionId);
            if (disposition.isEmpty()) {
                throw new NotFoundResponse();
            }
            Optional<DAssessment> assessment = AssessmentService.getOrCreateAssessment(disposition.get());
            ctx.json(PatientAssessmentDTO.forAssessment(assessment.orElseThrow(NotFoundResponse::new)));
        } else {
            throw new NotFoundResponse();
        }
    };

    public static Handler getDispositionById = ctx -> {
        Validator<UUID> dispositionIdParam = ctx.pathParam("id", UUID.class);
        if (dispositionIdParam.isValid()) {
            UUID dispositionId = dispositionIdParam.get();
            DispositionResponse result = DispositionService.getDisposition(dispositionId)
                    .map(DispositionResponse::fromDPatientDisposition)
                    .orElseThrow(NotFoundResponse::new);
            ctx.json(result);
        } else {
            throw new NotFoundResponse();
        }
    };

    public static Handler patientDispositionCrisisAcknowledged = ctx -> {
        performDispositionOperation(ctx, (disposition) -> {
            DPatient patient = AuthUtils.getIcPatient(ctx).get();

            // Can't update someone else's disposition
            if(!disposition.getPatient().getId().equals(patient.getId()))
                throw new UnauthorizedResponse();

            disposition = DispositionService.updateDispositionCrisisAcknowledged(disposition.getId(), true);
            ctx.json(DispositionResponse.fromDPatientDisposition(disposition));
        });
    };

    public static Handler createOrUpdateSpecialtyCareScheduling = ctx -> {
        performDispositionOperation(ctx, (disposition) -> {
            SpecialtyCareScheduling specialtyCareScheduling;

            try {
                specialtyCareScheduling = ctx.bodyAsClass(SpecialtyCareScheduling.class);
            } catch(Exception e) {
                throw new BadRequestResponse();
            }

            disposition = DispositionService.setSpecialtyCareScheduling(disposition.getId(), specialtyCareScheduling);
            ctx.json(DispositionResponse.fromDPatientDisposition(disposition));
        });
    };

    public static Handler deleteSpecialtyCareScheduling = ctx -> {
        performDispositionOperation(ctx, (disposition) -> {
            disposition = DispositionService.setSpecialtyCareScheduling(disposition.getId(), null);
            ctx.json(DispositionResponse.fromDPatientDisposition(disposition));
        });
    };

    // Assumes path parameter named "id" holds patient_disposition_id
    private static void performDispositionOperation(Context ctx, Consumer<DPatientDisposition> dispositionConsumer) {
        Validator<UUID> dispositionIdParam = ctx.pathParam("id", UUID.class);

        if (dispositionIdParam.isValid()) {
            UUID dispositionId = dispositionIdParam.get();
            DPatientDisposition disposition = DispositionService.getDisposition(dispositionId).orElse(null);

            if(disposition == null)
                throw new NotFoundResponse();

            dispositionConsumer.accept(disposition);
        } else {
            throw new NotFoundResponse();
        }
    }

    public static Handler createNote = ctx -> {
        var request  = ctx.bodyAsClass(DispositionNoteCreateRequest.class);
        request.setAccountId(IcContext.getCurrentContext().get().getCobaltAccount().get().getAccountId());

        DDispositionNote dispositionNote = DispositionService.createDispositionNote(request);
        ctx.json(new HashMap<String, Object>() {{
            put("dispositionNote", DispositionNote.from(dispositionNote).get());
        }});
    };

    public static Handler deleteNote = ctx -> {
        Validator<UUID> dispositionNoteIdParam = ctx.pathParam("id", UUID.class);
        if (dispositionNoteIdParam.isValid()) {
            UUID dispositionNoteId = dispositionNoteIdParam.get();
            DispositionService.deleteDispositionNote(dispositionNoteId);
        } else {
            throw new NotFoundResponse();
        }
    };
}
