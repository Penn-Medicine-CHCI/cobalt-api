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

package com.cobaltplatform.api.model.api.response;

import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.cobaltplatform.api.context.CurrentContext;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.ZoneId;
import java.time.format.TextStyle;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class TimeZoneApiResponse {
	@Nonnull
	private final ZoneId timeZone;
	@Nonnull
	private final String description;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface TimeZoneApiResponseFactory {
		@Nonnull
		TimeZoneApiResponse create(@Nonnull ZoneId timeZone);
	}

	@AssistedInject
	public TimeZoneApiResponse(@Nonnull Provider<CurrentContext> currentContextProvider,
														 @Assisted @Nonnull ZoneId timeZone) {
		requireNonNull(currentContextProvider);
		requireNonNull(timeZone);

		this.timeZone = timeZone;

		String friendlyName = timeZone.getDisplayName(TextStyle.FULL, currentContextProvider.get().getLocale());
		this.description = format("%s (%s)", friendlyName, timeZone.getId());
	}

	@Nonnull
	public ZoneId getTimeZone() {
		return timeZone;
	}

	@Nonnull
	public String getDescription() {
		return description;
	}
}