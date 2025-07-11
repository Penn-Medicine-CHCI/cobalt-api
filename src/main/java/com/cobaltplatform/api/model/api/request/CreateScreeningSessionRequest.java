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
import java.util.Map;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreateScreeningSessionRequest {
	@Nullable
	private UUID screeningFlowId;
	@Nullable
	private UUID screeningFlowVersionId;
	@Nullable
	private UUID targetAccountId;
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private UUID groupSessionId;
	@Nullable
	private UUID courseSessionId;
	@Nullable
	private UUID accountCheckInActionId;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private Boolean immediatelySkip;
	@Nullable
	private Map<String, Object> metadata;

	@Nullable
	public UUID getScreeningFlowId() {
		return this.screeningFlowId;
	}

	public void setScreeningFlowId(@Nullable UUID screeningFlowId) {
		this.screeningFlowId = screeningFlowId;
	}

	@Nullable
	public UUID getScreeningFlowVersionId() {
		return this.screeningFlowVersionId;
	}

	public void setScreeningFlowVersionId(@Nullable UUID screeningFlowVersionId) {
		this.screeningFlowVersionId = screeningFlowVersionId;
	}

	@Nullable
	public UUID getTargetAccountId() {
		return this.targetAccountId;
	}

	public void setTargetAccountId(@Nullable UUID targetAccountId) {
		this.targetAccountId = targetAccountId;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	public void setPatientOrderId(@Nullable UUID patientOrderId) {
		this.patientOrderId = patientOrderId;
	}

	@Nullable
	public UUID getGroupSessionId() {
		return this.groupSessionId;
	}

	public void setGroupSessionId(@Nullable UUID groupSessionId) {
		this.groupSessionId = groupSessionId;
	}

	@Nullable
	public UUID getCourseSessionId() {
		return this.courseSessionId;
	}

	public void setCourseSessionId(@Nullable UUID courseSessionId) {
		this.courseSessionId = courseSessionId;
	}

	@Nullable
	public Boolean getImmediatelySkip() {
		return this.immediatelySkip;
	}

	public void setImmediatelySkip(@Nullable Boolean immediatelySkip) {
		this.immediatelySkip = immediatelySkip;
	}

	@Nullable
	public UUID getAccountCheckInActionId() {
		return accountCheckInActionId;
	}

	public void setAccountCheckInActionId(@Nullable UUID accountCheckInActionId) {
		this.accountCheckInActionId = accountCheckInActionId;
	}

	@Nullable
	public Map<String, Object> getMetadata() {
		return this.metadata;
	}

	public void setMetadata(@Nullable Map<String, Object> metadata) {
		this.metadata = metadata;
	}
}