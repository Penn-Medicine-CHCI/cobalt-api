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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class AcuityCalendar {
	@Nullable
	private Long id;
	@Nullable
	private String name;
	@Nullable
	private String email;
	@Nullable
	private String replyTo;
	@Nullable
	private String description;
	@Nullable
	private String location;
	@Nullable
	private String timezone;
	@Nullable
	private Boolean thumbnail;
	@Nullable
	private Boolean image;

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
	public String getEmail() {
		return email;
	}

	public void setEmail(@Nullable String email) {
		this.email = email;
	}

	@Nullable
	public String getReplyTo() {
		return replyTo;
	}

	public void setReplyTo(@Nullable String replyTo) {
		this.replyTo = replyTo;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public String getLocation() {
		return location;
	}

	public void setLocation(@Nullable String location) {
		this.location = location;
	}

	@Nullable
	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(@Nullable String timezone) {
		this.timezone = timezone;
	}

	@Nullable
	public Boolean getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(@Nullable Boolean thumbnail) {
		this.thumbnail = thumbnail;
	}

	@Nullable
	public Boolean getImage() {
		return image;
	}

	public void setImage(@Nullable Boolean image) {
		this.image = image;
	}
}
