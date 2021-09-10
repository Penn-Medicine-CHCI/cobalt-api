/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Originally created at the University of Pennsylvania and Penn Medicine by:
 * Dr. David Asch; Dr. Lisa Bellini; Dr. Cecilia Livesey; Kelley Kugler; and Dr. Matthew Press.
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

package com.cobaltplatform.api.model.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * @author Transmogrify LLC.
 */
public class EvidenceScores {

	@Nullable
	private final Recommendation topRecommendation;
	@Nonnull
	private final Boolean isCrisis;
	@Nonnull
	private final Recommendation phq4Recommendation;
	@Nullable
	private final Recommendation phq9Recommendation;
	@Nullable
	private final Recommendation gad7Recommendation;
	@Nullable
	private final Recommendation pcptsdRecommendation;


	public static class Recommendation {
		@Nonnull
		private RecommendationLevel level;
		@Nullable
		private Integer score;
		@Nonnull
		private final UUID sessionId;
		@Nonnull
		private String answers;

		public Recommendation(@Nonnull RecommendationLevel level, @Nullable Integer score, @Nonnull UUID sessionId, @Nonnull String answers) {
			this.level = level;
			this.score = score;
			this.sessionId = sessionId;
			this.answers = answers;
		}

		@Nonnull
		public RecommendationLevel getLevel() {
			return level;
		}

		@Nullable
		public Integer getScore() {
			return score;
		}

		@Nonnull
		public String getAnswers() {
			return answers;
		}
	}

	public enum RecommendationLevel {
		NONE(0),
		PEER(1),
		PEER_COACH(2),
		COACH(3),
		COACH_CLINICIAN(4),
		CLINICIAN(5),
		CLINICIAN_PSYCHIATRIST(6),
		PSYCHIATRIST(7),
		CRISIS(8);

		private int value;

		RecommendationLevel(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public EvidenceScores(@Nonnull Recommendation phq4Recommendation,
												@Nullable Recommendation phq9Recommendation,
												@Nullable Recommendation gad7Recommendation,
												@Nullable Recommendation pcptsdRecommendation,
												@Nonnull Boolean isCrisis){

		this.phq4Recommendation = phq4Recommendation;
		this.phq9Recommendation = phq9Recommendation;
		this.gad7Recommendation = gad7Recommendation;
		this.pcptsdRecommendation = pcptsdRecommendation;
		this.isCrisis = isCrisis;
		this.topRecommendation = Stream.of(phq4Recommendation, phq9Recommendation, gad7Recommendation, pcptsdRecommendation)
				.filter(Objects::nonNull).max(Comparator.comparing(l -> l.getLevel().getValue())).orElse(null);
	}



	@Nullable
	public Recommendation getTopRecommendation() {
		return topRecommendation;
	}

	@Nullable
	public Recommendation getPhq4Recommendation() {
		return phq4Recommendation;
	}

	@Nullable
	public Recommendation getPhq9Recommendation() {
		return phq9Recommendation;
	}

	@Nullable
	public Recommendation getGad7Recommendation() {
		return gad7Recommendation;
	}

	@Nullable
	public Recommendation getPcptsdRecommendation() {
		return pcptsdRecommendation;
	}

	@Nonnull
	public Boolean getCrisis() {
		return isCrisis;
	}
}
