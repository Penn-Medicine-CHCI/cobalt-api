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

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.cache.Cache;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.response.ProviderAvailabilityApiResponse.AppointmentModalityAvailabilityApiResponse;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityId;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.Clinic;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.VideoconferencePlatform.VideoconferencePlatformId;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.cobaltplatform.api.model.service.AppointmentBookingRequirements.AppointmentBookingRequirementsDestinationId;
import com.cobaltplatform.api.model.service.AppointmentBookingScreeningKey;
import com.cobaltplatform.api.model.service.ProviderFind;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityDate;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityStatus;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityTime;
import com.cobaltplatform.api.model.service.ProviderSearchScreeningRequirement;
import com.cobaltplatform.api.util.Formatter;
import com.lokalized.Strings;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Transmogrify, LLC.
 */
public class ProviderAvailabilityApiResponseTests {
	@Test
	public void providerAvailabilityGroupsAvailableSlotsByAppointmentModalityAndDate() {
		UUID providerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID firstAppointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000011");
		UUID secondAppointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000012");
		UUID unknownAppointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000099");
		LocalDate date = LocalDate.of(2026, 1, 1);
		Provider provider = provider(providerId, "Test Provider", VideoconferencePlatformId.SWITCHBOARD, "215-555-1000");
		AppointmentType firstAppointmentType = appointmentType(firstAppointmentTypeId, "Alpha Visit");
		AppointmentType secondAppointmentType = appointmentType(secondAppointmentTypeId, "Beta Visit");
		ProviderFind providerFind = providerFind(providerId, "Test Provider", availabilityDate(date, List.of(
				availabilityTime(LocalTime.of(9, 0), AvailabilityStatus.AVAILABLE, List.of(firstAppointmentTypeId, secondAppointmentTypeId)),
				availabilityTime(LocalTime.of(10, 0), AvailabilityStatus.BOOKED, List.of(firstAppointmentTypeId)),
				availabilityTime(LocalTime.of(11, 0), AvailabilityStatus.AVAILABLE, List.of(unknownAppointmentTypeId))
		)));

		ProviderAvailabilityApiResponse response = new ProviderAvailabilityApiResponse(currentContextProvider(), formatter(), provider,
				List.of(providerFind), Map.of(
				firstAppointmentTypeId, firstAppointmentType,
				secondAppointmentTypeId, secondAppointmentType
		), date, date.plusDays(90), Set.of());

		assertEquals(providerId, response.getProviderId());
		assertEquals(2, response.getAppointmentTypes().size());
		assertEquals(firstAppointmentTypeId, response.getAppointmentTypes().get(0).getAppointmentTypeId());
		assertEquals(secondAppointmentTypeId, response.getAppointmentTypes().get(1).getAppointmentTypeId());
		assertEquals(2, response.getAppointmentModalities().size());

		AppointmentModalityAvailabilityApiResponse phoneAvailability = response.getAppointmentModalities().get(0);
		AppointmentModalityAvailabilityApiResponse virtualAvailability = response.getAppointmentModalities().get(1);

		assertEquals(ProviderAppointmentModalityId.PHONE, phoneAvailability.getAppointmentModalityId());
		assertEquals(1, phoneAvailability.getAvailability().size());
		assertEquals(date, phoneAvailability.getAvailability().get(0).getDate());
		assertEquals(1, phoneAvailability.getAvailability().get(0).getTimes().size());
		assertEquals(LocalTime.of(9, 0), phoneAvailability.getAvailability().get(0).getTimes().get(0).getTime());
		assertEquals(List.of(firstAppointmentTypeId, secondAppointmentTypeId),
				phoneAvailability.getAvailability().get(0).getTimes().get(0).getAppointmentTypeIds());
		assertNull(phoneAvailability.getAvailability().get(0).getTimes().get(0).getAppointmentTypeDescription());

		assertEquals(ProviderAppointmentModalityId.VIRTUAL, virtualAvailability.getAppointmentModalityId());
		assertEquals(1, virtualAvailability.getAvailability().get(0).getTimes().size());
		assertEquals(LocalTime.of(9, 0), virtualAvailability.getAvailability().get(0).getTimes().get(0).getTime());
		assertEquals(List.of(firstAppointmentTypeId, secondAppointmentTypeId),
				virtualAvailability.getAvailability().get(0).getTimes().get(0).getAppointmentTypeIds());
		assertNull(virtualAvailability.getAvailability().get(0).getTimes().get(0).getAppointmentTypeDescription());
	}

