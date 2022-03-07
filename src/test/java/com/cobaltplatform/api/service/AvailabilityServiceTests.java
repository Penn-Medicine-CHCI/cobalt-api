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

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.api.request.CreateAppointmentTypeRequest;
import com.cobaltplatform.api.model.api.request.CreateLogicalAvailabilityRequest;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.LogicalAvailabilityType.LogicalAvailabilityTypeId;
import com.cobaltplatform.api.model.db.ProviderAvailability;
import com.cobaltplatform.api.model.db.RecurrenceType.RecurrenceTypeId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.cobaltplatform.api.model.service.ProviderCalendar;
import com.google.inject.Injector;
import com.pyranid.Database;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AvailabilityServiceTests {
	@Nonnull
	private static final AtomicLong SEQUENCE_GENERATOR;

	static {
		SEQUENCE_GENERATOR = new AtomicLong(0);
	}

	@Test
	public void testLogicalAvailabilitySlots() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AvailabilityService availabilityService = app.getInjector().getInstance(AvailabilityService.class);

			// Make a new provider
			TestProvider testProvider = createTestProvider(app.getInjector());

			// 1. Create logical availability
			availabilityService.createLogicalAvailability(new CreateLogicalAvailabilityRequest() {{
				setProviderId(testProvider.getProviderId());
				setAccountId(testProvider.getAccountId());
				setLogicalAvailabilityTypeId(LogicalAvailabilityTypeId.OPEN);
				setRecurrenceTypeId(RecurrenceTypeId.NONE);
				setAppointmentTypeIds(List.of(testProvider.getNpvAppointmentTypeId()));
				setStartDateTime(LocalDateTime.of(
						LocalDate.of(2022, 2, 15),
						LocalTime.of(10, 30)));
				setEndDate(LocalDate.of(2022, 2, 15));
				setEndTime(LocalTime.of(14, 30));
			}});

			// 2. Ask for availability slots
			LocalDateTime startDateTime = LocalDateTime.of(
					LocalDate.of(2022, 2, 15),
					LocalTime.of(9, 00));

			LocalDateTime endDateTime = LocalDateTime.of(
					LocalDate.of(2022, 2, 15),
					LocalTime.of(18, 00));

			// 3. Verify slots
			List<ProviderAvailability> providerAvailabilities = availabilityService.nativeSchedulingProviderAvailabilitiesByProviderId(
					testProvider.getProviderId(), Set.of(VisitTypeId.INITIAL), startDateTime, endDateTime);

			// For 60 minute NPV, slots should be:
			// 10:30-11:30, 11:30-12:30, 12:30-1:30, 1:30-2:30

			Assert.assertEquals("Wrong number of availability slots", 4, providerAvailabilities.size());

			Assert.assertEquals("Wrong 1st slot time", LocalDateTime.of(
					LocalDate.of(2022, 2, 15),
					LocalTime.of(10, 30)), providerAvailabilities.get(0).getDateTime());

			Assert.assertEquals("Wrong 2nd slot time", LocalDateTime.of(
					LocalDate.of(2022, 2, 15),
					LocalTime.of(11, 30)), providerAvailabilities.get(1).getDateTime());

			Assert.assertEquals("Wrong 3rd slot time", LocalDateTime.of(
					LocalDate.of(2022, 2, 15),
					LocalTime.of(12, 30)), providerAvailabilities.get(2).getDateTime());

			Assert.assertEquals("Wrong 4th slot time", LocalDateTime.of(
					LocalDate.of(2022, 2, 15),
					LocalTime.of(13, 30)), providerAvailabilities.get(3).getDateTime());
		});
	}

	@Test
	public void testProviderCalendar() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AvailabilityService availabilityService = app.getInjector().getInstance(AvailabilityService.class);

			// Make a new provider
			TestProvider testProvider = createTestProvider(app.getInjector());

			// 1. Create logical availability
			availabilityService.createLogicalAvailability(new CreateLogicalAvailabilityRequest() {{
				setProviderId(testProvider.getProviderId());
				setAccountId(testProvider.getAccountId());
				setLogicalAvailabilityTypeId(LogicalAvailabilityTypeId.OPEN);
				setRecurrenceTypeId(RecurrenceTypeId.DAILY);
				setRecurTuesday(true);
				setRecurWednesday(true);
				setRecurFriday(true);
				setAppointmentTypeIds(List.of(testProvider.getNpvAppointmentTypeId()));
				setStartDateTime(LocalDateTime.of(
						LocalDate.of(2022, 3, 1),
						LocalTime.of(10, 30)));
				setEndDate(LocalDate.of(2022, 3, 5));
				setEndTime(LocalTime.of(14, 30));
			}});

			// 2. Pull the calendar for the provider
			ProviderCalendar providerCalendar = availabilityService.findProviderCalendar(testProvider.getProviderId(),
					LocalDate.of(2022, 3, 1),
					LocalDate.of(2022, 3, 6));

			// Should be 3 availabilities:
			// 2022-03-01 10:30 to 14:30 (tues), 2022-03-02 10:30 to 14:30 (weds), 2022-03-04 10:30 to 14:30 (fri)
			Assert.assertEquals("Wrong number of availabilities", 3, providerCalendar.getAvailabilities().size());

			// Spot check the last one
			Assert.assertEquals("Wrong 3rd availability start", LocalDateTime.of(
					LocalDate.of(2022, 3, 4),
					LocalTime.of(10, 30)), providerCalendar.getAvailabilities().get(2).getStartDateTime());

			Assert.assertEquals("Wrong 3rd availability end", LocalDateTime.of(
					LocalDate.of(2022, 3, 4),
					LocalTime.of(14, 30)), providerCalendar.getAvailabilities().get(2).getEndDateTime());
		});
	}

	@Nonnull
	protected TestProvider createTestProvider(@Nonnull Injector injector) {
		Database database = injector.getInstance(Database.class);
		AccountService accountService = injector.getInstance(AccountService.class);
		AppointmentService appointmentService = injector.getInstance(AppointmentService.class);

		UUID providerId = UUID.randomUUID();
		Long nextSequence = SEQUENCE_GENERATOR.incrementAndGet();
		Long NPV_DURATION_IN_MINUTES = 60L;
		Long RPV_DURATION_IN_MINUTES = 30L;

		// Make a provider
		database.execute("INSERT INTO provider (provider_id, institution_id, name, email_address) VALUES (?,?,?,?)",
				providerId, InstitutionId.COBALT, format("Test Provider %s", nextSequence),
				format("testprovider-%s@cobaltinnovations.org", nextSequence));

		// Make an account for the provider
		UUID accountId = accountService.createAccount(new CreateAccountRequest() {{
			setAccountSourceId(AccountSourceId.ANONYMOUS);
			setInstitutionId(InstitutionId.COBALT);
			setRoleId(RoleId.PROVIDER);
			setEmailAddress("test@cobaltinnovations.org");
		}});

		database.execute("UPDATE account SET provider_id=? WHERE account_id=?", providerId, accountId);

		// Create appointment types
		UUID npvAppointmentTypeId = appointmentService.createAppointmentType(new CreateAppointmentTypeRequest() {{
			setProviderId(providerId);
			setName("Test NPV");
			setDescription("Test NPV Description");
			setVisitTypeId(VisitTypeId.INITIAL);
			setDurationInMinutes(NPV_DURATION_IN_MINUTES);
			setHexColor("#FFFFFF");
			setSchedulingSystemId(SchedulingSystemId.COBALT);
			setPatientIntakeQuestions(Collections.emptyList());
			setScreeningQuestions(Collections.emptyList());
		}});

		UUID rpvAppointmentTypeId = appointmentService.createAppointmentType(new CreateAppointmentTypeRequest() {{
			setProviderId(providerId);
			setName("Test RPV");
			setDescription("Test RPV Description");
			setVisitTypeId(VisitTypeId.FOLLOWUP);
			setDurationInMinutes(RPV_DURATION_IN_MINUTES);
			setHexColor("#FFFFFF");
			setSchedulingSystemId(SchedulingSystemId.COBALT);
			setPatientIntakeQuestions(Collections.emptyList());
			setScreeningQuestions(Collections.emptyList());
		}});

		TestProvider testProvider = new TestProvider();
		testProvider.setProviderId(providerId);
		testProvider.setAccountId(accountId);
		testProvider.setNpvAppointmentTypeId(npvAppointmentTypeId);
		testProvider.setNpvDurationInMinutes(NPV_DURATION_IN_MINUTES);
		testProvider.setRpvAppointmentTypeId(rpvAppointmentTypeId);
		testProvider.setRpvDurationInMinutes(RPV_DURATION_IN_MINUTES);

		return testProvider;
	}

	@NotThreadSafe
	protected static class TestProvider {
		@Nullable
		private UUID accountId;
		@Nullable
		private UUID providerId;
		@Nullable
		private UUID npvAppointmentTypeId;
		@Nullable
		private Long npvDurationInMinutes;
		@Nullable
		private UUID rpvAppointmentTypeId;
		@Nullable
		private Long rpvDurationInMinutes;

		@Nullable
		public UUID getAccountId() {
			return accountId;
		}

		public void setAccountId(@Nullable UUID accountId) {
			this.accountId = accountId;
		}

		@Nullable
		public UUID getProviderId() {
			return providerId;
		}

		public void setProviderId(@Nullable UUID providerId) {
			this.providerId = providerId;
		}

		@Nullable
		public UUID getNpvAppointmentTypeId() {
			return npvAppointmentTypeId;
		}

		public void setNpvAppointmentTypeId(@Nullable UUID npvAppointmentTypeId) {
			this.npvAppointmentTypeId = npvAppointmentTypeId;
		}

		@Nullable
		public Long getNpvDurationInMinutes() {
			return npvDurationInMinutes;
		}

		public void setNpvDurationInMinutes(@Nullable Long npvDurationInMinutes) {
			this.npvDurationInMinutes = npvDurationInMinutes;
		}

		@Nullable
		public UUID getRpvAppointmentTypeId() {
			return rpvAppointmentTypeId;
		}

		public void setRpvAppointmentTypeId(@Nullable UUID rpvAppointmentTypeId) {
			this.rpvAppointmentTypeId = rpvAppointmentTypeId;
		}

		@Nullable
		public Long getRpvDurationInMinutes() {
			return rpvDurationInMinutes;
		}

		public void setRpvDurationInMinutes(@Nullable Long rpvDurationInMinutes) {
			this.rpvDurationInMinutes = rpvDurationInMinutes;
		}
	}
}
