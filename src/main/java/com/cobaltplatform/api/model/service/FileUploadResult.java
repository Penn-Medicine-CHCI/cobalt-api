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

package com.cobaltplatform.api.model.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.UUID;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Immutable
public class FileUploadResult {
	@Nonnull
	private final UUID fileUploadId;
	@Nullable
	private final PresignedUpload presignedUpload;

	public FileUploadResult(@Nonnull UUID fileUploadId,
													@Nonnull PresignedUpload presignedUpload) {
		requireNonNull(fileUploadId);
		requireNonNull(presignedUpload);

		this.fileUploadId = fileUploadId;
		this.presignedUpload = presignedUpload;
	}

	@Override
	public String toString() {
		return format("%s{fileUploadId=%s, presignedUpload=%s}", getClass().getSimpleName(), getFileUploadId(), getPresignedUpload());
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;

		if (other == null || !getClass().equals(other.getClass()))
			return false;

		FileUploadResult otherFileUploadResult = (FileUploadResult) other;

		return Objects.equals(this.getFileUploadId(), otherFileUploadResult.getFileUploadId())
				&& Objects.equals(this.getPresignedUpload(), otherFileUploadResult.getPresignedUpload());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getFileUploadId(), getPresignedUpload());
	}

	@Nonnull
	public UUID getFileUploadId() {
		return this.fileUploadId;
	}

	@Nullable
	public PresignedUpload getPresignedUpload() {
		return this.presignedUpload;
	}
}
