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

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.api.request.FindResourceLibraryContentRequest;
import com.cobaltplatform.api.model.api.request.PersonalizeAssessmentChoicesCommand.SubmissionAnswer;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSession;
import com.cobaltplatform.api.model.db.ActivityAction.ActivityActionId;
import com.cobaltplatform.api.model.db.ActivityType.ActivityTypeId;
import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.db.ContentStatus.ContentStatusId;
import com.cobaltplatform.api.model.db.ContentType;
import com.cobaltplatform.api.model.db.ContentType.ContentTypeId;
import com.cobaltplatform.api.model.db.ContentTypeLabel;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionContent;
import com.cobaltplatform.api.model.db.Tag;
import com.cobaltplatform.api.model.db.TagContent;
import com.cobaltplatform.api.model.service.AdminContent;
import com.cobaltplatform.api.model.service.ContentDurationId;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.LinkGenerator;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlInListPlaceholders;
import static com.cobaltplatform.api.util.DatabaseUtility.sqlVaragsParameters;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify LLC.
 */
@Singleton
public class ContentService {
	@Nonnull
	private static final int DEFAULT_PAGE_SIZE = 15;
	private static final int MAXIMUM_PAGE_SIZE = 100;

	@Nonnull
	private final Database database;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final SessionService sessionService;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final InstitutionService institutionService;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Provider<AssessmentService> assessmentServiceProvider;
	@Nonnull
	private final Provider<TagService> tagServiceProvider;
	@Nonnull
	private final Provider<MessageService> messageServiceProvider;
	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final Provider<Formatter> formatterProvider;
	@Nonnull
	private final Provider<LinkGenerator> linkGeneratorProvider;

	@Inject
	public ContentService(@Nonnull Provider<CurrentContext> currentContextProvider,
												@Nonnull Provider<AssessmentService> assessmentServiceProvider,
												@Nonnull Provider<TagService> tagServiceProvider,
												@Nonnull Provider<MessageService> messageServiceProvider,
												@Nonnull Provider<AccountService> accountServiceProvider,
												@Nonnull Provider<Formatter> formatterProvider,
												@Nonnull Provider<LinkGenerator> linkGeneratorProvider,
												@Nonnull Database database,
												@Nonnull SessionService sessionService,
												@Nonnull InstitutionService institutionService,
												@Nonnull Strings strings) {
		requireNonNull(currentContextProvider);
		requireNonNull(assessmentServiceProvider);
		requireNonNull(tagServiceProvider);
		requireNonNull(messageServiceProvider);
		requireNonNull(accountServiceProvider);
		requireNonNull(formatterProvider);
		requireNonNull(linkGeneratorProvider);
		requireNonNull(database);
		requireNonNull(sessionService);
		requireNonNull(institutionService);
		requireNonNull(strings);

		this.logger = LoggerFactory.getLogger(getClass());
		this.database = database;
		this.sessionService = sessionService;
		this.tagServiceProvider = tagServiceProvider;
		this.currentContextProvider = currentContextProvider;
		this.institutionService = institutionService;
		this.strings = strings;
		this.assessmentServiceProvider = assessmentServiceProvider;
		this.messageServiceProvider = messageServiceProvider;
		this.accountServiceProvider = accountServiceProvider;
		this.formatterProvider = formatterProvider;
		this.linkGeneratorProvider = linkGeneratorProvider;
	}

	@Nonnull
	public Optional<Content> findContentById(@Nullable Account account,
																					 @Nullable UUID contentId) {
		if (account == null || contentId == null)
			return Optional.empty();

		String institutionClause = " AND vc.institution_id = ? ";
		String institutionArg = account.getInstitutionId().name();
		if (account.getInstitutionId() == InstitutionId.COBALT) {
			institutionClause = " AND vc.institution_id LIKE ? ";
			institutionArg = "%";
		}

		String query = format("SELECT  DISTINCT ON (vc.content_id, new_flag) vc.*, " +
				"CASE WHEN activity_tracking_id IS NULL THEN true ELSE false " +
				"END as new_flag FROM v_institution_content vc " +
				"LEFT OUTER JOIN activity_tracking act ON vc.content_id = CAST (act.context ->> 'contentId' AS UUID) " +
				"AND act.account_id = ? WHERE vc.content_id=? %s", institutionClause);
		Content content = getDatabase().queryForObject(query,
				Content.class, account.getAccountId(), contentId, institutionArg).orElse(null);

		if (content == null)
			return Optional.empty();

		applyTagsToContents(content, account.getInstitutionId());

		return Optional.of(content);
	}

