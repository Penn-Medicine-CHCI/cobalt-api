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

import com.cobaltplatform.api.model.db.PatientOrderTrackingType.PatientOrderTrackingTypeId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pyranid.DatabaseColumn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class PatientOrderTracking {
	@Nonnull
	private static final Gson GSON;

	static {
		GSON = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping()
				.create();
	}

	@Nullable
	private UUID patientOrderTrackingId;
	@Nullable
	private PatientOrderTrackingTypeId patientOrderTrackingTypeId;
	@Nullable
	private UUID patientOrderId;
	@Nullable
	private UUID accountId;
	@Nullable
	private String message;
	@Nullable
	@DatabaseColumn("metadata")
	private String metadataAsString;
	@Nullable
	private Map<String, Object> metadata;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	public PatientOrderTracking() {
		this.metadata = Map.of();
	}

	@Nonnull
	protected Gson getGson() {
		return GSON;
	}

	@Nullable
	public String getMetadataAsString() {
		return this.metadataAsString;
	}

	public void setMetadataAsString(@Nullable String metadataAsString) {
		this.metadataAsString = metadataAsString;

		String metadata = trimToNull(metadataAsString);
		this.metadata = metadata == null ? Map.of() : getGson().fromJson(metadata, new TypeToken<Map<String, Object>>() {
		}.getType());
	}

	@Nonnull
	public Map<String, Object> getMetadata() {
		return this.metadata;
	}

	@Nullable
	public UUID getPatientOrderTrackingId() {
		return this.patientOrderTrackingId;
	}

	public void setPatientOrderTrackingId(@Nullable UUID patientOrderTrackingId) {
		this.patientOrderTrackingId = patientOrderTrackingId;
	}

	@Nullable
	public PatientOrderTrackingTypeId getPatientOrderTrackingTypeId() {
		return this.patientOrderTrackingTypeId;
	}

	public void setPatientOrderTrackingTypeId(@Nullable PatientOrderTrackingTypeId patientOrderTrackingTypeId) {
		this.patientOrderTrackingTypeId = patientOrderTrackingTypeId;
	}

	@Nullable
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	public void setPatientOrderId(@Nullable UUID patientOrderId) {
		this.patientOrderId = patientOrderId;
	}

	@Nullable
	public UUID getAccountId() {
		return this.accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public String getMessage() {
		return this.message;
	}

	public void setMessage(@Nullable String message) {
		this.message = message;
	}

	@Nullable
	public Instant getCreated() {
		return this.created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}