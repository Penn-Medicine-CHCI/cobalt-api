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
import com.cobaltplatform.api.model.api.request.CreateLogicalAvailabilityRequest;
import com.cobaltplatform.api.model.api.request.UpdateLogicalAvailabilityRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.CalendarPermission.CalendarPermissionId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.LogicalAvailability;
import com.cobaltplatform.api.model.db.LogicalAvailabilityType.LogicalAvailabilityTypeId;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.ProviderAvailability;
import com.cobaltplatform.api.model.db.RecurrenceType.RecurrenceTypeId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlVaragsParameters;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class AvailabilityService {
	@Nonnull
	private final javax.inject.Provider<AppointmentService> appointmentServiceProvider;
	@Nonnull
	private final javax.inject.Provider<ProviderService> providerServiceProvider;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public AvailabilityService(@Nonnull javax.inject.Provider<AppointmentService> appointmentServiceProvider,
														 @Nonnull javax.inject.Provider<ProviderService> providerServiceProvider,
														 @Nonnull Database database,
														 @Nonnull Configuration configuration,
														 @Nonnull Strings strings) {
		requireNonNull(appointmentServiceProvider);
		requireNonNull(providerServiceProvider);
		requireNonNull(database);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.appointmentServiceProvider = appointmentServiceProvider;
		this.providerServiceProvider = providerServiceProvider;
		this.database = database;
		this.configuration = configuration;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<LogicalAvailability> findLogicalAvailabilityById(@Nullable UUID logicalAvailabilityId) {
		if (logicalAvailabilityId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM logical_availability WHERE logical_availability_id=?",
				LogicalAvailability.class, logicalAvailabilityId);
	}

	@Nonnull
	public UUID createLogicalAvailability(@Nonnull CreateLogicalAvailabilityRequest request) {
		requireNonNull(request);

		UUID providerId = request.getProviderId();
		UUID accountId = request.getAccountId();
		LocalDateTime startDateTime = request.getStartDateTime();
		LocalDateTime endDateTime = request.getEndDateTime();
		List<UUID> appointmentTypeIds = request.getAppointmentTypeIds() == null ? Collections.emptyList() : request.getAppointmentTypeIds();
		LogicalAvailabilityTypeId logicalAvailabilityTypeId = request.getLogicalAvailabilityTypeId();
		RecurrenceTypeId recurrenceTypeId = request.getRecurrenceTypeId();
		boolean recurSunday = request.getRecurSunday() == null ? false : request.getRecurSunday();
		boolean recurMonday = request.getRecurMonday() == null ? false : request.getRecurMonday();
		boolean recurTuesday = request.getRecurTuesday() == null ? false : request.getRecurTuesday();
		boolean recurWednesday = request.getRecurWednesday() == null ? false : request.getRecurWednesday();
		boolean recurThursday = request.getRecurThursday() == null ? false : request.getRecurThursday();
		boolean recurFriday = request.getRecurFriday() == null ? false : request.getRecurFriday();
		boolean recurSaturday = request.getRecurSaturday() == null ? false : request.getRecurSaturday();

		ValidationException validationException = new ValidationException();

		if (providerId == null) {
			validationException.add(new FieldError("providerId", getStrings().get("Provider ID is required.")));
		} else {
			Provider provider = getProviderService().findProviderById(providerId).orElse(null);

			if (provider == null)
				validationException.add(new FieldError("providerId", getStrings().get("Provider ID is invalid.")));
		}

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (startDateTime == null)
			validationException.add(new FieldError("startDateTime", getStrings().get("Start date/time is required.")));

		if (endDateTime == null)
			validationException.add(new FieldError("endDateTime", getStrings().get("End date/time is required.")));

		List<AppointmentType> appointmentTypes = appointmentTypeIds.stream()
				.filter(appointmentTypeId -> appointmentTypeId != null)
				.distinct()
				.map(appointmentTypeId -> getAppointmentService().findAppointmentTypeById(appointmentTypeId).get())
				.collect(Collectors.toList());

		if (startDateTime != null && endDateTime != null && !endDateTime.isAfter(startDateTime))
			validationException.add(getStrings().get("End time must be after start time."));

		if (logicalAvailabilityTypeId == null)
			validationException.add(new FieldError("logicalAvailabilityTypeId", getStrings().get("Availability type is required.")));

		if (recurrenceTypeId == null) {
			validationException.add(new FieldError("recurrenceTypeId", getStrings().get("Recurrence type is required.")));
		} else {
			if (recurrenceTypeId == RecurrenceTypeId.NONE) {
				recurSunday = false;
				recurMonday = false;
				recurTuesday = false;
				recurWednesday = false;
				recurThursday = false;
				recurFriday = false;
				recurSaturday = false;
			} else if (recurrenceTypeId == RecurrenceTypeId.DAILY) {
				if (!recurSunday
						&& !recurMonday
						&& !recurTuesday
						&& !recurWednesday
						&& !recurThursday
						&& !recurFriday
						&& !recurSaturday)
					validationException.add(new FieldError("recurrenceTypeId", getStrings().get("You must specify at least one recurrence day.")));
			} else {
				validationException.add(new FieldError("recurrenceTypeId", getStrings().get("Unsupported recurrence type was specified.")));
			}
		}

		if (validationException.hasErrors())
			throw validationException;

		UUID logicalAvailabilityId = UUID.randomUUID();

		getDatabase().execute("INSERT INTO logical_availability(logical_availability_id, provider_id, start_date_time, " +
						"end_date_time, logical_availability_type_id, recurrence_type_id, recur_sunday, recur_monday, recur_tuesday, " +
						"recur_wednesday, recur_thursday, recur_friday, recur_saturday, created_by_account_id, last_updated_by_account_id) " +
						"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
				logicalAvailabilityId, providerId, startDateTime, endDateTime, logicalAvailabilityTypeId, recurrenceTypeId,
				recurSunday, recurMonday, recurTuesday, recurWednesday, recurThursday, recurFriday, recurSaturday, accountId, accountId);

		// Note: if no appointment types, any active appointment type for the provider is bookable
		for (AppointmentType appointmentType : appointmentTypes)
			getDatabase().execute("INSERT INTO logical_availability_appointment_type(logical_availability_id, appointment_type_id) VALUES (?,?)",
					logicalAvailabilityId, appointmentType.getAppointmentTypeId());

		return logicalAvailabilityId;
	}

	@Nonnull
	public Boolean updateLogicalAvailability(@Nonnull UpdateLogicalAvailabilityRequest request) {
		requireNonNull(request);

		UUID logicalAvailabilityId = request.getLogicalAvailabilityId();
		UUID providerId = request.getProviderId();
		UUID accountId = request.getAccountId();
		LocalDateTime startDateTime = request.getStartDateTime();
		LocalDateTime endDateTime = request.getEndDateTime();
		List<UUID> appointmentTypeIds = request.getAppointmentTypeIds() == null ? Collections.emptyList() : request.getAppointmentTypeIds();
		LogicalAvailabilityTypeId logicalAvailabilityTypeId = request.getLogicalAvailabilityTypeId();
		RecurrenceTypeId recurrenceTypeId = request.getRecurrenceTypeId();
		boolean recurSunday = request.getRecurSunday() == null ? false : request.getRecurSunday();
		boolean recurMonday = request.getRecurMonday() == null ? false : request.getRecurMonday();
		boolean recurTuesday = request.getRecurTuesday() == null ? false : request.getRecurTuesday();
		boolean recurWednesday = request.getRecurWednesday() == null ? false : request.getRecurWednesday();
		boolean recurThursday = request.getRecurThursday() == null ? false : request.getRecurThursday();
		boolean recurFriday = request.getRecurFriday() == null ? false : request.getRecurFriday();
		boolean recurSaturday = request.getRecurSaturday() == null ? false : request.getRecurSaturday();

		ValidationException validationException = new ValidationException();

		if (logicalAvailabilityId == null)
			validationException.add(new FieldError("logicalAvailabilityId", getStrings().get("Logical Availability ID is required.")));

		if (providerId == null) {
			validationException.add(new FieldError("providerId", getStrings().get("Provider ID is required.")));
		} else {
			Provider provider = getProviderService().findProviderById(providerId).orElse(null);

			if (provider == null)
				validationException.add(new FieldError("providerId", getStrings().get("Provider ID is invalid.")));
		}

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (startDateTime == null)
			validationException.add(new FieldError("startDateTime", getStrings().get("Start date/time is required.")));

		if (endDateTime == null)
			validationException.add(new FieldError("endDateTime", getStrings().get("End date/time is required.")));

		List<AppointmentType> appointmentTypes = appointmentTypeIds.stream()
				.filter(appointmentTypeId -> appointmentTypeId != null)
				.distinct()
				.map(appointmentTypeId -> getAppointmentService().findAppointmentTypeById(appointmentTypeId).get())
				.collect(Collectors.toList());

		if (startDateTime != null && endDateTime != null && !endDateTime.isAfter(startDateTime))
			validationException.add(getStrings().get("End time must be after start time."));

		if (logicalAvailabilityTypeId == null)
			validationException.add(new FieldError("logicalAvailabilityTypeId", getStrings().get("Availability type is required.")));

		if (recurrenceTypeId == null) {
			validationException.add(new FieldError("recurrenceTypeId", getStrings().get("Recurrence type is required.")));
		} else {
			if (recurrenceTypeId == RecurrenceTypeId.NONE) {
				recurSunday = false;
				recurMonday = false;
				recurTuesday = false;
				recurWednesday = false;
				recurThursday = false;
				recurFriday = false;
				recurSaturday = false;
			} else if (recurrenceTypeId == RecurrenceTypeId.DAILY) {
				if (!recurSunday
						&& !recurMonday
						&& !recurTuesday
						&& !recurWednesday
						&& !recurThursday
						&& !recurFriday
						&& !recurSaturday)
					validationException.add(new FieldError("recurrenceTypeId", getStrings().get("You must specify at least one recurrence day.")));
			} else {
				validationException.add(new FieldError("recurrenceTypeId", getStrings().get("Unsupported recurrence type was specified.")));
			}
		}

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("UPDATE logical_availability SET provider_id=?, start_date_time=?, " +
						"end_date_time=?, logical_availability_type_id=?, recurrence_type_id=?, recur_sunday=?, recur_monday=?, recur_tuesday=?, " +
						"recur_wednesday=?, recur_thursday=?, recur_friday=?, recur_saturday=?, last_updated_by_account_id=? WHERE logical_availability_id=?",
				providerId, startDateTime, endDateTime, logicalAvailabilityTypeId, recurrenceTypeId,
				recurSunday, recurMonday, recurTuesday, recurWednesday, recurThursday, recurFriday, recurSaturday, accountId, logicalAvailabilityId);

		getDatabase().execute("DELETE FROM logical_availability_appointment_type WHERE logical_availability_id=?", logicalAvailabilityId);

		// Note: if no appointment types, any active appointment type for the provider is bookable
		for (AppointmentType appointmentType : appointmentTypes)
			getDatabase().execute("INSERT INTO logical_availability_appointment_type(logical_availability_id, appointment_type_id) VALUES (?,?)",
					logicalAvailabilityId, appointmentType.getAppointmentTypeId());

		return true;
	}

	@Nonnull
	public List<ProviderAvailability> nativeSchedulingProviderAvailabilitiesByProviderId(@Nullable UUID providerId,
																																											 @Nullable Set<VisitTypeId> visitTypeIds,
																																											 @Nullable LocalDateTime startDateTime,
																																											 @Nullable LocalDateTime endDateTime) {
		if (providerId == null || startDateTime == null || endDateTime == null)
			return Collections.emptyList();

		Map<UUID, List<ProviderAvailability>> providerAvailabilitiesByProviderId = nativeSchedulingProviderAvailabilitiesByProviderId(Set.of(providerId), visitTypeIds, startDateTime, endDateTime);
		List<ProviderAvailability> providerAvailabilities = providerAvailabilitiesByProviderId.get(providerId);
		return providerAvailabilities == null ? Collections.emptyList() : providerAvailabilities;
	}

	@Nonnull
	public Map<UUID, List<ProviderAvailability>> nativeSchedulingProviderAvailabilitiesByProviderId(@Nullable Set<UUID> providerIds,
																																																	@Nullable Set<VisitTypeId> visitTypeIds,
																																																	@Nullable LocalDateTime startDateTime,
																																																	@Nullable LocalDateTime endDateTime) {
		if (providerIds == null || startDateTime == null || endDateTime == null)
			return Collections.emptyMap();

		if (startDateTime.isEqual(endDateTime) || startDateTime.isAfter(endDateTime))
			return Collections.emptyMap();

		providerIds = providerIds.stream()
				.filter(providerId -> providerId != null)
				.collect(Collectors.toSet());

		if (providerIds.size() == 0)
			return Collections.emptyMap();

		if (visitTypeIds == null)
			visitTypeIds = Collections.emptySet();
		else
			visitTypeIds = visitTypeIds.stream().filter(visitTypeId -> visitTypeId != null).collect(Collectors.toSet());

		Instant now = Instant.now();

		// e.g. "(?),(?),(?)" for Postgres' VALUES clause (faster than IN list)
		String providerIdValuesSql = providerIds.stream().map(providerId -> "(?)").collect(Collectors.joining(","));

		// Pull in all logical availabilities and group by provider ID.  Outer join so we include even those w/o explicit appointment type restriction
		// TODO: filter logical availabilities by date range, taking recurrence into account
		String logicalAvailabilitiesSql = format("SELECT la.* FROM logical_availability la " +
				"LEFT OUTER JOIN logical_availability_appointment_type laat ON la.logical_availability_id=laat.logical_availability_id " +
				"LEFT JOIN v_appointment_type apt ON apt.appointment_type_id=laat.appointment_type_id " +
				"LEFT JOIN provider p ON la.provider_id=p.provider_id " +
				"WHERE p.active=TRUE AND p.provider_id IN (VALUES %s)", providerIdValuesSql);

		List<Object> logicalAvailabilityParameters = new ArrayList<>(providerIds.size() + providerIds.size());
		logicalAvailabilityParameters.addAll(providerIds);

		if (visitTypeIds.size() > 0) {
			logicalAvailabilityParameters.addAll(visitTypeIds);
			// e.g. "(?),(?),(?)" for Postgres' VALUES clause (faster than IN list)
			String visitTypeIdValuesSql = visitTypeIds.stream().map(visitTypeId -> "(?)").collect(Collectors.joining(","));
			logicalAvailabilitiesSql = format("%s AND apt.visit_type_id IN (VALUES %s)", logicalAvailabilitiesSql, visitTypeIdValuesSql);
		}

		List<LogicalAvailability> allLogicalAvailabilities = getDatabase().queryForList(logicalAvailabilitiesSql,
				LogicalAvailability.class, logicalAvailabilityParameters.toArray());

		Map<UUID, List<LogicalAvailability>> logicalAvailabilitiesByProviderId = allLogicalAvailabilities.stream()
				.collect(Collectors.groupingBy(LogicalAvailability::getProviderId));

		// Pull in all appointment types and group by logical availability ID
		// TODO: filter logical availabilities by date range, taking recurrence into account
		// TODO: perhaps better to express this as one big query (combine w/above) instead of splitting into 2 similar queries like this
		String appointmentTypesSql = format("SELECT apt.*, la.logical_availability_id FROM v_appointment_type apt, logical_availability la, logical_availability_appointment_type laat, provider p " +
				"WHERE laat.appointment_type_id=apt.appointment_type_id AND laat.logical_availability_id=la.logical_availability_id " +
				"AND la.provider_id=p.provider_id AND p.active=TRUE AND p.provider_id IN (VALUES %s)", providerIdValuesSql);

		List<Object> appointmentTypeParameters = new ArrayList<>();
		appointmentTypeParameters.addAll(providerIds);

		if (visitTypeIds.size() > 0) {
			appointmentTypeParameters.addAll(visitTypeIds);
			// e.g. "(?),(?),(?)" for Postgres' VALUES clause (faster than IN list)
			String visitTypeIdValuesSql = visitTypeIds.stream().map(visitTypeId -> "(?)").collect(Collectors.joining(","));
			appointmentTypesSql = format("%s AND apt.visit_type_id IN (VALUES %s)", appointmentTypesSql, visitTypeIdValuesSql);
		}

		List<AppointmentTypeWithLogicalAvailabilityId> allAppointmentTypes = getDatabase().queryForList(appointmentTypesSql,
				AppointmentTypeWithLogicalAvailabilityId.class, appointmentTypeParameters.toArray());

		Map<UUID, List<AppointmentTypeWithLogicalAvailabilityId>> appointmentTypesByLogicalAvailabilityId = allAppointmentTypes.stream()
				.collect(Collectors.groupingBy(AppointmentTypeWithLogicalAvailabilityId::getLogicalAvailabilityId));

		String allActiveAppointmentTypesSql = format("SELECT apt.*, p.provider_id FROM appointment_type apt, provider_appointment_type pat, provider p " +
				"WHERE pat.appointment_type_id=apt.appointment_type_id " +
				"AND pat.provider_id=p.provider_id AND p.active=TRUE AND p.provider_id IN (VALUES %s)", providerIdValuesSql);

		List<Object> allActiveAppointmentTypesParameters = new ArrayList<>();
		allActiveAppointmentTypesParameters.addAll(providerIds);

		if (visitTypeIds.size() > 0) {
			allActiveAppointmentTypesParameters.addAll(visitTypeIds);
			// e.g. "(?),(?),(?)" for Postgres' VALUES clause (faster than IN list)
			String visitTypeIdValuesSql = visitTypeIds.stream().map(visitTypeId -> "(?)").collect(Collectors.joining(","));
			allActiveAppointmentTypesSql = format("%s AND apt.visit_type_id IN (VALUES %s)", allActiveAppointmentTypesSql, visitTypeIdValuesSql);
		}

		List<AppointmentTypeWithProviderId> allActiveAppointmentTypes = getDatabase().queryForList(allActiveAppointmentTypesSql,
				AppointmentTypeWithProviderId.class, allActiveAppointmentTypesParameters.toArray());

		Map<UUID, List<AppointmentTypeWithProviderId>> allActiveAppointmentTypesByProviderId = allActiveAppointmentTypes.stream()
				.collect(Collectors.groupingBy(AppointmentTypeWithProviderId::getProviderId));

		// Walk providers and create synthetic provider availability records
		Map<UUID, List<ProviderAvailability>> providerAvailabilitiesByProviderId = new HashMap<>(providerIds.size());

		for (UUID providerId : providerIds) {
			List<LogicalAvailability> logicalAvailabilities = logicalAvailabilitiesByProviderId.get(providerId);

			if (logicalAvailabilities == null)
				logicalAvailabilities = Collections.emptyList();

			List<ProviderAvailability> providerAvailabilities = providerAvailabilitiesByProviderId.get(providerId);

			if (providerAvailabilities == null) {
				providerAvailabilities = new ArrayList<>();
				providerAvailabilitiesByProviderId.put(providerId, providerAvailabilities);
			}

			for (LogicalAvailability logicalAvailability : logicalAvailabilities) {
				List<? extends AppointmentType> appointmentTypes = appointmentTypesByLogicalAvailabilityId.get(logicalAvailability.getLogicalAvailabilityId());

				// No appointment types defined for the logical availability?  That means all appointment types are applicable
				if (appointmentTypes == null || appointmentTypes.size() == 0)
					appointmentTypes = allActiveAppointmentTypesByProviderId.get(providerId);

				if (appointmentTypes == null)
					appointmentTypes = Collections.emptyList();

				Map<Long, List<AppointmentType>> appointmentTypesByDuration = appointmentTypes.stream()
						.collect(Collectors.groupingBy(AppointmentType::getDurationInMinutes));

				if (logicalAvailability.getRecurrenceTypeId() == RecurrenceTypeId.NONE) {
					List<ProviderAvailability> pinnedProviderAvailabilities = providerAvailabilities;

					appointmentTypesByDuration.entrySet().forEach((entry) -> {
						Long durationInMinutes = entry.getKey();
						List<AppointmentType> appointmentTypesForSlot = entry.getValue();

						LocalDateTime slotCurrentDateTime = logicalAvailability.getStartDateTime();
						LocalDateTime slotEndDateTime = logicalAvailability.getEndDateTime();

						while (slotCurrentDateTime.isBefore(slotEndDateTime)) {
							for (AppointmentType appointmentType : appointmentTypesForSlot) {
								ProviderAvailability providerAvailability = new ProviderAvailability();
								providerAvailability.setProviderAvailabilityId(UUID.randomUUID());
								providerAvailability.setProviderId(providerId);
								providerAvailability.setAppointmentTypeId(appointmentType.getAppointmentTypeId());
								providerAvailability.setDateTime(slotCurrentDateTime);
								providerAvailability.setCreated(now);
								providerAvailability.setLastUpdated(now);

								pinnedProviderAvailabilities.add(providerAvailability);
							}

							slotCurrentDateTime = slotCurrentDateTime.plusMinutes(durationInMinutes);
						}
					});
				} else if (logicalAvailability.getRecurrenceTypeId() == RecurrenceTypeId.DAILY) {
					// TODO: implement recurrence
				} else {
					throw new IllegalStateException(format("Not sure how to handle %s.%s", RecurrenceTypeId.class.getSimpleName(),
							logicalAvailability.getRecurrenceTypeId().name()));
				}
			}
		}

		return providerAvailabilitiesByProviderId;
	}

	@NotThreadSafe
	protected static class AppointmentTypeWithProviderId extends AppointmentType {
		@Nullable
		private UUID providerId;

		@Nullable
		public UUID getProviderId() {
			return providerId;
		}

		public void setProviderId(@Nullable UUID providerId) {
			this.providerId = providerId;
		}
	}

	@NotThreadSafe
	protected static class AppointmentTypeWithLogicalAvailabilityId extends AppointmentType {
		@Nullable
		private UUID logicalAvailabilityId;

		@Nullable
		public UUID getLogicalAvailabilityId() {
			return logicalAvailabilityId;
		}

		public void setLogicalAvailabilityId(@Nullable UUID logicalAvailabilityId) {
			this.logicalAvailabilityId = logicalAvailabilityId;
		}
	}

	@Nonnull
	public List<AppointmentType> findAppointmentTypesByLogicalAvailabilityId(@Nullable UUID logicalAvailabilityId) {
		if (logicalAvailabilityId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT apt.* FROM v_appointment_type apt, logical_availability_appointment_type laat "
				+ "WHERE laat.logical_availability_id=? AND apt.appointment_type_id=laat.appointment_type_id", AppointmentType.class, logicalAvailabilityId);
	}

	@Nonnull
	public Boolean deleteLogicalAvailability(@Nullable UUID logicalAvailabilityId) {
		if (logicalAvailabilityId == null)
			return false;

		boolean deleted = false;

		deleted = deleted || getDatabase().execute("DELETE FROM logical_availability_appointment_type WHERE logical_availability_id=?", logicalAvailabilityId) > 0;
		deleted = deleted || getDatabase().execute("DELETE FROM logical_availability WHERE logical_availability_id=?", logicalAvailabilityId) > 0;

		return deleted;
	}

	@Nonnull
	public List<LogicalAvailability> findLogicalAvailabilities(@Nullable UUID providerId,
																														 @Nullable LogicalAvailabilityTypeId logicalAvailabilityTypeId,
																														 @Nullable LocalDate startDate,
																														 @Nullable LocalDate endDate) {
		if (providerId == null)
			return Collections.emptyList();

		StringBuilder sql = new StringBuilder("SELECT * FROM logical_availability WHERE 1=1 ");
		List<Object> parameters = new ArrayList<>();

		sql.append("AND provider_id=? ");
		parameters.add(providerId);

		if (startDate != null) {
			sql.append("AND start_date_time >= ? ");
			parameters.add(startDate.atStartOfDay());
		}

		if (endDate != null) {
			sql.append("AND end_date_time <= ? ");
			parameters.add(endDate.atTime(LocalTime.MAX));
		}

		if (logicalAvailabilityTypeId != null) {
			sql.append("AND logical_availability_type_id = ? ");
			parameters.add(logicalAvailabilityTypeId);
		}

		sql.append("ORDER BY start_date_time, logical_availability_id");

		return getDatabase().queryForList(sql.toString(), LogicalAvailability.class, sqlVaragsParameters(parameters));
	}

	@Nonnull
	public Optional<CalendarPermissionId> findCalendarPermissionByAccountId(@Nullable UUID providerId,
																																					@Nullable UUID grantedToAccountId) {
		if (providerId == null || grantedToAccountId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT calendar_permission_id " +
						"FROM account_calendar_permission WHERE provider_id=? AND granted_to_account_id=?",
				CalendarPermissionId.class, providerId, grantedToAccountId);
	}

	public boolean canTakeActionOnCalendars(@Nullable Account account,
																					@Nullable InstitutionId institutionId) {
		if (account == null || institutionId == null)
			return false;

		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR)
			return true;

		if (account.getRoleId() == RoleId.ADMINISTRATOR && account.getInstitutionId() == institutionId)
			return true;

		// If you are a provider with COBALT scheduling type, you can take action.
		// Or if you have permission to do something to anyone else's calendar, you can take action
		return getDatabase().queryForObject("SELECT (" +
				"EXISTS(SELECT 1 FROM provider WHERE provider_id=? AND scheduling_system_id=?) " +
				"OR EXISTS(SELECT 1 FROM account_calendar_permission WHERE granted_to_account_id=?) " +
				")", Boolean.class, account.getProviderId(), SchedulingSystemId.COBALT, account.getAccountId()).get();
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return appointmentServiceProvider.get();
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return providerServiceProvider.get();
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
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
