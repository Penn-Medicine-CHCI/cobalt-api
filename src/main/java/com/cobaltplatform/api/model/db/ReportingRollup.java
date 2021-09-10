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

import com.pyranid.DatabaseColumn;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ReportingRollup {
	@Nullable
	private Integer year;
	@Nullable
	private Integer month;
	@Nullable
	@DatabaseColumn("week")
	private Integer dayOfMonth;
	@Nullable
	private Integer userCount;
	@Nullable
	@DatabaseColumn("apt_count")
	private Integer appointmentCount;
	@Nullable
	@DatabaseColumn("apt_completed_count")
	private Integer appointmentCompletedCount;
	@Nullable
	@DatabaseColumn("apt_canceled_count")
	private Integer appointmentCanceledCount;
	@Nullable
	@DatabaseColumn("apt_avail_count")
	private Integer appointmentAvailableCount;
	@Nullable
	@DatabaseColumn("prov_count")
	private Integer providerCount;
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public Integer getYear() {
		return year;
	}

	public void setYear(@Nullable Integer year) {
		this.year = year;
	}

	@Nullable
	public Integer getMonth() {
		return month;
	}

	public void setMonth(@Nullable Integer month) {
		this.month = month;
	}

	@Nullable
	public Integer getDayOfMonth() {
		return dayOfMonth;
	}

	public void setDayOfMonth(@Nullable Integer dayOfMonth) {
		this.dayOfMonth = dayOfMonth;
	}

	@Nullable
	public Integer getUserCount() {
		return userCount;
	}

	public void setUserCount(@Nullable Integer userCount) {
		this.userCount = userCount;
	}

	@Nullable
	public Integer getAppointmentCount() {
		return appointmentCount;
	}

	public void setAppointmentCount(@Nullable Integer appointmentCount) {
		this.appointmentCount = appointmentCount;
	}

	@Nullable
	public Integer getAppointmentCompletedCount() {
		return appointmentCompletedCount;
	}

	public void setAppointmentCompletedCount(@Nullable Integer appointmentCompletedCount) {
		this.appointmentCompletedCount = appointmentCompletedCount;
	}

	@Nullable
	public Integer getAppointmentCanceledCount() {
		return appointmentCanceledCount;
	}

	public void setAppointmentCanceledCount(@Nullable Integer appointmentCanceledCount) {
		this.appointmentCanceledCount = appointmentCanceledCount;
	}

	@Nullable
	public Integer getAppointmentAvailableCount() {
		return appointmentAvailableCount;
	}

	public void setAppointmentAvailableCount(@Nullable Integer appointmentAvailableCount) {
		this.appointmentAvailableCount = appointmentAvailableCount;
	}

	@Nullable
	public Integer getProviderCount() {
		return providerCount;
	}

	public void setProviderCount(@Nullable Integer providerCount) {
		this.providerCount = providerCount;
	}

	@Nullable
	public Instant getCreated() {
		return created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}