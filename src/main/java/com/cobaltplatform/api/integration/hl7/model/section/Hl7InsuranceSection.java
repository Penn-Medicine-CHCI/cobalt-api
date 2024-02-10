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

package com.cobaltplatform.api.integration.hl7.model.section;

import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7InsuranceAdditionalInformationCertificationSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7InsuranceAdditionalInformationSegment;
import com.cobaltplatform.api.integration.hl7.model.segment.Hl7InsuranceSegment;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/TriggerEvents/ORM_O01
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7InsuranceSection extends Hl7Object {
	@Nullable
	private Hl7InsuranceSegment insurance;
	@Nullable
	private Hl7InsuranceAdditionalInformationSegment insuranceAdditionalInformation;
	@Nullable
	private Hl7InsuranceAdditionalInformationCertificationSegment insuranceAdditionalInformationCertification;

	@Nullable
	public Hl7InsuranceSegment getInsurance() {
		return this.insurance;
	}

	public void setInsurance(@Nullable Hl7InsuranceSegment insurance) {
		this.insurance = insurance;
	}

	@Nullable
	public Hl7InsuranceAdditionalInformationSegment getInsuranceAdditionalInformation() {
		return this.insuranceAdditionalInformation;
	}

	public void setInsuranceAdditionalInformation(@Nullable Hl7InsuranceAdditionalInformationSegment insuranceAdditionalInformation) {
		this.insuranceAdditionalInformation = insuranceAdditionalInformation;
	}

	@Nullable
	public Hl7InsuranceAdditionalInformationCertificationSegment getInsuranceAdditionalInformationCertification() {
		return this.insuranceAdditionalInformationCertification;
	}

	public void setInsuranceAdditionalInformationCertification(@Nullable Hl7InsuranceAdditionalInformationCertificationSegment insuranceAdditionalInformationCertification) {
		this.insuranceAdditionalInformationCertification = insuranceAdditionalInformationCertification;
	}
}