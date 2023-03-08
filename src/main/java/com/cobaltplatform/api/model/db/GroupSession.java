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

import com.cobaltplatform.api.model.db.GroupSessionSchedulingSystem.GroupSessionSchedulingSystemId;
import com.cobaltplatform.api.model.db.GroupSessionStatus.GroupSessionStatusId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class GroupSession {
	@Nullable
	private UUID groupSessionId;
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private GroupSessionStatusId groupSessionStatusId;
	@Nullable
	private GroupSessionSchedulingSystemId groupSessionSchedulingSystemId;
	@Nullable
	private UUID assessmentId;
	@Nullable
	private UUID submitterAccountId;
	@Nullable
	private String submitterName;
	@Nullable
	private String submitterEmailAddress;
	@Nullable
	private String targetEmailAddress;
	@Nullable
	private String title;
	@Nullable
	private String description;
	@Nullable
	private String urlName;
	@Nullable
	private UUID facilitatorAccountId;
	@Nullable
	private String facilitatorName;
	@Nullable
	private String facilitatorEmailAddress;
	@Nullable
	private LocalDateTime startDateTime;
	@Nullable
	private LocalDateTime endDateTime;
	@Nullable
	private Integer seats;
	@Nullable
	private Integer seatsAvailable; // Joined in via DB view
	@Nullable
	private Integer seatsReserved; // Joined in via DB view
	@Nullable
	private ZoneId timeZone;
	@Nullable
	private String imageUrl;
	@Nullable
	private String videoconferenceUrl;
	@Nullable
	private String screeningQuestion;
	@Nullable
	private String confirmationEmailContent;
	@Nullable
	private String scheduleUrl;
	@Nullable
	private Boolean sendFollowupEmail;
	@Nullable
	private String followupEmailContent;
	@Nullable
	private String followupEmailSurveyUrl;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getGroupSessionId() {
		return groupSessionId;
	}

	public void setGroupSessionId(@Nullable UUID groupSessionId) {
		this.groupSessionId = groupSessionId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public GroupSessionStatusId getGroupSessionStatusId() {
		return groupSessionStatusId;
	}

	public void setGroupSessionStatusId(@Nullable GroupSessionStatusId groupSessionStatusId) {
		this.groupSessionStatusId = groupSessionStatusId;
	}

	@Nullable
	public GroupSessionSchedulingSystemId getGroupSessionSchedulingSystemId() {
		return groupSessionSchedulingSystemId;
	}

	public void setGroupSessionSchedulingSystemId(@Nullable GroupSessionSchedulingSystemId groupSessionSchedulingSystemId) {
		this.groupSessionSchedulingSystemId = groupSessionSchedulingSystemId;
	}

	@Nullable
	public String getTitle() {
		return title;
	}

	public void setTitle(@Nullable String title) {
		this.title = title;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public String getUrlName() {
		return urlName;
	}

	public void setUrlName(@Nullable String urlName) {
		this.urlName = urlName;
	}

	@Nullable
	public UUID getFacilitatorAccountId() {
		return facilitatorAccountId;
	}

	public void setFacilitatorAccountId(@Nullable UUID facilitatorAccountId) {
		this.facilitatorAccountId = facilitatorAccountId;
	}

	@Nullable
	public String getSubmitterName() {
		return this.submitterName;
	}

	public void setSubmitterName(@Nullable String submitterName) {
		this.submitterName = submitterName;
	}

	@Nullable
	public String getSubmitterEmailAddress() {
		return this.submitterEmailAddress;
	}

	public void setSubmitterEmailAddress(@Nullable String submitterEmailAddress) {
		this.submitterEmailAddress = submitterEmailAddress;
	}

	@Nullable
	public String getTargetEmailAddress() {
		return this.targetEmailAddress;
	}

	public void setTargetEmailAddress(@Nullable String targetEmailAddress) {
		this.targetEmailAddress = targetEmailAddress;
	}

	@Nullable
	public String getFacilitatorName() {
		return facilitatorName;
	}

	public void setFacilitatorName(@Nullable String facilitatorName) {
		this.facilitatorName = facilitatorName;
	}

	@Nullable
	public String getFacilitatorEmailAddress() {
		return facilitatorEmailAddress;
	}

	public void setFacilitatorEmailAddress(@Nullable String facilitatorEmailAddress) {
		this.facilitatorEmailAddress = facilitatorEmailAddress;
	}

	@Nullable
	public UUID getSubmitterAccountId() {
		return submitterAccountId;
	}

	public void setSubmitterAccountId(@Nullable UUID submitterAccountId) {
		this.submitterAccountId = submitterAccountId;
	}

	@Nullable
	public LocalDateTime getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(@Nullable LocalDateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

	@Nullable
	public LocalDateTime getEndDateTime() {
		return endDateTime;
	}

	public void setEndDateTime(@Nullable LocalDateTime endDateTime) {
		this.endDateTime = endDateTime;
	}

	@Nullable
	public Integer getSeats() {
		return seats;
	}

	public void setSeats(@Nullable Integer seats) {
		this.seats = seats;
	}

	@Nullable
	public Integer getSeatsAvailable() {
		return seatsAvailable;
	}

	public void setSeatsAvailable(@Nullable Integer seatsAvailable) {
		this.seatsAvailable = seatsAvailable;
	}

	@Nullable
	public Integer getSeatsReserved() {
		return seatsReserved;
	}

	public void setSeatsReserved(@Nullable Integer seatsReserved) {
		this.seatsReserved = seatsReserved;
	}

	@Nullable
	public ZoneId getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(@Nullable ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	@Nullable
	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(@Nullable String imageUrl) {
		this.imageUrl = imageUrl;
	}

	@Nullable
	public String getVideoconferenceUrl() {
		return videoconferenceUrl;
	}

	public void setVideoconferenceUrl(@Nullable String videoconferenceUrl) {
		this.videoconferenceUrl = videoconferenceUrl;
	}

	@Nullable
	public UUID getAssessmentId() {
		return assessmentId;
	}

	public void setAssessmentId(@Nullable UUID assessmentId) {
		this.assessmentId = assessmentId;
	}

	@Nullable
	public String getConfirmationEmailContent() {
		return confirmationEmailContent;
	}

	public void setConfirmationEmailContent(@Nullable String confirmationEmailContent) {
		this.confirmationEmailContent = confirmationEmailContent;
	}

	@Nullable
	public String getScheduleUrl() {
		return scheduleUrl;
	}

	public void setScheduleUrl(@Nullable String scheduleUrl) {
		this.scheduleUrl = scheduleUrl;
	}

	@Nullable
	public String getScreeningQuestion() {
		return screeningQuestion;
	}

	public void setScreeningQuestion(@Nullable String screeningQuestion) {
		this.screeningQuestion = screeningQuestion;
	}

	@Nullable
	public Boolean getSendFollowupEmail() {
		return sendFollowupEmail;
	}

	public void setSendFollowupEmail(@Nullable Boolean sendFollowupEmail) {
		this.sendFollowupEmail = sendFollowupEmail;
	}

	@Nullable
	public String getFollowupEmailContent() {
		return followupEmailContent;
	}

	public void setFollowupEmailContent(@Nullable String followupEmailContent) {
		this.followupEmailContent = followupEmailContent;
	}

	@Nullable
	public String getFollowupEmailSurveyUrl() {
		return followupEmailSurveyUrl;
	}

	public void setFollowupEmailSurveyUrl(@Nullable String followupEmailSurveyUrl) {
		this.followupEmailSurveyUrl = followupEmailSurveyUrl;
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
}