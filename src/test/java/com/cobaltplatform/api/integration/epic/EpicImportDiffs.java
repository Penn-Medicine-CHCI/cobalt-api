package com.cobaltplatform.api.integration.epic;

import com.cobaltplatform.api.integration.hl7.Hl7Client;
import com.cobaltplatform.api.integration.hl7.Hl7ParsingException;
import com.cobaltplatform.api.integration.hl7.model.event.Hl7GeneralOrderTriggerEvent;
import com.cobaltplatform.api.integration.hl7.model.section.Hl7PatientSection;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedCompositeIdWithCheckDigit;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.soklet.util.LoggingUtils.initializeLogback;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class EpicImportDiffs {
	public static void main(@Nullable String[] args) throws Exception {
		initializeLogback(Paths.get("config/local/logback.xml"));

		final String EPIC_HL7_DIRECTORY_ENV_VAR_NAME = "EPIC_HL7_DIRECTORY";
		final String EPIC_REPORT_CSV_FILE_ENV_VAR_NAME = "EPIC_REPORT_CSV_FILE";
		final String COBALT_REPORT_CSV_FILE_ENV_VAR_NAME = "COBALT_REPORT_CSV_FILE";
		final String DIFF_ANALYSIS_CSV_FILE_ENV_VAR_NAME = "DIFF_ANALYSIS_CSV_FILE";

		String epicHl7DirectoryName = extractEnvVarValue(EPIC_HL7_DIRECTORY_ENV_VAR_NAME);
		String epicReportCsvFilename = extractEnvVarValue(EPIC_REPORT_CSV_FILE_ENV_VAR_NAME);
		String cobaltReportCsvFilename = extractEnvVarValue(COBALT_REPORT_CSV_FILE_ENV_VAR_NAME);
		String diffAnalysisCsvFilename = extractEnvVarValue(DIFF_ANALYSIS_CSV_FILE_ENV_VAR_NAME);

		Path epicHl7Directory = Path.of(epicHl7DirectoryName);

		if (!Files.exists(epicHl7Directory))
			throw new IllegalStateException(format("Directory %s does not exist", epicHl7Directory.toAbsolutePath()));

		if (!Files.isDirectory(epicHl7Directory))
			throw new IllegalStateException(format("Path %s is not a directory", epicHl7Directory.toAbsolutePath()));

		Path epicReportCsvFile = Path.of(epicReportCsvFilename);

		if (!Files.exists(epicReportCsvFile))
			throw new IllegalStateException(format("File %s does not exist", epicReportCsvFile.toAbsolutePath()));

		if (Files.isDirectory(epicReportCsvFile))
			throw new IllegalStateException(format("Path %s is a directory", epicReportCsvFile.toAbsolutePath()));

		// \copy (select po.*, poi.created as patient_order_import_created from v_all_patient_order po, patient_order_import poi where po.patient_order_import_id=poi.patient_order_import_id and po.test_patient_order=false order by poi.created, po.order_date) to 'cobalt-import-report.csv' csv header;
		Path cobaltReportCsvFile = Path.of(cobaltReportCsvFilename);

		if (!Files.exists(cobaltReportCsvFile))
			throw new IllegalStateException(format("Directory %s does not exist", cobaltReportCsvFile.toAbsolutePath()));

		if (Files.isDirectory(cobaltReportCsvFile))
			throw new IllegalStateException(format("Path %s is a directory", cobaltReportCsvFile.toAbsolutePath()));

		Path diffAnalysisCsvFile = Path.of(diffAnalysisCsvFilename);

		if (Files.isDirectory(diffAnalysisCsvFile))
			throw new IllegalStateException(format("Path %s is a directory", diffAnalysisCsvFile.toAbsolutePath()));

		performEpicImportAnalysis(epicHl7Directory, epicReportCsvFile, cobaltReportCsvFile, diffAnalysisCsvFile);
	}

	static void performEpicImportAnalysis(@Nonnull Path epicHl7Directory,
																				@Nonnull Path epicReportCsvFile,
																				@Nonnull Path cobaltReportCsvFile,
																				@Nonnull Path diffAnalysisCsvFile) throws IOException, Hl7ParsingException {
		requireNonNull(epicHl7Directory);
		requireNonNull(epicReportCsvFile);
		requireNonNull(cobaltReportCsvFile);
		requireNonNull(diffAnalysisCsvFile);

		Hl7Client hl7Client = new Hl7Client();
		List<Hl7GeneralOrderTriggerEvent> ignoredHl7Orders = new ArrayList<>();
		Map<String, List<Hl7GeneralOrderTriggerEvent>> ignoredHl7OrdersByOrderId = new HashMap<>();
		Map<String, Set<String>> ignoredHl7OrderIdsByUid = new HashMap<>();
		List<Hl7GeneralOrderTriggerEvent> failedHl7Orders = new ArrayList<>();
		Map<String, List<Hl7GeneralOrderTriggerEvent>> failedHl7OrdersByOrderId = new HashMap<>();
		Map<String, Set<String>> failedHl7OrderIdsByUid = new HashMap<>();

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(epicHl7Directory.resolve("ignored"))) {
			for (Path path : stream) {
				if (!Files.isDirectory(path) && path.getFileName().toString().endsWith(".txt")) {
					byte[] generalOrderHl7 = Files.readAllBytes(path);
					String generalOrderHl7AsString = hl7Client.messageFromBytes(generalOrderHl7);
					Hl7GeneralOrderTriggerEvent generalOrder = hl7Client.parseGeneralOrder(generalOrderHl7AsString);
					ignoredHl7Orders.add(generalOrder);

					String orderId = generalOrder.getOrders().get(0).getCommonOrder().getPlacerOrderNumber().getEntityIdentifier();
					String uid = extractUidFromOrder(generalOrder);
					List<Hl7GeneralOrderTriggerEvent> orders = ignoredHl7OrdersByOrderId.get(orderId);

					if (orders == null) {
						orders = new ArrayList<>();
						orders.add(generalOrder);
						ignoredHl7OrdersByOrderId.put(orderId, orders);
					} else {
						System.err.printf("Encountered duplicate ignored order ID %s in HL7 message\n", orderId);
						orders.add(generalOrder);
					}
				}
			}
		}

		System.out.printf("%d ignored HL7 orders.\n", ignoredHl7Orders.size());

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(epicHl7Directory.resolve("failed"))) {
			for (Path path : stream) {
				if (!Files.isDirectory(path) && path.getFileName().toString().endsWith(".txt")) {
					byte[] generalOrderHl7 = Files.readAllBytes(path);
					String generalOrderHl7AsString = hl7Client.messageFromBytes(generalOrderHl7);
					Hl7GeneralOrderTriggerEvent generalOrder = hl7Client.parseGeneralOrder(generalOrderHl7AsString);
					failedHl7Orders.add(generalOrder);

					String orderId = generalOrder.getOrders().get(0).getCommonOrder().getPlacerOrderNumber().getEntityIdentifier();

					List<Hl7GeneralOrderTriggerEvent> orders = failedHl7OrdersByOrderId.get(orderId);

					if (orders == null) {
						orders = new ArrayList<>();
						orders.add(generalOrder);
						failedHl7OrdersByOrderId.put(orderId, orders);
					} else {
						System.err.printf("Encountered duplicate failed order ID %s in HL7 message\n", orderId);
						orders.add(generalOrder);
					}
				}
			}
		}

		System.out.printf("%d failed HL7 orders.\n", failedHl7Orders.size());

		List<EpicOrder> epicOrders = new ArrayList<>();
		Map<String, EpicOrder> epicOrdersByOrderId = new HashMap<>();

		try (Reader reader = new FileReader(epicReportCsvFile.toFile(), StandardCharsets.UTF_8)) {
			DateTimeFormatter epicOrderDateFormatter = DateTimeFormatter.ofPattern("M/d/yy h:mm", Locale.US);

			for (CSVRecord record : CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
				EpicOrder epicOrder = new EpicOrder();
				epicOrder.setOrderId(trimToNull(record.get("ORDER_PROC_ID")));
				epicOrder.setOrderStatus(trimToNull(record.get("ORDERSTATUS")));
				epicOrder.setOrderDate(LocalDate.parse(trimToNull(record.get("ORDERING_DATE")), epicOrderDateFormatter));
				epicOrder.setEncounterDate(LocalDate.parse(trimToNull(record.get("ENC_DATE")), epicOrderDateFormatter));
				epicOrder.setReferringPractice(trimToNull(record.get("REFERRINGPRACTICE")));

				String reasonForReferral = trimToNull(record.get("REASON(S) FOR REFERRAL. CONCERN FOR:"));

				if (reasonForReferral != null)
					epicOrder.getReasonsForReferral().add(reasonForReferral);

				EpicOrder existingEpicOrder = epicOrdersByOrderId.get(epicOrder.getOrderId());

				if (existingEpicOrder != null) {
					if (reasonForReferral != null)
						existingEpicOrder.getReasonsForReferral().add(reasonForReferral);
				} else {
					epicOrders.add(epicOrder);
					epicOrdersByOrderId.put(epicOrder.getOrderId(), epicOrder);
				}
			}
		}

		sortEpicOrdersInPlace(epicOrders);

		System.out.printf("%d Epic orders\n", epicOrders.size());

		List<CobaltOrder> cobaltOrders = new ArrayList<>();
		Map<String, CobaltOrder> cobaltOrdersByOrderId = new HashMap<>();
		Map<String, List<CobaltOrder>> cobaltOrdersByUid = new HashMap<>();

		try (Reader reader = new FileReader(cobaltReportCsvFile.toFile(), StandardCharsets.UTF_8)) {
			DateTimeFormatter cobaltOrderDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);

			for (CSVRecord record : CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
				CobaltOrder cobaltOrder = new CobaltOrder();
				cobaltOrder.setFirstName(trimToNull(record.get("patient_first_name")));
				cobaltOrder.setLastName(trimToNull(record.get("patient_last_name")));
				cobaltOrder.setMrn(trimToNull(record.get("patient_mrn")));
				cobaltOrder.setUid(trimToNull(record.get("patient_unique_id")));
				cobaltOrder.setOrderId(trimToNull(record.get("order_id")));
				cobaltOrder.setOrderDate(LocalDate.parse(trimToNull(record.get("order_date")), cobaltOrderDateFormatter));

				String episodeClosedAtAsString = trimToNull(record.get("episode_closed_at"));
				Instant episodeClosedAt = episodeClosedAtAsString == null ? null : convertStringToInstant(episodeClosedAtAsString);

				if (episodeClosedAt != null) {
					LocalDateTime episodeClosedAtAsLocalDateTime = LocalDateTime.ofInstant(episodeClosedAt, ZoneId.of("America/New_York"));
					cobaltOrder.setEpisodeClosedAt(episodeClosedAtAsLocalDateTime);
				}

				String assessmentsCompletedAtAsString = trimToNull(record.get("most_recent_screening_session_completed_at"));
				Instant assessmentsCompletedAt = assessmentsCompletedAtAsString == null ? null : convertStringToInstant(assessmentsCompletedAtAsString);

				if (assessmentsCompletedAt != null) {
					LocalDateTime assessmentsCompletedAtAsLocalDateTime = LocalDateTime.ofInstant(assessmentsCompletedAt, ZoneId.of("America/New_York"));
					cobaltOrder.setAssessmentsCompletedAt(assessmentsCompletedAtAsLocalDateTime);
				}

				String importedAtAsString = trimToNull(record.get("patient_order_import_created"));
				Instant importedAt = importedAtAsString == null ? null : convertStringToInstant(importedAtAsString);

				if (importedAt != null) {
					LocalDateTime importedAtAsLocalDateTime = LocalDateTime.ofInstant(importedAt, ZoneId.of("America/New_York"));
					cobaltOrder.setImportedAt(importedAtAsLocalDateTime);
				}

				cobaltOrder.setMostRecentScreeningSessionCreatedByAccountRoleId(trimToNull(record.get("most_recent_screening_session_created_by_account_role_id")));
				cobaltOrder.setMostRecentScreeningSessionCreatedByAccountFirstName(trimToNull(record.get("most_recent_screening_session_created_by_account_first_name")));
				cobaltOrder.setMostRecentScreeningSessionCreatedByAccountLastName(trimToNull(record.get("most_recent_screening_session_created_by_account_last_name")));

				cobaltOrder.setPanelAccountFirstName(trimToNull(record.get("panel_account_first_name")));
				cobaltOrder.setPanelAccountLastName(trimToNull(record.get("panel_account_last_name")));

				cobaltOrder.setCobaltReferenceNumber(trimToNull(record.get("reference_number")));

				cobaltOrders.add(cobaltOrder);

				if (cobaltOrdersByOrderId.containsKey(cobaltOrder.getOrderId()))
					System.err.printf("WARNING: Duplicate Cobalt order ID %s\n", cobaltOrder.getOrderId());

				cobaltOrdersByOrderId.put(cobaltOrder.getOrderId(), cobaltOrder);

				if (cobaltOrder.getUid() == null)
					throw new IllegalStateException(format("Missing UID for Cobalt order ID %s", cobaltOrder.getOrderId()));

				List<CobaltOrder> cobaltOrdersForUid = cobaltOrdersByUid.get(cobaltOrder.getUid());

				if (cobaltOrdersForUid == null) {
					cobaltOrdersForUid = new ArrayList<>();
					cobaltOrdersByUid.put(cobaltOrder.getUid(), cobaltOrdersForUid);
				}

				cobaltOrdersForUid.add(cobaltOrder);
			}
		}

		sortCobaltOrdersInPlace(cobaltOrders);

		System.out.printf("%d Cobalt orders\n", cobaltOrders.size());

		final List<String> DIFF_ANALYSIS_CSV_HEADERS = List.of(
				"Order ID",
				"Epic Order Date",
				"Epic Order Status",
				"Epic Referring Practice",
				"Order Received By Cobalt?",
				"Order Successfully Imported Into Cobalt?",
				"Order Deliberately Discarded By Cobalt?",
				"Notes"
		);

		try (Writer writer = new FileWriter(diffAnalysisCsvFile.toFile(), StandardCharsets.UTF_8);
				 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(DIFF_ANALYSIS_CSV_HEADERS.toArray(new String[0])))) {
			for (EpicOrder epicOrder : epicOrders) {
				List<String> recordElements = new ArrayList<>();

				recordElements.add(epicOrder.getOrderId().toString());
				recordElements.add(epicOrder.getOrderDate().toString());
				recordElements.add(epicOrder.getOrderStatus());
				recordElements.add(epicOrder.getReferringPractice());

				boolean orderImportedIntoCobalt = false;
				CobaltOrder cobaltOrder = cobaltOrdersByOrderId.get(epicOrder.getOrderId());

				if (cobaltOrder != null)
					orderImportedIntoCobalt = true;

				boolean orderDeliberatelyDiscardedByCobalt = false;

				if (failedHl7OrdersByOrderId.containsKey(epicOrder.getOrderId()))
					orderDeliberatelyDiscardedByCobalt = true;

				boolean orderReceivedByCobalt = orderImportedIntoCobalt || orderDeliberatelyDiscardedByCobalt;

				recordElements.add(orderReceivedByCobalt ? "Yes" : "No");
				recordElements.add(orderImportedIntoCobalt ? "Yes" : "No");
				recordElements.add(orderDeliberatelyDiscardedByCobalt ? "Yes" : "No");

				String notes = null;

				if (!orderReceivedByCobalt) {
					notes = format("Cobalt never received an HL7 message for Order ID %s.", epicOrder.getOrderId());
				} else if (orderReceivedByCobalt && !orderImportedIntoCobalt && orderDeliberatelyDiscardedByCobalt) {
					notes = "TODO: display open order ID for this patient that would have prevented this order from being imported.";

				} else if (orderReceivedByCobalt && orderImportedIntoCobalt && orderDeliberatelyDiscardedByCobalt) {
					notes = format("Cobalt received duplicate HL7 messages for Order ID %s.  One was imported and other[s] were discarded.", epicOrder.getOrderId());
				}

				recordElements.add(notes);

				csvPrinter.printRecord(recordElements.toArray(new Object[0]));
			}

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Nonnull
	private static String extractUidFromOrder(@Nonnull Hl7GeneralOrderTriggerEvent generalOrderTriggerEvent) {
		Hl7PatientSection patient = generalOrderTriggerEvent.getPatient();
		Hl7ExtendedCompositeIdWithCheckDigit patientUniqueIdentifier = patient.getPatientIdentification().getPatientIdentifierList().stream()
				.filter(patientIdentifier -> {
					return Objects.equals("UID", patientIdentifier.getIdentifierTypeCode());
				})
				.findFirst()
				.orElse(null);

		if (patientUniqueIdentifier == null || patientUniqueIdentifier.getIdNumber() == null)
			throw new IllegalStateException("HL7 message missing patient identifier");

		return patientUniqueIdentifier.getIdNumber();
	}

	// e.g. 2024-03-26 19:23:31.446265+00 to 2024-03-26T19:23:31Z
	@Nonnull
	private static Instant convertStringToInstant(@Nullable String instantAsString) {
		requireNonNull(instantAsString);

		String rewrittenInstantAsString = (instantAsString.substring(0, instantAsString.indexOf(".")) + "Z").replace(" ", "T");
		return DateTimeFormatter.ISO_INSTANT.parse(rewrittenInstantAsString, Instant::from);
	}

	private static void sortEpicOrdersInPlace(@Nonnull List<EpicOrder> orders) {
		requireNonNull(orders);

		Collections.sort(orders, Comparator.comparing((EpicOrder order) -> order.getOrderDate())
				.thenComparing(order -> order.getEncounterDate())
				.thenComparing(order -> order.getOrderId()));
	}

	private static void sortCobaltOrdersInPlace(@Nonnull List<CobaltOrder> orders) {
		requireNonNull(orders);

		Collections.sort(orders, Comparator.comparing((CobaltOrder order) -> order.getOrderDate())
				.thenComparing(order -> order.getFirstName())
				.thenComparing(order -> order.getLastName())
				.thenComparing(order -> order.getUid()));
	}

	@NotThreadSafe
	protected static class EpicOrder {
		@Nullable
		private String orderId;
		@Nullable
		private String orderStatus;
		@Nullable
		private LocalDate orderDate;
		@Nullable
		private LocalDate encounterDate;
		@Nullable
		private String referringPractice;
		@Nullable
		private List<String> reasonsForReferral = new ArrayList<>();

		@Nullable
		public String getOrderId() {
			return this.orderId;
		}

		public void setOrderId(@Nullable String orderId) {
			this.orderId = orderId;
		}

		@Nullable
		public String getOrderStatus() {
			return this.orderStatus;
		}

		public void setOrderStatus(@Nullable String orderStatus) {
			this.orderStatus = orderStatus;
		}

		@Nullable
		public LocalDate getOrderDate() {
			return this.orderDate;
		}

		public void setOrderDate(@Nullable LocalDate orderDate) {
			this.orderDate = orderDate;
		}

		@Nullable
		public LocalDate getEncounterDate() {
			return this.encounterDate;
		}

		public void setEncounterDate(@Nullable LocalDate encounterDate) {
			this.encounterDate = encounterDate;
		}

		@Nullable
		public String getReferringPractice() {
			return this.referringPractice;
		}

		public void setReferringPractice(@Nullable String referringPractice) {
			this.referringPractice = referringPractice;
		}

		@Nullable
		public List<String> getReasonsForReferral() {
			return this.reasonsForReferral;
		}

		public void setReasonsForReferral(@Nullable List<String> reasonsForReferral) {
			this.reasonsForReferral = reasonsForReferral;
		}
	}

	@NotThreadSafe
	protected static class CobaltOrder {
		@Nullable
		private String firstName;
		@Nullable
		private String lastName;
		@Nullable
		private String mrn;
		@Nullable
		private String uid;
		@Nullable
		private String orderId;
		@Nullable
		private LocalDate orderDate;
		@Nullable
		private LocalDateTime episodeClosedAt;
		@Nullable
		private LocalDateTime assessmentsCompletedAt;
		@Nullable
		private LocalDateTime importedAt;
		@Nullable
		private String mostRecentScreeningSessionCreatedByAccountRoleId;
		@Nullable
		private String mostRecentScreeningSessionCreatedByAccountFirstName;
		@Nullable
		private String mostRecentScreeningSessionCreatedByAccountLastName;
		@Nullable
		private String panelAccountFirstName;
		@Nullable
		private String panelAccountLastName;
		@Nullable
		private String cobaltReferenceNumber;

		@Override
		public String toString() {
			return format("%s %s: MRN %s, UID: %s, Order ID: %s, Order Date: %s", firstName, lastName, mrn, uid, orderId, orderDate);
		}

		@Nullable
		public String getFirstName() {
			return this.firstName;
		}

		public void setFirstName(@Nullable String firstName) {
			this.firstName = firstName;
		}

		@Nullable
		public String getLastName() {
			return this.lastName;
		}

		public void setLastName(@Nullable String lastName) {
			this.lastName = lastName;
		}

		@Nullable
		public String getMrn() {
			return this.mrn;
		}

		public void setMrn(@Nullable String mrn) {
			this.mrn = mrn;
		}

		@Nullable
		public String getUid() {
			return this.uid;
		}

		public void setUid(@Nullable String uid) {
			this.uid = uid;
		}

		@Nullable
		public String getOrderId() {
			return this.orderId;
		}

		public void setOrderId(@Nullable String orderId) {
			this.orderId = orderId;
		}

		@Nullable
		public LocalDate getOrderDate() {
			return this.orderDate;
		}

		public void setOrderDate(@Nullable LocalDate orderDate) {
			this.orderDate = orderDate;
		}

		@Nullable
		public LocalDateTime getEpisodeClosedAt() {
			return this.episodeClosedAt;
		}

		public void setEpisodeClosedAt(@Nullable LocalDateTime episodeClosedAt) {
			this.episodeClosedAt = episodeClosedAt;
		}

		@Nullable
		public LocalDateTime getAssessmentsCompletedAt() {
			return this.assessmentsCompletedAt;
		}

		public void setAssessmentsCompletedAt(@Nullable LocalDateTime assessmentsCompletedAt) {
			this.assessmentsCompletedAt = assessmentsCompletedAt;
		}

		@Nullable
		public LocalDateTime getImportedAt() {
			return this.importedAt;
		}

		public void setImportedAt(@Nullable LocalDateTime importedAt) {
			this.importedAt = importedAt;
		}

		@Nullable
		public String getMostRecentScreeningSessionCreatedByAccountRoleId() {
			return this.mostRecentScreeningSessionCreatedByAccountRoleId;
		}

		public void setMostRecentScreeningSessionCreatedByAccountRoleId(@Nullable String mostRecentScreeningSessionCreatedByAccountRoleId) {
			this.mostRecentScreeningSessionCreatedByAccountRoleId = mostRecentScreeningSessionCreatedByAccountRoleId;
		}

		@Nullable
		public String getMostRecentScreeningSessionCreatedByAccountFirstName() {
			return this.mostRecentScreeningSessionCreatedByAccountFirstName;
		}

		public void setMostRecentScreeningSessionCreatedByAccountFirstName(@Nullable String mostRecentScreeningSessionCreatedByAccountFirstName) {
			this.mostRecentScreeningSessionCreatedByAccountFirstName = mostRecentScreeningSessionCreatedByAccountFirstName;
		}

		@Nullable
		public String getMostRecentScreeningSessionCreatedByAccountLastName() {
			return this.mostRecentScreeningSessionCreatedByAccountLastName;
		}

		public void setMostRecentScreeningSessionCreatedByAccountLastName(@Nullable String mostRecentScreeningSessionCreatedByAccountLastName) {
			this.mostRecentScreeningSessionCreatedByAccountLastName = mostRecentScreeningSessionCreatedByAccountLastName;
		}

		@Nullable
		public String getPanelAccountFirstName() {
			return this.panelAccountFirstName;
		}

		public void setPanelAccountFirstName(@Nullable String panelAccountFirstName) {
			this.panelAccountFirstName = panelAccountFirstName;
		}

		@Nullable
		public String getPanelAccountLastName() {
			return this.panelAccountLastName;
		}

		public void setPanelAccountLastName(@Nullable String panelAccountLastName) {
			this.panelAccountLastName = panelAccountLastName;
		}

		@Nullable
		public String getCobaltReferenceNumber() {
			return this.cobaltReferenceNumber;
		}

		public void setCobaltReferenceNumber(@Nullable String cobaltReferenceNumber) {
			this.cobaltReferenceNumber = cobaltReferenceNumber;
		}
	}

	@Nonnull
	static String extractEnvVarValue(@Nonnull String envVarName) {
		requireNonNull(envVarName);

		String envVarValue = trimToNull(System.getenv(envVarName));

		if (envVarValue == null)
			throw new IllegalStateException(format("You must specify the %s environment variable", envVarName));

		return envVarValue;
	}
}
