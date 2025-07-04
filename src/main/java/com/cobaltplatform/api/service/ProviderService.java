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

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.integration.acuity.AcuitySchedulingCache;
import com.cobaltplatform.api.integration.acuity.AcuitySchedulingClient;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.integration.epic.response.AppointmentFindFhirStu3Response;
import com.cobaltplatform.api.model.api.request.ProviderFindRequest;
import com.cobaltplatform.api.model.api.request.ProviderFindRequest.ProviderFindAvailability;
import com.cobaltplatform.api.model.api.request.ProviderFindRequest.ProviderFindLicenseType;
import com.cobaltplatform.api.model.api.request.ProviderFindRequest.ProviderFindSupplement;
import com.cobaltplatform.api.model.api.request.SynchronizeEpicProviderSlotBookingRequest;
import com.cobaltplatform.api.model.api.request.UpdateEpicDepartmentRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSession;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.AppointmentType;
import com.cobaltplatform.api.model.db.Assessment;
import com.cobaltplatform.api.model.db.BusinessHour;
import com.cobaltplatform.api.model.db.DepartmentAvailabilityStatus.DepartmentAvailabilityStatusId;
import com.cobaltplatform.api.model.db.EpicDepartment;
import com.cobaltplatform.api.model.db.EpicFhirAppointmentFindCache;
import com.cobaltplatform.api.model.db.EpicProviderSchedule;
import com.cobaltplatform.api.model.db.EpicProviderSlotBooking;
import com.cobaltplatform.api.model.db.Holiday;
import com.cobaltplatform.api.model.db.Holiday.HolidayId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Interaction;
import com.cobaltplatform.api.model.db.InteractionType;
import com.cobaltplatform.api.model.db.LogicalAvailability;
import com.cobaltplatform.api.model.db.LogicalAvailabilityType.LogicalAvailabilityTypeId;
import com.cobaltplatform.api.model.db.PaymentFunding;
import com.cobaltplatform.api.model.db.PaymentFunding.PaymentFundingId;
import com.cobaltplatform.api.model.db.PaymentType;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.ProviderAvailability;
import com.cobaltplatform.api.model.db.RecurrenceType.RecurrenceTypeId;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.Specialty;
import com.cobaltplatform.api.model.db.SupportRole;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.model.db.SystemAffinity.SystemAffinityId;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.cobaltplatform.api.model.service.AppointmentTypeWithLogicalAvailabilityId;
import com.cobaltplatform.api.model.service.AppointmentTypeWithProviderId;
import com.cobaltplatform.api.model.service.Availability;
import com.cobaltplatform.api.model.service.Block;
import com.cobaltplatform.api.model.service.ProviderFind;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityDate;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityStatus;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityTime;
import com.cobaltplatform.api.util.BusinessHoursCalculator;
import com.cobaltplatform.api.util.BusinessHoursCalculator.BusinessHours;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.google.common.collect.Range;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlInListPlaceholders;
import static com.cobaltplatform.api.util.DatabaseUtility.sqlVaragsParameters;
import static com.cobaltplatform.api.util.ValidationUtility.isValidUUID;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class ProviderService {
	@Nonnull
	private final javax.inject.Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final javax.inject.Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final javax.inject.Provider<AssessmentService> assessmentServiceProvider;
	@Nonnull
	private final javax.inject.Provider<ScreeningService> screeningServiceProvider;
	@Nonnull
	private final javax.inject.Provider<SessionService> sessionServiceProvider;
	@Nonnull
	private final javax.inject.Provider<AssessmentScoringService> assessmentScoringServiceProvider;
	@Nonnull
	private final javax.inject.Provider<ClinicService> clinicServiceProvider;
	@Nonnull
	private final javax.inject.Provider<AvailabilityService> availabilityServiceProvider;
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final AcuitySchedulingClient acuitySchedulingClient;
	@Nonnull
	private final AcuitySchedulingCache acuitySchedulingCache;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Set<ZoneId> providerTimeZones;
	@Nonnull
	private final Logger logger;

	@Inject
	public ProviderService(@Nonnull javax.inject.Provider<AccountService> accountServiceProvider,
												 @Nonnull javax.inject.Provider<InstitutionService> institutionServiceProvider,
												 @Nonnull javax.inject.Provider<AssessmentService> assessmentServiceProvider,
												 @Nonnull javax.inject.Provider<ScreeningService> screeningServiceProvider,
												 @Nonnull javax.inject.Provider<SessionService> sessionServiceProvider,
												 @Nonnull javax.inject.Provider<AssessmentScoringService> assessmentScoringServiceProvider,
												 @Nonnull javax.inject.Provider<ClinicService> clinicServiceProvider,
												 @Nonnull javax.inject.Provider<AvailabilityService> availabilityServiceProvider,
												 @Nonnull EnterprisePluginProvider enterprisePluginProvider,
												 @Nonnull DatabaseProvider databaseProvider,
												 @Nonnull Configuration configuration,
												 @Nonnull AcuitySchedulingClient acuitySchedulingClient,
												 @Nonnull AcuitySchedulingCache acuitySchedulingCache,
												 @Nonnull Strings strings) {
		requireNonNull(accountServiceProvider);
		requireNonNull(institutionServiceProvider);
		requireNonNull(assessmentServiceProvider);
		requireNonNull(screeningServiceProvider);
		requireNonNull(sessionServiceProvider);
		requireNonNull(assessmentScoringServiceProvider);
		requireNonNull(clinicServiceProvider);
		requireNonNull(availabilityServiceProvider);
		requireNonNull(enterprisePluginProvider);
		requireNonNull(databaseProvider);
		requireNonNull(configuration);
		requireNonNull(acuitySchedulingClient);
		requireNonNull(acuitySchedulingCache);
		requireNonNull(strings);

		this.accountServiceProvider = accountServiceProvider;
		this.institutionServiceProvider = institutionServiceProvider;
		this.assessmentServiceProvider = assessmentServiceProvider;
		this.screeningServiceProvider = screeningServiceProvider;
		this.sessionServiceProvider = sessionServiceProvider;
		this.assessmentScoringServiceProvider = assessmentScoringServiceProvider;
		this.clinicServiceProvider = clinicServiceProvider;
		this.availabilityServiceProvider = availabilityServiceProvider;
		this.enterprisePluginProvider = enterprisePluginProvider;
		this.databaseProvider = databaseProvider;
		this.configuration = configuration;
		this.acuitySchedulingClient = acuitySchedulingClient;
		this.acuitySchedulingCache = acuitySchedulingCache;
		this.strings = strings;
		this.providerTimeZones = Collections.unmodifiableSet(determineProviderTimeZones());
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<Provider> findProviderById(@Nullable UUID providerId) {
		if (providerId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM provider WHERE provider_id=?", Provider.class, providerId);
	}

	@Nonnull
	public Optional<Provider> findProviderByInstitutionIdAndUrlName(@Nullable InstitutionId institutionId,
																																	@Nullable String urlName) {
		if (institutionId == null || urlName == null)
			return Optional.empty();

		urlName = urlName.trim();

		return getDatabase().queryForObject("""
				SELECT *
				FROM provider
				WHERE institution_id=?
				AND url_name=?
				""", Provider.class, institutionId, urlName);
	}

	@Nonnull
	public Optional<Provider> findProviderByIdentifier(@Nullable Object providerIdentifier,
																										 @Nullable InstitutionId institutionId) {
		if (providerIdentifier == null || institutionId == null)
			return Optional.empty();

		if (providerIdentifier instanceof UUID)
			return findProviderById((UUID) providerIdentifier);

		if (providerIdentifier instanceof String) {
			String providerIdentifierAsString = (String) providerIdentifier;

			if (isValidUUID(providerIdentifierAsString))
				return findProviderById(UUID.fromString(providerIdentifierAsString));

			return findProviderByInstitutionIdAndUrlName(institutionId, providerIdentifierAsString);
		}

		return Optional.empty();
	}

	@Nonnull
	public Optional<Provider> findProviderByAcuityCalendarId(@Nullable Long acuityCalendarId) {
		if (acuityCalendarId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM provider WHERE acuity_calendar_id=?", Provider.class, acuityCalendarId);
	}

	@Nonnull
	public Optional<Provider> findProviderByInstitutionIdAndEpicProviderId(@Nullable InstitutionId institutionId,
																																				 @Nullable String epicProviderId) {
		if (institutionId == null || epicProviderId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM provider WHERE UPPER(epic_provider_id)=? AND UPPER(epic_provider_id_type) IN ('INTERNAL', 'EXTERNAL')",
				Provider.class, epicProviderId.trim().toUpperCase(Locale.US));
	}

	@Nonnull
	public List<Provider> findProvidersByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM provider WHERE institution_id=? AND active=TRUE ORDER BY name", Provider.class, institutionId);
	}

	@Nonnull
	public List<Provider> findProvidersByInstitutionIdAndSupportRole(@Nullable InstitutionId institutionId,
																																	 @Nullable SupportRoleId supportRoleId) {
		if (institutionId == null || supportRoleId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT p.* FROM provider p, provider_support_role psr " +
				"WHERE psr.provider_id=p.provider_id AND psr.support_role_id=? AND p.institution_id=? AND p.active=TRUE ORDER BY p.name", Provider.class, supportRoleId, institutionId);
	}

	@Nonnull
	public List<Provider> findProvidersForAutocomplete(@Nullable String query,
																										 @Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		query = trimToNull(query);

		if (query == null)
			return findProvidersByInstitutionId(institutionId);

		// TODO: fuzzy search, include other fields too
		return getDatabase().queryForList("SELECT * FROM provider " +
				"WHERE institution_id=? AND UPPER(name) LIKE UPPER(?) AND active=TRUE ORDER BY name", Provider.class, institutionId, "%" + query + "%");
	}

	@Nonnull
	public List<Provider> findRecentProvidersByAccountId(@Nullable UUID accountId) {
		if (accountId == null)
			return Collections.emptyList();

		List<Provider> providers = getDatabase().queryForList("SELECT p.* FROM provider p, appointment a " +
				"WHERE p.provider_id=a.provider_id AND a.account_id=? AND p.active=TRUE ORDER BY a.start_time DESC, p.name LIMIT 5", Provider.class, accountId);

		// "a.start_time DESC" needs to be in the FROM with the DISTINCT clause, so we just do it manually...not great but good enough
		Set<UUID> providerIds = new HashSet<>();
		List<Provider> distinctProviders = new ArrayList<>(providers.size());

		for (Provider provider : providers) {
			if (!providerIds.contains(provider.getProviderId())) {
				providerIds.add(provider.getProviderId());
				distinctProviders.add(provider);
			}
		}

		return distinctProviders;
	}

	@Nonnull
	public List<Provider> findLcswProvidersByInstitutionId(@Nullable InstitutionId institutionId) {
		StringBuilder sql = new StringBuilder("SELECT * FROM provider WHERE license LIKE '%LCSW%' ");
		List<Object> parameters = new ArrayList<>(1);

		if (institutionId != null) {
			sql.append("AND institution_id=? ");
			parameters.add(institutionId);
		}

		sql.append("ORDER BY name");

		return getDatabase().queryForList(sql.toString(), Provider.class, sqlVaragsParameters(parameters));
	}

	@Nonnull
	public List<Provider> findProvidersForInstitutionWithSchedulingSystemId(@Nullable InstitutionId institutionId,
																																					@Nullable SchedulingSystemId schedulingSystemId) {
		if (institutionId == null)
			return Collections.emptyList();

		if (schedulingSystemId == null)
			return findProvidersByInstitutionId(institutionId);

		return getDatabase().queryForList("""
				SELECT * FROM provider
				WHERE institution_id=?
				AND scheduling_system_id=?
				AND active=TRUE
				ORDER BY name
				""", Provider.class, institutionId, SchedulingSystemId.EPIC_FHIR);
	}

	@Nonnull
	public List<SupportRole> findRecommendedSupportRolesForAccountId(@Nullable UUID accountId) {
		if (accountId == null)
			return List.of();

		Account account = getAccountService().findAccountById(accountId).orElse(null);

		if (account == null)
			return List.of();

		Institution institution = getInstitutionService().findInstitutionById(account.getInstitutionId()).get();

		// Check all the screenings that might result in recommended support roles (just feature and provider triage atm).
		// This list might include duplicates - they will be filtered out before returning
		List<SupportRole> allRecommendedSupportRoles = new ArrayList<>();

		if (institution.getFeatureScreeningFlowId() != null)
			allRecommendedSupportRoles.addAll(getScreeningService().findRecommendedSupportRolesByAccountId(accountId, institution.getFeatureScreeningFlowId()));

		if (institution.getProviderTriageScreeningFlowId() != null)
			allRecommendedSupportRoles.addAll(getScreeningService().findRecommendedSupportRolesByAccountId(accountId, institution.getProviderTriageScreeningFlowId()));

		Set<SupportRoleId> uniqueSupportRoleIds = new HashSet<>(SupportRoleId.values().length);
		List<SupportRole> uniqueRecommendedSupportRoles = new ArrayList<>(SupportRoleId.values().length);

		for (SupportRole supportRole : allRecommendedSupportRoles) {
			// If we have already included this support role, skip it
			if (uniqueSupportRoleIds.contains(supportRole.getSupportRoleId()))
				continue;

			uniqueRecommendedSupportRoles.add(supportRole);
			uniqueSupportRoleIds.add(supportRole.getSupportRoleId());
		}

		return uniqueRecommendedSupportRoles;
	}

	@Nonnull
	public Boolean doesAccountRequireMyChartConnectionForProviderBooking(@Nullable UUID accountId) {
		if (accountId == null)
			return false;

		Account account = getAccountService().findAccountById(accountId).orElse(null);

		if (account == null)
			return false;

		if (account.getEpicPatientFhirId() != null)
			return false;

		Institution institution = getInstitutionService().findInstitutionById(account.getInstitutionId()).get();
		return institution.getEpicFhirEnabled();
	}

	@Nonnull
	public List<ProviderFind> findProviders(@Nonnull ProviderFindRequest request,
																					@Nonnull Account account) {
		requireNonNull(request);
		requireNonNull(account);

		InstitutionId institutionId = request.getInstitutionId();
		UUID providerId = request.getProviderId();
		LocalDate startDate = request.getStartDate();
		LocalDate endDate = request.getEndDate();
		Set<DayOfWeek> daysOfWeek = request.getDaysOfWeek() == null ? Collections.emptySet() : request.getDaysOfWeek();
		LocalTime startTime = request.getStartTime();
		LocalTime endTime = request.getEndTime();
		ProviderFindAvailability availability = request.getAvailability() == null ? ProviderFindAvailability.ALL : request.getAvailability();
		Set<SupportRoleId> supportRoleIds = request.getSupportRoleIds() == null ? Collections.emptySet() : request.getSupportRoleIds();
		Set<String> paymentTypeIds = request.getPaymentTypeIds() == null ? Collections.emptySet() : request.getPaymentTypeIds();
		Set<UUID> clinicIds = request.getClinicIds() == null ? Collections.emptySet() : request.getClinicIds();
		Set<VisitTypeId> visitTypeIds = request.getVisitTypeIds() == null ? Collections.emptySet() : request.getVisitTypeIds();
		Set<ProviderFindSupplement> supplements = request.getSupplements() == null ? Collections.emptySet() : request.getSupplements();
		Set<ProviderFindLicenseType> licenseTypes = request.getLicenseTypes() == null ? Collections.emptySet() : request.getLicenseTypes();
		SystemAffinityId systemAffinityId = request.getSystemAffinityId() == null ? SystemAffinityId.COBALT : request.getSystemAffinityId();
		Set<UUID> specialtyIds = request.getSpecialtyIds() == null ? Collections.emptySet() : request.getSpecialtyIds();
		boolean includePastAvailability = request.getIncludePastAvailability() == null ? false : request.getIncludePastAvailability();
		LocalDateTime currentDateTime = includePastAvailability ? LocalDateTime.of(startDate, startTime) : LocalDateTime.now(account.getTimeZone());
		LocalDate currentDate = currentDateTime.toLocalDate();
		UUID patientOrderId = request.getPatientOrderId();
		ValidationException validationException = new ValidationException();
		UUID institutionLocationId = request.getInstitutionLocationId();

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		List<BusinessHour> institutionBusinessHours = getInstitutionService().findBusinessHoursByInstitutionId(institutionId);
		// TODO: incoporate business hour overrides
		// List<BusinessHourOverride> institutionBusinessHourOverrides = getInstitutionService().findBusinessHourOverridesByInstitutionId(institutionId);
		Map<DayOfWeek, BusinessHours> businessHoursByDayOfWeek = new HashMap<>(7);
		Set<HolidayId> holidayIds = new HashSet<>(16);
		boolean shouldUseInstitutionBusinessHours = institutionBusinessHours.size() > 0;
		List<Provider> providers;

		// If we use custom institution business hours, prepare data for that here.
		if (shouldUseInstitutionBusinessHours) {
			for (BusinessHour businessHour : institutionBusinessHours)
				businessHoursByDayOfWeek.put(businessHour.getDayOfWeek(), BusinessHours.from(businessHour.getOpenTime(), businessHour.getCloseTime()));

			List<Holiday> institutionHolidays = getInstitutionService().findHolidaysByInstitutionId(institutionId);

			for (Holiday holiday : institutionHolidays)
				holidayIds.add(holiday.getHolidayId());
		}

		// If provider ID is specified or clinic IDs are specified, ignore the rest of the filters
		if (providerId != null) {
			providers = getDatabase().queryForList("SELECT * FROM provider WHERE provider_id=?", Provider.class, providerId);
		} else if (clinicIds.size() > 0) {
			// For now - clinics also trump other filter types
			List<Object> parameters = new ArrayList<>();
			parameters.addAll(clinicIds);

			providers = getDatabase().queryForList(format("""
						SELECT DISTINCT p.* FROM provider p, provider_clinic pc
						WHERE p.provider_id=pc.provider_id
						AND pc.clinic_id IN %s
						ORDER BY p.name  
					""", sqlInListPlaceholders(clinicIds)), Provider.class, parameters.toArray(new Object[]{}));
		} else {
			StringBuilder query = new StringBuilder();
			List<Object> parameters = new ArrayList<>();

			query.append("SELECT DISTINCT p.* FROM provider p");

			if (supportRoleIds.size() > 0)
				query.append(", provider_support_role psr");

			if (paymentTypeIds.size() > 0)
				query.append(", provider_payment_type ppt");

			if (clinicIds.size() > 0)
				query.append(", provider_clinic pc");

			if (visitTypeIds.size() > 0)
				query.append(", provider_appointment_type pat, v_appointment_type at");

			if (patientOrderId != null)
				query.append(", patient_order po");

			query.append(" WHERE p.institution_id=? AND p.active=TRUE");
			parameters.add(institutionId);

			if (supportRoleIds.size() > 0) {
				query.append(" AND psr.provider_id=p.provider_id AND psr.support_role_id IN ");
				query.append(sqlInListPlaceholders(supportRoleIds));

				parameters.addAll(supportRoleIds);
			}

			if (paymentTypeIds.size() > 0) {
				query.append(" AND ppt.provider_id=p.provider_id AND ppt.payment_type_id IN ");
				query.append(sqlInListPlaceholders(paymentTypeIds));

				parameters.addAll(paymentTypeIds);
			}

			if (clinicIds.size() > 0) {
				query.append(" AND pc.provider_id=p.provider_id AND pc.clinic_id IN ");
				query.append(sqlInListPlaceholders(clinicIds));

				parameters.addAll(clinicIds);
			}

			if (institutionLocationId != null) {
				query.append(" AND (p.provider_id IN (SELECT pil1.provider_id FROM provider_institution_location pil1 WHERE pil1.institution_location_id = ?)");
				query.append(" OR p.provider_id NOT IN (SELECT pil2.provider_id FROM provider_institution_location pil2))");
				parameters.add(institutionLocationId);
			}

			if (visitTypeIds.size() > 0) {
				query.append(" AND pat.provider_id=p.provider_id AND pat.appointment_type_id=at.appointment_type_id AND at.visit_type_id IN ");
				query.append(sqlInListPlaceholders(visitTypeIds));

				parameters.addAll(visitTypeIds);
			}

			if (licenseTypes.size() > 0) {
				query.append(" AND (");

				List<String> licenseTypeConditions = new ArrayList<>(licenseTypes.size());

				for (ProviderFindLicenseType licenseType : licenseTypes) {
					if (licenseType == ProviderFindLicenseType.LCSW) {
						licenseTypeConditions.add("UPPER(p.license) LIKE '%LCSW%'");
					} else {
						throw new UnsupportedOperationException(format("%s.%s is not supported yet", ProviderFindLicenseType.class.getSimpleName(), licenseType.name()));
					}
				}

				query.append(licenseTypeConditions.stream().collect(Collectors.joining(" OR ")));
				query.append(") ");
			}

			// If this is an IC order, only expose providers that are currently associated with the order's department.
			// If the order's department has a scheduling override department, use that one instead.
			if (patientOrderId != null) {
				UUID epicDepartmentIdForScheduling = getDatabase().queryForObject("""
						SELECT COALESCE(ed.scheduling_override_epic_department_id, ed.epic_department_id) AS epic_department_id_for_scheduling
						FROM epic_department ed, patient_order po
						WHERE ed.epic_department_id=po.epic_department_id
						AND po.patient_order_id=?
						""", UUID.class, patientOrderId).get();

				query.append("""
						AND p.provider_id IN (
						  SELECT ped.provider_id
						  FROM provider_epic_department ped
						  WHERE ped.epic_department_id=?
						)
						""");

				parameters.add(epicDepartmentIdForScheduling);
			}

			query.append(" AND p.system_affinity_id=? ");
			parameters.add(systemAffinityId);

			query.append(" ORDER BY p.name");

			providers = getDatabase().queryForList(query.toString(), Provider.class, parameters.toArray(new Object[]{}));

			if (getLogger().isTraceEnabled()) {
				getLogger().trace("Query: {}\nParameters: {}", query, parameters);
				getLogger().trace("Providers: {}", providers.stream().map(provider -> provider.getName()).collect(Collectors.toList()));
			}
		}

		// If we are pulling previous slots, discard any providers of EPIC_FHIR type because we can't ask Epic for that data
		if (includePastAvailability)
			providers = providers.stream()
					.filter(provider -> provider.getSchedulingSystemId() != SchedulingSystemId.EPIC_FHIR)
					.collect(Collectors.toList());

		if (providers.size() == 0)
			return Collections.emptyList();

		// Single query to pull in all specialties for all providers in the resultset
		Map<UUID, List<Specialty>> specialtiesByProviderId = specialtiesByProviderIdForInstitutionId(institutionId);

		// If specialties are specified, throw out any provider that doesn't match them
		if (specialtyIds.size() > 0) {
			providers = providers.stream()
					.filter(provider -> {
						List<Specialty> specialties = specialtiesByProviderId.get(provider.getProviderId());

						if (specialties == null)
							return false;

						for (Specialty specialty : specialties)
							if (specialtyIds.contains(specialty.getSpecialtyId()))
								return true;

						return false;
					})
					.collect(Collectors.toList());
		}

		List<ProviderIntakeAssessmentPrompt> providerIntakeAssessmentPrompts = getDatabase().queryForList("SELECT p.provider_id, c.show_intake_assessment_prompt " +
				"FROM provider p, clinic c, provider_clinic pc " +
				"WHERE p.institution_id=? AND pc.provider_id=p.provider_id and pc.clinic_id=c.clinic_id AND pc.primary_clinic=true", ProviderIntakeAssessmentPrompt.class, institutionId);

		Map<UUID, Boolean> showIntakeAssessmentPromptsByProviderId = new HashMap<>(providerIntakeAssessmentPrompts.size());

		for (ProviderIntakeAssessmentPrompt providerIntakeAssessmentPrompt : providerIntakeAssessmentPrompts)
			showIntakeAssessmentPromptsByProviderId.put(providerIntakeAssessmentPrompt.getProviderId(), providerIntakeAssessmentPrompt.getShowIntakeAssessmentPrompt());

		// Keep track of all roles broken out by provider so we don't have to do 1+n queries
		List<ProviderSupportRole> providerSupportRoles = getDatabase().queryForList("SELECT psr.provider_id, sr.support_role_id, sr.description as support_role_description " +
				"FROM provider_support_role psr, support_role sr WHERE sr.support_role_id=psr.support_role_id", ProviderSupportRole.class);

		Map<UUID, List<ProviderSupportRole>> providerSupportRolesByProviderId = new HashMap<>(providerSupportRoles.size());

		for (ProviderSupportRole providerSupportRole : providerSupportRoles) {
			List<ProviderSupportRole> supportRoles = providerSupportRolesByProviderId.get(providerSupportRole.getProviderId());

			if (supportRoles == null) {
				supportRoles = new ArrayList<>(2);
				providerSupportRolesByProviderId.put(providerSupportRole.getProviderId(), supportRoles);
			}

			supportRoles.add(providerSupportRole);
		}

		// Keep track of all therapist/other IDs so we can determine which resultset values need the title massaged
		Set<UUID> titleOverrideProviderIds = new HashSet<>(getDatabase().queryForList("SELECT DISTINCT p.provider_id FROM provider p, provider_support_role psr " +
				"WHERE psr.provider_id=p.provider_id and psr.support_role_id IN (?,?,?)", UUID.class, SupportRoleId.CLINICIAN, SupportRoleId.OTHER, SupportRoleId.CARE_MANAGER));

		// Keep track of all treatment descriptions by provider ID
		Map<UUID, List<String>> treatmentDescriptionsByProviderId = new HashMap<>();

		List<ProviderTreatmentDescription> providerTreatmentDescriptions = getDatabase().queryForList("SELECT pc.provider_id, c.treatment_description " +
				"FROM provider_clinic pc, clinic c WHERE c.clinic_id=pc.clinic_id AND c.treatment_description IS NOT NULL " +
				"ORDER BY c.treatment_description", ProviderTreatmentDescription.class);

		for (ProviderTreatmentDescription providerTreatmentDescription : providerTreatmentDescriptions) {
			List<String> treatmentDescriptions = treatmentDescriptionsByProviderId.get(providerTreatmentDescription.getProviderId());

			if (treatmentDescriptions == null) {
				treatmentDescriptions = new ArrayList<>();
				treatmentDescriptionsByProviderId.put(providerTreatmentDescription.getProviderId(), treatmentDescriptions);
			}

			treatmentDescriptions.add(providerTreatmentDescription.getTreatmentDescription());
		}

		// Keep track of payment fundings
		List<PaymentFunding> paymentFundings = findPaymentFundings();
		Map<PaymentFundingId, String> paymentFundingDescriptionsById = new HashMap<>(paymentFundings.size());

		for (PaymentFunding paymentFunding : paymentFundings)
			paymentFundingDescriptionsById.put(paymentFunding.getPaymentFundingId(), paymentFunding.getDescription());

		// Special case this text for UI
		paymentFundingDescriptionsById.put(PaymentFundingId.INSURANCE, getStrings().get("Takes Insurance"));

		// Keep track of all provider appointment types and epic departments
		Map<UUID, Set<UUID>> appointmentTypeIdsByProviderId = new HashMap<>();
		Map<UUID, Set<UUID>> epicDepartmentIdsByProviderId = new HashMap<>();

		StringBuilder providerAppointmentTypesQuery = new StringBuilder("SELECT pat.provider_id, pat.appointment_type_id " +
				"FROM provider_appointment_type pat, v_appointment_type at, provider p " +
				"WHERE pat.appointment_type_id=at.appointment_type_id AND pat.provider_id=p.provider_id AND p.institution_id=? ");

		List<Object> providerAppointmentTypesParameters = new ArrayList<>();
		providerAppointmentTypesParameters.add(institutionId);

		if (visitTypeIds.size() > 0) {
			providerAppointmentTypesQuery.append(" AND at.visit_type_id IN ");
			providerAppointmentTypesQuery.append(sqlInListPlaceholders(visitTypeIds));

			providerAppointmentTypesParameters.addAll(visitTypeIds);
		}

		providerAppointmentTypesQuery.append(" ORDER BY pat.display_order");

		List<ProviderAppointmentType> providerAppointmentTypes = getDatabase().queryForList(providerAppointmentTypesQuery.toString(), ProviderAppointmentType.class, providerAppointmentTypesParameters.toArray(new Object[]{}));

		for (ProviderAppointmentType providerAppointmentType : providerAppointmentTypes) {
			Set<UUID> appointmentTypeIds = appointmentTypeIdsByProviderId.get(providerAppointmentType.getProviderId());

			if (appointmentTypeIds == null) {
				appointmentTypeIds = new HashSet<>();
				appointmentTypeIdsByProviderId.put(providerAppointmentType.getProviderId(), appointmentTypeIds);
			}

			appointmentTypeIds.add(providerAppointmentType.getAppointmentTypeId());
		}

		List<ProviderFind> providerFinds = new ArrayList<>(providers.size());

		// Special handling for native scheduling: precalculate all logical availability/appointment type data for the
		// specified providers up-front so we can use it further down to build availability date/time slots
		LocalDateTime nativeSchedulingStartDateTime = currentDateTime;
		LocalDateTime nativeSchedulingEndDateTime = nativeSchedulingStartDateTime.plusMonths(1).toLocalDate().atStartOfDay(); /* arbitrarily cap at 1 month ahead */

		NativeSchedulingAvailabilityData nativeSchedulingAvailabilityData = loadNativeSchedulingAvailabilityData(institutionId,
				visitTypeIds, nativeSchedulingStartDateTime, nativeSchedulingEndDateTime);

		// Slightly different flow to pull availablity if we have Epic FHIR providers
		Map<UUID, List<AvailabilityDate>> availabilityDatesByEpicFhirProviderId = determineAvailabilityDatesByEpicFhirProviderIds(
				account, institution, providers, new AvailabilityDatesCommand() {{
					setVisitTypeIds(visitTypeIds);
					setStartDate(startDate);
					setStartTime(startTime);
					setEndDate(endDate);
					setEndTime(endTime);
					setCurrentDate(currentDate);
					setDaysOfWeek(daysOfWeek);
					setAvailability(availability);
				}});

		for (Provider provider : providers) {
			boolean intakeAssessmentRequired = false;
			boolean intakeAssessmentIneligible = false;

			Assessment intakeAssessment = getAssessmentService().findIntakeAssessmentByProviderId(provider.getProviderId(), null).orElse(null);

			if (intakeAssessment != null) {
				intakeAssessmentRequired = true;

				AccountSession accountSession = getSessionService()
						.findCurrentIntakeAssessmentForAccountAndProvider(account, provider.getProviderId(), null, true).orElse(null);

				if (accountSession != null)
					intakeAssessmentIneligible = !getAssessmentScoringService().isBookingAllowed(accountSession);
			}

			// Figure out the final set of availability dates/times for this provider
			AvailabilityDatesCommand datesCommand = new AvailabilityDatesCommand();
			datesCommand.setProvider(provider);
			datesCommand.setVisitTypeIds(visitTypeIds);
			datesCommand.setStartDate(startDate);
			datesCommand.setStartTime(startTime);
			datesCommand.setEndDate(endDate);
			datesCommand.setEndTime(endTime);
			datesCommand.setCurrentDate(currentDate);
			datesCommand.setDaysOfWeek(daysOfWeek);
			datesCommand.setAvailability(availability);
			datesCommand.setEpicDepartmentIds(Set.of());

			// For IC, we discard any availability slots that are not relevant for the order's Epic department
			if (patientOrderId != null) {
				UUID patientOrderEpicDepartmentId = getDatabase().queryForObject("""
									SELECT epic_department_id
									FROM patient_order
									WHERE patient_order_id=?
						""", UUID.class, patientOrderId).orElse(null);

				if (patientOrderEpicDepartmentId != null)
					datesCommand.setEpicDepartmentIds(Set.of(patientOrderEpicDepartmentId));
			}

			List<AvailabilityDate> dates = new ArrayList<>();

			// Different code path for Cobalt native scheduling: it creates slots based on logical_availability records.
			// Non-native scheduling (Acuity, EPIC) will use provider_availability records.
			// EPIC FHIR uses its own different path.
			// This is the heavy lifting for creating slots
			if (provider.getSchedulingSystemId() == SchedulingSystemId.COBALT) {
				dates.addAll(availabilityDatesForNativeScheduling(datesCommand, nativeSchedulingStartDateTime, nativeSchedulingEndDateTime, nativeSchedulingAvailabilityData));
			} else if (provider.getSchedulingSystemId() == SchedulingSystemId.EPIC_FHIR) {
				List<AvailabilityDate> epicFhirAvailabilityDates = availabilityDatesByEpicFhirProviderId.get(provider.getProviderId());
				dates.addAll(epicFhirAvailabilityDates == null ? List.of() : epicFhirAvailabilityDates);
			} else {
				dates.addAll(availabilityDatesForNonNativeScheduling(datesCommand));
			}

			// Pick out distinct EPIC department IDs by provider by reviewing the availability data
			Set<UUID> epicDepartmentIds = new HashSet<>();
			epicDepartmentIdsByProviderId.put(provider.getProviderId(), epicDepartmentIds);

			for (AvailabilityDate availabilityDate : dates) {
				for (AvailabilityTime availabilityTime : availabilityDate.getTimes()) {
					if (availabilityTime.getEpicDepartmentId() != null)
						epicDepartmentIds.add(availabilityTime.getEpicDepartmentId());
				}
			}

			// Finally, sort everything and add in "fullyBooked" flags to make it easier on UI
			Collections.sort(dates, (date1, date2) -> date1.getDate().compareTo(date2.getDate()));

			for (AvailabilityDate availabilityDate : dates) {
				Collections.sort(availabilityDate.getTimes(), (time1, time2) -> time1.getTime().compareTo(time2.getTime()));

				boolean fullyBooked = true;

				for (AvailabilityTime availabilityTime : availabilityDate.getTimes())
					Collections.sort(availabilityTime.getAppointmentTypeIds());

				for (AvailabilityTime availabilityTime : availabilityDate.getTimes()) {
					if (availabilityTime.getStatus() != AvailabilityStatus.BOOKED) {
						fullyBooked = false;
						break;
					}
				}

				availabilityDate.setFullyBooked(fullyBooked);
			}

			List<PaymentFunding> providerPaymentFundings = findPaymentFundingsByProviderId(provider.getProviderId());
			List<String> providerPaymentFundingDescriptions = providerPaymentFundings.stream()
					.map(paymentFunding -> paymentFundingDescriptionsById.get(paymentFunding.getPaymentFundingId()))
					.collect(Collectors.toList());

			ProviderFind providerFind = new ProviderFind();
			providerFind.setProviderId(provider.getProviderId());
			providerFind.setUrlName(provider.getUrlName());
			providerFind.setName(provider.getName());
			providerFind.setTitle(provider.getTitle());
			providerFind.setDescription(provider.getDescription());
			providerFind.setLicense(provider.getLicense());
			providerFind.setSpecialty(provider.getSpecialty());
			providerFind.setClinic(provider.getClinic());
			providerFind.setEntity(provider.getEntity());
			providerFind.setImageUrl(provider.getImageUrl());
			providerFind.setBioUrl(provider.getBioUrl());
			providerFind.setIntakeAssessmentRequired(intakeAssessmentRequired);
			providerFind.setIntakeAssessmentIneligible(intakeAssessmentIneligible);
			providerFind.setSchedulingSystemId(provider.getSchedulingSystemId());
			providerFind.setSpecialties(specialtiesByProviderId.getOrDefault(provider.getProviderId(), Collections.emptyList()));

			// If assessment is required, include a flag that says whether the user is prompted before taking it
			if (intakeAssessmentRequired)
				providerFind.setSkipIntakePrompt(!showIntakeAssessmentPromptsByProviderId.get(provider.getProviderId()));

			List<ProviderSupportRole> currentProviderSupportRoles = providerSupportRolesByProviderId.get(provider.getProviderId());

			if (currentProviderSupportRoles == null)
				currentProviderSupportRoles = List.of();

			List<String> supportRoleDescriptions = currentProviderSupportRoles.stream()
					.map(providerSupportRole -> providerSupportRole.getSupportRoleDescription())
					.collect(Collectors.toList());

			String supportRolesDescription = supportRoleDescriptions.stream().collect(Collectors.joining(", "));

			// If necessary, replace support roles with title (only if a provider has a single support role)
			if (provider.getTitle() != null && supportRoleDescriptions.size() == 1 && titleOverrideProviderIds.contains(provider.getProviderId()))
				supportRolesDescription = provider.getTitle();

			// Special case: resilience coaches should have their title be the support role description instead
			Set<SupportRoleId> currentProviderSupportRoleIds = currentProviderSupportRoles.stream()
					.map(providerSupportRole -> providerSupportRole.getSupportRoleId())
					.collect(Collectors.toSet());

			if (currentProviderSupportRoleIds.size() == 1 && currentProviderSupportRoleIds.contains(SupportRoleId.COACH))
				providerFind.setTitle(supportRolesDescription);

			providerFind.setSupportRolesDescription(supportRolesDescription);
			providerFind.setDates(dates);
			providerFind.setPaymentFundingDescriptions(providerPaymentFundingDescriptions);

			List<String> treatmentDescriptions = treatmentDescriptionsByProviderId.get(provider.getProviderId());
			String treatmentDescription = null;

			if (treatmentDescriptions != null && treatmentDescriptions.size() > 0)
				treatmentDescription = format("* %s", treatmentDescriptions.stream().collect(Collectors.joining(", ")));

			providerFind.setTreatmentDescription(treatmentDescription);

			providerFind.setAppointmentTypeIds(appointmentTypeIdsByProviderId.get(provider.getProviderId()));
			providerFind.setEpicDepartmentIds(epicDepartmentIdsByProviderId.get(provider.getProviderId()));
			providerFind.setPhoneNumber(provider.getPhoneNumber());
			providerFind.setDisplayPhoneNumberOnlyForBooking(provider.getDisplayPhoneNumberOnlyForBooking());

			providerFinds.add(providerFind);
		}

		// Special case for Cobalt-scheduled and EPIC (private API)-scheduled providers: filter out any availability that is
		// too early for the provider's configured "lead time" (normally 24 to 48 hours), taking into account business hours if available
		if (!includePastAvailability)
			filterProviderFindsBySchedulingLeadTime(providerFinds, providers, currentDateTime, businessHoursByDayOfWeek, holidayIds);

		filterProviderFindsByEpicRules(institution, providerFinds);

		return providerFinds;
	}

	// Discards availability if the provider
	protected void filterProviderFindsByEpicRules(@Nonnull Institution institution,
																								@Nonnull List<ProviderFind> providerFinds) {
		requireNonNull(institution);
		requireNonNull(providerFinds);

		if (!institution.getEpicProviderSlotBookingSyncEnabled())
			return;

		Set<UUID> epicProviderIds = providerFinds.stream()
				.filter(providerFind -> providerFind.getSchedulingSystemId() == SchedulingSystemId.EPIC)
				.map(providerFind -> providerFind.getProviderId())
				.collect(Collectors.toSet());

		if (epicProviderIds.size() == 0)
			return;

		List<Object> slotBookingParameters = new ArrayList<>();
		slotBookingParameters.addAll(epicProviderIds);
		slotBookingParameters.add(LocalDateTime.now(institution.getTimeZone()));

		List<EpicProviderSlotBooking> epicProviderSlotBookings = getDatabase().queryForList(format("""
				SELECT epsb.*
				FROM epic_provider_slot_booking epsb, provider p
				WHERE epsb.provider_id=p.provider_id
				AND p.provider_id IN %s
				AND epsb.start_date_time >= ?
				""", sqlInListPlaceholders(epicProviderIds)), EpicProviderSlotBooking.class, slotBookingParameters.toArray(new Object[]{}));

		List<EpicProviderSchedule> epicProviderSchedules = getInstitutionService().findEpicProviderSchedulesByInstitutionId(institution.getInstitutionId());

		// DB constraint ensures start/end time ranges are unique per-institution
		Map<Range<LocalTime>, EpicProviderSchedule> epicProviderSchedulesByTimeRange = epicProviderSchedules.stream()
				.collect(Collectors.toMap((eps) -> Range.closedOpen(eps.getStartTime(), eps.getEndTime()), Function.identity()));

		// Keep track of how many NPVs there are by the combination of schedule + date + provider
		Map<EpicProviderScheduleNpvCountKey, Integer> npvCountsByKey = new HashMap<>();

		for (EpicProviderSlotBooking epicProviderSlotBooking : epicProviderSlotBookings) {
			Range<LocalTime> slotBookingRange = Range.closedOpen(epicProviderSlotBooking.getStartDateTime().toLocalTime(), epicProviderSlotBooking.getEndDateTime().toLocalTime());

			for (EpicProviderSchedule epicProviderSchedule : epicProviderSchedules) {
				Range<LocalTime> scheduleRange = Range.closedOpen(epicProviderSchedule.getStartTime(), epicProviderSchedule.getEndTime());

				boolean rangesOverlap = slotBookingRange.isConnected(scheduleRange)
						&& !slotBookingRange.intersection(scheduleRange).isEmpty();

				if (!rangesOverlap)
					continue;

				EpicProviderScheduleNpvCountKey npvCountKey = new EpicProviderScheduleNpvCountKey(epicProviderSlotBooking.getProviderId(), epicProviderSlotBooking.getStartDateTime().toLocalDate(), epicProviderSchedule.getEpicProviderScheduleId());
				Integer npvCounts = npvCountsByKey.get(npvCountKey);

				if (npvCounts == null) {
					npvCounts = 0;
					npvCountsByKey.put(npvCountKey, 0);
				}

				Duration slotBookingDuration = Duration.between(epicProviderSlotBooking.getStartDateTime(), epicProviderSlotBooking.getEndDateTime());

				if (slotBookingDuration.toMinutes() == epicProviderSchedule.getNpvDurationInMinutes().longValue()) {
					++npvCounts;
					npvCountsByKey.put(npvCountKey, npvCounts);
				}
			}
		}

		// Walk the list of availability.  If we find any slots that exceed the configured NPV maximum limits for a particular
		// schedule, mark them as "booked".
		for (ProviderFind providerFind : providerFinds) {
			for (AvailabilityDate date : providerFind.getDates()) {
				for (AvailabilityTime availabilityTime : date.getTimes()) {
					// Arbitrarily using 1 minute for the time range because duration is irrelevant, we just need to know if the range intersects with the schedule
					Range<LocalTime> availabilityTimeRange = Range.closedOpen(availabilityTime.getTime(), availabilityTime.getTime().plusMinutes(1L));

					for (Entry<Range<LocalTime>, EpicProviderSchedule> entry : epicProviderSchedulesByTimeRange.entrySet()) {
						Range<LocalTime> scheduleTimeRange = entry.getKey();
						EpicProviderSchedule epicProviderSchedule = entry.getValue();

						boolean rangesOverlap = availabilityTimeRange.isConnected(scheduleTimeRange)
								&& !availabilityTimeRange.intersection(scheduleTimeRange).isEmpty();

						if (rangesOverlap) {
							// Pull NPV counts for this schedule
							EpicProviderScheduleNpvCountKey npvCountKey = new EpicProviderScheduleNpvCountKey(providerFind.getProviderId(), date.getDate(), epicProviderSchedule.getEpicProviderScheduleId());
							Integer npvCount = npvCountsByKey.get(npvCountKey);

							if (npvCount == null)
								npvCount = 0;

							// If the NPV count exceeds the configured limit, don't let the slot be booked
							if (npvCount >= epicProviderSchedule.getMaximumNpvCount()) {
								getLogger().debug("NPV count for provider ID {} is {} for '{}' schedule, marking {} {} slot as 'booked'", providerFind.getProviderId(),
										npvCount, epicProviderSchedule.getName(), date.getDate(), availabilityTime.getTime());

								availabilityTime.setStatus(AvailabilityStatus.BOOKED);
							}
						}
					}
				}
			}
		}
	}

	// We count NPVs per-schedule using the combination of these 3 things as a key.
	protected static class EpicProviderScheduleNpvCountKey {
		@Nonnull
		private final UUID providerId;
		@Nonnull
		private final LocalDate date;
		@Nonnull
		private final UUID epicProviderScheduleId;

		public EpicProviderScheduleNpvCountKey(
				@Nonnull UUID providerId,
				@Nonnull LocalDate date,
				@Nonnull UUID epicProviderScheduleId
		) {
			requireNonNull(providerId);
			requireNonNull(date);
			requireNonNull(epicProviderScheduleId);

			this.providerId = providerId;
			this.date = date;
			this.epicProviderScheduleId = epicProviderScheduleId;
		}

		@Override
		public String toString() {
			return format("%s{providerId=%s, date=%s, epicProviderScheduleId=%s}", getClass().getSimpleName(),
					getProviderId(), getDate(), getEpicProviderScheduleId());
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other)
				return true;

			if (other == null || !getClass().equals(other.getClass()))
				return false;

			EpicProviderScheduleNpvCountKey otherNpvCountKey = (EpicProviderScheduleNpvCountKey) other;

			return Objects.equals(getProviderId(), otherNpvCountKey.getProviderId())
					&& Objects.equals(getDate(), otherNpvCountKey.getDate())
					&& Objects.equals(getEpicProviderScheduleId(), otherNpvCountKey.getEpicProviderScheduleId());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getProviderId(), getDate(), getEpicProviderScheduleId());
		}

		@Nonnull
		public UUID getProviderId() {
			return this.providerId;
		}

		@Nonnull
		public LocalDate getDate() {
			return this.date;
		}

		@Nonnull
		public UUID getEpicProviderScheduleId() {
			return this.epicProviderScheduleId;
		}
	}

	@Nonnull
	protected Map<UUID, List<AvailabilityDate>> determineAvailabilityDatesByEpicFhirProviderIds(@Nonnull Account account,
																																															@Nonnull Institution institution,
																																															@Nonnull List<Provider> providers,
																																															@Nonnull AvailabilityDatesCommand command) {
		requireNonNull(account);
		requireNonNull(institution);
		requireNonNull(providers);
		requireNonNull(command);

		// The Epic FHIR flow for pulling slots is totally different than our other mechanisms, so we special-case here
		List<Provider> providersThatUseEpicFhir = providers.stream()
				.filter(provider -> provider.getSchedulingSystemId() == SchedulingSystemId.EPIC_FHIR)
				.collect(Collectors.toList());

		if (providersThatUseEpicFhir.size() == 0)
			return Map.of();

		Map<UUID, Map<LocalDate, AvailabilityDate>> availabilityDatesByDateByProviderId = new HashMap<>(providersThatUseEpicFhir.size());

		// Prime the map with empty values
		for (Provider provider : providersThatUseEpicFhir)
			availabilityDatesByDateByProviderId.put(provider.getProviderId(), new HashMap<>());

		if (providersThatUseEpicFhir.size() > 0) {
			// If an institution has FHIR providers that match the search criteria and the requesting account doesn't have
			// a patient FHIR ID, then bubble out an error indicating that via special metadata.
			// This way FE can take action (route through MyChart flow etc.)
			if (account.getEpicPatientFhirId() == null) {
				ValidationException myChartValidationException = new ValidationException();
				myChartValidationException.add(getStrings().get("You need to connect to {{myChartName}} before accessing these healthcare providers.",
						Map.of("myChartName", institution.getMyChartName())));
				myChartValidationException.setMetadata(Map.of("patientFhirIdRequired", true));
				throw myChartValidationException;
			}

			Map<String, Provider> providersByEpicPractitionerFhirId = providersThatUseEpicFhir.stream()
					.collect(Collectors.toMap(Provider::getEpicPractitionerFhirId, Function.identity()));

			// Pick out all the Epic FHIR appointment types for this institution
			List<AppointmentType> appointmentTypes = getDatabase().queryForList("""
								SELECT DISTINCT at.*
								FROM appointment_type at, provider_appointment_type pat, provider p
								WHERE pat.provider_id=p.provider_id
								AND pat.appointment_type_id=at.appointment_type_id
								AND p.active=TRUE
								AND p.institution_id=?
								AND p.scheduling_system_id=?
					""", AppointmentType.class, institution.getInstitutionId(), SchedulingSystemId.EPIC_FHIR);

			Map<UUID, AppointmentType> appointmentTypesById = appointmentTypes.stream()
					.collect(Collectors.toMap(AppointmentType::getAppointmentTypeId, Function.identity()));

			List<ProviderAppointmentType> providerAppointmentTypes = getDatabase().queryForList("""
								SELECT pat.*
								FROM provider_appointment_type pat, provider p
								WHERE pat.provider_id=p.provider_id
								AND p.active=TRUE
								AND p.institution_id=?
								AND p.scheduling_system_id=?
					""", ProviderAppointmentType.class, institution.getInstitutionId(), SchedulingSystemId.EPIC_FHIR);

			Map<UUID, List<AppointmentType>> appointmentTypesByProviderId = new HashMap<>(appointmentTypes.size());

			for (ProviderAppointmentType providerAppointmentType : providerAppointmentTypes) {
				List<AppointmentType> currentAppointmentTypes = appointmentTypesByProviderId.get(providerAppointmentType.getProviderId());

				if (currentAppointmentTypes == null) {
					currentAppointmentTypes = new ArrayList<>();
					appointmentTypesByProviderId.put(providerAppointmentType.getProviderId(), currentAppointmentTypes);
				}

				currentAppointmentTypes.add(appointmentTypesById.get(providerAppointmentType.getAppointmentTypeId()));
			}

			LocalDate startDate = command.getStartDate() != null ? command.getStartDate() : LocalDate.now(account.getTimeZone());
			LocalDate endDate = command.getEndDate() != null ? command.getEndDate() : startDate.plusDays(90);

			// Instead of calling Epic FHIR directly to find appointments, look at our local cache of serialized responses.
			// Calling Epic over a date range like this for all providers would be prohibitively slow
			List<EpicFhirAppointmentFindCache> epicFhirAppointmentFindCaches = getDatabase().queryForList("""
					SELECT *
					FROM epic_fhir_appointment_find_cache
					WHERE institution_id=?
					AND date >= ?
					AND date <= ?
					""", EpicFhirAppointmentFindCache.class, institution.getInstitutionId(), startDate, endDate);

			// Deserialize the cached data into a list
			List<AppointmentFindFhirStu3Response> appointmentFindResponses = epicFhirAppointmentFindCaches.stream()
					.map(epicFhirAppointmentFindCache -> AppointmentFindFhirStu3Response.deserialize(epicFhirAppointmentFindCache.getApiResponse()))
					.collect(Collectors.toList());

			// Collect all of the entries for all dates into one big list of entries
			List<AppointmentFindFhirStu3Response.Entry> entries = appointmentFindResponses.stream()
					.map(appointmentFindFhirStu3Response -> appointmentFindFhirStu3Response.getEntry())
					.flatMap(List::stream)
					.collect(Collectors.toList());

			Set<AppointmentFindFhirStu3SlotKey> slotKeys = new HashSet<>(entries.size());

			for (AppointmentFindFhirStu3Response.Entry entry : entries) {
				if (!"Appointment".equals(entry.getResource().getResourceType()))
					continue;

				LocalDateTime slotStartDateTime = LocalDateTime.ofInstant(entry.getResource().getStart(), institution.getTimeZone());

				LocalDate slotDate = slotStartDateTime.toLocalDate();
				LocalTime slotTime = slotStartDateTime.toLocalTime();

				// Assumes single service type and coding
				if (entry.getResource().getServiceType().size() != 1)
					throw new IllegalStateException("Invalid size of serviceType element");

				if (entry.getResource().getServiceType().get(0).getCoding().size() != 1)
					throw new IllegalStateException("Invalid size of serviceType.coding element");

				AppointmentFindFhirStu3Response.Entry.Resource.ServiceType.Coding appointmentTypeCoding =
						entry.getResource().getServiceType().get(0).getCoding().get(0);

				// TODO: filter down slots based on passed-in criteria

				// Find any participant actor URLs that look like this and extract the Practitioner FHIR ID (xxx below):
				// $BASE_URL/FHIR/STU3/Practitioner/xxx
				Set<String> epicPractitionerFhirIds = entry.getResource().getParticipant().stream()
						.map(participant -> participant.getActor().getReference())
						.filter(actorReference -> actorReference.contains("/FHIR/STU3/Practitioner/"))
						.map(actorReference -> actorReference.substring(actorReference.lastIndexOf("/FHIR/STU3/Practitioner/") + "/FHIR/STU3/Practitioner/".length()))
						.collect(Collectors.toSet());

				if (epicPractitionerFhirIds.size() == 0) {
					getLogger().warn("Unable to find practitioner FHIR IDs for slot");
				} else {
					for (String epicPractitionerFhirId : epicPractitionerFhirIds) {
						Provider provider = providersByEpicPractitionerFhirId.get(epicPractitionerFhirId);

						if (provider != null) {
							AppointmentFindFhirStu3Response.Entry.Resource.Participant practitioner = entry.getResource().getParticipant().stream()
									.filter(participant -> participant.getActor().getReference().endsWith(epicPractitionerFhirId))
									.findAny()
									.get();

							Map<LocalDate, AvailabilityDate> availabilityDatesByDate = availabilityDatesByDateByProviderId.get(provider.getProviderId());
							AvailabilityDate availabilityDate = availabilityDatesByDate.get(slotDate);

							if (availabilityDate == null) {
								availabilityDate = new AvailabilityDate();
								availabilityDate.setDate(slotDate);
								availabilityDate.setTimes(new ArrayList<>());
								availabilityDatesByDate.put(slotDate, availabilityDate);
							}

							AvailabilityTime availabilityTime = null;

							for (AvailabilityTime potentialAvailabilityTime : availabilityDate.getTimes()) {
								if (potentialAvailabilityTime.getTime().equals(slotTime)) {
									availabilityTime = potentialAvailabilityTime;
									break;
								}
							}

							if (availabilityTime == null) {
								availabilityTime = new AvailabilityTime();
								availabilityTime.setTime(slotTime);
								availabilityTime.setStatus(AvailabilityStatus.AVAILABLE);
								availabilityTime.setEpicAppointmentFhirId(entry.getResource().getId());
								availabilityTime.setAppointmentTypeIds(new ArrayList<>());
								availabilityTime.setSlotStatusCodesByAppointmentTypeId(new HashMap<>());
								availabilityTime.setAppointmentStatusCodesByAppointmentTypeId(new HashMap<>());
								availabilityTime.setAppointmentParticipantStatusCodesByAppointmentTypeId(new HashMap<>());
							}

							List<AppointmentType> potentialAppointmentTypes = appointmentTypesByProviderId.get(provider.getProviderId());
							AppointmentType appointmentType = null;

							for (AppointmentType potentialAppointmentType : potentialAppointmentTypes) {
								if (potentialAppointmentType.getEpicVisitTypeSystem().equals(appointmentTypeCoding.getSystem())) {
									appointmentType = potentialAppointmentType;
									break;
								}
							}

							// Discard duplicate slots (protects against case where we get the same results back, e.g. running locally with mock data or Epic returns same data unintentionally)
							AppointmentFindFhirStu3SlotKey slotKey = new AppointmentFindFhirStu3SlotKey(epicPractitionerFhirId, slotDate, slotTime, appointmentType.getAppointmentTypeId());

							if (slotKeys.contains(slotKey)) {
								getLogger().trace("Already have a slot for {}, skipping...", slotKey);
								continue;
							}

							slotKeys.add(slotKey);

							availabilityTime.getAppointmentTypeIds().add(appointmentType.getAppointmentTypeId());
							availabilityTime.getAppointmentStatusCodesByAppointmentTypeId().put(appointmentType.getAppointmentTypeId(), entry.getResource().getStatus());
							availabilityTime.getAppointmentParticipantStatusCodesByAppointmentTypeId().put(appointmentType.getAppointmentTypeId(), practitioner.getStatus());

							// Assumes there is only one slot
							if (entry.getResource().getContained().size() != 1)
								throw new IllegalStateException("Invalid size of resource.contained element");

							availabilityTime.getSlotStatusCodesByAppointmentTypeId().put(appointmentType.getAppointmentTypeId(), entry.getResource().getContained().get(0).getStatus());

							// Ensure we are not booking within the provider's lead time, if applicable
							boolean slotIsTooEarly = false;

							if (provider.getSchedulingLeadTimeInHours() != null) {
								LocalDateTime now = LocalDateTime.now(provider.getTimeZone());
								LocalDateTime appointmentStartTime = LocalDateTime.of(availabilityDate.getDate(), availabilityTime.getTime());
								long hoursUntilAppointment = ChronoUnit.HOURS.between(now, appointmentStartTime);
								slotIsTooEarly = hoursUntilAppointment < provider.getSchedulingLeadTimeInHours();
							}

							if (!slotIsTooEarly)
								availabilityDate.getTimes().add(availabilityTime);
						}
					}
				}
			}
		}

		// Normalize the data for returning
		Map<UUID, List<AvailabilityDate>> availabilityDatesByEpicFhirProviderId = new HashMap<>();

		for (Provider provider : providersThatUseEpicFhir) {
			Map<LocalDate, AvailabilityDate> availabilityDatesByDate = availabilityDatesByDateByProviderId.get(provider.getProviderId());
			List<AvailabilityDate> availabilityDates = new ArrayList<>(availabilityDatesByDate.values());

			availabilityDatesByEpicFhirProviderId.put(provider.getProviderId(), availabilityDates);
		}

		return availabilityDatesByEpicFhirProviderId;
	}

	@Immutable
	protected static class AppointmentFindFhirStu3SlotKey {
		@Nonnull
		private final String key;

		public AppointmentFindFhirStu3SlotKey(@Nonnull String epicPractitionerFhirId,
																					@Nonnull LocalDate slotDate,
																					@Nonnull LocalTime slotTime,
																					@Nonnull UUID appointmentTypeId) {
			requireNonNull(epicPractitionerFhirId);
			requireNonNull(slotDate);
			requireNonNull(slotTime);
			requireNonNull(appointmentTypeId);

			this.key = format("%s-%s-%s-%s", epicPractitionerFhirId, slotDate, slotTime, appointmentTypeId);
		}

		@Override
		public int hashCode() {
			return this.key.hashCode();
		}

		@Override
		public boolean equals(Object object) {
			if (object == null)
				return false;

			if (!(object instanceof AppointmentFindFhirStu3SlotKey))
				return false;

			return this.key.equals(((AppointmentFindFhirStu3SlotKey) object).key);
		}

		@Override
		public String toString() {
			return format("%s{key=%s}", getClass().getSimpleName(), this.key);
		}
	}

	/**
	 * Be aware: this method may mutate the content of {@code providerFinds} in-place.
	 */
	protected void filterProviderFindsBySchedulingLeadTime(@Nonnull List<ProviderFind> providerFinds,
																												 @Nonnull List<Provider> providers,
																												 @Nonnull LocalDateTime currentDateTime,
																												 @Nonnull Map<DayOfWeek, BusinessHours> businessHoursByDayOfWeek,
																												 @Nonnull Set<HolidayId> holidayIds) {
		requireNonNull(providerFinds);
		requireNonNull(providers);
		requireNonNull(currentDateTime);
		requireNonNull(businessHoursByDayOfWeek);
		requireNonNull(holidayIds);

		boolean shouldUseBusinessHoursForCalculation = businessHoursByDayOfWeek.size() > 0;

		Map<UUID, LocalDateTime> earliestAllowedDateTimeByProviderId = providers.stream().collect(Collectors.toMap(
				provider -> provider.getProviderId(),
				provider -> shouldUseBusinessHoursForCalculation
						? BusinessHoursCalculator.determineEarliestBookingDateTime(currentDateTime, businessHoursByDayOfWeek, holidayIds, provider.getSchedulingLeadTimeInHours())
						: currentDateTime.plusHours(provider.getSchedulingLeadTimeInHours())));

		for (ProviderFind providerFind : providerFinds) {
			if (providerFind.getSchedulingSystemId() != SchedulingSystemId.COBALT && providerFind.getSchedulingSystemId() != SchedulingSystemId.EPIC)
				continue;

			List<AvailabilityDate> availabilityDatesToRemove = new ArrayList<>(providerFind.getDates().size());

			LocalDateTime earliestAllowedDateTime = earliestAllowedDateTimeByProviderId.get(providerFind.getProviderId());

			for (AvailabilityDate availabilityDate : providerFind.getDates()) {
				List<AvailabilityTime> availabilityTimesToRemove = new ArrayList<>();

				for (AvailabilityTime availabilityTime : availabilityDate.getTimes()) {
					if (availabilityTime.getStatus() == AvailabilityStatus.AVAILABLE) {
						LocalDateTime availabilityDateTime = LocalDateTime.of(availabilityDate.getDate(), availabilityTime.getTime());

						if (availabilityDateTime.isBefore(earliestAllowedDateTime))
							availabilityTimesToRemove.add(availabilityTime);
					}
				}

				availabilityDate.getTimes().removeAll(availabilityTimesToRemove);

				if (availabilityDate.getTimes().size() == 0)
					availabilityDatesToRemove.add(availabilityDate);
			}

			providerFind.getDates().removeAll(availabilityDatesToRemove);
		}
	}

	/**
	 * To try and keep things efficient, for native scheduling providers, we pull all their data at once and break it out in-memory
	 * to reduce the number of queries we need to make.  Then we put the processed results into NativeSchedulingAvailabilityData
	 * for later slot creation calculations.
	 */
	@Nonnull
	protected NativeSchedulingAvailabilityData loadNativeSchedulingAvailabilityData(@Nonnull InstitutionId institutionId,
																																									@Nonnull Set<VisitTypeId> visitTypeIds,
																																									@Nonnull LocalDateTime startDateTime,
																																									@Nonnull LocalDateTime endDateTime) {
		requireNonNull(institutionId);
		requireNonNull(visitTypeIds);
		requireNonNull(startDateTime);
		requireNonNull(endDateTime);

		if (startDateTime.isEqual(endDateTime) || startDateTime.isAfter(endDateTime))
			return NativeSchedulingAvailabilityData.empty();

		if (visitTypeIds == null)
			visitTypeIds = Collections.emptySet();
		else
			visitTypeIds = visitTypeIds.stream().filter(visitTypeId -> visitTypeId != null).collect(Collectors.toSet());

		Set<VisitTypeId> pinnedVisitTypeIds = visitTypeIds;

		// Pull only those logical availabilities that are for active providers and have not already ended
		String logicalAvailabilitiesSql = "SELECT la.* FROM logical_availability la, provider p " +
				"WHERE p.provider_id=la.provider_id AND p.active=TRUE AND p.institution_id=? AND p.scheduling_system_id=? " +
				"AND la.end_date_time > ?";

		List<Object> logicalAvailabilityParameters = new ArrayList<>(3);
		logicalAvailabilityParameters.add(institutionId);
		logicalAvailabilityParameters.add(SchedulingSystemId.COBALT);
		logicalAvailabilityParameters.add(startDateTime);

		Map<UUID, List<LogicalAvailability>> logicalAvailabilitiesByProviderId = getDatabase().queryForList(logicalAvailabilitiesSql, LogicalAvailability.class,
						logicalAvailabilityParameters.toArray()).stream()
				.collect(Collectors.groupingBy(LogicalAvailability::getProviderId));

		// Pull appointment types associated with logical availabilities
		String logicalAvailabilityAppointmentTypesSql = "SELECT apt.*, la.logical_availability_id FROM v_appointment_type apt, logical_availability la, logical_availability_appointment_type laat, provider p " +
				"WHERE laat.appointment_type_id=apt.appointment_type_id AND laat.logical_availability_id=la.logical_availability_id " +
				"AND la.logical_availability_type_id=? AND la.provider_id=p.provider_id AND p.active=TRUE AND p.institution_id=? AND p.scheduling_system_id=?";

		List<Object> logicalAvailabilityAppointmentTypeParameters = new ArrayList<>();
		logicalAvailabilityAppointmentTypeParameters.add(LogicalAvailabilityTypeId.OPEN);
		logicalAvailabilityAppointmentTypeParameters.add(institutionId);
		logicalAvailabilityAppointmentTypeParameters.add(SchedulingSystemId.COBALT);

		List<AppointmentTypeWithLogicalAvailabilityId> logicalAvailabilityAppointmentTypes = getDatabase().queryForList(logicalAvailabilityAppointmentTypesSql,
						AppointmentTypeWithLogicalAvailabilityId.class, logicalAvailabilityAppointmentTypeParameters.toArray()).stream()
				.filter((appointmentType -> {
					// If visit types specified, filter
					if (pinnedVisitTypeIds.size() > 0)
						return pinnedVisitTypeIds.contains(appointmentType.getVisitTypeId());

					return true;
				}))
				.collect(Collectors.toList());

		Map<UUID, List<AppointmentTypeWithLogicalAvailabilityId>> appointmentTypesByLogicalAvailabilityId = logicalAvailabilityAppointmentTypes.stream()
				.collect(Collectors.groupingBy(AppointmentTypeWithLogicalAvailabilityId::getLogicalAvailabilityId));

		// Pull all appointment types for active providers
		String allActiveAppointmentTypesSql = "SELECT apt.*, p.provider_id FROM appointment_type apt, provider_appointment_type pat, provider p " +
				"WHERE pat.appointment_type_id=apt.appointment_type_id " +
				"AND pat.provider_id=p.provider_id AND p.active=TRUE AND p.institution_id=? AND p.scheduling_system_id=?";

		List<Object> allActiveAppointmentTypesParameters = new ArrayList<>(2 + visitTypeIds.size());
		allActiveAppointmentTypesParameters.add(institutionId);
		allActiveAppointmentTypesParameters.add(SchedulingSystemId.COBALT);

		if (visitTypeIds.size() > 0) {
			allActiveAppointmentTypesParameters.addAll(visitTypeIds);
			// e.g. "(?),(?),(?)" for Postgres' VALUES clause (faster than IN list)
			String visitTypeIdValuesSql = visitTypeIds.stream().map(visitTypeId -> "(?)").collect(Collectors.joining(","));
			allActiveAppointmentTypesSql = format("%s AND apt.visit_type_id IN (VALUES %s)", allActiveAppointmentTypesSql, visitTypeIdValuesSql);
		}

		List<AppointmentTypeWithProviderId> allActiveAppointmentTypes = getDatabase().queryForList(allActiveAppointmentTypesSql,
				AppointmentTypeWithProviderId.class, allActiveAppointmentTypesParameters.toArray());

		Map<UUID, List<AppointmentTypeWithProviderId>> allActiveAppointmentTypesByProviderId = allActiveAppointmentTypes.stream()
				.collect(Collectors.groupingBy(AppointmentTypeWithProviderId::getProviderId));

		// Pull active appointments for providers within the current time window
		String appointmentsSql = "SELECT a.* FROM appointment a, provider p " +
				"WHERE p.provider_id=a.provider_id AND p.active=TRUE AND p.institution_id=? AND p.scheduling_system_id=? " +
				"AND a.canceled=FALSE AND a.start_time at time zone a.time_zone >= ? ORDER BY a.start_time";

		List<Object> appointmentsParameters = new ArrayList<>(3);
		appointmentsParameters.add(institutionId);
		appointmentsParameters.add(SchedulingSystemId.COBALT);

		// The "start_time at time zone a.time_zone" in the SQL above will normalize the appointment's start time to DB timezone (UTC).
		// This addresses the edge case of querying over a set of providers with different time zones.
		// The input startDateTime (a LocalDateTime) is normalized to UTC as well so we can do a consistent comparison across all appointments.
		//
		// Example: 3PM appointment in our DB with America/New_York is normalized to 7PM UTC by the query.
		//
		// select start_time, time_zone, start_time at time zone time_zone as normalized from appointment order by created desc limit 1;
		//     start_time      |    time_zone     |       normalized
		//---------------------+------------------+------------------------
		// 2022-03-16 15:00:00 | America/New_York | 2022-03-16 19:00:00+00
		appointmentsParameters.add(startDateTime.atZone(getConfiguration().getDefaultTimeZone()).toInstant());

		Map<UUID, List<Appointment>> activeAppointmentsByProviderId = getDatabase().queryForList(appointmentsSql, Appointment.class,
						appointmentsParameters.toArray()).stream()
				.collect(Collectors.groupingBy(Appointment::getProviderId));

		return new NativeSchedulingAvailabilityData(logicalAvailabilitiesByProviderId,
				appointmentTypesByLogicalAvailabilityId, allActiveAppointmentTypesByProviderId, activeAppointmentsByProviderId);
	}

	/**
	 * This performs the actual "slot" work, turning logical availabilities into bookable appointment slots.
	 */
	@Nonnull
	protected List<AvailabilityDate> availabilityDatesForNativeScheduling(@Nonnull AvailabilityDatesCommand command,
																																				@Nonnull LocalDateTime startDateTime,
																																				@Nonnull LocalDateTime endDateTime,
																																				@Nonnull NativeSchedulingAvailabilityData nativeSchedulingAvailabilityData) {
		requireNonNull(command);
		requireNonNull(startDateTime);
		requireNonNull(endDateTime);
		requireNonNull(nativeSchedulingAvailabilityData);

		LocalDate startDate = startDateTime.toLocalDate();
		LocalDate endDate = endDateTime.toLocalDate();

		List<LogicalAvailability> logicalAvailabilities = requiredValues(nativeSchedulingAvailabilityData.getLogicalAvailabilitiesByProviderId(), command.getProvider().getProviderId());
		List<Appointment> appointments = requiredValues(nativeSchedulingAvailabilityData.getActiveAppointmentsByProviderId(), command.getProvider().getProviderId());
		List<AppointmentTypeWithProviderId> allActiveAppointmentTypes = requiredValues(nativeSchedulingAvailabilityData.getAllActiveAppointmentTypesByProviderId(), command.getProvider().getProviderId());

		// First, break everything out by date - start with appointments...
		Map<LocalDate, List<Appointment>> appointmentsByDate = appointments.stream()
				.collect(Collectors.groupingBy((appointment -> appointment.getStartTime().toLocalDate())));

		// ... and then, logical availabilities (similar to how our ProviderCalendar "expands" logical availabilities based on recurrence rules)
		Map<LocalDate, List<Availability>> availabilitiesByDate = new HashMap<>();
		Map<LocalDate, List<Block>> blocksByDate = new HashMap<>();

		for (LogicalAvailability logicalAvailability : logicalAvailabilities) {
			List<? extends AppointmentType> appointmentTypes = nativeSchedulingAvailabilityData.getAppointmentTypesByLogicalAvailabilityId().get(logicalAvailability.getLogicalAvailabilityId());

			// If there are no appointment types specified, it indicates that _all_ active appointment types for the provider are valid.
			if (appointmentTypes == null || appointmentTypes.size() == 0)
				appointmentTypes = allActiveAppointmentTypes;

			if (logicalAvailability.getRecurrenceTypeId() == RecurrenceTypeId.NONE) {
				// Simple case: no recurrence
				if (logicalAvailability.getLogicalAvailabilityTypeId() == LogicalAvailabilityTypeId.OPEN) {
					Availability availability = new Availability();
					availability.setLogicalAvailabilityId(logicalAvailability.getLogicalAvailabilityId());
					availability.setStartDateTime(logicalAvailability.getStartDateTime());
					availability.setEndDateTime(logicalAvailability.getEndDateTime());
					availability.setAppointmentTypes(new ArrayList<>(appointmentTypes));

					addToValues(availabilitiesByDate, availability.getStartDateTime().toLocalDate(), availability);
				} else if (logicalAvailability.getLogicalAvailabilityTypeId() == LogicalAvailabilityTypeId.BLOCK) {
					Block block = new Block();
					block.setLogicalAvailabilityId(logicalAvailability.getLogicalAvailabilityId());
					block.setStartDateTime(logicalAvailability.getStartDateTime());
					block.setEndDateTime(logicalAvailability.getEndDateTime());

					addToValues(blocksByDate, block.getStartDateTime().toLocalDate(), block);
				} else {
					throw new IllegalStateException(format("Not sure how to handle %s.%s", LogicalAvailabilityTypeId.class.getSimpleName(),
							logicalAvailability.getLogicalAvailabilityTypeId().name()));
				}
			} else if (logicalAvailability.getRecurrenceTypeId() == RecurrenceTypeId.DAILY) {
				// Figure out the first and last dates of the range we're getting availability for
				LocalDate currentDate = startDate;

				// For each date within the range...
				while (currentDate.isEqual(endDate) || currentDate.isBefore(endDate)) {
					if ((currentDate.isEqual(logicalAvailability.getStartDateTime().toLocalDate()) || currentDate.isAfter(logicalAvailability.getStartDateTime().toLocalDate()))
							&& (currentDate.isEqual(logicalAvailability.getEndDateTime().toLocalDate()) || currentDate.isBefore(logicalAvailability.getEndDateTime().toLocalDate()))) {
						// If recurrence rule is enabled for the day...
						if ((currentDate.getDayOfWeek() == DayOfWeek.MONDAY && logicalAvailability.getRecurMonday())
								|| (currentDate.getDayOfWeek() == DayOfWeek.TUESDAY && logicalAvailability.getRecurTuesday())
								|| (currentDate.getDayOfWeek() == DayOfWeek.WEDNESDAY && logicalAvailability.getRecurWednesday())
								|| (currentDate.getDayOfWeek() == DayOfWeek.THURSDAY && logicalAvailability.getRecurThursday())
								|| (currentDate.getDayOfWeek() == DayOfWeek.FRIDAY && logicalAvailability.getRecurFriday())
								|| (currentDate.getDayOfWeek() == DayOfWeek.SATURDAY && logicalAvailability.getRecurSaturday())
								|| (currentDate.getDayOfWeek() == DayOfWeek.SUNDAY && logicalAvailability.getRecurSunday())) {
							// ...normalize the logical availability's start and end times to be "today"
							LocalDateTime currentStartDateTime = LocalDateTime.of(currentDate, logicalAvailability.getStartDateTime().toLocalTime());
							LocalDateTime currentEndDateTime = LocalDateTime.of(currentDate, logicalAvailability.getEndDateTime().toLocalTime());

							if (logicalAvailability.getLogicalAvailabilityTypeId() == LogicalAvailabilityTypeId.OPEN) {
								Availability availability = new Availability();
								availability.setLogicalAvailabilityId(logicalAvailability.getLogicalAvailabilityId());
								availability.setStartDateTime(currentStartDateTime);
								availability.setEndDateTime(currentEndDateTime);
								availability.setAppointmentTypes(new ArrayList<>(appointmentTypes));

								addToValues(availabilitiesByDate, currentDate, availability);
							} else if (logicalAvailability.getLogicalAvailabilityTypeId() == LogicalAvailabilityTypeId.BLOCK) {
								Block block = new Block();
								block.setLogicalAvailabilityId(logicalAvailability.getLogicalAvailabilityId());
								block.setStartDateTime(currentStartDateTime);
								block.setEndDateTime(currentEndDateTime);

								addToValues(blocksByDate, currentDate, block);
							} else {
								throw new IllegalStateException(format("Not sure how to handle %s.%s", LogicalAvailabilityTypeId.class.getSimpleName(),
										logicalAvailability.getLogicalAvailabilityTypeId().name()));
							}
						}
					}
					currentDate = currentDate.plusDays(1);
				}
			} else {
				throw new IllegalStateException(format("Not sure how to handle %s.%s", RecurrenceTypeId.class.getSimpleName(),
						logicalAvailability.getRecurrenceTypeId().name()));
			}
		}

		// Now that we have everything broken out by date, walk through all dates in the requested range and build out slots.
		Map<LocalDate, AvailabilityDate> availabilityDatesByDate = new HashMap<>();
		LocalDate currentDate = startDate;

		// For each date in the date range, figure out available ranges and remove ranges we know are unavailable
		// (either blocks or appointments) so we end up with a set of subrange[s].
		while (currentDate.isBefore(endDate)) {
			if ((command.getDaysOfWeek().size() > 0 && !command.getDaysOfWeek().contains(currentDate.getDayOfWeek()) /* Respect "day of week" filter */)
					|| (command.getStartDate() != null && currentDate.isBefore(command.getStartDate())) /* Respect "start date" filter */
					|| (command.getEndDate() != null && currentDate.isAfter(command.getEndDate())) /* Respect "end date" filter */
			) {
				currentDate = currentDate.plusDays(1);
				continue;
			}

			// First, get a list of all availabilities/blocks/appointments for this date
			List<Availability> currentAvailabilities = requiredValues(availabilitiesByDate, currentDate);
			List<Block> currentBlocks = requiredValues(blocksByDate, currentDate);
			List<Appointment> currentAppointments = requiredValues(appointmentsByDate, currentDate);

			// Ensure they are sorted before working with them
			Collections.sort(currentAvailabilities);
			Collections.sort(currentBlocks);
			Collections.sort(currentAppointments);

			// Create a list of "ranges" for each, so we can subtract out the "holes" of blocks/appointment ranges from availability ranges
			List<RangedValue<Availability>> availabilityRanges = currentAvailabilities.stream()
					.map(availability -> new RangedValue<>(availability, availability.getStartDateTime(), availability.getEndDateTime()))
					.collect(Collectors.toList());

			List<RangedValue<Block>> blockRanges = currentBlocks.stream()
					.map(block -> new RangedValue<>(block, block.getStartDateTime(), block.getEndDateTime()))
					.collect(Collectors.toList());

			List<RangedValue<Appointment>> appointmentRanges = currentAppointments.stream()
					.map(appointment -> new RangedValue<>(appointment, appointment.getStartTime(), appointment.getEndTime()))
					.collect(Collectors.toList());

			List<RangedValue<Availability>> availabilityRangesMinusBlocks = new ArrayList<>();

			// Remove block ranges first...
			if (blockRanges.size() > 0) {
				for (RangedValue<Availability> availabilityRange : availabilityRanges)
					for (RangedValue<Block> blockRange : blockRanges)
						availabilityRangesMinusBlocks.addAll(availabilityRange.minusRange(blockRange));
			} else {
				availabilityRangesMinusBlocks.addAll(availabilityRanges);
			}

			// ...then remove appointment ranges to finalize.
			List<RangedValue<Availability>> finalAvailabilityRanges = new ArrayList<>();

			if (appointmentRanges.size() > 0) {
				// For each availability range, cut it up into smaller and smaller sub-ranges as we see more and more appointments
				for (RangedValue<Availability> availabilityRange : availabilityRangesMinusBlocks) {
					List<RangedValue<Availability>> currentAvailabilityRanges = List.of(availabilityRange);

					for (RangedValue<Appointment> appointmentRange : appointmentRanges) {
						List<RangedValue<Availability>> updatedAvailabilityRanges = new ArrayList<>();

						for (RangedValue<Availability> currentAvailabilityRange : currentAvailabilityRanges)
							updatedAvailabilityRanges.addAll(currentAvailabilityRange.minusRange(appointmentRange));

						currentAvailabilityRanges = updatedAvailabilityRanges;
					}

					finalAvailabilityRanges.addAll(currentAvailabilityRanges);
				}
			} else {
				finalAvailabilityRanges.addAll(availabilityRangesMinusBlocks);
			}

			// We are left with a final set of availability ranges for this date.
			// We can turn these into a set of AvailabilityDates (slots) to return to the user.
			AvailabilityDate availabilityDate = availabilityDatesByDate.get(currentDate);

			if (availabilityDate == null) {
				availabilityDate = new AvailabilityDate();
				availabilityDate.setDate(currentDate);
				availabilityDate.setTimes(new ArrayList<>());
				availabilityDate.setFullyBooked(false);

				availabilityDatesByDate.put(currentDate, availabilityDate);
			}

			// Add appointments as "booked" slots
			for (Appointment appointment : currentAppointments) {
				AvailabilityTime availabilityTime = new AvailabilityTime();
				availabilityTime.setStatus(AvailabilityStatus.BOOKED);
				availabilityTime.setTime(appointment.getStartTime().toLocalTime());
				availabilityTime.setAppointmentTypeIds(Arrays.asList(appointment.getAppointmentTypeId()));

				availabilityDate.getTimes().add(availabilityTime);
			}

			// To make slots, we find the shortest appointment type duration in the range and make slots of that size.
			// If there are any appointment types that could cause a slot to "bleed" outside of the availability range, remove them.
			for (RangedValue<Availability> availabilityRange : finalAvailabilityRanges) {
				List<AppointmentType> appointmentTypes = availabilityRange.getValue().getAppointmentTypes();

				if (appointmentTypes.size() == 0) {
					getLogger().warn("No appointment types available for range with logical availability ID {}; we should not see this scenario",
							availabilityRange.getValue().getLogicalAvailabilityId());
					continue;
				}

				// Slot size is the size of the shortest appointment type in the range
				int slotSizeInMinutes = appointmentTypes.stream()
						.mapToInt(appointmentType -> appointmentType.getDurationInMinutes().intValue())
						.min()
						.getAsInt();

				LocalTime slotTime = availabilityRange.getStartDateTime().toLocalTime();
				LocalTime slotEndTime = availabilityRange.getEndDateTime().toLocalTime();

				while (slotTime.isBefore(slotEndTime)) {
					// Figure out which appointment IDs fit in the slot
					List<UUID> appointmentTypeIdsThatFit = new ArrayList<>(appointmentTypes.size());

					for (AppointmentType appointmentType : appointmentTypes) {
						LocalTime appointmentTypeEndTime = slotTime.plusMinutes(appointmentType.getDurationInMinutes());

						if (appointmentTypeEndTime.isBefore(slotEndTime) || appointmentTypeEndTime.equals(slotEndTime))
							appointmentTypeIdsThatFit.add(appointmentType.getAppointmentTypeId());
					}

					// Only add the slot if there are appointment types (if no appointment types, that means nothing fit in the slot)
					if (appointmentTypeIdsThatFit.size() > 0) {

						// Respect "start time" and "end time" filters
						boolean tooEarlyForFilter = command.getStartTime() != null && slotTime.isBefore(command.getStartTime());
						boolean tooLateForFilter = command.getEndTime() != null && slotTime.isAfter(command.getEndTime());

						if (!tooEarlyForFilter && !tooLateForFilter) {
							AvailabilityTime availabilityTime = new AvailabilityTime();
							availabilityTime.setStatus(AvailabilityStatus.AVAILABLE);
							availabilityTime.setTime(slotTime);
							availabilityTime.setAppointmentTypeIds(appointmentTypeIdsThatFit);

							availabilityDate.getTimes().add(availabilityTime);
						}
					}

					LocalTime currentSlotTime = slotTime;

					slotTime = slotTime.plusMinutes(slotSizeInMinutes);

					// If we hit this case, that means we wrapped to the next day.
					// If we don't break, then we can get into an infinite loop.
					// TODO: should we support handling of slots that cross date boundaries?  Probably not, but leaving a note here...
					if (currentSlotTime.isAfter(slotTime))
						break;
				}
			}

			currentDate = currentDate.plusDays(1);
		}

		// Get our final list of dates and make sure it's sorted
		List<AvailabilityDate> dates = new ArrayList<>(availabilityDatesByDate.values()).stream()
				.filter(date -> date.getTimes().size() > 0)
				.collect(Collectors.toList());

		Collections.sort(dates, (date1, date2) -> date1.getDate().compareTo(date2.getDate()));

		// If a user specifies overlapping logical availabilities, we might have duplicate time slots (with perhaps different appointment types).
		// Normalize these slots by "squishing" together into a single slot with the union of the appointment types.
		for (AvailabilityDate date : dates) {
			// See where our duplicates are, and keep track of the union of all appointment types by time
			SortedMap<LocalTime, List<AvailabilityTime>> availabilityTimesByTime = new TreeMap<>();
			Map<LocalTime, Set<UUID>> appointmentTypeIdsByTime = new HashMap<>(date.getTimes().size());

			for (AvailabilityTime time : date.getTimes()) {
				addToValues(availabilityTimesByTime, time.getTime(), time);

				Set<UUID> appointmentTypeIds = appointmentTypeIdsByTime.get(time.getTime());

				if (appointmentTypeIds == null) {
					appointmentTypeIds = new HashSet<>();
					appointmentTypeIdsByTime.put(time.getTime(), appointmentTypeIds);
				}

				appointmentTypeIds.addAll(time.getAppointmentTypeIds());
			}

			List<AvailabilityTime> normalizedAvailabilityTimes = new ArrayList<>(date.getTimes().size());

			for (Entry<LocalTime, List<AvailabilityTime>> entry : availabilityTimesByTime.entrySet()) {
				List<AvailabilityTime> availabilityTimes = entry.getValue();

				if (availabilityTimes.size() == 1) {
					// Normal case: no duplicates, so no squishing needed
					normalizedAvailabilityTimes.add(availabilityTimes.get(0));
				} else if (availabilityTimes.size() > 1) {
					// Found a duplicate - let's squish.  Pick the first one arbitrarily and then apply the union of appointment types
					AvailabilityTime squishedAvailabilityTime = availabilityTimes.get(0);
					squishedAvailabilityTime.setAppointmentTypeIds(new ArrayList<>(appointmentTypeIdsByTime.get(entry.getKey())));

					normalizedAvailabilityTimes.add(squishedAvailabilityTime);
				}
			}

			date.setTimes(normalizedAvailabilityTimes);
		}

		return dates;
	}

	@ThreadSafe
	protected static class RangedValue<T> {
		@Nonnull
		private final T value;
		@Nonnull
		private final LocalDateTime startDateTime;
		@Nonnull
		private final LocalDateTime endDateTime;

		public RangedValue(@Nonnull T value,
											 @Nonnull LocalDateTime startDateTime,
											 @Nonnull LocalDateTime endDateTime) {
			requireNonNull(value);
			requireNonNull(startDateTime);
			requireNonNull(endDateTime);

			this.value = value;
			this.startDateTime = startDateTime;
			this.endDateTime = endDateTime;
		}

		@Override
		public String toString() {
			return format("%s{startDateTime=%s, endDateTime=%s, value=%s}", getClass().getSimpleName(),
					getStartDateTime(), getEndDateTime(), getValue());
		}

		@Nonnull
		public List<RangedValue<T>> minusRange(@Nonnull RangedValue<?> otherRange) {
			requireNonNull(otherRange);

			// If the other range is equal or larger than this range, this range goes away entirely
			if ((otherRange.getStartDateTime().isBefore(getStartDateTime()) || otherRange.getStartDateTime().isEqual(getStartDateTime()))
					&& (otherRange.getEndDateTime().isAfter(getEndDateTime()) || otherRange.getEndDateTime().isEqual(getEndDateTime())))
				return List.of();

			// If the other range ends on or before this one begins, nothing to do
			if (otherRange.getEndDateTime().isBefore(getStartDateTime()) || otherRange.getEndDateTime().isEqual(getStartDateTime()))
				return List.of(this);

			// If the other range begins on or after this one ends, nothing to do
			if (otherRange.getStartDateTime().isAfter(getEndDateTime()) || otherRange.getStartDateTime().isEqual(getEndDateTime()))
				return List.of(this);

			// At this point, we must have an overlap - modify our range (potentially dividing it into two) by subtracting out the other range.
			//
			// There are 3 scenarios:
			//
			//  1: Other range might start before this range and end during it (result: 1 subrange)
			//  2: Other range might start during this range and end after it (result: 1 subrange)
			//  3: Other range might start during this range and end during this range (divide into 2 subranges)

			// Scenario 1
			if ((otherRange.getStartDateTime().isBefore(getStartDateTime()) || otherRange.getStartDateTime().isEqual(getStartDateTime()))
					&& otherRange.getEndDateTime().isBefore(getEndDateTime()))
				return List.of(new RangedValue<>(getValue(), otherRange.getEndDateTime(), getEndDateTime()));

			// Scenario 2
			if (otherRange.getStartDateTime().isAfter(getStartDateTime())
					&& (otherRange.getEndDateTime().isAfter(getEndDateTime()) || otherRange.getEndDateTime().isEqual(getEndDateTime())))
				return List.of(new RangedValue<>(getValue(), getStartDateTime(), otherRange.getStartDateTime()));

			// Scenario 3
			return List.of(new RangedValue<>(getValue(), getStartDateTime(), otherRange.getStartDateTime()),
					new RangedValue<>(getValue(), otherRange.getEndDateTime(), getEndDateTime()));
		}

		@Nonnull
		public T getValue() {
			return value;
		}

		@Nonnull
		public LocalDateTime getStartDateTime() {
			return startDateTime;
		}

		@Nonnull
		public LocalDateTime getEndDateTime() {
			return endDateTime;
		}
	}

	@Nonnull
	protected <K, V> List<V> requiredValues(@Nonnull Map<K, List<V>> valuesByKey,
																					@Nonnull K key) {
		requireNonNull(valuesByKey);
		requireNonNull(key);

		List<V> values = valuesByKey.get(key);
		return values == null ? new ArrayList<>() : values;
	}

	protected <K, V> void addToValues(@Nonnull Map<K, List<V>> valuesByKey,
																		@Nonnull K key,
																		@Nonnull V value) {
		requireNonNull(valuesByKey);
		requireNonNull(key);
		requireNonNull(value);

		List<V> values = valuesByKey.get(key);

		if (values == null) {
			values = new ArrayList<>();
			valuesByKey.put(key, values);
		}

		values.add(value);
	}

	@Nonnull
	protected List<AvailabilityDate> availabilityDatesForNonNativeScheduling(@Nonnull AvailabilityDatesCommand command) {
		requireNonNull(command);

		List<AvailabilityDate> dates = new ArrayList<>();

		// First, fill in "available" slots based on what we know from Acuity/EPIC
		List<ProviderAvailability> providerAvailabilities;
		StringBuilder providerAvailabilityQuery = new StringBuilder("SELECT pa.* FROM provider_availability pa, v_appointment_type at WHERE pa.provider_id=? AND pa.appointment_type_id=at.appointment_type_id ");
		List<Object> providerAvailabilityParameters = new ArrayList<>();
		providerAvailabilityParameters.add(command.getProvider().getProviderId());

		if (command.getVisitTypeIds().size() > 0) {
			providerAvailabilityQuery.append(" AND at.visit_type_id IN ");
			providerAvailabilityQuery.append(sqlInListPlaceholders(command.getVisitTypeIds()));

			providerAvailabilityParameters.addAll(command.getVisitTypeIds());
		}

		if (command.getEpicDepartmentIds().size() > 0) {
			providerAvailabilityQuery.append(" AND pa.epic_department_id IN ");
			providerAvailabilityQuery.append(sqlInListPlaceholders(command.getEpicDepartmentIds()));

			providerAvailabilityParameters.addAll(command.getEpicDepartmentIds());
		}

		providerAvailabilities = getDatabase().queryForList(providerAvailabilityQuery.toString(), ProviderAvailability.class, providerAvailabilityParameters.toArray(new Object[]{}));

		Map<LocalDate, AvailabilityDate> availabilityDatesByDate = new HashMap<>(14);
		Map<LocalDateTime, AvailabilityTime> availabilityTimesByDateTime = new HashMap<>();

		for (ProviderAvailability providerAvailability : providerAvailabilities) {
			LocalDateTime dateTime = providerAvailability.getDateTime();
			LocalDate date = dateTime.toLocalDate();
			LocalTime time = dateTime.toLocalTime();

			// Respect "day of week" filter
			if (command.getDaysOfWeek().size() > 0 && !command.getDaysOfWeek().contains(date.getDayOfWeek()))
				continue;

			// Respect "start date" filter
			if (command.getStartDate() != null && date.isBefore(command.getStartDate()))
				continue;

			// Respect "end date" filter
			if (command.getEndDate() != null && date.isAfter(command.getEndDate()))
				continue;

			// Respect "start time" filter
			if (command.getStartTime() != null && time.isBefore(command.getStartTime()))
				continue;

			// Respect "end time" filter
			if (command.getEndTime() != null && time.isAfter(command.getEndTime()))
				continue;

			// Don't include anything that is "today"
			if (!date.isAfter(command.getCurrentDate()))
				continue;

			AvailabilityDate availabilityDate = availabilityDatesByDate.get(date);

			if (availabilityDate == null) {
				availabilityDate = new AvailabilityDate();
				availabilityDate.setDate(date);
				availabilityDatesByDate.put(date, availabilityDate);
				dates.add(availabilityDate);
			}

			List<AvailabilityTime> availabilityTimes = availabilityDate.getTimes();

			if (availabilityTimes == null) {
				availabilityTimes = new ArrayList<>();
				availabilityDate.setTimes(availabilityTimes);
			}

			AvailabilityTime availabilityTime = availabilityTimesByDateTime.get(dateTime);

			if (availabilityTime == null) {
				availabilityTime = new AvailabilityTime();
				availabilityTime.setStatus(AvailabilityStatus.AVAILABLE);
				availabilityTime.setTime(time);
				availabilityTime.setAppointmentTypeIds(new ArrayList<>());
				availabilityTime.setEpicDepartmentId(providerAvailability.getEpicDepartmentId());
				availabilityTimes.add(availabilityTime);
				availabilityTimesByDateTime.put(dateTime, availabilityTime);
			}

			// If you set up overlapping logical availabilities with COBALT scheduling, you can have the same appointment type multiple times.
			// Ignore extras here
			if (!availabilityTime.getAppointmentTypeIds().contains(providerAvailability.getAppointmentTypeId()))
				availabilityTime.getAppointmentTypeIds().add(providerAvailability.getAppointmentTypeId());
		}

		if (command.getAvailability() == ProviderFindAvailability.ALL) {
			// Next, fill in "booked" slots using appointments on file in our DB.
			// Ignore times before now!
			LocalDateTime rightNow = LocalDateTime.now(command.getProvider().getTimeZone());
			List<Appointment> appointments = getDatabase().queryForList("SELECT * FROM appointment WHERE provider_id=? AND start_time > ? AND canceled=FALSE", Appointment.class, command.getProvider().getProviderId(), rightNow);

			for (Appointment appointment : appointments) {
				LocalDateTime dateTime = appointment.getStartTime();
				LocalDate date = dateTime.toLocalDate();
				LocalTime time = dateTime.toLocalTime();

				// Respect "day of week" filter
				if (command.getDaysOfWeek().size() > 0 && !command.getDaysOfWeek().contains(date.getDayOfWeek()))
					continue;

				// Respect "start date" filter
				if (command.getStartDate() != null && date.isBefore(command.getStartDate()))
					continue;

				// Respect "end date" filter
				if (command.getEndDate() != null && date.isAfter(command.getEndDate()))
					continue;

				// Respect "start time" filter
				if (command.getStartTime() != null && time.isBefore(command.getStartTime()))
					continue;

				// Respect "end time" filter
				if (command.getEndTime() != null && time.isAfter(command.getEndTime()))
					continue;

				AvailabilityDate availabilityDate = availabilityDatesByDate.get(date);

				if (availabilityDate == null) {
					availabilityDate = new AvailabilityDate();
					availabilityDate.setDate(date);
					availabilityDatesByDate.put(date, availabilityDate);
					dates.add(availabilityDate);
				}

				List<AvailabilityTime> availabilityTimes = availabilityDate.getTimes();

				if (availabilityTimes == null) {
					availabilityTimes = new ArrayList<>();
					availabilityDate.setTimes(availabilityTimes);
				}

				AvailabilityTime availabilityTime = availabilityTimesByDateTime.get(dateTime);

				// Fix up disconnect in the event of our DB being out of sync with what's live in Acuity (we sync every 10 mins or so).
				// BOOKED should trump AVAILABLE.
				// If availability time is not null here, that means there is already an AVAILABLE record and it should
				// instead be overwritten with BOOKED
				if (availabilityTime == null) {
					availabilityTime = new AvailabilityTime();
					availabilityTimes.add(availabilityTime);
					availabilityTime.setTime(time);
					availabilityTimesByDateTime.put(dateTime, availabilityTime);
				}

				List<UUID> appointmentTypeIds = new ArrayList<>();
				appointmentTypeIds.add(appointment.getAppointmentTypeId());

				availabilityTime.setStatus(AvailabilityStatus.BOOKED);
				availabilityTime.setAppointmentTypeIds(appointmentTypeIds);
			}
		}

		return dates;
	}

	@Immutable
	protected static class NativeSchedulingAvailabilityData {
		@Nonnull
		private Map<UUID, List<LogicalAvailability>> logicalAvailabilitiesByProviderId;
		@Nonnull
		private Map<UUID, List<AppointmentTypeWithLogicalAvailabilityId>> appointmentTypesByLogicalAvailabilityId;
		@Nonnull
		private Map<UUID, List<AppointmentTypeWithProviderId>> allActiveAppointmentTypesByProviderId;
		@Nonnull
		private Map<UUID, List<Appointment>> activeAppointmentsByProviderId;

		@Nonnull
		public static NativeSchedulingAvailabilityData empty() {
			return new NativeSchedulingAvailabilityData(Collections.emptyMap(), Collections.emptyMap(),
					Collections.emptyMap(), Collections.emptyMap());
		}

		public NativeSchedulingAvailabilityData(@Nonnull Map<UUID, List<LogicalAvailability>> logicalAvailabilitiesByProviderId,
																						@Nonnull Map<UUID, List<AppointmentTypeWithLogicalAvailabilityId>> appointmentTypesByLogicalAvailabilityId,
																						@Nonnull Map<UUID, List<AppointmentTypeWithProviderId>> allActiveAppointmentTypesByProviderId,
																						@Nonnull Map<UUID, List<Appointment>> activeAppointmentsByProviderId) {
			requireNonNull(logicalAvailabilitiesByProviderId);
			requireNonNull(appointmentTypesByLogicalAvailabilityId);
			requireNonNull(allActiveAppointmentTypesByProviderId);
			requireNonNull(activeAppointmentsByProviderId);

			this.logicalAvailabilitiesByProviderId = logicalAvailabilitiesByProviderId;
			this.appointmentTypesByLogicalAvailabilityId = appointmentTypesByLogicalAvailabilityId;
			this.allActiveAppointmentTypesByProviderId = allActiveAppointmentTypesByProviderId;
			this.activeAppointmentsByProviderId = activeAppointmentsByProviderId;
		}

		@Override
		@Nonnull
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
		}

		@Nonnull
		public Map<UUID, List<LogicalAvailability>> getLogicalAvailabilitiesByProviderId() {
			return logicalAvailabilitiesByProviderId;
		}

		@Nonnull
		public Map<UUID, List<AppointmentTypeWithLogicalAvailabilityId>> getAppointmentTypesByLogicalAvailabilityId() {
			return appointmentTypesByLogicalAvailabilityId;
		}

		@Nonnull
		public Map<UUID, List<AppointmentTypeWithProviderId>> getAllActiveAppointmentTypesByProviderId() {
			return allActiveAppointmentTypesByProviderId;
		}

		@Nonnull
		public Map<UUID, List<Appointment>> getActiveAppointmentsByProviderId() {
			return activeAppointmentsByProviderId;
		}
	}

	@NotThreadSafe
	protected static class AvailabilityDatesCommand {
		@Nullable
		private Provider provider;
		@Nullable
		private Set<VisitTypeId> visitTypeIds;
		@Nullable
		private LocalDate startDate;
		@Nullable
		private LocalTime startTime;
		@Nullable
		private LocalDate endDate;
		@Nullable
		private LocalTime endTime;
		@Nullable
		private LocalDate currentDate;
		@Nullable
		private Set<DayOfWeek> daysOfWeek;
		@Nullable
		private Set<UUID> epicDepartmentIds;
		@Nullable
		private ProviderFindAvailability availability;

		@Override
		@Nonnull
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
		}

		@Nullable
		public Provider getProvider() {
			return provider;
		}

		public void setProvider(@Nullable Provider provider) {
			this.provider = provider;
		}

		@Nullable
		public Set<VisitTypeId> getVisitTypeIds() {
			return visitTypeIds;
		}

		public void setVisitTypeIds(@Nullable Set<VisitTypeId> visitTypeIds) {
			this.visitTypeIds = visitTypeIds;
		}

		@Nullable
		public LocalDate getStartDate() {
			return startDate;
		}

		public void setStartDate(@Nullable LocalDate startDate) {
			this.startDate = startDate;
		}

		@Nullable
		public LocalTime getStartTime() {
			return startTime;
		}

		public void setStartTime(@Nullable LocalTime startTime) {
			this.startTime = startTime;
		}

		@Nullable
		public LocalDate getEndDate() {
			return endDate;
		}

		public void setEndDate(@Nullable LocalDate endDate) {
			this.endDate = endDate;
		}

		@Nullable
		public LocalTime getEndTime() {
			return endTime;
		}

		public void setEndTime(@Nullable LocalTime endTime) {
			this.endTime = endTime;
		}

		@Nullable
		public LocalDate getCurrentDate() {
			return currentDate;
		}

		public void setCurrentDate(@Nullable LocalDate currentDate) {
			this.currentDate = currentDate;
		}

		@Nullable
		public Set<DayOfWeek> getDaysOfWeek() {
			return daysOfWeek;
		}

		public void setDaysOfWeek(@Nullable Set<DayOfWeek> daysOfWeek) {
			this.daysOfWeek = daysOfWeek;
		}

		@Nullable
		public Set<UUID> getEpicDepartmentIds() {
			return this.epicDepartmentIds;
		}

		public void setEpicDepartmentIds(@Nullable Set<UUID> epicDepartmentIds) {
			this.epicDepartmentIds = epicDepartmentIds;
		}

		@Nullable
		public ProviderFindAvailability getAvailability() {
			return availability;
		}

		public void setAvailability(@Nullable ProviderFindAvailability availability) {
			this.availability = availability;
		}
	}

	@NotThreadSafe
	protected static class ProviderTreatmentDescription {
		@Nullable
		private UUID providerId;
		@Nullable
		private String treatmentDescription;

		@Nullable
		public UUID getProviderId() {
			return providerId;
		}

		public void setProviderId(@Nullable UUID providerId) {
			this.providerId = providerId;
		}

		@Nullable
		public String getTreatmentDescription() {
			return treatmentDescription;
		}

		public void setTreatmentDescription(@Nullable String treatmentDescription) {
			this.treatmentDescription = treatmentDescription;
		}
	}

	@NotThreadSafe
	protected static class ProviderAppointmentType {
		@Nullable
		private UUID providerId;
		@Nullable
		private UUID appointmentTypeId;

		@Nullable
		public UUID getProviderId() {
			return providerId;
		}

		public void setProviderId(@Nullable UUID providerId) {
			this.providerId = providerId;
		}

		@Nullable
		public UUID getAppointmentTypeId() {
			return appointmentTypeId;
		}

		public void setAppointmentTypeId(@Nullable UUID appointmentTypeId) {
			this.appointmentTypeId = appointmentTypeId;
		}
	}

	@NotThreadSafe
	protected static class ProviderSupportRole {
		@Nullable
		private UUID providerId;
		@Nullable
		private SupportRoleId supportRoleId;
		@Nullable
		private String supportRoleDescription;

		@Nullable
		public UUID getProviderId() {
			return providerId;
		}

		public void setProviderId(@Nullable UUID providerId) {
			this.providerId = providerId;
		}

		@Nullable
		public SupportRoleId getSupportRoleId() {
			return supportRoleId;
		}

		public void setSupportRoleId(@Nullable SupportRoleId supportRoleId) {
			this.supportRoleId = supportRoleId;
		}

		@Nullable
		public String getSupportRoleDescription() {
			return supportRoleDescription;
		}

		public void setSupportRoleDescription(@Nullable String supportRoleDescription) {
			this.supportRoleDescription = supportRoleDescription;
		}
	}

	@NotThreadSafe
	protected static class ProviderIntakeAssessmentPrompt {
		@Nullable
		private UUID providerId;
		@Nullable
		private Boolean showIntakeAssessmentPrompt;

		@Nullable
		public UUID getProviderId() {
			return providerId;
		}

		public void setProviderId(@Nullable UUID providerId) {
			this.providerId = providerId;
		}

		@Nullable
		public Boolean getShowIntakeAssessmentPrompt() {
			return showIntakeAssessmentPrompt;
		}

		public void setShowIntakeAssessmentPrompt(@Nullable Boolean showIntakeAssessmentPrompt) {
			this.showIntakeAssessmentPrompt = showIntakeAssessmentPrompt;
		}
	}

	@Nonnull
	public List<PaymentType> findPaymentTypes() {
		return getDatabase().queryForList("SELECT * FROM payment_type ORDER BY display_order", PaymentType.class);
	}

	@Nonnull
	public List<SupportRole> findSupportRolesByProviderId(@Nullable UUID providerId) {
		if (providerId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT sr.* FROM support_role sr, provider_support_role psr " +
				"WHERE psr.provider_id=? AND psr.support_role_id=sr.support_role_id ORDER BY sr.description", SupportRole.class, providerId);
	}

	@Nonnull
	public List<SupportRole> findSupportRolesByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT DISTINCT sr.*
				FROM support_role sr, provider_support_role psr, provider p
				WHERE sr.support_role_id=psr.support_role_id
				AND psr.provider_id=p.provider_id
				AND p.institution_id=?
				AND p.active=TRUE
				ORDER BY sr.description
				""", SupportRole.class, institutionId);
	}

	@Nonnull
	public List<PaymentFunding> findPaymentFundings() {
		return getDatabase().queryForList("SELECT * FROM payment_funding ORDER BY description", PaymentFunding.class);
	}

	@Nonnull
	public List<PaymentFunding> findPaymentFundingsByProviderId(@Nullable UUID providerId) {
		if (providerId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT DISTINCT pf.* FROM payment_funding pf, provider_payment_type ppt, payment_type pt " +
				"WHERE ppt.provider_id=? AND ppt.payment_type_id=pt.payment_type_id AND pt.payment_funding_id=pf.payment_funding_id " +
				"ORDER BY pf.description", PaymentFunding.class, providerId);
	}

	@Nonnull
	public List<Specialty> findSpecialtiesByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM specialty WHERE institution_id=? ORDER BY display_order",
				Specialty.class, institutionId);
	}

	@Nonnull
	public List<Specialty> findSpecialtiesByProviderId(@Nullable UUID providerId) {
		if (providerId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT s.* FROM specialty s, provider_specialty sp " +
						"WHERE sp.provider_id=? AND sp.specialty_id=s.specialty_id ORDER BY s.display_order",
				Specialty.class, providerId);
	}

	@Nonnull
	public Map<UUID, List<Specialty>> specialtiesByProviderIdForInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyMap();

		List<SpecialtyWithProviderId> specialtiesWithProviderId = getDatabase().queryForList(
				"SELECT s.*, ps.provider_id FROM specialty s, provider_specialty ps, provider p " +
						"WHERE s.specialty_id=ps.specialty_id AND ps.provider_id=p.provider_id AND p.institution_id=?",
				SpecialtyWithProviderId.class, institutionId);

		// Transform flat resultset into a map
		Map<UUID, List<Specialty>> specialtiesByProviderId = new HashMap<>(specialtiesWithProviderId.size());

		for (SpecialtyWithProviderId specialtyWithProviderId : specialtiesWithProviderId) {
			List<Specialty> specialties = specialtiesByProviderId.get(specialtyWithProviderId.getProviderId());

			if (specialties == null) {
				specialties = new ArrayList<Specialty>();
				specialtiesByProviderId.put(specialtyWithProviderId.getProviderId(), specialties);
			}

			specialties.add(specialtyWithProviderId);
		}

		return specialtiesByProviderId;
	}

	@Nonnull
	public Optional<EpicDepartment> findEpicDepartmentById(@Nullable UUID epicDepartmentId) {
		if (epicDepartmentId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM epic_department
				WHERE epic_department_id=?
				""", EpicDepartment.class, epicDepartmentId);
	}

	@Nonnull
	public List<EpicDepartment> findEpicDepartmentsByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM epic_department
				WHERE institution_id=?
				ORDER BY name, department_id
				""", EpicDepartment.class, institutionId);
	}

	@Nonnull
	public Boolean updateEpicDepartment(@Nonnull UpdateEpicDepartmentRequest request) {
		requireNonNull(request);

		UUID epicDepartmentId = request.getEpicDepartmentId();
		DepartmentAvailabilityStatusId departmentAvailabilityStatusId = request.getDepartmentAvailabilityStatusId();
		ValidationException validationException = new ValidationException();

		if (epicDepartmentId == null)
			validationException.add(new FieldError("epicDepartmentId", getStrings().get("Epic Department ID is required.")));

		if (departmentAvailabilityStatusId == null)
			validationException.add(new FieldError("departmentAvailabilityStatusId", getStrings().get("Department Availability Status ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		return getDatabase().execute("""
				UPDATE epic_department
				SET department_availability_status_id=?
				WHERE epic_department_id=?
				""", departmentAvailabilityStatusId, epicDepartmentId) > 0;
	}

	@NotThreadSafe
	protected static class SpecialtyWithProviderId extends Specialty {
		@Nullable
		private UUID providerId;

		@Nullable
		public UUID getProviderId() {
			return providerId;
		}

		public void setProviderId(@Nullable UUID providerId) {
			this.providerId = providerId;
		}
	}

	@Nonnull
	protected Set<ZoneId> determineProviderTimeZones() {
		// Basically USA-only for now
		return Set.of(
				ZoneId.of("America/Anchorage"),
				ZoneId.of("America/Juneau"),
				ZoneId.of("America/Metlakatla"),
				ZoneId.of("America/Nome"),
				ZoneId.of("America/Sitka"),
				ZoneId.of("America/Yakutat"),
				ZoneId.of("America/Puerto_Rico"),
				ZoneId.of("America/Chicago"),
				ZoneId.of("America/Indiana/Knox"),
				ZoneId.of("America/Indiana/Tell_City"),
				ZoneId.of("America/Menominee"),
				ZoneId.of("America/North_Dakota/Beulah"),
				ZoneId.of("America/North_Dakota/Center"),
				ZoneId.of("America/North_Dakota/New_Salem"),
				ZoneId.of("America/Detroit"),
				ZoneId.of("America/Fort_Wayne"),
				ZoneId.of("America/Indiana/Indianapolis"),
				ZoneId.of("America/Kentucky/Louisville"),
				ZoneId.of("America/Kentucky/Monticello"),
				ZoneId.of("America/New_York"),
				ZoneId.of("America/Adak"),
				ZoneId.of("America/Atka"),
				ZoneId.of("America/Boise"),
				ZoneId.of("America/Denver"),
				ZoneId.of("America/Phoenix"),
				ZoneId.of("America/Shiprock"),
				ZoneId.of("America/Los_Angeles")
		);
	}

	@Nonnull
	public List<Interaction> findInteractionsByTypeAndProviderId(InteractionType.InteractionTypeId interactionTypeId, UUID providerId) {
		return getDatabase().queryForList("SELECT i.* FROM interaction i, provider_interaction pi WHERE i.interaction_id = pi.interaction_id " +
				"AND pi.provider_id = ? AND i.interaction_type_id = ? ", Interaction.class, providerId, interactionTypeId);
	}

	public void synchronizeEpicProviderSlotBookingRequests(@Nullable List<SynchronizeEpicProviderSlotBookingRequest> requests) {
		if (requests == null || requests.size() == 0)
			return;

		ValidationException validationException = new ValidationException();

		// First, validate the data
		int i = 0;

		for (SynchronizeEpicProviderSlotBookingRequest request : requests) {
			if (request == null) {
				validationException.add(getStrings().get("Request index {{index}} is missing.", Map.of("index", i)));
			} else {
				if (request.getProviderId() == null)
					validationException.add(new FieldError(format("providerId[%d]", i), getStrings().get("Provider ID is required.")));

				if (request.getContactId() == null)
					validationException.add(new FieldError(format("contactId[%d]", i), getStrings().get("Contact ID is required.")));

				if (request.getContactIdType() == null)
					validationException.add(new FieldError(format("contactIdType[%d]", i), getStrings().get("Contact ID Type is required.")));

				if (request.getDepartmentId() == null)
					validationException.add(new FieldError(format("departmentId[%d]", i), getStrings().get("Department ID is required.")));

				if (request.getDepartmentIdType() == null)
					validationException.add(new FieldError(format("departmentIdType[%d]", i), getStrings().get("Department ID Type is required.")));

				if (request.getVisitTypeId() == null)
					validationException.add(new FieldError(format("visitTypeId[%d]", i), getStrings().get("Visit Type ID is required.")));

				if (request.getVisitTypeIdType() == null)
					validationException.add(new FieldError(format("visitTypeIdType[%d]", i), getStrings().get("Visit Type ID Type is required.")));

				if (request.getStartDateTime() == null)
					validationException.add(new FieldError(format("startDateTime[%d]", i), getStrings().get("Start Date/Time is required.")));

				if (request.getEndDateTime() == null)
					validationException.add(new FieldError(format("endDateTime[%d]", i), getStrings().get("End Date/Time is required.")));

				if (request.getStartDateTime() != null && request.getEndDateTime() != null && request.getEndDateTime().isBefore(request.getStartDateTime()))
					validationException.add(getStrings().get("End Date/Time at index {{index}} is before Start Date/Time.", Map.of("index", i)));
			}

			++i;
		}

		if (validationException.hasErrors())
			throw validationException;

		// Group data so it's easier to work with
		Map<UUID, Set<SynchronizeEpicProviderSlotBookingRequest>> requestsByProviderId = requests.stream()
				.collect(Collectors.groupingBy(SynchronizeEpicProviderSlotBookingRequest::getProviderId, Collectors.toSet()));

		for (Entry<UUID, Set<SynchronizeEpicProviderSlotBookingRequest>> entry : requestsByProviderId.entrySet()) {
			UUID providerId = entry.getKey();
			Set<SynchronizeEpicProviderSlotBookingRequest> providerRequests = entry.getValue();

			// First, for this provider, delete all existing sync records for the dates we're choosing to sync
			Set<LocalDate> providerDates = providerRequests.stream()
					.map(providerRequest -> providerRequest.getStartDateTime().toLocalDate())
					.collect(Collectors.toSet());

			List<List<Object>> batchDeleteParameterGroups = new ArrayList<>(providerDates.size());

			for (LocalDate providerDate : providerDates) {
				LocalDateTime startOfDayDateTime = providerDate.atStartOfDay();
				LocalDateTime endOfDayDateTime = LocalDateTime.of(providerDate, LocalTime.MAX); // Date @ 23:59:59.999999999

				batchDeleteParameterGroups.add(List.of(
						providerId,
						startOfDayDateTime,
						endOfDayDateTime
				));
			}

			getDatabase().executeBatch("""
					DELETE FROM epic_provider_slot_booking
					WHERE provider_id=?
					AND start_date_time >= ?
					AND end_date_time <= ?
					""", batchDeleteParameterGroups);

			List<List<Object>> batchInsertParameterGroups = new ArrayList<>(providerRequests.size());

			// Then, insert the fresh data for this provider
			for (SynchronizeEpicProviderSlotBookingRequest request : providerRequests) {
				batchInsertParameterGroups.add(List.of(
						request.getProviderId(),
						normalizeEpicString(request.getContactId()),
						normalizeEpicString(request.getContactIdType()),
						normalizeEpicString(request.getDepartmentId()),
						normalizeEpicString(request.getDepartmentIdType()),
						normalizeEpicString(request.getVisitTypeId()),
						normalizeEpicString(request.getVisitTypeIdType()),
						request.getStartDateTime(),
						request.getEndDateTime()
				));
			}

			getDatabase().executeBatch("""
					INSERT INTO epic_provider_slot_booking(
						provider_id,
						contact_id,
						contact_id_type,
						department_id,
						department_id_type,
						visit_type_id,
						visit_type_id_type,
						start_date_time,
						end_date_time
					) VALUES (?,?,?,?,?,?,?,?,?)
					""", batchInsertParameterGroups);
		}
	}

	@Nonnull
	protected String normalizeEpicString(@Nonnull String epicString) {
		requireNonNull(epicString);
		return epicString.trim().toUpperCase(Locale.US);
	}

	@Nonnull
	protected AccountService getAccountService() {
		return accountServiceProvider.get();
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
	}

	@Nonnull
	protected AssessmentService getAssessmentService() {
		return assessmentServiceProvider.get();
	}

	@Nonnull
	protected ScreeningService getScreeningService() {
		return this.screeningServiceProvider.get();
	}

	@Nonnull
	protected SessionService getSessionService() {
		return sessionServiceProvider.get();
	}

	@Nonnull
	protected AssessmentScoringService getAssessmentScoringService() {
		return assessmentScoringServiceProvider.get();
	}

	@Nonnull
	protected ClinicService getClinicService() {
		return clinicServiceProvider.get();
	}

	@Nonnull
	protected AvailabilityService getAvailabilityService() {
		return availabilityServiceProvider.get();
	}

	@Nonnull
	protected EnterprisePluginProvider getEnterprisePluginProvider() {
		return this.enterprisePluginProvider;
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected AcuitySchedulingClient getAcuitySchedulingClient() {
		return acuitySchedulingClient;
	}

	@Nonnull
	protected AcuitySchedulingCache getAcuitySchedulingCache() {
		return acuitySchedulingCache;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	public Set<ZoneId> getProviderTimeZones() {
		return providerTimeZones;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}