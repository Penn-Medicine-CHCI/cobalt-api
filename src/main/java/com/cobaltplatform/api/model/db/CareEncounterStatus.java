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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Lifecycle state for the administrative Care Encounter record.
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CareEncounterStatus {
	@Nullable
	private CareEncounterStatusId careEncounterStatusId;
	@Nullable
	private String description;
	@Nullable
	private Boolean terminal;

	public enum CareEncounterStatusId {
		OPEN("Open"),
		CLOSED("Closed"),
		CANCELED("Closed");

		@Nonnull
		private final String displayLabel;

		CareEncounterStatusId(@Nonnull String displayLabel) {
			this.displayLabel = displayLabel;
		}

		@Nonnull
		public String getDisplayLabel() {
			return this.displayLabel;
		}
	}

	@Nullable
	public CareEncounterStatusId getCareEncounterStatusId() {
		return this.careEncounterStatusId;
	}

	public void setCareEncounterStatusId(@Nullable CareEncounterStatusId careEncounterStatusId) {
		this.careEncounterStatusId = careEncounterStatusId;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public Boolean getTerminal() {
		return this.terminal;
	}

	public void setTerminal(@Nullable Boolean terminal) {
		this.terminal = terminal;
	}
}
