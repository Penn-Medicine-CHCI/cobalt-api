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
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionBlurb;
import com.cobaltplatform.api.model.db.InstitutionBlurbTeamMember;
import com.cobaltplatform.api.model.db.InstitutionLocation;
import com.cobaltplatform.api.model.db.InstitutionTeamMember;
import com.cobaltplatform.api.model.db.InstitutionUrl;
import com.cobaltplatform.api.model.db.Insurance;
import com.cobaltplatform.api.model.db.InsuranceType.InsuranceTypeId;
import com.cobaltplatform.api.model.db.ScreeningFlow;
import com.cobaltplatform.api.model.db.ScreeningFlowVersion;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.service.AccountSourceForInstitution;
import com.cobaltplatform.api.model.service.Feature;
import com.cobaltplatform.api.util.JsonMapper;
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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.WebUtility.normalizedHostnameForUrl;
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
	private final JsonMapper jsonMapper;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final Provider<ScreeningService> screeningServiceProvider;

	@Inject
	public InstitutionService(@Nonnull Database database,
														@Nonnull JsonMapper jsonMapper,
														@Nonnull Configuration configuration,
														@Nonnull Strings strings,
														@Nonnull Provider<ScreeningService> screeningServiceProvider) {
		requireNonNull(database);
		requireNonNull(jsonMapper);
		requireNonNull(configuration);
		requireNonNull(strings);
		requireNonNull(screeningServiceProvider);

		this.database = database;
		this.jsonMapper = jsonMapper;
		this.configuration = configuration;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
		this.screeningServiceProvider = screeningServiceProvider;
	}

	@Nonnull
	public Optional<Institution> findInstitutionById(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM institution WHERE institution_id=?",
				Institution.class, institutionId);
	}

	@Nonnull
	public Optional<String> findWebappBaseUrlByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Optional.empty();

		InstitutionUrl institutionUrl = getDatabase().queryForObject("""
				    SELECT *
				    FROM institution_url
				    WHERE institution_id=?
				    AND preferred=TRUE
				""", InstitutionUrl.class, institutionId).orElse(null);

		if (institutionUrl == null)
			return Optional.empty();

		return Optional.of(institutionUrl.getUrl());
	}

	@Nonnull
	public Optional<Institution> findInstitutionByWebappBaseUrl(@Nullable String webappBaseUrl) {
		if (webappBaseUrl == null || webappBaseUrl.trim().length() == 0)
			return Optional.empty();

		String hostname = normalizedHostnameForUrl(webappBaseUrl).orElse(null);

		if (hostname == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT i.* 
				FROM institution_url iu, institution i
				WHERE iu.hostname=?
				AND iu.institution_id=i.institution_id
				""", Institution.class, hostname);
	}

	@Nonnull
	public Optional<InstitutionUrl> findInstitutionUrlByWebappBaseUrl(@Nullable String webappBaseUrl) {
		if (webappBaseUrl == null)
			return Optional.empty();

		String hostname = normalizedHostnameForUrl(webappBaseUrl).orElse(null);

		if (hostname == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT * 
				FROM institution_url
				WHERE hostname=?
				""", InstitutionUrl.class, hostname);
	}

	@Nonnull
	public List<AccountSourceForInstitution> findAccountSourcesByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
						SELECT ias.institution_id, a.account_source_id, ias.account_source_display_style_id,
						a.description, ias.authentication_description, a.local_sso_url, a.dev_sso_url,
						a.prod_sso_url, ias.display_order 
						FROM institution_account_source ias, account_source a
						WHERE ias.institution_id=?
						AND ias.account_source_id=a.account_source_id
						ORDER BY ias.display_order
				""", AccountSourceForInstitution.class, institutionId);
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
	public List<Insurance> findInsurancesByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		List<Insurance> insurances = getDatabase().queryForList("""
				    SELECT i.*
				    FROM insurance i, institution_insurance ii
				    WHERE ii.institution_id=?
				    AND ii.insurance_id=i.insurance_id
				    ORDER BY i.description
				""", Insurance.class, institutionId);

		Insurance outOfPocket = insurances.stream()
				.filter(insurance -> insurance.getInsuranceTypeId() == InsuranceTypeId.OUT_OF_POCKET)
				.findFirst().orElse(null);

		Insurance other = insurances.stream()
				.filter(insurance -> insurance.getInsuranceTypeId() == InsuranceTypeId.OTHER)
				.findFirst().orElse(null);

		// If present, put out-of-pocket at the head of the list
		if (outOfPocket != null) {
			insurances.remove(outOfPocket);
			insurances.add(0, outOfPocket);
		}

		// If present, put 'other' at the tail of the list
		if (other != null) {
			insurances.remove(other);
			insurances.add(other);
		}

		return insurances;
	}

	@Nonnull
	public List<InstitutionBlurb> findInstitutionBlurbsByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM institution_blurb
				WHERE institution_id=?
				""", InstitutionBlurb.class, institutionId);
	}

	@Nonnull
	public List<InstitutionTeamMember> findInstitutionTeamMembersByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM institution_team_member
				WHERE institution_id=?
				ORDER BY name
				""", InstitutionTeamMember.class, institutionId);
	}

	@Nonnull
	public Map<UUID, List<InstitutionTeamMember>> findInstitutionTeamMembersByInstitutionBlurbIdForInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Map.of();

		List<InstitutionBlurbTeamMember> institutionBlurbTeamMembers = getDatabase().queryForList("""
				SELECT ibtm.*
				FROM institution_blurb_team_member ibtm, institution_blurb ib
				WHERE ibtm.institution_blurb_id=ib.institution_blurb_id
				AND ib.institution_id=?
				ORDER BY ibtm.display_order    
				""", InstitutionBlurbTeamMember.class, institutionId);

		Map<UUID, InstitutionTeamMember> institutionTeamMembersById = new HashMap();

		for (InstitutionTeamMember institutionTeamMember : findInstitutionTeamMembersByInstitutionId(institutionId))
			institutionTeamMembersById.put(institutionTeamMember.getInstitutionTeamMemberId(), institutionTeamMember);

		Map<UUID, List<InstitutionTeamMember>> institutionTeamMembersByInstitutionBlurbId = new HashMap<>();

		for (InstitutionBlurbTeamMember institutionBlurbTeamMember : institutionBlurbTeamMembers) {
			InstitutionTeamMember institutionTeamMember = institutionTeamMembersById.get(institutionBlurbTeamMember.getInstitutionTeamMemberId());
			List<InstitutionTeamMember> institutionTeamMembers = institutionTeamMembersByInstitutionBlurbId.get(institutionBlurbTeamMember.getInstitutionBlurbId());

			if (institutionTeamMembers == null) {
				institutionTeamMembers = new ArrayList<>();
				institutionTeamMembersByInstitutionBlurbId.put(institutionBlurbTeamMember.getInstitutionBlurbId(), institutionTeamMembers);
			}

			institutionTeamMembers.add(institutionTeamMember);
		}

		return institutionTeamMembersByInstitutionBlurbId;
	}

	@Nonnull
	public List<Feature> findFeaturesByInstitutionId(@Nullable Institution institution, @Nullable Account account) {
		Optional<ScreeningFlow> screeningFlow = getScreeningServiceProvider().get().findScreeningFlowById(institution.getProviderTriageScreeningFlowId());
		if (institution == null || account == null || !screeningFlow.isPresent())
			return List.of();

		Optional<ScreeningFlowVersion> screeningFlowVersion = getScreeningServiceProvider().get().findScreeningFlowVersionById(screeningFlow.get().getActiveScreeningFlowVersionId());
		if (!screeningFlowVersion.isPresent())
			return List.of();

		Optional<ScreeningSession> mostRecentCompletedTriageScreeningSession =
				getScreeningServiceProvider().get().findMostRecentCompletedTriageScreeningSession(account.getAccountId(), institution.getProviderTriageScreeningFlowId());

		UUID screeningSessionId = null;

		if (mostRecentCompletedTriageScreeningSession.isPresent())
			if (Duration.between(mostRecentCompletedTriageScreeningSession.get().getCompletedAt(), Instant.now()).toMinutes()
					< screeningFlowVersion.get().getRecommendationExpirationMinutes())
				screeningSessionId = mostRecentCompletedTriageScreeningSession.get().getScreeningSessionId();

		return getDatabase().queryForList("SELECT f.feature_id, f.url_name, f.name, if.description, if.nav_description, "+
				"CASE WHEN ss.screening_session_id IS NOT NULL THEN true ELSE false END AS recommended, f.navigation_header_id " +
				"FROM feature f, institution_feature if  " +
				"LEFT OUTER JOIN screening_session_feature_recommendation ss " +
				"ON if.institution_feature_id = ss.institution_feature_id " +
				"AND ss.screening_session_id = ? " +
				"WHERE f.feature_id = if.feature_id AND if.institution_id = ? ORDER BY if.display_order", Feature.class, screeningSessionId, institution.getInstitutionId());
	}


	@Nonnull
	public List<InstitutionLocation> findLocationsInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM institution_location WHERE institution_id = ?", InstitutionLocation.class, institutionId);
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

	@Nonnull
	public Provider<ScreeningService> getScreeningServiceProvider() {
		return screeningServiceProvider;
	}
}
