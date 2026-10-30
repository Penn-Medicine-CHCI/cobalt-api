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

import com.cobaltplatform.api.App;
import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.error.ConsoleErrorReporter;
import com.cobaltplatform.api.error.ErrorReporter;
import com.cobaltplatform.api.integration.enterprise.CobaltIcEnterprisePlugin;
import com.cobaltplatform.api.integration.hl7.model.section.Hl7OrderSection;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderImportRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderRequest;
import com.cobaltplatform.api.model.db.EpicDepartment;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.PatientOrderImport;
import com.cobaltplatform.api.model.db.PatientOrderImportType.PatientOrderImportTypeId;
import com.cobaltplatform.api.model.service.PatientOrderImportResult;
import com.cobaltplatform.api.util.AwsSecretManagerClient;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.lokalized.Strings;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PatientOrderHl7ImportTests {
	private static final String OBR_PROVIDER = "OBR-ID^EXAMPLE^ALICE^ANN";
	private static final String ORC_PROVIDER = "ORC-ID^SAMPLE^BOB^BEN";

	@Test
	public void importsOrderingProviderFromObr16() {
		assertImportedProvider(OBR_PROVIDER, "", "EXAMPLE", "ALICE", "ANN");
	}

	@Test
	public void preservesObr16PrecedenceWhenOrc12AlsoHasAProvider() {
		assertImportedProvider(OBR_PROVIDER, ORC_PROVIDER, "EXAMPLE", "ALICE", "ANN");
	}

	@Test
	public void fallsBackToOrc12WhenObr16IsMissing() {
		assertImportedProvider("", ORC_PROVIDER, "SAMPLE", "BOB", "BEN");
	}

	@Test
	public void importsMissingProvidersWithoutReportingOrUsingOrc10() {
		assertImportedProvider("", "", null, null, null);
	}

	@Test
	public void fallsBackToOrc12WhenObr16ContainsOnlyEmptyComponents() {
		assertImportedProvider("^^^", ORC_PROVIDER, "SAMPLE", "BOB", "BEN");
	}

	@Test
	public void importsWithoutReportingWhenBothProviderFieldsContainOnlyEmptyComponents() {
		assertImportedProvider("^^^", "^^^", null, null, null);
	}

	@Test
	public void handlesAnOrderingProviderWithoutAFamilyName() {
		assertImportedProvider("", "ORC-ID^^BOB^BEN", null, "BOB", "BEN");
	}

	@Test
	public void skipsEmptyProviderRepetitions() {
		assertImportedProvider("~" + OBR_PROVIDER, ORC_PROVIDER, "EXAMPLE", "ALICE", "ANN");
	}

	private void assertImportedProvider(String obrProvider, String orcProvider, String lastName,
			String firstName, String middleName) {
		RecordingErrorReporter errorReporter = new RecordingErrorReporter();

		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			PatientOrderService patientOrderService = app.getInjector().getInstance(PatientOrderService.class);
			ImportFixture fixture = createFixture(app, obrProvider, orcProvider);
			CreatePatientOrderImportRequest request = fixture.request();
			String orderId = fixture.orderId();
			String message = request.getHl7GeneralOrderTriggerEventMessage();
			errorReporter.messages.clear();
			errorReporter.throwables.clear();

			PatientOrderImportResult result = patientOrderService.createPatientOrderImport(request);
			List<PatientOrder> orders = patientOrderService.findPatientOrdersByPatientOrderImportId(result.getPatientOrderImportId());
			assertEquals(1, orders.size());
			PatientOrder order = orders.get(0);
			assertEquals(List.of(order.getPatientOrderId()), result.getPatientOrderIds());
			assertEquals(orderId, order.getOrderId());
			assertEquals(lastName, order.getOrderingProviderLastName());
			assertEquals(firstName, order.getOrderingProviderFirstName());
			assertEquals(middleName, order.getOrderingProviderMiddleName());

			PatientOrderImport patientOrderImport = patientOrderService.findPatientOrderImportById(result.getPatientOrderImportId()).get();
			assertEquals(message, patientOrderImport.getRawOrder());
			assertEquals("test-hl7-order.txt", patientOrderImport.getRawOrderFilename());
			assertTrue(errorReporter.throwables.toString(), errorReporter.throwables.isEmpty());
			assertTrue(errorReporter.messages.isEmpty());
		}, testModule(errorReporter, new AtomicBoolean()));
	}

	static ImportFixture createFixture(App app, String obrProvider, String orcProvider) {
		InstitutionId institutionId = InstitutionId.COBALT_IC;
		InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
		AccountService accountService = app.getInjector().getInstance(AccountService.class);
		Institution institution = institutionService.findInstitutionById(institutionId).get();
		EpicDepartment department = institutionService.findEpicDepartmentsByInstitutionId(institutionId).stream()
				.filter(EpicDepartment::getPatientOrderAutomaticImportEnabled)
				.findFirst().get();
		String orderId = "TEST-HL7-" + UUID.randomUUID();
		String patientId = UUID.randomUUID().toString();
		String message = String.join("\r\n",
				"MSH|^~\\&|TEST|TEST|||20261006142847||ORM^O01|" + UUID.randomUUID() + "|P|2.5.1",
				segment("PID", 11, Map.of(
						1, "1",
						3, patientId + "^^^TEST^" + institution.getEpicPatientMrnTypeName()
								+ "~" + patientId + "^^^TEST^" + institution.getEpicPatientUniqueIdType(),
						5, "PATIENT^TEST",
						7, "19800101",
						8, "F",
						11, "123 TEST STREET^^PHILADELPHIA^PA^19104^USA")),
				"PV1|1|O",
				segment("ORC", 12, Map.of(
						1, "NW", 2, orderId + "^TEST", 9, "20261006142842",
						10, "ENTERER-ID^ENTERER^EVE", 12, orcProvider)),
				segment("OBR", 16, Map.of(
						1, "1", 2, orderId + "^TEST", 4, "TEST^TEST CONSULT", 16, obrProvider)),
				"NTE|1||Referring Practice:->" + department.getName(),
				"NTE|2||Reason(s) for referral:->Mood or depression symptoms",
				"DG1|1|ICD-10-CM|F41.8^Other specified anxiety disorders^ICD-10-CM|Other specified anxiety disorders");

		CreatePatientOrderImportRequest request = new CreatePatientOrderImportRequest();
		request.setInstitutionId(institutionId);
		request.setAccountId(accountService.findAdminAccountsForInstitution(InstitutionId.COBALT).get(0).getAccountId());
		request.setPatientOrderImportTypeId(PatientOrderImportTypeId.HL7_MESSAGE);
		request.setHl7GeneralOrderTriggerEventMessage(message);
		request.setFilename("test-hl7-order.txt");
		return new ImportFixture(request, institution, orderId);
	}

	static Module testModule(RecordingErrorReporter errorReporter, AtomicBoolean invalidPlanName) {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(ErrorReporter.class).toInstance(errorReporter);
			}

			@Provides
			@Singleton
			CobaltIcEnterprisePlugin provideTestPlugin(InstitutionService institutionService,
					AwsSecretManagerClient awsSecretManagerClient, Configuration configuration,
					ScreeningService screeningService, PatientOrderService patientOrderService, Strings strings) {
				return new CobaltIcEnterprisePlugin(institutionService, awsSecretManagerClient, configuration,
						screeningService, patientOrderService, errorReporter, strings) {
					@Override
					public void applyCustomizationsToCreatePatientOrderRequestForHl7Order(
							@Nonnull CreatePatientOrderRequest request, @Nonnull Hl7OrderSection order) {
						super.applyCustomizationsToCreatePatientOrderRequestForHl7Order(request, order);
						// Exercise persistence without fetching demographics or sending patient messages.
						request.setTestPatientOrder(true);
						if (invalidPlanName.get())
							request.setPrimaryPlanName(null);
					}

					@Override
					public Boolean shouldSendAutomatedWelcomeMessageForPatientOrder(@Nonnull PatientOrder order) {
						return false;
					}
				};
			}
		};
	}

	static class ImportFixture {
		private final CreatePatientOrderImportRequest request;
		private final Institution institution;
		private final String orderId;

		ImportFixture(CreatePatientOrderImportRequest request, Institution institution, String orderId) {
			this.request = request;
			this.institution = institution;
			this.orderId = orderId;
		}

		CreatePatientOrderImportRequest request() {
			return request;
		}

		Institution institution() {
			return institution;
		}

		String orderId() {
			return orderId;
		}
	}

	private static String segment(String name, int fieldCount, Map<Integer, String> values) {
		String[] fields = new String[fieldCount + 1];
		Arrays.fill(fields, "");
		fields[0] = name;
		values.forEach((index, value) -> fields[index] = value);
		return String.join("|", fields);
	}

	static class RecordingErrorReporter extends ConsoleErrorReporter {
		final List<String> messages = new CopyOnWriteArrayList<>();
		final List<Throwable> throwables = new CopyOnWriteArrayList<>();

		@Override
		public void report(@Nonnull String message) {
			messages.add(message);
		}

		@Override
		protected void reportNormalizedThrowable(@Nonnull Throwable throwable) {
			throwables.add(throwable);
		}
	}
}
