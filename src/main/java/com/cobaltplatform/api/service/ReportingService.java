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
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSource;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.AppointmentTypeAssessment;
import com.cobaltplatform.api.model.db.Clinic;
import com.cobaltplatform.api.model.db.GenderIdentity.GenderIdentityId;
import com.cobaltplatform.api.model.db.GroupSessionReservation;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.PatientOrderConsentStatus;
import com.cobaltplatform.api.model.db.PatientOrderDisposition.PatientOrderDispositionId;
import com.cobaltplatform.api.model.db.PatientOrderResourcingStatus.PatientOrderResourcingStatusId;
import com.cobaltplatform.api.model.db.PatientOrderScreeningStatus.PatientOrderScreeningStatusId;
import com.cobaltplatform.api.model.db.PatientOrderTriageSource.PatientOrderTriageSourceId;
import com.cobaltplatform.api.model.db.PatientOrderTriageStatus.PatientOrderTriageStatusId;
import com.cobaltplatform.api.model.db.Race.RaceId;
import com.cobaltplatform.api.model.db.ReportType;
import com.cobaltplatform.api.model.db.ReportType.ReportTypeId;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.model.service.AccountCapabilityFlags;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
import com.soklet.web.annotation.QueryParameter;
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
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlInListPlaceholders;
import static com.cobaltplatform.api.util.ValidationUtility.isValidEmailAddress;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class ReportingService {
	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final Provider<GroupSessionService> groupSessionServiceProvider;
	@Nonnull
	private final Provider<AuthorizationService> authorizationServiceProvider;
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Configuration configuration;

	@Inject
	public ReportingService(@Nonnull Provider<AccountService> accountServiceProvider,
													@Nonnull Provider<GroupSessionService> groupSessionServiceProvider,
													@Nonnull Provider<AuthorizationService> authorizationServiceProvider,
													@Nonnull DatabaseProvider databaseProvider,
													@Nonnull Strings strings,
													@Nonnull Formatter formatter,
													@Nonnull Configuration configuration) {
		requireNonNull(accountServiceProvider);
		requireNonNull(groupSessionServiceProvider);
		requireNonNull(authorizationServiceProvider);
		requireNonNull(databaseProvider);
		requireNonNull(strings);
		requireNonNull(formatter);
		requireNonNull(configuration);

		this.accountServiceProvider = accountServiceProvider;
		this.groupSessionServiceProvider = groupSessionServiceProvider;
		this.authorizationServiceProvider = authorizationServiceProvider;
		this.databaseProvider = databaseProvider;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
		this.formatter = formatter;
		this.configuration = configuration;
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

		List<ReportType> reportTypes = getDatabase().queryForList("SELECT * FROM report_type ORDER BY display_order", ReportType.class);
		AccountCapabilityFlags accountCapabilityFlags = getAuthorizationService().determineAccountCapabilityFlagsForAccount(account);

		// Examine capabilities to determine available report types
		return reportTypes.stream()
				.filter(reportType -> {
					if (reportType.getReportTypeId() == ReportTypeId.PROVIDER_UNUSED_AVAILABILITY)
						return accountCapabilityFlags.isCanViewProviderReportUnusedAvailability();

					if (reportType.getReportTypeId() == ReportTypeId.PROVIDER_APPOINTMENTS)
						return accountCapabilityFlags.isCanViewProviderReportAppointments();

					if (reportType.getReportTypeId() == ReportTypeId.PROVIDER_APPOINTMENTS_EAP)
						return accountCapabilityFlags.isCanViewProviderReportAppointmentsEap();

					if (reportType.getReportTypeId() == ReportTypeId.PROVIDER_APPOINTMENT_CANCELATIONS)
						return accountCapabilityFlags.isCanViewProviderReportAppointmentCancelations();

					if (reportType.getReportTypeId() == ReportTypeId.IC_PIPELINE
							|| reportType.getReportTypeId() == ReportTypeId.IC_OUTREACH
							|| reportType.getReportTypeId() == ReportTypeId.IC_ASSESSMENT)
						return accountCapabilityFlags.isCanViewIcReports();

					if (reportType.getReportTypeId() == ReportTypeId.GROUP_SESSION_RESERVATION_EMAILS)
						return accountCapabilityFlags.isCanAdministerGroupSessions();

					// TODO: We might re-enable this later
					// throw new UnsupportedOperationException(format("Unexpected %s value '%s'",
					//		ReportTypeId.class.getSimpleName(), reportType.getReportTypeId().name()));

					return false;
				})
				.collect(Collectors.toList());
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


	public void runProviderAppointmentsEapReportCsv(@Nonnull InstitutionId institutionId,
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
		List<ProviderAppointmentEap> appointments = getDatabase().queryForList("""
				SELECT app.*, p.name as provider_name, a.email_address as account_email_address, a.account_source_id,
				a.first_name as account_first_name, a.last_name as account_last_name, a.email_address
				FROM appointment app, provider p, account a, provider_support_role psr
				WHERE p.provider_id=app.provider_id
				AND app.account_id=a.account_id
				AND psr.provider_id=p.provider_id
				AND psr.support_role_id=?
				AND p.institution_id=?
				AND app.canceled = FALSE
				AND app.start_time >= ?
				AND app.start_time <= ?
				ORDER BY p.name, app.start_time
								""", ProviderAppointmentEap.class, SupportRoleId.CARE_MANAGER, institutionId, startDateTime, endDateTime);

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm").withLocale(reportLocale);
		DateTimeFormatter instantFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss")
				.withLocale(reportLocale)
				.withZone(reportTimeZone);

		for (ProviderAppointmentEap appointment : appointments) {
			AppointmentType appointmentType = getDatabase().queryForObject("SELECT * FROM appointment_type WHERE appointment_type_id=?", AppointmentType.class, appointment.getAppointmentTypeId()).get();
			List<AppointmentTypeAssessment> appointmentTypeAssessments = getDatabase().queryForList("""
					SELECT *
					FROM appointment_type_assessment
					WHERE appointment_type_id=?
					""", AppointmentTypeAssessment.class, appointmentType.getAppointmentTypeId());

			Set<UUID> assessmentIdsForAppointmentType = appointmentTypeAssessments.stream()
					.map(ata -> ata.getAssessmentId())
					.collect(Collectors.toSet());

			List<Clinic> clinics = getDatabase().queryForList("""
								select c.* from clinic c, provider_clinic pc 
								where pc.provider_id=?
								and pc.clinic_id=c.clinic_id
					""", Clinic.class, appointment.getProviderId());

			Set<UUID> clinicIntakeAssessmentIds = clinics.stream()
					.map(clinic -> clinic.getIntakeAssessmentId())
					.filter(id -> id != null)
					.collect(Collectors.toSet());

			if (appointment.getIntakeAccountSessionId() != null) {
				if (!assessmentIdsForAppointmentType.contains(appointment.getIntakeAssessmentId())) {
					if (assessmentIdsForAppointmentType.size() == 0) {
						appointment.setNote(format("Clinic Intake Assessment ID %s was used (no appointment type assessment available)", appointment.getIntakeAssessmentId()));

						if (!clinicIntakeAssessmentIds.contains(appointment.getIntakeAssessmentId()))
							throw new IllegalStateException("Not sure where intake assessment ID came from...");
					} else {
						appointment.setNote(format("Error: Intake Assessment ID %s not in %s", appointment.getIntakeAssessmentId(), assessmentIdsForAppointmentType));
					}
				}

				List<AssessmentAnswer> assessmentAnswers = getDatabase().queryForList("""
						select q.question_text, asa.answer_text
						from account_session_answer asa, answer a, question q
						where asa.account_session_id = ?
						and asa.answer_id=a.answer_id
						and a.question_id=q.question_id
						and asa.answer_text is not null
						""", AssessmentAnswer.class, appointment.getIntakeAccountSessionId());

				// TODO: this is a temporary hardcode to support legacy intake assessments
				for (AssessmentAnswer assessmentAnswer : assessmentAnswers) {
					if ("What is your first name?".equals(assessmentAnswer.getQuestionText())) {
						appointment.setFirstNameAnswer(assessmentAnswer);
					} else if ("What is your last name?".equals(assessmentAnswer.getQuestionText())) {
						appointment.setLastNameAnswer(assessmentAnswer);
					} else if ("What is your phone number?".equals(assessmentAnswer.getQuestionText())) {
						appointment.setPhoneNumberAnswer(assessmentAnswer);
					} else {
						throw new IllegalStateException("Unexpected question: '" + assessmentAnswer.getQuestionText() + "'");
					}
				}
			}
		}

		// Not including some columns for the moment
		List<String> headerColumns = List.of(
				getStrings().get("Appointment ID"),
				getStrings().get("Provider"),
				getStrings().get("Booking Date/Time"),
				getStrings().get("Appointment Date/Time"),
				//getStrings().get("Account Source"),
				getStrings().get("First Name"),
				getStrings().get("Last Name"),
				getStrings().get("Email"),
				getStrings().get("Phone")
				//getStrings().get("Took Assessment?"),
				//getStrings().get("Assessment ID"),
				//getStrings().get("Account Session ID"),
				//getStrings().get("Note")
		);

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headerColumns.toArray(new String[0])))) {
			SortedSet<UUID> intakeAssessmentIds = new TreeSet<>();

			for (ProviderAppointmentEap appointment : appointments) {
				String provider = appointment.getProviderName();
				String bookingDateTime = instantFormatter.format(appointment.getCreated());
				String accountSourceId = appointment.getAccountSourceId().name();
				String appointmentDateTime = dateTimeFormatter.format(appointment.getStartTime());
				String emailAddress = appointment.getAccountEmailAddress();

				if (emailAddress == null) {
					Account account = getDatabase().queryForObject("SELECT * FROM account WHERE account_id=?", Account.class, appointment.getAccountId()).get();

					// If the SSO ID appears to be a valid email address, use it
					if (account.getSsoId() != null && isValidEmailAddress(account.getSsoId()))
						emailAddress = account.getSsoId();
				}

				String firstName = appointment.getFirstNameAnswer() != null ? appointment.getFirstNameAnswer().getAnswerText() : null;

				if (firstName == null)
					firstName = appointment.getAccountFirstName();

				String lastName = appointment.getLastNameAnswer() != null ? appointment.getLastNameAnswer().getAnswerText() : null;

				if (lastName == null)
					lastName = appointment.getAccountLastName();

				String phoneNumber = appointment.getPhoneNumberAnswer() != null ? appointment.getPhoneNumberAnswer().getAnswerText() : null;

				if (phoneNumber == null)
					phoneNumber = appointment.getPhoneNumber();

				boolean elidePii = true;

				if (elidePii) {
					if (firstName != null) {
						firstName = firstName.substring(0, 1).toUpperCase(Locale.US);
						firstName = firstName + ".";
					}

					if (lastName != null) {
						lastName = lastName.substring(0, 1).toUpperCase(Locale.US);
						lastName = lastName + ".";
					}

					if (emailAddress != null) {
						String suffix = emailAddress.substring(emailAddress.indexOf("@"));
						String prefix = emailAddress.substring(0, emailAddress.indexOf("@"));
						emailAddress = emailAddress.substring(0, 1) + format("%0" + (prefix.length() - 1) + "d", 0).replace("0", "*") + suffix; // + " --- " + emailAddress;
					}

					if (phoneNumber != null) {
						if (phoneNumber.startsWith("1"))
							phoneNumber = phoneNumber.substring(1);

						String prefix = phoneNumber.substring(0, 3);
						String suffix = phoneNumber.substring(phoneNumber.length() - 4, phoneNumber.length());
						phoneNumber = prefix + "***" + suffix;
					}
				}

				String tookAssessment = appointment.getIntakeAccountSessionId() != null ? "YES" : "NO";

				if (appointment.getIntakeAssessmentId() != null)
					intakeAssessmentIds.add(appointment.getIntakeAssessmentId());

				List<String> recordElements = new ArrayList<>();

				// Not including some columns for the moment
				recordElements.add(appointment.getAppointmentId().toString());
				recordElements.add(provider);
				recordElements.add(bookingDateTime);
				recordElements.add(appointmentDateTime);
				//recordElements.add(accountSourceId);
				recordElements.add(firstName);
				recordElements.add(lastName);
				recordElements.add(emailAddress);
				recordElements.add(phoneNumber);
				//recordElements.add(tookAssessment);
				//recordElements.add(appointment.getIntakeAssessmentId() == null ? null : appointment.getIntakeAssessmentId().toString());
				//recordElements.add(appointment.getIntakeAccountSessionId() == null ? null : appointment.getIntakeAccountSessionId().toString());
				//recordElements.add(appointment.getNote());

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

	private IcWhereClauseWithParameters buildIcWhereClauseWithParameters(@Nonnull InstitutionId institutionId,
																																			 @Nonnull LocalDateTime startDateTime,
																																			 @Nonnull LocalDateTime endDateTime,
																																			 @Nonnull Optional<List<String>> payorName,
																																			 @Nonnull Optional<List<String>> practiceName,
																																			 @Nonnull Optional<Integer> patientAgeFrom,
																																			 @Nonnull Optional<Integer> patientAgeTo,
																																			 @Nonnull Optional<List<RaceId>> raceId,
																																			 @Nonnull Optional<List<GenderIdentityId>> genderIdentityId,
																																			 @Nonnull Optional<List<UUID>> panelAccountId) {
		requireNonNull(startDateTime);
		requireNonNull(endDateTime);
		requireNonNull(payorName);
		requireNonNull(practiceName);
		requireNonNull(patientAgeFrom);
		requireNonNull(patientAgeTo);
		requireNonNull(raceId);
		requireNonNull(genderIdentityId);
		requireNonNull(panelAccountId);

		IcWhereClauseWithParameters whereClauseWithParameters = new IcWhereClauseWithParameters();
		List<Object> parameters = new ArrayList();

		StringBuilder whereClause = new StringBuilder(""" 
				WHERE institution_id = ? 
				AND order_date >= ? 
				AND order_date <= ? """);

		parameters.add(institutionId);
		parameters.add(startDateTime);
		parameters.add(endDateTime);

		if (!getConfiguration().getShouldIncludeTestDataInIcReports())
			whereClause.append(" AND test_patient_order = false ");

		if (payorName.isPresent()) {
			whereClause.append(format(" AND primary_payor_name IN %s ", sqlInListPlaceholders(payorName.get())));
			parameters.addAll(payorName.get());
		}

		if (practiceName.isPresent()) {
			whereClause.append(format(" AND referring_practice_name IN %s ", sqlInListPlaceholders(practiceName.get())));
			parameters.addAll(practiceName.get());
		}

		if (patientAgeFrom.isPresent() && patientAgeTo.isPresent()) {
			whereClause.append(" AND (date_part('year', order_date) - date_part('year', patient_birthdate)::INT) BETWEEN ? AND ? ");
			parameters.add(patientAgeFrom.get());
			parameters.add(patientAgeTo.get());
		}

		if (raceId.isPresent()) {
			whereClause.append(format(" AND patient_race_id IN  %s ", sqlInListPlaceholders(raceId.get())));
			parameters.addAll(raceId.get());
		}

		if (genderIdentityId.isPresent()) {
			whereClause.append(format(" AND patient_gender_identity_id IN %s ", sqlInListPlaceholders(genderIdentityId.get())));
			parameters.addAll(genderIdentityId.get());
		}

		if (panelAccountId.isPresent()) {
			whereClause.append(format(" AND panel_account_id IN %s ", sqlInListPlaceholders(panelAccountId.get())));
			parameters.addAll(panelAccountId.get());
		}

		whereClauseWithParameters.setWhereClause(whereClause.toString());
		whereClauseWithParameters.setParameters(parameters);

		return whereClauseWithParameters;
	}

	private void addFilterDescription(@Nonnull CSVPrinter csVPrinter,
																		@Nonnull Optional<List<String>> payorName,
																		@Nonnull Optional<List<String>> practiceName,
																		@Nonnull Optional<Integer> patientAgeFrom,
																		@Nonnull Optional<Integer> patientAgeTo,
																		@Nonnull Optional<List<RaceId>> raceId,
																		@Nonnull Optional<List<GenderIdentityId>> genderIdentityId,
																		@Nonnull Optional<List<UUID>> panelAccountId) {

		Boolean fileterDescriptionAdded = false;

		try {
			csVPrinter.printRecord("Additional Filters Applied");
			if (payorName.isPresent()) {
				csVPrinter.printRecord("Payor Names", payorName.get().toString());
				fileterDescriptionAdded = true;
			}

			if (practiceName.isPresent()) {
				csVPrinter.printRecord("Practice Names", practiceName.get().toString());
				fileterDescriptionAdded = true;
			}

			if (patientAgeFrom.isPresent()) {
				csVPrinter.printRecord("Patient Age", format("%s - %s years old", patientAgeFrom.get(), patientAgeTo.get()));
				fileterDescriptionAdded = true;
			}

			if (raceId.isPresent()) {
				csVPrinter.printRecord("Races", raceId.get().toString());
				fileterDescriptionAdded = true;
			}

			if (genderIdentityId.isPresent()) {
				csVPrinter.printRecord("Gender Identity", genderIdentityId.get().toString());
				fileterDescriptionAdded = true;
			}

			if (panelAccountId.isPresent()) {
				List<Account> accounts = getDatabase().queryForList(format("SELECT * FROM account WHERE account_id in %s",
						sqlInListPlaceholders(panelAccountId.get())), Account.class, panelAccountId.get().toArray());
				String mhicNames = accounts.stream().map(it -> it.getDisplayName()).collect(Collectors.joining(","));
				csVPrinter.printRecord("MHIC(s)", mhicNames);
				fileterDescriptionAdded = true;
			}

			if (!fileterDescriptionAdded)
				csVPrinter.printRecord("None");

			csVPrinter.println();

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public List<DescriptionWithCountRecord> findTriageReasonsPatientOrders(@Nonnull IcWhereClauseWithParameters whereClauseWithParameters,
																																				 @Nonnull PatientOrderTriageSourceId patientOrderTriageSourceId) {
		requireNonNull(whereClauseWithParameters);
		requireNonNull(patientOrderTriageSourceId);

		List<DescriptionWithCountRecord> assessmentOverridePatientOrders = getDatabase().queryForList(
				format("""
						WITH pot_reason AS
						(SELECT DISTINCT potg.patient_order_id, potor.description as reason
						FROM patient_order_triage_group potg, v_patient_order vpo, patient_order_triage_override_reason potor
						%s
						AND potg.patient_order_id = vpo.patient_order_id
						AND potg.patient_order_triage_source_id='%s'
						AND potg.patient_order_triage_override_reason_id=potor.patient_order_triage_override_reason_id
						AND potg.active = true)
						SELECT reason as description, count(*) as count
						FROM pot_reason
						GROUP BY reason
						ORDER BY reason
						""", whereClauseWithParameters.getWhereClause(), patientOrderTriageSourceId.name()),
				DescriptionWithCountRecord.class, whereClauseWithParameters.getParameters().toArray());

		return assessmentOverridePatientOrders;
	}

	public void runIcPipelineReportCsv(@Nonnull InstitutionId institutionId,
																		 @Nonnull LocalDateTime startDateTime,
																		 @Nonnull LocalDateTime endDateTime,
																		 @Nonnull Optional<List<String>> payorName,
																		 @Nonnull Optional<List<String>> practiceName,
																		 @Nonnull Optional<Integer> patientAgeFrom,
																		 @Nonnull Optional<Integer> patientAgeTo,
																		 @Nonnull Optional<List<RaceId>> raceId,
																		 @Nonnull Optional<List<GenderIdentityId>> genderIdentityId,
																		 @Nonnull ZoneId reportTimeZone,
																		 @Nonnull Locale reportLocale,
																		 @Nonnull Writer writer) {
		requireNonNull(institutionId);
		requireNonNull(startDateTime);
		requireNonNull(endDateTime);
		requireNonNull(payorName);
		requireNonNull(practiceName);
		requireNonNull(patientAgeFrom);
		requireNonNull(patientAgeTo);
		requireNonNull(raceId);
		requireNonNull(genderIdentityId);
		requireNonNull(reportTimeZone);
		requireNonNull(reportLocale);
		requireNonNull(writer);

		IcWhereClauseWithParameters whereClauseWithParameters = buildIcWhereClauseWithParameters(institutionId, startDateTime, endDateTime, payorName, practiceName, patientAgeFrom,
				patientAgeTo, raceId, genderIdentityId, Optional.empty());

		//All patient orders matching the report filters
		List<PatientOrder> patientOrders = getDatabase().queryForList(
				format("SELECT * FROM v_patient_order %s", whereClauseWithParameters.getWhereClause()), PatientOrder.class, whereClauseWithParameters.getParameters().toArray());

		//List of referral reasons
		StringBuilder reasonForReferralQuery = new StringBuilder(format("""
				SELECT reason_for_referral as description, count(*) as count
				FROM v_patient_order
				%s
				GROUP BY reason_for_referral
				ORDER BY reason_for_referral""", whereClauseWithParameters.getWhereClause()));
		List<DescriptionWithCountRecord> referralReasons = getDatabase().queryForList(reasonForReferralQuery.toString(),
				DescriptionWithCountRecord.class, whereClauseWithParameters.getParameters().toArray());

		List<DescriptionWithCountRecord> assessmentOverridePatientOrders = findTriageReasonsPatientOrders(whereClauseWithParameters, PatientOrderTriageSourceId.COBALT);

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
			addFilterDescription(csvPrinter, payorName, practiceName, patientAgeFrom, patientAgeTo, raceId, genderIdentityId, Optional.empty());

			csvPrinter.printRecord("Referral Count", Integer.toString(patientOrders.size()));
			csvPrinter.printRecord("Connection Count", Integer.toString(patientOrders.stream().filter(it -> it.getTotalOutreachCount() > 0).collect(Collectors.toList()).size()));
			csvPrinter.printRecord("Yes to Engage in Services", Integer.toString(patientOrders.stream().filter(it -> it.getPatientOrderConsentStatusId() ==
					PatientOrderConsentStatus.PatientOrderConsentStatusId.CONSENTED).collect(Collectors.toList()).size()));
			csvPrinter.printRecord("No to Engage in Services", Integer.toString(patientOrders.stream().filter(it -> it.getPatientOrderConsentStatusId() ==
					PatientOrderConsentStatus.PatientOrderConsentStatusId.REJECTED).collect(Collectors.toList()).size()));
			Integer inProgressScreeningCount = patientOrders.stream().filter(it -> it.getPatientOrderScreeningStatusId() ==
					PatientOrderScreeningStatusId.IN_PROGRESS).collect(Collectors.toList()).size();
			csvPrinter.printRecord("Started Assessments", Integer.toString(inProgressScreeningCount));
			Integer completedScreeningCount = patientOrders.stream().filter(it -> it.getPatientOrderScreeningStatusId() ==
					PatientOrderScreeningStatusId.COMPLETE).collect(Collectors.toList()).size();
			csvPrinter.printRecord("Completed Assessments", Integer.toString(completedScreeningCount));
			csvPrinter.printRecord("Completed Assessments by Patient", Integer.toString(patientOrders.stream().filter(it -> it.getPatientOrderScreeningStatusId() ==
					PatientOrderScreeningStatusId.COMPLETE && it.getMostRecentScreeningSessionByPatient() == true).collect(Collectors.toList()).size()));
			csvPrinter.printRecord("Completed Assessments by MHIC", Integer.toString(patientOrders.stream().filter(it -> it.getPatientOrderScreeningStatusId() ==
					PatientOrderScreeningStatusId.COMPLETE && it.getMostRecentScreeningSessionByPatient() == false).collect(Collectors.toList()).size()));
			Integer totalStartedScreenings = inProgressScreeningCount + completedScreeningCount;
			csvPrinter.printRecord("Abandonment Rate", getFormatter().formatPercent(totalStartedScreenings > 0 ?
					((double) inProgressScreeningCount / totalStartedScreenings) : 0));
			csvPrinter.printRecord("Completion Rate", getFormatter().formatPercent(totalStartedScreenings > 0 ?
					((double) completedScreeningCount / totalStartedScreenings) : 0));
			csvPrinter.printRecord("Triaged to Subclinical", Integer.toString(patientOrders.stream().filter(it -> it.getPatientOrderTriageStatusId() ==
					PatientOrderTriageStatusId.SUBCLINICAL).collect(Collectors.toList()).size()));
			csvPrinter.printRecord("Triaged to MHP", Integer.toString(patientOrders.stream().filter(it -> it.getPatientOrderTriageStatusId() ==
					PatientOrderTriageStatusId.MHP).collect(Collectors.toList()).size()));
			csvPrinter.printRecord("Triaged to Specialty Care", Integer.toString(patientOrders.stream().filter(it -> it.getPatientOrderTriageStatusId() ==
					PatientOrderTriageStatusId.SPECIALTY_CARE).collect(Collectors.toList()).size()));
			csvPrinter.printRecord("Self-scheduled Appointments", Integer.toString(patientOrders.stream().filter(it -> it.getAppointmentScheduledByPatient() ==
					true).collect(Collectors.toList()).size()));

			Optional<String> avgDaysFromReferralToCompletedAssessment = getDatabase().queryForObject(format("""
					SELECT to_char(avg(most_recent_screening_session_completed_at - order_date ) , 'DD') 
					FROM v_patient_order
					%s
					AND patient_order_screening_status_id = 'COMPLETE'""", whereClauseWithParameters.getWhereClause()), String.class, whereClauseWithParameters.getParameters().toArray());
			csvPrinter.printRecord("Average Days to Complete Assessment", avgDaysFromReferralToCompletedAssessment.isPresent() ? avgDaysFromReferralToCompletedAssessment.get() : "N/A");

			Optional<String> avgDaysToSelfScheduleAppointment = getDatabase().queryForObject(format("""
					SELECT to_char(avg(appointment_start_time - order_date), 'DD')
					FROM v_patient_order
					%s
					AND appointment_scheduled_by_patient = true""", whereClauseWithParameters.getWhereClause()), String.class, whereClauseWithParameters.getParameters().toArray());
			csvPrinter.printRecord("Average Days to Self Schedule Appointment", avgDaysToSelfScheduleAppointment.isPresent() ? avgDaysToSelfScheduleAppointment.get() : "N/A");

			csvPrinter.println();
			csvPrinter.printRecord("Referral Reason(s)", "Count");
			if (referralReasons.size() > 0) {
				for (DescriptionWithCountRecord referralReason : referralReasons) {
					List<String> recordElements = new ArrayList<>();

					recordElements.add(referralReason.getDescription());
					recordElements.add(referralReason.getCount().toString());

					csvPrinter.printRecord(recordElements.toArray(new Object[0]));
				}
			} else
				csvPrinter.printRecord("No Referral Reasons");

			csvPrinter.println();
			csvPrinter.printRecord("Assessment Reason(s)", "Count");
			if (assessmentOverridePatientOrders.size() > 0) {
				for (DescriptionWithCountRecord assessmentOverridePatientOrder : assessmentOverridePatientOrders) {
					List<String> recordElements = new ArrayList<>();
					recordElements.add(assessmentOverridePatientOrder.getDescription());
					recordElements.add(assessmentOverridePatientOrder.getCount().toString());
					csvPrinter.printRecord(recordElements.toArray(new Object[0]));
				}
			} else
				csvPrinter.printRecord("No Assessment Reasons");

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	public void runIcOutreachReportCsv(@Nonnull InstitutionId institutionId,
																		 @Nonnull LocalDateTime startDateTime,
																		 @Nonnull LocalDateTime endDateTime,
																		 @Nonnull Optional<List<String>> payorName,
																		 @Nonnull Optional<List<String>> practiceName,
																		 @Nonnull Optional<Integer> patientAgeFrom,
																		 @Nonnull Optional<Integer> patientAgeTo,
																		 @Nonnull Optional<List<RaceId>> raceId,
																		 @Nonnull Optional<List<GenderIdentityId>> genderIdentityId,
																		 @Nonnull @QueryParameter Optional<List<UUID>> panelAccountId,
																		 @Nonnull ZoneId reportTimeZone,
																		 @Nonnull Locale reportLocale,
																		 @Nonnull Writer writer) {
		requireNonNull(institutionId);
		requireNonNull(startDateTime);
		requireNonNull(endDateTime);
		requireNonNull(payorName);
		requireNonNull(practiceName);
		requireNonNull(patientAgeFrom);
		requireNonNull(patientAgeTo);
		requireNonNull(raceId);
		requireNonNull(genderIdentityId);
		requireNonNull(panelAccountId);
		requireNonNull(reportTimeZone);
		requireNonNull(reportLocale);
		requireNonNull(writer);

		IcWhereClauseWithParameters whereClauseWithParameters = buildIcWhereClauseWithParameters(institutionId, startDateTime, endDateTime, payorName, practiceName, patientAgeFrom,
				patientAgeTo, raceId, genderIdentityId, Optional.empty());

		//All patient orders matching the report filters
		List<PatientOrder> patientOrders = getDatabase().queryForList(
				format("SELECT * FROM v_patient_order %s", whereClauseWithParameters.getWhereClause()), PatientOrder.class, whereClauseWithParameters.getParameters().toArray());
		//Just Open Orders
		List<PatientOrder> openPatientOrders = patientOrders.stream().filter(it -> it.getPatientOrderDispositionId() == PatientOrderDispositionId.OPEN).collect(Collectors.toList());

		List<DescriptionWithCountRecord> assessmentStatusCounts = getDatabase().queryForList(format("""
						SELECT patient_order_screening_status_description as description, COUNT(*) as count
						FROM v_patient_order
						%s
						AND patient_order_screening_status_id != 'COMPLETE'
						AND patient_order_disposition_id = 'OPEN'
						GROUP BY patient_order_screening_status_description
						ORDER BY patient_order_screening_status_description""", whereClauseWithParameters.getWhereClause()),
				DescriptionWithCountRecord.class, whereClauseWithParameters.getParameters().toArray());

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
			addFilterDescription(csvPrinter, payorName, practiceName, patientAgeFrom, patientAgeTo, raceId, genderIdentityId, Optional.empty());

			csvPrinter.printRecord("Patients Requiring Scheduling", Integer.toString(openPatientOrders.stream().filter(it -> it.getPatientOrderTriageStatusId() ==
					PatientOrderTriageStatusId.MHP && it.getAppointmentScheduled() == false).collect(Collectors.toList()).size()));
			csvPrinter.printRecord("Patients Requiring Resources", Integer.toString(openPatientOrders.stream().filter(it -> it.getPatientOrderResourcingStatusId() ==
					PatientOrderResourcingStatusId.NEEDS_RESOURCES).collect(Collectors.toList()).size()));

			//Total # of pts requiring outreach = total referrals - count of patients that self-scheduled - patients that rejected care
			csvPrinter.printRecord("Patients Requiring Outreach", openPatientOrders.size() -
					openPatientOrders.stream().filter(it -> it.getAppointmentScheduledByPatient() == true ||
							it.getPatientOrderConsentStatusId() == PatientOrderConsentStatus.PatientOrderConsentStatusId.REJECTED).collect(Collectors.toList()).size());
			//TODO: # of days since referral + # of pt outreach attempts
			csvPrinter.printRecord("Patients Sent Resources", Integer.toString(patientOrders.stream().filter(it ->
					it.getPatientOrderResourcingStatusId() == PatientOrderResourcingStatusId.SENT_RESOURCES).collect(Collectors.toList()).size()));

			csvPrinter.println();
			csvPrinter.printRecord("Patients Requiring Assessment", "Count");
			if (assessmentStatusCounts.size() > 0) {
				for (DescriptionWithCountRecord assessmentStatusCount : assessmentStatusCounts) {
					List<String> recordElements = new ArrayList<>();

					recordElements.add(assessmentStatusCount.getDescription());
					recordElements.add(assessmentStatusCount.getCount().toString());

					csvPrinter.printRecord(recordElements.toArray(new Object[0]));
				}
			} else
				csvPrinter.printRecord("No Patients Requiring Assessment");
			csvPrinter.println();

			//The following data points are specific to one of more MHICs (if specified in the report filters
			IcWhereClauseWithParameters whereClauseWithParametersForMhics = buildIcWhereClauseWithParameters(institutionId, startDateTime, endDateTime, payorName, practiceName, patientAgeFrom,
					patientAgeTo, raceId, genderIdentityId, panelAccountId);

			//All patient orders matching the report filters
			List<PatientOrder> patientOrdersForMhics = getDatabase().queryForList(
					format("SELECT * FROM v_patient_order %s", whereClauseWithParametersForMhics.getWhereClause()), PatientOrder.class, whereClauseWithParametersForMhics.getParameters().toArray());
			addFilterDescription(csvPrinter, payorName, practiceName, patientAgeFrom, patientAgeTo, raceId, genderIdentityId, panelAccountId);
			csvPrinter.printRecord("Calls/Voicemails", patientOrdersForMhics.stream().map(it -> it.getOutreachCount()).reduce(0, Integer::sum));
			csvPrinter.printRecord("Texts/Emails", patientOrdersForMhics.stream().map(it -> it.getScheduledMessageGroupCount()).reduce(0, Integer::sum));
			csvPrinter.printRecord("Closed Orders", Integer.toString(patientOrdersForMhics.stream().filter(it -> it.getPatientOrderDispositionId() !=
					PatientOrderDispositionId.OPEN).collect(Collectors.toList()).size()));
			csvPrinter.printRecord("Assessment Overrides", Integer.toString(patientOrdersForMhics.stream().filter(it -> it.getPatientOrderTriageSourceId() ==
					PatientOrderTriageSourceId.MANUALLY_SET).collect(Collectors.toList()).size()));

			List<DescriptionWithCountRecord> assessmentOverridePatientOrders = findTriageReasonsPatientOrders(whereClauseWithParametersForMhics, PatientOrderTriageSourceId.MANUALLY_SET);
			csvPrinter.println();
			csvPrinter.printRecord("Assessment Override Reason(s)", "Count");
			if (assessmentOverridePatientOrders.size() > 0) {
				for (DescriptionWithCountRecord assessmentOverridePatientOrder : assessmentOverridePatientOrders) {
					List<String> recordElements = new ArrayList<>();
					recordElements.add(assessmentOverridePatientOrder.getDescription());
					recordElements.add(assessmentOverridePatientOrder.getCount().toString());
					csvPrinter.printRecord(recordElements.toArray(new Object[0]));
				}
			} else
				csvPrinter.printRecord("No Assessment Override Reasons");

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	public void runIcAssessmentReportCsv(@Nonnull InstitutionId institutionId,
																			 @Nonnull LocalDateTime startDateTime,
																			 @Nonnull LocalDateTime endDateTime,
																			 @Nonnull Optional<List<String>> payorName,
																			 @Nonnull Optional<List<String>> practiceName,
																			 @Nonnull Optional<Integer> patientAgeFrom,
																			 @Nonnull Optional<Integer> patientAgeTo,
																			 @Nonnull Optional<List<RaceId>> raceId,
																			 @Nonnull Optional<List<GenderIdentityId>> genderIdentityId,
																			 @Nonnull ZoneId reportTimeZone,
																			 @Nonnull Locale reportLocale,
																			 @Nonnull Writer writer) {
		requireNonNull(institutionId);
		requireNonNull(startDateTime);
		requireNonNull(endDateTime);
		requireNonNull(payorName);
		requireNonNull(practiceName);
		requireNonNull(patientAgeFrom);
		requireNonNull(patientAgeTo);
		requireNonNull(raceId);
		requireNonNull(genderIdentityId);
		requireNonNull(reportTimeZone);
		requireNonNull(reportLocale);
		requireNonNull(writer);

		IcWhereClauseWithParameters whereClauseWithParameters = buildIcWhereClauseWithParameters(institutionId, startDateTime, endDateTime, payorName, practiceName, patientAgeFrom,
				patientAgeTo, raceId, genderIdentityId, Optional.empty());

		//All patient orders matching the report filters
		List<PatientOrder> patientOrders = getDatabase().queryForList(
				format("SELECT * FROM v_patient_order %s", whereClauseWithParameters.getWhereClause()), PatientOrder.class, whereClauseWithParameters.getParameters().toArray());

		List<AssessmentScoreRecord> assessmentScores = getDatabase().queryForList(
				format("""
						SELECT st.description, sss.score->>'overallScore' as score, count(*) as count
						FROM screening_session_screening sss, screening_session ss, screening_version sv, screening_type st, patient_order po
						%s
						AND sss.screening_version_id = sv.screening_version_id
						AND sss.screening_session_id = ss.screening_session_id
						AND sv.screening_type_id  = st.screening_type_id
						AND ss.patient_order_id = po.patient_order_id
						AND st.screening_type_id NOT IN  ('IC_INTRO', 'IC_INTRO_SYMPTOMS')
						AND ss.completed = true
						GROUP BY st.description, sss.score->>'overallScore'
						ORDER BY st.description, (sss.score->>'overallScore')::int""", whereClauseWithParameters.getWhereClause()),
				AssessmentScoreRecord.class, whereClauseWithParameters.getParameters().toArray());
		String lastDescription = null;
		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
			addFilterDescription(csvPrinter, payorName, practiceName, patientAgeFrom, patientAgeTo, raceId, genderIdentityId, Optional.empty());
			csvPrinter.printRecord("Number of Patients Completing Assessment", patientOrders.stream().filter(it -> it.getPatientOrderScreeningStatusId() ==
					PatientOrderScreeningStatusId.COMPLETE).collect(Collectors.toList()).size());
			csvPrinter.println();
			csvPrinter.printRecord("Assessment", "Score", "Number of Patients Achieving Score");
			if (assessmentScores.size() > 0) {
				for (AssessmentScoreRecord assessmentScore : assessmentScores) {
					List<String> recordElements = new ArrayList<>();

					if (lastDescription != null && lastDescription.compareTo(assessmentScore.getDescription()) == 0)
						recordElements.add("");
					else
						recordElements.add(assessmentScore.getDescription());

					recordElements.add(assessmentScore.getScore().toString());
					recordElements.add(assessmentScore.getCount().toString());
					lastDescription = assessmentScore.getDescription();

					csvPrinter.printRecord(recordElements.toArray(new Object[0]));
				}
			} else
				csvPrinter.printRecord("No matching assessment data");

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}


	public void runGroupSessionReservationEmailsReportCsv(@Nonnull UUID groupSessionId,
																												@Nonnull ZoneId reportTimeZone,
																												@Nonnull Locale reportLocale,
																												@Nonnull Writer writer) {
		requireNonNull(groupSessionId);
		requireNonNull(reportTimeZone);
		requireNonNull(reportLocale);
		requireNonNull(writer);

		List<GroupSessionReservation> groupSessionReservations = getGroupSessionService().findGroupSessionReservationsByGroupSessionId(groupSessionId);

		DateTimeFormatter instantFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss")
				.withLocale(reportLocale)
				.withZone(reportTimeZone);

		List<String> headerColumns = List.of(
				getStrings().get("Group Session Reservation ID"),
				getStrings().get("Email Address"),
				getStrings().get("Reserved At")
		);

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headerColumns.toArray(new String[0])))) {
			for (GroupSessionReservation groupSessionReservation : groupSessionReservations) {
				List<String> recordElements = new ArrayList<>();

				recordElements.add(groupSessionReservation.getGroupSessionReservationId().toString());
				recordElements.add(groupSessionReservation.getEmailAddress());
				recordElements.add(instantFormatter.format(groupSessionReservation.getCreated()));

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

	@NotThreadSafe
	protected static class ProviderAppointmentEap extends Appointment {
		@Nullable
		private String providerName;
		@Nullable
		private String accountFirstName;
		@Nullable
		private String accountLastName;
		@Nullable
		private String accountEmailAddress;
		@Nullable
		private AccountSource.AccountSourceId accountSourceId;
		@Nullable
		private AssessmentAnswer firstNameAnswer;
		@Nullable
		private AssessmentAnswer lastNameAnswer;
		@Nullable
		private AssessmentAnswer phoneNumberAnswer;
		@Nullable
		private String note;

		@Nullable
		public String getProviderName() {
			return this.providerName;
		}

		public void setProviderName(@Nullable String providerName) {
			this.providerName = providerName;
		}

		@Nullable
		public AccountSource.AccountSourceId getAccountSourceId() {
			return this.accountSourceId;
		}

		public void setAccountSourceId(@Nullable AccountSource.AccountSourceId accountSourceId) {
			this.accountSourceId = accountSourceId;
		}

		@Nullable
		public String getAccountLastName() {
			return this.accountLastName;
		}

		@Nullable
		public String getAccountEmailAddress() {
			return this.accountEmailAddress;
		}

		public void setAccountEmailAddress(@Nullable String accountEmailAddress) {
			this.accountEmailAddress = accountEmailAddress;
		}

		public void setAccountLastName(@Nullable String accountLastName) {
			this.accountLastName = accountLastName;
		}

		@Nullable
		public String getAccountFirstName() {
			return this.accountFirstName;
		}

		public void setAccountFirstName(@Nullable String accountFirstName) {
			this.accountFirstName = accountFirstName;
		}

		@Nullable
		public AssessmentAnswer getFirstNameAnswer() {
			return this.firstNameAnswer;
		}

		public void setFirstNameAnswer(@Nullable AssessmentAnswer firstNameAnswer) {
			this.firstNameAnswer = firstNameAnswer;
		}

		@Nullable
		public AssessmentAnswer getLastNameAnswer() {
			return this.lastNameAnswer;
		}

		public void setLastNameAnswer(@Nullable AssessmentAnswer lastNameAnswer) {
			this.lastNameAnswer = lastNameAnswer;
		}

		@Nullable
		public AssessmentAnswer getPhoneNumberAnswer() {
			return this.phoneNumberAnswer;
		}

		public void setPhoneNumberAnswer(@Nullable AssessmentAnswer phoneNumberAnswer) {
			this.phoneNumberAnswer = phoneNumberAnswer;
		}

		@Nullable
		public String getNote() {
			return this.note;
		}

		public void setNote(@Nullable String note) {
			this.note = note;
		}
	}

	@NotThreadSafe
	protected static class AssessmentAnswer {
		@Nullable
		private String questionText;
		@Nullable
		private String answerText;

		@Nullable
		public String getQuestionText() {
			return this.questionText;
		}

		public void setQuestionText(@Nullable String questionText) {
			this.questionText = questionText;
		}

		@Nullable
		public String getAnswerText() {
			return this.answerText;
		}

		public void setAnswerText(@Nullable String answerText) {
			this.answerText = answerText;
		}
	}

	@NotThreadSafe
	protected static class IcWhereClauseWithParameters {
		@Nullable
		private String whereClause;
		@Nullable
		private List<Object> parameters;

		@Nullable
		public String getWhereClause() {
			return whereClause;
		}

		public void setWhereClause(@Nullable String whereClause) {
			this.whereClause = whereClause;
		}

		@Nullable
		public List<Object> getParameters() {
			return parameters;
		}

		public void setParameters(@Nullable List<Object> parameters) {
			this.parameters = parameters;
		}
	}

	@NotThreadSafe
	protected static class DescriptionWithCountRecord {
		@Nullable
		private String description;
		@Nullable
		private Integer count;

		@Nullable
		public String getDescription() {
			return description;
		}

		public void setDescription(@Nullable String description) {
			this.description = description;
		}

		@Nullable
		public Integer getCount() {
			return count;
		}

		public void setCount(@Nullable Integer count) {
			this.count = count;
		}
	}

	@NotThreadSafe
	protected static class AssessmentScoreRecord {
		@Nullable
		private String description;
		@Nullable
		private String score;
		@Nullable
		private Integer count;

		@Nullable
		public String getDescription() {
			return description;
		}

		public void setDescription(@Nullable String description) {
			this.description = description;
		}

		@Nullable
		public String getScore() {
			return score;
		}

		public void setScore(@Nullable String score) {
			this.score = score;
		}

		@Nullable
		public Integer getCount() {
			return count;
		}

		public void setCount(@Nullable Integer count) {
			this.count = count;
		}
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountServiceProvider.get();
	}

	@Nonnull
	protected GroupSessionService getGroupSessionService() {
		return this.groupSessionServiceProvider.get();
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return this.authorizationServiceProvider.get();
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return this.formatter;
	}
}
