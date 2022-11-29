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

package com.cobaltplatform.api.integration.epic;

import com.cobaltplatform.api.integration.epic.request.CancelAppointmentRequest;
import com.cobaltplatform.api.integration.epic.request.GetPatientAppointmentsRequest;
import com.cobaltplatform.api.integration.epic.request.GetPatientDemographicsRequest;
import com.cobaltplatform.api.integration.epic.request.GetProviderScheduleRequest;
import com.cobaltplatform.api.integration.epic.request.PatientCreateRequest;
import com.cobaltplatform.api.integration.epic.request.PatientSearchRequest;
import com.cobaltplatform.api.integration.epic.request.ScheduleAppointmentWithInsuranceRequest;
import com.cobaltplatform.api.integration.epic.response.CancelAppointmentResponse;
import com.cobaltplatform.api.integration.epic.response.GetPatientAppointmentsResponse;
import com.cobaltplatform.api.integration.epic.response.GetPatientDemographicsResponse;
import com.cobaltplatform.api.integration.epic.response.GetProviderScheduleResponse;
import com.cobaltplatform.api.integration.epic.response.PatientCreateResponse;
import com.cobaltplatform.api.integration.epic.response.PatientFhirR4Response;
import com.cobaltplatform.api.integration.epic.response.PatientSearchResponse;
import com.cobaltplatform.api.integration.epic.response.ScheduleAppointmentWithInsuranceResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Optional;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class MockEpicClient implements EpicClient {
	@Nonnull
	@Override
	public Optional<PatientFhirR4Response> findPatientFhirR4(@Nonnull String patientId) {
		return Optional.empty();
	}

	@Nonnull
	@Override
	public PatientSearchResponse performPatientSearch(@Nonnull PatientSearchRequest request) {
		PatientSearchResponse response = new PatientSearchResponse();
		response.setEntry(Collections.emptyList());
		response.setLink(Collections.emptyList());

		return response;
	}

	@Nonnull
	@Override
	public GetPatientDemographicsResponse performGetPatientDemographics(@Nonnull GetPatientDemographicsRequest request) {
		GetPatientDemographicsResponse response = new GetPatientDemographicsResponse();
		return response;
	}

	@Nonnull
	@Override
	public GetProviderScheduleResponse performGetProviderSchedule(@Nonnull GetProviderScheduleRequest request) {
		GetProviderScheduleResponse response = new GetProviderScheduleResponse();
		response.setScheduleSlots(Collections.emptyList());
		response.setProviderMessages(Collections.emptyList());
		response.setProviderIDs(Collections.emptyList());
		response.setDepartmentIDs(Collections.emptyList());

		return response;
	}

	@Nonnull
	@Override
	public GetPatientAppointmentsResponse performGetPatientAppointments(@Nonnull GetPatientAppointmentsRequest request) {
		GetPatientAppointmentsResponse response = new GetPatientAppointmentsResponse();
		response.setAppointments(Collections.emptyList());
		return response;
	}

	@Nonnull
	@Override
	public PatientCreateResponse performPatientCreate(@Nonnull PatientCreateRequest request) {
		PatientCreateResponse response = new PatientCreateResponse();
		return response;
	}

	@Nonnull
	@Override
	public ScheduleAppointmentWithInsuranceResponse performScheduleAppointmentWithInsurance(@Nonnull ScheduleAppointmentWithInsuranceRequest request) {
		ScheduleAppointmentWithInsuranceResponse response = new ScheduleAppointmentWithInsuranceResponse();
		response.setAppointment(new ScheduleAppointmentWithInsuranceResponse.Appointment());
		return response;
	}

	@Nonnull
	@Override
	public CancelAppointmentResponse performCancelAppointment(@Nonnull CancelAppointmentRequest request) {
		return new CancelAppointmentResponse();
	}

	@Nonnull
	@Override
	public LocalDate parseDateWithHyphens(@Nonnull String date) {
		return LocalDate.now(ZoneId.of("America/New_York"));
	}

	@Nonnull
	@Override
	public String formatDateWithHyphens(@Nonnull LocalDate date) {
		return "2020-08-31";
	}

	@Nonnull
	@Override
	public LocalDate parseDateWithSlashes(@Nonnull String date) {
		return LocalDate.now(ZoneId.of("America/New_York"));
	}

	@Nonnull
	@Override
	public String formatDateWithSlashes(@Nonnull LocalDate date) {
		return "08/31/2020";
	}

	@Nonnull
	@Override
	public String formatTimeInMilitary(@Nonnull LocalTime time) {
		return "11:45";
	}

	@Nonnull
	@Override
	public LocalTime parseTimeAmPm(@Nonnull String time) {
		return LocalTime.now(ZoneId.of("America/New_York"));
	}

	@Nonnull
	@Override
	public String formatPhoneNumber(@Nonnull String phoneNumber) {
		return phoneNumber;
	}
}
