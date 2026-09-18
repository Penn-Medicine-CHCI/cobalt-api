/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.cobaltplatform.api.model.service;

import com.cobaltplatform.api.UnitTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.annotation.concurrent.ThreadSafe;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@ThreadSafe
@Category(UnitTest.class)
public class ScreeningScoreTests {
	@Test
	public void legacyScoreJsonRemainsReadable() {
		ScreeningScore score = ScreeningScore.fromJsonRepresentation("{\"overallScore\":9}");

		assertEquals(Integer.valueOf(9), score.getOverallScore());
		assertNull(score.getRawScore());
		assertNull(score.getTScore());
		assertNull(score.getStandardError());
	}

	@Test
	public void promisV1EightItemBoundaryScoresSurviveJsonRoundTrip() {
		assertPromisScoreRoundTrip(8, "26.9", "4.1");
		assertPromisScoreRoundTrip(40, "66.1", "4.9");
	}

	private void assertPromisScoreRoundTrip(int rawScore, String tScore, String standardError) {
		ScreeningScore score = new ScreeningScore();
		score.setOverallScore(rawScore);
		score.setRawScore(rawScore);
		score.setTScore(new BigDecimal(tScore));
		score.setStandardError(new BigDecimal(standardError));

		ScreeningScore roundTrippedScore = ScreeningScore.fromJsonRepresentation(score.toJsonRepresentation());

		assertEquals(Integer.valueOf(rawScore), roundTrippedScore.getOverallScore());
		assertEquals(Integer.valueOf(rawScore), roundTrippedScore.getRawScore());
		assertEquals(0, new BigDecimal(tScore).compareTo(roundTrippedScore.getTScore()));
		assertEquals(0, new BigDecimal(standardError).compareTo(roundTrippedScore.getStandardError()));
	}
}
