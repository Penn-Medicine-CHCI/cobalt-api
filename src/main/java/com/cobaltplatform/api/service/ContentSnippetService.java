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

import com.cobaltplatform.api.model.db.ContentSnippet;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.util.db.DatabaseProvider;
import com.lokalized.Strings;
import com.pyranid.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static com.cobaltplatform.api.util.DatabaseUtility.sqlInListPlaceholders;
import static com.cobaltplatform.api.util.DatabaseUtility.sqlVaragsParameters;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class ContentSnippetService {
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public ContentSnippetService(@Nonnull DatabaseProvider databaseProvider,
															 @Nonnull Strings strings) {
		requireNonNull(databaseProvider);
		requireNonNull(strings);

		this.databaseProvider = databaseProvider;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public List<ContentSnippet> findContentSnippetsByInstitutionIdAndKeys(@Nullable InstitutionId institutionId,
																																 @Nullable List<String> contentSnippetKeys) {
		if (institutionId == null || contentSnippetKeys == null || contentSnippetKeys.isEmpty())
			return List.of();

		LinkedHashSet<String> normalizedContentSnippetKeys = new LinkedHashSet<>();

		for (String contentSnippetKey : contentSnippetKeys) {
			String normalizedContentSnippetKey = trimToNull(contentSnippetKey);

			if (normalizedContentSnippetKey != null)
				normalizedContentSnippetKeys.add(normalizedContentSnippetKey);
		}

		if (normalizedContentSnippetKeys.isEmpty())
			return List.of();

		List<Object> parameters = new ArrayList<>();
		parameters.add(institutionId);

		for (Object parameter : sqlVaragsParameters(normalizedContentSnippetKeys))
			parameters.add(parameter);

		return getDatabase().queryForList(format("""
				SELECT *
				FROM content_snippet
				WHERE institution_id=?
				AND content_snippet_key IN %s
				ORDER BY content_snippet_key
				""", sqlInListPlaceholders(normalizedContentSnippetKeys)), ContentSnippet.class, parameters.toArray());
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
	}
}
