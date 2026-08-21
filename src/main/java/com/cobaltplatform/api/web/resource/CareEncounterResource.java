/*
 * Copyright 2021 The University of Pennsylvania and Penn Medicine
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.cobaltplatform.api.web.resource;

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.request.AssignCareEncounterRequest;
import com.cobaltplatform.api.model.api.request.CancelCareEncounterAppointmentRequest;
import com.cobaltplatform.api.model.api.request.CancelCareEncounterRequest;
import com.cobaltplatform.api.model.api.request.ChangeAppointmentAttendanceStatusRequest;
import com.cobaltplatform.api.model.api.request.CreateCareEncounterRequest;
import com.cobaltplatform.api.model.api.request.FindCareEncountersRequest;
import com.cobaltplatform.api.model.api.request.FindCareEncountersRequest.CareEncounterAssignmentScopeId;
import com.cobaltplatform.api.model.api.request.FindCareEncountersRequest.CareEncounterSortColumnId;
import com.cobaltplatform.api.model.api.request.UpdateCareEncounterRequest;
import com.cobaltplatform.api.model.api.response.CareEncounterApiResponse.CareEncounterApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CareEncounterListApiResponse.CareEncounterListApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AuditLog;
import com.cobaltplatform.api.model.db.AuditLogEvent.AuditLogEventId;
import com.cobaltplatform.api.model.db.AttendanceStatus;
import com.cobaltplatform.api.model.db.CareEncounter;
import com.cobaltplatform.api.model.db.CareEncounterCancellationReason;
import com.cobaltplatform.api.model.db.CareEncounterStatus.CareEncounterStatusId;
import com.cobaltplatform.api.model.db.FootprintEventGroupType.FootprintEventGroupTypeId;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.model.service.SortDirectionId;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.AuditLogService;
import com.cobaltplatform.api.service.CareEncounterService;
import com.cobaltplatform.api.service.SystemService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.db.ReadReplica;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.DELETE;
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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Care Navigator administrative endpoints.
 *
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class CareEncounterResource {
	@Nonnull
	private final CareEncounterService careEncounterService;
	@Nonnull
	private final AuditLogService auditLogService;
	@Nonnull
	private final SystemService systemService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final CareEncounterApiResponseFactory careEncounterApiResponseFactory;
	@Nonnull
	private final CareEncounterListApiResponseFactory careEncounterListApiResponseFactory;
	@Nonnull
	private final Formatter formatter;

	@Inject
	public CareEncounterResource(@Nonnull CareEncounterService careEncounterService,
													 @Nonnull AuditLogService auditLogService,
													 @Nonnull SystemService systemService,
													 @Nonnull AuthorizationService authorizationService,
													 @Nonnull RequestBodyParser requestBodyParser,
													 @Nonnull Provider<CurrentContext> currentContextProvider,
													 @Nonnull CareEncounterApiResponseFactory careEncounterApiResponseFactory,
													 @Nonnull CareEncounterListApiResponseFactory careEncounterListApiResponseFactory,
													 @Nonnull Formatter formatter) {
		this.careEncounterService = careEncounterService;
		this.auditLogService = auditLogService;
		this.systemService = systemService;
		this.authorizationService = authorizationService;
		this.requestBodyParser = requestBodyParser;
		this.currentContextProvider = currentContextProvider;
		this.careEncounterApiResponseFactory = careEncounterApiResponseFactory;
		this.careEncounterListApiResponseFactory = careEncounterListApiResponseFactory;
		this.formatter = formatter;
	}

	@Nonnull
	@GET("/admin/care-encounters")
	@AuthenticationRequired
	@ReadReplica
	public ApiResponse careEncounters(@Nonnull @QueryParameter Optional<Integer> pageNumber,
															 @Nonnull @QueryParameter Optional<Integer> pageSize,
															 @Nonnull @QueryParameter Optional<LocalDate> startDate,
															 @Nonnull @QueryParameter Optional<LocalDate> endDate,
																				 @Nonnull @QueryParameter Optional<String> searchQuery,
																				 @Nonnull @QueryParameter Optional<CareEncounterStatusId> careEncounterStatusId,
																				 @Nonnull @QueryParameter Optional<CareEncounterAssignmentScopeId> careEncounterAssignmentScopeId,
																				 @Nonnull @QueryParameter Optional<CareEncounterSortColumnId> careEncounterSortColumnId,
																 @Nonnull @QueryParameter Optional<SortDirectionId> sortDirectionId) {
		requireNonNull(pageNumber);
		requireNonNull(pageSize);
		requireNonNull(startDate);
		requireNonNull(endDate);
		requireNonNull(searchQuery);
		requireNonNull(careEncounterStatusId);
		requireNonNull(careEncounterAssignmentScopeId);
		requireNonNull(careEncounterSortColumnId);
		requireNonNull(sortDirectionId);

		Account account = requireCareNavigatorAccount();
		FindCareEncountersRequest request = new FindCareEncountersRequest();
		request.setInstitutionId(account.getInstitutionId());
		request.setPageNumber(pageNumber.orElse(null));
		request.setPageSize(pageSize.orElse(null));
		request.setStartDate(startDate.orElse(null));
		request.setEndDate(endDate.orElse(null));
		request.setSearchQuery(searchQuery.orElse(null));
		request.setCareEncounterStatusId(careEncounterStatusId.orElse(null));
		request.setCareEncounterAssignmentScopeId(careEncounterAssignmentScopeId.orElse(CareEncounterAssignmentScopeId.ALL));
		request.setCareNavigatorAccountId(account.getAccountId());
		request.setCareEncounterSortColumnId(careEncounterSortColumnId.orElse(null));
		request.setSortDirectionId(sortDirectionId.orElse(null));

		FindResult<CareEncounter> result = getCareEncounterService().findCareEncounters(request);

		return new ApiResponse(new LinkedHashMap<String, Object>() {{
			put("totalCount", result.getTotalCount());
			put("totalCountDescription", getFormatter().formatNumber(result.getTotalCount()));
			put("careEncounters", result.getResults().stream()
						.map(getCareEncounterListApiResponseFactory()::create)
						.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/admin/care-encounters/{careEncounterId}")
	@AuthenticationRequired
	@ReadReplica
	public ApiResponse careEncounter(@Nonnull @PathParameter UUID careEncounterId) {
		requireNonNull(careEncounterId);

		Account account = requireCareNavigatorAccount();
		CareEncounter careEncounter = getCareEncounterService()
				.findCareEncounterByIdForInstitutionId(careEncounterId, account.getInstitutionId())
				.orElseThrow(NotFoundException::new);
		List<CareEncounter> otherCareEncounters = getCareEncounterService()
				.findOtherCareEncountersByAccountId(careEncounter.getAccountId(), careEncounter.getCareEncounterId(),
						account.getInstitutionId());

		return new ApiResponse(new LinkedHashMap<String, Object>() {{
			put("careEncounter", getCareEncounterApiResponseFactory().create(careEncounter));
			put("otherCareEncounters", otherCareEncounters.stream()
					.map(getCareEncounterListApiResponseFactory()::create)
					.collect(Collectors.toList()));
			put("otherCareEncountersTotalCount", otherCareEncounters.size());
			put("otherCareEncountersTotalCountDescription", getFormatter().formatNumber(otherCareEncounters.size()));
		}});
	}

	@Nonnull
	@POST("/admin/care-encounters")
	@AuthenticationRequired
	public ApiResponse createCareEncounter(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = requireCareNavigatorAccount();
		CreateCareEncounterRequest request = getRequestBodyParser().parse(requestBody, CreateCareEncounterRequest.class);
		request.setInstitutionId(account.getInstitutionId());
		request.setAccountId(account.getAccountId());
		CareEncounter careEncounter = getCareEncounterService().createCareEncounter(request);

		return new ApiResponse(Map.of("careEncounter", getCareEncounterApiResponseFactory().create(careEncounter)));
	}

	@Nonnull
	@PUT("/admin/care-encounters/{careEncounterId}")
	@AuthenticationRequired
	public ApiResponse updateCareEncounter(@Nonnull @PathParameter UUID careEncounterId,
																						 @Nonnull @RequestBody String requestBody) {
		requireNonNull(careEncounterId);
		requireNonNull(requestBody);

		Account account = requireCareNavigatorAccount();
		UpdateCareEncounterRequest request = getRequestBodyParser().parse(requestBody, UpdateCareEncounterRequest.class);
		request.setCareEncounterId(careEncounterId);
		request.setInstitutionId(account.getInstitutionId());
		request.setAccountId(account.getAccountId());
		CareEncounter careEncounter = getCareEncounterService().updateCareEncounter(request);

		return new ApiResponse(Map.of("careEncounter", getCareEncounterApiResponseFactory().create(careEncounter)));
	}

	@Nonnull
	@PUT("/admin/care-encounters/{careEncounterId}/close")
	@AuthenticationRequired
	public ApiResponse closeCareEncounter(@Nonnull @PathParameter UUID careEncounterId) {
		requireNonNull(careEncounterId);

		Account account = requireCareNavigatorAccount();
		CareEncounter careEncounter = getCareEncounterService().closeCareEncounter(
				careEncounterId,
				account.getInstitutionId(),
				account.getAccountId());

		return new ApiResponse(Map.of("careEncounter", getCareEncounterApiResponseFactory().create(careEncounter)));
	}

	@Nonnull
	@PUT("/admin/care-encounters/{careEncounterId}/assignment")
	@AuthenticationRequired
	public ApiResponse assignCareEncounter(@Nonnull @PathParameter UUID careEncounterId,
																				 @Nonnull @RequestBody String requestBody) {
		requireNonNull(careEncounterId);
		requireNonNull(requestBody);

		Account account = requireCareNavigatorAccount();
		AssignCareEncounterRequest request = getRequestBodyParser().parse(requestBody, AssignCareEncounterRequest.class);
		CareEncounter careEncounter = getCareEncounterService().assignCareEncounter(
				careEncounterId,
				account.getInstitutionId(),
				account.getAccountId(),
				request.getCareNavigatorAccountId());

		return new ApiResponse(Map.of("careEncounter", getCareEncounterApiResponseFactory().create(careEncounter)));
	}

	@Nonnull
	@GET("/admin/care-encounter-cancellation-reasons")
	@AuthenticationRequired
	@ReadReplica
	public ApiResponse careEncounterCancellationReasons() {
		requireCareNavigatorAccount();
		List<CareEncounterCancellationReason> cancellationReasons =
				getCareEncounterService().findCareEncounterCancellationReasons();

		return new ApiResponse(Map.of("careEncounterCancellationReasons", cancellationReasons.stream()
				.map(cancellationReason -> Map.of(
						"careEncounterCancellationReasonId", cancellationReason.getCareEncounterCancellationReasonId(),
						"description", cancellationReason.getDescription(),
						"displayOrder", cancellationReason.getDisplayOrder(),
						"freeformTextRequired", cancellationReason.getFreeformTextRequired()))
				.collect(Collectors.toList())));
	}

	@Nonnull
	@GET("/admin/care-encounter-attendance-statuses")
	@AuthenticationRequired
	@ReadReplica
	public ApiResponse careEncounterAttendanceStatuses() {
		requireCareNavigatorAccount();
		List<AttendanceStatus> attendanceStatuses = getCareEncounterService().findSelectableAttendanceStatuses();

		return new ApiResponse(Map.of("attendanceStatuses", attendanceStatuses.stream()
				.map(attendanceStatus -> Map.of(
						"attendanceStatusId", attendanceStatus.getAttendanceStatusId(),
						"description", attendanceStatus.getDescription()))
				.collect(Collectors.toList())));
	}

	@Nonnull
	@PUT("/admin/care-encounters/{careEncounterId}/appointments/{appointmentId}/attendance-status")
	@AuthenticationRequired
	public ApiResponse changeCareEncounterAppointmentAttendanceStatus(
			@Nonnull @PathParameter UUID careEncounterId,
			@Nonnull @PathParameter UUID appointmentId,
			@Nonnull @RequestBody String requestBody) {
		requireNonNull(careEncounterId);
		requireNonNull(appointmentId);
		requireNonNull(requestBody);

		Account account = requireCareNavigatorAccount();
		ChangeAppointmentAttendanceStatusRequest request = getRequestBodyParser().parse(
				requestBody, ChangeAppointmentAttendanceStatusRequest.class);
		request.setAppointmentId(appointmentId);
		request.setAccountId(account.getAccountId());

		AuditLog auditLog = new AuditLog();
		auditLog.setAccountId(account.getAccountId());
		auditLog.setAuditLogEventId(AuditLogEventId.APPOINTMENT_UPDATE);
		auditLog.setMessage(String.format("Change attendance status for Care Navigator appointment_id %s "
				+ "for care_encounter_id %s", appointmentId, careEncounterId));
		auditLog.setPayload(requestBody);
		getAuditLogService().audit(auditLog);

		CareEncounter careEncounter = getCareEncounterService().changeCareEncounterAppointmentAttendanceStatus(
				careEncounterId, account.getInstitutionId(), request);

		return new ApiResponse(Map.of("careEncounter", getCareEncounterApiResponseFactory().create(careEncounter)));
	}

	@Nonnull
	@PUT("/admin/care-encounters/{careEncounterId}/appointments/{appointmentId}/cancel")
	@AuthenticationRequired
	public ApiResponse cancelCareEncounterAppointment(@Nonnull @PathParameter UUID careEncounterId,
																						 @Nonnull @PathParameter UUID appointmentId,
																						 @Nonnull @RequestBody String requestBody) {
		requireNonNull(careEncounterId);
		requireNonNull(appointmentId);
		requireNonNull(requestBody);

		Account account = requireCareNavigatorAccount();
		CancelCareEncounterAppointmentRequest request = getRequestBodyParser().parse(
				requestBody, CancelCareEncounterAppointmentRequest.class);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.APPOINTMENT_CANCEL);

		AuditLog auditLog = new AuditLog();
		auditLog.setAccountId(account.getAccountId());
		auditLog.setAuditLogEventId(AuditLogEventId.APPOINTMENT_CANCEL);
		auditLog.setMessage(String.format("Cancel Care Navigator appointment_id %s for care_encounter_id %s",
				appointmentId, careEncounterId));
		auditLog.setPayload(requestBody);
		getAuditLogService().audit(auditLog);

		CareEncounter careEncounter = getCareEncounterService().cancelCareEncounterAppointment(
				careEncounterId,
				appointmentId,
				account.getInstitutionId(),
				account.getAccountId(),
				request);

		return new ApiResponse(Map.of("careEncounter", getCareEncounterApiResponseFactory().create(careEncounter)));
	}

	@Nonnull
	@PUT("/admin/care-encounters/{careEncounterId}/cancel")
	@AuthenticationRequired
	public ApiResponse cancelCareEncounter(@Nonnull @PathParameter UUID careEncounterId,
																			 @Nonnull @RequestBody String requestBody) {
		requireNonNull(careEncounterId);
		requireNonNull(requestBody);

		Account account = requireCareNavigatorAccount();
		CancelCareEncounterRequest request = getRequestBodyParser().parse(requestBody, CancelCareEncounterRequest.class);
		CareEncounter careEncounter = getCareEncounterService().cancelCareEncounter(
				careEncounterId,
				account.getInstitutionId(),
				account.getAccountId(),
				request);

		return new ApiResponse(Map.of("careEncounter", getCareEncounterApiResponseFactory().create(careEncounter)));
	}

	@Nonnull
	@DELETE("/admin/care-encounters/{careEncounterId}")
	@AuthenticationRequired
	public ApiResponse deleteCareEncounter(@Nonnull @PathParameter UUID careEncounterId) {
		requireNonNull(careEncounterId);

		Account account = requireCareNavigatorAccount();
		CareEncounter careEncounter = getCareEncounterService()
				.findCareEncounterByIdForInstitutionId(careEncounterId, account.getInstitutionId())
				.orElseThrow(NotFoundException::new);

		getCareEncounterService().deleteCareEncounter(careEncounter.getCareEncounterId(), account.getInstitutionId(), account.getAccountId());
		return new ApiResponse();
	}

	@Nonnull
	protected Account requireCareNavigatorAccount() {
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canManageCareEncounters(account))
			throw new AuthorizationException();

		return account;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected CareEncounterService getCareEncounterService() {
		return this.careEncounterService;
	}

	@Nonnull
	protected AuditLogService getAuditLogService() {
		return this.auditLogService;
	}

	@Nonnull
	protected SystemService getSystemService() {
		return this.systemService;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return this.authorizationService;
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return this.requestBodyParser;
	}

	@Nonnull
	protected CareEncounterApiResponseFactory getCareEncounterApiResponseFactory() {
		return this.careEncounterApiResponseFactory;
	}

	@Nonnull
	protected CareEncounterListApiResponseFactory getCareEncounterListApiResponseFactory() {
		return this.careEncounterListApiResponseFactory;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return this.formatter;
	}
}
