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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ReportingChart {
	@Nullable
	private ReportingChartTypeId chartTypeId;
	@Nullable
	private ReportingChartIntervalId intervalId;
	@Nullable
	private ReportingChartDisplayPreferenceId displayPreferenceId;
	@Nullable
	private String title;
	@Nullable
	private String detail;
	@Nullable
	private List<ReportingChartElement> elements;

	@Nullable
	public ReportingChartTypeId getChartTypeId() {
		return chartTypeId;
	}

	public void setChartTypeId(@Nullable ReportingChartTypeId chartTypeId) {
		this.chartTypeId = chartTypeId;
	}

	@Nullable
	public ReportingChartIntervalId getIntervalId() {
		return intervalId;
	}

	public void setIntervalId(@Nullable ReportingChartIntervalId intervalId) {
		this.intervalId = intervalId;
	}

	@Nullable
	public ReportingChartDisplayPreferenceId getDisplayPreferenceId() {
		return displayPreferenceId;
	}

	public void setDisplayPreferenceId(@Nullable ReportingChartDisplayPreferenceId displayPreferenceId) {
		this.displayPreferenceId = displayPreferenceId;
	}

	@Nullable
	public String getTitle() {
		return title;
	}

	public void setTitle(@Nullable String title) {
		this.title = title;
	}

	@Nullable
	public String getDetail() {
		return detail;
	}

	public void setDetail(@Nullable String detail) {
		this.detail = detail;
	}

	@Nullable
	public List<ReportingChartElement> getElements() {
		return elements;
	}

	public void setElements(@Nullable List<ReportingChartElement> elements) {
		this.elements = elements;
	}
}
