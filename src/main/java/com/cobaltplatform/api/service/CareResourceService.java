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
import com.cobaltplatform.api.model.db.Address;
import com.cobaltplatform.api.model.db.CareResource;
import com.cobaltplatform.api.model.db.CareResourceLocation;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Payor;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.util.Authenticator;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

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
	private final Authenticator authenticator;
	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final Provider<SystemService> systemServiceProvider;
	@Nonnull
	private final Object backgroundTaskLock;
	@Nonnull
	private Boolean backgroundTaskStarted;
	@Nullable
	private ScheduledExecutorService backgroundTaskExecutorService;

	@Nonnull
	private final Provider<MessageService> messageServiceProvider;

	@Inject
	public CareResourceService(@Nonnull DatabaseProvider databaseProvider,
														 @Nonnull Strings strings,
														 @Nonnull Authenticator authenticator,
														 @Nonnull Provider<AccountService> accountServiceProvider,
														 @Nonnull Provider<SystemService> systemServiceProvider,
														 @Nonnull Provider<MessageService> messageServiceProvider) {
		requireNonNull(databaseProvider);
		requireNonNull(strings);
		requireNonNull(authenticator);
		requireNonNull(accountServiceProvider);
		requireNonNull(systemServiceProvider);
		requireNonNull(messageServiceProvider);

		this.databaseProvider = databaseProvider;
		this.strings = strings;
		this.authenticator = authenticator;
		this.accountServiceProvider = accountServiceProvider;
		this.systemServiceProvider = systemServiceProvider;
		this.logger = LoggerFactory.getLogger(getClass());
		this.backgroundTaskLock = new Object();
		this.backgroundTaskStarted = false;
		this.messageServiceProvider = messageServiceProvider;
	}

	public List<Payor> findPayors() {
		return getDatabase().queryForList("""
				SELECT * 
				FROM payor""", Payor.class);
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

	public List<CareResource> findAllCareResourceByInstitutionId(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);

		return getDatabase().queryForList("""
				SELECT cr.*
				FROM care_resource cr, care_resource_institution cri
				WHERE cr.care_resource_id = cri.care_resource_id
				AND cri.institution_id = ?
				""", CareResource.class, institutionId);
	}

	public List<CareResourceLocation> findAllCareResourceAddresses(@Nonnull UUID careResourceId) {
		requireNonNull(careResourceId);

		return getDatabase().queryForList("""
				SELECT crl.*
				FROM care_resource_location crl
				WHERE crl.care_resource_id = ?
				""", CareResourceLocation.class, careResourceId);
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

		if (request.getSpecialtyIds() == null || request.getSpecialtyIds().size() == 0)
			validationException.add(new ValidationException.FieldError("specialtyIds", "At least one specialty is required."));

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
					INSERT INTO care_resource_specialty_resource
					(care_resource_id, care_resource_specialty_id)
					VALUES
					(?,?)""", careResourceId, specialtyId);

		for (String payorId : request.getPayorIds())
			getDatabase().execute("""
					INSERT INTO care_resource_payor
					(care_resource_id, payor_id)
					VALUES
					(?,?)""", careResourceId, payorId);

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
	protected Authenticator getAuthenticator() {
		return this.authenticator;
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountServiceProvider.get();
	}

	@Nonnull
	protected SystemService getSystemService() {
		return this.systemServiceProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	protected Object getBackgroundTaskLock() {
		return this.backgroundTaskLock;
	}

	@Nonnull
	protected Optional<ScheduledExecutorService> getBackgroundTaskExecutorService() {
		return Optional.ofNullable(this.backgroundTaskExecutorService);
	}

	@Nonnull
	protected MessageService getMessageService() {
		return this.messageServiceProvider.get();
	}
}
