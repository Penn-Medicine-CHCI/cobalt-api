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
import com.cobaltplatform.api.model.api.response.PatientOrderScheduledMessageGroupApiResponse.PatientOrderScheduledMessageGroupApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderTriageGroupApiResponse.PatientOrderTriageGroupFocusApiResponse;
import com.cobaltplatform.api.model.api.response.PatientOrderVoicemailTaskApiResponse.PatientOrderVoicemailTaskApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningSessionApiResponse.ScreeningSessionApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Address;
import com.cobaltplatform.api.model.db.BirthSex.BirthSexId;
import com.cobaltplatform.api.model.db.DistanceUnit.DistanceUnitId;
import com.cobaltplatform.api.model.db.Ethnicity.EthnicityId;
import com.cobaltplatform.api.model.db.GenderIdentity.GenderIdentityId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.PatientOrderCarePreference.PatientOrderCarePreferenceId;
import com.cobaltplatform.api.model.db.PatientOrderCareType;
import com.cobaltplatform.api.model.db.PatientOrderCareType.PatientOrderCareTypeId;
import com.cobaltplatform.api.model.db.PatientOrderClosureReason.PatientOrderClosureReasonId;
import com.cobaltplatform.api.model.db.PatientOrderConsentStatus.PatientOrderConsentStatusId;
import com.cobaltplatform.api.model.db.PatientOrderDisposition.PatientOrderDispositionId;
import com.cobaltplatform.api.model.db.PatientOrderFocusType;
import com.cobaltplatform.api.model.db.PatientOrderFocusType.PatientOrderFocusTypeId;
import com.cobaltplatform.api.model.db.PatientOrderInsurancePayorType.PatientOrderInsurancePayorTypeId;
import com.cobaltplatform.api.model.db.PatientOrderInsurancePlanType.PatientOrderInsurancePlanTypeId;
import com.cobaltplatform.api.model.db.PatientOrderResourceCheckInResponseStatus.PatientOrderResourceCheckInResponseStatusId;
import com.cobaltplatform.api.model.db.PatientOrderResourcingStatus.PatientOrderResourcingStatusId;
import com.cobaltplatform.api.model.db.PatientOrderResourcingType.PatientOrderResourcingTypeId;
import com.cobaltplatform.api.model.db.PatientOrderSafetyPlanningStatus.PatientOrderSafetyPlanningStatusId;
import com.cobaltplatform.api.model.db.PatientOrderScheduledMessage;
import com.cobaltplatform.api.model.db.PatientOrderScreeningStatus.PatientOrderScreeningStatusId;
import com.cobaltplatform.api.model.db.PatientOrderTriage;
import com.cobaltplatform.api.model.db.PatientOrderTriageSource.PatientOrderTriageSourceId;
import com.cobaltplatform.api.model.db.PatientOrderTriageStatus.PatientOrderTriageStatusId;
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
	private PatientOrderTriageStatusId patientOrderTriageStatusId;
	@Nullable
	private PatientOrderDispositionId patientOrderDispositionId;
	@Nullable
	private PatientOrderScreeningStatusId patientOrderScreeningStatusId;
	@Nullable
	private PatientOrderResourcingStatusId patientOrderResourcingStatusId;
	@Nullable
	private PatientOrderResourcingTypeId patientOrderResourcingTypeId;
	@Nullable
	private UUID patientAccountId;
	@Nullable
	private UUID patientAddressId;
	@Nullable
	private UUID panelAccountId;
	@Nullable
	private PatientOrderClosureReasonId patientOrderClosureReasonId;
	@Nullable
	private PatientOrderSafetyPlanningStatusId patientOrderSafetyPlanningStatusId;
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
	private String patientEmailAddress;
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
	private Integer episodeDurationInDays;
	@Nullable
	private String episodeDurationInDaysDescription;
	@Nullable
	private Boolean outreachNeeded;
	@Nullable
	private Boolean followupNeeded;
	@Nullable
	private Instant resourcesSentAt;
	@Nullable
	private String resourcesSentAtDescription;
	@Nullable
	private String resourcesSentNote;
	@Nullable
	private UUID appointmentId;
	@Nullable
	private LocalDateTime appointmentStartTime;
	@Nullable
	private String appointmentStartTimeDescription;
	@Nullable
	private UUID providerId;
	@Nullable
	private String providerName;
	@Nullable
	private PatientOrderConsentStatusId patientOrderConsentStatusId;
	@Nullable
	private UUID consentStatusUpdatedByByAccountId;
	@Nullable
	private Instant consentStatusUpdatedAt;
	@Nullable
	private String consentStatusUpdatedAtDescription;
	@Nullable
	private PatientOrderResourceCheckInResponseStatusId patientOrderResourceCheckInResponseStatusId;
	@Nullable
	private UUID resourceCheckInResponseStatusUpdatedByByAccountId;
	@Nullable
	private Instant resourceCheckInResponseStatusUpdatedAt;
	@Nullable
	private String resourceCheckInResponseStatusUpdatedAtDescription;
	@Nullable
	private Instant patientDemographicsConfirmedAt;
	@Nullable
	private String patientDemographicsConfirmedAtDescription;
	@Nullable
	private UUID patientDemographicsConfirmedByAccountId;
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
	private List<PatientOrderScheduledMessageGroupApiResponse> patientOrderScheduledMessageGroups;
	@Nullable
	private List<PatientOrderVoicemailTaskApiResponse> patientOrderVoicemailTasks;
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
	private Integer totalOutreachCount;
	@Nullable
	private String totalOutreachCountDescription;
	@Nullable
	private LocalDateTime mostRecentTotalOutreachDateTime;
	@Nullable
	private String mostRecentTotalOutreachDateTimeDescription;
	@Nullable
	private Integer outreachCount;
	@Nullable
	private String outreachCountDescription;
	@Nullable
	private LocalDateTime mostRecentOutreachDateTime;
	@Nullable
	private String mostRecentOutreachDateTimeDescription;
	@Nullable
	private Integer scheduledMessageGroupCount;
	@Nullable
	private String scheduledMessageGroupCountDescription;
	@Nullable
	private LocalDateTime mostRecentScheduledMessageGroupDateTime;
	@Nullable
	private String mostRecentScheduledMessageGroupDateTimeDescription;
	@Nullable
	private UUID mostRecentScreeningSessionId;
	@Nullable
	private Instant mostRecentScreeningSessionCreatedAt;
	@Nullable
	private String mostRecentScreeningSessionCreatedAtDescription;
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
	@Nullable
	private String patientOrderScreeningStatusDescription;
	@Nullable
	private String patientOrderDispositionDescription;
	@Nullable
	private String patientOrderTriageStatusDescription;
	@Nullable
	private String patientOrderClosureReasonDescription;
	@Nullable
	private Instant connectedToSafetyPlanningAt;
	@Nullable
	private String connectedToSafetyPlanningAtDescription;
	@Nullable
	private Integer patientAgeOnOrderDate;
	@Nullable
	private String patientAgeOnOrderDateDescription;
	@Nullable
	private Boolean patientBelowAgeThreshold;
	@Nullable
	private Instant episodeClosedAt;
	@Nullable
	private String episodeClosedAtDescription;
	@Nullable
	private UUID episodeClosedByAccountId;
	@Nullable
	private Instant mostRecentEpisodeClosedAt;
	@Nullable
	private String mostRecentEpisodeClosedAtDescription;
	@Nullable
	private Boolean mostRecentEpisodeClosedWithinDateThreshold;
	@Nullable
	private UUID mostRecentPatientOrderVoicemailTaskId;
	@Nullable
	private Boolean mostRecentPatientOrderVoicemailTaskCompleted;
	@Nullable
	private UUID patientOrderScheduledScreeningId;
	@Nullable
	private LocalDateTime patientOrderScheduledScreeningScheduledDateTime;
	@Nullable
	private String patientOrderScheduledScreeningScheduledDateTimeDescription;
	@Nullable
	private String patientOrderScheduledScreeningCalendarUrl;
	@Nullable
	private UUID patientOrderInsurancePayorId;
	@Nullable
	private PatientOrderInsurancePayorTypeId patientOrderInsurancePayorTypeId;
	@Nullable
	private String patientOrderInsurancePayorName;
	@Nullable
	private UUID patientOrderInsurancePlanId;
	@Nullable
	private String patientOrderInsurancePlanName;
	@Nullable
	private PatientOrderInsurancePlanTypeId patientOrderInsurancePlanTypeId;
	@Nullable
	private Boolean patientOrderInsurancePlanAccepted;
	@Nullable
	private String patientAddressStreetAddress1;
	@Nullable
	private String patientAddressLocality;
	@Nullable
	private String patientAddressRegion;
	@Nullable
	private String patientAddressPostalCode;
	@Nullable
	private String patientAddressCountryCode;
	@Nullable
	private Boolean patientAddressRegionAccepted;
	@Nullable
	private Boolean patientDemographicsCompleted;
	@Nullable
	private Boolean patientDemographicsAccepted;
	@Nullable
	private PatientOrderCarePreferenceId patientOrderCarePreferenceId;
	@Nullable
	private Integer inPersonCareRadius;
	@Nullable
	private String inPersonCareRadiusDescription;
	@Nullable
	private DistanceUnitId inPersonCareRadiusDistanceUnitId;
	@Nullable
	private String inPersonCareRadiusWithDistanceUnitDescription;

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
																 @Nonnull PatientOrderScheduledMessageGroupApiResponseFactory patientOrderScheduledMessageGroupApiResponseFactory,
																 @Nonnull ScreeningSessionApiResponseFactory screeningSessionApiResponseFactory,
																 @Nonnull AddressApiResponseFactory addressApiResponseFactory,
																 @Nonnull PatientOrderVoicemailTaskApiResponseFactory patientOrderVoicemailTaskApiResponseFactory,
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
				patientOrderScheduledMessageGroupApiResponseFactory,
				screeningSessionApiResponseFactory,
				addressApiResponseFactory,
				patientOrderVoicemailTaskApiResponseFactory,
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
																 @Nonnull PatientOrderScheduledMessageGroupApiResponseFactory patientOrderScheduledMessageGroupApiResponseFactory,
																 @Nonnull ScreeningSessionApiResponseFactory screeningSessionApiResponseFactory,
																 @Nonnull AddressApiResponseFactory addressApiResponseFactory,
																 @Nonnull PatientOrderVoicemailTaskApiResponseFactory patientOrderVoicemailTaskApiResponseFactory,
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
		requireNonNull(patientOrderScheduledMessageGroupApiResponseFactory);
		requireNonNull(screeningSessionApiResponseFactory);
		requireNonNull(addressApiResponseFactory);
		requireNonNull(patientOrderVoicemailTaskApiResponseFactory);
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
		List<PatientOrderVoicemailTaskApiResponse> patientOrderVoicemailTasks = null;

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

			patientOrderVoicemailTasks = patientOrderService.findPatientOrderVoicemailTasksByPatientOrderId(patientOrder.getPatientOrderId()).stream()
					.map(patientOrderVoicemailTask -> patientOrderVoicemailTaskApiResponseFactory.create(patientOrderVoicemailTask))
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

			List<PatientOrderTriage> patientOrderTriages = patientOrderService.findPatientOrderTriagesByPatientOrderId(patientOrder.getPatientOrderId());

			if (patientOrderTriages.size() > 0) {
				List<PatientOrderFocusType> patientOrderFocusTypes = patientOrderService.findPatientOrderFocusTypes();
				Map<PatientOrderFocusTypeId, PatientOrderFocusType> patientOrderFocusTypesById = patientOrderFocusTypes.stream()
						.collect(Collectors.toMap(PatientOrderFocusType::getPatientOrderFocusTypeId, patientOrderFocusType -> patientOrderFocusType));
				List<PatientOrderCareType> patientOrderCareTypes = patientOrderService.findPatientOrderCareTypes();
				Map<PatientOrderCareTypeId, PatientOrderCareType> patientOrderCareTypesById = patientOrderCareTypes.stream()
						.collect(Collectors.toMap(PatientOrderCareType::getPatientOrderCareTypeId, patientOrderCareType -> patientOrderCareType));

				Map<PatientOrderCareTypeId, List<PatientOrderTriage>> patientOrderTriagesByCareTypeIds = new LinkedHashMap<>();

				// Triage source should be the same across all triages, so just pick the first one
				PatientOrderTriageSourceId patientOrderTriageSourceId = patientOrderTriages.get(0).getPatientOrderTriageSourceId();

				for (PatientOrderTriage patientOrderTriage : patientOrderTriages) {
					List<PatientOrderTriage> groupedPatientOrderTriages = patientOrderTriagesByCareTypeIds.get(patientOrderTriage.getPatientOrderCareTypeId());

					if (groupedPatientOrderTriages == null) {
						groupedPatientOrderTriages = new ArrayList<>();
						patientOrderTriagesByCareTypeIds.put(patientOrderTriage.getPatientOrderCareTypeId(), groupedPatientOrderTriages);
					}

					groupedPatientOrderTriages.add(patientOrderTriage);
				}

				List<PatientOrderTriageGroupApiResponse> patientOrderTriageGroups = new ArrayList<>();

				for (Entry<PatientOrderCareTypeId, List<PatientOrderTriage>> entry : patientOrderTriagesByCareTypeIds.entrySet()) {
					PatientOrderCareTypeId patientOrderCareTypeId = entry.getKey();
					PatientOrderCareType patientOrderCareType = patientOrderCareTypesById.get(patientOrderCareTypeId);
					List<PatientOrderTriage> careTypePatientOrderTriages = entry.getValue();

					// Within this care type, further group by focus type
					Map<PatientOrderFocusTypeId, List<PatientOrderTriage>> patientOrderTriagesByFocusTypeIds = new LinkedHashMap<>();

					for (PatientOrderTriage patientOrderTriage : careTypePatientOrderTriages) {
						List<PatientOrderTriage> focusTypePatientOrderTriages = patientOrderTriagesByFocusTypeIds.get(patientOrderTriage.getPatientOrderFocusTypeId());

						if (focusTypePatientOrderTriages == null) {
							focusTypePatientOrderTriages = new ArrayList<>();
							patientOrderTriagesByFocusTypeIds.put(patientOrderTriage.getPatientOrderFocusTypeId(), focusTypePatientOrderTriages);
						}

						focusTypePatientOrderTriages.add(patientOrderTriage);
					}

					List<PatientOrderTriageGroupFocusApiResponse> focusTypePatientOrderTriages = new ArrayList<>();

					for (Entry<PatientOrderFocusTypeId, List<PatientOrderTriage>> focusEntry : patientOrderTriagesByFocusTypeIds.entrySet()) {
						PatientOrderFocusTypeId patientOrderFocusTypeId = focusEntry.getKey();
						List<PatientOrderTriage> focusPatientOrderTriages = focusEntry.getValue();

						List<String> focusReasons = focusPatientOrderTriages.stream()
								.map(focusPatientOrderTriage -> focusPatientOrderTriage.getReason())
								.distinct()
								.collect(Collectors.toList());

						focusTypePatientOrderTriages.add(new PatientOrderTriageGroupFocusApiResponse(patientOrderFocusTypesById.get(patientOrderFocusTypeId), focusReasons));
					}

					patientOrderTriageGroups.add(new PatientOrderTriageGroupApiResponse(patientOrderTriageSourceId, patientOrderCareType, focusTypePatientOrderTriages));
				}

				this.patientOrderTriageGroups = patientOrderTriageGroups;
			}
		}

		// Always available to both patients and MHICs
		this.patientOrderId = patientOrder.getPatientOrderId();
		this.patientOrderTriageStatusId = patientOrder.getPatientOrderTriageStatusId();
		this.patientOrderDispositionId = patientOrder.getPatientOrderDispositionId();
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
		this.patientEmailAddress = patientOrder.getPatientEmailAddress();
		this.patientBirthdate = patientOrder.getPatientBirthdate();
		this.patientBirthdateDescription = patientOrder.getPatientBirthdate() == null ? null : formatter.formatDate(patientOrder.getPatientBirthdate(), FormatStyle.MEDIUM);
		this.patientPhoneNumber = patientOrder.getPatientPhoneNumber();
		this.patientPhoneNumberDescription = patientOrder.getPatientPhoneNumber() == null ? null : formatter.formatPhoneNumber(patientOrder.getPatientPhoneNumber(), currentContext.getLocale());
		this.patientAddress = patientAddress;
		this.patientAccount = patientAccount;
		this.appointmentId = patientOrder.getAppointmentId();
		this.appointmentStartTime = patientOrder.getAppointmentStartTime();
		this.appointmentStartTimeDescription = patientOrder.getAppointmentStartTime() == null ? null : formatter.formatDateTime(patientOrder.getAppointmentStartTime(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.providerId = patientOrder.getProviderId();
		this.providerName = patientOrder.getProviderName();
		this.patientOrderConsentStatusId = patientOrder.getPatientOrderConsentStatusId();
		this.patientOrderResourceCheckInResponseStatusId = patientOrder.getPatientOrderResourceCheckInResponseStatusId();
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
		this.patientOrderInsurancePayorId = patientOrder.getPatientOrderInsurancePayorId();
		this.patientOrderInsurancePayorTypeId = patientOrder.getPatientOrderInsurancePayorTypeId();
		this.patientOrderInsurancePayorName = patientOrder.getPatientOrderInsurancePayorName();
		this.patientOrderInsurancePlanId = patientOrder.getPatientOrderInsurancePlanId();
		this.patientOrderInsurancePlanName = patientOrder.getPatientOrderInsurancePlanName();
		this.patientOrderInsurancePlanTypeId = patientOrder.getPatientOrderInsurancePlanTypeId();
		this.patientOrderInsurancePlanAccepted = patientOrder.getPatientOrderInsurancePlanAccepted();
		this.patientAddressStreetAddress1 = patientOrder.getPatientAddressStreetAddress1();
		this.patientAddressLocality = patientOrder.getPatientAddressLocality();
		this.patientAddressRegion = patientOrder.getPatientAddressRegion();
		this.patientAddressPostalCode = patientOrder.getPatientAddressPostalCode();
		this.patientAddressCountryCode = patientOrder.getPatientAddressCountryCode();
		this.patientAddressRegionAccepted = patientOrder.getPatientAddressRegionAccepted();
		this.patientDemographicsCompleted = patientOrder.getPatientDemographicsCompleted();
		this.patientDemographicsAccepted = patientOrder.getPatientDemographicsAccepted();
		this.patientDemographicsConfirmedAt = patientOrder.getPatientDemographicsConfirmedAt();
		this.patientDemographicsConfirmedAtDescription = patientOrder.getPatientDemographicsConfirmedAt() == null
				? null
				: formatter.formatTimestamp(patientOrder.getPatientDemographicsConfirmedAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.patientDemographicsConfirmedByAccountId = patientOrder.getPatientDemographicsConfirmedByAccountId();

		this.mostRecentScreeningSessionId = patientOrder.getMostRecentScreeningSessionId();
		this.mostRecentScreeningSessionCreatedByAccountId = patientOrder.getMostRecentScreeningSessionCreatedByAccountId();
		this.mostRecentScreeningSessionCreatedAt = patientOrder.getMostRecentScreeningSessionCreatedAt();
		this.mostRecentScreeningSessionCreatedAtDescription = patientOrder.getMostRecentScreeningSessionCreatedAt() == null ? null : formatter.formatTimestamp(patientOrder.getMostRecentScreeningSessionCreatedAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
		this.mostRecentScreeningSessionCompleted = patientOrder.getMostRecentScreeningSessionCompleted();
		this.mostRecentScreeningSessionCompletedAt = patientOrder.getMostRecentScreeningSessionCompletedAt();
		this.mostRecentScreeningSessionCompletedAtDescription = patientOrder.getMostRecentScreeningSessionCompletedAt() == null ? null : formatter.formatTimestamp(patientOrder.getMostRecentScreeningSessionCompletedAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);

		this.patientAgeOnOrderDate = patientOrder.getPatientAgeOnOrderDate();
		this.patientAgeOnOrderDateDescription = formatter.formatInteger(patientOrder.getPatientAgeOnOrderDate());

		this.patientOrderCarePreferenceId = patientOrder.getPatientOrderCarePreferenceId();
		this.inPersonCareRadius = patientOrder.getInPersonCareRadius();
		this.inPersonCareRadiusDescription = patientOrder.getInPersonCareRadius() == null ? null : formatter.formatInteger(patientOrder.getInPersonCareRadius());
		this.inPersonCareRadiusDistanceUnitId = patientOrder.getInPersonCareRadiusDistanceUnitId();
		this.inPersonCareRadiusWithDistanceUnitDescription = patientOrder.getInPersonCareRadius() == null ? strings.get("Unspecified")
				: strings.get("{{radius}} {{distanceUnit}}", Map.of(
				"radius", patientOrder.getInPersonCareRadius(),
				"distanceUnit", patientOrder.getInPersonCareRadiusDistanceUnitId() == DistanceUnitId.MILE ? strings.get("mi") : strings.get("km")
		));
		
		this.patientOrderSafetyPlanningStatusId = patientOrder.getPatientOrderSafetyPlanningStatusId();
		this.connectedToSafetyPlanningAt = patientOrder.getConnectedToSafetyPlanningAt();
		this.connectedToSafetyPlanningAtDescription = patientOrder.getConnectedToSafetyPlanningAt() == null ? null : formatter.formatTimestamp(patientOrder.getConnectedToSafetyPlanningAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);

		this.patientOrderClosureReasonId = patientOrder.getPatientOrderClosureReasonId();
		this.patientOrderScreeningStatusDescription = patientOrder.getPatientOrderScreeningStatusDescription();

		// MHIC-only view of the data
		if (format == PatientOrderApiResponseFormat.MHIC) {
			this.panelAccountId = patientOrder.getPanelAccountId();
			this.encounterDepartmentId = patientOrder.getEncounterDepartmentId();
			this.encounterDepartmentIdType = patientOrder.getEncounterDepartmentIdType();
			this.encounterDepartmentName = patientOrder.getEncounterDepartmentName();
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
			this.episodeClosedAt = patientOrder.getEpisodeClosedAt();
			this.episodeClosedAtDescription = patientOrder.getEpisodeClosedAt() == null
					? null
					: formatter.formatTimestamp(patientOrder.getEpisodeClosedAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
			this.episodeClosedByAccountId = patientOrder.getEpisodeClosedByAccountId();

			Institution institution = institutionService.findInstitutionById(patientOrder.getInstitutionId()).get();

			Instant orderDateTime = patientOrder.getOrderDate()
					.atTime(LocalTime.MIDNIGHT)
					.plus(patientOrder.getOrderAgeInMinutes(), ChronoUnit.MINUTES)
					.atZone(institution.getTimeZone())
					.toInstant();

			Instant endDateTime = patientOrder.getEpisodeClosedAt() == null ? Instant.now() : patientOrder.getEpisodeClosedAt();
			Duration orderDuration = Duration.between(orderDateTime, endDateTime);

			// Safe cast, int can always hold enough
			this.episodeDurationInDays = (int) orderDuration.toDays();
			this.episodeDurationInDaysDescription = strings.get("{{episodeDurationInDays}} days", Map.of("episodeDurationInDays", this.episodeDurationInDays));

			this.outreachNeeded = patientOrder.getOutreachNeeded();
			this.followupNeeded = patientOrder.getFollowupNeeded();

			this.patientOrderResourcingStatusId = patientOrder.getPatientOrderResourcingStatusId();
			this.resourcesSentAt = patientOrder.getResourcesSentAt();
			this.resourcesSentAtDescription = patientOrder.getResourcesSentAt() == null
					? null
					: formatter.formatTimestamp(patientOrder.getResourcesSentAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
			this.resourcesSentNote = patientOrder.getResourcesSentNote();

			this.patientOrderDiagnoses = patientOrderDiagnoses;
			this.patientOrderMedications = patientOrderMedications;
			this.patientOrderNotes = patientOrderNotes;
			this.patientOrderOutreaches = patientOrderOutreaches;

			this.patientOrderCareTypeId = patientOrder.getPatientOrderCareTypeId();
			this.patientOrderCareTypeDescription = patientOrder.getPatientOrderCareTypeDescription();
			this.totalOutreachCount = patientOrder.getTotalOutreachCount();
			this.totalOutreachCountDescription = formatter.formatNumber(patientOrder.getTotalOutreachCount() == null ? 0 : patientOrder.getTotalOutreachCount());
			this.mostRecentTotalOutreachDateTime = patientOrder.getMostRecentTotalOutreachDateTime();
			this.mostRecentTotalOutreachDateTimeDescription = patientOrder.getMostRecentTotalOutreachDateTime() == null ? null : formatter.formatDateTime(patientOrder.getMostRecentTotalOutreachDateTime(), FormatStyle.MEDIUM, FormatStyle.SHORT);
			this.outreachCount = patientOrder.getOutreachCount();
			this.outreachCountDescription = formatter.formatNumber(patientOrder.getOutreachCount() == null ? 0 : patientOrder.getOutreachCount());
			this.mostRecentOutreachDateTime = patientOrder.getMostRecentOutreachDateTime();
			this.mostRecentOutreachDateTimeDescription = patientOrder.getMostRecentOutreachDateTime() == null ? null : formatter.formatDateTime(patientOrder.getMostRecentOutreachDateTime(), FormatStyle.MEDIUM, FormatStyle.SHORT);
			this.scheduledMessageGroupCount = patientOrder.getScheduledMessageGroupCount();
			this.scheduledMessageGroupCountDescription = formatter.formatNumber(patientOrder.getScheduledMessageGroupCount() == null ? 0 : patientOrder.getScheduledMessageGroupCount());
			this.mostRecentScheduledMessageGroupDateTime = patientOrder.getMostRecentScheduledMessageGroupDateTime();
			this.mostRecentScheduledMessageGroupDateTimeDescription = patientOrder.getMostRecentScheduledMessageGroupDateTime() == null ? null : formatter.formatDateTime(patientOrder.getMostRecentScheduledMessageGroupDateTime(), FormatStyle.MEDIUM, FormatStyle.SHORT);
			this.mostRecentScreeningSessionCreatedByAccountFirstName = patientOrder.getMostRecentScreeningSessionCreatedByAccountFirstName();
			this.mostRecentScreeningSessionCreatedByAccountLastName = patientOrder.getMostRecentScreeningSessionCreatedByAccountLastName();
			this.mostRecentScreeningSessionCreatedByAccountDisplayName = Normalizer.normalizeName(patientOrder.getMostRecentScreeningSessionCreatedByAccountFirstName(), patientOrder.getMostRecentScreeningSessionCreatedByAccountLastName()).orElse(null);
			this.mostRecentScreeningSessionCreatedByAccountDisplayNameWithLastFirst = Normalizer.normalizeNameWithLastFirst(patientOrder.getMostRecentScreeningSessionCreatedByAccountFirstName(), patientOrder.getMostRecentScreeningSessionCreatedByAccountLastName()).orElse(null);
			this.panelAccountFirstName = patientOrder.getPanelAccountFirstName();
			this.panelAccountLastName = patientOrder.getPanelAccountLastName();
			this.panelAccountDisplayName = Normalizer.normalizeName(patientOrder.getPanelAccountFirstName(), patientOrder.getPanelAccountLastName()).orElse(null);
			this.panelAccountDisplayNameWithLastFirst = Normalizer.normalizeNameWithLastFirst(patientOrder.getPanelAccountFirstName(), patientOrder.getPanelAccountLastName()).orElse(null);
			this.patientOrderDispositionDescription = patientOrder.getPatientOrderDispositionDescription();
			this.patientOrderTriageStatusDescription = patientOrder.getPatientOrderTriageStatusDescription();
			this.patientOrderClosureReasonDescription = patientOrder.getPatientOrderClosureReasonDescription();
			this.patientBelowAgeThreshold = patientOrder.getPatientBelowAgeThreshold();
			this.mostRecentEpisodeClosedAt = patientOrder.getMostRecentEpisodeClosedAt();
			this.mostRecentEpisodeClosedAtDescription = patientOrder.getMostRecentEpisodeClosedAt() == null ? null : formatter.formatTimestamp(patientOrder.getMostRecentEpisodeClosedAt());
			this.mostRecentEpisodeClosedWithinDateThreshold = patientOrder.getMostRecentEpisodeClosedWithinDateThreshold();
			this.mostRecentPatientOrderVoicemailTaskId = patientOrder.getMostRecentPatientOrderVoicemailTaskId();
			this.mostRecentPatientOrderVoicemailTaskCompleted = patientOrder.getMostRecentPatientOrderVoicemailTaskCompleted();
			this.patientOrderScheduledScreeningId = patientOrder.getPatientOrderScheduledScreeningId();
			this.patientOrderScheduledScreeningScheduledDateTime = patientOrder.getPatientOrderScheduledScreeningScheduledDateTime();
			this.patientOrderScheduledScreeningScheduledDateTimeDescription = patientOrder.getPatientOrderScheduledScreeningScheduledDateTime() == null ? null : formatter.formatDateTime(patientOrder.getPatientOrderScheduledScreeningScheduledDateTime(), FormatStyle.MEDIUM, FormatStyle.SHORT);
			this.patientOrderScheduledScreeningCalendarUrl = patientOrder.getPatientOrderScheduledScreeningCalendarUrl();
			this.consentStatusUpdatedByByAccountId = patientOrder.getConsentStatusUpdatedByByAccountId();
			this.consentStatusUpdatedAt = patientOrder.getConsentStatusUpdatedAt();
			this.consentStatusUpdatedAtDescription = patientOrder.getConsentStatusUpdatedAt() == null ? null : formatter.formatTimestamp(patientOrder.getConsentStatusUpdatedAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);
			this.resourceCheckInResponseStatusUpdatedByByAccountId = patientOrder.getResourceCheckInResponseStatusUpdatedByByAccountId();
			this.resourceCheckInResponseStatusUpdatedAt = patientOrder.getResourceCheckInResponseStatusUpdatedAt();
			this.resourceCheckInResponseStatusUpdatedAtDescription = patientOrder.getResourceCheckInResponseStatusUpdatedAt() == null ? null : formatter.formatTimestamp(patientOrder.getResourceCheckInResponseStatusUpdatedAt(), FormatStyle.MEDIUM, FormatStyle.SHORT);

			List<PatientOrderScheduledMessage> patientOrderScheduledMessages = patientOrderService.findPatientOrderScheduledMessagesByPatientOrderId(patientOrder.getPatientOrderId());
			this.patientOrderScheduledMessageGroups = patientOrderService.generatePatientOrderScheduledMessageGroupApiResponses(patientOrderScheduledMessages);

			this.patientOrderVoicemailTasks = patientOrderVoicemailTasks;
			this.patientOrderResourcingTypeId = patientOrder.getPatientOrderResourcingTypeId();
		}
	}

	@Nonnull
	public UUID getPatientOrderId() {
		return this.patientOrderId;
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
	public String getPatientEmailAddress() {
		return this.patientEmailAddress;
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
	public Integer getEpisodeDurationInDays() {
		return this.episodeDurationInDays;
	}

	@Nullable
	public String getEpisodeDurationInDaysDescription() {
		return this.episodeDurationInDaysDescription;
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
	public PatientOrderResourcingStatusId getPatientOrderResourcingStatusId() {
		return this.patientOrderResourcingStatusId;
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
	public String getResourcesSentNote() {
		return this.resourcesSentNote;
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
	public Integer getTotalOutreachCount() {
		return this.totalOutreachCount;
	}

	@Nullable
	public String getTotalOutreachCountDescription() {
		return this.totalOutreachCountDescription;
	}

	@Nullable
	public Integer getScheduledMessageGroupCount() {
		return this.scheduledMessageGroupCount;
	}

	@Nullable
	public String getScheduledMessageGroupCountDescription() {
		return this.scheduledMessageGroupCountDescription;
	}

	@Nullable
	public LocalDateTime getMostRecentScheduledMessageGroupDateTime() {
		return this.mostRecentScheduledMessageGroupDateTime;
	}

	@Nullable
	public String getMostRecentScheduledMessageGroupDateTimeDescription() {
		return this.mostRecentScheduledMessageGroupDateTimeDescription;
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
	public LocalDateTime getMostRecentTotalOutreachDateTime() {
		return this.mostRecentTotalOutreachDateTime;
	}

	@Nullable
	public String getMostRecentTotalOutreachDateTimeDescription() {
		return this.mostRecentTotalOutreachDateTimeDescription;
	}

	@Nullable
	public Instant getMostRecentScreeningSessionCreatedAt() {
		return this.mostRecentScreeningSessionCreatedAt;
	}

	@Nullable
	public String getMostRecentScreeningSessionCreatedAtDescription() {
		return this.mostRecentScreeningSessionCreatedAtDescription;
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

	@Nullable
	public PatientOrderDispositionId getPatientOrderDispositionId() {
		return this.patientOrderDispositionId;
	}

	@Nullable
	public String getPatientOrderScreeningStatusDescription() {
		return this.patientOrderScreeningStatusDescription;
	}

	@Nullable
	public String getPatientOrderDispositionDescription() {
		return this.patientOrderDispositionDescription;
	}

	@Nullable
	public PatientOrderTriageStatusId getPatientOrderTriageStatusId() {
		return this.patientOrderTriageStatusId;
	}

	@Nullable
	public String getPatientOrderTriageStatusDescription() {
		return this.patientOrderTriageStatusDescription;
	}

	@Nullable
	public String getPatientOrderClosureReasonDescription() {
		return this.patientOrderClosureReasonDescription;
	}

	@Nullable
	public Boolean getPatientBelowAgeThreshold() {
		return this.patientBelowAgeThreshold;
	}

	@Nullable
	public PatientOrderSafetyPlanningStatusId getPatientOrderSafetyPlanningStatusId() {
		return this.patientOrderSafetyPlanningStatusId;
	}

	@Nullable
	public Instant getConnectedToSafetyPlanningAt() {
		return this.connectedToSafetyPlanningAt;
	}

	@Nullable
	public String getConnectedToSafetyPlanningAtDescription() {
		return this.connectedToSafetyPlanningAtDescription;
	}

	@Nullable
	public Instant getEpisodeClosedAt() {
		return this.episodeClosedAt;
	}

	@Nullable
	public String getEpisodeClosedAtDescription() {
		return this.episodeClosedAtDescription;
	}

	@Nullable
	public UUID getEpisodeClosedByAccountId() {
		return this.episodeClosedByAccountId;
	}

	@Nullable
	public Instant getMostRecentEpisodeClosedAt() {
		return this.mostRecentEpisodeClosedAt;
	}

	@Nullable
	public String getMostRecentEpisodeClosedAtDescription() {
		return this.mostRecentEpisodeClosedAtDescription;
	}

	@Nullable
	public Boolean getMostRecentEpisodeClosedWithinDateThreshold() {
		return this.mostRecentEpisodeClosedWithinDateThreshold;
	}

	@Nullable
	public UUID getPatientOrderScheduledScreeningId() {
		return this.patientOrderScheduledScreeningId;
	}

	@Nullable
	public LocalDateTime getPatientOrderScheduledScreeningScheduledDateTime() {
		return this.patientOrderScheduledScreeningScheduledDateTime;
	}

	@Nullable
	public String getPatientOrderScheduledScreeningScheduledDateTimeDescription() {
		return this.patientOrderScheduledScreeningScheduledDateTimeDescription;
	}

	@Nullable
	public String getPatientOrderScheduledScreeningCalendarUrl() {
		return this.patientOrderScheduledScreeningCalendarUrl;
	}

	@Nullable
	public List<PatientOrderScheduledMessageGroupApiResponse> getPatientOrderScheduledMessageGroups() {
		return this.patientOrderScheduledMessageGroups;
	}

	@Nullable
	public List<PatientOrderVoicemailTaskApiResponse> getPatientOrderVoicemailTasks() {
		return this.patientOrderVoicemailTasks;
	}

	@Nullable
	public UUID getAppointmentId() {
		return this.appointmentId;
	}

	@Nullable
	public LocalDateTime getAppointmentStartTime() {
		return this.appointmentStartTime;
	}

	@Nullable
	public String getAppointmentStartTimeDescription() {
		return this.appointmentStartTimeDescription;
	}

	@Nullable
	public UUID getProviderId() {
		return this.providerId;
	}

	@Nullable
	public String getProviderName() {
		return this.providerName;
	}

	@Nullable
	public PatientOrderConsentStatusId getPatientOrderConsentStatusId() {
		return this.patientOrderConsentStatusId;
	}

	@Nullable
	public UUID getConsentStatusUpdatedByByAccountId() {
		return this.consentStatusUpdatedByByAccountId;
	}

	@Nullable
	public Instant getConsentStatusUpdatedAt() {
		return this.consentStatusUpdatedAt;
	}

	@Nullable
	public String getConsentStatusUpdatedAtDescription() {
		return this.consentStatusUpdatedAtDescription;
	}

	@Nullable
	public PatientOrderResourceCheckInResponseStatusId getPatientOrderResourceCheckInResponseStatusId() {
		return this.patientOrderResourceCheckInResponseStatusId;
	}

	@Nullable
	public UUID getResourceCheckInResponseStatusUpdatedByByAccountId() {
		return this.resourceCheckInResponseStatusUpdatedByByAccountId;
	}

	@Nullable
	public Instant getResourceCheckInResponseStatusUpdatedAt() {
		return this.resourceCheckInResponseStatusUpdatedAt;
	}

	@Nullable
	public String getResourceCheckInResponseStatusUpdatedAtDescription() {
		return this.resourceCheckInResponseStatusUpdatedAtDescription;
	}

	@Nullable
	public UUID getMostRecentPatientOrderVoicemailTaskId() {
		return this.mostRecentPatientOrderVoicemailTaskId;
	}

	@Nullable
	public Boolean getMostRecentPatientOrderVoicemailTaskCompleted() {
		return this.mostRecentPatientOrderVoicemailTaskCompleted;
	}

	@Nullable
	public UUID getPatientOrderInsurancePayorId() {
		return this.patientOrderInsurancePayorId;
	}

	@Nullable
	public PatientOrderInsurancePayorTypeId getPatientOrderInsurancePayorTypeId() {
		return this.patientOrderInsurancePayorTypeId;
	}

	@Nullable
	public String getPatientOrderInsurancePayorName() {
		return this.patientOrderInsurancePayorName;
	}

	@Nullable
	public UUID getPatientOrderInsurancePlanId() {
		return this.patientOrderInsurancePlanId;
	}

	@Nullable
	public String getPatientOrderInsurancePlanName() {
		return this.patientOrderInsurancePlanName;
	}

	@Nullable
	public PatientOrderInsurancePlanTypeId getPatientOrderInsurancePlanTypeId() {
		return this.patientOrderInsurancePlanTypeId;
	}

	@Nullable
	public Boolean getPatientOrderInsurancePlanAccepted() {
		return this.patientOrderInsurancePlanAccepted;
	}

	@Nullable
	public String getPatientAddressStreetAddress1() {
		return this.patientAddressStreetAddress1;
	}

	@Nullable
	public String getPatientAddressLocality() {
		return this.patientAddressLocality;
	}

	@Nullable
	public String getPatientAddressRegion() {
		return this.patientAddressRegion;
	}

	@Nullable
	public String getPatientAddressPostalCode() {
		return this.patientAddressPostalCode;
	}

	@Nullable
	public String getPatientAddressCountryCode() {
		return this.patientAddressCountryCode;
	}

	@Nullable
	public Boolean getPatientAddressRegionAccepted() {
		return this.patientAddressRegionAccepted;
	}

	@Nullable
	public Boolean getPatientDemographicsCompleted() {
		return this.patientDemographicsCompleted;
	}

	@Nullable
	public Boolean getPatientDemographicsAccepted() {
		return this.patientDemographicsAccepted;
	}

	@Nullable
	public Instant getPatientDemographicsConfirmedAt() {
		return this.patientDemographicsConfirmedAt;
	}

	@Nullable
	public String getPatientDemographicsConfirmedAtDescription() {
		return this.patientDemographicsConfirmedAtDescription;
	}

	@Nullable
	public UUID getPatientDemographicsConfirmedByAccountId() {
		return this.patientDemographicsConfirmedByAccountId;
	}

	@Nullable
	public PatientOrderResourcingTypeId getPatientOrderResourcingTypeId() {
		return this.patientOrderResourcingTypeId;
	}

	@Nullable
	public Integer getPatientAgeOnOrderDate() {
		return this.patientAgeOnOrderDate;
	}

	@Nullable
	public String getPatientAgeOnOrderDateDescription() {
		return this.patientAgeOnOrderDateDescription;
	}

	@Nullable
	public PatientOrderCarePreferenceId getPatientOrderCarePreferenceId() {
		return this.patientOrderCarePreferenceId;
	}

	@Nullable
	public Integer getInPersonCareRadius() {
		return this.inPersonCareRadius;
	}

	@Nullable
	public String getInPersonCareRadiusDescription() {
		return this.inPersonCareRadiusDescription;
	}

	@Nullable
	public DistanceUnitId getInPersonCareRadiusDistanceUnitId() {
		return this.inPersonCareRadiusDistanceUnitId;
	}

	@Nullable
	public String getInPersonCareRadiusWithDistanceUnitDescription() {
		return this.inPersonCareRadiusWithDistanceUnitDescription;
	}
}