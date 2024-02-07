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
import com.cobaltplatform.api.integration.gcal.GoogleCalendarUrlGenerator;
import com.cobaltplatform.api.integration.ical.ICalInviteGenerator;
import com.cobaltplatform.api.integration.ical.ICalInviteGenerator.InviteAttendee;
import com.cobaltplatform.api.integration.ical.ICalInviteGenerator.InviteMethod;
import com.cobaltplatform.api.integration.ical.ICalInviteGenerator.InviteOrganizer;
import com.cobaltplatform.api.integration.ical.ICalInviteGenerator.OrganizerAttendeeStrategy;
import com.cobaltplatform.api.messaging.email.EmailAttachment;
import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageContextKey;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.api.request.CancelGroupSessionReservationRequest;
import com.cobaltplatform.api.model.api.request.CreateFileUploadRequest;
import com.cobaltplatform.api.model.api.request.CreateGroupSessionRequest;
import com.cobaltplatform.api.model.api.request.CreateGroupSessionRequestRequest;
import com.cobaltplatform.api.model.api.request.CreateGroupSessionReservationRequest;
import com.cobaltplatform.api.model.api.request.CreateGroupSessionResponseRequest;
import com.cobaltplatform.api.model.api.request.CreateScheduledMessageRequest;
import com.cobaltplatform.api.model.api.request.CreateScreeningQuestionRequest;
import com.cobaltplatform.api.model.api.request.FindGroupSessionRequestsRequest;
import com.cobaltplatform.api.model.api.request.FindGroupSessionsRequest;
import com.cobaltplatform.api.model.api.request.UpdateAccountEmailAddressRequest;
import com.cobaltplatform.api.model.api.request.UpdateGroupSessionRequest;
import com.cobaltplatform.api.model.api.request.UpdateGroupSessionRequestRequest;
import com.cobaltplatform.api.model.api.request.UpdateGroupSessionRequestStatusRequest;
import com.cobaltplatform.api.model.api.request.UpdateGroupSessionStatusRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.AssessmentType.AssessmentTypeId;
import com.cobaltplatform.api.model.db.FileUpload;
import com.cobaltplatform.api.model.db.FontSize.FontSizeId;
import com.cobaltplatform.api.model.db.GroupSession;
import com.cobaltplatform.api.model.db.GroupSessionCollection;
import com.cobaltplatform.api.model.db.GroupSessionLearnMoreMethod.GroupSessionLearnMoreMethodId;
import com.cobaltplatform.api.model.db.GroupSessionLocationType.GroupSessionLocationTypeId;
import com.cobaltplatform.api.model.db.GroupSessionRequest;
import com.cobaltplatform.api.model.db.GroupSessionRequestStatus;
import com.cobaltplatform.api.model.db.GroupSessionRequestStatus.GroupSessionRequestStatusId;
import com.cobaltplatform.api.model.db.GroupSessionReservation;
import com.cobaltplatform.api.model.db.GroupSessionResponse;
import com.cobaltplatform.api.model.db.GroupSessionSchedulingSystem.GroupSessionSchedulingSystemId;
import com.cobaltplatform.api.model.db.GroupSessionStatus;
import com.cobaltplatform.api.model.db.GroupSessionStatus.GroupSessionStatusId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Question;
import com.cobaltplatform.api.model.db.QuestionType.QuestionTypeId;
import com.cobaltplatform.api.model.db.Role.RoleId;
import com.cobaltplatform.api.model.db.ScreeningFlow;
import com.cobaltplatform.api.model.db.Tag;
import com.cobaltplatform.api.model.db.TagGroupSession;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;
import com.cobaltplatform.api.model.service.FileUploadResult;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.model.service.GroupSessionRequestWithTotalCount;
import com.cobaltplatform.api.model.service.GroupSessionStatusWithCount;
import com.cobaltplatform.api.model.service.GroupSessionUrlValidationResult;
import com.cobaltplatform.api.model.service.GroupSessionWithTotalCount;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.LinkGenerator;
import com.cobaltplatform.api.util.Normalizer;
import com.cobaltplatform.api.util.UploadManager;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.ValidationUtility;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.apache.commons.lang3.tuple.Pair;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
import static com.cobaltplatform.api.util.ValidationUtility.isValidEmailAddress;
import static com.cobaltplatform.api.util.ValidationUtility.isValidUrl;
import static com.cobaltplatform.api.util.ValidationUtility.isValidUrlSubdirectory;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class GroupSessionService implements AutoCloseable {
	@Nonnull
	private static final Long BACKGROUND_TASK_INTERVAL_IN_SECONDS;
	@Nonnull
	private static final Long BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS;
	@Nonnull
	private static final Set<String> ILLEGAL_GROUP_SESSION_URL_NAMES;

	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final Provider<MessageService> messageServiceProvider;
	@Nonnull
	private final Provider<BackgroundSyncTask> backgroundSyncTaskProvider;
	@Nullable
	private final ScreeningService screeningService;
	@Nullable
	private final TagService tagService;
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final UploadManager uploadManager;
	@Nonnull
	private final LinkGenerator linkGenerator;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Normalizer normalizer;
	@Nonnull
	private final GoogleCalendarUrlGenerator googleCalendarUrlGenerator;
	@Nonnull
	private final ICalInviteGenerator iCalInviteGenerator;
	@Nonnull
	private final PolicyFactory htmlSanitizingPolicyFactory;
	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;
	@Nonnull
	private final Object backgroundTaskLock;
	@Nonnull
	private Boolean backgroundTaskStarted;
	@Nullable
	private ScheduledExecutorService backgroundTaskExecutorService;

	@Nullable
	private SystemService systemService;

	static {
		BACKGROUND_TASK_INTERVAL_IN_SECONDS = 60L;
		BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS = 10L;

		ILLEGAL_GROUP_SESSION_URL_NAMES = Set.of(
				"request",
				"by-request",
				"collection",
				"add-internal",
				"add-external"
		);
	}

	@Inject
	public GroupSessionService(@Nonnull Provider<AccountService> accountServiceProvider,
														 @Nonnull Provider<InstitutionService> institutionServiceProvider,
														 @Nonnull Provider<MessageService> messageServiceProvider,
														 @Nonnull Provider<BackgroundSyncTask> backgroundSyncTaskProvider,
														 @Nonnull ScreeningService screeningService,
														 @Nonnull TagService tagService,
														 @Nonnull DatabaseProvider databaseProvider,
														 @Nonnull UploadManager uploadManager,
														 @Nonnull LinkGenerator linkGenerator,
														 @Nonnull Formatter formatter,
														 @Nonnull Normalizer normalizer,
														 @Nonnull GoogleCalendarUrlGenerator googleCalendarUrlGenerator,
														 @Nonnull ICalInviteGenerator iCalInviteGenerator,
														 @Nonnull Configuration configuration,
														 @Nonnull Strings strings,
														 @Nonnull SystemService systemService) {
		requireNonNull(accountServiceProvider);
		requireNonNull(institutionServiceProvider);
		requireNonNull(messageServiceProvider);
		requireNonNull(screeningService);
		requireNonNull(tagService);
		requireNonNull(backgroundSyncTaskProvider);
		requireNonNull(databaseProvider);
		requireNonNull(uploadManager);
		requireNonNull(linkGenerator);
		requireNonNull(formatter);
		requireNonNull(normalizer);
		requireNonNull(googleCalendarUrlGenerator);
		requireNonNull(iCalInviteGenerator);
		requireNonNull(configuration);
		requireNonNull(strings);
		requireNonNull(systemService);

		this.accountServiceProvider = accountServiceProvider;
		this.institutionServiceProvider = institutionServiceProvider;
		this.messageServiceProvider = messageServiceProvider;
		this.screeningService = screeningService;
		this.tagService = tagService;
		this.backgroundSyncTaskProvider = backgroundSyncTaskProvider;
		this.databaseProvider = databaseProvider;
		this.uploadManager = uploadManager;
		this.linkGenerator = linkGenerator;
		this.formatter = formatter;
		this.normalizer = normalizer;
		this.googleCalendarUrlGenerator = googleCalendarUrlGenerator;
		this.iCalInviteGenerator = iCalInviteGenerator;
		this.configuration = configuration;
		this.strings = strings;
		this.backgroundTaskLock = new Object();
		this.backgroundTaskStarted = false;
		this.htmlSanitizingPolicyFactory = createHtmlSanitizingPolicyFactory();
		this.logger = LoggerFactory.getLogger(getClass());
		this.systemService = systemService;
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

			getLogger().trace("Starting group session background task...");

			this.backgroundTaskExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("group-session-background-task").build());
			this.backgroundTaskStarted = true;

			getBackgroundTaskExecutorService().get().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						getBackgroundSyncTaskProvider().get().run();
					} catch (Exception e) {
						getLogger().warn(format("Unable to complete group session background task - will retry in %s seconds", String.valueOf(getBackgroundTaskIntervalInSeconds())), e);
					}
				}
			}, getBackgroundTaskInitialDelayInSeconds(), getBackgroundTaskIntervalInSeconds(), TimeUnit.SECONDS);

			getLogger().trace("Group session background task started.");

			return true;
		}
	}

	@Nonnull
	public Boolean stopBackgroundTask() {
		synchronized (getBackgroundTaskLock()) {
			if (!isBackgroundTaskStarted())
				return false;

			getLogger().trace("Stopping group session background task...");

			getBackgroundTaskExecutorService().get().shutdownNow();
			this.backgroundTaskExecutorService = null;
			this.backgroundTaskStarted = false;

			getLogger().trace("Group session background task stopped.");

			return true;
		}
	}

	@Nonnull
	public FindResult<GroupSession> findGroupSessions(@Nonnull FindGroupSessionsRequest request) {
		requireNonNull(request);

		Integer pageNumber = request.getPageNumber();
		Integer pageSize = request.getPageSize();
		GroupSessionStatusId groupSessionStatusId = request.getGroupSessionStatusId();
		String urlName = trimToNull(request.getUrlName());
		String searchQuery = trimToNull(request.getSearchQuery());
		InstitutionId institutionId = request.getInstitutionId();
		Account account = request.getAccount();
		FindGroupSessionsRequest.FilterBehavior filterBehavior = request.getFilterBehavior() == null ? FindGroupSessionsRequest.FilterBehavior.DEFAULT : request.getFilterBehavior();
		FindGroupSessionsRequest.OrderBy orderBy = request.getOrderBy() == null ? FindGroupSessionsRequest.OrderBy.START_TIME_DESCENDING : request.getOrderBy();
		UUID groupSessionCollectionId = request.getGroupSessionCollectionId();
		GroupSessionSchedulingSystemId groupSessionSchedulingSystemId = request.getGroupSessionSchedulingSystemId();
		Boolean visibleFlag = request.getVisibleFlag();

		List<Object> parameters = new ArrayList<>();

		if (pageNumber == null || pageNumber < 0)
			pageNumber = 0;

		if (pageSize == null || pageSize < 0)
			pageSize = getGroupSessionDefaultPageSize();

		if (pageSize > getGroupSessionMaximumPageSize())
			pageSize = getGroupSessionMaximumPageSize();

		StringBuilder sql = new StringBuilder("SELECT gs.*, COUNT(gs.group_session_id) OVER() AS total_count " +
				"FROM v_group_session gs WHERE 1=1 ");

		if (institutionId != null) {
			sql.append("AND gs.institution_id=? ");
			parameters.add(institutionId);
		}

		if (groupSessionStatusId != null) {
			sql.append("AND gs.group_session_status_id=? ");
			parameters.add(groupSessionStatusId);
		}

		if (urlName != null) {
			sql.append("AND gs.url_name=? ");
			parameters.add(urlName);
		}

		if (searchQuery != null) {
			sql.append("AND ((gs.en_search_vector @@ websearch_to_tsquery('english', ?)) OR (gs.title ILIKE CONCAT('%',?,'%') OR gs.description ILIKE CONCAT('%',?,'%'))) ");
			parameters.add(searchQuery);
			parameters.add(searchQuery);
			parameters.add(searchQuery);
		}

		if (filterBehavior == FindGroupSessionsRequest.FilterBehavior.ONLY_MY_SESSIONS) {
			if (account == null)
				throw new IllegalStateException(format("Account is required when calling %s.%s", FindGroupSessionsRequest.FilterBehavior.class.getSimpleName(), filterBehavior.name()));

			sql.append("AND gs.group_session_status_id=? AND (gs.submitter_account_id=? OR (gs.facilitator_account_id=? OR (gs.facilitator_email_address IS NOT NULL AND LOWER(gs.facilitator_email_address)=?))) ");
			parameters.add(GroupSessionStatusId.NEW);
			parameters.add(account.getAccountId());
			parameters.add(account.getAccountId());
			parameters.add(getNormalizer().normalizeEmailAddress(account.getEmailAddress()).orElse(null));
		}

		if (visibleFlag != null)
			if (visibleFlag == true)
				sql.append("AND gs.visible_flag = TRUE ");
			else
				sql.append("AND gs.visible_flag = FALSE ");

		if (groupSessionCollectionId != null) {
			sql.append("AND group_session_collection_id = ? ");
			parameters.add(groupSessionCollectionId);
		}

		if (groupSessionSchedulingSystemId != null) {
			sql.append("AND group_session_scheduling_system_id = ? ");
			parameters.add(groupSessionSchedulingSystemId);
		}

		sql.append("ORDER BY ");

		if (orderBy == FindGroupSessionsRequest.OrderBy.START_TIME_ASCENDING)
			sql.append("gs.start_date_time ASC ");
		else if (orderBy == FindGroupSessionsRequest.OrderBy.START_TIME_DESCENDING)
			sql.append("gs.start_date_time DESC NULLS LAST ");
		else if (orderBy == FindGroupSessionsRequest.OrderBy.CAPACITY_ASCENDING)
			sql.append("gs.seats ASC ");
		else if (orderBy == FindGroupSessionsRequest.OrderBy.CAPACITY_DESCENDING)
			sql.append("gs.seats DESC NULLS LAST ");
		else if (orderBy == FindGroupSessionsRequest.OrderBy.DATE_ADDED_ASCENDING)
			sql.append("gs.created ASC ");
		else if (orderBy == FindGroupSessionsRequest.OrderBy.DATE_ADDED_DESCENDING)
			sql.append("gs.created DESC NULLS LAST ");
		else if (orderBy == FindGroupSessionsRequest.OrderBy.REGISTERED_ASCENDING)
			sql.append("gs.seats_reserved ASC ");
		else if (orderBy == FindGroupSessionsRequest.OrderBy.REGISTERED_DESCENDING)
			sql.append("gs.seats_reserved DESC NULLS LAST ");
		else
			throw new IllegalArgumentException(format("Unsure what to do with %s.%s", FindGroupSessionsRequest.OrderBy.class.getSimpleName(), orderBy.name()));

		// Break ordering tie in the event that there are ambiguities
		sql.append(", gs.group_session_id ");

		sql.append("LIMIT ? ");
		parameters.add(pageSize);
		sql.append("OFFSET ?");
		parameters.add(pageNumber * pageSize);

		List<GroupSessionWithTotalCount> groupSessions = getDatabase().queryForList(sql.toString(),
				GroupSessionWithTotalCount.class, parameters.toArray());

		return new FindResult(groupSessions, groupSessions.size() == 0 ? 0 : groupSessions.get(0).getTotalCount());
	}

	@Nonnull
	public Integer getGroupSessionDefaultPageSize() {
		return 50;
	}

	@Nonnull
	public Integer getGroupSessionMaximumPageSize() {
		return 100;
	}

	public boolean canTakeActionOnGroupSessions(@Nullable Account account) {
		if (account == null)
			return false;

		if (account.getRoleId() == RoleId.ADMINISTRATOR)
			return getDatabase().queryForObject("SELECT COUNT(*) > 0 FROM group_session WHERE institution_id=?", Boolean.class, account.getInstitutionId()).get();

		return getDatabase().queryForObject("SELECT COUNT(*) > 0 FROM group_session " +
						"WHERE institution_id=? AND group_session_status_id=? AND (submitter_account_id=? OR (facilitator_account_id=? OR LOWER(?)=LOWER(facilitator_email_address)))", Boolean.class,
				account.getInstitutionId(), GroupSessionStatusId.NEW, account.getAccountId(), account.getAccountId(), account.getEmailAddress()).get();
	}

	public boolean canTakeActionOnGroupSessionRequests(@Nullable Account account) {
		if (account == null)
			return false;

		if (account.getRoleId() == RoleId.ADMINISTRATOR)
			return getDatabase().queryForObject("SELECT COUNT(*) > 0 FROM group_session_request WHERE institution_id=?", Boolean.class, account.getInstitutionId()).get();

		return getDatabase().queryForObject("SELECT COUNT(*) > 0 FROM group_session_request " +
						"WHERE institution_id=? AND group_session_request_status_id=? AND (submitter_account_id=? OR (facilitator_account_id=? OR LOWER(?)=LOWER(facilitator_email_address)))", Boolean.class,
				account.getInstitutionId(), GroupSessionRequestStatusId.NEW, account.getAccountId(), account.getAccountId(), account.getEmailAddress()).get();
	}

	@Nonnull
	public List<GroupSessionStatusWithCount> findGroupSessionStatusesWithCounts(@Nullable InstitutionId institutionId) {
		List<GroupSessionStatus> groupSessionStatuses = getDatabase().queryForList("SELECT * FROM group_session_status ORDER BY group_session_status_id", GroupSessionStatus.class);
		List<GroupSessionStatusWithCount> groupSessionStatusesWithCounts = new ArrayList<>(groupSessionStatuses.size());

		// Not ideal, but additional criteria makes LEFT OUTER JOIN awkward
		for (GroupSessionStatus groupSessionStatus : groupSessionStatuses) {
			List<Object> parameters = new ArrayList<>();

			StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM group_session WHERE group_session_status_id=? ");
			parameters.add(groupSessionStatus.getGroupSessionStatusId());

			if (institutionId != null) {
				sql.append("AND institution_id=? ");
				parameters.add(institutionId);
			}

			Integer totalCount = getDatabase().queryForObject(sql.toString(), Integer.class, sqlVaragsParameters(parameters)).get();

			GroupSessionStatusWithCount groupSessionStatusWithCount = new GroupSessionStatusWithCount();
			groupSessionStatusWithCount.setTotalCount(totalCount);
			groupSessionStatusWithCount.setGroupSessionStatusId(groupSessionStatus.getGroupSessionStatusId());
			groupSessionStatusWithCount.setGroupSessionStatusIdDescription(groupSessionStatus.getDescription());

			groupSessionStatusesWithCounts.add(groupSessionStatusWithCount);
		}

		return groupSessionStatusesWithCounts;
	}

	@Nonnull
	public Optional<GroupSession> findGroupSessionByIdIncludingDeleted(@Nullable UUID groupSessionId) {
		if (groupSessionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM group_session
				WHERE group_session_id=?
				""", GroupSession.class, groupSessionId);
	}

	@Nonnull
	public Optional<GroupSession> findGroupSessionById(@Nullable Object groupSessionIdentifier,
																										 @Nullable Account account) {
		if (groupSessionIdentifier == null || account == null)
			return Optional.empty();

		return findGroupSessionById(groupSessionIdentifier, account.getInstitutionId());
	}

	@Nonnull
	public Optional<GroupSession> findGroupSessionById(@Nullable Object groupSessionIdentifier,
																										 @Nullable InstitutionId institutionId) {
		if (groupSessionIdentifier == null || institutionId == null)
			return Optional.empty();
		GroupSession groupSession = null;
		UUID groupSessionId = null;

		if (groupSessionIdentifier instanceof UUID)
			groupSessionId = (UUID) groupSessionIdentifier;
		else if (groupSessionIdentifier instanceof String && ValidationUtility.isValidUUID((String) groupSessionIdentifier))
			groupSessionId = UUID.fromString((String) groupSessionIdentifier);

		if (groupSessionId != null) {
			groupSession = getDatabase().queryForObject("""
							SELECT *
							FROM v_group_session
							WHERE group_session_id=?
							""",
					GroupSession.class, groupSessionId).orElse(null);
		} else if (groupSessionIdentifier instanceof String) {
			groupSession = getDatabase().queryForObject("""
							SELECT *
							FROM v_group_session
							WHERE url_name=?
							AND institution_id=?
							""",
					GroupSession.class, groupSessionIdentifier, institutionId).orElse(null);
		}

		if (groupSession == null)
			return Optional.empty();

		applyTagsToGroupSession(groupSession, institutionId);

		return Optional.of(groupSession);
	}

	@Nonnull
	public Optional<GroupSessionStatus> findGroupSessionStatusById(@Nullable GroupSessionStatusId groupSessionStatusId) {
		if (groupSessionStatusId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM group_session_status WHERE group_session_status_id=?",
				GroupSessionStatus.class, groupSessionStatusId);
	}

	@Nonnull
	public UUID duplicateGroupSession(@Nonnull UUID groupSessionId, @Nonnull Account account) {
		requireNonNull(groupSessionId);
		requireNonNull(account);

		ValidationException validationException = new ValidationException();
		Integer dateOffset = 7;
		UUID destinationGroupSessionId = UUID.randomUUID();

		GroupSession sourceGroupSession = findGroupSessionById(groupSessionId, account).orElse(null);

		if (sourceGroupSession == null)
			validationException.add(new FieldError("groupSessionId", getStrings().get("Not a valid Group Session")));
		else if (sourceGroupSession.getInstitutionId() != account.getInstitutionId())
			validationException.add(new FieldError("groupSessionId", getStrings().get("Group Sessions from other Institutions cannot be duplicated")));

		if (validationException.hasErrors())
			throw validationException;

		//If this is a Cobalt scheduled Group Session then adjust the start and end dates
		if (sourceGroupSession.getGroupSessionSchedulingSystemId() == GroupSessionSchedulingSystemId.COBALT) {
			sourceGroupSession.setStartDateTime(sourceGroupSession.getStartDateTime().plusDays(dateOffset));
			sourceGroupSession.setEndDateTime(sourceGroupSession.getEndDateTime().plusDays(dateOffset));
		}

		GroupSessionUrlValidationResult groupSessionUrlValidationResult = findGroupSessionUrlValidationResults(sourceGroupSession.getUrlName(),
				sourceGroupSession.getInstitutionId(), destinationGroupSessionId);

		if (!groupSessionUrlValidationResult.getAvailable())
			sourceGroupSession.setUrlName(groupSessionUrlValidationResult.getRecommendation());

		getDatabase().execute("""
						INSERT INTO group_session (group_session_id, institution_id,
						group_session_status_id, title, description, submitter_account_id,
						target_email_address, facilitator_account_id, facilitator_name, facilitator_email_address,
						videoconference_url, start_date_time, end_date_time, seats, url_name,
						confirmation_email_content, locale, time_zone, group_session_scheduling_system_id, group_session_location_type_id,
						send_followup_email, followup_email_content, followup_email_survey_url,
						group_session_collection_id, visible_flag, screening_flow_id, send_reminder_email, reminder_email_content,
						followup_time_of_day, followup_day_offset, single_session_flag, date_time_description, group_session_learn_more_method_id, 
						learn_more_description, in_person_location, image_file_upload_id)
						VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
						""",
				destinationGroupSessionId, sourceGroupSession.getInstitutionId(), GroupSessionStatusId.NEW,
				sourceGroupSession.getTitle(), sourceGroupSession.getDescription(), sourceGroupSession.getSubmitterAccountId(), sourceGroupSession.getTargetEmailAddress(),
				sourceGroupSession.getFacilitatorAccountId(), sourceGroupSession.getFacilitatorName(), sourceGroupSession.getFacilitatorEmailAddress(),
				sourceGroupSession.getVideoconferenceUrl(), sourceGroupSession.getStartDateTime(), sourceGroupSession.getEndDateTime(),
				sourceGroupSession.getSeats(), sourceGroupSession.getUrlName(), sourceGroupSession.getConfirmationEmailContent(),
				sourceGroupSession.getLocale(), sourceGroupSession.getTimeZone(), sourceGroupSession.getGroupSessionSchedulingSystemId(),
				sourceGroupSession.getGroupSessionLocationTypeId(), sourceGroupSession.getSendFollowupEmail(), sourceGroupSession.getFollowupEmailContent(),
				sourceGroupSession.getFollowupEmailSurveyUrl(), sourceGroupSession.getGroupSessionCollectionId(), sourceGroupSession.getVisibleFlag(),
				sourceGroupSession.getScreeningFlowId(), sourceGroupSession.getSendReminderEmail(), sourceGroupSession.getReminderEmailContent(),
				sourceGroupSession.getFollowupTimeOfDay(), sourceGroupSession.getFollowupDayOffset(), sourceGroupSession.getSingleSessionFlag(),
				sourceGroupSession.getDateTimeDescription(), sourceGroupSession.getGroupSessionLearnMoreMethodId(), sourceGroupSession.getLearnMoreDescription(),
				sourceGroupSession.getInPersonLocation(), sourceGroupSession.getImageFileUploadId(), sourceGroupSession);

		getDatabase().execute("""
				INSERT INTO tag_group_session (tag_group_session_id, tag_id, group_session_id, institution_id)
				SELECT uuid_generate_v4(), tgs.tag_id, ?, institution_id
				FROM tag_group_session tgs
				WHERE tgs.group_session_id = ?
				""", destinationGroupSessionId, sourceGroupSession.getGroupSessionId());

		return destinationGroupSessionId;
	}

	@Nonnull
	public UUID createGroupSession(@Nonnull CreateGroupSessionRequest request,
																 @Nonnull Account account) {
		requireNonNull(request);
		requireNonNull(account);

		InstitutionId institutionId = request.getInstitutionId();
		GroupSessionSchedulingSystemId groupSessionSchedulingSystemId = request.getGroupSessionSchedulingSystemId();
		GroupSessionLocationTypeId groupSessionLocationTypeId = request.getGroupSessionLocationTypeId();
		String title = trimToNull(request.getTitle());
		String description = trimToNull(request.getDescription());
		String urlName = request.getUrlName() == null ? null : normalizeUrlName(request.getUrlName()).orElse(null);
		String inPersonLocation = trimToNull(request.getInPersonLocation());
		UUID facilitatorAccountId = request.getFacilitatorAccountId();
		String facilitatorName = trimToNull(request.getFacilitatorName());
		String facilitatorEmailAddress = trimToNull(request.getFacilitatorEmailAddress());
		LocalDateTime startDateTime = request.getStartDateTime();
		LocalDateTime endDateTime = request.getEndDateTime();
		Boolean sendFollowupEmail = request.getSendFollowupEmail();
		String followupEmailContent = trimToNull(request.getFollowupEmailContent());
		String followupEmailSurveyUrl = trimToNull(request.getFollowupEmailSurveyUrl());
		Integer seats = request.getSeats();
		String videoconferenceUrl = trimToNull(request.getVideoconferenceUrl());
		List<CreateScreeningQuestionRequest> screeningQuestions = normalizeScreeningQuestions(request.getScreeningQuestions(), request.getScreeningQuestionsV2());
		String confirmationEmailContent = trimToNull(request.getConfirmationEmailContent());
		UUID submitterAccountId = request.getSubmitterAccountId();
		String targetEmailAddress = trimToNull(request.getTargetEmailAddress());
		Account submitterAccount = null;
		Institution institution = null;
		UUID groupSessionId = UUID.randomUUID();
		UUID groupSessionCollectionId = request.getGroupSessionCollectionId();
		Boolean visibleFlag = request.getVisibleFlag();
		UUID screeningFlowId = request.getScreeningFlowId();
		Boolean sendReminderEmail = request.getSendReminderEmail();
		String reminderEmailContent = trimToNull(request.getReminderEmailContent());
		LocalTime followupTimeOfDay = request.getFollowupTimeOfDay();
		Integer followupDayOffset = request.getFollowupDayOffset();
		Boolean singleSessionFlag = request.getSingleSessionFlag();
		String dateTimeDescription = trimToNull(request.getDateTimeDescription());
		Set<String> tagIds = request.getTagIds() == null ? Set.of() : request.getTagIds();
		String learnMoreDescription = trimToNull(request.getLearnMoreDescription());
		GroupSessionLearnMoreMethodId groupSessionLearnMoreMethodId = request.getGroupSessionLearnMoreMethodId();
		Boolean differentEmailAddressForNotifications = request.getDifferentEmailAddressForNotifications();
		LocalDateTime registrationEndDateTime = request.getRegistrationEndDateTime();
		UUID imageFileUploadId = request.getImageFileUploadId();

		ValidationException validationException = new ValidationException();

		if (groupSessionSchedulingSystemId == null)
			groupSessionSchedulingSystemId = GroupSessionSchedulingSystemId.COBALT;

		if (sendFollowupEmail == null)
			sendFollowupEmail = false;

		if (sendReminderEmail == null)
			sendReminderEmail = false;

		if (visibleFlag == null)
			visibleFlag = true;

		if (singleSessionFlag == null)
			singleSessionFlag = true;

		if (!differentEmailAddressForNotifications)
			targetEmailAddress = facilitatorEmailAddress;
		else if (targetEmailAddress == null)
			validationException.add(new FieldError("targetEmailAddress", getStrings().get("Notification email is required.")));

		if (institutionId == null) {
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));
		} else {
			institution = getInstitutionService().findInstitutionById(institutionId).orElse(null);

			if (institution == null)
				validationException.add(new FieldError("institutionId", getStrings().get("Invalid institution.")));
		}

		if (submitterAccountId == null) {
			validationException.add(new FieldError("submitterAccountId", getStrings().get("Submitter account ID is required.")));
		} else {
			submitterAccount = getAccountService().findAccountById(submitterAccountId).orElse(null);

			if (submitterAccount == null)
				validationException.add(new FieldError("submitterAccountId", getStrings().get("Invalid submitter account.")));
		}

		if (title == null)
			validationException.add(new FieldError("title", getStrings().get("Title is required.")));

		// If a non-administrator is adding this content, sanitize its HTML
		if (description != null && account.getRoleId() != RoleId.ADMINISTRATOR)
			description = trimToNull(getHtmlSanitizingPolicyFactory().sanitize(description));

		if (description == null)
			validationException.add(new FieldError("description", getStrings().get("Description is required.")));

		if (urlName == null)
			validationException.add(new FieldError("urlName", getStrings().get("Friendly URL name is required.")));
		else if (!isValidUrlSubdirectory(urlName))
			validationException.add(new FieldError("urlName", getStrings().get("Not a valid Friendly URL")));
		else if (urlNameExistsForInstitutionId(urlName, institution.getInstitutionId(), groupSessionId))
			validationException.add(new FieldError("urlName", getStrings().get("Friendly URL name is already in use.")));

		if (facilitatorName == null)
			validationException.add(new FieldError("facilitatorName", getStrings().get("Facilitator name is required.")));

		if (facilitatorEmailAddress == null)
			validationException.add(new FieldError("facilitatorEmailAddress", getStrings().get("Facilitator email address is required.")));
		else if (!isValidEmailAddress(facilitatorEmailAddress))
			validationException.add(new FieldError("facilitatorEmailAddress", getStrings().get("Facilitator email address is invalid.")));

		LocalDateTime currentLocalDateTime = LocalDateTime.now(institution.getTimeZone());

		if (startDateTime == null) {
			validationException.add(new FieldError("startDateTime", getStrings().get("Start time is required.")));
		} else {
			if (singleSessionFlag && currentLocalDateTime.isAfter(startDateTime))
				validationException.add(new FieldError("startDateTime", getStrings().get("Start time must be in the future.")));
		}

		if (endDateTime == null) {
			validationException.add(new FieldError("endDateTime", getStrings().get("End time is required.")));
		} else {
			if (!singleSessionFlag && currentLocalDateTime.toLocalDate().isAfter(endDateTime.toLocalDate()))
				validationException.add(new FieldError("endDateTime", getStrings().get("End date must be in the future.")));
		}

		if (startDateTime != null && endDateTime != null) {
			if (endDateTime.equals(startDateTime) || endDateTime.isBefore(startDateTime))
				validationException.add(getStrings().get("End time cannot be before start time."));
		}

		if (groupSessionLocationTypeId == null) {
			validationException.add(new FieldError("groupSessionLocationTypeId", getStrings().get("Location type is required.")));
		} else if (groupSessionSchedulingSystemId == GroupSessionSchedulingSystemId.COBALT && groupSessionLocationTypeId == GroupSessionLocationTypeId.VIRTUAL && videoconferenceUrl == null) {
			validationException.add(new FieldError("videoconferenceUrl", getStrings().get("Videoconference URL is required for virtual group sessions.")));
		} else if (groupSessionSchedulingSystemId == GroupSessionSchedulingSystemId.COBALT && groupSessionLocationTypeId == GroupSessionLocationTypeId.IN_PERSON && inPersonLocation == null) {
			validationException.add(new FieldError("inPersonLocation", getStrings().get("A location is required for in-person group sessions.")));
		}

		if (groupSessionSchedulingSystemId == GroupSessionSchedulingSystemId.COBALT) {
			if (sendReminderEmail)
				if (reminderEmailContent == null)
					validationException.add(new FieldError("reminderEmailContent", getStrings().get("Reminder Email Text is required.")));

			if (sendFollowupEmail) {
				if (followupEmailContent == null)
					validationException.add(new FieldError("followupEmailContent", getStrings().get("Follow-up Email Text is required.")));
				if (followupDayOffset == null)
					validationException.add(new FieldError("followupDayOffset", getStrings().get("Number of days after session is required.")));
				if (followupTimeOfDay == null)
					validationException.add(new FieldError("followupTimeOfDay", getStrings().get("Time is required.")));
			}

			if (!singleSessionFlag)
				validationException.add(new FieldError("singleSessionFlag", getStrings().get("Ongoing series is only permitted for external Group Sessions.")));
		} else if (groupSessionSchedulingSystemId == GroupSessionSchedulingSystemId.EXTERNAL) {
			videoconferenceUrl = null;
			confirmationEmailContent = null;

			if (sendReminderEmail)
				validationException.add(new FieldError("sendReminderEmail", getStrings().get("Cannot send reminder emails for external sessions.")));
			if (sendFollowupEmail)
				validationException.add(new FieldError("sendFollowupEmail", getStrings().get("Cannot send follow-up emails for external sessions.")));
		} else {
			throw new UnsupportedOperationException(format("Not sure what to do with %s.%s", GroupSessionSchedulingSystemId.class.getSimpleName(), groupSessionSchedulingSystemId.name()));
		}

		if (videoconferenceUrl != null && !isValidUrl(videoconferenceUrl))
			validationException.add(new FieldError("videoconferenceUrl", getStrings().get("Videoconference URL is invalid.")));

		if (groupSessionCollectionId != null) {
			GroupSessionCollection groupSessionCollection = findGroupSessionCollectionById(groupSessionCollectionId).orElse(null);
			if (groupSessionCollection == null)
				validationException.add(new FieldError("groupSessionCollectionId", getStrings().get("GroupSessionCollectionId is invalid.")));
		}

		if (screeningFlowId != null) {
			Optional<ScreeningFlow> screeningFlow = getScreeningService().findScreeningFlowById(screeningFlowId);
			if (!screeningFlow.isPresent())
				validationException.add(new FieldError("screeningFlowId", getStrings().get("ScreeningFlowId is invalid.")));
		}

		if (groupSessionLearnMoreMethodId != null && learnMoreDescription == null)
			validationException.add(new FieldError("learnMoreDescription", getStrings().get("A way to learn more is required")));

		if (validationException.hasErrors())
			throw validationException;

		if (imageFileUploadId == null)
			imageFileUploadId = getDefaultGroupSessionImageFileUploadId();

		// Never makes sense to specify a videoconference URL in these scenarios
		if (groupSessionLocationTypeId == GroupSessionLocationTypeId.IN_PERSON
				|| groupSessionSchedulingSystemId == GroupSessionSchedulingSystemId.EXTERNAL)
			videoconferenceUrl = null;

		targetEmailAddress = getNormalizer().normalizeEmailAddress(targetEmailAddress).get();
		facilitatorEmailAddress = getNormalizer().normalizeEmailAddress(facilitatorEmailAddress).get();

		getDatabase().execute("""
						INSERT INTO group_session (group_session_id, institution_id,
						group_session_status_id, title, description, submitter_account_id,
						target_email_address, facilitator_account_id, facilitator_name, facilitator_email_address,
					  videoconference_url, start_date_time, end_date_time, seats, url_name,
						confirmation_email_content, locale, time_zone, group_session_scheduling_system_id,
						group_session_location_type_id, send_followup_email, followup_email_content, followup_email_survey_url,
						group_session_collection_id, visible_flag, screening_flow_id, send_reminder_email, reminder_email_content,
						followup_time_of_day, followup_day_offset, single_session_flag, date_time_description, group_session_learn_more_method_id, 
						learn_more_description, different_email_address_for_notifications, in_person_location, registration_end_date_time, image_file_upload_id)
						VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
						""",
				groupSessionId, institutionId, GroupSessionStatusId.NEW,
				title, description, submitterAccountId, targetEmailAddress, facilitatorAccountId, facilitatorName, facilitatorEmailAddress, videoconferenceUrl,
				startDateTime, endDateTime, seats, urlName, confirmationEmailContent, institution.getLocale(), institution.getTimeZone(),
				groupSessionSchedulingSystemId, groupSessionLocationTypeId, sendFollowupEmail, followupEmailContent, followupEmailSurveyUrl,
				groupSessionCollectionId, visibleFlag, screeningFlowId, sendReminderEmail, reminderEmailContent,
				followupTimeOfDay, followupDayOffset, singleSessionFlag, dateTimeDescription, groupSessionLearnMoreMethodId, learnMoreDescription, differentEmailAddressForNotifications,
				inPersonLocation, registrationEndDateTime, imageFileUploadId);

		addTagsToGroupSession(groupSessionId, tagIds, institutionId);

		// TODO: Remove this once screeningFlowId is wired up
		// Create assessment if there is a screening question
		if (screeningQuestions.size() > 0) {
			UUID assessmentId = createAssessmentForScreeningQuestions(screeningQuestions);
			getDatabase().execute("UPDATE group_session SET assessment_id=? WHERE group_session_id=?", assessmentId, groupSessionId);
		}

		sendAdminNotification(submitterAccount, findGroupSessionById(groupSessionId, account).get());

		return groupSessionId;
	}

	@Nonnull
	protected void addTagsToGroupSession(@Nonnull UUID groupSessionId,
																			 @Nonnull Set<String> tagIds,
																			 @Nonnull InstitutionId institutionId) {
		requireNonNull(groupSessionId);
		requireNonNull(tagIds);
		requireNonNull(institutionId);

		for (String tagId : tagIds) {
			tagId = trimToNull(tagId);

			if (tagId == null)
				continue;

			getDatabase().execute("""
					INSERT INTO tag_group_session (tag_id, institution_id, group_session_id) 
					VALUES (?,?,?)
					""", tagId, institutionId, groupSessionId);
		}
	}

	@Nonnull
	protected <T extends GroupSession> void applyTagsToGroupSession(@Nonnull T groupSession,
																																	@Nonnull InstitutionId institutionId) {
		requireNonNull(groupSession);
		requireNonNull(institutionId);

		groupSession.setTags(getTagService().findTagsByGroupSessionIdAndInstitutionId(groupSession.getGroupSessionId(), institutionId));
	}

	@Nonnull
	protected void applyTagsToGroupSession(@Nonnull List<? extends GroupSession> groupSessions,
																				 @Nonnull InstitutionId institutionId) {
		requireNonNull(groupSessions);
		requireNonNull(institutionId);

		// Pull back all data up-front to avoid N+1 selects
		Map<UUID, List<TagGroupSession>> tagGroupSessionsByContentId = getTagService().findTagGroupSessionsByInstitutionId(institutionId).stream()
				.collect(Collectors.groupingBy(TagGroupSession::getGroupSessionId));
		Map<String, Tag> tagsByTagId = getTagService().findTagsByInstitutionId(institutionId).stream()
				.collect(Collectors.toMap(Tag::getTagId, Function.identity()));

		for (GroupSession groupSession : groupSessions) {
			List<Tag> tags = Collections.emptyList();
			List<TagGroupSession> tagGroupSessions = tagGroupSessionsByContentId.get(groupSession.getGroupSessionId());

			if (tagGroupSessions != null)
				tags = tagGroupSessions.stream()
						.map(tagContent -> tagsByTagId.get(tagContent.getTagId()))
						.collect(Collectors.toList());

			groupSession.setTags(tags);
		}
	}

	@Nonnull
	public Optional<GroupSessionCollection> findGroupSessionCollectionById(@Nullable UUID groupSessionCollectionId) {
		if (groupSessionCollectionId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM group_session_collection WHERE group_session_collection_id = ?",
				GroupSessionCollection.class, groupSessionCollectionId);
	}

	@Nonnull
	public Optional<GroupSessionCollection> findGroupSessionCollectionByInstitutionIdAndUrlName(@Nullable InstitutionId institutionId,
																																															@Nullable String urlName) {
		if (institutionId == null || urlName == null)
			return Optional.empty();

		urlName = urlName.trim();

		return getDatabase().queryForObject("""
				SELECT *
				FROM group_session_collection
				WHERE institution_id=?
				AND url_name=?
				""", GroupSessionCollection.class, institutionId, urlName);
	}

	@Nonnull
	public GroupSessionUrlValidationResult findGroupSessionUrlValidationResults(@Nonnull String urlName,
																																							@Nonnull InstitutionId institutionId,
																																							@Nullable UUID groupSessionId) {
		requireNonNull(urlName);
		requireNonNull(institutionId);

		GroupSessionUrlValidationResult result = new GroupSessionUrlValidationResult();

		boolean urlNameContainsIllegalCharacters = urlNameContainsIllegalCharacters(urlName.trim());
		urlName = normalizeUrlName(urlName).orElse("");

		if (urlNameContainsIllegalCharacters) {
			result.setAvailable(false);
			result.setRecommendation(recommendedUrlNameForUrlName(urlName, institutionId, groupSessionId));
		} else if (!urlNameExistsForInstitutionId(urlName, institutionId, groupSessionId)) {
			result.setAvailable(true);
			result.setRecommendation(urlName);
		} else {
			result.setAvailable(false);
			result.setRecommendation(recommendedUrlNameForUrlName(urlName, institutionId, groupSessionId));
		}

		return result;
	}

	@Nonnull
	protected String recommendedUrlNameForUrlName(@Nonnull String urlName,
																								@Nonnull InstitutionId institutionId,
																								@Nullable UUID groupSessionId) {
		requireNonNull(urlName);
		requireNonNull(institutionId);

		String recommendedUrlName = urlName;
		boolean suggestedUrlAvailable = !urlNameExistsForInstitutionId(recommendedUrlName, institutionId, groupSessionId);
		int urlSuffix = 1;

		while (!suggestedUrlAvailable) {
			recommendedUrlName = format("%s-%s", urlName, urlSuffix);
			suggestedUrlAvailable = !urlNameExistsForInstitutionId(recommendedUrlName, institutionId, groupSessionId);
			urlSuffix++;
		}

		return recommendedUrlName;
	}

	@Nonnull
	protected Optional<String> normalizeUrlName(@Nullable String urlName) {
		urlName = trimToNull(urlName);

		if (urlName == null)
			return Optional.empty();

		return Optional.ofNullable(urlName.toLowerCase(Locale.ENGLISH)
				// All groups of whitespace characters are converted to a single '-'
				.replaceAll("\\p{Zs}+", "-")
				// Anything that's not alphanumeric or a hyphen is discarded
				.replaceAll("[^-\\pL\\pN]", ""));
	}

	@Nonnull
	protected Boolean urlNameContainsIllegalCharacters(@Nonnull String urlName) {
		requireNonNull(urlName);
		// Alphanumerics and hyphens only
		return !urlName.matches("[-\\pL\\pN]+");
	}

	@Nonnull
	protected Boolean urlNameExistsForInstitutionId(@Nonnull String urlName,
																									@Nonnull InstitutionId institutionId,
																									@Nullable UUID groupSessionId) {
		requireNonNull(urlName);
		requireNonNull(institutionId);

		urlName = normalizeUrlName(urlName).orElse("");

		// If this is an illegal name, it already "exists"
		if (getIllegalGroupSessionUrlNames().contains(urlName))
			return true;

		List<Object> parameters = new ArrayList<>();
		StringBuilder query = new StringBuilder("""
				SELECT COUNT(*) > 0
				FROM group_session gs
				WHERE gs.institution_id = ?
				AND LOWER(gs.url_name) = LOWER(?)
				""");

		parameters.add(institutionId);
		parameters.add(urlName);

		if (groupSessionId != null) {
			query.append(" AND gs.group_session_id != ?");
			parameters.add(groupSessionId);
		}

		return getDatabase().queryForObject(query.toString(), Boolean.class, parameters.toArray()).get();
	}

	@Nonnull
	public UUID updateGroupSession(@Nonnull UpdateGroupSessionRequest request,
																 @Nonnull Account account) {
		requireNonNull(request);
		requireNonNull(account);

		UUID groupSessionId = request.getGroupSessionId();
		GroupSessionSchedulingSystemId groupSessionSchedulingSystemId = request.getGroupSessionSchedulingSystemId();
		GroupSessionLocationTypeId groupSessionLocationTypeId = request.getGroupSessionLocationTypeId();
		String title = trimToNull(request.getTitle());
		String description = trimToNull(request.getDescription());
		String urlName = request.getUrlName() == null ? null : normalizeUrlName(request.getUrlName()).orElse(null);
		String inPersonLocation = trimToNull(request.getInPersonLocation());
		UUID facilitatorAccountId = request.getFacilitatorAccountId();
		String facilitatorName = trimToNull(request.getFacilitatorName());
		String facilitatorEmailAddress = trimToNull(request.getFacilitatorEmailAddress());
		String targetEmailAddress = trimToNull(request.getTargetEmailAddress());
		LocalDateTime startDateTime = request.getStartDateTime();
		LocalDateTime endDateTime = request.getEndDateTime();
		Integer seats = request.getSeats();
		String videoconferenceUrl = trimToNull(request.getVideoconferenceUrl());
		List<CreateScreeningQuestionRequest> screeningQuestions = normalizeScreeningQuestions(request.getScreeningQuestions(), request.getScreeningQuestionsV2());
		String confirmationEmailContent = trimToNull(request.getConfirmationEmailContent());
		Boolean sendFollowupEmail = request.getSendFollowupEmail();
		String followupEmailContent = trimToNull(request.getFollowupEmailContent());
		String followupEmailSurveyUrl = trimToNull(request.getFollowupEmailSurveyUrl());
		GroupSession groupSession = findGroupSessionById(groupSessionId, account).get();
		Institution institution = getInstitutionService().findInstitutionById(groupSession.getInstitutionId()).get();
		UUID groupSessionCollectionId = request.getGroupSessionCollectionId();
		Boolean visibleFlag = request.getVisibleFlag();
		UUID screeningFlowId = request.getScreeningFlowId();
		Boolean sendReminderEmail = request.getSendReminderEmail();
		String reminderEmailContent = trimToNull(request.getReminderEmailContent());
		LocalTime followupTimeOfDay = request.getFollowupTimeOfDay();
		Integer followupDayOffset = request.getFollowupDayOffset();
		Boolean singleSessionFlag = request.getSingleSessionFlag();
		String dateTimeDescription = trimToNull(request.getDateTimeDescription());
		Set<String> tagIds = request.getTagIds() == null ? Set.of() : request.getTagIds();
		String learnMoreDescription = trimToNull(request.getLearnMoreDescription());
		GroupSessionLearnMoreMethodId groupSessionLearnMoreMethodId = request.getGroupSessionLearnMoreMethodId();
		Boolean differentEmailAddressForNotifications = request.getDifferentEmailAddressForNotifications();
		LocalDateTime registrationEndDateTime = request.getRegistrationEndDateTime();
		UUID imageFileUploadId = request.getImageFileUploadId();

		// Updates are restricted to certain fields if there are reservations already made for this session
		int reservationCount = findGroupSessionReservationsByGroupSessionId(groupSessionId).size();
		boolean restrictedUpdate = reservationCount > 0;

		ValidationException validationException = new ValidationException();

		if (sendFollowupEmail == null)
			sendFollowupEmail = false;

		if (groupSessionSchedulingSystemId == null)
			groupSessionSchedulingSystemId = GroupSessionSchedulingSystemId.COBALT;

		if (sendReminderEmail == null)
			sendReminderEmail = false;

		if (visibleFlag == null)
			visibleFlag = true;

		if (singleSessionFlag == null)
			singleSessionFlag = true;

		if (differentEmailAddressForNotifications == null)
			differentEmailAddressForNotifications = false;

		if (!differentEmailAddressForNotifications)
			targetEmailAddress = facilitatorEmailAddress;

		if (title == null)
			validationException.add(new FieldError("title", getStrings().get("Title is required.")));

		// If a non-administrator is updating this content, sanitize its HTML
		if (description != null && account.getRoleId() != RoleId.ADMINISTRATOR)
			description = trimToNull(getHtmlSanitizingPolicyFactory().sanitize(description));

		if (description == null)
			validationException.add(new FieldError("description", getStrings().get("Description is required.")));

		if (urlName == null)
			validationException.add(new FieldError("urlName", getStrings().get("Friendly URL name is required.")));
		else if (!isValidUrlSubdirectory(urlName))
			validationException.add(new FieldError("urlName", getStrings().get("Not a valid Friendly URL")));
		else if (urlNameExistsForInstitutionId(urlName, institution.getInstitutionId(), groupSessionId))
			validationException.add(new FieldError("urlName", getStrings().get("Friendly URL name is already in use.")));

		if (facilitatorName == null)
			validationException.add(new FieldError("facilitatorName", getStrings().get("Facilitator name is required.")));

		if (facilitatorEmailAddress == null)
			validationException.add(new FieldError("facilitatorEmailAddress", getStrings().get("Facilitator email address is required.")));
		else if (!isValidEmailAddress(facilitatorEmailAddress))
			validationException.add(new FieldError("facilitatorEmailAddress", getStrings().get("Facilitator email address is invalid.")));

		LocalDateTime currentLocalDateTime = LocalDateTime.now(institution.getTimeZone());

		if (startDateTime == null) {
			validationException.add(new FieldError("startDateTime", getStrings().get("Start time is required.")));
		} else {
			if (singleSessionFlag && currentLocalDateTime.isAfter(startDateTime))
				validationException.add(new FieldError("startDateTime", getStrings().get("Start time must be in the future.")));
		}

		if (endDateTime == null) {
			validationException.add(new FieldError("endDateTime", getStrings().get("End time is required.")));
		} else {
			if (!singleSessionFlag && currentLocalDateTime.toLocalDate().isAfter(endDateTime.toLocalDate()))
				validationException.add(new FieldError("endDateTime", getStrings().get("End date must be in the future.")));
		}

		if (groupSessionLocationTypeId == null) {
			validationException.add(new FieldError("groupSessionLocationTypeId", getStrings().get("Location type is required.")));
		} else if (groupSessionSchedulingSystemId == GroupSessionSchedulingSystemId.COBALT && groupSessionLocationTypeId == GroupSessionLocationTypeId.VIRTUAL && videoconferenceUrl == null) {
			validationException.add(new FieldError("videoconferenceUrl", getStrings().get("Videoconference URL is required for virtual group sessions.")));
		} else if (groupSessionSchedulingSystemId == GroupSessionSchedulingSystemId.COBALT && groupSessionLocationTypeId == GroupSessionLocationTypeId.IN_PERSON && inPersonLocation == null) {
			validationException.add(new FieldError("inPersonLocation", getStrings().get("A location is required for in-person group sessions.")));
		}

		if (startDateTime != null && endDateTime != null) {
			if (endDateTime.equals(startDateTime) || endDateTime.isBefore(startDateTime))
				validationException.add(getStrings().get("End time cannot be before start time."));
		}

		if (groupSessionSchedulingSystemId == GroupSessionSchedulingSystemId.COBALT) {
			if (seats != null && seats < reservationCount)
				validationException.add(new FieldError("seats", getStrings().get("The number of permitted attendees cannot be less than the current number of registrants.")));

			if (sendReminderEmail)
				if (reminderEmailContent == null)
					validationException.add(new FieldError("reminderEmailContent", getStrings().get("Reminder Email Text is required.")));

			if (sendFollowupEmail) {
				if (followupEmailContent == null)
					validationException.add(new FieldError("followupEmailContent", getStrings().get("Follow-up Email Text is required.")));
				if (followupDayOffset == null)
					validationException.add(new FieldError("followupDayOffset", getStrings().get("Number of days after session is required.")));
				if (followupTimeOfDay == null)
					validationException.add(new FieldError("followupTimeOfDay", getStrings().get("Time is required.")));
			}

			if (!singleSessionFlag)
				validationException.add(new FieldError("singleSessionFlag", getStrings().get("Ongoing series is only permitted for external Group Sessions.")));
		} else if (groupSessionSchedulingSystemId == GroupSessionSchedulingSystemId.EXTERNAL) {
			videoconferenceUrl = null;
			confirmationEmailContent = null;

			if (sendReminderEmail)
				validationException.add(new FieldError("sendReminderEmail", getStrings().get("Cannot send reminder emails for external sessions.")));
			if (sendFollowupEmail)
				validationException.add(new FieldError("sendFollowupEmail", getStrings().get("Cannot send follow-up emails for external sessions.")));
		} else if (groupSessionSchedulingSystemId == null) {
			validationException.add(new FieldError("groupSessionSchedulingSystemId", getStrings().get("Group session scheduling system is required.")));
		} else {
			throw new UnsupportedOperationException(format("Not sure what to do with %s.%s", GroupSessionSchedulingSystemId.class.getSimpleName(), groupSessionSchedulingSystemId.name()));
		}

		if (videoconferenceUrl != null && !isValidUrl(videoconferenceUrl))
			validationException.add(new FieldError("videoconferenceUrl", getStrings().get("Videoconference URL is invalid.")));

		if (groupSessionCollectionId != null) {
			GroupSessionCollection groupSessionCollection = findGroupSessionCollectionById(groupSessionCollectionId).orElse(null);
			if (groupSessionCollection == null)
				validationException.add(new FieldError("groupSessionCollectionId", getStrings().get("GroupSessionCollectionId is invalid.")));
		}

		if (screeningFlowId != null) {
			Optional<ScreeningFlow> screeningFlow = getScreeningService().findScreeningFlowById(screeningFlowId);
			if (!screeningFlow.isPresent())
				validationException.add(new FieldError("screeningFlowId", getStrings().get("ScreeningFlowId is invalid.")));
		}

		if (groupSessionLearnMoreMethodId != null && learnMoreDescription == null)
			validationException.add(new FieldError("learnMoreDescription", getStrings().get("A way to learn more is required")));

		if (validationException.hasErrors())
			throw validationException;

		if (imageFileUploadId == null)
			imageFileUploadId = getDefaultGroupSessionImageFileUploadId();

		// Never makes sense to specify a videoconference URL in these scenarios
		if (groupSessionLocationTypeId == GroupSessionLocationTypeId.IN_PERSON
				|| groupSessionSchedulingSystemId == GroupSessionSchedulingSystemId.EXTERNAL)
			videoconferenceUrl = null;

		targetEmailAddress = getNormalizer().normalizeEmailAddress(targetEmailAddress).get();
		facilitatorEmailAddress = getNormalizer().normalizeEmailAddress(facilitatorEmailAddress).get();

		if (restrictedUpdate) {
			getDatabase().execute("""
							UPDATE group_session SET description=?, facilitator_account_id=?, facilitator_name=?, facilitator_email_address=?,
							target_email_address=?, videoconference_url=?, seats=?,
							confirmation_email_content=?, send_followup_email=?, followup_email_content=?, followup_email_survey_url=?,
							group_session_collection_id=?, visible_flag=?, screening_flow_id=?, send_reminder_email=?, reminder_email_content=?,
							followup_time_of_day=?, followup_day_offset=?, single_session_flag=?, date_time_description=?, 
							group_session_learn_more_method_id=?, learn_more_description=?, different_email_address_for_notifications=?,
							group_session_location_type_id=?, in_person_location=?, registration_end_date_time=?, image_file_upload_id=?
							WHERE group_session_id=?
							""", description, facilitatorAccountId, facilitatorName, facilitatorEmailAddress,
					targetEmailAddress, videoconferenceUrl, seats, confirmationEmailContent,
					sendFollowupEmail, followupEmailContent, followupEmailSurveyUrl, groupSessionCollectionId, visibleFlag, screeningFlowId,
					sendReminderEmail, reminderEmailContent, followupTimeOfDay, followupDayOffset, singleSessionFlag, dateTimeDescription,
					groupSessionLearnMoreMethodId, learnMoreDescription, differentEmailAddressForNotifications, groupSessionLocationTypeId,
					inPersonLocation, registrationEndDateTime, imageFileUploadId, groupSessionId);
		} else {
			getDatabase().execute("""
							UPDATE group_session SET title=?, description=?, facilitator_account_id=?, facilitator_name=?, facilitator_email_address=?,
							target_email_address=?, videoconference_url=?, start_date_time=?, end_date_time=?, seats=?, url_name=?,
							confirmation_email_content=?, group_session_scheduling_system_id=?, send_followup_email=?, followup_email_content=?, followup_email_survey_url=?,
							group_session_collection_id=?, visible_flag=?, screening_flow_id=?, send_reminder_email=?, reminder_email_content=?,
							followup_time_of_day=?, followup_day_offset=?, single_session_flag=?, date_time_description=?,
							group_session_learn_more_method_id=?, learn_more_description=?, different_email_address_for_notifications=?,
							group_session_location_type_id=?, in_person_location=?, registration_end_date_time=?, image_file_upload_id=?
							WHERE group_session_id=?
							""",
					title, description, facilitatorAccountId, facilitatorName, facilitatorEmailAddress,
					targetEmailAddress, videoconferenceUrl, startDateTime, endDateTime, seats, urlName, confirmationEmailContent,
					groupSessionSchedulingSystemId, sendFollowupEmail, followupEmailContent, followupEmailSurveyUrl,
					groupSessionCollectionId, visibleFlag, screeningFlowId, sendReminderEmail, reminderEmailContent,
					followupTimeOfDay, followupDayOffset, singleSessionFlag, dateTimeDescription,
					groupSessionLearnMoreMethodId, learnMoreDescription, differentEmailAddressForNotifications, groupSessionLocationTypeId,
					inPersonLocation, registrationEndDateTime, imageFileUploadId, groupSessionId);

			List<Question> existingScreeningQuestions = findScreeningQuestionsByGroupSessionId(groupSessionId);
			boolean screeningQuestionsChanged = false;

			if (existingScreeningQuestions.size() != screeningQuestions.size()) {
				screeningQuestionsChanged = true;
			} else {
				for (int i = 0; i < screeningQuestions.size(); ++i) {
					CreateScreeningQuestionRequest currentScreeningQuestion = screeningQuestions.get(i);
					Question existingScreeningQuestion = existingScreeningQuestions.get(i);

					boolean questionsMatch = Objects.equals(currentScreeningQuestion.getQuestion(), existingScreeningQuestion.getQuestionText());
					boolean fontSizesMatch = Objects.equals(currentScreeningQuestion.getFontSizeId(), existingScreeningQuestion.getFontSizeId());

					if (!questionsMatch || !fontSizesMatch) {
						screeningQuestionsChanged = true;
						break;
					}
				}
			}

			// Create assessment if there is a different screening question
			if (screeningQuestionsChanged) {
				UUID assessmentId = createAssessmentForScreeningQuestions(screeningQuestions);
				getDatabase().execute("UPDATE group_session SET assessment_id=? WHERE group_session_id=?", assessmentId, groupSessionId);
			}
		}

		//Remove any tags for this Group Session and add whatever tags we have for the update
		getDatabase().execute("DELETE FROM tag_group_session WHERE group_session_id=?", groupSessionId);
		addTagsToGroupSession(groupSessionId, tagIds, institution.getInstitutionId());

		cancelAndResendAllScheduledMessagesForGroupSessionId(groupSessionId, account);

		return groupSessionId;
	}

	protected void cancelAllScheduledMessagesForGroupSessionId(@Nullable UUID groupSessionId) {
		if (groupSessionId == null)
			return;

		List<GroupSessionReservation> groupSessionReservations = findGroupSessionReservationsByGroupSessionId(groupSessionId);

		getLogger().info("Canceling scheduled messages for {} group session reservations for group session ID {}...", groupSessionReservations.size(), groupSessionId);

		for (GroupSessionReservation groupSessionReservation : groupSessionReservations) {
			getMessageService().cancelScheduledMessage(groupSessionReservation.getAttendeeReminderScheduledMessageId());
			getMessageService().cancelScheduledMessage(groupSessionReservation.getAttendeeFollowupScheduledMessageId());
		}
	}

	protected void cancelAndResendAllScheduledMessagesForGroupSessionId(@Nullable UUID groupSessionId, @Nonnull Account account) {
		if (groupSessionId == null || account == null)
			return;

		// After an update to the group session, rework all the scheduled messages to take any changes into account
		List<GroupSessionReservation> groupSessionReservations = findGroupSessionReservationsByGroupSessionId(groupSessionId);
		GroupSession groupSession = findGroupSessionById(groupSessionId, account).get();

		getLogger().info("Re-sending scheduled messages for {} group session reservations for group session ID {}...", groupSessionReservations.size(), groupSessionId);

		for (GroupSessionReservation groupSessionReservation : groupSessionReservations) {
			// First, cancel anything that might have already been scheduled
			getMessageService().cancelScheduledMessage(groupSessionReservation.getAttendeeReminderScheduledMessageId());
			getMessageService().cancelScheduledMessage(groupSessionReservation.getAttendeeFollowupScheduledMessageId());

			// Next, reschedule as needed
			if (groupSession.getSendReminderEmail())
				scheduleGroupSessionReservationReminderMessage(groupSession, groupSessionReservation);

			if (groupSession.getSendFollowupEmail())
				scheduleGroupSessionReservationFollowupMessage(groupSession, groupSessionReservation);
		}
	}

	@Nonnull
	protected UUID createAssessmentForScreeningQuestions(@Nonnull List<CreateScreeningQuestionRequest> screeningQuestions) {
		requireNonNull(screeningQuestions);

		UUID assessmentId = UUID.randomUUID();

		getDatabase().execute("INSERT INTO assessment (assessment_id, assessment_type_id, " +
				"minimum_eligibility_score, answers_may_contain_pii) VALUES (?,?,?,?)", assessmentId, AssessmentTypeId.INTAKE, screeningQuestions.size(), false);

		UUID mostRecentQuestionId = null;

		// Walk in reverse order since each question's "yes" answer needs to know the _next_ question to point to
		for (int i = screeningQuestions.size() - 1; i >= 0; --i) {
			String screeningQuestion = screeningQuestions.get(i).getQuestion();
			FontSizeId fontSizeId = screeningQuestions.get(i).getFontSizeId();
			UUID nextQuestionId = mostRecentQuestionId;
			UUID answerYesId = UUID.randomUUID();
			UUID answerNoId = UUID.randomUUID();

			mostRecentQuestionId = UUID.randomUUID();

			// Careful: display order must start at 1, not 0
			getDatabase().execute("INSERT INTO question (question_id, assessment_id, question_type_id, font_size_id, " +
					"question_text, display_order) VALUES (?,?,?,?,?,?)", mostRecentQuestionId, assessmentId, QuestionTypeId.QUAD, fontSizeId, screeningQuestion, i + 1);

			getDatabase().execute("INSERT INTO answer (answer_id, question_id, answer_text, display_order, " +
					"answer_value, next_question_id) VALUES (?,?,?,?,?,?)", answerYesId, mostRecentQuestionId, getStrings().get("Yes"), 1, 1, nextQuestionId);

			getDatabase().execute("INSERT INTO answer (answer_id, question_id, answer_text, display_order, " +
					"answer_value, next_question_id) VALUES (?,?,?,?,?,?)", answerNoId, mostRecentQuestionId, getStrings().get("No"), 2, 0, null);
		}

		return assessmentId;
	}

	@Nonnull
	protected List<CreateScreeningQuestionRequest> normalizeScreeningQuestions(@Nullable List<String> screeningQuestions,
																																						 @Nullable List<CreateScreeningQuestionRequest> screeningQuestionsV2) {
		if (screeningQuestions == null && screeningQuestionsV2 == null)
			return Collections.emptyList();

		// We take both screeningQuestions (deprecated) and screeningQuestionsV2 (current) as input.
		// We normalize screeningQuestions to List<CreateScreeningQuestionRequest> so other code can work with one standard construct.
		// This allows for backwards compatibility.

		List<CreateScreeningQuestionRequest> normalizedScreeningQuestions = new ArrayList<>();

		if (screeningQuestionsV2 != null) {
			for (CreateScreeningQuestionRequest screeningQuestion : screeningQuestionsV2) {
				String question = trimToNull(screeningQuestion.getQuestion());

				if (question != null) {
					screeningQuestion.setQuestion(question);

					if (screeningQuestion.getFontSizeId() == null)
						screeningQuestion.setFontSizeId(FontSizeId.DEFAULT);

					normalizedScreeningQuestions.add(screeningQuestion);
				}
			}
		} else if (screeningQuestions != null) {
			for (String screeningQuestion : screeningQuestions) {
				screeningQuestion = trimToNull(screeningQuestion);

				if (screeningQuestion != null) {
					CreateScreeningQuestionRequest request = new CreateScreeningQuestionRequest();
					request.setQuestion(screeningQuestion);
					request.setFontSizeId(FontSizeId.DEFAULT);
					normalizedScreeningQuestions.add(request);
				}
			}
		}

		return normalizedScreeningQuestions;
	}

	@Nonnull
	public Boolean updateGroupSessionStatus(@Nonnull UpdateGroupSessionStatusRequest request, @Nonnull Account account) {
		requireNonNull(request);
		requireNonNull(account);

		UUID groupSessionId = request.getGroupSessionId();
		UUID accountId = request.getAccountId();
		GroupSessionStatusId groupSessionStatusId = request.getGroupSessionStatusId();
		GroupSession groupSession = null;
		ValidationException validationException = new ValidationException();

		if (groupSessionId == null) {
			validationException.add(new FieldError("groupSessionId", getStrings().get("Group Session ID is required.")));
		} else {
			groupSession = findGroupSessionById(groupSessionId, account).orElse(null);

			if (groupSession == null)
				validationException.add(new FieldError("groupSessionId", getStrings().get("Invalid Group Session.")));
		}

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (groupSessionStatusId == null)
			validationException.add(new FieldError("groupSessionStatusId", getStrings().get("Group Session Status ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		// No-op if status is not changing
		if (groupSessionStatusId == groupSession.getGroupSessionStatusId())
			return false;

		boolean succeeded = getDatabase().execute("UPDATE group_session SET group_session_status_id=? WHERE group_session_id=?",
				groupSessionStatusId, groupSessionId) > 0;

		if (groupSessionStatusId == GroupSessionStatusId.CANCELED) {
			GroupSession pinnedGroupSession = groupSession;
			List<GroupSessionReservation> reservations = findGroupSessionReservationsByGroupSessionId(groupSessionId);

			cancelAllScheduledMessagesForGroupSessionId(groupSessionId);

			// Cancel all the reservations...
			getDatabase().execute("UPDATE group_session_reservation SET canceled=TRUE WHERE group_session_id=?", groupSessionId);

			// ... and then, after committing, email everyone
			getDatabase().currentTransaction().get().addPostCommitOperation(() -> {
				for (GroupSessionReservation reservation : reservations) {
					Account attendeeAccount = getAccountService().findAccountById(reservation.getAccountId()).get();
					String attendeeName = Normalizer.normalizeName(attendeeAccount.getFirstName(), attendeeAccount.getLastName()).orElse(getStrings().get("Anonymous User"));

					EmailMessage attendeeEmailMessage = new EmailMessage.Builder(attendeeAccount.getInstitutionId(), EmailMessageTemplate.GROUP_SESSION_CANCELED, attendeeAccount.getLocale())
							.toAddresses(new ArrayList<>() {{
								add(attendeeAccount.getEmailAddress());
							}})
							.replyToAddress(pinnedGroupSession.getTargetEmailAddress())
							.messageContext(new HashMap<String, Object>() {{
								put("groupSession", pinnedGroupSession);
								put("imageUrl", firstNonNull(findFileUploadUrlByForGroupSession(pinnedGroupSession), getConfiguration().getDefaultGroupSessionImageUrlForEmail()));
								put("attendeeName", attendeeName);
								put("groupSessionStartDateDescription", getFormatter().formatDate(pinnedGroupSession.getStartDateTime().toLocalDate()));
								put("groupSessionStartTimeDescription", getFormatter().formatTime(pinnedGroupSession.getStartDateTime().toLocalTime(), FormatStyle.SHORT));
								put("anotherTimeUrl", format("%s/in-the-studio", getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(pinnedGroupSession.getInstitutionId(), UserExperienceTypeId.PATIENT).get()));
								put(EmailMessageContextKey.OVERRIDE_PLATFORM_NAME.name(), pinnedGroupSession.getOverridePlatformName());
								put(EmailMessageContextKey.OVERRIDE_PLATFORM_EMAIL_IMAGE_URL.name(), pinnedGroupSession.getOverridePlatformEmailImageUrl());
								put(EmailMessageContextKey.OVERRIDE_PLATFORM_SUPPORT_EMAIL_ADDRESS.name(), pinnedGroupSession.getOverridePlatformSupportEmailAddress());
							}})
							.build();

					getMessageService().enqueueMessage(attendeeEmailMessage);
				}
			});
		} else if (groupSessionStatusId == GroupSessionStatusId.ADDED) {
			// After committing a change to ADDED status, email the submitter if it's a different account)
			GroupSession pinnedGroupSession = groupSession;

			if (!Objects.equals(accountId, groupSession.getSubmitterAccountId())) {
				getDatabase().currentTransaction().get().addPostCommitOperation(() -> {
					Account submitterAccount = getAccountService().findAccountById(pinnedGroupSession.getSubmitterAccountId()).get();

					EmailMessage attendeeEmailMessage = new EmailMessage.Builder(submitterAccount.getInstitutionId(), EmailMessageTemplate.GROUP_SESSION_LIVE_SUBMITTER, submitterAccount.getLocale())
							.toAddresses(new ArrayList<>() {{
								add(submitterAccount.getEmailAddress());
							}})
							.replyToAddress(pinnedGroupSession.getTargetEmailAddress())
							.messageContext(new HashMap<String, Object>() {{
								put("groupSession", pinnedGroupSession);
								put("imageUrl", firstNonNull(findFileUploadUrlByForGroupSession(pinnedGroupSession), getConfiguration().getDefaultGroupSessionImageUrlForEmail()));
								put("groupSessionUrl", format("%s/in-the-studio", getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(pinnedGroupSession.getInstitutionId(), UserExperienceTypeId.PATIENT).get()));
								put(EmailMessageContextKey.OVERRIDE_PLATFORM_NAME.name(), pinnedGroupSession.getOverridePlatformName());
								put(EmailMessageContextKey.OVERRIDE_PLATFORM_EMAIL_IMAGE_URL.name(), pinnedGroupSession.getOverridePlatformEmailImageUrl());
								put(EmailMessageContextKey.OVERRIDE_PLATFORM_SUPPORT_EMAIL_ADDRESS.name(), pinnedGroupSession.getOverridePlatformSupportEmailAddress());
							}})
							.build();

					getMessageService().enqueueMessage(attendeeEmailMessage);
				});
			}
		}

		return succeeded;
	}


	@Nonnull
	public List<GroupSessionReservation> findGroupSessionReservationsByGroupSessionId(@Nullable UUID groupSessionId) {
		if (groupSessionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT * FROM v_group_session_reservation WHERE group_session_id=? ORDER BY last_updated DESC",
				GroupSessionReservation.class, groupSessionId);
	}

	@Nonnull
	public List<Pair<GroupSession, GroupSessionReservation>> findUpcomingGroupSessionReservationsByAccountId(@Nullable UUID accountId,
																																																					 @Nullable ZoneId timeZone) {
		if (accountId == null || timeZone == null)
			return Collections.emptyList();

		List<GroupSessionReservation> groupSessionReservations = getDatabase().queryForList("SELECT gsr.* FROM v_group_session_reservation gsr, v_group_session gs " +
						"WHERE gs.group_session_id=gsr.group_session_id AND gsr.account_id=? AND gs.start_date_time >= ? " +
						"ORDER BY gs.start_date_time",
				GroupSessionReservation.class, accountId, LocalDateTime.now(timeZone));

		Set<UUID> groupSessionIds = groupSessionReservations.stream()
				.map(groupSessionReservation -> groupSessionReservation.getGroupSessionId())
				.collect(Collectors.toSet());

		List<GroupSession> groupSessions = groupSessionIds.size() == 0 ? Collections.emptyList() : getDatabase().queryForList(format("SELECT * FROM v_group_session WHERE group_session_id IN %s",
				sqlInListPlaceholders(groupSessionIds)), GroupSession.class, sqlVaragsParameters(groupSessionIds));

		Map<UUID, GroupSession> groupSessionsById = new HashMap<>(groupSessions.size());

		for (GroupSession groupSession : groupSessions)
			groupSessionsById.put(groupSession.getGroupSessionId(), groupSession);

		List<Pair<GroupSession, GroupSessionReservation>> groupSessionReservationPairs = new ArrayList<>(groupSessionReservations.size());

		for (GroupSessionReservation groupSessionReservation : groupSessionReservations) {
			GroupSession groupSession = groupSessionsById.get(groupSessionReservation.getGroupSessionId());

			if (groupSession != null)
				groupSessionReservationPairs.add(Pair.of(groupSession, groupSessionReservation));
		}

		return groupSessionReservationPairs;
	}

	@Nonnull
	public Optional<Pair<GroupSession, GroupSessionReservation>> findGroupSessionReservationPairById(@Nullable UUID groupSessionReservationId,
																																																	 @Nullable Account account) {
		if (groupSessionReservationId == null || account == null)
			return Optional.empty();

		GroupSessionReservation groupSessionReservation = getDatabase().queryForObject("SELECT * FROM v_group_session_reservation " +
						"WHERE group_session_reservation_id=?",
				GroupSessionReservation.class, groupSessionReservationId).orElse(null);

		if (groupSessionReservation == null)
			return Optional.empty();

		GroupSession groupSession = findGroupSessionById(groupSessionReservation.getGroupSessionId(), account).orElse(null);

		if (groupSession == null)
			return Optional.empty();

		return Optional.of(Pair.of(groupSession, groupSessionReservation));
	}

	@Nonnull
	public List<Question> findScreeningQuestionsByGroupSessionId(@Nullable UUID groupSessionId) {
		if (groupSessionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("SELECT q.* FROM group_session gs, assessment a, question q " +
				"WHERE gs.group_session_id=? AND gs.assessment_id=a.assessment_id AND q.assessment_id=a.assessment_id " +
				"ORDER BY q.display_order", Question.class, groupSessionId);
	}

	@Nonnull
	public UUID createGroupSessionReservation(@Nonnull CreateGroupSessionReservationRequest request, @Nonnull Account account) {
		requireNonNull(request);
		requireNonNull(account);

		UUID accountId = request.getAccountId();
		String emailAddress = getNormalizer().normalizeEmailAddress(request.getEmailAddress()).orElse(null);
		UUID groupSessionId = request.getGroupSessionId();
		UUID groupSessionReservationId = UUID.randomUUID();
		Account attendeeAccount = null;

		ValidationException validationException = new ValidationException();

		if (accountId == null) {
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));
		} else {
			attendeeAccount = getAccountService().findAccountById(accountId).orElse(null);

			if (attendeeAccount == null)
				validationException.add(new FieldError("accountId", getStrings().get("Account ID is invalid.")));
			else if (attendeeAccount.getEmailAddress() == null && emailAddress == null)
				validationException.add(getStrings().get("You must provide an email address."));
		}

		if (emailAddress != null && !isValidEmailAddress(emailAddress))
			validationException.add(new FieldError("emailAddress", getStrings().get("Email address is invalid.")));

		if (groupSessionId == null) {
			validationException.add(new FieldError("groupSessionId", getStrings().get("Group Session ID is required.")));
		} else {
			GroupSession groupSession = findGroupSessionById(groupSessionId, account).orElse(null);

			if (groupSession == null) {
				validationException.add(new FieldError("groupSessionId", getStrings().get("Group Session ID is invalid.")));
			} else if (groupSession.getGroupSessionSchedulingSystemId() == GroupSessionSchedulingSystemId.COBALT) {
				List<GroupSessionReservation> reservations = findGroupSessionReservationsByGroupSessionId(groupSessionId);

				for (GroupSessionReservation reservation : reservations) {
					if (reservation.getAccountId().equals(accountId)) {
						getLogger().debug("Account ID {} already has an active reservation for Group Session ID {}, not creating...", accountId, groupSessionId);
						return reservation.getGroupSessionReservationId();
					}
				}

				if (groupSession.getSeats() != null && groupSession.getSeatsReserved() >= groupSession.getSeats())
					validationException.add(new FieldError("groupSessionId", getStrings().get("Sorry, this group session is full.")));

				if (groupSession.getGroupSessionStatusId() == GroupSessionStatusId.ARCHIVED)
					validationException.add(new FieldError("groupSessionId", getStrings().get("Sorry, you can't join this group session because it has already ended.")));
				else if (groupSession.getGroupSessionStatusId() == GroupSessionStatusId.CANCELED)
					validationException.add(new FieldError("groupSessionId", getStrings().get("Sorry, you can't join this group session because it was canceled.")));
				else if (groupSession.getGroupSessionStatusId() == GroupSessionStatusId.DELETED)
					validationException.add(new FieldError("groupSessionId", getStrings().get("Sorry, you can't join this group session because it was removed.")));
				else if (groupSession.getGroupSessionStatusId() == GroupSessionStatusId.NEW)
					validationException.add(new FieldError("groupSessionId", getStrings().get("Sorry, you can't join this group session because it is not taking reservations yet.")));
			} else if (groupSession.getGroupSessionSchedulingSystemId() == GroupSessionSchedulingSystemId.EXTERNAL) {
				validationException.add(getStrings().get("You are not permitted to book this group session through Cobalt."));
			} else {
				throw new UnsupportedOperationException(format("Not sure what to do with %s.%s", GroupSessionSchedulingSystemId.class.getSimpleName(), groupSession.getGroupSessionSchedulingSystemId().name()));
			}
		}

		if (validationException.hasErrors())
			throw validationException;

		if (emailAddress != null) {
			getAccountService().updateAccountEmailAddress(new UpdateAccountEmailAddressRequest() {{
				setAccountId(accountId);
				setEmailAddress(emailAddress);
			}});
		}

		getDatabase().execute("INSERT INTO group_session_reservation (group_session_reservation_id, group_session_id, " +
				"account_id) VALUES (?,?,?)", groupSessionReservationId, groupSessionId, accountId);

		String attendeeEmailAddress = emailAddress == null ? attendeeAccount.getEmailAddress() : emailAddress;
		String attendeeName = Normalizer.normalizeName(attendeeAccount.getFirstName(), attendeeAccount.getLastName()).orElse(getStrings().get("Anonymous User"));
		Account pinnedAttendeeAccount = attendeeAccount;

		GroupSession groupSession = findGroupSessionById(groupSessionId, account).get();
		Institution institution = getInstitutionService().findInstitutionById(groupSession.getInstitutionId()).get();

		getDatabase().currentTransaction().get().addPostCommitOperation(() -> {
			GroupSessionReservation groupSessionReservation = findGroupSessionReservationPairById(groupSessionReservationId, account).get().getRight();
			Map<String, Object> attendeeMessageContext = new HashMap<String, Object>() {{
				put("groupSession", groupSession);
				put("imageUrl", firstNonNull((groupSession.getImageFileUploadUrl()), getConfiguration().getDefaultGroupSessionImageUrlForEmail()));
				put("attendeeName", attendeeName);
				put("groupSessionStartDateDescription", getFormatter().formatDate(groupSession.getStartDateTime().toLocalDate()));
				put("groupSessionStartTimeDescription", getFormatter().formatTime(groupSession.getStartDateTime().toLocalTime(), FormatStyle.SHORT));
				put("groupSessionEndDateDescription", getFormatter().formatDate(groupSession.getEndDateTime().toLocalDate()));
				put("groupSessionEndTimeDescription", getFormatter().formatTime(groupSession.getEndDateTime().toLocalTime(), FormatStyle.SHORT));
				put("cancelUrl", format("%s/my-calendar?groupSessionReservationId=%s&action=cancel", getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institution.getInstitutionId(), UserExperienceTypeId.PATIENT).get(), groupSessionReservationId));
				put("icalUrl", format("%s/group-session-reservations/%s/ical", getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institution.getInstitutionId(), UserExperienceTypeId.PATIENT).get(), groupSessionReservationId));
				put("googleCalendarUrl", format("%s/group-session-reservations/%s/google-calendar", getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institution.getInstitutionId(), UserExperienceTypeId.PATIENT).get(), groupSessionReservationId));
				put("anotherTimeUrl", format("%s/in-the-studio", getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institution.getInstitutionId(), UserExperienceTypeId.PATIENT).get()));
				put(EmailMessageContextKey.OVERRIDE_PLATFORM_NAME.name(), groupSession.getOverridePlatformName());
				put(EmailMessageContextKey.OVERRIDE_PLATFORM_EMAIL_IMAGE_URL.name(), groupSession.getOverridePlatformEmailImageUrl());
				put(EmailMessageContextKey.OVERRIDE_PLATFORM_SUPPORT_EMAIL_ADDRESS.name(), groupSession.getOverridePlatformSupportEmailAddress());
			}};

			EmailMessage attendeeEmailMessage = new EmailMessage.Builder(groupSession.getInstitutionId(), EmailMessageTemplate.GROUP_SESSION_RESERVATION_CREATED_ATTENDEE, pinnedAttendeeAccount.getLocale())
					.toAddresses(new ArrayList<>() {{
						add(attendeeEmailAddress);
					}})
					.replyToAddress(groupSession.getTargetEmailAddress())
					.messageContext(attendeeMessageContext)
					.emailAttachments(List.of(generateICalInviteAsEmailAttachment(groupSession, groupSessionReservation, InviteMethod.REQUEST)))
					.build();

			getMessageService().enqueueMessage(attendeeEmailMessage);

			// Schedule a reminder message for the group session reservation
			if (groupSession.getSendReminderEmail())
				scheduleGroupSessionReservationReminderMessage(groupSession, groupSessionReservation);

			// Schedule a followup message, if applicable
			if (groupSession.getSendFollowupEmail())
				scheduleGroupSessionReservationFollowupMessage(groupSession, groupSessionReservation);

			EmailMessage facilitatorEmailMessage = new EmailMessage.Builder(groupSession.getInstitutionId(), EmailMessageTemplate.GROUP_SESSION_RESERVATION_CREATED_FACILITATOR, institution.getLocale())
					.toAddresses(new ArrayList<>() {{
						add(groupSession.getTargetEmailAddress());
					}})
					.replyToAddress(replyToAddressForEmailsTargetingFacilitator(groupSession))
					.messageContext(new HashMap<String, Object>() {{
						put("groupSession", groupSession);
						put("imageUrl", firstNonNull(findFileUploadUrlByForGroupSession(groupSession), getConfiguration().getDefaultGroupSessionImageUrlForEmail()));
						put("facilitatorName", groupSession.getFacilitatorName());
						put("attendeeName", attendeeName);
						put("groupSessionUrl", format("%s/admin/group-sessions/edit/%s", getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institution.getInstitutionId(), UserExperienceTypeId.STAFF).get(), groupSession.getGroupSessionId()));
						put("attendeeEmailAddress", attendeeEmailAddress);
						put("groupSessionStartDateDescription", getFormatter().formatDate(groupSession.getStartDateTime().toLocalDate()));
						put("groupSessionStartTimeDescription", getFormatter().formatTime(groupSession.getStartDateTime().toLocalTime(), FormatStyle.SHORT));
						put(EmailMessageContextKey.OVERRIDE_PLATFORM_NAME.name(), groupSession.getOverridePlatformName());
						put(EmailMessageContextKey.OVERRIDE_PLATFORM_EMAIL_IMAGE_URL.name(), groupSession.getOverridePlatformEmailImageUrl());
						put(EmailMessageContextKey.OVERRIDE_PLATFORM_SUPPORT_EMAIL_ADDRESS.name(), groupSession.getOverridePlatformSupportEmailAddress());
					}})
					.build();

			getMessageService().enqueueMessage(facilitatorEmailMessage);
		});

		return groupSessionReservationId;
	}

	@Nonnull
	protected String replyToAddressForEmailsTargetingFacilitator(@Nonnull GroupSession groupSession) {
		requireNonNull(groupSession);
		// TODO: institution-specific support once we see how this goes
		return "support@cobaltinnovations.org";
	}

	@Nonnull
	protected String replyToAddressForEmailsTargetingFacilitator(@Nonnull GroupSessionRequest groupSessionRequest) {
		requireNonNull(groupSessionRequest);
		// TODO: institution-specific support once we see how this goes
		return "support@cobaltinnovations.org";
	}

	@Nonnull
	protected void scheduleGroupSessionReservationReminderMessage(@Nonnull GroupSession groupSession,
																																@Nonnull GroupSessionReservation groupSessionReservation) {
		requireNonNull(groupSession);
		requireNonNull(groupSessionReservation);

		Account attendeeAccount = getAccountService().findAccountById(groupSessionReservation.getAccountId()).get();

		String attendeeEmailAddress = attendeeAccount.getEmailAddress();
		String attendeeName = Normalizer.normalizeName(attendeeAccount.getFirstName(), attendeeAccount.getLastName()).orElse(getStrings().get("Anonymous User"));
		Account pinnedAttendeeAccount = attendeeAccount;

		Institution institution = getInstitutionService().findInstitutionById(groupSession.getInstitutionId()).get();

		Map<String, Object> attendeeMessageContext = new HashMap<String, Object>() {{
			put("groupSession", groupSession);
			put("imageUrl", firstNonNull(findFileUploadUrlByForGroupSession(groupSession), getConfiguration().getDefaultGroupSessionImageUrlForEmail()));
			put("attendeeName", attendeeName);
			put("groupSessionTitle", groupSession.getTitle());
			put("groupSessionStartDateDescription", getFormatter().formatDate(groupSession.getStartDateTime().toLocalDate()));
			put("groupSessionStartTimeDescription", getFormatter().formatTime(groupSession.getStartDateTime().toLocalTime(), FormatStyle.SHORT));
			put("groupSessionEndDateDescription", getFormatter().formatDate(groupSession.getEndDateTime().toLocalDate()));
			put("groupSessionEndTimeDescription", getFormatter().formatTime(groupSession.getEndDateTime().toLocalTime(), FormatStyle.SHORT));
			put("cancelUrl", format("%s/my-calendar?groupSessionReservationId=%s&action=cancel", getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institution.getInstitutionId(), UserExperienceTypeId.PATIENT).get(), groupSessionReservation.getGroupSessionReservationId()));
			put("icalUrl", format("%s/group-session-reservations/%s/ical", getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institution.getInstitutionId(), UserExperienceTypeId.PATIENT).get(), groupSessionReservation.getGroupSessionReservationId()));
			put("googleCalendarUrl", format("%s/group-session-reservations/%s/google-calendar", getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institution.getInstitutionId(), UserExperienceTypeId.PATIENT).get(), groupSessionReservation.getGroupSessionReservationId()));
			put("anotherTimeUrl", format("%s/in-the-studio", getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institution.getInstitutionId(), UserExperienceTypeId.PATIENT).get()));
			put(EmailMessageContextKey.OVERRIDE_PLATFORM_NAME.name(), groupSession.getOverridePlatformName());
			put(EmailMessageContextKey.OVERRIDE_PLATFORM_EMAIL_IMAGE_URL.name(), groupSession.getOverridePlatformEmailImageUrl());
			put(EmailMessageContextKey.OVERRIDE_PLATFORM_SUPPORT_EMAIL_ADDRESS.name(), groupSession.getOverridePlatformSupportEmailAddress());
		}};

		// Schedule a reminder message for this reservation based on institution rules
		LocalDateTime reminderMessageDateTime = groupSession.getStartDateTime().minusMinutes(institution.getGroupSessionReservationDefaultReminderMinutesOffset());

		EmailMessage attendeeReminderEmailMessage = new EmailMessage.Builder(groupSession.getInstitutionId(), EmailMessageTemplate.GROUP_SESSION_RESERVATION_REMINDER_ATTENDEE, pinnedAttendeeAccount.getLocale())
				.toAddresses(Collections.singletonList(attendeeEmailAddress))
				.replyToAddress(groupSession.getTargetEmailAddress())
				.messageContext(attendeeMessageContext)
				.build();

		UUID attendeeReminderScheduledMessageId = getMessageService().createScheduledMessage(new CreateScheduledMessageRequest<>() {{
			setMetadata(Map.of("groupSessionReservationId", groupSessionReservation.getGroupSessionReservationId()));
			setMessage(attendeeReminderEmailMessage);
			setTimeZone(institution.getTimeZone());
			setScheduledAt(reminderMessageDateTime);
		}});

		getDatabase().execute("""
				UPDATE group_session_reservation
				SET attendee_reminder_scheduled_message_id=? 
				WHERE group_session_reservation_id=?
				""", attendeeReminderScheduledMessageId, groupSessionReservation.getGroupSessionReservationId());
	}

	@Nonnull
	protected Boolean scheduleGroupSessionReservationFollowupMessage(@Nonnull GroupSession groupSession,
																																	 @Nonnull GroupSessionReservation groupSessionReservation) {
		requireNonNull(groupSession);
		requireNonNull(groupSessionReservation);

		if (!groupSession.getSendFollowupEmail())
			return false;

		Account account = getAccountService().findAccountById(groupSessionReservation.getAccountId()).get();
		Institution institution = getInstitutionService().findInstitutionById(groupSession.getInstitutionId()).get();

		Map<String, Object> messageContext = new HashMap<>();
		messageContext.put("groupSession", groupSession);
		messageContext.put("imageUrl", firstNonNull(findFileUploadUrlByForGroupSession(groupSession), getConfiguration().getDefaultGroupSessionImageUrlForEmail()));
		messageContext.put(EmailMessageContextKey.OVERRIDE_PLATFORM_NAME.name(), groupSession.getOverridePlatformName());
		messageContext.put(EmailMessageContextKey.OVERRIDE_PLATFORM_EMAIL_IMAGE_URL.name(), groupSession.getOverridePlatformEmailImageUrl());
		messageContext.put(EmailMessageContextKey.OVERRIDE_PLATFORM_SUPPORT_EMAIL_ADDRESS.name(), groupSession.getOverridePlatformSupportEmailAddress());

		EmailMessage followupEmailMessage = new EmailMessage.Builder(groupSession.getInstitutionId(), EmailMessageTemplate.GROUP_SESSION_RESERVATION_FOLLOWUP_ATTENDEE, account.getLocale())
				.toAddresses(new ArrayList<>() {{
					add(account.getEmailAddress());
				}})
				.replyToAddress(groupSession.getTargetEmailAddress())
				.messageContext(messageContext)
				.build();

		// Schedule a followup message for this reservation based on group session parameters
		LocalDate followupMessageDate = groupSession.getStartDateTime().toLocalDate().plusDays(groupSession.getFollowupDayOffset());
		LocalTime followupMessageTimeOfDay = groupSession.getFollowupTimeOfDay();

		UUID attendeeFollowupScheduledMessageId = getMessageService().createScheduledMessage(new CreateScheduledMessageRequest<>() {{
			setMetadata(Map.of("groupSessionReservationId", groupSessionReservation.getGroupSessionReservationId()));
			setMessage(followupEmailMessage);
			setTimeZone(institution.getTimeZone());
			setScheduledAt(LocalDateTime.of(followupMessageDate, followupMessageTimeOfDay));
		}});

		getDatabase().execute("""
				UPDATE group_session_reservation
				SET attendee_followup_scheduled_message_id=? 
				WHERE group_session_reservation_id=?
				""", attendeeFollowupScheduledMessageId, groupSessionReservation.getGroupSessionReservationId());

		return true;
	}

	@Nonnull
	public Boolean cancelGroupSessionReservation(@Nonnull CancelGroupSessionReservationRequest request, @Nonnull Account account) {
		requireNonNull(request);
		requireNonNull(account);

		UUID groupSessionReservationId = request.getGroupSessionReservationId();
		Pair<GroupSession, GroupSessionReservation> groupSessionReservationPair = null;
		ValidationException validationException = new ValidationException();

		if (groupSessionReservationId == null) {
			validationException.add(new FieldError("groupSessionReservationId", getStrings().get("Group Session Reservation ID is required.")));
		} else {
			groupSessionReservationPair = findGroupSessionReservationPairById(groupSessionReservationId, account).orElse(null);

			if (groupSessionReservationPair == null)
				validationException.add(new FieldError("groupSessionReservationId", getStrings().get("Group Session Reservation ID is invalid.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		boolean success = getDatabase().execute("UPDATE group_session_reservation SET canceled=TRUE WHERE group_session_reservation_id=?",
				groupSessionReservationId) > 0;

		GroupSession groupSession = groupSessionReservationPair.getLeft();
		GroupSessionReservation groupSessionReservation = groupSessionReservationPair.getRight();
		Account attendeeAccount = getAccountService().findAccountById(groupSessionReservation.getAccountId()).get();
		String attendeeEmailAddress = attendeeAccount.getEmailAddress();
		String attendeeName = Normalizer.normalizeName(attendeeAccount.getFirstName(), attendeeAccount.getLastName()).orElse(getStrings().get("Anonymous User"));
		Institution institution = getInstitutionService().findInstitutionById(groupSession.getInstitutionId()).get();

		// Cancel any scheduled followup and reminder messages for this attendee
		getMessageService().cancelScheduledMessage(groupSessionReservation.getAttendeeReminderScheduledMessageId());
		getMessageService().cancelScheduledMessage(groupSessionReservation.getAttendeeFollowupScheduledMessageId());

		getDatabase().currentTransaction().get().addPostCommitOperation(() -> {
			EmailMessage attendeeEmailMessage = new EmailMessage.Builder(groupSession.getInstitutionId(), EmailMessageTemplate.GROUP_SESSION_RESERVATION_CANCELED_ATTENDEE, attendeeAccount.getLocale())
					.toAddresses(new ArrayList<>() {{
						add(attendeeEmailAddress);
					}})
					.replyToAddress(groupSession.getTargetEmailAddress())
					.messageContext(new HashMap<String, Object>() {{
						put("groupSession", groupSession);
						put("imageUrl", firstNonNull(findFileUploadUrlByForGroupSession(groupSession), getConfiguration().getDefaultGroupSessionImageUrlForEmail()));
						put("attendeeName", attendeeName);
						put("groupSessionStartDateDescription", getFormatter().formatDate(groupSession.getStartDateTime().toLocalDate()));
						put("groupSessionStartTimeDescription", getFormatter().formatTime(groupSession.getStartDateTime().toLocalTime(), FormatStyle.SHORT));
						put("anotherTimeUrl", format("%s/in-the-studio", getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institution.getInstitutionId(), UserExperienceTypeId.PATIENT).get()));
						put(EmailMessageContextKey.OVERRIDE_PLATFORM_NAME.name(), groupSession.getOverridePlatformName());
						put(EmailMessageContextKey.OVERRIDE_PLATFORM_EMAIL_IMAGE_URL.name(), groupSession.getOverridePlatformEmailImageUrl());
						put(EmailMessageContextKey.OVERRIDE_PLATFORM_SUPPORT_EMAIL_ADDRESS.name(), groupSession.getOverridePlatformSupportEmailAddress());
					}})
					.emailAttachments(List.of(generateICalInviteAsEmailAttachment(groupSession, groupSessionReservation, InviteMethod.CANCEL)))
					.build();

			getMessageService().enqueueMessage(attendeeEmailMessage);

			EmailMessage facilitatorEmailMessage = new EmailMessage.Builder(groupSession.getInstitutionId(), EmailMessageTemplate.GROUP_SESSION_RESERVATION_CANCELED_FACILITATOR, institution.getLocale())
					.toAddresses(new ArrayList<>() {{
						add(groupSession.getTargetEmailAddress());
					}})
					.replyToAddress(replyToAddressForEmailsTargetingFacilitator(groupSession))
					.messageContext(new HashMap<String, Object>() {{
						put("groupSession", groupSession);
						put("imageUrl", firstNonNull(findFileUploadUrlByForGroupSession(groupSession), getConfiguration().getDefaultGroupSessionImageUrlForEmail()));
						put("facilitatorName", groupSession.getFacilitatorName());
						put("attendeeName", attendeeName);
						put("attendeeEmailAddress", attendeeEmailAddress);
						put("groupSessionUrl", format("%s/admin/group-sessions/edit/%s", getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(institution.getInstitutionId(), UserExperienceTypeId.STAFF).get(), groupSession.getGroupSessionId()));
						put("groupSessionStartDateDescription", getFormatter().formatDate(groupSession.getStartDateTime().toLocalDate()));
						put("groupSessionStartTimeDescription", getFormatter().formatTime(groupSession.getStartDateTime().toLocalTime(), FormatStyle.SHORT));
						put(EmailMessageContextKey.OVERRIDE_PLATFORM_NAME.name(), groupSession.getOverridePlatformName());
						put(EmailMessageContextKey.OVERRIDE_PLATFORM_EMAIL_IMAGE_URL.name(), groupSession.getOverridePlatformEmailImageUrl());
						put(EmailMessageContextKey.OVERRIDE_PLATFORM_SUPPORT_EMAIL_ADDRESS.name(), groupSession.getOverridePlatformSupportEmailAddress());
					}})
					.build();

			getMessageService().enqueueMessage(facilitatorEmailMessage);
		});

		return success;
	}

	@Nonnull
	public FindResult<GroupSessionRequest> findGroupSessionRequests(@Nonnull FindGroupSessionRequestsRequest request) {
		requireNonNull(request);

		Integer pageNumber = request.getPageNumber();
		Integer pageSize = request.getPageSize();
		String urlName = trimToNull(request.getUrlName());
		String searchQuery = trimToNull(request.getSearchQuery());
		InstitutionId institutionId = request.getInstitutionId();
		GroupSessionRequestStatusId groupSessionRequestStatusId = request.getGroupSessionRequestStatusId();
		Account account = request.getAccount();
		FindGroupSessionRequestsRequest.FilterBehavior filterBehavior = request.getFilterBehavior() == null ? FindGroupSessionRequestsRequest.FilterBehavior.DEFAULT : request.getFilterBehavior();

		List<Object> parameters = new ArrayList<>();

		if (pageNumber == null || pageNumber < 0)
			pageNumber = 0;

		if (pageSize == null || pageSize < 0)
			pageSize = getGroupSessionRequestDefaultPageSize();

		if (pageSize > getGroupSessionRequestMaximumPageSize())
			pageSize = getGroupSessionRequestMaximumPageSize();

		StringBuilder sql = new StringBuilder("SELECT gsr.*, COUNT(gsr.*) OVER() AS total_count " +
				"FROM v_group_session_request gsr WHERE 1=1 ");

		if (institutionId != null) {
			sql.append("AND gsr.institution_id=? ");
			parameters.add(institutionId);
		}

		if (groupSessionRequestStatusId != null) {
			sql.append("AND gsr.group_session_request_status_id=? ");
			parameters.add(groupSessionRequestStatusId);
		}

		if (urlName != null) {
			sql.append("AND gsr.url_name=? ");
			parameters.add(urlName);
		}

		if (searchQuery != null) {
			sql.append("AND ((gsr.en_search_vector @@ websearch_to_tsquery('english', ?)) OR (gsr.title ILIKE CONCAT('%',?,'%') OR gsr.description ILIKE CONCAT('%',?,'%'))) ");
			parameters.add(searchQuery);
			parameters.add(searchQuery);
			parameters.add(searchQuery);
		}

		if (filterBehavior == FindGroupSessionRequestsRequest.FilterBehavior.ONLY_MY_SESSIONS) {
			if (account == null)
				throw new IllegalStateException(format("Account is required when calling %s.%s", FindGroupSessionRequestsRequest.FilterBehavior.class.getSimpleName(), filterBehavior.name()));

			sql.append("AND gsr.group_session_request_status_id=? AND (gsr.submitter_account_id=? OR (gsr.facilitator_account_id=? OR (gsr.facilitator_email_address IS NOT NULL AND LOWER(gsr.facilitator_email_address)=?))) ");
			parameters.add(GroupSessionRequestStatusId.NEW);
			parameters.add(account.getAccountId());
			parameters.add(account.getAccountId());
			parameters.add(getNormalizer().normalizeEmailAddress(account.getEmailAddress()).orElse(null));
		}

		sql.append("ORDER BY gsr.last_updated DESC ");

		sql.append("LIMIT ? ");
		parameters.add(pageSize);
		sql.append("OFFSET ?");
		parameters.add(pageNumber * pageSize);

		List<GroupSessionRequestWithTotalCount> groupSessionRequests = getDatabase().queryForList(sql.toString(),
				GroupSessionRequestWithTotalCount.class, parameters.toArray());

		return new FindResult(groupSessionRequests, groupSessionRequests.size() == 0 ? 0 : groupSessionRequests.get(0).getTotalCount());
	}

	@Nonnull
	public Integer getGroupSessionRequestDefaultPageSize() {
		return 50;
	}

	@Nonnull
	public Integer getGroupSessionRequestMaximumPageSize() {
		return 100;
	}

	@Nonnull
	public Optional<GroupSessionRequest> findGroupSessionRequestById(@Nullable UUID groupSessionRequestId) {
		if (groupSessionRequestId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM v_group_session_request WHERE group_session_request_id=?",
				GroupSessionRequest.class, groupSessionRequestId);
	}

	@Nonnull
	public Optional<GroupSessionRequestStatus> findGroupSessionRequestStatusById(@Nullable GroupSessionRequestStatusId groupSessionRequestStatusId) {
		if (groupSessionRequestStatusId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM group_session_request_status WHERE group_session_request_status_id=?",
				GroupSessionRequestStatus.class, groupSessionRequestStatusId);
	}

	@Nonnull
	public UUID createGroupSessionRequest(@Nonnull CreateGroupSessionRequestRequest request) {
		requireNonNull(request);

		InstitutionId institutionId = request.getInstitutionId();
		UUID submitterAccountId = request.getSubmitterAccountId();
		String title = trimToNull(request.getTitle());
		String description = trimToNull(request.getDescription());
		String urlName = trimToNull(request.getUrlName());
		UUID facilitatorAccountId = request.getFacilitatorAccountId();
		String facilitatorName = trimToNull(request.getFacilitatorName());
		String facilitatorEmailAddress = trimToNull(request.getFacilitatorEmailAddress());
		String customQuestion1 = trimToNull(request.getCustomQuestion1());
		String customQuestion2 = trimToNull(request.getCustomQuestion2());
		boolean dataCollectionEnabled = request.getDataCollectionEnabled() == null ? true : request.getDataCollectionEnabled();
		Institution institution = null;
		Account submitterAccount = null;
		UUID groupSessionRequestId = UUID.randomUUID();
		UUID imageFileUploadId = request.getImageFileUploadId();

		ValidationException validationException = new ValidationException();

		if (institutionId == null) {
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));
		} else {
			institution = getInstitutionService().findInstitutionById(institutionId).orElse(null);

			if (institution == null)
				validationException.add(new FieldError("institutionId", getStrings().get("Invalid institution.")));
		}

		if (submitterAccountId == null) {
			validationException.add(new FieldError("submitterAccountId", getStrings().get("Submitter account ID is required.")));
		} else {
			submitterAccount = getAccountService().findAccountById(submitterAccountId).orElse(null);

			if (submitterAccount == null)
				validationException.add(new FieldError("submitterAccountId", getStrings().get("Invalid submitter account.")));
		}

		if (title == null)
			validationException.add(new FieldError("title", getStrings().get("Title is required.")));

		if (description == null)
			validationException.add(new FieldError("description", getStrings().get("Description is required.")));

		if (urlName == null)
			validationException.add(new FieldError("urlName", getStrings().get("URL name is required.")));

		if (facilitatorName == null)
			validationException.add(new FieldError("facilitatorName", getStrings().get("Facilitator name is required.")));

		if (facilitatorEmailAddress == null)
			validationException.add(new FieldError("facilitatorEmailAddress", getStrings().get("Facilitator email address is required.")));
		else if (!isValidEmailAddress(facilitatorEmailAddress))
			validationException.add(new FieldError("facilitatorEmailAddress", getStrings().get("Facilitator email address is invalid.")));

		if (validationException.hasErrors())
			throw validationException;

		if (imageFileUploadId == null)
			imageFileUploadId = getDefaultGroupSessionImageFileUploadId();

		if (!dataCollectionEnabled) {
			customQuestion1 = null;
			customQuestion2 = null;
		}

		getDatabase().execute("""
						INSERT INTO group_session_request (group_session_request_id, institution_id, 
						group_session_request_status_id, title, description, submitter_account_id, facilitator_account_id, 
						facilitator_name, facilitator_email_address, url_name, custom_question_1, custom_question_2, 
						data_collection_enabled, image_file_upload_id)
						VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
						""", groupSessionRequestId, institutionId, GroupSessionRequestStatusId.NEW,
				title, description, submitterAccountId, facilitatorAccountId, facilitatorName, facilitatorEmailAddress,
				urlName, customQuestion1, customQuestion2, dataCollectionEnabled, imageFileUploadId);

		return groupSessionRequestId;
	}

	public void updateGroupSessionRequest(@Nonnull UpdateGroupSessionRequestRequest request) {
		requireNonNull(request);

		UUID groupSessionRequestId = request.getGroupSessionRequestId();
		String title = trimToNull(request.getTitle());
		String description = trimToNull(request.getDescription());
		String urlName = trimToNull(request.getUrlName());
		UUID facilitatorAccountId = request.getFacilitatorAccountId();
		String facilitatorName = trimToNull(request.getFacilitatorName());
		String facilitatorEmailAddress = trimToNull(request.getFacilitatorEmailAddress());
		String customQuestion1 = trimToNull(request.getCustomQuestion1());
		String customQuestion2 = trimToNull(request.getCustomQuestion2());
		boolean dataCollectionEnabled = request.getDataCollectionEnabled() == null ? true : request.getDataCollectionEnabled();
		UUID imageFileUploadId = request.getImageFileUploadId();

		ValidationException validationException = new ValidationException();

		if (title == null)
			validationException.add(new FieldError("title", getStrings().get("Title is required.")));

		if (description == null)
			validationException.add(new FieldError("description", getStrings().get("Description is required.")));

		if (urlName == null)
			validationException.add(new FieldError("urlName", getStrings().get("URL name is required.")));

		if (facilitatorName == null)
			validationException.add(new FieldError("facilitatorName", getStrings().get("Facilitator name is required.")));

		if (facilitatorEmailAddress == null)
			validationException.add(new FieldError("facilitatorEmailAddress", getStrings().get("Facilitator email address is required.")));
		else if (!isValidEmailAddress(facilitatorEmailAddress))
			validationException.add(new FieldError("facilitatorEmailAddress", getStrings().get("Facilitator email address is invalid.")));

		if (validationException.hasErrors())
			throw validationException;

		if (imageFileUploadId == null)
			imageFileUploadId = getDefaultGroupSessionImageFileUploadId();

		if (!dataCollectionEnabled) {
			customQuestion1 = null;
			customQuestion2 = null;
		}

		getDatabase().execute("""
						UPDATE group_session_request SET title=?, description=?, facilitator_account_id=?,
						facilitator_name=?, facilitator_email_address=?, url_name=?, custom_question_1=?, 
						custom_question_2=?, data_collection_enabled=?, image_file_upload_id=?
						WHERE group_session_request_id=?
						""", title, description, facilitatorAccountId, facilitatorName, facilitatorEmailAddress,
				urlName, customQuestion1, customQuestion2, dataCollectionEnabled, imageFileUploadId, groupSessionRequestId);
	}

	@Nonnull
	public Boolean updateGroupSessionRequestStatus(@Nonnull UpdateGroupSessionRequestStatusRequest request) {
		requireNonNull(request);

		UUID groupSessionRequestId = request.getGroupSessionRequestId();
		UUID accountId = request.getAccountId();
		GroupSessionRequestStatusId groupSessionRequestStatusId = request.getGroupSessionRequestStatusId();
		GroupSessionRequest groupSessionRequest = null;
		ValidationException validationException = new ValidationException();

		if (groupSessionRequestId == null) {
			validationException.add(new FieldError("groupSessionRequestId", getStrings().get("Group Session Request ID is required.")));
		} else {
			groupSessionRequest = findGroupSessionRequestById(groupSessionRequestId).orElse(null);

			if (groupSessionRequest == null)
				validationException.add(new FieldError("groupSessionRequestId", getStrings().get("Invalid Group Session Request.")));
		}

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (groupSessionRequestStatusId == null)
			validationException.add(new FieldError("groupSessionRequestStatusId", getStrings().get("Group Session Request Status ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		if (groupSessionRequestStatusId == groupSessionRequest.getGroupSessionRequestStatusId())
			return false;

		boolean updated = getDatabase().execute("UPDATE group_session_request SET group_session_request_status_id=? WHERE group_session_request_id=?",
				groupSessionRequestStatusId, groupSessionRequestId) > 0;

		if (groupSessionRequestStatusId == GroupSessionRequestStatusId.ADDED) {
			// After committing a change to ADDED status, email the submitter if it's a different account)
			GroupSessionRequest pinnedGroupSessionRequest = groupSessionRequest;

			if (!Objects.equals(accountId, groupSessionRequest.getSubmitterAccountId())) {
				getDatabase().currentTransaction().get().addPostCommitOperation(() -> {
					Account submitterAccount = getAccountService().findAccountById(pinnedGroupSessionRequest.getSubmitterAccountId()).get();

					EmailMessage attendeeEmailMessage = new EmailMessage.Builder(submitterAccount.getInstitutionId(), EmailMessageTemplate.GROUP_SESSION_LIVE_SUBMITTER, submitterAccount.getLocale())
							.toAddresses(new ArrayList<>() {{
								add(submitterAccount.getEmailAddress());
							}})
							.replyToAddress(replyToAddressForEmailsTargetingFacilitator(pinnedGroupSessionRequest))
							.messageContext(new HashMap<String, Object>() {{
								put("groupSessionRequest", pinnedGroupSessionRequest);
								put("imageUrl", firstNonNull(pinnedGroupSessionRequest.getImageUrl(), getConfiguration().getDefaultGroupSessionImageUrlForEmail()));
								put("groupSessionRequestUrl", format("%s/in-the-studio", getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(pinnedGroupSessionRequest.getInstitutionId(), UserExperienceTypeId.PATIENT).get()));
							}})
							.build();

					getMessageService().enqueueMessage(attendeeEmailMessage);
				});
			}
		}

		return updated;
	}

	@Nonnull
	public Optional<GroupSessionResponse> findGroupSessionResponseById(@Nullable UUID groupSessionResponseId) {
		if (groupSessionResponseId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM group_session_response WHERE group_session_response_id=?",
				GroupSessionResponse.class, groupSessionResponseId);
	}

	@Nonnull
	public UUID createGroupSessionResponse(@Nonnull CreateGroupSessionResponseRequest request) {
		requireNonNull(request);

		UUID groupSessionRequestId = request.getGroupSessionRequestId();
		UUID respondentAccountId = request.getRespondentAccountId();
		String respondentName = trimToNull(request.getRespondentName());
		String respondentEmailAddress = trimToNull(request.getRespondentEmailAddress());
		String respondentPhoneNumber = trimToNull(request.getRespondentPhoneNumber());
		LocalDate suggestedDate = request.getSuggestedDate();
		String suggestedTime = trimToNull(request.getSuggestedTime());
		String expectedParticipants = trimToNull(request.getExpectedParticipants());
		String notes = trimToNull(request.getNotes());
		String customAnswer1 = trimToNull(request.getCustomAnswer1());
		String customAnswer2 = trimToNull(request.getCustomAnswer2());
		GroupSessionRequest groupSessionRequest = null;
		UUID groupSessionResponseId = UUID.randomUUID();

		ValidationException validationException = new ValidationException();

		if (groupSessionRequestId == null) {
			validationException.add(new FieldError("groupSessionRequestId", getStrings().get("Group Session Request ID is required.")));
		} else {
			groupSessionRequest = findGroupSessionRequestById(groupSessionRequestId).orElse(null);

			if (groupSessionRequest == null)
				validationException.add(new FieldError("groupSessionRequestId", getStrings().get("Group Session Request ID is invalid.")));
		}

		if (respondentAccountId == null)
			validationException.add(new FieldError("respondentAccountId", getStrings().get("Account ID is required.")));

		if (respondentName == null)
			validationException.add(new FieldError("respondentName", getStrings().get("Name is required.")));

		if (respondentEmailAddress == null)
			validationException.add(new FieldError("urlName", getStrings().get("URL name is required.")));

		if (respondentPhoneNumber != null) {
			respondentPhoneNumber = getNormalizer().normalizePhoneNumberToE164(respondentPhoneNumber).orElse(null);

			if (respondentPhoneNumber == null)
				validationException.add(new FieldError("respondentPhoneNumber", getStrings().get("Phone number is invalid.")));
		}

		if (respondentEmailAddress == null)
			validationException.add(new FieldError("respondentEmailAddress", getStrings().get("Email address is required.")));
		else if (!isValidEmailAddress(respondentEmailAddress))
			validationException.add(new FieldError("respondentEmailAddress", getStrings().get("Email address is invalid.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("INSERT INTO group_session_response (group_session_response_id, group_session_request_id, " +
						"respondent_account_id, respondent_name, respondent_email_address, respondent_phone_number, suggested_date, " +
						"suggested_time, expected_participants, notes, custom_answer_1, custom_answer_2) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
				groupSessionResponseId, groupSessionRequestId, respondentAccountId, respondentName, respondentEmailAddress, respondentPhoneNumber,
				suggestedDate, suggestedTime, expectedParticipants, notes, customAnswer1, customAnswer2);

		Account facilitatorAccount = getAccountService().findAccountById(groupSessionRequest.getFacilitatorAccountId()).orElse(null);
		Locale facilitatorLocale = facilitatorAccount == null ? Locale.US : facilitatorAccount.getLocale();
		GroupSessionRequest pinnedGroupSessionRequest = groupSessionRequest;
		String formattedRespondentPhoneNumber = getFormatter().formatPhoneNumber(respondentPhoneNumber, facilitatorLocale);

		getDatabase().currentTransaction().get().addPostCommitOperation(() -> {
			EmailMessage facilitatorEmailMessage = new EmailMessage.Builder(pinnedGroupSessionRequest.getInstitutionId(), EmailMessageTemplate.GROUP_SESSION_RESPONSE_CREATED_FACILITATOR, facilitatorLocale)
					.toAddresses(new ArrayList<>() {{
						add(pinnedGroupSessionRequest.getFacilitatorEmailAddress());
					}})
					.replyToAddress(replyToAddressForEmailsTargetingFacilitator(pinnedGroupSessionRequest))
					.messageContext(new HashMap<String, Object>() {{
						put("groupSessionRequestTitle", pinnedGroupSessionRequest.getTitle());
						put("facilitatorName", pinnedGroupSessionRequest.getFacilitatorName());
						put("respondentName", respondentName);
						put("imageUrl", pinnedGroupSessionRequest.getImageUrl());
						put("respondentEmailAddress", respondentEmailAddress);
						put("respondentPhoneNumber", formattedRespondentPhoneNumber);
						put("suggestedDate", suggestedDate == null ? getStrings().get("(not specified)") : getFormatter().formatDate(suggestedDate));
						put("suggestedTime", suggestedTime == null ? getStrings().get("(not specified)") : suggestedTime);
						put("expectedParticipants", expectedParticipants);
						put("notes", notes);
						put("customQuestion1", pinnedGroupSessionRequest.getCustomQuestion1());
						put("customAnswer1", customAnswer1);
						put("customQuestion2", pinnedGroupSessionRequest.getCustomQuestion2());
						put("customAnswer2", customAnswer2);
					}})
					.build();

			getMessageService().enqueueMessage(facilitatorEmailMessage);
		});

		return groupSessionResponseId;
	}

	@Nonnull
	private void sendAdminNotification(@Nonnull Account accountAddingGroupSession,
																		 @Nonnull GroupSession groupSession) {
		requireNonNull(accountAddingGroupSession);
		requireNonNull(groupSession);

		if (accountAddingGroupSession.getRoleId() == RoleId.ADMINISTRATOR)
			return;

		List<Account> accountsToNotify = getAccountService().findAdminAccountsForInstitution(accountAddingGroupSession.getInstitutionId());

		getDatabase().currentTransaction().get().addPostCommitOperation(() -> {
			for (Account accountToNotify : accountsToNotify) {
				if (accountToNotify.getEmailAddress() != null) {
					EmailMessage emailMessage = new EmailMessage.Builder(accountToNotify.getInstitutionId(), EmailMessageTemplate.ADMIN_GROUP_SESSION_ADDED, accountToNotify.getLocale())
							.toAddresses(List.of(accountToNotify.getEmailAddress()))
							.replyToAddress(replyToAddressForEmailsTargetingFacilitator(groupSession))
							.messageContext(Map.of(
									"adminAccountName", Normalizer.normalizeName(accountToNotify.getFirstName(), accountToNotify.getLastName()).orElse(getStrings().get("Anonymous User")),
									"submittingAccountName", Normalizer.normalizeName(accountAddingGroupSession.getFirstName(), accountAddingGroupSession.getLastName()).orElse(getStrings().get("Anonymous User")),
									"groupSessionTitle", groupSession.getTitle(),
									"groupSessionListUrl", getLinkGenerator().generateGroupSessionsAdminListLink(accountToNotify.getInstitutionId(), UserExperienceTypeId.STAFF)
							))
							.build();

					getMessageService().enqueueMessage(emailMessage);
				}
			}
		});
	}

	@Nonnull
	public FileUploadResult createGroupSessionFileUpload(@Nonnull CreateFileUploadRequest request, @Nonnull String storagePrefixKey) {
		requireNonNull(request);
		requireNonNull(storagePrefixKey);

		UUID accountId = request.getAccountId();
		String filename = trimToNull(request.getFilename());
		String contentType = trimToNull(request.getContentType());

		ValidationException validationException = new ValidationException();

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (filename == null)
			validationException.add(new FieldError("filename", getStrings().get("Filename is required.")));

		if (contentType == null)
			validationException.add(new FieldError("contentType", getStrings().get("Content type is required.")));

		if (validationException.hasErrors())
			throw validationException;

		// Make a separate instance so we don't mutate the request passed into this method
		CreateFileUploadRequest fileUploadRequest = new CreateFileUploadRequest();
		fileUploadRequest.setAccountId(request.getAccountId());
		fileUploadRequest.setContentType(request.getContentType());
		fileUploadRequest.setFilename(request.getFilename());
		fileUploadRequest.setPublicRead(true);
		fileUploadRequest.setStorageKeyPrefix(storagePrefixKey);
		fileUploadRequest.setMetadata(Map.of(
				"account-id", request.getAccountId().toString()
		));
		fileUploadRequest.setFileUploadTypeId(request.getFileUploadTypeId());

		FileUploadResult fileUploadResult = getSystemService().createFileUpload(fileUploadRequest);

		return fileUploadResult;
	}
	@Nonnull
	public String generateGoogleCalendarTemplateUrl(@Nonnull GroupSession groupSession) {
		requireNonNull(groupSession);

		return getGoogleCalendarUrlGenerator().generateNewEventUrl(groupSession.getTitle(),
				groupSession.getDescription(), groupSession.getStartDateTime(), groupSession.getEndDateTime(),
				groupSession.getTimeZone(), groupSession.getVideoconferenceUrl());
	}

	@Nonnull
	public String generateICalInvite(@Nonnull GroupSession groupSession,
																	 @Nonnull GroupSessionReservation groupSessionReservation,
																	 @Nonnull InviteMethod inviteMethod) {
		requireNonNull(groupSession);
		requireNonNull(groupSessionReservation);
		requireNonNull(inviteMethod);

		String extendedDescription = groupSession.getVideoconferenceUrl() == null ? groupSession.getDescription() : format("%s\n\n%s", groupSession.getDescription(), getStrings().get("Join videoconference: {{videoconferenceUrl}}", new HashMap<String, Object>() {{
			put("videoconferenceUrl", groupSession.getVideoconferenceUrl());
		}}));

		String location = groupSession.getVideoconferenceUrl() == null ? groupSession.getInPersonLocation() : groupSession.getVideoconferenceUrl();

		InviteAttendee inviteAttendee = InviteAttendee.forEmailAddress(groupSessionReservation.getEmailAddress());
		InviteOrganizer inviteOrganizer = InviteOrganizer.forEmailAddress(groupSession.getTargetEmailAddress());

		return getiCalInviteGenerator().generateInvite(groupSession.getGroupSessionId().toString(), groupSession.getTitle(),
				extendedDescription, groupSession.getStartDateTime(), groupSession.getEndDateTime(),
				groupSession.getTimeZone(), location, inviteMethod, inviteOrganizer, inviteAttendee, OrganizerAttendeeStrategy.BOTH_ATTENDEES);
	}

	@Nonnull
	public EmailAttachment generateICalInviteAsEmailAttachment(@Nonnull GroupSession groupSession,
																														 @Nonnull GroupSessionReservation groupSessionReservation,
																														 @Nonnull InviteMethod inviteMethod) {
		requireNonNull(groupSession);
		requireNonNull(groupSessionReservation);
		requireNonNull(inviteMethod);

		String iCalInvite = generateICalInvite(groupSession, groupSessionReservation, inviteMethod);

		String filename = "invite.ics";
		String method = inviteMethod == InviteMethod.CANCEL ? "CANCEL" : "REQUEST";
		String contentType = format("text/calendar; charset=utf-8; method=%s; name=%s", method, filename);

		return new EmailAttachment(filename, contentType, iCalInvite.getBytes(StandardCharsets.UTF_8));
	}

	@Nonnull
	protected UUID getDefaultGroupSessionImageFileUploadId() {
		return UUID.fromString("3e39722c-5ae9-4e35-a1f6-e0adc0df391c");
	}

	@Nonnull
	public List<GroupSessionCollection> findGroupSessionCollections(@Nonnull Account account) {
		requireNonNull(account);

		return getDatabase().queryForList("SELECT * from group_session_collection WHERE institution_id = ? ORDER BY display_order",
				GroupSessionCollection.class, account.getInstitutionId());
	}

	@Nonnull
	private String findFileUploadUrlByForGroupSession(GroupSession groupSession) {
		requireNonNull(groupSession);

		if (groupSession.getImageFileUploadId() == null)
			return null;
		else {
			Optional<FileUpload> fileUpload= getDatabase().queryForObject("""
				SELECT *
				FROM file_upload
				WHERE file_upload_id = ?""", FileUpload.class, groupSession.getGroupSessionId());

			if (fileUpload.isPresent())
				return fileUpload.get().getUrl();
			else
				return null;
		}
	}

	@Nonnull
	protected PolicyFactory createHtmlSanitizingPolicyFactory() {
		// Here's what we want to permit in our rich-text input
		// for non-administrative users:
		//
		// "p"
		// "strong"
		// "br"
		// "em"
		// "u"
		// "s"
		// "ol"
		// "li"
		// "ul"
		// "a" with href rel="noopener noreferrer" target="_blank"
		return new HtmlPolicyBuilder()
				.allowElements("p", "strong", "br", "em", "u", "s", "ol", "li", "ul", "a")
				.allowUrlProtocols("https")
				.allowAttributes("href", "rel", "target").onElements("a")
				.requireRelNofollowOnLinks()
				.toFactory();
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
					closeOutCompletedGroupSessions();
				} catch (Exception e) {
					getLogger().error("Unable to close out completed group session", e);
					getErrorReporter().report(e);
				}
			});
		}

		protected void closeOutCompletedGroupSessions() {
			// Can probably be more efficient and do it all in one query, doing it this way to be specific to timezones...
			// Basically saying "if ADDED group session has completed in the group session's timezone, then mark it ARCHIVED and send out any followup emails"
			List<GroupSession> groupSessions = getDatabase().queryForList("""
					SELECT * FROM group_session
					WHERE group_session_status_id=? 
					AND start_date_time IS NOT NULL
					AND end_date_time IS NOT NULL
					""", GroupSession.class, GroupSessionStatusId.ADDED);

			for (GroupSession groupSession : groupSessions) {
				LocalDateTime currentDateTime = LocalDateTime.now(groupSession.getTimeZone());
				LocalDateTime groupSessionEndDateTime = groupSession.getEndDateTime();

				if (currentDateTime.isAfter(groupSessionEndDateTime)) {
					getLogger().info("Group session ID {} is now over, marking as {}...", groupSession.getGroupSessionId(), GroupSessionStatusId.ARCHIVED.name());
					getDatabase().execute("UPDATE group_session SET group_session_status_id=? WHERE group_session_id=?", GroupSessionStatusId.ARCHIVED, groupSession.getGroupSessionId());
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
	protected AccountService getAccountService() {
		return this.accountServiceProvider.get();
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
	}

	@Nonnull
	protected MessageService getMessageService() {
		return this.messageServiceProvider.get();
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected UploadManager getUploadManager() {
		return this.uploadManager;
	}

	@Nonnull
	protected LinkGenerator getLinkGenerator() {
		return this.linkGenerator;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return this.formatter;
	}

	@Nonnull
	protected Normalizer getNormalizer() {
		return this.normalizer;
	}

	@Nonnull
	protected GoogleCalendarUrlGenerator getGoogleCalendarUrlGenerator() {
		return this.googleCalendarUrlGenerator;
	}

	@Nonnull
	protected ICalInviteGenerator getiCalInviteGenerator() {
		return this.iCalInviteGenerator;
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
	protected Long getBackgroundTaskIntervalInSeconds() {
		return BACKGROUND_TASK_INTERVAL_IN_SECONDS;
	}

	@Nonnull
	protected Long getBackgroundTaskInitialDelayInSeconds() {
		return BACKGROUND_TASK_INITIAL_DELAY_IN_SECONDS;
	}

	@Nonnull
	protected Set<String> getIllegalGroupSessionUrlNames() {
		return ILLEGAL_GROUP_SESSION_URL_NAMES;
	}

	@Nonnull
	protected Object getBackgroundTaskLock() {
		return this.backgroundTaskLock;
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
	protected ScreeningService getScreeningService() {
		return this.screeningService;
	}

	@Nonnull
	protected TagService getTagService() {
		return this.tagService;
	}

	@Nonnull
	protected PolicyFactory getHtmlSanitizingPolicyFactory() {
		return this.htmlSanitizingPolicyFactory;
	}

	@Nullable
	protected SystemService getSystemService() {
		return systemService;
	}

}