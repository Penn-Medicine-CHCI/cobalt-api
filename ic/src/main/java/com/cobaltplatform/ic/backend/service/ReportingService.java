package com.cobaltplatform.ic.backend.service;

import com.cobaltplatform.ic.backend.model.db.v2.Assessment;
import com.cobaltplatform.ic.backend.model.db.v2.Patient;
import com.cobaltplatform.ic.backend.model.db.v2.PatientDisposition;
import com.cobaltplatform.ic.backend.model.db.v2.ReferralOrderReport;
import com.pyranid.Database;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ReportingService {
	@Nonnull
	private final Database database;

	public ReportingService(@Nonnull Database database) {
		requireNonNull(database);
		this.database = database;
	}

	@Nonnull
	public List<Patient> findPatients() {
		return getDatabase().queryForList("SELECT * FROM patient ORDER BY created_dt", Patient.class);
	}

	@Nonnull
	public List<ReferralOrderReport> findReferralOrderReports() {
		return getDatabase().queryForList("SELECT * FROM referral_order_report ORDER BY order_id", ReferralOrderReport.class);
	}

	@Nonnull
	public List<ReferralOrderReport> findReferralOrderReportsByUid(@Nullable String uid) {
		uid = trimToNull(uid);

		if (uid == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM referral_order_report WHERE TRIM(uid)=? ORDER BY order_id", ReferralOrderReport.class, uid);
	}

	@Nonnull
	public List<Assessment> findAssessmentsByPatientId(@Nullable UUID patientId) {
		if (patientId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM assessment WHERE patient_id=? ORDER BY created_dt", Assessment.class, patientId);
	}

	@Nonnull
	public Optional<Patient> findPatientById(@Nullable UUID patientId) {
		if (patientId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM patient WHERE id=?", Patient.class, patientId);
	}

	@Nonnull
	public List<PatientDisposition> findPatientDispositions() {
		return getDatabase().queryForList("SELECT * FROM patient_disposition ORDER BY created_dt", PatientDisposition.class);
	}

	@Nonnull
	public List<PatientDisposition> findPatientDispositionsByPatientId(@Nullable UUID patientId) {
		if (patientId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM patient_disposition WHERE patient_id=? ORDER BY created_dt", PatientDisposition.class, patientId);
	}

	@Nonnull
	public Optional<PatientDisposition> findPatientDispositionById(@Nullable UUID patientDispositionId) {
		if (patientDispositionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM patient_disposition WHERE id=?", PatientDisposition.class, patientDispositionId);
	}

	@Nonnull
	public Optional<PatientDisposition> findPatientDispositionByAssessmentId(@Nullable UUID assessmentId) {
		if (assessmentId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT pd.* FROM patient_disposition pd, assessment a " +
				"WHERE a.id=? AND a.disposition_id=pd.id", PatientDisposition.class, assessmentId);
	}

	@Nonnull
	protected Database getDatabase() {
		return database;
	}
}
