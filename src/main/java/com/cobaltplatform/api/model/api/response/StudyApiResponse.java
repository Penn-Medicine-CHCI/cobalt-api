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

import com.cobaltplatform.api.model.db.Study;
import com.cobaltplatform.api.util.Formatter;
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
public class StudyApiResponse {
	@Nullable
	private final UUID studyId;
	@Nullable
	private final String name;
	@Nullable
	private final String urlName;
	@Nullable
	private final String onboardingDestinationUrl;
	@Nullable
	private final Integer minutesBetweenCheckIns;
	@Nullable
	private final Integer gracePeriodInMinutes;
	@Nullable
	private final String coordinatorName;
	@Nullable
	private final String coordinatorEmailAddress;
	@Nullable
	private final String coordinatorPhoneNumber;
	@Nullable
	private final String coordinatorPhoneNumberDescription;
	@Nullable
	private final String coordinatorAvailability;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface StudyApiResponseFactory {
		@Nonnull
		StudyApiResponse create(@Nonnull Study study);
	}

	@AssistedInject
	public StudyApiResponse(@Nonnull Formatter formatter,
													@Assisted @Nonnull Study study) {
		requireNonNull(formatter);
		requireNonNull(study);

		this.studyId = study.getStudyId();
		this.name = study.getName();
		this.urlName = study.getUrlName();
		this.onboardingDestinationUrl = study.getOnboardingDestinationUrl();
		this.minutesBetweenCheckIns = study.getMinutesBetweenCheckIns();
		this.gracePeriodInMinutes = study.getGracePeriodInMinutes();
		this.coordinatorName = study.getCoordinatorName();
		this.coordinatorEmailAddress = study.getCoordinatorEmailAddress();
		this.coordinatorPhoneNumber = study.getCoordinatorPhoneNumber();
		this.coordinatorPhoneNumberDescription = getCoordinatorPhoneNumber() == null ? null : formatter.formatPhoneNumber(getCoordinatorPhoneNumber());
		this.coordinatorAvailability = study.getCoordinatorAvailability();
	}

	@Nullable
	public UUID getStudyId() {
		return studyId;
	}

	@Nullable
	public String getName() {
		return name;
	}

	@Nullable
	public String getUrlName() {
		return urlName;
	}

	@Nullable
	public String getOnboardingDestinationUrl() {
		return onboardingDestinationUrl;
	}

	@Nullable
	public Integer getMinutesBetweenCheckIns() {
		return minutesBetweenCheckIns;
	}

	@Nullable
	public Integer getGracePeriodInMinutes() {
		return gracePeriodInMinutes;
	}

	@Nullable
	public String getCoordinatorName() {
		return this.coordinatorName;
	}

	@Nullable
	public String getCoordinatorEmailAddress() {
		return this.coordinatorEmailAddress;
	}

	@Nullable
	public String getCoordinatorPhoneNumber() {
		return this.coordinatorPhoneNumber;
	}

	@Nullable
	public String getCoordinatorPhoneNumberDescription() {
		return this.coordinatorPhoneNumberDescription;
	}

	@Nullable
	public String getCoordinatorAvailability() {
		return this.coordinatorAvailability;
	}
}