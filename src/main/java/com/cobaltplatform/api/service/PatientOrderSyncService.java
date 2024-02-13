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
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.error.ErrorReporter;
import com.cobaltplatform.api.integration.hl7.Hl7Client;
import com.cobaltplatform.api.integration.hl7.Hl7ParsingException;
import com.cobaltplatform.api.integration.hl7.model.event.Hl7GeneralOrderTriggerEvent;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderImportRequest;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PatientOrderImportType.PatientOrderImportTypeId;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class PatientOrderSyncService implements AutoCloseable {
	@Nonnull
	private static final Long BACKGROUND_TASK_INTERVAL_IN_SECONDS;
	@Nonnull
	private static final Long BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS;

	@Nonnull
	private final Provider<BackgroundSyncTask> backgroundSyncTaskProvider;
	@Nonnull
	private final Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final Provider<PatientOrderService> patientOrderServiceProvider;
	@Nonnull
	private final S3Client s3Client;
	@Nonnull
	private final Hl7Client hl7Client;
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final ErrorReporter errorReporter;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final Object backgroundTaskLock;
	@Nonnull
	private Boolean backgroundTaskStarted;
	@Nullable
	private ScheduledExecutorService backgroundTaskExecutorService;

	static {
		BACKGROUND_TASK_INTERVAL_IN_SECONDS = 60L;
		BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS = 1L;
	}

	@Inject
	public PatientOrderSyncService(@Nonnull Provider<BackgroundSyncTask> backgroundSyncTaskProvider,
																 @Nonnull Provider<InstitutionService> institutionServiceProvider,
																 @Nonnull Provider<PatientOrderService> patientOrderServiceProvider,
																 @Nonnull Hl7Client hl7Client,
																 @Nonnull DatabaseProvider databaseProvider,
																 @Nonnull ErrorReporter errorReporter,
																 @Nonnull Configuration configuration,
																 @Nonnull Strings strings) {
		requireNonNull(backgroundSyncTaskProvider);
		requireNonNull(institutionServiceProvider);
		requireNonNull(patientOrderServiceProvider);
		requireNonNull(hl7Client);
		requireNonNull(databaseProvider);
		requireNonNull(errorReporter);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.backgroundSyncTaskProvider = backgroundSyncTaskProvider;
		this.institutionServiceProvider = institutionServiceProvider;
		this.patientOrderServiceProvider = patientOrderServiceProvider;
		this.databaseProvider = databaseProvider;
		this.errorReporter = errorReporter;
		this.configuration = configuration;
		this.hl7Client = hl7Client;
		this.s3Client = createS3Client(configuration);
		this.strings = strings;
		this.backgroundTaskLock = new Object();
		this.backgroundTaskStarted = false;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public void close() throws Exception {
		stopBackgroundTask();
	}

	@Nonnull
	public Boolean startBackgroundTask() {
		synchronized (getBackgroundTaskLock()) {
			if (isBackgroundTaskStarted())
				return false;

			getLogger().trace("Starting patient order sync background task...");

			this.backgroundTaskExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("patient-order-sync-background-task").build());
			this.backgroundTaskStarted = true;

			getBackgroundTaskExecutorService().get().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						getBackgroundSyncTaskProvider().get().run();
					} catch (Exception e) {
						getLogger().warn(format("Unable to complete patient order sync background task - will retry in %s seconds", String.valueOf(getBackgroundTaskIntervalInSeconds())), e);
					}
				}
			}, getBackgroundTaskInitialDelayInSeconds(), getBackgroundTaskIntervalInSeconds(), TimeUnit.SECONDS);

			getLogger().trace("Patient order sync background task started.");

			return true;
		}
	}

	@Nonnull
	public Boolean stopBackgroundTask() {
		synchronized (getBackgroundTaskLock()) {
			if (!isBackgroundTaskStarted())
				return false;

			getLogger().trace("Stopping patient order sync background task...");

			getBackgroundTaskExecutorService().get().shutdownNow();
			this.backgroundTaskExecutorService = null;
			this.backgroundTaskStarted = false;

			getLogger().trace("Patient order sync background task stopped.");

			return true;
		}
	}

	@Nonnull
	public Set<UUID> syncPatientOrdersForInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Set.of();

		Institution institution = getInstitutionService().findInstitutionById(institutionId).orElse(null);
		return institution == null ? Set.of() : syncPatientOrdersForInstitution(institution);
	}

	@Nonnull
	public Set<UUID> syncPatientOrdersForInstitution(@Nullable Institution institution) {
		if (institution == null || !institution.getIntegratedCareEnabled() || institution.getIntegratedCareOrderImportBucketName() == null)
			return Set.of();

		// TODO: acquire lock

		String bucket = institution.getIntegratedCareOrderImportBucketName();

		Set<UUID> patientOrderIds = new HashSet<>();

		ListObjectsV2Request request = ListObjectsV2Request.builder()
				.bucket(bucket)
				.prefix("To_COBALT_Ord_") // e.g. To_COBALT_Ord_20240201_08.txt
				.build();

		ListObjectsV2Iterable response = getS3Client().listObjectsV2Paginator(request);

		for (ListObjectsV2Response page : response) {
			page.contents().forEach((S3Object s3Object) -> {
				getLogger().info("Downloading {} HL7 Order message from {}/{}...",
						institution.getInstitutionId().name(), bucket, s3Object.key());

				GetObjectRequest getObjectRequest = GetObjectRequest.builder()
						.bucket(bucket)
						.key(s3Object.key())
						.build();

				try (InputStream inputStream = getS3Client().getObject(getObjectRequest)) {
					byte[] generalOrderHl7 = IOUtils.toByteArray(inputStream);
					getLogger().info("{} HL7 Order message download completed for {}/{} ({} bytes).",
							institution.getInstitutionId().name(), bucket, s3Object.key(), generalOrderHl7.length);

					String generalOrderHl7AsString = getHl7Client().messageFromBytes(generalOrderHl7);

					getLogger().info("HL7 Order content for {}/{}:\n{}", bucket, s3Object.key(), generalOrderHl7AsString);

					Hl7GeneralOrderTriggerEvent generalOrder = null;

					try {
						generalOrder = getHl7Client().parseGeneralOrder(generalOrderHl7AsString);
					} catch (Hl7ParsingException e) {
						getErrorReporter().report(e);
					}

					if (generalOrder == null) {
						// TODO: handle error case
					} else {

					}

					// TODO: need an account ID for this
					UUID accountId = null;

					CreatePatientOrderImportRequest importRequest = new CreatePatientOrderImportRequest();
					importRequest.setPatientOrderImportTypeId(PatientOrderImportTypeId.HL7_MESSAGE);
					importRequest.setInstitutionId(institution.getInstitutionId());
					importRequest.setAccountId(accountId);
					getPatientOrderService().createPatientOrderImport(importRequest);

				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		}

		return patientOrderIds;
	}

	@Nonnull
	protected S3Client createS3Client(@Nonnull Configuration configuration) {
		requireNonNull(configuration);

		S3ClientBuilder builder = S3Client.builder().region(configuration.getAmazonS3Region());

		if (configuration.getAmazonUseLocalstack())
			builder.endpointOverride(URI.create(configuration.getAmazonS3BaseUrl()));

		return builder.build();
	}

	@ThreadSafe
	protected static class BackgroundSyncTask implements Runnable {
		@Nonnull
		private final Provider<PatientOrderSyncService> patientOrderSyncServiceProvider;
		@Nonnull
		private final Provider<InstitutionService> institutionServiceProvider;
		@Nonnull
		private final CurrentContextExecutor currentContextExecutor;
		@Nonnull
		private final ErrorReporter errorReporter;
		@Nonnull
		private final DatabaseProvider databaseProvider;
		@Nonnull
		private final Configuration configuration;
		@Nonnull
		private final Logger logger;

		@Inject
		public BackgroundSyncTask(@Nonnull Provider<PatientOrderSyncService> patientOrderSyncServiceProvider,
															@Nonnull Provider<InstitutionService> institutionServiceProvider,
															@Nonnull CurrentContextExecutor currentContextExecutor,
															@Nonnull ErrorReporter errorReporter,
															@Nonnull DatabaseProvider databaseProvider,
															@Nonnull Configuration configuration) {
			requireNonNull(patientOrderSyncServiceProvider);
			requireNonNull(institutionServiceProvider);
			requireNonNull(currentContextExecutor);
			requireNonNull(errorReporter);
			requireNonNull(databaseProvider);
			requireNonNull(configuration);

			this.patientOrderSyncServiceProvider = patientOrderSyncServiceProvider;
			this.institutionServiceProvider = institutionServiceProvider;
			this.currentContextExecutor = currentContextExecutor;
			this.errorReporter = errorReporter;
			this.databaseProvider = databaseProvider;
			this.configuration = configuration;
			this.logger = LoggerFactory.getLogger(getClass());
		}

		@Override
		public void run() {
			CurrentContext currentContext = new CurrentContext.Builder(InstitutionId.COBALT,
					getConfiguration().getDefaultLocale(), getConfiguration().getDefaultTimeZone()).build();

			getCurrentContextExecutor().execute(currentContext, () -> {
				try {
					List<Institution> orderImportEnabledInstitutions = getInstitutionService().findInstitutions().stream()
							.filter(institution -> institution.getIntegratedCareEnabled() && institution.getIntegratedCareOrderImportBucketName() != null)
							.collect(Collectors.toList());

					for (Institution institution : orderImportEnabledInstitutions) {
						try {
							getPatientOrderSyncService().syncPatientOrdersForInstitution(institution);
						} catch (Exception e) {
							getLogger().error(format("Unable to sync incoming patient orders for institution ID %s",
									institution.getInstitutionId().name()), e);
							getErrorReporter().report(e);
						}
					}
				} catch (Exception e) {
					getLogger().error("Unable to sync incoming patient orders", e);
					getErrorReporter().report(e);
				}
			});
		}

		@Nonnull
		protected PatientOrderSyncService getPatientOrderSyncService() {
			return this.patientOrderSyncServiceProvider.get();
		}

		@Nonnull
		protected InstitutionService getInstitutionService() {
			return this.institutionServiceProvider.get();
		}

		@Nonnull
		protected CurrentContextExecutor getCurrentContextExecutor() {
			return this.currentContextExecutor;
		}

		@Nonnull
		protected ErrorReporter getErrorReporter() {
			return this.errorReporter;
		}

		@Nonnull
		protected Database getDatabase() {
			return this.databaseProvider.get();
		}

		@Nonnull
		protected Configuration getConfiguration() {
			return this.configuration;
		}

		@Nonnull
		protected Logger getLogger() {
			return this.logger;
		}
	}

	@Nonnull
	public Boolean isBackgroundTaskStarted() {
		synchronized (getBackgroundTaskLock()) {
			return this.backgroundTaskStarted;
		}
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
	}

	@Nonnull
	protected PatientOrderService getPatientOrderService() {
		return this.patientOrderServiceProvider.get();
	}

	@Nonnull
	protected S3Client getS3Client() {
		return this.s3Client;
	}

	@Nonnull
	protected Hl7Client getHl7Client() {
		return this.hl7Client;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected ErrorReporter getErrorReporter() {
		return this.errorReporter;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	protected Long getBackgroundTaskIntervalInSeconds() {
		return BACKGROUND_TASK_INTERVAL_IN_SECONDS;
	}

	@Nonnull
	protected Long getBackgroundTaskInitialDelayInSeconds() {
		return BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS;
	}

	@Nonnull
	protected Object getBackgroundTaskLock() {
		return this.backgroundTaskLock;
	}

	@Nonnull
	protected Provider<BackgroundSyncTask> getBackgroundSyncTaskProvider() {
		return this.backgroundSyncTaskProvider;
	}

	@Nonnull
	protected Optional<ScheduledExecutorService> getBackgroundTaskExecutorService() {
		return Optional.ofNullable(this.backgroundTaskExecutorService);
	}
}