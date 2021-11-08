package com.cobaltplatform.ic.backend.model.db;

import io.ebean.Model;
import io.ebean.annotation.SoftDelete;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "disposition_note", schema = "ic")
public class DDispositionNote extends Model {
	@Id
	private UUID dispositionNoteId;
	@Column(nullable = false, columnDefinition = "TEXT")
	private String note;
	@ManyToOne
	private DPatientDisposition disposition;
	@ManyToOne
	private DCobaltAccount cobaltAccount;
	@WhenCreated
	private Instant createdDt;
	@WhenModified
	private Instant updatedDt;
	@SoftDelete
	private boolean deleted;

	public UUID getDispositionNoteId() {
		return dispositionNoteId;
	}

	public void setDispositionNoteId(UUID dispositionNoteId) {
		this.dispositionNoteId = dispositionNoteId;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public DPatientDisposition getDisposition() {
		return disposition;
	}

	public void setDisposition(DPatientDisposition disposition) {
		this.disposition = disposition;
	}

	public DCobaltAccount getCobaltAccount() {
		return cobaltAccount;
	}

	public void setCobaltAccount(DCobaltAccount cobaltAccount) {
		this.cobaltAccount = cobaltAccount;
	}

	public Instant getCreatedDt() {
		return createdDt;
	}

	public void setCreatedDt(Instant createdDt) {
		this.createdDt = createdDt;
	}

	public Instant getUpdatedDt() {
		return updatedDt;
	}

	public void setUpdatedDt(Instant updatedDt) {
		this.updatedDt = updatedDt;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
}
