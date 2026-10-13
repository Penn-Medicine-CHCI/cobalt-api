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

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.integration.enterprise.EnterprisePlugin;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.integration.epic.request.GetProviderScheduleRequest;
import com.cobaltplatform.api.integration.epic.response.GetProviderScheduleResponse;
import com.cobaltplatform.api.integration.epic.response.GetProviderScheduleResponse.ScheduleSlot;
import com.cobaltplatform.api.model.db.EpicAppointmentFilter.EpicAppointmentFilterId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.ProviderAvailability;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.pyranid.Database;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the persisted availability produced by Epic sync using synthetic Epic schedules.
 */
public class EpicSyncManagerStartTimeAlignmentTests {
	@Test
	public void unsetIntervalRetainsEpicHalfHourStarts() {
		runWithSchedule(halfHourSchedule(30), (database, syncManager, epicClient) -> {
			Fixture fixture = createFixture(database, EpicAppointmentFilterId.VISIT_TYPE, null);
			assertTrue(syncManager.syncProviderAvailability(fixture.providerId, fixture.date, false));
			assertEquals(halfHourStarts(fixture.date), availability(database, fixture, fixture.appointmentTypeId));
			assertEquals(1, epicClient.requests.size());
			assertEquals(fixture.appointmentTypeId.toString(), epicClient.requests.get(0).getVisitTypeID());
		});
	}

	@Test
	public void hourlyIntervalFiltersHalfHourStartsAndReplacesStaleCache() {
		runWithSchedule(halfHourSchedule(30), (database, syncManager, epicClient) -> {
			Fixture fixture = createFixture(database, EpicAppointmentFilterId.VISIT_TYPE, 60);
			database.execute("""
					INSERT INTO provider_availability (provider_id, appointment_type_id, date_time, epic_department_id)
					VALUES (?, ?, ?, ?)
					""", fixture.providerId, fixture.appointmentTypeId,
					fixture.date.atTime(13, 30), fixture.departmentId);

			assertTrue(syncManager.syncProviderAvailability(fixture.providerId, fixture.date, false));
			assertEquals(hourlyStarts(fixture.date), availability(database, fixture, fixture.appointmentTypeId));
		});
	}

	@Test
	public void intervalIsScopedToExactProviderAndAppointmentType() {
		runWithSchedule(halfHourSchedule(30), (database, syncManager, epicClient) -> {
			Fixture alignedFixture = createFixture(database, EpicAppointmentFilterId.VISIT_TYPE, 60);
			UUID otherAppointmentTypeId = insertAppointmentType(database);
			associateAppointmentType(database, alignedFixture.providerId, otherAppointmentTypeId, 2, null);

			UUID otherProviderId = insertProvider(database, EpicAppointmentFilterId.VISIT_TYPE);
			associateAppointmentType(database, otherProviderId, alignedFixture.appointmentTypeId, 1, null);
			associateDepartment(database, otherProviderId, alignedFixture.departmentId);
			Fixture otherFixture = new Fixture(otherProviderId, alignedFixture.appointmentTypeId,
					alignedFixture.departmentId, alignedFixture.date);

			assertTrue(syncManager.syncProviderAvailability(alignedFixture.providerId, alignedFixture.date, false));
			assertTrue(syncManager.syncProviderAvailability(otherFixture.providerId, otherFixture.date, false));
			assertEquals(hourlyStarts(alignedFixture.date), availability(database, alignedFixture, alignedFixture.appointmentTypeId));
			assertEquals(halfHourStarts(alignedFixture.date), availability(database, alignedFixture, otherAppointmentTypeId));
			assertEquals(halfHourStarts(otherFixture.date), availability(database, otherFixture, otherFixture.appointmentTypeId));
		});
	}

	@Test
	public void offGridHeldAndUnavailableRangesStillBlockOverlappingHourlyVisits() {
		ScheduleSlot held = slot("1:30 PM", 30);
		held.setHeldTimeReason("Held time");
		ScheduleSlot unavailable = slot("2:30 PM", 30);
		unavailable.setUnavailableTimeReason("Unavailable time");

		runWithSchedule(List.of(slot("1:00 PM", 30), held, slot("2:00 PM", 30), unavailable, slot("3:00 PM", 30)),
				(database, syncManager, epicClient) -> {
					Fixture fixture = createFixture(database, EpicAppointmentFilterId.VISIT_TYPE, 60);
					assertTrue(syncManager.syncProviderAvailability(fixture.providerId, fixture.date, false));
					assertEquals(List.of(fixture.date.atTime(15, 0)), availability(database, fixture, fixture.appointmentTypeId));
				});
	}

