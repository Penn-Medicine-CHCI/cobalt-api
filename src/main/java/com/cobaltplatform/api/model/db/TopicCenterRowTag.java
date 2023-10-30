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

import com.cobaltplatform.api.model.db.TopicCenterRowTagType.TopicCenterRowTagTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class TopicCenterRowTag {
	@Nullable
	private UUID topicCenterRowId;
	@Nullable
	private TopicCenterRowTagTypeId topicCenterRowTagTypeId;
	@Nullable
	private String tagId;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private String title;
	@Nullable
	private String description;
	@Nullable
	private String cta;
	@Nullable
	private String ctaUrl;
	@Nullable
	private Integer displayOrder;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getTopicCenterRowId() {
		return this.topicCenterRowId;
	}

	public void setTopicCenterRowId(@Nullable UUID topicCenterRowId) {
		this.topicCenterRowId = topicCenterRowId;
	}

	@Nullable
	public TopicCenterRowTagTypeId getTopicCenterRowTagTypeId() {
		return this.topicCenterRowTagTypeId;
	}

	public void setTopicCenterRowTagTypeId(@Nullable TopicCenterRowTagTypeId topicCenterRowTagTypeId) {
		this.topicCenterRowTagTypeId = topicCenterRowTagTypeId;
	}

	@Nullable
	public String getTagId() {
		return this.tagId;
	}

	public void setTagId(@Nullable String tagId) {
		this.tagId = tagId;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public String getTitle() {
		return this.title;
	}

	public void setTitle(@Nullable String title) {
		this.title = title;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public String getCta() {
		return this.cta;
	}

	public void setCta(@Nullable String cta) {
		this.cta = cta;
	}

	@Nullable
	public String getCtaUrl() {
		return this.ctaUrl;
	}

	public void setCtaUrl(@Nullable String ctaUrl) {
		this.ctaUrl = ctaUrl;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return this.displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	@Nullable
	public Instant getCreated() {
		return this.created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}