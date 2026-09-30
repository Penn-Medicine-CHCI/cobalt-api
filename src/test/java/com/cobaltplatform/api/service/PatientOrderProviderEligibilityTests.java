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
import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.integration.enterprise.EnterprisePlugin;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.integration.epic.EpicClient;
import com.cobaltplatform.api.integration.epic.MockEpicClient;
import com.cobaltplatform.api.integration.epic.request.GetProviderScheduleRequest;
import com.cobaltplatform.api.integration.epic.request.ScheduleAppointmentWithInsuranceRequest;
import com.cobaltplatform.api.integration.epic.response.GetProviderScheduleResponse;
import com.cobaltplatform.api.integration.epic.response.ScheduleAppointmentWithInsuranceResponse;
import com.cobaltplatform.api.model.api.request.CreateAppointmentRequest;
import com.cobaltplatform.api.model.api.request.ProviderFindRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.EpicDepartment;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.service.ProviderFind;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.pyranid.Database;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Transmogrify, LLC.
 */
public class PatientOrderProviderEligibilityTests {
	@Test
	public void providerEligibilityPoolsRejectProvidersFromOtherInstitutions() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			String otherInstitutionId = findOtherInstitutionId(database);
			UUID poolId = UUID.randomUUID();
			UUID providerId = database.queryForObject("""
					SELECT provider_id
					FROM provider
					WHERE institution_id=?
					ORDER BY provider_id
					LIMIT 1
					""", UUID.class, InstitutionId.COBALT_IC).get();

			database.execute("""
					INSERT INTO provider_eligibility_pool (
						provider_eligibility_pool_id,
						institution_id,
						name
					) VALUES (?, ?, ?)
					""", poolId, otherInstitutionId, "Test Cross-Institution Pool " + poolId);

