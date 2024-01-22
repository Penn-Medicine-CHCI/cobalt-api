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

import com.cobaltplatform.api.model.db.ContentFeedback;
import com.cobaltplatform.api.model.db.ContentFeedbackType.ContentFeedbackTypeId;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class ContentFeedbackApiResponse {
	@Nonnull
	private final UUID contentFeedbackId;
	@Nonnull
	private final ContentFeedbackTypeId contentFeedbackTypeId;
	@Nonnull
	private final UUID contentId;
	@Nonnull
	private final UUID accountId;
	@Nullable
	private final String message;
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
	public interface ContentFeedbackApiResponseFactory {
		@Nonnull
		ContentFeedbackApiResponse create(@Nonnull ContentFeedback contentFeedback);
	}

	@AssistedInject
	public ContentFeedbackApiResponse(@Nonnull Formatter formatter,
																		@Nonnull Strings strings,
																		@Assisted @Nonnull ContentFeedback contentFeedback) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(contentFeedback);

		this.contentFeedbackId = contentFeedback.getContentFeedbackId();
		this.contentFeedbackTypeId = contentFeedback.getContentFeedbackTypeId();
		this.contentId = contentFeedback.getContentId();
		this.accountId = contentFeedback.getAccountId();
		this.message = contentFeedback.getMessage();
		this.created = contentFeedback.getCreated();
		this.createdDescription = formatter.formatTimestamp(contentFeedback.getCreated());
		this.lastUpdated = contentFeedback.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(contentFeedback.getLastUpdated());
	}

	@Nonnull
	public UUID getContentFeedbackId() {
		return this.contentFeedbackId;
	}

	@Nonnull
	public ContentFeedbackTypeId getContentFeedbackTypeId() {
		return this.contentFeedbackTypeId;
	}

	@Nonnull
	public UUID getContentId() {
		return this.contentId;
	}

	@Nonnull
	public UUID getAccountId() {
		return this.accountId;
	}

	@Nonnull
	public Optional<String> getMessage() {
		return Optional.ofNullable(this.message);
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