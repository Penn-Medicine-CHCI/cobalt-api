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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.service.AvailabilityTime;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Provider;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AvailabilityTimeApiResponse {
	@Nonnull
	private final LocalTime startTime;
	@Nonnull
	private final String startTimeDescription;
	@Nonnull
	private final LocalTime endTime;
	@Nonnull
	private final String endTimeDescription;
	@Nonnull
	private final Long durationInMinutes;
	@Nonnull
	private final String durationInMinutesDescription;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface AvailabilityTimeApiResponseFactory {
		@Nonnull
		AvailabilityTimeApiResponse create(@Nonnull AvailabilityTime availabilityTime);
	}

	@AssistedInject
	public AvailabilityTimeApiResponse(@Nonnull Formatter formatter,
																		 @Nonnull Strings strings,
																		 @Nonnull Provider<CurrentContext> currentContextProvider,
																		 @Assisted @Nonnull AvailabilityTime availabilityTime) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);
		requireNonNull(availabilityTime);

		Locale locale = currentContextProvider.get().getLocale();

		this.startTime = availabilityTime.getStartTime();
		this.startTimeDescription = normalizeTimeFormat(formatter.formatTime(availabilityTime.getStartTime(), FormatStyle.SHORT), locale);
		this.endTime = availabilityTime.getEndTime();
		this.endTimeDescription = normalizeTimeFormat(formatter.formatTime(availabilityTime.getEndTime(), FormatStyle.SHORT), locale);

		Duration duration = Duration.between(availabilityTime.getStartTime(), availabilityTime.getEndTime());

		this.durationInMinutes = duration.toMinutes();
		this.durationInMinutesDescription = strings.get("{{duration}} minutes", new HashMap<String, Object>() {{
			put("duration", getDurationInMinutes());
		}});
	}

	@Nonnull
	protected String normalizeTimeFormat(@Nonnull String timeDescription,
																			 @Nonnull Locale locale) {
		requireNonNull(timeDescription);
		requireNonNull(locale);

		// Turns "10:00 AM" into "10:00am", for example
		return timeDescription.replace(" ", "").toLowerCase(locale);
	}

	@Nonnull
	public LocalTime getStartTime() {
		return startTime;
	}

	@Nonnull
	public String getStartTimeDescription() {
		return startTimeDescription;
	}

	@Nonnull
	public LocalTime getEndTime() {
		return endTime;
	}

	@Nonnull
	public String getEndTimeDescription() {
		return endTimeDescription;
	}

	@Nonnull
	public Long getDurationInMinutes() {
		return durationInMinutes;
	}

	@Nonnull
	public String getDurationInMinutesDescription() {
		return durationInMinutesDescription;
	}
}