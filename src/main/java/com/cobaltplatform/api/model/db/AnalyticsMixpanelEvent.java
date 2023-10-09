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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.util.GsonUtility;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AnalyticsMixpanelEvent {
	@Nonnull
	private static final Gson GSON;

	static {
		GsonBuilder gsonBuilder = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping();

		GsonUtility.applyDefaultTypeAdapters(gsonBuilder);

		GSON = gsonBuilder.create();
	}

	@Nullable
	private UUID analyticsMixpanelEventId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private UUID accountId;
	@Nullable
	private String distinctId;
	@Nullable
	private String name;
	@Nullable
	private LocalDate date;
	@Nullable
	private Instant timestamp;
	@Nullable
	private String propertiesAsJson;

	@Override
	@Nonnull
	public String toString() {
		return GSON.toJson(this);
	}

	@Nonnull
	public static String toPropertiesJson(@Nonnull Map<String, Object> properties) {
		requireNonNull(properties);
		return GSON.toJson(properties);
	}

	@Nonnull
	public Map<String, Object> getProperties() {
		return this.propertiesAsJson == null ? Map.of() : GSON.fromJson(this.propertiesAsJson, new TypeToken<Map<String, Object>>() {
		}.getType());
	}

	@Nullable
	public UUID getAnalyticsMixpanelEventId() {
		return this.analyticsMixpanelEventId;
	}

	public void setAnalyticsMixpanelEventId(@Nullable UUID analyticsMixpanelEventId) {
		this.analyticsMixpanelEventId = analyticsMixpanelEventId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public String getDistinctId() {
		return this.distinctId;
	}

	public void setDistinctId(@Nullable String distinctId) {
		this.distinctId = distinctId;
	}

	@Nullable
	public String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public LocalDate getDate() {
		return this.date;
	}

	public void setDate(@Nullable LocalDate date) {
		this.date = date;
	}

	@Nullable
	public Instant getTimestamp() {
		return this.timestamp;
	}

	public void setTimestamp(@Nullable Instant timestamp) {
		this.timestamp = timestamp;
	}

	@Nullable
	public String getPropertiesAsJson() {
		return this.propertiesAsJson;
	}

	public void setPropertiesAsJson(@Nullable String propertiesAsJson) {
		this.propertiesAsJson = propertiesAsJson;
	}
}