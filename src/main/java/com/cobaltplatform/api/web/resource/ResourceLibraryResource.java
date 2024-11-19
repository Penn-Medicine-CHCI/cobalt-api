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

package com.cobaltplatform.api.web.resource;

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.integration.enterprise.EnterprisePlugin;
import com.cobaltplatform.api.integration.enterprise.EnterprisePluginProvider;
import com.cobaltplatform.api.model.api.request.FindResourceLibraryContentRequest;
import com.cobaltplatform.api.model.api.response.ContentApiResponse;
import com.cobaltplatform.api.model.api.response.ContentApiResponse.ContentApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ContentAudienceTypeApiResponse;
import com.cobaltplatform.api.model.api.response.ContentAudienceTypeApiResponse.ContentAudienceTypeApiResponseFactory;
import com.cobaltplatform.api.model.api.response.ContentAudienceTypeGroupApiResponse;
import com.cobaltplatform.api.model.api.response.ContentAudienceTypeGroupApiResponse.ContentAudienceTypeGroupApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TagApiResponse;
import com.cobaltplatform.api.model.api.response.TagApiResponse.TagApiResponseFactory;
import com.cobaltplatform.api.model.api.response.TagGroupApiResponse;
import com.cobaltplatform.api.model.api.response.TagGroupApiResponse.TagGroupApiResponseFactory;
import com.cobaltplatform.api.model.db.Account;
import com.cobaltplatform.api.model.db.Color.ColorId;
import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.db.ContentAudienceType.ContentAudienceTypeId;
import com.cobaltplatform.api.model.db.ContentType.ContentTypeId;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Tag;
import com.cobaltplatform.api.model.db.TagGroup;
import com.cobaltplatform.api.model.security.AuthenticationRequired;
import com.cobaltplatform.api.model.service.ContentDurationId;
import com.cobaltplatform.api.model.service.FindResult;
import com.cobaltplatform.api.model.service.ResourceLibrarySortColumnId;
import com.cobaltplatform.api.service.AuthorizationService;
import com.cobaltplatform.api.service.ContentService;
import com.cobaltplatform.api.service.TagService;
import com.cobaltplatform.api.util.Formatter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.lokalized.Strings;
import com.soklet.web.annotation.GET;
import com.soklet.web.annotation.PathParameter;
import com.soklet.web.annotation.QueryParameter;
import com.soklet.web.annotation.Resource;
import com.soklet.web.exception.NotFoundException;
import com.soklet.web.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Resource
@Singleton
@ThreadSafe
public class ResourceLibraryResource {
	@Nonnull
	private final ContentService contentService;
	@Nonnull
	private final TagService tagService;
	@Nonnull
	private final AuthorizationService authorizationService;
	@Nonnull
	private final EnterprisePluginProvider enterprisePluginProvider;
	@Nonnull
	private final Formatter formatter;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Provider<CurrentContext> currentContextProvider;
	@Nonnull
	private final ContentApiResponseFactory contentApiResponseFactory;
	@Nonnull
	private final TagApiResponseFactory tagApiResponseFactory;
	@Nonnull
	private final TagGroupApiResponseFactory tagGroupApiResponseFactory;
	@Nonnull
	private final ContentAudienceTypeApiResponseFactory contentAudienceTypeApiResponseFactory;
	@Nonnull
	private final ContentAudienceTypeGroupApiResponseFactory contentAudienceTypeGroupApiResponseFactory;
	@Nonnull
	private final Logger logger;

	@Inject
	public ResourceLibraryResource(@Nonnull ContentService contentService,
																 @Nonnull TagService tagService,
																 @Nonnull AuthorizationService authorizationService,
																 @Nonnull EnterprisePluginProvider enterprisePluginProvider,
																 @Nonnull Formatter formatter,
																 @Nonnull Strings strings,
																 @Nonnull Provider<CurrentContext> currentContextProvider,
																 @Nonnull ContentApiResponseFactory contentApiResponseFactory,
																 @Nonnull TagApiResponseFactory tagApiResponseFactory,
																 @Nonnull TagGroupApiResponseFactory tagGroupApiResponseFactory,
																 @Nonnull ContentAudienceTypeApiResponseFactory contentAudienceTypeApiResponseFactory,
																 @Nonnull ContentAudienceTypeGroupApiResponseFactory contentAudienceTypeGroupApiResponseFactory) {
		requireNonNull(contentService);
		requireNonNull(tagService);
		requireNonNull(authorizationService);
		requireNonNull(enterprisePluginProvider);
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);
		requireNonNull(contentApiResponseFactory);
		requireNonNull(tagApiResponseFactory);
		requireNonNull(tagGroupApiResponseFactory);
		requireNonNull(contentAudienceTypeApiResponseFactory);
		requireNonNull(contentAudienceTypeGroupApiResponseFactory);

