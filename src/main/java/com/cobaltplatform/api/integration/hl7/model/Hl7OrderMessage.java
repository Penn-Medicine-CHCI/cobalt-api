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

package com.cobaltplatform.api.integration.hl7.model;

import ca.uhn.hl7v2.model.v251.datatype.CE;
import ca.uhn.hl7v2.model.v251.datatype.HD;
import ca.uhn.hl7v2.model.v251.datatype.MSG;
import ca.uhn.hl7v2.model.v251.datatype.PT;
import ca.uhn.hl7v2.model.v251.datatype.VID;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * @author Transmogrify, LLC.
 */
@NotThreadSafe
public class Hl7OrderMessage {
	// See https://hl7-definition.caristix.com/v2/hl7v2.5.1/TriggerEvents/ORM_O01

	@Nonnull
	private static final Gson GSON;

	static {
		GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	}

	@Nullable
	private MessageHeader messageHeader;
	@Nullable
	private CommonOrder commonOrder;

	@Override
	public String toString() {
		return GSON.toJson(this);
	}

	@Nullable
	public MessageHeader getMessageHeaderSegment() {
		return this.messageHeader;
	}

	public void setMessageHeaderSegment(@Nullable MessageHeader messageHeader) {
		this.messageHeader = messageHeader;
	}

	@Nullable
	public CommonOrder getCommonOrderSegment() {
		return this.commonOrder;
	}

	public void setCommonOrderSegment(@Nullable CommonOrder commonOrder) {
		this.commonOrder = commonOrder;
	}

	/**
	 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/HD
	 */
	@NotThreadSafe
	public static class HierarchicDesignator {
		@Nullable
		private String namespaceId;
		@Nullable
		private String universalId;
		@Nullable
		private String universalIdType;

		@Nonnull
		public static Boolean isPresent(@Nullable HD hd) {
			if (hd == null)
				return false;

			return trimToNull(hd.getNamespaceID().getValueOrEmpty()) != null
					|| trimToNull(hd.getUniversalID().getValueOrEmpty()) != null
					|| trimToNull(hd.getUniversalIDType().getValueOrEmpty()) != null;
		}

		public HierarchicDesignator() {
			// Nothing to do
		}

		public HierarchicDesignator(@Nullable HD hd) {
			if (hd != null) {
				this.namespaceId = trimToNull(hd.getNamespaceID().getValueOrEmpty());
				this.universalId = trimToNull(hd.getUniversalID().getValueOrEmpty());
				this.universalIdType = trimToNull(hd.getUniversalIDType().getValueOrEmpty());
			}
		}

		@Override
		public String toString() {
			return GSON.toJson(this);
		}

		@Nullable
		public String getNamespaceId() {
			return this.namespaceId;
		}

		public void setNamespaceId(@Nullable String namespaceId) {
			this.namespaceId = namespaceId;
		}

		@Nullable
		public String getUniversalId() {
			return this.universalId;
		}

		public void setUniversalId(@Nullable String universalId) {
			this.universalId = universalId;
		}

		@Nullable
		public String getUniversalIdType() {
			return this.universalIdType;
		}

		public void setUniversalIdType(@Nullable String universalIdType) {
			this.universalIdType = universalIdType;
		}
	}

	/**
	 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/CM_MSG
	 */
	@NotThreadSafe
	public static class MessageType {
		@Nullable
		private String messageCode;
		@Nullable
		private String triggerEvent;
		@Nullable
		private String messageStructure;

		@Nonnull
		public static Boolean isPresent(@Nullable MSG msg) {
			if (msg == null)
				return false;

			return trimToNull(msg.getMessageCode().getValueOrEmpty()) != null
					|| trimToNull(msg.getTriggerEvent().getValueOrEmpty()) != null
					|| trimToNull(msg.getMessageStructure().getValueOrEmpty()) != null;
		}

		public MessageType() {
			// Nothing to do
		}

		public MessageType(@Nullable MSG msg) {
			if (msg != null) {
				this.messageCode = trimToNull(msg.getMessageCode().getValueOrEmpty());
				this.triggerEvent = trimToNull(msg.getTriggerEvent().getValueOrEmpty());
				this.messageStructure = trimToNull(msg.getMessageStructure().getValueOrEmpty());
			}
		}