	@Nonnull
	public FindResult<Content> findResourceLibraryContent(@Nonnull FindResourceLibraryContentRequest request) {
		requireNonNull(request);

		InstitutionId institutionId = request.getInstitutionId();
		String searchQuery = trimToNull(request.getSearchQuery());
		Set<String> tagIds = request.getTagIds() == null ? Set.of() : request.getTagIds();
		Set<ContentTypeId> contentTypeIds = request.getContentTypeIds() == null ? Set.of() : request.getContentTypeIds();
		Set<ContentDurationId> contentDurationIds = request.getContentDurationIds() == null ? Set.of() : request.getContentDurationIds();
		Integer pageNumber = request.getPageNumber();
		Integer pageSize = request.getPageSize();
		String tagGroupId = trimToNull(request.getTagGroupId());
		UUID prioritizeUnviewedForAccountId = request.getPrioritizeUnviewedForAccountId();

		searchQuery = trimToNull(searchQuery);

		if (pageNumber == null || pageNumber < 0)
			pageNumber = 0;

		if (pageSize == null || pageSize <= 0)
			pageSize = DEFAULT_PAGE_SIZE;
		else if (pageSize > MAXIMUM_PAGE_SIZE)
			pageSize = MAXIMUM_PAGE_SIZE;

		Integer offset = pageNumber * pageSize;
		Integer limit = pageSize;
		List<String> fromClauseComponents = new ArrayList<>();
		List<String> whereClauseComponents = new ArrayList<>();
		String contentViewedQuery = null;
		String contentViewedSelect = null;
		String contentViewedJoin = null;
		String contentViewedOrderBy = null;
		List<Object> parameters = new ArrayList<>();

		if (tagGroupId != null) {
			fromClauseComponents.add("tag_content tc");
			fromClauseComponents.add("tag t");

			whereClauseComponents.add("AND tc.content_id=c.content_id");
			whereClauseComponents.add("AND tc.institution_id=?");
			whereClauseComponents.add("AND tc.tag_id=t.tag_id");
			whereClauseComponents.add("AND t.tag_group_id=?");

			parameters.add(institutionId);
			parameters.add(tagGroupId);

			if (tagIds.size() > 0) {
				whereClauseComponents.add(format("AND tc.tag_id IN %s", sqlInListPlaceholders(tagIds)));
				parameters.addAll(tagIds);
			}
		} else if (tagIds.size() > 0) {
			fromClauseComponents.add("tag_content tc");

			whereClauseComponents.add("AND tc.content_id=c.content_id");
			whereClauseComponents.add("AND tc.institution_id=?");
			whereClauseComponents.add(format("AND tc.tag_id IN %s", sqlInListPlaceholders(tagIds)));

			parameters.add(institutionId);
			parameters.addAll(tagIds);
		}

		// TODO: search over tag names (?)
		if (searchQuery != null) {
			whereClauseComponents.add("AND ((c.en_search_vector @@ websearch_to_tsquery('english', ?)) OR (c.title ILIKE CONCAT('%',?,'%') OR c.description ILIKE CONCAT('%',?,'%')))");
			parameters.add(searchQuery);
			parameters.add(searchQuery);
			parameters.add(searchQuery);
		}

		if (contentTypeIds.size() > 0) {
			whereClauseComponents.add(format("AND c.content_type_id IN %s", sqlInListPlaceholders(contentTypeIds)));
			parameters.addAll(contentTypeIds);
		}

		if (contentDurationIds.size() > 0) {
			List<String> durationClauses = new ArrayList<>(contentDurationIds.size());

			if (contentDurationIds.contains(ContentDurationId.UNDER_FIVE_MINUTES))
				durationClauses.add("(c.duration_in_minutes < 5)");
			if (contentDurationIds.contains(ContentDurationId.BETWEEN_FIVE_AND_TEN_MINUTES))
				durationClauses.add("(c.duration_in_minutes >= 5 AND c.duration_in_minutes <= 10)");
			if (contentDurationIds.contains(ContentDurationId.BETWEEN_TEN_AND_THIRTY_MINUTES))
				durationClauses.add("(c.duration_in_minutes >= 10 AND c.duration_in_minutes <= 30)");
			if (contentDurationIds.contains(ContentDurationId.OVER_THIRTY_MINUTES))
				durationClauses.add("(c.duration_in_minutes >= 31)");

			whereClauseComponents.add(format("AND (%s)", durationClauses.stream().collect(Collectors.joining(" OR "))));
		}

		parameters.add(institutionId);

		if (prioritizeUnviewedForAccountId != null) {
			contentViewedQuery = """
					                         , content_viewed_query AS (
					                         SELECT CAST (context ->> 'contentId' AS UUID) AS content_id, MAX(created) AS last_viewed_at
					                         FROM activity_tracking
					                         WHERE activity_action_id=? 
					                         AND activity_type_id=?
					                         AND account_id=?
					                         GROUP BY content_id
					                       )
					""";

			parameters.add(ActivityActionId.VIEW);
			parameters.add(ActivityTypeId.CONTENT);
			parameters.add(prioritizeUnviewedForAccountId);

			contentViewedJoin = "LEFT OUTER JOIN content_viewed_query as cvq ON bq.content_id=cvq.content_id";
			contentViewedSelect = ", cvq.last_viewed_at";
			contentViewedOrderBy = "cvq.last_viewed_at ASC NULLS FIRST,";
		}

		parameters.add(limit);
		parameters.add(offset);

		String sql = """
				WITH base_query AS (
				    SELECT DISTINCT
				        c.*,
				        ct.call_to_action,
				        ctl.description AS content_type_label,
				        ct.description AS content_type_description
				    FROM
				        content c,
				        content_type ct,
				        content_type_label ctl,
				        institution_content ic
				        {{fromClause}}
				    WHERE 1=1
				        {{whereClause}}
				        AND c.content_type_id = ct.content_type_id
				        AND c.content_type_label_id = ctl.content_type_label_id
				        AND ic.content_id = c.content_id
				        AND ic.institution_id = ?				   
				        AND c.deleted_flag = FALSE
				        AND c.content_status_id = 'LIVE'				        
				),
				total_count_query AS (
				    SELECT
				        COUNT(DISTINCT bq.content_id) AS total_count
				    FROM
				        base_query bq
				)
				{{contentViewedQuery}}
				SELECT
				    bq.*,
				    tcq.total_count
				    {{contentViewedSelect}}
				FROM				    
				    total_count_query tcq,
				    base_query bq
				    {{contentViewedJoin}}
				ORDER BY
						{{contentViewedOrderBy}}
				    bq.last_updated DESC
				LIMIT ?
				OFFSET ?
								"""
				.replace("{{fromClause}}", fromClauseComponents.size() == 0 ? "" : ",\n" + fromClauseComponents.stream().collect(Collectors.joining(",\n")))
				.replace("{{whereClause}}", whereClauseComponents.size() == 0 ? "" : "\n" + whereClauseComponents.stream().collect(Collectors.joining("\n")))
				.replace("{{contentViewedQuery}}", contentViewedQuery == null ? "" : contentViewedQuery)
				.replace("{{contentViewedSelect}}", contentViewedSelect == null ? "" : contentViewedSelect)
				.replace("{{contentViewedJoin}}", contentViewedJoin == null ? "" : contentViewedJoin)
				.replace("{{contentViewedOrderBy}}", contentViewedOrderBy == null ? "" : contentViewedOrderBy);

		List<ContentWithTotalCount> contents = getDatabase().queryForList(sql, ContentWithTotalCount.class, sqlVaragsParameters(parameters));

		applyTagsToContents(contents, institutionId);

		Integer totalCount = contents.stream()
				.filter(content -> content.getTotalCount() != null)
				.mapToInt(ContentWithTotalCount::getTotalCount)
				.findFirst()
				.orElse(0);

		FindResult<? extends Content> findResult = new FindResult<>(contents, totalCount);
		return (FindResult<Content>) findResult;
	}

