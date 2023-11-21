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

package com.cobaltplatform.api.service;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.model.api.request.CreatePresignedUploadRequest;
import com.cobaltplatform.api.model.service.PresignedUpload;
import com.cobaltplatform.api.util.UploadManager;
import com.cobaltplatform.api.util.ValidationException;
import com.cobaltplatform.api.util.ValidationException.FieldError;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.UUID;

import static com.soklet.util.StringUtils.trimToNull;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify LLC.
 */
@Singleton
@ThreadSafe
public class ImageUploadService {

	@Nonnull
	private final Configuration configuration;
	@Nonnull
	private final Provider<Strings> stringsProvider;
	@Nonnull
	private final Provider<UploadManager> uploadManagerProvider;

	@Inject
	public ImageUploadService(@Nonnull Configuration configuration,
														@Nonnull Provider<Strings> stringsProvider,
														@Nonnull Provider<UploadManager> uploadManagerProvider) {
		this.configuration = configuration;
		this.stringsProvider = stringsProvider;
		this.uploadManagerProvider = uploadManagerProvider;
	}

	@Nonnull
	public PresignedUpload generatePresignedUploadForGroupSession(@Nonnull CreatePresignedUploadRequest request) {
		requireNonNull(request);
		return generatePresignedUploadForUseCase(request, "group-sessions");
	}

	@Nonnull
	public PresignedUpload generatePresignedUploadForContent(@Nonnull CreatePresignedUploadRequest request) {
		requireNonNull(request);
		return generatePresignedUploadForUseCase(request, "content");
	}

	private PresignedUpload generatePresignedUploadForUseCase(@Nonnull CreatePresignedUploadRequest request,
																														@Nonnull String useCase) {
		UUID accountId = request.getAccountId();
		String filename = trimToNull(request.getFilename());
		String contentType = trimToNull(request.getContentType());

		ValidationException validationException = new ValidationException();

		if (accountId == null) {
			validationException.add(new FieldError("accountId", getStrings().get("Account ID is required.")));
		}

		if (filename == null) {
			validationException.add(new FieldError("filename", getStrings().get("Filename is required.")));
		}

		if (contentType == null) {
			validationException.add(new FieldError("contentType", getStrings().get("Content type is required.")));
		}

		if (validationException.hasErrors())
			throw validationException;

		String key = format("%s/%s/%s/%s", getConfiguration().getEnvironment(), useCase, UUID.randomUUID(), filename);

		return getUploadManager().createPresignedUpload(key, contentType, true, new HashMap<>() {{
			put("account-id", accountId.toString());
		}});
	}

	@Nonnull
	public Configuration getConfiguration() {
		return configuration;
	}

	@Nonnull
	public Strings getStrings() {
		return stringsProvider.get();
	}

	@Nonnull
	public UploadManager getUploadManager() {
		return uploadManagerProvider.get();
	}
}