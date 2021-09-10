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
import com.cobaltplatform.api.integration.acuity.AcuitySchedulingCache;
import com.cobaltplatform.api.integration.acuity.AcuitySchedulingClient;
import com.cobaltplatform.api.integration.acuity.model.AcuityClass;
import com.cobaltplatform.api.model.db.ExternalGroupEventType;
import com.cobaltplatform.api.model.db.GroupEventType;
import com.cobaltplatform.api.model.db.GroupSessionSystem.GroupSessionSystemId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.service.GroupEvent;
import com.cobaltplatform.api.util.DatabaseUtility;
import com.pyranid.Database;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlInListPlaceholders;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
@Deprecated
public class GroupEventService {
	@Nonnull
	private final javax.inject.Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final AcuitySchedulingClient acuitySchedulingClient;
	@Nonnull
	private final AcuitySchedulingCache acuitySchedulingCache;
	@Nonnull
	private final javax.inject.Provider<ProviderService> providerServiceProvider;

	@Inject
	public GroupEventService(@Nonnull javax.inject.Provider<InstitutionService> institutionServiceProvider,
													 @Nonnull Database database,
													 @Nonnull Configuration configuration,
													 @Nonnull AcuitySchedulingClient acuitySchedulingClient,
													 @Nonnull AcuitySchedulingCache acuitySchedulingCache,
													 @Nonnull javax.inject.Provider<ProviderService> providerServiceProvider) {
		requireNonNull(institutionServiceProvider);
		requireNonNull(database);
		requireNonNull(configuration);
		requireNonNull(acuitySchedulingClient);
		requireNonNull(acuitySchedulingCache);
		requireNonNull(providerServiceProvider);

		this.institutionServiceProvider = institutionServiceProvider;
		this.database = database;
		this.configuration = configuration;
		this.acuitySchedulingClient = acuitySchedulingClient;
		this.acuitySchedulingCache = acuitySchedulingCache;
		this.providerServiceProvider = providerServiceProvider;
	}

	@Nonnull
	public List<GroupEvent> findGroupEventsByInstitutionId(@Nonnull InstitutionId institutionId,
																												 @Nonnull ZoneId timeZone) {
		requireNonNull(institutionId);
		requireNonNull(timeZone);

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();

		if (!institution.getGroupSessionSystemId().equals(GroupSessionSystemId.ACUITY))
			return Collections.emptyList();

		LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.now(), timeZone);
		YearMonth currentYearMonth = YearMonth.of(dateTime.getYear(), dateTime.getMonth());

		List<YearMonth> yearMonths = List.of(
				currentYearMonth,
				currentYearMonth.plusMonths(1)
		);

		List<CompletableFuture<Void>> groupEventFutures = new ArrayList<>(yearMonths.size());
		List<AcuityClass> acuityClasses = new CopyOnWriteArrayList<>();

		for (YearMonth yearMonth : yearMonths) {
			groupEventFutures.add(CompletableFuture.supplyAsync(() -> {
				//groupEvents.addAll(findGroupEventsByInstitutionId(institutionId, yearMonth, timeZone));
				acuityClasses.addAll(getAcuitySchedulingCache().findAvailabilityClasses(yearMonth, timeZone));
				return null;
			}));
		}

		// Combine all futures into a single one so we can wait for everything to complete
		CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(groupEventFutures.toArray(
				new CompletableFuture[groupEventFutures.size()]));

		// Don't wait too long for results...
		try {
			combinedFuture.get(15, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new RuntimeException("Unable to fetch Acuity classes", e);
		}

		Set<Long> acuityAppointmentTypeIds = acuityClasses.stream().map(acuityClass -> acuityClass.getAppointmentTypeId()).collect(Collectors.toSet());
		Map<Long, GroupEventType> groupEventTypesByAcuityAppointmentTypeId = findGroupEventTypesByAcuityAppointmentTypeId(acuityAppointmentTypeIds, institutionId);
		Instant now = Instant.now();

		return acuityClasses.stream()
				// If we don't have a group_event_type record for this Acuity class, throw it away - it's not supported yet in our system
				.filter((acuityClass) -> groupEventTypesByAcuityAppointmentTypeId.containsKey(acuityClass.getAppointmentTypeId()))
				.map((acuityClass) -> toGroupEvent(acuityClass, groupEventTypesByAcuityAppointmentTypeId.get(acuityClass.getAppointmentTypeId())))
				.filter(groupEvent -> groupEvent.getStartTime().isAfter(now))
				.sorted((groupEvent1, groupEvent2) -> groupEvent1.getStartTime().compareTo(groupEvent2.getStartTime()))
				.collect(Collectors.toList());
	}

	@Nonnull
	public Optional<GroupEvent> findGroupEventById(@Nonnull String groupEventId,
																								 @Nonnull InstitutionId institutionId,
																								 @Nonnull ZoneId timeZone) {
		requireNonNull(groupEventId);
		requireNonNull(institutionId);
		requireNonNull(timeZone);

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();

		if (!institution.getGroupSessionSystemId().equals(GroupSessionSystemId.ACUITY))
			return Optional.empty();

		// Acuity API does not seem to allow fetching a class by ID (class != appointment)
		// ...so we go looking through the list of classes for the ID
		List<GroupEvent> groupEvents = findGroupEventsByInstitutionId(institutionId, timeZone);

		for (GroupEvent groupEvent : groupEvents) {
			if (groupEvent.getGroupEventId().equals(groupEventId))
				return Optional.of(groupEvent);
		}

		return Optional.empty();
	}

