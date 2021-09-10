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

package com.cobaltplatform.api.context;

import com.cobaltplatform.api.cache.Cache;
import com.cobaltplatform.api.cache.CaffeineCache;
import com.cobaltplatform.api.error.ErrorReporter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class CurrentContextExecutor {
	@Nonnull
	private static final ThreadLocal<CurrentContext> CURRENT_CONTEXT_HOLDER;
	@Nonnull
	private static final ThreadLocal<Cache> CURRENT_CONTEXT_CACHE_HOLDER;
	@Nonnull
	private static final Integer DEFAULT_CURRENT_CONTEXT_CACHE_SIZE;

	@Nonnull
	private final ErrorReporter errorReporter;
	@Nonnull
	private final Integer currentContextCacheSize;

	static {
		CURRENT_CONTEXT_HOLDER = new ThreadLocal<>();
		CURRENT_CONTEXT_CACHE_HOLDER = new ThreadLocal<>();
		DEFAULT_CURRENT_CONTEXT_CACHE_SIZE = 100;
	}

	public CurrentContextExecutor(@Nonnull ErrorReporter errorReporter) {
		this(errorReporter, null);
	}

	public CurrentContextExecutor(@Nonnull ErrorReporter errorReporter,
																@Nullable Integer currentContextCacheSize) {
		requireNonNull(errorReporter);

		this.errorReporter = errorReporter;
		this.currentContextCacheSize = currentContextCacheSize == null ? getDefaultCurrentContextCacheSize() : currentContextCacheSize;
	}

	public void execute(@Nonnull CurrentContextOperation currentContextOperation) throws Exception {
		requireNonNull(currentContextOperation);

		// Creates a "default" current context
		execute(new CurrentContext.Builder(Locale.getDefault(), ZoneId.systemDefault()).build(), currentContextOperation);
	}

	public void execute(@Nonnull CurrentContext currentContext,
											@Nonnull CurrentContextOperation currentContextOperation) {
		requireNonNull(currentContext);
		requireNonNull(currentContextOperation);

		boolean createErrorScope = getErrorReporter().currentScope().isEmpty();

		if (createErrorScope)
			getErrorReporter().startScope();

		CurrentContext previousCurrentContext = getCurrentContextHolder().get();
		Cache previousCurrentContextCache = getCurrentContextCacheHolder().get();

		boolean shouldRestorePreviousContext = previousCurrentContext != null && previousCurrentContextCache != null;

		Cache currentContextCache = createCurrentContextCache();

		getCurrentContextCacheHolder().set(currentContextCache);
		getCurrentContextHolder().set(currentContext);

		getErrorReporter().applyCurrentContext(currentContext);

		try {
			currentContextOperation.execute();
		} catch (Exception e) {
			getErrorReporter().report(e);
			throw e;
		} finally {
			getCurrentContextHolder().remove();
			getCurrentContextCacheHolder().remove();

			if (shouldRestorePreviousContext) {
				getCurrentContextCacheHolder().set(previousCurrentContextCache);
				getCurrentContextHolder().set(previousCurrentContext);
			}

			getErrorReporter().applyCurrentContext(null);

			if (createErrorScope)
				getErrorReporter().endScope();
		}
	}

	@FunctionalInterface
	public interface CurrentContextOperation {
		void execute();
	}

	@Nonnull
	public Optional<CurrentContext> getCurrentContext() {
		return Optional.ofNullable(CURRENT_CONTEXT_HOLDER.get());
	}

	@Nonnull
	public Optional<Cache> getCurrentContextCache() {
		return Optional.ofNullable(CURRENT_CONTEXT_CACHE_HOLDER.get());
	}

	@Nonnull
	protected Cache createCurrentContextCache() {
		return new CaffeineCache(currentContextCacheSize);
	}

	@Nonnull
	protected ThreadLocal<CurrentContext> getCurrentContextHolder() {
		return CURRENT_CONTEXT_HOLDER;
	}

	@Nonnull
	protected ThreadLocal<Cache> getCurrentContextCacheHolder() {
		return CURRENT_CONTEXT_CACHE_HOLDER;
	}

	@Nonnull
	protected Integer getDefaultCurrentContextCacheSize() {
		return DEFAULT_CURRENT_CONTEXT_CACHE_SIZE;
	}

	@Nonnull
	protected ErrorReporter getErrorReporter() {
		return errorReporter;
	}

	@Nonnull
	protected Integer getCurrentContextCacheSize() {
		return currentContextCacheSize;
	}
}