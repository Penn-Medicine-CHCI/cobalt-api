package com.cobaltplatform.ic.backend.service;

import com.cobaltplatform.ic.backend.exception.ValidationException;
import com.cobaltplatform.ic.backend.exception.ValidationException.FieldError;
import com.cobaltplatform.ic.backend.model.db.DCobaltAccount;
import com.cobaltplatform.ic.backend.model.db.DDispositionNote;
import com.cobaltplatform.ic.backend.model.db.DPatient;
import com.cobaltplatform.ic.backend.model.db.DPatientDisposition;
import com.cobaltplatform.ic.backend.model.db.DSpecialtyCareScheduling;
import com.cobaltplatform.ic.backend.model.db.query.QDDispositionNote;
import com.cobaltplatform.ic.backend.model.db.query.QDPatientDisposition;
import com.cobaltplatform.ic.backend.model.db.query.assoc.QAssocDPatient;
import com.cobaltplatform.ic.backend.model.request.DispositionNoteCreateRequest;
import com.cobaltplatform.ic.model.DispositionFlag;
import com.cobaltplatform.ic.model.DispositionOutcomeDiagnosis;
import com.cobaltplatform.ic.model.SpecialtyCareScheduling;
import io.ebean.DB;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

public class DispositionService {
    public static List<DPatientDisposition> getDispositionsForAllPatients() {
        String sql = "SELECT \"disp\".id from patient, LATERAL (SELECT * FROM ic.patient_disposition " +
                "where ic.patient_disposition.patient_id = patient.id and flag not in (:graduatedStatus, :connectedToCareStatus, :lostContactWithPatientStatus) order by created_dt desc LIMIT 1) as \"disp\"";

        // This is suboptimal -- we should be able to query this all as one query, but ebean is misbehaving with the mapping and , but this will at the cost of one query mean we don't need to maintain the mapping manually here
        List<UUID> ids = DB.sqlQuery(sql)
                .setParameter("graduatedStatus", DispositionFlag.GRADUATED.getId())
                .setParameter("connectedToCareStatus", DispositionFlag.CONNECTED_TO_CARE.getId())
                .setParameter("lostContactWithPatientStatus", DispositionFlag.LOST_CONTACT_WITH_PATIENT.getId())
                .mapToScalar(UUID.class)
                .findList();

        return new QDPatientDisposition()
                .fetch("patient")
                .fetch("triageReview")
                .fetch("specialtyCareScheduling")
                .id.in(ids).findList();
    }

    public static Optional<DPatientDisposition> getLatestDispositionForPatient(UUID patientId) {
        return getLatestDisposition((patient) -> patient.id.eq(patientId));
    }

    public static Optional<DPatientDisposition> getLatestDispositionForCobaltAccountId(UUID cobaltAccountId) {
        return getLatestDisposition((patient) -> patient.cobaltAccountId.eq(cobaltAccountId));
    }

    private static Optional<DPatientDisposition> getLatestDisposition(Function<QAssocDPatient<QDPatientDisposition>, QDPatientDisposition> whereClauseApplier) {
        QAssocDPatient<QDPatientDisposition> patient = new QDPatientDisposition().patient;
        QDPatientDisposition disposition = whereClauseApplier.apply(patient);

        return disposition
                .fetch("orders")
            .flag.notIn(DispositionFlag.GRADUATED, DispositionFlag.CONNECTED_TO_CARE, DispositionFlag.LOST_CONTACT_WITH_PATIENT)
            .orderBy().createdDt.desc()
            .setMaxRows(1)
            .findOneOrEmpty();
    }

    public static DPatientDisposition createDisposition(DPatient patient){
        DPatientDisposition disposition = new DPatientDisposition()
                .setPatient(patient)
                .setDigital(true)
                .setFlag(DispositionFlag.NOT_YET_SCREENED);
        disposition.save();
        return disposition;
    }

    public static DPatientDisposition createDisposition(DPatient patient, boolean isDigital){
        DPatientDisposition disposition = new DPatientDisposition()
            .setPatient(patient)
            .setDigital(isDigital)
            .setFlag(DispositionFlag.NOT_YET_SCREENED);
        disposition.save();
        return disposition;
    }