		@Override
		public String toString() {
			return GSON.toJson(this);
		}

		@Nullable
		public String getMessageCode() {
			return this.messageCode;
		}

		public void setMessageCode(@Nullable String messageCode) {
			this.messageCode = messageCode;
		}

		@Nullable
		public String getTriggerEvent() {
			return this.triggerEvent;
		}

		public void setTriggerEvent(@Nullable String triggerEvent) {
			this.triggerEvent = triggerEvent;
		}

		@Nullable
		public String getMessageStructure() {
			return this.messageStructure;
		}

		public void setMessageStructure(@Nullable String messageStructure) {
			this.messageStructure = messageStructure;
		}
	}

	/**
	 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/PT
	 */
	@NotThreadSafe
	public static class ProcessingType {
		@Nullable
		private String processingId;
		@Nullable
		private String processingMode;

		@Nonnull
		public static Boolean isPresent(@Nullable PT pt) {
			if (pt == null)
				return false;

			return trimToNull(pt.getProcessingID().getValueOrEmpty()) != null
					|| trimToNull(pt.getProcessingMode().getValueOrEmpty()) != null;
		}

		public ProcessingType() {
			// Nothing to do
		}

		public ProcessingType(@Nullable PT pt) {
			if (pt != null) {
				this.processingId = trimToNull(pt.getProcessingID().getValueOrEmpty());
				this.processingMode = trimToNull(pt.getProcessingMode().getValueOrEmpty());
			}
		}

		@Override
		public String toString() {
			return GSON.toJson(this);
		}

		@Nullable
		public String getProcessingId() {
			return this.processingId;
		}

		public void setProcessingId(@Nullable String processingId) {
			this.processingId = processingId;
		}

		@Nullable
		public String getProcessingMode() {
			return this.processingMode;
		}

		public void setProcessingMode(@Nullable String processingMode) {
			this.processingMode = processingMode;
		}
	}

	/**
	 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/CE
	 */
	@NotThreadSafe
	public static class CodedElement {
		@Nullable
		private String identifier;
		@Nullable
		private String text;
		@Nullable
		private String nameOfCodingSystem;
		@Nullable
		private String alternateIdentifier;
		@Nullable
		private String alternateText;
		@Nullable
		private String nameOfAlternateCodingSystem;

		@Nonnull
		public static Boolean isPresent(@Nullable CE ce) {
			if (ce == null)
				return false;

			return trimToNull(ce.getIdentifier().getValueOrEmpty()) != null
					|| trimToNull(ce.getText().getValueOrEmpty()) != null
					|| trimToNull(ce.getNameOfCodingSystem().getValueOrEmpty()) != null
					|| trimToNull(ce.getAlternateIdentifier().getValueOrEmpty()) != null
					|| trimToNull(ce.getAlternateText().getValueOrEmpty()) != null
					|| trimToNull(ce.getNameOfAlternateCodingSystem().getValueOrEmpty()) != null;
		}

		public CodedElement() {
			// Nothing to do
		}

		public CodedElement(@Nullable CE ce) {
			if (ce != null) {
				this.identifier = trimToNull(ce.getIdentifier().getValueOrEmpty());
				this.text = trimToNull(ce.getText().getValueOrEmpty());
				this.nameOfCodingSystem = trimToNull(ce.getNameOfCodingSystem().getValueOrEmpty());
				this.alternateIdentifier = trimToNull(ce.getAlternateIdentifier().getValueOrEmpty());
				this.alternateText = trimToNull(ce.getAlternateText().getValueOrEmpty());
				this.nameOfAlternateCodingSystem = trimToNull(ce.getNameOfAlternateCodingSystem().getValueOrEmpty());
			}
		}

		@Override
		public String toString() {
			return GSON.toJson(this);
		}

		@Nullable
		public String getIdentifier() {
			return this.identifier;
		}

		public void setIdentifier(@Nullable String identifier) {
			this.identifier = identifier;
		}

		@Nullable
		public String getText() {
			return this.text;
		}

		public void setText(@Nullable String text) {
			this.text = text;
		}

		@Nullable
		public String getNameOfCodingSystem() {
			return this.nameOfCodingSystem;
		}

		public void setNameOfCodingSystem(@Nullable String nameOfCodingSystem) {
			this.nameOfCodingSystem = nameOfCodingSystem;
		}

