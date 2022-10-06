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

import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.db.GroupSession;
import com.cobaltplatform.api.model.db.GroupSessionRequest;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionTopicCenter;
import com.cobaltplatform.api.model.db.PinboardNote;
import com.cobaltplatform.api.model.db.TopicCenter;
import com.cobaltplatform.api.model.service.NavigationItem;
import com.cobaltplatform.api.model.service.TopicCenterRowDetail;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class TopicCenterService {
	@Nonnull
	private final Database database;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public TopicCenterService(@Nonnull Database database,
														@Nonnull Strings strings) {
		requireNonNull(database);
		requireNonNull(strings);

		this.database = database;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
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
				SELECT itc.navigation_icon_name as icon_name, tc.url_name as url, tc.name as name
				FROM institution_topic_center itc, topic_center tc
				WHERE itc.topic_center_id=tc.topic_center_id
				AND itc.navigation_item_enabled=TRUE
				ORDER BY itc.navigation_display_order
				""", NavigationItem.class);
	}

	@Nonnull
	public List<TopicCenterRowDetail> findTopicCenterRowsByTopicCenterId(@Nullable UUID topicCenterId) {
		if (topicCenterId == null)
			return Collections.emptyList();

		List<TopicCenterRowDetail> topicCenterRows = getDatabase().queryForList("""
				    SELECT *
				    FROM topic_center_row
				    WHERE topic_center_id=?
				    ORDER BY display_order
				""", TopicCenterRowDetail.class, topicCenterId);

		// TODO: later, we need to support pulling data by tags as well

		// Pull all of the group sessions across all rows in the topic center (so we don't have to query for each row individually)
		List<GroupSessionTopicCenterRow> groupSessionTopicCenterRows = getDatabase().queryForList("""
				SELECT gs.*, tcr.topic_center_row_id
				FROM topic_center_row tcr, v_group_session gs, topic_center_row_group_session tcrgs
				WHERE tcr.topic_center_id=?
				AND tcr.topic_center_row_id=tcrgs.topic_center_row_id
				AND tcrgs.group_session_id=gs.group_session_id
				ORDER BY tcr.display_order, tcrgs.display_order
				""", GroupSessionTopicCenterRow.class, topicCenterId);

		// Pull all of the group sessions across all rows in the topic center (so we don't have to query for each row individually)
		List<GroupSessionRequestTopicCenterRow> groupSessionRequestTopicCenterRows = getDatabase().queryForList("""
				SELECT gsr.*, tcr.topic_center_row_id
				FROM topic_center_row tcr, v_group_session_request gsr, topic_center_row_group_session_request tcrgsr
				WHERE tcr.topic_center_id=?
				AND tcr.topic_center_row_id=tcrgsr.topic_center_row_id
				AND tcrgsr.group_session_request_id=gsr.group_session_request_id
				ORDER BY tcr.display_order, tcrgsr.display_order
				""", GroupSessionRequestTopicCenterRow.class, topicCenterId);

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
				FROM topic_center_row tcr, content c, topic_center_row_content tcrc
				WHERE tcr.topic_center_id=?
				AND tcr.topic_center_row_id=tcrc.topic_center_row_id
				AND tcrc.content_id=c.content_id
				ORDER BY tcr.display_order, tcrc.display_order
				""", ContentTopicCenterRow.class, topicCenterId);

		for (TopicCenterRowDetail topicCenterRow : topicCenterRows) {
			topicCenterRow.setGroupSessions(new ArrayList<>());
			topicCenterRow.setGroupSessionRequests(new ArrayList<>());
			topicCenterRow.setPinboardNotes(new ArrayList<>());
			topicCenterRow.setContents(new ArrayList<>());

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
		}

		return topicCenterRows;
	}

	@Nonnull
	protected Optional<String> normalizeUrlName(@Nullable String urlName) {
		urlName = trimToEmpty(urlName).toLowerCase(Locale.US);
		return urlName.length() == 0 ? Optional.empty() : Optional.of(urlName);
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
		return database;
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
