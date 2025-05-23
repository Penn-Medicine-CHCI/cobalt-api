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

import com.cobaltplatform.api.model.db.DisplayType.DisplayTypeId;
import com.cobaltplatform.api.model.db.Feature.FeatureId;
import com.cobaltplatform.api.model.db.NavigationHeader.NavigationHeaderId;
import com.cobaltplatform.api.model.db.SupportRole.SupportRoleId;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class FeatureForInstitution {
	@Nullable
	private FeatureId featureId;
	@Nullable
	private String urlName;
	@Nullable
	private String name;
	@Nullable
	private String subtitle;
	@Nullable
	private String description;
	@Nullable
	private String navDescription;
	@Nullable
	private Boolean navVisible;
	@Nullable
	private Boolean landingPageVisible;
	@Nullable
	private Boolean recommended;
	@Nullable
	private NavigationHeaderId navigationHeaderId;
	@Nullable
	private List<SupportRoleId> supportRoleIds;
	@Nullable
	private Boolean locationPromptRequired;
	@Nullable
	private String treatmentDescription;
	@Nullable
	private String bannerMessage;
	@Nullable
	private DisplayTypeId bannerMessageDisplayTypeId;
	@Nullable
	private String recommendationTitleOverride;
	@Nullable
	private String recommendationDescriptionOverride;
	@Nullable
	private String recommendationBookingTitleOverride;
	@Nullable
	private String recommendationBookingUrlOverride;

	@Nullable
	public FeatureId getFeatureId() {
		return featureId;
	}

	public void setFeatureId(@Nullable FeatureId featureId) {
		this.featureId = featureId;
	}

	@Nullable
	public String getUrlName() {
		return urlName;
	}

	public void setUrlName(@Nullable String urlName) {
		this.urlName = urlName;
	}

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nullable
	public String getSubtitle() {
		return this.subtitle;
	}

	public void setSubtitle(@Nullable String subtitle) {
		this.subtitle = subtitle;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	@Nullable
	public Boolean getRecommended() {
		return recommended;
	}

	public void setRecommended(@Nullable Boolean recommended) {
		this.recommended = recommended;
	}

	@Nullable
	public String getNavDescription() {
		return navDescription;
	}

	public void setNavDescription(@Nullable String navDescription) {
		this.navDescription = navDescription;
	}

	@Nullable
	public Boolean getNavVisible() {
		return this.navVisible;
	}

	public void setNavVisible(@Nullable Boolean navVisible) {
		this.navVisible = navVisible;
	}

	@Nullable
	public NavigationHeaderId getNavigationHeaderId() {
		return navigationHeaderId;
	}

	public void setNavigationHeaderId(@Nullable NavigationHeaderId navigationHeaderId) {
		this.navigationHeaderId = navigationHeaderId;
	}

	@Nullable
	public List<SupportRoleId> getSupportRoleIds() {
		return supportRoleIds;
	}

	public void setSupportRoleIds(@Nullable List<SupportRoleId> supportRoleIds) {
		this.supportRoleIds = supportRoleIds;
	}

	@Nullable
	public Boolean getLocationPromptRequired() {
		return locationPromptRequired;
	}

	public void setLocationPromptRequired(@Nullable Boolean locationPromptRequired) {
		this.locationPromptRequired = locationPromptRequired;
	}

	@Nullable
	public Boolean getLandingPageVisible() {
		return this.landingPageVisible;
	}

	public void setLandingPageVisible(@Nullable Boolean landingPageVisible) {
		this.landingPageVisible = landingPageVisible;
	}

	@Nullable
	public String getTreatmentDescription() {
		return this.treatmentDescription;
	}

	public void setTreatmentDescription(@Nullable String treatmentDescription) {
		this.treatmentDescription = treatmentDescription;
	}

	@Nullable
	public String getBannerMessage() {
		return this.bannerMessage;
	}

	public void setBannerMessage(@Nullable String bannerMessage) {
		this.bannerMessage = bannerMessage;
	}

	@Nullable
	public DisplayTypeId getBannerMessageDisplayTypeId() {
		return this.bannerMessageDisplayTypeId;
	}

	public void setBannerMessageDisplayTypeId(@Nullable DisplayTypeId bannerMessageDisplayTypeId) {
		this.bannerMessageDisplayTypeId = bannerMessageDisplayTypeId;
	}

	@Nullable
	public String getRecommendationTitleOverride() {
		return this.recommendationTitleOverride;
	}

	public void setRecommendationTitleOverride(@Nullable String recommendationTitleOverride) {
		this.recommendationTitleOverride = recommendationTitleOverride;
	}

	@Nullable
	public String getRecommendationDescriptionOverride() {
		return this.recommendationDescriptionOverride;
	}

	public void setRecommendationDescriptionOverride(@Nullable String recommendationDescriptionOverride) {
		this.recommendationDescriptionOverride = recommendationDescriptionOverride;
	}

	@Nullable
	public String getRecommendationBookingTitleOverride() {
		return this.recommendationBookingTitleOverride;
	}

	public void setRecommendationBookingTitleOverride(@Nullable String recommendationBookingTitleOverride) {
		this.recommendationBookingTitleOverride = recommendationBookingTitleOverride;
	}

	@Nullable
	public String getRecommendationBookingUrlOverride() {
		return this.recommendationBookingUrlOverride;
	}

	public void setRecommendationBookingUrlOverride(@Nullable String recommendationBookingUrlOverride) {
		this.recommendationBookingUrlOverride = recommendationBookingUrlOverride;
	}
}
