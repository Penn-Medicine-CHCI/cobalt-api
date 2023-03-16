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
import com.cobaltplatform.api.model.api.response.AccountApiResponse.AccountApiResponseFactory;
import com.cobaltplatform.api.model.api.response.AddressApiResponse.AddressApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderDiagnosisApiResponse.PatientOrderDiagnosisApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderMedicationApiResponse.PatientOrderMedicationApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderNoteApiResponse.PatientOrderNoteApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderOutreachApiResponse.PatientOrderOutreachApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningSessionApiResponse.ScreeningSessionApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Address;
import com.cobaltplatform.api.model.db.BirthSex.BirthSexId;
import com.cobaltplatform.api.model.db.Ethnicity.EthnicityId;
import com.cobaltplatform.api.model.db.GenderIdentity.GenderIdentityId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.PatientOrderCareType;
import com.cobaltplatform.api.model.db.PatientOrderCareType.PatientOrderCareTypeId;
import com.cobaltplatform.api.model.db.PatientOrderClosureReason.PatientOrderClosureReasonId;
import com.cobaltplatform.api.model.db.PatientOrderFocusType;
import com.cobaltplatform.api.model.db.PatientOrderFocusType.PatientOrderFocusTypeId;
import com.cobaltplatform.api.model.db.PatientOrderScreeningStatus.PatientOrderScreeningStatusId;
import com.cobaltplatform.api.model.db.PatientOrderStatus.PatientOrderStatusId;
import com.cobaltplatform.api.model.db.PatientOrderTriage;
import com.cobaltplatform.api.model.db.Race.RaceId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.service.ScreeningSessionResult;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AddressService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.PatientOrderService;
import com.cobaltplatform.api.service.ScreeningService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.Normalizer;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class PatientOrderApiResponse {
	@Nonnull
	private UUID patientOrderId;
	@Nullable
	private PatientOrderStatusId patientOrderStatusId;
	@Nullable
	private PatientOrderScreeningStatusId patientOrderScreeningStatusId;
	@Nullable
	private UUID patientAccountId;
	@Nullable
	private UUID patientAddressId;
	@Nullable
	private UUID panelAccountId;
	@Nullable
	private PatientOrderClosureReasonId patientOrderClosureReasonId;
	@Nullable
	private String encounterDepartmentId;
	@Nullable
	private String encounterDepartmentIdType;
	@Nullable
	private String encounterDepartmentName;
	@Nullable
	private String referringPracticeId;
	@Nullable
	private String referringPracticeIdType;
	@Nullable
	private String referringPracticeName;
	@Nullable
	private String orderingProviderId;
	@Nullable
	private String orderingProviderIdType;
	@Nullable
	private String orderingProviderLastName;
	@Nullable
	private String orderingProviderFirstName;
	@Nullable
	private String orderingProviderMiddleName;
	@Nullable
	private String orderingProviderDisplayName;
	@Nullable
	private String orderingProviderDisplayNameWithLastFirst;
	@Nullable
	private String billingProviderId;
	@Nullable
	private String billingProviderIdType;
	@Nullable
	private String billingProviderLastName;
	@Nullable
	private String billingProviderFirstName;
	@Nullable
	private String billingProviderMiddleName;
	@Nullable
	private String billingProviderDisplayName;
	@Nullable
	private String billingProviderDisplayNameWithLastFirst;
	@Nullable
	private String patientLastName;
	@Nullable
	private String patientFirstName;
	@Nullable
	private String patientDisplayName;
	@Nullable
	private String patientDisplayNameWithLastFirst;
	@Nullable
	private String patientMrn;
	@Nullable
	private String patientId;
	@Nullable
	private String patientIdType;
	@Nullable
	private BirthSexId patientBirthSexId;
	@Nullable
	private LocalDate patientBirthdate;
	@Nullable
	private String patientBirthdateDescription;
	@Nullable
	private String primaryPayorId;
	@Nullable
	private String primaryPayorName;
	@Nullable
	private String primaryPlanId;
	@Nullable
	private String primaryPlanName;
	@Nullable
	private LocalDate orderDate;
	@Nullable
	private String orderDateDescription;
	@Nullable
	private Integer orderAgeInMinutes;
	@Nullable
	private String orderAgeInMinutesDescription;
	@Nullable
	private String orderId;
	@Nullable
	private String routing;
	@Nullable
	private String reasonForReferral;
	@Nullable
	private String associatedDiagnosis;
	@Nullable
	private String patientPhoneNumber;
	@Nullable
	private String patientPhoneNumberDescription;
	@Nullable
	private EthnicityId patientEthnicityId;
	@Nullable
	private RaceId patientRaceId;
	@Nullable
	private GenderIdentityId patientGenderIdentityId;
	@Nullable
	private String patientLanguageCode;
	@Nullable
	private String patientEmailEddress;
	@Nullable
	private String preferredContactHours;
	@Nullable
	private String comments;
	@Nullable
	private String ccRecipients;
	@Nullable
	private String lastActiveMedicationOrderSummary;
	@Nullable
	private String medications;
	@Nullable
	private String recentPsychotherapeuticMedications;
	@Nullable
	private Instant episodeEndedAt;
	@Nullable
	private String episodeEndedAtDescription;
	@Nullable
	private Integer episodeDurationInDays;
	@Nullable
	private String episodeDurationInDaysDescription;
	@Nullable
	private Boolean safetyPlanningNeeded;
	@Nullable
	private Boolean outreachNeeded;
	@Nullable
	private Boolean followupNeeded;
	@Nullable
	private Boolean resourcesSent;
	@Nullable
	private Instant resourcesSentAt;
	@Nullable
	private String resourcesSentAtDescription;

	@Nullable
	private AddressApiResponse patientAddress;
	@Nullable
	private AccountApiResponse patientAccount;
	@Nullable
	private List<PatientOrderDiagnosisApiResponse> patientOrderDiagnoses;
	@Nullable
	private List<PatientOrderMedicationApiResponse> patientOrderMedications;
	@Nullable
	private List<PatientOrderNoteApiResponse> patientOrderNotes;
	@Nullable
	private List<PatientOrderOutreachApiResponse> patientOrderOutreaches;
	@Nullable
	private List<PatientOrderTriageGroupApiResponse> patientOrderTriageGroups;
	@Nullable
	private ScreeningSessionApiResponse screeningSession;
	@Nullable
	private ScreeningSessionResult screeningSessionResult;

	// Included in v_patient_order

	@Nullable
	private PatientOrderCareTypeId patientOrderCareTypeId;
	@Nullable
	private String patientOrderCareTypeDescription;
	@Nullable
	private Integer outreachCount;
	@Nullable
	private String outreachCountDescription;
	@Nullable
	private LocalDateTime mostRecentOutreachDateTime;
	@Nullable
	private String mostRecentOutreachDateTimeDescription;
	@Nullable
	private UUID mostRecentScreeningSessionId;
	@Nullable
	private UUID mostRecentScreeningSessionCreatedByAccountId;
	@Nullable
	private String mostRecentScreeningSessionCreatedByAccountFirstName;
	@Nullable
	private String mostRecentScreeningSessionCreatedByAccountLastName;
	@Nullable
	private String mostRecentScreeningSessionCreatedByAccountDisplayName;
	@Nullable
	private String mostRecentScreeningSessionCreatedByAccountDisplayNameWithLastFirst;
	@Nullable
	private Boolean mostRecentScreeningSessionCompleted;
	@Nullable
	private Instant mostRecentScreeningSessionCompletedAt;
	@Nullable
	private String mostRecentScreeningSessionCompletedAtDescription;
	@Nullable
	private String panelAccountFirstName;
	@Nullable
	private String panelAccountLastName;
	@Nullable
	private String panelAccountDisplayName;
	@Nullable
	private String panelAccountDisplayNameWithLastFirst;

	public enum PatientOrderApiResponseSupplement {
		MINIMAL,
		PANEL,
		EVERYTHING
	}

	public enum PatientOrderApiResponseFormat {
		MHIC,
		PATIENT;

		@Nonnull
		public static PatientOrderApiResponseFormat fromRoleId(@Nonnull RoleId roleId) {
			requireNonNull(roleId);

			if (roleId == RoleId.MHIC || roleId == RoleId.ADMINISTRATOR || roleId == RoleId.PROVIDER)
				return MHIC;

			return PATIENT;
		}
	}

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface PatientOrderApiResponseFactory {
		@Nonnull
		PatientOrderApiResponse create(@Nonnull PatientOrder patientOrder,
																	 @Nonnull PatientOrderApiResponseFormat format);

		@Nonnull
		PatientOrderApiResponse create(@Nonnull PatientOrder patientOrder,
																	 @Nonnull PatientOrderApiResponseFormat format,
																	 @Nonnull Set<PatientOrderApiResponseSupplement> supplements);
	}

	@AssistedInject
	public PatientOrderApiResponse(@Nonnull PatientOrderService patientOrderService,
																 @Nonnull AccountService accountService,
																 @Nonnull AddressService addressService,
																 @Nonnull InstitutionService institutionService,
																 @Nonnull ScreeningService screeningService,
																 @Nonnull AccountApiResponseFactory accountApiResponseFactory,
																 @Nonnull PatientOrderNoteApiResponseFactory patientOrderNoteApiResponseFactory,
																 @Nonnull PatientOrderOutreachApiResponseFactory patientOrderOutreachApiResponseFactory,
																 @Nonnull PatientOrderDiagnosisApiResponseFactory patientOrderDiagnosisApiResponseFactory,
																 @Nonnull PatientOrderMedicationApiResponseFactory patientOrderMedicationApiResponseFactory,
																 @Nonnull ScreeningSessionApiResponseFactory screeningSessionApiResponseFactory,
																 @Nonnull AddressApiResponseFactory addressApiResponseFactory,
																 @Nonnull Formatter formatter,
																 @Nonnull Strings strings,
																 @Nonnull Provider<CurrentContext> currentContextProvider,
																 @Assisted @Nonnull PatientOrder patientOrder,
																 @Assisted @Nonnull PatientOrderApiResponseFormat format) {
		this(patientOrderService,
				accountService,
				addressService,
				institutionService,
				screeningService,
				accountApiResponseFactory,
				patientOrderNoteApiResponseFactory,
				patientOrderOutreachApiResponseFactory,
				patientOrderDiagnosisApiResponseFactory,
				patientOrderMedicationApiResponseFactory,
				screeningSessionApiResponseFactory,
				addressApiResponseFactory,
				formatter,
				strings,
				currentContextProvider,
				patientOrder,
				format,
				Set.of());
	}

	@AssistedInject
	public PatientOrderApiResponse(@Nonnull PatientOrderService patientOrderService,
																 @Nonnull AccountService accountService,
																 @Nonnull AddressService addressService,
																 @Nonnull InstitutionService institutionService,
																 @Nonnull ScreeningService screeningService,
																 @Nonnull AccountApiResponseFactory accountApiResponseFactory,
																 @Nonnull PatientOrderNoteApiResponseFactory patientOrderNoteApiResponseFactory,
																 @Nonnull PatientOrderOutreachApiResponseFactory patientOrderOutreachApiResponseFactory,
																 @Nonnull PatientOrderDiagnosisApiResponseFactory patientOrderDiagnosisApiResponseFactory,
																 @Nonnull PatientOrderMedicationApiResponseFactory patientOrderMedicationApiResponseFactory,
																 @Nonnull ScreeningSessionApiResponseFactory screeningSessionApiResponseFactory,
																 @Nonnull AddressApiResponseFactory addressApiResponseFactory,
																 @Nonnull Formatter formatter,
																 @Nonnull Strings strings,
																 @Nonnull Provider<CurrentContext> currentContextProvider,
																 @Assisted @Nonnull PatientOrder patientOrder,
																 @Assisted @Nonnull PatientOrderApiResponseFormat format,
																 @Assisted @Nonnull Set<PatientOrderApiResponseSupplement> supplements) {
		requireNonNull(patientOrderService);
		requireNonNull(accountService);
		requireNonNull(addressService);
		requireNonNull(institutionService);
		requireNonNull(screeningService);
		requireNonNull(accountApiResponseFactory);
		requireNonNull(patientOrderNoteApiResponseFactory);
		requireNonNull(patientOrderOutreachApiResponseFactory);
		requireNonNull(patientOrderDiagnosisApiResponseFactory);
		requireNonNull(patientOrderMedicationApiResponseFactory);
		requireNonNull(screeningSessionApiResponseFactory);
		requireNonNull(addressApiResponseFactory);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);
		requireNonNull(patientOrder);
		requireNonNull(format);
		requireNonNull(supplements);

		CurrentContext currentContext = currentContextProvider.get();

		AddressApiResponse patientAddress = null;
		AccountApiResponse patientAccount = null;
		List<PatientOrderDiagnosisApiResponse> patientOrderDiagnoses = null;
		List<PatientOrderMedicationApiResponse> patientOrderMedications = null;
		List<PatientOrderNoteApiResponse> patientOrderNotes = null;
		List<PatientOrderOutreachApiResponse> patientOrderOutreaches = null;

		if (supplements.contains(PatientOrderApiResponseSupplement.EVERYTHING)) {
			Address address = addressService.findAddressById(patientOrder.getPatientAddressId()).orElse(null);
			patientAddress = address == null ? null : addressApiResponseFactory.create(address);

			Account account = accountService.findAccountById(patientOrder.getPatientAccountId()).orElse(null);
			patientAccount = patientOrder.getPatientAccountId() == null ? null : accountApiResponseFactory.create(account);

			patientOrderDiagnoses = patientOrderService.findPatientOrderDiagnosesByPatientOrderId(patientOrder.getPatientOrderId()).stream()
					.map(patientOrderDiagnosis -> patientOrderDiagnosisApiResponseFactory.create(patientOrderDiagnosis))
					.collect(Collectors.toList());

			patientOrderMedications = patientOrderService.findPatientOrderMedicationsByPatientOrderId(patientOrder.getPatientOrderId()).stream()
					.map(patientOrderMedication -> patientOrderMedicationApiResponseFactory.create(patientOrderMedication))
					.collect(Collectors.toList());

			patientOrderNotes = patientOrderService.findPatientOrderNotesByPatientOrderId(patientOrder.getPatientOrderId()).stream()
					.map(patientOrderNote -> patientOrderNoteApiResponseFactory.create(patientOrderNote))
					.collect(Collectors.toList());

			patientOrderOutreaches = patientOrderService.findPatientOrderOutreachesByPatientOrderId(patientOrder.getPatientOrderId()).stream()
					.map(patientOrderOutreach -> patientOrderOutreachApiResponseFactory.create(patientOrderOutreach))
					.collect(Collectors.toList());

			List<ScreeningSession> screeningSessions = screeningService.findScreeningSessionsByPatientOrderId(patientOrder.getPatientOrderId());

			// Look for a completed screening session...
			ScreeningSession currentScreeningSession = null;
			ScreeningSession completedScreeningSession = screeningSessions.stream()
					.filter(screeningSession -> screeningSession.getCompleted())
					.findFirst()
					.orElse(null);

			// ...if no completed screening session, pick the first one in the list
			if (completedScreeningSession != null)
				currentScreeningSession = completedScreeningSession;
			else
				currentScreeningSession = screeningSessions.size() == 0 ? null : screeningSessions.get(0);

			this.screeningSession = currentScreeningSession == null ? null : screeningSessionApiResponseFactory.create(currentScreeningSession);
			this.screeningSessionResult = completedScreeningSession == null ? null : screeningService.findScreeningSessionResult(completedScreeningSession).get();

			// TODO: this doesn't take manual overrides into account...
			if (completedScreeningSession != null) {
				List<PatientOrderFocusType> patientOrderFocusTypes = patientOrderService.findPatientOrderFocusTypes();
				Map<PatientOrderFocusTypeId, PatientOrderFocusType> patientOrderFocusTypesById = patientOrderFocusTypes.stream()
						.collect(Collectors.toMap(PatientOrderFocusType::getPatientOrderFocusTypeId, patientOrderFocusType -> patientOrderFocusType));
				List<PatientOrderCareType> patientOrderCareTypes = patientOrderService.findPatientOrderCareTypes();
				Map<PatientOrderCareTypeId, PatientOrderCareType> patientOrderCareTypesById = patientOrderCareTypes.stream()
						.collect(Collectors.toMap(PatientOrderCareType::getPatientOrderCareTypeId, patientOrderCareType -> patientOrderCareType));
				List<PatientOrderTriage> patientOrderTriages = patientOrderService.findPatientOrderTriagesByPatientOrderId(patientOrder.getPatientOrderId(), completedScreeningSession.getScreeningSessionId());

				Map<Pair<PatientOrderFocusTypeId, PatientOrderCareTypeId>, List<PatientOrderTriage>> patientOrderTriagesByFocusAndCareTypeIds = new LinkedHashMap<>();

				for (PatientOrderTriage patientOrderTriage : patientOrderTriages) {
					Pair<PatientOrderFocusTypeId, PatientOrderCareTypeId> key = Pair.of(patientOrderTriage.getPatientOrderFocusTypeId(), patientOrderTriage.getPatientOrderCareTypeId());
					List<PatientOrderTriage> groupedPatientOrderTriages = patientOrderTriagesByFocusAndCareTypeIds.get(key);

					if (groupedPatientOrderTriages == null) {
						groupedPatientOrderTriages = new ArrayList<>();
						patientOrderTriagesByFocusAndCareTypeIds.put(key, groupedPatientOrderTriages);
					}

					groupedPatientOrderTriages.add(patientOrderTriage);
				}

				List<PatientOrderTriageGroupApiResponse> patientOrderTriageGroups = new ArrayList<>();

				for (Entry<Pair<PatientOrderFocusTypeId, PatientOrderCareTypeId>, List<PatientOrderTriage>> entry : patientOrderTriagesByFocusAndCareTypeIds.entrySet()) {
					Pair<PatientOrderFocusTypeId, PatientOrderCareTypeId> key = entry.getKey();
					List<PatientOrderTriage> value = entry.getValue();

					PatientOrderFocusType patientOrderFocusType = patientOrderFocusTypesById.get(key.getLeft());
					PatientOrderCareType patientOrderCareType = patientOrderCareTypesById.get(key.getRight());

					List<String> reasons = value.stream()
							.map(patientOrderTriage -> patientOrderTriage.getReason())
							.distinct()
							.collect(Collectors.toList());

					patientOrderTriageGroups.add(new PatientOrderTriageGroupApiResponse(patientOrderFocusType, patientOrderCareType, reasons));
				}

				this.patientOrderTriageGroups = patientOrderTriageGroups;
			}
		}

		// Always available to both patients and MHICs
		this.patientOrderId = patientOrder.getPatientOrderId();
		this.patientOrderStatusId = patientOrder.getPatientOrderStatusId();
		this.patientOrderScreeningStatusId = patientOrder.getPatientOrderScreeningStatusId();
		this.patientAccountId = patientOrder.getPatientAccountId();
		this.patientAddressId = patientOrder.getPatientAddressId();
		this.patientLastName = patientOrder.getPatientLastName();
		this.patientFirstName = patientOrder.getPatientFirstName();
		this.patientDisplayName = Normalizer.normalizeName(patientOrder.getPatientFirstName(), null, patientOrder.getPatientLastName()).orElse(null);
		this.patientDisplayNameWithLastFirst = Normalizer.normalizeNameWithLastFirst(patientOrder.getPatientFirstName(), null, patientOrder.getPatientLastName()).orElse(null);
		this.patientMrn = patientOrder.getPatientMrn();
		this.patientId = patientOrder.getPatientId();
		this.patientIdType = patientOrder.getPatientIdType();
		this.patientBirthSexId = patientOrder.getPatientBirthSexId();
		this.patientEthnicityId = patientOrder.getPatientEthnicityId();
		this.patientRaceId = patientOrder.getPatientRaceId();
		this.patientGenderIdentityId = patientOrder.getPatientGenderIdentityId();
		this.patientLanguageCode = patientOrder.getPatientLanguageCode();
		this.patientEmailEddress = patientOrder.getPatientEmailAddress();
		this.patientBirthdate = patientOrder.getPatientBirthdate();
		this.patientBirthdateDescription = patientOrder.getPatientBirthdate() == null ? null : formatter.formatDate(patientOrder.getPatientBirthdate(), FormatStyle.MEDIUM);
		this.patientPhoneNumber = patientOrder.getPatientPhoneNumber();
		this.patientPhoneNumberDescription = patientOrder.getPatientPhoneNumber() == null ? null : formatter.formatPhoneNumber(patientOrder.getPatientPhoneNumber(), currentContext.getLocale());
		this.patientAddress = patientAddress;
		this.patientAccount = patientAccount;

		// MHIC-only view of the data
		if (format == PatientOrderApiResponseFormat.MHIC) {
			this.panelAccountId = patientOrder.getPanelAccountId();
			this.patientOrderClosureReasonId = patientOrder.getPatientOrderClosureReasonId();
			this.encounterDepartmentId = patientOrder.getEncounterDepartmentId();
			this.encounterDepartmentIdType = patientOrder.getEncounterDepartmentIdType();
			this.encounterDepartmentName = patientOrder.getEncounterDepartmentName();
			this.referringPracticeId = patientOrder.getReferringPracticeId();
			this.referringPracticeIdType = patientOrder.getReferringPracticeIdType();
			this.referringPracticeName = patientOrder.getReferringPracticeName();
			this.orderingProviderId = patientOrder.getOrderingProviderId();
			this.orderingProviderIdType = patientOrder.getOrderingProviderIdType();
			this.orderingProviderLastName = patientOrder.getOrderingProviderLastName();
			this.orderingProviderFirstName = patientOrder.getOrderingProviderFirstName();
			this.orderingProviderMiddleName = patientOrder.getOrderingProviderMiddleName();
			this.orderingProviderDisplayName = Normalizer.normalizeName(patientOrder.getOrderingProviderFirstName(), patientOrder.getOrderingProviderMiddleName(), patientOrder.getOrderingProviderLastName()).orElse(null);
			this.orderingProviderDisplayNameWithLastFirst = Normalizer.normalizeNameWithLastFirst(patientOrder.getOrderingProviderFirstName(), patientOrder.getOrderingProviderMiddleName(), patientOrder.getOrderingProviderLastName()).orElse(null);
			this.billingProviderId = patientOrder.getBillingProviderId();
			this.billingProviderIdType = patientOrder.getBillingProviderIdType();
			this.billingProviderLastName = patientOrder.getBillingProviderLastName();
			this.billingProviderFirstName = patientOrder.getBillingProviderFirstName();
			this.billingProviderMiddleName = patientOrder.getBillingProviderMiddleName();
			this.billingProviderDisplayName = Normalizer.normalizeName(patientOrder.getBillingProviderFirstName(), patientOrder.getBillingProviderMiddleName(), patientOrder.getBillingProviderLastName()).orElse(null);
			this.billingProviderDisplayNameWithLastFirst = Normalizer.normalizeNameWithLastFirst(patientOrder.getBillingProviderFirstName(), patientOrder.getBillingProviderMiddleName(), patientOrder.getBillingProviderLastName()).orElse(null);
			this.primaryPayorId = patientOrder.getPrimaryPayorId();
			this.primaryPayorName = patientOrder.getPrimaryPayorName();
			this.primaryPlanId = patientOrder.getPrimaryPlanId();
			this.primaryPlanName = patientOrder.getPrimaryPlanName();
			this.orderDate = patientOrder.getOrderDate();
			this.orderDateDescription = patientOrder.getOrderDate() == null ? null : formatter.formatDate(patientOrder.getOrderDate(), FormatStyle.MEDIUM);
			this.orderAgeInMinutes = patientOrder.getOrderAgeInMinutes();
			this.orderAgeInMinutesDescription = patientOrder.getOrderAgeInMinutes() == null ? null : formatter.formatNumber(patientOrder.getOrderAgeInMinutes());
			this.orderId = patientOrder.getOrderId();
			this.routing = patientOrder.getRouting();
			this.reasonForReferral = patientOrder.getReasonForReferral();
			this.associatedDiagnosis = patientOrder.getAssociatedDiagnosis();
			this.preferredContactHours = patientOrder.getPreferredContactHours();
			this.comments = patientOrder.getComments();
			this.ccRecipients = patientOrder.getCcRecipients();
			this.lastActiveMedicationOrderSummary = patientOrder.getLastActiveMedicationOrderSummary();
			this.medications = patientOrder.getMedications();
			this.recentPsychotherapeuticMedications = patientOrder.getRecentPsychotherapeuticMedications();
			this.episodeEndedAt = patientOrder.getEpisodeEndedAt();
			this.episodeEndedAtDescription = patientOrder.getEpisodeEndedAt() == null
					? null
					: formatter.formatTimestamp(patientOrder.getEpisodeEndedAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);

			Institution institution = institutionService.findInstitutionById(patientOrder.getInstitutionId()).get();

			Instant orderDateTime = patientOrder.getOrderDate()
					.atTime(LocalTime.MIDNIGHT)
					.plus(patientOrder.getOrderAgeInMinutes(), ChronoUnit.MINUTES)
					.atZone(institution.getTimeZone())
					.toInstant();

			Instant endDateTime = patientOrder.getEpisodeEndedAt() == null ? Instant.now() : patientOrder.getEpisodeEndedAt();
			Duration orderDuration = Duration.between(orderDateTime, endDateTime);

			// Safe cast, int can always hold enough
			this.episodeDurationInDays = (int) orderDuration.toDays();
			this.episodeDurationInDaysDescription = strings.get("{{episodeDurationInDays}} days", Map.of("episodeDurationInDays", this.episodeDurationInDays));

			this.safetyPlanningNeeded = patientOrder.getSafetyPlanningNeeded();
			this.outreachNeeded = patientOrder.getOutreachNeeded();
			this.followupNeeded = patientOrder.getFollowupNeeded();

			this.resourcesSent = patientOrder.getResourcesSent();
			this.resourcesSentAt = patientOrder.getResourcesSentAt();
			this.resourcesSentAtDescription = patientOrder.getResourcesSentAt() == null
					? null
					: formatter.formatTimestamp(patientOrder.getResourcesSentAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);

			this.patientOrderDiagnoses = patientOrderDiagnoses;
			this.patientOrderMedications = patientOrderMedications;
			this.patientOrderNotes = patientOrderNotes;
			this.patientOrderOutreaches = patientOrderOutreaches;


			this.patientOrderCareTypeId = patientOrder.getPatientOrderCareTypeId();
			this.patientOrderCareTypeDescription = patientOrder.getPatientOrderCareTypeDescription();
			this.outreachCount = patientOrder.getOutreachCount();
			this.outreachCountDescription = formatter.formatNumber(patientOrder.getOutreachCount() == null ? 0 : patientOrder.getOutreachCount());
			this.mostRecentOutreachDateTime = patientOrder.getMostRecentOutreachDateTime();
			this.mostRecentOutreachDateTimeDescription = patientOrder.getMostRecentOutreachDateTime() == null ? null : formatter.formatDateTime(patientOrder.getMostRecentOutreachDateTime(), FormatStyle.MEDIUM, FormatStyle.SHORT);
			this.mostRecentScreeningSessionId = patientOrder.getMostRecentScreeningSessionId();
			this.mostRecentScreeningSessionCreatedByAccountId = patientOrder.getMostRecentScreeningSessionCreatedByAccountId();
			this.mostRecentScreeningSessionCreatedByAccountFirstName = patientOrder.getMostRecentScreeningSessionCreatedByAccountFirstName();
			this.mostRecentScreeningSessionCreatedByAccountLastName = patientOrder.getMostRecentScreeningSessionCreatedByAccountLastName();
			this.mostRecentScreeningSessionCreatedByAccountDisplayName = Normalizer.normalizeName(patientOrder.getMostRecentScreeningSessionCreatedByAccountFirstName(), patientOrder.getMostRecentScreeningSessionCreatedByAccountLastName()).orElse(null);
			this.mostRecentScreeningSessionCreatedByAccountDisplayNameWithLastFirst = Normalizer.normalizeNameWithLastFirst(patientOrder.getMostRecentScreeningSessionCreatedByAccountFirstName(), patientOrder.getMostRecentScreeningSessionCreatedByAccountLastName()).orElse(null);
			this.mostRecentScreeningSessionCompleted = patientOrder.getMostRecentScreeningSessionCompleted();
			this.mostRecentScreeningSessionCompletedAt = patientOrder.getMostRecentScreeningSessionCompletedAt();
			this.mostRecentScreeningSessionCompletedAtDescription = patientOrder.getMostRecentScreeningSessionCompletedAt() == null ? null : formatter.formatTimestamp(patientOrder.getMostRecentScreeningSessionCompletedAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
			this.panelAccountFirstName = patientOrder.getPanelAccountFirstName();
			this.panelAccountLastName = patientOrder.getPanelAccountLastName();
			this.panelAccountDisplayName = Normalizer.normalizeName(patientOrder.getPanelAccountFirstName(), patientOrder.getPanelAccountLastName()).orElse(null);
			this.panelAccountDisplayNameWithLastFirst = Normalizer.normalizeNameWithLastFirst(patientOrder.getPanelAccountFirstName(), patientOrder.getPanelAccountLastName()).orElse(null);
		}
	}

	@Nonnull
	public UUID getPatientOrderId() {
		return this.patientOrderId;
	}

	@Nullable
	public PatientOrderStatusId getPatientOrderStatusId() {
		return this.patientOrderStatusId;
	}

	@Nullable
	public PatientOrderScreeningStatusId getPatientOrderScreeningStatusId() {
		return this.patientOrderScreeningStatusId;
	}

	@Nullable
	public UUID getPatientAccountId() {
		return this.patientAccountId;
	}

	@Nullable
	public UUID getPatientAddressId() {
		return this.patientAddressId;
	}

	@Nullable
	public UUID getPanelAccountId() {
		return this.panelAccountId;
	}

	@Nullable
	public PatientOrderClosureReasonId getPatientOrderClosureReasonId() {
		return this.patientOrderClosureReasonId;
	}

	@Nullable
	public String getEncounterDepartmentId() {
		return this.encounterDepartmentId;
	}

	@Nullable
	public String getEncounterDepartmentIdType() {
		return this.encounterDepartmentIdType;
	}

	@Nullable
	public String getEncounterDepartmentName() {
		return this.encounterDepartmentName;
	}

	@Nullable
	public String getReferringPracticeId() {
		return this.referringPracticeId;
	}

	@Nullable
	public String getReferringPracticeIdType() {
		return this.referringPracticeIdType;
	}

	@Nullable
	public String getReferringPracticeName() {
		return this.referringPracticeName;
	}

	@Nullable
	public String getOrderingProviderId() {
		return this.orderingProviderId;
	}

	@Nullable
	public String getOrderingProviderIdType() {
		return this.orderingProviderIdType;
	}

	@Nullable
	public String getOrderingProviderLastName() {
		return this.orderingProviderLastName;
	}

	@Nullable
	public String getOrderingProviderFirstName() {
		return this.orderingProviderFirstName;
	}

	@Nullable
	public String getOrderingProviderMiddleName() {
		return this.orderingProviderMiddleName;
	}

	@Nullable
	public String getOrderingProviderDisplayName() {
		return this.orderingProviderDisplayName;
	}

	@Nullable
	public String getOrderingProviderDisplayNameWithLastFirst() {
		return this.orderingProviderDisplayNameWithLastFirst;
	}

	@Nullable
	public String getBillingProviderId() {
		return this.billingProviderId;
	}

	@Nullable
	public String getBillingProviderIdType() {
		return this.billingProviderIdType;
	}

	@Nullable
	public String getBillingProviderLastName() {
		return this.billingProviderLastName;
	}

	@Nullable
	public String getBillingProviderFirstName() {
		return this.billingProviderFirstName;
	}

	@Nullable
	public String getBillingProviderMiddleName() {
		return this.billingProviderMiddleName;
	}

	@Nullable
	public String getBillingProviderDisplayName() {
		return this.billingProviderDisplayName;
	}

	@Nullable
	public String getBillingProviderDisplayNameWithLastFirst() {
		return this.billingProviderDisplayNameWithLastFirst;
	}

	@Nullable
	public String getPatientLastName() {
		return this.patientLastName;
	}

	@Nullable
	public String getPatientFirstName() {
		return this.patientFirstName;
	}

	@Nullable
	public String getPatientDisplayName() {
		return this.patientDisplayName;
	}

	@Nullable
	public String getPatientDisplayNameWithLastFirst() {
		return this.patientDisplayNameWithLastFirst;
	}

	@Nullable
	public String getPatientMrn() {
		return this.patientMrn;
	}

	@Nullable
	public String getPatientId() {
		return this.patientId;
	}

	@Nullable
	public String getPatientIdType() {
		return this.patientIdType;
	}

	@Nullable
	public BirthSexId getPatientBirthSexId() {
		return this.patientBirthSexId;
	}

	@Nullable
	public LocalDate getPatientBirthdate() {
		return this.patientBirthdate;
	}

	@Nullable
	public String getPatientBirthdateDescription() {
		return this.patientBirthdateDescription;
	}

	@Nullable
	public String getPrimaryPayorId() {
		return this.primaryPayorId;
	}

	@Nullable
	public String getPrimaryPayorName() {
		return this.primaryPayorName;
	}

	@Nullable
	public String getPrimaryPlanId() {
		return this.primaryPlanId;
	}

	@Nullable
	public String getPrimaryPlanName() {
		return this.primaryPlanName;
	}

	@Nullable
	public LocalDate getOrderDate() {
		return this.orderDate;
	}

	@Nullable
	public String getOrderDateDescription() {
		return this.orderDateDescription;
	}

	@Nullable
	public Integer getOrderAgeInMinutes() {
		return this.orderAgeInMinutes;
	}

	@Nullable
	public String getOrderAgeInMinutesDescription() {
		return this.orderAgeInMinutesDescription;
	}

	@Nullable
	public String getOrderId() {
		return this.orderId;
	}

	@Nullable
	public String getRouting() {
		return this.routing;
	}

	@Nullable
	public String getReasonForReferral() {
		return this.reasonForReferral;
	}

	@Nullable
	public String getAssociatedDiagnosis() {
		return this.associatedDiagnosis;
	}

	@Nullable
	public String getPatientPhoneNumber() {
		return this.patientPhoneNumber;
	}

	@Nullable
	public String getPatientPhoneNumberDescription() {
		return this.patientPhoneNumberDescription;
	}

	@Nullable
	public EthnicityId getPatientEthnicityId() {
		return this.patientEthnicityId;
	}

	@Nullable
	public RaceId getPatientRaceId() {
		return this.patientRaceId;
	}

	@Nullable
	public GenderIdentityId getPatientGenderIdentityId() {
		return this.patientGenderIdentityId;
	}

	@Nullable
	public String getPatientLanguageCode() {
		return this.patientLanguageCode;
	}

	@Nullable
	public String getPatientEmailEddress() {
		return this.patientEmailEddress;
	}

	@Nullable
	public String getPreferredContactHours() {
		return this.preferredContactHours;
	}

	@Nullable
	public String getComments() {
		return this.comments;
	}

	@Nullable
	public String getCcRecipients() {
		return this.ccRecipients;
	}

	@Nullable
	public String getLastActiveMedicationOrderSummary() {
		return this.lastActiveMedicationOrderSummary;
	}

	@Nullable
	public String getMedications() {
		return this.medications;
	}

	@Nullable
	public String getRecentPsychotherapeuticMedications() {
		return this.recentPsychotherapeuticMedications;
	}

	@Nullable
	public Instant getEpisodeEndedAt() {
		return this.episodeEndedAt;
	}

	@Nullable
	public String getEpisodeEndedAtDescription() {
		return this.episodeEndedAtDescription;
	}

	@Nullable
	public Integer getEpisodeDurationInDays() {
		return this.episodeDurationInDays;
	}

	@Nullable
	public String getEpisodeDurationInDaysDescription() {
		return this.episodeDurationInDaysDescription;
	}

	@Nullable
	public Boolean getSafetyPlanningNeeded() {
		return this.safetyPlanningNeeded;
	}

	@Nullable
	public Boolean getOutreachNeeded() {
		return this.outreachNeeded;
	}

	@Nullable
	public Boolean getFollowupNeeded() {
		return this.followupNeeded;
	}

	@Nullable
	public Boolean getResourcesSent() {
		return this.resourcesSent;
	}

	@Nullable
	public Instant getResourcesSentAt() {
		return this.resourcesSentAt;
	}

	@Nullable
	public String getResourcesSentAtDescription() {
		return this.resourcesSentAtDescription;
	}

	@Nullable
	public AddressApiResponse getPatientAddress() {
		return this.patientAddress;
	}

	@Nullable
	public AccountApiResponse getPatientAccount() {
		return this.patientAccount;
	}

	@Nullable
	public List<PatientOrderDiagnosisApiResponse> getPatientOrderDiagnoses() {
		return this.patientOrderDiagnoses;
	}

	@Nullable
	public List<PatientOrderMedicationApiResponse> getPatientOrderMedications() {
		return this.patientOrderMedications;
	}

	@Nullable
	public List<PatientOrderNoteApiResponse> getPatientOrderNotes() {
		return this.patientOrderNotes;
	}

	@Nullable
	public List<PatientOrderOutreachApiResponse> getPatientOrderOutreaches() {
		return this.patientOrderOutreaches;
	}

	@Nullable
	public List<PatientOrderTriageGroupApiResponse> getPatientOrderTriageGroups() {
		return this.patientOrderTriageGroups;
	}

	@Nullable
	public ScreeningSessionApiResponse getScreeningSession() {
		return this.screeningSession;
	}

	@Nullable
	public ScreeningSessionResult getScreeningSessionResult() {
		return this.screeningSessionResult;
	}

	@Nullable
	public PatientOrderCareTypeId getPatientOrderCareTypeId() {
		return this.patientOrderCareTypeId;
	}

	@Nullable
	public String getPatientOrderCareTypeDescription() {
		return this.patientOrderCareTypeDescription;
	}

	@Nullable
	public Integer getOutreachCount() {
		return this.outreachCount;
	}

	@Nullable
	public String getOutreachCountDescription() {
		return this.outreachCountDescription;
	}

	@Nullable
	public LocalDateTime getMostRecentOutreachDateTime() {
		return this.mostRecentOutreachDateTime;
	}

	@Nullable
	public String getMostRecentOutreachDateTimeDescription() {
		return this.mostRecentOutreachDateTimeDescription;
	}

	@Nullable
	public UUID getMostRecentScreeningSessionId() {
		return this.mostRecentScreeningSessionId;
	}

	@Nullable
	public UUID getMostRecentScreeningSessionCreatedByAccountId() {
		return this.mostRecentScreeningSessionCreatedByAccountId;
	}

	@Nullable
	public String getMostRecentScreeningSessionCreatedByAccountFirstName() {
		return this.mostRecentScreeningSessionCreatedByAccountFirstName;
	}

	@Nullable
	public String getMostRecentScreeningSessionCreatedByAccountLastName() {
		return this.mostRecentScreeningSessionCreatedByAccountLastName;
	}

	@Nullable
	public String getMostRecentScreeningSessionCreatedByAccountDisplayName() {
		return this.mostRecentScreeningSessionCreatedByAccountDisplayName;
	}

	@Nullable
	public String getMostRecentScreeningSessionCreatedByAccountDisplayNameWithLastFirst() {
		return this.mostRecentScreeningSessionCreatedByAccountDisplayNameWithLastFirst;
	}

	@Nullable
	public Boolean getMostRecentScreeningSessionCompleted() {
		return this.mostRecentScreeningSessionCompleted;
	}

	@Nullable
	public Instant getMostRecentScreeningSessionCompletedAt() {
		return this.mostRecentScreeningSessionCompletedAt;
	}

	@Nullable
	public String getMostRecentScreeningSessionCompletedAtDescription() {
		return this.mostRecentScreeningSessionCompletedAtDescription;
	}

	@Nullable
	public String getPanelAccountFirstName() {
		return this.panelAccountFirstName;
	}

	@Nullable
	public String getPanelAccountLastName() {
		return this.panelAccountLastName;
	}

	@Nullable
	public String getPanelAccountDisplayName() {
		return this.panelAccountDisplayName;
	}

	@Nullable
	public String getPanelAccountDisplayNameWithLastFirst() {
		return this.panelAccountDisplayNameWithLastFirst;
	}
}