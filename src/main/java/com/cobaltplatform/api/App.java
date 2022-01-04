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
import com.cobaltplatform.api.integration.epic.EpicSyncManager;
import com.cobaltplatform.api.messaging.call.CallMessageManager;
import com.cobaltplatform.api.messaging.email.EmailMessageManager;
import com.cobaltplatform.api.messaging.sms.SmsMessageManager;
import com.cobaltplatform.api.service.GroupSessionService;
import com.cobaltplatform.api.service.Way2HealthService;
import com.google.inject.Guice;
import com.google.inject.Injector;
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

		initializeLogback(Paths.get(format("config/%s/logback.xml", configuration.getEnvironment())));

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
		App app = new App(new Configuration());
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
			SmsMessageManager smsMessageManager = getInjector().getInstance(SmsMessageManager.class);
			smsMessageManager.start();
		} catch (Exception e) {
			getLogger().warn("Failed to start SMS message manager", e);
		}

		try {
			CallMessageManager callMessageManager = getInjector().getInstance(CallMessageManager.class);
			callMessageManager.start();
		} catch (Exception e) {
			getLogger().warn("Failed to start call message manager", e);
		}

		try {
			EmailMessageManager emailMessageManager = getInjector().getInstance(EmailMessageManager.class);
			emailMessageManager.start();
		} catch (Exception e) {
			getLogger().warn("Failed to start email manager", e);
		}

		if (!configuration.getEnvironment().equalsIgnoreCase("LOCAL")) {
			try {
				BluejeansCredentialsProvider bluejeansCredentialsProvider = getInjector().getInstance(BluejeansCredentialsProvider.class);
				bluejeansCredentialsProvider.start();
			} catch (Exception e) {
				getLogger().warn("Failed to start bluejeans credential provider", e);
			}
		}

		try {
			AcuitySyncManager acuitySyncManager = getInjector().getInstance(AcuitySyncManager.class);
			acuitySyncManager.start();
		} catch (Exception e) {
			getLogger().warn("Failed to start Acuity sync manager", e);
		}

		try {
			EpicSyncManager epicSyncManager = getInjector().getInstance(EpicSyncManager.class);
			epicSyncManager.start();
		} catch (Exception e) {
			getLogger().warn("Failed to start EPIC sync manager", e);
		}

		try {
			GroupSessionService groupSessionService = getInjector().getInstance(GroupSessionService.class);
			groupSessionService.startBackgroundTask();
		} catch (Exception e) {
			getLogger().warn("Failed to start Group Session background task", e);
		}

		try {
			Way2HealthService way2HealthService = getInjector().getInstance(Way2HealthService.class);
			way2HealthService.startBackgroundTask();
		} catch (Exception e) {
			getLogger().warn("Failed to start Way2Health background task", e);
		}
	}

	public void performShutdownTasks() {
		try {
			Way2HealthService way2HealthService = getInjector().getInstance(Way2HealthService.class);
			way2HealthService.stopBackgroundTask();
		} catch (Exception e) {
			getLogger().warn("Failed to stop Way2Health background task", e);
		}

		try {
			GroupSessionService groupSessionService = getInjector().getInstance(GroupSessionService.class);
			groupSessionService.stopBackgroundTask();
		} catch (Exception e) {
			getLogger().warn("Failed to stop Group Session background task", e);
		}

		try {
			EpicSyncManager epicSyncManager = getInjector().getInstance(EpicSyncManager.class);
			epicSyncManager.stop();
		} catch (Exception e) {
			getLogger().warn("Unable to stop EPIC sync manager", e);
		}

		try {
			AcuitySyncManager acuitySyncManager = getInjector().getInstance(AcuitySyncManager.class);
			acuitySyncManager.stop();
		} catch (Exception e) {
			getLogger().warn("Unable to stop Acuity sync manager", e);
		}

		try {
			EmailMessageManager emailMessageManager = getInjector().getInstance(EmailMessageManager.class);
			emailMessageManager.stop();
		} catch (Exception e) {
			getLogger().warn("Unable to stop email message manager", e);
		}

		try {
			SmsMessageManager smsMessageManager = getInjector().getInstance(SmsMessageManager.class);
			smsMessageManager.stop();
		} catch (Exception e) {
			getLogger().warn("Failed to stop SMS message manager", e);
		}

		try {
			CallMessageManager callMessageManager = getInjector().getInstance(CallMessageManager.class);
			callMessageManager.stop();
		} catch (Exception e) {
			getLogger().warn("Failed to stop call message manager", e);
		}

		if (!configuration.getEnvironment().equalsIgnoreCase("LOCAL")) {
			try {
				BluejeansCredentialsProvider bluejeansCredentialsProvider = getInjector().getInstance(BluejeansCredentialsProvider.class);
				bluejeansCredentialsProvider.shutdown();
			} catch (Exception e) {
				getLogger().warn("Unable to stop bluejeans credentials provider");
			}
		}

		try {
			DataSource dataSource = getInjector().getInstance(DataSource.class);

			if (dataSource instanceof Closeable)
				((Closeable) dataSource).close();
		} catch (Exception e) {
			getLogger().warn("Unable to close datasource", e);
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