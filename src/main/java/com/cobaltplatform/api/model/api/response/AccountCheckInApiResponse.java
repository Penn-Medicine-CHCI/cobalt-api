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
import com.cobaltplatform.api.model.db.AccountCheckIn;
import com.cobaltplatform.api.model.db.AccountCheckInAction;
import com.cobaltplatform.api.model.db.CheckInType;
import com.cobaltplatform.api.service.StudyService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import static java.lang.String.format;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class AccountCheckInApiResponse {
	@Nullable
	private final UUID accountCheckInId;
	@Nullable
	private final UUID accountId;
	@Nullable
	private final CheckInType.CheckInTypeId checkInTypeId;
	@Nullable
	private final Integer checkInNumber;
	@Nullable
	private final String checkInNumberDescription;
	@Nullable
	private final String checkInDescription;
	@Nullable
	private final Boolean checkInActive;
	@Nullable
	private final List<AccountCheckInAction> accountCheckInActions;
	@Nullable
	private final Instant created;
	@Nullable
	private final Instant lastUpdated;


	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface AccountCheckInApiResponseFactory {
		@Nonnull
		AccountCheckInApiResponse create(@Nonnull AccountCheckIn accountCheckIn);
	}

	@AssistedInject
	public AccountCheckInApiResponse(@Nonnull Provider<CurrentContext> currentContextProvider,
																	 @Assisted @Nonnull AccountCheckIn accountCheckIn,
																	 @Nonnull Formatter formatter,
																	 @Nonnull StudyService studyService) {
		requireNonNull(currentContextProvider);
		requireNonNull(accountCheckIn);
		requireNonNull(studyService);
		requireNonNull(formatter);

		Boolean checkInActive = studyService.accountCheckActive(currentContextProvider.get().getAccount().get(), accountCheckIn);
		Boolean includeTimeInDescription = !accountCheckIn.getCheckInStartDateTime().toLocalTime().equals(LocalTime.of(0, 0, 0));
		String checkInDescription;

		if (checkInActive) {
			if (includeTimeInDescription)
				checkInDescription = formatter.formatDateTime(accountCheckIn.getCheckInEndDateTime(), FormatStyle.LONG, FormatStyle.MEDIUM);
			else
				checkInDescription = formatter.formatDate(accountCheckIn.getCheckInEndDateTime().toLocalDate());
		} else if (includeTimeInDescription)
			checkInDescription = formatter.formatDateTime(accountCheckIn.getCheckInStartDateTime(), FormatStyle.LONG, FormatStyle.MEDIUM);
		else
			checkInDescription = formatter.formatDate(accountCheckIn.getCheckInStartDateTime().toLocalDate());

		this.accountCheckInId = accountCheckIn.getAccountCheckInId();
		this.accountId = accountCheckIn.getAccountId();
		this.checkInTypeId = accountCheckIn.getCheckInTypeId();
		this.checkInNumber = accountCheckIn.getCheckInNumber();
		this.checkInNumberDescription = format("Check %s", accountCheckIn.getCheckInNumber());
		this.checkInDescription = format(checkInActive ? format("Ends %s", checkInDescription)
				: format("Starts %s", checkInDescription));
		this.checkInActive = checkInActive;
		this.created = accountCheckIn.getCreated();
		this.lastUpdated = accountCheckIn.getLastUpdated();
		this.accountCheckInActions = studyService.findAccountCheckInActionsFoAccountAndCheckIn
				(currentContextProvider.get().getAccount().get().getAccountId(), accountCheckIn.getAccountCheckInId());
	}

	@Nullable
	public UUID getAccountCheckInId() {
		return accountCheckInId;
	}

	@Nullable
	public UUID getAccountId() {
		return accountId;
	}

	@Nullable
	public CheckInType.CheckInTypeId getCheckInTypeId() {
		return checkInTypeId;
	}

	@Nullable
	public List<AccountCheckInAction> getAccountCheckInActions() {
		return accountCheckInActions;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	@Nullable
	public Integer getCheckInNumber() {
		return checkInNumber;
	}

	@Nullable
	public String getCheckInNumberDescription() {
		return checkInNumberDescription;
	}

	@Nullable
	public String getCheckInDescription() {
		return checkInDescription;
	}
}