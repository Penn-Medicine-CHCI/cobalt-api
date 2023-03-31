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
import com.cobaltplatform.api.model.db.PatientOrderOutreach;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
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
public class PatientOrderOutreachApiResponse {
	@Nonnull
	private final UUID patientOrderOutreachId;
	@Nonnull
	private final UUID patientOrderOutreachResultId;
	@Nonnull
	private final UUID patientOrderId;
	@Nonnull
	private final UUID accountId;
	@Nonnull
	private final String note;
	@Nonnull
	private final LocalDate outreachDate;
	@Nonnull
	private final String outreachDateDescription;
	@Nonnull
	private final LocalTime outreachTime;
	@Nonnull
	private final String outreachTimeDescription;
	@Nonnull
	private final LocalDateTime outreachDateTime;
	@Nonnull
	private final String outreachDateTimeDescription;
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
	public interface PatientOrderOutreachApiResponseFactory {
		@Nonnull
		PatientOrderOutreachApiResponse create(@Nonnull PatientOrderOutreach patientOrderOutreach);
	}

	@AssistedInject
	public PatientOrderOutreachApiResponse(@Nonnull AccountService accountService,
																				 @Nonnull AccountApiResponseFactory accountApiResponseFactory,
																				 @Nonnull Formatter formatter,
																				 @Nonnull Provider<CurrentContext> currentContextProvider,
																				 @Assisted @Nonnull PatientOrderOutreach patientOrderOutreach) {
		requireNonNull(accountService);
		requireNonNull(accountApiResponseFactory);
		requireNonNull(formatter);
		requireNonNull(currentContextProvider);
		requireNonNull(patientOrderOutreach);

		this.patientOrderOutreachId = patientOrderOutreach.getPatientOrderOutreachId();
		this.patientOrderOutreachResultId = patientOrderOutreach.getPatientOrderOutreachResultId();
		this.patientOrderId = patientOrderOutreach.getPatientOrderId();
		this.accountId = patientOrderOutreach.getAccountId();
		this.note = patientOrderOutreach.getNote();
		this.outreachDate = patientOrderOutreach.getOutreachDateTime().toLocalDate();
		this.outreachDateDescription = formatter.formatDate(getOutreachDate(), FormatStyle.MEDIUM);
		this.outreachTime = patientOrderOutreach.getOutreachDateTime().toLocalTime();
		this.outreachTimeDescription = formatter.formatTime(getOutreachTime(), FormatStyle.SHORT);
		this.outreachDateTime = patientOrderOutreach.getOutreachDateTime();
		this.outreachDateTimeDescription = formatter.formatDateTime(patientOrderOutreach.getOutreachDateTime(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.created = patientOrderOutreach.getCreated();
		this.createdDescription = formatter.formatTimestamp(patientOrderOutreach.getCreated());
		this.lastUpdated = patientOrderOutreach.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(patientOrderOutreach.getLastUpdated());
		this.account = accountApiResponseFactory.create(accountService.findAccountById(patientOrderOutreach.getAccountId()).get());
	}

	@Nonnull
	public UUID getPatientOrderOutreachId() {
		return this.patientOrderOutreachId;
	}

	@Nonnull
	public UUID getPatientOrderOutreachResultId() {
		return this.patientOrderOutreachResultId;
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
	public LocalDate getOutreachDate() {
		return this.outreachDate;
	}

	@Nonnull
	public String getOutreachDateDescription() {
		return this.outreachDateDescription;
	}

	@Nonnull
	public LocalTime getOutreachTime() {
		return this.outreachTime;
	}

	@Nonnull
	public String getOutreachTimeDescription() {
		return this.outreachTimeDescription;
	}

	@Nonnull
	public LocalDateTime getOutreachDateTime() {
		return this.outreachDateTime;
	}

	@Nonnull
	public String getOutreachDateTimeDescription() {
		return this.outreachDateTimeDescription;
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