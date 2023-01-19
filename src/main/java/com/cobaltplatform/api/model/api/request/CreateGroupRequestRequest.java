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

package com.cobaltplatform.api.model.api.request;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Set;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreateGroupRequestRequest {
	@Nullable
	private UUID requestorAccountId;
	@Nullable
	private String requestorName;
	@Nullable
	private String requestorEmailAddress;
	@Nullable
	private String preferredDateDescription;
	@Nullable
	private String preferredTimeDescription;
	@Nullable
	private String additionalDescription;
	@Nullable
	private String otherGroupTopicsDescription;
	@Nullable
	private Integer minimumAttendeeCount;
	@Nullable
	private Integer maximumAttendeeCount;
	@Nullable
	private Set<UUID> groupTopicIds;

	@Nullable
	public UUID getRequestorAccountId() {
		return this.requestorAccountId;
	}

	public void setRequestorAccountId(@Nullable UUID requestorAccountId) {
		this.requestorAccountId = requestorAccountId;
	}

	@Nullable
	public String getRequestorName() {
		return this.requestorName;
	}

	public void setRequestorName(@Nullable String requestorName) {
		this.requestorName = requestorName;
	}

	@Nullable
	public String getRequestorEmailAddress() {
		return this.requestorEmailAddress;
	}

	public void setRequestorEmailAddress(@Nullable String requestorEmailAddress) {
		this.requestorEmailAddress = requestorEmailAddress;
	}

	@Nullable
	public String getPreferredDateDescription() {
		return this.preferredDateDescription;
	}

	public void setPreferredDateDescription(@Nullable String preferredDateDescription) {
		this.preferredDateDescription = preferredDateDescription;
	}

	@Nullable
	public String getPreferredTimeDescription() {
		return this.preferredTimeDescription;
	}

	public void setPreferredTimeDescription(@Nullable String preferredTimeDescription) {
		this.preferredTimeDescription = preferredTimeDescription;
	}

	@Nullable
	public String getAdditionalDescription() {
		return this.additionalDescription;
	}

	public void setAdditionalDescription(@Nullable String additionalDescription) {
		this.additionalDescription = additionalDescription;
	}

	@Nullable
	public String getOtherGroupTopicsDescription() {
		return this.otherGroupTopicsDescription;
	}

	public void setOtherGroupTopicsDescription(@Nullable String otherGroupTopicsDescription) {
		this.otherGroupTopicsDescription = otherGroupTopicsDescription;
	}

	@Nullable
	public Integer getMinimumAttendeeCount() {
		return this.minimumAttendeeCount;
	}

	public void setMinimumAttendeeCount(@Nullable Integer minimumAttendeeCount) {
		this.minimumAttendeeCount = minimumAttendeeCount;
	}

	@Nullable
	public Integer getMaximumAttendeeCount() {
		return this.maximumAttendeeCount;
	}

	public void setMaximumAttendeeCount(@Nullable Integer maximumAttendeeCount) {
		this.maximumAttendeeCount = maximumAttendeeCount;
	}

	@Nullable
	public Set<UUID> getGroupTopicIds() {
		return this.groupTopicIds;
	}

	public void setGroupTopicIds(@Nullable Set<UUID> groupTopicIds) {
		this.groupTopicIds = groupTopicIds;
	}
}
