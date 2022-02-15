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
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.CalendarPermission.CalendarPermissionId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.LogicalAvailability;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.ProviderAvailability;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlVaragsParameters;
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

		if (appointmentTypes.size() == 0)
			validationException.add(new FieldError("appointmentTypeIds", getStrings().get("Appointment types are required.")));

		if (startDateTime != null && endDateTime != null && !endDateTime.isAfter(startDateTime))
			validationException.add(getStrings().get("End time must be after start time."));

		if (validationException.hasErrors())
			throw validationException;

		UUID logicalAvailabilityId = UUID.randomUUID();

		getDatabase().execute("INSERT INTO logical_availability(logical_availability_id, provider_id, start_date_time, " +
						"end_date_time, created_by_account_id, last_updated_by_account_id) VALUES (?,?,?,?,?,?)",
				logicalAvailabilityId, providerId, startDateTime, endDateTime, accountId, accountId);

		for (AppointmentType appointmentType : appointmentTypes)
			getDatabase().execute("INSERT INTO logical_availability_appointment_type(logical_availability_id, appointment_type_id) VALUES (?,?)",
					logicalAvailabilityId, appointmentType.getAppointmentTypeId());

		return logicalAvailabilityId;
	}

	@Nonnull
	public List<ProviderAvailability> findProviderAvailabilities(@Nullable UUID providerId,
																															 @Nullable LocalDateTime startDateTime,
																															 @Nullable LocalDateTime endDateTime) {
		if (providerId == null || startDateTime == null || endDateTime == null)
			return Collections.emptyList();

		Map<UUID, List<ProviderAvailability>> providerAvailabilitiesByProviderId = findProviderAvailabilities(Set.of(providerId), startDateTime, endDateTime);
		List<ProviderAvailability> providerAvailabilities = providerAvailabilitiesByProviderId.get(providerId);
		return providerAvailabilities == null ? Collections.emptyList() : providerAvailabilities;
	}

	@Nonnull
	public Map<UUID, List<ProviderAvailability>> findProviderAvailabilities(@Nullable Set<UUID> providerIds,
																																					@Nullable LocalDateTime startDateTime,
																																					@Nullable LocalDateTime endDateTime) {
		if (providerIds == null || startDateTime == null || endDateTime == null)
			return Collections.emptyMap();

		if (startDateTime.isEqual(endDateTime) || startDateTime.isAfter(endDateTime))
			return Collections.emptyMap();

		// TODO: heavy lifting of filling in synthetic availability slots

		return Collections.emptyMap();
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

		sql.append("ORDER BY start_date_time");

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