	@NotThreadSafe
	protected static class ContentWithTotalCount extends Content {
		@Nullable
		private Integer totalCount;
		@Nullable
		private Instant lastViewedAt;

		@Nullable
		public Integer getTotalCount() {
			return this.totalCount;
		}

		public void setTotalCount(@Nullable Integer totalCount) {
			this.totalCount = totalCount;
		}

		@Nullable
		public Instant getLastViewedAt() {
			return this.lastViewedAt;
		}

		public void setLastViewedAt(@Nullable Instant lastViewedAt) {
			this.lastViewedAt = lastViewedAt;
		}
	}

	@Nonnull
	public Optional<Content> findContentById(@Nonnull UUID contentId) {
		return getDatabase().queryForObject("SELECT * FROM v_admin_content WHERE content_id = ?", Content.class, contentId);
	}

	@Nonnull
	public List<ContentType> findContentTypes() {
		return getDatabase().queryForList("SELECT * FROM content_type WHERE deleted=FALSE ORDER BY description", ContentType.class);
	}

	@Nonnull
	public List<UUID> findTagsForContent(@Nonnull UUID contentId) {
		return getDatabase().queryForList("SELECT answer_id FROM answer_content WHERE content_id = ?",
				UUID.class, contentId);
	}

	private void tagContent(@Nonnull UUID contentId,
													@Nonnull Map<UUID, List<SubmissionAnswer>> contentTags,
													@Nonnull Boolean addToInstitution) {
		if (!addToInstitution)
			getDatabase().execute("DELETE FROM answer_content WHERE content_id = ?", contentId);

		contentTags.values().stream().flatMap(it -> it.stream().map(SubmissionAnswer::getAnswerId)).forEach(answerId ->
				getDatabase().execute("INSERT INTO answer_content (answer_content_id, answer_id, content_id) VALUES (?, ?,?)",
						UUID.randomUUID(), answerId, contentId)
		);
	}

