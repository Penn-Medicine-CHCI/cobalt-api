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

import com.cobaltplatform.api.model.db.Faq;
import com.cobaltplatform.api.model.db.FaqTopic;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class FaqService {
	@Nonnull
	private final DatabaseProvider databaseProvider;
	@Nonnull
	private final Strings strings;
	@Nonnull
	private final Logger logger;

	@Inject
	public FaqService(@Nonnull DatabaseProvider databaseProvider,
										@Nonnull Strings strings) {
		requireNonNull(databaseProvider);
		requireNonNull(strings);

		this.databaseProvider = databaseProvider;
		this.strings = strings;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<Faq> findFaqById(@Nullable UUID faqId) {
		if (faqId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM faq
				WHERE faq_id=?
				""", Faq.class, faqId);
	}

	@Nonnull
	public Optional<Faq> findFaqByInstitutionIdAndUrlName(@Nullable InstitutionId institutionId,
																												@Nullable String urlName) {
		if (institutionId == null || urlName == null)
			return Optional.empty();

		urlName = urlName.trim();

		return getDatabase().queryForObject("""
				SELECT *
				FROM faq
				WHERE institution_id=?
				AND url_name=?
				""", Faq.class, institutionId, urlName);
	}

	@Nonnull
	public Optional<Faq> findFaqByIdentifier(@Nullable Object faqIdentifier,
																					 @Nullable InstitutionId institutionId) {
		if (faqIdentifier == null || institutionId == null)
			return Optional.empty();

		if (faqIdentifier instanceof UUID)
			return findFaqById((UUID) faqIdentifier);

		if (faqIdentifier instanceof String)
			return findFaqByInstitutionIdAndUrlName(institutionId, (String) faqIdentifier);

		return Optional.empty();
	}

	@Nonnull
	public List<Faq> findFaqsByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM faq
				WHERE institution_id=?
				ORDER BY display_order, question
				""", Faq.class, institutionId);
	}

	@Nonnull
	public List<FaqTopic> findFaqTopicsByInstitutionId(@Nullable InstitutionId institutionId) {
		if (institutionId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM faq_topic
				WHERE institution_id=?
				ORDER BY display_order
				""", FaqTopic.class, institutionId);
	}

	@Nonnull
	public Optional<FaqTopic> findFaqTopicById(@Nullable UUID faqTopicId) {
		if (faqTopicId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM faq_topic
				WHERE faq_topic_id=?
				""", FaqTopic.class, faqTopicId);
	}

	@Nonnull
	public Optional<FaqTopic> findFaqTopicByInstitutionIdAndUrlName(@Nullable InstitutionId institutionId,
																																	@Nullable String urlName) {
		if (institutionId == null || urlName == null)
			return Optional.empty();

		urlName = urlName.trim();

		return getDatabase().queryForObject("""
				SELECT *
				FROM faq_topic
				WHERE institution_id=?
				AND url_name=?
				""", FaqTopic.class, institutionId, urlName);
	}

	@Nonnull
	public Optional<FaqTopic> findFaqTopicByIdentifier(@Nullable Object faqTopicIdentifier,
																										 @Nullable InstitutionId institutionId) {
		if (faqTopicIdentifier == null || institutionId == null)
			return Optional.empty();

		if (faqTopicIdentifier instanceof UUID)
			return findFaqTopicById((UUID) faqTopicIdentifier);

		if (faqTopicIdentifier instanceof String)
			return findFaqTopicByInstitutionIdAndUrlName(institutionId, (String) faqTopicIdentifier);

		return Optional.empty();
	}

	@Nonnull
	public List<Faq> findFaqsByFaqTopicId(@Nullable UUID faqTopicId) {
		if (faqTopicId == null)
			return List.of();

		return getDatabase().queryForList("""
				SELECT *
				FROM faq
				WHERE faq_topic_id=?
				ORDER BY display_order
				""", Faq.class, faqTopicId);
	}

	@Nonnull
	protected Database getDatabase() {
		return this.databaseProvider.get();
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
