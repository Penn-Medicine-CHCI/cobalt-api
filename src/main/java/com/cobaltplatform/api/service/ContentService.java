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
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.error.ErrorReporter;
import com.cobaltplatform.api.model.api.request.CreateContentFeedbackRequest;
import com.cobaltplatform.api.model.api.request.FindResourceLibraryContentRequest;
import com.cobaltplatform.api.model.api.request.UpdateContentFeedbackRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.ActivityAction.ActivityActionId;
import com.cobaltplatform.api.model.db.ActivityType.ActivityTypeId;
import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.db.ContentAudienceType;
import com.cobaltplatform.api.model.db.ContentAudienceType.ContentAudienceTypeId;
import com.cobaltplatform.api.model.db.ContentAudienceTypeGroup;
import com.cobaltplatform.api.model.db.ContentFeedback;
import com.cobaltplatform.api.model.db.ContentFeedbackType.ContentFeedbackTypeId;
import com.cobaltplatform.api.model.db.ContentStatus.ContentStatusId;
import com.cobaltplatform.api.model.db.ContentType;
import com.cobaltplatform.api.model.db.ContentType.ContentTypeId;
import com.cobaltplatform.api.model.db.ContentVisibilityType.ContentVisibilityTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionContent;
import com.cobaltplatform.api.model.db.Tag;
import com.cobaltplatform.api.model.db.TagContent;
import com.cobaltplatform.api.model.service.AdminContent;
import com.cobaltplatform.api.model.service.ContentDurationId;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.model.service.ResourceLibrarySortColumnId;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.LinkGenerator;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
public class ContentService implements AutoCloseable {
	@Nonnull
	private static final int DEFAULT_PAGE_SIZE = 15;
	private static final int MAXIMUM_PAGE_SIZE = 100;

	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final SessionService sessionService;
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
	@Nonnull
	private final Provider<BackgroundSyncTask> backgroundSyncTaskProvider;
	@Nonnull
	private final Object backgroundTaskLock;
	@Nonnull
	private Boolean backgroundTaskStarted;
	@Nullable
	private ScheduledExecutorService backgroundTaskExecutorService;

	@Nonnull
	private static final Long BACKGROUND_TASK_INTERVAL_IN_SECONDS;
	@Nonnull
	private static final Long BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS;

	@Nonnull
	private final Configuration configuration;

	static {
		BACKGROUND_TASK_INTERVAL_IN_SECONDS = 60L;
		BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS = 10L;
	}

