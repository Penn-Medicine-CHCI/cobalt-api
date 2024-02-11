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

import ca.uhn.hl7v2.model.v251.segment.ORC;
import com.cobaltplatform.api.integration.hl7.model.Hl7Object;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7CodedElement;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7CodedWithExceptions;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7CodedWithNoExceptions;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7EntityIdentifier;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7EntityIdentifierPair;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedAddress;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedCompositeIdNumberAndNameForPersons;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7ExtendedTelecommunicationNumber;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7PersonLocation;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7TimeStamp;
import com.cobaltplatform.api.integration.hl7.model.type.Hl7TimingQuantity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/Segments/ORC
 *
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7CommonOrderSegment extends Hl7Object {
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
	private Hl7TimeStamp orderEffectiveDateTime; // ORC.15 - Order Effective Date/Time
	@Nullable
	private Hl7CodedElement orderControlCodeReason; // ORC.16 - Order Control Code Reason
	@Nullable
	private Hl7CodedElement enteringOrganization; // ORC.17 - Entering Organization
	@Nullable
	private Hl7CodedElement enteringDevice; // ORC.18 - Entering Device
	@Nullable
	private List<Hl7ExtendedCompositeIdNumberAndNameForPersons> actionBy; // ORC.19 - Action By
	@Nullable
	private Hl7CodedElement advancedBeneficiaryNoticeCode; // ORC.20 - Advanced Beneficiary Notice Code
	@Nullable
	private List<Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations> orderingFacilityName; // ORC.21 - Ordering Facility Name
	@Nullable
	private List<Hl7ExtendedAddress> orderingFacilityAddress; // ORC.22 - Ordering Facility Address
	@Nullable
	private List<Hl7ExtendedTelecommunicationNumber> orderingFacilityPhoneNumber; // ORC.23 - Ordering Facility Phone Number
	@Nullable
	private List<Hl7ExtendedAddress> orderingProviderAddress; // ORC.24 - Ordering Provider Address
	@Nullable
	private Hl7CodedWithExceptions orderStatusModifier; // ORC.25 - Order Status Modifier
	@Nullable
	private Hl7CodedWithExceptions advancedBeneficiaryNoticeOverrideReason; // ORC.26 - Advanced Beneficiary Notice Override Reason
	@Nullable
	private Hl7TimeStamp fillersExpectedAvailabilityDateTime; // ORC.27 - Filler's Expected Availability Date/Time
	@Nullable
	private Hl7CodedWithExceptions confidentialityCode; // ORC.28 - Confidentiality Code
	@Nullable
	private Hl7CodedWithExceptions orderType; // ORC.29 - Order Type
	@Nullable
	private Hl7CodedWithNoExceptions entererAuthorizationMode; // ORC.30 - Enterer Authorization Mode
	@Nullable
	private Hl7CodedWithExceptions parentUniversalServiceIdentifier; // ORC.31 - Parent Universal Service Identifier

	@Nonnull
	public static Boolean isPresent(@Nullable ORC orc) {
		if (orc == null)
			return false;

		return true;
	}

	public Hl7CommonOrderSegment() {
		// Nothing to do
	}

	public Hl7CommonOrderSegment(@Nullable ORC orc) {
		if (orc != null) {
			this.orderControl = trimToNull(orc.getOrderControl().getValueOrEmpty());

			if (Hl7EntityIdentifier.isPresent(orc.getPlacerOrderNumber()))
				this.placerOrderNumber = new Hl7EntityIdentifier(orc.getPlacerOrderNumber());

			if (Hl7EntityIdentifier.isPresent(orc.getPlacerGroupNumber()))
				this.placerGroupNumber = new Hl7EntityIdentifier(orc.getPlacerGroupNumber());

			if (Hl7EntityIdentifier.isPresent(orc.getFillerOrderNumber()))
				this.fillerOrderNumber = new Hl7EntityIdentifier(orc.getFillerOrderNumber());

			this.orderStatus = trimToNull(orc.getOrderStatus().getValueOrEmpty());
			this.responseFlag = trimToNull(orc.getResponseFlag().getValueOrEmpty());

			if (orc.getQuantityTiming() != null && orc.getQuantityTiming().length > 0)
				this.quantityTiming = Arrays.stream(orc.getQuantityTiming())
						.map((qt) -> Hl7TimingQuantity.isPresent(qt) ? new Hl7TimingQuantity(qt) : null)
						.filter(tq -> tq != null)
						.collect(Collectors.toList());

			if (Hl7EntityIdentifierPair.isPresent(orc.getORCParent()))
				this.parentOrder = new Hl7EntityIdentifierPair(orc.getORCParent());

			if (Hl7TimeStamp.isPresent(orc.getDateTimeOfTransaction()))
				this.dateTimeOfTransaction = new Hl7TimeStamp(orc.getDateTimeOfTransaction());

			if (orc.getEnteredBy() != null && orc.getEnteredBy().length > 0)
				this.enteredBy = Arrays.stream(orc.getEnteredBy())
						.map(xcn -> Hl7ExtendedCompositeIdNumberAndNameForPersons.isPresent(xcn) ? new Hl7ExtendedCompositeIdNumberAndNameForPersons(xcn) : null)
						.filter(enteredBy -> enteredBy != null)
						.collect(Collectors.toList());

			if (orc.getVerifiedBy() != null && orc.getVerifiedBy().length > 0)
				this.verifiedBy = Arrays.stream(orc.getVerifiedBy())
						.map(xcn -> Hl7ExtendedCompositeIdNumberAndNameForPersons.isPresent(xcn) ? new Hl7ExtendedCompositeIdNumberAndNameForPersons(xcn) : null)
						.filter(verifiedBy -> verifiedBy != null)
						.collect(Collectors.toList());

			if (orc.getOrderingProvider() != null && orc.getOrderingProvider().length > 0)
				this.orderingProvider = Arrays.stream(orc.getOrderingProvider())
						.map(xcn -> Hl7ExtendedCompositeIdNumberAndNameForPersons.isPresent(xcn) ? new Hl7ExtendedCompositeIdNumberAndNameForPersons(xcn) : null)
						.filter(orderingProvider -> orderingProvider != null)
						.collect(Collectors.toList());

			if (Hl7PersonLocation.isPresent(orc.getEntererSLocation()))
				this.enterersLocation = new Hl7PersonLocation(orc.getEntererSLocation());

			if (orc.getCallBackPhoneNumber() != null && orc.getCallBackPhoneNumber().length > 0)
				this.callBackPhoneNumber = Arrays.stream(orc.getCallBackPhoneNumber())
						.map(xtn -> Hl7ExtendedTelecommunicationNumber.isPresent(xtn) ? new Hl7ExtendedTelecommunicationNumber(xtn) : null)
						.filter(extendedTelecommunicationNumber -> extendedTelecommunicationNumber != null)
						.collect(Collectors.toList());

			if (Hl7TimeStamp.isPresent(orc.getOrderEffectiveDateTime()))
				this.orderEffectiveDateTime = new Hl7TimeStamp(orc.getOrderEffectiveDateTime());

			if (Hl7CodedElement.isPresent(orc.getOrderControlCodeReason()))
				this.orderControlCodeReason = new Hl7CodedElement(orc.getOrderControlCodeReason());

			if (Hl7CodedElement.isPresent(orc.getEnteringOrganization()))
				this.enteringOrganization = new Hl7CodedElement(orc.getEnteringOrganization());

			if (Hl7CodedElement.isPresent(orc.getEnteringDevice()))
				this.enteringDevice = new Hl7CodedElement(orc.getEnteringDevice());

			if (orc.getActionBy() != null && orc.getActionBy().length > 0)
				this.actionBy = Arrays.stream(orc.getActionBy())
						.map(xcn -> Hl7ExtendedCompositeIdNumberAndNameForPersons.isPresent(xcn) ? new Hl7ExtendedCompositeIdNumberAndNameForPersons(xcn) : null)
						.filter(actionBy -> actionBy != null)
						.collect(Collectors.toList());

			if (Hl7CodedElement.isPresent(orc.getAdvancedBeneficiaryNoticeCode()))
				this.advancedBeneficiaryNoticeCode = new Hl7CodedElement(orc.getAdvancedBeneficiaryNoticeCode());

			if (orc.getOrderingFacilityName() != null && orc.getOrderingFacilityName().length > 0)
				this.orderingFacilityName = Arrays.stream(orc.getOrderingFacilityName())
						.map(xon -> Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations.isPresent(xon) ? new Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations(xon) : null)
						.filter(orderingFacilityName -> orderingFacilityName != null)
						.collect(Collectors.toList());

			if (orc.getOrderingFacilityAddress() != null && orc.getOrderingFacilityAddress().length > 0)
				this.orderingFacilityAddress = Arrays.stream(orc.getOrderingFacilityAddress())
						.map(xad -> Hl7ExtendedAddress.isPresent(xad) ? new Hl7ExtendedAddress(xad) : null)
						.filter(orderingFacilityAddress -> orderingFacilityAddress != null)
						.collect(Collectors.toList());

			if (orc.getOrderingFacilityPhoneNumber() != null && orc.getOrderingFacilityPhoneNumber().length > 0)
				this.orderingFacilityPhoneNumber = Arrays.stream(orc.getOrderingFacilityPhoneNumber())
						.map(xtn -> Hl7ExtendedTelecommunicationNumber.isPresent(xtn) ? new Hl7ExtendedTelecommunicationNumber(xtn) : null)
						.filter(orderingFacilityPhoneNumber -> orderingFacilityPhoneNumber != null)
						.collect(Collectors.toList());

			if (orc.getOrderingProviderAddress() != null && orc.getOrderingProviderAddress().length > 0)
				this.orderingProviderAddress = Arrays.stream(orc.getOrderingProviderAddress())
						.map(xad -> Hl7ExtendedAddress.isPresent(xad) ? new Hl7ExtendedAddress(xad) : null)
						.filter(orderingProviderAddress -> orderingProviderAddress != null)
						.collect(Collectors.toList());

			if (Hl7CodedWithExceptions.isPresent(orc.getOrderStatusModifier()))
				this.orderStatusModifier = new Hl7CodedWithExceptions(orc.getOrderStatusModifier());

			if (Hl7CodedWithExceptions.isPresent(orc.getAdvancedBeneficiaryNoticeOverrideReason()))
				this.advancedBeneficiaryNoticeOverrideReason = new Hl7CodedWithExceptions(orc.getAdvancedBeneficiaryNoticeOverrideReason());

			if (Hl7TimeStamp.isPresent(orc.getFillerSExpectedAvailabilityDateTime()))
				this.fillersExpectedAvailabilityDateTime = new Hl7TimeStamp(orc.getFillerSExpectedAvailabilityDateTime());

			if (Hl7CodedWithExceptions.isPresent(orc.getConfidentialityCode()))
				this.confidentialityCode = new Hl7CodedWithExceptions(orc.getConfidentialityCode());

			if (Hl7CodedWithExceptions.isPresent(orc.getOrderType()))
				this.orderType = new Hl7CodedWithExceptions(orc.getOrderType());

			if (Hl7CodedWithNoExceptions.isPresent(orc.getEntererAuthorizationMode()))
				this.entererAuthorizationMode = new Hl7CodedWithNoExceptions(orc.getEntererAuthorizationMode());

			if (Hl7CodedWithExceptions.isPresent(orc.getParentUniversalServiceIdentifier()))
				this.parentUniversalServiceIdentifier = new Hl7CodedWithExceptions(orc.getParentUniversalServiceIdentifier());
		}
	}

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
	public Hl7TimeStamp getOrderEffectiveDateTime() {
		return this.orderEffectiveDateTime;
	}

	public void setOrderEffectiveDateTime(@Nullable Hl7TimeStamp orderEffectiveDateTime) {
		this.orderEffectiveDateTime = orderEffectiveDateTime;
	}

	@Nullable
	public Hl7CodedElement getOrderControlCodeReason() {
		return this.orderControlCodeReason;
	}

	public void setOrderControlCodeReason(@Nullable Hl7CodedElement orderControlCodeReason) {
		this.orderControlCodeReason = orderControlCodeReason;
	}

	@Nullable
	public Hl7CodedElement getEnteringOrganization() {
		return this.enteringOrganization;
	}

	public void setEnteringOrganization(@Nullable Hl7CodedElement enteringOrganization) {
		this.enteringOrganization = enteringOrganization;
	}

	@Nullable
	public Hl7CodedElement getEnteringDevice() {
		return this.enteringDevice;
	}

	public void setEnteringDevice(@Nullable Hl7CodedElement enteringDevice) {
		this.enteringDevice = enteringDevice;
	}

	@Nullable
	public List<Hl7ExtendedCompositeIdNumberAndNameForPersons> getActionBy() {
		return this.actionBy;
	}

	public void setActionBy(@Nullable List<Hl7ExtendedCompositeIdNumberAndNameForPersons> actionBy) {
		this.actionBy = actionBy;
	}

	@Nullable
	public Hl7CodedElement getAdvancedBeneficiaryNoticeCode() {
		return this.advancedBeneficiaryNoticeCode;
	}

	public void setAdvancedBeneficiaryNoticeCode(@Nullable Hl7CodedElement advancedBeneficiaryNoticeCode) {
		this.advancedBeneficiaryNoticeCode = advancedBeneficiaryNoticeCode;
	}

	@Nullable
	public List<Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations> getOrderingFacilityName() {
		return this.orderingFacilityName;
	}

	public void setOrderingFacilityName(@Nullable List<Hl7ExtendedCompositeNameAndIdentificationNumberForOrganizations> orderingFacilityName) {
		this.orderingFacilityName = orderingFacilityName;
	}

	@Nullable
	public List<Hl7ExtendedAddress> getOrderingFacilityAddress() {
		return this.orderingFacilityAddress;
	}

	public void setOrderingFacilityAddress(@Nullable List<Hl7ExtendedAddress> orderingFacilityAddress) {
		this.orderingFacilityAddress = orderingFacilityAddress;
	}

	@Nullable
	public List<Hl7ExtendedTelecommunicationNumber> getOrderingFacilityPhoneNumber() {
		return this.orderingFacilityPhoneNumber;
	}

	public void setOrderingFacilityPhoneNumber(@Nullable List<Hl7ExtendedTelecommunicationNumber> orderingFacilityPhoneNumber) {
		this.orderingFacilityPhoneNumber = orderingFacilityPhoneNumber;
	}

	@Nullable
	public List<Hl7ExtendedAddress> getOrderingProviderAddress() {
		return this.orderingProviderAddress;
	}

	public void setOrderingProviderAddress(@Nullable List<Hl7ExtendedAddress> orderingProviderAddress) {
		this.orderingProviderAddress = orderingProviderAddress;
	}

	@Nullable
	public Hl7CodedWithExceptions getOrderStatusModifier() {
		return this.orderStatusModifier;
	}

	public void setOrderStatusModifier(@Nullable Hl7CodedWithExceptions orderStatusModifier) {
		this.orderStatusModifier = orderStatusModifier;
	}

	@Nullable
	public Hl7CodedWithExceptions getAdvancedBeneficiaryNoticeOverrideReason() {
		return this.advancedBeneficiaryNoticeOverrideReason;
	}

	public void setAdvancedBeneficiaryNoticeOverrideReason(@Nullable Hl7CodedWithExceptions advancedBeneficiaryNoticeOverrideReason) {
		this.advancedBeneficiaryNoticeOverrideReason = advancedBeneficiaryNoticeOverrideReason;
	}

	@Nullable
	public Hl7TimeStamp getFillersExpectedAvailabilityDateTime() {
		return this.fillersExpectedAvailabilityDateTime;
	}

	public void setFillersExpectedAvailabilityDateTime(@Nullable Hl7TimeStamp fillersExpectedAvailabilityDateTime) {
		this.fillersExpectedAvailabilityDateTime = fillersExpectedAvailabilityDateTime;
	}

	@Nullable
	public Hl7CodedWithExceptions getConfidentialityCode() {
		return this.confidentialityCode;
	}

	public void setConfidentialityCode(@Nullable Hl7CodedWithExceptions confidentialityCode) {
		this.confidentialityCode = confidentialityCode;
	}

	@Nullable
	public Hl7CodedWithExceptions getOrderType() {
		return this.orderType;
	}

	public void setOrderType(@Nullable Hl7CodedWithExceptions orderType) {
		this.orderType = orderType;
	}

	@Nullable
	public Hl7CodedWithNoExceptions getEntererAuthorizationMode() {
		return this.entererAuthorizationMode;
	}

	public void setEntererAuthorizationMode(@Nullable Hl7CodedWithNoExceptions entererAuthorizationMode) {
		this.entererAuthorizationMode = entererAuthorizationMode;
	}

	@Nullable
	public Hl7CodedWithExceptions getParentUniversalServiceIdentifier() {
		return this.parentUniversalServiceIdentifier;
	}

	public void setParentUniversalServiceIdentifier(@Nullable Hl7CodedWithExceptions parentUniversalServiceIdentifier) {
		this.parentUniversalServiceIdentifier = parentUniversalServiceIdentifier;
	}
}