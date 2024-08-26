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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class FootprintEventGroupType {
	@Nullable
	private FootprintEventGroupTypeId footprintEventGroupTypeId;
	@Nullable
	private String description;

	public enum FootprintEventGroupTypeId {
		UNSPECIFIED,
		// Content
		CONTENT_CREATE,
		CONTENT_UPDATE,
		CONTENT_DELETE,
		CONTENT_PUBLISH,
		// Group Sessions
		GROUP_SESSION_CREATE,
		GROUP_SESSION_UPDATE,
		GROUP_SESSION_UPDATE_STATUS,
		GROUP_SESSION_RESERVATION_CREATE,
		GROUP_SESSION_RESERVATION_CANCEL,
		// Appointments
		APPOINTMENT_CREATE,
		APPOINTMENT_CREATE_MESSAGES,
		APPOINTMENT_CANCEL,
		APPOINTMENT_RESCHEDULE,
		// Patient Orders
		PATIENT_ORDER_UPDATE_PANEL_ACCOUNT,
		PATIENT_ORDER_UPDATE_DISPOSITION,
		PATIENT_ORDER_UPDATE_CONSENT,
		PATIENT_ORDER_UPDATE_RESOURCE_CHECK_IN_RESPONSE,
		PATIENT_ORDER_UPDATE_RESOURCING,
		PATIENT_ORDER_UPDATE_SAFETY_PLANNING,
		PATIENT_ORDER_UPDATE_ENCOUNTER,
		// Patient Order Imports
		PATIENT_ORDER_IMPORT_CREATE,
		// Patient Order Notes
		PATIENT_ORDER_NOTE_CREATE,
		PATIENT_ORDER_NOTE_UPDATE,
		PATIENT_ORDER_NOTE_DELETE,
		// Patient Order Voicemail Tasks
		PATIENT_ORDER_VOICEMAIL_TASK_CREATE,
		PATIENT_ORDER_VOICEMAIL_TASK_UPDATE,
		PATIENT_ORDER_VOICEMAIL_TASK_DELETE,
		PATIENT_ORDER_VOICEMAIL_TASK_COMPLETE,
		// Patient Order Scheduled Outreaches
		PATIENT_ORDER_SCHEDULED_OUTREACH_CREATE,
		PATIENT_ORDER_SCHEDULED_OUTREACH_UPDATE,
		PATIENT_ORDER_SCHEDULED_OUTREACH_CANCEL,
		PATIENT_ORDER_SCHEDULED_OUTREACH_COMPLETE,
		// Patient Order Outreaches
		PATIENT_ORDER_OUTREACH_CREATE,
		PATIENT_ORDER_OUTREACH_UPDATE,
		PATIENT_ORDER_OUTREACH_DELETE,
		// Patient Order Scheduled Message Groups
		PATIENT_ORDER_SCHEDULED_MESSAGE_GROUP_CREATE,
		PATIENT_ORDER_SCHEDULED_MESSAGE_GROUP_UPDATE,
		PATIENT_ORDER_SCHEDULED_MESSAGE_GROUP_DELETE,
		// Screening Answers
		SCREENING_ANSWER_CREATE
	}

	@Override
	public String toString() {
		return String.format("%s{footprintEventGroupTypeId=%s, description=%s}", getClass().getSimpleName(), getFootprintEventGroupTypeId(), getDescription());
	}

	@Nullable
	public FootprintEventGroupTypeId getFootprintEventGroupTypeId() {
		return this.footprintEventGroupTypeId;
	}

	public void setFootprintEventGroupTypeId(@Nullable FootprintEventGroupTypeId footprintEventGroupTypeId) {
		this.footprintEventGroupTypeId = footprintEventGroupTypeId;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}
}