	@Inject
	public ContentService(@Nonnull Provider<CurrentContext> currentContextProvider,
												@Nonnull Provider<AssessmentService> assessmentServiceProvider,
												@Nonnull Provider<TagService> tagServiceProvider,
												@Nonnull Provider<MessageService> messageServiceProvider,
												@Nonnull Provider<AccountService> accountServiceProvider,
												@Nonnull Provider<Formatter> formatterProvider,
												@Nonnull Provider<LinkGenerator> linkGeneratorProvider,
												@Nonnull DatabaseProvider databaseProvider,
												@Nonnull SessionService sessionService,
												@Nonnull InstitutionService institutionService,
												@Nonnull Strings strings,
												@Nonnull Provider<BackgroundSyncTask> backgroundSyncTaskProvider,
												@Nonnull Configuration configuration) {
		requireNonNull(currentContextProvider);
		requireNonNull(assessmentServiceProvider);
		requireNonNull(tagServiceProvider);
		requireNonNull(messageServiceProvider);
		requireNonNull(accountServiceProvider);
		requireNonNull(formatterProvider);
		requireNonNull(linkGeneratorProvider);
		requireNonNull(databaseProvider);
		requireNonNull(sessionService);
		requireNonNull(institutionService);
		requireNonNull(strings);
		requireNonNull(backgroundSyncTaskProvider);
		requireNonNull(configuration);

		this.logger = LoggerFactory.getLogger(getClass());
		this.databaseProvider = databaseProvider;
		this.sessionService = sessionService;
		this.tagServiceProvider = tagServiceProvider;
		this.institutionService = institutionService;
		this.strings = strings;
		this.assessmentServiceProvider = assessmentServiceProvider;
		this.messageServiceProvider = messageServiceProvider;
		this.accountServiceProvider = accountServiceProvider;
		this.formatterProvider = formatterProvider;
		this.linkGeneratorProvider = linkGeneratorProvider;
		this.backgroundSyncTaskProvider = backgroundSyncTaskProvider;
		this.backgroundTaskLock = new Object();
		this.backgroundTaskStarted = false;
		this.configuration = configuration;
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
		Set<ContentAudienceTypeId> contentAudienceTypeIds = request.getContentAudienceTypeIds() == null ? Set.of() : request.getContentAudienceTypeIds();
		ResourceLibrarySortColumnId resourceLibrarySortColumnId = request.getResourceLibrarySortColumnId() == null ? ResourceLibrarySortColumnId.MOST_RECENT : ResourceLibrarySortColumnId.MOST_VIEWED;
		ContentVisibilityTypeId contentVisibilityTypeId = request.getContentVisibilityTypeId();
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
		String orderBy = null;
		List<Object> parameters = new ArrayList<>();

		if (tagGroupId != null) {
			fromClauseComponents.add("tag_content tc");
			fromClauseComponents.add("tag t");
			fromClauseComponents.add("institution_content ic");

			whereClauseComponents.add("AND tc.content_id=c.content_id");
			whereClauseComponents.add("AND tc.content_id=ic.content_id");
			whereClauseComponents.add("AND ic.institution_id=?");
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
			fromClauseComponents.add("institution_content ic");

			whereClauseComponents.add("AND tc.content_id=c.content_id");
			whereClauseComponents.add("AND tc.content_id=ic.content_id");
			whereClauseComponents.add("AND ic.institution_id=?");
			whereClauseComponents.add(format("AND tc.tag_id IN %s", sqlInListPlaceholders(tagIds)));

			parameters.add(institutionId);
			parameters.addAll(tagIds);
		}

		if (contentAudienceTypeIds.size() > 0) {
			fromClauseComponents.add("content_audience ca");

			whereClauseComponents.add("AND ca.content_id=c.content_id");
			whereClauseComponents.add(format("AND ca.content_audience_type_id IN %s", sqlInListPlaceholders(contentAudienceTypeIds)));
			parameters.addAll(contentAudienceTypeIds);
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

		if (contentVisibilityTypeId != null) {
			whereClauseComponents.add("AND c.content_visibility_type_id=?");
			parameters.add(contentVisibilityTypeId);
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

		if (resourceLibrarySortColumnId == ResourceLibrarySortColumnId.MOST_VIEWED) {
			// TODO: implement sorting for "most viewed" (currently no UI for this)
			orderBy = "bq.institution_created_date DESC";
		} else {
			// ResourceLibrarySortColumnId.MOST_RECENT or anything else we don't recognize
			orderBy = "bq.institution_created_date DESC";
		}

		parameters.add(limit);
		parameters.add(offset);

		String sql = """
				WITH base_query AS (
				    SELECT DISTINCT
				        c.*
				    FROM
				        v_institution_content c
				        {{fromClause}}
				    WHERE 1=1
				        {{whereClause}}
				        AND c.institution_id = ?
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
				    {{orderBy}}
				LIMIT ?
				OFFSET ?
				"""
				.replace("{{fromClause}}", fromClauseComponents.size() == 0 ? "" : ",\n" + fromClauseComponents.stream().collect(Collectors.joining(",\n")))
				.replace("{{whereClause}}", whereClauseComponents.size() == 0 ? "" : "\n" + whereClauseComponents.stream().collect(Collectors.joining("\n")))
				.replace("{{contentViewedQuery}}", contentViewedQuery == null ? "" : contentViewedQuery)
				.replace("{{contentViewedSelect}}", contentViewedSelect == null ? "" : contentViewedSelect)
				.replace("{{contentViewedJoin}}", contentViewedJoin == null ? "" : contentViewedJoin)
				.replace("{{contentViewedOrderBy}}", contentViewedOrderBy == null ? "" : contentViewedOrderBy)
				.replace("{{orderBy}}", orderBy == null ? "" : orderBy);

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
						AND c.content_visibility_type_id=?
						ORDER BY cvq.last_viewed_at ASC NULLS FIRST, ic.created DESC
						""", Content.class, ActivityActionId.VIEW, ActivityTypeId.CONTENT, account.getAccountId(),
				account.getInstitutionId(), ContentStatusId.LIVE, ContentVisibilityTypeId.PUBLIC);

		applyTagsToContents(contents, account.getInstitutionId());

		return contents;
	}

	@Nonnull
	public Optional<ContentFeedback> findContentFeedbackById(@Nullable UUID contentFeedbackId) {
		if (contentFeedbackId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM content_feedback
				WHERE content_feedback_id=?
				""", ContentFeedback.class, contentFeedbackId);
	}

	@Nonnull
	public UUID createContentFeedback(@Nonnull CreateContentFeedbackRequest request) {
		requireNonNull(request);

		ContentFeedbackTypeId contentFeedbackTypeId = request.getContentFeedbackTypeId();
		UUID contentId = request.getContentId();
		UUID accountId = request.getAccountId();
		String message = trimToNull(request.getMessage());
		UUID contentFeedbackId = UUID.randomUUID();
		ValidationException validationException = new ValidationException();

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (contentId == null)
			validationException.add(new FieldError("contentId", getStrings().get("Content ID is required.")));

		if (contentFeedbackTypeId == null)
			validationException.add(new FieldError("contentFeedbackTypeId", getStrings().get("Content Feedback Type ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
				INSERT INTO content_feedback (
				    content_feedback_id,
				    content_feedback_type_id,
				    content_id,
				    account_id,
				    message
				) VALUES (?,?,?,?,?)
				""", contentFeedbackId, contentFeedbackTypeId, contentId, accountId, message);

		return contentFeedbackId;
	}

	@Nonnull
	public Boolean updateContentFeedback(@Nonnull UpdateContentFeedbackRequest request) {
		requireNonNull(request);

		UUID contentFeedbackId = request.getContentFeedbackId();
		ContentFeedbackTypeId contentFeedbackTypeId = request.getContentFeedbackTypeId();
		String message = trimToNull(request.getMessage());
		ValidationException validationException = new ValidationException();

		if (contentFeedbackId == null)
			validationException.add(new FieldError("contentFeedbackId", getStrings().get("Content Feedback ID is required.")));

		if (contentFeedbackTypeId == null)
			validationException.add(new FieldError("contentFeedbackTypeId", getStrings().get("Content Feedback Type ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		return getDatabase().execute("""
				UPDATE content_feedback
				SET content_feedback_type_id=?, message=?
				WHERE content_feedback_id=?
				""", contentFeedbackTypeId, message, contentFeedbackId) > 0;
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
				AND i.sharing_content = true
				ORDER BY ic.content_id, i.name			
				""", InstitutionContent.class, ContentStatusId.LIVE);
	}

	@Nonnull
	protected void applyInstitutionsToAdminContent(@Nonnull AdminContent adminContent,
																								 @Nonnull InstitutionId institutionId) {

		List<AdminContent> adminContents = new ArrayList<>();
		adminContents.add(adminContent);
		applyInstitutionsToAdminContents(adminContents, institutionId);
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
	public List<ContentAudienceTypeGroup> findContentAudienceTypeGroups() {
		return getDatabase().queryForList("""
				SELECT *
				FROM content_audience_type_group
				ORDER BY display_order
				""", ContentAudienceTypeGroup.class);
	}

	@Nonnull
	public List<ContentAudienceType> findContentAudienceTypes() {
		return getDatabase().queryForList("""
				SELECT *
				FROM content_audience_type
				ORDER BY display_order
				""", ContentAudienceType.class);
	}

	@Nonnull
	public List<ContentAudienceType> findContentAudienceTypesByContentId(@Nullable UUID contentId) {
		if (contentId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT cat.*
				FROM content_audience ca, content_audience_type cat
				WHERE ca.content_id=?
				AND ca.content_audience_type_id=cat.content_audience_type_id
				ORDER BY cat.content_audience_type_id
				""", ContentAudienceType.class, contentId);
	}

	@Override
	public void close() throws Exception {
		stopBackgroundTask();
	}

	@Nonnull
	public Boolean startBackgroundTask() {
		synchronized (getBackgroundTaskLock()) {
			if (isBackgroundTaskStarted())
				return false;

			getLogger().trace("Starting Content Service background task...");

			this.backgroundTaskExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("content-service-background-task").build());
			this.backgroundTaskStarted = true;
			getBackgroundTaskExecutorService().get().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						getBackgroundSyncTaskProvider().get().run();
					} catch (Exception e) {
						getLogger().warn(format("Unable to complete Content Service background task - will retry in %s seconds", String.valueOf(getBackgroundTaskIntervalInSeconds())), e);
					}
				}
			}, getBackgroundTaskInitialDelayInSeconds(), getBackgroundTaskIntervalInSeconds(), TimeUnit.SECONDS);

			getLogger().trace("Content Service background task started.");

			return true;
		}
	}

	@Nonnull
	public Boolean stopBackgroundTask() {
		synchronized (getBackgroundTaskLock()) {
			if (!isBackgroundTaskStarted())
				return false;

			getLogger().trace("Stopping Study Service background task...");

			getBackgroundTaskExecutorService().get().shutdownNow();
			this.backgroundTaskExecutorService = null;
			this.backgroundTaskStarted = false;

			getLogger().trace("Study Service background task stopped.");

			return true;
		}
	}

	@ThreadSafe
	protected static class BackgroundSyncTask implements Runnable {
		@Nonnull
		private final CurrentContextExecutor currentContextExecutor;
		@Nonnull
		private final ErrorReporter errorReporter;
		@Nonnull
		private final DatabaseProvider databaseProvider;
		@Nonnull
		private final Configuration configuration;
		@Nonnull
		private final Logger logger;

		@Inject
		public BackgroundSyncTask(@Nonnull CurrentContextExecutor currentContextExecutor,
															@Nonnull ErrorReporter errorReporter,
															@Nonnull DatabaseProvider databaseProvider,
															@Nonnull Configuration configuration) {
			requireNonNull(currentContextExecutor);
			requireNonNull(errorReporter);
			requireNonNull(databaseProvider);
			requireNonNull(configuration);

			this.currentContextExecutor = currentContextExecutor;
			this.errorReporter = errorReporter;
			this.databaseProvider = databaseProvider;
			this.configuration = configuration;
			this.logger = LoggerFactory.getLogger(getClass());
		}

		@Override
		public void run() {
			CurrentContext currentContext = new CurrentContext.Builder(InstitutionId.COBALT,
					getConfiguration().getDefaultLocale(), getConfiguration().getDefaultTimeZone()).build();

			getCurrentContextExecutor().execute(currentContext, () -> {
				try {
					rescheduleContentActivationExpiration();
				} catch (Exception e) {
					getLogger().error("Unable to reschedule content activation/expiration", e);
					getErrorReporter().report(e);
				}
			});
		}

		protected void rescheduleContentActivationExpiration() {
			List<Content> expiredRecurringContent = getDatabase().queryForList("""
					SELECT * 
					FROM v_admin_content vc
					WHERE content_status_id = ? 
					AND publish_recurring = TRUE """, Content.class, ContentStatusId.EXPIRED);

			for (Content content : expiredRecurringContent) {
				LocalDate publishStartDate = content.getPublishStartDate();
				LocalDate publishEndDate = content.getPublishEndDate();
				LocalDate now = LocalDate.now();

				if (publishStartDate.getYear() == publishEndDate.getYear())
					publishStartDate = publishStartDate.withYear(now.getYear());
				else
					publishStartDate = publishStartDate.withYear(now.getYear() - 1);

				publishEndDate = publishEndDate.withYear(now.getYear());

				if (now.isAfter(publishStartDate) && now.isBefore(publishEndDate)) {
					getLogger().info("Content ID {} is being activated", content.getContentId());
					getDatabase().execute("""
							UPDATE content 
							SET publish_start_date = ?, publish_end_date = ? 
							WHERE content_id = ?
							""", publishStartDate, publishEndDate, content.getContentId());
				}

			}

		}

		@Nonnull
		protected CurrentContextExecutor getCurrentContextExecutor() {
			return this.currentContextExecutor;
		}

		@Nonnull
		protected ErrorReporter getErrorReporter() {
			return this.errorReporter;
		}

		@Nonnull
		protected Database getDatabase() {
			return this.databaseProvider.get();
		}

		@Nonnull
		protected Configuration getConfiguration() {
			return this.configuration;
		}

		@Nonnull
		protected Logger getLogger() {
			return this.logger;
		}
	}

	@Nonnull
	public Boolean isBackgroundTaskStarted() {
		synchronized (getBackgroundTaskLock()) {
			return this.backgroundTaskStarted;
		}
	}

	@Nonnull
	protected SessionService getSessionService() {
		return sessionService;
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
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

	@Nonnull
	protected Object getBackgroundTaskLock() {
		return this.backgroundTaskLock;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}

	@Nonnull
	protected Long getBackgroundTaskIntervalInSeconds() {
		return BACKGROUND_TASK_INTERVAL_IN_SECONDS;
	}

	@Nonnull
	protected Long getBackgroundTaskInitialDelayInSeconds() {
		return BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS;
	}

	@Nonnull
	protected Provider<BackgroundSyncTask> getBackgroundSyncTaskProvider() {
		return this.backgroundSyncTaskProvider;
	}

	@Nonnull
	protected Optional<ScheduledExecutorService> getBackgroundTaskExecutorService() {
		return Optional.ofNullable(this.backgroundTaskExecutorService);
	}

	@Nonnull
	protected Configuration getConfiguration() {
		return this.configuration;
	}

}
