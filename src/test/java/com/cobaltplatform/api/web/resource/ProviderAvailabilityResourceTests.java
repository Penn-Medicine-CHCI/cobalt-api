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

package com.cobaltplatform.api.web.resource;

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.response.ProviderAvailabilityApiResponse;
import com.cobaltplatform.api.model.api.response.ProviderAvailabilityApiResponse.AppointmentModalityAvailabilityApiResponse;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityId;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.Clinic;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.VideoconferencePlatform.VideoconferencePlatformId;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.cobaltplatform.api.model.service.ProviderFind;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityDate;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityStatus;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityTime;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * @author Transmogrify, LLC.
 */
public class ProviderAvailabilityResourceTests {
	@Test
	public void appointmentTypeFilterResolvesClinicSlotAppointmentDescription() {
		UUID providerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID clinicId = UUID.fromString("00000000-0000-0000-0000-000000000002");
		UUID firstAppointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000011");
		UUID secondAppointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000012");
		LocalDate date = LocalDate.of(2026, 1, 1);
		Provider provider = provider(providerId, "Test Provider", VideoconferencePlatformId.SWITCHBOARD);
		Clinic clinic = clinic(clinicId, "Test Clinic");
		AppointmentType firstAppointmentType = appointmentType(firstAppointmentTypeId, "Alpha Visit", "Alpha appointment description");
		AppointmentType secondAppointmentType = appointmentType(secondAppointmentTypeId, "Beta Visit", "Beta appointment description");
		ProviderFind providerFind = providerFind(providerId, "Test Provider", availabilityDate(date, List.of(
				availabilityTime(LocalTime.of(9, 0), AvailabilityStatus.AVAILABLE, List.of(firstAppointmentTypeId, secondAppointmentTypeId))
		)));
		List<ProviderFind> providerFinds = List.of(providerFind);

		ProviderAvailabilityResource.filterProviderFindsByAppointmentTypeIds(providerFinds, Set.of(secondAppointmentTypeId));
		ProviderAvailabilityApiResponse response = new ProviderAvailabilityApiResponse(currentContextProvider(), clinic,
				providerFinds, Map.of(providerId, provider), Map.of(
				firstAppointmentTypeId, firstAppointmentType,
				secondAppointmentTypeId, secondAppointmentType
		), date, date.plusDays(90));
		AppointmentModalityAvailabilityApiResponse virtualAvailability = response.getAppointmentModalities().get(0);

		assertEquals(ProviderAppointmentModalityId.VIRTUAL, virtualAvailability.getAppointmentModalityId());
		assertEquals(List.of(secondAppointmentTypeId), virtualAvailability.getAvailability().get(0).getTimes().get(0).getAppointmentTypeIds());
		assertEquals("Beta appointment description", virtualAvailability.getAvailability().get(0).getTimes().get(0).getAppointmentTypeDescription());
	}

	@Nonnull
	protected javax.inject.Provider<CurrentContext> currentContextProvider() {
		return () -> new CurrentContext.Builder(InstitutionId.COBALT, Locale.US, ZoneId.of("America/New_York")).build();
	}

	@Nonnull
	protected Provider provider(@Nonnull UUID providerId,
															@Nonnull String name,
															@Nonnull VideoconferencePlatformId videoconferencePlatformId) {
		Provider provider = new Provider();
		provider.setProviderId(providerId);
		provider.setInstitutionId(InstitutionId.COBALT);
		provider.setName(name);
		provider.setActive(true);
		provider.setVideoconferencePlatformId(videoconferencePlatformId);
		return provider;
	}

	@Nonnull
	protected Clinic clinic(@Nonnull UUID clinicId,
													@Nonnull String description) {
		Clinic clinic = new Clinic();
		clinic.setClinicId(clinicId);
		clinic.setInstitutionId(InstitutionId.COBALT);
		clinic.setDescription(description);
		return clinic;
	}

	@Nonnull
	protected AppointmentType appointmentType(@Nonnull UUID appointmentTypeId,
																						@Nonnull String name,
																						@Nonnull String description) {
		AppointmentType appointmentType = new AppointmentType();
		appointmentType.setAppointmentTypeId(appointmentTypeId);
		appointmentType.setName(name);
		appointmentType.setDescription(description);
		appointmentType.setDurationInMinutes(60L);
		appointmentType.setSchedulingSystemId(SchedulingSystemId.COBALT);
		appointmentType.setVisitTypeId(VisitTypeId.INITIAL);
		return appointmentType;
	}

	@Nonnull
	protected ProviderFind providerFind(@Nonnull UUID providerId,
																			@Nonnull String name,
																			@Nonnull AvailabilityDate availabilityDate) {
		ProviderFind providerFind = new ProviderFind();
		providerFind.setProviderId(providerId);
		providerFind.setName(name);
		providerFind.setDates(List.of(availabilityDate));
		return providerFind;
	}

	@Nonnull
	protected AvailabilityDate availabilityDate(@Nonnull LocalDate date,
																							@Nonnull List<AvailabilityTime> availabilityTimes) {
		AvailabilityDate availabilityDate = new AvailabilityDate();
		availabilityDate.setDate(date);
		availabilityDate.setTimes(availabilityTimes);
		return availabilityDate;
	}

	@Nonnull
	protected AvailabilityTime availabilityTime(@Nonnull LocalTime time,
																							@Nonnull AvailabilityStatus status,
																							@Nonnull List<UUID> appointmentTypeIds) {
		AvailabilityTime availabilityTime = new AvailabilityTime();
		availabilityTime.setTime(time);
		availabilityTime.setStatus(status);
		availabilityTime.setAppointmentTypeIds(appointmentTypeIds);
		return availabilityTime;
	}
}
