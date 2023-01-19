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

import com.cobaltplatform.api.context.CurrentContext;
import com.cobaltplatform.api.model.db.GroupTopic;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class GroupTopicApiResponse {
	@Nonnull
	private final UUID groupTopicId;
	@Nonnull
	private final String name;
	@Nonnull
	private final String description;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface GroupTopicApiResponseFactory {
		@Nonnull
		GroupTopicApiResponse create(@Nonnull GroupTopic groupTopic);
	}

	@AssistedInject
	public GroupTopicApiResponse(@Nonnull Provider<CurrentContext> currentContextProvider,
															 @Assisted @Nonnull GroupTopic groupTopic) {
		requireNonNull(currentContextProvider);
		requireNonNull(groupTopic);

		this.groupTopicId = groupTopic.getGroupTopicId();
		this.name = groupTopic.getName();
		this.description = groupTopic.getDescription();
	}

	@Nonnull
	public UUID getGroupTopicId() {
		return this.groupTopicId;
	}

	@Nonnull
	public String getName() {
		return this.name;
	}

	@Nonnull
	public String getDescription() {
		return this.description;
	}
}