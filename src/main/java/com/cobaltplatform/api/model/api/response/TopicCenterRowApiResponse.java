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

import com.cobaltplatform.api.model.api.response.ContentApiResponse.ContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionApiResponse.GroupSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionRequestApiResponse.GroupSessionRequestApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PinboardNoteApiResponse.PinboardNoteApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TopicCenterRowTagApiResponse.TopicCenterRowTagApiResponseFactory;
import com.cobaltplatform.api.model.service.TopicCenterRowDetail;
import com.cobaltplatform.api.service.TopicCenterService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class TopicCenterRowApiResponse {
	@Nonnull
	private final UUID topicCenterRowId;
	@Nonnull
	private final String title;
	@Nullable
	private final String description;
	@Nullable
	private final String groupSessionsTitle;
	@Nullable
	private final String groupSessionsDescription;
	@Nullable
	private final String groupSessionRequestsTitle;
	@Nullable
	private final String groupSessionRequestsDescription;
	@Nonnull
	private final List<GroupSessionApiResponse> groupSessions;
	@Nonnull
	private final List<GroupSessionRequestApiResponse> groupSessionRequests;
	@Nonnull
	private final List<PinboardNoteApiResponse> pinboardNotes;
	@Nonnull
	private final List<ContentApiResponse> contents;
	@Nonnull
	private final List<TopicCenterRowTagApiResponse> topicCenterRowTags;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface TopicCenterRowApiResponseFactory {
		@Nonnull
		TopicCenterRowApiResponse create(@Nonnull TopicCenterRowDetail topicCenterRow);
	}

	@AssistedInject
	public TopicCenterRowApiResponse(@Nonnull TopicCenterService topicCenterService,
																	 @Nonnull GroupSessionApiResponseFactory groupSessionApiResponseFactory,
																	 @Nonnull GroupSessionRequestApiResponseFactory groupSessionRequestApiResponseFactory,
																	 @Nonnull PinboardNoteApiResponseFactory pinboardNoteApiResponseFactory,
																	 @Nonnull ContentApiResponseFactory contentApiResponseFactory,
																	 @Nonnull TopicCenterRowTagApiResponseFactory topicCenterRowTagApiResponseFactory,
																	 @Nonnull Formatter formatter,
																	 @Nonnull Strings strings,
																	 @Assisted @Nonnull TopicCenterRowDetail topicCenterRow) {
		requireNonNull(topicCenterService);
		requireNonNull(groupSessionApiResponseFactory);
		requireNonNull(groupSessionRequestApiResponseFactory);
		requireNonNull(pinboardNoteApiResponseFactory);
		requireNonNull(contentApiResponseFactory);
		requireNonNull(topicCenterRowTagApiResponseFactory);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(topicCenterRow);

		this.topicCenterRowId = topicCenterRow.getTopicCenterRowId();
		this.title = topicCenterRow.getTitle();
		this.description = topicCenterRow.getDescription();
		this.groupSessionsTitle = topicCenterRow.getGroupSessionsTitle();
		this.groupSessionsDescription = topicCenterRow.getGroupSessionsDescription();
		this.groupSessionRequestsTitle = topicCenterRow.getGroupSessionRequestsTitle();
		this.groupSessionRequestsDescription = topicCenterRow.getGroupSessionRequestsDescription();

		this.groupSessions = topicCenterRow.getGroupSessions() == null ? Collections.emptyList() :
				topicCenterRow.getGroupSessions().stream()
						.map(groupSession -> groupSessionApiResponseFactory.create(groupSession))
						.collect(Collectors.toList());

		this.groupSessionRequests = topicCenterRow.getGroupSessionRequests() == null ? Collections.emptyList() :
				topicCenterRow.getGroupSessionRequests().stream()
						.map(groupSessionRequest -> groupSessionRequestApiResponseFactory.create(groupSessionRequest))
						.collect(Collectors.toList());

		this.pinboardNotes = topicCenterRow.getPinboardNotes() == null ? Collections.emptyList() :
				topicCenterRow.getPinboardNotes().stream()
						.map(pinboardNote -> pinboardNoteApiResponseFactory.create(pinboardNote))
						.collect(Collectors.toList());

		this.contents = topicCenterRow.getContents() == null ? Collections.emptyList() :
				topicCenterRow.getContents().stream()
						.map(content -> contentApiResponseFactory.create(content))
						.collect(Collectors.toList());

		this.topicCenterRowTags = topicCenterRow.getTopicCenterRowTags() == null ? Collections.emptyList() :
				topicCenterRow.getTopicCenterRowTags().stream()
						.map(topicCenterRowTag -> topicCenterRowTagApiResponseFactory.create(topicCenterRowTag))
						.collect(Collectors.toList());
	}

	@Nonnull
	public UUID getTopicCenterRowId() {
		return this.topicCenterRowId;
	}

	@Nonnull
	public String getTitle() {
		return this.title;
	}

	@Nonnull
	public Optional<String> getDescription() {
		return Optional.ofNullable(this.description);
	}

	@Nonnull
	public Optional<String> getGroupSessionsTitle() {
		return Optional.ofNullable(this.groupSessionsTitle);
	}

	@Nonnull
	public Optional<String> getGroupSessionsDescription() {
		return Optional.ofNullable(this.groupSessionsDescription);
	}

	@Nonnull
	public Optional<String> getGroupSessionRequestsTitle() {
		return Optional.ofNullable(this.groupSessionRequestsTitle);
	}

	@Nonnull
	public Optional<String> getGroupSessionRequestsDescription() {
		return Optional.ofNullable(this.groupSessionRequestsDescription);
	}

	@Nonnull
	public List<GroupSessionApiResponse> getGroupSessions() {
		return this.groupSessions;
	}

	@Nonnull
	public List<GroupSessionRequestApiResponse> getGroupSessionRequests() {
		return this.groupSessionRequests;
	}

	@Nonnull
	public List<PinboardNoteApiResponse> getPinboardNotes() {
		return this.pinboardNotes;
	}

	@Nonnull
	public List<ContentApiResponse> getContents() {
		return this.contents;
	}

	@Nonnull
	public List<TopicCenterRowTagApiResponse> getTopicCenterRowTags() {
		return this.topicCenterRowTags;
	}
}