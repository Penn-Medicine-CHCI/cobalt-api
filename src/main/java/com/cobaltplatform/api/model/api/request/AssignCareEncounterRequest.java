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
public class AssignCareEncounterRequest {
	@Nullable
	private UUID careNavigatorAccountId;

	@Nullable
	public UUID getCareNavigatorAccountId() {
		return this.careNavigatorAccountId;
	}

	public void setCareNavigatorAccountId(@Nullable UUID careNavigatorAccountId) {
		this.careNavigatorAccountId = careNavigatorAccountId;
	}
}
