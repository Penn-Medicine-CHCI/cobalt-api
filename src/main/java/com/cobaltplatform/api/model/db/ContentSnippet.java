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
public class ContentSnippet {
	@Nonnull
	private static final Gson GSON;

	static {
		GSON = new GsonBuilder()
				.setPrettyPrinting()
				.disableHtmlEscaping()
				.create();
	}

	@Nullable
	private UUID contentSnippetId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private String contentSnippetKey;
	@Nullable
	private String contentSnippetTypeId;
	@Nullable
	private String title;
	@Nullable
	private String bodyHtml;
	@Nullable
	private String dismissButtonText;
	@Nullable
	@DatabaseColumn("content")
	private String contentAsString;
	@Nullable
	private Map<String, Object> content;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nonnull
	protected Gson getGson() {
		return GSON;
	}

	@Nullable
	public UUID getContentSnippetId() {
		return this.contentSnippetId;
	}

	public void setContentSnippetId(@Nullable UUID contentSnippetId) {
		this.contentSnippetId = contentSnippetId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public String getContentSnippetKey() {
		return this.contentSnippetKey;
	}

	public void setContentSnippetKey(@Nullable String contentSnippetKey) {
		this.contentSnippetKey = contentSnippetKey;
	}

	@Nullable
	public String getContentSnippetTypeId() {
		return this.contentSnippetTypeId;
	}

	public void setContentSnippetTypeId(@Nullable String contentSnippetTypeId) {
		this.contentSnippetTypeId = contentSnippetTypeId;
	}

	@Nullable
	public String getTitle() {
		return this.title;
	}

	public void setTitle(@Nullable String title) {
		this.title = title;
	}

	@Nullable
	public String getBodyHtml() {
		return this.bodyHtml;
	}

	public void setBodyHtml(@Nullable String bodyHtml) {
		this.bodyHtml = bodyHtml;
	}

	@Nullable
	public String getDismissButtonText() {
		return this.dismissButtonText;
	}

	public void setDismissButtonText(@Nullable String dismissButtonText) {
		this.dismissButtonText = dismissButtonText;
	}

	@Nullable
	public String getContentAsString() {
		return this.contentAsString;
	}

	public void setContentAsString(@Nullable String contentAsString) {
		this.contentAsString = contentAsString;

		String content = trimToNull(contentAsString);
		this.content = content == null ? Map.of() : getGson().fromJson(content, new TypeToken<Map<String, Object>>() {
		}.getType());
	}

	@Nonnull
	public Map<String, Object> getContent() {
		return this.content == null ? Map.of() : this.content;
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
