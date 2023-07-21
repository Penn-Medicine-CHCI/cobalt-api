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
import com.cobaltplatform.api.model.db.PatientOrderCareType.PatientOrderCareTypeId;
import com.cobaltplatform.api.model.db.PatientOrderFocusType.PatientOrderFocusTypeId;
import com.cobaltplatform.api.model.db.PatientOrderTriage;
import com.cobaltplatform.api.model.db.PatientOrderTriageOverrideReason.PatientOrderTriageOverrideReasonId;
import com.cobaltplatform.api.model.db.PatientOrderTriageSource.PatientOrderTriageSourceId;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class PatientOrderTriageApiResponse {
	@Nonnull
	private final UUID patientOrderTriageId;
	@Nonnull
	private final UUID patientOrderId;
	@Nonnull
	private final PatientOrderFocusTypeId patientOrderFocusTypeId;
	@Nonnull
	private final PatientOrderCareTypeId patientOrderCareTypeId;
	@Nonnull
	private final PatientOrderTriageSourceId patientOrderTriageSourceId;
	@Nonnull
	private final PatientOrderTriageOverrideReasonId patientOrderTriageOverrideReasonId;
	@Nullable
	private final UUID screeningSessionId;
	@Nullable
	private final UUID accountId;
	@Nullable
	private final String reason;
	@Nonnull
	private final Boolean active;
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
	public interface PatientOrderTriageApiResponseFactory {
		@Nonnull
		PatientOrderTriageApiResponse create(@Nonnull PatientOrderTriage patientOrderTriage);
	}

	@AssistedInject
	public PatientOrderTriageApiResponse(@Nonnull Formatter formatter,
																			 @Nonnull Provider<CurrentContext> currentContextProvider,
																			 @Assisted @Nonnull PatientOrderTriage patientOrderTriage) {
		requireNonNull(formatter);
		requireNonNull(currentContextProvider);
		requireNonNull(patientOrderTriage);

		this.patientOrderTriageId = patientOrderTriage.getPatientOrderTriageId();
		this.patientOrderId = patientOrderTriage.getPatientOrderId();
		this.patientOrderFocusTypeId = patientOrderTriage.getPatientOrderFocusTypeId();
		this.patientOrderCareTypeId = patientOrderTriage.getPatientOrderCareTypeId();
		this.patientOrderTriageSourceId = patientOrderTriage.getPatientOrderTriageSourceId();
		this.patientOrderTriageOverrideReasonId = patientOrderTriage.getPatientOrderTriageOverrideReasonId();
		this.screeningSessionId = patientOrderTriage.getScreeningSessionId();
		this.accountId = patientOrderTriage.getAccountId();
		this.reason = patientOrderTriage.getReason();
		this.active = patientOrderTriage.getActive();
		this.created = patientOrderTriage.getCreated();
		this.createdDescription = formatter.formatTimestamp(patientOrderTriage.getCreated());
		this.lastUpdated = patientOrderTriage.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(patientOrderTriage.getLastUpdated());
	}

	@Nonnull
	public UUID getPatientOrderTriageId() {
		return this.patientOrderTriageId;
	}

	@Nonnull
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	@Nonnull
	public PatientOrderFocusTypeId getPatientOrderFocusTypeId() {
		return this.patientOrderFocusTypeId;
	}

	@Nonnull
	public PatientOrderCareTypeId getPatientOrderCareTypeId() {
		return this.patientOrderCareTypeId;
	}

	@Nonnull
	public PatientOrderTriageSourceId getPatientOrderTriageSourceId() {
		return this.patientOrderTriageSourceId;
	}

	@Nonnull
	public PatientOrderTriageOverrideReasonId getPatientOrderTriageOverrideReasonId() {
		return this.patientOrderTriageOverrideReasonId;
	}

	@Nullable
	public UUID getScreeningSessionId() {
		return this.screeningSessionId;
	}

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	@Nullable
	public String getReason() {
		return this.reason;
	}

	@Nonnull
	public Boolean getActive() {
		return this.active;
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