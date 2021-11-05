package com.cobaltplatform.ic.backend.service;

import com.cobaltplatform.ic.backend.config.IcConfig;
import com.cobaltplatform.ic.backend.model.auth.IcContext;
import com.cobaltplatform.ic.backend.model.cobalt.SendCallMessageRequest;
import com.cobaltplatform.ic.backend.model.db.DAssessment;
import com.cobaltplatform.ic.backend.model.db.DAssessmentStatusChange;
import com.cobaltplatform.ic.backend.model.db.DPatient;
import com.cobaltplatform.ic.backend.model.db.DPatientDisposition;
import com.cobaltplatform.ic.backend.model.db.DResponseItem;
import com.cobaltplatform.ic.backend.model.db.DScoring;
import com.cobaltplatform.ic.backend.model.db.query.QDAssessment;
import com.cobaltplatform.ic.backend.model.db.query.QDResponseItem;
import com.cobaltplatform.ic.backend.model.response.ScoredResponseDTO;
import com.cobaltplatform.ic.model.AcuityCategory;
import com.cobaltplatform.ic.model.Assessment;
import com.cobaltplatform.ic.model.AssessmentStatus;
import com.cobaltplatform.ic.model.DispositionFlag;
import com.cobaltplatform.ic.model.DispositionOutcomeDiagnosis;
import com.cobaltplatform.ic.model.QuestionnaireScoring;
import com.cobaltplatform.ic.model.QuestionnaireType;
import com.cobaltplatform.ic.backend.exception.CobaltException;
import io.ebean.annotation.Transactional;
import io.javalin.http.NotFoundResponse;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class AssessmentService {
    private static final String DEFAULT_CALL_COPY = "This is an alert from IC Digital. A pick patient just took their digital assessment and may be in crisis. Please log into IC Digital and call them back as soon as possible to do safety planning.";
    private static final Logger logger = LoggerFactory.getLogger(AssessmentService.class);

    public static Optional<DAssessment> getLatestAssessment(DPatientDisposition disposition) {
        return new QDAssessment()
                .disposition.id.equalTo(disposition.getId())
                .status.notEqualTo(AssessmentStatus.STALE)
                .orderBy().updatedDt.desc()
                .findOneOrEmpty();
    }

    public static Optional<DAssessment> getOrCreateAssessment(DPatientDisposition disposition) {
        Optional<DAssessment> lastAssessment = getLatestAssessment(disposition);

        return lastAssessment.or(() -> {
            if (disposition.getFlag() == DispositionFlag.NOT_YET_SCREENED) {
                logger.debug("Creating new assessment for disposition {}", disposition.getId());
                var assessment = new DAssessment()
                        .setPatient(disposition.getPatient())
                        .setDisposition(disposition)
                        .setStatus(AssessmentStatus.NOT_STARTED);
                assessment.save();

                var assessmentStatusChange = new DAssessmentStatusChange();
                assessmentStatusChange.setAssessment(assessment);
                assessmentStatusChange.setOldStatus(null);
                assessmentStatusChange.setNewStatus(assessment.getStatus());
                assessmentStatusChange.setAccount(IcContext.currentContextCobaltAccount().orElse(null));
                assessmentStatusChange.setPatient(IcContext.currentContextPatient().orElse(null));

                assessmentStatusChange.save();

                return Optional.of(assessment);
            }
            return Optional.empty();
        });
    }


    public static Optional<DAssessment> getAssessmentById(UUID assessmentId) {
        return new QDAssessment()
                .id.equalTo(assessmentId)
                .findOneOrEmpty();
    }

    @Transactional
    public static void saveResponses(DAssessment assessment, QuestionnaireResponse questionnaireResponse) {
        // Delete old responses with relevant link ids
        Set<String> includedLinkIds = questionnaireResponse.getItem().stream().map(QuestionnaireResponse.QuestionnaireResponseItemComponent::getLinkId).collect(Collectors.toSet());
        new QDResponseItem().assessment.id.eq(assessment.getId()).linkId.in(includedLinkIds).delete();

        questionnaireResponse.getItem().forEach(item -> {
            // TODO: Handle bad answer format
            item.getAnswer().forEach(answer -> {
                DResponseItem response = new DResponseItem(assessment, item.getLinkId());
                if (answer.hasValueCoding()) {
                    response.setCodingValue(answer.getValueCoding());
                } else if (answer.hasValueBooleanType()) {
                    response.setBooleanValue(answer.getValueBooleanType().booleanValue());
                } else if (answer.hasValueStringType()) {
                    response.setStringValue(answer.getValueStringType().asStringValue());
                } else {
                    throw new io.javalin.http.BadRequestResponse("Unhandled Format");
                }
                response.save();
            });
            logger.trace("Saving Response");
        });

        AssessmentStatus previousStatus = assessment.getStatus();

        assessment.setStatus(AssessmentStatus.IN_PROGRESS);
        assessment.save();

        if(previousStatus != AssessmentStatus.IN_PROGRESS) {
            var assessmentStatusChange = new DAssessmentStatusChange();
            assessmentStatusChange.setAssessment(assessment);
            assessmentStatusChange.setOldStatus(previousStatus);
            assessmentStatusChange.setNewStatus(assessment.getStatus());
            assessmentStatusChange.setAccount(IcContext.currentContextCobaltAccount().orElse(null));
            assessmentStatusChange.setPatient(IcContext.currentContextPatient().orElse(null));

            assessmentStatusChange.save();
        }

        List<DResponseItem> responses = new QDResponseItem().assessment.id.eq(assessment.getId()).findList();
        // Check Crisis
        Assessment myAssessment = new Assessment(assessment.getPatient(), responses.stream().collect(Collectors.groupingBy(DResponseItem::getLinkId)));
        if(myAssessment.isCrisis()){
            DPatientDisposition disposition = assessment.getDisposition();
            // Only one crisis notification per disposition, but possibly do this at the assessment level instead?
            if(!disposition.isCrisis()){
                disposition.setCrisis(true);
                disposition.setAcuityCategory(AcuityCategory.HIGH);
                disposition.setFlag(DispositionOutcomeDiagnosis.CRISIS_CARE.getFlag());
                disposition.setDiagnosis(DispositionOutcomeDiagnosis.CRISIS_CARE);
                disposition.save();
                DPatient patient = assessment.getPatient();

                try {
                    CobaltService.getSharedInstance().sendCallMessages(IcConfig.getCrisisPhoneNumbers().stream()
                        .map(phoneNumber -> new SendCallMessageRequest(phoneNumber, DEFAULT_CALL_COPY))
                        .collect(Collectors.toList()));
                } catch (CobaltException e) {
                    logger.error("Failed to send message", e);
                }
            }

        }
    }

    public static QuestionnaireResponse loadQuestionnaireResponse(UUID assessmentID){
        DAssessment assessment = new QDAssessment().id.equalTo(assessmentID).findOneOrEmpty().orElseThrow(NotFoundResponse::new);

        Map<String, List<DResponseItem>> responsesByLinkId = assessment.getResponses().stream()
                .collect(Collectors.groupingBy(DResponseItem::getLinkId));

        QuestionnaireResponse response = new QuestionnaireResponse();

        responsesByLinkId.forEach((linkId, items) -> {
            QuestionnaireResponse.QuestionnaireResponseItemComponent item = response.addItem().setLinkId(linkId);
            items.forEach(i -> {
                i.getBooleanValue().ifPresent(v -> item.addAnswer().setValue(new BooleanType(v)));
                i.getCodingValue().ifPresent(v -> item.addAnswer().setValue(v));
                i.getStringValue().ifPresent(v -> item.addAnswer().setValue(new StringType(v)));
            });
        });


        if(assessment.getStatus().equals(AssessmentStatus.COMPLETED)){
            response.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
        } else if(assessment.getStatus().equals(AssessmentStatus.IN_PROGRESS)){
            response.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS);
        }

        return response;
    }

    public static Questionnaire loadQuestionnaire(DAssessment assessment) {
        Map<String, List<DResponseItem>> responsesByLinkId = new QDResponseItem().assessment.id.equalTo(assessment.getId())
                .findStream().collect(Collectors.groupingBy(DResponseItem::getLinkId));

        Assessment myAssessment = new Assessment(assessment.getPatient(), responsesByLinkId);

        // TODO: Basically all of this can be cached. There are a handful of pivotal linkIds -- use their intersection as a cache key

        List<Questionnaire.QuestionnaireItemComponent> items = Arrays.stream(QuestionnaireType.values())
                .filter(q -> q.getQuestionnaire().isPresent())
                .filter(q -> q.includeInAssessment(myAssessment))
                .map(q -> q.getQuestionnaire().orElseThrow())
                .flatMap(q -> q.getItem().stream())
                .map(Questionnaire.QuestionnaireItemComponent::copy) // So the next set doesn't blow away the cached files
                .collect(Collectors.toList());

        Set<String> seenLinkIds = new HashSet<>();
        for (var item: items){
            item.getItem().removeIf(i -> seenLinkIds.contains(i.getLinkId()));
            item.getItem().forEach(i -> seenLinkIds.add(i.getLinkId()));
        }

        List<Questionnaire.QuestionnaireItemComponent> itemsToInclude = items.stream()
                .filter(i -> i.getType() == Questionnaire.QuestionnaireItemType.DISPLAY || i.getItem().size() > 0)
                .collect(Collectors.toList());


        return new Questionnaire().setItem(itemsToInclude);
    }

    @Transactional
    public static DAssessment completeAssessment(DAssessment assessment){
        AssessmentStatus previousStatus = assessment.getStatus();

        assessment.setStatus(AssessmentStatus.COMPLETED);

        if(previousStatus != AssessmentStatus.COMPLETED) {
            var assessmentStatusChange = new DAssessmentStatusChange();
            assessmentStatusChange.setAssessment(assessment);
            assessmentStatusChange.setOldStatus(previousStatus);
            assessmentStatusChange.setNewStatus(assessment.getStatus());
            assessmentStatusChange.setAccount(IcContext.currentContextCobaltAccount().orElse(null));
            assessmentStatusChange.setPatient(IcContext.currentContextPatient().orElse(null));

            assessmentStatusChange.save();
        }

        Map<String, List<DResponseItem>> responsesByLinkId = new QDResponseItem().assessment.id.equalTo(assessment.getId())
                .findStream().collect(Collectors.groupingBy(DResponseItem::getLinkId));

        Assessment myAssessment = new Assessment(assessment.getPatient(), responsesByLinkId);
        Map<QuestionnaireType, QuestionnaireScoring> scores = myAssessment.getScores();
        scores.forEach((key, value) -> {
            DScoring s = new DScoring();
            s.setAssessment(assessment)
                    .setAcuity(value.getAcuityCategory())
                    .setScore(value.getScore())
                    .setQuestionnaireType(key);
            s.save();
        });

        // TODO: Set Disposition
        assessment.save();
        DPatientDisposition disposition = assessment.getDisposition();

        if(disposition.isCrisis()){
            logger.debug("Not setting disposition to assessment score because of crisis");
        } else {
            myAssessment.getDiagnosis().ifPresentOrElse(d -> {
                disposition.setDiagnosis(d).setFlag(d.getFlag());
            }, () -> {
                logger.warn("Finalizing assessment with no diagnosis");
            });
            myAssessment.getOverallAcuity().ifPresentOrElse(disposition::setAcuityCategory, () -> {
                logger.warn("Finalizing assessment with no acuity");
            });
        }

        disposition.save();

        return assessment;
    }

    private static Optional<QuestionnaireResponse> buildResponse(QuestionnaireType questionnaireType, Map<String, List<DResponseItem>> responsesByLinkId){
        List<Questionnaire.QuestionnaireItemComponent> questions = questionnaireType.getQuestions();

        QuestionnaireResponse response = new QuestionnaireResponse();

        int totalResponses = questions.stream().filter(q -> responsesByLinkId.containsKey(q.getLinkId())).mapToInt(q -> {
            List<DResponseItem> responses = responsesByLinkId.get(q.getLinkId());

            QuestionnaireResponse.QuestionnaireResponseItemComponent item = response.addItem().setLinkId(q.getLinkId());
            item.setText(q.getText());

            responses.forEach(i -> {
                i.getBooleanValue().ifPresent(v -> item.addAnswer().setValue(new BooleanType(v)));
                i.getCodingValue().ifPresent(v -> {
                    QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent answer = item.addAnswer().setValue(v);
                    Optional<Questionnaire.QuestionnaireItemAnswerOptionComponent> selectedOption = q.getAnswerOption().stream().filter(a -> a.getValueCoding().getCode().equals(v.getCode())).findFirst();
                    selectedOption.ifPresent( o -> {
                        answer.addExtension(o.getExtensionByUrl("http://hl7.org/fhir/StructureDefinition/ordinalValue"));
                    });
                });
                i.getStringValue().ifPresent(v -> item.addAnswer().setValue(new StringType(v)));
            });
            return responses.size();
        }).sum();
        
        // Return empty if there were no responses
        if(totalResponses > 0){
            return Optional.of(response);

        }
        return Optional.empty();
    }

    public static List<ScoredResponseDTO> getScoredResponses(DAssessment assessment){
        Map<String, List<DResponseItem>> responsesByLinkId = assessment.getResponses().stream()
                .collect(Collectors.groupingBy(DResponseItem::getLinkId));

        return Arrays.stream(QuestionnaireType.values()).flatMap(qt -> {
            Optional<QuestionnaireResponse> response = buildResponse(qt, responsesByLinkId);
            return response.map(r -> {
                if(assessment.getStatus() == AssessmentStatus.COMPLETED) {
                    r.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
                } else {
                    r.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS);
                }
                ScoredResponseDTO scoredResponse = new ScoredResponseDTO(qt, r);
                Optional<QuestionnaireScoring> score = qt.score(responsesByLinkId, assessment.getPatient());

                score.ifPresent(s -> {
                    scoredResponse
                            .setScore(s.getScore())
                            .setAcuity(s.getAcuityCategory());
                });
                return scoredResponse;
            }).stream();
        }).collect(Collectors.toList());
    }
}