	@Test
	public void durationBasedSyncAppliesEachAppointmentTypesOwnInterval() {
		runWithSchedule(halfHourSchedule(60), (database, syncManager, epicClient) -> {
			Fixture fixture = createFixture(database, EpicAppointmentFilterId.NONE, 60);
			UUID otherAppointmentTypeId = insertAppointmentType(database);
			associateAppointmentType(database, fixture.providerId, otherAppointmentTypeId, 2, null);

			assertTrue(syncManager.syncProviderAvailability(fixture.providerId, fixture.date, false));
			assertEquals(hourlyStarts(fixture.date), availability(database, fixture, fixture.appointmentTypeId));
			assertEquals(halfHourStarts(fixture.date), availability(database, fixture, otherAppointmentTypeId));
			assertEquals(1, epicClient.requests.size());
			assertNull(epicClient.requests.get(0).getVisitTypeID());
		});
	}

	@Test
	public void manualVisitTypeRetainsDurationFilteringAndAppliesAlignment() {
		List<ScheduleSlot> slots = new ArrayList<>(halfHourSchedule(60));
		slots.add(slot("4:00 PM", 30));

		runWithSchedule(slots, (database, syncManager, epicClient) -> {
			Fixture fixture = createFixture(database, EpicAppointmentFilterId.MANUAL_VISIT_TYPE, 60);
			assertTrue(syncManager.syncProviderAvailability(fixture.providerId, fixture.date, false));
			assertEquals(hourlyStarts(fixture.date), availability(database, fixture, fixture.appointmentTypeId));
			assertEquals(1, epicClient.requests.size());
			assertNull(epicClient.requests.get(0).getVisitTypeID());
		});
	}

