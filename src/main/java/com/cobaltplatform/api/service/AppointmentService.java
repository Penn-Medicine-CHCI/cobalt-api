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
import com.cobaltplatform.api.error.ErrorReporter;
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
import com.cobaltplatform.api.integration.epic.EpicFhirSyncManager;
import com.cobaltplatform.api.integration.epic.EpicSyncManager;
import com.cobaltplatform.api.integration.epic.code.AppointmentStatusCode;
import com.cobaltplatform.api.integration.epic.request.AppointmentBookFhirStu3Request;
import com.cobaltplatform.api.integration.epic.request.AppointmentSearchFhirStu3Request;
import com.cobaltplatform.api.integration.epic.request.GetProviderScheduleRequest;
import com.cobaltplatform.api.integration.epic.request.ScheduleAppointmentWithInsuranceRequest;
import com.cobaltplatform.api.integration.epic.response.AppointmentBookFhirStu3Response;
import com.cobaltplatform.api.integration.epic.response.AppointmentSearchFhirStu3Response;
import com.cobaltplatform.api.integration.epic.response.GetProviderScheduleResponse;
import com.cobaltplatform.api.integration.epic.response.ScheduleAppointmentWithInsuranceResponse;
import com.cobaltplatform.api.integration.gcal.GoogleCalendarUrlGenerator;
import com.cobaltplatform.api.integration.ical.ICalInviteGenerator;
import com.cobaltplatform.api.integration.ical.ICalInviteGenerator.InviteAttendee;
import com.cobaltplatform.api.integration.ical.ICalInviteGenerator.InviteMethod;
import com.cobaltplatform.api.integration.ical.ICalInviteGenerator.InviteOrganizer;
import com.cobaltplatform.api.integration.ical.ICalInviteGenerator.OrganizerAttendeeStrategy;
import com.cobaltplatform.api.integration.microsoft.request.OnlineMeetingCreateRequest;
import com.cobaltplatform.api.messaging.email.EmailAttachment;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.api.request.CancelAppointmentRequest;
import com.cobaltplatform.api.model.api.request.ChangeAppointmentAttendanceStatusRequest;
import com.cobaltplatform.api.model.api.request.CreateAcuityAppointmentTypeRequest;
import com.cobaltplatform.api.model.api.request.CreateAppointmentRequest;
import com.cobaltplatform.api.model.api.request.CreateAppointmentTypeRequest;
import com.cobaltplatform.api.model.api.request.CreateInteractionInstanceRequest;
import com.cobaltplatform.api.model.api.request.CreateMicrosoftTeamsMeetingRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientIntakeQuestionRequest;
import com.cobaltplatform.api.model.api.request.CreateScheduledMessageRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningQuestionRequest;
import com.cobaltplatform.api.model.api.request.ProviderFindRequest;
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
import com.cobaltplatform.api.model.db.AppointmentCancelationReason.AppointmentCancelationReasonId;
import com.cobaltplatform.api.model.db.AppointmentReason;
import com.cobaltplatform.api.model.db.AppointmentReasonType.AppointmentReasonTypeId;
import com.cobaltplatform.api.model.db.AppointmentScheduledMessage;
import com.cobaltplatform.api.model.db.AppointmentScheduledMessageType.AppointmentScheduledMessageTypeId;
import com.cobaltplatform.api.model.db.AppointmentTime;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.Assessment;
import com.cobaltplatform.api.model.db.AssessmentType;
import com.cobaltplatform.api.model.db.AttendanceStatus.AttendanceStatusId;
import com.cobaltplatform.api.model.db.AuditLog;
import com.cobaltplatform.api.model.db.AuditLogEvent.AuditLogEventId;
import com.cobaltplatform.api.model.db.EpicDepartment;
import com.cobaltplatform.api.model.db.FontSize.FontSizeId;
import com.cobaltplatform.api.model.db.FootprintEventGroupType.FootprintEventGroupTypeId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Interaction;
import com.cobaltplatform.api.model.db.InteractionType;
import com.cobaltplatform.api.model.db.MicrosoftTeamsMeeting;
import com.cobaltplatform.api.model.db.PatientOrderScheduledMessageType;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.Question;
import com.cobaltplatform.api.model.db.QuestionContentHint.QuestionContentHintId;
import com.cobaltplatform.api.model.db.QuestionType.QuestionTypeId;
import com.cobaltplatform.api.model.db.ScheduledMessageStatus.ScheduledMessageStatusId;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;
import com.cobaltplatform.api.model.db.VideoconferencePlatform.VideoconferencePlatformId;
import com.cobaltplatform.api.model.db.VisitType;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.cobaltplatform.api.model.service.EvidenceScores;
import com.cobaltplatform.api.model.service.ProviderFind;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityDate;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.Normalizer;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlInListPlaceholders;
import static com.cobaltplatform.api.util.DatabaseUtility.sqlVaragsParameters;
import static com.cobaltplatform.api.util.ValidationUtility.isValidHexColor;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class AppointmentService {
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final EpicSyncManager epicSyncManager;
	@Nonnull
	private final EpicFhirSyncManager epicFhirSyncManager;
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
	private final javax.inject.Provider<SystemService> systemServiceProvider;
	@Nonnull
	private final javax.inject.Provider<PatientOrderService> patientOrderServiceProvider;
	@Nonnull
	private final Logger logger;
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
	@Nonnull
	private final ErrorReporter errorReporter;

	@Inject
	public AppointmentService(@Nonnull DatabaseProvider databaseProvider,
														@Nonnull Configuration configuration,
														@Nonnull Strings strings,
														@Nonnull EpicSyncManager epicSyncManager,
														@Nonnull EpicFhirSyncManager epicFhirSyncManager,
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
														@Nonnull javax.inject.Provider<SystemService> systemServiceProvider,
														@Nonnull javax.inject.Provider<PatientOrderService> patientOrderServiceProvider,
														@Nonnull Formatter formatter,
														@Nonnull Normalizer normalizer,
														@Nonnull SessionService sessionService,
														@Nonnull AssessmentService assessmentService,
														@Nonnull GoogleCalendarUrlGenerator googleCalendarUrlGenerator,
														@Nonnull ICalInviteGenerator iCalInviteGenerator,
														@Nonnull JsonMapper jsonMapper,
														@Nonnull InteractionService interactionService,
														@Nonnull ErrorReporter errorReporter) {
		requireNonNull(databaseProvider);
		requireNonNull(configuration);
		requireNonNull(strings);
		requireNonNull(enterprisePluginProvider);
		requireNonNull(epicSyncManager);
		requireNonNull(epicFhirSyncManager);
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
		requireNonNull(systemServiceProvider);
		requireNonNull(patientOrderServiceProvider);
		requireNonNull(formatter);
		requireNonNull(normalizer);
		requireNonNull(sessionService);
		requireNonNull(assessmentService);
		requireNonNull(googleCalendarUrlGenerator);
		requireNonNull(iCalInviteGenerator);
		requireNonNull(jsonMapper);
		requireNonNull(interactionService);
		requireNonNull(errorReporter);

		this.databaseProvider = databaseProvider;
		this.configuration = configuration;
		this.strings = strings;
		this.enterprisePluginProvider = enterprisePluginProvider;
		this.epicSyncManager = epicSyncManager;
		this.epicFhirSyncManager = epicFhirSyncManager;
		this.acuitySchedulingClient = acuitySchedulingClient;
		this.acuitySchedulingCache = acuitySchedulingCache;
		this.bluejeansClient = bluejeansClient;
		this.providerServiceProvider = providerServiceProvider;
		this.accountServiceProvider = accountServiceProvider;
		this.acuitySyncManagerProvider = acuitySyncManagerProvider;
		this.auditLogServiceProvider = auditLogServiceProvider;
		this.clinicServiceProvider = clinicServiceProvider;
		this.institutionServiceProvider = institutionServiceProvider;
		this.assessmentScoringServiceProvider = assessmentScoringServiceProvider;
		this.messageServiceProvider = messageServiceProvider;
		this.systemServiceProvider = systemServiceProvider;
		this.patientOrderServiceProvider = patientOrderServiceProvider;
		this.formatter = formatter;
		this.normalizer = normalizer;
		this.sessionService = sessionService;
		this.assessmentService = assessmentService;
		this.googleCalendarUrlGenerator = googleCalendarUrlGenerator;
		this.iCalInviteGenerator = iCalInviteGenerator;
		this.jsonMapper = jsonMapper;
		this.logger = LoggerFactory.getLogger(getClass());
		this.interactionService = interactionService;
		this.errorReporter = errorReporter;
	}

	@Nonnull
	public Optional<Appointment> findAppointmentById(@Nullable UUID appointmentId) {
		if (appointmentId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM appointment WHERE appointment_id=?", Appointment.class, appointmentId);
	}

	@Nonnull
	public Optional<Appointment> findAppointmentByEpicFhirId(@Nullable String appointmentEpicFhirId,
																													 @Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Optional.empty();

		appointmentEpicFhirId = trimToNull(appointmentEpicFhirId);

		if (appointmentEpicFhirId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT a.*
				FROM appointment a, provider p
				WHERE a.provider_id=p.provider_id
				AND p.institution_id=?
				AND a.epic_appointment_fhir_id=?
				""", Appointment.class, institutionId, appointmentEpicFhirId);
	}

	@Nonnull
	public List<Appointment> findAppointmentsByAccountId(@Nullable UUID accountId) {
		if (accountId == null)
			return List.of();

		return getDatabase().queryForList("""
					SELECT *
					FROM appointment
					WHERE account_id=?
					ORDER BY start_time DESC
				""", Appointment.class, accountId);
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

		Account account = getAccountService().findAccountById(accountId).orElse(null);

		if (account == null)
			return List.of();

		Institution institution = getInstitutionService().findInstitutionById(account.getInstitutionId()).get();
		Instant now = Instant.now();

		if (institution.getEpicFhirEnabled()) {
			// Special behavior for Epic FHIR institutions -
			// Ask Epic for all appointments, then filter by only those providers that are active in Cobalt
			EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(institution.getInstitutionId());
			EpicClient epicClient = enterprisePlugin.epicClientForBackendService().get();

			// We don't have a FHIR ID associated with this account yet?  No results available
			if (account.getEpicPatientFhirId() == null)
				return List.of();

			// Sync Epic cancelations into our own database to make sure we're up-to-date
			synchronizeEpicFhirCanceledAppointmentsForAccountId(accountId);

			// Find all Epic FHIR providers we're aware of and make them quickly accessible by their Epic Practitioner FHIR ID
			Map<String, Provider> providersByEpicPractitionerFhirId = getProviderService().findProvidersByInstitutionId(institution.getInstitutionId()).stream()
					.filter(provider -> provider.getSchedulingSystemId() == SchedulingSystemId.EPIC_FHIR)
					.collect(Collectors.toMap(Provider::getEpicPractitionerFhirId, Function.identity()));

			AppointmentSearchFhirStu3Request request = new AppointmentSearchFhirStu3Request();
			request.setPatient(account.getEpicPatientFhirId());
			request.setStatus(AppointmentSearchFhirStu3Request.Status.BOOKED);

			AppointmentSearchFhirStu3Response response = epicClient.appointmentSearchFhirStu3(request);

			getLogger().debug("Patient {} has {} appointment[s] overall.", account.getEpicPatientFhirId(), response.getTotal());

			if (response.getTotal() == 0)
				return List.of();

			List<Appointment> appointments = new ArrayList<>();

			// Pick out all the appointment resources that match providers in our system
			for (AppointmentSearchFhirStu3Response.Entry entry : response.getEntry()) {
				AppointmentSearchFhirStu3Response.Entry.Resource resource = entry.getResource();

				if ("Appointment".equals(resource.getResourceType())) {
					boolean legitimateStatus = resource.getStatus() == AppointmentStatusCode.ARRIVED
							|| resource.getStatus() == AppointmentStatusCode.BOOKED
							|| resource.getStatus() == AppointmentStatusCode.CHECKED_IN
							|| resource.getStatus() == AppointmentStatusCode.FULFILLED;

					// Find the "actor" in each appointment that matches one of our providers
					Provider matchingProvider = null;

					for (AppointmentSearchFhirStu3Response.Entry.Resource.Participant participant : resource.getParticipant()) {
						AppointmentSearchFhirStu3Response.Entry.Resource.Participant.Actor actor = participant.getActor();

						if (actor != null) {
							for (String epicPractitionerFhirId : providersByEpicPractitionerFhirId.keySet()) {
								if (actor.getReference() != null && actor.getReference().endsWith(format("Practitioner/%s", epicPractitionerFhirId))) {
									matchingProvider = providersByEpicPractitionerFhirId.get(epicPractitionerFhirId);
									break;
								}
							}
						}

						if (matchingProvider != null)
							break;
					}

					// OK, this appointment is with a provider in our system, has a valid status, and is after "now".
					// It can be included in our results.
					// Create a simulated appointment for display.
					// TODO: match against visit type as well?
					if (legitimateStatus && matchingProvider != null && resource.getStart().isAfter(now)) {
						Appointment appointment = new Appointment();
						appointment.setAppointmentId(UUID.randomUUID());
						appointment.setAccountId(accountId);
						appointment.setAppointmentTypeId(UUID.randomUUID());
						appointment.setAttendanceStatusId(AttendanceStatusId.UNKNOWN);
						appointment.setCanceled(false);
						appointment.setCreated(now);
						appointment.setLastUpdated(now);
						appointment.setDurationInMinutes(Long.valueOf(resource.getMinutesDuration()));
						appointment.setSchedulingSystemId(SchedulingSystemId.EPIC_FHIR);
						appointment.setProviderId(matchingProvider.getProviderId());
						appointment.setStartTime(LocalDateTime.ofInstant(resource.getStart(), institution.getTimeZone()));
						appointment.setEndTime(LocalDateTime.ofInstant(resource.getEnd(), institution.getTimeZone()));
						appointment.setTimeZone(institution.getTimeZone());
						appointment.setVideoconferencePlatformId(VideoconferencePlatformId.EXTERNAL);
						appointment.setTitle(getStrings().get("1:1 Appointment with {{providerName}}", Map.of("providerName", matchingProvider.getName())));

						appointments.add(appointment);
					}
				}
			}

			return appointments;
		} else {
			return getDatabase().queryForList("""
					SELECT *
					FROM appointment
					WHERE account_id=?
					AND canceled=FALSE
					AND start_time >= ?
					ORDER BY start_time
					""", Appointment.class, accountId, now);
		}
	}

	@Nonnull
	public Set<UUID> synchronizeEpicFhirCanceledAppointmentsForAccountId(@Nullable UUID accountId) {
		if (accountId == null)
			return Set.of();

		Account account = getAccountService().findAccountById(accountId).orElse(null);

		if (account == null || account.getEpicPatientFhirId() == null)
			return Set.of();

		Institution institution = getInstitutionService().findInstitutionById(account.getInstitutionId()).get();

		// Only applicable for Epic FHIR institutions
		if (!institution.getEpicFhirEnabled())
			return Set.of();

		Instant now = Instant.now();

		// Ask Epic for all canceled appointments, then filter by only those providers that are active in Cobalt
		EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(institution.getInstitutionId());
		EpicClient epicClient = enterprisePlugin.epicClientForBackendService().get();

		// Find all Epic FHIR providers we're aware of and make them quickly accessible by their Epic Practitioner FHIR ID
		Map<String, Provider> providersByEpicPractitionerFhirId = getProviderService().findProvidersByInstitutionId(institution.getInstitutionId()).stream()
				.filter(provider -> provider.getSchedulingSystemId() == SchedulingSystemId.EPIC_FHIR)
				.collect(Collectors.toMap(Provider::getEpicPractitionerFhirId, Function.identity()));

		Set<UUID> canceledAppointmentIds = new HashSet<>();

		// Ask Epic for canceled appointments and cancel them in Cobalt as well if they are present
		AppointmentSearchFhirStu3Request canceledAppointmentsRequest = new AppointmentSearchFhirStu3Request();
		canceledAppointmentsRequest.setPatient(account.getEpicPatientFhirId());
		canceledAppointmentsRequest.setStatus(AppointmentSearchFhirStu3Request.Status.CANCELLED);

		AppointmentSearchFhirStu3Response canceledAppointmentsResponse = epicClient.appointmentSearchFhirStu3(canceledAppointmentsRequest);

		if (canceledAppointmentsResponse.getTotal() > 0) {
			getLogger().debug("Patient {} has {} canceled appointment[s] overall.", account.getEpicPatientFhirId(), canceledAppointmentsResponse.getTotal());

			// Pick out all the appointment resources that match providers in our system
			for (AppointmentSearchFhirStu3Response.Entry entry : canceledAppointmentsResponse.getEntry()) {
				AppointmentSearchFhirStu3Response.Entry.Resource resource = entry.getResource();

				if ("Appointment".equals(resource.getResourceType())) {
					if (resource.getStatus() != AppointmentStatusCode.CANCELLED)
						continue;

					// Find the "actor" in each appointment that matches one of our providers
					Provider matchingProvider = null;

					for (AppointmentSearchFhirStu3Response.Entry.Resource.Participant participant : resource.getParticipant()) {
						AppointmentSearchFhirStu3Response.Entry.Resource.Participant.Actor actor = participant.getActor();

						if (actor != null) {
							for (String epicPractitionerFhirId : providersByEpicPractitionerFhirId.keySet()) {
								if (actor.getReference() != null && actor.getReference().endsWith(format("Practitioner/%s", epicPractitionerFhirId))) {
									matchingProvider = providersByEpicPractitionerFhirId.get(epicPractitionerFhirId);
									break;
								}
							}
						}

						if (matchingProvider != null)
							break;
					}

					// Special behavior: if appointment is marked as canceled, cancel it in our database as well
					if (resource.getStatus() == AppointmentStatusCode.CANCELLED && matchingProvider != null && resource.getStart().isAfter(now)) {
						Appointment appointment = findAppointmentByEpicFhirId(resource.getId(), account.getInstitutionId()).orElse(null);

						if (appointment != null && !appointment.getCanceled()) {
							getLogger().info("Appointment ID {} with Epic FHIR ID {} was canceled on the Epic side, so canceling it in Cobalt...",
									appointment.getAppointmentId(), appointment.getEpicAppointmentFhirId());

							cancelAppointment(new CancelAppointmentRequest() {{
								setAppointmentId(appointment.getAppointmentId());
								setAppointmentCancelationReasonId(AppointmentCancelationReasonId.EXTERNALLY_CANCELED);
								setAccountId(account.getAccountId());
								setCanceledByWebhook(false);
								setCanceledForReschedule(false);
								setForce(true);
							}});

							canceledAppointmentIds.add(appointment.getAppointmentId());
						}
					}
				}
			}
		}

		return canceledAppointmentIds;
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
		UUID appointmentTypeId = request.getAppointmentTypeId();
		UUID intakeAssessmentId = request.getIntakeAssessmentId();
		UUID patientOrderId = request.getPatientOrderId();
		String emailAddress = getNormalizer().normalizeEmailAddress(request.getEmailAddress()).orElse(null);
		String phoneNumber = trimToNull(request.getPhoneNumber());
		String comment = trimToNull(request.getComment());
		String epicAppointmentFhirId = trimToNull(request.getEpicAppointmentFhirId());
		String epicAppointmentFhirIdentifierSystem = null;
		String epicAppointmentFhirIdentifierValue = null;
		String epicAppointmentFhirStu3ResponseJson = null;
		Account account = null;
		AppointmentType appointmentType = null;
		MicrosoftTeamsMeeting microsoftTeamsMeeting = null;
		UUID appointmentId = UUID.randomUUID();

		ValidationException validationException = new ValidationException();

		if (accountId == null) {
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));
		} else {
			account = getAccountService().findAccountById(accountId).orElse(null);

			if (account == null)
				validationException.add(new FieldError("accountId", getStrings().get("Account ID is invalid.")));
		}

		// Short-circuit right away if no account
		if (validationException.hasErrors())
			throw validationException;

		Institution institution = getInstitutionService().findInstitutionById(account.getInstitutionId()).get();

		if (date == null)
			validationException.add(new FieldError("date", getStrings().get("Date is required.")));

		if (time == null)
			validationException.add(new FieldError("time", getStrings().get("Time is required.")));

		// If account has no email address and none was passed in, force user to provide one (unless they are IC users...then it's optional)
		if (emailAddress == null && account != null && account.getEmailAddress() == null && !institution.getIntegratedCareEnabled()) {
			validationException.add(new FieldError("emailAddress", getStrings().get("An email address is required to book an appointment.")));

			Map<String, Object> metadata = new HashMap<>();
			metadata.put("accountEmailAddressRequired", true);

			validationException.setMetadata(metadata);
		}

		if (providerId == null)
			validationException.add(getStrings().get("Provider ID is required."));

		if (appointmentTypeId == null) {
			validationException.add(new FieldError("appointmentTypeId", getStrings().get("Appointment type ID is required for 1:1 appointments.")));
		} else {
			appointmentType = findAppointmentTypeById(appointmentTypeId).orElse(null);

			if (appointmentType == null)
				validationException.add(new FieldError("appointmentTypeId", getStrings().get("Appointment type ID is invalid.")));
		}

		AccountSession intakeSession = getSessionService().findCurrentIntakeAssessmentForAccountAndProvider(account,
				providerId, appointmentTypeId, true).orElse(null);

		if (providerId != null && getAssessmentService().findIntakeAssessmentByProviderId(providerId, appointmentTypeId).isPresent()) {
			if (intakeSession != null && !getAssessmentScoringService().isBookingAllowed(intakeSession))
				validationException.add(getStrings().get("Based on your responses you are not permitted to book with this provider."));
			else if (intakeSession == null)
				validationException.add(getStrings().get("You did not answer the necessary intake questions to book with this provider."));
		}

		if (validationException.hasErrors())
			throw validationException;

		Provider provider = getProviderService().findProviderById(providerId).get();
		EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(institution.getInstitutionId());

		if (provider.getSchedulingSystemId() == SchedulingSystemId.EPIC_FHIR && epicAppointmentFhirId == null)
			throw new ValidationException(new FieldError("epicAppointmentFhirId", getStrings().get("Epic FHIR Appointment ID is required.")));

		if (appointmentReasonId == null)
			appointmentReasonId = findNotSpecifiedAppointmentReasonByInstitutionId(institution.getInstitutionId()).getAppointmentReasonId();

		// Update account data for non-IC institutions
		if (!institution.getIntegratedCareEnabled()) {
			// If email address was provided for non-IC scenarios, update the account's email on file
			if (emailAddress != null) {
				String pinnedEmailAddress = emailAddress;
				getAccountService().updateAccountEmailAddress(new UpdateAccountEmailAddressRequest() {{
					setAccountId(accountId);
					setEmailAddress(pinnedEmailAddress);
				}});
			} else {
				emailAddress = account.getEmailAddress();
			}

			// Only care about validated email addresses for non-IC accounts
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
		}

		AcuityAppointmentType acuityAppointmentType = null;
		Long acuityCalendarId = provider.getAcuityCalendarId();
		String videoconferenceUrl = null;
		ZoneId timeZone = provider.getTimeZone();

		// Integrated care handling.  If the institution is an IC institution, require appointments be tied to orders.
		// Otherwise, it's not permitted to tie to orders.
		if (institution.getIntegratedCareEnabled()) {
			if (patientOrderId == null)
				throw new ValidationException(new FieldError("patientOrderId", getStrings().get("Patient Order ID is required.")));

			// TODO: track event
		} else {
			patientOrderId = null;
		}

		// Special handling for Acuity - read the latest value for appointment type
		if (appointmentType.getSchedulingSystemId() == SchedulingSystemId.ACUITY)
			// TODO: use cache here
			acuityAppointmentType = getAcuitySchedulingClient().findAppointmentTypeById(appointmentType.getAcuityAppointmentTypeId()).get();

		// Ensure we can't double-book the same time.
		// EPIC FHIR providers have a special flow for this where we just ask Epic directly instead of our own DB.
		if (provider.getSchedulingSystemId() == SchedulingSystemId.EPIC_FHIR) {
			// Quickly force a sync to get latest data
			getEpicFhirSyncManager().syncProviderAvailability(providerId, date);

			boolean slotStillAvailable = false;

			ProviderFindRequest providerFindRequest = new ProviderFindRequest();
			providerFindRequest.setStartDate(date);
			providerFindRequest.setStartTime(LocalTime.MIN);
			providerFindRequest.setEndDate(date);
			providerFindRequest.setEndTime(LocalTime.MAX);
			providerFindRequest.setInstitutionId(institution.getInstitutionId());
			providerFindRequest.setProviderId(providerId);

			List<ProviderFind> providerFinds = getProviderService().findProviders(providerFindRequest, account);

			for (ProviderFind providerFind : providerFinds) {
				if (!providerFind.getProviderId().equals(providerId))
					continue;

				for (AvailabilityDate availabilityDate : providerFind.getDates()) {
					if (!availabilityDate.getDate().equals(date))
						continue;

					for (ProviderFind.AvailabilityTime availabilityTime : availabilityDate.getTimes()) {
						if (availabilityTime.getTime().equals(time)) {
							slotStillAvailable = true;
							break;
						}
					}
				}
			}

			if (!slotStillAvailable) {
				getLogger().info("Can't find an open timeslot for provider ID {} on {} at {}", provider.getProviderId(), date, time);
				throw new ValidationException(getStrings().get("Sorry, this appointment time is no longer available. Please pick a different time."), Map.of("appointmentTimeslotUnavailable", true));
			}
		} else {
			List<Appointment> existingAppointmentsForDate = findAppointmentsByProviderId(providerId, date, date.plusDays(1));
			LocalDateTime appointmentStartTime = LocalDateTime.of(date, time);

			for (Appointment existingAppointmentForDate : existingAppointmentsForDate) {
				if (existingAppointmentForDate.getStartTime().equals(appointmentStartTime)) {
					getLogger().info("Attempted to book an appointment with provider ID {} at {} but existing appointment ID {} already is at that time", provider.getProviderId(), appointmentStartTime, existingAppointmentForDate.getAppointmentId());
					throw new ValidationException(getStrings().get("Sorry, this appointment time is no longer available. Please pick a different time.", Map.of("appointmentTimeslotUnavailable", true)));
				}
			}

			// Ensure we are not booking within the provider's lead time
			if (provider.getSchedulingLeadTimeInHours() != null) {
				LocalDateTime now = LocalDateTime.now(provider.getTimeZone());
				long hoursUntilAppointment = ChronoUnit.HOURS.between(now, appointmentStartTime);

				if (hoursUntilAppointment < provider.getSchedulingLeadTimeInHours()) {
					getLogger().info("Attempted to book an appointment {} hours away, but provider ID {} lead time in hours is {}", hoursUntilAppointment, provider.getProviderId(), provider.getSchedulingLeadTimeInHours());
					throw new ValidationException(getStrings().get("Sorry, this appointment time is no longer available. Please pick a different time.", Map.of("appointmentTimeslotUnavailable", true)));
				}
			}
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
		VideoconferencePlatformId videoconferencePlatformId = provider.getVideoconferencePlatformId();

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
		} else if (videoconferencePlatformId == VideoconferencePlatformId.MICROSOFT_TEAMS) {
			// Prepare Teams meeting request
			OnlineMeetingCreateRequest onlineMeetingCreateRequest = new OnlineMeetingCreateRequest();
			onlineMeetingCreateRequest.setUserId(institution.getMicrosoftTeamsUserId());
			onlineMeetingCreateRequest.setSubject(getStrings().get("1:1 Appointment with {{providerName}}", Map.of(
					"providerName", provider.getName()
			)));
			onlineMeetingCreateRequest.setStartDateTime(meetingStartTime.atZone(timeZone));
			onlineMeetingCreateRequest.setEndDateTime(meetingEndTime.atZone(timeZone));

			try {
				// Create the Teams meeting
				UUID microsoftTeamsMeetingId = getSystemService().createMicrosoftTeamsMeeting(new CreateMicrosoftTeamsMeetingRequest() {{
					setInstitutionId(institution.getInstitutionId());
					setCreatedByAccountId(accountId);
					setOnlineMeetingCreateRequest(onlineMeetingCreateRequest);
				}});

				// Use the "join" URL as the videoconference URL
				microsoftTeamsMeeting = getSystemService().findMicrosoftTeamsMeetingById(microsoftTeamsMeetingId).get();
				videoconferenceUrl = microsoftTeamsMeeting.getJoinUrl();
			} catch (ValidationException e) {
				// We want to know if there is a problem creating Teams meetings.
				// In theory this above code should not fail unless there is a systemic issue, e.g. Teams is down, or creds revoked
				getErrorReporter().report(e);

				// Let the user-friendly exception bubble out
				throw e;
			}
		} else if (videoconferencePlatformId == VideoconferencePlatformId.TELEPHONE) {
			// Hack: phone number is encoded as the URL in the provider sheet.
			// The real URL is the webapp - we have a `GET /appointments/{appointmentId}`
			appointmentPhoneNumber = provider.getVideoconferenceUrl();
			// TODO: this defaults to "patient" experience type but is also used by staff.
			// Doesn't matter atm, and this concept of TELEPHONE should be removed/reworked, but just noting here for posterity...
			videoconferenceUrl = format("%s/appointments/%s", getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(provider.getInstitutionId(), UserExperienceTypeId.PATIENT).get(), appointmentId);
		} else if (videoconferencePlatformId == VideoconferencePlatformId.EXTERNAL) {
			videoconferenceUrl = provider.getVideoconferenceUrl();
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
					throw new ValidationException(getStrings().get("Sorry, this appointment time is no longer available. Please pick a different time.", Map.of("appointmentTimeslotUnavailable", true)));

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
				EpicClient epicClient = enterprisePlugin.epicClientForBackendService().get();
				account = getAccountService().findAccountById(accountId).get();

				// Figure out the department for this provider and timeslot
				EpicDepartment epicDepartment = getInstitutionService().findEpicDepartmentByProviderIdAndTimeslot(providerId, LocalDateTime.of(date, time)).orElse(null);

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
					throw new ValidationException(getStrings().get("Sorry, this appointment time is no longer available. Please pick a different time.", Map.of("appointmentTimeslotUnavailable", true)));

				// Now we are ready to book
				ScheduleAppointmentWithInsuranceRequest appointmentRequest = new ScheduleAppointmentWithInsuranceRequest();
				appointmentRequest.setPatientID(account.getEpicPatientUniqueId());
				appointmentRequest.setPatientIDType(account.getEpicPatientUniqueIdType());
				appointmentRequest.setDepartmentID(epicDepartment.getDepartmentId());
				appointmentRequest.setDepartmentIDType(epicDepartment.getDepartmentIdType());
				appointmentRequest.setProviderID(provider.getEpicProviderId());
				appointmentRequest.setProviderIDType(provider.getEpicProviderIdType());
				appointmentRequest.setVisitTypeID(appointmentType.getEpicVisitTypeId());
				appointmentRequest.setVisitTypeIDType(appointmentType.getEpicVisitTypeIdType());
				appointmentRequest.setDate(epicClient.formatDateWithHyphens(date));
				appointmentRequest.setTime(epicClient.formatTimeInMilitary(time));
				appointmentRequest.setIsReviewOnly(false);
				appointmentRequest.setComments(List.of("Booked with Cobalt"));

				// Perform any institution-specific customizations needed
				enterprisePlugin.customizeScheduleAppointmentWithInsuranceRequest(appointmentRequest, account);

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
		} else if (appointmentType.getSchedulingSystemId() == SchedulingSystemId.EPIC_FHIR) {
			EpicClient epicClient = enterprisePlugin.epicClientForBackendService().get();

			AppointmentBookFhirStu3Request appointmentBookRequest = new AppointmentBookFhirStu3Request();
			appointmentBookRequest.setAppointment(epicAppointmentFhirId);
			appointmentBookRequest.setPatient(account.getEpicPatientFhirId());
			appointmentBookRequest.setAppointmentNote(getStrings().get("Booked via Cobalt"));

			// In addition to storing off raw JSON from the response, pull out identifying information for the appointment
			AppointmentBookFhirStu3Response appointmentBookResponse = epicClient.appointmentBookFhirStu3(appointmentBookRequest);
			epicAppointmentFhirStu3ResponseJson = appointmentBookResponse.getRawJson();

			try {
				AppointmentBookFhirStu3Response.Entry entry = appointmentBookResponse.getEntry().get(0);
				epicAppointmentFhirId = entry.getResource().getId();

				AppointmentBookFhirStu3Response.Entry.Resource.Identifier identifier = entry.getResource().getIdentifier().get(0);
				epicAppointmentFhirIdentifierSystem = trimToNull(identifier.getSystem());
				epicAppointmentFhirIdentifierValue = trimToNull(identifier.getValue());
			} catch (Exception e) {
				getLogger().error(format("Unable to parse %s - JSON was: %s", AppointmentBookFhirStu3Response.class.getSimpleName(), appointmentBookResponse.getRawJson()), e);
				getErrorReporter().report(e);
			}
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
						"appointment_type_id, acuity_appointment_id, bluejeans_meeting_id, bluejeans_participant_passcode, title, start_time, end_time, " +
						"duration_in_minutes, time_zone, videoconference_url, epic_contact_id, epic_contact_id_type, videoconference_platform_id, " +
						"phone_number, appointment_reason_id, comment, intake_assessment_id, scheduling_system_id, intake_account_session_id, patient_order_id, " +
						"microsoft_teams_meeting_id, epic_appointment_fhir_id, epic_appointment_fhir_identifier_system, epic_appointment_fhir_identifier_value, epic_appointment_fhir_stu3_response) " +
						"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,CAST (? AS JSONB))", appointmentId, providerId,
				accountId, createdByAccountId, appointmentTypeId, acuityAppointmentId, bluejeansMeetingId, bluejeansParticipantPasscode,
				title, meetingStartTime, meetingEndTime, durationInMinutes, timeZone, videoconferenceUrl, epicContactId,
				epicContactIdType, videoconferencePlatformId, appointmentPhoneNumber, appointmentReasonId, comment, intakeAssessmentId, appointmentType.getSchedulingSystemId(),
				intakeAccountSessionId, patientOrderId, microsoftTeamsMeeting == null ? null : microsoftTeamsMeeting.getMicrosoftTeamsMeetingId(),
				epicAppointmentFhirId, epicAppointmentFhirIdentifierSystem, epicAppointmentFhirIdentifierValue, epicAppointmentFhirStu3ResponseJson);

		sendProviderScoreEmail(provider, account, emailAddress, phoneNumber, videoconferenceUrl,
				getFormatter().formatDate(meetingStartTime.toLocalDate()),
				getFormatter().formatTime(meetingStartTime.toLocalTime()),
				getFormatter().formatTime(meetingEndTime.toLocalTime()), intakeSession);

		ZoneId pinnedTimeZone = timeZone;
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
					getEpicSyncManager().syncProviderAvailability(providerId, meetingStartTime.toLocalDate(), true);
				});
			} else if (schedulingSystemId == SchedulingSystemId.EPIC_FHIR) {
				ForkJoinPool.commonPool().execute(() -> {
					getEpicFhirSyncManager().syncProviderAvailability(providerId, meetingStartTime.toLocalDate(), true);
				});
			} else if (schedulingSystemId == SchedulingSystemId.COBALT) {
				// For native appointments, we are responsible for sending emails out
				getDatabase().transaction(() -> {
					getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.APPOINTMENT_CREATE_MESSAGES);
					sendPatientAndProviderCobaltAppointmentCreatedEmails(appointmentId);
				});
			}
		});

		//Send any patient appointment interactions that might be defined for the provider being scheduled with
		List<Interaction> patientInteractions = getProviderService().findInteractionsByTypeAndProviderId(InteractionType.InteractionTypeId.APPOINTMENT_PATIENT, providerId);

		if (!patientInteractions.isEmpty()) {
			for (Interaction interaction : patientInteractions) {
				LocalDateTime followupSendTime = meetingEndTime;
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
				LocalDateTime followupSendTime = meetingEndTime;
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

		// For IC, the act of booking an appointment cancels any booking reminder messages that might be pending
		if (institution.getIntegratedCareEnabled()) {
			getPatientOrderService().deleteFuturePatientOrderScheduledMessageGroupsForPatientOrderId(patientOrderId, accountId, Set.of(
					PatientOrderScheduledMessageType.PatientOrderScheduledMessageTypeId.APPOINTMENT_BOOKING_REMINDER
			));
		}

		scheduleAppointmentFeedbackSurveyMessagesIfEnabled(appointmentId);

		return appointmentId;
	}

	@Nonnull
	protected Boolean scheduleAppointmentFeedbackSurveyMessagesIfEnabled(@Nullable UUID appointmentId) {
		if (appointmentId == null)
			return false;

		Appointment appointment = findAppointmentById(appointmentId).orElse(null);

		if (appointment == null)
			return false;

		// These should never be null and will fail-fast if so
		Account account = getAccountService().findAccountById(appointment.getAccountId()).get();
		Institution institution = getInstitutionService().findInstitutionById(account.getInstitutionId()).get();

		if (!institution.getAppointmentFeedbackSurveyEnabled() || institution.getAppointmentFeedbackSurveyUrl() == null)
			return false;

		LocalDateTime scheduledAt = appointment.getStartTime().plusMinutes(institution.getAppointmentFeedbackSurveyDelayInMinutes());

		UUID appointmentScheduledMessageId = UUID.randomUUID();
		UUID messageId = UUID.randomUUID();

		Map<String, Object> messageContext = new HashMap<>();
		messageContext.put("appointmentFeedbackSurveyUrl", institution.getAppointmentFeedbackSurveyUrl());
		messageContext.put("appointmentFeedbackSurveyDurationDescription", institution.getAppointmentFeedbackSurveyDurationDescription());

		EmailMessage emailMessage = new EmailMessage.Builder(messageId, institution.getInstitutionId(), EmailMessageTemplate.APPOINTMENT_FEEDBACK_SURVEY_PATIENT, account.getLocale())
				.toAddresses(List.of(account.getEmailAddress()))
				.fromAddress(institution.getDefaultFromEmailAddress())
				.messageContext(messageContext)
				.build();

		UUID scheduledMessageId = getMessageService().createScheduledMessage(new CreateScheduledMessageRequest() {{
			setMetadata(Map.of("appointmentScheduledMessageId", appointmentScheduledMessageId));
			setMessage(emailMessage);
			setTimeZone(appointment.getTimeZone());
			setScheduledAt(scheduledAt);
		}});

		getDatabase().execute("""
						INSERT INTO appointment_scheduled_message (
							appointment_scheduled_message_id,
							appointment_scheduled_message_type_id,
							appointment_id,
							scheduled_message_id
						) VALUES (?,?,?,?)
						""", appointmentScheduledMessageId, AppointmentScheduledMessageTypeId.FEEDBACK_SURVEY,
				appointment.getAppointmentId(), scheduledMessageId);

		return true;
	}

	@Nonnull
	public List<AppointmentScheduledMessage> findFutureAppointmentScheduledMessagesByAppointmentId(@Nullable UUID appointmentId) {
		if (appointmentId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT asm.*
				FROM appointment_scheduled_message asm, scheduled_message sm
				WHERE asm.appointment_id=?
				AND asm.scheduled_message_id=sm.scheduled_message_id
				AND sm.scheduled_at AT TIME ZONE sm.time_zone > NOW()
				AND sm.scheduled_message_status_id = ?
				ORDER BY sm.scheduled_at AT TIME ZONE sm.time_zone
				""", AppointmentScheduledMessage.class, appointmentId, ScheduledMessageStatusId.PENDING);
	}

	@Nonnull
	public Set<UUID> cancelAllFutureScheduledMessagesForAppointmentId(@Nonnull UUID appointmentId,
																																		@Nonnull UUID accountId) {
		return cancelFutureScheduledMessagesForAppointmentId(appointmentId, accountId, Arrays.stream(AppointmentScheduledMessageTypeId.values()).collect(Collectors.toSet()));
	}

	@Nonnull
	public Set<UUID> cancelFutureScheduledMessagesForAppointmentId(@Nonnull UUID appointmentId,
																																 @Nonnull UUID accountId,
																																 @Nonnull Set<AppointmentScheduledMessageTypeId> appointmentScheduledMessageTypeIdsToCancel) {
		requireNonNull(appointmentId);
		requireNonNull(accountId);
		requireNonNull(appointmentScheduledMessageTypeIdsToCancel);

		Set<UUID> canceledAppointmentScheduledMessageIds = new HashSet<>();

		List<AppointmentScheduledMessage> futureAppointmentScheduledMessagesToCancel = findFutureAppointmentScheduledMessagesByAppointmentId(appointmentId).stream()
				.filter(futureAppointmentScheduledMessage -> appointmentScheduledMessageTypeIdsToCancel.contains(futureAppointmentScheduledMessage.getAppointmentScheduledMessageTypeId()))
				.collect(Collectors.toList());

		for (AppointmentScheduledMessage futureAppointmentScheduledMessageToCancel : futureAppointmentScheduledMessagesToCancel) {
			boolean canceled = getMessageService().cancelScheduledMessage(futureAppointmentScheduledMessageToCancel.getScheduledMessageId());

			if (canceled)
				canceledAppointmentScheduledMessageIds.add(futureAppointmentScheduledMessageToCancel.getAppointmentScheduledMessageId());
		}

		return canceledAppointmentScheduledMessageIds;
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

		if (appointment.getVideoconferencePlatformId() == VideoconferencePlatformId.EXTERNAL && appointment.getVideoconferenceUrl() == null) {
			getLogger().debug("Appointment ID {} uses external videoconferencing and there is no videoconference URL, so don't send out booking email.", appointment.getAppointmentId());
			return;
		}

		// Failsafe; we shouldn't get here
		if (appointment.getSchedulingSystemId() == SchedulingSystemId.EPIC || appointment.getSchedulingSystemId() == SchedulingSystemId.EPIC_FHIR) {
			getLogger().warn("Appointment ID {} is Epic-backed, so don't send out booking emails.", appointment.getAppointmentId());
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

		String webappBaseUrlForPatient = getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(provider.getInstitutionId(), UserExperienceTypeId.PATIENT).get();
		String webappBaseUrlForStaff = getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(provider.getInstitutionId(), UserExperienceTypeId.STAFF).get();
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
			cobaltPatientEmailMessageContext.put("cancelUrl", format("%s/my-calendar?appointmentId=%s&action=cancel", webappBaseUrlForPatient, appointmentId));
			cobaltPatientEmailMessageContext.put("icalUrl", format("%s/appointments/%s/ical", webappBaseUrlForPatient, appointmentId));
			cobaltPatientEmailMessageContext.put("googleCalendarUrl", format("%s/appointments/%s/google-calendar", webappBaseUrlForPatient, appointmentId));
			cobaltPatientEmailMessageContext.put("anotherTimeUrl", format("%s/connect-with-support", webappBaseUrlForPatient));
			cobaltPatientEmailMessageContext.put("showMicrosoftTeamsAnonymousDirections", appointment.getVideoconferencePlatformId() == VideoconferencePlatformId.MICROSOFT_TEAMS);

			EmailMessage patientEmailMessage = new EmailMessage.Builder(account.getInstitutionId(), EmailMessageTemplate.APPOINTMENT_CREATED_PATIENT, account.getLocale())
					.toAddresses(Collections.singletonList(account.getEmailAddress()))
					.replyToAddress(provider.getEmailAddress())
					.messageContext(cobaltPatientEmailMessageContext)
					.emailAttachments(List.of(generateICalInviteAsEmailAttachment(appointment, InviteMethod.REQUEST)))
					.build();

			getMessageService().enqueueMessage(patientEmailMessage);

			// Schedule a reminder message for this booking based on institution rules
			LocalDate reminderMessageDate = appointment.getStartTime().toLocalDate().minusDays(institution.getAppointmentReservationDefaultReminderDayOffset());
			LocalTime reminderMessageTimeOfDay = institution.getAppointmentReservationDefaultReminderTimeOfDay();

			EmailMessage patientReminderEmailMessage = new EmailMessage.Builder(account.getInstitutionId(), EmailMessageTemplate.APPOINTMENT_REMINDER_PATIENT, account.getLocale())
					.toAddresses(Collections.singletonList(account.getEmailAddress()))
					.replyToAddress(provider.getEmailAddress())
					.messageContext(cobaltPatientEmailMessageContext)
					.build();

			UUID patientReminderScheduledMessageId = getMessageService().createScheduledMessage(new CreateScheduledMessageRequest<>() {{
				setMetadata(Map.of("appointmentId", appointmentId));
				setMessage(patientReminderEmailMessage);
				setTimeZone(provider.getTimeZone());
				setScheduledAt(LocalDateTime.of(reminderMessageDate, reminderMessageTimeOfDay));
			}});

			// TODO: this should be migrated over to the appointment_scheduled_message construct
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
		cobaltProviderEmailMessageContext.put("icalUrl", format("%s/appointments/%s/ical", webappBaseUrlForStaff, appointmentId));
		cobaltProviderEmailMessageContext.put("googleCalendarUrl", format("%s/appointments/%s/google-calendar", webappBaseUrlForStaff, appointmentId));

		// With native scheduling, providers can deeplink right to the appointment on their calendar
		if (appointment.getSchedulingSystemId() == SchedulingSystemId.COBALT)
			cobaltProviderEmailMessageContext.put("providerSchedulingUrl", format("%s/scheduling/appointments/%s", webappBaseUrlForStaff, appointmentId));

		EmailMessage providerEmailMessage = new EmailMessage.Builder(provider.getInstitutionId(), EmailMessageTemplate.APPOINTMENT_CREATED_PROVIDER, provider.getLocale())
				.toAddresses(List.of(provider.getEmailAddress()))
				.replyToAddress(replyToAddressForEmailsTargetingProvider(provider))
				.messageContext(cobaltProviderEmailMessageContext)
				.emailAttachments(List.of(generateICalInviteAsEmailAttachment(appointment, InviteMethod.REQUEST)))
				.build();

		getMessageService().enqueueMessage(providerEmailMessage);
	}

	protected void sendPatientAndProviderCobaltAppointmentCanceledEmails(@Nonnull UUID appointmentId) {
		requireNonNull(appointmentId);

		Appointment appointment = findAppointmentById(appointmentId).get();

		if (appointment.getVideoconferencePlatformId() == VideoconferencePlatformId.SWITCHBOARD) {
			getLogger().debug("Appointment ID {} is Switchboard-backed, so don't send out cancelation emails.", appointment.getAppointmentId());
			return;
		}

		if (appointment.getVideoconferencePlatformId() == VideoconferencePlatformId.EXTERNAL && appointment.getVideoconferenceUrl() == null) {
			getLogger().debug("Appointment ID {} uses external videoconferencing and there is no videoconference URL, so don't send out cancelation emails.", appointment.getAppointmentId());
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

			EmailMessage patientEmailMessage = new EmailMessage.Builder(provider.getInstitutionId(), EmailMessageTemplate.APPOINTMENT_CANCELED_PATIENT, account.getLocale())
					.toAddresses(Collections.singletonList(account.getEmailAddress()))
					.replyToAddress(provider.getEmailAddress())
					.messageContext(cobaltPatientEmailMessageContext)
					.emailAttachments(List.of(generateICalInviteAsEmailAttachment(appointment, InviteMethod.CANCEL)))
					.build();

			getMessageService().enqueueMessage(patientEmailMessage);
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

		EmailMessage providerEmailMessage = new EmailMessage.Builder(provider.getInstitutionId(), EmailMessageTemplate.APPOINTMENT_CANCELED_PROVIDER, provider.getLocale())
				.toAddresses(List.of(provider.getEmailAddress()))
				.replyToAddress(replyToAddressForEmailsTargetingProvider(provider))
				.messageContext(cobaltProviderEmailMessageContext)
				.emailAttachments(List.of(generateICalInviteAsEmailAttachment(appointment, InviteMethod.CANCEL)))
				.build();

		getMessageService().enqueueMessage(providerEmailMessage);
	}

	@Nonnull
	protected String replyToAddressForEmailsTargetingProvider(@Nonnull Provider provider) {
		requireNonNull(provider);
		// TODO: institution-specific support once we see how this goes
		return "support@cobaltinnovations.org";
	}

	@Nonnull
	public Boolean cancelAppointment(@Nonnull CancelAppointmentRequest request) {
		requireNonNull(request);

		UUID appointmentId = request.getAppointmentId();
		UUID accountId = request.getAccountId();
		Boolean canceledByWebhook = request.getCanceledByWebhook() == null ? false : request.getCanceledByWebhook();
		AppointmentCancelationReasonId appointmentCancelationReasonId = request.getAppointmentCancelationReasonId() == null ? AppointmentCancelationReasonId.UNSPECIFIED : request.getAppointmentCancelationReasonId();
		Appointment appointment = null;
		Account account = null;
		ValidationException validationException = new ValidationException();

		if (appointmentId == null) {
			validationException.add(new FieldError("appointmentId", getStrings().get("Appointment ID is required.")));
		} else {
			appointment = findAppointmentById(appointmentId).orElse(null);

			if (appointment == null) {
				validationException.add(new FieldError("appointmentId", getStrings().get("Appointment ID is invalid.")));
			} else {
				if (appointment.getCanceled()) {
					getLogger().info("Appointment is already canceled, not re-canceling.");
					return false;
				}

				// Special behavior: you cannot directly cancel an Epic FHIR appointment unless "force" is specified
				if (appointment.getSchedulingSystemId() == SchedulingSystemId.EPIC_FHIR && !request.isForce()) {
					Provider provider = getProviderService().findProviderById(appointment.getProviderId()).get();
					Institution institution = getInstitutionService().findInstitutionById(provider.getInstitutionId()).get();
					String clinicalSupportPhoneNumber = institution.getClinicalSupportPhoneNumber();

					if (clinicalSupportPhoneNumber == null)
						validationException.add(getStrings().get("Appointment ID is invalid."));
					else
						validationException.add(getStrings().get("In order to cancel this appointment, please call {{phoneNumber}}.",
								Map.of("phoneNumber", getFormatter().formatPhoneNumber(clinicalSupportPhoneNumber))));
				}
			}
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
				EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(account.getInstitutionId());
				EpicClient epicClient = enterprisePlugin.epicClientForBackendService().get();

				com.cobaltplatform.api.integration.epic.request.CancelAppointmentRequest.Patient patient = new com.cobaltplatform.api.integration.epic.request.CancelAppointmentRequest.Patient();
				patient.setID(account.getEpicPatientUniqueId());
				patient.setType(account.getEpicPatientUniqueIdType());

				com.cobaltplatform.api.integration.epic.request.CancelAppointmentRequest.Contact contact = new com.cobaltplatform.api.integration.epic.request.CancelAppointmentRequest.Contact();
				contact.setID(appointment.getEpicContactId());
				contact.setType(appointment.getEpicContactIdType());

				com.cobaltplatform.api.integration.epic.request.CancelAppointmentRequest cancelRequest = new com.cobaltplatform.api.integration.epic.request.CancelAppointmentRequest();
				cancelRequest.setReason("Patient requested cancelation via Cobalt");
				cancelRequest.setPatient(patient);
				cancelRequest.setContact(contact);

				// Perform any institution-specific customizations needed
				enterprisePlugin.customizeCancelAppointmentRequest(cancelRequest, account);

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

		// Delete Bluejeans meeting if necessary
		if (appointment.getBluejeansMeetingId() != null) {
			try {
				getBluejeansClient().cancelScheduledMeeting(provider.getBluejeansUserId().intValue(), appointment.getBluejeansMeetingId().intValue(),
						false, getStrings().get("The patient canceled the meeting."));
			} catch (Exception e) {
				getLogger().warn("Unable to cancel Bluejeans meeting, continuing on...", e);
			}
		}

		boolean canceled = getDatabase().execute("UPDATE appointment SET canceled=TRUE, attendance_status_id=?, canceled_at=NOW(), " +
						"canceled_for_reschedule=?, rescheduled_appointment_id=?, appointment_cancelation_reason_id=? WHERE appointment_id=?",
				AttendanceStatusId.CANCELED, request.getCanceledForReschedule(), request.getRescheduleAppointmentId(), appointmentCancelationReasonId, appointmentId) > 0;

		// Cancel any interaction instances that are scheduled for this appointment
		getInteractionService().cancelInteractionInstancesForAppointment(appointmentId);

		Appointment pinnedAppointment = appointment;

		// Cancel any scheduled reminder message for the patient
		// TODO: this should be migrated over to the appointment_scheduled_message construct
		getMessageService().cancelScheduledMessage(appointment.getPatientReminderScheduledMessageId());

		// Cancel other future scheduled messages
		cancelAllFutureScheduledMessagesForAppointmentId(appointment.getAppointmentId(), accountId);

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
			} else if (appointmentType.getSchedulingSystemId() == SchedulingSystemId.EPIC_FHIR) {
				ForkJoinPool.commonPool().execute(() -> {
					getEpicFhirSyncManager().syncProviderAvailability(pinnedAppointment.getProviderId(), pinnedAppointment.getStartTime().toLocalDate());
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

	// TODO: this is replaced by native scheduling, should remove...
	@Deprecated
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

		if (provider.getSchedulingSystemId() == SchedulingSystemId.COBALT && provider.getVideoconferencePlatformId() != VideoconferencePlatformId.TELEPHONE) {
			getLogger().debug("Provider {} uses native scheduling, do not send a provider score email.", provider.getName());
			return;
		}

		if (provider.getSchedulingSystemId() == SchedulingSystemId.EPIC_FHIR) {
			getLogger().debug("Provider {} uses Epic FHIR scheduling, do not send a provider score email.", provider.getName());
			return;
		}

		if (provider.getSchedulingSystemId() == SchedulingSystemId.EPIC) {
			getLogger().debug("Provider {} uses Epic scheduling, do not send a provider score email.", provider.getName());
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
						getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(provider.getInstitutionId(), UserExperienceTypeId.STAFF).get(), intakeSession.getAccountSessionId());
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

		getMessageService().enqueueMessage(new EmailMessage.Builder(provider.getInstitutionId(), EmailMessageTemplate.PROVIDER_ASSESSMENT_SCORES, provider.getLocale() == null ? Locale.US : provider.getLocale())
				.toAddresses(new ArrayList<>() {{
					add(provider.getEmailAddress());
				}})
				.replyToAddress(replyToAddressForEmailsTargetingProvider(provider))
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
				appointment.getTimeZone(), appointment.getVideoconferenceUrl(), inviteMethod, inviteOrganizer, inviteAttendee, OrganizerAttendeeStrategy.ORGANIZER_AND_ATTENDEE);
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
	public List<AppointmentTime> findAppointmentTimes() {
		return getDatabase().queryForList("SELECT * FROM appointment_time ORDER BY display_order", AppointmentTime.class);
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
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
	protected EpicFhirSyncManager getEpicFhirSyncManager() {
		return this.epicFhirSyncManager;
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
	protected SystemService getSystemService() {
		return this.systemServiceProvider.get();
	}

	@Nonnull
	protected PatientOrderService getPatientOrderService() {
		return this.patientOrderServiceProvider.get();
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

	@Nonnull
	protected ErrorReporter getErrorReporter() {
		return this.errorReporter;
	}
}
