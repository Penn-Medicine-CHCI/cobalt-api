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
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.error.ErrorReporter;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.messaging.sms.SmsMessage;
import com.cobaltplatform.api.messaging.sms.SmsMessageTemplate;
import com.cobaltplatform.api.model.api.request.ArchivePatientOrderRequest;
import com.cobaltplatform.api.model.api.request.AssignPatientOrdersRequest;
import com.cobaltplatform.api.model.api.request.CancelPatientOrderScheduledScreeningRequest;
import com.cobaltplatform.api.model.api.request.ClosePatientOrderRequest;
import com.cobaltplatform.api.model.api.request.CompletePatientOrderVoicemailTaskRequest;
import com.cobaltplatform.api.model.api.request.CreateAddressRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderEventRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderImportRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderNoteRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderOutreachRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderRequest.CreatePatientOrderDiagnosisRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderRequest.CreatePatientOrderMedicationRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderScheduledMessageGroupRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderScheduledScreeningRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderVoicemailTaskRequest;
import com.cobaltplatform.api.model.api.request.CreateScheduledMessageRequest;
import com.cobaltplatform.api.model.api.request.DeletePatientOrderNoteRequest;
import com.cobaltplatform.api.model.api.request.DeletePatientOrderOutreachRequest;
import com.cobaltplatform.api.model.api.request.DeletePatientOrderScheduledMessageGroupRequest;
import com.cobaltplatform.api.model.api.request.DeletePatientOrderVoicemailTaskRequest;
import com.cobaltplatform.api.model.api.request.FindPatientOrdersRequest;
import com.cobaltplatform.api.model.api.request.FindPatientOrdersRequest.PatientOrderSortColumnId;
import com.cobaltplatform.api.model.api.request.FindPatientOrdersRequest.PatientOrderSortRule;
import com.cobaltplatform.api.model.api.request.OpenPatientOrderRequest;
import com.cobaltplatform.api.model.api.request.PatchPatientOrderRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderConsentStatusRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderFollowupNeededRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderNoteRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderOutreachNeededRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderOutreachRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderResourcingStatusRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderSafetyPlanningStatusRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderScheduledMessageGroupRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderScheduledScreeningRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderTriagesRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderTriagesRequest.CreatePatientOrderTriageRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderVoicemailTaskRequest;
import com.cobaltplatform.api.model.api.response.PatientOrderScheduledMessageGroupApiResponse;
import com.cobaltplatform.api.model.api.response.PatientOrderScheduledMessageGroupApiResponse.PatientOrderScheduledMessageGroupApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.BirthSex.BirthSexId;
import com.cobaltplatform.api.model.db.Ethnicity.EthnicityId;
import com.cobaltplatform.api.model.db.GenderIdentity.GenderIdentityId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.MessageType.MessageTypeId;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.PatientOrderCareType;
import com.cobaltplatform.api.model.db.PatientOrderClosureReason;
import com.cobaltplatform.api.model.db.PatientOrderClosureReason.PatientOrderClosureReasonId;
import com.cobaltplatform.api.model.db.PatientOrderConsentStatus.PatientOrderConsentStatusId;
import com.cobaltplatform.api.model.db.PatientOrderDiagnosis;
import com.cobaltplatform.api.model.db.PatientOrderDisposition;
import com.cobaltplatform.api.model.db.PatientOrderDisposition.PatientOrderDispositionId;
import com.cobaltplatform.api.model.db.PatientOrderEventType.PatientOrderEventTypeId;
import com.cobaltplatform.api.model.db.PatientOrderFocusType;
import com.cobaltplatform.api.model.db.PatientOrderImport;
import com.cobaltplatform.api.model.db.PatientOrderImportType.PatientOrderImportTypeId;
import com.cobaltplatform.api.model.db.PatientOrderInsurancePayor;
import com.cobaltplatform.api.model.db.PatientOrderInsurancePayorType.PatientOrderInsurancePayorTypeId;
import com.cobaltplatform.api.model.db.PatientOrderInsurancePlan;
import com.cobaltplatform.api.model.db.PatientOrderInsurancePlanType.PatientOrderInsurancePlanTypeId;
import com.cobaltplatform.api.model.db.PatientOrderMedication;
import com.cobaltplatform.api.model.db.PatientOrderNote;
import com.cobaltplatform.api.model.db.PatientOrderOutreach;
import com.cobaltplatform.api.model.db.PatientOrderOutreachResult;
import com.cobaltplatform.api.model.db.PatientOrderResourcingStatus.PatientOrderResourcingStatusId;
import com.cobaltplatform.api.model.db.PatientOrderResourcingType;
import com.cobaltplatform.api.model.db.PatientOrderResourcingType.PatientOrderResourcingTypeId;
import com.cobaltplatform.api.model.db.PatientOrderSafetyPlanningStatus.PatientOrderSafetyPlanningStatusId;
import com.cobaltplatform.api.model.db.PatientOrderScheduledMessage;
import com.cobaltplatform.api.model.db.PatientOrderScheduledMessageGroup;
import com.cobaltplatform.api.model.db.PatientOrderScheduledMessageType;
import com.cobaltplatform.api.model.db.PatientOrderScheduledMessageType.PatientOrderScheduledMessageTypeId;
import com.cobaltplatform.api.model.db.PatientOrderScheduledScreening;
import com.cobaltplatform.api.model.db.PatientOrderTriage;
import com.cobaltplatform.api.model.db.PatientOrderTriageSource.PatientOrderTriageSourceId;
import com.cobaltplatform.api.model.db.PatientOrderTriageStatus.PatientOrderTriageStatusId;
import com.cobaltplatform.api.model.db.PatientOrderVoicemailTask;
import com.cobaltplatform.api.model.db.Race.RaceId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.ScheduledMessageStatus.ScheduledMessageStatusId;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;
import com.cobaltplatform.api.model.service.AdvisoryLock;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.model.service.IcTestPatientEmailAddress;
import com.cobaltplatform.api.model.service.PatientOrderAssignmentStatusId;
import com.cobaltplatform.api.model.service.PatientOrderAutocompleteResult;
import com.cobaltplatform.api.model.service.PatientOrderFilterFlagTypeId;
import com.cobaltplatform.api.model.service.PatientOrderImportResult;
import com.cobaltplatform.api.model.service.PatientOrderOutreachStatusId;
import com.cobaltplatform.api.model.service.PatientOrderResponseStatusId;
import com.cobaltplatform.api.model.service.PatientOrderViewTypeId;
import com.cobaltplatform.api.model.service.SortDirectionId;
import com.cobaltplatform.api.model.service.SortNullsId;
import com.cobaltplatform.api.util.Authenticator;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.Normalizer;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
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
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlInListPlaceholders;
import static com.cobaltplatform.api.util.DatabaseUtility.sqlVaragsParameters;
import static com.cobaltplatform.api.util.ValidationUtility.isValidEmailAddress;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class PatientOrderService implements AutoCloseable {
	@Nonnull
	private static final Long BACKGROUND_TASK_INTERVAL_IN_SECONDS;
	@Nonnull
	private static final Long BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS;

	static {
		BACKGROUND_TASK_INTERVAL_IN_SECONDS = 60L * 1L;
		BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS = 10L;
	}

	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final Provider<AddressService> addressServiceProvider;
	@Nonnull
	private final Provider<MessageService> messageServiceProvider;
	@Nonnull
	private final Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final Provider<BackgroundTask> backgroundTaskProvider;
	@Nonnull
	private final PatientOrderScheduledMessageGroupApiResponseFactory patientOrderScheduledMessageGroupApiResponseFactory;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Normalizer normalizer;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Authenticator authenticator;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Gson gson;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Nonnull
	private final ReentrantLock backgroundTaskLock;
	@Nonnull
	private Boolean backgroundTaskStarted;
	@Nullable
	private ScheduledExecutorService backgroundTaskExecutorService;

	@Inject
	public PatientOrderService(@Nonnull Provider<AddressService> addressServiceProvider,
														 @Nonnull Provider<AccountService> accountServiceProvider,
														 @Nonnull Provider<MessageService> messageServiceProvider,
														 @Nonnull Provider<InstitutionService> institutionServiceProvider,
														 @Nonnull Provider<BackgroundTask> backgroundTaskProvider,
														 @Nonnull PatientOrderScheduledMessageGroupApiResponseFactory patientOrderScheduledMessageGroupApiResponseFactory,
														 @Nonnull Database database,
														 @Nonnull Normalizer normalizer,
														 @Nonnull Formatter formatter,
														 @Nonnull Authenticator authenticator,
														 @Nonnull Configuration configuration,
														 @Nonnull Strings strings) {
		requireNonNull(addressServiceProvider);
		requireNonNull(accountServiceProvider);
		requireNonNull(messageServiceProvider);
		requireNonNull(institutionServiceProvider);
		requireNonNull(backgroundTaskProvider);
		requireNonNull(patientOrderScheduledMessageGroupApiResponseFactory);
		requireNonNull(database);
		requireNonNull(normalizer);
		requireNonNull(formatter);
		requireNonNull(authenticator);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.addressServiceProvider = addressServiceProvider;
		this.accountServiceProvider = accountServiceProvider;
		this.messageServiceProvider = messageServiceProvider;
		this.institutionServiceProvider = institutionServiceProvider;
		this.backgroundTaskProvider = backgroundTaskProvider;
		this.patientOrderScheduledMessageGroupApiResponseFactory = patientOrderScheduledMessageGroupApiResponseFactory;
		this.database = database;
		this.normalizer = normalizer;
		this.formatter = formatter;
		this.authenticator = authenticator;
		this.configuration = configuration;
		this.gson = createGson();
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());

		this.backgroundTaskLock = new ReentrantLock();
		this.backgroundTaskStarted = false;
	}

	@Override
	public void close() throws Exception {
		stopBackgroundTasks();
	}

	@Nonnull
	public Boolean startBackgroundTasks() {
		getBackgroundTaskLock().lock();

		try {
			if (isBackgroundTaskStarted())
				return false;

			getLogger().trace("Starting Patient Order background tasks...");

			this.backgroundTaskExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("patient-order-background-task").build());
			this.backgroundTaskStarted = true;

			getBackgroundTaskExecutorService().get().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						getBackgroundTaskProvider().get().run();
					} catch (Exception e) {
						getLogger().warn(format("Unable to complete Patient Order background task - will retry in %s seconds", String.valueOf(getBackgroundTaskIntervalInSeconds())), e);
					}
				}
			}, getBackgroundTaskInitialDelayInSeconds(), getBackgroundTaskIntervalInSeconds(), TimeUnit.SECONDS);

			getLogger().trace("Patient Order background tasks started.");

			return true;
		} finally {
			getBackgroundTaskLock().unlock();
		}
	}

	@Nonnull
	public Boolean stopBackgroundTasks() {
		getBackgroundTaskLock().lock();

		try {
			if (!isBackgroundTaskStarted())
				return false;

			getLogger().trace("Stopping Patient Order background tasks...");

			getBackgroundTaskExecutorService().get().shutdownNow();
			this.backgroundTaskExecutorService = null;
			this.backgroundTaskStarted = false;

			getLogger().trace("Patient Order background tasks stopped.");

			return true;
		} finally {
			getBackgroundTaskLock().unlock();
		}
	}

	@Nonnull
	public Optional<PatientOrder> findPatientOrderById(@Nullable UUID patientOrderId) {
		if (patientOrderId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_patient_order 
				WHERE patient_order_id=?
				""", PatientOrder.class, patientOrderId);
	}

	@Nonnull
	public Optional<PatientOrderImport> findPatientOrderImportById(@Nullable UUID patientOrderImportId) {
		if (patientOrderImportId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM patient_order_import
				WHERE patient_order_import_id=?
				""", PatientOrderImport.class, patientOrderImportId);
	}

	@Nonnull
	public List<PatientOrder> findPatientOrdersByPatientAccountId(@Nullable UUID accountId) {
		if (accountId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM v_patient_order
				WHERE patient_account_id=?
				AND patient_order_disposition_id != ?
				ORDER BY order_date DESC, order_age_in_minutes
				""", PatientOrder.class, accountId, PatientOrderDispositionId.ARCHIVED);
	}

	@Nonnull
	public List<PatientOrder> findPatientOrdersByPatientOrderImportId(@Nullable UUID patientOrderImportId) {
		if (patientOrderImportId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM v_patient_order
				WHERE patient_order_import_id=?
				AND patient_order_disposition_id != ?
				ORDER BY order_date DESC, order_age_in_minutes
				""", PatientOrder.class, patientOrderImportId, PatientOrderDispositionId.ARCHIVED);
	}

	@Nonnull
	public List<PatientOrder> findPatientOrdersByMrnAndInstitutionId(@Nullable String patientMrn,
																																	 @Nullable InstitutionId institutionId) {
		patientMrn = trimToNull(patientMrn);

		if (patientMrn == null || institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM v_patient_order
				WHERE UPPER(?)=UPPER(patient_mrn)
				AND institution_id=?
				AND patient_order_disposition_id != ?
				ORDER BY order_date DESC, order_age_in_minutes
				""", PatientOrder.class, patientMrn, institutionId, PatientOrderDispositionId.ARCHIVED);
	}

	@Nonnull
	public Optional<PatientOrder> findOpenPatientOrderByMrnAndInstitutionId(@Nullable String patientMrn,
																																					@Nullable InstitutionId institutionId) {
		patientMrn = trimToNull(patientMrn);

		if (patientMrn == null || institutionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_patient_order
				WHERE UPPER(?)=UPPER(patient_mrn)
				AND institution_id=?
				AND patient_order_disposition_id=?
				""", PatientOrder.class, patientMrn, institutionId, PatientOrderDispositionId.OPEN);
	}

	@Nonnull
	public Optional<PatientOrder> findLatestPatientOrderByMrnAndInstitutionId(@Nullable String patientMrn,
																																						@Nullable InstitutionId institutionId) {
		patientMrn = trimToNull(patientMrn);

		if (patientMrn == null || institutionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_patient_order
				WHERE UPPER(?)=UPPER(patient_mrn)
				AND institution_id=?
				ORDER BY order_date DESC
				LIMIT 1
				""", PatientOrder.class, patientMrn, institutionId);
	}

	@Nonnull
	public Optional<PatientOrder> findOpenPatientOrderByPatientAccountId(@Nullable UUID patientAccountId) {
		if (patientAccountId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_patient_order
				WHERE patient_account_id=?
				AND patient_order_disposition_id=?
				""", PatientOrder.class, patientAccountId, PatientOrderDispositionId.OPEN);
	}

	@Nonnull
	public Optional<PatientOrder> findPatientOrderByScreeningSessionId(@Nullable UUID screeningSessionId) {
		if (screeningSessionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT po.*
				FROM v_patient_order po, screening_session ss
				WHERE ss.screening_session_id=?
				AND ss.patient_order_id=po.patient_order_id
				""", PatientOrder.class, screeningSessionId);
	}

	@Nonnull
	public List<PatientOrder> findPatientOrdersByTestPatientEmailAddressAndInstitutionId(@Nullable IcTestPatientEmailAddress icTestPatientEmailAddress,
																																											 @Nullable InstitutionId institutionId) {
		if (icTestPatientEmailAddress == null || institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM v_patient_order
				WHERE UPPER(?)=UPPER(test_patient_email_address)
				AND institution_id=?
				AND patient_order_disposition_id != ?
				ORDER BY order_date DESC, order_age_in_minutes    
				""", PatientOrder.class, icTestPatientEmailAddress.getEmailAddress(), institutionId, PatientOrderDispositionId.ARCHIVED);
	}

	@Nonnull
	public List<String> findReferringPracticeNamesByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT DISTINCT ON (UPPER(referring_practice_name)) referring_practice_name
				FROM patient_order
				WHERE institution_id=?
				ORDER BY UPPER(referring_practice_name)
				""", String.class, institutionId);
	}

	@Nonnull
	public List<String> findReasonsForReferralByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT DISTINCT ON (UPPER(porfr.reason_for_referral)) reason_for_referral
				FROM patient_order_reason_for_referral porfr, patient_order p
				WHERE p.patient_order_id=porfr.patient_order_id
				AND p.institution_id=?
				ORDER BY UPPER(porfr.reason_for_referral)
				""", String.class, institutionId);
	}

	@Nonnull
	public List<PatientOrderInsurancePayor> findPatientOrderInsurancePayorsByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM patient_order_insurance_payor
				WHERE institution_id=?
				AND deleted=FALSE
				ORDER BY display_order
				""", PatientOrderInsurancePayor.class, institutionId);
	}

	@Nonnull
	public List<PatientOrderInsurancePlan> findPatientOrderInsurancePlansByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT poipl.*
				FROM patient_order_insurance_plan poipl, patient_order_insurance_payor poipa
				WHERE poipl.patient_order_insurance_payor_id=poipa.patient_order_insurance_payor_id
				AND poipa.institution_id=?
				AND poipl.deleted=FALSE
				AND poipa.deleted=FALSE
				ORDER BY poipl.display_order
				""", PatientOrderInsurancePlan.class, institutionId);
	}

	@Nonnull
	public List<PatientOrderInsurancePlan> findPatientOrderInsurancePlansByPatientOrderInsurancePayorId(@Nullable UUID patientOrderInsurancePayorId) {
		if (patientOrderInsurancePayorId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM patient_order_insurance_plan
				WHERE patient_order_insurance_payor_id=?
				AND deleted=FALSE
				ORDER BY display_order
				""", PatientOrderInsurancePlan.class, patientOrderInsurancePayorId);
	}

	@Nonnull
	public List<PatientOrderClosureReason> findPatientOrderClosureReasons() {
		return getDatabase().queryForList("""
				SELECT *
				FROM patient_order_closure_reason
				WHERE patient_order_closure_reason_id != ?
				ORDER BY display_order
				""", PatientOrderClosureReason.class, PatientOrderClosureReasonId.NOT_CLOSED);
	}

	@Nonnull
	public List<PatientOrderDiagnosis> findPatientOrderDiagnosesByPatientOrderId(@Nullable UUID patientOrderId) {
		if (patientOrderId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM patient_order_diagnosis
				WHERE patient_order_id=?
				ORDER BY display_order
				""", PatientOrderDiagnosis.class, patientOrderId);
	}

	@Nonnull
	public List<PatientOrderMedication> findPatientOrderMedicationsByPatientOrderId(@Nullable UUID patientOrderId) {
		if (patientOrderId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT * 
				FROM patient_order_medication
				WHERE patient_order_id=?
				ORDER BY display_order
				""", PatientOrderMedication.class, patientOrderId);
	}

	@Nonnull
	public Optional<PatientOrderNote> findPatientOrderNoteById(@Nullable UUID patientOrderNoteId) {
		if (patientOrderNoteId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT * 
				FROM patient_order_note
				WHERE patient_order_note_id=?
				""", PatientOrderNote.class, patientOrderNoteId);
	}

	@Nonnull
	public List<PatientOrderNote> findPatientOrderNotesByPatientOrderId(@Nullable UUID patientOrderId) {
		if (patientOrderId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM patient_order_note
				WHERE patient_order_id=?
				AND deleted=FALSE
				ORDER BY created DESC
				""", PatientOrderNote.class, patientOrderId);
	}

	@Nonnull
	public Optional<PatientOrderOutreach> findPatientOrderOutreachById(@Nullable UUID patientOrderOutreachId) {
		if (patientOrderOutreachId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM patient_order_outreach
				WHERE patient_order_outreach_id=?
				""", PatientOrderOutreach.class, patientOrderOutreachId);
	}

	@Nonnull
	public List<PatientOrderOutreach> findPatientOrderOutreachesByPatientOrderId(@Nullable UUID patientOrderId) {
		if (patientOrderId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM patient_order_outreach
				WHERE patient_order_id=?
				AND deleted=FALSE
				ORDER BY created DESC
				""", PatientOrderOutreach.class, patientOrderId);
	}

	@Nonnull
	public List<PatientOrderResourcingType> findPatientOrderResourcingTypesByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		// Don't use institutionId currently, but we might in the future

		return getDatabase().queryForList("""
				SELECT *
				FROM patient_order_resourcing_type
				ORDER BY display_order
				""", PatientOrderResourcingType.class);
	}

	@Nonnull
	public List<Account> findPanelAccountsByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		// Panel accounts are either MHICs or any account that has been assigned to manage a panel
		// (this might be some kind of administrator, for example)
		return getDatabase().queryForList("""
				SELECT *
				FROM account
				WHERE institution_id=?
				AND (role_id=? OR account_id IN (SELECT panel_account_id FROM patient_order WHERE institution_id=?))
				ORDER BY first_name, last_name, account_id
				""", Account.class, institutionId, RoleId.MHIC, institutionId);
	}

	@Nonnull
	public Map<UUID, Integer> findOpenPatientOrderCountsByPanelAccountIdForInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Map.of();

		List<AccountIdWithCount> accountIdsWithCount = getDatabase().queryForList("""
				SELECT a.account_id, COUNT(po.*)
				FROM account a, patient_order po
				WHERE a.account_id=po.panel_account_id
				AND po.institution_id=?
				AND po.patient_order_disposition_id=?
				GROUP BY a.account_id
				""", AccountIdWithCount.class, institutionId, PatientOrderDispositionId.OPEN);

		return accountIdsWithCount.stream()
				.collect(Collectors.toMap(AccountIdWithCount::getAccountId, AccountIdWithCount::getCount));
	}

	@Nonnull
	public Map<PatientOrderViewTypeId, Integer> findPatientOrderCountsByPatientOrderViewTypeIdForInstitutionId(@Nullable InstitutionId institutionId,
																																																						 @Nullable UUID panelAccountId) {
		Map<PatientOrderViewTypeId, Integer> patientOrderCountsByPatientOrderViewTypeId = new HashMap<>(PatientOrderViewTypeId.values().length);

		// Seed the map with zeroes for all possible view types
		for (PatientOrderViewTypeId patientOrderViewTypeId : PatientOrderViewTypeId.values())
			patientOrderCountsByPatientOrderViewTypeId.put(patientOrderViewTypeId, 0);

		if (institutionId == null)
			return patientOrderCountsByPatientOrderViewTypeId;

		// SCHEDULED
		patientOrderCountsByPatientOrderViewTypeId.put(PatientOrderViewTypeId.SCHEDULED, findScheduledPatientOrderCountForInstitutionId(institutionId, panelAccountId));

		// NEED_ASSESSMENT
		// SUBCLINICAL
		// MHP
		// SPECIALTY_CARE
		Map<PatientOrderTriageStatusId, Integer> countsByPatientOrderTriageStatusId = findPatientOrderTriageStatusCountsForInstitutionId(institutionId, panelAccountId);
		patientOrderCountsByPatientOrderViewTypeId.put(PatientOrderViewTypeId.NEED_ASSESSMENT, countsByPatientOrderTriageStatusId.get(PatientOrderTriageStatusId.NEEDS_ASSESSMENT));
		patientOrderCountsByPatientOrderViewTypeId.put(PatientOrderViewTypeId.SPECIALTY_CARE, countsByPatientOrderTriageStatusId.get(PatientOrderTriageStatusId.SPECIALTY_CARE));
		patientOrderCountsByPatientOrderViewTypeId.put(PatientOrderViewTypeId.SUBCLINICAL, countsByPatientOrderTriageStatusId.get(PatientOrderTriageStatusId.SUBCLINICAL));
		patientOrderCountsByPatientOrderViewTypeId.put(PatientOrderViewTypeId.MHP, countsByPatientOrderTriageStatusId.get(PatientOrderTriageStatusId.MHP));

		// CLOSED
		patientOrderCountsByPatientOrderViewTypeId.put(PatientOrderViewTypeId.CLOSED, findPatientOrderDispositionCountForInstitutionId(institutionId, panelAccountId, PatientOrderDispositionId.CLOSED));

		return patientOrderCountsByPatientOrderViewTypeId;
	}

	@Nonnull
	public Integer findScheduledPatientOrderCountForInstitutionId(@Nullable InstitutionId institutionId,
																																@Nullable UUID panelAccountId) {
		if (institutionId == null)
			return 0;

		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		LocalDateTime now = LocalDateTime.now(institution.getTimeZone());

		List<String> whereClauseLines = new ArrayList<>();
		List<Object> parameters = new ArrayList<>();

		parameters.add(institutionId);

		// Default to OPEN orders
		whereClauseLines.add("AND patient_order_disposition_id=?");
		parameters.add(PatientOrderDispositionId.OPEN);

		// Only those orders scheduled for the future
		// TODO: is this the correct criteria?
		whereClauseLines.add("AND patient_order_scheduled_screening_scheduled_date_time IS NOT NULL");
		whereClauseLines.add("AND patient_order_scheduled_screening_scheduled_date_time >= ?");
		parameters.add(now);

		if (panelAccountId != null) {
			whereClauseLines.add("AND panel_account_id=?");
			parameters.add(panelAccountId);
		}

		String sql = """
				  SELECT COUNT(*)
				  FROM v_patient_order
				  WHERE institution_id=?
				  {{whereClauseLines}}
				""".trim()
				.replace("{{whereClauseLines}}", whereClauseLines.stream().collect(Collectors.joining("\n")));

		return getDatabase().queryForObject(sql, Integer.class, sqlVaragsParameters(parameters)).get();
	}

	@Nonnull
	public Map<PatientOrderTriageStatusId, Integer> findPatientOrderTriageStatusCountsForInstitutionId(@Nullable InstitutionId institutionId,
																																																		 @Nullable UUID panelAccountId) {
		if (institutionId == null)
			return Map.of();

		List<String> whereClauseLines = new ArrayList<>();
		List<Object> parameters = new ArrayList<>();

		// Special parameters in the SELECT for our filter operations
		parameters.add(PatientOrderTriageStatusId.NEEDS_ASSESSMENT);
		parameters.add(PatientOrderTriageStatusId.SUBCLINICAL);
		parameters.add(PatientOrderTriageStatusId.MHP);
		parameters.add(PatientOrderTriageStatusId.SPECIALTY_CARE);

		parameters.add(institutionId);

		// Default to OPEN orders
		whereClauseLines.add("AND patient_order_disposition_id=?");
		parameters.add(PatientOrderDispositionId.OPEN);

		if (panelAccountId != null) {
			whereClauseLines.add("AND panel_account_id=?");
			parameters.add(panelAccountId);
		}

		String sql = """
				  SELECT institution_id,
				  COUNT(1) FILTER (where patient_order_triage_status_id = ?) as needs_assessment_count,
				  COUNT(1) FILTER (where patient_order_triage_status_id = ?) as subclinical_count,
				  COUNT(1) FILTER (where patient_order_triage_status_id = ?) as mhp_count,
				  COUNT(1) FILTER (where patient_order_triage_status_id = ?) as specialty_care_count
				  FROM v_patient_order
				  WHERE institution_id=?
				  {{whereClauseLines}}
				  GROUP BY institution_id
				""".trim()
				.replace("{{whereClauseLines}}", whereClauseLines.stream().collect(Collectors.joining("\n")));

		PatientOrderTriageStatusCountsResult patientOrderTriageStatusCountsResult = getDatabase().queryForObject(sql,
				PatientOrderTriageStatusCountsResult.class, sqlVaragsParameters(parameters)).orElse(null);

		Map<PatientOrderTriageStatusId, Integer> countsByPatientOrderTriageStatusId = new HashMap<>(PatientOrderTriageStatusId.values().length);

		// Initialize each enum count to 0...
		for (PatientOrderTriageStatusId patientOrderTriageStatusId : PatientOrderTriageStatusId.values())
			countsByPatientOrderTriageStatusId.put(patientOrderTriageStatusId, 0);

		// ...then, overwrite with results (if any) from our query
		if (patientOrderTriageStatusCountsResult != null) {
			countsByPatientOrderTriageStatusId.put(PatientOrderTriageStatusId.NEEDS_ASSESSMENT, patientOrderTriageStatusCountsResult.getNeedsAssessmentCount());
			countsByPatientOrderTriageStatusId.put(PatientOrderTriageStatusId.SUBCLINICAL, patientOrderTriageStatusCountsResult.getSubclinicalCount());
			countsByPatientOrderTriageStatusId.put(PatientOrderTriageStatusId.MHP, patientOrderTriageStatusCountsResult.getMhpCount());
			countsByPatientOrderTriageStatusId.put(PatientOrderTriageStatusId.SPECIALTY_CARE, patientOrderTriageStatusCountsResult.getSpecialtyCareCount());
		}

		return countsByPatientOrderTriageStatusId;
	}

	@NotThreadSafe
	protected static class PatientOrderTriageStatusCountsResult {
		@Nullable
		private Integer needsAssessmentCount;
		@Nullable
		private Integer subclinicalCount;
		@Nullable
		private Integer mhpCount;
		@Nullable
		private Integer specialtyCareCount;

		@Nullable
		public Integer getNeedsAssessmentCount() {
			return this.needsAssessmentCount;
		}

		public void setNeedsAssessmentCount(@Nullable Integer needsAssessmentCount) {
			this.needsAssessmentCount = needsAssessmentCount;
		}

		@Nullable
		public Integer getSubclinicalCount() {
			return this.subclinicalCount;
		}

		public void setSubclinicalCount(@Nullable Integer subclinicalCount) {
			this.subclinicalCount = subclinicalCount;
		}

		@Nullable
		public Integer getMhpCount() {
			return this.mhpCount;
		}

		public void setMhpCount(@Nullable Integer mhpCount) {
			this.mhpCount = mhpCount;
		}

		@Nullable
		public Integer getSpecialtyCareCount() {
			return this.specialtyCareCount;
		}

		public void setSpecialtyCareCount(@Nullable Integer specialtyCareCount) {
			this.specialtyCareCount = specialtyCareCount;
		}
	}

	@Nonnull
	public Integer findPatientOrderDispositionCountForInstitutionId(@Nullable InstitutionId institutionId,
																																	@Nullable UUID panelAccountId,
																																	@Nullable PatientOrderDispositionId patientOrderDispositionId) {
		if (institutionId == null || patientOrderDispositionId == null)
			return 0;

		List<String> whereClauseLines = new ArrayList<>();
		List<Object> parameters = new ArrayList<>();

		parameters.add(institutionId);

		whereClauseLines.add("AND patient_order_disposition_id=?");
		parameters.add(patientOrderDispositionId);

		if (panelAccountId != null) {
			whereClauseLines.add("AND panel_account_id=?");
			parameters.add(panelAccountId);
		}

		String sql = """
				  SELECT COUNT(*)
				  FROM v_patient_order
				  WHERE institution_id=?
				  {{whereClauseLines}}
				""".trim()
				.replace("{{whereClauseLines}}", whereClauseLines.stream().collect(Collectors.joining("\n")));

		return getDatabase().queryForObject(sql, Integer.class, sqlVaragsParameters(parameters)).get();
	}

	@Nonnull
	public Boolean arePatientOrderIdsAssociatedWithInstitutionId(@Nullable Collection<UUID> patientOrderIds,
																															 @Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);

		if (patientOrderIds == null)
			return true;

		Set<UUID> uniquePatientOrderIds = patientOrderIds.stream()
				.filter(patientOrderId -> patientOrderId != null)
				.collect(Collectors.toSet());

		List<Object> parameters = new ArrayList<>(uniquePatientOrderIds);
		parameters.add(institutionId);

		int orderCount = getDatabase().queryForObject(format("""
						SELECT COUNT(*)
						FROM patient_order
						WHERE patient_order_id IN %s
						AND institution_id=?
						""",
				sqlInListPlaceholders(uniquePatientOrderIds)), Integer.class, parameters.toArray(new Object[]{})).get();

		return uniquePatientOrderIds.size() == orderCount;
	}

	@Nonnull
	public Integer assignPatientOrdersToPanelAccount(@Nonnull AssignPatientOrdersRequest request) {
		requireNonNull(request);

		UUID assignedByAccountId = request.getAssignedByAccountId();
		UUID panelAccountId = request.getPanelAccountId();
		Set<UUID> patientOrderIds = request.getPatientOrderIds() == null ? Set.of() : request.getPatientOrderIds().stream()
				.filter(patientOrderId -> patientOrderId != null)
				.collect(Collectors.toSet());
		ValidationException validationException = new ValidationException();

		if (assignedByAccountId == null)
			validationException.add(new FieldError("assignedByAccountId", getStrings().get("Assigned-by Account ID is required.")));

		if (panelAccountId == null)
			validationException.add(new FieldError("panelAccountId", getStrings().get("Panel Account ID is required.")));

		if (patientOrderIds.size() == 0)
			validationException.add(new FieldError("patientOrderIds", getStrings().get("Please select at least one order to assign.")));

		int assignedCount = 0;

		for (UUID patientOrderId : patientOrderIds) {
			boolean assigned = assignPatientOrderToPanelAccount(patientOrderId, panelAccountId, assignedByAccountId);

			if (assigned)
				++assignedCount;
		}

		return assignedCount;
	}

	@Nonnull
	protected Boolean assignPatientOrderToPanelAccount(@Nonnull UUID patientOrderId,
																										 @Nullable UUID panelAccountId, // Null panel account removes the order from the panel
																										 @Nullable UUID assignedByAccountId) {
		requireNonNull(patientOrderId);

		PatientOrder patientOrder = findPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			return false;

		boolean assigned = getDatabase().execute("""
				UPDATE patient_order
				SET panel_account_id=?
				WHERE patient_order_id=?
				""", panelAccountId, patientOrderId) > 0;

		if (assigned) {
			if (panelAccountId == null)
				getLogger().info("Patient order ID {} was removed from panel account ID {}", patientOrderId, patientOrder.getPanelAccountId());
			else
				getLogger().info("Patient order ID {} was added to panel account ID {}", patientOrderId, panelAccountId);

			Map<String, Object> metadata = new HashMap<>();
			metadata.put("oldPanelAccountId", patientOrder.getPanelAccountId());
			metadata.put("newPanelAccountId", panelAccountId);

			createPatientOrderEvent(new CreatePatientOrderEventRequest() {
				{
					setPatientOrderEventTypeId(PatientOrderEventTypeId.PANEL_ACCOUNT_CHANGED);
					setPatientOrderId(patientOrderId);
					setAccountId(assignedByAccountId);
					setMessage(panelAccountId == null ? "Removed from panel." : "Assigned to panel."); // Not localized on the way in
					setMetadata(metadata);
				}
			});

			// TODO: any other action?  Send a notification?
		}

		return assigned;
	}

	@Nonnull
	public FindResult<PatientOrder> findPatientOrders(@Nonnull FindPatientOrdersRequest request) {
		requireNonNull(request);

		InstitutionId institutionId = request.getInstitutionId();
		PatientOrderConsentStatusId patientOrderConsentStatusId = request.getPatientOrderConsentStatusId();
		PatientOrderDispositionId patientOrderDispositionId = request.getPatientOrderDispositionId();
		Set<PatientOrderTriageStatusId> patientOrderTriageStatusIds = request.getPatientOrderTriageStatusIds() == null ? Set.of() : request.getPatientOrderTriageStatusIds();
		PatientOrderAssignmentStatusId patientOrderAssignmentStatusId = request.getPatientOrderAssignmentStatusId();
		PatientOrderOutreachStatusId patientOrderOutreachStatusId = request.getPatientOrderOutreachStatusId();
		PatientOrderResponseStatusId patientOrderResponseStatusId = request.getPatientOrderResponseStatusId();
		PatientOrderSafetyPlanningStatusId patientOrderSafetyPlanningStatusId = request.getPatientOrderSafetyPlanningStatusId();
		Set<PatientOrderFilterFlagTypeId> patientOrderFilterFlagTypeIds = request.getPatientOrderFilterFlagTypeIds() == null ? Set.of() : request.getPatientOrderFilterFlagTypeIds();
		Set<String> referringPracticeNames = request.getReferringPracticeNames() == null ? Set.of() : request.getReferringPracticeNames();
		Set<UUID> panelAccountIds = request.getPanelAccountIds() == null ? Set.of() : request.getPanelAccountIds();
		String patientMrn = trimToNull(request.getPatientMrn());
		String searchQuery = trimToNull(request.getSearchQuery());
		Integer pageNumber = request.getPageNumber();
		Integer pageSize = request.getPageSize();
		List<PatientOrderSortRule> patientOrderSortRules = request.getPatientOrderSortRules() == null ? List.of() : request.getPatientOrderSortRules();

		final int DEFAULT_PAGE_SIZE = 50;
		final int MAXIMUM_PAGE_SIZE = 100;

		if (pageNumber == null || pageNumber < 0)
			pageNumber = 0;

		if (pageSize == null || pageSize <= 0)
			pageSize = DEFAULT_PAGE_SIZE;
		else if (pageSize > MAXIMUM_PAGE_SIZE)
			pageSize = MAXIMUM_PAGE_SIZE;

		Integer offset = pageNumber * pageSize;
		Integer limit = pageSize;
		List<String> whereClauseLines = new ArrayList<>();
		List<String> orderByColumns = new ArrayList<>();
		List<Object> parameters = new ArrayList<>();

		// Only include complete/valid sort order rules.
		patientOrderSortRules = patientOrderSortRules.stream()
				.filter(patientOrderSortRule -> patientOrderSortRule.getPatientOrderSortColumnId() != null
						&& patientOrderSortRule.getSortDirectionId() != null
						&& patientOrderSortRule.getSortNullsId() != null)
				.collect(Collectors.toList());

		// If no rules available, pick a safe default
		if (patientOrderSortRules.size() == 0)
			patientOrderSortRules = List.of(
					new PatientOrderSortRule() {{
						setPatientOrderSortColumnId(PatientOrderSortColumnId.ORDER_DATE);
						setSortDirectionId(SortDirectionId.DESCENDING);
						setSortNullsId(SortNullsId.NULLS_LAST);
					}},
					new PatientOrderSortRule() {{
						setPatientOrderSortColumnId(PatientOrderSortColumnId.PATIENT_FIRST_NAME);
						setSortDirectionId(SortDirectionId.ASCENDING);
						setSortNullsId(SortNullsId.NULLS_LAST);
					}},
					new PatientOrderSortRule() {{
						setPatientOrderSortColumnId(PatientOrderSortColumnId.PATIENT_LAST_NAME);
						setSortDirectionId(SortDirectionId.ASCENDING);
						setSortNullsId(SortNullsId.NULLS_LAST);
					}}
			);

		parameters.add(institutionId);

		// Default to OPEN orders unless specified otherwise
		if (patientOrderDispositionId == null)
			patientOrderDispositionId = PatientOrderDispositionId.OPEN;

		whereClauseLines.add("AND po.patient_order_disposition_id=?");
		parameters.add(patientOrderDispositionId);

		if (patientOrderConsentStatusId != null) {
			whereClauseLines.add("AND po.patient_order_consent_status_id=?");
			parameters.add(patientOrderConsentStatusId);
		}

		if (patientOrderTriageStatusIds.size() > 0) {
			whereClauseLines.add(format("AND po.patient_order_triage_status_id IN %s", sqlInListPlaceholders(patientOrderTriageStatusIds)));
			parameters.addAll(patientOrderTriageStatusIds);
		}

		if (patientOrderAssignmentStatusId != null) {
			if (patientOrderAssignmentStatusId == PatientOrderAssignmentStatusId.UNASSIGNED)
				whereClauseLines.add("AND po.panel_account_id IS NULL");
			else if (patientOrderAssignmentStatusId == PatientOrderAssignmentStatusId.ASSIGNED)
				whereClauseLines.add("AND po.panel_account_id IS NOT NULL");
		}

		if (patientOrderOutreachStatusId != null) {
			if (patientOrderOutreachStatusId == PatientOrderOutreachStatusId.HAS_OUTREACH)
				whereClauseLines.add("AND po.total_outreach_count > 0");
			else if (patientOrderOutreachStatusId == PatientOrderOutreachStatusId.NO_OUTREACH)
				whereClauseLines.add("AND po.total_outreach_count = 0");
		}

		if (patientOrderResponseStatusId != null) {
//			if (patientOrderResponseStatusId == PatientOrderResponseStatusId.WAITING_FOR_RESPONSE)
//				whereClauseLines.add("TODO");
//			else if (patientOrderResponseStatusId == PatientOrderResponseStatusId.NOT_WAITING_FOR_RESPONSE)
//				whereClauseLines.add("TODO");
		}

		if (patientOrderSafetyPlanningStatusId != null) {
			whereClauseLines.add("AND po.patient_order_safety_planning_status_id=?");
			parameters.add(patientOrderSafetyPlanningStatusId);
		}

		if (patientOrderFilterFlagTypeIds.size() > 0) {
			List<String> filterFlagWhereClauseLines = new ArrayList<>();

			// Note: we are ignoring the NONE flag for now since no UI supports it atm
			if (patientOrderFilterFlagTypeIds.contains(PatientOrderFilterFlagTypeId.INSURANCE_NOT_ACCEPTED)) {
				filterFlagWhereClauseLines.add("po.patient_order_insurance_plan_accepted=FALSE");
			}

			if (patientOrderFilterFlagTypeIds.contains(PatientOrderFilterFlagTypeId.ADDRESS_REGION_NOT_ACCEPTED)) {
				filterFlagWhereClauseLines.add("po.patient_address_region_accepted=FALSE");
			}

			if (patientOrderFilterFlagTypeIds.contains(PatientOrderFilterFlagTypeId.MOST_RECENT_EPISODE_CLOSED_WITHIN_DATE_THRESHOLD)) {
				filterFlagWhereClauseLines.add("po.most_recent_episode_closed_within_date_threshold=TRUE");
			}

			if (patientOrderFilterFlagTypeIds.contains(PatientOrderFilterFlagTypeId.PATIENT_BELOW_AGE_THRESHOLD)) {
				filterFlagWhereClauseLines.add("po.patient_below_age_threshold=TRUE");
			}

			whereClauseLines.add(format("AND (%s)", filterFlagWhereClauseLines.stream().collect(Collectors.joining(" OR "))));
		}

		if (referringPracticeNames.size() > 0) {
			whereClauseLines.add(format("AND po.referring_practice_name IN %s", sqlInListPlaceholders(referringPracticeNames)));
			parameters.addAll(referringPracticeNames);
		}

		if (panelAccountIds.size() > 0) {
			whereClauseLines.add(format("AND po.panel_account_id IN %s", sqlInListPlaceholders(panelAccountIds)));
			parameters.addAll(panelAccountIds);
		}

		// Search query is trumped by Patient MRN
		if (patientMrn != null) {
			whereClauseLines.add("AND LOWER(po.patient_mrn)=LOWER(?)");
			parameters.add(patientMrn);
		} else if (searchQuery != null) {
			// TODO: this is quick and dirty so FE can build.  Need to significantly improve matching
			whereClauseLines.add("""
					      AND (
					      patient_first_name ILIKE CONCAT('%',?,'%')
					      OR patient_last_name ILIKE CONCAT('%',?,'%')
					      OR patient_mrn ILIKE CONCAT('%',?,'%')
					      OR (patient_phone_number IS NOT NULL AND patient_phone_number ILIKE CONCAT('%',?,'%'))
					      OR (patient_email_address IS NOT NULL AND patient_email_address ILIKE CONCAT('%',?,'%'))
					      )
					""");

			parameters.add(searchQuery);
			parameters.add(searchQuery);
			parameters.add(searchQuery);
			parameters.add(searchQuery);
			parameters.add(searchQuery);
		}

		// Apply ORDER BY rules
		for (PatientOrderSortRule patientOrderSortRule : patientOrderSortRules) {
			String sortDirection = patientOrderSortRule.getSortDirectionId() == SortDirectionId.ASCENDING ? "ASC" : "DESC";
			String nullsFirst = patientOrderSortRule.getSortNullsId() == SortNullsId.NULLS_FIRST ? "NULLS FIRST" : "NULLS LAST";
			String orderByColumn;

			if (patientOrderSortRule.getPatientOrderSortColumnId() == PatientOrderSortColumnId.ORDER_DATE)
				orderByColumn = "bq.order_date";
			else if (patientOrderSortRule.getPatientOrderSortColumnId() == PatientOrderSortColumnId.PATIENT_FIRST_NAME)
				orderByColumn = "bq.patient_first_name";
			else if (patientOrderSortRule.getPatientOrderSortColumnId() == PatientOrderSortColumnId.PATIENT_LAST_NAME)
				orderByColumn = "bq.patient_last_name";
			else if (patientOrderSortRule.getPatientOrderSortColumnId() == PatientOrderSortColumnId.MOST_RECENT_SCREENING_SESSION_COMPLETED_AT)
				orderByColumn = "bq.most_recent_screening_session_completed_at";
			else if (patientOrderSortRule.getPatientOrderSortColumnId() == PatientOrderSortColumnId.MOST_RECENT_OUTREACH_DATE_TIME)
				orderByColumn = "bq.most_recent_outreach_date_time";
			else if (patientOrderSortRule.getPatientOrderSortColumnId() == PatientOrderSortColumnId.MOST_RECENT_SCHEDULED_SCREENING_SCHEDULED_DATE_TIME)
				orderByColumn = "bq.patient_order_scheduled_screening_scheduled_date_time";
			else if (patientOrderSortRule.getPatientOrderSortColumnId() == PatientOrderSortColumnId.EPISODE_CLOSED_AT)
				orderByColumn = "bq.episode_closed_at";
			else
				throw new IllegalStateException(format("Not sure what to do with %s.%s", PatientOrderSortColumnId.class.getSimpleName(), patientOrderSortRule.getPatientOrderSortColumnId().name()));

			orderByColumns.add(format("%s %s %s", orderByColumn, sortDirection, nullsFirst));
		}

		parameters.add(limit);
		parameters.add(offset);

		String sql = """
				  WITH base_query AS (
				  SELECT po.*
				  FROM v_patient_order po
				  WHERE po.institution_id=?
				  {{whereClauseLines}}
				  ),
				  total_count_query AS (
				  SELECT COUNT(bq.*) AS total_count
				  FROM base_query bq
				  )
				  SELECT
				  bq.*,
				  tcq.total_count
				  FROM
				  total_count_query tcq,
				  base_query bq
				  ORDER BY {{orderByColumns}}
				  LIMIT ?
				  OFFSET ?
				""".trim()
				.replace("{{whereClauseLines}}", whereClauseLines.stream().collect(Collectors.joining("\n")))
				.replace("{{orderByColumns}}", orderByColumns.stream().collect(Collectors.joining(", ")))
				.trim();

		List<PatientOrderWithTotalCount> patientOrders = getDatabase().queryForList(sql, PatientOrderWithTotalCount.class, sqlVaragsParameters(parameters));

		Integer totalCount = patientOrders.stream()
				.filter(patientOrder -> patientOrder.getTotalCount() != null)
				.mapToInt(PatientOrderWithTotalCount::getTotalCount)
				.findFirst()
				.orElse(0);

		FindResult<? extends PatientOrder> findResult = new FindResult<>(patientOrders, totalCount);
		return (FindResult<PatientOrder>) findResult;
	}

	@Nonnull
	public Optional<PatientOrderAutocompleteResult> findPatientOrderAutocompleteResultByMrn(@Nullable String patientMrn,
																																													@Nullable InstitutionId institutionId) {
		patientMrn = trimToNull(patientMrn);

		if (patientMrn == null || institutionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM patient_order
				WHERE institution_id=?
				AND patient_mrn=?
				LIMIT 1
				""", PatientOrderAutocompleteResult.class, institutionId, patientMrn);
	}

	@Nonnull
	public List<PatientOrder> findTodayPatientOrdersForPanelAccountId(@Nullable UUID panelAccountId) {
		if (panelAccountId == null)
			return List.of();

		return getDatabase().queryForList("""
					SELECT po.*
				  FROM v_patient_order po
				  WHERE po.panel_account_id=?
				  AND po.patient_order_disposition_id=?
				  ORDER BY po.order_date DESC, po.order_age_in_minutes, po.patient_first_name, po.patient_last_name
				""", PatientOrder.class, panelAccountId, PatientOrderDispositionId.OPEN);
	}

	@Nonnull
	public List<PatientOrderAutocompleteResult> findPatientOrderAutocompleteResults(@Nullable String searchQuery,
																																									@Nullable InstitutionId institutionId) {
		searchQuery = trimToNull(searchQuery);

		if (searchQuery == null || institutionId == null)
			return List.of();

		// For phone numbers, remove anything that's not a digit
		String searchQueryPhoneNumber = searchQuery.replaceAll("[^0-9]", "");

		if (searchQueryPhoneNumber.length() == 0)
			searchQueryPhoneNumber = "invalid";

		// TODO: this is quick and dirty so FE can build.  Need to significantly improve matching

		return getDatabase().queryForList("""
						SELECT *
						FROM patient_order
						WHERE institution_id=?
						AND (
						patient_first_name ILIKE CONCAT('%',?,'%')
						OR patient_last_name ILIKE CONCAT('%',?,'%')
						OR patient_mrn ILIKE CONCAT('%',?,'%')
						OR (patient_phone_number IS NOT NULL AND patient_phone_number ILIKE CONCAT('%',?,'%'))
						OR (patient_email_address IS NOT NULL AND patient_email_address ILIKE CONCAT('%',?,'%'))
						)
						ORDER BY patient_last_name, patient_first_name
						LIMIT 10
						""", PatientOrderAutocompleteResult.class, institutionId,
				searchQuery, searchQuery, searchQuery, searchQueryPhoneNumber, searchQuery);
	}

	@Nonnull
	public List<PatientOrderDisposition> findPatientOrderDispositions() {
		return getDatabase().queryForList("""
				SELECT *
				FROM patient_order_disposition
				ORDER BY display_order
				""", PatientOrderDisposition.class);
	}

	@Nonnull
	public List<PatientOrderFocusType> findPatientOrderFocusTypes() {
		return getDatabase().queryForList("""
				SELECT *
				FROM patient_order_focus_type
				ORDER BY description
				""", PatientOrderFocusType.class);
	}

	@Nonnull
	public List<PatientOrderCareType> findPatientOrderCareTypes() {
		return getDatabase().queryForList("""
				SELECT *
				FROM patient_order_care_type
				ORDER BY description
				""", PatientOrderCareType.class);
	}

	@Nonnull
	public List<PatientOrderTriage> findPatientOrderTriagesByPatientOrderId(@Nullable UUID patientOrderId) {
		return findPatientOrderTriagesByPatientOrderId(patientOrderId, null);
	}

	@Nonnull
	public List<PatientOrderTriage> findPatientOrderTriagesByPatientOrderId(@Nullable UUID patientOrderId,
																																					@Nullable UUID screeningSessionId) {
		if (patientOrderId == null)
			return List.of();

		List<Object> parameters = new ArrayList<>();

		String sql = """
				SELECT *
				FROM patient_order_triage
				WHERE patient_order_id=?
				""";

		parameters.add(patientOrderId);

		// If a screening session is specified, pull triages regardless of whether they're still active.
		// If not specified, only pull active triages
		if (screeningSessionId != null) {
			sql += " AND screening_session_id=?";
			parameters.add(screeningSessionId);
		} else {
			sql += " AND active=TRUE";
		}

		sql += " ORDER BY display_order";

		return getDatabase().queryForList(sql, PatientOrderTriage.class, parameters.toArray(new Object[]{}));
	}

	@Nonnull
	public Boolean openPatientOrder(@Nonnull OpenPatientOrderRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		UUID patientOrderId = request.getPatientOrderId();
		PatientOrder patientOrder = null;
		ValidationException validationException = new ValidationException();

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (patientOrderId == null) {
			validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is required.")));
		} else {
			patientOrder = findPatientOrderById(patientOrderId).orElse(null);

			if (patientOrder == null)
				validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		// If we're already open, nothing to do
		if (patientOrder.getPatientOrderDispositionId() == PatientOrderDispositionId.OPEN)
			return false;

		PatientOrder otherAlreadyOpenPatientOrder = findOpenPatientOrderByMrnAndInstitutionId(patientOrder.getPatientMrn(), patientOrder.getInstitutionId()).orElse(null);

		if (otherAlreadyOpenPatientOrder != null)
			throw new ValidationException(getStrings().get("Order ID {{orderId}} is already open for patient {{firstName}} {{lastName}} with MRN {{mrn}}. You must close that order before opening this one.",
					Map.of(
							"orderId", otherAlreadyOpenPatientOrder.getOrderId(),
							"firstName", otherAlreadyOpenPatientOrder.getPatientFirstName(),
							"lastName", otherAlreadyOpenPatientOrder.getPatientLastName(),
							"mrn", otherAlreadyOpenPatientOrder.getPatientMrn())));

		getDatabase().execute("""
				UPDATE patient_order
				SET patient_order_disposition_id=?, patient_order_closure_reason_id=?,
				episode_closed_at=NULL, episode_closed_by_account_id=NULL
				WHERE patient_order_id=?
				""", PatientOrderDispositionId.OPEN, PatientOrderClosureReasonId.NOT_CLOSED, patientOrderId);

		createPatientOrderEvent(new CreatePatientOrderEventRequest() {
			{
				setPatientOrderEventTypeId(PatientOrderEventTypeId.STATUS_CHANGED);
				setPatientOrderId(patientOrderId);
				setAccountId(accountId);
				setMessage("Order opened."); // Not localized on the way in
				setMetadata(Map.of());
			}
		});

		return true;
	}

	@Nonnull
	public Boolean closePatientOrder(@Nonnull ClosePatientOrderRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		UUID patientOrderId = request.getPatientOrderId();
		PatientOrderClosureReasonId patientOrderClosureReasonId = request.getPatientOrderClosureReasonId();
		PatientOrder patientOrder = null;
		ValidationException validationException = new ValidationException();

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (patientOrderId == null) {
			validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is required.")));
		} else {
			patientOrder = findPatientOrderById(patientOrderId).orElse(null);

			if (patientOrder == null)
				validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is invalid.")));
		}

		if (patientOrderClosureReasonId == null) {
			validationException.add(new FieldError("patientOrderClosureReasonId", getStrings().get("Patient Order Closure Reason ID is required.")));
		} else if (patientOrderClosureReasonId == PatientOrderClosureReasonId.NOT_CLOSED) {
			// Illegal to say "I'm closing with NOT_CLOSED reason"
			validationException.add(new FieldError("patientOrderClosureReasonId", getStrings().get("Patient Order Closure Reason ID is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		// If we're already closed, nothing to do
		if (patientOrder.getPatientOrderDispositionId() == PatientOrderDispositionId.CLOSED)
			return false;

		getDatabase().execute("""
				UPDATE patient_order
				SET patient_order_disposition_id=?, patient_order_closure_reason_id=?,
				episode_closed_at=NOW(), episode_closed_by_account_id=?
				WHERE patient_order_id=?
				""", PatientOrderDispositionId.CLOSED, patientOrderClosureReasonId, accountId, patientOrderId);

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("patientOrderClosureReasonId", patientOrderClosureReasonId);

		createPatientOrderEvent(new CreatePatientOrderEventRequest() {
			{
				setPatientOrderEventTypeId(PatientOrderEventTypeId.STATUS_CHANGED);
				setPatientOrderId(patientOrderId);
				setAccountId(accountId);
				setMessage("Order closed."); // Not localized on the way in
				setMetadata(metadata);
			}
		});

		return true;
	}

	@Nonnull
	public Boolean updatePatientOrderConsentStatus(@Nonnull UpdatePatientOrderConsentStatusRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		UUID patientOrderId = request.getPatientOrderId();
		PatientOrderConsentStatusId patientOrderConsentStatusId = request.getPatientOrderConsentStatusId();
		PatientOrder patientOrder = null;
		ValidationException validationException = new ValidationException();

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (patientOrderId == null) {
			validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is required.")));
		} else {
			patientOrder = findPatientOrderById(patientOrderId).orElse(null);

			if (patientOrder == null)
				validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is invalid.")));
		}

		if (patientOrderConsentStatusId == null)
			validationException.add(new FieldError("patientOrderConsentStatusId", getStrings().get("Patient Order Consent Status ID is required.")));
		else if (patientOrderConsentStatusId != PatientOrderConsentStatusId.CONSENTED
				&& patientOrderConsentStatusId != PatientOrderConsentStatusId.REJECTED)
			validationException.add(new FieldError("patientOrderConsentStatusId", getStrings().get("Patient Order Consent Status ID is invalid.")));

		if (validationException.hasErrors())
			throw validationException;

		// If we're not actually changing the status, nothing to do
		if (patientOrder.getPatientOrderConsentStatusId() == patientOrderConsentStatusId)
			return false;

		getDatabase().execute("""
				UPDATE patient_order
				SET patient_order_consent_status_id=?, consent_status_updated_at=NOW(), consent_status_updated_by_account_id=?
				WHERE patient_order_id=?
				""", patientOrderConsentStatusId, accountId, patientOrderId);

		// Side effect of rejection: immediately close out the order
		if (patientOrderConsentStatusId == PatientOrderConsentStatusId.REJECTED) {
			getLogger().info("Patient Order ID had its consent rejected, so closing it out...", patientOrderId);

			closePatientOrder(new ClosePatientOrderRequest() {{
				setPatientOrderId(patientOrderId);
				setPatientOrderClosureReasonId(PatientOrderClosureReasonId.REFUSED_CARE);
				setAccountId(accountId);
			}});
		}

		// TODO: track event

		return true;
	}

	@Nonnull
	public List<UUID> updatePatientOrderTriages(@Nonnull UpdatePatientOrderTriagesRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		UUID patientOrderId = request.getPatientOrderId();
		PatientOrderTriageSourceId patientOrderTriageSourceId = request.getPatientOrderTriageSourceId();
		UUID screeningSessionId = request.getScreeningSessionId();
		List<CreatePatientOrderTriageRequest> patientOrderTriages = request.getPatientOrderTriages() == null
				? List.of() : request.getPatientOrderTriages();
		Account account = null;
		PatientOrder patientOrder = null;
		List<UUID> patientOrderTriageIds = new ArrayList<>();
		ValidationException validationException = new ValidationException();

		if (accountId == null) {
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));
		} else {
			account = getAccountService().findAccountById(accountId).orElse(null);

			if (account == null)
				validationException.add(new FieldError("accountId", getStrings().get("Account ID is invalid.")));
		}

		if (patientOrderId == null) {
			validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is required.")));
		} else {
			patientOrder = findPatientOrderById(patientOrderId).orElse(null);

			if (patientOrder == null)
				validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is invalid.")));
		}

		if (patientOrderTriageSourceId == null) {
			validationException.add(new FieldError("patientOrderTriageSourceId", getStrings().get("Patient Order Triage Source ID is required.")));
		} else {
			if (patientOrderTriageSourceId == PatientOrderTriageSourceId.COBALT && screeningSessionId == null)
				validationException.add(new FieldError("screeningSessionId", getStrings().get("Screening session ID is required.")));
		}

		if (patientOrderTriages.size() == 0) {
			validationException.add(new FieldError("patientOrderTriages", getStrings().get("You must specify at least one triage.")));
		} else {
			for (int i = 0; i < patientOrderTriages.size(); ++i) {
				CreatePatientOrderTriageRequest patientOrderTriage = patientOrderTriages.get(i);

				if (patientOrderTriage == null) {
					validationException.add(new FieldError(format("patientOrderTriage[%d]", i), getStrings().get("Patient Order Triage is required.")));
				} else {
					if (patientOrderTriage.getPatientOrderCareTypeId() == null)
						validationException.add(new FieldError(format("patientOrderTriage[%d].patientOrderCareTypeId", i),
								getStrings().get("Patient Order Care Type ID is required.")));

					if (patientOrderTriage.getPatientOrderFocusTypeId() == null)
						validationException.add(new FieldError(format("patientOrderTriage[%d].patientOrderFocusTypeId", i),
								getStrings().get("Patient Order Focus Type ID is required.")));
				}
			}
		}

		if (validationException.hasErrors())
			throw validationException;

		if (!Objects.equals(patientOrder.getInstitutionId(), account.getInstitutionId()))
			throw new IllegalStateException(format("Unexpected institution mismatch between patient order ID %s and account ID %s",
					patientOrder.getPatientOrderId(), account.getAccountId()));

		getDatabase().execute("""
				UPDATE patient_order_triage
				SET active=FALSE
				WHERE patient_order_id=?
				""", patientOrder.getPatientOrderId());

		int displayOrder = 0;

		for (CreatePatientOrderTriageRequest patientOrderTriage : patientOrderTriages) {
			UUID patientOrderTriageId = UUID.randomUUID();
			String reason = trimToNull(patientOrderTriage.getReason());
			++displayOrder;

			getDatabase().execute("""
							INSERT INTO patient_order_triage (
							     patient_order_triage_id,
							     patient_order_id,
							     patient_order_triage_source_id,
							     patient_order_care_type_id,
							     patient_order_focus_type_id,
							     screening_session_id,
							     account_id,
							     reason,
							     active,
							     display_order
							) VALUES (?,?,?,?,?,?,?,?,?,?)
							""", patientOrderTriageId, patientOrderId, patientOrderTriageSourceId,
					patientOrderTriage.getPatientOrderCareTypeId(), patientOrderTriage.getPatientOrderFocusTypeId(),
					screeningSessionId, accountId, reason, true, displayOrder);

			patientOrderTriageIds.add(patientOrderTriageId);
		}

		// TODO: track events

		return patientOrderTriageIds;
	}

	@Nonnull
	public Boolean resetPatientOrderTriages(@Nonnull UUID patientOrderId,
																					@Nonnull UUID accountId) {
		requireNonNull(patientOrderId);
		requireNonNull(accountId);

		// "Resettable" means inactive triages that were initially sourced by Cobalt, e.g.
		// via automated analysis of screening session answers
		List<PatientOrderTriage> resettablePatientOrderTriages = getDatabase().queryForList("""
				SELECT *
				FROM patient_order_triage
				WHERE patient_order_id=?
				AND patient_order_triage_source_id=?
				AND active=FALSE
				""", PatientOrderTriage.class, patientOrderId, PatientOrderTriageSourceId.COBALT);

		if (resettablePatientOrderTriages.size() == 0)
			return false;

		// Disable all triages for this order
		getDatabase().execute("""
				UPDATE patient_order_triage
				SET active=FALSE
				WHERE patient_order_id=?
				""", patientOrderId);

		// Enable just the original triages
		boolean updated = getDatabase().execute("""
				UPDATE patient_order_triage
				SET active=TRUE
				WHERE patient_order_id=?
				AND patient_order_triage_source_id=?
				""", patientOrderId, PatientOrderTriageSourceId.COBALT) > 0;

		// TODO: track events

		return updated;
	}

	@Nonnull
	public UUID createPatientOrderNote(@Nonnull CreatePatientOrderNoteRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		UUID patientOrderId = request.getPatientOrderId();
		String note = trimToNull(request.getNote());
		PatientOrder patientOrder;
		UUID patientOrderNoteId = UUID.randomUUID();
		ValidationException validationException = new ValidationException();

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (patientOrderId == null) {
			validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is required.")));
		} else {
			patientOrder = findPatientOrderById(patientOrderId).orElse(null);

			if (patientOrder == null)
				validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is invalid.")));
		}

		if (note == null)
			validationException.add(new FieldError("note", getStrings().get("Comment cannot be blank.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				INSERT INTO patient_order_note (
				patient_order_note_id, patient_order_id, account_id, note
				) VALUES (?,?,?,?)
				""", patientOrderNoteId, patientOrderId, accountId, note);

		createPatientOrderEvent(new CreatePatientOrderEventRequest() {{
			setPatientOrderEventTypeId(PatientOrderEventTypeId.NOTE_CREATED);
			setPatientOrderId(patientOrderId);
			setAccountId(accountId);
			setMessage("Created note."); // Not localized on the way in
			setMetadata(Map.of(
					"patientOrderNoteId", patientOrderNoteId,
					"accountId", accountId,
					"note", note));
		}});

		return patientOrderNoteId;
	}

	@Nonnull
	public Boolean updatePatientOrderNote(@Nonnull UpdatePatientOrderNoteRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		UUID patientOrderNoteId = request.getPatientOrderNoteId();
		String note = trimToNull(request.getNote());
		PatientOrderNote patientOrderNote = null;
		ValidationException validationException = new ValidationException();

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (patientOrderNoteId == null) {
			validationException.add(new FieldError("patientOrderNoteId", getStrings().get("Patient Order Note ID is required.")));
		} else {
			patientOrderNote = findPatientOrderNoteById(patientOrderNoteId).orElse(null);

			if (patientOrderNote == null)
				validationException.add(new FieldError("patientOrderNoteId", getStrings().get("Patient Order Note ID is invalid.")));
		}

		if (note == null)
			validationException.add(new FieldError("note", getStrings().get("Comment cannot be blank.")));

		if (validationException.hasErrors())
			throw validationException;

		boolean updated = getDatabase().execute("""
				UPDATE patient_order_note
				SET note=?
				WHERE patient_order_note_id=?
				""", note, patientOrderNoteId) > 0;

		if (updated) {
			UUID patientOrderId = patientOrderNote.getPatientOrderId();
			String oldNote = patientOrderNote.getNote();

			createPatientOrderEvent(new CreatePatientOrderEventRequest() {{
				setPatientOrderEventTypeId(PatientOrderEventTypeId.NOTE_UPDATED);
				setPatientOrderId(patientOrderId);
				setAccountId(accountId);
				setMessage("Updated note."); // Not localized on the way in
				setMetadata(Map.of(
						"patientOrderNoteId", patientOrderNoteId,
						"accountId", accountId,
						"oldNote", oldNote,
						"newNote", note));
			}});
		}

		return updated;
	}

	@Nonnull
	public Boolean deletePatientOrderNote(@Nonnull DeletePatientOrderNoteRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		UUID patientOrderNoteId = request.getPatientOrderNoteId();
		PatientOrderNote patientOrderNote = null;
		ValidationException validationException = new ValidationException();

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (patientOrderNoteId == null) {
			validationException.add(new FieldError("patientOrderNoteId", getStrings().get("Patient Order Note ID is required.")));
		} else {
			patientOrderNote = findPatientOrderNoteById(patientOrderNoteId).orElse(null);

			if (patientOrderNote == null)
				validationException.add(new FieldError("patientOrderNoteId", getStrings().get("Patient Order Note ID is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		boolean deleted = getDatabase().execute("""
				UPDATE patient_order_note
				SET deleted=TRUE
				WHERE patient_order_note_id=?
				""", patientOrderNoteId) > 0;

		if (deleted) {
			UUID patientOrderId = patientOrderNote.getPatientOrderId();
			String note = patientOrderNote.getNote();

			createPatientOrderEvent(new CreatePatientOrderEventRequest() {{
				setPatientOrderEventTypeId(PatientOrderEventTypeId.NOTE_DELETED);
				setPatientOrderId(patientOrderId);
				setAccountId(accountId);
				setMessage("Deleted note."); // Not localized on the way in
				setMetadata(Map.of(
						"patientOrderNoteId", patientOrderNoteId,
						"accountId", accountId,
						"note", note));
			}});
		}

		return deleted;
	}

	@Nonnull
	public List<PatientOrderOutreachResult> findPatientOrderOutreachResults() {
		return getDatabase().queryForList("""
				SELECT *
				FROM v_patient_order_outreach_result
				ORDER BY display_order
				""", PatientOrderOutreachResult.class);
	}

	@Nonnull
	public UUID createPatientOrderOutreach(@Nonnull CreatePatientOrderOutreachRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		UUID patientOrderId = request.getPatientOrderId();
		UUID patientOrderOutreachResultId = request.getPatientOrderOutreachResultId();
		String note = trimToNull(request.getNote());
		LocalDate outreachDate = request.getOutreachDate();
		String outreachTimeAsString = trimToNull(request.getOutreachTime());
		PatientOrder patientOrder;
		LocalTime outreachTime = null;
		UUID patientOrderOutreachId = UUID.randomUUID();
		ValidationException validationException = new ValidationException();

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (patientOrderId == null) {
			validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is required.")));
		} else {
			patientOrder = findPatientOrderById(patientOrderId).orElse(null);

			if (patientOrder == null)
				validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is invalid.")));
		}

		if (patientOrderOutreachResultId == null)
			validationException.add(new FieldError("patientOrderOutreachResultId", getStrings().get("Patient Order Outreach Result ID is required.")));

		if (outreachDate == null)
			validationException.add(new FieldError("outreachDate", getStrings().get("Outreach date is required.")));

		if (outreachTimeAsString == null) {
			validationException.add(new FieldError("outreachTime", getStrings().get("Outreach time is required.")));
		} else {
			// TODO: support other locales
			outreachTime = getNormalizer().normalizeTime(outreachTimeAsString, Locale.US).orElse(null);

			if (outreachTime == null)
				validationException.add(new FieldError("outreachTime", getStrings().get("Outreach time is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		LocalDateTime outreachDateTime = LocalDateTime.of(outreachDate, outreachTime);

		getDatabase().execute("""
				INSERT INTO patient_order_outreach (
				patient_order_outreach_id, patient_order_id, account_id, patient_order_outreach_result_id, note, outreach_date_time
				) VALUES (?,?,?,?,?,?)
				""", patientOrderOutreachId, patientOrderId, accountId, patientOrderOutreachResultId, note, outreachDateTime);

		createPatientOrderEvent(new CreatePatientOrderEventRequest() {{
			setPatientOrderEventTypeId(PatientOrderEventTypeId.OUTREACH_CREATED);
			setPatientOrderId(patientOrderId);
			setAccountId(accountId);
			setMessage("Created outreach."); // Not localized on the way in
			setMetadata(Map.of(
					"patientOrderOutreachId", patientOrderOutreachId
			));
		}});

		return patientOrderOutreachId;
	}

	@Nonnull
	public Boolean updatePatientOrderOutreach(@Nonnull UpdatePatientOrderOutreachRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		UUID patientOrderOutreachId = request.getPatientOrderOutreachId();
		UUID patientOrderOutreachResultId = request.getPatientOrderOutreachResultId();
		String note = trimToNull(request.getNote());
		LocalDate outreachDate = request.getOutreachDate();
		String outreachTimeAsString = trimToNull(request.getOutreachTime());
		PatientOrderOutreach patientOrderOutreach = null;
		LocalTime outreachTime = null;
		ValidationException validationException = new ValidationException();

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (patientOrderOutreachId == null) {
			validationException.add(new FieldError("patientOrderOutreachId", getStrings().get("Patient Order Outreach ID is required.")));
		} else {
			patientOrderOutreach = findPatientOrderOutreachById(patientOrderOutreachId).orElse(null);

			if (patientOrderOutreach == null)
				validationException.add(new FieldError("patientOrderOutreachId", getStrings().get("Patient Order Outreach ID is invalid.")));
		}

		if (patientOrderOutreachResultId == null)
			validationException.add(new FieldError("patientOrderOutreachResultId", getStrings().get("Patient Order Outreach Result ID is required.")));

		if (outreachDate == null)
			validationException.add(new FieldError("outreachDate", getStrings().get("Outreach date is required.")));

		if (outreachTimeAsString == null) {
			validationException.add(new FieldError("outreachTime", getStrings().get("Outreach time is required.")));
		} else {
			// TODO: support other locales
			outreachTime = getNormalizer().normalizeTime(outreachTimeAsString, Locale.US).orElse(null);

			if (outreachTime == null)
				validationException.add(new FieldError("outreachTime", getStrings().get("Outreach time is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		LocalDateTime outreachDateTime = LocalDateTime.of(outreachDate, outreachTime);

		boolean updated = getDatabase().execute("""
				UPDATE patient_order_outreach
				SET patient_order_outreach_result_id=?, note=?, outreach_date_time=?
				WHERE patient_order_outreach_id=?
				""", patientOrderOutreachResultId, note, outreachDateTime, patientOrderOutreachId) > 0;

		if (updated) {
			UUID patientOrderId = patientOrderOutreach.getPatientOrderId();
			UUID oldPatientOrderOutreachResultId = patientOrderOutreach.getPatientOrderOutreachResultId();
			String oldNote = patientOrderOutreach.getNote();
			LocalDateTime oldOutreachDateTime = patientOrderOutreach.getOutreachDateTime();

			Map<String, Object> metadata = new HashMap<>();
			metadata.put("patientOrderOutreachId", patientOrderOutreachId);
			metadata.put("accountId", accountId);
			metadata.put("oldPatientOrderOutreachResultId", oldPatientOrderOutreachResultId);
			metadata.put("newPatientOrderOutreachResultId", patientOrderOutreachResultId);
			metadata.put("oldNote", oldNote);
			metadata.put("newNote", note);
			metadata.put("oldOutreachDateTime", oldOutreachDateTime);
			metadata.put("newOutreachDateTime", outreachDateTime);

			createPatientOrderEvent(new CreatePatientOrderEventRequest() {{
				setPatientOrderEventTypeId(PatientOrderEventTypeId.OUTREACH_UPDATED);
				setPatientOrderId(patientOrderId);
				setAccountId(accountId);
				setMessage("Updated outreach."); // Not localized on the way in
				setMetadata(metadata);
			}});
		}

		return updated;
	}

	@Nonnull
	public Boolean deletePatientOrderOutreach(@Nonnull DeletePatientOrderOutreachRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		UUID patientOrderOutreachId = request.getPatientOrderOutreachId();
		PatientOrderOutreach patientOrderOutreach = null;
		ValidationException validationException = new ValidationException();

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (patientOrderOutreachId == null) {
			validationException.add(new FieldError("patientOrderOutreachId", getStrings().get("Patient Order Outreach ID is required.")));
		} else {
			patientOrderOutreach = findPatientOrderOutreachById(patientOrderOutreachId).orElse(null);

			if (patientOrderOutreach == null)
				validationException.add(new FieldError("patientOrderOutreachId", getStrings().get("Patient Order Outreach ID is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		boolean deleted = getDatabase().execute("""
				UPDATE patient_order_outreach
				SET deleted=TRUE
				WHERE patient_order_outreach_id=?
				""", patientOrderOutreachId) > 0;

		if (deleted) {
			UUID patientOrderId = patientOrderOutreach.getPatientOrderId();
			String note = patientOrderOutreach.getNote();
			LocalDateTime outreachDateTime = patientOrderOutreach.getOutreachDateTime();

			createPatientOrderEvent(new CreatePatientOrderEventRequest() {{
				setPatientOrderEventTypeId(PatientOrderEventTypeId.NOTE_DELETED);
				setPatientOrderId(patientOrderId);
				setAccountId(accountId);
				setMessage("Deleted outreach."); // Not localized on the way in
				setMetadata(Map.of(
						"patientOrderOutreachId", patientOrderOutreachId,
						"accountId", accountId,
						"note", note,
						"outreachDateTime", outreachDateTime));
			}});
		}

		return deleted;
	}

	@Nonnull
	public Set<UUID> associatePatientAccountWithPatientOrders(@Nullable UUID patientAccountId) {
		if (patientAccountId == null)
			return Set.of();

		Account patientAccount = getAccountService().findAccountById(patientAccountId).orElse(null);

		if (patientAccount == null)
			return Set.of();

		List<PatientOrder> unassignedMatchingPatientOrders = getDatabase().queryForList("""
				SELECT * 
				FROM patient_order
				WHERE patient_id=?
				AND patient_id_type=?				
				AND institution_id=?
				AND patient_account_id IS NULL
				""", PatientOrder.class, patientAccount.getEpicPatientId(), patientAccount.getEpicPatientIdType(), patientAccount.getInstitutionId());

		for (PatientOrder unassignedMatchingPatientOrder : unassignedMatchingPatientOrders) {
			getLogger().info("Assigning patient account ID {} to patient order ID {}...", patientAccountId, unassignedMatchingPatientOrder.getPatientOrderId());

			getDatabase().execute("""
					UPDATE patient_order
					SET patient_account_id=?
					WHERE patient_order_id=?
					""", patientAccountId, unassignedMatchingPatientOrder.getPatientOrderId());

			createPatientOrderEvent(new CreatePatientOrderEventRequest() {{
				setPatientOrderEventTypeId(PatientOrderEventTypeId.PATIENT_ACCOUNT_ASSIGNED);
				setPatientOrderId(unassignedMatchingPatientOrder.getPatientOrderId());
				setAccountId(patientAccountId);
				setMessage("Assigned patient account to order."); // Not localized on the way in
				setMetadata(Map.of("patientAccountId", patientAccountId));
			}});
		}

		return unassignedMatchingPatientOrders.stream()
				.map(patientOrder -> patientOrder.getPatientOrderId())
				.collect(Collectors.toSet());
	}

	@Nonnull
	public Boolean updatePatientOrderSafetyPlanningStatus(@Nonnull UpdatePatientOrderSafetyPlanningStatusRequest request) {
		UUID accountId = request.getAccountId();
		UUID patientOrderId = request.getPatientOrderId();
		PatientOrderSafetyPlanningStatusId patientOrderSafetyPlanningStatusId = request.getPatientOrderSafetyPlanningStatusId();
		PatientOrder patientOrder = null;
		ValidationException validationException = new ValidationException();

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (patientOrderId == null) {
			validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is required.")));
		} else {
			patientOrder = findPatientOrderById(patientOrderId).orElse(null);

			if (patientOrder == null)
				validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is invalid.")));
		}

		if (patientOrderSafetyPlanningStatusId == null)
			validationException.add(new FieldError("patientOrderSafetyPlanningStatusId", getStrings().get("Patient Order Safety Planning Status ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		// Nothing to do if we're already in the requested state
		if (patientOrder.getPatientOrderSafetyPlanningStatusId() == patientOrderSafetyPlanningStatusId)
			return false;

		// TODO: track changes in event history table

		Instant connectedToSafetyPlanningAt = patientOrderSafetyPlanningStatusId == PatientOrderSafetyPlanningStatusId.CONNECTED_TO_SAFETY_PLANNING
				? Instant.now() : null;

		boolean updated = getDatabase().execute("""
				UPDATE patient_order
				SET patient_order_safety_planning_status_id=?, connected_to_safety_planning_at=?
				WHERE patient_order_id=?
				""", patientOrderSafetyPlanningStatusId, connectedToSafetyPlanningAt, patientOrderId) > 0;

		return updated;
	}

	@Nonnull
	public Boolean updatePatientOrderResourcingStatus(@Nonnull UpdatePatientOrderResourcingStatusRequest request) {
		UUID accountId = request.getAccountId();
		UUID patientOrderId = request.getPatientOrderId();
		PatientOrderResourcingStatusId patientOrderResourcingStatusId = request.getPatientOrderResourcingStatusId();
		PatientOrderResourcingTypeId patientOrderResourcingTypeId = request.getPatientOrderResourcingTypeId();
		LocalDate resourcesSentAtDate = request.getResourcesSentAtDate();
		String resourcesSentAtTimeAsString = request.getResourcesSentAtTime();
		String resourcesSentNote = trimToNull(request.getResourcesSentNote());
		LocalTime resourcesSentAtTime = null;
		PatientOrder patientOrder = null;
		Account account = null;
		ValidationException validationException = new ValidationException();

		if (accountId == null) {
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));
		} else {
			account = getAccountService().findAccountById(accountId).orElse(null);

			if (account == null)
				validationException.add(new FieldError("accountId", getStrings().get("Account ID is invalid.")));
		}

		if (patientOrderId == null) {
			validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is required.")));
		} else {
			patientOrder = findPatientOrderById(patientOrderId).orElse(null);

			if (patientOrder == null)
				validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is invalid.")));
		}

		if (patientOrderResourcingStatusId == null)
			validationException.add(new FieldError("patientOrderResourcingStatusId", getStrings().get("Patient Order Resourcing Status ID is required.")));

		if (resourcesSentAtTimeAsString != null) {
			// TODO: support other locales
			resourcesSentAtTime = getNormalizer().normalizeTime(resourcesSentAtTimeAsString, Locale.US).orElse(null);

			if (resourcesSentAtTime == null)
				validationException.add(new FieldError("resourcesSentAtTime", getStrings().get("Resources Sent At Time is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		// Nothing to do if we're already in the requested state
		if (patientOrder.getPatientOrderResourcingStatusId() == patientOrderResourcingStatusId)
			return false;

		Instant resourcesSentAt = null;

		if (patientOrderResourcingStatusId == PatientOrderResourcingStatusId.SENT_RESOURCES) {
			if (patientOrderResourcingTypeId == null || patientOrderResourcingTypeId == PatientOrderResourcingTypeId.NONE)
				throw new ValidationException(new FieldError("patientOrderResourcingTypeId", getStrings().get("You must specify how the resources were sent.")));

			// If provided a sent-at date/time, use them.
			// Otherwise, assuming "now"
			if (resourcesSentAtDate != null && resourcesSentAtTime != null) {
				LocalDateTime resourcesSentAtDateTime = LocalDateTime.of(resourcesSentAtDate, resourcesSentAtTime);
				resourcesSentAt = resourcesSentAtDateTime.atZone(account.getTimeZone()).toInstant();
			} else {
				resourcesSentAt = Instant.now();
			}

			// TODO: Schedule a message to be sent to the patient regarding these resources

		} else {
			resourcesSentAt = null;
			patientOrderResourcingTypeId = PatientOrderResourcingTypeId.NONE;

			// TODO: Cancel scheduled check-in messages to this patient for this order
		}

		// TODO: track changes in event history table

		return getDatabase().execute("""
				UPDATE patient_order
				SET patient_order_resourcing_status_id=?, resources_sent_at=?, resources_sent_note=?, patient_order_resourcing_type_id=?
				WHERE patient_order_id=?
				""", patientOrderResourcingStatusId, resourcesSentAt, resourcesSentNote, patientOrderResourcingTypeId, patientOrderId) > 0;
	}

	@Nonnull
	public Boolean archivePatientOrder(@Nonnull ArchivePatientOrderRequest request) {
		requireNonNull(request);

		UUID patientOrderId = request.getPatientOrderId();
		@SuppressWarnings("unused")
		// Currently unused; would be the account flipping the flag (understood to be 'system' if not specified)
		UUID accountId = request.getAccountId();
		PatientOrder patientOrder = null;
		ValidationException validationException = new ValidationException();

		if (patientOrderId == null) {
			validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is required.")));
		} else {
			patientOrder = findPatientOrderById(patientOrderId).orElse(null);

			if (patientOrder == null)
				validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		// Not changing anything, no action to take
		if (patientOrder.getPatientOrderDispositionId() == PatientOrderDispositionId.ARCHIVED)
			return false;

		boolean updated = getDatabase().execute("""
				UPDATE patient_order
				SET patient_order_disposition_id=?
				WHERE patient_order_id=?
				""", PatientOrderDispositionId.ARCHIVED, patientOrderId) > 0;

		// TODO: track event

		return updated;
	}

	@Nonnull
	public Boolean updatePatientOrderOutreachNeeded(@Nonnull UpdatePatientOrderOutreachNeededRequest request) {
		requireNonNull(request);

		UUID patientOrderId = request.getPatientOrderId();
		Boolean outreachNeeded = request.getOutreachNeeded();
		@SuppressWarnings("unused")
		// Currently unused; would be the account flipping the flag (understood to be 'system' if not specified)
		UUID accountId = request.getAccountId();
		PatientOrder patientOrder = null;
		ValidationException validationException = new ValidationException();

		if (patientOrderId == null) {
			validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is required.")));
		} else {
			patientOrder = findPatientOrderById(patientOrderId).orElse(null);

			if (patientOrder == null)
				validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is invalid.")));
		}

		if (outreachNeeded == null)
			validationException.add(new FieldError("outreachNeeded", getStrings().get("Outreach Needed is required.")));

		if (validationException.hasErrors())
			throw validationException;

		// Not changing anything, no action to take
		if (patientOrder.getOutreachNeeded().equals(outreachNeeded))
			return false;

		boolean updated = getDatabase().execute("""
				UPDATE patient_order
				SET outreach_needed=?
				WHERE patient_order_id=?
				""", outreachNeeded, patientOrderId) > 0;

		// TODO: track event

		return updated;
	}

	@Nonnull
	public Boolean updatePatientOrderFollowupNeeded(@Nonnull UpdatePatientOrderFollowupNeededRequest request) {
		requireNonNull(request);

		UUID patientOrderId = request.getPatientOrderId();
		Boolean followupNeeded = request.getFollowupNeeded();
		@SuppressWarnings("unused")
		// Currently unused; would be the account flipping the flag (understood to be 'system' if not specified)
		UUID accountId = request.getAccountId();
		PatientOrder patientOrder = null;
		ValidationException validationException = new ValidationException();

		if (patientOrderId == null) {
			validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is required.")));
		} else {
			patientOrder = findPatientOrderById(patientOrderId).orElse(null);

			if (patientOrder == null)
				validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is invalid.")));
		}

		if (followupNeeded == null)
			validationException.add(new FieldError("followupNeeded", getStrings().get("Followup Needed is required.")));

		if (validationException.hasErrors())
			throw validationException;

		// Not changing anything, no action to take
		if (patientOrder.getFollowupNeeded().equals(followupNeeded))
			return false;

		boolean updated = getDatabase().execute("""
				UPDATE patient_order
				SET followup_needed=?
				WHERE patient_order_id=?
				""", followupNeeded, patientOrderId) > 0;

		// TODO: track event

		return updated;
	}

	@Nonnull
	public Optional<PatientOrderScheduledScreening> findPatientOrderScheduledScreeningById(@Nullable UUID patientOrderScheduledScreeningId) {
		if (patientOrderScheduledScreeningId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				    SELECT *
				    FROM patient_order_scheduled_screening
				    WHERE patient_order_scheduled_screening_id=?
				""", PatientOrderScheduledScreening.class, patientOrderScheduledScreeningId);
	}

	@Nonnull
	public Optional<PatientOrderScheduledScreening> findActivePatientOrderScheduledScreeningByPatientOrderId(@Nullable UUID patientOrderId) {
		if (patientOrderId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				    SELECT *
				    FROM patient_order_scheduled_screening
				    WHERE patient_order_id=?
				    AND canceled=FALSE
				""", PatientOrderScheduledScreening.class, patientOrderId);
	}

	@Nonnull
	public UUID createPatientOrderScheduledScreening(@Nonnull CreatePatientOrderScheduledScreeningRequest request) {
		requireNonNull(request);

		UUID patientOrderId = request.getPatientOrderId();
		UUID accountId = request.getAccountId();
		LocalDate scheduledDate = request.getScheduledDate();
		String calendarUrl = trimToNull(request.getCalendarUrl());
		String scheduledTimeAsString = trimToNull(request.getScheduledTime());
		LocalTime scheduledTime = null;
		UUID patientOrderScheduledScreeningId = UUID.randomUUID();
		ValidationException validationException = new ValidationException();

		if (patientOrderId == null) {
			validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is required.")));
		} else {
			PatientOrder patientOrder = findPatientOrderById(patientOrderId).orElse(null);

			if (patientOrder == null) {
				validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is invalid.")));
			} else {
				PatientOrderScheduledScreening patientOrderScheduledScreening = findActivePatientOrderScheduledScreeningByPatientOrderId(patientOrderId).orElse(null);

				if (patientOrderScheduledScreening != null)
					validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order already has a scheduled screening.")));
			}
		}

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (scheduledDate == null)
			validationException.add(new FieldError("scheduledDate", getStrings().get("Scheduled Date is required.")));

		if (scheduledTimeAsString == null) {
			validationException.add(new FieldError("scheduledTime", getStrings().get("Scheduled Time is required.")));
		} else {
			// TODO: support other locales
			scheduledTime = getNormalizer().normalizeTime(scheduledTimeAsString, Locale.US).orElse(null);

			if (scheduledTime == null)
				validationException.add(new FieldError("scheduledTime", getStrings().get("Scheduled Time is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		LocalDateTime scheduledDateTime = LocalDateTime.of(scheduledDate, scheduledTime);

		getDatabase().execute("""
				INSERT INTO patient_order_scheduled_screening (
				    patient_order_scheduled_screening_id,
				    patient_order_id,
				    account_id,
				    scheduled_date_time,
				    calendar_url   
				) VALUES (?,?,?,?,?)
				""", patientOrderScheduledScreeningId, patientOrderId, accountId, scheduledDateTime, calendarUrl);

		// TODO: track changes

		return patientOrderScheduledScreeningId;
	}

	@Nonnull
	public Boolean updatePatientOrderScheduledScreening(@Nonnull UpdatePatientOrderScheduledScreeningRequest request) {
		requireNonNull(request);

		UUID patientOrderScheduledScreeningId = request.getPatientOrderScheduledScreeningId();
		UUID accountId = request.getAccountId();
		LocalDate scheduledDate = request.getScheduledDate();
		String calendarUrl = trimToNull(request.getCalendarUrl());
		String scheduledTimeAsString = trimToNull(request.getScheduledTime());
		LocalTime scheduledTime = null;
		PatientOrderScheduledScreening patientOrderScheduledScreening = null;
		ValidationException validationException = new ValidationException();

		if (patientOrderScheduledScreeningId == null) {
			validationException.add(new FieldError("patientOrderScheduledScreeningId", getStrings().get("Patient Order Scheduled Screening ID is required.")));
		} else {
			patientOrderScheduledScreening = findPatientOrderScheduledScreeningById(patientOrderScheduledScreeningId).orElse(null);

			if (patientOrderScheduledScreening == null)
				validationException.add(new FieldError("patientOrderScheduledScreeningId", getStrings().get("Patient Order Scheduled Screening ID is invalid.")));
		}

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (scheduledDate == null)
			validationException.add(new FieldError("scheduledDate", getStrings().get("Scheduled Date is required.")));

		if (scheduledTimeAsString == null) {
			validationException.add(new FieldError("scheduledTime", getStrings().get("Scheduled Time is required.")));
		} else {
			// TODO: support other locales
			scheduledTime = getNormalizer().normalizeTime(scheduledTimeAsString, Locale.US).orElse(null);

			if (scheduledTime == null)
				validationException.add(new FieldError("scheduledTime", getStrings().get("Scheduled Time is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		LocalDateTime scheduledDateTime = LocalDateTime.of(scheduledDate, scheduledTime);

		boolean updated = getDatabase().execute("""
				UPDATE patient_order_scheduled_screening SET
				account_id=?,
				scheduled_date_time=?,
				calendar_url=?
				WHERE patient_order_scheduled_screening_id=?
				""", accountId, scheduledDateTime, calendarUrl, patientOrderScheduledScreeningId) > 0;

		// TODO: track changes

		return updated;
	}

	@Nonnull
	public Boolean cancelPatientOrderScheduledScreening(@Nonnull CancelPatientOrderScheduledScreeningRequest request) {
		requireNonNull(request);

		UUID patientOrderScheduledScreeningId = request.getPatientOrderScheduledScreeningId();
		UUID accountId = request.getAccountId();
		PatientOrderScheduledScreening patientOrderScheduledScreening = null;
		ValidationException validationException = new ValidationException();

		if (patientOrderScheduledScreeningId == null) {
			validationException.add(new FieldError("patientOrderScheduledScreeningId", getStrings().get("Patient Order Scheduled Screening ID is required.")));
		} else {
			patientOrderScheduledScreening = findPatientOrderScheduledScreeningById(patientOrderScheduledScreeningId).orElse(null);

			if (patientOrderScheduledScreening == null)
				validationException.add(new FieldError("patientOrderScheduledScreeningId", getStrings().get("Patient Order Scheduled Screening ID is invalid.")));
		}

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		boolean updated = getDatabase().execute("""
				UPDATE patient_order_scheduled_screening SET
				account_id=?,
				canceled=TRUE,
				canceled_at=NOW()
				WHERE patient_order_scheduled_screening_id=?
				""", accountId, patientOrderScheduledScreeningId) > 0;

		// TODO: track changes

		return updated;
	}

	@Nonnull
	public Optional<PatientOrderVoicemailTask> findPatientOrderVoicemailTaskById(@Nullable UUID patientOrderVoicemailTaskId) {
		if (patientOrderVoicemailTaskId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_patient_order_voicemail_task
				WHERE patient_order_voicemail_task_id=?
				AND deleted=false
				""", PatientOrderVoicemailTask.class, patientOrderVoicemailTaskId);
	}

	@Nonnull
	public List<PatientOrderVoicemailTask> findPatientOrderVoicemailTasksByPatientOrderId(@Nullable UUID patientOrderId) {
		if (patientOrderId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM v_patient_order_voicemail_task
				WHERE patient_order_id=?
				ORDER BY last_updated DESC
				""", PatientOrderVoicemailTask.class, patientOrderId);
	}

	@Nonnull
	public UUID createPatientOrderVoicemailTask(@Nonnull CreatePatientOrderVoicemailTaskRequest request) {
		requireNonNull(request);

		UUID patientOrderId = request.getPatientOrderId();
		UUID createdByAccountId = request.getCreatedByAccountId();
		UUID panelAccountId = request.getPanelAccountId();
		String message = trimToNull(request.getMessage());
		PatientOrder patientOrder = null;
		UUID patientOrderVoicemailTaskId = UUID.randomUUID();
		ValidationException validationException = new ValidationException();

		if (patientOrderId == null) {
			validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is required.")));
		} else {
			patientOrder = findPatientOrderById(patientOrderId).orElse(null);

			if (patientOrder == null) {
				validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is invalid.")));
			} else {
				List<PatientOrderVoicemailTask> patientOrderVoicemailTasks = findPatientOrderVoicemailTasksByPatientOrderId(patientOrderId);

				for (PatientOrderVoicemailTask patientOrderVoicemailTask : patientOrderVoicemailTasks) {
					if (!patientOrderVoicemailTask.getCompleted()) {
						validationException.add(new FieldError("patientOrderId", getStrings().get("You may only have one active voicemail task per patient order.")));
						break;
					}
				}
			}
		}

		if (createdByAccountId == null)
			validationException.add(new FieldError("createdByAccountId", getStrings().get("Created-By Account ID is required.")));

		if (panelAccountId == null)
			validationException.add(new FieldError("panelAccountId", getStrings().get("Panel Account ID is required.")));

		if (message == null)
			validationException.add(new FieldError("message", getStrings().get("Message is required.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				INSERT INTO patient_order_voicemail_task (
				    patient_order_voicemail_task_id,
				    patient_order_id,
				    created_by_account_id,
				    message
				) VALUES (?,?,?,?)
				""", patientOrderVoicemailTaskId, patientOrderId, createdByAccountId, message);

		if (!Objects.equals(patientOrder.getPanelAccountId(), panelAccountId)) {
			getLogger().debug("As a side effect of creating a voicemail task, assigning Patient Order ID {} to Panel Account ID {}...",
					patientOrderId, panelAccountId);

			assignPatientOrderToPanelAccount(patientOrderId, panelAccountId, createdByAccountId);
		}

		// TODO: track changes

		return patientOrderVoicemailTaskId;
	}

	@Nonnull
	public Boolean updatePatientOrderVoicemailTask(@Nonnull UpdatePatientOrderVoicemailTaskRequest request) {
		requireNonNull(request);

		UUID patientOrderVoicemailTaskId = request.getPatientOrderVoicemailTaskId();
		UUID updatedByAccountId = request.getUpdatedByAccountId();
		UUID panelAccountId = request.getPanelAccountId();
		String message = trimToNull(request.getMessage());
		PatientOrderVoicemailTask patientOrderVoicemailTask = null;
		PatientOrder patientOrder = null;
		ValidationException validationException = new ValidationException();

		if (patientOrderVoicemailTaskId == null) {
			validationException.add(new FieldError("patientOrderVoicemailTaskId", getStrings().get("Patient Order Voicemail Task ID is required.")));
		} else {
			patientOrderVoicemailTask = findPatientOrderVoicemailTaskById(patientOrderVoicemailTaskId).orElse(null);

			if (patientOrderVoicemailTask == null) {
				validationException.add(new FieldError("patientOrderVoicemailTaskId", getStrings().get("Patient Order Voicemail Task ID is invalid.")));
			} else {
				patientOrder = findPatientOrderById(patientOrderVoicemailTask.getPatientOrderId()).get();

				if (patientOrderVoicemailTask.getCompleted() || patientOrderVoicemailTask.getDeleted())
					validationException.add(new FieldError("patientOrderVoicemailTaskId", getStrings().get("Cannot update past Patient Order Voicemail Tasks.")));
			}
		}

		if (updatedByAccountId == null)
			validationException.add(new FieldError("updatedByAccountId", getStrings().get("Updated-By Account ID is required.")));

		if (panelAccountId == null)
			validationException.add(new FieldError("panelAccountId", getStrings().get("Panel Account ID is required.")));

		if (message == null)
			validationException.add(new FieldError("message", getStrings().get("Message is required.")));

		if (validationException.hasErrors())
			throw validationException;

		boolean updated = getDatabase().execute("""
				UPDATE patient_order_voicemail_task
				SET message=?
				WHERE patient_order_voicemail_task_id=?
				""", message, patientOrderVoicemailTaskId) > 0;

		if (!Objects.equals(patientOrder.getPanelAccountId(), panelAccountId)) {
			getLogger().debug("As a side effect of updating a voicemail task, assigning Patient Order ID {} to Panel Account ID {}...",
					patientOrderVoicemailTask.getPatientOrderId(), panelAccountId);

			assignPatientOrderToPanelAccount(patientOrderVoicemailTask.getPatientOrderId(), panelAccountId, updatedByAccountId);
		}

		// TODO: track changes

		return updated;
	}

	@Nonnull
	public Boolean deletePatientOrderVoicemailTask(@Nonnull DeletePatientOrderVoicemailTaskRequest request) {
		requireNonNull(request);

		UUID patientOrderVoicemailTaskId = request.getPatientOrderVoicemailTaskId();
		UUID deletedByAccountId = request.getDeletedByAccountId();
		PatientOrderVoicemailTask patientOrderVoicemailTask = null;
		ValidationException validationException = new ValidationException();

		if (patientOrderVoicemailTaskId == null) {
			validationException.add(new FieldError("patientOrderVoicemailTaskId", getStrings().get("Patient Order Voicemail Task ID is required.")));
		} else {
			patientOrderVoicemailTask = findPatientOrderVoicemailTaskById(patientOrderVoicemailTaskId).orElse(null);

			if (patientOrderVoicemailTask == null)
				validationException.add(new FieldError("patientOrderVoicemailTaskId", getStrings().get("Patient Order Voicemail Task ID is invalid.")));
			else if (patientOrderVoicemailTask.getCompleted() || patientOrderVoicemailTask.getDeleted())
				validationException.add(new FieldError("patientOrderVoicemailTaskId", getStrings().get("Cannot delete past Patient Order Voicemail Tasks.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		boolean deleted = getDatabase().execute("""
				UPDATE patient_order_voicemail_task
				SET deleted=TRUE,
				deleted_by_account_id=?
				WHERE patient_order_voicemail_task_id=?
				""", deletedByAccountId, patientOrderVoicemailTaskId) > 0;

		// TODO: track changes

		return deleted;
	}

	@Nonnull
	public void completePatientOrderVoicemailTask(@Nonnull CompletePatientOrderVoicemailTaskRequest request) {
		requireNonNull(request);

		UUID patientOrderVoicemailTaskId = request.getPatientOrderVoicemailTaskId();
		UUID completedByAccountId = request.getCompletedByAccountId();
		PatientOrderVoicemailTask patientOrderVoicemailTask = null;
		ValidationException validationException = new ValidationException();

		if (patientOrderVoicemailTaskId == null) {
			validationException.add(new FieldError("patientOrderVoicemailTaskId", getStrings().get("Patient Order Voicemail Task ID is required.")));
		} else {
			patientOrderVoicemailTask = findPatientOrderVoicemailTaskById(patientOrderVoicemailTaskId).orElse(null);

			if (patientOrderVoicemailTask == null)
				validationException.add(new FieldError("patientOrderVoicemailTaskId", getStrings().get("Patient Order Voicemail Task ID is invalid.")));
			else if (patientOrderVoicemailTask.getCompleted() || patientOrderVoicemailTask.getDeleted())
				validationException.add(new FieldError("patientOrderVoicemailTaskId", getStrings().get("Cannot complete past Patient Order Voicemail Tasks.")));
		}

		if (completedByAccountId == null)
			validationException.add(new FieldError("completedByAccountId", getStrings().get("Completed-By Account ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				UPDATE patient_order_voicemail_task
				SET completed=TRUE,
				completed_by_account_id=?
				WHERE patient_order_voicemail_task_id=?
				""", completedByAccountId, patientOrderVoicemailTaskId);

		// TODO: track changes
	}

	@Nonnull
	public List<PatientOrderScheduledMessage> findPatientOrderScheduledMessagesByPatientOrderId(@Nullable UUID patientOrderId) {
		if (patientOrderId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM v_patient_order_scheduled_message
				WHERE patient_order_id=?
				AND scheduled_message_status_id != ?
				ORDER BY scheduled_at at time zone time_zone DESC
				""", PatientOrderScheduledMessage.class, patientOrderId, ScheduledMessageStatusId.CANCELED);
	}

	// Do some gymnastics to "unflatten" messages into group API responses.
	@Nonnull
	public List<PatientOrderScheduledMessageGroupApiResponse> generatePatientOrderScheduledMessageGroupApiResponses(@Nullable List<PatientOrderScheduledMessage> patientOrderScheduledMessages) {
		if (patientOrderScheduledMessages == null || patientOrderScheduledMessages.size() == 0)
			return List.of();

		Map<UUID, List<PatientOrderScheduledMessage>> messagesByPatientOrderScheduledMessageGroupId = new LinkedHashMap<>();

		for (PatientOrderScheduledMessage patientOrderScheduledMessage : patientOrderScheduledMessages) {
			List<PatientOrderScheduledMessage> messages = messagesByPatientOrderScheduledMessageGroupId.get(patientOrderScheduledMessage.getPatientOrderScheduledMessageGroupId());

			if (messages == null) {
				messages = new ArrayList<>();
				messagesByPatientOrderScheduledMessageGroupId.put(patientOrderScheduledMessage.getPatientOrderScheduledMessageGroupId(), messages);
			}

			messages.add(patientOrderScheduledMessage);
		}

		if (messagesByPatientOrderScheduledMessageGroupId.size() == 0)
			return List.of();

		List<PatientOrderScheduledMessageGroup> groups = getDatabase().queryForList(format("""
						SELECT *
						FROM patient_order_scheduled_message_group
						WHERE patient_order_scheduled_message_group_id IN %s
						""", sqlInListPlaceholders(messagesByPatientOrderScheduledMessageGroupId.keySet())),
				PatientOrderScheduledMessageGroup.class, messagesByPatientOrderScheduledMessageGroupId.keySet().toArray(new Object[0]));

		Map<UUID, PatientOrderScheduledMessageGroup> groupsByGroupId = groups.stream()
				.collect(Collectors.toMap(PatientOrderScheduledMessageGroup::getPatientOrderScheduledMessageGroupId, Function.identity()));

		List<PatientOrderScheduledMessageGroupApiResponse> groupApiResponses = new ArrayList<>(groupsByGroupId.size());

		for (Entry<UUID, List<PatientOrderScheduledMessage>> entry : messagesByPatientOrderScheduledMessageGroupId.entrySet()) {
			UUID patientOrderScheduledMessageGroupId = entry.getKey();
			List<PatientOrderScheduledMessage> messages = entry.getValue();

			PatientOrderScheduledMessageGroup patientOrderScheduledMessageGroup = groupsByGroupId.get(patientOrderScheduledMessageGroupId);
			groupApiResponses.add(getPatientOrderScheduledMessageGroupApiResponseFactory().create(patientOrderScheduledMessageGroup, messages));
		}

		return groupApiResponses;
	}

	@Nonnull
	public List<PatientOrderScheduledMessageType> findPatientOrderScheduledMessageTypes() {
		return getDatabase().queryForList("""
				SELECT *
				FROM patient_order_scheduled_message_type
				ORDER BY display_order
				""", PatientOrderScheduledMessageType.class);
	}

	@Nonnull
	public Optional<PatientOrderScheduledMessageType> findPatientOrderScheduledMessageTypeById(@Nullable PatientOrderScheduledMessageTypeId patientOrderScheduledMessageTypeId) {
		if (patientOrderScheduledMessageTypeId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM patient_order_scheduled_message_type
				WHERE patient_order_scheduled_message_type_id=?
				""", PatientOrderScheduledMessageType.class, patientOrderScheduledMessageTypeId);
	}

	@Nonnull
	public Optional<PatientOrderScheduledMessageGroup> findPatientOrderScheduledMessageGroupById(@Nullable UUID patientOrderScheduledMessageGroupId) {
		if (patientOrderScheduledMessageGroupId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM patient_order_scheduled_message_group
				WHERE patient_order_scheduled_message_group_id=?
				""", PatientOrderScheduledMessageGroup.class, patientOrderScheduledMessageGroupId);
	}

	@Nonnull
	public UUID createPatientOrderScheduledMessageGroup(@Nonnull CreatePatientOrderScheduledMessageGroupRequest request) {
		requireNonNull(request);

		UUID patientOrderId = request.getPatientOrderId();
		UUID accountId = request.getAccountId();
		PatientOrderScheduledMessageTypeId patientOrderScheduledMessageTypeId = request.getPatientOrderScheduledMessageTypeId();
		Set<MessageTypeId> messageTypeIds = request.getMessageTypeIds() == null ? Set.of() : request.getMessageTypeIds();
		LocalDate scheduledAtDate = request.getScheduledAtDate();
		String scheduledAtTimeAsString = trimToNull(request.getScheduledAtTime());
		LocalTime scheduledAtTime = request.getScheduledAtTimeAsLocalTime();
		PatientOrder patientOrder = null;
		PatientOrderScheduledMessageType patientOrderScheduledMessageType = null;
		UUID patientOrderScheduledMessageGroupId = UUID.randomUUID();
		ValidationException validationException = new ValidationException();

		if (patientOrderId == null) {
			validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is required.")));
		} else {
			patientOrder = findPatientOrderById(patientOrderId).orElse(null);

			if (patientOrder == null)
				validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is invalid.")));
		}

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (patientOrderScheduledMessageTypeId == null) {
			validationException.add(new FieldError("patientOrderScheduledMessageTypeId", getStrings().get("Patient Order Scheduled Message Type ID is required.")));
		} else {
			// Use .get() here b/c if this is not present, it's a programmer error and should 500
			patientOrderScheduledMessageType = findPatientOrderScheduledMessageTypeById(patientOrderScheduledMessageTypeId).get();
		}

		if (patientOrder != null) {
			if (messageTypeIds.contains(MessageTypeId.EMAIL) && patientOrder.getPatientEmailAddress() == null)
				validationException.add(new FieldError("messageTypeId", getStrings().get("Cannot schedule an email because this patient's order does not have an email address.")));

			if (messageTypeIds.contains(MessageTypeId.SMS) && patientOrder.getPatientPhoneNumber() == null)
				validationException.add(new FieldError("messageTypeId", getStrings().get("Cannot schedule an SMS message because this patient's order does not have a phone number.")));
		}

		if (scheduledAtDate == null)
			validationException.add(new FieldError("scheduledAtDate", getStrings().get("Scheduled-At Date is required.")));

		if (scheduledAtTime == null) {
			if (scheduledAtTimeAsString == null) {
				validationException.add(new FieldError("scheduledAtTime", getStrings().get("Scheduled-At Time is required.")));
			} else {
				// TODO: support other locales
				scheduledAtTime = getNormalizer().normalizeTime(scheduledAtTimeAsString, Locale.US).orElse(null);

				if (scheduledAtTime == null)
					validationException.add(new FieldError("scheduledAtTime", getStrings().get("Scheduled-At Time is invalid.")));
			}
		}

		if (messageTypeIds.size() == 0)
			validationException.add(getStrings().get("You must specify at least one type of message to send."));

		if (validationException.hasErrors())
			throw validationException;

		LocalDateTime scheduledAtDateTime = LocalDateTime.of(scheduledAtDate, scheduledAtTime);

		getDatabase().execute("""
				INSERT INTO patient_order_scheduled_message_group (
					patient_order_scheduled_message_group_id,
					patient_order_id,
					patient_order_scheduled_message_type_id,
					scheduled_at_date_time
				) VALUES (?,?,?,?)
				""", patientOrderScheduledMessageGroupId, patientOrderId, patientOrderScheduledMessageTypeId, scheduledAtDateTime);

		createScheduledMessagesForPatientOrderScheduledMessageGroup(patientOrderScheduledMessageGroupId, patientOrder,
				patientOrderScheduledMessageType, messageTypeIds, scheduledAtDate, scheduledAtTime);

		// TODO: track in event log

		return patientOrderScheduledMessageGroupId;
	}

	@Nonnull
	public Boolean updatePatientOrderScheduledMessageGroup(@Nonnull UpdatePatientOrderScheduledMessageGroupRequest request) {
		requireNonNull(request);

		UUID patientOrderScheduledMessageGroupId = request.getPatientOrderScheduledMessageGroupId();
		PatientOrderScheduledMessageTypeId patientOrderScheduledMessageTypeId = request.getPatientOrderScheduledMessageTypeId();
		UUID accountId = request.getAccountId();
		Set<MessageTypeId> messageTypeIds = request.getMessageTypeIds() == null ? Set.of() : request.getMessageTypeIds();
		LocalDate scheduledAtDate = request.getScheduledAtDate();
		String scheduledAtTimeAsString = trimToNull(request.getScheduledAtTime());
		LocalTime scheduledAtTime = null;
		PatientOrder patientOrder = null;
		PatientOrderScheduledMessageGroup patientOrderScheduledMessageGroup = null;
		PatientOrderScheduledMessageType patientOrderScheduledMessageType = null;
		ValidationException validationException = new ValidationException();

		if (patientOrderScheduledMessageGroupId == null) {
			validationException.add(new FieldError("patientOrderScheduledMessageGroupId", getStrings().get("Patient Order Scheduled Message Group ID is required.")));
		} else {
			patientOrderScheduledMessageGroup = findPatientOrderScheduledMessageGroupById(patientOrderScheduledMessageGroupId).orElse(null);

			if (patientOrderScheduledMessageGroup == null) {
				validationException.add(new FieldError("patientOrderScheduledMessageGroupId", getStrings().get("Patient Order Scheduled Message Group ID is invalid.")));
			} else {
				patientOrder = findPatientOrderById(patientOrderScheduledMessageGroup.getPatientOrderId()).get();

				if (messageTypeIds.contains(MessageTypeId.EMAIL) && patientOrder.getPatientEmailAddress() == null)
					validationException.add(new FieldError("messageTypeId", getStrings().get("Cannot schedule an email because this patient's order does not have an email address.")));

				if (messageTypeIds.contains(MessageTypeId.SMS) && patientOrder.getPatientPhoneNumber() == null)
					validationException.add(new FieldError("messageTypeId", getStrings().get("Cannot schedule an SMS message because this patient's order does not have a phone number.")));
			}
		}

		if (patientOrderScheduledMessageTypeId == null) {
			validationException.add(new FieldError("patientOrderScheduledMessageTypeId", getStrings().get("Patient Order Scheduled Message Type ID is required.")));
		} else {
			// Use .get() here b/c if this is not present, it's a programmer error and should 500
			patientOrderScheduledMessageType = findPatientOrderScheduledMessageTypeById(patientOrderScheduledMessageTypeId).get();
		}

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (scheduledAtDate == null)
			validationException.add(new FieldError("scheduledAtDate", getStrings().get("Scheduled-At Date is required.")));

		if (scheduledAtTimeAsString == null) {
			validationException.add(new FieldError("scheduledAtTime", getStrings().get("Scheduled-At Time is required.")));
		} else {
			// TODO: support other locales
			scheduledAtTime = getNormalizer().normalizeTime(scheduledAtTimeAsString, Locale.US).orElse(null);

			if (scheduledAtTime == null)
				validationException.add(new FieldError("scheduledAtTime", getStrings().get("Scheduled-At Time is invalid.")));
		}

		if (messageTypeIds.size() == 0)
			validationException.add(getStrings().get("You must specify at least one type of message to send."));

		if (validationException.hasErrors())
			throw validationException;

		LocalDateTime scheduledAtDateTime = LocalDateTime.of(scheduledAtDate, scheduledAtTime);

		boolean updated = getDatabase().execute("""
				UPDATE patient_order_scheduled_message_group
				SET patient_order_scheduled_message_type_id=?, scheduled_at_date_time=?
				WHERE patient_order_scheduled_message_group_id=?
				""", patientOrderScheduledMessageTypeId, scheduledAtDateTime, patientOrderScheduledMessageGroupId) > 0;

		// Cancel existing messages in this group...
		cancelScheduledMessagesForPatientOrderScheduledMessageGroup(patientOrderScheduledMessageGroupId);

		// ...then create new scheduled messages.
		createScheduledMessagesForPatientOrderScheduledMessageGroup(patientOrderScheduledMessageGroupId, patientOrder,
				patientOrderScheduledMessageType, messageTypeIds, scheduledAtDate, scheduledAtTime);

		// TODO: track changes in event log

		return updated;
	}

	@Nonnull
	public Boolean deletePatientOrderScheduledMessageGroup(@Nonnull DeletePatientOrderScheduledMessageGroupRequest request) {
		requireNonNull(request);

		UUID patientOrderScheduledMessageGroupId = request.getPatientOrderScheduledMessageGroupId();
		UUID accountId = request.getAccountId();
		ValidationException validationException = new ValidationException();

		if (validationException.hasErrors())
			throw validationException;

		boolean deleted = getDatabase().execute("""
				UPDATE patient_order_scheduled_message_group
				SET deleted=TRUE, deleted_at=NOW()
				WHERE patient_order_scheduled_message_group_id=?
				AND deleted=FALSE
				""", patientOrderScheduledMessageGroupId) > 0;

		cancelScheduledMessagesForPatientOrderScheduledMessageGroup(patientOrderScheduledMessageGroupId);

		// TODO: track changes in event log

		return deleted;
	}

	protected void cancelScheduledMessagesForPatientOrderScheduledMessageGroup(@Nonnull UUID patientOrderScheduledMessageGroupId) {
		requireNonNull(patientOrderScheduledMessageGroupId);

		List<UUID> cancelableScheduledMessageIds = getDatabase().queryForList("""
				SELECT sm.scheduled_message_id
				FROM scheduled_message sm, patient_order_scheduled_message posm
				WHERE sm.scheduled_message_id=posm.scheduled_message_id
				AND posm.patient_order_scheduled_message_group_id=?
				AND sm.scheduled_message_status_id=?
				""", UUID.class, patientOrderScheduledMessageGroupId, ScheduledMessageStatusId.PENDING);

		getLogger().info("Canceling {} pending scheduled message[s] for patientOrderScheduledMessageGroupId {}...",
				cancelableScheduledMessageIds.size(), patientOrderScheduledMessageGroupId);

		for (UUID cancelableScheduledMessageId : cancelableScheduledMessageIds)
			getMessageService().cancelScheduledMessage(cancelableScheduledMessageId);
	}

	protected void createScheduledMessagesForPatientOrderScheduledMessageGroup(@Nonnull UUID patientOrderScheduledMessageGroupId,
																																						 @Nonnull PatientOrder patientOrder,
																																						 @Nonnull PatientOrderScheduledMessageType patientOrderScheduledMessageType,
																																						 @Nonnull Set<MessageTypeId> messageTypeIds,
																																						 @Nonnull LocalDate scheduledAtDate,
																																						 @Nonnull LocalTime scheduledAtTime) {
		requireNonNull(patientOrderScheduledMessageGroupId);
		requireNonNull(patientOrder);
		requireNonNull(patientOrderScheduledMessageType);
		requireNonNull(messageTypeIds);
		requireNonNull(scheduledAtDate);
		requireNonNull(scheduledAtTime);

		Institution institution = getInstitutionService().findInstitutionById(patientOrder.getInstitutionId()).get();
		String webappBaseUrl = getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institution.getInstitutionId(), UserExperienceTypeId.PATIENT).get();

		Set<UUID> scheduledMessageIds = new HashSet<>();

		Map<String, Object> standardMessageContext = new HashMap<>();
		standardMessageContext.put("webappBaseUrl", webappBaseUrl);
		standardMessageContext.put("integratedCarePhoneNumber", institution.getIntegratedCarePhoneNumber());
		standardMessageContext.put("integratedCarePhoneNumberDescription", getFormatter().formatPhoneNumber(institution.getIntegratedCarePhoneNumber(), institution.getLocale()));
		standardMessageContext.put("integratedCareAvailabilityDescription", institution.getIntegratedCareAvailabilityDescription());
		standardMessageContext.put("integratedCareProgramName", institution.getIntegratedCareProgramName());
		standardMessageContext.put("integratedCarePrimaryCareName", institution.getIntegratedCarePrimaryCareName());

		// For now, all messages get the same standard context.  We might have custom contexts per-message/message type as we introduce more
		if (messageTypeIds.contains(MessageTypeId.EMAIL)) {
			EmailMessage emailMessage = new EmailMessage.Builder(institution.getInstitutionId(), EmailMessageTemplate.valueOf(patientOrderScheduledMessageType.getTemplateName()), Locale.US)
					.toAddresses(List.of(patientOrder.getPatientEmailAddress()))
					.fromAddress(institution.getDefaultFromEmailAddress())
					.messageContext(standardMessageContext)
					.build();

			scheduledMessageIds.add(getMessageService().createScheduledMessage(new CreateScheduledMessageRequest<>() {{
				setMetadata(Map.of("patientOrderScheduledMessageGroupId", patientOrderScheduledMessageGroupId));
				setMessage(emailMessage);
				setTimeZone(institution.getTimeZone());
				setScheduledAt(LocalDateTime.of(scheduledAtDate, scheduledAtTime));
			}}));
		}

		if (messageTypeIds.contains(MessageTypeId.SMS)) {
			SmsMessage smsMessage = new SmsMessage.Builder(institution.getInstitutionId(), SmsMessageTemplate.valueOf(patientOrderScheduledMessageType.getTemplateName()), patientOrder.getPatientPhoneNumber(), Locale.US)
					.messageContext(standardMessageContext)
					.build();

			scheduledMessageIds.add(getMessageService().createScheduledMessage(new CreateScheduledMessageRequest<>() {{
				setMetadata(Map.of("patientOrderScheduledMessageGroupId", patientOrderScheduledMessageGroupId));
				setMessage(smsMessage);
				setTimeZone(institution.getTimeZone());
				setScheduledAt(LocalDateTime.of(scheduledAtDate, scheduledAtTime));
			}}));
		}

		for (UUID scheduledMessageId : scheduledMessageIds) {
			getDatabase().execute("""
					INSERT INTO patient_order_scheduled_message (
						patient_order_scheduled_message_group_id,
						scheduled_message_id
					) VALUES (?,?)
					""", patientOrderScheduledMessageGroupId, scheduledMessageId);
		}
	}

	@Nonnull
	public PatientOrderImportResult createPatientOrderImport(@Nonnull CreatePatientOrderImportRequest request) {
		requireNonNull(request);

		InstitutionId institutionId = request.getInstitutionId();
		PatientOrderImportTypeId patientOrderImportTypeId = request.getPatientOrderImportTypeId();
		UUID accountId = request.getAccountId();
		String csvContent = trimToNull(request.getCsvContent());
		String rawOrderChecksum = null;
		boolean automaticallyAssignToPanelAccounts = request.getAutomaticallyAssignToPanelAccounts() == null ? false : request.getAutomaticallyAssignToPanelAccounts();
		UUID patientOrderImportId = UUID.randomUUID();
		List<UUID> patientOrderIds = new ArrayList<>();
		ValidationException validationException = new ValidationException();

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (patientOrderImportTypeId == null) {
			validationException.add(new FieldError("patientOrderImportTypeId", getStrings().get("Patient Order Import Type ID is required.")));
		} else if (patientOrderImportTypeId == PatientOrderImportTypeId.CSV) {

			if (csvContent == null) {
				validationException.add(new FieldError("csvContent", getStrings().get("CSV file is required.")));
			} else {
				rawOrderChecksum = Hashing.sha256()
						.hashString(csvContent, StandardCharsets.UTF_8)
						.toString();

				PatientOrderImport existingPatientOrderImportMatchingChecksum = getDatabase().queryForObject("""
						SELECT *
						FROM patient_order_import
						WHERE raw_order_checksum=?
						AND institution_id=?
						""", PatientOrderImport.class, rawOrderChecksum, institutionId).orElse(null);

				if (existingPatientOrderImportMatchingChecksum != null)
					validationException.add(new FieldError("csvContent", getStrings().get("This file has already been imported.")));
			}

			if (accountId == null)
				validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));
		} else {
			// TODO: revisit when we support EPIC imports directly
			throw new IllegalArgumentException(format("We do not yet support %s.%s", PatientOrderImportTypeId.class.getSimpleName(),
					patientOrderImportTypeId.name()));
		}

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				INSERT INTO patient_order_import (
				patient_order_import_id,
				patient_order_import_type_id,
				institution_id,
				account_id,
				raw_order,
				raw_order_checksum
				) VALUES (?,?,?,?,?,?)
				""", patientOrderImportId, patientOrderImportTypeId, institutionId, accountId, csvContent, rawOrderChecksum);

		if (patientOrderImportTypeId == PatientOrderImportTypeId.CSV) {
			Map<Integer, ValidationException> validationExceptionsByRowNumber = new HashMap<>();
			int rowNumber = 0;

			// If first column header is "Test Patient Email Address", then this is a test file
			boolean containsTestPatientData = csvContent.startsWith("Test Patient Email Address");

			if (containsTestPatientData && !getConfiguration().getShouldEnableIcDebugging())
				throw new IllegalStateException("Cannot upload test patient data in this environment.");

			getLogger().info("Importing patient orders from CSV...");
			int i = 0;

			// Pull data from the CSV
			try (Reader reader = new StringReader(csvContent)) {
				for (CSVRecord record : CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
					getLogger().info("Importing patient order {}...", ++i);

					CreatePatientOrderRequest patientOrderRequest = new CreatePatientOrderRequest();
					patientOrderRequest.setPatientOrderImportId(patientOrderImportId);
					patientOrderRequest.setInstitutionId(institutionId);
					patientOrderRequest.setAccountId(accountId);

					int columnOffset = 0;

					if (containsTestPatientData) {
						patientOrderRequest.setTestPatientEmailAddress(trimToNull(record.get("Test Patient Email Address")));
						patientOrderRequest.setTestPatientPassword(trimToNull(record.get("Test Patient Password")));
						columnOffset = 2;
					}

					String encounterDepartmentName = null;

					// Support alternate names for this field
					if (record.isMapped("Encounter Dept"))
						encounterDepartmentName = trimToNull(record.get("Encounter Dept"));
					else if (record.isMapped("Encounter Dept Name"))
						encounterDepartmentName = trimToNull(record.get("Encounter Dept Name"));

					patientOrderRequest.setEncounterDepartmentName(encounterDepartmentName);
					patientOrderRequest.setEncounterDepartmentId(trimToNull(record.get("Encounter Dept ID")));

					// Referring Practice has 2 fields with the same name (currently...)
					// So we try the first one, and if it's null, we try the second
					String rawReferringPracticeName = trimToNull(record.get(columnOffset + 2));

					if (rawReferringPracticeName == null)
						rawReferringPracticeName = trimToNull(record.get(columnOffset + 3));

					if (rawReferringPracticeName != null) {
						NameWithEmbeddedId referringPractice = new NameWithEmbeddedId(rawReferringPracticeName);
						String referringPracticeId = referringPractice.getId().orElse(null);

						if (referringPracticeId != null)
							patientOrderRequest.setReferringPracticeId(referringPracticeId);

						patientOrderRequest.setReferringPracticeName(referringPractice.getName());
					}

					CsvName orderingProviderName = new CsvName(trimToNull(record.get("Ordering Provider")));
					patientOrderRequest.setOrderingProviderLastName(orderingProviderName.getLastName().orElse(null));
					patientOrderRequest.setOrderingProviderFirstName(orderingProviderName.getFirstName().orElse(null));
					patientOrderRequest.setOrderingProviderMiddleName(orderingProviderName.getMiddleName().orElse(null));

					// Normalizes some names and also extracts IDs.
					//
					// Examples:
					// billingProviderName="ROBINSON, LAURA E [R11853]" -> "ROBINSON, LAURA E" (name), "R11853" (id)
					String rawBillingProviderName = trimToNull(record.get("Billing Provider"));

					if (rawBillingProviderName != null) {
						NameWithEmbeddedId billingProviderName = new NameWithEmbeddedId(rawBillingProviderName);
						String billingProviderId = billingProviderName.getId().orElse(null);

						if (billingProviderId != null)
							patientOrderRequest.setBillingProviderId(billingProviderId);

						CsvName csvBillingProviderName = new CsvName(billingProviderName.getName());
						patientOrderRequest.setBillingProviderLastName(csvBillingProviderName.getLastName().orElse(null));
						patientOrderRequest.setBillingProviderFirstName(csvBillingProviderName.getFirstName().orElse(null));
						patientOrderRequest.setBillingProviderMiddleName(csvBillingProviderName.getMiddleName().orElse(null));
					}

					patientOrderRequest.setPatientLastName(trimToNull(record.get("Last Name")));
					patientOrderRequest.setPatientFirstName(trimToNull(record.get("First Name")));
					patientOrderRequest.setPatientMrn(trimToNull(record.get("MRN")));
					patientOrderRequest.setPatientId(trimToNull(record.get("UID")));
					patientOrderRequest.setPatientIdType("UID");
					patientOrderRequest.setPatientBirthSexId(trimToNull(record.get("Sex")));
					patientOrderRequest.setPatientBirthdate(trimToNull(record.get("DOB")));

					// e.g. 128000-IBC
					String primaryPayor = trimToNull(record.get("Primary Payor"));
					String primaryPayorId = null;
					String primaryPayorName = null;

					if (primaryPayor != null) {
						int primaryPayorSeparatorIndex = primaryPayor.indexOf("-");

						if (primaryPayorSeparatorIndex == -1) {
							primaryPayorName = primaryPayor;
						} else {
							primaryPayorId = primaryPayor.substring(0, primaryPayorSeparatorIndex);
							primaryPayorName = primaryPayor.length() > primaryPayorId.length() + 1
									? primaryPayor.substring(primaryPayorSeparatorIndex + 1)
									: null;
						}
					}

					patientOrderRequest.setPrimaryPayorId(primaryPayorId);
					patientOrderRequest.setPrimaryPayorName(primaryPayorName);

					// e.g. 128002-KEYSTONE HEALTH PLAN EAST
					String primaryPlan = trimToNull(record.get("Primary Plan"));
					String primaryPlanId = null;
					String primaryPlanName = null;

					if (primaryPlan != null) {
						int primaryPlanSeparatorIndex = primaryPlan.indexOf("-");

						if (primaryPlanSeparatorIndex == -1) {
							primaryPlanName = primaryPayor;
						} else {
							primaryPlanId = primaryPlan.substring(0, primaryPlanSeparatorIndex);
							primaryPlanName = primaryPlan.length() > primaryPlanId.length() + 1
									? primaryPlan.substring(primaryPlanSeparatorIndex + 1)
									: null;
						}
					}

					patientOrderRequest.setPrimaryPlanId(primaryPlanId);
					patientOrderRequest.setPrimaryPlanName(primaryPlanName);

					// Hack: if test data, pick random insurance to assign
					if (containsTestPatientData) {
						List<PatientOrderInsurancePayor> patientOrderInsurancePayors = findPatientOrderInsurancePayorsByInstitutionId(institutionId);
						List<PatientOrderInsurancePlan> patientOrderInsurancePlans = findPatientOrderInsurancePlansByInstitutionId(institutionId);

						PatientOrderInsurancePlan randomInsurancePlan = patientOrderInsurancePlans.get(ThreadLocalRandom.current().nextInt(patientOrderInsurancePlans.size()));
						PatientOrderInsurancePayor insurancePayor = patientOrderInsurancePayors.stream()
								.filter(patientOrderInsurancePayor -> patientOrderInsurancePayor.getPatientOrderInsurancePayorId().equals(randomInsurancePlan.getPatientOrderInsurancePayorId()))
								.findAny()
								.get();

						patientOrderRequest.setPrimaryPayorName(insurancePayor.getName());
						patientOrderRequest.setPrimaryPlanName(randomInsurancePlan.getName());
					}

					patientOrderRequest.setOrderDate(trimToNull(record.get("Order Date")));
					patientOrderRequest.setOrderId(trimToNull(record.get("Order ID")));
					patientOrderRequest.setOrderAge(trimToNull(record.get("Age of Order")));
					patientOrderRequest.setRouting(trimToNull(record.get("CCBH Order Routing")));

					// Comma-separated list
					String reasonsForReferralAsString = trimToNull(record.get("Reasons for Referral"));
					List<String> reasonsForReferral = new ArrayList<>();
					Set<String> uniqueReasonsForReferral = new HashSet<>();

					if (reasonsForReferralAsString != null) {
						for (String reasonForReferral : reasonsForReferralAsString.split(",")) {
							reasonForReferral = trimToNull(reasonForReferral);

							if (reasonForReferral != null) {
								// Prevent duplicates for this order
								if (uniqueReasonsForReferral.contains(reasonsForReferral))
									continue;

								uniqueReasonsForReferral.add(reasonForReferral);
								reasonsForReferral.add(reasonForReferral);
							}
						}
					}

					patientOrderRequest.setReasonsForReferral(reasonsForReferral);

					// Might be encoded as names + bracketed IDs in CSV like this (a single field with newlines)
					// "GAD (generalized anxiety disorder) [213881]
					// Smoker [283397]
					// Alcohol abuse [155739]"
					String diagnosesAsString = trimToNull(record.get("DX"));
					List<CreatePatientOrderDiagnosisRequest> diagnoses = parseNamesWithEmbeddedIds(diagnosesAsString).stream()
							.map(nameWithEmbeddedId -> {
								CreatePatientOrderDiagnosisRequest diagnosisRequest = new CreatePatientOrderDiagnosisRequest();
								diagnosisRequest.setDiagnosisId(nameWithEmbeddedId.getId().orElse(null));
								diagnosisRequest.setDiagnosisName(nameWithEmbeddedId.getName());
								return diagnosisRequest;
							})
							.collect(Collectors.toList());

					patientOrderRequest.setDiagnoses(diagnoses);

					patientOrderRequest.setAssociatedDiagnosis(trimToNull(record.get("Order Associated Diagnosis (ICD-10)")));
					patientOrderRequest.setPatientPhoneNumber(trimToNull(record.get("Call Back Number")));
					patientOrderRequest.setPreferredContactHours(trimToNull(record.get("Preferred Contact Hours")));
					patientOrderRequest.setComments(trimToNull(record.get("Order Comments")));
					patientOrderRequest.setCcRecipients(trimToNull(record.get("IMG CC Recipients")));
					patientOrderRequest.setPatientAddressLine1(trimToNull(record.get("Patient Address (Line 1)")));
					patientOrderRequest.setPatientAddressLine2(trimToNull(record.get("Patient Address (Line 2)")));
					patientOrderRequest.setPatientLocality(trimToNull(record.get("City")));
					patientOrderRequest.setPatientRegion(trimToNull(record.get("Patient State")));
					patientOrderRequest.setPatientPostalCode(trimToNull(record.get("ZIP Code")));

					// e.g. "Take 1 tablet by mouth daily.<br>E-Prescribe, Disp-60 tablet, R-1"
					String lastActiveMedicationOrderSummary = trimToNull(record.get("CCBH Last Active Med Order Summary"));

					if (lastActiveMedicationOrderSummary != null)
						// Replacing just <br> for now - any others?
						lastActiveMedicationOrderSummary = lastActiveMedicationOrderSummary.replace("<br>", "\n");

					patientOrderRequest.setLastActiveMedicationOrderSummary(lastActiveMedicationOrderSummary);

					// e.g. "escitalopram 10 mg tablet [517587114]"
					// Might have multiple lines...
					String medicationsAsString = trimToNull(record.get("CCBH Medications List"));

					List<CreatePatientOrderMedicationRequest> medications = parseNamesWithEmbeddedIds(medicationsAsString).stream()
							.map(nameWithEmbeddedId -> {
								CreatePatientOrderMedicationRequest medicationRequest = new CreatePatientOrderMedicationRequest();
								medicationRequest.setMedicationId(nameWithEmbeddedId.getId().orElse(null));

								String medicationName = nameWithEmbeddedId.getName();

								// e.g. "escitalopram 10 mg tablet" -> "Escitalopram 10 mg tablet"
								if (medicationName != null)
									medicationName = StringUtils.capitalize(medicationName);

								medicationRequest.setMedicationName(medicationName);
								return medicationRequest;
							})
							.collect(Collectors.toList());

					patientOrderRequest.setMedications(medications);
					patientOrderRequest.setRecentPsychotherapeuticMedications(trimToNull(record.get("Psychotherapeutic Med Lst 2 Weeks")));

					try {
						UUID patientOrderId = createPatientOrder(patientOrderRequest);
						patientOrderIds.add(patientOrderId);
					} catch (ValidationException e) {
						validationExceptionsByRowNumber.put(rowNumber, e);
					}

					++rowNumber;
				}
			} catch (IOException e) {
				// In practice, we should never hit IOException because the Reader is operating over an in-memory String
				throw new UncheckedIOException("Unable to read CSV string", e);
			} catch (IllegalArgumentException e) {
				getLogger().warn("Unable to read CSV order import file", e);
				throw new ValidationException(getStrings().get("Unable to read the CSV patient order import file. Please double-check that the format is correct."));
			}

			// If any row-level validation exceptions, group all the errors per row into a single line.
			// Then throw back the list of lines to the client.
			// example: "Row 1: Patient ID is required. Callback Phone Number is invalid.", "Row 3: Patient Last Name is required."
			if (validationExceptionsByRowNumber.size() > 0) {
				List<String> globalErrors = new ArrayList<>();
				List<Integer> rowNumbers = validationExceptionsByRowNumber.keySet().stream().sorted().toList();

				for (Integer currentRowNumber : rowNumbers) {
					ValidationException currentValidationException = validationExceptionsByRowNumber.get(currentRowNumber);
					List<String> rowErrors = new ArrayList<>(currentValidationException.getGlobalErrors());
					rowErrors.addAll(currentValidationException.getFieldErrors().stream()
							.map(fieldError -> fieldError.getError())
							.collect(Collectors.toSet()));

					String rowErrorsAsString = rowErrors.stream().collect(Collectors.joining(" "));

					globalErrors.add(getStrings().get("Row {{rowNumber}}: {{rowErrors}}", new HashMap<>() {{
						put("rowNumber", currentRowNumber + 1);
						put("rowErrors", rowErrorsAsString);
					}}));
				}

				throw new ValidationException(globalErrors, List.of());
			}
		}

		if (automaticallyAssignToPanelAccounts) {
			List<Account> panelAccounts = findPanelAccountsByInstitutionId(institutionId);

			if (panelAccounts.size() > 0) {
				int i = 0;

				// TODO: more details on criteria for how to assign?  For now we're distributing evenly
				for (UUID patientOrderId : patientOrderIds) {
					int panelAccountIndex = i % panelAccounts.size();
					Account panelAccount = panelAccounts.get(panelAccountIndex);

					assignPatientOrderToPanelAccount(patientOrderId, panelAccount.getAccountId(), accountId);

					++i;
				}
			}
		}

		// For any orders in the import batch that have no flags, send a welcome message automatically
		List<PatientOrder> importedPatientOrders = findPatientOrdersByPatientOrderImportId(patientOrderImportId);
		Institution institution = getInstitutionService().findInstitutionById(institutionId).get();
		LocalDateTime now = LocalDateTime.now(institution.getTimeZone());

		for (PatientOrder importedPatientOrder : importedPatientOrders) {
			// If the order's insurance and region (state in US) are OK, then send the welcome message...
			if (importedPatientOrder.getPatientOrderInsurancePlanAccepted() && importedPatientOrder.getPatientAddressRegionAccepted()) {
				getLogger().info("Patient Order ID {} has an accepted region and insurance - automatically sending welcome message.", importedPatientOrder.getPatientOrderId());

				Set<MessageTypeId> messageTypeIds = new HashSet<>();

				if (importedPatientOrder.getPatientEmailAddress() != null)
					messageTypeIds.add(MessageTypeId.EMAIL);

				if (importedPatientOrder.getPatientPhoneNumber() != null)
					messageTypeIds.add(MessageTypeId.SMS);

				createPatientOrderScheduledMessageGroup(new CreatePatientOrderScheduledMessageGroupRequest() {{
					setPatientOrderId(importedPatientOrder.getPatientOrderId());
					setAccountId(accountId);
					setPatientOrderScheduledMessageTypeId(PatientOrderScheduledMessageTypeId.WELCOME);
					setMessageTypeIds(messageTypeIds);
					setScheduledAtDate(now.toLocalDate());
					setScheduledAtTimeAsLocalTime(now.toLocalTime());
				}});
			} else {
				// ...the order needs review by a human.  Don't send the welcome message.
				getLogger().info("Patient Order ID {} has either an un-accepted region or insurance - not automatically sending welcome message.", importedPatientOrder.getPatientOrderId());
			}
		}

		return new PatientOrderImportResult(patientOrderImportId, patientOrderIds);
	}

	@Nonnull
	public UUID createPatientOrder(@Nonnull CreatePatientOrderRequest request) {
		requireNonNull(request);

		PatientOrderDispositionId patientOrderDispositionId = PatientOrderDispositionId.OPEN;
		UUID patientOrderImportId = request.getPatientOrderImportId();
		InstitutionId institutionId = request.getInstitutionId();
		UUID accountId = request.getAccountId();
		String encounterDepartmentId = trimToNull(request.getEncounterDepartmentId());
		String encounterDepartmentIdType = trimToNull(request.getEncounterDepartmentIdType());
		String encounterDepartmentName = trimToNull(request.getEncounterDepartmentName());
		String referringPracticeId = trimToNull(request.getReferringPracticeId());
		String referringPracticeIdType = trimToNull(request.getReferringPracticeIdType());
		String referringPracticeName = trimToNull(request.getReferringPracticeName());
		String orderingProviderId = trimToNull(request.getOrderingProviderId());
		String orderingProviderIdType = trimToNull(request.getOrderingProviderIdType());
		String orderingProviderLastName = trimToNull(request.getOrderingProviderLastName());
		String orderingProviderFirstName = trimToNull(request.getOrderingProviderFirstName());
		String orderingProviderMiddleName = trimToNull(request.getOrderingProviderMiddleName());
		String billingProviderId = trimToNull(request.getBillingProviderId());
		String billingProviderIdType = trimToNull(request.getBillingProviderIdType());
		String billingProviderLastName = trimToNull(request.getBillingProviderLastName());
		String billingProviderFirstName = trimToNull(request.getBillingProviderFirstName());
		String billingProviderMiddleName = trimToNull(request.getBillingProviderMiddleName());
		String patientLastName = trimToNull(request.getPatientLastName());
		String patientFirstName = trimToNull(request.getPatientFirstName());
		String patientMrn = trimToNull(request.getPatientMrn());
		String patientId = trimToNull(request.getPatientId());
		String patientIdType = trimToNull(request.getPatientIdType());
		String patientBirthSexIdAsString = trimToNull(request.getPatientBirthSexId());
		BirthSexId patientBirthSexId = null;
		String patientBirthdateAsString = trimToNull(request.getPatientBirthdate());
		LocalDate patientBirthdate = null;
		String patientAddressLine1 = trimToNull(request.getPatientAddressLine1());
		String patientAddressLine2 = trimToNull(request.getPatientAddressLine2());
		String patientLocality = trimToNull(request.getPatientLocality());
		String patientRegion = trimToNull(request.getPatientRegion());
		String patientPostalCode = trimToNull(request.getPatientPostalCode());
		String patientCountryCode = trimToNull(request.getPatientCountryCode());
		UUID patientAddressId = null;
		String primaryPayorId = trimToNull(request.getPrimaryPayorId());
		String primaryPayorName = trimToNull(request.getPrimaryPayorName());
		String primaryPlanId = trimToNull(request.getPrimaryPlanId());
		String primaryPlanName = trimToNull(request.getPrimaryPlanName());
		String orderDateAsString = trimToNull(request.getOrderDate());
		LocalDate orderDate = null;
		String orderAge = trimToNull(request.getOrderAge());
		Long orderAgeInMinutes = null;
		String orderId = trimToNull(request.getOrderId());
		String routing = trimToNull(request.getRouting());
		List<String> reasonsForReferral = request.getReasonsForReferral();
		List<CreatePatientOrderDiagnosisRequest> diagnoses = request.getDiagnoses() == null ? List.of() : request.getDiagnoses().stream()
				.filter(diagnosis -> diagnosis != null)
				.collect(Collectors.toList());
		String associatedDiagnosis = trimToNull(request.getAssociatedDiagnosis());
		String patientPhoneNumber = trimToNull(request.getPatientPhoneNumber());
		String preferredContactHours = trimToNull(request.getPreferredContactHours());
		String comments = trimToNull(request.getComments());
		String ccRecipients = trimToNull(request.getCcRecipients());
		String lastActiveMedicationOrderSummary = trimToNull(request.getLastActiveMedicationOrderSummary());
		List<CreatePatientOrderMedicationRequest> medications = request.getMedications() == null ? List.of() : request.getMedications().stream()
				.filter(medication -> medication != null)
				.collect(Collectors.toList());
		String recentPsychotherapeuticMedications = trimToNull(request.getRecentPsychotherapeuticMedications());
		UUID patientOrderInsurancePlanId = null;
		String testPatientEmailAddress = trimToNull(request.getTestPatientEmailAddress());
		String testPatientPassword = trimToNull(request.getTestPatientPassword());
		UUID patientOrderId = UUID.randomUUID();
		Institution institution = null;
		ValidationException validationException = new ValidationException();

		// TODO: revisit when we support non-US institutions
		// Example: "2/25/21"
		DateTimeFormatter twoDigitYearDateFormatter = DateTimeFormatter.ofPattern("M/d/yy", Locale.US);
		// Example: "2/25/2021"
		DateTimeFormatter fourDigitYearDateFormatter = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US);

		if (patientOrderImportId == null)
			validationException.add(new FieldError("patientOrderImportId", getStrings().get("Patient Order Import ID is required.")));

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));
		else
			institution = getInstitutionService().findInstitutionById(institutionId).get();


		if (patientMrn == null)
			validationException.add(new FieldError("patientMrn", getStrings().get("Patient MRN is required.")));
		else
			patientMrn = patientMrn.toUpperCase(Locale.US); // TODO: revisit when we support non-US institutions

		if (patientId == null)
			validationException.add(new FieldError("patientId", getStrings().get("Patient ID is required.")));

		if (patientIdType == null)
			validationException.add(new FieldError("patientIdType", getStrings().get("Patient ID Type is required.")));

		if (orderId == null)
			validationException.add(new FieldError("orderId", getStrings().get("Order ID is required.")));

		if (orderDateAsString == null) {
			validationException.add(new FieldError("orderDate", getStrings().get("Order date is required.")));
		} else {
			try {
				orderDate = LocalDate.parse(orderDateAsString, twoDigitYearDateFormatter);
			} catch (Exception e) {
				validationException.add(new FieldError("orderDate", getStrings().get("Unrecognized order date format: {{orderDate}}",
						Map.of("orderDate", orderDateAsString))));
			}
		}

		if (diagnoses.size() > 0) {
			Set<String> diagnosisErrors = new HashSet<>();

			for (CreatePatientOrderDiagnosisRequest diagnosis : diagnoses)
				if (trimToNull(diagnosis.getDiagnosisName()) == null)
					diagnosisErrors.add(getStrings().get("Diagnosis name is required."));

			for (String diagnosisError : diagnosisErrors)
				validationException.add(new FieldError("diagnoses", diagnosisError));
		}

		if (medications.size() > 0) {
			Set<String> medicationErrors = new HashSet<>();

			for (CreatePatientOrderMedicationRequest medication : medications)
				if (trimToNull(medication.getMedicationName()) == null)
					medicationErrors.add(getStrings().get("Medication name is required."));

			for (String medicationError : medicationErrors)
				validationException.add(new FieldError("medications", medicationError));
		}

		if (orderAge == null) {
			validationException.add(new FieldError("orderAge", getStrings().get("Order age is required.")));
		} else {
			// Order Age example: "5d 05h 43m"
			int days = 0;
			int hours = 0;
			int minutes = 0;

			try {
				for (String orderAgeComponent : orderAge.split(" ")) {
					if (orderAgeComponent.endsWith("d")) {
						days = Integer.parseInt(orderAgeComponent.replace("d", ""), 10);
					} else if (orderAgeComponent.endsWith("h")) {
						hours = Integer.parseInt(orderAgeComponent.replace("h", ""), 10);
					} else if (orderAgeComponent.endsWith("m")) {
						minutes = Integer.parseInt(orderAgeComponent.replace("m", ""), 10);
					} else if (orderAgeComponent.length() > 0) {
						throw new IllegalArgumentException(format("Unexpected format for order age component '%s'", orderAgeComponent));
					}
				}

				orderAgeInMinutes = Duration.ofDays(days).plus(Duration.ofHours(hours)).plus(Duration.ofMinutes(minutes)).toMinutes();
			} catch (Exception e) {
				getLogger().warn(format("Unable to process order age string %s", orderAge), e);
			}
		}

		if (patientBirthdateAsString != null) {
			try {
				patientBirthdate = LocalDate.parse(patientBirthdateAsString, fourDigitYearDateFormatter);
			} catch (Exception e) {
				validationException.add(new FieldError("patientBirthdate", getStrings().get("Unrecognized patient birthdate format: {{patientBirthdate}}",
						Map.of("patientBirthdate", patientBirthdateAsString))));
			}
		}

		if (patientBirthSexIdAsString != null) {
			String normalizedPatientBirthSexIdAsString = patientBirthSexIdAsString.toUpperCase(Locale.US);

			if ("MALE".equals(normalizedPatientBirthSexIdAsString))
				patientBirthSexId = BirthSexId.MALE;
			else if ("FEMALE".equals(normalizedPatientBirthSexIdAsString))
				patientBirthSexId = BirthSexId.FEMALE;
		}

		// Fall back to UNKNOWN if we're not sure
		if (patientBirthSexId == null)
			patientBirthSexId = BirthSexId.UNKNOWN;

		try {
			String postalName = Normalizer.normalizeName(patientFirstName, patientLastName).get();

			patientAddressId = getAddressService().createAddress(new CreateAddressRequest() {{
				setPostalName(postalName);
				setStreetAddress1(patientAddressLine1);
				setStreetAddress2(patientAddressLine2);
				setLocality(patientLocality);
				setRegion(patientRegion);
				setPostalCode(patientPostalCode);
				// TODO: revisit when we support non-US institutions
				setCountryCode(patientCountryCode == null ? "US" : patientCountryCode);
			}});
		} catch (ValidationException addressValidationException) {
			validationException.add(addressValidationException);
		}

		if (patientPhoneNumber != null) {
			String originalPatientPhoneNumber = patientPhoneNumber;
			patientPhoneNumber = getNormalizer().normalizePhoneNumberToE164(patientPhoneNumber, Locale.US).orElse(null);

			if (patientPhoneNumber == null)
				validationException.add(new FieldError("patientPhoneNumber", getStrings().get("Invalid patient phone number: {{patientPhoneNumber}}.",
						Map.of("patientPhoneNumber", originalPatientPhoneNumber))));
		}

		if (testPatientEmailAddress == null && testPatientPassword != null)
			validationException.add(getStrings().get("If you specify a test patient password, you must also specify a test email address."));
		else if (testPatientEmailAddress != null && testPatientPassword == null)
			validationException.add(getStrings().get("If you specify a test patient email address, you must also specify a test password."));

		if (testPatientEmailAddress != null) {
			testPatientEmailAddress = getNormalizer().normalizeEmailAddress(testPatientEmailAddress).orElse(null);

			if (testPatientEmailAddress == null)
				validationException.add(new FieldError("testPatientEmailAddress", getStrings().get("Test patient email address is invalid.")));
		}

		PatientOrder openPatientOrder = findOpenPatientOrderByMrnAndInstitutionId(patientMrn, institutionId).orElse(null);

		if (openPatientOrder != null)
			validationException.add(getStrings().get("Patient {{firstName}} {{lastName}} with MRN {{mrn}} already has an open order.", Map.of(
					"firstName", openPatientOrder.getPatientFirstName(),
					"lastName", openPatientOrder.getPatientLastName(),
					"mrn", openPatientOrder.getPatientMrn()
			)));

		if (primaryPayorName == null)
			validationException.add(new FieldError("primaryPayorName", getStrings().get("Primary payor name is required.")));

		if (primaryPlanName == null)
			validationException.add(new FieldError("primaryPlanName", getStrings().get("Primary plan name is required.")));

		// TODO: clean up once we have more clarity on insurance information (will we receive IDs, or match on names?  etc.)
		// For now, just do our best and fall back to "other" if we don't know
		if (primaryPayorName != null && primaryPlanName != null) {
			List<PatientOrderInsurancePayor> patientOrderInsurancePayors = findPatientOrderInsurancePayorsByInstitutionId(institutionId);
			PatientOrderInsurancePayor matchingPatientOrderInsurancePayor = null;
			Locale locale = institution == null ? getConfiguration().getDefaultLocale() : institution.getLocale();

			for (PatientOrderInsurancePayor patientOrderInsurancePayor : patientOrderInsurancePayors) {
				if (Objects.equals(patientOrderInsurancePayor.getName().toUpperCase(locale), primaryPayorName.toUpperCase(locale))) {
					matchingPatientOrderInsurancePayor = patientOrderInsurancePayor;
					break;
				}
			}

			if (matchingPatientOrderInsurancePayor == null) {
				getLogger().warn("Unable to find payor named '{}', defaulting Patient Order ID {} to '{}' for Institution ID {}...", primaryPayorName, patientOrderId, PatientOrderInsurancePayorTypeId.OTHER.name(), institutionId);
				matchingPatientOrderInsurancePayor = findPatientOrderInsurancePayorsByInstitutionId(institutionId).stream()
						.filter(patientOrderInsurancePayor -> patientOrderInsurancePayor.getPatientOrderInsurancePayorTypeId() == PatientOrderInsurancePayorTypeId.OTHER)
						.findFirst()
						.get();
			}

			List<PatientOrderInsurancePlan> patientOrderInsurancePlans = findPatientOrderInsurancePlansByPatientOrderInsurancePayorId(matchingPatientOrderInsurancePayor.getPatientOrderInsurancePayorId());
			PatientOrderInsurancePlan matchingPatientOrderInsurancePlan = null;

			for (PatientOrderInsurancePlan patientOrderInsurancePlan : patientOrderInsurancePlans) {
				if (Objects.equals(patientOrderInsurancePlan.getName().toUpperCase(locale), primaryPlanName.toUpperCase(locale))) {
					matchingPatientOrderInsurancePlan = patientOrderInsurancePlan;
					break;
				}
			}

			if (matchingPatientOrderInsurancePlan == null) {
				getLogger().warn("Unable to find plan named '{}', defaulting Patient Order ID {} to '{}' for Institution ID {}...", primaryPlanName, patientOrderId, PatientOrderInsurancePlanTypeId.OTHER.name(), institutionId);

				matchingPatientOrderInsurancePlan = patientOrderInsurancePlans.stream()
						.filter(patientOrderInsurancePlan -> patientOrderInsurancePlan.getPatientOrderInsurancePlanTypeId() == PatientOrderInsurancePlanTypeId.OTHER)
						.findFirst()
						.get();
			}

			patientOrderInsurancePlanId = matchingPatientOrderInsurancePlan.getPatientOrderInsurancePlanId();
		}

		if (validationException.hasErrors())
			throw validationException;

		String hashedTestPatientPassword = testPatientPassword == null ? null : getAuthenticator().hashPassword(testPatientPassword);

		getDatabase().execute("""
						  INSERT INTO patient_order (
						  patient_order_id,
						  patient_order_disposition_id,
						  patient_order_import_id,
						  institution_id,
						  encounter_department_id,
						  encounter_department_id_type,
						  encounter_department_name,
						  referring_practice_id,
						  referring_practice_id_type,
						  referring_practice_name,
						  ordering_provider_id,
						  ordering_provider_id_type,
						  ordering_provider_last_name,
						  ordering_provider_first_name,
						  ordering_provider_middle_name,
						  billing_provider_id,
						  billing_provider_id_type,
						  billing_provider_last_name,
						  billing_provider_first_name,
						  billing_provider_middle_name,
						  patient_last_name,
						  patient_first_name,
						  patient_mrn,
						  patient_id,
						  patient_id_type,
						  patient_birth_sex_id,
						  patient_birthdate,
						  patient_address_id,
						  primary_payor_id,
						  primary_payor_name,
						  primary_plan_id,
						  primary_plan_name,
						  order_date,
						  order_age_in_minutes,
						  order_id,
						  routing,
						  associated_diagnosis,
						  patient_phone_number,
						  preferred_contact_hours,
						  comments,
						  cc_recipients,
						  last_active_medication_order_summary,
						  recent_psychotherapeutic_medications,
						  patient_order_insurance_plan_id,
						  test_patient_email_address,
						  test_patient_password
						) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
						""",
				patientOrderId, patientOrderDispositionId, patientOrderImportId,
				institutionId, encounterDepartmentId, encounterDepartmentIdType, encounterDepartmentName, referringPracticeId,
				referringPracticeIdType, referringPracticeName, orderingProviderId, orderingProviderIdType, orderingProviderLastName,
				orderingProviderFirstName, orderingProviderMiddleName, billingProviderId, billingProviderIdType,
				billingProviderLastName, billingProviderFirstName, billingProviderMiddleName, patientLastName, patientFirstName,
				patientMrn, patientId, patientIdType, patientBirthSexId, patientBirthdate, patientAddressId, primaryPayorId,
				primaryPayorName, primaryPlanId, primaryPlanName, orderDate, orderAgeInMinutes, orderId, routing,
				associatedDiagnosis, patientPhoneNumber, preferredContactHours, comments, ccRecipients,
				lastActiveMedicationOrderSummary, recentPsychotherapeuticMedications, patientOrderInsurancePlanId,
				testPatientEmailAddress, hashedTestPatientPassword);

		int diagnosisDisplayOrder = 0;

		for (CreatePatientOrderDiagnosisRequest diagnosis : diagnoses) {
			String diagnosisId = trimToNull(diagnosis.getDiagnosisId());
			String diagnosisIdType = trimToNull(diagnosis.getDiagnosisIdType());
			String diagnosisName = trimToNull(diagnosis.getDiagnosisName());

			getDatabase().execute("""
					INSERT INTO patient_order_diagnosis (
					patient_order_id,
					diagnosis_id,
					diagnosis_id_type,
					diagnosis_name,
					display_order
					) VALUES (?,?,?,?,?)
					""", patientOrderId, diagnosisId, diagnosisIdType, diagnosisName, diagnosisDisplayOrder);

			++diagnosisDisplayOrder;
		}

		int reasonForReferralDisplayOrder = 0;

		for (String reasonForReferral : reasonsForReferral) {
			getDatabase().execute("""
					INSERT INTO patient_order_reason_for_referral (
					patient_order_id,
					reason_for_referral,
					display_order
					) VALUES (?,?,?)
					""", patientOrderId, reasonForReferral, reasonForReferralDisplayOrder);

			++reasonForReferralDisplayOrder;
		}

		int medicationDisplayOrder = 0;

		for (CreatePatientOrderMedicationRequest medication : medications) {
			String medicationId = trimToNull(medication.getMedicationId());
			String medicationIdType = trimToNull(medication.getMedicationIdType());
			String medicationName = trimToNull(medication.getMedicationName());

			getDatabase().execute("""
					INSERT INTO patient_order_medication (
					patient_order_id, 
					medication_id,
					medication_id_type,
					medication_name,
					display_order
					) VALUES (?,?,?,?,?)
					""", patientOrderId, medicationId, medicationIdType, medicationName, medicationDisplayOrder);

			++medicationDisplayOrder;
		}

		createPatientOrderEvent(new CreatePatientOrderEventRequest() {{
			setPatientOrderEventTypeId(PatientOrderEventTypeId.IMPORTED);
			setPatientOrderId(patientOrderId);
			setAccountId(accountId);
			setMessage("Order imported."); // Not localized on the way in
			setMetadata(Map.of("patientOrderImportId", patientOrderImportId));
		}});

		return patientOrderId;
	}

	@Nonnull
	public Boolean patchPatientOrder(@Nonnull PatchPatientOrderRequest request) {
		requireNonNull(request);

		UUID patientOrderId = request.getPatientOrderId();
		UUID accountId = request.getAccountId();
		String patientFirstName = trimToNull(request.getPatientFirstName());
		String patientLastName = trimToNull(request.getPatientLastName());
		String patientEmailAddress = getNormalizer().normalizeEmailAddress(request.getPatientEmailAddress()).orElse(null);
		String patientPhoneNumber = trimToNull(request.getPatientPhoneNumber());
		GenderIdentityId patientGenderIdentityId = request.getPatientGenderIdentityId();
		EthnicityId patientEthnicityId = request.getPatientEthnicityId();
		BirthSexId patientBirthSexId = request.getPatientBirthSexId();
		RaceId patientRaceId = request.getPatientRaceId();
		LocalDate patientBirthdate = request.getPatientBirthdate();
		String patientLanguageCode = trimToNull(request.getPatientLanguageCode());
		CreateAddressRequest patientAddress = request.getPatientAddress();
		UUID patientOrderInsurancePlanId = request.getPatientOrderInsurancePlanId();
		Boolean patientDemographicsConfirmed = request.getPatientDemographicsConfirmed();
		PatientOrder patientOrder = null;
		Account account = null;
		List<Pair<String, Object>> columnNamesAndValues = new ArrayList<>();
		ValidationException validationException = new ValidationException();

		if (patientOrderId == null) {
			validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is required.")));
		} else {
			patientOrder = findPatientOrderById(patientOrderId).orElse(null);

			if (patientOrder == null)
				validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is invalid.")));
		}

		if (accountId == null) {
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));
		} else {
			account = getAccountService().findAccountById(accountId).orElse(null);

			if (account == null)
				validationException.add(new FieldError("accountId", getStrings().get("Account ID is invalid.")));
		}

		if (request.isShouldUpdatePatientFirstName()) {
			if (patientFirstName == null)
				validationException.add(new FieldError("patientFirstName", getStrings().get("First name is required.")));
			else
				columnNamesAndValues.add(Pair.of("patient_first_name", patientFirstName));
		}

		if (request.isShouldUpdatePatientLastName()) {
			if (patientLastName == null)
				validationException.add(new FieldError("patientLastName", getStrings().get("Last name is required.")));
			else
				columnNamesAndValues.add(Pair.of("patient_last_name", patientLastName));
		}

		if (request.isShouldUpdatePatientEmailAddress()) {
			if (patientEmailAddress == null)
				validationException.add(new FieldError("patientEmailAddress", getStrings().get("Email address is required.")));
			else if (!isValidEmailAddress(patientEmailAddress))
				validationException.add(new FieldError("patientEmailAddress", getStrings().get("Email address is invalid.")));
			else
				columnNamesAndValues.add(Pair.of("patient_email_address", getNormalizer().normalizeEmailAddress(patientEmailAddress).get()));
		}

		if (request.isShouldUpdatePatientPhoneNumber()) {
			if (patientPhoneNumber == null) {
				validationException.add(new FieldError("patientPhoneNumber", getStrings().get("Phone number is required.")));
			} else {
				// TODO: revisit when we support non-US institutions
				patientPhoneNumber = getNormalizer().normalizePhoneNumberToE164(request.getPatientPhoneNumber(), Locale.US).orElse(null);

				if (patientPhoneNumber == null)
					validationException.add(new FieldError("patientPhoneNumber", getStrings().get("Phone number is invalid.")));
				else
					columnNamesAndValues.add(Pair.of("patient_phone_number", patientPhoneNumber));
			}
		}

		if (request.isShouldUpdatePatientGenderIdentityId()) {
			if (patientGenderIdentityId == null)
				validationException.add(new FieldError("patientGenderIdentityId", getStrings().get("Gender identity is required.")));
			else
				columnNamesAndValues.add(Pair.of("patient_gender_identity_id", patientGenderIdentityId));
		}

		if (request.isShouldUpdatePatientEthnicityId()) {
			if (patientEthnicityId == null)
				validationException.add(new FieldError("patientEthnicityId", getStrings().get("Ethnicity is required.")));
			else
				columnNamesAndValues.add(Pair.of("patient_ethnicity_id", patientEthnicityId));
		}

		if (request.isShouldUpdatePatientBirthSexId()) {
			if (patientBirthSexId == null)
				validationException.add(new FieldError("patientBirthSexId", getStrings().get("Birth sex is required.")));
			else
				columnNamesAndValues.add(Pair.of("patient_birth_sex_id", patientBirthSexId));
		}

		if (request.isShouldUpdatePatientRaceId()) {
			if (patientRaceId == null)
				validationException.add(new FieldError("patientRaceId", getStrings().get("Race is required.")));
			else
				columnNamesAndValues.add(Pair.of("patient_race_id", patientRaceId));
		}

		if (request.isShouldUpdatePatientBirthdate()) {
			if (patientBirthdate == null)
				validationException.add(new FieldError("patientBirthdate", getStrings().get("Birthdate is required.")));
			else if (account != null && patientBirthdate.isAfter(LocalDate.now(account.getTimeZone())))
				validationException.add(new FieldError("patientBirthdate", getStrings().get("Birthdate cannot be in the future.")));
			else if (account != null)
				columnNamesAndValues.add(Pair.of("patient_birthdate", patientBirthdate));
		}

		if (request.isShouldUpdatePatientLanguageCode()) {
			if (patientLanguageCode == null)
				validationException.add(new FieldError("patientLanguageCode", getStrings().get("Language is required.")));
			else if (!Arrays.asList(Locale.getISOLanguages()).contains(patientLanguageCode))
				validationException.add(new FieldError("patientLanguageCode", getStrings().get("Language is invalid.")));
			else
				columnNamesAndValues.add(Pair.of("patient_language_code", patientLanguageCode));
		}

		if (request.isShouldUpdatePatientOrderInsurancePlanId()) {
			if (patientOrderInsurancePlanId == null)
				validationException.add(new FieldError("patientOrderInsurancePlanId", getStrings().get("Insurance Plan is required.")));
			else
				columnNamesAndValues.add(Pair.of("patient_order_insurance_plan_id", patientOrderInsurancePlanId));
		}

		if (request.isShouldUpdatePatientDemographicsConfirmed()) {
			if (patientDemographicsConfirmed == null) {
				validationException.add(new FieldError("patientDemographicsConfirmed", getStrings().get("Please confirm this information is accurate.")));
			} else if (patientDemographicsConfirmed == false) {
				validationException.add(new FieldError("patientDemographicsConfirmed", getStrings().get("You cannot un-confirm this information.")));
			} else {
				columnNamesAndValues.add(Pair.of("patient_demographics_confirmed_at", Instant.now()));
				columnNamesAndValues.add(Pair.of("patient_demographics_confirmed_by_account_id", accountId));
			}
		}

		if (validationException.hasErrors())
			throw validationException;

		if (request.isShouldUpdatePatientAddress()) {
			UUID patientAddressId = getAddressService().createAddress(patientAddress);
			columnNamesAndValues.add(Pair.of("patient_address_id", patientAddressId));
		}

		String setClause = columnNamesAndValues.stream()
				.map(columnNameAndValue -> format("%s=?", columnNameAndValue.getLeft()))
				.collect(Collectors.joining(", "));

		// Nothing to update? Bail
		if (setClause == null || setClause.length() == 0)
			return false;

		List<Object> parameters = columnNamesAndValues.stream()
				.map(columNameAndValue -> columNameAndValue.getRight())
				.collect(Collectors.toList());

		parameters.add(patientOrderId);

		boolean patched = getDatabase().execute(format("""
				UPDATE patient_order
				SET %s
				WHERE patient_order_id=?
				""", setClause), parameters.toArray(new Object[]{})) > 0;

		if (patched) {
			// TODO: record fields that were patched in our event log
		}

		return patched;
	}

	@Nonnull
	public UUID createPatientOrderEvent(@Nonnull CreatePatientOrderEventRequest request) {
		requireNonNull(request);

		PatientOrderEventTypeId patientOrderEventTypeId = request.getPatientOrderEventTypeId();
		UUID patientOrderId = request.getPatientOrderId();
		UUID accountId = request.getAccountId();
		String message = request.getMessage();
		Map<String, Object> metadata = request.getMetadata() == null ? Map.of() : request.getMetadata();
		UUID patientOrderEventId = UUID.randomUUID();
		ValidationException validationException = new ValidationException();

		if (patientOrderEventTypeId == null)
			validationException.add(new FieldError("patientOrderEventTypeId", getStrings().get("Patient Order Tracking Type ID is required.")));

		if (patientOrderId == null)
			validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is required.")));

		if (message == null)
			validationException.add(new FieldError("message", getStrings().get("Message is required.")));

		if (validationException.hasErrors())
			throw validationException;

		String metadataJson = getGson().toJson(metadata);

		getDatabase().execute("""
				INSERT INTO patient_order_event (
				patient_order_event_id, 
				patient_order_event_type_id,
				patient_order_id,
				account_id,
				message,
				metadata
				) VALUES (?,?,?,?,?,CAST(? AS JSONB))
				""", patientOrderEventId, patientOrderEventTypeId, patientOrderId, accountId, message, metadataJson);

		return patientOrderEventId;
	}

	@Nonnull
	protected Gson createGson() {
		GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();

		gsonBuilder.registerTypeAdapter(LocalDate.class, new JsonDeserializer<LocalDate>() {
			@Override
			@Nullable
			public LocalDate deserialize(@Nullable JsonElement json,
																	 @Nonnull Type type,
																	 @Nonnull JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
				requireNonNull(type);
				requireNonNull(jsonDeserializationContext);

				if (json == null)
					return null;

				JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();

				if (jsonPrimitive.isString()) {
					String string = trimToNull(json.getAsString());
					return string == null ? null : LocalDate.parse(string);
				}

				throw new IllegalArgumentException(format("Unable to convert JSON value '%s' to %s", json, type));
			}
		});

		gsonBuilder.registerTypeAdapter(LocalDate.class, new JsonSerializer<LocalDate>() {
			@Override
			@Nullable
			public JsonElement serialize(@Nullable LocalDate localDate,
																	 @Nonnull Type type,
																	 @Nonnull JsonSerializationContext jsonSerializationContext) {
				requireNonNull(type);
				requireNonNull(jsonSerializationContext);

				return localDate == null ? null : new JsonPrimitive(localDate.toString());
			}
		});

		return gsonBuilder.create();
	}

	/**
	 * Breaks an order import name/ID string encoding into its component parts.
	 * <p>
	 * Examples:
	 * <p>
	 * billingProviderName="ROBINSON, LAURA E [R11853]" -> "ROBINSON, LAURA E", "R11853"
	 * diagnosis="Anxiety state [208252]" -> "Anxiety state", "208252"
	 */
	@ThreadSafe
	protected static class NameWithEmbeddedId {
		@Nonnull
		private static final Pattern PATTERN;

		@Nonnull
		private final String originalName;
		@Nonnull
		private final String name;
		@Nullable
		private final String id;

		static {
			// Finds "[...]" at the end of the string
			PATTERN = Pattern.compile("\\[(.*?)\\]$", Pattern.CASE_INSENSITIVE);
		}

		public NameWithEmbeddedId(@Nonnull String originalName) {
			requireNonNull(originalName);

			originalName = originalName.trim();

			String name = originalName;
			String id = null;

			Matcher matcher = PATTERN.matcher(originalName);
			boolean matchFound = matcher.find();

			if (matchFound) {
				name = name.replaceAll("\\[(.*?)\\]$", "").trim();
				id = matcher.group().replace("[", "").replace("]", "");
			}

			this.originalName = originalName;
			this.name = name;
			this.id = id;
		}

		@Nonnull
		public String getOriginalName() {
			return this.originalName;
		}

		@Nonnull
		public String getName() {
			return this.name;
		}

		@Nonnull
		public Optional<String> getId() {
			return Optional.ofNullable(this.id);
		}
	}

	/**
	 * Might be encoded as names + bracketed IDs in CSV like this (a single field with newlines):
	 * "GAD (generalized anxiety disorder) [213881]
	 * Smoker [283397]
	 * Alcohol abuse [155739]"
	 */
	@Nonnull
	protected List<NameWithEmbeddedId> parseNamesWithEmbeddedIds(@Nullable String csvRecord) {
		csvRecord = trimToNull(csvRecord);

		if (csvRecord == null)
			return List.of();

		List<NameWithEmbeddedId> namesWithEmbeddedIds = new ArrayList<>();


		if (csvRecord.contains("\n")) {
			for (String csvRecordLine : csvRecord.split("\n")) {
				csvRecordLine = csvRecordLine.trim();

				if (csvRecordLine.length() > 0)
					namesWithEmbeddedIds.add(new NameWithEmbeddedId(csvRecordLine));
			}
		} else {
			namesWithEmbeddedIds.add(new NameWithEmbeddedId(csvRecord));
		}

		return namesWithEmbeddedIds;
	}

	@ThreadSafe
	protected static class BackgroundTask implements Runnable {
		@Nonnull
		private final InstitutionService institutionService;
		@Nonnull
		private final PatientOrderService patientOrderService;
		@Nonnull
		private final SystemService systemService;
		@Nonnull
		private final CurrentContextExecutor currentContextExecutor;
		@Nonnull
		private final ErrorReporter errorReporter;
		@Nonnull
		private final Database database;
		@Nonnull
		private final Logger logger;

		@Inject
		public BackgroundTask(@Nonnull InstitutionService institutionService,
													@Nonnull PatientOrderService patientOrderService,
													@Nonnull SystemService systemService,
													@Nonnull CurrentContextExecutor currentContextExecutor,
													@Nonnull ErrorReporter errorReporter,
													@Nonnull Database database) {
			requireNonNull(institutionService);
			requireNonNull(patientOrderService);
			requireNonNull(systemService);
			requireNonNull(currentContextExecutor);
			requireNonNull(errorReporter);
			requireNonNull(database);

			this.institutionService = institutionService;
			this.patientOrderService = patientOrderService;
			this.systemService = systemService;
			this.currentContextExecutor = currentContextExecutor;
			this.errorReporter = errorReporter;
			this.database = database;
			this.logger = LoggerFactory.getLogger(getClass());
		}

		@Override
		public void run() {
			List<Institution> integratedCareInstitutions = getInstitutionService().findInstitutions().stream()
					.filter(institution -> institution.getIntegratedCareEnabled())
					.collect(Collectors.toList());

			for (Institution institution : integratedCareInstitutions) {
				CurrentContext currentContext = new CurrentContext.Builder(institution.getInstitutionId(), institution.getLocale(), institution.getTimeZone()).build();

				getCurrentContextExecutor().execute(currentContext, () -> {
					try {
						getDatabase().transaction(() -> {
							getSystemService().performAdvisoryLockOperationIfAvailable(AdvisoryLock.PATIENT_ORDER_BACKGROUND_TASK, () -> {
								performBackgroundProcessingForInstitution(institution);
							});
						});
					} catch (Exception e) {
						getLogger().error(format("An error occurred while performing patient order background task for institution ID %s", institution.getInstitutionId()), e);
						getErrorReporter().report(e);
					}
				});
			}
		}

		protected void performBackgroundProcessingForInstitution(@Nonnull Institution institution) {
			requireNonNull(institution);

			LocalDateTime now = LocalDateTime.now(institution.getTimeZone());

			// 1. "closed" -> "archived": if episode_closed_at >= 30 days ago, it's moved to ARCHIVED disposition
			LocalDateTime archivedThreshold = now.minusDays(30);

			List<PatientOrder> archivablePatientOrders = getDatabase().queryForList("""
					     SELECT *
					     FROM v_patient_order
					     WHERE institution_id=?
					     AND patient_order_disposition_id=?
					     AND episode_closed_at <= ?
					""", PatientOrder.class, institution.getInstitutionId(), PatientOrderDispositionId.CLOSED, archivedThreshold);

			for (PatientOrder archivablePatientOrder : archivablePatientOrders) {
				getLogger().info("Detected that patient order ID {} was closed on {} - archiving...",
						archivablePatientOrder.getPatientOrderId(), archivablePatientOrder.getEpisodeClosedAt());
				getPatientOrderService().archivePatientOrder(new ArchivePatientOrderRequest() {{
					setPatientOrderId(archivablePatientOrder.getPatientOrderId());
				}});
			}

			// 2. "outreach needed": need another outreach to patient because it's been
			//   institution.integrated_care_outreach_followup_day_offset days since most_recent_outreach_date_time and order is still in NEEDS_ASSESSMENT state
			// TODO: revisit this, do we need to take scheduled messages into account?
			List<PatientOrder> outreachNeededPatientOrders = getDatabase().queryForList("""
							     SELECT *
							     FROM v_patient_order
							     WHERE institution_id=?
							     AND patient_order_disposition_id=?
							     AND patient_order_triage_status_id=?
							     AND outreach_needed=FALSE
							     AND most_recent_outreach_date_time IS NOT NULL
							     AND most_recent_outreach_date_time <= (? - make_interval(days => ?))
							""", PatientOrder.class, institution.getInstitutionId(), PatientOrderDispositionId.OPEN, PatientOrderTriageStatusId.NEEDS_ASSESSMENT,
					now, institution.getIntegratedCareOutreachFollowupDayOffset());

			// Syntax reference:
			// make_interval(years int DEFAULT 0, months int DEFAULT 0, weeks int DEFAULT 0, days int DEFAULT 0, hours int DEFAULT 0, mins int DEFAULT 0, secs double precision DEFAULT 0.0)

			for (PatientOrder outreachNeededPatientOrder : outreachNeededPatientOrders) {
				getLogger().info("Detected that patient order ID {} needs outreach, since unassessed and last outreach was {}...",
						outreachNeededPatientOrder.getPatientOrderId(), outreachNeededPatientOrder.getMostRecentOutreachDateTime());
				getPatientOrderService().updatePatientOrderOutreachNeeded(new UpdatePatientOrderOutreachNeededRequest() {{
					setPatientOrderId(outreachNeededPatientOrder.getPatientOrderId());
					setOutreachNeeded(true);
				}});
			}

			// 3. "followup needed": need to follow up with patient because it's been
			//   institution.integrated_care_sent_resources_followup_week_offset + institution.integrated_care_sent_resources_followup_day_offset
			//   since resources_sent_at.  This applies to both open and closed orders, but not archived.
			// TODO: how do we mark "followup no longer needed", is there a UI to flip back to false?
			List<PatientOrder> followupNeededPatientOrders = getDatabase().queryForList("""
							     SELECT *
							     FROM v_patient_order
							     WHERE institution_id=?
							     AND followup_needed=FALSE
							     AND resources_sent_at IS NOT NULL
							     AND resources_sent_at <= (? - make_interval(weeks => ?, days => ?))
							""", PatientOrder.class, institution.getInstitutionId(),
					now, institution.getIntegratedCareSentResourcesFollowupWeekOffset(), institution.getIntegratedCareSentResourcesFollowupDayOffset());

			for (PatientOrder followupNeededPatientOrder : followupNeededPatientOrders) {
				getLogger().info("Detected that patient order ID {} needs followup, since resources were sent at {}...",
						followupNeededPatientOrder.getPatientOrderId(), followupNeededPatientOrder.getResourcesSentAt());
				getPatientOrderService().updatePatientOrderFollowupNeeded(new UpdatePatientOrderFollowupNeededRequest() {{
					setPatientOrderId(followupNeededPatientOrder.getPatientOrderId());
					setFollowupNeeded(true);
				}});
			}
		}

		@Nonnull
		protected InstitutionService getInstitutionService() {
			return this.institutionService;
		}

		@Nonnull
		protected PatientOrderService getPatientOrderService() {
			return this.patientOrderService;
		}

		@Nonnull
		protected SystemService getSystemService() {
			return this.systemService;
		}

		@Nonnull
		protected CurrentContextExecutor getCurrentContextExecutor() {
			return this.currentContextExecutor;
		}

		@Nonnull
		protected ErrorReporter getErrorReporter() {
			return this.errorReporter;
		}

		@Nonnull
		protected Database getDatabase() {
			return this.database;
		}

		@Nonnull
		protected Logger getLogger() {
			return this.logger;
		}
	}

	@NotThreadSafe
	protected static class PatientOrderWithTotalCount extends PatientOrder {
		@Nullable
		private Integer totalCount;

		@Nullable
		public Integer getTotalCount() {
			return this.totalCount;
		}

		public void setTotalCount(@Nullable Integer totalCount) {
			this.totalCount = totalCount;
		}
	}

	@NotThreadSafe
	protected static class CsvName {
		@Nullable
		private final String lastName;
		@Nullable
		private final String firstName;
		@Nullable
		private final String middleName;

		public CsvName(@Nullable String name) {
			this(name, null);
		}

		public CsvName(@Nullable String name,
									 @Nullable Locale locale) {
			if (locale == null)
				locale = Locale.US;

			name = trimToNull(name);

			String lastName = null;
			String firstName = null;
			String middleName = null;

			if (name != null) {
				// Names should look like "Lastname, Firstname [optional middle initial]"...
				// but in case there is no comma, assume it's Firstname Lastname
				int commaIndex = name.indexOf(",");
				if (commaIndex != -1) {
					lastName = name.substring(0, commaIndex).trim();
					String remainder = name.substring(commaIndex + 1).trim();

					List<String> remainingNameComponents = Arrays.stream(remainder.replaceAll("\\s{2,}", " ").split(" ")).toList();

					if (remainingNameComponents.size() == 1) {
						firstName = remainingNameComponents.get(0);
					} else if (remainingNameComponents.size() > 1) {
						firstName = remainingNameComponents.subList(0, remainingNameComponents.size() - 1).stream().collect(Collectors.joining(" "));
						middleName = remainingNameComponents.get(remainingNameComponents.size() - 1);
					}
				} else {
					List<String> nameComponents = Arrays.stream(name.replaceAll("\\s{2,}", " ").split(" ")).toList();

					if (nameComponents.size() > 0) {
						firstName = nameComponents.get(0);
						lastName = nameComponents.subList(1, nameComponents.size()).stream().collect(Collectors.joining(" "));
					}
				}
			}

			if (firstName != null && firstName.equals(firstName.toUpperCase(locale)))
				firstName = Normalizer.normalizeNameCasing(firstName, locale).orElse(null);

			if (lastName != null && lastName.equals(lastName.toUpperCase(locale)))
				lastName = Normalizer.normalizeNameCasing(lastName, locale).orElse(null);

			if (middleName != null)
				middleName = Normalizer.normalizeNameCasing(middleName, locale).orElse(null);

			this.lastName = lastName;
			this.firstName = firstName;
			this.middleName = middleName;
		}

		@Nonnull
		public Optional<String> getLastName() {
			return Optional.ofNullable(this.lastName);
		}

		@Nonnull
		public Optional<String> getFirstName() {
			return Optional.ofNullable(this.firstName);
		}

		@Nonnull
		public Optional<String> getMiddleName() {
			return Optional.ofNullable(this.middleName);
		}
	}

	@NotThreadSafe
	protected static class AccountIdWithCount {
		@Nullable
		private UUID accountId;
		@Nullable
		private Integer count;

		@Nullable
		public UUID getAccountId() {
			return this.accountId;
		}

		public void setAccountId(@Nullable UUID accountId) {
			this.accountId = accountId;
		}

		@Nullable
		public Integer getCount() {
			return this.count;
		}

		public void setCount(@Nullable Integer count) {
			this.count = count;
		}
	}

	@Nonnull
	protected Long getBackgroundTaskInitialDelayInSeconds() {
		return BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS;
	}

	@Nonnull
	protected Long getBackgroundTaskIntervalInSeconds() {
		return BACKGROUND_TASK_INTERVAL_IN_SECONDS;
	}

	@Nonnull
	protected AddressService getAddressService() {
		return this.addressServiceProvider.get();
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountServiceProvider.get();
	}

	@Nonnull
	protected MessageService getMessageService() {
		return this.messageServiceProvider.get();
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
	}

	@Nonnull
	protected PatientOrderScheduledMessageGroupApiResponseFactory getPatientOrderScheduledMessageGroupApiResponseFactory() {
		return this.patientOrderScheduledMessageGroupApiResponseFactory;
	}


	@Nonnull
	protected Provider<BackgroundTask> getBackgroundTaskProvider() {
		return this.backgroundTaskProvider;
	}

	@Nonnull
	protected ReentrantLock getBackgroundTaskLock() {
		return this.backgroundTaskLock;
	}

	@Nonnull
	public Boolean isBackgroundTaskStarted() {
		getBackgroundTaskLock().lock();

		try {
			return this.backgroundTaskStarted;
		} finally {
			getBackgroundTaskLock().unlock();
		}
	}

	@Nonnull
	protected Optional<ScheduledExecutorService> getBackgroundTaskExecutorService() {
		return Optional.ofNullable(this.backgroundTaskExecutorService);
	}

	@Nonnull
	protected Database getDatabase() {
		return this.database;
	}

	@Nonnull
	protected Normalizer getNormalizer() {
		return this.normalizer;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return this.formatter;
	}

	@Nonnull
	protected Authenticator getAuthenticator() {
		return this.authenticator;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected Gson getGson() {
		return this.gson;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
