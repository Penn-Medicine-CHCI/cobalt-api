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
import com.cobaltplatform.api.model.api.response.AccountCheckInActionApiResponse.AccountCheckInActionApiResponseFactory;
import com.cobaltplatform.api.model.db.AccountCheckIn;
import com.cobaltplatform.api.model.db.AccountCheckInAction;
import com.cobaltplatform.api.model.db.CheckInActionStatus;
import com.cobaltplatform.api.model.db.CheckInStatus;
import com.cobaltplatform.api.model.db.CheckInType.CheckInTypeId;
import com.cobaltplatform.api.service.StudyService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalTime;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class AccountCheckInApiResponse {
	@Nonnull
	private static final List<String> COLOR_CSS_REPRESENTATIONS;

	static {
		COLOR_CSS_REPRESENTATIONS = List.of(
				"#34C759",
				"#00C7BE",
				"#30B0C7",
				"#32ADE6",
				"#007AFF",
				"#5856D6",
				"#AF52DE",
				"#BE65E9"
		);
	}

	@Nullable
	private final UUID accountCheckInId;
	@Nullable
	private final CheckInTypeId checkInTypeId;
	@Nullable
	private final Integer checkInNumber;
	@Nullable
	private final String checkInNumberDescription;
	@Nullable
	private final String checkInDescription;
	@Nullable
	private final Boolean checkInActive;
	@Nullable
	private final String colorCssRepresentation;
	@Nullable
	private final List<AccountCheckInActionApiResponse> accountCheckInActions;

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
																	 @Nonnull Strings strings,
																	 @Nonnull StudyService studyService,
																	 @Nonnull AccountCheckInActionApiResponseFactory accountCheckInActionApiResponseFactory) {
		requireNonNull(currentContextProvider);
		requireNonNull(accountCheckIn);
		requireNonNull(studyService);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(accountCheckInActionApiResponseFactory);

		Boolean checkInActive = studyService.accountCheckActive(currentContextProvider.get().getAccount().get(), accountCheckIn);
		Boolean includeTimeInDescription = !accountCheckIn.getCheckInStartDateTime().toLocalTime().equals(LocalTime.of(0, 0, 0));
		String checkInDateDescription;
		List<AccountCheckInAction> accountCheckInActionList = studyService.findAccountCheckInActionsFoAccountAndCheckIn
				(currentContextProvider.get().getAccount().get().getAccountId(), accountCheckIn.getAccountCheckInId(),
						Optional.empty());
		Integer completedCheckInActionCount = accountCheckInActionList.stream().filter(ac ->
				ac.getCheckInActionStatusId().equals(CheckInActionStatus.CheckInActionStatusId.COMPLETE)).collect(Collectors.toList()).size();

		if (checkInActive) {
			if (includeTimeInDescription)
				checkInDateDescription = formatter.formatDateTime(accountCheckIn.getCheckInEndDateTime(), FormatStyle.MEDIUM, FormatStyle.MEDIUM);
			else
				checkInDateDescription = formatter.formatDate(accountCheckIn.getCheckInEndDateTime().toLocalDate());
		} else if (includeTimeInDescription)
			checkInDateDescription = formatter.formatDateTime(accountCheckIn.getCheckInStartDateTime(), FormatStyle.MEDIUM, FormatStyle.MEDIUM);
		else
			checkInDateDescription = formatter.formatDate(accountCheckIn.getCheckInStartDateTime().toLocalDate());

		this.accountCheckInId = accountCheckIn.getAccountCheckInId();
		this.checkInTypeId = accountCheckIn.getCheckInTypeId();
		this.checkInNumber = accountCheckIn.getCheckInNumber();
		this.checkInNumberDescription = strings.get("Check {{checkInNumber}}", Map.of("checkInNumber", accountCheckIn.getCheckInNumber()));
		this.checkInDescription = checkInActive ? strings.get("Ends {{checkInDateDescription}}", Map.of("checkInDateDescription", checkInDateDescription))
				: accountCheckIn.getCheckInStatusId().equals(CheckInStatus.CheckInStatusId.COMPLETE) ||
				accountCheckIn.getCheckInStatusId().equals(CheckInStatus.CheckInStatusId.EXPIRED) ?
				strings.get("{{completedCheckInActionCount}} of {{totalCheckInActionCount}} Complete", Map.of(
						"completedCheckInActionCount", completedCheckInActionCount,
						"totalCheckInActionCount", accountCheckInActionList.size())) : strings.get("Starts {{checkInDateDescription}}", Map.of("checkInDateDescription", checkInDateDescription));
		this.checkInActive = checkInActive;
		this.accountCheckInActions = accountCheckInActionList.stream().map(accountCheckInAction -> accountCheckInActionApiResponseFactory.create(accountCheckInAction)).collect(Collectors.toList());

		// Pick a color from our hardcoded list.
		// Note that check-in numbers are 1-indexed, so we need to subtract one for the modulo operation
		this.colorCssRepresentation = COLOR_CSS_REPRESENTATIONS.get((accountCheckIn.getCheckInNumber() - 1) % COLOR_CSS_REPRESENTATIONS.size());
	}

	@Nullable
	public UUID getAccountCheckInId() {
		return accountCheckInId;
	}

	@Nullable
	public CheckInTypeId getCheckInTypeId() {
		return checkInTypeId;
	}

	@Nullable
	public List<AccountCheckInActionApiResponse> getAccountCheckInActions() {
		return accountCheckInActions;
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

	@Nullable
	public Boolean getCheckInActive() {
		return checkInActive;
	}

	@Nullable
	public String getColorCssRepresentation() {
		return this.colorCssRepresentation;
	}
}