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

package com.cobaltplatform.api.integration.epic.response;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class GetProviderAppointmentsResponse {
	@Nullable
	private String rawJson;

	@Nullable
	private List<Appointment> Appointments;

	// TODO: real fields

	@NotThreadSafe
	public static class Appointment {
		@Nullable
		private String PatientName;
		@Nullable
		private String Date;
		@Nullable
		private String VisitTypeName;
		@Nullable
		private List<String> AppointmentNotes;
		@Nullable
		private String AppointmentStartTime; // e.g. " 9:00 AM"
		@Nullable
		private String AppointmentDuration; // e.g. "30"
		@Nullable
		private String AppointmentStatus; // e.g. "Scheduled"
		@Nullable
		private List<TypedId> PatientIDs;
		@Nullable
		private List<TypedId> ContactIDs;
		@Nullable
		private List<TypedId> VisitTypeIDs;
		@Nullable
		private List<Provider> Providers;

		@Nullable
		public String getPatientName() {
			return this.PatientName;
		}

		public void setPatientName(@Nullable String patientName) {
			PatientName = patientName;
		}

		@Nullable
		public String getDate() {
			return this.Date;
		}

		public void setDate(@Nullable String date) {
			Date = date;
		}

		@Nullable
		public String getVisitTypeName() {
			return this.VisitTypeName;
		}

		public void setVisitTypeName(@Nullable String visitTypeName) {
			VisitTypeName = visitTypeName;
		}

		@Nullable
		public List<String> getAppointmentNotes() {
			return this.AppointmentNotes;
		}

		public void setAppointmentNotes(@Nullable List<String> appointmentNotes) {
			AppointmentNotes = appointmentNotes;
		}

		@Nullable
		public String getAppointmentStartTime() {
			return this.AppointmentStartTime;
		}

		public void setAppointmentStartTime(@Nullable String appointmentStartTime) {
			AppointmentStartTime = appointmentStartTime;
		}

		@Nullable
		public String getAppointmentDuration() {
			return this.AppointmentDuration;
		}

		public void setAppointmentDuration(@Nullable String appointmentDuration) {
			AppointmentDuration = appointmentDuration;
		}

		@Nullable
		public String getAppointmentStatus() {
			return this.AppointmentStatus;
		}

		public void setAppointmentStatus(@Nullable String appointmentStatus) {
			AppointmentStatus = appointmentStatus;
		}

		@Nullable
		public List<TypedId> getPatientIDs() {
			return this.PatientIDs;
		}

		public void setPatientIDs(@Nullable List<TypedId> patientIDs) {
			PatientIDs = patientIDs;
		}

		@Nullable
		public List<TypedId> getContactIDs() {
			return this.ContactIDs;
		}

		public void setContactIDs(@Nullable List<TypedId> contactIDs) {
			ContactIDs = contactIDs;
		}

		@Nullable
		public List<TypedId> getVisitTypeIDs() {
			return this.VisitTypeIDs;
		}

		public void setVisitTypeIDs(@Nullable List<TypedId> visitTypeIDs) {
			VisitTypeIDs = visitTypeIDs;
		}

		@Nullable
		public List<Provider> getProviders() {
			return this.Providers;
		}

		public void setProviders(@Nullable List<Provider> providers) {
			Providers = providers;
		}
	}

	@NotThreadSafe
	public static class Provider {
		@Nullable
		private String ProviderName;
		@Nullable
		private String DepartmentName;
		@Nullable
		private String Time;
		@Nullable
		private String Duration;
		@Nullable
		private List<TypedId> ProviderIDs;
		@Nullable
		private List<TypedId> DepartmentIDs;

		@Nullable
		public String getProviderName() {
			return this.ProviderName;
		}

		public void setProviderName(@Nullable String providerName) {
			ProviderName = providerName;
		}

		@Nullable
		public String getDepartmentName() {
			return this.DepartmentName;
		}

		public void setDepartmentName(@Nullable String departmentName) {
			DepartmentName = departmentName;
		}

		@Nullable
		public String getTime() {
			return this.Time;
		}

		public void setTime(@Nullable String time) {
			Time = time;
		}

		@Nullable
		public String getDuration() {
			return this.Duration;
		}

		public void setDuration(@Nullable String duration) {
			Duration = duration;
		}

		@Nullable
		public List<TypedId> getProviderIDs() {
			return this.ProviderIDs;
		}

		public void setProviderIDs(@Nullable List<TypedId> providerIDs) {
			ProviderIDs = providerIDs;
		}

		@Nullable
		public List<TypedId> getDepartmentIDs() {
			return this.DepartmentIDs;
		}

		public void setDepartmentIDs(@Nullable List<TypedId> departmentIDs) {
			DepartmentIDs = departmentIDs;
		}
	}

	@NotThreadSafe
	public static class TypedId {
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
	public String getRawJson() {
		return this.rawJson;
	}

	public void setRawJson(@Nullable String rawJson) {
		this.rawJson = rawJson;
	}

	@Nullable
	public List<Appointment> getAppointments() {
		return this.Appointments;
	}

	public void setAppointments(@Nullable List<Appointment> appointments) {
		Appointments = appointments;
	}
}
