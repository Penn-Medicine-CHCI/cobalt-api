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
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class BusinessHour {
	@Nullable
	private UUID businessHourId;
	@Nullable
	private DayOfWeek dayOfWeek;
	@Nullable
	private LocalTime openTime;
	@Nullable
	private LocalTime closeTime;

	@Nullable
	public UUID getBusinessHourId() {
		return this.businessHourId;
	}

	public void setBusinessHourId(@Nullable UUID businessHourId) {
		this.businessHourId = businessHourId;
	}

	@Nullable
	public DayOfWeek getDayOfWeek() {
		return this.dayOfWeek;
	}

	public void setDayOfWeek(@Nullable DayOfWeek dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	@Nullable
	public LocalTime getOpenTime() {
		return this.openTime;
	}

	public void setOpenTime(@Nullable LocalTime openTime) {
		this.openTime = openTime;
	}

	@Nullable
	public LocalTime getCloseTime() {
		return this.closeTime;
	}

	public void setCloseTime(@Nullable LocalTime closeTime) {
		this.closeTime = closeTime;
	}
}