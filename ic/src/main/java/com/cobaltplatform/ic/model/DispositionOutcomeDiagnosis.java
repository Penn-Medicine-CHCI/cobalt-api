package com.cobaltplatform.ic.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.cobaltplatform.ic.backend.model.db.DResponseItem;
import io.ebean.annotation.DbEnumValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.stream.Stream;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum DispositionOutcomeDiagnosis {
    CRISIS_CARE(100, "Crisis Care", DispositionOutcomeCare.IC, DispositionFlag.NEEDS_INITIAL_SAFETY_PLANNING) {
        public boolean hasOutcome(Assessment assessment) {
            return assessment.isCrisis();
        }
    },
    LCSW_CAPACITY(0, "LCSW Capacity", DispositionOutcomeCare.SPECIALTY, DispositionFlag.COORDINATE_REFERRAL) {
        // TODO confirm this flag
        public boolean hasOutcome(Assessment assessment) {
            return false;
        }
    },
    EATING_DISORDER(6, "Eating Disorder", DispositionOutcomeCare.SPECIALTY, DispositionFlag.COORDINATE_REFERRAL) {
        // TODO confirm this flag
        public boolean hasOutcome(Assessment assessment) {
            return assessment.getSingleResponse("/diagnosis/ED")
                    .flatMap(DResponseItem::getBooleanValue).orElse(false);
        }
    },
    TRAUMA(4, "Trauma", DispositionOutcomeCare.SPECIALTY, DispositionFlag.COORDINATE_REFERRAL) {
        public boolean hasOutcome(Assessment assessment) {
            boolean selectedTrauma = assessment.getSingleResponse("/diagnosis/PTSD")
                    .flatMap(DResponseItem::getBooleanValue).orElse(false);

            boolean isHighAcuity = assessment.getAcuity(QuestionnaireType.PTSD5)
                    .map(i -> i.equals(AcuityCategory.HIGH)).orElse(false);
            return selectedTrauma || isHighAcuity;
        }
    },
    ADHD(3, "ADHD", DispositionOutcomeCare.SPECIALTY, DispositionFlag.COORDINATE_REFERRAL) {
        // TODO confirm this flag
        public boolean hasOutcome(Assessment assessment) {
            return assessment.getSingleResponse("/diagnosis/ADHD")
                    .flatMap(DResponseItem::getBooleanValue).orElse(false);
        }
    },
    EVALUATION(1, "Evaluation", DispositionOutcomeCare.SPECIALTY, DispositionFlag.NEEDS_FURTHER_ASSESSMENT_WITH_MHIC) {
        public boolean hasOutcome(Assessment assessment) {
            if (assessment.getAcuity(QuestionnaireType.ASRM).map(a -> a.equals(AcuityCategory.MEDIUM)).orElse(false)) {
                return true;
            }
            if(assessment.getAcuity(QuestionnaireType.PTSD5)
                    .map(i -> i.equals(AcuityCategory.MEDIUM)).orElse(false)){
                return true;
            }

            return assessment.getAcuity(QuestionnaireType.PRIME5).map(a -> a.equals(AcuityCategory.MEDIUM)).orElse(false);
        }
    },
    OPIOID_USE_DISORDER(11, "Opioid Use Disorder", DispositionOutcomeCare.IC, DispositionFlag.COORDINATE_REFERRAL) {
        public boolean hasOutcome(Assessment assessment) {
            return assessment.getScore(QuestionnaireType.OPIOIDSCREEN)
                    .filter(s -> s >= 1L)
                    .flatMap((s) -> assessment.getAcuity(QuestionnaireType.DAST10))
                    .map(a -> a.equals(AcuityCategory.HIGH))
                    .orElse(false);
        }
    },
    SUD(5, "SUD", DispositionOutcomeCare.SPECIALTY, DispositionFlag.COORDINATE_REFERRAL) {
        public boolean hasOutcome(Assessment assessment) {
            boolean selectedSubstance = assessment.getSingleResponse("/diagnosis/substance")
                    .flatMap(DResponseItem::getBooleanValue).orElse(false);

            boolean isHighAcuity = assessment.getAcuity(QuestionnaireType.DAST10).map(a -> a.equals(AcuityCategory.HIGH)).orElse(false);
            return selectedSubstance || isHighAcuity;
        }
    },
    ALCOHOL_USE_DISORDER(7, "Alcohol Use Disorder", DispositionOutcomeCare.SPECIALTY, DispositionFlag.COORDINATE_REFERRAL) {
        public boolean hasOutcome(Assessment assessment) {
            return assessment.getScore(QuestionnaireType.AUDITC).map(s -> s >= 6L).orElse(false);
        }
    },
    PSYCHOTHERAPY_ANDOR_MM(2, "Psychotherapy and/or MM", DispositionOutcomeCare.SPECIALTY, DispositionFlag.COORDINATE_REFERRAL) {
        public boolean hasOutcome(Assessment assessment) {
            boolean selectedSchizophrenia = assessment.getSingleResponse("/diagnosis/schizophrenia")
                    .flatMap(DResponseItem::getBooleanValue).orElse(false);
            boolean selectedBipolar = assessment.getSingleResponse("/diagnosis/bipolar")
                    .flatMap(DResponseItem::getBooleanValue).orElse(false);

            if (selectedSchizophrenia || selectedBipolar) return true;
            Optional<AcuityCategory> phq9Acuity = assessment.getAcuity(QuestionnaireType.PHQ9);
            Optional<AcuityCategory> gad7Acuity = assessment.getAcuity(QuestionnaireType.GAD7);

            boolean phq9flag = phq9Acuity.isPresent() && phq9Acuity.get() != AcuityCategory.LOW;
            boolean gad7flag = gad7Acuity.isPresent() && gad7Acuity.get() != AcuityCategory.LOW;
            return phq9flag || gad7flag;
        }
    },
    INSOMNIA(9, "Insomnia", DispositionOutcomeCare.IC, DispositionFlag.AWAITING_IC_SCHEDULING) {
        public boolean hasOutcome(Assessment assessment) {
            var acuityISI = assessment.getAcuity(QuestionnaireType.ISI);
            return DispositionOutcomeDiagnosis.GENERAL.hasOutcome(assessment) && acuityISI.map(a -> a.equals(AcuityCategory.MEDIUM)).orElse(false);
        }
    },
    GRIEF(10, "Grief", DispositionOutcomeCare.IC, DispositionFlag.AWAITING_IC_SCHEDULING) {
        public boolean hasOutcome(Assessment assessment) {
            var hasGrief = assessment.getSingleResponse("/symptom/grief")
                    .flatMap(DResponseItem::getBooleanValue).orElse(false);
            return DispositionOutcomeDiagnosis.GENERAL.hasOutcome(assessment) && hasGrief;
        }
    },
    GENERAL(8, "General", DispositionOutcomeCare.IC, DispositionFlag.AWAITING_IC_SCHEDULING) {
        public boolean hasOutcome(Assessment assessment) {
            var elevatedPHQ9 = assessment.getScore(QuestionnaireType.PHQ9).map(i -> i >= 5L).orElse(false);
            var elevatedGAD7 = assessment.getScore(QuestionnaireType.GAD7).map(i -> i >= 5L).orElse(false);
            return elevatedGAD7 || elevatedPHQ9;
        }
    },
    SELF_DIRECTED(-1, "Sub-clinical symptoms", DispositionOutcomeCare.SUB_CLINICAL, DispositionFlag.OPTIONAL_REFERRAL) {
        public boolean hasOutcome(Assessment assessment) {
            return true;
        }
    };

    private final String name;
    private final long code;
    @JsonIgnore
    private final DispositionOutcomeCare care;
    @JsonIgnore
    private final DispositionFlag flag;

    DispositionOutcomeDiagnosis(final long code, final String name, DispositionOutcomeCare care, DispositionFlag flag) {
        this.code = code;
        this.name = name;
        this.care = care;
        this.flag = flag;
    }

    @DbEnumValue
    public long getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public DispositionOutcomeCare getCare() {
        return care;
    }

    public static Optional<DispositionOutcomeDiagnosis> fromCode(long code){
        return Stream.of(DispositionOutcomeDiagnosis.values())
                .filter(d -> code == d.code)
        .findFirst();
    }

    @JsonCreator
    public static DispositionOutcomeDiagnosis fromNode(JsonNode node) {
        if (!node.has("code"))
            return null;

        long code = node.get("code").asLong();
        // Throwing here is fine -- it means bad data
        return fromCode(code).orElseThrow();
    }

    public abstract boolean hasOutcome(Assessment assessment);

    public DispositionFlag getFlag() {
        return flag;
    }

    private static final Logger logger = LoggerFactory.getLogger(
        DispositionOutcomeDiagnosis.class);
}
