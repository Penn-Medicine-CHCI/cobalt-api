package com.cobaltplatform.ic.model;

import com.cobaltplatform.ic.backend.model.db.DPatient;
import com.cobaltplatform.ic.backend.model.db.DResponseItem;
import com.cobaltplatform.ic.backend.model.db.DScoring;

import java.util.*;
import java.util.stream.Collectors;

public class Assessment {
    // TODO: Use something a bit smarter than the raw database type
    private final DPatient patient;
    private final Map<String, List<DResponseItem>> responses;

    private Map<QuestionnaireType, QuestionnaireScoring> scores;

    // Note: this is only used by test code
    @Deprecated
    public Assessment(DPatient patient, Map<String, List<DResponseItem>> responses, List<DScoring> scores) {
        this.patient = patient;
        this.responses = responses;
        this.scores = new EnumMap<>(QuestionnaireType.class);
        scores.forEach(s -> this.scores.put(s.getQuestionnaireType(), new QuestionnaireScoring(s.getScore(), s.getAcuity())));
    }

    public Assessment(DPatient patient, Map<String, List<DResponseItem>> responses) {
        this.patient = patient;
        this.responses = responses;
        this.scores = null;
    }

    public Map<QuestionnaireType, QuestionnaireScoring> getScores(){
        if(this.scores != null){
            return this.scores;
        }
        List<QuestionnaireType> scorable = Arrays.stream(QuestionnaireType.values())
                .filter(qt -> (qt.isClinical() && qt.includeInAssessment(this)) || qt == QuestionnaireType.CSSRS)
                .collect(Collectors.toList());

        this.scores = scorable.stream().map(s -> Map.entry(s, s.score(this.responses, this.patient)))
                .filter(v -> v.getValue().isPresent())
                .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().get(), (l, r) -> {
                            throw new IllegalArgumentException("Duplicate keys " + l + "and " + r + ".");
                        },
                        () -> new EnumMap<>(QuestionnaireType.class)));
        return this.scores;
    }

    private static final String PHQ9_QUESTION_9_LINK_ID = "/44260-8";

    public boolean isCrisis(){
        boolean cssrsCrisis = getSingleResponse("/93246-7")
                    .flatMap(DResponseItem::getCodingValue)
                    .map(i -> i.getCode().equals(LOINCAnswerCode.YES.getAnswerId()))
                     .orElse(false)
                ||
                getSingleResponse("/93247-5")
                        .flatMap(DResponseItem::getCodingValue)
                        .map(i -> i.getCode().equals(LOINCAnswerCode.YES.getAnswerId()))
                        .orElse(false)
                ||
                (getSingleResponse("/93267-3")
                        .flatMap(DResponseItem::getCodingValue)
                        .map(i -> i.getCode().equals(LOINCAnswerCode.YES.getAnswerId()))
                        .orElse(false) && getSingleResponse("/93269-9")
                                .flatMap(DResponseItem::getCodingValue)
                                .map(i -> i.getCode().equals(LOINCAnswerCode.YES.getAnswerId()))
                                .orElse(false));

        boolean phq9crisis = getSingleResponse(PHQ9_QUESTION_9_LINK_ID)
                .flatMap(DResponseItem::getCodingValue)
                .map(q9 -> !q9.getCode().equals(LOINCAnswerCode.NOT_AT_ALL.getAnswerId()))
                .orElse(false);

        return cssrsCrisis || phq9crisis;
    }

    public Optional<AcuityCategory> getOverallAcuity() {
        return this.getScores().entrySet().stream()
            .filter(q -> q.getKey().isClinical())
            .map(Map.Entry::getValue)
            .map(QuestionnaireScoring::getAcuityCategory)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder());
    }

    public Optional<DispositionOutcomeDiagnosis> getPreselectedDiagnosis(){
        Set<String> selectedDiagnoses = this.responses.keySet().stream().filter(
                k -> k.startsWith("/diagnosis") &&
                this.getSingleResponse(k).flatMap(d -> d.getBooleanValue().or(() -> d.getStringValue().map(s -> s.length() > 0)))
                        .orElse(false))
                .collect(Collectors.toSet());
        if(selectedDiagnoses.isEmpty()){
            return Optional.empty();
        } else if(selectedDiagnoses.size() > 1){
            return Optional.of(DispositionOutcomeDiagnosis.PSYCHOTHERAPY_ANDOR_MM);
        } else if(selectedDiagnoses.contains("/diagnosis/ED")){
            return Optional.of(DispositionOutcomeDiagnosis.EATING_DISORDER);
        } else if(selectedDiagnoses.contains("/diagnosis/ADHD")){
            return Optional.of(DispositionOutcomeDiagnosis.ADHD);
        } else if(selectedDiagnoses.contains("/diagnosis/substance")){
            return Optional.of(DispositionOutcomeDiagnosis.SUD);
        } else if(selectedDiagnoses.contains("/diagnosis/schizophrenia")){
            return Optional.of(DispositionOutcomeDiagnosis.PSYCHOTHERAPY_ANDOR_MM);
        } else if(selectedDiagnoses.contains("/diagnosis/bipolar")){
            return Optional.of(DispositionOutcomeDiagnosis.PSYCHOTHERAPY_ANDOR_MM);
        } else if(selectedDiagnoses.contains("/diagnosis/PTSD")){
            return Optional.of(DispositionOutcomeDiagnosis.TRAUMA);
        } else if(selectedDiagnoses.contains("/diagnosis/other")){
            // TODO: Find out if we need to process the plain-text entry to find disorders
            return Optional.of(DispositionOutcomeDiagnosis.EVALUATION);
        } else {
            // Insomnia, Depression, Anxiety
            return Optional.empty();
        }
    }
