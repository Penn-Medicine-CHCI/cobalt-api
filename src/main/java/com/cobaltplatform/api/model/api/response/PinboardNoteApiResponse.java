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

import com.cobaltplatform.api.model.db.PinboardNote;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class PinboardNoteApiResponse {
	@Nonnull
	private final UUID pinboardNoteId;
	@Nonnull
	private final String title;
	@Nonnull
	private final String description;
	@Nullable
	private final String url;
	@Nullable
	private final String imageUrl;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PinboardNoteApiResponseFactory {
		@Nonnull
		PinboardNoteApiResponse create(@Nonnull PinboardNote pinboardNote);
	}

	@AssistedInject
	public PinboardNoteApiResponse(@Nonnull Formatter formatter,
																 @Nonnull Strings strings,
																 @Assisted @Nonnull PinboardNote pinboardNote) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(pinboardNote);

		this.pinboardNoteId = pinboardNote.getPinboardNoteId();
		this.title = pinboardNote.getTitle();
		this.description = pinboardNote.getDescription();
		this.url = pinboardNote.getUrl();
		this.imageUrl = pinboardNote.getImageUrl();
	}

	@Nonnull
	public UUID getPinboardNoteId() {
		return this.pinboardNoteId;
	}

	@Nonnull
	public String getTitle() {
		return this.title;
	}

	@Nonnull
	public String getDescription() {
		return this.description;
	}

	@Nonnull
	public Optional<String> getUrl() {
		return Optional.ofNullable(this.url);
	}

	@Nullable
	public Optional<String> getImageUrl() {
		return Optional.ofNullable(this.imageUrl);
	}
}