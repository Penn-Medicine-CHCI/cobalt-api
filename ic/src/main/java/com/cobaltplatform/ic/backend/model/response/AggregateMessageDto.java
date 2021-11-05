package com.cobaltplatform.ic.backend.model.response;

import org.joda.time.DateTime;

import java.util.UUID;

public class AggregateMessageDto {
	private UUID patientId;

	private short maxAttempts;

	private DateTime latestDt;

	private String preferredPhoneNumber;

	private UUID dispositionId;

	public UUID getDispositionId() {
		return dispositionId;
	}

	public void setDispositionId(UUID dispositionId) {
		this.dispositionId = dispositionId;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	private String fullName;

	public String getPreferredPhoneNumber() {
		return preferredPhoneNumber;
	}

	public UUID getPatientId() {
		return patientId;
	}

	public short getMaxAttempts() {
		return maxAttempts;
	}

	public DateTime getLatestDt() {
		return latestDt;
	}

	public AggregateMessageDto(UUID patientId, String preferredPhoneNumber, String fullName, short maxAttempts, DateTime latestDt, UUID dispositionId) {
		this.patientId = patientId;
		this.maxAttempts = maxAttempts;
		this.latestDt = latestDt;
		this.preferredPhoneNumber = preferredPhoneNumber;
		this.fullName = fullName;
		this.dispositionId = dispositionId;
	}
}
