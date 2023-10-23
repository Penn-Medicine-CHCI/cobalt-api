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
import com.cobaltplatform.api.model.db.AccountCheckInAction;
import com.cobaltplatform.api.model.db.CheckInActionStatus.CheckInActionStatusId;
import com.cobaltplatform.api.model.db.CheckInType;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import java.util.UUID;


import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class AccountCheckInActionApiResponse {
	@Nullable
	private UUID accountCheckInActionId;

	@Nullable
	private CheckInActionStatusId checkInActionStatusId;
	@Nullable
	private String checkInActionStatusDescription;

	@Nullable
	private CheckInType.CheckInTypeId checkInTypeId;

	@Nullable
	String checkInTypeDescription;

	@Nullable
	private UUID screeningSessionId;

	@Nullable
	private UUID screeningFlowId;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface AccountCheckInActionApiResponseFactory {
		@Nonnull
		AccountCheckInActionApiResponse create(@Nonnull AccountCheckInAction accountCheckInAction);
	}

	@AssistedInject
	public AccountCheckInActionApiResponse(@Nonnull Provider<CurrentContext> currentContextProvider,
																				 @Assisted @Nonnull AccountCheckInAction accountCheckInAction,
																				 @Nonnull Formatter formatter) {
		requireNonNull(currentContextProvider);
		requireNonNull(accountCheckInAction);
		requireNonNull(formatter);

		accountCheckInActionId = accountCheckInAction.getAccountCheckInActionId();
		checkInActionStatusId = accountCheckInAction.getCheckInActionStatusId();
		checkInActionStatusId = accountCheckInAction.getCheckInActionStatusId();
		checkInActionStatusDescription = accountCheckInAction.getCheckInActionStatusDescription();
		checkInTypeId = accountCheckInAction.getCheckInTypeId();
		checkInTypeDescription = accountCheckInAction.getCheckInTypeDescription();
		screeningSessionId = accountCheckInAction.getScreeningSessionId();
		screeningFlowId = accountCheckInAction.getScreeningFlowId();
	}

	@Nullable
	public UUID getAccountCheckInActionId() {
		return accountCheckInActionId;
	}

	@Nullable
	public CheckInActionStatusId getCheckInActionStatusId() {
		return checkInActionStatusId;
	}

	@Nullable
	public String getCheckInActionStatusDescription() {
		return checkInActionStatusDescription;
	}

	@Nullable
	public CheckInType.CheckInTypeId getCheckInTypeId() {
		return checkInTypeId;
	}

	@Nullable
	public String getCheckInTypeDescription() {
		return checkInTypeDescription;
	}

	@Nullable
	public UUID getScreeningSessionId() {
		return screeningSessionId;
	}

	@Nullable
	public UUID getScreeningFlowId() {
		return screeningFlowId;
	}
}