	@Test
	public void providerAvailabilityExposesAppointmentDescriptionForUnambiguousSlots() {
		UUID providerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID describedAppointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000011");
		UUID blankDescriptionAppointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000012");
		UUID nullDescriptionAppointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000013");
		LocalDate date = LocalDate.of(2026, 1, 1);
		Provider provider = provider(providerId, "Test Provider", VideoconferencePlatformId.SWITCHBOARD, null);
		AppointmentType describedAppointmentType = appointmentType(describedAppointmentTypeId, "Alpha Visit", "Alpha appointment description");
		AppointmentType blankDescriptionAppointmentType = appointmentType(blankDescriptionAppointmentTypeId, "Beta Visit", " ");
		AppointmentType nullDescriptionAppointmentType = appointmentType(nullDescriptionAppointmentTypeId, "Gamma Visit", null);
		ProviderFind providerFind = providerFind(providerId, "Test Provider", availabilityDate(date, List.of(
				availabilityTime(LocalTime.of(9, 0), AvailabilityStatus.AVAILABLE, List.of(describedAppointmentTypeId)),
				availabilityTime(LocalTime.of(10, 0), AvailabilityStatus.AVAILABLE, List.of(blankDescriptionAppointmentTypeId)),
				availabilityTime(LocalTime.of(11, 0), AvailabilityStatus.AVAILABLE, List.of(nullDescriptionAppointmentTypeId))
		)));

		ProviderAvailabilityApiResponse response = new ProviderAvailabilityApiResponse(currentContextProvider(), formatter(), provider,
				List.of(providerFind), Map.of(
				describedAppointmentTypeId, describedAppointmentType,
				blankDescriptionAppointmentTypeId, blankDescriptionAppointmentType,
				nullDescriptionAppointmentTypeId, nullDescriptionAppointmentType
		), date, date.plusDays(90), Set.of());

		AppointmentModalityAvailabilityApiResponse virtualAvailability = response.getAppointmentModalities().get(0);

		assertEquals(ProviderAppointmentModalityId.VIRTUAL, virtualAvailability.getAppointmentModalityId());
		assertEquals("Alpha appointment description",
				virtualAvailability.getAvailability().get(0).getTimes().get(0).getAppointmentTypeDescription());
		assertEquals("Beta Visit",
				virtualAvailability.getAvailability().get(0).getTimes().get(1).getAppointmentTypeDescription());
		assertEquals("Gamma Visit",
					virtualAvailability.getAvailability().get(0).getTimes().get(2).getAppointmentTypeDescription());
	}

	@Test
	public void providerAvailabilityDoesNotExposeVirtualSlotsForUnsupportedVideoconferencePlatform() {
		UUID providerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID appointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000011");
		LocalDate date = LocalDate.of(2026, 1, 1);
		Provider provider = provider(providerId, "Test Provider", VideoconferencePlatformId.BLUEJEANS, "215-555-1000");
		AppointmentType appointmentType = appointmentType(appointmentTypeId, "Alpha Visit");
		ProviderFind providerFind = providerFind(providerId, "Test Provider", availabilityDate(date, List.of(
				availabilityTime(LocalTime.of(9, 0), AvailabilityStatus.AVAILABLE, List.of(appointmentTypeId))
		)));

		ProviderAvailabilityApiResponse response = new ProviderAvailabilityApiResponse(currentContextProvider(), formatter(), provider,
				List.of(providerFind), Map.of(appointmentTypeId, appointmentType), date, date.plusDays(90), Set.of());

		assertEquals(1, response.getAppointmentModalities().size());
		assertEquals(ProviderAppointmentModalityId.PHONE, response.getAppointmentModalities().get(0).getAppointmentModalityId());
		assertEquals(LocalTime.of(9, 0), response.getAppointmentModalities().get(0).getAvailability().get(0).getTimes().get(0).getTime());
	}

