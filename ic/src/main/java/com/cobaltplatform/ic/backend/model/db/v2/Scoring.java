package com.cobaltplatform.ic.backend.model.db.v2;

import com.cobaltplatform.ic.model.AcuityCategory;
import com.cobaltplatform.ic.model.QuestionnaireType;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Scoring {
	@Nullable
	private UUID id;
	@Nullable
	private UUID assessmentId;
	@Nullable
	private Integer score;
	@Nullable
	private AcuityCategory acuity;
	@Nullable
	private QuestionnaireType questionnaireType;
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
	public Integer getScore() {
		return score;
	}

	public void setScore(@Nullable Integer score) {
		this.score = score;
	}

	@Nullable
	public AcuityCategory getAcuity() {
		return acuity;
	}

	public void setAcuity(@Nullable AcuityCategory acuity) {
		this.acuity = acuity;
	}

	@Nullable
	public QuestionnaireType getQuestionnaireType() {
		return questionnaireType;
	}

	public void setQuestionnaireType(@Nullable QuestionnaireType questionnaireType) {
		this.questionnaireType = questionnaireType;
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
