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
import com.cobaltplatform.api.model.db.AccountSource.AccountSourceId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.util.JsonMapper;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class InstitutionService {
	@Nonnull
	private final Database database;
	@Nonnull
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public InstitutionService(@Nonnull Database database,
														@Nonnull JsonMapper jsonMapper,
														@Nonnull Configuration configuration,
														@Nonnull Strings strings) {
		requireNonNull(database);
		requireNonNull(jsonMapper);
		requireNonNull(configuration);
		requireNonNull(strings);

		this.database = database;
		this.jsonMapper = jsonMapper;
		this.configuration = configuration;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<Institution> findInstitutionById(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM institution WHERE institution_id=?",
				Institution.class, institutionId);
	}

	@Nonnull
	public Institution findInstitutionBySubdomain(@Nullable String subdomain) {
		subdomain = (subdomain == null ? "" : subdomain).trim().toLowerCase(Locale.US);

		Institution institution = getDatabase().queryForObject("SELECT * FROM institution WHERE LOWER(subdomain)=?",
				Institution.class, subdomain).orElse(null);

		return institution == null ? findInstitutionById(getConfiguration().getDefaultSubdomainInstitutionId()).get() : institution;
	}

	@Nonnull
	public Institution findInstitutionByWebappBaseUrl(@Nullable String webappBaseUrl) {
		webappBaseUrl = trimToEmpty(webappBaseUrl).toLowerCase(Locale.US);

		if (webappBaseUrl.length() == 0)
			return findInstitutionBySubdomain(null);

		// Assume input is equivalent to window.location.origin in JS
		// See https://developer.mozilla.org/en-US/docs/Web/API/Location/origin
		//
		// Example: https://subdomain.cobaltinnovations.org

		if (webappBaseUrl.startsWith("https://"))
			webappBaseUrl = webappBaseUrl.substring("https://".length());
		else if (webappBaseUrl.startsWith("http://"))
			webappBaseUrl = webappBaseUrl.substring("http://".length());

		// Discard any trailing port number
		int portNumberSeparator = webappBaseUrl.indexOf(":");

		if (portNumberSeparator != -1)
			webappBaseUrl = webappBaseUrl.substring(0, portNumberSeparator);

		String[] components = webappBaseUrl.split("\\.");
		String subdomain;

		// Length 1 is special case, like "localhost"
		if (components.length == 1)
			subdomain = components[0];
		else
			subdomain = components.length > 0 ? trimToNull(components[0]) : null;

		return findInstitutionBySubdomain(subdomain);
	}

	@Nonnull
	public List<AccountSource> findAccountSourcesByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT a.* FROM account_source a, institution_account_source ia " +
						"WHERE a.account_source_id = ia.account_source_id AND ia.institution_id = ? ",
				AccountSource.class, institutionId);
	}

	@Nonnull
	public List<AccountSource> findAccountSourcesByInstitutionIdAndAccountSourceId(@Nullable InstitutionId institutionId,
																																								 @Nullable AccountSourceId accountSourceId) {
		if (institutionId == null || accountSourceId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT a.* FROM account_source a, institution_account_source ia " +
						"WHERE a.account_source_id = ia.account_source_id AND ia.institution_id = ? AND a.account_source_id = ? ",
				AccountSource.class, institutionId, accountSourceId);
	}

	@Nonnull
	public List<Institution> findNonCobaltInstitutions() {
		return findInstitutions().stream()
				.filter(institution -> institution.getInstitutionId() != InstitutionId.COBALT)
				.collect(Collectors.toList());
	}

	@Nonnull
	public List<Institution> findInstitutions() {
		return getDatabase().queryForList("SELECT * FROM institution ORDER BY institution_id", Institution.class);
	}

	@Nonnull
	public List<Institution> findInstitutionsWithoutSpecifiedContentId(@Nullable UUID contentId) {
		if (contentId == null)
			return findNonCobaltInstitutions();

		return getDatabase().queryForList("SELECT i.* FROM institution i WHERE i.institution_id NOT IN " +
				"(SELECT ic.institution_id FROM institution_content ic WHERE ic.content_id = ?)", Institution.class, contentId);
	}

	@Nonnull
	public List<Institution> findNetworkInstitutions(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

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
	public List<Institution> findInstitutionsMatchingMetadata(@Nullable Map<String, Object> metadata) {
		requireNonNull(metadata);

		if (metadata == null || metadata.size() == 0)
			return Collections.emptyList();

		String metadataAsJson = getJsonMapper().toJson(metadata);

		return getDatabase().queryForList("SELECT * FROM institution WHERE metadata @> CAST(? AS JSONB)",
				Institution.class, metadataAsJson);
	}

	@Nonnull
	protected Database getDatabase() {
		return database;
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return jsonMapper;
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
