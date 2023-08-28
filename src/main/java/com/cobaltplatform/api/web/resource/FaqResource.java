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

package com.cobaltplatform.api.web.resource;

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.response.FaqApiResponse;
import com.cobaltplatform.api.model.api.response.FaqApiResponse.FaqApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FaqTopicApiResponse.FaqTopicApiResponseFactory;
import com.cobaltplatform.api.model.db.Faq;
import com.cobaltplatform.api.model.db.FaqTopic;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.FaqService;
import com.cobaltplatform.api.service.InstitutionService;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class FaqResource {
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final FaqService faqService;
	@Nonnull
	private final FaqApiResponseFactory faqApiResponseFactory;
	@Nonnull
	private final FaqTopicApiResponseFactory faqTopicApiResponseFactory;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public FaqResource(@Nonnull InstitutionService institutionService,
										 @Nonnull FaqService faqService,
										 @Nonnull FaqApiResponseFactory faqApiResponseFactory,
										 @Nonnull FaqTopicApiResponseFactory faqTopicApiResponseFactory,
										 @Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(institutionService);
		requireNonNull(faqService);
		requireNonNull(faqApiResponseFactory);
		requireNonNull(faqTopicApiResponseFactory);
		requireNonNull(currentContextProvider);

		this.institutionService = institutionService;
		this.faqService = faqService;
		this.faqApiResponseFactory = faqApiResponseFactory;
		this.faqTopicApiResponseFactory = faqTopicApiResponseFactory;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/faq-topics")
	@AuthenticationRequired
	public ApiResponse faqTopics() {
		List<FaqTopic> faqTopics = getFaqService().findFaqTopicsByInstitutionId(getCurrentContext().getInstitutionId());
		List<Faq> faqs = getFaqService().findFaqsByInstitutionId(getCurrentContext().getInstitutionId());
		Map<UUID, List<FaqApiResponse>> faqsByFaqTopicId = faqs.stream()
				.collect(Collectors.groupingBy(
								Faq::getFaqTopicId, Collectors.mapping(
										faq -> getFaqApiResponseFactory().create(faq),
										Collectors.toList()
								)
						)
				);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("faqTopics", faqTopics.stream()
					.map(faqTopic -> getFaqTopicApiResponseFactory().create(faqTopic))
					.collect(Collectors.toList()));
			put("faqsByFaqTopicId", faqsByFaqTopicId);
		}});
	}

	@Nonnull
	@GET("/faqs/{faqId}")
	@AuthenticationRequired
	public ApiResponse faq(@Nonnull @PathParameter("faqId") String faqIdentifier) {
		requireNonNull(faqIdentifier);

		Faq faq = getFaqService().findFaqByIdentifier(faqIdentifier, getCurrentContext().getInstitutionId()).orElse(null);

		if (faq == null)
			throw new NotFoundException();

		if (!faq.getInstitutionId().equals(getCurrentContext().getInstitutionId()))
			throw new AuthorizationException();

		FaqTopic faqTopic = getFaqService().findFaqTopicById(faq.getFaqTopicId()).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("faq", getFaqApiResponseFactory().create(faq));
			put("faqTopic", getFaqTopicApiResponseFactory().create(faqTopic));
		}});
	}

	@Nonnull
	@GET("/faqs")
	@AuthenticationRequired
	public ApiResponse faqs(@Nonnull @QueryParameter("faqTopicId") String faqTopicIdentifier) {
		requireNonNull(faqTopicIdentifier);

		FaqTopic faqTopic = getFaqService().findFaqTopicByIdentifier(faqTopicIdentifier, getCurrentContext().getInstitutionId()).orElse(null);

		if (faqTopic == null)
			throw new NotFoundException();

		if (!faqTopic.getInstitutionId().equals(getCurrentContext().getInstitutionId()))
			throw new AuthorizationException();

		List<Faq> faqs = getFaqService().findFaqsByFaqTopicId(faqTopic.getFaqTopicId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("faqs", faqs.stream()
					.map(faq -> getFaqApiResponseFactory().create(faq))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionService;
	}

	@Nonnull
	protected FaqService getFaqService() {
		return this.faqService;
	}

	@Nonnull
	protected FaqApiResponseFactory getFaqApiResponseFactory() {
		return this.faqApiResponseFactory;
	}

	@Nonnull
	protected FaqTopicApiResponseFactory getFaqTopicApiResponseFactory() {
		return this.faqTopicApiResponseFactory;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
