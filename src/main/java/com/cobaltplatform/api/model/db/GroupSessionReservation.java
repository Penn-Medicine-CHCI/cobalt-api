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

package com.cobaltplatform.api.model.db;

import com.cobaltplatform.api.model.db.FollowupEmailStatus.FollowupEmailStatusId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class GroupSessionReservation {
	@Nullable
	private UUID groupSessionReservationId;
	@Nullable
	private UUID groupSessionId;
	@Nullable
	private UUID accountId;
	@Nullable
	private UUID attendeeReminderScheduledMessageId;
	@Nullable
	private UUID attendeeFollowupScheduledMessageId;
	@Nullable
	@Deprecated
	private FollowupEmailStatusId followupEmailStatusId;
	@Nullable
	@Deprecated
	private Instant followupEmailSentTimestamp;
	@Nullable
	private String firstName; // Joined in via DB view
	@Nullable
	private String lastName; // Joined in via DB view
	@Nullable
	private String emailAddress; // Joined in via DB view
	@Nullable
	private String phoneNumber; // Joined in via DB view
	@Nullable
	private Boolean canceled;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getGroupSessionReservationId() {
		return groupSessionReservationId;
	}

	public void setGroupSessionReservationId(@Nullable UUID groupSessionReservationId) {
		this.groupSessionReservationId = groupSessionReservationId;
	}

	@Nullable
	public UUID getGroupSessionId() {
		return groupSessionId;
	}

	public void setGroupSessionId(@Nullable UUID groupSessionId) {
		this.groupSessionId = groupSessionId;
	}

	@Nullable
	public UUID getAccountId() {
		return accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public UUID getAttendeeReminderScheduledMessageId() {
		return this.attendeeReminderScheduledMessageId;
	}

	public void setAttendeeReminderScheduledMessageId(@Nullable UUID attendeeReminderScheduledMessageId) {
		this.attendeeReminderScheduledMessageId = attendeeReminderScheduledMessageId;
	}

	@Nullable
	public UUID getAttendeeFollowupScheduledMessageId() {
		return this.attendeeFollowupScheduledMessageId;
	}

	public void setAttendeeFollowupScheduledMessageId(@Nullable UUID attendeeFollowupScheduledMessageId) {
		this.attendeeFollowupScheduledMessageId = attendeeFollowupScheduledMessageId;
	}

	@Nullable
	@Deprecated
	public FollowupEmailStatusId getFollowupEmailStatusId() {
		return followupEmailStatusId;
	}

	@Deprecated
	public void setFollowupEmailStatusId(@Nullable FollowupEmailStatusId followupEmailStatusId) {
		this.followupEmailStatusId = followupEmailStatusId;
	}

	@Nullable
	@Deprecated
	public Instant getFollowupEmailSentTimestamp() {
		return followupEmailSentTimestamp;
	}

	@Deprecated
	public void setFollowupEmailSentTimestamp(@Nullable Instant followupEmailSentTimestamp) {
		this.followupEmailSentTimestamp = followupEmailSentTimestamp;
	}

	@Nullable
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(@Nullable String firstName) {
		this.firstName = firstName;
	}

	@Nullable
	public String getLastName() {
		return lastName;
	}

	public void setLastName(@Nullable String lastName) {
		this.lastName = lastName;
	}

	@Nullable
	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(@Nullable String emailAddress) {
		this.emailAddress = emailAddress;
	}

	@Nullable
	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(@Nullable String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@Nullable
	public Boolean getCanceled() {
		return canceled;
	}

	public void setCanceled(@Nullable Boolean canceled) {
		this.canceled = canceled;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}