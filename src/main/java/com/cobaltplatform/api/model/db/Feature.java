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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import com.cobaltplatform.api.model.db.NavigationHeader.NavigationHeaderId;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;

import java.time.Instant;
import java.util.List;


/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class Feature {
	@Nullable
	private FeatureId featureId;
	@Nullable
	private NavigationHeaderId navigationHeaderId;
	@Nullable
	private String name;
	@Nullable
	private String urlName;
	@Nullable
	private List<SupportRoleId> supportRoleIds;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	//Joined in from feature_institution
	@Nullable
	private String description;

	public enum FeatureId {
		THERAPY,
		MEDICATION_SUBSCRIBER,
		GROUP_SESSIONS,
		COACHING,
		SELF_HELP_RESOURCES,
		SPIRITUAL_SUPPORT,
		CRISIS_SUPPORT
	}

	@Nullable
	public FeatureId getFeatureId() {
		return featureId;
	}

	public void setFeatureId(@Nullable FeatureId featureId) {
		this.featureId = featureId;
	}

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getUrlName() {
		return urlName;
	}

	public void setUrlName(@Nullable String urlName) {
		this.urlName = urlName;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	@Nullable
	public List<SupportRoleId> getSupportRoleIds() {
		return supportRoleIds;
	}

	public void setSupportRoleIds(@Nullable List<SupportRoleId> supportRoleIds) {
		this.supportRoleIds = supportRoleIds;
	}

	@Nullable
	public NavigationHeaderId getNavigationHeaderId() {
		return navigationHeaderId;
	}

	public void setNavigationHeaderId(@Nullable NavigationHeaderId navigationHeaderId) {
		this.navigationHeaderId = navigationHeaderId;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public String getDescription() {
		return description;
	}
}