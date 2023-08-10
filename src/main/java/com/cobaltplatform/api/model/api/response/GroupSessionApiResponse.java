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
import com.cobaltplatform.api.model.db.GroupSessionLearnMoreMethod;
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

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class GroupSessionApiResponse {
	@Nonnull
	private final UUID groupSessionId;
	@Nullable
	private final InstitutionId institutionId;
	@Nullable
	private final String institutionDescription;
	@Nullable
	private final GroupSessionStatusId groupSessionStatusId;
	@Nullable
	private final String groupSessionStatusIdDescription;
	@Nullable
	private GroupSessionSchedulingSystemId groupSessionSchedulingSystemId;
	@Nonnull
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
	private final UUID facilitatorAccountId;
	@Nullable
	private final String facilitatorName;
	@Nullable
	private final String facilitatorEmailAddress;
	@Nonnull
	private final String appointmentTimeDescription;
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
	@Deprecated
	private final List<String> screeningQuestions;
	@Nullable
	private final List<QuestionApiResponse> screeningQuestionsV2;
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
	private final List<String> tagIds;
	@Nullable
	private String learnMoreDescription;
	@Nullable
	private GroupSessionLearnMoreMethod.GroupSessionLearnMoreMethodId groupSessionLearnMoreMethodId;
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
																 @Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
																 @Assisted @Nonnull GroupSession groupSession) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(institutionService);
		requireNonNull(groupSessionService);
		requireNonNull(questionApiResponseFactory);
		requireNonNull(currentContextProvider);
		requireNonNull(groupSession);

		CurrentContext currentContext = currentContextProvider.get();
		Account account = currentContext.getAccount().get();

		Boolean hasStartEndTime = groupSession.getStartDateTime() != null && groupSession.getEndDateTime() != null;

		this.groupSessionId = groupSession.getGroupSessionId();
		this.institutionId = groupSession.getInstitutionId();
		this.institutionDescription = institutionService.findInstitutionById(institutionId).get().getName();
		this.groupSessionStatusId = groupSession.getGroupSessionStatusId();
		this.groupSessionStatusIdDescription = groupSessionService.findGroupSessionStatusById(groupSession.getGroupSessionStatusId()).get().getDescription();
		this.groupSessionSchedulingSystemId = groupSession.getGroupSessionSchedulingSystemId();
		this.assessmentId = groupSession.getAssessmentId();
		this.submitterAccountId = groupSession.getSubmitterAccountId();

		// Can only see submitter info if you're an admin or you're the submitter/facilitator
		if (account.getRoleId() == RoleId.ADMINISTRATOR
				|| Objects.equals(account.getAccountId(), groupSession.getSubmitterAccountId())
				|| Objects.equals(account.getAccountId(), groupSession.getFacilitatorAccountId())) {
			this.targetEmailAddress = groupSession.getTargetEmailAddress();
		} else {
			this.targetEmailAddress = null;
		}

		this.title = groupSession.getTitle();
		this.description = groupSession.getDescription();
		this.urlName = groupSession.getUrlName();
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
		this.appointmentTimeDescription = hasStartEndTime ? AppointmentTimeFormatter.createTimeDescription(groupSession.getStartDateTime(), groupSession.getEndDateTime(), groupSession.getTimeZone()) : null;
		this.durationInMinutes = hasStartEndTime ? (int) Duration.between(startDateTime, endDateTime).
				toMinutes() : 0;
		this.durationInMinutesDescription = hasStartEndTime ? strings.get("{{duration}} minutes", new HashMap<String, Object>() {
			{
				put("duration", durationInMinutes);
			}
		}) : null;

		if (groupSession.getGroupSessionSchedulingSystemId() == GroupSessionSchedulingSystemId.COBALT) {
			this.seats = groupSession.getSeats();
			this.seatsDescription = strings.get("{{seatsDescription}} seats", new HashMap<String, Object>() {{
				put("seats", groupSession.getSeats());
				put("seatsDescription", formatter.formatNumber(groupSession.getSeats()));
			}});
			this.seatsAvailable = groupSession.getSeatsAvailable();
			this.seatsAvailableDescription = strings.get("{{seatsAvailableDescription}} seats left", new HashMap<String, Object>() {{
				put("seatsAvailable", groupSession.getSeatsAvailable());
				put("seatsAvailableDescription", formatter.formatNumber(groupSession.getSeatsAvailable()));
			}});
			this.seatsReserved = groupSession.getSeatsReserved();
			this.seatsReservedDescription = strings.get("{{seatsReservedDescription}} reservations", new HashMap<String, Object>() {{
				put("seatsReserved", groupSession.getSeatsReserved());
				put("seatsReservedDescription", formatter.formatNumber(groupSession.getSeatsReserved()));
			}});
		} else if (groupSession.getGroupSessionSchedulingSystemId() == GroupSessionSchedulingSystemId.EXTERNAL) {
			this.seats = null;
			this.seatsDescription = null;
			this.seatsAvailable = null;
			this.seatsAvailableDescription = null;
			this.seatsReserved = null;
			this.seatsReservedDescription = null;
		} else {
			throw new UnsupportedOperationException(format("Not sure what to do with %s.%s", GroupSessionSchedulingSystemId.class.getSimpleName(), this.groupSessionSchedulingSystemId.name()));
		}

		this.timeZone = groupSession.getTimeZone();
		this.imageUrl = groupSession.getImageUrl();
		this.videoconferenceUrl = groupSession.getVideoconferenceUrl();
		this.scheduleUrl = groupSession.getScheduleUrl();
		this.screeningQuestionsV2 = groupSessionService.findScreeningQuestionsByGroupSessionId(groupSession.getGroupSessionId()).

				stream()
						.

				map(question -> questionApiResponseFactory.create(question))
						.

				collect(Collectors.toList());
		this.screeningQuestions = this.screeningQuestionsV2.stream()
						.

				map(question -> question.getQuestion().

						orElse(null))
						.

				collect(Collectors.toList());

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
		this.tagIds = groupSession.getTags() == null ? Collections.emptyList() : groupSession.getTags().stream()
				.map(tag -> tag.getTagId())
				.collect(Collectors.toList());
		this.groupSessionLearnMoreMethodId = groupSession.getGroupSessionLearnMoreMethodId();
		this.learnMoreDescription = groupSession.getLearnMoreDescription();
		this.created = groupSession.getCreated();
		this.createdDescription = formatter.formatTimestamp(groupSession.getCreated());

		LocalDate createdDate = LocalDate.ofInstant(groupSession.getCreated(), currentContextProvider.get().getTimeZone());
		this.createdDateDescription = formatter.formatDate(createdDate, FormatStyle.SHORT);

		this.lastUpdated = groupSession.getLastUpdated();
		this.lastUpdatedDescription = formatter.formatTimestamp(groupSession.getLastUpdated());
	}

	@Nonnull
	public UUID getGroupSessionId() {
		return groupSessionId;
	}

	@Nullable
	public InstitutionId getInstitutionId() {
		return institutionId;
	}

	@Nullable
	public String getInstitutionDescription() {
		return institutionDescription;
	}

	@Nullable
	public GroupSessionStatusId getGroupSessionStatusId() {
		return groupSessionStatusId;
	}

	@Nullable
	public String getGroupSessionStatusIdDescription() {
		return groupSessionStatusIdDescription;
	}

	@Nullable
	public GroupSessionSchedulingSystemId getGroupSessionSchedulingSystemId() {
		return groupSessionSchedulingSystemId;
	}

	@Nonnull
	public String getAppointmentTimeDescription() {
		return appointmentTimeDescription;
	}

	@Nonnull
	public UUID getAssessmentId() {
		return assessmentId;
	}

	@Nonnull
	public UUID getSubmitterAccountId() {
		return submitterAccountId;
	}

	@Nonnull
	public String getTargetEmailAddress() {
		return this.targetEmailAddress;
	}

	@Nullable
	public String getTitle() {
		return title;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	@Nullable
	public String getUrlName() {
		return urlName;
	}

	@Nullable
	public UUID getFacilitatorAccountId() {
		return facilitatorAccountId;
	}

	@Nullable
	public String getFacilitatorName() {
		return facilitatorName;
	}

	@Nullable
	public String getFacilitatorEmailAddress() {
		return facilitatorEmailAddress;
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
	public LocalDateTime getEndDateTime() {
		return endDateTime;
	}

	@Nullable
	public String getEndDateTimeDescription() {
		return endDateTimeDescription;
	}

	@Nullable
	public Integer getDurationInMinutes() {
		return durationInMinutes;
	}

	@Nullable
	public String getDurationInMinutesDescription() {
		return durationInMinutesDescription;
	}

	@Nullable
	public Integer getSeats() {
		return seats;
	}

	@Nullable
	public String getSeatsDescription() {
		return seatsDescription;
	}

	@Nullable
	public Integer getSeatsAvailable() {
		return seatsAvailable;
	}

	@Nullable
	public String getSeatsAvailableDescription() {
		return seatsAvailableDescription;
	}

	@Nullable
	public Integer getSeatsReserved() {
		return seatsReserved;
	}

	@Nullable
	public String getSeatsReservedDescription() {
		return seatsReservedDescription;
	}

	@Nullable
	public ZoneId getTimeZone() {
		return timeZone;
	}

	@Nullable
	public String getImageUrl() {
		return imageUrl;
	}

	@Nullable
	public String getVideoconferenceUrl() {
		return videoconferenceUrl;
	}

	@Nullable
	public String getScheduleUrl() {
		return scheduleUrl;
	}

	@Nullable
	public List<String> getScreeningQuestions() {
		return screeningQuestions;
	}

	@Nullable
	public List<QuestionApiResponse> getScreeningQuestionsV2() {
		return screeningQuestionsV2;
	}

	@Nullable
	public String getConfirmationEmailContent() {
		return confirmationEmailContent;
	}

	@Nonnull
	public Boolean getSendFollowupEmail() {
		return sendFollowupEmail;
	}

	@Nullable
	public String getFollowupEmailContent() {
		return followupEmailContent;
	}

	@Nullable
	public String getFollowupEmailSurveyUrl() {
		return followupEmailSurveyUrl;
	}

	@Nullable
	public UUID getGroupSessionCollectionId() {
		return groupSessionCollectionId;
	}

	@Nullable
	public Boolean getVisibleFlag() {
		return visibleFlag;
	}

	@Nullable
	public UUID getScreeningFlowId() {
		return screeningFlowId;
	}

	@Nullable
	public Boolean getSendReminderEmail() {
		return sendReminderEmail;
	}

	@Nullable
	public String getReminderEmailContent() {
		return reminderEmailContent;
	}

	@Nullable
	public String getLearnMoreDescription() {
		return learnMoreDescription;
	}

	@Nullable
	public GroupSessionLearnMoreMethod.GroupSessionLearnMoreMethodId getGroupSessionLearnMoreMethodId() {
		return groupSessionLearnMoreMethodId;
	}

	@Nullable
	public LocalTime getFollowupTimeOfDay() {
		return followupTimeOfDay;
	}

	@Nullable
	public Integer getFollowupDayOffset() {
		return followupDayOffset;
	}

	@Nullable
	public Boolean getSingleSessionFlag() {
		return singleSessionFlag;
	}

	@Nullable
	public String getDateTimeDescription() {
		return dateTimeDescription;
	}

	@Nonnull
	public List<String> getTagIds() {
		return tagIds;
	}

	@Nonnull
	public Instant getCreated() {
		return created;
	}

	@Nonnull
	public String getCreatedDescription() {
		return createdDescription;
	}

	@Nonnull
	public String getCreatedDateDescription() {
		return createdDateDescription;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	@Nullable
	public String getLastUpdatedDescription() {
		return lastUpdatedDescription;
	}
}
