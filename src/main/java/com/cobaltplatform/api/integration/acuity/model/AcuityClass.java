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

package com.cobaltplatform.api.integration.acuity.model;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AcuityClass {
	@Nullable
	private Long id;
	@Nullable
	@SerializedName("appointmentTypeID")
	private Long appointmentTypeId;
	@Nullable
	@SerializedName("calendarID")
	private Long calendarId;
	@Nullable
	@SerializedName("serviceGroupID")
	private Long serviceGroupId;
	@Nullable
	private String name;
	@Nullable
	private String time;
	@Nullable
	private String calendar;
	@Nullable
	private Long duration;
	@Nullable
	private Boolean isSeries;
	@Nullable
	private Long slots;
	@Nullable
	private Long slotsAvailable;
	@Nullable
	private String color;
	@Nullable
	private String price;
	@Nullable
	private String category;
	@Nullable
	private String description;
	@Nullable
	private String calendarTimezone;
	@Nullable
	private String localeTime;

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

	@Nullable
	public Long getId() {
		return id;
	}

	public void setId(@Nullable Long id) {
		this.id = id;
	}

	@Nullable
	public Long getAppointmentTypeId() {
		return appointmentTypeId;
	}

	public void setAppointmentTypeId(@Nullable Long appointmentTypeId) {
		this.appointmentTypeId = appointmentTypeId;
	}

	@Nullable
	public Long getCalendarId() {
		return calendarId;
	}

	public void setCalendarId(@Nullable Long calendarId) {
		this.calendarId = calendarId;
	}

	@Nullable
	public Long getServiceGroupId() {
		return serviceGroupId;
	}

	public void setServiceGroupId(@Nullable Long serviceGroupId) {
		this.serviceGroupId = serviceGroupId;
	}

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getTime() {
		return time;
	}

	public void setTime(@Nullable String time) {
		this.time = time;
	}

	@Nullable
	public String getCalendar() {
		return calendar;
	}

	public void setCalendar(@Nullable String calendar) {
		this.calendar = calendar;
	}

	@Nullable
	public Long getDuration() {
		return duration;
	}

	public void setDuration(@Nullable Long duration) {
		this.duration = duration;
	}

	@Nullable
	public Boolean getSeries() {
		return isSeries;
	}

	public void setSeries(@Nullable Boolean series) {
		isSeries = series;
	}

	@Nullable
	public Long getSlots() {
		return slots;
	}

	public void setSlots(@Nullable Long slots) {
		this.slots = slots;
	}

	@Nullable
	public Long getSlotsAvailable() {
		return slotsAvailable;
	}

	public void setSlotsAvailable(@Nullable Long slotsAvailable) {
		this.slotsAvailable = slotsAvailable;
	}

	@Nullable
	public String getColor() {
		return color;
	}

	public void setColor(@Nullable String color) {
		this.color = color;
	}

	@Nullable
	public String getPrice() {
		return price;
	}

	public void setPrice(@Nullable String price) {
		this.price = price;
	}

	@Nullable
	public String getCategory() {
		return category;
	}

	public void setCategory(@Nullable String category) {
		this.category = category;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public String getCalendarTimezone() {
		return calendarTimezone;
	}

	public void setCalendarTimezone(@Nullable String calendarTimezone) {
		this.calendarTimezone = calendarTimezone;
	}

	@Nullable
	public String getLocaleTime() {
		return localeTime;
	}

	public void setLocaleTime(@Nullable String localeTime) {
		this.localeTime = localeTime;
	}
}
