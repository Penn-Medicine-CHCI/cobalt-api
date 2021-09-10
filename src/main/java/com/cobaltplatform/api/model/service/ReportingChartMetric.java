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

package com.cobaltplatform.api.model.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ReportingChartMetric {
	@Nonnull
	private ReportingChartMetricTypeId metricTypeId;
	@Nonnull
	private String description;
	@Nonnull
	private Double count;
	@Nullable
	private Integer hexColor;
	@Nullable
	private Double alpha;

	@Nonnull
	public ReportingChartMetricTypeId getMetricTypeId() {
		return metricTypeId;
	}

	public void setMetricTypeId(@Nonnull ReportingChartMetricTypeId metricTypeId) {
		this.metricTypeId = metricTypeId;
	}

	@Nonnull
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nonnull String description) {
		this.description = description;
	}

	@Nonnull
	public Double getCount() {
		return count;
	}

	public void setCount(@Nonnull Double count) {
		this.count = count;
	}

	@Nullable
	public Integer getHexColor() {
		return hexColor;
	}

	public void setHexColor(@Nullable Integer hexColor) {
		this.hexColor = hexColor;
	}

	@Nullable
	public Double getAlpha() {
		return alpha;
	}

	public void setAlpha(@Nullable Double alpha) {
		this.alpha = alpha;
	}
}
