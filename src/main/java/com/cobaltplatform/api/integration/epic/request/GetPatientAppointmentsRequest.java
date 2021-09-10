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

package com.cobaltplatform.api.integration.epic.request;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class GetPatientAppointmentsRequest {
	@Nullable
	private String UserID;
	@Nullable
	private String UserIDType;
	@Nullable
	private String PatientID;
	@Nullable
	private String PatientIDType;
	@Nullable
	private String StartDate;
	@Nullable
	private String EndDate;
	@Nullable
	private String IncludeAllStatuses;  // e.g. "1"
	@Nullable
	private String IncludeOutsideAppointments; // e.g. "1"
	@Nullable
	private List<String> ExtraItems; // e.g. ["7040"]

	@Nullable
	public String getUserID() {
		return UserID;
	}

	public void setUserID(@Nullable String userID) {
		UserID = userID;
	}

	@Nullable
	public String getUserIDType() {
		return UserIDType;
	}

	public void setUserIDType(@Nullable String userIDType) {
		UserIDType = userIDType;
	}

	@Nullable
	public String getPatientID() {
		return PatientID;
	}

	public void setPatientID(@Nullable String patientID) {
		PatientID = patientID;
	}

	@Nullable
	public String getPatientIDType() {
		return PatientIDType;
	}

	public void setPatientIDType(@Nullable String patientIDType) {
		PatientIDType = patientIDType;
	}

	@Nullable
	public String getStartDate() {
		return StartDate;
	}

	public void setStartDate(@Nullable String startDate) {
		StartDate = startDate;
	}

	@Nullable
	public String getEndDate() {
		return EndDate;
	}

	public void setEndDate(@Nullable String endDate) {
		EndDate = endDate;
	}

	@Nullable
	public String getIncludeAllStatuses() {
		return IncludeAllStatuses;
	}

	public void setIncludeAllStatuses(@Nullable String includeAllStatuses) {
		IncludeAllStatuses = includeAllStatuses;
	}

	@Nullable
	public String getIncludeOutsideAppointments() {
		return IncludeOutsideAppointments;
	}

	public void setIncludeOutsideAppointments(@Nullable String includeOutsideAppointments) {
		IncludeOutsideAppointments = includeOutsideAppointments;
	}

	@Nullable
	public List<String> getExtraItems() {
		return ExtraItems;
	}

	public void setExtraItems(@Nullable List<String> extraItems) {
		ExtraItems = extraItems;
	}
}
