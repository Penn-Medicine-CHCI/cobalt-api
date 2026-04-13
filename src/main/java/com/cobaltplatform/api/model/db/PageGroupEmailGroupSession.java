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
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PageGroupEmailGroupSession {
	@Nullable
	private UUID pageGroupEmailGroupSessionId;
	@Nullable
	private UUID pageGroupId;
	@Nullable
	private UUID groupSessionId;
	@Nullable
	private String descriptionOverride;
	@Nullable
	private Integer displayOrder;

	@Nullable
	public UUID getPageGroupEmailGroupSessionId() {
		return pageGroupEmailGroupSessionId;
	}

	public void setPageGroupEmailGroupSessionId(@Nullable UUID pageGroupEmailGroupSessionId) {
		this.pageGroupEmailGroupSessionId = pageGroupEmailGroupSessionId;
	}

	@Nullable
	public UUID getPageGroupId() {
		return pageGroupId;
	}

	public void setPageGroupId(@Nullable UUID pageGroupId) {
		this.pageGroupId = pageGroupId;
	}

	@Nullable
	public UUID getGroupSessionId() {
		return groupSessionId;
	}

	public void setGroupSessionId(@Nullable UUID groupSessionId) {
		this.groupSessionId = groupSessionId;
	}

	@Nullable
	public String getDescriptionOverride() {
		return descriptionOverride;
	}

	public void setDescriptionOverride(@Nullable String descriptionOverride) {
		this.descriptionOverride = descriptionOverride;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}
}
