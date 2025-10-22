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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AnalyticsLineChartWidget extends AnalyticsWidget {
	@Nullable
	private Number widgetTotal;
	@Nullable
	private String widgetTotalDescription;
	@Nullable
	private String widgetChartLabel;
	@Nullable
	private List<AnalyticsWidgetChartData> widgetData;

	public AnalyticsLineChartWidget() {
		super(AnalyticsWidgetTypeId.LINE_CHART);
	}

	@Nullable
	public Number getWidgetTotal() {
		return this.widgetTotal;
	}

	public void setWidgetTotal(@Nullable Number widgetTotal) {
		this.widgetTotal = widgetTotal;
	}

	@Nullable
	public String getWidgetTotalDescription() {
		return this.widgetTotalDescription;
	}

	public void setWidgetTotalDescription(@Nullable String widgetTotalDescription) {
		this.widgetTotalDescription = widgetTotalDescription;
	}

	@Nullable
	public String getWidgetChartLabel() {
		return this.widgetChartLabel;
	}

	public void setWidgetChartLabel(@Nullable String widgetChartLabel) {
		this.widgetChartLabel = widgetChartLabel;
	}

	@Nullable
	public List<AnalyticsWidgetChartData> getWidgetData() {
		return this.widgetData;
	}

	public void setWidgetData(@Nullable List<AnalyticsWidgetChartData> widgetData) {
		this.widgetData = widgetData;
	}
}