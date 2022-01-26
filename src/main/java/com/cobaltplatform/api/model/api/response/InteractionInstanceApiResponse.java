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
import com.cobaltplatform.api.util.JsonMapper;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.FormatStyle;
import java.util.Map;
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
	private ZoneId timeZone;
	@Nullable
	private Boolean completedFlag;
	@Nullable
	private Instant completedDate;
	@Nullable
	private String completedDateDescription;
	@Nullable
	private Map<String, Object> metadata;
	@Nullable
	private Map<String, Object> hipaaCompliantMetadata;
	@Nullable
	private String caseNumber;


	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface InteractionInstanceApiResponseFactory {
		@Nonnull
		InteractionInstanceApiResponse create(InteractionInstance interactionInstance);
	}

	@AssistedInject
	public InteractionInstanceApiResponse(@Nonnull Formatter formatter,
																				@Nonnull Strings strings,
																				@Nonnull JsonMapper jsonMapper,
																				@Assisted @Nonnull InteractionInstance interactionInstance) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(jsonMapper);
		requireNonNull(interactionInstance);

		this.interactionId = interactionInstance.getInteractionId();
		this.interactionInstanceId = interactionInstance.getInteractionInstanceId();
		this.accountId = interactionInstance.getAccountId();
		this.metadata = interactionInstance.getMetadata() == null ? null : jsonMapper.toMapFromRawJson(interactionInstance.getMetadata());
		this.hipaaCompliantMetadata = interactionInstance.getHipaaCompliantMetadata() == null ? null : jsonMapper.toMapFromRawJson(interactionInstance.getHipaaCompliantMetadata());
		this.timeZone = interactionInstance.getTimeZone();
		this.startDateTime = interactionInstance.getStartDateTime();
		this.startDateTimeDescription = formatter.formatDateTime(interactionInstance.getStartDateTime(), FormatStyle.LONG, FormatStyle.MEDIUM);
		this.completedDate = interactionInstance.getCompletedDate();
		this.completedDateDescription = interactionInstance.getCompletedDate() == null ? null : formatter.formatTimestamp(interactionInstance.getCompletedDate(), FormatStyle.LONG, FormatStyle.MEDIUM);
		this.completedFlag = interactionInstance.getCompletedFlag();
		this.caseNumber = interactionInstance.getCaseNumber();
	}

	@Nullable
	public UUID getInteractionInstanceId() {
		return interactionInstanceId;
	}

	@Nullable
	public UUID getInteractionId() {
		return interactionId;
	}

	@Nullable
	public UUID getAccountId() {
		return accountId;
	}

	@Nullable
	public LocalDateTime getStartDateTime() {
		return startDateTime;
	}

	@Nullable
	public String getStartDateTimeDescription() {
		return startDateTimeDescription;
	}

	@Nullable
	public ZoneId getTimeZone() {
		return timeZone;
	}

	@Nullable
	public Map<String, Object> getMetadata() {
		return metadata;
	}

	@Nullable
	public Boolean getCompletedFlag() {
		return completedFlag;
	}

	@Nullable
	public Instant getCompletedDate() {
		return completedDate;
	}

	@Nullable
	public String getCompletedDateDescription() {
		return completedDateDescription;
	}

	@Nullable
	public Map<String, Object> getHipaaCompliantMetadata() {
		return hipaaCompliantMetadata;
	}

	@Nullable
	public String getCaseNumber() {
		return caseNumber;
	}
}