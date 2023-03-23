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


import com.cobaltplatform.api.model.db.Feature;
import com.cobaltplatform.api.model.db.Feature.FeatureId;
import com.cobaltplatform.api.model.db.Filter;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
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

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class FeatureService {
	@Nonnull
	private final Database database;
	@Nonnull
	private final Logger logger;

	@Inject
	public FeatureService(@Nonnull Database database) {
		requireNonNull(database);

		this.database = database;
		this.logger = LoggerFactory.getLogger(getClass());
	}

	@Nonnull
	public Optional<Feature> findFeatureById(@Nullable FeatureId featureId) {
		if (featureId == null)
			return Optional.empty();

		return getDatabase().queryForObject("""
				SELECT *
				FROM feature
				WHERE feature_id=?""", Feature.class, featureId);
	}

	@Nonnull
	public List<Filter> findFiltersByFeatureId(@Nullable FeatureId featureId) {

		return getDatabase().queryForList("""
				SELECT f.*
				FROM filter f, feature_filter ff
				WHERE ff.filter_id = f.filter_id AND ff.feature_id=?""", Filter.class, featureId);
	}

	@Nonnull
	public List<SupportRoleId> findSupportRoleByFeatureId(FeatureId featureId) {
		return getDatabase().queryForList("""
				SELECT support_role_id
				FROM feature_support_role
				WHERE feature_id = ?""", SupportRoleId.class, featureId);
	}

	@Nonnull
	public Boolean featureSupportsLocation(FeatureId featureId) {
		return getDatabase().queryForObject("""
				SELECT count(*) > 0
				FROM feature_filter 
				WHERE feature_id = ? AND filter_id = ?""", Boolean.class, featureId, Filter.FilterId.LOCATION).get();
	}

	@Nonnull
	protected Database getDatabase() {
		return this.database;
	}

	@Nonnull
	protected Logger getLogger() {
		return this.logger;
	}
}