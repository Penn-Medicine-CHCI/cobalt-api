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
public class Attachment extends Way2HealthEntity {
	@Nullable
	private Long attachmentId;
	@Nullable
	private Long objectId;
	@Nullable
	private String objectType;
	@Nullable
	private Details details;

	@NotThreadSafe
	public static class Details extends Way2HealthEntity {
		@Nullable
		private Long id;
		@Nullable
		private String status;
		@Nullable
		private String type;
		@Nullable
		private Boolean completed;
		@Nullable
		private LocalDateTime completedAt;
		@Nullable
		private LocalDateTime startDate;
		@Nullable
		private LocalDateTime endDate;
		@Nullable
		private Long enrollmentStepId;
		@Nullable
		private Long templateEventId;
		@Nullable
		private Boolean feedbackApplied;
		@Nullable
		private Boolean started;
		@Nullable
		private LocalDateTime startedAt;
		@Nullable
		private Boolean eventsCreated;
		@Nullable
		private Long studyUserScheduleId;
		@Nullable
		private String apiId;
		@Nullable
		private Event event;

		@Nullable
		public Long getId() {
			return id;
		}

		public void setId(@Nullable Long id) {
			this.id = id;
		}

		@Nullable
		public String getStatus() {
			return status;
		}

		public void setStatus(@Nullable String status) {
			this.status = status;
		}

		@Nullable
		public String getType() {
			return type;
		}

		public void setType(@Nullable String type) {
			this.type = type;
		}

		@Nullable
		public Boolean getCompleted() {
			return completed;
		}

		public void setCompleted(@Nullable Boolean completed) {
			this.completed = completed;
		}

		@Nullable
		public LocalDateTime getCompletedAt() {
			return completedAt;
		}

		public void setCompletedAt(@Nullable LocalDateTime completedAt) {
			this.completedAt = completedAt;
		}

		@Nullable
		public LocalDateTime getStartDate() {
			return startDate;
		}

		public void setStartDate(@Nullable LocalDateTime startDate) {
			this.startDate = startDate;
		}

		@Nullable
		public LocalDateTime getEndDate() {
			return endDate;
		}

		public void setEndDate(@Nullable LocalDateTime endDate) {
			this.endDate = endDate;
		}

		@Nullable
		public Long getEnrollmentStepId() {
			return enrollmentStepId;
		}

		public void setEnrollmentStepId(@Nullable Long enrollmentStepId) {
			this.enrollmentStepId = enrollmentStepId;
		}

		@Nullable
		public Long getTemplateEventId() {
			return templateEventId;
		}

		public void setTemplateEventId(@Nullable Long templateEventId) {
			this.templateEventId = templateEventId;
		}

		@Nullable
		public Boolean getFeedbackApplied() {
			return feedbackApplied;
		}

		public void setFeedbackApplied(@Nullable Boolean feedbackApplied) {
			this.feedbackApplied = feedbackApplied;
		}

		@Nullable
		public Boolean getStarted() {
			return started;
		}

		public void setStarted(@Nullable Boolean started) {
			this.started = started;
		}

		@Nullable
		public LocalDateTime getStartedAt() {
			return startedAt;
		}

		public void setStartedAt(@Nullable LocalDateTime startedAt) {
			this.startedAt = startedAt;
		}

		@Nullable
		public Boolean getEventsCreated() {
			return eventsCreated;
		}

		public void setEventsCreated(@Nullable Boolean eventsCreated) {
			this.eventsCreated = eventsCreated;
		}

		@Nullable
		public Long getStudyUserScheduleId() {
			return studyUserScheduleId;
		}

		public void setStudyUserScheduleId(@Nullable Long studyUserScheduleId) {
			this.studyUserScheduleId = studyUserScheduleId;
		}

		@Nullable
		public String getApiId() {
			return apiId;
		}

		public void setApiId(@Nullable String apiId) {
			this.apiId = apiId;
		}

		@Nullable
		public Event getEvent() {
			return event;
		}

		public void setEvent(@Nullable Event event) {
			this.event = event;
		}
	}

	@Nullable
	public Long getAttachmentId() {
		return attachmentId;
	}

	public void setAttachmentId(@Nullable Long attachmentId) {
		this.attachmentId = attachmentId;
	}

	@Nullable
	public Long getObjectId() {
		return objectId;
	}

	public void setObjectId(@Nullable Long objectId) {
		this.objectId = objectId;
	}

	@Nullable
	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(@Nullable String objectType) {
		this.objectType = objectType;
	}

	@Nullable
	public Details getDetails() {
		return details;
	}

	public void setDetails(@Nullable Details details) {
		this.details = details;
	}
}
