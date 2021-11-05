package com.cobaltplatform.ic.model;

public class DispositionAcuity {
    private AcuityCategory category;

    public AcuityCategory getCategory() {
        return category;
    }

    public DispositionAcuity setCategory(final AcuityCategory category) {
        this.category = category;
        return this;
    }
}
