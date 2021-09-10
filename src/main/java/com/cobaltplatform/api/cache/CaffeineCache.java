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

package com.cobaltplatform.api.cache;

import com.github.benmanes.caffeine.cache.Caffeine;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class CaffeineCache implements Cache {
	@Nonnull
	private final com.github.benmanes.caffeine.cache.Cache cache;

	public CaffeineCache(@Nonnegative long maximumSize) {
		if (maximumSize < 0)
			throw new IllegalArgumentException(format("Maximum size cannot be negative. You specified %d", maximumSize));

		this.cache = Caffeine.newBuilder().maximumSize(maximumSize).build();
	}

	@Nonnull
	@Override
	public <T> Optional<T> get(@Nonnull String key,
														 @Nonnull Class<T> type) {
		requireNonNull(key);
		requireNonNull(type);

		return Optional.ofNullable((T) getCache().getIfPresent(key));
	}


	@Nonnull
	@Override
	public <T> Optional<List<T>> getList(@Nonnull String key,
																			 @Nonnull Class<T> type) {
		requireNonNull(key);
		requireNonNull(type);

		return Optional.ofNullable((List<T>) getCache().getIfPresent(key));
	}

	@Nonnull
	@Override
	public <T> T get(@Nonnull String key,
									 @Nonnull Supplier<T> supplier,
									 @Nonnull Class<T> type) {
		requireNonNull(key);
		requireNonNull(supplier);
		requireNonNull(type);

		return (T) getCache().get(key, (ignored) -> supplier.get());
	}

	@Nonnull
	@Override
	public <T> List<T> getList(@Nonnull String key,
														 @Nonnull Supplier<List<T>> supplier,
														 @Nonnull Class<T> type) {
		requireNonNull(key);
		requireNonNull(supplier);
		requireNonNull(type);

		return (List<T>) getCache().get(key, (ignored) -> supplier.get());
	}

	@Override
	public void put(@Nonnull String key,
									@Nonnull Object value) {
		requireNonNull(key);
		requireNonNull(value);

		getCache().put(key, value);
	}

	@Override
	public void invalidate(@Nonnull String key) {
		requireNonNull(key);
		getCache().invalidate(key);
	}

	@Override
	public void invalidateAll() {
		getCache().invalidateAll();
	}

	@Nonnull
	@Override
	public Set<String> getKeys() {
		return getCache().asMap().keySet();
	}

	protected com.github.benmanes.caffeine.cache.Cache getCache() {
		return this.cache;
	}
}
