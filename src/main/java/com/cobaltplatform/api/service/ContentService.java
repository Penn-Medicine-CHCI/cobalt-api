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
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageManager;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.api.request.CreateContentRequest;
import com.cobaltplatform.api.model.api.request.FindResourceLibraryContentRequest;
import com.cobaltplatform.api.model.api.request.PersonalizeAssessmentChoicesCommand;
import com.cobaltplatform.api.model.api.request.PersonalizeAssessmentChoicesCommand.SubmissionAnswer;
import com.cobaltplatform.api.model.api.request.UpdateContentApprovalStatusRequest;
import com.cobaltplatform.api.model.api.request.UpdateContentArchivedStatus;
import com.cobaltplatform.api.model.api.request.UpdateContentRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AccountSession;
import com.cobaltplatform.api.model.db.AccountSource;
import com.cobaltplatform.api.model.db.ApprovalStatus;
import com.cobaltplatform.api.model.db.Assessment;
import com.cobaltplatform.api.model.db.AssessmentType.AssessmentTypeId;
import com.cobaltplatform.api.model.db.AvailableStatus;
import com.cobaltplatform.api.model.db.AvailableStatus.AvailableStatusId;
import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.db.ContentType;
import com.cobaltplatform.api.model.db.ContentType.ContentTypeId;
import com.cobaltplatform.api.model.db.ContentTypeLabel;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Tag;
import com.cobaltplatform.api.model.db.TagContent;
import com.cobaltplatform.api.model.db.Visibility;
import com.cobaltplatform.api.model.service.AdminContent;
import com.cobaltplatform.api.model.service.ContentDurationId;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.LinkGenerator;
import com.cobaltplatform.api.util.Normalizer;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.ValidationUtility;
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
import java.time.LocalDate;
import java.time.format.FormatStyle;
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

