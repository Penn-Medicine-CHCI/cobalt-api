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

import com.cobaltplatform.api.model.db.AccountStudy;
import com.cobaltplatform.api.model.db.RecordingPreference.RecordingPreferenceId;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class AccountStudyApiResponse {
	@Nonnull
	private final UUID accountStudyId;
	@Nonnull
	private final UUID accountId;
	@Nonnull
	private final UUID studyId;
	@Nonnull
	private final UUID encryptionKeypairId;
	@Nonnull
	private final ZoneId timeZone;
	@Nonnull
	private final Boolean studyStarted;
	@Nonnull
	private final RecordingPreferenceId recordingPreferenceId;
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
	public interface AccountStudyApiResponseFactory {
		@Nonnull
		AccountStudyApiResponse create(@Nonnull AccountStudy accountStudy);
	}

	@AssistedInject
	public AccountStudyApiResponse(@Nonnull Formatter formatter,
																 @Assisted @Nonnull AccountStudy accountStudy) {
		requireNonNull(formatter);
		requireNonNull(accountStudy);

		this.accountStudyId = accountStudy.getAccountStudyId();
		this.accountId = accountStudy.getAccountId();
		this.studyId = accountStudy.getStudyId();
		this.encryptionKeypairId = accountStudy.getEncryptionKeypairId();
		this.timeZone = accountStudy.getTimeZone();
		this.studyStarted = accountStudy.getStudyStarted();
		this.recordingPreferenceId = accountStudy.getRecordingPreferenceId();
		this.created = accountStudy.getCreated();
		this.createdDescription = formatter.formatTimestamp(accountStudy.getCreated());
		this.lastUpdated = accountStudy.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(accountStudy.getLastUpdated());
	}

	@Nonnull
	public UUID getAccountStudyId() {
		return this.accountStudyId;
	}

	@Nonnull
	public UUID getAccountId() {
		return this.accountId;
	}

	@Nonnull
	public UUID getStudyId() {
		return this.studyId;
	}

	@Nonnull
	public UUID getEncryptionKeypairId() {
		return this.encryptionKeypairId;
	}

	@Nonnull
	public ZoneId getTimeZone() {
		return this.timeZone;
	}

	@Nonnull
	public Boolean getStudyStarted() {
		return this.studyStarted;
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
	public RecordingPreferenceId getRecordingPreferenceId() {
		return this.recordingPreferenceId;
	}
}