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

package com.cobaltplatform.api.model.db.assessment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
public class AccountSessionAnswer {

	@Nonnull
	private UUID accountSessionAnswerId;
	@Nonnull
	private UUID accountSessionId;
	@Nonnull
	private UUID answerId;
	@Nonnull
	private UUID questionId;
	@Nullable
	private String answerText;

	@Nonnull
	public UUID getAccountSessionAnswerId() {
		return accountSessionAnswerId;
	}

	public void setAccountSessionAnswerId(@Nonnull UUID accountSessionAnswerId) {
		this.accountSessionAnswerId = accountSessionAnswerId;
	}

	@Nonnull
	public UUID getAccountSessionId() {
		return accountSessionId;
	}

	public void setAccountSessionId(@Nonnull UUID accountSessionId) {
		this.accountSessionId = accountSessionId;
	}

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

	@Nullable
	public String getAnswerText() {
		return answerText;
	}

	public void setAnswerText(@Nullable String answerText) {
		this.answerText = answerText;
	}

}
