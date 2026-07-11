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
import com.cobaltplatform.api.integration.acuity.AcuitySchedulingClient;
import com.cobaltplatform.api.integration.acuity.MockAcuitySchedulingClient;
import com.cobaltplatform.api.integration.acuity.model.AcuityAppointment;
import com.cobaltplatform.api.integration.acuity.model.AcuityAppointmentType;
import com.cobaltplatform.api.integration.acuity.model.request.AcuityAppointmentCreateRequest;
import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.api.request.CreateAppointmentRequest;
import com.cobaltplatform.api.model.api.request.CreateAppointmentTypeRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningQuestionRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningSessionRequest;
import com.cobaltplatform.api.model.api.request.FindAppointmentBookingRequirementsRequest;
import com.cobaltplatform.api.model.api.request.UpdateAppointmentRequest;
import com.cobaltplatform.api.model.api.request.UpdateAppointmentTypeRequest;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentModalityId;
import com.cobaltplatform.api.model.api.response.ProviderListDetailsApiResponse.ProviderAppointmentSelectionTypeId;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.FontSize.FontSizeId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.SourceSystem.SourceSystemId;
import com.cobaltplatform.api.model.db.VideoconferencePlatform.VideoconferencePlatformId;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.cobaltplatform.api.model.service.AppointmentBookingRequirements;
import com.cobaltplatform.api.model.service.AppointmentBookingRequirements.AppointmentBookingRequirementsDestinationId;
import com.cobaltplatform.api.model.service.AppointmentBookingScreeningKey;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.google.inject.AbstractModule;
import com.pyranid.Database;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
			assertEquals("PROVIDER", appointmentBookingRequirements.getContext().get("providerSearchResultTypeId"));
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

	@Test
	public void createAppointmentAllowsCompletedRequiredScreeningWithoutAppointmentBookingMetadata() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			ScreeningService screeningService = app.getInjector().getInstance(ScreeningService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			Account account = accountService.findAccountById(testData.getAccountId()).get();
			UUID screeningFlowId = institutionService.findInstitutionById(InstitutionId.COBALT).get().getFeatureScreeningFlowId();
			AppointmentBookingScreeningKey expectedScreeningKey =
					new AppointmentBookingScreeningKey(testData.getProviderId(), testData.getAppointmentTypeId(), screeningFlowId);

			setAppointmentTypeScreeningFlow(database, testData.getAppointmentTypeId(), screeningFlowId);
			createCompletedScreeningSession(screeningService, database, account, screeningFlowId);

			assertTrue(appointmentService.findCompletedAppointmentBookingScreeningKeys(account.getAccountId(),
					Set.of(expectedScreeningKey)).contains(expectedScreeningKey));

			UUID appointmentId = appointmentService.createAppointment(requestForAcuityAppointment(testData));

			assertNotNull(appointmentId);
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void createAppointmentAllowsCompletedProviderSearchScreeningForSameProviderAndFlow() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			Account account = accountService.findAccountById(testData.getAccountId()).get();
			UUID screeningFlowId = institutionService.findInstitutionById(InstitutionId.COBALT).get().getFeatureScreeningFlowId();

			setAppointmentTypeScreeningFlow(database, testData.getAppointmentTypeId(), screeningFlowId);
			createCompletedAppointmentBookingScreeningSession(appointmentService, database, account, pairFor(testData));

			UUID appointmentId = appointmentService.createAppointment(requestForAcuityAppointment(testData));

			assertNotNull(appointmentId);
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void createAppointmentAllowsCompletedProviderSearchScreeningForSameProviderAndFlowAcrossAppointmentTypes() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			Account account = accountService.findAccountById(testData.getAccountId()).get();
			UUID screeningFlowId = institutionService.findInstitutionById(InstitutionId.COBALT).get().getFeatureScreeningFlowId();
			UUID otherAppointmentTypeId = createAdditionalAcuityAppointmentType(database, acuitySchedulingClient,
					testData.getProviderId(), "Follow-up Consult", 45L, 2);
			AcuityAppointmentTestData otherAppointmentTypeTestData =
					new AcuityAppointmentTestData(testData.getAccountId(), testData.getProviderId(), otherAppointmentTypeId);

			setAppointmentTypeScreeningFlow(database, testData.getAppointmentTypeId(), screeningFlowId);
			setAppointmentTypeScreeningFlow(database, otherAppointmentTypeId, screeningFlowId);
			createCompletedAppointmentBookingScreeningSession(appointmentService, database, account, pairFor(testData));

			UUID appointmentId = appointmentService.createAppointment(requestForAcuityAppointment(otherAppointmentTypeTestData));

			assertNotNull(appointmentId);
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void createAppointmentRejectsMissingProviderSearchScreening() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			UUID screeningFlowId = institutionService.findInstitutionById(InstitutionId.COBALT).get().getFeatureScreeningFlowId();

			setAppointmentTypeScreeningFlow(database, testData.getAppointmentTypeId(), screeningFlowId);

			assertCreateAppointmentRejectsMissingScreening(appointmentService, requestForAcuityAppointment(testData));
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void createAppointmentIgnoresMissingProviderSearchScreeningWhenBookingV2Disabled() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			UUID screeningFlowId = institutionService.findInstitutionById(InstitutionId.COBALT).get().getFeatureScreeningFlowId();

				setBookingV2Enabled(database, false);
				setAppointmentTypeScreeningFlow(database, testData.getAppointmentTypeId(), screeningFlowId);

				CreateAppointmentRequest request = requestForAcuityAppointment(testData);
				request.setFirstName(null);
				request.setLastName(null);
				request.setEmailAddress(null);
				request.setPhoneNumber(null);

				UUID appointmentId = appointmentService.createAppointment(request);
				Appointment appointment = appointmentService.findAppointmentById(appointmentId).get();

			assertNotNull(appointmentId);
			assertNull(appointment.getFirstName());
			assertNull(appointment.getLastName());
			assertNull(appointment.getEmailAddress());
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void createAppointmentRejectsCompletedProviderSearchScreeningForOtherProvider() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			Account account = accountService.findAccountById(testData.getAccountId()).get();
			UUID screeningFlowId = institutionService.findInstitutionById(InstitutionId.COBALT).get().getFeatureScreeningFlowId();
			AcuityAppointmentTestData otherProviderTestData = createAdditionalAcuityProviderAppointmentTestData(database,
					acuitySchedulingClient, testData.getAccountId(), "Other Acuity Test Provider", "Other Initial Consult");

			setAppointmentTypeScreeningFlow(database, testData.getAppointmentTypeId(), screeningFlowId);
			setAppointmentTypeScreeningFlow(database, otherProviderTestData.getAppointmentTypeId(), screeningFlowId);
			createCompletedAppointmentBookingScreeningSession(appointmentService, database, account, pairFor(otherProviderTestData));

			assertCreateAppointmentRejectsMissingScreening(appointmentService, requestForAcuityAppointment(testData));
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void createAppointmentRejectsCompletedProviderSearchScreeningForOtherFlow() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			Account account = accountService.findAccountById(testData.getAccountId()).get();
			UUID screeningFlowId = institutionService.findInstitutionById(InstitutionId.COBALT).get().getFeatureScreeningFlowId();
			UUID otherScreeningFlowId = findOtherScreeningFlowId(database, screeningFlowId);
			UUID otherAppointmentTypeId = createAdditionalAcuityAppointmentType(database, acuitySchedulingClient,
					testData.getProviderId(), "Other Flow Consult", 45L, 2);
			AcuityAppointmentTestData otherFlowTestData =
					new AcuityAppointmentTestData(testData.getAccountId(), testData.getProviderId(), otherAppointmentTypeId);

			setAppointmentTypeScreeningFlow(database, testData.getAppointmentTypeId(), screeningFlowId);
			setAppointmentTypeScreeningFlow(database, otherAppointmentTypeId, otherScreeningFlowId);
			createCompletedAppointmentBookingScreeningSession(appointmentService, database, account, pairFor(otherFlowTestData));

			assertCreateAppointmentRejectsMissingScreening(appointmentService, requestForAcuityAppointment(testData));
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void createAppointmentUsesSubmittedNamesForAcuityRequest() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			Account originalAccount = accountService.findAccountById(testData.getAccountId()).get();
			CreateAppointmentRequest request = requestForAcuityAppointment(testData);
			request.setFirstName(" Submitted ");
			request.setLastName(" Person ");
			request.setEmailAddress("booking-email@cobaltinnovations.org");

			UUID appointmentId = appointmentService.createAppointment(request);

			assertNotNull(appointmentId);
			assertNotNull(acuitySchedulingClient.getLastCreateAppointmentRequest());
			assertEquals("booking-email@cobaltinnovations.org", acuitySchedulingClient.getLastCreateAppointmentRequest().getEmail());
			assertEquals("Submitted", acuitySchedulingClient.getLastCreateAppointmentRequest().getFirstName());
			assertEquals("Person", acuitySchedulingClient.getLastCreateAppointmentRequest().getLastName());

			Appointment appointment = appointmentService.findAppointmentById(appointmentId).get();
			assertEquals("Submitted", appointment.getFirstName());
			assertEquals("Person", appointment.getLastName());
			assertEquals("booking-email@cobaltinnovations.org", appointment.getEmailAddress());

			AppointmentApiResponse appointmentApiResponse = app.getInjector().getInstance(AppointmentApiResponseFactory.class).create(appointment);
			assertEquals("Submitted", appointmentApiResponse.getFirstName());
			assertEquals("Person", appointmentApiResponse.getLastName());
			assertEquals("booking-email@cobaltinnovations.org", appointmentApiResponse.getEmailAddress());

			Account account = accountService.findAccountById(testData.getAccountId()).get();
			assertEquals(originalAccount.getEmailAddress(), account.getEmailAddress());
			assertEquals("Account", account.getFirstName());
			assertEquals("Fallback", account.getLastName());
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void createAppointmentUpdatesAccountEmailWhenBookingV2Disabled() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			String legacyBookingEmailAddress = String.format("legacy-booking-%s@cobaltinnovations.org", UUID.randomUUID());
			CreateAppointmentRequest request = requestForAcuityAppointment(testData);
			request.setEmailAddress(legacyBookingEmailAddress);

			setBookingV2Enabled(database, false);
			database.execute("""
					INSERT INTO account_email_verification (account_id, code, email_address, verified)
					VALUES (?, ?, ?, ?)
					""", testData.getAccountId(), "654321", legacyBookingEmailAddress, true);

			UUID appointmentId = appointmentService.createAppointment(request);
			Appointment appointment = appointmentService.findAppointmentById(appointmentId).get();
			Account account = accountService.findAccountById(testData.getAccountId()).get();

			assertNotNull(appointmentId);
			assertEquals(legacyBookingEmailAddress, account.getEmailAddress());
			assertNull(appointment.getEmailAddress());
			assertNotNull(acuitySchedulingClient.getLastCreateAppointmentRequest());
			assertEquals(legacyBookingEmailAddress, acuitySchedulingClient.getLastCreateAppointmentRequest().getEmail());
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void createAppointmentRejectsMissingContactFields() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			CreateAppointmentRequest request = requestForAcuityAppointment(testData);
			request.setFirstName(null);
			request.setLastName(null);
			request.setEmailAddress(null);
			request.setPhoneNumber(null);

			assertCreateAppointmentRejectsContactFields(appointmentService, request);
			assertNull(acuitySchedulingClient.getLastCreateAppointmentRequest());
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void createAppointmentRejectsBlankContactFields() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			CreateAppointmentRequest request = requestForAcuityAppointment(testData);
			request.setFirstName("   ");
			request.setLastName("   ");
			request.setEmailAddress("   ");
			request.setPhoneNumber("   ");

			assertCreateAppointmentRejectsContactFields(appointmentService, request);
			assertNull(acuitySchedulingClient.getLastCreateAppointmentRequest());
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void createAppointmentRejectsInvalidSubmittedEmailAddress() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			Account originalAccount = accountService.findAccountById(testData.getAccountId()).get();
			CreateAppointmentRequest request = requestForAcuityAppointment(testData);
			request.setEmailAddress("invalid-email-address");

			try {
				appointmentService.createAppointment(request);
				fail("Expected appointment creation to reject an invalid email address.");
			} catch (ValidationException e) {
				assertTrue(e.getFieldErrors().contains(new FieldError("emailAddress", "Email address is invalid.")));
			}

			assertNull(acuitySchedulingClient.getLastCreateAppointmentRequest());
			assertFalse(appointmentService.findAppointmentsByAccountId(testData.getAccountId()).stream()
					.anyMatch(appointment -> testData.getProviderId().equals(appointment.getProviderId())));
			Account account = accountService.findAccountById(testData.getAccountId()).get();
			assertEquals(originalAccount.getEmailAddress(), account.getEmailAddress());
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void createAppointmentAllowsPhoneBookingForProviderWithUnsupportedVideoconferencePlatform() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);

			database.execute("""
					UPDATE provider
					SET videoconference_platform_id=?,
					    phone_number=?
					WHERE provider_id=?
					""", VideoconferencePlatformId.BLUEJEANS, "+12155551000", testData.getProviderId());

			CreateAppointmentRequest request = requestForAcuityAppointment(testData);
			request.setAppointmentModalityId(ProviderAppointmentModalityId.PHONE);

			UUID appointmentId = appointmentService.createAppointment(request);

			assertNotNull(appointmentId);
			assertNotNull(acuitySchedulingClient.getLastCreateAppointmentRequest());
			Appointment appointment = appointmentService.findAppointmentById(appointmentId).get();
			assertEquals(VideoconferencePlatformId.TELEPHONE, appointment.getVideoconferencePlatformId());
			assertEquals("+12155551000", appointment.getPhoneNumber());
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void createAppointmentUsesLegacyVideoconferencePlatformWhenBookingV2Disabled() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);

			setBookingV2Enabled(database, false);
			database.execute("""
					UPDATE provider
					SET videoconference_platform_id=?,
					    phone_number=?
					WHERE provider_id=?
					""", VideoconferencePlatformId.BLUEJEANS, "+12155551000", testData.getProviderId());

				CreateAppointmentRequest request = requestForAcuityAppointment(testData);
				request.setAppointmentModalityId(ProviderAppointmentModalityId.PHONE);
				request.setFirstName(null);
				request.setLastName(null);
				request.setEmailAddress(null);
				request.setPhoneNumber(null);

				try {
					appointmentService.createAppointment(request);
					fail("Expected legacy booking to reject unsupported provider videoconference platform.");
				} catch (ValidationException e) {
					assertTrue(e.getGlobalErrors().contains("Sorry, this provider's videoconference platform is no longer supported. Please choose a different provider."));
			}

			assertNull(acuitySchedulingClient.getLastCreateAppointmentRequest());
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void createAppointmentRejectsVirtualBookingForProviderWithUnsupportedVideoconferencePlatform() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);

			database.execute("""
					UPDATE provider
					SET videoconference_platform_id=?,
					    phone_number=?
					WHERE provider_id=?
					""", VideoconferencePlatformId.BLUEJEANS, "+12155551000", testData.getProviderId());

			CreateAppointmentRequest request = requestForAcuityAppointment(testData);
			request.setAppointmentModalityId(ProviderAppointmentModalityId.VIRTUAL);

			try {
				appointmentService.createAppointment(request);
				fail("Expected appointment creation to reject unsupported virtual booking.");
			} catch (ValidationException e) {
				assertTrue(e.getGlobalErrors().contains("Sorry, this provider's videoconference platform is no longer supported. Please choose a different provider."));
			}

			assertNull(acuitySchedulingClient.getLastCreateAppointmentRequest());
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void rescheduleAppointmentCarriesForwardStoredNamesWhenNamesAreOmitted() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			CreateAppointmentRequest createRequest = requestForAcuityAppointment(testData);
			createRequest.setFirstName("Original");
			createRequest.setLastName("Snapshot");
			createRequest.setEmailAddress("reschedule-carry-forward@cobaltinnovations.org");
			UUID originalAppointmentId = appointmentService.createAppointment(createRequest);

			UpdateAppointmentRequest updateRequest = new UpdateAppointmentRequest();
			updateRequest.setAppointmentId(originalAppointmentId);
			updateRequest.setAccountId(testData.getAccountId());
			updateRequest.setCreatedByAcountId(testData.getAccountId());
			updateRequest.setProviderId(testData.getProviderId());
			updateRequest.setAppointmentTypeId(testData.getAppointmentTypeId());
			updateRequest.setDate(LocalDate.now().plusDays(31));
			updateRequest.setTime(LocalTime.of(11, 0));

			UUID rescheduledAppointmentId = appointmentService.rescheduleAppointment(updateRequest);

			Appointment rescheduledAppointment = appointmentService.findAppointmentById(rescheduledAppointmentId).get();
			assertEquals("Original", rescheduledAppointment.getFirstName());
			assertEquals("Snapshot", rescheduledAppointment.getLastName());
			assertEquals("reschedule-carry-forward@cobaltinnovations.org", rescheduledAppointment.getEmailAddress());
			assertNotNull(acuitySchedulingClient.getLastCreateAppointmentRequest());
			assertEquals("reschedule-carry-forward@cobaltinnovations.org", acuitySchedulingClient.getLastCreateAppointmentRequest().getEmail());
			assertEquals("Original", acuitySchedulingClient.getLastCreateAppointmentRequest().getFirstName());
			assertEquals("Snapshot", acuitySchedulingClient.getLastCreateAppointmentRequest().getLastName());
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void rescheduleAppointmentUsesSubmittedNamesWhenProvided() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			CreateAppointmentRequest createRequest = requestForAcuityAppointment(testData);
			createRequest.setFirstName("Original");
			createRequest.setLastName("Snapshot");
			UUID originalAppointmentId = appointmentService.createAppointment(createRequest);

			UpdateAppointmentRequest updateRequest = new UpdateAppointmentRequest();
			updateRequest.setAppointmentId(originalAppointmentId);
			updateRequest.setAccountId(testData.getAccountId());
			updateRequest.setCreatedByAcountId(testData.getAccountId());
			updateRequest.setProviderId(testData.getProviderId());
			updateRequest.setAppointmentTypeId(testData.getAppointmentTypeId());
			updateRequest.setFirstName(" Rescheduled ");
			updateRequest.setLastName(" Patient ");
			updateRequest.setDate(LocalDate.now().plusDays(31));
			updateRequest.setTime(LocalTime.of(11, 0));

			UUID rescheduledAppointmentId = appointmentService.rescheduleAppointment(updateRequest);

			Appointment rescheduledAppointment = appointmentService.findAppointmentById(rescheduledAppointmentId).get();
			assertEquals("Rescheduled", rescheduledAppointment.getFirstName());
			assertEquals("Patient", rescheduledAppointment.getLastName());
			assertNotNull(acuitySchedulingClient.getLastCreateAppointmentRequest());
			assertEquals("Rescheduled", acuitySchedulingClient.getLastCreateAppointmentRequest().getFirstName());
			assertEquals("Patient", acuitySchedulingClient.getLastCreateAppointmentRequest().getLastName());
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void createAppointmentTypeIgnoresScreeningFlowWhenBookingV2Disabled() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			UUID screeningFlowId = institutionService.findInstitutionById(InstitutionId.COBALT).get().getFeatureScreeningFlowId();

			setBookingV2Enabled(database, false);

			UUID appointmentTypeId = appointmentService.createAppointmentType(appointmentTypeRequest(testData.getProviderId(), screeningFlowId));

			assertNull(appointmentService.findAppointmentTypeById(appointmentTypeId).get().getScreeningFlowId());
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void createAppointmentTypePersistsScreeningFlowWhenBookingV2Enabled() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			UUID screeningFlowId = institutionService.findInstitutionById(InstitutionId.COBALT).get().getFeatureScreeningFlowId();

			setBookingV2Enabled(database, true);

			UUID appointmentTypeId = appointmentService.createAppointmentType(appointmentTypeRequest(testData.getProviderId(), screeningFlowId));

			assertEquals(screeningFlowId, appointmentService.findAppointmentTypeById(appointmentTypeId).get().getScreeningFlowId());
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void createAppointmentTypeCreatesScreeningFlowForScreeningQuestionsWhenBookingV2Enabled() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			CreateAppointmentTypeRequest request = appointmentTypeRequest(testData.getProviderId());
			request.setScreeningQuestions(screeningQuestions("Are you seeking therapy?", "Are you in Pennsylvania?"));

			setBookingV2Enabled(database, true);

			UUID appointmentTypeId = appointmentService.createAppointmentType(request);
			AppointmentType appointmentType = appointmentService.findAppointmentTypeById(appointmentTypeId).get();

			assertNotNull(appointmentType.getScreeningFlowId());
			assertEquals(0L, activeAssessmentCount(database, appointmentTypeId));
			assertEquals(2L, activeInitialScreeningQuestionCount(database, appointmentType.getScreeningFlowId()));
			assertEquals(4L, activeInitialScreeningAnswerOptionCount(database, appointmentType.getScreeningFlowId()));
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void updateAppointmentTypeVersionsScreeningFlowAndPreservesLegacyAssessmentWhenBookingV2Enabled() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			CreateAppointmentTypeRequest createRequest = appointmentTypeRequest(testData.getProviderId());
			createRequest.setScreeningQuestions(screeningQuestions("Legacy screening?"));

			setBookingV2Enabled(database, false);
			UUID appointmentTypeId = appointmentService.createAppointmentType(createRequest);
			assertEquals(1L, activeAssessmentCount(database, appointmentTypeId));

			setBookingV2Enabled(database, true);
			appointmentService.updateAppointmentType(updateAppointmentTypeRequest(testData.getProviderId(), appointmentTypeId,
					screeningQuestions("First v2 question?", "Second v2 question?")));

			AppointmentType firstUpdate = appointmentService.findAppointmentTypeById(appointmentTypeId).get();
			UUID screeningFlowId = firstUpdate.getScreeningFlowId();
			UUID firstScreeningFlowVersionId = activeScreeningFlowVersionId(database, screeningFlowId);

			assertNotNull(screeningFlowId);
			assertEquals(1L, activeAssessmentCount(database, appointmentTypeId));
			assertEquals(2L, activeInitialScreeningQuestionCount(database, screeningFlowId));

			appointmentService.updateAppointmentType(updateAppointmentTypeRequest(testData.getProviderId(), appointmentTypeId,
					screeningQuestions("Replacement v2 question?")));

			AppointmentType secondUpdate = appointmentService.findAppointmentTypeById(appointmentTypeId).get();
			UUID secondScreeningFlowVersionId = activeScreeningFlowVersionId(database, screeningFlowId);

			assertEquals(screeningFlowId, secondUpdate.getScreeningFlowId());
			assertFalse(firstScreeningFlowVersionId.equals(secondScreeningFlowVersionId));
			assertEquals(1L, activeAssessmentCount(database, appointmentTypeId));
			assertEquals(1L, activeInitialScreeningQuestionCount(database, screeningFlowId));
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void updateAppointmentTypeClearsScreeningFlowWhenBookingV2EnabledAndQuestionsRemoved() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			CreateAppointmentTypeRequest createRequest = appointmentTypeRequest(testData.getProviderId());
			createRequest.setScreeningQuestions(screeningQuestions("Initial v2 question?"));

			setBookingV2Enabled(database, true);
			UUID appointmentTypeId = appointmentService.createAppointmentType(createRequest);
			assertNotNull(appointmentService.findAppointmentTypeById(appointmentTypeId).get().getScreeningFlowId());

			appointmentService.updateAppointmentType(updateAppointmentTypeRequest(testData.getProviderId(), appointmentTypeId,
					List.of()));

			assertNull(appointmentService.findAppointmentTypeById(appointmentTypeId).get().getScreeningFlowId());
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void createAppointmentTypeKeepsAssessmentBehaviorForScreeningQuestionsWhenBookingV2Disabled() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			CreateAppointmentTypeRequest request = appointmentTypeRequest(testData.getProviderId());
			request.setScreeningQuestions(screeningQuestions("Legacy screening?"));

			setBookingV2Enabled(database, false);
			UUID appointmentTypeId = appointmentService.createAppointmentType(request);

			assertNull(appointmentService.findAppointmentTypeById(appointmentTypeId).get().getScreeningFlowId());
			assertEquals(1L, activeAssessmentCount(database, appointmentTypeId));
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Test
	public void createAppointmentTypeRejectsScreeningFlowFromOtherInstitutionWhenBookingV2Enabled() {
		RecordingAcuitySchedulingClient acuitySchedulingClient = new RecordingAcuitySchedulingClient();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AcuityAppointmentTestData testData = createAcuityAppointmentTestData(accountService, database, acuitySchedulingClient);
			UUID otherInstitutionScreeningFlowId = createOtherInstitutionScreeningFlow(database,
					accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0).getAccountId());
			CreateAppointmentTypeRequest request = appointmentTypeRequest(testData.getProviderId(), otherInstitutionScreeningFlowId);

			setBookingV2Enabled(database, true);

			try {
				appointmentService.createAppointmentType(request);
				fail("Expected appointment type creation to reject a screening flow from another institution.");
			} catch (ValidationException e) {
				Optional<FieldError> fieldError = e.getFieldErrors().stream()
						.filter(error -> "screeningFlowId".equals(error.getField()))
						.findFirst();

				assertTrue(fieldError.isPresent());
			}
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(AcuitySchedulingClient.class).toInstance(acuitySchedulingClient);
			}
		});
	}

	@Nonnull
	protected AcuityAppointmentTestData createAcuityAppointmentTestData(@Nonnull AccountService accountService,
																																		 @Nonnull Database database,
																																		 @Nonnull RecordingAcuitySchedulingClient acuitySchedulingClient) {
		setBookingV2Enabled(database, true);

		String uniqueSuffix = UUID.randomUUID().toString();
		String accountEmailAddress = String.format("patient-%s@cobaltinnovations.org", uniqueSuffix);
		UUID accountId = accountService.createAccount(new CreateAccountRequest() {{
			setRoleId(RoleId.PATIENT);
			setInstitutionId(InstitutionId.COBALT);
			setAccountSourceId(AccountSourceId.COBALT_SSO);
			setSourceSystemId(SourceSystemId.COBALT);
			setSsoId(String.format("appointment-test-%s", uniqueSuffix));
			setEmailAddress(accountEmailAddress);
			setPhoneNumber("+12155551212");
			setFirstName("Account");
			setLastName("Fallback");
			setDisplayName("Account Fallback");
		}});

		database.execute("""
				INSERT INTO account_email_verification (account_id, code, email_address, verified)
				VALUES (?, ?, ?, ?)
				""", accountId, "123456", accountEmailAddress, true);

		UUID providerId = UUID.randomUUID();

			database.execute("""
					INSERT INTO provider (
					  provider_id, institution_id, name, url_name, email_address, locale, time_zone, acuity_calendar_id,
					  scheduling_system_id, videoconference_platform_id, videoconference_url
					)
					VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
					""", providerId, InstitutionId.COBALT, "Acuity Test Provider",
					String.format("acuity-test-provider-%s", uniqueSuffix),
					String.format("provider-%s@cobaltinnovations.org", uniqueSuffix), "en-US", "America/New_York",
					123456L, SchedulingSystemId.ACUITY, VideoconferencePlatformId.EXTERNAL, "https://example.com/meeting");

		UUID appointmentTypeId = UUID.randomUUID();
		Long acuityAppointmentTypeId = Math.abs(UUID.randomUUID().getMostSignificantBits() % 1_000_000_000L) + 1_000_000_000L;

		database.execute("""
				INSERT INTO appointment_type (
				  appointment_type_id, acuity_appointment_type_id, visit_type_id, name, duration_in_minutes,
				  scheduling_system_id
				)
				VALUES (?, ?, ?, ?, ?, ?)
				""", appointmentTypeId, acuityAppointmentTypeId, VisitTypeId.INITIAL, "Initial Consult", 60L,
				SchedulingSystemId.ACUITY);

		database.execute("""
				INSERT INTO provider_appointment_type (provider_id, appointment_type_id, display_order)
				VALUES (?, ?, ?)
				""", providerId, appointmentTypeId, 1);

		AcuityAppointmentType acuityAppointmentType = new AcuityAppointmentType();
		acuityAppointmentType.setId(acuityAppointmentTypeId);
		acuityAppointmentType.setName("Initial Consult");
		acuityAppointmentType.setDuration(60.0);
		acuitySchedulingClient.setAppointmentType(acuityAppointmentType);

		return new AcuityAppointmentTestData(accountId, providerId, appointmentTypeId);
	}

	@Nonnull
	protected CreateAppointmentTypeRequest appointmentTypeRequest(@Nonnull UUID providerId,
																															 @Nonnull UUID screeningFlowId) {
		CreateAppointmentTypeRequest request = appointmentTypeRequest(providerId);
		request.setScreeningFlowId(screeningFlowId);
		return request;
	}

	@Nonnull
	protected CreateAppointmentTypeRequest appointmentTypeRequest(@Nonnull UUID providerId) {
		CreateAppointmentTypeRequest request = new CreateAppointmentTypeRequest();
		request.setProviderId(providerId);
		request.setSchedulingSystemId(SchedulingSystemId.COBALT);
		request.setVisitTypeId(VisitTypeId.INITIAL);
		request.setName("Native Visit");
		request.setDescription("Native visit");
		request.setDurationInMinutes(30L);
		request.setHexColor("#336699");
		return request;
	}

	@Nonnull
	protected UpdateAppointmentTypeRequest updateAppointmentTypeRequest(@Nonnull UUID providerId,
																																		 @Nonnull UUID appointmentTypeId,
																																		 @Nonnull List<CreateScreeningQuestionRequest> screeningQuestions) {
		UpdateAppointmentTypeRequest request = new UpdateAppointmentTypeRequest();
		request.setAppointmentTypeId(appointmentTypeId);
		request.setProviderId(providerId);
		request.setSchedulingSystemId(SchedulingSystemId.COBALT);
		request.setVisitTypeId(VisitTypeId.INITIAL);
		request.setName("Updated Native Visit");
		request.setDescription("Updated native visit");
		request.setDurationInMinutes(30L);
		request.setHexColor("#336699");
		request.setScreeningQuestions(screeningQuestions);
		return request;
	}

	@Nonnull
	protected List<CreateScreeningQuestionRequest> screeningQuestions(@Nonnull String... questions) {
		List<CreateScreeningQuestionRequest> screeningQuestions = new ArrayList<>(questions.length);

		for (String question : questions) {
			CreateScreeningQuestionRequest screeningQuestion = new CreateScreeningQuestionRequest();
			screeningQuestion.setQuestion(question);
			screeningQuestion.setFontSizeId(FontSizeId.DEFAULT);
			screeningQuestions.add(screeningQuestion);
		}

		return screeningQuestions;
	}

	protected long activeAssessmentCount(@Nonnull Database database,
																			 @Nonnull UUID appointmentTypeId) {
		return database.queryForObject("""
				SELECT COUNT(*)
				FROM appointment_type_assessment
				WHERE appointment_type_id=?
				AND active=TRUE
				""", Long.class, appointmentTypeId).get();
	}

	protected long activeInitialScreeningQuestionCount(@Nonnull Database database,
																										 @Nonnull UUID screeningFlowId) {
		return database.queryForObject("""
				SELECT COUNT(*)
				FROM screening_question sq
				JOIN screening s
					ON s.active_screening_version_id=sq.screening_version_id
				JOIN screening_flow_version sfv
					ON sfv.initial_screening_id=s.screening_id
				JOIN screening_flow sf
					ON sf.active_screening_flow_version_id=sfv.screening_flow_version_id
				WHERE sf.screening_flow_id=?
				""", Long.class, screeningFlowId).get();
	}

	protected long activeInitialScreeningAnswerOptionCount(@Nonnull Database database,
																											 @Nonnull UUID screeningFlowId) {
		return database.queryForObject("""
				SELECT COUNT(*)
				FROM screening_answer_option sao
				JOIN screening_question sq
					ON sq.screening_question_id=sao.screening_question_id
				JOIN screening s
					ON s.active_screening_version_id=sq.screening_version_id
				JOIN screening_flow_version sfv
					ON sfv.initial_screening_id=s.screening_id
				JOIN screening_flow sf
					ON sf.active_screening_flow_version_id=sfv.screening_flow_version_id
				WHERE sf.screening_flow_id=?
				""", Long.class, screeningFlowId).get();
	}

	@Nonnull
	protected UUID activeScreeningFlowVersionId(@Nonnull Database database,
																							@Nonnull UUID screeningFlowId) {
		return database.queryForObject("""
				SELECT active_screening_flow_version_id
				FROM screening_flow
				WHERE screening_flow_id=?
				""", UUID.class, screeningFlowId).get();
	}

	@Nonnull
	protected UUID createOtherInstitutionScreeningFlow(@Nonnull Database database,
																										 @Nonnull UUID createdByAccountId) {
		UUID screeningFlowId = UUID.randomUUID();

		database.execute("""
				INSERT INTO screening_flow (
				  screening_flow_id,
				  institution_id,
				  screening_flow_type_id,
				  created_by_account_id,
				  name
				) VALUES (?, ?, ?, ?, ?)
				""", screeningFlowId, InstitutionId.COBALT_COURSES, "PROVIDER_INTAKE", createdByAccountId,
				String.format("Other Institution Flow %s", screeningFlowId));

		return screeningFlowId;
	}

	@Nonnull
	protected UUID createAdditionalAcuityAppointmentType(@Nonnull Database database,
																											@Nonnull RecordingAcuitySchedulingClient acuitySchedulingClient,
																											@Nonnull UUID providerId,
																											@Nonnull String appointmentTypeName,
																											@Nonnull Long durationInMinutes,
																											int displayOrder) {
		UUID appointmentTypeId = UUID.randomUUID();
		Long acuityAppointmentTypeId = Math.abs(UUID.randomUUID().getMostSignificantBits() % 1_000_000_000L) + 1_000_000_000L;

		database.execute("""
				INSERT INTO appointment_type (
				  appointment_type_id, acuity_appointment_type_id, visit_type_id, name, duration_in_minutes,
				  scheduling_system_id
				)
				VALUES (?, ?, ?, ?, ?, ?)
				""", appointmentTypeId, acuityAppointmentTypeId, VisitTypeId.INITIAL, appointmentTypeName, durationInMinutes,
				SchedulingSystemId.ACUITY);

		database.execute("""
				INSERT INTO provider_appointment_type (provider_id, appointment_type_id, display_order)
				VALUES (?, ?, ?)
				""", providerId, appointmentTypeId, displayOrder);

		AcuityAppointmentType acuityAppointmentType = new AcuityAppointmentType();
		acuityAppointmentType.setId(acuityAppointmentTypeId);
		acuityAppointmentType.setName(appointmentTypeName);
		acuityAppointmentType.setDuration(durationInMinutes.doubleValue());
		acuitySchedulingClient.setAppointmentType(acuityAppointmentType);

		return appointmentTypeId;
	}

	@Nonnull
	protected AcuityAppointmentTestData createAdditionalAcuityProviderAppointmentTestData(@Nonnull Database database,
																																											 @Nonnull RecordingAcuitySchedulingClient acuitySchedulingClient,
																																											 @Nonnull UUID accountId,
																																											 @Nonnull String providerName,
																																											 @Nonnull String appointmentTypeName) {
		String uniqueSuffix = UUID.randomUUID().toString();
		UUID providerId = UUID.randomUUID();

			database.execute("""
					INSERT INTO provider (
					  provider_id, institution_id, name, url_name, email_address, locale, time_zone, acuity_calendar_id,
					  scheduling_system_id, videoconference_platform_id, videoconference_url
					)
					VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
					""", providerId, InstitutionId.COBALT, providerName,
					String.format("other-acuity-test-provider-%s", uniqueSuffix),
					String.format("provider-%s@cobaltinnovations.org", uniqueSuffix), "en-US", "America/New_York",
					123457L, SchedulingSystemId.ACUITY, VideoconferencePlatformId.EXTERNAL, "https://example.com/other-meeting");

		UUID appointmentTypeId = createAdditionalAcuityAppointmentType(database, acuitySchedulingClient, providerId,
				appointmentTypeName, 60L, 1);

		return new AcuityAppointmentTestData(accountId, providerId, appointmentTypeId);
	}

	protected void setAppointmentTypeScreeningFlow(@Nonnull Database database,
																								 @Nonnull UUID appointmentTypeId,
																								 @Nonnull UUID screeningFlowId) {
		database.execute("UPDATE appointment_type SET screening_flow_id=? WHERE appointment_type_id=?",
				screeningFlowId, appointmentTypeId);
	}

	protected void setBookingV2Enabled(@Nonnull Database database,
																		 boolean enabled) {
		database.execute("UPDATE institution SET booking_v2_enabled=? WHERE institution_id=?", enabled, InstitutionId.COBALT);
	}

	@Nonnull
	protected UUID createCompletedScreeningSession(@Nonnull ScreeningService screeningService,
																								 @Nonnull Database database,
																								 @Nonnull Account account,
																								 @Nonnull UUID screeningFlowId) {
		CreateScreeningSessionRequest request = new CreateScreeningSessionRequest();
		request.setScreeningFlowId(screeningFlowId);
		request.setTargetAccountId(account.getAccountId());
		request.setCreatedByAccountId(account.getAccountId());

		UUID screeningSessionId = screeningService.createScreeningSession(request);
		completeScreeningSession(database, screeningSessionId);
		return screeningSessionId;
	}

	@Nonnull
	protected UUID createCompletedAppointmentBookingScreeningSession(@Nonnull AppointmentService appointmentService,
																																	 @Nonnull Database database,
																																	 @Nonnull Account account,
																																	 @Nonnull ProviderAppointmentTypePair pair) {
		AppointmentBookingRequirements appointmentBookingRequirements =
				appointmentService.findAppointmentBookingRequirements(requestFor(account, pair), account);

		assertEquals(AppointmentBookingRequirementsDestinationId.SCREENING_SESSION,
				appointmentBookingRequirements.getAppointmentBookingRequirementsDestinationId());
		assertNotNull(appointmentBookingRequirements.getScreeningSession());

		UUID screeningSessionId = appointmentBookingRequirements.getScreeningSession().getScreeningSessionId();
		completeScreeningSession(database, screeningSessionId);
		return screeningSessionId;
	}

	protected void completeScreeningSession(@Nonnull Database database,
																					@Nonnull UUID screeningSessionId) {
		database.execute("""
				UPDATE screening_session
				SET completed=TRUE,
				    completed_at=NOW()
				WHERE screening_session_id=?
				""", screeningSessionId);
	}

	@Nonnull
	protected UUID findOtherScreeningFlowId(@Nonnull Database database,
																					@Nonnull UUID screeningFlowId) {
		return database.queryForObject("""
				SELECT screening_flow_id
				FROM screening_flow
				WHERE screening_flow_id<>?
				AND active_screening_flow_version_id IS NOT NULL
				ORDER BY screening_flow_id
				LIMIT 1
				""", UUID.class, screeningFlowId).get();
	}

	protected void assertCreateAppointmentRejectsMissingScreening(@Nonnull AppointmentService appointmentService,
																																	@Nonnull CreateAppointmentRequest request) {
		try {
			appointmentService.createAppointment(request);
			fail("Expected appointment creation to fail because required screening was not completed.");
		} catch (ValidationException e) {
			assertTrue(e.getGlobalErrors().contains("You did not complete the necessary screening questions to book this appointment."));
		}
	}

	protected void assertCreateAppointmentRejectsContactFields(@Nonnull AppointmentService appointmentService,
																														 @Nonnull CreateAppointmentRequest request) {
		try {
			appointmentService.createAppointment(request);
			fail("Expected appointment creation to fail because required contact fields were not supplied.");
		} catch (ValidationException e) {
			assertEquals(0, e.getGlobalErrors().size());
			assertEquals(4, e.getFieldErrors().size());
			assertTrue(e.getFieldErrors().contains(new FieldError("firstName", "First name is required.")));
			assertTrue(e.getFieldErrors().contains(new FieldError("lastName", "Last name is required.")));
			assertTrue(e.getFieldErrors().contains(new FieldError("emailAddress", "Email address is required.")));
			assertTrue(e.getFieldErrors().contains(new FieldError("phoneNumber", "Phone number is required.")));
		}
	}

	@Nonnull
	protected CreateAppointmentRequest requestForAcuityAppointment(@Nonnull AcuityAppointmentTestData testData) {
		CreateAppointmentRequest request = new CreateAppointmentRequest();
		request.setAccountId(testData.getAccountId());
		request.setCreatedByAcountId(testData.getAccountId());
		request.setProviderId(testData.getProviderId());
		request.setAppointmentTypeId(testData.getAppointmentTypeId());
		request.setDate(LocalDate.now().plusDays(30));
		request.setTime(LocalTime.of(10, 0));
		request.setFirstName("Booking");
		request.setLastName("Patient");
		request.setEmailAddress("booking-email@cobaltinnovations.org");
		request.setPhoneNumber("+12155550123");
		return request;
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

	protected ProviderAppointmentTypePair pairFor(AcuityAppointmentTestData testData) {
		ProviderAppointmentTypePair pair = new ProviderAppointmentTypePair();
		pair.setProviderId(testData.getProviderId());
		pair.setAppointmentTypeId(testData.getAppointmentTypeId());
		return pair;
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

	@ThreadSafe
	public static class AcuityAppointmentTestData {
		@Nonnull
		private final UUID accountId;
		@Nonnull
		private final UUID providerId;
		@Nonnull
		private final UUID appointmentTypeId;

		public AcuityAppointmentTestData(@Nonnull UUID accountId,
																		 @Nonnull UUID providerId,
																		 @Nonnull UUID appointmentTypeId) {
			this.accountId = accountId;
			this.providerId = providerId;
			this.appointmentTypeId = appointmentTypeId;
		}

		@Nonnull
		public UUID getAccountId() {
			return this.accountId;
		}

		@Nonnull
		public UUID getProviderId() {
			return this.providerId;
		}

		@Nonnull
		public UUID getAppointmentTypeId() {
			return this.appointmentTypeId;
		}
	}

	@ThreadSafe
	public static class RecordingAcuitySchedulingClient extends MockAcuitySchedulingClient {
		@Nullable
		private AcuityAppointmentType appointmentType;
		@Nullable
		private AcuityAppointmentCreateRequest lastCreateAppointmentRequest;
		private Long nextAppointmentId = 1_000_000L;

		@Nonnull
		@Override
		public Optional<AcuityAppointmentType> findAppointmentTypeById(@Nullable Long appointmentTypeId) {
			if (appointmentType == null || !appointmentType.getId().equals(appointmentTypeId))
				return Optional.empty();

			return Optional.of(appointmentType);
		}

		@Nonnull
		@Override
		public AcuityAppointment createAppointment(@Nonnull AcuityAppointmentCreateRequest request) {
			this.lastCreateAppointmentRequest = request;

			AcuityAppointment acuityAppointment = new AcuityAppointment();
			acuityAppointment.setId(nextAppointmentId++);
			return acuityAppointment;
		}

		public void setAppointmentType(@Nullable AcuityAppointmentType appointmentType) {
			this.appointmentType = appointmentType;
		}

		@Nullable
		public AcuityAppointmentCreateRequest getLastCreateAppointmentRequest() {
			return this.lastCreateAppointmentRequest;
		}
	}
}
