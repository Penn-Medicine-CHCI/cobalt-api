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
import com.cobaltplatform.api.model.db.ScreeningAnswerFormat.ScreeningAnswerFormatId;
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
public class ScreeningQuestion {
	@Nonnull
	private static final Gson GSON;

	static {
		GSON = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping()
				.create();
	}

	@Nullable
	private UUID screeningQuestionId;
	@Nullable
	private UUID screeningVersionId;
	@Nullable
	private UUID preQuestionScreeningConfirmationPromptId;
	@Nullable
	private ScreeningAnswerFormatId screeningAnswerFormatId;
	@Nullable
	private ScreeningAnswerContentHintId screeningAnswerContentHintId;
	@Nullable
	private String introText;
	@Nullable
	private String questionText;
	@Nullable
	private String footerText;
	@Nullable
	private Integer minimumAnswerCount;
	@Nullable
	private Integer maximumAnswerCount;
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
	public UUID getScreeningQuestionId() {
		return this.screeningQuestionId;
	}

	public void setScreeningQuestionId(@Nullable UUID screeningQuestionId) {
		this.screeningQuestionId = screeningQuestionId;
	}

	@Nullable
	public UUID getScreeningVersionId() {
		return this.screeningVersionId;
	}

	public void setScreeningVersionId(@Nullable UUID screeningVersionId) {
		this.screeningVersionId = screeningVersionId;
	}

	@Nullable
	public UUID getPreQuestionScreeningConfirmationPromptId() {
		return this.preQuestionScreeningConfirmationPromptId;
	}

	public void setPreQuestionScreeningConfirmationPromptId(@Nullable UUID preQuestionScreeningConfirmationPromptId) {
		this.preQuestionScreeningConfirmationPromptId = preQuestionScreeningConfirmationPromptId;
	}

	@Nullable
	public ScreeningAnswerFormatId getScreeningAnswerFormatId() {
		return this.screeningAnswerFormatId;
	}

	public void setScreeningAnswerFormatId(@Nullable ScreeningAnswerFormatId screeningAnswerFormatId) {
		this.screeningAnswerFormatId = screeningAnswerFormatId;
	}

	@Nullable
	public ScreeningAnswerContentHintId getScreeningAnswerContentHintId() {
		return this.screeningAnswerContentHintId;
	}

	public void setScreeningAnswerContentHintId(@Nullable ScreeningAnswerContentHintId screeningAnswerContentHintId) {
		this.screeningAnswerContentHintId = screeningAnswerContentHintId;
	}

	@Nullable
	public String getIntroText() {
		return this.introText;
	}

	public void setIntroText(@Nullable String introText) {
		this.introText = introText;
	}

	@Nullable
	public String getQuestionText() {
		return this.questionText;
	}

	public void setQuestionText(@Nullable String questionText) {
		this.questionText = questionText;
	}

	@Nullable
	public String getFooterText() {
		return this.footerText;
	}

	public void setFooterText(@Nullable String footerText) {
		this.footerText = footerText;
	}

	@Nullable
	public Integer getMinimumAnswerCount() {
		return this.minimumAnswerCount;
	}

	public void setMinimumAnswerCount(@Nullable Integer minimumAnswerCount) {
		this.minimumAnswerCount = minimumAnswerCount;
	}

	@Nullable
	public Integer getMaximumAnswerCount() {
		return this.maximumAnswerCount;
	}

	public void setMaximumAnswerCount(@Nullable Integer maximumAnswerCount) {
		this.maximumAnswerCount = maximumAnswerCount;
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
}