	@Nonnull
	public Optional<GroupEventType> findGroupEventTypeById(@Nullable UUID groupEventTypeId,
																												 @Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);

		if (groupEventTypeId == null)
			return Optional.empty();

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();

		if (!institution.getGroupSessionSystemId().equals(GroupSessionSystemId.ACUITY))
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM group_event_type WHERE group_event_type_id=?", GroupEventType.class, groupEventTypeId);
	}

	@Nonnull
	public Optional<GroupEventType> findGroupEventTypeByUrlName(@Nullable String urlName,
																															@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();

		if (!institution.getGroupSessionSystemId().equals(GroupSessionSystemId.ACUITY))
			return Optional.empty();

		urlName = trimToNull(urlName);

		if (urlName == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM group_event_type WHERE url_name=?", GroupEventType.class, urlName);
	}

	@Nonnull
	public List<ExternalGroupEventType> findExternalGroupEventTypesByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM external_group_event_type " +
				"WHERE institution_id=? AND available=TRUE ORDER BY display_order", ExternalGroupEventType.class, institutionId);
	}

	@Nonnull
	public Optional<ExternalGroupEventType> findExternalGroupEventTypeById(@Nullable UUID externalGroupEventTypeId,
																																				 @Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();

		if (!institution.getGroupSessionSystemId().equals(GroupSessionSystemId.ACUITY))
			return Optional.empty();

		if (externalGroupEventTypeId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM external_group_event_type " +
				"WHERE external_group_event_type_id=? AND available=TRUE", ExternalGroupEventType.class, externalGroupEventTypeId);
	}

	@Nonnull
	protected Map<Long, GroupEventType> findGroupEventTypesByAcuityAppointmentTypeId(@Nullable Set<Long> acuityAppointmentTypeIds,
																																									 @Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();

		if (!institution.getGroupSessionSystemId().equals(GroupSessionSystemId.ACUITY))
			return Collections.emptyMap();

		if (acuityAppointmentTypeIds == null || acuityAppointmentTypeIds.size() == 0)
			return Collections.emptyMap();

		List<GroupEventType> groupEventTypes = getDatabase().queryForList(format("SELECT * FROM group_event_type WHERE acuity_appointment_type_id IN %s",
				sqlInListPlaceholders(acuityAppointmentTypeIds)), GroupEventType.class, DatabaseUtility.sqlVaragsParameters(acuityAppointmentTypeIds));

		Map<Long, GroupEventType> groupEventTypesByAcuityAppointmentTypeId = new HashMap<>(groupEventTypes.size());

		for (GroupEventType groupEventType : groupEventTypes)
			groupEventTypesByAcuityAppointmentTypeId.put(groupEventType.getAcuityAppointmentTypeId(), groupEventType);

		return groupEventTypesByAcuityAppointmentTypeId;
	}

	@Nonnull
	protected GroupEvent toGroupEvent(@Nonnull AcuityClass acuityClass,
																		@Nonnull GroupEventType groupEventType) {
		requireNonNull(acuityClass);
		requireNonNull(groupEventType);

		Instant startTime = getAcuitySchedulingClient().parseAcuityTime(acuityClass.getTime());

		GroupEvent groupEvent = new GroupEvent();
		groupEvent.setGroupEventId(String.valueOf(acuityClass.getId()));
		groupEvent.setGroupEventTypeId(groupEventType.getGroupEventTypeId());
		groupEvent.setName(trimToNull(acuityClass.getName()));
		groupEvent.setDescription(acuityClass.getDescription());
		groupEvent.setStartTime(startTime);
		groupEvent.setDurationInMinutes(acuityClass.getDuration());
		groupEvent.setEndTime(startTime.plus(acuityClass.getDuration(), ChronoUnit.MINUTES));
		groupEvent.setSeats(acuityClass.getSlots());
		groupEvent.setSeatsAvailable(acuityClass.getSlotsAvailable());
		groupEvent.setTimeZone(ZoneId.of(acuityClass.getCalendarTimezone()));
		groupEvent.setProvider(getProviderService().findProviderByAcuityCalendarId(acuityClass.getCalendarId()).orElse(null));
		groupEvent.setVideoconferenceUrl(groupEventType.getVideoconferenceUrl());
		groupEvent.setImageUrl(groupEventType.getImageUrl());

		return groupEvent;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return institutionServiceProvider.get();
	}

	@Nonnull
	protected Database getDatabase() {
		return database;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected AcuitySchedulingClient getAcuitySchedulingClient() {
		return acuitySchedulingClient;
	}

	@Nonnull
	protected AcuitySchedulingCache getAcuitySchedulingCache() {
		return acuitySchedulingCache;
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return providerServiceProvider.get();
	}
}
