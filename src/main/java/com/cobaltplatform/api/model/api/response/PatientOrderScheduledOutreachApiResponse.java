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

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.PatientOrderOutreachType.PatientOrderOutreachTypeId;
import com.cobaltplatform.api.model.db.PatientOrderScheduledOutreach;
import com.cobaltplatform.api.model.db.PatientOrderScheduledOutreachReason.PatientOrderScheduledOutreachReasonId;
import com.cobaltplatform.api.model.db.PatientOrderScheduledOutreachStatus.PatientOrderScheduledOutreachStatusId;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.Normalizer;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.FormatStyle;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class PatientOrderScheduledOutreachApiResponse {
	@Nonnull
	private final UUID patientOrderScheduledOutreachId;
	@Nonnull
	private final UUID patientOrderId;
	@Nullable
	private PatientOrderOutreachTypeId patientOrderOutreachTypeId;
	@Nullable
	private PatientOrderScheduledOutreachReasonId patientOrderScheduledOutreachReasonId;
	@Nullable
	private PatientOrderScheduledOutreachStatusId patientOrderScheduledOutreachStatusId;
	@Nullable
	private final UUID createdByAccountId;
	@Nullable
	private final UUID completedByAccountId;
	@Nonnull
	private final String message;
	@Nullable
	private final LocalDate scheduledAtDate;
	@Nullable
	private final String scheduledAtDateDescription;
	@Nullable
	private final LocalTime scheduledAtTime;
	@Nullable
	private final String scheduledAtTimeDescription;
	@Nullable
	private final LocalDateTime scheduledAtDateTime;
	@Nullable
	private final String scheduledAtDateTimeDescription;
	@Nullable
	private final Instant completedAt;
	@Nullable
	private final String completedAtDescription;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final Instant lastUpdated;
	@Nonnull
	private final String lastUpdatedDescription;

	@Nullable
	private String createdByAccountFirstName;
	@Nullable
	private String createdByAccountLastName;
	@Nullable
	private String createdByAccountDisplayName;
	@Nullable
	private String createdByAccountDisplayNameWithLastFirst;
	@Nullable
	private String completedByAccountFirstName;
	@Nullable
	private String completedByAccountLastName;
	@Nullable
	private String completedByAccountDisplayName;
	@Nullable
	private String completedByAccountDisplayNameWithLastFirst;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PatientOrderScheduledOutreachApiResponseFactory {
		@Nonnull
		PatientOrderScheduledOutreachApiResponse create(@Nonnull PatientOrderScheduledOutreach patientOrderScheduledOutreach);
	}

	@AssistedInject
	public PatientOrderScheduledOutreachApiResponse(@Nonnull Provider<CurrentContext> currentContextProvider,
																									@Nonnull Formatter formatter,
																									@Assisted @Nonnull PatientOrderScheduledOutreach patientOrderScheduledOutreach) {
		requireNonNull(currentContextProvider);
		requireNonNull(formatter);
		requireNonNull(patientOrderScheduledOutreach);

		this.patientOrderScheduledOutreachId = patientOrderScheduledOutreach.getPatientOrderScheduledOutreachId();
		this.patientOrderId = patientOrderScheduledOutreach.getPatientOrderId();
		this.patientOrderOutreachTypeId = patientOrderScheduledOutreach.getPatientOrderOutreachTypeId();
		this.patientOrderScheduledOutreachReasonId = patientOrderScheduledOutreach.getPatientOrderScheduledOutreachReasonId();
		this.patientOrderScheduledOutreachStatusId = patientOrderScheduledOutreach.getPatientOrderScheduledOutreachStatusId();
		this.createdByAccountId = patientOrderScheduledOutreach.getCreatedByAccountId();
		this.completedByAccountId = patientOrderScheduledOutreach.getCompletedByAccountId();
		this.message = patientOrderScheduledOutreach.getMessage();
		this.scheduledAtDate = patientOrderScheduledOutreach.getScheduledAtDateTime().toLocalDate();
		this.scheduledAtDateDescription = formatter.formatDate(this.scheduledAtDate, FormatStyle.MEDIUM);
		this.scheduledAtTime = patientOrderScheduledOutreach.getScheduledAtDateTime().toLocalTime();
		this.scheduledAtTimeDescription = formatter.formatTime(this.scheduledAtTime, FormatStyle.SHORT);
		this.scheduledAtDateTime = patientOrderScheduledOutreach.getScheduledAtDateTime();
		this.scheduledAtDateTimeDescription = formatter.formatDateTime(patientOrderScheduledOutreach.getScheduledAtDateTime(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.completedAt = patientOrderScheduledOutreach.getCompletedAt();
		this.completedAtDescription = patientOrderScheduledOutreach.getCompletedAt() == null ? null : formatter.formatTimestamp(patientOrderScheduledOutreach.getCompletedAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.created = patientOrderScheduledOutreach.getCreated();
		this.createdDescription = formatter.formatTimestamp(patientOrderScheduledOutreach.getCreated(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.lastUpdated = patientOrderScheduledOutreach.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(patientOrderScheduledOutreach.getLastUpdated(), FormatStyle.MEDIUM, FormatStyle.SHORT);

		this.createdByAccountFirstName = patientOrderScheduledOutreach.getCreatedByAccountFirstName();
		this.createdByAccountLastName = patientOrderScheduledOutreach.getCreatedByAccountLastName();
		this.createdByAccountDisplayName = Normalizer.normalizeName(patientOrderScheduledOutreach.getCreatedByAccountFirstName(), patientOrderScheduledOutreach.getCreatedByAccountLastName()).orElse(null);
		this.createdByAccountDisplayNameWithLastFirst = Normalizer.normalizeNameWithLastFirst(patientOrderScheduledOutreach.getCreatedByAccountFirstName(), patientOrderScheduledOutreach.getCreatedByAccountLastName()).orElse(null);

		this.completedByAccountFirstName = patientOrderScheduledOutreach.getCompletedByAccountFirstName();
		this.completedByAccountLastName = patientOrderScheduledOutreach.getCompletedByAccountLastName();
		this.completedByAccountDisplayName = Normalizer.normalizeName(patientOrderScheduledOutreach.getCompletedByAccountFirstName(), patientOrderScheduledOutreach.getCompletedByAccountLastName()).orElse(null);
		this.completedByAccountDisplayNameWithLastFirst = Normalizer.normalizeNameWithLastFirst(patientOrderScheduledOutreach.getCompletedByAccountFirstName(), patientOrderScheduledOutreach.getCompletedByAccountLastName()).orElse(null);
	}

	@Nonnull
	public UUID getPatientOrderScheduledOutreachId() {
		return this.patientOrderScheduledOutreachId;
	}

	@Nonnull
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	@Nullable
	public PatientOrderOutreachTypeId getPatientOrderOutreachTypeId() {
		return this.patientOrderOutreachTypeId;
	}

	@Nullable
	public PatientOrderScheduledOutreachReasonId getPatientOrderScheduledOutreachReasonId() {
		return this.patientOrderScheduledOutreachReasonId;
	}

	@Nullable
	public PatientOrderScheduledOutreachStatusId getPatientOrderScheduledOutreachStatusId() {
		return this.patientOrderScheduledOutreachStatusId;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	@Nullable
	public UUID getCompletedByAccountId() {
		return this.completedByAccountId;
	}

	@Nonnull
	public String getMessage() {
		return this.message;
	}

	@Nullable
	public Instant getCompletedAt() {
		return this.completedAt;
	}

	@Nullable
	public String getCompletedAtDescription() {
		return this.completedAtDescription;
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

	@Nullable
	public String getCreatedByAccountFirstName() {
		return this.createdByAccountFirstName;
	}

	@Nullable
	public String getCreatedByAccountLastName() {
		return this.createdByAccountLastName;
	}

	@Nullable
	public String getCreatedByAccountDisplayName() {
		return this.createdByAccountDisplayName;
	}

	@Nullable
	public String getCreatedByAccountDisplayNameWithLastFirst() {
		return this.createdByAccountDisplayNameWithLastFirst;
	}

	@Nullable
	public String getCompletedByAccountFirstName() {
		return this.completedByAccountFirstName;
	}

	@Nullable
	public String getCompletedByAccountLastName() {
		return this.completedByAccountLastName;
	}

	@Nullable
	public String getCompletedByAccountDisplayName() {
		return this.completedByAccountDisplayName;
	}

	@Nullable
	public String getCompletedByAccountDisplayNameWithLastFirst() {
		return this.completedByAccountDisplayNameWithLastFirst;
	}
}