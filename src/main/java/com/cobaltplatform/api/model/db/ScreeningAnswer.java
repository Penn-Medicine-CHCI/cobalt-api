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

import com.cobaltplatform.api.model.db.ScreeningAnswerStatus.ScreeningAnswerStatusId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ScreeningAnswer {
	@Nullable
	private UUID screeningAnswerId;
	@Nullable
	private UUID screeningAnswerOptionId;
	@Nullable
	private ScreeningAnswerStatusId screeningAnswerStatusId;
	@Nullable
	private UUID screeningSessionScreeningId;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private String text;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getScreeningAnswerId() {
		return this.screeningAnswerId;
	}

	public void setScreeningAnswerId(@Nullable UUID screeningAnswerId) {
		this.screeningAnswerId = screeningAnswerId;
	}

	@Nullable
	public UUID getScreeningAnswerOptionId() {
		return this.screeningAnswerOptionId;
	}

	public void setScreeningAnswerOptionId(@Nullable UUID screeningAnswerOptionId) {
		this.screeningAnswerOptionId = screeningAnswerOptionId;
	}

	@Nullable
	public ScreeningAnswerStatusId getScreeningAnswerStatusId() {
		return this.screeningAnswerStatusId;
	}

	public void setScreeningAnswerStatusId(@Nullable ScreeningAnswerStatusId screeningAnswerStatusId) {
		this.screeningAnswerStatusId = screeningAnswerStatusId;
	}

	@Nullable
	public UUID getScreeningSessionScreeningId() {
		return this.screeningSessionScreeningId;
	}

	public void setScreeningSessionScreeningId(@Nullable UUID screeningSessionScreeningId) {
		this.screeningSessionScreeningId = screeningSessionScreeningId;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public String getText() {
		return this.text;
	}

	public void setText(@Nullable String text) {
		this.text = text;
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
