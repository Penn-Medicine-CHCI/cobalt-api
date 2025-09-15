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
public class AnalyticsWidgetTableData {
	@Nullable
	private List<String> headers;
	@Nullable
	private List<AnalyticsWidgetTableRow> rows;

	@Nullable
	public List<String> getHeaders() {
		return this.headers;
	}

	public void setHeaders(@Nullable List<String> headers) {
		this.headers = headers;
	}

	@Nullable
	public List<AnalyticsWidgetTableRow> getRows() {
		return this.rows;
	}

	public void setRows(@Nullable List<AnalyticsWidgetTableRow> rows) {
		this.rows = rows;
	}
}