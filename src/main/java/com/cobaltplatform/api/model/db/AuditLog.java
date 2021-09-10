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

import com.cobaltplatform.api.model.db.AuditLogEvent.AuditLogEventId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.UUID;

/**
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class AuditLog {
	@Nullable
	private UUID auditLogId;
	@Nullable
	private AuditLogEventId auditLogEventId;
	@Nullable
	private UUID accountId;
	@Nullable
	private String message;
	@Nullable
	private String payload;

	@Nullable
	public UUID getAuditLogId() {
		return auditLogId;
	}

	public void setAuditLogId(@Nullable UUID auditLogId) {
		this.auditLogId = auditLogId;
	}

	@Nullable
	public AuditLogEventId getAuditLogEventId() {
		return auditLogEventId;
	}

	public void setAuditLogEventId(@Nullable AuditLogEventId auditLogEventId) {
		this.auditLogEventId = auditLogEventId;
	}

	@Nullable
	public UUID getAccountId() {
		return accountId;
	}

	public void setAccountId(@Nullable UUID accountId) {
		this.accountId = accountId;
	}

	@Nullable
	public String getMessage() {
		return message;
	}

	public void setMessage(@Nullable String message) {
		this.message = message;
	}

	@Nullable
	public String getPayload() {
		return payload;
	}

	public void setPayload(@Nullable String payload) {
		this.payload = payload;
	}
}