		this.contentService = contentService;
		this.tagService = tagService;
		this.authorizationService = authorizationService;
		this.enterprisePluginProvider = enterprisePluginProvider;
		this.formatter = formatter;
		this.strings = strings;
		this.currentContextProvider = currentContextProvider;
		this.contentApiResponseFactory = contentApiResponseFactory;
		this.tagApiResponseFactory = tagApiResponseFactory;
		this.tagGroupApiResponseFactory = tagGroupApiResponseFactory;
		this.contentAudienceTypeApiResponseFactory = contentAudienceTypeApiResponseFactory;
		this.contentAudienceTypeGroupApiResponseFactory = contentAudienceTypeGroupApiResponseFactory;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	@GET("/resource-library/tag-groups")
	@AuthenticationRequired
	public ApiResponse resourceLibraryTagGroups() {
		CurrentContext currentContext = getCurrentContext();
		List<TagGroupApiResponse> tagGroups = getTagService().findTagGroupsByInstitutionId(currentContext.getInstitutionId()).stream()
				.map(tagGroup -> getTagGroupApiResponseFactory().create(tagGroup))
				.collect(Collectors.toList());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("tagGroups", tagGroups);
		}});
	}

	@Nonnull
	@GET("/resource-library")
	@AuthenticationRequired
	public ApiResponse resourceLibrary() {
		CurrentContext currentContext = getCurrentContext();
		Account account = currentContext.getAccount().get();
		List<Content> contents = getContentService().findVisibleContentByAccount(account);

		// Pick out tags in the content
		Set<String> tagGroupIds = new HashSet<>();
		Map<String, TagApiResponse> tagsByTagId = new HashMap<>();

		for (Content content : contents) {
			for (Tag tag : content.getTags()) {
				if (tagsByTagId.containsKey(tag.getTagId()))
					continue;

				tagGroupIds.add(tag.getTagGroupId());
				tagsByTagId.put(tag.getTagId(), getTagApiResponseFactory().create(tag));
			}
		}

		// Only those tag groups associated with the tags in the content
		List<TagGroupApiResponse> tagGroups = getTagService().findTagGroupsByInstitutionId(currentContext.getInstitutionId()).stream()
				.filter(tagGroup -> tagGroupIds.contains(tagGroup.getTagGroupId()))
				.map(tagGroup -> getTagGroupApiResponseFactory().create(tagGroup))
				.collect(Collectors.toList());

		// Keep track of content IDs so we don't show the same content more than once
		Set<UUID> contentIds = new HashSet<>(contents.size());

		// Group content by tag group
		Map<String, List<ContentApiResponse>> contentsByTagGroupId = new HashMap<>(tagGroups.size());

		// Don't have too many pieces of content per tag group
		final int MAXIMUM_CONTENT_COUNT_PER_TAG_GROUP = 20;

		for (Content content : contents) {
			if (contentIds.contains(content.getContentId()))
				continue;

			String tagGroupId = content.getTags().stream()
					.map(tag -> tag.getTagGroupId())
					.findFirst()
					.orElse(null);

			if (tagGroupId != null) {
				List<ContentApiResponse> tagGroupContents = contentsByTagGroupId.get(tagGroupId);

				if (tagGroupContents == null) {
					tagGroupContents = new ArrayList<>();
					contentsByTagGroupId.put(tagGroupId, tagGroupContents);
				}

				if (tagGroupContents.size() < MAXIMUM_CONTENT_COUNT_PER_TAG_GROUP) {
					contentIds.add(content.getContentId());
					tagGroupContents.add(getContentApiResponseFactory().create(content));
				}
			}
		}

		// It's possible some of the tag groups don't have any content.  If so, discard them from the list of tag groups.
		// Also, discard any deprecated tag groups.
		List<TagGroupApiResponse> populatedTagGroups = tagGroups.stream()
				.filter(tagGroup -> {
					List<ContentApiResponse> tagGroupContents = contentsByTagGroupId.get(tagGroup.getTagGroupId());
					return tagGroupContents != null && tagGroupContents.size() > 0 && !tagGroup.getDeprecated();
				})
				.collect(Collectors.toList());

		return new ApiResponse(new HashMap<String, Object>() {{
			put("contentsByTagGroupId", contentsByTagGroupId);
			put("tagGroups", populatedTagGroups);
			put("tagsByTagId", tagsByTagId);
		}});
	}

	@Nonnull
	@GET("/resource-library/search")
	@AuthenticationRequired
	public ApiResponse searchResourceLibrary(@Nonnull @QueryParameter Optional<String> searchQuery,
																					 @Nonnull @QueryParameter("tagId") Optional<List<String>> tagIds,
																					 @Nonnull @QueryParameter("contentTypeId") Optional<List<ContentTypeId>> contentTypeIds,
																					 @Nonnull @QueryParameter("contentDurationId") Optional<List<ContentDurationId>> contentDurationIds,
																					 @Nonnull @QueryParameter("contentAudienceTypeId") Optional<List<ContentAudienceTypeId>> contentAudienceTypeIds,
																					 @Nonnull @QueryParameter Optional<ResourceLibrarySortColumnId> resourceLibrarySortColumnId,
																					 @Nonnull @QueryParameter Optional<Integer> pageNumber,
																					 @Nonnull @QueryParameter Optional<Integer> pageSize) {
		requireNonNull(searchQuery);
		requireNonNull(tagIds);
		requireNonNull(contentTypeIds);
		requireNonNull(contentDurationIds);
		requireNonNull(contentAudienceTypeIds);
		requireNonNull(resourceLibrarySortColumnId);
		requireNonNull(pageNumber);
		requireNonNull(pageSize);

		CurrentContext currentContext = getCurrentContext();
		Account account = currentContext.getAccount().get();

		FindResult<Content> findResult = getContentService().findResourceLibraryContent(new FindResourceLibraryContentRequest() {
			{
				setInstitutionId(account.getInstitutionId());
				setSearchQuery(searchQuery.orElse(null));
				setTagIds(new HashSet<>(tagIds.orElse(List.of())));
				setContentTypeIds(new HashSet<>(contentTypeIds.orElse(List.of())));
				setContentDurationIds(new HashSet<>(contentDurationIds.orElse(List.of())));
				setContentAudienceTypeIds(new HashSet<>(contentAudienceTypeIds.orElse(List.of())));
				setResourceLibrarySortColumnId(resourceLibrarySortColumnId.orElse(null));
				setPageNumber(pageNumber.orElse(0));
				setPageSize(pageSize.orElse(0));
			}
		});

		List<ContentApiResponse> contents = new ArrayList<>();
		Map<String, TagApiResponse> tagsByTagId = new HashMap<>();

		for (Tag tag : getTagService().findTagsByInstitutionId(account.getInstitutionId()))
			tagsByTagId.put(tag.getTagId(), getTagApiResponseFactory().create(tag));

		for (Content content : findResult.getResults())
			contents.add(getContentApiResponseFactory().create(content));

		Map<String, Object> findResultJson = new HashMap<>();
		findResultJson.put("contents", contents);
		findResultJson.put("totalCount", findResult.getTotalCount());
		findResultJson.put("totalCountDescription", getFormatter().formatNumber(findResult.getTotalCount()));

		return new ApiResponse(new HashMap<String, Object>() {{
			put("findResult", findResultJson);
			put("tagsByTagId", tagsByTagId);
		}});
	}

	@Nonnull
	@GET("/resource-library/recommended")
	@AuthenticationRequired
	public ApiResponse recommendedResourceLibrary(@Nonnull @QueryParameter("tagId") Optional<List<String>> tagIds,
																								@Nonnull @QueryParameter("contentTypeId") Optional<List<ContentTypeId>> contentTypeIds,
																								@Nonnull @QueryParameter("contentDurationId") Optional<List<ContentDurationId>> contentDurationIds,
																								@Nonnull @QueryParameter Optional<Integer> pageNumber,
																								@Nonnull @QueryParameter Optional<Integer> pageSize) {
		requireNonNull(tagIds);
		requireNonNull(contentTypeIds);
		requireNonNull(contentDurationIds);
		requireNonNull(pageNumber);
		requireNonNull(pageSize);

		CurrentContext currentContext = getCurrentContext();
		Account account = currentContext.getAccount().get();
		EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForInstitutionId(account.getInstitutionId());

		Set<String> tagIdsToMatch = new HashSet<>(tagIds.orElse(List.of()));
		Set<ContentTypeId> contentTypeIdsToMatch = new HashSet<>(contentTypeIds.orElse(List.of()));
		Set<ContentDurationId> contentDurationIdsToMatch = new HashSet<>(contentDurationIds.orElse(List.of()));

		// Delegate to enterprise plugin to determine what an institution's "recommended" content is.
		// For now, recommended content size is relatively small, so we do our filtering here in-memory.
		List<ContentApiResponse> contents = enterprisePlugin.recommendedContentForAccountId(account.getAccountId()).stream()
				.filter(content -> {
					boolean tagIdFilterSucceeded = false;

					// Include content if tag filter succeeds
					if (tagIdsToMatch.size() == 0) {
						tagIdFilterSucceeded = true;
					} else {
						Set<String> contentTagIds = content.getTags().stream().map(tag -> tag.getTagId()).collect(Collectors.toSet());

						if (Sets.intersection(tagIdsToMatch, contentTagIds).size() > 0)
							tagIdFilterSucceeded = true;
					}

					boolean contentTypeIdFilterSucceeded = false;

					// Include content if content type filter succeeds
					if (contentTypeIdsToMatch.size() == 0)
						contentTypeIdFilterSucceeded = true;
					else if (contentTypeIdsToMatch.contains(content.getContentTypeId()))
						contentTypeIdFilterSucceeded = true;

					boolean contentDurationIdFilterSucceeded = false;

					// Include content if content duration filter succeeds
					if (contentDurationIdsToMatch.size() == 0) {
						contentDurationIdFilterSucceeded = true;
					} else {
						if (content.getDurationInMinutes() != null)
							for (ContentDurationId contentDurationId : contentDurationIdsToMatch)
								if (content.getDurationInMinutes() >= contentDurationId.getLowerBoundInclusive()
										&& content.getDurationInMinutes() <= contentDurationId.getUpperBoundInclusive())
									contentDurationIdFilterSucceeded = true;
					}

					return tagIdFilterSucceeded && contentTypeIdFilterSucceeded && contentDurationIdFilterSucceeded;
				})
				.map(content -> getContentApiResponseFactory().create(content))
				.collect(Collectors.toList());

		// Chunk results into pages
		final int MAXIMUM_PAGE_SIZE = 50;

		int normalizedPageNumber = pageNumber.orElse(0);

		if (normalizedPageNumber < 0)
			normalizedPageNumber = 0;

		int normalizedPageSize = pageSize.orElse(MAXIMUM_PAGE_SIZE);

		if (normalizedPageSize < 0)
			normalizedPageSize = 0;
		else if (normalizedPageSize > MAXIMUM_PAGE_SIZE)
			normalizedPageSize = MAXIMUM_PAGE_SIZE;

		List<List<ContentApiResponse>> contentPages = normalizedPageSize == 0 ? List.of() : Lists.partition(contents, normalizedPageSize);
		List<ContentApiResponse> contentPage = List.of();

		if (contentPages.size() > normalizedPageNumber)
			contentPage = contentPages.get(normalizedPageNumber);

		// Pull supporting data (tags, tag groups)
		Map<String, TagApiResponse> tagsByTagId = new HashMap<>();

		for (Tag tag : getTagService().findTagsByInstitutionId(account.getInstitutionId()))
			tagsByTagId.put(tag.getTagId(), getTagApiResponseFactory().create(tag));

		List<TagGroupApiResponse> tagGroups = getTagService().findTagGroupsByInstitutionId(currentContext.getInstitutionId()).stream()
				.map(tagGroup -> getTagGroupApiResponseFactory().create(tagGroup))
				.collect(Collectors.toList());

		Map<String, Object> findResultJson = new HashMap<>();
		findResultJson.put("contents", contentPage);
		findResultJson.put("totalCount", contents.size());
		findResultJson.put("totalCountDescription", getFormatter().formatNumber(contents.size()));

		return new ApiResponse(new HashMap<String, Object>() {{
			put("findResult", findResultJson);
			put("tagsByTagId", tagsByTagId);
			put("tagGroups", tagGroups);
			put("contentDurations", availableContentDurations());
			put("contentTypes", availableContentTypes());
		}});
	}

	@Nonnull
	@GET("/resource-library/tag-groups/{tagGroupId}")
	@AuthenticationRequired
	public ApiResponse resourceLibraryTagGroup(@Nonnull @PathParameter String tagGroupId,
																						 @Nonnull @QueryParameter Optional<String> searchQuery,
																						 @Nonnull @QueryParameter("tagId") Optional<List<String>> tagIds,
																						 @Nonnull @QueryParameter("contentTypeId") Optional<List<ContentTypeId>> contentTypeIds,
																						 @Nonnull @QueryParameter("contentDurationId") Optional<List<ContentDurationId>> contentDurationIds,
																						 @Nonnull @QueryParameter Optional<Integer> pageNumber,
																						 @Nonnull @QueryParameter Optional<Integer> pageSize) {
		requireNonNull(tagGroupId);
		requireNonNull(searchQuery);
		requireNonNull(tagIds);
		requireNonNull(contentTypeIds);
		requireNonNull(contentDurationIds);
		requireNonNull(pageNumber);
		requireNonNull(pageSize);

		CurrentContext currentContext = getCurrentContext();
		Account account = currentContext.getAccount().get();

		// TODO: remove this harcoded RECOMMENDED support - it's a temporary workaround for an iOS build
		if (tagGroupId.toUpperCase(Locale.ENGLISH).equals("RECOMMENDED")) {
			EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForCurrentInstitution();
			List<Content> contents = enterprisePlugin.recommendedContentForAccountId(account.getAccountId());

			Set<String> recommendedTagIds = new HashSet<>();

			for (Content content : contents)
				for (Tag tag : content.getTags())
					recommendedTagIds.add(tag.getTagId());

			FindResult<Content> findResult = new FindResult<>(contents, contents.size());

			Map<String, Object> findResultJson = new HashMap<>();
			findResultJson.put("contents", contents);
			findResultJson.put("totalCount", findResult.getTotalCount());
			findResultJson.put("totalCountDescription", getFormatter().formatNumber(findResult.getTotalCount()));

			Map<String, TagApiResponse> tagsByTagId = new HashMap<>();
			List<Tag> tags = getTagService().findTagsByInstitutionId(account.getInstitutionId());

			for (Tag tag : tags)
				tagsByTagId.put(tag.getTagId(), getTagApiResponseFactory().create(tag));

			List<Tag> recommendedTags = tags.stream()
					.filter(tag -> recommendedTagIds.contains(tag.getTagId()))
					.collect(Collectors.toList());

			Map<String, Object> tagGroupJson = new HashMap<>();
			tagGroupJson.put("tagGroupId", "RECOMMENDED");
			tagGroupJson.put("colorId", ColorId.BRAND_PRIMARY);
			tagGroupJson.put("name", "Recommended");
			tagGroupJson.put("urlName", "recommended");
			tagGroupJson.put("description", "Content recommendations for you.");
			tagGroupJson.put("tags", recommendedTags);

			return new ApiResponse(new HashMap<String, Object>() {{
				put("findResult", findResultJson);
				put("tagsByTagId", tagsByTagId);
				put("tagGroup", tagGroupJson);
			}});
		} else {
			// Support both tag group ID and URL name
			TagGroup tagGroup = getTagService().findTagGroupsByInstitutionId(account.getInstitutionId()).stream()
					.filter(potentialTagGroup -> potentialTagGroup.getTagGroupId().equals(tagGroupId)
							|| potentialTagGroup.getUrlName().equals(tagGroupId))
					.findFirst()
					.orElse(null);

			if (tagGroup == null)
				throw new NotFoundException();

			FindResult<Content> findResult = getContentService().findResourceLibraryContent(new FindResourceLibraryContentRequest() {
				{
					setInstitutionId(account.getInstitutionId());
					setSearchQuery(searchQuery.orElse(null));
					setTagIds(new HashSet<>(tagIds.orElse(List.of())));
					setContentTypeIds(new HashSet<>(contentTypeIds.orElse(List.of())));
					setContentDurationIds(new HashSet<>(contentDurationIds.orElse(List.of())));
					setPageNumber(pageNumber.orElse(0));
					setPageSize(pageSize.orElse(0));
					setTagGroupId(tagGroup.getTagGroupId());
					setPrioritizeUnviewedForAccountId(account.getAccountId());
				}
			});

			List<ContentApiResponse> contents = new ArrayList<>();
			Map<String, TagApiResponse> tagsByTagId = new HashMap<>();

			for (Tag tag : getTagService().findTagsByInstitutionId(account.getInstitutionId()))
				tagsByTagId.put(tag.getTagId(), getTagApiResponseFactory().create(tag));

			for (Content content : findResult.getResults())
				contents.add(getContentApiResponseFactory().create(content));

			Map<String, Object> findResultJson = new HashMap<>();
			findResultJson.put("contents", contents);
			findResultJson.put("totalCount", findResult.getTotalCount());
			findResultJson.put("totalCountDescription", getFormatter().formatNumber(findResult.getTotalCount()));

			return new ApiResponse(new HashMap<String, Object>() {{
				put("findResult", findResultJson);
				put("tagsByTagId", tagsByTagId);
				put("tagGroup", getTagGroupApiResponseFactory().create(tagGroup));
			}});
		}
	}

	@Nonnull
	@GET("/resource-library/tags/{tagId}")
	@AuthenticationRequired
	public ApiResponse resourceLibraryTag(@Nonnull @PathParameter String tagId,
																				@Nonnull @QueryParameter Optional<String> searchQuery,
																				@Nonnull @QueryParameter("contentTypeId") Optional<List<ContentTypeId>> contentTypeIds,
																				@Nonnull @QueryParameter("contentDurationId") Optional<List<ContentDurationId>> contentDurationIds,
																				@Nonnull @QueryParameter Optional<Integer> pageNumber,
																				@Nonnull @QueryParameter Optional<Integer> pageSize) {
		requireNonNull(tagId);
		requireNonNull(searchQuery);
		requireNonNull(contentTypeIds);
		requireNonNull(contentDurationIds);
		requireNonNull(pageNumber);
		requireNonNull(pageSize);

		CurrentContext currentContext = getCurrentContext();
		Account account = currentContext.getAccount().get();

		// Support both tag ID and URL name
		Tag tag = getTagService().findTagsByInstitutionId(account.getInstitutionId()).stream()
				.filter(potentialTag -> potentialTag.getTagId().equals(tagId)
						|| potentialTag.getUrlName().equals(tagId))
				.findFirst()
				.orElse(null);

		if (tag == null)
			throw new NotFoundException();

		TagGroup tagGroup = getTagService().findTagGroupsByInstitutionId(account.getInstitutionId()).stream()
				.filter(potentialTagGroup -> potentialTagGroup.getTagGroupId().equals(tag.getTagGroupId()))
				.findFirst()
				.get();

		FindResult<Content> findResult = getContentService().findResourceLibraryContent(new FindResourceLibraryContentRequest() {
			{
				setInstitutionId(account.getInstitutionId());
				setSearchQuery(searchQuery.orElse(null));
				setTagIds(Set.of(tag.getTagId()));
				setContentTypeIds(new HashSet<>(contentTypeIds.orElse(List.of())));
				setContentDurationIds(new HashSet<>(contentDurationIds.orElse(List.of())));
				setPageNumber(pageNumber.orElse(0));
				setPageSize(pageSize.orElse(0));
				setPrioritizeUnviewedForAccountId(account.getAccountId());
			}
		});

		List<ContentApiResponse> contents = new ArrayList<>();
		Map<String, TagApiResponse> tagsByTagId = new HashMap<>();

		for (Tag currentTag : getTagService().findTagsByInstitutionId(account.getInstitutionId()))
			tagsByTagId.put(currentTag.getTagId(), getTagApiResponseFactory().create(currentTag));

		for (Content content : findResult.getResults())
			contents.add(getContentApiResponseFactory().create(content));

		Map<String, Object> findResultJson = new HashMap<>();
		findResultJson.put("contents", contents);
		findResultJson.put("totalCount", findResult.getTotalCount());
		findResultJson.put("totalCountDescription", getFormatter().formatNumber(findResult.getTotalCount()));

		return new ApiResponse(new HashMap<String, Object>() {{
			put("findResult", findResultJson);
			put("tagsByTagId", tagsByTagId);
			put("tagGroup", getTagGroupApiResponseFactory().create(tagGroup));
			put("tag", getTagApiResponseFactory().create(tag));
		}});
	}

	@Nonnull
	@GET("/resource-library/tag-group-filters/{tagGroupId}")
	@AuthenticationRequired
	public ApiResponse tagGroupFilters(@Nonnull @PathParameter String tagGroupId) {
		requireNonNull(tagGroupId);

		CurrentContext currentContext = getCurrentContext();
		Account account = currentContext.getAccount().get();

		// TODO: remove this harcoded RECOMMENDED support - it's a temporary workaround for an iOS build
		if (tagGroupId.toUpperCase(Locale.ENGLISH).equals("RECOMMENDED")) {
			EnterprisePlugin enterprisePlugin = getEnterprisePluginProvider().enterprisePluginForCurrentInstitution();
			List<Content> contents = enterprisePlugin.recommendedContentForAccountId(account.getAccountId());
			Set<String> tagIds = new HashSet<>();

			for (Content content : contents)
				for (Tag tag : content.getTags())
					tagIds.add(tag.getTagId());

			List<TagApiResponse> tags = getTagService().findTagsByInstitutionId(account.getInstitutionId()).stream()
					.filter(tag -> tagIds.contains(tag.getTagId()))
					.map(tag -> getTagApiResponseFactory().create(tag))
					.collect(Collectors.toList());

			return new ApiResponse(new HashMap<String, Object>() {{
				put("contentDurations", availableContentDurations());
				put("contentTypes", availableContentTypes());
				put("tags", tags);
			}});
		} else {

			// Support both tag group ID and URL name
			TagGroup tagGroup = getTagService().findTagGroupsByInstitutionId(account.getInstitutionId()).stream()
					.filter(potentialTagGroup -> potentialTagGroup.getTagGroupId().equals(tagGroupId)
							|| potentialTagGroup.getUrlName().equals(tagGroupId))
					.findFirst()
					.orElse(null);

			if (tagGroup == null)
				throw new NotFoundException();

			List<TagApiResponse> tags = getTagService().findTagsByInstitutionId(account.getInstitutionId()).stream()
					.filter(tag -> tag.getTagGroupId().equals(tagGroup.getTagGroupId()))
					.map(tag -> getTagApiResponseFactory().create(tag))
					.collect(Collectors.toList());

			return new ApiResponse(new HashMap<String, Object>() {{
				put("contentDurations", availableContentDurations());
				put("contentTypes", availableContentTypes());
				put("tags", tags);
			}});
		}
	}

	@Nonnull
	@GET("/resource-library/tag-filters/{tagIdentifier}")
	@AuthenticationRequired
	public ApiResponse tagFilters(@Nonnull @PathParameter String tagIdentifier) {
		requireNonNull(tagIdentifier);

		CurrentContext currentContext = getCurrentContext();
		Account account = currentContext.getAccount().get();

		// Support both tag ID and URL name
		Tag tag = getTagService().findTagsByInstitutionId(account.getInstitutionId()).stream()
				.filter(potentialTag -> potentialTag.getTagId().equals(tagIdentifier)
						|| potentialTag.getUrlName().equals(tagIdentifier))
				.findFirst()
				.orElse(null);

		if (tag == null)
			throw new NotFoundException();

		// Webapp needs some additional response data to do its work
		TagGroup tagGroup = getTagService().findTagGroupsByInstitutionId(account.getInstitutionId()).stream()
				.filter(potentialTagGroup -> potentialTagGroup.getTagGroupId().equals(tag.getTagGroupId()))
				.findAny().orElse(null);

		if (tagGroup == null)
			throw new IllegalStateException(format("Missing tag group for tag ID %s", tag.getTagId()));

		Map<String, TagApiResponse> tagsByTagId = new HashMap<>();

		for (Tag currentTag : getTagService().findTagsByInstitutionId(account.getInstitutionId()))
			tagsByTagId.put(currentTag.getTagId(), getTagApiResponseFactory().create(currentTag));

		return new ApiResponse(new HashMap<String, Object>() {{
			put("contentDurations", availableContentDurations());
			put("contentTypes", availableContentTypes());
			put("tag", getTagApiResponseFactory().create(tag));
			put("tagGroup", getTagGroupApiResponseFactory().create(tagGroup));
			put("tagsByTagId", tagsByTagId);
		}});
	}

	@Nonnull
	@GET("/resource-library/content-types")
	@AuthenticationRequired
	public ApiResponse contentTypes() {
		return new ApiResponse(new HashMap<String, Object>() {{
			put("contentTypes", availableContentTypes());
		}});
	}

	@Nonnull
	@GET("/resource-library/content-durations")
	@AuthenticationRequired
	public ApiResponse contentDurations() {
		return new ApiResponse(new HashMap<String, Object>() {{
			put("contentDurations", availableContentDurations());
		}});
	}

	@Nonnull
	@GET("/resource-library/filters")
	@AuthenticationRequired
	public ApiResponse filters() {
		InstitutionId institutionId = getCurrentContext().getInstitutionId();

		List<TagGroupApiResponse> tagGroups = getTagService().findTagGroupsByInstitutionId(institutionId).stream()
				.filter(tagGroup -> !tagGroup.getDeprecated())
				.map(tagGroup -> getTagGroupApiResponseFactory().create(tagGroup))
				.collect(Collectors.toList());

		List<ContentAudienceTypeApiResponse> contentAudienceTypes = getContentService().findContentAudienceTypes().stream()
				.map(contentAudienceType -> getContentAudienceTypeApiResponseFactory().create(contentAudienceType))
				.collect(Collectors.toList());

		List<ContentAudienceTypeGroupApiResponse> contentAudienceTypeGroups = getContentService().findContentAudienceTypeGroups().stream()
				.map(contentAudienceTypeGroup -> getContentAudienceTypeGroupApiResponseFactory().create(contentAudienceTypeGroup))
				.collect(Collectors.toList());

		return new ApiResponse(Map.of(
				"tagGroups", tagGroups,
				"contentAudienceTypes", contentAudienceTypes,
				"contentAudienceTypeGroups", contentAudienceTypeGroups,
				"contentTypes", availableContentTypes(),
				"contentDurations", availableContentDurations(),
				"resourceLibrarySortColumnIds", availableResourceLibrarySortColumnIds()
		));
	}

	@Nonnull
	protected List<Map<String, Object>> availableResourceLibrarySortColumnIds() {
		return List.of(
				Map.of(
						"resourceLibrarySortColumnId", ResourceLibrarySortColumnId.MOST_RECENT,
						"description", getStrings().get("Most Recent")
				),
				Map.of(
						"resourceLibrarySortColumnId", ResourceLibrarySortColumnId.MOST_VIEWED,
						"description", getStrings().get("Most Viewed")
				)
		);
	}

	@Nonnull
	protected List<Map<String, Object>> availableContentDurations() {
		List<Map<String, Object>> contentDurations = new ArrayList<>();
		contentDurations.add(Map.of("contentDurationId", ContentDurationId.UNDER_FIVE_MINUTES, "description", getStrings().get("< 5 Minutes")));
		contentDurations.add(Map.of("contentDurationId", ContentDurationId.BETWEEN_FIVE_AND_TEN_MINUTES, "description", getStrings().get("5-10 Minutes")));
		contentDurations.add(Map.of("contentDurationId", ContentDurationId.BETWEEN_TEN_AND_THIRTY_MINUTES, "description", getStrings().get("10-30 Minutes")));
		contentDurations.add(Map.of("contentDurationId", ContentDurationId.OVER_THIRTY_MINUTES, "description", getStrings().get("> 30 Minutes")));

		return Collections.unmodifiableList(contentDurations);
	}

	@Nonnull
	protected List<Map<String, Object>> availableContentTypes() {
		return getContentService().findContentTypes().stream()
				.map(contentType -> Map.<String, Object>of(
						"contentTypeId", contentType.getContentTypeId(),
						"description", contentType.getDescription(),
						"callToAction", contentType.getCallToAction()))
				.collect(Collectors.toList());
	}

	@Nonnull
	protected ContentService getContentService() {
		return this.contentService;
	}

	@Nonnull
	protected TagService getTagService() {
		return this.tagService;
	}

	@Nonnull
	protected AuthorizationService getAuthorizationService() {
		return this.authorizationService;
	}

	@Nonnull
	protected EnterprisePluginProvider getEnterprisePluginProvider() {
		return this.enterprisePluginProvider;
	}

	@Nonnull
	protected Formatter getFormatter() {
		return this.formatter;
	}

	@Nonnull
	protected Strings getStrings() {
		return this.strings;
	}

	@Nonnull
	protected ContentApiResponseFactory getContentApiResponseFactory() {
		return this.contentApiResponseFactory;
	}

	@Nonnull
	protected TagApiResponseFactory getTagApiResponseFactory() {
		return this.tagApiResponseFactory;
	}

	@Nonnull
	protected TagGroupApiResponseFactory getTagGroupApiResponseFactory() {
		return this.tagGroupApiResponseFactory;
	}

	@Nonnull
	protected ContentAudienceTypeApiResponseFactory getContentAudienceTypeApiResponseFactory() {
		return this.contentAudienceTypeApiResponseFactory;
	}

	@Nonnull
	protected ContentAudienceTypeGroupApiResponseFactory getContentAudienceTypeGroupApiResponseFactory() {
		return this.contentAudienceTypeGroupApiResponseFactory;
	}

	@Nonnull
	protected CurrentContext getCurrentContext() {
		return this.currentContextProvider.get();
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}
