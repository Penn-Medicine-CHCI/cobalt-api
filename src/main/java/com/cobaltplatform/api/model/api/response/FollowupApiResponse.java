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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.cobaltplatform.api.model.api.response.AccountApiResponse.AccountApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ProviderApiResponse.ProviderApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ProviderApiResponse.ProviderApiResponseSupplement;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AppointmentReason;
import com.cobaltplatform.api.model.db.Followup;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AppointmentService;
import com.cobaltplatform.api.service.ProviderService;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class FollowupApiResponse {
	@Nonnull
	private final UUID followupId;
	@Nonnull
	private final UUID accountId;
	@Nonnull
	private final UUID createdByAccountId;
	@Nonnull
	private final UUID providerId;
	@Nonnull
	private final UUID appointmentReasonId;
	@Nonnull
	private final LocalDate followupDate;
	@Nonnull
	private final String followupDateDescription;
	@Nullable
	private final String comment;
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
	@Nullable
	private final AccountApiResponse account;
	@Nullable
	private final ProviderApiResponse provider;
	@Nullable
	private final AppointmentReasonApiResponse appointmentReason;

	public enum FollowupApiResponseSupplement {
		ACCOUNT,
		APPOINTMENT_REASON,
		PROVIDER,

		// Special supplement to mean "everything"
		ALL
	}

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface FollowupApiResponseFactory {
		@Nonnull
		FollowupApiResponse create(@Nonnull Followup followup);

		@Nonnull
		FollowupApiResponse create(@Nonnull Followup followup,
															 @Nullable Set<FollowupApiResponseSupplement> supplements);
	}

	@AssistedInject
	public FollowupApiResponse(@Nonnull Formatter formatter,
														 @Nonnull AccountService accountService,
														 @Nonnull AppointmentService appointmentService,
														 @Nonnull ProviderService providerService,
														 @Nonnull AccountApiResponseFactory accountApiResponseFactory,
														 @Nonnull ProviderApiResponseFactory providerApiResponseFactory,
														 @Assisted @Nonnull Followup followup) {
		this(formatter, accountService, appointmentService, providerService, accountApiResponseFactory, providerApiResponseFactory, followup, null);
	}

	@AssistedInject
	public FollowupApiResponse(@Nonnull Formatter formatter,
														 @Nonnull AccountService accountService,
														 @Nonnull AppointmentService appointmentService,
														 @Nonnull ProviderService providerService,
														 @Nonnull AccountApiResponseFactory accountApiResponseFactory,
														 @Nonnull ProviderApiResponseFactory providerApiResponseFactory,
														 @Assisted @Nonnull Followup followup,
														 @Assisted @Nullable Set<FollowupApiResponseSupplement> supplements) {
		requireNonNull(formatter);
		requireNonNull(accountService);
		requireNonNull(appointmentService);
		requireNonNull(providerService);
		requireNonNull(accountApiResponseFactory);
		requireNonNull(providerApiResponseFactory);
		requireNonNull(followup);

		if (supplements == null)
			supplements = Collections.emptySet();

		this.followupId = followup.getFollowupId();
		this.accountId = followup.getAccountId();
		this.createdByAccountId = followup.getCreatedByAccountId();
		this.providerId = followup.getProviderId();
		this.appointmentReasonId = followup.getAppointmentReasonId();
		this.followupDate = followup.getFollowupDate();
		this.followupDateDescription = formatter.formatDate(followup.getFollowupDate(), FormatStyle.MEDIUM);
		this.comment = followup.getComment();
		this.canceled = followup.getCanceled();
		this.canceledAt = followup.getCanceledAt();
		this.canceledAtDescription = followup.getCanceledAt() == null ? null : formatter.formatTimestamp(followup.getCanceledAt());
		this.created = followup.getCreated();
		this.createdDescription = formatter.formatTimestamp(followup.getCreated());
		this.lastUpdated = followup.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(followup.getLastUpdated());

		AccountApiResponse accountApiResponse = null;

		if (supplements.contains(FollowupApiResponseSupplement.ALL) || supplements.contains(FollowupApiResponseSupplement.ACCOUNT)) {
			Account account = accountService.findAccountById(followup.getAccountId()).orElse(null);

			if (account != null)
				accountApiResponse = accountApiResponseFactory.create(account);
		}

		this.account = accountApiResponse;

		AppointmentReasonApiResponse appointmentReasonApiResponse = null;

		if (supplements.contains(FollowupApiResponseSupplement.ALL) || supplements.contains(FollowupApiResponseSupplement.ACCOUNT)) {
			AppointmentReason appointmentReason = appointmentService.findAppointmentReasonById(followup.getAppointmentReasonId()).orElse(null);

			if (account != null)
				appointmentReasonApiResponse = new AppointmentReasonApiResponse(appointmentReason);
		}

		this.appointmentReason = appointmentReasonApiResponse;

		ProviderApiResponse providerApiResponse = null;

		if (supplements.contains(FollowupApiResponseSupplement.ALL) || supplements.contains(FollowupApiResponseSupplement.PROVIDER)) {
			Provider provider = providerService.findProviderById(providerId).orElse(null);

			if (provider != null)
				providerApiResponse = providerApiResponseFactory.create(provider, ProviderApiResponseSupplement.EVERYTHING);
		}

		this.provider = providerApiResponse;
	}

	@Nonnull
	public UUID getFollowupId() {
		return followupId;
	}

	@Nonnull
	public UUID getAccountId() {
		return accountId;
	}

	@Nonnull
	public UUID getCreatedByAccountId() {
		return createdByAccountId;
	}

	@Nonnull
	public UUID getProviderId() {
		return providerId;
	}

	@Nonnull
	public UUID getAppointmentReasonId() {
		return appointmentReasonId;
	}

	@Nonnull
	public LocalDate getFollowupDate() {
		return followupDate;
	}

	@Nonnull
	public String getFollowupDateDescription() {
		return followupDateDescription;
	}

	@Nullable
	public String getComment() {
		return comment;
	}

	@Nonnull
	public Instant getCreated() {
		return created;
	}

	@Nonnull
	public String getCreatedDescription() {
		return createdDescription;
	}

	@Nonnull
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	@Nonnull
	public String getLastUpdatedDescription() {
		return lastUpdatedDescription;
	}

	@Nonnull
	public Boolean getCanceled() {
		return canceled;
	}

	@Nullable
	public Instant getCanceledAt() {
		return canceledAt;
	}

	@Nullable
	public String getCanceledAtDescription() {
		return canceledAtDescription;
	}

	@Nullable
	public AccountApiResponse getAccount() {
		return account;
	}

	@Nullable
	public AppointmentReasonApiResponse getAppointmentReason() {
		return appointmentReason;
	}

	@Nullable
	public ProviderApiResponse getProvider() {
		return provider;
	}
}
