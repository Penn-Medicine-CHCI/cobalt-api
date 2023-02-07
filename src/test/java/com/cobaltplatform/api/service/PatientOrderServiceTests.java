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
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.PatientOrderDiagnosis;
import com.cobaltplatform.api.model.db.PatientOrderImport;
import com.cobaltplatform.api.model.db.PatientOrderImportType.PatientOrderImportTypeId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

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

			// Create a patient record, which has patient record SSO attributes that contain a UID that matches one of the imported orders...
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
		});
	}

	@Nonnull
	protected Gson createGson() {
		GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();

		gsonBuilder.registerTypeAdapter(LocalDate.class, new JsonDeserializer<LocalDate>() {
			@Override
			@Nullable
			public LocalDate deserialize(@Nullable JsonElement json,
																	 @Nonnull Type type,
																	 @Nonnull JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
				requireNonNull(type);
				requireNonNull(jsonDeserializationContext);

				if (json == null)
					return null;

				JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();

				if (jsonPrimitive.isString()) {
					String string = trimToNull(json.getAsString());
					return string == null ? null : LocalDate.parse(string);
				}

				throw new IllegalArgumentException(format("Unable to convert JSON value '%s' to %s", json, type));
			}
		});

		gsonBuilder.registerTypeAdapter(LocalDate.class, new JsonSerializer<LocalDate>() {
			@Override
			@Nullable
			public JsonElement serialize(@Nullable LocalDate localDate,
																	 @Nonnull Type type,
																	 @Nonnull JsonSerializationContext jsonSerializationContext) {
				requireNonNull(type);
				requireNonNull(jsonSerializationContext);

				return localDate == null ? null : new JsonPrimitive(localDate.toString());
			}
		});

		return gsonBuilder.create();
	}

}
