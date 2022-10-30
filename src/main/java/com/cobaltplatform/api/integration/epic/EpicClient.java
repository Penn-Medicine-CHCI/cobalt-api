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
import com.cobaltplatform.api.integration.epic.response.PatientSearchResponse;
import com.cobaltplatform.api.integration.epic.response.ScheduleAppointmentWithInsuranceResponse;
import com.cobaltplatform.api.integration.mychart.MyChartAccessToken;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public interface EpicClient {
	@Nonnull
	EpicConfiguration getEpicConfiguration();

	@Nonnull
	Optional<Object> findPatientFhirR4(@Nonnull MyChartAccessToken myChartAccessToken,
																		 @Nullable String patientId);

	@Nonnull
	PatientSearchResponse performPatientSearch(@Nonnull PatientSearchRequest request);

	@Nonnull
	GetPatientDemographicsResponse performGetPatientDemographics(@Nonnull GetPatientDemographicsRequest request);

	@Nonnull
	GetProviderScheduleResponse performGetProviderSchedule(@Nonnull GetProviderScheduleRequest request);

	@Nonnull
	GetPatientAppointmentsResponse performGetPatientAppointments(@Nonnull GetPatientAppointmentsRequest request);

	@Nonnull
	PatientCreateResponse performPatientCreate(@Nonnull PatientCreateRequest request);

	@Nonnull
	ScheduleAppointmentWithInsuranceResponse performScheduleAppointmentWithInsurance(@Nonnull ScheduleAppointmentWithInsuranceRequest request);

	@Nonnull
	CancelAppointmentResponse performCancelAppointment(@Nonnull CancelAppointmentRequest request);

	@Nonnull
	LocalDate parseDateWithHyphens(@Nonnull String date);

	@Nonnull
	String formatDateWithHyphens(@Nonnull LocalDate date);

	@Nonnull
	LocalDate parseDateWithSlashes(@Nonnull String date);

	@Nonnull
	String formatDateWithSlashes(@Nonnull LocalDate date);

	@Nonnull
	String formatTimeInMilitary(@Nonnull LocalTime time);

	@Nonnull
	LocalTime parseTimeAmPm(@Nonnull String time);

	@Nonnull
	String formatPhoneNumber(@Nonnull String phoneNumber);

	@Nonnull
	@Deprecated
	default Optional<String> determineLatestUIDForPatientIdentifier(@Nonnull String oldIdentifierId,
																																	@Nonnull String oldIdentifierTypeId) {
		requireNonNull(oldIdentifierId);
		requireNonNull(oldIdentifierTypeId);

		// If we already have a UID, we don't need to requery for it
		if (oldIdentifierTypeId.equals("UID"))
			return Optional.of(oldIdentifierId);

		PatientSearchRequest searchRequest = new PatientSearchRequest();
		searchRequest.setIdentifier(oldIdentifierId);

		PatientSearchResponse response = performPatientSearch(searchRequest);

		if (response.getEntry().size() == 0)
			return Optional.empty();

		return extractUIDFromPatientEntry(response.getEntry().get(0));
	}

	@Nonnull
	default Optional<String> extractUIDFromPatientEntry(@Nonnull PatientSearchResponse.Entry patientEntry) {
		requireNonNull(patientEntry);

		if (patientEntry.getResource().getIdentifier() != null)
			for (PatientSearchResponse.Entry.Resource.Identifier identifier : patientEntry.getResource().getIdentifier())
				if ("urn:oid:1.3.6.1.4.1.22812.19.44324.0".equals(identifier.getSystem()))
					return Optional.of(identifier.getValue());

		return Optional.empty();
	}
}
