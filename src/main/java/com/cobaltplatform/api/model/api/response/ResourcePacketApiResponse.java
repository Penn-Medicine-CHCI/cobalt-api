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


import com.cobaltplatform.api.model.api.response.ResourcePacketCareResourceLocationApiResponse.ResourcePacketCareResourceLocationApiResponseFactory;
import com.cobaltplatform.api.model.db.ResourcePacket;
import com.cobaltplatform.api.service.CareResourceService;
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
public class ResourcePacketApiResponse {
	@Nullable
	private UUID resourcePacketId;
	@Nullable
	private Integer careResourceLocationCount;
	@Nullable
	private List<ResourcePacketCareResourceLocationApiResponse> careResourceLocations;
	@Nullable
	private ResourcePacketCareResourceLocationApiResponseFactory resourcePacketCareResourceLocationApiResponseFactory;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ResourcePacketApiResponseFactory {
		@Nonnull
		ResourcePacketApiResponse create(@Nonnull ResourcePacket resourcePacket);
	}

	@AssistedInject
	public ResourcePacketApiResponse(@Assisted @Nonnull ResourcePacket resourcePacket,
																	 @Nonnull ResourcePacketCareResourceLocationApiResponseFactory resourcePacketCareResourceLocationApiResponseFactory,
																	 @Nonnull CareResourceService careResourceService) {
		requireNonNull(resourcePacket);
		requireNonNull(resourcePacketCareResourceLocationApiResponseFactory);
		requireNonNull(careResourceService);

		this.resourcePacketId = resourcePacket.getResourcePacketId();
		this.careResourceLocations = careResourceService.findResourcePacketLocations(resourcePacket.getResourcePacketId())
				.stream().map(careResourceLocation -> resourcePacketCareResourceLocationApiResponseFactory.create(careResourceLocation)).collect(Collectors.toList());

	}

	@Nullable
	public UUID getResourcePacketId() {
		return resourcePacketId;
	}

	@Nullable
	public Integer getCareResourceLocationCount() {
		return careResourceLocationCount;
	}

	@Nullable
	public List<ResourcePacketCareResourceLocationApiResponse> getCareResourceLocations() {
		return careResourceLocations;
	}

	@Nullable
	public ResourcePacketCareResourceLocationApiResponseFactory getResourcePacketCareResourceLocationApiResponseFactory() {
		return resourcePacketCareResourceLocationApiResponseFactory;
	}
}