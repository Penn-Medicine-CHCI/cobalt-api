package com.cobaltplatform.api;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Runs real migration SQL against an isolated local PostgreSQL database.
 * Override cobalt.test.postgres.adminUrl/adminUsername/adminPassword when the
 * local PostgreSQL administrator differs from the Docker Compose defaults.
 */
public final class DisposablePostgresTestExecutor {
	private DisposablePostgresTestExecutor() {}

	public static void run(DatabaseTest test) throws Exception {
		String adminUrl = System.getProperty("cobalt.test.postgres.adminUrl", "jdbc:postgresql://localhost:5501/postgres");
		String username = System.getProperty("cobalt.test.postgres.adminUsername", "postgres");
		String password = System.getProperty("cobalt.test.postgres.adminPassword", "password");
		String databaseName = "cobalt_migration_test_" + UUID.randomUUID().toString().replace("-", "");
		int queryStart = adminUrl.indexOf('?');
		String baseUrl = queryStart < 0 ? adminUrl : adminUrl.substring(0, queryStart);
		String query = queryStart < 0 ? "" : adminUrl.substring(queryStart);
		String testUrl = baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1) + databaseName + query;

		try (Connection admin = DriverManager.getConnection(adminUrl, username, password);
			 Statement adminStatement = admin.createStatement()) {
			adminStatement.execute("CREATE DATABASE " + databaseName);
			try {
				try (Connection database = DriverManager.getConnection(testUrl, username, password);
					 Statement statement = database.createStatement()) {
					statement.execute("CREATE SCHEMA cobalt");
					statement.execute(Files.readString(Paths.get("sql/initial/install-versioning.sql")));
					statement.execute("SET search_path TO cobalt, public");
					test.run(database);
				}
			} finally {
				adminStatement.execute("DROP DATABASE " + databaseName);
			}
		}
	}

	@FunctionalInterface
	public interface DatabaseTest {
		void run(Connection connection) throws Exception;
	}
}
