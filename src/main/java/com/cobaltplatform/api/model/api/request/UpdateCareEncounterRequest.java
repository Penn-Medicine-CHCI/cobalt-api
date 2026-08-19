/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.cobaltplatform.api.model.api.request;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

@NotThreadSafe
public class UpdateCareEncounterRequest {
	@Nullable
	private UUID careEncounterId;
	@Nullable
	private String notes;
	@Nullable
	private UUID providerId;
	@Nullable
	private UUID accountId;

	@Nullable
	public UUID getCareEncounterId() {
		return this.careEncounterId;
	}

	public void setCareEncounterId(@Nullable UUID careEncounterId) {
		this.careEncounterId = careEncounterId;
	}

	@Nullable
	public String getNotes() {
		return this.notes;
	}

	public void setNotes(@Nullable String notes) {
		this.notes = notes;
	}

	@Nullable
	public UUID getProviderId() {
		return this.providerId;
	}

	public void setProviderId(@Nullable UUID providerId) {
		this.providerId = providerId;
	}

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}
}
