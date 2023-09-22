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

package com.cobaltplatform.api.integration.google;

import com.google.cloud.bigquery.FieldValueList;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
public interface GoogleBigQueryClient {
	@Nonnull
	String getProjectId();

	@Nonnull
	String getBigQueryResourceId();

	@Nonnull
	default String getDatasetId() {
		return format("%s.%s", getProjectId(), getBigQueryResourceId());
	}

	@Nonnull
	List<FieldValueList> queryForList(@Nonnull String sql);

	@Nonnull
	default String dateAsTableSuffix(@Nonnull LocalDate date) {
		requireNonNull(date);
		// e.g. Oct 31, 2023 would be "20231031"
		return DateTimeFormatter.ISO_DATE.format(date).replace("-", "");
	}
}
