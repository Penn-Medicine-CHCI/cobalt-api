package com.cobaltplatform.ic.model;

import com.cobaltplatform.ic.backend.model.db.DPatient;
import com.cobaltplatform.ic.backend.model.db.DResponseItem;
import com.cobaltplatform.ic.backend.service.QuestionnaireService;
import org.hl7.fhir.r4.model.Questionnaire;

import java.util.*;
import java.util.function.LongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum QuestionnaireType {
    // Does this form actually exist?
    INFO("/personalInformationForm", false) {
        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            return Optional.empty();
        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            return false;
        }
    },
    // There's probably a better way to handle the flow for these next two
    SYMPTOMS("patientSelectedSymptoms", false){
        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            return Optional.empty();
        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            // There is probably a better way to handle this flow
            return false;
        }
    },
    DIAGNOSES("patientSelectedDiagnoses", false){
        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            return Optional.empty();
        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            // There is probably a better way to handle this flow
            return false;
        }
    },
    MILITARY("/ic-military", false){
        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            return Optional.empty();
        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            return false;
        }
    },
    /* This is a hack. We don't have functionality to conditionally display questions, so this takes advantage of
       deduping functionality to put a non-scored 3-question CSSRS in front of a scored 4-question one, and only show the questions 3 times
     */
    CSSRS_SHORT("/93373-9-short", "short-CSSRS-R4-3question.json", false){
        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            return Optional.empty();
        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            return true;
        }
    },
    CSSRS("/93373-9", "short-CSSRS-R4.json"){
        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            long score = QuestionnaireScoring.countYesResponses(this.getQuestions().stream()
                    .filter(q -> responses.containsKey(q.getLinkId()))
                    .map(q -> responses.get(q.getLinkId()).get(0))
                    .collect(Collectors.toList()));
            AcuityCategory ac = score > 0 ? AcuityCategory.HIGH : AcuityCategory.LOW;

            return Optional.of(new QuestionnaireScoring(score, ac));
        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            return assessment.getSingleResponse("/93267-3")
                    .flatMap(DResponseItem::getCodingValue)
                    .map(i -> i.getCode().equals(LOINCAnswerCode.YES.getAnswerId())).orElse(false);
        }
    },
    PHQ9("/44249-1", "PHQ9-R4.json") {
        private final String QUESTION_9_LINK_ID = "/44260-8";
        private final String QUESTION_10_LINK_ID = "/69722-7";

        final LongFunction<AcuityCategory> calculateAcuityCategory = QuestionnaireScoring.acuityFromThresholds(17, 21);

        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            List<String> scorableAnswers = getQuestions().stream()
                    .map(Questionnaire.QuestionnaireItemComponent::getLinkId)
                    .filter(id -> !id.equals(QUESTION_10_LINK_ID))
                    .collect(Collectors.toList());
            long score = QuestionnaireScoring.sumValues(scorableAnswers, responses, this.getQuestions()).longValue();

            var q9 = Optional.ofNullable(responses.get(QUESTION_9_LINK_ID))
                    .flatMap(v -> v.get(0).getCodingValue());

            if (q9.isPresent()) {
                // Should be HIGH if scored anything other than 0 on PHQ9 Q9
                if(!q9.get().getCode().equals(LOINCAnswerCode.NOT_AT_ALL.getAnswerId())){
                    return Optional.of(new QuestionnaireScoring(score, AcuityCategory.HIGH));
                }
            }

            return Optional.of(new QuestionnaireScoring(score, calculateAcuityCategory.apply(score)));
        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            return true;
        }
    },
    GAD7("/69737-5", "GAD7-R4.json") {
        final LongFunction<AcuityCategory> calculateAcuityCategory = QuestionnaireScoring.acuityFromThresholds(13, 16);

        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            long score = QuestionnaireScoring.sumValues(this.getQuestions().stream().map(Questionnaire.QuestionnaireItemComponent::getLinkId).collect(Collectors.toList()),
                    responses,
                    this.getQuestions()).longValue();
            return Optional.of(new QuestionnaireScoring(score, calculateAcuityCategory.apply(score)));
        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            return true;
        }
    },
    ISI("/ic-isi", "ISI-R4.json") {
        final LongFunction<AcuityCategory> calculateAcuityCategory = QuestionnaireScoring.acuityFromThresholdsNoRed(13);

        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            long score = QuestionnaireScoring.sumValues(this.getQuestions().stream().map(Questionnaire.QuestionnaireItemComponent::getLinkId).collect(Collectors.toList()),
                    responses,
                    this.getQuestions()).longValue();
            return Optional.of(new QuestionnaireScoring(score, calculateAcuityCategory.apply(score)));
        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            return true;
        }
    },
    PREPTSD("/ic-pre-ptsd", "Pre-PTSD-R4.json", false) {
        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            long score = QuestionnaireScoring.countYesResponses(this.getQuestions().stream()
                    .filter(q -> responses.containsKey(q.getLinkId()))
                    .map(q -> responses.get(q.getLinkId()).get(0))
                    .collect(Collectors.toList()));
            AcuityCategory ac = score > 0 ? AcuityCategory.HIGH : AcuityCategory.LOW;
            return Optional.of(new QuestionnaireScoring(score, ac));
        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            return true;
        }
    },
    PTSD5("/ic-ptsd5", "PTSD-5-R4.json"){
        final LongFunction<AcuityCategory> calculateAcuityCategory = QuestionnaireScoring.acuityFromThresholds(2, 4);

        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            long score = QuestionnaireScoring.sumValues(this.getQuestions().stream().map(Questionnaire.QuestionnaireItemComponent::getLinkId).collect(Collectors.toList()),
                    responses,
                    this.getQuestions()).longValue();
            return Optional.of(new QuestionnaireScoring(score, calculateAcuityCategory.apply(score)));
        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            return assessment.ptsdConcern();
        }
    },
    ASRM("/ic-ASRM", "ASRM-R4.json"){
        final LongFunction<AcuityCategory> calculateAcuityCategory = QuestionnaireScoring.acuityFromThresholdsNoRed(5);

        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            long score = QuestionnaireScoring.sumValues(this.getQuestions().stream().map(Questionnaire.QuestionnaireItemComponent::getLinkId).collect(Collectors.toList()),
                    responses,
                    this.getQuestions()).longValue();
            return Optional.of(new QuestionnaireScoring(score, calculateAcuityCategory.apply(score)));

        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            return true;
        }
    },
    PRIME5("/ic-PRIME5", "PRIME-5-R4.json"){
        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            long score = QuestionnaireScoring.sumValues(this.getQuestions().stream().map(Questionnaire.QuestionnaireItemComponent::getLinkId).collect(Collectors.toList()),
                    responses,
                    this.getQuestions()).longValue();
            long maxScore = QuestionnaireScoring.maxValue(this.getQuestions().stream().map(Questionnaire.QuestionnaireItemComponent::getLinkId).collect(Collectors.toList()),
                    responses,
                    this.getQuestions()).longValue();

            AcuityCategory ac = (score >= 13 || maxScore >= 6) ? AcuityCategory.MEDIUM : AcuityCategory.LOW;
            return Optional.of(new QuestionnaireScoring(score, ac));
        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            return true;
        }
    },
    SIMPLEPAIN("/ic-simple-pain", "SimplePain-R4.json") {
        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            return Optional.empty();
        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            return true;
        }
    },
    BPI("/77564-3", "BPI-R4.json") {
        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            long score = QuestionnaireScoring.sumValues(this.getQuestions().stream().map(Questionnaire.QuestionnaireItemComponent::getLinkId).collect(Collectors.toList()),
                    responses,
                    this.getQuestions()).longValue();
            // TODO: Should this have an acuity category
            return Optional.of(new QuestionnaireScoring(score, null));

        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            return assessment.painConcern();
        }
    },
    SIMPLEDRUGALCOHOL("/ic-simple-drug-alcohol", "SimpleDrugAlcohol-R4.json") {
        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            return Optional.empty();
        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            return true;
        }
    },
    OPIOIDSCREEN("/ic-opioid-screen", "OpioidQuestion-R4.json") {
        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            long score = QuestionnaireScoring.countYesResponses(this.getQuestions().stream()
                    .filter(q -> responses.containsKey(q.getLinkId()))
                    .map(q -> responses.get(q.getLinkId()).get(0))
                    .collect(Collectors.toList()));
            // TODO: Should there be acuity here
            return Optional.of(new QuestionnaireScoring(score, null));
        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            return assessment.drugConcern();
        }
    },
    DAST10("/82666-9", "DAST-10-R4.json"){
        final LongFunction<AcuityCategory> calculateAcuityCategory = QuestionnaireScoring.acuityFromThresholds(3, 6);

        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            long score = QuestionnaireScoring.sumValues(this.getQuestions().stream().map(Questionnaire.QuestionnaireItemComponent::getLinkId).collect(Collectors.toList()),
                    responses,
                    this.getQuestions()).longValue();
            return Optional.of(new QuestionnaireScoring(score, calculateAcuityCategory.apply(score)));
        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            return assessment.drugConcern();
        }
    },
    AUDITC("/72109-2", "Audit-C-R4.json") {
        final LongFunction<AcuityCategory> calculateAcuityCategoryForWomen = QuestionnaireScoring.acuityFromThresholds(2, 4);
        final LongFunction<AcuityCategory> calculateAcuityCategoryForMen = QuestionnaireScoring.acuityFromThresholds(3, 5);

        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            long score = QuestionnaireScoring.sumValues(this.getQuestions().stream().map(Questionnaire.QuestionnaireItemComponent::getLinkId).collect(Collectors.toList()),
                    responses,
                    this.getQuestions()).longValue();
            if (patient.getPreferredGender().toLowerCase(Locale.ROOT).equals("male")) {
                return Optional.of(new QuestionnaireScoring(score, calculateAcuityCategoryForMen.apply(score)));
            }
            else return Optional.of(new QuestionnaireScoring(score, calculateAcuityCategoryForWomen.apply(score)));
        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            return assessment.alcoholConcern();
        }
    },
    // Is this a form?
    GOALS("patientGoals", false){
        @Override
        public Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient) {
            return Optional.empty();
        }

        @Override
        public boolean includeInAssessment(Assessment assessment) {
            // TODO: Figure out what to do with goals in general
            return false;
        }
    };

    private final String linkId;
    private final boolean isClinical;
    private final String fileName;
    private final Questionnaire questionnaire;

    public String getLinkId() {
        return linkId;
    }

    // I'm not sure if this should be a property, or whether the two enums should be separate
    public boolean isClinical(){
        return this.isClinical;
    }


    public static Optional<QuestionnaireType> fromLinkId(String linkId){
        return Stream.of(QuestionnaireType.values()).filter(questionnaire -> {
            return linkId.equals(questionnaire.getLinkId());
        }).findFirst();
    }

    public List<Questionnaire.QuestionnaireItemComponent> getQuestions() {
        if(this.questionnaire != null){
            return this.questionnaire.getItem()
                    .stream()
                    .flatMap(i -> i.getItem().stream())
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }


    public Optional<Questionnaire> getQuestionnaire(){
        return Optional.ofNullable(this.questionnaire);
    }

    public abstract Optional<QuestionnaireScoring> score(Map<String, List<DResponseItem>> responses, DPatient patient);

    public abstract boolean includeInAssessment(Assessment assessment);


    QuestionnaireType(String linkId, boolean isClinical) {
        this(linkId, null, isClinical);
    }

    QuestionnaireType(String linkId, String fileName) {
        this(linkId, fileName, true);
    }

    QuestionnaireType(String linkId, String fileName, boolean isClinical) {
        this.linkId = linkId;
        this.isClinical = isClinical;
        this.fileName = fileName;
        if(fileName != null){
            this.questionnaire = QuestionnaireService.getQuestionnaireByName(fileName);
        } else {
            this.questionnaire = null;
        }
    }
}


