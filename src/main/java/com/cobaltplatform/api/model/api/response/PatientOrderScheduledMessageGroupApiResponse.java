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

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.model.api.response.PatientOrderScheduledMessageApiResponse.PatientOrderScheduledMessageApiResponseFactory;
import com.cobaltplatform.api.model.db.PatientOrderScheduledMessage;
import com.cobaltplatform.api.model.db.PatientOrderScheduledMessageGroup;
import com.cobaltplatform.api.model.db.PatientOrderScheduledMessageType.PatientOrderScheduledMessageTypeId;
import com.cobaltplatform.api.model.db.ScheduledMessageSource.ScheduledMessageSourceId;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PatientOrderScheduledMessageGroupApiResponse {
	@Nonnull
	private final UUID patientOrderScheduledMessageGroupId;
	@Nonnull
	private final PatientOrderScheduledMessageTypeId patientOrderScheduledMessageTypeId;
	@Nonnull
	private final String patientOrderScheduledMessageTypeDescription;
	@Nonnull
	private final ScheduledMessageSourceId scheduledMessageSourceId;
	@Nonnull
	private final UUID patientOrderId;
	@Nullable
	private final UUID scheduledByAccountId;
	@Nonnull
	private final LocalDate scheduledAtDate;
	@Nonnull
	private final String scheduledAtDateDescription;
	@Nonnull
	private final LocalTime scheduledAtTime;
	@Nonnull
	private final String scheduledAtTimeDescription;
	@Nonnull
	private final LocalDateTime scheduledAtDateTime;
	@Nonnull
	private final String scheduledAtDateTimeDescription;
	@Nonnull
	private final Boolean scheduledAtDateTimeHasPassed;
	@Nonnull
	private final Boolean atLeastOneMessageDelivered;
	@Nonnull
	private final ZoneId timeZone;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final Instant lastUpdated;
	@Nonnull
	private final String lastUpdatedDescription;
	@Nonnull
	private final List<PatientOrderScheduledMessageApiResponse> patientOrderScheduledMessages;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PatientOrderScheduledMessageGroupApiResponseFactory {
		@Nonnull
		PatientOrderScheduledMessageGroupApiResponse create(@Nonnull PatientOrderScheduledMessageGroup patientOrderScheduledMessageGroup,
																												@Nonnull List<PatientOrderScheduledMessage> patientOrderScheduledMessages);
	}

	@AssistedInject
	public PatientOrderScheduledMessageGroupApiResponse(@Nonnull Formatter formatter,
																											@Nonnull Strings strings,
																											@Nonnull PatientOrderScheduledMessageApiResponseFactory patientOrderScheduledMessageApiResponseFactory,
																											@Assisted @Nonnull PatientOrderScheduledMessageGroup patientOrderScheduledMessageGroup,
																											@Assisted @Nonnull List<PatientOrderScheduledMessage> patientOrderScheduledMessages) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(patientOrderScheduledMessageApiResponseFactory);
		requireNonNull(patientOrderScheduledMessageGroup);

		this.patientOrderScheduledMessageGroupId = patientOrderScheduledMessageGroup.getPatientOrderScheduledMessageGroupId();
		this.patientOrderScheduledMessageTypeId = patientOrderScheduledMessageGroup.getPatientOrderScheduledMessageTypeId();
		this.patientOrderId = patientOrderScheduledMessageGroup.getPatientOrderId();

		if (patientOrderScheduledMessages.size() == 0)
			throw new IllegalStateException(format("Unexpected empty group of messages for patient_order_scheduled_message_group_id %s", patientOrderScheduledMessageGroup.getPatientOrderScheduledMessageGroupId()));

		// Pick an arbitrary message to pull some toplevel data out of.
		// We can do this because all of the messages in the group should have identical settings (timezone, scheduled time, etc.)
		// It's easier for the FE to work with this b/c it matches well with the UI
		PatientOrderScheduledMessage arbitraryPatientOrderScheduledMessage = patientOrderScheduledMessages.get(0);

		this.scheduledByAccountId = arbitraryPatientOrderScheduledMessage.getScheduledByAccountId();
		this.patientOrderScheduledMessageTypeDescription = arbitraryPatientOrderScheduledMessage.getPatientOrderScheduledMessageTypeDescription();
		this.scheduledMessageSourceId = arbitraryPatientOrderScheduledMessage.getScheduledMessageSourceId();
		this.timeZone = arbitraryPatientOrderScheduledMessage.getTimeZone();
		this.scheduledAtDate = arbitraryPatientOrderScheduledMessage.getScheduledAt().toLocalDate();
		this.scheduledAtDateDescription = formatter.formatDate(arbitraryPatientOrderScheduledMessage.getScheduledAt().toLocalDate(), FormatStyle.MEDIUM);
		this.scheduledAtTime = arbitraryPatientOrderScheduledMessage.getScheduledAt().toLocalTime();
		this.scheduledAtTimeDescription = formatter.formatTime(arbitraryPatientOrderScheduledMessage.getScheduledAt().toLocalTime(), FormatStyle.SHORT);
		this.scheduledAtDateTime = arbitraryPatientOrderScheduledMessage.getScheduledAt();
		this.scheduledAtDateTimeDescription = formatter.formatDateTime(arbitraryPatientOrderScheduledMessage.getScheduledAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.scheduledAtDateTimeHasPassed = patientOrderScheduledMessageGroup.getScheduledAtDateTimeHasPassed();
		this.atLeastOneMessageDelivered = patientOrderScheduledMessageGroup.getAtLeastOneMessageDelivered();
		this.created = arbitraryPatientOrderScheduledMessage.getCreated();
		this.createdDescription = formatter.formatTimestamp(arbitraryPatientOrderScheduledMessage.getCreated());
		this.lastUpdated = arbitraryPatientOrderScheduledMessage.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(arbitraryPatientOrderScheduledMessage.getLastUpdated());
		this.patientOrderScheduledMessages = patientOrderScheduledMessages.stream()
				.map(patientOrderScheduledMessage -> patientOrderScheduledMessageApiResponseFactory.create(patientOrderScheduledMessage))
				.collect(Collectors.toList());
	}

	@Nonnull
	public UUID getPatientOrderScheduledMessageGroupId() {
		return this.patientOrderScheduledMessageGroupId;
	}

	@Nonnull
	public PatientOrderScheduledMessageTypeId getPatientOrderScheduledMessageTypeId() {
		return this.patientOrderScheduledMessageTypeId;
	}

	@Nonnull
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	@Nonnull
	public LocalDate getScheduledAtDate() {
		return this.scheduledAtDate;
	}

	@Nonnull
	public String getScheduledAtDateDescription() {
		return this.scheduledAtDateDescription;
	}

	@Nonnull
	public LocalTime getScheduledAtTime() {
		return this.scheduledAtTime;
	}

	@Nonnull
	public String getScheduledAtTimeDescription() {
		return this.scheduledAtTimeDescription;
	}

	@Nonnull
	public LocalDateTime getScheduledAtDateTime() {
		return this.scheduledAtDateTime;
	}

	@Nonnull
	public String getScheduledAtDateTimeDescription() {
		return this.scheduledAtDateTimeDescription;
	}

	@Nonnull
	public Boolean getScheduledAtDateTimeHasPassed() {
		return this.scheduledAtDateTimeHasPassed;
	}

	@Nonnull
	public Boolean getAtLeastOneMessageDelivered() {
		return this.atLeastOneMessageDelivered;
	}

	@Nonnull
	public Instant getCreated() {
		return this.created;
	}

	@Nonnull
	public String getCreatedDescription() {
		return this.createdDescription;
	}

	@Nonnull
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	@Nonnull
	public String getLastUpdatedDescription() {
		return this.lastUpdatedDescription;
	}

	@Nonnull
	public String getPatientOrderScheduledMessageTypeDescription() {
		return this.patientOrderScheduledMessageTypeDescription;
	}

	@Nonnull
	public ScheduledMessageSourceId getScheduledMessageSourceId() {
		return this.scheduledMessageSourceId;
	}

	@Nullable
	public UUID getScheduledByAccountId() {
		return this.scheduledByAccountId;
	}

	@Nonnull
	public ZoneId getTimeZone() {
		return this.timeZone;
	}

	@Nonnull
	public List<PatientOrderScheduledMessageApiResponse> getPatientOrderScheduledMessages() {
		return this.patientOrderScheduledMessages;
	}
}