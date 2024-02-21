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

import com.cobaltplatform.api.model.db.FlowsheetType.FlowsheetTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pyranid.DatabaseColumn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class Flowsheet {
	@Nonnull
	private static final Gson GSON;

	static {
		GsonBuilder gsonBuilder = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping();

		GSON = gsonBuilder.create();
	}

	@Nullable
	private UUID flowsheetId;
	@Nullable
	private FlowsheetTypeId flowsheetTypeId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private String epicFlowsheetId;
	@Nullable
	private String epicFlowsheetTemplateId;
	@DatabaseColumn("permitted_values")
	private String permittedValuesAsJson;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nonnull
	public List<Object> getPermittedValues() {
		return this.permittedValuesAsJson == null ? List.of() : GSON.fromJson(this.permittedValuesAsJson, new TypeToken<List<Object>>() {
		}.getType());
	}

	@Nullable
	public UUID getFlowsheetId() {
		return this.flowsheetId;
	}

	public void setFlowsheetId(@Nullable UUID flowsheetId) {
		this.flowsheetId = flowsheetId;
	}

	@Nullable
	public FlowsheetTypeId getFlowsheetTypeId() {
		return this.flowsheetTypeId;
	}

	public void setFlowsheetTypeId(@Nullable FlowsheetTypeId flowsheetTypeId) {
		this.flowsheetTypeId = flowsheetTypeId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public String getEpicFlowsheetId() {
		return this.epicFlowsheetId;
	}

	public void setEpicFlowsheetId(@Nullable String epicFlowsheetId) {
		this.epicFlowsheetId = epicFlowsheetId;
	}

	@Nullable
	public String getEpicFlowsheetTemplateId() {
		return this.epicFlowsheetTemplateId;
	}

	public void setEpicFlowsheetTemplateId(@Nullable String epicFlowsheetTemplateId) {
		this.epicFlowsheetTemplateId = epicFlowsheetTemplateId;
	}

	public String getPermittedValuesAsJson() {
		return this.permittedValuesAsJson;
	}

	public void setPermittedValuesAsJson(String permittedValuesAsJson) {
		this.permittedValuesAsJson = permittedValuesAsJson;
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