	@Test
	public void clinicAvailabilityAggregatesProvidersAndPreservesProviderIdentity() {
		UUID firstProviderId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID secondProviderId = UUID.fromString("00000000-0000-0000-0000-000000000002");
		UUID clinicId = UUID.fromString("00000000-0000-0000-0000-000000000003");
		UUID appointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000011");
		LocalDate date = LocalDate.of(2026, 1, 1);
		Provider firstProvider = provider(firstProviderId, "Alpha Provider", VideoconferencePlatformId.SWITCHBOARD, null);
		Provider secondProvider = provider(secondProviderId, "Beta Provider", VideoconferencePlatformId.TELEPHONE, null);
		Clinic clinic = clinic(clinicId, "Test Clinic");
		AppointmentType appointmentType = appointmentType(appointmentTypeId, "Alpha Visit");
		List<ProviderFind> providerFinds = List.of(
				providerFind(firstProviderId, "Alpha Provider", availabilityDate(date, List.of(
						availabilityTime(LocalTime.of(9, 0), AvailabilityStatus.AVAILABLE, List.of(appointmentTypeId))
				))),
				providerFind(secondProviderId, "Beta Provider", availabilityDate(date, List.of(
						availabilityTime(LocalTime.of(9, 30), AvailabilityStatus.AVAILABLE, List.of(appointmentTypeId))
				)))
		);

		ProviderAvailabilityApiResponse response = new ProviderAvailabilityApiResponse(currentContextProvider(), formatter(), clinic,
				providerFinds, Map.of(firstProviderId, firstProvider, secondProviderId, secondProvider),
				Map.of(appointmentTypeId, appointmentType), date, date.plusDays(90), Set.of());

		assertEquals(clinicId, response.getClinicId());
		assertEquals(1, response.getAppointmentTypes().size());
		assertEquals(2, response.getAppointmentModalities().size());
		assertEquals(ProviderAppointmentModalityId.PHONE, response.getAppointmentModalities().get(0).getAppointmentModalityId());
		assertEquals(ProviderAppointmentModalityId.VIRTUAL, response.getAppointmentModalities().get(1).getAppointmentModalityId());
		assertEquals(secondProviderId, response.getAppointmentModalities().get(0).getAvailability().get(0).getTimes().get(0).getProviderId());
		assertEquals(firstProviderId, response.getAppointmentModalities().get(1).getAvailability().get(0).getTimes().get(0).getProviderId());
		assertEquals("Alpha Visit", response.getAppointmentModalities().get(0).getAvailability().get(0).getTimes().get(0).getAppointmentTypeDescription());
		assertEquals("Alpha Visit", response.getAppointmentModalities().get(1).getAvailability().get(0).getTimes().get(0).getAppointmentTypeDescription());
	}

	@Test
	public void providerAvailabilityScreeningRequirementReflectsUnsatisfiedRequiredFirstAvailableAppointment() {
		UUID providerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID appointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000011");
		UUID screeningFlowId = UUID.fromString("00000000-0000-0000-0000-000000000021");
		LocalDate date = LocalDate.of(2026, 1, 1);
		Provider provider = provider(providerId, "Test Provider", VideoconferencePlatformId.SWITCHBOARD, null);
		AppointmentType appointmentType = appointmentType(appointmentTypeId, "Alpha Visit", "Alpha appointment description", screeningFlowId);
		ProviderFind providerFind = providerFind(providerId, "Test Provider", availabilityDate(date, List.of(
				availabilityTime(LocalTime.of(9, 0), AvailabilityStatus.AVAILABLE, List.of(appointmentTypeId))
		)));

		ProviderAvailabilityApiResponse response = new ProviderAvailabilityApiResponse(currentContextProvider(), formatter(), provider,
				List.of(providerFind), Map.of(appointmentTypeId, appointmentType), date, date.plusDays(90), Set.of());
		ProviderSearchScreeningRequirement screeningRequirement = response.getScreeningRequirement();

		assertNotNull(screeningRequirement);
		assertEquals(AppointmentBookingRequirementsDestinationId.SCREENING_SESSION,
				screeningRequirement.getAppointmentBookingRequirementsDestinationId());
		assertEquals(screeningFlowId, screeningRequirement.getScreeningFlowId());
		assertEquals(true, screeningRequirement.getScreeningRequired());
		assertEquals(false, screeningRequirement.getScreeningSatisfied());
	}

