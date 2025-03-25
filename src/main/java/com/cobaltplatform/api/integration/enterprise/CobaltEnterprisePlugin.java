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

package com.cobaltplatform.api.integration.enterprise;

import com.cobaltplatform.api.Configuration;
import com.cobaltplatform.api.model.db.Content;
import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.Tag;
import com.cobaltplatform.api.service.ContentService;
import com.cobaltplatform.api.service.InstitutionService;
import com.cobaltplatform.api.util.AwsSecretManagerClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@Singleton
@ThreadSafe
public class CobaltEnterprisePlugin extends DefaultEnterprisePlugin {
	@Nonnull
	private final ContentService contentService;

	@Inject
	public CobaltEnterprisePlugin(@Nonnull InstitutionService institutionService,
																@Nonnull AwsSecretManagerClient awsSecretManagerClient,
																@Nonnull ContentService contentService,
																@Nonnull Configuration configuration) {
		super(institutionService, awsSecretManagerClient, configuration);
		this.contentService = contentService;
	}

	@Nonnull
	@Override
	public InstitutionId getInstitutionId() {
		return InstitutionId.COBALT;
	}

	@Nonnull
	@Override
	public List<Content> recommendedContentForAccountId(@Nullable UUID accountId) {
		if (accountId == null)
			return Collections.emptyList();

		// Naive implementation for our COBALT institution - return all the content
		return getContentService().findVisibleContentByAccountId(accountId);
	}

	@Nonnull
	protected ContentService getContentService() {
		return this.contentService;
	}

	@Override
	public Tag applyCustomizationsToTag(@Nonnull Tag tag) {
		requireNonNull(tag);

		// Show how we might override tag display
		if ("Physical Health".equals(tag.getName())) {
			tag.setName("Example Tag Override");
			tag.setDescription("Example Tag description override");
		}

		return tag;
	}
}
