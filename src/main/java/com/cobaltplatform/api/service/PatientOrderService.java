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
import com.cobaltplatform.api.model.api.request.ClosePatientOrderRequest;
import com.cobaltplatform.api.model.api.request.CreateAddressRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderEventRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderImportRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderNoteRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderOutreachRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderRequest.CreatePatientOrderDiagnosisRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderRequest.CreatePatientOrderMedicationRequest;
import com.cobaltplatform.api.model.api.request.DeletePatientOrderNoteRequest;
import com.cobaltplatform.api.model.api.request.DeletePatientOrderOutreachRequest;
import com.cobaltplatform.api.model.api.request.FindPatientOrdersRequest;
import com.cobaltplatform.api.model.api.request.OpenPatientOrderRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderNoteRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderOutreachRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderTriagesRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderTriagesRequest.CreatePatientOrderTriageRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.BirthSex.BirthSexId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.PatientOrderClosureReason;
import com.cobaltplatform.api.model.db.PatientOrderClosureReason.PatientOrderClosureReasonId;
import com.cobaltplatform.api.model.db.PatientOrderDiagnosis;
import com.cobaltplatform.api.model.db.PatientOrderEventType.PatientOrderEventTypeId;
import com.cobaltplatform.api.model.db.PatientOrderImport;
import com.cobaltplatform.api.model.db.PatientOrderImportType.PatientOrderImportTypeId;
import com.cobaltplatform.api.model.db.PatientOrderMedication;
import com.cobaltplatform.api.model.db.PatientOrderNote;
import com.cobaltplatform.api.model.db.PatientOrderOutreach;
import com.cobaltplatform.api.model.db.PatientOrderScreeningStatus.PatientOrderScreeningStatusId;
import com.cobaltplatform.api.model.db.PatientOrderStatus.PatientOrderStatusId;
import com.cobaltplatform.api.model.db.PatientOrderTriage;
import com.cobaltplatform.api.model.db.PatientOrderTriageSource.PatientOrderTriageSourceId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.model.service.IcTestPatientEmailAddress;
import com.cobaltplatform.api.model.service.PatientOrderImportResult;
import com.cobaltplatform.api.model.service.PatientOrderPanelTypeId;
import com.cobaltplatform.api.util.Authenticator;
import com.cobaltplatform.api.util.Normalizer;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlVaragsParameters;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class PatientOrderService {
	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final Provider<AddressService> addressServiceProvider;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Normalizer normalizer;
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

	@Inject
	public PatientOrderService(@Nonnull Provider<AddressService> addressServiceProvider,
														 @Nonnull Provider<AccountService> accountServiceProvider,
														 @Nonnull Database database,
														 @Nonnull Normalizer normalizer,
														 @Nonnull Authenticator authenticator,
														 @Nonnull Configuration configuration,
														 @Nonnull Strings strings) {
		requireNonNull(addressServiceProvider);
		requireNonNull(accountServiceProvider);
		requireNonNull(database);
		requireNonNull(normalizer);
		requireNonNull(authenticator);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.addressServiceProvider = addressServiceProvider;
		this.accountServiceProvider = accountServiceProvider;
		this.database = database;
		this.normalizer = normalizer;
		this.authenticator = authenticator;
		this.configuration = configuration;
		this.gson = createGson();
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<PatientOrder> findPatientOrderById(@Nullable UUID patientOrderId) {
		if (patientOrderId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM patient_order 
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
				FROM patient_order
				WHERE patient_account_id=?
				AND patient_order_status_id != ?
				ORDER BY order_date DESC, order_age_in_minutes
				""", PatientOrder.class, accountId, PatientOrderStatusId.DELETED);
	}

	@Nonnull
	public List<PatientOrder> findPatientOrdersByPatientOrderImportId(@Nullable UUID patientOrderImportId) {
		if (patientOrderImportId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM patient_order
				WHERE patient_order_import_id=?
				AND patient_order_status_id != ?
				ORDER BY order_date DESC, order_age_in_minutes
				""", PatientOrder.class, patientOrderImportId, PatientOrderStatusId.DELETED);
	}

	@Nonnull
	public List<PatientOrder> findPatientOrdersByMrnAndInstitutionId(@Nullable String patientMrn,
																																	 @Nullable InstitutionId institutionId) {
		patientMrn = trimToNull(patientMrn);

		if (patientMrn == null || institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM patient_order
				WHERE UPPER(?)=UPPER(patient_mrn) 
				AND institution_id=?
				AND patient_order_status_id != ?
				ORDER BY order_date DESC, order_age_in_minutes    
				""", PatientOrder.class, patientMrn, institutionId, PatientOrderStatusId.DELETED);
	}

	@Nonnull
	public Optional<PatientOrder> findOpenPatientOrderByMrnAndInstitutionId(@Nullable String patientMrn,
																																					@Nullable InstitutionId institutionId) {
		patientMrn = trimToNull(patientMrn);

		if (patientMrn == null || institutionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM patient_order
				WHERE UPPER(?)=UPPER(patient_mrn)
				AND institution_id=?
				AND patient_order_status_id=?
				""", PatientOrder.class, patientMrn, institutionId, PatientOrderStatusId.OPEN);
	}

	@Nonnull
	public Optional<PatientOrder> findOpenPatientOrderByPatientAccountId(@Nullable UUID patientAccountId) {
		if (patientAccountId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM patient_order
				WHERE patient_account_id=?
				AND patient_order_status_id=?
				""", PatientOrder.class, patientAccountId, PatientOrderStatusId.OPEN);
	}

	@Nonnull
	public Optional<PatientOrder> findPatientOrderByScreeningSessionId(@Nullable UUID screeningSessionId) {
		if (screeningSessionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT po.*
				FROM patient_order po, patient_order_screening_session poss
				WHERE po.patient_order_id=poss.patient_order_id
				AND poss.screening_session_id=?
				""", PatientOrder.class, screeningSessionId);
	}

	@Nonnull
	public List<PatientOrder> findPatientOrdersByTestPatientEmailAddressAndInstitutionId(@Nullable IcTestPatientEmailAddress icTestPatientEmailAddress,
																																											 @Nullable InstitutionId institutionId) {
		if (icTestPatientEmailAddress == null || institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM patient_order
				WHERE UPPER(?)=UPPER(test_patient_email_address)
				AND institution_id=?
				AND patient_order_status_id != ?
				ORDER BY order_date DESC, order_age_in_minutes    
				""", PatientOrder.class, icTestPatientEmailAddress.getEmailAddress(), institutionId, PatientOrderStatusId.DELETED);
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
	public Integer findActivePatientOrderCountByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return 0;

		return getDatabase().queryForObject("""
				SELECT COUNT(*)
				FROM patient_order
				WHERE institution_id=?
				AND patient_order_status_id NOT IN (?,?)
				""", Integer.class, institutionId, PatientOrderStatusId.ARCHIVED, PatientOrderStatusId.DELETED).get();
	}

	@Nonnull
	public Integer findActivePatientOrderCountByPanelAccountIdForInstitutionId(@Nullable UUID panelAccountId,
																																						 @Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return 0;

		if (panelAccountId == null)
			return findActivePatientOrderCountByInstitutionId(institutionId);

		return getDatabase().queryForObject("""
				SELECT COUNT(*)
				FROM patient_order
				WHERE panel_account_id=?
				AND institution_id=?		
				AND patient_order_status_id NOT IN (?,?)
				""", Integer.class, panelAccountId, institutionId, PatientOrderStatusId.ARCHIVED, PatientOrderStatusId.DELETED).get();
	}

	@Nonnull
	public Map<UUID, Integer> findActivePatientOrderCountsByPanelAccountIdForInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Map.of();

		List<AccountIdWithCount> accountIdsWithCount = getDatabase().queryForList("""
				SELECT a.account_id, COUNT(po.*)
				FROM account a, patient_order po
				WHERE a.account_id=po.panel_account_id
				AND po.institution_id=?
				AND po.patient_order_status_id NOT IN (?,?)
				GROUP BY a.account_id
				""", AccountIdWithCount.class, institutionId, PatientOrderStatusId.ARCHIVED, PatientOrderStatusId.DELETED);

		return accountIdsWithCount.stream()
				.collect(Collectors.toMap(AccountIdWithCount::getAccountId, AccountIdWithCount::getCount));
	}

	@Nonnull
	public Map<PatientOrderStatusId, Integer> findActivePatientOrderCountsByPatientOrderStatusIdForPanelAccountIdAndInstitutionId(@Nullable UUID panelAccountId,
																																																																@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Map.of();

		Map<PatientOrderStatusId, Integer> activePatientOrderCountsByPatientOrderStatusId = new HashMap<>();

		// Prime the map with zeroes
		for (PatientOrderStatusId patientOrderStatusId : PatientOrderStatusId.values())
			activePatientOrderCountsByPatientOrderStatusId.put(patientOrderStatusId, 0);

		List<PatientOrderWithTotalCount> patientOrders = List.of();

		if (panelAccountId == null) {
			patientOrders = getDatabase().queryForList("""
					SELECT patient_order_status_id, COUNT(*) as total_count
					FROM patient_order
					WHERE institution_id=?
					AND patient_order_status_id NOT IN (?,?)
					GROUP BY patient_order_status_id
					""", PatientOrderWithTotalCount.class, institutionId, PatientOrderStatusId.ARCHIVED, PatientOrderStatusId.DELETED);
		} else {
			patientOrders = getDatabase().queryForList("""
					SELECT patient_order_status_id, COUNT(*) as total_count
					FROM patient_order
					WHERE panel_account_id=?
					AND institution_id=?
					AND patient_order_status_id NOT IN (?,?)
					GROUP BY patient_order_status_id
						""", PatientOrderWithTotalCount.class, panelAccountId, institutionId, PatientOrderStatusId.ARCHIVED, PatientOrderStatusId.DELETED);
		}

		// Add on counts
		for (PatientOrderWithTotalCount patientOrder : patientOrders)
			activePatientOrderCountsByPatientOrderStatusId.put(patientOrder.getPatientOrderStatusId(), patientOrder.getTotalCount());

		return activePatientOrderCountsByPatientOrderStatusId;
	}

	@Nonnull
	public Boolean assignPatientOrderToPanelAccount(@Nonnull UUID patientOrderId,
																									@Nullable UUID panelAccountId, // Null panel account removes the order from the panel
																									@Nullable UUID assigningAccountId) {
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
					setAccountId(assigningAccountId);
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
		PatientOrderPanelTypeId patientOrderPanelTypeId = request.getPatientOrderPanelTypeId();
		UUID panelAccountId = request.getPanelAccountId();
		String searchQuery = trimToNull(request.getSearchQuery());
		Integer pageNumber = request.getPageNumber();
		Integer pageSize = request.getPageSize();

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
		List<Object> parameters = new ArrayList<>();

		parameters.add(institutionId);
		parameters.add(PatientOrderStatusId.DELETED);

		if (panelAccountId != null) {
			whereClauseLines.add("AND po.panel_account_id=?");
			parameters.add(panelAccountId);
		}

		// TODO: finish adding other parameters/filters

		parameters.add(limit);
		parameters.add(offset);

		// TODO: handle rest of query criteria

		String sql = """
				  WITH base_query AS (
				  SELECT po.*
				  FROM patient_order po
				  WHERE po.institution_id=?
				  AND po.patient_order_status_id != ?
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
				  LIMIT ?
				  OFFSET ?
				""".trim()
				.replace("{{whereClauseLines}}", whereClauseLines.stream().collect(Collectors.joining("\n")));

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

		if (screeningSessionId != null) {
			sql += " AND screening_session_id=?";
			parameters.add(screeningSessionId);
		}

		sql += "ORDER BY display_order";

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
		if (patientOrder.getPatientOrderStatusId() == PatientOrderStatusId.OPEN)
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
				SET patient_order_status_id=?, patient_order_closure_reason_id=?
				WHERE patient_order_id=?
				""", PatientOrderStatusId.OPEN, PatientOrderClosureReasonId.NOT_CLOSED, patientOrderId);

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("oldPatientOrderStatusId", patientOrder.getPatientOrderStatusId());
		metadata.put("newPatientOrderStatusId", PatientOrderStatusId.OPEN);

		createPatientOrderEvent(new CreatePatientOrderEventRequest() {
			{
				setPatientOrderEventTypeId(PatientOrderEventTypeId.STATUS_CHANGED);
				setPatientOrderId(patientOrderId);
				setAccountId(accountId);
				setMessage("Order opened."); // Not localized on the way in
				setMetadata(metadata);
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

		// If we're already not open, nothing to do
		if (patientOrder.getPatientOrderStatusId() != PatientOrderStatusId.OPEN)
			return false;

		getDatabase().execute("""
				UPDATE patient_order
				SET patient_order_status_id=?, patient_order_closure_reason_id=?
				WHERE patient_order_id=?
				""", PatientOrderStatusId.CLOSED, patientOrderClosureReasonId, patientOrderId);

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("oldPatientOrderStatusId", patientOrder.getPatientOrderStatusId());
		metadata.put("newPatientOrderStatusId", PatientOrderStatusId.CLOSED);
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

			if (account == null)
				validationException.add(new FieldError("patientOrderId", getStrings().get("Patient Order ID is invalid.")));
		}

		if (patientOrderTriageSourceId == null) {
			validationException.add(new FieldError("patientOrderTriageSourceId", getStrings().get("Patient Order Triage Source ID is required.")));
		} else {
			if (patientOrderTriageSourceId == PatientOrderTriageSourceId.COBALT && screeningSessionId == null)
				validationException.add(new FieldError("screeningSessionId", getStrings().get("Screening session ID is required.")));
		}

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

		return patientOrderTriageIds;
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
	public UUID createPatientOrderOutreach(@Nonnull CreatePatientOrderOutreachRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		UUID patientOrderId = request.getPatientOrderId();
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
				patient_order_outreach_id, patient_order_id, account_id, note, outreach_date_time
				) VALUES (?,?,?,?,?)
				""", patientOrderOutreachId, patientOrderId, accountId, note, outreachDateTime);

		createPatientOrderEvent(new CreatePatientOrderEventRequest() {{
			setPatientOrderEventTypeId(PatientOrderEventTypeId.OUTREACH_CREATED);
			setPatientOrderId(patientOrderId);
			setAccountId(accountId);
			setMessage("Created outreach."); // Not localized on the way in
			setMetadata(Map.of(
					"patientOrderOutreachId", patientOrderOutreachId,
					"accountId", accountId,
					"outreachDateTime", outreachDateTime,
					"note", note));
		}});

		return patientOrderOutreachId;
	}

	@Nonnull
	public Boolean updatePatientOrderOutreach(@Nonnull UpdatePatientOrderOutreachRequest request) {
		requireNonNull(request);

		UUID accountId = request.getAccountId();
		UUID patientOrderOutreachId = request.getPatientOrderOutreachId();
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
				SET note=?, outreach_date_time=?
				WHERE patient_order_outreach_id=?
				""", note, outreachDateTime, patientOrderOutreachId) > 0;

		if (updated) {
			UUID patientOrderId = patientOrderOutreach.getPatientOrderId();
			String oldNote = patientOrderOutreach.getNote();
			LocalDateTime oldOutreachDateTime = patientOrderOutreach.getOutreachDateTime();

			createPatientOrderEvent(new CreatePatientOrderEventRequest() {{
				setPatientOrderEventTypeId(PatientOrderEventTypeId.OUTREACH_UPDATED);
				setPatientOrderId(patientOrderId);
				setAccountId(accountId);
				setMessage("Updated outreach."); // Not localized on the way in
				setMetadata(Map.of(
						"patientOrderOutreachId", patientOrderOutreachId,
						"accountId", accountId,
						"oldNote", oldNote,
						"newNote", note,
						"oldOutreachDateTime", oldOutreachDateTime,
						"newOutreachDateTime", outreachDateTime));
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
	public PatientOrderImportResult createPatientOrderImport(@Nonnull CreatePatientOrderImportRequest request) {
		requireNonNull(request);

		InstitutionId institutionId = request.getInstitutionId();
		PatientOrderImportTypeId patientOrderImportTypeId = request.getPatientOrderImportTypeId();
		UUID accountId = request.getAccountId();
		String csvContent = trimToNull(request.getCsvContent());
		boolean automaticallyAssignToPanelAccounts = request.getAutomaticallyAssignToPanelAccounts() == null ? false : request.getAutomaticallyAssignToPanelAccounts();
		UUID patientOrderImportId = UUID.randomUUID();
		List<UUID> patientOrderIds = new ArrayList<>();
		ValidationException validationException = new ValidationException();

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (patientOrderImportTypeId == null)
			validationException.add(new FieldError("patientOrderImportTypeId", getStrings().get("Patient Order Import Type ID is required.")));

		if (patientOrderImportTypeId == PatientOrderImportTypeId.CSV && csvContent == null)
			validationException.add(new FieldError("csvContent", getStrings().get("CSV file is required.")));

		if (patientOrderImportTypeId == PatientOrderImportTypeId.CSV && accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		// TODO: revisit when we support EPIC imports directly
		if (patientOrderImportTypeId != PatientOrderImportTypeId.CSV)
			throw new IllegalArgumentException(format("We do not yet support %s.%s", PatientOrderImportTypeId.class.getSimpleName(),
					patientOrderImportTypeId.name()));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				INSERT INTO patient_order_import (
				patient_order_import_id,
				patient_order_import_type_id,
				institution_id,
				account_id,
				raw_order
				) VALUES (?,?,?,?,?)
				""", patientOrderImportId, patientOrderImportTypeId, institutionId, accountId, csvContent);

		if (patientOrderImportTypeId == PatientOrderImportTypeId.CSV) {
			Map<Integer, ValidationException> validationExceptionsByRowNumber = new HashMap<>();
			int rowNumber = 0;

			// If first column header is "Test Patient Email Address", then this is a test file
			boolean containsTestPatientData = csvContent.startsWith("Test Patient Email Address");

			if (containsTestPatientData && !getConfiguration().getShouldEnableIcDebugging())
				throw new IllegalStateException("Cannot upload test patient data in this environment.");

			// Pull data from the CSV
			try (Reader reader = new StringReader(csvContent)) {
				for (CSVRecord record : CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
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

					patientOrderRequest.setOrderDate(trimToNull(record.get("Order Date")));
					patientOrderRequest.setOrderId(trimToNull(record.get("Order ID")));
					patientOrderRequest.setOrderAge(trimToNull(record.get("Age of Order")));
					patientOrderRequest.setRouting(trimToNull(record.get("CCBH Order Routing")));
					patientOrderRequest.setReasonForReferral(trimToNull(record.get("Reasons for Referral")));

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
					patientOrderRequest.setCallbackPhoneNumber(trimToNull(record.get("Call Back Number")));
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

		return new PatientOrderImportResult(patientOrderImportId, patientOrderIds);
	}

	@Nonnull
	public UUID createPatientOrder(@Nonnull CreatePatientOrderRequest request) {
		requireNonNull(request);

		PatientOrderStatusId patientOrderStatusId = PatientOrderStatusId.OPEN;
		PatientOrderScreeningStatusId patientOrderScreeningStatusId = PatientOrderScreeningStatusId.NOT_SCREENED;
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
		String reasonForReferral = trimToNull(request.getReasonForReferral());
		List<CreatePatientOrderDiagnosisRequest> diagnoses = request.getDiagnoses() == null ? List.of() : request.getDiagnoses().stream()
				.filter(diagnosis -> diagnosis != null)
				.collect(Collectors.toList());
		String associatedDiagnosis = trimToNull(request.getAssociatedDiagnosis());
		String callbackPhoneNumber = trimToNull(request.getCallbackPhoneNumber());
		String preferredContactHours = trimToNull(request.getPreferredContactHours());
		String comments = trimToNull(request.getComments());
		String ccRecipients = trimToNull(request.getCcRecipients());
		String lastActiveMedicationOrderSummary = trimToNull(request.getLastActiveMedicationOrderSummary());
		List<CreatePatientOrderMedicationRequest> medications = request.getMedications() == null ? List.of() : request.getMedications().stream()
				.filter(medication -> medication != null)
				.collect(Collectors.toList());
		String recentPsychotherapeuticMedications = trimToNull(request.getRecentPsychotherapeuticMedications());
		String testPatientEmailAddress = trimToNull(request.getTestPatientEmailAddress());
		String testPatientPassword = trimToNull(request.getTestPatientPassword());
		UUID patientOrderId = UUID.randomUUID();
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

		if (callbackPhoneNumber != null) {
			String originalCallbackPhoneNumber = callbackPhoneNumber;
			callbackPhoneNumber = getNormalizer().normalizePhoneNumberToE164(callbackPhoneNumber, Locale.US).orElse(null);

			if (callbackPhoneNumber == null)
				validationException.add(new FieldError("callbackPhoneNumber", getStrings().get("Invalid callback phone number: {{callbackPhoneNumber}}.",
						Map.of("callbackPhoneNumber", originalCallbackPhoneNumber))));
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

		if (validationException.hasErrors())
			throw validationException;

		String hashedTestPatientPassword = testPatientPassword == null ? null : getAuthenticator().hashPassword(testPatientPassword);

		getDatabase().execute("""
						  INSERT INTO patient_order (
						  patient_order_id,
						  patient_order_status_id,
						  patient_order_screening_status_id,
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
						  reason_for_referral,
						  associated_diagnosis,
						  callback_phone_number,
						  preferred_contact_hours,
						  comments,
						  cc_recipients,
						  last_active_medication_order_summary,						  
						  recent_psychotherapeutic_medications,
						  test_patient_email_address,
						  test_patient_password
						) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
						""",
				patientOrderId, patientOrderStatusId, patientOrderScreeningStatusId, patientOrderImportId, institutionId, encounterDepartmentId,
				encounterDepartmentIdType, encounterDepartmentName, referringPracticeId, referringPracticeIdType,
				referringPracticeName, orderingProviderId, orderingProviderIdType, orderingProviderLastName,
				orderingProviderFirstName, orderingProviderMiddleName, billingProviderId, billingProviderIdType,
				billingProviderLastName, billingProviderFirstName, billingProviderMiddleName, patientLastName, patientFirstName,
				patientMrn, patientId, patientIdType, patientBirthSexId, patientBirthdate, patientAddressId, primaryPayorId,
				primaryPayorName, primaryPlanId, primaryPlanName, orderDate, orderAgeInMinutes, orderId, routing,
				reasonForReferral, associatedDiagnosis, callbackPhoneNumber, preferredContactHours, comments, ccRecipients,
				lastActiveMedicationOrderSummary, recentPsychotherapeuticMedications, testPatientEmailAddress, hashedTestPatientPassword);

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
					""", patientOrderId, medicationId, medicationIdType, medicationName, diagnosisDisplayOrder);

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
	protected AddressService getAddressService() {
		return this.addressServiceProvider.get();
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountServiceProvider.get();
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