	@Nonnull
	public List<Content> findAdditionalContentForAccount(@Nonnull Account account,
																											 @Nonnull List<Content> filteredContent,
																											 @Nullable String format,
																											 @Nullable Integer maxLengthMinutes,
																											 @Nullable String searchQuery) {
		requireNonNull(account);
		requireNonNull(filteredContent);

		format = trimToNull(format);
		searchQuery = trimToNull(searchQuery);

		List<Object> unfilteredParameters = new ArrayList();
		StringBuilder unfilteredQuery = new StringBuilder("SELECT DISTINCT ON (c.content_id, c.created, new_flag) c.* , " +
				"CASE WHEN (activity_tracking_id IS NULL) AND (c.created >= now() - INTERVAL '1 WEEK') THEN true " +
				"ELSE false END as new_flag " +
				"FROM v_admin_content c LEFT OUTER JOIN activity_tracking act ON c.content_id = CAST (act.context ->> 'contentId' AS UUID) " +
				"AND act.account_id = ? WHERE c.institution_id = ? AND c.approved_flag=TRUE AND c.archived_flag=FALSE ");
		final String ORDER_BY = "ORDER BY new_flag DESC, c.created DESC";
		unfilteredParameters.add(account.getAccountId());
		unfilteredParameters.add(account.getInstitutionId());

		if (format != null) {
			String formatList = Arrays.asList(format.split(","))
					.stream().map(c -> String.format("'%s'", c))
					.collect(Collectors.joining(","));
			unfilteredQuery.append(String.format("AND c.content_type_label_id IN (%s) ", formatList));
		}

		if (maxLengthMinutes != null) {
			unfilteredQuery.append("AND duration_in_minutes <= ? ");
			unfilteredParameters.add(maxLengthMinutes);
		}

		String inList = filteredContent.stream().map(c -> String.format("'%s'", c.getContentId().toString()))
				.collect(Collectors.joining(","));

		if (trimToNull(inList) != null)
			unfilteredQuery.append(String.format(" AND c.content_id NOT IN (%s) ", inList));

		if (searchQuery != null) {
			unfilteredQuery.append("AND ((c.en_search_vector @@ websearch_to_tsquery('english', ?)) OR (c.title ILIKE CONCAT('%',?,'%') OR c.description ILIKE CONCAT('%',?,'%'))) ");
			unfilteredParameters.add(searchQuery);
			unfilteredParameters.add(searchQuery);
			unfilteredParameters.add(searchQuery);
		}

		unfilteredQuery.append(ORDER_BY);

		List<Content> unfilteredContent = getDatabase().queryForList(unfilteredQuery.toString(),
				Content.class, unfilteredParameters.toArray());

		applyTagsToContents(unfilteredContent, account.getInstitutionId());

		return unfilteredContent;
	}