	@Test
	public void providerAvailabilityScreeningRequirementReflectsSatisfiedRequiredFirstAvailableAppointment() {
		UUID providerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID appointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000011");
		UUID screeningFlowId = UUID.fromString("00000000-0000-0000-0000-000000000021");
		LocalDate date = LocalDate.of(2026, 1, 1);
		Provider provider = provider(providerId, "Test Provider", VideoconferencePlatformId.SWITCHBOARD, null);
		AppointmentType appointmentType = appointmentType(appointmentTypeId, "Alpha Visit", "Alpha appointment description", screeningFlowId);
		ProviderFind providerFind = providerFind(providerId, "Test Provider", availabilityDate(date, List.of(
				availabilityTime(LocalTime.of(9, 0), AvailabilityStatus.AVAILABLE, List.of(appointmentTypeId))
		)));

		ProviderAvailabilityApiResponse response = new ProviderAvailabilityApiResponse(currentContextProvider(), formatter(), provider,
				List.of(providerFind), Map.of(appointmentTypeId, appointmentType), date, date.plusDays(90),
				Set.of(new AppointmentBookingScreeningKey(providerId, appointmentTypeId, screeningFlowId)));
		ProviderSearchScreeningRequirement screeningRequirement = response.getScreeningRequirement();

		assertNotNull(screeningRequirement);
		assertEquals(AppointmentBookingRequirementsDestinationId.APPOINTMENT_BOOKING,
				screeningRequirement.getAppointmentBookingRequirementsDestinationId());
		assertEquals(screeningFlowId, screeningRequirement.getScreeningFlowId());
		assertEquals(true, screeningRequirement.getScreeningRequired());
		assertEquals(true, screeningRequirement.getScreeningSatisfied());
	}

	@Test
	public void providerAvailabilityScreeningRequirementReflectsNoRequiredScreeningForFirstAvailableAppointment() {
		UUID providerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID appointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000011");
		LocalDate date = LocalDate.of(2026, 1, 1);
		Provider provider = provider(providerId, "Test Provider", VideoconferencePlatformId.SWITCHBOARD, null);
		AppointmentType appointmentType = appointmentType(appointmentTypeId, "Alpha Visit", "Alpha appointment description", null);
		ProviderFind providerFind = providerFind(providerId, "Test Provider", availabilityDate(date, List.of(
				availabilityTime(LocalTime.of(9, 0), AvailabilityStatus.AVAILABLE, List.of(appointmentTypeId))
		)));

		ProviderAvailabilityApiResponse response = new ProviderAvailabilityApiResponse(currentContextProvider(), formatter(), provider,
				List.of(providerFind), Map.of(appointmentTypeId, appointmentType), date, date.plusDays(90), Set.of());
		ProviderSearchScreeningRequirement screeningRequirement = response.getScreeningRequirement();

		assertNotNull(screeningRequirement);
		assertEquals(AppointmentBookingRequirementsDestinationId.APPOINTMENT_BOOKING,
				screeningRequirement.getAppointmentBookingRequirementsDestinationId());
		assertNull(screeningRequirement.getScreeningFlowId());
		assertEquals(false, screeningRequirement.getScreeningRequired());
		assertEquals(true, screeningRequirement.getScreeningSatisfied());
	}

