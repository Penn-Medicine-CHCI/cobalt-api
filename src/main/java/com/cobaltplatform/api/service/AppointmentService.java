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
import com.cobaltplatform.api.integration.acuity.AcuitySchedulingException;
import com.cobaltplatform.api.integration.acuity.AcuitySchedulingNotAvailableException;
import com.cobaltplatform.api.integration.acuity.AcuitySyncManager;
import com.cobaltplatform.api.integration.acuity.model.AcuityAppointment;
import com.cobaltplatform.api.integration.acuity.model.AcuityAppointmentType;
import com.cobaltplatform.api.integration.acuity.model.AcuityError;
import com.cobaltplatform.api.integration.acuity.model.request.AcuityAppointmentCreateRequest;
import com.cobaltplatform.api.integration.acuity.model.request.AcuityAppointmentCreateRequest.AcuityAppointmentFieldCreateRequest;
import com.cobaltplatform.api.integration.bluejeans.BluejeansClient;
import com.cobaltplatform.api.integration.bluejeans.MeetingResponse;
import com.cobaltplatform.api.integration.enterprise.EnterprisePlugin;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.integration.epic.EpicClient;
import com.cobaltplatform.api.integration.epic.EpicSyncManager;
import com.cobaltplatform.api.integration.epic.request.GetPatientAppointmentsRequest;
import com.cobaltplatform.api.integration.epic.request.GetProviderScheduleRequest;
import com.cobaltplatform.api.integration.epic.request.ScheduleAppointmentWithInsuranceRequest;
import com.cobaltplatform.api.integration.epic.response.GetPatientAppointmentsResponse;
import com.cobaltplatform.api.integration.epic.response.GetProviderScheduleResponse;
import com.cobaltplatform.api.integration.epic.response.ScheduleAppointmentWithInsuranceResponse;
import com.cobaltplatform.api.integration.gcal.GoogleCalendarUrlGenerator;
import com.cobaltplatform.api.integration.ical.ICalInviteGenerator;
import com.cobaltplatform.api.integration.ical.ICalInviteGenerator.InviteAttendee;
import com.cobaltplatform.api.integration.ical.ICalInviteGenerator.InviteMethod;
import com.cobaltplatform.api.integration.ical.ICalInviteGenerator.InviteOrganizer;
import com.cobaltplatform.api.messaging.email.EmailAttachment;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageManager;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.api.request.CancelAppointmentRequest;
import com.cobaltplatform.api.model.api.request.ChangeAppointmentAttendanceStatusRequest;
import com.cobaltplatform.api.model.api.request.CreateAcuityAppointmentTypeRequest;
import com.cobaltplatform.api.model.api.request.CreateAppointmentRequest;
import com.cobaltplatform.api.model.api.request.CreateAppointmentTypeRequest;
import com.cobaltplatform.api.model.api.request.CreateInteractionInstanceRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientIntakeQuestionRequest;
import com.cobaltplatform.api.model.api.request.CreateScheduledMessageRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningQuestionRequest;
import com.cobaltplatform.api.model.api.request.UpdateAccountEmailAddressRequest;
import com.cobaltplatform.api.model.api.request.UpdateAccountPhoneNumberRequest;
import com.cobaltplatform.api.model.api.request.UpdateAcuityAppointmentTypeRequest;
import com.cobaltplatform.api.model.api.request.UpdateAppointmentRequest;
import com.cobaltplatform.api.model.api.request.UpdateAppointmentTypeRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSession;
import com.cobaltplatform.api.model.db.AccountSessionAnswer;
import com.cobaltplatform.api.model.db.Answer;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.AppointmentReason;
import com.cobaltplatform.api.model.db.AppointmentReasonType.AppointmentReasonTypeId;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.Assessment;
import com.cobaltplatform.api.model.db.AssessmentType;
import com.cobaltplatform.api.model.db.AttendanceStatus.AttendanceStatusId;
import com.cobaltplatform.api.model.db.AuditLog;
import com.cobaltplatform.api.model.db.AuditLogEvent.AuditLogEventId;
import com.cobaltplatform.api.model.db.EpicDepartment;
import com.cobaltplatform.api.model.db.FontSize.FontSizeId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Interaction;
import com.cobaltplatform.api.model.db.InteractionType;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.Question;
import com.cobaltplatform.api.model.db.QuestionContentHint.QuestionContentHintId;
import com.cobaltplatform.api.model.db.QuestionType.QuestionTypeId;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.SourceSystem.SourceSystemId;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.model.db.VideoconferencePlatform.VideoconferencePlatformId;
import com.cobaltplatform.api.model.db.VisitType;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.cobaltplatform.api.model.service.EvidenceScores;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.Normalizer;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlInListPlaceholders;
import static com.cobaltplatform.api.util.DatabaseUtility.sqlVaragsParameters;
import static com.cobaltplatform.api.util.ValidationUtility.isValidHexColor;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class AppointmentService {
	@Nonnull
	private final Database database;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final EpicSyncManager epicSyncManager;
	@Nonnull
	private final AcuitySchedulingClient acuitySchedulingClient;
	@Nonnull
	private final AcuitySchedulingCache acuitySchedulingCache;
	@Nonnull
	private final BluejeansClient bluejeansClient;
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final javax.inject.Provider<ProviderService> providerServiceProvider;
	@Nonnull
	private final javax.inject.Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final javax.inject.Provider<AuditLogService> auditLogServiceProvider;
	@Nonnull
	private final javax.inject.Provider<ClinicService> clinicServiceProvider;
	@Nonnull
	private final javax.inject.Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final javax.inject.Provider<MessageService> messageServiceProvider;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final EmailMessageManager emailMessageManager;
	@Nonnull
	private final javax.inject.Provider<AssessmentScoringService> assessmentScoringServiceProvider;
	@Nonnull
	private final javax.inject.Provider<AcuitySyncManager> acuitySyncManagerProvider;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Normalizer normalizer;
	@Nonnull
	private final SessionService sessionService;
	@Nonnull
	private final AssessmentService assessmentService;
	@Nonnull
	private final GoogleCalendarUrlGenerator googleCalendarUrlGenerator;
	@Nonnull
	private final ICalInviteGenerator iCalInviteGenerator;
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final InteractionService interactionService;

	@Inject
	public AppointmentService(@Nonnull Database database,
														@Nonnull Configuration configuration,
														@Nonnull Strings strings,
														@Nonnull EpicSyncManager epicSyncManager,
														@Nonnull AcuitySchedulingClient acuitySchedulingClient,
														@Nonnull AcuitySchedulingCache acuitySchedulingCache,
														@Nonnull BluejeansClient bluejeansClient,
														@Nonnull EnterprisePluginProvider enterprisePluginProvider,
														@Nonnull javax.inject.Provider<ProviderService> providerServiceProvider,
														@Nonnull javax.inject.Provider<AccountService> accountServiceProvider,
														@Nonnull javax.inject.Provider<AssessmentScoringService> assessmentScoringServiceProvider,
														@Nonnull javax.inject.Provider<AcuitySyncManager> acuitySyncManagerProvider,
														@Nonnull javax.inject.Provider<AuditLogService> auditLogServiceProvider,
														@Nonnull javax.inject.Provider<ClinicService> clinicServiceProvider,
														@Nonnull javax.inject.Provider<InstitutionService> institutionServiceProvider,
														@Nonnull javax.inject.Provider<MessageService> messageServiceProvider,
														@Nonnull EmailMessageManager emailMessageManager,
														@Nonnull Formatter formatter,
														@Nonnull Normalizer normalizer,
														@Nonnull SessionService sessionService,
														@Nonnull AssessmentService assessmentService,
														@Nonnull GoogleCalendarUrlGenerator googleCalendarUrlGenerator,
														@Nonnull ICalInviteGenerator iCalInviteGenerator,
														@Nonnull JsonMapper jsonMapper,
														@Nonnull InteractionService interactionService) {
		requireNonNull(database);
		requireNonNull(configuration);
		requireNonNull(strings);
		requireNonNull(enterprisePluginProvider);
		requireNonNull(epicSyncManager);
		requireNonNull(acuitySchedulingClient);
		requireNonNull(acuitySchedulingCache);
		requireNonNull(bluejeansClient);
		requireNonNull(providerServiceProvider);
		requireNonNull(accountServiceProvider);
		requireNonNull(acuitySyncManagerProvider);
		requireNonNull(auditLogServiceProvider);
		requireNonNull(clinicServiceProvider);
		requireNonNull(institutionServiceProvider);
		requireNonNull(messageServiceProvider);
		requireNonNull(emailMessageManager);
		requireNonNull(formatter);
		requireNonNull(normalizer);
		requireNonNull(sessionService);
		requireNonNull(assessmentService);
		requireNonNull(googleCalendarUrlGenerator);
		requireNonNull(iCalInviteGenerator);
		requireNonNull(jsonMapper);
		requireNonNull(interactionService);

		this.database = database;
		this.configuration = configuration;
		this.strings = strings;
		this.enterprisePluginProvider = enterprisePluginProvider;
		this.epicSyncManager = epicSyncManager;
		this.acuitySchedulingClient = acuitySchedulingClient;
		this.acuitySchedulingCache = acuitySchedulingCache;
		this.bluejeansClient = bluejeansClient;
		this.providerServiceProvider = providerServiceProvider;
		this.accountServiceProvider = accountServiceProvider;
		this.emailMessageManager = emailMessageManager;
		this.acuitySyncManagerProvider = acuitySyncManagerProvider;
		this.auditLogServiceProvider = auditLogServiceProvider;
		this.clinicServiceProvider = clinicServiceProvider;
		this.institutionServiceProvider = institutionServiceProvider;
		this.assessmentScoringServiceProvider = assessmentScoringServiceProvider;
		this.messageServiceProvider = messageServiceProvider;
		this.formatter = formatter;
		this.normalizer = normalizer;
		this.sessionService = sessionService;
		this.assessmentService = assessmentService;
		this.googleCalendarUrlGenerator = googleCalendarUrlGenerator;
		this.iCalInviteGenerator = iCalInviteGenerator;
		this.jsonMapper = jsonMapper;
		this.logger = LoggerFactory.getLogger(getClass());
		this.interactionService = interactionService;
	}

	@Nonnull
	public Optional<Appointment> findAppointmentById(@Nullable UUID appointmentId) {
		if (appointmentId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM appointment WHERE appointment_id=?", Appointment.class, appointmentId);
	}

	@Nonnull
	public Optional<Appointment> findAppointmentByAcuityAppointmentId(@Nullable Long acuityAppointmentId) {
		if (acuityAppointmentId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM appointment WHERE acuity_appointment_id=?", Appointment.class, acuityAppointmentId);
	}

	@Nonnull
	public List<Appointment> findAppointmentsByProviderId(@Nullable UUID providerId,
																												@Nullable LocalDate startDate,
																												@Nullable LocalDate endDate) {
		if (providerId == null || startDate == null || endDate == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM appointment WHERE provider_id=? AND canceled=FALSE " +
				"AND start_time >= ? AND start_time <= ? ORDER BY start_time DESC", Appointment.class, providerId, startDate, endDate);
	}

	@Nonnull
	public List<Appointment> findUpcomingAppointmentsByAccountIdAndProviderId(@Nullable UUID accountId,
																																						@Nullable UUID providerId,
																																						@Nullable ZoneId timeZone) {
		if (providerId == null || accountId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM appointment WHERE account_id = ? AND provider_id=? AND canceled=FALSE " +
				"AND start_time >= ?  ORDER BY start_time ASC", Appointment.class, accountId, providerId, LocalDate.now(timeZone));
	}


	@Nonnull
	public List<Appointment> findRecentAppointmentsByAccountId(@Nullable UUID accountId,
																														 @Nullable ZoneId timeZone) {
		if (accountId == null || timeZone == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM appointment WHERE account_id=? AND canceled=FALSE " +
				"AND start_time < ? ORDER BY start_time DESC LIMIT 10", Appointment.class, accountId, LocalDateTime.now(timeZone));
	}

	@Nonnull
	public List<Appointment> findUpcomingAppointmentsByAccountId(@Nullable UUID accountId,
																															 @Nullable ZoneId timeZone) {
		if (accountId == null || timeZone == null)
			return Collections.emptyList();

		Account account = getAccountService().findAccountById(accountId).get();
		List<Appointment> appointments = getDatabase().queryForList("SELECT * FROM appointment WHERE account_id=? AND canceled=FALSE " +
				"AND start_time >= ? ORDER BY start_time", Appointment.class, accountId, LocalDateTime.now(timeZone));

		// If you're an Epic account, pull the schedule from Epic so we can reconcile data
		if (account.getEpicPatientId() != null && getConfiguration().getShouldUseRealEpic()) {
			EpicClient epicClient = getEnterprisePluginProvider().enterprisePluginForInstitutionId(account.getInstitutionId()).epicClientForBackendService().get();
			Institution institution = getInstitutionService().findInstitutionById(account.getInstitutionId()).get();
			LocalDate startDate = LocalDate.now(account.getTimeZone());
			LocalDate endDate = startDate.plusDays(50L); // Arbitrary number of days in the future for now...

			// SYNC ACCOUNT UID
			String uid = epicClient.determineLatestUIDForPatientIdentifier(account.getEpicPatientId(), account.getEpicPatientIdType()).get();

			if (!uid.equals(account.getEpicPatientId()))
				getAccountService().updateAccountEpicPatient(account.getAccountId(), uid, "UID");

			account = getAccountService().findAccountById(accountId).get();

			GetPatientAppointmentsRequest request = new GetPatientAppointmentsRequest();
			request.setPatientID(account.getEpicPatientId());
			request.setPatientIDType(account.getEpicPatientIdType());
			request.setUserID(institution.getEpicUserId());
			request.setUserIDType(institution.getEpicUserIdType());
			request.setIncludeAllStatuses("1");
			request.setIncludeOutsideAppointments("1");
			request.setStartDate(epicClient.formatDateWithSlashes(startDate));
			request.setEndDate(epicClient.formatDateWithSlashes(endDate));
			request.setExtraItems(List.of("7040", "7050", "28006")); // TODO: pull into plugin/data-drive

			GetPatientAppointmentsResponse response = epicClient.performGetPatientAppointments(request);

			Map<String, Object> payload = new HashMap<>();
			payload.put("request", request);

			AuditLog auditLog = new AuditLog();
			auditLog.setAccountId(account.getAccountId());
			auditLog.setAuditLogEventId(AuditLogEventId.EPIC_APPOINTMENT_LOOKUP);
			auditLog.setPayload(getJsonMapper().toJson(payload));
			getAuditLogService().audit(auditLog);

			// Key EPIC and Local appointments by CSN for quick access
			Map<String, GetPatientAppointmentsResponse.Appointment> epicAppointmentsByCSN = new HashMap<>(response.getAppointments().size());

			for (GetPatientAppointmentsResponse.Appointment epicAppointment : response.getAppointments())
				for (GetPatientAppointmentsResponse.Appointment.ContactID contactID : epicAppointment.getContactIDs())
					if ("CSN".equals(contactID.getType()))
						epicAppointmentsByCSN.put(contactID.getID(), epicAppointment);

			Map<String, Appointment> localAppointmentsByCSN = new HashMap<>(appointments.size());

			for (Appointment appointment : appointments)
				if (appointment.getEpicContactId() != null)
					localAppointmentsByCSN.put(appointment.getEpicContactId(), appointment);

			Set<String> localAppointmentCSNsToCreate = new HashSet<>();
			Set<String> localAppointmentCSNsToCancel = new HashSet<>();

			for (Entry<String, GetPatientAppointmentsResponse.Appointment> epicAppointmentEntry : epicAppointmentsByCSN.entrySet()) {
				String csn = epicAppointmentEntry.getKey();
				Appointment localAppointment = localAppointmentsByCSN.get(csn);

				// No local appointment was found for the EPIC CSN...we need to create this local appointment
				if (localAppointment == null)
					localAppointmentCSNsToCreate.add(csn);
			}

			for (Entry<String, Appointment> localAppointmentEntry : localAppointmentsByCSN.entrySet()) {
				String csn = localAppointmentEntry.getKey();
				GetPatientAppointmentsResponse.Appointment epicAppointment = epicAppointmentsByCSN.get(csn);

				// No EPIC appointment was found for the local CSN...we need to cancel this local appointment
				if (epicAppointment == null)
					localAppointmentCSNsToCancel.add(csn);
			}

			// Cancel local appointments for which there is no corresponding EPIC appointment
			for (String csn : localAppointmentCSNsToCancel) {
				Appointment localAppointment = localAppointmentsByCSN.get(csn);

				getLogger().info("Unable to find EPIC appointment with CSN {}, canceling local appointment ID {}...", csn, localAppointment.getAppointmentId());

				// Kind of a "manual" cancel here since the appointment is already gone from Epic
				getDatabase().execute("UPDATE appointment SET canceled=TRUE, attendance_status_id=?, canceled_at=NOW() WHERE appointment_id=?", AttendanceStatusId.CANCELED, localAppointment.getAppointmentId());

				Map<String, Object> cancelPayload = new HashMap<>();
				cancelPayload.put("csn", csn);
				cancelPayload.put("appointmentId", localAppointment.getAppointmentId());

				AuditLog cancelAuditLog = new AuditLog();
				cancelAuditLog.setAccountId(account.getAccountId());
				cancelAuditLog.setAuditLogEventId(AuditLogEventId.EPIC_APPOINTMENT_IMPLICIT_CANCEL);
				cancelAuditLog.setMessage(format("Canceling local appointment ID %s because we cannot find CSN %s in EPIC", localAppointment.getAppointmentId(), csn));
				cancelAuditLog.setPayload(getJsonMapper().toJson(cancelPayload));
				getAuditLogService().audit(cancelAuditLog);

				getDatabase().currentTransaction().get().addPostCommitOperation(() -> {
					ForkJoinPool.commonPool().execute(() -> {
						getEpicSyncManager().syncProviderAvailability(localAppointment.getProviderId(), localAppointment.getStartTime().toLocalDate());
					});
				});
			}

			// Create local appointments for which there is a corresponding EPIC appointment
			for (String csn : localAppointmentCSNsToCreate) {
				GetPatientAppointmentsResponse.Appointment epicAppointment = epicAppointmentsByCSN.get(csn);

				getLogger().info("Found EPIC appointment with CSN {}, creating corresponding local appointment...", csn);

				GetPatientAppointmentsResponse.Appointment.Provider epicProvider = epicAppointment.getProviders() == null || epicAppointment.getProviders().size() == 0 ? null : epicAppointment.getProviders().get(0);

				if (epicProvider == null) {
					getLogger().warn("Unable to find provider data in EPIC appointment with CSN {}, cannot create new appointment.", csn);
					continue;
				}

				Provider provider = null;

				for (GetPatientAppointmentsResponse.Appointment.Provider.ProviderID providerID : epicProvider.getProviderIDs()) {
					String epicProviderId = trimToEmpty(providerID.getID()).toUpperCase(Locale.US);
					String type = StringUtils.trimToEmpty(providerID.getType()).toUpperCase(Locale.US);

					if ("INTERNAL".equals(type) || "EXTERNAL".equals(type)) {
						provider = getProviderService().findProviderByInstitutionIdAndEpicProviderId(account.getInstitutionId(), epicProviderId).orElse(null);

						if (provider != null)
							break;
					}
				}

				if (provider == null) {
					getLogger().warn("Unable to find a matching provider with EPIC appointment with CSN {}, cannot create new appointment.", csn);
					continue;
				}

				LocalDate epicAppointmentDate = epicClient.parseDateWithSlashes(epicAppointment.getDate()); // e.g. "7/17/2020"
				Integer epicAppointmentDuration = Integer.parseInt(epicAppointment.getAppointmentDuration());
				LocalTime epicAppointmentStartTime = epicClient.parseTimeAmPm(epicAppointment.getAppointmentStartTime());
				LocalTime epicAppointmentEndTime = epicAppointmentStartTime.plusMinutes(epicAppointmentDuration);
				LocalDateTime meetingStartTime = LocalDateTime.of(epicAppointmentDate, epicAppointmentStartTime);
				LocalDateTime meetingEndTime = LocalDateTime.of(epicAppointmentDate, epicAppointmentEndTime);

				LocalDateTime now = LocalDateTime.now(timeZone);

				if (meetingStartTime.isBefore(now)) {
					getLogger().warn("Meeting start time {} for EPIC appointment with CSN {} is after 'now' ({} in {} time zone), cannot create new appointment.", meetingStartTime, csn, now, timeZone.getId());
					continue;
				}

				AppointmentType appointmentType = null;

				List<GetPatientAppointmentsResponse.Appointment.VisitTypeID> visitTypeIDs = epicAppointment.getVisitTypeIDs() == null ? Collections.emptyList() : epicAppointment.getVisitTypeIDs();

				if (visitTypeIDs.size() == 0) {
					getLogger().warn("Unable to find visit type data in EPIC appointment with CSN {}, cannot create new appointment.", csn);
					continue;
				}

				for (GetPatientAppointmentsResponse.Appointment.VisitTypeID visitTypeID : visitTypeIDs) {
					String epicVisitTypeId = trimToEmpty(visitTypeID.getID()).toUpperCase(Locale.US);
					String type = StringUtils.trimToEmpty(visitTypeID.getType()).toUpperCase(Locale.US);

					if ("INTERNAL".equals(type) || "EXTERNAL".equals(type)) {
						appointmentType = getDatabase().queryForObject("SELECT * FROM v_appointment_type WHERE scheduling_system_id=? AND epic_visit_type_id=? AND " +
										"UPPER(epic_visit_type_id_type) IN ('INTERNAL', 'EXTERNAL') AND duration_in_minutes=?", AppointmentType.class,
								SchedulingSystemId.EPIC, epicVisitTypeId, epicAppointmentDuration).orElse(null);

						if (appointmentType != null)
							break;
					}
				}

				if (appointmentType == null) {
					getLogger().warn("Unable to find a matching appointment type for EPIC appointment with CSN {}, cannot create new appointment.", csn);
					continue;
				}

				UUID appointmentId = UUID.randomUUID();
				UUID providerId = provider.getProviderId();

				// It's possible this appointment already exists for other accounts, like in the scenario where
				// an appointment is initially booked on behalf of a patient by a user who signs in anonymously to "manually" set things up.
				// And then later the real patient signs in and views her calendar.
				// We don't want to detect that as an updated appointment and generate new BJ link and send out a confusing "your appointment was updated" email, we want to quietly
				// duplicate the existing appointment, but for this account.
				List<Appointment> duplicateAppointmentsForOtherAccounts = getDatabase().queryForList("SELECT * FROM appointment WHERE account_id != ? AND canceled=FALSE " +
						"AND start_time=? AND epic_contact_id=?", Appointment.class, accountId, meetingStartTime, csn);

				Appointment duplicateAppointmentForOtherAccount = duplicateAppointmentsForOtherAccounts.size() > 0 ? duplicateAppointmentsForOtherAccounts.get(0) : null;

				Long bluejeansMeetingId;
				String videoconferenceUrl;
				String bluejeansParticipantPasscode;
				UUID appointmentReasonId;
				String comment;
				UUID createdByAccountId;
				String phoneNumber;
				UUID intakeAssessmentId = null;

				if (duplicateAppointmentForOtherAccount == null) {
					MeetingResponse meetingResponse = null;

					// TODO: this assumes EPIC providers use Bluejeans for now, but if they eventually support other platforms, we'll need to support that here
					if (provider.getVideoconferencePlatformId() == VideoconferencePlatformId.BLUEJEANS) {
						meetingResponse = getBluejeansClient().scheduleMeetingForUser(provider.getBluejeansUserId().intValue(),
								appointmentType.getName(),
								account.getEmailAddress(),
								true,
								false,
								timeZone,
								meetingStartTime.atZone(timeZone).toInstant(),
								meetingEndTime.atZone(timeZone).toInstant()
						);
					}

					bluejeansMeetingId = meetingResponse == null ? null : (long) meetingResponse.getId();
					videoconferenceUrl = meetingResponse == null ? null : meetingResponse.meetingLinkWithAttendeePasscode();
					bluejeansParticipantPasscode = meetingResponse == null ? null : meetingResponse.getAttendeePasscode();
					appointmentReasonId = findNotSpecifiedAppointmentReasonByInstitutionId(provider.getInstitutionId()).getAppointmentReasonId();
					comment = null;
					createdByAccountId = accountId;
					phoneNumber = account.getPhoneNumber();
				} else {
					bluejeansMeetingId = duplicateAppointmentForOtherAccount.getBluejeansMeetingId();
					videoconferenceUrl = duplicateAppointmentForOtherAccount.getVideoconferenceUrl();
					bluejeansParticipantPasscode = duplicateAppointmentForOtherAccount.getBluejeansParticipantPasscode();
					appointmentReasonId = duplicateAppointmentForOtherAccount.getAppointmentReasonId();
					comment = duplicateAppointmentForOtherAccount.getComment();
					createdByAccountId = duplicateAppointmentForOtherAccount.getCreatedByAccountId();
					phoneNumber = duplicateAppointmentForOtherAccount.getPhoneNumber();
					intakeAssessmentId = duplicateAppointmentForOtherAccount.getIntakeAssessmentId();
				}

				getDatabase().execute("INSERT INTO appointment (appointment_id, provider_id, account_id, created_by_account_id, " +
								"appointment_type_id, acuity_appointment_id, acuity_class_id, bluejeans_meeting_id, bluejeans_participant_passcode, title, start_time, end_time, " +
								"duration_in_minutes, time_zone, videoconference_url, videoconference_platform_id, epic_contact_id, epic_contact_id_type, " +
								"phone_number, appointment_reason_id, comment, intake_assessment_id, scheduling_system_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
						appointmentId, providerId, accountId, createdByAccountId, appointmentType.getAppointmentTypeId(), null, null, bluejeansMeetingId, bluejeansParticipantPasscode,
						appointmentType.getName(), meetingStartTime, meetingEndTime, appointmentType.getDurationInMinutes(), timeZone, videoconferenceUrl, provider.getVideoconferencePlatformId(), csn, "CSN", phoneNumber, appointmentReasonId, comment, intakeAssessmentId, appointmentType.getSchedulingSystemId());

				Map<String, Object> createPayload = new HashMap<>();
				createPayload.put("csn", csn);
				createPayload.put("appointmentId", appointmentId);

				AuditLog createAuditLog = new AuditLog();
				createAuditLog.setAccountId(account.getAccountId());
				createAuditLog.setAuditLogEventId(AuditLogEventId.EPIC_APPOINTMENT_IMPLICIT_CREATE);

				if (duplicateAppointmentForOtherAccount == null)
					createAuditLog.setMessage(format("Created local appointment ID %s because we found CSN %s in EPIC", appointmentId, csn));
				else
					createAuditLog.setMessage(format("Created local appointment ID %s because we found CSN %s in EPIC. Note: this duplicates appointment ID %s", appointmentId, csn, duplicateAppointmentForOtherAccount.getAppointmentId()));

				createAuditLog.setPayload(getJsonMapper().toJson(createPayload));
				getAuditLogService().audit(createAuditLog);

				Locale locale = provider.getLocale() == null ? Locale.US : provider.getLocale();
				String providerEmailAddress = provider.getEmailAddress();
				String patientEmailAddress = account.getEmailAddress();

				Map<String, Object> messageContext = new HashMap<>();
				messageContext.put("appointmentId", appointmentId);
				messageContext.put("appointmentDateDescription", getFormatter().formatDate(epicAppointmentDate, FormatStyle.MEDIUM, locale));
				messageContext.put("appointmentTimeDescription", getFormatter().formatTime(epicAppointmentStartTime, FormatStyle.MEDIUM, locale));
				messageContext.put("videoconferenceUrl", videoconferenceUrl);

				boolean sendEmails = provider.getVideoconferencePlatformId() != VideoconferencePlatformId.SWITCHBOARD && duplicateAppointmentForOtherAccount == null;

				getDatabase().currentTransaction().get().addPostCommitOperation(() -> {
					// Only send out "appointment was updated" email for non-duplicate appointments
					if (sendEmails) {
						// Notify patient and provider
						getEmailMessageManager().enqueueMessage(new EmailMessage.Builder(EmailMessageTemplate.APPOINTMENT_UPDATE, locale)
								.toAddresses(new ArrayList<>() {{
									add(providerEmailAddress);
								}})
								.messageContext(messageContext)
								.build());

						if (patientEmailAddress != null) {
							getEmailMessageManager().enqueueMessage(new EmailMessage.Builder(EmailMessageTemplate.APPOINTMENT_UPDATE, locale)
									.toAddresses(new ArrayList<>() {{
										add(patientEmailAddress);
									}})
									.messageContext(messageContext)
									.build());
						}
					}

					ForkJoinPool.commonPool().execute(() -> {
						getEpicSyncManager().syncProviderAvailability(providerId, epicAppointmentDate);
					});
				});
			}

			// Re-query appointments to pull the latest
			appointments = getDatabase().queryForList("SELECT * FROM appointment WHERE account_id=? AND canceled=FALSE " +
					"AND start_time >= ? ORDER BY start_time", Appointment.class, accountId, LocalDateTime.now(timeZone));
		}

		return appointments;
	}

	@Nonnull
	public Map<String, Appointment> findAppointmentsForGroupEvents(@Nullable UUID accountId,
																																 @Nullable List<String> groupEventIds) {
		if (accountId == null || groupEventIds == null || groupEventIds.size() == 0)
			return Collections.emptyMap();

		List<Long> accuityClassIds = groupEventIds.stream().map((groupEventId) -> Long.valueOf(groupEventId)).collect(Collectors.toList());

		List<Object> parameters = new ArrayList<>(accuityClassIds.size() + 1);
		parameters.add(accountId);
		parameters.addAll(accuityClassIds);

		List<Appointment> appointments = getDatabase().queryForList(format("SELECT * FROM appointment WHERE canceled=FALSE AND account_id=? AND acuity_class_id IN %s",
				sqlInListPlaceholders(accuityClassIds)), Appointment.class, parameters.toArray(new Object[]{}));

		Map<String, Appointment> appointmentsByGroupEventId = new HashMap<>(appointments.size());

		for (Appointment appointment : appointments)
			appointmentsByGroupEventId.put(String.valueOf(appointment.getAcuityClassId()), appointment);

		return appointmentsByGroupEventId;
	}

	@Nonnull
	public Optional<AppointmentType> findAppointmentTypeById(@Nullable UUID appointmentTypeId) {
		if (appointmentTypeId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM v_appointment_type " +
				"WHERE appointment_type_id=?", AppointmentType.class, appointmentTypeId);
	}

	@Nonnull
	public Optional<AppointmentType> findAppointmentTypeByIdEvenIfDeleted(@Nullable UUID appointmentTypeId) {
		if (appointmentTypeId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT app_type.*, ata.assessment_id
				FROM appointment_type app_type
				LEFT OUTER JOIN appointment_type_assessment ata ON app_type.appointment_type_id = ata.appointment_type_id
				WHERE app_type.appointment_type_id=?
				AND (ata.assessment_id IS NULL OR ata.active=TRUE)
				""", AppointmentType.class, appointmentTypeId);
	}

	@Nonnull
	public Optional<AppointmentType> findAppointmentTypeByAcuityAppointmentTypeId(@Nullable Long acuityAppointmentTypeId) {
		if (acuityAppointmentTypeId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM v_appointment_type " +
				"WHERE acuity_appointment_type_id=?", AppointmentType.class, acuityAppointmentTypeId);
	}

	@Nonnull
	public List<AppointmentType> findAppointmentTypesByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT DISTINCT vat.* FROM v_appointment_type vat, provider_appointment_type pat, provider p " +
				"WHERE vat.appointment_type_id=pat.appointment_type_id AND pat.provider_id=p.provider_id AND p.institution_id=? " +
				"ORDER BY vat.appointment_type_id", AppointmentType.class, institutionId);
	}

	@Nonnull
	public List<AppointmentType> findAppointmentTypesByProviderId(@Nullable UUID providerId) {
		if (providerId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT app_type.* FROM provider_appointment_type pat, v_appointment_type app_type " +
				"WHERE pat.provider_id=? AND pat.appointment_type_id=app_type.appointment_type_id " +
				"ORDER BY pat.display_order", AppointmentType.class, providerId);
	}

	@Nonnull
	public List<EpicDepartment> findEpicDepartmentsByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM epic_department " +
				"WHERE institution_id=? ORDER BY name", EpicDepartment.class, institutionId);
	}

	@Nonnull
	public List<EpicDepartment> findEpicDepartmentsByProviderId(@Nullable UUID providerId) {
		if (providerId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT ed.* FROM epic_department ed, provider_epic_department ped " +
				"WHERE ped.provider_id=? AND ped.epic_department_id=ed.epic_department_id ORDER BY ed.name", EpicDepartment.class, providerId);
	}

	@Nonnull
	public Optional<EpicDepartment> findEpicDepartmentByProviderIdAndTimeslot(@Nullable UUID providerId,
																																						@Nullable LocalDateTime timeslot) {
		if (providerId == null || timeslot == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT DISTINCT ed.* FROM epic_department ed, provider_availability pa " +
				"WHERE pa.provider_id=? AND pa.epic_department_id=ed.epic_department_id AND pa.date_time=?", EpicDepartment.class, providerId, timeslot);
	}

	@Nonnull
	public UUID createAcuityAppointmentType(@Nonnull CreateAcuityAppointmentTypeRequest request) {
		requireNonNull(request);

		Long acuityAppointmentTypeId = request.getAcuityAppointmentTypeId();
		String name = trimToNull(request.getName());
		String description = trimToNull(request.getDescription());
		Long durationInMinutes = request.getDurationInMinutes();
		Boolean deleted = request.getDeleted() == null ? false : request.getDeleted();
		UUID appointmentTypeId = UUID.randomUUID();
		ValidationException validationException = new ValidationException();

		if (acuityAppointmentTypeId == null)
			validationException.add(new FieldError("acuityAppointmentTypeId", "Acuity appointment type ID is required."));

		if (name == null)
			validationException.add(new FieldError("name", "Name is required."));

		if (durationInMinutes == null)
			validationException.add(new FieldError("durationInMinutes", "Duration in minutes is required."));
		else if (durationInMinutes <= 0)
			validationException.add(new FieldError("durationInMinutes", "Duration in minutes is invalid (must be > 0)."));

		if (validationException.hasErrors())
			throw validationException;

		VisitTypeId visitTypeId = VisitTypeId.fromAcuityName(name);

		getDatabase().execute("INSERT INTO appointment_type (appointment_type_id, acuity_appointment_type_id, visit_type_id, " +
						"name, description, duration_in_minutes, deleted) VALUES (?,?,?,?,?,?,?)", appointmentTypeId, acuityAppointmentTypeId,
				visitTypeId, name, description, durationInMinutes, deleted);

		return appointmentTypeId;
	}

	@Nonnull
	public Boolean updateAcuityAppointmentType(@Nonnull UpdateAcuityAppointmentTypeRequest request) {
		requireNonNull(request);

		UUID appointmentTypeId = request.getAppointmentTypeId();
		String name = trimToNull(request.getName());
		String description = trimToNull(request.getDescription());
		Long durationInMinutes = request.getDurationInMinutes();
		Boolean deleted = request.getDeleted() == null ? false : request.getDeleted();
		AppointmentType appointmentType = null;
		ValidationException validationException = new ValidationException();

		if (appointmentTypeId == null) {
			validationException.add(new FieldError("appointmentTypeId", "Appointment type ID is required."));
		} else {
			appointmentType = findAppointmentTypeById(appointmentTypeId).orElse(null);

			if (appointmentType == null)
				validationException.add(new FieldError("appointmentTypeId", "Appointment type ID is invalid."));
		}

		if (name == null)
			validationException.add(new FieldError("name", "Name is required."));

		if (durationInMinutes == null)
			validationException.add(new FieldError("durationInMinutes", "Duration in minutes is required."));
		else if (durationInMinutes <= 0)
			validationException.add(new FieldError("durationInMinutes", "Duration in minutes is invalid (must be > 0)."));

		if (validationException.hasErrors())
			throw validationException;

		return getDatabase().execute("UPDATE appointment_type SET name=?, description=?, duration_in_minutes=?, deleted=? " +
				"WHERE appointment_type_id=?", name, description, durationInMinutes, deleted, appointmentTypeId) > 0;
	}

	@Nonnull
	public UUID rescheduleAppointment(@Nonnull UpdateAppointmentRequest request) {
		requireNonNull(request);

		ValidationException validationException = new ValidationException();
		Optional<Appointment> appointment = findAppointmentById(request.getAppointmentId());

		if (!appointment.isPresent())
			validationException.add(new FieldError("appointmentId", "Not a valid appointment"));
		else if (appointment.get().getCanceled())
			validationException.add(new FieldError("canceled", "Canceled appointments cannot be edited"));

		if (validationException.hasErrors())
			throw validationException;

		UUID newAppointmentId = createAppointment(request);

		CancelAppointmentRequest cancelRequest = new CancelAppointmentRequest();
		cancelRequest.setAppointmentId(request.getAppointmentId());
		cancelRequest.setAccountId(request.getAccountId());
		cancelRequest.setCanceledByWebhook(false);
		cancelRequest.setCanceledForReschedule(true);
		cancelRequest.setRescheduleAppointmentId(newAppointmentId);

		cancelAppointment(cancelRequest);

		return newAppointmentId;
	}

	@Nonnull
	public UUID createAppointment(@Nonnull CreateAppointmentRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		UUID providerId = request.getProviderId();
		UUID createdByAccountId = request.getCreatedByAcountId();
		UUID appointmentReasonId = request.getAppointmentReasonId();
		LocalDate date = request.getDate();
		LocalTime time = request.getTime();
		String groupEventId = trimToNull(request.getGroupEventId());
		UUID groupEventTypeId = request.getGroupEventTypeId();
		UUID appointmentTypeId = request.getAppointmentTypeId();
		UUID intakeAssessmentId = request.getIntakeAssessmentId();
		String emailAddress = getNormalizer().normalizeEmailAddress(request.getEmailAddress()).orElse(null);
		String phoneNumber = trimToNull(request.getPhoneNumber());
		String comment = trimToNull(request.getComment());
		Account account = null;
		Provider provider = null;
		AppointmentType appointmentType = null;
		UUID appointmentId = UUID.randomUUID();

		ValidationException validationException = new ValidationException();

		if (accountId == null) {
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));
		} else {
			account = getAccountService().findAccountById(accountId).orElse(null);

			if (account == null)
				validationException.add(new FieldError("accountId", getStrings().get("Account ID is invalid.")));
		}

		if (date == null)
			validationException.add(new FieldError("date", getStrings().get("Date is required.")));

		if (time == null)
			validationException.add(new FieldError("time", getStrings().get("Time is required.")));

		// If account has no email address and none was passed in, force user to provide one (unless they are IC users...then it's optional)
		if (emailAddress == null && account != null && account.getEmailAddress() == null && account.getSourceSystemId() != SourceSystemId.IC) {
			validationException.add(new FieldError("emailAddress", getStrings().get("An email address is required to book an appointment.")));

			Map<String, Object> metadata = new HashMap<>();
			metadata.put("accountEmailAddressRequired", true);

			validationException.setMetadata(metadata);
		}

		boolean oneOnOne = groupEventId == null;

		if (phoneNumber == null && account != null && account.getPhoneNumber() == null && oneOnOne) {
			boolean psychiatrist = getProviderService().findSupportRolesByProviderId(providerId).stream()
					.anyMatch(role -> role.getSupportRoleId().equals(SupportRoleId.PSYCHIATRIST));

			if (psychiatrist) {
				validationException.add(new FieldError("phoneNumber", getStrings().get("A phone number is required to book an appointment.")));

				Map<String, Object> metadata = new HashMap<>();
				metadata.put("accountPhoneNumberRequired", true);

				validationException.setMetadata(metadata);
			}
		}

		if ((groupEventId == null && groupEventTypeId != null) || (groupEventId != null && groupEventTypeId == null))
			validationException.add(getStrings().get("You must specify both 'groupEventId' and 'groupEventTypeId' when booking an appointment for a studio session."));

		if (providerId == null && groupEventId == null)
			validationException.add(getStrings().get("You must specify either 'providerId' (1:1) or 'groupEventId' and 'groupEventTypeId' (studio session) when booking an appointment."));

		if (oneOnOne) {
			if (appointmentTypeId == null) {
				validationException.add(new FieldError("appointmentTypeId", getStrings().get("Appointment type ID is required for 1:1 appointments.")));
			} else {
				appointmentType = findAppointmentTypeById(appointmentTypeId).orElse(null);

				if (appointmentType == null)
					validationException.add(new FieldError("appointmentTypeId", getStrings().get("Appointment type ID is invalid.")));
			}
		}

		AccountSession intakeSession = getSessionService().findCurrentIntakeAssessmentForAccountAndProvider(account,
				providerId, appointmentTypeId, true).orElse(null);

		if (oneOnOne && providerId != null && getAssessmentService().findIntakeAssessmentByProviderId(providerId, appointmentTypeId).isPresent()) {
			if (intakeSession != null && !getAssessmentScoringService().isBookingAllowed(intakeSession))
				validationException.add(getStrings().get("Based on your responses you are not permitted to book with this provider."));
			else if (intakeSession == null)
				validationException.add(getStrings().get("You did not answer the necessary intake questions to book with this provider."));
		}

		if (validationException.hasErrors())
			throw validationException;

		if (appointmentReasonId == null) {
			// TODO: once we are rid of Acuity appointments we can assume provider is always non-null.
			// Until then, failsafe check against provider
			InstitutionId institutionId = provider == null ? InstitutionId.COBALT : provider.getInstitutionId();
			appointmentReasonId = findNotSpecifiedAppointmentReasonByInstitutionId(institutionId).getAppointmentReasonId();
		}

		// If email address was provided, update the account's email on file
		if (emailAddress != null) {
			String pinnedEmailAddress = emailAddress;
			getAccountService().updateAccountEmailAddress(new UpdateAccountEmailAddressRequest() {{
				setAccountId(accountId);
				setEmailAddress(pinnedEmailAddress);
			}});
		} else {
			emailAddress = account.getEmailAddress();
		}

		if (!getAccountService().isEmailAddressVerifiedForAccountId(emailAddress, accountId))
			throw new ValidationException(getStrings().get("Sorry, you must validate your email address before booking an appointment."));

		// If phone number was provided and account has no phone number, permit updating the account's phone number on file
		if (phoneNumber != null && account.getPhoneNumber() == null) {
			String pinnedPhoneNumber = phoneNumber;
			getAccountService().updateAccountPhoneNumber(new UpdateAccountPhoneNumberRequest() {{
				setAccountId(accountId);
				setPhoneNumber(pinnedPhoneNumber);
			}});
		} else {
			phoneNumber = account.getPhoneNumber();
		}

		// Special handling for special clinic
		boolean isCalmingAnAnxiousMindIntakeAppointment = oneOnOne && providerId != null && getClinicService().isProviderPartOfCalmingAnAnxiousMindClinic(providerId);

		AcuityAppointmentType acuityAppointmentType = null;
		Long acuityClassId = null;
		Long acuityCalendarId = null;
		String videoconferenceUrl = null;
		ZoneId timeZone = null;

		if (oneOnOne) {
			provider = getProviderService().findProviderById(providerId).get();
			timeZone = provider.getTimeZone();
			acuityCalendarId = provider.getAcuityCalendarId();

			// Special handling for Acuity - read the latest value for appointment type
			if (appointmentType.getSchedulingSystemId() == SchedulingSystemId.ACUITY)
				// TODO: use cache here
				acuityAppointmentType = getAcuitySchedulingClient().findAppointmentTypeById(appointmentType.getAcuityAppointmentTypeId()).get();
		}

		Long durationInMinutes = null;
		String title = null;

		if (appointmentType.getSchedulingSystemId() == SchedulingSystemId.ACUITY) {
			durationInMinutes = acuityAppointmentType.getDuration().longValue();
			title = acuityAppointmentType.getName();
		} else {
			durationInMinutes = appointmentType.getDurationInMinutes();
			title = appointmentType.getName();
		}

		LocalDateTime meetingStartTime = LocalDateTime.of(date, time);
		LocalDateTime meetingEndTime = meetingStartTime.plusMinutes(durationInMinutes);

		MeetingResponse meetingResponse = null;
		Long bluejeansMeetingId = null;
		String bluejeansParticipantPasscode = null;
		String appointmentPhoneNumber = null;
		VideoconferencePlatformId videoconferencePlatformId = provider == null ? VideoconferencePlatformId.BLUEJEANS : provider.getVideoconferencePlatformId();

		// Only create a Bluejeans meeting for 1:1 meetings.
		// Other types (group events) are created outside of our system
		if (oneOnOne) {
			if (provider != null) {
				if (videoconferencePlatformId == VideoconferencePlatformId.BLUEJEANS) {
					meetingResponse = getBluejeansClient().scheduleMeetingForUser(provider.getBluejeansUserId().intValue(),
							title,
							emailAddress,
							true,
							false,
							timeZone,
							meetingStartTime.atZone(timeZone).toInstant(),
							meetingEndTime.atZone(timeZone).toInstant()
					);

					bluejeansMeetingId = (long) meetingResponse.getId();
					bluejeansParticipantPasscode = meetingResponse.getAttendeePasscode();
					videoconferenceUrl = meetingResponse.meetingLinkWithAttendeePasscode();
				} else if (videoconferencePlatformId == VideoconferencePlatformId.TELEPHONE) {
					// Hack: phone number is encoded as the URL in the provider sheet.
					// The real URL is the webapp - we have a `GET /appointments/{appointmentId}`
					appointmentPhoneNumber = provider.getVideoconferenceUrl();
					videoconferenceUrl = format("%s/appointments/%s", getInstitutionService().findWebappBaseUrlByInstitutionId(provider.getInstitutionId()).get(), appointmentId);

				} else if (videoconferencePlatformId == VideoconferencePlatformId.EXTERNAL) {
					videoconferenceUrl = provider.getVideoconferenceUrl();
				}
			}
		}

		String firstName = account.getFirstName();
		String lastName = account.getLastName();

		if (firstName == null)
			firstName = getStrings().get("Anonymous");
		if (lastName == null)
			lastName = getStrings().get("User");

		AcuityAppointment acuityAppointment = null;
		ScheduleAppointmentWithInsuranceResponse appointmentResponse = null;

		if (appointmentType.getSchedulingSystemId() == SchedulingSystemId.ACUITY) {
			AcuityAppointmentCreateRequest acuityAppointmentCreateRequest = new AcuityAppointmentCreateRequest();
			acuityAppointmentCreateRequest.setAppointmentTypeId(acuityAppointmentType.getId());
			acuityAppointmentCreateRequest.setCalendarId(acuityCalendarId);
			acuityAppointmentCreateRequest.setDatetime(meetingStartTime);
			// Acuity does not permit nullable email addresses
			acuityAppointmentCreateRequest.setEmail(emailAddress == null ? getConfiguration().getDefaultEmailToAddress(account.getInstitutionId()) : emailAddress);
			acuityAppointmentCreateRequest.setFirstName(firstName);
			acuityAppointmentCreateRequest.setLastName(lastName);
			acuityAppointmentCreateRequest.setTimeZone(timeZone);

			// Special support for including Bluejeans URLs - we use a custom form field to do it
			AcuityAppointmentFieldCreateRequest fieldCreateRequest = new AcuityAppointmentFieldCreateRequest();
			fieldCreateRequest.setId(getConfiguration().getAcuityVideoconferenceFormFieldId());
			fieldCreateRequest.setValue(videoconferenceUrl);

			acuityAppointmentCreateRequest.setFields(List.of(fieldCreateRequest));

			try {
				acuityAppointment = getAcuitySchedulingClient().createAppointment(acuityAppointmentCreateRequest);
			} catch (Exception e) {
				getLogger().info("An error occurred during appointment creation", e);

				if (bluejeansMeetingId != null) {
					getLogger().info("Now cleaning up Bluejeans meeting...", e);

					try {
						getBluejeansClient().cancelScheduledMeeting(provider.getBluejeansUserId().intValue(), bluejeansMeetingId.intValue(), false, null);
					} catch (Exception meetingDeleteException) {
						getLogger().warn("Unable to delete Bluejeans meeting, continuing on...", meetingDeleteException);
					}
				}

				if (e instanceof AcuitySchedulingNotAvailableException)
					throw new ValidationException(getStrings().get("Sorry, this booking time is no longer available. Please choose a different time."));

				// If we have a different exception from Acuity, then handle it specially
				if (e instanceof AcuitySchedulingException) {
					AcuityError acuityError = ((AcuitySchedulingException) e).getAcuityError().orElse(null);
					if (acuityError != null && trimToNull(acuityError.getMessage()) != null)
						throw new ValidationException(acuityError.getMessage());
				}

				throw new RuntimeException("Unexpected error occurred creating Acuity appointment", e);
			}
		} else if (appointmentType.getSchedulingSystemId() == SchedulingSystemId.EPIC) {
			try {
				EnterprisePlugin enterprisePlugin = enterprisePluginProvider.enterprisePluginForInstitutionId(account.getInstitutionId());
				Institution institution = getInstitutionService().findInstitutionById(account.getInstitutionId()).get();
				EpicClient epicClient = enterprisePlugin.epicClientForBackendService().get();

				// SYNC ACCOUNT UID
				String uid = epicClient.determineLatestUIDForPatientIdentifier(account.getEpicPatientId(), account.getEpicPatientIdType()).get();

				if (!uid.equals(account.getEpicPatientId()))
					getAccountService().updateAccountEpicPatient(account.getAccountId(), uid, "UID");

				account = getAccountService().findAccountById(accountId).get();

				// Figure out the department for this provider and timeslot
				EpicDepartment epicDepartment = findEpicDepartmentByProviderIdAndTimeslot(providerId, LocalDateTime.of(date, time)).orElse(null);

				// Should not occur...
				if (epicDepartment == null)
					throw new IllegalStateException(format("Cannot find an EPIC department for this provider/timeslot: %s / %s", providerId, LocalDateTime.of(date, time)));

				// Double-check provider schedule to make sure we can book
				GetProviderScheduleRequest scheduleRequest = new GetProviderScheduleRequest();
				scheduleRequest.setProviderID(provider.getEpicProviderId());
				scheduleRequest.setProviderIDType(provider.getEpicProviderIdType());
				scheduleRequest.setDepartmentID(epicDepartment.getDepartmentId());
				scheduleRequest.setDepartmentIDType(epicDepartment.getDepartmentIdType());
				scheduleRequest.setUserID(institution.getEpicUserId());
				scheduleRequest.setUserIDType(institution.getEpicUserIdType());
				scheduleRequest.setDate(date);

				GetProviderScheduleResponse scheduleResponse = epicClient.performGetProviderSchedule(scheduleRequest);

				boolean slotStillOpen = false;

				for (GetProviderScheduleResponse.ScheduleSlot scheduleSlot : scheduleResponse.getScheduleSlots()) {
					LocalTime slotTime = epicClient.parseTimeAmPm(scheduleSlot.getStartTime());
					Integer slotAvailableOpenings = Integer.valueOf(scheduleSlot.getAvailableOpenings());
					LocalDateTime slotDateTime = LocalDateTime.of(date, slotTime);

					if (slotDateTime.equals(LocalDateTime.of(date, time)) && slotAvailableOpenings > 0) {
						slotStillOpen = true;
						break;
					}
				}

				if (!slotStillOpen)
					throw new ValidationException(getStrings().get("Sorry, this time is currently unavailable for booking.  Please choose a different time."));

				// Now we are ready to book
				ScheduleAppointmentWithInsuranceRequest appointmentRequest = new ScheduleAppointmentWithInsuranceRequest();
				appointmentRequest.setPatientID(account.getEpicPatientId());
				appointmentRequest.setPatientIDType(account.getEpicPatientIdType());
				appointmentRequest.setDepartmentID(epicDepartment.getDepartmentId());
				appointmentRequest.setDepartmentIDType(epicDepartment.getDepartmentIdType());
				appointmentRequest.setProviderID(provider.getEpicProviderId());
				appointmentRequest.setProviderIDType(provider.getEpicProviderIdType());
				appointmentRequest.setVisitTypeID(appointmentType.getEpicVisitTypeId());
				appointmentRequest.setVisitTypeIDType(appointmentType.getEpicVisitTypeIdType());
				appointmentRequest.setDate(epicClient.formatDateWithHyphens(date));
				appointmentRequest.setTime(epicClient.formatTimeInMilitary(time));
				appointmentRequest.setIsReviewOnly(false);

				if (videoconferenceUrl != null)
					appointmentRequest.setComments(List.of(format("Videoconference URL: %s", videoconferenceUrl)));

				appointmentResponse = epicClient.performScheduleAppointmentWithInsurance(appointmentRequest);
			} catch (Exception e) {
				getLogger().info("An error occurred during appointment creation", e);

				if (bluejeansMeetingId != null) {
					getLogger().info("Now cleaning up Bluejeans meeting...", e);

					try {
						getBluejeansClient().cancelScheduledMeeting(provider.getBluejeansUserId().intValue(), bluejeansMeetingId.intValue(), false, null);
					} catch (Exception meetingDeleteException) {
						getLogger().warn("Unable to delete Bluejeans meeting, continuing on...", meetingDeleteException);
					}
				}

				throw e;
			}
		} else if (appointmentType.getSchedulingSystemId() == SchedulingSystemId.COBALT) {
			// Nothing to do for now
		} else {
			throw new RuntimeException(format("Unexpected value %s.%s provided", SchedulingSystemId.class.getSimpleName(), appointmentType.getSchedulingSystemId().name()));
		}

		Long acuityAppointmentId = acuityAppointment == null ? null : acuityAppointment.getId();
		String epicContactId = null;
		String epicContactIdType = null;

		if (appointmentResponse != null) {
			for (ScheduleAppointmentWithInsuranceResponse.Appointment.ContactID contactID : appointmentResponse.getAppointment().getContactIDs()) {
				if ("CSN".equals(contactID.getType())) {
					epicContactId = contactID.getID();
					epicContactIdType = contactID.getType();
					break;
				}
			}

			Map<String, Object> payload = new HashMap<>();
			payload.put("csn", epicContactId);
			payload.put("appointmentId", appointmentId);
			payload.put("request", request);

			AuditLog auditLog = new AuditLog();
			auditLog.setAccountId(account.getAccountId());
			auditLog.setAuditLogEventId(AuditLogEventId.EPIC_APPOINTMENT_CREATE);
			auditLog.setPayload(getJsonMapper().toJson(payload));
			getAuditLogService().audit(auditLog);
		}

		Optional<Assessment> intakeAssessment = getAssessmentService().findAssessmentById(intakeAssessmentId);
		UUID intakeAccountSessionId = null;
		if (intakeAssessment.isPresent())
			intakeAccountSessionId = getSessionService().findCurrentAccountSessionForAssessment(account, intakeAssessment.get()).get().getAccountSessionId();

		getDatabase().execute("INSERT INTO appointment (appointment_id, provider_id, account_id, created_by_account_id, " +
						"appointment_type_id, acuity_appointment_id, acuity_class_id, bluejeans_meeting_id, bluejeans_participant_passcode, title, start_time, end_time, " +
						"duration_in_minutes, time_zone, videoconference_url, epic_contact_id, epic_contact_id_type, videoconference_platform_id, " +
						"phone_number, appointment_reason_id, comment, intake_assessment_id, scheduling_system_id, intake_account_session_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", appointmentId, providerId,
				accountId, createdByAccountId, appointmentTypeId, acuityAppointmentId, acuityClassId, bluejeansMeetingId, bluejeansParticipantPasscode,
				title, meetingStartTime, meetingEndTime, durationInMinutes, timeZone, videoconferenceUrl, epicContactId,
				epicContactIdType, videoconferencePlatformId, appointmentPhoneNumber, appointmentReasonId, comment, intakeAssessmentId, appointmentType.getSchedulingSystemId(), intakeAccountSessionId);

		if (provider != null) {
			sendProviderScoreEmail(provider, account, emailAddress, phoneNumber, videoconferenceUrl,
					getFormatter().formatDate(meetingStartTime.toLocalDate()),
					getFormatter().formatTime(meetingStartTime.toLocalTime()),
					getFormatter().formatTime(meetingEndTime.toLocalTime()), intakeSession);
		}

		Provider pinnedProvider = provider;
		Account pinnedPatientAccount = account;
		String pinnedEmailAddress = emailAddress;
		ZoneId pinnedTimeZone = timeZone;
		String pinnedVideoconferenceUrl = videoconferenceUrl;
		SchedulingSystemId schedulingSystemId = appointmentType.getSchedulingSystemId();

		getDatabase().currentTransaction().get().addPostCommitOperation(() -> {
			if (schedulingSystemId == SchedulingSystemId.ACUITY) {
				getAcuitySchedulingCache().invalidateAvailability(meetingStartTime.toLocalDate(), pinnedTimeZone);

				// Kick off a manual resync for this date just in case there's some disconnect
				if (providerId != null)
					ForkJoinPool.commonPool().execute(() -> {
						getAcuitySyncManager().syncProviderAvailability(providerId, meetingStartTime.toLocalDate());
					});
			} else if (schedulingSystemId == SchedulingSystemId.EPIC) {
				ForkJoinPool.commonPool().execute(() -> {
					getEpicSyncManager().syncProviderAvailability(providerId, meetingStartTime.toLocalDate());
				});
			} else if (schedulingSystemId == SchedulingSystemId.COBALT) {
				// For native appointments, we are responsible for sending emails out
				sendPatientAndProviderCobaltAppointmentCreatedEmails(appointmentId);
			}

			if (isCalmingAnAnxiousMindIntakeAppointment && pinnedEmailAddress != null && videoconferencePlatformId != VideoconferencePlatformId.SWITCHBOARD) {
				// Send custom email to CTSA intake patients
				EmailMessage emailMessage = new EmailMessage.Builder(EmailMessageTemplate.APPOINTMENT_CREATED_CTSA_PATIENT, pinnedPatientAccount.getLocale())
						.toAddresses(Collections.singletonList(pinnedEmailAddress))
						.messageContext(new HashMap<String, Object>() {{
							String webappBaseUrl = getInstitutionService().findWebappBaseUrlByInstitutionId(pinnedProvider.getInstitutionId()).get();

							String providerNameAndCredentials = pinnedProvider.getName();

							if (pinnedProvider.getLicense() != null)
								providerNameAndCredentials = format("%s, %s", pinnedProvider.getName(), pinnedProvider.getLicense());

							put("appointmentId", appointmentId);
							put("providerName", pinnedProvider.getName());
							put("providerNameAndCredentials", providerNameAndCredentials);
							put("videoconferenceUrl", pinnedVideoconferenceUrl);
							put("imageUrl", firstNonNull(pinnedProvider.getImageUrl(), getConfiguration().getDefaultProviderImageUrlForEmail()));
							put("patientName", Normalizer.normalizeName(pinnedPatientAccount.getFirstName(), pinnedPatientAccount.getLastName()).orElse(getStrings().get("Anonymous User")));
							put("appointmentStartDateDescription", getFormatter().formatDate(meetingStartTime.toLocalDate()));
							put("appointmentStartTimeDescription", getFormatter().formatTime(meetingStartTime.toLocalTime(), FormatStyle.SHORT));
							put("cancelUrl", format("%s/my-calendar?appointmentId=%s&action=cancel", webappBaseUrl, appointmentId));
							put("icalUrl", format("%s/appointments/%s/ical", webappBaseUrl, appointmentId));
							put("googleCalendarUrl", format("%s/appointments/%s/google-calendar", webappBaseUrl, appointmentId));
							put("anotherTimeUrl", format("%s/connect-with-support", webappBaseUrl));
						}})
						.build();

				getEmailMessageManager().enqueueMessage(emailMessage);
			}
		});

		//Send any patient appointment interactions that might be defined for the provider being scheduled with
		List<Interaction> patientInteractions = getProviderService().findInteractionsByTypeAndProviderId(InteractionType.InteractionTypeId.APPOINTMENT_PATIENT, providerId);

		if (!patientInteractions.isEmpty()) {
			for (Interaction interaction : patientInteractions) {
				LocalDateTime followupSendTime = meetingEndTime.plusMinutes(interaction.getSendOffsetInMinutes());
				List<UUID> additionalAccountsToNotify = new ArrayList<>();
				additionalAccountsToNotify.add(createdByAccountId);

				UUID interactionInstanceId = getInteractionService().createInteractionInstance(new CreateInteractionInstanceRequest() {{
					setStartDateTime(followupSendTime);
					setTimeZone(pinnedTimeZone);
					setInteractionId(interaction.getInteractionId());
					setAdditionalAccountsToNotify(additionalAccountsToNotify);
				}});

				getInteractionService().linkInteractionInstanceToAppointment(interactionInstanceId, appointmentId);
			}
		}

		//Send any provider appointment interactions that might be defined for the provider being scheduled with
		List<Interaction> providerInteractions = getProviderService().findInteractionsByTypeAndProviderId(InteractionType.InteractionTypeId.APPOINTMENT_PROVIDER, providerId);

		if (!providerInteractions.isEmpty()) {
			for (Interaction interaction : providerInteractions) {
				LocalDateTime followupSendTime = meetingEndTime.plusMinutes(interaction.getSendOffsetInMinutes());
				List<UUID> additionalAccountsToNotify = new ArrayList<>();
				Optional<Account> providerAccount = getAccountService().findAccountByProviderId(providerId);

				if (providerAccount.isPresent())
					additionalAccountsToNotify.add(providerAccount.get().getAccountId());

				UUID interactionInstanceId = getInteractionService().createInteractionInstance(new CreateInteractionInstanceRequest() {{
					setStartDateTime(followupSendTime);
					setTimeZone(pinnedTimeZone);
					setInteractionId(interaction.getInteractionId());
					setAdditionalAccountsToNotify(additionalAccountsToNotify);
				}});

				getInteractionService().linkInteractionInstanceToAppointment(interactionInstanceId, appointmentId);
			}
		}

		return appointmentId;
	}

	@Nonnull
	public UUID createAppointmentType(@Nonnull CreateAppointmentTypeRequest request) {
		requireNonNull(request);

		UUID providerId = request.getProviderId();
		SchedulingSystemId schedulingSystemId = request.getSchedulingSystemId();
		VisitTypeId visitTypeId = request.getVisitTypeId();
		String name = trimToNull(request.getName());
		String description = trimToNull(request.getDescription());
		Long durationInMinutes = request.getDurationInMinutes();
		String hexColor = trimToNull(request.getHexColor());
		List<CreatePatientIntakeQuestionRequest> patientIntakeQuestions = request.getPatientIntakeQuestions() == null ? Collections.emptyList() : request.getPatientIntakeQuestions();
		List<CreateScreeningQuestionRequest> screeningQuestions = request.getScreeningQuestions() == null ? Collections.emptyList() : request.getScreeningQuestions();
		UUID appointmentTypeId = UUID.randomUUID();
		Provider provider;

		ValidationException validationException = new ValidationException();

		if (providerId == null) {
			validationException.add(new FieldError("providerId", getStrings().get("Provider ID is required.")));
		} else {
			provider = getProviderService().findProviderById(providerId).orElse(null);

			if (provider == null)
				validationException.add(new FieldError("providerId", getStrings().get("Provider ID is invalid.")));
		}

		if (name == null)
			validationException.add(new FieldError("name", getStrings().get("Name is required.")));

		if (description == null)
			validationException.add(new FieldError("description", getStrings().get("Nickname is required.")));

		if (hexColor == null)
			validationException.add(new FieldError("hexColor", getStrings().get("Hex color is required.")));
		else if (!isValidHexColor(hexColor))
			validationException.add(new FieldError("hexColor", getStrings().get("Hex color is invalid.")));

		if (schedulingSystemId == null)
			validationException.add(new FieldError("schedulingSystemId", getStrings().get("Scheduling System ID is required.")));
		else if (schedulingSystemId != SchedulingSystemId.COBALT)
			validationException.add(new FieldError("schedulingSystemId", getStrings().get("Sorry, the only supported Scheduling System ID is {{supportedSchedulingSystemId}}.", new HashMap<String, Object>() {{
				put("supportedSchedulingSystemId", SchedulingSystemId.COBALT.name());
			}})));

		if (visitTypeId == null)
			validationException.add(new FieldError("visitTypeId", getStrings().get("Visit Type ID is required.")));

		if (durationInMinutes == null)
			validationException.add(new FieldError("durationInMinutes", getStrings().get("Duration is required.")));
		else if (durationInMinutes < 5)
			validationException.add(new FieldError("durationInMinutes", getStrings().get("Duration is too small (minimum 5 minutes).")));
		else if (durationInMinutes > 60 * 24 /* arbitrary upper bound to prevent accidental errors */)
			validationException.add(new FieldError("durationInMinutes", getStrings().get("Duration is too large (maximum 24 hours).")));
		else if (durationInMinutes % 5 != 0 /* for now, enforce increments of 5 to make it simpler to handle edge cases */)
			validationException.add(new FieldError("durationInMinutes", getStrings().get("Only 5 minute increments are supported for appointment duration.")));

		for (int i = 0; i < patientIntakeQuestions.size(); ++i) {
			CreatePatientIntakeQuestionRequest patientIntakeQuestion = patientIntakeQuestions.get(i);

			if (patientIntakeQuestion == null)
				continue;

			String question = trimToNull(patientIntakeQuestion.getQuestion());
			FontSizeId fontSizeId = patientIntakeQuestion.getFontSizeId();
			int questionNumber = i + 1;

			if (question == null)
				validationException.add(new FieldError(format("patientIntakeQuestions[%d].question", questionNumber), getStrings().get("Question text is required for patient intake question {{questionNumber}}.", new HashMap<String, Object>() {{
					put("questionNumber", questionNumber);
				}})));

			if (fontSizeId == null)
				validationException.add(new FieldError(format("patientIntakeQuestions[%d].fontSizeId", questionNumber), getStrings().get("Font size is required for patient intake question {{questionNumber}}.", new HashMap<String, Object>() {{
					put("questionNumber", questionNumber);
				}})));
		}

		for (int i = 0; i < screeningQuestions.size(); ++i) {
			CreateScreeningQuestionRequest screeningIntakeQuestion = screeningQuestions.get(i);
			String question = trimToNull(screeningIntakeQuestion.getQuestion());
			FontSizeId fontSizeId = screeningIntakeQuestion.getFontSizeId();
			int questionNumber = i + 1;

			if (question == null)
				validationException.add(new FieldError(format("screeningQuestions[%d].question", questionNumber), getStrings().get("Question text is required for screening question {{questionNumber}}.", new HashMap<String, Object>() {{
					put("questionNumber", questionNumber);
				}})));

			if (fontSizeId == null)
				validationException.add(new FieldError(format("screeningQuestions[%d].fontSizeId", questionNumber), getStrings().get("Font size is required for screening question {{questionNumber}}.", new HashMap<String, Object>() {{
					put("questionNumber", questionNumber);
				}})));
		}

		if (validationException.hasErrors())
			throw validationException;

		Integer normalizedHexColor = getNormalizer().normalizeHexColor(hexColor).get();

		getDatabase().execute("INSERT INTO appointment_type (appointment_type_id, visit_type_id, " +
						"name, description, duration_in_minutes, scheduling_system_id, hex_color) VALUES (?,?,?,?,?,?,?)",
				appointmentTypeId, visitTypeId, name, description,
				durationInMinutes, schedulingSystemId, normalizedHexColor);

		getDatabase().execute("INSERT INTO provider_appointment_type (provider_id, appointment_type_id, display_order) " +
						"SELECT ?,?, COALESCE(MAX(display_order) + 1, 1) FROM provider_appointment_type WHERE provider_id=?",
				providerId, appointmentTypeId, providerId);

		// Build assessment, if needed

		// Normalize
		patientIntakeQuestions = patientIntakeQuestions.stream()
				.filter(patientIntakeQuestion -> patientIntakeQuestion != null)
				.collect(Collectors.toList());

		// Normalize
		screeningQuestions = screeningQuestions.stream()
				.filter(screeningQuestion -> screeningQuestion != null)
				.collect(Collectors.toList());

		if (patientIntakeQuestions.size() > 0 || screeningQuestions.size() > 0) {
			UUID assessmentId = createIntakeAssessmentForAppointmentTypeQuestions(screeningQuestions, patientIntakeQuestions);

			getDatabase().execute("INSERT INTO appointment_type_assessment (appointment_type_id, " +
					"assessment_id, active) VALUES (?,?,?)", appointmentTypeId, assessmentId, true);
		}

		return appointmentTypeId;
	}

	@Nonnull
	public Boolean updateAppointmentType(@Nonnull UpdateAppointmentTypeRequest request) {
		requireNonNull(request);

		UUID appointmentTypeId = request.getAppointmentTypeId();
		UUID providerId = request.getProviderId();
		SchedulingSystemId schedulingSystemId = request.getSchedulingSystemId();
		VisitTypeId visitTypeId = request.getVisitTypeId();
		String name = trimToNull(request.getName());
		String description = trimToNull(request.getDescription());
		Long durationInMinutes = request.getDurationInMinutes();
		String hexColor = trimToNull(request.getHexColor());
		List<CreatePatientIntakeQuestionRequest> patientIntakeQuestions = request.getPatientIntakeQuestions() == null ? Collections.emptyList() : request.getPatientIntakeQuestions();
		List<CreateScreeningQuestionRequest> screeningQuestions = request.getScreeningQuestions() == null ? Collections.emptyList() : request.getScreeningQuestions();
		Provider provider;

		ValidationException validationException = new ValidationException();

		if (appointmentTypeId == null)
			validationException.add(new FieldError("appointmentTypeId", getStrings().get("Appointment Type ID is required.")));

		if (providerId == null) {
			validationException.add(new FieldError("providerId", getStrings().get("Provider ID is required.")));
		} else {
			provider = getProviderService().findProviderById(providerId).orElse(null);

			if (provider == null)
				validationException.add(new FieldError("providerId", getStrings().get("Provider ID is invalid.")));
		}

		if (name == null)
			validationException.add(new FieldError("name", getStrings().get("Name is required.")));

		if (description == null)
			validationException.add(new FieldError("description", getStrings().get("Nickname is required.")));

		if (hexColor == null)
			validationException.add(new FieldError("hexColor", getStrings().get("Hex color is required.")));
		else if (!isValidHexColor(hexColor))
			validationException.add(new FieldError("hexColor", getStrings().get("Hex color is invalid.")));

		if (schedulingSystemId == null)
			validationException.add(new FieldError("schedulingSystemId", getStrings().get("Scheduling System ID is required.")));
		else if (schedulingSystemId != SchedulingSystemId.COBALT)
			validationException.add(new FieldError("schedulingSystemId", getStrings().get("Sorry, the only supported Scheduling System ID is {{supportedSchedulingSystemId}}.", new HashMap<String, Object>() {{
				put("supportedSchedulingSystemId", SchedulingSystemId.COBALT.name());
			}})));

		if (visitTypeId == null)
			validationException.add(new FieldError("visitTypeId", getStrings().get("Visit Type ID is required.")));

		if (durationInMinutes == null)
			validationException.add(new FieldError("durationInMinutes", getStrings().get("Duration is required.")));
		else if (durationInMinutes < 1)
			validationException.add(new FieldError("durationInMinutes", getStrings().get("Duration is too small.")));
		else if (durationInMinutes > 60 * 4 /* arbitrary upper bound to prevent accidental errors */)
			validationException.add(new FieldError("durationInMinutes", getStrings().get("Duration is too large.")));

		for (int i = 0; i < patientIntakeQuestions.size(); ++i) {
			CreatePatientIntakeQuestionRequest patientIntakeQuestion = patientIntakeQuestions.get(i);

			if (patientIntakeQuestion == null)
				continue;

			String question = trimToNull(patientIntakeQuestion.getQuestion());
			FontSizeId fontSizeId = patientIntakeQuestion.getFontSizeId();
			int questionNumber = i + 1;

			if (question == null)
				validationException.add(new FieldError(format("patientIntakeQuestions[%d].question", questionNumber), getStrings().get("Question text is required for patient intake question {{questionNumber}}.", new HashMap<String, Object>() {{
					put("questionNumber", questionNumber);
				}})));

			if (fontSizeId == null)
				validationException.add(new FieldError(format("patientIntakeQuestions[%d].fontSizeId", questionNumber), getStrings().get("Font size is required for patient intake question {{questionNumber}}.", new HashMap<String, Object>() {{
					put("questionNumber", questionNumber);
				}})));
		}

		for (int i = 0; i < screeningQuestions.size(); ++i) {
			CreateScreeningQuestionRequest screeningIntakeQuestion = screeningQuestions.get(i);
			String question = trimToNull(screeningIntakeQuestion.getQuestion());
			FontSizeId fontSizeId = screeningIntakeQuestion.getFontSizeId();
			int questionNumber = i + 1;

			if (question == null)
				validationException.add(new FieldError(format("screeningQuestions[%d].question", questionNumber), getStrings().get("Question text is required for screening question {{questionNumber}}.", new HashMap<String, Object>() {{
					put("questionNumber", questionNumber);
				}})));

			if (fontSizeId == null)
				validationException.add(new FieldError(format("screeningQuestions[%d].fontSizeId", questionNumber), getStrings().get("Font size is required for screening question {{questionNumber}}.", new HashMap<String, Object>() {{
					put("questionNumber", questionNumber);
				}})));
		}

		if (validationException.hasErrors())
			throw validationException;

		Integer normalizedHexColor = getNormalizer().normalizeHexColor(hexColor).get();

		getDatabase().execute("UPDATE appointment_type SET visit_type_id=?, " +
						"name=?, description=?, duration_in_minutes=?, scheduling_system_id=?, hex_color=? WHERE appointment_type_id=?", visitTypeId, name,
				description, durationInMinutes, schedulingSystemId, normalizedHexColor, appointmentTypeId);

		getDatabase().execute("DELETE FROM provider_appointment_type WHERE provider_id=? AND appointment_type_id=?", providerId, appointmentTypeId);

		getDatabase().execute("INSERT INTO provider_appointment_type (provider_id, appointment_type_id, display_order) " +
						"SELECT ?,?, COALESCE(MAX(display_order) + 1, 1) FROM provider_appointment_type WHERE provider_id=?",
				providerId, appointmentTypeId, providerId);

		// Build assessment, if needed

		// Normalize
		patientIntakeQuestions = patientIntakeQuestions.stream()
				.filter(patientIntakeQuestion -> patientIntakeQuestion != null)
				.collect(Collectors.toList());

		// Normalize
		screeningQuestions = screeningQuestions.stream()
				.filter(screeningQuestion -> screeningQuestion != null)
				.collect(Collectors.toList());

		// TODO: would be nice to only recreate the assessment if it has changed instead of on every edit

		getDatabase().execute("UPDATE appointment_type_assessment SET active=FALSE WHERE appointment_type_id=?", appointmentTypeId);

		if (patientIntakeQuestions.size() > 0 || screeningQuestions.size() > 0) {
			UUID assessmentId = createIntakeAssessmentForAppointmentTypeQuestions(screeningQuestions, patientIntakeQuestions);

			getDatabase().execute("INSERT INTO appointment_type_assessment (appointment_type_id, " +
					"assessment_id, active) VALUES (?,?,?)", appointmentTypeId, assessmentId, true);
		}

		return true;
	}

	@Nonnull
	protected UUID createIntakeAssessmentForAppointmentTypeQuestions(@Nonnull List<CreateScreeningQuestionRequest> screeningQuestions,
																																	 @Nonnull List<CreatePatientIntakeQuestionRequest> patientIntakeQuestions) {
		requireNonNull(screeningQuestions);
		requireNonNull(patientIntakeQuestions);

		UUID assessmentId = UUID.randomUUID();

		getDatabase().execute("INSERT INTO assessment (assessment_id, assessment_type_id, " +
				"minimum_eligibility_score, answers_may_contain_pii) VALUES (?,?,?,?)", assessmentId, AssessmentType.AssessmentTypeId.INTAKE, screeningQuestions.size(), false);

		UUID mostRecentQuestionId = null;

		List<Object> allQuestions = new ArrayList<>(screeningQuestions.size() + patientIntakeQuestions.size());

		allQuestions.addAll(screeningQuestions);
		allQuestions.addAll(patientIntakeQuestions);

		// Walk in reverse order since each question's "yes" answer needs to know the _next_ question to point to
		for (int i = allQuestions.size() - 1; i >= 0; --i) {
			Object question = allQuestions.get(i);

			UUID nextQuestionId = mostRecentQuestionId;

			mostRecentQuestionId = UUID.randomUUID();

			if (question instanceof CreateScreeningQuestionRequest) {
				CreateScreeningQuestionRequest screeningQuestionRequest = (CreateScreeningQuestionRequest) question;

				String screeningQuestion = screeningQuestionRequest.getQuestion();
				FontSizeId fontSizeId = screeningQuestionRequest.getFontSizeId();

				UUID answerYesId = UUID.randomUUID();
				UUID answerNoId = UUID.randomUUID();

				// Careful: display order must start at 1, not 0
				getDatabase().execute("INSERT INTO question (question_id, assessment_id, question_type_id, font_size_id, " +
						"question_text, display_order, is_root_question) VALUES (?,?,?,?,?,?,?)", mostRecentQuestionId, assessmentId, QuestionTypeId.QUAD, fontSizeId, screeningQuestion, i + 1, true);

				getDatabase().execute("INSERT INTO answer (answer_id, question_id, answer_text, display_order, " +
						"answer_value, next_question_id) VALUES (?,?,?,?,?,?)", answerYesId, mostRecentQuestionId, getStrings().get("Yes"), 1, 1, nextQuestionId);

				getDatabase().execute("INSERT INTO answer (answer_id, question_id, answer_text, display_order, " +
						"answer_value, next_question_id) VALUES (?,?,?,?,?,?)", answerNoId, mostRecentQuestionId, getStrings().get("No"), 2, 0, null);
			} else if (question instanceof CreatePatientIntakeQuestionRequest) {
				CreatePatientIntakeQuestionRequest intakeQuestionRequest = (CreatePatientIntakeQuestionRequest) question;

				String screeningQuestion = intakeQuestionRequest.getQuestion();
				QuestionTypeId questionTypeId = QuestionTypeId.TEXT;
				FontSizeId fontSizeId = intakeQuestionRequest.getFontSizeId();
				QuestionContentHintId questionContentHintId = intakeQuestionRequest.getQuestionContentHintId();

				if (questionContentHintId == null)
					questionContentHintId = QuestionContentHintId.NONE;

				UUID answerId = UUID.randomUUID();

				// Careful: display order must start at 1, not 0
				getDatabase().execute("INSERT INTO question (question_id, assessment_id, question_type_id, font_size_id, " +
								"question_content_hint_id, question_text, display_order, is_root_question) VALUES (?,?,?,?,?,?,?,?)", mostRecentQuestionId,
						assessmentId, questionTypeId, fontSizeId, questionContentHintId, screeningQuestion, i + 1, true);

				getDatabase().execute("INSERT INTO answer (answer_id, question_id, answer_text, display_order, " +
						"answer_value, next_question_id) VALUES (?,?,?,?,?,?)", answerId, mostRecentQuestionId, getStrings().get("Type here"), 1, 1, nextQuestionId);
			}
		}

		return assessmentId;
	}

	@Nonnull
	public Boolean deleteAppointmentType(@Nullable UUID appointmentTypeId) {
		if (appointmentTypeId == null)
			return false;

		return getDatabase().execute("UPDATE appointment_type SET deleted=TRUE " +
				"WHERE appointment_type_id=?", appointmentTypeId) > 0;
	}

	@Nonnull
	public Optional<Institution> findInstitutionForAppointmentTypeId(@Nullable UUID appointmentTypeId) {
		if (appointmentTypeId == null)
			return Optional.empty();

		List<Institution> institutions = getDatabase().queryForList("SELECT DISTINCT i.* FROM institution i, " +
				"provider_appointment_type pat, provider p, appointment_type at WHERE pat.appointment_type_id=? AND pat.provider_id=p.provider_id " +
				"AND p.institution_id=i.institution_id AND pat.appointment_type_id=at.appointment_type_id " +
				"AND at.deleted=FALSE AND p.active=TRUE", Institution.class, appointmentTypeId);

		if (institutions.size() == 0)
			return Optional.empty();

		if (institutions.size() == 1)
			return Optional.of(institutions.get(0));

		throw new IllegalStateException(format("Found multiple institutions (%s) for appointment type ID %s",
				institutions.stream()
						.map(institution -> institution.getInstitutionId().name())
						.collect(Collectors.joining(", ")),
				appointmentTypeId
		));
	}

	protected void sendPatientAndProviderCobaltAppointmentCreatedEmails(@Nonnull UUID appointmentId) {
		requireNonNull(appointmentId);

		Appointment appointment = findAppointmentById(appointmentId).get();

		if (appointment.getVideoconferencePlatformId() == VideoconferencePlatformId.SWITCHBOARD) {
			getLogger().debug("Appointment ID {} is Switchboard-backed, so don't send out booking emails.", appointment.getAppointmentId());
			return;
		}

		Account account = getAccountService().findAccountById(appointment.getAccountId()).get();
		Provider provider = getProviderService().findProviderById(appointment.getProviderId()).get();
		Institution institution = getInstitutionService().findInstitutionById(provider.getInstitutionId()).get();

		String appointmentStartDateTimeDescription = getFormatter().formatDateTime(appointment.getStartTime(), FormatStyle.LONG, FormatStyle.SHORT);
		String appointmentStartDateDescription = getFormatter().formatDate(appointment.getStartTime().toLocalDate());
		String appointmentStartTimeDescription = getFormatter().formatTime(appointment.getStartTime().toLocalTime(), FormatStyle.SHORT);
		String accountName = getAccountService().determineDisplayName(account);
		String providerName = provider.getName();
		String providerEmailAddress = provider.getEmailAddress();
		String videoconferenceUrl = appointment.getVideoconferenceUrl();

		String webappBaseUrl = getInstitutionService().findWebappBaseUrlByInstitutionId(provider.getInstitutionId()).get();
		String providerNameAndCredentials = provider.getName();

		if (provider.getLicense() != null)
			providerNameAndCredentials = format("%s, %s", provider.getName(), provider.getLicense());

		// Patient email
		if (account.getEmailAddress() != null) {
			Map<String, Object> cobaltPatientEmailMessageContext = new HashMap<>();
			cobaltPatientEmailMessageContext.put("appointmentId", appointmentId);
			cobaltPatientEmailMessageContext.put("providerName", provider.getName());
			cobaltPatientEmailMessageContext.put("providerEmailAddress", provider.getEmailAddress());
			cobaltPatientEmailMessageContext.put("providerNameAndCredentials", providerNameAndCredentials);
			cobaltPatientEmailMessageContext.put("videoconferenceUrl", appointment.getVideoconferenceUrl());
			cobaltPatientEmailMessageContext.put("imageUrl", firstNonNull(provider.getImageUrl(), getConfiguration().getDefaultProviderImageUrlForEmail()));
			cobaltPatientEmailMessageContext.put("patientName", accountName);
			cobaltPatientEmailMessageContext.put("appointmentStartDateDescription", appointmentStartDateDescription);
			cobaltPatientEmailMessageContext.put("appointmentStartTimeDescription", appointmentStartTimeDescription);
			cobaltPatientEmailMessageContext.put("cancelUrl", format("%s/my-calendar?appointmentId=%s&action=cancel", webappBaseUrl, appointmentId));
			cobaltPatientEmailMessageContext.put("icalUrl", format("%s/appointments/%s/ical", webappBaseUrl, appointmentId));
			cobaltPatientEmailMessageContext.put("googleCalendarUrl", format("%s/appointments/%s/google-calendar", webappBaseUrl, appointmentId));
			cobaltPatientEmailMessageContext.put("anotherTimeUrl", format("%s/connect-with-support", webappBaseUrl));

			EmailMessage patientEmailMessage = new EmailMessage.Builder(EmailMessageTemplate.APPOINTMENT_CREATED_PATIENT, account.getLocale())
					.toAddresses(Collections.singletonList(account.getEmailAddress()))
					.messageContext(cobaltPatientEmailMessageContext)
					.emailAttachments(List.of(generateICalInviteAsEmailAttachment(appointment, InviteMethod.REQUEST)))
					.build();

			getEmailMessageManager().enqueueMessage(patientEmailMessage);

			// Schedule a reminder message for this booking based on institution rules
			LocalDate reminderMessageDate = appointment.getStartTime().toLocalDate().minusDays(institution.getAppointmentReservationDefaultReminderDayOffset());
			LocalTime reminderMessageTimeOfDay = institution.getAppointmentReservationDefaultReminderTimeOfDay();

			EmailMessage patientReminderEmailMessage = new EmailMessage.Builder(EmailMessageTemplate.APPOINTMENT_REMINDER_PATIENT, account.getLocale())
					.toAddresses(Collections.singletonList(account.getEmailAddress()))
					.messageContext(cobaltPatientEmailMessageContext)
					.build();

			UUID patientReminderScheduledMessageId = getMessageService().createScheduledMessage(new CreateScheduledMessageRequest<>() {{
				setMetadata(Map.of("appointmentId", appointmentId));
				setMessage(patientReminderEmailMessage);
				setTimeZone(provider.getTimeZone());
				setScheduledAt(LocalDateTime.of(reminderMessageDate, reminderMessageTimeOfDay));
			}});

			getDatabase().execute("""
					UPDATE appointment
					SET patient_reminder_scheduled_message_id=? 
					WHERE appointment_id=?
					""", patientReminderScheduledMessageId, appointmentId);
		}

		// Provider email
		Map<String, Object> cobaltProviderEmailMessageContext = new HashMap<>();
		cobaltProviderEmailMessageContext.put("appointmentId", appointmentId);
		cobaltProviderEmailMessageContext.put("appointmentStartDateTimeDescription", appointmentStartDateTimeDescription);
		cobaltProviderEmailMessageContext.put("appointmentStartDateDescription", appointmentStartDateDescription);
		cobaltProviderEmailMessageContext.put("appointmentStartTimeDescription", appointmentStartTimeDescription);
		cobaltProviderEmailMessageContext.put("providerName", providerName);
		cobaltProviderEmailMessageContext.put("accountName", accountName);
		cobaltProviderEmailMessageContext.put("accountEmailAddress", account.getEmailAddress());
		cobaltProviderEmailMessageContext.put("videoconferenceUrl", videoconferenceUrl);
		cobaltProviderEmailMessageContext.put("icalUrl", format("%s/appointments/%s/ical", webappBaseUrl, appointmentId));
		cobaltProviderEmailMessageContext.put("googleCalendarUrl", format("%s/appointments/%s/google-calendar", webappBaseUrl, appointmentId));

		// With native scheduling, providers can deeplink right to the appointment on their calendar
		if (appointment.getSchedulingSystemId() == SchedulingSystemId.COBALT)
			cobaltProviderEmailMessageContext.put("providerSchedulingUrl", format("%s/scheduling/appointments/%s", webappBaseUrl, appointmentId));

		EmailMessage providerEmailMessage = new EmailMessage.Builder(EmailMessageTemplate.APPOINTMENT_CREATED_PROVIDER, provider.getLocale())
				.toAddresses(List.of(provider.getEmailAddress()))
				.messageContext(cobaltProviderEmailMessageContext)
				.emailAttachments(List.of(generateICalInviteAsEmailAttachment(appointment, InviteMethod.REQUEST)))
				.build();

		getEmailMessageManager().enqueueMessage(providerEmailMessage);
	}

	protected void sendPatientAndProviderCobaltAppointmentCanceledEmails(@Nonnull UUID appointmentId) {
		requireNonNull(appointmentId);

		Appointment appointment = findAppointmentById(appointmentId).get();

		if (appointment.getVideoconferencePlatformId() == VideoconferencePlatformId.SWITCHBOARD) {
			getLogger().debug("Appointment ID {} is Switchboard-backed, so don't send out cancelation emails.", appointment.getAppointmentId());
			return;
		}

		Account account = getAccountService().findAccountById(appointment.getAccountId()).get();
		Provider provider = getProviderService().findProviderById(appointment.getProviderId()).get();

		String appointmentStartDateTimeDescription = getFormatter().formatDateTime(appointment.getStartTime(), FormatStyle.LONG, FormatStyle.SHORT);
		String appointmentStartDateDescription = getFormatter().formatDate(appointment.getStartTime().toLocalDate());
		String appointmentStartTimeDescription = getFormatter().formatTime(appointment.getStartTime().toLocalTime(), FormatStyle.SHORT);
		String accountName = getAccountService().determineDisplayName(account);
		String providerName = provider.getName();

		String providerNameAndCredentials = provider.getName();

		if (provider.getLicense() != null)
			providerNameAndCredentials = format("%s, %s", provider.getName(), provider.getLicense());

		// Patient email
		if (account.getEmailAddress() != null) {
			Map<String, Object> cobaltPatientEmailMessageContext = new HashMap<>();
			cobaltPatientEmailMessageContext.put("appointmentId", appointmentId);
			cobaltPatientEmailMessageContext.put("providerName", provider.getName());
			cobaltPatientEmailMessageContext.put("providerNameAndCredentials", providerNameAndCredentials);
			cobaltPatientEmailMessageContext.put("videoconferenceUrl", appointment.getVideoconferenceUrl());
			cobaltPatientEmailMessageContext.put("imageUrl", firstNonNull(provider.getImageUrl(), getConfiguration().getDefaultProviderImageUrlForEmail()));
			cobaltPatientEmailMessageContext.put("patientName", accountName);
			cobaltPatientEmailMessageContext.put("appointmentStartDateDescription", appointmentStartDateDescription);
			cobaltPatientEmailMessageContext.put("appointmentStartTimeDescription", appointmentStartTimeDescription);

			EmailMessage patientEmailMessage = new EmailMessage.Builder(EmailMessageTemplate.APPOINTMENT_CANCELED_PATIENT, account.getLocale())
					.toAddresses(Collections.singletonList(account.getEmailAddress()))
					.messageContext(cobaltPatientEmailMessageContext)
					.emailAttachments(List.of(generateICalInviteAsEmailAttachment(appointment, InviteMethod.CANCEL)))
					.build();

			getEmailMessageManager().enqueueMessage(patientEmailMessage);
		}

		// Provider email
		Map<String, Object> cobaltProviderEmailMessageContext = new HashMap<>();
		cobaltProviderEmailMessageContext.put("appointmentId", appointmentId);
		cobaltProviderEmailMessageContext.put("appointmentStartDateTimeDescription", appointmentStartDateTimeDescription);
		cobaltProviderEmailMessageContext.put("appointmentStartDateDescription", appointmentStartDateDescription);
		cobaltProviderEmailMessageContext.put("appointmentStartTimeDescription", appointmentStartTimeDescription);
		cobaltProviderEmailMessageContext.put("providerName", providerName);
		cobaltProviderEmailMessageContext.put("accountName", accountName);
		cobaltProviderEmailMessageContext.put("accountEmailAddress", account.getEmailAddress());

		EmailMessage providerEmailMessage = new EmailMessage.Builder(EmailMessageTemplate.APPOINTMENT_CANCELED_PROVIDER, provider.getLocale())
				.toAddresses(List.of(provider.getEmailAddress()))
				.messageContext(cobaltProviderEmailMessageContext)
				.emailAttachments(List.of(generateICalInviteAsEmailAttachment(appointment, InviteMethod.CANCEL)))
				.build();

		getEmailMessageManager().enqueueMessage(providerEmailMessage);
	}

	@Nonnull
	public Boolean cancelAppointment(@Nonnull CancelAppointmentRequest request) {
		requireNonNull(request);

		UUID appointmentId = request.getAppointmentId();
		UUID accountId = request.getAccountId();
		Boolean canceledByWebhook = request.getCanceledByWebhook() == null ? false : request.getCanceledByWebhook();
		Appointment appointment = null;
		Account account = null;
		ValidationException validationException = new ValidationException();

		if (appointmentId == null) {
			validationException.add(new FieldError("appointmentId", getStrings().get("Appointment ID is required.")));
		} else {
			appointment = findAppointmentById(appointmentId).orElse(null);

			if (appointment == null)
				validationException.add(new FieldError("appointmentId", getStrings().get("Appointment ID is invalid.")));
		}

		// Account is not necessarily required - for example, cancelation via external source like a webhook
		if (accountId != null)
			account = getAccountService().findAccountById(accountId).orElse(null);

		if (validationException.hasErrors())
			throw validationException;

		AppointmentType appointmentType = findAppointmentTypeById(appointment.getAppointmentTypeId()).get();
		Provider provider = getProviderService().findProviderById(appointment.getProviderId()).orElse(null);

		if (!canceledByWebhook) {
			if (appointmentType.getSchedulingSystemId() == SchedulingSystemId.ACUITY) {
				try {
					getAcuitySchedulingClient().cancelAppointment(appointment.getAcuityAppointmentId());
				} catch (Exception e) {
					getLogger().warn("Unable to cancel appointment via Acuity, continuing on...", e);
				}
			} else if (appointmentType.getSchedulingSystemId() == SchedulingSystemId.EPIC) {
				EpicClient epicClient = getEnterprisePluginProvider().enterprisePluginForInstitutionId(account.getInstitutionId()).epicClientForBackendService().get();

				// SYNC ACCOUNT UID
				String uid = epicClient.determineLatestUIDForPatientIdentifier(account.getEpicPatientId(), account.getEpicPatientIdType()).get();

				if (!uid.equals(account.getEpicPatientId()))
					getAccountService().updateAccountEpicPatient(account.getAccountId(), uid, "UID");

				account = getAccountService().findAccountById(accountId).get();

				com.cobaltplatform.api.integration.epic.request.CancelAppointmentRequest.Patient patient = new com.cobaltplatform.api.integration.epic.request.CancelAppointmentRequest.Patient();
				patient.setID(account.getEpicPatientId());
				patient.setType(account.getEpicPatientIdType());

				com.cobaltplatform.api.integration.epic.request.CancelAppointmentRequest.Contact contact = new com.cobaltplatform.api.integration.epic.request.CancelAppointmentRequest.Contact();
				contact.setID(appointment.getEpicContactId());
				contact.setType(appointment.getEpicContactIdType());

				com.cobaltplatform.api.integration.epic.request.CancelAppointmentRequest cancelRequest = new com.cobaltplatform.api.integration.epic.request.CancelAppointmentRequest();
				cancelRequest.setReason("Patient requested cancelation via Cobalt");
				cancelRequest.setPatient(patient);
				cancelRequest.setContact(contact);

				epicClient.performCancelAppointment(cancelRequest);

				Map<String, Object> payload = new HashMap<>();
				payload.put("csn", appointment.getEpicContactId());
				payload.put("appointmentId", appointmentId);
				payload.put("request", request);

				AuditLog auditLog = new AuditLog();
				auditLog.setAccountId(account.getAccountId());
				auditLog.setAuditLogEventId(AuditLogEventId.EPIC_APPOINTMENT_CANCEL);
				auditLog.setPayload(getJsonMapper().toJson(payload));
				getAuditLogService().audit(auditLog);
			}
		}

		// Only delete Bluejeans if this is a 1:1 meeting
		if (appointmentType.getSchedulingSystemId() == SchedulingSystemId.EPIC || (appointment.getAcuityClassId() == null && provider != null)) {
			if (appointment.getBluejeansMeetingId() != null) {
				try {
					getBluejeansClient().cancelScheduledMeeting(provider.getBluejeansUserId().intValue(), appointment.getBluejeansMeetingId().intValue(),
							false, getStrings().get("The patient canceled the meeting."));
				} catch (Exception e) {
					getLogger().warn("Unable to cancel Bluejeans meeting, continuing on...", e);
				}
			}
		}

		boolean canceled = getDatabase().execute("UPDATE appointment SET canceled=TRUE, attendance_status_id=?, canceled_at=NOW(), " +
				"canceled_for_reschedule=?, rescheduled_appointment_id=? WHERE appointment_id=?", AttendanceStatusId.CANCELED, request.getCanceledForReschedule(), request.getRescheduleAppointmentId(), appointmentId) > 0;

		//Cancel any interaction instances that are scheduled for this appointment
		getInteractionService().cancelInteractionInstancesForAppointment(appointmentId);

		Appointment pinnedAppointment = appointment;
		Account appointmentAccount = getAccountService().findAccountById(pinnedAppointment.getAccountId()).orElse(null);

		// Cancel any scheduled reminder message for the patient
		getMessageService().cancelScheduledMessage(appointment.getPatientReminderScheduledMessageId());

		getDatabase().currentTransaction().get().addPostCommitOperation(() -> {
			if (appointmentType.getSchedulingSystemId() == SchedulingSystemId.ACUITY) {
				getAcuitySchedulingCache().invalidateAvailability(pinnedAppointment.getStartTime().toLocalDate(), pinnedAppointment.getTimeZone());

				// Kick off a manual resync for this date just in case there's some disconnect
				if (pinnedAppointment.getProviderId() != null)
					ForkJoinPool.commonPool().execute(() -> {
						getAcuitySyncManager().syncProviderAvailability(pinnedAppointment.getProviderId(), pinnedAppointment.getStartTime().toLocalDate());
					});
			} else if (appointmentType.getSchedulingSystemId() == SchedulingSystemId.EPIC) {
				ForkJoinPool.commonPool().execute(() -> {
					getEpicSyncManager().syncProviderAvailability(pinnedAppointment.getProviderId(), pinnedAppointment.getStartTime().toLocalDate());
				});
			} else if (appointmentType.getSchedulingSystemId() == SchedulingSystemId.COBALT) {
				sendPatientAndProviderCobaltAppointmentCanceledEmails(appointmentId);
			}
		});

		return canceled;
	}

	public void rescheduleAppointmentFromWebhook(@Nonnull AcuityAppointment acuityAppointment,
																							 @Nonnull Appointment localAppointment) {
		requireNonNull(acuityAppointment);
		requireNonNull(localAppointment);

		Long durationInMinutes = Long.valueOf(acuityAppointment.getDuration());
		Instant startTime = getAcuitySchedulingClient().parseAcuityTime(acuityAppointment.getDatetime());
		Instant endTime = startTime.plus(durationInMinutes, ChronoUnit.MINUTES);

		getDatabase().execute("UPDATE appointment SET start_time=?, end_time=?, duration_in_minutes=? WHERE appointment_id=?",
				startTime, endTime, durationInMinutes, localAppointment.getAppointmentId());
	}

	@Nonnull
	public Boolean changeAppointmentAttendanceStatus(@Nonnull ChangeAppointmentAttendanceStatusRequest request) {
		requireNonNull(request);

		UUID appointmentId = request.getAppointmentId();
		UUID accountId = request.getAccountId();
		AttendanceStatusId attendanceStatusId = request.getAttendanceStatusId();
		Appointment appointment = null;
		Account account = null;
		ValidationException validationException = new ValidationException();

		if (appointmentId == null) {
			validationException.add(new FieldError("appointmentId", getStrings().get("Appointment ID is required.")));
		} else {
			appointment = findAppointmentById(appointmentId).orElse(null);

			if (appointment == null)
				validationException.add(new FieldError("appointmentId", getStrings().get("Appointment ID is invalid.")));
		}

		if (accountId == null) {
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));
		} else {
			account = getAccountService().findAccountById(accountId).orElse(null);

			if (account == null)
				validationException.add(new FieldError("accountId", getStrings().get("Appointment ID is invalid.")));
		}

		if (attendanceStatusId == null)
			validationException.add(new FieldError("attendanceStatusId", getStrings().get("Attendance Status ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		// Short-circuit if updating to the same status
		if (appointment.getAttendanceStatusId().equals(attendanceStatusId))
			return false;

		return getDatabase().execute("UPDATE appointment SET attendance_status_id=? WHERE appointment_id=?", attendanceStatusId, appointmentId) > 0;
	}

	private void sendProviderScoreEmail(@Nonnull Provider provider,
																			@Nonnull Account account,
																			@Nullable String accountEmailAddress,
																			@Nonnull String phoneNumber,
																			@Nonnull String videoconferenceUrl,
																			@Nonnull String appointmentDate,
																			@Nonnull String appointmentStartTime,
																			@Nonnull String appointmentEndTime,
																			@Nullable AccountSession intakeSession) {
		requireNonNull(provider);
		requireNonNull(account);

		if (provider.getVideoconferencePlatformId() == VideoconferencePlatformId.SWITCHBOARD) {
			getLogger().debug("Provider {} uses Switchboard, do not send a provider score email.", provider.getName());
			return;
		}

		if (provider.getSchedulingSystemId() == SchedulingSystemId.COBALT) {
			getLogger().debug("Provider {} uses native scheduling, do not send a provider score email.", provider.getName());
			return;
		}

		Optional<EvidenceScores> evidenceScores = getAssessmentScoringService().getEvidenceAssessmentRecommendation(account);
		String intakeAssessmentAnswerString = intakeSession == null ? getStrings().get("No intake responses") :
				getSessionService().findAnswersForSession(intakeSession).stream()
						.map(r -> r.getAnswerText()).collect(Collectors.joining("-"));

		if (intakeSession != null) {
			Assessment assessment = getAssessmentService().findAssessmentById(intakeSession.getAssessmentId()).orElse(null);

			if (assessment != null && assessment.getAnswersMayContainPii())
				intakeAssessmentAnswerString = format("Please sign in to Cobalt and then click this link to view intake responses: %s/account-sessions/%s/text",
						getInstitutionService().findWebappBaseUrlByInstitutionId(provider.getInstitutionId()).get(), intakeSession.getAccountSessionId());
		}

		String accountScoreString;
		if (evidenceScores.isPresent()) {
			accountScoreString = Stream.of(
					evidenceScores.get().getPhq9Recommendation(),
					evidenceScores.get().getGad7Recommendation(),
					evidenceScores.get().getPcptsdRecommendation()
			).filter(Objects::nonNull).map(r -> r.getAnswers()).collect(Collectors.joining(":"));
			if (accountScoreString.isEmpty()) {
				accountScoreString = getStrings().get("No assessment taken yet");
			}
		} else {
			accountScoreString = getStrings().get("No assessment taken yet");
		}

		String name = getAccountService().determineDisplayName(account);
		phoneNumber = getFormatter().formatPhoneNumber(phoneNumber, account.getLocale());

		// Special case for Health Advocate
		if (provider.getVideoconferencePlatformId() == VideoconferencePlatformId.TELEPHONE) {
			intakeAssessmentAnswerString = null;
			videoconferenceUrl = null;

			// Temporary hack: pull out the assessment answers for HA...
			try {
				List<Question> questions = getAssessmentService().findQuestionsForAssessmentId(intakeSession.getAssessmentId());
				List<Answer> answers = getSessionService().findAnswersForSession(intakeSession);
				List<AccountSessionAnswer> accountSessionAnswers = getSessionService().findAccountSessionAnswersForAccountSessionId(intakeSession.getAccountSessionId());
				Map<UUID, Question> questionsByQuestionId = new HashMap<>(questions.size());

				for (Question question : questions)
					questionsByQuestionId.put(question.getQuestionId(), question);

				List<String> finalAnswers = new ArrayList<>(answers.size());

				for (int i = 0; i < answers.size(); ++i) {
					Answer answer = answers.get(i);
					AccountSessionAnswer accountSessionAnswer = accountSessionAnswers.get(i);
					Question question = questionsByQuestionId.get(answer.getQuestionId());
					String finalAnswer = question.getQuestionTypeId().isFreeform() ? accountSessionAnswer.getAnswerText() : answer.getAnswerText();

					finalAnswers.add(finalAnswer);
				}

				// ...and we know the name is at index 3, so include in email
				// TODO: this should be cleaned up, it will error out if the HA intake assessment changes so name/number is in a different spot
				name = finalAnswers.get(3);
				phoneNumber = finalAnswers.get(4);
				phoneNumber = getFormatter().formatPhoneNumber(phoneNumber, account.getLocale());
			} catch (Exception e) {
				getLogger().warn("Unable to pull data from Health Advocate intake, continuing on...", e);
			}
		}

		Map<String, Object> messageContext = new HashMap<>();
		messageContext.put("accountDescription", format("%s - %s - %s", name == null ? getStrings().get("[no name]") : name,
				accountEmailAddress == null ? getStrings().get("[no email address]") : accountEmailAddress,
				phoneNumber == null ? getStrings().get("[no phone number]") : phoneNumber));
		messageContext.put("accountScores", accountScoreString);
		messageContext.put("videoconferenceUrl", videoconferenceUrl);
		messageContext.put("appointmentDate", appointmentDate);
		messageContext.put("appointmentStartTime", appointmentStartTime);
		messageContext.put("appointmentEndTime", appointmentEndTime);
		messageContext.put("intakeResults", intakeAssessmentAnswerString);

		getEmailMessageManager().enqueueMessage(new EmailMessage.Builder(EmailMessageTemplate.PROVIDER_ASSESSMENT_SCORES, provider.getLocale() == null ? Locale.US : provider.getLocale())
				.toAddresses(new ArrayList<>() {{
					add(provider.getEmailAddress());
				}})
				.messageContext(messageContext)
				.build());
	}

	@Nonnull
	public Optional<AppointmentReason> findAppointmentReasonById(@Nullable UUID appointmentReasonId) {
		if (appointmentReasonId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM appointment_reason " +
				"WHERE appointment_reason_id=?", AppointmentReason.class, appointmentReasonId);
	}

	@Nonnull
	public List<AppointmentReason> findAppointmentReasons() {
		return getDatabase().queryForList("SELECT * FROM appointment_reason ORDER BY display_order", AppointmentReason.class);
	}

	@Nonnull
	public List<AppointmentReason> findAppointmentReasons(@Nullable InstitutionId institutionId,
																												@Nullable AppointmentReasonTypeId appointmentReasonTypeId) {
		return getDatabase().queryForList("SELECT * FROM appointment_reason " +
				"WHERE appointment_reason_type_id=? AND institution_id=? ORDER BY display_order", AppointmentReason.class, appointmentReasonTypeId, institutionId);
	}

	@Nonnull
	public AppointmentReason findNotSpecifiedAppointmentReasonByInstitutionId(@Nonnull InstitutionId institutionId) {
		// It's assumed each institution has a single NOT_SPECIFIED account reason...
		return getDatabase().queryForObject("SELECT * FROM appointment_reason " +
				"WHERE institution_id=? AND appointment_reason_type_id=?", AppointmentReason.class, institutionId, AppointmentReasonTypeId.NOT_SPECIFIED).get();
	}

	@Nonnull
	public List<Appointment> findActiveAppointmentsForProviderId(@Nullable UUID providerId) {
		return findActiveAppointmentsForProviderId(providerId, null, null);
	}

	@Nonnull
	public List<Appointment> findActiveAppointmentsForProviderId(@Nullable UUID providerId,
																															 @Nullable LocalDate startDate,
																															 @Nullable LocalDate endDate) {
		if (providerId == null)
			return Collections.emptyList();

		StringBuilder sql = new StringBuilder("SELECT * FROM appointment WHERE 1=1 ");
		List<Object> parameters = new ArrayList<>();

		sql.append("AND provider_id = ? ");
		parameters.add(providerId);

		if (startDate != null) {
			sql.append("AND start_time >= ? ");
			parameters.add(startDate.atStartOfDay());
		}

		if (endDate != null) {
			sql.append("AND start_time <= ? ");
			parameters.add(endDate.atTime(LocalTime.MAX));
		}

		sql.append("ORDER BY start_time");

		return getDatabase().queryForList(sql.toString(), Appointment.class, sqlVaragsParameters(parameters));
	}

	@Nonnull
	public String generateGoogleCalendarTemplateUrl(@Nonnull Appointment appointment) {
		requireNonNull(appointment);

		String title = calendarTitleForAppointment(appointment);

		return getGoogleCalendarUrlGenerator().generateNewEventUrl(title,
				null, appointment.getStartTime(), appointment.getEndTime(),
				appointment.getTimeZone(), appointment.getVideoconferenceUrl());
	}

	@Nonnull
	public String generateICalInvite(@Nonnull Appointment appointment,
																	 @Nonnull InviteMethod inviteMethod) {
		requireNonNull(appointment);
		requireNonNull(inviteMethod);

		String title = calendarTitleForAppointment(appointment);

		String extendedDescription = format("%s\n\n%s", title, getStrings().get("Join videoconference: {{videoconferenceUrl}}", new HashMap<String, Object>() {{
			put("videoconferenceUrl", appointment.getVideoconferenceUrl());
		}}));

		Account patient = getAccountService().findAccountById(appointment.getAccountId()).get();
		Provider provider = getProviderService().findProviderById(appointment.getProviderId()).get();

		InviteOrganizer inviteOrganizer = InviteOrganizer.forEmailAddress(provider.getEmailAddress());
		InviteAttendee inviteAttendee = InviteAttendee.forEmailAddress(patient.getEmailAddress());

		return getiCalInviteGenerator().generateInvite(appointment.getAppointmentId().toString(), title,
				extendedDescription, appointment.getStartTime(), appointment.getEndTime(),
				appointment.getTimeZone(), appointment.getVideoconferenceUrl(), inviteMethod, inviteOrganizer, inviteAttendee);
	}

	@Nonnull
	public EmailAttachment generateICalInviteAsEmailAttachment(@Nonnull Appointment appointment,
																														 @Nonnull InviteMethod inviteMethod) {
		requireNonNull(appointment);
		requireNonNull(inviteMethod);

		String iCalInvite = generateICalInvite(appointment, inviteMethod);

		String filename = "invite.ics";
		String method = inviteMethod == InviteMethod.CANCEL ? "CANCEL" : "REQUEST";
		String contentType = format("text/calendar; charset=utf-8; method=%s; name=%s", method, filename);

		return new EmailAttachment(filename, contentType, iCalInvite.getBytes(StandardCharsets.UTF_8));
	}

	@Nonnull
	protected String calendarTitleForAppointment(@Nonnull Appointment appointment) {
		requireNonNull(appointment);

		Provider provider = getProviderService().findProviderById(appointment.getProviderId()).orElse(null);

		if (provider == null)
			return getStrings().get("1:1 Session");

		return getStrings().get("1:1 Session with {{providerName}}", new HashMap<String, Object>() {{
			put("providerName", provider.getName());
		}});
	}

	@Nonnull
	public List<VisitType> findVisitTypes() {
		return getDatabase().queryForList("SELECT * FROM visit_type ORDER BY display_order", VisitType.class);
	}

	@Nonnull
	protected Database getDatabase() {
		return this.database;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected EnterprisePluginProvider getEnterprisePluginProvider() {
		return this.enterprisePluginProvider;
	}

	@Nonnull
	protected EpicSyncManager getEpicSyncManager() {
		return this.epicSyncManager;
	}

	@Nonnull
	protected AcuitySchedulingClient getAcuitySchedulingClient() {
		return this.acuitySchedulingClient;
	}

	@Nonnull
	protected BluejeansClient getBluejeansClient() {
		return this.bluejeansClient;
	}

	@Nonnull
	protected AcuitySchedulingCache getAcuitySchedulingCache() {
		return this.acuitySchedulingCache;
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return this.providerServiceProvider.get();
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountServiceProvider.get();
	}

	@Nonnull
	protected AuditLogService getAuditLogService() {
		return this.auditLogServiceProvider.get();
	}

	@Nonnull
	protected ClinicService getClinicService() {
		return this.clinicServiceProvider.get();
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
	}

	@Nonnull
	protected MessageService getMessageService() {
		return this.messageServiceProvider.get();
	}

	@Nonnull
	protected EmailMessageManager getEmailMessageManager() {
		return this.emailMessageManager;
	}

	@Nonnull
	protected AssessmentScoringService getAssessmentScoringService() {
		return this.assessmentScoringServiceProvider.get();
	}

	@Nonnull
	public AcuitySyncManager getAcuitySyncManager() {
		return this.acuitySyncManagerProvider.get();
	}

	@Nonnull
	protected Formatter getFormatter() {
		return this.formatter;
	}

	@Nonnull
	protected Normalizer getNormalizer() {
		return this.normalizer;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	protected SessionService getSessionService() {
		return this.sessionService;
	}

	@Nonnull
	protected AssessmentService getAssessmentService() {
		return this.assessmentService;
	}

	@Nonnull
	protected GoogleCalendarUrlGenerator getGoogleCalendarUrlGenerator() {
		return this.googleCalendarUrlGenerator;
	}

	@Nonnull
	protected ICalInviteGenerator getiCalInviteGenerator() {
		return this.iCalInviteGenerator;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return this.jsonMapper;
	}

	@Nonnull
	protected InteractionService getInteractionService() {
		return this.interactionService;
	}
}
