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
import com.cobaltplatform.api.model.db.GroupSession;
import com.cobaltplatform.api.model.db.GroupSessionRequest;
import com.cobaltplatform.api.model.db.GroupSessionRequestStatus.GroupSessionRequestStatusId;
import com.cobaltplatform.api.model.db.GroupSessionStatus.GroupSessionStatusId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionTopicCenter;
import com.cobaltplatform.api.model.db.Interaction;
import com.cobaltplatform.api.model.db.InteractionInstance;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.ReportType.ReportTypeId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.db.TopicCenter;
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
	private final Normalizer normalizer;

	@Inject
	public AuthorizationService(@Nonnull javax.inject.Provider<AvailabilityService> availabilityServiceProvider,
															@Nonnull javax.inject.Provider<GroupSessionService> groupSessionServiceProvider,
															@Nonnull javax.inject.Provider<InteractionService> interactionServiceProvider,
															@Nonnull javax.inject.Provider<AppointmentService> appointmentServiceProvider,
															@Nonnull javax.inject.Provider<TopicCenterService> topicCenterServiceProvider,
															@Nonnull javax.inject.Provider<ReportingService> reportingServiceProvider,
															@Nonnull javax.inject.Provider<PatientOrderService> patientOrderServiceProvider,
															@Nonnull Normalizer normalizer) {
		requireNonNull(availabilityServiceProvider);
		requireNonNull(groupSessionServiceProvider);
		requireNonNull(interactionServiceProvider);
		requireNonNull(appointmentServiceProvider);
		requireNonNull(topicCenterServiceProvider);
		requireNonNull(reportingServiceProvider);
		requireNonNull(patientOrderServiceProvider);
		requireNonNull(normalizer);

		this.availabilityServiceProvider = availabilityServiceProvider;
		this.groupSessionServiceProvider = groupSessionServiceProvider;
		this.interactionServiceProvider = interactionServiceProvider;
		this.appointmentServiceProvider = appointmentServiceProvider;
		this.topicCenterServiceProvider = topicCenterServiceProvider;
		this.reportingServiceProvider = reportingServiceProvider;
		this.patientOrderServiceProvider = patientOrderServiceProvider;
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
		accountCapabilityFlags.setCanEditIcTriages(accountCapabilityTypeIds.contains(AccountCapabilityTypeId.MHIC_ADMIN));
		accountCapabilityFlags.setCanEditIcSafetyPlanning(accountCapabilityTypeIds.contains(AccountCapabilityTypeId.MHIC_ADMIN)
				|| accountCapabilityTypeIds.contains(AccountCapabilityTypeId.MHIC_SAFETY_PLANNING_ADMIN));
		accountCapabilityFlags.setCanViewIcReports(accountCapabilityTypeIds.contains(AccountCapabilityTypeId.MHIC_ADMIN));
		accountCapabilityFlags.setCanImportIcPatientOrders(accountCapabilityTypeIds.contains(AccountCapabilityTypeId.MHIC_ADMIN));

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

		String accountEmailAddress = getNormalizer().normalizeEmailAddress(account.getEmailAddress()).orElse(null);
		String facilitatorEmailAddress = getNormalizer().normalizeEmailAddress(groupSessionRequest.getFacilitatorEmailAddress()).orElse(null);

		boolean facilitator = Objects.equals(account.getAccountId(), groupSessionRequest.getFacilitatorAccountId())
				|| (account.getEmailAddress() != null && Objects.equals(accountEmailAddress, facilitatorEmailAddress));
		boolean submitter = Objects.equals(account.getAccountId(), groupSessionRequest.getSubmitterAccountId());

		// Submitters and facilitators can only edit in NEW status
		if ((submitter || facilitator) && groupSessionRequest.getGroupSessionRequestStatusId() == GroupSessionRequestStatusId.NEW)
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
	public Boolean canPerformScreening(@Nonnull Account performingAccount,
																		 @Nonnull PatientOrder patientOrder) {
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
	public Boolean canViewTopicCenter(@Nonnull TopicCenter topicCenter,
																		@Nonnull Account account) {
		requireNonNull(topicCenter);
		requireNonNull(account);

		InstitutionTopicCenter institutionTopicCenter = getTopicCenterService().findInstitutionTopicCenter(
				account.getInstitutionId(), topicCenter.getTopicCenterId()).orElse(null);

		return institutionTopicCenter != null;
	}

	@Nonnull
	public Boolean canViewPatientOrder(@Nonnull PatientOrder patientOrder,
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
	public Boolean canViewPatientOrderTriages(@Nonnull PatientOrder patientOrder,
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
	public Boolean canUpdatePatientOrderTriages(@Nonnull PatientOrder patientOrder,
																							@Nonnull Account account) {
		requireNonNull(patientOrder);
		requireNonNull(account);

		return canViewPatientOrderTriages(patientOrder, account);
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
	public Boolean canEditPatientOrder(@Nonnull PatientOrder patientOrder,
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
	protected Normalizer getNormalizer() {
		return this.normalizer;
	}
}
