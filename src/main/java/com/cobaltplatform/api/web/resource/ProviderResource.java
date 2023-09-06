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

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.integration.epic.code.AppointmentParticipantStatusCode;
import com.cobaltplatform.api.integration.epic.code.AppointmentStatusCode;
import com.cobaltplatform.api.integration.epic.code.SlotStatusCode;
import com.cobaltplatform.api.model.api.request.ProviderFindRequest;
import com.cobaltplatform.api.model.api.request.ProviderFindRequest.ProviderFindAvailability;
import com.cobaltplatform.api.model.api.request.ProviderFindRequest.ProviderFindSupplement;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AppointmentApiResponse.AppointmentApiResponseSupplement;
import com.cobaltplatform.api.model.api.response.AppointmentTimeApiResponse.AppointmentTimeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AvailabilityTimeApiResponse.AvailabilityTimeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ClinicApiResponse.ClinicApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FeatureApiResponse.FeatureApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FilterApiResponse.FilterApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FollowupApiResponse.FollowupApiResponseFactory;
import com.cobaltplatform.api.model.api.response.FollowupApiResponse.FollowupApiResponseSupplement;
import com.cobaltplatform.api.model.api.response.ProviderApiResponse.ProviderApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ProviderApiResponse.ProviderApiResponseSupplement;
import com.cobaltplatform.api.model.api.response.ProviderCalendarApiResponse.ProviderCalendarApiResponseFactory;
import com.cobaltplatform.api.model.api.response.SpecialtyApiResponse.SpecialtyApiResponseFactory;
import com.cobaltplatform.api.model.api.response.SupportRoleApiResponse.SupportRoleApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TimeZoneApiResponse;
import com.cobaltplatform.api.model.api.response.TimeZoneApiResponse.TimeZoneApiResponseFactory;
import com.cobaltplatform.api.model.api.response.VisitTypeApiResponse.VisitTypeApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.AppointmentTime;
import com.cobaltplatform.api.model.db.AppointmentTime.AppointmentTimeId;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.Clinic;
import com.cobaltplatform.api.model.db.Feature;
import com.cobaltplatform.api.model.db.Feature.FeatureId;
import com.cobaltplatform.api.model.db.Filter;
import com.cobaltplatform.api.model.db.Followup;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PaymentType;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.Specialty;
import com.cobaltplatform.api.model.db.SupportRole;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.model.db.VisitType;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.ProviderCalendar;
import com.cobaltplatform.api.model.service.ProviderFind;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityDate;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityTime;
import com.cobaltplatform.api.service.AppointmentService;
import com.cobaltplatform.api.service.AssessmentScoringService;
import com.cobaltplatform.api.service.AssessmentService;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.AvailabilityService;
import com.cobaltplatform.api.service.ClinicService;
import com.cobaltplatform.api.service.FeatureService;
import com.cobaltplatform.api.service.FollowupService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.ProviderService;
import com.cobaltplatform.api.service.ScreeningService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.lokalized.Strings;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.RequestBody;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.AuthorizationException;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class ProviderResource {
	@Nonnull
	private final AssessmentService assessmentService;
	@Nonnull
	private final AssessmentScoringService assessmentScoringService;
	@Nonnull
	private final ProviderService providerService;
	@Nonnull
	private final AppointmentService appointmentService;
	@Nonnull
	private final ClinicService clinicService;
	@Nonnull
	private final FollowupService followupService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final AvailabilityService availabilityService;
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final ScreeningService screeningService;
	@Nonnull
	private final ProviderApiResponseFactory providerApiResponseFactory;
	@Nonnull
	private final ClinicApiResponseFactory clinicApiResponseFactory;
	@Nonnull
	private final AppointmentApiResponseFactory appointmentApiResponseFactory;
	@Nonnull
	private final AvailabilityTimeApiResponseFactory availabilityTimeApiResponseFactory;
	@Nonnull
	private final FollowupApiResponseFactory followupApiResponseFactory;
	@Nonnull
	private final TimeZoneApiResponseFactory timeZoneApiResponseFactory;
	@Nonnull
	private final SupportRoleApiResponseFactory supportRoleApiResponseFactory;
	@Nonnull
	private final SpecialtyApiResponseFactory specialtyApiResponseFactory;
	@Nonnull
	private final ProviderCalendarApiResponseFactory providerCalendarApiResponseFactory;
	@Nonnull
	private final VisitTypeApiResponseFactory visitTypeApiResponseFactory;
	@Nonnull
	private final javax.inject.Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final FeatureService featureService;
	@Nonnull
	private final FilterApiResponseFactory filterApiResponseFactory;
	@Nonnull
	private final AppointmentTimeApiResponseFactory appointmentTimeApiResponseFactory;
	@Nonnull
	private final FeatureApiResponseFactory featureApiResponseFactory;

	@Inject
	public ProviderResource(@Nonnull AssessmentService assessmentService,
													@Nonnull AssessmentScoringService assessmentScoringService,
													@Nonnull ProviderService providerService,
													@Nonnull AppointmentService appointmentService,
													@Nonnull ClinicService clinicService,
													@Nonnull FollowupService followupService,
													@Nonnull AuthorizationService authorizationService,
													@Nonnull AvailabilityService availabilityService,
													@Nonnull InstitutionService institutionService,
													@Nonnull ScreeningService screeningService,
													@Nonnull ProviderApiResponseFactory providerApiResponseFactory,
													@Nonnull ClinicApiResponseFactory clinicApiResponseFactory,
													@Nonnull AppointmentApiResponseFactory appointmentApiResponseFactory,
													@Nonnull AvailabilityTimeApiResponseFactory availabilityTimeApiResponseFactory,
													@Nonnull FollowupApiResponseFactory followupApiResponseFactory,
													@Nonnull TimeZoneApiResponseFactory timeZoneApiResponseFactory,
													@Nonnull SupportRoleApiResponseFactory supportRoleApiResponseFactory,
													@Nonnull SpecialtyApiResponseFactory specialtyApiResponseFactory,
													@Nonnull ProviderCalendarApiResponseFactory providerCalendarApiResponseFactory,
													@Nonnull VisitTypeApiResponseFactory visitTypeApiResponseFactory,
													@Nonnull javax.inject.Provider<CurrentContext> currentContextProvider,
													@Nonnull RequestBodyParser requestBodyParser,
													@Nonnull Formatter formatter,
													@Nonnull Strings strings,
													@Nonnull FeatureService featureService,
													@Nonnull FilterApiResponseFactory filterApiResponseFactory,
													@Nonnull AppointmentTimeApiResponseFactory appointmentTimeApiResponseFactory,
													@Nonnull FeatureApiResponseFactory featureApiResponseFactory) {
		requireNonNull(assessmentService);
		requireNonNull(assessmentScoringService);
		requireNonNull(providerService);
		requireNonNull(appointmentService);
		requireNonNull(clinicService);
		requireNonNull(followupService);
		requireNonNull(authorizationService);
		requireNonNull(availabilityService);
		requireNonNull(institutionService);
		requireNonNull(screeningService);
		requireNonNull(providerApiResponseFactory);
		requireNonNull(clinicApiResponseFactory);
		requireNonNull(appointmentApiResponseFactory);
		requireNonNull(availabilityTimeApiResponseFactory);
		requireNonNull(followupApiResponseFactory);
		requireNonNull(timeZoneApiResponseFactory);
		requireNonNull(supportRoleApiResponseFactory);
		requireNonNull(specialtyApiResponseFactory);
		requireNonNull(providerCalendarApiResponseFactory);
		requireNonNull(visitTypeApiResponseFactory);
		requireNonNull(currentContextProvider);
		requireNonNull(requestBodyParser);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(featureService);
		requireNonNull(filterApiResponseFactory);
		requireNonNull(appointmentTimeApiResponseFactory);
		requireNonNull(featureApiResponseFactory);

		this.assessmentService = assessmentService;
		this.assessmentScoringService = assessmentScoringService;
		this.providerService = providerService;
		this.appointmentService = appointmentService;
		this.clinicService = clinicService;
		this.followupService = followupService;
		this.authorizationService = authorizationService;
		this.availabilityService = availabilityService;
		this.institutionService = institutionService;
		this.screeningService = screeningService;
		this.providerApiResponseFactory = providerApiResponseFactory;
		this.clinicApiResponseFactory = clinicApiResponseFactory;
		this.appointmentApiResponseFactory = appointmentApiResponseFactory;
		this.availabilityTimeApiResponseFactory = availabilityTimeApiResponseFactory;
		this.followupApiResponseFactory = followupApiResponseFactory;
		this.timeZoneApiResponseFactory = timeZoneApiResponseFactory;
		this.supportRoleApiResponseFactory = supportRoleApiResponseFactory;
		this.specialtyApiResponseFactory = specialtyApiResponseFactory;
		this.providerCalendarApiResponseFactory = providerCalendarApiResponseFactory;
		this.visitTypeApiResponseFactory = visitTypeApiResponseFactory;
		this.currentContextProvider = currentContextProvider;
		this.requestBodyParser = requestBodyParser;
		this.formatter = formatter;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
		this.featureService = featureService;
		this.filterApiResponseFactory = filterApiResponseFactory;
		this.appointmentTimeApiResponseFactory = appointmentTimeApiResponseFactory;
		this.featureApiResponseFactory = featureApiResponseFactory;
	}

	@Nonnull
	@POST("/providers/find")
	@AuthenticationRequired
	public ApiResponse findProviders(@Nonnull @RequestBody String requestBody) {
		Account account = getCurrentContext().getAccount().get();
		Locale locale = getCurrentContext().getLocale();
		Institution institution = getInstitutionService().findInstitutionById(account.getInstitutionId()).get();
		int defaultNumberOfWeeksToSearch = 4;

		ProviderFindRequest request = getRequestBodyParser().parse(requestBody, ProviderFindRequest.class);
		request.setInstitutionId(institution.getInstitutionId());
		request.setIncludePastAvailability(false);

		if (request.getStartDate() != null && request.getEndDate() == null)
			request.setEndDate(request.getStartDate().plusWeeks(defaultNumberOfWeeksToSearch));

		Set<UUID> providerIds = new HashSet<>();
		Set<ProviderFindSupplement> supplements = request.getSupplements() == null ? Collections.emptySet() : request.getSupplements();

		// There is no longer a UI for this, so ignore legacy data
		// TODO: remove legacy time filtering options
		if (institution.getFeaturesEnabled()) {
			request.setStartTime(null);
			request.setEndTime(null);
		}

		// 1. Pull raw data
		List<AppointmentType> appointmentTypes = getAppointmentService().findAppointmentTypesByInstitutionId(institution.getInstitutionId());
		Map<UUID, AppointmentType> appointmentTypesById = appointmentTypes.stream()
				.collect(Collectors.toMap(appointmentType -> appointmentType.getAppointmentTypeId(), Function.identity()));

		List<ProviderFind> providerFinds = getProviderService().findProviders(request, account);

		// 2. Throw out results that don't fall within specified appointment time windows
		if (institution.getFeaturesEnabled()) {
			// Reference data
			Map<AppointmentTimeId, AppointmentTime> appointmentTimesById = getAppointmentService().findAppointmentTimes().stream()
					.collect(Collectors.toMap(AppointmentTime::getAppointmentTimeId, Function.identity()));

			// Get a list of provided appointment times
			List<AppointmentTime> appointmentTimes = request.getAppointmentTimeIds() == null ? List.of() : request.getAppointmentTimeIds().stream()
					.filter(appointmentTimeId -> appointmentTimeId != null)
					.map(appointmentTimeId -> appointmentTimesById.get(appointmentTimeId))
					.collect(Collectors.toList());

			boolean noAppointmentTimesSpecified = appointmentTimes.size() == 0;
			boolean allAppointmentTimesSpecified = appointmentTimes.size() == appointmentTimesById.size();
			boolean needToTakeAppointmentTimesIntoAccount = !(noAppointmentTimesSpecified || allAppointmentTimesSpecified);

			if (needToTakeAppointmentTimesIntoAccount) {
				for (ProviderFind providerFind : providerFinds) {
					List<AvailabilityDate> emptyDates = new ArrayList<>(providerFind.getDates().size());

					for (AvailabilityDate date : providerFind.getDates()) {
						List<AvailabilityTime> timesWithinRange = new ArrayList<>(date.getTimes().size());

						for (AvailabilityTime time : date.getTimes()) {
							boolean withinRange = false;

							for (AppointmentTime appointmentTime : appointmentTimes) {
								// If a slot's time is within range, keep it (start inclusive, end exclusive)
								if ((appointmentTime.getStartTime().isBefore(time.getTime()) || appointmentTime.getStartTime().equals(time.getTime()))
										&& appointmentTime.getEndTime().isAfter(time.getTime())) {
									withinRange = true;
									break;
								}
							}

							if (withinRange)
								timesWithinRange.add(time);
						}

						date.setTimes(timesWithinRange);

						if (timesWithinRange.size() == 0)
							emptyDates.add(date);
					}

					// If the filter resulted in any empty dates (no slots available), remove those dates entirely
					providerFind.getDates().removeAll(emptyDates);
				}
			}
		}

		// 3. Group by date
		SortedMap<LocalDate, List<ProviderFind>> providerFindsByDate = new TreeMap<>();

		for (ProviderFind providerFind : providerFinds) {
			providerIds.add(providerFind.getProviderId());

			for (AvailabilityDate availabilityDate : providerFind.getDates()) {
				List<ProviderFind> providerFindsForDate = providerFindsByDate.get(availabilityDate.getDate());

				if (providerFindsForDate == null) {
					providerFindsForDate = new ArrayList<>();
					providerFindsByDate.put(availabilityDate.getDate(), providerFindsForDate);
				}

				providerFindsForDate.add(providerFind);
			}
		}

		// 4. Insert empty lists for each missing date to fill in "holes" where there are no
		// results for that date (UI prefers to show "no providers available for this date" kind of message in that scenario)
		if (institution.getFeaturesEnabled()) {
			LocalDate startDate = request.getStartDate() == null ? LocalDate.now(account.getTimeZone()) : request.getStartDate();
			LocalDate endDate = request.getEndDate() == null ? startDate.plusWeeks(defaultNumberOfWeeksToSearch) : request.getEndDate();

			for (LocalDate currentDate = startDate;
					 currentDate.isBefore(endDate) || currentDate.isEqual(endDate);
					 currentDate = currentDate.plusDays(1)) {
				if (!providerFindsByDate.containsKey(currentDate))
					providerFindsByDate.put(currentDate, List.of());
			}
		}

		// 5. Walk grouped dates to prepare for response
		List<ProviderFindSection> sections = new ArrayList<>(providerFindsByDate.size());

		for (Entry<LocalDate, List<ProviderFind>> entry : providerFindsByDate.entrySet()) {
			LocalDate date = entry.getKey();
			List<ProviderFind> providerFindsForDate = entry.getValue();
			List<Object> normalizedProviderFinds = new ArrayList<>(providerFindsForDate.size());

			boolean allProvidersFullyBooked = true;

			for (ProviderFind providerFind : providerFindsForDate) {
				List<Object> normalizedTimes = new ArrayList<>(providerFindsForDate.size());

				boolean providerFullyBooked = false;

				for (AvailabilityDate availabilityDate : providerFind.getDates()) {
					if (availabilityDate.getDate().equals(date)) {
						for (AvailabilityTime availabilityTime : availabilityDate.getTimes()) {
							String timeDescription = normalizeTimeFormat(formatter.formatTime(availabilityTime.getTime(), FormatStyle.SHORT), locale);

							boolean debugFhirSlotInformation = false;

							// Tack on a bunch of debugging information for FHIR slots, if enabled
							if (debugFhirSlotInformation) {
								if (providerFind.getSchedulingSystemId() == SchedulingSystemId.EPIC_FHIR) {
									List<AppointmentType> slotAppointmentTypes = availabilityTime.getAppointmentTypeIds().stream()
											.map(appointmentTypeId -> appointmentTypesById.get(appointmentTypeId))
											.collect(Collectors.toList());

									List<String> debugSupplements = new ArrayList<>(slotAppointmentTypes.size());

									for (AppointmentType slotAppointmentType : slotAppointmentTypes) {
										SlotStatusCode slotStatusCode = availabilityTime.getSlotStatusCodesByAppointmentTypeId().get(slotAppointmentType.getAppointmentTypeId());
										AppointmentStatusCode appointmentStatusCode = availabilityTime.getAppointmentStatusCodesByAppointmentTypeId().get(slotAppointmentType.getAppointmentTypeId());
										AppointmentParticipantStatusCode appointmentParticipantStatusCode = availabilityTime.getAppointmentParticipantStatusCodesByAppointmentTypeId().get(slotAppointmentType.getAppointmentTypeId());

										debugSupplements.add(format("(%s: [slot=%s, appointment=%s, participant=%s])", slotAppointmentType.getName(),
												(slotStatusCode == null ? null : slotStatusCode.getFhirValue()),
												(appointmentStatusCode == null ? null : appointmentStatusCode.getFhirValue()),
												(appointmentParticipantStatusCode == null ? null : appointmentParticipantStatusCode.getFhirValue())));
									}

									timeDescription = timeDescription + " " + debugSupplements.stream().collect(Collectors.joining(", "));
								}
							}

							Map<String, Object> normalizedTime = new LinkedHashMap<>();
							normalizedTime.put("time", availabilityTime.getTime());
							normalizedTime.put("timeDescription", timeDescription);
							normalizedTime.put("status", availabilityTime.getStatus());
							normalizedTime.put("epicDepartmentId", availabilityTime.getEpicDepartmentId());
							normalizedTime.put("epicAppointmentFhirId", availabilityTime.getEpicAppointmentFhirId());
							normalizedTime.put("appointmentTypeIds", availabilityTime.getAppointmentTypeIds());
							normalizedTime.put("slotStatusCodesByAppointmentTypeId", availabilityTime.getSlotStatusCodesByAppointmentTypeId());
							normalizedTime.put("appointmentStatusCodesByAppointmentTypeId", availabilityTime.getAppointmentStatusCodesByAppointmentTypeId());
							normalizedTime.put("appointmentParticipantStatusCodesByAppointmentTypeId", availabilityTime.getAppointmentParticipantStatusCodesByAppointmentTypeId());

							normalizedTimes.add(normalizedTime);
						}

						providerFullyBooked = availabilityDate.getFullyBooked();
						allProvidersFullyBooked = allProvidersFullyBooked && providerFullyBooked;
					}
				}

				Map<String, Object> normalizedProviderFind = new LinkedHashMap<>();
				normalizedProviderFind.put("providerId", providerFind.getProviderId());
				normalizedProviderFind.put("name", providerFind.getName());
				normalizedProviderFind.put("title", providerFind.getTitle());
				normalizedProviderFind.put("description", providerFind.getDescription());
				normalizedProviderFind.put("entity", providerFind.getEntity());
				normalizedProviderFind.put("clinic", providerFind.getClinic());
				normalizedProviderFind.put("license", providerFind.getLicense());
				normalizedProviderFind.put("specialty", providerFind.getSpecialty());
				normalizedProviderFind.put("supportRolesDescription", providerFind.getSupportRolesDescription());
				normalizedProviderFind.put("imageUrl", providerFind.getImageUrl());
				normalizedProviderFind.put("bioUrl", providerFind.getBioUrl());
				normalizedProviderFind.put("schedulingSystemId", providerFind.getSchedulingSystemId());
				normalizedProviderFind.put("phoneNumberRequiredForAppointment", providerFind.getPhoneNumberRequiredForAppointment());
				normalizedProviderFind.put("paymentFundingDescriptions", providerFind.getPaymentFundingDescriptions());
				normalizedProviderFind.put("fullyBooked", providerFullyBooked);
				normalizedProviderFind.put("treatmentDescription", providerFind.getTreatmentDescription());
				normalizedProviderFind.put("intakeAssessmentRequired", providerFind.getIntakeAssessmentRequired());
				normalizedProviderFind.put("intakeAssessmentIneligible", providerFind.getIntakeAssessmentIneligible());
				normalizedProviderFind.put("skipIntakePrompt", providerFind.getSkipIntakePrompt());
				normalizedProviderFind.put("appointmentTypeIds", providerFind.getAppointmentTypeIds());
				normalizedProviderFind.put("times", normalizedTimes);
				List<UUID> specialtyIds = providerFind.getSpecialties().stream()
						.map(specialty -> specialty.getSpecialtyId())
						.collect(Collectors.toList());
				normalizedProviderFind.put("specialtyIds", specialtyIds);
				normalizedProviderFind.put("displayPhoneNumberOnlyForBooking", providerFind.getDisplayPhoneNumberOnlyForBooking());
				normalizedProviderFind.put("phoneNumber", providerFind.getPhoneNumber());
				normalizedProviderFind.put("formattedPhoneNumber", getFormatter().formatPhoneNumber(providerFind.getPhoneNumber()));

				normalizedProviderFinds.add(normalizedProviderFind);
			}

			ProviderFindSection section = new ProviderFindSection();
			section.setDate(date);
			section.setDateDescription(getFormatter().formatDate(date, FormatStyle.FULL));
			section.setFullyBooked(allProvidersFullyBooked);
			section.setProviders(normalizedProviderFinds);

			sections.add(section);
		}

		// Extract distinct appointment types and epic department IDs from raw results
		Set<UUID> appointmentTypeIds = new HashSet<>();
		Set<UUID> epicDepartmentIds = new HashSet<>();

		for (ProviderFind providerFind : providerFinds)
			if (providerFind.getAppointmentTypeIds() != null)
				appointmentTypeIds.addAll(providerFind.getAppointmentTypeIds());

		List<Map<String, Object>> appointmentTypesJson = appointmentTypes.stream()
				.filter((appointmentType -> appointmentTypeIds.contains(appointmentType.getAppointmentTypeId())))
				.map((appointmentType -> {
					Map<String, Object> appointmentTypeJson = new LinkedHashMap<>();
					appointmentTypeJson.put("appointmentTypeId", appointmentType.getAppointmentTypeId());
					appointmentTypeJson.put("schedulingSystemId", appointmentType.getSchedulingSystemId());
					appointmentTypeJson.put("visitTypeId", appointmentType.getVisitTypeId());
					appointmentTypeJson.put("acuityAppointmentTypeId", appointmentType.getAcuityAppointmentTypeId());
					appointmentTypeJson.put("assessmentId", appointmentType.getAssessmentId());
					appointmentTypeJson.put("epicVisitTypeId", appointmentType.getEpicVisitTypeId());
					appointmentTypeJson.put("epicVisitTypeIdType", appointmentType.getEpicVisitTypeIdType());
					appointmentTypeJson.put("name", appointmentType.getName());
					appointmentTypeJson.put("description", appointmentType.getDescription());
					appointmentTypeJson.put("durationInMinutes", appointmentType.getDurationInMinutes());
					appointmentTypeJson.put("durationInMinutesDescription", getStrings().get("{{duration}} minutes", new HashMap<String, Object>() {{
						put("duration", appointmentType.getDurationInMinutes());
					}}));

					return appointmentTypeJson;
				}))
				.collect(Collectors.toList());

		for (ProviderFind providerFind : providerFinds)
			if (providerFind.getEpicDepartmentIds() != null)
				epicDepartmentIds.addAll(providerFind.getEpicDepartmentIds());

		List<Map<String, Object>> epicDepartmentsJson = getAppointmentService().findEpicDepartmentsByInstitutionId(institution.getInstitutionId()).stream()
				.filter((epicDepartment -> epicDepartmentIds.contains(epicDepartment.getEpicDepartmentId())))
				.map((epicDepartment -> {
					Map<String, Object> epicDepartmentJson = new LinkedHashMap<>();
					epicDepartmentJson.put("epicDepartmentId", epicDepartment.getEpicDepartmentId());
					epicDepartmentJson.put("departmentId", epicDepartment.getDepartmentId());
					epicDepartmentJson.put("departmentIdType", epicDepartment.getDepartmentIdType());
					epicDepartmentJson.put("name", epicDepartment.getName());

					return epicDepartmentJson;
				}))
				.collect(Collectors.toList());

		// Pull out distinct specialties from the provider data
		Map<UUID, Specialty> specialtiesById = providerFinds.stream()
				.map(providerFind -> providerFind.getSpecialties())
				.filter(providerSpecialties -> providerSpecialties != null && providerSpecialties.size() > 0)
				.flatMap(Collection::stream)
				.collect(Collectors.toMap(Specialty::getSpecialtyId, Function.identity(), (existing, replacement) -> existing));

		List<Specialty> specialties = new ArrayList<>(specialtiesById.values());

		// If caller filters on clinics, return the clinics that were filtered on
		List<Clinic> clinics = new ArrayList<>();

		if (request.getClinicIds() != null && request.getClinicIds().size() > 0)
			clinics.addAll(getClinicService().findClinicsByInstitutionId(institution.getInstitutionId()).stream()
					.filter(clinic -> request.getClinicIds().contains(clinic.getClinicId()))
					.collect(Collectors.toList()));

		// Same for provider
		Provider provider = request.getProviderId() == null ? null : getProviderService().findProviderById(request.getProviderId()).orElse(null);

		// If appointments are specified and requestor has permission, pull them too
		List<Appointment> appointments = new ArrayList<>();
		boolean includeAppointments = supplements.contains(ProviderFindSupplement.APPOINTMENTS);

		if (includeAppointments) {
			for (UUID providerId : providerIds) {
				List<Appointment> providerAppointments = getAppointmentService().findActiveAppointmentsForProviderId(providerId, request.getStartDate(), request.getEndDate());
				appointments.addAll(providerAppointments);
			}
		}

		List<Appointment> sortedAppointments = appointments.stream()
				.sorted(Comparator
						.comparing(Appointment::getStartTime)
						.thenComparing(Appointment::getAccountId))
				.collect(Collectors.toList());

		// If followups are specified and requestor has permission, pull them too
		List<Followup> followups = new ArrayList<>();
		boolean includeFollowups = supplements.contains(ProviderFindSupplement.FOLLOWUPS);

		if (includeFollowups) {
			for (UUID providerId : providerIds) {
				List<Followup> providerFollowups = getFollowupService().findFollowupsByProviderId(providerId, request.getStartDate(), request.getEndDate());
				followups.addAll(providerFollowups);
			}
		}

		List<Followup> sortedFollowups = followups.stream()
				.sorted(Comparator
						.comparing(Followup::getFollowupDate)
						.thenComparing(Followup::getAccountId))
				.collect(Collectors.toList());

		// If there are "runs" of sections with no providers, collapse them into a single section
		List<ProviderFindSection> finalSections = new ArrayList<>(sections.size());

		// If there are not enough to collapse, use as-is
		if (sections.size() < 2) {
			finalSections.addAll(sections);
		} else {
			// There are enough sections to attempt collapsing empty sections.  Do that here
			ProviderFindSection firstEmptySectionInRange = null;

			for (ProviderFindSection section : sections) {
				List<Object> providers = section.getProviders();

				if (providers.size() > 0) {
					LocalDate previousDay = section.getDate().minusDays(1);

					if (firstEmptySectionInRange != null) {
						if (firstEmptySectionInRange.getDate().equals(previousDay)) {
							// Range of 1: put it in as-is
							finalSections.add(firstEmptySectionInRange);
						} else {
							// Range of > 1: make it a range
							firstEmptySectionInRange.setEndDate(previousDay);
							firstEmptySectionInRange.setDateDescription(format("%s - %s",
									getFormatter().formatDate(firstEmptySectionInRange.getDate(), FormatStyle.FULL), getFormatter().formatDate(previousDay, FormatStyle.FULL)));
							finalSections.add(firstEmptySectionInRange);
						}

						firstEmptySectionInRange = null;
					}

					finalSections.add(section);
				} else if (firstEmptySectionInRange == null) {
					firstEmptySectionInRange = section;
				}
			}

			// Catch any empty sections at the end of the list
			if (firstEmptySectionInRange != null) {
				LocalDate previousDay = sections.get(sections.size() - 1).getDate().minusDays(1);

				if (firstEmptySectionInRange.getDate().equals(previousDay)) {
					// Range of 1: put it in as-is
					finalSections.add(firstEmptySectionInRange);
				} else {
					// Range of > 1: make it a range
					firstEmptySectionInRange.setEndDate(previousDay);
					firstEmptySectionInRange.setDateDescription(format("%s - %s",
							getFormatter().formatDate(firstEmptySectionInRange.getDate(), FormatStyle.FULL), getFormatter().formatDate(previousDay, FormatStyle.FULL)));
					finalSections.add(firstEmptySectionInRange);
				}
			}
		}

		return new ApiResponse(new LinkedHashMap<String, Object>() {{
			put("sections", finalSections);
			put("appointmentTypes", appointmentTypesJson);
			put("epicDepartments", epicDepartmentsJson);

			if (provider != null)
				put("provider", getProviderApiResponseFactory().create(provider, ProviderApiResponseSupplement.PAYMENT_FUNDING));

			if (clinics.size() > 0)
				put("clinics", clinics.stream()
						.map(clinic -> getClinicApiResponseFactory().create(clinic))
						.collect(Collectors.toList()));

			if (specialties.size() > 0) {
				put("specialties", specialties.stream()
						.map(specialty -> getSpecialtyApiResponseFactory().create(specialty))
						.collect(Collectors.toList()));
				put("showSpecialties", true);
			} else
				put("showSpecialties", false);

			if (includeAppointments)
				put("appointments", sortedAppointments.stream()
						.map(appointment -> getAppointmentApiResponseFactory().create(appointment, Set.of(AppointmentApiResponseSupplement.ACCOUNT, AppointmentApiResponseSupplement.APPOINTMENT_REASON)))
						.collect(Collectors.toList()));

			if (includeFollowups)
				put("followups", sortedFollowups.stream()
						.map(followup -> getFollowupApiResponseFactory().create(followup, Set.of(FollowupApiResponseSupplement.ALL)))
						.collect(Collectors.toList()));
		}});
	}

	protected static class ProviderFindSection {
		@Nullable
		private LocalDate date;
		@Nullable
		private LocalDate endDate; // Used for empty ranges
		@Nullable
		private String dateDescription;
		@Nullable
		private Boolean fullyBooked;
		@Nullable
		private List<Object> providers;

		@Nullable
		public LocalDate getDate() {
			return this.date;
		}

		public void setDate(@Nullable LocalDate date) {
			this.date = date;
		}

		@Nullable
		public LocalDate getEndDate() {
			return this.endDate;
		}

		public void setEndDate(@Nullable LocalDate endDate) {
			this.endDate = endDate;
		}

		@Nullable
		public String getDateDescription() {
			return this.dateDescription;
		}

		public void setDateDescription(@Nullable String dateDescription) {
			this.dateDescription = dateDescription;
		}

		@Nullable
		public Boolean getFullyBooked() {
			return this.fullyBooked;
		}

		public void setFullyBooked(@Nullable Boolean fullyBooked) {
			this.fullyBooked = fullyBooked;
		}

		@Nullable
		public List<Object> getProviders() {
			return this.providers;
		}

		public void setProviders(@Nullable List<Object> providers) {
			this.providers = providers;
		}
	}

	@Nonnull
	protected String normalizeTimeFormat(@Nonnull String timeDescription,
																			 @Nonnull Locale locale) {
		requireNonNull(timeDescription);
		requireNonNull(locale);

		// Turns "10:00 AM" into "10:00am", for example
		return timeDescription.replace(" ", "").toLowerCase(locale);
	}

	@Nonnull
	@GET("/providers")
	@AuthenticationRequired
	public ApiResponse providers() {
		Account account = getCurrentContext().getAccount().get();
		List<Provider> providers = getProviderService().findProvidersByInstitutionId(account.getInstitutionId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("providers", providers.stream()
					.map((provider) -> getProviderApiResponseFactory().create(provider))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/providers/autocomplete")
	@AuthenticationRequired
	public ApiResponse autocompleteProviders(@Nonnull @QueryParameter Optional<String> query) {
		requireNonNull(query);

		Account account = getCurrentContext().getAccount().get();
		List<Provider> providers = getProviderService().findProvidersForAutocomplete(query.orElse(null), account.getInstitutionId());
		List<Clinic> clinics = getClinicService().findClinicsForAutocomplete(query.orElse(null), account.getInstitutionId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("providers", providers.stream()
					.map((provider) -> getProviderApiResponseFactory().create(provider))
					.collect(Collectors.toList()));
			put("clinics", clinics.stream()
					.map((clinic) -> getClinicApiResponseFactory().create(clinic))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/providers/recent")
	@AuthenticationRequired
	public ApiResponse recentProviders() {
		Account account = getCurrentContext().getAccount().get();
		List<Provider> providers = getProviderService().findRecentProvidersByAccountId(account.getAccountId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("providers", providers.stream()
					.map((provider) -> getProviderApiResponseFactory().create(provider))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/providers/lcsw")
	@AuthenticationRequired
	public ApiResponse lcswProviders() {
		Account account = getCurrentContext().getAccount().get();
		List<Provider> providers = getProviderService().findLcswProvidersByInstitutionId(account.getInstitutionId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("providers", providers.stream()
					.map((provider) -> getProviderApiResponseFactory().create(provider))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/providers/find-options")
	@AuthenticationRequired
	public ApiResponse providerFindOptions(@Nonnull @QueryParameter InstitutionId institutionId,
																				 @Nonnull @QueryParameter Optional<SupportRoleId> supportRoleId,
																				 @Nonnull @QueryParameter("startDate") Optional<LocalDate> startDateOverride,
																				 @Nonnull @QueryParameter("endDate") Optional<LocalDate> endDateOverride,
																				 @Nonnull @QueryParameter("clinicId") Optional<List<UUID>> clinicIdsOverride,
																				 @Nonnull @QueryParameter("visitTypeId") Optional<List<VisitTypeId>> visitTypeIdsOverride,
																				 @Nonnull @QueryParameter("featureId") Optional<FeatureId> featureId) {
		requireNonNull(supportRoleId);

		// You can force a psychiatrist (for example) - this is useful for "immediate links"
		SupportRoleId supportRoleIdOverride = supportRoleId.orElse(null);
		SupportRole overriddenSupportRole = null;

		Account account = getCurrentContext().getAccount().get();
		Institution institution = getInstitutionService().findInstitutionById(account.getInstitutionId()).get();

		List<SupportRole> allSupportRoles = getProviderService().findSupportRolesByInstitutionId(institutionId).stream()
				.collect(Collectors.toList());

		List<SupportRole> recommendedSupportRoles = getProviderService().findRecommendedSupportRolesForAccountId(account.getAccountId());
		List<SupportRole> defaultSupportRoles = new ArrayList<>(recommendedSupportRoles);

		Optional<Feature> feature = Optional.empty();
		List<Filter> filters = null;
		if (featureId.isPresent()) {
			feature = getFeatureService().findFeatureById(featureId.get());
			if (feature.isPresent())
				filters = getFeatureService().findFiltersByFeatureId(feature.get().getFeatureId());
		}

		if (defaultSupportRoles.size() == 0)
			defaultSupportRoles = allSupportRoles;

		if (supportRoleIdOverride != null) {
			for (SupportRole supportRole : allSupportRoles) {
				if (supportRole.getSupportRoleId().equals(supportRoleIdOverride)) {
					getLogger().debug("Overridding support role from query parameter: {}", supportRoleIdOverride);
					overriddenSupportRole = supportRole;
					break;
				}
			}
		}

		List<SupportRoleId> defaultSupportRoleIds = defaultSupportRoles.stream().map(supportRole -> supportRole.getSupportRoleId()).collect(Collectors.toList());
		List<PaymentType> paymentTypes = getProviderService().findPaymentTypes();
		List<Specialty> specialties = getProviderService().findSpecialtiesByInstitutionId(institutionId);

		Map<String, Object> availabilityAll = new LinkedHashMap<>();
		availabilityAll.put("availability", ProviderFindAvailability.ALL);
		availabilityAll.put("description", getStrings().get("Show All"));

		Map<String, Object> availabilityOnlyAvailable = new LinkedHashMap<>();
		availabilityOnlyAvailable.put("availability", ProviderFindAvailability.ONLY_AVAILABLE);
		availabilityOnlyAvailable.put("description", getStrings().get("Show Only Available"));

		List<Object> availabilities = List.of(availabilityAll, availabilityOnlyAvailable);

		LocalDate startDate = startDateOverride.orElse(LocalDate.now(getCurrentContext().getTimeZone()));
		LocalDate endDate = endDateOverride.orElse(startDate.plusWeeks(6));

		LocalTime startTime = LocalTime.of(6, 00);
		LocalTime endTime = LocalTime.of(20, 00);

		List<UUID> clinicIds = clinicIdsOverride.orElse(Collections.emptyList());

		String recommendation;
		String recommendationHtml;

		if (recommendedSupportRoles.size() == 0) {
			recommendation = getStrings().get("Our 1:1 resources are here to listen, support, and provide clinical care");
			recommendationHtml = recommendation;
		} else {
			String supportRoleDescription = overriddenSupportRole != null
					? overriddenSupportRole.getDescription()
					: recommendedSupportRoles.stream().map(supportRole -> supportRole.getDescription()).collect(Collectors.joining(" or "));

			// Hack for the moment until Lokalized supports "starts with vowel" functionality
			boolean startsWithVowel = "aeiou".indexOf(supportRoleDescription.toLowerCase(account.getLocale()).charAt(0)) != -1;

			if (startsWithVowel) {
				recommendation = getStrings().get("We recommend that you meet with an {{supportRoleDescription}}", new HashMap<String, Object>() {{
					put("supportRoleDescription", supportRoleDescription);
				}});
				recommendationHtml = getStrings().get("We <strong>recommend</strong> that you meet with an <strong>{{supportRoleDescription}}</strong>", new HashMap<String, Object>() {{
					put("supportRoleDescription", supportRoleDescription);
				}});
			} else {
				recommendation = getStrings().get("We recommend that you meet with a {{supportRoleDescription}}", new HashMap<String, Object>() {{
					put("supportRoleDescription", supportRoleDescription);
				}});
				recommendationHtml = getStrings().get("We <strong>recommend</strong> that you meet with a <strong>{{supportRoleDescription}}</strong>", new HashMap<String, Object>() {{
					put("supportRoleDescription", supportRoleDescription);
				}});
			}
		}

		// Psychiatrist filter should now read “Psychiatrist or Psych NP”
		for (SupportRole supportRole : allSupportRoles)
			if (supportRole.getSupportRoleId().equals(SupportRoleId.PSYCHIATRIST))
				supportRole.setDescription(getStrings().get("Psychiatrist or Psych NP"));

		List<VisitTypeId> visitTypeIds = new ArrayList<>(VisitTypeId.values().length);

		if (visitTypeIdsOverride.isPresent())
			visitTypeIds.addAll(visitTypeIdsOverride.get());
		else
			for (VisitTypeId visitTypeId : VisitTypeId.values())
				visitTypeIds.add(visitTypeId);

		List<VisitType> visitTypes = getAppointmentService().findVisitTypes();

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("defaultSupportRoleIds", defaultSupportRoleIds);
		response.put("defaultStartDate", startDate);
		response.put("defaultEndDate", endDate);
		response.put("defaultStartTime", startTime);
		response.put("defaultEndTime", endTime);
		response.put("defaultAvailability", ProviderFindAvailability.ALL);
		response.put("defaultClinicIds", clinicIds);
		response.put("defaultVisitTypeIds", visitTypeIds);
		response.put("recommendation", recommendation);
		response.put("recommendationHtml", recommendationHtml);
		response.put("recommendedSupportRoleIds", recommendedSupportRoles.stream()
				.map(supportRole -> supportRole.getSupportRoleId())
				.collect(Collectors.toList()));
		response.put("availabilities", availabilities);
		response.put("supportRoles", allSupportRoles);
		response.put("paymentTypes", paymentTypes);
		response.put("visitTypes", visitTypes.stream()
				.map(visitType -> getVisitTypeApiResponseFactory().create(visitType))
				.collect(Collectors.toList()));
		response.put("specialties", specialties.stream()
				.map(specialty -> getSpecialtyApiResponseFactory().create(specialty))
				.collect(Collectors.toList()));
		response.put("appointmentTimes", getAppointmentService().findAppointmentTimes().stream()
				.map(appointmentTime -> getAppointmentTimeApiResponseFactory().create(appointmentTime))
				.collect(Collectors.toList()));
		if (feature.isPresent()) {
			response.put("feature", getFeatureApiResponseFactory().create(feature.get()));
			if (filters != null)
				response.put("filters", filters.stream()
						.map(filter -> getFilterApiResponseFactory().create(filter))
						.collect(Collectors.toList()));
		}

		return new ApiResponse(response);
	}

	@Nonnull
	@GET("/providers/{providerId}")
	@AuthenticationRequired
	public ApiResponse provider(@Nonnull @PathParameter UUID providerId) {
		requireNonNull(providerId);

		Account account = getCurrentContext().getAccount().get();
		Provider provider = getProviderService().findProviderById(providerId).orElse(null);

		if (provider == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canViewProvider(provider, account))
			throw new AuthorizationException();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("provider", getProviderApiResponseFactory().create(provider, ProviderApiResponseSupplement.EVERYTHING));
		}});
	}

	@Nonnull
	@GET("/providers/lookup/time-zones")
	public ApiResponse lookupTimeZones() {
		Set<ZoneId> zoneIds = getProviderService().getProviderTimeZones();
		List<TimeZoneApiResponse> timeZones = new ArrayList<>(zoneIds.size());

		for (ZoneId zoneId : zoneIds)
			timeZones.add(getTimeZoneApiResponseFactory().create(zoneId));

		Collections.sort(timeZones, (tz1, tz2) -> {
			return tz1.getDescription().compareTo(tz2.getDescription());
		});

		return new ApiResponse(new HashMap<String, Object>() {{
			put("timeZones", timeZones);
		}});
	}

	@Nonnull
	@GET("/providers/lookup/support-roles")
	public ApiResponse lookupSupportRoles(@Nonnull @QueryParameter InstitutionId institutionId) {
		requireNonNull(institutionId);

		List<SupportRole> supportRoles = getProviderService().findSupportRolesByInstitutionId(institutionId);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("supportRoles", supportRoles.stream()
					.map(supportRole -> getSupportRoleApiResponseFactory().create(supportRole))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/providers/{providerId}/calendar")
	@AuthenticationRequired
	public ApiResponse providerCalendar(@Nonnull @PathParameter UUID providerId,
																			@Nonnull @QueryParameter LocalDate startDate,
																			@Nonnull @QueryParameter LocalDate endDate) {
		requireNonNull(providerId);
		requireNonNull(startDate);
		requireNonNull(endDate);

		Account account = getCurrentContext().getAccount().get();
		Provider provider = getProviderService().findProviderById(providerId).orElse(null);

		if (provider == null)
			throw new ValidationException(new ValidationException.FieldError("providerId", getStrings().get("Provider is invalid.")));

		if (!getAuthorizationService().canViewProviderCalendar(provider, account))
			throw new AuthorizationException();

		ProviderCalendar providerCalendar = getAvailabilityService().findProviderCalendar(providerId, startDate, endDate);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("providerCalendar", getProviderCalendarApiResponseFactory().create(providerCalendar));
		}});
	}

	@Nonnull
	protected AssessmentService getAssessmentService() {
		return this.assessmentService;
	}

	@Nonnull
	protected AssessmentScoringService getAssessmentScoringService() {
		return this.assessmentScoringService;
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return this.providerService;
	}

	@Nonnull
	protected ClinicService getClinicService() {
		return this.clinicService;
	}

	@Nonnull
	protected AppointmentService getAppointmentService() {
		return this.appointmentService;
	}

	@Nonnull
	protected AvailabilityService getAvailabilityService() {
		return this.availabilityService;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionService;
	}

	@Nonnull
	protected ScreeningService getScreeningService() {
		return this.screeningService;
	}

	@Nonnull
	protected ProviderApiResponseFactory getProviderApiResponseFactory() {
		return this.providerApiResponseFactory;
	}

	@Nonnull
	protected ClinicApiResponseFactory getClinicApiResponseFactory() {
		return this.clinicApiResponseFactory;
	}

	@Nonnull
	protected AppointmentApiResponseFactory getAppointmentApiResponseFactory() {
		return this.appointmentApiResponseFactory;
	}

	@Nonnull
	protected AvailabilityTimeApiResponseFactory getAvailabilityTimeApiResponseFactory() {
		return this.availabilityTimeApiResponseFactory;
	}

	@Nonnull
	protected TimeZoneApiResponseFactory getTimeZoneApiResponseFactory() {
		return this.timeZoneApiResponseFactory;
	}

	@Nonnull
	protected FollowupService getFollowupService() {
		return this.followupService;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return this.authorizationService;
	}

	@Nonnull
	protected FollowupApiResponseFactory getFollowupApiResponseFactory() {
		return this.followupApiResponseFactory;
	}

	@Nonnull
	protected SupportRoleApiResponseFactory getSupportRoleApiResponseFactory() {
		return this.supportRoleApiResponseFactory;
	}

	@Nonnull
	protected SpecialtyApiResponseFactory getSpecialtyApiResponseFactory() {
		return this.specialtyApiResponseFactory;
	}

	@Nonnull
	protected ProviderCalendarApiResponseFactory getProviderCalendarApiResponseFactory() {
		return this.providerCalendarApiResponseFactory;
	}

	@Nonnull
	protected VisitTypeApiResponseFactory getVisitTypeApiResponseFactory() {
		return this.visitTypeApiResponseFactory;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return this.requestBodyParser;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return this.formatter;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	protected FeatureService getFeatureService() {
		return this.featureService;
	}

	@Nonnull
	protected FilterApiResponseFactory getFilterApiResponseFactory() {
		return this.filterApiResponseFactory;
	}

	@Nonnull
	protected AppointmentTimeApiResponseFactory getAppointmentTimeApiResponseFactory() {
		return this.appointmentTimeApiResponseFactory;
	}

	@Nonnull
	protected FeatureApiResponseFactory getFeatureApiResponseFactory() {
		return this.featureApiResponseFactory;
	}
}