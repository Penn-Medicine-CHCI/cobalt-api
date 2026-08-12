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

import com.cobaltplatform.api.IntegrationTestExecutor;
import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.context.CurrentContextExecutor;
import com.cobaltplatform.api.model.api.request.CreateGroupSessionRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRequest;
import com.cobaltplatform.api.model.api.request.CreatePageRowGroupSessionRequest;
import com.cobaltplatform.api.model.api.response.GroupSessionApiResponse;
import com.cobaltplatform.api.model.api.response.PageApiResponse;
import com.cobaltplatform.api.model.api.response.PageApiResponse.PageApiResponseFactory;
import com.cobaltplatform.api.model.api.response.PageRowApiResponse;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.GroupSessionLocationType.GroupSessionLocationTypeId;
import com.cobaltplatform.api.model.db.GroupSessionSchedulingSystem.GroupSessionSchedulingSystemId;
import com.cobaltplatform.api.model.db.GroupSessionStatus.GroupSessionStatusId;
import com.cobaltplatform.api.model.db.GroupSessionVisibilityType.GroupSessionVisibilityTypeId;
import com.cobaltplatform.api.model.db.Institution;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Page;
import com.cobaltplatform.api.model.db.PageSection;
import com.cobaltplatform.api.model.db.PageStatus.PageStatusId;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.pyranid.Database;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

@ThreadSafe
public class PageServiceGroupSessionVisibilityTests {
	@Test
	public void publishedPageResponseIncludesOnlyUpcomingGroupSessions() {
		IntegrationTestExecutor.runTransactionallyAndForceRollback((app) -> {
			Database database = app.getInjector().getInstance(DatabaseProvider.class).getWritableMasterDatabase();
			AccountService accountService = app.getInjector().getInstance(AccountService.class);
			InstitutionService institutionService = app.getInjector().getInstance(InstitutionService.class);
			CurrentContextExecutor currentContextExecutor = app.getInjector().getInstance(CurrentContextExecutor.class);
			UUID accountId = database.queryForObject("""
					SELECT account_id
					FROM v_account
					WHERE institution_id = ?
					ORDER BY created
					LIMIT 1
					""", UUID.class, InstitutionId.COBALT).orElseThrow();
			Account account = accountService.findAccountById(accountId).orElseThrow();
			Institution institution = institutionService.findInstitutionById(account.getInstitutionId()).orElseThrow();

			currentContextExecutor.execute(new CurrentContext.Builder(account, Locale.US, institution.getTimeZone()).build(), () -> {
				PageService pageService = app.getInjector().getInstance(PageService.class);
				GroupSessionService groupSessionService = app.getInjector().getInstance(GroupSessionService.class);
				PageApiResponseFactory pageApiResponseFactory = app.getInjector().getInstance(PageApiResponseFactory.class);
				LocalDateTime currentDateTime = LocalDateTime.now(institution.getTimeZone());
				UUID pastGroupSessionId = createGroupSession(groupSessionService, account, "past");
				UUID futureGroupSessionId = createGroupSession(groupSessionService, account, "future");
				UUID unscheduledGroupSessionId = createGroupSession(groupSessionService, account, "unscheduled");

				database.execute("""
						UPDATE group_session
						SET group_session_status_id = ?, start_date_time = ?, end_date_time = ?
						WHERE group_session_id = ?
						""", GroupSessionStatusId.ADDED, currentDateTime.minusDays(2), currentDateTime.plusDays(2), pastGroupSessionId);
				database.execute("""
						UPDATE group_session
						SET group_session_status_id = ?
						WHERE group_session_id = ?
						""", GroupSessionStatusId.ADDED, futureGroupSessionId);
				database.execute("""
						UPDATE group_session
						SET group_session_status_id = ?, start_date_time = NULL
						WHERE group_session_id = ?
						""", GroupSessionStatusId.ADDED, unscheduledGroupSessionId);

				UUID pastOnlyPageId = createPageWithGroupSessions(pageService, account, List.of(pastGroupSessionId), "past-only");
				UUID mixedPageId = createPageWithGroupSessions(pageService, account,
						List.of(pastGroupSessionId, futureGroupSessionId, unscheduledGroupSessionId), "mixed");

				PageApiResponse draftPageResponse = pageResponse(pageService, pageApiResponseFactory, mixedPageId,
						account.getInstitutionId());
				Assert.assertEquals(1, draftPageResponse.getPageSections().size());
				Assert.assertEquals(1, draftPageResponse.getPageSections().get(0).getPageRows().size());
				Assert.assertEquals(3, draftPageResponse.getPageSections().get(0).getPageRows().get(0).getGroupSessions().size());

				database.execute("""
						UPDATE page
						SET page_status_id = ?
						WHERE page_id IN (?, ?)
						""", PageStatusId.LIVE, pastOnlyPageId, mixedPageId);

				PageApiResponse pastOnlyPageResponse = pageResponse(pageService, pageApiResponseFactory, pastOnlyPageId,
						account.getInstitutionId());
				Assert.assertTrue(pastOnlyPageResponse.getPageSections().isEmpty());

				PageApiResponse mixedPageResponse = pageResponse(pageService, pageApiResponseFactory, mixedPageId,
						account.getInstitutionId());
				Assert.assertEquals(1, mixedPageResponse.getPageSections().size());
				Assert.assertEquals(1, mixedPageResponse.getPageSections().get(0).getPageRows().size());

				PageRowApiResponse groupSessionsRow = mixedPageResponse.getPageSections().get(0).getPageRows().get(0);
				Set<UUID> visibleGroupSessionIds = groupSessionsRow.getGroupSessions().stream()
						.map(GroupSessionApiResponse::getGroupSessionId)
						.collect(Collectors.toSet());
				Assert.assertEquals(Set.of(futureGroupSessionId, unscheduledGroupSessionId), visibleGroupSessionIds);
			});
		});
	}

