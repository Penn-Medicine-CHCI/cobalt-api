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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CallToAction {
	@Nullable
	private String message;
	@Nullable
	private String messageAsHtml;
	@Nullable
	private String modalButtonText;
	@Nullable
	private String modalMessage;
	@Nullable
	private String modalMessageAsHtml;
	@Nullable
	private List<ActionLink> actionLinks;

	@Nonnull
	public enum ActionLinkTypeId {
		INTERNAL,
		EXTERNAL,
		CRISIS
	}

	@NotThreadSafe
	public static class ActionLink {
		@Nullable
		private ActionLinkTypeId actionLinkTypeId;
		@Nullable
		private String description;
		@Nullable
		private String link;
		@Nullable
		private String analyticsEventAction;
		@Nullable
		private String analyticsEventCategory;
		@Nullable
		private String analyticsEventLabel;

		@Nullable
		public ActionLinkTypeId getActionLinkTypeId() {
			return this.actionLinkTypeId;
		}

		public void setActionLinkTypeId(@Nullable ActionLinkTypeId actionLinkTypeId) {
			this.actionLinkTypeId = actionLinkTypeId;
		}

		@Nullable
		public String getDescription() {
			return this.description;
		}

		public void setDescription(@Nullable String description) {
			this.description = description;
		}

		@Nullable
		public String getLink() {
			return this.link;
		}

		public void setLink(@Nullable String link) {
			this.link = link;
		}

		@Nullable
		public String getAnalyticsEventAction() {
			return this.analyticsEventAction;
		}

		public void setAnalyticsEventAction(@Nullable String analyticsEventAction) {
			this.analyticsEventAction = analyticsEventAction;
		}

		@Nullable
		public String getAnalyticsEventCategory() {
			return this.analyticsEventCategory;
		}

		public void setAnalyticsEventCategory(@Nullable String analyticsEventCategory) {
			this.analyticsEventCategory = analyticsEventCategory;
		}

		@Nullable
		public String getAnalyticsEventLabel() {
			return this.analyticsEventLabel;
		}

		public void setAnalyticsEventLabel(@Nullable String analyticsEventLabel) {
			this.analyticsEventLabel = analyticsEventLabel;
		}
	}

	@Nullable
	public String getMessage() {
		return this.message;
	}

	public void setMessage(@Nullable String message) {
		this.message = message;
	}

	@Nullable
	public String getMessageAsHtml() {
		return this.messageAsHtml;
	}

	public void setMessageAsHtml(@Nullable String messageAsHtml) {
		this.messageAsHtml = messageAsHtml;
	}

	@Nullable
	public String getModalButtonText() {
		return this.modalButtonText;
	}

	public void setModalButtonText(@Nullable String modalButtonText) {
		this.modalButtonText = modalButtonText;
	}

	@Nullable
	public String getModalMessage() {
		return this.modalMessage;
	}

	public void setModalMessage(@Nullable String modalMessage) {
		this.modalMessage = modalMessage;
	}

	@Nullable
	public String getModalMessageAsHtml() {
		return this.modalMessageAsHtml;
	}

	public void setModalMessageAsHtml(@Nullable String modalMessageAsHtml) {
		this.modalMessageAsHtml = modalMessageAsHtml;
	}

	@Nullable
	public List<ActionLink> getActionLinks() {
		return this.actionLinks;
	}

	public void setActionLinks(@Nullable List<ActionLink> actionLinks) {
		this.actionLinks = actionLinks;
	}
}