	@Test
	public void clinicAvailabilityScreeningRequirementUsesFirstAvailableProvider() {
		UUID clinicId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID earlierProviderId = UUID.fromString("00000000-0000-0000-0000-000000000002");
		UUID laterProviderId = UUID.fromString("00000000-0000-0000-0000-000000000003");
		UUID appointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000011");
		UUID screeningFlowId = UUID.fromString("00000000-0000-0000-0000-000000000021");
		LocalDate date = LocalDate.of(2026, 1, 1);
		Clinic clinic = clinic(clinicId, "Test Clinic");
		Provider earlierProvider = provider(earlierProviderId, "Earlier Provider", VideoconferencePlatformId.SWITCHBOARD, null);
		Provider laterProvider = provider(laterProviderId, "Later Provider", VideoconferencePlatformId.SWITCHBOARD, null);
		AppointmentType appointmentType = appointmentType(appointmentTypeId, "Alpha Visit", "Alpha appointment description", screeningFlowId);
		ProviderFind earlierProviderFind = providerFind(earlierProviderId, "Earlier Provider", availabilityDate(date, List.of(
				availabilityTime(LocalTime.of(9, 0), AvailabilityStatus.AVAILABLE, List.of(appointmentTypeId))
		)));
		ProviderFind laterProviderFind = providerFind(laterProviderId, "Later Provider", availabilityDate(date, List.of(
				availabilityTime(LocalTime.of(10, 0), AvailabilityStatus.AVAILABLE, List.of(appointmentTypeId))
		)));

		ProviderAvailabilityApiResponse response = new ProviderAvailabilityApiResponse(currentContextProvider(), formatter(), clinic,
				List.of(laterProviderFind, earlierProviderFind), Map.of(earlierProviderId, earlierProvider, laterProviderId, laterProvider),
				Map.of(appointmentTypeId, appointmentType), date, date.plusDays(90),
				Set.of(new AppointmentBookingScreeningKey(laterProviderId, appointmentTypeId, screeningFlowId)));
		ProviderSearchScreeningRequirement screeningRequirement = response.getScreeningRequirement();

		assertNotNull(response.getFirstAvailableAppointment());
		assertEquals(earlierProviderId, response.getFirstAvailableAppointment().getProviderId());
		assertNotNull(screeningRequirement);
		assertEquals(AppointmentBookingRequirementsDestinationId.SCREENING_SESSION,
				screeningRequirement.getAppointmentBookingRequirementsDestinationId());
		assertEquals(false, screeningRequirement.getScreeningSatisfied());
	}

	@Test
	public void providerAvailabilityScreeningRequirementReflectsSharedFlowAmbiguousFirstAvailableAppointment() {
		UUID providerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID firstAppointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000011");
		UUID secondAppointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000012");
		UUID screeningFlowId = UUID.fromString("00000000-0000-0000-0000-000000000021");
		LocalDate date = LocalDate.of(2026, 1, 1);
		Provider provider = provider(providerId, "Test Provider", VideoconferencePlatformId.SWITCHBOARD, null);
		AppointmentType firstAppointmentType = appointmentType(firstAppointmentTypeId, "Alpha Visit", "Alpha appointment description", screeningFlowId);
		AppointmentType secondAppointmentType = appointmentType(secondAppointmentTypeId, "Beta Visit", "Beta appointment description", screeningFlowId);
		ProviderFind providerFind = providerFind(providerId, "Test Provider", availabilityDate(date, List.of(
				availabilityTime(LocalTime.of(9, 0), AvailabilityStatus.AVAILABLE, List.of(firstAppointmentTypeId, secondAppointmentTypeId))
		)));

		ProviderAvailabilityApiResponse response = new ProviderAvailabilityApiResponse(currentContextProvider(), formatter(), provider,
				List.of(providerFind), Map.of(firstAppointmentTypeId, firstAppointmentType, secondAppointmentTypeId, secondAppointmentType),
				date, date.plusDays(90), Set.of());
		ProviderSearchScreeningRequirement screeningRequirement = response.getScreeningRequirement();

		assertNotNull(screeningRequirement);
		assertEquals(AppointmentBookingRequirementsDestinationId.SCREENING_SESSION,
				screeningRequirement.getAppointmentBookingRequirementsDestinationId());
		assertEquals(screeningFlowId, screeningRequirement.getScreeningFlowId());
		assertEquals(true, screeningRequirement.getScreeningRequired());
		assertEquals(false, screeningRequirement.getScreeningSatisfied());
	}

