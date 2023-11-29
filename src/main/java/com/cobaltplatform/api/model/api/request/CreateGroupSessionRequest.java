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

import com.cobaltplatform.api.model.db.GroupSessionLearnMoreMethod;
import com.cobaltplatform.api.model.db.GroupSessionLocationType.GroupSessionLocationTypeId;
import com.cobaltplatform.api.model.db.GroupSessionSchedulingSystem.GroupSessionSchedulingSystemId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreateGroupSessionRequest {
	@Nullable
	private InstitutionId institutionId;
	@Nullable
	private GroupSessionSchedulingSystemId groupSessionSchedulingSystemId;
	@Nullable
	private GroupSessionLocationTypeId groupSessionLocationTypeId;
	@Nullable
	private UUID submitterAccountId;
	@Nullable
	private String targetEmailAddress;
	@Nullable
	private String title;
	@Nullable
	private String description;
	@Nullable
	private String urlName;
	@Nullable
	private String inPersonLocation;
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
	private String imageUrl;
	@Nullable
	private String videoconferenceUrl;
	@Nullable
	private String scheduleUrl;
	@Nullable
	@Deprecated
	private List<String> screeningQuestions;
	@Nullable
	private List<CreateScreeningQuestionRequest> screeningQuestionsV2;
	@Nullable
	private String confirmationEmailContent;
	@Nullable
	private Boolean sendFollowupEmail;
	@Nullable
	private String followupEmailContent;
	@Nullable
	private String followupEmailSurveyUrl;
	@Nullable
	private UUID groupSessionCollectionId;
	@Nullable
	private Boolean visibleFlag;
	@Nullable
	private UUID screeningFlowId;
	@Nullable
	private Boolean sendReminderEmail;
	@Nullable
	private String reminderEmailContent;
	@Nullable
	private LocalTime followupTimeOfDay;
	@Nullable
	private Integer followupDayOffset;
	@Nullable
	private Boolean singleSessionFlag;
	@Nullable
	private String dateTimeDescription;
	@Nullable
	private Set<String> tagIds;
	@Nullable
	private String learnMoreDescription;
	@Nullable
	private GroupSessionLearnMoreMethod.GroupSessionLearnMoreMethodId groupSessionLearnMoreMethodId;
	@Nullable
	private Boolean differentEmailAddressForNotifications;
	@Nullable
	private LocalDateTime registrationEndDateTime;

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	public void setInstitutionId(@Nullable InstitutionId institutionId) {
		this.institutionId = institutionId;
	}

	@Nullable
	public GroupSessionSchedulingSystemId getGroupSessionSchedulingSystemId() {
		return groupSessionSchedulingSystemId;
	}

	public void setGroupSessionSchedulingSystemId(@Nullable GroupSessionSchedulingSystemId groupSessionSchedulingSystemId) {
		this.groupSessionSchedulingSystemId = groupSessionSchedulingSystemId;
	}

	@Nullable
	public GroupSessionLocationTypeId getGroupSessionLocationTypeId() {
		return this.groupSessionLocationTypeId;
	}

	public void setGroupSessionLocationTypeId(@Nullable GroupSessionLocationTypeId groupSessionLocationTypeId) {
		this.groupSessionLocationTypeId = groupSessionLocationTypeId;
	}

	@Nullable
	public UUID getSubmitterAccountId() {
		return submitterAccountId;
	}

	public void setSubmitterAccountId(@Nullable UUID submitterAccountId) {
		this.submitterAccountId = submitterAccountId;
	}

	@Nullable
	public String getTargetEmailAddress() {
		return this.targetEmailAddress;
	}

	public void setTargetEmailAddress(@Nullable String targetEmailAddress) {
		this.targetEmailAddress = targetEmailAddress;
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
	public String getInPersonLocation() {
		return this.inPersonLocation;
	}

	public void setInPersonLocation(@Nullable String inPersonLocation) {
		this.inPersonLocation = inPersonLocation;
	}

	@Nullable
	public UUID getFacilitatorAccountId() {
		return facilitatorAccountId;
	}

	public void setFacilitatorAccountId(@Nullable UUID facilitatorAccountId) {
		this.facilitatorAccountId = facilitatorAccountId;
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
	public String getScheduleUrl() {
		return scheduleUrl;
	}

	public void setScheduleUrl(@Nullable String scheduleUrl) {
		this.scheduleUrl = scheduleUrl;
	}

	@Nullable
	public List<String> getScreeningQuestions() {
		return screeningQuestions;
	}

	public void setScreeningQuestions(@Nullable List<String> screeningQuestions) {
		this.screeningQuestions = screeningQuestions;
	}

	@Nullable
	public List<CreateScreeningQuestionRequest> getScreeningQuestionsV2() {
		return screeningQuestionsV2;
	}

	public void setScreeningQuestionsV2(@Nullable List<CreateScreeningQuestionRequest> screeningQuestionsV2) {
		this.screeningQuestionsV2 = screeningQuestionsV2;
	}

	@Nullable
	public String getConfirmationEmailContent() {
		return confirmationEmailContent;
	}

	public void setConfirmationEmailContent(@Nullable String confirmationEmailContent) {
		this.confirmationEmailContent = confirmationEmailContent;
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
	public UUID getGroupSessionCollectionId() {
		return groupSessionCollectionId;
	}

	public void setGroupSessionCollectionId(@Nullable UUID groupSessionCollectionId) {
		this.groupSessionCollectionId = groupSessionCollectionId;
	}

	@Nullable
	public Boolean getVisibleFlag() {
		return visibleFlag;
	}

	public void setVisibleFlag(@Nullable Boolean visibleFlag) {
		this.visibleFlag = visibleFlag;
	}

	@Nullable
	public UUID getScreeningFlowId() {
		return screeningFlowId;
	}

	public void setScreeningFlowId(@Nullable UUID screeningFlowId) {
		this.screeningFlowId = screeningFlowId;
	}

	@Nullable
	public Boolean getSendReminderEmail() {
		return sendReminderEmail;
	}

	public void setSendReminderEmail(@Nullable Boolean sendReminderEmail) {
		this.sendReminderEmail = sendReminderEmail;
	}

	@Nullable
	public String getReminderEmailContent() {
		return reminderEmailContent;
	}

	public void setReminderEmailContent(@Nullable String reminderEmailContent) {
		this.reminderEmailContent = reminderEmailContent;
	}

	@Nullable
	public LocalTime getFollowupTimeOfDay() {
		return followupTimeOfDay;
	}

	public void setFollowupTimeOfDay(@Nullable LocalTime followupTimeOfDay) {
		this.followupTimeOfDay = followupTimeOfDay;
	}

	@Nullable
	public Integer getFollowupDayOffset() {
		return followupDayOffset;
	}

	public void setFollowupDayOffset(@Nullable Integer followupDayOffset) {
		this.followupDayOffset = followupDayOffset;
	}

	@Nullable
	public Boolean getSingleSessionFlag() {
		return singleSessionFlag;
	}

	public void setSingleSessionFlag(@Nullable Boolean singleSessionFlag) {
		this.singleSessionFlag = singleSessionFlag;
	}

	@Nullable
	public String getDateTimeDescription() {
		return dateTimeDescription;
	}

	public void setDateTimeDescription(@Nullable String dateTimeDescription) {
		this.dateTimeDescription = dateTimeDescription;
	}

	@Nullable
	public Set<String> getTagIds() {
		return tagIds;
	}

	public void setTagIds(@Nullable Set<String> tagIds) {
		this.tagIds = tagIds;
	}

	@Nullable
	public String getLearnMoreDescription() {
		return learnMoreDescription;
	}

	public void setLearnMoreDescription(@Nullable String learnMoreDescription) {
		this.learnMoreDescription = learnMoreDescription;
	}

	@Nullable
	public GroupSessionLearnMoreMethod.GroupSessionLearnMoreMethodId getGroupSessionLearnMoreMethodId() {
		return groupSessionLearnMoreMethodId;
	}

	public void setGroupSessionLearnMoreMethodId(@Nullable GroupSessionLearnMoreMethod.GroupSessionLearnMoreMethodId groupSessionLearnMoreMethodId) {
		this.groupSessionLearnMoreMethodId = groupSessionLearnMoreMethodId;
	}

	@Nullable
	public Boolean getDifferentEmailAddressForNotifications() {
		return differentEmailAddressForNotifications;
	}

	public void setDifferentEmailAddressForNotifications(@Nullable Boolean differentEmailAddressForNotifications) {
		this.differentEmailAddressForNotifications = differentEmailAddressForNotifications;
	}

	@Nullable
	public LocalDateTime getRegistrationEndDateTime() {
		return this.registrationEndDateTime;
	}

	public void setRegistrationEndDateTime(@Nullable LocalDateTime registrationEndDateTime) {
		this.registrationEndDateTime = registrationEndDateTime;
	}
}
