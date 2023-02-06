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
import com.cobaltplatform.api.model.api.request.CreatePatientOrderImportRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.PatientOrderImport;
import com.cobaltplatform.api.model.db.PatientOrderImportType.PatientOrderImportTypeId;
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
			InstitutionId institutionId = InstitutionId.COBALT;
			PatientOrderService patientOrderService = app.getInjector().getInstance(PatientOrderService.class);
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			Account account = accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0);

			String csvContent = Files.readString(Path.of("resources/test/ic-order-report.csv"), StandardCharsets.UTF_8);

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
		});
	}
}
