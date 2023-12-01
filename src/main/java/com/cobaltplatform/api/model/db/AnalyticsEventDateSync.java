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

package com.cobaltplatform.api.model.db;

import com.cobaltplatform.api.model.db.AnalyticsSyncStatus.AnalyticsSyncStatusId;
import com.cobaltplatform.api.model.db.AnalyticsVendor.AnalyticsVendorId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalDate;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AnalyticsEventDateSync {
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private AnalyticsVendorId analyticsVendorId;
	@Nullable
	private AnalyticsSyncStatusId analyticsSyncStatusId;
	@Nullable
	private LocalDate date;
	@Nullable
	private Instant syncStartedAt;
	@Nullable
	private Instant syncEndedAt;

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public AnalyticsVendorId getAnalyticsVendorId() {
		return this.analyticsVendorId;
	}

	public void setAnalyticsVendorId(@Nullable AnalyticsVendorId analyticsVendorId) {
		this.analyticsVendorId = analyticsVendorId;
	}

	@Nullable
	public AnalyticsSyncStatusId getAnalyticsSyncStatusId() {
		return this.analyticsSyncStatusId;
	}

	public void setAnalyticsSyncStatusId(@Nullable AnalyticsSyncStatusId analyticsSyncStatusId) {
		this.analyticsSyncStatusId = analyticsSyncStatusId;
	}

	@Nullable
	public LocalDate getDate() {
		return this.date;
	}

	public void setDate(@Nullable LocalDate date) {
		this.date = date;
	}

	@Nullable
	public Instant getSyncStartedAt() {
		return this.syncStartedAt;
	}

	public void setSyncStartedAt(@Nullable Instant syncStartedAt) {
		this.syncStartedAt = syncStartedAt;
	}

	@Nullable
	public Instant getSyncEndedAt() {
		return this.syncEndedAt;
	}

	public void setSyncEndedAt(@Nullable Instant syncEndedAt) {
		this.syncEndedAt = syncEndedAt;
	}
}