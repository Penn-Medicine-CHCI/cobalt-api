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

package com.cobaltplatform.api.integration.epic.matching;

import com.cobaltplatform.api.integration.epic.response.PatientSearchResponse;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class EpicPatientMatch {
	@Nonnull
	private final PatientSearchResponse.Entry patient;
	@Nonnull
	private final Set<EpicPatientMatchRule> matchRules;
	@Nonnull
	private final Integer score;
	@Nonnull
	private final Boolean match;

	public EpicPatientMatch(@Nonnull PatientSearchResponse.Entry patient,
													@Nonnull Set<EpicPatientMatchRule> matchRules,
													@Nonnull Integer score,
													@Nonnull Boolean match) {
		requireNonNull(patient);
		requireNonNull(matchRules);
		requireNonNull(score);
		requireNonNull(match);

		this.patient = patient;
		this.matchRules = Collections.unmodifiableSet(new HashSet<>(matchRules));
		this.score = score;
		this.match = match;
	}

	@Nonnull
	public PatientSearchResponse.Entry getPatient() {
		return patient;
	}

	@Nonnull
	public Set<EpicPatientMatchRule> getMatchRules() {
		return matchRules;
	}

	@Nonnull
	public Integer getScore() {
		return score;
	}

	@Nonnull
	public Boolean getMatch() {
		return match;
	}
}
