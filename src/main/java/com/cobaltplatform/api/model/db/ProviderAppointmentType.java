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
 * Configuration for one provider's offering of an appointment type.
 */
@NotThreadSafe
public class ProviderAppointmentType {
	@Nullable
	private UUID providerAppointmentTypeId;
	@Nullable
	private UUID providerId;
	@Nullable
	private UUID appointmentTypeId;
	@Nullable
	private Integer displayOrder;
	@Nullable
	private Integer schedulingStartTimeIntervalInMinutes;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getProviderAppointmentTypeId() {
		return providerAppointmentTypeId;
	}

	public void setProviderAppointmentTypeId(@Nullable UUID providerAppointmentTypeId) {
		this.providerAppointmentTypeId = providerAppointmentTypeId;
	}

	@Nullable
	public UUID getProviderId() {
		return providerId;
	}

	public void setProviderId(@Nullable UUID providerId) {
		this.providerId = providerId;
	}

	@Nullable
	public UUID getAppointmentTypeId() {
		return appointmentTypeId;
	}

	public void setAppointmentTypeId(@Nullable UUID appointmentTypeId) {
		this.appointmentTypeId = appointmentTypeId;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(@Nullable Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	@Nullable
	public Integer getSchedulingStartTimeIntervalInMinutes() {
		return schedulingStartTimeIntervalInMinutes;
	}

	public void setSchedulingStartTimeIntervalInMinutes(@Nullable Integer schedulingStartTimeIntervalInMinutes) {
		this.schedulingStartTimeIntervalInMinutes = schedulingStartTimeIntervalInMinutes;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}
