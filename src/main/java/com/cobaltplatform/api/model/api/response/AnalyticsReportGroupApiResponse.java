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

import com.cobaltplatform.api.model.db.AnalyticsReportGroup;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class AnalyticsReportGroupApiResponse {
	@Nonnull
	private final UUID analyticsReportGroupId;
	@Nullable
	private final String name;
	@Nullable
	private final Integer displayOrder;
	@Nullable
	private final String displayOrderDescription;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final Instant lastUpdated;
	@Nonnull
	private final String lastUpdatedDescription;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface AnalyticsReportGroupApiResponseFactory {
		@Nonnull
		AnalyticsReportGroupApiResponse create(@Nonnull AnalyticsReportGroup analyticsReportGroup);
	}

	@AssistedInject
	public AnalyticsReportGroupApiResponse(@Nonnull Formatter formatter,
																				 @Assisted @Nonnull AnalyticsReportGroup analyticsReportGroup) {
		requireNonNull(formatter);
		requireNonNull(analyticsReportGroup);

		this.analyticsReportGroupId = analyticsReportGroup.getAnalyticsReportGroupId();
		this.name = analyticsReportGroup.getName();
		this.displayOrder = analyticsReportGroup.getDisplayOrder();
		this.displayOrderDescription = formatter.formatNumber(analyticsReportGroup.getDisplayOrder());
		this.created = analyticsReportGroup.getCreated();
		this.createdDescription = formatter.formatTimestamp(analyticsReportGroup.getCreated());
		this.lastUpdated = analyticsReportGroup.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(analyticsReportGroup.getLastUpdated());
	}

	@Nonnull
	public UUID getAnalyticsReportGroupId() {
		return this.analyticsReportGroupId;
	}

	@Nullable
	public String getName() {
		return this.name;
	}

	@Nullable
	public Integer getDisplayOrder() {
		return this.displayOrder;
	}

	@Nullable
	public String getDisplayOrderDescription() {
		return this.displayOrderDescription;
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
}