			try {
				database.execute("""
						INSERT INTO provider_eligibility_pool_provider (
							provider_eligibility_pool_id,
							provider_id
						) VALUES (?, ?)
						""", poolId, providerId);
				fail("Expected a pool to reject a provider from another institution.");
			} catch (RuntimeException expected) {
				// Expected: the migration's same-institution constraint rejected the membership.
			}
		});
	}

	@Test
	public void epicDepartmentsRejectPoolsFromOtherInstitutions() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			String otherInstitutionId = findOtherInstitutionId(database);
			UUID poolId = UUID.randomUUID();

			database.execute("""
					INSERT INTO provider_eligibility_pool (
						provider_eligibility_pool_id,
						institution_id,
						name
					) VALUES (?, ?, ?)
					""", poolId, otherInstitutionId, "Test Cross-Institution Pool " + poolId);

			try {
				insertDepartment(database, UUID.randomUUID(), "TEST-CROSS-INSTITUTION", null, poolId);
				fail("Expected a department to reject a pool from another institution.");
			} catch (RuntimeException expected) {
				// Expected: the migration's same-institution constraint rejected the assignment.
			}
		});
	}

	@Test
	public void providerEligibilityPoolsControlSearchAndSchedulingDepartment() {
		RecordingEpicClient epicClient = new RecordingEpicClient(LocalTime.of(10, 0));

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			PatientOrderService patientOrderService = app.getInjector().getInstance(PatientOrderService.class);
			ProviderService providerService = app.getInjector().getInstance(ProviderService.class);
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			database.execute("""
					INSERT INTO audit_log_event (audit_log_event_id, description)
					VALUES ('EPIC_APPOINTMENT_CREATE', 'Create an Epic appointment')
					ON CONFLICT (audit_log_event_id) DO NOTHING
					""");

			UUID poolAId = UUID.randomUUID();
			UUID poolBId = UUID.randomUUID();
			UUID overlappingPoolId = UUID.randomUUID();
			UUID emptyPoolId = UUID.randomUUID();
			UUID virtualDepartmentGuardPoolId = UUID.randomUUID();
			UUID virtualSchedulingDepartmentId = UUID.randomUUID();
			UUID poolADepartmentId = UUID.randomUUID();
			UUID poolBDepartmentId = UUID.randomUUID();
			UUID overlappingPoolDepartmentId = UUID.randomUUID();
			UUID emptyPoolDepartmentId = UUID.randomUUID();
			UUID selfSchedulingPoolDepartmentId = UUID.randomUUID();
			UUID legacyDepartmentId = UUID.randomUUID();
			UUID providerAId = UUID.randomUUID();
			UUID providerBId = UUID.randomUUID();
			UUID clinicId = UUID.randomUUID();
			UUID patientOrderImportId = UUID.randomUUID();

			insertPool(database, poolAId, "Test Pool A");
			insertPool(database, poolBId, "Test Pool B");
			insertPool(database, overlappingPoolId, "Test Overlapping Pool");
			insertPool(database, emptyPoolId, "Test Empty Pool");
			insertPool(database, virtualDepartmentGuardPoolId, "Test Virtual Department Guard Pool");
			insertDepartment(database, virtualSchedulingDepartmentId, "TEST-VIRTUAL", null, virtualDepartmentGuardPoolId);
			insertDepartment(database, poolADepartmentId, "TEST-POOL-A", virtualSchedulingDepartmentId, poolAId);
			insertDepartment(database, poolBDepartmentId, "TEST-POOL-B", virtualSchedulingDepartmentId, poolBId);
			insertDepartment(database, overlappingPoolDepartmentId, "TEST-POOL-OVERLAP", virtualSchedulingDepartmentId,
					overlappingPoolId);
			insertDepartment(database, emptyPoolDepartmentId, "TEST-EMPTY", virtualSchedulingDepartmentId, emptyPoolId);
			insertDepartment(database, selfSchedulingPoolDepartmentId, "TEST-SELF-POOL", null, poolAId);
			insertDepartment(database, legacyDepartmentId, "TEST-LEGACY", null, null);
			database.execute("""
					UPDATE epic_department
					SET connect_with_support_description_override=?
					WHERE epic_department_id=?
					""", "Virtual department support copy", virtualSchedulingDepartmentId);
			database.execute("""
					UPDATE epic_department
					SET connect_with_support_description_override=?
					WHERE epic_department_id=?
					""", "Pool B support copy", poolBDepartmentId);
			insertProvider(database, providerAId, "Test Provider A");
			insertProvider(database, providerBId, "Test Provider B");
			UUID appointmentTypeId = database.queryForObject("""
					SELECT pat.appointment_type_id
					FROM provider_appointment_type pat
					JOIN provider p ON p.provider_id=pat.provider_id
					JOIN appointment_type at ON at.appointment_type_id=pat.appointment_type_id
					WHERE p.institution_id=?
					AND at.scheduling_system_id='EPIC'
					ORDER BY pat.appointment_type_id
					LIMIT 1
					""", UUID.class, InstitutionId.COBALT_IC).get();
			database.execute("""
					INSERT INTO provider_appointment_type (
						provider_id,
						appointment_type_id,
						display_order
					) VALUES (?, ?, 1), (?, ?, 1)
					""", providerAId, appointmentTypeId, providerBId, appointmentTypeId);

			database.execute("""
					INSERT INTO provider_eligibility_pool_provider (
						provider_eligibility_pool_id,
						provider_id
					) VALUES (?, ?), (?, ?), (?, ?), (?, ?)
					""", poolAId, providerAId, poolBId, providerBId,
					overlappingPoolId, providerAId, overlappingPoolId, providerBId);
			database.execute("""
					INSERT INTO provider_epic_department (
						provider_id,
						epic_department_id,
						display_order
					) VALUES (?, ?, 1), (?, ?, 1), (?, ?, 2), (?, ?, 2)
					""", providerAId, virtualSchedulingDepartmentId,
					providerBId, virtualSchedulingDepartmentId,
					providerAId, legacyDepartmentId,
					providerBId, legacyDepartmentId);

			database.execute("""
					INSERT INTO clinic (
						clinic_id,
						description,
						institution_id
					) VALUES (?, 'Test Provider Eligibility Clinic', ?)
					""", clinicId, InstitutionId.COBALT_IC);
			database.execute("""
					INSERT INTO provider_clinic (
						provider_clinic_id,
						provider_id,
						clinic_id,
						primary_clinic
					) VALUES (?, ?, ?, TRUE), (?, ?, ?, TRUE)
					""", UUID.randomUUID(), providerAId, clinicId,
					UUID.randomUUID(), providerBId, clinicId);

			database.execute("""
					INSERT INTO patient_order_import (
						patient_order_import_id,
						patient_order_import_type_id,
						institution_id,
						raw_order
					) VALUES (?, 'CSV', ?, '{}')
					""", patientOrderImportId, InstitutionId.COBALT_IC);

			UUID poolAOrderId = insertOrder(database, patientOrderImportId, poolADepartmentId, null);
			UUID overlappingPoolOrderId = insertOrder(database, patientOrderImportId, overlappingPoolDepartmentId, null);
			UUID legacyOrderId = insertOrder(database, patientOrderImportId, legacyDepartmentId, null);
			UUID emptyPoolOrderId = insertOrder(database, patientOrderImportId, emptyPoolDepartmentId, null);
			UUID virtualDepartmentOrderId = insertOrder(database, patientOrderImportId, virtualSchedulingDepartmentId, null);
			UUID selfSchedulingPoolOrderId = insertOrder(database, patientOrderImportId, selfSchedulingPoolDepartmentId, null);
			UUID overriddenOrderId = insertOrder(database, patientOrderImportId, poolADepartmentId, poolBDepartmentId);
			UUID legacyOverriddenOrderId = insertOrder(database, patientOrderImportId, poolADepartmentId, legacyDepartmentId);
			LocalDateTime timeslot = LocalDate.now().plusDays(14).atTime(10, 0);

			database.execute("""
					INSERT INTO provider_availability (
						provider_availability_id,
						provider_id,
						date_time,
						appointment_type_id,
						epic_department_id
					) VALUES (?, ?, ?, ?, ?), (?, ?, ?, ?, ?)
					""", UUID.randomUUID(), providerAId, timeslot, appointmentTypeId, poolADepartmentId,
					UUID.randomUUID(), providerAId, timeslot, appointmentTypeId, virtualSchedulingDepartmentId);

			assertEquals(Set.of(providerAId),
					patientOrderService.findEligibleProviderIdsForPatientOrderId(poolAOrderId));
			assertEquals(Set.of(providerAId, providerBId),
					patientOrderService.findEligibleProviderIdsForPatientOrderId(overlappingPoolOrderId));
			assertEquals(Set.of(providerAId, providerBId),
					patientOrderService.findEligibleProviderIdsForPatientOrderId(legacyOrderId));
			assertTrue(patientOrderService.findEligibleProviderIdsForPatientOrderId(emptyPoolOrderId).isEmpty());
			assertTrue(patientOrderService.hasProviderEligibilityPoolForPatientOrderId(emptyPoolOrderId));
			assertFalse(patientOrderService.hasProviderEligibilityPoolForPatientOrderId(legacyOverriddenOrderId));
			assertEquals(Set.of(providerAId, providerBId),
					patientOrderService.findEligibleProviderIdsForPatientOrderId(legacyOverriddenOrderId));
			assertTrue(patientOrderService.findEligibleProviderIdsForPatientOrderId(virtualDepartmentOrderId).isEmpty());
			assertEquals(Set.of(providerAId),
					patientOrderService.findEligibleProviderIdsForPatientOrderId(selfSchedulingPoolOrderId));
			assertEquals(selfSchedulingPoolDepartmentId,
					patientOrderService.findSchedulingEpicDepartmentIdForPatientOrderId(selfSchedulingPoolOrderId));
			assertEquals(legacyDepartmentId,
					patientOrderService.findSchedulingEpicDepartmentIdForPatientOrderId(legacyOrderId));
			assertEquals(Set.of(providerBId),
					patientOrderService.findEligibleProviderIdsForPatientOrderId(overriddenOrderId));
			assertEquals(virtualSchedulingDepartmentId,
					patientOrderService.findSchedulingEpicDepartmentIdForPatientOrderId(overriddenOrderId));
			assertEquals("Pool B support copy",
					patientOrderService.findConnectWithSupportDescriptionOverrideForPatientOrderId(overriddenOrderId).get());

			UUID accountId = database.queryForObject("""
					SELECT account_id
					FROM account
					WHERE institution_id=?
					AND active=TRUE
					ORDER BY CASE WHEN role_id='PATIENT' THEN 0 ELSE 1 END, account_id
					LIMIT 1
					""", UUID.class, InstitutionId.COBALT_IC).get();
			database.execute("""
					UPDATE account
					SET epic_patient_unique_id=?, epic_patient_unique_id_type='UID'
					WHERE account_id=?
					""", "test-epic-patient-" + accountId, accountId);
			Account account = accountService.findAccountById(accountId).get();

			List<ProviderFind> poolAProviderFinds = providerService.findProviders(
					providerFindRequest(poolAOrderId, null, Set.of()), account, false);
			assertEquals(Set.of(providerAId), providerIds(poolAProviderFinds));
			assertEquals(Set.of(virtualSchedulingDepartmentId), availabilityEpicDepartmentIds(poolAProviderFinds));
			assertEquals(Set.of(providerAId), providerIds(providerService.findProviders(
					providerFindRequest(poolAOrderId, providerAId, Set.of()), account, false)));
			assertTrue(providerService.findProviders(
					providerFindRequest(poolAOrderId, providerBId, Set.of()), account, false).isEmpty());
			assertEquals(Set.of(providerAId), providerIds(providerService.findProviders(
					providerFindRequest(poolAOrderId, null, Set.of(clinicId)), account, false)));
			assertTrue(providerService.findProviders(
					providerFindRequest(emptyPoolOrderId, null, Set.of()), account, false).isEmpty());
			assertTrue(providerService.findProviders(
					providerFindRequest(virtualDepartmentOrderId, null, Set.of()), account, false).isEmpty());

			CreateAppointmentRequest appointmentRequest = new CreateAppointmentRequest();
			appointmentRequest.setAccountId(accountId);
			appointmentRequest.setCreatedByAcountId(accountId);
			appointmentRequest.setProviderId(providerBId);
			appointmentRequest.setAppointmentTypeId(appointmentTypeId);
			appointmentRequest.setPatientOrderId(poolAOrderId);
			appointmentRequest.setDate(timeslot.toLocalDate());
			appointmentRequest.setTime(LocalTime.of(10, 0));

			try {
				appointmentService.createAppointment(appointmentRequest);
				fail("Expected appointment creation to reject a provider outside the order's pool.");
			} catch (IllegalStateException e) {
				assertTrue(e.getMessage().contains(providerBId.toString()));
				assertTrue(e.getMessage().contains(poolAOrderId.toString()));
				assertTrue(e.getMessage().contains(poolAId.toString()));
			}
			assertNull(epicClient.getLastGetProviderScheduleRequest());
			assertNull(epicClient.getLastScheduleAppointmentRequest());

			EpicDepartment selectedDepartment = institutionService.findEpicDepartmentByProviderIdAndTimeslot(
					providerAId, timeslot, virtualSchedulingDepartmentId).get();
			assertEquals(virtualSchedulingDepartmentId, selectedDepartment.getEpicDepartmentId());

			appointmentRequest.setProviderId(providerAId);
			appointmentRequest.setPatientOrderId(emptyPoolOrderId);
			try {
				appointmentService.createAppointment(appointmentRequest);
				fail("Expected an assigned empty pool to reject booking even with cached availability.");
			} catch (IllegalStateException e) {
				assertTrue(e.getMessage().contains(providerAId.toString()));
				assertTrue(e.getMessage().contains(emptyPoolOrderId.toString()));
				assertTrue(e.getMessage().contains(emptyPoolId.toString()));
			}
			assertNull(epicClient.getLastScheduleAppointmentRequest());

			appointmentRequest.setPatientOrderId(poolAOrderId);
			database.execute("""
					DELETE FROM provider_availability
					WHERE provider_id=? AND date_time=? AND epic_department_id=?
					""", providerAId, timeslot, virtualSchedulingDepartmentId);
			try {
				appointmentService.createAppointment(appointmentRequest);
				fail("Expected a pool-backed order to reject a slot in the wrong scheduling department.");
			} catch (IllegalStateException e) {
				assertTrue(e.getMessage().contains(providerAId.toString()));
				assertTrue(e.getMessage().contains(poolAOrderId.toString()));
				assertTrue(e.getMessage().contains(poolAId.toString()));
				assertTrue(e.getMessage().contains(virtualSchedulingDepartmentId.toString()));
				assertTrue(e.getMessage().contains(timeslot.toString()));
			}
			assertNull(epicClient.getLastGetProviderScheduleRequest());
			database.execute("""
					INSERT INTO provider_availability (
						provider_availability_id, provider_id, date_time, appointment_type_id, epic_department_id
					) VALUES (?, ?, ?, ?, ?)
					""", UUID.randomUUID(), providerAId, timeslot, appointmentTypeId, virtualSchedulingDepartmentId);

			// A real availability race in Epic remains a user-facing validation
			// error even though routing and the cached slot are consistent.
			epicClient.setSlotAvailable(false);
			try {
				appointmentService.createAppointment(appointmentRequest);
				fail("Expected a slot closed in Epic to return the normal availability error.");
			} catch (ValidationException e) {
				assertEquals(true, e.getMetadata().get("appointmentTimeslotUnavailable"));
			}
			assertEquals(selectedDepartment.getDepartmentId(),
					epicClient.getLastGetProviderScheduleRequest().getDepartmentID());
			assertNull(epicClient.getLastScheduleAppointmentRequest());
			assertEquals(Long.valueOf(0), database.queryForObject(
					"SELECT count(*) FROM appointment WHERE patient_order_id IN (?, ?)",
					Long.class, poolAOrderId, emptyPoolOrderId).get());
			epicClient.setSlotAvailable(true);

			UUID appointmentId = appointmentService.createAppointment(appointmentRequest);
			assertTrue(database.queryForObject("""
					SELECT EXISTS (
						SELECT 1
						FROM appointment
						WHERE appointment_id=?
						AND patient_order_id=?
					)
					""", Boolean.class, appointmentId, poolAOrderId).orElse(false));
			assertEquals(selectedDepartment.getDepartmentId(),
					epicClient.getLastGetProviderScheduleRequest().getDepartmentID());
			assertEquals(selectedDepartment.getDepartmentIdType(),
					epicClient.getLastGetProviderScheduleRequest().getDepartmentIDType());
			assertEquals(selectedDepartment.getDepartmentId(),
					epicClient.getLastScheduleAppointmentRequest().getDepartmentID());
			assertEquals(selectedDepartment.getDepartmentIdType(),
					epicClient.getLastScheduleAppointmentRequest().getDepartmentIDType());
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(RecordingEpicClient.class).toInstance(epicClient);
				bind(EnterprisePluginProvider.class).to(RecordingEnterprisePluginProvider.class);
			}
		});
	}

	@Test
	public void picOrdersWithoutPoolsRetainLegacySearchAndBookingBehavior() {
		assertLegacyNonPoolBehavior(InstitutionId.COBALT_IC);
	}

	@Test
	public void easeOrdersWithoutPoolsRetainLegacySearchAndBookingBehavior() {
		assertLegacyNonPoolBehavior(InstitutionId.COBALT_IC_EASE);
	}

	@Test
	public void selfReferralOrdersWithoutPoolsRetainLegacySearchAndBookingBehavior() {
		assertLegacyNonPoolBehavior(InstitutionId.COBALT_IC_SELF_REFERRAL);
	}

	private void assertLegacyNonPoolBehavior(InstitutionId institutionId) {
		RecordingEpicClient epicClient = new RecordingEpicClient(LocalTime.of(10, 0));
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			PatientOrderService patientOrderService = app.getInjector().getInstance(PatientOrderService.class);
			ProviderService providerService = app.getInjector().getInstance(ProviderService.class);
			AppointmentService appointmentService = app.getInjector().getInstance(AppointmentService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			assertTrue(app.getInjector().getInstance(InstitutionService.class)
					.findInstitutionById(institutionId).get().getIntegratedCareEnabled());

			UUID sourceDepartmentId = UUID.randomUUID();
			UUID overrideDepartmentId = UUID.randomUUID();
			UUID chainedDepartmentId = UUID.randomUUID();
			UUID mappedProviderId = UUID.randomUUID();
			UUID unmappedProviderId = UUID.randomUUID();
			UUID otherInstitutionProviderId = UUID.randomUUID();
			UUID appointmentTypeId = UUID.randomUUID();
			UUID patientOrderImportId = UUID.randomUUID();
			UUID clinicId = UUID.randomUUID();
			UUID accountId = UUID.randomUUID();
			LocalDateTime timeslot = LocalDate.now().plusDays(14).atTime(10, 0);

			insertDepartment(database, institutionId, chainedDepartmentId, "TEST-CHAINED", null, null);
			insertDepartment(database, institutionId, overrideDepartmentId, "TEST-OVERRIDE", chainedDepartmentId, null);
			insertDepartment(database, institutionId, sourceDepartmentId, "TEST-SOURCE", overrideDepartmentId, null);
			insertProvider(database, institutionId, mappedProviderId, "Test Mapped Provider");
			insertProvider(database, institutionId, unmappedProviderId, "Test Unmapped Provider");
			insertProvider(database, institutionId == InstitutionId.COBALT_IC_EASE
					? InstitutionId.COBALT_IC : InstitutionId.COBALT_IC_EASE,
					otherInstitutionProviderId, "Test Other Institution Provider");
			database.execute("""
					INSERT INTO appointment_type (
						appointment_type_id, name, duration_in_minutes, scheduling_system_id,
						epic_visit_type_id, epic_visit_type_id_type, visit_type_id
					) VALUES (?, 'Test Legacy Epic Appointment', 60, 'EPIC', '1008', 'INTERNAL', 'INITIAL')
					""", appointmentTypeId);
			database.execute("""
					INSERT INTO provider_appointment_type (provider_id, appointment_type_id, display_order)
					VALUES (?, ?, 1), (?, ?, 1), (?, ?, 1)
					""", mappedProviderId, appointmentTypeId, unmappedProviderId, appointmentTypeId,
					otherInstitutionProviderId, appointmentTypeId);
			database.execute("""
					INSERT INTO provider_epic_department (provider_id, epic_department_id, display_order)
					VALUES (?, ?, 1), (?, ?, 1)
					""", mappedProviderId, overrideDepartmentId, otherInstitutionProviderId, overrideDepartmentId);
			database.execute("""
					INSERT INTO clinic (clinic_id, description, institution_id)
					VALUES (?, 'Test Legacy Clinic', ?)
					""", clinicId, institutionId);
			database.execute("""
					INSERT INTO provider_clinic (provider_clinic_id, provider_id, clinic_id, primary_clinic)
					VALUES (?, ?, ?, TRUE), (?, ?, ?, TRUE), (?, ?, ?, TRUE)
					""", UUID.randomUUID(), mappedProviderId, clinicId,
					UUID.randomUUID(), unmappedProviderId, clinicId,
					UUID.randomUUID(), otherInstitutionProviderId, clinicId);
			database.execute("""
					INSERT INTO patient_order_import (
						patient_order_import_id, patient_order_import_type_id, institution_id, raw_order
					) VALUES (?, 'CSV', ?, '{}')
					""", patientOrderImportId, institutionId);
			UUID ordinaryOrderId = insertOrder(database, institutionId, patientOrderImportId, sourceDepartmentId, null);
			UUID overriddenOrderId = insertOrder(database, institutionId, patientOrderImportId,
					sourceDepartmentId, overrideDepartmentId);
			database.execute("""
					INSERT INTO account (
						account_id, institution_id, role_id, account_source_id, first_name, last_name,
						epic_patient_unique_id, epic_patient_unique_id_type
					) VALUES (?, ?, 'PATIENT', 'EMAIL_PASSWORD', 'Test', 'Patient', ?, 'UID')
					""", accountId, institutionId, "test-patient-" + accountId);
			database.execute("""
					INSERT INTO provider_availability (
						provider_availability_id, provider_id, date_time, appointment_type_id, epic_department_id
					) VALUES (?, ?, ?, ?, ?), (?, ?, ?, ?, ?)
					""", UUID.randomUUID(), mappedProviderId, timeslot, appointmentTypeId, overrideDepartmentId,
					UUID.randomUUID(), unmappedProviderId, timeslot, appointmentTypeId, chainedDepartmentId);
			database.execute("""
					INSERT INTO audit_log_event (audit_log_event_id, description)
					VALUES ('EPIC_APPOINTMENT_CREATE', 'Create an Epic appointment')
					ON CONFLICT (audit_log_event_id) DO NOTHING
					""");
			Account account = accountService.findAccountById(accountId).get();

			// Department overrides are one-hop; an order override is the final
			// department even when that department has its own scheduling override.
			for (UUID orderId : List.of(ordinaryOrderId, overriddenOrderId)) {
				assertFalse(patientOrderService.hasProviderEligibilityPoolForPatientOrderId(orderId));
				assertEquals(overrideDepartmentId, patientOrderService.findSchedulingEpicDepartmentIdForPatientOrderId(orderId));
				assertEquals(Set.of(mappedProviderId, otherInstitutionProviderId),
						patientOrderService.findEligibleProviderIdsForPatientOrderId(orderId));
				List<ProviderFind> normalResults = providerService.findProviders(
						providerFindRequest(institutionId, orderId, null, Set.of()), account, false);
				assertEquals(Set.of(mappedProviderId), providerIds(normalResults));
				assertEquals(Set.of(overrideDepartmentId), availabilityEpicDepartmentIds(normalResults));
				List<ProviderFind> directResults = providerService.findProviders(
						providerFindRequest(institutionId, orderId, unmappedProviderId, Set.of()), account, false);
				assertEquals(Set.of(unmappedProviderId), providerIds(directResults));
				assertTrue(availabilityEpicDepartmentIds(directResults).isEmpty());
				List<ProviderFind> clinicResults = providerService.findProviders(
						providerFindRequest(institutionId, orderId, null, Set.of(clinicId)), account, false);
				assertEquals(Set.of(mappedProviderId, unmappedProviderId), providerIds(clinicResults));
				assertEquals(Set.of(overrideDepartmentId), availabilityEpicDepartmentIds(clinicResults));
				// Candidate selection still prevents another institution's provider
				// from leaking through shared department or clinic mappings.
				assertTrue(providerService.findProviders(providerFindRequest(
						institutionId, orderId, otherInstitutionProviderId, Set.of()), account, false).isEmpty());
			}

			CreateAppointmentRequest appointmentRequest = new CreateAppointmentRequest();
			appointmentRequest.setAccountId(accountId);
			appointmentRequest.setCreatedByAcountId(accountId);
			appointmentRequest.setProviderId(unmappedProviderId);
			appointmentRequest.setAppointmentTypeId(appointmentTypeId);
			appointmentRequest.setDate(timeslot.toLocalDate());
			appointmentRequest.setTime(timeslot.toLocalTime());
			try {
				appointmentService.createAppointment(appointmentRequest);
				fail("Expected non-pool IC booking to continue requiring a patient order.");
			} catch (ValidationException e) {
				assertTrue(e.getFieldErrors().contains(new FieldError("patientOrderId", "Patient Order ID is required.")));
			}

			// Legacy booking does not require a department membership or constrain
			// the cached slot to the order department. Preserve that behavior.
			appointmentRequest.setPatientOrderId(overriddenOrderId);
			UUID unmappedAppointmentId = appointmentService.createAppointment(appointmentRequest);
			assertEquals(overriddenOrderId, database.queryForObject(
					"SELECT patient_order_id FROM appointment WHERE appointment_id=?", UUID.class, unmappedAppointmentId).get());
			String chainedEpicDepartmentId = "TEST-CHAINED-" + chainedDepartmentId;
			assertEquals(chainedEpicDepartmentId, epicClient.getLastGetProviderScheduleRequest().getDepartmentID());
			assertEquals(chainedEpicDepartmentId, epicClient.getLastScheduleAppointmentRequest().getDepartmentID());

			appointmentRequest.setProviderId(mappedProviderId);
			UUID mappedAppointmentId = appointmentService.createAppointment(appointmentRequest);
			assertEquals(overriddenOrderId, database.queryForObject(
					"SELECT patient_order_id FROM appointment WHERE appointment_id=?", UUID.class, mappedAppointmentId).get());
			String overrideEpicDepartmentId = "TEST-OVERRIDE-" + overrideDepartmentId;
			assertEquals(overrideEpicDepartmentId, epicClient.getLastScheduleAppointmentRequest().getDepartmentID());
		}, new AbstractModule() {
			@Override
			protected void configure() {
				bind(RecordingEpicClient.class).toInstance(epicClient);
				bind(EnterprisePluginProvider.class).to(RecordingEnterprisePluginProvider.class);
			}
		});
	}

	private void insertPool(Database database, UUID providerEligibilityPoolId, String name) {
		database.execute("""
				INSERT INTO provider_eligibility_pool (
					provider_eligibility_pool_id,
					institution_id,
					name
				) VALUES (?, ?, ?)
				""", providerEligibilityPoolId, InstitutionId.COBALT_IC, name + " " + providerEligibilityPoolId);
	}

	private String findOtherInstitutionId(Database database) {
		return database.queryForObject("""
				SELECT institution_id
				FROM institution
				WHERE institution_id<>?
				ORDER BY institution_id
				LIMIT 1
				""", String.class, InstitutionId.COBALT_IC).get();
	}

	private void insertDepartment(Database database,
																UUID epicDepartmentId,
																String departmentId,
																UUID schedulingOverrideEpicDepartmentId,
																UUID providerEligibilityPoolId) {
		insertDepartment(database, InstitutionId.COBALT_IC, epicDepartmentId, departmentId,
				schedulingOverrideEpicDepartmentId, providerEligibilityPoolId);
	}

	private void insertDepartment(Database database, InstitutionId institutionId,
																UUID epicDepartmentId, String departmentId,
																UUID schedulingOverrideEpicDepartmentId, UUID providerEligibilityPoolId) {
		database.execute("""
				INSERT INTO epic_department (
					epic_department_id,
					institution_id,
					department_id,
					department_id_type,
					name,
					scheduling_override_epic_department_id,
					provider_eligibility_pool_id
				) VALUES (?, ?, ?, 'INTERNAL', ?, ?, ?)
				""", epicDepartmentId, institutionId,
				departmentId + "-" + epicDepartmentId, departmentId,
				schedulingOverrideEpicDepartmentId, providerEligibilityPoolId);
	}

	private void insertProvider(Database database, UUID providerId, String name) {
		insertProvider(database, InstitutionId.COBALT_IC, providerId, name);
	}

	private void insertProvider(Database database, InstitutionId institutionId, UUID providerId, String name) {
		database.execute("""
				INSERT INTO provider (
					provider_id,
					institution_id,
					name,
					url_name,
					scheduling_system_id,
					videoconference_platform_id,
					epic_provider_id,
					epic_provider_id_type
				) VALUES (?, ?, ?, ?, 'EPIC', 'EXTERNAL', ?, 'INTERNAL')
				""", providerId, institutionId, name,
				"test-provider-" + providerId, providerId.toString());
	}

	private UUID insertOrder(Database database,
											 UUID patientOrderImportId,
											 UUID epicDepartmentId,
											 UUID overrideSchedulingEpicDepartmentId) {
		return insertOrder(database, InstitutionId.COBALT_IC, patientOrderImportId,
				epicDepartmentId, overrideSchedulingEpicDepartmentId);
	}

	private UUID insertOrder(Database database, InstitutionId institutionId,
													 UUID patientOrderImportId, UUID epicDepartmentId,
													 UUID overrideSchedulingEpicDepartmentId) {
		UUID patientOrderId = UUID.randomUUID();

		database.execute("""
				INSERT INTO patient_order (
					patient_order_id,
					patient_order_import_id,
					institution_id,
					patient_last_name,
					patient_first_name,
					patient_mrn,
					patient_unique_id,
					patient_unique_id_type,
					order_id,
					epic_department_id,
					override_scheduling_epic_department_id
				) VALUES (?, ?, ?, 'Patient', 'Test', ?, ?, 'INTERNAL', ?, ?, ?)
				""", patientOrderId, patientOrderImportId, institutionId,
				"test-mrn-" + patientOrderId, "test-patient-" + patientOrderId,
				"test-order-" + patientOrderId, epicDepartmentId, overrideSchedulingEpicDepartmentId);

		return patientOrderId;
	}

	private ProviderFindRequest providerFindRequest(UUID patientOrderId,
																	 UUID providerId,
																	 Set<UUID> clinicIds) {
		return providerFindRequest(InstitutionId.COBALT_IC, patientOrderId, providerId, clinicIds);
	}

	private ProviderFindRequest providerFindRequest(InstitutionId institutionId, UUID patientOrderId,
																	 UUID providerId, Set<UUID> clinicIds) {
		ProviderFindRequest request = new ProviderFindRequest();
		LocalDate date = LocalDate.now().plusDays(14);

		request.setInstitutionId(institutionId);
		request.setPatientOrderId(patientOrderId);
		request.setProviderId(providerId);
		request.setClinicIds(clinicIds);
		request.setStartDate(date);
		request.setEndDate(date);
		request.setStartTime(LocalTime.MIN);
		request.setEndTime(LocalTime.MAX);
		return request;
	}

	private Set<UUID> providerIds(List<ProviderFind> providerFinds) {
		return providerFinds.stream()
				.map(ProviderFind::getProviderId)
				.collect(Collectors.toSet());
	}

	private Set<UUID> availabilityEpicDepartmentIds(List<ProviderFind> providerFinds) {
		return providerFinds.stream()
				.flatMap(providerFind -> providerFind.getDates().stream())
				.flatMap(availabilityDate -> availabilityDate.getTimes().stream())
				.map(availabilityTime -> availabilityTime.getEpicDepartmentId())
				.collect(Collectors.toSet());
	}

	@Singleton
	public static class RecordingEnterprisePluginProvider extends EnterprisePluginProvider {
		@Nonnull
		private final EpicClient epicClient;

		@Inject
		public RecordingEnterprisePluginProvider(@Nonnull Injector injector,
																			@Nonnull Configuration configuration,
																			@Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
																			@Nonnull RecordingEpicClient epicClient) {
			super(injector, configuration, currentContextProvider);
			this.epicClient = epicClient;
		}

		@Nonnull
		@Override
		public EnterprisePlugin enterprisePluginForInstitutionId(@Nonnull InstitutionId institutionId) {
			if (!Set.of(InstitutionId.COBALT_IC, InstitutionId.COBALT_IC_EASE,
					InstitutionId.COBALT_IC_SELF_REFERRAL).contains(institutionId))
				return super.enterprisePluginForInstitutionId(institutionId);

			return new EnterprisePlugin() {
				@Nonnull
				@Override
				public InstitutionId getInstitutionId() {
					return institutionId;
				}

				@Nonnull
				@Override
				public Optional<EpicClient> epicClientForBackendService() {
					return Optional.of(epicClient);
				}
			};
		}
	}

	public static class RecordingEpicClient extends MockEpicClient {
		@Nonnull
		private final LocalTime openTime;
		private boolean slotAvailable = true;
		@Nullable
		private GetProviderScheduleRequest lastGetProviderScheduleRequest;
		@Nullable
		private ScheduleAppointmentWithInsuranceRequest lastScheduleAppointmentRequest;

		public RecordingEpicClient(@Nonnull LocalTime openTime) {
			this.openTime = openTime;
		}

		public void setSlotAvailable(boolean slotAvailable) {
			this.slotAvailable = slotAvailable;
		}

		@Nonnull
		@Override
		public LocalTime parseTimeAmPm(@Nonnull String time) {
			return openTime;
		}

		@Nonnull
		@Override
		public GetProviderScheduleResponse performGetProviderSchedule(@Nonnull GetProviderScheduleRequest request) {
			this.lastGetProviderScheduleRequest = request;

			GetProviderScheduleResponse.ScheduleSlot scheduleSlot = new GetProviderScheduleResponse.ScheduleSlot();
			scheduleSlot.setStartTime("10:00 AM");
			scheduleSlot.setAvailableOpenings(slotAvailable ? "1" : "0");

			GetProviderScheduleResponse response = new GetProviderScheduleResponse();
			response.setScheduleSlots(List.of(scheduleSlot));
			response.setProviderMessages(Collections.emptyList());
			response.setProviderIDs(Collections.emptyList());
			response.setDepartmentIDs(Collections.emptyList());
			return response;
		}

		@Nonnull
		@Override
		public ScheduleAppointmentWithInsuranceResponse performScheduleAppointmentWithInsurance(
					@Nonnull ScheduleAppointmentWithInsuranceRequest request) {
			this.lastScheduleAppointmentRequest = request;

			ScheduleAppointmentWithInsuranceResponse.Appointment appointment =
					new ScheduleAppointmentWithInsuranceResponse.Appointment();
			appointment.setContactIDs(Collections.emptyList());

			ScheduleAppointmentWithInsuranceResponse response = new ScheduleAppointmentWithInsuranceResponse();
			response.setAppointment(appointment);
			return response;
		}

		@Nonnull
		public GetProviderScheduleRequest getLastGetProviderScheduleRequest() {
			return lastGetProviderScheduleRequest;
		}

		@Nonnull
		public ScheduleAppointmentWithInsuranceRequest getLastScheduleAppointmentRequest() {
			return lastScheduleAppointmentRequest;
		}
	}
}