		@Nullable
		public String getAlternateIdentifier() {
			return this.alternateIdentifier;
		}

		public void setAlternateIdentifier(@Nullable String alternateIdentifier) {
			this.alternateIdentifier = alternateIdentifier;
		}

		@Nullable
		public String getAlternateText() {
			return this.alternateText;
		}

		public void setAlternateText(@Nullable String alternateText) {
			this.alternateText = alternateText;
		}

		@Nullable
		public String getNameOfAlternateCodingSystem() {
			return this.nameOfAlternateCodingSystem;
		}

		public void setNameOfAlternateCodingSystem(@Nullable String nameOfAlternateCodingSystem) {
			this.nameOfAlternateCodingSystem = nameOfAlternateCodingSystem;
		}
	}

	/**
	 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/DataTypes/VID
	 */
	@NotThreadSafe
	public static class VersionId {
		@Nullable
		private String versionId;
		@Nullable
		private CodedElement internationalizationCode;
		@Nullable
		private CodedElement internationalVersionId;

		@Nonnull
		public static Boolean isPresent(@Nullable VID vid) {
			if (vid == null)
				return false;

			return trimToNull(vid.getVersionID().getValueOrEmpty()) != null
					|| CodedElement.isPresent(vid.getInternationalizationCode())
					|| CodedElement.isPresent(vid.getInternationalVersionID());
		}

		public VersionId() {
			// Nothing to do
		}

		public VersionId(@Nullable VID vid) {
			if (vid != null) {
				this.versionId = trimToNull(vid.getVersionID().getValueOrEmpty());

				if (CodedElement.isPresent(vid.getInternationalizationCode()))
					this.internationalizationCode = new CodedElement(vid.getInternationalizationCode());

				if (CodedElement.isPresent(vid.getInternationalVersionID()))
					this.internationalVersionId = new CodedElement(vid.getInternationalVersionID());
			}
		}

		@Override
		public String toString() {
			return GSON.toJson(this);
		}

		@Nullable
		public String getVersionId() {
			return this.versionId;
		}

		public void setVersionId(@Nullable String versionId) {
			this.versionId = versionId;
		}

		@Nullable
		public CodedElement getInternationalizationCode() {
			return this.internationalizationCode;
		}

		public void setInternationalizationCode(@Nullable CodedElement internationalizationCode) {
			this.internationalizationCode = internationalizationCode;
		}

		@Nullable
		public CodedElement getInternationalVersionId() {
			return this.internationalVersionId;
		}

		public void setInternationalVersionId(@Nullable CodedElement internationalVersionId) {
			this.internationalVersionId = internationalVersionId;
		}
	}

	/**
	 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/Segments/MSH
	 */
	@NotThreadSafe
	public static class MessageHeader {
		@Nullable
		private String fieldSeparator; // MSH.1 - Field Separator
		@Nullable
		private String encodingCharacters; // MSH.2 - Encoding Characters
		@Nullable
		private HierarchicDesignator sendingApplication; // MSH.3 - Sending Application
		@Nullable
		private HierarchicDesignator sendingFacility; // MSH.4 - Sending Facility
		@Nullable
		private HierarchicDesignator receivingApplication; // MSH.5 - Receiving Application
		@Nullable
		private HierarchicDesignator receivingFacility; // MSH.6 - Receiving Facility
		@Nullable
		private String dateTimeOfMessage; // MSH.7 - Date / Time of Message
		@Nullable
		private String security; // MSH.8 - Security
		@Nullable
		private MessageType messageType; // MSH.9 - Message Type
		@Nullable
		private String messageControlId; // MSH.10 - Message Control ID
		@Nullable
		private ProcessingType processingId; // MSH.11 - Processing ID
		@Nullable
		private VersionId versionId; // MSH.12 - Version ID
		@Nullable
		private String sequenceNumber; // MSH.13 - Sequence Number
		@Nullable
		private String continuationPointer; // MSH.14 - Continuation Pointer
		@Nullable
		private String acceptAcknowledgementType; // MSH.15 - Accept Acknowledgement Type
		@Nullable
		private String applicationAcknowledgementType; // MSH.16 - Application Acknowledgement Type
		@Nullable
		private String countryCode; // MSH.17 - Country Code
		@Nullable
		private String characterSet; // MSH.18 - Character Set
		@Nullable
		private String principalLanguageOfMessage; // MSH.19 - Principal Language of Message

