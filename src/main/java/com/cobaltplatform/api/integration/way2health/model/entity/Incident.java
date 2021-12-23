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
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Incident extends Way2HealthEntity {
	@Nullable
	private BigInteger id;
	@Nullable
	private BigInteger participantId;
	@Nullable
	private BigInteger reporterId;
	@Nullable
	private String type;
	@Nullable
	private String status;
	@Nullable
	private BigInteger studyId;
	@Nullable
	private LocalDateTime createdAt;
	@Nullable
	private Participant participant;
	@Nullable
	private List<Comment> comments;
	@Nullable
	private List<Tag> tags;
	@Nullable
	private List<Attachment> attachments;

	@Nullable
	public BigInteger getId() {
		return id;
	}

	public void setId(@Nullable BigInteger id) {
		this.id = id;
	}

	@Nullable
	public BigInteger getParticipantId() {
		return participantId;
	}

	public void setParticipantId(@Nullable BigInteger participantId) {
		this.participantId = participantId;
	}

	@Nullable
	public BigInteger getReporterId() {
		return reporterId;
	}

	public void setReporterId(@Nullable BigInteger reporterId) {
		this.reporterId = reporterId;
	}

	@Nullable
	public String getType() {
		return type;
	}

	public void setType(@Nullable String type) {
		this.type = type;
	}

	@Nullable
	public String getStatus() {
		return status;
	}

	public void setStatus(@Nullable String status) {
		this.status = status;
	}

	@Nullable
	public BigInteger getStudyId() {
		return studyId;
	}

	public void setStudyId(@Nullable BigInteger studyId) {
		this.studyId = studyId;
	}

	@Nullable
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(@Nullable LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	@Nullable
	public Participant getParticipant() {
		return participant;
	}

	public void setParticipant(@Nullable Participant participant) {
		this.participant = participant;
	}

	@Nullable
	public List<Comment> getComments() {
		return comments;
	}

	public void setComments(@Nullable List<Comment> comments) {
		this.comments = comments;
	}

	@Nullable
	public List<Tag> getTags() {
		return tags;
	}

	public void setTags(@Nullable List<Tag> tags) {
		this.tags = tags;
	}

	@Nullable
	public List<Attachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(@Nullable List<Attachment> attachments) {
		this.attachments = attachments;
	}
}
