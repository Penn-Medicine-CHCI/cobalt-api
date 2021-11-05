package com.cobaltplatform.ic.backend.model.db.v2;

import com.cobaltplatform.ic.model.AcuityCategory;
import com.cobaltplatform.ic.model.DispositionFlag;
import com.cobaltplatform.ic.model.DispositionOutcomeDiagnosis;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientDisposition {
	@Nullable
	private UUID id;
	@Nullable
	private UUID patientId;
	@Nullable
	@Deprecated
	private String notes; // JSONB, not used
	@Nullable
	private DispositionFlag flag;
	@Nullable
	private Boolean isDigital;
	@Nullable
	private AcuityCategory acuityCategory;
	@Nullable
	private DispositionOutcomeDiagnosis diagnosis;
	@Nullable
	private Boolean crisis;
	@Nullable
	private Boolean crisisAcknowledged;
	@Nullable
	private Instant createdDt;
	@Nullable
	private Instant updatedDt;
	@Nullable
	private Boolean deleted;

	@Nullable
	public UUID getId() {
		return id;
	}

	public void setId(@Nullable UUID id) {
		this.id = id;
	}

	@Nullable
	public UUID getPatientId() {
		return patientId;
	}

	public void setPatientId(@Nullable UUID patientId) {
		this.patientId = patientId;
	}

	@Nullable
	public String getNotes() {
		return notes;
	}

	public void setNotes(@Nullable String notes) {
		this.notes = notes;
	}

	@Nullable
	public DispositionFlag getFlag() {
		return flag;
	}

	public void setFlag(@Nullable DispositionFlag flag) {
		this.flag = flag;
	}

	@Nullable
	public Boolean getIsDigital() {
		return isDigital;
	}

	public void setIsDigital(@Nullable Boolean isDigital) {
		this.isDigital = isDigital;
	}

	@Nullable
	public AcuityCategory getAcuityCategory() {
		return acuityCategory;
	}

	public void setAcuityCategory(@Nullable AcuityCategory acuityCategory) {
		this.acuityCategory = acuityCategory;
	}

	@Nullable
	public DispositionOutcomeDiagnosis getDiagnosis() {
		return diagnosis;
	}

	public void setDiagnosis(@Nullable DispositionOutcomeDiagnosis diagnosis) {
		this.diagnosis = diagnosis;
	}

	@Nullable
	public Boolean getCrisis() {
		return crisis;
	}

	public void setCrisis(@Nullable Boolean crisis) {
		this.crisis = crisis;
	}

	@Nullable
	public Boolean getCrisisAcknowledged() {
		return crisisAcknowledged;
	}

	public void setCrisisAcknowledged(@Nullable Boolean crisisAcknowledged) {
		this.crisisAcknowledged = crisisAcknowledged;
	}

	@Nullable
	public Instant getCreatedDt() {
		return createdDt;
	}

	public void setCreatedDt(@Nullable Instant createdDt) {
		this.createdDt = createdDt;
	}

	@Nullable
	public Instant getUpdatedDt() {
		return updatedDt;
	}

	public void setUpdatedDt(@Nullable Instant updatedDt) {
		this.updatedDt = updatedDt;
	}

	@Nullable
	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(@Nullable Boolean deleted) {
		this.deleted = deleted;
	}
}