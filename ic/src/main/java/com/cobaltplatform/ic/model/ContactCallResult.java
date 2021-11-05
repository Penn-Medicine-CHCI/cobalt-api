package com.cobaltplatform.ic.model;

import io.ebean.annotation.DbEnumValue;

import java.util.Arrays;
import java.util.NoSuchElementException;

public enum ContactCallResult {
    LEFT_VOICE_MAIL(0),
    LEFT_MESSAGE(1),
    NO_ANSWER(2),
    BUSY(3),
    DISCONNECTED_WRONG_NUMBER(4),
    DISCUSSED_APPOINTMENT_TIME(5),
    DISCUSSED_DIGITAL_SCREENING_REMINDER(6),
    SENT_EMAIL(7),
    SENT_TEXT_MESSAGE(8),
    SENT_LETTER(9);

    private long id;

    ContactCallResult(final long code) {
        this.id = code;
    }

    @DbEnumValue
    public long getId() {
        return id;
    }

    public void setId(long value) {
        this.id = value;
    }

    public static ContactCallResult valueOf(long value) throws NoSuchElementException {
        return Arrays
            .stream(ContactCallResult.values())
            .filter(v -> v.getId() == value)
            .findFirst()
            .orElseThrow();
    }
}

