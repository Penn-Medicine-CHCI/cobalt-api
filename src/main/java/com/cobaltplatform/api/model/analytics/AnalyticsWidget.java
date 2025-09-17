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

package com.cobaltplatform.api.model.analytics;

import com.cobaltplatform.api.model.db.ReportType;
import com.cobaltplatform.api.model.db.ReportType.ReportTypeId;
import com.cobaltplatform.api.web.resource.AnalyticsResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public abstract class AnalyticsWidget {
	@Nonnull
	private final AnalyticsWidgetTypeId widgetTypeId;
	@Nullable
	private ReportTypeId widgetReportId;
	@Nullable
	private String widgetTitle;
	@Nullable
	private String widgetSubtitle;

	public AnalyticsWidget(@Nonnull AnalyticsWidgetTypeId widgetTypeId) {
		this.widgetTypeId = requireNonNull(widgetTypeId);
	}

	@Nonnull
	public final AnalyticsWidgetTypeId getWidgetTypeId() {
		return this.widgetTypeId;
	}

	@Nullable
	public ReportTypeId getWidgetReportId() {
		return this.widgetReportId;
	}

	public void setWidgetReportId(@Nullable ReportTypeId widgetReportId) {
		this.widgetReportId = widgetReportId;
	}

	@Nullable
	public String getWidgetTitle() {
		return this.widgetTitle;
	}

	public void setWidgetTitle(@Nullable String widgetTitle) {
		this.widgetTitle = widgetTitle;
	}

	@Nullable
	public String getWidgetSubtitle() {
		return this.widgetSubtitle;
	}

	public void setWidgetSubtitle(@Nullable String widgetSubtitle) {
		this.widgetSubtitle = widgetSubtitle;
	}
}