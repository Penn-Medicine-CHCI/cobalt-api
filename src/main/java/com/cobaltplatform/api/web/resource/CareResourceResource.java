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
import com.cobaltplatform.api.model.api.request.CreateCareResourceLocationForResourcePacket;
import com.cobaltplatform.api.model.api.request.CreateCareResourceLocationRequest;
import com.cobaltplatform.api.model.api.request.CreateCareResourceRequest;
import com.cobaltplatform.api.model.api.request.FindCareResourceLocationsRequest;
import com.cobaltplatform.api.model.api.request.FindCareResourcesRequest;
import com.cobaltplatform.api.model.api.request.UpdateCareResourceLocationForResourcePacket;
import com.cobaltplatform.api.model.api.request.UpdateCareResourceLocationNoteRequest;
import com.cobaltplatform.api.model.api.request.UpdateCareResourceLocationRequest;
import com.cobaltplatform.api.model.api.request.UpdateCareResourceRequest;
import com.cobaltplatform.api.model.api.response.CareResourceApiResponse.CareResourceApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CareResourceLocationApiResponse.CareResourceLocationApiResponseBatchContext;
import com.cobaltplatform.api.model.api.response.CareResourceLocationApiResponse.CareResourceLocationApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CareResourceTagApiResponse.CareResourceTagApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ResourcePacketApiResponse.ResourcePacketApiResponseFactory;
import com.cobaltplatform.api.model.api.response.SupportRoleApiResponse.SupportRoleApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Address;
import com.cobaltplatform.api.model.db.CareResource;
import com.cobaltplatform.api.model.db.CareResourceLocation;
import com.cobaltplatform.api.model.db.CareResourceTag;
import com.cobaltplatform.api.model.db.CareResourceTag.CareResourceTagGroupId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.CareResourceService;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public class CareResourceResource {
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final CareResourceTagApiResponseFactory careResourceTagApiResponseFactory;
	@Nonnull
	private final CareResourceApiResponseFactory careResourceApiResponseFactory;
	@Nonnull
	private final CareResourceLocationApiResponseFactory careResourceLocationApiResponseFactory;
	@Nonnull
	private final ResourcePacketApiResponseFactory resourcePacketApiResponseFactory;
	@Nonnull
	private final SupportRoleApiResponseFactory supportRoleApiResponseFactory;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final CareResourceService careResourceService;
	@Nonnull
	private final RequestBodyParser requestBodyParser;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final AuthorizationService authorizationService;

	@Inject
	public CareResourceResource(@Nonnull Provider<CurrentContext> currentContextProvider,
															@Nonnull CareResourceService careResourceService,
															@Nonnull CareResourceTagApiResponseFactory careResourceTagApiResponseFactory,
															@Nonnull CareResourceLocationApiResponseFactory careResourceLocationApiResponseFactory,
															@Nonnull SupportRoleApiResponseFactory supportRoleApiResponseFactory,
															@Nonnull ResourcePacketApiResponseFactory resourcePacketApiResponseFactory,
															@Nonnull RequestBodyParser requestBodyParser,
															@Nonnull CareResourceApiResponseFactory careResourceApiResponseFactory,
															@Nonnull AuthorizationService authorizationService,
															@Nonnull Formatter formatter) {
		requireNonNull(currentContextProvider);
		requireNonNull(careResourceService);
		requireNonNull(careResourceTagApiResponseFactory);
		requireNonNull(careResourceLocationApiResponseFactory);
		requireNonNull(supportRoleApiResponseFactory);
		requireNonNull(requestBodyParser);
		requireNonNull(careResourceApiResponseFactory);
		requireNonNull(resourcePacketApiResponseFactory);
		requireNonNull(authorizationService);
		requireNonNull(formatter);

		this.currentContextProvider = currentContextProvider;
		this.careResourceService = careResourceService;
		this.careResourceTagApiResponseFactory = careResourceTagApiResponseFactory;
		this.careResourceLocationApiResponseFactory = careResourceLocationApiResponseFactory;
		this.supportRoleApiResponseFactory = supportRoleApiResponseFactory;
		this.resourcePacketApiResponseFactory = resourcePacketApiResponseFactory;
		this.logger = LoggerFactory.getLogger(getClass());
		this.careResourceApiResponseFactory = careResourceApiResponseFactory;
		this.requestBodyParser = requestBodyParser;
		this.authorizationService = authorizationService;
		this.formatter = formatter;
	}

	@Nonnull
	@GET("/care-resource-tags")
	@AuthenticationRequired
	public ApiResponse findCareResourceTags(@Nonnull @QueryParameter CareResourceTagGroupId careResourceTagGroupId) {
		requireNonNull(careResourceTagGroupId);

		List<CareResourceTag> careResourceTags = getCareResourceService().findTagsByGroupId(careResourceTagGroupId);
		return new ApiResponse(new HashMap<String, Object>() {{
			put("careResourceTags", careResourceTags.stream()
					.map(careResourceTag -> getCareResourceTagApiResponseFactory().create(careResourceTag))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/care-resources/{careResourceId}")
	@AuthenticationRequired
	public ApiResponse findAllCareResources(@Nonnull @PathParameter UUID careResourceId) {
		requireNonNull(careResourceId);

		Account account = getCurrentContext().getAccount().get();
		CareResource careResource = getCareResourceService().findCareResourceById(careResourceId, account.getInstitutionId()).orElse(null);
		if (careResource == null)
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("careResource", getCareResourceApiResponseFactory().create(careResource, true));
		}});

	}

	@Nonnull
	@GET("/care-resources/association-list")
	@AuthenticationRequired
	public ApiResponse findAllCareResources() {
		Account account = getCurrentContext().getAccount().get();
		List<CareResource> careResources = getCareResourceService().findAllCareResourcesByInstitutionId(account.getInstitutionId());
		return new ApiResponse(new LinkedHashMap<String, Object>() {{
			put("careResources", careResources.stream()
					.map(careResource -> getCareResourceApiResponseFactory().create(careResource, false))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/care-resources")
	@AuthenticationRequired
	public ApiResponse findAllCareResourcesWithFilters(@Nonnull @QueryParameter Optional<Integer> pageNumber,
																										 @Nonnull @QueryParameter Optional<Integer> pageSize,
																										 @Nonnull @QueryParameter Optional<String> searchQuery,
																										 @Nonnull @QueryParameter Optional<FindCareResourcesRequest.OrderBy> orderBy) {
		requireNonNull(pageNumber);
		requireNonNull(pageSize);
		requireNonNull(searchQuery);
		requireNonNull(orderBy);

		Account account = getCurrentContext().getAccount().get();
		FindResult<CareResource> findResult = getCareResourceService().findAllCareResourceByInstitutionIdWithFilters(new FindCareResourcesRequest() {
			{
				setPageNumber(pageNumber.orElse(0));
				setPageSize(pageSize.orElse(0));
				setInstitutionId(account.getInstitutionId());
				setSearch(searchQuery.orElse(null));
				setOrderBy(orderBy.orElse(null));
			}
		});

		return new ApiResponse(new LinkedHashMap<String, Object>() {{
			put("totalCount", findResult.getTotalCount());
			put("totalCountDescription", getFormatter().formatNumber(findResult.getTotalCount()));
			put("careResources", findResult.getResults().stream()
					.map(careResource -> getCareResourceApiResponseFactory().create(careResource, true))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@GET("/care-resources/locations")
	@AuthenticationRequired
	public ApiResponse findAllCareResourcesLocationsWithFilters(@Nonnull @QueryParameter Optional<Integer> pageNumber,
																															@Nonnull @QueryParameter Optional<Integer> pageSize,
																															@Nonnull @QueryParameter Optional<String> searchQuery,
																															@Nonnull @QueryParameter Optional<FindCareResourceLocationsRequest.OrderBy> orderBy,
																															@Nonnull @QueryParameter Optional<Boolean> wheelchairAccess,
																															@Nonnull @QueryParameter Optional<Integer> searchRadiusMiles,
																															@Nonnull @QueryParameter Optional<List<String>> payorIds,
																															@Nonnull @QueryParameter Optional<List<String>> specialtyIds,
																															@Nonnull @QueryParameter Optional<List<String>> therapyTypeIds,
																															@Nonnull @QueryParameter Optional<List<String>> populationServedIds,
																															@Nonnull @QueryParameter Optional<List<String>> genderIds,
																															@Nonnull @QueryParameter Optional<List<String>> ethnicityIds,
																															@Nonnull @QueryParameter Optional<List<String>> languageIds,
																															@Nonnull @QueryParameter Optional<List<String>> facilityTypes,
																															@Nonnull @QueryParameter Optional<String> addressId) {
		requireNonNull(pageNumber);
		requireNonNull(pageSize);
		requireNonNull(searchQuery);
		requireNonNull(orderBy);
		requireNonNull(payorIds);
		requireNonNull(specialtyIds);
		requireNonNull(therapyTypeIds);
		requireNonNull(populationServedIds);
		requireNonNull(genderIds);
		requireNonNull(ethnicityIds);
		requireNonNull(languageIds);
		requireNonNull(facilityTypes);
		requireNonNull(wheelchairAccess);
		requireNonNull(searchRadiusMiles);
		requireNonNull(addressId);

		Account account = getCurrentContext().getAccount().get();
		FindResult<CareResourceLocation> findResult = getCareResourceService().findAllCareResourceLocationsByInstitutionIdWithFilters(new FindCareResourceLocationsRequest() {
			{
				setPageNumber(pageNumber.orElse(0));
				setPageSize(pageSize.orElse(0));
				setInstitutionId(account.getInstitutionId());
				setSearch(searchQuery.orElse(null));
				setOrderBy(orderBy.orElse(null));
				setPayorIds(new HashSet<>(payorIds.orElse(List.of())));
				setSpecialtyIds(new HashSet<>(specialtyIds.orElse(List.of())));
				setTherapyTypeIds(new HashSet<>(therapyTypeIds.orElse(List.of())));
				setPopulationServedIds(new HashSet<>(populationServedIds.orElse(List.of())));
				setGenderIds(new HashSet<>(genderIds.orElse(List.of())));
				setEthnicityIds(new HashSet<>(ethnicityIds.orElse(List.of())));
				setLanguageIds(new HashSet<>(languageIds.orElse(List.of())));
				setFacilityTypes(new HashSet<>(facilityTypes.orElse(List.of())));
				setWheelchairAccess(wheelchairAccess.orElse(null));
				setAddressId(addressId.orElse(null));
				setSearchRadiusMiles(searchRadiusMiles.orElse(null));
			}
		});
		CareResourceLocationApiResponseBatchContext batchContext = careResourceLocationApiResponseBatchContextFor(findResult.getResults());

		return new ApiResponse(new LinkedHashMap<String, Object>() {{
			put("totalCount", findResult.getTotalCount());
			put("totalCountDescription", getFormatter().formatNumber(findResult.getTotalCount()));
			put("careResourceLocations", findResult.getResults().stream()
					.map(careResourceLocation -> getCareResourceLocationApiResponseFactory().create(careResourceLocation, batchContext))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@PUT("/care-resources")
	@AuthenticationRequired
	public ApiResponse updateCareResource(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Institution.InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canManageCareResources(institutionId, account))
			throw new AuthorizationException();

		UpdateCareResourceRequest request = getRequestBodyParser().parse(requestBody, UpdateCareResourceRequest.class);
		request.setInstitutionId(getCurrentContext().getAccount().get().getInstitutionId());

		UUID careResourceId = getCareResourceService().updateCareResource(request);
		CareResource careResource = getCareResourceService().findCareResourceById
				(careResourceId, getCurrentContext().getInstitutionId()).orElse(null);

		if (careResource == null)
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("careResource", getCareResourceApiResponseFactory().create(careResource, true));
		}});
	}

	@Nonnull
	@POST("/care-resources")
	@AuthenticationRequired
	public ApiResponse createCareResource(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Institution.InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canManageCareResources(institutionId, account))
			throw new AuthorizationException();

		CreateCareResourceRequest request = getRequestBodyParser().parse(requestBody, CreateCareResourceRequest.class);
		request.setCreatedByAccountId(getCurrentContext().getAccount().get().getAccountId());
		request.setInstitutionId(getCurrentContext().getAccount().get().getInstitutionId());
		UUID careResourceId = getCareResourceService().createCareResource(request);
		CareResource careResource = getCareResourceService().findCareResourceById
				(careResourceId, getCurrentContext().getInstitutionId()).orElse(null);

		if (careResource == null)
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("careResource", getCareResourceApiResponseFactory().create(careResource, true));
		}});
	}

	@Nonnull
	@DELETE("/care-resources/{careResourceId}")
	@AuthenticationRequired
	public ApiResponse deleteCareResource(@Nonnull @PathParameter UUID careResourceId) {
		requireNonNull(careResourceId);

		Institution.InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canManageCareResources(institutionId, account))
			throw new AuthorizationException();

		getCareResourceService().deleteCareResource(careResourceId);

		return new ApiResponse();
	}

	@Nonnull
	@DELETE("/care-resources/location/{careResourceLocationId}")
	@AuthenticationRequired
	public ApiResponse deleteCareResourceLocation(@Nonnull @PathParameter UUID careResourceLocationId) {
		requireNonNull(careResourceLocationId);

		Institution.InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canManageCareResources(institutionId, account))
			throw new AuthorizationException();

		getCareResourceService().deleteCareResourceLocation(careResourceLocationId);

		return new ApiResponse();
	}

	@Nonnull
	@POST("/care-resources/location")
	@AuthenticationRequired
	public ApiResponse createCareResourceLocation(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Institution.InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canManageCareResources(institutionId, account))
			throw new AuthorizationException();

		CreateCareResourceLocationRequest request = getRequestBodyParser().parse(requestBody, CreateCareResourceLocationRequest.class);
		request.setCreatedByAccountId(getCurrentContext().getAccount().get().getAccountId());
		request.setInstitutionId(getCurrentContext().getAccount().get().getInstitutionId());

		UUID careResourceLocationId = getCareResourceService().createCareResourceLocation(request);
		CareResourceLocation careResourceLocation = getCareResourceService().findCareResourceLocationById(careResourceLocationId,
				getCurrentContext().getAccount().get().getInstitutionId()).orElse(null);

		if (careResourceLocation == null)
			throw new NotFoundException();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("careResourceLocation", getCareResourceLocationApiResponseFactory().create(careResourceLocation));
		}});
	}

	@Nonnull
	@GET("/care-resources/location/{careResourceLocationId}")
	@AuthenticationRequired
	public ApiResponse findCareResourceLocation(@Nonnull @PathParameter UUID careResourceLocationId) {
		requireNonNull(careResourceLocationId);

		CareResourceLocation careResourceLocation = getCareResourceService().findCareResourceLocationById(careResourceLocationId,
				getCurrentContext().getAccount().get().getInstitutionId()).orElse(null);

		if (careResourceLocation == null)
			throw new NotFoundException();
		return new ApiResponse(new HashMap<String, Object>() {{
			put("careResourceLocation", getCareResourceLocationApiResponseFactory().create(careResourceLocation));
		}});
	}

	@Nonnull
	@GET("/care-resources/locations/{careResourceId}")
	@AuthenticationRequired
	public ApiResponse findCareResourceLocations(@Nonnull @PathParameter UUID careResourceId) {
		requireNonNull(careResourceId);

		List<CareResourceLocation> careResourceLocations = getCareResourceService().findCareResourceLocations(careResourceId);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("careResourceLocations", careResourceLocations.stream()
					.map(careResourceLocation -> getCareResourceLocationApiResponseFactory().create(careResourceLocation))
					.collect(Collectors.toList()));
		}});
	}

	@Nonnull
	@PUT("/care-resources/location/internal-notes")
	@AuthenticationRequired
	public ApiResponse updateCareResourceLocationNote(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		UpdateCareResourceLocationNoteRequest request = getRequestBodyParser().parse(requestBody, UpdateCareResourceLocationNoteRequest.class);

		UUID careResourceLocationId = getCareResourceService().updateCareResourceLocationNote(request);

		CareResourceLocation careResourceLocation = getCareResourceService().findCareResourceLocationById(careResourceLocationId,
				getCurrentContext().getAccount().get().getInstitutionId()).orElse(null);

		if (careResourceLocation == null)
			throw new NotFoundException();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("careResourceLocation", getCareResourceLocationApiResponseFactory().create(careResourceLocation));
		}});
	}

	@Nonnull
	@PUT("/care-resources/location")
	@AuthenticationRequired
	public ApiResponse updateCareResourceLocation(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		Institution.InstitutionId institutionId = getCurrentContext().getInstitutionId();
		Account account = getCurrentContext().getAccount().get();

		if (!getAuthorizationService().canManageCareResources(institutionId, account))
			throw new AuthorizationException();

		UpdateCareResourceLocationRequest request = getRequestBodyParser().parse(requestBody, UpdateCareResourceLocationRequest.class);
		request.setInstitutionId(getCurrentContext().getAccount().get().getInstitutionId());

		UUID careResourceLocationId = getCareResourceService().updateCareResourceLocation(request);
		CareResourceLocation careResourceLocation = getCareResourceService().findCareResourceLocationById(careResourceLocationId,
				getCurrentContext().getAccount().get().getInstitutionId()).orElse(null);

		if (careResourceLocation == null)
			throw new NotFoundException();

		return new ApiResponse(new HashMap<String, Object>() {{
			put("careResourceLocation", getCareResourceLocationApiResponseFactory().create(careResourceLocation));
		}});
	}

	@Nonnull
	@POST("/resource-packets/location")
	@AuthenticationRequired
	public ApiResponse createCareResourceLocationForResourcePacket(@Nonnull @RequestBody String requestBody) {
		requireNonNull(requestBody);

		CreateCareResourceLocationForResourcePacket request = getRequestBodyParser().parse(requestBody, CreateCareResourceLocationForResourcePacket.class);
		request.setCreatedByAccountId(getCurrentContext().getAccount().get().getAccountId());
		UUID resourcePacketCareResourceLocationId = careResourceService.createCareResourceLocationForResourcePacket(request);

		return new ApiResponse(new HashMap<String, Object>() {{
			put("resourcePacketCareResourceLocationId", resourcePacketCareResourceLocationId);
		}});
	}

	@Nonnull
	@DELETE("/resource-packets/location/{resourcePacketCareResourceLocationId}")
	@AuthenticationRequired
	public ApiResponse deleteResourcePacketLocation(@Nonnull @PathParameter UUID resourcePacketCareResourceLocationId) {
		requireNonNull(resourcePacketCareResourceLocationId);

		getCareResourceService().deleteCareResourceLocationFromResourcePacket(resourcePacketCareResourceLocationId);

		return new ApiResponse();
	}

	@Nonnull
	@PUT("/resource-packets/location/{resourcePacketCareResourceLocationId}")
	@AuthenticationRequired
	public ApiResponse updateResourcePacketLocationOrder(@Nonnull @PathParameter UUID resourcePacketCareResourceLocationId,
																											 @Nonnull @RequestBody String requestBody) {
		requireNonNull(resourcePacketCareResourceLocationId);

		UpdateCareResourceLocationForResourcePacket request = getRequestBodyParser().parse(requestBody, UpdateCareResourceLocationForResourcePacket.class);

		getCareResourceService().updateCareResourceLocationFromResourcePacket(resourcePacketCareResourceLocationId, request);

		return new ApiResponse();
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected CareResourceService getCareResourceService() {
		return this.careResourceService;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return this.authorizationService;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	protected CareResourceLocationApiResponseBatchContext careResourceLocationApiResponseBatchContextFor(@Nonnull List<CareResourceLocation> careResourceLocations) {
		requireNonNull(careResourceLocations);

		if (careResourceLocations.isEmpty())
			return CareResourceLocationApiResponseBatchContext.empty();

		Set<UUID> addressIds = careResourceLocations.stream()
				.map(CareResourceLocation::getAddressId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Set<UUID> careResourceLocationIds = careResourceLocations.stream()
				.map(CareResourceLocation::getCareResourceLocationId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		Set<UUID> careResourceIds = careResourceLocations.stream()
				.map(CareResourceLocation::getCareResourceId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		Map<UUID, Address> addressesByAddressId = getCareResourceService().getAddressService().findAddressesByIds(addressIds);
		Map<UUID, Map<CareResourceTagGroupId, List<CareResourceTag>>> careResourceLocationTagsByCareResourceLocationId =
				getCareResourceService().findTagsByCareResourceLocationIdsAndGroupIds(careResourceLocationIds, EnumSet.allOf(CareResourceTagGroupId.class));
		Map<UUID, Map<CareResourceTagGroupId, List<CareResourceTag>>> careResourceTagsByCareResourceId =
				getCareResourceService().findTagsByCareResourceIdsAndGroupIds(careResourceIds, EnumSet.of(CareResourceTagGroupId.SPECIALTIES, CareResourceTagGroupId.PAYORS));

		return new CareResourceLocationApiResponseBatchContext(
				addressesByAddressId,
				careResourceLocationTagsByCareResourceLocationId,
				careResourceTagsByCareResourceId,
				true,
				true
		);
	}

	@Nonnull
	public CareResourceApiResponseFactory getCareResourceApiResponseFactory() {
		return careResourceApiResponseFactory;
	}

	@Nonnull
	public RequestBodyParser getRequestBodyParser() {
		return requestBodyParser;
	}

	@Nonnull
	public SupportRoleApiResponseFactory getSupportRoleApiResponseFactory() {
		return supportRoleApiResponseFactory;
	}

	@Nonnull
	public Provider<CurrentContext> getCurrentContextProvider() {
		return currentContextProvider;
	}

	@Nonnull
	public Formatter getFormatter() {
		return formatter;
	}

	@Nonnull
	public CareResourceTagApiResponseFactory getCareResourceTagApiResponseFactory() {
		return careResourceTagApiResponseFactory;
	}

	@Nonnull
	public ResourcePacketApiResponseFactory getResourcePacketApiResponseFactory() {
		return resourcePacketApiResponseFactory;
	}

	@Nonnull
	public CareResourceLocationApiResponseFactory getCareResourceLocationApiResponseFactory() {
		return careResourceLocationApiResponseFactory;
	}

}
