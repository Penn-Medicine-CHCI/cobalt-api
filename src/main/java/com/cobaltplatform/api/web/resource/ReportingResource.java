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

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.response.ReportTypeApiResponse;
import com.cobaltplatform.api.model.api.response.ReportTypeApiResponse.ReportTypeApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.GenderIdentity.GenderIdentityId;
import com.cobaltplatform.api.model.db.Race.RaceId;
import com.cobaltplatform.api.model.db.ReportType.ReportTypeId;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.ReportingService;
import com.cobaltplatform.api.util.db.ReadReplica;
import com.lokalized.Strings;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.response.ApiResponse;
import com.soklet.web.response.CustomResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class ReportingResource {
	@Nonnull
	private final ReportingService reportingService;
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final ReportTypeApiResponseFactory reportTypeApiResponseFactory;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Strings strings;

	@Inject
	public ReportingResource(@Nonnull ReportingService reportingService,
													 @Nonnull InstitutionService institutionService,
													 @Nonnull AuthorizationService authorizationService,
													 @Nonnull ReportTypeApiResponseFactory reportTypeApiResponseFactory,
													 @Nonnull Configuration configuration,
													 @Nonnull Provider<CurrentContext> currentContextProvider,
													 @Nonnull Strings strings) {
		requireNonNull(reportingService);
		requireNonNull(institutionService);
		requireNonNull(authorizationService);
		requireNonNull(reportTypeApiResponseFactory);
		requireNonNull(configuration);
		requireNonNull(currentContextProvider);
		requireNonNull(strings);

		this.reportingService = reportingService;
		this.institutionService = institutionService;
		this.authorizationService = authorizationService;
		this.reportTypeApiResponseFactory = reportTypeApiResponseFactory;
		this.configuration = configuration;
		this.currentContextProvider = currentContextProvider;
		this.strings = strings;
	}

	@Nonnull
	@GET("/reporting/report-types")
	@AuthenticationRequired
	@ReadReplica
	public ApiResponse reportTypes() {
		Account account = getCurrentContext().getAccount().get();
		List<ReportTypeApiResponse> reportTypes = getReportingService().findReportTypesAvailableForAccount(account).stream()
				.map(reportType -> getReportTypeApiResponseFactory().create(reportType))
				.collect(Collectors.toList());

		return new ApiResponse(new HashMap<>() {{
			put("reportTypes", reportTypes);
		}});
	}

	@Nonnull
	@GET("/reporting/run-report")
	@ReadReplica
	@AuthenticationRequired
	public Object runReport(@Nonnull @QueryParameter ReportTypeId reportTypeId,
													@Nonnull @QueryParameter("startDateTime") Optional<LocalDateTime> suppliedStartDateTime, // inclusive
													@Nonnull @QueryParameter("endDateTime") Optional<LocalDateTime> suppliedEndDateTime, // inclusive
													@Nonnull @QueryParameter Optional<List<String>> patientOrderInsurancePayorId,
													@Nonnull @QueryParameter Optional<List<String>> referringPracticeIds,
													@Nonnull @QueryParameter Optional<Integer> minimumPatientAge,
													@Nonnull @QueryParameter Optional<Integer> maximumPatientAge,
													@Nonnull @QueryParameter Optional<List<RaceId>> patientRaceId,
													@Nonnull @QueryParameter Optional<List<GenderIdentityId>> patientGenderIdentityId,
													@Nonnull @QueryParameter Optional<List<UUID>> panelAccountId,
													@Nonnull @QueryParameter Optional<UUID> groupSessionId,
													@Nonnull @QueryParameter Optional<ZoneId> timeZone,
													@Nonnull @QueryParameter Optional<Locale> locale,
													@Nonnull HttpServletResponse httpServletResponse) throws IOException {
		requireNonNull(reportTypeId);
		requireNonNull(suppliedStartDateTime);
		requireNonNull(suppliedEndDateTime);
		requireNonNull(patientOrderInsurancePayorId);
		requireNonNull(referringPracticeIds);
		requireNonNull(minimumPatientAge);
		requireNonNull(maximumPatientAge);
		requireNonNull(patientRaceId);
		requireNonNull(patientGenderIdentityId);
		requireNonNull(panelAccountId);
		requireNonNull(groupSessionId);
		requireNonNull(timeZone);
		requireNonNull(locale);
		requireNonNull(httpServletResponse);

		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canViewReportTypeId(account, reportTypeId))
			throw new AuthorizationException();

		LocalDateTime startDateTime = suppliedStartDateTime.orElse(null);
		LocalDateTime endDateTime = suppliedEndDateTime.orElse(null);

		ZoneId reportTimeZone = timeZone.orElse(account.getTimeZone());
		Locale reportLocale = locale.orElse(account.getLocale());

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", reportLocale);
		DateTimeFormatter instantFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss")
				.withLocale(reportLocale)
				.withZone(reportTimeZone);

		String filename = startDateTime != null & endDateTime != null ?
				format("Cobalt %s %s to %s.csv", reportTypeId.name(), dateTimeFormatter.format(startDateTime), dateTimeFormatter.format(endDateTime))
				: format("Cobalt %s on %s.csv", reportTypeId.name(), instantFormatter.format(Instant.now()));

		httpServletResponse.setContentType("text/csv");
		httpServletResponse.setHeader("Content-Encoding", "gzip");
		httpServletResponse.setHeader("Content-Disposition", format("attachment; filename=\"%s\"", filename));

		try (PrintWriter printWriter = new PrintWriter(new GZIPOutputStream(httpServletResponse.getOutputStream()))) {
			if (reportTypeId == ReportTypeId.PROVIDER_UNUSED_AVAILABILITY)
				getReportingService().runProviderUnusedAvailabilityReportCsv(account.getInstitutionId(), startDateTime, endDateTime, reportTimeZone, reportLocale, printWriter);
			else if (reportTypeId == ReportTypeId.PROVIDER_APPOINTMENTS)
				getReportingService().runProviderAppointmentsReportCsv(account.getInstitutionId(), startDateTime, endDateTime, reportTimeZone, reportLocale, printWriter);
			else if (reportTypeId == ReportTypeId.PROVIDER_APPOINTMENTS_EAP)
				getReportingService().runProviderAppointmentsEapReportCsv(account.getInstitutionId(), startDateTime, endDateTime, reportTimeZone, reportLocale, printWriter);
			else if (reportTypeId == ReportTypeId.PROVIDER_APPOINTMENT_CANCELATIONS)
				getReportingService().runProviderAppointmentCancelationsReportCsv(account.getInstitutionId(), startDateTime, endDateTime, reportTimeZone, reportLocale, printWriter);
			else if (reportTypeId == ReportTypeId.IC_PIPELINE)
				getReportingService().runIcPipelineReportCsv(account.getInstitutionId(), startDateTime, endDateTime, patientOrderInsurancePayorId, referringPracticeIds, minimumPatientAge, maximumPatientAge, patientRaceId, patientGenderIdentityId,
						reportTimeZone, reportLocale, printWriter);
			else if (reportTypeId == ReportTypeId.IC_OUTREACH)
				getReportingService().runIcOutreachReportCsv(account.getInstitutionId(), startDateTime, endDateTime, patientOrderInsurancePayorId, referringPracticeIds, minimumPatientAge, maximumPatientAge, patientRaceId, patientGenderIdentityId,
						panelAccountId, reportTimeZone, reportLocale, printWriter);
			else if (reportTypeId == ReportTypeId.IC_ASSESSMENT)
				getReportingService().runIcAssessmentReportCsv(account.getInstitutionId(), startDateTime, endDateTime, patientOrderInsurancePayorId, referringPracticeIds, minimumPatientAge, maximumPatientAge, patientRaceId, patientGenderIdentityId,
						reportTimeZone, reportLocale, printWriter);
			else if (reportTypeId == ReportTypeId.IC_SAFETY_PLANNING)
				getReportingService().runIcSafetyPlanningReportCsv(account.getInstitutionId(), startDateTime, endDateTime, reportTimeZone, reportLocale, printWriter);
			else if (reportTypeId == ReportTypeId.IC_TRIAGE)
				getReportingService().runIcTriageReportCsv(account.getInstitutionId(), startDateTime, endDateTime, reportTimeZone, reportLocale, printWriter);
			else if (reportTypeId == ReportTypeId.GROUP_SESSION_RESERVATION_EMAILS)
				getReportingService().runGroupSessionReservationEmailsReportCsv(groupSessionId.get(), reportTimeZone, reportLocale, printWriter);
			else
				throw new IllegalStateException(format("We don't support %s.%s yet", ReportTypeId.class.getSimpleName(), reportTypeId.name()));
		}

		return CustomResponse.instance();
	}

	@Nonnull
	protected ReportingService getReportingService() {
		return this.reportingService;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionService;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return this.authorizationService;
	}

	@Nonnull
	protected ReportTypeApiResponseFactory getReportTypeApiResponseFactory() {
		return this.reportTypeApiResponseFactory;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}
}