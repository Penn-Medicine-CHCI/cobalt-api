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

import com.cobaltplatform.api.model.db.assessment.Answer;
import com.cobaltplatform.api.model.db.assessment.Question;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

/**
 * @author Transmogrify LLC.
 */
public class QuestionAnswers {

	@Nonnull
	private Question question;
	@Nonnull
	private List<Answer> answers;
	@Nonnull
	private final Optional<Question> nextQuestion;
	@Nonnull
	private final Optional<Question> previousQuestion;

	public QuestionAnswers(@Nonnull Question question,
												 @Nonnull List<Answer> answers,
												 @Nonnull Optional<Question> nextQuestion,
												 @Nonnull Optional<Question> previousQuestion) {
		this.question = question;
		this.answers = answers;

		this.nextQuestion = nextQuestion;
		this.previousQuestion = previousQuestion;
	}

	@Nonnull
	public Question getQuestion() {
		return question;
	}

	public List<Answer> getAnswers() {
		return answers;
	}

	@Nonnull
	public Optional<Question> getNextQuestion() {
		return nextQuestion;
	}

	@Nonnull
	public Optional<Question> getPreviousQuestion() {
		return previousQuestion;
	}
}
