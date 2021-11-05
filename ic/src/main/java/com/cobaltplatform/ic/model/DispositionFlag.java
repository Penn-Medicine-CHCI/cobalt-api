package com.cobaltplatform.ic.model;

import io.ebean.annotation.DbEnumType;
import io.ebean.annotation.DbEnumValue;

import java.util.Arrays;
import java.util.NoSuchElementException;

public enum DispositionFlag {
    START_PHONE_SCREENING(DispositionFlagType.GENERAL, "Start phone screening", 0),
    REMIND_ABOUT_DIGITAL_SCREENING(DispositionFlagType.GENERAL, "Remind about digital screening", 1),
    FINAL_OUTREACH_ATTEMPT(DispositionFlagType.GENERAL, "Final outreach attempt", 2),
    SCHEDULE_WITH_IC(DispositionFlagType.GENERAL, "Schedule with IC", 3),
    NEEDS_FURTHER_ASSESSMENT_WITH_MHIC(DispositionFlagType.GENERAL, "Needs further assessment with MHIC", 4),
    COORDINATE_REFERRAL(DispositionFlagType.GENERAL, "Coordinate Referral", 5),
    CONFIRM_REFERRAL_CONNECTION(DispositionFlagType.GENERAL, "Confirm referral connection", 6),
    NEEDS_INITIAL_SAFETY_PLANNING(DispositionFlagType.SAFETY, "Needs initial safety planning", 7),
    NEEDS_SAFETY_PLANNING_FOLLOW(DispositionFlagType.SAFETY, "Needs safety planning follow-up", 8),

    // STATUSES
    NOT_YET_SCREENED(DispositionFlagType.NONE, "Not yet screened", 11),
    AWAITING_IC_SCHEDULING(DispositionFlagType.NONE, "Awaiting IC scheduling", 12),
    AWAITING_FIRST_IC_APPOINTMENT(DispositionFlagType.NONE, "Awaiting first IC appointment", 13),
    AWAITING_FIRST_EXTERNAL_APPOINTMENT(DispositionFlagType.NONE, "Awaiting first external appointment", 14),
    IN_IC_TREATMENT(DispositionFlagType.NONE, "In IC treatment", 15),
    GRADUATED(DispositionFlagType.NONE, "Graduated", 16),
    CONNECTED_TO_CARE(DispositionFlagType.NONE, "Connected to care", 17),
    LOST_CONTACT_WITH_PATIENT(DispositionFlagType.NONE, "Lost contact with patient", 18),
    OPTIONAL_REFERRAL(DispositionFlagType.NONE, "Optional Referral", 19),
    ASSESSMENT_STARTED_NO_DISPOSITION_YET(DispositionFlagType.NONE, "The assessment has begun but there is no disposition yet", 20),
    PATIENT_CREATED_NOT_YET_LOGGED_IN(DispositionFlagType.NONE, "The patient has been created via order report but has not logged in yet", 21);

    private final DispositionFlagType type;
    private final String label;
    private final Integer id;

    DispositionFlag(DispositionFlagType type, String label, Integer id){
        this.type = type;
        this.label = label;
        this.id = id;
    }

    public static DispositionFlag valueOf(int value) throws NoSuchElementException {
        return Arrays
            .stream(DispositionFlag.values())
            .filter(v -> v.getId() == value)
            .findFirst()
            .orElseThrow();
    }

    public DispositionFlagType getType() { return type; }

    public String getLabel() { return label; }

    @DbEnumValue(storage = DbEnumType.INTEGER)
    public Integer getId() { return id; }
}
