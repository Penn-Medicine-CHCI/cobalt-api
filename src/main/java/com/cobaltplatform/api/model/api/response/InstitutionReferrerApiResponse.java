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

import com.cobaltplatform.api.model.db.Institution.InstitutionId;
import com.cobaltplatform.api.model.db.InstitutionReferrer;
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
public class InstitutionReferrerApiResponse {
	@Nonnull
	private final UUID institutionReferrerId;
	@Nonnull
	private final InstitutionId fromInstitutionId;
	@Nonnull
	private final InstitutionId toInstitutionId;
	@Nullable
	private final UUID intakeScreeningFlowId;
	@Nonnull
	private final String urlName;
	@Nonnull
	private final String title;
	@Nonnull
	private final String description; // Can include HTML
	@Nonnull
	private final String pageContent; // Content for the referrer page (e.g. markup with list of FAQs), should be HTML
	@Nullable
	private final String ctaTitle; // CTA to be displayed on the institution referrer page
	@Nullable
	private final String ctaDescription; // CTA to be displayed on the institution referrer page.  Can include HTML

	// Note: requires FactoryModuleBuilder entry in AppModule
	@ThreadSafe
	public interface InstitutionReferrerApiResponseFactory {
		@Nonnull
		InstitutionReferrerApiResponse create(@Nonnull InstitutionReferrer institutionReferrer);
	}

	@AssistedInject
	public InstitutionReferrerApiResponse(@Nonnull Formatter formatter,
																				@Nonnull Strings strings,
																				@Assisted @Nonnull InstitutionReferrer institutionReferrer) {
		requireNonNull(formatter);
		requireNonNull(strings);
		requireNonNull(institutionReferrer);

		this.institutionReferrerId = institutionReferrer.getInstitutionReferrerId();
		this.fromInstitutionId = institutionReferrer.getFromInstitutionId();
		this.toInstitutionId = institutionReferrer.getToInstitutionId();
		this.intakeScreeningFlowId = institutionReferrer.getIntakeScreeningFlowId();
		this.urlName = institutionReferrer.getUrlName();
		this.title = institutionReferrer.getTitle();
		this.description = institutionReferrer.getDescription();
		this.pageContent = institutionReferrer.getPageContent();
		this.ctaTitle = institutionReferrer.getCtaTitle();
		this.ctaDescription = institutionReferrer.getCtaDescription();
	}

	@Nonnull
	public UUID getInstitutionReferrerId() {
		return this.institutionReferrerId;
	}

	@Nonnull
	public InstitutionId getFromInstitutionId() {
		return this.fromInstitutionId;
	}

	@Nonnull
	public InstitutionId getToInstitutionId() {
		return this.toInstitutionId;
	}

	@Nonnull
	public Optional<UUID> getIntakeScreeningFlowId() {
		return Optional.ofNullable(this.intakeScreeningFlowId);
	}

	@Nonnull
	public String getUrlName() {
		return this.urlName;
	}

	@Nonnull
	public String getTitle() {
		return this.title;
	}

	@Nonnull
	public String getDescription() {
		return this.description;
	}

	@Nonnull
	public String getPageContent() {
		return this.pageContent;
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