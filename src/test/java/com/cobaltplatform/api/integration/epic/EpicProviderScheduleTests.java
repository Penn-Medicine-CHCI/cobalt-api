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

package com.cobaltplatform.api.integration.epic;

import com.cobaltplatform.api.integration.epic.EpicProviderScheduleTests.EpicProviderScheduleConfig.TestProvider;
import com.cobaltplatform.api.integration.epic.EpicProviderScheduleTests.EpicProviderScheduleConfig.TestProvider.TestDepartment;
import com.cobaltplatform.api.integration.epic.EpicProviderScheduleTests.EpicProviderScheduleConfig.TestProvider.TestVisitType;
import com.cobaltplatform.api.integration.epic.EpicSyncManager.ProviderAvailabilityDateInsert;
import com.cobaltplatform.api.integration.epic.request.GetProviderScheduleRequest;
import com.cobaltplatform.api.integration.epic.response.GetProviderScheduleResponse;
import com.cobaltplatform.api.integration.epic.response.GetProviderScheduleResponse.ScheduleSlot;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.EpicAppointmentFilter.EpicAppointmentFilterId;
import com.cobaltplatform.api.model.db.EpicDepartment;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.util.GsonUtility;
import com.google.common.collect.Range;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pyranid.Database;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.soklet.util.LoggingUtils.initializeLogback;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class EpicProviderScheduleTests {
	@BeforeClass
	public static void initialize() {
		initializeLogback(Paths.get("config/local/logback.xml"));
	}

	@Test
	public void testScheduleSlots() throws Exception {
		EpicProviderScheduleConfig epicProviderScheduleConfig = epicProviderScheduleConfigFromFile(Path.of("resources/test/epic-provider-schedule-config.json"));
		EpicEmpCredentials epicEmpCredentials = epicEmpCredentialsFromConfig((epicProviderScheduleConfig));
		Logger logger = LoggerFactory.getLogger(getClass());

		EpicConfiguration epicConfiguration = new EpicConfiguration.Builder(
				epicEmpCredentials,
				epicEmpCredentials.getClientId(),
				epicProviderScheduleConfig.getBaseUrl()
		).build();

		EpicClient epicClient = new DefaultEpicClient(epicConfiguration);

		List<ScheduleCsvRow> scheduleCsvRows = new ArrayList<>();
		LocalDate date = epicProviderScheduleConfig.getStartDate();
		LocalDate endDate = epicProviderScheduleConfig.getEndDate();

		if (date.isAfter(endDate))
			throw new IllegalStateException();

		while (!date.isAfter(endDate)) {
			for (TestProvider testProvider : epicProviderScheduleConfig.getTestProviders()) {
				if (testProvider.getIgnored() != null && testProvider.getIgnored())
					continue;

				logger.info("Processing {} ({}) on {}...", testProvider.getName(), testProvider.getProviderId(), date);

				if (testProvider.getEpicAppointmentFilterId() == EpicAppointmentFilterId.VISIT_TYPE) {
					for (TestVisitType testVisitType : testProvider.getTestVisitTypes()) {
						for (TestDepartment testDepartment : testProvider.getTestDepartments()) {
							GetProviderScheduleRequest request = new GetProviderScheduleRequest();
							request.setDate(date);
							request.setProviderID(testProvider.getProviderId());
							request.setProviderIDType(testProvider.getProviderIdType());
							request.setDepartmentID(testDepartment.getDepartmentId());
							request.setDepartmentIDType(testDepartment.getDepartmentIdType());
							request.setVisitTypeID(testVisitType.getVisitTypeId());
							request.setVisitTypeIDType(testVisitType.getVisitTypeIdType());
							request.setUserID(epicProviderScheduleConfig.getUserId());
							request.setUserIDType(epicProviderScheduleConfig.getUserIdType());

							GetProviderScheduleResponse response = epicClient.performGetProviderSchedule(request);

							for (ScheduleSlot scheduleSlot : response.getScheduleSlots()) {
								ScheduleCsvRow scheduleCsvRow = new ScheduleCsvRow();
								scheduleCsvRow.setProviderId(testProvider.getProviderId());
								scheduleCsvRow.setProviderName(testProvider.getName());
								scheduleCsvRow.setDepartmentId(testDepartment.getDepartmentId());
								scheduleCsvRow.setDepartmentName(testDepartment.getName());
								scheduleCsvRow.setVisitTypeId(testVisitType.getVisitTypeId());
								scheduleCsvRow.setUnavailableDayReason(trimToNull(response.getUnavailableDayReason()));
								scheduleCsvRow.setUnavailableDayComment(trimToNull(response.getUnavailableDayComment()));
								scheduleCsvRow.setDate(trimToNull(response.getDate()));
								scheduleCsvRow.setStartTime(trimToNull(scheduleSlot.getStartTime()));
								scheduleCsvRow.setLength(trimToNull(scheduleSlot.getLength()));
								scheduleCsvRow.setAvailableOpenings(trimToNull(scheduleSlot.getAvailableOpenings()));
								scheduleCsvRow.setOriginalOpenings(trimToNull(scheduleSlot.getOriginalOpenings()));
								scheduleCsvRow.setIsPublic(String.valueOf(scheduleSlot.getPublic()));
								scheduleCsvRow.setUnavailableTimeReason(trimToNull(scheduleSlot.getUnavailableTimeReason()));
								scheduleCsvRow.setUnavailableTimeComment(trimToNull(scheduleSlot.getUnavailableTimeComment()));
								scheduleCsvRow.setHeldTimeReason(trimToNull(scheduleSlot.getHeldTimeReason()));
								scheduleCsvRow.setHeldTimeComment(trimToNull(scheduleSlot.getHeldTimeComment()));
								scheduleCsvRow.setHeldTimeAllDay(String.valueOf(scheduleSlot.getHeldTimeAllDay()));

								scheduleCsvRows.add(scheduleCsvRow);
							}
						}
					}
				} else {
					throw new UnsupportedOperationException(format("We don't yet support %s.%s",
							EpicAppointmentFilterId.class.getSimpleName(), testProvider.getEpicAppointmentFilterId().name()));
				}
			}

			date = date.plusDays(1);
		}

		// Write data out to CSV
		final List<String> CSV_COLUMN_HEADERS = List.of(
				"Provider ID",
				"Provider Name",
				"Department ID",
				"Department Name",
				"Visit Type ID",
				"Date",
				"Start Time",
				"Length",
				"Available Openings",
				"Original Openings",
				"Overbook Openings",
				"Public",
				"Unavailable Time Reason",
				"Unavailable Time Comment",
				"Held Time Reason",
				"Held Time Comment",
				"Held Time All Day",
				"Unavailable Day Reason",
				"Unavailable Day Comment"
		);

		try (Writer writer = new FileWriter(epicProviderScheduleConfig.getDestinationCsvFile(), StandardCharsets.UTF_8);
				 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(CSV_COLUMN_HEADERS.toArray(new String[0])))) {
			for (ScheduleCsvRow scheduleCsvRow : scheduleCsvRows) {
				List<String> recordElements = new ArrayList<>();

				recordElements.add(scheduleCsvRow.getProviderId());
				recordElements.add(scheduleCsvRow.getProviderName());
				recordElements.add(scheduleCsvRow.getDepartmentId());
				recordElements.add(scheduleCsvRow.getDepartmentName());
				recordElements.add(scheduleCsvRow.getVisitTypeId());
				recordElements.add(scheduleCsvRow.getDate());
				recordElements.add(scheduleCsvRow.getStartTime());
				recordElements.add(scheduleCsvRow.getLength());
				recordElements.add(scheduleCsvRow.getAvailableOpenings());
				recordElements.add(scheduleCsvRow.getOriginalOpenings());
				recordElements.add(scheduleCsvRow.getOverbookOpenings());
				recordElements.add(scheduleCsvRow.getIsPublic());
				recordElements.add(scheduleCsvRow.getUnavailableTimeReason());
				recordElements.add(scheduleCsvRow.getUnavailableTimeComment());
				recordElements.add(scheduleCsvRow.getHeldTimeReason());
				recordElements.add(scheduleCsvRow.getHeldTimeComment());
				recordElements.add(scheduleCsvRow.getHeldTimeAllDay());
				recordElements.add(scheduleCsvRow.getUnavailableDayReason());
				recordElements.add(scheduleCsvRow.getUnavailableDayComment());

				csvPrinter.printRecord(recordElements.toArray(new Object[0]));
			}

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		// Optionally dump out what we'd be inserting for provider availability (our slot calculations)
		boolean debugInserts = false;

		if (debugInserts) {
			performOperationWithDatabase((database -> {
				for (TestProvider testProvider : epicProviderScheduleConfig.getTestProviders()) {
					if (testProvider.getIgnored() != null && testProvider.getIgnored())
						continue;

					LocalDate insertDate = epicProviderScheduleConfig.getStartDate();
					LocalDate insertEndDate = epicProviderScheduleConfig.getEndDate();

					logger.info("Performing provider availability insert calculations {} ({}) on {}...", testProvider.getName(), testProvider.getProviderId(), insertDate);

					Provider provider = database.queryForObject("SELECT * FROM provider WHERE epic_provider_id=?", Provider.class, testProvider.getProviderId()).get();

					while (!insertDate.isAfter(insertEndDate)) {
						ProviderAvailabilityDateInsert providerAvailabilityDateInsert = generateProviderAvailabilityDateInsert(
								database,
								logger,
								epicClient,
								epicEmpCredentials,
								provider,
								insertDate
						);

						System.out.println("Availability INSERTs: " + providerAvailabilityDateInsert);

						insertDate = insertDate.plusDays(1);
					}
				}
			}));
		}
	}

	protected void performOperationWithDatabase(@Nonnull Consumer<Database> databaseConsumer) throws IOException {
		requireNonNull(databaseConsumer);

		Path credentialsFile = Path.of("resources/test/epic-schedule-slot-db-credentials.json");
		Map<String, String> credentials = new Gson().fromJson(Files.readString(credentialsFile, StandardCharsets.UTF_8), new TypeToken<Map<String, String>>() {
		}.getType());

		String jdbcUrl = requireNonNull(trimToNull(credentials.get("jdbcUrl")));
		String jdbcUsername = requireNonNull(trimToNull(credentials.get("jdbcUsername")));
		String jdbcPassword = requireNonNull(trimToNull(credentials.get("jdbcPassword")));

		try (HikariDataSource dataSource = new HikariDataSource(new HikariConfig() {
			{
				setJdbcUrl(jdbcUrl);
				setUsername(jdbcUsername);
				setPassword(jdbcPassword);
				setMaximumPoolSize(1);
			}
		})) {
			Database database = Database.forDataSource(dataSource)
					.timeZone(ZoneId.of("UTC"))
					.build();

			databaseConsumer.accept(database);
		}
	}

	// Mirrors functionality in EpicSyncManager so we can test availability calculations per-provider
	@Nonnull
	protected ProviderAvailabilityDateInsert generateProviderAvailabilityDateInsert(@Nonnull Database database,
																																									@Nonnull Logger logger,
																																									@Nonnull EpicClient epicClient,
																																									@Nonnull EpicEmpCredentials epicEmpCredentials,
																																									@Nonnull Provider provider,
																																									@Nonnull LocalDate date) {
		requireNonNull(database);
		requireNonNull(logger);
		requireNonNull(epicClient);
		requireNonNull(epicEmpCredentials);
		requireNonNull(provider);
		requireNonNull(date);

		List<AppointmentType> appointmentTypes = database.queryForList("""
						SELECT app_type.* FROM provider_appointment_type pat, v_appointment_type app_type
						WHERE pat.provider_id=? AND pat.appointment_type_id=app_type.appointment_type_id
						ORDER BY pat.display_order
						""", AppointmentType.class, provider.getProviderId()).stream()
				.filter(appointmentType -> appointmentType.getSchedulingSystemId().equals(SchedulingSystemId.EPIC) && provider.getActive())
				.collect(Collectors.toList());

		List<EpicDepartment> epicDepartments = database.queryForList("""
				SELECT ed.*
				FROM epic_department ed, provider_epic_department ped
				WHERE ped.provider_id=?
				AND ped.epic_department_id=ed.epic_department_id
				ORDER BY ed.name
				""", EpicDepartment.class, provider.getProviderId());

		List<EpicSyncManager.ProviderAvailabilityDateInsertRow> rows = new ArrayList<>();

		if (provider.getEpicAppointmentFilterId().equals(EpicAppointmentFilterId.VISIT_TYPE)) {
			// This path is for providers that want explicit filtering on EPIC visit types (as opposed to us looking at any open slot regardless of visit type)
			for (AppointmentType appointmentType : appointmentTypes) {
				for (EpicDepartment epicDepartment : epicDepartments) {
					GetProviderScheduleRequest request = new GetProviderScheduleRequest();
					request.setDate(date);
					request.setProviderID(provider.getEpicProviderId());
					request.setProviderIDType(provider.getEpicProviderIdType());
					request.setDepartmentID(epicDepartment.getDepartmentId());
					request.setDepartmentIDType(epicDepartment.getDepartmentIdType());
					request.setVisitTypeID(appointmentType.getEpicVisitTypeId());
					request.setVisitTypeIDType(appointmentType.getEpicVisitTypeIdType());
					request.setUserID(epicEmpCredentials.getUserId());
					request.setUserIDType(epicEmpCredentials.getUserIdType());

					GetProviderScheduleResponse response = epicClient.performGetProviderSchedule(request);

					if (response.getScheduleSlots() == null)
						throw new IllegalStateException(format("Unable to detect schedule slots in Epic response for provider %s (%s) " +
										"in department ID %s and visit type ID %s on %s.  Epic response JSON follows:\n%s",
								provider.getName(), provider.getEpicProviderId(), epicDepartment.getDepartmentId(), appointmentType.getEpicVisitTypeId(),
								date, response.getRawJson()));

					List<NormalizedScheduleSlot> availableScheduleSlots = response.getScheduleSlots().stream()
							.map(scheduleSlot -> new NormalizedScheduleSlot(epicClient, date, scheduleSlot))
							.filter(nss -> nss.getPotentiallyUsableForBooking())
							.sorted((nss1, nss2) -> nss1.getDateTime().compareTo(nss2.getDateTime()))
							.collect(Collectors.toUnmodifiableList());

//					for (NormalizedScheduleSlot availableScheduleSlot : availableScheduleSlots)
//						System.out.println("Available slot: " + availableScheduleSlot);

					List<Range<LocalDateTime>> availableScheduleSlotRanges = availableScheduleSlots.stream()
							.map(availableScheduleSlot -> availableScheduleSlot.getRange())
							.collect(Collectors.toUnmodifiableList());

					List<Range<LocalDateTime>> availableScheduleSlotContiguousRanges = mergeContiguousRanges(availableScheduleSlotRanges);

//					for (Range<LocalDateTime> availableScheduleSlotContiguousRange : availableScheduleSlotContiguousRanges)
//						System.out.println("Available contiguous range: " + availableScheduleSlotContiguousRange);

					// Now that we know the contiguous availability ranges, we can see what slots fit in them.
					// We need the contiguous ranges because the slots might be narrower than the visit type duration,
					// e.g. 30 minute slot but 60 minute NPV.
					for (NormalizedScheduleSlot availableScheduleSlot : availableScheduleSlots) {
						LocalDateTime startDateTime = availableScheduleSlot.getDateTime();
						LocalDateTime endDateTime = startDateTime.plusMinutes(appointmentType.getDurationInMinutes());
						Range<LocalDateTime> proposedRange = Range.closedOpen(startDateTime, endDateTime);

						if (proposedRangeFits(proposedRange, availableScheduleSlotContiguousRanges)) {
							logger.debug("Proposed range {} fits within our contiguous ranges: {}", proposedRange, availableScheduleSlotContiguousRanges);

							EpicSyncManager.ProviderAvailabilityDateInsertRow row = new EpicSyncManager.ProviderAvailabilityDateInsertRow();
							row.setAppointmentTypeId(appointmentType.getAppointmentTypeId());
							row.setDateTime(startDateTime);
							row.setEpicDepartmentId(epicDepartment.getEpicDepartmentId());
							rows.add(row);
						} else {
							logger.debug("Proposed range {} does not fit within our contiguous ranges: {}", proposedRange, availableScheduleSlotContiguousRanges);
						}
					}
				}
			}
		} else {
			// We look at appointment type durations to figure out NPVs/RPVs instead of filtering on visit type with EPIC
			Map<Long, List<AppointmentType>> appointmentTypesByDurationInMinutes = new HashMap<>(appointmentTypes.size());

			for (AppointmentType appointmentType : appointmentTypes) {
				List<AppointmentType> currentAppointmentTypes = appointmentTypesByDurationInMinutes.get(appointmentType.getDurationInMinutes());

				if (currentAppointmentTypes == null) {
					currentAppointmentTypes = new ArrayList<>();
					appointmentTypesByDurationInMinutes.put(appointmentType.getDurationInMinutes(), currentAppointmentTypes);
				}

				currentAppointmentTypes.add(appointmentType);
			}

			for (EpicDepartment epicDepartment : epicDepartments) {
				GetProviderScheduleRequest request = new GetProviderScheduleRequest();
				request.setDate(date);
				request.setProviderID(provider.getEpicProviderId());
				request.setProviderIDType(provider.getEpicProviderIdType());
				request.setDepartmentID(epicDepartment.getDepartmentId());
				request.setDepartmentIDType(epicDepartment.getDepartmentIdType());
				request.setUserID(epicEmpCredentials.getUserId());
				request.setUserIDType(epicEmpCredentials.getUserIdType());

				GetProviderScheduleResponse response = epicClient.performGetProviderSchedule(request);

				for (ScheduleSlot scheduleSlot : response.getScheduleSlots()) {
					LocalTime startTime = epicClient.parseTimeAmPm(scheduleSlot.getStartTime());
					Integer availableOpenings = Integer.valueOf(scheduleSlot.getAvailableOpenings());
					Long length = Long.valueOf(scheduleSlot.getLength());
					LocalDateTime dateTime = LocalDateTime.of(date, startTime);
					List<AppointmentType> currentAppointmentTypes = appointmentTypesByDurationInMinutes.get(length);

					if (currentAppointmentTypes == null || currentAppointmentTypes.size() == 0) {
						logger.info("No appointment type found in Cobalt for the {}-minute appointment for {} in department {} on {}.", length, provider.getName(), epicDepartment.getDepartmentId(), dateTime);
					} else if (availableOpenings > 0) {
						boolean held = trimToEmpty(scheduleSlot.getHeldTimeReason()).length() > 0;
						boolean unavailable = trimToEmpty(scheduleSlot.getUnavailableTimeReason()).length() > 0;

						if (!held && !unavailable) {
							for (AppointmentType currentAppointmentType : currentAppointmentTypes) {
								EpicSyncManager.ProviderAvailabilityDateInsertRow row = new EpicSyncManager.ProviderAvailabilityDateInsertRow();
								row.setAppointmentTypeId(currentAppointmentType.getAppointmentTypeId());
								row.setDateTime(dateTime);
								row.setEpicDepartmentId(epicDepartment.getEpicDepartmentId());
								rows.add(row);
							}
						}
					}
				}
			}
		}

		return new ProviderAvailabilityDateInsert(provider.getProviderId(), date, provider.getTimeZone(), rows);
	}

	@Nonnull
	protected EpicProviderScheduleConfig epicProviderScheduleConfigFromFile(@Nonnull Path file) {
		requireNonNull(file);

		if (!Files.exists(file))
			throw new IllegalArgumentException(format("No such file %s", file.toAbsolutePath()));

		GsonBuilder gsonBuilder = new GsonBuilder().disableHtmlEscaping();
		GsonUtility.applyDefaultTypeAdapters(gsonBuilder);
		Gson gson = gsonBuilder.create();

		try {
			String credentialsJson = Files.readString(file, StandardCharsets.UTF_8);
			return gson.fromJson(credentialsJson, EpicProviderScheduleConfig.class);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@ThreadSafe
	private static class NormalizedScheduleSlot {
		@Nonnull
		private final LocalDateTime dateTime;
		@Nonnull
		private final Range<LocalDateTime> range;
		@Nonnull
		private final Integer availableOpenings;
		@Nonnull
		private final Long length;
		@Nonnull
		private final Boolean held;
		@Nonnull
		private final Boolean unavailable;
		@Nonnull
		private final Boolean potentiallyUsableForBooking;
		@Nonnull
		private final ScheduleSlot scheduleSlot;

		public NormalizedScheduleSlot(@Nonnull EpicClient epicClient,
																	@Nonnull LocalDate date,
																	@Nonnull ScheduleSlot scheduleSlot) {
			requireNonNull(epicClient);
			requireNonNull(date);
			requireNonNull(scheduleSlot);

			String availableOpeningsAsString = trimToNull(scheduleSlot.getAvailableOpenings());
			String lengthAsString = trimToNull(scheduleSlot.getLength());

			if (lengthAsString == null)
				throw new IllegalArgumentException(format("%s has invalid length: %s", ScheduleSlot.class, scheduleSlot));

			this.availableOpenings = availableOpeningsAsString == null ? 0 : Integer.valueOf(availableOpeningsAsString);
			this.length = Long.valueOf(lengthAsString);
			this.dateTime = LocalDateTime.of(date, epicClient.parseTimeAmPm(scheduleSlot.getStartTime()));
			this.range = Range.closedOpen(this.dateTime, this.dateTime.plusMinutes(this.length));
			this.held = trimToEmpty(scheduleSlot.getHeldTimeReason()).length() > 0;
			this.unavailable = trimToEmpty(scheduleSlot.getUnavailableTimeReason()).length() > 0;
			this.scheduleSlot = scheduleSlot;
			this.potentiallyUsableForBooking = this.availableOpenings > 0 && !this.held && !this.unavailable;
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
		}

		@Nonnull
		public LocalDateTime getDateTime() {
			return this.dateTime;
		}

		@Nonnull
		public Range<LocalDateTime> getRange() {
			return this.range;
		}

		@Nonnull
		public Integer getAvailableOpenings() {
			return this.availableOpenings;
		}

		@Nonnull
		public Long getLength() {
			return this.length;
		}

		@Nonnull
		public Boolean getHeld() {
			return this.held;
		}

		@Nonnull
		public Boolean getUnavailable() {
			return this.unavailable;
		}

		@Nonnull
		public Boolean getPotentiallyUsableForBooking() {
			return this.potentiallyUsableForBooking;
		}

		@Nonnull
		public ScheduleSlot getScheduleSlot() {
			return this.scheduleSlot;
		}
	}

	@Nonnull
	private List<Range<LocalDateTime>> mergeContiguousRanges(@Nullable List<Range<LocalDateTime>> ranges) {
		if (ranges == null || ranges.isEmpty())
			return Collections.emptyList();

		if (ranges.size() == 1)
			return ranges;

		// Create a copy of the list and sort it by the lower endpoint.
		List<Range<LocalDateTime>> sortedRanges = new ArrayList<>(ranges);
		sortedRanges.sort(Comparator.comparing(Range::lowerEndpoint));

		List<Range<LocalDateTime>> mergedRanges = new ArrayList<>();
		// Start with the first range
		Range<LocalDateTime> currentRange = sortedRanges.get(0);

		for (int i = 1; i < sortedRanges.size(); i++) {
			Range<LocalDateTime> nextRange = sortedRanges.get(i);

			// Check if the two ranges are contiguous.
			// For closedOpen ranges, if the current range’s upper endpoint equals the next range’s lower endpoint,
			// they are contiguous.
			if (currentRange.upperEndpoint().equals(nextRange.lowerEndpoint())) {
				// Merge the two ranges. Since they're contiguous, the new range spans from the
				// current range's lower endpoint to the next range's upper endpoint.
				currentRange = Range.closedOpen(currentRange.lowerEndpoint(), nextRange.upperEndpoint());
			} else {
				// Otherwise, add the currentRange to the result and move on.
				mergedRanges.add(currentRange);
				currentRange = nextRange;
			}
		}

		// Add the final range
		mergedRanges.add(currentRange);

		return mergedRanges;
	}

	@Nonnull
	private Boolean proposedRangeFits(@Nonnull Range<LocalDateTime> proposedRange,
																		@Nonnull List<Range<LocalDateTime>> contiguousRanges) {
		requireNonNull(proposedRange);
		requireNonNull(contiguousRanges);

		for (Range<LocalDateTime> contiguousRange : contiguousRanges)
			if (contiguousRange.encloses(proposedRange))
				// If the contiguousRange encloses proposedRange, it means proposedRange fits.
				return true;

		return false;
	}

	@Nonnull
	protected EpicEmpCredentials epicEmpCredentialsFromConfig(@Nonnull EpicProviderScheduleConfig epicProviderScheduleConfig) {
		requireNonNull(epicProviderScheduleConfig);

		String clientId = epicProviderScheduleConfig.getClientId().toString();
		String userId = epicProviderScheduleConfig.getUserId();
		String userIdType = epicProviderScheduleConfig.getUserIdType();
		String username = epicProviderScheduleConfig.getUsername();
		String password = epicProviderScheduleConfig.getPassword();
		String baseUrl = epicProviderScheduleConfig.getBaseUrl();

		return new EpicEmpCredentials(clientId, userId, userIdType, username, password);
	}

	@NotThreadSafe
	protected static class EpicProviderScheduleConfig {
		@Nullable
		private UUID clientId;
		@Nullable
		private String userId;
		@Nullable
		private String userIdType;
		@Nullable
		private String username;
		@Nullable
		private String password;
		@Nullable
		private String baseUrl;
		@Nullable
		private LocalDate startDate;
		@Nullable
		private LocalDate endDate;
		@Nullable
		private String destinationCsvFile;
		@Nullable
		private List<TestProvider> testProviders;

		@NotThreadSafe
		protected static class TestProvider {
			@Nullable
			private String name;
			@Nullable
			private String providerId;
			@Nullable
			private String providerIdType;
			@Nullable
			private Boolean ignored;
			@Nullable
			private EpicAppointmentFilterId epicAppointmentFilterId;
			@Nullable
			private List<TestVisitType> testVisitTypes;
			@Nullable
			private List<TestDepartment> testDepartments;

			@NotThreadSafe
			protected static class TestVisitType {
				@Nullable
				private String name;
				@Nullable
				private String visitTypeId;
				@Nullable
				private String visitTypeIdType;
				@Nullable
				private Integer durationInMinutes;

				@Nullable
				public String getName() {
					return this.name;
				}

				public void setName(@Nullable String name) {
					this.name = name;
				}

				@Nullable
				public String getVisitTypeId() {
					return this.visitTypeId;
				}

				public void setVisitTypeId(@Nullable String visitTypeId) {
					this.visitTypeId = visitTypeId;
				}

				@Nullable
				public String getVisitTypeIdType() {
					return this.visitTypeIdType;
				}

				public void setVisitTypeIdType(@Nullable String visitTypeIdType) {
					this.visitTypeIdType = visitTypeIdType;
				}

				@Nullable
				public Integer getDurationInMinutes() {
					return this.durationInMinutes;
				}

				public void setDurationInMinutes(@Nullable Integer durationInMinutes) {
					this.durationInMinutes = durationInMinutes;
				}
			}

			@NotThreadSafe
			protected static class TestDepartment {
				@Nullable
				private String name;
				@Nullable
				private String departmentId;
				@Nullable
				private String departmentIdType;

				@Nullable
				public String getName() {
					return this.name;
				}

				public void setName(@Nullable String name) {
					this.name = name;
				}

				@Nullable
				public String getDepartmentId() {
					return this.departmentId;
				}

				public void setDepartmentId(@Nullable String departmentId) {
					this.departmentId = departmentId;
				}

				@Nullable
				public String getDepartmentIdType() {
					return this.departmentIdType;
				}

				public void setDepartmentIdType(@Nullable String departmentIdType) {
					this.departmentIdType = departmentIdType;
				}
			}

			@Nullable
			public String getName() {
				return this.name;
			}

			public void setName(@Nullable String name) {
				this.name = name;
			}

			@Nullable
			public String getProviderId() {
				return this.providerId;
			}

			public void setProviderId(@Nullable String providerId) {
				this.providerId = providerId;
			}

			@Nullable
			public String getProviderIdType() {
				return this.providerIdType;
			}

			public void setProviderIdType(@Nullable String providerIdType) {
				this.providerIdType = providerIdType;
			}

			@Nullable
			public Boolean getIgnored() {
				return this.ignored;
			}

			public void setIgnored(@Nullable Boolean ignored) {
				this.ignored = ignored;
			}

			@Nullable
			public EpicAppointmentFilterId getEpicAppointmentFilterId() {
				return this.epicAppointmentFilterId;
			}

			public void setEpicAppointmentFilterId(@Nullable EpicAppointmentFilterId epicAppointmentFilterId) {
				this.epicAppointmentFilterId = epicAppointmentFilterId;
			}

			@Nullable
			public List<TestVisitType> getTestVisitTypes() {
				return this.testVisitTypes;
			}

			public void setTestVisitTypes(@Nullable List<TestVisitType> testVisitTypes) {
				this.testVisitTypes = testVisitTypes;
			}

			@Nullable
			public List<TestDepartment> getTestDepartments() {
				return this.testDepartments;
			}

			public void setTestDepartments(@Nullable List<TestDepartment> testDepartments) {
				this.testDepartments = testDepartments;
			}
		}

		@Nullable
		public UUID getClientId() {
			return this.clientId;
		}

		public void setClientId(@Nullable UUID clientId) {
			this.clientId = clientId;
		}

		@Nullable
		public String getUserId() {
			return this.userId;
		}

		public void setUserId(@Nullable String userId) {
			this.userId = userId;
		}

		@Nullable
		public String getUserIdType() {
			return this.userIdType;
		}

		public void setUserIdType(@Nullable String userIdType) {
			this.userIdType = userIdType;
		}

		@Nullable
		public String getUsername() {
			return this.username;
		}

		public void setUsername(@Nullable String username) {
			this.username = username;
		}

		@Nullable
		public String getPassword() {
			return this.password;
		}

		public void setPassword(@Nullable String password) {
			this.password = password;
		}

		@Nullable
		public String getBaseUrl() {
			return this.baseUrl;
		}

		public void setBaseUrl(@Nullable String baseUrl) {
			this.baseUrl = baseUrl;
		}

		@Nullable
		public LocalDate getStartDate() {
			return this.startDate;
		}

		public void setStartDate(@Nullable LocalDate startDate) {
			this.startDate = startDate;
		}

		@Nullable
		public LocalDate getEndDate() {
			return this.endDate;
		}

		public void setEndDate(@Nullable LocalDate endDate) {
			this.endDate = endDate;
		}

		@Nullable
		public String getDestinationCsvFile() {
			return this.destinationCsvFile;
		}

		public void setDestinationCsvFile(@Nullable String destinationCsvFile) {
			this.destinationCsvFile = destinationCsvFile;
		}

		@Nullable
		public List<TestProvider> getTestProviders() {
			return this.testProviders;
		}

		public void setTestProviders(@Nullable List<TestProvider> testProviders) {
			this.testProviders = testProviders;
		}
	}

	@ThreadSafe
	private static class EpicTestConfiguration {
		@Nonnull
		private final EpicEmpCredentials epicEmpCredentials;
		@Nonnull
		private final String baseUrl;

		public EpicTestConfiguration(@Nonnull EpicEmpCredentials epicEmpCredentials,
																 @Nonnull String baseUrl) {
			requireNonNull(epicEmpCredentials);
			requireNonNull(baseUrl);

			this.epicEmpCredentials = epicEmpCredentials;
			this.baseUrl = baseUrl;
		}

		@Nonnull
		public EpicEmpCredentials getEpicEmpCredentials() {
			return this.epicEmpCredentials;
		}

		@Nonnull
		public String getBaseUrl() {
			return this.baseUrl;
		}
	}

	@NotThreadSafe
	protected static class ScheduleCsvRow {
		@Nullable
		private String providerId;
		@Nullable
		private String providerName;
		@Nullable
		private String departmentId;
		@Nullable
		private String departmentName;
		@Nullable
		private String visitTypeId;
		@Nullable
		private String unavailableDayReason;
		@Nullable
		private String unavailableDayComment;
		@Nullable
		private String date;
		@Nullable
		private String startTime;
		@Nullable
		private String length;
		@Nullable
		private String availableOpenings;
		@Nullable
		private String originalOpenings;
		@Nullable
		private String overbookOpenings;
		@Nullable
		private String isPublic;
		@Nullable
		private String unavailableTimeReason;
		@Nullable
		private String unavailableTimeComment;
		@Nullable
		private String heldTimeReason;
		@Nullable
		private String heldTimeComment;
		@Nullable
		private String heldTimeAllDay;

		@Nullable
		public String getProviderId() {
			return this.providerId;
		}

		public void setProviderId(@Nullable String providerId) {
			this.providerId = providerId;
		}

		@Nullable
		public String getProviderName() {
			return this.providerName;
		}

		public void setProviderName(@Nullable String providerName) {
			this.providerName = providerName;
		}

		@Nullable
		public String getDepartmentId() {
			return this.departmentId;
		}

		public void setDepartmentId(@Nullable String departmentId) {
			this.departmentId = departmentId;
		}

		@Nullable
		public String getDepartmentName() {
			return this.departmentName;
		}

		public void setDepartmentName(@Nullable String departmentName) {
			this.departmentName = departmentName;
		}

		@Nullable
		public String getVisitTypeId() {
			return this.visitTypeId;
		}

		public void setVisitTypeId(@Nullable String visitTypeId) {
			this.visitTypeId = visitTypeId;
		}

		@Nullable
		public String getUnavailableDayReason() {
			return this.unavailableDayReason;
		}

		public void setUnavailableDayReason(@Nullable String unavailableDayReason) {
			this.unavailableDayReason = unavailableDayReason;
		}

		@Nullable
		public String getUnavailableDayComment() {
			return this.unavailableDayComment;
		}

		public void setUnavailableDayComment(@Nullable String unavailableDayComment) {
			this.unavailableDayComment = unavailableDayComment;
		}

		@Nullable
		public String getDate() {
			return this.date;
		}

		public void setDate(@Nullable String date) {
			this.date = date;
		}

		@Nullable
		public String getStartTime() {
			return this.startTime;
		}

		public void setStartTime(@Nullable String startTime) {
			this.startTime = startTime;
		}

		@Nullable
		public String getLength() {
			return this.length;
		}

		public void setLength(@Nullable String length) {
			this.length = length;
		}

		@Nullable
		public String getAvailableOpenings() {
			return this.availableOpenings;
		}

		public void setAvailableOpenings(@Nullable String availableOpenings) {
			this.availableOpenings = availableOpenings;
		}

		@Nullable
		public String getOriginalOpenings() {
			return this.originalOpenings;
		}

		public void setOriginalOpenings(@Nullable String originalOpenings) {
			this.originalOpenings = originalOpenings;
		}

		@Nullable
		public String getOverbookOpenings() {
			return this.overbookOpenings;
		}

		public void setOverbookOpenings(@Nullable String overbookOpenings) {
			this.overbookOpenings = overbookOpenings;
		}

		@Nullable
		public String getIsPublic() {
			return this.isPublic;
		}

		public void setIsPublic(@Nullable String isPublic) {
			this.isPublic = isPublic;
		}

		@Nullable
		public String getUnavailableTimeReason() {
			return this.unavailableTimeReason;
		}

		public void setUnavailableTimeReason(@Nullable String unavailableTimeReason) {
			this.unavailableTimeReason = unavailableTimeReason;
		}

		@Nullable
		public String getUnavailableTimeComment() {
			return this.unavailableTimeComment;
		}

		public void setUnavailableTimeComment(@Nullable String unavailableTimeComment) {
			this.unavailableTimeComment = unavailableTimeComment;
		}

		@Nullable
		public String getHeldTimeReason() {
			return this.heldTimeReason;
		}

		public void setHeldTimeReason(@Nullable String heldTimeReason) {
			this.heldTimeReason = heldTimeReason;
		}

		@Nullable
		public String getHeldTimeComment() {
			return this.heldTimeComment;
		}

		public void setHeldTimeComment(@Nullable String heldTimeComment) {
			this.heldTimeComment = heldTimeComment;
		}

		@Nullable
		public String getHeldTimeAllDay() {
			return this.heldTimeAllDay;
		}

		public void setHeldTimeAllDay(@Nullable String heldTimeAllDay) {
			this.heldTimeAllDay = heldTimeAllDay;
		}
	}
}
