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
import java.time.LocalDateTime;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Variable extends Way2HealthEntity {
	@Nullable
	private Long id;
	@Nullable
	private Long participantId;
	@Nullable
	private Long variableId;
	@Nullable
	private String variableName;
	@Nullable
	private String value;
	@Nullable
	private String formattedValue;
	@Nullable
	private LocalDateTime asOf;
	@Nullable
	private Long changedByUserId;
	@Nullable
	private Boolean isManual;
	@Nullable
	private Long feedbackEventId;
	@Nullable
	private String changedBy;
	@Nullable
	private String changeReason;
	@Nullable
	private Participant participant;
	@Nullable
	private FeedbackEvent feedbackEvent;

	@Nullable
	public Long getId() {
		return id;
	}

	public void setId(@Nullable Long id) {
		this.id = id;
	}

	@Nullable
	public Long getParticipantId() {
		return participantId;
	}

	public void setParticipantId(@Nullable Long participantId) {
		this.participantId = participantId;
	}

	@Nullable
	public Long getVariableId() {
		return variableId;
	}

	public void setVariableId(@Nullable Long variableId) {
		this.variableId = variableId;
	}

	@Nullable
	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(@Nullable String variableName) {
		this.variableName = variableName;
	}

	@Nullable
	public String getValue() {
		return value;
	}

	public void setValue(@Nullable String value) {
		this.value = value;
	}

	@Nullable
	public String getFormattedValue() {
		return formattedValue;
	}

	public void setFormattedValue(@Nullable String formattedValue) {
		this.formattedValue = formattedValue;
	}

	@Nullable
	public LocalDateTime getAsOf() {
		return asOf;
	}

	public void setAsOf(@Nullable LocalDateTime asOf) {
		this.asOf = asOf;
	}

	@Nullable
	public Long getChangedByUserId() {
		return changedByUserId;
	}

	public void setChangedByUserId(@Nullable Long changedByUserId) {
		this.changedByUserId = changedByUserId;
	}

	@Nullable
	public Boolean getManual() {
		return isManual;
	}

	public void setManual(@Nullable Boolean manual) {
		isManual = manual;
	}

	@Nullable
	public Long getFeedbackEventId() {
		return feedbackEventId;
	}

	public void setFeedbackEventId(@Nullable Long feedbackEventId) {
		this.feedbackEventId = feedbackEventId;
	}

	@Nullable
	public String getChangedBy() {
		return changedBy;
	}

	public void setChangedBy(@Nullable String changedBy) {
		this.changedBy = changedBy;
	}

	@Nullable
	public String getChangeReason() {
		return changeReason;
	}

	public void setChangeReason(@Nullable String changeReason) {
		this.changeReason = changeReason;
	}

	@Nullable
	public Participant getParticipant() {
		return participant;
	}

	public void setParticipant(@Nullable Participant participant) {
		this.participant = participant;
	}

	@Nullable
	public FeedbackEvent getFeedbackEvent() {
		return feedbackEvent;
	}

	public void setFeedbackEvent(@Nullable FeedbackEvent feedbackEvent) {
		this.feedbackEvent = feedbackEvent;
	}
}
