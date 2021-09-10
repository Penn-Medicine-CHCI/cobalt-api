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
import com.cobaltplatform.api.model.db.AccountSource;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
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
public class InstitutionService {
	@Nonnull
	private final Database database;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public InstitutionService(@Nonnull Database database,
														@Nonnull Configuration configuration,
														@Nonnull Strings strings) {
		requireNonNull(database);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.database = database;
		this.configuration = configuration;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<Institution> findInstitutionById(@Nonnull InstitutionId institutionId) {

		return getDatabase().queryForObject("SELECT * FROM institution WHERE institution_id=?",
				Institution.class, institutionId);
	}

	@Nonnull
	public Optional<Institution> findInstitutionBySubdomain(@Nonnull String subdomain) {

		Institution institution = getDatabase().queryForObject("SELECT * FROM institution WHERE LOWER(subdomain)=?",
				Institution.class, subdomain.toLowerCase()).orElse(null);

		return Optional.of(institution == null ? findInstitutionById(InstitutionId.COBALT).get() : institution);
	}

	@NonNull
	public List<AccountSource> findAccountSourcesForByInstitutionId(String institutionId) {

		return getDatabase().queryForList("SELECT a.* FROM account_source a, institution_account_source ia " +
						"WHERE a.account_source_id = ia.account_source_id AND ia.institution_id = ? ",
				AccountSource.class, institutionId);
	}

	@Nonnull
	public List<Institution> findInstitutions() {
		return getDatabase().queryForList("SELECT * FROM institution WHERE institution_id != ?", Institution.class, InstitutionId.COBALT);
	}

	@Nonnull
	public List<Institution> findInstitutionsWithoutSpecifiedContentId(@Nonnull UUID contentId) {
		requireNonNull(contentId);

		return getDatabase().queryForList("SELECT i.* FROM institution i WHERE i.institution_id NOT IN " +
						"(SELECT ic.institution_id FROM institution_content ic WHERE ic.content_id = ?)"
				, Institution.class, contentId);
	}

	@Nonnull
	public List<Institution> findNetworkInstitutions(@Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);

		return getDatabase().queryForList("SELECT i.* FROM institution i, institution_network ii WHERE " +
				"i.institution_id = ii.related_institution_id AND ii.parent_institution_id = ? ", Institution.class, institutionId);
	}

	@Nonnull
	public List<Institution> findSelectedNetworkInstitutionsForContentId(@Nonnull InstitutionId institutionId,
																																			 @Nonnull UUID contentId) {
		requireNonNull(institutionId);
		requireNonNull(contentId);

		return getDatabase().queryForList("SELECT i.institution_id, i.name FROM institution i, institution_network ii, institution_content ic WHERE " +
				"i.institution_id = ii.related_institution_id AND ii.related_institution_id = ic. institution_id " +
				"AND ii.parent_institution_id = ? AND ic.content_id = ?", Institution.class, institutionId, contentId);
	}

	@Nonnull
	protected Database getDatabase() {
		return database;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
