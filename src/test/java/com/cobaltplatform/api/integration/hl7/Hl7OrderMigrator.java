package com.cobaltplatform.api.integration.hl7;

import com.cobaltplatform.api.integration.hl7.model.event.Hl7GeneralOrderTriggerEvent;
import com.cobaltplatform.api.integration.hl7.model.section.Hl7OrderSection;
import com.cobaltplatform.api.integration.hl7.model.section.Hl7PatientSection;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedCompositeIdWithCheckDigit;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.soklet.util.LoggingUtils.initializeLogback;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class Hl7OrderMigrator {
	public static void main(@Nullable String[] args) throws Exception {
		initializeLogback(Paths.get("config/local/logback.xml"));

		final String HL7_ORDERS_TO_MIGRATE_DIRECTORY_ENV_VAR_NAME = "HL7_ORDERS_TO_MIGRATE_DIRECTORY";
		final String HL7_MRN_TYPE_ENV_VAR_NAME = "HL7_MRN_TYPE";
		final String HL7_ORDERS_TO_MIGRATE_FILE_ENV_VAR_NAME = "HL7_ORDERS_TO_MIGRATE_FILE";

		String hl7OrdersToMigrateFileName = extractEnvVarValue(HL7_ORDERS_TO_MIGRATE_FILE_ENV_VAR_NAME);
		String hl7OrdersToMigrateDirectoryName = extractEnvVarValue(HL7_ORDERS_TO_MIGRATE_DIRECTORY_ENV_VAR_NAME);
		String hl7MrnType = extractEnvVarValue(HL7_MRN_TYPE_ENV_VAR_NAME);

		Path hl7OrdersToMigrateFile = Path.of(hl7OrdersToMigrateFileName);

		if (!Files.exists(hl7OrdersToMigrateFile))
			throw new IllegalStateException(format("File %s does not exist", hl7OrdersToMigrateFile.toAbsolutePath()));

		if (Files.isDirectory(hl7OrdersToMigrateFile))
			throw new IllegalStateException(format("Path %s is a directory", hl7OrdersToMigrateFile.toAbsolutePath()));

		Path hl7OrdersToMigrateDirectory = Path.of(hl7OrdersToMigrateDirectoryName);

		if (!Files.exists(hl7OrdersToMigrateDirectory))
			throw new IllegalStateException(format("Directory %s does not exist", hl7OrdersToMigrateDirectory.toAbsolutePath()));

		if (!Files.isDirectory(hl7OrdersToMigrateDirectory))
			throw new IllegalStateException(format("Path %s is not a directory", hl7OrdersToMigrateDirectory.toAbsolutePath()));

		migrateHl7Orders(hl7OrdersToMigrateFile, hl7OrdersToMigrateDirectory, hl7MrnType);
	}

	static void migrateHl7Orders(@Nonnull Path hl7OrdersToMigrateFile,
															 @Nonnull Path hl7OrdersToMigrateDirectory,
															 @Nonnull String hl7MrnType) throws IOException, Hl7ParsingException {
		requireNonNull(hl7OrdersToMigrateFile);
		requireNonNull(hl7OrdersToMigrateDirectory);
		requireNonNull(hl7MrnType);

		List<Path> hl7OrderFiles = Stream.of(hl7OrdersToMigrateDirectory.toFile().listFiles())
				.filter(file -> !file.isDirectory() && file.getName().startsWith("To_COBALT_Ord_"))
				.sorted()
				.map(file -> file.toPath())
				.collect(Collectors.toList());

		System.out.printf("Processing %d order file[s]...", hl7OrderFiles.size());

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

			// TODO: reference against import file
		}
	}

	@Nonnull
	static String extractEnvVarValue(@Nonnull String envVarName) {
		requireNonNull(envVarName);

		String envVarValue = StringUtils.trimToNull(System.getenv(envVarName));

		if (envVarValue == null)
			throw new IllegalStateException(format("You must specify the %s environment variable", envVarName));

		return envVarValue;
	}
}
