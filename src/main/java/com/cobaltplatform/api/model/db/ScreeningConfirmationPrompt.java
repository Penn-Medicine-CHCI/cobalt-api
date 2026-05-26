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

import com.cobaltplatform.api.model.db.ScreeningImage.ScreeningImageId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ScreeningConfirmationPrompt {
	@Nullable
	private UUID screeningConfirmationPromptId;
	@Nullable
	private ScreeningImageId screeningImageId;
	@Nullable
	private String text;
	@Nullable
	private String titleText;
	@Nullable
	private String actionText;
	@Nullable
	private List<ScreeningConfirmationPromptCallout> callouts;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getScreeningConfirmationPromptId() {
		return this.screeningConfirmationPromptId;
	}

	public void setScreeningConfirmationPromptId(@Nullable UUID screeningConfirmationPromptId) {
		this.screeningConfirmationPromptId = screeningConfirmationPromptId;
	}

	@Nullable
	public ScreeningImageId getScreeningImageId() {
		return this.screeningImageId;
	}

	public void setScreeningImageId(@Nullable ScreeningImageId screeningImageId) {
		this.screeningImageId = screeningImageId;
	}

	@Nullable
	public String getText() {
		return this.text;
	}

	public void setText(@Nullable String text) {
		this.text = text;
	}

	@Nullable
	public String getTitleText() {
		return this.titleText;
	}

	public void setTitleText(@Nullable String titleText) {
		this.titleText = titleText;
	}

	@Nullable
	public String getActionText() {
		return this.actionText;
	}

	public void setActionText(@Nullable String actionText) {
		this.actionText = actionText;
	}

	@Nonnull
	public List<ScreeningConfirmationPromptCallout> getCallouts() {
		return this.callouts == null ? List.of() : this.callouts;
	}

	public void setCallouts(@Nullable List<ScreeningConfirmationPromptCallout> callouts) {
		this.callouts = callouts;
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

	@NotThreadSafe
	public static class ScreeningConfirmationPromptCallout {
		@Nullable
		private String title;
		@Nullable
		private String descriptionHtml;
		@Nullable
		private String url;
		@Nullable
		private String urlText;

		public ScreeningConfirmationPromptCallout() {

		}

		public ScreeningConfirmationPromptCallout(@Nullable String title,
																						 @Nullable String descriptionHtml,
																						 @Nullable String url,
																						 @Nullable String urlText) {
			this.title = title;
			this.descriptionHtml = descriptionHtml;
			this.url = url;
			this.urlText = urlText;
		}

		@Nullable
		public String getTitle() {
			return this.title;
		}

		public void setTitle(@Nullable String title) {
			this.title = title;
		}

		@Nullable
		public String getDescriptionHtml() {
			return this.descriptionHtml;
		}

		public void setDescriptionHtml(@Nullable String descriptionHtml) {
			this.descriptionHtml = descriptionHtml;
		}

		@Nullable
		public String getUrl() {
			return this.url;
		}

		public void setUrl(@Nullable String url) {
			this.url = url;
		}

		@Nullable
		public String getUrlText() {
			return this.urlText;
		}

		public void setUrlText(@Nullable String urlText) {
			this.urlText = urlText;
		}
	}
}
