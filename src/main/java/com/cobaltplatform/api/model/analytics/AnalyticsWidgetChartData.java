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

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AnalyticsWidgetChartData {
	@Nullable
	private String label;
	@Nullable
	private Number count;
	@Nullable
	private String countDescription;
	@Nullable
	private String color;

	@Nullable
	public String getLabel() {
		return this.label;
	}

	public void setLabel(@Nullable String label) {
		this.label = label;
	}

	@Nullable
	public Number getCount() {
		return this.count;
	}

	public void setCount(@Nullable Number count) {
		this.count = count;
	}

	@Nullable
	public String getCountDescription() {
		return this.countDescription;
	}

	public void setCountDescription(@Nullable String countDescription) {
		this.countDescription = countDescription;
	}

	@Nullable
	public String getColor() {
		return this.color;
	}

	public void setColor(@Nullable String color) {
		this.color = color;
	}
}