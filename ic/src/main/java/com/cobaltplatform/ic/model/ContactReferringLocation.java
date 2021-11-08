package com.cobaltplatform.ic.model;

import io.ebean.annotation.DbEnumValue;

import java.util.Arrays;
import java.util.NoSuchElementException;

public enum ContactReferringLocation {
    REFERRING_LOCATION_0(0),
    REFERRING_LOCATION_1(1),
    REFERRING_LOCATION_2(2),
    REFERRING_LOCATION_3(3),
    REFERRING_LOCATION_4(4),
    REFERRING_LOCATION_5(5),
    REFERRING_LOCATION_6(6),
    REFERRING_LOCATION_7(7),
    REFERRING_LOCATION_8(8);

    private long id;

    ContactReferringLocation(final long code) {
        this.id = code;
    }

    @DbEnumValue
    public long getId() {
        return id;
    }

    public void setId(long value) {
        this.id = value;
    }

    public static ContactReferringLocation valueOf(long value) throws NoSuchElementException {
        return Arrays
            .stream(ContactReferringLocation.values())
            .filter(v -> v.getId() == value)
            .findFirst()
            .orElseThrow();
    }
}
