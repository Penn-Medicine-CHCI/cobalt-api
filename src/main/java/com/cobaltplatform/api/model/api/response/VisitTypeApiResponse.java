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

import com.cobaltplatform.api.model.db.VisitType;
import com.cobaltplatform.api.model.db.VisitType.VisitTypeId;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class VisitTypeApiResponse {
	@Nonnull
	private final VisitTypeId visitTypeId;
	@Nonnull
	private final String description;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface VisitTypeApiResponseFactory {
		@Nonnull
		VisitTypeApiResponse create(@Nonnull VisitType visitType);
	}

	@AssistedInject
	public VisitTypeApiResponse(@Nonnull Formatter formatter,
															@Nonnull Strings strings,
															@Assisted @Nonnull VisitType visitType) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(visitType);

		this.visitTypeId = visitType.getVisitTypeId();
		this.description = visitType.getDescription();
	}

	@Nonnull
	public VisitTypeId getVisitTypeId() {
		return this.visitTypeId;
	}

	@Nonnull
	public String getDescription() {
		return description;
	}
}