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

import com.cobaltplatform.api.model.db.CheckInActionStatus.CheckInActionStatusId;
import com.cobaltplatform.api.model.db.CheckInType.CheckInTypeId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;


/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AccountCheckInAction {
	@Nullable
	private UUID accountCheckInActionId;
	@Nullable
	private UUID accountCheckInId;
	@Nullable
	private UUID studyCheckInActionId;
	@Nullable
	private CheckInActionStatusId checkInActionStatusId;
	@Nullable
	private String checkInActionStatusDescription;

	@Nullable
	private CheckInTypeId checkInTypeId;

	@Nullable
	String checkInTypeDescription;

	@Nullable
	private UUID screeningSessionId;

	@Nullable
	private UUID screeningFlowId;

	@Nullable
	private String videoPrompt;

	@Nullable
	private String videoScript;

	@Nullable
	private String videoIntro;
	@Nullable
	private Integer minVideoTimeSeconds;
	@Nullable
	private Integer maxVideoTimeSeconds;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;
	@Nullable
	private Boolean sendFollowupNotification;
	@Nullable
	private Integer followupNotificationMinutes;
	@Nullable
	private String followupNotificationMessageTitle;
	@Nullable
	private String followupNotificationMessageBody;


	@Nullable
	public UUID getAccountCheckInActionId() {
		return accountCheckInActionId;
	}

	public void setAccountCheckInActionId(@Nullable UUID accountCheckInActionId) {
		this.accountCheckInActionId = accountCheckInActionId;
	}

	@Nullable
	public UUID getStudyCheckInActionId() {
		return studyCheckInActionId;
	}

	public void setStudyCheckInActionId(@Nullable UUID studyCheckInActionId) {
		this.studyCheckInActionId = studyCheckInActionId;
	}

	@Nullable
	public CheckInActionStatusId getCheckInActionStatusId() {
		return checkInActionStatusId;
	}

	public void setCheckInActionStatusId(@Nullable CheckInActionStatusId checkInActionStatusId) {
		this.checkInActionStatusId = checkInActionStatusId;
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
	public UUID getAccountCheckInId() {
		return accountCheckInId;
	}

	public void setAccountCheckInId(@Nullable UUID accountCheckInId) {
		this.accountCheckInId = accountCheckInId;
	}

	@Nullable
	public String getCheckInActionStatusDescription() {
		return checkInActionStatusDescription;
	}

	public void setCheckInActionStatusDescription(@Nullable String checkInActionStatusDescription) {
		this.checkInActionStatusDescription = checkInActionStatusDescription;
	}

	@Nullable
	public CheckInTypeId getCheckInTypeId() {
		return checkInTypeId;
	}

	public void setCheckInTypeId(@Nullable CheckInTypeId checkInTypeId) {
		this.checkInTypeId = checkInTypeId;
	}

	@Nullable
	public String getCheckInTypeDescription() {
		return checkInTypeDescription;
	}

	public void setCheckInTypeDescription(@Nullable String checkInTypeDescription) {
		this.checkInTypeDescription = checkInTypeDescription;
	}

	@Nullable
	public UUID getScreeningSessionId() {
		return screeningSessionId;
	}

	public void setScreeningSessionId(@Nullable UUID screeningSessionId) {
		this.screeningSessionId = screeningSessionId;
	}

	@Nullable
	public UUID getScreeningFlowId() {
		return screeningFlowId;
	}

	public void setScreeningFlowId(@Nullable UUID screeningFlowId) {
		this.screeningFlowId = screeningFlowId;
	}

	@Nullable
	public String getVideoPrompt() {
		return videoPrompt;
	}

	public void setVideoPrompt(@Nullable String videoPrompt) {
		this.videoPrompt = videoPrompt;
	}

	@Nullable
	public String getVideoScript() {
		return videoScript;
	}

	public void setVideoScript(@Nullable String videoScript) {
		this.videoScript = videoScript;
	}

	@Nullable
	public String getVideoIntro() {
		return videoIntro;
	}

	public void setVideoIntro(@Nullable String videoIntro) {
		this.videoIntro = videoIntro;
	}

	@Nullable
	public Integer getMinVideoTimeSeconds() {
		return minVideoTimeSeconds;
	}

	public void setMinVideoTimeSeconds(@Nullable Integer minVideoTimeSeconds) {
		this.minVideoTimeSeconds = minVideoTimeSeconds;
	}

	@Nullable
	public Integer getMaxVideoTimeSeconds() {
		return maxVideoTimeSeconds;
	}

	public void setMaxVideoTimeSeconds(@Nullable Integer maxVideoTimeSeconds) {
		this.maxVideoTimeSeconds = maxVideoTimeSeconds;
	}

	@Nullable
	public Boolean getSendFollowupNotification() {
		return sendFollowupNotification;
	}

	public void setSendFollowupNotification(@Nullable Boolean sendFollowupNotification) {
		this.sendFollowupNotification = sendFollowupNotification;
	}

	@Nullable
	public Integer getFollowupNotificationMinutes() {
		return followupNotificationMinutes;
	}

	public void setFollowupNotificationMinutes(@Nullable Integer followupNotificationMinutes) {
		this.followupNotificationMinutes = followupNotificationMinutes;
	}

	@Nullable
	public String getFollowupNotificationMessageTitle() {
		return followupNotificationMessageTitle;
	}

	public void setFollowupNotificationMessageTitle(@Nullable String followupNotificationMessageTitle) {
		this.followupNotificationMessageTitle = followupNotificationMessageTitle;
	}

	@Nullable
	public String getFollowupNotificationMessageBody() {
		return followupNotificationMessageBody;
	}

	public void setFollowupNotificationMessageBody(@Nullable String followupNotificationMessageBody) {
		this.followupNotificationMessageBody = followupNotificationMessageBody;
	}

}