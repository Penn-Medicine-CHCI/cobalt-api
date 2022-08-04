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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ScreeningSessionNote {
	@Nullable
	private UUID screeningSessionNoteId;
	@Nullable
	private UUID screeningSessionScreeningId;
	@Nullable
	private String note;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getScreeningSessionNoteId() {
		return this.screeningSessionNoteId;
	}

	public void setScreeningSessionNoteId(@Nullable UUID screeningSessionNoteId) {
		this.screeningSessionNoteId = screeningSessionNoteId;
	}

	@Nullable
	public UUID getScreeningSessionScreeningId() {
		return this.screeningSessionScreeningId;
	}

	public void setScreeningSessionScreeningId(@Nullable UUID screeningSessionScreeningId) {
		this.screeningSessionScreeningId = screeningSessionScreeningId;
	}

	@Nullable
	public String getNote() {
		return this.note;
	}

	public void setNote(@Nullable String note) {
		this.note = note;
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
