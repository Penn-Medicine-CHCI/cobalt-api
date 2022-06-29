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

package com.cobaltplatform.api.model.service;

import com.devskiller.friendly_id.FriendlyId;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Combines screeningSessionScreeningId and screeningQuestionId into a single identifier string for API ease-of-use.
 *
 * @author Transmogrify, LLC.
 */
@Immutable
public class ScreeningQuestionContextId {
	@Nonnull
	private final String identifier;
	@Nonnull
	private final UUID screeningSessionScreeningId;
	@Nonnull
	private final UUID screeningQuestionId;

	public ScreeningQuestionContextId(@Nonnull UUID screeningSessionScreeningId,
																		@Nonnull UUID screeningQuestionId) {
		requireNonNull(screeningSessionScreeningId);
		requireNonNull(screeningQuestionId);

		this.screeningSessionScreeningId = screeningSessionScreeningId;
		this.screeningQuestionId = screeningQuestionId;
		this.identifier = format("%s-%s", FriendlyId.toFriendlyId(screeningSessionScreeningId), FriendlyId.toFriendlyId(screeningQuestionId));
	}

	public ScreeningQuestionContextId(@Nonnull String screeningQuestionContextId) {
		requireNonNull(screeningQuestionContextId);

		screeningQuestionContextId = screeningQuestionContextId.trim();

		try {
			String[] components = screeningQuestionContextId.split("-");

			this.screeningSessionScreeningId = FriendlyId.toUuid(components[0]);
			this.screeningQuestionId = FriendlyId.toUuid(components[1]);
			this.identifier = screeningQuestionContextId;
		} catch (Exception e) {
			throw new IllegalArgumentException(format("Illegal ScreeningQuestionContextId was specified: '%s'", screeningQuestionContextId));
		}
	}

	@Nonnull
	public String getIdentifier() {
		return this.identifier;
	}

	@Nonnull
	public UUID getScreeningSessionScreeningId() {
		return this.screeningSessionScreeningId;
	}

	@Nonnull
	public UUID getScreeningQuestionId() {
		return this.screeningQuestionId;
	}
}
