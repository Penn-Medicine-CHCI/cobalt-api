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

import com.cobaltplatform.api.model.db.PatientOrderScheduledScreening;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.FormatStyle;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PatientOrderScheduledScreeningApiResponse {
	@Nonnull
	private final UUID patientOrderScheduledScreeningId;
	@Nonnull
	private final UUID patientOrderId;
	@Nonnull
	private final UUID accountId;
	@Nonnull
	private final LocalDateTime scheduledDateTime;
	@Nonnull
	private final String scheduledDateTimeDescription;
	@Nullable
	private final String calendarUrl;
	@Nonnull
	private final Boolean canceled;
	@Nullable
	private final Instant canceledAt;
	@Nullable
	private final String canceledAtDescription;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final Instant lastUpdated;
	@Nonnull
	private final String lastUpdatedDescription;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PatientOrderScheduledScreeningApiResponseFactory {
		@Nonnull
		PatientOrderScheduledScreeningApiResponse create(@Nonnull PatientOrderScheduledScreening patientOrderScheduledScreening);
	}

	@AssistedInject
	public PatientOrderScheduledScreeningApiResponse(@Nonnull Formatter formatter,
																									 @Nonnull Strings strings,
																									 @Assisted @Nonnull PatientOrderScheduledScreening patientOrderScheduledScreening) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(patientOrderScheduledScreening);


		this.patientOrderScheduledScreeningId = patientOrderScheduledScreening.getPatientOrderScheduledScreeningId();
		this.patientOrderId = patientOrderScheduledScreening.getPatientOrderId();
		this.accountId = patientOrderScheduledScreening.getAccountId();
		this.scheduledDateTime = patientOrderScheduledScreening.getScheduledDateTime();
		this.scheduledDateTimeDescription = formatter.formatDateTime(patientOrderScheduledScreening.getScheduledDateTime(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.calendarUrl = patientOrderScheduledScreening.getCalendarUrl();
		this.canceled = patientOrderScheduledScreening.getCanceled();
		this.canceledAt = patientOrderScheduledScreening.getCanceledAt();
		this.canceledAtDescription = patientOrderScheduledScreening.getCanceledAt() == null ? null : formatter.formatTimestamp(patientOrderScheduledScreening.getCanceledAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.created = patientOrderScheduledScreening.getCreated();
		this.createdDescription = formatter.formatTimestamp(patientOrderScheduledScreening.getCreated(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.lastUpdated = patientOrderScheduledScreening.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(patientOrderScheduledScreening.getLastUpdated(), FormatStyle.MEDIUM, FormatStyle.SHORT);
	}

	@Nonnull
	public UUID getPatientOrderScheduledScreeningId() {
		return this.patientOrderScheduledScreeningId;
	}

	@Nonnull
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	@Nonnull
	public UUID getAccountId() {
		return this.accountId;
	}

	@Nonnull
	public LocalDateTime getScheduledDateTime() {
		return this.scheduledDateTime;
	}

	@Nonnull
	public String getScheduledDateTimeDescription() {
		return this.scheduledDateTimeDescription;
	}

	@Nonnull
	public Optional<String> getCalendarUrl() {
		return Optional.ofNullable(this.calendarUrl);
	}

	@Nonnull
	public Boolean getCanceled() {
		return this.canceled;
	}

	@Nonnull
	public Optional<Instant> getCanceledAt() {
		return Optional.ofNullable(this.canceledAt);
	}

	@Nonnull
	public Optional<String> getCanceledAtDescription() {
		return Optional.ofNullable(this.canceledAtDescription);
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
}