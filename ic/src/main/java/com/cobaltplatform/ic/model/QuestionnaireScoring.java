package com.cobaltplatform.ic.model;

import com.cobaltplatform.ic.backend.model.db.DResponseItem;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Questionnaire;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongFunction;

public class QuestionnaireScoring {
    private final long score;
    private final AcuityCategory acuityCategory;

    public QuestionnaireScoring(long score, AcuityCategory acuityCategory){
        this.score = score;
        this.acuityCategory = acuityCategory;
    }

    public static long countYesResponses(List<DResponseItem> answers){
        if (answers.isEmpty()) {
            return 0;
        }
        return answers.stream().filter(answer ->
                answer.getCodingValue().get().getCode().equals(LOINCAnswerCode.YES.getAnswerId())).count();
    }

    public static BigDecimal sumValues(List<String> linkIds, Map<String, List<DResponseItem>> responses, List<Questionnaire.QuestionnaireItemComponent> questions) {
        return questions.stream().filter(q -> linkIds.contains(q.getLinkId()) && responses.containsKey(q.getLinkId())).map(q -> {
            DResponseItem response = responses.get(q.getLinkId()).get(0);
            Optional<Questionnaire.QuestionnaireItemAnswerOptionComponent> value = q.getAnswerOption().stream()
                    .filter(a -> a.getValueCoding().getCode().equals(response.getCodingValue().orElseThrow().getCode())).findFirst();
            return ((DecimalType) (value.orElseThrow().getExtensionByUrl("http://hl7.org/fhir/StructureDefinition/ordinalValue").getValue())).getValue();
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Returns the maximum Value from a list of answers (or zero)
     * @param answers
     * @return
     */
    public static BigDecimal maxValue(List<String> linkIds, Map<String, List<DResponseItem>> responses, List<Questionnaire.QuestionnaireItemComponent> questions) {
        // Might need to actually look for the correct extension in the list rather than get(0)
        return questions.stream().filter(q -> linkIds.contains(q.getLinkId()) && responses.containsKey(q.getLinkId())).map(q -> {
            DResponseItem response = responses.get(q.getLinkId()).get(0);
            Optional<Questionnaire.QuestionnaireItemAnswerOptionComponent> value = q.getAnswerOption().stream()
                    .filter(a -> a.getValueCoding().getCode().equals(response.getCodingValue().orElseThrow().getCode())).findFirst();
            return ((DecimalType) (value.orElseThrow().getExtensionByUrl("http://hl7.org/fhir/StructureDefinition/ordinalValue").getValue())).getValue();
        }).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    }

    public long getScore() {
        return score;
    }

    public static LongFunction<AcuityCategory> acuityFromThresholds(long maxLow, long maxMedium){
        return score -> {
            if (score <= maxLow) {
                return AcuityCategory.LOW;
            } else if (score <= maxMedium) {
                return AcuityCategory.MEDIUM;
            } else {
                return AcuityCategory.HIGH;
            }
        };
    }

    public static LongFunction<AcuityCategory> acuityFromThresholdsNoRed(long maxGreen){
        return score -> {
            if (score <= maxGreen) {
                return AcuityCategory.LOW;
            } else {
                return AcuityCategory.MEDIUM;
            }
        };
    }

    public AcuityCategory getAcuityCategory() {
        return acuityCategory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuestionnaireScoring that = (QuestionnaireScoring) o;
        return score == that.score && acuityCategory == that.acuityCategory;
    }

    @Override
    public int hashCode() {
        return Objects.hash(score, acuityCategory);
    }

    @Override
    public String toString() {
        return "QuestionnaireScoring{" +
                "score=" + score +
                ", acuityCategory=" + acuityCategory +
                '}';
    }
}
