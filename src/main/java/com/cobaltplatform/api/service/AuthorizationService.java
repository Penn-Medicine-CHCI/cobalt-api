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
import com.cobaltplatform.api.model.db.AccountCapabilityType.AccountCapabilityTypeId;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.CalendarPermission.CalendarPermissionId;
import com.cobaltplatform.api.model.db.Course;
import com.cobaltplatform.api.model.db.CourseSession;
import com.cobaltplatform.api.model.db.GroupSession;
import com.cobaltplatform.api.model.db.GroupSessionRequest;
import com.cobaltplatform.api.model.db.GroupSessionStatus.GroupSessionStatusId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionTopicCenter;
import com.cobaltplatform.api.model.db.Interaction;
import com.cobaltplatform.api.model.db.InteractionInstance;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.RawPatientOrder;
import com.cobaltplatform.api.model.db.ReportType.ReportTypeId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.ScreeningFlow;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.db.TopicCenter;
import com.cobaltplatform.api.model.db.Video;
import com.cobaltplatform.api.model.security.AccountCapabilities;
import com.cobaltplatform.api.model.service.AccountCapabilityFlags;
import com.cobaltplatform.api.util.Normalizer;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AuthorizationService {
	@Nonnull
	private final javax.inject.Provider<AvailabilityService> availabilityServiceProvider;
	@Nonnull
	private final javax.inject.Provider<GroupSessionService> groupSessionServiceProvider;
	@Nonnull
	private final javax.inject.Provider<InteractionService> interactionServiceProvider;
	@Nonnull
	private final javax.inject.Provider<AppointmentService> appointmentServiceProvider;
	@Nonnull
	private final javax.inject.Provider<TopicCenterService> topicCenterServiceProvider;
	@Nonnull
	private final javax.inject.Provider<ReportingService> reportingServiceProvider;
	@Nonnull
	private final javax.inject.Provider<PatientOrderService> patientOrderServiceProvider;
	@Nonnull
	private final javax.inject.Provider<StudyService> studyServiceProvider;
	@Nonnull
	private final javax.inject.Provider<CourseService> courseServiceProvider;
	@Nonnull
	private final Normalizer normalizer;

	@Inject
	public AuthorizationService(@Nonnull javax.inject.Provider<AvailabilityService> availabilityServiceProvider,
															@Nonnull javax.inject.Provider<GroupSessionService> groupSessionServiceProvider,
															@Nonnull javax.inject.Provider<InteractionService> interactionServiceProvider,
															@Nonnull javax.inject.Provider<AppointmentService> appointmentServiceProvider,
															@Nonnull javax.inject.Provider<TopicCenterService> topicCenterServiceProvider,
															@Nonnull javax.inject.Provider<ReportingService> reportingServiceProvider,
															@Nonnull javax.inject.Provider<PatientOrderService> patientOrderServiceProvider,
															@Nonnull javax.inject.Provider<StudyService> studyServiceProvider,
															@Nonnull javax.inject.Provider<CourseService> courseServiceProvider,
															@Nonnull Normalizer normalizer) {
		requireNonNull(availabilityServiceProvider);
		requireNonNull(groupSessionServiceProvider);
		requireNonNull(interactionServiceProvider);
		requireNonNull(appointmentServiceProvider);
		requireNonNull(topicCenterServiceProvider);
		requireNonNull(reportingServiceProvider);
		requireNonNull(patientOrderServiceProvider);
		requireNonNull(studyServiceProvider);
		requireNonNull(courseServiceProvider);
		requireNonNull(normalizer);

		this.availabilityServiceProvider = availabilityServiceProvider;
		this.groupSessionServiceProvider = groupSessionServiceProvider;
		this.interactionServiceProvider = interactionServiceProvider;
		this.appointmentServiceProvider = appointmentServiceProvider;
		this.topicCenterServiceProvider = topicCenterServiceProvider;
		this.reportingServiceProvider = reportingServiceProvider;
		this.patientOrderServiceProvider = patientOrderServiceProvider;
		this.studyServiceProvider = studyServiceProvider;
		this.courseServiceProvider = courseServiceProvider;
		this.normalizer = normalizer;
	}

	@Nonnull
	public AccountCapabilities determineAccountCapabilities(@Nonnull Account account) {
		requireNonNull(account);

		AccountCapabilities accountCapabilities = new AccountCapabilities();

		if (account.getRoleId() == RoleId.ADMINISTRATOR) {
			accountCapabilities.setViewNavAdminGroupSession(true);
			accountCapabilities.setViewNavAdminGroupSessionRequest(true);
			accountCapabilities.setViewNavAdminMyContent(true);
			accountCapabilities.setViewNavAdminAvailableContent(true);
		} else {
			accountCapabilities.setViewNavAdminGroupSession(getGroupSessionService().canTakeActionOnGroupSessions(account));
			accountCapabilities.setViewNavAdminGroupSessionRequest(getGroupSessionService().canTakeActionOnGroupSessionRequests(account));
		}

		accountCapabilities.setViewNavAdminReports(getReportingService().findReportTypesAvailableForAccount(account).size() > 0);

		return accountCapabilities;
	}

	@Nonnull
	public Boolean canViewReportTypeId(@Nonnull Account account,
																		 @Nonnull ReportTypeId reportTypeId) {
		requireNonNull(account);
		requireNonNull(reportTypeId);

		return getReportingService().findReportTypesAvailableForAccount(account).stream()
				.map(reportType -> reportType.getReportTypeId())
				.collect(Collectors.toSet())
				.contains(reportTypeId);
	}

	@Nonnull
	@Deprecated
	// This should be removed - with the removal of super admin role, there is no longer the concept of accounts who can
	// cross institution boundaries.
	// Once FE is updated to no longer rely on this structure, we can remove it.
	// Should instead use new AccountCapabilityFlags/determineAccountCapabilityFlagsForAccount below
	public Map<InstitutionId, AccountCapabilities> determineAccountCapabilitiesByInstitutionId(@Nonnull Account account) {
		requireNonNull(account);
		return Map.of(account.getInstitutionId(), determineAccountCapabilities(account));
	}

	@Nonnull
	public AccountCapabilityFlags determineAccountCapabilityFlagsForAccount(@Nonnull Account account) {
		requireNonNull(account);

		Set<AccountCapabilityTypeId> accountCapabilityTypeIds = account.getAccountCapabilityTypeIds();

		AccountCapabilityFlags accountCapabilityFlags = new AccountCapabilityFlags();
		accountCapabilityFlags.setCanServiceIcOrders(account.getRoleId() == RoleId.MHIC && accountCapabilityTypeIds.contains(AccountCapabilityTypeId.MHIC_ORDER_SERVICER));
		accountCapabilityFlags.setCanEditIcTriages(account.getRoleId() == RoleId.MHIC); // All MHICs can do this
		accountCapabilityFlags.setCanEditIcSafetyPlanning(accountCapabilityTypeIds.contains(AccountCapabilityTypeId.MHIC_ADMIN)
				|| accountCapabilityTypeIds.contains(AccountCapabilityTypeId.MHIC_SAFETY_PLANNING_ADMIN));
		accountCapabilityFlags.setCanViewIcReports(accountCapabilityTypeIds.contains(AccountCapabilityTypeId.MHIC_ADMIN)
				|| accountCapabilityTypeIds.contains(AccountCapabilityTypeId.MHIC_REPORT_VIEWER));
		accountCapabilityFlags.setCanImportIcPatientOrders(accountCapabilityTypeIds.contains(AccountCapabilityTypeId.MHIC_ADMIN));
		accountCapabilityFlags.setCanAdministerIcDepartments(accountCapabilityTypeIds.contains(AccountCapabilityTypeId.MHIC_ADMIN)
				|| accountCapabilityTypeIds.contains(AccountCapabilityTypeId.MHIC_DEPARTMENT_ADMIN));
		accountCapabilityFlags.setCanAdministerContent(accountCapabilityTypeIds.contains(AccountCapabilityTypeId.CONTENT_ADMIN));
		accountCapabilityFlags.setCanAdministerGroupSessions(accountCapabilityTypeIds.contains(AccountCapabilityTypeId.GROUP_SESSION_ADMIN));
		accountCapabilityFlags.setCanViewAnalytics(accountCapabilityTypeIds.contains(AccountCapabilityTypeId.ANALYTICS_VIEWER));

		// You can view provider reports if you have "report admin" (all reports) or any individual report access
		accountCapabilityFlags.setCanViewProviderReports(
				accountCapabilityTypeIds.contains(AccountCapabilityTypeId.PROVIDER_REPORT_ADMIN)
						|| accountCapabilityTypeIds.contains(AccountCapabilityTypeId.PROVIDER_REPORT_UNUSED_AVAILABILITY_VIEWER)
						|| accountCapabilityTypeIds.contains(AccountCapabilityTypeId.PROVIDER_REPORT_APPOINTMENT_CANCELATIONS_VIEWER)
						|| accountCapabilityTypeIds.contains(AccountCapabilityTypeId.PROVIDER_REPORT_APPOINTMENTS_VIEWER)
						|| accountCapabilityTypeIds.contains(AccountCapabilityTypeId.PROVIDER_REPORT_APPOINTMENTS_EAP_VIEWER)
		);

		accountCapabilityFlags.setCanViewProviderReportUnusedAvailability(accountCapabilityTypeIds.contains(AccountCapabilityTypeId.PROVIDER_REPORT_ADMIN)
				|| accountCapabilityTypeIds.contains(AccountCapabilityTypeId.PROVIDER_REPORT_UNUSED_AVAILABILITY_VIEWER));
		accountCapabilityFlags.setCanViewProviderReportAppointmentCancelations(accountCapabilityTypeIds.contains(AccountCapabilityTypeId.PROVIDER_REPORT_ADMIN)
				|| accountCapabilityTypeIds.contains(AccountCapabilityTypeId.PROVIDER_REPORT_APPOINTMENT_CANCELATIONS_VIEWER));
		accountCapabilityFlags.setCanViewProviderReportAppointments(accountCapabilityTypeIds.contains(AccountCapabilityTypeId.PROVIDER_REPORT_ADMIN)
				|| accountCapabilityTypeIds.contains(AccountCapabilityTypeId.PROVIDER_REPORT_APPOINTMENTS_VIEWER));
		accountCapabilityFlags.setCanViewProviderReportAppointmentsEap(accountCapabilityTypeIds.contains(AccountCapabilityTypeId.PROVIDER_REPORT_ADMIN)
				|| accountCapabilityTypeIds.contains(AccountCapabilityTypeId.PROVIDER_REPORT_APPOINTMENTS_EAP_VIEWER));
		accountCapabilityFlags.setCanViewStudyInsights(accountCapabilityTypeIds.contains(AccountCapabilityTypeId.STUDY_ADMIN));
		accountCapabilityFlags.setCanManageCareResources(accountCapabilityTypeIds.contains(AccountCapabilityTypeId.MHIC_RESOURCE_MANAGER));
		accountCapabilityFlags.setCanCreatePages((accountCapabilityTypeIds.contains(AccountCapabilityTypeId.PAGE_CREATOR)));

		return accountCapabilityFlags;
	}

	@Nonnull
	public boolean canEditGroupSession(@Nonnull GroupSession groupSession,
																		 @Nonnull Account account) {
		requireNonNull(groupSession);
		requireNonNull(account);

		if (account.getRoleId() == RoleId.ADMINISTRATOR && groupSession.getInstitutionId() == account.getInstitutionId())
			return true;

		String accountEmailAddress = getNormalizer().normalizeEmailAddress(account.getEmailAddress()).orElse(null);
		String facilitatorEmailAddress = getNormalizer().normalizeEmailAddress(groupSession.getFacilitatorEmailAddress()).orElse(null);

		boolean submitter = Objects.equals(account.getAccountId(), groupSession.getSubmitterAccountId());
		boolean facilitator = Objects.equals(account.getAccountId(), groupSession.getFacilitatorAccountId())
				|| (account.getEmailAddress() != null && Objects.equals(accountEmailAddress, facilitatorEmailAddress));

		// Submitters and facilitators can only edit in NEW status
		if ((submitter || facilitator) && groupSession.getGroupSessionStatusId() == GroupSessionStatusId.NEW)
			return true;

		return false;
	}

	@Nonnull
	public boolean canEditGroupSessionStatus(@Nonnull GroupSession groupSession,
																					 @Nonnull Account account) {
		requireNonNull(groupSession);
		requireNonNull(account);

		if (account.getRoleId() == RoleId.ADMINISTRATOR && groupSession.getInstitutionId() == account.getInstitutionId())
			return true;

		return false;
	}

	@Nonnull
	public boolean canEditGroupSessionRequest(@Nonnull GroupSessionRequest groupSessionRequest,
																						@Nonnull Account account) {
		requireNonNull(groupSessionRequest);
		requireNonNull(account);

		if (account.getRoleId() == RoleId.ADMINISTRATOR && groupSessionRequest.getInstitutionId() == account.getInstitutionId())
			return true;

		return false;
	}

	@Nonnull
	public boolean canEditGroupSessionRequestStatus(@Nonnull GroupSessionRequest groupSessionRequest,
																									@Nonnull Account account) {
		requireNonNull(groupSessionRequest);
		requireNonNull(account);

		if (account.getRoleId() == RoleId.ADMINISTRATOR && groupSessionRequest.getInstitutionId() == account.getInstitutionId())
			return true;

		return false;
	}

	@Nonnull
	public boolean canViewProvider(@Nonnull Provider provider,
																 @Nonnull Account account) {
		requireNonNull(provider);
		requireNonNull(account);

		return provider.getInstitutionId() == account.getInstitutionId();
	}

	@Nonnull
	public Boolean canEditProvider(@Nonnull Provider provider,
																 @Nonnull Account account) {
		requireNonNull(provider);
		requireNonNull(account);

		if (account.getRoleId() == RoleId.ADMINISTRATOR && provider.getInstitutionId() == account.getInstitutionId())
			return true;

		return Objects.equals(account.getProviderId(), provider.getProviderId());
	}

	@Nonnull
	public boolean canViewProviderCalendar(@Nonnull Provider provider,
																				 @Nonnull Account account) {
		requireNonNull(provider);
		requireNonNull(account);

		if (account.getRoleId() == RoleId.ADMINISTRATOR && provider.getInstitutionId() == account.getInstitutionId())
			return true;

		if (Objects.equals(account.getProviderId(), provider.getProviderId()))
			return true;

		CalendarPermissionId calendarPermissionId = getAvailabilityService().findCalendarPermissionByAccountId(
				provider.getProviderId(), account.getAccountId()).orElse(null);

		return calendarPermissionId == CalendarPermissionId.MANAGER || calendarPermissionId == CalendarPermissionId.VIEWER;
	}

	@Nonnull
	public boolean canEditProviderCalendar(@Nonnull Provider provider,
																				 @Nonnull Account account) {
		requireNonNull(provider);
		requireNonNull(account);

		if (account.getRoleId() == RoleId.ADMINISTRATOR && provider.getInstitutionId() == account.getInstitutionId())
			return true;

		if (Objects.equals(account.getProviderId(), provider.getProviderId()))
			return true;

		CalendarPermissionId calendarPermissionId = getAvailabilityService().findCalendarPermissionByAccountId(
				provider.getProviderId(), account.getAccountId()).orElse(null);

		return calendarPermissionId == CalendarPermissionId.MANAGER;
	}

	@Nonnull
	public Boolean canCreateCourseSession(@Nonnull UUID courseId,
																				@Nonnull Account account) {
		requireNonNull(courseId);
		requireNonNull(account);

		// If this course is available for the account's institution, it's legal for the account to create a session for it
		Course course = getCourseService().findCourseById(courseId, account.getInstitutionId()).orElse(null);
		return course != null;
	}

	@Nonnull
	public Boolean canModifyCourseSession(@Nonnull CourseSession courseSession,
																				@Nonnull Account account) {
		requireNonNull(courseSession);
		requireNonNull(account);

		// You must be the owner of the session to modify it
		return account.getAccountId() != null
				&& courseSession.getAccountId() != null
				&& account.getAccountId().equals(courseSession.getAccountId());
	}

	@Nonnull
	public Boolean canCreateInteractionInstance(@Nonnull Interaction interaction,
																							@Nonnull Account account) {
		requireNonNull(interaction);
		requireNonNull(account);

		if (!Objects.equals(interaction.getInstitutionId(), account.getInstitutionId()))
			return false;

		return account.getStandardMetadata().getInteractionIds() != null
				&& account.getStandardMetadata().getInteractionIds().contains(interaction.getInteractionId());
	}

	@Nonnull
	public Boolean canViewInteractionInstance(@Nonnull InteractionInstance interactionInstance,
																						@Nonnull Account account) {
		requireNonNull(interactionInstance);
		requireNonNull(account);

		Interaction interaction = getInteractionService().findInteractionById(interactionInstance.getInteractionId()).orElse(null);

		if (interaction == null)
			return false;

		if (!Objects.equals(interaction.getInstitutionId(), account.getInstitutionId()))
			return false;

		return account.getStandardMetadata().getInteractionIds() != null
				&& account.getStandardMetadata().getInteractionIds().contains(interactionInstance.getInteractionId());
	}

	@Nonnull
	public Boolean canTakeActionOnInteractionInstance(@Nonnull InteractionInstance interactionInstance,
																										@Nonnull Account account) {
		requireNonNull(interactionInstance);
		requireNonNull(account);

		return canViewInteractionInstance(interactionInstance, account);
	}

	@Nonnull
	public Boolean canViewAppointmentType(@Nonnull AppointmentType appointmentType,
																				@Nonnull Account account) {
		requireNonNull(appointmentType);
		requireNonNull(account);

		Institution institution = getAppointmentService().findInstitutionForAppointmentTypeId(appointmentType.getAppointmentTypeId()).get();

		return Objects.equals(institution.getInstitutionId(), account.getInstitutionId());
	}

	@Nonnull
	public Boolean canViewAppointmentTypesForProvider(@Nonnull Provider provider,
																										@Nonnull Account account) {
		requireNonNull(provider);
		requireNonNull(account);

		return Objects.equals(provider.getInstitutionId(), account.getInstitutionId());
	}

	@Nonnull
	public Boolean canUpdateAppointmentType(@Nonnull AppointmentType appointmentType,
																					@Nonnull Account account) {
		requireNonNull(appointmentType);
		requireNonNull(account);

		return canDeleteAppointmentType(appointmentType, account);
	}

	@Nonnull
	public Boolean canDeleteAppointmentType(@Nonnull AppointmentType appointmentType,
																					@Nonnull Account account) {
		requireNonNull(appointmentType);
		requireNonNull(account);

		// You can only delete your own appointment types
		List<AppointmentType> appointmentTypes = getAppointmentService().findAppointmentTypesByProviderId(account.getProviderId());

		for (AppointmentType providerAppointmentType : appointmentTypes)
			if (Objects.equals(providerAppointmentType.getAppointmentTypeId(), appointmentType.getAppointmentTypeId()))
				return true;

		return false;
	}

	@Nonnull
	public Boolean canViewAppointment(@Nonnull Appointment appointment,
																		@Nonnull Account account) {
		requireNonNull(appointment);
		requireNonNull(account);

		if (Objects.equals(appointment.getAccountId(), account.getAccountId()))
			return true;

		if (Objects.equals(appointment.getProviderId(), account.getProviderId()))
			return true;

		// TODO: probably want more detailed rules here, like if we share calendars across MHICs
		return Objects.equals(appointment.getCreatedByAccountId(), account.getAccountId());
	}

	@Nonnull
	public Boolean canCancelAppointment(@Nonnull Appointment appointment,
																			@Nonnull Account account,
																			@Nonnull Account appointmentAccount) {
		requireNonNull(appointment);
		requireNonNull(account);
		requireNonNull(appointmentAccount);

		// Some users can cancel appointments on behalf of other users
		if (account.getRoleId() == RoleId.ADMINISTRATOR || account.getRoleId() == RoleId.MHIC) {
			// "Normal" admins or MHICs can cancel anything within the same institution
			if (account.getInstitutionId().equals(appointmentAccount.getInstitutionId()))
				return true;
		} else {
			// If the canceling account is the provider for the appointment, canceling is OK
			if (Objects.equals(account.getProviderId(), appointment.getProviderId()))
				return true;

			// You can cancel your own appointments
			if (appointmentAccount.getAccountId().equals(account.getAccountId()))
				return true;
		}

		return false;
	}

	@Nonnull
	public Boolean canUpdateAppointment(@Nonnull Account account,
																			@Nonnull Account appointmentAccount) {
		requireNonNull(account);
		requireNonNull(appointmentAccount);

		// Some users can update appointments on behalf of other users
		if (account.getRoleId() == RoleId.MHIC || account.getRoleId() == RoleId.ADMINISTRATOR || account.getRoleId() == RoleId.PROVIDER) {
			// "Normal" admins can update anything within the same institution
			// TODO: Should we include the PROVIDER role in this?
			if (!account.getInstitutionId().equals(appointmentAccount.getInstitutionId()))
				return false;
			else
				return true;
		} else {
			// If you are not a special role, you can only update for yourself
			if (appointmentAccount.getAccountId().equals(account.getAccountId()))
				return true;
		}

		return false;
	}

	@Nonnull
	public Boolean canViewScreeningSession(@Nonnull ScreeningSession screeningSession,
																				 @Nonnull Account viewingAccount,
																				 @Nonnull Account screenedAccount) {
		requireNonNull(screeningSession);
		requireNonNull(viewingAccount);
		requireNonNull(screenedAccount);

		// TODO: revisit later when we have more usage; we might open results up
		// to a provider who has been granted access to treat the patient, for example
		return canPerformScreening(viewingAccount, screenedAccount);
	}

	@Nonnull
	public Boolean canPerformScreening(@Nonnull Account performingAccount,
																		 @Nonnull Account targetAccount) {
		requireNonNull(performingAccount);
		requireNonNull(targetAccount);

		// You can always screen yourself
		if (Objects.equals(performingAccount.getAccountId(), targetAccount.getAccountId()))
			return true;

		// An admin or MHIC at the same institution is able to screen others at that institution
		if (Objects.equals(performingAccount.getInstitutionId(), targetAccount.getInstitutionId())
				&& (performingAccount.getRoleId() == RoleId.ADMINISTRATOR || performingAccount.getRoleId() == RoleId.MHIC))
			return true;

		return false;
	}

	@Nonnull
	public Boolean canViewScreeningFlow(@Nonnull Account account,
																			@Nonnull ScreeningFlow screeningFlow) {
		requireNonNull(account);
		requireNonNull(screeningFlow);

		return Objects.equals(account.getInstitutionId(), screeningFlow.getInstitutionId());
	}

	@Nonnull
	public Boolean canPerformScreening(@Nonnull Account performingAccount,
																		 @Nonnull RawPatientOrder patientOrder) {
		requireNonNull(performingAccount);
		requireNonNull(patientOrder);

		// You can always screen yourself
		if (Objects.equals(performingAccount.getAccountId(), patientOrder.getPatientAccountId()))
			return true;

		// An admin or MHIC at the same institution is able to screen others at that institution
		if (Objects.equals(performingAccount.getInstitutionId(), patientOrder.getInstitutionId())
				&& (performingAccount.getRoleId() == RoleId.ADMINISTRATOR || performingAccount.getRoleId() == RoleId.MHIC))
			return true;

		return false;
	}

	@Nonnull
	public Boolean canResetPatientOrder(@Nonnull Account performingAccount,
																			@Nonnull RawPatientOrder patientOrder) {
		requireNonNull(performingAccount);
		requireNonNull(patientOrder);

		// An admin or MHIC at the same institution is able to reset orders for others at that institution
		if (Objects.equals(performingAccount.getInstitutionId(), patientOrder.getInstitutionId())
				&& (performingAccount.getRoleId() == RoleId.ADMINISTRATOR || performingAccount.getRoleId() == RoleId.MHIC))
			return true;

		return false;
	}

	@Nonnull
	public Boolean canViewTopicCenter(@Nonnull TopicCenter topicCenter,
																		@Nonnull Account account) {
		requireNonNull(topicCenter);
		requireNonNull(account);

		InstitutionTopicCenter institutionTopicCenter = getTopicCenterService().findInstitutionTopicCenter(
				account.getInstitutionId(), topicCenter.getTopicCenterId()).orElse(null);

		return institutionTopicCenter != null;
	}

	@Nonnull
	public Boolean canViewPatientOrder(@Nonnull RawPatientOrder patientOrder,
																		 @Nonnull Account account) {
		requireNonNull(patientOrder);
		requireNonNull(account);

		// An admin or MHIC at the same institution is able to view patient orders at that institution
		if (Objects.equals(account.getInstitutionId(), patientOrder.getInstitutionId())
				&& (account.getRoleId() == RoleId.ADMINISTRATOR || account.getRoleId() == RoleId.MHIC))
			return true;

		// You can view your own order
		if (Objects.equals(account.getAccountId(), patientOrder.getPatientAccountId()))
			return true;

		return false;
	}

	@Nonnull
	public Boolean canViewPatientOrderTriages(@Nonnull RawPatientOrder patientOrder,
																						@Nonnull Account account) {
		requireNonNull(patientOrder);
		requireNonNull(account);

		// An admin or MHIC at the same institution is able to view patient orders at that institution
		if (Objects.equals(account.getInstitutionId(), patientOrder.getInstitutionId())
				&& (account.getRoleId() == RoleId.ADMINISTRATOR || account.getRoleId() == RoleId.MHIC))
			return true;

		return false;
	}

	@Nonnull
	public Boolean canUpdatePatientOrderTriages(@Nonnull RawPatientOrder patientOrder,
																							@Nonnull Account account) {
		requireNonNull(patientOrder);
		requireNonNull(account);

		return canViewPatientOrderTriages(patientOrder, account);
	}

	@Nonnull
	public Boolean canUpdatePatientOrderEncounterCsn(@Nonnull RawPatientOrder patientOrder,
																									 @Nonnull Account account) {
		requireNonNull(patientOrder);
		requireNonNull(account);

		return canUpdatePatientOrderTriages(patientOrder, account);
	}

	@Nonnull
	public Boolean canViewPanelAccounts(@Nonnull InstitutionId institutionId,
																			@Nonnull Account account) {
		requireNonNull(institutionId);
		requireNonNull(account);

		// Re-use existing logic
		return canImportPatientOrders(institutionId, account);
	}

	@Nonnull
	public Boolean canAdministerIcDepartments(@Nonnull InstitutionId institutionId,
																						@Nonnull Account account) {
		requireNonNull(institutionId);
		requireNonNull(account);

		if (!institutionId.equals(account.getInstitutionId()))
			return false;

		AccountCapabilityFlags accountCapabilityFlags = determineAccountCapabilityFlagsForAccount(account);
		return accountCapabilityFlags.isCanAdministerIcDepartments();
	}

	@Nonnull
	public Boolean canViewPatientOrders(@Nonnull InstitutionId institutionId,
																			@Nonnull Account account) {
		requireNonNull(institutionId);
		requireNonNull(account);

		// Re-use existing logic
		return canImportPatientOrders(institutionId, account);
	}

	@Nonnull
	public Boolean canViewPatientOrdersForPanelAccount(@Nonnull Account viewingAccount,
																										 @Nonnull Account panelAccount) {
		requireNonNull(viewingAccount);
		requireNonNull(panelAccount);

		// Both viewing and panel "owner" accounts must be of correct role and in the same institution
		if ((viewingAccount.getRoleId() == RoleId.ADMINISTRATOR || viewingAccount.getRoleId() == RoleId.MHIC)
				&& (panelAccount.getRoleId() == RoleId.ADMINISTRATOR || panelAccount.getRoleId() == RoleId.MHIC)
				&& Objects.equals(viewingAccount.getInstitutionId(), panelAccount.getInstitutionId()))
			return true;

		return false;
	}

	@Nonnull
	public Boolean canImportPatientOrders(@Nonnull InstitutionId institutionId,
																				@Nonnull Account account) {
		requireNonNull(institutionId);
		requireNonNull(account);

		// An admin or MHIC at the same institution is able to view patient orders at that institution
		if (Objects.equals(account.getInstitutionId(), institutionId)
				&& (account.getRoleId() == RoleId.ADMINISTRATOR || account.getRoleId() == RoleId.MHIC))
			return true;

		return false;
	}

	@Nonnull
	public Boolean canViewPatientOrderClinicalReport(@Nonnull PatientOrder patientOrder,
																									 @Nonnull Account account) {
		requireNonNull(patientOrder);
		requireNonNull(account);

		// An admin or MHIC at the same institution is able to view clinical reports at that institution.
		if (Objects.equals(account.getInstitutionId(), patientOrder.getInstitutionId())
				&& (account.getRoleId() == RoleId.ADMINISTRATOR || account.getRoleId() == RoleId.MHIC))
			return true;

		return false;
	}

	@Nonnull
	public Boolean canEditPatientOrder(@Nonnull RawPatientOrder patientOrder,
																		 @Nonnull Account account) {
		requireNonNull(patientOrder);
		requireNonNull(account);

		// An admin or MHIC at the same institution is able to edit patient orders at that institution.
		if (Objects.equals(account.getInstitutionId(), patientOrder.getInstitutionId())
				&& (account.getRoleId() == RoleId.ADMINISTRATOR || account.getRoleId() == RoleId.MHIC))
			return true;

		// You can also edit your own, to an extent, if you are the patient who's tied to it...
		if (Objects.equals(patientOrder.getPatientAccountId(), account.getAccountId()))
			return true;

		return false;
	}

	@Nonnull
	public Boolean canEditAccount(@Nonnull Account accountToEdit,
																@Nonnull Account accountMakingEdit) {
		requireNonNull(accountToEdit);
		requireNonNull(accountMakingEdit);

		// An account can edit itself
		// TODO: additional restrictions here?  e.g. for an integrated care institution,
		//  patient cannot edit herself after an associated order is in a particular state?
		if (Objects.equals(accountMakingEdit.getAccountId(), accountToEdit.getAccountId()))
			return true;

		// An admin or MHIC at the same institution is able to edit patients at that institution
		if (Objects.equals(accountMakingEdit.getInstitutionId(), accountToEdit.getInstitutionId())
				&& (accountMakingEdit.getRoleId() == RoleId.ADMINISTRATOR || accountMakingEdit.getRoleId() == RoleId.MHIC))
			return true;

		return false;
	}

	@Nonnull
	public Boolean canViewVideo(@Nonnull Video video,
															@Nonnull InstitutionId institutionId) {
		requireNonNull(video);
		requireNonNull(institutionId);

		// By default, you can see any video in the institution
		if (Objects.equals(video.getInstitutionId(), institutionId))
			return true;

		return false;
	}

	@Nonnull
	public Boolean canViewVideo(@Nonnull Video video,
															@Nonnull Account account) {
		requireNonNull(video);
		requireNonNull(account);

		// By default, you can see any video in your institution
		if (Objects.equals(video.getInstitutionId(), account.getInstitutionId()))
			return true;

		return false;
	}

	@Nonnull
	public Boolean canViewAnalytics(@Nonnull InstitutionId institutionId,
																	@Nonnull Account account) {
		requireNonNull(institutionId);
		requireNonNull(account);

		if (!account.getInstitutionId().equals(institutionId))
			return false;

		return account.getAccountCapabilityTypeIds().contains(AccountCapabilityTypeId.ANALYTICS_VIEWER);
	}

	@Nonnull
	public Boolean canManageCareResources(@Nonnull InstitutionId institutionId,
																				@Nonnull Account account) {
		requireNonNull(institutionId);
		requireNonNull(account);

		// An admin or a user with MHIC_RESOURCE_MANAGER capability can create and update resources
		if (Objects.equals(account.getInstitutionId(), institutionId)
				&& (account.getRoleId() == RoleId.ADMINISTRATOR ||
				account.getAccountCapabilityTypeIds().contains(AccountCapabilityTypeId.MHIC_RESOURCE_MANAGER)))
			return true;

		return false;
	}

	@Nonnull
	public Boolean canManagePages(@Nonnull InstitutionId institutionId,
																@Nonnull Account account) {
		requireNonNull(institutionId);
		requireNonNull(account);

		// An admin or a user with PAGE_CREATOR capability can create and update pages
		if (Objects.equals(account.getInstitutionId(), institutionId)
				&& (account.getRoleId() == RoleId.ADMINISTRATOR ||
				account.getAccountCapabilityTypeIds().contains(AccountCapabilityTypeId.PAGE_CREATOR)))
			return true;

		return false;
	}

	@Nonnull
	protected GroupSessionService getGroupSessionService() {
		return this.groupSessionServiceProvider.get();
	}

	@Nonnull
	protected InteractionService getInteractionService() {
		return this.interactionServiceProvider.get();
	}

	@Nonnull
	protected AvailabilityService getAvailabilityService() {
		return this.availabilityServiceProvider.get();
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return this.appointmentServiceProvider.get();
	}

	@Nonnull
	protected TopicCenterService getTopicCenterService() {
		return this.topicCenterServiceProvider.get();
	}

	@Nonnull
	protected ReportingService getReportingService() {
		return this.reportingServiceProvider.get();
	}

	@Nonnull
	protected PatientOrderService getPatientOrderService() {
		return this.patientOrderServiceProvider.get();
	}

	@Nonnull
	protected StudyService getStudyService() {
		return this.studyServiceProvider.get();
	}

	@Nonnull
	protected CourseService getCourseService() {
		return this.courseServiceProvider.get();
	}

	@Nonnull
	protected Normalizer getNormalizer() {
		return this.normalizer;
	}
}
