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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.ScreeningFlow;
import com.cobaltplatform.api.model.db.ScreeningFlowType.ScreeningFlowTypeId;
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
public class ScreeningFlowApiResponse {
	@Nonnull
	private final UUID screeningFlowId;
	@Nonnull
	private final InstitutionId institutionId;
	@Nonnull
	private final ScreeningFlowTypeId screeningFlowTypeId;
	@Nonnull
	private final UUID activeScreeningFlowVersionId;
	@Nonnull
	private final String name;
	@Nonnull
	private final UUID createdByAccountId;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ScreeningFlowApiResponseFactory {
		@Nonnull
		ScreeningFlowApiResponse create(@Nonnull ScreeningFlow screeningFlow);
	}

	@AssistedInject
	public ScreeningFlowApiResponse(@Nonnull Formatter formatter,
																	@Nonnull Strings strings,
																	@Assisted @Nonnull ScreeningFlow screeningFlow) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(screeningFlow);

		this.screeningFlowId = screeningFlow.getScreeningFlowId();
		this.institutionId = screeningFlow.getInstitutionId();
		this.screeningFlowTypeId = screeningFlow.getScreeningFlowTypeId();
		this.activeScreeningFlowVersionId = screeningFlow.getActiveScreeningFlowVersionId();
		this.name = screeningFlow.getName();
		this.createdByAccountId = screeningFlow.getCreatedByAccountId();
	}

	@Nonnull
	public UUID getScreeningFlowId() {
		return this.screeningFlowId;
	}

	@Nonnull
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	@Nonnull
	public ScreeningFlowTypeId getScreeningFlowTypeId() {
		return this.screeningFlowTypeId;
	}

	@Nonnull
	public UUID getActiveScreeningFlowVersionId() {
		return this.activeScreeningFlowVersionId;
	}

	@Nonnull
	public String getName() {
		return this.name;
	}

	@Nonnull
	public UUID getCreatedByAccountId() {
		return this.createdByAccountId;
	}
}