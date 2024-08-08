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

import com.cobaltplatform.api.model.db.AppointmentScheduledMessageType.AppointmentScheduledMessageTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AppointmentScheduledMessage {
	@Nullable
	private UUID appointmentScheduledMessageId;
	@Nullable
	private AppointmentScheduledMessageTypeId appointmentScheduledMessageTypeId;
	@Nullable
	private UUID appointmentId;
	@Nullable
	private UUID scheduledMessageId;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getAppointmentScheduledMessageId() {
		return this.appointmentScheduledMessageId;
	}

	public void setAppointmentScheduledMessageId(@Nullable UUID appointmentScheduledMessageId) {
		this.appointmentScheduledMessageId = appointmentScheduledMessageId;
	}

	@Nullable
	public AppointmentScheduledMessageTypeId getAppointmentScheduledMessageTypeId() {
		return this.appointmentScheduledMessageTypeId;
	}

	public void setAppointmentScheduledMessageTypeId(@Nullable AppointmentScheduledMessageTypeId appointmentScheduledMessageTypeId) {
		this.appointmentScheduledMessageTypeId = appointmentScheduledMessageTypeId;
	}

	@Nullable
	public UUID getAppointmentId() {
		return this.appointmentId;
	}

	public void setAppointmentId(@Nullable UUID appointmentId) {
		this.appointmentId = appointmentId;
	}

	@Nullable
	public UUID getScheduledMessageId() {
		return this.scheduledMessageId;
	}

	public void setScheduledMessageId(@Nullable UUID scheduledMessageId) {
		this.scheduledMessageId = scheduledMessageId;
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
