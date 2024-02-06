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
import com.cobaltplatform.api.model.db.Color.ColorId;
import com.cobaltplatform.api.model.db.Feature.FeatureId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionBlurb;
import com.cobaltplatform.api.model.db.InstitutionBlurbTeamMember;
import com.cobaltplatform.api.model.db.InstitutionColorValue;
import com.cobaltplatform.api.model.db.InstitutionLocation;
import com.cobaltplatform.api.model.db.InstitutionTeamMember;
import com.cobaltplatform.api.model.db.InstitutionUrl;
import com.cobaltplatform.api.model.db.ScreeningFlow;
import com.cobaltplatform.api.model.db.ScreeningFlowVersion;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;
import com.cobaltplatform.api.model.service.AccountSourceForInstitution;
import com.cobaltplatform.api.model.service.FeatureForInstitution;
import com.cobaltplatform.api.util.JsonMapper;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.WebUtility.normalizedHostnameForUrl;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class InstitutionService {
	@Nonnull
	private final DatabaseProvider databaseProvider;
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
	@Nonnull
	private final Provider<FeatureService> featureServiceProvider;
	@Nonnull
	private final LoadingCache<WebappBaseUrlCacheKey, Optional<String>> webappBaseUrlCache;

	@Inject
	public InstitutionService(@Nonnull DatabaseProvider databaseProvider,
														@Nonnull JsonMapper jsonMapper,
														@Nonnull Configuration configuration,
														@Nonnull Strings strings,
														@Nonnull Provider<ScreeningService> screeningServiceProvider,
														@Nonnull Provider<FeatureService> featureServiceProvider) {
		requireNonNull(databaseProvider);
		requireNonNull(jsonMapper);
		requireNonNull(configuration);
		requireNonNull(strings);
		requireNonNull(screeningServiceProvider);
		requireNonNull(featureServiceProvider);

		this.databaseProvider = databaseProvider;
		this.jsonMapper = jsonMapper;
		this.configuration = configuration;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
		this.screeningServiceProvider = screeningServiceProvider;
		this.featureServiceProvider = featureServiceProvider;
		this.webappBaseUrlCache = Caffeine.newBuilder()
				.maximumSize(100)
				.refreshAfterWrite(Duration.ofMinutes(5))
				.expireAfterWrite(Duration.ofMinutes(10))
				.build(key -> findUncachedWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(key));
	}

	@Nonnull
	public Optional<Institution> findInstitutionById(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM institution WHERE institution_id=?",
				Institution.class, institutionId);
	}

	@Nonnull
	public Optional<String> findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(@Nullable InstitutionId institutionId,
																																									@Nullable UserExperienceTypeId userExperienceTypeId) {
		if (institutionId == null || userExperienceTypeId == null)
			return Optional.empty();

		WebappBaseUrlCacheKey webappBaseUrlCacheKey = new WebappBaseUrlCacheKey(institutionId, userExperienceTypeId);
		return getWebappBaseUrlCache().get(webappBaseUrlCacheKey);
	}

	@Nonnull
	protected Optional<String> findUncachedWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(@Nullable WebappBaseUrlCacheKey webappBaseUrlCacheKey) {
		if (webappBaseUrlCacheKey == null)
			return Optional.empty();

		// First, see if we have a URL matching the user experience type for the institution
		InstitutionUrl institutionUrl = getDatabase().queryForObject("""
				    SELECT *
				    FROM institution_url
				    WHERE institution_id=?
				    AND preferred=TRUE
				    AND user_experience_type_id=?
				""", InstitutionUrl.class, webappBaseUrlCacheKey.getInstitutionId(), webappBaseUrlCacheKey.getUserExperienceTypeId()).orElse(null);

		// ...if not, just pick the URL regardless of user experience type (perhaps institution does not have
		// multiple user experience types)
		if (institutionUrl == null)
			institutionUrl = getDatabase().queryForObject("""
					    SELECT *
					    FROM institution_url
					    WHERE institution_id=?
					    AND preferred=TRUE
					""", InstitutionUrl.class, webappBaseUrlCacheKey.getInstitutionId()).orElse(null);

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

		List<AccountSourceForInstitution> accountSources = getDatabase().queryForList("""
						SELECT ias.institution_id, a.account_source_id, ias.account_source_display_style_id,
						a.description, a.short_description, ias.authentication_description, a.local_sso_url, a.dev_sso_url,
						a.prod_sso_url, ias.display_order, ias.requires_user_experience_type_id, ias.visible
						FROM institution_account_source ias, account_source a
						WHERE ias.institution_id=?
						AND ias.account_source_id=a.account_source_id
						ORDER BY ias.display_order
				""", AccountSourceForInstitution.class, institutionId);

		// Adjust URLs to include institution ID query parameter to support scenario where the same SSO account source is
		// used across multiple institutions
		accountSources.stream().forEach(accountSource -> {
			accountSource.setLocalSsoUrl(applyInstititutionIdToUrl(accountSource.getLocalSsoUrl(), institutionId).orElse(null));
			accountSource.setDevSsoUrl(applyInstititutionIdToUrl(accountSource.getDevSsoUrl(), institutionId).orElse(null));
			accountSource.setProdSsoUrl(applyInstititutionIdToUrl(accountSource.getProdSsoUrl(), institutionId).orElse(null));
		});

		return accountSources;
	}

	@Nonnull
	protected Optional<String> applyInstititutionIdToUrl(@Nullable String url,
																											 @Nonnull InstitutionId institutionId) {
		requireNonNull(institutionId);

		url = trimToNull(url);

		if (url == null || !url.startsWith("http"))
			return Optional.empty();

		if (url.contains("?"))
			return Optional.of(format("%s&institutionId=%s", url, institutionId.name()));

		return Optional.of(format("%s?institutionId=%s", url, institutionId.name()));
	}

	@Nonnull
	public List<Institution> findInstitutionsWhoAreSharingWithMe() {
		return getDatabase().queryForList("""
				SELECT * 
				FROM institution 
				WHERE sharing_content = true ORDER BY institution_id
				""", Institution.class);
	}

	@Nonnull
	public List<Institution> findInstitutions() {
		return getDatabase().queryForList("SELECT * FROM institution ORDER BY institution_id", Institution.class);
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
	public List<FeatureForInstitution> findFeaturesByInstitutionId(@Nullable Institution institution,
																																 @Nullable Account account) {
		if (institution == null || account == null)
			return List.of();

		ScreeningFlow screeningFlow = getScreeningService().findScreeningFlowById(institution.getFeatureScreeningFlowId()).orElse(null);
		ScreeningFlowVersion screeningFlowVersion = screeningFlow == null ? null : getScreeningService().findScreeningFlowVersionById(screeningFlow.getActiveScreeningFlowVersionId()).orElse(null);

		ScreeningSession mostRecentCompletedFeatureScreeningSession =
				getScreeningService().findMostRecentCompletedScreeningSession(account.getAccountId(), institution.getFeatureScreeningFlowId()).orElse(null);

		UUID screeningSessionId = null;

		if (mostRecentCompletedFeatureScreeningSession != null)
			if (screeningFlowVersion.getRecommendationExpirationMinutes() == null
					|| (Duration.between(mostRecentCompletedFeatureScreeningSession.getCompletedAt(), Instant.now()).toMinutes()
					< screeningFlowVersion.getRecommendationExpirationMinutes()))
				screeningSessionId = mostRecentCompletedFeatureScreeningSession.getScreeningSessionId();

		List<FeatureForInstitution> features = getDatabase().queryForList("SELECT f.feature_id, f.url_name, COALESCE(if.name_override, f.name) AS name, if.description, if.nav_description, if.nav_visible, if.landing_page_visible, if.treatment_description, " +
				"CASE WHEN ss.screening_session_id IS NOT NULL THEN true ELSE false END AS recommended, f.navigation_header_id " +
				"FROM institution_feature if, feature f  " +
				"LEFT OUTER JOIN screening_session_feature_recommendation ss " +
				"ON f.feature_id = ss.feature_id " +
				"AND ss.screening_session_id = ? " +
				"WHERE f.feature_id = if.feature_id AND if.institution_id = ? ORDER BY if.display_order", FeatureForInstitution.class, screeningSessionId, institution.getInstitutionId());

		features.stream().map(feature -> {
			List<SupportRoleId> supportRoleIds = getFeatureService().findSupportRoleByFeatureId(feature.getFeatureId());
			feature.setSupportRoleIds(supportRoleIds);
			if (!account.getPromptedForInstitutionLocation() && getFeatureService().featureSupportsLocation(feature.getFeatureId()))
				feature.setLocationPromptRequired(true);
			else
				feature.setLocationPromptRequired(false);

			// Special case for recommended content - only some institutions support content screening/reqs,
			// so only include the query param for those institutions.
			// If no query param, user is sent to regular content landing page
			if (feature.getFeatureId().equals(FeatureId.SELF_HELP_RESOURCES)
					&& feature.getRecommended()
					&& institution.getRecommendedContentEnabled()) {
				feature.setUrlName(format("%s?recommended=true", feature.getUrlName()));
			}

			return true;
		}).collect(Collectors.toList());

		return features;
	}

	@Nonnull
	public List<InstitutionLocation> findLocationsByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT *
				FROM institution_location
				WHERE institution_id=?
				ORDER BY display_order
				""", InstitutionLocation.class, institutionId);
	}

	@Nonnull
	public List<InstitutionColorValue> findInstitutionColorValuesByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT *
				FROM v_institution_color_value
				WHERE institution_id=?
				ORDER BY name
				""", InstitutionColorValue.class, institutionId);
	}

	@Nonnull
	public List<InstitutionColorValue> findInstitutionColorValuesByInstitutionId(@Nullable InstitutionId institutionId,
																																							 @Nullable ColorId colorId) {
		if (institutionId == null || colorId == null)
			return Collections.emptyList();

		// Ordering is darkest first
		return getDatabase().queryForList("""
					SELECT icv.*
					FROM color_value cv, v_institution_color_value icv
					WHERE icv.color_value_id=cv.color_value_id
					AND cv.color_id=?
					AND icv.institution_id=?
					ORDER BY -LENGTH(icv.color_value_id), icv.color_value_id DESC
				""", InstitutionColorValue.class, colorId, institutionId);
	}

	@Immutable
	private static final class WebappBaseUrlCacheKey {
		@Nonnull
		private final InstitutionId institutionId;
		@Nonnull
		private final UserExperienceTypeId userExperienceTypeId;

		public WebappBaseUrlCacheKey(@Nonnull InstitutionId institutionId,
																 @Nonnull UserExperienceTypeId userExperienceTypeId) {
			requireNonNull(institutionId);
			requireNonNull(userExperienceTypeId);

			this.institutionId = institutionId;
			this.userExperienceTypeId = userExperienceTypeId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			WebappBaseUrlCacheKey that = (WebappBaseUrlCacheKey) o;
			return getInstitutionId() == that.getInstitutionId() && getUserExperienceTypeId() == that.getUserExperienceTypeId();
		}

		@Override
		public int hashCode() {
			return Objects.hash(getInstitutionId(), getUserExperienceTypeId());
		}

		@Nonnull
		public InstitutionId getInstitutionId() {
			return this.institutionId;
		}

		@Nonnull
		public UserExperienceTypeId getUserExperienceTypeId() {
			return this.userExperienceTypeId;
		}
	}


	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected JsonMapper getJsonMapper() {
		return this.jsonMapper;
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	protected ScreeningService getScreeningService() {
		return this.screeningServiceProvider.get();
	}

	@Nonnull
	protected FeatureService getFeatureService() {
		return this.featureServiceProvider.get();
	}

	@Nonnull
	protected LoadingCache<WebappBaseUrlCacheKey, Optional<String>> getWebappBaseUrlCache() {
		return this.webappBaseUrlCache;
	}
}
