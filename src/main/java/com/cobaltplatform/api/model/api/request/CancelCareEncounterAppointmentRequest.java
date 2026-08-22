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

/**
 * User-supplied details for a Care Navigator appointment cancellation.
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CancelCareEncounterAppointmentRequest {
	@Nullable
	private String cancellationReason;

	@Nullable
	public String getCancellationReason() {
		return this.cancellationReason;
	}

	public void setCancellationReason(@Nullable String cancellationReason) {
		this.cancellationReason = cancellationReason;
	}
}
