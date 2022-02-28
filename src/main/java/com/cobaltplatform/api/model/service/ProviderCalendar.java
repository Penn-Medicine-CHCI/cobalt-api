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

package com.cobaltplatform.api.model.service;

import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.Followup;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ProviderCalendar {
	@Nullable
	private UUID providerId;
	@Nullable
	private List<Availability> availabilities;
	@Nullable
	private List<Block> blocks;
	@Nullable
	private List<Followup> followups;

	@Override
	public String toString() {
		return format("%s {\n\tProvider ID: %s\n\tAvailabilities:\n\t\t%s\n\tBlocks:\n\t\t%s\n\tFollowups:\n\t\t%s\n}", getClass().getSimpleName(), getProviderId(),
				(getAvailabilities().size() == 0 ? "[none]" : getAvailabilities().stream().map(availability -> availability.toString()).collect(Collectors.joining("\n\t\t"))),
				(getBlocks().size() == 0 ? "[none]" : getBlocks().stream().map(block -> block.toString()).collect(Collectors.joining("\n\t\t"))),
				(getFollowups().size() == 0 ? "[none]" : getFollowups().stream().map(followup -> followup.toString()).collect(Collectors.joining("\n\t\t"))));
	}

	@NotThreadSafe
	public static class Availability {
		@Nullable
		private UUID logicalAvailabilityId;
		@Nullable
		private LocalDateTime startDateTime;
		@Nullable
		private LocalDateTime endDateTime;
		@Nullable
		private List<AppointmentType> appointmentTypes;

		@Override
		public String toString() {
			return format("%s{%s to %s, appointment types %s}", getClass().getSimpleName(), getStartDateTime(),
					getEndDateTime(), getAppointmentTypes());
		}

		@Nullable
		public UUID getLogicalAvailabilityId() {
			return logicalAvailabilityId;
		}

		public void setLogicalAvailabilityId(@Nullable UUID logicalAvailabilityId) {
			this.logicalAvailabilityId = logicalAvailabilityId;
		}

		@Nullable
		public LocalDateTime getStartDateTime() {
			return startDateTime;
		}

		public void setStartDateTime(@Nullable LocalDateTime startDateTime) {
			this.startDateTime = startDateTime;
		}

		@Nullable
		public LocalDateTime getEndDateTime() {
			return endDateTime;
		}

		public void setEndDateTime(@Nullable LocalDateTime endDateTime) {
			this.endDateTime = endDateTime;
		}

		@Nullable
		public List<AppointmentType> getAppointmentTypes() {
			return appointmentTypes;
		}

		public void setAppointmentTypes(@Nullable List<AppointmentType> appointmentTypes) {
			this.appointmentTypes = appointmentTypes;
		}
	}

	@NotThreadSafe
	public static class Block {
		@Nullable
		private UUID logicalAvailabilityId;
		@Nullable
		private LocalDateTime startDateTime;
		@Nullable
		private LocalDateTime endDateTime;

		@Override
		public String toString() {
			return format("%s{%s to %s}", getClass().getSimpleName(), getStartDateTime(),
					getEndDateTime());
		}

		@Nullable
		public UUID getLogicalAvailabilityId() {
			return logicalAvailabilityId;
		}

		public void setLogicalAvailabilityId(@Nullable UUID logicalAvailabilityId) {
			this.logicalAvailabilityId = logicalAvailabilityId;
		}

		@Nullable
		public LocalDateTime getStartDateTime() {
			return startDateTime;
		}

		public void setStartDateTime(@Nullable LocalDateTime startDateTime) {
			this.startDateTime = startDateTime;
		}

		@Nullable
		public LocalDateTime getEndDateTime() {
			return endDateTime;
		}

		public void setEndDateTime(@Nullable LocalDateTime endDateTime) {
			this.endDateTime = endDateTime;
		}
	}

	@Nullable
	public UUID getProviderId() {
		return providerId;
	}

	public void setProviderId(@Nullable UUID providerId) {
		this.providerId = providerId;
	}

	@Nullable
	public List<Availability> getAvailabilities() {
		return availabilities;
	}

	public void setAvailabilities(@Nullable List<Availability> availabilities) {
		this.availabilities = availabilities;
	}

	@Nullable
	public List<Block> getBlocks() {
		return blocks;
	}

	public void setBlocks(@Nullable List<Block> blocks) {
		this.blocks = blocks;
	}

	@Nullable
	public List<Followup> getFollowups() {
		return followups;
	}

	public void setFollowups(@Nullable List<Followup> followups) {
		this.followups = followups;
	}
}
