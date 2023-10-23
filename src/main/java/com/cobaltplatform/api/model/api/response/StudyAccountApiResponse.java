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
import com.cobaltplatform.api.model.service.StudyAccount;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class StudyAccountApiResponse {
	@Nonnull
	private final String username;
	@Nonnull
	private final String password;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface StudyAccountApiResponseFactory {
		@Nonnull
		StudyAccountApiResponse create(@Nonnull StudyAccount studyAccount);
	}

	@AssistedInject
	public StudyAccountApiResponse(@Nonnull Provider<CurrentContext> currentContextProvider,
																 @Assisted @Nonnull StudyAccount studyAccount) {
		requireNonNull(currentContextProvider);
		requireNonNull(studyAccount);

		this.username = studyAccount.getUsername();
		this.password = studyAccount.getPassword();
	}

	@Nonnull
	public String getUsername() {
		return username;
	}

	@Nonnull
	public String getPassword() {
		return password;
	}
}