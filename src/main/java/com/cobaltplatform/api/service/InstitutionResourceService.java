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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionResource;
import com.cobaltplatform.api.model.db.InstitutionResourceGroup;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class InstitutionResourceService {
	@Nonnull
	private final Database database;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public InstitutionResourceService(@Nonnull Database database,
																		@Nonnull Strings strings) {
		requireNonNull(database);
		requireNonNull(strings);

		this.database = database;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<InstitutionResource> findInstitutionResourceById(@Nullable UUID institutionResourceId) {
		if (institutionResourceId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM institution_resource
				WHERE institution_resource_id=?
				""", InstitutionResource.class, institutionResourceId);
	}

	@Nonnull
	public Optional<InstitutionResource> findInstitutionResourceByInstitutionIdAndUrlName(@Nullable InstitutionId institutionId,
																																												@Nullable String urlName) {
		if (institutionId == null || urlName == null)
			return Optional.empty();

		urlName = urlName.trim();

		return getDatabase().queryForObject("""
				SELECT *
				FROM institution_resource
				WHERE institution_id=?
				AND url_name=?
				""", InstitutionResource.class, institutionId, urlName);
	}

	@Nonnull
	public Optional<InstitutionResource> findInstitutionResourceByIdentifier(@Nullable Object institutionResourceIdentifier,
																																					 @Nullable InstitutionId institutionId) {
		if (institutionResourceIdentifier == null || institutionId == null)
			return Optional.empty();

		if (institutionResourceIdentifier instanceof UUID)
			return findInstitutionResourceById((UUID) institutionResourceIdentifier);

		if (institutionResourceIdentifier instanceof String)
			return findInstitutionResourceByInstitutionIdAndUrlName(institutionId, (String) institutionResourceIdentifier);

		return Optional.empty();
	}

	@Nonnull
	public List<InstitutionResourceGroup> findInstitutionResourceGroupsByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM v_institution_resource_group
				WHERE institution_id=?
				ORDER BY display_order
				""", InstitutionResourceGroup.class, institutionId);
	}

	@Nonnull
	public Optional<InstitutionResourceGroup> findInstitutionResourceGroupById(@Nullable UUID institutionResourceGroupId) {
		if (institutionResourceGroupId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_institution_resource_group
				WHERE institution_resource_group_id=?
				""", InstitutionResourceGroup.class, institutionResourceGroupId);
	}

	@Nonnull
	public Optional<InstitutionResourceGroup> findInstitutionResourceGroupByInstitutionIdAndUrlName(@Nullable InstitutionId institutionId,
																																																	@Nullable String urlName) {
		if (institutionId == null || urlName == null)
			return Optional.empty();

		urlName = urlName.trim();

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_institution_resource_group
				WHERE institution_id=?
				AND url_name=?
				""", InstitutionResourceGroup.class, institutionId, urlName);
	}

	@Nonnull
	public Optional<InstitutionResourceGroup> findInstitutionResourceGroupByIdentifier(@Nullable Object institutionResourceGroupIdentifier,
																																										 @Nullable InstitutionId institutionId) {
		if (institutionResourceGroupIdentifier == null || institutionId == null)
			return Optional.empty();

		if (institutionResourceGroupIdentifier instanceof UUID)
			return findInstitutionResourceGroupById((UUID) institutionResourceGroupIdentifier);

		if (institutionResourceGroupIdentifier instanceof String)
			return findInstitutionResourceGroupByInstitutionIdAndUrlName(institutionId, (String) institutionResourceGroupIdentifier);

		return Optional.empty();
	}

	@Nonnull
	public List<InstitutionResource> findInstitutionResourcesByInstitutionResourceGroupId(@Nullable UUID institutionResourceGroupId) {
		if (institutionResourceGroupId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT ir.*
				FROM institution_resource ir, institution_resource_group_institution_resource irgir
				WHERE ir.institution_resource_id=irgir.institution_resource_id
				AND irgir.institution_resource_group_id=?
				ORDER BY irgir.display_order
				""", InstitutionResource.class, institutionResourceGroupId);
	}

	@Nonnull
	protected Database getDatabase() {
		return this.database;
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
