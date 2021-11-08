package com.cobaltplatform.ic.backend.model.db.v2;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Assessment {
	@Nullable
	private UUID id;
	@Nullable
	private UUID patientId;
	@Nullable
	private UUID dispositionId;
	@Nullable
	private AssessmentStatus status;
	@Nullable
	private Instant due;
	@Nullable
	private String authoredBy;
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
	public UUID getDispositionId() {
		return dispositionId;
	}

	public void setDispositionId(@Nullable UUID dispositionId) {
		this.dispositionId = dispositionId;
	}

	@Nullable
	public AssessmentStatus getStatus() {
		return status;
	}

	public void setStatus(@Nullable AssessmentStatus status) {
		this.status = status;
	}

	@Nullable
	public Instant getDue() {
		return due;
	}

	public void setDue(@Nullable Instant due) {
		this.due = due;
	}

	@Nullable
	public String getAuthoredBy() {
		return authoredBy;
	}

	public void setAuthoredBy(@Nullable String authoredBy) {
		this.authoredBy = authoredBy;
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
