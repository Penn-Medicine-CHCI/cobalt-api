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
import com.cobaltplatform.api.model.api.request.CreatePatientOrderImportRequest;
import com.cobaltplatform.api.model.api.request.CreatePatientOrderNoteRequest;
import com.cobaltplatform.api.model.api.request.DeletePatientOrderNoteRequest;
import com.cobaltplatform.api.model.api.request.FindPatientOrdersRequest;
import com.cobaltplatform.api.model.api.request.UpdatePatientOrderNoteRequest;
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
import com.cobaltplatform.api.model.api.response.PatientOrderNoteApiResponse.PatientOrderNoteApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TimeZoneApiResponse;
import com.cobaltplatform.api.model.api.response.TimeZoneApiResponse.TimeZoneApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.BirthSex;
import com.cobaltplatform.api.model.db.Ethnicity;
import com.cobaltplatform.api.model.db.GenderIdentity;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.PatientOrderImportType.PatientOrderImportTypeId;
import com.cobaltplatform.api.model.db.PatientOrderNote;
import com.cobaltplatform.api.model.db.Race;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.model.service.PatientOrderPanelTypeId;
import com.cobaltplatform.api.model.service.Region;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.service.PatientOrderService;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.web.request.RequestBodyParser;
import com.soklet.web.annotation.DELETE;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.POST;
import com.soklet.web.annotation.PUT;
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
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
	private final PatientOrderApiResponseFactory patientOrderApiResponseFactory;
	@Nonnull
	private final PatientOrderNoteApiResponseFactory patientOrderNoteApiResponseFactory;
	@Nonnull
	private final AccountApiResponseFactory accountApiResponseFactory;
	@Nonnull
	private final TimeZoneApiResponseFactory timeZoneApiResponseFactory;
	@Nonnull
	private final LanguageApiResponseFactory languageApiResponseFactory;
	@Nonnull
	private final CountryApiResponseFactory countryApiResponseFactory;
	@Nonnull
	private final InsuranceApiResponseFactory insuranceApiResponseFactory;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final Logger logger;

	@Inject
	public PatientOrderResource(@Nonnull PatientOrderService patientOrderService,
															@Nonnull AccountService accountService,
															@Nonnull InstitutionService institutionService,
															@Nonnull AuthorizationService authorizationService,
															@Nonnull PatientOrderApiResponseFactory patientOrderApiResponseFactory,
															@Nonnull PatientOrderNoteApiResponseFactory patientOrderNoteApiResponseFactory,
															@Nonnull AccountApiResponseFactory accountApiResponseFactory,
															@Nonnull TimeZoneApiResponseFactory timeZoneApiResponseFactory,
															@Nonnull LanguageApiResponseFactory languageApiResponseFactory,
															@Nonnull CountryApiResponseFactory countryApiResponseFactory,
															@Nonnull InsuranceApiResponseFactory insuranceApiResponseFactory,
															@Nonnull RequestBodyParser requestBodyParser,
															@Nonnull Formatter formatter,
															@Nonnull Provider<CurrentContext> currentContextProvider) {
		requireNonNull(patientOrderService);
		requireNonNull(accountService);
		requireNonNull(institutionService);
		requireNonNull(authorizationService);
		requireNonNull(patientOrderApiResponseFactory);
		requireNonNull(patientOrderNoteApiResponseFactory);
		requireNonNull(accountApiResponseFactory);
		requireNonNull(timeZoneApiResponseFactory);
		requireNonNull(languageApiResponseFactory);
		requireNonNull(countryApiResponseFactory);
		requireNonNull(insuranceApiResponseFactory);
		requireNonNull(requestBodyParser);
		requireNonNull(formatter);
		requireNonNull(currentContextProvider);

		this.patientOrderService = patientOrderService;
		this.accountService = accountService;
		this.institutionService = institutionService;
		this.authorizationService = authorizationService;
		this.patientOrderApiResponseFactory = patientOrderApiResponseFactory;
		this.patientOrderNoteApiResponseFactory = patientOrderNoteApiResponseFactory;
		this.accountApiResponseFactory = accountApiResponseFactory;
		this.timeZoneApiResponseFactory = timeZoneApiResponseFactory;
		this.languageApiResponseFactory = languageApiResponseFactory;
		this.countryApiResponseFactory = countryApiResponseFactory;
		this.insuranceApiResponseFactory = insuranceApiResponseFactory;
		this.requestBodyParser = requestBodyParser;
		this.formatter = formatter;
		this.currentContextProvider = currentContextProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/patient-orders/{patientOrderId}")
	@AuthenticationRequired
	public ApiResponse patientOrder(@Nonnull @PathParameter UUID patientOrderId) {
		requireNonNull(patientOrderId);

		Account account = getCurrentContext().getAccount().get();
		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(patientOrderId).orElse(null);

		if (patientOrder == null)
			throw new NotFoundException();

		if (!getAuthorizationService().canViewPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrder", getPatientOrderApiResponseFactory().create(patientOrder,
					PatientOrderApiResponseFormat.fromRoleId(account.getRoleId()),
					Set.of(PatientOrderApiResponseSupplement.EVERYTHING)));
		}});
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

		for (Entry<String, List<Region>> entry : regionsByCountryCode.entrySet())
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
		}});
	}

	@Nonnull
	@GET("/patient-orders")
	@AuthenticationRequired
	public ApiResponse findPatientOrders(@Nonnull @QueryParameter Optional<PatientOrderPanelTypeId> patientOrderPanelTypeId,
																			 @Nonnull @QueryParameter Optional<UUID> panelAccountId,
																			 @Nonnull @QueryParameter Optional<String> searchQuery,
																			 @Nonnull @QueryParameter Optional<Integer> pageNumber,
																			 @Nonnull @QueryParameter Optional<Integer> pageSize) {
		requireNonNull(patientOrderPanelTypeId);
		requireNonNull(panelAccountId);
		requireNonNull(searchQuery);
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
				setPatientOrderPanelTypeId(patientOrderPanelTypeId.orElse(null));
				setPanelAccountId(panelAccountId.orElse(null));
				setSearchQuery(searchQuery.orElse(null));
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

		return new ApiResponse(new HashMap<String, Object>() {{
			put("findResult", findResultJson);
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

		getPatientOrderService().createPatientOrderImport(request);

		return new ApiResponse();
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

		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(patientOrderNoteId).orElse(null);

		if (patientOrder != null && !getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		getPatientOrderService().updatePatientOrderNote(request);
		PatientOrderNote patientOrderNote = getPatientOrderService().findPatientOrderNoteById(patientOrderNoteId).get();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("patientOrderNote", getPatientOrderNoteApiResponseFactory().create(patientOrderNote));
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

		PatientOrder patientOrder = getPatientOrderService().findPatientOrderById(patientOrderNoteId).orElse(null);

		if (patientOrder != null && !getAuthorizationService().canEditPatientOrder(patientOrder, account))
			throw new AuthorizationException();

		getPatientOrderService().deletePatientOrderNote(request);

		return new ApiResponse(); // 204
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

		Map<UUID, Integer> activePatientOrderCountsByPanelAccountId = getPatientOrderService().findActivePatientOrderCountsByPanelAccountIdForInstitutionId(institutionId);

		// If there are any "holes" in the mapping of panel account IDs -> active order counts,
		// fill in the holes with 0-counts.
		for (AccountApiResponse panelAccount : panelAccounts)
			if (!activePatientOrderCountsByPanelAccountId.containsKey(panelAccount.getAccountId()))
				activePatientOrderCountsByPanelAccountId.put(panelAccount.getAccountId(), 0);

		Map<UUID, Map<String, Object>> activePatientOrderCountsByPanelAccountIdJson = new HashMap<>(activePatientOrderCountsByPanelAccountId.size());

		for (Entry<UUID, Integer> entry : activePatientOrderCountsByPanelAccountId.entrySet()) {
			UUID panelAccountId = entry.getKey();
			Integer activePatientOrderCount = entry.getValue();
			activePatientOrderCountsByPanelAccountIdJson.put(panelAccountId, Map.of(
					"activePatientOrderCount", activePatientOrderCount,
					"activePatientOrderCountDescription", getFormatter().formatNumber(activePatientOrderCount)
			));
		}

		return new ApiResponse(new HashMap<String, Object>() {{
			put("panelAccounts", panelAccounts);
			put("activePatientOrderCountsByPanelAccountId", activePatientOrderCountsByPanelAccountIdJson);
		}});
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
	protected PatientOrderApiResponseFactory getPatientOrderApiResponseFactory() {
		return this.patientOrderApiResponseFactory;
	}

	@Nonnull
	protected PatientOrderNoteApiResponseFactory getPatientOrderNoteApiResponseFactory() {
		return this.patientOrderNoteApiResponseFactory;
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
	protected InsuranceApiResponseFactory getInsuranceApiResponseFactory() {
		return this.insuranceApiResponseFactory;
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
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
