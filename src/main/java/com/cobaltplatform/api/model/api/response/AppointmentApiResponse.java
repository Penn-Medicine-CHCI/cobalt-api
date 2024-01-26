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

import com.cobaltplatform.api.model.api.response.AppointmentTypeApiResponse.AppointmentTypeApiResponseFactory;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;
import com.cobaltplatform.api.model.api.response.AccountApiResponse.AccountApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ProviderApiResponse.ProviderApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ProviderApiResponse.ProviderApiResponseSupplement;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.AppointmentReason;
import com.cobaltplatform.api.model.db.AttendanceStatus.AttendanceStatusId;
import com.cobaltplatform.api.model.db.VideoconferencePlatform.VideoconferencePlatformId;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AppointmentService;
import com.cobaltplatform.api.service.ProviderService;
import com.cobaltplatform.api.util.AppointmentTimeFormatter;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AppointmentApiResponse {
	@Nonnull
	private final UUID appointmentId;
	@Nonnull
	private final UUID accountId;
	@Nonnull
	private final UUID appointmentReasonId;
	@Nonnull
	private final AttendanceStatusId attendanceStatusId;
	@Nonnull
	private final UUID createdByAccountId;
	@Nullable
	private final UUID appointmentTypeId;
	@Nullable
	private final UUID intakeAssessmentId;
	@Nullable
	private final UUID patientOrderId;
	@Nullable
	private final Long acuityAppointmentId;
	@Nonnull
	private final Long bluejeansMeetingId;
	@Nullable
	private final String groupEventId;
	@Nullable
	private final String groupEventTypeId;
	@Nonnull
	private final String title;
	@Nonnull
	private final String subtitle;
	@Nonnull
	private final String appointmentDescription;
	@Nonnull
	private final Instant startTime;
	@Nonnull
	private final String startTimeDescription;
	@Nonnull
	private final LocalDate localStartDate;
	@Nonnull
	private final LocalTime localStartTime;
	@Nonnull
	private final Instant endTime;
	@Nonnull
	private final String endTimeDescription;
	@Nonnull
	private final LocalDate localEndDate;
	@Nonnull
	private final LocalTime localEndTime;
	@Nonnull
	private final Long durationInMinutes;
	@Nonnull
	private final String durationInMinutesDescription;
	@Nullable
	private String timeDescription;
	@Nonnull
	private final ZoneId timeZone;
	@Nonnull
	private final String videoconferenceUrl;
	@Nonnull
	private final VideoconferencePlatformId videoconferencePlatformId;
	@Nonnull
	private final SchedulingSystemId schedulingSystemId;
	@Nonnull
	private final Boolean canceledForReschedule;
	@Nullable
	private final UUID rescheduledAppointmentId;
	@Nullable
	private final Boolean canceled;
	@Nullable
	private final Instant canceledAt;
	@Nullable
	private final String canceledAtDescription;
	@Nullable
	private final String phoneNumber;
	@Nullable
	private final String phoneNumberDescription;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;
	@Nullable
	private final ProviderApiResponse provider;
	@Nullable
	private final AccountApiResponse account;
	@Nullable
	private final AppointmentReasonApiResponse appointmentReason;
	@Nullable
	private final AppointmentTypeApiResponse appointmentType;

	public enum AppointmentApiResponseSupplement {
		PROVIDER,
		ACCOUNT,
		APPOINTMENT_REASON,
		APPOINTMENT_TYPE,

		ALL // Special status
	}

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface AppointmentApiResponseFactory {
		@Nonnull
		AppointmentApiResponse create(@Nonnull Appointment appointment);

		@Nonnull
		AppointmentApiResponse create(@Nonnull Appointment appointment,
																	@Nonnull Set<AppointmentApiResponseSupplement> supplements);
	}

	@AssistedInject
	public AppointmentApiResponse(@Nonnull ProviderService providerService,
																@Nonnull AccountService accountService,
																@Nonnull AppointmentService appointmentService,
																@Nonnull ProviderApiResponseFactory providerApiResponseFactory,
																@Nonnull AccountApiResponseFactory accountApiResponseFactory,
																@Nonnull AppointmentTypeApiResponseFactory appointmentTypeApiResponseFactory,
																@Nonnull Formatter formatter,
																@Nonnull Strings strings,
																@Assisted @Nonnull Appointment appointment) {
		this(providerService, accountService, appointmentService, providerApiResponseFactory, accountApiResponseFactory, appointmentTypeApiResponseFactory, formatter, strings, appointment, null);
	}

	@AssistedInject
	public AppointmentApiResponse(@Nonnull ProviderService providerService,
																@Nonnull AccountService accountService,
																@Nonnull AppointmentService appointmentService,
																@Nonnull ProviderApiResponseFactory providerApiResponseFactory,
																@Nonnull AccountApiResponseFactory accountApiResponseFactory,
																@Nonnull AppointmentTypeApiResponseFactory appointmentTypeApiResponseFactory,
																@Nonnull Formatter formatter,
																@Nonnull Strings strings,
																@Assisted @Nonnull Appointment appointment,
																@Assisted @Nullable Set<AppointmentApiResponseSupplement> supplements) {
		requireNonNull(providerService);
		requireNonNull(accountService);
		requireNonNull(appointmentService);
		requireNonNull(providerApiResponseFactory);
		requireNonNull(accountApiResponseFactory);
		requireNonNull(appointmentTypeApiResponseFactory);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(appointment);

		if(supplements == null)
			supplements = Collections.emptySet();

		this.appointmentId = appointment.getAppointmentId();
		this.accountId = appointment.getAccountId();
		this.appointmentReasonId = appointment.getAppointmentReasonId();
		this.attendanceStatusId = appointment.getAttendanceStatusId();
		this.createdByAccountId = appointment.getCreatedByAccountId();
		this.acuityAppointmentId = appointment.getAcuityAppointmentId();
		this.intakeAssessmentId = appointment.getIntakeAssessmentId();
		this.patientOrderId = appointment.getPatientOrderId();
		this.appointmentTypeId = appointment.getAppointmentTypeId();
		this.bluejeansMeetingId = appointment.getBluejeansMeetingId();
		this.groupEventId = appointment.getAcuityClassId() == null ? null : String.valueOf(appointment.getAcuityClassId());
		this.groupEventTypeId = appointment.getAcuityClassId() == null ? null : String.valueOf(appointment.getAppointmentTypeId());
		this.created = appointment.getCreated();
		this.createdDescription = formatter.formatTimestamp(appointment.getCreated());
		this.title = appointment.getTitle();
		this.subtitle = appointment.getAcuityClassId() == null ? strings.get("1:1 Support") : strings.get("In the Studio");
		this.appointmentDescription = appointment.getAcuityClassId() == null ? strings.get("Appointment") : strings.get("Reservation");
		this.startTime = appointment.getStartTime().atZone(appointment.getTimeZone()).toInstant();
		this.startTimeDescription = formatter.formatTimestamp(getStartTime());
		this.localStartDate = appointment.getStartTime().toLocalDate();
		this.localStartTime = appointment.getStartTime().toLocalTime();
		this.endTime = appointment.getEndTime().atZone(appointment.getTimeZone()).toInstant();
		this.endTimeDescription = formatter.formatTimestamp(getEndTime());
		this.localEndDate = appointment.getEndTime().toLocalDate();
		this.localEndTime = appointment.getEndTime().toLocalTime();
		this.durationInMinutes = appointment.getDurationInMinutes();
		this.durationInMinutesDescription = strings.get("{{duration}} minutes", new HashMap<String, Object>() {{
			put("duration", appointment.getDurationInMinutes());
		}});
		this.timeDescription = AppointmentTimeFormatter.createTimeDescription(getStartTime(), getEndTime(), appointment.getTimeZone());
		this.timeZone = appointment.getTimeZone();
		this.videoconferenceUrl = appointment.getVideoconferenceUrl();
		this.videoconferencePlatformId = appointment.getVideoconferencePlatformId();
		this.schedulingSystemId = appointment.getSchedulingSystemId();
		this.rescheduledAppointmentId = appointment.getRescheduledAppointmentId();
		this.canceledForReschedule = appointment.getCanceledForReschedule() == null ? false : appointment.getCanceledForReschedule();
		this.canceled = appointment.getCanceled();
		this.canceledAt = appointment.getCanceledAt();
		this.canceledAtDescription = appointment.getCanceledAt() == null ? null : formatter.formatTimestamp(appointment.getCanceledAt());

		if (supplements.contains(AppointmentApiResponseSupplement.ALL) || supplements.contains(AppointmentApiResponseSupplement.PROVIDER) && appointment.getProviderId() != null)
			this.provider = providerApiResponseFactory.create(providerService.findProviderById(appointment.getProviderId()).get(), ProviderApiResponseSupplement.PAYMENT_FUNDING);
		else
			this.provider = null;

		this.phoneNumber = appointment.getPhoneNumber();
		this.phoneNumberDescription = appointment.getPhoneNumber() == null ? null : formatter.formatPhoneNumber(appointment.getPhoneNumber());

		if (supplements.contains(AppointmentApiResponseSupplement.ALL) || supplements.contains(AppointmentApiResponseSupplement.ACCOUNT))
			this.account = accountApiResponseFactory.create(accountService.findAccountById(appointment.getAccountId()).get());
		else
			this.account = null;

		AppointmentReasonApiResponse appointmentReasonApiResponse = null;

		if (supplements.contains(AppointmentApiResponseSupplement.ALL) || supplements.contains(AppointmentApiResponseSupplement.APPOINTMENT_REASON)) {
			AppointmentReason appointmentReason = appointmentService.findAppointmentReasonById(appointment.getAppointmentReasonId()).orElse(null);

			if (appointmentReason != null)
				appointmentReasonApiResponse = new AppointmentReasonApiResponse(appointmentReason);
		}

		this.appointmentReason = appointmentReasonApiResponse;

		AppointmentTypeApiResponse appointmentTypeApiResponse = null;

		if (supplements.contains(AppointmentApiResponseSupplement.ALL) || supplements.contains(AppointmentApiResponseSupplement.APPOINTMENT_TYPE)) {
			AppointmentType appointmentType = appointmentService.findAppointmentTypeById(appointment.getAppointmentTypeId()).orElse(null);

			if (appointmentType != null)
				appointmentTypeApiResponse = appointmentTypeApiResponseFactory.create(appointmentType);
		}

		this.appointmentType = appointmentTypeApiResponse;
	}

	@Nonnull
	public UUID getAppointmentId() {
		return appointmentId;
	}

	@Nonnull
	public UUID getAccountId() {
		return accountId;
	}

	@Nonnull
	public UUID getCreatedByAccountId() {
		return createdByAccountId;
	}

	@Nonnull
	public AttendanceStatusId getAttendanceStatusId() {
		return attendanceStatusId;
	}

	@Nonnull
	public Long getAcuityAppointmentId() {
		return acuityAppointmentId;
	}

	@Nullable
	public UUID getIntakeAssessmentId() {
		return intakeAssessmentId;
	}

	@Nullable
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	@Nullable
	public UUID getAppointmentTypeId() {
		return appointmentTypeId;
	}

	@Nonnull
	public Long getBluejeansMeetingId() {
		return bluejeansMeetingId;
	}

	@Nullable
	public String getGroupEventId() {
		return groupEventId;
	}

	@Nullable
	public String getGroupEventTypeId() {
		return groupEventTypeId;
	}

	@Nonnull
	public String getTitle() {
		return title;
	}

	@Nonnull
	public String getSubtitle() {
		return subtitle;
	}

	@Nonnull
	public String getAppointmentDescription() {
		return appointmentDescription;
	}

	@Nonnull
	public Instant getStartTime() {
		return startTime;
	}

	@Nonnull
	public String getStartTimeDescription() {
		return startTimeDescription;
	}

	@Nonnull
	public Instant getEndTime() {
		return endTime;
	}

	@Nonnull
	public String getEndTimeDescription() {
		return endTimeDescription;
	}

	@Nonnull
	public Long getDurationInMinutes() {
		return durationInMinutes;
	}

	@Nonnull
	public String getDurationInMinutesDescription() {
		return durationInMinutesDescription;
	}

	@Nullable
	public String getTimeDescription() {
		return timeDescription;
	}

	public void setTimeDescription(@Nullable String timeDescription) {
		this.timeDescription = timeDescription;
	}

	@Nonnull
	public ZoneId getTimeZone() {
		return timeZone;
	}

	@Nonnull
	public String getVideoconferenceUrl() {
		return videoconferenceUrl;
	}

	@Nonnull
	public VideoconferencePlatformId getVideoconferencePlatformId() {
		return videoconferencePlatformId;
	}

	@Nonnull
	public SchedulingSystemId getSchedulingSystemId() {
		return schedulingSystemId;
	}

	@Nonnull
	public Boolean getCanceledForReschedule() {
		return canceledForReschedule;
	}

	@Nullable
	public UUID getRescheduledAppointmentId() {
		return rescheduledAppointmentId;
	}

	@Nullable
	public String getPhoneNumber() {
		return phoneNumber;
	}

	@Nullable
	public String getPhoneNumberDescription() {
		return phoneNumberDescription;
	}

	@Nullable
	public Boolean getCanceled() {
		return canceled;
	}

	@Nullable
	public Instant getCanceledAt() {
		return canceledAt;
	}

	@Nullable
	public String getCanceledAtDescription() {
		return canceledAtDescription;
	}

	@Nonnull
	public Instant getCreated() {
		return created;
	}

	@Nonnull
	public String getCreatedDescription() {
		return createdDescription;
	}

	@Nullable
	public ProviderApiResponse getProvider() {
		return provider;
	}

	@Nonnull
	public LocalDate getLocalStartDate() {
		return localStartDate;
	}

	@Nonnull
	public LocalTime getLocalStartTime() {
		return localStartTime;
	}

	@Nonnull
	public LocalDate getLocalEndDate() {
		return localEndDate;
	}

	@Nonnull
	public LocalTime getLocalEndTime() {
		return localEndTime;
	}

	@Nullable
	public AccountApiResponse getAccount() {
		return account;
	}

	@Nonnull
	public UUID getAppointmentReasonId() {
		return appointmentReasonId;
	}

	@Nullable
	public AppointmentReasonApiResponse getAppointmentReason() {
		return appointmentReason;
	}

	@Nullable
	public AppointmentTypeApiResponse getAppointmentType() {
		return appointmentType;
	}
}