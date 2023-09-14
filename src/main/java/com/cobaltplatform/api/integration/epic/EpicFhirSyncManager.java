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
import com.cobaltplatform.api.integration.epic.request.AppointmentFindFhirStu3Request;
import com.cobaltplatform.api.integration.epic.response.AppointmentFindFhirStu3Response;
import com.cobaltplatform.api.model.db.EpicFhirAppointmentFindCache;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.service.AdvisoryLock;
import com.cobaltplatform.api.service.AppointmentService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.ProviderService;
import com.cobaltplatform.api.service.SystemService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class EpicFhirSyncManager implements ProviderAvailabilitySyncManager, AutoCloseable {
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
	private final Database database;
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
		AVAILABILITY_SYNC_NUMBER_OF_DAYS_AHEAD = 60;
		AVAILABILITY_SYNC_INTERVAL_IN_SECONDS = 60L;
		AVAILABILITY_SYNC_INITIAL_DELAY_IN_SECONDS = 10L;
	}

	@Inject
	public EpicFhirSyncManager(@Nonnull javax.inject.Provider<AvailabilitySyncTask> availabilitySyncTaskProvider,
														 @Nonnull javax.inject.Provider<ProviderService> providerServiceProvider,
														 @Nonnull javax.inject.Provider<AppointmentService> appointmentServiceProvider,
														 @Nonnull javax.inject.Provider<InstitutionService> institutionServiceProvider,
														 @Nonnull javax.inject.Provider<SystemService> systemServiceProvider,
														 @Nonnull EnterprisePluginProvider enterprisePluginProvider,
														 @Nonnull Database database,
														 @Nonnull Configuration configuration,
														 @Nonnull Strings strings) {
		requireNonNull(availabilitySyncTaskProvider);
		requireNonNull(providerServiceProvider);
		requireNonNull(appointmentServiceProvider);
		requireNonNull(systemServiceProvider);
		requireNonNull(enterprisePluginProvider);
		requireNonNull(database);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.availabilitySyncTaskProvider = availabilitySyncTaskProvider;
		this.providerServiceProvider = providerServiceProvider;
		this.appointmentServiceProvider = appointmentServiceProvider;
		this.institutionServiceProvider = institutionServiceProvider;
		this.systemServiceProvider = systemServiceProvider;
		this.enterprisePluginProvider = enterprisePluginProvider;
		this.database = database;
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

			getLogger().trace("Starting EPIC FHIR sync...");

			this.availabilitySyncExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("epic-fhir-availability-sync-task").build());

			this.started = true;

			getAvailabilitySyncExecutorService().get().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						getSystemService().performAdvisoryLockOperationIfAvailable(AdvisoryLock.EPIC_FHIR_PROVIDER_AVAILABILITY_SYNC, () -> {
							getAvailabilitySyncTaskProvider().get().run();
						});
					} catch (Exception e) {
						getLogger().warn(format("Unable to sync EPIC FHIR provider availability - will retry in %s seconds", String.valueOf(getAvailabilitySyncIntervalInSeconds())), e);
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

			getLogger().trace("Stopping EPIC FHIR sync...");

			getAvailabilitySyncExecutorService().get().shutdownNow();
			this.availabilitySyncExecutorService = null;

			this.started = false;

			getLogger().trace("EPIC FHIR sync stopped.");

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
		AvailabilitySyncTask availabilitySyncTask = getAvailabilitySyncTaskProvider().get();

		getLogger().info("Syncing availability for {} on {}...", institution.getInstitutionId().name(), date);

		if (performInOwnTransaction) {
			getDatabase().transaction(() -> {
				availabilitySyncTask.syncDate(institution, epicClient, date);
			});
		} else {
			availabilitySyncTask.syncDate(institution, epicClient, date);
		}

		return true;
	}

	@ThreadSafe
	protected static class AvailabilitySyncTask implements Runnable {
		@Nonnull
		private final javax.inject.Provider<EpicFhirSyncManager> epicSyncManager;
		@Nonnull
		private final javax.inject.Provider<ProviderService> providerServiceProvider;
		@Nonnull
		private final javax.inject.Provider<InstitutionService> institutionServiceProvider;
		@Nonnull
		private final EnterprisePluginProvider enterprisePluginProvider;
		@Nonnull
		private final CurrentContextExecutor currentContextExecutor;
		@Nonnull
		private final Database database;
		@Nonnull
		private final Configuration configuration;
		@Nonnull
		private final Logger logger;

		@Inject
		public AvailabilitySyncTask(@Nonnull javax.inject.Provider<EpicFhirSyncManager> epicSyncManager,
																@Nonnull javax.inject.Provider<ProviderService> providerServiceProvider,
																@Nonnull javax.inject.Provider<InstitutionService> institutionServiceProvider,
																@Nonnull EnterprisePluginProvider enterprisePluginProvider,
																@Nonnull CurrentContextExecutor currentContextExecutor,
																@Nonnull Database database,
																@Nonnull Configuration configuration) {
			requireNonNull(epicSyncManager);
			requireNonNull(providerServiceProvider);
			requireNonNull(institutionServiceProvider);
			requireNonNull(enterprisePluginProvider);
			requireNonNull(currentContextExecutor);
			requireNonNull(database);
			requireNonNull(configuration);

			this.epicSyncManager = epicSyncManager;
			this.providerServiceProvider = providerServiceProvider;
			this.institutionServiceProvider = institutionServiceProvider;
			this.currentContextExecutor = currentContextExecutor;
			this.enterprisePluginProvider = enterprisePluginProvider;
			this.database = database;
			this.configuration = configuration;
			this.logger = LoggerFactory.getLogger(getClass());
		}

		@Override
		public void run() {
			List<Institution> institutions = getDatabase().queryForList("""
					     SELECT *
					     FROM institution
					     WHERE epic_fhir_enabled=TRUE
					     ORDER BY institution_id
					""", Institution.class);

			for (Institution institution : institutions) {
				CurrentContext currentContext = new CurrentContext.Builder(institution.getInstitutionId(),
						getConfiguration().getDefaultLocale(), getConfiguration().getDefaultTimeZone()).build();

				getCurrentContextExecutor().execute(currentContext, () -> {
					EpicClient epicClient = getEnterprisePluginProvider().enterprisePluginForInstitutionId(institution.getInstitutionId()).epicClientForBackendService().get();

					getLogger().info("Running EPIC FHIR provider availability sync for {}...", institution.getInstitutionId().name());

					// Ask Epic for potential slots by making 1 call per date in parallel.
					// Store off results in the database for quick access elsewhere

					// ExecutorService is only Autocloseable in Java 19+
					ExecutorService epicFhirExecutorService = null;

					try {
						epicFhirExecutorService = Executors.newFixedThreadPool(10);

						// First, make a list of dates to call.
						LocalDate startDate = LocalDate.now(institution.getTimeZone());
						LocalDate currentDate = startDate;
						LocalDate endDate = startDate.plusDays(AVAILABILITY_SYNC_NUMBER_OF_DAYS_AHEAD);
						List<LocalDate> dates = new ArrayList<>();

						if (!endDate.isBefore(currentDate)) {
							while (endDate.isAfter(currentDate)) {
								dates.add(currentDate);
								currentDate = currentDate.plusDays(1);
							}
						}

						// Fan out and make a call for each date and wait for
						List<CompletableFuture<Void>> completableFutures = new ArrayList<>(dates.size());

						getLogger().debug("Pulling Epic FHIR data from {} to {}...", startDate, endDate);

						for (LocalDate date : dates) {
							completableFutures.add(CompletableFuture.supplyAsync(() -> {
								// Check the local cache to see if there's a fresh value we can use
								EpicFhirAppointmentFindCache epicFhirAppointmentFindCache = getDatabase().queryForObject("""
										SELECT *
										FROM epic_fhir_appointment_find_cache
										WHERE institution_id=?
										AND date=?
										""", EpicFhirAppointmentFindCache.class, institution.getInstitutionId(), date).orElse(null);

								if (epicFhirAppointmentFindCache != null) {
									boolean cacheEntryExpired = epicFhirAppointmentFindCache.getLastUpdated()
											.isBefore(Instant.now().minusSeconds(institution.getEpicFhirAppointmentFindCacheExpirationInSeconds()));

									if (cacheEntryExpired) {
										getLogger().debug("Cache entry is stale for {} on {}, asking Epic for data...", institution.getInstitutionId().name(), date);
										syncDate(institution, epicClient, date);
									} else {
										getLogger().debug("Cache is still fresh for {} on {}, nothing to do.", institution.getInstitutionId().name(), date);
									}
								} else {
									getLogger().debug("Cache miss for {} on {}, asking Epic for data...", institution.getInstitutionId().name(), date);
									syncDate(institution, epicClient, date);
								}

								// Don't care about this value
								return null;
							}, epicFhirExecutorService));
						}

						CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]));

						getLogger().debug("Waiting for all futures to complete...");

						try {
							combinedFuture.get(180, TimeUnit.SECONDS);
						} catch (ExecutionException e) {
							throw new RuntimeException("Epic FHIR provider find cache job failed", e);
						} catch (TimeoutException e) {
							throw new RuntimeException("Epic FHIR provider find cache job timed out", e);
						} catch (InterruptedException e) {
							throw new RuntimeException("Epic FHIR provider find cache job was interrupted", e);
						}

						getLogger().debug("All futures completed.");
					} finally {
						epicFhirExecutorService.shutdownNow();
					}

					getLogger().info("EPIC FHIR provider availability sync complete for {}.", institution.getInstitutionId().name());
				});
			}
		}

		public void syncDate(@Nonnull Institution institution,
												 @Nonnull EpicClient epicClient,
												 @Nonnull LocalDate date) {
			requireNonNull(institution);
			requireNonNull(epicClient);
			requireNonNull(date);

			LocalDateTime startDateTime = LocalDateTime.of(date, LocalTime.MIN);
			LocalDateTime endDateTime = LocalDateTime.of(date, LocalTime.MAX);

			// Don't filter at all except for start/end datetime.  We want all the provider/visit type/etc. data back from Epic.
			// We can filter later on-demand in-memory for every request
			AppointmentFindFhirStu3Request appointmentFindRequest = new AppointmentFindFhirStu3Request();
			appointmentFindRequest.setStartTime(startDateTime.atZone(institution.getTimeZone()).toInstant());
			appointmentFindRequest.setEndTime(endDateTime.atZone(institution.getTimeZone()).toInstant());

			AppointmentFindFhirStu3Response response = epicClient.appointmentFindFhirStu3(appointmentFindRequest);

			// Upsert the response into our local cache
			String apiResponse = response.serialize();
			Instant lastUpdated = Instant.now();

			getDatabase().execute("""
					INSERT INTO epic_fhir_appointment_find_cache (
					  institution_id,
					  date,
					  api_response,
					  last_updated
					) VALUES (?,?,CAST(? AS JSONB),?)
					ON CONFLICT (institution_id, date) DO UPDATE SET
					  api_response = EXCLUDED.api_response,
					  last_updated = EXCLUDED.last_updated
					""", institution.getInstitutionId(), date, apiResponse, lastUpdated);
		}

		@Nonnull
		protected EpicFhirSyncManager getEpicSyncManager() {
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
	protected Object getEpicSyncLock() {
		return epicSyncLock;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}