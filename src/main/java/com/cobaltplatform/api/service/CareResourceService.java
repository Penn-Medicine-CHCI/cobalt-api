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


import com.cobaltplatform.api.model.api.request.CreateCareResourceRequest;
import com.cobaltplatform.api.model.api.request.FindCareResourcesRequest;
import com.cobaltplatform.api.model.db.CareResource;
import com.cobaltplatform.api.model.db.CareResourceLocation;
import com.cobaltplatform.api.model.db.CareResourceSpecialty;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Language;
import com.cobaltplatform.api.model.db.Payor;
import com.cobaltplatform.api.model.db.SupportRole;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.db.DatabaseProvider;
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

	@Inject
	public CareResourceService(@Nonnull DatabaseProvider databaseProvider,
														 @Nonnull Strings strings,
														 @Nonnull Provider<AccountService> accountServiceProvider) {
		requireNonNull(databaseProvider);
		requireNonNull(strings);
		requireNonNull(accountServiceProvider);

		this.databaseProvider = databaseProvider;
		this.strings = strings;
		this.accountServiceProvider = accountServiceProvider;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	public List<Payor> findPayors() {
		return getDatabase().queryForList("""
				SELECT * 
				FROM payor
				ORDER BY name""", Payor.class);
	}

	public List<SupportRole> findCareResourceSupportRoles() {
		return getDatabase().queryForList("""
				SELECT sr.*
				FROM support_role sr
				ORDER BY display_order
				""", SupportRole.class);
	}

	public Optional<CareResource> findCareResourceByInstitutionId(@Nonnull UUID careResourceId,
																																@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);
		requireNonNull(careResourceId);

		return getDatabase().queryForObject("""
				SELECT cr.*
				FROM care_resource cr, care_resource_institution cri
				WHERE cr.care_resource_id = cri.care_resource_id
				AND cr.care_resource_id = ?
				AND cri.institution_id = ?
				""", CareResource.class, careResourceId, institutionId);
	}

	public FindResult<CareResource> findAllCareResourceByInstitutionId(@Nonnull FindCareResourcesRequest request) {
		requireNonNull(request);

		InstitutionId institutionId = request.getInstitutionId();
		Integer pageNumber = request.getPageNumber();
		Integer pageSize = request.getPageSize();

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

		parameters.add(institutionId);
		parameters.add(limit);
		parameters.add(offset);

		String query = """
				SELECT cr.*
				FROM care_resource cr, care_resource_institution cri
				WHERE cr.care_resource_id = cri.care_resource_id
				AND cri.institution_id = ?
				ORDER BY cr.name
				LIMIT ?
				OFFSET ?
				""";
		List<CareResource> careResources = getDatabase().queryForList(query, CareResource.class, parameters.toArray());

		FindResult<? extends CareResource> findResult = new FindResult<>(careResources, careResources.size());

		return (FindResult<CareResource>) findResult;
	}

	public List<CareResourceLocation> findCareResourceLocations(@Nonnull UUID careResourceId) {
		requireNonNull(careResourceId);

		return getDatabase().queryForList("""
				SELECT crl.*
				FROM care_resource_location crl
				WHERE crl.care_resource_id = ?
				""", CareResourceLocation.class, careResourceId);
	}

	public List<Language> findLanguagesForCareResourceLocation(@Nonnull UUID careResourceLocationId) {
		requireNonNull(careResourceLocationId);

		return getDatabase().queryForList("""
				SELECT l.*
				FROM language l, care_resource_location_language crl
				WHERE crl.language_id = l.language_id
				AND crl.care_resource_location_id = ?
				""", Language.class, careResourceLocationId);
	}

	public List<CareResourceSpecialty> findCareResourceSpecialties(@Nonnull UUID careResourceId) {
		requireNonNull(careResourceId);

		return getDatabase().queryForList("""
				SELECT crs.*
				FROM care_resource_specialty crs, care_resource_care_resource_specialty crc
				WHERE crs.care_resource_specialty_id= crc.care_resource_specialty_id
				AND crc.care_resource_id = ?
				""", CareResourceSpecialty.class, careResourceId);
	}

	public List<Payor> findCareResourcePayors(@Nonnull UUID careResourceId) {
		requireNonNull(careResourceId);

		return getDatabase().queryForList("""
				SELECT p.*
				FROM payor p, care_resource_payor crp
				WHERE p.payor_id= crp.payor_id
				AND crp.care_resource_id = ?
				""", Payor.class, careResourceId);
	}

	public List<SupportRole> findCareResourceSupportRoles(@Nonnull UUID careResourceId) {
		requireNonNull(careResourceId);

		return getDatabase().queryForList("""
				SELECT sr.*
				FROM support_role sr, care_resource_support_role crs
				WHERE sr.support_role_id= crs.support_role_id
				AND crs.care_resource_id = ?
				""", SupportRole.class, careResourceId);
	}

	public UUID createCareResource(@Nonnull CreateCareResourceRequest request) {
		requireNonNull(request);

		String name = trimToNull(request.getName());
		String notes = trimToNull(request.getNotes());
		String websiteUrl = trimToNull(request.getWebsiteUrl());
		String phoneNumber = trimToNull(request.getPhoneNumber());
		Boolean resourceAvailable = request.getResourceAvailable();
		UUID createdByAccountId = request.getCreatedByAccountId();
		UUID careResourceId = UUID.randomUUID();
		ValidationException validationException = new ValidationException();

		if (name == null)
			validationException.add(new ValidationException.FieldError("name", "Name is required."));

		if (phoneNumber == null)
			validationException.add(new ValidationException.FieldError("phoneNumber", "Phone number is required."));

		if (request.getSupportRoleIds() == null || request.getSupportRoleIds().size() == 0)
			validationException.add(new ValidationException.FieldError("supportRoleIds", "At least one therapy type is required."));

		if (request.getPayorIds() == null || request.getPayorIds().size() == 0)
			validationException.add(new ValidationException.FieldError("payorIds", "At least one insurance is required."));

		if (createdByAccountId == null)
			validationException.add(new ValidationException.FieldError("createdByAccountId", "Created by account ID is required."));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				INSERT INTO care_resource
				(care_resource_id, name, notes, website_url, phone_number, care_resource_available, created_by_account_id)
				VALUES
				(?,?,?,?,?,?,?)""", careResourceId, name, notes, websiteUrl, phoneNumber, resourceAvailable, createdByAccountId);

		for (UUID specialtyId : request.getSpecialtyIds())
			getDatabase().execute("""
					INSERT INTO care_resource_care_resource_specialty
					(care_resource_id, care_resource_specialty_id)
					VALUES
					(?,?)""", careResourceId, specialtyId);

		for (String payorId : request.getPayorIds()) {
			if (trimToNull(payorId) != null)
			getDatabase().execute("""
					INSERT INTO care_resource_payor
					(care_resource_id, payor_id)
					VALUES
					(?,?)""", careResourceId, payorId);
		}

		for (SupportRoleId supportRoleId : request.getSupportRoleIds())
			getDatabase().execute("""
					INSERT INTO care_resource_support_role
					(care_resource_id, support_role_id)
					VALUES
					(?,?)""", careResourceId, supportRoleId);

		getDatabase().execute("""
				INSERT INTO care_resource_institution 
				(care_resource_id, institution_id)
				VALUES
				(?,?)""", careResourceId, request.getInstitutionId());

		return careResourceId;
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

}
