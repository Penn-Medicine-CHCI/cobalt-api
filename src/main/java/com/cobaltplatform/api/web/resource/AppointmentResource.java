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

import com.cobaltplatform.api.model.api.request.CreateActivityTrackingRequest;
import com.cobaltplatform.api.model.db.ActivityType.ActivityTypeId;
import com.cobaltplatform.api.model.db.ActivityAction.ActivityActionId;
import com.cobaltplatform.api.service.ActivityTrackingService;
import com.lokalized.Strings;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.request.CancelAppointmentRequest;
import com.cobaltplatform.api.model.api.request.ChangeAppointmentAttendanceStatusRequest;
import com.cobaltplatform.api.model.api.request.CreateAppointmentRequest;
import com.cobaltplatform.api.model.api.response.AccountApiResponse.AccountApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseSupplement;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.AuditLog;
import com.cobaltplatform.api.model.db.AuditLogEvent;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AppointmentService;
import com.cobaltplatform.api.service.AuditLogService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.json.JSONObject;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PUT;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import com.soklet.web.response.CustomResponse;
import com.soklet.web.response.RedirectResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class AppointmentResource {
	@Nonnull
	private final AppointmentService appointmentService;
	@Nonnull
	private final AccountService accountService;
	@Nonnull
	private final AppointmentApiResponseFactory appointmentApiResponseFactory;
	@Nonnull
	private final AccountApiResponseFactory accountApiResponseFactory;
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
	private final ActivityTrackingService activityTrackingService;
	@Nonnull
	private final JsonMapper jsonMapper;

	@Inject
	public AppointmentResource(@Nonnull AppointmentService appointmentService,
														 @Nonnull AccountService accountService,
														 @Nonnull AppointmentApiResponseFactory appointmentApiResponseFactory,
														 @Nonnull AccountApiResponseFactory accountApiResponseFactory,
														 @Nonnull RequestBodyParser requestBodyParser,
														 @Nonnull Formatter formatter,
														 @Nonnull Strings strings,
														 @Nonnull Provider<CurrentContext> currentContextProvider,
														 @Nonnull AuditLogService auditLogService,
														 @Nonnull ActivityTrackingService activityTrackingService,
														 @Nonnull JsonMapper jsonMapper) {
		requireNonNull(appointmentService);
		requireNonNull(accountService);
		requireNonNull(appointmentApiResponseFactory);
		requireNonNull(accountApiResponseFactory);
		requireNonNull(requestBodyParser);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);
		requireNonNull(auditLogService);
		requireNonNull(activityTrackingService);
		requireNonNull(jsonMapper);

		this.appointmentService = appointmentService;
		this.accountService = accountService;
		this.appointmentApiResponseFactory = appointmentApiResponseFactory;
		this.accountApiResponseFactory = accountApiResponseFactory;
		this.requestBodyParser = requestBodyParser;
		this.formatter = formatter;
		this.strings = strings;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
		this.auditLogService = auditLogService;
		this.activityTrackingService = activityTrackingService;
		this.jsonMapper = jsonMapper;
	}

	public enum AppointmentResponseFormat {
		DEFAULT,
		GROUPED_BY_DATE
	}

	public enum AppointmentResponseType {
		RECENT,
		UPCOMING
	}

	@Nonnull
	@GET("/appointments/{appointmentId}")
	@AuthenticationRequired
	public ApiResponse appointment(@Nonnull @PathParameter UUID appointmentId) {
		requireNonNull(appointmentId);

		Account account = getCurrentContext().getAccount().get();

		AuditLog auditLog = new AuditLog();
		auditLog.setAccountId(account.getAccountId());
		auditLog.setAuditLogEventId(AuditLogEvent.AuditLogEventId.APPOINTMENT_LOOKUP);
		auditLog.setMessage(String.format("Looked up appointment_id %s", appointmentId.toString()));
		auditLog.setPayload(getJsonMapper().toJson(new HashMap<String, Object>() {{
			put("appointmentId", appointmentId);
		}}));
		getAuditLogService().audit(auditLog);

		Appointment appointment = getAppointmentService().findAppointmentById(appointmentId).orElse(null);

		if (appointment == null)
			throw new NotFoundException();

		if (!appointment.getAccountId().equals(account.getAccountId()))
			throw new AuthorizationException();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("appointment", getAppointmentApiResponseFactory().create(appointment, Collections.singleton(AppointmentApiResponseSupplement.PROVIDER)));
		}});
	}

	@Nonnull
	@GET("/appointments")
	@AuthenticationRequired
	public ApiResponse appointments(@Nonnull @QueryParameter Optional<AppointmentResponseFormat> responseFormat,
																	@Nonnull @QueryParameter Optional<AppointmentResponseType> type,
																	@Nonnull @QueryParameter Optional<UUID> accountId) {
		requireNonNull(responseFormat);
		requireNonNull(type);
		requireNonNull(accountId);

		Account account = getCurrentContext().getAccount().get();

		// Some users can book on behalf of other users
		// TODO: this checking should be more strict and institution-specific
		if (account.getRoleId() == RoleId.MHIC
				|| account.getRoleId() == RoleId.ADMINISTRATOR
				|| account.getRoleId() == RoleId.SUPER_ADMINISTRATOR) {
			// If an account ID was passed in, use it if we can
			if (!accountId.isEmpty())
				account = getAccountService().findAccountById(accountId.get()).orElse(null);
		}

		if (account == null)
			account = getCurrentContext().getAccount().get();

		AppointmentResponseFormat finalResponseFormat = responseFormat.orElse(AppointmentResponseFormat.DEFAULT);
		Map<String, Object> responseData = new HashMap<>();

		List<Appointment> appointments = type.isPresent() && type.get() == AppointmentResponseType.RECENT
				? getAppointmentService().findRecentAppointmentsByAccountId(account.getAccountId(), getCurrentContext().getTimeZone())
				: getAppointmentService().findUpcomingAppointmentsByAccountId(account.getAccountId(), getCurrentContext().getTimeZone());

		if (finalResponseFormat == AppointmentResponseFormat.GROUPED_BY_DATE) {
			Map<LocalDate, List<Appointment>> appointmentsByDate = new TreeMap<>();
			LocalDate today = LocalDate.now(getCurrentContext().getTimeZone());

			for (Appointment appointment : appointments) {
				LocalDate date = appointment.getStartTime().toLocalDate();

				List<Appointment> appointmentsForDate = appointmentsByDate.get(date);

				if (appointmentsForDate == null) {
					appointmentsForDate = new ArrayList<>();
					appointmentsByDate.put(date, appointmentsForDate);
				}

				appointmentsForDate.add(appointment);
			}

			List<Map<String, Object>> dateGroups = new ArrayList<>(appointmentsByDate.size());

			for (Entry<LocalDate, List<Appointment>> entry : appointmentsByDate.entrySet()) {
				LocalDate date = entry.getKey();
				List<Appointment> appointmentsForDate = entry.getValue();

				Map<String, Object> dateGroup = new HashMap<>();
				dateGroup.put("date", date.equals(today) ? getStrings().get("Today") : getFormatter().formatDate(date, FormatStyle.MEDIUM));
				dateGroup.put("appointments", appointmentsForDate.stream()
						.map((appointment) -> getAppointmentApiResponseFactory().create(appointment, Set.of(AppointmentApiResponseSupplement.PROVIDER, AppointmentApiResponseSupplement.APPOINTMENT_REASON)))
						.collect(Collectors.toList()));

				dateGroups.add(dateGroup);
			}

			responseData.put("appointmentGroups", dateGroups);
		} else {
			responseData.put("appointments", appointments.stream()
					.map((appointment) -> getAppointmentApiResponseFactory().create(appointment, Set.of(AppointmentApiResponseSupplement.PROVIDER, AppointmentApiResponseSupplement.APPOINTMENT_REASON)))
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
	@POST("/appointments")
	@AuthenticationRequired
	public ApiResponse createAppointment(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		AuditLog auditLog = new AuditLog();
		auditLog.setAccountId(account.getAccountId());
		auditLog.setAuditLogEventId(AuditLogEvent.AuditLogEventId.APPOINTMENT_CREATE);
		auditLog.setPayload(requestBody);
		getAuditLogService().audit(auditLog);

		CreateAppointmentRequest request = getRequestBodyParser().parse(requestBody, CreateAppointmentRequest.class);
		request.setCreatedByAcountId(account.getAccountId());

		// Some users can book on behalf of other users
		// TODO: this checking should be more strict and institution-specific
		if (account.getRoleId() == RoleId.MHIC
				|| account.getRoleId() == RoleId.ADMINISTRATOR
				|| account.getRoleId() == RoleId.SUPER_ADMINISTRATOR) {
			// If an account ID was passed in, use it as-is, otherwise default to "me"
			if (request.getAccountId() == null)
				request.setAccountId(account.getAccountId());
		} else {
			// If you are not a special role, you can only book for yourself
			request.setAccountId(account.getAccountId());
		}

		UUID appointmentId = getAppointmentService().createAppointment(request);
		Appointment appointment = getAppointmentService().findAppointmentById(appointmentId).get();

		CreateActivityTrackingRequest activityTrackingRequest = new CreateActivityTrackingRequest();
		activityTrackingRequest.setSessionTrackingId(getCurrentContext().getSessionTrackingId());
		activityTrackingRequest.setActivityActionId(ActivityActionId.CREATE);
		activityTrackingRequest.setActivityTypeId(ActivityTypeId.APPOINTMENT);
		activityTrackingRequest.setContext(new JSONObject().put("appointmentId",appointmentId.toString()).toString());

		getActivityTrackingService().trackActivity(account, activityTrackingRequest);

		// It's possible creating the appointment has updated the account's email address.
		// Vend the account so client has the latest and greatest
		Account updatedAccount = getAccountService().findAccountById(request.getAccountId()).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("appointment", getAppointmentApiResponseFactory().create(appointment, Collections.singleton(AppointmentApiResponseSupplement.PROVIDER)));
			put("account", getAccountApiResponseFactory().create(updatedAccount));
		}});
	}

	@Nonnull
	@PUT("/appointments/{appointmentId}/cancel")
	@AuthenticationRequired
	public ApiResponse cancelAppointment(@Nonnull @PathParameter UUID appointmentId,
																			 @Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		AuditLog auditLog = new AuditLog();
		auditLog.setAccountId(account.getAccountId());
		auditLog.setAuditLogEventId(AuditLogEvent.AuditLogEventId.APPOINTMENT_CANCEL);
		auditLog.setMessage(String.format("Cancel appointment_id %s", appointmentId.toString()));
		auditLog.setPayload(requestBody);
		getAuditLogService().audit(auditLog);

		Appointment appointment = getAppointmentService().findAppointmentById(appointmentId).orElse(null);

		if (appointment == null)
			throw new NotFoundException();

		Account appointmentAccount = getAccountService().findAccountById(appointment.getAccountId()).get();

		// Some users can cancel appointments on behalf of other users
		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR) {
			// Superadmin can cancel any appointment
		} else if (account.getRoleId() == RoleId.MHIC || account.getRoleId() == RoleId.ADMINISTRATOR) {
			// "Normal" admins can cancel anything within the same institution
			if (!account.getInstitutionId().equals(appointmentAccount.getInstitutionId()))
				throw new AuthorizationException();
		} else {
			// If you are not a special role, you can only cancel for yourself
			if (!appointmentAccount.getAccountId().equals(account.getAccountId()))
				throw new AuthorizationException();
		}

		CancelAppointmentRequest request = getRequestBodyParser().parse(requestBody, CancelAppointmentRequest.class);
		request.setAccountId(appointmentAccount.getAccountId());
		request.setAppointmentId(appointmentId);
		request.setCanceledByWebhook(false);

		getAppointmentService().cancelAppointment(request);

		CreateActivityTrackingRequest activityTrackingRequest = new CreateActivityTrackingRequest();
		activityTrackingRequest.setSessionTrackingId(getCurrentContext().getSessionTrackingId());
		activityTrackingRequest.setActivityActionId(ActivityActionId.CANCEL);
		activityTrackingRequest.setActivityTypeId(ActivityTypeId.APPOINTMENT);
		activityTrackingRequest.setContext(new JSONObject().put("appointmentId",appointmentId.toString()).toString());

		getActivityTrackingService().trackActivity(account, activityTrackingRequest);

		return new ApiResponse();
	}

	@Nonnull
	@PUT("/appointments/{appointmentId}/attendance-status")
	@AuthenticationRequired
	public ApiResponse changeAppointmentAttendanceStatus(@Nonnull @PathParameter UUID appointmentId,
																											 @Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();
		Appointment appointment = getAppointmentService().findAppointmentById(appointmentId).orElse(null);

		if (appointment == null)
			throw new NotFoundException();

		// TODO: eventually providers should be able to mark appointments as "missed"...but for now, the UI only permits patients to self-report
		if (!appointment.getAccountId().equals(account.getAccountId()))
			throw new AuthorizationException();

		ChangeAppointmentAttendanceStatusRequest request = getRequestBodyParser().parse(requestBody, ChangeAppointmentAttendanceStatusRequest.class);
		request.setAccountId(account.getAccountId());
		request.setAppointmentId(appointmentId);

		getAppointmentService().changeAppointmentAttendanceStatus(request);

		Appointment updatedAppointment = getAppointmentService().findAppointmentById(appointmentId).orElse(null);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("appointment", getAppointmentApiResponseFactory().create(updatedAppointment, Collections.singleton(AppointmentApiResponseSupplement.ALL)));
		}});
	}

	@GET("/appointments/{appointmentId}/google-calendar")
	@AuthenticationRequired
	@Nonnull
	public RedirectResponse appointmentGoogleCalendar(@Nonnull @PathParameter UUID appointmentId) {
		requireNonNull(appointmentId);

		Appointment appointment = getAppointmentService().findAppointmentById(appointmentId).orElse(null);

		if (appointment == null)
			throw new NotFoundException();

		return new RedirectResponse(getAppointmentService().generateGoogleCalendarTemplateUrl(appointment), RedirectResponse.Type.TEMPORARY);
	}

	@GET("/appointments/{appointmentId}/ical")
	@AuthenticationRequired
	@Nonnull
	public CustomResponse appointmentIcal(@Nonnull @PathParameter UUID appointmentId,
																										@Nonnull HttpServletResponse httpServletResponse) throws IOException {
		requireNonNull(appointmentId);
		requireNonNull(httpServletResponse);

		Appointment appointment = getAppointmentService().findAppointmentById(appointmentId).orElse(null);

		if (appointment == null)
			throw new NotFoundException();

		String icalInvite = getAppointmentService().generateICalInvite(appointment);

		httpServletResponse.setContentType("text/calendar; charset=UTF-8");
		httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"invite.ics\"");

		IOUtils.copy(new StringReader(icalInvite), httpServletResponse.getOutputStream(), StandardCharsets.UTF_8);

		return CustomResponse.instance();
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return appointmentService;
	}

	@Nonnull
	protected AccountService getAccountService() {
		return accountService;
	}

	@Nonnull
	protected AppointmentApiResponseFactory getAppointmentApiResponseFactory() {
		return appointmentApiResponseFactory;
	}

	@Nonnull
	protected AccountApiResponseFactory getAccountApiResponseFactory() {
		return accountApiResponseFactory;
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
	protected ActivityTrackingService getActivityTrackingService() {
		return activityTrackingService;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return jsonMapper;
	}
}