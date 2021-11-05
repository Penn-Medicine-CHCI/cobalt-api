package com.cobaltplatform.ic.backend.controller;

import com.cobaltplatform.ic.backend.model.response.TriageReviewDTO;
import com.cobaltplatform.ic.backend.service.TriageReviewService;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;
import java.util.UUID;

public class TriageReviewController {
    private static final Logger logger = LoggerFactory.getLogger(TriageReviewController.class);
    public static Handler updateTriageReview = ctx -> {
        UUID dispositionId = ctx.pathParam("id", UUID.class).get();
        TriageReviewDTO triageReviewDTO = ctx.bodyAsClass(TriageReviewDTO.class);

        var comment = triageReviewDTO.getComment();
        var focusedReview = triageReviewDTO.getNeedsFocusedReview();
        var bhpReview = triageReviewDTO.getBhpReviewedDt();
        var psyReview = triageReviewDTO.getPsychiatristReviewedDt();

        try {
            TriageReviewService.putTriageReview(dispositionId, comment, focusedReview, bhpReview, psyReview);
            ctx.status(HttpStatus.OK_200);
            ctx.json(dispositionId);
        } catch (NoSuchElementException e) {
            logger.error("Could not find triage review {}", dispositionId, e);
            throw new NotFoundResponse();
        }
    };
}
