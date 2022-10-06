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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class InstitutionTopicCenter {
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private UUID topicCenterId;
	@Nullable
	private Boolean navigationItemEnabled;
	@Nullable
	private String navigationIconName;
	@Nullable
	private Integer navigationDisplayOrder;

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public UUID getTopicCenterId() {
		return this.topicCenterId;
	}

	public void setTopicCenterId(@Nullable UUID topicCenterId) {
		this.topicCenterId = topicCenterId;
	}

	@Nullable
	public Boolean getNavigationItemEnabled() {
		return this.navigationItemEnabled;
	}

	public void setNavigationItemEnabled(@Nullable Boolean navigationItemEnabled) {
		this.navigationItemEnabled = navigationItemEnabled;
	}

	@Nullable
	public String getNavigationIconName() {
		return this.navigationIconName;
	}

	public void setNavigationIconName(@Nullable String navigationIconName) {
		this.navigationIconName = navigationIconName;
	}

	@Nullable
	public Integer getNavigationDisplayOrder() {
		return this.navigationDisplayOrder;
	}

	public void setNavigationDisplayOrder(@Nullable Integer navigationDisplayOrder) {
		this.navigationDisplayOrder = navigationDisplayOrder;
	}
}
