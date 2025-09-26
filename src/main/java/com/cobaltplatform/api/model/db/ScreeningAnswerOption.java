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

package com.cobaltplatform.api.model.db;

import com.cobaltplatform.api.model.db.ScreeningAnswerContentHint.ScreeningAnswerContentHintId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pyranid.DatabaseColumn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ScreeningAnswerOption {
	@Nonnull
	private static final Gson GSON;

	static {
		GSON = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping()
				.create();
	}

	@Nullable
	private UUID screeningAnswerOptionId;
	@Nullable
	private UUID screeningQuestionId;
	@Nullable
	private String answerOptionText;
	@Nullable
	private Integer score;
	@Nullable
	private Boolean indicatesCrisis;
	@Nullable
	private Boolean freeformSupplement;
	@Nullable
	private String freeformSupplementText;

	@Nullable
	private Boolean freeformSupplementTextAutoShow;
	@Nullable
	private ScreeningAnswerContentHintId freeformSupplementContentHintId;
	@Nullable
	private Integer displayOrder;
	@Nullable
	@DatabaseColumn("metadata")
	private String metadataAsString;
	@Nullable
	private Map<String, Object> metadata;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nonnull
	protected Gson getGson() {
		return GSON;
	}

	@Nullable
	public String getMetadataAsString() {
		return this.metadataAsString;
	}

	public void setMetadataAsString(@Nullable String metadataAsString) {
		this.metadataAsString = metadataAsString;

		String metadata = trimToNull(metadataAsString);
		this.metadata = metadata == null ? Map.of() : getGson().fromJson(metadata, new TypeToken<Map<String, Object>>() {
		}.getType());
	}

	@Nonnull
	public Map<String, Object> getMetadata() {
		return this.metadata;
	}

	@Nullable
	public UUID getScreeningAnswerOptionId() {
		return this.screeningAnswerOptionId;
	}

	public void setScreeningAnswerOptionId(@Nullable UUID screeningAnswerOptionId) {
		this.screeningAnswerOptionId = screeningAnswerOptionId;
	}

	@Nullable
	public UUID getScreeningQuestionId() {
		return this.screeningQuestionId;
	}

	public void setScreeningQuestionId(@Nullable UUID screeningQuestionId) {
		this.screeningQuestionId = screeningQuestionId;
	}

	@Nullable
	public String getAnswerOptionText() {
		return this.answerOptionText;
	}

	public void setAnswerOptionText(@Nullable String answerOptionText) {
		this.answerOptionText = answerOptionText;
	}

	@Nullable
	public Integer getScore() {
		return this.score;
	}

	public void setScore(@Nullable Integer score) {
		this.score = score;
	}

	@Nullable
	public Boolean getIndicatesCrisis() {
		return this.indicatesCrisis;
	}

	public void setIndicatesCrisis(@Nullable Boolean indicatesCrisis) {
		this.indicatesCrisis = indicatesCrisis;
	}

	@Nullable
	public Boolean getFreeformSupplement() {
		return this.freeformSupplement;
	}

	public void setFreeformSupplement(@Nullable Boolean freeformSupplement) {
		this.freeformSupplement = freeformSupplement;
	}

	@Nullable
	public String getFreeformSupplementText() {
		return this.freeformSupplementText;
	}

	public void setFreeformSupplementText(@Nullable String freeformSupplementText) {
		this.freeformSupplementText = freeformSupplementText;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return this.displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	@Nullable
	public Instant getCreated() {
		return this.created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	@Nullable
	public Boolean getFreeformSupplementTextAutoShow() {
		return freeformSupplementTextAutoShow;
	}

	public void setFreeformSupplementTextAutoShow(@Nullable Boolean freeformSupplementTextAutoShow) {
		this.freeformSupplementTextAutoShow = freeformSupplementTextAutoShow;
	}

	@Nullable
	public ScreeningAnswerContentHintId getFreeformSupplementContentHintId() {
		return freeformSupplementContentHintId;
	}

	public void setFreeformSupplementContentHintId(@Nullable ScreeningAnswerContentHintId freeformSupplementContentHintId) {
		this.freeformSupplementContentHintId = freeformSupplementContentHintId;
	}
}
