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

import com.cobaltplatform.api.model.db.ScreeningConfirmationPrompt;
import com.cobaltplatform.api.model.db.ScreeningImage.ScreeningImageId;
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
public class ScreeningConfirmationPromptApiResponse {
	@Nonnull
	private final UUID screeningConfirmationPromptId;
	@Nullable
	private final ScreeningImageId screeningImageId;
	@Nullable
	private final String text;
	@Nonnull
	private final String actionText;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ScreeningConfirmationPromptApiResponseFactory {
		@Nonnull
		ScreeningConfirmationPromptApiResponse create(@Nonnull ScreeningConfirmationPrompt screeningConfirmationPrompt);
	}

	@AssistedInject
	public ScreeningConfirmationPromptApiResponse(@Nonnull Formatter formatter,
																								@Nonnull Strings strings,
																								@Assisted @Nonnull ScreeningConfirmationPrompt screeningConfirmationPrompt) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(screeningConfirmationPrompt);

		this.screeningConfirmationPromptId = screeningConfirmationPrompt.getScreeningConfirmationPromptId();
		this.screeningImageId = screeningConfirmationPrompt.getScreeningImageId();
		this.text = screeningConfirmationPrompt.getText();
		this.actionText = screeningConfirmationPrompt.getActionText();
	}

	@Nonnull
	public UUID getScreeningConfirmationPromptId() {
		return this.screeningConfirmationPromptId;
	}

	@Nonnull
	public Optional<ScreeningImageId> getScreeningImageId() {
		return Optional.ofNullable(this.screeningImageId);
	}

	@Nonnull
	public Optional<String> getText() {
		return Optional.ofNullable(this.text);
	}

	@Nonnull
	public String getActionText() {
		return this.actionText;
	}
}