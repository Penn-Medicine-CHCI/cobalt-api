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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.math.BigDecimal;
import java.util.UUID;


/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class CourseSessionCompletionPercentage {
	@Nonnull
	private UUID courseSessionId;

	@Nonnull
	private Integer minutesCompleted;

	@Nonnull
	private Integer totalMinutes;
	@Nonnull
	private BigDecimal completionPercentage;

	@Nonnull
	public UUID getCourseSessionId() {
		return courseSessionId;
	}

	public void setCourseSessionId(@Nonnull UUID courseSessionId) {
		this.courseSessionId = courseSessionId;
	}

	@Nonnull
	public Integer getMinutesCompleted() {
		return minutesCompleted;
	}

	public void setMinutesCompleted(@Nonnull Integer minutesCompleted) {
		this.minutesCompleted = minutesCompleted;
	}

	@Nonnull
	public Integer getTotalMinutes() {
		return totalMinutes;
	}

	public void setTotalMinutes(@Nonnull Integer totalMinutes) {
		this.totalMinutes = totalMinutes;
	}

	@Nonnull
	public BigDecimal getCompletionPercentage() {
		return completionPercentage;
	}

	public void setCompletionPercentage(@Nonnull BigDecimal completionPercentage) {
		this.completionPercentage = completionPercentage;
	}
}
