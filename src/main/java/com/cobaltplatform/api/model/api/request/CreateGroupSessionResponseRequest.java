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

package com.cobaltplatform.api.model.api.request;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDate;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class CreateGroupSessionResponseRequest {
	@Nullable
	private UUID groupSessionRequestId;
	@Nullable
	private UUID respondentAccountId;
	@Nullable
	private String respondentName;
	@Nullable
	private String respondentEmailAddress;
	@Nullable
	private String respondentPhoneNumber;
	@Nullable
	private LocalDate suggestedDate;
	@Nullable
	private String suggestedTime;
	@Nullable
	private String expectedParticipants;
	@Nullable
	private String notes;
	@Nullable
	private String customAnswer1;
	@Nullable
	private String customAnswer2;

	@Nullable
	public UUID getGroupSessionRequestId() {
		return groupSessionRequestId;
	}

	public void setGroupSessionRequestId(@Nullable UUID groupSessionRequestId) {
		this.groupSessionRequestId = groupSessionRequestId;
	}

	@Nullable
	public UUID getRespondentAccountId() {
		return respondentAccountId;
	}

	public void setRespondentAccountId(@Nullable UUID respondentAccountId) {
		this.respondentAccountId = respondentAccountId;
	}

	@Nullable
	public String getRespondentName() {
		return respondentName;
	}

	public void setRespondentName(@Nullable String respondentName) {
		this.respondentName = respondentName;
	}

	@Nullable
	public String getRespondentEmailAddress() {
		return respondentEmailAddress;
	}

	public void setRespondentEmailAddress(@Nullable String respondentEmailAddress) {
		this.respondentEmailAddress = respondentEmailAddress;
	}

	@Nullable
	public String getRespondentPhoneNumber() {
		return respondentPhoneNumber;
	}

	public void setRespondentPhoneNumber(@Nullable String respondentPhoneNumber) {
		this.respondentPhoneNumber = respondentPhoneNumber;
	}

	@Nullable
	public LocalDate getSuggestedDate() {
		return suggestedDate;
	}

	public void setSuggestedDate(@Nullable LocalDate suggestedDate) {
		this.suggestedDate = suggestedDate;
	}

	@Nullable
	public String getSuggestedTime() {
		return suggestedTime;
	}

	public void setSuggestedTime(@Nullable String suggestedTime) {
		this.suggestedTime = suggestedTime;
	}

	@Nullable
	public String getExpectedParticipants() {
		return expectedParticipants;
	}

	public void setExpectedParticipants(@Nullable String expectedParticipants) {
		this.expectedParticipants = expectedParticipants;
	}

	@Nullable
	public String getNotes() {
		return notes;
	}

	public void setNotes(@Nullable String notes) {
		this.notes = notes;
	}

	@Nullable
	public String getCustomAnswer1() {
		return customAnswer1;
	}

	public void setCustomAnswer1(@Nullable String customAnswer1) {
		this.customAnswer1 = customAnswer1;
	}

	@Nullable
	public String getCustomAnswer2() {
		return customAnswer2;
	}

	public void setCustomAnswer2(@Nullable String customAnswer2) {
		this.customAnswer2 = customAnswer2;
	}
}
