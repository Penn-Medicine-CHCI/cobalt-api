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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ScreeningScore {
	@Nonnull
	private static final Gson GSON;

	static {
		GSON = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping()
				.create();
	}

	@Nullable
	private Integer overallScore;  // Should always be present
	@Nullable
	private Integer personalAccomplishmentScore;  // MBI-9
	@Nullable
	private Integer depersonalizationScore;  // MBI-9
	@Nullable
	private Integer emotionalExhaustionScore;  // MBI-9

	@Override
	public String toString() {
		return format("%s{overallScore=%s, personalAccomplishmentScore=%s, " +
						"depersonalizationScore=%s, emotionalExhaustionScore=%s}",
				getClass().getSimpleName(), getOverallScore(), getPersonalAccomplishmentScore(),
				getDepersonalizationScore(), getEmotionalExhaustionScore());
	}

	@Nonnull
	public static ScreeningScore fromJsonRepresentation(@Nonnull String json) {
		return GSON.fromJson(json, ScreeningScore.class);
	}

	@Nonnull
	public String toJsonRepresentation() {
		Map<String, Object> jsonObject = new HashMap<>();
		jsonObject.put("overallScore", getOverallScore());
		jsonObject.put("personalAccomplishmentScore", getPersonalAccomplishmentScore());
		jsonObject.put("depersonalizationScore", getDepersonalizationScore());
		jsonObject.put("emotionalExhaustionScore", getEmotionalExhaustionScore());

		return GSON.toJson(jsonObject);
	}

	@Nullable
	public Integer getOverallScore() {
		return this.overallScore;
	}

	public void setOverallScore(@Nullable Integer overallScore) {
		this.overallScore = overallScore;
	}

	@Nullable
	public Integer getPersonalAccomplishmentScore() {
		return this.personalAccomplishmentScore;
	}

	public void setPersonalAccomplishmentScore(@Nullable Integer personalAccomplishmentScore) {
		this.personalAccomplishmentScore = personalAccomplishmentScore;
	}

	@Nullable
	public Integer getDepersonalizationScore() {
		return this.depersonalizationScore;
	}

	public void setDepersonalizationScore(@Nullable Integer depersonalizationScore) {
		this.depersonalizationScore = depersonalizationScore;
	}

	@Nullable
	public Integer getEmotionalExhaustionScore() {
		return this.emotionalExhaustionScore;
	}

	public void setEmotionalExhaustionScore(@Nullable Integer emotionalExhaustionScore) {
		this.emotionalExhaustionScore = emotionalExhaustionScore;
	}
}
