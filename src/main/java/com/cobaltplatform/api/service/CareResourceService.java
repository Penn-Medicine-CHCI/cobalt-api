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
import com.cobaltplatform.api.model.api.request.CreateCareResourceLocationRequest;
import com.cobaltplatform.api.model.api.request.CreateCareResourceRequest;
import com.cobaltplatform.api.model.api.request.FindCareResourcesRequest;
import com.cobaltplatform.api.model.api.request.UpdateAddressRequest;
import com.cobaltplatform.api.model.api.request.UpdateCareResourceLocationRequest;
import com.cobaltplatform.api.model.api.request.UpdateCareResourceRequest;
import com.cobaltplatform.api.model.db.Address;
import com.cobaltplatform.api.model.db.CareResource;
import com.cobaltplatform.api.model.db.CareResourceLocation;
import com.cobaltplatform.api.model.db.CareResourceTag;
import com.cobaltplatform.api.model.db.CareResourceTag.CareResourceTagGroupId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
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
import java.util.UUID;

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
	private final PlaceService placeService;

	@Inject
	public CareResourceService(@Nonnull DatabaseProvider databaseProvider,
														 @Nonnull Strings strings,
														 @Nonnull Provider<AccountService> accountServiceProvider,
														 @Nonnull Provider<AddressService> addressServiceProvider,
														 @Nonnull PlaceService placeService) {
		requireNonNull(databaseProvider);
		requireNonNull(strings);
		requireNonNull(accountServiceProvider);
		requireNonNull(addressServiceProvider);
		requireNonNull(placeService);

		this.databaseProvider = databaseProvider;
		this.strings = strings;
		this.accountServiceProvider = accountServiceProvider;
		this.addressServiceProvider = addressServiceProvider;
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
		getLogger().debug(query.toString());
		List<CareResourceWithTotalCount> careResources = getDatabase().queryForList(query.toString(), CareResourceWithTotalCount.class, parameters.toArray());

		FindResult<? extends CareResource> findResult = new FindResult<>(careResources, careResources.size() == 0 ? 0 : careResources.get(0).getTotalCount());

		return (FindResult<CareResource>) findResult;
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
	public UUID updateCareResourceLocation(UpdateCareResourceLocationRequest request) {
		requireNonNull(request);

		String googlePlaceId = trimToNull(request.getGooglePlaceId());
		String notes = trimToNull(request.getNotes());
		String internalNotes = trimToNull(request.getInternalNotes());
		String emailAddress = trimToNull(request.getEmailAddress());
		String phoneNumber = trimToNull(request.getPhoneNumber());
		String streetAddress2 = trimToNull(request.getStreetAddress2());
		Boolean acceptingNewPatients = request.getAcceptingNewPatients();
		Boolean wheelchairAccessible = request.getWheelchairAccess();
		String websiteUrl = trimToNull(request.getWebsiteUrl());
		ValidationException validationException = new ValidationException();
		String name = trimToNull(request.getName());
		String insuranceNotes = trimToNull(request.getInsuranceNotes());
		InstitutionId institutionId = request.getInstitutionId();
		UUID careResourceLocationId = request.getCareResourceLocationId();
		UpdateAddressRequest updateAddressRequest = new UpdateAddressRequest();

		CareResourceLocation currentCareResourceLocation = findCareResourceLocationById(careResourceLocationId, institutionId).orElse(null);
		Address currentAddress = getAddressService().findAddressById(currentCareResourceLocation.getAddressId()).orElse(null);

		if (currentCareResourceLocation == null)
			validationException.add(new ValidationException.FieldError("careResourceLocation", "Could not find Care Resource Location."));
		if (currentAddress == null)
			validationException.add(new ValidationException.FieldError("address", "Could not find address for this location."));
		if (googlePlaceId == null)
			validationException.add(new ValidationException.FieldError("googlePlaceId", "Google Place Id is required."));

		if (validationException.hasErrors())
			throw validationException;

		String currentGooglePlaceId = trimToNull(currentAddress.getGooglePlaceId());

		if (!currentGooglePlaceId.equals(googlePlaceId)) {
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

		getDatabase().execute("""
						UPDATE care_resource_location
						SET phone_number = ?, wheelchair_access=?, notes=?, accepting_new_patients=?,
						website_url=?, name=?, insurance_notes=?, email_address =?, internal_notes =?
						WHERE care_resource_location_id = ?
						""",
				phoneNumber, wheelchairAccessible != null && wheelchairAccessible, notes, acceptingNewPatients != null && acceptingNewPatients,
				websiteUrl, name, insuranceNotes, emailAddress, internalNotes, careResourceLocationId);

		getDatabase().execute("""
				DELETE FROM care_resource_location_care_resource_tag
				WHERE care_resource_location_id=?
				""", careResourceLocationId);

		List<String> allTags = new ArrayList<>();
		if (request.getPayorIds() != null)
			allTags.addAll(request.getPayorIds());
		if (request.getEthnicityIds() != null)
			allTags.addAll(request.getEthnicityIds());
		if (request.getSpecialtyIds() != null)
			allTags.addAll(request.getSpecialtyIds());
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
		String internalNotes = trimToNull(request.getInternalNotes());
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
		String insuranceNotes = trimToNull(request.getInsuranceNotes());
		InstitutionId institutionId = request.getInstitutionId();

		CareResource careResource = findCareResourceById(careResourceId, institutionId).orElse(null);

		if (careResource == null)
			validationException.add(new ValidationException.FieldError("careResource", "Could not find Care Resource."));
		if (googlePlaceId == null)
			validationException.add(new ValidationException.FieldError("googlePlaceId", "Address is required."));
		if (request.getPayorIds() != null && request.getPayorIds().isEmpty() && findTagsByCareResourceIdAndGroupId(careResourceId, CareResourceTagGroupId.PAYORS).isEmpty())
			validationException.add(new ValidationException.FieldError("payors", "At least one insurance carrier is required."));


		if (validationException.hasErrors())
			throw validationException;

		Place place = getPlaceService().findPlaceByPlaceId(googlePlaceId);

		if (place == null)
			validationException.add(new ValidationException.FieldError("place", "Could not find the Google place"));

		if (validationException.hasErrors())
			throw validationException;

		// If there is no name for this location then use the Care Resource name
		if (name == null)
			name = careResource.getName();

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

		UUID addressId = getAddressService().createAddress(createAddressRequest);

		getDatabase().execute("""
						INSERT INTO care_resource_location
						  (care_resource_location_id, care_resource_id, address_id,
						  phone_number, wheelchair_access, notes, accepting_new_patients,
						  website_url, name, insurance_notes, created_by_account_id, email_address,
						  internal_notes)
						VALUES
						  (?,?,?,?,?,?,?,?,?,?,?,?,?)
						  """, careResourceLocationId, careResourceId, addressId,
				phoneNumber, wheelchairAccessible != null && wheelchairAccessible, notes, acceptingNewPatients != null && acceptingNewPatients,
				websiteUrl, name, insuranceNotes, createdByAccountId, emailAddress, internalNotes);

		List<String> allTags = new ArrayList<>();
		if (request.getPayorIds() != null)
			allTags.addAll(request.getPayorIds());
		if (request.getEthnicityIds() != null)
			allTags.addAll(request.getEthnicityIds());
		if (request.getSpecialtyIds() != null)
			allTags.addAll(request.getSpecialtyIds());
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
	public boolean deleteCareResource (@Nonnull UUID careResourceId) {
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
	public boolean deleteCareResourceLocation (@Nonnull UUID careResourceLocationId) {
		requireNonNull(careResourceLocationId);

		boolean deleted = getDatabase().execute("""
				UPDATE care_resource_location
				SET deleted = TRUE
				WHERE care_resource_location_id=?
				""", careResourceLocationId) > 0;

		return deleted;
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
