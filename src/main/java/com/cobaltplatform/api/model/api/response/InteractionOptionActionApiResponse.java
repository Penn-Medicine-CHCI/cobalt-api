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

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.InteractionOption;
import com.cobaltplatform.api.model.db.InteractionOptionAction;
import com.cobaltplatform.api.service.AccountService;
import com.cobaltplatform.api.service.InteractionService;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class InteractionOptionActionApiResponse {
	@Nonnull
	private final UUID interactionOptionActionId;
	@Nonnull
	private final UUID interactionOptionId;
	@Nonnull
	private final UUID interactionInstanceId;
	@Nonnull
	private final UUID accountId;
	@Nonnull
	private final String description;
	@Nonnull
	private final String descriptionAsHtml;
	@Nonnull
	private final Instant created;
	@Nonnull
	private final String createdDescription;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface InteractionOptionActionApiResponseFactory {
		@Nonnull
		InteractionOptionActionApiResponse create(@Nonnull InteractionOptionAction interactionOptionAction);
	}

	@AssistedInject
	public InteractionOptionActionApiResponse(@Nonnull Formatter formatter,
																						@Nonnull Strings strings,
																						@Nonnull InteractionService interactionService,
																						@Nonnull AccountService accountService,
																						@Assisted @Nonnull InteractionOptionAction interactionOptionAction) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(interactionService);
		requireNonNull(accountService);
		requireNonNull(interactionOptionAction);

		InteractionOption interactionOption = interactionService.findInteractionOptionById(interactionOptionAction.getInteractionOptionId()).get();

		this.interactionOptionActionId = interactionOptionAction.getInteractionOptionActionId();
		this.interactionOptionId = interactionOption.getInteractionOptionId();
		this.interactionInstanceId = interactionOptionAction.getInteractionInstanceId();
		this.accountId = interactionOptionAction.getAccountId();
		this.created = interactionOptionAction.getCreated();
		this.createdDescription = formatter.formatTimestamp(interactionOptionAction.getCreated());

		Account account = accountService.findAccountById(accountId).get();

		this.description = createDescription(interactionOption, interactionOptionAction, account, strings, formatter);
		this.descriptionAsHtml = createDescriptionAsHtml(interactionOption, interactionOptionAction, account, strings, formatter);
	}

	@Nonnull
	protected String createDescription(@Nonnull InteractionOption interactionOption,
																		 @Nonnull InteractionOptionAction interactionOptionAction,
																		 @Nonnull Account account,
																		 @Nonnull Strings strings,
																		 @Nonnull Formatter formatter) {
		requireNonNull(interactionOption);
		requireNonNull(interactionOptionAction);
		requireNonNull(account);
		requireNonNull(strings);
		requireNonNull(formatter);

		String accountDescription;

		if (account.getDisplayName() != null && account.getEmailAddress() != null) {
			accountDescription = format("%s (%s)", account.getDisplayName(), account.getEmailAddress());
		} else if (account.getDisplayName() != null) {
			accountDescription = account.getDisplayName();
		} else if (account.getEmailAddress() != null) {
			accountDescription = account.getEmailAddress();
		} else {
			accountDescription = strings.get("Account ID {{accountId}}", new HashMap<String, Object>() {{
				put("accountId", accountId);
			}});
		}

		return strings.get("{{accountDescription}} selected \"{{optionDescription}}\" on {{timestampDescription}}.", new HashMap<String, Object>() {{
			put("accountDescription", accountDescription);
			put("optionDescription", interactionOption.getOptionDescription());
			put("timestampDescription", formatter.formatTimestamp(interactionOptionAction.getCreated()));
		}});
	}

	@Nonnull
	protected String createDescriptionAsHtml(@Nonnull InteractionOption interactionOption,
																					 @Nonnull InteractionOptionAction interactionOptionAction,
																					 @Nonnull Account account,
																					 @Nonnull Strings strings,
																					 @Nonnull Formatter formatter) {
		requireNonNull(interactionOption);
		requireNonNull(interactionOptionAction);
		requireNonNull(account);
		requireNonNull(strings);
		requireNonNull(formatter);

		String accountDescription;

		if (account.getDisplayName() != null && account.getEmailAddress() != null) {
			accountDescription = format("<a href='mailto:%s'>%s</a>", account.getEmailAddress(), account.getDisplayName());
		} else if (account.getDisplayName() != null) {
			accountDescription = account.getDisplayName();
		} else if (account.getEmailAddress() != null) {
			accountDescription = account.getEmailAddress();
		} else {
			accountDescription = strings.get("Account ID {{accountId}}", new HashMap<String, Object>() {{
				put("accountId", accountId);
			}});
		}

		return strings.get("<strong>{{accountDescription}}</strong> selected <strong>{{optionDescription}}</strong> on <strong>{{timestampDescription}}</strong>.", new HashMap<String, Object>() {{
			put("accountDescription", accountDescription);
			put("optionDescription", interactionOption.getOptionDescription());
			put("timestampDescription", formatter.formatTimestamp(interactionOptionAction.getCreated()));
		}});
	}

	@Nonnull
	public UUID getInteractionOptionActionId() {
		return interactionOptionActionId;
	}

	@Nonnull
	public UUID getInteractionOptionId() {
		return interactionOptionId;
	}

	@Nonnull
	public UUID getInteractionInstanceId() {
		return interactionInstanceId;
	}

	@Nonnull
	public UUID getAccountId() {
		return accountId;
	}

	@Nonnull
	public String getDescription() {
		return description;
	}

	@Nonnull
	public Instant getCreated() {
		return created;
	}

	@Nonnull
	public String getCreatedDescription() {
		return createdDescription;
	}
}