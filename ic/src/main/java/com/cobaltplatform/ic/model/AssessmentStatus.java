package com.cobaltplatform.ic.model;

import io.ebean.annotation.DbEnumValue;

public enum AssessmentStatus {
    NOT_STARTED, IN_PROGRESS, COMPLETED, STALE;

    @DbEnumValue
    public String getValue() {
        return this.name();
    }
}
