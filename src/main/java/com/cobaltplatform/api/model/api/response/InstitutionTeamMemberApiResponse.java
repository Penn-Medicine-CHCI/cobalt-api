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
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionTeamMember;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Provider;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class InstitutionTeamMemberApiResponse {
	@Nonnull
	private final UUID institutionTeamMemberId;
	@Nonnull
	private final InstitutionId institutionId;
	@Nonnull
	private final String title;
	@Nonnull
	private final String name;
	@Nonnull
	private final String imageUrl;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface InstitutionTeamMemberApiResponseFactory {
		@Nonnull
		InstitutionTeamMemberApiResponse create(@Nonnull InstitutionTeamMember institutionTeamMember);
	}

	@AssistedInject
	public InstitutionTeamMemberApiResponse(@Nonnull Formatter formatter,
																					@Nonnull Strings strings,
																					@Nonnull Provider<CurrentContext> currentContextProvider,
																					@Assisted @Nonnull InstitutionTeamMember institutionTeamMember) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(currentContextProvider);
		requireNonNull(institutionTeamMember);

		this.institutionTeamMemberId = institutionTeamMember.getInstitutionTeamMemberId();
		this.institutionId = institutionTeamMember.getInstitutionId();
		this.title = institutionTeamMember.getTitle();
		this.name = institutionTeamMember.getName();
		this.imageUrl = institutionTeamMember.getImageUrl();
	}

	@Nonnull
	public UUID getInstitutionTeamMemberId() {
		return this.institutionTeamMemberId;
	}

	@Nonnull
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	@Nonnull
	public String getTitle() {
		return this.title;
	}

	@Nonnull
	public String getName() {
		return this.name;
	}

	@Nonnull
	public String getImageUrl() {
		return this.imageUrl;
	}
}