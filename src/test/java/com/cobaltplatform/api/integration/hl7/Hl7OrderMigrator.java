package com.cobaltplatform.api.integration.hl7;

import com.cobaltplatform.api.integration.hl7.model.event.Hl7GeneralOrderTriggerEvent;
import com.cobaltplatform.api.integration.hl7.model.section.Hl7OrderSection;
import com.cobaltplatform.api.integration.hl7.model.section.Hl7PatientSection;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedCompositeIdWithCheckDigit;
import com.google.common.collect.Sets;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.soklet.util.LoggingUtils.initializeLogback;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class Hl7OrderMigrator {
	public static void main(@Nullable String[] args) throws Exception {
		initializeLogback(Paths.get("config/local/logback.xml"));

		final String CURRENT_SYSTEM_ORDERS_FILE_ENV_VAR_NAME = "CURRENT_SYSTEM_ORDERS_FILE";
		final String HL7_ORDERS_TO_MIGRATE_DIRECTORY_ENV_VAR_NAME = "HL7_ORDERS_TO_MIGRATE_DIRECTORY";
		final String HL7_MRN_TYPE_ENV_VAR_NAME = "HL7_MRN_TYPE";
		final String HL7_ORDERS_TO_MIGRATE_FILE_ENV_VAR_NAME = "HL7_ORDERS_TO_MIGRATE_FILE";

		// e.g. \copy (select patient_order_id,order_date, patient_mrn, created at time zone 'America/New_York' as imported_at from patient_order where test_patient_order=false order by order_date) to 'imported-orders.csv' csv header;
		String currentSystemOrdersFilename = extractEnvVarValue(CURRENT_SYSTEM_ORDERS_FILE_ENV_VAR_NAME);
		String hl7OrdersToMigrateFilename = extractEnvVarValue(HL7_ORDERS_TO_MIGRATE_FILE_ENV_VAR_NAME);
		String hl7OrdersToMigrateDirectoryName = extractEnvVarValue(HL7_ORDERS_TO_MIGRATE_DIRECTORY_ENV_VAR_NAME);
		String hl7MrnType = extractEnvVarValue(HL7_MRN_TYPE_ENV_VAR_NAME);

		Path currentSystemOrdersFile = Path.of(currentSystemOrdersFilename);

		if (!Files.exists(currentSystemOrdersFile))
			throw new IllegalStateException(format("File %s does not exist", currentSystemOrdersFile.toAbsolutePath()));

		if (Files.isDirectory(currentSystemOrdersFile))
			throw new IllegalStateException(format("Path %s is a directory", currentSystemOrdersFile.toAbsolutePath()));

		Path hl7OrdersToMigrateFile = Path.of(hl7OrdersToMigrateFilename);

		if (!Files.exists(hl7OrdersToMigrateFile))
			throw new IllegalStateException(format("File %s does not exist", hl7OrdersToMigrateFile.toAbsolutePath()));

		if (Files.isDirectory(hl7OrdersToMigrateFile))
			throw new IllegalStateException(format("Path %s is a directory", hl7OrdersToMigrateFile.toAbsolutePath()));

		Path hl7OrdersToMigrateDirectory = Path.of(hl7OrdersToMigrateDirectoryName);

		if (!Files.exists(hl7OrdersToMigrateDirectory))
			throw new IllegalStateException(format("Directory %s does not exist", hl7OrdersToMigrateDirectory.toAbsolutePath()));

		if (!Files.isDirectory(hl7OrdersToMigrateDirectory))
			throw new IllegalStateException(format("Path %s is not a directory", hl7OrdersToMigrateDirectory.toAbsolutePath()));

		migrateHl7Orders(currentSystemOrdersFile, hl7OrdersToMigrateFile, hl7OrdersToMigrateDirectory, hl7MrnType);
	}

	static void migrateHl7Orders(@Nonnull Path currentSystemOrdersFile,
															 @Nonnull Path hl7OrdersToMigrateFile,
															 @Nonnull Path hl7OrdersToMigrateDirectory,
															 @Nonnull String hl7MrnType) throws IOException, Hl7ParsingException {
		requireNonNull(currentSystemOrdersFile);
		requireNonNull(hl7OrdersToMigrateFile);
		requireNonNull(hl7OrdersToMigrateDirectory);
		requireNonNull(hl7MrnType);

		Set<String> alreadyImportedMrns = new LinkedHashSet<>();

		// Pull order data from the existing IC instance's database export
		try (Reader reader = new FileReader(currentSystemOrdersFile.toFile(), StandardCharsets.UTF_8)) {
			for (CSVRecord record : CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
				String mrn = trimToNull(record.get(2));
				alreadyImportedMrns.add(mrn);
			}
		}

		System.out.println(format("Already imported %d MRNs", alreadyImportedMrns.size()));

		Set<String> mrnsToMigrate = new LinkedHashSet<>();

		// Pull data from the "to migrate" CSV
		try (Reader reader = new FileReader(hl7OrdersToMigrateFile.toFile(), StandardCharsets.UTF_8)) {
			for (CSVRecord record : CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
				// String practice = StringUtils.trimToNull(record.get(0));
				// String firstName = StringUtils.trimToNull(record.get(1));
				// String lastName = StringUtils.trimToNull(record.get(2));
				String mrnToMigrate = trimToNull(record.get(3));
				// String orderDateAsString = StringUtils.trimToNull(record.get(4));

				// MRN has a leading value that needs to be removed
				mrnToMigrate = mrnToMigrate.substring(1);

				if (mrnsToMigrate.contains(mrnToMigrate))
					throw new IllegalStateException(format("Duplicate MRN encountered: %s", mrnToMigrate));

				mrnsToMigrate.add(mrnToMigrate);
			}
		}

		System.out.printf("MRNs to migrate: %d\n", mrnsToMigrate.size());

		Set<String> mrnsToMigrateThatWereAlreadyImported = Sets.intersection(alreadyImportedMrns, mrnsToMigrate);

		System.out.printf("MRNs to migrate that were already imported: %s\n", mrnsToMigrateThatWereAlreadyImported);

		List<Path> hl7OrderFiles = Stream.of(hl7OrdersToMigrateDirectory.toFile().listFiles())
				.filter(file -> !file.isDirectory() && file.getName().startsWith("To_COBALT_Ord_"))
				.sorted()
				.map(file -> file.toPath())
				.collect(Collectors.toList());

		System.out.printf("Processing %d order file[s]...\n", hl7OrderFiles.size());

		Map<String, Path> hl7OrderFilesToMigrateByMrn = new LinkedHashMap<>();

		Hl7Client hl7Client = new Hl7Client();

		for (Path hl7OrderFile : hl7OrderFiles) {
			byte[] generalOrderHl7 = Files.readAllBytes(hl7OrderFile);
			String generalOrderHl7AsString = hl7Client.messageFromBytes(generalOrderHl7);
			Hl7GeneralOrderTriggerEvent generalOrder = hl7Client.parseGeneralOrder(generalOrderHl7AsString);

			if (generalOrder.getOrders() == null || generalOrder.getOrders().size() != 1)
				throw new IllegalStateException(format("Invalid order message for %s", hl7OrderFile.toAbsolutePath()));

			Hl7OrderSection order = generalOrder.getOrders().get(0);
			Hl7PatientSection patient = generalOrder.getPatient();

			Hl7ExtendedCompositeIdWithCheckDigit patientMrn = patient.getPatientIdentification().getPatientIdentifierList().stream()
					.filter(patientIdentifier -> {
						return Objects.equals(hl7MrnType, patientIdentifier.getIdentifierTypeCode());
					})
					.findFirst()
					.orElse(null);

			if (patientMrn == null)
				throw new IllegalStateException(format("Unable to find patient MRN in %s", hl7OrderFile.toAbsolutePath()));

			String orderFileMrn = trimToNull(patientMrn.getIdNumber());

			if (mrnsToMigrate.contains(orderFileMrn)) {
				if (hl7OrderFilesToMigrateByMrn.containsKey(orderFileMrn))
					System.out.printf("Already have an order file for MRN %s\n", orderFileMrn);

				hl7OrderFilesToMigrateByMrn.put(orderFileMrn, hl7OrderFile);
			}
		}

		for (Entry<String, Path> entry : hl7OrderFilesToMigrateByMrn.entrySet()) {
			String mrn = entry.getKey();
			Path hl7OrderFile = entry.getValue();

			System.out.printf("%s=%s\n", mrn, hl7OrderFile);
		}

		Set<String> mrnsToMigrateWithoutHl7OrderFiles = Sets.difference(mrnsToMigrate, hl7OrderFilesToMigrateByMrn.keySet());

		System.out.printf("There are %d MRNs that do not have corresponding HL7 order files.\n", mrnsToMigrateWithoutHl7OrderFiles.size());
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
