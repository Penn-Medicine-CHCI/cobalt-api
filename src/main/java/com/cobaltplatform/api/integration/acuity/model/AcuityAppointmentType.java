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
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AcuityAppointmentType {
	@Nullable
	private Long id;
	@Nullable
	private String name;
	@Nullable
	private Boolean active;
	@Nullable
	private String description;
	@Nullable
	private Double duration;
	@Nullable
	private String price;
	@Nullable
	private String category;
	@Nullable
	private String color;
	@Nullable
	@SerializedName("private")
	private Boolean isPrivate;
	@Nullable
	private String service;
	@Nullable
	private String schedulingUrl;
	@Nullable
	private String image;
	@Nullable
	private Long classSize;
	@Nullable
	private Double paddingAfter;
	@Nullable
	private Double paddingBefore;
	@Nullable
	@SerializedName("calendarIDs")
	private List<Long> calendarIds;
	@Nullable
	@SerializedName("addonIDs")
	private List<Long> addonIds;
	@Nullable
	@SerializedName("formIDs")
	private List<Long> formIds;

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
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public Boolean getActive() {
		return active;
	}

	public void setActive(@Nullable Boolean active) {
		this.active = active;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public Double getDuration() {
		return duration;
	}

	public void setDuration(@Nullable Double duration) {
		this.duration = duration;
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
	public String getColor() {
		return color;
	}

	public void setColor(@Nullable String color) {
		this.color = color;
	}

	@Nullable
	public Boolean getPrivate() {
		return isPrivate;
	}

	public void setPrivate(@Nullable Boolean aPrivate) {
		isPrivate = aPrivate;
	}

	@Nullable
	public String getService() {
		return service;
	}

	public void setService(@Nullable String service) {
		this.service = service;
	}

	@Nullable
	public String getSchedulingUrl() {
		return schedulingUrl;
	}

	public void setSchedulingUrl(@Nullable String schedulingUrl) {
		this.schedulingUrl = schedulingUrl;
	}

	@Nullable
	public String getImage() {
		return image;
	}

	public void setImage(@Nullable String image) {
		this.image = image;
	}

	@Nullable
	public Long getClassSize() {
		return classSize;
	}

	public void setClassSize(@Nullable Long classSize) {
		this.classSize = classSize;
	}

	@Nullable
	public Double getPaddingAfter() {
		return paddingAfter;
	}

	public void setPaddingAfter(@Nullable Double paddingAfter) {
		this.paddingAfter = paddingAfter;
	}

	@Nullable
	public Double getPaddingBefore() {
		return paddingBefore;
	}

	public void setPaddingBefore(@Nullable Double paddingBefore) {
		this.paddingBefore = paddingBefore;
	}

	@Nullable
	public List<Long> getCalendarIds() {
		return calendarIds;
	}

	public void setCalendarIds(@Nullable List<Long> calendarIds) {
		this.calendarIds = calendarIds;
	}

	@Nullable
	public List<Long> getAddonIds() {
		return addonIds;
	}

	public void setAddonIds(@Nullable List<Long> addonIds) {
		this.addonIds = addonIds;
	}

	@Nullable
	public List<Long> getFormIds() {
		return formIds;
	}

	public void setFormIds(@Nullable List<Long> formIds) {
		this.formIds = formIds;
	}
}
