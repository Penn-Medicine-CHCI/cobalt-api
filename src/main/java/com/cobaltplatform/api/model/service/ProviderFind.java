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

package com.cobaltplatform.api.model.service;

import com.cobaltplatform.api.integration.epic.code.AppointmentParticipantStatusCode;
import com.cobaltplatform.api.integration.epic.code.AppointmentStatusCode;
import com.cobaltplatform.api.integration.epic.code.SlotStatusCode;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.Specialty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class ProviderFind {
	@Nullable
	private UUID providerId;
	@Nullable
	private String name;
	@Nullable
	private String title;
	@Nullable
	private String entity;
	@Nullable
	private String clinic;
	@Nullable
	private String license;
	@Nullable
	private String specialty;
	@Nullable
	private String supportRolesDescription;
	@Nullable
	private Boolean phoneNumberRequiredForAppointment;
	@Nullable
	private String imageUrl;
	@Nullable
	private String bioUrl;
	@Nullable
	private String description;
	@Nullable
	private SchedulingSystemId schedulingSystemId;
	@Nullable
	private List<String> paymentFundingDescriptions;
	@Nullable
	private Set<UUID> appointmentTypeIds;
	@Nullable
	private Set<UUID> epicDepartmentIds;
	@Nullable
	private List<AvailabilityDate> dates;
	@Nullable
	private Boolean intakeAssessmentRequired;
	@Nullable
	private Boolean intakeAssessmentIneligible;
	@Nullable
	private Boolean skipIntakePrompt;
	@Nullable
	private String treatmentDescription;
	@Nullable
	private List<Specialty> specialties;
	private String phoneNumber;
	@Nullable
	private Boolean displayPhoneNumberOnlyForBooking;

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.DEFAULT_STYLE);
	}

	public enum AvailabilityStatus {
		AVAILABLE,
		BOOKED
	}

	@NotThreadSafe
	public static class AvailabilityDate {
		@Nullable
		private LocalDate date;
		@Nullable
		private Boolean fullyBooked; // Computed field for quick reference
		@Nullable
		private List<AvailabilityTime> times;

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
		}

		@Nullable
		public LocalDate getDate() {
			return date;
		}

		public void setDate(@Nullable LocalDate date) {
			this.date = date;
		}

		@Nullable
		public List<AvailabilityTime> getTimes() {
			return times;
		}

		public void setTimes(@Nullable List<AvailabilityTime> times) {
			this.times = times;
		}

		@Nullable
		public Boolean getFullyBooked() {
			return fullyBooked;
		}

		public void setFullyBooked(@Nullable Boolean fullyBooked) {
			this.fullyBooked = fullyBooked;
		}
	}

	@NotThreadSafe
	public static class AvailabilityTime {
		@Nullable
		private LocalTime time;
		@Nullable
		private AvailabilityStatus status;
		@Nullable
		private List<UUID> appointmentTypeIds;
		@Nullable
		private UUID epicDepartmentId; // Optional, Epic-only
		@Nullable
		private String epicAppointmentFhirId; // Optional, Epic FHIR-only
		@Nullable
		private Map<UUID, SlotStatusCode> slotStatusCodesByAppointmentTypeId; // Optional, Epic FHIR-only
		@Nullable
		private Map<UUID, AppointmentStatusCode> appointmentStatusCodesByAppointmentTypeId; // Optional, Epic FHIR-only
		@Nullable
		private Map<UUID, AppointmentParticipantStatusCode> appointmentParticipantStatusCodesByAppointmentTypeId; // Optional, Epic FHIR-only

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
		}

		@Nullable
		public LocalTime getTime() {
			return time;
		}

		public void setTime(@Nullable LocalTime time) {
			this.time = time;
		}

		@Nullable
		public AvailabilityStatus getStatus() {
			return status;
		}

		public void setStatus(@Nullable AvailabilityStatus status) {
			this.status = status;
		}

		@Nullable
		public List<UUID> getAppointmentTypeIds() {
			return appointmentTypeIds;
		}

		public void setAppointmentTypeIds(@Nullable List<UUID> appointmentTypeIds) {
			this.appointmentTypeIds = appointmentTypeIds;
		}

		@Nullable
		public UUID getEpicDepartmentId() {
			return epicDepartmentId;
		}

		public void setEpicDepartmentId(@Nullable UUID epicDepartmentId) {
			this.epicDepartmentId = epicDepartmentId;
		}

		@Nullable
		public String getEpicAppointmentFhirId() {
			return this.epicAppointmentFhirId;
		}

		public void setEpicAppointmentFhirId(@Nullable String epicAppointmentFhirId) {
			this.epicAppointmentFhirId = epicAppointmentFhirId;
		}

		@Nullable
		public Map<UUID, SlotStatusCode> getSlotStatusCodesByAppointmentTypeId() {
			return this.slotStatusCodesByAppointmentTypeId;
		}

		public void setSlotStatusCodesByAppointmentTypeId(@Nullable Map<UUID, SlotStatusCode> slotStatusCodesByAppointmentTypeId) {
			this.slotStatusCodesByAppointmentTypeId = slotStatusCodesByAppointmentTypeId;
		}

		@Nullable
		public Map<UUID, AppointmentStatusCode> getAppointmentStatusCodesByAppointmentTypeId() {
			return this.appointmentStatusCodesByAppointmentTypeId;
		}

		public void setAppointmentStatusCodesByAppointmentTypeId(@Nullable Map<UUID, AppointmentStatusCode> appointmentStatusCodesByAppointmentTypeId) {
			this.appointmentStatusCodesByAppointmentTypeId = appointmentStatusCodesByAppointmentTypeId;
		}

		@Nullable
		public Map<UUID, AppointmentParticipantStatusCode> getAppointmentParticipantStatusCodesByAppointmentTypeId() {
			return this.appointmentParticipantStatusCodesByAppointmentTypeId;
		}

		public void setAppointmentParticipantStatusCodesByAppointmentTypeId(@Nullable Map<UUID, AppointmentParticipantStatusCode> appointmentParticipantStatusCodesByAppointmentTypeId) {
			this.appointmentParticipantStatusCodesByAppointmentTypeId = appointmentParticipantStatusCodesByAppointmentTypeId;
		}
	}

	@Nullable
	public UUID getProviderId() {
		return providerId;
	}

	public void setProviderId(@Nullable UUID providerId) {
		this.providerId = providerId;
	}

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getTitle() {
		return title;
	}

	public void setTitle(@Nullable String title) {
		this.title = title;
	}

	@Nullable
	public String getEntity() {
		return entity;
	}

	public void setEntity(@Nullable String entity) {
		this.entity = entity;
	}

	@Nullable
	public String getClinic() {
		return clinic;
	}

	public void setClinic(@Nullable String clinic) {
		this.clinic = clinic;
	}

	@Nullable
	public String getLicense() {
		return license;
	}

	public void setLicense(@Nullable String license) {
		this.license = license;
	}

	@Nullable
	public String getSpecialty() {
		return specialty;
	}

	public void setSpecialty(@Nullable String specialty) {
		this.specialty = specialty;
	}

	@Nullable
	public String getSupportRolesDescription() {
		return supportRolesDescription;
	}

	public void setSupportRolesDescription(@Nullable String supportRolesDescription) {
		this.supportRolesDescription = supportRolesDescription;
	}

	@Nullable
	public Boolean getPhoneNumberRequiredForAppointment() {
		return phoneNumberRequiredForAppointment;
	}

	public void setPhoneNumberRequiredForAppointment(@Nullable Boolean phoneNumberRequiredForAppointment) {
		this.phoneNumberRequiredForAppointment = phoneNumberRequiredForAppointment;
	}

	@Nullable
	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(@Nullable String imageUrl) {
		this.imageUrl = imageUrl;
	}

	@Nullable
	public String getBioUrl() {
		return bioUrl;
	}

	public void setBioUrl(@Nullable String bioUrl) {
		this.bioUrl = bioUrl;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public SchedulingSystemId getSchedulingSystemId() {
		return schedulingSystemId;
	}

	public void setSchedulingSystemId(@Nullable SchedulingSystemId schedulingSystemId) {
		this.schedulingSystemId = schedulingSystemId;
	}

	@Nullable
	public List<String> getPaymentFundingDescriptions() {
		return paymentFundingDescriptions;
	}

	public void setPaymentFundingDescriptions(@Nullable List<String> paymentFundingDescriptions) {
		this.paymentFundingDescriptions = paymentFundingDescriptions;
	}

	@Nullable
	public Set<UUID> getAppointmentTypeIds() {
		return appointmentTypeIds;
	}

	public void setAppointmentTypeIds(@Nullable Set<UUID> appointmentTypeIds) {
		this.appointmentTypeIds = appointmentTypeIds;
	}

	@Nullable
	public List<AvailabilityDate> getDates() {
		return dates;
	}

	public void setDates(@Nullable List<AvailabilityDate> dates) {
		this.dates = dates;
	}

	@Nullable
	public Boolean getIntakeAssessmentRequired() {
		return intakeAssessmentRequired;
	}

	public void setIntakeAssessmentRequired(@Nullable Boolean intakeAssessmentRequired) {
		this.intakeAssessmentRequired = intakeAssessmentRequired;
	}

	@Nullable
	public Boolean getIntakeAssessmentIneligible() {
		return intakeAssessmentIneligible;
	}

	public void setIntakeAssessmentIneligible(@Nullable Boolean intakeAssessmentIneligible) {
		this.intakeAssessmentIneligible = intakeAssessmentIneligible;
	}

	@Nullable
	public Boolean getSkipIntakePrompt() {
		return skipIntakePrompt;
	}

	public void setSkipIntakePrompt(@Nullable Boolean skipIntakePrompt) {
		this.skipIntakePrompt = skipIntakePrompt;
	}

	@Nullable
	public String getTreatmentDescription() {
		return treatmentDescription;
	}

	public void setTreatmentDescription(@Nullable String treatmentDescription) {
		this.treatmentDescription = treatmentDescription;
	}

	@Nullable
	public Set<UUID> getEpicDepartmentIds() {
		return epicDepartmentIds;
	}

	public void setEpicDepartmentIds(@Nullable Set<UUID> epicDepartmentIds) {
		this.epicDepartmentIds = epicDepartmentIds;
	}

	@Nullable
	public List<Specialty> getSpecialties() {
		return specialties;
	}

	public void setSpecialties(@Nullable List<Specialty> specialties) {
		this.specialties = specialties;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@Nullable
	public Boolean getDisplayPhoneNumberOnlyForBooking() {
		return displayPhoneNumberOnlyForBooking;
	}

	public void setDisplayPhoneNumberOnlyForBooking(@Nullable Boolean displayPhoneNumberOnlyForBooking) {
		this.displayPhoneNumberOnlyForBooking = displayPhoneNumberOnlyForBooking;
	}
}
