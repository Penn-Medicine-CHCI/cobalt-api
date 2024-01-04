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

import com.cobaltplatform.api.model.db.AccountSession;
import com.cobaltplatform.api.service.AssessmentService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AccountSessionApiResponse {
	@Nonnull
	private UUID accountSessionId;
	@Nonnull
	private UUID accountId;
	@Nonnull
	private UUID assessmentId;
	@Nonnull
	private String assessmentSessionDate;


	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface AccountSessionApiResponseFactory {
		@Nonnull
		AccountSessionApiResponse create(@Nonnull AccountSession AccountSession);
	}

	@AssistedInject
	public AccountSessionApiResponse(@Nonnull Formatter formatter,
																	 @Nonnull Strings strings,
																	 @Assisted @Nonnull AccountSession accountSession,
																	 @Nonnull AssessmentService assessmentService) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(accountSession);
		requireNonNull(assessmentService);

		this.accountSessionId = accountSession.getAccountSessionId();
		this.accountId = accountSession.getAccountId();
		this.assessmentId = accountSession.getAssessmentId();
		//TODO: Drive this off a real completed timestamp?
		this.assessmentSessionDate = formatter.formatTimestamp(accountSession.getCreated());
	}

	@Nonnull
	public UUID getAccountSessionId() {
		return accountSessionId;
	}

	@Nonnull
	public UUID getAccountId() {
		return accountId;
	}

	@Nonnull
	public UUID getAssessmentId() {
		return assessmentId;
	}

	@Nonnull
	public String getAssessmentSessionDate() {
		return assessmentSessionDate;
	}
}