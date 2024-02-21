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

package com.cobaltplatform.api.model.db;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static java.lang.String.format;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class FlowsheetType {
	@Nullable
	private FlowsheetTypeId flowsheetTypeId;
	@Nullable
	private String description;

	public enum FlowsheetTypeId {
		CSSRS_QUESTION_1,
		CSSRS_QUESTION_2,
		CSSRS_QUESTION_3,
		CSSRS_QUESTION_4,
		CSSRS_QUESTION_5,
		CSSRS_QUESTION_6_LIFETIME,
		CSSRS_QUESTION_6_3_MONTHS,
		CSSRS_QUESTION_6_DESCRIPTION,
		CSSRS_IC_RISK_SCORE,
		PHQ9_QUESTION_1,
		PHQ9_QUESTION_2,
		PHQ2_SCORE,
		PHQ9_QUESTION_3,
		PHQ9_QUESTION_4,
		PHQ9_QUESTION_5,
		PHQ9_QUESTION_6,
		PHQ9_QUESTION_7,
		PHQ9_QUESTION_8,
		PHQ9_QUESTION_9,
		PHQ9_TOTAL_SCORE,
		PHQ9_DIFFICULTY_FUNCTIONING,
		GAD7_QUESTION_1,
		GAD7_QUESTION_2,
		GAD7_QUESTION_3,
		GAD7_QUESTION_4,
		GAD7_QUESTION_5,
		GAD7_QUESTION_6,
		GAD7_QUESTION_7,
		GAD7_TOTAL_SCORE,
		GAD7_DIFFICULTY_FUNCTIONING
	}

	@Override
	public String toString() {
		return format("%s{flowsheetTypeId=%s, description=%s}", getClass().getSimpleName(), getFlowsheetTypeId(), getDescription());
	}

	@Nullable
	public FlowsheetTypeId getFlowsheetTypeId() {
		return this.flowsheetTypeId;
	}

	public void setFlowsheetTypeId(@Nullable FlowsheetTypeId flowsheetTypeId) {
		this.flowsheetTypeId = flowsheetTypeId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