	private void runWithSchedule(List<ScheduleSlot> scheduleSlots, SyncAssertions assertions) {
		RecordingEpicClient epicClient = new RecordingEpicClient(scheduleSlots);

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			database.execute("UPDATE institution SET epic_provider_slot_booking_sync_enabled=FALSE WHERE institution_id=?", InstitutionId.COBALT);
			assertions.run(database, app.getInjector().getInstance(EpicSyncManager.class), epicClient);
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(RecordingEpicClient.class).toInstance(epicClient);
				bind(EnterprisePluginProvider.class).to(RecordingEnterprisePluginProvider.class);
			}
		});
	}

	private Fixture createFixture(Database database, EpicAppointmentFilterId filterId, Integer intervalInMinutes) {
		UUID providerId = insertProvider(database, filterId);
		UUID appointmentTypeId = insertAppointmentType(database);
		UUID departmentId = UUID.randomUUID();

		database.execute("""
				INSERT INTO epic_department (epic_department_id, institution_id, department_id, department_id_type, name)
				VALUES (?, ?, ?, 'EXTERNAL', 'Test Epic Alignment Department')
				""", departmentId, InstitutionId.COBALT, departmentId.toString());
		associateAppointmentType(database, providerId, appointmentTypeId, 1, intervalInMinutes);
		associateDepartment(database, providerId, departmentId);
		return new Fixture(providerId, appointmentTypeId, departmentId,
				LocalDate.now(ZoneId.of("America/New_York")).plusDays(14));
	}

	private UUID insertProvider(Database database, EpicAppointmentFilterId filterId) {
		UUID providerId = UUID.randomUUID();
		database.execute("""
				INSERT INTO provider (
					provider_id, institution_id, name, email_address, scheduling_system_id,
					videoconference_platform_id, epic_provider_id, epic_provider_id_type, epic_appointment_filter_id, url_name
				) VALUES (?, ?, 'Test Epic Alignment Provider', ?, 'EPIC', 'EXTERNAL', ?, 'EXTERNAL', ?, ?)
				""", providerId, InstitutionId.COBALT, "alignment-" + providerId + "@example.org",
				providerId.toString(), filterId, "test-epic-alignment-" + providerId);
		return providerId;
	}

	private UUID insertAppointmentType(Database database) {
		UUID appointmentTypeId = UUID.randomUUID();
		database.execute("""
				INSERT INTO appointment_type (
					appointment_type_id, name, duration_in_minutes, scheduling_system_id,
					epic_visit_type_id, epic_visit_type_id_type, visit_type_id
				) VALUES (?, 'Test Epic Alignment Consultation', 60, 'EPIC', ?, 'EXTERNAL', 'INITIAL')
				""", appointmentTypeId, appointmentTypeId.toString());
		return appointmentTypeId;
	}

	private void associateAppointmentType(Database database, UUID providerId, UUID appointmentTypeId,
																				 int displayOrder, Integer intervalInMinutes) {
		database.execute("""
				INSERT INTO provider_appointment_type (
					provider_id, appointment_type_id, display_order, scheduling_start_time_interval_in_minutes
				) VALUES (?, ?, ?, ?)
				""", providerId, appointmentTypeId, displayOrder, intervalInMinutes);
	}

	private void associateDepartment(Database database, UUID providerId, UUID departmentId) {
		database.execute("""
				INSERT INTO provider_epic_department (provider_id, epic_department_id, display_order)
				VALUES (?, ?, 1)
				""", providerId, departmentId);
	}

	private List<LocalDateTime> availability(Database database, Fixture fixture, UUID appointmentTypeId) {
		return database.queryForList("""
				SELECT pa.* FROM provider_availability pa
				WHERE pa.provider_id=? AND pa.appointment_type_id=? AND pa.date_time::date=?
				ORDER BY pa.date_time
				""", ProviderAvailability.class, fixture.providerId, appointmentTypeId, fixture.date).stream()
				.map(ProviderAvailability::getDateTime)
				.collect(Collectors.toList());
	}

	private static List<ScheduleSlot> halfHourSchedule(int lengthInMinutes) {
		return List.of(slot("1:00 PM", lengthInMinutes), slot("1:30 PM", lengthInMinutes),
				slot("2:00 PM", lengthInMinutes), slot("2:30 PM", lengthInMinutes), slot("3:00 PM", lengthInMinutes));
	}

	private static ScheduleSlot slot(String startTime, int lengthInMinutes) {
		ScheduleSlot slot = new ScheduleSlot();
		slot.setStartTime(startTime);
		slot.setLength(Integer.toString(lengthInMinutes));
		slot.setAvailableOpenings("1");
		return slot;
	}

	private static List<LocalDateTime> halfHourStarts(LocalDate date) {
		return List.of(date.atTime(13, 0), date.atTime(13, 30), date.atTime(14, 0), date.atTime(14, 30), date.atTime(15, 0));
	}

	private static List<LocalDateTime> hourlyStarts(LocalDate date) {
		return List.of(date.atTime(13, 0), date.atTime(14, 0), date.atTime(15, 0));
	}

	private static final class Fixture {
		private final UUID providerId;
		private final UUID appointmentTypeId;
		private final UUID departmentId;
		private final LocalDate date;

		private Fixture(UUID providerId, UUID appointmentTypeId, UUID departmentId, LocalDate date) {
			this.providerId = providerId;
			this.appointmentTypeId = appointmentTypeId;
			this.departmentId = departmentId;
			this.date = date;
		}
	}

	@FunctionalInterface
	private interface SyncAssertions {
		void run(Database database, EpicSyncManager syncManager, RecordingEpicClient epicClient);
	}

	@Singleton
	public static class RecordingEnterprisePluginProvider extends EnterprisePluginProvider {
		private final EpicClient epicClient;

		@Inject
		public RecordingEnterprisePluginProvider(Injector injector, Configuration configuration,
				javax.inject.Provider<CurrentContext> currentContextProvider, RecordingEpicClient epicClient) {
			super(injector, configuration, currentContextProvider);
			this.epicClient = epicClient;
		}

		@Nonnull
		@Override
		public EnterprisePlugin enterprisePluginForInstitutionId(@Nonnull InstitutionId institutionId) {
			if (institutionId != InstitutionId.COBALT)
				return super.enterprisePluginForInstitutionId(institutionId);

			return new EnterprisePlugin() {
				@Nonnull
				@Override
				public InstitutionId getInstitutionId() {
					return institutionId;
				}

				@Nonnull
				@Override
				public Optional<EpicClient> epicClientForBackendService() {
					return Optional.of(epicClient);
				}
			};
		}
	}

	public static class RecordingEpicClient extends MockEpicClient {
		private final List<ScheduleSlot> scheduleSlots;
		private final List<GetProviderScheduleRequest> requests = new ArrayList<>();

		public RecordingEpicClient(List<ScheduleSlot> scheduleSlots) {
			this.scheduleSlots = scheduleSlots;
		}

		@Nonnull
		@Override
		public LocalTime parseTimeAmPm(@Nonnull String time) {
			return LocalTime.parse(time, DateTimeFormatter.ofPattern("h:mm a", Locale.US));
		}

		@Nonnull
		@Override
		public GetProviderScheduleResponse performGetProviderSchedule(@Nonnull GetProviderScheduleRequest request) {
			requests.add(request);
			GetProviderScheduleResponse response = new GetProviderScheduleResponse();
			response.setScheduleSlots(scheduleSlots);
			response.setProviderMessages(Collections.emptyList());
			response.setProviderIDs(Collections.emptyList());
			response.setDepartmentIDs(Collections.emptyList());
			return response;
		}
	}
}
