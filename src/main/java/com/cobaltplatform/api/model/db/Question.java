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

import com.cobaltplatform.api.model.db.FontSize.FontSizeId;
import com.cobaltplatform.api.model.db.QuestionType.QuestionTypeId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
public class Question {

	@Nonnull
	private UUID questionId;
	@Nonnull
	private UUID assessmentId;
	@Nonnull
	private QuestionTypeId questionTypeId;
	@Nonnull
	private FontSizeId fontSizeId;
	@Nonnull
	private String questionText;
	@Nullable
	private String cmsQuestionText;
	@Nonnull
	private Integer answerColumnCount;
	@Nonnull
	private Integer displayOrder;
	@Nonnull
	private Boolean isRootQuestion;
	@Nonnull
	private Boolean answerRequired;

	@Nonnull
	public UUID getQuestionId() {
		return questionId;
	}

	public void setQuestionId(@Nonnull UUID questionId) {
		this.questionId = questionId;
	}

	@Nonnull
	public UUID getAssessmentId() {
		return assessmentId;
	}

	public void setAssessmentId(@Nonnull UUID assessmentId) {
		this.assessmentId = assessmentId;
	}

	@Nonnull
	public QuestionTypeId getQuestionTypeId() {
		return questionTypeId;
	}

	public void setQuestionTypeId(@Nonnull QuestionTypeId questionTypeId) {
		this.questionTypeId = questionTypeId;
	}

	@Nonnull
	public FontSizeId getFontSizeId() {
		return fontSizeId;
	}

	public void setFontSizeId(@Nonnull FontSizeId fontSizeId) {
		this.fontSizeId = fontSizeId;
	}

	@Nonnull
	public Boolean getRootQuestion() {
		return isRootQuestion;
	}

	public void setRootQuestion(@Nonnull Boolean rootQuestion) {
		isRootQuestion = rootQuestion;
	}

	@Nonnull
	public String getQuestionText() {
		return questionText;
	}

	public void setQuestionText(@Nonnull String questionText) {
		this.questionText = questionText;
	}

	@Nullable
	public String getCmsQuestionText() {
		return cmsQuestionText;
	}

	public void setCmsQuestionText(@Nullable String cmsQuestionText) {
		this.cmsQuestionText = cmsQuestionText;
	}

	@Nonnull
	public Integer getAnswerColumnCount() {
		return answerColumnCount;
	}

	public void setAnswerColumnCount(@Nonnull Integer answerColumnCount) {
		this.answerColumnCount = answerColumnCount;
	}

	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	@Nonnull
	public Boolean getIsRootQuestion() {
		return isRootQuestion;
	}

	public void setIsRootQuestion(@Nonnull Boolean rootQuestion) {
		isRootQuestion = rootQuestion;
	}

	@Nonnull
	public Boolean getAnswerRequired() {
		return answerRequired;
	}

	public void setAnswerRequired(@Nonnull Boolean answerRequired) {
		this.answerRequired = answerRequired;
	}
}
