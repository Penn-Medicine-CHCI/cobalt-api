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

/**
 * Private Epic Interconnect GetCoverages query parameters.
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class GetCoveragesRequest {
	@Nullable
	private String PatientID;
	@Nullable
	private String PatientIDType;
	@Nullable
	private String UserID;
	@Nullable
	private String UserIDType;

	@Nullable
	public String getPatientID() {
		return this.PatientID;
	}

	public void setPatientID(@Nullable String patientID) {
		PatientID = patientID;
	}

	@Nullable
	public String getPatientIDType() {
		return this.PatientIDType;
	}

	public void setPatientIDType(@Nullable String patientIDType) {
		PatientIDType = patientIDType;
	}

	@Nullable
	public String getUserID() {
		return this.UserID;
	}

	public void setUserID(@Nullable String userID) {
		UserID = userID;
	}

	@Nullable
	public String getUserIDType() {
		return this.UserIDType;
	}

	public void setUserIDType(@Nullable String userIDType) {
		UserIDType = userIDType;
	}
}
