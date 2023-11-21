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

import com.cobaltplatform.api.model.api.response.PresignedUploadApiResponse.PresignedUploadApiResponseFactory;
import com.cobaltplatform.api.model.service.FileUploadResult;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class FileUploadResultApiResponse {
	@Nonnull
	private final UUID fileUploadId;
	@Nonnull
	private final PresignedUploadApiResponse presignedUpload;

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface FileUploadResultApiResponseFactory {
		@Nonnull
		FileUploadResultApiResponse create(@Nonnull FileUploadResult fileUploadResult);
	}

	@AssistedInject
	public FileUploadResultApiResponse(@Nonnull PresignedUploadApiResponseFactory presignedUploadApiResponseFactory,
																		 @Assisted @Nonnull FileUploadResult fileUploadResult) {
		requireNonNull(presignedUploadApiResponseFactory);
		requireNonNull(fileUploadResult);

		this.fileUploadId = fileUploadResult.getFileUploadId();
		this.presignedUpload = presignedUploadApiResponseFactory.create(fileUploadResult.getPresignedUpload());
	}

	@Nonnull
	public UUID getFileUploadId() {
		return this.fileUploadId;
	}

	@Nonnull
	public PresignedUploadApiResponse getPresignedUpload() {
		return this.presignedUpload;
	}
}
