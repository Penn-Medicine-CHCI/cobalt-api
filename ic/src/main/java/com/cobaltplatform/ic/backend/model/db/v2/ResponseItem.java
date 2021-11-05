package com.cobaltplatform.ic.backend.model.db.v2;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ResponseItem {
	@Nullable
	private UUID id;
	@Nullable
	private UUID assessmentId;
	@Nullable
	private String linkId;
	@Nullable
	private String stringValue;
	@Nullable
	private Boolean booleanValue;
	@Nullable
	private String codeSystem;
	@Nullable
	private String codeValue;
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
	public UUID getAssessmentId() {
		return assessmentId;
	}

	public void setAssessmentId(@Nullable UUID assessmentId) {
		this.assessmentId = assessmentId;
	}

	@Nullable
	public String getLinkId() {
		return linkId;
	}

	public void setLinkId(@Nullable String linkId) {
		this.linkId = linkId;
	}

	@Nullable
	public String getStringValue() {
		return stringValue;
	}

	public void setStringValue(@Nullable String stringValue) {
		this.stringValue = stringValue;
	}

	@Nullable
	public Boolean getBooleanValue() {
		return booleanValue;
	}

	public void setBooleanValue(@Nullable Boolean booleanValue) {
		this.booleanValue = booleanValue;
	}

	@Nullable
	public String getCodeSystem() {
		return codeSystem;
	}

	public void setCodeSystem(@Nullable String codeSystem) {
		this.codeSystem = codeSystem;
	}

	@Nullable
	public String getCodeValue() {
		return codeValue;
	}

	public void setCodeValue(@Nullable String codeValue) {
		this.codeValue = codeValue;
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
