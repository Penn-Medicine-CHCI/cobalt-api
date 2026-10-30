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
import com.cobaltplatform.api.integration.hl7.Hl7Client;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderImportRequest;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.service.PatientOrderImportResult;
import com.cobaltplatform.api.service.PatientOrderHl7ImportTests.ImportFixture;
import com.cobaltplatform.api.service.PatientOrderHl7ImportTests.RecordingErrorReporter;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.cobaltplatform.api.util.db.WritableMaster;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.lokalized.Strings;
import com.pyranid.Database;
import com.pyranid.TransactionalOperation;
import org.junit.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import javax.annotation.Nonnull;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.cobaltplatform.api.service.PatientOrderHl7ImportTests.createFixture;
import static com.cobaltplatform.api.service.PatientOrderHl7ImportTests.testModule;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Exercises the sync entry point with real imports and an in-memory S3 client. */
public class PatientOrderSyncServiceTests {
	private static final String SOURCE_KEY = "To_COBALT_Ord_test.txt";
	private static final String BUCKET = "test-hl7-imports";

	@Test
	public void missingProvidersImportWithoutReporting() {
		withSync(context -> {
			context.s3.put(SOURCE_KEY, context.fixture.request().getHl7GeneralOrderTriggerEventMessage());
			context.sync();
			context.assertArchived("imported/" + SOURCE_KEY);
			assertEquals(1L, context.orderCount());
			assertTrue(context.reporter.messages.isEmpty());
			assertTrue(context.reporter.throwables.isEmpty());
		});
	}

	@Test
	public void reportsValidationFailuresForMissingDiagnoses() {
		withSync(context -> {
			String message = context.fixture.request().getHl7GeneralOrderTriggerEventMessage()
					.replaceAll("(?m)^DG1[^\\r\\n]*", "");
			context.s3.put(SOURCE_KEY, message);
			context.sync();
			context.assertValidationReport("HL7 Order is missing diagnoses.");
			assertEquals(0L, context.orderCount());
		});
	}

	@Test
	public void reportsValidationFailuresForUnparseableMessages() {
		withSync(context -> {
			context.s3.put(SOURCE_KEY, "INVALID HL7 MESSAGE");
			context.sync();
			context.assertValidationReport("Unable to parse HL7 General Order Trigger Event message.");
			assertEquals(0L, context.orderCount());
		});
	}

	@Test
	public void reportsFieldValidationFailuresDuringOrderCreation() {
		withSync(context -> {
			context.invalidPlanName.set(true);
			context.s3.put(SOURCE_KEY, context.fixture.request().getHl7GeneralOrderTriggerEventMessage());
			context.sync();
			context.assertValidationReport("Primary plan name is required.");
			assertEquals(0L, context.orderCount());
		});
	}

	@Test
	public void existingOpenOrderAloneRejectsImportWithoutReporting() {
		withSync(context -> {
			context.createExistingOrder();
			context.s3.put(SOURCE_KEY, context.fixture.request().getHl7GeneralOrderTriggerEventMessage());
			context.sync();
			context.assertArchived("failed/" + SOURCE_KEY);
			assertTrue(context.reporter.messages.isEmpty());
			assertTrue(context.reporter.throwables.isEmpty());
			assertEquals(1L, context.orderCount());
		});
	}

	@Test
	public void existingOpenOrderDoesNotSuppressAdditionalValidationFailures() {
		withSync(context -> {
			context.createExistingOrder();
			context.invalidPlanName.set(true);
			context.s3.put(SOURCE_KEY, context.fixture.request().getHl7GeneralOrderTriggerEventMessage());
			context.sync();
			context.assertValidationReport("Primary plan name is required.");
			assertEquals(1L, context.orderCount());
		});
	}

	@Test
	public void unexpectedImportExceptionsContinueToBeReported() {
		withSync(context -> {
			String message = context.fixture.request().getHl7GeneralOrderTriggerEventMessage().replace("19800101", "");
			context.s3.put(SOURCE_KEY, message);
			context.sync();
			context.assertArchived("failed/" + SOURCE_KEY);
			assertEquals(1, context.reporter.throwables.size());
			assertTrue(context.reporter.throwables.get(0) instanceof NullPointerException);
			assertTrue(context.reporter.messages.isEmpty());
			assertEquals(0L, context.orderCount());
		});
	}

	private void withSync(Consumer<SyncContext> test) {
		RecordingErrorReporter reporter = new RecordingErrorReporter();
		RecordingS3 s3 = new RecordingS3();
		AtomicBoolean invalidPlanName = new AtomicBoolean();
		IntegrationTestExecutor.runTransactionallyAndForceRollback(app -> {
			ImportFixture fixture = createFixture(app, "", "");
			fixture.institution().setIntegratedCareEnabled(true);
			fixture.institution().setIntegratedCareOrderImportBucketName(BUCKET);
			reporter.messages.clear();
			reporter.throwables.clear();
			test.accept(new SyncContext(app, fixture, reporter, s3, invalidPlanName));
		}, testModule(reporter, invalidPlanName), new AbstractModule() {
			@Provides
			@Singleton
			PatientOrderSyncService provideSyncService(Injector injector, Hl7Client hl7Client,
					DatabaseProvider databaseProvider, Configuration configuration, Strings strings) {
				Database database = databaseProvider.getWritableMasterDatabase();
				// Production sync starts its own transaction. Join this test's rollback-only
				// transaction so existing fixtures are visible and no test imports are committed.
				Database participatingDatabase = new Database(Database.forDataSource(
						injector.getInstance(Key.get(DataSource.class, WritableMaster.class)))) {
					@Override
					public void transaction(TransactionalOperation operation) {
						database.participate(database.currentTransaction().get(), operation);
					}
				};
				return new PatientOrderSyncService(injector.getProvider(PatientOrderSyncService.BackgroundSyncTask.class),
						injector.getProvider(InstitutionService.class), injector.getProvider(AccountService.class),
						injector.getProvider(PatientOrderService.class), injector.getProvider(SystemService.class),
						hl7Client, databaseProvider, reporter, configuration, strings) {
					@Nonnull
					@Override
					protected S3Client getS3Client() {
						return s3.client;
					}

					@Nonnull
					@Override
					protected Database getDatabase() {
						return participatingDatabase;
					}
				};
			}
		});
	}

