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

import com.cobaltplatform.api.cache.Cache;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.response.ProviderAvailabilityApiResponse;
import com.cobaltplatform.api.model.api.response.ProviderAvailabilityApiResponse.AppointmentModalityAvailabilityApiResponse;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityId;
import com.cobaltplatform.api.model.api.response.ProviderSearchResultApiResponse.FirstAvailableAppointmentApiResponse;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.Clinic;
import com.cobaltplatform.api.model.db.Feature.FeatureId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.model.db.VideoconferencePlatform.VideoconferencePlatformId;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.cobaltplatform.api.model.service.ProviderFind;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityDate;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityStatus;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityTime;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.ValidationException;
import com.lokalized.Strings;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Transmogrify, LLC.
 */
public class ProviderAvailabilityResourceTests {
	@Test
	public void parseFeatureIdForAvailabilityTreatsMissingAndBlankAsAbsent() {
		assertFalse(ProviderAvailabilityResource.parseFeatureIdForAvailability(Optional.empty()).isPresent());
		assertFalse(ProviderAvailabilityResource.parseFeatureIdForAvailability(Optional.of("")).isPresent());
		assertFalse(ProviderAvailabilityResource.parseFeatureIdForAvailability(Optional.of("   ")).isPresent());
	}

	@Test
	public void parseFeatureIdForAvailabilityParsesValidValue() {
		assertEquals(Optional.of(FeatureId.THERAPY),
				ProviderAvailabilityResource.parseFeatureIdForAvailability(Optional.of("THERAPY")));
		assertEquals(Optional.of(FeatureId.THERAPY),
				ProviderAvailabilityResource.parseFeatureIdForAvailability(Optional.of(" THERAPY ")));
	}

	@Test(expected = ValidationException.class)
	public void parseFeatureIdForAvailabilityRejectsInvalidValue() {
		ProviderAvailabilityResource.parseFeatureIdForAvailability(Optional.of("invalid"));
	}

	@Test
	public void availabilityDateRangeDefaultsToTodayThroughNinetyDays() {
		ZoneId timeZone = ZoneId.of("America/New_York");
		LocalDate dateBeforeResolution = LocalDate.now(timeZone);

		ProviderAvailabilityResource.AvailabilityDateRange dateRange =
				ProviderAvailabilityResource.availabilityDateRangeFor(Optional.empty(), Optional.empty(), timeZone);

		LocalDate dateAfterResolution = LocalDate.now(timeZone);

		assertTrue(dateRange.getStartDate().equals(dateBeforeResolution)
				|| dateRange.getStartDate().equals(dateAfterResolution));
		assertEquals(dateRange.getStartDate().plusDays(90), dateRange.getEndDate());
	}

	@Test
	public void availabilityDateRangeDefaultsEndDateFromSuppliedStartDate() {
		LocalDate startDate = LocalDate.of(2026, 1, 1);

		ProviderAvailabilityResource.AvailabilityDateRange dateRange =
				ProviderAvailabilityResource.availabilityDateRangeFor(Optional.of(startDate), Optional.empty(), ZoneId.of("America/New_York"));

		assertEquals(startDate, dateRange.getStartDate());
		assertEquals(startDate.plusDays(90), dateRange.getEndDate());
	}

	@Test(expected = ValidationException.class)
	public void availabilityDateRangeRejectsInvalidRange() {
		ProviderAvailabilityResource.availabilityDateRangeFor(Optional.of(LocalDate.of(2026, 1, 2)),
				Optional.of(LocalDate.of(2026, 1, 1)), ZoneId.of("America/New_York"));
	}

	@Test
	public void providerSupportRolesMatchFeature() {
		assertTrue(ProviderAvailabilityResource.providerSupportRolesMatchFeature(List.of(SupportRoleId.CLINICIAN),
				List.of(SupportRoleId.COACH, SupportRoleId.CLINICIAN)));
	}

	@Test
	public void providerSupportRolesMatchFeatureRejectsNoOverlap() {
		assertFalse(ProviderAvailabilityResource.providerSupportRolesMatchFeature(List.of(SupportRoleId.CLINICIAN),
				List.of(SupportRoleId.COACH)));
	}

	@Test
	public void providerSupportRolesMatchFeatureRejectsFeatureWithoutSupportRoles() {
		assertFalse(ProviderAvailabilityResource.providerSupportRolesMatchFeature(List.of(),
				List.of(SupportRoleId.CLINICIAN)));
	}

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
		ProviderAvailabilityApiResponse response = new ProviderAvailabilityApiResponse(currentContextProvider(), formatter(), clinic,
				providerFinds, Map.of(providerId, provider), Map.of(
				firstAppointmentTypeId, firstAppointmentType,
				secondAppointmentTypeId, secondAppointmentType
		), date, date.plusDays(90));
		AppointmentModalityAvailabilityApiResponse virtualAvailability = response.getAppointmentModalities().get(0);