    /**
     * Updates the status of a disposition
     * @param dispositionId The ID of the disposition to update
     * @throws NoSuchElementException if there is no disposition with the requested ID
     */
    public static DPatientDisposition updateDispositionFlag(UUID dispositionId, DispositionFlag dispositionFlag) throws NoSuchElementException {
        DPatientDisposition disposition = new QDPatientDisposition()
            .id.equalTo(dispositionId)
            .findOneOrEmpty().orElseThrow();
        disposition
            .setFlag(dispositionFlag)
            .save();
        return disposition;
    }

    public static DPatientDisposition updateDispositionOutcome(UUID dispositionId, DispositionOutcomeDiagnosis dispositionOutcome) throws NoSuchElementException {
        DPatientDisposition disposition = new QDPatientDisposition()
                .id.equalTo(dispositionId)
                .findOneOrEmpty().orElseThrow();

        disposition
                .setDiagnosis(dispositionOutcome)
                .setCrisis(dispositionOutcome == DispositionOutcomeDiagnosis.CRISIS_CARE)
                .setFlag(dispositionOutcome.getFlag());
        disposition.save();
        return disposition;
    }

    public static DPatientDisposition updateDispositionCrisisAcknowledged(UUID dispositionId, boolean crisisAcknowledged) {
        DPatientDisposition disposition = new QDPatientDisposition()
            .id.equalTo(dispositionId)
            .findOneOrEmpty().orElseThrow();
        disposition
            .setCrisisAcknowledged(crisisAcknowledged)
            .save();
        return disposition;
    }

    public static Optional<DPatientDisposition> getDisposition(UUID dispositionId) {
        return new QDPatientDisposition()
                .id.equalTo(dispositionId)
                .findOneOrEmpty();
    }

    public static Optional<DDispositionNote> getDispositionNote(UUID dispositionNoteId) {
        return new QDDispositionNote()
            .dispositionNoteId.equalTo(dispositionNoteId)
            .findOneOrEmpty();
    }

    @Nonnull
    public static DDispositionNote createDispositionNote(@Nonnull DispositionNoteCreateRequest request) {
        requireNonNull(request);

        DCobaltAccount cobaltAccount = CobaltAccountService.getSharedInstance().findCobaltAccountById(request.getAccountId()).orElse(null);
        DPatientDisposition disposition = getDisposition(request.getDispositionId()).orElse(null);
        String note = trimToNull(request.getNote());
        Instant now = Instant.now();

        ValidationException validationException = new ValidationException();

        if(cobaltAccount == null)
            validationException.add(new FieldError("account", "Account is required."));

        if(disposition == null)
            validationException.add(new FieldError("disposition", "Disposition is required."));

        if(note == null)
            validationException.add(new FieldError("note", "Note is required."));

        if(validationException.hasErrors())
            throw validationException;

        DDispositionNote dispositionNote = new DDispositionNote();
        dispositionNote.setCobaltAccount(cobaltAccount);
        dispositionNote.setDisposition(disposition);
        dispositionNote.setNote(note);
        dispositionNote.setCreatedDt(now);
        dispositionNote.setUpdatedDt(now);
        dispositionNote.setDeleted(false);

        dispositionNote.save();

        return dispositionNote;
    }

    public static boolean deleteDispositionNote(@Nullable UUID dispositionNoteId) {
        if(dispositionNoteId == null)
            return false;

        DDispositionNote dispositionNote = getDispositionNote(dispositionNoteId).orElse(null);

        if(dispositionNote == null)
            return false;

        dispositionNote.setDeleted(true);
        dispositionNote.save();

        return true;
    }

