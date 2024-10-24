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


import com.cobaltplatform.api.model.db.CareResourceTag.CareResourceTagGroupId;
import com.cobaltplatform.api.model.db.CareResourceTag;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class CareResourceTagApiResponse {
	@Nullable
	private String careResourceTagId;
	@Nullable
	private String name;
	@Nullable
	private CareResourceTagGroupId careResourceTagGroupId;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface CareResourceTagApiResponseFactory {
		@Nonnull
		CareResourceTagApiResponse create(@Nonnull CareResourceTag careResourceTag);
	}

	@AssistedInject
	public CareResourceTagApiResponse(@Assisted @Nonnull CareResourceTag careResourceTag) {
		requireNonNull(careResourceTag);

		this.careResourceTagGroupId = careResourceTag.getCareResourceTagGroupId();
		this.careResourceTagId = careResourceTag.getCareResourceTagId();
		this.name = careResourceTag.getName();

	}

	@Nullable
	public String getCareResourceTagId() {
		return careResourceTagId;
	}

	@Nullable
	public String getName() {
		return name;
	}

	@Nullable
	public CareResourceTag.CareResourceTagGroupId getCareResourceTagGroupId() {
		return careResourceTagGroupId;
	}
}