		assertEquals(ProviderAppointmentModalityId.VIRTUAL, virtualAvailability.getAppointmentModalityId());
		assertEquals(List.of(secondAppointmentTypeId), virtualAvailability.getAvailability().get(0).getTimes().get(0).getAppointmentTypeIds());
		assertEquals("Beta appointment description", virtualAvailability.getAvailability().get(0).getTimes().get(0).getAppointmentTypeDescription());
	}

	@Test
	public void providerAvailabilityIncludesFirstAvailableAppointment() {
		UUID providerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID appointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000011");
		UUID assessmentId = UUID.fromString("00000000-0000-0000-0000-000000000021");
		UUID screeningFlowId = UUID.fromString("00000000-0000-0000-0000-000000000031");
		UUID epicDepartmentId = UUID.fromString("00000000-0000-0000-0000-000000000041");
		String epicAppointmentFhirId = "FHIR-APPT-1";
		LocalDate date = LocalDate.of(2026, 1, 1);
		LocalTime time = LocalTime.of(9, 0);
		Provider provider = provider(providerId, "Test Provider", VideoconferencePlatformId.SWITCHBOARD);
		AppointmentType appointmentType = appointmentType(appointmentTypeId, "Alpha Visit", "Alpha appointment description",
				assessmentId, screeningFlowId);
		ProviderFind providerFind = providerFind(providerId, "Test Provider", availabilityDate(date, List.of(
				availabilityTime(time, AvailabilityStatus.AVAILABLE, List.of(appointmentTypeId), epicDepartmentId, epicAppointmentFhirId)
		)));

		ProviderAvailabilityApiResponse response = new ProviderAvailabilityApiResponse(currentContextProvider(), formatter(), provider,
				List.of(providerFind), Map.of(appointmentTypeId, appointmentType), date, date.plusDays(90));
		FirstAvailableAppointmentApiResponse firstAvailableAppointment = response.getFirstAvailableAppointment();

		assertNotNull(firstAvailableAppointment);
		assertEquals(providerId, firstAvailableAppointment.getProviderId());
		assertEquals(date, firstAvailableAppointment.getDate());
		assertEquals(time, firstAvailableAppointment.getTime());
		assertEquals(LocalDateTime.of(date, time), firstAvailableAppointment.getDateTime());
		assertEquals(normalizedTimeDescription(time), firstAvailableAppointment.getTimeDescription());
		assertEquals(appointmentTypeId, firstAvailableAppointment.getAppointmentTypeId());
		assertEquals(List.of(appointmentTypeId), firstAvailableAppointment.getAppointmentTypeIds());
		assertEquals("Alpha appointment description", firstAvailableAppointment.getAppointmentDescription());
		assertEquals(assessmentId, firstAvailableAppointment.getAssessmentId());
		assertEquals(screeningFlowId, firstAvailableAppointment.getScreeningFlowId());
		assertEquals(epicDepartmentId, firstAvailableAppointment.getEpicDepartmentId());
		assertEquals(epicAppointmentFhirId, firstAvailableAppointment.getEpicAppointmentFhirId());
	}

	@Test
	public void clinicAvailabilityUsesEarliestProviderFirstAvailableAppointment() {
		UUID clinicId = UUID.fromString("00000000-0000-0000-0000-000000000002");
		UUID laterProviderId = UUID.fromString("00000000-0000-0000-0000-000000000003");
		UUID earlierProviderId = UUID.fromString("00000000-0000-0000-0000-000000000004");
		UUID appointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000011");
		LocalDate date = LocalDate.of(2026, 1, 1);
		Provider laterProvider = provider(laterProviderId, "Later Provider", VideoconferencePlatformId.SWITCHBOARD);
		Provider earlierProvider = provider(earlierProviderId, "Earlier Provider", VideoconferencePlatformId.SWITCHBOARD);
		Clinic clinic = clinic(clinicId, "Test Clinic");
		AppointmentType appointmentType = appointmentType(appointmentTypeId, "Alpha Visit", "Alpha appointment description");
		ProviderFind laterProviderFind = providerFind(laterProviderId, "Later Provider", availabilityDate(date, List.of(
				availabilityTime(LocalTime.of(11, 0), AvailabilityStatus.AVAILABLE, List.of(appointmentTypeId))
		)));
		ProviderFind earlierProviderFind = providerFind(earlierProviderId, "Earlier Provider", availabilityDate(date, List.of(
				availabilityTime(LocalTime.of(10, 0), AvailabilityStatus.AVAILABLE, List.of(appointmentTypeId))
		)));

		ProviderAvailabilityApiResponse response = new ProviderAvailabilityApiResponse(currentContextProvider(), formatter(), clinic,
				List.of(laterProviderFind, earlierProviderFind),
				Map.of(laterProviderId, laterProvider, earlierProviderId, earlierProvider),
				Map.of(appointmentTypeId, appointmentType), date, date.plusDays(90));

		assertNotNull(response.getFirstAvailableAppointment());
		assertEquals(earlierProviderId, response.getFirstAvailableAppointment().getProviderId());
		assertEquals(LocalTime.of(10, 0), response.getFirstAvailableAppointment().getTime());
	}

	@Test
	public void firstAvailableAppointmentPreservesAmbiguousAppointmentTypeIds() {
		UUID providerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID firstAppointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000011");
		UUID secondAppointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000012");
		LocalDate date = LocalDate.of(2026, 1, 1);
		Provider provider = provider(providerId, "Test Provider", VideoconferencePlatformId.SWITCHBOARD);
		AppointmentType firstAppointmentType = appointmentType(firstAppointmentTypeId, "Alpha Visit", "Alpha appointment description");
		AppointmentType secondAppointmentType = appointmentType(secondAppointmentTypeId, "Beta Visit", "Beta appointment description");
		ProviderFind providerFind = providerFind(providerId, "Test Provider", availabilityDate(date, List.of(
				availabilityTime(LocalTime.of(9, 0), AvailabilityStatus.AVAILABLE, List.of(firstAppointmentTypeId, secondAppointmentTypeId))
		)));

		ProviderAvailabilityApiResponse response = new ProviderAvailabilityApiResponse(currentContextProvider(), formatter(), provider,
				List.of(providerFind), Map.of(firstAppointmentTypeId, firstAppointmentType, secondAppointmentTypeId, secondAppointmentType),
				date, date.plusDays(90));

		assertNotNull(response.getFirstAvailableAppointment());
		assertNull(response.getFirstAvailableAppointment().getAppointmentTypeId());
		assertEquals(List.of(firstAppointmentTypeId, secondAppointmentTypeId),
				response.getFirstAvailableAppointment().getAppointmentTypeIds());
	}

	@Test
	public void firstAvailableAppointmentIsNullWhenNoAppointmentsAreAvailable() {
		UUID providerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID appointmentTypeId = UUID.fromString("00000000-0000-0000-0000-000000000011");
		LocalDate date = LocalDate.of(2026, 1, 1);
		Provider provider = provider(providerId, "Test Provider", VideoconferencePlatformId.SWITCHBOARD);
		AppointmentType appointmentType = appointmentType(appointmentTypeId, "Alpha Visit", "Alpha appointment description");
		ProviderFind providerFind = providerFind(providerId, "Test Provider", availabilityDate(date, List.of(
				availabilityTime(LocalTime.of(9, 0), AvailabilityStatus.BOOKED, List.of(appointmentTypeId))
		)));

		ProviderAvailabilityApiResponse response = new ProviderAvailabilityApiResponse(currentContextProvider(), formatter(), provider,
				List.of(providerFind), Map.of(appointmentTypeId, appointmentType), date, date.plusDays(90));

		assertNull(response.getFirstAvailableAppointment());
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
		provider.setLocale(Locale.US);
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
		clinic.setLocale(Locale.US);
		return clinic;
	}

	@Nonnull
	protected AppointmentType appointmentType(@Nonnull UUID appointmentTypeId,
																						@Nonnull String name,
																						@Nonnull String description) {
		return appointmentType(appointmentTypeId, name, description, null, null);
	}

	@Nonnull
	protected AppointmentType appointmentType(@Nonnull UUID appointmentTypeId,
																						@Nonnull String name,
																						@Nonnull String description,
																						UUID assessmentId,
																						UUID screeningFlowId) {
		AppointmentType appointmentType = new AppointmentType();
		appointmentType.setAppointmentTypeId(appointmentTypeId);
		appointmentType.setName(name);
		appointmentType.setDescription(description);
		appointmentType.setDurationInMinutes(60L);
		appointmentType.setSchedulingSystemId(SchedulingSystemId.COBALT);
		appointmentType.setVisitTypeId(VisitTypeId.INITIAL);
		appointmentType.setAssessmentId(assessmentId);
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
		return availabilityTime(time, status, appointmentTypeIds, null, null);
	}

	@Nonnull
	protected AvailabilityTime availabilityTime(@Nonnull LocalTime time,
																							@Nonnull AvailabilityStatus status,
																							@Nonnull List<UUID> appointmentTypeIds,
																							UUID epicDepartmentId,
																							String epicAppointmentFhirId) {
		AvailabilityTime availabilityTime = new AvailabilityTime();
		availabilityTime.setTime(time);
		availabilityTime.setStatus(status);
		availabilityTime.setAppointmentTypeIds(appointmentTypeIds);
		availabilityTime.setEpicDepartmentId(epicDepartmentId);
		availabilityTime.setEpicAppointmentFhirId(epicAppointmentFhirId);
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
	protected String normalizedTimeDescription(@Nonnull LocalTime time) {
		return formatter().formatTime(time, FormatStyle.SHORT)
				.replace(" ", "")
				.toLowerCase(Locale.US);
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
