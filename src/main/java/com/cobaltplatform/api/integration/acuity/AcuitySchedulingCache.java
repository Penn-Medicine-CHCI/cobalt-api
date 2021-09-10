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

package com.cobaltplatform.api.integration.acuity;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.integration.acuity.model.AcuityClass;
import com.cobaltplatform.api.integration.acuity.model.AcuityTime;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
@Singleton
public class AcuitySchedulingCache {
	@Nonnull
	private final LoadingCache<String, List<AcuityTime>> availabilityTimesCache;
	@Nonnull
	private final LoadingCache<String, List<AcuityClass>> availabilityClassesCache;

	@Nonnull
	private final AcuitySchedulingClient acuitySchedulingClient;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Logger logger;

	@Inject
	public AcuitySchedulingCache(@Nonnull AcuitySchedulingClient acuitySchedulingClient,
															 @Nonnull Configuration configuration) {
		requireNonNull(acuitySchedulingClient);
		requireNonNull(configuration);

		this.availabilityTimesCache = createAvailabilityTimesCache();
		this.availabilityClassesCache = createAvailabilityClassesCache();
		this.acuitySchedulingClient = acuitySchedulingClient;
		this.configuration = configuration;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public List<AcuityTime> findAvailabilityTimes(@Nonnull Long calendarId,
																								@Nonnull Long appointmentTypeId,
																								@Nonnull LocalDate localDate,
																								@Nonnull ZoneId timeZone) {
		requireNonNull(calendarId);
		requireNonNull(appointmentTypeId);
		requireNonNull(localDate);
		requireNonNull(timeZone);

		// Key format:
		// {calendarId}:{appointmentTypeId}:{localDate}:{timeZone}
		// e.g.
		// 3829372:3119372:2020-04-20:America/New_York
		String key = format("%s:%s:%s:%s", calendarId, appointmentTypeId, localDate, timeZone.getId());

		return getAvailabilityTimesCache().get(key);
	}

	@Nonnull
	protected List<AcuityTime> findAvailabilityTimesInternal(@Nonnull String key) {
		requireNonNull(key);

		getLogger().debug("Cache miss for availability times {}...", key);

		String[] keyComponents = key.split(":");

		Long calendarId = Long.valueOf(keyComponents[0]);
		Long appointmentTypeId = Long.valueOf(keyComponents[1]);
		LocalDate localDate = LocalDate.parse(keyComponents[2]);
		ZoneId timeZone = ZoneId.of(keyComponents[3]);

		return getAcuitySchedulingClient().findAvailabilityTimes(calendarId, appointmentTypeId, localDate, timeZone);
	}

	@Nonnull
	public List<AcuityClass> findAvailabilityClasses(@Nonnull YearMonth yearMonth,
																									 @Nonnull ZoneId timeZone) {
		requireNonNull(yearMonth);
		requireNonNull(timeZone);

		// Key format:
		// {yearMonth}:{timeZone}
		// e.g.
		// 2020-04:America/New_York
		String key = format("%s:%s", yearMonth, timeZone.getId());

		return getAvailabilityClassesCache().get(key);
	}

	@Nonnull
	protected List<AcuityClass> findAvailabilityClassesInternal(@Nonnull String key) {
		requireNonNull(key);

		getLogger().debug("Cache miss for availability classes {}...", key);

		String[] keyComponents = key.split(":");

		YearMonth yearMonth = YearMonth.parse(keyComponents[0]);
		ZoneId timeZone = ZoneId.of(keyComponents[1]);

		return getAcuitySchedulingClient().findAvailabilityClasses(yearMonth, timeZone);
	}

	public void invalidateAvailability(@Nonnull LocalDate date,
																		 @Nonnull ZoneId timeZone) {
		requireNonNull(date);
		requireNonNull(timeZone);

		// TODO: for MVP, we are actually ignoring timezone...
		getLogger().debug("Invalidating Acuity availability cache for {} at {}...", date, timeZone);

		Set<String> availabilityTimeKeys = getAvailabilityTimesCache().asMap().keySet();
		Set<String> availabilityTimeKeysToInvalidate = new HashSet<>();
		Set<String> availabilityClassKeys = getAvailabilityClassesCache().asMap().keySet();
		Set<String> availabilityClassKeysToInvalidate = new HashSet<>();

		String dateString = date.toString();

		for (String availabilityTimeKey : availabilityTimeKeys) {
			if (availabilityTimeKey.contains(dateString))
				availabilityTimeKeysToInvalidate.add(availabilityTimeKey);
		}

		for (String availabilityClassKey : availabilityClassKeys) {
			if (availabilityClassKey.startsWith(dateString))
				availabilityClassKeysToInvalidate.add(availabilityClassKey);
		}

		getLogger().debug("Invalidating {} availability time keys...", availabilityTimeKeysToInvalidate.size());
		getAvailabilityTimesCache().invalidateAll(availabilityTimeKeysToInvalidate);

		getLogger().debug("Invalidating {} availability class keys...", availabilityClassKeysToInvalidate.size());
		getAvailabilityClassesCache().invalidateAll(availabilityClassKeysToInvalidate);
	}

	@Nonnull
	public Set<String> getAvailabilityTimesCacheKeys() {
		return getAvailabilityTimesCache().asMap().keySet();
	}

	public void invalidateAvailabilityTimesCache() {
		getAvailabilityTimesCache().invalidateAll();
	}

	@Nonnull
	public Set<String> getAvailabilityClassesCacheKeys() {
		return getAvailabilityClassesCache().asMap().keySet();
	}

	public void invalidateAvailabilityClassesCache() {
		getAvailabilityClassesCache().invalidateAll();
	}

	@Nonnull
	protected LoadingCache<String, List<AcuityTime>> createAvailabilityTimesCache() {
		return Caffeine.newBuilder()
				.expireAfterWrite(180, TimeUnit.SECONDS)
				.refreshAfterWrite(60, TimeUnit.SECONDS)
				.build(key -> findAvailabilityTimesInternal(key));
	}

	@Nonnull
	protected LoadingCache<String, List<AcuityClass>> createAvailabilityClassesCache() {
		return Caffeine.newBuilder()
				.expireAfterWrite(5, TimeUnit.MINUTES)
				.refreshAfterWrite(1, TimeUnit.MINUTES)
				.build(key -> findAvailabilityClassesInternal(key));
	}

	@Nonnull
	protected LoadingCache<String, List<AcuityTime>> getAvailabilityTimesCache() {
		return availabilityTimesCache;
	}

	@Nonnull
	protected LoadingCache<String, List<AcuityClass>> getAvailabilityClassesCache() {
		return availabilityClassesCache;
	}

	@Nonnull
	protected AcuitySchedulingClient getAcuitySchedulingClient() {
		return acuitySchedulingClient;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
