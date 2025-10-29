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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AnalyticsMultiChartWidget extends AnalyticsWidget {
	@Nullable
	private Number widgetTotal;
	@Nullable
	private String widgetTotalDescription;
	@Nullable
	private WidgetData widgetData;

	// Conforms to FE model:
	//
	// export interface AdminAnalyticsMultiChartWidget extends BaseAdminAnalyticsWidget {
	//	widgetTypeId: 'MULTI_CHART';
	//	widgetTotal: number;
	//	widgetTotalDescription: string;
	//	widgetChartLabel: string;
	//	widgetData: {
	//		labels: string[];
	//		datasets: {
	//			type: 'line' | 'bar' | 'pie';
	//			label: string;
	//			data: number[];
	//			backgroundColor: string[];
	//			borderColor: string[];
	//		}[];
	//	};
	// }

	public AnalyticsMultiChartWidget() {
		super(AnalyticsWidgetTypeId.MULTI_CHART);
	}

	// See GsonUtility for marshaling configuration
	public enum DatasetType {
		LINE("line"),
		BAR("bar"),
		PIE("pie");

		@Nonnull
		private final String jsonRepresentation;

		private DatasetType(@Nonnull String jsonRepresentation) {
			requireNonNull(jsonRepresentation);
			this.jsonRepresentation = jsonRepresentation;
		}

		@Nonnull
		public String getJsonRepresentation() {
			return this.jsonRepresentation;
		}
	}

	// Note: all lists must have matching lengths
	@NotThreadSafe
	public static class Dataset {
		@Nullable
		private DatasetType type;
		@Nullable
		private String label;
		@Nullable
		private List<Number> data;
		@Nullable
		private List<String> dataDescriptions;
		@Nullable
		private List<String> backgroundColor;
		@Nullable
		private List<String> borderColor;

		@Nullable
		public DatasetType getType() {
			return this.type;
		}

		public void setType(@Nullable DatasetType type) {
			this.type = type;
		}

		@Nullable
		public String getLabel() {
			return this.label;
		}

		public void setLabel(@Nullable String label) {
			this.label = label;
		}

		@Nullable
		public List<Number> getData() {
			return this.data;
		}

		public void setData(@Nullable List<Number> data) {
			this.data = data;
		}

		@Nullable
		public List<String> getDataDescriptions() {
			return this.dataDescriptions;
		}

		public void setDataDescriptions(@Nullable List<String> dataDescriptions) {
			this.dataDescriptions = dataDescriptions;
		}

		@Nullable
		public List<String> getBackgroundColor() {
			return this.backgroundColor;
		}

		public void setBackgroundColor(@Nullable List<String> backgroundColor) {
			this.backgroundColor = backgroundColor;
		}

		@Nullable
		public List<String> getBorderColor() {
			return this.borderColor;
		}

		public void setBorderColor(@Nullable List<String> borderColor) {
			this.borderColor = borderColor;
		}
	}

	@NotThreadSafe
	public static class WidgetData {
		@Nullable
		private List<String> labels;
		@Nullable
		private List<Dataset> datasets;

		@Nullable
		public List<String> getLabels() {
			return this.labels;
		}

		public void setLabels(@Nullable List<String> labels) {
			this.labels = labels;
		}

		@Nullable
		public List<Dataset> getDatasets() {
			return this.datasets;
		}

		public void setDatasets(@Nullable List<Dataset> datasets) {
			this.datasets = datasets;
		}
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
	public WidgetData getWidgetData() {
		return this.widgetData;
	}

	public void setWidgetData(@Nullable WidgetData widgetData) {
		this.widgetData = widgetData;
	}
}