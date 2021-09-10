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

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;
import com.cobaltplatform.api.model.db.SupportRole;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;
import com.cobaltplatform.api.util.Formatter;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class SupportRoleApiResponse {
	@Nonnull
	private final SupportRoleId supportRoleId;
	@Nonnull
	private final String description;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface SupportRoleApiResponseFactory {
		@Nonnull
		SupportRoleApiResponse create(@Nonnull SupportRole supportRole);
	}

	@AssistedInject
	public SupportRoleApiResponse(@Nonnull Formatter formatter,
																@Nonnull Strings strings,
																@Assisted @Nonnull SupportRole supportRole) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(supportRole);

		this.supportRoleId = supportRole.getSupportRoleId();
		this.description = supportRole.getDescription();
	}

	@Nonnull
	public SupportRoleId getSupportRoleId() {
		return supportRoleId;
	}

	@Nonnull
	public String getDescription() {
		return description;
	}
}