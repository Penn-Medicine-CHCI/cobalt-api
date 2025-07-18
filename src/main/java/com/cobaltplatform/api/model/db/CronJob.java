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

import com.cobaltplatform.api.model.db.CronJobRunStatus.CronJobRunStatusId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
public class CronJob {
	@Nullable
	private UUID cronJobId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private String cronExpression;
	@Nonnull
	private ZoneId timeZone;
	@Nullable
	private String callbackType;
	@Nullable
	private String callbackPayload; // JSON string
	@Nullable
	private Instant nextRunAt;
	@Nullable
	private Instant lastRunStartedAt;
	@Nullable
	private Instant lastRunFinishedAt;
	@Nullable
	private CronJobRunStatusId lastRunStatusId;
	@Nullable
	private String lastRunStackTrace;
	@Nullable
	private Boolean enabled;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getCronJobId() {
		return this.cronJobId;
	}

	public void setCronJobId(@Nullable UUID cronJobId) {
		this.cronJobId = cronJobId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public String getCronExpression() {
		return this.cronExpression;
	}

	public void setCronExpression(@Nullable String cronExpression) {
		this.cronExpression = cronExpression;
	}

	@Nonnull
	public ZoneId getTimeZone() {
		return this.timeZone;
	}

	public void setTimeZone(@Nonnull ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	@Nullable
	public String getCallbackType() {
		return this.callbackType;
	}

	public void setCallbackType(@Nullable String callbackType) {
		this.callbackType = callbackType;
	}

	@Nullable
	public String getCallbackPayload() {
		return this.callbackPayload;
	}

	public void setCallbackPayload(@Nullable String callbackPayload) {
		this.callbackPayload = callbackPayload;
	}

	@Nullable
	public Instant getNextRunAt() {
		return this.nextRunAt;
	}

	public void setNextRunAt(@Nullable Instant nextRunAt) {
		this.nextRunAt = nextRunAt;
	}

	@Nullable
	public Instant getLastRunStartedAt() {
		return this.lastRunStartedAt;
	}

	public void setLastRunStartedAt(@Nullable Instant lastRunStartedAt) {
		this.lastRunStartedAt = lastRunStartedAt;
	}

	@Nullable
	public Instant getLastRunFinishedAt() {
		return this.lastRunFinishedAt;
	}

	public void setLastRunFinishedAt(@Nullable Instant lastRunFinishedAt) {
		this.lastRunFinishedAt = lastRunFinishedAt;
	}

	@Nullable
	public CronJobRunStatusId getLastRunStatusId() {
		return this.lastRunStatusId;
	}

	public void setLastRunStatusId(@Nullable CronJobRunStatusId lastRunStatusId) {
		this.lastRunStatusId = lastRunStatusId;
	}

	@Nullable
	public String getLastRunStackTrace() {
		return this.lastRunStackTrace;
	}

	public void setLastRunStackTrace(@Nullable String lastRunStackTrace) {
		this.lastRunStackTrace = lastRunStackTrace;
	}

	@Nullable
	public Boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(@Nullable Boolean enabled) {
		this.enabled = enabled;
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
}
