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

package com.cobaltplatform.api.integration.hl7.model.segment;

import ca.uhn.hl7v2.model.v251.segment.DG1;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7CodedElement;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7CompositePrice;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7EntityIdentifier;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedCompositeIdNumberAndNameForPersons;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7TimeStamp;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.soklet.util.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/Segments/DG1
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7DiagnosisSegment extends Hl7Object {
	@Nullable
	private Integer setId; // DG1.1 - Set ID
	@Nullable
	private String diagnosisCodingMethod; // DG1.2 - Diagnosis Coding Method
	@Nullable
	private Hl7CodedElement diagnosisCode; // DG1.3 - Diagnosis Code
	@Nullable
	private String diagnosisDescription; // DG1.4 - Diagnosis Description
	@Nullable
	private Hl7TimeStamp diagnosisDateTime; // DG1.5 - Diagnosis Date/Time
	@Nullable
	private String diagnosisType; // DG1.6 - Diagnosis Type
	@Nullable
	private Hl7CodedElement majorDiagnosticCategory; // DG1.7 - Major Diagnostic Category
	@Nullable
	private Hl7CodedElement diagnosticRelatedGroup; // DG1.8 - Diagnostic Related Group
	@Nullable
	private String drgApprovalIndicator; // DG1.9 - DRG Approval Indicator
	@Nullable
	private String drgGrouperReviewCode; // DG1.10 - DRG Grouper Review Code
	@Nullable
	private Hl7CodedElement outlierType; // DG1.11 - Outlier Type
	@Nullable
	private Double outlierDays; // DG1.12 - Outlier Days
	@Nullable
	private Hl7CompositePrice outlierCost; // DG1.13 - Outlier Cost
	@Nullable
	private String grouperVersionAndType; // DG1.14 - Grouper Version And Type
	@Nullable
	private String diagnosisPriority; // DG1.15 - Diagnosis Priority
	@Nullable
	private List<Hl7ExtendedCompositeIdNumberAndNameForPersons> diagnosingClinician; // DG1.16 - Diagnosing Clinician
	@Nullable
	private String diagnosisClassification; // DG1.17 - Diagnosis Classification
	@Nullable
	private String confidentialIndicator; // DG1.18 - Confidential Indicator
	@Nullable
	private Hl7TimeStamp attestationDateTime; // DG1.19 - Attestation Date/Time
	@Nullable
	private Hl7EntityIdentifier diagnosisIdentifier; // DG1.20 - Diagnosis Identifier
	@Nullable
	private String diagnosisActionCode; // DG1.21 - Diagnosis Action Code

	@Nonnull
	public static Boolean isPresent(@Nullable DG1 dg1) {
		if (dg1 == null)
			return false;

		return trimToNull(dg1.getSetIDDG1().getValue()) != null;
	}

	public Hl7DiagnosisSegment() {
		// Nothing to do
	}

	public Hl7DiagnosisSegment(@Nullable DG1 dg1) {
		if (dg1 != null) {
			String setIdAsString = trimToNull(dg1.getSetIDDG1().getValue());
			if (setIdAsString != null)
				this.setId = Integer.parseInt(setIdAsString, 10);

			this.diagnosisCodingMethod = trimToNull(dg1.getDiagnosisCodingMethod().getValueOrEmpty());

			if (Hl7CodedElement.isPresent(dg1.getDiagnosisCodeDG1()))
				this.diagnosisCode = new Hl7CodedElement(dg1.getDiagnosisCodeDG1());

			this.diagnosisDescription = trimToNull(dg1.getDiagnosisDescription().getValueOrEmpty());

			if (Hl7TimeStamp.isPresent(dg1.getDiagnosisDateTime()))
				this.diagnosisDateTime = new Hl7TimeStamp(dg1.getDiagnosisDateTime());

			this.diagnosisType = trimToNull(dg1.getDiagnosisType().getValueOrEmpty());

			if (Hl7CodedElement.isPresent(dg1.getMajorDiagnosticCategory()))
				this.majorDiagnosticCategory = new Hl7CodedElement(dg1.getMajorDiagnosticCategory());

			if (Hl7CodedElement.isPresent(dg1.getDiagnosticRelatedGroup()))
				this.diagnosticRelatedGroup = new Hl7CodedElement(dg1.getDiagnosticRelatedGroup());

			this.drgApprovalIndicator = trimToNull(dg1.getDRGApprovalIndicator().getValueOrEmpty());
			this.drgGrouperReviewCode = trimToNull(dg1.getDRGGrouperReviewCode().getValueOrEmpty());

			if (Hl7CodedElement.isPresent(dg1.getOutlierType()))
				this.outlierType = new Hl7CodedElement(dg1.getOutlierType());

			String outlierDaysAsString = trimToNull(dg1.getOutlierDays().getValue());
			if (outlierDaysAsString != null)
				this.outlierDays = Double.parseDouble(outlierDaysAsString);

			if (Hl7CompositePrice.isPresent(dg1.getOutlierCost()))
				this.outlierCost = new Hl7CompositePrice(dg1.getOutlierCost());

			this.grouperVersionAndType = trimToNull(dg1.getGrouperVersionAndType().getValueOrEmpty());
			this.diagnosisPriority = trimToNull(dg1.getDiagnosisPriority().getValueOrEmpty());

			if (dg1.getDiagnosingClinician() != null && dg1.getDiagnosingClinician().length > 0)
				this.diagnosingClinician = Arrays.stream(dg1.getDiagnosingClinician())
						.map(xcn -> Hl7ExtendedCompositeIdNumberAndNameForPersons.isPresent(xcn) ? new Hl7ExtendedCompositeIdNumberAndNameForPersons(xcn) : null)
						.filter(diagnosingClinician -> diagnosingClinician != null)
						.collect(Collectors.toList());

			this.diagnosisClassification = trimToNull(dg1.getDiagnosisClassification().getValueOrEmpty());
			this.confidentialIndicator = trimToNull(dg1.getConfidentialIndicator().getValueOrEmpty());

			if (Hl7TimeStamp.isPresent(dg1.getAttestationDateTime()))
				this.attestationDateTime = new Hl7TimeStamp(dg1.getAttestationDateTime());

			if (Hl7EntityIdentifier.isPresent(dg1.getDiagnosisIdentifier()))
				this.diagnosisIdentifier = new Hl7EntityIdentifier(dg1.getDiagnosisIdentifier());

			this.diagnosisActionCode = trimToNull(dg1.getDiagnosisActionCode().getValueOrEmpty());
		}
	}

	@Nullable
	public Integer getSetId() {
		return this.setId;
	}

	public void setSetId(@Nullable Integer setId) {
		this.setId = setId;
	}

	@Nullable
	public String getDiagnosisCodingMethod() {
		return this.diagnosisCodingMethod;
	}

	public void setDiagnosisCodingMethod(@Nullable String diagnosisCodingMethod) {
		this.diagnosisCodingMethod = diagnosisCodingMethod;
	}

	@Nullable
	public Hl7CodedElement getDiagnosisCode() {
		return this.diagnosisCode;
	}

	public void setDiagnosisCode(@Nullable Hl7CodedElement diagnosisCode) {
		this.diagnosisCode = diagnosisCode;
	}

	@Nullable
	public String getDiagnosisDescription() {
		return this.diagnosisDescription;
	}

	public void setDiagnosisDescription(@Nullable String diagnosisDescription) {
		this.diagnosisDescription = diagnosisDescription;
	}

	@Nullable
	public Hl7TimeStamp getDiagnosisDateTime() {
		return this.diagnosisDateTime;
	}

	public void setDiagnosisDateTime(@Nullable Hl7TimeStamp diagnosisDateTime) {
		this.diagnosisDateTime = diagnosisDateTime;
	}

	@Nullable
	public String getDiagnosisType() {
		return this.diagnosisType;
	}

	public void setDiagnosisType(@Nullable String diagnosisType) {
		this.diagnosisType = diagnosisType;
	}

	@Nullable
	public Hl7CodedElement getMajorDiagnosticCategory() {
		return this.majorDiagnosticCategory;
	}

	public void setMajorDiagnosticCategory(@Nullable Hl7CodedElement majorDiagnosticCategory) {
		this.majorDiagnosticCategory = majorDiagnosticCategory;
	}

	@Nullable
	public Hl7CodedElement getDiagnosticRelatedGroup() {
		return this.diagnosticRelatedGroup;
	}

	public void setDiagnosticRelatedGroup(@Nullable Hl7CodedElement diagnosticRelatedGroup) {
		this.diagnosticRelatedGroup = diagnosticRelatedGroup;
	}

	@Nullable
	public String getDrgApprovalIndicator() {
		return this.drgApprovalIndicator;
	}

	public void setDrgApprovalIndicator(@Nullable String drgApprovalIndicator) {
		this.drgApprovalIndicator = drgApprovalIndicator;
	}

	@Nullable
	public String getDrgGrouperReviewCode() {
		return this.drgGrouperReviewCode;
	}

	public void setDrgGrouperReviewCode(@Nullable String drgGrouperReviewCode) {
		this.drgGrouperReviewCode = drgGrouperReviewCode;
	}

	@Nullable
	public Hl7CodedElement getOutlierType() {
		return this.outlierType;
	}

	public void setOutlierType(@Nullable Hl7CodedElement outlierType) {
		this.outlierType = outlierType;
	}

	@Nullable
	public Double getOutlierDays() {
		return this.outlierDays;
	}

	public void setOutlierDays(@Nullable Double outlierDays) {
		this.outlierDays = outlierDays;
	}

	@Nullable
	public Hl7CompositePrice getOutlierCost() {
		return this.outlierCost;
	}

	public void setOutlierCost(@Nullable Hl7CompositePrice outlierCost) {
		this.outlierCost = outlierCost;
	}

	@Nullable
	public String getGrouperVersionAndType() {
		return this.grouperVersionAndType;
	}

	public void setGrouperVersionAndType(@Nullable String grouperVersionAndType) {
		this.grouperVersionAndType = grouperVersionAndType;
	}

	@Nullable
	public String getDiagnosisPriority() {
		return this.diagnosisPriority;
	}

	public void setDiagnosisPriority(@Nullable String diagnosisPriority) {
		this.diagnosisPriority = diagnosisPriority;
	}

	@Nullable
	public List<Hl7ExtendedCompositeIdNumberAndNameForPersons> getDiagnosingClinician() {
		return this.diagnosingClinician;
	}

	public void setDiagnosingClinician(@Nullable List<Hl7ExtendedCompositeIdNumberAndNameForPersons> diagnosingClinician) {
		this.diagnosingClinician = diagnosingClinician;
	}

	@Nullable
	public String getDiagnosisClassification() {
		return this.diagnosisClassification;
	}

	public void setDiagnosisClassification(@Nullable String diagnosisClassification) {
		this.diagnosisClassification = diagnosisClassification;
	}

	@Nullable
	public String getConfidentialIndicator() {
		return this.confidentialIndicator;
	}

	public void setConfidentialIndicator(@Nullable String confidentialIndicator) {
		this.confidentialIndicator = confidentialIndicator;
	}

	@Nullable
	public Hl7TimeStamp getAttestationDateTime() {
		return this.attestationDateTime;
	}

	public void setAttestationDateTime(@Nullable Hl7TimeStamp attestationDateTime) {
		this.attestationDateTime = attestationDateTime;
	}

	@Nullable
	public Hl7EntityIdentifier getDiagnosisIdentifier() {
		return this.diagnosisIdentifier;
	}

	public void setDiagnosisIdentifier(@Nullable Hl7EntityIdentifier diagnosisIdentifier) {
		this.diagnosisIdentifier = diagnosisIdentifier;
	}

	@Nullable
	public String getDiagnosisActionCode() {
		return this.diagnosisActionCode;
	}

	public void setDiagnosisActionCode(@Nullable String diagnosisActionCode) {
		this.diagnosisActionCode = diagnosisActionCode;
	}
}