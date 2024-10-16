package com.cobaltplatform.api.integration.epic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pyranid.Database;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.soklet.util.LoggingUtils.initializeLogback;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PrimeFiveReporter {
	public static void main(@Nullable String[] args) throws Exception {
		initializeLogback(Paths.get("config/local/logback.xml"));

		// Total number of patients who completed the PRIME5 (regardless of positive/negative result)
		//  - If it is easiest in this case to provide all the PRIME-5 data (whether positive or not) since it was launched, then that would be great.
		// Raw data of patients between ages 18 to 30 years old who screened positive on PRIME5
		//  - Is it possible to get the individual responses to all of the assessment questions for each patient?
		//  - If not possible, could we get each patient’s responses to each item on the PRIME5?

		performOperationWithDatabase((database -> {
			System.out.println("OK!");

			final List<String> DIFF_ANALYSIS_CSV_HEADERS = List.of(
					"TODO"
			);

			Path reportCsvFile = Path.of("resources/test/prime5-report.csv");

			try (Writer writer = new FileWriter(reportCsvFile.toFile(), StandardCharsets.UTF_8);
					 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(DIFF_ANALYSIS_CSV_HEADERS.toArray(new String[0])))) {

				// For each row...
				List<String> recordElements = new ArrayList<>();
				recordElements.add("TODO");
				csvPrinter.printRecord(recordElements.toArray(new Object[0]));

				// After all records are written
				csvPrinter.flush();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}));
	}

	static void performOperationWithDatabase(@Nonnull Consumer<Database> databaseConsumer) {
		requireNonNull(databaseConsumer);

		Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		Path databaseConfigurationFile = Path.of("resources/test/prime-five-db-credentials.json");
		String databaseConfigurationJson = null;

		try {
			databaseConfigurationJson = Files.readString(databaseConfigurationFile, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		Map<String, String> databaseConfiguration = gson.fromJson(databaseConfigurationJson, new TypeToken<Map<String, String>>() {
			// Ignored
		});

		try (HikariDataSource dataSource = new HikariDataSource(new HikariConfig() {
			{
				setJdbcUrl(databaseConfiguration.get("jdbcUrl"));
				setUsername(databaseConfiguration.get("jdbcUsername"));
				setPassword(databaseConfiguration.get("jdbcPassword"));
				setMaximumPoolSize(1);
			}
		})) {
			Database database = Database.forDataSource(dataSource)
					.timeZone(ZoneId.of("UTC"))
					.build();

			databaseConsumer.accept(database);
		}
	}
}
