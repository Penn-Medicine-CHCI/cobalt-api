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

package com.cobaltplatform.api.model.db;

import com.cobaltplatform.api.model.db.Institution.InstitutionId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.Instant;
import java.util.UUID;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class InstitutionReferrer {
	@Nullable
	private UUID institutionReferrerId;
	@Nullable
	private InstitutionId fromInstitutionId;
	@Nullable
	private InstitutionId toInstitutionId;
	@Nullable
	private UUID intakeScreeningFlowId;
	@Nullable
	private String urlName;
	@Nullable
	private String title;
	@Nullable
	private String description; // Can include HTML
	@Nullable
	private String pageContent; // Content for the referrer page (e.g. markup with list of FAQs), should be HTML
	@Nullable
	private String ctaTitle; // CTA to be displayed on the institution referrer page
	@Nullable
	private String ctaDescription; // CTA to be displayed on the institution referrer page.  Can include HTML
	@Nullable
	private Instant created;
	@Nullable
	private Instant lastUpdated;

	@Nullable
	public UUID getInstitutionReferrerId() {
		return this.institutionReferrerId;
	}

	public void setInstitutionReferrerId(@Nullable UUID institutionReferrerId) {
		this.institutionReferrerId = institutionReferrerId;
	}

	@Nullable
	public InstitutionId getFromInstitutionId() {
		return this.fromInstitutionId;
	}

	public void setFromInstitutionId(@Nullable InstitutionId fromInstitutionId) {
		this.fromInstitutionId = fromInstitutionId;
	}

	@Nullable
	public InstitutionId getToInstitutionId() {
		return this.toInstitutionId;
	}

	public void setToInstitutionId(@Nullable InstitutionId toInstitutionId) {
		this.toInstitutionId = toInstitutionId;
	}

	@Nullable
	public UUID getIntakeScreeningFlowId() {
		return this.intakeScreeningFlowId;
	}

	public void setIntakeScreeningFlowId(@Nullable UUID intakeScreeningFlowId) {
		this.intakeScreeningFlowId = intakeScreeningFlowId;
	}

	@Nullable
	public String getUrlName() {
		return this.urlName;
	}

	public void setUrlName(@Nullable String urlName) {
		this.urlName = urlName;
	}

	@Nullable
	public String getTitle() {
		return this.title;
	}

	public void setTitle(@Nullable String title) {
		this.title = title;
	}

	@Nullable
	public String getDescription() {
		return this.description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public String getPageContent() {
		return this.pageContent;
	}

	public void setPageContent(@Nullable String pageContent) {
		this.pageContent = pageContent;
	}

	@Nullable
	public String getCtaTitle() {
		return this.ctaTitle;
	}

	public void setCtaTitle(@Nullable String ctaTitle) {
		this.ctaTitle = ctaTitle;
	}

	@Nullable
	public String getCtaDescription() {
		return this.ctaDescription;
	}

	public void setCtaDescription(@Nullable String ctaDescription) {
		this.ctaDescription = ctaDescription;
	}

	@Nullable
	public Instant getCreated() {
		return this.created;
	}

	public void setCreated(@Nullable Instant created) {
		this.created = created;
	}

	@Nullable
	public Instant getLastUpdated() {
		return this.lastUpdated;
	}

	public void setLastUpdated(@Nullable Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}