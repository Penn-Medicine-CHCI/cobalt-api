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
import java.time.LocalDate;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class GetProviderScheduleRequest {
	@Nullable
	private String ProviderID;
	@Nullable
	private String ProviderIDType;
	@Nullable
	private String DepartmentID;
	@Nullable
	private String DepartmentIDType;
	@Nullable
	private String UserID;
	@Nullable
	private String UserIDType;
	@Nullable
	private String VisitTypeID;
	@Nullable
	private String VisitTypeIDType;
	@Nullable
	private LocalDate Date;

	@Nullable
	public String getProviderID() {
		return ProviderID;
	}

	public void setProviderID(@Nullable String providerID) {
		ProviderID = providerID;
	}

	@Nullable
	public String getProviderIDType() {
		return ProviderIDType;
	}

	public void setProviderIDType(@Nullable String providerIDType) {
		ProviderIDType = providerIDType;
	}

	@Nullable
	public String getDepartmentID() {
		return DepartmentID;
	}

	public void setDepartmentID(@Nullable String departmentID) {
		DepartmentID = departmentID;
	}

	@Nullable
	public String getDepartmentIDType() {
		return DepartmentIDType;
	}

	public void setDepartmentIDType(@Nullable String departmentIDType) {
		DepartmentIDType = departmentIDType;
	}

	@Nullable
	public String getVisitTypeID() {
		return VisitTypeID;
	}

	public void setVisitTypeID(@Nullable String visitTypeID) {
		VisitTypeID = visitTypeID;
	}

	@Nullable
	public String getVisitTypeIDType() {
		return VisitTypeIDType;
	}

	public void setVisitTypeIDType(@Nullable String visitTypeIDType) {
		VisitTypeIDType = visitTypeIDType;
	}

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
	public LocalDate getDate() {
		return Date;
	}

	public void setDate(@Nullable LocalDate date) {
		Date = date;
	}
}
