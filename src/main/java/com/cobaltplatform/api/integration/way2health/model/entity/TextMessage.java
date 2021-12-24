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
public class TextMessage extends Way2HealthEntity {
	@Nullable
	private BigInteger id;
	@Nullable
	private LocalDateTime createdAt;
	@Nullable
	private LocalDateTime updatedAt;
	@Nullable
	private BigInteger participantId;
	@Nullable
	private String messageText;
	@Nullable
	private String direction;
	@Nullable
	private String status;
	@Nullable
	private String fromNumber;
	@Nullable
	private String toNumber;
	@Nullable
	private String sender;
	@Nullable
	private String errorCode;
	@Nullable
	private String errorMessage;
	@Nullable
	private Integer numMedia;
	@Nullable
	private String mediaUrl;
	@Nullable
	private List<Media> media;
	@Nullable
	private String messageSid;
	@Nullable
	private String messagingServiceSid;
	@Nullable
	private LocalDateTime sentAt;
	@Nullable
	private List<Metadata> metadata;

	@NotThreadSafe
	public static class Metadata extends Way2HealthEntity {
		@Nullable
		private BigInteger triggerId;
		@Nullable
		private String message;
		@Nullable
		private State previousState;
		@Nullable
		private State nextState;
		@Nullable
		private Params params;
		@Nullable
		private Result result;

		@NotThreadSafe
		public static class Params extends Way2HealthEntity {
			@Nullable
			private String url;
			@Nullable
			private String intent;
			@Nullable
			private String confidence;

			@Nullable
			public String getUrl() {
				return url;
			}

			public void setUrl(@Nullable String url) {
				this.url = url;
			}

			@Nullable
			public String getIntent() {
				return intent;
			}

			public void setIntent(@Nullable String intent) {
				this.intent = intent;
			}

			@Nullable
			public String getConfidence() {
				return confidence;
			}

			public void setConfidence(@Nullable String confidence) {
				this.confidence = confidence;
			}
		}

		@NotThreadSafe
		public static class Result extends Way2HealthEntity {
			@Nullable
			private String text;
			@Nullable
			private String intent;
			@Nullable
			private String intentRanking;

			@Nullable
			public String getText() {
				return text;
			}

			public void setText(@Nullable String text) {
				this.text = text;
			}

			@Nullable
			public String getIntent() {
				return intent;
			}

			public void setIntent(@Nullable String intent) {
				this.intent = intent;
			}

			@Nullable
			public String getIntentRanking() {
				return intentRanking;
			}

			public void setIntentRanking(@Nullable String intentRanking) {
				this.intentRanking = intentRanking;
			}
		}

		@NotThreadSafe
		public static class State extends Way2HealthEntity {
			@Nullable
			private String values;
			@Nullable
			private String seenMessages;

			@Nullable
			public String getValues() {
				return values;
			}

			public void setValues(@Nullable String values) {
				this.values = values;
			}

			@Nullable
			public String getSeenMessages() {
				return seenMessages;
			}

			public void setSeenMessages(@Nullable String seenMessages) {
				this.seenMessages = seenMessages;
			}
		}
	}

	@NotThreadSafe
	public static class Media extends Way2HealthEntity {
		@Nullable
		private String contentType;
		@Nullable
		private String uri;
		@Nullable
		private String size;
		@Nullable
		private Long fileSize;

		@Nullable
		public String getContentType() {
			return contentType;
		}

		public void setContentType(@Nullable String contentType) {
			this.contentType = contentType;
		}

		@Nullable
		public String getUri() {
			return uri;
		}

		public void setUri(@Nullable String uri) {
			this.uri = uri;
		}

		@Nullable
		public String getSize() {
			return size;
		}

		public void setSize(@Nullable String size) {
			this.size = size;
		}

		@Nullable
		public Long getFileSize() {
			return fileSize;
		}

		public void setFileSize(@Nullable Long fileSize) {
			this.fileSize = fileSize;
		}
	}

	@Nullable
	public BigInteger getId() {
		return id;
	}

	public void setId(@Nullable BigInteger id) {
		this.id = id;
	}

	@Nullable
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(@Nullable LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	@Nullable
	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(@Nullable LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	@Nullable
	public BigInteger getParticipantId() {
		return participantId;
	}

	public void setParticipantId(@Nullable BigInteger participantId) {
		this.participantId = participantId;
	}

	@Nullable
	public String getMessageText() {
		return messageText;
	}

	public void setMessageText(@Nullable String messageText) {
		this.messageText = messageText;
	}

	@Nullable
	public String getDirection() {
		return direction;
	}

	public void setDirection(@Nullable String direction) {
		this.direction = direction;
	}

	@Nullable
	public String getStatus() {
		return status;
	}

	public void setStatus(@Nullable String status) {
		this.status = status;
	}

	@Nullable
	public String getFromNumber() {
		return fromNumber;
	}

	public void setFromNumber(@Nullable String fromNumber) {
		this.fromNumber = fromNumber;
	}

	@Nullable
	public String getToNumber() {
		return toNumber;
	}

	public void setToNumber(@Nullable String toNumber) {
		this.toNumber = toNumber;
	}

	@Nullable
	public String getSender() {
		return sender;
	}

	public void setSender(@Nullable String sender) {
		this.sender = sender;
	}

	@Nullable
	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(@Nullable String errorCode) {
		this.errorCode = errorCode;
	}

	@Nullable
	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(@Nullable String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@Nullable
	public Integer getNumMedia() {
		return numMedia;
	}

	public void setNumMedia(@Nullable Integer numMedia) {
		this.numMedia = numMedia;
	}

	@Nullable
	public String getMediaUrl() {
		return mediaUrl;
	}

	public void setMediaUrl(@Nullable String mediaUrl) {
		this.mediaUrl = mediaUrl;
	}

	@Nullable
	public List<Media> getMedia() {
		return media;
	}

	public void setMedia(@Nullable List<Media> media) {
		this.media = media;
	}

	@Nullable
	public String getMessageSid() {
		return messageSid;
	}

	public void setMessageSid(@Nullable String messageSid) {
		this.messageSid = messageSid;
	}

	@Nullable
	public String getMessagingServiceSid() {
		return messagingServiceSid;
	}

	public void setMessagingServiceSid(@Nullable String messagingServiceSid) {
		this.messagingServiceSid = messagingServiceSid;
	}

	@Nullable
	public LocalDateTime getSentAt() {
		return sentAt;
	}

	public void setSentAt(@Nullable LocalDateTime sentAt) {
		this.sentAt = sentAt;
	}

	@Nullable
	public List<Metadata> getMetadata() {
		return metadata;
	}

	public void setMetadata(@Nullable List<Metadata> metadata) {
		this.metadata = metadata;
	}
}