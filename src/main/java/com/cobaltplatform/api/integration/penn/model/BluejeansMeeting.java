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

package com.cobaltplatform.api.integration.penn.model;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDateTime;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class BluejeansMeeting {
	@Nullable
	private Long csn;
	@Nullable
	private String meetingId;
	@Nullable
	private Long internalMeetingId;
	@Nullable
	private LocalDateTime meetingStart;
	@Nullable
	private LocalDateTime meetingEnd;
	@Nullable
	private LocalDateTime meetingCreated;
	@Nullable
	private String passCode;

	@Nullable
	public Long getCsn() {
		return csn;
	}

	public void setCsn(@Nullable Long csn) {
		this.csn = csn;
	}

	@Nullable
	public String getMeetingId() {
		return meetingId;
	}

	public void setMeetingId(@Nullable String meetingId) {
		this.meetingId = meetingId;
	}

	@Nullable
	public Long getInternalMeetingId() {
		return internalMeetingId;
	}

	public void setInternalMeetingId(@Nullable Long internalMeetingId) {
		this.internalMeetingId = internalMeetingId;
	}

	@Nullable
	public LocalDateTime getMeetingStart() {
		return meetingStart;
	}

	public void setMeetingStart(@Nullable LocalDateTime meetingStart) {
		this.meetingStart = meetingStart;
	}

	@Nullable
	public LocalDateTime getMeetingEnd() {
		return meetingEnd;
	}

	public void setMeetingEnd(@Nullable LocalDateTime meetingEnd) {
		this.meetingEnd = meetingEnd;
	}

	@Nullable
	public LocalDateTime getMeetingCreated() {
		return meetingCreated;
	}

	public void setMeetingCreated(@Nullable LocalDateTime meetingCreated) {
		this.meetingCreated = meetingCreated;
	}

	@Nullable
	public String getPassCode() {
		return passCode;
	}

	public void setPassCode(@Nullable String passCode) {
		this.passCode = passCode;
	}
}
