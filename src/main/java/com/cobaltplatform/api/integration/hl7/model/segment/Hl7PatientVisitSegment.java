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

import ca.uhn.hl7v2.model.v251.segment.PV1;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedCompositeIdNumberAndNameForPersons;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedCompositeIdWithCheckDigit;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7FinancialClass;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7PersonLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/Segments/PV1
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7PatientVisitSegment extends Hl7Object {
	@Nullable
	private Integer setId; //  PV1.1 - Set ID - PV1
	@Nullable
	private String patientClass; // PV1.2 - Patient Class
	@Nullable
	private Hl7PersonLocation assignedPatientLocation; // PV1.3 - Assigned Patient Location
	@Nullable
	private String admissionType; // PV1.4 - Admission Type
	@Nullable
	private Hl7ExtendedCompositeIdWithCheckDigit preadmitNumber; // PV1.5 - Preadmit Number
	@Nullable
	private Hl7PersonLocation priorPatientLocation; // PV1.6 - Prior Patient Location
	@Nullable
	private List<Hl7ExtendedCompositeIdNumberAndNameForPersons> attendingDoctor; // PV1.7 - Attending Doctor
	@Nullable
	private List<Hl7ExtendedCompositeIdNumberAndNameForPersons> referringDoctor; // PV1.8 - Referring Doctor
	@Nullable
	private List<Hl7ExtendedCompositeIdNumberAndNameForPersons> consultingDoctor; // PV1.9 - Consulting Doctor
	@Nullable
	private String hospitalService; // PV1.10 - Hospital Service
	@Nullable
	private Hl7PersonLocation temporaryLocation; // PV1.11 - Temporary Location
	@Nullable
	private String preadmitTestIndicator; // PV1.12 - Preadmit Test Indicator
	@Nullable
	private String readmissionIndicator; // PV1.13 - Re-admission Indicator
	@Nullable
	private String admitSource; // PV1.14 - Admit Source
	@Nullable
	private List<String> ambulatoryStatus; // PV1.15 - Ambulatory Status
	@Nullable
	private String vipIndicator; // PV1.16 - VIP Indicator
	@Nullable
	private List<Hl7ExtendedCompositeIdNumberAndNameForPersons> admittingDoctor; // PV1.17 - Admitting Doctor
	@Nullable
	private String patientType; // PV1.18 - Patient Type
	@Nullable
	private Hl7ExtendedCompositeIdWithCheckDigit visitNumber; // PV1.19 - Visit Number
	@Nullable
	private List<Hl7FinancialClass> financialClass; // PV1.20 - Financial Class
	@Nullable
	private String chargePriceIndicator; // PV1.21 - Charge Price Indicator
	@Nullable
	private String courtesyCode; // PV1.22 - Courtesy Code
	@Nullable
	private String creditRating; // PV1.23 - Credit Rating
	@Nullable
	private List<String> contractCode; // PV1.24 - Contract Code
	@Nullable
	private List<String> contractEffectiveDate; // PV1.25 - Contract Effective Date
	@Nullable
	private List<Double> contractAmount; // PV1.26 - Contract Amount
	@Nullable
	private List<Double> contractPeriod; // PV1.27 - Contract Period
	@Nullable
	private String interestCode; // PV1.28 - Interest Code
	@Nullable
	private String transferToBadDebtCode; // PV1.29 - Transfer to Bad Debt Code
	@Nullable
	private String transferToBadDebtDate; // PV1.30 - Transfer to Bad Debt Date
	@Nullable
	private String badDebtAgencyCode; // PV1.31 - Bad Debt Agency Code;
	@Nullable
	private Double badDebtTransferAmount; // PV1.32 - Bad Debt Transfer Amount
	@Nullable
	private Double badDebtRecoveryAmount; // PV1.33 - Bad Debt Recovery Amount
	@Nullable
	private String deleteAccountIndicator; // PV1.34 - Delete Account Indicator
	@Nullable
	private String deleteAccountDate; // PV1.35 - Delete Account Date
	@Nullable
	private String dischargeDisposition; // PV1.36 - Discharge Disposition

	@Nonnull
	public static Boolean isPresent(@Nullable PV1 pv1) {
		if (pv1 == null)
			return false;

		return trimToNull(pv1.getSetIDPV1().getValue()) != null
				|| trimToNull(pv1.getPatientClass().getValueOrEmpty()) != null
				|| Hl7PersonLocation.isPresent(pv1.getAssignedPatientLocation())
				|| trimToNull(pv1.getAdmissionType().getValueOrEmpty()) != null
				|| Hl7ExtendedCompositeIdWithCheckDigit.isPresent(pv1.getPreadmitNumber())
				|| Hl7PersonLocation.isPresent(pv1.getPriorPatientLocation())
				|| (pv1.getAttendingDoctor() != null && pv1.getAttendingDoctor().length > 0)
				|| (pv1.getReferringDoctor() != null && pv1.getReferringDoctor().length > 0)
				|| (pv1.getConsultingDoctor() != null && pv1.getConsultingDoctor().length > 0)
				|| trimToNull(pv1.getHospitalService().getValueOrEmpty()) != null
				|| Hl7PersonLocation.isPresent(pv1.getTemporaryLocation())
				|| trimToNull(pv1.getPreadmitTestIndicator().getValueOrEmpty()) != null
				|| trimToNull(pv1.getReAdmissionIndicator().getValueOrEmpty()) != null
				|| trimToNull(pv1.getAdmitSource().getValueOrEmpty()) != null
				|| (pv1.getAmbulatoryStatus() != null && pv1.getAmbulatoryStatus().length > 0)
				|| trimToNull(pv1.getVIPIndicator().getValueOrEmpty()) != null
				|| (pv1.getAdmittingDoctor() != null && pv1.getAdmittingDoctor().length > 0)
				|| trimToNull(pv1.getPatientType().getValueOrEmpty()) != null
				|| Hl7ExtendedCompositeIdWithCheckDigit.isPresent(pv1.getVisitNumber())
				|| (pv1.getFinancialClass() != null && pv1.getFinancialClass().length > 0)
				|| trimToNull(pv1.getChargePriceIndicator().getValueOrEmpty()) != null
				|| trimToNull(pv1.getCourtesyCode().getValueOrEmpty()) != null
				|| trimToNull(pv1.getCreditRating().getValueOrEmpty()) != null
				|| (pv1.getContractCode() != null && pv1.getContractCode().length > 0)
				|| (pv1.getContractEffectiveDate() != null && pv1.getContractEffectiveDate().length > 0)
				|| (pv1.getContractAmount() != null && pv1.getContractAmount().length > 0)
				|| (pv1.getContractPeriod() != null && pv1.getContractPeriod().length > 0)
				|| trimToNull(pv1.getInterestCode().getValueOrEmpty()) != null
				|| trimToNull(pv1.getTransferToBadDebtCode().getValueOrEmpty()) != null
				|| trimToNull(pv1.getTransferToBadDebtDate().getValue()) != null
				|| trimToNull(pv1.getBadDebtAgencyCode().getValueOrEmpty()) != null
				|| trimToNull(pv1.getBadDebtTransferAmount().getValue()) != null
				|| trimToNull(pv1.getBadDebtRecoveryAmount().getValue()) != null
				|| trimToNull(pv1.getDeleteAccountIndicator().getValueOrEmpty()) != null
				|| trimToNull(pv1.getDeleteAccountDate().getValue()) != null
				|| trimToNull(pv1.getDischargeDisposition().getValueOrEmpty()) != null
				;

	}

	public Hl7PatientVisitSegment() {
		// Nothing to do
	}

	public Hl7PatientVisitSegment(@Nullable PV1 pv1) {
		if (pv1 != null) {
			String setIdAsString = trimToNull(pv1.getSetIDPV1().getValue());
			if (setIdAsString != null)
				this.setId = Integer.parseInt(setIdAsString, 10);

			this.patientClass = trimToNull(pv1.getPatientClass().getValueOrEmpty());

			if (Hl7PersonLocation.isPresent(pv1.getAssignedPatientLocation()))
				this.assignedPatientLocation = new Hl7PersonLocation(pv1.getAssignedPatientLocation());

			this.admissionType = trimToNull(pv1.getAdmissionType().getValueOrEmpty());

			if (Hl7ExtendedCompositeIdWithCheckDigit.isPresent(pv1.getPreadmitNumber()))
				this.preadmitNumber = new Hl7ExtendedCompositeIdWithCheckDigit(pv1.getPreadmitNumber());

			if (Hl7PersonLocation.isPresent(pv1.getPriorPatientLocation()))
				this.priorPatientLocation = new Hl7PersonLocation(pv1.getPriorPatientLocation());

			if (pv1.getAttendingDoctor() != null && pv1.getAttendingDoctor().length > 0)
				this.attendingDoctor = Arrays.stream(pv1.getAttendingDoctor())
						.map(xcn -> Hl7ExtendedCompositeIdNumberAndNameForPersons.isPresent(xcn) ? new Hl7ExtendedCompositeIdNumberAndNameForPersons(xcn) : null)
						.filter(attendingDoctor -> attendingDoctor != null)
						.collect(Collectors.toList());

			if (pv1.getReferringDoctor() != null && pv1.getReferringDoctor().length > 0)
				this.referringDoctor = Arrays.stream(pv1.getReferringDoctor())
						.map(xcn -> Hl7ExtendedCompositeIdNumberAndNameForPersons.isPresent(xcn) ? new Hl7ExtendedCompositeIdNumberAndNameForPersons(xcn) : null)
						.filter(referringDoctor -> referringDoctor != null)
						.collect(Collectors.toList());

			if (pv1.getConsultingDoctor() != null && pv1.getConsultingDoctor().length > 0)
				this.consultingDoctor = Arrays.stream(pv1.getConsultingDoctor())
						.map(xcn -> Hl7ExtendedCompositeIdNumberAndNameForPersons.isPresent(xcn) ? new Hl7ExtendedCompositeIdNumberAndNameForPersons(xcn) : null)
						.filter(consultingDoctor -> consultingDoctor != null)
						.collect(Collectors.toList());

			this.hospitalService = trimToNull(pv1.getHospitalService().getValueOrEmpty());

			if (Hl7PersonLocation.isPresent(pv1.getTemporaryLocation()))
				this.temporaryLocation = new Hl7PersonLocation(pv1.getTemporaryLocation());

			this.preadmitTestIndicator = trimToNull(pv1.getPreadmitTestIndicator().getValueOrEmpty());
			this.readmissionIndicator = trimToNull(pv1.getReAdmissionIndicator().getValueOrEmpty());
			this.admitSource = trimToNull(pv1.getAdmitSource().getValueOrEmpty());

			if (pv1.getAmbulatoryStatus() != null && pv1.getAmbulatoryStatus().length > 0)
				this.ambulatoryStatus = Arrays.stream(pv1.getAmbulatoryStatus())
						.map(is -> trimToNull(is.getValueOrEmpty()))
						.filter(ambulatoryStatus -> ambulatoryStatus != null)
						.collect(Collectors.toList());

			this.vipIndicator = trimToNull(pv1.getVIPIndicator().getValueOrEmpty());

			if (pv1.getAdmittingDoctor() != null && pv1.getAdmittingDoctor().length > 0)
				this.admittingDoctor = Arrays.stream(pv1.getAdmittingDoctor())
						.map(xcn -> Hl7ExtendedCompositeIdNumberAndNameForPersons.isPresent(xcn) ? new Hl7ExtendedCompositeIdNumberAndNameForPersons(xcn) : null)
						.filter(admittingDoctor -> admittingDoctor != null)
						.collect(Collectors.toList());

			this.patientType = trimToNull(pv1.getPatientType().getValueOrEmpty());

			if (Hl7ExtendedCompositeIdWithCheckDigit.isPresent(pv1.getVisitNumber()))
				this.visitNumber = new Hl7ExtendedCompositeIdWithCheckDigit(pv1.getVisitNumber());

			if (pv1.getFinancialClass() != null && pv1.getFinancialClass().length > 0)
				this.financialClass = Arrays.stream(pv1.getFinancialClass())
						.map(fc -> Hl7FinancialClass.isPresent(fc) ? new Hl7FinancialClass(fc) : null)
						.filter(financialClass -> financialClass != null)
						.collect(Collectors.toList());

			this.chargePriceIndicator = trimToNull(pv1.getChargePriceIndicator().getValueOrEmpty());
			this.courtesyCode = trimToNull(pv1.getCourtesyCode().getValueOrEmpty());
			this.creditRating = trimToNull(pv1.getCreditRating().getValueOrEmpty());

			if (pv1.getContractCode() != null && pv1.getContractCode().length > 0)
				this.contractCode = Arrays.stream(pv1.getContractCode())
						.map(is -> trimToNull(is.getValueOrEmpty()))
						.filter(contractCode -> contractCode != null)
						.collect(Collectors.toList());

			if (pv1.getContractEffectiveDate() != null && pv1.getContractEffectiveDate().length > 0)
				this.contractEffectiveDate = Arrays.stream(pv1.getContractEffectiveDate())
						.map(dt -> trimToNull(dt.getValue()))
						.filter(contractEffectiveDate -> contractEffectiveDate != null)
						.collect(Collectors.toList());

			if (pv1.getContractAmount() != null && pv1.getContractAmount().length > 0)
				this.contractAmount = Arrays.stream(pv1.getContractAmount())
						.map(nm -> trimToNull(nm.getValue()) != null ? Double.parseDouble(nm.getValue()) : null)
						.filter(contractAmount -> contractAmount != null)
						.collect(Collectors.toList());

			if (pv1.getContractPeriod() != null && pv1.getContractPeriod().length > 0)
				this.contractPeriod = Arrays.stream(pv1.getContractPeriod())
						.map(nm -> trimToNull(nm.getValue()) != null ? Double.parseDouble(nm.getValue()) : null)
						.filter(contractPeriod -> contractPeriod != null)
						.collect(Collectors.toList());

			this.interestCode = trimToNull(pv1.getInterestCode().getValueOrEmpty());
			this.transferToBadDebtCode = trimToNull(pv1.getTransferToBadDebtCode().getValueOrEmpty());
			this.transferToBadDebtDate = trimToNull(pv1.getTransferToBadDebtCode().getValue());
			this.badDebtAgencyCode = trimToNull(pv1.getBadDebtAgencyCode().getValue());

			String badDebtTransferAmountAsString = trimToNull(pv1.getBadDebtTransferAmount().getValue());
			if (badDebtTransferAmountAsString != null)
				this.badDebtTransferAmount = Double.parseDouble(badDebtTransferAmountAsString);

			String badDebtRecoveryAmountAsString = trimToNull(pv1.getBadDebtRecoveryAmount().getValue());
			if (badDebtRecoveryAmountAsString != null)
				this.badDebtRecoveryAmount = Double.parseDouble(badDebtRecoveryAmountAsString);

			this.deleteAccountIndicator = trimToNull(pv1.getDeleteAccountIndicator().getValueOrEmpty());
			this.deleteAccountDate = trimToNull(pv1.getDeleteAccountDate().getValue());
			this.dischargeDisposition = trimToNull(pv1.getDischargeDisposition().getValueOrEmpty());

		}
	}
}