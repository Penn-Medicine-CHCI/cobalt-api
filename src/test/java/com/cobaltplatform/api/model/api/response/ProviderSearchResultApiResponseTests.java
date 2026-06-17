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
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityId;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentSelectionTypeId;
import com.cobaltplatform.api.model.db.AppointmentBookingLevel.AppointmentBookingLevelId;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.Clinic;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.VideoconferencePlatform.VideoconferencePlatformId;
import com.cobaltplatform.api.model.service.AppointmentBookingRequirements.AppointmentBookingRequirementsDestinationId;
import com.cobaltplatform.api.model.service.AppointmentBookingScreeningKey;
import com.cobaltplatform.api.model.service.ProviderFind;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityDate;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityStatus;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityTime;
import com.cobaltplatform.api.model.service.ProviderSearchResult;
import com.cobaltplatform.api.model.service.ProviderSearchScreeningRequirement;
import com.cobaltplatform.api.util.Formatter;
import com.lokalized.Strings;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ProviderSearchResultApiResponseTests {
	@Test
	public void responseExposesProviderBookingLevelForProvider() {
		UUID providerId = UUID.randomUUID();
		Provider provider = provider(providerId, null);
		ProviderFind providerFind = providerFind(providerId, null);

		ProviderSearchResultApiResponse response = new ProviderSearchResultApiResponse(formatter(), strings(), provider,
				providerFind, Map.of());

		assertEquals(AppointmentBookingLevelId.PROVIDER, response.getAppointmentBookingLevelId());
	}

	@Test
	public void responseExposesClinicBookingLevelForClinic() {
		UUID providerId = UUID.randomUUID();
		UUID clinicId = UUID.randomUUID();
		Provider provider = provider(providerId, null);
		ProviderFind providerFind = providerFind(providerId, null);
		Clinic clinic = clinic(clinicId, AppointmentBookingLevelId.CLINIC);

		ProviderSearchResultApiResponse response = new ProviderSearchResultApiResponse(formatter(), strings(), clinic,
				List.of(providerFind), Map.of(providerId, provider), Map.of());

		assertEquals(AppointmentBookingLevelId.CLINIC, response.getAppointmentBookingLevelId());
	}

	@Test
	public void responseDoesNotExposeVirtualModalityForUnsupportedVideoconferencePlatform() {
		UUID providerId = UUID.randomUUID();
		UUID appointmentTypeId = UUID.randomUUID();
		Provider provider = provider(providerId, VideoconferencePlatformId.BLUEJEANS);
		provider.setPhoneNumber("+12155551000");
		ProviderFind providerFind = providerFindWithAvailableAppointment(providerId, Set.of(appointmentTypeId), List.of(appointmentTypeId));
		ProviderSearchResult providerSearchResult = ProviderSearchResult.forProvider(provider, providerFind,
				Map.of(appointmentTypeId, appointmentType(appointmentTypeId, null)), Set.of());

		ProviderSearchResultApiResponse response = new ProviderSearchResultApiResponse(formatter(), strings(), providerSearchResult);

		assertEquals(1, response.getSupportedAppointmentModalities().size());
		assertEquals(ProviderAppointmentModalityId.PHONE, response.getSupportedAppointmentModalities().get(0).getAppointmentModalityId());
	}

	@Test
	public void providerSupportedAppointmentModalitiesIgnoreMissingAvailability() {
		UUID providerId = UUID.randomUUID();
		Provider provider = provider(providerId, VideoconferencePlatformId.SWITCHBOARD);
		provider.setPhoneNumber("+12155551000");
		ProviderFind providerFind = providerFind(providerId, Set.of(UUID.randomUUID()));
		ProviderSearchResult providerSearchResult = ProviderSearchResult.forProvider(provider, providerFind, Map.of(), Set.of());

		ProviderSearchResultApiResponse response = new ProviderSearchResultApiResponse(formatter(), strings(), providerSearchResult);

		assertSupportedAppointmentModalities(response, List.of(
				ProviderAppointmentModalityId.PHONE,
				ProviderAppointmentModalityId.VIRTUAL));
	}

	@Test
	public void providerSupportedAppointmentModalitiesIgnoreBookedAndUnknownSlotAvailability() {
		UUID providerId = UUID.randomUUID();
		UUID appointmentTypeId = UUID.randomUUID();
		UUID unknownAppointmentTypeId = UUID.randomUUID();
		Provider provider = provider(providerId, VideoconferencePlatformId.SWITCHBOARD);
		provider.setPhoneNumber("+12155551000");
		ProviderFind providerFind = providerFind(providerId, Set.of(appointmentTypeId));
		AvailabilityDate availabilityDate = new AvailabilityDate();
		AvailabilityTime bookedAvailabilityTime = new AvailabilityTime();
		AvailabilityTime unknownAppointmentTypeAvailabilityTime = new AvailabilityTime();

		availabilityDate.setDate(LocalDate.of(2026, 1, 1));
		bookedAvailabilityTime.setTime(LocalTime.NOON);
		bookedAvailabilityTime.setStatus(AvailabilityStatus.BOOKED);
		bookedAvailabilityTime.setAppointmentTypeIds(List.of(appointmentTypeId));
		unknownAppointmentTypeAvailabilityTime.setTime(LocalTime.of(13, 0));
		unknownAppointmentTypeAvailabilityTime.setStatus(AvailabilityStatus.AVAILABLE);
		unknownAppointmentTypeAvailabilityTime.setAppointmentTypeIds(List.of(unknownAppointmentTypeId));
		availabilityDate.setTimes(List.of(bookedAvailabilityTime, unknownAppointmentTypeAvailabilityTime));
		providerFind.setDates(List.of(availabilityDate));

		ProviderSearchResult providerSearchResult = ProviderSearchResult.forProvider(provider, providerFind,
				Map.of(appointmentTypeId, appointmentType(appointmentTypeId, null)), Set.of());

		ProviderSearchResultApiResponse response = new ProviderSearchResultApiResponse(formatter(), strings(), providerSearchResult);

		assertSupportedAppointmentModalities(response, List.of(
				ProviderAppointmentModalityId.PHONE,
				ProviderAppointmentModalityId.VIRTUAL));
	}

	@Test
	public void clinicSupportedAppointmentModalitiesUnionStaticProviderSupportWithoutAvailability() {
		UUID phoneProviderId = UUID.randomUUID();
		UUID virtualProviderId = UUID.randomUUID();
		UUID clinicId = UUID.randomUUID();
		Provider phoneProvider = provider(phoneProviderId, VideoconferencePlatformId.TELEPHONE);
		Provider virtualProvider = provider(virtualProviderId, VideoconferencePlatformId.SWITCHBOARD);
		ProviderFind phoneProviderFind = providerFind(phoneProviderId, Set.of(UUID.randomUUID()));
		ProviderFind virtualProviderFind = providerFind(virtualProviderId, Set.of(UUID.randomUUID()));
		ProviderSearchResult providerSearchResult = ProviderSearchResult.forClinic(clinic(clinicId, AppointmentBookingLevelId.CLINIC),
				List.of(phoneProviderFind, virtualProviderFind),
				Map.of(phoneProviderId, phoneProvider, virtualProviderId, virtualProvider),
				Map.of(), Set.of());

		ProviderSearchResultApiResponse response = new ProviderSearchResultApiResponse(formatter(), strings(), providerSearchResult);

		assertSupportedAppointmentModalities(response, List.of(
				ProviderAppointmentModalityId.PHONE,
				ProviderAppointmentModalityId.VIRTUAL));
	}

	@Test
	public void appointmentSelectionTypeUsesKnownSlotAppointmentTypeIdsFirst() {
		UUID providerId = UUID.randomUUID();
		UUID appointmentTypeId = UUID.randomUUID();
		UUID otherAppointmentTypeId = UUID.randomUUID();
		Provider provider = provider(providerId, VideoconferencePlatformId.SWITCHBOARD);
		ProviderFind providerFind = providerFind(providerId, Set.of(appointmentTypeId, otherAppointmentTypeId));

		ProviderAppointmentSelectionTypeId appointmentSelectionTypeId =
				ProviderSearchResultApiResponse.appointmentSelectionTypeIdFor(List.of(providerFind),
						Map.of(providerId, provider), List.of(availableAppointment(provider, List.of(appointmentTypeId))), Map.of());

		assertEquals(ProviderAppointmentSelectionTypeId.APPOINTMENT_PREDETERMINED, appointmentSelectionTypeId);
	}

	@Test
	public void appointmentSelectionTypeAggregatesClinicSlotAppointmentTypeIds() {
		UUID firstProviderId = UUID.randomUUID();
		UUID secondProviderId = UUID.randomUUID();
		UUID appointmentTypeId = UUID.randomUUID();
		UUID otherAppointmentTypeId = UUID.randomUUID();
		UUID screeningFlowId = UUID.randomUUID();
		Provider firstProvider = provider(firstProviderId, VideoconferencePlatformId.SWITCHBOARD);
		Provider secondProvider = provider(secondProviderId, VideoconferencePlatformId.SWITCHBOARD);
		List<ProviderFind> providerFinds = List.of(
				providerFind(firstProviderId, Set.of(appointmentTypeId)),
				providerFind(secondProviderId, Set.of(appointmentTypeId)));
		Map<UUID, Provider> providersById = Map.of(firstProviderId, firstProvider, secondProviderId, secondProvider);
		Map<UUID, AppointmentType> appointmentTypesById = Map.of(
				appointmentTypeId, appointmentType(appointmentTypeId, screeningFlowId),
				otherAppointmentTypeId, appointmentType(otherAppointmentTypeId, screeningFlowId));

		ProviderAppointmentSelectionTypeId predeterminedSelectionTypeId =
				ProviderSearchResultApiResponse.appointmentSelectionTypeIdFor(providerFinds, providersById, List.of(
						availableAppointment(firstProvider, List.of(appointmentTypeId)),
						availableAppointment(secondProvider, List.of(appointmentTypeId))), appointmentTypesById);
		ProviderAppointmentSelectionTypeId undeterminedSelectionTypeId =
				ProviderSearchResultApiResponse.appointmentSelectionTypeIdFor(providerFinds, providersById, List.of(
						availableAppointment(firstProvider, List.of(appointmentTypeId)),
						availableAppointment(secondProvider, List.of(otherAppointmentTypeId))), appointmentTypesById);

		assertEquals(ProviderAppointmentSelectionTypeId.APPOINTMENT_PREDETERMINED, predeterminedSelectionTypeId);
		assertEquals(ProviderAppointmentSelectionTypeId.APPOINTMENT_UNDETERMINED, undeterminedSelectionTypeId);
	}

	@Test
	public void appointmentSelectionTypeFallsBackToProviderAppointmentTypeIdsWhenSlotsAreIncomplete() {
		UUID providerId = UUID.randomUUID();
		UUID appointmentTypeId = UUID.randomUUID();
		Provider provider = provider(providerId, VideoconferencePlatformId.SWITCHBOARD);
		ProviderFind providerFind = providerFind(providerId, Set.of(appointmentTypeId));

		ProviderAppointmentSelectionTypeId appointmentSelectionTypeId =
				ProviderSearchResultApiResponse.appointmentSelectionTypeIdFor(List.of(providerFind),
						Map.of(providerId, provider), List.of(availableAppointment(provider, null)), Map.of());

		assertEquals(ProviderAppointmentSelectionTypeId.APPOINTMENT_PREDETERMINED, appointmentSelectionTypeId);
	}

	@Test
	public void appointmentSelectionTypeUsesPhoneWhenNoFutureAvailabilityCanBeDetermined() {
		UUID providerId = UUID.randomUUID();
		UUID appointmentTypeId = UUID.randomUUID();
		Provider provider = provider(providerId, VideoconferencePlatformId.SWITCHBOARD);
		ProviderFind providerFind = providerFind(providerId, Set.of(appointmentTypeId));

		ProviderAppointmentSelectionTypeId appointmentSelectionTypeId =
				ProviderSearchResultApiResponse.appointmentSelectionTypeIdFor(List.of(providerFind),
						Map.of(providerId, provider), List.of(), Map.of());

		assertEquals(ProviderAppointmentSelectionTypeId.APPOINTMENT_BY_PHONE, appointmentSelectionTypeId);
	}

	@Test
	public void appointmentSelectionTypeOnlyUsesByPhoneWhenEveryProviderIsPhoneOnly() {
		UUID firstProviderId = UUID.randomUUID();
		UUID secondProviderId = UUID.randomUUID();
		UUID appointmentTypeId = UUID.randomUUID();
		Provider firstProvider = provider(firstProviderId, VideoconferencePlatformId.TELEPHONE);
		Provider secondProvider = provider(secondProviderId, VideoconferencePlatformId.TELEPHONE);
		List<ProviderFind> providerFinds = List.of(
				providerFind(firstProviderId, Set.of(appointmentTypeId)),
				providerFind(secondProviderId, Set.of(appointmentTypeId)));

		ProviderAppointmentSelectionTypeId appointmentSelectionTypeId =
				ProviderSearchResultApiResponse.appointmentSelectionTypeIdFor(providerFinds,
						Map.of(firstProviderId, firstProvider, secondProviderId, secondProvider), List.of(), Map.of());

		assertEquals(ProviderAppointmentSelectionTypeId.APPOINTMENT_BY_PHONE, appointmentSelectionTypeId);
	}

	@Test
	public void screeningRequirementReflectsUnsatisfiedRequiredFirstAvailableAppointment() {
		UUID providerId = UUID.randomUUID();
		UUID appointmentTypeId = UUID.randomUUID();
		UUID screeningFlowId = UUID.randomUUID();
		Provider provider = provider(providerId, VideoconferencePlatformId.SWITCHBOARD);
		ProviderFind providerFind = providerFindWithAvailableAppointment(providerId, Set.of(appointmentTypeId), List.of(appointmentTypeId));
		ProviderSearchResult providerSearchResult = ProviderSearchResult.forProvider(provider, providerFind,
				Map.of(appointmentTypeId, appointmentType(appointmentTypeId, screeningFlowId)), Set.of());

		ProviderSearchResultApiResponse response = new ProviderSearchResultApiResponse(formatter(), strings(), providerSearchResult);
		ProviderSearchScreeningRequirement screeningRequirement = response.getScreeningRequirement();

		assertNotNull(screeningRequirement);
		assertEquals(AppointmentBookingRequirementsDestinationId.SCREENING_SESSION,
				screeningRequirement.getAppointmentBookingRequirementsDestinationId());
		assertEquals(screeningFlowId, screeningRequirement.getScreeningFlowId());
		assertEquals(true, screeningRequirement.getScreeningRequired());
		assertEquals(false, screeningRequirement.getScreeningSatisfied());
	}

	@Test
	public void screeningRequirementReflectsSatisfiedRequiredFirstAvailableAppointment() {
		UUID providerId = UUID.randomUUID();
		UUID appointmentTypeId = UUID.randomUUID();
		UUID screeningFlowId = UUID.randomUUID();
		Provider provider = provider(providerId, VideoconferencePlatformId.SWITCHBOARD);
		ProviderFind providerFind = providerFindWithAvailableAppointment(providerId, Set.of(appointmentTypeId), List.of(appointmentTypeId));
		ProviderSearchResult providerSearchResult = ProviderSearchResult.forProvider(provider, providerFind,
				Map.of(appointmentTypeId, appointmentType(appointmentTypeId, screeningFlowId)),
				Set.of(new AppointmentBookingScreeningKey(providerId, appointmentTypeId, screeningFlowId)));

		ProviderSearchResultApiResponse response = new ProviderSearchResultApiResponse(formatter(), strings(), providerSearchResult);
		ProviderSearchScreeningRequirement screeningRequirement = response.getScreeningRequirement();

		assertNotNull(screeningRequirement);
		assertEquals(AppointmentBookingRequirementsDestinationId.APPOINTMENT_BOOKING,
				screeningRequirement.getAppointmentBookingRequirementsDestinationId());
		assertEquals(screeningFlowId, screeningRequirement.getScreeningFlowId());
		assertEquals(true, screeningRequirement.getScreeningRequired());
		assertEquals(true, screeningRequirement.getScreeningSatisfied());
	}

	@Test
	public void screeningRequirementReflectsNoRequiredScreeningForFirstAvailableAppointment() {
		UUID providerId = UUID.randomUUID();
		UUID appointmentTypeId = UUID.randomUUID();
		Provider provider = provider(providerId, VideoconferencePlatformId.SWITCHBOARD);
		ProviderFind providerFind = providerFindWithAvailableAppointment(providerId, Set.of(appointmentTypeId), List.of(appointmentTypeId));
		ProviderSearchResult providerSearchResult = ProviderSearchResult.forProvider(provider, providerFind,
				Map.of(appointmentTypeId, appointmentType(appointmentTypeId, null)), Set.of());

		ProviderSearchResultApiResponse response = new ProviderSearchResultApiResponse(formatter(), strings(), providerSearchResult);
		ProviderSearchScreeningRequirement screeningRequirement = response.getScreeningRequirement();

		assertNotNull(screeningRequirement);
		assertEquals(AppointmentBookingRequirementsDestinationId.APPOINTMENT_BOOKING,
				screeningRequirement.getAppointmentBookingRequirementsDestinationId());
		assertNull(screeningRequirement.getScreeningFlowId());
		assertEquals(false, screeningRequirement.getScreeningRequired());
		assertEquals(true, screeningRequirement.getScreeningSatisfied());
	}

	@Test
	public void screeningRequirementIsNullForAmbiguousFirstAvailableAppointmentWithDifferentFlows() {
		UUID providerId = UUID.randomUUID();
		UUID appointmentTypeId = UUID.randomUUID();
		UUID otherAppointmentTypeId = UUID.randomUUID();
		Provider provider = provider(providerId, VideoconferencePlatformId.SWITCHBOARD);
		ProviderFind providerFind = providerFindWithAvailableAppointment(providerId, Set.of(appointmentTypeId, otherAppointmentTypeId),
				List.of(appointmentTypeId, otherAppointmentTypeId));
		ProviderSearchResult providerSearchResult = ProviderSearchResult.forProvider(provider, providerFind,
				Map.of(
						appointmentTypeId, appointmentType(appointmentTypeId, UUID.randomUUID()),
						otherAppointmentTypeId, appointmentType(otherAppointmentTypeId, UUID.randomUUID())),
				Set.of());

		ProviderSearchResultApiResponse response = new ProviderSearchResultApiResponse(formatter(), strings(), providerSearchResult);

		assertEquals(ProviderAppointmentSelectionTypeId.APPOINTMENT_BY_PHONE, response.getAppointmentSelectionTypeId());
		assertNull(response.getScreeningRequirement());
	}

	@Test
	public void screeningRequirementIsNullForAmbiguousFirstAvailableAppointmentWithUnknownAppointmentType() {
		UUID providerId = UUID.randomUUID();
		UUID appointmentTypeId = UUID.randomUUID();
		UUID unknownAppointmentTypeId = UUID.randomUUID();
		UUID screeningFlowId = UUID.randomUUID();
		Provider provider = provider(providerId, VideoconferencePlatformId.SWITCHBOARD);
		ProviderFind providerFind = providerFindWithAvailableAppointment(providerId, Set.of(appointmentTypeId, unknownAppointmentTypeId),
				List.of(appointmentTypeId, unknownAppointmentTypeId));
		ProviderSearchResult providerSearchResult = ProviderSearchResult.forProvider(provider, providerFind,
				Map.of(appointmentTypeId, appointmentType(appointmentTypeId, screeningFlowId)), Set.of());

		ProviderSearchResultApiResponse response = new ProviderSearchResultApiResponse(formatter(), strings(), providerSearchResult);

		assertEquals(ProviderAppointmentSelectionTypeId.APPOINTMENT_BY_PHONE, response.getAppointmentSelectionTypeId());
		assertNull(response.getScreeningRequirement());
	}

	@Test
	public void screeningRequirementReflectsSharedFlowAmbiguousFirstAvailableAppointment() {
		UUID providerId = UUID.randomUUID();
		UUID appointmentTypeId = UUID.randomUUID();
		UUID otherAppointmentTypeId = UUID.randomUUID();
		UUID screeningFlowId = UUID.randomUUID();
		Provider provider = provider(providerId, VideoconferencePlatformId.SWITCHBOARD);
		ProviderFind providerFind = providerFindWithAvailableAppointment(providerId, Set.of(appointmentTypeId, otherAppointmentTypeId),
				List.of(appointmentTypeId, otherAppointmentTypeId));
		Map<UUID, AppointmentType> appointmentTypesById = Map.of(
				appointmentTypeId, appointmentType(appointmentTypeId, screeningFlowId),
				otherAppointmentTypeId, appointmentType(otherAppointmentTypeId, screeningFlowId));
		ProviderSearchResult unsatisfiedProviderSearchResult = ProviderSearchResult.forProvider(provider, providerFind,
				appointmentTypesById, Set.of());
		ProviderSearchResult satisfiedProviderSearchResult = ProviderSearchResult.forProvider(provider, providerFind,
				appointmentTypesById, Set.of(new AppointmentBookingScreeningKey(providerId, otherAppointmentTypeId, screeningFlowId)));

		ProviderSearchResultApiResponse unsatisfiedResponse = new ProviderSearchResultApiResponse(formatter(), strings(),
				unsatisfiedProviderSearchResult);
		ProviderSearchResultApiResponse satisfiedResponse = new ProviderSearchResultApiResponse(formatter(), strings(),
				satisfiedProviderSearchResult);

		assertEquals(ProviderAppointmentSelectionTypeId.APPOINTMENT_UNDETERMINED, unsatisfiedResponse.getAppointmentSelectionTypeId());
		assertNotNull(unsatisfiedResponse.getFirstAvailableAppointment());
		assertNull(unsatisfiedResponse.getFirstAvailableAppointment().getAppointmentTypeId());
		assertEquals(Set.of(appointmentTypeId, otherAppointmentTypeId),
				Set.copyOf(unsatisfiedResponse.getFirstAvailableAppointment().getAppointmentTypeIds()));
		assertNotNull(unsatisfiedResponse.getScreeningRequirement());
		assertEquals(AppointmentBookingRequirementsDestinationId.SCREENING_SESSION,
				unsatisfiedResponse.getScreeningRequirement().getAppointmentBookingRequirementsDestinationId());
		assertEquals(screeningFlowId, unsatisfiedResponse.getScreeningRequirement().getScreeningFlowId());
		assertEquals(true, unsatisfiedResponse.getScreeningRequirement().getScreeningRequired());
		assertEquals(false, unsatisfiedResponse.getScreeningRequirement().getScreeningSatisfied());

		assertNotNull(satisfiedResponse.getScreeningRequirement());
		assertEquals(ProviderAppointmentSelectionTypeId.APPOINTMENT_PREDETERMINED, satisfiedResponse.getAppointmentSelectionTypeId());
		assertNotNull(satisfiedResponse.getFirstAvailableAppointment());
		assertNull(satisfiedResponse.getFirstAvailableAppointment().getAppointmentTypeId());
		assertEquals(Set.of(appointmentTypeId, otherAppointmentTypeId),
				Set.copyOf(satisfiedResponse.getFirstAvailableAppointment().getAppointmentTypeIds()));
		assertEquals(AppointmentBookingRequirementsDestinationId.APPOINTMENT_BOOKING,
				satisfiedResponse.getScreeningRequirement().getAppointmentBookingRequirementsDestinationId());
		assertEquals(true, satisfiedResponse.getScreeningRequirement().getScreeningSatisfied());
	}

	@Test
	public void appointmentSelectionTypeDoesNotUseCompletedScreeningForOtherProviderOrFlow() {
		UUID providerId = UUID.randomUUID();
		UUID otherProviderId = UUID.randomUUID();
		UUID appointmentTypeId = UUID.randomUUID();
		UUID otherAppointmentTypeId = UUID.randomUUID();
		UUID screeningFlowId = UUID.randomUUID();
		UUID otherScreeningFlowId = UUID.randomUUID();
		Provider provider = provider(providerId, VideoconferencePlatformId.SWITCHBOARD);
		ProviderFind providerFind = providerFindWithAvailableAppointment(providerId, Set.of(appointmentTypeId, otherAppointmentTypeId),
				List.of(appointmentTypeId, otherAppointmentTypeId));
		Map<UUID, AppointmentType> appointmentTypesById = Map.of(
				appointmentTypeId, appointmentType(appointmentTypeId, screeningFlowId),
				otherAppointmentTypeId, appointmentType(otherAppointmentTypeId, screeningFlowId));
		ProviderSearchResult otherProviderScreeningResult = ProviderSearchResult.forProvider(provider, providerFind,
				appointmentTypesById, Set.of(new AppointmentBookingScreeningKey(otherProviderId, appointmentTypeId, screeningFlowId)));
		ProviderSearchResult otherFlowScreeningResult = ProviderSearchResult.forProvider(provider, providerFind,
				appointmentTypesById, Set.of(new AppointmentBookingScreeningKey(providerId, appointmentTypeId, otherScreeningFlowId)));

		ProviderSearchResultApiResponse otherProviderResponse = new ProviderSearchResultApiResponse(formatter(), strings(),
				otherProviderScreeningResult);
		ProviderSearchResultApiResponse otherFlowResponse = new ProviderSearchResultApiResponse(formatter(), strings(),
				otherFlowScreeningResult);

		assertEquals(ProviderAppointmentSelectionTypeId.APPOINTMENT_UNDETERMINED,
				otherProviderResponse.getAppointmentSelectionTypeId());
		assertEquals(ProviderAppointmentSelectionTypeId.APPOINTMENT_UNDETERMINED,
				otherFlowResponse.getAppointmentSelectionTypeId());
		assertNotNull(otherProviderResponse.getScreeningRequirement());
		assertEquals(false, otherProviderResponse.getScreeningRequirement().getScreeningSatisfied());
		assertNotNull(otherFlowResponse.getScreeningRequirement());
		assertEquals(false, otherFlowResponse.getScreeningRequirement().getScreeningSatisfied());
	}

	@Test
	public void clinicAppointmentSelectionTypeUsesCompletedSharedFlowScreeningForFirstAvailableProvider() {
		UUID providerId = UUID.randomUUID();
		UUID clinicId = UUID.randomUUID();
		UUID appointmentTypeId = UUID.randomUUID();
		UUID otherAppointmentTypeId = UUID.randomUUID();
		UUID screeningFlowId = UUID.randomUUID();
		Provider provider = provider(providerId, VideoconferencePlatformId.SWITCHBOARD);
		ProviderFind providerFind = providerFindWithAvailableAppointment(providerId, Set.of(appointmentTypeId, otherAppointmentTypeId),
				List.of(appointmentTypeId, otherAppointmentTypeId));
		Map<UUID, AppointmentType> appointmentTypesById = Map.of(
				appointmentTypeId, appointmentType(appointmentTypeId, screeningFlowId),
				otherAppointmentTypeId, appointmentType(otherAppointmentTypeId, screeningFlowId));
		ProviderSearchResult providerSearchResult = ProviderSearchResult.forClinic(clinic(clinicId, AppointmentBookingLevelId.CLINIC),
				List.of(providerFind), Map.of(providerId, provider), appointmentTypesById,
				Set.of(new AppointmentBookingScreeningKey(providerId, otherAppointmentTypeId, screeningFlowId)));

		ProviderSearchResultApiResponse response = new ProviderSearchResultApiResponse(formatter(), strings(), providerSearchResult);

		assertEquals(ProviderAppointmentSelectionTypeId.APPOINTMENT_PREDETERMINED, response.getAppointmentSelectionTypeId());
		assertNotNull(response.getFirstAvailableAppointment());
		assertNull(response.getFirstAvailableAppointment().getAppointmentTypeId());
		assertEquals(Set.of(appointmentTypeId, otherAppointmentTypeId),
				Set.copyOf(response.getFirstAvailableAppointment().getAppointmentTypeIds()));
		assertNotNull(response.getScreeningRequirement());
		assertEquals(AppointmentBookingRequirementsDestinationId.APPOINTMENT_BOOKING,
				response.getScreeningRequirement().getAppointmentBookingRequirementsDestinationId());
		assertEquals(true, response.getScreeningRequirement().getScreeningSatisfied());
	}

	@Test
	public void clinicFirstAvailableAppointmentExposesProviderIdAndScreeningRequirementUsesThatProvider() {
		UUID providerId = UUID.randomUUID();
		UUID clinicId = UUID.randomUUID();
		UUID appointmentTypeId = UUID.randomUUID();
		UUID screeningFlowId = UUID.randomUUID();
		Provider provider = provider(providerId, VideoconferencePlatformId.SWITCHBOARD);
		ProviderFind providerFind = providerFindWithAvailableAppointment(providerId, Set.of(appointmentTypeId), List.of(appointmentTypeId));
		ProviderSearchResult providerSearchResult = ProviderSearchResult.forClinic(clinic(clinicId, AppointmentBookingLevelId.CLINIC),
				List.of(providerFind), Map.of(providerId, provider), Map.of(appointmentTypeId, appointmentType(appointmentTypeId, screeningFlowId)),
				Set.of(new AppointmentBookingScreeningKey(providerId, appointmentTypeId, screeningFlowId)));

		ProviderSearchResultApiResponse response = new ProviderSearchResultApiResponse(formatter(), strings(), providerSearchResult);

		assertNotNull(response.getFirstAvailableAppointment());
		assertEquals(providerId, response.getFirstAvailableAppointment().getProviderId());
		assertNotNull(response.getScreeningRequirement());
		assertEquals(AppointmentBookingRequirementsDestinationId.APPOINTMENT_BOOKING,
				response.getScreeningRequirement().getAppointmentBookingRequirementsDestinationId());
		assertEquals(true, response.getScreeningRequirement().getScreeningSatisfied());
	}

	@Test
	public void screeningRequirementDoesNotExposeAppointmentTypeAccessors() {
		assertFalse(Arrays.stream(ProviderSearchScreeningRequirement.class.getMethods())
				.anyMatch(method -> method.getName().equals("getAppointmentTypeId")));
		assertFalse(Arrays.stream(ProviderSearchScreeningRequirement.class.getMethods())
				.anyMatch(method -> method.getName().equals("getAppointmentTypeName")));
		assertFalse(Arrays.stream(ProviderSearchScreeningRequirement.class.getMethods())
				.anyMatch(method -> method.getName().equals("getAppointmentDescription")));
	}

	protected void assertSupportedAppointmentModalities(@Nonnull ProviderSearchResultApiResponse response,
																											@Nonnull List<ProviderAppointmentModalityId> expectedAppointmentModalityIds) {
		requireNonNull(response);
		requireNonNull(expectedAppointmentModalityIds);

		assertEquals(expectedAppointmentModalityIds.size(), response.getSupportedAppointmentModalities().size());

		for (int i = 0; i < expectedAppointmentModalityIds.size(); ++i)
			assertEquals(expectedAppointmentModalityIds.get(i), response.getSupportedAppointmentModalities().get(i).getAppointmentModalityId());
	}

	@Nonnull
	protected Provider provider(@Nonnull UUID providerId,
															@Nullable VideoconferencePlatformId videoconferencePlatformId) {
		Provider provider = new Provider();
		provider.setProviderId(providerId);
		provider.setInstitutionId(InstitutionId.COBALT);
		provider.setLocale(Locale.US);
		provider.setVideoconferencePlatformId(videoconferencePlatformId);
		return provider;
	}

	@Nonnull
	protected Clinic clinic(@Nonnull UUID clinicId,
													@Nonnull AppointmentBookingLevelId appointmentBookingLevelId) {
		Clinic clinic = new Clinic();
		clinic.setClinicId(clinicId);
		clinic.setInstitutionId(InstitutionId.COBALT);
		clinic.setDescription("Clinic");
		clinic.setLocale(Locale.US);
		clinic.setAppointmentBookingLevelId(appointmentBookingLevelId);
		return clinic;
	}

	@Nonnull
	protected ProviderFind providerFind(@Nonnull UUID providerId,
																			@Nullable Set<UUID> appointmentTypeIds) {
		ProviderFind providerFind = new ProviderFind();
		providerFind.setProviderId(providerId);
		providerFind.setAppointmentTypeIds(appointmentTypeIds);
		return providerFind;
	}

	@Nonnull
	protected ProviderFind providerFindWithAvailableAppointment(@Nonnull UUID providerId,
																															@Nullable Set<UUID> appointmentTypeIds,
																															@Nullable List<UUID> availableAppointmentTypeIds) {
		ProviderFind providerFind = providerFind(providerId, appointmentTypeIds);
		AvailabilityDate availabilityDate = new AvailabilityDate();
		AvailabilityTime availabilityTime = new AvailabilityTime();

		availabilityDate.setDate(LocalDate.of(2026, 1, 1));
		availabilityTime.setTime(LocalTime.NOON);
		availabilityTime.setStatus(AvailabilityStatus.AVAILABLE);
		availabilityTime.setAppointmentTypeIds(availableAppointmentTypeIds);
		availabilityDate.setTimes(List.of(availabilityTime));
		providerFind.setDates(List.of(availabilityDate));

		return providerFind;
	}

	@Nonnull
	protected AppointmentType appointmentType(@Nonnull UUID appointmentTypeId,
																						@Nullable UUID screeningFlowId) {
		AppointmentType appointmentType = new AppointmentType();
		appointmentType.setAppointmentTypeId(appointmentTypeId);
		appointmentType.setName("Visit");
		appointmentType.setScreeningFlowId(screeningFlowId);

		return appointmentType;
	}

	@Nonnull
	protected ProviderSearchResultApiResponse.AvailableAppointment availableAppointment(@Nonnull Provider provider,
																																										 @Nullable List<UUID> appointmentTypeIds) {
		AvailabilityTime availabilityTime = new AvailabilityTime();
		availabilityTime.setTime(LocalTime.NOON);
		availabilityTime.setStatus(AvailabilityStatus.AVAILABLE);
		availabilityTime.setAppointmentTypeIds(appointmentTypeIds);

		return new ProviderSearchResultApiResponse.AvailableAppointment(provider, LocalDate.of(2026, 1, 1), availabilityTime, null);
	}

	@Nonnull
	protected Formatter formatter() {
		Cache cache = cache();

		return new Formatter(cache,
				() -> new CurrentContext.Builder(InstitutionId.COBALT, Locale.US, ZoneId.of("America/New_York")).build(),
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
				return Set.of();
			}
		};
	}
}
