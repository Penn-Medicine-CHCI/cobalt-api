/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Originally created at the University of Pennsylvania and Penn Medicine by:
 * Dr. David Asch; Dr. Lisa Bellini; Dr. Cecilia Livesey; Kelley Kugler; and Dr. Matthew Press.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cobaltplatform.api.model.db;

import com.cobaltplatform.api.model.db.AssessmentType.AssessmentTypeId;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
public class Assessment {
	@Nonnull
	private UUID assessmentId;
	@Nonnull
	private AssessmentTypeId assessmentTypeId;
	@Nonnull
	private String baseQuestion;
	@Nullable
	private UUID nextAssessmentId;
	@Nullable
	private Integer minimumEligibilityScore;
	@Nullable
	private Boolean answersMayContainPii;

	@Nonnull
	public UUID getAssessmentId() {
		return assessmentId;
	}

	public void setAssessmentId(@Nonnull UUID assessmentId) {
		this.assessmentId = assessmentId;
	}

	@Nonnull
	public AssessmentTypeId getAssessmentTypeId() {
		return assessmentTypeId;
	}

	public void setAssessmentTypeId(@Nonnull AssessmentTypeId assessmentTypeId) {
		this.assessmentTypeId = assessmentTypeId;
	}

	@Nonnull
	public String getBaseQuestion() {
		return baseQuestion;
	}

	public void setBaseQuestion(@Nonnull String baseQuestion) {
		this.baseQuestion = baseQuestion;
	}

	@Nullable
	public UUID getNextAssessmentId() {
		return nextAssessmentId;
	}

	public void setNextAssessmentId(@Nullable UUID nextAssessmentId) {
		this.nextAssessmentId = nextAssessmentId;
	}

	@Nullable
	public Integer getMinimumEligibilityScore() {
		return minimumEligibilityScore;
	}

	public void setMinimumEligibilityScore(@Nullable Integer minimumEligibilityScore) {
		this.minimumEligibilityScore = minimumEligibilityScore;
	}

	@Nullable
	public Boolean getAnswersMayContainPii() {
		return answersMayContainPii;
	}

	public void setAnswersMayContainPii(@Nullable Boolean answersMayContainPii) {
		this.answersMayContainPii = answersMayContainPii;
	}
}
