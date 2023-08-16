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

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.GroupSessionRequest;
import com.cobaltplatform.api.model.db.GroupSessionRequestStatus.GroupSessionRequestStatusId;
import com.cobaltplatform.api.model.db.GroupSessionSuggestion;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.service.GroupSessionService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.FormatStyle;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class GroupSessionSuggestionApiResponse {
	@Nonnull
	private final UUID groupSessionSuggestionId;
	@Nullable
	private final String title;
	@Nullable
	private final String description;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface GroupSessionSuggestionApiResponseFactory {
		@Nonnull
		GroupSessionSuggestionApiResponse create(@Nonnull GroupSessionSuggestion groupSessionSuggestion);
	}

	@AssistedInject
	public GroupSessionSuggestionApiResponse(@Assisted @Nonnull GroupSessionSuggestion groupSessionSuggestion) {
		requireNonNull(groupSessionSuggestion);

		this.groupSessionSuggestionId = groupSessionSuggestion.getGroupSessionSuggestionId();
		this.title = groupSessionSuggestion.getTitle();
		this.description = groupSessionSuggestion.getDescription();
	}

	@Nonnull
	public UUID getGroupSessionSuggestionId() {
		return groupSessionSuggestionId;
	}

	@Nullable
	public String getTitle() {
		return title;
	}

	@Nullable
	public String getDescription() {
		return description;
	}
}
