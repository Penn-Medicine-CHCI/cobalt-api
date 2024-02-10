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

import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7EntityIdentifier;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7EntityIdentifierPair;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedCompositeIdNumberAndNameForPersons;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedTelecommunicationNumber;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7PersonLocation;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7TimeStamp;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7TimingQuantity;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/Segments/ORC
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7CommonOrder extends Hl7Object {
	@Nullable
	private String orderControl; // ORC.1 - Order Control
	@Nullable
	private Hl7EntityIdentifier placerOrderNumber; // ORC.2 - Placer Order Number
	@Nullable
	private Hl7EntityIdentifier fillerOrderNumber; // ORC.3 - Filler Order Number
	@Nullable
	private Hl7EntityIdentifier placerGroupNumber; // ORC.4 - Placer Group Number
	@Nullable
	private String orderStatus; // ORC.5 - Order Status
	@Nullable
	private String responseFlag; // ORC.6 - Response Flag
	@Nullable
	private List<Hl7TimingQuantity> quantityTiming; // ORC.7 - Quantity/Timing
	@Nullable
	private Hl7EntityIdentifierPair parentOrder; // ORC.8 - Parent Order
	@Nullable
	private Hl7TimeStamp dateTimeOfTransaction; // ORC.9 - Date/Time of Transaction
	@Nullable
	private List<Hl7ExtendedCompositeIdNumberAndNameForPersons> enteredBy; // ORC.10 - Entered By
	@Nullable
	private List<Hl7ExtendedCompositeIdNumberAndNameForPersons> verifiedBy; // ORC.11 - Verified By
	@Nullable
	private List<Hl7ExtendedCompositeIdNumberAndNameForPersons> orderingProvider; // ORC.12 - Ordering Provider
	@Nullable
	private Hl7PersonLocation enterersLocation; // ORC.13 - Enterer's Location
	@Nullable
	private List<Hl7ExtendedTelecommunicationNumber> callBackPhoneNumber; // ORC.14 - Call Back Phone Number
	@Nullable
	private String orderEffectiveDateTime; // ORC.15 - Order Effective Date/Time
	@Nullable
	private String orderControlCodeReason; // ORC.16 - Order Control Code Reason
	@Nullable
	private String enteringOrganization; // ORC.17 - Entering Organization
	@Nullable
	private String enteringDevice; // ORC.18 - Entering Device
	@Nullable
	private String actionBy; // ORC.19 - Action By
	@Nullable
	private String advancedBeneficiaryNoticeCode; // ORC.20 - Advanced Beneficiary Notice Code
	@Nullable
	private String orderingFacilityName; // ORC.21 - Ordering Facility Name
	@Nullable
	private String orderingFacilityAddress; // ORC.22 - Ordering Facility Address
	@Nullable
	private String orderingFacilityPhoneNumber; // ORC.23 - Ordering Facility Phone Number
	@Nullable
	private String orderingProviderAddress; // ORC.24 - Ordering Provider Address
	@Nullable
	private String orderStatusModifier; // ORC.25 - Order Status Modifier
	@Nullable
	private String advancedBeneficiaryNoticeOverrideReason; // ORC.26 - Advanced Beneficiary Notice Override Reason
	@Nullable
	private String fillersExpectedAvailabilityDateTime; // ORC.27 - Filler's Expected Availability Date/Time
	@Nullable
	private String confidentialityCode; // ORC.28 - Confidentiality Code
	@Nullable
	private String orderType; // ORC.29 - Order Type
	@Nullable
	private String entererAuthorizationMode; // ORC.30 - Enterer Authorization Mode
	@Nullable
	private String parentUniversalServiceIdentifier; // ORC.31 - Parent Universal Service Identifier

	@Nullable
	public String getOrderControl() {
		return this.orderControl;
	}

	public void setOrderControl(@Nullable String orderControl) {
		this.orderControl = orderControl;
	}

	@Nullable
	public Hl7EntityIdentifier getPlacerOrderNumber() {
		return this.placerOrderNumber;
	}

	public void setPlacerOrderNumber(@Nullable Hl7EntityIdentifier placerOrderNumber) {
		this.placerOrderNumber = placerOrderNumber;
	}

	@Nullable
	public Hl7EntityIdentifier getFillerOrderNumber() {
		return this.fillerOrderNumber;
	}

	public void setFillerOrderNumber(@Nullable Hl7EntityIdentifier fillerOrderNumber) {
		this.fillerOrderNumber = fillerOrderNumber;
	}

	@Nullable
	public Hl7EntityIdentifier getPlacerGroupNumber() {
		return this.placerGroupNumber;
	}

	public void setPlacerGroupNumber(@Nullable Hl7EntityIdentifier placerGroupNumber) {
		this.placerGroupNumber = placerGroupNumber;
	}

	@Nullable
	public String getOrderStatus() {
		return this.orderStatus;
	}

	public void setOrderStatus(@Nullable String orderStatus) {
		this.orderStatus = orderStatus;
	}

	@Nullable
	public String getResponseFlag() {
		return this.responseFlag;
	}

	public void setResponseFlag(@Nullable String responseFlag) {
		this.responseFlag = responseFlag;
	}

	@Nullable
	public List<Hl7TimingQuantity> getQuantityTiming() {
		return this.quantityTiming;
	}

	public void setQuantityTiming(@Nullable List<Hl7TimingQuantity> quantityTiming) {
		this.quantityTiming = quantityTiming;
	}

	@Nullable
	public Hl7EntityIdentifierPair getParentOrder() {
		return this.parentOrder;
	}

	public void setParentOrder(@Nullable Hl7EntityIdentifierPair parentOrder) {
		this.parentOrder = parentOrder;
	}

	@Nullable
	public Hl7TimeStamp getDateTimeOfTransaction() {
		return this.dateTimeOfTransaction;
	}

	public void setDateTimeOfTransaction(@Nullable Hl7TimeStamp dateTimeOfTransaction) {
		this.dateTimeOfTransaction = dateTimeOfTransaction;
	}

	@Nullable
	public List<Hl7ExtendedCompositeIdNumberAndNameForPersons> getEnteredBy() {
		return this.enteredBy;
	}

	public void setEnteredBy(@Nullable List<Hl7ExtendedCompositeIdNumberAndNameForPersons> enteredBy) {
		this.enteredBy = enteredBy;
	}

	@Nullable
	public List<Hl7ExtendedCompositeIdNumberAndNameForPersons> getVerifiedBy() {
		return this.verifiedBy;
	}

	public void setVerifiedBy(@Nullable List<Hl7ExtendedCompositeIdNumberAndNameForPersons> verifiedBy) {
		this.verifiedBy = verifiedBy;
	}

	@Nullable
	public List<Hl7ExtendedCompositeIdNumberAndNameForPersons> getOrderingProvider() {
		return this.orderingProvider;
	}

	public void setOrderingProvider(@Nullable List<Hl7ExtendedCompositeIdNumberAndNameForPersons> orderingProvider) {
		this.orderingProvider = orderingProvider;
	}

	@Nullable
	public Hl7PersonLocation getEnterersLocation() {
		return this.enterersLocation;
	}

	public void setEnterersLocation(@Nullable Hl7PersonLocation enterersLocation) {
		this.enterersLocation = enterersLocation;
	}

	@Nullable
	public List<Hl7ExtendedTelecommunicationNumber> getCallBackPhoneNumber() {
		return this.callBackPhoneNumber;
	}

	public void setCallBackPhoneNumber(@Nullable List<Hl7ExtendedTelecommunicationNumber> callBackPhoneNumber) {
		this.callBackPhoneNumber = callBackPhoneNumber;
	}

	@Nullable
	public String getOrderEffectiveDateTime() {
		return this.orderEffectiveDateTime;
	}

	public void setOrderEffectiveDateTime(@Nullable String orderEffectiveDateTime) {
		this.orderEffectiveDateTime = orderEffectiveDateTime;
	}

	@Nullable
	public String getOrderControlCodeReason() {
		return this.orderControlCodeReason;
	}

	public void setOrderControlCodeReason(@Nullable String orderControlCodeReason) {
		this.orderControlCodeReason = orderControlCodeReason;
	}

	@Nullable
	public String getEnteringOrganization() {
		return this.enteringOrganization;
	}

	public void setEnteringOrganization(@Nullable String enteringOrganization) {
		this.enteringOrganization = enteringOrganization;
	}

	@Nullable
	public String getEnteringDevice() {
		return this.enteringDevice;
	}

	public void setEnteringDevice(@Nullable String enteringDevice) {
		this.enteringDevice = enteringDevice;
	}

	@Nullable
	public String getActionBy() {
		return this.actionBy;
	}

	public void setActionBy(@Nullable String actionBy) {
		this.actionBy = actionBy;
	}

	@Nullable
	public String getAdvancedBeneficiaryNoticeCode() {
		return this.advancedBeneficiaryNoticeCode;
	}

	public void setAdvancedBeneficiaryNoticeCode(@Nullable String advancedBeneficiaryNoticeCode) {
		this.advancedBeneficiaryNoticeCode = advancedBeneficiaryNoticeCode;
	}

	@Nullable
	public String getOrderingFacilityName() {
		return this.orderingFacilityName;
	}

	public void setOrderingFacilityName(@Nullable String orderingFacilityName) {
		this.orderingFacilityName = orderingFacilityName;
	}

	@Nullable
	public String getOrderingFacilityAddress() {
		return this.orderingFacilityAddress;
	}

	public void setOrderingFacilityAddress(@Nullable String orderingFacilityAddress) {
		this.orderingFacilityAddress = orderingFacilityAddress;
	}

	@Nullable
	public String getOrderingFacilityPhoneNumber() {
		return this.orderingFacilityPhoneNumber;
	}

	public void setOrderingFacilityPhoneNumber(@Nullable String orderingFacilityPhoneNumber) {
		this.orderingFacilityPhoneNumber = orderingFacilityPhoneNumber;
	}

	@Nullable
	public String getOrderingProviderAddress() {
		return this.orderingProviderAddress;
	}

	public void setOrderingProviderAddress(@Nullable String orderingProviderAddress) {
		this.orderingProviderAddress = orderingProviderAddress;
	}

	@Nullable
	public String getOrderStatusModifier() {
		return this.orderStatusModifier;
	}

	public void setOrderStatusModifier(@Nullable String orderStatusModifier) {
		this.orderStatusModifier = orderStatusModifier;
	}

	@Nullable
	public String getAdvancedBeneficiaryNoticeOverrideReason() {
		return this.advancedBeneficiaryNoticeOverrideReason;
	}

	public void setAdvancedBeneficiaryNoticeOverrideReason(@Nullable String advancedBeneficiaryNoticeOverrideReason) {
		this.advancedBeneficiaryNoticeOverrideReason = advancedBeneficiaryNoticeOverrideReason;
	}

	@Nullable
	public String getFillersExpectedAvailabilityDateTime() {
		return this.fillersExpectedAvailabilityDateTime;
	}

	public void setFillersExpectedAvailabilityDateTime(@Nullable String fillersExpectedAvailabilityDateTime) {
		this.fillersExpectedAvailabilityDateTime = fillersExpectedAvailabilityDateTime;
	}

	@Nullable
	public String getConfidentialityCode() {
		return this.confidentialityCode;
	}

	public void setConfidentialityCode(@Nullable String confidentialityCode) {
		this.confidentialityCode = confidentialityCode;
	}

	@Nullable
	public String getOrderType() {
		return this.orderType;
	}

	public void setOrderType(@Nullable String orderType) {
		this.orderType = orderType;
	}

	@Nullable
	public String getEntererAuthorizationMode() {
		return this.entererAuthorizationMode;
	}

	public void setEntererAuthorizationMode(@Nullable String entererAuthorizationMode) {
		this.entererAuthorizationMode = entererAuthorizationMode;
	}

	@Nullable
	public String getParentUniversalServiceIdentifier() {
		return this.parentUniversalServiceIdentifier;
	}

	public void setParentUniversalServiceIdentifier(@Nullable String parentUniversalServiceIdentifier) {
		this.parentUniversalServiceIdentifier = parentUniversalServiceIdentifier;
	}
}