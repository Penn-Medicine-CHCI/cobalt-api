package com.cobaltplatform.api.integration.epic;

import com.cobaltplatform.api.integration.hl7.Hl7ParsingException;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.soklet.util.LoggingUtils.initializeLogback;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class EpicAssessmentDiffs {
	public static void main(@Nullable String[] args) throws Exception {
		initializeLogback(Paths.get("config/local/logback.xml"));

		final String EPIC_REPORT_CSV_FILE_ENV_VAR_NAME = "EPIC_REPORT_CSV_FILE";
		final String COBALT_REPORT_CSV_FILE_ENV_VAR_NAME = "COBALT_REPORT_CSV_FILE";
		final String DIFF_ANALYSIS_CSV_FILE_ENV_VAR_NAME = "DIFF_ANALYSIS_CSV_FILE";

		String epicReportCsvFilename = extractEnvVarValue(EPIC_REPORT_CSV_FILE_ENV_VAR_NAME);
		String cobaltReportCsvFilename = extractEnvVarValue(COBALT_REPORT_CSV_FILE_ENV_VAR_NAME);
		String diffAnalysisCsvFilename = extractEnvVarValue(DIFF_ANALYSIS_CSV_FILE_ENV_VAR_NAME);

		Path epicReportCsvFile = Path.of(epicReportCsvFilename);

		if (!Files.exists(epicReportCsvFile))
			throw new IllegalStateException(format("File %s does not exist", epicReportCsvFile.toAbsolutePath()));

		if (Files.isDirectory(epicReportCsvFile))
			throw new IllegalStateException(format("Path %s is a directory", epicReportCsvFile.toAbsolutePath()));

		// \copy (select po.*, poi.created as patient_order_import_created from v_all_patient_order po, patient_order_import poi where po.patient_order_import_id=poi.patient_order_import_id and po.test_patient_order=false) to 'cobalt-assessment-report.csv' csv header;
		Path cobaltReportCsvFile = Path.of(cobaltReportCsvFilename);

		if (!Files.exists(cobaltReportCsvFile))
			throw new IllegalStateException(format("Directory %s does not exist", cobaltReportCsvFile.toAbsolutePath()));

		if (Files.isDirectory(cobaltReportCsvFile))
			throw new IllegalStateException(format("Path %s is a directory", cobaltReportCsvFile.toAbsolutePath()));

		Path diffAnalysisCsvFile = Path.of(diffAnalysisCsvFilename);

		if (Files.isDirectory(diffAnalysisCsvFile))
			throw new IllegalStateException(format("Path %s is a directory", diffAnalysisCsvFile.toAbsolutePath()));

		performEpicAssessmentAnalysis(epicReportCsvFile, cobaltReportCsvFile, diffAnalysisCsvFile);
	}

	static void performEpicAssessmentAnalysis(@Nonnull Path epicReportCsvFile,
																						@Nonnull Path cobaltReportCsvFile,
																						@Nonnull Path diffAnalysisCsvFile) throws IOException, Hl7ParsingException {
		requireNonNull(epicReportCsvFile);
		requireNonNull(cobaltReportCsvFile);

		List<DiffableOrder> epicOrders = new ArrayList<>();
		Map<String, DiffableOrder> epicOrdersByOrderId = new HashMap<>();
		Map<String, List<DiffableOrder>> epicOrdersByUid = new HashMap<>();

		try (Reader reader = new FileReader(epicReportCsvFile.toFile(), StandardCharsets.UTF_8)) {
			DateTimeFormatter epicOrderDateFormatter = DateTimeFormatter.ofPattern("M/d/yy", Locale.US);
			DateTimeFormatter epicAssessmentsCompletedAtDateFormatter = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US);

			for (CSVRecord record : CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
				DiffableOrder epicOrder = new DiffableOrder();
				epicOrder.setFirstName(trimToNull(record.get("First Name")));
				epicOrder.setLastName(trimToNull(record.get("Last Name")));
				epicOrder.setMrn(trimToNull(record.get("MRN")));
				epicOrder.setUid(trimToNull(record.get("UID")));
				epicOrder.setOrderId(trimToNull(record.get("Order ID")));
				epicOrder.setOrderDate(LocalDate.parse(trimToNull(record.get("Order Date")), epicOrderDateFormatter));

				String assessmentsCompletedAtAsString = trimToNull(record.get("IC Assessment Date"));

				if (assessmentsCompletedAtAsString != null) {
					LocalDate assessmentsCompletedAt = LocalDate.parse(assessmentsCompletedAtAsString, epicAssessmentsCompletedAtDateFormatter);
					epicOrder.setAssessmentsCompletedAt(LocalDateTime.of(assessmentsCompletedAt, LocalTime.MIN));
				}

				epicOrders.add(epicOrder);

				if (epicOrdersByOrderId.containsKey(epicOrder.getOrderId()))
					throw new IllegalStateException(format("Duplicate Epic order ID %s", epicOrder.getOrderId()));

				epicOrdersByOrderId.put(epicOrder.getOrderId(), epicOrder);

				if (epicOrder.getUid() == null)
					throw new IllegalStateException(format("Missing UID for Epic order ID %s", epicOrder.getOrderId()));

				List<DiffableOrder> epicOrdersForUid = epicOrdersByUid.get(epicOrder.getUid());

				if (epicOrdersForUid == null) {
					epicOrdersForUid = new ArrayList<>();
					epicOrdersByUid.put(epicOrder.getUid(), epicOrdersForUid);
				}

				epicOrdersForUid.add(epicOrder);
			}
		}

		sortOrdersInPlace(epicOrders);

		System.out.printf("%d Epic orders\n", epicOrders.size());

		List<DiffableOrder> cobaltOrders = new ArrayList<>();
		Map<String, DiffableOrder> cobaltOrdersByOrderId = new HashMap<>();
		Map<String, List<DiffableOrder>> cobaltOrdersByUid = new HashMap<>();

		try (Reader reader = new FileReader(cobaltReportCsvFile.toFile(), StandardCharsets.UTF_8)) {
			DateTimeFormatter cobaltOrderDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);

			for (CSVRecord record : CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
				DiffableOrder cobaltOrder = new DiffableOrder();
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

				List<DiffableOrder> cobaltOrdersForUid = cobaltOrdersByUid.get(cobaltOrder.getUid());

				if (cobaltOrdersForUid == null) {
					cobaltOrdersForUid = new ArrayList<>();
					cobaltOrdersByUid.put(cobaltOrder.getUid(), cobaltOrdersForUid);
				}

				cobaltOrdersForUid.add(cobaltOrder);
			}
		}

		sortOrdersInPlace(cobaltOrders);

		System.out.printf("%d Cobalt orders\n", cobaltOrders.size());

		List<DiffAnalysisRow> diffAnalysisRows = new ArrayList<>(epicOrders.size());

		for (DiffableOrder epicOrder : epicOrders) {
			DiffableOrder cobaltOrder = cobaltOrdersByOrderId.get(epicOrder.getOrderId());

			DiffAnalysisRow diffAnalysisRow = new DiffAnalysisRow();
			diffAnalysisRow.setEpicOrderDate(epicOrder.getOrderDate());
			diffAnalysisRow.setOrderId(epicOrder.getOrderId());
			diffAnalysisRow.setFirstName(epicOrder.getFirstName());
			diffAnalysisRow.setLastName(epicOrder.getLastName());
			diffAnalysisRow.setUid(epicOrder.getUid());
			diffAnalysisRow.setMrn(epicOrder.getMrn());
			diffAnalysisRow.setEpicAssessmentsCompletedAt(epicOrder.getAssessmentsCompletedAt() == null ? null : epicOrder.getAssessmentsCompletedAt().toLocalDate());
			diffAnalysisRow.setNotes(new ArrayList<>());

			if (cobaltOrder != null) {
				diffAnalysisRow.setCobaltOrderDate(cobaltOrder.getOrderDate());

				if (!epicOrder.getOrderDate().equals(cobaltOrder.getOrderDate()))
					diffAnalysisRow.getNotes().add(format("Epic Order Date is %s but Cobalt Order Date is %s.", epicOrder.getOrderDate(), cobaltOrder.getOrderDate()));

				diffAnalysisRow.setCobaltAssessmentsCompletedAt(cobaltOrder.getAssessmentsCompletedAt());
				diffAnalysisRow.setOrderExistsInCobalt(true);

				if (cobaltOrder.getMostRecentScreeningSessionCreatedByAccountFirstName() != null)
					diffAnalysisRow.setCobaltAssessmentsCompletedBy(format("%s %s", cobaltOrder.getMostRecentScreeningSessionCreatedByAccountFirstName().toUpperCase(Locale.US), cobaltOrder.getMostRecentScreeningSessionCreatedByAccountLastName().toUpperCase(Locale.US)));

				diffAnalysisRow.setCobaltAssessmentsCompletedByRole(cobaltOrder.getMostRecentScreeningSessionCreatedByAccountRoleId());

				if (cobaltOrder.getPanelAccountFirstName() != null)
					diffAnalysisRow.setCobaltPanelAssignment(format("%s %s", cobaltOrder.getPanelAccountFirstName().toUpperCase(Locale.US), cobaltOrder.getPanelAccountLastName().toUpperCase(Locale.US)));

				diffAnalysisRow.setCobaltReferenceNumber(cobaltOrder.getCobaltReferenceNumber());
			} else {
				diffAnalysisRow.setOrderExistsInCobalt(false);

				List<DiffableOrder> potentialCobaltOrders = cobaltOrdersByUid.get(epicOrder.getUid());

				if (potentialCobaltOrders != null && potentialCobaltOrders.size() > 0) {
					for (DiffableOrder potentialCobaltOrder : potentialCobaltOrders) {
						String otherOrderClosedDescription = potentialCobaltOrder.getEpisodeClosedAt() == null ? "is not yet closed" : format("was closed %s", potentialCobaltOrder.getEpisodeClosedAt());
						String otherOrderNote = format("This patient has another Cobalt order that was imported on %s and %s.", potentialCobaltOrder.getImportedAt(), otherOrderClosedDescription);
						diffAnalysisRow.getNotes().add(otherOrderNote);
					}
				} else {
					diffAnalysisRow.getNotes().add(format("No order for this patient's UID (%s) exists in Cobalt.", epicOrder.getUid()));
				}
			}

			diffAnalysisRows.add(diffAnalysisRow);
		}

		final List<String> DIFF_ANALYSIS_CSV_HEADERS = List.of(
				"Epic Order Date",
				"Cobalt Order Date",
				"Order ID",
				"Patient First Name",
				"Patient Last Name",
				"Patient UID",
				"Patient MRN",
				"Cobalt Reference Number",
				"Epic Assessments Completed At",
				"Cobalt Assessments Completed At",
				"Cobalt Assessments Performed By",
				"Cobalt Assessments Performed By Role",
				"Cobalt Panel Assignment",
				"Notes"
		);

		try (Writer writer = new FileWriter(diffAnalysisCsvFile.toFile(), StandardCharsets.UTF_8);
				 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(DIFF_ANALYSIS_CSV_HEADERS.toArray(new String[0])))) {
			for (DiffAnalysisRow diffAnalysisRow : diffAnalysisRows) {
				List<String> recordElements = new ArrayList<>();

				recordElements.add(diffAnalysisRow.getEpicOrderDate().toString());
				recordElements.add(diffAnalysisRow.getCobaltOrderDate() == null ? null : diffAnalysisRow.getCobaltOrderDate().toString());
				recordElements.add(diffAnalysisRow.getOrderId());
				recordElements.add(diffAnalysisRow.getFirstName().toUpperCase(Locale.US));
				recordElements.add(diffAnalysisRow.getLastName().toUpperCase(Locale.US));
				recordElements.add(diffAnalysisRow.getUid().toUpperCase(Locale.US));
				recordElements.add(diffAnalysisRow.getMrn().toUpperCase(Locale.US));
				recordElements.add(diffAnalysisRow.getCobaltReferenceNumber());
				recordElements.add(diffAnalysisRow.getEpicAssessmentsCompletedAt() == null ? null : diffAnalysisRow.getEpicAssessmentsCompletedAt().toString());
				recordElements.add(diffAnalysisRow.getCobaltAssessmentsCompletedAt() == null ? null : diffAnalysisRow.getCobaltAssessmentsCompletedAt().toString());
				recordElements.add(diffAnalysisRow.getCobaltAssessmentsCompletedBy());
				recordElements.add(diffAnalysisRow.getCobaltAssessmentsCompletedByRole());
				recordElements.add(diffAnalysisRow.getCobaltPanelAssignment());
				recordElements.add(diffAnalysisRow.getNotes().stream().collect(Collectors.joining(" ")));

				csvPrinter.printRecord(recordElements.toArray(new Object[0]));
			}

			csvPrinter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// e.g. 2024-03-26 19:23:31.446265+00 to 2024-03-26T19:23:31Z
	@Nonnull
	private static Instant convertStringToInstant(@Nullable String instantAsString) {
		requireNonNull(instantAsString);

		String rewrittenInstantAsString = (instantAsString.substring(0, instantAsString.indexOf(".")) + "Z").replace(" ", "T");
		return DateTimeFormatter.ISO_INSTANT.parse(rewrittenInstantAsString, Instant::from);
	}

	private static void sortOrdersInPlace(@Nonnull List<DiffableOrder> orders) {
		requireNonNull(orders);

		Collections.sort(orders, Comparator.comparing((DiffableOrder order) -> order.getOrderDate())
				.thenComparing(order -> order.getFirstName())
				.thenComparing(order -> order.getLastName())
				.thenComparing(order -> order.getUid()));
	}

	@NotThreadSafe
	protected static class DiffAnalysisRow {
		@Nullable
		private LocalDate epicOrderDate;
		@Nullable
		private LocalDate cobaltOrderDate;
		@Nullable
		private String orderId;
		@Nullable
		private String firstName;
		@Nullable
		private String lastName;
		@Nullable
		private String mrn;
		@Nullable
		private String uid;
		@Nullable
		private Boolean orderExistsInCobalt;
		@Nullable
		private LocalDate epicAssessmentsCompletedAt;
		@Nullable
		private LocalDateTime cobaltAssessmentsCompletedAt;
		@Nullable
		private String cobaltAssessmentsCompletedBy;
		@Nullable
		private String cobaltAssessmentsCompletedByRole;
		@Nullable
		private String cobaltPanelAssignment;
		@Nullable
		private String cobaltReferenceNumber;
		@Nullable
		private List<String> notes;

		@Nullable
		public LocalDate getEpicOrderDate() {
			return this.epicOrderDate;
		}

		public void setEpicOrderDate(@Nullable LocalDate epicOrderDate) {
			this.epicOrderDate = epicOrderDate;
		}

		@Nullable
		public LocalDate getCobaltOrderDate() {
			return this.cobaltOrderDate;
		}

		public void setCobaltOrderDate(@Nullable LocalDate cobaltOrderDate) {
			this.cobaltOrderDate = cobaltOrderDate;
		}

		@Nullable
		public String getOrderId() {
			return this.orderId;
		}

		public void setOrderId(@Nullable String orderId) {
			this.orderId = orderId;
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
		public Boolean getOrderExistsInCobalt() {
			return this.orderExistsInCobalt;
		}

		public void setOrderExistsInCobalt(@Nullable Boolean orderExistsInCobalt) {
			this.orderExistsInCobalt = orderExistsInCobalt;
		}

		@Nullable
		public LocalDate getEpicAssessmentsCompletedAt() {
			return this.epicAssessmentsCompletedAt;
		}

		public void setEpicAssessmentsCompletedAt(@Nullable LocalDate epicAssessmentsCompletedAt) {
			this.epicAssessmentsCompletedAt = epicAssessmentsCompletedAt;
		}

		@Nullable
		public LocalDateTime getCobaltAssessmentsCompletedAt() {
			return this.cobaltAssessmentsCompletedAt;
		}

		public void setCobaltAssessmentsCompletedAt(@Nullable LocalDateTime cobaltAssessmentsCompletedAt) {
			this.cobaltAssessmentsCompletedAt = cobaltAssessmentsCompletedAt;
		}

		@Nullable
		public String getCobaltAssessmentsCompletedBy() {
			return this.cobaltAssessmentsCompletedBy;
		}

		public void setCobaltAssessmentsCompletedBy(@Nullable String cobaltAssessmentsCompletedBy) {
			this.cobaltAssessmentsCompletedBy = cobaltAssessmentsCompletedBy;
		}

		@Nullable
		public String getCobaltAssessmentsCompletedByRole() {
			return this.cobaltAssessmentsCompletedByRole;
		}

		public void setCobaltAssessmentsCompletedByRole(@Nullable String cobaltAssessmentsCompletedByRole) {
			this.cobaltAssessmentsCompletedByRole = cobaltAssessmentsCompletedByRole;
		}

		@Nullable
		public String getCobaltPanelAssignment() {
			return this.cobaltPanelAssignment;
		}

		public void setCobaltPanelAssignment(@Nullable String cobaltPanelAssignment) {
			this.cobaltPanelAssignment = cobaltPanelAssignment;
		}

		@Nullable
		public String getCobaltReferenceNumber() {
			return this.cobaltReferenceNumber;
		}

		public void setCobaltReferenceNumber(@Nullable String cobaltReferenceNumber) {
			this.cobaltReferenceNumber = cobaltReferenceNumber;
		}

		@Nullable
		public List<String> getNotes() {
			return this.notes;
		}

		public void setNotes(@Nullable List<String> notes) {
			this.notes = notes;
		}
	}

	@NotThreadSafe
	protected static class DiffableOrder {
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
