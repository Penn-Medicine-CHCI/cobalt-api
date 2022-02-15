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

package com.cobaltplatform.api.web.resource;

import com.cobaltplatform.api.model.security.ContentSecurityLevel;
import com.lokalized.Strings;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionApiResponse.GroupSessionApiResponseFactory;
import com.cobaltplatform.api.model.api.response.GroupSessionReservationApiResponse.GroupSessionReservationApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.AuditLog;
import com.cobaltplatform.api.model.db.AuditLogEvent;
import com.cobaltplatform.api.model.db.GroupSession;
import com.cobaltplatform.api.model.db.GroupSessionReservation;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AppointmentService;
import com.cobaltplatform.api.service.AuditLogService;
import com.cobaltplatform.api.service.GroupSessionService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.response.ApiResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class CalendarEventsResource {
	@Nonnull
	private final AppointmentService appointmentService;
	@Nonnull
	private final GroupSessionService groupSessionService;
	@Nonnull
	private final AppointmentApiResponseFactory appointmentApiResponseFactory;
	@Nonnull
	private final GroupSessionApiResponseFactory groupSessionApiResponseFactory;
	@Nonnull
	private final GroupSessionReservationApiResponseFactory groupSessionReservationApiResponseFactory;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final AuditLogService auditLogService;
	@Nonnull
	private final JsonMapper jsonMapper;

	@Inject
	public CalendarEventsResource(@Nonnull AppointmentService appointmentService,
																@Nonnull GroupSessionService groupSessionService,
																@Nonnull AppointmentApiResponseFactory appointmentApiResponseFactory,
																@Nonnull GroupSessionApiResponseFactory groupSessionApiResponseFactory,
																@Nonnull GroupSessionReservationApiResponseFactory groupSessionReservationApiResponseFactory,
																@Nonnull RequestBodyParser requestBodyParser,
																@Nonnull Formatter formatter,
																@Nonnull Strings strings,
																@Nonnull Provider<CurrentContext> currentContextProvider,
																@Nonnull AuditLogService auditLogService,
																@Nonnull JsonMapper jsonMapper) {
		requireNonNull(appointmentService);
		requireNonNull(groupSessionService);
		requireNonNull(appointmentApiResponseFactory);
		requireNonNull(groupSessionApiResponseFactory);
		requireNonNull(groupSessionReservationApiResponseFactory);
		requireNonNull(requestBodyParser);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);
		requireNonNull(auditLogService);
		requireNonNull(jsonMapper);

		this.appointmentService = appointmentService;
		this.groupSessionService = groupSessionService;
		this.appointmentApiResponseFactory = appointmentApiResponseFactory;
		this.groupSessionApiResponseFactory = groupSessionApiResponseFactory;
		this.groupSessionReservationApiResponseFactory = groupSessionReservationApiResponseFactory;
		this.requestBodyParser = requestBodyParser;
		this.formatter = formatter;
		this.strings = strings;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
		this.auditLogService = auditLogService;
		this.jsonMapper = jsonMapper;
	}

	public enum CalendarEventResponseFormat {
		DEFAULT,
		GROUPED_BY_DATE
	}

	public enum CalendarEventType {
		APPOINTMENT,
		GROUP_SESSION_RESERVATION
	}

	@Nonnull
	@GET("/calendar-events/upcoming")
	@AuthenticationRequired(contentSecurityLevel = ContentSecurityLevel.HIGH)
	public ApiResponse appointments(@Nonnull @QueryParameter Optional<CalendarEventResponseFormat> responseFormat) {
		requireNonNull(responseFormat);

		CalendarEventResponseFormat finalResponseFormat = responseFormat.orElse(CalendarEventResponseFormat.DEFAULT);
		Account account = getCurrentContext().getAccount().get();
		ZoneId timeZone = getCurrentContext().getTimeZone();

		List<Appointment> appointments = getAppointmentService().findUpcomingAppointmentsByAccountId(account.getAccountId(), timeZone);
		List<Pair<GroupSession, GroupSessionReservation>> groupSessionReservationPairs = getGroupSessionService().findUpcomingGroupSessionReservationsByAccountId(account.getAccountId(), timeZone);
		List<CalendarEvent> calendarEvents = new ArrayList<>(appointments.size() + groupSessionReservationPairs.size());

		calendarEvents.addAll(appointments.stream().map(appointment -> new CalendarEvent(appointment)).collect(Collectors.toList()));
		calendarEvents.addAll(groupSessionReservationPairs.stream().map(groupSessionReservationPair -> new CalendarEvent(groupSessionReservationPair.getLeft(), groupSessionReservationPair.getRight())).collect(Collectors.toList()));

		Collections.sort(calendarEvents);

		Map<String, Object> responseData = new HashMap<>();

		if (finalResponseFormat == CalendarEventResponseFormat.GROUPED_BY_DATE) {
			Map<LocalDate, List<CalendarEvent>> calendarEventsByDate = new TreeMap<>();
			LocalDate today = LocalDate.now(timeZone);

			for (CalendarEvent calendarEvent : calendarEvents) {
				LocalDate date = calendarEvent.getStartDateTime().toLocalDate();

				List<CalendarEvent> calendarEventsForDate = calendarEventsByDate.get(date);

				if (calendarEventsForDate == null) {
					calendarEventsForDate = new ArrayList<>();
					calendarEventsByDate.put(date, calendarEventsForDate);
				}

				calendarEventsForDate.add(calendarEvent);
			}

			List<Map<String, Object>> dateGroups = new ArrayList<>(calendarEventsByDate.size());

			for (Entry<LocalDate, List<CalendarEvent>> entry : calendarEventsByDate.entrySet()) {
				LocalDate date = entry.getKey();
				List<CalendarEvent> calendarEventsForDate = entry.getValue();

				Map<String, Object> dateGroup = new HashMap<>();
				dateGroup.put("date", date.equals(today) ? getStrings().get("Today") : getFormatter().formatDate(date, FormatStyle.MEDIUM));
				dateGroup.put("calendarEvents", calendarEventsForDate.stream()
						.map((calendarEvent) -> toJson(calendarEvent))
						.collect(Collectors.toList()));

				dateGroups.add(dateGroup);
			}

			responseData.put("calendarEventGroups", dateGroups);
		} else {
			responseData.put("calendarEvents", calendarEvents.stream()
					.map((calendarEvent) -> toJson(calendarEvent))
					.collect(Collectors.toList()));
		}

		AuditLog auditLog = new AuditLog();
		auditLog.setAccountId(account.getAccountId());
		auditLog.setAuditLogEventId(AuditLogEvent.AuditLogEventId.APPOINTMENT_LOOKUP);
		auditLog.setPayload(getJsonMapper().toJson(responseData));
		getAuditLogService().audit(auditLog);

		return new ApiResponse(responseData);
	}

	@Nonnull
	protected Map<String, Object> toJson(@Nonnull CalendarEvent calendarEvent) {
		requireNonNull(calendarEvent);

		Map<String, Object> json = new HashMap<>();

		if (calendarEvent.getCalendarEventType() == CalendarEventType.APPOINTMENT) {
			json.put("calendarEventTypeId", "APPOINTMENT");
			json.put("appointment", getAppointmentApiResponseFactory().create(calendarEvent.getAppointment().get(), Set.of(AppointmentApiResponse.AppointmentApiResponseSupplement.PROVIDER)));
		} else if (calendarEvent.getCalendarEventType() == CalendarEventType.GROUP_SESSION_RESERVATION) {
			json.put("calendarEventTypeId", "GROUP_SESSION_RESERVATION");
			json.put("groupSession", getGroupSessionApiResponseFactory().create(calendarEvent.getGroupSession().get()));
			json.put("groupSessionReservation", getGroupSessionReservationApiResponseFactory().create(calendarEvent.getGroupSessionReservation().get()));
		} else {
			throw new IllegalStateException(format("Not sure how to handle %s.%s", calendarEvent.getCalendarEventType().getClass().getSimpleName(),
					calendarEvent.getCalendarEventType().name()));
		}

		return json;
	}

	@NotThreadSafe
	public static class CalendarEvent implements Comparable<CalendarEvent> {
		@Nonnull
		private final CalendarEventType calendarEventType;
		@Nullable
		private final Appointment appointment;
		@Nullable
		private final GroupSession groupSession;
		@Nullable
		private final GroupSessionReservation groupSessionReservation;
		@Nonnull
		private final LocalDateTime startDateTime;

		public CalendarEvent(@Nonnull Appointment appointment) {
			requireNonNull(appointment);

			this.calendarEventType = CalendarEventType.APPOINTMENT;
			this.appointment = appointment;
			this.groupSession = null;
			this.groupSessionReservation = null;
			this.startDateTime = appointment.getStartTime();
		}

		public CalendarEvent(@Nonnull GroupSession groupSession,
												 @Nonnull GroupSessionReservation groupSessionReservation) {
			requireNonNull(groupSession);
			requireNonNull(groupSessionReservation);

			this.calendarEventType = CalendarEventType.GROUP_SESSION_RESERVATION;
			this.appointment = null;
			this.groupSession = groupSession;
			this.groupSessionReservation = groupSessionReservation;
			this.startDateTime = groupSession.getStartDateTime();
		}

		@Override
		public int compareTo(@Nonnull CalendarEvent calendarEvent) {
			requireNonNull(calendarEvent);
			return getStartDateTime().compareTo(calendarEvent.getStartDateTime());
		}

		@Nonnull
		public CalendarEventType getCalendarEventType() {
			return calendarEventType;
		}

		@Nonnull
		public Optional<Appointment> getAppointment() {
			return Optional.ofNullable(appointment);
		}

		@Nonnull
		public Optional<GroupSession> getGroupSession() {
			return Optional.ofNullable(groupSession);
		}

		@Nonnull
		public Optional<GroupSessionReservation> getGroupSessionReservation() {
			return Optional.ofNullable(groupSessionReservation);
		}

		@Nonnull
		public LocalDateTime getStartDateTime() {
			return startDateTime;
		}
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return appointmentService;
	}

	@Nonnull
	protected AppointmentApiResponseFactory getAppointmentApiResponseFactory() {
		return appointmentApiResponseFactory;
	}

	@Nonnull
	protected GroupSessionService getGroupSessionService() {
		return groupSessionService;
	}

	@Nonnull
	protected GroupSessionApiResponseFactory getGroupSessionApiResponseFactory() {
		return groupSessionApiResponseFactory;
	}

	@Nonnull
	protected GroupSessionReservationApiResponseFactory getGroupSessionReservationApiResponseFactory() {
		return groupSessionReservationApiResponseFactory;
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return requestBodyParser;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return formatter;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return currentContextProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}

	@Nonnull
	protected AuditLogService getAuditLogService() {
		return auditLogService;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return jsonMapper;
	}
}