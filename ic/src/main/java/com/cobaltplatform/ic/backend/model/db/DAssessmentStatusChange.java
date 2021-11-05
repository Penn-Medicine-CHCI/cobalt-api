package com.cobaltplatform.ic.backend.model.db;

import com.cobaltplatform.ic.model.AssessmentStatus;
import io.ebean.Model;
import io.ebean.annotation.WhenCreated;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@Entity
@Table(name = "assessment_status_change", schema = "ic")
public class DAssessmentStatusChange extends Model {
	@Id
	private UUID id;
	@ManyToOne
	private DAssessment assessment;
	@ManyToOne
	@JoinColumn(name = "account_id")
	private DCobaltAccount account;
	@ManyToOne
	@JoinColumn(name = "patient_id")
	private DPatient patient;
	@Column
	private AssessmentStatus oldStatus;
	@Column(nullable = false)
	private AssessmentStatus newStatus;
	@Column(nullable = false)
	@WhenCreated
	private Instant createdDt;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public DAssessment getAssessment() {
		return assessment;
	}

	public void setAssessment(DAssessment assessment) {
		this.assessment = assessment;
	}

	public DCobaltAccount getAccount() {
		return account;
	}

	public void setAccount(DCobaltAccount account) {
		this.account = account;
	}

	public DPatient getPatient() {
		return patient;
	}

	public void setPatient(DPatient patient) {
		this.patient = patient;
	}

	public AssessmentStatus getOldStatus() {
		return oldStatus;
	}

	public void setOldStatus(AssessmentStatus oldStatus) {
		this.oldStatus = oldStatus;
	}

	public AssessmentStatus getNewStatus() {
		return newStatus;
	}

	public void setNewStatus(AssessmentStatus newStatus) {
		this.newStatus = newStatus;
	}

	public Instant getCreatedDt() {
		return createdDt;
	}

	public void setCreatedDt(Instant createdDt) {
		this.createdDt = createdDt;
	}
}
