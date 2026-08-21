/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.cobaltplatform.api.model.db;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * An editable note attached to a Care Navigator encounter.
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CareEncounterNote {
	@Nullable
	private UUID careEncounterNoteId;
	@Nullable
	private UUID careEncounterId;
	@Nullable
	private String note;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private String createdByAccountDisplayName;
	@Nullable
	private UUID lastUpdatedByAccountId;
	@Nullable
	private String lastUpdatedByAccountDisplayName;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getCareEncounterNoteId() {
		return this.careEncounterNoteId;
	}

	public void setCareEncounterNoteId(@Nullable UUID careEncounterNoteId) {
		this.careEncounterNoteId = careEncounterNoteId;
	}

	@Nullable
	public UUID getCareEncounterId() {
		return this.careEncounterId;
	}

	public void setCareEncounterId(@Nullable UUID careEncounterId) {
		this.careEncounterId = careEncounterId;
	}

	@Nullable
	public String getNote() {
		return this.note;
	}

	public void setNote(@Nullable String note) {
		this.note = note;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public String getCreatedByAccountDisplayName() {
		return this.createdByAccountDisplayName;
	}

	public void setCreatedByAccountDisplayName(@Nullable String createdByAccountDisplayName) {
		this.createdByAccountDisplayName = createdByAccountDisplayName;
	}

	@Nullable
	public UUID getLastUpdatedByAccountId() {
		return this.lastUpdatedByAccountId;
	}

	public void setLastUpdatedByAccountId(@Nullable UUID lastUpdatedByAccountId) {
		this.lastUpdatedByAccountId = lastUpdatedByAccountId;
	}

	@Nullable
	public String getLastUpdatedByAccountDisplayName() {
		return this.lastUpdatedByAccountDisplayName;
	}

	public void setLastUpdatedByAccountDisplayName(@Nullable String lastUpdatedByAccountDisplayName) {
		this.lastUpdatedByAccountDisplayName = lastUpdatedByAccountDisplayName;
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
