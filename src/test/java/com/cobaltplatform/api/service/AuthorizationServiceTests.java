/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
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
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.service.AccountCapabilityFlags;
import com.cobaltplatform.api.util.Normalizer;
import com.google.gson.Gson;
import org.junit.Test;

import javax.inject.Provider;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuthorizationServiceTests {
	@Test
	public void careNavigatorFlagSupportsAdministratorAndProviderAccountsWithNavigatorCapability() {
		AuthorizationService authorizationService = authorizationService();
		Account administratorNavigator = account(RoleId.ADMINISTRATOR, "[\"NAVIGATOR\"]");
		Account providerNavigator = account(RoleId.PROVIDER, "[\"NAVIGATOR\"]");
		Account providerWithoutCapability = account(RoleId.PROVIDER, null);
		Account patientWithCapability = account(RoleId.PATIENT, "[\"NAVIGATOR\"]");

		assertTrue(authorizationService.determineAccountCapabilityFlagsForAccount(administratorNavigator).isCareNavigator());
		assertTrue(authorizationService.determineAccountCapabilityFlagsForAccount(providerNavigator).isCareNavigator());
		assertFalse(authorizationService.determineAccountCapabilityFlagsForAccount(providerWithoutCapability).isCareNavigator());
		assertFalse(authorizationService.determineAccountCapabilityFlagsForAccount(patientWithCapability).isCareNavigator());
	}

	@Test
	public void careNavigatorFlagUsesPublicResponseFieldName() {
		AccountCapabilityFlags flags = new AccountCapabilityFlags();
		flags.setCareNavigator(true);

		assertTrue(new Gson().toJson(flags).contains("\"isCareNavigator\":true"));
	}

	@Test
	public void careEncounterManagementRequiresNavigatorRoleButNotProviderIdentity() {
		AuthorizationService authorizationService = authorizationService(true);
		Account administratorNavigator = account(RoleId.ADMINISTRATOR, "[\"NAVIGATOR\"]");
		Account administratorWithoutCapability = account(RoleId.ADMINISTRATOR, null);

		assertTrue(authorizationService.canManageCareEncounters(administratorNavigator));
		assertFalse(authorizationService.canManageCareEncounters(administratorWithoutCapability));
	}

	@Test
	public void careEncounterManagementRequiresOrganizationBookingProvider() {
		Account administratorNavigator = account(RoleId.ADMINISTRATOR, "[\"NAVIGATOR\"]");

		assertTrue(authorizationService(true).canManageCareEncounters(administratorNavigator));
		assertFalse(authorizationService(false).canManageCareEncounters(administratorNavigator));
	}

	@Test
	public void mappedProviderNavigatorCanCancelAssociatedProviderAppointment() {
		UUID navigatorAccountId = UUID.randomUUID();
		UUID patientAccountId = UUID.randomUUID();
		UUID appointmentProviderId = UUID.randomUUID();
		Account navigator = account(RoleId.PROVIDER, "[\"NAVIGATOR\"]");
		navigator.setAccountId(navigatorAccountId);
		navigator.setProviderId(UUID.randomUUID());
		Account patient = account(RoleId.PATIENT, null);
		patient.setAccountId(patientAccountId);
		Appointment appointment = new Appointment();
		appointment.setAccountId(patientAccountId);
		appointment.setProviderId(appointmentProviderId);

		assertTrue(authorizationService(true, true).canCancelAppointment(appointment, navigator, patient));
		assertFalse(authorizationService(true, false).canCancelAppointment(appointment, navigator, patient));
	}

	protected Account account(RoleId roleId, String accountCapabilityTypeIdsAsString) {
		Account account = new Account();
		account.setRoleId(roleId);
		account.setInstitutionId(InstitutionId.COBALT);
		account.setAccountCapabilityTypeIdsAsString(accountCapabilityTypeIdsAsString);
		return account;
	}

	protected AuthorizationService authorizationService() {
		return authorizationService(true);
	}

	protected AuthorizationService authorizationService(boolean institutionHasCareNavigatorBookingProvider) {
		return authorizationService(institutionHasCareNavigatorBookingProvider, false);
	}

	protected AuthorizationService authorizationService(boolean institutionHasCareNavigatorBookingProvider,
																					 boolean mappedToProvider) {
		return new AuthorizationService(unavailable(), unavailable(), unavailable(), unavailable(), unavailable(), unavailable(),
				unavailable(), unavailable(), unavailable(), unavailable(), new Normalizer()) {
			@Override
			protected boolean institutionHasCareNavigatorBookingProvider(InstitutionId institutionId) {
				return institutionHasCareNavigatorBookingProvider;
			}

			@Override
			protected boolean isCareNavigatorAccountMappedToProvider(UUID accountId, UUID providerId) {
				return mappedToProvider;
			}
		};
	}

	protected <T> Provider<T> unavailable() {
		return () -> null;
	}
}