	private static class SyncContext {
		private final App app;
		private final ImportFixture fixture;
		private final RecordingErrorReporter reporter;
		private final RecordingS3 s3;
		private final AtomicBoolean invalidPlanName;

		SyncContext(App app, ImportFixture fixture, RecordingErrorReporter reporter,
				RecordingS3 s3, AtomicBoolean invalidPlanName) {
			this.app = app;
			this.fixture = fixture;
			this.reporter = reporter;
			this.s3 = s3;
			this.invalidPlanName = invalidPlanName;
		}

		void sync() {
			app.getInjector().getInstance(PatientOrderSyncService.class).syncPatientOrdersForInstitution(fixture.institution());
		}

		void createExistingOrder() {
			CreatePatientOrderImportRequest existing = new CreatePatientOrderImportRequest();
			existing.setInstitutionId(fixture.request().getInstitutionId());
			existing.setAccountId(fixture.request().getAccountId());
			existing.setPatientOrderImportTypeId(fixture.request().getPatientOrderImportTypeId());
			// Keep the same patient and order ID, but use a different message checksum.
			existing.setHl7GeneralOrderTriggerEventMessage(fixture.request().getHl7GeneralOrderTriggerEventMessage()
					+ "\r\nNTE|3||Existing order fixture " + UUID.randomUUID());
			PatientOrderService patientOrderService = app.getInjector().getInstance(PatientOrderService.class);
			PatientOrderImportResult result = patientOrderService.createPatientOrderImport(existing);
			PatientOrder existingOrder = patientOrderService.findPatientOrderById(result.getPatientOrderIds().get(0)).get();
			assertEquals(1L, orderCount());
			assertTrue(patientOrderService.findOpenPatientOrderByMrnAndInstitutionId(
					existingOrder.getPatientMrn(), fixture.institution().getInstitutionId()).isPresent());
			reporter.messages.clear();
			reporter.throwables.clear();
		}

		long orderCount() {
			return app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase().queryForObject(
					"SELECT COUNT(*) FROM patient_order WHERE institution_id=? AND order_id=?",
					Long.class, fixture.institution().getInstitutionId(), fixture.orderId()).get();
		}

		void assertArchived(String destination) {
			assertEquals(List.of(destination), s3.destinations);
			assertEquals(List.of(SOURCE_KEY), s3.deleted);
		}

		void assertValidationReport(String detail) {
			assertArchived("failed/" + SOURCE_KEY);
			assertEquals(1, reporter.messages.size());
			String message = reporter.messages.get(0);
			assertTrue(message.contains("HL7 order import failed validation"));
			assertTrue(message.contains("Institution ID: " + fixture.institution().getInstitutionId()));
			assertTrue(message.contains("source: " + BUCKET + "/" + SOURCE_KEY));
			assertTrue(message.contains(detail));
			assertTrue(reporter.throwables.isEmpty());
		}
	}

	private static class RecordingS3 {
		final Map<String, byte[]> objects = new LinkedHashMap<>();
		final List<String> destinations = new ArrayList<>();
		final List<String> deleted = new ArrayList<>();
		final S3Client client = (S3Client) Proxy.newProxyInstance(S3Client.class.getClassLoader(),
				new Class<?>[]{S3Client.class}, (proxy, method, args) -> {
					switch (method.getName()) {
						case "listObjectsV2Paginator":
							return new ListObjectsV2Iterable((S3Client) proxy, (ListObjectsV2Request) args[0]);
						case "listObjectsV2":
							ListObjectsV2Request request = (ListObjectsV2Request) args[0];
							return ListObjectsV2Response.builder().isTruncated(false).contents(objects.keySet().stream()
									.filter(key -> key.startsWith(request.prefix()))
									.map(key -> S3Object.builder().key(key).build()).toList()).build();
						case "getObject":
							return new ResponseInputStream<>(GetObjectResponse.builder().build(),
									AbortableInputStream.create(new ByteArrayInputStream(objects.get(((GetObjectRequest) args[0]).key()))));
						case "copyObject":
							CopyObjectRequest copy = (CopyObjectRequest) args[0];
							destinations.add(copy.destinationKey());
							objects.put(copy.destinationKey(), objects.get(copy.sourceKey()));
							return CopyObjectResponse.builder().build();
						case "deleteObject":
							String key = ((DeleteObjectRequest) args[0]).key();
							deleted.add(key);
							objects.remove(key);
							return DeleteObjectResponse.builder().build();
						case "serviceName":
							return "s3";
						case "close":
							return null;
						default:
							throw new AssertionError("Unexpected S3 call: " + method.getName());
					}
				});

		void put(String key, String message) {
			objects.put(key, message.getBytes(StandardCharsets.UTF_8));
		}
	}
}
