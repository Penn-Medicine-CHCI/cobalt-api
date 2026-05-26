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
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.AnalyticsNativeEventType.AnalyticsNativeEventTypeId;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.AppointmentTypeAssessment;
import com.cobaltplatform.api.model.db.Clinic;
import com.cobaltplatform.api.model.db.GenderIdentity.GenderIdentityId;
import com.cobaltplatform.api.model.db.GroupSessionReservation;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.MessageType.MessageTypeId;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.PatientOrderConsentStatus;
import com.cobaltplatform.api.model.db.PatientOrderDisposition.PatientOrderDispositionId;
import com.cobaltplatform.api.model.db.PatientOrderResourcingStatus.PatientOrderResourcingStatusId;
import com.cobaltplatform.api.model.db.PatientOrderSafetyPlanningStatus.PatientOrderSafetyPlanningStatusId;
import com.cobaltplatform.api.model.db.PatientOrderScreeningStatus.PatientOrderScreeningStatusId;
import com.cobaltplatform.api.model.db.PatientOrderTriageSource.PatientOrderTriageSourceId;
import com.cobaltplatform.api.model.db.PatientOrderTriageStatus.PatientOrderTriageStatusId;
import com.cobaltplatform.api.model.db.Race.RaceId;
import com.cobaltplatform.api.model.db.ReportType;
import com.cobaltplatform.api.model.db.ReportType.ReportTypeId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.model.service.AccountCapabilityFlags;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lokalized.Strings;
import com.pyranid.Database;
import com.pyranid.DatabaseColumn;
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
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
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
	private static final Gson GSON;

	static {
		GSON = new Gson();
	}

	@Nonnull
	private final Provider<InstitutionService> institutionServiceProvider;
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
	public ReportingService(@Nonnull Provider<InstitutionService> institutionServiceProvider,
													@Nonnull Provider<AccountService> accountServiceProvider,
													@Nonnull Provider<GroupSessionService> groupSessionServiceProvider,
													@Nonnull Provider<AuthorizationService> authorizationServiceProvider,
													@Nonnull DatabaseProvider databaseProvider,
													@Nonnull Strings strings,
													@Nonnull Formatter formatter,
													@Nonnull Configuration configuration) {
		requireNonNull(institutionServiceProvider);
		requireNonNull(accountServiceProvider);
		requireNonNull(groupSessionServiceProvider);
		requireNonNull(authorizationServiceProvider);
		requireNonNull(databaseProvider);
		requireNonNull(strings);
		requireNonNull(formatter);
		requireNonNull(configuration);

		this.institutionServiceProvider = institutionServiceProvider;
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
							|| reportType.getReportTypeId() == ReportTypeId.IC_ASSESSMENT
							|| reportType.getReportTypeId() == ReportTypeId.IC_SAFETY_PLANNING
							|| reportType.getReportTypeId() == ReportTypeId.IC_TRIAGE)
						return accountCapabilityFlags.isCanViewIcReports();

					if (reportType.getReportTypeId() == ReportTypeId.GROUP_SESSION_RESERVATION_EMAILS)
						return accountCapabilityFlags.isCanAdministerGroupSessions();

					if (reportType.getReportTypeId() == ReportTypeId.SIGN_IN_PAGEVIEW_NO_ACCOUNT)
						return accountCapabilityFlags.isCanViewAnalytics();

					if (reportType.getReportTypeId() == ReportTypeId.ACCOUNT_SIGNUP_UNVERIFIED)
						return accountCapabilityFlags.isCanViewAnalytics();

					if (reportType.getReportTypeId() == ReportTypeId.ACCOUNT_ONBOARDING_INCOMPLETE)
						return accountCapabilityFlags.isCanViewAnalytics();

					if (reportType.getReportTypeId() == ReportTypeId.ACCOUNT_ONBOARDING_COMPLETE)
						return accountCapabilityFlags.isCanViewAnalytics();

					if (reportType.getReportTypeId() == ReportTypeId.ACCOUNT_ONBOARDING_COMPLETE_V2)
						return accountCapabilityFlags.isCanViewAnalytics();

					if (reportType.getReportTypeId() == ReportTypeId.ACCOUNT_GEOLOCATION)
						return accountCapabilityFlags.isCanViewAnalytics();

					if (reportType.getReportTypeId() == ReportTypeId.COURSE_MCB_DOWNLOAD)
						return accountCapabilityFlags.isCanViewAnalytics();

					if (reportType.getReportTypeId() == ReportTypeId.ACCOUNT_TIMELINE)
						return accountCapabilityFlags.isCanViewAnalytics();

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
																																			 @Nonnull Optional<List<String>> referringPracticeIds,
																																			 @Nonnull Optional<Integer> patientAgeFrom,
																																			 @Nonnull Optional<Integer> patientAgeTo,
																																			 @Nonnull Optional<List<RaceId>> raceId,
																																			 @Nonnull Optional<List<GenderIdentityId>> genderIdentityId,
																																			 @Nonnull Optional<List<UUID>> panelAccountId) {
		requireNonNull(startDateTime);
		requireNonNull(endDateTime);
		requireNonNull(payorName);
		requireNonNull(referringPracticeIds);
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

		if (referringPracticeIds.isPresent()) {
			whereClause.append(format(" AND epic_department_id IN (SELECT epic_department_id FROM epic_department WHERE institution_id=? AND department_id IN %s) ", sqlInListPlaceholders(referringPracticeIds.get())));
			parameters.add(institutionId);
			parameters.addAll(referringPracticeIds.get());
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
																		@Nonnull Optional<List<String>> referringPracticeIds,
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

			if (referringPracticeIds.isPresent()) {
				csVPrinter.printRecord("Practice IDs", referringPracticeIds.get().stream().collect(Collectors.joining(", ")));
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
						FROM patient_order_triage_group potg, v_all_patient_order vpo, patient_order_triage_override_reason potor
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
																		 @Nonnull Optional<List<String>> referringPracticeIds,
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
		requireNonNull(referringPracticeIds);
		requireNonNull(patientAgeFrom);
		requireNonNull(patientAgeTo);
		requireNonNull(raceId);
		requireNonNull(genderIdentityId);
		requireNonNull(reportTimeZone);
		requireNonNull(reportLocale);
		requireNonNull(writer);

		IcWhereClauseWithParameters whereClauseWithParameters = buildIcWhereClauseWithParameters(institutionId, startDateTime, endDateTime, payorName, referringPracticeIds, patientAgeFrom,
				patientAgeTo, raceId, genderIdentityId, Optional.empty());

		//All patient orders matching the report filters
		List<PatientOrder> patientOrders = getDatabase().queryForList(
				format("SELECT * FROM v_all_patient_order %s", whereClauseWithParameters.getWhereClause()), PatientOrder.class, whereClauseWithParameters.getParameters().toArray());

		//List of referral reasons
		StringBuilder reasonForReferralQuery = new StringBuilder(format("""
				SELECT reason_for_referral as description, count(*) as count
				FROM v_all_patient_order
				%s
				GROUP BY reason_for_referral
				ORDER BY reason_for_referral""", whereClauseWithParameters.getWhereClause()));
		List<DescriptionWithCountRecord> referralReasons = getDatabase().queryForList(reasonForReferralQuery.toString(),
				DescriptionWithCountRecord.class, whereClauseWithParameters.getParameters().toArray());

		List<DescriptionWithCountRecord> assessmentOverridePatientOrders = findTriageReasonsPatientOrders(whereClauseWithParameters, PatientOrderTriageSourceId.COBALT);

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
			addFilterDescription(csvPrinter, payorName, referringPracticeIds, patientAgeFrom, patientAgeTo, raceId, genderIdentityId, Optional.empty());

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
					FROM v_all_patient_order
					%s
					AND patient_order_screening_status_id = 'COMPLETE'""", whereClauseWithParameters.getWhereClause()), String.class, whereClauseWithParameters.getParameters().toArray());
			csvPrinter.printRecord("Average Days to Complete Assessment", avgDaysFromReferralToCompletedAssessment.isPresent() ? avgDaysFromReferralToCompletedAssessment.get() : "N/A");

			Optional<String> avgDaysToSelfScheduleAppointment = getDatabase().queryForObject(format("""
					SELECT to_char(avg(appointment_start_time - order_date), 'DD')
					FROM v_all_patient_order
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
																		 @Nonnull Optional<List<String>> referringPracticeIds,
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
		requireNonNull(referringPracticeIds);
		requireNonNull(patientAgeFrom);
		requireNonNull(patientAgeTo);
		requireNonNull(raceId);
		requireNonNull(genderIdentityId);
		requireNonNull(panelAccountId);
		requireNonNull(reportTimeZone);
		requireNonNull(reportLocale);
		requireNonNull(writer);

		IcWhereClauseWithParameters whereClauseWithParameters = buildIcWhereClauseWithParameters(institutionId, startDateTime, endDateTime, payorName, referringPracticeIds, patientAgeFrom,
				patientAgeTo, raceId, genderIdentityId, Optional.empty());

		//All patient orders matching the report filters
		List<PatientOrder> patientOrders = getDatabase().queryForList(
				format("SELECT * FROM v_all_patient_order %s", whereClauseWithParameters.getWhereClause()), PatientOrder.class, whereClauseWithParameters.getParameters().toArray());
		//Just Open Orders
		List<PatientOrder> openPatientOrders = patientOrders.stream().filter(it -> it.getPatientOrderDispositionId() == PatientOrderDispositionId.OPEN).collect(Collectors.toList());

		List<DescriptionWithCountRecord> assessmentStatusCounts = getDatabase().queryForList(format("""
						SELECT patient_order_screening_status_description as description, COUNT(*) as count
						FROM v_all_patient_order
						%s
						AND patient_order_screening_status_id != 'COMPLETE'
						AND patient_order_disposition_id = 'OPEN'
						GROUP BY patient_order_screening_status_description
						ORDER BY patient_order_screening_status_description""", whereClauseWithParameters.getWhereClause()),
				DescriptionWithCountRecord.class, whereClauseWithParameters.getParameters().toArray());

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
			addFilterDescription(csvPrinter, payorName, referringPracticeIds, patientAgeFrom, patientAgeTo, raceId, genderIdentityId, Optional.empty());

			csvPrinter.printRecord("Patients Requiring Scheduling", Integer.toString(openPatientOrders.stream().filter(it ->
					(it.getPatientOrderTriageStatusId() == PatientOrderTriageStatusId.MHP
							|| (it.getPatientOrderTriageStatusId() == PatientOrderTriageStatusId.SPECIALTY_CARE
							&& it.getOverrideSchedulingEpicDepartmentId() != null))
							&& it.getAppointmentScheduled() == false).collect(Collectors.toList()).size()));
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
			IcWhereClauseWithParameters whereClauseWithParametersForMhics = buildIcWhereClauseWithParameters(institutionId, startDateTime, endDateTime, payorName, referringPracticeIds, patientAgeFrom,
					patientAgeTo, raceId, genderIdentityId, panelAccountId);

			//All patient orders matching the report filters
			List<PatientOrder> patientOrdersForMhics = getDatabase().queryForList(
					format("SELECT * FROM v_all_patient_order %s", whereClauseWithParametersForMhics.getWhereClause()), PatientOrder.class, whereClauseWithParametersForMhics.getParameters().toArray());
			addFilterDescription(csvPrinter, payorName, referringPracticeIds, patientAgeFrom, patientAgeTo, raceId, genderIdentityId, panelAccountId);
			csvPrinter.printRecord("Calls/Voicemails", patientOrdersForMhics.stream().map(it -> it.getOutreachCount()).reduce(0, Integer::sum));
			csvPrinter.printRecord("Texts/Emails", patientOrdersForMhics.stream().map(it -> it.getScheduledMessageGroupDeliveredCount()).reduce(0, Integer::sum));
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
																			 @Nonnull Optional<List<String>> referringPracticeIds,
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
		requireNonNull(referringPracticeIds);
		requireNonNull(patientAgeFrom);
		requireNonNull(patientAgeTo);
		requireNonNull(raceId);
		requireNonNull(genderIdentityId);
		requireNonNull(reportTimeZone);
		requireNonNull(reportLocale);
		requireNonNull(writer);

		IcWhereClauseWithParameters whereClauseWithParameters = buildIcWhereClauseWithParameters(institutionId, startDateTime, endDateTime, payorName, referringPracticeIds, patientAgeFrom,
				patientAgeTo, raceId, genderIdentityId, Optional.empty());

		//All patient orders matching the report filters
		List<PatientOrder> patientOrders = getDatabase().queryForList(
				format("SELECT * FROM v_all_patient_order %s", whereClauseWithParameters.getWhereClause()), PatientOrder.class, whereClauseWithParameters.getParameters().toArray());

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
			addFilterDescription(csvPrinter, payorName, referringPracticeIds, patientAgeFrom, patientAgeTo, raceId, genderIdentityId, Optional.empty());
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

	public void runIcSafetyPlanningReportCsv(@Nonnull InstitutionId institutionId,
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

		List<IcSafetyPlanningReportRecord> records = getDatabase().queryForList("""
				SELECT
					po.reference_number as cobalt_reference_number,
					ss.crisis_indicated_at,
					po.most_recent_screening_session_created_by_account_role_id as assessment_performed_by,
					po.patient_mrn as patient_mrn,
					po.patient_unique_id as patient_uid,
					po.patient_first_name,
					po.patient_last_name,
					po.patient_order_safety_planning_status_id
				FROM
				  v_all_patient_order po,
				  screening_session ss
				WHERE
				  ss.patient_order_id=po.patient_order_id
				  AND po.institution_id=?
				  AND ss.crisis_indicated_at >= ?
				  AND ss.crisis_indicated_at <= ?
				  AND po.test_patient_order=FALSE
				  AND ss.crisis_indicated=TRUE
				ORDER BY
				  ss.crisis_indicated_at
				""", IcSafetyPlanningReportRecord.class, institutionId, startDateTime.atZone(reportTimeZone).toInstant(), endDateTime.atZone(reportTimeZone).toInstant());

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
				.withZone(reportTimeZone)
				.withLocale(reportLocale);

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
			csvPrinter.printRecord(
					getStrings().get("Cobalt Reference Number"),
					getStrings().get("Crisis Indicated At"),
					getStrings().get("Assessment Performed By"),
					getStrings().get("Patient {{mrnType}}", Map.of("mrnType", institution.getEpicPatientMrnTypeName())),
					getStrings().get("Patient {{uidType}}", Map.of("uidType", institution.getEpicPatientUniqueIdType())),
					getStrings().get("Patient First Name"),
					getStrings().get("Patient Last Name"),
					getStrings().get("Safety Planning Status")
			);

			for (IcSafetyPlanningReportRecord record : records) {
				List<String> recordElements = new ArrayList<>(8);
				recordElements.add(String.valueOf(record.getCobaltReferenceNumber()));
				recordElements.add(dateTimeFormatter.format(record.getCrisisIndicatedAt()));
				recordElements.add(record.getAssessmentPerformedBy());
				recordElements.add(record.getPatientMrn());
				recordElements.add(record.getPatientUid());
				recordElements.add(record.getPatientFirstName());
				recordElements.add(record.getPatientLastName());
				recordElements.add(record.getPatientOrderSafetyPlanningStatusId().name());

				csvPrinter.printRecord(recordElements.toArray(new Object[0]));
			}

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@NotThreadSafe
	protected static class IcSafetyPlanningReportRecord {
		@Nullable
		private Long cobaltReferenceNumber;
		@Nullable
		private Instant crisisIndicatedAt;
		@Nullable
		private String assessmentPerformedBy;
		@Nullable
		private String patientMrn;
		@Nullable
		private String patientUid;
		@Nullable
		private String patientFirstName;
		@Nullable
		private String patientLastName;
		@Nullable
		private PatientOrderSafetyPlanningStatusId patientOrderSafetyPlanningStatusId;

		@Nullable
		public Long getCobaltReferenceNumber() {
			return this.cobaltReferenceNumber;
		}

		public void setCobaltReferenceNumber(@Nullable Long cobaltReferenceNumber) {
			this.cobaltReferenceNumber = cobaltReferenceNumber;
		}

		@Nullable
		public Instant getCrisisIndicatedAt() {
			return this.crisisIndicatedAt;
		}

		public void setCrisisIndicatedAt(@Nullable Instant crisisIndicatedAt) {
			this.crisisIndicatedAt = crisisIndicatedAt;
		}

		@Nullable
		public String getAssessmentPerformedBy() {
			return this.assessmentPerformedBy;
		}

		public void setAssessmentPerformedBy(@Nullable String assessmentPerformedBy) {
			this.assessmentPerformedBy = assessmentPerformedBy;
		}

		@Nullable
		public String getPatientMrn() {
			return this.patientMrn;
		}

		public void setPatientMrn(@Nullable String patientMrn) {
			this.patientMrn = patientMrn;
		}

		@Nullable
		public String getPatientUid() {
			return this.patientUid;
		}

		public void setPatientUid(@Nullable String patientUid) {
			this.patientUid = patientUid;
		}

		@Nullable
		public String getPatientFirstName() {
			return this.patientFirstName;
		}

		public void setPatientFirstName(@Nullable String patientFirstName) {
			this.patientFirstName = patientFirstName;
		}

		@Nullable
		public String getPatientLastName() {
			return this.patientLastName;
		}

		public void setPatientLastName(@Nullable String patientLastName) {
			this.patientLastName = patientLastName;
		}

		@Nullable
		public PatientOrderSafetyPlanningStatusId getPatientOrderSafetyPlanningStatusId() {
			return this.patientOrderSafetyPlanningStatusId;
		}

		public void setPatientOrderSafetyPlanningStatusId(@Nullable PatientOrderSafetyPlanningStatusId patientOrderSafetyPlanningStatusId) {
			this.patientOrderSafetyPlanningStatusId = patientOrderSafetyPlanningStatusId;
		}
	}

	public void runIcTriageReportCsv(@Nonnull InstitutionId institutionId,
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

		List<IcTriageReportRecord> records = getDatabase().queryForList("""
				SELECT
				  po.reference_number as cobalt_reference_number,
				  po.patient_mrn as patient_mrn,
				  po.patient_unique_id as patient_uid,
				  po.patient_first_name,
				  po.patient_last_name,
				  po.most_recent_intake_screening_session_created_by_account_role_id as assessments_performed_by,
				  po.most_recent_intake_screening_session_created_at as intake_assessment_started_at,
				  po.most_recent_intake_screening_session_completed_at as intake_assessment_completed_at,
				  po.most_recent_screening_session_created_at as clinical_assessment_started_at,
				  po.most_recent_screening_session_completed_at as clinical_assessment_completed_at,
				  po.patient_order_triage_status_id as triage
				FROM
				  v_all_patient_order po
				WHERE
				  po.institution_id=?
				  AND po.most_recent_intake_and_clinical_screenings_satisfied=TRUE
				  AND po.most_recent_intake_screening_session_created_at >= ?
				  AND po.most_recent_intake_screening_session_created_at <= ?
				  AND po.test_patient_order=FALSE
				ORDER BY po.most_recent_intake_screening_session_created_at
				""", IcTriageReportRecord.class, institutionId, startDateTime.atZone(reportTimeZone).toInstant(), endDateTime.atZone(reportTimeZone).toInstant());

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
				.withZone(reportTimeZone)
				.withLocale(reportLocale);

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
			csvPrinter.printRecord(
					getStrings().get("Cobalt Reference Number"),
					getStrings().get("Patient {{mrnType}}", Map.of("mrnType", institution.getEpicPatientMrnTypeName())),
					getStrings().get("Patient {{uidType}}", Map.of("uidType", institution.getEpicPatientUniqueIdType())),
					getStrings().get("Patient First Name"),
					getStrings().get("Patient Last Name"),
					getStrings().get("Assessments Performed By"),
					getStrings().get("Intake Assessment Started At"),
					getStrings().get("Intake Assessment Completed At"),
					getStrings().get("Clinical Assessment Started At"),
					getStrings().get("Clinical Assessment Completed At"),
					getStrings().get("Triage")
			);

			for (IcTriageReportRecord record : records) {
				List<String> recordElements = new ArrayList<>(11);
				recordElements.add(String.valueOf(record.getCobaltReferenceNumber()));
				recordElements.add(record.getPatientMrn());
				recordElements.add(record.getPatientUid());
				recordElements.add(record.getPatientFirstName());
				recordElements.add(record.getPatientLastName());
				recordElements.add(record.getAssessmentsPerformedBy());
				recordElements.add(record.getIntakeAssessmentStartedAt() == null ? "" : dateTimeFormatter.format(record.getIntakeAssessmentStartedAt()));
				recordElements.add(record.getIntakeAssessmentCompletedAt() == null ? "" : dateTimeFormatter.format(record.getIntakeAssessmentCompletedAt()));
				recordElements.add(record.getClinicalAssessmentStartedAt() == null ? "" : dateTimeFormatter.format(record.getClinicalAssessmentStartedAt()));
				recordElements.add(record.getClinicalAssessmentCompletedAt() == null ? "" : dateTimeFormatter.format(record.getClinicalAssessmentCompletedAt()));
				recordElements.add(record.getTriage());

				csvPrinter.printRecord(recordElements.toArray(new Object[0]));
			}

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@NotThreadSafe
	protected static class IcTriageReportRecord {
		@Nullable
		private Long cobaltReferenceNumber;
		@Nullable
		private String patientMrn;
		@Nullable
		private String patientUid;
		@Nullable
		private String patientFirstName;
		@Nullable
		private String patientLastName;
		@Nullable
		private String assessmentsPerformedBy;
		@Nullable
		private Instant intakeAssessmentStartedAt;
		@Nullable
		private Instant intakeAssessmentCompletedAt;
		@Nullable
		private Instant clinicalAssessmentStartedAt;
		@Nullable
		private Instant clinicalAssessmentCompletedAt;
		@Nullable
		private String triage;

		@Nullable
		public Long getCobaltReferenceNumber() {
			return this.cobaltReferenceNumber;
		}

		public void setCobaltReferenceNumber(@Nullable Long cobaltReferenceNumber) {
			this.cobaltReferenceNumber = cobaltReferenceNumber;
		}

		@Nullable
		public String getPatientMrn() {
			return this.patientMrn;
		}

		public void setPatientMrn(@Nullable String patientMrn) {
			this.patientMrn = patientMrn;
		}

		@Nullable
		public String getPatientUid() {
			return this.patientUid;
		}

		public void setPatientUid(@Nullable String patientUid) {
			this.patientUid = patientUid;
		}

		@Nullable
		public String getPatientFirstName() {
			return this.patientFirstName;
		}

		public void setPatientFirstName(@Nullable String patientFirstName) {
			this.patientFirstName = patientFirstName;
		}

		@Nullable
		public String getPatientLastName() {
			return this.patientLastName;
		}

		public void setPatientLastName(@Nullable String patientLastName) {
			this.patientLastName = patientLastName;
		}

		@Nullable
		public String getAssessmentsPerformedBy() {
			return this.assessmentsPerformedBy;
		}

		public void setAssessmentsPerformedBy(@Nullable String assessmentsPerformedBy) {
			this.assessmentsPerformedBy = assessmentsPerformedBy;
		}

		@Nullable
		public Instant getIntakeAssessmentStartedAt() {
			return this.intakeAssessmentStartedAt;
		}

		public void setIntakeAssessmentStartedAt(@Nullable Instant intakeAssessmentStartedAt) {
			this.intakeAssessmentStartedAt = intakeAssessmentStartedAt;
		}

		@Nullable
		public Instant getIntakeAssessmentCompletedAt() {
			return this.intakeAssessmentCompletedAt;
		}

		public void setIntakeAssessmentCompletedAt(@Nullable Instant intakeAssessmentCompletedAt) {
			this.intakeAssessmentCompletedAt = intakeAssessmentCompletedAt;
		}

		@Nullable
		public Instant getClinicalAssessmentStartedAt() {
			return this.clinicalAssessmentStartedAt;
		}

		public void setClinicalAssessmentStartedAt(@Nullable Instant clinicalAssessmentStartedAt) {
			this.clinicalAssessmentStartedAt = clinicalAssessmentStartedAt;
		}

		@Nullable
		public Instant getClinicalAssessmentCompletedAt() {
			return this.clinicalAssessmentCompletedAt;
		}

		public void setClinicalAssessmentCompletedAt(@Nullable Instant clinicalAssessmentCompletedAt) {
			this.clinicalAssessmentCompletedAt = clinicalAssessmentCompletedAt;
		}

		@Nullable
		public String getTriage() {
			return this.triage;
		}

		public void setTriage(@Nullable String triage) {
			this.triage = triage;
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

	public void runAdminAnalyticsSignInPageviewNoAccountReportCsv(@Nonnull InstitutionId institutionId,
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

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		ZoneId institutionTimeZone = institution.getTimeZone() != null ? institution.getTimeZone() : reportTimeZone;

		Instant startInstant = startDateTime.atZone(institutionTimeZone).toInstant();
		Instant endInstant = endDateTime.atZone(institutionTimeZone).toInstant();

		List<AdminAnalyticsSignInPageviewNoAccountReportRecord> records = getDatabase().queryForList("""
						SELECT
							ane.analytics_native_event_id,
							ane.timestamp,
							ane.analytics_native_event_type_id,
							ane.client_device_id,
							ane.session_id,
							ane.webapp_url,
							ane.referring_message_id,
							ane.referring_campaign,
							ane.user_agent,
							ane.app_name,
							ane.app_version,
							ane.client_device_time_zone
						FROM analytics_native_event ane
						WHERE ane.institution_id = ?
							AND ane.timestamp >= ?
							AND ane.timestamp <= ?
							AND ane.analytics_native_event_type_id IN (?, ?)
							AND ane.analytics_native_event_type_id <> ?
							AND ane.account_id IS NULL
							AND ane.user_agent_device_family IS NOT NULL
							AND ane.user_agent_device_family <> ALL (
								ARRAY['Googlebot'::text, 'Spider'::text, 'Baiduspider'::text, 'facebookexternalhit'::text]
							)
							AND NOT EXISTS (
								SELECT 1
								FROM account_client_device acd
								WHERE acd.client_device_id = ane.client_device_id
							)
						ORDER BY ane.timestamp
						""", AdminAnalyticsSignInPageviewNoAccountReportRecord.class, institutionId, startInstant, endInstant,
				AnalyticsNativeEventTypeId.PAGE_VIEW_SIGN_IN, AnalyticsNativeEventTypeId.PAGE_VIEW_SIGN_IN_EMAIL,
				AnalyticsNativeEventTypeId.HEARTBEAT);

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
				.withZone(institutionTimeZone)
				.withLocale(reportLocale);

		List<String> headerColumns = List.of(
				getStrings().get("Event ID"),
				getStrings().get("Event Timestamp ({{timeZone}})", Map.of("timeZone", institutionTimeZone.getId())),
				getStrings().get("Event Type"),
				getStrings().get("Client Device ID"),
				getStrings().get("Session ID"),
				getStrings().get("Webapp URL"),
				getStrings().get("Referring Message ID"),
				getStrings().get("Referring Campaign"),
				getStrings().get("User Agent"),
				getStrings().get("App Name"),
				getStrings().get("App Version"),
				getStrings().get("Client Device Time Zone")
		);

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headerColumns.toArray(new String[0])))) {
			for (AdminAnalyticsSignInPageviewNoAccountReportRecord record : records) {
				List<String> recordElements = new ArrayList<>(12);

				recordElements.add(record.getAnalyticsNativeEventId() == null ? "" : record.getAnalyticsNativeEventId().toString());
				recordElements.add(record.getTimestamp() == null ? "" : dateTimeFormatter.format(record.getTimestamp()));
				recordElements.add(record.getAnalyticsNativeEventTypeId() == null ? "" : record.getAnalyticsNativeEventTypeId().name());
				recordElements.add(record.getClientDeviceId() == null ? "" : record.getClientDeviceId().toString());
				recordElements.add(record.getSessionId() == null ? "" : record.getSessionId().toString());
				recordElements.add(record.getWebappUrl());
				recordElements.add(record.getReferringMessageId() == null ? "" : record.getReferringMessageId().toString());
				recordElements.add(record.getReferringCampaign());
				recordElements.add(record.getUserAgent());
				recordElements.add(record.getAppName());
				recordElements.add(record.getAppVersion());
				recordElements.add(record.getClientDeviceTimeZone());

				csvPrinter.printRecord(recordElements.toArray(new Object[0]));
			}

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void runAdminAnalyticsAccountSignupUnverifiedReportCsv(@Nonnull InstitutionId institutionId,
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

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		ZoneId institutionTimeZone = institution.getTimeZone() != null ? institution.getTimeZone() : reportTimeZone;

		Instant startInstant = startDateTime.atZone(institutionTimeZone).toInstant();
		Instant endInstant = endDateTime.atZone(institutionTimeZone).toInstant();

		List<AdminAnalyticsAccountSignupUnverifiedReportRecord> records = getDatabase().queryForList("""
						SELECT
							a.account_id,
							ai.created AS invite_created_at,
							ai.email_address,
							a.first_name,
							a.last_name,
							a.role_id,
							a.account_source_id,
							ml.message_id,
							ml.created AS message_created_at,
							ml.message_status_id,
							ml.delivered,
							ml.delivery_failed,
							ml.delivery_failed_reason
						FROM account_invite ai
						LEFT JOIN account a
							ON a.institution_id = ai.institution_id
							AND LOWER(a.email_address) = LOWER(ai.email_address)
						LEFT JOIN message_log ml
							ON ml.institution_id = ai.institution_id
							AND ml.message_type_id = ?
							AND ml.serialized_message->>'messageTemplate' = ?
							AND jsonb_typeof(ml.serialized_message->'toAddresses') = 'array'
							AND EXISTS (
								SELECT 1
								FROM jsonb_array_elements_text(ml.serialized_message->'toAddresses') addr
								WHERE LOWER(addr) = LOWER(ai.email_address)
							)
						WHERE ai.institution_id = ?
							AND ai.created >= ?
							AND ai.created <= ?
							AND ai.claimed = FALSE
							AND NOT EXISTS (
								SELECT 1
								FROM account_invite ai_claimed
								WHERE ai_claimed.institution_id = ai.institution_id
									AND LOWER(ai_claimed.email_address) = LOWER(ai.email_address)
									AND ai_claimed.claimed = TRUE
							)
							AND (a.account_id IS NULL OR a.role_id = ?)
							AND (a.account_id IS NULL OR a.test_account = FALSE)
							AND (a.account_id IS NULL OR a.account_source_id = ?)
						ORDER BY ai.created, ml.created
						""", AdminAnalyticsAccountSignupUnverifiedReportRecord.class, MessageTypeId.EMAIL, EmailMessageTemplate.ACCOUNT_VERIFICATION,
				institutionId, startInstant, endInstant, RoleId.PATIENT, AccountSourceId.EMAIL_PASSWORD);

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
				.withZone(institutionTimeZone)
				.withLocale(reportLocale);

		List<String> headerColumns = List.of(
				getStrings().get("Account ID"),
				getStrings().get("Invite Created At ({{timeZone}})", Map.of("timeZone", institutionTimeZone.getId())),
				getStrings().get("Email Address"),
				getStrings().get("First Name"),
				getStrings().get("Last Name"),
				getStrings().get("Role ID"),
				getStrings().get("Account Source ID"),
				getStrings().get("Message ID"),
				getStrings().get("Message Created At ({{timeZone}})", Map.of("timeZone", institutionTimeZone.getId())),
				getStrings().get("Message Status ID"),
				getStrings().get("Delivered At ({{timeZone}})", Map.of("timeZone", institutionTimeZone.getId())),
				getStrings().get("Delivery Failed At ({{timeZone}})", Map.of("timeZone", institutionTimeZone.getId())),
				getStrings().get("Delivery Failed Reason")
		);

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headerColumns.toArray(new String[0])))) {
			for (AdminAnalyticsAccountSignupUnverifiedReportRecord record : records) {
				List<String> recordElements = new ArrayList<>(13);

				recordElements.add(record.getAccountId() == null ? "" : record.getAccountId().toString());
				recordElements.add(record.getInviteCreatedAt() == null ? "" : dateTimeFormatter.format(record.getInviteCreatedAt()));
				recordElements.add(record.getEmailAddress() == null ? "" : record.getEmailAddress());
				recordElements.add(obfuscateName(record.getFirstName()));
				recordElements.add(obfuscateName(record.getLastName()));
				recordElements.add(record.getRoleId());
				recordElements.add(record.getAccountSourceId() == null ? "" : record.getAccountSourceId().name());
				recordElements.add(record.getMessageId() == null ? "" : record.getMessageId().toString());
				recordElements.add(record.getMessageCreatedAt() == null ? "" : dateTimeFormatter.format(record.getMessageCreatedAt()));
				recordElements.add(record.getMessageStatusId());
				recordElements.add(record.getDelivered() == null ? "" : dateTimeFormatter.format(record.getDelivered()));
				recordElements.add(record.getDeliveryFailed() == null ? "" : dateTimeFormatter.format(record.getDeliveryFailed()));
				recordElements.add(record.getDeliveryFailedReason());

				csvPrinter.printRecord(recordElements.toArray(new Object[0]));
			}

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void runAdminAnalyticsAccountOnboardingIncompleteReportCsv(@Nonnull InstitutionId institutionId,
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

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		ZoneId institutionTimeZone = institution.getTimeZone() != null ? institution.getTimeZone() : reportTimeZone;

		Instant startInstant = startDateTime.atZone(institutionTimeZone).toInstant();
		Instant endInstant = endDateTime.atZone(institutionTimeZone).toInstant();

		List<AdminAnalyticsAccountOnboardingIncompleteReportRecord> records = getDatabase().queryForList("""
						SELECT
							a.account_id,
							a.created AS account_created_at,
							a.email_address,
							a.first_name,
							a.last_name,
							a.role_id,
							a.account_source_id,
							i.onboarding_screening_flow_id,
							ss.screening_session_id,
							ss.created AS screening_session_created_at,
							ss.completed AS screening_session_completed,
							ss.completed_at AS screening_session_completed_at,
							ss.skipped AS screening_session_skipped,
							ss.skipped_at AS screening_session_skipped_at,
							sss.screening_session_screening_id,
							s.name AS screening_name,
							sq.question_text,
							sao.answer_option_text,
							sa.text AS answer_freeform_text,
							sao.score AS answer_score,
							sa.created AS answer_created_at
						FROM account a
						JOIN institution i
							ON a.institution_id = i.institution_id
						LEFT JOIN screening_session ss
							ON ss.target_account_id = a.account_id
							AND ss.screening_flow_version_id IN (
								SELECT screening_flow_version_id
								FROM screening_flow_version
								WHERE screening_flow_id = i.onboarding_screening_flow_id
							)
						LEFT JOIN v_screening_session_screening sss
							ON sss.screening_session_id = ss.screening_session_id
						LEFT JOIN v_screening_session_answered_screening_question ssasq
							ON ssasq.screening_session_screening_id = sss.screening_session_screening_id
						LEFT JOIN screening_question sq
							ON ssasq.screening_question_id = sq.screening_question_id
						LEFT JOIN v_screening_answer sa
							ON sa.screening_session_answered_screening_question_id = ssasq.screening_session_answered_screening_question_id
						LEFT JOIN screening_answer_option sao
							ON sa.screening_answer_option_id = sao.screening_answer_option_id
						LEFT JOIN screening_version sv
							ON sss.screening_version_id = sv.screening_version_id
						LEFT JOIN screening s
							ON sv.screening_id = s.screening_id
						WHERE a.institution_id = ?
							AND i.onboarding_screening_flow_id IS NOT NULL
							AND a.created >= ?
							AND a.created <= ?
							AND a.role_id = ?
							AND a.test_account = FALSE
							AND NOT EXISTS (
								SELECT 1
								FROM screening_session ss_completed
								JOIN screening_flow_version sfv_completed
									ON sfv_completed.screening_flow_version_id = ss_completed.screening_flow_version_id
								WHERE ss_completed.target_account_id = a.account_id
									AND sfv_completed.screening_flow_id = i.onboarding_screening_flow_id
									AND ss_completed.completed = TRUE
							)
						ORDER BY a.created, ss.created, sa.created
						""", AdminAnalyticsAccountOnboardingIncompleteReportRecord.class, institutionId, startInstant, endInstant,
				RoleId.PATIENT);

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
				.withZone(institutionTimeZone)
				.withLocale(reportLocale);

		List<String> headerColumns = List.of(
				getStrings().get("Account ID"),
				getStrings().get("Account Created At ({{timeZone}})", Map.of("timeZone", institutionTimeZone.getId())),
				getStrings().get("Email Address"),
				getStrings().get("First Name"),
				getStrings().get("Last Name"),
				getStrings().get("Role ID"),
				getStrings().get("Account Source ID"),
				getStrings().get("Onboarding Screening Flow ID"),
				getStrings().get("Screening Session ID"),
				getStrings().get("Screening Session Created At ({{timeZone}})", Map.of("timeZone", institutionTimeZone.getId())),
				getStrings().get("Screening Session Completed"),
				getStrings().get("Screening Session Completed At ({{timeZone}})", Map.of("timeZone", institutionTimeZone.getId())),
				getStrings().get("Screening Session Skipped"),
				getStrings().get("Screening Session Skipped At ({{timeZone}})", Map.of("timeZone", institutionTimeZone.getId())),
				getStrings().get("Screening Session Screening ID"),
				getStrings().get("Screening Name"),
				getStrings().get("Question"),
				getStrings().get("Answer Option"),
				getStrings().get("Answer Freeform Text"),
				getStrings().get("Answer Score"),
				getStrings().get("Answer Created At ({{timeZone}})", Map.of("timeZone", institutionTimeZone.getId()))
		);

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headerColumns.toArray(new String[0])))) {
			for (AdminAnalyticsAccountOnboardingIncompleteReportRecord record : records) {
				List<String> recordElements = new ArrayList<>(21);

				recordElements.add(record.getAccountId() == null ? "" : record.getAccountId().toString());
				recordElements.add(record.getAccountCreatedAt() == null ? "" : dateTimeFormatter.format(record.getAccountCreatedAt()));
				recordElements.add(record.getEmailAddress() == null ? "" : record.getEmailAddress());
				recordElements.add(obfuscateName(record.getFirstName()));
				recordElements.add(obfuscateName(record.getLastName()));
				recordElements.add(record.getRoleId());
				recordElements.add(record.getAccountSourceId() == null ? "" : record.getAccountSourceId().name());
				recordElements.add(record.getOnboardingScreeningFlowId() == null ? "" : record.getOnboardingScreeningFlowId().toString());
				recordElements.add(record.getScreeningSessionId() == null ? "" : record.getScreeningSessionId().toString());
				recordElements.add(record.getScreeningSessionCreatedAt() == null ? "" : dateTimeFormatter.format(record.getScreeningSessionCreatedAt()));
				recordElements.add(record.getScreeningSessionCompleted() == null ? "" : record.getScreeningSessionCompleted().toString());
				recordElements.add(record.getScreeningSessionCompletedAt() == null ? "" : dateTimeFormatter.format(record.getScreeningSessionCompletedAt()));
				recordElements.add(record.getScreeningSessionSkipped() == null ? "" : record.getScreeningSessionSkipped().toString());
				recordElements.add(record.getScreeningSessionSkippedAt() == null ? "" : dateTimeFormatter.format(record.getScreeningSessionSkippedAt()));
				recordElements.add(record.getScreeningSessionScreeningId() == null ? "" : record.getScreeningSessionScreeningId().toString());
				recordElements.add(record.getScreeningName());
				recordElements.add(record.getQuestionText());
				recordElements.add(record.getAnswerOptionText());
				recordElements.add(record.getAnswerFreeformText());
				recordElements.add(record.getAnswerScore() == null ? "" : record.getAnswerScore().toString());
				recordElements.add(record.getAnswerCreatedAt() == null ? "" : dateTimeFormatter.format(record.getAnswerCreatedAt()));

				csvPrinter.printRecord(recordElements.toArray(new Object[0]));
			}

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void runAdminAnalyticsAccountOnboardingCompleteReportCsv(@Nonnull InstitutionId institutionId,
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

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		ZoneId institutionTimeZone = institution.getTimeZone() != null ? institution.getTimeZone() : reportTimeZone;

		Instant startInstant = startDateTime.atZone(institutionTimeZone).toInstant();
		Instant endInstant = endDateTime.atZone(institutionTimeZone).toInstant();

		List<AdminAnalyticsAccountOnboardingCompleteReportRecord> records = getDatabase().queryForList("""
						SELECT
							a.account_id,
							a.created AS account_created_at,
							a.email_address,
							a.first_name,
							a.last_name,
							a.role_id,
							a.account_source_id,
							i.onboarding_screening_flow_id,
							ss.screening_session_id,
							ss.created AS screening_session_created_at,
							ss.completed AS screening_session_completed,
							ss.completed_at AS screening_session_completed_at,
							ss.skipped AS screening_session_skipped,
							ss.skipped_at AS screening_session_skipped_at,
							sss.screening_session_screening_id,
							s.name AS screening_name,
							sq.question_text,
							sao.answer_option_text,
							sa.text AS answer_freeform_text,
							sao.score AS answer_score,
							sa.created AS answer_created_at
						FROM account a
						JOIN institution i
							ON a.institution_id = i.institution_id
						JOIN screening_session ss
							ON ss.target_account_id = a.account_id
							AND ss.completed = TRUE
							AND ss.screening_flow_version_id IN (
								SELECT screening_flow_version_id
								FROM screening_flow_version
								WHERE screening_flow_id = i.onboarding_screening_flow_id
							)
						LEFT JOIN v_screening_session_screening sss
							ON sss.screening_session_id = ss.screening_session_id
						LEFT JOIN v_screening_session_answered_screening_question ssasq
							ON ssasq.screening_session_screening_id = sss.screening_session_screening_id
						LEFT JOIN screening_question sq
							ON ssasq.screening_question_id = sq.screening_question_id
						LEFT JOIN v_screening_answer sa
							ON sa.screening_session_answered_screening_question_id = ssasq.screening_session_answered_screening_question_id
						LEFT JOIN screening_answer_option sao
							ON sa.screening_answer_option_id = sao.screening_answer_option_id
						LEFT JOIN screening_version sv
							ON sss.screening_version_id = sv.screening_version_id
						LEFT JOIN screening s
							ON sv.screening_id = s.screening_id
						WHERE a.institution_id = ?
							AND i.onboarding_screening_flow_id IS NOT NULL
							AND a.created >= ?
							AND a.created <= ?
							AND a.role_id = ?
							AND a.test_account = FALSE
						ORDER BY a.created, ss.created, sa.created
						""", AdminAnalyticsAccountOnboardingCompleteReportRecord.class, institutionId, startInstant, endInstant,
				RoleId.PATIENT);

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
				.withZone(institutionTimeZone)
				.withLocale(reportLocale);

		List<String> headerColumns = List.of(
				getStrings().get("Account ID"),
				getStrings().get("Account Created At ({{timeZone}})", Map.of("timeZone", institutionTimeZone.getId())),
				getStrings().get("Email Address"),
				getStrings().get("First Name"),
				getStrings().get("Last Name"),
				getStrings().get("Role ID"),
				getStrings().get("Account Source ID"),
				getStrings().get("Onboarding Screening Flow ID"),
				getStrings().get("Screening Session ID"),
				getStrings().get("Screening Session Created At ({{timeZone}})", Map.of("timeZone", institutionTimeZone.getId())),
				getStrings().get("Screening Session Completed"),
				getStrings().get("Screening Session Completed At ({{timeZone}})", Map.of("timeZone", institutionTimeZone.getId())),
				getStrings().get("Screening Session Skipped"),
				getStrings().get("Screening Session Skipped At ({{timeZone}})", Map.of("timeZone", institutionTimeZone.getId())),
				getStrings().get("Screening Session Screening ID"),
				getStrings().get("Screening Name"),
				getStrings().get("Question"),
				getStrings().get("Answer Option"),
				getStrings().get("Answer Freeform Text"),
				getStrings().get("Answer Score"),
				getStrings().get("Answer Created At ({{timeZone}})", Map.of("timeZone", institutionTimeZone.getId()))
		);

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headerColumns.toArray(new String[0])))) {
			for (AdminAnalyticsAccountOnboardingCompleteReportRecord record : records) {
				List<String> recordElements = new ArrayList<>(21);

				recordElements.add(record.getAccountId() == null ? "" : record.getAccountId().toString());
				recordElements.add(record.getAccountCreatedAt() == null ? "" : dateTimeFormatter.format(record.getAccountCreatedAt()));
				recordElements.add(record.getEmailAddress() == null ? "" : record.getEmailAddress());
				recordElements.add(obfuscateName(record.getFirstName()));
				recordElements.add(obfuscateName(record.getLastName()));
				recordElements.add(record.getRoleId());
				recordElements.add(record.getAccountSourceId() == null ? "" : record.getAccountSourceId().name());
				recordElements.add(record.getOnboardingScreeningFlowId() == null ? "" : record.getOnboardingScreeningFlowId().toString());
				recordElements.add(record.getScreeningSessionId() == null ? "" : record.getScreeningSessionId().toString());
				recordElements.add(record.getScreeningSessionCreatedAt() == null ? "" : dateTimeFormatter.format(record.getScreeningSessionCreatedAt()));
				recordElements.add(record.getScreeningSessionCompleted() == null ? "" : record.getScreeningSessionCompleted().toString());
				recordElements.add(record.getScreeningSessionCompletedAt() == null ? "" : dateTimeFormatter.format(record.getScreeningSessionCompletedAt()));
				recordElements.add(record.getScreeningSessionSkipped() == null ? "" : record.getScreeningSessionSkipped().toString());
				recordElements.add(record.getScreeningSessionSkippedAt() == null ? "" : dateTimeFormatter.format(record.getScreeningSessionSkippedAt()));
				recordElements.add(record.getScreeningSessionScreeningId() == null ? "" : record.getScreeningSessionScreeningId().toString());
				recordElements.add(record.getScreeningName());
				recordElements.add(record.getQuestionText());
				recordElements.add(record.getAnswerOptionText());
				recordElements.add(record.getAnswerFreeformText());
				recordElements.add(record.getAnswerScore() == null ? "" : record.getAnswerScore().toString());
				recordElements.add(record.getAnswerCreatedAt() == null ? "" : dateTimeFormatter.format(record.getAnswerCreatedAt()));

				csvPrinter.printRecord(recordElements.toArray(new Object[0]));
			}

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	private static final List<String> ACCOUNT_GEOLOCATION_HEADER_COLUMNS = List.of(
			"account_id",
			"email_address",
			"account_created_at",
			"role_id",
			"ip_address",
			"first_analytics_event_at",
			"last_analytics_event_at",
			"ip_geolocation_status_id",
			"ip_type",
			"continent_code",
			"continent_name",
			"country_code",
			"country_name",
			"region_code",
			"region_name",
			"city",
			"postal_code",
			"latitude",
			"longitude",
			"msa",
			"dma",
			"radius",
			"ip_routing_type",
			"connection_type",
			"connection_asn",
			"connection_isp",
			"connection_organization_type",
			"connection_home",
			"hostname",
			"time_zone_id",
			"time_zone_gmt_offset",
			"time_zone_code",
			"location_geoname_id",
			"location_is_eu",
			"provider_error_code",
			"provider_error_type",
			"provider_error_message",
			"last_lookup_attempted_at",
			"last_lookup_succeeded_at"
	);

	public void runAccountGeolocationReportCsv(@Nonnull InstitutionId institutionId,
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

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		ZoneId institutionTimeZone = institution.getTimeZone() != null ? institution.getTimeZone() : reportTimeZone;

		Instant startInstant = startDateTime.atZone(institutionTimeZone).toInstant();
		Instant endInstant = endDateTime.atZone(institutionTimeZone).toInstant();

		List<AccountGeolocationReportRecord> records = getDatabase().queryForList("""
						WITH event_ip_aggregates AS (
							SELECT
								ane.account_id,
								ane.ip_address AS ip_address_inet,
								host(ane.ip_address) AS ip_address,
								MIN(ane.timestamp) AS first_analytics_event_at,
								MAX(ane.timestamp) AS last_analytics_event_at
							FROM analytics_native_event ane
							WHERE ane.institution_id = ?
								AND ane.timestamp >= ?
								AND ane.timestamp <= ?
								AND ane.account_id IS NOT NULL
								AND ane.ip_address IS NOT NULL
							GROUP BY ane.account_id, ane.ip_address
						)
						SELECT
							a.account_id,
							a.email_address,
							a.created AS account_created_at,
							a.role_id,
							eia.ip_address,
							eia.first_analytics_event_at,
							eia.last_analytics_event_at,
							COALESCE(ipg.ip_geolocation_status_id, 'PENDING') AS ip_geolocation_status_id,
							ipg.ip_type,
							ipg.continent_code,
							ipg.continent_name,
							ipg.country_code,
							ipg.country_name,
							ipg.region_code,
							ipg.region_name,
							ipg.city,
							ipg.postal_code,
							ipg.latitude,
							ipg.longitude,
							ipg.msa,
							ipg.dma,
							ipg.radius,
							ipg.ip_routing_type,
							ipg.connection_type,
							ipg.connection_asn,
							ipg.connection_isp,
							ipg.connection_organization_type,
							ipg.connection_home,
							ipg.hostname,
							ipg.time_zone_id,
							ipg.time_zone_gmt_offset,
							ipg.time_zone_code,
							ipg.location_geoname_id,
							ipg.location_is_eu,
							ipg.provider_error_code,
							ipg.provider_error_type,
							ipg.provider_error_message,
							ipg.last_lookup_attempted_at,
							ipg.last_lookup_succeeded_at
						FROM event_ip_aggregates eia
						JOIN account a
							ON a.account_id = eia.account_id
							AND a.institution_id = ?
						LEFT JOIN ip_geolocation ipg
							ON ipg.ip_address = eia.ip_address_inet
						ORDER BY eia.last_analytics_event_at DESC, a.created DESC, a.account_id, eia.ip_address
						""", AccountGeolocationReportRecord.class, institutionId, startInstant, endInstant, institutionId);

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
				.withZone(institutionTimeZone)
				.withLocale(reportLocale);

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(ACCOUNT_GEOLOCATION_HEADER_COLUMNS.toArray(new String[0])))) {
			for (AccountGeolocationReportRecord record : records) {
				List<String> recordElements = new ArrayList<>(ACCOUNT_GEOLOCATION_HEADER_COLUMNS.size());

				recordElements.add(record.getAccountId() == null ? "" : record.getAccountId().toString());
				recordElements.add(record.getEmailAddress());
				recordElements.add(record.getAccountCreatedAt() == null ? "" : dateTimeFormatter.format(record.getAccountCreatedAt()));
				recordElements.add(record.getRoleId() == null ? "" : record.getRoleId().name());
				recordElements.add(record.getIpAddress());
				recordElements.add(record.getFirstAnalyticsEventAt() == null ? "" : dateTimeFormatter.format(record.getFirstAnalyticsEventAt()));
				recordElements.add(record.getLastAnalyticsEventAt() == null ? "" : dateTimeFormatter.format(record.getLastAnalyticsEventAt()));
				recordElements.add(record.getIpGeolocationStatusId());
				recordElements.add(record.getIpType());
				recordElements.add(record.getContinentCode());
				recordElements.add(record.getContinentName());
				recordElements.add(record.getCountryCode());
				recordElements.add(record.getCountryName());
				recordElements.add(record.getRegionCode());
				recordElements.add(record.getRegionName());
				recordElements.add(record.getCity());
				recordElements.add(record.getPostalCode());
				recordElements.add(record.getLatitude() == null ? "" : record.getLatitude().toString());
				recordElements.add(record.getLongitude() == null ? "" : record.getLongitude().toString());
				recordElements.add(record.getMsa());
				recordElements.add(record.getDma());
				recordElements.add(record.getRadius() == null ? "" : record.getRadius().toString());
				recordElements.add(record.getIpRoutingType());
				recordElements.add(record.getConnectionType());
				recordElements.add(record.getConnectionAsn() == null ? "" : record.getConnectionAsn().toString());
				recordElements.add(record.getConnectionIsp());
				recordElements.add(record.getConnectionOrganizationType());
				recordElements.add(record.getConnectionHome() == null ? "" : record.getConnectionHome().toString());
				recordElements.add(record.getHostname());
				recordElements.add(record.getTimeZoneId());
				recordElements.add(record.getTimeZoneGmtOffset() == null ? "" : record.getTimeZoneGmtOffset().toString());
				recordElements.add(record.getTimeZoneCode());
				recordElements.add(record.getLocationGeonameId() == null ? "" : record.getLocationGeonameId().toString());
				recordElements.add(record.getLocationIsEu() == null ? "" : record.getLocationIsEu().toString());
				recordElements.add(record.getProviderErrorCode() == null ? "" : record.getProviderErrorCode().toString());
				recordElements.add(record.getProviderErrorType());
				recordElements.add(record.getProviderErrorMessage());
				recordElements.add(record.getLastLookupAttemptedAt() == null ? "" : dateTimeFormatter.format(record.getLastLookupAttemptedAt()));
				recordElements.add(record.getLastLookupSucceededAt() == null ? "" : dateTimeFormatter.format(record.getLastLookupSucceededAt()));

				csvPrinter.printRecord(recordElements.toArray(new Object[0]));
			}

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	private static final String METRIC_COMPLETE_COLUMN_SUFFIX = "_complete";
	@Nonnull
	private static final String METRIC_TIME_COLUMN_SUFFIX = "_time";
	@Nonnull
	private static final String METRIC_VISIT_COLUMN_SUFFIX = "_visit";

	@Nonnull
	private static final List<String> COURSE_MCB_DOWNLOAD_HEADER_COLUMNS = List.of(
			"recordID",
			"email",
			"bb_email_entered_date",
			"bb_email_verified_date",
			"bb_zipcode",
			"bb_referrer",
			"bb_onboarding_1",
			"bb_onboarding_2",
			"bb_onboarding_2a",
			"bb_onboarding_2b",
			"bb_onboarding_3",
			"bb_onboarding_4",
			"bb_onboarding_5",
			"bb_onboarding_6",
			"bb_onboarding_7",
			"bb_onboarding_8",
			"bb_n_sitevisit",
			"bb_tot_time",
			"bb_sleep_complete",
			"bb_sleep_time",
			"bb_sleep_visit",
			"bb_trauma_complete",
			"bb_trauma_time",
			"bb_trauma_visit",
			"bb_teens_complete",
			"bb_teens_time",
			"bb_teens_visit",
			"bb_mcb_complete",
			"bb_mcb_time",
			"bb_mcb_visit",
			"bb_mcb_precourse_1",
			"bb_mcb_precourse_2",
			"bb_mcb_precourse_3",
			"bb_mcb_precourse_4",
			"bb_mcb_precourse_4a",
			"bb_mcb_precourse_5",
			"bb_mcb_precourse_6",
			"bb_mcb_precourse_7",
			"bb_mcb_adhd_track",
			"bb_mcb_adhdintro_video_complete",
			"bb_mcb_adhdintro_video_time",
			"bb_mcb_adhdintro_video_visit",
			"bb_mcb_adhdbasics_activity_complete",
			"bb_mcb_adhdbasics_activity_time",
			"bb_mcb_adhdbasics_activity_visit",
			"bb_mcb_treatingadhd_video_complete",
			"bb_mcb_treatingadhd_video_time",
			"bb_mcb_treatingadhd_video_visit",
			"bb_mcb_effectinter_info_complete",
			"bb_mcb_effectinter_info _time",
			"bb_mcb_effectinter_info_visit",
			"bb_mcb_adhdtx_activity_complete",
			"bb_mcb_adhdtx_activity_time",
			"bb_mcb_adhdtx_activity_visit",
			"bb_mcb_buildadhd_video_complete",
			"bb_mcb_buildadhd_video_time",
			"bb_mcb_buildadhd_video_visit",
			"bb_mcb_adhdteam_info_complete",
			"bb_mcb_adhdteam_info_time",
			"bb_mcb_adhdteam_info_visit",
			"bb_mcb_adhd_handouts_info_complete",
			"bb_mcb_adhd_handouts_info_time",
			"bb_mcb_adhd_handouts_info_visit",
			"bb_mcb_workingdoc_video_complete",
			"bb_mcb_workingdoc_video_time",
			"bb_mcb_workingdoc_video_visit",
			"bb_mcb_workingschool_video_complete",
			"bb_mcb_workingschool_video_time",
			"bb_mcb_workingschool_video_visit",
			"bb_mcb_iep504_video_complete",
			"bb_mcb_iep504_video_time",
			"bb_mcb_iep504_video_visit",
			"bb_mcb_reportcard_video_complete",
			"bb_mcb_reportcard_video_time",
			"bb_mcb_reportcard_video_visit",
			"bb_mcb_intro_video_complete",
			"bb_mcb_intro_video_time",
			"bb_mcb_intro_video_visit",
			"bb_mcb_intro_info_complete",
			"bb_mcb_intro_info_time",
			"bb_mcb_intro_info_visit",
			"bb_mcb_identifyingok_complete",
			"bb_mcb_identifyingok_time",
			"bb_mcb_identifyingok_visit",
			"bb_mcb_okworksheet_complete",
			"bb_mcb_okworksheet_time",
			"bb_mcb_okworksheet_visit",
			"bb_mcb_attending_video_complete",
			"bb_mcb_attending_video_time",
			"bb_mcb_attending_video_visit",
			"bb_mcb_attending_activity_complete",
			"bb_mcb_attending_activity_time",
			"bb_mcb_attending_activity_visit",
			"bb_mcb_praise_video_complete",
			"bb_mcb_praise_video_time",
			"bb_mcb_praise_video_visit",
			"bb_mcb_praise_info_complete",
			"bb_mcb_praise_info_time",
			"bb_mcb_praise_info_visit",
			"bb_mcb_childsgame_video_complete",
			"bb_mcb_childsgame_video_time",
			"bb_mcb_childsgame_video_visit",
			"bb_mcb_idstatements_activity_complete",
			"bb_mcb_idstatements_activity_time",
			"bb_mcb_idstatements_activity_visit",
			"bb_mcb_attendingreview_info_complete",
			"bb_mcb_attendingreview_info_time",
			"bb_mcb_attendingreview_info_visit",
			"bb_mcb_childstracking_info_complete",
			"bb_mcb_childstracking_info_time",
			"bb_mcb_childstracking_info_visit",
			"bb_mcb_ignoring_video_complete",
			"bb_mcb_ignoring_video_time",
			"bb_mcb_ignoring_video_visit",
			"bb_mcb_ignoring_info_complete",
			"bb_mcb_ignoring_info_time",
			"bb_mcb_ignoring_info_visit",
			"bb_mcb_ignoring_activity_complete",
			"bb_mcb_ignoring_activity_time",
			"bb_mcb_ignoring_activity_visit",
			"bb_mcb_ignoringpractice_info_complete",
			"bb_mcb_ignoringpractice_info_time",
			"bb_mcb_ignoringpractice_info_visit",
			"bb_mcb_instructions_video_complete",
			"bb_mcb_instructions_video_time",
			"bb_mcb_instructions_video_visit",
			"bb_mcb_instructions_info_complete",
			"bb_mcb_instructions_info_time",
			"bb_mcb_instructions_info_visit",
			"bb_mcb_instructions_activity_complete",
			"bb_mcb_instructions_activity_time",
			"bb_mcb_instructions_activity_visit",
			"bb_mcb_unclear_video_complete",
			"bb_mcb_unclear_video_time",
			"bb_mcb_unclear_video_visit",
			"bb_mcb_clear_video_complete",
			"bb_mcb_clear_video_time",
			"bb_mcb_clear_video_visit",
			"bb_mcb_practiceinstruct_complete",
			"bb_mcb_practiceinstruct_time",
			"bb_mcb_practiceinstruct_visit",
			"bb_mcb_timeout_video_complete",
			"bb_mcb_timeout_video_time",
			"bb_mcb_timeout_video_visit",
			"bb_mcb_timeout_info_complete",
			"bb_mcb_timeout_info_time",
			"bb_mcb_timeout_info_visit",
			"bb_mcb_timeout_activity_complete",
			"bb_mcb_timeout_activity_time",
			"bb_mcb_timeout_activity_visit",
			"bb_mcb_timeoutspots_activity_complete",
			"bb_mcb_timeoutspots_activity_time",
			"bb_mcb_timeoutspots_activity_visit",
			"bb_mcb_givingending_video_complete",
			"bb_mcb_givingending_video_time",
			"bb_mcb_givingending_video_visit",
			"bb_mcb_givingending_info_complete",
			"bb_mcb_givingending_info_time",
			"bb_mcb_givingending_info_visit",
			"bb_mcb_example1_video_complete",
			"bb_mcb_example1_video_time",
			"bb_mcb_example1_video_visit",
			"bb_mcb_timeouthw_info_complete",
			"bb_mcb_timeouthw_info_time",
			"bb_mcb_timeouthw_info_visit",
			"bb_mcb_houserules_video_complete",
			"bb_mcb_houserules_video_time",
			"bb_mcb_houserules_video_visit",
			"bb_mcb_houserules_info_complete",
			"bb_mcb_houserules_info_time",
			"bb_mcb_houserules_info_visit",
			"bb_mcb_problems_video_complete",
			"bb_mcb_problems_video_time",
			"bb_mcb_problems_video_visit",
			"bb_mcb_example2_video_complete",
			"bb_mcb_example2_video_time",
			"bb_mcb_example2_video_visit",
			"bb_mcb_emotions_video_complete",
			"bb_mcb_emotions_video_time",
			"bb_mcb_emotions_video_visit",
			"bb_mcb_extra_info_complete",
			"bb_mcb_extra_info_time",
			"bb_mcb_extra_info_visit",
			"bb_mcb_together_info_complete",
			"bb_mcb_together_info_time",
			"bb_mcb_together_info_visit",
			"bb_mcb_tei_1",
			"bb_mcb_tei_2",
			"bb_mcb_tei_3",
			"bb_mcb_tei_4",
			"bb_mcb_tei_5",
			"bb_mcb_tei_6",
			"bb_mcb_tei_7",
			"bb_mcb_tei_8",
			"bb_mcb_tei_9",
			"bb_mcb_postcourse_qual",
			"bb_mcb_handouts_info_complete",
			"bb_mcb_handouts_info_time",
			"bb_mcb_handouts_info_visit",
			"bb_mcb_additional_info_complete",
			"bb_mcb_additional_info_time",
			"bb_mcb_additional_info_visit"
	);

	@Nonnull
	private static final List<String> ACCOUNT_ONBOARDING_COMPLETE_V2_HEADER_COLUMNS = courseMcbDownloadHeaderColumnsThrough("bb_mcb_visit");

	@Nonnull
	private static List<String> courseMcbDownloadHeaderColumnsThrough(@Nonnull String headerColumn) {
		requireNonNull(headerColumn);

		int headerColumnIndex = COURSE_MCB_DOWNLOAD_HEADER_COLUMNS.indexOf(headerColumn);

		if (headerColumnIndex < 0)
			throw new IllegalStateException(format("Missing expected COURSE_MCB_DOWNLOAD header column '%s'", headerColumn));

		return List.copyOf(COURSE_MCB_DOWNLOAD_HEADER_COLUMNS.subList(0, headerColumnIndex + 1));
	}

	@Nonnull
	private static final List<String> ACCOUNT_TIMELINE_HEADER_COLUMNS = List.of(
			"occurred_at",
			"ended_at",
			"event_type_id",
			"actor_type_id",
			"summary",
			"page_view_type",
			"location_label",
			"dwell_time_seconds",
			"video_watched_seconds",
			"session_id",
			"screening_session_id",
			"course_session_id",
			"course_id",
			"course_title",
			"course_unit_id",
			"course_unit_title",
			"group_session_id",
			"source_table",
			"source_id",
			"details_json"
	);

	public void runAccountTimelineReportCsv(@Nonnull InstitutionId institutionId,
																					@Nonnull UUID accountId,
																					@Nullable LocalDateTime startDateTime,
																					@Nullable LocalDateTime endDateTime,
																					@Nonnull ZoneId reportTimeZone,
																					@Nonnull Locale reportLocale,
																					@Nonnull Writer writer) {
		requireNonNull(institutionId);
		requireNonNull(accountId);
		requireNonNull(reportTimeZone);
		requireNonNull(reportLocale);
		requireNonNull(writer);

		Account targetAccount = getAccountService().findAccountById(accountId)
				.orElseThrow(() -> new IllegalArgumentException(format("No account exists for ID %s", accountId)));

		if (!institutionId.equals(targetAccount.getInstitutionId()))
			throw new IllegalArgumentException(format("Account %s does not belong to institution %s", accountId, institutionId.name()));

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		ZoneId institutionTimeZone = institution.getTimeZone() != null ? institution.getTimeZone() : reportTimeZone;

		Instant startInstant = startDateTime == null ? null : startDateTime.atZone(institutionTimeZone).toInstant();
		Instant endInstant = endDateTime == null ? null : endDateTime.atZone(institutionTimeZone).toInstant();

		List<AccountTimelineReportRecord> records = getDatabase().queryForList("""
				WITH report_context AS (
					SELECT
						?::TEXT AS institution_id,
						?::UUID AS account_id,
						?::TIMESTAMPTZ AS start_at,
						?::TIMESTAMPTZ AS end_at
				),
				target_account AS (
					SELECT
						a.account_id,
						a.created AS account_created_at
					FROM account a
					JOIN report_context rc
						ON rc.account_id = a.account_id
					WHERE a.institution_id = rc.institution_id
				),
				account_created_events AS (
					SELECT
						ta.account_created_at AS occurred_at,
						NULL::TIMESTAMPTZ AS ended_at,
						'ACCOUNT_CREATED'::TEXT AS event_type_id,
						'SYSTEM'::TEXT AS actor_type_id,
						'Account created'::TEXT AS summary,
						NULL::TEXT AS page_view_type,
						'Account'::TEXT AS location_label,
						NULL::DOUBLE PRECISION AS dwell_time_seconds,
						NULL::DOUBLE PRECISION AS video_watched_seconds,
						NULL::UUID AS session_id,
						NULL::UUID AS screening_session_id,
						NULL::UUID AS course_session_id,
						NULL::UUID AS course_id,
						NULL::TEXT AS course_title,
						NULL::UUID AS course_unit_id,
						NULL::TEXT AS course_unit_title,
						NULL::UUID AS group_session_id,
						'account'::TEXT AS source_table,
						ta.account_id::TEXT AS source_id,
						jsonb_build_object('accountId', ta.account_id)::TEXT AS details_json
					FROM target_account ta
					JOIN report_context rc
						ON TRUE
					WHERE (rc.start_at IS NULL OR ta.account_created_at >= rc.start_at)
						AND (rc.end_at IS NULL OR ta.account_created_at <= rc.end_at)
				),
				session_events AS (
					SELECT
						ane.timestamp AS occurred_at,
						NULL::TIMESTAMPTZ AS ended_at,
						ane.analytics_native_event_type_id::TEXT AS event_type_id,
						'ACCOUNT'::TEXT AS actor_type_id,
						CASE ane.analytics_native_event_type_id
							WHEN 'SESSION_STARTED' THEN 'Session started'
							WHEN 'ACCOUNT_SIGNED_IN' THEN 'Account signed in'
							WHEN 'ACCOUNT_SIGNED_OUT' THEN 'Account signed out'
							ELSE INITCAP(REPLACE(ane.analytics_native_event_type_id::TEXT, '_', ' '))
						END AS summary,
						NULL::TEXT AS page_view_type,
						NULL::TEXT AS location_label,
						NULL::DOUBLE PRECISION AS dwell_time_seconds,
						NULL::DOUBLE PRECISION AS video_watched_seconds,
						ane.session_id,
						NULL::UUID AS screening_session_id,
						NULL::UUID AS course_session_id,
						NULL::UUID AS course_id,
						NULL::TEXT AS course_title,
						NULL::UUID AS course_unit_id,
						NULL::TEXT AS course_unit_title,
						NULL::UUID AS group_session_id,
						'analytics_native_event'::TEXT AS source_table,
						ane.analytics_native_event_id::TEXT AS source_id,
						COALESCE(ane.data, '{}'::JSONB)::TEXT AS details_json
					FROM analytics_native_event ane
					JOIN report_context rc
						ON rc.account_id = ane.account_id
					WHERE ane.institution_id = rc.institution_id
						AND (rc.start_at IS NULL OR ane.timestamp >= rc.start_at)
						AND (rc.end_at IS NULL OR ane.timestamp <= rc.end_at)
						AND ane.analytics_native_event_type_id IN ('SESSION_STARTED', 'ACCOUNT_SIGNED_IN', 'ACCOUNT_SIGNED_OUT')
				),
				dwell_page_events AS (
					SELECT
						mv.page_viewed_at AS occurred_at,
						mv.page_viewed_at + make_interval(secs => GREATEST(0, ROUND(mv.dwell_time_seconds))::INTEGER) AS ended_at,
						'PAGE_VIEW'::TEXT AS event_type_id,
						'ACCOUNT'::TEXT AS actor_type_id,
						CASE
							WHEN mv.page_view_type = 'PAGE_VIEW_COURSE_UNIT' AND cu.title IS NOT NULL THEN 'Viewed course unit page'
							ELSE INITCAP(REPLACE(mv.page_view_type::TEXT, '_', ' '))
						END AS summary,
						mv.page_view_type::TEXT AS page_view_type,
						CASE
							WHEN mv.page_view_type = 'PAGE_VIEW_COURSE_UNIT' THEN CONCAT_WS(' / ', c.title, cu.title)
							ELSE COALESCE(NULLIF(mv.page_view_data->>'title', ''), NULLIF(mv.page_view_data->>'name', ''), mv.page_view_type::TEXT)
						END AS location_label,
						mv.dwell_time_seconds,
						NULL::DOUBLE PRECISION AS video_watched_seconds,
						mv.session_id,
						NULL::UUID AS screening_session_id,
						NULL::UUID AS course_session_id,
						c.course_id,
						c.title AS course_title,
						cu.course_unit_id,
						cu.title AS course_unit_title,
						(mv.page_view_data->>'groupSessionId')::UUID AS group_session_id,
						'mv_analytics_dwell_time'::TEXT AS source_table,
						CONCAT(mv.session_id::TEXT, ':', mv.dwell_num::TEXT) AS source_id,
						jsonb_build_object(
							'pageViewType', mv.page_view_type,
							'pageViewData', COALESCE(mv.page_view_data, '{}'::JSONB)
						)::TEXT AS details_json
					FROM mv_analytics_dwell_time mv
					JOIN report_context rc
						ON rc.account_id = mv.account_id
					LEFT JOIN course_unit cu
						ON cu.course_unit_id = mv.course_unit_id
					LEFT JOIN course_module cm
						ON cm.course_module_id = cu.course_module_id
					LEFT JOIN course c
						ON c.course_id = cm.course_id
					WHERE mv.institution_id = rc.institution_id
						AND (rc.start_at IS NULL OR mv.page_viewed_at >= rc.start_at)
						AND (rc.end_at IS NULL OR mv.page_viewed_at <= rc.end_at)
				),
				clickthrough_events AS (
					SELECT
						ane.timestamp AS occurred_at,
						NULL::TIMESTAMPTZ AS ended_at,
						'CLICKTHROUGH'::TEXT AS event_type_id,
						'ACCOUNT'::TEXT AS actor_type_id,
						INITCAP(REPLACE(ane.analytics_native_event_type_id::TEXT, '_', ' ')) AS summary,
						NULL::TEXT AS page_view_type,
						COALESCE(NULLIF(ane.data->>'title', ''), NULLIF(ane.data->>'name', ''), NULLIF(ane.data->>'label', ''), ane.analytics_native_event_type_id::TEXT) AS location_label,
						NULL::DOUBLE PRECISION AS dwell_time_seconds,
						NULL::DOUBLE PRECISION AS video_watched_seconds,
						ane.session_id,
						NULL::UUID AS screening_session_id,
						NULL::UUID AS course_session_id,
						NULL::UUID AS course_id,
						NULL::TEXT AS course_title,
						NULL::UUID AS course_unit_id,
						NULL::TEXT AS course_unit_title,
						NULL::UUID AS group_session_id,
						'analytics_native_event'::TEXT AS source_table,
						ane.analytics_native_event_id::TEXT AS source_id,
						COALESCE(ane.data, '{}'::JSONB)::TEXT AS details_json
					FROM analytics_native_event ane
					JOIN report_context rc
						ON rc.account_id = ane.account_id
					WHERE ane.institution_id = rc.institution_id
						AND (rc.start_at IS NULL OR ane.timestamp >= rc.start_at)
						AND (rc.end_at IS NULL OR ane.timestamp <= rc.end_at)
						AND ane.analytics_native_event_type_id LIKE 'CLICKTHROUGH_%'
				),
				screening_started_events AS (
					SELECT
						ss.created AS occurred_at,
						NULL::TIMESTAMPTZ AS ended_at,
						'SCREENING_SESSION_STARTED'::TEXT AS event_type_id,
						CASE
							WHEN ss.created_by_account_id = ss.target_account_id THEN 'ACCOUNT'
							ELSE 'STAFF'
						END::TEXT AS actor_type_id,
						CONCAT('Started screening session: ', sf.name) AS summary,
						NULL::TEXT AS page_view_type,
						sf.name AS location_label,
						NULL::DOUBLE PRECISION AS dwell_time_seconds,
						NULL::DOUBLE PRECISION AS video_watched_seconds,
						NULL::UUID AS session_id,
						ss.screening_session_id,
						NULL::UUID AS course_session_id,
						NULL::UUID AS course_id,
						NULL::TEXT AS course_title,
						NULL::UUID AS course_unit_id,
						NULL::TEXT AS course_unit_title,
						ss.group_session_id,
						'screening_session'::TEXT AS source_table,
						CONCAT(ss.screening_session_id::TEXT, ':STARTED') AS source_id,
						jsonb_build_object(
							'screeningFlowId', sf.screening_flow_id,
							'screeningFlowVersionId', ss.screening_flow_version_id,
							'screeningFlowName', sf.name,
							'createdByAccountId', ss.created_by_account_id,
							'crisisIndicated', ss.crisis_indicated
						)::TEXT AS details_json
					FROM screening_session ss
					JOIN screening_flow_version sfv
						ON sfv.screening_flow_version_id = ss.screening_flow_version_id
					JOIN screening_flow sf
						ON sf.screening_flow_id = sfv.screening_flow_id
					JOIN report_context rc
						ON rc.account_id = ss.target_account_id
					WHERE sf.institution_id = rc.institution_id
						AND (rc.start_at IS NULL OR ss.created >= rc.start_at)
						AND (rc.end_at IS NULL OR ss.created <= rc.end_at)
				),
				screening_completed_events AS (
					SELECT
						ss.completed_at AS occurred_at,
						NULL::TIMESTAMPTZ AS ended_at,
						'SCREENING_SESSION_COMPLETED'::TEXT AS event_type_id,
						CASE
							WHEN ss.created_by_account_id = ss.target_account_id THEN 'ACCOUNT'
							ELSE 'STAFF'
						END::TEXT AS actor_type_id,
						CONCAT('Completed screening session: ', sf.name) AS summary,
						NULL::TEXT AS page_view_type,
						sf.name AS location_label,
						NULL::DOUBLE PRECISION AS dwell_time_seconds,
						NULL::DOUBLE PRECISION AS video_watched_seconds,
						NULL::UUID AS session_id,
						ss.screening_session_id,
						NULL::UUID AS course_session_id,
						NULL::UUID AS course_id,
						NULL::TEXT AS course_title,
						NULL::UUID AS course_unit_id,
						NULL::TEXT AS course_unit_title,
						ss.group_session_id,
						'screening_session'::TEXT AS source_table,
						CONCAT(ss.screening_session_id::TEXT, ':COMPLETED') AS source_id,
						jsonb_build_object(
							'screeningFlowId', sf.screening_flow_id,
							'screeningFlowVersionId', ss.screening_flow_version_id,
							'screeningFlowName', sf.name,
							'createdByAccountId', ss.created_by_account_id,
							'crisisIndicated', ss.crisis_indicated
						)::TEXT AS details_json
					FROM screening_session ss
					JOIN screening_flow_version sfv
						ON sfv.screening_flow_version_id = ss.screening_flow_version_id
					JOIN screening_flow sf
						ON sf.screening_flow_id = sfv.screening_flow_id
					JOIN report_context rc
						ON rc.account_id = ss.target_account_id
					WHERE sf.institution_id = rc.institution_id
						AND ss.completed = TRUE
						AND ss.completed_at IS NOT NULL
						AND (rc.start_at IS NULL OR ss.completed_at >= rc.start_at)
						AND (rc.end_at IS NULL OR ss.completed_at <= rc.end_at)
				),
				course_session_started_events AS (
					SELECT
						cs.created AS occurred_at,
						NULL::TIMESTAMPTZ AS ended_at,
						'COURSE_SESSION_STARTED'::TEXT AS event_type_id,
						'ACCOUNT'::TEXT AS actor_type_id,
						CONCAT('Started course: ', c.title) AS summary,
						NULL::TEXT AS page_view_type,
						c.title AS location_label,
						NULL::DOUBLE PRECISION AS dwell_time_seconds,
						NULL::DOUBLE PRECISION AS video_watched_seconds,
						NULL::UUID AS session_id,
						NULL::UUID AS screening_session_id,
						cs.course_session_id,
						c.course_id,
						c.title AS course_title,
						NULL::UUID AS course_unit_id,
						NULL::TEXT AS course_unit_title,
						NULL::UUID AS group_session_id,
						'course_session'::TEXT AS source_table,
						CONCAT(cs.course_session_id::TEXT, ':STARTED') AS source_id,
						jsonb_build_object(
							'courseSessionStatusId', cs.course_session_status_id
						)::TEXT AS details_json
					FROM course_session cs
					JOIN course c
						ON c.course_id = cs.course_id
					JOIN report_context rc
						ON rc.account_id = cs.account_id
					WHERE (rc.start_at IS NULL OR cs.created >= rc.start_at)
						AND (rc.end_at IS NULL OR cs.created <= rc.end_at)
				),
				video_playback_events AS (
					SELECT
						vr.first_event_at AS occurred_at,
						vr.last_event_at AS ended_at,
						'COURSE_UNIT_VIDEO_PLAYBACK'::TEXT AS event_type_id,
						'ACCOUNT'::TEXT AS actor_type_id,
						CONCAT('Video playback: ', COALESCE(cu.title, 'Course video')) AS summary,
						NULL::TEXT AS page_view_type,
						CONCAT_WS(' / ', c.title, cu.title) AS location_label,
						NULL::DOUBLE PRECISION AS dwell_time_seconds,
						vr.cumulative_watched_seconds AS video_watched_seconds,
						vr.session_id,
						NULL::UUID AS screening_session_id,
						vr.course_session_id,
						c.course_id,
						c.title AS course_title,
						cu.course_unit_id,
						cu.title AS course_unit_title,
						NULL::UUID AS group_session_id,
						'mv_analytics_course_unit_video_rollup'::TEXT AS source_table,
						vr.playback_stream_id AS source_id,
						jsonb_build_object(
							'maxPercentComplete', vr.max_percent_complete,
							'playStartEventCount', vr.play_start_event_count,
							'seekEventCount', vr.seek_event_count,
							'initializationErrorCount', vr.initialization_error_count
						)::TEXT AS details_json
					FROM mv_analytics_course_unit_video_rollup vr
					JOIN course_unit cu
						ON cu.course_unit_id = vr.course_unit_id
					JOIN course_module cm
						ON cm.course_module_id = cu.course_module_id
					JOIN course c
						ON c.course_id = cm.course_id
					JOIN report_context rc
						ON rc.account_id = vr.account_id
					WHERE vr.institution_id = rc.institution_id
						AND (rc.start_at IS NULL OR vr.first_event_at >= rc.start_at)
						AND (rc.end_at IS NULL OR vr.first_event_at <= rc.end_at)
				),
				course_unit_completed_events AS (
					SELECT
						csu.completed_at AS occurred_at,
						NULL::TIMESTAMPTZ AS ended_at,
						'COURSE_UNIT_COMPLETED'::TEXT AS event_type_id,
						'ACCOUNT'::TEXT AS actor_type_id,
						CONCAT('Completed course unit: ', cu.title) AS summary,
						NULL::TEXT AS page_view_type,
						CONCAT_WS(' / ', c.title, cu.title) AS location_label,
						NULL::DOUBLE PRECISION AS dwell_time_seconds,
						NULL::DOUBLE PRECISION AS video_watched_seconds,
						NULL::UUID AS session_id,
						NULL::UUID AS screening_session_id,
						cs.course_session_id,
						c.course_id,
						c.title AS course_title,
						cu.course_unit_id,
						cu.title AS course_unit_title,
						NULL::UUID AS group_session_id,
						'course_session_unit'::TEXT AS source_table,
						CONCAT(cs.course_session_id::TEXT, ':', cu.course_unit_id::TEXT, ':COMPLETED') AS source_id,
						jsonb_build_object(
							'courseSessionUnitStatusId', csu.course_session_unit_status_id,
							'completionMessage', csu.completion_message
						)::TEXT AS details_json
					FROM course_session_unit csu
					JOIN course_session cs
						ON cs.course_session_id = csu.course_session_id
					JOIN course_unit cu
						ON cu.course_unit_id = csu.course_unit_id
					JOIN course_module cm
						ON cm.course_module_id = cu.course_module_id
					JOIN course c
						ON c.course_id = cm.course_id
					JOIN report_context rc
						ON rc.account_id = cs.account_id
					WHERE csu.course_session_unit_status_id = 'COMPLETED'
						AND csu.completed_at IS NOT NULL
						AND (rc.start_at IS NULL OR csu.completed_at >= rc.start_at)
						AND (rc.end_at IS NULL OR csu.completed_at <= rc.end_at)
				),
				course_completed_events AS (
					SELECT
						cs.completed_at AS occurred_at,
						NULL::TIMESTAMPTZ AS ended_at,
						'COURSE_COMPLETED'::TEXT AS event_type_id,
						'ACCOUNT'::TEXT AS actor_type_id,
						CONCAT('Completed course: ', c.title) AS summary,
						NULL::TEXT AS page_view_type,
						c.title AS location_label,
						NULL::DOUBLE PRECISION AS dwell_time_seconds,
						NULL::DOUBLE PRECISION AS video_watched_seconds,
						NULL::UUID AS session_id,
						NULL::UUID AS screening_session_id,
						cs.course_session_id,
						c.course_id,
						c.title AS course_title,
						NULL::UUID AS course_unit_id,
						NULL::TEXT AS course_unit_title,
						NULL::UUID AS group_session_id,
						'course_session'::TEXT AS source_table,
						CONCAT(cs.course_session_id::TEXT, ':COMPLETED') AS source_id,
						jsonb_build_object(
							'courseSessionStatusId', cs.course_session_status_id
						)::TEXT AS details_json
					FROM course_session cs
					JOIN course c
						ON c.course_id = cs.course_id
					JOIN report_context rc
						ON rc.account_id = cs.account_id
					WHERE cs.course_session_status_id = 'COMPLETED'
						AND cs.completed_at IS NOT NULL
						AND (rc.start_at IS NULL OR cs.completed_at >= rc.start_at)
						AND (rc.end_at IS NULL OR cs.completed_at <= rc.end_at)
				),
				group_session_reservation_events AS (
					SELECT
						gsr.created AS occurred_at,
						NULL::TIMESTAMPTZ AS ended_at,
						'GROUP_SESSION_RESERVATION_CREATED'::TEXT AS event_type_id,
						'ACCOUNT'::TEXT AS actor_type_id,
						CONCAT('Reserved group session: ', COALESCE(vgs.title, 'Group Session')) AS summary,
						NULL::TEXT AS page_view_type,
						vgs.title AS location_label,
						NULL::DOUBLE PRECISION AS dwell_time_seconds,
						NULL::DOUBLE PRECISION AS video_watched_seconds,
						NULL::UUID AS session_id,
						NULL::UUID AS screening_session_id,
						NULL::UUID AS course_session_id,
						NULL::UUID AS course_id,
						NULL::TEXT AS course_title,
						NULL::UUID AS course_unit_id,
						NULL::TEXT AS course_unit_title,
						gsr.group_session_id,
						'group_session_reservation'::TEXT AS source_table,
						gsr.group_session_reservation_id::TEXT AS source_id,
						jsonb_build_object(
							'canceled', gsr.canceled,
							'groupSessionTitle', vgs.title
						)::TEXT AS details_json
					FROM group_session_reservation gsr
					JOIN v_group_session vgs
						ON vgs.group_session_id = gsr.group_session_id
					JOIN report_context rc
						ON rc.account_id = gsr.account_id
					WHERE vgs.institution_id = rc.institution_id
						AND (rc.start_at IS NULL OR gsr.created >= rc.start_at)
						AND (rc.end_at IS NULL OR gsr.created <= rc.end_at)
				)
				SELECT *
				FROM (
					SELECT * FROM account_created_events
					UNION ALL
					SELECT * FROM session_events
					UNION ALL
					SELECT * FROM dwell_page_events
					UNION ALL
					SELECT * FROM clickthrough_events
					UNION ALL
					SELECT * FROM screening_started_events
					UNION ALL
					SELECT * FROM screening_completed_events
					UNION ALL
					SELECT * FROM course_session_started_events
					UNION ALL
					SELECT * FROM video_playback_events
					UNION ALL
					SELECT * FROM course_unit_completed_events
					UNION ALL
					SELECT * FROM course_completed_events
					UNION ALL
					SELECT * FROM group_session_reservation_events
				) timeline
				ORDER BY occurred_at, event_type_id, source_id
				""", AccountTimelineReportRecord.class, institutionId, accountId, startInstant, endInstant);

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
				.withZone(institutionTimeZone)
				.withLocale(reportLocale);

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(ACCOUNT_TIMELINE_HEADER_COLUMNS.toArray(new String[0])))) {
			for (AccountTimelineReportRecord record : records) {
				List<String> recordElements = new ArrayList<>(ACCOUNT_TIMELINE_HEADER_COLUMNS.size());

				recordElements.add(record.getOccurredAt() == null ? "" : dateTimeFormatter.format(record.getOccurredAt()));
				recordElements.add(record.getEndedAt() == null ? "" : dateTimeFormatter.format(record.getEndedAt()));
				recordElements.add(record.getEventTypeId() == null ? "" : record.getEventTypeId().name());
				recordElements.add(record.getActorTypeId() == null ? "" : record.getActorTypeId().name());
				recordElements.add(record.getSummary() == null ? "" : record.getSummary());
				recordElements.add(record.getPageViewType() == null ? "" : record.getPageViewType());
				recordElements.add(record.getLocationLabel() == null ? "" : record.getLocationLabel());
				recordElements.add(record.getDwellTimeSeconds() == null ? "" : Long.toString(Math.max(0, Math.round(record.getDwellTimeSeconds()))));
				recordElements.add(record.getVideoWatchedSeconds() == null ? "" : Long.toString(Math.max(0, Math.round(record.getVideoWatchedSeconds()))));
				recordElements.add(record.getSessionId() == null ? "" : record.getSessionId().toString());
				recordElements.add(record.getScreeningSessionId() == null ? "" : record.getScreeningSessionId().toString());
				recordElements.add(record.getCourseSessionId() == null ? "" : record.getCourseSessionId().toString());
				recordElements.add(record.getCourseId() == null ? "" : record.getCourseId().toString());
				recordElements.add(record.getCourseTitle() == null ? "" : record.getCourseTitle());
				recordElements.add(record.getCourseUnitId() == null ? "" : record.getCourseUnitId().toString());
				recordElements.add(record.getCourseUnitTitle() == null ? "" : record.getCourseUnitTitle());
				recordElements.add(record.getGroupSessionId() == null ? "" : record.getGroupSessionId().toString());
				recordElements.add(record.getSourceTable() == null ? "" : record.getSourceTable());
				recordElements.add(record.getSourceId() == null ? "" : record.getSourceId());
				recordElements.add(record.getDetailsJson() == null ? "" : record.getDetailsJson());

				csvPrinter.printRecord(recordElements.toArray(new Object[0]));
			}

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void runMcbDownloadReportCsv(@Nonnull InstitutionId institutionId,
																			@Nonnull LocalDateTime startDateTime,
																			@Nonnull LocalDateTime endDateTime,
																			@Nonnull ZoneId reportTimeZone,
																									@Nonnull Locale reportLocale,
																									@Nonnull Writer writer) {
		runCourseMcbDownloadStyleReportCsv(institutionId, startDateTime, endDateTime, reportTimeZone, reportLocale, writer,
				COURSE_MCB_DOWNLOAD_HEADER_COLUMNS);
	}

	public void runAccountOnboardingCompleteV2ReportCsv(@Nonnull InstitutionId institutionId,
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

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		ZoneId institutionTimeZone = institution.getTimeZone() != null ? institution.getTimeZone() : reportTimeZone;

		Instant startInstant = startDateTime.atZone(institutionTimeZone).toInstant();
		Instant endInstant = endDateTime.atZone(institutionTimeZone).toInstant();

		List<CourseMcbDownloadReportRecord> records = getDatabase().queryForList("""
						WITH institution_onboarding AS (
							SELECT onboarding_screening_flow_id
							FROM institution
							WHERE institution_id = ?
						),
						report_window AS (
							SELECT
								?::TIMESTAMPTZ AS report_start_at,
								?::TIMESTAMPTZ AS report_end_at
						),
						target_course_keys AS (
							SELECT UNNEST(ARRAY[
								'bb_sleep',
								'bb_trauma',
								'bb_teens',
								'bb_mcb'
							]) AS reporting_key
						),
						report_accounts AS (
							SELECT
								a.account_id,
								a.created AS account_created_at,
								a.email_address,
								a.metadata
							FROM account a
							JOIN report_window rw
								ON TRUE
							JOIN institution_onboarding io
								ON io.onboarding_screening_flow_id IS NOT NULL
							WHERE a.institution_id = ?
								AND a.created >= rw.report_start_at
								AND a.created <= rw.report_end_at
								AND a.role_id = ?
								AND a.test_account = FALSE
								AND EXISTS (
									SELECT 1
									FROM screening_session ss_completed
									JOIN screening_flow_version sfv_completed
										ON sfv_completed.screening_flow_version_id = ss_completed.screening_flow_version_id
									WHERE ss_completed.target_account_id = a.account_id
										AND sfv_completed.screening_flow_id = io.onboarding_screening_flow_id
										AND ss_completed.completed = TRUE
								)
						),
						account_analytics_metrics AS MATERIALIZED (
							SELECT
								ra.account_id,
								COUNT(DISTINCT mv.session_id)::BIGINT AS bb_n_sitevisit,
								COALESCE(SUM(mv.dwell_time_seconds), 0)::DOUBLE PRECISION AS bb_tot_time_seconds
							FROM report_accounts ra
							JOIN report_window rw
								ON TRUE
							LEFT JOIN mv_analytics_dwell_time mv
								ON mv.institution_id = ?
								AND mv.account_id = ra.account_id
								AND mv.page_viewed_at >= ra.account_created_at
								AND mv.page_viewed_at <= rw.report_end_at
							GROUP BY ra.account_id
						),
						account_email_metrics AS MATERIALIZED (
							SELECT
								ra.account_id,
								first_invite.email_entered_at,
								first_claimed.email_verified_at
							FROM report_accounts ra
							JOIN report_window rw
								ON TRUE
							LEFT JOIN LATERAL (
								SELECT ai.created AS email_entered_at
								FROM account_invite ai
								WHERE ai.institution_id = ?
									AND LOWER(ai.email_address) = LOWER(ra.email_address)
									AND ai.created <= rw.report_end_at
								ORDER BY ai.created
								LIMIT 1
							) first_invite ON TRUE
							LEFT JOIN LATERAL (
								SELECT ai_claimed.last_updated AS email_verified_at
								FROM account_invite ai_claimed
								WHERE ai_claimed.institution_id = ?
									AND LOWER(ai_claimed.email_address) = LOWER(ra.email_address)
									AND ai_claimed.claimed = TRUE
									AND ai_claimed.last_updated <= rw.report_end_at
								ORDER BY ai_claimed.last_updated
								LIMIT 1
							) first_claimed ON TRUE
						),
						account_referrer AS MATERIALIZED (
							SELECT
								ra.account_id,
								first_referrer.bb_referrer
							FROM report_accounts ra
							JOIN report_window rw
								ON TRUE
							LEFT JOIN LATERAL (
								SELECT
									COALESCE(
										NULLIF(LOWER(SPLIT_PART(REGEXP_REPLACE(COALESCE(ane.data->>'referringUrl', ''), '^https?://', ''), '/', 1)), ''),
										NULLIF(LOWER(ane.referring_campaign), '')
									) AS bb_referrer
								FROM analytics_native_event ane
								WHERE ane.institution_id = ?
									AND ane.account_id = ra.account_id
									AND ane.timestamp >= ra.account_created_at
									AND ane.timestamp <= rw.report_end_at
									AND (
										NULLIF(ane.data->>'referringUrl', '') IS NOT NULL
										OR NULLIF(ane.referring_campaign, '') IS NOT NULL
									)
								ORDER BY ane.timestamp
								LIMIT 1
							) first_referrer ON TRUE
						),
						screening_question_answers AS (
							SELECT
								ss.target_account_id AS account_id,
								NULLIF(REGEXP_REPLACE(sq.metadata->'reporting'->>'key', '\\s+', '', 'g'), '') AS reporting_key,
								ssasq.screening_session_answered_screening_question_id,
								COALESCE(MAX(sa.created), MAX(ss.created)) AS answered_at,
								STRING_AGG(
									COALESCE(NULLIF(sa.text, ''), NULLIF(sao.answer_option_text, ''), sao.display_order::TEXT, sao.score::TEXT),
									',' ORDER BY sa.answer_order
								) AS reporting_value
							FROM screening_session ss
							JOIN report_accounts ra
								ON ra.account_id = ss.target_account_id
							JOIN report_window rw
								ON TRUE
							JOIN institution_onboarding io
								ON io.onboarding_screening_flow_id IS NOT NULL
							JOIN screening_flow_version sfv
								ON sfv.screening_flow_version_id = ss.screening_flow_version_id
								AND sfv.screening_flow_id = io.onboarding_screening_flow_id
							JOIN v_screening_session_screening sss
								ON sss.screening_session_id = ss.screening_session_id
							JOIN v_screening_session_answered_screening_question ssasq
								ON ssasq.screening_session_screening_id = sss.screening_session_screening_id
							JOIN screening_question sq
								ON sq.screening_question_id = ssasq.screening_question_id
							JOIN v_screening_answer sa
								ON sa.screening_session_answered_screening_question_id = ssasq.screening_session_answered_screening_question_id
							LEFT JOIN screening_answer_option sao
								ON sao.screening_answer_option_id = sa.screening_answer_option_id
							WHERE sq.metadata IS NOT NULL
								AND ss.created >= ra.account_created_at
								AND ss.created <= rw.report_end_at
								AND NULLIF(REGEXP_REPLACE(sq.metadata->'reporting'->>'key', '\\s+', '', 'g'), '') LIKE 'bb_onboarding_%'
							GROUP BY 1, 2, 3
						),
						latest_screening_values AS (
							SELECT DISTINCT ON (account_id, reporting_key)
								account_id,
								reporting_key,
								reporting_value
							FROM screening_question_answers
							ORDER BY account_id, reporting_key, answered_at DESC, screening_session_answered_screening_question_id DESC
						),
						account_screening_values AS MATERIALIZED (
							SELECT
								account_id,
								COALESCE(jsonb_object_agg(reporting_key, reporting_value), '{}'::jsonb) AS screening_values_json
							FROM latest_screening_values
							GROUP BY account_id
						),
						account_course_completions AS (
							SELECT
								ra.account_id,
								NULLIF(REGEXP_REPLACE(c.reporting_key, '\\s+', '', 'g'), '') AS reporting_key,
								CASE WHEN BOOL_OR(cs.course_session_status_id = 'COMPLETED' AND cs.completed_at <= rw.report_end_at) THEN 1 ELSE 0 END AS complete_value
							FROM report_accounts ra
							JOIN report_window rw
								ON TRUE
							JOIN course_session cs
								ON cs.account_id = ra.account_id
								AND cs.created <= rw.report_end_at
							JOIN course c
								ON c.course_id = cs.course_id
							JOIN target_course_keys tck
								ON tck.reporting_key = NULLIF(REGEXP_REPLACE(c.reporting_key, '\\s+', '', 'g'), '')
							GROUP BY 1, 2
						),
						account_course_page_metrics AS (
							SELECT
								ra.account_id,
								NULLIF(REGEXP_REPLACE(c.reporting_key, '\\s+', '', 'g'), '') AS reporting_key,
								0 AS complete_value,
								COALESCE(SUM(
									CASE
										WHEN cu.course_unit_type_id <> 'VIDEO' THEN mv.dwell_time_seconds
										ELSE 0
									END
								), 0)::DOUBLE PRECISION AS time_seconds,
								COUNT(*)::BIGINT AS visit_count
							FROM report_accounts ra
							JOIN report_window rw
								ON TRUE
							JOIN mv_analytics_dwell_time mv
								ON mv.institution_id = ?
								AND mv.account_id = ra.account_id
								AND mv.page_view_type = 'PAGE_VIEW_COURSE_UNIT'
								AND mv.course_unit_id IS NOT NULL
								AND mv.page_viewed_at >= ra.account_created_at
								AND mv.page_viewed_at <= rw.report_end_at
							JOIN course_unit cu
								ON cu.course_unit_id = mv.course_unit_id
							JOIN course_module cm
								ON cm.course_module_id = cu.course_module_id
							JOIN course c
								ON c.course_id = cm.course_id
							JOIN target_course_keys tck
								ON tck.reporting_key = NULLIF(REGEXP_REPLACE(c.reporting_key, '\\s+', '', 'g'), '')
							GROUP BY 1, 2
						),
						account_video_course_time_rows AS (
							SELECT
								ra.account_id,
								NULLIF(REGEXP_REPLACE(c.reporting_key, '\\s+', '', 'g'), '') AS reporting_key,
								0 AS complete_value,
								COALESCE(SUM(vr.cumulative_watched_seconds), 0)::DOUBLE PRECISION AS time_seconds,
								0::BIGINT AS visit_count
							FROM report_accounts ra
							JOIN report_window rw
								ON TRUE
							JOIN mv_analytics_course_unit_video_rollup vr
								ON vr.institution_id = ?
								AND vr.account_id = ra.account_id
								AND vr.first_event_at >= ra.account_created_at
								AND vr.first_event_at <= rw.report_end_at
							JOIN course_unit cu
								ON cu.course_unit_id = vr.course_unit_id
							JOIN course_module cm
								ON cm.course_module_id = cu.course_module_id
							JOIN course c
								ON c.course_id = cm.course_id
							JOIN target_course_keys tck
								ON tck.reporting_key = NULLIF(REGEXP_REPLACE(c.reporting_key, '\\s+', '', 'g'), '')
							WHERE cu.course_unit_type_id = 'VIDEO'
							GROUP BY 1, 2
						),
						account_course_metric_rows AS (
							SELECT
								account_id,
								reporting_key,
								complete_value,
								0::DOUBLE PRECISION AS time_seconds,
								0::BIGINT AS visit_count
							FROM account_course_completions
							UNION ALL
							SELECT
								account_id,
								reporting_key,
								complete_value,
								time_seconds,
								visit_count
							FROM account_course_page_metrics
							UNION ALL
							SELECT
								account_id,
								reporting_key,
								complete_value,
								time_seconds,
								visit_count
							FROM account_video_course_time_rows
						),
						account_content_metric_maps AS MATERIALIZED (
							SELECT
								account_id,
								COALESCE(jsonb_object_agg(reporting_key, complete_value::TEXT), '{}'::jsonb) AS metric_complete_values_json,
								COALESCE(jsonb_object_agg(reporting_key, time_seconds::TEXT), '{}'::jsonb) AS metric_time_values_json,
								COALESCE(jsonb_object_agg(reporting_key, visit_count::TEXT), '{}'::jsonb) AS metric_visit_values_json
							FROM (
								SELECT
									account_id,
									reporting_key,
									MAX(complete_value) AS complete_value,
									SUM(time_seconds)::DOUBLE PRECISION AS time_seconds,
									SUM(visit_count)::BIGINT AS visit_count
								FROM account_course_metric_rows
								GROUP BY 1, 2
							) metrics
							GROUP BY account_id
						)
						SELECT
							ra.account_id,
							ra.account_created_at,
							ra.email_address,
							aem.email_entered_at,
							aem.email_verified_at,
							COALESCE(NULLIF(ra.metadata->>'zipCode', ''), asv.screening_values_json->>'bb_onboarding_6') AS bb_zipcode,
							ar.bb_referrer,
							COALESCE(aam.bb_n_sitevisit, 0) AS bb_n_sitevisit,
							COALESCE(aam.bb_tot_time_seconds, 0) AS bb_tot_time_seconds,
							COALESCE(asv.screening_values_json, '{}'::jsonb)::TEXT AS screening_values_json,
							COALESCE(acmm.metric_complete_values_json, '{}'::jsonb)::TEXT AS metric_complete_values_json,
							COALESCE(acmm.metric_time_values_json, '{}'::jsonb)::TEXT AS metric_time_values_json,
							COALESCE(acmm.metric_visit_values_json, '{}'::jsonb)::TEXT AS metric_visit_values_json
						FROM report_accounts ra
						LEFT JOIN account_analytics_metrics aam
							ON aam.account_id = ra.account_id
						LEFT JOIN account_email_metrics aem
							ON aem.account_id = ra.account_id
						LEFT JOIN account_referrer ar
							ON ar.account_id = ra.account_id
						LEFT JOIN account_screening_values asv
							ON asv.account_id = ra.account_id
						LEFT JOIN account_content_metric_maps acmm
							ON acmm.account_id = ra.account_id
						ORDER BY ra.account_created_at, ra.account_id
						""", CourseMcbDownloadReportRecord.class, institutionId, startInstant, endInstant, institutionId, RoleId.PATIENT,
				institutionId, institutionId, institutionId, institutionId, institutionId, institutionId);

		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
				.withZone(institutionTimeZone)
				.withLocale(reportLocale);

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(ACCOUNT_ONBOARDING_COMPLETE_V2_HEADER_COLUMNS.toArray(new String[0])))) {
			for (CourseMcbDownloadReportRecord record : records) {
				Map<String, String> screeningValues = parseJsonObjectAsStringMap(record.getScreeningValuesJson());
				Map<String, String> metricCompleteValues = parseJsonObjectAsStringMap(record.getMetricCompleteValuesJson());
				Map<String, String> metricTimeValues = parseJsonObjectAsStringMap(record.getMetricTimeValuesJson());
				Map<String, String> metricVisitValues = parseJsonObjectAsStringMap(record.getMetricVisitValuesJson());

				List<String> recordElements = new ArrayList<>(ACCOUNT_ONBOARDING_COMPLETE_V2_HEADER_COLUMNS.size());

				for (String headerColumn : ACCOUNT_ONBOARDING_COMPLETE_V2_HEADER_COLUMNS)
					recordElements.add(resolveCourseMcbDownloadColumnValue(headerColumn, record, dateFormatter, screeningValues, metricCompleteValues, metricTimeValues, metricVisitValues));

				csvPrinter.printRecord(recordElements.toArray(new Object[0]));
			}

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void runCourseMcbDownloadStyleReportCsv(@Nonnull InstitutionId institutionId,
																									@Nonnull LocalDateTime startDateTime,
																									@Nonnull LocalDateTime endDateTime,
																									@Nonnull ZoneId reportTimeZone,
																									@Nonnull Locale reportLocale,
																									@Nonnull Writer writer,
																									@Nonnull List<String> headerColumns) {
		requireNonNull(institutionId);
		requireNonNull(startDateTime);
		requireNonNull(endDateTime);
		requireNonNull(reportTimeZone);
		requireNonNull(reportLocale);
		requireNonNull(writer);
		requireNonNull(headerColumns);

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		ZoneId institutionTimeZone = institution.getTimeZone() != null ? institution.getTimeZone() : reportTimeZone;

		Instant startInstant = startDateTime.atZone(institutionTimeZone).toInstant();
		Instant endInstant = endDateTime.atZone(institutionTimeZone).toInstant();

		List<CourseMcbDownloadReportRecord> records = getDatabase().queryForList("""
							WITH institution_onboarding AS (
								SELECT onboarding_screening_flow_id
								FROM institution
								WHERE institution_id = ?
							),
							report_window AS (
								SELECT
									?::TIMESTAMPTZ AS report_start_at,
									?::TIMESTAMPTZ AS report_end_at
							),
							institution_has_reporting_keys AS (
							SELECT (
								EXISTS (
									SELECT 1
									FROM institution_onboarding io
									JOIN screening_flow sf
										ON sf.screening_flow_id = io.onboarding_screening_flow_id
									JOIN screening_flow_version sfv
										ON sfv.screening_flow_version_id = sf.active_screening_flow_version_id
									JOIN screening s
										ON s.screening_id = sfv.initial_screening_id
									JOIN screening_question sq
										ON sq.screening_version_id = s.active_screening_version_id
									WHERE NULLIF(REGEXP_REPLACE(sq.metadata->'reporting'->>'key', '\\s+', '', 'g'), '') LIKE 'bb_onboarding_%'
								)
								AND (
									EXISTS (
										SELECT 1
										FROM screening_flow sf
										JOIN screening_flow_version sfv
											ON sfv.screening_flow_version_id = sf.active_screening_flow_version_id
										JOIN screening s
											ON s.screening_id = sfv.initial_screening_id
										JOIN screening_question sq
											ON sq.screening_version_id = s.active_screening_version_id
										JOIN course_unit cu
											ON cu.screening_flow_id = sf.screening_flow_id
										JOIN course_module cm
											ON cm.course_module_id = cu.course_module_id
										JOIN institution_course ic
											ON ic.course_id = cm.course_id
										WHERE ic.institution_id = ?
											AND NULLIF(REGEXP_REPLACE(sq.metadata->'reporting'->>'key', '\\s+', '', 'g'), '') LIKE 'bb_mcb_%'
									)
									OR EXISTS (
										SELECT 1
										FROM institution_course ic
										JOIN course c
											ON c.course_id = ic.course_id
										WHERE ic.institution_id = ?
											AND NULLIF(REGEXP_REPLACE(c.reporting_key, '\\s+', '', 'g'), '') LIKE 'bb_mcb%'
									)
									OR EXISTS (
										SELECT 1
										FROM institution_course ic
										JOIN course_module cm
											ON cm.course_id = ic.course_id
										JOIN course_unit cu
											ON cu.course_module_id = cm.course_module_id
										WHERE ic.institution_id = ?
											AND NULLIF(REGEXP_REPLACE(cu.reporting_key, '\\s+', '', 'g'), '') LIKE 'bb_mcb_%'
									)
								)
							) AS has_keys
						),
							report_accounts AS (
								SELECT
									a.account_id,
									a.created AS account_created_at,
									a.email_address,
									a.metadata
								FROM account a
								JOIN report_window rw
									ON TRUE
								JOIN institution_has_reporting_keys ihrk
									ON ihrk.has_keys = TRUE
								WHERE a.institution_id = ?
									AND a.created >= rw.report_start_at
									AND a.created <= rw.report_end_at
									AND a.role_id = ?
									AND a.test_account = FALSE
							),
								account_site_metrics AS MATERIALIZED (
									SELECT
										ra.account_id,
										COUNT(DISTINCT mv.session_id)::BIGINT AS bb_n_sitevisit
									FROM report_accounts ra
									JOIN report_window rw
										ON TRUE
									LEFT JOIN mv_analytics_dwell_time mv
										ON mv.institution_id = ?
										AND mv.account_id = ra.account_id
										AND mv.page_viewed_at >= ra.account_created_at
										AND mv.page_viewed_at <= rw.report_end_at
									GROUP BY ra.account_id
								),
							account_tot_time AS MATERIALIZED (
								SELECT
									ra.account_id,
									COALESCE(SUM(mv.dwell_time_seconds), 0)::DOUBLE PRECISION AS bb_tot_time_seconds
								FROM report_accounts ra
								JOIN report_window rw
									ON TRUE
								LEFT JOIN mv_analytics_dwell_time mv
									ON mv.institution_id = ?
									AND mv.account_id = ra.account_id
									AND mv.page_viewed_at >= ra.account_created_at
									AND mv.page_viewed_at <= rw.report_end_at
								GROUP BY ra.account_id
							),
							account_email_metrics AS MATERIALIZED (
								SELECT
									ra.account_id,
									MIN(ai.created) AS email_entered_at,
									MIN(ai_claimed.last_updated) AS email_verified_at
								FROM report_accounts ra
								JOIN report_window rw
									ON TRUE
								LEFT JOIN account_invite ai
									ON ai.institution_id = ?
									AND LOWER(ai.email_address) = LOWER(ra.email_address)
									AND ai.created <= rw.report_end_at
								LEFT JOIN account_invite ai_claimed
									ON ai_claimed.institution_id = ai.institution_id
									AND LOWER(ai_claimed.email_address) = LOWER(ai.email_address)
									AND ai_claimed.claimed = TRUE
									AND ai_claimed.last_updated <= rw.report_end_at
								GROUP BY ra.account_id
							),
							account_referrer AS MATERIALIZED (
								SELECT
									ra.account_id,
									first_referrer.bb_referrer
								FROM report_accounts ra
								JOIN report_window rw
									ON TRUE
								LEFT JOIN LATERAL (
									SELECT
										COALESCE(
											NULLIF(LOWER(SPLIT_PART(REGEXP_REPLACE(COALESCE(ane.data->>'referringUrl', ''), '^https?://', ''), '/', 1)), ''),
										NULLIF(LOWER(ane.referring_campaign), '')
									) AS bb_referrer
									FROM analytics_native_event ane
									WHERE ane.institution_id = ?
										AND ane.account_id = ra.account_id
										AND ane.timestamp >= ra.account_created_at
										AND ane.timestamp <= rw.report_end_at
										AND (
											NULLIF(ane.data->>'referringUrl', '') IS NOT NULL
											OR NULLIF(ane.referring_campaign, '') IS NOT NULL
									)
								ORDER BY ane.timestamp
								LIMIT 1
							) first_referrer ON TRUE
						),
							account_unit_completions AS (
								SELECT
									ra.account_id,
									NULLIF(REGEXP_REPLACE(cu.reporting_key, '\\s+', '', 'g'), '') AS reporting_key,
									CASE WHEN BOOL_OR(csu.course_session_unit_status_id = 'COMPLETED' AND csu.completed_at <= rw.report_end_at) THEN 1 ELSE 0 END AS complete_value
								FROM report_accounts ra
								JOIN report_window rw
									ON TRUE
								JOIN course_session cs
									ON cs.account_id = ra.account_id
									AND cs.created <= rw.report_end_at
								JOIN course_session_unit csu
									ON csu.course_session_id = cs.course_session_id
								JOIN course_unit cu
									ON cu.course_unit_id = csu.course_unit_id
							WHERE NULLIF(REGEXP_REPLACE(cu.reporting_key, '\\s+', '', 'g'), '') IS NOT NULL
							GROUP BY 1, 2
						),
							account_unit_page_metrics AS MATERIALIZED (
								SELECT
									ra.account_id,
									NULLIF(REGEXP_REPLACE(cu.reporting_key, '\\s+', '', 'g'), '') AS unit_reporting_key,
									cu.course_unit_type_id AS unit_type,
									NULLIF(REGEXP_REPLACE(c.reporting_key, '\\s+', '', 'g'), '') AS course_reporting_key,
									COALESCE(SUM(mv.dwell_time_seconds), 0)::DOUBLE PRECISION AS time_seconds,
									COUNT(*)::BIGINT AS visit_count
								FROM report_accounts ra
								JOIN report_window rw
									ON TRUE
								JOIN mv_analytics_dwell_time mv
									ON mv.institution_id = ?
									AND mv.account_id = ra.account_id
									AND mv.page_view_type = 'PAGE_VIEW_COURSE_UNIT'
									AND mv.course_unit_id IS NOT NULL
									AND mv.page_viewed_at >= ra.account_created_at
									AND mv.page_viewed_at <= rw.report_end_at
								JOIN course_unit cu
									ON cu.course_unit_id = mv.course_unit_id
							JOIN course_module cm
								ON cm.course_module_id = cu.course_module_id
							JOIN course c
								ON c.course_id = cm.course_id
							WHERE
								NULLIF(REGEXP_REPLACE(cu.reporting_key, '\\s+', '', 'g'), '') IS NOT NULL
								OR NULLIF(REGEXP_REPLACE(c.reporting_key, '\\s+', '', 'g'), '') IS NOT NULL
							GROUP BY 1, 2, 3, 4
						),
						account_unit_visit_rows AS (
							SELECT
								account_id,
								unit_reporting_key AS reporting_key,
								0 AS complete_value,
								0::DOUBLE PRECISION AS time_seconds,
								SUM(visit_count)::BIGINT AS visit_count
							FROM account_unit_page_metrics
							WHERE unit_reporting_key IS NOT NULL
							GROUP BY 1, 2
						),
						account_non_video_unit_time_rows AS (
							SELECT
								account_id,
								unit_reporting_key AS reporting_key,
								0 AS complete_value,
								SUM(time_seconds)::DOUBLE PRECISION AS time_seconds,
								0::BIGINT AS visit_count
							FROM account_unit_page_metrics
							WHERE unit_reporting_key IS NOT NULL
								AND unit_type <> 'VIDEO'
							GROUP BY 1, 2
						),
							account_video_unit_metric_rows AS (
								SELECT
									ra.account_id,
									NULLIF(REGEXP_REPLACE(cu.reporting_key, '\\s+', '', 'g'), '') AS reporting_key,
									0 AS complete_value,
									COALESCE(SUM(vr.cumulative_watched_seconds), 0)::DOUBLE PRECISION AS time_seconds,
									0::BIGINT AS visit_count
								FROM report_accounts ra
								JOIN report_window rw
									ON TRUE
								JOIN mv_analytics_course_unit_video_rollup vr
									ON vr.account_id = ra.account_id
									AND vr.first_event_at >= ra.account_created_at
									AND vr.first_event_at <= rw.report_end_at
								JOIN course_unit cu
									ON cu.course_unit_id = vr.course_unit_id
								WHERE cu.course_unit_type_id = 'VIDEO'
									AND NULLIF(REGEXP_REPLACE(cu.reporting_key, '\\s+', '', 'g'), '') IS NOT NULL
								GROUP BY ra.account_id, NULLIF(REGEXP_REPLACE(cu.reporting_key, '\\s+', '', 'g'), '')
						),
						account_unit_completion_rows AS (
							SELECT
								account_id,
								reporting_key,
								complete_value,
								0::DOUBLE PRECISION AS time_seconds,
								0::BIGINT AS visit_count
							FROM account_unit_completions
						),
						account_unit_metric_rows AS (
							SELECT * FROM account_unit_completion_rows
							UNION ALL
							SELECT * FROM account_non_video_unit_time_rows
							UNION ALL
							SELECT * FROM account_video_unit_metric_rows
							UNION ALL
							SELECT * FROM account_unit_visit_rows
						),
						account_unit_metrics AS (
							SELECT
								account_id,
								reporting_key,
								MAX(complete_value) AS complete_value,
								SUM(time_seconds)::DOUBLE PRECISION AS time_seconds,
								SUM(visit_count)::BIGINT AS visit_count
							FROM account_unit_metric_rows
							GROUP BY 1, 2
						),
							account_course_completions AS (
								SELECT
									ra.account_id,
									NULLIF(REGEXP_REPLACE(c.reporting_key, '\\s+', '', 'g'), '') AS reporting_key,
									CASE WHEN BOOL_OR(cs.course_session_status_id = 'COMPLETED' AND cs.completed_at <= rw.report_end_at) THEN 1 ELSE 0 END AS complete_value
								FROM report_accounts ra
								JOIN report_window rw
									ON TRUE
								JOIN course_session cs
									ON cs.account_id = ra.account_id
									AND cs.created <= rw.report_end_at
								JOIN course c
									ON c.course_id = cs.course_id
								WHERE NULLIF(REGEXP_REPLACE(c.reporting_key, '\\s+', '', 'g'), '') IS NOT NULL
								GROUP BY 1, 2
						),
						account_course_visit_rows AS (
							SELECT
								account_id,
								course_reporting_key AS reporting_key,
								0 AS complete_value,
								0::DOUBLE PRECISION AS time_seconds,
								SUM(visit_count)::BIGINT AS visit_count
							FROM account_unit_page_metrics
							WHERE course_reporting_key IS NOT NULL
							GROUP BY 1, 2
						),
						account_non_video_course_time_rows AS (
							SELECT
								account_id,
								course_reporting_key AS reporting_key,
								0 AS complete_value,
								SUM(time_seconds)::DOUBLE PRECISION AS time_seconds,
								0::BIGINT AS visit_count
							FROM account_unit_page_metrics
							WHERE course_reporting_key IS NOT NULL
								AND unit_type <> 'VIDEO'
							GROUP BY 1, 2
						),
							account_video_course_time_rows AS (
								SELECT
									ra.account_id,
									NULLIF(REGEXP_REPLACE(c.reporting_key, '\\s+', '', 'g'), '') AS reporting_key,
									0 AS complete_value,
									COALESCE(SUM(vr.cumulative_watched_seconds), 0)::DOUBLE PRECISION AS time_seconds,
									0::BIGINT AS visit_count
								FROM report_accounts ra
								JOIN report_window rw
									ON TRUE
								JOIN mv_analytics_course_unit_video_rollup vr
									ON vr.account_id = ra.account_id
									AND vr.first_event_at >= ra.account_created_at
									AND vr.first_event_at <= rw.report_end_at
								JOIN course_unit cu
									ON cu.course_unit_id = vr.course_unit_id
							JOIN course_module cm
								ON cm.course_module_id = cu.course_module_id
								JOIN course c
									ON c.course_id = cm.course_id
								WHERE cu.course_unit_type_id = 'VIDEO'
									AND NULLIF(REGEXP_REPLACE(c.reporting_key, '\\s+', '', 'g'), '') IS NOT NULL
								GROUP BY ra.account_id, NULLIF(REGEXP_REPLACE(c.reporting_key, '\\s+', '', 'g'), '')
						),
						account_course_completion_rows AS (
							SELECT
								account_id,
								reporting_key,
								complete_value,
								0::DOUBLE PRECISION AS time_seconds,
								0::BIGINT AS visit_count
							FROM account_course_completions
						),
						account_course_metric_rows AS (
							SELECT * FROM account_course_completion_rows
							UNION ALL
							SELECT * FROM account_non_video_course_time_rows
							UNION ALL
							SELECT * FROM account_video_course_time_rows
							UNION ALL
							SELECT * FROM account_course_visit_rows
						),
						account_course_metrics AS (
							SELECT
								account_id,
								reporting_key,
								MAX(complete_value) AS complete_value,
								SUM(time_seconds)::DOUBLE PRECISION AS time_seconds,
								SUM(visit_count)::BIGINT AS visit_count
							FROM account_course_metric_rows
							GROUP BY 1, 2
						),
						account_content_metrics AS (
							SELECT * FROM account_unit_metrics
							UNION ALL
							SELECT * FROM account_course_metrics
						),
						account_content_metric_maps AS MATERIALIZED (
							SELECT
								account_id,
								COALESCE(jsonb_object_agg(reporting_key, complete_value::TEXT), '{}'::jsonb) AS metric_complete_values_json,
								COALESCE(jsonb_object_agg(reporting_key, time_seconds::TEXT), '{}'::jsonb) AS metric_time_values_json,
								COALESCE(jsonb_object_agg(reporting_key, visit_count::TEXT), '{}'::jsonb) AS metric_visit_values_json
							FROM (
								SELECT
									account_id,
									reporting_key,
									MAX(complete_value) AS complete_value,
									SUM(time_seconds)::DOUBLE PRECISION AS time_seconds,
									SUM(visit_count)::BIGINT AS visit_count
								FROM account_content_metrics
								GROUP BY 1, 2
							) metrics
							GROUP BY account_id
						),
							screening_question_answers AS (
								SELECT
									ss.target_account_id AS account_id,
									NULLIF(REGEXP_REPLACE(sq.metadata->'reporting'->>'key', '\\s+', '', 'g'), '') AS reporting_key,
									ssasq.screening_session_answered_screening_question_id,
									COALESCE(MAX(sa.created), MAX(ss.created)) AS answered_at,
									STRING_AGG(
										COALESCE(NULLIF(sa.text, ''), NULLIF(sao.answer_option_text, ''), sao.display_order::TEXT, sao.score::TEXT),
										',' ORDER BY sa.answer_order
									) AS reporting_value
								FROM screening_session ss
								JOIN report_accounts ra
									ON ra.account_id = ss.target_account_id
								JOIN report_window rw
									ON TRUE
								JOIN v_screening_session_screening sss
									ON sss.screening_session_id = ss.screening_session_id
							JOIN v_screening_session_answered_screening_question ssasq
								ON ssasq.screening_session_screening_id = sss.screening_session_screening_id
							JOIN screening_question sq
								ON sq.screening_question_id = ssasq.screening_question_id
							JOIN v_screening_answer sa
								ON sa.screening_session_answered_screening_question_id = ssasq.screening_session_answered_screening_question_id
								LEFT JOIN screening_answer_option sao
									ON sao.screening_answer_option_id = sa.screening_answer_option_id
								WHERE sq.metadata IS NOT NULL
									AND ss.created >= ra.account_created_at
									AND ss.created <= rw.report_end_at
									AND NULLIF(REGEXP_REPLACE(sq.metadata->'reporting'->>'key', '\\s+', '', 'g'), '') IS NOT NULL
								GROUP BY 1, 2, 3
							),
						latest_screening_values AS (
							SELECT DISTINCT ON (account_id, reporting_key)
								account_id,
								reporting_key,
								reporting_value
							FROM screening_question_answers
							ORDER BY account_id, reporting_key, answered_at DESC, screening_session_answered_screening_question_id DESC
						),
						account_derived_screening_values AS (
							SELECT DISTINCT ON (ra.account_id)
								ra.account_id,
								'bb_mcb_adhd_track' AS reporting_key,
								CASE
									WHEN csom.course_module_id IS NOT NULL THEN '0'
									ELSE '1'
								END AS reporting_value
							FROM report_accounts ra
							JOIN report_window rw
								ON TRUE
							JOIN course_session cs
								ON cs.account_id = ra.account_id
								AND cs.created <= rw.report_end_at
							JOIN course_session_unit csu
								ON csu.course_session_id = cs.course_session_id
								AND csu.course_unit_id = '6d90275d-6b41-4329-8446-a02482dd2f5e'::UUID
								AND csu.course_session_unit_status_id = 'COMPLETED'
								AND csu.completed_at <= rw.report_end_at
							LEFT JOIN course_session_optional_module csom
								ON csom.course_session_id = cs.course_session_id
								AND csom.course_module_id = 'eaf80a54-2ff0-4620-8e50-a8dcd331d8f4'::UUID
							ORDER BY ra.account_id, csu.completed_at DESC, cs.course_session_id DESC
						),
						account_screening_value_rows AS (
							SELECT
								account_id,
								reporting_key,
								reporting_value
							FROM latest_screening_values
							UNION ALL
							SELECT
								account_id,
								reporting_key,
								reporting_value
							FROM account_derived_screening_values
						),
						account_screening_values AS MATERIALIZED (
							SELECT
								account_id,
								COALESCE(jsonb_object_agg(reporting_key, reporting_value), '{}'::jsonb) AS screening_values_json
							FROM account_screening_value_rows
							GROUP BY account_id
						)
						SELECT
							ra.account_id,
							ra.account_created_at,
							ra.email_address,
							aem.email_entered_at,
							aem.email_verified_at,
							COALESCE(NULLIF(ra.metadata->>'zipCode', ''), asv.screening_values_json->>'bb_onboarding_6') AS bb_zipcode,
							ar.bb_referrer,
							COALESCE(asm.bb_n_sitevisit, 0) AS bb_n_sitevisit,
							COALESCE(att.bb_tot_time_seconds, 0) AS bb_tot_time_seconds,
							COALESCE(asv.screening_values_json, '{}'::jsonb)::TEXT AS screening_values_json,
							COALESCE(acmm.metric_complete_values_json, '{}'::jsonb)::TEXT AS metric_complete_values_json,
							COALESCE(acmm.metric_time_values_json, '{}'::jsonb)::TEXT AS metric_time_values_json,
							COALESCE(acmm.metric_visit_values_json, '{}'::jsonb)::TEXT AS metric_visit_values_json
						FROM report_accounts ra
						LEFT JOIN account_site_metrics asm
							ON asm.account_id = ra.account_id
						LEFT JOIN account_tot_time att
							ON att.account_id = ra.account_id
						LEFT JOIN account_email_metrics aem
							ON aem.account_id = ra.account_id
						LEFT JOIN account_referrer ar
							ON ar.account_id = ra.account_id
						LEFT JOIN account_screening_values asv
							ON asv.account_id = ra.account_id
						LEFT JOIN account_content_metric_maps acmm
							ON acmm.account_id = ra.account_id
						ORDER BY ra.account_created_at, ra.account_id
						""", CourseMcbDownloadReportRecord.class, institutionId, startInstant, endInstant, institutionId, institutionId, institutionId, institutionId,
				RoleId.PATIENT, institutionId, institutionId, institutionId, institutionId, institutionId);

		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
				.withZone(institutionTimeZone)
				.withLocale(reportLocale);

		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headerColumns.toArray(new String[0])))) {
			for (CourseMcbDownloadReportRecord record : records) {
				Map<String, String> screeningValues = parseJsonObjectAsStringMap(record.getScreeningValuesJson());
				Map<String, String> metricCompleteValues = parseJsonObjectAsStringMap(record.getMetricCompleteValuesJson());
				Map<String, String> metricTimeValues = parseJsonObjectAsStringMap(record.getMetricTimeValuesJson());
				Map<String, String> metricVisitValues = parseJsonObjectAsStringMap(record.getMetricVisitValuesJson());

				List<String> recordElements = new ArrayList<>(headerColumns.size());

				for (String headerColumn : headerColumns)
					recordElements.add(resolveCourseMcbDownloadColumnValue(headerColumn, record, dateFormatter, screeningValues, metricCompleteValues, metricTimeValues, metricVisitValues));

				csvPrinter.printRecord(recordElements.toArray(new Object[0]));
			}

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	private String resolveCourseMcbDownloadColumnValue(@Nonnull String headerColumn,
																										 @Nonnull CourseMcbDownloadReportRecord record,
																										 @Nonnull DateTimeFormatter dateFormatter,
																										 @Nonnull Map<String, String> screeningValues,
																										 @Nonnull Map<String, String> metricCompleteValues,
																										 @Nonnull Map<String, String> metricTimeValues,
																										 @Nonnull Map<String, String> metricVisitValues) {
		requireNonNull(headerColumn);
		requireNonNull(record);
		requireNonNull(dateFormatter);
		requireNonNull(screeningValues);
		requireNonNull(metricCompleteValues);
		requireNonNull(metricTimeValues);
		requireNonNull(metricVisitValues);

		String normalizedHeaderColumn = normalizeReportingKey(headerColumn);

		if ("recordID".equals(normalizedHeaderColumn))
			return record.getAccountId() == null ? "" : record.getAccountId().toString();

		if ("email".equals(normalizedHeaderColumn))
			return record.getEmailAddress() == null ? "" : record.getEmailAddress();

		if ("bb_email_entered_date".equals(normalizedHeaderColumn))
			return record.getEmailEnteredAt() == null ? "" : dateFormatter.format(record.getEmailEnteredAt());

		if ("bb_email_verified_date".equals(normalizedHeaderColumn))
			return record.getEmailVerifiedAt() == null ? "" : dateFormatter.format(record.getEmailVerifiedAt());

		if ("bb_zipcode".equals(normalizedHeaderColumn))
			return record.getBbZipcode() == null ? "" : record.getBbZipcode();

		if ("bb_referrer".equals(normalizedHeaderColumn))
			return record.getBbReferrer() == null ? "" : record.getBbReferrer();

		if ("bb_n_sitevisit".equals(normalizedHeaderColumn))
			return record.getBbNSitevisit() == null ? "0" : record.getBbNSitevisit().toString();

		if ("bb_tot_time".equals(normalizedHeaderColumn))
			return formatDurationSeconds(record.getBbTotTimeSeconds());

		if (normalizedHeaderColumn.endsWith(METRIC_COMPLETE_COLUMN_SUFFIX)) {
			String metricKey = normalizedHeaderColumn.substring(0, normalizedHeaderColumn.length() - METRIC_COMPLETE_COLUMN_SUFFIX.length());
			return formatCount(metricCompleteValues.get(metricKey));
		}

		if (normalizedHeaderColumn.endsWith(METRIC_TIME_COLUMN_SUFFIX)) {
			String metricKey = normalizedHeaderColumn.substring(0, normalizedHeaderColumn.length() - METRIC_TIME_COLUMN_SUFFIX.length());
			return formatDurationSeconds(parseNullableDouble(metricTimeValues.get(metricKey)));
		}

		if (normalizedHeaderColumn.endsWith(METRIC_VISIT_COLUMN_SUFFIX)) {
			String metricKey = normalizedHeaderColumn.substring(0, normalizedHeaderColumn.length() - METRIC_VISIT_COLUMN_SUFFIX.length());
			return formatCount(metricVisitValues.get(metricKey));
		}

		String screeningValue = screeningValues.get(normalizedHeaderColumn);
		return formatCourseMcbDownloadScreeningValue(normalizedHeaderColumn, screeningValue);
	}

	@Nonnull
	private String formatCourseMcbDownloadScreeningValue(@Nonnull String reportingKey,
																											 @Nullable String rawValue) {
		requireNonNull(reportingKey);

		if (rawValue == null)
			return "";

		String trimmedRawValue = rawValue.trim();

		if (trimmedRawValue.isEmpty())
			return "";

		return switch (reportingKey) {
			case "bb_onboarding_1" ->
					mapCourseMcbSingleResponseValue(trimmedRawValue, this::mapOnboardingRelationshipResponseValue);
			case "bb_onboarding_2" ->
					mapCourseMcbSingleResponseValue(trimmedRawValue, this::mapOnboardingReferralSourceResponseValue);
			case "bb_onboarding_2a" ->
					mapCourseMcbSingleResponseValue(trimmedRawValue, this::mapOnboardingMedicalProviderResponseValue);
			case "bb_onboarding_2b", "bb_onboarding_4", "bb_onboarding_6", "bb_mcb_precourse_2", "bb_mcb_postcourse_qual" ->
					mapCourseMcbOpenEndedResponseValue(trimmedRawValue);
			case "bb_onboarding_3", "bb_mcb_precourse_3" ->
					mapCourseMcbSingleResponseValue(trimmedRawValue, this::mapGenderResponseValue);
			case "bb_onboarding_5" ->
					mapCourseMcbDelimitedResponseValue(trimmedRawValue, this::mapRaceEthnicityResponseValue);
			case "bb_onboarding_7" -> mapCourseMcbSingleResponseValue(trimmedRawValue, this::mapEducationResponseValue);
			case "bb_onboarding_8" ->
					mapCourseMcbSingleResponseValue(trimmedRawValue, this::mapResourceAvailabilityResponseValue);
			case "bb_mcb_precourse_1" ->
					mapCourseMcbSingleResponseValue(trimmedRawValue, this::mapMcbRelationshipResponseValue);
			case "bb_mcb_precourse_4" ->
					mapCourseMcbSingleResponseValue(trimmedRawValue, this::mapMcbDiagnosedAdhdResponseValue);
			case "bb_mcb_precourse_4a", "bb_mcb_precourse_5", "bb_mcb_adhd_track" ->
					mapCourseMcbSingleResponseValue(trimmedRawValue, this::mapYesNoResponseValue);
			case "bb_mcb_precourse_6" ->
					mapCourseMcbDelimitedResponseValue(trimmedRawValue, this::mapMcbBehaviorResponseValue);
			case "bb_mcb_precourse_7" ->
					mapCourseMcbSingleResponseValue(trimmedRawValue, this::mapMcbDifficultyResponseValue);
			case "bb_mcb_tei_1", "bb_mcb_tei_2", "bb_mcb_tei_3", "bb_mcb_tei_4", "bb_mcb_tei_5", "bb_mcb_tei_6",
					 "bb_mcb_tei_7", "bb_mcb_tei_8", "bb_mcb_tei_9" ->
					mapCourseMcbSingleResponseValue(trimmedRawValue, this::mapLikertAgreementResponseValue);
			default -> trimmedRawValue;
		};
	}

	@Nonnull
	private String mapCourseMcbSingleResponseValue(@Nonnull String rawValue,
																								 @Nonnull Function<String, String> mapper) {
		requireNonNull(rawValue);
		requireNonNull(mapper);

		String trimmedRawValue = rawValue.trim();

		if (trimmedRawValue.isEmpty())
			return "";

		String mappedValue = mapper.apply(trimmedRawValue);
		return mappedValue.isEmpty() ? trimmedRawValue : mappedValue;
	}

	@Nonnull
	private String mapCourseMcbDelimitedResponseValue(@Nonnull String rawValue,
																										@Nonnull Function<String, String> mapper) {
		requireNonNull(rawValue);
		requireNonNull(mapper);

		String[] rawTokens = rawValue.split(",");
		List<String> mappedTokens = new ArrayList<>(rawTokens.length);
		Set<String> seenMappedTokens = new LinkedHashSet<>(rawTokens.length);

		for (String rawToken : rawTokens) {
			String trimmedRawToken = rawToken.trim();

			if (trimmedRawToken.isEmpty())
				continue;

			String mappedToken = mapper.apply(trimmedRawToken);

			String normalizedMappedToken;

			if (!mappedToken.isEmpty()) {
				normalizedMappedToken = mappedToken;
			} else if (isCourseMcbNumericResponseToken(trimmedRawToken)) {
				normalizedMappedToken = trimmedRawToken;
			} else {
				continue;
			}

			if (seenMappedTokens.add(normalizedMappedToken))
				mappedTokens.add(normalizedMappedToken);
		}

		return String.join(",", mappedTokens);
	}

	@Nonnull
	private String mapCourseMcbOpenEndedResponseValue(@Nonnull String rawValue) {
		requireNonNull(rawValue);

		String trimmedRawValue = rawValue.trim();

		if (trimmedRawValue.isEmpty())
			return "";

		String normalizedResponseToken = normalizeCourseMcbResponseToken(trimmedRawValue);

		if (isCourseMcbPreferNotToAnswerResponseValue(normalizedResponseToken))
			return "99";

		return trimmedRawValue;
	}

	@Nonnull
	private String mapOnboardingRelationshipResponseValue(@Nonnull String rawValue) {
		requireNonNull(rawValue);

		String normalizedResponseToken = normalizeCourseMcbResponseToken(rawValue);

		if (isCourseMcbPreferNotToAnswerResponseValue(normalizedResponseToken))
			return "99";
		if ("1".equals(normalizedResponseToken) || "mother".equals(normalizedResponseToken))
			return "1";
		if ("2".equals(normalizedResponseToken) || "father".equals(normalizedResponseToken))
			return "2";
		if ("3".equals(normalizedResponseToken) || "grandparent".equals(normalizedResponseToken))
			return "3";
		if ("4".equals(normalizedResponseToken) || normalizedResponseToken.contains("other parent guardian"))
			return "4";
		if ("5".equals(normalizedResponseToken) || normalizedResponseToken.contains("other family member"))
			return "5";
		if ("6".equals(normalizedResponseToken)
				|| normalizedResponseToken.contains("professional working with children")
				|| normalizedResponseToken.contains("professional working with the child"))
			return "6";

		return "";
	}

	@Nonnull
	private String mapOnboardingReferralSourceResponseValue(@Nonnull String rawValue) {
		requireNonNull(rawValue);

		String normalizedResponseToken = normalizeCourseMcbResponseToken(rawValue);

		if (isCourseMcbPreferNotToAnswerResponseValue(normalizedResponseToken))
			return "99";
		if ("1".equals(normalizedResponseToken) || normalizedResponseToken.contains("medical provider recommended"))
			return "1";
		if ("2".equals(normalizedResponseToken) || normalizedResponseToken.contains("friend referred"))
			return "2";
		if ("3".equals(normalizedResponseToken)
				|| (normalizedResponseToken.contains("found the program") && normalizedResponseToken.contains("online")))
			return "3";
		if ("4".equals(normalizedResponseToken)
				|| normalizedResponseToken.contains("family resource center")
				|| normalizedResponseToken.contains("community organization recommended"))
			return "4";
		if ("5".equals(normalizedResponseToken) || "other".equals(normalizedResponseToken))
			return "5";

		return "";
	}

	@Nonnull
	private String mapOnboardingMedicalProviderResponseValue(@Nonnull String rawValue) {
		requireNonNull(rawValue);

		String normalizedResponseToken = normalizeCourseMcbResponseToken(rawValue);

		if (isCourseMcbPreferNotToAnswerResponseValue(normalizedResponseToken))
			return "99";
		if ("1".equals(normalizedResponseToken) || "yes".equals(normalizedResponseToken))
			return "1";
		if ("2".equals(normalizedResponseToken) || "no".equals(normalizedResponseToken))
			return "2";
		if ("3".equals(normalizedResponseToken) || normalizedResponseToken.contains("not sure"))
			return "3";

		return "";
	}

	@Nonnull
	private String mapGenderResponseValue(@Nonnull String rawValue) {
		requireNonNull(rawValue);

		String normalizedResponseToken = normalizeCourseMcbResponseToken(rawValue);

		if (isCourseMcbPreferNotToAnswerResponseValue(normalizedResponseToken))
			return "99";
		if ("0".equals(normalizedResponseToken) || "male".equals(normalizedResponseToken))
			return "0";
		if ("1".equals(normalizedResponseToken) || "female".equals(normalizedResponseToken))
			return "1";

		return "";
	}

	@Nonnull
	private String mapRaceEthnicityResponseValue(@Nonnull String rawValue) {
		requireNonNull(rawValue);

		String normalizedResponseToken = normalizeCourseMcbResponseToken(rawValue);

		if (isCourseMcbPreferNotToAnswerResponseValue(normalizedResponseToken))
			return "99";
		if ("1".equals(normalizedResponseToken) || normalizedResponseToken.contains("american indian") || normalizedResponseToken.contains("alaska native"))
			return "1";
		if ("2".equals(normalizedResponseToken) || "asian".equals(normalizedResponseToken))
			return "2";
		if ("3".equals(normalizedResponseToken) || normalizedResponseToken.contains("black") || normalizedResponseToken.contains("african american"))
			return "3";
		if ("4".equals(normalizedResponseToken) || normalizedResponseToken.contains("hispanic") || normalizedResponseToken.contains("latino"))
			return "4";
		if ("6".equals(normalizedResponseToken) || normalizedResponseToken.contains("native hawaiian") || normalizedResponseToken.contains("pacific islander"))
			return "6";
		if ("7".equals(normalizedResponseToken) || "white".equals(normalizedResponseToken))
			return "7";

		return "";
	}

	@Nonnull
	private String mapEducationResponseValue(@Nonnull String rawValue) {
		requireNonNull(rawValue);

		String normalizedResponseToken = normalizeCourseMcbResponseToken(rawValue);

		if (isCourseMcbPreferNotToAnswerResponseValue(normalizedResponseToken))
			return "99";
		if ("1".equals(normalizedResponseToken) || normalizedResponseToken.contains("less than") && normalizedResponseToken.contains("high school"))
			return "1";
		if ("2".equals(normalizedResponseToken) || normalizedResponseToken.contains("high school diploma") || normalizedResponseToken.contains("ged"))
			return "2";
		if ("3".equals(normalizedResponseToken) || normalizedResponseToken.contains("some college"))
			return "3";
		if ("4".equals(normalizedResponseToken) || normalizedResponseToken.contains("associate"))
			return "4";
		if ("5".equals(normalizedResponseToken) || normalizedResponseToken.contains("bachelor"))
			return "5";
		if ("6".equals(normalizedResponseToken) || normalizedResponseToken.contains("master"))
			return "6";
		if ("7".equals(normalizedResponseToken) || normalizedResponseToken.contains("professional") || normalizedResponseToken.contains("doctoral"))
			return "7";

		return "";
	}

	@Nonnull
	private String mapResourceAvailabilityResponseValue(@Nonnull String rawValue) {
		requireNonNull(rawValue);

		String normalizedResponseToken = normalizeCourseMcbResponseToken(rawValue);

		if (isCourseMcbPreferNotToAnswerResponseValue(normalizedResponseToken))
			return "99";
		if ("5".equals(normalizedResponseToken) || "excellent".equals(normalizedResponseToken))
			return "5";
		if ("4".equals(normalizedResponseToken) || "good".equals(normalizedResponseToken))
			return "4";
		if ("3".equals(normalizedResponseToken) || "fair".equals(normalizedResponseToken))
			return "3";
		if ("2".equals(normalizedResponseToken) || "poor".equals(normalizedResponseToken))
			return "2";
		if ("1".equals(normalizedResponseToken) || normalizedResponseToken.contains("very poor"))
			return "1";

		return "";
	}

	@Nonnull
	private String mapMcbRelationshipResponseValue(@Nonnull String rawValue) {
		requireNonNull(rawValue);

		String normalizedResponseToken = normalizeCourseMcbResponseToken(rawValue);

		if (isCourseMcbPreferNotToAnswerResponseValue(normalizedResponseToken))
			return "99";
		if ("1".equals(normalizedResponseToken) || normalizedResponseToken.contains("family member") || normalizedResponseToken.contains("guardian"))
			return "1";
		if ("2".equals(normalizedResponseToken) || normalizedResponseToken.contains("professional working with children"))
			return "2";

		return "";
	}

	@Nonnull
	private String mapMcbDiagnosedAdhdResponseValue(@Nonnull String rawValue) {
		requireNonNull(rawValue);

		String normalizedResponseToken = normalizeCourseMcbResponseToken(rawValue);

		if (isCourseMcbPreferNotToAnswerResponseValue(normalizedResponseToken))
			return "99";
		if ("1".equals(normalizedResponseToken) || "yes".equals(normalizedResponseToken))
			return "1";
		if ("2".equals(normalizedResponseToken) || "no".equals(normalizedResponseToken))
			return "2";

		return "";
	}

	@Nonnull
	private String mapYesNoResponseValue(@Nonnull String rawValue) {
		requireNonNull(rawValue);

		String normalizedResponseToken = normalizeCourseMcbResponseToken(rawValue);

		if (isCourseMcbPreferNotToAnswerResponseValue(normalizedResponseToken))
			return "99";
		if ("1".equals(normalizedResponseToken) || "yes".equals(normalizedResponseToken))
			return "1";
		if ("0".equals(normalizedResponseToken) || "no".equals(normalizedResponseToken))
			return "0";

		return "";
	}

	@Nonnull
	private String mapMcbBehaviorResponseValue(@Nonnull String rawValue) {
		requireNonNull(rawValue);

		String normalizedResponseToken = normalizeCourseMcbResponseToken(rawValue);

		if (isCourseMcbPreferNotToAnswerResponseValue(normalizedResponseToken))
			return "99";
		if ("1".equals(normalizedResponseToken) || normalizedResponseToken.contains("difficulty completing tasks"))
			return "1";
		if ("2".equals(normalizedResponseToken) || normalizedResponseToken.contains("arguing with adults"))
			return "2";
		if ("3".equals(normalizedResponseToken) || normalizedResponseToken.contains("aggressive behaviors"))
			return "3";
		if ("4".equals(normalizedResponseToken) || normalizedResponseToken.contains("tantrums") || normalizedResponseToken.contains("meltdowns"))
			return "4";
		if ("5".equals(normalizedResponseToken) || normalizedResponseToken.contains("hyperactive behaviors"))
			return "5";
		if ("6".equals(normalizedResponseToken) || normalizedResponseToken.contains("household rules") || normalizedResponseToken.contains("house hold rules"))
			return "6";
		if ("7".equals(normalizedResponseToken) || normalizedResponseToken.contains("meeting expectations at school"))
			return "7";
		if ("8".equals(normalizedResponseToken) || normalizedResponseToken.contains("getting along with friends"))
			return "8";
		if ("9".equals(normalizedResponseToken) || normalizedResponseToken.contains("none of the above"))
			return "9";

		return "";
	}

	@Nonnull
	private String mapMcbDifficultyResponseValue(@Nonnull String rawValue) {
		requireNonNull(rawValue);

		String normalizedResponseToken = normalizeCourseMcbResponseToken(rawValue);

		if (isCourseMcbPreferNotToAnswerResponseValue(normalizedResponseToken))
			return "99";
		if ("1".equals(normalizedResponseToken) || normalizedResponseToken.contains("not at all difficult"))
			return "1";
		if ("2".equals(normalizedResponseToken) || normalizedResponseToken.contains("little difficult"))
			return "2";
		if ("3".equals(normalizedResponseToken) || normalizedResponseToken.contains("somewhat difficult"))
			return "3";
		if ("4".equals(normalizedResponseToken) || normalizedResponseToken.contains("very difficult"))
			return "4";
		if ("5".equals(normalizedResponseToken) || normalizedResponseToken.contains("extremely difficult"))
			return "5";

		return "";
	}

	@Nonnull
	private String mapLikertAgreementResponseValue(@Nonnull String rawValue) {
		requireNonNull(rawValue);

		String normalizedResponseToken = normalizeCourseMcbResponseToken(rawValue);

		if ("1".equals(normalizedResponseToken) || normalizedResponseToken.contains("strongly disagree"))
			return "1";
		if ("2".equals(normalizedResponseToken) || "disagree".equals(normalizedResponseToken))
			return "2";
		if ("3".equals(normalizedResponseToken) || "neutral".equals(normalizedResponseToken))
			return "3";
		if ("4".equals(normalizedResponseToken) || "agree".equals(normalizedResponseToken))
			return "4";
		if ("5".equals(normalizedResponseToken) || normalizedResponseToken.contains("strongly agree"))
			return "5";

		return "";
	}

	@Nonnull
	private String normalizeCourseMcbResponseToken(@Nonnull String responseToken) {
		requireNonNull(responseToken);

		return responseToken.toLowerCase(Locale.US)
				.replaceAll("[^a-z0-9]+", " ")
				.trim()
				.replaceAll("\\s+", " ");
	}

	private boolean isCourseMcbNumericResponseToken(@Nonnull String responseToken) {
		requireNonNull(responseToken);

		return responseToken.matches("\\d+");
	}

	private boolean isCourseMcbPreferNotToAnswerResponseValue(@Nonnull String normalizedResponseToken) {
		requireNonNull(normalizedResponseToken);

		return "99".equals(normalizedResponseToken)
				|| normalizedResponseToken.contains("prefer not to answer");
	}

	@Nonnull
	private String formatCount(@Nullable String value) {
		Double parsedValue = parseNullableDouble(value);
		long roundedValue = Math.max(0, Math.round(parsedValue == null ? 0 : parsedValue));
		return Long.toString(roundedValue);
	}

	@Nullable
	private Double parseNullableDouble(@Nullable String value) {
		if (value == null)
			return null;

		String trimmedValue = value.trim();

		if (trimmedValue.isEmpty())
			return null;

		try {
			return Double.parseDouble(trimmedValue);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Nonnull
	private Map<String, String> parseJsonObjectAsStringMap(@Nullable String jsonObjectAsString) {
		if (jsonObjectAsString == null)
			return Map.of();

		String trimmedJsonObjectAsString = jsonObjectAsString.trim();

		if (trimmedJsonObjectAsString.isEmpty())
			return Map.of();

		Map<String, Object> parsedValues = GSON.fromJson(trimmedJsonObjectAsString, new TypeToken<Map<String, Object>>() {
		}.getType());

		if (parsedValues == null || parsedValues.isEmpty())
			return Map.of();

		Map<String, String> normalizedValues = new HashMap<>(parsedValues.size());

		for (Map.Entry<String, Object> parsedEntry : parsedValues.entrySet()) {
			String normalizedKey = normalizeReportingKey(parsedEntry.getKey());

			if (normalizedKey.isEmpty())
				continue;

			Object parsedValue = parsedEntry.getValue();
			normalizedValues.put(normalizedKey, parsedValue == null ? "" : parsedValue.toString());
		}

		return normalizedValues;
	}

	@Nonnull
	private String normalizeReportingKey(@Nullable String reportingKey) {
		if (reportingKey == null)
			return "";

		return reportingKey.replaceAll("\\s+", "");
	}

	@Nonnull
	private String formatDurationSeconds(@Nullable Double durationInSeconds) {
		long totalSeconds = durationInSeconds == null ? 0 : Math.max(0, Math.round(durationInSeconds));
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;

		return format("%03d:%02d:%02d", hours, minutes, seconds);
	}

	@Nonnull
	private String obfuscateName(@Nullable String name) {
		if (name == null)
			return "";

		String trimmed = name.trim();

		if (trimmed.isEmpty())
			return "";

		return format("%s***", trimmed.substring(0, 1));
	}

	@NotThreadSafe
	protected enum AccountTimelineEventTypeId {
		ACCOUNT_CREATED,
		SESSION_STARTED,
		ACCOUNT_SIGNED_IN,
		ACCOUNT_SIGNED_OUT,
		PAGE_VIEW,
		CLICKTHROUGH,
		SCREENING_SESSION_STARTED,
		SCREENING_SESSION_COMPLETED,
		COURSE_SESSION_STARTED,
		COURSE_UNIT_VIDEO_PLAYBACK,
		COURSE_UNIT_COMPLETED,
		COURSE_COMPLETED,
		GROUP_SESSION_RESERVATION_CREATED
	}

	@NotThreadSafe
	protected enum AccountTimelineActorTypeId {
		ACCOUNT,
		STAFF,
		SYSTEM
	}

	@NotThreadSafe
	protected static class AccountTimelineReportRecord {
		@Nullable
		private Instant occurredAt;
		@Nullable
		private Instant endedAt;
		@Nullable
		private AccountTimelineEventTypeId eventTypeId;
		@Nullable
		private AccountTimelineActorTypeId actorTypeId;
		@Nullable
		private String summary;
		@Nullable
		private String pageViewType;
		@Nullable
		private String locationLabel;
		@Nullable
		private Double dwellTimeSeconds;
		@Nullable
		private Double videoWatchedSeconds;
		@Nullable
		private UUID sessionId;
		@Nullable
		private UUID screeningSessionId;
		@Nullable
		private UUID courseSessionId;
		@Nullable
		private UUID courseId;
		@Nullable
		private String courseTitle;
		@Nullable
		private UUID courseUnitId;
		@Nullable
		private String courseUnitTitle;
		@Nullable
		private UUID groupSessionId;
		@Nullable
		private String sourceTable;
		@Nullable
		private String sourceId;
		@Nullable
		private String detailsJson;

		@Nullable
		public Instant getOccurredAt() {
			return occurredAt;
		}

		public void setOccurredAt(@Nullable Instant occurredAt) {
			this.occurredAt = occurredAt;
		}

		@Nullable
		public Instant getEndedAt() {
			return endedAt;
		}

		public void setEndedAt(@Nullable Instant endedAt) {
			this.endedAt = endedAt;
		}

		@Nullable
		public AccountTimelineEventTypeId getEventTypeId() {
			return eventTypeId;
		}

		public void setEventTypeId(@Nullable AccountTimelineEventTypeId eventTypeId) {
			this.eventTypeId = eventTypeId;
		}

		@Nullable
		public AccountTimelineActorTypeId getActorTypeId() {
			return actorTypeId;
		}

		public void setActorTypeId(@Nullable AccountTimelineActorTypeId actorTypeId) {
			this.actorTypeId = actorTypeId;
		}

		@Nullable
		public String getSummary() {
			return summary;
		}

		public void setSummary(@Nullable String summary) {
			this.summary = summary;
		}

		@Nullable
		public String getPageViewType() {
			return pageViewType;
		}

		public void setPageViewType(@Nullable String pageViewType) {
			this.pageViewType = pageViewType;
		}

		@Nullable
		public String getLocationLabel() {
			return locationLabel;
		}

		public void setLocationLabel(@Nullable String locationLabel) {
			this.locationLabel = locationLabel;
		}

		@Nullable
		public Double getDwellTimeSeconds() {
			return dwellTimeSeconds;
		}

		public void setDwellTimeSeconds(@Nullable Double dwellTimeSeconds) {
			this.dwellTimeSeconds = dwellTimeSeconds;
		}

		@Nullable
		public Double getVideoWatchedSeconds() {
			return videoWatchedSeconds;
		}

		public void setVideoWatchedSeconds(@Nullable Double videoWatchedSeconds) {
			this.videoWatchedSeconds = videoWatchedSeconds;
		}

		@Nullable
		public UUID getSessionId() {
			return sessionId;
		}

		public void setSessionId(@Nullable UUID sessionId) {
			this.sessionId = sessionId;
		}

		@Nullable
		public UUID getScreeningSessionId() {
			return screeningSessionId;
		}

		public void setScreeningSessionId(@Nullable UUID screeningSessionId) {
			this.screeningSessionId = screeningSessionId;
		}

		@Nullable
		public UUID getCourseSessionId() {
			return courseSessionId;
		}

		public void setCourseSessionId(@Nullable UUID courseSessionId) {
			this.courseSessionId = courseSessionId;
		}

		@Nullable
		public UUID getCourseId() {
			return courseId;
		}

		public void setCourseId(@Nullable UUID courseId) {
			this.courseId = courseId;
		}

		@Nullable
		public String getCourseTitle() {
			return courseTitle;
		}

		public void setCourseTitle(@Nullable String courseTitle) {
			this.courseTitle = courseTitle;
		}

		@Nullable
		public UUID getCourseUnitId() {
			return courseUnitId;
		}

		public void setCourseUnitId(@Nullable UUID courseUnitId) {
			this.courseUnitId = courseUnitId;
		}

		@Nullable
		public String getCourseUnitTitle() {
			return courseUnitTitle;
		}

		public void setCourseUnitTitle(@Nullable String courseUnitTitle) {
			this.courseUnitTitle = courseUnitTitle;
		}

		@Nullable
		public UUID getGroupSessionId() {
			return groupSessionId;
		}

		public void setGroupSessionId(@Nullable UUID groupSessionId) {
			this.groupSessionId = groupSessionId;
		}

		@Nullable
		public String getSourceTable() {
			return sourceTable;
		}

		public void setSourceTable(@Nullable String sourceTable) {
			this.sourceTable = sourceTable;
		}

		@Nullable
		public String getSourceId() {
			return sourceId;
		}

		public void setSourceId(@Nullable String sourceId) {
			this.sourceId = sourceId;
		}

		@Nullable
		public String getDetailsJson() {
			return detailsJson;
		}

		public void setDetailsJson(@Nullable String detailsJson) {
			this.detailsJson = detailsJson;
		}
	}

	@NotThreadSafe
	protected static class AccountGeolocationReportRecord {
		@Nullable
		private UUID accountId;
		@Nullable
		private String emailAddress;
		@Nullable
		private Instant accountCreatedAt;
		@Nullable
		private RoleId roleId;
		@Nullable
		private AccountSourceId accountSourceId;
		@Nullable
		private UUID accountInviteId;
		@Nullable
		private Boolean inviteClaimed;
		@Nullable
		private Instant inviteCreatedAt;
		@Nullable
		private Instant inviteLastUpdatedAt;
		@Nullable
		private String ipAddress;
		@Nullable
		private Long analyticsEventCount;
		@Nullable
		private Long analyticsSessionCount;
		@Nullable
		private Instant firstAnalyticsEventAt;
		@Nullable
		private Instant lastAnalyticsEventAt;
		@Nullable
		private String ipGeolocationStatusId;
		@Nullable
		private String ipType;
		@Nullable
		private String continentCode;
		@Nullable
		private String continentName;
		@Nullable
		private String countryCode;
		@Nullable
		private String countryName;
		@Nullable
		private String regionCode;
		@Nullable
		private String regionName;
		@Nullable
		private String city;
		@Nullable
		private String postalCode;
		@Nullable
		private Double latitude;
		@Nullable
		private Double longitude;
		@Nullable
		private String msa;
		@Nullable
		private String dma;
		@Nullable
		private Double radius;
		@Nullable
		private String ipRoutingType;
		@Nullable
		private String connectionType;
		@Nullable
		private Long connectionAsn;
		@Nullable
		private String connectionIsp;
		@Nullable
		private String connectionOrganizationType;
		@Nullable
		private Boolean connectionHome;
		@Nullable
		private String hostname;
		@Nullable
		private String timeZoneId;
		@Nullable
		private Integer timeZoneGmtOffset;
		@Nullable
		private String timeZoneCode;
		@Nullable
		private Long locationGeonameId;
		@Nullable
		private Boolean locationIsEu;
		@Nullable
		private Integer providerErrorCode;
		@Nullable
		private String providerErrorType;
		@Nullable
		private String providerErrorMessage;
		@Nullable
		private Instant lastLookupAttemptedAt;
		@Nullable
		private Instant lastLookupSucceededAt;

		@Nullable
		public UUID getAccountId() {
			return accountId;
		}

		public void setAccountId(@Nullable UUID accountId) {
			this.accountId = accountId;
		}

		@Nullable
		public String getEmailAddress() {
			return emailAddress;
		}

		public void setEmailAddress(@Nullable String emailAddress) {
			this.emailAddress = emailAddress;
		}

		@Nullable
		public Instant getAccountCreatedAt() {
			return accountCreatedAt;
		}

		public void setAccountCreatedAt(@Nullable Instant accountCreatedAt) {
			this.accountCreatedAt = accountCreatedAt;
		}

		@Nullable
		public RoleId getRoleId() {
			return roleId;
		}

		public void setRoleId(@Nullable RoleId roleId) {
			this.roleId = roleId;
		}

		@Nullable
		public AccountSourceId getAccountSourceId() {
			return accountSourceId;
		}

		public void setAccountSourceId(@Nullable AccountSourceId accountSourceId) {
			this.accountSourceId = accountSourceId;
		}

		@Nullable
		public UUID getAccountInviteId() {
			return accountInviteId;
		}

		public void setAccountInviteId(@Nullable UUID accountInviteId) {
			this.accountInviteId = accountInviteId;
		}

		@Nullable
		public Boolean getInviteClaimed() {
			return inviteClaimed;
		}

		public void setInviteClaimed(@Nullable Boolean inviteClaimed) {
			this.inviteClaimed = inviteClaimed;
		}

		@Nullable
		public Instant getInviteCreatedAt() {
			return inviteCreatedAt;
		}

		public void setInviteCreatedAt(@Nullable Instant inviteCreatedAt) {
			this.inviteCreatedAt = inviteCreatedAt;
		}

		@Nullable
		public Instant getInviteLastUpdatedAt() {
			return inviteLastUpdatedAt;
		}

		public void setInviteLastUpdatedAt(@Nullable Instant inviteLastUpdatedAt) {
			this.inviteLastUpdatedAt = inviteLastUpdatedAt;
		}

		@Nullable
		public String getIpAddress() {
			return ipAddress;
		}

		public void setIpAddress(@Nullable String ipAddress) {
			this.ipAddress = ipAddress;
		}

		@Nullable
		public Long getAnalyticsEventCount() {
			return analyticsEventCount;
		}

		public void setAnalyticsEventCount(@Nullable Long analyticsEventCount) {
			this.analyticsEventCount = analyticsEventCount;
		}

		@Nullable
		public Long getAnalyticsSessionCount() {
			return analyticsSessionCount;
		}

		public void setAnalyticsSessionCount(@Nullable Long analyticsSessionCount) {
			this.analyticsSessionCount = analyticsSessionCount;
		}

		@Nullable
		public Instant getFirstAnalyticsEventAt() {
			return firstAnalyticsEventAt;
		}

		public void setFirstAnalyticsEventAt(@Nullable Instant firstAnalyticsEventAt) {
			this.firstAnalyticsEventAt = firstAnalyticsEventAt;
		}

		@Nullable
		public Instant getLastAnalyticsEventAt() {
			return lastAnalyticsEventAt;
		}

		public void setLastAnalyticsEventAt(@Nullable Instant lastAnalyticsEventAt) {
			this.lastAnalyticsEventAt = lastAnalyticsEventAt;
		}

		@Nullable
		public String getIpGeolocationStatusId() {
			return ipGeolocationStatusId;
		}

		public void setIpGeolocationStatusId(@Nullable String ipGeolocationStatusId) {
			this.ipGeolocationStatusId = ipGeolocationStatusId;
		}

		@Nullable
		public String getIpType() {
			return ipType;
		}

		public void setIpType(@Nullable String ipType) {
			this.ipType = ipType;
		}

		@Nullable
		public String getContinentCode() {
			return continentCode;
		}

		public void setContinentCode(@Nullable String continentCode) {
			this.continentCode = continentCode;
		}

		@Nullable
		public String getContinentName() {
			return continentName;
		}

		public void setContinentName(@Nullable String continentName) {
			this.continentName = continentName;
		}

		@Nullable
		public String getCountryCode() {
			return countryCode;
		}

		public void setCountryCode(@Nullable String countryCode) {
			this.countryCode = countryCode;
		}

		@Nullable
		public String getCountryName() {
			return countryName;
		}

		public void setCountryName(@Nullable String countryName) {
			this.countryName = countryName;
		}

		@Nullable
		public String getRegionCode() {
			return regionCode;
		}

		public void setRegionCode(@Nullable String regionCode) {
			this.regionCode = regionCode;
		}

		@Nullable
		public String getRegionName() {
			return regionName;
		}

		public void setRegionName(@Nullable String regionName) {
			this.regionName = regionName;
		}

		@Nullable
		public String getCity() {
			return city;
		}

		public void setCity(@Nullable String city) {
			this.city = city;
		}

		@Nullable
		public String getPostalCode() {
			return postalCode;
		}

		public void setPostalCode(@Nullable String postalCode) {
			this.postalCode = postalCode;
		}

		@Nullable
		public Double getLatitude() {
			return latitude;
		}

		public void setLatitude(@Nullable Double latitude) {
			this.latitude = latitude;
		}

		@Nullable
		public Double getLongitude() {
			return longitude;
		}

		public void setLongitude(@Nullable Double longitude) {
			this.longitude = longitude;
		}

		@Nullable
		public String getMsa() {
			return msa;
		}

		public void setMsa(@Nullable String msa) {
			this.msa = msa;
		}

		@Nullable
		public String getDma() {
			return dma;
		}

		public void setDma(@Nullable String dma) {
			this.dma = dma;
		}

		@Nullable
		public Double getRadius() {
			return radius;
		}

		public void setRadius(@Nullable Double radius) {
			this.radius = radius;
		}

		@Nullable
		public String getIpRoutingType() {
			return ipRoutingType;
		}

		public void setIpRoutingType(@Nullable String ipRoutingType) {
			this.ipRoutingType = ipRoutingType;
		}

		@Nullable
		public String getConnectionType() {
			return connectionType;
		}

		public void setConnectionType(@Nullable String connectionType) {
			this.connectionType = connectionType;
		}

		@Nullable
		public Long getConnectionAsn() {
			return connectionAsn;
		}

		public void setConnectionAsn(@Nullable Long connectionAsn) {
			this.connectionAsn = connectionAsn;
		}

		@Nullable
		public String getConnectionIsp() {
			return connectionIsp;
		}

		public void setConnectionIsp(@Nullable String connectionIsp) {
			this.connectionIsp = connectionIsp;
		}

		@Nullable
		public String getConnectionOrganizationType() {
			return connectionOrganizationType;
		}

		public void setConnectionOrganizationType(@Nullable String connectionOrganizationType) {
			this.connectionOrganizationType = connectionOrganizationType;
		}

		@Nullable
		public Boolean getConnectionHome() {
			return connectionHome;
		}

		public void setConnectionHome(@Nullable Boolean connectionHome) {
			this.connectionHome = connectionHome;
		}

		@Nullable
		public String getHostname() {
			return hostname;
		}

		public void setHostname(@Nullable String hostname) {
			this.hostname = hostname;
		}

		@Nullable
		public String getTimeZoneId() {
			return timeZoneId;
		}

		public void setTimeZoneId(@Nullable String timeZoneId) {
			this.timeZoneId = timeZoneId;
		}

		@Nullable
		public Integer getTimeZoneGmtOffset() {
			return timeZoneGmtOffset;
		}

		public void setTimeZoneGmtOffset(@Nullable Integer timeZoneGmtOffset) {
			this.timeZoneGmtOffset = timeZoneGmtOffset;
		}

		@Nullable
		public String getTimeZoneCode() {
			return timeZoneCode;
		}

		public void setTimeZoneCode(@Nullable String timeZoneCode) {
			this.timeZoneCode = timeZoneCode;
		}

		@Nullable
		public Long getLocationGeonameId() {
			return locationGeonameId;
		}

		public void setLocationGeonameId(@Nullable Long locationGeonameId) {
			this.locationGeonameId = locationGeonameId;
		}

		@Nullable
		public Boolean getLocationIsEu() {
			return locationIsEu;
		}

		public void setLocationIsEu(@Nullable Boolean locationIsEu) {
			this.locationIsEu = locationIsEu;
		}

		@Nullable
		public Integer getProviderErrorCode() {
			return providerErrorCode;
		}

		public void setProviderErrorCode(@Nullable Integer providerErrorCode) {
			this.providerErrorCode = providerErrorCode;
		}

		@Nullable
		public String getProviderErrorType() {
			return providerErrorType;
		}

		public void setProviderErrorType(@Nullable String providerErrorType) {
			this.providerErrorType = providerErrorType;
		}

		@Nullable
		public String getProviderErrorMessage() {
			return providerErrorMessage;
		}

		public void setProviderErrorMessage(@Nullable String providerErrorMessage) {
			this.providerErrorMessage = providerErrorMessage;
		}

		@Nullable
		public Instant getLastLookupAttemptedAt() {
			return lastLookupAttemptedAt;
		}

		public void setLastLookupAttemptedAt(@Nullable Instant lastLookupAttemptedAt) {
			this.lastLookupAttemptedAt = lastLookupAttemptedAt;
		}

		@Nullable
		public Instant getLastLookupSucceededAt() {
			return lastLookupSucceededAt;
		}

		public void setLastLookupSucceededAt(@Nullable Instant lastLookupSucceededAt) {
			this.lastLookupSucceededAt = lastLookupSucceededAt;
		}
	}

	@NotThreadSafe
	protected static class CourseMcbDownloadReportRecord {
		@Nullable
		private UUID accountId;
		@Nullable
		private Instant accountCreatedAt;
		@Nullable
		private String emailAddress;
		@Nullable
		private Instant emailEnteredAt;
		@Nullable
		private Instant emailVerifiedAt;
		@Nullable
		private String bbZipcode;
		@Nullable
		private String bbReferrer;
		@Nullable
		@DatabaseColumn("bb_n_sitevisit")
		private Long bbNSitevisit;
		@Nullable
		private Double bbTotTimeSeconds;
		@Nullable
		private String screeningValuesJson;
		@Nullable
		private String metricCompleteValuesJson;
		@Nullable
		private String metricTimeValuesJson;
		@Nullable
		private String metricVisitValuesJson;

		@Nullable
		public UUID getAccountId() {
			return accountId;
		}

		public void setAccountId(@Nullable UUID accountId) {
			this.accountId = accountId;
		}

		@Nullable
		public Instant getAccountCreatedAt() {
			return accountCreatedAt;
		}

		public void setAccountCreatedAt(@Nullable Instant accountCreatedAt) {
			this.accountCreatedAt = accountCreatedAt;
		}

		@Nullable
		public String getEmailAddress() {
			return emailAddress;
		}

		public void setEmailAddress(@Nullable String emailAddress) {
			this.emailAddress = emailAddress;
		}

		@Nullable
		public Instant getEmailEnteredAt() {
			return emailEnteredAt;
		}

		public void setEmailEnteredAt(@Nullable Instant emailEnteredAt) {
			this.emailEnteredAt = emailEnteredAt;
		}

		@Nullable
		public Instant getEmailVerifiedAt() {
			return emailVerifiedAt;
		}

		public void setEmailVerifiedAt(@Nullable Instant emailVerifiedAt) {
			this.emailVerifiedAt = emailVerifiedAt;
		}

		@Nullable
		public String getBbZipcode() {
			return bbZipcode;
		}

		public void setBbZipcode(@Nullable String bbZipcode) {
			this.bbZipcode = bbZipcode;
		}

		@Nullable
		public String getBbReferrer() {
			return bbReferrer;
		}

		public void setBbReferrer(@Nullable String bbReferrer) {
			this.bbReferrer = bbReferrer;
		}

		@Nullable
		public Long getBbNSitevisit() {
			return bbNSitevisit;
		}

		public void setBbNSitevisit(@Nullable Long bbNSitevisit) {
			this.bbNSitevisit = bbNSitevisit;
		}

		@Nullable
		public Double getBbTotTimeSeconds() {
			return bbTotTimeSeconds;
		}

		public void setBbTotTimeSeconds(@Nullable Double bbTotTimeSeconds) {
			this.bbTotTimeSeconds = bbTotTimeSeconds;
		}

		@Nullable
		public String getScreeningValuesJson() {
			return screeningValuesJson;
		}

		public void setScreeningValuesJson(@Nullable String screeningValuesJson) {
			this.screeningValuesJson = screeningValuesJson;
		}

		@Nullable
		public String getMetricCompleteValuesJson() {
			return metricCompleteValuesJson;
		}

		public void setMetricCompleteValuesJson(@Nullable String metricCompleteValuesJson) {
			this.metricCompleteValuesJson = metricCompleteValuesJson;
		}

		@Nullable
		public String getMetricTimeValuesJson() {
			return metricTimeValuesJson;
		}

		public void setMetricTimeValuesJson(@Nullable String metricTimeValuesJson) {
			this.metricTimeValuesJson = metricTimeValuesJson;
		}

		@Nullable
		public String getMetricVisitValuesJson() {
			return metricVisitValuesJson;
		}

		public void setMetricVisitValuesJson(@Nullable String metricVisitValuesJson) {
			this.metricVisitValuesJson = metricVisitValuesJson;
		}
	}

	@NotThreadSafe
	protected static class AdminAnalyticsSignInPageviewNoAccountReportRecord {
		@Nullable
		private UUID analyticsNativeEventId;
		@Nullable
		private Instant timestamp;
		@Nullable
		private AnalyticsNativeEventTypeId analyticsNativeEventTypeId;
		@Nullable
		private UUID clientDeviceId;
		@Nullable
		private UUID sessionId;
		@Nullable
		private String webappUrl;
		@Nullable
		private UUID referringMessageId;
		@Nullable
		private String referringCampaign;
		@Nullable
		private String userAgent;
		@Nullable
		private String appName;
		@Nullable
		private String appVersion;
		@Nullable
		private String clientDeviceTimeZone;

		@Nullable
		public UUID getAnalyticsNativeEventId() {
			return analyticsNativeEventId;
		}

		public void setAnalyticsNativeEventId(@Nullable UUID analyticsNativeEventId) {
			this.analyticsNativeEventId = analyticsNativeEventId;
		}

		@Nullable
		public Instant getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(@Nullable Instant timestamp) {
			this.timestamp = timestamp;
		}

		@Nullable
		public AnalyticsNativeEventTypeId getAnalyticsNativeEventTypeId() {
			return analyticsNativeEventTypeId;
		}

		public void setAnalyticsNativeEventTypeId(@Nullable AnalyticsNativeEventTypeId analyticsNativeEventTypeId) {
			this.analyticsNativeEventTypeId = analyticsNativeEventTypeId;
		}

		@Nullable
		public UUID getClientDeviceId() {
			return clientDeviceId;
		}

		public void setClientDeviceId(@Nullable UUID clientDeviceId) {
			this.clientDeviceId = clientDeviceId;
		}

		@Nullable
		public UUID getSessionId() {
			return sessionId;
		}

		public void setSessionId(@Nullable UUID sessionId) {
			this.sessionId = sessionId;
		}

		@Nullable
		public String getWebappUrl() {
			return webappUrl;
		}

		public void setWebappUrl(@Nullable String webappUrl) {
			this.webappUrl = webappUrl;
		}

		@Nullable
		public UUID getReferringMessageId() {
			return referringMessageId;
		}

		public void setReferringMessageId(@Nullable UUID referringMessageId) {
			this.referringMessageId = referringMessageId;
		}

		@Nullable
		public String getReferringCampaign() {
			return referringCampaign;
		}

		public void setReferringCampaign(@Nullable String referringCampaign) {
			this.referringCampaign = referringCampaign;
		}

		@Nullable
		public String getUserAgent() {
			return userAgent;
		}

		public void setUserAgent(@Nullable String userAgent) {
			this.userAgent = userAgent;
		}

		@Nullable
		public String getAppName() {
			return appName;
		}

		public void setAppName(@Nullable String appName) {
			this.appName = appName;
		}

		@Nullable
		public String getAppVersion() {
			return appVersion;
		}

		public void setAppVersion(@Nullable String appVersion) {
			this.appVersion = appVersion;
		}

		@Nullable
		public String getClientDeviceTimeZone() {
			return clientDeviceTimeZone;
		}

		public void setClientDeviceTimeZone(@Nullable String clientDeviceTimeZone) {
			this.clientDeviceTimeZone = clientDeviceTimeZone;
		}
	}

	@NotThreadSafe
	protected static class AdminAnalyticsAccountSignupUnverifiedReportRecord {
		@Nullable
		private UUID accountId;
		@Nullable
		private Instant inviteCreatedAt;
		@Nullable
		private String emailAddress;
		@Nullable
		private String firstName;
		@Nullable
		private String lastName;
		@Nullable
		private String roleId;
		@Nullable
		private AccountSourceId accountSourceId;
		@Nullable
		private UUID messageId;
		@Nullable
		private Instant messageCreatedAt;
		@Nullable
		private String messageStatusId;
		@Nullable
		private Instant delivered;
		@Nullable
		private Instant deliveryFailed;
		@Nullable
		private String deliveryFailedReason;

		@Nullable
		public UUID getAccountId() {
			return accountId;
		}

		public void setAccountId(@Nullable UUID accountId) {
			this.accountId = accountId;
		}

		@Nullable
		public Instant getInviteCreatedAt() {
			return inviteCreatedAt;
		}

		public void setInviteCreatedAt(@Nullable Instant inviteCreatedAt) {
			this.inviteCreatedAt = inviteCreatedAt;
		}

		@Nullable
		public String getEmailAddress() {
			return emailAddress;
		}

		public void setEmailAddress(@Nullable String emailAddress) {
			this.emailAddress = emailAddress;
		}

		@Nullable
		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(@Nullable String firstName) {
			this.firstName = firstName;
		}

		@Nullable
		public String getLastName() {
			return lastName;
		}

		public void setLastName(@Nullable String lastName) {
			this.lastName = lastName;
		}

		@Nullable
		public String getRoleId() {
			return roleId;
		}

		public void setRoleId(@Nullable String roleId) {
			this.roleId = roleId;
		}

		@Nullable
		public AccountSourceId getAccountSourceId() {
			return accountSourceId;
		}

		public void setAccountSourceId(@Nullable AccountSourceId accountSourceId) {
			this.accountSourceId = accountSourceId;
		}

		@Nullable
		public UUID getMessageId() {
			return messageId;
		}

		public void setMessageId(@Nullable UUID messageId) {
			this.messageId = messageId;
		}

		@Nullable
		public Instant getMessageCreatedAt() {
			return messageCreatedAt;
		}

		public void setMessageCreatedAt(@Nullable Instant messageCreatedAt) {
			this.messageCreatedAt = messageCreatedAt;
		}

		@Nullable
		public String getMessageStatusId() {
			return messageStatusId;
		}

		public void setMessageStatusId(@Nullable String messageStatusId) {
			this.messageStatusId = messageStatusId;
		}

		@Nullable
		public Instant getDelivered() {
			return delivered;
		}

		public void setDelivered(@Nullable Instant delivered) {
			this.delivered = delivered;
		}

		@Nullable
		public Instant getDeliveryFailed() {
			return deliveryFailed;
		}

		public void setDeliveryFailed(@Nullable Instant deliveryFailed) {
			this.deliveryFailed = deliveryFailed;
		}

		@Nullable
		public String getDeliveryFailedReason() {
			return deliveryFailedReason;
		}

		public void setDeliveryFailedReason(@Nullable String deliveryFailedReason) {
			this.deliveryFailedReason = deliveryFailedReason;
		}
	}

	@NotThreadSafe
	protected static class AdminAnalyticsAccountOnboardingIncompleteReportRecord {
		@Nullable
		private UUID accountId;
		@Nullable
		private Instant accountCreatedAt;
		@Nullable
		private String emailAddress;
		@Nullable
		private String firstName;
		@Nullable
		private String lastName;
		@Nullable
		private String roleId;
		@Nullable
		private AccountSourceId accountSourceId;
		@Nullable
		private UUID onboardingScreeningFlowId;
		@Nullable
		private UUID screeningSessionId;
		@Nullable
		private Instant screeningSessionCreatedAt;
		@Nullable
		private Boolean screeningSessionCompleted;
		@Nullable
		private Instant screeningSessionCompletedAt;
		@Nullable
		private Boolean screeningSessionSkipped;
		@Nullable
		private Instant screeningSessionSkippedAt;
		@Nullable
		private UUID screeningSessionScreeningId;
		@Nullable
		private String screeningName;
		@Nullable
		private String questionText;
		@Nullable
		private String answerOptionText;
		@Nullable
		private String answerFreeformText;
		@Nullable
		private Integer answerScore;
		@Nullable
		private Instant answerCreatedAt;

		@Nullable
		public UUID getAccountId() {
			return accountId;
		}

		public void setAccountId(@Nullable UUID accountId) {
			this.accountId = accountId;
		}

		@Nullable
		public Instant getAccountCreatedAt() {
			return accountCreatedAt;
		}

		public void setAccountCreatedAt(@Nullable Instant accountCreatedAt) {
			this.accountCreatedAt = accountCreatedAt;
		}

		@Nullable
		public String getEmailAddress() {
			return emailAddress;
		}

		public void setEmailAddress(@Nullable String emailAddress) {
			this.emailAddress = emailAddress;
		}

		@Nullable
		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(@Nullable String firstName) {
			this.firstName = firstName;
		}

		@Nullable
		public String getLastName() {
			return lastName;
		}

		public void setLastName(@Nullable String lastName) {
			this.lastName = lastName;
		}

		@Nullable
		public String getRoleId() {
			return roleId;
		}

		public void setRoleId(@Nullable String roleId) {
			this.roleId = roleId;
		}

		@Nullable
		public AccountSourceId getAccountSourceId() {
			return accountSourceId;
		}

		public void setAccountSourceId(@Nullable AccountSourceId accountSourceId) {
			this.accountSourceId = accountSourceId;
		}

		@Nullable
		public UUID getOnboardingScreeningFlowId() {
			return onboardingScreeningFlowId;
		}

		public void setOnboardingScreeningFlowId(@Nullable UUID onboardingScreeningFlowId) {
			this.onboardingScreeningFlowId = onboardingScreeningFlowId;
		}

		@Nullable
		public UUID getScreeningSessionId() {
			return screeningSessionId;
		}

		public void setScreeningSessionId(@Nullable UUID screeningSessionId) {
			this.screeningSessionId = screeningSessionId;
		}

		@Nullable
		public Instant getScreeningSessionCreatedAt() {
			return screeningSessionCreatedAt;
		}

		public void setScreeningSessionCreatedAt(@Nullable Instant screeningSessionCreatedAt) {
			this.screeningSessionCreatedAt = screeningSessionCreatedAt;
		}

		@Nullable
		public Boolean getScreeningSessionCompleted() {
			return screeningSessionCompleted;
		}

		public void setScreeningSessionCompleted(@Nullable Boolean screeningSessionCompleted) {
			this.screeningSessionCompleted = screeningSessionCompleted;
		}

		@Nullable
		public Instant getScreeningSessionCompletedAt() {
			return screeningSessionCompletedAt;
		}

		public void setScreeningSessionCompletedAt(@Nullable Instant screeningSessionCompletedAt) {
			this.screeningSessionCompletedAt = screeningSessionCompletedAt;
		}

		@Nullable
		public Boolean getScreeningSessionSkipped() {
			return screeningSessionSkipped;
		}

		public void setScreeningSessionSkipped(@Nullable Boolean screeningSessionSkipped) {
			this.screeningSessionSkipped = screeningSessionSkipped;
		}

		@Nullable
		public Instant getScreeningSessionSkippedAt() {
			return screeningSessionSkippedAt;
		}

		public void setScreeningSessionSkippedAt(@Nullable Instant screeningSessionSkippedAt) {
			this.screeningSessionSkippedAt = screeningSessionSkippedAt;
		}

		@Nullable
		public UUID getScreeningSessionScreeningId() {
			return screeningSessionScreeningId;
		}

		public void setScreeningSessionScreeningId(@Nullable UUID screeningSessionScreeningId) {
			this.screeningSessionScreeningId = screeningSessionScreeningId;
		}

		@Nullable
		public String getScreeningName() {
			return screeningName;
		}

		public void setScreeningName(@Nullable String screeningName) {
			this.screeningName = screeningName;
		}

		@Nullable
		public String getQuestionText() {
			return questionText;
		}

		public void setQuestionText(@Nullable String questionText) {
			this.questionText = questionText;
		}

		@Nullable
		public String getAnswerOptionText() {
			return answerOptionText;
		}

		public void setAnswerOptionText(@Nullable String answerOptionText) {
			this.answerOptionText = answerOptionText;
		}

		@Nullable
		public String getAnswerFreeformText() {
			return answerFreeformText;
		}

		public void setAnswerFreeformText(@Nullable String answerFreeformText) {
			this.answerFreeformText = answerFreeformText;
		}

		@Nullable
		public Integer getAnswerScore() {
			return answerScore;
		}

		public void setAnswerScore(@Nullable Integer answerScore) {
			this.answerScore = answerScore;
		}

		@Nullable
		public Instant getAnswerCreatedAt() {
			return answerCreatedAt;
		}

		public void setAnswerCreatedAt(@Nullable Instant answerCreatedAt) {
			this.answerCreatedAt = answerCreatedAt;
		}
	}

	@NotThreadSafe
	protected static class AdminAnalyticsAccountOnboardingCompleteReportRecord {
		@Nullable
		private UUID accountId;
		@Nullable
		private Instant accountCreatedAt;
		@Nullable
		private String emailAddress;
		@Nullable
		private String firstName;
		@Nullable
		private String lastName;
		@Nullable
		private String roleId;
		@Nullable
		private AccountSourceId accountSourceId;
		@Nullable
		private UUID onboardingScreeningFlowId;
		@Nullable
		private UUID screeningSessionId;
		@Nullable
		private Instant screeningSessionCreatedAt;
		@Nullable
		private Boolean screeningSessionCompleted;
		@Nullable
		private Instant screeningSessionCompletedAt;
		@Nullable
		private Boolean screeningSessionSkipped;
		@Nullable
		private Instant screeningSessionSkippedAt;
		@Nullable
		private UUID screeningSessionScreeningId;
		@Nullable
		private String screeningName;
		@Nullable
		private String questionText;
		@Nullable
		private String answerOptionText;
		@Nullable
		private String answerFreeformText;
		@Nullable
		private Integer answerScore;
		@Nullable
		private Instant answerCreatedAt;

		@Nullable
		public UUID getAccountId() {
			return accountId;
		}

		public void setAccountId(@Nullable UUID accountId) {
			this.accountId = accountId;
		}

		@Nullable
		public Instant getAccountCreatedAt() {
			return accountCreatedAt;
		}

		public void setAccountCreatedAt(@Nullable Instant accountCreatedAt) {
			this.accountCreatedAt = accountCreatedAt;
		}

		@Nullable
		public String getEmailAddress() {
			return emailAddress;
		}

		public void setEmailAddress(@Nullable String emailAddress) {
			this.emailAddress = emailAddress;
		}

		@Nullable
		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(@Nullable String firstName) {
			this.firstName = firstName;
		}

		@Nullable
		public String getLastName() {
			return lastName;
		}

		public void setLastName(@Nullable String lastName) {
			this.lastName = lastName;
		}

		@Nullable
		public String getRoleId() {
			return roleId;
		}

		public void setRoleId(@Nullable String roleId) {
			this.roleId = roleId;
		}

		@Nullable
		public AccountSourceId getAccountSourceId() {
			return accountSourceId;
		}

		public void setAccountSourceId(@Nullable AccountSourceId accountSourceId) {
			this.accountSourceId = accountSourceId;
		}

		@Nullable
		public UUID getOnboardingScreeningFlowId() {
			return onboardingScreeningFlowId;
		}

		public void setOnboardingScreeningFlowId(@Nullable UUID onboardingScreeningFlowId) {
			this.onboardingScreeningFlowId = onboardingScreeningFlowId;
		}

		@Nullable
		public UUID getScreeningSessionId() {
			return screeningSessionId;
		}

		public void setScreeningSessionId(@Nullable UUID screeningSessionId) {
			this.screeningSessionId = screeningSessionId;
		}

		@Nullable
		public Instant getScreeningSessionCreatedAt() {
			return screeningSessionCreatedAt;
		}

		public void setScreeningSessionCreatedAt(@Nullable Instant screeningSessionCreatedAt) {
			this.screeningSessionCreatedAt = screeningSessionCreatedAt;
		}

		@Nullable
		public Boolean getScreeningSessionCompleted() {
			return screeningSessionCompleted;
		}

		public void setScreeningSessionCompleted(@Nullable Boolean screeningSessionCompleted) {
			this.screeningSessionCompleted = screeningSessionCompleted;
		}

		@Nullable
		public Instant getScreeningSessionCompletedAt() {
			return screeningSessionCompletedAt;
		}

		public void setScreeningSessionCompletedAt(@Nullable Instant screeningSessionCompletedAt) {
			this.screeningSessionCompletedAt = screeningSessionCompletedAt;
		}

		@Nullable
		public Boolean getScreeningSessionSkipped() {
			return screeningSessionSkipped;
		}

		public void setScreeningSessionSkipped(@Nullable Boolean screeningSessionSkipped) {
			this.screeningSessionSkipped = screeningSessionSkipped;
		}

		@Nullable
		public Instant getScreeningSessionSkippedAt() {
			return screeningSessionSkippedAt;
		}

		public void setScreeningSessionSkippedAt(@Nullable Instant screeningSessionSkippedAt) {
			this.screeningSessionSkippedAt = screeningSessionSkippedAt;
		}

		@Nullable
		public UUID getScreeningSessionScreeningId() {
			return screeningSessionScreeningId;
		}

		public void setScreeningSessionScreeningId(@Nullable UUID screeningSessionScreeningId) {
			this.screeningSessionScreeningId = screeningSessionScreeningId;
		}

		@Nullable
		public String getScreeningName() {
			return screeningName;
		}

		public void setScreeningName(@Nullable String screeningName) {
			this.screeningName = screeningName;
		}

		@Nullable
		public String getQuestionText() {
			return questionText;
		}

		public void setQuestionText(@Nullable String questionText) {
			this.questionText = questionText;
		}

		@Nullable
		public String getAnswerOptionText() {
			return answerOptionText;
		}

		public void setAnswerOptionText(@Nullable String answerOptionText) {
			this.answerOptionText = answerOptionText;
		}

		@Nullable
		public String getAnswerFreeformText() {
			return answerFreeformText;
		}

		public void setAnswerFreeformText(@Nullable String answerFreeformText) {
			this.answerFreeformText = answerFreeformText;
		}

		@Nullable
		public Integer getAnswerScore() {
			return answerScore;
		}

		public void setAnswerScore(@Nullable Integer answerScore) {
			this.answerScore = answerScore;
		}

		@Nullable
		public Instant getAnswerCreatedAt() {
			return answerCreatedAt;
		}

		public void setAnswerCreatedAt(@Nullable Instant answerCreatedAt) {
			this.answerCreatedAt = answerCreatedAt;
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
		private AccountSourceId accountSourceId;
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
		public AccountSourceId getAccountSourceId() {
			return this.accountSourceId;
		}

		public void setAccountSourceId(@Nullable AccountSourceId accountSourceId) {
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
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
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
