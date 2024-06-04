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

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.response.FaqSubtopicApiResponse.FaqSubtopicApiResponseFactory;
import com.cobaltplatform.api.model.db.Faq;
import com.cobaltplatform.api.service.FaqService;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class FaqApiResponse {
	@Nonnull
	private final UUID faqId;
	@Nonnull
	private final UUID faqTopicId;
	@Nonnull
	private final String urlName;
	@Nonnull
	private final String question;
	@Nonnull
	private final String answer;
	@Nullable
	private final String shortAnswer;
	@Nonnull
	private final Boolean permitEllipsizing;
	@Nonnull
	private final List<FaqSubtopicApiResponse> faqSubtopics;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface FaqApiResponseFactory {
		@Nonnull
		FaqApiResponse create(@Nonnull Faq faq);
	}

	@AssistedInject
	public FaqApiResponse(@Nonnull Provider<CurrentContext> currentContextProvider,
												@Nonnull FaqSubtopicApiResponseFactory faqSubtopicApiResponseFactory,
												@Nonnull FaqService faqService,
												@Assisted @Nonnull Faq faq) {
		requireNonNull(currentContextProvider);
		requireNonNull(faqSubtopicApiResponseFactory);
		requireNonNull(faqService);
		requireNonNull(faq);

		this.faqId = faq.getFaqId();
		this.faqTopicId = faq.getFaqTopicId();
		this.urlName = faq.getUrlName();
		this.question = faq.getQuestion();
		this.answer = faq.getAnswer();
		this.shortAnswer = faq.getShortAnswer();
		this.permitEllipsizing = faq.getPermitEllipsizing();
		this.faqSubtopics = faqService.findFaqSubtopicsByFaqId(faq.getFaqId()).stream()
				.map(faqSubtopic -> faqSubtopicApiResponseFactory.create(faqSubtopic))
				.collect(Collectors.toList());
	}

	@Nonnull
	public UUID getFaqId() {
		return this.faqId;
	}

	@Nonnull
	public UUID getFaqTopicId() {
		return this.faqTopicId;
	}

	@Nonnull
	public String getUrlName() {
		return this.urlName;
	}

	@Nonnull
	public String getQuestion() {
		return this.question;
	}

	@Nonnull
	public String getAnswer() {
		return this.answer;
	}

	@Nonnull
	public Optional<String> getShortAnswer() {
		return Optional.ofNullable(this.shortAnswer);
	}

	@Nonnull
	public Boolean getPermitEllipsizing() {
		return this.permitEllipsizing;
	}

	@Nonnull
	public List<FaqSubtopicApiResponse> getFaqSubtopics() {
		return this.faqSubtopics;
	}
}