	@Nonnull
	public List<ContentTypeLabel> findContentTypeLabelsForAccount(Account account) {
		//TODO: Limit this to just approved content and cache
		return getDatabase().queryForList("SELECT DISTINCT ctl.content_type_label_id, ctl.description FROM content c, institution_content i, content_type_label ctl " +
				"WHERE i.content_id = c.content_id AND i.institution_id = ? AND c.content_type_label_id = ctl.content_type_label_id ORDER BY ctl.description ", ContentTypeLabel.class, account.getInstitutionId());
	}

	@Nonnull
	public List<Content> findContentForAccount(@Nonnull Account account) {
		requireNonNull(account);
		return findContentForAccount(account, null, null, null);
	}

	@Nonnull
	public List<Content> findContentForAccount(@Nonnull Account account,
																						 @Nullable String format,
																						 @Nullable Integer maxLengthMinutes,
																						 @Nullable String searchQuery) {
		requireNonNull(account);

		format = trimToNull(format);
		searchQuery = trimToNull(searchQuery);

		List<Content> content = new ArrayList<>();
		Optional<AccountSession> accountSession = getSessionService().getCurrentIntroSessionForAccount(
				currentContextProvider.get().getAccount().get());
		List<Object> parameters = new ArrayList();
		Boolean introAssessmentComplete = accountSession.isPresent() &&
				getSessionService().doesAccountSessionHaveAnswers(accountSession.get().getAccountSessionId());

		if (!introAssessmentComplete)
			return content;

		StringBuilder query = new StringBuilder("SELECT c.content_id,c.content_type_id,c.title,c.url,c.date_created,c.image_url, " +
				"c.description,c.author,c.created,c.last_updated,c.owner_institution_id, " +
				"c.content_type_label,c.content_type_description,c.call_to_action,c.institution_id, c.duration_in_minutes,  " +
				"CASE WHEN (activity_tracking_id IS NULL) AND (c.created >= now() - INTERVAL '1 WEEK') THEN true  " +
				"ELSE false END as new_flag, count(*) as match_count " +
				"FROM answer_content ac, v_account_session_answer a1, v_admin_content c  " +
				"LEFT OUTER JOIN activity_tracking act ON c.content_id = CAST (act.context ->> 'contentId' AS UUID) AND act.account_id = ? " +
				"WHERE ac.content_id = c.content_id  " +
				"AND ac.answer_id = a1.answer_id " +
				"and a1.account_session_id= ? " +
				"AND c.institution_id = ? AND c.approved_flag = TRUE AND c.archived_flag=FALSE AND c.owner_institution_approval_status_id='APPROVED' ");
		final String GROUP_BY = "group by c.content_id,c.content_type_id,c.title,c.url,c.date_created,c.image_url, " +
				"c.description,c.author,c.created,c.last_updated,c.owner_institution_id, " +
				"c.content_type_label,c.content_type_description,c.call_to_action,c.institution_id, c.duration_in_minutes , new_flag  ";
		final String ORDER_BY = "ORDER BY new_flag DESC, match_count DESC, c.created DESC";
		parameters.add(account.getAccountId());
		parameters.add(accountSession.get().getAccountSessionId());
		parameters.add(account.getInstitutionId());

		if (searchQuery != null) {
			query.append("AND ((c.en_search_vector @@ websearch_to_tsquery('english', ?)) OR (c.title ILIKE CONCAT('%',?,'%') OR c.description ILIKE CONCAT('%',?,'%'))) ");
			parameters.add(searchQuery);
			parameters.add(searchQuery);
			parameters.add(searchQuery);
		}

		if (format != null) {
			String formatList = Arrays.asList(format.split(","))
					.stream().map(c -> String.format("'%s'", c))
					.collect(Collectors.joining(","));
			query.append(String.format("AND c.content_type_label_id IN (%s) ", formatList));
		}

		if (maxLengthMinutes != null) {
			query.append("AND duration_in_minutes <= ? ");
			parameters.add(maxLengthMinutes);
		}

		query.append(GROUP_BY);
		query.append(ORDER_BY);

		content = getDatabase().queryForList(query.toString(), Content.class, parameters.toArray());

		applyTagsToContents(content, account.getInstitutionId());

		return content;
	}

