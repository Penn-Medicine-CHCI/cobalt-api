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
 * @author Transmogrify LLC.
 */
@NotThreadSafe
public class ScreeningImage {
	@Nullable
	private ScreeningImageId screeningImageId;
	@Nullable
	private String description;

	public enum ScreeningImageId {
		APPOINTMENT,
		CONNECTED_TO_CARE,
		CONNECTING_TO_CARE,
		FEELING_RECENTLY,
		GOALS,
		KEEP_GOING,
		NEXT_APPOINTMENT_SCHEDULED,
		RESOURCES,
		SAFETY,
		SCREENING_COMPLETE,
		SCREENING_TO_DO,
		WELCOME,
		SUDS_THERMOMETER,
		PEOPLE_PUZZLE,
		SLEEP
	}

	@Override
	public String toString() {
		return format("%s{screeningImageId=%s, description=%s}", getClass().getSimpleName(), getScreeningImageId(), getDescription());
	}

	@Nullable
	public ScreeningImageId getScreeningImageId() {
		return this.screeningImageId;
	}

	public void setScreeningImageId(@Nullable ScreeningImageId screeningImageId) {
		this.screeningImageId = screeningImageId;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}
}