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

package com.cobaltplatform.api.integration.google;

import com.cobaltplatform.api.model.db.AnalyticsGoogleBigQueryEvent;
import com.cobaltplatform.api.util.GsonUtility;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Normalized/simplified representation of https://support.google.com/analytics/answer/7029846?hl=en.
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class GoogleBigQueryExportRecord {
	@Nonnull
	private static final Gson GSON;

	static {
		GsonBuilder gsonBuilder = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping();

		GsonUtility.applyDefaultTypeAdapters(gsonBuilder);

		// Only exists for nice toString() output
		GSON = gsonBuilder.create();
	}

	@Nullable
	private AnalyticsGoogleBigQueryEvent.Event event;
	@Nullable
	private AnalyticsGoogleBigQueryEvent.User user;
	@Nullable
	private AnalyticsGoogleBigQueryEvent.TrafficSource trafficSource;
	@Nullable
	private AnalyticsGoogleBigQueryEvent.CollectedTrafficSource collectedTrafficSource;
	@Nullable
	private AnalyticsGoogleBigQueryEvent.Geo geo;
	@Nullable
	private AnalyticsGoogleBigQueryEvent.Device device;

	@Override
	@Nonnull
	public String toString() {
		return GSON.toJson(this);
	}

	@Nullable
	public AnalyticsGoogleBigQueryEvent.Event getEvent() {
		return this.event;
	}

	public void setEvent(@Nullable AnalyticsGoogleBigQueryEvent.Event event) {
		this.event = event;
	}

	@Nullable
	public AnalyticsGoogleBigQueryEvent.User getUser() {
		return this.user;
	}

	public void setUser(@Nullable AnalyticsGoogleBigQueryEvent.User user) {
		this.user = user;
	}

	@Nullable
	public AnalyticsGoogleBigQueryEvent.TrafficSource getTrafficSource() {
		return this.trafficSource;
	}

	public void setTrafficSource(@Nullable AnalyticsGoogleBigQueryEvent.TrafficSource trafficSource) {
		this.trafficSource = trafficSource;
	}

	@Nullable
	public AnalyticsGoogleBigQueryEvent.CollectedTrafficSource getCollectedTrafficSource() {
		return this.collectedTrafficSource;
	}

	public void setCollectedTrafficSource(@Nullable AnalyticsGoogleBigQueryEvent.CollectedTrafficSource collectedTrafficSource) {
		this.collectedTrafficSource = collectedTrafficSource;
	}

	@Nullable
	public AnalyticsGoogleBigQueryEvent.Geo getGeo() {
		return this.geo;
	}

	public void setGeo(@Nullable AnalyticsGoogleBigQueryEvent.Geo geo) {
		this.geo = geo;
	}

	@Nullable
	public AnalyticsGoogleBigQueryEvent.Device getDevice() {
		return this.device;
	}

	public void setDevice(@Nullable AnalyticsGoogleBigQueryEvent.Device device) {
		this.device = device;
	}
}