import static com.cobaltplatform.api.model.db.ApprovalStatus.ApprovalStatusId;
import static com.cobaltplatform.api.model.db.Role.RoleId;
import static com.cobaltplatform.api.model.db.Visibility.VisibilityId;
import static com.cobaltplatform.api.util.DatabaseUtility.sqlInListPlaceholders;
import static com.cobaltplatform.api.util.DatabaseUtility.sqlVaragsParameters;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
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
	private final Provider<EmailMessageManager> emailMessageManagerProvider;
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
												@Nonnull Provider<EmailMessageManager> emailMessageManagerProvider,
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
		requireNonNull(emailMessageManagerProvider);
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
		this.emailMessageManagerProvider = emailMessageManagerProvider;
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
				"END as new_flag FROM v_admin_content vc " +
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
	public FindResult<AdminContent> findAllContentForAccount(@Nonnull Boolean myContent,
																													 @Nonnull Account account,
																													 @Nonnull Optional<Integer> page,
																													 @Nonnull Optional<ContentTypeId> contentTypeId,
																													 @Nonnull Optional<InstitutionId> institutionId,
																													 @Nonnull Optional<VisibilityId> visibilityId,
																													 @Nonnull Optional<ApprovalStatusId> myApprovalStatusId,
																													 @Nonnull Optional<ApprovalStatusId> otherApprovalStatusId,
																													 @Nonnull Optional<AvailableStatusId> availableStatusId,
																													 @Nonnull Optional<String> search) {
		requireNonNull(myContent);
		requireNonNull(account);

		List<Object> parameters = new ArrayList();
		Integer pageNumber = page.orElse(0);
		Integer limit = DEFAULT_PAGE_SIZE;
		Integer offset = pageNumber * DEFAULT_PAGE_SIZE;
		StringBuilder whereClause = new StringBuilder(" 1=1 ");
		Boolean isSuperAdmin = account.getRoleId() == RoleId.SUPER_ADMINISTRATOR;

		if (contentTypeId.isPresent()) {
			whereClause.append("AND va.content_type_id = ? ");
			parameters.add(contentTypeId.get());
		}

		if (institutionId.isPresent()) {
			whereClause.append("AND va.owner_institution_id = ? ");
			parameters.add(institutionId.get());
		}

		if (myApprovalStatusId.isPresent()) {
			whereClause.append("AND va.owner_institution_approval_status_id = ? ");
			parameters.add(myApprovalStatusId.get());
		}

		if (otherApprovalStatusId.isPresent()) {
			whereClause.append("AND va.other_institution_approval_status_id = ? ");
			parameters.add(otherApprovalStatusId.get());
		}

		if (visibilityId.isPresent()) {
			whereClause.append("AND va.visibility_id = ? ");
			parameters.add(visibilityId.get());
		}

		if (search.isPresent()) {
			String lowerSearch = trimToEmpty(search.get().toLowerCase());
			whereClause.append("AND (LOWER(title) % ? or SIMILARITY(LOWER(title), ?) > 0.5 OR LOWER(title) LIKE ?) ");
			parameters.add(lowerSearch);
			parameters.add(lowerSearch);
			parameters.add('%' + lowerSearch + '%');
		}

		if (!isSuperAdmin) {
			if (myContent) {
				whereClause.append("AND va.owner_institution_id = ? AND institution_id = ? ");
				parameters.add(account.getInstitutionId());
				parameters.add(account.getInstitutionId());
				if (availableStatusId.isPresent()) {
					whereClause.append(" AND approved_flag = ? ");
					parameters.add(availableStatusId.get() == AvailableStatusId.ADDED);
				}
			} else {
				whereClause.append("AND va.owner_institution_id != ? ");
				parameters.add(account.getInstitutionId());
				whereClause.append("AND va.owner_institution_approval_status_id = ?");
				parameters.add(ApprovalStatusId.APPROVED);
				whereClause.append("AND va.institution_id = ? ");
				parameters.add(account.getInstitutionId());
				if (availableStatusId.isPresent()) {
					whereClause.append("AND va.approved_flag = ? ");
					parameters.add(availableStatusId.get() == AvailableStatusId.ADDED);
				}
			}
		}

		if (isSuperAdmin) {
			whereClause.append("AND va.deleted_flag = FALSE ");
			whereClause.append("AND va.institution_id = ? ");
			parameters.add(InstitutionId.COBALT);
		}

		String query =
				String.format("SELECT va.*, " +
						"(select COUNT(*) FROM " +
						"  activity_tracking a WHERE " +
						"  va.content_id = CAST (a.context ->> 'contentId' AS UUID) AND " +
						"  a.activity_action_id = 'VIEW' AND " +
						"  activity_type_id='CONTENT') AS views ," +
						"count(*) over() AS total_count " +
						"FROM v_admin_content va " +
						"WHERE %s " +
						"ORDER BY last_updated DESC LIMIT ? OFFSET ? ", whereClause.toString());

		logger.debug("query: " + query);
		parameters.add(limit);
		parameters.add(offset);
		List<AdminContent> content = getDatabase().queryForList(query, AdminContent.class, sqlVaragsParameters(parameters));
		Integer totalCount = content.stream().filter(it -> it.getTotalCount() != null).mapToInt(AdminContent::getTotalCount).findFirst().orElse(0);

		applyTagsToAdminContents(content, account.getInstitutionId());

		return new FindResult<>(content, totalCount);
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
				        AND ic.approved_flag = TRUE
				        AND c.deleted_flag = FALSE
				        AND c.archived_flag = FALSE				        
				),
				total_count_query AS (
				    SELECT
				        COUNT(DISTINCT bq.content_id) AS total_count
				    FROM
				        base_query bq
				)
				SELECT
				    bq.*,
				    tcq.total_count
				FROM
				    base_query bq,
				    total_count_query tcq
				ORDER BY
				    bq.last_updated DESC
				LIMIT ?
				OFFSET ?
								"""
				.replace("{{fromClause}}", fromClauseComponents.size() == 0 ? "" : ",\n" + fromClauseComponents.stream().collect(Collectors.joining(",\n")))
				.replace("{{whereClause}}", whereClauseComponents.size() == 0 ? "" : "\n" + whereClauseComponents.stream().collect(Collectors.joining("\n")));

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
		public Integer getTotalCount() {
			return this.totalCount;
		}

		public void setTotalCount(@Nullable Integer totalCount) {
			this.totalCount = totalCount;
		}
	}

	@Nonnull
	private Optional<Content> findContentById(@Nonnull UUID contentId) {
		return getDatabase().queryForObject("SELECT * FROM content WHERE content_id = ?", Content.class, contentId);
	}

	@Nonnull
	public Optional<AdminContent> findAdminContentByIdForInstitution(@Nonnull InstitutionId institutionId, @Nonnull UUID contentId) {
		requireNonNull(institutionId);
		requireNonNull(contentId);

		AdminContent adminContent = getDatabase().queryForObject("SELECT va.*, " +
						"(select COUNT(*) FROM " +
						" activity_tracking a WHERE " +
						" va.content_id = CAST (a.context ->> 'contentId' AS UUID) AND " +
						" a.activity_action_id = 'VIEW' AND " +
						" activity_type_id='CONTENT') AS views " +
						"FROM v_admin_content va " +
						"WHERE va.content_id = ? " +
						"AND va.institution_id = ? ",
				AdminContent.class, contentId, institutionId).orElse(null);

		if (adminContent != null)
			applyTagsToAdminContent(adminContent, institutionId);

		return Optional.ofNullable(adminContent);
	}

	@Nonnull
	private void addContentToInstitution(@Nonnull UUID contentId, @Nonnull InstitutionId institutionId,
																			 @Nonnull Boolean approvedFlag) {
		requireNonNull(contentId);
		requireNonNull(institutionId);
		requireNonNull(approvedFlag);

		getDatabase().execute("INSERT INTO institution_content (institution_content_id, institution_id, content_id, approved_flag) " +
				"VALUES (?,?,?,?)" +
				"ON CONFLICT (institution_id, content_id) DO " +
				"UPDATE SET approved_flag = ? ", UUID.randomUUID(), institutionId, contentId, approvedFlag, approvedFlag);
	}

	@Nonnull
	private void addContentToInstitutionsForPublic(@Nonnull UUID contentId) {
		requireNonNull(contentId);

		getDatabase().execute("INSERT INTO institution_content (institution_content_id, institution_id, content_id) " +
						"SELECT uuid_generate_v4(), i.institution_id, ? FROM institution i WHERE " +
						"i.institution_id NOT IN (SELECT ic.institution_id FROM institution_content ic WHERE ic.content_id=?) ",
				contentId, contentId);
	}

	@Nonnull
	private void removeContentFromInstitutionsForPublic(@Nonnull UUID contentId) {
		requireNonNull(contentId);

		getDatabase().execute("DELETE FROM institution_content WHERE content_id = ? AND institution_id != " +
				"(SELECT c.owner_institution_id FROM content c WHERE c.content_id = ?)", contentId, contentId);
	}

	@Nonnull
	public List<ContentType> findContentTypes() {
		return getDatabase().queryForList("SELECT * FROM content_type WHERE deleted=FALSE ORDER BY description", ContentType.class);
	}

	@Nonnull
	public List<Visibility> findVisibilities() {
		return getDatabase().queryForList("SELECT * FROM visibility ORDER BY description", Visibility.class);
	}

	@Nonnull
	public List<AvailableStatus> findAvailableStatuses() {
		return getDatabase().queryForList("SELECT * FROM available_status ORDER BY description", AvailableStatus.class);
	}

	@Nonnull
	public List<ApprovalStatus> findApprovalStatuses() {
		return getDatabase().queryForList("SELECT * FROM approval_status ORDER BY description", ApprovalStatus.class);
	}

	@Nonnull
	public ApprovalStatus findApprovalStatusById(ApprovalStatusId approvalStatusId) {
		return getDatabase().queryForObject("SELECT * FROM approval_status WHERE approval_status_id = ?",
				ApprovalStatus.class, approvalStatusId).get();
	}

	@Nonnull
	private void updateContentVisibilityApprovalStatus(@Nonnull UUID contentId,
																										 @Nonnull ApprovalStatusId approvalStatusId,
																										 @Nonnull VisibilityId visibilityId) {
		requireNonNull(contentId);
		requireNonNull(approvalStatusId);
		requireNonNull(visibilityId);

		if (visibilityId == VisibilityId.PRIVATE)
			getDatabase().execute("UPDATE content SET owner_institution_approval_status_id = ? WHERE content_id=? ",
					approvalStatusId, contentId);
		else
			getDatabase().execute("UPDATE content SET other_institution_approval_status_id = ? WHERE content_id=? ",
					approvalStatusId, contentId);

	}

	@Nonnull
	public void updateContentVisibilityApprovalStatusForAccount(@Nonnull Account account,
																															@Nonnull UpdateContentApprovalStatusRequest request) {
		requireNonNull(account);
		requireNonNull(request);

		AdminContent content = findAdminContentByIdForInstitution(account.getInstitutionId(), request.getContentId())
				.orElseThrow();

		//Super Administrators only control public visibility
		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR) {
			updateContentVisibilityApprovalStatus(request.getContentId(), request.getApprovalStatusId(), VisibilityId.PUBLIC);

			if (request.getApprovalStatusId() == ApprovalStatusId.APPROVED) {
				addContentToInstitutionsForPublic(request.getContentId());
			} else if (request.getApprovalStatusId() == ApprovalStatusId.REJECTED) {
				removeContentFromInstitutionsForPublic(request.getContentId());
			}

		} else if (account.getRoleId() == RoleId.ADMINISTRATOR) {
			//Administrators control network and private visibility
			if (content.getVisibilityId() == VisibilityId.NETWORK) {
				updateContentVisibilityApprovalStatus(request.getContentId(), request.getApprovalStatusId(), VisibilityId.NETWORK);
			}
			updateContentVisibilityApprovalStatus(request.getContentId(), request.getApprovalStatusId(), VisibilityId.PRIVATE);

			if (content.getOwnerInstitutionId() == account.getInstitutionId()) {
				getDatabase().execute("UPDATE institution_content SET approved_flag = ? WHERE content_id = ? AND institution_id = ?", true, content.getContentId(), account.getInstitutionId());
			}
		}
	}

	@Nonnull
	public void deleteContentById(@Nonnull UUID contentId) {
		requireNonNull(contentId);

		getDatabase().execute("UPDATE content SET deleted_flag = true WHERE content_id = ? ", contentId);
	}

	@Nonnull
	public void updateArchiveFlagContentById(@Nonnull UpdateContentArchivedStatus command) {
		requireNonNull(command);

		getDatabase().execute("UPDATE content SET archived_flag = ? WHERE content_id = ? ",
				command.getArchivedFlag(), command.getContentId());
	}

	@Nonnull
	public List<UUID> findTagsForContent(@Nonnull UUID contentId) {
		return getDatabase().queryForList("SELECT answer_id FROM answer_content WHERE content_id = ?",
				UUID.class, contentId);
	}

	@Nonnull
	public AdminContent createContent(@Nonnull Account account, @Nonnull CreateContentRequest command) {
		requireNonNull(account);
		requireNonNull(command);

		UUID contentId = UUID.randomUUID();

		String title = trimToNull(command.getTitle());
		String url = trimToNull(command.getUrl());
		String imageUrl = trimToNull(command.getImageUrl());
		String description = trimToNull(command.getDescription());
		String author = trimToNull(command.getAuthor());
		String contentTypeLabelId = trimToNull(command.getContentTypeLabelId());
		VisibilityId visibilityCommand = command.getVisibilityId();
		ContentTypeId contentTypeId = command.getContentTypeId();
		String durationInMinutesString = trimToNull(command.getDurationInMinutes());
		ApprovalStatusId otherInstitutionsApprovalStatusId = ApprovalStatusId.PENDING;

		PersonalizeAssessmentChoicesCommand contentTagCommand = command.getContentTags();
		List<InstitutionId> visibleInstitutionIds = command.getInstitutionIdList() == null ? emptyList() : command.getInstitutionIdList();

		LocalDate createdDate = command.getDateCreated();

		ValidationException validationException = new ValidationException();

		if (title == null)
			validationException.add(new FieldError("title", getStrings().get("Title is required.")));

		if (url == null && contentTypeId != null && contentTypeId != ContentTypeId.INT_BLOG)
			validationException.add(new FieldError("url", getStrings().get("URL is required.")));

		if (author == null)
			validationException.add(new FieldError("author", getStrings().get("Author is required.")));

		if (description == null) {
			validationException.add(new FieldError("description", getStrings().get("Description is required")));
		}

		if (contentTypeId == null) {
			validationException.add(new FieldError("contentTypeId", getStrings().get("Content type is required")));
		}

		if (contentTypeLabelId == null) {
			validationException.add(new FieldError("contentTypeLabel", getStrings().get("Content type label is required")));
		}

		if (account.getRoleId() != RoleId.ADMINISTRATOR && account.getRoleId() != RoleId.SUPER_ADMINISTRATOR) {
			visibilityCommand = VisibilityId.PRIVATE;
		} else if (visibilityCommand == null) {
			validationException.add(new FieldError("visiblityId", getStrings().get("Visibility is required")));
		}

		if (visibilityCommand == VisibilityId.NETWORK && visibleInstitutionIds.isEmpty()) {
			validationException.add(new FieldError("institutionIds", getStrings().get("Institutions are required for in-network visibility")));
		}

		Map<UUID, List<SubmissionAnswer>> contentTagChoices = null;
		if (account.getRoleId() == RoleId.ADMINISTRATOR) {
			if (contentTagCommand != null && contentTagCommand.getChoices().size() > 0) {
				Assessment assessment = getAssessmentService().findAssessmentByTypeForInstitution(AssessmentTypeId.INTRO, account.getInstitutionId()).orElseThrow();
				contentTagChoices = getAssessmentService().validateIntroAssessmentSubmissionCommand(contentTagCommand, assessment, validationException);
			}
		} else {
			contentTagChoices = emptyMap();
		}

		if (durationInMinutesString != null && !ValidationUtility.isValidInteger(durationInMinutesString))
			validationException.add(new FieldError("durationInMinutes", getStrings().get("Must be an integer")));

		if (validationException.hasErrors())
			throw validationException;

		Integer durationInMinutes = durationInMinutesString == null ? null : Integer.parseInt(durationInMinutesString);

		if (url != null && !url.startsWith("http://") && !url.startsWith("https://"))
			url = String.format("https://%s", url);

		ApprovalStatusId initialApprovalStatus;
		if (account.getRoleId() == RoleId.ADMINISTRATOR || account.getRoleId() == RoleId.SUPER_ADMINISTRATOR) {
			initialApprovalStatus = ApprovalStatusId.APPROVED;
		} else {
			initialApprovalStatus = ApprovalStatusId.PENDING;
		}

		getDatabase().execute("INSERT INTO content (content_id, content_type_id, title, url, image_url, date_created, " +
						"duration_in_minutes, description, author, owner_institution_id, content_type_label_id, " +
						"owner_institution_approval_status_id, other_institution_approval_status_id, visibility_id) " +
						"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
				contentId, command.getContentTypeId(), title, url, imageUrl, createdDate, durationInMinutes,
				description, author, account.getInstitutionId(), command.getContentTypeLabelId(),
				initialApprovalStatus, otherInstitutionsApprovalStatusId, visibilityCommand);


		addContentToInstitution(contentId, account.getInstitutionId(), initialApprovalStatus == ApprovalStatusId.APPROVED);
		addContentToInstitution(contentId, InstitutionId.COBALT, true);

		if (visibilityCommand == VisibilityId.NETWORK) {
			otherInstitutionsApprovalStatusId = ApprovalStatusId.APPROVED;
			for (InstitutionId institutionId : visibleInstitutionIds) {
				addContentToInstitution(contentId, institutionId, false);
			}
		} else if (visibilityCommand == VisibilityId.PUBLIC) {
			ApprovalStatusId publicApprovalStatusId = account.getRoleId() == RoleId.SUPER_ADMINISTRATOR ? ApprovalStatusId.APPROVED : ApprovalStatusId.PENDING;
			otherInstitutionsApprovalStatusId = publicApprovalStatusId;
			for (Institution institution : getInstitutionService().findInstitutionsWithoutSpecifiedContentId(contentId))
				addContentToInstitution(contentId, institution.getInstitutionId(), otherInstitutionsApprovalStatusId == ApprovalStatusId.APPROVED);
		}

		getDatabase().execute("UPDATE content SET other_institution_approval_status_id = ? WHERE content_id=?",
				otherInstitutionsApprovalStatusId, contentId);

		if (contentTagChoices != null) {
			tagContent(contentId, contentTagChoices, false);
		}

		AdminContent adminContent = findAdminContentByIdForInstitution(account.getInstitutionId(), contentId).get();
		applyTagsToAdminContent(adminContent, account.getInstitutionId());
		sendAdminNotification(account, adminContent);
		return adminContent;
	}

	@Nonnull
	private void sendAdminNotification(@Nonnull Account accountAddingContent,
																		 @Nonnull AdminContent adminContent) {
		List<Account> accountsToNotify;
		if (accountAddingContent.getRoleId() == RoleId.SUPER_ADMINISTRATOR) {
			return;
		} else if (accountAddingContent.getRoleId() == RoleId.ADMINISTRATOR) {
			accountsToNotify = getAccountService().findSuperAdminAccounts();
		} else {
			accountsToNotify = getAccountService().findAdminAccountsForInstitution(accountAddingContent.getInstitutionId());
		}

		String date = adminContent.getDateCreated() == null ? getFormatter().formatDate(LocalDate.now(), FormatStyle.SHORT) : getFormatter().formatDate(adminContent.getDateCreated(), FormatStyle.SHORT);

		getDatabase().currentTransaction().get().addPostCommitOperation(() -> {
			for (Account accountToNotify : accountsToNotify) {
				if (accountToNotify.getEmailAddress() != null) {
					EmailMessage emailMessage = new EmailMessage.Builder(EmailMessageTemplate.ADMIN_CMS_CONTENT_ADDED, accountToNotify.getLocale())
							.toAddresses(List.of(accountToNotify.getEmailAddress()))
							.messageContext(Map.of(
									"adminAccountName", Normalizer.normalizeName(accountToNotify.getFirstName(), accountToNotify.getLastName()).orElse(getStrings().get("Anonymous User")),
									"submittingAccountName", Normalizer.normalizeName(accountAddingContent.getFirstName(), accountAddingContent.getLastName()).orElse(getStrings().get("Anonymous User")),
									"contentType", adminContent.getContentTypeId().name(),
									"contentTitle", adminContent.getTitle(),
									"contentAuthor", adminContent.getAuthor(),
									"submissionDate", date,
									"cmsListUrl", getLinkGenerator().generateCmsMyContentLink(accountToNotify.getInstitutionId())
							))
							.build();

					getEmailMessageManager().enqueueMessage(emailMessage);
				}
			}
		});

	}

	@Nonnull
	public AdminContent updateContent(@Nonnull Account account, @Nonnull UpdateContentRequest command) {
		requireNonNull(account);
		requireNonNull(command);

		String titleCommand = trimToNull(command.getTitle());
		String urlCommand = trimToNull(command.getUrl());
		String imageUrlCommand = trimToNull(command.getImageUrl());
		String descriptionCommand = trimToNull(command.getDescription());
		String authorCommand = trimToNull(command.getAuthor());
		String contentTypeLabelIdCommand = trimToNull(command.getContentTypeLabelId());
		LocalDate createdDateCommand = command.getDateCreated();
		VisibilityId visibilityIdCommand = command.getVisibilityId();
		ContentTypeId contentTypeIdCommand = command.getContentTypeId();
		PersonalizeAssessmentChoicesCommand contentTagCommand = command.getContentTags();
		List<InstitutionId> visibleInstitutionIds = command.getInstitutionIdList();
		Boolean addToInstitution = command.getAddToInstitution() == null ? false : command.getAddToInstitution();
		Boolean removeFromInstitution = command.getRemoveFromInstitution() == null ? false : command.getRemoveFromInstitution();
		String durationInMinutesString = trimToNull(command.getDurationInMinutes());
		ApprovalStatusId otherInstitutionsApprovalStatusId = ApprovalStatusId.PENDING;
		ApprovalStatusId ownerInstitutionsApprovalStatusId = ApprovalStatusId.APPROVED;

		ValidationException validationException = new ValidationException();

		AdminContent existingContent = findAdminContentByIdForInstitution(account.getInstitutionId(), command.getContentId()).orElseThrow();

		if (hasAdminAccessToContent(account, existingContent)) {
			if (titleCommand != null) {
				existingContent.setTitle(titleCommand);
			}

			if (urlCommand != null) {
				if (!urlCommand.startsWith("http://") && !urlCommand.startsWith("https://"))
					urlCommand = String.format("https://%s", urlCommand);
				existingContent.setUrl(urlCommand);
			}

			if (imageUrlCommand != null) {
				existingContent.setImageUrl(imageUrlCommand);
			}

			if (descriptionCommand != null) {
				existingContent.setDescription(descriptionCommand);
			}

			if (authorCommand != null) {
				existingContent.setAuthor(authorCommand);
			}

			if (contentTypeLabelIdCommand != null) {
				existingContent.setContentTypeLabelId(contentTypeLabelIdCommand);
			}

			if (contentTypeIdCommand != null) {
				existingContent.setContentTypeId(contentTypeIdCommand);
			}

			if (createdDateCommand != null) {
				existingContent.setDateCreated(createdDateCommand);
			}
		}

		Map<UUID, List<SubmissionAnswer>> contentTagChoices = null;
		Assessment assessment = getAssessmentService().findAssessmentByTypeForInstitution(AssessmentTypeId.INTRO, account.getInstitutionId()).orElseThrow();
		;
		//if (existingContent.getOwnerInstitutionId().equals(account.getInstitutionId())) {
		if (contentTagCommand != null && contentTagCommand.getChoices().size() > 0) {
			contentTagChoices = getAssessmentService().validateIntroAssessmentSubmissionCommand(contentTagCommand, assessment, validationException);
		}
		//} else {

		//}

		if (addToInstitution && removeFromInstitution) {
			validationException.add(new FieldError("remove", getStrings().get("Are you adding or removing? Can't do both")));
		}

		if (durationInMinutesString != null && !ValidationUtility.isValidInteger(durationInMinutesString))
			validationException.add(new FieldError("durationInMinutes", getStrings().get("Must be an integer")));

		if (validationException.hasErrors())
			throw validationException;

		Integer durationInMinutes = durationInMinutesString == null ? null : Integer.parseInt(durationInMinutesString);
		boolean shouldNotify = false;
		if (removeFromInstitution) {
			getDatabase().execute("UPDATE institution_content SET approved_flag = ? WHERE content_id = ? AND institution_id = ?", false, existingContent.getContentId(), account.getInstitutionId());
			getDatabase().execute("DELETE FROM answer_content WHERE content_id = ? AND answer_id IN " +
					"(SELECT a.answer_id FROM answer a, question q WHERE a.question_id = q.question_id AND " +
					"q.assessment_id = ? )", existingContent.getContentId(), assessment.getAssessmentId());

		} else {

			if (visibilityIdCommand == VisibilityId.NETWORK) {
				otherInstitutionsApprovalStatusId = ApprovalStatusId.APPROVED;
				if (visibleInstitutionIds != null & visibleInstitutionIds.size() > 0) {
					String institutionList = visibleInstitutionIds.stream().map(c -> String.format("'%s'", c))
							.collect(Collectors.joining(","));
					String sql = String.format("DELETE FROM institution_content WHERE content_id= %s " +
									"AND institution_id NOT IN (%s,%s,%s)", institutionList, existingContent.getContentId(),
							InstitutionId.COBALT, existingContent.getOwnerInstitutionId());
					logger.debug("sql = " + sql);
					getDatabase().execute(String.format("DELETE FROM institution_content WHERE content_id= ? " +
									"AND institution_id NOT IN (%s, ?, ?)", institutionList), existingContent.getContentId(),
							InstitutionId.COBALT, existingContent.getOwnerInstitutionId());
					getDatabase().execute(String.format("INSERT INTO institution_content (institution_content_id, institution_id, content_id) " +
									"SELECT uuid_generate_v4(), i.institution_id, ? FROM institution i WHERE i.institution_id IN (%s) AND i.institution_id NOT IN " +
									"(SELECT ic.institution_id FROM institution_content ic WHERE content_id = ?)",
							institutionList), existingContent.getContentId(), existingContent.getContentId());
				} else {
					getDatabase().execute("DELETE FROM institution_content WHERE content_id= ? " +
									"AND institution_id NOT IN (?, ?)", existingContent.getContentId(),
							InstitutionId.COBALT, existingContent.getOwnerInstitutionId());
				}

				if (existingContent.getVisibilityId() != VisibilityId.PUBLIC || existingContent.getVisibilityId() != VisibilityId.NETWORK) {
					shouldNotify = true;
				}

			} else if (visibilityIdCommand == VisibilityId.PUBLIC) {
				ApprovalStatusId publicApprovalStatusId = account.getRoleId() == RoleId.SUPER_ADMINISTRATOR ? ApprovalStatusId.APPROVED : ApprovalStatusId.PENDING;
				otherInstitutionsApprovalStatusId = publicApprovalStatusId;
				if (existingContent.getVisibilityId() != VisibilityId.PUBLIC) {
					shouldNotify = true;
				}
			}

			if (account.getRoleId() != RoleId.ADMINISTRATOR)
				ownerInstitutionsApprovalStatusId = existingContent.getOwnerInstitutionApprovalStatusId();

			getDatabase().execute("UPDATE content SET content_type_id=?, title=?, url=?, image_url=?, date_created=?, " +
							"duration_in_minutes=?, description=?, author=?, content_type_label_id=?, other_institution_approval_status_id = ?, " + "" +
							"visibility_id = ?, owner_institution_approval_status_id = ? WHERE content_id=?",
					existingContent.getContentTypeId(), existingContent.getTitle(), existingContent.getUrl(), existingContent.getImageUrl(), existingContent.getDateCreated(),
					durationInMinutes, existingContent.getDescription(), existingContent.getAuthor(),
					existingContent.getContentTypeLabelId(), otherInstitutionsApprovalStatusId, visibilityIdCommand, ownerInstitutionsApprovalStatusId, existingContent.getContentId());

			if (addToInstitution || account.getRoleId() == RoleId.ADMINISTRATOR) {
				getDatabase().execute("UPDATE institution_content SET approved_flag = ? WHERE content_id = ? AND institution_id = ?", true, existingContent.getContentId(), account.getInstitutionId());
			}

			if (contentTagChoices != null) {
				tagContent(existingContent.getContentId(), contentTagChoices, addToInstitution);
			}

		}

		AdminContent adminContent = findAdminContentByIdForInstitution(account.getInstitutionId(), command.getContentId()).orElse(null);

		if (adminContent == null) {
			if (addToInstitution) {
				Content content = findContentById(command.getContentId()).get();
				adminContent = findAdminContentByIdForInstitution(content.getOwnerInstitutionId(), command.getContentId()).get();
			} else {
				throw new IllegalStateException(format("Can't find admin content for account ID %s and content ID %s",
						account.getAccountId(), command.getContentId()));
			}
		}

		applyTagsToAdminContent(adminContent, account.getInstitutionId());

		if (shouldNotify) {
			sendAdminNotification(account, adminContent);
		}
		return adminContent;
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
																											 @Nonnull List<Content> filteredContent) {
		requireNonNull(account);
		requireNonNull(filteredContent);

		return findAdditionalContentForAccount(account, filteredContent, null, null, null);
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

		//Do not show internal blog posts to anon users
		if (account.getAccountSourceId().compareTo(AccountSource.AccountSourceId.ANONYMOUS) == 0) {
			unfilteredQuery.append("AND content_type_id != ? ");
			unfilteredParameters.add(ContentTypeId.INT_BLOG);
		}

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
	public List<ContentTypeLabel> findContentTypeLabels() {
		return getDatabase().queryForList("SELECT * FROM content_type_label ORDER BY description", ContentTypeLabel.class);
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

		// Do not show internal blog posts to anon users
		if (account.getAccountSourceId().compareTo(AccountSource.AccountSourceId.ANONYMOUS) == 0) {
			query.append("AND content_type_id != ? ");
			parameters.add(ContentTypeId.INT_BLOG);
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
	public Boolean hasAdminAccessToContent(@Nonnull Account account, @Nonnull AdminContent content) {
		requireNonNull(account);
		requireNonNull(content);

		if (account.getRoleId() == RoleId.SUPER_ADMINISTRATOR)
			return true;
		else if (account.getRoleId() == RoleId.ADMINISTRATOR) {
			if (account.getInstitutionId() == content.getOwnerInstitutionId())
				return true;
			else
				return false;
		} else
			return false;
	}

	@Nonnull
	public List<Content> findVisibleContentByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		// Currently we don't have institution-specific tag groups.
		// But this method accepts an institution ID in case we do in the future...
		List<Content> contents = getDatabase().queryForList("""
				SELECT c.*		    
				FROM content c, institution_content ic
				WHERE c.content_id=ic.content_id
				AND ic.approved_flag=TRUE
				AND ic.institution_id=?
								""", Content.class, institutionId);

		applyTagsToContents(contents, institutionId);

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

	/**
	 * Note: modifies {@code content} parameter in-place.
	 */
	@Nonnull
	protected <T extends AdminContent> void applyTagsToAdminContent(@Nonnull T adminContent,
																																	@Nonnull InstitutionId institutionId) {
		requireNonNull(adminContent);
		requireNonNull(institutionId);

		adminContent.setTags(getTagService().findTagsByContentIdAndInstitutionId(adminContent.getContentId(), institutionId));
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
	protected EmailMessageManager getEmailMessageManager() {
		return emailMessageManagerProvider.get();
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