		@Override
		public String toString() {
			return GSON.toJson(this);
		}

		@Nullable
		public String getFieldSeparator() {
			return this.fieldSeparator;
		}

		public void setFieldSeparator(@Nullable String fieldSeparator) {
			this.fieldSeparator = fieldSeparator;
		}

		@Nullable
		public String getEncodingCharacters() {
			return this.encodingCharacters;
		}

		public void setEncodingCharacters(@Nullable String encodingCharacters) {
			this.encodingCharacters = encodingCharacters;
		}

		@Nullable
		public HierarchicDesignator getSendingApplication() {
			return this.sendingApplication;
		}

		public void setSendingApplication(@Nullable HierarchicDesignator sendingApplication) {
			this.sendingApplication = sendingApplication;
		}

		@Nullable
		public HierarchicDesignator getSendingFacility() {
			return this.sendingFacility;
		}

		public void setSendingFacility(@Nullable HierarchicDesignator sendingFacility) {
			this.sendingFacility = sendingFacility;
		}

		@Nullable
		public HierarchicDesignator getReceivingApplication() {
			return this.receivingApplication;
		}

		public void setReceivingApplication(@Nullable HierarchicDesignator receivingApplication) {
			this.receivingApplication = receivingApplication;
		}

		@Nullable
		public HierarchicDesignator getReceivingFacility() {
			return this.receivingFacility;
		}

		public void setReceivingFacility(@Nullable HierarchicDesignator receivingFacility) {
			this.receivingFacility = receivingFacility;
		}

		@Nullable
		public String getDateTimeOfMessage() {
			return this.dateTimeOfMessage;
		}

		public void setDateTimeOfMessage(@Nullable String dateTimeOfMessage) {
			this.dateTimeOfMessage = dateTimeOfMessage;
		}

		@Nullable
		public String getSecurity() {
			return this.security;
		}

		public void setSecurity(@Nullable String security) {
			this.security = security;
		}

		@Nullable
		public MessageType getMessageType() {
			return this.messageType;
		}

		public void setMessageType(@Nullable MessageType messageType) {
			this.messageType = messageType;
		}

		@Nullable
		public String getMessageControlId() {
			return this.messageControlId;
		}

		public void setMessageControlId(@Nullable String messageControlId) {
			this.messageControlId = messageControlId;
		}

		@Nullable
		public ProcessingType getProcessingId() {
			return this.processingId;
		}

		public void setProcessingId(@Nullable ProcessingType processingId) {
			this.processingId = processingId;
		}

		@Nullable
		public VersionId getVersionId() {
			return this.versionId;
		}

		public void setVersionId(@Nullable VersionId versionId) {
			this.versionId = versionId;
		}

		@Nullable
		public String getSequenceNumber() {
			return this.sequenceNumber;
		}

		public void setSequenceNumber(@Nullable String sequenceNumber) {
			this.sequenceNumber = sequenceNumber;
		}

		@Nullable
		public String getContinuationPointer() {
			return this.continuationPointer;
		}

		public void setContinuationPointer(@Nullable String continuationPointer) {
			this.continuationPointer = continuationPointer;
		}

		@Nullable
		public String getAcceptAcknowledgementType() {
			return this.acceptAcknowledgementType;
		}

		public void setAcceptAcknowledgementType(@Nullable String acceptAcknowledgementType) {
			this.acceptAcknowledgementType = acceptAcknowledgementType;
		}

		@Nullable
		public String getApplicationAcknowledgementType() {
			return this.applicationAcknowledgementType;
		}

		public void setApplicationAcknowledgementType(@Nullable String applicationAcknowledgementType) {
			this.applicationAcknowledgementType = applicationAcknowledgementType;
		}

		@Nullable
		public String getCountryCode() {
			return this.countryCode;
		}

		public void setCountryCode(@Nullable String countryCode) {
			this.countryCode = countryCode;
		}

		@Nullable
		public String getCharacterSet() {
			return this.characterSet;
		}

		public void setCharacterSet(@Nullable String characterSet) {
			this.characterSet = characterSet;
		}

