package com.cobaltplatform.ic.backend.service;

import com.cobaltplatform.ic.backend.model.db.DPatientDisposition;
import com.cobaltplatform.ic.backend.model.db.DTriageReview;
import com.cobaltplatform.ic.backend.model.db.query.QDPatientDisposition;
import com.cobaltplatform.ic.backend.model.db.query.QDTriageReview;
import io.ebean.annotation.Transactional;
import io.javalin.http.NotFoundResponse;
import org.joda.time.DateTime;

import java.util.Optional;
import java.util.UUID;

public class TriageReviewService {
    @Transactional
    public static DTriageReview putTriageReview(UUID dispositionId, String comment, Boolean focusedReview, DateTime bhpReview, DateTime psyReview) {
        DTriageReview triageReview =  new QDTriageReview().where().disposition.id.eq(dispositionId).setMaxRows(1).findOneOrEmpty().orElse(new DTriageReview());

        Optional<DPatientDisposition> disposition = new QDPatientDisposition().id.equalTo(dispositionId).findOneOrEmpty();

        if (disposition.isPresent()) {
            triageReview
                    .setComment(comment)
                    .setBhpReviewedDt(bhpReview)
                    .setPsychiatristReviewedDt(psyReview)
                    .setNeedsFocusedReview(focusedReview)
                    .setDisposition(disposition.get())
                    .save();

            disposition.get()
                    .setTriageReview(triageReview)
                    .save();

            return triageReview;
        }
        else {
            throw new NotFoundResponse(String.format("Disposition not found with id %s", dispositionId));
        }

    }
}
