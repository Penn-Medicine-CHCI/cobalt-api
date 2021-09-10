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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.db.ContentType.ContentTypeId;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ContentApiResponse {
	@Nonnull
	private final UUID contentId;
	@Nonnull
	private final ContentTypeId contentTypeId;
	@Nonnull
	private final String title;
	@Nullable
	private final String url;
	@Nullable
	private final Instant dateCreated;
	@Nonnull
	private final String dateCreatedDescription;
	@Nullable
	private String imageUrl;
	@Nullable
	private final String description;
	@Nullable
	private final String author;
	@Nullable
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nullable
	private final Instant lastUpdated;
	@Nullable
	private final String lastUpdatedDescription;
	@Nullable
	private String contentTypeDescription;
	@Nullable
	private String callToAction;
	@Nullable
	private Boolean newFlag;
	@Nullable
	private String contentTypeLabel;
	@Nullable
	private String duration;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ContentApiResponseFactory {
		@Nonnull
		ContentApiResponse create(@Nonnull Content content);
	}

	@AssistedInject
	public ContentApiResponse(@Nonnull Formatter formatter,
														@Assisted @Nonnull Content content) {
		requireNonNull(formatter);
		requireNonNull(content);

		this.contentId = content.getContentId();
		this.contentTypeId = content.getContentTypeId();
		this.title = content.getTitle();
		this.url = content.getUrl();
		this.dateCreated = content.getDateCreated();
		this.dateCreatedDescription = content.getDateCreated() != null ? formatter.formatTimestamp(content.getDateCreated()) : null;
		this.imageUrl = content.getImageUrl();
		this.description = content.getDescription();
		this.author = content.getAuthor();
		this.created = content.getCreated();
		this.createdDescription = formatter.formatTimestamp(content.getCreated());
		this.lastUpdated = content.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(content.getLastUpdated());
		this.contentTypeDescription = content.getContentTypeDescription();
		this.callToAction = content.getCallToAction();
		this.newFlag = content.getNewFlag();
		this.contentTypeLabel = content.getContentTypeLabel();
		this.duration = content.getDurationInMinutes() != null ?
				String.format("%s min", content.getDurationInMinutes().toString()) : null;
	}

	@Nonnull
	public UUID getContentId() {
		return contentId;
	}

	@Nonnull
	public ContentTypeId getContentTypeId() {
		return contentTypeId;
	}

	@Nonnull
	public String getTitle() {
		return title;
	}

	@Nullable
	public String getUrl() {
		return url;
	}

	@Nullable
	public Instant getDateCreated() {
		return dateCreated;
	}

	@Nonnull
	public String getDateCreatedDescription() {
		return dateCreatedDescription;
	}

	@Nullable
	public String getImageUrl() {
		return imageUrl;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	@Nullable
	public String getAuthor() {
		return author;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	@Nonnull
	public String getCreatedDescription() {
		return createdDescription;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	@Nullable
	public String getContentTypeLabel() {
		return contentTypeLabel;
	}

	@Nullable
	public String getLastUpdatedDescription() {
		return lastUpdatedDescription;
	}

	@Nullable
	public String getContentTypeDescription() {
		return contentTypeDescription;
	}

	@Nullable
	public String getCallToAction() {
		return callToAction;
	}

	@Nullable
	public Boolean getNewFlag() {
		return newFlag;
	}

	@Nullable
	public String getDuration() {
		return duration;
	}

	public void setDuration(@Nullable String duration) {
		this.duration = duration;
	}
}