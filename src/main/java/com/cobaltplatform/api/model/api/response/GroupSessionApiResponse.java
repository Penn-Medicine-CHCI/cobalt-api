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

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.response.QuestionApiResponse.QuestionApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.GroupSession;
import com.cobaltplatform.api.model.db.GroupSessionLearnMoreMethod.GroupSessionLearnMoreMethodId;
import com.cobaltplatform.api.model.db.GroupSessionLocationType.GroupSessionLocationTypeId;
import com.cobaltplatform.api.model.db.GroupSessionSchedulingSystem.GroupSessionSchedulingSystemId;
import com.cobaltplatform.api.model.db.GroupSessionStatus.GroupSessionStatusId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.service.GroupSessionService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.util.AppointmentTimeFormatter;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class GroupSessionApiResponse {
	@Nonnull
	private final UUID groupSessionId;
	@Nonnull
	private final InstitutionId institutionId;
	@Nonnull
	private final String institutionDescription;
	@Nonnull
	private final GroupSessionStatusId groupSessionStatusId;
	@Nonnull
	private final String groupSessionStatusIdDescription;
	@Nonnull
	private final GroupSessionSchedulingSystemId groupSessionSchedulingSystemId;
	@Nonnull
	private final GroupSessionLocationTypeId groupSessionLocationTypeId;
	@Nullable
	private final UUID assessmentId;
	@Nonnull
	private final UUID submitterAccountId;
	@Nonnull
	private final String targetEmailAddress;
	@Nullable
	private final String title;
	@Nullable
	private final String description;
	@Nullable
	private final String urlName;
	@Nullable
	private final String inPersonLocation;
	@Nullable
	private final UUID facilitatorAccountId;
	@Nullable
	private final String facilitatorName;
	@Nullable
	private final String facilitatorEmailAddress;
	@Nonnull
	private final String appointmentTimeDescription;
	@Nullable
	private final LocalTime startTime;
	@Nullable
	private final String startTimeDescription;
	@Nullable
	private final LocalTime endTime;
	@Nullable
	private final String endTimeDescription;
	@Nullable
	private final LocalDate startDate;
	@Nullable
	private final String startDateDescription;
	@Nullable
	private final LocalDate endDate;
	@Nullable
	private final String endDateDescription;
	@Nullable
	private final LocalDateTime startDateTime;
	@Nullable
	private final String startDateTimeDescription;
	@Nullable
	private final LocalDateTime endDateTime;
	@Nullable
	private final String endDateTimeDescription;
	@Nullable
	private final Integer durationInMinutes;
	@Nullable
	private final String durationInMinutesDescription;
	@Nullable
	private final Integer seats;
	@Nullable
	private final String seatsDescription;
	@Nullable
	private final Integer seatsAvailable;
	@Nullable
	private final String seatsAvailableDescription;
	@Nullable
	private final Integer seatsReserved;
	@Nullable
	private final String seatsReservedDescription;
	@Nullable
	private final ZoneId timeZone;
	@Nullable
	private final String imageUrl;
	@Nullable
	private final String videoconferenceUrl;
	@Nullable
	private final String scheduleUrl;
	@Nullable
	private final String confirmationEmailContent;
	@Nonnull
	private final Boolean sendFollowupEmail;
	@Nullable
	private final String followupEmailContent;
	@Nullable
	private final String followupEmailSurveyUrl;
	@Nullable
	private final UUID groupSessionCollectionId;
	@Nullable
	private final Boolean visibleFlag;
	@Nullable
	private final UUID screeningFlowId;
	@Nullable
	private final Boolean sendReminderEmail;
	@Nullable
	private final String reminderEmailContent;
	@Nullable
	private final LocalTime followupTimeOfDay;
	@Nullable
	private final Integer followupDayOffset;
	@Nullable
	private final Boolean singleSessionFlag;
	@Nullable
	private final String dateTimeDescription;
	@Nonnull
	private final List<TagApiResponse> tags;
	@Nullable
	private String learnMoreDescription;
	@Nullable
	private GroupSessionLearnMoreMethodId groupSessionLearnMoreMethodId;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nonnull
	private final String createdDateDescription;
	@Nullable
	private final Instant lastUpdated;
	@Nullable
	private final String lastUpdatedDescription;
	@Nullable
	private final Boolean differentEmailAddressForNotifications;
	@Nullable
	private final String groupSessionCollectionUrlName;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface GroupSessionApiResponseFactory {
		@Nonnull
		GroupSessionApiResponse create(@Nonnull GroupSession groupSession);
	}

	@AssistedInject
	public GroupSessionApiResponse(@Nonnull Formatter formatter,
																 @Nonnull Strings strings,
																 @Nonnull InstitutionService institutionService,
																 @Nonnull GroupSessionService groupSessionService,
																 @Nonnull QuestionApiResponseFactory questionApiResponseFactory,
																 @Nonnull TagApiResponse.TagApiResponseFactory tagApiResponseFactory,
																 @Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
																 @Assisted @Nonnull GroupSession groupSession) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(institutionService);
		requireNonNull(groupSessionService);
		requireNonNull(questionApiResponseFactory);
		requireNonNull(currentContextProvider);
		requireNonNull(groupSession);
		requireNonNull(tagApiResponseFactory);

		CurrentContext currentContext = currentContextProvider.get();
		Account account = currentContext.getAccount().get();

		Boolean hasStartEndTime = groupSession.getStartDateTime() != null && groupSession.getEndDateTime() != null;

		this.groupSessionId = groupSession.getGroupSessionId();
		this.institutionId = groupSession.getInstitutionId();
		this.institutionDescription = institutionService.findInstitutionById(institutionId).get().getName();
		this.groupSessionStatusId = groupSession.getGroupSessionStatusId();
		this.groupSessionStatusIdDescription = groupSessionService.findGroupSessionStatusById(groupSession.getGroupSessionStatusId()).get().getDescription();
		this.groupSessionSchedulingSystemId = groupSession.getGroupSessionSchedulingSystemId();
		this.groupSessionLocationTypeId = groupSession.getGroupSessionLocationTypeId();
		this.assessmentId = groupSession.getAssessmentId();
		this.submitterAccountId = groupSession.getSubmitterAccountId();

		// Can only see submitter info if you're an admin or you're the submitter/facilitator
		if (account.getRoleId() == RoleId.ADMINISTRATOR
				|| Objects.equals(account.getAccountId(), groupSession.getSubmitterAccountId())
				|| Objects.equals(account.getAccountId(), groupSession.getFacilitatorAccountId())) {
			if (groupSession.getDifferentEmailAddressForNotifications())
				this.targetEmailAddress = groupSession.getTargetEmailAddress();
			else
				this.targetEmailAddress = null;
		} else {
			this.targetEmailAddress = null;
		}

		this.title = groupSession.getTitle();
		this.description = groupSession.getDescription();
		this.urlName = groupSession.getUrlName();
		this.inPersonLocation = groupSession.getInPersonLocation();
		this.facilitatorAccountId = groupSession.getFacilitatorAccountId();
		this.facilitatorName = groupSession.getFacilitatorName();
		this.facilitatorEmailAddress = groupSession.getFacilitatorEmailAddress();
		this.startDateTime = groupSession.getStartDateTime();

		if (hasStartEndTime)
			this.startDateTimeDescription = groupSession.getSingleSessionFlag() ?
					formatter.formatDateTime(groupSession.getStartDateTime(), FormatStyle.LONG, FormatStyle.SHORT) :
					formatter.formatDate(groupSession.getStartDateTime().toLocalDate(), FormatStyle.LONG);
		else
			this.startDateTimeDescription = null;

		this.endDateTime = groupSession.getEndDateTime();

		if (hasStartEndTime)
			this.endDateTimeDescription = groupSession.getSingleSessionFlag() ?
					formatter.formatDateTime(groupSession.getEndDateTime(), FormatStyle.LONG, FormatStyle.SHORT) :
					formatter.formatDate(groupSession.getEndDateTime().toLocalDate(), FormatStyle.LONG);
		else
			this.endDateTimeDescription = null;

		if (this.startDateTime != null) {
			this.startDate = this.startDateTime.toLocalDate();
			this.startDateDescription = formatter.formatDate(this.startDate, FormatStyle.LONG);
			this.startTime = this.startDateTime.toLocalTime();
			this.startTimeDescription = formatter.formatTime(this.startTime, FormatStyle.SHORT);
		} else {
			this.startDate = null;
			this.startDateDescription = null;
			this.startTime = null;
			this.startTimeDescription = null;
		}

		if (this.endDateTime != null) {
			this.endDate = this.endDateTime.toLocalDate();
			this.endDateDescription = formatter.formatDate(this.endDate, FormatStyle.LONG);
			this.endTime = this.endDateTime.toLocalTime();
			this.endTimeDescription = formatter.formatTime(this.endTime, FormatStyle.SHORT);
		} else {
			this.endDate = null;
			this.endDateDescription = null;
			this.endTime = null;
			this.endTimeDescription = null;
		}

		String appointmentTimeDescription;

		if (groupSession.getSingleSessionFlag()) {
			// e.g. Thu Nov 16 @ 12:00am-12:00am
			appointmentTimeDescription = hasStartEndTime ? AppointmentTimeFormatter.createTimeDescription(groupSession.getStartDateTime(), groupSession.getEndDateTime(), groupSession.getTimeZone()) : null;
		} else {
			// Recurring sessions
			if (hasStartEndTime) {
				// "dateTimeDescription" is whatever the user specifies in free-form input
				if (groupSession.getDateTimeDescription() != null)
					appointmentTimeDescription = String.format("%s (%s)", AppointmentTimeFormatter.createDateDescription(this.startDate, this.endDate), groupSession.getDateTimeDescription());
				else
					appointmentTimeDescription = AppointmentTimeFormatter.createDateDescription(this.startDate, this.endDate);
			} else {
				appointmentTimeDescription = groupSession.getDateTimeDescription();
			}
		}

		this.appointmentTimeDescription = appointmentTimeDescription;

		this.durationInMinutes = hasStartEndTime ? (int) Duration.between(startDateTime, endDateTime).toMinutes() : 0;
		this.durationInMinutesDescription = hasStartEndTime ? strings.get("{{duration}} minutes", new HashMap<String, Object>() {
			{
				put("duration", durationInMinutes);
			}
		}) : null;

		this.seats = groupSession.getSeats();

		if (this.seats != null) {
			this.seatsDescription = strings.get("{{seatsDescription}} seats total", new HashMap<String, Object>() {{
				put("seats", groupSession.getSeats());
				put("seatsDescription", formatter.formatNumber(groupSession.getSeats()));
			}});
			this.seatsAvailable = groupSession.getSeatsAvailable();
			this.seatsAvailableDescription = strings.get("{{seatsAvailableDescription}} seats left", new HashMap<String, Object>() {{
				put("seatsAvailable", groupSession.getSeatsAvailable());
				put("seatsAvailableDescription", formatter.formatNumber(groupSession.getSeatsAvailable()));
			}});
		} else {
			this.seatsDescription = null;
			this.seatsAvailable = null;
			this.seatsAvailableDescription = null;
		}

		this.seatsReserved = groupSession.getGroupSessionSchedulingSystemId() != GroupSessionSchedulingSystemId.COBALT ? null : groupSession.getSeatsReserved();
		this.seatsReservedDescription = groupSession.getGroupSessionSchedulingSystemId() != GroupSessionSchedulingSystemId.COBALT ? strings.get("N/A") : strings.get("{{seatsReservedDescription}} reservations", new HashMap<String, Object>() {{
			put("seatsReserved", groupSession.getSeatsReserved());
			put("seatsReservedDescription", formatter.formatNumber(groupSession.getSeatsReserved()));
		}});

		this.timeZone = groupSession.getTimeZone();
		this.imageUrl = groupSession.getImageUrl();
		this.videoconferenceUrl = groupSession.getVideoconferenceUrl();
		this.scheduleUrl = groupSession.getScheduleUrl();
		this.confirmationEmailContent = groupSession.getConfirmationEmailContent();
		this.sendFollowupEmail = groupSession.getSendFollowupEmail();
		this.followupEmailContent = groupSession.getFollowupEmailContent();
		this.followupEmailSurveyUrl = groupSession.getFollowupEmailSurveyUrl();
		this.groupSessionCollectionId = groupSession.getGroupSessionCollectionId();
		this.visibleFlag = groupSession.getVisibleFlag();
		this.screeningFlowId = groupSession.getScreeningFlowId();
		this.sendReminderEmail = groupSession.getSendReminderEmail();
		this.reminderEmailContent = groupSession.getReminderEmailContent();
		this.followupTimeOfDay = groupSession.getFollowupTimeOfDay();
		this.followupDayOffset = groupSession.getFollowupDayOffset();
		this.singleSessionFlag = groupSession.getSingleSessionFlag();
		this.dateTimeDescription = groupSession.getDateTimeDescription();
		this.tags = groupSession.getTags() == null ? Collections.emptyList() : groupSession.getTags().stream()
				.map(tag -> tagApiResponseFactory.create(tag)).collect(Collectors.toList());
		this.groupSessionLearnMoreMethodId = groupSession.getGroupSessionLearnMoreMethodId();
		this.learnMoreDescription = groupSession.getLearnMoreDescription();
		this.created = groupSession.getCreated();
		this.createdDescription = formatter.formatTimestamp(groupSession.getCreated());

		LocalDate createdDate = LocalDate.ofInstant(groupSession.getCreated(), currentContextProvider.get().getTimeZone());
		this.createdDateDescription = formatter.formatDate(createdDate, FormatStyle.SHORT);

		this.lastUpdated = groupSession.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(groupSession.getLastUpdated());

		this.differentEmailAddressForNotifications = groupSession.getDifferentEmailAddressForNotifications();
		this.groupSessionCollectionUrlName = groupSession.getGroupSessionCollectionUrlName();
	}

	@Nonnull
	public UUID getGroupSessionId() {
		return this.groupSessionId;
	}

	@Nonnull
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	@Nonnull
	public String getInstitutionDescription() {
		return this.institutionDescription;
	}

	@Nonnull
	public GroupSessionStatusId getGroupSessionStatusId() {
		return this.groupSessionStatusId;
	}

	@Nonnull
	public String getGroupSessionStatusIdDescription() {
		return this.groupSessionStatusIdDescription;
	}

	@Nonnull
	public GroupSessionSchedulingSystemId getGroupSessionSchedulingSystemId() {
		return this.groupSessionSchedulingSystemId;
	}

	@Nonnull
	public GroupSessionLocationTypeId getGroupSessionLocationTypeId() {
		return this.groupSessionLocationTypeId;
	}

	@Nullable
	public UUID getAssessmentId() {
		return this.assessmentId;
	}

	@Nonnull
	public UUID getSubmitterAccountId() {
		return this.submitterAccountId;
	}

	@Nonnull
	public String getTargetEmailAddress() {
		return this.targetEmailAddress;
	}

	@Nullable
	public String getTitle() {
		return this.title;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	@Nullable
	public String getUrlName() {
		return this.urlName;
	}

	@Nullable
	public String getInPersonLocation() {
		return this.inPersonLocation;
	}

	@Nullable
	public UUID getFacilitatorAccountId() {
		return this.facilitatorAccountId;
	}

	@Nullable
	public String getFacilitatorName() {
		return this.facilitatorName;
	}

	@Nullable
	public String getFacilitatorEmailAddress() {
		return this.facilitatorEmailAddress;
	}

	@Nonnull
	public String getAppointmentTimeDescription() {
		return this.appointmentTimeDescription;
	}

	@Nullable
	public LocalTime getStartTime() {
		return this.startTime;
	}

	@Nullable
	public String getStartTimeDescription() {
		return this.startTimeDescription;
	}

	@Nullable
	public LocalTime getEndTime() {
		return this.endTime;
	}

	@Nullable
	public String getEndTimeDescription() {
		return this.endTimeDescription;
	}

	@Nullable
	public LocalDateTime getStartDateTime() {
		return this.startDateTime;
	}

	@Nullable
	public String getStartDateTimeDescription() {
		return this.startDateTimeDescription;
	}

	@Nullable
	public LocalDateTime getEndDateTime() {
		return this.endDateTime;
	}

	@Nullable
	public String getEndDateTimeDescription() {
		return this.endDateTimeDescription;
	}

	@Nullable
	public Integer getDurationInMinutes() {
		return this.durationInMinutes;
	}

	@Nullable
	public String getDurationInMinutesDescription() {
		return this.durationInMinutesDescription;
	}

	@Nullable
	public Integer getSeats() {
		return this.seats;
	}

	@Nullable
	public String getSeatsDescription() {
		return this.seatsDescription;
	}

	@Nullable
	public Integer getSeatsAvailable() {
		return this.seatsAvailable;
	}

	@Nullable
	public String getSeatsAvailableDescription() {
		return this.seatsAvailableDescription;
	}

	@Nullable
	public Integer getSeatsReserved() {
		return this.seatsReserved;
	}

	@Nullable
	public String getSeatsReservedDescription() {
		return this.seatsReservedDescription;
	}

	@Nullable
	public ZoneId getTimeZone() {
		return this.timeZone;
	}

	@Nullable
	public String getImageUrl() {
		return this.imageUrl;
	}

	@Nullable
	public String getVideoconferenceUrl() {
		return this.videoconferenceUrl;
	}

	@Nullable
	public String getScheduleUrl() {
		return this.scheduleUrl;
	}

	@Nullable
	public String getConfirmationEmailContent() {
		return this.confirmationEmailContent;
	}

	@Nonnull
	public Boolean getSendFollowupEmail() {
		return this.sendFollowupEmail;
	}

	@Nullable
	public String getFollowupEmailContent() {
		return this.followupEmailContent;
	}

	@Nullable
	public String getFollowupEmailSurveyUrl() {
		return this.followupEmailSurveyUrl;
	}

	@Nullable
	public UUID getGroupSessionCollectionId() {
		return this.groupSessionCollectionId;
	}

	@Nullable
	public Boolean getVisibleFlag() {
		return this.visibleFlag;
	}

	@Nullable
	public UUID getScreeningFlowId() {
		return this.screeningFlowId;
	}

	@Nullable
	public Boolean getSendReminderEmail() {
		return this.sendReminderEmail;
	}

	@Nullable
	public String getReminderEmailContent() {
		return this.reminderEmailContent;
	}

	@Nullable
	public LocalTime getFollowupTimeOfDay() {
		return this.followupTimeOfDay;
	}

	@Nullable
	public Integer getFollowupDayOffset() {
		return this.followupDayOffset;
	}

	@Nullable
	public Boolean getSingleSessionFlag() {
		return this.singleSessionFlag;
	}

	@Nullable
	public String getDateTimeDescription() {
		return this.dateTimeDescription;
	}

	@Nonnull
	public List<TagApiResponse> getTags() {
		return this.tags;
	}

	@Nullable
	public String getLearnMoreDescription() {
		return this.learnMoreDescription;
	}

	@Nullable
	public GroupSessionLearnMoreMethodId getGroupSessionLearnMoreMethodId() {
		return this.groupSessionLearnMoreMethodId;
	}

	@Nonnull
	public Instant getCreated() {
		return this.created;
	}

	@Nonnull
	public String getCreatedDescription() {
		return this.createdDescription;
	}

	@Nonnull
	public String getCreatedDateDescription() {
		return this.createdDateDescription;
	}

	@Nullable
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	@Nullable
	public String getLastUpdatedDescription() {
		return this.lastUpdatedDescription;
	}

	@Nullable
	public Boolean getDifferentEmailAddressForNotifications() {
		return this.differentEmailAddressForNotifications;
	}

	@Nullable
	public LocalDate getStartDate() {
		return this.startDate;
	}

	@Nullable
	public String getStartDateDescription() {
		return this.startDateDescription;
	}

	@Nullable
	public LocalDate getEndDate() {
		return this.endDate;
	}

	@Nullable
	public String getEndDateDescription() {
		return this.endDateDescription;
	}

	@Nullable
	public String getGroupSessionCollectionUrlName() {
		return this.groupSessionCollectionUrlName;
	}
}
