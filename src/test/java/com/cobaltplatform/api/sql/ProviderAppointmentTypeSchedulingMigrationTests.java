/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.cobaltplatform.api.sql;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Applies the real patch to databases in an isolated temporary PostgreSQL
 * cluster. No configured application database or external server is used.
 * Requires initdb and pg_ctl on PATH, or -Dcobalt.test.postgresBin=/path/to/bin.
 */
public class ProviderAppointmentTypeSchedulingMigrationTests {
	private static final Path MIGRATION = Path.of("sql", "updates", "290-provider-appointment-start-time-alignment.sql");
	private static final String PATCH_NAME = "290-provider-appointment-start-time-alignment";
	private static final String DATABASE_USER = "cobalt_alignment_test";
	private static Path temporaryDirectory;
	private static Path dataDirectory;
	private static Path pgCtl;
	private static String jdbcBaseUrl;
	private static boolean serverStarted;

	@BeforeClass
	public static void startDisposablePostgres() throws Exception {
		Path initdb = findPostgresCommand("initdb");
		pgCtl = findPostgresCommand("pg_ctl");
		Assume.assumeTrue("PostgreSQL initdb and pg_ctl are required for disposable migration tests.",
				initdb != null && pgCtl != null);

		temporaryDirectory = Files.createTempDirectory("cobalt-start-alignment-pg-");
		dataDirectory = temporaryDirectory.resolve("data");
		runCommand(temporaryDirectory.resolve("initdb.log"), initdb.toString(),
				"-D", dataDirectory.toString(), "--auth=trust", "--username=" + DATABASE_USER,
				"--no-locale", "--encoding=UTF8");

		int port;
		try (ServerSocket socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
			port = socket.getLocalPort();
		}

		Files.writeString(dataDirectory.resolve("postgresql.conf"), """
				listen_addresses = '127.0.0.1'
				unix_socket_directories = ''
				fsync = off
				""" + "port = " + port + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);

		runCommand(temporaryDirectory.resolve("pg-start.log"), pgCtl.toString(),
				"-D", dataDirectory.toString(), "-l", temporaryDirectory.resolve("postgres.log").toString(),
				"-w", "-t", "15", "start");
		serverStarted = true;
		jdbcBaseUrl = "jdbc:postgresql://127.0.0.1:" + port + "/";
	}

	@AfterClass
	public static void stopDisposablePostgres() throws Exception {
		try {
			if (serverStarted)
				runCommand(temporaryDirectory.resolve("pg-stop.log"), pgCtl.toString(),
						"-D", dataDirectory.toString(), "-w", "-t", "15", "-m", "immediate", "stop");
		} finally {
			if (temporaryDirectory != null) {
				try (Stream<Path> paths = Files.walk(temporaryDirectory)) {
					for (Path path : paths.sorted(Comparator.reverseOrder()).toList())
						Files.deleteIfExists(path);
				}
			}
		}
	}