	@Nonnull
	public List<Content> findVisibleContentByAccountId(@Nullable UUID accountId) {
		if (accountId == null)
			return List.of();

		Account account = getAccountService().findAccountById(accountId).orElse(null);

		if (account == null)
			return List.of();

		return findVisibleContentByAccount(account);
	}

	@Nonnull
	public List<Content> findVisibleContentByAccount(@Nullable Account account) {
		if (account == null)
			return List.of();

		List<Content> contents = getDatabase().queryForList("""
				WITH content_viewed_query as (
				       SELECT CAST (context ->> 'contentId' AS UUID) AS content_id, MAX(created) AS last_viewed_at
				       FROM activity_tracking
				       WHERE activity_action_id=?
				       AND activity_type_id=?
				       AND account_id=?
				       GROUP BY content_id
				     )
				SELECT cvq.last_viewed_at, c.*    
				FROM institution_content ic, v_admin_content c
				LEFT OUTER JOIN content_viewed_query as cvq ON c.content_id=cvq.content_id
				WHERE c.content_id=ic.content_id
				AND ic.institution_id=?
				AND c.content_status_id = ?
				ORDER BY cvq.last_viewed_at ASC NULLS FIRST, c.created DESC
								""", Content.class, ActivityActionId.VIEW, ActivityTypeId.CONTENT, account.getAccountId(),
				account.getInstitutionId(), ContentStatusId.LIVE);

		applyTagsToContents(contents, account.getInstitutionId());

		return contents;
	}

	/**
	 * Note: modifies {@code contents} parameter in-place.
	 */
	@Nonnull
	protected void applyTagsToContents(@Nonnull List<? extends Content> contents,
																		 @Nonnull InstitutionId institutionId) {
		requireNonNull(contents);
		requireNonNull(institutionId);

		// Pull back all data up-front to avoid N+1 selects
		Map<UUID, List<TagContent>> tagContentsByContentId = getTagService().findTagContentsByInstitutionId(institutionId).stream()
				.collect(Collectors.groupingBy(TagContent::getContentId));
		Map<String, Tag> tagsByTagId = getTagService().findTagsByInstitutionId(institutionId).stream()
				.collect(Collectors.toMap(Tag::getTagId, Function.identity()));

		for (Content content : contents) {
			List<Tag> tags = Collections.emptyList();
			List<TagContent> tagContents = tagContentsByContentId.get(content.getContentId());

			if (tagContents != null)
				tags = tagContents.stream()
						.map(tagContent -> tagsByTagId.get(tagContent.getTagId()))
						.collect(Collectors.toList());

			content.setTags(tags);
		}
	}

