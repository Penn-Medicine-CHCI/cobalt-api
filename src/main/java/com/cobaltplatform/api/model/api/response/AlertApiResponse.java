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

package com.cobaltplatform.api.model.api.response;

import com.cobaltplatform.api.model.db.Alert;
import com.cobaltplatform.api.model.db.AlertType.AlertTypeId;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class AlertApiResponse {
	@Nonnull
	private final UUID alertId;
	@Nonnull
	private final AlertTypeId alertTypeId;
	@Nonnull
	private final String title;
	@Nonnull
	private final String message;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface AlertApiResponseFactory {
		@Nonnull
		AlertApiResponse create(@Nonnull Alert alert);
	}

	@AssistedInject
	public AlertApiResponse(@Nonnull Formatter formatter,
													@Nonnull Strings strings,
													@Assisted @Nonnull Alert alert) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(alert);

		this.alertId = alert.getAlertId();
		this.alertTypeId = alert.getAlertTypeId();
		this.title = alert.getTitle();
		this.message = alert.getMessage();
	}

	@Nonnull
	public UUID getAlertId() {
		return this.alertId;
	}

	@Nonnull
	public AlertTypeId getAlertTypeId() {
		return this.alertTypeId;
	}

	@Nonnull
	public String getTitle() {
		return this.title;
	}

	@Nonnull
	public String getMessage() {
		return this.message;
	}
}