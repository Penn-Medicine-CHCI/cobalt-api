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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class BusinessHourOverride {
	@Nullable
	private UUID businessHourOverrideId;
	@Nullable
	private UUID businessHourId;
	@Nullable
	private LocalDate date;
	@Nullable
	private LocalTime openTime;
	@Nullable
	private LocalTime closeTime;
	@Nullable
	private String description;

	@Nullable
	public UUID getBusinessHourOverrideId() {
		return this.businessHourOverrideId;
	}

	public void setBusinessHourOverrideId(@Nullable UUID businessHourOverrideId) {
		this.businessHourOverrideId = businessHourOverrideId;
	}

	@Nullable
	public UUID getBusinessHourId() {
		return this.businessHourId;
	}

	public void setBusinessHourId(@Nullable UUID businessHourId) {
		this.businessHourId = businessHourId;
	}

	@Nullable
	public LocalDate getDate() {
		return this.date;
	}

	public void setDate(@Nullable LocalDate date) {
		this.date = date;
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

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}
}