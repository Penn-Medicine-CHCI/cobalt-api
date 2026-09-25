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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

@NotThreadSafe
public class ScreeningDestinationContent {
	@Nullable
	private UUID screeningFlowId;
	@Nullable
	private String screeningSessionDestinationId;
	@Nullable
	private String title;
	@Nullable
	private String message;
	@Nullable
	private String actionUrl;
	@Nullable
	private String actionText;
	@Nullable
	private String contactName;
	@Nullable
	private String contactPhone;

	@Nullable
	public UUID getScreeningFlowId() {
		return screeningFlowId;
	}

	@Nullable
	public String getScreeningSessionDestinationId() {
		return screeningSessionDestinationId;
	}

	@Nullable
	public String getTitle() {
		return title;
	}

	@Nullable
	public String getMessage() {
		return message;
	}

	@Nullable
	public String getActionUrl() {
		return actionUrl;
	}

	@Nullable
	public String getActionText() {
		return actionText;
	}

	@Nullable
	public String getContactName() {
		return contactName;
	}

	@Nullable
	public String getContactPhone() {
		return contactPhone;
	}
}
