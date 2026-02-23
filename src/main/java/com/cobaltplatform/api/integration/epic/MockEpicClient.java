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

import com.cobaltplatform.api.integration.epic.request.AddFlowsheetValueRequest;
import com.cobaltplatform.api.integration.epic.request.AppointmentBookFhirStu3Request;
import com.cobaltplatform.api.integration.epic.request.AppointmentFindFhirStu3Request;
import com.cobaltplatform.api.integration.epic.request.AppointmentSearchFhirStu3Request;
import com.cobaltplatform.api.integration.epic.request.CancelAppointmentRequest;
import com.cobaltplatform.api.integration.epic.request.GetPatientAppointmentsRequest;
import com.cobaltplatform.api.integration.epic.request.GetCoveragesRequest;
import com.cobaltplatform.api.integration.epic.request.GetPatientDemographicsRequest;
import com.cobaltplatform.api.integration.epic.request.GetProviderAppointmentsRequest;
import com.cobaltplatform.api.integration.epic.request.GetProviderAvailabilityRequest;
import com.cobaltplatform.api.integration.epic.request.GetProviderScheduleRequest;
import com.cobaltplatform.api.integration.epic.request.GetScheduleDaysForProviderRequest;
import com.cobaltplatform.api.integration.epic.request.PatientCreateRequest;
import com.cobaltplatform.api.integration.epic.request.PatientSearchRequest;
import com.cobaltplatform.api.integration.epic.request.ScheduleAppointmentWithInsuranceRequest;
import com.cobaltplatform.api.integration.epic.response.AddFlowsheetValueResponse;
import com.cobaltplatform.api.integration.epic.response.AppointmentBookFhirStu3Response;
import com.cobaltplatform.api.integration.epic.response.AppointmentFindFhirStu3Response;
import com.cobaltplatform.api.integration.epic.response.AppointmentSearchFhirStu3Response;
import com.cobaltplatform.api.integration.epic.response.CancelAppointmentResponse;
import com.cobaltplatform.api.integration.epic.response.CoverageSearchFhirR4Response;
import com.cobaltplatform.api.integration.epic.response.EncounterSearchFhirR4Response;
import com.cobaltplatform.api.integration.epic.response.GetCoveragesResponse;
import com.cobaltplatform.api.integration.epic.response.GetPatientAppointmentsResponse;
import com.cobaltplatform.api.integration.epic.response.GetPatientDemographicsResponse;
import com.cobaltplatform.api.integration.epic.response.GetProviderAppointmentsResponse;
import com.cobaltplatform.api.integration.epic.response.GetProviderAvailabilityResponse;
import com.cobaltplatform.api.integration.epic.response.GetProviderScheduleResponse;
import com.cobaltplatform.api.integration.epic.response.GetScheduleDaysForProviderResponse;
import com.cobaltplatform.api.integration.epic.response.PatientCreateResponse;
import com.cobaltplatform.api.integration.epic.response.PatientReadFhirR4Response;
import com.cobaltplatform.api.integration.epic.response.PatientSearchResponse;
import com.cobaltplatform.api.integration.epic.response.ScheduleAppointmentWithInsuranceResponse;
import com.google.gson.Gson;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class MockEpicClient implements EpicClient {
	@Nonnull
	private final Gson gson;

	public MockEpicClient() {
		this.gson = EpicUtilities.defaultGson();
	}

	@Nonnull
	@Override
	public Optional<PatientReadFhirR4Response> patientReadFhirR4(@Nullable String patientId) {
		return Optional.empty();
	}

	@Nonnull
	@Override
	public PatientSearchResponse patientSearchFhirR4(@Nullable String patientIdSystem,
																									 @Nullable String patientIdValue) {
		PatientSearchResponse patientSearchResponse = new PatientSearchResponse();
		patientSearchResponse.setEntry(List.of());
		patientSearchResponse.setLink(List.of());
		patientSearchResponse.setType("searchset");
		patientSearchResponse.setTotal(0);
		patientSearchResponse.setResourceType("Bundle");

		return patientSearchResponse;
	}

	@Nonnull
	@Override
	public AppointmentFindFhirStu3Response appointmentFindFhirStu3(@Nonnull AppointmentFindFhirStu3Request request) {
		requireNonNull(request);
		return acquireMockResponseInstance(AppointmentFindFhirStu3Response.class);
	}

	@Nonnull
	@Override
	public AppointmentBookFhirStu3Response appointmentBookFhirStu3(@Nonnull AppointmentBookFhirStu3Request request) {
		requireNonNull(request);
		return acquireMockResponseInstance(AppointmentBookFhirStu3Response.class);
	}

	@Nonnull
	@Override
	public AppointmentSearchFhirStu3Response appointmentSearchFhirStu3(@Nonnull AppointmentSearchFhirStu3Request request) {
		requireNonNull(request);
		return acquireMockResponseInstance(AppointmentSearchFhirStu3Response.class);
	}

	@Nonnull
	@Override
	public EncounterSearchFhirR4Response encounterSearchFhirR4(@Nullable String patientId) {
		return acquireMockResponseInstance(EncounterSearchFhirR4Response.class);
	}

	@Nonnull
	@Override
	public CoverageSearchFhirR4Response coverageSearchFhirR4(@Nullable String patientId) {
		CoverageSearchFhirR4Response coverageSearchResponse = new CoverageSearchFhirR4Response();
		coverageSearchResponse.setEntry(List.of());
		coverageSearchResponse.setLink(List.of());
		coverageSearchResponse.setType("searchset");
		coverageSearchResponse.setTotal(0);
		coverageSearchResponse.setResourceType("Bundle");

		return coverageSearchResponse;
	}

	@Nonnull
	@Override
	public GetCoveragesResponse getCoverages(@Nonnull GetCoveragesRequest request) {
		requireNonNull(request);
		return acquireMockResponseInstance(GetCoveragesResponse.class);
	}

	@Nonnull
	@Override
	public PatientSearchResponse performPatientSearch(@Nonnull PatientSearchRequest request) {
		requireNonNull(request);
		return acquireMockResponseInstance(PatientSearchResponse.class);
	}

	@Nonnull
	@Override
	public GetPatientDemographicsResponse performGetPatientDemographics(@Nonnull GetPatientDemographicsRequest request) {
		requireNonNull(request);
		return acquireMockResponseInstance(GetPatientDemographicsResponse.class);
	}

	@Nonnull
	@Override
	public GetProviderScheduleResponse performGetProviderSchedule(@Nonnull GetProviderScheduleRequest request) {
		requireNonNull(request);

		GetProviderScheduleResponse response = acquireMockResponseInstance(GetProviderScheduleResponse.class);
		response.setScheduleSlots(Collections.emptyList());
		response.setProviderMessages(Collections.emptyList());
		response.setProviderIDs(Collections.emptyList());
		response.setDepartmentIDs(Collections.emptyList());

		return response;
	}

	@Nonnull
	@Override
	public GetPatientAppointmentsResponse performGetPatientAppointments(@Nonnull GetPatientAppointmentsRequest request) {
		requireNonNull(request);

		GetPatientAppointmentsResponse response = acquireMockResponseInstance(GetPatientAppointmentsResponse.class);
		response.setAppointments(Collections.emptyList());

		return response;
	}

	@Nonnull
	@Override
	public PatientCreateResponse performPatientCreate(@Nonnull PatientCreateRequest request) {
		requireNonNull(request);
		return acquireMockResponseInstance(PatientCreateResponse.class);
	}

	@Nonnull
	@Override
	public ScheduleAppointmentWithInsuranceResponse performScheduleAppointmentWithInsurance(@Nonnull ScheduleAppointmentWithInsuranceRequest request) {
		requireNonNull(request);
		return acquireMockResponseInstance(ScheduleAppointmentWithInsuranceResponse.class);
	}

	@Nonnull
	@Override
	public CancelAppointmentResponse performCancelAppointment(@Nonnull CancelAppointmentRequest request) {
		requireNonNull(request);
		return acquireMockResponseInstance(CancelAppointmentResponse.class);
	}

	@Nonnull
	@Override
	public AddFlowsheetValueResponse addFlowsheetValue(@Nonnull AddFlowsheetValueRequest request) {
		AddFlowsheetValueResponse response = new AddFlowsheetValueResponse();
		return response;
	}

	@Nonnull
	@Override
	public GetProviderAppointmentsResponse getProviderAppointments(@Nonnull GetProviderAppointmentsRequest request) {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public GetScheduleDaysForProviderResponse getScheduleDaysForProvider(@Nonnull GetScheduleDaysForProviderRequest request) {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public GetProviderAvailabilityResponse getProviderAvailability(@Nonnull GetProviderAvailabilityRequest request) {
		throw new UnsupportedOperationException();
	}

	/**
	 * If there is a response file available on the filesystem, use it.
	 * Otherwise, make a new "empty" instance and return it.
	 */
	@Nonnull
	protected <T> T acquireMockResponseInstance(@Nonnull Class<T> type) {
		requireNonNull(type);

		// e.g. AppointmentFindFhirStu3Response -> AppointmentFindFhirStu3
		String directoryName = type.getSimpleName().replace("Response", "");
		Path mockJsonFile = Path.of(format("resources/mock/epic/%s/response.json", directoryName));

		if (!Files.exists(mockJsonFile)) {
			try {
				return type.getDeclaredConstructor().newInstance();
			} catch (NoSuchMethodException
							 | SecurityException
							 | InstantiationException
							 | IllegalAccessException
							 | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}

		try {
			String mockJson = Files.readString(mockJsonFile, StandardCharsets.UTF_8);
			return getGson().fromJson(mockJson, type);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
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

	@Nonnull
	protected Gson getGson() {
		return this.gson;
	}
}
