/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
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

@NotThreadSafe
public class CareEncounterCancellationReason {
	@Nullable
	private CareEncounterCancellationReasonId careEncounterCancellationReasonId;
	@Nullable
	private String description;
	@Nullable
	private Integer displayOrder;
	@Nullable
	private Boolean freeformTextRequired;

	public enum CareEncounterCancellationReasonId {
		PATIENT_REQUESTED,
		NO_LONGER_NEEDED,
		UNABLE_TO_REACH_PATIENT,
		SCHEDULING_CONFLICT,
		DUPLICATE_BOOKING,
		OTHER
	}

	@Nullable
	public CareEncounterCancellationReasonId getCareEncounterCancellationReasonId() {
		return this.careEncounterCancellationReasonId;
	}

	public void setCareEncounterCancellationReasonId(@Nullable CareEncounterCancellationReasonId careEncounterCancellationReasonId) {
		this.careEncounterCancellationReasonId = careEncounterCancellationReasonId;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return this.displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	@Nullable
	public Boolean getFreeformTextRequired() {
		return this.freeformTextRequired;
	}

	public void setFreeformTextRequired(@Nullable Boolean freeformTextRequired) {
		this.freeformTextRequired = freeformTextRequired;
	}
}
