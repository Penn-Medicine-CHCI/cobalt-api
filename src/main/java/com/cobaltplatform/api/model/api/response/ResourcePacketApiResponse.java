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


import com.cobaltplatform.api.model.api.response.CareResourceLocationApiResponse.CareResourceLocationApiResponseFactory;
import com.cobaltplatform.api.model.api.response.CareResourceTagApiResponse.CareResourceTagApiResponseFactory;
import com.cobaltplatform.api.model.db.CareResource;
import com.cobaltplatform.api.model.db.CareResourceTag.CareResourceTagGroupId;
import com.cobaltplatform.api.model.db.PatientOrderResourcePacket;
import com.cobaltplatform.api.service.CareResourceService;
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
public class ResourcePacketApiResponse {
	@Nullable
	private UUID patientOrderResourcePacketId;
	@Nullable
	private Integer careResourceLocationCount;
	@Nullable
	private List<CareResourceLocationApiResponse> careResourceLocations;
	@Nullable
	private CareResourceLocationApiResponseFactory careResourceLocationApiResponseFactory;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface ResourcePacketApiResponseFactory {
		@Nonnull
		ResourcePacketApiResponse create(@Nonnull PatientOrderResourcePacket patientOrderResourcePacket);
	}

	@AssistedInject
	public ResourcePacketApiResponse(@Nonnull PatientOrderResourcePacket patientOrderResourcePacket,
																	 @Nonnull CareResourceLocationApiResponseFactory careResourceLocationApiResponseFactory,
																	 @Nonnull CareResourceService careResourceService,
																	 @Nonnull Formatter formatter) {
		requireNonNull(patientOrderResourcePacket);
		requireNonNull(formatter);
		requireNonNull(careResourceLocationApiResponseFactory);
		requireNonNull(careResourceService);

		this.patientOrderResourcePacketId = patientOrderResourcePacket.getPatientOrderResourcePacketId();

		//this.careResourceLocations = careResourceService.findPatientOrderResourcePacketLocations(patientOrderResourcePacket.getPatientOrderResourcePacketId())
		//		.stream().map(careResourceLocation -> careResourceLocationApiResponseFactory.create(careResourceLocation, careResource)).collect(Collectors.toList());

	}

	@Nullable
	public UUID getPatientOrderResourcePacketId() {
		return patientOrderResourcePacketId;
	}

	@Nullable
	public Integer getCareResourceLocationCount() {
		return careResourceLocationCount;
	}

	@Nullable
	public List<CareResourceLocationApiResponse> getCareResourceLocations() {
		return careResourceLocations;
	}

	@Nullable
	public CareResourceLocationApiResponseFactory getCareResourceLocationApiResponseFactory() {
		return careResourceLocationApiResponseFactory;
	}
}