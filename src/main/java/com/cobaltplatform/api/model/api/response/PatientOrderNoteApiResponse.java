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
import com.cobaltplatform.api.model.api.response.AccountApiResponse.AccountApiResponseFactory;
import com.cobaltplatform.api.model.db.PatientOrderNote;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class PatientOrderNoteApiResponse {
	@Nonnull
	private final UUID patientOrderNoteId;
	@Nonnull
	private final UUID patientOrderId;
	@Nonnull
	private final UUID accountId;
	@Nonnull
	private final String note;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final Instant lastUpdated;
	@Nonnull
	private final String lastUpdatedDescription;

	@Nonnull
	private final AccountApiResponse account;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PatientOrderNoteApiResponseFactory {
		@Nonnull
		PatientOrderNoteApiResponse create(@Nonnull PatientOrderNote patientOrderNote);
	}

	@AssistedInject
	public PatientOrderNoteApiResponse(@Nonnull AccountService accountService,
																		 @Nonnull AccountApiResponseFactory accountApiResponseFactory,
																		 @Nonnull Formatter formatter,
																		 @Nonnull Provider<CurrentContext> currentContextProvider,
																		 @Assisted @Nonnull PatientOrderNote patientOrderNote) {
		requireNonNull(accountService);
		requireNonNull(accountApiResponseFactory);
		requireNonNull(formatter);
		requireNonNull(currentContextProvider);
		requireNonNull(patientOrderNote);

		this.patientOrderNoteId = patientOrderNote.getPatientOrderNoteId();
		this.patientOrderId = patientOrderNote.getPatientOrderId();
		this.accountId = patientOrderNote.getAccountId();
		this.note = patientOrderNote.getNote();
		this.created = patientOrderNote.getCreated();
		this.createdDescription = formatter.formatTimestamp(patientOrderNote.getCreated());
		this.lastUpdated = patientOrderNote.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(patientOrderNote.getLastUpdated());
		this.account = accountApiResponseFactory.create(accountService.findAccountById(patientOrderNote.getAccountId()).get());
	}

	@Nonnull
	public UUID getPatientOrderNoteId() {
		return this.patientOrderNoteId;
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
	public String getNote() {
		return this.note;
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
	public AccountApiResponse getAccount() {
		return this.account;
	}
}