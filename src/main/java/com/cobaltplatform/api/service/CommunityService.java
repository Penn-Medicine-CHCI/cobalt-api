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
import com.cobaltplatform.api.messaging.email.EmailMessageContextKey;
import com.cobaltplatform.api.messaging.email.EmailMessageTemplate;
import com.cobaltplatform.api.model.api.request.CreateCommunitySubscriberNotificationRequest;
import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.db.GroupSession;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.MailingListEntry;
import com.cobaltplatform.api.model.db.MailingListEntryType.MailingListEntryTypeId;
import com.cobaltplatform.api.model.db.Page;
import com.cobaltplatform.api.model.db.PageGroup;
import com.cobaltplatform.api.model.db.PageGroupEmailContent;
import com.cobaltplatform.api.model.db.PageGroupEmailGroupSession;
import com.cobaltplatform.api.model.db.PageSection;
import com.cobaltplatform.api.model.db.RowType.RowTypeId;
import com.cobaltplatform.api.model.db.UserExperienceType.UserExperienceTypeId;
import com.cobaltplatform.api.service.MailingListService.MailingListEntryStatusFilter;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.cobaltplatform.api.util.ValidationUtility;
import com.cobaltplatform.api.util.WebUtility;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.devskiller.friendly_id.FriendlyId;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class CommunityService {
	@Nonnull
	private static final String COMMUNITY_HIGHLIGHTS_PLATFORM_EMAIL_IMAGE_URL_FORMAT = "https://cobalt-prod-media.s3.us-east-1.amazonaws.com/prod/logos/email-v2/%s.png";
	@Nonnull
	private static final String COMMUNITY_HIGHLIGHTS_UTM_SOURCE = "cobalt";
	@Nonnull
	private static final String COMMUNITY_HIGHLIGHTS_UTM_MEDIUM = "subscription";
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Provider<PageService> pageServiceProvider;
	@Nonnull
	private final Provider<MailingListService> mailingListServiceProvider;
	@Nonnull
	private final Provider<InstitutionService> institutionServiceProvider;
	@Nonnull
	private final Provider<MessageService> messageServiceProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public CommunityService(@Nonnull DatabaseProvider databaseProvider,
													@Nonnull Provider<PageService> pageServiceProvider,
													@Nonnull Provider<MailingListService> mailingListServiceProvider,
													@Nonnull Provider<InstitutionService> institutionServiceProvider,
													@Nonnull Provider<MessageService> messageServiceProvider,
													@Nonnull Strings strings) {
		requireNonNull(databaseProvider);
		requireNonNull(pageServiceProvider);
		requireNonNull(mailingListServiceProvider);
		requireNonNull(institutionServiceProvider);
		requireNonNull(messageServiceProvider);
		requireNonNull(strings);

		this.databaseProvider = databaseProvider;
		this.pageServiceProvider = pageServiceProvider;
		this.mailingListServiceProvider = mailingListServiceProvider;
		this.institutionServiceProvider = institutionServiceProvider;
		this.messageServiceProvider = messageServiceProvider;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	public void notifySubscribers(@Nonnull UUID pageGroupId) {
		notifySubscribers(pageGroupId, null);
	}

	public void notifySubscribers(@Nonnull CreateCommunitySubscriberNotificationRequest request) {
		requireNonNull(request);

		UUID pageGroupId = request.getPageGroupId();
		List<String> overrideEmailAddresses = request.getOverrideEmailAddresses();
		boolean force = request.getForce() != null && request.getForce();
		ValidationException validationException = new ValidationException();

		if (pageGroupId == null)
			validationException.add(new FieldError("pageGroupId", getStrings().get("Page Group ID is required.")));

		if (validationException.hasErrors())
			throw validationException;

		NotifySubscribersPreview preview = previewNotifySubscribers(pageGroupId, overrideEmailAddresses);

		// Match screening-style confirmation behavior: first call prompts with metadata, second call sets force=true.
		if (!force
				&& preview.getRecipientEmailAddresses().size() > 0
				&& preview.getUpcomingGroupSessionCount() > 0) {
			ValidationException confirmRecipientsValidationException = new ValidationException(
					getStrings().get("Please confirm recipient email addresses before sending.")
			);

			confirmRecipientsValidationException.setMetadata(Map.of(
					"communitySubscriberNotification", Map.of(
							"pageGroupId", preview.getPageGroupId(),
							"pageTitle", preview.getPageTitle(),
							"overrideEmailAddressesApplied", preview.getOverrideEmailAddressesApplied(),
							"recipientEmailAddresses", preview.getRecipientEmailAddresses(),
							"recipientCount", preview.getRecipientEmailAddresses().size(),
							"upcomingGroupSessionCount", preview.getUpcomingGroupSessionCount(),
							"forceRequired", true
					)
			));

			throw confirmRecipientsValidationException;
		}

		notifySubscribers(pageGroupId, overrideEmailAddresses);
	}

	public void notifySubscribers(@Nonnull UUID pageGroupId,
																@Nullable List<String> overrideEmailAddresses) {
		requireNonNull(pageGroupId);

		NotifySubscribersContext context = createNotifySubscribersContext(pageGroupId, overrideEmailAddresses);

		if (context.getEmailRecipients().isEmpty()) {
			getLogger().info("No recipient email addresses found for page group ID {}, nothing to notify.", pageGroupId);
			return;
		}

		if (context.getUpcomingGroupSessionCount() == 0) {
			getLogger().info("No upcoming group sessions found for page group ID {}, skipping subscriber notifications.", pageGroupId);
			return;
		}

		int emailMessagesEnqueued = 0;

		for (EmailRecipient emailRecipient : context.getEmailRecipients()) {
			UUID messageId = UUID.randomUUID();
			Map<String, Object> messageContext = new HashMap<>(context.getBaseMessageContext());
			messageContext.put("recipientEmailAddress", emailRecipient.getEmailAddress());

			if (emailRecipient.getCommunicationPreferencesUrl().isPresent())
				messageContext.put("communicationPreferencesUrl", emailRecipient.getCommunicationPreferencesUrl().get());

			addMessageTrackingToV2CommunityHighlightsContext(messageContext, messageId);

			EmailMessage emailMessage = new EmailMessage.Builder(messageId, context.getInstitutionId(), EmailMessageTemplate.V2_COMMUNITY_HIGHLIGHTS, context.getLocale())
					.toAddresses(List.of(emailRecipient.getEmailAddress()))
					.messageContext(messageContext)
					.build();

			getMessageService().enqueueMessage(emailMessage);
			emailMessagesEnqueued++;
		}

		getLogger().info("Notified subscribers for page group ID {}. emails={}, invalidEmails={}, skippedSms={}, sessions={}, overrideEmailAddressesApplied={}",
				pageGroupId, emailMessagesEnqueued, context.getInvalidEmailEntries(), context.getSmsEntriesSkipped(),
				context.getUpcomingGroupSessionCount(), context.getOverrideEmailAddressesApplied());
	}

	protected void addMessageTrackingToV2CommunityHighlightsContext(@Nonnull Map<String, Object> messageContext,
																																 @Nonnull UUID messageId) {
		requireNonNull(messageContext);
		requireNonNull(messageId);

		addMessageTrackingToContextUrl(messageContext, "communityPageUrl", messageId);
		addMessageTrackingToContextUrl(messageContext, "recordingUrl", messageId);
		addMessageTrackingToContextUrl(messageContext, "communicationPreferencesUrl", messageId);
		addMessageTrackingToContextUrls(messageContext, "upcomingGroupSessions", "reserveSeatUrl", messageId);
		addMessageTrackingToContextUrls(messageContext, "footerContents", "url", messageId);
	}

	protected void addMessageTrackingToContextUrl(@Nonnull Map<String, Object> messageContext,
																								@Nonnull String contextUrlKey,
																								@Nonnull UUID messageId) {
		requireNonNull(messageContext);
		requireNonNull(contextUrlKey);
		requireNonNull(messageId);

		Object contextUrl = messageContext.get(contextUrlKey);

		if (!(contextUrl instanceof String contextUrlString))
			return;

		messageContext.put(contextUrlKey, addMessageTrackingToUrl(contextUrlString, messageId));
	}

	protected void addMessageTrackingToContextUrls(@Nonnull Map<String, Object> messageContext,
																								 @Nonnull String contextCollectionKey,
																								 @Nonnull String contextUrlKey,
																								 @Nonnull UUID messageId) {
		requireNonNull(messageContext);
		requireNonNull(contextCollectionKey);
		requireNonNull(contextUrlKey);
		requireNonNull(messageId);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> contextCollection = (List<Map<String, Object>>) messageContext.get(contextCollectionKey);

		if (contextCollection == null)
			return;

		List<Map<String, Object>> trackedContextCollection = new ArrayList<>(contextCollection.size());

		for (Map<String, Object> contextCollectionItem : contextCollection) {
			Map<String, Object> trackedContextCollectionItem = new HashMap<>(contextCollectionItem);
			addMessageTrackingToContextUrl(trackedContextCollectionItem, contextUrlKey, messageId);
			trackedContextCollection.add(trackedContextCollectionItem);
		}

		messageContext.put(contextCollectionKey, trackedContextCollection);
	}

	@Nonnull
	protected String addMessageTrackingToUrl(@Nonnull String url,
																						 @Nonnull UUID messageId) {
		requireNonNull(url);
		requireNonNull(messageId);

		return WebUtility.appendQueryParameters(url, Map.of(
				AnalyticsService.ANALYTICS_REFERRING_MESSAGE_ID_QUERY_PARAMETER_NAME, FriendlyId.toFriendlyId(messageId)
		));
	}

	@Nonnull
	public NotifySubscribersPreview previewNotifySubscribers(@Nonnull UUID pageGroupId,
																											 @Nullable List<String> overrideEmailAddresses) {
		requireNonNull(pageGroupId);

		NotifySubscribersContext context = createNotifySubscribersContext(pageGroupId, overrideEmailAddresses);
		List<String> recipientEmailAddresses = new ArrayList<>(context.getEmailRecipients().size());

		for (EmailRecipient emailRecipient : context.getEmailRecipients())
			recipientEmailAddresses.add(emailRecipient.getEmailAddress());

		return new NotifySubscribersPreview(
				pageGroupId,
				context.getPageTitle(),
				recipientEmailAddresses,
				context.getUpcomingGroupSessionCount(),
				context.getOverrideEmailAddressesApplied()
		);
	}

	@Nonnull
	protected NotifySubscribersContext createNotifySubscribersContext(@Nonnull UUID pageGroupId,
																														@Nullable List<String> overrideEmailAddresses) {
		requireNonNull(pageGroupId);

		Page page = findCurrentPageByPageGroupId(pageGroupId).orElse(null);

		if (page == null)
			throw new ValidationException(new FieldError("pageGroupId", getStrings().get("Could not find page group.")));

		if (page.getPageId() == null)
			throw new IllegalStateException(format("Page Group ID %s resolved to a page without a page ID.", pageGroupId));

		if (page.getInstitutionId() == null)
			throw new IllegalStateException(format("Page Group ID %s resolved to a page without an institution ID.", pageGroupId));

		Institution institution = getInstitutionService().findInstitutionById(page.getInstitutionId()).orElse(null);

		if (institution == null)
			throw new IllegalStateException(format("Unable to find institution for Page Group ID %s.", pageGroupId));

		PageGroup pageGroup = getPageService().findPageGroupById(pageGroupId)
				.orElseThrow(() -> new IllegalStateException(format("Unable to find page group for Page Group ID %s.", pageGroupId)));
		String analyticsCampaignKey = trimToNull(pageGroup.getAnalyticsCampaignKey());

		Locale locale = institution.getLocale() == null ? Locale.US : institution.getLocale();
		ZoneId defaultTimeZone = institution.getTimeZone() == null ? ZoneId.of("UTC") : institution.getTimeZone();
		String webappBaseUrl = getInstitutionService().findWebappBaseUrlByInstitutionIdAndUserExperienceTypeId(
				institution.getInstitutionId(), UserExperienceTypeId.PATIENT).orElse(null);

		if (webappBaseUrl == null)
			throw new IllegalStateException(format("Unable to find patient webapp URL for institution %s.", institution.getInstitutionId()));

		EmailRecipientResolution emailRecipientResolution = resolveEmailRecipientsForPage(page, webappBaseUrl, overrideEmailAddresses);
		List<PageGroupEmailGroupSession> highlightedGroupSessionSelections = getPageService().findPageGroupEmailGroupSessionsByPageGroupId(pageGroupId);
		List<GroupSession> upcomingGroupSessions = findUpcomingGroupSessionsForSubscriberEmail(page, highlightedGroupSessionSelections, pageGroupId, defaultTimeZone);
		List<Content> footerContents = findFooterContentForSubscriberEmail(pageGroupId);
		Map<UUID, PageGroupEmailGroupSession> highlightedGroupSessionSelectionsByGroupSessionId = new HashMap<>(highlightedGroupSessionSelections.size());

		List<Map<String, Object>> upcomingGroupSessionContext = new ArrayList<>(upcomingGroupSessions.size());
		List<Map<String, Object>> footerContentContext = new ArrayList<>(footerContents.size());

		for (PageGroupEmailGroupSession highlightedGroupSessionSelection : highlightedGroupSessionSelections) {
			if (highlightedGroupSessionSelection.getGroupSessionId() != null)
				highlightedGroupSessionSelectionsByGroupSessionId.put(highlightedGroupSessionSelection.getGroupSessionId(), highlightedGroupSessionSelection);
		}

		for (GroupSession upcomingGroupSession : upcomingGroupSessions)
			upcomingGroupSessionContext.add(createUpcomingGroupSessionMessageContext(
					upcomingGroupSession,
					upcomingGroupSession.getGroupSessionId() == null ? null : highlightedGroupSessionSelectionsByGroupSessionId.get(upcomingGroupSession.getGroupSessionId()),
					locale,
					defaultTimeZone,
					webappBaseUrl,
					analyticsCampaignKey
			));

		for (Content footerContent : footerContents)
			footerContentContext.add(createFooterContentMessageContext(footerContent, webappBaseUrl, analyticsCampaignKey));

		// Keep email display title aligned with the unsubscribe page's "displayName" logic: headline first, then name.
		String pageTitle = trimToNull(page.getHeadline());

		if (pageTitle == null)
			pageTitle = trimToNull(page.getName());

		if (pageTitle == null)
			pageTitle = getStrings().get("Community");

		String communityPageUrl = addCommunityCampaignTrackingToUrl(
				trimToNull(page.getUrlName()) == null ? webappBaseUrl : format("%s/pages/%s", webappBaseUrl, page.getUrlName()),
				analyticsCampaignKey);
		String recordingUrl = footerContentContext.size() > 0 ? (String) footerContentContext.get(0).get("url") : null;
		String recordingTitle = footerContentContext.size() > 0 ? (String) footerContentContext.get(0).get("title") : null;
		String currentMonthName = LocalDate.now(defaultTimeZone).format(DateTimeFormatter.ofPattern("MMMM", locale));
		Boolean multipleUpcomingGroupSessions = upcomingGroupSessionContext.size() > 1;
		Boolean multipleFooterContents = footerContentContext.size() > 1;
		String singleUpcomingGroupSessionTitle = null;

		if (upcomingGroupSessionContext.size() == 1)
			singleUpcomingGroupSessionTitle = trimToNull((String) upcomingGroupSessionContext.get(0).get("title"));

		Map<String, Object> baseMessageContext = new HashMap<>(12);
		baseMessageContext.put("pageTitle", pageTitle);
		baseMessageContext.put("singleUpcomingGroupSessionTitle", singleUpcomingGroupSessionTitle);
		baseMessageContext.put("currentMonthName", currentMonthName);
		baseMessageContext.put("multipleUpcomingGroupSessions", multipleUpcomingGroupSessions);
		baseMessageContext.put("multipleFooterContents", multipleFooterContents);
		baseMessageContext.put("communityPageUrl", communityPageUrl);
		baseMessageContext.put("recordingUrl", recordingUrl);
		baseMessageContext.put("recordingTitle", recordingTitle);
		baseMessageContext.put("footerContents", footerContentContext);
		baseMessageContext.put("upcomingGroupSessions", upcomingGroupSessionContext);
		baseMessageContext.put(EmailMessageContextKey.OVERRIDE_PLATFORM_EMAIL_IMAGE_URL.name(),
				format(COMMUNITY_HIGHLIGHTS_PLATFORM_EMAIL_IMAGE_URL_FORMAT, institution.getInstitutionId().name()));

		return new NotifySubscribersContext(
				institution.getInstitutionId(),
				locale,
				pageTitle,
				baseMessageContext,
				upcomingGroupSessionContext.size(),
				emailRecipientResolution.getEmailRecipients(),
				emailRecipientResolution.getInvalidEmailEntries(),
				emailRecipientResolution.getSmsEntriesSkipped(),
				emailRecipientResolution.getOverrideEmailAddressesApplied()
		);
	}

	@Nonnull
	protected EmailRecipientResolution resolveEmailRecipientsForPage(@Nonnull Page page,
																													 @Nonnull String webappBaseUrl,
																													 @Nullable List<String> overrideEmailAddresses) {
		requireNonNull(page);
		requireNonNull(webappBaseUrl);

		List<MailingListEntry> subscriberEntries = findSubscribedEntriesForPage(page);

		if (overrideEmailAddresses != null)
			return resolveOverrideEmailRecipients(overrideEmailAddresses, subscriberEntries, webappBaseUrl);

		List<EmailRecipient> emailRecipients = new ArrayList<>(subscriberEntries.size());
		Set<String> deduplicatedEmailAddresses = new LinkedHashSet<>();
		int invalidEmailEntries = 0;
		int smsEntriesSkipped = 0;

		for (MailingListEntry subscriberEntry : subscriberEntries) {
			MailingListEntryTypeId entryTypeId = subscriberEntry.getMailingListEntryTypeId();
			String value = trimToNull(subscriberEntry.getValue());

			if (entryTypeId == null || value == null)
				continue;

			if (entryTypeId == MailingListEntryTypeId.EMAIL_ADDRESS) {
				if (!ValidationUtility.isValidEmailAddress(value)) {
					invalidEmailEntries++;
					continue;
				}

				String normalizedEmailAddress = value.toLowerCase(Locale.ROOT);

				if (!deduplicatedEmailAddresses.add(normalizedEmailAddress))
					continue;

				String communicationPreferencesUrl = null;

				if (subscriberEntry.getMailingListEntryId() != null)
					communicationPreferencesUrl = format("%s/mailing-list-entries/%s/unsubscribe", webappBaseUrl, subscriberEntry.getMailingListEntryId());

				emailRecipients.add(new EmailRecipient(normalizedEmailAddress, communicationPreferencesUrl));
			} else if (entryTypeId == MailingListEntryTypeId.SMS) {
				// SMS support is planned but not currently implemented.
				smsEntriesSkipped++;
			}
		}

		return new EmailRecipientResolution(emailRecipients, invalidEmailEntries, smsEntriesSkipped, false);
	}

	@Nonnull
	protected EmailRecipientResolution resolveOverrideEmailRecipients(@Nonnull List<String> overrideEmailAddresses,
																																 @Nonnull List<MailingListEntry> subscriberEntries,
																																 @Nonnull String webappBaseUrl) {
		requireNonNull(overrideEmailAddresses);
		requireNonNull(subscriberEntries);
		requireNonNull(webappBaseUrl);

		ValidationException validationException = new ValidationException();
		List<EmailRecipient> emailRecipients = new ArrayList<>(overrideEmailAddresses.size());
		Set<String> deduplicatedEmailAddresses = new LinkedHashSet<>();
		Map<String, String> communicationPreferencesUrlsByEmailAddress = new HashMap<>(subscriberEntries.size());

		for (MailingListEntry subscriberEntry : subscriberEntries) {
			if (subscriberEntry.getMailingListEntryTypeId() != MailingListEntryTypeId.EMAIL_ADDRESS)
				continue;

			String emailAddress = trimToNull(subscriberEntry.getValue());

			if (emailAddress == null || !ValidationUtility.isValidEmailAddress(emailAddress) || subscriberEntry.getMailingListEntryId() == null)
				continue;

			communicationPreferencesUrlsByEmailAddress.putIfAbsent(
					emailAddress.toLowerCase(Locale.ROOT),
					format("%s/mailing-list-entries/%s/unsubscribe", webappBaseUrl, subscriberEntry.getMailingListEntryId())
			);
		}

		for (int i = 0; i < overrideEmailAddresses.size(); ++i) {
			String overrideEmailAddress = trimToNull(overrideEmailAddresses.get(i));

			if (overrideEmailAddress == null || !ValidationUtility.isValidEmailAddress(overrideEmailAddress)) {
				validationException.add(new FieldError(format("overrideEmailAddresses[%d]", i), getStrings().get("Sorry, this is not a valid email address.")));
				continue;
			}

			String normalizedEmailAddress = overrideEmailAddress.toLowerCase(Locale.ROOT);

			if (!deduplicatedEmailAddresses.add(normalizedEmailAddress))
				continue;

			emailRecipients.add(new EmailRecipient(normalizedEmailAddress, communicationPreferencesUrlsByEmailAddress.get(normalizedEmailAddress)));
		}

		if (validationException.hasErrors())
			throw validationException;

		return new EmailRecipientResolution(emailRecipients, 0, 0, true);
	}

	@Nonnull
	protected Optional<Page> findCurrentPageByPageGroupId(@Nullable UUID pageGroupId) {
		if (pageGroupId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM v_page
				WHERE page_group_id=?
				AND deleted_flag=FALSE
				ORDER BY
				  (page_status_id = 'LIVE') DESC,
				  published_date DESC NULLS LAST,
				  COALESCE(last_updated, created) DESC
				LIMIT 1
				""", Page.class, pageGroupId);
	}

	@Nonnull
	protected List<MailingListEntry> findSubscribedEntriesForPage(@Nonnull Page page) {
		requireNonNull(page);

		if (page.getPageId() == null)
			return List.of();

		Map<String, MailingListEntry> entriesByTypeAndValue = new LinkedHashMap<>();

		getPageService().findPageRowMailingListsByPageId(page.getPageId()).forEach(pageRowMailingList -> {
			UUID mailingListId = pageRowMailingList.getMailingListId();

			if (mailingListId == null)
				return;

			getMailingListService().findMailingListEntriesByMailingListId(mailingListId, MailingListEntryStatusFilter.SUBSCRIBED)
					.forEach(mailingListEntry -> {
						MailingListEntryTypeId typeId = mailingListEntry.getMailingListEntryTypeId();
						String value = trimToNull(mailingListEntry.getValue());

						if (typeId == null || value == null)
							return;

						String key = format("%s:%s", typeId.name(), value.toLowerCase(Locale.ROOT));
						entriesByTypeAndValue.putIfAbsent(key, mailingListEntry);
					});
		});

		return List.copyOf(entriesByTypeAndValue.values());
	}

	@Nonnull
	protected List<GroupSession> findUpcomingGroupSessionsForPage(@Nonnull Page page,
																														@Nonnull ZoneId defaultTimeZone) {
		requireNonNull(page);
		requireNonNull(defaultTimeZone);

		if (page.getPageId() == null || page.getInstitutionId() == null)
			return List.of();

		List<GroupSession> upcomingGroupSessions = new ArrayList<>();
		Set<UUID> uniqueGroupSessionIds = new LinkedHashSet<>();

		List<PageSection> pageSections = getPageService().findPageSectionsByPageId(page.getPageId(), page.getInstitutionId());

		for (PageSection pageSection : pageSections) {
			UUID pageSectionId = pageSection.getPageSectionId();

			if (pageSectionId == null)
				continue;

			getPageService().findPageRowsBySectionId(pageSectionId, page.getInstitutionId()).forEach(pageRow -> {
				if (pageRow.getRowTypeId() != RowTypeId.GROUP_SESSIONS || pageRow.getPageRowId() == null)
					return;

				getPageService().findGroupSessionsByPageRowId(pageRow.getPageRowId(), true).forEach(groupSession -> {
					if (!isUpcomingGroupSession(groupSession, defaultTimeZone))
						return;

					UUID groupSessionId = groupSession.getGroupSessionId();

					if (groupSessionId != null) {
						if (uniqueGroupSessionIds.contains(groupSessionId))
							return;

						uniqueGroupSessionIds.add(groupSessionId);
					}

					upcomingGroupSessions.add(groupSession);
				});
			});
		}

		return upcomingGroupSessions;
	}

	@Nonnull
	protected List<GroupSession> findUpcomingGroupSessionsForSubscriberEmail(@Nonnull Page page,
																															 @Nonnull List<PageGroupEmailGroupSession> highlightedGroupSessionSelections,
																															 @Nonnull UUID pageGroupId,
																															 @Nonnull ZoneId defaultTimeZone) {
		requireNonNull(page);
		requireNonNull(highlightedGroupSessionSelections);
		requireNonNull(pageGroupId);
		requireNonNull(defaultTimeZone);

		if (highlightedGroupSessionSelections.isEmpty())
			return findUpcomingGroupSessionsForPage(page, defaultTimeZone);

		List<GroupSession> upcomingHighlightedGroupSessions = new ArrayList<>();

		for (GroupSession highlightedGroupSession : getPageService().findHighlightedGroupSessionsByPageGroupId(pageGroupId, true)) {
			if (isUpcomingGroupSession(highlightedGroupSession, defaultTimeZone))
				upcomingHighlightedGroupSessions.add(highlightedGroupSession);
		}

		return upcomingHighlightedGroupSessions;
	}

	@Nonnull
	protected List<Content> findFooterContentForSubscriberEmail(@Nonnull UUID pageGroupId) {
		requireNonNull(pageGroupId);

		List<PageGroupEmailContent> footerContentSelections = getPageService().findPageGroupEmailContentByPageGroupId(pageGroupId);

		if (footerContentSelections.isEmpty())
			return List.of();

		return getPageService().findHighlightedContentByPageGroupId(pageGroupId, true);
	}

	protected boolean isUpcomingGroupSession(@Nonnull GroupSession groupSession,
																					 @Nonnull ZoneId defaultTimeZone) {
		requireNonNull(groupSession);
		requireNonNull(defaultTimeZone);

		LocalDateTime startDateTime = groupSession.getStartDateTime();

		if (startDateTime == null)
			return true;

		ZoneId timeZone = groupSession.getTimeZone() == null ? defaultTimeZone : groupSession.getTimeZone();
		LocalDateTime currentDateTime = LocalDateTime.now(timeZone);

		return !startDateTime.isBefore(currentDateTime);
	}

	@Nonnull
	protected Map<String, Object> createUpcomingGroupSessionMessageContext(@Nonnull GroupSession groupSession,
																																						@Nullable PageGroupEmailGroupSession highlightedGroupSessionSelection,
																																						@Nonnull Locale locale,
																																						@Nonnull ZoneId defaultTimeZone,
																																						@Nonnull String webappBaseUrl,
																																						@Nullable String analyticsCampaignKey) {
		requireNonNull(groupSession);
		requireNonNull(locale);
		requireNonNull(defaultTimeZone);
		requireNonNull(webappBaseUrl);

		Map<String, Object> messageContext = new HashMap<>(8);
		LocalDateTime startDateTime = groupSession.getStartDateTime();
		LocalDateTime endDateTime = groupSession.getEndDateTime();
		ZoneId timeZone = groupSession.getTimeZone() == null ? defaultTimeZone : groupSession.getTimeZone();

		if (startDateTime != null) {
			messageContext.put("dateTimeDescription", formatGroupSessionDateTimeDescription(startDateTime, endDateTime, timeZone, locale));
		}

		String description = trimToNull(highlightedGroupSessionSelection == null ? null : highlightedGroupSessionSelection.getDescriptionOverride());

		if (description == null)
			description = trimToNull(groupSession.getDescription());

		messageContext.put("title", trimToNull(groupSession.getTitle()));
		messageContext.put("description", description);
		messageContext.put("imageUrl", trimToNull(groupSession.getImageFileUploadUrl()));
		messageContext.put("reserveSeatUrl", addCommunityCampaignTrackingToUrl(groupSessionDetailUrl(groupSession, webappBaseUrl), analyticsCampaignKey));

		return messageContext;
	}

	@Nonnull
	protected String formatGroupSessionDateTimeDescription(@Nonnull LocalDateTime startDateTime,
																												 @Nullable LocalDateTime endDateTime,
																												 @Nonnull ZoneId timeZone,
																												 @Nonnull Locale locale) {
		requireNonNull(startDateTime);
		requireNonNull(timeZone);
		requireNonNull(locale);

		String date = startDateTime.toLocalDate().format(DateTimeFormatter.ofPattern("EEE, MMM d", locale));
		String time = formatGroupSessionTimeRange(startDateTime, endDateTime, locale);
		String timeZoneAbbreviation = startDateTime.atZone(timeZone).format(DateTimeFormatter.ofPattern("z", locale)).toUpperCase(locale);

		return format("%s @ %s %s", date, time, timeZoneAbbreviation);
	}

	@Nonnull
	protected String formatGroupSessionTimeRange(@Nonnull LocalDateTime startDateTime,
																							 @Nullable LocalDateTime endDateTime,
																							 @Nonnull Locale locale) {
		requireNonNull(startDateTime);
		requireNonNull(locale);

		if (endDateTime == null)
			return startDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("h:mma", locale)).toUpperCase(locale);

		String startAmPm = startDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("a", locale));
		String endAmPm = endDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("a", locale));
		String startTime = startDateTime.toLocalTime()
				.format(DateTimeFormatter.ofPattern(startAmPm.equals(endAmPm) ? "h:mm" : "h:mma", locale))
				.toUpperCase(locale);
		String endTime = endDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("h:mma", locale)).toUpperCase(locale);

		return format("%s-%s", startTime, endTime);
	}

	@Nonnull
	protected Map<String, Object> createFooterContentMessageContext(@Nonnull Content content,
																																	@Nonnull String webappBaseUrl,
																																	@Nullable String analyticsCampaignKey) {
		requireNonNull(content);
		requireNonNull(webappBaseUrl);

		Map<String, Object> messageContext = new HashMap<>(2);
		messageContext.put("title", trimToNull(content.getTitle()));
		messageContext.put("url", addCommunityCampaignTrackingToUrl(resourceLibraryContentUrl(content, webappBaseUrl), analyticsCampaignKey));
		return messageContext;
	}

	@Nullable
	protected String addCommunityCampaignTrackingToUrl(@Nullable String url,
																									 @Nullable String analyticsCampaignKey) {
		if (url == null)
			return null;

		String normalizedAnalyticsCampaignKey = trimToNull(analyticsCampaignKey);

		if (normalizedAnalyticsCampaignKey == null)
			return url;

		Map<String, String> queryParameters = new LinkedHashMap<>();
		queryParameters.put("utm_source", COMMUNITY_HIGHLIGHTS_UTM_SOURCE);
		queryParameters.put("utm_medium", COMMUNITY_HIGHLIGHTS_UTM_MEDIUM);
		queryParameters.put("utm_campaign", normalizedAnalyticsCampaignKey);

		return WebUtility.appendQueryParameters(url, queryParameters);
	}

	@Nullable
	protected String groupSessionDetailUrl(@Nonnull GroupSession groupSession,
																					 @Nonnull String webappBaseUrl) {
		requireNonNull(groupSession);
		requireNonNull(webappBaseUrl);

		String identifier = trimToNull(groupSession.getUrlName());

		if (identifier == null && groupSession.getGroupSessionId() != null)
			identifier = groupSession.getGroupSessionId().toString();

		if (identifier == null)
			return null;

		return format("%s/group-sessions/%s", webappBaseUrl, identifier);
	}

	@Nullable
	protected String resourceLibraryContentUrl(@Nonnull Content content,
																						 @Nonnull String webappBaseUrl) {
		requireNonNull(content);
		requireNonNull(webappBaseUrl);

		UUID contentId = content.getContentId();

		if (contentId == null)
			return null;

		return format("%s/resource-library/%s", webappBaseUrl, contentId);
	}

	@Immutable
	public static class NotifySubscribersPreview {
		@Nonnull
		private final UUID pageGroupId;
		@Nonnull
		private final String pageTitle;
		@Nonnull
		private final List<String> recipientEmailAddresses;
		@Nonnull
		private final Integer upcomingGroupSessionCount;
		@Nonnull
		private final Boolean overrideEmailAddressesApplied;

		public NotifySubscribersPreview(@Nonnull UUID pageGroupId,
																		@Nonnull String pageTitle,
																		@Nonnull List<String> recipientEmailAddresses,
																		@Nonnull Integer upcomingGroupSessionCount,
																		@Nonnull Boolean overrideEmailAddressesApplied) {
			requireNonNull(pageGroupId);
			requireNonNull(pageTitle);
			requireNonNull(recipientEmailAddresses);
			requireNonNull(upcomingGroupSessionCount);
			requireNonNull(overrideEmailAddressesApplied);

			this.pageGroupId = pageGroupId;
			this.pageTitle = pageTitle;
			this.recipientEmailAddresses = List.copyOf(recipientEmailAddresses);
			this.upcomingGroupSessionCount = upcomingGroupSessionCount;
			this.overrideEmailAddressesApplied = overrideEmailAddressesApplied;
		}

		@Nonnull
		public UUID getPageGroupId() {
			return this.pageGroupId;
		}

		@Nonnull
		public String getPageTitle() {
			return this.pageTitle;
		}

		@Nonnull
		public List<String> getRecipientEmailAddresses() {
			return this.recipientEmailAddresses;
		}

		@Nonnull
		public Integer getUpcomingGroupSessionCount() {
			return this.upcomingGroupSessionCount;
		}

		@Nonnull
		public Boolean getOverrideEmailAddressesApplied() {
			return this.overrideEmailAddressesApplied;
		}
	}

	@Immutable
	protected static class NotifySubscribersContext {
		@Nonnull
		private final Institution.InstitutionId institutionId;
		@Nonnull
		private final Locale locale;
		@Nonnull
		private final String pageTitle;
		@Nonnull
		private final Map<String, Object> baseMessageContext;
		@Nonnull
		private final Integer upcomingGroupSessionCount;
		@Nonnull
		private final List<EmailRecipient> emailRecipients;
		@Nonnull
		private final Integer invalidEmailEntries;
		@Nonnull
		private final Integer smsEntriesSkipped;
		@Nonnull
		private final Boolean overrideEmailAddressesApplied;

		public NotifySubscribersContext(@Nonnull Institution.InstitutionId institutionId,
																		@Nonnull Locale locale,
																		@Nonnull String pageTitle,
																		@Nonnull Map<String, Object> baseMessageContext,
																		@Nonnull Integer upcomingGroupSessionCount,
																		@Nonnull List<EmailRecipient> emailRecipients,
																		@Nonnull Integer invalidEmailEntries,
																		@Nonnull Integer smsEntriesSkipped,
																		@Nonnull Boolean overrideEmailAddressesApplied) {
			requireNonNull(institutionId);
			requireNonNull(locale);
			requireNonNull(pageTitle);
			requireNonNull(baseMessageContext);
			requireNonNull(upcomingGroupSessionCount);
			requireNonNull(emailRecipients);
			requireNonNull(invalidEmailEntries);
			requireNonNull(smsEntriesSkipped);
			requireNonNull(overrideEmailAddressesApplied);

			this.institutionId = institutionId;
			this.locale = locale;
			this.pageTitle = pageTitle;
			this.baseMessageContext = Collections.unmodifiableMap(new HashMap<>(baseMessageContext));
			this.upcomingGroupSessionCount = upcomingGroupSessionCount;
			this.emailRecipients = List.copyOf(emailRecipients);
			this.invalidEmailEntries = invalidEmailEntries;
			this.smsEntriesSkipped = smsEntriesSkipped;
			this.overrideEmailAddressesApplied = overrideEmailAddressesApplied;
		}

		@Nonnull
		public Institution.InstitutionId getInstitutionId() {
			return this.institutionId;
		}

		@Nonnull
		public Locale getLocale() {
			return this.locale;
		}

		@Nonnull
		public String getPageTitle() {
			return this.pageTitle;
		}

		@Nonnull
		public Map<String, Object> getBaseMessageContext() {
			return this.baseMessageContext;
		}

		@Nonnull
		public Integer getUpcomingGroupSessionCount() {
			return this.upcomingGroupSessionCount;
		}

		@Nonnull
		public List<EmailRecipient> getEmailRecipients() {
			return this.emailRecipients;
		}

		@Nonnull
		public Integer getInvalidEmailEntries() {
			return this.invalidEmailEntries;
		}

		@Nonnull
		public Integer getSmsEntriesSkipped() {
			return this.smsEntriesSkipped;
		}

		@Nonnull
		public Boolean getOverrideEmailAddressesApplied() {
			return this.overrideEmailAddressesApplied;
		}
	}

	@Immutable
	protected static class EmailRecipientResolution {
		@Nonnull
		private final List<EmailRecipient> emailRecipients;
		@Nonnull
		private final Integer invalidEmailEntries;
		@Nonnull
		private final Integer smsEntriesSkipped;
		@Nonnull
		private final Boolean overrideEmailAddressesApplied;

		public EmailRecipientResolution(@Nonnull List<EmailRecipient> emailRecipients,
																		@Nonnull Integer invalidEmailEntries,
																		@Nonnull Integer smsEntriesSkipped,
																		@Nonnull Boolean overrideEmailAddressesApplied) {
			requireNonNull(emailRecipients);
			requireNonNull(invalidEmailEntries);
			requireNonNull(smsEntriesSkipped);
			requireNonNull(overrideEmailAddressesApplied);

			this.emailRecipients = List.copyOf(emailRecipients);
			this.invalidEmailEntries = invalidEmailEntries;
			this.smsEntriesSkipped = smsEntriesSkipped;
			this.overrideEmailAddressesApplied = overrideEmailAddressesApplied;
		}

		@Nonnull
		public List<EmailRecipient> getEmailRecipients() {
			return this.emailRecipients;
		}

		@Nonnull
		public Integer getInvalidEmailEntries() {
			return this.invalidEmailEntries;
		}

		@Nonnull
		public Integer getSmsEntriesSkipped() {
			return this.smsEntriesSkipped;
		}

		@Nonnull
		public Boolean getOverrideEmailAddressesApplied() {
			return this.overrideEmailAddressesApplied;
		}
	}

	@Immutable
	protected static class EmailRecipient {
		@Nonnull
		private final String emailAddress;
		@Nullable
		private final String communicationPreferencesUrl;

		public EmailRecipient(@Nonnull String emailAddress,
													@Nullable String communicationPreferencesUrl) {
			requireNonNull(emailAddress);

			this.emailAddress = emailAddress;
			this.communicationPreferencesUrl = communicationPreferencesUrl;
		}

		@Nonnull
		public String getEmailAddress() {
			return this.emailAddress;
		}

		@Nonnull
		public Optional<String> getCommunicationPreferencesUrl() {
			return Optional.ofNullable(this.communicationPreferencesUrl);
		}
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}

	@Nonnull
	protected PageService getPageService() {
		return this.pageServiceProvider.get();
	}

	@Nonnull
	protected MailingListService getMailingListService() {
		return this.mailingListServiceProvider.get();
	}

	@Nonnull
	protected InstitutionService getInstitutionService() {
		return this.institutionServiceProvider.get();
	}

	@Nonnull
	protected MessageService getMessageService() {
		return this.messageServiceProvider.get();
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
