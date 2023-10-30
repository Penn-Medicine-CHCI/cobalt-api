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
import com.cobaltplatform.api.model.api.response.ContentApiResponse.ContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionApiResponse.GroupSessionApiResponseFactory;
import com.cobaltplatform.api.model.db.TopicCenterRowTagType.TopicCenterRowTagTypeId;
import com.cobaltplatform.api.service.TopicCenterService.TopicCenterRowTagDetail;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class TopicCenterRowTagApiResponse {
	@Nonnull
	private final String tagId;
	@Nonnull
	private final TopicCenterRowTagTypeId topicCenterRowTagTypeId;
	@Nonnull
	private final String title;
	@Nullable
	private final String description;
	@Nullable
	private final String cta;
	@Nullable
	private final String ctaUrl;
	@Nullable
	private final List<ContentApiResponse> contents;
	@Nullable
	private final List<GroupSessionApiResponse> groupSessions;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface TopicCenterRowTagApiResponseFactory {
		@Nonnull
		TopicCenterRowTagApiResponse create(@Nonnull TopicCenterRowTagDetail topicCenterRowTag);
	}

	@AssistedInject
	public TopicCenterRowTagApiResponse(@Nonnull Provider<CurrentContext> currentContextProvider,
																			@Nonnull ContentApiResponseFactory contentApiResponseFactory,
																			@Nonnull GroupSessionApiResponseFactory groupSessionApiResponseFactory,
																			@Assisted @Nonnull TopicCenterRowTagDetail topicCenterRowTag) {
		requireNonNull(currentContextProvider);
		requireNonNull(contentApiResponseFactory);
		requireNonNull(groupSessionApiResponseFactory);
		requireNonNull(topicCenterRowTag);

		this.tagId = topicCenterRowTag.getTagId();
		this.topicCenterRowTagTypeId = topicCenterRowTag.getTopicCenterRowTagTypeId();
		this.title = topicCenterRowTag.getTitle();
		this.description = topicCenterRowTag.getDescription();
		this.cta = topicCenterRowTag.getCta();
		this.ctaUrl = topicCenterRowTag.getCtaUrl();
		this.contents = topicCenterRowTag.getContents() == null ? null : topicCenterRowTag.getContents().stream()
				.map(content -> contentApiResponseFactory.create(content))
				.collect(Collectors.toList());
		this.groupSessions = topicCenterRowTag.getGroupSessions() == null ? null : topicCenterRowTag.getGroupSessions().stream()
				.map(groupSession -> groupSessionApiResponseFactory.create(groupSession))
				.collect(Collectors.toList());
	}

	@Nonnull
	public String getTagId() {
		return this.tagId;
	}

	@Nonnull
	public TopicCenterRowTagTypeId getTopicCenterRowTagTypeId() {
		return this.topicCenterRowTagTypeId;
	}

	@Nonnull
	public String getTitle() {
		return this.title;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	@Nullable
	public String getCta() {
		return this.cta;
	}

	@Nullable
	public String getCtaUrl() {
		return this.ctaUrl;
	}

	@Nullable
	public List<ContentApiResponse> getContents() {
		return this.contents;
	}

	@Nullable
	public List<GroupSessionApiResponse> getGroupSessions() {
		return this.groupSessions;
	}
}