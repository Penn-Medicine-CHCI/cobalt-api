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

import org.junit.Test;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@ThreadSafe
public class ProviderBookingScoringSqlTests {
	@Test
	public void migratedProviderIntakeScoringPreservesThresholdSemantics() throws IOException {
		String migrationSql = Files.readString(Path.of("sql/updates/259-provider-booking-database.sql"),
				StandardCharsets.UTF_8);

		assertTrue(migrationSql.contains("const questionIsRequired = Number(currentQuestion.minimumAnswerCount || 0) > 0;"));
		assertTrue(migrationSql.contains("selectedAnswerOptions.some((answerOption) =>"));
		assertTrue(migrationSql.contains("answerOption.metadata.terminal === true"));
		assertTrue(migrationSql.contains("overallScore += Number(answerOption.score || 0);"));
		assertTrue(migrationSql.contains("overallScore < minimumEligibilityScore"));
		assertFalse(migrationSql.contains("Number(selectedAnswerOption.score || 0) <= 0"));
	}
}
