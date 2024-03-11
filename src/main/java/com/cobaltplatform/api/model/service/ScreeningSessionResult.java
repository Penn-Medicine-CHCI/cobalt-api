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

package com.cobaltplatform.api.model.service;

import com.cobaltplatform.api.model.db.ScreeningAnswerContentHint.ScreeningAnswerContentHintId;
import com.cobaltplatform.api.model.db.ScreeningAnswerFormat.ScreeningAnswerFormatId;
import com.cobaltplatform.api.model.db.ScreeningType.ScreeningTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ScreeningSessionResult {
	@Nullable
	private UUID screeningFlowId;
	@Nullable
	private String screeningFlowName;
	@Nullable
	private UUID screeningFlowVersionId;
	@Nullable
	private Integer screeningFlowVersionNumber;
	@Nullable
	private List<ScreeningSessionScreeningResult> screeningSessionScreeningResults;

	@NotThreadSafe
	public static class ScreeningQuestionResult {
		@Nullable
		private UUID screeningQuestionId;
		@Nullable
		private ScreeningAnswerFormatId screeningAnswerFormatId;
		@Nullable
		private ScreeningAnswerContentHintId screeningAnswerContentHintId;
		@Nullable
		private String screeningQuestionIntroText;
		@Nullable
		private String screeningQuestionText;
		@Nullable
		private List<ScreeningAnswerResult> screeningAnswerResults;

		@Nullable
		public UUID getScreeningQuestionId() {
			return this.screeningQuestionId;
		}

		public void setScreeningQuestionId(@Nullable UUID screeningQuestionId) {
			this.screeningQuestionId = screeningQuestionId;
		}

		@Nullable
		public ScreeningAnswerFormatId getScreeningAnswerFormatId() {
			return this.screeningAnswerFormatId;
		}

		public void setScreeningAnswerFormatId(@Nullable ScreeningAnswerFormatId screeningAnswerFormatId) {
			this.screeningAnswerFormatId = screeningAnswerFormatId;
		}

		@Nullable
		public ScreeningAnswerContentHintId getScreeningAnswerContentHintId() {
			return this.screeningAnswerContentHintId;
		}

		public void setScreeningAnswerContentHintId(@Nullable ScreeningAnswerContentHintId screeningAnswerContentHintId) {
			this.screeningAnswerContentHintId = screeningAnswerContentHintId;
		}

		@Nullable
		public String getScreeningQuestionIntroText() {
			return this.screeningQuestionIntroText;
		}

		public void setScreeningQuestionIntroText(@Nullable String screeningQuestionIntroText) {
			this.screeningQuestionIntroText = screeningQuestionIntroText;
		}

		@Nullable
		public String getScreeningQuestionText() {
			return this.screeningQuestionText;
		}

		public void setScreeningQuestionText(@Nullable String screeningQuestionText) {
			this.screeningQuestionText = screeningQuestionText;
		}

		@Nullable
		public List<ScreeningAnswerResult> getScreeningAnswerResults() {
			return this.screeningAnswerResults;
		}

		public void setScreeningAnswerResults(@Nullable List<ScreeningAnswerResult> screeningAnswerResults) {
			this.screeningAnswerResults = screeningAnswerResults;
		}
	}

	@NotThreadSafe
	public static class ScreeningAnswerResult {
		@Nullable
		private UUID screeningAnswerId;
		@Nullable
		private UUID screeningAnswerOptionId;
		@Nullable
		private String answerOptionText;
		@Nullable
		private String text;
		@Nullable
		private Integer score;

		@Nullable
		public UUID getScreeningAnswerId() {
			return this.screeningAnswerId;
		}

		public void setScreeningAnswerId(@Nullable UUID screeningAnswerId) {
			this.screeningAnswerId = screeningAnswerId;
		}

		@Nullable
		public UUID getScreeningAnswerOptionId() {
			return this.screeningAnswerOptionId;
		}

		public void setScreeningAnswerOptionId(@Nullable UUID screeningAnswerOptionId) {
			this.screeningAnswerOptionId = screeningAnswerOptionId;
		}

		@Nullable
		public String getAnswerOptionText() {
			return this.answerOptionText;
		}

		public void setAnswerOptionText(@Nullable String answerOptionText) {
			this.answerOptionText = answerOptionText;
		}

		@Nullable
		public String getText() {
			return this.text;
		}

		public void setText(@Nullable String text) {
			this.text = text;
		}

		@Nullable
		public Integer getScore() {
			return this.score;
		}

		public void setScore(@Nullable Integer score) {
			this.score = score;
		}
	}

	@NotThreadSafe
	public static class ScreeningSessionScreeningResult {
		@Nullable
		private UUID screeningVersionId;
		@Nullable
		private UUID screeningId;
		@Nullable
		private Integer screeningVersionNumber;
		@Nullable
		private ScreeningTypeId screeningTypeId;
		@Nullable
		private String screeningName;
		@Nullable
		private ScreeningScore screeningScore;
		@Nullable
		private Boolean belowScoringThreshold;
		@Nullable
		private List<ScreeningQuestionResult> screeningQuestionResults;

		@Nullable
		public UUID getScreeningVersionId() {
			return this.screeningVersionId;
		}

		public void setScreeningVersionId(@Nullable UUID screeningVersionId) {
			this.screeningVersionId = screeningVersionId;
		}

		@Nullable
		public UUID getScreeningId() {
			return this.screeningId;
		}

		public void setScreeningId(@Nullable UUID screeningId) {
			this.screeningId = screeningId;
		}

		@Nullable
		public Integer getScreeningVersionNumber() {
			return this.screeningVersionNumber;
		}

		public void setScreeningVersionNumber(@Nullable Integer screeningVersionNumber) {
			this.screeningVersionNumber = screeningVersionNumber;
		}

		@Nullable
		public ScreeningTypeId getScreeningTypeId() {
			return this.screeningTypeId;
		}

		public void setScreeningTypeId(@Nullable ScreeningTypeId screeningTypeId) {
			this.screeningTypeId = screeningTypeId;
		}

		@Nullable
		public String getScreeningName() {
			return this.screeningName;
		}

		public void setScreeningName(@Nullable String screeningName) {
			this.screeningName = screeningName;
		}

		@Nullable
		public ScreeningScore getScreeningScore() {
			return this.screeningScore;
		}

		public void setScreeningScore(@Nullable ScreeningScore screeningScore) {
			this.screeningScore = screeningScore;
		}

		@Nullable
		public Boolean getBelowScoringThreshold() {
			return this.belowScoringThreshold;
		}

		public void setBelowScoringThreshold(@Nullable Boolean belowScoringThreshold) {
			this.belowScoringThreshold = belowScoringThreshold;
		}

		@Nullable
		public List<ScreeningQuestionResult> getScreeningQuestionResults() {
			return this.screeningQuestionResults;
		}

		public void setScreeningQuestionResults(@Nullable List<ScreeningQuestionResult> screeningQuestionResults) {
			this.screeningQuestionResults = screeningQuestionResults;
		}
	}

	@Nullable
	public UUID getScreeningFlowId() {
		return this.screeningFlowId;
	}

	public void setScreeningFlowId(@Nullable UUID screeningFlowId) {
		this.screeningFlowId = screeningFlowId;
	}

	@Nullable
	public String getScreeningFlowName() {
		return this.screeningFlowName;
	}

	public void setScreeningFlowName(@Nullable String screeningFlowName) {
		this.screeningFlowName = screeningFlowName;
	}

	@Nullable
	public UUID getScreeningFlowVersionId() {
		return this.screeningFlowVersionId;
	}

	public void setScreeningFlowVersionId(@Nullable UUID screeningFlowVersionId) {
		this.screeningFlowVersionId = screeningFlowVersionId;
	}

	@Nullable
	public Integer getScreeningFlowVersionNumber() {
		return this.screeningFlowVersionNumber;
	}

	public void setScreeningFlowVersionNumber(@Nullable Integer screeningFlowVersionNumber) {
		this.screeningFlowVersionNumber = screeningFlowVersionNumber;
	}

	@Nullable
	public List<ScreeningSessionScreeningResult> getScreeningSessionScreeningResults() {
		return this.screeningSessionScreeningResults;
	}

	public void setScreeningSessionScreeningResults(@Nullable List<ScreeningSessionScreeningResult> screeningSessionScreeningResults) {
		this.screeningSessionScreeningResults = screeningSessionScreeningResults;
	}
}
