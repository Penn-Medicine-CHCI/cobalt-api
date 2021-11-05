package com.cobaltplatform.ic.model;

public class DispositionOutcome {
    private DispositionOutcomeCare care;
    private DispositionOutcomeDiagnosis diagnosis;
    private boolean crisis;

    public DispositionOutcomeCare getCare() {
        return care;
    }

    public DispositionOutcome setCare(final DispositionOutcomeCare care) {
        this.care = care;
        return this;
    }

    public DispositionOutcomeDiagnosis getDiagnosis() {
        return diagnosis;
    }

    public DispositionOutcome setDiagnosis(final DispositionOutcomeDiagnosis diagnosis) {
        this.diagnosis = diagnosis;
        return this;
    }

    public boolean isCrisis() {
        return crisis;
    }

    public DispositionOutcome setCrisis(final boolean crisis) {
        this.crisis = crisis;
        return this;
    }
}


