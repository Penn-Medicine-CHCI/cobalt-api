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

package com.cobaltplatform.api.integration.acuity;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.integration.acuity.model.AcuityAppointmentType;
import com.cobaltplatform.api.integration.acuity.model.AcuityTime;
import com.cobaltplatform.api.integration.common.ProviderAvailabilitySyncManager;
import com.cobaltplatform.api.model.api.request.CreateAcuityAppointmentTypeRequest;
import com.cobaltplatform.api.model.api.request.UpdateAcuityAppointmentTypeRequest;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.service.AppointmentService;
import com.cobaltplatform.api.service.ProviderService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class AcuitySyncManager implements ProviderAvailabilitySyncManager, AutoCloseable {
	@Nonnull
	private static final Integer AVAILABILITY_SYNC_NUMBER_OF_DAYS_AHEAD;
	@Nonnull
	private static final Long AVAILABILITY_SYNC_INTERVAL_IN_SECONDS;
	@Nonnull
	private static final Long AVAILABILITY_SYNC_INITIAL_DELAY_IN_SECONDS;
	@Nonnull
	private static final Long APPOINTMENT_TYPE_SYNC_INTERVAL_IN_SECONDS;
	@Nonnull
	private static final Long APPOINTMENT_TYPE_SYNC_INITIAL_DELAY_IN_SECONDS;

	@Nonnull
	private final javax.inject.Provider<AvailabilitySyncTask> availabilitySyncTaskProvider;
	@Nonnull
	private final javax.inject.Provider<AppointmentTypeSyncTask> appointmentTypeSyncTaskProvider;
	@Nonnull
	private final javax.inject.Provider<ProviderService> providerServiceProvider;
	@Nonnull
	private final javax.inject.Provider<AppointmentService> appointmentServiceProvider;
	@Nonnull
	private final AcuitySchedulingClient acuitySchedulingClient;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Object acuitySyncLock;
	@Nonnull
	private final Logger logger;

	@Nonnull
	private Boolean started;
	@Nullable
	private ScheduledExecutorService availabilitySyncExecutorService;
	@Nullable
	private ScheduledExecutorService appointmentTypeSyncExecutorService;

	static {
		AVAILABILITY_SYNC_NUMBER_OF_DAYS_AHEAD = 50; // 7 weeks and 1 day
		AVAILABILITY_SYNC_INTERVAL_IN_SECONDS = 60L * 10;
		AVAILABILITY_SYNC_INITIAL_DELAY_IN_SECONDS = 30L;
		APPOINTMENT_TYPE_SYNC_INTERVAL_IN_SECONDS = 60L * 5;
		APPOINTMENT_TYPE_SYNC_INITIAL_DELAY_IN_SECONDS = 10L;
	}

	@Inject
	public AcuitySyncManager(@Nonnull javax.inject.Provider<AvailabilitySyncTask> availabilitySyncTaskProvider,
													 @Nonnull javax.inject.Provider<AppointmentTypeSyncTask> appointmentTypeSyncTaskProvider,
													 @Nonnull javax.inject.Provider<ProviderService> providerServiceProvider,
													 @Nonnull javax.inject.Provider<AppointmentService> appointmentServiceProvider,
													 @Nonnull AcuitySchedulingClient acuitySchedulingClient,
													 @Nonnull Database database,
													 @Nonnull Configuration configuration,
													 @Nonnull Strings strings) {
		requireNonNull(availabilitySyncTaskProvider);
		requireNonNull(appointmentTypeSyncTaskProvider);
		requireNonNull(providerServiceProvider);
		requireNonNull(appointmentServiceProvider);
		requireNonNull(acuitySchedulingClient);
		requireNonNull(database);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.availabilitySyncTaskProvider = availabilitySyncTaskProvider;
		this.appointmentTypeSyncTaskProvider = appointmentTypeSyncTaskProvider;
		this.providerServiceProvider = providerServiceProvider;
		this.appointmentServiceProvider = appointmentServiceProvider;
		this.acuitySchedulingClient = acuitySchedulingClient;
		this.database = database;
		this.configuration = configuration;
		this.strings = strings;
		this.acuitySyncLock = new Object();
		this.started = false;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public void close() throws Exception {
		stop();
	}

	@Nonnull
	public Boolean start() {
		synchronized (getAcuitySyncLock()) {
			if (isStarted())
				return false;

			getLogger().trace("Starting Acuity sync...");

			this.availabilitySyncExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("acuity-availability-sync-task").build());
			this.appointmentTypeSyncExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("acuity-appointment-type-sync-task").build());

			this.started = true;

			getAvailabilitySyncExecutorService().get().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						getAvailabilitySyncTaskProvider().get().run();
					} catch (Exception e) {
						getLogger().warn(format("Unable to sync Acuity provider availability - will retry in %s seconds", String.valueOf(getAvailabilitySyncIntervalInSeconds())), e);
					}
				}
			}, getAvailabilitySyncInitialDelayInSeconds(), getAvailabilitySyncIntervalInSeconds(), TimeUnit.SECONDS);

			getAppointmentTypeSyncExecutorService().get().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						getAppointmentTypeSyncTaskProvider().get().run();
					} catch (Exception e) {
						getLogger().warn(format("Unable to sync Acuity appointment types - will retry in %s seconds", String.valueOf(getAppointmentTypeSyncIntervalInSeconds())), e);
					}
				}
			}, getAppointmentTypeSyncInitialDelayInSeconds(), getAppointmentTypeSyncIntervalInSeconds(), TimeUnit.SECONDS);

			getLogger().trace("Acuity sync started.");

			return true;
		}
	}

	@Nonnull
	public Boolean stop() {
		synchronized (getAcuitySyncLock()) {
			if (!isStarted())
				return false;

			getLogger().trace("Stopping Acuity sync...");

			getAvailabilitySyncExecutorService().get().shutdownNow();
			this.availabilitySyncExecutorService = null;

			getAppointmentTypeSyncExecutorService().get().shutdownNow();
			this.appointmentTypeSyncExecutorService = null;

			this.started = false;

			getLogger().trace("Acuity sync stopped.");

			return true;
		}
	}

	/**
	 * Forces re-sync of a provider's availability for a particular day by pulling from Acuity and writing records to our DB.
	 * <p>
	 * This should be called if an appointment is booked through us or we get a webhook notification that one happened.
	 * <p>
	 * This way we don't have to wait for the next automatic re-sync, which might be a little ways in the future.
	 *
	 * @param providerId              the provider to sync
	 * @param date                    the date that should be synced
	 * @param performInOwnTransaction should this be performed in its own transaction to minimize time that the
	 *                                transaction is open?  we normally want this, as sync is run out-of-band, and
	 *                                doesn't need to participate in an existing transaction
	 * @return {@code true} if the sync was performed, {@code false} otherwise
	 */
	@Nonnull
	@Override
	public Boolean syncProviderAvailability(@Nonnull UUID providerId,
																					@Nonnull LocalDate date,
																					@Nonnull Boolean performInOwnTransaction) {
		requireNonNull(providerId);
		requireNonNull(date);
		requireNonNull(performInOwnTransaction);

		Provider provider = getProviderService().findProviderById(providerId).orElse(null);

		if (provider == null) {
			getLogger().warn("No provider found with ID {}, ignoring request to sync", providerId);
			return false;
		}

		getLogger().info("Syncing availabilty for provider {} on {}...", provider.getName(), date);

		ProviderAvailabilityDateInsert insert = generateProviderAvailabilityDateInsert(provider, date);

		if (performInOwnTransaction)
			getDatabase().transaction(() -> {
				performProviderAvailabilityDateInsert(insert);
			});
		else
			performProviderAvailabilityDateInsert(insert);

		return true;
	}

	@Nonnull
	protected ProviderAvailabilityDateInsert generateProviderAvailabilityDateInsert(@Nonnull Provider provider,
																																									@Nonnull LocalDate date) {
		requireNonNull(provider);
		requireNonNull(date);

		List<AppointmentType> appointmentTypes = getAppointmentService().findAppointmentTypesByProviderId(provider.getProviderId()).stream()
				.filter(appointmentType -> appointmentType.getSchedulingSystemId().equals(SchedulingSystemId.ACUITY))
				.collect(Collectors.toList());

		List<ProviderAvailabilityDateInsertRow> rows = new ArrayList<>();

		for (AppointmentType appointmentType : appointmentTypes) {
			List<AcuityTime> acuityTimes = getAcuitySchedulingClient().findAvailabilityTimes(provider.getAcuityCalendarId(), appointmentType.getAcuityAppointmentTypeId(), date, provider.getTimeZone());

			for (AcuityTime acuityTime : acuityTimes) {
				Instant dateTimeInstant = getAcuitySchedulingClient().parseAcuityTime(acuityTime.getTime());
				LocalDateTime dateTime = LocalDateTime.ofInstant(dateTimeInstant, provider.getTimeZone());

				ProviderAvailabilityDateInsertRow row = new ProviderAvailabilityDateInsertRow();
				row.setAppointmentTypeId(appointmentType.getAppointmentTypeId());
				row.setDateTime(dateTime);
				rows.add(row);
			}
		}

		return new ProviderAvailabilityDateInsert(provider.getProviderId(), date, provider.getTimeZone(), rows);
	}

	protected void performProviderAvailabilityDateInsert(@Nonnull ProviderAvailabilityDateInsert insert) {
		requireNonNull(insert);

		// To keep historical data, never change anything in the past
		LocalDateTime currentDateTime = LocalDateTime.now(insert.getTimeZone());
		LocalDate insertDate = insert.getDate();
		boolean today = currentDateTime.toLocalDate().equals(insertDate);

		if (insertDate.isBefore(currentDateTime.toLocalDate())) {
			getLogger().info("Ignoring provider sync request for {} because it's for a date in the past: {}", insert.getProviderId(), insertDate);
			return;
		}

		if (today) {
			// 1. This is "today" from the provider's perspective - clear out anything after right now for today
			LocalDateTime endOfDayDateTime = LocalDateTime.of(currentDateTime.toLocalDate(), LocalTime.MAX); // Insert Date @ 23:59:59.999999999
			getLogger().info("Provider ID {} is being synced for 'today' - removing any availability between {} and {}...", insert.getProviderId(), currentDateTime, endOfDayDateTime);
			getDatabase().execute("DELETE FROM provider_availability WHERE provider_id=? AND date_time > ? AND date_time <= ?", insert.getProviderId(), currentDateTime, endOfDayDateTime);
		} else {
			// 1. This is a future date - clear out the whole day
			getDatabase().execute("DELETE FROM provider_availability WHERE provider_id=? AND date_time::date=?", insert.getProviderId(), insert.getDate());
		}

		// 2. Insert new ones for the day (in batch)
		List<List<Object>> parameterGroups = new ArrayList<>(insert.getRows().size());

		for (ProviderAvailabilityDateInsertRow row : insert.getRows()) {
			if (today && row.getDateTime().isBefore(currentDateTime)) {
				getLogger().info("Provider ID {} is being synced for 'today', so ignore availability insert for {} because it's before now ({})...", insert.getProviderId(), row.getDateTime(), currentDateTime);
				continue;
			}

			List<Object> parameterGroup = new ArrayList<>(3);
			parameterGroup.add(insert.getProviderId());
			parameterGroup.add(row.getAppointmentTypeId());
			parameterGroup.add(row.getDateTime());
			parameterGroups.add(parameterGroup);
		}

		getDatabase().executeBatch("INSERT INTO provider_availability(provider_id, appointment_type_id, date_time) VALUES (?,?,?)", parameterGroups);
	}

	protected void performDebugLogging(@Nonnull Provider provider,
																		 @Nonnull ProviderAvailabilityDateInsert insert) {
		requireNonNull(provider);
		requireNonNull(insert);

		// Example output:
		//
		// Apr-26-2020 2:22:33.190 PM EDT [acuity-sync-task] DEBUG AvailabilityService$AcuitySyncTask:243 [] Dr. Mark Allen availability for 2020-04-27:
		// Acuity Appointment Type ID 13757193: [09:00, 09:30, 10:00, 10:30, 11:00, 11:30, 12:00, 12:30, 13:00, 13:30, 14:00, 14:30, 15:00, 15:30, 16:00]
		// Acuity Appointment Type ID 14051518: [09:00, 09:30, 10:00, 10:30, 11:00, 11:30, 12:00, 12:30, 13:00, 13:30, 14:00, 14:30, 15:00, 15:30, 16:00, 16:30]

		Map<UUID, List<LocalDateTime>> dateTimesByAppointmentTypeId = new TreeMap<>();

		for (ProviderAvailabilityDateInsertRow row : insert.getRows()) {
			List<LocalDateTime> dateTimes = dateTimesByAppointmentTypeId.get(row.getAppointmentTypeId());

			if (dateTimes == null) {
				dateTimes = new ArrayList<>();
				dateTimesByAppointmentTypeId.put(row.getAppointmentTypeId(), dateTimes);
			}

			dateTimes.add(row.getDateTime());
		}

		List<String> providerDescriptionComponents = new ArrayList<>(1 + dateTimesByAppointmentTypeId.size());

		providerDescriptionComponents.add(format("%s availability for %s:", provider.getName(), insert.getDate()));

		for (Entry<UUID, List<LocalDateTime>> entry : dateTimesByAppointmentTypeId.entrySet())
			providerDescriptionComponents.add(format("Appointment Type ID %s: %s", entry.getKey(), entry.getValue().stream()
					.map(localDateTime -> localDateTime.toLocalTime())
					.sorted()
					.collect(Collectors.toList())));

		if (providerDescriptionComponents.size() == 1)
			providerDescriptionComponents.add("[none]");

		getLogger().debug(providerDescriptionComponents.stream().collect(Collectors.joining("\n")));
	}

	@ThreadSafe
	protected static class AvailabilitySyncTask implements Runnable {
		@Nonnull
		private final javax.inject.Provider<AcuitySyncManager> acuitySyncManager;
		@Nonnull
		private final javax.inject.Provider<ProviderService> providerServiceProvider;
		@Nonnull
		private final AcuitySchedulingClient acuitySchedulingClient;
		@Nonnull
		private final CurrentContextExecutor currentContextExecutor;
		@Nonnull
		private final Database database;
		@Nonnull
		private final Configuration configuration;
		@Nonnull
		private final Logger logger;

		@Inject
		public AvailabilitySyncTask(@Nonnull javax.inject.Provider<AcuitySyncManager> acuitySyncManager,
																@Nonnull javax.inject.Provider<ProviderService> providerServiceProvider,
																@Nonnull CurrentContextExecutor currentContextExecutor,
																@Nonnull AcuitySchedulingClient acuitySchedulingClient,
																@Nonnull Database database,
																@Nonnull Configuration configuration) {
			requireNonNull(acuitySyncManager);
			requireNonNull(providerServiceProvider);
			requireNonNull(currentContextExecutor);
			requireNonNull(acuitySchedulingClient);
			requireNonNull(database);
			requireNonNull(configuration);

			this.acuitySyncManager = acuitySyncManager;
			this.providerServiceProvider = providerServiceProvider;
			this.currentContextExecutor = currentContextExecutor;
			this.acuitySchedulingClient = acuitySchedulingClient;
			this.database = database;
			this.configuration = configuration;
			this.logger = LoggerFactory.getLogger(getClass());
		}

		@Override
		public void run() {
			CurrentContext currentContext = new CurrentContext.Builder(InstitutionId.COBALT,
					getConfiguration().getDefaultLocale(), getConfiguration().getDefaultTimeZone()).build();

			getCurrentContextExecutor().execute(currentContext, () -> {
				// Pick out all Acuity-scheduled providers
				List<Provider> providers = getProviderService().findProvidersByInstitutionId(InstitutionId.COBALT).stream()
						.filter(provider -> provider.getSchedulingSystemId().equals(SchedulingSystemId.ACUITY) && provider.getActive())
						.collect(Collectors.toList());

				// This is nuts, but Acuity has some weird undocumented syncing limits...shuffle so each sync can get a different random ordering
				// Meaning if the sync cuts off at 50 providers and we have 150 in our system, this lets us get a mostly-different set each sync
				Collections.shuffle(providers);

				getLogger().info("Running Acuity availability sync for {} providers...", providers.size());
				int providerSuccessCount = 0;

				for (Provider provider : providers) {
					try {
						LocalDate syncDate = LocalDate.now(provider.getTimeZone());
						LocalDate today = syncDate;

						List<ProviderAvailabilityDateInsert> inserts = new ArrayList<>(getAcuitySyncManager().getAvailabilitySyncNumberOfDaysAhead());

						for (int i = 0; i < getAcuitySyncManager().getAvailabilitySyncNumberOfDaysAhead(); ++i) {
							ProviderAvailabilityDateInsert insert = getAcuitySyncManager().generateProviderAvailabilityDateInsert(provider, syncDate);
							inserts.add(insert);
							syncDate = syncDate.plusDays(1);
						}

						// After we've done all the Acuity calls to pull data for this provider, commit to DB.
						// This way we keep transaction time to a minimum to reduce contention

						// For each provider-date, clear out existing availabilities and insert the new ones (if any)
						for (ProviderAvailabilityDateInsert insert : inserts) {
							// Dump out info for debugging...
							if (getLogger().isDebugEnabled())
								getAcuitySyncManager().performDebugLogging(provider, insert);

							getDatabase().transaction(() -> {
								getAcuitySyncManager().performProviderAvailabilityDateInsert(insert);
							});
						}

						++providerSuccessCount;
					} catch (AcuitySchedulingUndocumentedRateLimitException e) {
						getLogger().warn("Unable to sync provider ID {} ({}) with Acuity: {}", provider.getProviderId(), provider.getName(), e.getMessage());
					} catch (Exception e) {
						getLogger().warn(format("Unable to sync provider ID %s (%s) with Acuity", provider.getProviderId(), provider.getName()), e);
					}
				}

				getLogger().info("Acuity provider availability sync complete. Successfully synced {} of {} providers.", providerSuccessCount, providers.size());
			});
		}

		@Nonnull
		protected AcuitySyncManager getAcuitySyncManager() {
			return acuitySyncManager.get();
		}

		@Nonnull
		protected ProviderService getProviderService() {
			return providerServiceProvider.get();
		}

		@Nonnull
		protected CurrentContextExecutor getCurrentContextExecutor() {
			return currentContextExecutor;
		}

		@Nonnull
		protected AcuitySchedulingClient getAcuitySchedulingClient() {
			return acuitySchedulingClient;
		}

		@Nonnull
		protected Database getDatabase() {
			return database;
		}

		@Nonnull
		protected Configuration getConfiguration() {
			return configuration;
		}

		@Nonnull
		protected Logger getLogger() {
			return logger;
		}
	}

	@ThreadSafe
	protected static class AppointmentTypeSyncTask implements Runnable {
		@Nonnull
		private final javax.inject.Provider<AcuitySyncManager> acuitySyncManagerProvider;
		@Nonnull
		private final javax.inject.Provider<AppointmentService> appointmentServiceProvider;
		@Nonnull
		private final AcuitySchedulingClient acuitySchedulingClient;
		@Nonnull
		private final CurrentContextExecutor currentContextExecutor;
		@Nonnull
		private final Database database;
		@Nonnull
		private final Configuration configuration;
		@Nonnull
		private final Logger logger;

		@Inject
		public AppointmentTypeSyncTask(@Nonnull javax.inject.Provider<AcuitySyncManager> acuitySyncManagerProvider,
																	 @Nonnull javax.inject.Provider<AppointmentService> appointmentServiceProvider,
																	 @Nonnull CurrentContextExecutor currentContextExecutor,
																	 @Nonnull AcuitySchedulingClient acuitySchedulingClient,
																	 @Nonnull Database database,
																	 @Nonnull Configuration configuration) {
			requireNonNull(acuitySyncManagerProvider);
			requireNonNull(appointmentServiceProvider);
			requireNonNull(currentContextExecutor);
			requireNonNull(acuitySchedulingClient);
			requireNonNull(database);
			requireNonNull(configuration);

			this.acuitySyncManagerProvider = acuitySyncManagerProvider;
			this.appointmentServiceProvider = appointmentServiceProvider;
			this.currentContextExecutor = currentContextExecutor;
			this.acuitySchedulingClient = acuitySchedulingClient;
			this.database = database;
			this.configuration = configuration;
			this.logger = LoggerFactory.getLogger(getClass());
		}

		@Override
		public void run() {
			CurrentContext currentContext = new CurrentContext.Builder(InstitutionId.COBALT,
					getConfiguration().getDefaultLocale(), getConfiguration().getDefaultTimeZone()).build();

			getCurrentContextExecutor().execute(currentContext, () -> {
				getLogger().info("Running Acuity appointment type sync...");

				List<AcuityAppointmentType> acuityAppointmentTypes = getAcuitySchedulingClient().findAppointmentTypes();

				getLogger().info("There are {} Acuity appointment types.", acuityAppointmentTypes.size());

				for (AcuityAppointmentType acuityAppointmentType : acuityAppointmentTypes) {
					try {
						AppointmentType appointmentType = getAppointmentService().findAppointmentTypeByAcuityAppointmentTypeId(acuityAppointmentType.getId()).orElse(null);

						if (appointmentType == null) {
							getLogger().info("Detected new appointment type '{}' - inserting it...", acuityAppointmentType.getName());

							getDatabase().transaction(() -> {
								getAppointmentService().createAcuityAppointmentType(new CreateAcuityAppointmentTypeRequest() {{
									setAcuityAppointmentTypeId(acuityAppointmentType.getId());
									setName(acuityAppointmentType.getName());
									setDescription(acuityAppointmentType.getDescription());
									setDurationInMinutes(acuityAppointmentType.getDuration().longValue());
									setDeleted(!acuityAppointmentType.getActive());
								}});
							});
						} else {
							boolean appointmentTypesMatch = Objects.equals(trimToNull(acuityAppointmentType.getName()), appointmentType.getName())
									&& Objects.equals(trimToNull(acuityAppointmentType.getDescription()), appointmentType.getDescription())
									&& Objects.equals(acuityAppointmentType.getDuration().longValue(), appointmentType.getDurationInMinutes())
									&& Objects.equals(!acuityAppointmentType.getActive(), appointmentType.getDeleted());

							if (!appointmentTypesMatch) {
								getLogger().info("Detected a change to appointment type '{}' - updating it...", acuityAppointmentType.getName());

								getDatabase().transaction(() -> {
									getAppointmentService().updateAcuityAppointmentType(new UpdateAcuityAppointmentTypeRequest() {{
										setAppointmentTypeId(appointmentType.getAppointmentTypeId());
										setName(acuityAppointmentType.getName());
										setDescription(acuityAppointmentType.getDescription());
										setDurationInMinutes(acuityAppointmentType.getDuration().longValue());
										setDeleted(!acuityAppointmentType.getActive());
									}});
								});
							}
						}
					} catch (AcuitySchedulingUndocumentedRateLimitException e) {
						getLogger().warn("Unable to sync appointment type ID {} ({}): {}", acuityAppointmentType.getId(), acuityAppointmentType.getName(), e.getMessage());
					} catch (Exception e) {
						getLogger().warn(format("Unable to sync appointment type ID %s (%s)", acuityAppointmentType.getId(), acuityAppointmentType.getName()), e);
					}
				}

				getLogger().info("Acuity appointment type sync complete.");
			});
		}

		@Nonnull
		protected AcuitySyncManager getAcuitySyncManager() {
			return acuitySyncManagerProvider.get();
		}

		@Nonnull
		protected AppointmentService getAppointmentService() {
			return appointmentServiceProvider.get();
		}

		@Nonnull
		protected CurrentContextExecutor getCurrentContextExecutor() {
			return currentContextExecutor;
		}

		@Nonnull
		protected AcuitySchedulingClient getAcuitySchedulingClient() {
			return acuitySchedulingClient;
		}

		@Nonnull
		protected Database getDatabase() {
			return database;
		}

		@Nonnull
		protected Configuration getConfiguration() {
			return configuration;
		}

		@Nonnull
		protected Logger getLogger() {
			return logger;
		}
	}

	@NotThreadSafe
	protected static class ProviderAvailabilityDateInsertRow {
		@Nullable
		private UUID appointmentTypeId;
		@Nullable
		private LocalDateTime dateTime;

		@Nullable
		public UUID getAppointmentTypeId() {
			return appointmentTypeId;
		}

		public void setAppointmentTypeId(@Nullable UUID appointmentTypeId) {
			this.appointmentTypeId = appointmentTypeId;
		}

		@Nullable
		public LocalDateTime getDateTime() {
			return dateTime;
		}

		public void setDateTime(@Nullable LocalDateTime dateTime) {
			this.dateTime = dateTime;
		}
	}

	@NotThreadSafe
	protected static class ProviderAvailabilityDateInsert {
		@Nonnull
		private final UUID providerId;
		@Nonnull
		private final LocalDate date;
		@Nonnull
		private final ZoneId timeZone;
		@Nonnull
		private final List<ProviderAvailabilityDateInsertRow> rows;

		public ProviderAvailabilityDateInsert(@Nonnull UUID providerId,
																					@Nonnull LocalDate date,
																					@Nonnull ZoneId timeZone,
																					@Nonnull List<ProviderAvailabilityDateInsertRow> rows) {
			requireNonNull(providerId);
			requireNonNull(date);
			requireNonNull(timeZone);
			requireNonNull(rows);

			this.providerId = providerId;
			this.date = date;
			this.timeZone = timeZone;
			this.rows = rows;
		}

		@Nonnull
		public UUID getProviderId() {
			return providerId;
		}

		@Nonnull
		public LocalDate getDate() {
			return date;
		}

		@Nonnull
		public ZoneId getTimeZone() {
			return timeZone;
		}

		@Nonnull
		public List<ProviderAvailabilityDateInsertRow> getRows() {
			return rows;
		}
	}

	@Nonnull
	public Boolean isStarted() {
		synchronized (getAcuitySyncLock()) {
			return started;
		}
	}

	@Nonnull
	protected Integer getAvailabilitySyncNumberOfDaysAhead() {
		return AVAILABILITY_SYNC_NUMBER_OF_DAYS_AHEAD;
	}

	@Nonnull
	protected Long getAvailabilitySyncIntervalInSeconds() {
		return AVAILABILITY_SYNC_INTERVAL_IN_SECONDS;
	}

	@Nonnull
	protected Long getAppointmentTypeSyncIntervalInSeconds() {
		return APPOINTMENT_TYPE_SYNC_INTERVAL_IN_SECONDS;
	}

	@Nonnull
	protected Long getAvailabilitySyncInitialDelayInSeconds() {
		return AVAILABILITY_SYNC_INITIAL_DELAY_IN_SECONDS;
	}

	@Nonnull
	protected Long getAppointmentTypeSyncInitialDelayInSeconds() {
		return APPOINTMENT_TYPE_SYNC_INITIAL_DELAY_IN_SECONDS;
	}

	@Nonnull
	protected Optional<ScheduledExecutorService> getAvailabilitySyncExecutorService() {
		return Optional.ofNullable(availabilitySyncExecutorService);
	}

	@Nonnull
	protected javax.inject.Provider<AvailabilitySyncTask> getAvailabilitySyncTaskProvider() {
		return availabilitySyncTaskProvider;
	}

	@Nonnull
	protected Optional<ScheduledExecutorService> getAppointmentTypeSyncExecutorService() {
		return Optional.ofNullable(appointmentTypeSyncExecutorService);
	}

	@Nonnull
	protected javax.inject.Provider<AppointmentTypeSyncTask> getAppointmentTypeSyncTaskProvider() {
		return appointmentTypeSyncTaskProvider;
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return providerServiceProvider.get();
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return appointmentServiceProvider.get();
	}

	@Nonnull
	protected AcuitySchedulingClient getAcuitySchedulingClient() {
		return acuitySchedulingClient;
	}

	@Nonnull
	protected Database getDatabase() {
		return database;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected Object getAcuitySyncLock() {
		return acuitySyncLock;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}