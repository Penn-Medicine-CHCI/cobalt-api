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

package com.cobaltplatform.api.model.service;

import com.cobaltplatform.api.model.db.Provider;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class GroupEvent {
	@Nullable
	private String groupEventId; // Really acuity class id (for now)
	@Nullable
	private UUID groupEventTypeId;
	@Nullable
	private String name;
	@Nullable
	private String description;
	@Nullable
	private Instant startTime;
	@Nullable
	private Instant endTime;
	@Nullable
	private Long durationInMinutes;
	@Nullable
	private Long seats;
	@Nullable
	private Long seatsAvailable;
	@Nullable
	private ZoneId timeZone;
	@Nullable
	private String imageUrl;
	@Nullable
	private Boolean isDefaultImageUrl;
	@Nullable
	private String videoconferenceUrl;
	@Nullable
	private Provider provider;

	@Nullable
	public String getGroupEventId() {
		return groupEventId;
	}

	public void setGroupEventId(@Nullable String groupEventId) {
		this.groupEventId = groupEventId;
	}

	@Nullable
	public UUID getGroupEventTypeId() {
		return groupEventTypeId;
	}

	public void setGroupEventTypeId(@Nullable UUID groupEventTypeId) {
		this.groupEventTypeId = groupEventTypeId;
	}

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public Instant getStartTime() {
		return startTime;
	}

	public void setStartTime(@Nullable Instant startTime) {
		this.startTime = startTime;
	}

	@Nullable
	public Instant getEndTime() {
		return endTime;
	}

	public void setEndTime(@Nullable Instant endTime) {
		this.endTime = endTime;
	}

	@Nullable
	public Long getDurationInMinutes() {
		return durationInMinutes;
	}

	public void setDurationInMinutes(@Nullable Long durationInMinutes) {
		this.durationInMinutes = durationInMinutes;
	}

	@Nullable
	public Long getSeats() {
		return seats;
	}

	public void setSeats(@Nullable Long seats) {
		this.seats = seats;
	}

	@Nullable
	public Long getSeatsAvailable() {
		return seatsAvailable;
	}

	public void setSeatsAvailable(@Nullable Long seatsAvailable) {
		this.seatsAvailable = seatsAvailable;
	}

	@Nullable
	public ZoneId getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(@Nullable ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	@Nullable
	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(@Nullable String imageUrl) {
		this.imageUrl = imageUrl;
	}

	@Nullable
	public Boolean getDefaultImageUrl() {
		return isDefaultImageUrl;
	}

	public void setDefaultImageUrl(@Nullable Boolean defaultImageUrl) {
		isDefaultImageUrl = defaultImageUrl;
	}

	@Nullable
	public Provider getProvider() {
		return provider;
	}

	public void setProvider(@Nullable Provider provider) {
		this.provider = provider;
	}

	@Nullable
	public String getVideoconferenceUrl() {
		return videoconferenceUrl;
	}

	public void setVideoconferenceUrl(@Nullable String videoconferenceUrl) {
		this.videoconferenceUrl = videoconferenceUrl;
	}
}
