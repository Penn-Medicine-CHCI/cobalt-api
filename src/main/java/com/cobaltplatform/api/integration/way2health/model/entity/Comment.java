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

package com.cobaltplatform.api.integration.way2health.model.entity;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.LocalDateTime;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Comment extends Way2HealthEntity {
	@Nullable
	private User user;
	@Nullable
	private String comment;
	@Nullable
	private LocalDateTime time;

	@Nullable
	public User getUser() {
		return user;
	}

	public void setUser(@Nullable User user) {
		this.user = user;
	}

	@Nullable
	public String getComment() {
		return comment;
	}

	public void setComment(@Nullable String comment) {
		this.comment = comment;
	}

	@Nullable
	public LocalDateTime getTime() {
		return time;
	}

	public void setTime(@Nullable LocalDateTime time) {
		this.time = time;
	}
}
