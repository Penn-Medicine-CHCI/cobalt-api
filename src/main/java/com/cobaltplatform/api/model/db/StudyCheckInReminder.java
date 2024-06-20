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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class StudyCheckInReminder {
	@Nullable
	private UUID studyCheckInReminderId;
	@Nullable
	private UUID studyId;
	@Nullable
	private Integer checkInReminderNotificationMinutes;
	@Nullable
	private String checkInReminderNotificationMessageTitle;
	@Nullable
	private String checkInReminderNotificationMessageBody;
	@Nonnull
	private Instant created;
	@Nonnull
	private Instant lastUpdated;
	@Nonnull
	private Boolean sendCheckInReminderNotification;

	@Nullable
	public UUID getStudyCheckInReminderId() {
		return studyCheckInReminderId;
	}

	public void setStudyCheckInReminderId(@Nullable UUID studyCheckInReminderId) {
		this.studyCheckInReminderId = studyCheckInReminderId;
	}

	@Nullable
	public UUID getStudyId() {
		return studyId;
	}

	public void setStudyId(@Nullable UUID studyId) {
		this.studyId = studyId;
	}

	public Boolean getSendCheckInReminderNotification() {
		return sendCheckInReminderNotification;
	}

	public void setSendCheckInReminderNotification(Boolean sendCheckInReminderNotification) {
		this.sendCheckInReminderNotification = sendCheckInReminderNotification;
	}

	@Nullable
	public Integer getCheckInReminderNotificationMinutes() {
		return checkInReminderNotificationMinutes;
	}

	public void setCheckInReminderNotificationMinutes(@Nullable Integer checkInReminderNotificationMinutes) {
		this.checkInReminderNotificationMinutes = checkInReminderNotificationMinutes;
	}

	@Nullable
	public String getCheckInReminderNotificationMessageTitle() {
		return checkInReminderNotificationMessageTitle;
	}

	public void setCheckInReminderNotificationMessageTitle(@Nullable String checkInReminderNotificationMessageTitle) {
		this.checkInReminderNotificationMessageTitle = checkInReminderNotificationMessageTitle;
	}

	@Nullable
	public String getCheckInReminderNotificationMessageBody() {
		return checkInReminderNotificationMessageBody;
	}

	public void setCheckInReminderNotificationMessageBody(@Nullable String checkInReminderNotificationMessageBody) {
		this.checkInReminderNotificationMessageBody = checkInReminderNotificationMessageBody;
	}

	@Nonnull
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nonnull Instant created) {
		this.created = created;
	}

	@Nonnull
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nonnull Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}