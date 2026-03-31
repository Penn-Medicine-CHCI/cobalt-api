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

import com.cobaltplatform.api.model.db.ContentSnippet;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ContentSnippetApiResponse {
	@Nonnull
	private final UUID contentSnippetId;
	@Nonnull
	private final InstitutionId institutionId;
	@Nonnull
	private final String contentSnippetKey;
	@Nonnull
	private final String contentSnippetTypeId;
	@Nullable
	private final String title;
	@Nullable
	private final String bodyHtml;
	@Nullable
	private final String dismissButtonText;
	@Nonnull
	private final Map<String, Object> content;

	@ThreadSafe
	public interface ContentSnippetApiResponseFactory {
		@Nonnull
		ContentSnippetApiResponse create(@Nonnull ContentSnippet contentSnippet);
	}

	@AssistedInject
	public ContentSnippetApiResponse(@Nonnull Formatter formatter,
																	 @Nonnull Strings strings,
																	 @Assisted @Nonnull ContentSnippet contentSnippet) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(contentSnippet);

		this.contentSnippetId = contentSnippet.getContentSnippetId();
		this.institutionId = contentSnippet.getInstitutionId();
		this.contentSnippetKey = contentSnippet.getContentSnippetKey();
		this.contentSnippetTypeId = contentSnippet.getContentSnippetTypeId();
		this.title = contentSnippet.getTitle();
		this.bodyHtml = contentSnippet.getBodyHtml();
		this.dismissButtonText = contentSnippet.getDismissButtonText();
		this.content = Collections.unmodifiableMap(new HashMap<>(contentSnippet.getContent()));
	}

	@Nonnull
	public UUID getContentSnippetId() {
		return this.contentSnippetId;
	}

	@Nonnull
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	@Nonnull
	public String getContentSnippetKey() {
		return this.contentSnippetKey;
	}

	@Nonnull
	public String getContentSnippetTypeId() {
		return this.contentSnippetTypeId;
	}

	@Nonnull
	public Optional<String> getTitle() {
		return Optional.ofNullable(this.title);
	}

	@Nonnull
	public Optional<String> getBodyHtml() {
		return Optional.ofNullable(this.bodyHtml);
	}

	@Nonnull
	public Optional<String> getDismissButtonText() {
		return Optional.ofNullable(this.dismissButtonText);
	}

	@Nonnull
	public Map<String, Object> getContent() {
		return this.content;
	}
}
