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

import com.cobaltplatform.api.messaging.email.EmailMessage;
import com.cobaltplatform.api.messaging.email.EmailMessageManager;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.api.request.CreateGroupRequestRequest;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.GroupRequest;
import com.cobaltplatform.api.model.db.GroupTopic;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.util.Formatter;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cobaltplatform.api.util.ValidationUtility.isValidEmailAddress;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class GroupRequestService {
	@Nonnull
	private final Provider<AccountService> accountServiceProvider;
	@Nonnull
	private final Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final EmailMessageManager emailMessageManager;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Database database;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public GroupRequestService(@Nonnull Provider<AccountService> accountServiceProvider,
														 @Nonnull Provider<InstitutionService> institutionServiceProvider,
														 @Nonnull EmailMessageManager emailMessageManager,
														 @Nonnull Formatter formatter,
														 @Nonnull Database database,
														 @Nonnull Strings strings) {
		requireNonNull(accountServiceProvider);
		requireNonNull(institutionServiceProvider);
		requireNonNull(emailMessageManager);
		requireNonNull(formatter);
		requireNonNull(database);
		requireNonNull(strings);

		this.accountServiceProvider = accountServiceProvider;
		this.institutionServiceProvider = institutionServiceProvider;
		this.emailMessageManager = emailMessageManager;
		this.formatter = formatter;
		this.database = database;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<GroupRequest> findGroupRequestById(@Nullable UUID groupRequestId) {
		if (groupRequestId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM group_request WHERE group_request_id=?",
				GroupRequest.class, groupRequestId);
	}

	@Nonnull
	public List<GroupTopic> findGroupTopicsByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM group_topic
				WHERE institution_id=?
				ORDER BY display_order
				""", GroupTopic.class, institutionId);
	}

	@Nonnull
	public Optional<GroupTopic> findGroupTopicById(@Nullable UUID groupTopicId) {
		if (groupTopicId == null)
			return Optional.empty();

		return getDatabase().queryForObject("SELECT * FROM group_topic WHERE group_topic_id=?",
				GroupTopic.class, groupTopicId);
	}

	@Nonnull
	public List<GroupTopic> findGroupTopicsByGroupRequestId(@Nullable UUID groupRequestId) {
		if (groupRequestId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT gt.*
				FROM group_topic gt, group_request_topic grt
				WHERE gt.group_topic_id=grt.group_topic_id
				AND grt.group_request_id=?
				ORDER BY gt.name
				""", GroupTopic.class, groupRequestId);
	}

	@Nonnull
	public List<String> findGroupRequestContactEmailAddressesByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT email_address
				FROM group_request_institution_contact
				WHERE institution_id=?
				ORDER BY email_address
				""", String.class, institutionId);
	}

	@Nonnull
	public UUID createGroupRequest(@Nonnull CreateGroupRequestRequest request) {
		requireNonNull(request);

		UUID requestorAccountId = request.getRequestorAccountId();
		String requestorName = trimToNull(request.getRequestorName());
		String requestorEmailAddress = trimToNull(request.getRequestorEmailAddress());
		String preferredDateDescription = trimToNull(request.getPreferredDateDescription());
		String preferredTimeDescription = trimToNull(request.getPreferredTimeDescription());
		String additionalDescription = trimToNull(request.getAdditionalDescription());
		String otherGroupTopicsDescription = trimToNull(request.getOtherGroupTopicsDescription());
		Integer minimumAttendeeCount = request.getMinimumAttendeeCount();
		Integer maximumAttendeeCount = request.getMaximumAttendeeCount();
		Set<UUID> groupTopicIds = request.getGroupTopicIds() == null
				? Set.of()
				: request.getGroupTopicIds().stream()
				.filter(groupTopicId -> groupTopicId != null)
				.collect(Collectors.toSet());
		ValidationException validationException = new ValidationException();
		UUID groupRequestId = UUID.randomUUID();
		Account requestorAccount = null;

		if (requestorAccountId == null) {
			validationException.add(new FieldError("requestorAccountId", getStrings().get("Account ID is required.")));
		} else {
			requestorAccount = getAccountService().findAccountById(requestorAccountId).orElse(null);

			if (requestorAccount == null)
				validationException.add(new FieldError("requestorAccountId", getStrings().get("Invalid Account ID specified.")));
		}

		if (requestorName == null)
			validationException.add(new FieldError("requestorName", getStrings().get("Name is required.")));

		if (requestorEmailAddress == null)
			validationException.add(new FieldError("requestorEmailAddress", getStrings().get("Email address is required.")));
		else if (!isValidEmailAddress(requestorEmailAddress))
			validationException.add(new FieldError("requestorEmailAddress", getStrings().get("Email address is invalid.")));

		if (minimumAttendeeCount == null)
			validationException.add(new FieldError("minimumAttendeeCount", getStrings().get("Minimum attendee count is required.")));
		else if (minimumAttendeeCount < 0)
			validationException.add(new FieldError("minimumAttendeeCount", getStrings().get("Minimum attendee count must be 0 or greater.")));

		if (minimumAttendeeCount != null && maximumAttendeeCount != null && maximumAttendeeCount < minimumAttendeeCount)
			validationException.add(getStrings().get("Maximum attendee count cannot be less than minimum attendee count."));

		List<GroupTopic> groupTopics = groupTopicIds.stream()
				.map(groupTopicId -> findGroupTopicById(groupTopicId).orElse(null))
				.filter(groupTopic -> groupTopic != null)
				.collect(Collectors.toList());

		// Ensure only topics that are enabled for the requestor's institution are included
		if (requestorAccount != null) {
			InstitutionId expectedInstitutionId = requestorAccount.getInstitutionId();
			groupTopics = groupTopics.stream()
					.filter(groupTopic -> groupTopic.getInstitutionId().equals(expectedInstitutionId))
					.collect(Collectors.toList());
		}

		if (groupTopics.size() == 0 && otherGroupTopicsDescription == null)
			validationException.add(new FieldError("groupTopicIds", getStrings().get("At least one topic must be specified.")));

		if (validationException.hasErrors())
			throw validationException;

		getDatabase().execute("""
						INSERT INTO group_request(group_request_id, requestor_account_id, requestor_name, requestor_email_address,
						preferred_date_description, preferred_time_description, additional_description, other_group_topics_description,
						minimum_attendee_count, maximum_attendee_count)
						VALUES (?,?,?,?,?,?,?,?,?,?)
						""", groupRequestId, requestorAccountId, requestorName, requestorEmailAddress,
				preferredDateDescription, preferredTimeDescription, additionalDescription, otherGroupTopicsDescription,
				minimumAttendeeCount, maximumAttendeeCount);

		for (GroupTopic groupTopic : groupTopics)
			getDatabase().execute("INSERT INTO group_request_topic(group_request_id, group_topic_id) VALUES (?,?)",
					groupRequestId, groupTopic.getGroupTopicId());

		// Send off an email to let administrators know a group request has been submitted
		Institution institution = getInstitutionService().findInstitutionById(requestorAccount.getInstitutionId()).get();
		List<String> groupRequestContactEmailAddresses = findGroupRequestContactEmailAddressesByInstitutionId(institution.getInstitutionId());

		if (groupRequestContactEmailAddresses.size() == 0)
			throw new IllegalStateException(format("No group request contact email addresses configured for institution ID %s",
					requestorAccount.getInstitutionId().name()));

		// Ensure mutable list via `new ArrayList<>()` so we can sort afterwards
		List<String> groupTopicNames = new ArrayList<>(groupTopics.stream()
				.map(groupTopic -> groupTopic.getName())
				.collect(Collectors.toList()));

		if (otherGroupTopicsDescription != null)
			groupTopicNames.add(getStrings().get("Other: {{otherGroupTopicsDescription}}", Map.of("otherGroupTopicsDescription", otherGroupTopicsDescription)));

		Collections.sort(groupTopicNames);

		Locale emailLocale = institution.getLocale();

		Map<String, Object> messageContext = new HashMap<>();
		messageContext.put("requestorName", requestorName);
		messageContext.put("requestorEmailAddress", requestorEmailAddress);
		messageContext.put("preferredDateDescription", preferredDateDescription == null ? getStrings().get("Not specified") : preferredDateDescription);
		messageContext.put("preferredTimeDescription", preferredTimeDescription == null ? getStrings().get("Not specified") : preferredTimeDescription);
		messageContext.put("additionalDescription", additionalDescription == null ? getStrings().get("Not specified") : additionalDescription);
		messageContext.put("minimumAttendeeCount", getFormatter().formatNumber(minimumAttendeeCount, emailLocale));
		messageContext.put("maximumAttendeeCount", maximumAttendeeCount == null ? getStrings().get("Not specified") : getFormatter().formatNumber(maximumAttendeeCount, emailLocale));
		messageContext.put("groupTopicNames", groupTopicNames);

		getDatabase().currentTransaction().get().addPostCommitOperation(() -> {
			getEmailMessageManager().enqueueMessage(new EmailMessage.Builder(institution.getInstitutionId(), EmailMessageTemplate.GROUP_REQUEST_SUBMITTED, emailLocale)
					.toAddresses(groupRequestContactEmailAddresses)
					.messageContext(messageContext)
					.build());
		});

		return groupRequestId;
	}

	@Nonnull
	protected AccountService getAccountService() {
		return this.accountServiceProvider.get();
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
	}

	@Nonnull
	protected EmailMessageManager getEmailMessageManager() {
		return this.emailMessageManager;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return this.formatter;
	}

	@Nonnull
	protected Database getDatabase() {
		return this.database;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
