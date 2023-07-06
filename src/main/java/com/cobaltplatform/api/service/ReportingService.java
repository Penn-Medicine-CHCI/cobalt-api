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

import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.ReportType;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class ReportingService {
	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public ReportingService(@Nonnull Provider<AccountService> accountServiceProvider,
													@Nonnull Database database,
													@Nonnull Strings strings) {
		requireNonNull(accountServiceProvider);
		requireNonNull(database);
		requireNonNull(strings);

		this.accountServiceProvider = accountServiceProvider;
		this.database = database;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public List<ReportType> findReportTypesAvailableForAccount(@Nullable UUID accountId) {
		if (accountId == null)
			return List.of();

		Account account = getAccountService().findAccountById(accountId).orElse(null);

		if (account == null)
			return List.of();

		return findReportTypesAvailableForAccount(account);
	}

	@Nonnull
	public List<ReportType> findReportTypesAvailableForAccount(@Nullable Account account) {
		if (account == null)
			return List.of();

		// All reports are available to admins
		if (account.getRoleId() == RoleId.ADMINISTRATOR)
			return getDatabase().queryForList("SELECT * FROM report_type ORDER BY display_order", ReportType.class);

		// For other users, only pick reports to which they are explicitly granted access
		return getDatabase().queryForList("""
				SELECT rt.* 
				FROM report_type rt, account_report_type art
				WHERE art.account_id=?
				AND art.report_type_id=rt.report_type_id
				ORDER BY rt.display_order
				""", ReportType.class, account.getAccountId());
	}

	public void runProviderUnusedAvailabilityReportCsv(@Nonnull InstitutionId institutionId,
																										 @Nonnull LocalDateTime startDateTime,
																										 @Nonnull LocalDateTime endDateTime,
																										 @Nonnull ZoneId reportTimeZone,
																										 @Nonnull Locale reportLocale,
																										 @Nonnull Writer writer) {
		requireNonNull(institutionId);
		requireNonNull(startDateTime);
		requireNonNull(endDateTime);
		requireNonNull(reportTimeZone);
		requireNonNull(reportLocale);
		requireNonNull(writer);

		// Ignoring TZ for now because the slot date-times are stored as "wall clock" times in the database
		// and in practice anyone reporting over them is in the same institution/timezone as the provider
		List<ProviderUnusedAvailabilityReportRecord> records = getDatabase().queryForList("""
				SELECT pah.provider_id, pah.name AS provider_name, pah.slot_date_time
				FROM provider_availability_history pah, provider p
				WHERE pah.provider_id=p.provider_id
				AND p.display_phone_number_only_for_booking != TRUE
				AND p.institution_id = ?
				AND slot_date_time >= ?
				AND slot_date_time <= ?
				ORDER BY pah.name, pah.slot_date_time
				""", ProviderUnusedAvailabilityReportRecord.class, institutionId, startDateTime, endDateTime);

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm").withLocale(reportLocale);

		List<String> headerColumns = List.of(
				getStrings().get("Provider ID"),
				getStrings().get("Provider Name"),
				getStrings().get("Slot Date/Time")
		);

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headerColumns.toArray(new String[0])))) {
			for (ProviderUnusedAvailabilityReportRecord record : records) {
				List<String> recordElements = new ArrayList<>();

				recordElements.add(record.getProviderId().toString());
				recordElements.add(record.getProviderName());
				recordElements.add(dateTimeFormatter.format(record.getSlotDateTime()));

				csvPrinter.printRecord(recordElements.toArray(new Object[0]));
			}

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void runProviderAppointmentsReportCsv(@Nonnull InstitutionId institutionId,
																							 @Nonnull LocalDateTime startDateTime,
																							 @Nonnull LocalDateTime endDateTime,
																							 @Nonnull ZoneId reportTimeZone,
																							 @Nonnull Locale reportLocale,
																							 @Nonnull Writer writer) {
		requireNonNull(institutionId);
		requireNonNull(startDateTime);
		requireNonNull(endDateTime);
		requireNonNull(reportTimeZone);
		requireNonNull(reportLocale);
		requireNonNull(writer);

		// Ignoring TZ for now because the slot date-times are stored as "wall clock" times in the database
		// and in practice anyone reporting over them is in the same institution/timezone as the provider
		List<ProviderAppointmentReportRecord> records = getDatabase().queryForList("""
				SELECT p.provider_id, p.name AS provider_name, app.start_time AS start_date_time, app.created as booked_at,
				a.account_id AS patient_account_id, a.display_name AS patient_name, a.email_address AS patient_email_address,
				a.phone_number AS patient_phone_number
				FROM appointment app, provider p, account a
				WHERE p.provider_id=app.provider_id
				AND app.account_id=a.account_id
				AND p.institution_id=?
				AND app.canceled = FALSE
				AND app.start_time >= ?
				AND app.start_time <= ?  
				ORDER BY p.name, app.start_time
								""", ProviderAppointmentReportRecord.class, institutionId, startDateTime, endDateTime);

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm").withLocale(reportLocale);
		DateTimeFormatter instantFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss")
				.withLocale(reportLocale)
				.withZone(reportTimeZone);

		List<String> headerColumns = List.of(
				getStrings().get("Provider ID"),
				getStrings().get("Provider Name"),
				getStrings().get("Slot Date/Time"),
				getStrings().get("Booked At"),
				getStrings().get("Patient Account ID"),
				getStrings().get("Patient Name"),
				getStrings().get("Patient Email Address"),
				getStrings().get("Patient Phone Number")
		);

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headerColumns.toArray(new String[0])))) {
			for (ProviderAppointmentReportRecord record : records) {
				List<String> recordElements = new ArrayList<>();

				recordElements.add(record.getProviderId().toString());
				recordElements.add(record.getProviderName());
				recordElements.add(dateTimeFormatter.format(record.getStartDateTime()));
				recordElements.add(instantFormatter.format(record.getBookedAt()));
				recordElements.add(record.getPatientAccountId().toString());
				recordElements.add(record.getPatientName());
				recordElements.add(record.getPatientEmailAddress());
				recordElements.add(record.getPatientPhoneNumber());

				csvPrinter.printRecord(recordElements.toArray(new Object[0]));
			}

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void runProviderAppointmentCancelationsReportCsv(@Nonnull InstitutionId institutionId,
																													@Nonnull LocalDateTime startDateTime,
																													@Nonnull LocalDateTime endDateTime,
																													@Nonnull ZoneId reportTimeZone,
																													@Nonnull Locale reportLocale,
																													@Nonnull Writer writer) {
		requireNonNull(institutionId);
		requireNonNull(startDateTime);
		requireNonNull(endDateTime);
		requireNonNull(reportTimeZone);
		requireNonNull(reportLocale);
		requireNonNull(writer);

		// Ignoring TZ for now because the slot date-times are stored as "wall clock" times in the database
		// and in practice anyone reporting over them is in the same institution/timezone as the provider
		List<ProviderAppointmentCancelationReportRecord> records = getDatabase().queryForList("""
				SELECT p.provider_id, p.name AS provider_name, app.start_time AS start_date_time, app.canceled_at,
				a.account_id AS patient_account_id, a.display_name AS patient_name, a.email_address AS patient_email_address,
				a.phone_number AS patient_phone_number
				FROM appointment app, provider p, account a
				WHERE p.provider_id=app.provider_id
				AND app.account_id=a.account_id
				AND p.institution_id=?
				AND app.canceled = TRUE
				AND app.start_time >= ?
				AND app.start_time <= ?  
				ORDER BY p.name, app.start_time
				""", ProviderAppointmentCancelationReportRecord.class, institutionId, startDateTime, endDateTime);

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm").withLocale(reportLocale);
		DateTimeFormatter instantFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss")
				.withLocale(reportLocale)
				.withZone(reportTimeZone);

		List<String> headerColumns = List.of(
				getStrings().get("Provider ID"),
				getStrings().get("Provider Name"),
				getStrings().get("Slot Date/Time"),
				getStrings().get("Canceled At"),
				getStrings().get("Patient Account ID"),
				getStrings().get("Patient Name"),
				getStrings().get("Patient Email Address"),
				getStrings().get("Patient Phone Number")
		);

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headerColumns.toArray(new String[0])))) {
			for (ProviderAppointmentCancelationReportRecord record : records) {
				List<String> recordElements = new ArrayList<>();

				recordElements.add(record.getProviderId().toString());
				recordElements.add(record.getProviderName());
				recordElements.add(dateTimeFormatter.format(record.getStartDateTime()));
				recordElements.add(instantFormatter.format(record.getCanceledAt()));
				recordElements.add(record.getPatientAccountId().toString());
				recordElements.add(record.getPatientName());
				recordElements.add(record.getPatientEmailAddress());
				recordElements.add(record.getPatientPhoneNumber());

				csvPrinter.printRecord(recordElements.toArray(new Object[0]));
			}

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@NotThreadSafe
	protected static class ProviderUnusedAvailabilityReportRecord {
		@Nullable
		private UUID providerId;
		@Nullable
		private String providerName;
		@Nullable
		private LocalDateTime slotDateTime;

		@Nullable
		public UUID getProviderId() {
			return this.providerId;
		}

		public void setProviderId(@Nullable UUID providerId) {
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
		public LocalDateTime getSlotDateTime() {
			return this.slotDateTime;
		}

		public void setSlotDateTime(@Nullable LocalDateTime slotDateTime) {
			this.slotDateTime = slotDateTime;
		}
	}

	@NotThreadSafe
	protected static class ProviderAppointmentReportRecord {
		@Nullable
		private UUID providerId;
		@Nullable
		private String providerName;
		@Nullable
		private LocalDateTime startDateTime;
		@Nullable
		private Instant bookedAt;
		@Nullable
		private UUID patientAccountId;
		@Nullable
		private String patientName;
		@Nullable
		private String patientEmailAddress;
		@Nullable
		private String patientPhoneNumber;

		@Nullable
		public UUID getProviderId() {
			return this.providerId;
		}

		public void setProviderId(@Nullable UUID providerId) {
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
		public LocalDateTime getStartDateTime() {
			return this.startDateTime;
		}

		public void setStartDateTime(@Nullable LocalDateTime startDateTime) {
			this.startDateTime = startDateTime;
		}

		@Nullable
		public Instant getBookedAt() {
			return this.bookedAt;
		}

		public void setBookedAt(@Nullable Instant bookedAt) {
			this.bookedAt = bookedAt;
		}

		@Nullable
		public UUID getPatientAccountId() {
			return this.patientAccountId;
		}

		public void setPatientAccountId(@Nullable UUID patientAccountId) {
			this.patientAccountId = patientAccountId;
		}

		@Nullable
		public String getPatientName() {
			return this.patientName;
		}

		public void setPatientName(@Nullable String patientName) {
			this.patientName = patientName;
		}

		@Nullable
		public String getPatientEmailAddress() {
			return this.patientEmailAddress;
		}

		public void setPatientEmailAddress(@Nullable String patientEmailAddress) {
			this.patientEmailAddress = patientEmailAddress;
		}

		@Nullable
		public String getPatientPhoneNumber() {
			return this.patientPhoneNumber;
		}

		public void setPatientPhoneNumber(@Nullable String patientPhoneNumber) {
			this.patientPhoneNumber = patientPhoneNumber;
		}
	}


	@NotThreadSafe
	protected static class ProviderAppointmentCancelationReportRecord {
		@Nullable
		private UUID providerId;
		@Nullable
		private String providerName;
		@Nullable
		private LocalDateTime startDateTime;
		@Nullable
		private Instant canceledAt;
		@Nullable
		private UUID patientAccountId;
		@Nullable
		private String patientName;
		@Nullable
		private String patientEmailAddress;
		@Nullable
		private String patientPhoneNumber;

		@Nullable
		public UUID getProviderId() {
			return this.providerId;
		}

		public void setProviderId(@Nullable UUID providerId) {
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
		public LocalDateTime getStartDateTime() {
			return this.startDateTime;
		}

		public void setStartDateTime(@Nullable LocalDateTime startDateTime) {
			this.startDateTime = startDateTime;
		}

		@Nullable
		public Instant getCanceledAt() {
			return this.canceledAt;
		}

		public void setCanceledAt(@Nullable Instant canceledAt) {
			this.canceledAt = canceledAt;
		}

		@Nullable
		public UUID getPatientAccountId() {
			return this.patientAccountId;
		}

		public void setPatientAccountId(@Nullable UUID patientAccountId) {
			this.patientAccountId = patientAccountId;
		}

		@Nullable
		public String getPatientName() {
			return this.patientName;
		}

		public void setPatientName(@Nullable String patientName) {
			this.patientName = patientName;
		}

		@Nullable
		public String getPatientEmailAddress() {
			return this.patientEmailAddress;
		}

		public void setPatientEmailAddress(@Nullable String patientEmailAddress) {
			this.patientEmailAddress = patientEmailAddress;
		}

		@Nullable
		public String getPatientPhoneNumber() {
			return this.patientPhoneNumber;
		}

		public void setPatientPhoneNumber(@Nullable String patientPhoneNumber) {
			this.patientPhoneNumber = patientPhoneNumber;
		}
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountServiceProvider.get();
	}

	@Nonnull
	protected Database getDatabase() {
		return this.database;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
