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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Study {
	@Nullable
	private UUID studyId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private String name;
	@Nullable
	private String urlName;
	@Nullable
	private String onboardingDestinationUrl;
	@Nullable
	private Integer minutesBetweenCheckIns;
	@Nullable
	private Integer gracePeriodInMinutes;
	@Nullable
	private String coordinatorName;
	@Nullable
	private String coordinatorEmailAddress;
	@Nullable
	private String coordinatorPhoneNumber;
	@Nullable
	private String coordinatorAvailability;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;
	@Nullable
	private Boolean sendReminderNotification;
	@Nullable
	private Integer reminderNotificationMinutes;
	@Nullable
	private String reminderNotificationMessageTitle;
	@Nullable
	private String reminderNotificationMessageBody;
	@Nullable
	private Boolean resetAfterFinalCheckIn;

	@Nullable
	private Boolean leaveFirstCheckInOpenUntilStarted;

	@Nullable
	private Boolean checkInWindowsFixed;

	@Nullable
	public UUID getStudyId() {
		return studyId;
	}

	public void setStudyId(@Nullable UUID studyId) {
		this.studyId = studyId;
	}

	@Nullable
	public String getName() {
		return this.name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getUrlName() {
		return this.urlName;
	}

	public void setUrlName(@Nullable String urlName) {
		this.urlName = urlName;
	}

	@Nullable
	public String getOnboardingDestinationUrl() {
		return this.onboardingDestinationUrl;
	}

	public void setOnboardingDestinationUrl(@Nullable String onboardingDestinationUrl) {
		this.onboardingDestinationUrl = onboardingDestinationUrl;
	}

	@Nullable
	public Integer getMinutesBetweenCheckIns() {
		return minutesBetweenCheckIns;
	}

	public void setMinutesBetweenCheckIns(@Nullable Integer minutesBetweenCheckIns) {
		this.minutesBetweenCheckIns = minutesBetweenCheckIns;
	}

	@Nullable
	public Integer getGracePeriodInMinutes() {
		return gracePeriodInMinutes;
	}

	public void setGracePeriodInMinutes(@Nullable Integer gracePeriodInMinutes) {
		this.gracePeriodInMinutes = gracePeriodInMinutes;
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
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public String getCoordinatorName() {
		return this.coordinatorName;
	}

	public void setCoordinatorName(@Nullable String coordinatorName) {
		this.coordinatorName = coordinatorName;
	}

	@Nullable
	public String getCoordinatorEmailAddress() {
		return this.coordinatorEmailAddress;
	}

	public void setCoordinatorEmailAddress(@Nullable String coordinatorEmailAddress) {
		this.coordinatorEmailAddress = coordinatorEmailAddress;
	}

	@Nullable
	public String getCoordinatorPhoneNumber() {
		return this.coordinatorPhoneNumber;
	}

	public void setCoordinatorPhoneNumber(@Nullable String coordinatorPhoneNumber) {
		this.coordinatorPhoneNumber = coordinatorPhoneNumber;
	}

	@Nullable
	public String getCoordinatorAvailability() {
		return this.coordinatorAvailability;
	}

	public void setCoordinatorAvailability(@Nullable String coordinatorAvailability) {
		this.coordinatorAvailability = coordinatorAvailability;
	}

	@Nullable
	public Boolean getSendReminderNotification() {
		return sendReminderNotification;
	}

	public void setSendReminderNotification(@Nullable Boolean sendReminderNotification) {
		this.sendReminderNotification = sendReminderNotification;
	}

	@Nullable
	public Integer getReminderNotificationMinutes() {
		return reminderNotificationMinutes;
	}

	public void setReminderNotificationMinutes(@Nullable Integer reminderNotificationMinutes) {
		this.reminderNotificationMinutes = reminderNotificationMinutes;
	}

	@Nullable
	public String getReminderNotificationMessageTitle() {
		return reminderNotificationMessageTitle;
	}

	public void setReminderNotificationMessageTitle(@Nullable String reminderNotificationMessageTitle) {
		this.reminderNotificationMessageTitle = reminderNotificationMessageTitle;
	}

	@Nullable
	public String getReminderNotificationMessageBody() {
		return reminderNotificationMessageBody;
	}

	public void setReminderNotificationMessageBody(@Nullable String reminderNotificationMessageBody) {
		this.reminderNotificationMessageBody = reminderNotificationMessageBody;
	}

	@Nullable
	public Boolean getResetAfterFinalCheckIn() {
		return resetAfterFinalCheckIn;
	}

	public void setResetAfterFinalCheckIn(@Nullable Boolean resetAfterFinalCheckIn) {
		this.resetAfterFinalCheckIn = resetAfterFinalCheckIn;
	}

	@Nullable
	public Boolean getLeaveFirstCheckInOpenUntilStarted() {
		return leaveFirstCheckInOpenUntilStarted;
	}

	public void setLeaveFirstCheckInOpenUntilStarted(@Nullable Boolean leaveFirstCheckInOpenUntilStarted) {
		this.leaveFirstCheckInOpenUntilStarted = leaveFirstCheckInOpenUntilStarted;
	}

	@Nullable
	public Boolean getCheckInWindowsFixed() {
		return checkInWindowsFixed;
	}

	public void setCheckInWindowsFixed(@Nullable Boolean checkInWindowsFixed) {
		this.checkInWindowsFixed = checkInWindowsFixed;
	}
}