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

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.integration.acuity.AcuitySyncManager;
import com.cobaltplatform.api.integration.common.ProviderAvailabilitySyncManager;
import com.cobaltplatform.api.integration.epic.EpicFhirSyncManager;
import com.cobaltplatform.api.integration.epic.EpicSyncManager;
import com.cobaltplatform.api.model.db.BetaFeature;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.service.AdvisoryLock;
import com.lokalized.Strings;
import com.pyranid.Database;
import com.soklet.web.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class SystemService {
	@Nonnull
	private final Database database;
	@Nonnull
	private final ProviderService providerService;
	@Nonnull
	private final EpicSyncManager epicSyncManager;
	@Nonnull
	private final EpicFhirSyncManager epicFhirSyncManager;
	@Nonnull
	private final AcuitySyncManager acuitySyncManager;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public SystemService(@Nonnull Database database,
											 @Nonnull ProviderService providerService,
											 @Nonnull EpicSyncManager epicSyncManager,
											 @Nonnull EpicFhirSyncManager epicFhirSyncManager,
											 @Nonnull AcuitySyncManager acuitySyncManager,
											 @Nonnull Configuration configuration,
											 @Nonnull Strings strings) {
		requireNonNull(database);
		requireNonNull(providerService);
		requireNonNull(epicSyncManager);
		requireNonNull(epicFhirSyncManager);
		requireNonNull(acuitySyncManager);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.database = database;
		this.providerService = providerService;
		this.epicSyncManager = epicSyncManager;
		this.epicFhirSyncManager = epicFhirSyncManager;
		this.acuitySyncManager = acuitySyncManager;
		this.configuration = configuration;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public List<BetaFeature> findBetaFeatures() {
		return getDatabase().queryForList("SELECT * FROM beta_feature ORDER BY description", BetaFeature.class);
	}

	@Nonnull
	public Boolean performAdvisoryLockOperationIfAvailable(@Nonnull AdvisoryLock advisoryLock,
																												 @Nonnull Runnable runnable) {
		requireNonNull(advisoryLock);
		requireNonNull(runnable);

		getLogger().trace("Attempting to acquire advisory lock {} (key {})",
				advisoryLock.name(), advisoryLock.getKey());

		Boolean lockAcquired = getDatabase().queryForObject("SELECT pg_try_advisory_lock(?)",
				Boolean.class, advisoryLock.getKey()).get();

		if (!lockAcquired) {
			getLogger().trace("Advisory lock {} (key {}) has already been acquired, not performing operation.",
					advisoryLock.name(), advisoryLock.getKey());
			return false;
		}

		try {
			runnable.run();
		} finally {
			getLogger().trace("Releasing advisory lock {} (key {})...", advisoryLock.name(), advisoryLock.getKey());
			getDatabase().queryForObject("SELECT pg_advisory_unlock(?)", Boolean.class, advisoryLock.getKey());
			getLogger().trace("Advisory lock {} (key {}) has been released.", advisoryLock.name(), advisoryLock.getKey());
		}

		return true;
	}

	public void syncPastProviderAvailability(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);
		syncPastProviderAvailability(institutionId, null);
	}

	public void syncPastProviderAvailability(@Nonnull InstitutionId institutionId,
																					 @Nullable LocalDate startingAtDate) {
		requireNonNull(institutionId);

		List<Provider> providers = providerService.findProvidersByInstitutionId(institutionId).stream()
				.filter(provider -> provider.getSchedulingSystemId() == SchedulingSystemId.EPIC || provider.getSchedulingSystemId() == SchedulingSystemId.ACUITY)
				.sorted(Comparator.comparing(Provider::getName))
				.collect(Collectors.toList());

		int i = 1;

		getLogger().info("*** STARTING PAST PROVIDER AVAILABILITY SYNC FOR {} ***", institutionId.name());

		for (Provider provider : providers) {
			List<ProviderSyncRecord> providerSyncRecords = new ArrayList<>(providers.size() * 365 * 2);

			// Sync date starts on the date at which the provider record was created and goes until yesterday (inclusive).
			// If a starting-at date was passed in, use that instead
			LocalDate syncDate = startingAtDate != null ? startingAtDate : LocalDate.ofInstant(provider.getCreated(), provider.getTimeZone());
			LocalDate today = LocalDate.now(provider.getTimeZone());

			ProviderAvailabilitySyncManager providerAvailabilitySyncManager = null;

			if (provider.getSchedulingSystemId() == SchedulingSystemId.EPIC)
				providerAvailabilitySyncManager = getEpicSyncManager();
			else if (provider.getSchedulingSystemId() == SchedulingSystemId.EPIC_FHIR)
				providerAvailabilitySyncManager = getEpicFhirSyncManager();
			else if (provider.getSchedulingSystemId() == SchedulingSystemId.ACUITY)
				providerAvailabilitySyncManager = getAcuitySyncManager();

			while (syncDate.isBefore(today)) {
				getLogger().info("Syncing provider {} ({}/{}) with {} on {}...", provider.getName(), i, providers.size(), provider.getSchedulingSystemId().name(), syncDate);

				try {
					providerAvailabilitySyncManager.syncProviderAvailability(provider.getProviderId(), syncDate, true);
					providerSyncRecords.add(new ProviderSyncRecord(provider, syncDate));
				} catch (Exception e) {
					getLogger().warn(format("Error performing old availabilty sync for %s on %s using %s...", provider.getName(), syncDate, provider.getSchedulingSystemId().name()), e);
					providerSyncRecords.add(new ProviderSyncRecord(provider, syncDate, e));
				}

				syncDate = syncDate.plusDays(1);
			}

			for (ProviderSyncRecord providerSyncRecord : providerSyncRecords) {
				getDatabase().transaction(() -> {
					getDatabase().execute(
							"INSERT INTO provider_old_availability_sync_log(provider_id, date, success, sync_timestamp) VALUES (?,?,?,?)",
							providerSyncRecord.getProvider().getProviderId(), providerSyncRecord.getDate(), providerSyncRecord.getSuccess(), providerSyncRecord.getCreated());
				});
			}

			++i;
		}

		getLogger().info("*** ENDING PAST PROVIDER AVAILABILITY SYNC FOR {} ***", institutionId.name());
	}

	public void syncProviderAvailability(@Nonnull UUID providerId,
																			 @Nullable LocalDate startingAtDate,
																			 @Nullable LocalDate endingAtDate) {
		requireNonNull(providerId);

		Provider provider = providerService.findProviderById(providerId).orElse(null);

		if (provider == null)
			throw new NotFoundException();

		if (startingAtDate == null)
			startingAtDate = LocalDate.ofInstant(provider.getCreated(), provider.getTimeZone());

		if (endingAtDate == null)
			endingAtDate = LocalDate.ofInstant(Instant.now(), provider.getTimeZone());

		if (startingAtDate.isAfter(endingAtDate))
			throw new IllegalStateException(format("Starting date %s is after ending date %s", startingAtDate, endingAtDate));

		getLogger().info("Syncing provider availability for {} for {} - {}", provider.getName(), startingAtDate, endingAtDate);

		LocalDate syncDate = startingAtDate;

		ProviderAvailabilitySyncManager providerAvailabilitySyncManager = null;

		if (provider.getSchedulingSystemId() == SchedulingSystemId.EPIC)
			providerAvailabilitySyncManager = getEpicSyncManager();
		else if (provider.getSchedulingSystemId() == SchedulingSystemId.EPIC_FHIR)
			providerAvailabilitySyncManager = getEpicFhirSyncManager();
		else if (provider.getSchedulingSystemId() == SchedulingSystemId.ACUITY)
			providerAvailabilitySyncManager = getAcuitySyncManager();

		if (providerAvailabilitySyncManager == null) {
			getLogger().info("Provider {} uses {} for calendaring, no need to sync.", provider.getName(), provider.getSchedulingSystemId().name());
			return;
		}

		boolean success = false;

		while (!syncDate.isAfter(endingAtDate)) {
			getLogger().info("Syncing provider {} on {}...", provider.getName(), syncDate);

			try {
				providerAvailabilitySyncManager.syncProviderAvailability(providerId, syncDate, true);
				success = true;
			} catch (Exception e) {
				getLogger().warn(format("Unable to sync provider %s on %s", provider.getName(), syncDate), e);
			} finally {
				LocalDate pinnedSyncDate = syncDate;
				boolean pinnedSuccess = success;

				getDatabase().transaction(() -> {
					getDatabase().execute(
							"INSERT INTO provider_old_availability_sync_log(provider_id, date, success, sync_timestamp) VALUES (?,?,?,?)",
							providerId, pinnedSyncDate, pinnedSuccess, Instant.now());
				});
			}

			getLogger().info("Syncing provider {} on {} finished.", provider.getName(), syncDate);

			syncDate = syncDate.plusDays(1);
		}

		getLogger().info("Finished syncing provider availability for {} for {} - {}", provider.getName(), startingAtDate, endingAtDate);
	}

	@ThreadSafe
	public static class ProviderSyncRecord {
		@Nonnull
		private Provider provider;
		@Nonnull
		private LocalDate date;
		@Nonnull
		private Boolean success;
		@Nullable
		private Exception exception;
		@Nullable
		private Instant created;

		public ProviderSyncRecord(@Nonnull Provider provider,
															@Nonnull LocalDate date) {
			this(provider, date, null);
		}

		public ProviderSyncRecord(@Nonnull Provider provider,
															@Nonnull LocalDate date,
															@Nullable Exception exception) {
			requireNonNull(provider);
			requireNonNull(date);

			this.provider = provider;
			this.date = date;
			this.success = exception == null;
			this.exception = exception;
			this.created = Instant.now();
		}

		@Override
		public String toString() {
			return getSuccess() ? format("%s %s: SUCCESS", getProvider().getName(), getDate()) :
					format("%s %s: ERROR! Reason: %s", getProvider().getName(), getDate(), getException().get().getMessage());
		}

		@Nonnull
		public Provider getProvider() {
			return this.provider;
		}

		@Nonnull
		public LocalDate getDate() {
			return this.date;
		}

		@Nonnull
		public Boolean getSuccess() {
			return this.success;
		}

		@Nonnull
		public Optional<Exception> getException() {
			return Optional.ofNullable(this.exception);
		}

		@Nullable
		public Instant getCreated() {
			return this.created;
		}
	}

	@Nonnull
	protected Database getDatabase() {
		return this.database;
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return this.providerService;
	}

	@Nonnull
	protected EpicSyncManager getEpicSyncManager() {
		return this.epicSyncManager;
	}

	@Nonnull
	protected EpicFhirSyncManager getEpicFhirSyncManager() {
		return this.epicFhirSyncManager;
	}

	@Nonnull
	protected AcuitySyncManager getAcuitySyncManager() {
		return this.acuitySyncManager;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
