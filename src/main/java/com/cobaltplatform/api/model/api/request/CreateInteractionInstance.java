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

package com.cobaltplatform.api.model.api.request;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreateInteractionInstance {
	@Nullable
	private UUID interactionId;
	@Nullable
	private LocalDateTime startDateTime;
	@Nullable
	private String metaData;
	@Nullable
	private UUID accountId;

	@Nullable
	public UUID getInteractionId() {
		return interactionId;
	}

	public void setInteractionId(@Nullable UUID interactionId) {
		this.interactionId = interactionId;
	}

	@Nullable
	public LocalDateTime getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(@Nullable LocalDateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

	@Nullable
	public String getmetadata() {
		return metaData;
	}

	public void setmetadata(@Nullable String metaData) {
		this.metaData = metaData;
	}

	@Nullable
	public UUID getAccountId() {
		return accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}
}
