package com.cobaltplatform.ic.model;

import io.ebean.annotation.DbEnumValue;

public enum AcuityCategory {
    LOW, MEDIUM, HIGH;

    @DbEnumValue
    public String getValue() {
        return this.name();
    }
}