	@Test
	public void providerAvailabilityScreeningRequirementIsNullForAmbiguousFirstAvailableAppointmentWithDifferentFlows() {
		UUID providerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID firstAppointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000011");
		UUID secondAppointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000012");
		LocalDate date = LocalDate.of(2026, 1, 1);
		Provider provider = provider(providerId, "Test Provider", VideoconferencePlatformId.SWITCHBOARD, null);
		AppointmentType firstAppointmentType = appointmentType(firstAppointmentTypeId, "Alpha Visit", "Alpha appointment description",
				UUID.fromString("00000000-0000-0000-0000-000000000021"));
		AppointmentType secondAppointmentType = appointmentType(secondAppointmentTypeId, "Beta Visit", "Beta appointment description",
				UUID.fromString("00000000-0000-0000-0000-000000000022"));
		ProviderFind providerFind = providerFind(providerId, "Test Provider", availabilityDate(date, List.of(
				availabilityTime(LocalTime.of(9, 0), AvailabilityStatus.AVAILABLE, List.of(firstAppointmentTypeId, secondAppointmentTypeId))
		)));

		ProviderAvailabilityApiResponse response = new ProviderAvailabilityApiResponse(currentContextProvider(), formatter(), provider,
				List.of(providerFind), Map.of(firstAppointmentTypeId, firstAppointmentType, secondAppointmentTypeId, secondAppointmentType),
				date, date.plusDays(90), Set.of());

		assertNull(response.getScreeningRequirement());
	}

	@Nonnull
	protected javax.inject.Provider<CurrentContext> currentContextProvider() {
		return () -> new CurrentContext.Builder(InstitutionId.COBALT, Locale.US, ZoneId.of("America/New_York")).build();
	}

	@Nonnull
	protected Provider provider(@Nonnull UUID providerId,
															@Nonnull String name) {
		return provider(providerId, name, null, null);
	}

	@Nonnull
	protected Provider provider(@Nonnull UUID providerId,
															@Nonnull String name,
															VideoconferencePlatformId videoconferencePlatformId,
															String phoneNumber) {
		Provider provider = new Provider();
		provider.setProviderId(providerId);
		provider.setInstitutionId(InstitutionId.COBALT);
		provider.setName(name);
		provider.setActive(true);
		provider.setVideoconferencePlatformId(videoconferencePlatformId);
		provider.setPhoneNumber(phoneNumber);
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
																						@Nonnull String name) {
		return appointmentType(appointmentTypeId, name, name);
	}

	@Nonnull
	protected AppointmentType appointmentType(@Nonnull UUID appointmentTypeId,
																						@Nonnull String name,
																						String description) {
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
	protected AppointmentType appointmentType(@Nonnull UUID appointmentTypeId,
																						@Nonnull String name,
																						String description,
																						UUID screeningFlowId) {
		AppointmentType appointmentType = appointmentType(appointmentTypeId, name, description);
		appointmentType.setScreeningFlowId(screeningFlowId);
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

	@Nonnull
	protected Formatter formatter() {
		Cache cache = cache();

		return new Formatter(cache,
				currentContextProvider(),
				() -> cache,
				strings());
	}

	@Nonnull
	protected Strings strings() {
		return (Strings) Proxy.newProxyInstance(Strings.class.getClassLoader(), new Class[]{Strings.class},
				(proxy, method, args) -> {
					if (method.getName().equals("get") && args != null && args.length > 0)
						return args[0];

					if (method.getName().equals("toString"))
						return "TestStrings";

					return null;
				});
	}

	@Nonnull
	protected Cache cache() {
		return new Cache() {
			@Nonnull
			@Override
			public <T> Optional<T> get(@Nonnull String key,
																 @Nonnull Class<T> type) {
				return Optional.empty();
			}

			@Nonnull
			@Override
			public <T> Optional<List<T>> getList(@Nonnull String key,
																					 @Nonnull Class<T> type) {
				return Optional.empty();
			}

			@Nonnull
			@Override
			public <T> T get(@Nonnull String key,
											 @Nonnull Supplier<T> supplier,
											 @Nonnull Class<T> type) {
				return supplier.get();
			}

			@Nonnull
			@Override
			public <T> List<T> getList(@Nonnull String key,
																 @Nonnull Supplier<List<T>> supplier,
																 @Nonnull Class<T> type) {
				return supplier.get();
			}

			@Override
			public void put(@Nonnull String key,
											@Nonnull Object value) {
			}

			@Override
			public void invalidate(@Nonnull String key) {
			}

			@Override
			public void invalidateAll() {
			}

			@Nonnull
			@Override
			public Set<String> getKeys() {
				return Collections.emptySet();
			}
		};
	}
}
