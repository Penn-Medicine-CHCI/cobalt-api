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

import com.cobaltplatform.api.model.db.AttendanceStatus.AttendanceStatusId;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.VideoconferencePlatform.VideoconferencePlatformId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Appointment implements Comparable<Appointment> {
	@Nonnull
	private static final Comparator<Appointment> DEFAULT_COMPARATOR;

	@Nullable
	private UUID appointmentId;
	@Nullable
	private UUID providerId;
	@Nullable
	private UUID accountId;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private UUID appointmentTypeId;
	@Nullable
	private UUID appointmentReasonId;
	@Nullable
	private AttendanceStatusId attendanceStatusId;
	@Nullable
	private UUID intakeAssessmentId;
	@Nullable
	private Long acuityAppointmentId;
	@Nullable
	private Long acuityClassId;
	@Nullable
	private Long bluejeansMeetingId;
	@Nullable
	private String bluejeansParticipantPasscode;
	@Nullable
	private String videoconferenceUrl;
	@Nullable
	private VideoconferencePlatformId videoconferencePlatformId;
	@Nullable
	private SchedulingSystemId schedulingSystemId;
	@Nullable
	private String epicContactId;
	@Nullable
	private String epicContactIdType;
	@Nullable
	private String title;
	@Nullable
	private String phoneNumber;
	@Nullable
	private String comment;
	@Nullable
	private LocalDateTime startTime;
	@Nullable
	private LocalDateTime endTime;
	@Nullable
	private Long durationInMinutes;
	@Nullable
	private ZoneId timeZone;
	@Nullable
	private Boolean canceled;
	@Nullable
	private Instant canceledAt;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;
	@Nullable
	private Boolean canceledForReschedule;
	@Nullable
	private UUID rescheduledAppointmentId;
	@Nullable
	private UUID intakeAccountSessionId;

	static {
		DEFAULT_COMPARATOR = Comparator.comparing(Appointment::getStartTime)
				.thenComparing(Appointment::getEndTime);
	}

	@Override
	public int compareTo(@Nonnull Appointment otherAppointment) {
		requireNonNull(otherAppointment);
		return getDefaultComparator().compare(this, otherAppointment);
	}

	@Nonnull
	public Comparator<Appointment> getDefaultComparator() {
		return DEFAULT_COMPARATOR;
	}

	@Nullable
	public UUID getAppointmentId() {
		return appointmentId;
	}

	public void setAppointmentId(@Nullable UUID appointmentId) {
		this.appointmentId = appointmentId;
	}

	@Nullable
	public UUID getProviderId() {
		return providerId;
	}

	public void setProviderId(@Nullable UUID providerId) {
		this.providerId = providerId;
	}

	@Nullable
	public UUID getAccountId() {
		return accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return createdByAccountId;
	}

	public void setCreatedByAccountId(@Nullable UUID createdByAccountId) {
		this.createdByAccountId = createdByAccountId;
	}

	@Nullable
	public Long getAcuityAppointmentId() {
		return acuityAppointmentId;
	}

	public void setAcuityAppointmentId(@Nullable Long acuityAppointmentId) {
		this.acuityAppointmentId = acuityAppointmentId;
	}

	@Nullable
	public UUID getAppointmentTypeId() {
		return appointmentTypeId;
	}

	public void setAppointmentTypeId(@Nullable UUID appointmentTypeId) {
		this.appointmentTypeId = appointmentTypeId;
	}

	@Nullable
	public Long getAcuityClassId() {
		return acuityClassId;
	}

	public void setAcuityClassId(@Nullable Long acuityClassId) {
		this.acuityClassId = acuityClassId;
	}

	@Nullable
	public Long getBluejeansMeetingId() {
		return bluejeansMeetingId;
	}

	public void setBluejeansMeetingId(@Nullable Long bluejeansMeetingId) {
		this.bluejeansMeetingId = bluejeansMeetingId;
	}

	@Nullable
	public String getBluejeansParticipantPasscode() {
		return bluejeansParticipantPasscode;
	}

	public void setBluejeansParticipantPasscode(@Nullable String bluejeansParticipantPasscode) {
		this.bluejeansParticipantPasscode = bluejeansParticipantPasscode;
	}

	@Nullable
	public String getVideoconferenceUrl() {
		return videoconferenceUrl;
	}

	public void setVideoconferenceUrl(@Nullable String videoconferenceUrl) {
		this.videoconferenceUrl = videoconferenceUrl;
	}

	@Nullable
	public String getTitle() {
		return title;
	}

	public void setTitle(@Nullable String title) {
		this.title = title;
	}

	@Nullable
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(@Nullable String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@Nullable
	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(@Nullable LocalDateTime startTime) {
		this.startTime = startTime;
	}

	@Nullable
	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(@Nullable LocalDateTime endTime) {
		this.endTime = endTime;
	}

	@Nullable
	public Long getDurationInMinutes() {
		return durationInMinutes;
	}

	public void setDurationInMinutes(@Nullable Long durationInMinutes) {
		this.durationInMinutes = durationInMinutes;
	}

	@Nullable
	public ZoneId getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(@Nullable ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	@Nullable
	public Boolean getCanceled() {
		return canceled;
	}

	public void setCanceled(@Nullable Boolean canceled) {
		this.canceled = canceled;
	}

	@Nullable
	public Instant getCanceledAt() {
		return canceledAt;
	}

	public void setCanceledAt(@Nullable Instant canceledAt) {
		this.canceledAt = canceledAt;
	}

	@Nullable
	public String getEpicContactId() {
		return epicContactId;
	}

	public void setEpicContactId(@Nullable String epicContactId) {
		this.epicContactId = epicContactId;
	}

	@Nullable
	public String getEpicContactIdType() {
		return epicContactIdType;
	}

	public void setEpicContactIdType(@Nullable String epicContactIdType) {
		this.epicContactIdType = epicContactIdType;
	}

	@Nullable
	public VideoconferencePlatformId getVideoconferencePlatformId() {
		return videoconferencePlatformId;
	}

	public void setVideoconferencePlatformId(@Nullable VideoconferencePlatformId videoconferencePlatformId) {
		this.videoconferencePlatformId = videoconferencePlatformId;
	}

	@Nullable
	public SchedulingSystemId getSchedulingSystemId() {
		return schedulingSystemId;
	}

	public void setSchedulingSystemId(@Nullable SchedulingSystemId schedulingSystemId) {
		this.schedulingSystemId = schedulingSystemId;
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
	public UUID getAppointmentReasonId() {
		return appointmentReasonId;
	}

	public void setAppointmentReasonId(@Nullable UUID appointmentReasonId) {
		this.appointmentReasonId = appointmentReasonId;
	}

	@Nullable
	public String getComment() {
		return comment;
	}

	public void setComment(@Nullable String comment) {
		this.comment = comment;
	}

	@Nullable
	public AttendanceStatusId getAttendanceStatusId() {
		return attendanceStatusId;
	}

	public void setAttendanceStatusId(@Nullable AttendanceStatusId attendanceStatusId) {
		this.attendanceStatusId = attendanceStatusId;
	}

	@Nullable
	public UUID getIntakeAssessmentId() {
		return intakeAssessmentId;
	}

	public void setIntakeAssessmentId(@Nullable UUID intakeAssessmentId) {
		this.intakeAssessmentId = intakeAssessmentId;
	}

	@Nullable
	public Boolean getCanceledForReschedule() {
		return canceledForReschedule;
	}

	public void setCanceledForReschedule(@Nullable Boolean canceledForReschedule) {
		this.canceledForReschedule = canceledForReschedule;
	}

	@Nullable
	public UUID getRescheduledAppointmentId() {
		return rescheduledAppointmentId;
	}

	public void setRescheduledAppointmentId(@Nullable UUID rescheduledAppointmentId) {
		this.rescheduledAppointmentId = rescheduledAppointmentId;
	}

	@Nullable
	public UUID getIntakeAccountSessionId() {
		return intakeAccountSessionId;
	}

	public void setIntakeAccountSessionId(@Nullable UUID intakeAccountSessionId) {
		this.intakeAccountSessionId = intakeAccountSessionId;
	}
}