package com.cobaltplatform.ic.backend.model.db;

import com.cobaltplatform.ic.model.DispositionFlag;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "messages", schema = "ic")
public class DPatientMessage extends DBaseModel {
	private UUID patientId;

	private short attempt;

	private String body;

	private String address;

	private DispositionFlag hadFlag;

	private UUID dispositionId;

	public boolean isSucceeded() {
		return succeeded;
	}

	public DPatientMessage setSucceeded(boolean succeeded) {
		this.succeeded = succeeded;
		return this;
	}

	public boolean succeeded;

	public UUID getPatientId() {
		return patientId;
	}

	public DPatientMessage setPatientId(UUID patientId) {
		this.patientId = patientId;
		return this;
	}

	public UUID getDispositionId() {
		return dispositionId;
	}

	public DPatientMessage setDispositionId(UUID dispositionId) {
		this.dispositionId = dispositionId;
		return this;
	}

	public short getAttempt() {
		return attempt;
	}

	public DPatientMessage setAttempt(short attempt) {
		this.attempt = attempt;
		return this;
	}

	public String getBody() {
		return body;
	}

	public DPatientMessage setBody(String body) {
		this.body = body;
		return this;
	}

	public String getAddress() {
		return address;
	}

	public DPatientMessage setAddress(String address) {
		this.address = address;
		return this;
	}

	public DispositionFlag getHadFlag() {
		return hadFlag;
	}

	public DPatientMessage setHadFlag(DispositionFlag hadFlag) {
		this.hadFlag = hadFlag;
		return this;
	}
}
