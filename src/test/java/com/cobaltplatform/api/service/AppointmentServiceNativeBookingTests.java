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
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.model.api.request.CreateAppointmentRequest;
import com.cobaltplatform.api.model.api.request.CreateAppointmentRequest.BookingExperienceId;
import com.cobaltplatform.api.model.api.request.CreateAppointmentTypeRequest;
import com.cobaltplatform.api.model.api.request.CreateLogicalAvailabilityRequest;
import com.cobaltplatform.api.model.api.request.UpdateAppointmentRequest;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityId;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.LogicalAvailabilityType.LogicalAvailabilityTypeId;
import com.cobaltplatform.api.model.db.RecurrenceType.RecurrenceTypeId;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.VideoconferencePlatform.VideoconferencePlatformId;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.pyranid.Database;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Database-backed coverage for the native V2 booking serialization boundary.
 *
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AppointmentServiceNativeBookingTests {
	@Nonnull
	private static final ZoneId PROVIDER_TIME_ZONE = ZoneId.of("America/New_York");
	@Nonnull
	private static final LocalTime FIRST_OVERLAPPING_START_TIME = LocalTime.of(10, 0);
	@Nonnull
	private static final LocalTime SECOND_OVERLAPPING_START_TIME = LocalTime.of(10, 30);

	@Test(timeout = 120_000L)
	public void nativeV2SequentialDuplicateCreateCommitsOnlyOneAppointment() {
		IntegrationTestExecutor.run((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AvailabilityService availabilityService = app.getInjector().getInstance(AvailabilityService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			NativeBookingFixture fixture = createCommittedFixture(database, appointmentService, availabilityService, account);

			try {
				AtomicReference<UUID> firstAppointmentId = new AtomicReference<>();
				database.transaction(() -> firstAppointmentId.set(appointmentService.createAppointment(
						requestFor(fixture, FIRST_OVERLAPPING_START_TIME))));

				AtomicReference<UUID> duplicateAppointmentId = new AtomicReference<>();
				ValidationException duplicateFailure = null;

				try {
					database.transaction(() -> duplicateAppointmentId.set(appointmentService.createAppointment(
							requestFor(fixture, FIRST_OVERLAPPING_START_TIME))));
				} catch (ValidationException e) {
					duplicateFailure = e;
				}

				assertNotNull(firstAppointmentId.get());
				assertNull(duplicateAppointmentId.get());
				assertTimeslotUnavailable(duplicateFailure);
				assertEquals(Long.valueOf(1L), activeNativeAppointmentCount(database, fixture));
			} finally {
				cleanupCommittedFixture(database, fixture);
			}
		});
	}

	@Test(timeout = 120_000L)
	public void nativeV2ConcurrentOverlappingCreatesSerializeByProvider() {
		IntegrationTestExecutor.run((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AvailabilityService availabilityService = app.getInjector().getInstance(AvailabilityService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			NativeBookingFixture fixture = createCommittedFixture(database, appointmentService, availabilityService, account);
			ExecutorService executorService = Executors.newFixedThreadPool(3);
			CountDownLatch controlLockHeld = new CountDownLatch(1);
			CountDownLatch releaseControlLock = new CountDownLatch(1);
			CountDownLatch workersReady = new CountDownLatch(2);
			CountDownLatch startWorkers = new CountDownLatch(1);
			CountDownLatch lockWaiterCheckCompleted = new CountDownLatch(1);
			AtomicReference<Throwable> lockWaiterCheckFailure = new AtomicReference<>();
			Future<?> controlFuture = null;
			Future<NativeBookingAttempt> firstWorker = null;
			Future<NativeBookingAttempt> secondWorker = null;

			try {
				controlFuture = executorService.submit(() -> database.transaction(() -> {
					assertTrue(appointmentService.acquireNativeAppointmentProviderLock(fixture.getProviderId()));
					controlLockHeld.countDown();

					try {
						waitForProviderLockWaiters(database, fixture.getProviderId(), 2L);
					} catch (Throwable t) {
						lockWaiterCheckFailure.set(t);
					} finally {
						lockWaiterCheckCompleted.countDown();
					}

					await(releaseControlLock, 30L, "Timed out waiting to release the control provider lock.");
				}));

				assertTrue("Timed out waiting for the control provider lock.",
						controlLockHeld.await(10L, TimeUnit.SECONDS));

				firstWorker = executorService.submit(() -> {
					workersReady.countDown();
					await(startWorkers, 10L, "Timed out waiting to start the first booking worker.");
					return createAppointmentInOwnTransaction(database, appointmentService, currentContextExecutor,
							account, requestFor(fixture, FIRST_OVERLAPPING_START_TIME));
				});
				secondWorker = executorService.submit(() -> {
					workersReady.countDown();
					await(startWorkers, 10L, "Timed out waiting to start the second booking worker.");
					return createAppointmentInOwnTransaction(database, appointmentService, currentContextExecutor,
							account, requestFor(fixture, SECOND_OVERLAPPING_START_TIME));
				});

				assertTrue("Timed out waiting for both booking workers.", workersReady.await(10L, TimeUnit.SECONDS));
				startWorkers.countDown();
				assertTrue("Timed out waiting for both booking workers to queue on the provider lock.",
						lockWaiterCheckCompleted.await(20L, TimeUnit.SECONDS));

				if (lockWaiterCheckFailure.get() != null)
					throw new AssertionError("Could not observe both booking workers waiting on the provider advisory lock.",
							lockWaiterCheckFailure.get());

				releaseControlLock.countDown();
				controlFuture.get(10L, TimeUnit.SECONDS);
				NativeBookingAttempt firstAttempt = firstWorker.get(30L, TimeUnit.SECONDS);
				NativeBookingAttempt secondAttempt = secondWorker.get(30L, TimeUnit.SECONDS);

				long successfulAttemptCount = List.of(firstAttempt, secondAttempt).stream()
						.filter(attempt -> attempt.getAppointmentId() != null)
						.count();
				long unavailableAttemptCount = List.of(firstAttempt, secondAttempt).stream()
						.filter(attempt -> attempt.getValidationException() != null)
						.peek(attempt -> assertTimeslotUnavailable(attempt.getValidationException()))
						.count();

				assertEquals(1L, successfulAttemptCount);
				assertEquals(1L, unavailableAttemptCount);
				assertEquals(Long.valueOf(1L), activeNativeAppointmentCount(database, fixture));
			} finally {
				startWorkers.countDown();
				releaseControlLock.countDown();
				waitForFutureToStop(controlFuture);
				waitForFutureToStop(firstWorker);
				waitForFutureToStop(secondWorker);
				executorService.shutdownNow();
				executorService.awaitTermination(10L, TimeUnit.SECONDS);
				cleanupCommittedFixture(database, fixture);
			}
		});
	}

	@Test(timeout = 120_000L)
	public void nativeV2SameStartRescheduleAtomicallyReplacesOriginal() {
		IntegrationTestExecutor.run((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AvailabilityService availabilityService = app.getInjector().getInstance(AvailabilityService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			NativeBookingFixture fixture = createCommittedFixture(database, appointmentService, availabilityService, account);

			try {
				AtomicReference<UUID> originalAppointmentId = new AtomicReference<>();
				database.transaction(() -> originalAppointmentId.set(appointmentService.createAppointment(
						requestFor(fixture, FIRST_OVERLAPPING_START_TIME))));

				AtomicReference<UUID> replacementAppointmentId = new AtomicReference<>();
				database.transaction(() -> replacementAppointmentId.set(appointmentService.rescheduleAppointment(
						rescheduleRequestFor(fixture, originalAppointmentId.get(), FIRST_OVERLAPPING_START_TIME))));

				Appointment originalAppointment = appointmentService.findAppointmentById(originalAppointmentId.get()).get();
				Appointment replacementAppointment = appointmentService.findAppointmentById(replacementAppointmentId.get()).get();

				assertTrue(originalAppointment.getCanceled());
				assertEquals(replacementAppointmentId.get(), originalAppointment.getRescheduledAppointmentId());
				assertFalse(replacementAppointment.getCanceled());
				assertEquals(Long.valueOf(1L), activeNativeAppointmentCount(database, fixture));
			} finally {
				cleanupCommittedFixture(database, fixture);
			}
		});
	}

	@Nonnull
	protected static NativeBookingFixture createCommittedFixture(@Nonnull Database database,
																								 @Nonnull AppointmentService appointmentService,
																								 @Nonnull AvailabilityService availabilityService,
																								 @Nonnull Account account) {
		AtomicReference<NativeBookingFixture> fixtureHolder = new AtomicReference<>();
		database.transaction(() -> fixtureHolder.set(createFixture(database, appointmentService, availabilityService, account)));
		return fixtureHolder.get();
	}

	@Nonnull
	protected static NativeBookingFixture createFixture(@Nonnull Database database,
																					 @Nonnull AppointmentService appointmentService,
																					 @Nonnull AvailabilityService availabilityService,
																					 @Nonnull Account account) {
		NativeBookingFixture fixture = new NativeBookingFixture();
		fixture.setAccountId(account.getAccountId());
		fixture.setProviderId(UUID.randomUUID());
		fixture.setEmailVerificationId(UUID.randomUUID());
		fixture.setEmailAddress(format("native-booking-%s@example.com", fixture.getProviderId()));
		fixture.setBookingDate(LocalDate.now(PROVIDER_TIME_ZONE).plusDays(14L));
		fixture.setOriginalBookingV2Enabled(database.queryForObject("""
				SELECT booking_v2_enabled
				FROM institution
				WHERE institution_id=?
				""", Boolean.class, InstitutionId.COBALT).get());
		fixture.setOriginalAppointmentFeedbackSurveyEnabled(database.queryForObject("""
				SELECT appointment_feedback_survey_enabled
				FROM institution
				WHERE institution_id=?
				""", Boolean.class, InstitutionId.COBALT).get());

		database.execute("""
				UPDATE institution
				SET booking_v2_enabled=TRUE,
				    appointment_feedback_survey_enabled=FALSE
				WHERE institution_id=?
				""", InstitutionId.COBALT);
		database.execute("""
				INSERT INTO account_email_verification (
				  account_email_verification_id, account_id, code, email_address, verified
				) VALUES (?, ?, ?, ?, TRUE)
				""", fixture.getEmailVerificationId(), fixture.getAccountId(), "123456", fixture.getEmailAddress());
		database.execute("""
				INSERT INTO provider (
				  provider_id, institution_id, name, email_address, url_name, locale, time_zone,
				  scheduling_system_id, videoconference_platform_id, active
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)
				""", fixture.getProviderId(), InstitutionId.COBALT,
				"Native Booking Test Provider " + fixture.getProviderId(),
				"native-booking-provider-" + fixture.getProviderId() + "@example.com",
				"native-booking-provider-" + fixture.getProviderId(), "en-US", PROVIDER_TIME_ZONE.getId(),
				SchedulingSystemId.COBALT, VideoconferencePlatformId.SWITCHBOARD);

		fixture.setAppointmentTypeId(appointmentService.createAppointmentType(
				appointmentTypeRequest(fixture.getProviderId(), "Native Booking 60 Minute Visit", 60L)));
		fixture.setGridAppointmentTypeId(appointmentService.createAppointmentType(
				appointmentTypeRequest(fixture.getProviderId(), "Native Booking 30 Minute Grid", 30L)));

		CreateLogicalAvailabilityRequest availabilityRequest = new CreateLogicalAvailabilityRequest();
		availabilityRequest.setProviderId(fixture.getProviderId());
		availabilityRequest.setAccountId(fixture.getAccountId());
		availabilityRequest.setLogicalAvailabilityTypeId(LogicalAvailabilityTypeId.OPEN);
		availabilityRequest.setRecurrenceTypeId(RecurrenceTypeId.NONE);
		availabilityRequest.setAppointmentTypeIds(List.of(fixture.getAppointmentTypeId(), fixture.getGridAppointmentTypeId()));
		availabilityRequest.setStartDateTime(LocalDateTime.of(fixture.getBookingDate(), LocalTime.of(9, 0)));
		availabilityRequest.setEndDate(fixture.getBookingDate());
		availabilityRequest.setEndTime(LocalTime.of(13, 0));
		fixture.setLogicalAvailabilityId(availabilityService.createLogicalAvailability(availabilityRequest));

		return fixture;
	}

	@Nonnull
	protected static CreateAppointmentTypeRequest appointmentTypeRequest(@Nonnull UUID providerId,
																											 @Nonnull String name,
																											 @Nonnull Long durationInMinutes) {
		CreateAppointmentTypeRequest request = new CreateAppointmentTypeRequest();
		request.setProviderId(providerId);
		request.setSchedulingSystemId(SchedulingSystemId.COBALT);
		request.setVisitTypeId(VisitTypeId.INITIAL);
		request.setName(name);
		request.setDescription(name);
		request.setDurationInMinutes(durationInMinutes);
		request.setHexColor("#336699");
		request.setPatientIntakeQuestions(Collections.emptyList());
		request.setScreeningQuestions(Collections.emptyList());
		return request;
	}

	@Nonnull
	protected static CreateAppointmentRequest requestFor(@Nonnull NativeBookingFixture fixture,
																							 @Nonnull LocalTime appointmentTime) {
		CreateAppointmentRequest request = new CreateAppointmentRequest();
		request.setAccountId(fixture.getAccountId());
		request.setCreatedByAcountId(fixture.getAccountId());
		request.setProviderId(fixture.getProviderId());
		request.setAppointmentTypeId(fixture.getAppointmentTypeId());
		request.setDate(fixture.getBookingDate());
		request.setTime(appointmentTime);
		request.setFirstName("Native");
		request.setLastName("Booking Test");
		request.setEmailAddress(fixture.getEmailAddress());
		request.setPhoneNumber("+12155550123");
		request.setBookingExperienceId(BookingExperienceId.V2);
		request.setAppointmentModalityId(ProviderAppointmentModalityId.VIRTUAL);
		return request;
	}

	@Nonnull
	protected static UpdateAppointmentRequest rescheduleRequestFor(@Nonnull NativeBookingFixture fixture,
																														@Nonnull UUID appointmentId,
																														@Nonnull LocalTime appointmentTime) {
		UpdateAppointmentRequest request = new UpdateAppointmentRequest();
		request.setAppointmentId(appointmentId);
		request.setAccountId(fixture.getAccountId());
		request.setCreatedByAcountId(fixture.getAccountId());
		request.setProviderId(fixture.getProviderId());
		request.setAppointmentTypeId(fixture.getAppointmentTypeId());
		request.setDate(fixture.getBookingDate());
		request.setTime(appointmentTime);
		request.setFirstName("Native");
		request.setLastName("Booking Test");
		request.setEmailAddress(fixture.getEmailAddress());
		request.setPhoneNumber("+12155550123");
		request.setBookingExperienceId(BookingExperienceId.V2);
		request.setAppointmentModalityId(ProviderAppointmentModalityId.VIRTUAL);
		return request;
	}

	@Nonnull
	protected static NativeBookingAttempt createAppointmentInOwnTransaction(@Nonnull Database database,
																												 @Nonnull AppointmentService appointmentService,
																												 @Nonnull CurrentContextExecutor currentContextExecutor,
																												 @Nonnull Account account,
																												 @Nonnull CreateAppointmentRequest request) {
		AtomicReference<NativeBookingAttempt> attemptHolder = new AtomicReference<>();
		CurrentContext currentContext = new CurrentContext.Builder(account, Locale.US, PROVIDER_TIME_ZONE).build();

		currentContextExecutor.execute(currentContext, () -> {
			AtomicReference<UUID> appointmentId = new AtomicReference<>();

			try {
				database.transaction(() -> appointmentId.set(appointmentService.createAppointment(request)));
				attemptHolder.set(NativeBookingAttempt.success(appointmentId.get()));
			} catch (ValidationException e) {
				attemptHolder.set(NativeBookingAttempt.failure(e));
			}
		});

		return attemptHolder.get();
	}

	protected static void waitForProviderLockWaiters(@Nonnull Database database,
																							 @Nonnull UUID providerId,
																							 long expectedWaiterCount) {
		String lockKey = format("appointment|%s", providerId);
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15L);

		while (System.nanoTime() < deadline) {
			Long waiterCount = database.queryForObject("""
					SELECT COUNT(*)
					FROM pg_locks
					WHERE locktype='advisory'
					AND granted=FALSE
					AND classid::BIGINT=((hashtextextended(?, 0) >> 32) & 4294967295)
					AND objid::BIGINT=(hashtextextended(?, 0) & 4294967295)
					""", Long.class, lockKey, lockKey).get();

			if (waiterCount >= expectedWaiterCount)
				return;

			try {
				Thread.sleep(25L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("Interrupted while waiting for provider advisory lock waiters.", e);
			}
		}

		throw new AssertionError(format("Expected %d waiters on provider advisory lock for provider ID %s.",
				expectedWaiterCount, providerId));
	}

	protected static void assertTimeslotUnavailable(ValidationException validationException) {
		assertNotNull(validationException);
		assertEquals(Boolean.TRUE, validationException.getMetadata().get("appointmentTimeslotUnavailable"));
	}

	@Nonnull
	protected static Long activeNativeAppointmentCount(@Nonnull Database database,
																							 @Nonnull NativeBookingFixture fixture) {
		return database.queryForObject("""
				SELECT COUNT(*)
				FROM appointment
				WHERE provider_id=?
				AND canceled=FALSE
				AND scheduling_system_id=?
				""", Long.class, fixture.getProviderId(), SchedulingSystemId.COBALT).get();
	}

	protected static void cleanupCommittedFixture(@Nonnull Database database,
																							 @Nonnull NativeBookingFixture fixture) {
		RuntimeException cleanupFailure = null;

		try {
			database.transaction(() -> {
				database.execute("DELETE FROM appointment_interaction_instance WHERE appointment_id IN (SELECT appointment_id FROM appointment WHERE provider_id=?)",
						fixture.getProviderId());
				database.execute("DELETE FROM appointment_scheduled_message WHERE appointment_id IN (SELECT appointment_id FROM appointment WHERE provider_id=?)",
						fixture.getProviderId());
				database.execute("DELETE FROM appointment WHERE provider_id=?", fixture.getProviderId());
				database.execute("DELETE FROM appointment_booking_failure WHERE provider_id=?", fixture.getProviderId());
				database.execute("DELETE FROM logical_availability_appointment_type WHERE logical_availability_id=?",
						fixture.getLogicalAvailabilityId());
				database.execute("DELETE FROM logical_availability WHERE logical_availability_id=?",
						fixture.getLogicalAvailabilityId());
				database.execute("DELETE FROM provider_appointment_type WHERE provider_id=?", fixture.getProviderId());
				database.execute("DELETE FROM appointment_type WHERE appointment_type_id IN (?, ?)",
						fixture.getAppointmentTypeId(), fixture.getGridAppointmentTypeId());
				database.execute("DELETE FROM provider WHERE provider_id=?", fixture.getProviderId());
				database.execute("DELETE FROM account_email_verification WHERE account_email_verification_id=?",
						fixture.getEmailVerificationId());
			});
		} catch (RuntimeException e) {
			cleanupFailure = e;
		} finally {
			database.transaction(() -> database.execute("""
					UPDATE institution
					SET booking_v2_enabled=?,
					    appointment_feedback_survey_enabled=?
					WHERE institution_id=?
					""", fixture.getOriginalBookingV2Enabled(), fixture.getOriginalAppointmentFeedbackSurveyEnabled(),
					InstitutionId.COBALT));
		}

		if (cleanupFailure != null)
			throw cleanupFailure;
	}

	protected static void await(@Nonnull CountDownLatch latch,
														long timeoutInSeconds,
														@Nonnull String timeoutMessage) {
		try {
			if (!latch.await(timeoutInSeconds, TimeUnit.SECONDS))
				throw new AssertionError(timeoutMessage);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while waiting on a test latch.", e);
		}
	}

	protected static void waitForFutureToStop(Future<?> future) {
		if (future == null)
			return;

		try {
			future.get(30L, TimeUnit.SECONDS);
		} catch (Exception ignored) {
			future.cancel(true);
		}
	}

	@ThreadSafe
	protected static class NativeBookingAttempt {
		private final UUID appointmentId;
		private final ValidationException validationException;

		protected NativeBookingAttempt(UUID appointmentId,
																 ValidationException validationException) {
			this.appointmentId = appointmentId;
			this.validationException = validationException;
		}

		@Nonnull
		public static NativeBookingAttempt success(@Nonnull UUID appointmentId) {
			return new NativeBookingAttempt(appointmentId, null);
		}

		@Nonnull
		public static NativeBookingAttempt failure(@Nonnull ValidationException validationException) {
			return new NativeBookingAttempt(null, validationException);
		}

		public UUID getAppointmentId() {
			return this.appointmentId;
		}

		public ValidationException getValidationException() {
			return this.validationException;
		}
	}

	@NotThreadSafe
	protected static class NativeBookingFixture {
		private UUID accountId;
		private UUID providerId;
		private UUID appointmentTypeId;
		private UUID gridAppointmentTypeId;
		private UUID logicalAvailabilityId;
		private UUID emailVerificationId;
		private String emailAddress;
		private LocalDate bookingDate;
		private Boolean originalBookingV2Enabled;
		private Boolean originalAppointmentFeedbackSurveyEnabled;

		public UUID getAccountId() {
			return this.accountId;
		}

		public void setAccountId(UUID accountId) {
			this.accountId = accountId;
		}

		public UUID getProviderId() {
			return this.providerId;
		}

		public void setProviderId(UUID providerId) {
			this.providerId = providerId;
		}

		public UUID getAppointmentTypeId() {
			return this.appointmentTypeId;
		}

		public void setAppointmentTypeId(UUID appointmentTypeId) {
			this.appointmentTypeId = appointmentTypeId;
		}

		public UUID getGridAppointmentTypeId() {
			return this.gridAppointmentTypeId;
		}

		public void setGridAppointmentTypeId(UUID gridAppointmentTypeId) {
			this.gridAppointmentTypeId = gridAppointmentTypeId;
		}

		public UUID getLogicalAvailabilityId() {
			return this.logicalAvailabilityId;
		}

		public void setLogicalAvailabilityId(UUID logicalAvailabilityId) {
			this.logicalAvailabilityId = logicalAvailabilityId;
		}

		public UUID getEmailVerificationId() {
			return this.emailVerificationId;
		}

		public void setEmailVerificationId(UUID emailVerificationId) {
			this.emailVerificationId = emailVerificationId;
		}

		public String getEmailAddress() {
			return this.emailAddress;
		}

		public void setEmailAddress(String emailAddress) {
			this.emailAddress = emailAddress;
		}

		public LocalDate getBookingDate() {
			return this.bookingDate;
		}

		public void setBookingDate(LocalDate bookingDate) {
			this.bookingDate = bookingDate;
		}

		public Boolean getOriginalBookingV2Enabled() {
			return this.originalBookingV2Enabled;
		}

		public void setOriginalBookingV2Enabled(Boolean originalBookingV2Enabled) {
			this.originalBookingV2Enabled = originalBookingV2Enabled;
		}

		public Boolean getOriginalAppointmentFeedbackSurveyEnabled() {
			return this.originalAppointmentFeedbackSurveyEnabled;
		}

		public void setOriginalAppointmentFeedbackSurveyEnabled(Boolean originalAppointmentFeedbackSurveyEnabled) {
			this.originalAppointmentFeedbackSurveyEnabled = originalAppointmentFeedbackSurveyEnabled;
		}
	}
}
