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
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.CalendarPermission.CalendarPermissionId;
import com.cobaltplatform.api.model.db.GroupSession;
import com.cobaltplatform.api.model.db.GroupSessionRequest;
import com.cobaltplatform.api.model.db.GroupSessionRequestStatus.GroupSessionRequestStatusId;
import com.cobaltplatform.api.model.db.GroupSessionStatus.GroupSessionStatusId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Interaction;
import com.cobaltplatform.api.model.db.InteractionInstance;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.security.AccountCapabilities;
import com.cobaltplatform.api.util.Normalizer;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
	private final Normalizer normalizer;

	@Inject
	public AuthorizationService(@Nonnull javax.inject.Provider<AvailabilityService> availabilityServiceProvider,
															@Nonnull javax.inject.Provider<GroupSessionService> groupSessionServiceProvider,
															@Nonnull javax.inject.Provider<InteractionService> interactionServiceProvider,
															@Nonnull javax.inject.Provider<AppointmentService> appointmentServiceProvider,
															@Nonnull Normalizer normalizer) {
		requireNonNull(availabilityServiceProvider);
		requireNonNull(groupSessionServiceProvider);
		requireNonNull(interactionServiceProvider);
		requireNonNull(appointmentServiceProvider);
		requireNonNull(normalizer);

		this.availabilityServiceProvider = availabilityServiceProvider;
		this.groupSessionServiceProvider = groupSessionServiceProvider;
		this.interactionServiceProvider = interactionServiceProvider;
		this.appointmentServiceProvider = appointmentServiceProvider;
		this.normalizer = normalizer;
	}

	@Nonnull
	public AccountCapabilities determineAccountCapabilities(@Nonnull Account account,
																													@Nonnull InstitutionId institutionId) {
		requireNonNull(account);
		requireNonNull(institutionId);

		AccountCapabilities accountCapabilities = new AccountCapabilities();

		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR) {
			accountCapabilities.setViewNavAdminGroupSession(true);
			accountCapabilities.setViewNavAdminGroupSessionRequest(true);
			accountCapabilities.setViewNavAdminMyContent(true);
			accountCapabilities.setViewNavAdminAvailableContent(true);
			accountCapabilities.setViewNavAdminCalendar(true);
		} else if (account.getRoleId() == RoleId.ADMINISTRATOR && account.getInstitutionId() == institutionId) {
			accountCapabilities.setViewNavAdminGroupSession(true);
			accountCapabilities.setViewNavAdminGroupSessionRequest(true);
			accountCapabilities.setViewNavAdminMyContent(true);
			accountCapabilities.setViewNavAdminAvailableContent(true);
			accountCapabilities.setViewNavAdminCalendar(true);
		} else if (account.getInstitutionId() == institutionId) {
			accountCapabilities.setViewNavAdminGroupSession(getGroupSessionService().canTakeActionOnGroupSessions(account, institutionId));
			accountCapabilities.setViewNavAdminGroupSessionRequest(getGroupSessionService().canTakeActionOnGroupSessionRequests(account, institutionId));
			accountCapabilities.setViewNavAdminCalendar(getAvailabilityService().canTakeActionOnCalendars(account, institutionId));
		}

		return accountCapabilities;
	}

	@Nonnull
	public Map<InstitutionId, AccountCapabilities> determineAccountCapabilitiesByInstitutionId(@Nonnull Account account) {
		requireNonNull(account);

		Map<InstitutionId, AccountCapabilities> accountCapabilitiesByInstitutionId = new HashMap<>(InstitutionId.values().length);

		for (InstitutionId institutionId : InstitutionId.values())
			accountCapabilitiesByInstitutionId.put(institutionId, determineAccountCapabilities(account, institutionId));

		return accountCapabilitiesByInstitutionId;
	}

	@Nonnull
	public boolean canEditGroupSession(@Nonnull GroupSession groupSession,
																		 @Nonnull Account account) {
		requireNonNull(groupSession);
		requireNonNull(account);

		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR)
			return true;

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

		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR)
			return true;

		if (account.getRoleId() == RoleId.ADMINISTRATOR && groupSession.getInstitutionId() == account.getInstitutionId())
			return true;

		return false;
	}

	@Nonnull
	public boolean canEditGroupSessionRequest(@Nonnull GroupSessionRequest groupSessionRequest,
																						@Nonnull Account account) {
		requireNonNull(groupSessionRequest);
		requireNonNull(account);

		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR)
			return true;

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

		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR)
			return true;

		if (account.getRoleId() == RoleId.ADMINISTRATOR && groupSessionRequest.getInstitutionId() == account.getInstitutionId())
			return true;

		return false;
	}

	@Nonnull
	public boolean canViewProvider(@Nonnull Provider provider,
																 @Nonnull Account account) {
		requireNonNull(provider);
		requireNonNull(account);

		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR)
			return true;

		return provider.getInstitutionId() == account.getInstitutionId();
	}

	@Nonnull
	public Boolean canEditProvider(@Nonnull Provider provider,
																 @Nonnull Account account) {
		requireNonNull(provider);
		requireNonNull(account);

		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR)
			return true;

		if (account.getRoleId() == RoleId.ADMINISTRATOR && provider.getInstitutionId() == account.getInstitutionId())
			return true;

		return Objects.equals(account.getProviderId(), provider.getProviderId());
	}

	@Nonnull
	public boolean canViewProviderCalendar(@Nonnull Provider provider,
																				 @Nonnull Account account) {
		requireNonNull(provider);
		requireNonNull(account);

		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR)
			return true;

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

		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR)
			return true;

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

		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR)
			return true;

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

		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR)
			return true;

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

		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR)
			return true;

		Institution institution = getAppointmentService().findInstitutionForAppointmentTypeId(appointmentType.getAppointmentTypeId()).get();

		return Objects.equals(institution.getInstitutionId(), account.getInstitutionId());
	}

	@Nonnull
	public Boolean canViewAppointmentTypesForProvider(@Nonnull Provider provider,
																										@Nonnull Account account) {
		requireNonNull(provider);
		requireNonNull(account);

		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR)
			return true;

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

		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR)
			return true;

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

		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR)
			return true;

		if (Objects.equals(appointment.getAccountId(), account.getAccountId()))
			return true;

		if (Objects.equals(appointment.getProviderId(), account.getProviderId()))
			return true;

		// TODO: probably want more detailed rules here, like if we share calendars across MHICs
		return Objects.equals(appointment.getCreatedByAccountId(), account.getAccountId());
	}

	@Nonnull
	public boolean canUpdateAppointment(@Nonnull Account account,
																			@Nonnull Account appointmentAccount) {
		requireNonNull(account);
		requireNonNull(appointmentAccount);

		// Some users can update appointments on behalf of other users
		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR) {
			return true;
		} else if (account.getRoleId() == RoleId.MHIC || account.getRoleId() == RoleId.ADMINISTRATOR || account.getRoleId() == RoleId.PROVIDER) {
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
	protected GroupSessionService getGroupSessionService() {
		return groupSessionServiceProvider.get();
	}

	@Nonnull
	protected InteractionService getInteractionService() {
		return interactionServiceProvider.get();
	}

	@Nonnull
	protected AvailabilityService getAvailabilityService() {
		return availabilityServiceProvider.get();
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return appointmentServiceProvider.get();
	}

	@Nonnull
	protected Normalizer getNormalizer() {
		return normalizer;
	}
}
