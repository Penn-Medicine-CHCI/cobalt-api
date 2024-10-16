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


import com.cobaltplatform.api.model.db.CareResource;
import com.cobaltplatform.api.model.db.CareResourceLocation;
import com.cobaltplatform.api.model.db.CareResourceSpecialty;
import com.cobaltplatform.api.model.db.Payor;
import com.cobaltplatform.api.model.db.SupportRole;
import com.cobaltplatform.api.service.CareResourceService;
import com.cobaltplatform.api.model.api.response.CareResourceLocationApiResponse.CareResourceLocationApiResponseFactory;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class CareResourceApiResponse {
	@Nullable
	private UUID careResourceId;
	@Nullable
	private String name;
	@Nullable
	private String notes;
	@Nullable
	private String phoneNumber;
	@Nullable
	private String formattedPhoneNumber;
	@Nullable
	private String websiteUrl;
	@Nullable
	private Boolean resourceAvailable;
	@Nullable
	private UUID createdByAccountId;
	@Nullable
	private List<CareResourceLocationApiResponse> careResourceLocations;
	@Nullable
	private List<Payor> payors;
	@Nullable
	private List<CareResourceSpecialty> specialties;
	@Nullable
	private List<SupportRole> supportRoles;
	@Nullable
	private CareResourceLocationApiResponseFactory careResourceLocationApiResponseFactory;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface CareResourceApiResponseFactory {
		@Nonnull
		CareResourceApiResponse create(@Nonnull CareResource careResource);
	}

	@AssistedInject
	public CareResourceApiResponse(@Nonnull CareResourceService careResourceService,
																 @Assisted @Nonnull CareResource careResource,
																 @Nonnull CareResourceLocationApiResponseFactory careResourceLocationApiResponseFactory,
																 @Nonnull Formatter formatter) {
		requireNonNull(careResource);
		requireNonNull(formatter);
		requireNonNull(careResourceLocationApiResponseFactory);

		this.careResourceId = careResource.getCareResourceId();
		this.name = careResource.getName();
		this.notes = careResource.getNotes();
		this.phoneNumber = careResource.getPhoneNumber();
		this.websiteUrl = careResource.getWebsiteUrl();
		this.resourceAvailable = careResource.getResourceAvailable();
		this.createdByAccountId = careResource.getCreatedByAccountId();
		this.formattedPhoneNumber = formatter.formatPhoneNumber(careResource.getPhoneNumber());
		this.careResourceLocations = careResourceService.findCareResourceLocations(careResource.getCareResourceId())
				.stream().map(careResourceLocation -> careResourceLocationApiResponseFactory.create(careResourceLocation)).collect(Collectors.toList());
		this.specialties = careResourceService.findCareResourceSpecialties(careResource.getCareResourceId());
		this.supportRoles = careResourceService.findCareResourceSupportRoles(careResource.getCareResourceId());
		this.payors = careResourceService.findCareResourcePayors(careResource.getCareResourceId());
	}


	@Nullable
	public UUID getCareResourceId() {
		return careResourceId;
	}

	@Nullable
	public List<CareResourceLocationApiResponse> getCareResourceLocations() {
		return careResourceLocations;
	}

	@Nullable
	public String getName() {
		return name;
	}

	@Nullable
	public String getNotes() {
		return notes;
	}

	@Nullable
	public String getPhoneNumber() {
		return phoneNumber;
	}

	@Nullable
	public String getWebsiteUrl() {
		return websiteUrl;
	}

	@Nullable
	public Boolean getResourceAvailable() {
		return resourceAvailable;
	}

	@Nullable
	public UUID getCreatedByAccountId() {
		return createdByAccountId;
	}

	@Nullable
	public String getFormattedPhoneNumber() {
		return formattedPhoneNumber;
	}

	@Nullable
	public List<Payor> getPayors() {
		return payors;
	}

	@Nullable
	public List<CareResourceSpecialty> getSpecialties() {
		return specialties;
	}

	@Nullable
	public List<SupportRole> getSupportRoles() {
		return supportRoles;
	}

	@Nullable
	public CareResourceLocationApiResponseFactory getCareResourceLocationApiResponseFactory() {
		return careResourceLocationApiResponseFactory;
	}
}