    public static DPatientDisposition setSpecialtyCareScheduling(UUID dispositionId, SpecialtyCareScheduling specialtyCareScheduling) {
        DPatientDisposition disposition = new QDPatientDisposition()
            .id.equalTo(dispositionId)
            .fetch("specialtyCareScheduling")
            .findOneOrEmpty().orElseThrow();

        DSpecialtyCareScheduling existingSpecialtyCareScheduling = disposition.getSpecialtyCareScheduling();
        DSpecialtyCareScheduling specialtyCareSchedulingToSave = null;
        DSpecialtyCareScheduling specialtyCareSchedulingToDelete = null;

        if(specialtyCareScheduling == null) {
            specialtyCareSchedulingToDelete = disposition.getSpecialtyCareScheduling();
        } else {
            String agency = trimToNull(specialtyCareScheduling.getAgency());
            boolean attendanceConfirmed = specialtyCareScheduling.isAttendanceConfirmed();
            LocalDate date = specialtyCareScheduling.getDate();
            LocalTime time = specialtyCareScheduling.getTime();
            String notes = trimToNull(specialtyCareScheduling.getNotes());

            ValidationException validationException = new ValidationException();

            if(agency == null)
                validationException.add(new FieldError("agency", "Agency is required."));

            if(date == null)
                validationException.add(new FieldError("date", "Date is required."));

            if(attendanceConfirmed && disposition.getSpecialtyCareScheduling() == null)
                validationException.add(new FieldError("attendanceConfirmed", "You cannot set 'attendance confirmed' when creating a new specialty care scheduling."));

            if(validationException.hasErrors())
                throw validationException;

            specialtyCareSchedulingToSave = existingSpecialtyCareScheduling == null ? new DSpecialtyCareScheduling() : existingSpecialtyCareScheduling;
            specialtyCareSchedulingToSave.setAgency(agency);
            specialtyCareSchedulingToSave.setAttendanceConfirmed(attendanceConfirmed);
            specialtyCareSchedulingToSave.setDate(date);
            specialtyCareSchedulingToSave.setTime(time);
            specialtyCareSchedulingToSave.setNotes(notes);
            specialtyCareSchedulingToSave.setDisposition(disposition);
        }

        // If creating specialty care scheduling for the first time, transition to CONFIRM_REFERRAL_CONNECTION
        // Otherwise, if we are flagged CONFIRM_REFERRAL_CONNECTION and the specialty care is updated such
        //   that "attendance confirmed" checkbox is checked, transition to CONNECTED_TO_CARE
        // Otherwise, if we are flagged CONNECTED_TO_CARE and the specialty care is updated such
        //   that "attendance confirmed" checkbox is unchecked, transition to back COORDINATE_REFERRAL to "reset"
        // Otherwise, if we are deleting an existing specialty care scheduling, transition back to COORDINATE_REFERRAL to "reset"
        if(existingSpecialtyCareScheduling == null) {
            // In theory we should also check if flag == DispositionFlag.COORDINATE_REFERRAL, but let MHIC use their own judgment to kick off the flow
            DispositionService.updateDispositionFlag(disposition.getId(), DispositionFlag.CONFIRM_REFERRAL_CONNECTION);
        } else if(disposition.getFlag() == DispositionFlag.CONFIRM_REFERRAL_CONNECTION
            && specialtyCareScheduling != null
            && specialtyCareScheduling.isAttendanceConfirmed()) {
            DispositionService.updateDispositionFlag(disposition.getId(), DispositionFlag.CONNECTED_TO_CARE);
        } else if(disposition.getFlag() == DispositionFlag.CONNECTED_TO_CARE
            && specialtyCareScheduling != null
            && !specialtyCareScheduling.isAttendanceConfirmed()) {
            DispositionService.updateDispositionFlag(disposition.getId(), DispositionFlag.COORDINATE_REFERRAL);
        } else if(specialtyCareSchedulingToDelete != null) {
            DispositionService.updateDispositionFlag(disposition.getId(), DispositionFlag.COORDINATE_REFERRAL);
        }

        if(specialtyCareSchedulingToDelete != null)
            specialtyCareSchedulingToDelete.delete();

        if(specialtyCareSchedulingToSave != null)
            specialtyCareSchedulingToSave.save();

        return new QDPatientDisposition().id.equalTo(dispositionId).findOne();
    }
}