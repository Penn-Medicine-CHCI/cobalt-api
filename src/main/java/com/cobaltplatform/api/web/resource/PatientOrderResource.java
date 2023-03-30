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
import com.cobaltplatform.api.model.api.request.ClosePatientOrderRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderImportRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderNoteRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderOutreachRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderScheduledMessageGroupRequest;
import com.cobaltplatform.api.model.api.request.DeletePatientOrderNoteRequest;
import com.cobaltplatform.api.model.api.request.DeletePatientOrderOutreachRequest;
import com.cobaltplatform.api.model.api.request.FindPatientOrdersRequest;
import com.cobaltplatform.api.model.api.request.OpenPatientOrderRequest;
import com.cobaltplatform.api.model.api.request.PatchPatientOrderRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderNoteRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderOutreachRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderResourcingStatusRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderSafetyPlanningStatusRequest;
import com.cobaltplatform.api.model.api.response.AccountApiResponse;
import com.cobaltplatform.api.model.api.response.AccountApiResponse.AccountApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CountryApiResponse;
import com.cobaltplatform.api.model.api.response.CountryApiResponse.CountryApiResponseFactory;
import com.cobaltplatform.api.model.api.response.InsuranceApiResponse;
import com.cobaltplatform.api.model.api.response.InsuranceApiResponse.InsuranceApiResponseFactory;
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
import com.cobaltplatform.api.model.api.response.PatientOrderTriageApiResponse.PatientOrderTriageApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TimeZoneApiResponse;
import com.cobaltplatform.api.model.api.response.TimeZoneApiResponse.TimeZoneApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.BirthSex;
import com.cobaltplatform.api.model.db.Ethnicity;
import com.cobaltplatform.api.model.db.GenderIdentity;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.PatientOrderClosureReason;
import com.cobaltplatform.api.model.db.PatientOrderDisposition.PatientOrderDispositionId;
import com.cobaltplatform.api.model.db.PatientOrderImportType.PatientOrderImportTypeId;
import com.cobaltplatform.api.model.db.PatientOrderNote;
import com.cobaltplatform.api.model.db.PatientOrderOutreach;
import com.cobaltplatform.api.model.db.PatientOrderScheduledMessage;
import com.cobaltplatform.api.model.db.PatientOrderStatus.PatientOrderStatusId;
import com.cobaltplatform.api.model.db.PatientOrderTriage;
import com.cobaltplatform.api.model.db.Race;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.model.service.PatientOrderAutocompleteResult;
import com.cobaltplatform.api.model.service.PatientOrderImportResult;
import com.cobaltplatform.api.model.service.Region;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.PatientOrderService;
import com.cobaltplatform.api.service.ScreeningService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.PatientOrderCsvGenerator;
import com.cobaltplatform.api.web.request.RequestBodyParser;
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
	private final AccountService accountService;
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final ScreeningService screeningService;
	@Nonnull
	private final PatientOrderApiResponseFactory patientOrderApiResponseFactory;
	@Nonnull
	private final PatientOrderNoteApiResponseFactory patientOrderNoteApiResponseFactory;
	@Nonnull
	private final PatientOrderOutreachApiResponseFactory patientOrderOutreachApiResponseFactory;
	@Nonnull
	private final AccountApiResponseFactory accountApiResponseFactory;
	@Nonnull
	private final PatientOrderTriageApiResponseFactory patientOrderTriageApiResponseFactory;
	@Nonnull
	private final TimeZoneApiResponseFactory timeZoneApiResponseFactory;
	@Nonnull
	private final LanguageApiResponseFactory languageApiResponseFactory;
	@Nonnull
	private final CountryApiResponseFactory countryApiResponseFactory;
	@Nonnull
	private final InsuranceApiResponseFactory insuranceApiResponseFactory;
	@Nonnull
	private final PatientOrderAutocompleteResultApiResponseFactory patientOrderAutocompleteResultApiResponseFactory;
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
	private final Logger logger;

	@Inject
	public PatientOrderResource(@Nonnull PatientOrderService patientOrderService,
															@Nonnull AccountService accountService,
															@Nonnull InstitutionService institutionService,
															@Nonnull AuthorizationService authorizationService,
															@Nonnull ScreeningService screeningService,
															@Nonnull PatientOrderApiResponseFactory patientOrderApiResponseFactory,
															@Nonnull PatientOrderNoteApiResponseFactory patientOrderNoteApiResponseFactory,
															@Nonnull PatientOrderOutreachApiResponseFactory patientOrderOutreachApiResponseFactory,
															@Nonnull AccountApiResponseFactory accountApiResponseFactory,
															@Nonnull PatientOrderTriageApiResponseFactory patientOrderTriageApiResponseFactory,
															@Nonnull TimeZoneApiResponseFactory timeZoneApiResponseFactory,
															@Nonnull LanguageApiResponseFactory languageApiResponseFactory,
															@Nonnull CountryApiResponseFactory countryApiResponseFactory,
															@Nonnull InsuranceApiResponseFactory insuranceApiResponseFactory,
															@Nonnull PatientOrderAutocompleteResultApiResponseFactory patientOrderAutocompleteResultApiResponseFactory,
															@Nonnull PatientOrderCsvGenerator patientOrderCsvGenerator,
															@Nonnull RequestBodyParser requestBodyParser,
															@Nonnull JsonMapper jsonMapper,
															@Nonnull Formatter formatter,
															@Nonnull Configuration configuration,
															@Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(patientOrderService);
		requireNonNull(accountService);
		requireNonNull(institutionService);
		requireNonNull(authorizationService);
		requireNonNull(screeningService);
		requireNonNull(patientOrderApiResponseFactory);
		requireNonNull(patientOrderNoteApiResponseFactory);
		requireNonNull(patientOrderOutreachApiResponseFactory);
		requireNonNull(accountApiResponseFactory);
		requireNonNull(patientOrderTriageApiResponseFactory);
		requireNonNull(timeZoneApiResponseFactory);
		requireNonNull(languageApiResponseFactory);
		requireNonNull(countryApiResponseFactory);
		requireNonNull(insuranceApiResponseFactory);
		requireNonNull(patientOrderAutocompleteResultApiResponseFactory);
		requireNonNull(patientOrderCsvGenerator);
		requireNonNull(requestBodyParser);
		requireNonNull(jsonMapper);
		requireNonNull(formatter);
		requireNonNull(configuration);
		requireNonNull(currentContextProvider);

		this.patientOrderService = patientOrderService;
		this.accountService = accountService;
		this.institutionService = institutionService;
		this.authorizationService = authorizationService;
		this.screeningService = screeningService;
		this.patientOrderApiResponseFactory = patientOrderApiResponseFactory;
		this.patientOrderNoteApiResponseFactory = patientOrderNoteApiResponseFactory;
		this.patientOrderOutreachApiResponseFactory = patientOrderOutreachApiResponseFactory;
		this.accountApiResponseFactory = accountApiResponseFactory;
		this.patientOrderTriageApiResponseFactory = patientOrderTriageApiResponseFactory;
		this.timeZoneApiResponseFactory = timeZoneApiResponseFactory;
		this.languageApiResponseFactory = languageApiResponseFactory;
		this.countryApiResponseFactory = countryApiResponseFactory;
		this.insuranceApiResponseFactory = insuranceApiResponseFactory;
		this.patientOrderAutocompleteResultApiResponseFactory = patientOrderAutocompleteResultApiResponseFactory;
		this.patientOrderCsvGenerator = patientOrderCsvGenerator;
		this.requestBodyParser = requestBodyParser;
		this.jsonMapper = jsonMapper;
		this.formatter = formatter;
		this.configuration = configuration;
		this.currentContextProvider = currentContextProvider;
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

		List<PatientOrder> associatedPatientOrders = finalResponseSupplements.contains(PatientOrderApiResponseSupplement.EVERYTHING)
				? getPatientOrderService().findPatientOrdersByMrnAndInstitutionId(patientOrder.getPatientMrn(), account.getInstitutionId()).stream()
				.filter(associatedPatientOrder -> !associatedPatientOrder.getPatientOrderId().equals(patientOrderId))
				.sorted((patientOrder1, patientOrder2) -> patientOrder2.getOrderDate().compareTo(patientOrder1.getOrderDate()))
				.collect(Collectors.toList())
				: List.of();

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
		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

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

		getPatientOrderService().patchPatientOrder(request);

		PatientOrder updatedPatientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).get();
		PatientOrderApiResponseFormat responseFormat = PatientOrderApiResponseFormat.fromRoleId(account.getRoleId());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrder", getPatientOrderApiResponseFactory().create(updatedPatientOrder, responseFormat,
					Set.of(PatientOrderApiResponseSupplement.EVERYTHING)));
		}});
	}

	@Nonnull
	@GET("/patient-orders/open")
	@AuthenticationRequired
	public ApiResponse openPatientOrder(@Nonnull @QueryParameter Optional<UUID> accountId) {
		requireNonNull(accountId);

		// Pull the open order (if it exists) for the open account by default.
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

		Account account = getCurrentContext().getAccount().get();
		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).orElse(null);

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
	@PUT("/patient-orders/{patientOrderId}/open")
	@AuthenticationRequired
	public ApiResponse openPatientOrder(@Nonnull @PathParameter UUID patientOrderId) {
		requireNonNull(patientOrderId);

		Account account = getCurrentContext().getAccount().get();
		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).orElse(null);

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
	@GET("/patient-orders")
	@AuthenticationRequired
	public ApiResponse findPatientOrders(@Nonnull @QueryParameter Optional<PatientOrderDispositionId> patientOrderDispositionId,
																			 @Nonnull @QueryParameter("patientOrderStatusId") Optional<List<PatientOrderStatusId>> patientOrderStatusIds,
																			 @Nonnull @QueryParameter Optional<UUID> panelAccountId,
																			 @Nonnull @QueryParameter Optional<String> patientMrn,
																			 @Nonnull @QueryParameter Optional<Integer> pageNumber,
																			 @Nonnull @QueryParameter Optional<Integer> pageSize) {
		requireNonNull(patientOrderDispositionId);
		requireNonNull(patientOrderStatusIds);
		requireNonNull(panelAccountId);
		requireNonNull(patientMrn);
		requireNonNull(pageNumber);
		requireNonNull(pageSize);

		CurrentContext currentContext = getCurrentContext();
		Account account = currentContext.getAccount().get();
		InstitutionId institutionId = account.getInstitutionId();

		if (!getAuthorizationService().canViewPatientOrders(institutionId, account))
			throw new AuthorizationException();

		// If you want to look at another account's panel, make sure you're authorized to do so
		if (panelAccountId.isPresent()) {
			Account panelAccount = getAccountService().findAccountById(panelAccountId.orElse(null)).orElse(null);

			if (!getAuthorizationService().canViewPatientOrdersForPanelAccount(account, panelAccount))
				throw new AuthorizationException();
		}

		FindResult<PatientOrder> findResult = getPatientOrderService().findPatientOrders(new FindPatientOrdersRequest() {
			{
				setInstitutionId(account.getInstitutionId());
				setPatientOrderDispositionId(patientOrderDispositionId.orElse(null));
				setPatientOrderStatusIds(new HashSet<>(patientOrderStatusIds.orElse(List.of())));
				setPanelAccountId(panelAccountId.orElse(null));
				setPatientMrn(patientMrn.orElse(null));
				setPageNumber(pageNumber.orElse(0));
				setPageSize(pageSize.orElse(0));
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

		// If there's a panel account provided, return it
		Account panelAccount = getAccountService().findAccountById(panelAccountId.orElse(null)).orElse(null);

		// If there's a patient MRN provided, return it in the form of an autocomplete result.
		// We assume this one is just whatever the first result is...
		PatientOrderAutocompleteResult patientOrderAutocompleteResult = patientMrn.isPresent() ? getPatientOrderService().findPatientOrderAutocompleteResultByMrn(patientMrn.get(), institutionId).orElse(null) : null;

		return new ApiResponse(new HashMap<String, Object>() {{
			put("findResult", findResultJson);
			put("panelAccount", panelAccount == null ? null : getAccountApiResponseFactory().create(panelAccount));
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
	@GET("/patient-order-triages")
	@AuthenticationRequired
	public ApiResponse patientOrderTriages(@Nonnull @QueryParameter UUID patientOrderId,
																				 @Nonnull @QueryParameter Optional<UUID> screeningSessionId) {
		requireNonNull(patientOrderId);
		requireNonNull(screeningSessionId);

		Account account = getCurrentContext().getAccount().get();
		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canViewPatientOrderTriages(patientOrder, account))
			throw new AuthorizationException();

		List<PatientOrderTriage> patientOrderTriages = getPatientOrderService().findPatientOrderTriagesByPatientOrderId(patientOrderId, screeningSessionId.orElse(null));

		return new ApiResponse(new HashMap<>() {{
			put("patientOrderTriages", patientOrderTriages.stream()
					.map(patientOrderTriage -> getPatientOrderTriageApiResponseFactory().create(patientOrderTriage))
					.collect(Collectors.toList()));
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

		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).orElse(null);

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

		CreatePatientOrderNoteRequest request = getRequestBodyParser().parse(requestBody, CreatePatientOrderNoteRequest.class);
		request.setAccountId(account.getAccountId());

		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(request.getPatientOrderId()).orElse(null);

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

		Account account = getCurrentContext().getAccount().get();

		UpdatePatientOrderNoteRequest request = getRequestBodyParser().parse(requestBody, UpdatePatientOrderNoteRequest.class);
		request.setAccountId(account.getAccountId());
		request.setPatientOrderNoteId(patientOrderNoteId);

		PatientOrderNote patientOrderNote = getPatientOrderService().findPatientOrderNoteById(patientOrderNoteId).orElse(null);

		if (patientOrderNote == null)
			throw new NotFoundException();

		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(patientOrderNote.getPatientOrderId()).get();

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

		Account account = getCurrentContext().getAccount().get();

		DeletePatientOrderNoteRequest request = new DeletePatientOrderNoteRequest();
		request.setAccountId(account.getAccountId());
		request.setPatientOrderNoteId(patientOrderNoteId);

		PatientOrderNote patientOrderNote = getPatientOrderService().findPatientOrderNoteById(patientOrderNoteId).orElse(null);

		if (patientOrderNote == null)
			throw new NotFoundException();

		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(patientOrderNote.getPatientOrderId()).orElse(null);

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
	@GET("/patient-order-outreaches")
	@AuthenticationRequired
	public ApiResponse patientOrderOutreaches(@Nonnull @QueryParameter UUID patientOrderId) {
		requireNonNull(patientOrderId);

		Account account = getCurrentContext().getAccount().get();

		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).orElse(null);

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

		Account account = getCurrentContext().getAccount().get();

		CreatePatientOrderOutreachRequest request = getRequestBodyParser().parse(requestBody, CreatePatientOrderOutreachRequest.class);
		request.setAccountId(account.getAccountId());

		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(request.getPatientOrderId()).orElse(null);

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

		Account account = getCurrentContext().getAccount().get();

		UpdatePatientOrderOutreachRequest request = getRequestBodyParser().parse(requestBody, UpdatePatientOrderOutreachRequest.class);
		request.setAccountId(account.getAccountId());
		request.setPatientOrderOutreachId(patientOrderOutreachId);

		PatientOrderOutreach patientOrderOutreach = getPatientOrderService().findPatientOrderOutreachById(patientOrderOutreachId).orElse(null);

		if (patientOrderOutreach == null)
			throw new NotFoundException();

		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(patientOrderOutreach.getPatientOrderId()).get();

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

		Account account = getCurrentContext().getAccount().get();

		DeletePatientOrderOutreachRequest request = new DeletePatientOrderOutreachRequest();
		request.setAccountId(account.getAccountId());
		request.setPatientOrderOutreachId(patientOrderOutreachId);

		PatientOrderOutreach patientOrderOutreach = getPatientOrderService().findPatientOrderOutreachById(patientOrderOutreachId).orElse(null);

		if (patientOrderOutreach == null)
			throw new NotFoundException();

		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(patientOrderOutreach.getPatientOrderId()).get();

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

		Account account = getCurrentContext().getAccount().get();

		CreatePatientOrderScheduledMessageGroupRequest request = getRequestBodyParser().parse(requestBody, CreatePatientOrderScheduledMessageGroupRequest.class);
		request.setAccountId(account.getAccountId());

		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(request.getPatientOrderId()).orElse(null);

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
	@GET("/integrated-care/panel-accounts")
	@AuthenticationRequired
	public ApiResponse panelAccounts() {
		Account account = getCurrentContext().getAccount().get();
		InstitutionId institutionId = account.getInstitutionId();

		if (!getAuthorizationService().canViewPanelAccounts(institutionId, account))
			throw new AuthorizationException();

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

		int overallOpenPatientOrderCount = getPatientOrderService().findOpenPatientOrderCountByInstitutionId(account.getInstitutionId());
		String overallOpenPatientOrderCountDescription = getFormatter().formatNumber(overallOpenPatientOrderCount);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("panelAccounts", panelAccounts);
			put("openPatientOrderCountsByPanelAccountId", openPatientOrderCountsByPanelAccountIdJson);
			put("overallOpenPatientOrderCount", overallOpenPatientOrderCount);
			put("overallOpenPatientOrderCountDescription", overallOpenPatientOrderCountDescription);
		}});
	}

	@Nonnull
	@PUT("/patient-orders/{patientOrderId}/patient-order-resourcing-status")
	@AuthenticationRequired
	public ApiResponse updatePatientOrderResourcingStatus(@Nonnull @PathParameter UUID patientOrderId,
																												@Nonnull @RequestBody String requestBody) {
		requireNonNull(patientOrderId);
		requireNonNull(requestBody);

		Account account = getCurrentContext().getAccount().get();
		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).orElse(null);

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

		Account account = getCurrentContext().getAccount().get();
		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).orElse(null);

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
	@GET("/accounts/reference-data")
	@AuthenticationRequired
	@Deprecated // temporary until FE can transition to GET /patient-orders/reference-data
	public ApiResponse accountReferenceData() {
		return patientOrderReferenceData();
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

		// Insurances
		List<InsuranceApiResponse> insurances = getInstitutionService().findInsurancesByInstitutionId(institutionId).stream()
				.map(insurance -> getInsuranceApiResponseFactory().create(insurance))
				.collect(Collectors.toList());

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
				.filter(race -> race.getRaceId() != Race.RaceId.NOT_ASKED)
				.map(race -> Map.<String, Object>of("raceId", race.getRaceId(), "description", race.getDescription()))
				.collect(Collectors.toList());

		List<Map<String, Object>> birthSexes = getAccountService().findBirthSexes().stream()
				.filter(birthSex -> birthSex.getBirthSexId() != BirthSex.BirthSexId.NOT_ASKED)
				.map(birthSex -> Map.<String, Object>of("birthSexId", birthSex.getBirthSexId(), "description", birthSex.getDescription()))
				.collect(Collectors.toList());

		List<Map<String, Object>> ethnicities = getAccountService().findEthnicities().stream()
				.filter(ethnicity -> ethnicity.getEthnicityId() != Ethnicity.EthnicityId.NOT_ASKED)
				.map(ethnicity -> Map.<String, Object>of("ethnicityId", ethnicity.getEthnicityId(), "description", ethnicity.getDescription()))
				.collect(Collectors.toList());

		// Screening data
		List<Map<String, Object>> screeningTypes = getScreeningService().findScreeningTypes().stream()
				.map(screeningType -> {
					Map<String, Object> screeningTypeJson = new HashMap<>();
					screeningTypeJson.put("screeningTypeId", screeningType.getScreeningTypeId());
					screeningTypeJson.put("description", screeningType.getDescription());
					screeningTypeJson.put("overallScoreMaximum", screeningType.getOverallScoreMaximum());
					return screeningTypeJson;
				})
				.collect(Collectors.toList());

		List<Map<String, Object>> patientOrderStatuses = getPatientOrderService().findPatientOrderStatuses().stream()
				.map(patientOrderStatus -> {
					Map<String, Object> patientOrderStatusJson = new HashMap<>();
					patientOrderStatusJson.put("patientOrderStatusId", patientOrderStatus.getPatientOrderStatusId());
					patientOrderStatusJson.put("description", patientOrderStatus.getDescription());
					return patientOrderStatusJson;
				})
				.collect(Collectors.toList());

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

		return new ApiResponse(new HashMap<String, Object>() {{
			put("timeZones", timeZones);
			put("countries", countries);
			put("languages", languages);
			put("insurances", insurances);
			put("genderIdentities", genderIdentities);
			put("races", races);
			put("birthSexes", birthSexes);
			put("ethnicities", ethnicities);
			put("regionsByCountryCode", normalizedRegionsByCountryCode);
			put("screeningTypes", screeningTypes);
			put("patientOrderStatuses", patientOrderStatuses);
			put("patientOrderDispositions", patientOrderDispositions);
			put("patientOrderOutreachResults", patientOrderOutreachResults);
			put("patientOrderScheduledMessageTypes", patientOrderScheduledMessageTypes);
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
	protected AccountApiResponseFactory getAccountApiResponseFactory() {
		return this.accountApiResponseFactory;
	}

	@Nonnull
	protected PatientOrderTriageApiResponseFactory getPatientOrderTriageApiResponseFactory() {
		return this.patientOrderTriageApiResponseFactory;
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
	protected InsuranceApiResponseFactory getInsuranceApiResponseFactory() {
		return this.insuranceApiResponseFactory;
	}

	@Nonnull
	protected PatientOrderAutocompleteResultApiResponseFactory getPatientOrderAutocompleteResultApiResponseFactory() {
		return this.patientOrderAutocompleteResultApiResponseFactory;
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
	protected Logger getLogger() {
		return this.logger;
	}
}
