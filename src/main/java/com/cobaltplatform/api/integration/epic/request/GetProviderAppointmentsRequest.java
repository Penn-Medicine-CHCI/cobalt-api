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
public class GetProviderAppointmentsRequest {
	@Nullable
	private String UserID;
	@Nullable
	private String UserIDType;
	@Nullable
	private String StartDate; // 5/03/2020
	@Nullable
	private String EndDate; // 5/03/2020
	@Nullable
	private List<Provider> Providers;

	@NotThreadSafe
	public static class Provider {
		@Nullable
		private String ID;
		@Nullable
		private String IDType;
		@Nullable
		private String DepartmentID;
		@Nullable
		private String DepartmentIDType;

		@Nullable
		public String getID() {
			return this.ID;
		}

		public void setID(@Nullable String ID) {
			this.ID = ID;
		}

		@Nullable
		public String getIDType() {
			return this.IDType;
		}

		public void setIDType(@Nullable String IDType) {
			this.IDType = IDType;
		}

		@Nullable
		public String getDepartmentID() {
			return this.DepartmentID;
		}

		public void setDepartmentID(@Nullable String departmentID) {
			DepartmentID = departmentID;
		}

		@Nullable
		public String getDepartmentIDType() {
			return this.DepartmentIDType;
		}

		public void setDepartmentIDType(@Nullable String departmentIDType) {
			DepartmentIDType = departmentIDType;
		}
	}

	// "UserID":"15498182",
	//
	//"UserIDType":"CID",
	//
	//"StartDate":"05/03/2020",
	//
	////"EndDate":"",
	//
	////"CombineDepartments":"",
	//
	////"ResourceType":"",
	//
	////"Specialty":"",
	//
	////"ExtraItems":["8"],
	//
	//"Providers":[{"ID":"R09651","IDType":"External","DepartmentID":"603","DepartmentIDType":"EXTERNAL"}]
	//
	////"Departments":[{"ID":"10501101","Type":"External"}],
	//
	////"Subgroups":[{"ID":"","IDType":"","DepartmentID":"","DepartmentIDType":""}],
	//
	////"ExtraExtensions":[{"ID":"","Type":""}]}'


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

	@Nullable
	public String getStartDate() {
		return this.StartDate;
	}

	public void setStartDate(@Nullable String startDate) {
		StartDate = startDate;
	}

	@Nullable
	public String getEndDate() {
		return this.EndDate;
	}

	public void setEndDate(@Nullable String endDate) {
		EndDate = endDate;
	}

	@Nullable
	public List<Provider> getProviders() {
		return this.Providers;
	}

	public void setProviders(@Nullable List<Provider> providers) {
		Providers = providers;
	}
}
