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

import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.model.api.request.FindAppointmentBookingRequirementsRequest;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentSelectionTypeId;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.service.AppointmentBookingRequirements;
import com.cobaltplatform.api.model.service.AppointmentBookingRequirements.AppointmentBookingRequirementsDestinationId;
import com.cobaltplatform.api.model.service.AppointmentBookingScreeningKey;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.pyranid.Database;
import org.junit.Test;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AppointmentServiceTests {
	@Test
	public void appointmentBookingRequirementsSatisfiedWhenAppointmentTypeHasNoScreeningFlow() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			ProviderAppointmentTypePair pair = findProviderAppointmentTypePair(database);

			database.execute("UPDATE appointment_type SET screening_flow_id=NULL WHERE appointment_type_id=?", pair.getAppointmentTypeId());

			AppointmentBookingRequirements appointmentBookingRequirements =
					appointmentService.findAppointmentBookingRequirements(requestFor(account, pair), account);

			assertEquals(AppointmentBookingRequirementsDestinationId.APPOINTMENT_BOOKING,
					appointmentBookingRequirements.getAppointmentBookingRequirementsDestinationId());
			assertEquals(false, appointmentBookingRequirements.getScreeningRequired());
			assertEquals(true, appointmentBookingRequirements.getScreeningSatisfied());
			assertNull(appointmentBookingRequirements.getScreeningFlowId());
			assertNull(appointmentBookingRequirements.getScreeningSession());
		});
	}

	@Test
	public void appointmentBookingRequirementsCreateResumeAndSatisfyScreeningSession() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Institution institution = institutionService.findInstitutionById(InstitutionId.COBALT).get();
			ProviderAppointmentTypePair pair = findProviderAppointmentTypePair(database);
			UUID screeningFlowId = institution.getFeatureScreeningFlowId();

			database.execute("UPDATE appointment_type SET screening_flow_id=? WHERE appointment_type_id=?", screeningFlowId, pair.getAppointmentTypeId());

			AppointmentBookingRequirements appointmentBookingRequirements =
					appointmentService.findAppointmentBookingRequirements(requestFor(account, pair), account);

			assertEquals(AppointmentBookingRequirementsDestinationId.SCREENING_SESSION,
					appointmentBookingRequirements.getAppointmentBookingRequirementsDestinationId());
			assertEquals(true, appointmentBookingRequirements.getScreeningRequired());
			assertEquals(false, appointmentBookingRequirements.getScreeningSatisfied());
			assertEquals(screeningFlowId, appointmentBookingRequirements.getScreeningFlowId());
			assertNotNull(appointmentBookingRequirements.getScreeningSession());
			assertEquals(pair.getProviderId().toString(), appointmentBookingRequirements.getContext().get("providerId"));
			assertEquals(pair.getAppointmentTypeId().toString(), appointmentBookingRequirements.getContext().get("appointmentTypeId"));

			UUID screeningSessionId = appointmentBookingRequirements.getScreeningSession().getScreeningSessionId();
			AppointmentBookingRequirements resumedAppointmentBookingRequirements =
					appointmentService.findAppointmentBookingRequirements(requestFor(account, pair), account);

			assertEquals(screeningSessionId, resumedAppointmentBookingRequirements.getScreeningSession().getScreeningSessionId());

			AppointmentBookingScreeningKey expectedScreeningKey =
					new AppointmentBookingScreeningKey(pair.getProviderId(), pair.getAppointmentTypeId(), screeningFlowId);
			assertTrue(appointmentService.findCompletedAppointmentBookingScreeningKeys(account.getAccountId(),
					Set.of(expectedScreeningKey)).isEmpty());

			database.execute("""
					UPDATE screening_session
					SET completed=TRUE,
					completed_at=NOW()
					WHERE screening_session_id=?
					""", screeningSessionId);

			AppointmentBookingRequirements satisfiedAppointmentBookingRequirements =
					appointmentService.findAppointmentBookingRequirements(requestFor(account, pair), account);

			assertEquals(AppointmentBookingRequirementsDestinationId.APPOINTMENT_BOOKING,
					satisfiedAppointmentBookingRequirements.getAppointmentBookingRequirementsDestinationId());
			assertEquals(true, satisfiedAppointmentBookingRequirements.getScreeningRequired());
			assertEquals(true, satisfiedAppointmentBookingRequirements.getScreeningSatisfied());
			assertNull(satisfiedAppointmentBookingRequirements.getScreeningSession());

			AppointmentBookingScreeningKey otherProviderScreeningKey =
					new AppointmentBookingScreeningKey(UUID.randomUUID(), pair.getAppointmentTypeId(), screeningFlowId);
			AppointmentBookingScreeningKey sameProviderOtherAppointmentTypeScreeningKey =
					new AppointmentBookingScreeningKey(pair.getProviderId(), UUID.randomUUID(), screeningFlowId);
			AppointmentBookingScreeningKey otherScreeningFlowKey =
					new AppointmentBookingScreeningKey(pair.getProviderId(), pair.getAppointmentTypeId(), UUID.randomUUID());
			Set<AppointmentBookingScreeningKey> completedScreeningKeys =
					appointmentService.findCompletedAppointmentBookingScreeningKeys(account.getAccountId(), Set.of(
							expectedScreeningKey, otherProviderScreeningKey, sameProviderOtherAppointmentTypeScreeningKey,
							otherScreeningFlowKey));

			assertTrue(completedScreeningKeys.contains(expectedScreeningKey));
			assertFalse(completedScreeningKeys.contains(otherProviderScreeningKey));
			assertTrue(completedScreeningKeys.contains(sameProviderOtherAppointmentTypeScreeningKey));
			assertFalse(completedScreeningKeys.contains(otherScreeningFlowKey));
		});
	}

	@Test
	public void appointmentBookingRequirementsCompletedScreeningSatisfiesSameProviderAndFlowAcrossAppointmentTypes() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Institution institution = institutionService.findInstitutionById(InstitutionId.COBALT).get();
			ProviderAppointmentTypePair pair = findProviderAppointmentTypePairWithOtherAppointmentType(database);
			ProviderAppointmentTypePair otherPair = pairForOtherAppointmentType(pair);
			UUID screeningFlowId = institution.getFeatureScreeningFlowId();

			database.execute("UPDATE appointment_type SET screening_flow_id=? WHERE appointment_type_id IN (?, ?)",
					screeningFlowId, pair.getAppointmentTypeId(), otherPair.getAppointmentTypeId());

			AppointmentBookingRequirements appointmentBookingRequirements =
					appointmentService.findAppointmentBookingRequirements(requestFor(account, pair), account);
			UUID screeningSessionId = appointmentBookingRequirements.getScreeningSession().getScreeningSessionId();

			database.execute("""
					UPDATE screening_session
					SET completed=TRUE,
					completed_at=NOW()
					WHERE screening_session_id=?
					""", screeningSessionId);

			AppointmentBookingRequirements otherAppointmentTypeRequirements =
					appointmentService.findAppointmentBookingRequirements(requestFor(account, otherPair), account);

			assertEquals(AppointmentBookingRequirementsDestinationId.APPOINTMENT_BOOKING,
					otherAppointmentTypeRequirements.getAppointmentBookingRequirementsDestinationId());
			assertEquals(true, otherAppointmentTypeRequirements.getScreeningRequired());
			assertEquals(true, otherAppointmentTypeRequirements.getScreeningSatisfied());
			assertNull(otherAppointmentTypeRequirements.getScreeningSession());
		});
	}

	@Test
	public void appointmentBookingRequirementsIncompleteScreeningResumeRemainsAppointmentTypeSpecific() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);
			Institution institution = institutionService.findInstitutionById(InstitutionId.COBALT).get();
			ProviderAppointmentTypePair pair = findProviderAppointmentTypePairWithOtherAppointmentType(database);
			ProviderAppointmentTypePair otherPair = pairForOtherAppointmentType(pair);
			UUID screeningFlowId = institution.getFeatureScreeningFlowId();

			database.execute("UPDATE appointment_type SET screening_flow_id=? WHERE appointment_type_id IN (?, ?)",
					screeningFlowId, pair.getAppointmentTypeId(), otherPair.getAppointmentTypeId());

			AppointmentBookingRequirements appointmentBookingRequirements =
					appointmentService.findAppointmentBookingRequirements(requestFor(account, pair), account);
			AppointmentBookingRequirements otherAppointmentTypeRequirements =
					appointmentService.findAppointmentBookingRequirements(requestFor(account, otherPair), account);
			AppointmentBookingRequirements resumedOtherAppointmentTypeRequirements =
					appointmentService.findAppointmentBookingRequirements(requestFor(account, otherPair), account);

			assertEquals(AppointmentBookingRequirementsDestinationId.SCREENING_SESSION,
					otherAppointmentTypeRequirements.getAppointmentBookingRequirementsDestinationId());
			assertNotNull(appointmentBookingRequirements.getScreeningSession());
			assertNotNull(otherAppointmentTypeRequirements.getScreeningSession());
			assertNotNull(resumedOtherAppointmentTypeRequirements.getScreeningSession());
			assertFalse(appointmentBookingRequirements.getScreeningSession().getScreeningSessionId()
					.equals(otherAppointmentTypeRequirements.getScreeningSession().getScreeningSessionId()));
			assertEquals(otherAppointmentTypeRequirements.getScreeningSession().getScreeningSessionId(),
					resumedOtherAppointmentTypeRequirements.getScreeningSession().getScreeningSessionId());
		});
	}

	protected ProviderAppointmentTypePair findProviderAppointmentTypePair(Database database) {
		return database.queryForObject("""
				SELECT p.provider_id, at.appointment_type_id
				FROM provider p, provider_appointment_type pat, v_appointment_type at
				WHERE p.provider_id=pat.provider_id
				AND pat.appointment_type_id=at.appointment_type_id
				AND p.institution_id=?
				AND p.active=TRUE
				ORDER BY p.name, at.name
				LIMIT 1
				""", ProviderAppointmentTypePair.class, InstitutionId.COBALT).get();
	}

	protected ProviderAppointmentTypePair findProviderAppointmentTypePairWithOtherAppointmentType(Database database) {
		return database.queryForObject("""
				SELECT
				  p.provider_id,
				  MIN(at.appointment_type_id::TEXT)::UUID AS appointment_type_id,
				  MAX(at.appointment_type_id::TEXT)::UUID AS other_appointment_type_id
				FROM provider p, provider_appointment_type pat, v_appointment_type at
				WHERE p.provider_id=pat.provider_id
				AND pat.appointment_type_id=at.appointment_type_id
				AND p.institution_id=?
				AND p.active=TRUE
				GROUP BY p.provider_id
				HAVING COUNT(DISTINCT at.appointment_type_id) > 1
				ORDER BY p.provider_id
				LIMIT 1
				""", ProviderAppointmentTypePair.class, InstitutionId.COBALT).get();
	}

	protected ProviderAppointmentTypePair pairForOtherAppointmentType(ProviderAppointmentTypePair pair) {
		ProviderAppointmentTypePair otherPair = new ProviderAppointmentTypePair();
		otherPair.setProviderId(pair.getProviderId());
		otherPair.setAppointmentTypeId(pair.getOtherAppointmentTypeId());
		return otherPair;
	}

	protected FindAppointmentBookingRequirementsRequest requestFor(Account account,
																																	 ProviderAppointmentTypePair pair) {
		FindAppointmentBookingRequirementsRequest request = new FindAppointmentBookingRequirementsRequest();
		request.setAccountId(account.getAccountId());
		request.setProviderId(pair.getProviderId());
		request.setAppointmentTypeId(pair.getAppointmentTypeId());
		request.setAppointmentSelectionTypeId(ProviderAppointmentSelectionTypeId.APPOINTMENT_PREDETERMINED);
		return request;
	}

	@ThreadSafe
	public static class ProviderAppointmentTypePair {
		@Nullable
		private UUID providerId;
		@Nullable
		private UUID appointmentTypeId;
		@Nullable
		private UUID otherAppointmentTypeId;

		@Nullable
		public UUID getProviderId() {
			return this.providerId;
		}

		public void setProviderId(@Nullable UUID providerId) {
			this.providerId = providerId;
		}

		@Nullable
		public UUID getAppointmentTypeId() {
			return this.appointmentTypeId;
		}

		public void setAppointmentTypeId(@Nullable UUID appointmentTypeId) {
			this.appointmentTypeId = appointmentTypeId;
		}

		@Nullable
		public UUID getOtherAppointmentTypeId() {
			return this.otherAppointmentTypeId;
		}

		public void setOtherAppointmentTypeId(@Nullable UUID otherAppointmentTypeId) {
			this.otherAppointmentTypeId = otherAppointmentTypeId;
		}
	}
}