//
    public Optional<AcuityCategory> getAcuity(QuestionnaireType questionnaireType) {
        return Optional.ofNullable(this.getScores().get(questionnaireType)).map(QuestionnaireScoring::getAcuityCategory);
    }

    public Optional<Long> getScore(QuestionnaireType questionnaireType) {
        return Optional.ofNullable(this.getScores().get(questionnaireType)).map(QuestionnaireScoring::getScore);
    }

    public Optional<DispositionOutcomeDiagnosis> getDiagnosis() {
        return this.getPreselectedDiagnosis().or(() -> Arrays.stream(DispositionOutcomeDiagnosis.values())
                .filter(dg -> dg.hasOutcome(this))
                .findFirst());
    }

    // There may be a better way to model these

    private List<DResponseItem> getResponses(String linkId){
        return responses.getOrDefault(linkId, Collections.emptyList());
    }

    public Optional<DResponseItem> getSingleResponse(String linkId){
        List<DResponseItem> responseList = getResponses(linkId);
        if(responseList.size() != 1){
            return Optional.empty();
        }
        return Optional.of(responseList.get(0));
    }

    public boolean ptsdConcern(){
        return getSingleResponse("/pre-ptsd-1")
                .flatMap(DResponseItem::getCodingValue)
                .map(i -> i.getCode().equals(LOINCAnswerCode.YES.getAnswerId())).orElse(false);
    }

    public boolean drugConcern(){
        return getSingleResponse("/single-q-drug-screen")
                .flatMap(DResponseItem::getCodingValue)
                .map(i -> !i.getCode().equals(LOINCAnswerCode.PC_0.getAnswerId())).orElse(false);
    }

    public boolean alcoholConcern(){
        return getSingleResponse("/68518-0")
                .flatMap(DResponseItem::getCodingValue)
                .map(i -> !i.getCode().equals(LOINCAnswerCode.NEVER.getAnswerId())).orElse(false);
    }

    public boolean painConcern(){
        return getSingleResponse("/ic-simplePain-1")
                .flatMap(DResponseItem::getCodingValue)
                .map(i -> i.getCode().equals(LOINCAnswerCode.YES.getAnswerId())).orElse(false);
    }
}
