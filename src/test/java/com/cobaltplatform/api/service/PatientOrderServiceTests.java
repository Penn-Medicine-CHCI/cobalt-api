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
import com.cobaltplatform.api.model.api.request.CreateAccountRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderImportRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderNoteRequest;
import com.cobaltplatform.api.model.api.request.DeletePatientOrderNoteRequest;
import com.cobaltplatform.api.model.api.request.FindPatientOrdersRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderNoteRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.PatientOrderDiagnosis;
import com.cobaltplatform.api.model.db.PatientOrderImport;
import com.cobaltplatform.api.model.db.PatientOrderImportType.PatientOrderImportTypeId;
import com.cobaltplatform.api.model.db.PatientOrderNote;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.service.FindResult;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PatientOrderServiceTests {
	@Test
	public void patientOrderImport() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			InstitutionId institutionId = InstitutionId.COBALT_IC;
			PatientOrderService patientOrderService = app.getInjector().getInstance(PatientOrderService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);

			String csvContent = Files.readString(Path.of("resources/test/ic-order-report.csv"), StandardCharsets.UTF_8);
			String epicPatientJson = Files.readString(Path.of("resources/test/epic-patient.json"), StandardCharsets.UTF_8);

			UUID patientOrderImportId = patientOrderService.createPatientOrderImport(new CreatePatientOrderImportRequest() {{
				setCsvContent(csvContent);
				setInstitutionId(institutionId);
				setPatientOrderImportTypeId(PatientOrderImportTypeId.CSV);
				setAccountId(account.getAccountId());
			}});

			PatientOrderImport patientOrderImport = patientOrderService.findPatientOrderImportById(patientOrderImportId).orElse(null);

			Assert.assertNotNull("Unable to read back patient order import after creating it", patientOrderImport);

			List<PatientOrder> patientOrders = patientOrderService.findPatientOrdersByPatientOrderImportId(patientOrderImportId);

			Assert.assertEquals("Unexpected number of patient orders created for import", 8, patientOrders.size());

			for (PatientOrder patientOrder : patientOrders) {
				if (patientOrder.getOrderId().equals("1014")) {
					List<PatientOrderDiagnosis> patientOrderDiagnoses = patientOrderService.findPatientOrderDiagnosesByPatientOrderId(patientOrder.getPatientOrderId());
					Assert.assertEquals("Unexpected number of patient diagnoses for order 1014", 3, patientOrderDiagnoses.size());
				}
			}

			// Simulate MyChart authentication flow -
			// create a patient record, which has patient record SSO attributes that contain a UID that matches one of the imported orders...
			UUID patientAccountId = accountService.createAccount(new CreateAccountRequest() {{
				setEpicPatientId("junk");
				setEpicPatientIdType("junk");
				setAccountSourceId(AccountSourceId.MYCHART);
				setRoleId(RoleId.PATIENT);
				setInstitutionId(institutionId);
				setSsoAttributesAsJson(epicPatientJson);
			}});

			// ...then confirm the patient was automatically associated with the imported order.
			List<PatientOrder> patientOrdersForTestPatientAccount = patientOrderService.findPatientOrdersByAccountId(patientAccountId);

			Assert.assertEquals("Patient was not automatically associated with imported order", 1, patientOrdersForTestPatientAccount.size());

			// Pull back the orders in the system...
			FindResult<PatientOrder> patientOrderFindResult = patientOrderService.findPatientOrders(new FindPatientOrdersRequest() {{
				setInstitutionId(institutionId);
			}});

			Assert.assertEquals("Patient order find didn't return expected results", 8, patientOrderFindResult.getResults().size());
			Assert.assertEquals("Patient order find didn't return expected results", 8L, (long) patientOrderFindResult.getTotalCount());

			// ...pick the first one, and add a note to it.
			PatientOrder patientOrder = patientOrderFindResult.getResults().get(0);

			UUID patientOrderNoteId = patientOrderService.createPatientOrderNote(new CreatePatientOrderNoteRequest() {{
				setPatientOrderId(patientOrder.getPatientOrderId());
				setNote("This is a test note");
				setAccountId(account.getAccountId());
			}});

			PatientOrderNote patientOrderNote = patientOrderService.findPatientOrderNoteById(patientOrderNoteId).get();

			Assert.assertEquals("Patient order note content mismatch after create", "This is a test note", patientOrderNote.getNote());

			patientOrderService.updatePatientOrderNote(new UpdatePatientOrderNoteRequest() {{
				setPatientOrderNoteId(patientOrderNoteId);
				setNote("This is a test note 2");
				setAccountId(account.getAccountId());
			}});

			patientOrderNote = patientOrderService.findPatientOrderNoteById(patientOrderNoteId).get();

			Assert.assertEquals("Patient order note content mismatch after update", "This is a test note 2", patientOrderNote.getNote());

			// Confirm note exists for the order before deleting, and that it's gone after deleting
			List<PatientOrderNote> patientOrderNotes = patientOrderService.findPatientOrderNotesByPatientOrderId(patientOrder.getPatientOrderId());

			Assert.assertEquals("Patient order note was not associated with patient order", 1, patientOrderNotes.size());

			patientOrderService.deletePatientOrderNote(new DeletePatientOrderNoteRequest() {{
				setPatientOrderNoteId(patientOrderNoteId);
				setAccountId(account.getAccountId());
			}});

			patientOrderNotes = patientOrderService.findPatientOrderNotesByPatientOrderId(patientOrder.getPatientOrderId());

			Assert.assertEquals("Patient order note was not deleted correctly", 0, patientOrderNotes.size());
		});
	}
}
