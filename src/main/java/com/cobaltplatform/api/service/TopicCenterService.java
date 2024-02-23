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

import com.cobaltplatform.api.model.api.request.FindResourceLibraryContentRequest;
import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.db.GroupSession;
import com.cobaltplatform.api.model.db.GroupSessionRequest;
import com.cobaltplatform.api.model.db.GroupSessionRequestStatus.GroupSessionRequestStatusId;
import com.cobaltplatform.api.model.db.GroupSessionStatus.GroupSessionStatusId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionTopicCenter;
import com.cobaltplatform.api.model.db.PinboardNote;
import com.cobaltplatform.api.model.db.TopicCenter;
import com.cobaltplatform.api.model.db.TopicCenterRowTag;
import com.cobaltplatform.api.model.db.TopicCenterRowTagType.TopicCenterRowTagTypeId;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.model.service.NavigationItem;
import com.cobaltplatform.api.model.service.TopicCenterRowDetail;
import com.cobaltplatform.api.util.db.DatabaseProvider;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class TopicCenterService {
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Provider<ContentService> contentServiceProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public TopicCenterService(@Nonnull DatabaseProvider databaseProvider,
														@Nonnull Provider<ContentService> contentServiceProvider,
														@Nonnull Strings strings) {
		requireNonNull(databaseProvider);
		requireNonNull(contentServiceProvider);
		requireNonNull(strings);

		this.databaseProvider = databaseProvider;
		this.contentServiceProvider = contentServiceProvider;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public List<TopicCenter> findTopicCentersByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT tc.*
				FROM topic_center tc, institution_topic_center itc
				WHERE itc.institution_id=?
				AND itc.topic_center_id=tc.topic_center_id
				ORDER BY tc.name
				""", TopicCenter.class, institutionId);
	}

	@Nonnull
	public Optional<TopicCenter> findTopicCenterById(@Nullable UUID topicCenterId) {
		if (topicCenterId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
						SELECT *
						FROM topic_center
						WHERE topic_center_id=?
						""",
				TopicCenter.class, topicCenterId);
	}

	@Nonnull
	public Optional<TopicCenter> findTopicCenterByInstitutionIdAndUrlName(@Nullable InstitutionId institutionId,
																																				@Nullable String urlName) {
		urlName = normalizeUrlName(urlName).orElse(null);

		if (institutionId == null || urlName == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
						SELECT tc.*
						FROM topic_center tc, institution_topic_center itc
						WHERE tc.topic_center_id=itc.topic_center_id
						AND itc.institution_id=?
						AND tc.url_name=?
						""",
				TopicCenter.class, institutionId, urlName);
	}

	@Nonnull
	public Optional<InstitutionTopicCenter> findInstitutionTopicCenter(@Nullable InstitutionId institutionId,
																																		 @Nullable UUID topicCenterId) {
		if (institutionId == null || topicCenterId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM institution_topic_center
				WHERE institution_id=?
				AND topic_center_id=?
				""", InstitutionTopicCenter.class, institutionId, topicCenterId);
	}

	@Nonnull
	public List<NavigationItem> findTopicCenterNavigationItemsByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return Collections.emptyList();

		return getDatabase().queryForList("""
				SELECT
					itc.navigation_icon_name as icon_name,
					('/topic-centers/' || tc.url_name) as url,
					COALESCE(itc.navigation_item_name, tc.name) as name,
					tc.image_url
				FROM institution_topic_center itc, topic_center tc
				WHERE itc.topic_center_id=tc.topic_center_id
				AND itc.navigation_item_enabled=TRUE
				AND itc.institution_id=?
				ORDER BY itc.navigation_display_order
				""", NavigationItem.class, institutionId);
	}

	@Nonnull
	public List<TopicCenterRowDetail> findTopicCenterRowsByTopicCenterId(@Nullable UUID topicCenterId,
																																			 @Nullable InstitutionId institutionId,
																																			 @Nullable UUID accountId) {
		if (topicCenterId == null || institutionId == null)
			return Collections.emptyList();

		List<TopicCenterRowDetail> topicCenterRows = getDatabase().queryForList("""
				    SELECT *
				    FROM topic_center_row
				    WHERE topic_center_id=?
				    ORDER BY display_order
				""", TopicCenterRowDetail.class, topicCenterId);

		// Pull all of the tags across all rows in the topic center (so we don't have to query for each row individually)
		List<TopicCenterRowTagDetail> topicCenterRowTags = getDatabase().queryForList("""
				    SELECT tcrt.*
				    FROM topic_center_row_tag tcrt, topic_center_row tcr
				    WHERE tcr.topic_center_row_id=tcrt.topic_center_row_id
				    AND tcr.topic_center_id=?
				    ORDER BY tcr.display_order, tcrt.display_order
				""", TopicCenterRowTagDetail.class, topicCenterId);

		// Make topic center row tags available by row ID for easy access
		Map<UUID, List<TopicCenterRowTagDetail>> topicCenterRowTagsByTopicCenterRowId = new HashMap<>(topicCenterRowTags.size());

		for (TopicCenterRowTagDetail topicCenterRowTag : topicCenterRowTags) {
			List<TopicCenterRowTagDetail> topicCenterRowTagsForRow = topicCenterRowTagsByTopicCenterRowId.get(topicCenterRowTag.getTopicCenterRowId());

			if (topicCenterRowTagsForRow == null) {
				topicCenterRowTagsForRow = new ArrayList<>();
				topicCenterRowTagsByTopicCenterRowId.put(topicCenterRowTag.getTopicCenterRowId(), topicCenterRowTagsForRow);
			}

			topicCenterRowTagsForRow.add(topicCenterRowTag);
		}

		// Pull all of the group sessions across all rows in the topic center (so we don't have to query for each row individually)
		List<GroupSessionTopicCenterRow> groupSessionTopicCenterRows = getDatabase().queryForList("""
				SELECT gs.*, tcr.topic_center_row_id
				FROM topic_center_row tcr, v_group_session gs, topic_center_row_group_session tcrgs
				WHERE tcr.topic_center_id=?
				AND tcr.topic_center_row_id=tcrgs.topic_center_row_id
				AND tcrgs.group_session_id=gs.group_session_id
				AND gs.group_session_status_id=?
				ORDER BY tcr.display_order, tcrgs.display_order
				""", GroupSessionTopicCenterRow.class, topicCenterId, GroupSessionStatusId.ADDED);

		// Pull all of the group sessions across all rows in the topic center (so we don't have to query for each row individually)
		List<GroupSessionRequestTopicCenterRow> groupSessionRequestTopicCenterRows = getDatabase().queryForList("""
				SELECT gsr.*, tcr.topic_center_row_id
				FROM topic_center_row tcr, v_group_session_request gsr, topic_center_row_group_session_request tcrgsr
				WHERE tcr.topic_center_id=?
				AND tcr.topic_center_row_id=tcrgsr.topic_center_row_id
				AND tcrgsr.group_session_request_id=gsr.group_session_request_id
				AND gsr.group_session_request_status_id=?
				ORDER BY tcr.display_order, tcrgsr.display_order
				""", GroupSessionRequestTopicCenterRow.class, topicCenterId, GroupSessionRequestStatusId.ADDED);

		// Pull all of the content across all rows in the topic center (so we don't have to query for each row individually)
		List<PinboardNoteTopicCenterRow> pinboardNoteTopicCenterRows = getDatabase().queryForList("""
				SELECT pn.*, tcr.topic_center_row_id
				FROM topic_center_row tcr, pinboard_note pn, topic_center_row_pinboard_note tcrpn
				WHERE tcr.topic_center_id=?
				AND tcr.topic_center_row_id=tcrpn.topic_center_row_id
				AND tcrpn.pinboard_note_id=pn.pinboard_note_id
				ORDER BY tcr.display_order, tcrpn.display_order
				""", PinboardNoteTopicCenterRow.class, topicCenterId);

		// Pull all of the content across all rows in the topic center (so we don't have to query for each row individually)
		List<ContentTopicCenterRow> contentTopicCenterRows = getDatabase().queryForList("""
				SELECT c.*, tcr.topic_center_row_id
				FROM topic_center_row tcr, v_institution_content c, topic_center_row_content tcrc
				WHERE tcr.topic_center_id=?
				AND tcr.topic_center_row_id=tcrc.topic_center_row_id
				AND tcrc.content_id=c.content_id
				ORDER BY tcr.display_order, tcrc.display_order
				""", ContentTopicCenterRow.class, topicCenterId);

		getContentService().applyTagsToContents(contentTopicCenterRows, institutionId);

		for (TopicCenterRowDetail topicCenterRow : topicCenterRows) {
			topicCenterRow.setGroupSessions(new ArrayList<>());
			topicCenterRow.setGroupSessionRequests(new ArrayList<>());
			topicCenterRow.setPinboardNotes(new ArrayList<>());
			topicCenterRow.setContents(new ArrayList<>());
			topicCenterRow.setTopicCenterRowTags(new ArrayList<>());

			for (GroupSessionTopicCenterRow groupSession : groupSessionTopicCenterRows)
				if (groupSession.getTopicCenterRowId().equals(topicCenterRow.getTopicCenterRowId()))
					topicCenterRow.getGroupSessions().add(groupSession);

			for (GroupSessionRequestTopicCenterRow groupSessionRequest : groupSessionRequestTopicCenterRows)
				if (groupSessionRequest.getTopicCenterRowId().equals(topicCenterRow.getTopicCenterRowId()))
					topicCenterRow.getGroupSessionRequests().add(groupSessionRequest);

			for (PinboardNoteTopicCenterRow pinboardNote : pinboardNoteTopicCenterRows)
				if (pinboardNote.getTopicCenterRowId().equals(topicCenterRow.getTopicCenterRowId()))
					topicCenterRow.getPinboardNotes().add(pinboardNote);

			for (ContentTopicCenterRow content : contentTopicCenterRows)
				if (content.getTopicCenterRowId().equals(topicCenterRow.getTopicCenterRowId()))
					topicCenterRow.getContents().add(content);

			List<TopicCenterRowTagDetail> rowTags = topicCenterRowTagsByTopicCenterRowId.get(topicCenterRow.getTopicCenterRowId());

			if (rowTags != null && rowTags.size() > 0) {
				for (TopicCenterRowTagDetail rowTag : rowTags) {
					// For now, we only support CONTENT type.  Later, we can support others
					if (rowTag.getTopicCenterRowTagTypeId() == TopicCenterRowTagTypeId.CONTENT) {
						// Pull a limited set of content for this tag
						FindResult<Content> findResult = getContentService().findResourceLibraryContent(new FindResourceLibraryContentRequest() {
							{
								setInstitutionId(institutionId);
								setSearchQuery(null);
								setTagIds(Set.of(rowTag.getTagId()));
								setContentTypeIds(Set.of());
								setContentDurationIds(Set.of());
								setPageNumber(0);
								setPageSize(10);
								setPrioritizeUnviewedForAccountId(accountId);
							}
						});

						rowTag.setContents(findResult.getResults());
					} else {
						throw new UnsupportedOperationException(format("%s.%s is not yet supported",
								TopicCenterRowTagTypeId.class.getSimpleName(), rowTag.getTopicCenterRowTagTypeId().name()));
					}

					topicCenterRow.getTopicCenterRowTags().add(rowTag);
				}
			}
		}

		// Set group session (and by-request) carousel titles if applicable, taking overrides into account
		for (TopicCenterRowDetail topicCenterRow : topicCenterRows) {
			if (topicCenterRow.getGroupSessions() != null && topicCenterRow.getGroupSessions().size() > 0) {
				String groupSessionsTitle = getStrings().get("Scheduled");

				if (topicCenterRow.getGroupSessionsTitleOverride() != null)
					groupSessionsTitle = topicCenterRow.getGroupSessionsTitleOverride();

				topicCenterRow.setGroupSessionsTitle(groupSessionsTitle);

				String groupSessionsDescription = getStrings().get("Scheduled sessions have a set date and time.");

				if (topicCenterRow.getGroupSessionsDescriptionOverride() != null)
					groupSessionsDescription = topicCenterRow.getGroupSessionsDescriptionOverride();

				topicCenterRow.setGroupSessionsDescription(groupSessionsDescription);
			}

			if (topicCenterRow.getGroupSessionRequests() != null && topicCenterRow.getGroupSessionRequests().size() > 0) {
				String groupSessionRequestsTitle = getStrings().get("By Request");

				if (topicCenterRow.getGroupSessionRequestsTitleOverride() != null)
					groupSessionRequestsTitle = topicCenterRow.getGroupSessionRequestsTitleOverride();

				topicCenterRow.setGroupSessionRequestsTitle(groupSessionRequestsTitle);

				String groupSessionRequestsDescription = getStrings().get("Submit a request to facilitate a one-time session.");

				if (topicCenterRow.getGroupSessionRequestsDescriptionOverride() != null)
					groupSessionRequestsDescription = topicCenterRow.getGroupSessionRequestsDescriptionOverride();

				topicCenterRow.setGroupSessionRequestsDescription(groupSessionRequestsDescription);
			}
		}

		return topicCenterRows;
	}

	@Nonnull
	protected Optional<String> normalizeUrlName(@Nullable String urlName) {
		urlName = trimToEmpty(urlName).toLowerCase(Locale.US);
		return urlName.length() == 0 ? Optional.empty() : Optional.of(urlName);
	}

	@NotThreadSafe
	public static class TopicCenterRowTagDetail extends TopicCenterRowTag {
		@Nullable
		private List<Content> contents;
		@Nullable
		private List<GroupSession> groupSessions;

		@Nullable
		public List<Content> getContents() {
			return this.contents;
		}

		public void setContents(@Nullable List<Content> contents) {
			this.contents = contents;
		}

		@Nullable
		public List<GroupSession> getGroupSessions() {
			return this.groupSessions;
		}

		public void setGroupSessions(@Nullable List<GroupSession> groupSessions) {
			this.groupSessions = groupSessions;
		}
	}

	@NotThreadSafe
	protected static class GroupSessionTopicCenterRow extends GroupSession {
		@Nullable
		private UUID topicCenterRowId;

		@Nullable
		public UUID getTopicCenterRowId() {
			return this.topicCenterRowId;
		}

		public void setTopicCenterRowId(@Nullable UUID topicCenterRowId) {
			this.topicCenterRowId = topicCenterRowId;
		}
	}

	@NotThreadSafe
	protected static class GroupSessionRequestTopicCenterRow extends GroupSessionRequest {
		@Nullable
		private UUID topicCenterRowId;

		@Nullable
		public UUID getTopicCenterRowId() {
			return this.topicCenterRowId;
		}

		public void setTopicCenterRowId(@Nullable UUID topicCenterRowId) {
			this.topicCenterRowId = topicCenterRowId;
		}
	}

	@NotThreadSafe
	protected static class PinboardNoteTopicCenterRow extends PinboardNote {
		@Nullable
		private UUID topicCenterRowId;

		@Nullable
		public UUID getTopicCenterRowId() {
			return this.topicCenterRowId;
		}

		public void setTopicCenterRowId(@Nullable UUID topicCenterRowId) {
			this.topicCenterRowId = topicCenterRowId;
		}
	}

	@NotThreadSafe
	protected static class ContentTopicCenterRow extends Content {
		@Nullable
		private UUID topicCenterRowId;

		@Nullable
		public UUID getTopicCenterRowId() {
			return this.topicCenterRowId;
		}

		public void setTopicCenterRowId(@Nullable UUID topicCenterRowId) {
			this.topicCenterRowId = topicCenterRowId;
		}
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected Strings getStrings() {
		return strings;
	}

	@Nonnull
	protected ContentService getContentService() {
		return this.contentServiceProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}
}
