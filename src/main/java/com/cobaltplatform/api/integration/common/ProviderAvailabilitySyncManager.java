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

package com.cobaltplatform.api.integration.common;

import javax.annotation.Nonnull;
import java.time.LocalDate;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
public interface ProviderAvailabilitySyncManager {
	/**
	 * Forces re-sync of a provider's availability for a particular day by pulling from source system (e.g. Acuity, EPIC) and writing records to our DB.
	 * <p>
	 * This should be called if an appointment is booked through us or we get a webhook notification that one happened.
	 * <p>
	 * This way we don't have to wait for the next automatic re-sync, which might be a little ways in the future.
	 *
	 * @param providerId the provider to sync
	 * @param date       the date that should be synced
	 * @return {@code true} if the sync was performed, {@code false} otherwise
	 */
	@Nonnull
	default Boolean syncProviderAvailability(@Nonnull UUID providerId,
																					 @Nonnull LocalDate date) {

		requireNonNull(providerId);
		requireNonNull(date);

		return syncProviderAvailability(providerId, date, true);
	}

	/**
	 * Forces re-sync of a provider's availability for a particular day by pulling from source system (e.g. Acuity, EPIC) and writing records to our DB.
	 * <p>
	 * This should be called if an appointment is booked through us or we get a webhook notification that one happened.
	 * <p>
	 * This way we don't have to wait for the next automatic re-sync, which might be a little ways in the future.
	 *
	 * @param providerId              the provider to sync
	 * @param date                    the date that should be synced
	 * @param performInOwnTransaction should this be performed in its own transaction to minimize time that the
	 *                                transaction is open?  we normally want this, as sync is run out-of-band, and
	 *                                doesn't need to participate in an existing transaction
	 * @return {@code true} if the sync was performed, {@code false} otherwise
	 */
	@Nonnull
	Boolean syncProviderAvailability(@Nonnull UUID providerId,
																	 @Nonnull LocalDate date,
																	 @Nonnull Boolean performInOwnTransaction);
}
