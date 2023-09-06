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
import com.cobaltplatform.api.model.api.response.TagApiResponse.TagApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TopicCenterRowApiResponse.TopicCenterRowApiResponseFactory;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Tag;
import com.cobaltplatform.api.model.db.TopicCenter;
import com.cobaltplatform.api.model.db.TopicCenterDisplayStyle.TopicCenterDisplayStyleId;
import com.cobaltplatform.api.model.service.TopicCenterRowDetail;
import com.cobaltplatform.api.service.TagService;
import com.cobaltplatform.api.service.TopicCenterService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Provider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class TopicCenterApiResponse {
	@Nonnull
	private final UUID topicCenterId;
	@Nonnull
	private final TopicCenterDisplayStyleId topicCenterDisplayStyleId;
	@Nonnull
	private final String name;
	@Nullable
	private final String description;
	@Nonnull
	private final String urlName;
	@Nullable
	private final String featuredTitle;
	@Nullable
	private final String featuredDescription;
	@Nullable
	private final String featuredCallToAction;
	@Nullable
	private final String imageUrl;

	@Nonnull
	private final List<TopicCenterRowApiResponse> topicCenterRows;
	@Nonnull
	private final Map<String, TagApiResponse> tagsByTagId;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface TopicCenterApiResponseFactory {
		@Nonnull
		TopicCenterApiResponse create(@Nonnull TopicCenter topicCenter);
	}

	@AssistedInject
	public TopicCenterApiResponse(@Nonnull TopicCenterService topicCenterService,
																@Nonnull TagService tagService,
																@Nonnull TopicCenterRowApiResponseFactory topicCenterRowApiResponseFactory,
																@Nonnull TagApiResponseFactory tagApiResponseFactory,
																@Nonnull Provider<CurrentContext> currentContextProvider,
																@Nonnull Formatter formatter,
																@Nonnull Strings strings,
																@Assisted @Nonnull TopicCenter topicCenter) {
		requireNonNull(topicCenterService);
		requireNonNull(tagService);
		requireNonNull(topicCenterRowApiResponseFactory);
		requireNonNull(tagApiResponseFactory);
		requireNonNull(currentContextProvider);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(topicCenter);

		InstitutionId institutionId = currentContextProvider.get().getInstitutionId();

		this.topicCenterId = topicCenter.getTopicCenterId();
		this.name = topicCenter.getName();
		this.description = topicCenter.getDescription();
		this.urlName = topicCenter.getUrlName();

		List<TopicCenterRowDetail> topicCenterRows = topicCenterService.findTopicCenterRowsByTopicCenterId(topicCenter.getTopicCenterId(), institutionId);

		this.topicCenterRows = topicCenterRows.stream()
				.map(topicCenterRowDetail -> topicCenterRowApiResponseFactory.create(topicCenterRowDetail))
				.collect(Collectors.toList());

		Map<String, TagApiResponse> tagsByTagId = new HashMap<>();

		for (Tag tag : tagService.findTagsByInstitutionId(institutionId))
			tagsByTagId.put(tag.getTagId(), tagApiResponseFactory.create(tag));

		this.tagsByTagId = tagsByTagId;
	}

	@Nonnull
	public UUID getTopicCenterId() {
		return this.topicCenterId;
	}

	@Nonnull
	public String getName() {
		return this.name;
	}

	@Nonnull
	public Optional<String> getDescription() {
		return Optional.ofNullable(this.description);
	}

	@Nonnull
	public String getUrlName() {
		return this.urlName;
	}

	@Nonnull
	public List<TopicCenterRowApiResponse> getTopicCenterRows() {
		return this.topicCenterRows;
	}

	@Nonnull
	public Map<String, TagApiResponse> getTagsByTagId() {
		return this.tagsByTagId;
	}
}