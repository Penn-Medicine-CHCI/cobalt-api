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
public class GetScheduleDaysForProviderRequest {
	@Nullable
	private String UserID;
	@Nullable
	private String UserIDType;
	@Nullable
	private String ProviderID;
	@Nullable
	private String ProviderIDType;
	@Nullable
	private String StartDate; // 05/03/2020
	@Nullable
	private String EndDate; // 05/03/2020
	@Nullable
	private List<DepartmentID> DepartmentIDs;
	@Nullable
	private List<VisitTypeID> VisitTypeIDs;

	@NotThreadSafe
	public static class DepartmentID {
		@Nullable
		private String ID;
		@Nullable
		private String Type;

		@Nullable
		public String getID() {
			return this.ID;
		}

		public void setID(@Nullable String ID) {
			this.ID = ID;
		}

		@Nullable
		public String getType() {
			return this.Type;
		}

		public void setType(@Nullable String type) {
			Type = type;
		}
	}

	@NotThreadSafe
	public static class VisitTypeID {
		@Nullable
		private String ID;
		@Nullable
		private String Type;

		@Nullable
		public String getID() {
			return this.ID;
		}

		public void setID(@Nullable String ID) {
			this.ID = ID;
		}

		@Nullable
		public String getType() {
			return this.Type;
		}

		public void setType(@Nullable String type) {
			Type = type;
		}
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

	@Nullable
	public String getProviderID() {
		return this.ProviderID;
	}

	public void setProviderID(@Nullable String providerID) {
		ProviderID = providerID;
	}

	@Nullable
	public String getProviderIDType() {
		return this.ProviderIDType;
	}

	public void setProviderIDType(@Nullable String providerIDType) {
		ProviderIDType = providerIDType;
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
	public List<DepartmentID> getDepartmentIDs() {
		return this.DepartmentIDs;
	}

	public void setDepartmentIDs(@Nullable List<DepartmentID> departmentIDs) {
		DepartmentIDs = departmentIDs;
	}

	@Nullable
	public List<VisitTypeID> getVisitTypeIDs() {
		return this.VisitTypeIDs;
	}

	public void setVisitTypeIDs(@Nullable List<VisitTypeID> visitTypeIDs) {
		VisitTypeIDs = visitTypeIDs;
	}
}
