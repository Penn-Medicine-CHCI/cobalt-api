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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ReferringPractice {
	@Nullable
	private String referringPracticeId;
	@Nullable
	private String referringPracticeName;

	@Nullable
	public String getReferringPracticeId() {
		return this.referringPracticeId;
	}

	public void setReferringPracticeId(@Nullable String referringPracticeId) {
		this.referringPracticeId = referringPracticeId;
	}

	@Nullable
	public String getReferringPracticeName() {
		return this.referringPracticeName;
	}

	public void setReferringPracticeName(@Nullable String referringPracticeName) {
		this.referringPracticeName = referringPracticeName;
	}
}