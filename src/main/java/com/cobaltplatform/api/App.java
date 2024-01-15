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

package com.cobaltplatform.api;

import com.cobaltplatform.api.integration.acuity.AcuitySyncManager;
import com.cobaltplatform.api.integration.bluejeans.BluejeansCredentialsProvider;
import com.cobaltplatform.api.integration.epic.EpicFhirSyncManager;
import com.cobaltplatform.api.integration.epic.EpicSyncManager;
import com.cobaltplatform.api.service.AnalyticsService;
import com.cobaltplatform.api.service.AvailabilityService;
import com.cobaltplatform.api.service.ContentService;
import com.cobaltplatform.api.service.GroupSessionService;
import com.cobaltplatform.api.service.MessageService;
import com.cobaltplatform.api.service.PatientOrderService;
import com.cobaltplatform.api.service.Way2HealthService;
import com.cobaltplatform.api.util.db.ReadReplica;
import com.cobaltplatform.api.util.db.WritableMaster;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.soklet.guice.SokletModule;
import com.soklet.web.server.Server;
import com.soklet.web.server.ServerException;
import com.soklet.web.server.ServerLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.io.Closeable;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;

import static com.soklet.util.LoggingUtils.initializeLogback;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class App implements AutoCloseable {
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Injector injector;
	@Nonnull
	private final Logger logger;

	public App(@Nonnull Configuration configuration) {
		this(configuration, Collections.emptyList());
	}

	public App(@Nonnull Configuration configuration, @Nullable Module... overrideModules) {
		this(configuration, overrideModules == null ? Collections.emptyList() : Arrays.asList(overrideModules));
	}

	public App(@Nonnull Configuration configuration, @Nonnull Iterable<Module> overrideModules) {
		requireNonNull(configuration);
		requireNonNull(overrideModules);

		// Override system defaults for locale and timezone
		TimeZone.setDefault(TimeZone.getTimeZone(configuration.getDefaultTimeZone()));
		Locale.setDefault(configuration.getDefaultLocale());

		Logger logger = LoggerFactory.getLogger(getClass());

		// 1. Override Soklet module with our standard module
		Module module = Modules.override(new SokletModule()).with(new AppModule(configuration));

		// 2. Perform any additional overrides
		module = Modules.override(module).with(overrideModules);

		// Failsafe for more severe issues.
		// Sentry automatically hooks into this, so no need to send an event to our ErrorReporter
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
			getLogger().error(format("Uncaught exception on thread '%s'", thread.getName()), throwable);
		});

		this.configuration = configuration;
		this.logger = logger;
		this.injector = Guice.createInjector(module);
	}

	public static void main(String[] args) throws ServerException {
		// Initialize logging before we do anything else
		String environment = Configuration.determineEnvironment();
		initializeLogback(Paths.get(format("config/%s/logback.xml", environment)));

		App app = new App(new Configuration(environment));
		app.startServer();
	}

	public void startServer() throws ServerException {
		Server server = getInjector().getInstance(Server.class);

		ServerLauncher.ServerLifecycleOperation onStartupOperation = () -> performStartupTasks();

		ServerLauncher.ServerLifecycleOperation onShutdownOperation = () -> {
			getLogger().info("Shutting down...");
			performShutdownTasks();
		};

		new ServerLauncher(server).launch(getConfiguration().getServerStoppingStrategy(), onStartupOperation, onShutdownOperation);
	}

	@Override
	public void close() throws Exception {
		performShutdownTasks();
	}

	public void performStartupTasks() {
		try {
			MessageService messageService = getInjector().getInstance(MessageService.class);
			messageService.start();
		} catch (Exception e) {
			getLogger().warn("Failed to start message service", e);
		}

		if (getConfiguration().getShouldPollBluejeans()) {
			try {
				BluejeansCredentialsProvider bluejeansCredentialsProvider = getInjector().getInstance(BluejeansCredentialsProvider.class);
				bluejeansCredentialsProvider.start();
			} catch (Exception e) {
				getLogger().warn("Failed to start bluejeans credential provider", e);
			}
		}

		if (getConfiguration().getShouldPollAcuity()) {
			try {
				AcuitySyncManager acuitySyncManager = getInjector().getInstance(AcuitySyncManager.class);
				acuitySyncManager.start();
			} catch (Exception e) {
				getLogger().warn("Failed to start Acuity sync manager", e);
			}
		}

		if (getConfiguration().getShouldPollEpic()) {
			try {
				EpicSyncManager epicSyncManager = getInjector().getInstance(EpicSyncManager.class);
				epicSyncManager.start();
			} catch (Exception e) {
				getLogger().warn("Failed to start EPIC sync manager", e);
			}

			try {
				EpicFhirSyncManager epicFhirSyncManager = getInjector().getInstance(EpicFhirSyncManager.class);
				epicFhirSyncManager.start();
			} catch (Exception e) {
				getLogger().warn("Failed to start EPIC FHIR sync manager", e);
			}
		}

		try {
			GroupSessionService groupSessionService = getInjector().getInstance(GroupSessionService.class);
			groupSessionService.startBackgroundTask();
		} catch (Exception e) {
			getLogger().warn("Failed to start Group Session background task", e);
		}

		if (getConfiguration().getShouldPollWay2Health()) {
			try {
				Way2HealthService way2HealthService = getInjector().getInstance(Way2HealthService.class);
				way2HealthService.startBackgroundTask();
			} catch (Exception e) {
				getLogger().warn("Failed to start Way2Health background task", e);
			}
		}

		try {
			AvailabilityService availabilityService = getInjector().getInstance(AvailabilityService.class);
			availabilityService.startHistoryBackgroundTask();
		} catch (Exception e) {
			getLogger().warn("Failed to start Availability Service history background task", e);
		}

		try {
			PatientOrderService patientOrderService = getInjector().getInstance(PatientOrderService.class);
			patientOrderService.startBackgroundTasks();
		} catch (Exception e) {
			getLogger().warn("Failed to start Patient Order Service background tasks", e);
		}

		try {
			AnalyticsService analyticsService = getInjector().getInstance(AnalyticsService.class);
			analyticsService.startAnalyticsSync();
		} catch (Exception e) {
			getLogger().warn("Failed to start Analytics Service background sync task", e);
		}

		try {
			ContentService contentService = getInjector().getInstance(ContentService.class);
			contentService.startBackgroundTask();
			getLogger().debug("Started Content Service background task");
		} catch (Exception e) {
			getLogger().warn("Failed to start Content Service background task", e);
		}
	}

	public void performShutdownTasks() {
		try {
			AnalyticsService analyticsService = getInjector().getInstance(AnalyticsService.class);
			analyticsService.stopAnalyticsSync();
		} catch (Exception e) {
			getLogger().warn("Failed to stop Analytics Service background sync task", e);
		}

		try {
			PatientOrderService patientOrderService = getInjector().getInstance(PatientOrderService.class);
			patientOrderService.stopBackgroundTasks();
		} catch (Exception e) {
			getLogger().warn("Failed to stop Patient Order Service background tasks", e);
		}

		try {
			AvailabilityService availabilityService = getInjector().getInstance(AvailabilityService.class);
			availabilityService.stopHistoryBackgroundTask();
		} catch (Exception e) {
			getLogger().warn("Failed to stop Availability Service history background task", e);
		}

		if (getConfiguration().getShouldPollWay2Health()) {
			try {
				Way2HealthService way2HealthService = getInjector().getInstance(Way2HealthService.class);
				way2HealthService.stopBackgroundTask();
			} catch (Exception e) {
				getLogger().warn("Failed to stop Way2Health background task", e);
			}
		}

		try {
			GroupSessionService groupSessionService = getInjector().getInstance(GroupSessionService.class);
			groupSessionService.stopBackgroundTask();
		} catch (Exception e) {
			getLogger().warn("Failed to stop Group Session background task", e);
		}

		if (getConfiguration().getShouldPollEpic()) {
			try {
				EpicSyncManager epicSyncManager = getInjector().getInstance(EpicSyncManager.class);
				epicSyncManager.stop();
			} catch (Exception e) {
				getLogger().warn("Unable to stop EPIC sync manager", e);
			}

			try {
				EpicFhirSyncManager epicFhirSyncManager = getInjector().getInstance(EpicFhirSyncManager.class);
				epicFhirSyncManager.stop();
			} catch (Exception e) {
				getLogger().warn("Unable to stop EPIC FHIR sync manager", e);
			}
		}

		if (getConfiguration().getShouldPollAcuity()) {
			try {
				AcuitySyncManager acuitySyncManager = getInjector().getInstance(AcuitySyncManager.class);
				acuitySyncManager.stop();
			} catch (Exception e) {
				getLogger().warn("Unable to stop Acuity sync manager", e);
			}
		}

		try {
			MessageService messageService = getInjector().getInstance(MessageService.class);
			messageService.stop();
		} catch (Exception e) {
			getLogger().warn("Failed to stop message service", e);
		}

		if (getConfiguration().getShouldPollBluejeans()) {
			try {
				BluejeansCredentialsProvider bluejeansCredentialsProvider = getInjector().getInstance(BluejeansCredentialsProvider.class);
				bluejeansCredentialsProvider.shutdown();
			} catch (Exception e) {
				getLogger().warn("Unable to stop bluejeans credentials provider");
			}
		}

		try {
			DataSource readReplicaDataSource = getInjector().getInstance(Key.get(DataSource.class, ReadReplica.class));

			if (readReplicaDataSource instanceof Closeable)
				((Closeable) readReplicaDataSource).close();
		} catch (Exception e) {
			getLogger().warn("Unable to close read-replica datasource", e);
		}

		try {
			DataSource writableMasterDataSource = getInjector().getInstance(Key.get(DataSource.class, WritableMaster.class));

			if (writableMasterDataSource instanceof Closeable)
				((Closeable) writableMasterDataSource).close();
		} catch (Exception e) {
			getLogger().warn("Unable to close writable-master datasource", e);
		}

		try {
			ContentService contentService = getInjector().getInstance(ContentService.class);
			contentService.stopBackgroundTask();
		} catch (Exception e) {
			getLogger().warn("Failed to stop Content background task", e);
		}

		getLogger().debug("Shutdown complete.");
	}

	@Nonnull
	public Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	public Injector getInjector() {
		return this.injector;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}