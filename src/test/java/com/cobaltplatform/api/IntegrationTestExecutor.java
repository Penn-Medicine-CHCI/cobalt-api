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

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.pyranid.Database;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public final class IntegrationTestExecutor {
	private IntegrationTestExecutor() {
		// Non-instantiable
	}

	public static void run(@Nonnull AppFunction<App> appFunction,
												 @Nullable Module... modules) {
		requireNonNull(appFunction);

		try (App app = new App(new Configuration("local"), normalizeModules(modules))) {
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			currentContextExecutor.execute(new CurrentContext.Builder(InstitutionId.COBALT, Locale.US, ZoneId.of("America/New_York")).build(), () -> {
				app.performStartupTasks();

				try {
					appFunction.accept(app);
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void runTransactionallyAndForceRollback(@Nonnull AppFunction<App> appFunction,
																												@Nullable Module... modules) {
		requireNonNull(appFunction);
		runTransactionally(true, appFunction, modules);
	}

	public static void runTransactionallyAndCommit(@Nonnull AppFunction<App> appFunction,
																								 @Nullable Module... modules) {
		requireNonNull(appFunction);
		runTransactionally(false, appFunction, modules);
	}

	private static void runTransactionally(@Nonnull Boolean rollbackOnly,
																				 @Nonnull AppFunction<App> appFunction,
																				 @Nullable Module... modules) {
		requireNonNull(rollbackOnly);
		requireNonNull(appFunction);

		run((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();

			database.transaction(() -> {
				appFunction.accept(app);
				// If true, force rollback at the end of the test even if successful (exception will also cause rollback)
				database.currentTransaction().get().setRollbackOnly(rollbackOnly);
			});
		}, modules);
	}

	@FunctionalInterface
	public interface AppFunction<T> {
		void accept(T t) throws Exception;
	}

	@Nonnull
	private static Module[] normalizeModules(@Nullable Module[] modules) {
		modules = modules == null || modules.length == 0 ? new Module[1] : modules;
		Module[] newModules = new Module[modules.length + 1];

		// Hijack module list to have provided configuration object always return "true" for integration test flag
		newModules[0] = new AbstractModule() {
			@Provides
			@Singleton
			@Nonnull
			public Configuration provideConfiguration() {
				return new Configuration() {
					@Nonnull
					@Override
					public Boolean isRunningInIntegrationTestMode() {
						return true;
					}
				};
			}
		};

		for (int i = 0; i < modules.length; ++i)
			newModules[i + 1] = modules[i];

		return Arrays.stream(newModules)
				.filter(module -> module != null)
				.toArray(Module[]::new);
	}
}
