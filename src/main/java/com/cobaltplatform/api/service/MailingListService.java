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

import com.cobaltplatform.api.model.api.request.CreateMailingListEntryRequest;
import com.cobaltplatform.api.model.api.request.CreateMailingListRequest;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.MailingList;
import com.cobaltplatform.api.model.db.MailingListEntry;
import com.cobaltplatform.api.model.db.MailingListEntryType.MailingListEntryTypeId;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.ValidationUtility;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
import com.pyranid.DatabaseException;
import com.pyranid.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Savepoint;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class MailingListService {
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public MailingListService(@Nonnull DatabaseProvider databaseProvider,
														@Nonnull Strings strings) {
		requireNonNull(databaseProvider);
		requireNonNull(strings);

		this.databaseProvider = databaseProvider;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<MailingList> findMailingListById(@Nullable UUID mailingListId) {
		if (mailingListId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM mailing_list
				WHERE mailing_list_id=?
				""", MailingList.class, mailingListId);
	}

	@Nonnull
	public Optional<MailingListEntry> findMailingListEntryById(@Nullable UUID mailingListEntryId) {
		if (mailingListEntryId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM mailing_list_entry
				WHERE mailing_list_entry_id=?
				""", MailingListEntry.class, mailingListEntryId);
	}

	@Nonnull
	public UUID createMailingList(@Nonnull CreateMailingListRequest request) {
		requireNonNull(request);

		InstitutionId institutionId = request.getInstitutionId();
		UUID createdByAccountId = request.getCreatedByAccountId();
		UUID mailingListId = UUID.randomUUID();
		ValidationException validationException = new ValidationException();

		if (institutionId == null)
			validationException.add(new FieldError("institutionId", getStrings().get("Institution ID is required.")));

		if (createdByAccountId == null)
			validationException.add(new FieldError("createdByAccountId", getStrings().get("Created-By Account ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
						INSERT INTO mailing_list (
							mailing_list_id,
							institution_id,
							created_by_account_id
						)
						VALUES (?,?,?)
						""",
				mailingListId,
				institutionId,
				createdByAccountId
		);

		return mailingListId;
	}

	@Nonnull
	public UUID createMailingListEntry(@Nonnull CreateMailingListEntryRequest request) {
		requireNonNull(request);

		UUID mailingListId = request.getMailingListId();
		MailingListEntryTypeId mailingListEntryTypeId = request.getMailingListEntryTypeId();
		UUID accountId = request.getAccountId();
		UUID createdByAccountId = request.getCreatedByAccountId();
		String value = trimToNull(request.getValue());
		UUID mailingListEntryId = UUID.randomUUID();
		ValidationException validationException = new ValidationException();

		if (mailingListId == null)
			validationException.add(new FieldError("mailingListId", getStrings().get("Mailing List ID is required.")));

		if (mailingListEntryTypeId == null)
			validationException.add(new FieldError("mailingListEntryTypeId", getStrings().get("Mailing List Entry Type ID is required.")));

		if (accountId == null)
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));

		if (createdByAccountId == null)
			validationException.add(new FieldError("createdByAccountId", getStrings().get("Created-By Account ID is required.")));

		if (value == null) {
			validationException.add(new FieldError("value", getStrings().get("Value is required.")));
		} else if (mailingListEntryTypeId != null) {
			if (mailingListEntryTypeId == MailingListEntryTypeId.EMAIL_ADDRESS) {
				if (!ValidationUtility.isValidEmailAddress(value))
					validationException.add(new FieldError("value", getStrings().get("Sorry, this is not a valid email address.")));
			} else {
				// We don't yet support SMS or other types
				throw new IllegalStateException(format("Unsupported %s.%s value specified", MailingListEntryTypeId.class.getSimpleName(), mailingListEntryTypeId.name()));
			}
		}

		if (validationException.hasErrors())
			throw validationException;

		// Normalize value, e.g. email address
		value = value.toLowerCase(Locale.ROOT);

		Transaction transaction = getDatabase().currentTransaction().get();
		Savepoint savepoint = transaction.createSavepoint();

		try {
			getDatabase().execute("""
							INSERT INTO mailing_list_entry (
								mailing_list_entry_id,
								mailing_list_id,
								mailing_list_entry_type_id,
								account_id,
								created_by_account_id,
								value
							)
							VALUES (?,?,?,?,?,?)
							""",
					mailingListEntryId,
					mailingListId,
					mailingListEntryTypeId,
					accountId,
					createdByAccountId,
					value
			);

			return mailingListEntryId;
		} catch (DatabaseException e) {
			if ("mailing_list_entry_value_unique_idx".equals(e.constraint().orElse(null))) {
				getLogger().info("Value '{}' already in use for {} on Mailing List ID {}, not re-saving.",
						value, mailingListEntryTypeId.name(), mailingListId);
				transaction.rollback(savepoint);

				// Pull back the existing value
				return getDatabase().queryForObject("""
						SELECT *
						FROM mailing_list_entry
						WHERE mailing_list_id=?						
						AND mailing_list_entry_type_id=?
						AND value=?
						""", MailingListEntry.class, mailingListId, mailingListEntryTypeId, value).get().getMailingListEntryId();
			} else {
				throw e;
			}
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
	protected Logger getLogger() {
		return logger;
	}
}
