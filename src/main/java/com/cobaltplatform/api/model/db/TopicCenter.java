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

import com.cobaltplatform.api.model.db.TopicCenterDisplayStyle.TopicCenterDisplayStyleId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class TopicCenter {
	@Nullable
	private UUID topicCenterId;
	@Nullable
	private TopicCenterDisplayStyleId topicCenterDisplayStyleId;
	@Nullable
	private String name;
	@Nullable
	private String description;
	@Nullable
	private String urlName;
	@Nullable
	private String featuredTitle;
	@Nullable
	private String featuredDescription;
	@Nullable
	private String featuredCallToAction;
	@Nullable
	private String navDescription;
	@Nullable
	private String imageUrl;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getTopicCenterId() {
		return this.topicCenterId;
	}

	public void setTopicCenterId(@Nullable UUID topicCenterId) {
		this.topicCenterId = topicCenterId;
	}

	@Nullable
	public String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public String getUrlName() {
		return this.urlName;
	}

	public void setUrlName(@Nullable String urlName) {
		this.urlName = urlName;
	}

	@Nullable
	public TopicCenterDisplayStyleId getTopicCenterDisplayStyleId() {
		return this.topicCenterDisplayStyleId;
	}

	public void setTopicCenterDisplayStyleId(@Nullable TopicCenterDisplayStyleId topicCenterDisplayStyleId) {
		this.topicCenterDisplayStyleId = topicCenterDisplayStyleId;
	}

	@Nullable
	public String getFeaturedTitle() {
		return this.featuredTitle;
	}

	public void setFeaturedTitle(@Nullable String featuredTitle) {
		this.featuredTitle = featuredTitle;
	}

	@Nullable
	public String getFeaturedDescription() {
		return this.featuredDescription;
	}

	public void setFeaturedDescription(@Nullable String featuredDescription) {
		this.featuredDescription = featuredDescription;
	}

	@Nullable
	public String getFeaturedCallToAction() {
		return this.featuredCallToAction;
	}

	public void setFeaturedCallToAction(@Nullable String featuredCallToAction) {
		this.featuredCallToAction = featuredCallToAction;
	}

	@Nullable
	public String getNavDescription() {
		return this.navDescription;
	}

	public void setNavDescription(@Nullable String navDescription) {
		this.navDescription = navDescription;
	}

	@Nullable
	public String getImageUrl() {
		return this.imageUrl;
	}

	public void setImageUrl(@Nullable String imageUrl) {
		this.imageUrl = imageUrl;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
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
