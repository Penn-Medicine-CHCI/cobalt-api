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
import com.cobaltplatform.api.model.api.response.InstitutionTeamMemberApiResponse.InstitutionTeamMemberApiResponseFactory;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionBlurb;
import com.cobaltplatform.api.model.db.InstitutionBlurbType.InstitutionBlurbTypeId;
import com.cobaltplatform.api.model.db.InstitutionTeamMember;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Provider;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class InstitutionBlurbApiResponse {
	@Nonnull
	private final UUID institutionBlurbId;
	@Nonnull
	private final InstitutionId institutionId;
	@Nonnull
	private final InstitutionBlurbTypeId institutionBlurbTypeId;
	@Nullable
	private final String title;
	@Nullable
	private final String description;
	@Nullable
	private final String shortDescription;
	@Nonnull
	private final List<InstitutionTeamMemberApiResponse> institutionTeamMembers;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface InstitutionBlurbApiResponseFactory {
		@Nonnull
		InstitutionBlurbApiResponse create(@Nonnull InstitutionBlurb institutionBlurb,
																			 @Nonnull List<InstitutionTeamMember> institutionTeamMembers);
	}

	@AssistedInject
	public InstitutionBlurbApiResponse(@Nonnull Formatter formatter,
																		 @Nonnull Strings strings,
																		 @Nonnull InstitutionTeamMemberApiResponseFactory institutionTeamMemberApiResponseFactory,
																		 @Nonnull Provider<CurrentContext> currentContextProvider,
																		 @Assisted @Nonnull InstitutionBlurb institutionBlurb,
																		 @Assisted @Nonnull List<InstitutionTeamMember> institutionTeamMembers) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(institutionTeamMemberApiResponseFactory);
		requireNonNull(currentContextProvider);
		requireNonNull(institutionBlurb);
		requireNonNull(institutionTeamMembers);

		this.institutionBlurbId = institutionBlurb.getInstitutionBlurbId();
		this.institutionId = institutionBlurb.getInstitutionId();
		this.institutionBlurbTypeId = institutionBlurb.getInstitutionBlurbTypeId();
		this.title = institutionBlurb.getTitle();
		this.description = institutionBlurb.getDescription();
		this.shortDescription = institutionBlurb.getShortDescription();
		this.institutionTeamMembers = institutionTeamMembers.stream()
				.map(institutionTeamMember -> institutionTeamMemberApiResponseFactory.create(institutionTeamMember))
				.collect(Collectors.toList());
	}

	@Nonnull
	public UUID getInstitutionBlurbId() {
		return this.institutionBlurbId;
	}

	@Nonnull
	public InstitutionId getInstitutionId() {
		return this.institutionId;
	}

	@Nonnull
	public InstitutionBlurbTypeId getInstitutionBlurbTypeId() {
		return this.institutionBlurbTypeId;
	}

	@Nullable
	public String getTitle() {
		return this.title;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	@Nullable
	public String getShortDescription() {
		return this.shortDescription;
	}

	@Nonnull
	public List<InstitutionTeamMemberApiResponse> getInstitutionTeamMembers() {
		return this.institutionTeamMembers;
	}
}