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

import com.google.gson.Gson;
import com.cobaltplatform.api.model.db.AuditLog;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class AuditLogService {
	@Nonnull
	private final Database database;
	@Nonnull
	private final Logger logger;

	@Inject
	public AuditLogService(@Nonnull Database database,
												 @Nonnull Gson gson) {
		requireNonNull(database);

		this.database = database;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public void audit(@Nonnull AuditLog auditLog) {
		requireNonNull(auditLog);

		getDatabase().execute("INSERT INTO audit_log (audit_log_id, audit_log_event_id, account_id, message, payload) " +
						" VALUES (?,?,?,?,CAST(? AS JSONB))", UUID.randomUUID(), auditLog.getAuditLogEventId(), auditLog.getAccountId(), auditLog.getMessage(),
				auditLog.getPayload());
	}

	@Nonnull
	public void auditInSeparateTransaction(@Nonnull AuditLog auditLog) {
		requireNonNull(auditLog);

		getDatabase().transaction(() -> {
			audit(auditLog);
		});
	}

	@Nonnull
	protected Database getDatabase() {
		return database;
	}

	@Nonnull
	protected Logger getLogger() {
		return logger;
	}

}
