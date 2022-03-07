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

import static java.lang.String.format;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class AuditLogEvent {
	@Nullable
	private AuditLogEventId auditLogEventId;
	@Nullable
	private String description;

	public enum AuditLogEventId {
		PATIENT_CREATE,
		PATIENT_SEARCH,
		APPOINTMENT_CREATE,
		APPOINTMENT_CANCEL,
		APPOINTMENT_LOOKUP,
		APPOINTMENT_UPDATE,
		ACCOUNT_LOOKUP_SUCCESS,
		ACCOUNT_LOOKUP_FAILURE,
		ACCOUNT_CREATE,
		ACCOUNT_COBALT_SAML_ASSERTION,
		ACCOUNT_COBALT_SAML_LOGOUT,
		ACCOUNT_ROLE_REQUEST,
		EPIC_MATCH_ATTEMPT,
		EPIC_ACCOUNT_CREATE,
		EPIC_ACCOUNT_ASSOCIATE,
		EPIC_ACCOUNT_MULTIPLE_MATCHES,
		EPIC_APPOINTMENT_CREATE,
		EPIC_APPOINTMENT_CANCEL,
		EPIC_APPOINTMENT_IMPLICIT_CREATE,
		EPIC_APPOINTMENT_IMPLICIT_CANCEL,
		EPIC_APPOINTMENT_LOOKUP,
		EPIC_ERROR
	}

	@Override
	public String toString() {
		return format("%s{auditLogEventId=%s, description=%s}", getClass().getSimpleName(), getAuditLogEventId(), getDescription());
	}

	@Nullable
	public AuditLogEventId getAuditLogEventId() {
		return auditLogEventId;
	}

	public void setAuditLogEventId(@Nullable AuditLogEventId auditLogEventId) {
		this.auditLogEventId = auditLogEventId;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}
}