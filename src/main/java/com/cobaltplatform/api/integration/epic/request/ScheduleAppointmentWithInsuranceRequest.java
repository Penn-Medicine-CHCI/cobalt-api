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
public class ScheduleAppointmentWithInsuranceRequest {
	@Nullable
	private String Date;
	@Nullable
	private String DepartmentID;
	@Nullable
	private String DepartmentIDType;
	@Nullable
	private Boolean IsReviewOnly;
	@Nullable
	private String PatientID;
	@Nullable
	private String PatientIDType;
	@Nullable
	private String ProviderID;
	@Nullable
	private String ProviderIDType;
	@Nullable
	private String Time;
	@Nullable
	private String VisitTypeID;
	@Nullable
	private String VisitTypeIDType;
	@Nullable
	private Insurance Insurance;
	@Nullable
	private List<String> Comments;

	@NotThreadSafe
	public static class Insurance {
		@Nullable
		private String GroupNumber;
		@Nullable
		private String InsuranceName;
		@Nullable
		private String MemberNumber;
		@Nullable
		private String PayorID;
		@Nullable
		private String PayorIDType;
		@Nullable
		private String SubscriberDateOfBirth;
		@Nullable
		private String SubscriberID;
		@Nullable
		private String SubscriberName;

		@Nullable
		public String getGroupNumber() {
			return GroupNumber;
		}

		public void setGroupNumber(@Nullable String groupNumber) {
			GroupNumber = groupNumber;
		}

		@Nullable
		public String getInsuranceName() {
			return InsuranceName;
		}

		public void setInsuranceName(@Nullable String insuranceName) {
			InsuranceName = insuranceName;
		}

		@Nullable
		public String getMemberNumber() {
			return MemberNumber;
		}

		public void setMemberNumber(@Nullable String memberNumber) {
			MemberNumber = memberNumber;
		}

		@Nullable
		public String getPayorID() {
			return PayorID;
		}

		public void setPayorID(@Nullable String payorID) {
			PayorID = payorID;
		}

		@Nullable
		public String getPayorIDType() {
			return PayorIDType;
		}

		public void setPayorIDType(@Nullable String payorIDType) {
			PayorIDType = payorIDType;
		}

		@Nullable
		public String getSubscriberDateOfBirth() {
			return SubscriberDateOfBirth;
		}

		public void setSubscriberDateOfBirth(@Nullable String subscriberDateOfBirth) {
			SubscriberDateOfBirth = subscriberDateOfBirth;
		}

		@Nullable
		public String getSubscriberID() {
			return SubscriberID;
		}

		public void setSubscriberID(@Nullable String subscriberID) {
			SubscriberID = subscriberID;
		}

		@Nullable
		public String getSubscriberName() {
			return SubscriberName;
		}

		public void setSubscriberName(@Nullable String subscriberName) {
			SubscriberName = subscriberName;
		}
	}

	@Nullable
	public String getDate() {
		return Date;
	}

	public void setDate(@Nullable String date) {
		Date = date;
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
	public Boolean getIsReviewOnly() {
		return IsReviewOnly;
	}

	public void setIsReviewOnly(@Nullable Boolean reviewOnly) {
		IsReviewOnly = reviewOnly;
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
	public String getTime() {
		return Time;
	}

	public void setTime(@Nullable String time) {
		Time = time;
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
	public ScheduleAppointmentWithInsuranceRequest.Insurance getInsurance() {
		return Insurance;
	}

	public void setInsurance(@Nullable ScheduleAppointmentWithInsuranceRequest.Insurance insurance) {
		Insurance = insurance;
	}

	@Nullable
	public List<String> getComments() {
		return Comments;
	}

	public void setComments(@Nullable List<String> comments) {
		Comments = comments;
	}
}