	@Test
	public void migrationPreservesExistingRowsAndEnforcesPolicyConstraints() throws Exception {
		try (Connection connection = createFixtureDatabase(); Statement statement = connection.createStatement()) {
			UUID providerId = UUID.randomUUID();
			UUID appointmentTypeId = UUID.randomUUID();
			UUID mappingId = UUID.randomUUID();
			statement.execute(insertMapping(mappingId, providerId, appointmentTypeId, 1));
			applyMigration(statement);

			try (ResultSet result = statement.executeQuery("""
					SELECT provider_appointment_type_id, display_order, scheduling_start_time_interval_in_minutes
					FROM provider_appointment_type
					""")) {
				assertTrue(result.next());
				assertEquals(mappingId, result.getObject("provider_appointment_type_id", UUID.class));
				assertEquals(1, result.getInt("display_order"));
				assertNull(result.getObject("scheduling_start_time_interval_in_minutes"));
			}

			for (int interval : new int[]{1, 60, 1440}) {
				statement.executeUpdate("UPDATE provider_appointment_type SET scheduling_start_time_interval_in_minutes=" + interval);
				try (ResultSet result = statement.executeQuery("SELECT scheduling_start_time_interval_in_minutes FROM provider_appointment_type")) {
					assertTrue(result.next());
					assertEquals(interval, result.getInt(1));
				}
			}

			for (int interval : new int[]{-1, 0, 1441}) {
				SQLException failure = assertThrows(SQLException.class, () -> statement.executeUpdate(
						"UPDATE provider_appointment_type SET scheduling_start_time_interval_in_minutes=" + interval));
				assertEquals("23514", failure.getSQLState());
			}

			statement.executeUpdate("UPDATE provider_appointment_type SET scheduling_start_time_interval_in_minutes=NULL");
			SQLException duplicate = assertThrows(SQLException.class,
					() -> statement.execute(insertMapping(UUID.randomUUID(), providerId, appointmentTypeId, 2)));
			assertEquals("23505", duplicate.getSQLState());

			// Different pairs remain valid, including the same type offered by a different provider.
			statement.execute(insertMapping(UUID.randomUUID(), UUID.randomUUID(), appointmentTypeId, 1));
			statement.execute(insertMapping(UUID.randomUUID(), providerId, UUID.randomUUID(), 2));
			try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM provider_appointment_type")) {
				assertTrue(result.next());
				assertEquals(3, result.getInt(1));
			}
			try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM _v.patches WHERE patch_name='" + PATCH_NAME + "'")) {
				assertTrue(result.next());
				assertEquals(1, result.getInt(1));
			}
		}
	}

	@Test
	public void preexistingDuplicatesFailWithoutChangingRowsOrRegisteringPatch() throws Exception {
		try (Connection connection = createFixtureDatabase(); Statement statement = connection.createStatement()) {
			UUID providerId = UUID.randomUUID();
			UUID appointmentTypeId = UUID.randomUUID();
			statement.execute(insertMapping(UUID.randomUUID(), providerId, appointmentTypeId, 1));
			statement.execute(insertMapping(UUID.randomUUID(), providerId, appointmentTypeId, 2));

			SQLException failure = assertThrows(SQLException.class, () -> applyMigration(statement));
			assertEquals("23505", failure.getSQLState());
			statement.execute("ROLLBACK");

			try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM provider_appointment_type")) {
				assertTrue(result.next());
				assertEquals(2, result.getInt(1));
			}
			try (ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM _v.patches WHERE patch_name='" + PATCH_NAME + "'")) {
				assertTrue(result.next());
				assertEquals(0, result.getInt(1));
			}
			try (ResultSet result = statement.executeQuery("""
					SELECT COUNT(*) FROM information_schema.columns
					WHERE table_schema='public' AND table_name='provider_appointment_type'
					AND column_name='scheduling_start_time_interval_in_minutes'
					""")) {
				assertTrue(result.next());
				assertEquals(0, result.getInt(1));
			}
		}
	}

	private static Connection createFixtureDatabase() throws Exception {
		String databaseName = "cobalt_alignment_" + UUID.randomUUID().toString().replace("-", "");
		try (Connection connection = DriverManager.getConnection(jdbcBaseUrl + "postgres", DATABASE_USER, "");
				 Statement statement = connection.createStatement()) {
			statement.execute("CREATE DATABASE " + databaseName);
		}

		Connection connection = DriverManager.getConnection(jdbcBaseUrl + databaseName, DATABASE_USER, "");
		try (Statement statement = connection.createStatement()) {
			statement.execute(Files.readString(Path.of("sql", "initial", "install-versioning.sql"), StandardCharsets.UTF_8));
			statement.execute("""
					CREATE TABLE provider_appointment_type (
						provider_appointment_type_id UUID PRIMARY KEY,
						provider_id UUID NOT NULL,
						appointment_type_id UUID NOT NULL,
						display_order SMALLINT NOT NULL,
						created TIMESTAMPTZ NOT NULL DEFAULT now(),
						last_updated TIMESTAMPTZ NOT NULL DEFAULT now(),
						UNIQUE (provider_id, display_order)
					)
					""");
		}
		return connection;
	}

	private static void applyMigration(Statement statement) throws IOException, SQLException {
		statement.execute(Files.readString(MIGRATION, StandardCharsets.UTF_8));
	}

	private static String insertMapping(UUID mappingId, UUID providerId, UUID appointmentTypeId, int displayOrder) {
		return "INSERT INTO provider_appointment_type (provider_appointment_type_id, provider_id, appointment_type_id, display_order) "
				+ "VALUES ('" + mappingId + "', '" + providerId + "', '" + appointmentTypeId + "', " + displayOrder + ")";
	}

	private static Path findPostgresCommand(String command) {
		String configuredDirectory = System.getProperty("cobalt.test.postgresBin");
		if (configuredDirectory != null) {
			Path path = Path.of(configuredDirectory).resolve(command);
			return Files.isExecutable(path) ? path : null;
		}

		String path = System.getenv("PATH");
		if (path != null) {
			for (String directory : path.split(File.pathSeparator)) {
				Path executable = Path.of(directory).resolve(command);
				if (Files.isExecutable(executable))
					return executable;
			}
		}
		return null;
	}

	private static void runCommand(Path log, String... command) throws Exception {
		Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
		boolean finished = process.waitFor(30, TimeUnit.SECONDS);
		if (!finished)
			process.destroyForcibly();
		assertTrue("PostgreSQL command timed out: " + command[0], finished);
		assertEquals(Files.readString(log, StandardCharsets.UTF_8), 0, process.exitValue());
	}
}
