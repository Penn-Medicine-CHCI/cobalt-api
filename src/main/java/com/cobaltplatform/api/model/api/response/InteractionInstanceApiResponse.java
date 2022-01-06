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

import com.cobaltplatform.api.model.db.InteractionInstance;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDateTime;
import java.time.format.FormatStyle;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class InteractionInstanceApiResponse {
	@Nullable
	private UUID interactionInstanceId;
	@Nullable
	private UUID interactionId;
	@Nullable
	private UUID accountId;
	@Nullable
	private LocalDateTime startDateTime;
	@Nullable
	private String startDateTimeDescription;
	@Nullable
	private String timeZone;
	@Nullable
	private String metaData;


	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface InteractionInstanceApiResponseFactory {
		@Nonnull
		InteractionInstanceApiResponse create(InteractionInstance interactionInstance);
	}

	@AssistedInject
	public InteractionInstanceApiResponse(@Nonnull Formatter formatter,
																				@Nonnull Strings strings,
																				@Assisted @Nonnull InteractionInstance interactionInstance) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(interactionInstance);

		this.interactionId = interactionInstance.getInteractionId();
		this.interactionInstanceId = interactionInstance.getInteractionInstanceId();
		this.accountId = interactionInstance.getAccountId();
		this.metaData = interactionInstance.getmetadata();
		this.timeZone = interactionInstance.getTimeZone();
		this.startDateTime = interactionInstance.getStartDateTime();
		this.startDateTimeDescription = formatter.formatDateTime(interactionInstance.getStartDateTime(), FormatStyle.LONG, FormatStyle.MEDIUM);

	}


	@Nonnull
	public LocalDateTime getStartDateTime() {
		return startDateTime;
	}

}