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
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.integration.common.ProviderAvailabilitySyncManager;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.integration.epic.request.GetProviderScheduleRequest;
import com.cobaltplatform.api.integration.epic.response.GetProviderScheduleResponse;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.EpicAppointmentFilter.EpicAppointmentFilterId;
import com.cobaltplatform.api.model.db.EpicDepartment;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.service.AdvisoryLock;
import com.cobaltplatform.api.service.AppointmentService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.ProviderService;
import com.cobaltplatform.api.service.SystemService;
import com.cobaltplatform.api.util.db.DatabaseProvider;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class EpicSyncManager implements ProviderAvailabilitySyncManager, AutoCloseable {
	@Nonnull
	private static final Integer AVAILABILITY_SYNC_NUMBER_OF_DAYS_AHEAD;
	@Nonnull
	private static final Long AVAILABILITY_SYNC_INTERVAL_IN_SECONDS;
	@Nonnull
	private static final Long AVAILABILITY_SYNC_INITIAL_DELAY_IN_SECONDS;

	@Nonnull
	private final javax.inject.Provider<AvailabilitySyncTask> availabilitySyncTaskProvider;
	@Nonnull
	private final javax.inject.Provider<ProviderService> providerServiceProvider;
	@Nonnull
	private final javax.inject.Provider<AppointmentService> appointmentServiceProvider;
	@Nonnull
	private final javax.inject.Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final javax.inject.Provider<SystemService> systemServiceProvider;
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Object epicSyncLock;
	@Nonnull
	private final Logger logger;

	@Nonnull
	private Boolean started;
	@Nullable
	private ScheduledExecutorService availabilitySyncExecutorService;

	static {
		AVAILABILITY_SYNC_NUMBER_OF_DAYS_AHEAD = 50; // 7 weeks and 1 day
		AVAILABILITY_SYNC_INTERVAL_IN_SECONDS = 60L * 10;
		AVAILABILITY_SYNC_INITIAL_DELAY_IN_SECONDS = 10L;
	}

	@Inject
	public EpicSyncManager(@Nonnull javax.inject.Provider<AvailabilitySyncTask> availabilitySyncTaskProvider,
												 @Nonnull javax.inject.Provider<ProviderService> providerServiceProvider,
												 @Nonnull javax.inject.Provider<AppointmentService> appointmentServiceProvider,
												 @Nonnull javax.inject.Provider<InstitutionService> institutionServiceProvider,
												 @Nonnull javax.inject.Provider<SystemService> systemServiceProvider,
												 @Nonnull EnterprisePluginProvider enterprisePluginProvider,
												 @Nonnull DatabaseProvider databaseProvider,
												 @Nonnull Configuration configuration,
												 @Nonnull Strings strings) {
		requireNonNull(availabilitySyncTaskProvider);
		requireNonNull(providerServiceProvider);
		requireNonNull(appointmentServiceProvider);
		requireNonNull(systemServiceProvider);
		requireNonNull(enterprisePluginProvider);
		requireNonNull(databaseProvider);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.availabilitySyncTaskProvider = availabilitySyncTaskProvider;
		this.providerServiceProvider = providerServiceProvider;
		this.appointmentServiceProvider = appointmentServiceProvider;
		this.institutionServiceProvider = institutionServiceProvider;
		this.systemServiceProvider = systemServiceProvider;
		this.enterprisePluginProvider = enterprisePluginProvider;
		this.databaseProvider = databaseProvider;
		this.configuration = configuration;
		this.strings = strings;
		this.epicSyncLock = new Object();
		this.started = false;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public void close() throws Exception {
		stop();
	}

	@Nonnull
	public Boolean start() {
		synchronized (getEpicSyncLock()) {
			if (isStarted())
				return false;

			getLogger().trace("Starting EPIC sync...");

			this.availabilitySyncExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("epic-availability-sync-task").build());

			this.started = true;

			getAvailabilitySyncExecutorService().get().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						getSystemService().performAdvisoryLockOperationIfAvailable(AdvisoryLock.EPIC_PROVIDER_AVAILABILITY_SYNC, () -> {
							getAvailabilitySyncTaskProvider().get().run();
						});
					} catch (Exception e) {
						getLogger().warn(format("Unable to sync EPIC provider availability - will retry in %s seconds", String.valueOf(getAvailabilitySyncIntervalInSeconds())), e);
					}
				}
			}, getAvailabilitySyncInitialDelayInSeconds(), getAvailabilitySyncIntervalInSeconds(), TimeUnit.SECONDS);

			getLogger().trace("EPIC sync started.");

			return true;
		}
	}

	@Nonnull
	public Boolean stop() {
		synchronized (getEpicSyncLock()) {
			if (!isStarted())
				return false;

			getLogger().trace("Stopping EPIC sync...");

			getAvailabilitySyncExecutorService().get().shutdownNow();
			this.availabilitySyncExecutorService = null;

			this.started = false;

			getLogger().trace("EPIC sync stopped.");

			return true;
		}
	}

	/**
	 * Forces re-sync of a provider's availability for a particular day by pulling from EPIC and writing records to our DB.
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

		Institution institution = getInstitutionService().findInstitutionById(provider.getInstitutionId()).get();
		EpicClient epicClient = getEnterprisePluginProvider().enterprisePluginForInstitutionId(provider.getInstitutionId()).epicClientForBackendService().get();

		getLogger().info("Syncing availability for {} provider {} on {}...", institution.getInstitutionId().name(), provider.getName(), date);

		ProviderAvailabilityDateInsert insert = generateProviderAvailabilityDateInsert(epicClient, institution, provider, date);

		if (performInOwnTransaction)
			getDatabase().transaction(() -> {
				performProviderAvailabilityDateInsert(insert);
			});
		else
			performProviderAvailabilityDateInsert(insert);

		return true;
	}

	@Nonnull
	protected ProviderAvailabilityDateInsert generateProviderAvailabilityDateInsert(@Nonnull EpicClient epicClient,
																																									@Nonnull Institution institution,
																																									@Nonnull Provider provider,
																																									@Nonnull LocalDate date) {
		requireNonNull(epicClient);
		requireNonNull(institution);
		requireNonNull(provider);
		requireNonNull(date);

		List<AppointmentType> appointmentTypes = getAppointmentService().findAppointmentTypesByProviderId(provider.getProviderId()).stream()
				.filter(appointmentType -> appointmentType.getSchedulingSystemId().equals(SchedulingSystemId.EPIC) && provider.getActive())
				.collect(Collectors.toList());

		List<EpicDepartment> epicDepartments = getAppointmentService().findEpicDepartmentsByProviderId(provider.getProviderId());
		List<ProviderAvailabilityDateInsertRow> rows = new ArrayList<>();

		if (provider.getEpicAppointmentFilterId().equals(EpicAppointmentFilterId.VISIT_TYPE)) {
			// This path is for providers that want explicit filtering on EPIC visit types (as opposed to us looking at any open slot regardless of visit type)
			for (AppointmentType appointmentType : appointmentTypes) {
				for (EpicDepartment epicDepartment : epicDepartments) {
					GetProviderScheduleRequest request = new GetProviderScheduleRequest();
					request.setDate(date);
					request.setProviderID(provider.getEpicProviderId());
					request.setProviderIDType(provider.getEpicProviderIdType());
					request.setDepartmentID(epicDepartment.getDepartmentId());
					request.setDepartmentIDType(epicDepartment.getDepartmentIdType());
					request.setVisitTypeID(appointmentType.getEpicVisitTypeId());
					request.setVisitTypeIDType(appointmentType.getEpicVisitTypeIdType());
					request.setUserID(institution.getEpicUserId());
					request.setUserIDType(institution.getEpicUserIdType());

					GetProviderScheduleResponse response = epicClient.performGetProviderSchedule(request);

					for (GetProviderScheduleResponse.ScheduleSlot scheduleSlot : response.getScheduleSlots()) {
						LocalTime startTime = epicClient.parseTimeAmPm(scheduleSlot.getStartTime());
						Integer availableOpenings = Integer.valueOf(scheduleSlot.getAvailableOpenings());
						LocalDateTime dateTime = LocalDateTime.of(date, startTime);

						if (availableOpenings > 0) {
							boolean held = trimToEmpty(scheduleSlot.getHeldTimeReason()).length() > 0;
							boolean unavailable = trimToEmpty(scheduleSlot.getUnavailableTimeReason()).length() > 0;

							if (!held && !unavailable) {
								ProviderAvailabilityDateInsertRow row = new ProviderAvailabilityDateInsertRow();
								row.setAppointmentTypeId(appointmentType.getAppointmentTypeId());
								row.setDateTime(dateTime);
								row.setEpicDepartmentId(epicDepartment.getEpicDepartmentId());
								rows.add(row);
							}
						}
					}
				}
			}
		} else {
			// We look at appointment type durations to figure out NPVs/RPVs instead of filtering on visit type with EPIC
			Map<Long, List<AppointmentType>> appointmentTypesByDurationInMinutes = new HashMap<>(appointmentTypes.size());

			for (AppointmentType appointmentType : appointmentTypes) {
				List<AppointmentType> currentAppointmentTypes = appointmentTypesByDurationInMinutes.get(appointmentType.getDurationInMinutes());

				if (currentAppointmentTypes == null) {
					currentAppointmentTypes = new ArrayList<>();
					appointmentTypesByDurationInMinutes.put(appointmentType.getDurationInMinutes(), currentAppointmentTypes);
				}

				currentAppointmentTypes.add(appointmentType);
			}

			for (EpicDepartment epicDepartment : epicDepartments) {
				GetProviderScheduleRequest request = new GetProviderScheduleRequest();
				request.setDate(date);
				request.setProviderID(provider.getEpicProviderId());
				request.setProviderIDType(provider.getEpicProviderIdType());
				request.setDepartmentID(epicDepartment.getDepartmentId());
				request.setDepartmentIDType(epicDepartment.getDepartmentIdType());
				request.setUserID(institution.getEpicUserId());
				request.setUserIDType(institution.getEpicUserIdType());

				GetProviderScheduleResponse response = epicClient.performGetProviderSchedule(request);

				for (GetProviderScheduleResponse.ScheduleSlot scheduleSlot : response.getScheduleSlots()) {
					LocalTime startTime = epicClient.parseTimeAmPm(scheduleSlot.getStartTime());
					Integer availableOpenings = Integer.valueOf(scheduleSlot.getAvailableOpenings());
					Long length = Long.valueOf(scheduleSlot.getLength());
					LocalDateTime dateTime = LocalDateTime.of(date, startTime);
					List<AppointmentType> currentAppointmentTypes = appointmentTypesByDurationInMinutes.get(length);

					if (currentAppointmentTypes == null || currentAppointmentTypes.size() == 0) {
						getLogger().info("No appointment type found in Cobalt for the {}-minute appointment for {} in department {} on {}.", length, provider.getName(), epicDepartment.getDepartmentId(), dateTime);
					} else if (availableOpenings > 0) {
						boolean held = trimToEmpty(scheduleSlot.getHeldTimeReason()).length() > 0;
						boolean unavailable = trimToEmpty(scheduleSlot.getUnavailableTimeReason()).length() > 0;

						if (!held && !unavailable) {
							for (AppointmentType currentAppointmentType : currentAppointmentTypes) {
								ProviderAvailabilityDateInsertRow row = new ProviderAvailabilityDateInsertRow();
								row.setAppointmentTypeId(currentAppointmentType.getAppointmentTypeId());
								row.setDateTime(dateTime);
								row.setEpicDepartmentId(epicDepartment.getEpicDepartmentId());
								rows.add(row);
							}
						}
					}
				}
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

			List<Object> parameterGroup = new ArrayList<>(4);
			parameterGroup.add(insert.getProviderId());
			parameterGroup.add(row.getAppointmentTypeId());
			parameterGroup.add(row.getDateTime());
			parameterGroup.add(row.getEpicDepartmentId());
			parameterGroups.add(parameterGroup);
		}

		getDatabase().executeBatch("INSERT INTO provider_availability(provider_id, appointment_type_id, date_time, epic_department_id) VALUES (?,?,?,?)", parameterGroups);
	}

	protected void performDebugLogging(@Nonnull Provider provider,
																		 @Nonnull ProviderAvailabilityDateInsert insert) {
		requireNonNull(provider);
		requireNonNull(insert);

		// Example output:
		//
		// Apr-26-2020 2:22:33.190 PM EDT [epic-sync-task] DEBUG EpicSyncTask:243 [] Dr. Mark Allen availability for 2020-04-27:
		// Appointment Type ID XXX: [09:00, 09:30, 10:00, 10:30, 11:00, 11:30, 12:00, 12:30, 13:00, 13:30, 14:00, 14:30, 15:00, 15:30, 16:00]
		// Appointment Type ID XXX: [09:00, 09:30, 10:00, 10:30, 11:00, 11:30, 12:00, 12:30, 13:00, 13:30, 14:00, 14:30, 15:00, 15:30, 16:00, 16:30]

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

		providerDescriptionComponents.add(format("%s availability for %s (filter type %s):", provider.getName(), insert.getDate(), provider.getEpicAppointmentFilterId().name()));

		for (Entry<UUID, List<LocalDateTime>> entry : dateTimesByAppointmentTypeId.entrySet())
			providerDescriptionComponents.add(format("Appointment Type ID %s: %s", entry.getKey(), entry.getValue().stream()
					.map(localDateTime -> localDateTime.toLocalTime())
					.sorted()
					.collect(Collectors.toList())));

		if (providerDescriptionComponents.size() == 1)
			providerDescriptionComponents.add("[none]");

		getLogger().trace(providerDescriptionComponents.stream().collect(Collectors.joining("\n")));
	}

	@ThreadSafe
	protected static class AvailabilitySyncTask implements Runnable {
		@Nonnull
		private final javax.inject.Provider<EpicSyncManager> epicSyncManager;
		@Nonnull
		private final javax.inject.Provider<ProviderService> providerServiceProvider;
		@Nonnull
		private final javax.inject.Provider<InstitutionService> institutionServiceProvider;
		@Nonnull
		private final EnterprisePluginProvider enterprisePluginProvider;
		@Nonnull
		private final CurrentContextExecutor currentContextExecutor;
		@Nonnull
		private final DatabaseProvider databaseProvider;
		@Nonnull
		private final Configuration configuration;
		@Nonnull
		private final Logger logger;

		@Inject
		public AvailabilitySyncTask(@Nonnull javax.inject.Provider<EpicSyncManager> epicSyncManager,
																@Nonnull javax.inject.Provider<ProviderService> providerServiceProvider,
																@Nonnull javax.inject.Provider<InstitutionService> institutionServiceProvider,
																@Nonnull EnterprisePluginProvider enterprisePluginProvider,
																@Nonnull CurrentContextExecutor currentContextExecutor,
																@Nonnull DatabaseProvider databaseProvider,
																@Nonnull Configuration configuration) {
			requireNonNull(epicSyncManager);
			requireNonNull(providerServiceProvider);
			requireNonNull(institutionServiceProvider);
			requireNonNull(enterprisePluginProvider);
			requireNonNull(currentContextExecutor);
			requireNonNull(databaseProvider);
			requireNonNull(configuration);

			this.epicSyncManager = epicSyncManager;
			this.providerServiceProvider = providerServiceProvider;
			this.institutionServiceProvider = institutionServiceProvider;
			this.currentContextExecutor = currentContextExecutor;
			this.enterprisePluginProvider = enterprisePluginProvider;
			this.databaseProvider = databaseProvider;
			this.configuration = configuration;
			this.logger = LoggerFactory.getLogger(getClass());
		}

		@Override
		public void run() {
			List<Institution> institutions = getDatabase().queryForList("""
					     SELECT *
					     FROM institution
					     WHERE institution_id IN (SELECT institution_id FROM provider WHERE scheduling_system_id=? AND active=TRUE);
					""", Institution.class, SchedulingSystemId.EPIC);

			for (Institution institution : institutions) {
				CurrentContext currentContext = new CurrentContext.Builder(institution.getInstitutionId(),
						getConfiguration().getDefaultLocale(), getConfiguration().getDefaultTimeZone()).build();

				getCurrentContextExecutor().execute(currentContext, () -> {
					// Pick out all EPIC-scheduled providers
					List<Provider> providers = getProviderService().findProvidersByInstitutionId(institution.getInstitutionId()).stream()
							.filter(provider -> provider.getSchedulingSystemId().equals(SchedulingSystemId.EPIC))
							.collect(Collectors.toList());

					EpicClient epicClient = getEnterprisePluginProvider().enterprisePluginForInstitutionId(institution.getInstitutionId()).epicClientForBackendService().get();

					getLogger().info("Running EPIC availability sync for {} providers in {}...",
							providers.size(), institution.getInstitutionId().name());

					int providerSuccessCount = 0;

					for (Provider provider : providers) {
						try {
							LocalDate syncDate = LocalDate.now(provider.getTimeZone());

							List<ProviderAvailabilityDateInsert> inserts = new ArrayList<>(getEpicSyncManager().getAvailabilitySyncNumberOfDaysAhead());

							for (int i = 0; i < getEpicSyncManager().getAvailabilitySyncNumberOfDaysAhead(); ++i) {
								ProviderAvailabilityDateInsert insert = getEpicSyncManager().generateProviderAvailabilityDateInsert(epicClient, institution, provider, syncDate);
								inserts.add(insert);
								syncDate = syncDate.plusDays(1);
							}

							// After we've done all the EPIC calls to pull data for this provider, commit to DB.
							// This way we keep transaction time to a minimum to reduce contention

							// For each provider-date, clear out existing availabilities and insert the new ones (if any)
							for (ProviderAvailabilityDateInsert insert : inserts) {
								// Dump out info for debugging...
								if (getLogger().isDebugEnabled())
									getEpicSyncManager().performDebugLogging(provider, insert);

								getDatabase().transaction(() -> {
									getEpicSyncManager().performProviderAvailabilityDateInsert(insert);
								});
							}

							++providerSuccessCount;
						} catch (Exception e) {
							getLogger().warn(format("Unable to sync provider ID %s (%s) with EPIC", provider.getProviderId(), provider.getName()), e);
						}
					}

					getLogger().info("EPIC provider availability sync complete for {}. Successfully synced {} of {} providers.",
							institution.getInstitutionId().name(), providerSuccessCount, providers.size());
				});
			}
		}

		@Nonnull
		protected EpicSyncManager getEpicSyncManager() {
			return epicSyncManager.get();
		}

		@Nonnull
		protected ProviderService getProviderService() {
			return providerServiceProvider.get();
		}

		@Nonnull
		protected InstitutionService getInstitutionService() {
			return this.institutionServiceProvider.get();
		}

		@Nonnull
		protected CurrentContextExecutor getCurrentContextExecutor() {
			return currentContextExecutor;
		}

		@Nonnull
		protected EnterprisePluginProvider getEnterprisePluginProvider() {
			return enterprisePluginProvider;
		}

		@Nonnull
		protected Database getDatabase() {
			return this.databaseProvider.get();
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
		private UUID epicDepartmentId;

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

		@Nullable
		public UUID getEpicDepartmentId() {
			return epicDepartmentId;
		}

		public void setEpicDepartmentId(@Nullable UUID epicDepartmentId) {
			this.epicDepartmentId = epicDepartmentId;
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
		synchronized (getEpicSyncLock()) {
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
	protected Long getAvailabilitySyncInitialDelayInSeconds() {
		return AVAILABILITY_SYNC_INITIAL_DELAY_IN_SECONDS;
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
	protected ProviderService getProviderService() {
		return providerServiceProvider.get();
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return appointmentServiceProvider.get();
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
	}

	@Nonnull
	protected SystemService getSystemService() {
		return this.systemServiceProvider.get();
	}

	@Nonnull
	protected EnterprisePluginProvider getEnterprisePluginProvider() {
		return this.enterprisePluginProvider;
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
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
	protected Object getEpicSyncLock() {
		return epicSyncLock;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}