	@Nonnull
	protected UUID createGroupSession(@Nonnull GroupSessionService groupSessionService,
																				 @Nonnull Account account,
																				 @Nonnull String urlNameSuffix) {
		requireNonNull(groupSessionService);
		requireNonNull(account);
		requireNonNull(urlNameSuffix);

		LocalDateTime startDateTime = LocalDateTime.now().plusDays(7);
		CreateGroupSessionRequest request = new CreateGroupSessionRequest();
		request.setInstitutionId(account.getInstitutionId());
		request.setGroupSessionSchedulingSystemId(GroupSessionSchedulingSystemId.COBALT);
		request.setGroupSessionLocationTypeId(GroupSessionLocationTypeId.IN_PERSON);
		request.setSubmitterAccountId(account.getAccountId());
		request.setTitle("Page visibility test");
		request.setDescription("Page visibility test description.");
		request.setUrlName(format("page-visibility-test-%s-%s", urlNameSuffix, UUID.randomUUID()));
		request.setInPersonLocation("Test location");
		request.setFacilitatorName("Test Facilitator");
		request.setFacilitatorEmailAddress("facilitator@example.com");
		request.setStartDateTime(startDateTime);
		request.setEndDateTime(startDateTime.plusHours(1));
		request.setGroupSessionVisibilityTypeId(GroupSessionVisibilityTypeId.PUBLIC);
		request.setDifferentEmailAddressForNotifications(false);
		request.setSingleSessionFlag(true);
		request.setSendFollowupEmail(false);
		request.setSendReminderEmail(false);
		return groupSessionService.createGroupSession(request, account);
	}

	@Nonnull
	protected UUID createPageWithGroupSessions(@Nonnull PageService pageService,
																						 @Nonnull Account account,
																						 @Nonnull List<UUID> groupSessionIds,
																						 @Nonnull String urlNameSuffix) {
		requireNonNull(pageService);
		requireNonNull(account);
		requireNonNull(groupSessionIds);
		requireNonNull(urlNameSuffix);

		CreatePageRequest createPageRequest = new CreatePageRequest();
		createPageRequest.setName(format("Page visibility test %s %s", urlNameSuffix, UUID.randomUUID()));
		createPageRequest.setUrlName(format("page-visibility-test-%s-%s", urlNameSuffix, UUID.randomUUID()));
		createPageRequest.setInstitutionId(account.getInstitutionId());
		createPageRequest.setCreatedByAccountId(account.getAccountId());
		UUID pageId = pageService.createPage(createPageRequest);
		PageSection pageSection = pageService.findPageSectionsByPageId(pageId, account.getInstitutionId()).get(0);

		CreatePageRowGroupSessionRequest createPageRowRequest = new CreatePageRowGroupSessionRequest();
		createPageRowRequest.setPageSectionId(pageSection.getPageSectionId());
		createPageRowRequest.setGroupSessionIds(groupSessionIds);
		createPageRowRequest.setCreatedByAccountId(account.getAccountId());
		pageService.createPageRowGroupSession(createPageRowRequest, account.getInstitutionId());
		return pageId;
	}

	@Nonnull
	protected PageApiResponse pageResponse(@Nonnull PageService pageService,
																			 @Nonnull PageApiResponseFactory pageApiResponseFactory,
																			 @Nonnull UUID pageId,
																			 @Nonnull InstitutionId institutionId) {
		requireNonNull(pageService);
		requireNonNull(pageApiResponseFactory);
		requireNonNull(pageId);
		requireNonNull(institutionId);

		Page page = pageService.findPageById(pageId, institutionId, true).orElseThrow();
		return pageApiResponseFactory.create(page, true);
	}
}
