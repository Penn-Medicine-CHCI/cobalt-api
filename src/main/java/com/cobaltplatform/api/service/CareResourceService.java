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


import com.cobaltplatform.api.integration.google.model.NormalizedPlace;
import com.cobaltplatform.api.model.api.request.CreateAddressRequest;
import com.cobaltplatform.api.model.api.request.CreateCareResourceLocationForResourcePacket;
import com.cobaltplatform.api.model.api.request.CreateCareResourceLocationRequest;
import com.cobaltplatform.api.model.api.request.CreateCareResourceRequest;
import com.cobaltplatform.api.model.api.request.CreateResourcePacketRequest;
import com.cobaltplatform.api.model.api.request.FindCareResourceLocationsRequest;
import com.cobaltplatform.api.model.api.request.FindCareResourcesRequest;
import com.cobaltplatform.api.model.api.request.UpdateAddressRequest;
import com.cobaltplatform.api.model.api.request.UpdateCareResourceLocationForResourcePacket;
import com.cobaltplatform.api.model.api.request.UpdateCareResourceLocationNoteRequest;
import com.cobaltplatform.api.model.api.request.UpdateCareResourceLocationRequest;
import com.cobaltplatform.api.model.api.request.UpdateCareResourceRequest;
import com.cobaltplatform.api.model.db.Address;
import com.cobaltplatform.api.model.db.CareResource;
import com.cobaltplatform.api.model.db.CareResourceLocation;
import com.cobaltplatform.api.model.db.CareResourceTag;
import com.cobaltplatform.api.model.db.CareResourceTag.CareResourceTagGroupId;
import com.cobaltplatform.api.model.db.DistanceUnit.DistanceUnitId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.PatientOrder;
import com.cobaltplatform.api.model.db.ResourcePacket;
import com.cobaltplatform.api.model.db.ResourcePacketCareResourceLocation;
import com.cobaltplatform.api.model.service.CareResourceLocationWithTotalCount;
import com.cobaltplatform.api.model.service.CareResourceWithTotalCount;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.google.maps.places.v1.Place;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlInListPlaceholders;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class CareResourceService {

	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final Provider<AddressService> addressServiceProvider;
	@Nonnull
	private final Provider<PatientOrderService> patientOrderServiceProvider;
	@Nonnull
	private final PlaceService placeService;

	@Inject
	public CareResourceService(@Nonnull DatabaseProvider databaseProvider,
														 @Nonnull Strings strings,
														 @Nonnull Provider<AccountService> accountServiceProvider,
														 @Nonnull Provider<AddressService> addressServiceProvider,
														 @Nonnull Provider<PatientOrderService> patientOrderServiceProvider,
														 @Nonnull PlaceService placeService) {
		requireNonNull(databaseProvider);
		requireNonNull(strings);
		requireNonNull(accountServiceProvider);
		requireNonNull(addressServiceProvider);
		requireNonNull(patientOrderServiceProvider);
		requireNonNull(placeService);

		this.databaseProvider = databaseProvider;
		this.strings = strings;
		this.accountServiceProvider = accountServiceProvider;
		this.addressServiceProvider = addressServiceProvider;
		this.patientOrderServiceProvider = patientOrderServiceProvider;
		this.logger = LoggerFactory.getLogger(getClass());
		this.placeService = placeService;
	}

	@Nonnull
	public List<CareResourceTag> findTagsByGroupId(CareResourceTagGroupId careResourceTagGroupId) {
		return getDatabase().queryForList("""
				SELECT * 
				FROM care_resource_tag
				WHERE care_resource_tag_group_id = ?
				ORDER BY name""", CareResourceTag.class, careResourceTagGroupId);
	}

	@Nonnull
	public List<CareResourceTag> findTagsByCareResourceIdAndGroupId(UUID careResourceId,
																																	CareResourceTagGroupId careResourceTagGroupId) {
		requireNonNull(careResourceId);

		return getDatabase().queryForList("""
				SELECT * 
				FROM care_resource_tag crt, care_resource_care_resource_tag crc
				WHERE crt.care_resource_tag_id = crc.care_resource_tag_id 
				AND crc.care_resource_id = ?
				AND crt.care_resource_tag_group_id = ?
				ORDER BY name""", CareResourceTag.class, careResourceId, careResourceTagGroupId);
	}

	@Nonnull
	public List<CareResourceTag> findTagsByCareResourceLocationIdAndGroupId(UUID careResourceLocationId,
																																					CareResourceTagGroupId careResourceTagGroupId) {
		requireNonNull(careResourceLocationId);

		return getDatabase().queryForList("""
				SELECT * 
				FROM care_resource_tag crt, care_resource_location_care_resource_tag crl
				WHERE crt.care_resource_tag_id = crl.care_resource_tag_id 
				AND crl.care_resource_location_id = ?
				AND crt.care_resource_tag_group_id = ?
				ORDER BY name""", CareResourceTag.class, careResourceLocationId, careResourceTagGroupId);
	}

	@Nonnull
	public Optional<CareResource> findCareResourceById(@Nonnull UUID careResourceId,
																										 @Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);
		requireNonNull(careResourceId);

		return getDatabase().queryForObject("""
				SELECT vcr.*
				FROM v_care_resource_institution vcr
				WHERE vcr.care_resource_id = ?
				AND vcr.institution_id = ?
				""", CareResource.class, careResourceId, institutionId);
	}

	@Nonnull
	public List<CareResource> findAllCareResourcesByInstitutionId(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);

		return getDatabase().queryForList("""
				SELECT * 
				FROM v_care_resource_institution vc
				WHERE vc.institution_id = ?
				ORDER BY vc.name """, CareResource.class, institutionId);
	}

	@Nonnull
	public FindResult<CareResource> findAllCareResourceByInstitutionIdWithFilters(@Nonnull FindCareResourcesRequest request) {
		requireNonNull(request);

		InstitutionId institutionId = request.getInstitutionId();
		Integer pageNumber = request.getPageNumber();
		Integer pageSize = request.getPageSize();
		String search = trimToNull(request.getSearch());
		FindCareResourcesRequest.OrderBy orderBy = request.getOrderBy() == null ? FindCareResourcesRequest.OrderBy.NAME_ASC : request.getOrderBy();

		final int DEFAULT_PAGE_SIZE = 25;
		final int MAXIMUM_PAGE_SIZE = 100;

		if (pageNumber == null || pageNumber < 0)
			pageNumber = 0;

		if (pageSize == null || pageSize <= 0)
			pageSize = DEFAULT_PAGE_SIZE;
		else if (pageSize > MAXIMUM_PAGE_SIZE)
			pageSize = MAXIMUM_PAGE_SIZE;

		Integer limit = pageSize;
		Integer offset = pageNumber * pageSize;
		List<Object> parameters = new ArrayList<>();

		StringBuilder query = new StringBuilder("SELECT vcr.*, COUNT(vcr.care_resource_id) OVER() AS total_count FROM v_care_resource_institution vcr ");

		query.append("WHERE vcr.institution_id = ? ");
		parameters.add(institutionId);

		if (search != null) {
			query.append("AND vcr.name ILIKE CONCAT('%',?,'%') ");
			parameters.add(search);
		}

		query.append("ORDER BY ");

		if (orderBy == FindCareResourcesRequest.OrderBy.NAME_DESC)
			query.append("vcr.name DESC ");
		else if (orderBy == FindCareResourcesRequest.OrderBy.NAME_ASC)
			query.append("vcr.name ASC ");

		query.append("LIMIT ? OFFSET ? ");

		parameters.add(limit);
		parameters.add(offset);

		List<CareResourceWithTotalCount> careResources = getDatabase().queryForList(query.toString(), CareResourceWithTotalCount.class, parameters.toArray());

		FindResult<? extends CareResource> findResult = new FindResult<>(careResources, careResources.size() == 0 ? 0 : careResources.get(0).getTotalCount());

		return (FindResult<CareResource>) findResult;
	}

	@Nonnull
	private void appendTagWhereClause(@Nonnull StringBuilder query, Set<String> tags) {
		query.append(format("""
				AND EXISTS 
				(SELECT 'X'
				FROM care_resource_location_care_resource_tag crlc
				WHERE crlc.care_resource_location_id = vcr.care_resource_location_id
				AND crlc.care_resource_tag_id IN %s
				UNION ALL
				SELECT 'X'
				FROM care_resource_care_resource_tag crlc
				WHERE crlc.care_resource_id = vcr.care_resource_id
				AND crlc.care_resource_tag_id IN %s) """, sqlInListPlaceholders(tags), sqlInListPlaceholders(tags)));
	}

	@Nonnull
	public FindResult<CareResourceLocation> findAllCareResourceLocationsByInstitutionIdWithFilters(@Nonnull FindCareResourceLocationsRequest request) {
		requireNonNull(request);

		InstitutionId institutionId = request.getInstitutionId();
		Integer pageNumber = request.getPageNumber();
		Integer pageSize = request.getPageSize();
		String search = trimToNull(request.getSearch());
		Boolean wheelchairAccess = request.getWheelchairAccess() == null ? null : request.getWheelchairAccess();
		Set<String> payorIds = request.getPayorIds() == null ? Set.of() : request.getPayorIds();
		Set<String> specialtyIds = request.getSpecialtyIds() == null ? Set.of() : request.getSpecialtyIds();
		Set<String> therapyTypeIds = request.getTherapyTypeIds() == null ? Set.of() : request.getTherapyTypeIds();
		Set<String> populationServedIds = request.getPopulationServedIds() == null ? Set.of() : request.getPopulationServedIds();
		Set<String> genderIds = request.getGenderIds() == null ? Set.of() : request.getGenderIds();
		Set<String> ethnicityIds = request.getEthnicityIds() == null ? Set.of() : request.getEthnicityIds();
		Set<String> languageIds = request.getLanguageIds() == null ? Set.of() : request.getLanguageIds();
		Set<String> facilityTypes = request.getFacilityTypes() == null ? Set.of() : request.getFacilityTypes();
		String googlePlaceId = trimToNull(request.getGooglePlaceId());
		Integer searchRadiusMiles = request.getSearchRadiusMiles();
		Place place = null;

		FindCareResourceLocationsRequest.OrderBy orderBy = request.getOrderBy() == null ? FindCareResourceLocationsRequest.OrderBy.NAME_ASC : request.getOrderBy();

		final int DEFAULT_PAGE_SIZE = 25;
		final int MAXIMUM_PAGE_SIZE = 100;
		final int DEFAULT_SEARCH_RADIUS_MILES = 5;

		if (pageNumber == null || pageNumber < 0)
			pageNumber = 0;

		if (pageSize == null || pageSize <= 0)
			pageSize = DEFAULT_PAGE_SIZE;
		else if (pageSize > MAXIMUM_PAGE_SIZE)
			pageSize = MAXIMUM_PAGE_SIZE;

		Integer limit = pageSize;
		Integer offset = pageNumber * pageSize;
		List<Object> parameters = new ArrayList<>();

		double latitude = 0.0;
		double longitude = 0.0;

		if (googlePlaceId != null) {
			place = getPlaceService().findPlaceByPlaceId(googlePlaceId);
			NormalizedPlace normalizedPlace = new NormalizedPlace(place);
			latitude = normalizedPlace.getLatitude();
			longitude = normalizedPlace.getLongitude();
		}

		StringBuilder query = new StringBuilder("SELECT vcr.*, COUNT(vcr.care_resource_location_id) OVER() AS total_count ");

		if (googlePlaceId != null) {
			query.append(", round((ST_DistanceSphere(ST_MakePoint(?, ?), ST_MakePoint(longitude, latitude)) / 1609.344)::numeric, 2) AS distance_in_miles ");
			parameters.add(longitude);
			parameters.add(latitude);
		}

		query.append("FROM v_care_resource_location_institution vcr ");

		query.append("WHERE vcr.institution_id = ? ");
		parameters.add(institutionId);

		if (search != null) {
			query.append("AND vcr.name ILIKE CONCAT('%',?,'%') ");
			parameters.add(search);
		}
		if (payorIds.size() > 0) {
			appendTagWhereClause(query, request.getPayorIds());
			parameters.addAll(request.getPayorIds());
			parameters.addAll(request.getPayorIds());
		}
		if (specialtyIds.size() > 0) {
			appendTagWhereClause(query, request.getSpecialtyIds());
			parameters.addAll(request.getSpecialtyIds());
			parameters.addAll(request.getSpecialtyIds());
		}
		if (therapyTypeIds.size() > 0) {
			appendTagWhereClause(query, request.getTherapyTypeIds());
			parameters.addAll(request.getTherapyTypeIds());
			parameters.addAll(request.getTherapyTypeIds());
		}
		if (populationServedIds.size() > 0) {
			appendTagWhereClause(query, request.getPopulationServedIds());
			parameters.addAll(request.getPopulationServedIds());
			parameters.addAll(request.getPopulationServedIds());
		}
		if (genderIds.size() > 0) {
			appendTagWhereClause(query, request.getGenderIds());
			parameters.addAll(request.getGenderIds());
			parameters.addAll(request.getGenderIds());
		}
		if (ethnicityIds.size() > 0) {
			appendTagWhereClause(query, request.getEthnicityIds());
			parameters.addAll(request.getEthnicityIds());
			parameters.addAll(request.getEthnicityIds());
		}
		if (languageIds.size() > 0) {
			appendTagWhereClause(query, request.getLanguageIds());
			parameters.addAll(request.getLanguageIds());
			parameters.addAll(request.getLanguageIds());
		}
		if (facilityTypes.size() > 0) {
			appendTagWhereClause(query, request.getFacilityTypes());
			parameters.addAll(request.getFacilityTypes());
			parameters.addAll(request.getFacilityTypes());
		}

		if (wheelchairAccess != null) {
			query.append("AND wheelchair_access = ? ");
			parameters.add(wheelchairAccess);
		}

		if (googlePlaceId != null) {
			query.append("AND ST_DistanceSphere(ST_MakePoint(?, ?),ST_MakePoint(longitude, latitude)) / 1609.344 < ? ");
			parameters.add(longitude);
			parameters.add(latitude);
			parameters.add(searchRadiusMiles == null ? DEFAULT_SEARCH_RADIUS_MILES : searchRadiusMiles);
		}

		query.append(" ORDER BY ");

		if (orderBy == FindCareResourceLocationsRequest.OrderBy.NAME_DESC)
			query.append("vcr.name DESC ");
		else if (orderBy == FindCareResourceLocationsRequest.OrderBy.NAME_ASC)
			query.append("vcr.name ASC ");

		query.append("LIMIT ? OFFSET ? ");

		parameters.add(limit);
		parameters.add(offset);

		getLogger().debug(format("SQL = %s", query.toString()));
		List<CareResourceLocationWithTotalCount> careResources = getDatabase().queryForList(query.toString(), CareResourceLocationWithTotalCount.class, parameters.toArray());

		FindResult<? extends CareResourceLocation> findResult = new FindResult<>(careResources, careResources.size() == 0 ? 0 : careResources.get(0).getTotalCount());

		return (FindResult<CareResourceLocation>) findResult;
	}

	@Nonnull
	public List<CareResourceLocation> findCareResourceLocations(@Nonnull UUID careResourceId) {
		requireNonNull(careResourceId);

		return getDatabase().queryForList("""
				SELECT crl.*
				FROM care_resource_location crl
				WHERE crl.care_resource_id = ?
				AND crl.deleted=false
				""", CareResourceLocation.class, careResourceId);
	}

	@Nonnull
	public Optional<ResourcePacket> findCurrentResourcePacketByPatientOrderId(@Nonnull UUID patientOrderId) {
		return getDatabase().queryForObject("""
				SELECT *
				FROM resource_packet
				WHERE patient_order_id = ?
				AND current_flag=true
				""", ResourcePacket.class, patientOrderId);
	}

	@Nonnull
	public Optional<ResourcePacket> findResourcePacketByLocationId(@Nonnull UUID resourcePacketCareResourceLocationId) {
		return getDatabase().queryForObject("""
				SELECT *
				FROM resource_packet rp
				WHERE rp.resource_packet_id = 
				(SELECT resource_packet_id
				FROM resource_packet_care_resource_location
				WHERE resource_packet_care_resource_location_id = ?)
				""", ResourcePacket.class, resourcePacketCareResourceLocationId);
	}

	@Nonnull
	public List<ResourcePacketCareResourceLocation> findResourcePacketLocations(@Nonnull UUID resourcePacketId) {
		requireNonNull(resourcePacketId);

		return getDatabase().queryForList("""
				SELECT po.*, 
				cr.notes AS care_resource_notes,
				crl.notes,
				crl.address_id,
				CASE WHEN crl.website_url IS NULL THEN cr.website_url ELSE CRL.website_url END AS website_url,
				CASE WHEN crl.phone_number IS NULL THEN cr.phone_number ELSE CRL.phone_number END AS phone_number,
				CASE WHEN crl.email_address IS NULL THEN cr.email_address ELSE CRL.email_address END AS email_address,
				CASE WHEN crl.name IS NULL THEN cr.name ELSE CRL.name END AS care_resource_location_name,
				a.first_name AS created_by_account_first_name, 
				a.last_name AS created_by_account_last_name
				FROM care_resource_location crl, resource_packet_care_resource_location po, care_resource cr,
				account a
				WHERE crl.care_resource_location_id = po.care_resource_location_id
				AND crl.care_resource_id = cr.care_resource_id
				AND crl.created_by_account_id = a.account_id
				AND po.resource_packet_id = ?
				AND crl.deleted=false
				ORDER BY po.display_order ASC
				""", ResourcePacketCareResourceLocation.class, resourcePacketId);
	}

	@Nonnull
	public UUID createCareResourceLocationForResourcePacket(@Nonnull CreateCareResourceLocationForResourcePacket request) {
		requireNonNull(request);

		UUID resourcePacketCareResourceLocationId = UUID.randomUUID();
		UUID resourcePacketId = request.getResourcePacketId();
		UUID careResourceLocationId = request.getCareResourceLocationId();
		UUID createdByAccountId = request.getCreatedByAccountId();
		ValidationException validationException = new ValidationException();

		if (resourcePacketId == null)
			validationException.add(new ValidationException.FieldError("resourcePacketId", "resourcePacketId is required."));

		if (careResourceLocationId == null)
			validationException.add(new ValidationException.FieldError("careResourceLocationId", "careResourceLocationId is required."));

		if (createdByAccountId == null)
			validationException.add(new ValidationException.FieldError("createdByAccountId", "createdByAccountId is required."));

		if (validationException.hasErrors())
			throw validationException;

		Boolean locationExists = getDatabase().queryForObject("""
				SELECT COUNT(*) > 0
				FROM resource_packet_care_resource_location rp
				WHERE resource_packet_id = ? 
				AND care_resource_location_id=?
				""", Boolean.class, resourcePacketId, careResourceLocationId).get();

		if (locationExists)
			getDatabase().execute("""
					UPDATE resource_packet_care_resource_location
					SET deleted=false
					WHERE resource_packet_id = ? 
					AND care_resource_location_id=?
					""", resourcePacketId, careResourceLocationId);
		else {
			Integer displayOrder = getDatabase().queryForObject("""
					SELECT COUNT(*) 
					FROM resource_packet_care_resource_location rp
					WHERE rp.resource_packet_id = ?
					""", Integer.class, resourcePacketId).get();

			getDatabase().execute("""
					INSERT INTO resource_packet_care_resource_location
					(resource_packet_care_resource_location_id, resource_packet_id, care_resource_location_id, created_by_account_id, display_order)
					VALUES 
					(?,?,?,?,?)
					""", resourcePacketCareResourceLocationId, resourcePacketId, careResourceLocationId, createdByAccountId, displayOrder);
		}

		return resourcePacketCareResourceLocationId;
	}

	@Nonnull
	public Optional<CareResourceLocation> findCareResourceLocationById(@Nonnull UUID careResourceLocationId,
																																		 @Nonnull InstitutionId institutionId) {
		requireNonNull(careResourceLocationId);
		requireNonNull(institutionId);

		return getDatabase().queryForObject("""
				SELECT crl.*
				FROM v_care_resource_location_institution crl
				WHERE crl.care_resource_location_id = ?
				AND crl.institution_id = ?
				""", CareResourceLocation.class, careResourceLocationId, institutionId);
	}

	@Nonnull
	public UUID updateCareResourceLocationNote(UpdateCareResourceLocationNoteRequest request) {
		requireNonNull(request);

		String internalNotes = trimToNull(request.getInternalNotes());
		UUID careResourceLocationId = request.getCareResourceLocationId();

		getDatabase().execute("""
				UPDATE care_resource_location
				SET internal_notes = ?
				WHERE care_resource_location_id = ?
				""", internalNotes, careResourceLocationId);

		return careResourceLocationId;
	}

	@Nonnull
	public UUID updateCareResourceLocation(UpdateCareResourceLocationRequest request) {
		requireNonNull(request);

		String googlePlaceId = trimToNull(request.getGooglePlaceId());
		String notes = trimToNull(request.getNotes());
		String emailAddress = trimToNull(request.getEmailAddress());
		String phoneNumber = trimToNull(request.getPhoneNumber());
		String streetAddress2 = trimToNull(request.getStreetAddress2());
		Boolean acceptingNewPatients = request.getAcceptingNewPatients();
		Boolean wheelchairAccessible = request.getWheelchairAccess();
		String websiteUrl = trimToNull(request.getWebsiteUrl());
		ValidationException validationException = new ValidationException();
		String name = trimToNull(request.getName());
		InstitutionId institutionId = request.getInstitutionId();
		UUID careResourceLocationId = request.getCareResourceLocationId();
		UpdateAddressRequest updateAddressRequest = new UpdateAddressRequest();
		Boolean overridePayors = request.getOverridePayors();
		Boolean overrideSpecialties = request.getOverrideSpecialties();
		Boolean appointmentTypeInPerson = request.getAppointmentTypeInPerson();
		Boolean appointmentTypeOnline = request.getAppointmentTypeOnline();
		// Only use the location level insurance notes if the resource level is being overridden
		String insuranceNotes = overridePayors ? trimToNull(request.getInsuranceNotes()) : null;

		CareResourceLocation currentCareResourceLocation = findCareResourceLocationById(careResourceLocationId, institutionId).orElse(null);

		if (appointmentTypeInPerson && googlePlaceId == null)
			validationException.add(new ValidationException.FieldError("googlePlaceId", "Address is required for an in person location."));
		if (currentCareResourceLocation == null)
			validationException.add(new ValidationException.FieldError("careResourceLocation", "Could not find Care Resource Location."));
		if (appointmentTypeInPerson == null)
			validationException.add(new ValidationException.FieldError("appointmentTypeInPerson", "Must specify if in person appointments are allowed."));
		if (appointmentTypeOnline == null)
			validationException.add(new ValidationException.FieldError("appointmentTypeOnline", "Must specify if in online appointments are allowed."));
		if (overridePayors && request.getPayorIds().size() == 0)
			validationException.add(new ValidationException.FieldError("payorIds", "At least on insurance carrier is required to override."));
		if (!overridePayors) {
			List<CareResourceTag> resourcePayors = findTagsByCareResourceIdAndGroupId(currentCareResourceLocation.getCareResourceId(), CareResourceTagGroupId.PAYORS);

			if (resourcePayors.size() == 0)
				validationException.add(new ValidationException.FieldError("payorIds", "At least on insurance carrier is required."));
		}

		if (validationException.hasErrors())
			throw validationException;

		UUID addressId = currentCareResourceLocation.getAddressId();

		if (appointmentTypeInPerson && addressId != null) {
			Address currentAddress = getAddressService().findAddressById(currentCareResourceLocation.getAddressId()).orElse(null);
			String currentGooglePlaceId = trimToNull(currentAddress.getGooglePlaceId());

			if (!currentGooglePlaceId.equals(googlePlaceId)) {
				// Place has changed so get the new place from Google
				Place place = getPlaceService().findPlaceByPlaceId(googlePlaceId);

				if (place == null)
					validationException.add(new ValidationException.FieldError("place", "Could not find the Google place"));

				if (validationException.hasErrors())
					throw validationException;

				NormalizedPlace normalizedPlace = new NormalizedPlace(place);
				updateAddressRequest.setAddressId(currentAddress.getAddressId());
				updateAddressRequest.setGooglePlaceId(googlePlaceId);
				updateAddressRequest.setStreetAddress1(normalizedPlace.getStreetAddress1());
				updateAddressRequest.setStreetAddress2(streetAddress2);
				updateAddressRequest.setPostalCode(normalizedPlace.getPostalCode());
				updateAddressRequest.setPostalName(name);
				updateAddressRequest.setLocality(normalizedPlace.getLocality());
				updateAddressRequest.setRegion(normalizedPlace.getRegion());
				updateAddressRequest.setGoogleMapsUrl(normalizedPlace.getGoogleMapsUrl());
				updateAddressRequest.setPremise(normalizedPlace.getPremise());
				updateAddressRequest.setSubpremise(normalizedPlace.getSubpremise());
				updateAddressRequest.setRegionSubdivision(normalizedPlace.getRegionSubdivision());
				updateAddressRequest.setPostalCodeSuffix(normalizedPlace.getPostalCodeSuffix());
				updateAddressRequest.setFormattedAddress(place.getFormattedAddress());
				updateAddressRequest.setLatitude(place.getLocation().getLatitude());
				updateAddressRequest.setLongitude(place.getLocation().getLongitude());
				updateAddressRequest.setCountryCode(normalizedPlace.getCountryCode());
			} else {
				updateAddressRequest.setAddressId(currentAddress.getAddressId());
				updateAddressRequest.setGooglePlaceId(googlePlaceId);
				updateAddressRequest.setStreetAddress1(currentAddress.getStreetAddress1());
				updateAddressRequest.setStreetAddress2(streetAddress2);
				updateAddressRequest.setPostalCode(currentAddress.getPostalCode());
				updateAddressRequest.setPostalName(name);
				updateAddressRequest.setLocality(currentAddress.getLocality());
				updateAddressRequest.setRegion(currentAddress.getRegion());
				updateAddressRequest.setGoogleMapsUrl(currentAddress.getGoogleMapsUrl());
				updateAddressRequest.setPremise(currentAddress.getPremise());
				updateAddressRequest.setSubpremise(currentAddress.getSubpremise());
				updateAddressRequest.setRegionSubdivision(currentAddress.getRegionSubdivision());
				updateAddressRequest.setPostalCodeSuffix(currentAddress.getPostalCodeSuffix());
				updateAddressRequest.setFormattedAddress(currentAddress.getFormattedAddress());
				updateAddressRequest.setLatitude(currentAddress.getLatitude());
				updateAddressRequest.setLongitude(currentAddress.getLongitude());
				updateAddressRequest.setCountryCode(currentAddress.getCountryCode());
			}
			getAddressService().updateAddress(updateAddressRequest);
		} else if (appointmentTypeInPerson && addressId == null) {
			//We're creating an address for the location
			Place place = getPlaceService().findPlaceByPlaceId(googlePlaceId);

			if (place == null)
				validationException.add(new ValidationException.FieldError("place", "Could not find the Google place"));

			if (validationException.hasErrors())
				throw validationException;

			CreateAddressRequest createAddressRequest = new CreateAddressRequest();
			NormalizedPlace normalizedPlace = new NormalizedPlace(place);

			createAddressRequest.setStreetAddress1(normalizedPlace.getStreetAddress1());
			createAddressRequest.setStreetAddress2(streetAddress2);
			createAddressRequest.setPostalCode(normalizedPlace.getPostalCode());
			createAddressRequest.setLocality(normalizedPlace.getLocality());
			createAddressRequest.setRegion(normalizedPlace.getRegion());
			createAddressRequest.setPostalName(name);
			createAddressRequest.setGooglePlaceId(googlePlaceId);
			createAddressRequest.setGoogleMapsUrl(normalizedPlace.getGoogleMapsUrl());
			createAddressRequest.setPremise(normalizedPlace.getPremise());
			createAddressRequest.setSubpremise(normalizedPlace.getSubpremise());
			createAddressRequest.setRegionSubdivision(normalizedPlace.getRegionSubdivision());
			createAddressRequest.setPostalCodeSuffix(normalizedPlace.getPostalCodeSuffix());
			createAddressRequest.setFormattedAddress(place.getFormattedAddress());
			createAddressRequest.setLatitude(place.getLocation().getLatitude());
			createAddressRequest.setLongitude(place.getLocation().getLongitude());

			addressId = getAddressService().createAddress(createAddressRequest);
		} else if (!appointmentTypeInPerson)
			addressId = null;

		getDatabase().execute("""
						UPDATE care_resource_location
						SET phone_number = ?, wheelchair_access=?, notes=?, accepting_new_patients=?,
						website_url=?, name=?, insurance_notes=?, email_address =?, override_payors =?, override_specialties =?,
						appointment_type_in_person =?, appointment_type_online=?, address_id =?
						WHERE care_resource_location_id = ?
						""",
				phoneNumber, wheelchairAccessible != null && wheelchairAccessible, notes, acceptingNewPatients != null && acceptingNewPatients,
				websiteUrl, name, insuranceNotes, emailAddress, overridePayors, overrideSpecialties, appointmentTypeInPerson, appointmentTypeOnline,
				addressId, careResourceLocationId);

		getDatabase().execute("""
				DELETE FROM care_resource_location_care_resource_tag
				WHERE care_resource_location_id=?
				""", careResourceLocationId);

		List<String> allTags = new ArrayList<>();
		if (request.getPayorIds() != null && overridePayors)
			allTags.addAll(request.getPayorIds());
		else
			allTags.addAll(findTagsByCareResourceIdAndGroupId(currentCareResourceLocation.getCareResourceId(), CareResourceTagGroupId.PAYORS)
					.stream().map(tag -> tag.getCareResourceTagId()).collect(Collectors.toList()));
		if (request.getEthnicityIds() != null)
			allTags.addAll(request.getEthnicityIds());
		if (request.getSpecialtyIds() != null && overrideSpecialties)
			allTags.addAll(request.getSpecialtyIds());
		else
			allTags.addAll(findTagsByCareResourceIdAndGroupId(currentCareResourceLocation.getCareResourceId(), CareResourceTagGroupId.SPECIALTIES)
					.stream().map(tag -> tag.getCareResourceTagId()).collect(Collectors.toList()));
		if (request.getLanguageIds() != null)
			allTags.addAll(request.getLanguageIds());
		if (request.getPopulationServedIds() != null)
			allTags.addAll(request.getPopulationServedIds());
		if (request.getTherapyTypeIds() != null)
			allTags.addAll(request.getTherapyTypeIds());
		if (request.getGenderIds() != null)
			allTags.addAll(request.getGenderIds());
		if (request.getFacilityTypes() != null)
			allTags.addAll(request.getFacilityTypes());

		if (allTags != null)
			for (String tag : allTags)
				getDatabase().execute("""
						INSERT INTO care_resource_location_care_resource_tag
						(care_resource_location_id, care_resource_tag_id)
						VALUES
						(?,?)""", careResourceLocationId, tag);

		return careResourceLocationId;
	}

	@Nonnull
	public UUID createCareResourceLocation(CreateCareResourceLocationRequest request) {
		requireNonNull(request);

		String googlePlaceId = trimToNull(request.getGooglePlaceId());
		String notes = trimToNull(request.getNotes());
		String emailAddress = trimToNull(request.getEmailAddress());
		String phoneNumber = trimToNull(request.getPhoneNumber());
		String streetAddress2 = trimToNull(request.getStreetAddress2());
		Boolean acceptingNewPatients = request.getAcceptingNewPatients();
		Boolean wheelchairAccessible = request.getWheelchairAccess();
		UUID createdByAccountId = request.getCreatedByAccountId();
		UUID careResourceId = request.getCareResourceId();
		UUID careResourceLocationId = UUID.randomUUID();
		String websiteUrl = trimToNull(request.getWebsiteUrl());
		ValidationException validationException = new ValidationException();
		String name = trimToNull(request.getName());
		InstitutionId institutionId = request.getInstitutionId();
		Boolean overridePayors = request.getOverridePayors();
		Boolean overrideSpecialties = request.getOverrideSpecialties();
		Boolean appointmentTypeInPerson = request.getAppointmentTypeInPerson();
		Boolean appointmentTypeOnline = request.getAppointmentTypeOnline();
		// Only use the location level insurance notes if the resource level is being overridden
		String insuranceNotes = overridePayors ? trimToNull(request.getInsuranceNotes()) : null;
		Place place = null;

		CareResource careResource = findCareResourceById(careResourceId, institutionId).orElse(null);

		if (careResource == null)
			validationException.add(new ValidationException.FieldError("careResource", "Could not find Care Resource."));
		if (appointmentTypeInPerson && googlePlaceId == null)
			validationException.add(new ValidationException.FieldError("googlePlaceId", "Address is required for in an person location."));
		else if (appointmentTypeInPerson && googlePlaceId != null) {
			place = getPlaceService().findPlaceByPlaceId(googlePlaceId);

			if (place == null)
				validationException.add(new ValidationException.FieldError("place", "Could not find the Google place"));
		}
		if (overridePayors && request.getPayorIds().size() == 0)
			validationException.add(new ValidationException.FieldError("payorIds", "At least on insurance carrier is required."));

		if (!overridePayors) {
			List<CareResourceTag> resourcePayors = findTagsByCareResourceIdAndGroupId(careResourceId, CareResourceTagGroupId.PAYORS);

			if (resourcePayors.size() == 0)
				validationException.add(new ValidationException.FieldError("payorIds", "At least on insurance carrier is required."));
		}

		if (validationException.hasErrors())
			throw validationException;

		// If there is no name for this location then use the Care Resource name
		if (name == null)
			name = careResource.getName();

		UUID addressId = null;

		if (appointmentTypeInPerson) {
			CreateAddressRequest createAddressRequest = new CreateAddressRequest();
			NormalizedPlace normalizedPlace = new NormalizedPlace(place);

			createAddressRequest.setStreetAddress1(normalizedPlace.getStreetAddress1());
			createAddressRequest.setStreetAddress2(streetAddress2);
			createAddressRequest.setPostalCode(normalizedPlace.getPostalCode());
			createAddressRequest.setLocality(normalizedPlace.getLocality());
			createAddressRequest.setRegion(normalizedPlace.getRegion());
			createAddressRequest.setPostalName(name);
			createAddressRequest.setGooglePlaceId(googlePlaceId);
			createAddressRequest.setGoogleMapsUrl(normalizedPlace.getGoogleMapsUrl());
			createAddressRequest.setPremise(normalizedPlace.getPremise());
			createAddressRequest.setSubpremise(normalizedPlace.getSubpremise());
			createAddressRequest.setRegionSubdivision(normalizedPlace.getRegionSubdivision());
			createAddressRequest.setPostalCodeSuffix(normalizedPlace.getPostalCodeSuffix());
			createAddressRequest.setFormattedAddress(place.getFormattedAddress());
			createAddressRequest.setLatitude(place.getLocation().getLatitude());
			createAddressRequest.setLongitude(place.getLocation().getLongitude());

			addressId = getAddressService().createAddress(createAddressRequest);
		}

		getDatabase().execute("""
						INSERT INTO care_resource_location
						  (care_resource_location_id, care_resource_id, address_id,
						  phone_number, wheelchair_access, notes, accepting_new_patients,
						  website_url, name, insurance_notes, created_by_account_id, email_address, override_payors, override_specialties,
						  appointment_type_in_person, appointment_type_online)
						VALUES
						  (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
						  """, careResourceLocationId, careResourceId, addressId,
				phoneNumber, wheelchairAccessible != null && wheelchairAccessible, notes, acceptingNewPatients != null && acceptingNewPatients,
				websiteUrl, name, insuranceNotes, createdByAccountId, emailAddress, overridePayors, overrideSpecialties,
				appointmentTypeInPerson, appointmentTypeOnline);

		List<String> allTags = new ArrayList<>();
		if (request.getPayorIds() != null && overridePayors)
			allTags.addAll(request.getPayorIds());
		else
			allTags.addAll(findTagsByCareResourceIdAndGroupId(careResourceId, CareResourceTagGroupId.PAYORS)
					.stream().map(tag -> tag.getCareResourceTagId()).collect(Collectors.toList()));
		if (request.getEthnicityIds() != null)
			allTags.addAll(request.getEthnicityIds());
		if (request.getSpecialtyIds() != null && overrideSpecialties)
			allTags.addAll(request.getSpecialtyIds());
		else
			allTags.addAll(findTagsByCareResourceIdAndGroupId(careResourceId, CareResourceTagGroupId.SPECIALTIES)
					.stream().map(tag -> tag.getCareResourceTagId()).collect(Collectors.toList()));
		if (request.getLanguageIds() != null)
			allTags.addAll(request.getLanguageIds());
		if (request.getPopulationServedIds() != null)
			allTags.addAll(request.getPopulationServedIds());
		if (request.getTherapyTypeIds() != null)
			allTags.addAll(request.getTherapyTypeIds());
		if (request.getGenderIds() != null)
			allTags.addAll(request.getGenderIds());
		if (request.getFacilityTypes() != null)
			allTags.addAll(request.getFacilityTypes());

		if (allTags != null)
			for (String tag : allTags)
				getDatabase().execute("""
						INSERT INTO care_resource_location_care_resource_tag
						(care_resource_location_id, care_resource_tag_id)
						VALUES
						(?,?)""", careResourceLocationId, tag);

		return careResourceLocationId;
	}

	@Nonnull
	public UUID updateCareResource(@Nonnull UpdateCareResourceRequest request) {
		requireNonNull(request);

		String name = trimToNull(request.getName());
		UUID careResourceId = request.getCareResourceId();
		String notes = trimToNull(request.getNotes());
		String insuranceNotes = trimToNull(request.getInsuranceNotes());
		String websiteUrl = trimToNull(request.getWebsiteUrl());
		String phoneNumber = trimToNull(request.getPhoneNumber());
		String emailAddress = trimToNull(request.getEmailAddress());
		ValidationException validationException = new ValidationException();

		if (name == null)
			validationException.add(new ValidationException.FieldError("name", "Name is required."));

		if (request.getPayorIds().size() == 0) {
			Boolean locationsWithNoPayors = getDatabase().queryForObject("""
					SELECT COUNT(*) > 0
					FROM care_resource_location 
					WHERE care_resource_id = ? 
					AND override_payors = false
					""", Boolean.class, careResourceId).get();

			if (locationsWithNoPayors)
				validationException.add(new ValidationException.FieldError("payorIds", "Cannot remove all insurance carriers because one or more locations do not have any defined."));
		}

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				UPDATE care_resource
				SET name=?, notes=?, insurance_notes=?, website_url=?, phone_number=?, email_address=?
				WHERE care_resource_id=?
				""", name, notes, insuranceNotes, websiteUrl, phoneNumber, emailAddress, careResourceId);

		getDatabase().execute("""
				DELETE FROM care_resource_care_resource_tag
				WHERE care_resource_id=? 
				""", careResourceId);

		List<String> allTags = new ArrayList<>();

		if (request.getSpecialtyIds() != null)
			allTags.addAll(request.getSpecialtyIds());
		if (request.getPayorIds() != null)
			allTags.addAll(request.getPayorIds());

		if (allTags != null)
			for (String tag : allTags)
				getDatabase().execute("""
						INSERT INTO care_resource_care_resource_tag
						(care_resource_id, care_resource_tag_id)
						VALUES
						(?,?)""", careResourceId, tag);

		return careResourceId;

	}

	@Nonnull
	public UUID createCareResource(@Nonnull CreateCareResourceRequest request) {
		requireNonNull(request);

		String name = trimToNull(request.getName());
		UUID createdByAccountId = request.getCreatedByAccountId();
		UUID careResourceId = UUID.randomUUID();
		String notes = trimToNull(request.getNotes());
		String insuranceNotes = trimToNull(request.getInsuranceNotes());
		String websiteUrl = trimToNull(request.getWebsiteUrl());
		String phoneNumber = trimToNull(request.getPhoneNumber());
		String emailAddress = trimToNull(request.getEmailAddress());
		ValidationException validationException = new ValidationException();

		if (name == null)
			validationException.add(new ValidationException.FieldError("name", "Name is required."));

		if (createdByAccountId == null)
			validationException.add(new ValidationException.FieldError("createdByAccountId", "Created by account ID is required."));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				INSERT INTO care_resource
				(care_resource_id, name, created_by_account_id, notes, insurance_notes, phone_number, email_address, website_url)
				VALUES
				(?,?,?,?,?,?,?,?)""", careResourceId, name, createdByAccountId, notes, insuranceNotes, phoneNumber, emailAddress, websiteUrl);

		getDatabase().execute("""
				INSERT INTO care_resource_institution 
				(care_resource_id, institution_id)
				VALUES
				(?,?)""", careResourceId, request.getInstitutionId());

		List<String> allTags = new ArrayList<>();

		if (request.getSpecialtyIds() != null)
			allTags.addAll(request.getSpecialtyIds());
		if (request.getPayorIds() != null)
			allTags.addAll(request.getPayorIds());

		if (allTags != null)
			for (String tag : allTags)
				getDatabase().execute("""
						INSERT INTO care_resource_care_resource_tag
						(care_resource_id, care_resource_tag_id)
						VALUES
						(?,?)""", careResourceId, tag);

		return careResourceId;
	}

	@Nonnull
	public boolean deleteCareResource(@Nonnull UUID careResourceId) {
		requireNonNull(careResourceId);

		List<CareResourceLocation> careResourceLocations = findCareResourceLocations(careResourceId);

		for (CareResourceLocation careResourceLocation : careResourceLocations)
			deleteCareResourceLocation((careResourceLocation.getCareResourceLocationId()));

		boolean deleted = getDatabase().execute("""
				UPDATE care_resource
				SET deleted = TRUE
				WHERE care_resource_id=?
				""", careResourceId) > 0;

		return deleted;
	}

	@Nonnull
	public boolean deleteCareResourceLocation(@Nonnull UUID careResourceLocationId) {
		requireNonNull(careResourceLocationId);

		boolean deleted = getDatabase().execute("""
				UPDATE care_resource_location
				SET deleted = TRUE
				WHERE care_resource_location_id=?
				""", careResourceLocationId) > 0;

		return deleted;
	}

	@Nonnull
	public boolean deleteCareResourceLocationFromResourcePacket(@Nonnull UUID resourcePacketCareResourceLocationId) {
		requireNonNull(resourcePacketCareResourceLocationId);

		Optional<ResourcePacketCareResourceLocation> resourcePacketCareResourceLocation =
				findResourcePacketCareResourceLocationById(resourcePacketCareResourceLocationId);

		ValidationException validationException = new ValidationException();

		if (!resourcePacketCareResourceLocation.isPresent())
			validationException.add(new ValidationException.FieldError("resourcePacketCareResourceLocationId", "Could not find resource packet."));

		boolean deleted = getDatabase().execute("""
				DELETE FROM resource_packet_care_resource_location				
				WHERE resource_packet_care_resource_location_id=?
				""", resourcePacketCareResourceLocationId) > 0;

		getDatabase().execute("""
				WITH RowNumbered AS (
				    SELECT
				        resource_packet_care_resource_location_id,
				        ROW_NUMBER() OVER (ORDER BY display_order) AS row_num
				    FROM resource_packet_care_resource_location
				)
				UPDATE resource_packet_care_resource_location
				SET display_order = RowNumbered.row_num - 1
				FROM RowNumbered
				WHERE resource_packet_care_resource_location.resource_packet_care_resource_location_id 
				= RowNumbered.resource_packet_care_resource_location_id
				AND resource_packet_care_resource_location.resource_packet_id = ?;
				""", resourcePacketCareResourceLocation.get().getResourcePacketId());

		return deleted;
	}

	@Nonnull
	public Optional<ResourcePacketCareResourceLocation> findResourcePacketCareResourceLocationById(@Nonnull UUID resourcePacketCareResourceLocationId){
		requireNonNull((resourcePacketCareResourceLocationId));

		return getDatabase().queryForObject("""
				SELECT *
				FROM resource_packet_care_resource_location
				WHERE resource_packet_care_resource_location_id=?
				""", ResourcePacketCareResourceLocation.class, resourcePacketCareResourceLocationId);
	}

	@Nonnull
	public UUID updateCareResourceLocationFromResourcePacket(@Nonnull UUID resourcePacketCareResourceLocationId,
																													 @Nonnull UpdateCareResourceLocationForResourcePacket request) {
		requireNonNull(resourcePacketCareResourceLocationId);
		requireNonNull(request);

		ValidationException validationException = new ValidationException();
		Optional<ResourcePacketCareResourceLocation> resourcePacketCareResourceLocation =
				findResourcePacketCareResourceLocationById(resourcePacketCareResourceLocationId);

		if (!resourcePacketCareResourceLocation.isPresent())
			validationException.add(new ValidationException.FieldError("resourcePacketCareResourceLocationId", "Could not find location for resource packet"));

		if (validationException.hasErrors())
			throw validationException;

		if (resourcePacketCareResourceLocation.get().getDisplayOrder() == request.getDisplayOrder())
			return resourcePacketCareResourceLocationId;

		getDatabase().execute("""
				UPDATE resource_packet_care_resource_location
				SET display_order = 
				CASE
						WHEN display_order >= ? AND display_order < ? THEN display_order + 1
						WHEN display_order <= ? AND display_order > ? THEN display_order - 1
						ELSE display_order
				END
				WHERE resource_packet_care_resource_location_id != ?
				""", request.getDisplayOrder(), resourcePacketCareResourceLocation.get().getDisplayOrder(),
				request.getDisplayOrder(), resourcePacketCareResourceLocation.get().getDisplayOrder(), resourcePacketCareResourceLocationId);

		getDatabase().execute("""
				UPDATE resource_packet_care_resource_location
				SET display_order = ?
				WHERE resource_packet_care_resource_location_id = ?
				""", request.getDisplayOrder(), resourcePacketCareResourceLocationId);

		return resourcePacketCareResourceLocationId;
	}

	@Nonnull
	public UUID createResourcePacket(@Nonnull CreateResourcePacketRequest request) {
		requireNonNull(request);

		UUID resourcePacketId = UUID.randomUUID();
		UUID patientOrderId = request.getPatientOrderId();
		UUID createdByAccountId = request.getAccountId();
		ValidationException validationException = new ValidationException();

		Optional<PatientOrder> patientOrder = getPatientOrderService().findPatientOrderById(patientOrderId);

		if (!patientOrder.isPresent())
			validationException.add(new ValidationException.FieldError("patientOrderId", "Could not find patient order."));

		if (createdByAccountId == null)
			validationException.add(new ValidationException.FieldError("createdByAccountId", "createdByAccountId is required."));

		if (validationException.hasErrors())
			throw validationException;

		UUID addressId = patientOrder.get().getPatientAddressId();
		Integer travelRadius = patientOrder.get().getInPersonCareRadius();
		DistanceUnitId travelRadiusDistanceUnitId = patientOrder.get().getInPersonCareRadiusDistanceUnitId();

		//Update and resource packets that this patient order may already have to not be current
		getDatabase().execute("""
				UPDATE resource_packet
				SET current_flag=false
				WHERE patient_order_id = ?
				AND current_flag=true
				""", patientOrderId);

		getDatabase().execute("""
				INSERT INTO resource_packet
				  (resource_packet_id, patient_order_id, address_id, travel_radius, travel_radius_distance_unit_id)
				VALUES
				  (?,?,?,?,?)
				  """, resourcePacketId, patientOrderId, addressId, travelRadius, travelRadiusDistanceUnitId);

		//add the resource locations from the most recent resource packet for this patient order if any exist
		getDatabase().execute("""
				INSERT INTO resource_packet_care_resource_location
				   (resource_packet_id, care_resource_location_id, created_by_account_id, display_order)
				 SELECT ?, crl.care_resource_location_id, ?, crl.display_order
				 FROM resource_packet_care_resource_location crl
				 WHERE crl.resource_packet_id = 
				 (SELECT rp.resource_packet_id
				 FROM resource_packet rp
				 WHERE rp.patient_order_id = ?
				 AND resource_packet_id != ?
				 ORDER BY rp.created DESC
				 LIMIT 1)
				 """, resourcePacketId, createdByAccountId, patientOrderId, resourcePacketId);
		return resourcePacketId;
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountServiceProvider.get();
	}

	@Nonnull
	protected PatientOrderService getPatientOrderService() {
		return this.patientOrderServiceProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	public PlaceService getPlaceService() {
		return placeService;
	}

	@Nonnull
	public AddressService getAddressService() {
		return addressServiceProvider.get();
	}
}
