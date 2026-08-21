/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.model.db.CareEncounterNote;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Immutable
public class CareEncounterNoteApiResponse {
	@Nonnull
	private final UUID careEncounterNoteId;
	@Nonnull
	private final UUID careEncounterId;
	@Nonnull
	private final String note;
	@Nonnull
	private final UUID createdByAccountId;
	@Nullable
	private final String createdByAccountDisplayName;
	@Nonnull
	private final UUID lastUpdatedByAccountId;
	@Nullable
	private final String lastUpdatedByAccountDisplayName;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final Instant lastUpdated;
	@Nonnull
	private final String lastUpdatedDescription;

	@ThreadSafe
	public interface CareEncounterNoteApiResponseFactory {
		@Nonnull
		CareEncounterNoteApiResponse create(@Nonnull CareEncounterNote careEncounterNote);
	}

	@AssistedInject
	public CareEncounterNoteApiResponse(@Nonnull Formatter formatter,
														@Assisted @Nonnull CareEncounterNote careEncounterNote) {
		requireNonNull(formatter);
		requireNonNull(careEncounterNote);

		this.careEncounterNoteId = careEncounterNote.getCareEncounterNoteId();
		this.careEncounterId = careEncounterNote.getCareEncounterId();
		this.note = careEncounterNote.getNote();
		this.createdByAccountId = careEncounterNote.getCreatedByAccountId();
		this.createdByAccountDisplayName = careEncounterNote.getCreatedByAccountDisplayName();
		this.lastUpdatedByAccountId = careEncounterNote.getLastUpdatedByAccountId();
		this.lastUpdatedByAccountDisplayName = careEncounterNote.getLastUpdatedByAccountDisplayName();
		this.created = careEncounterNote.getCreated();
		this.createdDescription = formatter.formatTimestamp(careEncounterNote.getCreated());
		this.lastUpdated = careEncounterNote.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(careEncounterNote.getLastUpdated());
	}

	@Nonnull public UUID getCareEncounterNoteId() { return this.careEncounterNoteId; }
	@Nonnull public UUID getCareEncounterId() { return this.careEncounterId; }
	@Nonnull public String getNote() { return this.note; }
	@Nonnull public UUID getCreatedByAccountId() { return this.createdByAccountId; }
	@Nullable public String getCreatedByAccountDisplayName() { return this.createdByAccountDisplayName; }
	@Nonnull public UUID getLastUpdatedByAccountId() { return this.lastUpdatedByAccountId; }
	@Nullable public String getLastUpdatedByAccountDisplayName() { return this.lastUpdatedByAccountDisplayName; }
	@Nonnull public Instant getCreated() { return this.created; }
	@Nonnull public String getCreatedDescription() { return this.createdDescription; }
	@Nonnull public Instant getLastUpdated() { return this.lastUpdated; }
	@Nonnull public String getLastUpdatedDescription() { return this.lastUpdatedDescription; }
}
