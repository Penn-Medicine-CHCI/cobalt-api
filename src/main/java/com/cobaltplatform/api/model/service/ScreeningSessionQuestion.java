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

import com.cobaltplatform.api.model.db.Screening;
import com.cobaltplatform.api.model.db.ScreeningAnswerOption;
import com.cobaltplatform.api.model.db.ScreeningFlow;
import com.cobaltplatform.api.model.db.ScreeningFlowVersion;
import com.cobaltplatform.api.model.db.ScreeningQuestion;
import com.cobaltplatform.api.model.db.ScreeningSession;
import com.cobaltplatform.api.model.db.ScreeningSessionScreening;
import com.cobaltplatform.api.model.db.ScreeningVersion;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class ScreeningSessionQuestion {
	@Nonnull
	private ScreeningSession screeningSession;
	@Nonnull
	private ScreeningSessionScreening screeningSessionScreening;
	@Nonnull
	private Screening screening;
	@Nonnull
	private ScreeningVersion screeningVersion;
	@Nonnull
	private ScreeningFlow screeningFlow;
	@Nonnull
	private ScreeningFlowVersion screeningFlowVersion;
	@Nonnull
	private ScreeningQuestion screeningQuestion;
	@Nonnull
	private List<ScreeningAnswerOption> screeningAnswerOptions;

	@Nonnull
	public ScreeningSession getScreeningSession() {
		return this.screeningSession;
	}

	public void setScreeningSession(@Nonnull ScreeningSession screeningSession) {
		this.screeningSession = screeningSession;
	}

	@Nonnull
	public ScreeningSessionScreening getScreeningSessionScreening() {
		return this.screeningSessionScreening;
	}

	public void setScreeningSessionScreening(@Nonnull ScreeningSessionScreening screeningSessionScreening) {
		this.screeningSessionScreening = screeningSessionScreening;
	}

	@Nonnull
	public Screening getScreening() {
		return this.screening;
	}

	public void setScreening(@Nonnull Screening screening) {
		this.screening = screening;
	}

	@Nonnull
	public ScreeningVersion getScreeningVersion() {
		return this.screeningVersion;
	}

	public void setScreeningVersion(@Nonnull ScreeningVersion screeningVersion) {
		this.screeningVersion = screeningVersion;
	}

	@Nonnull
	public ScreeningFlow getScreeningFlow() {
		return this.screeningFlow;
	}

	public void setScreeningFlow(@Nonnull ScreeningFlow screeningFlow) {
		this.screeningFlow = screeningFlow;
	}

	@Nonnull
	public ScreeningFlowVersion getScreeningFlowVersion() {
		return this.screeningFlowVersion;
	}

	public void setScreeningFlowVersion(@Nonnull ScreeningFlowVersion screeningFlowVersion) {
		this.screeningFlowVersion = screeningFlowVersion;
	}

	@Nonnull
	public ScreeningQuestion getScreeningQuestion() {
		return this.screeningQuestion;
	}

	public void setScreeningQuestion(@Nonnull ScreeningQuestion screeningQuestion) {
		this.screeningQuestion = screeningQuestion;
	}

	@Nonnull
	public List<ScreeningAnswerOption> getScreeningAnswerOptions() {
		return this.screeningAnswerOptions;
	}

	public void setScreeningAnswerOptions(@Nonnull List<ScreeningAnswerOption> screeningAnswerOptions) {
		this.screeningAnswerOptions = screeningAnswerOptions;
	}
}
