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

import com.cobaltplatform.api.model.service.CallToAction;
import com.cobaltplatform.api.model.service.CallToAction.ActionLink;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class CallToActionApiResponse {
	@Nonnull
	private final String message;
	@Nonnull
	private final String messageAsHtml;
	@Nullable
	private final String modalButtonText;
	@Nullable
	private final String modalMessage;
	@Nullable
	private final String modalMessageAsHtml;
	@Nonnull
	private final List<Map<String, Object>> actionLinks;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface CallToActionApiResponseFactory {
		@Nonnull
		CallToActionApiResponse create(@Nonnull CallToAction callToAction);
	}

	@AssistedInject
	public CallToActionApiResponse(@Nonnull Formatter formatter,
																 @Nonnull Strings strings,
																 @Assisted @Nonnull CallToAction callToAction) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(callToAction);

		List<Map<String, Object>> actionLinkJsons = new ArrayList<>();

		if (callToAction.getActionLinks() != null) {
			for (ActionLink actionLink : callToAction.getActionLinks()) {
				Map<String, Object> actionLinkJson = new HashMap<>();
				actionLinkJson.put("actionLinkTypeId", actionLink.getActionLinkTypeId());
				actionLinkJson.put("description", actionLink.getDescription());
				actionLinkJson.put("link", actionLink.getLink());
				actionLinkJson.put("analyticsEventAction", actionLink.getAnalyticsEventAction());
				actionLinkJson.put("analyticsEventCategory", actionLink.getAnalyticsEventCategory());
				actionLinkJson.put("analyticsEventLabel", actionLink.getAnalyticsEventLabel());

				actionLinkJsons.add(actionLinkJson);
			}
		}

		this.message = callToAction.getMessage();
		this.messageAsHtml = callToAction.getMessageAsHtml();
		this.modalButtonText = callToAction.getModalButtonText();
		this.modalMessage = callToAction.getModalMessage();
		this.modalMessageAsHtml = callToAction.getModalMessageAsHtml();
		this.actionLinks = actionLinkJsons;
	}

	@Nonnull
	public String getMessage() {
		return this.message;
	}

	@Nonnull
	public String getMessageAsHtml() {
		return this.messageAsHtml;
	}

	@Nullable
	public String getModalButtonText() {
		return this.modalButtonText;
	}

	@Nullable
	public String getModalMessage() {
		return this.modalMessage;
	}

	@Nullable
	public String getModalMessageAsHtml() {
		return this.modalMessageAsHtml;
	}

	@Nonnull
	public List<Map<String, Object>> getActionLinks() {
		return this.actionLinks;
	}
}