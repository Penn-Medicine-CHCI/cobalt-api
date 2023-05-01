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
import com.cobaltplatform.api.model.db.PatientOrderVoicemailTask;
import com.cobaltplatform.api.util.Formatter;
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
public class PatientOrderVoicemailTaskApiResponse {
	@Nonnull
	private final UUID patientOrderVoicemailTaskId;
	@Nonnull
	private final UUID patientOrderId;
	@Nullable
	private final UUID createdByAccountId;
	@Nullable
	private final UUID completedByAccountId;
	@Nullable
	private final UUID deletedByAccountId;
	@Nonnull
	private final String message;
	@Nonnull
	private final Boolean completed;
	@Nullable
	private final Instant completedAt;
	@Nullable
	private final String completedAtDescription;
	@Nonnull
	private final Boolean deleted;
	@Nullable
	private final Instant deletedAt;
	@Nullable
	private final String deletedAtDescription;
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
	public interface PatientOrderVoicemailTaskApiResponseFactory {
		@Nonnull
		PatientOrderVoicemailTaskApiResponse create(@Nonnull PatientOrderVoicemailTask patientOrderVoicemailTask);
	}

	@AssistedInject
	public PatientOrderVoicemailTaskApiResponse(@Nonnull Provider<CurrentContext> currentContextProvider,
																							@Nonnull Formatter formatter,
																							@Assisted @Nonnull PatientOrderVoicemailTask patientOrderVoicemailTask) {
		requireNonNull(currentContextProvider);
		requireNonNull(formatter);
		requireNonNull(patientOrderVoicemailTask);

		this.patientOrderVoicemailTaskId = patientOrderVoicemailTask.getPatientOrderVoicemailTaskId();
		this.patientOrderId = patientOrderVoicemailTask.getPatientOrderId();
		this.createdByAccountId = patientOrderVoicemailTask.getCreatedByAccountId();
		this.completedByAccountId = patientOrderVoicemailTask.getCompletedByAccountId();
		this.deletedByAccountId = patientOrderVoicemailTask.getDeletedByAccountId();
		this.message = patientOrderVoicemailTask.getMessage();
		this.completed = patientOrderVoicemailTask.getCompleted();
		this.completedAt = patientOrderVoicemailTask.getCompletedAt();
		this.completedAtDescription = patientOrderVoicemailTask.getCompletedAt() == null ? null : formatter.formatTimestamp(patientOrderVoicemailTask.getCompletedAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.deleted = patientOrderVoicemailTask.getDeleted();
		this.deletedAt = patientOrderVoicemailTask.getDeletedAt();
		this.deletedAtDescription = patientOrderVoicemailTask.getDeletedAt() == null ? null : formatter.formatTimestamp(patientOrderVoicemailTask.getDeletedAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.created = patientOrderVoicemailTask.getCreated();
		this.createdDescription = formatter.formatTimestamp(patientOrderVoicemailTask.getCreated(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.lastUpdated = patientOrderVoicemailTask.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(patientOrderVoicemailTask.getLastUpdated(), FormatStyle.MEDIUM, FormatStyle.SHORT);
	}

	@Nonnull
	public UUID getPatientOrderVoicemailTaskId() {
		return this.patientOrderVoicemailTaskId;
	}

	@Nonnull
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}

	@Nullable
	public UUID getCompletedByAccountId() {
		return this.completedByAccountId;
	}

	@Nullable
	public UUID getDeletedByAccountId() {
		return this.deletedByAccountId;
	}

	@Nonnull
	public String getMessage() {
		return this.message;
	}

	@Nonnull
	public Boolean getCompleted() {
		return this.completed;
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
	public Boolean getDeleted() {
		return this.deleted;
	}

	@Nullable
	public Instant getDeletedAt() {
		return this.deletedAt;
	}

	@Nullable
	public String getDeletedAtDescription() {
		return this.deletedAtDescription;
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