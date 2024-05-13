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
import com.cobaltplatform.api.model.db.PatientOrderScheduledOutreach;
import com.cobaltplatform.api.model.db.PatientOrderScheduledOutreachReason.PatientOrderScheduledFollowupContactTypeId;
import com.cobaltplatform.api.model.db.PatientOrderScheduledOutreachStatus.PatientOrderScheduledFollowupStatusId;
import com.cobaltplatform.api.model.db.PatientOrderScheduledFollowupType.PatientOrderScheduledFollowupTypeId;
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
import java.time.format.FormatStyle;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class PatientOrderScheduledFollowupApiResponse {
	@Nonnull
	private final UUID patientOrderScheduledFollowupId;
	@Nonnull
	private final UUID patientOrderId;
	@Nonnull
	private final PatientOrderScheduledFollowupStatusId patientOrderScheduledFollowupStatusId;
	@Nonnull
	private final PatientOrderScheduledFollowupTypeId patientOrderScheduledFollowupTypeId;
	@Nonnull
	private final PatientOrderScheduledFollowupContactTypeId patientOrderScheduledFollowupContactTypeId;
	@Nullable
	private final UUID createdByAccountId;
	@Nullable
	private final UUID completedByAccountId;
	@Nonnull
	private final String message;
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
	public interface PatientOrderScheduledFollowupApiResponseFactory {
		@Nonnull
		PatientOrderScheduledFollowupApiResponse create(@Nonnull PatientOrderScheduledOutreach patientOrderScheduledFollowup);
	}

	@AssistedInject
	public PatientOrderScheduledFollowupApiResponse(@Nonnull Provider<CurrentContext> currentContextProvider,
																									@Nonnull Formatter formatter,
																									@Assisted @Nonnull PatientOrderScheduledOutreach patientOrderScheduledFollowup) {
		requireNonNull(currentContextProvider);
		requireNonNull(formatter);
		requireNonNull(patientOrderScheduledFollowup);

		this.patientOrderScheduledFollowupId = patientOrderScheduledFollowup.getPatientOrderScheduledFollowupId();
		this.patientOrderId = patientOrderScheduledFollowup.getPatientOrderId();
		this.patientOrderScheduledFollowupStatusId = patientOrderScheduledFollowup.getPatientOrderScheduledFollowupStatusId();
		this.patientOrderScheduledFollowupTypeId = patientOrderScheduledFollowup.getPatientOrderScheduledFollowupTypeId();
		this.patientOrderScheduledFollowupContactTypeId = patientOrderScheduledFollowup.getPatientOrderScheduledFollowupContactTypeId();
		this.createdByAccountId = patientOrderScheduledFollowup.getCreatedByAccountId();
		this.completedByAccountId = patientOrderScheduledFollowup.getCompletedByAccountId();
		this.message = patientOrderScheduledFollowup.getMessage();
		this.completedAt = patientOrderScheduledFollowup.getCompletedAt();
		this.completedAtDescription = patientOrderScheduledFollowup.getCompletedAt() == null ? null : formatter.formatTimestamp(patientOrderScheduledFollowup.getCompletedAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.created = patientOrderScheduledFollowup.getCreated();
		this.createdDescription = formatter.formatTimestamp(patientOrderScheduledFollowup.getCreated(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.lastUpdated = patientOrderScheduledFollowup.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(patientOrderScheduledFollowup.getLastUpdated(), FormatStyle.MEDIUM, FormatStyle.SHORT);

		this.createdByAccountFirstName = patientOrderScheduledFollowup.getCreatedByAccountFirstName();
		this.createdByAccountLastName = patientOrderScheduledFollowup.getCreatedByAccountLastName();
		this.createdByAccountDisplayName = Normalizer.normalizeName(patientOrderScheduledFollowup.getCreatedByAccountFirstName(), patientOrderScheduledFollowup.getCreatedByAccountLastName()).orElse(null);
		this.createdByAccountDisplayNameWithLastFirst = Normalizer.normalizeNameWithLastFirst(patientOrderScheduledFollowup.getCreatedByAccountFirstName(), patientOrderScheduledFollowup.getCreatedByAccountLastName()).orElse(null);

		this.completedByAccountFirstName = patientOrderScheduledFollowup.getCompletedByAccountFirstName();
		this.completedByAccountLastName = patientOrderScheduledFollowup.getCompletedByAccountLastName();
		this.completedByAccountDisplayName = Normalizer.normalizeName(patientOrderScheduledFollowup.getCompletedByAccountFirstName(), patientOrderScheduledFollowup.getCompletedByAccountLastName()).orElse(null);
		this.completedByAccountDisplayNameWithLastFirst = Normalizer.normalizeNameWithLastFirst(patientOrderScheduledFollowup.getCompletedByAccountFirstName(), patientOrderScheduledFollowup.getCompletedByAccountLastName()).orElse(null);
	}

	@Nonnull
	public UUID getPatientOrderScheduledFollowupId() {
		return this.patientOrderScheduledFollowupId;
	}

	@Nonnull
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	@Nonnull
	public PatientOrderScheduledFollowupStatusId getPatientOrderScheduledFollowupStatusId() {
		return this.patientOrderScheduledFollowupStatusId;
	}

	@Nonnull
	public PatientOrderScheduledFollowupTypeId getPatientOrderScheduledFollowupTypeId() {
		return this.patientOrderScheduledFollowupTypeId;
	}

	@Nonnull
	public PatientOrderScheduledFollowupContactTypeId getPatientOrderScheduledFollowupContactTypeId() {
		return this.patientOrderScheduledFollowupContactTypeId;
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