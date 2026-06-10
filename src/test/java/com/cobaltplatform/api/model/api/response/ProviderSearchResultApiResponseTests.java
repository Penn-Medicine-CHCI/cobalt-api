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
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentSelectionTypeId;
import com.cobaltplatform.api.model.db.AppointmentBookingLevel.AppointmentBookingLevelId;
import com.cobaltplatform.api.model.db.Clinic;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.VideoconferencePlatform.VideoconferencePlatformId;
import com.cobaltplatform.api.model.service.ProviderFind;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityStatus;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityTime;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

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
