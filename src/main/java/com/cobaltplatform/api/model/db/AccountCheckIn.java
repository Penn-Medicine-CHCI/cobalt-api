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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import com.cobaltplatform.api.model.db.CheckInType.CheckInTypeId;
import com.cobaltplatform.api.model.db.CheckInStatus.CheckInStatusId;


/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AccountCheckIn {
	@Nullable
	private UUID accountCheckInId;
	@Nullable
	private UUID accountId;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	private UUID studyID;

	@Nullable
	private CheckInTypeId checkInTypeId;

	@Nullable
	private Integer checkInNumber;

	@Nullable
	private LocalDateTime checkInStartDateTime;

	@Nullable
	private LocalDateTime checkInEndDateTime;

	@Nullable
	private LocalDateTime completedDate;

	@Nullable
	private CheckInStatusId checkInStatusId;

	@Nullable
	public UUID getAccountCheckInId() {
		return accountCheckInId;
	}

	public void setAccountCheckInId(@Nullable UUID accountCheckInId) {
		this.accountCheckInId = accountCheckInId;
	}

	@Nullable
	public UUID getAccountId() {
		return accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public CheckInTypeId getCheckInTypeId() {
		return checkInTypeId;
	}

	public void setCheckInTypeId(@Nullable CheckInTypeId checkInTypeId) {
		this.checkInTypeId = checkInTypeId;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	@Nullable
	public UUID getStudyID() {
		return studyID;
	}

	public void setStudyID(@Nullable UUID studyID) {
		this.studyID = studyID;
	}

	@Nullable
	public Integer getCheckInNumber() {
		return checkInNumber;
	}

	public void setCheckInNumber(@Nullable Integer checkInNumber) {
		this.checkInNumber = checkInNumber;
	}

	@Nullable
	public LocalDateTime getCheckInStartDateTime() {
		return checkInStartDateTime;
	}

	public void setCheckInStartDateTime(@Nullable LocalDateTime checkInStartDateTime) {
		this.checkInStartDateTime = checkInStartDateTime;
	}

	@Nullable
	public LocalDateTime getCheckInEndDateTime() {
		return checkInEndDateTime;
	}

	public void setCheckInEndDateTime(@Nullable LocalDateTime checkInEndDateTime) {
		this.checkInEndDateTime = checkInEndDateTime;
	}

	@Nullable
	public CheckInStatusId getCheckInStatusId() {
		return checkInStatusId;
	}

	public void setCheckInStatusId(@Nullable CheckInStatusId checkInStatusId) {
		this.checkInStatusId = checkInStatusId;
	}

	@Nullable
	public LocalDateTime getCompletedDate() {
		return completedDate;
	}

	public void setCompletedDate(@Nullable LocalDateTime completedDate) {
		this.completedDate = completedDate;
	}
}