		@Nullable
		public String getPrincipalLanguageOfMessage() {
			return this.principalLanguageOfMessage;
		}

		public void setPrincipalLanguageOfMessage(@Nullable String principalLanguageOfMessage) {
			this.principalLanguageOfMessage = principalLanguageOfMessage;
		}
	}

	/**
	 * See https://hl7-definition.caristix.com/v2/hl7v2.5.1/Segments/ORC
	 */
	@NotThreadSafe
	public static class CommonOrder {
		@Nullable
		private String orderControl; // ORC.1 - Order Control
		@Nullable
		private String placerOrderNumber; // ORC.2 - Placer Order Number
		@Nullable
		private String fillerOrderNumber; // ORC.3 - Filler Order Number
		@Nullable
		private String placerGroupNumber; // ORC.4 - Placer Group Number
		@Nullable
		private String orderStatus; // ORC.5 - Order Status
		@Nullable
		private String responseFlag; // ORC.6 - Response Flag
		@Nullable
		private String quantityTiming; // ORC.7 - Quantity/Timing
		@Nullable
		private String parentOrder; // ORC.8 - Parent Order
		@Nullable
		private String dateTimeOfTransaction; // ORC.9 - Date/Time of Transaction
		@Nullable
		private String enteredBy; // ORC.10 - Entered By
		@Nullable
		private String verifiedBy; // ORC.11 - Verified By
		@Nullable
		private String orderingProvider; // ORC.12 - Ordering Provider
		@Nullable
		private String enterersLocation; // ORC.13 - Enterer's Location
		@Nullable
		private String callBackPhoneNumber; // ORC.14 - Call Back Phone Number
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

		@Override
		public String toString() {
			return GSON.toJson(this);
		}

		@Nullable
		public String getOrderControl() {
			return this.orderControl;
		}

		public void setOrderControl(@Nullable String orderControl) {
			this.orderControl = orderControl;
		}

		@Nullable
		public String getPlacerOrderNumber() {
			return this.placerOrderNumber;
		}

		public void setPlacerOrderNumber(@Nullable String placerOrderNumber) {
			this.placerOrderNumber = placerOrderNumber;
		}

		@Nullable
		public String getFillerOrderNumber() {
			return this.fillerOrderNumber;
		}

		public void setFillerOrderNumber(@Nullable String fillerOrderNumber) {
			this.fillerOrderNumber = fillerOrderNumber;
		}

		@Nullable
		public String getPlacerGroupNumber() {
			return this.placerGroupNumber;
		}

		public void setPlacerGroupNumber(@Nullable String placerGroupNumber) {
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
		public String getQuantityTiming() {
			return this.quantityTiming;
		}

		public void setQuantityTiming(@Nullable String quantityTiming) {
			this.quantityTiming = quantityTiming;
		}

		@Nullable
		public String getParentOrder() {
			return this.parentOrder;
		}

		public void setParentOrder(@Nullable String parentOrder) {
			this.parentOrder = parentOrder;
		}

		@Nullable
		public String getDateTimeOfTransaction() {
			return this.dateTimeOfTransaction;
		}

		public void setDateTimeOfTransaction(@Nullable String dateTimeOfTransaction) {
			this.dateTimeOfTransaction = dateTimeOfTransaction;
		}

		@Nullable
		public String getEnteredBy() {
			return this.enteredBy;
		}

		public void setEnteredBy(@Nullable String enteredBy) {
			this.enteredBy = enteredBy;
		}

		@Nullable
		public String getVerifiedBy() {
			return this.verifiedBy;
		}

		public void setVerifiedBy(@Nullable String verifiedBy) {
			this.verifiedBy = verifiedBy;
		}

		@Nullable
		public String getOrderingProvider() {
			return this.orderingProvider;
		}

		public void setOrderingProvider(@Nullable String orderingProvider) {
			this.orderingProvider = orderingProvider;
		}

		@Nullable
		public String getEnterersLocation() {
			return this.enterersLocation;
		}

		public void setEnterersLocation(@Nullable String enterersLocation) {
			this.enterersLocation = enterersLocation;
		}

		@Nullable
		public String getCallBackPhoneNumber() {
			return this.callBackPhoneNumber;
		}

		public void setCallBackPhoneNumber(@Nullable String callBackPhoneNumber) {
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
	}
}