	/**
	 * Note: modifies {@code content} parameter in-place.
	 */
	@Nonnull
	protected <T extends Content> void applyTagsToContents(@Nonnull T content,
																												 @Nonnull InstitutionId institutionId) {
		requireNonNull(content);
		requireNonNull(institutionId);

		content.setTags(getTagService().findTagsByContentIdAndInstitutionId(content.getContentId(), institutionId));
	}

	/**
	 * Note: modifies {@code contents} parameter in-place.
	 */
	@Nonnull
	protected void applyTagsToAdminContents(@Nonnull List<? extends AdminContent> adminContents,
																					@Nonnull InstitutionId institutionId) {
		requireNonNull(adminContents);
		requireNonNull(institutionId);

		// Pull back all data up-front to avoid N+1 selects
		Map<UUID, List<TagContent>> tagContentsByContentId = getTagService().findTagContentsByInstitutionId(institutionId).stream()
				.collect(Collectors.groupingBy(TagContent::getContentId));
		Map<String, Tag> tagsByTagId = getTagService().findTagsByInstitutionId(institutionId).stream()
				.collect(Collectors.toMap(Tag::getTagId, Function.identity()));

		for (AdminContent adminContent : adminContents) {
			List<Tag> tags = Collections.emptyList();
			List<TagContent> tagContents = tagContentsByContentId.get(adminContent.getContentId());

			if (tagContents != null)
				tags = tagContents.stream()
						.map(tagContent -> tagsByTagId.get(tagContent.getTagId()))
						.collect(Collectors.toList());

			adminContent.setTags(tags);
		}
	}

	@Nonnull
	private List<InstitutionContent> findNonInstitutionContent() {

		return getDatabase().queryForList("""
				SELECT ic.*, i.name as institution_name
				FROM institution_content ic, institution i, v_admin_content va
				WHERE ic.institution_id = i.institution_id	
				AND ic.content_id = va.content_id
				AND va.content_status_id = ?
				ORDER BY ic.content_id, i.name			
				""", InstitutionContent.class, ContentStatusId.LIVE);
	}

	@Nonnull
	protected void applyInstitutionsToAdminContents(@Nonnull List<? extends AdminContent> adminContents,
																									@Nonnull InstitutionId institutionId) {
		requireNonNull(adminContents);
		requireNonNull(institutionId);

		// Pull back all data up-front to avoid N+1 selects
		Map<UUID, List<InstitutionContent>> institutionContentsByContentId = findNonInstitutionContent().stream()
				.collect(Collectors.groupingBy(InstitutionContent::getContentId));

		for (AdminContent adminContent : adminContents) {
			List<InstitutionContent> institutionContents = institutionContentsByContentId.get(adminContent.getContentId());
			String institutions = null;
			Integer inUseCount = 0;

			if (institutionContents != null) {
				institutions = institutionContents.stream().map(i -> i.getInstitutionName()).collect(Collectors.joining(", "));
				inUseCount = institutionContents.size();
			}

			adminContent.setInUseInstitutionDescription(institutions);
			adminContent.setInUseCount(inUseCount);
		}
	}


	@Nonnull
	protected SessionService getSessionService() {
		return sessionService;
	}

	@Nonnull
	protected Database getDatabase() {
		return database;
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return institutionService;
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected AssessmentService getAssessmentService() {
		return assessmentServiceProvider.get();
	}

	@Nonnull
	protected TagService getTagService() {
		return this.tagServiceProvider.get();
	}

	@Nonnull
	protected MessageService getEmailMessageManager() {
		return messageServiceProvider.get();
	}

	@Nonnull
	protected AccountService getAccountService() {
		return accountServiceProvider.get();
	}

	@Nonnull
	protected Formatter getFormatter() {
		return formatterProvider.get();
	}

	@Nonnull
	protected LinkGenerator getLinkGenerator() {
		return linkGeneratorProvider.get();
	}
}
