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

import com.cobaltplatform.api.model.db.InstitutionFeatureInstitutionReferrer;
import com.cobaltplatform.api.util.Formatter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.lokalized.Strings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * @author Transmogrify, LLC.
 */
@ThreadSafe
public class InstitutionFeatureInstitutionReferrerApiResponse {
	@Nonnull
	private final UUID institutionFeatureInstitutionReferrerId;
	@Nonnull
	private final UUID institutionFeatureId;
	@Nonnull
	private final UUID institutionReferrerId;
	@Nullable
	private final String ctaTitle; // CTA to be displayed on the feature page (if applicable)
	@Nullable
	private final String ctaDescription; // CTA to be displayed on the institution feature page (if applicable).  Can include HTML

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface InstitutionFeatureInstitutionReferrerApiResponseFactory {
		@Nonnull
		InstitutionFeatureInstitutionReferrerApiResponse create(@Nonnull InstitutionFeatureInstitutionReferrer institutionFeatureInstitutionReferrer);
	}

	@AssistedInject
	public InstitutionFeatureInstitutionReferrerApiResponse(@Nonnull Formatter formatter,
																													@Nonnull Strings strings,
																													@Assisted @Nonnull InstitutionFeatureInstitutionReferrer institutionFeatureInstitutionReferrer) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(institutionFeatureInstitutionReferrer);

		this.institutionFeatureInstitutionReferrerId = institutionFeatureInstitutionReferrer.getInstitutionFeatureInstitutionReferrerId();
		this.institutionFeatureId = institutionFeatureInstitutionReferrer.getInstitutionFeatureId();
		this.institutionReferrerId = institutionFeatureInstitutionReferrer.getInstitutionReferrerId();
		this.ctaTitle = institutionFeatureInstitutionReferrer.getCtaTitle();
		this.ctaDescription = institutionFeatureInstitutionReferrer.getCtaDescription();
	}

	@Nonnull
	public UUID getInstitutionFeatureInstitutionReferrerId() {
		return this.institutionFeatureInstitutionReferrerId;
	}

	@Nonnull
	public UUID getInstitutionFeatureId() {
		return this.institutionFeatureId;
	}

	@Nonnull
	public UUID getInstitutionReferrerId() {
		return this.institutionReferrerId;
	}

	@Nonnull
	public Optional<String> getCtaTitle() {
		return Optional.ofNullable(this.ctaTitle);
	}

	@Nonnull
	public Optional<String> getCtaDescription() {
		return Optional.ofNullable(this.ctaDescription);
	}
}