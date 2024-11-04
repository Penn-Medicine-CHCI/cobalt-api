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
import com.cobaltplatform.api.model.api.request.AssignPatientOrdersRequest;
import com.cobaltplatform.api.model.api.request.CancelPatientOrderScheduledOutreachRequest;
import com.cobaltplatform.api.model.api.request.CancelPatientOrderScheduledScreeningRequest;
import com.cobaltplatform.api.model.api.request.ClosePatientOrderRequest;
import com.cobaltplatform.api.model.api.request.CompletePatientOrderScheduledOutreachRequest;
import com.cobaltplatform.api.model.api.request.CompletePatientOrderVoicemailTaskRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderImportRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderNoteRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderOutreachRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderScheduledMessageGroupRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderScheduledOutreachRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderScheduledScreeningRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderTriageGroupRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderVoicemailTaskRequest;
import com.cobaltplatform.api.model.api.request.DeletePatientOrderNoteRequest;
import com.cobaltplatform.api.model.api.request.DeletePatientOrderOutreachRequest;
import com.cobaltplatform.api.model.api.request.DeletePatientOrderScheduledMessageGroupRequest;
import com.cobaltplatform.api.model.api.request.DeletePatientOrderVoicemailTaskRequest;
import com.cobaltplatform.api.model.api.request.FindPatientOrdersRequest;
import com.cobaltplatform.api.model.api.request.FindPatientOrdersRequest.PatientOrderSortColumnId;
import com.cobaltplatform.api.model.api.request.FindPatientOrdersRequest.PatientOrderSortRule;
import com.cobaltplatform.api.model.api.request.OpenPatientOrderRequest;
import com.cobaltplatform.api.model.api.request.PatchPatientOrderRequest;
import com.cobaltplatform.api.model.api.request.UpdateEpicDepartmentRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderConsentStatusRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderEncounterCsnRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderNoteRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderOutreachRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderResourceCheckInResponseStatusRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderResourcingStatusRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderSafetyPlanningStatusRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderScheduledMessageGroupRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderScheduledOutreachRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderScheduledScreeningRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderVoicemailTaskRequest;
import com.cobaltplatform.api.model.api.response.AccountApiResponse;
import com.cobaltplatform.api.model.api.response.AccountApiResponse.AccountApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CountryApiResponse;
import com.cobaltplatform.api.model.api.response.CountryApiResponse.CountryApiResponseFactory;
import com.cobaltplatform.api.model.api.response.EncounterApiResponse.EncounterApiResponseFactory;
import com.cobaltplatform.api.model.api.response.EpicDepartmentApiResponse.EpicDepartmentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.LanguageApiResponse;
import com.cobaltplatform.api.model.api.response.LanguageApiResponse.LanguageApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderApiResponse;
import com.cobaltplatform.api.model.api.response.PatientOrderApiResponse.PatientOrderApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderApiResponse.PatientOrderApiResponseFormat;
import com.cobaltplatform.api.model.api.response.PatientOrderApiResponse.PatientOrderApiResponseSupplement;
import com.cobaltplatform.api.model.api.response.PatientOrderAutocompleteResultApiResponse.PatientOrderAutocompleteResultApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderNoteApiResponse.PatientOrderNoteApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderOutreachApiResponse.PatientOrderOutreachApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderScheduledMessageGroupApiResponse;
import com.cobaltplatform.api.model.api.response.PatientOrderScheduledOutreachApiResponse.PatientOrderScheduledOutreachApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderScheduledScreeningApiResponse.PatientOrderScheduledScreeningApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PatientOrderVoicemailTaskApiResponse.PatientOrderVoicemailTaskApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ScreeningTypeApiResponse;
import com.cobaltplatform.api.model.api.response.ScreeningTypeApiResponse.ScreeningTypeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TimeZoneApiResponse;
import com.cobaltplatform.api.model.api.response.TimeZoneApiResponse.TimeZoneApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.BirthSex.BirthSexId;
import com.cobaltplatform.api.model.db.EpicDepartment;
import com.cobaltplatform.api.model.db.Ethnicity.EthnicityId;
import com.cobaltplatform.api.model.db.FootprintEventGroupType.FootprintEventGroupTypeId;
import com.cobaltplatform.api.model.db.GenderIdentity;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.PatientOrderClosureReason;
import com.cobaltplatform.api.model.db.PatientOrderConsentStatus.PatientOrderConsentStatusId;
import com.cobaltplatform.api.model.db.PatientOrderDisposition.PatientOrderDispositionId;
import com.cobaltplatform.api.model.db.PatientOrderImportType.PatientOrderImportTypeId;
import com.cobaltplatform.api.model.db.PatientOrderNote;
import com.cobaltplatform.api.model.db.PatientOrderOutreach;
import com.cobaltplatform.api.model.db.PatientOrderResourcingStatus.PatientOrderResourcingStatusId;
import com.cobaltplatform.api.model.db.PatientOrderSafetyPlanningStatus.PatientOrderSafetyPlanningStatusId;
import com.cobaltplatform.api.model.db.PatientOrderScheduledMessage;
import com.cobaltplatform.api.model.db.PatientOrderScheduledMessageGroup;
import com.cobaltplatform.api.model.db.PatientOrderScheduledOutreach;
import com.cobaltplatform.api.model.db.PatientOrderScheduledScreening;
import com.cobaltplatform.api.model.db.PatientOrderScreeningStatus.PatientOrderScreeningStatusId;
import com.cobaltplatform.api.model.db.PatientOrderTriageSource.PatientOrderTriageSourceId;
import com.cobaltplatform.api.model.db.PatientOrderTriageStatus.PatientOrderTriageStatusId;
import com.cobaltplatform.api.model.db.PatientOrderVoicemailTask;
import com.cobaltplatform.api.model.db.Race.RaceId;
import com.cobaltplatform.api.model.db.RawPatientOrder;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.Encounter;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.model.service.PatientOrderAssignmentStatusId;
import com.cobaltplatform.api.model.service.PatientOrderAutocompleteResult;
import com.cobaltplatform.api.model.service.PatientOrderContactTypeId;
import com.cobaltplatform.api.model.service.PatientOrderFilterFlagTypeId;
import com.cobaltplatform.api.model.service.PatientOrderImportResult;
import com.cobaltplatform.api.model.service.PatientOrderOutreachStatusId;
import com.cobaltplatform.api.model.service.PatientOrderResponseStatusId;
import com.cobaltplatform.api.model.service.PatientOrderViewTypeId;
import com.cobaltplatform.api.model.service.ReferringPractice;
import com.cobaltplatform.api.model.service.Region;
import com.cobaltplatform.api.model.service.SortDirectionId;
import com.cobaltplatform.api.model.service.SortNullsId;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.PatientOrderService;
import com.cobaltplatform.api.service.ProviderService;
import com.cobaltplatform.api.service.ScreeningService;
import com.cobaltplatform.api.service.SystemService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.PatientOrderCsvGenerator;
import com.cobaltplatform.api.util.db.ReadReplica;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.lokalized.Strings;
import com.soklet.web.annotation.DELETE;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.PATCH;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
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
public class PatientOrderResource {
	@Nonnull
	private final PatientOrderService patientOrderService;
	@Nonnull
	private final ProviderService providerService;
	@Nonnull
	private final AccountService accountService;
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final ScreeningService screeningService;
	@Nonnull
	private final SystemService systemService;
	@Nonnull
	private final PatientOrderApiResponseFactory patientOrderApiResponseFactory;
	@Nonnull
	private final PatientOrderNoteApiResponseFactory patientOrderNoteApiResponseFactory;
	@Nonnull
	private final PatientOrderOutreachApiResponseFactory patientOrderOutreachApiResponseFactory;
	@Nonnull
	private final EpicDepartmentApiResponseFactory epicDepartmentApiResponseFactory;
	@Nonnull
	private final EncounterApiResponseFactory encounterApiResponseFactory;
	@Nonnull
	private final AccountApiResponseFactory accountApiResponseFactory;
	@Nonnull
	private final TimeZoneApiResponseFactory timeZoneApiResponseFactory;
	@Nonnull
	private final LanguageApiResponseFactory languageApiResponseFactory;
	@Nonnull
	private final CountryApiResponseFactory countryApiResponseFactory;
	@Nonnull
	private final PatientOrderAutocompleteResultApiResponseFactory patientOrderAutocompleteResultApiResponseFactory;
	@Nonnull
	private final PatientOrderScheduledScreeningApiResponseFactory patientOrderScheduledScreeningApiResponseFactory;
	@Nonnull
	private final ScreeningTypeApiResponseFactory screeningTypeApiResponseFactory;
	@Nonnull
	private final PatientOrderVoicemailTaskApiResponseFactory patientOrderVoicemailTaskApiResponseFactory;
	@Nonnull
	private final PatientOrderScheduledOutreachApiResponseFactory patientOrderScheduledOutreachApiResponseFactory;
	@Nonnull
	private final PatientOrderCsvGenerator patientOrderCsvGenerator;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public PatientOrderResource(@Nonnull PatientOrderService patientOrderService,
															@Nonnull ProviderService providerService,
															@Nonnull AccountService accountService,
															@Nonnull InstitutionService institutionService,
															@Nonnull AuthorizationService authorizationService,
															@Nonnull ScreeningService screeningService,
															@Nonnull SystemService systemService,
															@Nonnull PatientOrderApiResponseFactory patientOrderApiResponseFactory,
															@Nonnull PatientOrderNoteApiResponseFactory patientOrderNoteApiResponseFactory,
															@Nonnull PatientOrderOutreachApiResponseFactory patientOrderOutreachApiResponseFactory,
															@Nonnull EpicDepartmentApiResponseFactory epicDepartmentApiResponseFactory,
															@Nonnull EncounterApiResponseFactory encounterApiResponseFactory,
															@Nonnull AccountApiResponseFactory accountApiResponseFactory,
															@Nonnull TimeZoneApiResponseFactory timeZoneApiResponseFactory,
															@Nonnull LanguageApiResponseFactory languageApiResponseFactory,
															@Nonnull CountryApiResponseFactory countryApiResponseFactory,
															@Nonnull PatientOrderAutocompleteResultApiResponseFactory patientOrderAutocompleteResultApiResponseFactory,
															@Nonnull PatientOrderScheduledScreeningApiResponseFactory patientOrderScheduledScreeningApiResponseFactory,
															@Nonnull ScreeningTypeApiResponseFactory screeningTypeApiResponseFactory,
															@Nonnull PatientOrderVoicemailTaskApiResponseFactory patientOrderVoicemailTaskApiResponseFactory,
															@Nonnull PatientOrderScheduledOutreachApiResponseFactory patientOrderScheduledOutreachApiResponseFactory,
															@Nonnull PatientOrderCsvGenerator patientOrderCsvGenerator,
															@Nonnull RequestBodyParser requestBodyParser,
															@Nonnull JsonMapper jsonMapper,
															@Nonnull Formatter formatter,
															@Nonnull Configuration configuration,
															@Nonnull Provider<CurrentContext> currentContextProvider,
															@Nonnull Strings strings) {
		requireNonNull(patientOrderService);
		requireNonNull(providerService);
		requireNonNull(accountService);
		requireNonNull(institutionService);
		requireNonNull(authorizationService);
		requireNonNull(screeningService);
		requireNonNull(systemService);
		requireNonNull(patientOrderApiResponseFactory);
		requireNonNull(patientOrderNoteApiResponseFactory);
		requireNonNull(patientOrderOutreachApiResponseFactory);
		requireNonNull(epicDepartmentApiResponseFactory);
		requireNonNull(encounterApiResponseFactory);
		requireNonNull(accountApiResponseFactory);
		requireNonNull(timeZoneApiResponseFactory);
		requireNonNull(languageApiResponseFactory);
		requireNonNull(countryApiResponseFactory);
		requireNonNull(patientOrderAutocompleteResultApiResponseFactory);
		requireNonNull(patientOrderScheduledScreeningApiResponseFactory);
		requireNonNull(screeningTypeApiResponseFactory);
		requireNonNull(patientOrderVoicemailTaskApiResponseFactory);
		requireNonNull(patientOrderScheduledOutreachApiResponseFactory);
		requireNonNull(patientOrderCsvGenerator);
		requireNonNull(requestBodyParser);
		requireNonNull(jsonMapper);
		requireNonNull(formatter);
		requireNonNull(configuration);
		requireNonNull(currentContextProvider);
		requireNonNull(strings);

		this.patientOrderService = patientOrderService;
		this.providerService = providerService;
		this.accountService = accountService;
		this.institutionService = institutionService;
		this.authorizationService = authorizationService;
		this.screeningService = screeningService;
		this.systemService = systemService;
		this.patientOrderApiResponseFactory = patientOrderApiResponseFactory;
		this.patientOrderNoteApiResponseFactory = patientOrderNoteApiResponseFactory;
		this.patientOrderOutreachApiResponseFactory = patientOrderOutreachApiResponseFactory;
		this.epicDepartmentApiResponseFactory = epicDepartmentApiResponseFactory;
		this.encounterApiResponseFactory = encounterApiResponseFactory;
		this.accountApiResponseFactory = accountApiResponseFactory;
		this.timeZoneApiResponseFactory = timeZoneApiResponseFactory;
		this.languageApiResponseFactory = languageApiResponseFactory;
		this.countryApiResponseFactory = countryApiResponseFactory;
		this.patientOrderAutocompleteResultApiResponseFactory = patientOrderAutocompleteResultApiResponseFactory;
		this.patientOrderScheduledScreeningApiResponseFactory = patientOrderScheduledScreeningApiResponseFactory;
		this.screeningTypeApiResponseFactory = screeningTypeApiResponseFactory;
		this.patientOrderVoicemailTaskApiResponseFactory = patientOrderVoicemailTaskApiResponseFactory;
		this.patientOrderScheduledOutreachApiResponseFactory = patientOrderScheduledOutreachApiResponseFactory;
		this.patientOrderCsvGenerator = patientOrderCsvGenerator;
		this.requestBodyParser = requestBodyParser;
		this.jsonMapper = jsonMapper;
		this.formatter = formatter;
		this.configuration = configuration;
		this.currentContextProvider = currentContextProvider;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/patient-orders/{patientOrderId}")
	@AuthenticationRequired
	public ApiResponse patientOrder(@Nonnull @PathParameter UUID patientOrderId,
																	@Nonnull @QueryParameter("responseSupplement") Optional<List<PatientOrderApiResponseSupplement>> responseSupplements) {
		requireNonNull(patientOrderId);
		requireNonNull(responseSupplements);

		Account account = getCurrentContext().getAccount().get();
		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).orElse(null);
		Set<PatientOrderApiResponseSupplement> finalResponseSupplements = new HashSet<>();
		finalResponseSupplements.addAll(responseSupplements.orElse(List.of(PatientOrderApiResponseSupplement.EVERYTHING)));

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canViewPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		List<PatientOrder> associatedPatientOrders = new ArrayList<>();

		// Only pull associated orders if we need "everything", and even then pull raw orders initially for speed
		if (finalResponseSupplements.contains(PatientOrderApiResponseSupplement.EVERYTHING)) {
			List<RawPatientOrder> rawAssociatedPatientOrders = getPatientOrderService().findRawPatientOrdersByMrnAndInstitutionId(patientOrder.getPatientMrn(), account.getInstitutionId()).stream()
					.filter(associatedPatientOrder -> !associatedPatientOrder.getPatientOrderId().equals(patientOrderId))
					.sorted((patientOrder1, patientOrder2) -> patientOrder2.getOrderDate().compareTo(patientOrder1.getOrderDate()))
					.collect(Collectors.toList());

			for (RawPatientOrder rawAssociatedPatientOrder : rawAssociatedPatientOrders)
				associatedPatientOrders.add(getPatientOrderService().findPatientOrderById(rawAssociatedPatientOrder.getPatientOrderId()).get());
		}

		PatientOrderApiResponseFormat responseFormat = PatientOrderApiResponseFormat.fromRoleId(account.getRoleId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrder", getPatientOrderApiResponseFactory().create(patientOrder, responseFormat, finalResponseSupplements));
			put("associatedPatientOrders", associatedPatientOrders.stream()
					.map(associatedPatientOrder -> getPatientOrderApiResponseFactory().create(associatedPatientOrder,
							responseFormat, Set.of(PatientOrderApiResponseSupplement.PANEL)))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@PATCH("/patient-orders/{patientOrderId}")
	@AuthenticationRequired
	public ApiResponse patchPatientOrder(@Nonnull @PathParameter UUID patientOrderId,
																			 @Nonnull @RequestBody String requestBody) {
		requireNonNull(patientOrderId);
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();
		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_UPDATE_GENERAL);

		// Only patch fields specified in the request
		Map<String, Object> requestBodyAsJson = getJsonMapper().fromJson(requestBody);

		PatchPatientOrderRequest request = getRequestBodyParser().parse(requestBody, PatchPatientOrderRequest.class);
		request.setPatientOrderId(patientOrderId);
		request.setAccountId(account.getAccountId());
		request.setShouldUpdatePatientFirstName(requestBodyAsJson.containsKey("patientFirstName"));
		request.setShouldUpdatePatientLastName(requestBodyAsJson.containsKey("patientLastName"));
		request.setShouldUpdatePatientEmailAddress(requestBodyAsJson.containsKey("patientEmailAddress"));
		request.setShouldUpdatePatientPhoneNumber(requestBodyAsJson.containsKey("patientPhoneNumber"));
		request.setShouldUpdatePatientLanguageCode(requestBodyAsJson.containsKey("patientLanguageCode"));
		request.setShouldUpdatePatientBirthdate(requestBodyAsJson.containsKey("patientBirthdate"));
		request.setShouldUpdatePatientEthnicityId(requestBodyAsJson.containsKey("patientEthnicityId"));
		request.setShouldUpdatePatientGenderIdentityId(requestBodyAsJson.containsKey("patientGenderIdentityId"));
		request.setShouldUpdatePatientBirthSexId(requestBodyAsJson.containsKey("patientBirthSexId"));
		request.setShouldUpdatePatientRaceId(requestBodyAsJson.containsKey("patientRaceId"));
		request.setShouldUpdatePatientAddress(requestBodyAsJson.containsKey("patientAddress"));
		request.setShouldUpdatePatientDemographicsConfirmed(requestBodyAsJson.containsKey("patientDemographicsConfirmed"));
		request.setShouldUpdatePatientOrderCarePreferenceId(requestBodyAsJson.containsKey("patientOrderCarePreferenceId"));
		request.setShouldUpdateInPersonCareRadius(requestBodyAsJson.containsKey("inPersonCareRadius"));

		getPatientOrderService().patchPatientOrder(request);

		PatientOrder updatedPatientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).get();
		PatientOrderApiResponseFormat responseFormat = PatientOrderApiResponseFormat.fromRoleId(account.getRoleId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrder", getPatientOrderApiResponseFactory().create(updatedPatientOrder, responseFormat,
					Set.of(PatientOrderApiResponseSupplement.EVERYTHING)));
		}});
	}

	@Nonnull
	@GET("/patient-orders/{patientOrderId}/clinical-report")
	@AuthenticationRequired
	public ApiResponse patientOrderClinicalReport(@Nonnull @PathParameter UUID patientOrderId) {
		requireNonNull(patientOrderId);

		Account account = getCurrentContext().getAccount().get();
		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canViewPatientOrderClinicalReport(patientOrder, account))
			throw new AuthorizationException();

		String clinicalReport = getPatientOrderService().generateClinicalReport(patientOrder);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("clinicalReport", clinicalReport);
		}});
	}

	@Nonnull
	@GET("/patient-orders/latest")
	@AuthenticationRequired
	public ApiResponse latestPatientOrder(@Nonnull @QueryParameter Optional<UUID> accountId) {
		requireNonNull(accountId);

		// Pull the latest order (if it exists) for the current account by default.
		// Or - if an account ID is provided - pull the current order for that account.
		// We perform an authorization check below after all data is loaded.
		Account currentAccount = getCurrentContext().getAccount().get();
		Account targetAccount = currentAccount;

		if (accountId.isPresent()) {
			targetAccount = getAccountService().findAccountById(accountId.get()).orElse(null);

			if (targetAccount == null)
				throw new NotFoundException();
		}

		PatientOrder patientOrder = getPatientOrderService().findLatestPatientOrderByMrnAndInstitutionId(targetAccount.getEpicPatientMrn(), targetAccount.getInstitutionId()).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canViewPatientOrder(patientOrder, currentAccount))
			throw new AuthorizationException();

		PatientOrderApiResponseFormat responseFormat = PatientOrderApiResponseFormat.fromRoleId(currentAccount.getRoleId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrder", getPatientOrderApiResponseFactory().create(patientOrder, responseFormat,
					Set.of(PatientOrderApiResponseSupplement.EVERYTHING)));
		}});
	}


	@Nonnull
	@GET("/patient-orders/open")
	@AuthenticationRequired
	public ApiResponse openPatientOrder(@Nonnull @QueryParameter Optional<UUID> accountId) {
		requireNonNull(accountId);

		// Pull the open order (if it exists) for the current account by default.
		// Or - if an account ID is provided - pull the open order for that account.
		// We perform an authorization check below after all data is loaded.
		Account currentAccount = getCurrentContext().getAccount().get();
		Account targetAccount = currentAccount;

		if (accountId.isPresent()) {
			targetAccount = getAccountService().findAccountById(accountId.get()).orElse(null);

			if (targetAccount == null)
				throw new NotFoundException();
		}

		PatientOrder patientOrder = getPatientOrderService().findOpenPatientOrderByMrnAndInstitutionId(targetAccount.getEpicPatientMrn(), targetAccount.getInstitutionId()).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canViewPatientOrder(patientOrder, currentAccount))
			throw new AuthorizationException();

		PatientOrderApiResponseFormat responseFormat = PatientOrderApiResponseFormat.fromRoleId(currentAccount.getRoleId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrder", getPatientOrderApiResponseFactory().create(patientOrder, responseFormat,
					Set.of(PatientOrderApiResponseSupplement.EVERYTHING)));
		}});
	}

	@Nonnull
	@PUT("/patient-orders/{patientOrderId}/close")
	@AuthenticationRequired
	public ApiResponse closePatientOrder(@Nonnull @PathParameter UUID patientOrderId,
																			 @Nonnull @RequestBody String requestBody) {
		requireNonNull(patientOrderId);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_UPDATE_DISPOSITION);

		Account account = getCurrentContext().getAccount().get();
		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		ClosePatientOrderRequest request = getRequestBodyParser().parse(requestBody, ClosePatientOrderRequest.class);
		request.setPatientOrderId(patientOrder.getPatientOrderId());
		request.setAccountId(account.getAccountId());

		getPatientOrderService().closePatientOrder(request);

		PatientOrder updatedPatientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).get();
		PatientOrderApiResponseFormat responseFormat = PatientOrderApiResponseFormat.fromRoleId(account.getRoleId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrder", getPatientOrderApiResponseFactory().create(updatedPatientOrder, responseFormat,
					Set.of(PatientOrderApiResponseSupplement.EVERYTHING)));
		}});
	}

	@Nonnull
	@PUT("/patient-orders/{patientOrderId}/consent-status")
	@AuthenticationRequired
	public ApiResponse updatePatientOrderConsentStatus(@Nonnull @PathParameter UUID patientOrderId,
																										 @Nonnull @RequestBody String requestBody) {
		requireNonNull(patientOrderId);
		requireNonNull(requestBody);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_UPDATE_CONSENT);

		Account account = getCurrentContext().getAccount().get();
		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		UpdatePatientOrderConsentStatusRequest request = getRequestBodyParser().parse(requestBody, UpdatePatientOrderConsentStatusRequest.class);
		request.setPatientOrderId(patientOrder.getPatientOrderId());
		request.setAccountId(account.getAccountId());

		getPatientOrderService().updatePatientOrderConsentStatus(request);

		PatientOrder updatedPatientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).get();
		PatientOrderApiResponseFormat responseFormat = PatientOrderApiResponseFormat.fromRoleId(account.getRoleId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrder", getPatientOrderApiResponseFactory().create(updatedPatientOrder, responseFormat,
					Set.of(PatientOrderApiResponseSupplement.EVERYTHING)));
		}});
	}

	@Nonnull
	@PUT("/patient-orders/{patientOrderId}/open")
	@AuthenticationRequired
	public ApiResponse openPatientOrder(@Nonnull @PathParameter UUID patientOrderId) {
		requireNonNull(patientOrderId);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_UPDATE_DISPOSITION);

		Account account = getCurrentContext().getAccount().get();
		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		getPatientOrderService().openPatientOrder(new OpenPatientOrderRequest() {{
			setPatientOrderId(patientOrder.getPatientOrderId());
			setAccountId(account.getAccountId());
		}});

		PatientOrder updatedPatientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).get();
		PatientOrderApiResponseFormat responseFormat = PatientOrderApiResponseFormat.fromRoleId(account.getRoleId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrder", getPatientOrderApiResponseFactory().create(updatedPatientOrder, responseFormat,
					Set.of(PatientOrderApiResponseSupplement.EVERYTHING)));
		}});
	}

	@Nonnull
	@PUT("/patient-orders/{patientOrderId}/patient-order-triage-groups")
	@AuthenticationRequired
	public ApiResponse updatePatientOrderTriageGroups(@Nonnull @PathParameter UUID patientOrderId,
																										@Nonnull @RequestBody String requestBody) {
		requireNonNull(patientOrderId);
		requireNonNull(requestBody);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_TRIAGE_GROUP_CREATE);

		Account account = getCurrentContext().getAccount().get();
		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canUpdatePatientOrderTriages(patientOrder, account))
			throw new AuthorizationException();

		CreatePatientOrderTriageGroupRequest request = getRequestBodyParser().parse(requestBody, CreatePatientOrderTriageGroupRequest.class);
		request.setPatientOrderId(patientOrderId);
		request.setAccountId(account.getAccountId());
		request.setPatientOrderTriageSourceId(PatientOrderTriageSourceId.MANUALLY_SET);
		request.setScreeningSessionId(null);

		getPatientOrderService().createPatientOrderTriageGroup(request);

		PatientOrder updatedPatientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).get();
		PatientOrderApiResponseFormat responseFormat = PatientOrderApiResponseFormat.fromRoleId(account.getRoleId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrder", getPatientOrderApiResponseFactory().create(updatedPatientOrder, responseFormat,
					Set.of(PatientOrderApiResponseSupplement.EVERYTHING)));
		}});
	}

	@Nonnull
	@PUT("/patient-orders/{patientOrderId}/reset-patient-order-triages")
	@AuthenticationRequired
	public ApiResponse resetPatientOrderTriages(@Nonnull @PathParameter UUID patientOrderId) {
		requireNonNull(patientOrderId);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_TRIAGE_GROUP_RESET);

		Account account = getCurrentContext().getAccount().get();
		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canUpdatePatientOrderTriages(patientOrder, account))
			throw new AuthorizationException();

		getPatientOrderService().resetPatientOrderTriages(patientOrderId, account.getAccountId());

		PatientOrder updatedPatientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).get();
		PatientOrderApiResponseFormat responseFormat = PatientOrderApiResponseFormat.fromRoleId(account.getRoleId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrder", getPatientOrderApiResponseFactory().create(updatedPatientOrder, responseFormat,
					Set.of(PatientOrderApiResponseSupplement.EVERYTHING)));
		}});
	}

	@Nonnull
	@GET("/patient-orders")
	@AuthenticationRequired
	public ApiResponse findPatientOrders(@Nonnull @QueryParameter Optional<PatientOrderViewTypeId> patientOrderViewTypeId,
																			 @Nonnull @QueryParameter("patientOrderDispositionId") Optional<List<PatientOrderDispositionId>> patientOrderDispositionIds,
																			 @Nonnull @QueryParameter Optional<PatientOrderConsentStatusId> patientOrderConsentStatusId,
																			 @Nonnull @QueryParameter Optional<PatientOrderScreeningStatusId> patientOrderScreeningStatusId,
																			 @Nonnull @QueryParameter("patientOrderTriageStatusId") Optional<List<PatientOrderTriageStatusId>> patientOrderTriageStatusIds,
																			 @Nonnull @QueryParameter Optional<PatientOrderAssignmentStatusId> patientOrderAssignmentStatusId,
																			 @Nonnull @QueryParameter Optional<PatientOrderOutreachStatusId> patientOrderOutreachStatusId,
																			 @Nonnull @QueryParameter Optional<PatientOrderResponseStatusId> patientOrderResponseStatusId,
																			 @Nonnull @QueryParameter Optional<PatientOrderSafetyPlanningStatusId> patientOrderSafetyPlanningStatusId,
																			 @Nonnull @QueryParameter("patientOrderFilterFlagTypeId") Optional<List<PatientOrderFilterFlagTypeId>> patientOrderFilterFlagTypeIds,
																			 @Nonnull @QueryParameter Optional<List<String>> referringPracticeIds,
																			 @Nonnull @QueryParameter("panelAccountId") Optional<List<UUID>> panelAccountIds,
																			 @Nonnull @QueryParameter Optional<String> patientMrn,
																			 @Nonnull @QueryParameter Optional<String> searchQuery,
																			 @Nonnull @QueryParameter Optional<Integer> pageNumber,
																			 @Nonnull @QueryParameter Optional<Integer> pageSize,
																			 // These 3 are used to construct a single sort rule
																			 @Nonnull @QueryParameter Optional<PatientOrderSortColumnId> patientOrderSortColumnId,
																			 @Nonnull @QueryParameter Optional<SortDirectionId> sortDirectionId,
																			 @Nonnull @QueryParameter Optional<SortNullsId> sortNullsId) {
		requireNonNull(patientOrderViewTypeId);
		requireNonNull(patientOrderDispositionIds);
		requireNonNull(patientOrderConsentStatusId);
		requireNonNull(patientOrderScreeningStatusId);
		requireNonNull(patientOrderTriageStatusIds);
		requireNonNull(patientOrderAssignmentStatusId);
		requireNonNull(patientOrderOutreachStatusId);
		requireNonNull(patientOrderResponseStatusId);
		requireNonNull(patientOrderSafetyPlanningStatusId);
		requireNonNull(patientOrderFilterFlagTypeIds);
		requireNonNull(referringPracticeIds);
		requireNonNull(panelAccountIds);
		requireNonNull(patientMrn);
		requireNonNull(searchQuery);
		requireNonNull(pageNumber);
		requireNonNull(pageSize);
		requireNonNull(patientOrderSortColumnId);
		requireNonNull(sortDirectionId);
		requireNonNull(sortNullsId);

		CurrentContext currentContext = getCurrentContext();
		Account account = currentContext.getAccount().get();
		InstitutionId institutionId = account.getInstitutionId();

		if (!getAuthorizationService().canViewPatientOrders(institutionId, account))
			throw new AuthorizationException();

		// If you want to look at another account's panel, make sure you're authorized to do so.
		// Normally in UI you only check off one person, maybe 2. If users start doing more, we should do this
		// check more efficiently
		if (panelAccountIds.isPresent()) {
			for (UUID panelAccountId : panelAccountIds.get()) {
				Account panelAccount = getAccountService().findAccountById(panelAccountId).orElse(null);

				if (!getAuthorizationService().canViewPatientOrdersForPanelAccount(account, panelAccount))
					throw new AuthorizationException();
			}
		}

		// BE technically accepts a list of PatientOrderSortRule, but FE only exposes a single one to the user in the UI.
		// So we collect the query parameters for the single rule (if all present) into a one-element list.
		// If we want to support multi-column sorting in the future, FE should introduce a new query parameter
		// with some kind of JSON encoding for a list of rule objects
		List<PatientOrderSortRule> patientOrderSortRules = new ArrayList<>();

		if (patientOrderSortColumnId.isPresent()
				&& sortDirectionId.isPresent()) {
			patientOrderSortRules.add(new PatientOrderSortRule() {{
				setPatientOrderSortColumnId(patientOrderSortColumnId.get());
				setSortDirectionId(sortDirectionId.get());

				// If not explicitly specified, use NULLS LAST if the sort is ASC, NULLS FIRST if the sort is DESC
				setSortNullsId(sortNullsId.orElse(
						sortDirectionId.get() == SortDirectionId.ASCENDING ? SortNullsId.NULLS_LAST : SortNullsId.NULLS_FIRST
				));
			}});
		}

		FindResult<PatientOrder> findResult = getPatientOrderService().findPatientOrders(new FindPatientOrdersRequest() {
			{
				setInstitutionId(account.getInstitutionId());
				setPatientOrderViewTypeId(patientOrderViewTypeId.orElse(null));
				setPatientOrderDispositionIds(new HashSet<>(patientOrderDispositionIds.orElse(List.of())));
				setPatientOrderConsentStatusId(patientOrderConsentStatusId.orElse(null));
				setPatientOrderScreeningStatusId(patientOrderScreeningStatusId.orElse(null));
				setPatientOrderTriageStatusIds(new HashSet<>(patientOrderTriageStatusIds.orElse(List.of())));
				setPatientOrderAssignmentStatusId(patientOrderAssignmentStatusId.orElse(null));
				setPatientOrderOutreachStatusId(patientOrderOutreachStatusId.orElse(null));
				setPatientOrderResponseStatusId(patientOrderResponseStatusId.orElse(null));
				setPatientOrderSafetyPlanningStatusId(patientOrderSafetyPlanningStatusId.orElse(null));
				setPatientOrderFilterFlagTypeIds(new HashSet<>(patientOrderFilterFlagTypeIds.orElse(List.of())));
				setReferringPracticeIds(new HashSet<>(referringPracticeIds.orElse(List.of())));
				setPanelAccountIds(new HashSet<>(panelAccountIds.orElse(List.of())));
				setPatientMrn(patientMrn.orElse(null));
				setSearchQuery(searchQuery.orElse(null));
				setPageNumber(pageNumber.orElse(0));
				setPageSize(pageSize.orElse(0));
				setPatientOrderSortRules(patientOrderSortRules);
			}
		});

		List<PatientOrderApiResponse> patientOrders = findResult.getResults().stream()
				.map(patientOrder -> getPatientOrderApiResponseFactory().create(patientOrder,
						PatientOrderApiResponseFormat.fromRoleId(account.getRoleId()),
						Set.of(PatientOrderApiResponseSupplement.PANEL)))
				.collect(Collectors.toList());

		Map<String, Object> findResultJson = new HashMap<>();
		findResultJson.put("patientOrders", patientOrders);
		findResultJson.put("totalCount", findResult.getTotalCount());
		findResultJson.put("totalCountDescription", getFormatter().formatNumber(findResult.getTotalCount()));

		// If there's a patient MRN provided, return it in the form of an autocomplete result.
		// We assume this one is just whatever the first result is...
		PatientOrderAutocompleteResult patientOrderAutocompleteResult = patientMrn.isPresent() ? getPatientOrderService().findPatientOrderAutocompleteResultByMrn(patientMrn.get(), institutionId).orElse(null) : null;

		return new ApiResponse(new HashMap<String, Object>() {{
			put("findResult", findResultJson);
			put("patientOrderAutocompleteResult", patientOrderAutocompleteResult == null ? null : getPatientOrderAutocompleteResultApiResponseFactory().create(patientOrderAutocompleteResult));
		}});
	}

	@Nonnull
	@GET("/patient-orders/autocomplete")
	@AuthenticationRequired
	public ApiResponse patientOrderAutocomplete(@Nonnull @QueryParameter Optional<String> searchQuery) {
		requireNonNull(searchQuery);

		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = account.getInstitutionId();

		if (!getAuthorizationService().canViewPatientOrders(institutionId, account))
			throw new AuthorizationException();

		List<PatientOrderAutocompleteResult> results = getPatientOrderService().findPatientOrderAutocompleteResults(searchQuery.orElse(null), institutionId);

		return new ApiResponse(new HashMap<>() {{
			put("patientOrderAutocompleteResults", results.stream()
					.map(result -> getPatientOrderAutocompleteResultApiResponseFactory().create(result))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@POST("/patient-orders/assign")
	@AuthenticationRequired
	public ApiResponse assignPatientOrders(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = account.getInstitutionId();

		if (!getAuthorizationService().canViewPatientOrders(institutionId, account))
			throw new AuthorizationException();

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_UPDATE_PANEL_ACCOUNT);

		AssignPatientOrdersRequest request = getRequestBodyParser().parse(requestBody, AssignPatientOrdersRequest.class);
		request.setAssignedByAccountId(account.getAccountId());

		if (!getPatientOrderService().arePatientOrderIdsAssociatedWithInstitutionId(request.getPatientOrderIds(), institutionId))
			throw new AuthorizationException();

		int assignedCount = getPatientOrderService().assignPatientOrdersToPanelAccount(request);

		return new ApiResponse(new HashMap<>() {{
			put("assignedCount", assignedCount);
			put("assignedCountDescription", getFormatter().formatNumber(assignedCount));
		}});
	}

	@Nonnull
	@POST("/patient-order-imports")
	@AuthenticationRequired
	public ApiResponse createPatientOrderImport(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canImportPatientOrders(account.getInstitutionId(), account))
			throw new AuthorizationException();

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_IMPORT_CREATE);

		CreatePatientOrderImportRequest request = getRequestBodyParser().parse(requestBody, CreatePatientOrderImportRequest.class);
		request.setInstitutionId(account.getInstitutionId());
		request.setAccountId(account.getAccountId());
		request.setPatientOrderImportTypeId(PatientOrderImportTypeId.CSV);
		request.setAutomaticallyAssignToPanelAccounts(false);

		PatientOrderImportResult patientOrderImportResult = getPatientOrderService().createPatientOrderImport(request);

		return new ApiResponse(patientOrderImportResult);
	}

	@Nonnull
	@GET("/patient-order-notes")
	@AuthenticationRequired
	public ApiResponse patientOrderNotes(@Nonnull @QueryParameter UUID patientOrderId) {
		requireNonNull(patientOrderId);

		Account account = getCurrentContext().getAccount().get();

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canViewPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		List<PatientOrderNote> patientOrderNotes = getPatientOrderService().findPatientOrderNotesByPatientOrderId(patientOrderId);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrderNotes", patientOrderNotes.stream()
					.map(patientOrderNote -> getPatientOrderNoteApiResponseFactory().create(patientOrderNote))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@POST("/patient-order-notes")
	@AuthenticationRequired
	public ApiResponse createPatientOrderNote(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_NOTE_CREATE);

		CreatePatientOrderNoteRequest request = getRequestBodyParser().parse(requestBody, CreatePatientOrderNoteRequest.class);
		request.setAccountId(account.getAccountId());

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(request.getPatientOrderId()).orElse(null);

		if (patientOrder != null && !getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		UUID patientOrderNoteId = getPatientOrderService().createPatientOrderNote(request);
		PatientOrderNote patientOrderNote = getPatientOrderService().findPatientOrderNoteById(patientOrderNoteId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrderNote", getPatientOrderNoteApiResponseFactory().create(patientOrderNote));
		}});
	}

	@Nonnull
	@PUT("/patient-order-notes/{patientOrderNoteId}")
	@AuthenticationRequired
	public ApiResponse updatePatientOrderNote(@Nonnull @PathParameter UUID patientOrderNoteId,
																						@Nonnull @RequestBody String requestBody) {
		requireNonNull(patientOrderNoteId);
		requireNonNull(requestBody);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_NOTE_UPDATE);

		Account account = getCurrentContext().getAccount().get();

		UpdatePatientOrderNoteRequest request = getRequestBodyParser().parse(requestBody, UpdatePatientOrderNoteRequest.class);
		request.setAccountId(account.getAccountId());
		request.setPatientOrderNoteId(patientOrderNoteId);

		PatientOrderNote patientOrderNote = getPatientOrderService().findPatientOrderNoteById(patientOrderNoteId).orElse(null);

		if (patientOrderNote == null)
			throw new NotFoundException();

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderNote.getPatientOrderId()).get();

		if (!getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		getPatientOrderService().updatePatientOrderNote(request);
		PatientOrderNote updatedPatientOrderNote = getPatientOrderService().findPatientOrderNoteById(patientOrderNoteId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrderNote", getPatientOrderNoteApiResponseFactory().create(updatedPatientOrderNote));
		}});
	}

	@Nonnull
	@DELETE("/patient-order-notes/{patientOrderNoteId}")
	@AuthenticationRequired
	public ApiResponse deletePatientOrderNote(@Nonnull @PathParameter UUID patientOrderNoteId) {
		requireNonNull(patientOrderNoteId);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_NOTE_DELETE);

		Account account = getCurrentContext().getAccount().get();

		DeletePatientOrderNoteRequest request = new DeletePatientOrderNoteRequest();
		request.setAccountId(account.getAccountId());
		request.setPatientOrderNoteId(patientOrderNoteId);

		PatientOrderNote patientOrderNote = getPatientOrderService().findPatientOrderNoteById(patientOrderNoteId).orElse(null);

		if (patientOrderNote == null)
			throw new NotFoundException();

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderNote.getPatientOrderId()).orElse(null);

		if (!getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		getPatientOrderService().deletePatientOrderNote(request);

		return new ApiResponse(); // 204
	}

	@Nonnull
	@GET("/patient-order-closure-reasons")
	@AuthenticationRequired
	public ApiResponse patientOrderClosureReasons() {
		List<PatientOrderClosureReason> patientOrderClosureReasons = getPatientOrderService().findPatientOrderClosureReasons();

		return new ApiResponse(new HashMap<>() {{
			put("patientOrderClosureReasons", patientOrderClosureReasons.stream()
					.map(patientOrderClosureReason -> Map.of(
							"patientOrderClosureReasonId", patientOrderClosureReason.getPatientOrderClosureReasonId(),
							"description", patientOrderClosureReason.getDescription()))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@POST("/patient-order-voicemail-tasks")
	@AuthenticationRequired
	public ApiResponse createPatientOrderVoicemailTask(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_VOICEMAIL_TASK_CREATE);

		Account account = getCurrentContext().getAccount().get();

		CreatePatientOrderVoicemailTaskRequest request = getRequestBodyParser().parse(requestBody, CreatePatientOrderVoicemailTaskRequest.class);
		request.setCreatedByAccountId(account.getAccountId());

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(request.getPatientOrderId()).orElse(null);

		if (patientOrder != null && !getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		UUID patientOrderVoicemailTaskId = getPatientOrderService().createPatientOrderVoicemailTask(request);
		PatientOrderVoicemailTask patientOrderVoicemailTask = getPatientOrderService().findPatientOrderVoicemailTaskById(patientOrderVoicemailTaskId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrderVoicemailTask", getPatientOrderVoicemailTaskApiResponseFactory().create(patientOrderVoicemailTask));
		}});
	}

	@Nonnull
	@PUT("/patient-order-voicemail-tasks/{patientOrderVoicemailTaskId}")
	@AuthenticationRequired
	public ApiResponse updatePatientOrderVoicemailTask(@Nonnull @RequestBody String requestBody,
																										 @Nonnull @PathParameter UUID patientOrderVoicemailTaskId) {
		requireNonNull(requestBody);
		requireNonNull(patientOrderVoicemailTaskId);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_VOICEMAIL_TASK_UPDATE);

		Account account = getCurrentContext().getAccount().get();

		PatientOrderVoicemailTask patientOrderVoicemailTask = getPatientOrderService().findPatientOrderVoicemailTaskById(patientOrderVoicemailTaskId).orElse(null);

		if (patientOrderVoicemailTask == null)
			throw new NotFoundException();

		UpdatePatientOrderVoicemailTaskRequest request = getRequestBodyParser().parse(requestBody, UpdatePatientOrderVoicemailTaskRequest.class);
		request.setUpdatedByAccountId(account.getAccountId());
		request.setPatientOrderVoicemailTaskId(patientOrderVoicemailTaskId);

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderVoicemailTask.getPatientOrderId()).orElse(null);

		if (patientOrder != null && !getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		getPatientOrderService().updatePatientOrderVoicemailTask(request);
		PatientOrderVoicemailTask updatedPatientOrderVoicemailTask = getPatientOrderService().findPatientOrderVoicemailTaskById(patientOrderVoicemailTaskId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrderVoicemailTask", getPatientOrderVoicemailTaskApiResponseFactory().create(updatedPatientOrderVoicemailTask));
		}});
	}

	@Nonnull
	@DELETE("/patient-order-voicemail-tasks/{patientOrderVoicemailTaskId}")
	@AuthenticationRequired
	public ApiResponse deletePatientOrderVoicemailTask(@Nonnull @RequestBody String requestBody,
																										 @Nonnull @PathParameter UUID patientOrderVoicemailTaskId) {
		requireNonNull(requestBody);
		requireNonNull(patientOrderVoicemailTaskId);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_VOICEMAIL_TASK_DELETE);

		Account account = getCurrentContext().getAccount().get();

		PatientOrderVoicemailTask patientOrderVoicemailTask = getPatientOrderService().findPatientOrderVoicemailTaskById(patientOrderVoicemailTaskId).orElse(null);

		if (patientOrderVoicemailTask == null)
			throw new NotFoundException();

		DeletePatientOrderVoicemailTaskRequest request = getRequestBodyParser().parse(requestBody, DeletePatientOrderVoicemailTaskRequest.class);
		request.setDeletedByAccountId(account.getAccountId());
		request.setPatientOrderVoicemailTaskId(patientOrderVoicemailTaskId);

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderVoicemailTask.getPatientOrderId()).orElse(null);

		if (patientOrder != null && !getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		getPatientOrderService().deletePatientOrderVoicemailTask(request);

		return new ApiResponse();
	}

	@Nonnull
	@POST("/patient-order-voicemail-tasks/{patientOrderVoicemailTaskId}/complete")
	@AuthenticationRequired
	public ApiResponse completePatientOrderVoicemailTask(@Nonnull @PathParameter UUID patientOrderVoicemailTaskId) {
		requireNonNull(patientOrderVoicemailTaskId);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_VOICEMAIL_TASK_COMPLETE);

		Account account = getCurrentContext().getAccount().get();

		PatientOrderVoicemailTask patientOrderVoicemailTask = getPatientOrderService().findPatientOrderVoicemailTaskById(patientOrderVoicemailTaskId).orElse(null);

		if (patientOrderVoicemailTask == null)
			throw new NotFoundException();

		CompletePatientOrderVoicemailTaskRequest request = new CompletePatientOrderVoicemailTaskRequest();
		request.setCompletedByAccountId(account.getAccountId());
		request.setPatientOrderVoicemailTaskId(patientOrderVoicemailTaskId);

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderVoicemailTask.getPatientOrderId()).orElse(null);

		if (patientOrder != null && !getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		getPatientOrderService().completePatientOrderVoicemailTask(request);
		PatientOrderVoicemailTask completedPatientOrderVoicemailTask = getPatientOrderService().findPatientOrderVoicemailTaskById(patientOrderVoicemailTaskId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrderVoicemailTask", getPatientOrderVoicemailTaskApiResponseFactory().create(completedPatientOrderVoicemailTask));
		}});
	}

	@Nonnull
	@POST("/patient-order-scheduled-outreaches")
	@AuthenticationRequired
	public ApiResponse createPatientOrderScheduledOutreach(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_SCHEDULED_OUTREACH_CREATE);

		Account account = getCurrentContext().getAccount().get();

		CreatePatientOrderScheduledOutreachRequest request = getRequestBodyParser().parse(requestBody, CreatePatientOrderScheduledOutreachRequest.class);
		request.setCreatedByAccountId(account.getAccountId());

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(request.getPatientOrderId()).orElse(null);

		if (patientOrder != null && !getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		UUID patientOrderScheduledOutreachId = getPatientOrderService().createPatientOrderScheduledOutreach(request);
		PatientOrderScheduledOutreach patientOrderScheduledOutreach = getPatientOrderService().findPatientOrderScheduledOutreachById(patientOrderScheduledOutreachId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrderScheduledOutreach", getPatientOrderScheduledOutreachApiResponseFactory().create(patientOrderScheduledOutreach));
		}});
	}

	@Nonnull
	@PUT("/patient-order-scheduled-outreaches/{patientOrderScheduledOutreachId}")
	@AuthenticationRequired
	public ApiResponse updatePatientOrderScheduledOutreach(@Nonnull @RequestBody String requestBody,
																												 @Nonnull @PathParameter UUID patientOrderScheduledOutreachId) {
		requireNonNull(requestBody);
		requireNonNull(patientOrderScheduledOutreachId);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_SCHEDULED_OUTREACH_UPDATE);

		Account account = getCurrentContext().getAccount().get();

		PatientOrderScheduledOutreach patientOrderScheduledOutreach = getPatientOrderService().findPatientOrderScheduledOutreachById(patientOrderScheduledOutreachId).orElse(null);

		if (patientOrderScheduledOutreach == null)
			throw new NotFoundException();

		UpdatePatientOrderScheduledOutreachRequest request = getRequestBodyParser().parse(requestBody, UpdatePatientOrderScheduledOutreachRequest.class);
		request.setUpdatedByAccountId(account.getAccountId());
		request.setPatientOrderScheduledOutreachId(patientOrderScheduledOutreachId);

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderScheduledOutreach.getPatientOrderId()).orElse(null);

		if (patientOrder != null && !getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		getPatientOrderService().updatePatientOrderScheduledOutreach(request);
		PatientOrderScheduledOutreach updatedPatientOrderScheduledOutreach = getPatientOrderService().findPatientOrderScheduledOutreachById(patientOrderScheduledOutreachId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrderScheduledOutreach", getPatientOrderScheduledOutreachApiResponseFactory().create(updatedPatientOrderScheduledOutreach));
		}});
	}

	@Nonnull
	@POST("/patient-order-scheduled-outreaches/{patientOrderScheduledOutreachId}/cancel")
	@AuthenticationRequired
	public ApiResponse cancelPatientOrderScheduledOutreach(@Nonnull @PathParameter UUID patientOrderScheduledOutreachId) {
		requireNonNull(patientOrderScheduledOutreachId);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_SCHEDULED_OUTREACH_CANCEL);

		Account account = getCurrentContext().getAccount().get();

		PatientOrderScheduledOutreach patientOrderScheduledOutreach = getPatientOrderService().findPatientOrderScheduledOutreachById(patientOrderScheduledOutreachId).orElse(null);

		if (patientOrderScheduledOutreach == null)
			throw new NotFoundException();

		CancelPatientOrderScheduledOutreachRequest request = new CancelPatientOrderScheduledOutreachRequest();
		request.setCanceledByAccountId(account.getAccountId());
		request.setPatientOrderScheduledOutreachId(patientOrderScheduledOutreachId);

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderScheduledOutreach.getPatientOrderId()).orElse(null);

		if (patientOrder != null && !getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		getPatientOrderService().cancelPatientOrderScheduledOutreach(request);

		return new ApiResponse();
	}

	@Nonnull
	@POST("/patient-order-scheduled-outreaches/{patientOrderScheduledOutreachId}/complete")
	@AuthenticationRequired
	public ApiResponse completePatientOrderScheduledOutreach(@Nonnull @PathParameter UUID patientOrderScheduledOutreachId,
																													 @Nonnull @RequestBody String requestBody) {
		requireNonNull(patientOrderScheduledOutreachId);
		requireNonNull(requestBody);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_SCHEDULED_OUTREACH_COMPLETE);

		Account account = getCurrentContext().getAccount().get();

		PatientOrderScheduledOutreach patientOrderScheduledOutreach = getPatientOrderService().findPatientOrderScheduledOutreachById(patientOrderScheduledOutreachId).orElse(null);

		if (patientOrderScheduledOutreach == null)
			throw new NotFoundException();

		CompletePatientOrderScheduledOutreachRequest request = getRequestBodyParser().parse(requestBody, CompletePatientOrderScheduledOutreachRequest.class);
		request.setCompletedByAccountId(account.getAccountId());
		request.setPatientOrderScheduledOutreachId(patientOrderScheduledOutreachId);

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderScheduledOutreach.getPatientOrderId()).orElse(null);

		if (patientOrder != null && !getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		getPatientOrderService().completePatientOrderScheduledOutreach(request);
		PatientOrderScheduledOutreach completedPatientOrderScheduledOutreach = getPatientOrderService().findPatientOrderScheduledOutreachById(patientOrderScheduledOutreachId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrderScheduledOutreach", getPatientOrderScheduledOutreachApiResponseFactory().create(completedPatientOrderScheduledOutreach));
		}});
	}

	@Nonnull
	@GET("/patient-order-outreaches")
	@AuthenticationRequired
	public ApiResponse patientOrderOutreaches(@Nonnull @QueryParameter UUID patientOrderId) {
		requireNonNull(patientOrderId);

		Account account = getCurrentContext().getAccount().get();

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canViewPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		List<PatientOrderOutreach> patientOrderOutreaches = getPatientOrderService().findPatientOrderOutreachesByPatientOrderId(patientOrderId);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrderOutreaches", patientOrderOutreaches.stream()
					.map(patientOrderOutreach -> getPatientOrderOutreachApiResponseFactory().create(patientOrderOutreach))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@POST("/patient-order-outreaches")
	@AuthenticationRequired
	public ApiResponse createPatientOrderOutreach(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_OUTREACH_CREATE);

		Account account = getCurrentContext().getAccount().get();

		CreatePatientOrderOutreachRequest request = getRequestBodyParser().parse(requestBody, CreatePatientOrderOutreachRequest.class);
		request.setAccountId(account.getAccountId());

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(request.getPatientOrderId()).orElse(null);

		if (patientOrder != null && !getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		UUID patientOrderOutreachId = getPatientOrderService().createPatientOrderOutreach(request);
		PatientOrderOutreach patientOrderOutreach = getPatientOrderService().findPatientOrderOutreachById(patientOrderOutreachId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrderOutreach", getPatientOrderOutreachApiResponseFactory().create(patientOrderOutreach));
		}});
	}

	@Nonnull
	@PUT("/patient-order-outreaches/{patientOrderOutreachId}")
	@AuthenticationRequired
	public ApiResponse updatePatientOrderOutreach(@Nonnull @PathParameter UUID patientOrderOutreachId,
																								@Nonnull @RequestBody String requestBody) {
		requireNonNull(patientOrderOutreachId);
		requireNonNull(requestBody);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_OUTREACH_UPDATE);

		Account account = getCurrentContext().getAccount().get();

		UpdatePatientOrderOutreachRequest request = getRequestBodyParser().parse(requestBody, UpdatePatientOrderOutreachRequest.class);
		request.setAccountId(account.getAccountId());
		request.setPatientOrderOutreachId(patientOrderOutreachId);

		PatientOrderOutreach patientOrderOutreach = getPatientOrderService().findPatientOrderOutreachById(patientOrderOutreachId).orElse(null);

		if (patientOrderOutreach == null)
			throw new NotFoundException();

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderOutreach.getPatientOrderId()).get();

		if (!getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		getPatientOrderService().updatePatientOrderOutreach(request);
		PatientOrderOutreach updatedPatientOrderOutreach = getPatientOrderService().findPatientOrderOutreachById(patientOrderOutreachId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrderOutreach", getPatientOrderOutreachApiResponseFactory().create(updatedPatientOrderOutreach));
		}});
	}

	@Nonnull
	@DELETE("/patient-order-outreaches/{patientOrderOutreachId}")
	@AuthenticationRequired
	public ApiResponse deletePatientOrderOutreach(@Nonnull @PathParameter UUID patientOrderOutreachId) {
		requireNonNull(patientOrderOutreachId);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_OUTREACH_DELETE);

		Account account = getCurrentContext().getAccount().get();

		DeletePatientOrderOutreachRequest request = new DeletePatientOrderOutreachRequest();
		request.setAccountId(account.getAccountId());
		request.setPatientOrderOutreachId(patientOrderOutreachId);

		PatientOrderOutreach patientOrderOutreach = getPatientOrderService().findPatientOrderOutreachById(patientOrderOutreachId).orElse(null);

		if (patientOrderOutreach == null)
			throw new NotFoundException();

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderOutreach.getPatientOrderId()).get();

		if (!getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		getPatientOrderService().deletePatientOrderOutreach(request);

		return new ApiResponse(); // 204
	}

	@Nonnull
	@POST("/patient-order-scheduled-message-groups")
	@AuthenticationRequired
	public ApiResponse createPatientOrderScheduledMessageGroup(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_SCHEDULED_MESSAGE_GROUP_CREATE);

		Account account = getCurrentContext().getAccount().get();

		CreatePatientOrderScheduledMessageGroupRequest request = getRequestBodyParser().parse(requestBody, CreatePatientOrderScheduledMessageGroupRequest.class);
		request.setAccountId(account.getAccountId());

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(request.getPatientOrderId()).orElse(null);

		if (patientOrder != null && !getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		UUID patientOrderScheduledMessageGroupId = getPatientOrderService().createPatientOrderScheduledMessageGroup(request);

		List<PatientOrderScheduledMessage> patientOrderScheduledMessages = getPatientOrderService().findPatientOrderScheduledMessagesByPatientOrderId(request.getPatientOrderId()).stream()
				.filter(patientOrderScheduledMessage -> patientOrderScheduledMessage.getPatientOrderScheduledMessageGroupId().equals(patientOrderScheduledMessageGroupId))
				.collect(Collectors.toList());

		PatientOrderScheduledMessageGroupApiResponse patientOrderScheduledMessageGroupApiResponse = getPatientOrderService().generatePatientOrderScheduledMessageGroupApiResponses(patientOrderScheduledMessages).stream()
				.findFirst()
				.get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrderScheduledMessageGroup", patientOrderScheduledMessageGroupApiResponse);
		}});
	}

	@Nonnull
	@PUT("/patient-order-scheduled-message-groups/{patientOrderScheduledMessageGroupId}")
	@AuthenticationRequired
	public ApiResponse updatePatientOrderScheduledMessageGroup(@Nonnull @PathParameter UUID patientOrderScheduledMessageGroupId,
																														 @Nonnull @RequestBody String requestBody) {
		requireNonNull(patientOrderScheduledMessageGroupId);
		requireNonNull(requestBody);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_SCHEDULED_MESSAGE_GROUP_UPDATE);

		PatientOrderScheduledMessageGroup patientOrderScheduledMessageGroup = getPatientOrderService().findPatientOrderScheduledMessageGroupById(patientOrderScheduledMessageGroupId).orElse(null);

		if (patientOrderScheduledMessageGroup == null)
			throw new NotFoundException();

		Account account = getCurrentContext().getAccount().get();
		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderScheduledMessageGroup.getPatientOrderId()).orElse(null);

		if (patientOrder != null && !getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		UpdatePatientOrderScheduledMessageGroupRequest request = getRequestBodyParser().parse(requestBody, UpdatePatientOrderScheduledMessageGroupRequest.class);
		request.setAccountId(account.getAccountId());
		request.setPatientOrderScheduledMessageGroupId(patientOrderScheduledMessageGroupId);

		getPatientOrderService().updatePatientOrderScheduledMessageGroup(request);

		List<PatientOrderScheduledMessage> patientOrderScheduledMessages = getPatientOrderService().findPatientOrderScheduledMessagesByPatientOrderId(patientOrder.getPatientOrderId()).stream()
				.filter(patientOrderScheduledMessage -> patientOrderScheduledMessage.getPatientOrderScheduledMessageGroupId().equals(patientOrderScheduledMessageGroupId))
				.collect(Collectors.toList());

		PatientOrderScheduledMessageGroupApiResponse patientOrderScheduledMessageGroupApiResponse = getPatientOrderService().generatePatientOrderScheduledMessageGroupApiResponses(patientOrderScheduledMessages).stream()
				.findFirst()
				.get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrderScheduledMessageGroup", patientOrderScheduledMessageGroupApiResponse);
		}});
	}

	@Nonnull
	@DELETE("/patient-order-scheduled-message-groups/{patientOrderScheduledMessageGroupId}")
	@AuthenticationRequired
	public ApiResponse deletePatientOrderScheduledMessageGroup(@Nonnull @PathParameter UUID patientOrderScheduledMessageGroupId) {
		requireNonNull(patientOrderScheduledMessageGroupId);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_SCHEDULED_MESSAGE_GROUP_DELETE);

		PatientOrderScheduledMessageGroup patientOrderScheduledMessageGroup = getPatientOrderService().findPatientOrderScheduledMessageGroupById(patientOrderScheduledMessageGroupId).orElse(null);

		if (patientOrderScheduledMessageGroup == null)
			throw new NotFoundException();

		Account account = getCurrentContext().getAccount().get();
		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderScheduledMessageGroup.getPatientOrderId()).orElse(null);

		if (patientOrder != null && !getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		DeletePatientOrderScheduledMessageGroupRequest request = new DeletePatientOrderScheduledMessageGroupRequest();
		request.setAccountId(account.getAccountId());
		request.setPatientOrderScheduledMessageGroupId(patientOrderScheduledMessageGroupId);

		getPatientOrderService().deletePatientOrderScheduledMessageGroup(request);

		return new ApiResponse(); // 204
	}

	@Nonnull
	@POST("/patient-order-scheduled-screenings")
	@AuthenticationRequired
	public ApiResponse createPatientOrderScheduledScreening(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_SCHEDULED_SCREENING_CREATE);

		Account account = getCurrentContext().getAccount().get();

		CreatePatientOrderScheduledScreeningRequest request = getRequestBodyParser().parse(requestBody, CreatePatientOrderScheduledScreeningRequest.class);
		request.setAccountId(account.getAccountId());

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(request.getPatientOrderId()).orElse(null);

		if (patientOrder != null && !getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		UUID patientOrderScheduledScreeningId = getPatientOrderService().createPatientOrderScheduledScreening(request);
		PatientOrderScheduledScreening patientOrderScheduledScreening = getPatientOrderService().findPatientOrderScheduledScreeningById(patientOrderScheduledScreeningId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrderScheduledScreening", getPatientOrderScheduledScreeningApiResponseFactory().create(patientOrderScheduledScreening));
		}});
	}

	@Nonnull
	@PUT("/patient-order-scheduled-screenings/{patientOrderScheduledScreeningId}")
	@AuthenticationRequired
	public ApiResponse updatePatientOrderScheduledScreening(@Nonnull @PathParameter UUID patientOrderScheduledScreeningId,
																													@Nonnull @RequestBody String requestBody) {
		requireNonNull(patientOrderScheduledScreeningId);
		requireNonNull(requestBody);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_SCHEDULED_SCREENING_UPDATE);

		Account account = getCurrentContext().getAccount().get();

		UpdatePatientOrderScheduledScreeningRequest request = getRequestBodyParser().parse(requestBody, UpdatePatientOrderScheduledScreeningRequest.class);
		request.setPatientOrderScheduledScreeningId(patientOrderScheduledScreeningId);
		request.setAccountId(account.getAccountId());

		PatientOrderScheduledScreening patientOrderScheduledScreening = getPatientOrderService().findPatientOrderScheduledScreeningById(patientOrderScheduledScreeningId).orElse(null);

		if (patientOrderScheduledScreening == null)
			throw new NotFoundException();

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderScheduledScreening.getPatientOrderId()).orElse(null);

		if (patientOrder != null && !getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		getPatientOrderService().updatePatientOrderScheduledScreening(request);
		PatientOrderScheduledScreening updatedPatientOrderScheduledScreening = getPatientOrderService().findPatientOrderScheduledScreeningById(patientOrderScheduledScreeningId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrderScheduledScreening", getPatientOrderScheduledScreeningApiResponseFactory().create(updatedPatientOrderScheduledScreening));
		}});
	}

	@Nonnull
	@DELETE("/patient-order-scheduled-screenings/{patientOrderScheduledScreeningId}")
	@AuthenticationRequired
	public ApiResponse cancelPatientOrderScheduledScreening(@Nonnull @PathParameter UUID patientOrderScheduledScreeningId) {
		requireNonNull(patientOrderScheduledScreeningId);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_SCHEDULED_SCREENING_CANCEL);

		Account account = getCurrentContext().getAccount().get();
		PatientOrderScheduledScreening patientOrderScheduledScreening = getPatientOrderService().findPatientOrderScheduledScreeningById(patientOrderScheduledScreeningId).orElse(null);

		if (patientOrderScheduledScreening == null)
			throw new NotFoundException();

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderScheduledScreening.getPatientOrderId()).orElse(null);

		if (patientOrder != null && !getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		CancelPatientOrderScheduledScreeningRequest request = new CancelPatientOrderScheduledScreeningRequest();
		request.setPatientOrderScheduledScreeningId(patientOrderScheduledScreeningId);
		request.setAccountId(account.getAccountId());

		getPatientOrderService().cancelPatientOrderScheduledScreening(request);

		return new ApiResponse();
	}

	@Nonnull
	@GET("/integrated-care/panel-today")
	@AuthenticationRequired
	@ReadReplica
	public ApiResponse panelToday(@Nonnull @QueryParameter Optional<UUID> panelAccountId) {
		requireNonNull(panelAccountId);

		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = account.getInstitutionId();

		if (panelAccountId.isPresent()) {
			account = getAccountService().findAccountById(panelAccountId.get()).orElse(null);

			if (account == null)
				throw new NotFoundException();
		}

		if (!getAuthorizationService().canViewPanelAccounts(institutionId, account))
			throw new AuthorizationException();

		LocalDateTime today = LocalDateTime.now(account.getTimeZone());
		LocalDateTime endOfDayToday = LocalDateTime.of(today.toLocalDate(), LocalTime.MAX);

		// Pull all the orders for the "today" view and chunk them up into the sections needed for the UI
		List<PatientOrder> patientOrders = getPatientOrderService().findOpenPatientOrdersForPanelAccountId(account.getAccountId());

		// "Safety Planning": patient_order_safety_planning_status_id == NEEDS_SAFETY_PLANNING
		List<PatientOrderApiResponse> safetyPlanningPatientOrders = patientOrders.stream()
				.filter(patientOrder -> patientOrder.getPatientOrderSafetyPlanningStatusId() == PatientOrderSafetyPlanningStatusId.NEEDS_SAFETY_PLANNING)
				.map(patientOrder -> getPatientOrderApiResponseFactory().create(patientOrder, PatientOrderApiResponseFormat.MHIC))
				.collect(Collectors.toList());

		// "Outreach Review": total_outreach_count == 0
		List<PatientOrderApiResponse> outreachReviewPatientOrders = patientOrders.stream()
				.filter(patientOrder -> patientOrder.getTotalOutreachCount() == 0)
				.map(patientOrder -> getPatientOrderApiResponseFactory().create(patientOrder, PatientOrderApiResponseFormat.MHIC))
				.collect(Collectors.toList());

		// "Voicemails": most recent voicemail task exists, but is incomplete
		List<PatientOrderApiResponse> voicemailTaskPatientOrders = patientOrders.stream()
				.filter(patientOrder -> patientOrder.getMostRecentPatientOrderVoicemailTaskId() != null
						&& !patientOrder.getMostRecentPatientOrderVoicemailTaskCompleted())
				.map(patientOrder -> getPatientOrderApiResponseFactory().create(patientOrder, PatientOrderApiResponseFormat.MHIC))
				.collect(Collectors.toList());

		Set<PatientOrderContactTypeId> validFollowUpContactTypeIds = Set.of(
				PatientOrderContactTypeId.ASSESSMENT_OUTREACH,
				PatientOrderContactTypeId.ASSESSMENT,
				PatientOrderContactTypeId.OTHER,
				PatientOrderContactTypeId.RESOURCE_FOLLOWUP
		);

		// "Follow Up"
		List<PatientOrderApiResponse> outreachFollowupNeededPatientOrders = patientOrders.stream()
				.filter(patientOrder -> {
					boolean overdueForOutreach = patientOrder.getNextContactScheduledAt() != null
							&& patientOrder.getNextContactTypeId() != null
							&& validFollowUpContactTypeIds.contains(patientOrder.getNextContactTypeId())
							&& patientOrder.getNextContactScheduledAt().isBefore(endOfDayToday);

					if (overdueForOutreach)
						return true;

					// If screening session has been started but abandoned and no upcoming contact is scheduled, show the order in "follow up"
					boolean startedButAbandonedScreeningSession = patientOrder.getMostRecentIntakeScreeningSessionAppearsAbandoned()
							|| patientOrder.getMostRecentScreeningSessionAppearsAbandoned();

					if (startedButAbandonedScreeningSession && patientOrder.getNextContactScheduledAt() == null)
						return true;

					return false;
				})
				.map(patientOrder -> getPatientOrderApiResponseFactory().create(patientOrder, PatientOrderApiResponseFormat.MHIC))
				.collect(Collectors.toList());

		// "Assessments": status == NOT_TRIAGED && patient_order_scheduled_screening_scheduled_date_time <= TODAY
		List<PatientOrderApiResponse> scheduledAssessmentPatientOrders = patientOrders.stream()
				.filter(patientOrder -> patientOrder.getPatientOrderTriageStatusId() == PatientOrderTriageStatusId.NOT_TRIAGED
						&& patientOrder.getPatientOrderScheduledScreeningScheduledDateTime() != null
						&& patientOrder.getPatientOrderScheduledScreeningScheduledDateTime().isBefore(endOfDayToday))
				.map(patientOrder -> getPatientOrderApiResponseFactory().create(patientOrder, PatientOrderApiResponseFormat.MHIC))
				.collect(Collectors.toList());

		// "Resources": patient_order_resourcing_status_id == NEEDS_RESOURCES
		List<PatientOrderApiResponse> needResourcesPatientOrders = patientOrders.stream()
				.filter(patientOrder -> patientOrder.getPatientOrderResourcingStatusId() == PatientOrderResourcingStatusId.NEEDS_RESOURCES)
				.map(patientOrder -> getPatientOrderApiResponseFactory().create(patientOrder, PatientOrderApiResponseFormat.MHIC))
				.collect(Collectors.toList());

		return new ApiResponse(new HashMap<>() {{
			put("safetyPlanningPatientOrders", safetyPlanningPatientOrders);
			put("outreachReviewPatientOrders", outreachReviewPatientOrders);
			put("voicemailTaskPatientOrders", voicemailTaskPatientOrders);
			put("scheduledAssessmentPatientOrders", scheduledAssessmentPatientOrders);
			put("needResourcesPatientOrders", needResourcesPatientOrders);
			put("outreachFollowupNeededPatientOrders", outreachFollowupNeededPatientOrders);
		}});
	}

	@Nonnull
	@GET("/integrated-care/panel-counts")
	@AuthenticationRequired
	public ApiResponse panelCounts(@Nonnull @QueryParameter Optional<UUID> panelAccountId) {
		requireNonNull(panelAccountId);

		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = account.getInstitutionId();

		if (panelAccountId.isPresent()) {
			account = getAccountService().findAccountById(panelAccountId.get()).orElse(null);

			if (account == null)
				throw new NotFoundException();
		}

		if (!getAuthorizationService().canViewPanelAccounts(institutionId, account))
			throw new AuthorizationException();

		Map<PatientOrderViewTypeId, Integer> patientOrderCountsByPatientOrderViewTypeId = getPatientOrderService().findPatientOrderCountsByPatientOrderViewTypeIdForInstitutionId(institutionId, account.getAccountId());
		Map<PatientOrderViewTypeId, Map<String, Object>> patientOrderCountsByPatientOrderViewTypeIdJson = new HashMap<>(patientOrderCountsByPatientOrderViewTypeId.size());

		for (Entry<PatientOrderViewTypeId, Integer> entry : patientOrderCountsByPatientOrderViewTypeId.entrySet()) {
			PatientOrderViewTypeId patientOrderViewTypeId = entry.getKey();
			Integer count = entry.getValue();
			patientOrderCountsByPatientOrderViewTypeIdJson.put(patientOrderViewTypeId, Map.of(
					"patientOrderCount", count,
					"patientOrderCountDescription", getFormatter().formatNumber(count)
			));
		}

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrderCountsByPatientOrderViewTypeId", patientOrderCountsByPatientOrderViewTypeIdJson);
		}});
	}

	@Nonnull
	@GET("/integrated-care/panel-accounts")
	@AuthenticationRequired
	public ApiResponse panelAccounts() {
		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = account.getInstitutionId();

		if (!getAuthorizationService().canViewPanelAccounts(institutionId, account))
			throw new AuthorizationException();

		List<AccountApiResponse> orderServicerAccounts = getPatientOrderService().findOrderServicerAccountsByInstitutionId(institutionId).stream()
				.map(orderServicerAccount -> getAccountApiResponseFactory().create(orderServicerAccount))
				.collect(Collectors.toList());

		List<AccountApiResponse> panelAccounts = getPatientOrderService().findPanelAccountsByInstitutionId(institutionId).stream()
				.map(panelAccount -> getAccountApiResponseFactory().create(panelAccount))
				.collect(Collectors.toList());

		Map<UUID, Integer> openPatientOrderCountsByPanelAccountId = getPatientOrderService().findOpenPatientOrderCountsByPanelAccountIdForInstitutionId(institutionId);

		// If there are any "holes" in the mapping of panel account IDs -> open order counts,
		// fill in the holes with 0-counts.
		for (AccountApiResponse panelAccount : panelAccounts)
			if (!openPatientOrderCountsByPanelAccountId.containsKey(panelAccount.getAccountId()))
				openPatientOrderCountsByPanelAccountId.put(panelAccount.getAccountId(), 0);

		Map<UUID, Map<String, Object>> openPatientOrderCountsByPanelAccountIdJson = new HashMap<>(openPatientOrderCountsByPanelAccountId.size());

		for (Entry<UUID, Integer> entry : openPatientOrderCountsByPanelAccountId.entrySet()) {
			UUID panelAccountId = entry.getKey();
			Integer openPatientOrderCount = entry.getValue();
			openPatientOrderCountsByPanelAccountIdJson.put(panelAccountId, Map.of(
					"openPatientOrderCount", openPatientOrderCount,
					"openPatientOrderCountDescription", getFormatter().formatNumber(openPatientOrderCount)
			));
		}

		int overallOpenPatientOrderCount = getPatientOrderService().findPatientOrderDispositionCountForInstitutionId(account.getInstitutionId(), null, PatientOrderDispositionId.OPEN);
		String overallOpenPatientOrderCountDescription = getFormatter().formatNumber(overallOpenPatientOrderCount);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("orderServicerAccounts", orderServicerAccounts);
			put("panelAccounts", panelAccounts);
			put("openPatientOrderCountsByPanelAccountId", openPatientOrderCountsByPanelAccountIdJson);
			put("overallOpenPatientOrderCount", overallOpenPatientOrderCount);
			put("overallOpenPatientOrderCountDescription", overallOpenPatientOrderCountDescription);
		}});
	}

	@Nonnull
	@PUT("/patient-orders/{patientOrderId}/patient-order-resource-check-in-response-status")
	@AuthenticationRequired
	public ApiResponse updatePatientOrderResourceCheckInResponseStatus(@Nonnull @PathParameter UUID patientOrderId,
																																		 @Nonnull @RequestBody String requestBody) {
		requireNonNull(patientOrderId);
		requireNonNull(requestBody);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_UPDATE_RESOURCE_CHECK_IN_RESPONSE);

		Account account = getCurrentContext().getAccount().get();
		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		UpdatePatientOrderResourceCheckInResponseStatusRequest request = getRequestBodyParser().parse(requestBody, UpdatePatientOrderResourceCheckInResponseStatusRequest.class);
		request.setPatientOrderId(patientOrder.getPatientOrderId());
		request.setAccountId(account.getAccountId());

		getPatientOrderService().updatePatientOrderResourceCheckInResponseStatus(request);

		PatientOrder updatedPatientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).get();
		PatientOrderApiResponseFormat responseFormat = PatientOrderApiResponseFormat.fromRoleId(account.getRoleId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrder", getPatientOrderApiResponseFactory().create(updatedPatientOrder, responseFormat,
					Set.of(PatientOrderApiResponseSupplement.EVERYTHING)));
		}});
	}

	@Nonnull
	@PUT("/patient-orders/{patientOrderId}/patient-order-resourcing-status")
	@AuthenticationRequired
	public ApiResponse updatePatientOrderResourcingStatus(@Nonnull @PathParameter UUID patientOrderId,
																												@Nonnull @RequestBody String requestBody) {
		requireNonNull(patientOrderId);
		requireNonNull(requestBody);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_UPDATE_RESOURCING);

		Account account = getCurrentContext().getAccount().get();
		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		UpdatePatientOrderResourcingStatusRequest request = getRequestBodyParser().parse(requestBody, UpdatePatientOrderResourcingStatusRequest.class);
		request.setPatientOrderId(patientOrder.getPatientOrderId());
		request.setAccountId(account.getAccountId());

		getPatientOrderService().updatePatientOrderResourcingStatus(request);

		PatientOrder updatedPatientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).get();
		PatientOrderApiResponseFormat responseFormat = PatientOrderApiResponseFormat.fromRoleId(account.getRoleId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrder", getPatientOrderApiResponseFactory().create(updatedPatientOrder, responseFormat,
					Set.of(PatientOrderApiResponseSupplement.EVERYTHING)));
		}});
	}

	@Nonnull
	@PUT("/patient-orders/{patientOrderId}/patient-order-safety-planning-status")
	@AuthenticationRequired
	public ApiResponse updatePatientOrderSafetyPlanningStatus(@Nonnull @PathParameter UUID patientOrderId,
																														@Nonnull @RequestBody String requestBody) {
		requireNonNull(patientOrderId);
		requireNonNull(requestBody);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_UPDATE_SAFETY_PLANNING);

		Account account = getCurrentContext().getAccount().get();
		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		UpdatePatientOrderSafetyPlanningStatusRequest request = getRequestBodyParser().parse(requestBody, UpdatePatientOrderSafetyPlanningStatusRequest.class);
		request.setPatientOrderId(patientOrder.getPatientOrderId());
		request.setAccountId(account.getAccountId());

		getPatientOrderService().updatePatientOrderSafetyPlanningStatus(request);

		PatientOrder updatedPatientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).get();
		PatientOrderApiResponseFormat responseFormat = PatientOrderApiResponseFormat.fromRoleId(account.getRoleId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrder", getPatientOrderApiResponseFactory().create(updatedPatientOrder, responseFormat,
					Set.of(PatientOrderApiResponseSupplement.EVERYTHING)));
		}});
	}

	@Nonnull
	@GET("/integrated-care/epic-departments")
	@AuthenticationRequired
	public ApiResponse epicDepartments() {
		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = account.getInstitutionId();

		if (!getAuthorizationService().canAdministerIcDepartments(institutionId, account))
			throw new AuthorizationException();

		List<EpicDepartment> epicDepartments = getProviderService().findEpicDepartmentsByInstitutionId(institutionId);

		return new ApiResponse(Map.of(
				"epicDepartments", epicDepartments.stream()
						.map(epicDepartment -> getEpicDepartmentApiResponseFactory().create(epicDepartment))
						.collect(Collectors.toList())
		));
	}

	@Nonnull
	@PUT("/integrated-care/epic-departments/{epicDepartmentId}")
	@AuthenticationRequired
	public ApiResponse updateEpicDepartment(@Nonnull @PathParameter UUID epicDepartmentId,
																					@Nonnull @RequestBody String requestBody) {
		requireNonNull(epicDepartmentId);
		requireNonNull(requestBody);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.EPIC_DEPARTMENT_UPDATE);

		Account account = getCurrentContext().getAccount().get();
		EpicDepartment epicDepartment = getProviderService().findEpicDepartmentById(epicDepartmentId).orElse(null);

		if (epicDepartment == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canAdministerIcDepartments(epicDepartment.getInstitutionId(), account))
			throw new AuthorizationException();

		UpdateEpicDepartmentRequest request = getRequestBodyParser().parse(requestBody, UpdateEpicDepartmentRequest.class);
		request.setEpicDepartmentId(epicDepartmentId);

		getProviderService().updateEpicDepartment(request);

		EpicDepartment updatedEpicDepartment = getProviderService().findEpicDepartmentById(epicDepartmentId).get();

		return new ApiResponse(Map.of(
				"epicDepartment", getEpicDepartmentApiResponseFactory().create(updatedEpicDepartment)
		));
	}

	@Nonnull
	@GET("/patient-orders/{patientOrderId}/encounters")
	@AuthenticationRequired
	public ApiResponse patientOrderEncounters(@Nonnull @PathParameter UUID patientOrderId) {
		requireNonNull(patientOrderId);

		Account account = getCurrentContext().getAccount().get();

		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		List<Encounter> encounters = getPatientOrderService().findEncountersByPatientOrderId(patientOrderId);

		return new ApiResponse(Map.of(
				"encounters", encounters.stream()
						.map(encounter -> getEncounterApiResponseFactory().create(encounter))
						.collect(Collectors.toList())
		));
	}

	@Nonnull
	@PUT("/patient-orders/{patientOrderId}/encounter-csn")
	@AuthenticationRequired
	public ApiResponse updatePatientOrderEncounterCsn(@Nonnull @PathParameter UUID patientOrderId,
																										@Nonnull @RequestBody String requestBody) {
		requireNonNull(patientOrderId);
		requireNonNull(requestBody);

		getSystemService().applyFootprintEventGroupToCurrentTransaction(FootprintEventGroupTypeId.PATIENT_ORDER_UPDATE_ENCOUNTER);

		Account account = getCurrentContext().getAccount().get();
		RawPatientOrder patientOrder = getPatientOrderService().findRawPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canUpdatePatientOrderEncounterCsn(patientOrder, account))
			throw new AuthorizationException();

		UpdatePatientOrderEncounterCsnRequest request = getRequestBodyParser().parse(requestBody, UpdatePatientOrderEncounterCsnRequest.class);
		request.setPatientOrderId(patientOrderId);

		getPatientOrderService().updatePatientOrderEncounterCsn(request);

		PatientOrder updatedPatientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).get();
		PatientOrderApiResponseFormat responseFormat = PatientOrderApiResponseFormat.fromRoleId(account.getRoleId());

		return new ApiResponse(Map.of(
				"patientOrder", getPatientOrderApiResponseFactory().create(updatedPatientOrder, responseFormat, Set.of(PatientOrderApiResponseSupplement.EVERYTHING))
		));
	}

	@Nonnull
	@GET("/patient-orders/reference-data")
	@AuthenticationRequired
	public ApiResponse patientOrderReferenceData() {
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		// Time zones
		List<TimeZoneApiResponse> timeZones = getAccountService().getAccountTimeZones().stream()
				.map(timeZone -> getTimeZoneApiResponseFactory().create(timeZone))
				.collect(Collectors.toList());

		Collections.sort(timeZones, (tz1, tz2) -> {
			return tz1.getDescription().compareTo(tz2.getDescription());
		});

		// Countries
		Set<Locale> countryLocales = getAccountService().getAccountCountries();
		List<CountryApiResponse> countries = new ArrayList<>(countryLocales.size());

		for (Locale locale : countryLocales)
			countries.add(getCountryApiResponseFactory().create(locale));

		Collections.sort(countries, (country1, country2) -> {
			return country1.getDescription().compareTo(country2.getDescription());
		});

		// Languages
		List<LanguageApiResponse> languages = getAccountService().getAccountLanguages().stream()
				.map(language -> getLanguageApiResponseFactory().create(language))
				.collect(Collectors.toList());

		Collections.sort(languages, (language1, language2) -> {
			return language1.getDescription().compareTo(language2.getDescription());
		});

		// Regions
		Map<String, List<Region>> regionsByCountryCode = Region.getRegionsByCountryCode();
		Map<String, List<Map<String, Object>>> normalizedRegionsByCountryCode = new HashMap<>(regionsByCountryCode.size());

		for (Map.Entry<String, List<Region>> entry : regionsByCountryCode.entrySet())
			normalizedRegionsByCountryCode.put(entry.getKey(), entry.getValue().stream()
					.map(region -> Map.of("name", (Object) region.getName(), "abbreviation", region.getAbbreviation()))
					.collect(Collectors.toList()));

		// Demographics
		List<Map<String, Object>> genderIdentities = getAccountService().findGenderIdentities().stream()
				.filter(genderIdentity -> genderIdentity.getGenderIdentityId() != GenderIdentity.GenderIdentityId.NOT_ASKED)
				.map(genderIdentity -> Map.<String, Object>of("genderIdentityId", genderIdentity.getGenderIdentityId(), "description", genderIdentity.getDescription()))
				.collect(Collectors.toList());

		List<Map<String, Object>> races = getAccountService().findRaces().stream()
				.filter(race -> race.getRaceId() != RaceId.NOT_ASKED)
				.map(race -> Map.<String, Object>of("raceId", race.getRaceId(), "description", race.getDescription()))
				.collect(Collectors.toList());

		List<Map<String, Object>> birthSexes = getAccountService().findBirthSexes().stream()
				.filter(birthSex -> birthSex.getBirthSexId() != BirthSexId.NOT_ASKED)
				.map(birthSex -> Map.<String, Object>of("birthSexId", birthSex.getBirthSexId(), "description", birthSex.getDescription()))
				.collect(Collectors.toList());

		List<Map<String, Object>> ethnicities = getAccountService().findEthnicities().stream()
				.filter(ethnicity -> ethnicity.getEthnicityId() != EthnicityId.NOT_ASKED)
				.map(ethnicity -> Map.<String, Object>of("ethnicityId", ethnicity.getEthnicityId(), "description", ethnicity.getDescription()))
				.collect(Collectors.toList());

		// Screening data
		List<ScreeningTypeApiResponse> screeningTypes = getScreeningService().findScreeningTypes().stream()
				.map(screeningType -> getScreeningTypeApiResponseFactory().create(screeningType))
				.collect(Collectors.toList());

		List<Map<PatientOrderTriageStatusId, Object>> patientOrderTriageStatuses = List.of(
				Map.of(PatientOrderTriageStatusId.NOT_TRIAGED, getStrings().get("Not Triaged")),
				Map.of(PatientOrderTriageStatusId.SPECIALTY_CARE, getStrings().get("Specialty Care")),
				Map.of(PatientOrderTriageStatusId.MHP, getStrings().get("MHP")),
				Map.of(PatientOrderTriageStatusId.SUBCLINICAL, getStrings().get("Subclinical"))
		);

		List<Map<String, Object>> patientOrderDispositions = getPatientOrderService().findPatientOrderDispositions().stream()
				.map(patientOrderDisposition -> {
					Map<String, Object> patientOrderDispositionJson = new HashMap<>();
					patientOrderDispositionJson.put("patientOrderDispositionId", patientOrderDisposition.getPatientOrderDispositionId());
					patientOrderDispositionJson.put("description", patientOrderDisposition.getDescription());
					return patientOrderDispositionJson;
				})
				.collect(Collectors.toList());

		List<Map<String, Object>> patientOrderOutreachResults = getPatientOrderService().findPatientOrderOutreachResults().stream()
				.map(patientOrderOutreachResult -> {
					Map<String, Object> patientOrderOutreachResultJson = new HashMap<>();
					patientOrderOutreachResultJson.put("patientOrderOutreachResultId", patientOrderOutreachResult.getPatientOrderOutreachResultId());
					patientOrderOutreachResultJson.put("patientOrderOutreachTypeId", patientOrderOutreachResult.getPatientOrderOutreachTypeId());
					patientOrderOutreachResultJson.put("patientOrderOutreachTypeDescription", patientOrderOutreachResult.getPatientOrderOutreachTypeDescription());
					patientOrderOutreachResultJson.put("patientOrderOutreachResultTypeId", patientOrderOutreachResult.getPatientOrderOutreachResultTypeId());
					patientOrderOutreachResultJson.put("patientOrderOutreachResultTypeDescription", patientOrderOutreachResult.getPatientOrderOutreachResultTypeDescription());
					patientOrderOutreachResultJson.put("patientOrderOutreachResultStatusId", patientOrderOutreachResult.getPatientOrderOutreachResultStatusId());
					patientOrderOutreachResultJson.put("patientOrderOutreachResultStatusDescription", patientOrderOutreachResult.getPatientOrderOutreachResultStatusDescription());
					return patientOrderOutreachResultJson;
				})
				.collect(Collectors.toList());

		List<Map<String, Object>> patientOrderScheduledMessageTypes = getPatientOrderService().findPatientOrderScheduledMessageTypes().stream()
				.map(patientOrderScheduledMessageType -> {
					Map<String, Object> patientOrderScheduledMessageTypeJson = new HashMap<>();
					patientOrderScheduledMessageTypeJson.put("patientOrderScheduledMessageTypeId", patientOrderScheduledMessageType.getPatientOrderScheduledMessageTypeId());
					patientOrderScheduledMessageTypeJson.put("description", patientOrderScheduledMessageType.getDescription());
					return patientOrderScheduledMessageTypeJson;
				})
				.collect(Collectors.toList());

		List<Map<String, Object>> patientOrderCareTypes = getPatientOrderService().findPatientOrderCareTypes().stream()
				.map(patientOrderCareType -> {
					Map<String, Object> patientOrderCareTypeJson = new HashMap<>();
					patientOrderCareTypeJson.put("patientOrderCareTypeId", patientOrderCareType.getPatientOrderCareTypeId());
					patientOrderCareTypeJson.put("description", patientOrderCareType.getDescription());
					return patientOrderCareTypeJson;
				})
				.collect(Collectors.toList());

		List<Map<String, Object>> patientOrderFocusTypes = getPatientOrderService().findPatientOrderFocusTypes().stream()
				.map(patientOrderFocusType -> {
					Map<String, Object> patientOrderFocusTypeJson = new HashMap<>();
					patientOrderFocusTypeJson.put("patientOrderFocusTypeId", patientOrderFocusType.getPatientOrderFocusTypeId());
					patientOrderFocusTypeJson.put("description", patientOrderFocusType.getDescription());
					return patientOrderFocusTypeJson;
				})
				.collect(Collectors.toList());

		List<Map<String, Object>> patientOrderResourcingTypes = getPatientOrderService().findPatientOrderResourcingTypesByInstitutionId(institutionId).stream()
				.map(patientOrderResourcingType -> {
					Map<String, Object> patientOrderResourcingTypeJson = new HashMap<>();
					patientOrderResourcingTypeJson.put("patientOrderResourcingTypeId", patientOrderResourcingType.getPatientOrderResourcingTypeId());
					patientOrderResourcingTypeJson.put("description", patientOrderResourcingType.getDescription());
					return patientOrderResourcingTypeJson;
				})
				.collect(Collectors.toList());

		List<ReferringPractice> referringPractices = getPatientOrderService().findReferringPracticesByInstitutionId(institutionId);

		List<Map<String, Object>> patientOrderReferralReasons = getPatientOrderService().findPatientOrderReferralReasonsByInstitutionId(institutionId).stream()
				.map(patientOrderReferralReason -> {
					Map<String, Object> patientOrderReferralReasonJson = new HashMap<>();
					patientOrderReferralReasonJson.put("patientOrderReferralReasonId", patientOrderReferralReason.getPatientOrderReferralReasonId());
					patientOrderReferralReasonJson.put("description", patientOrderReferralReason.getDescription());
					return patientOrderReferralReasonJson;
				})
				.collect(Collectors.toList());

		List<String> primaryPayorNames = getPatientOrderService().findPrimaryPayorNamesByInstitutionId(institutionId);

		List<Map<String, Object>> patientOrderCarePreferences = getPatientOrderService().findPatientOrderCarePreferencesByInstitutionId(institutionId).stream()
				.map(patientOrderCarePreference -> {
					Map<String, Object> patientOrderCarePreferenceJson = new HashMap<>();
					patientOrderCarePreferenceJson.put("patientOrderCarePreferenceId", patientOrderCarePreference.getPatientOrderCarePreferenceId());
					patientOrderCarePreferenceJson.put("description", patientOrderCarePreference.getDescription());
					return patientOrderCarePreferenceJson;
				})
				.collect(Collectors.toList());

		List<Map<String, Object>> patientOrderTriageOverrideReasons = getPatientOrderService().findPatientOrderTriageOverrideReasonsByInstitutionId(institutionId).stream()
				.map(patientOrderTriageOverrideReason -> {
					Map<String, Object> patientOrderTriageOverrideReasonJson = new HashMap<>();
					patientOrderTriageOverrideReasonJson.put("patientOrderTriageOverrideReasonId", patientOrderTriageOverrideReason.getPatientOrderTriageOverrideReasonId());
					patientOrderTriageOverrideReasonJson.put("description", patientOrderTriageOverrideReason.getDescription());
					return patientOrderTriageOverrideReasonJson;
				})
				.collect(Collectors.toList());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("timeZones", timeZones);
			put("countries", countries);
			put("languages", languages);
			put("genderIdentities", genderIdentities);
			put("races", races);
			put("birthSexes", birthSexes);
			put("ethnicities", ethnicities);
			put("regionsByCountryCode", normalizedRegionsByCountryCode);
			put("screeningTypes", screeningTypes);
			put("patientOrderTriageStatuses", patientOrderTriageStatuses);
			put("patientOrderDispositions", patientOrderDispositions);
			put("patientOrderOutreachResults", patientOrderOutreachResults);
			put("patientOrderScheduledMessageTypes", patientOrderScheduledMessageTypes);
			put("patientOrderCareTypes", patientOrderCareTypes);
			put("patientOrderFocusTypes", patientOrderFocusTypes);
			put("referringPractices", referringPractices);
			put("patientOrderReferralReasons", patientOrderReferralReasons);
			put("patientOrderResourcingTypes", patientOrderResourcingTypes);
			put("patientOrderCarePreferences", patientOrderCarePreferences);
			put("primaryPayorNames", primaryPayorNames);
			put("patientOrderTriageOverrideReasons", patientOrderTriageOverrideReasons);
		}});
	}

	@Nonnull
	@GET("/patient-order-csv-generator")
	@AuthenticationRequired
	public CustomResponse patientOrderCsvGenerator(@Nonnull @QueryParameter Optional<Integer> orderCount,
																								 @Nonnull HttpServletResponse httpServletResponse) throws IOException {
		requireNonNull(orderCount);

		if (!getConfiguration().getShouldEnableIcDebugging())
			throw new IllegalStateException("Cannot call this unless IC debugging is enabled");

		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canImportPatientOrders(account.getInstitutionId(), account))
			throw new AuthorizationException();

		int finalOrderCount = orderCount.orElse(100);

		if (finalOrderCount < 1)
			finalOrderCount = 100;
		else if (finalOrderCount > 5_000)
			finalOrderCount = 5_000;

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss", account.getLocale()).withZone(account.getTimeZone());
		String filename = format("Cobalt Test Order Report - %s - %d Rows.csv", dateTimeFormatter.format(Instant.now()), finalOrderCount);

		httpServletResponse.setContentType("text/csv");
		httpServletResponse.setHeader("Content-Encoding", "gzip");
		httpServletResponse.setHeader("Content-Disposition", format("attachment; filename=\"%s\"", filename));

		try (PrintWriter printWriter = new PrintWriter(new GZIPOutputStream(httpServletResponse.getOutputStream()))) {
			getPatientOrderCsvGenerator().generateCsv(finalOrderCount, printWriter);
		}

		return CustomResponse.instance();
	}

	@Nonnull
	protected PatientOrderService getPatientOrderService() {
		return this.patientOrderService;
	}

	@Nonnull
	protected ProviderService getProviderService() {
		return this.providerService;
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountService;
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
	protected ScreeningService getScreeningService() {
		return this.screeningService;
	}

	@Nonnull
	protected SystemService getSystemService() {
		return this.systemService;
	}

	@Nonnull
	protected PatientOrderApiResponseFactory getPatientOrderApiResponseFactory() {
		return this.patientOrderApiResponseFactory;
	}

	@Nonnull
	protected PatientOrderNoteApiResponseFactory getPatientOrderNoteApiResponseFactory() {
		return this.patientOrderNoteApiResponseFactory;
	}

	@Nonnull
	protected PatientOrderOutreachApiResponseFactory getPatientOrderOutreachApiResponseFactory() {
		return this.patientOrderOutreachApiResponseFactory;
	}

	@Nonnull
	protected EpicDepartmentApiResponseFactory getEpicDepartmentApiResponseFactory() {
		return this.epicDepartmentApiResponseFactory;
	}

	@Nonnull
	protected EncounterApiResponseFactory getEncounterApiResponseFactory() {
		return this.encounterApiResponseFactory;
	}

	@Nonnull
	protected AccountApiResponseFactory getAccountApiResponseFactory() {
		return this.accountApiResponseFactory;
	}

	@Nonnull
	protected TimeZoneApiResponseFactory getTimeZoneApiResponseFactory() {
		return this.timeZoneApiResponseFactory;
	}

	@Nonnull
	protected LanguageApiResponseFactory getLanguageApiResponseFactory() {
		return this.languageApiResponseFactory;
	}

	@Nonnull
	protected CountryApiResponseFactory getCountryApiResponseFactory() {
		return this.countryApiResponseFactory;
	}

	@Nonnull
	protected PatientOrderAutocompleteResultApiResponseFactory getPatientOrderAutocompleteResultApiResponseFactory() {
		return this.patientOrderAutocompleteResultApiResponseFactory;
	}

	@Nonnull
	protected PatientOrderScheduledScreeningApiResponseFactory getPatientOrderScheduledScreeningApiResponseFactory() {
		return this.patientOrderScheduledScreeningApiResponseFactory;
	}

	@Nonnull
	protected ScreeningTypeApiResponseFactory getScreeningTypeApiResponseFactory() {
		return this.screeningTypeApiResponseFactory;
	}

	@Nonnull
	protected PatientOrderVoicemailTaskApiResponseFactory getPatientOrderVoicemailTaskApiResponseFactory() {
		return this.patientOrderVoicemailTaskApiResponseFactory;
	}

	@Nonnull
	protected PatientOrderScheduledOutreachApiResponseFactory getPatientOrderScheduledOutreachApiResponseFactory() {
		return this.patientOrderScheduledOutreachApiResponseFactory;
	}

	@Nonnull
	protected PatientOrderCsvGenerator getPatientOrderCsvGenerator() {
		return this.patientOrderCsvGenerator;
	}

	@Nonnull
	protected RequestBodyParser getRequestBodyParser() {
		return this.requestBodyParser;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return this.jsonMapper;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return this.formatter;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
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
