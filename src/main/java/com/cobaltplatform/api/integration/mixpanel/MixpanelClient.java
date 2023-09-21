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

package com.cobaltplatform.api.integration.mixpanel;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
public interface MixpanelClient {
	/**
	 * Pulls raw Mixpanel events over a date range per https://developer.mixpanel.com/reference/raw-event-export.
	 *
	 * @param fromDate date range start (inclusive)
	 * @param toDate   date range end (inclusive)
	 * @return events that occurred within the provided date range
	 */
	@Nonnull
	List<MixpanelEvent> findEventsForDateRange(@Nonnull LocalDate fromDate,
																						 @Nonnull LocalDate toDate);
}