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

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
public class Answer {

	@Nonnull
	private UUID answerId;
	@Nonnull
	private UUID questionId;
	@Nonnull
	private String answerText;
	@Nonnull
	private Integer answerValue;
	@Nonnull
	private Integer displayOrder;
	@Nonnull
	private Boolean crisis;
	@Nonnull
	private Boolean call;
	@Nonnull
	private UUID nextQuestionId;

	@Nonnull
	public UUID getAnswerId() {
		return answerId;
	}

	public void setAnswerId(@Nonnull UUID answerId) {
		this.answerId = answerId;
	}

	@Nonnull
	public UUID getQuestionId() {
		return questionId;
	}

	public void setQuestionId(@Nonnull UUID questionId) {
		this.questionId = questionId;
	}

	@Nonnull
	public String getAnswerText() {
		return answerText;
	}

	public void setAnswerText(@Nonnull String answerText) {
		this.answerText = answerText;
	}

	@Nonnull
	public Integer getAnswerValue() {
		return answerValue;
	}

	public void setAnswerValue(@Nonnull Integer answerValue) {
		this.answerValue = answerValue;
	}

	@Nonnull
	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(@Nonnull Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	@Nonnull
	public Boolean getCrisis() {
		return crisis;
	}

	public void setCrisis(@Nonnull Boolean crisis) {
		this.crisis = crisis;
	}

	@Nonnull
	public Boolean getCall() {
		return call;
	}

	public void setCall(@Nonnull Boolean call) {
		this.call = call;
	}

	@Nonnull
	public UUID getNextQuestionId() {
		return nextQuestionId;
	}

	public void setNextQuestionId(@Nonnull UUID nextQuestionId) {
		this.nextQuestionId = nextQuestionId;
	}
}
