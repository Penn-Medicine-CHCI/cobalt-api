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

package com.cobaltplatform.api.model.service;

import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.db.GroupSession;
import com.cobaltplatform.api.model.db.GroupSessionRequest;
import com.cobaltplatform.api.model.db.PinboardNote;
import com.cobaltplatform.api.model.db.TopicCenterRow;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class TopicCenterRowDetail extends TopicCenterRow {
	@Nullable
	private List<GroupSession> groupSessions;
	@Nullable
	private List<GroupSessionRequest> groupSessionRequests;
	@Nullable
	private List<PinboardNote> pinboardNotes;
	@Nullable
	private List<Content> contents;

	@Nullable
	public List<GroupSession> getGroupSessions() {
		return this.groupSessions;
	}

	public void setGroupSessions(@Nullable List<GroupSession> groupSessions) {
		this.groupSessions = groupSessions;
	}

	@Nullable
	public List<GroupSessionRequest> getGroupSessionRequests() {
		return this.groupSessionRequests;
	}

	public void setGroupSessionRequests(@Nullable List<GroupSessionRequest> groupSessionRequests) {
		this.groupSessionRequests = groupSessionRequests;
	}

	@Nullable
	public List<PinboardNote> getPinboardNotes() {
		return this.pinboardNotes;
	}

	public void setPinboardNotes(@Nullable List<PinboardNote> pinboardNotes) {
		this.pinboardNotes = pinboardNotes;
	}

	@Nullable
	public List<Content> getContents() {
		return this.contents;
	}

	public void setContents(@Nullable List<Content> contents) {
		this.contents = contents;
	}
}
