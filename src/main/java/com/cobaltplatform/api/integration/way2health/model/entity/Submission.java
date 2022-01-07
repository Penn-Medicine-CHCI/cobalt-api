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

package com.cobaltplatform.api.integration.way2health.model.entity;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Submission extends Way2HealthEntity {
	@Nullable
	private Long id;
	@Nullable
	private LocalDateTime asOf;
	@Nullable
	private Long participantId;
	@Nullable
	private List<Field> fields;
	@Nullable
	private LocalDateTime createdAt;
	@Nullable
	private String epicStatus;
	@Nullable
	private Instant sentToEpicAt;
	@Nullable
	private Participant participant;

	@NotThreadSafe
	public static class Field {
		@Nullable
		private String field;
		@Nullable
		private String label;
		@Nullable
		private String value;
		@Nullable
		private String decodedValue;

		@Nullable
		public String getField() {
			return field;
		}

		public void setField(@Nullable String field) {
			this.field = field;
		}

		@Nullable
		public String getLabel() {
			return label;
		}

		public void setLabel(@Nullable String label) {
			this.label = label;
		}

		@Nullable
		public String getValue() {
			return value;
		}

		public void setValue(@Nullable String value) {
			this.value = value;
		}

		@Nullable
		public String getDecodedValue() {
			return decodedValue;
		}

		public void setDecodedValue(@Nullable String decodedValue) {
			this.decodedValue = decodedValue;
		}
	}

	@Nullable
	public Long getId() {
		return id;
	}

	public void setId(@Nullable Long id) {
		this.id = id;
	}

	@Nullable
	public LocalDateTime getAsOf() {
		return asOf;
	}

	public void setAsOf(@Nullable LocalDateTime asOf) {
		this.asOf = asOf;
	}

	@Nullable
	public Long getParticipantId() {
		return participantId;
	}

	public void setParticipantId(@Nullable Long participantId) {
		this.participantId = participantId;
	}

	@Nullable
	public List<Field> getFields() {
		return fields;
	}

	public void setFields(@Nullable List<Field> fields) {
		this.fields = fields;
	}

	@Nullable
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(@Nullable LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	@Nullable
	public String getEpicStatus() {
		return epicStatus;
	}

	public void setEpicStatus(@Nullable String epicStatus) {
		this.epicStatus = epicStatus;
	}

	@Nullable
	public Instant getSentToEpicAt() {
		return sentToEpicAt;
	}

	public void setSentToEpicAt(@Nullable Instant sentToEpicAt) {
		this.sentToEpicAt = sentToEpicAt;
	}

	@Nullable
	public Participant getParticipant() {
		return participant;
	}

	public void setParticipant(@Nullable Participant participant) {
		this.participant = participant;
	}
}
