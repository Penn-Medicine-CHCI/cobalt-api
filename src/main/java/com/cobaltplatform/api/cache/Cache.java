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

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Transmogrify, LLC.
 */
public interface Cache {
	@Nonnull
	<T> Optional<T> get(@Nonnull String key,
											@Nonnull Class<T> type);

	@Nonnull
	<T> Optional<List<T>> getList(@Nonnull String key,
																@Nonnull Class<T> type);

	@Nonnull
	<T> T get(@Nonnull String key,
						@Nonnull Supplier<T> supplier,
						@Nonnull Class<T> type);

	@Nonnull
	<T> List<T> getList(@Nonnull String key,
											@Nonnull Supplier<List<T>> supplier,
											@Nonnull Class<T> type);

	void put(@Nonnull String key, @Nonnull Object value);

	void invalidate(@Nonnull String key);

	void invalidateAll();

	@Nonnull
	Set<String> getKeys();
}