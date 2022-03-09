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
import com.cobaltplatform.api.model.api.request.ProviderFindRequest;
import com.cobaltplatform.api.model.api.request.ProviderFindRequest.ProviderFindAvailability;
import com.cobaltplatform.api.model.api.request.ProviderFindRequest.ProviderFindLicenseType;
import com.cobaltplatform.api.model.api.request.ProviderFindRequest.ProviderFindSupplement;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSession;
import com.cobaltplatform.api.model.db.Appointment;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PaymentFunding;
import com.cobaltplatform.api.model.db.PaymentFunding.PaymentFundingId;
import com.cobaltplatform.api.model.db.PaymentType;
import com.cobaltplatform.api.model.db.Provider;
import com.cobaltplatform.api.model.db.ProviderAvailability;
import com.cobaltplatform.api.model.db.RecommendationLevel.RecommendationLevelId;
import com.cobaltplatform.api.model.db.SchedulingSystem;
import com.cobaltplatform.api.model.db.SchedulingSystem.SchedulingSystemId;
import com.cobaltplatform.api.model.db.Specialty;
import com.cobaltplatform.api.model.db.SupportRole;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.model.db.SystemAffinity.SystemAffinityId;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.cobaltplatform.api.model.db.Assessment;
import com.cobaltplatform.api.model.service.ProviderFind;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityDate;
import com.cobaltplatform.api.model.service.ProviderFind.AvailabilityStatus;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlInListPlaceholders;
import static com.cobaltplatform.api.util.DatabaseUtility.sqlVaragsParameters;
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
	private final javax.inject.Provider<AssessmentService> assessmentServiceProvider;
	@Nonnull
	private final javax.inject.Provider<SessionService> sessionServiceProvider;
	@Nonnull
	private final javax.inject.Provider<AssessmentScoringService> assessmentScoringServiceProvider;
	@Nonnull
	private final javax.inject.Provider<ClinicService> clinicServiceProvider;
	@Nonnull
	private final javax.inject.Provider<AvailabilityService> availabilityServiceProvider;
	@Nonnull
	private final Database database;
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
	public ProviderService(@Nonnull javax.inject.Provider<AssessmentService> assessmentServiceProvider,
												 @Nonnull javax.inject.Provider<SessionService> sessionServiceProvider,
												 @Nonnull javax.inject.Provider<AssessmentScoringService> assessmentScoringServiceProvider,
												 @Nonnull javax.inject.Provider<ClinicService> clinicServiceProvider,
												 @Nonnull javax.inject.Provider<AvailabilityService> availabilityServiceProvider,
												 @Nonnull Database database,
												 @Nonnull Configuration configuration,
												 @Nonnull AcuitySchedulingClient acuitySchedulingClient,
												 @Nonnull AcuitySchedulingCache acuitySchedulingCache,
												 @Nonnull Strings strings) {
		requireNonNull(assessmentServiceProvider);
		requireNonNull(sessionServiceProvider);
		requireNonNull(assessmentScoringServiceProvider);
		requireNonNull(clinicServiceProvider);
		requireNonNull(availabilityServiceProvider);
		requireNonNull(database);
		requireNonNull(configuration);
		requireNonNull(acuitySchedulingClient);
		requireNonNull(acuitySchedulingCache);
		requireNonNull(strings);

		this.assessmentServiceProvider = assessmentServiceProvider;
		this.sessionServiceProvider = sessionServiceProvider;
		this.assessmentScoringServiceProvider = assessmentScoringServiceProvider;
		this.clinicServiceProvider = clinicServiceProvider;
		this.availabilityServiceProvider = availabilityServiceProvider;
		this.database = database;
		this.configuration = configuration;
		this.acuitySchedulingClient = acuitySchedulingClient;
		this.acuitySchedulingCache = acuitySchedulingCache;
		this.strings = strings;
		this.providerTimeZones = determineProviderTimeZones();
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<Provider> findProviderById(@Nullable UUID providerId) {
		if (providerId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM provider WHERE provider_id=?", Provider.class, providerId);
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
		LocalDateTime currentDateTime = LocalDateTime.now(account.getTimeZone());
		LocalDate currentDate = currentDateTime.toLocalDate();
		ValidationException validationException = new ValidationException();

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		List<Provider> providers;

		// If provider ID is specified, ignore the rest
		if (providerId != null) {
			providers = getDatabase().queryForList("SELECT * FROM provider WHERE provider_id=?", Provider.class, providerId);
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

			query.append(" WHERE institution_id=? AND p.active=TRUE");
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
			} else {
				// Special case: don't show "calming an anxious mind" providers unless that clinic is explicitly asked for via clinicId
				query.append(" AND NOT EXISTS (SELECT * FROM provider_clinic WHERE p.provider_id=provider_clinic.provider_id AND provider_clinic.clinic_id=?)");
				parameters.add(getClinicService().getCalmingAnAnxiousMindClinicId());
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

			query.append(" AND p.system_affinity_id=? ");
			parameters.add(systemAffinityId);

			query.append(" ORDER BY p.name");

			providers = getDatabase().queryForList(query.toString(), Provider.class, parameters.toArray(new Object[]{}));

			if (getLogger().isTraceEnabled()) {
				getLogger().trace("Query: {}\nParameters: {}", query.toString(), parameters);
				getLogger().trace("Providers: {}", providers.stream().map(provider -> provider.getName()).collect(Collectors.toList()));
			}
		}

		if (providers.size() == 0)
			return Collections.emptyList();

		// Single query to pull in all specialties for all providers in the resultset
		Map<UUID, List<Specialty>> specialtiesByProviderId = specialtiesByProviderIdForProviderIds(providers.stream()
				.map(provider -> provider.getProviderId())
				.collect(Collectors.toSet()));

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

		// Keep track of all psychiatrist IDs so we can determine which resultset values need the "phone number required" field to be set
		Set<UUID> psychiatristProviderIds = new HashSet<>(getDatabase().queryForList("SELECT p.provider_id FROM provider p, provider_support_role psr " +
				"WHERE psr.provider_id=p.provider_id and psr.support_role_id=?", UUID.class, SupportRoleId.PSYCHIATRIST));

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

		// Different code path for Cobalt Native scheduling: calculate synthetic "provider availability" records from logical availability data
		Map<UUID, List<ProviderAvailability>> nativeSchedulingProviderAvailabilitiesByProviderId = getAvailabilityService().nativeSchedulingProviderAvailabilitiesByProviderId(
				providers.stream()
						.filter(provider -> provider.getSchedulingSystemId() == SchedulingSystemId.COBALT)
						.map(provider -> provider.getProviderId())
						.collect(Collectors.toSet()), visitTypeIds, currentDateTime, currentDateTime.plusMonths(1) /* arbitrarily cap at 1 month ahead */);

		Set<UUID> nativeSchedulingProviderIds = nativeSchedulingProviderAvailabilitiesByProviderId.keySet();

		for (Provider provider : providers) {
			boolean intakeAssessmentRequired = false;
			boolean intakeAssessmentIneligible = false;

			Optional<Assessment> intakeAssessment = getAssessmentService().findIntakeAssessmentByProviderId(provider.getProviderId(), null);
			intakeAssessmentRequired = intakeAssessment.isPresent();


			if (intakeAssessment.isPresent()) {
				Optional<AccountSession> accountSession = getSessionService()
						.findCurrentIntakeAssessmentForAccountAndProvider(account, provider.getProviderId(), null, true);
				if (accountSession.isPresent())
					intakeAssessmentIneligible = !getAssessmentScoringService().isBookingAllowed(accountSession.get());
			}

			List<AvailabilityDate> dates = new ArrayList<>();
			List<ProviderAvailability> providerAvailabilities;

			if(provider.getSchedulingSystemId() == SchedulingSystemId.COBALT) {
				// Different code path for Cobalt native scheduling: use synthetic "provider availability" records
				providerAvailabilities = nativeSchedulingProviderAvailabilitiesByProviderId.get(provider.getProviderId());

				if(providerAvailabilities == null)
					providerAvailabilities = Collections.emptyList();
			} else {
				// First, fill in "available" slots based on what we know from Acuity
				StringBuilder providerAvailabilityQuery = new StringBuilder("SELECT pa.* FROM provider_availability pa, v_appointment_type at WHERE pa.provider_id=? AND pa.appointment_type_id=at.appointment_type_id ");
				List<Object> providerAvailabilityParameters = new ArrayList<>();
				providerAvailabilityParameters.add(provider.getProviderId());

				if (visitTypeIds.size() > 0) {
					providerAvailabilityQuery.append(" AND at.visit_type_id IN ");
					providerAvailabilityQuery.append(sqlInListPlaceholders(visitTypeIds));

					providerAvailabilityParameters.addAll(visitTypeIds);
				}

				providerAvailabilities = getDatabase().queryForList(providerAvailabilityQuery.toString(), ProviderAvailability.class, providerAvailabilityParameters.toArray(new Object[]{}));
			}

			// Keep track of all epic department IDs for this provider
			Set<UUID> epicDepartmentIds = new HashSet<>();
			epicDepartmentIdsByProviderId.put(provider.getProviderId(), epicDepartmentIds);

			Map<LocalDate, AvailabilityDate> availabilityDatesByDate = new HashMap<>(14);
			Map<LocalDateTime, ProviderFind.AvailabilityTime> availabilityTimesByDateTime = new HashMap<>();

			for (ProviderAvailability providerAvailability : providerAvailabilities) {
				LocalDateTime dateTime = providerAvailability.getDateTime();
				LocalDate date = dateTime.toLocalDate();
				LocalTime time = dateTime.toLocalTime();

				// Respect "day of week" filter
				if (daysOfWeek.size() > 0 && !daysOfWeek.contains(date.getDayOfWeek()))
					continue;

				// Respect "start date" filter
				if (startDate != null && date.isBefore(startDate))
					continue;

				// Respect "end date" filter
				if (endDate != null && date.isAfter(endDate))
					continue;

				// Respect "start time" filter
				if (startTime != null && time.isBefore(startTime))
					continue;

				// Respect "end time" filter
				if (endTime != null && time.isAfter(endTime))
					continue;

				// Don't include anything that is "today"
				if (!date.isAfter(currentDate))
					continue;

				AvailabilityDate availabilityDate = availabilityDatesByDate.get(date);

				if (availabilityDate == null) {
					availabilityDate = new AvailabilityDate();
					availabilityDate.setDate(date);
					availabilityDatesByDate.put(date, availabilityDate);
					dates.add(availabilityDate);
				}

				List<ProviderFind.AvailabilityTime> availabilityTimes = availabilityDate.getTimes();

				if (availabilityTimes == null) {
					availabilityTimes = new ArrayList<>();
					availabilityDate.setTimes(availabilityTimes);
				}

				ProviderFind.AvailabilityTime availabilityTime = availabilityTimesByDateTime.get(dateTime);

				if (availabilityTime == null) {
					availabilityTime = new ProviderFind.AvailabilityTime();
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

				if (providerAvailability.getEpicDepartmentId() != null)
					epicDepartmentIds.add(providerAvailability.getEpicDepartmentId());
			}

			if (availability == ProviderFindAvailability.ALL) {
				// Next, fill in "booked" slots using appointments on file in our DB.
				// Ignore times before now!
				LocalDateTime rightNow = LocalDateTime.now(provider.getTimeZone());
				List<Appointment> appointments = getDatabase().queryForList("SELECT * FROM appointment WHERE provider_id=? AND start_time > ? AND canceled=FALSE", Appointment.class, provider.getProviderId(), rightNow);

				for (Appointment appointment : appointments) {
					LocalDateTime dateTime = appointment.getStartTime();
					LocalDate date = dateTime.toLocalDate();
					LocalTime time = dateTime.toLocalTime();

					// Respect "day of week" filter
					if (daysOfWeek.size() > 0 && !daysOfWeek.contains(date.getDayOfWeek()))
						continue;

					// Respect "start date" filter
					if (startDate != null && date.isBefore(startDate))
						continue;

					// Respect "end date" filter
					if (endDate != null && date.isAfter(endDate))
						continue;

					// Respect "start time" filter
					if (startTime != null && time.isBefore(startTime))
						continue;

					// Respect "end time" filter
					if (endTime != null && time.isAfter(endTime))
						continue;

					AvailabilityDate availabilityDate = availabilityDatesByDate.get(date);

					if (availabilityDate == null) {
						availabilityDate = new AvailabilityDate();
						availabilityDate.setDate(date);
						availabilityDatesByDate.put(date, availabilityDate);
						dates.add(availabilityDate);
					}

					List<ProviderFind.AvailabilityTime> availabilityTimes = availabilityDate.getTimes();

					if (availabilityTimes == null) {
						availabilityTimes = new ArrayList<>();
						availabilityDate.setTimes(availabilityTimes);
					}

					ProviderFind.AvailabilityTime availabilityTime = availabilityTimesByDateTime.get(dateTime);

					// Fix up disconnect in the event of our DB being out of sync with what's live in Acuity (we sync every 10 mins or so).
					// BOOKED should trump AVAILABLE.
					// If availability time is not null here, that means there is already an AVAILABLE record and it should
					// instead be overwritten with BOOKED
					if (availabilityTime == null) {
						availabilityTime = new ProviderFind.AvailabilityTime();
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

			// Finally, sort everything and add in "fullyBooked" flags to make it easier on UI
			Collections.sort(dates, (date1, date2) -> date1.getDate().compareTo(date2.getDate()));

			for (AvailabilityDate availabilityDate : dates) {
				Collections.sort(availabilityDate.getTimes(), (time1, time2) -> time1.getTime().compareTo(time2.getTime()));

				boolean fullyBooked = true;

				for (ProviderFind.AvailabilityTime availabilityTime : availabilityDate.getTimes())
					Collections.sort(availabilityTime.getAppointmentTypeIds());

				for (ProviderFind.AvailabilityTime availabilityTime : availabilityDate.getTimes()) {
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
			providerFind.setName(provider.getName());
			providerFind.setTitle(provider.getTitle());
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

			if (psychiatristProviderIds.contains(provider.getProviderId()))
				providerFind.setPhoneNumberRequiredForAppointment(true);

			List<ProviderSupportRole> currentProviderSupportRoles = providerSupportRolesByProviderId.get(provider.getProviderId());

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

			providerFinds.add(providerFind);
		}

		return providerFinds;
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
	public List<SupportRole> findSupportRolesByInstitutionId(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);

		// For now - we don't care about institution.  We might later on
		return getDatabase().queryForList("SELECT * FROM support_role ORDER BY display_order", SupportRole.class);
	}

	@Nonnull
	public List<SupportRole> findRecommendedSupportRolesByRecommendationLevelId(@Nullable RecommendationLevelId recommendationLevelId) {
		if (recommendationLevelId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT sr.* FROM support_role sr, recommendation_level_to_support_role rlsr " +
				"WHERE sr.support_role_id=rlsr.support_role_id AND rlsr.recommendation_level_id=?", SupportRole.class, recommendationLevelId);
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
	public Map<UUID, List<Specialty>> specialtiesByProviderIdForProviderIds(@Nullable Set<UUID> providerIds) {
		if (providerIds == null || providerIds.size() == 0)
			return Collections.emptyMap();

		// Use subquery expression for better performance than in "in" list.
		// This will not be efficient for set sizes in the thousands, but in practice that is a nonissue for us.
		// See https://dba.stackexchange.com/a/91539

		// e.g. "('b795f6b5-3709-48aa-a294-91ed049ccce0'), ('32433795-ad52-4605-8c90-39d30d3dab23'), ..."
		String subqueryExpressionValues = providerIds.stream()
				.map(providerId -> format("('%s'::uuid)", providerId))
				.collect(Collectors.joining(", "));

		List<SpecialtyWithProviderId> specialtiesWithProviderId = getDatabase().queryForList(
				format("SELECT s.*, ps.provider_id FROM specialty s, provider_specialty ps " +
						"WHERE s.specialty_id=ps.specialty_id AND ps.provider_id IN (VALUES %s)", subqueryExpressionValues),
				SpecialtyWithProviderId.class);

		// Transform flat resultset into a map
		Map<UUID, List<Specialty>> specialtiesByProviderId = new HashMap<>(providerIds.size());

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
	protected AssessmentService getAssessmentService() {
		return assessmentServiceProvider.get();
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
	protected Database getDatabase() {
		return database;
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