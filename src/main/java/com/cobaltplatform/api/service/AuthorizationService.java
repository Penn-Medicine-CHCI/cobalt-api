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
import com.cobaltplatform.api.model.db.CalendarPermission.CalendarPermissionId;
import com.cobaltplatform.api.model.db.GroupSession;
import com.cobaltplatform.api.model.db.GroupSessionRequest;
import com.cobaltplatform.api.model.db.GroupSessionRequestStatus.GroupSessionRequestStatusId;
import com.cobaltplatform.api.model.db.GroupSessionStatus.GroupSessionStatusId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.security.AccountCapabilities;
import com.cobaltplatform.api.util.Normalizer;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import java.util.HashMap;
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
	private final Normalizer normalizer;

	@Inject
	public AuthorizationService(@Nonnull javax.inject.Provider<AvailabilityService> availabilityServiceProvider,
															@Nonnull javax.inject.Provider<GroupSessionService> groupSessionServiceProvider,
															@Nonnull Normalizer normalizer) {
		requireNonNull(availabilityServiceProvider);
		requireNonNull(groupSessionServiceProvider);
		requireNonNull(normalizer);

		this.availabilityServiceProvider = availabilityServiceProvider;
		this.groupSessionServiceProvider = groupSessionServiceProvider;
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
	public boolean canEditProvider(@Nonnull Provider provider,
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
	protected AvailabilityService getAvailabilityService() {
		return availabilityServiceProvider.get();
	}

	@Nonnull
	protected GroupSessionService getGroupSessionService() {
		return groupSessionServiceProvider.get();
	}

	@Nonnull
	protected Normalizer getNormalizer() {
		return normalizer;
	}
}
