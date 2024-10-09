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
public class MyChartAuthenticationClaims {
	@Nonnull
	private static final Gson GSON;

	static {
		GSON = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping()
				.create();
	}

	@Nullable
	@DatabaseColumn("mychart_authentication_claims_id")
	private UUID myChartAuthenticationClaimsId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	@DatabaseColumn("claims")
	private String claimsAsString;
	@Nullable
	private Map<String, Object> claims;
	@Nullable
	private Instant consumedAt;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	public MyChartAuthenticationClaims() {
		this.claims = Map.of();
	}

	@Nonnull
	protected Gson getGson() {
		return GSON;
	}

	@Nullable
	public String getClaimsAsString() {
		return this.claimsAsString;
	}

	public void setClaimsAsString(@Nullable String claimsAsString) {
		this.claimsAsString = claimsAsString;

		String claims = trimToNull(claimsAsString);
		this.claims = claims == null ? Map.of() : getGson().fromJson(claims, new TypeToken<Map<String, Object>>() {
		}.getType());
	}

	@Nonnull
	public Map<String, Object> getClaims() {
		return this.claims;
	}

	@Nullable
	public UUID getMyChartAuthenticationClaimsId() {
		return this.myChartAuthenticationClaimsId;
	}

	public void setMyChartAuthenticationClaimsId(@Nullable UUID myChartAuthenticationClaimsId) {
		this.myChartAuthenticationClaimsId = myChartAuthenticationClaimsId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public Instant getConsumedAt() {
		return this.consumedAt;
	}

	public void setConsumedAt(@Nullable Instant consumedAt) {
		this.consumedAt = consumedAt;
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