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

import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentSelectionTypeId;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.VideoconferencePlatform.VideoconferencePlatformId;
import com.cobaltplatform.api.model.service.ProviderFind;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityStatus;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityTime;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ProviderSearchResultApiResponseTests {
	@Test
	public void appointmentSelectionTypeUsesKnownSlotAppointmentTypeIdsFirst() {
		UUID providerId = UUID.randomUUID();
		UUID appointmentTypeId = UUID.randomUUID();
		UUID otherAppointmentTypeId = UUID.randomUUID();
		Provider provider = provider(providerId, VideoconferencePlatformId.SWITCHBOARD);
		ProviderFind providerFind = providerFind(providerId, Set.of(appointmentTypeId, otherAppointmentTypeId));

		ProviderAppointmentSelectionTypeId appointmentSelectionTypeId =
				ProviderSearchResultApiResponse.appointmentSelectionTypeIdFor(List.of(providerFind),
						Map.of(providerId, provider), List.of(availableAppointment(provider, List.of(appointmentTypeId))));

		assertEquals(ProviderAppointmentSelectionTypeId.APPOINTMENT_PREDETERMINED, appointmentSelectionTypeId);
	}

	@Test
	public void appointmentSelectionTypeAggregatesClinicSlotAppointmentTypeIds() {
		UUID firstProviderId = UUID.randomUUID();
		UUID secondProviderId = UUID.randomUUID();
		UUID appointmentTypeId = UUID.randomUUID();
		UUID otherAppointmentTypeId = UUID.randomUUID();
		Provider firstProvider = provider(firstProviderId, VideoconferencePlatformId.SWITCHBOARD);
		Provider secondProvider = provider(secondProviderId, VideoconferencePlatformId.SWITCHBOARD);
		List<ProviderFind> providerFinds = List.of(
				providerFind(firstProviderId, Set.of(appointmentTypeId)),
				providerFind(secondProviderId, Set.of(appointmentTypeId)));
		Map<UUID, Provider> providersById = Map.of(firstProviderId, firstProvider, secondProviderId, secondProvider);

		ProviderAppointmentSelectionTypeId predeterminedSelectionTypeId =
				ProviderSearchResultApiResponse.appointmentSelectionTypeIdFor(providerFinds, providersById, List.of(
						availableAppointment(firstProvider, List.of(appointmentTypeId)),
						availableAppointment(secondProvider, List.of(appointmentTypeId))));
		ProviderAppointmentSelectionTypeId undeterminedSelectionTypeId =
				ProviderSearchResultApiResponse.appointmentSelectionTypeIdFor(providerFinds, providersById, List.of(
						availableAppointment(firstProvider, List.of(appointmentTypeId)),
						availableAppointment(secondProvider, List.of(otherAppointmentTypeId))));

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
						Map.of(providerId, provider), List.of(availableAppointment(provider, null)));

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
						Map.of(providerId, provider), List.of());

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
						Map.of(firstProviderId, firstProvider, secondProviderId, secondProvider), List.of());

		assertEquals(ProviderAppointmentSelectionTypeId.APPOINTMENT_BY_PHONE, appointmentSelectionTypeId);
	}

	@Nonnull
	protected Provider provider(@Nonnull UUID providerId,
															@Nullable VideoconferencePlatformId videoconferencePlatformId) {
		Provider provider = new Provider();
		provider.setProviderId(providerId);
		provider.setVideoconferencePlatformId(videoconferencePlatformId);
		return provider;
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
	protected ProviderSearchResultApiResponse.AvailableAppointment availableAppointment(@Nonnull Provider provider,
																																										 @Nullable List<UUID> appointmentTypeIds) {
		AvailabilityTime availabilityTime = new AvailabilityTime();
		availabilityTime.setTime(LocalTime.NOON);
		availabilityTime.setStatus(AvailabilityStatus.AVAILABLE);
		availabilityTime.setAppointmentTypeIds(appointmentTypeIds);

		return new ProviderSearchResultApiResponse.AvailableAppointment(provider, LocalDate.of(2026, 1, 1), availabilityTime, null);
	}
}
